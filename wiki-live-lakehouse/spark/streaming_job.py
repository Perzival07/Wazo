import os

from delta.tables import DeltaTable
from pyspark.sql import functions as F
from pyspark.sql.types import (BooleanType, IntegerType, LongType, StringType, StructField, StructType, TimestampType)

from spark_session import (BRONZE_PATH, CHECKPOINTS, GOLD_ACTIVITY_PATH, GOLD_TRENDING_PATH, GOLD_WIKIS_PATH, SILVER_PATH, get_spark)

KAFKA_BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP", "localhost:9092")
KAFKA_TOPIC = os.getenv("KAFKA_TOPIC", "wiki-changes")

BRONZE_SCHEMA = StructType([
    StructField("kafka_key", StringType()),
    StructField("raw_json", StringType()),
    StructField("kafka_partition", IntegerType()),
    StructField("kafka_offset", LongType()),
    StructField("kafka_timestamp", TimestampType()),
    StructField("ingested_at", TimestampType()),
])

EVENT_SCHEMA = StructType([
    StructField("meta", StructType([
        StructField("id", StringType()),
        StructField("uri", StringType()),
        StructField("domain", StringType()),
    ])),
    StructField("type", StringType()),
    StructField("namespace", IntegerType()),
    StructField("title", StringType()),
    StructField("bot", BooleanType()),
    StructField("minor", BooleanType()),
    StructField("timestamp", LongType()),
    StructField("user", StringType()),
    StructField("wiki", StringType()),
    StructField("server_name", StringType()),
    StructField("comment", StringType()),
    StructField("length", StructType([
        StructField("old", LongType()),
        StructField("new", LongType()),
    ])),
])

spark = get_spark("wiki-live-lakehouse")




DeltaTable.createIfNotExists(spark).location(BRONZE_PATH).addColumns(BRONZE_SCHEMA).execute()


bronze = (
    spark.readStream.format("kafka")
    .option("kafka.bootstrap.servers", KAFKA_BOOTSTRAP)
    .option("subscribe", KAFKA_TOPIC)
    .option("startingOffsets", "earliest")
    .option("failOnDataLoss", "false")
    .option("maxOffsetsperTrigger", 20000)
    .load()
    .select(
        F.col("key").cast("string").alias("kafka_key"),
        F.col("value").cast("string").alias("raw_json"),
        F.col("partition").alias("kafka_partition"),
        F.col("offset").alias("kafka_offset"),
        F.col("timestamp").alias("kafka_timestamp"),
        F.current_timestamp().alias("ingested_at"),
    )
)


(bronze.writeStream.format("delta")
    .outputMode("append")
    .option("checkpointLocation", f"{CHECKPOINTS}/bronze")
    .trigger(processingTime="5 seconds")
    .queryName("bronze_wiki")
    .start(BRONZE_PATH))


silver_stream = (
    spark.readStream.format("delta").load(BRONZE_PATH)
    .select(F.from_json("raw_json", EVENT_SCHEMA).alias("e"), "ingested_at")
    .select(
        F.col("e.meta.id").alias("event_id"),
        F.timestamp_seconds("e.timestamp").alias("event_time"),
        F.col("e.wiki").alias("wiki"),
        F.col("e.server_name").alias("server_name"),
        F.col("e.type").alias("change_type"),
        F.col("e.namespace").alias("namespace"),
        F.col("e.title").alias("title"),
        F.col("e.user").alias("user"),
        F.coalesce(F.col("e.bot"), F.lit(False)).alias("is_bot"),
        F.coalesce(F.col("e.minor"), F.lit(False)).alias("is_minor"),
        (F.coalesce(F.col("e.length.new"), F.lit(0))
         - F.coalesce(F.col("e.length.old"), F.lit(0))).alias("bytes_changed"),
        F.col("e.meta.uri").alias("page_url"),
        F.col("e.comment").alias("comment"),
        "ingested_at",
        F.col("e.meta.domain").alias("_domain"),
    )
    .filter(F.col("event_id").isNotNull()
            & F.col("event_time").isNotNull()
            & (F.col("_domain") != "canary"))
    .drop("_domain")
    .withWatermark("event_time", "10 minutes")
    .dropDuplicatesWithinWatermark(["event_id"])
)


def write_silver_and_refresh_gold(batch_df, batch_id):
    batch_df.persist()
    if batch_df.isEmpty():
        batch_df.unpersist()
        return 

    (batch_df.write.format("delta").mode("append")
        .option("txnAppId", "silver_wiki_writer")
        .option("txnVersion", batch_id)
        .save(SILVER_PATH))

    silver = batch_df.sparkSession.read.format("delta").load(SILVER_PATH)

    (silver.groupBy(F.window("event_time", "1 minute").alias("w"))
        .agg(F.count("*").alias("events"),
             F.sum(F.when(F.col("change_type") == "edit", 1).otherwise(0)).alias("edits"),
             F.sum(F.when(F.col("change_type") == "new", 1).otherwise(0)).alias("new_pages"),
             F.sum(F.col("is_bot").cast("int")).alias("bot_events"),
             F.sum((~F.col("is_bot")).cast("int")).alias("human_events"),
             F.sum("bytes_changed").alias("bytes_changed"),
             F.countDistinct("wiki").alias("active_wikis"))
        .select(F.col("w.start").alias("window_start"), F.col("w.end").alias("window_end"), "*")
        .drop("w")
        .write.format("delta").mode("overwrite").option("overwriteSchema", "true")
        .save(GOLD_ACTIVITY_PATH))

    (silver.groupBy("wiki")
        .agg(F.count("*").alias("events"),
             F.countDistinct("user").alias("unique_users"),
             F.round(F.avg(F.col("is_bot").cast("int")) * 100, 1).alias("bot_share_pct"),
             F.sum("bytes_changed").alias("bytes_changed"))
        .write.format("delta").mode("overwrite").save(GOLD_WIKIS_PATH))

    latest = silver.agg(F.max("event_time")).first()[0]
    (silver
        .filter((F.col("event_time") >= F.lit(latest) - F.expr("INTERVAL 60 MINUTES"))
                & F.col("change_type").isin("edit", "new")
                & (F.col("namespace") == 0))
        .groupBy("wiki", "title")
        .agg(F.count("*").alias("edits"),
             F.countDistinct("user").alias("editors"),
             F.max("event_time").alias("last_edit"),
             F.first("page_url").alias("page_url"))
        .orderBy(F.desc("edits"))
        .limit(50)
        .write.format("delta").mode("overwrite").save(GOLD_TRENDING_PATH))

    print(f"[batch {batch_id}] +{batch_df.count()} live changes → silver; gold refreshed")
    batch_df.unpersist()


(silver_stream.writeStream
    .foreachBatch(write_silver_and_refresh_gold)
    .option("checkpointLocation", f"{CHECKPOINTS}/silver")
    .trigger(processingTime="15 seconds")
    .queryName("silver_and_gold")
    .start())

print("Live pipeline running. Spark UI: http://localhost:4040")
spark.streams.awaitAnyTermination()
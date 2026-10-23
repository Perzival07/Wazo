import os

from delta import configure_spark_with_delta_pip
from pyspark.sql import SparkSession

KAFKA_PACKAGE = "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.3"

LAKE = os.getenv("LAKE_PATH", "./data/lakehouse")
CHECKPOINTS = os.getenv("CHECKPOINT_PATH", "./data/checkpoints")

BRONZE_PATH = f"{LAKE}/bronze/wiki_events"
SILVER_PATH = f"{LAKE}/silver/wiki_changes"
GOLD_ACTIVITY_PATH = f"{LAKE}/gold/activity_per_minute"
GOLD_WIKIS_PATH = f"{LAKE}/gold/top_wikis"
GOLD_TRENDING_PATH = f"{LAKE}/gold/trending_pages"


def get_spark(app_name: str) -> SparkSession:
    builder = (
        SparkSession.builder.appName(app_name)
        .master(os.getenv("SPARK_MASTER", "local[*]"))
        .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
        .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
        .config("spark.sql.shuffle.partitions", "4")
        .config("spark.sql.parquet.outputTimestampType", "TIMESTAMP_MICROS")
    )

    spark = configure_spark_with_delta_pip(builder, extra_packages=[KAFKA_PACKAGE]).getOrCreate()
    spark.sparkContext.setLogLevel("WARN")
    return spark
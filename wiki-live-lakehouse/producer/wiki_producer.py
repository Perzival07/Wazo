import json
import os
import time

import requests
from confluent_kafka import Producer

STREAM_URL = os.getenv("WIKI_STREAM_URL", "https://stream.wikimedia.org/v2/stream/recentchange")
BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP", "localhost:9092")
TOPIC = os.getenv("KAFKA_TOPIC", "wiki-changes")
WIKI_FILTER = os.getenv("WIKI_FILTER", "").strip()
USER_AGENT = os.getenv("WIKI_USER_AGENT", "WikiLiveLakehouse/1.0 (learning project)")


def read_sse(last_event_id=None):
    headers = {"User-Agent": USER_AGENT, "Accept": "text/event-stream"}
    if last_event_id:
        headers["Last-Event-ID"] = last_event_id


    with requests.get(STREAM_URL, headers=headers, stream=True, timeout=(10,60)) as resp:
        resp.raise_for_status()
        event_id, data_lines = None, []
        for line in resp.iter_lines(decode_unicode=True):
            if line is None:
                continue
            if line == "":
                if data_lines:
                    yield event_id, "\n".join(data_lines)
                event_id, data_lines = None, []
            elif line.startswith("id:"):
                event_id = line[3:].strip()
            elif line.startswith("data:"):
                data_lines.append(line[5:].lstrip())



def send(producer, key, value):
    while True:
        try:
            producer.produce(TOPIC, key=key,value=value)
            producer.poll(0)
            return
        except BufferError:
            producer.poll(1)



def main():
    producer = Producer({"bootstrap.servers": BOOTSTRAP, "linger.ms": 100, "compression.type": "lz4"})
    last_id, sent, backoff = None, 0, 1

    print(f"Streaming {STREAM_URL} -> Kafka topic '{TOPIC}'" + (f" (only {WIKI_FILTER})" if WIKI_FILTER else " (all wikis)"))

    while True:
        try:
            for event_id, data in read_sse(last_id):
                last_id = event_id or last_id
                backoff = 1
                try:
                    event = json.loads(data)
                except json.JSONDecodeError:
                    continue
                if event.get("meta", {}).get("domain") == "canary":
                    continue
                if WIKI_FILTER and event.get("wiki") != WIKI_FILTER:
                    continue

                send(producer, key=event.get("wiki"), value=data)
                sent += 1
                if sent % 200 == 0:
                    print(f"Sent {sent} live events (latest: {event.get('wiki')} - {event.get('title')})")


        except (requests.RequestException, ConnectionError) as e:
            print(f"Stream disconnected ({e}); reconnecting in {backoff}s...")
            time.sleep(backoff)
            backoff = min(backoff * 2, 60)
        finally:
            producer.flush(5)

if __name__ == "__main__":
    main()
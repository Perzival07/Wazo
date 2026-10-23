import os

import pandas as pd
import streamlit as st
from deltalake import DeltaTable

LAKE = os.getenv("LAKE_PATH", "./data/lakehouse")

st.set_page_config(page_title="Wikipedia Live Lakehouse", layout="wide")
st.title("Wikipedia Live: Lakehouse Dashboard")
st.caption("Real edits from all Wikimedia sites → Kafka → Spark → Delta Lake (Gold). Refreshes every 5 seconds.")


def load(table: str) -> pd.DataFrame:
    try:
        return DeltaTable(f"{LAKE}/{table}").to_pandas()
    except Exception:
        return pd.DataFrame()


@st.fragment(run_every=5)
def live_view():
    activity = load("gold/activity_per_minute")
    if activity.empty:
        st.info("Waiting for the first data... (Spark needs about 1 minute on first start)")
        return

    activity = activity.sort_values("window_start")


    last = activity.iloc[-2] if len(activity) > 1 else activity.iloc[-1]

    c1, c2, c3, c4 = st.columns(4)
    c1.metric("Changes / min", int(last["events"]))
    c2.metric("Human edits / min", int(last["human_events"]))
    c3.metric("Bot edits / min", int(last["bot_events"]))
    c4.metric("New pages / min", int(last["new_pages"]))

    st.subheader("Activity per minute")
    st.line_chart(activity.set_index("window_start")[["human_events", "bot_events"]].tail(60))

    left, right = st.columns(2)

    with left:
        st.subheader("Most active wikis")
        wikis = load("gold/top_wikis")
        if not wikis.empty:
            top = wikis.sort_values("events", ascending=False).head(10)
            st.bar_chart(top.set_index("wiki")["events"])
            st.dataframe(top, hide_index=True, use_container_width=True)

    with right:
        st.subheader("Trending articles (last 60 min)")
        trending = load("gold/trending_pages")
        if not trending.empty:
            st.dataframe(
                trending.sort_values("edits", ascending=False)
                        [["wiki", "title", "edits", "editors", "page_url"]].head(20),
                hide_index=True, use_container_width=True,
                column_config={"page_url": st.column_config.LinkColumn("link")},
            )


live_view()
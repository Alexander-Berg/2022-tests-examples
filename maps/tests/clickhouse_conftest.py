import time

import clickhouse_driver
import pytest
import requests

from smb.common.multiruntime import lib as multiruntime

ch_setup_config = {
    "source": {
        "fixture": "source_setup.sql",
        "sql": ["TRUNCATE TABLE stat.source_sample"],
    },
    "normalized": {
        "fixture": "normalized_setup.sql",
        "sql": ["TRUNCATE TABLE stat.normalized_sample"],
    },
    "accepted": {
        "fixture": "accepted_setup.sql",
        "sql": [
            "TRUNCATE TABLE stat.accepted_sample",
            "TRUNCATE TABLE stat.accepted_sample_event_group_ids",
        ],
    },
}


def sql_fixture(fname):
    return map(
        lambda s: f"{s.strip()};",
        filter(
            lambda s: s.strip(),
            multiruntime.io.read_file(f"tests/fixtures/{fname}").split(";"),
        ),
    )


@pytest.fixture(scope="session")
def ch_client():
    return clickhouse_driver.Client("localhost", 9001)


@pytest.fixture(scope="session")
def wait_for_chs(ch_client):
    waited = 0

    while waited < 100:
        try:
            ch_client.connection.connect()
        except clickhouse_driver.errors.NetworkError:
            time.sleep(1)
            waited += 1
        else:
            ch_client.disconnect()
            break
    else:
        ConnectionRefusedError()


@pytest.fixture(autouse=True)
def setup_clickhouses(wait_for_chs, ch_client):
    url = "http://localhost:8124"

    for ch_config in ch_setup_config.values():
        for query in sql_fixture(ch_config["fixture"]):
            response = requests.post(url, params={"query": query})
            assert response.status_code == 200, response.content

    yield

    for ch_config in ch_setup_config.values():
        for stmt in ch_config["sql"]:
            ch_client.execute(stmt)


@pytest.fixture
def clear_normalized_ch(ch_client):
    for stmt in ch_setup_config["normalized"]["sql"]:
        return lambda: ch_client.execute(stmt) or time.sleep(0.1)

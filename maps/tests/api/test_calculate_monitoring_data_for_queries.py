import json

import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.no_setup_ch]

url = "/monitoring/queries/?now=2020-10-10T11:15:01Z&period=900s"


@pytest.fixture
def fill_query_log(ch, setup_ch_for_monitorings):
    try:
        ch.execute(
            "INSERT INTO sys.query_log VALUES",
            [
                (
                    "QueryFinish",
                    dt("2020-10-10 11:00:01"),
                    2,
                    1,
                    "--tag:a\nselect 1",
                    "",
                ),
                (
                    "QueryFinish",
                    dt("2020-10-10 11:00:02"),
                    4,
                    3,
                    "--tag:a\nselect 1",
                    "",
                ),
            ],
        )

        yield

    finally:
        ch.execute("TRUNCATE TABLE sys.query_log")


@pytest.mark.mock_ch_query_log
@pytest.mark.usefixtures("fill_query_log")
async def test_returns_expected(api):
    result = json.loads(await api.get(url, expected_status=200))

    assert result == {
        "metrics": [
            {
                "labels": {
                    "aggregate": "min",
                    "exception_code": 0,
                    "host": "localhost",
                    "port": 9001,
                    "tags": "a",
                    "type": "memory",
                },
                "type": "IGAUGE",
                "value": 1,
            },
            {
                "labels": {
                    "aggregate": "min",
                    "exception_code": 0,
                    "host": "localhost",
                    "port": 9001,
                    "tags": "a",
                    "type": "duration_ms",
                },
                "type": "IGAUGE",
                "value": 2,
            },
            {
                "labels": {
                    "aggregate": "max",
                    "exception_code": 0,
                    "host": "localhost",
                    "port": 9001,
                    "tags": "a",
                    "type": "memory",
                },
                "type": "IGAUGE",
                "value": 3,
            },
            {
                "labels": {
                    "aggregate": "max",
                    "exception_code": 0,
                    "host": "localhost",
                    "port": 9001,
                    "tags": "a",
                    "type": "duration_ms",
                },
                "type": "IGAUGE",
                "value": 4,
            },
            {
                "labels": {
                    "aggregate": "avg",
                    "exception_code": 0,
                    "host": "localhost",
                    "port": 9001,
                    "tags": "a",
                    "type": "memory",
                },
                "type": "IGAUGE",
                "value": 2,
            },
            {
                "labels": {
                    "aggregate": "avg",
                    "exception_code": 0,
                    "host": "localhost",
                    "port": 9001,
                    "tags": "a",
                    "type": "duration_ms",
                },
                "type": "IGAUGE",
                "value": 3,
            },
            {
                "labels": {
                    "aggregate": "median",
                    "exception_code": 0,
                    "host": "localhost",
                    "port": 9001,
                    "tags": "a",
                    "type": "memory",
                },
                "type": "IGAUGE",
                "value": 2,
            },
            {
                "labels": {
                    "aggregate": "median",
                    "exception_code": 0,
                    "host": "localhost",
                    "port": 9001,
                    "tags": "a",
                    "type": "duration_ms",
                },
                "type": "IGAUGE",
                "value": 3,
            },
        ]
    }

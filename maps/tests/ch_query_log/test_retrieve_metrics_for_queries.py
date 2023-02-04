from itertools import chain

import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def fill_query_log(ch):
    try:
        ch.execute(
            "INSERT INTO sys.query_log VALUES",
            [
                (
                    "QueryStart",
                    dt("2020-10-10 11:00:00"),
                    0,
                    0,
                    "--tag:a\nselect 1000",
                    "",
                ),
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
                (
                    "QueryFinish",
                    dt("2020-10-10 11:00:03"),
                    6,
                    5,
                    "--tag:b\n--tag:a\nselect 1",
                    "",
                ),
                (
                    "QueryFinish",
                    dt("2020-10-10 11:10:00"),
                    8,
                    7,
                    # "--tag:b\nselect 1",
                    "select 1\n   --   tag   :   b   ",
                    "Code: 123, e.displayText() = kek",
                ),
                ("QueryFinish", dt("2020-10-10 11:20:00"), 10, 9, "select 1", ""),
            ],
        )

        yield

    finally:
        ch.execute("TRUNCATE TABLE sys.query_log")


@pytest.mark.usefixtures("fill_query_log")
async def test_returns_metrics(ch_query_log):
    got = await ch_query_log.retrieve_metrics_for_queries(
        from_datetime=dt("2020-10-10 11:00:00"), to_datetime=dt("2020-10-10 11:10:00")
    )

    assert got == [
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
        {
            "labels": {
                "aggregate": "min",
                "exception_code": 0,
                "host": "localhost",
                "port": 9001,
                "tags": "a,b",
                "type": "memory",
            },
            "type": "IGAUGE",
            "value": 5,
        },
        {
            "labels": {
                "aggregate": "min",
                "exception_code": 0,
                "host": "localhost",
                "port": 9001,
                "tags": "a,b",
                "type": "duration_ms",
            },
            "type": "IGAUGE",
            "value": 6,
        },
        {
            "labels": {
                "aggregate": "max",
                "exception_code": 0,
                "host": "localhost",
                "port": 9001,
                "tags": "a,b",
                "type": "memory",
            },
            "type": "IGAUGE",
            "value": 5,
        },
        {
            "labels": {
                "aggregate": "max",
                "exception_code": 0,
                "host": "localhost",
                "port": 9001,
                "tags": "a,b",
                "type": "duration_ms",
            },
            "type": "IGAUGE",
            "value": 6,
        },
        {
            "labels": {
                "aggregate": "avg",
                "exception_code": 0,
                "host": "localhost",
                "port": 9001,
                "tags": "a,b",
                "type": "memory",
            },
            "type": "IGAUGE",
            "value": 5,
        },
        {
            "labels": {
                "aggregate": "avg",
                "exception_code": 0,
                "host": "localhost",
                "port": 9001,
                "tags": "a,b",
                "type": "duration_ms",
            },
            "type": "IGAUGE",
            "value": 6,
        },
        {
            "labels": {
                "aggregate": "median",
                "exception_code": 0,
                "host": "localhost",
                "port": 9001,
                "tags": "a,b",
                "type": "memory",
            },
            "type": "IGAUGE",
            "value": 5,
        },
        {
            "labels": {
                "aggregate": "median",
                "exception_code": 0,
                "host": "localhost",
                "port": 9001,
                "tags": "a,b",
                "type": "duration_ms",
            },
            "type": "IGAUGE",
            "value": 6,
        },
        {
            "labels": {
                "aggregate": "min",
                "exception_code": 123,
                "host": "localhost",
                "port": 9001,
                "tags": "b",
                "type": "memory",
            },
            "type": "IGAUGE",
            "value": 7,
        },
        {
            "labels": {
                "aggregate": "min",
                "exception_code": 123,
                "host": "localhost",
                "port": 9001,
                "tags": "b",
                "type": "duration_ms",
            },
            "type": "IGAUGE",
            "value": 8,
        },
        {
            "labels": {
                "aggregate": "max",
                "exception_code": 123,
                "host": "localhost",
                "port": 9001,
                "tags": "b",
                "type": "memory",
            },
            "type": "IGAUGE",
            "value": 7,
        },
        {
            "labels": {
                "aggregate": "max",
                "exception_code": 123,
                "host": "localhost",
                "port": 9001,
                "tags": "b",
                "type": "duration_ms",
            },
            "type": "IGAUGE",
            "value": 8,
        },
        {
            "labels": {
                "aggregate": "avg",
                "exception_code": 123,
                "host": "localhost",
                "port": 9001,
                "tags": "b",
                "type": "memory",
            },
            "type": "IGAUGE",
            "value": 7,
        },
        {
            "labels": {
                "aggregate": "avg",
                "exception_code": 123,
                "host": "localhost",
                "port": 9001,
                "tags": "b",
                "type": "duration_ms",
            },
            "type": "IGAUGE",
            "value": 8,
        },
        {
            "labels": {
                "aggregate": "median",
                "exception_code": 123,
                "host": "localhost",
                "port": 9001,
                "tags": "b",
                "type": "memory",
            },
            "type": "IGAUGE",
            "value": 7,
        },
        {
            "labels": {
                "aggregate": "median",
                "exception_code": 123,
                "host": "localhost",
                "port": 9001,
                "tags": "b",
                "type": "duration_ms",
            },
            "type": "IGAUGE",
            "value": 8,
        },
    ]


@pytest.mark.usefixtures("fill_query_log")
async def test_ignores_queries_without_tags(ch_query_log):
    got = await ch_query_log.retrieve_metrics_for_queries(
        from_datetime=dt("2020-10-10 11:20:00"), to_datetime=dt("2020-10-10 11:20:00")
    )

    assert got == []


@pytest.mark.usefixtures("fill_query_log")
async def test_no_exceptions_if_clickhouse_node_of_shutdown(
    ch_query_log, caplog, mocker
):
    mocker.patch.object(
        ch_query_log,
        "_hosts",
        list(chain(ch_query_log._hosts, [{"host": "shutdown.host", "port": 1234}])),
    )

    got = await ch_query_log.retrieve_metrics_for_queries(
        from_datetime=dt("2020-10-10 11:20:00"), to_datetime=dt("2020-10-10 11:20:00")
    )

    assert got == []
    assert (
        "Code: 210. Name or service not known (shutdown.host:1234)" in caplog.messages
    )

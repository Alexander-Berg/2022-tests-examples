import json

import pytest
from yql.util.format import YqlStruct

from maps_adv.geosmb.landlord.server.lib.tasks import ImportGoogleCountersTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportGoogleCountersTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_google_counters_from_yt.side_effect = gen_consumer


async def test_executes_yql(mock_yql, task):
    await task

    mock_yql["query"].assert_called_with(
        """
    SELECT permalink, AGGREGATE_LIST(AsStruct(counter_id as id, goals as goals)) FROM (
        SELECT
            permalink,
            counter_id,
            ToDict(AGGREGATE_LIST(AsTuple(event_name, event_id))) as goals
        FROM senecaman.`//home/counters`
        GROUP BY permalink, counter_id
    )
    GROUP BY permalink
    """,
        syntax_version=1,
    )


async def test_sends_data_to_dm(task, dm, mock_yql):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        (
            12345,
            [
                YqlStruct(
                    [
                        (
                            "goals",
                            {
                                "call": "ggg111",
                                "route": "ggg222",
                            },
                        ),
                        ("id", "GOOGLE1"),
                    ]
                )
            ],
        ),
        (
            23456,
            [
                YqlStruct(
                    [
                        (
                            "goals",
                            {
                                "call": "ggg333",
                                "cta": "ggg444",
                            },
                        ),
                        ("id", "GOOGLE2"),
                    ]
                )
            ],
        ),
        (
            34567,
            [
                YqlStruct(
                    [
                        (
                            "goals",
                            {
                                "phone": "ggg555",
                                "route": "ggg666",
                                "cta": "ggg777",
                            },
                        ),
                        ("id", "GOOGLE3"),
                    ]
                )
            ],
        ),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.import_google_counters_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        (
            12345,
            json.dumps(
                [
                    {
                        "id": "GOOGLE1",
                        "goals": {
                            "call": "ggg111",
                            "route": "ggg222",
                        },
                    }
                ]
            ),
        ),
        (
            23456,
            json.dumps(
                [
                    {
                        "id": "GOOGLE2",
                        "goals": {
                            "call": "ggg333",
                            "cta": "ggg444",
                        },
                    }
                ]
            ),
        ),
        (
            34567,
            json.dumps(
                [
                    {
                        "id": "GOOGLE3",
                        "goals": {
                            "phone": "ggg555",
                            "route": "ggg666",
                            "cta": "ggg777",
                        },
                    }
                ]
            ),
        ),
    ]

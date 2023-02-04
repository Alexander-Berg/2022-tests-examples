import json

import pytest
from yql.util.format import YqlStruct

from maps_adv.geosmb.landlord.server.lib.tasks import ImportTikTokPixelsTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportTikTokPixelsTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_tiktok_pixels_from_yt.side_effect = gen_consumer


async def test_executes_yql(mock_yql, task):
    await task

    mock_yql["query"].assert_called_with(
        """
    SELECT permalink, AGGREGATE_LIST(AsStruct(tiktok_pixel_id as id, goals as goals)) FROM (
        SELECT
            permalink,
            tiktok_pixel_id,
            ToDict(AGGREGATE_LIST(AsTuple(goal_name, pixel_event_name))) as goals
        FROM hahn.`//home/pixels`
        GROUP BY permalink, tiktok_pixel_id
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
                                "goal1": "ppp1",
                            },
                        ),
                        ("id", "PIXEL1"),
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
                                "goal1": "ppp1",
                                "goal2": "ppp2",
                            },
                        ),
                        ("id", "PIXEL2"),
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
                                "goal1": "ppp1",
                                "goal2": "ppp2",
                                "goal3": "ppp3",
                            },
                        ),
                        ("id", "PIXEL3"),
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

    dm.import_tiktok_pixels_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        (
            12345,
            json.dumps(
                [
                    {
                        "id": "PIXEL1",
                        "goals": {
                            "goal1": "ppp1",
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
                        "id": "PIXEL2",
                        "goals": {
                            "goal1": "ppp1",
                            "goal2": "ppp2",
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
                        "id": "PIXEL3",
                        "goals": {
                            "goal1": "ppp1",
                            "goal2": "ppp2",
                            "goal3": "ppp3",
                        },
                    }
                ]
            ),
        ),
    ]

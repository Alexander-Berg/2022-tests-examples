import json

import pytest

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [
        (
            100,
            json.dumps([{"id": "QQQ", "goals": {"route": "ggg111", "call": "ggg222"}}]),
        ),
        (
            101,
            json.dumps([{"id": "WWW", "goals": {"route": "ggg333", "call": "ggg444"}}]),
        ),
    ],
    [
        (
            102,
            json.dumps(
                [
                    {
                        "id": "EEE",
                        "goals": {"route": "ggg555", "call": "ggg666", "cta": "ggg777"},
                    }
                ]
            ),
        )
    ],
]


async def test_writes_lines(factory, dm):
    await dm.import_vk_pixels_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_vk_pixels() == [
        {
            "permalink": 100,
            "pixel_data": [
                {"id": "QQQ", "goals": {"route": "ggg111", "call": "ggg222"}}
            ],
        },
        {
            "permalink": 101,
            "pixel_data": [
                {"id": "WWW", "goals": {"route": "ggg333", "call": "ggg444"}}
            ],
        },
        {
            "permalink": 102,
            "pixel_data": [
                {
                    "id": "EEE",
                    "goals": {"route": "ggg555", "call": "ggg666", "cta": "ggg777"},
                }
            ],
        },
    ]


async def test_removes_existing_data(factory, dm):
    await factory.create_vk_pixels()

    await dm.import_vk_pixels_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_vk_pixels() == [
        {
            "permalink": 100,
            "pixel_data": [
                {"id": "QQQ", "goals": {"route": "ggg111", "call": "ggg222"}}
            ],
        },
        {
            "permalink": 101,
            "pixel_data": [
                {"id": "WWW", "goals": {"route": "ggg333", "call": "ggg444"}}
            ],
        },
        {
            "permalink": 102,
            "pixel_data": [
                {
                    "id": "EEE",
                    "goals": {"route": "ggg555", "call": "ggg666", "cta": "ggg777"},
                }
            ],
        },
    ]


async def test_clears_existing_data_if_empty_generator(factory, dm):
    await factory.create_vk_pixels()

    try:
        await dm.import_vk_pixels_from_yt(AsyncIterator([]))
    except:  # noqa
        pytest.fail("Should not raise")

    assert await factory.retrieve_all_vk_pixels() == []


async def test_returns_nothing(dm):
    got = await dm.import_vk_pixels_from_yt(AsyncIterator(input_data))

    assert got is None

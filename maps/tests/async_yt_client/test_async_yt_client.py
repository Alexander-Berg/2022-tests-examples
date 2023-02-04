import pytest
from yt.packages.requests import ReadTimeout

from maps_adv.geosmb.landlord.server.lib.async_yt_client import AsyncYtClient
from maps_adv.geosmb.landlord.server.tests.conftest import yt_rows_interator


@pytest.fixture()
async def async_yt_client(config):
    client = AsyncYtClient(config["YT_TOKEN"], config["YT_GOOGLE_COUNTERS_TABLE"])
    client.start()
    return client


async def test_returns_google_counters(async_yt_client, yt_client):
    yt_client.select_rows.return_value = yt_rows_interator(
        [
            {
                "permalink": 111,
                "counter_id": "GoogleId1",
                "event_name": "click",
                "event_id": "GoogleId111",
            },
            {
                "permalink": 111,
                "counter_id": "GoogleId1",
                "event_name": "route",
                "event_id": "GoogleId112",
            },
        ]
    )

    result = await async_yt_client.get_google_counters_for_permalink(111)

    assert result == [
        {"id": "GoogleId1", "goals": {"click": "GoogleId111", "route": "GoogleId112"}}
    ]


async def test_returns_google_counters_multiple(async_yt_client, yt_client):
    yt_client.select_rows.return_value = yt_rows_interator(
        [
            {
                "permalink": 111,
                "counter_id": "GoogleId1",
                "event_name": "click",
                "event_id": "GoogleId111",
            },
            {
                "permalink": 111,
                "counter_id": "GoogleId1",
                "event_name": "route",
                "event_id": "GoogleId112",
            },
            {
                "permalink": 111,
                "counter_id": "GoogleId2",
                "event_name": "click",
                "event_id": "GoogleId211",
            },
            {
                "permalink": 111,
                "counter_id": "GoogleId2",
                "event_name": "call",
                "event_id": "GoogleId212",
            },
        ]
    )

    result = await async_yt_client.get_google_counters_for_permalink(111)

    assert result == [
        {"id": "GoogleId1", "goals": {"click": "GoogleId111", "route": "GoogleId112"}},
        {"id": "GoogleId2", "goals": {"click": "GoogleId211", "call": "GoogleId212"}},
    ]


async def test_returns_none_if_no_google_counters(async_yt_client, yt_client):
    yt_client.select_rows.return_value = yt_rows_interator([])

    result = await async_yt_client.get_google_counters_for_permalink(111)

    assert result is None


async def test_returns_none_if_connection_error(async_yt_client, yt_client):
    yt_client.select_rows.side_effect = ReadTimeout()

    result = await async_yt_client.get_google_counters_for_permalink(111)

    assert result is None

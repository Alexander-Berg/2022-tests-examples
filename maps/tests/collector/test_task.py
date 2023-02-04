import asyncio
from decimal import Decimal
from unittest.mock import patch

import pytest

from maps_adv.stat_controller.client.lib.base import Conflict
from maps_adv.stat_tasks_starter.tests.tools import coro_mock, dt, make_event

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("run_server")]


charged_events = [
    [1, 100, "pin.show", Decimal("10"), "1"],
    [1, 200, "pin.show", Decimal("10"), "2"],
    [1, 300, "pin.show", Decimal("10"), "3"],
]

normalized_events = [
    # group 1
    [1, 40, "pin.openSite", None, "unexpected0"],
    [1, 100, "pin.show", None, "1"],
    [1, 130, "pin.click", None, "1"],
    [1, 140, "pin.openSite", None, "1"],
    # group 2
    [1, 200, "pin.show", None, "2"],
    [1, 210, "pin.click", None, "2"],
    [1, 220, "pin.click", None, "unexpected1"],
    [1, 240, "pin.openSite", None, "2"],
    # group 3
    [1, 300, "pin.show", None, "3"],
    [1, 330, "pin.tap", None, "3"],
    [1, 400, "pin.tap", None, "unexpected2"],
    [1, 410, "pin.tap", None, "unexpected3"],
    [1, 430, "pin.call", None, "3"],
]


@pytest.fixture
def setup_table(ch_client):
    def _setup(table: str, events: list):
        events = [make_event(*e) for e in events]
        ch_client.execute(f"insert into {table} values", events)

    return _setup


@pytest.fixture(autouse=True)
def setup_tables(setup_table):
    setup_table("stat.accepted_sample", charged_events)
    setup_table("stat.normalized_sample", normalized_events)


@pytest.fixture
def mock_find_new_task(mocker):
    return mocker.patch(
        "maps_adv.stat_controller.client.lib.collector.Client.find_new_task",
        new_callable=coro_mock,
    ).coro


@pytest.fixture
def mock_pipeline(mocker, mock_find_new_task):
    mock_find_new_task.side_effect = [
        {"id": 1, "timing_from": dt(100), "timing_to": dt(199)},
        asyncio.sleep(0.1),
        {"id": 2, "timing_from": dt(200), "timing_to": dt(300)},
    ]
    mocker.patch(
        "maps_adv.stat_controller.client.lib.collector.Client.update_task",
        new_callable=coro_mock,
    )


@pytest.fixture
def config(config):
    with patch.dict(
        config.options_map,
        {
            "TASKS_TO_START": {"default": ["Collector"]},
            "RELAUNCH_INTERVAL": {"default": 0.2},
        },
    ):
        config.init()
        yield config


@pytest.fixture
async def run_server(mock_pipeline, app, aiohttp_server):
    await aiohttp_server(app.api)


async def test_immediately_starts_data_transferring(ch_client):
    await asyncio.sleep(0.1)

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.accepted_sample")
    expected = {
        make_event(*a)
        for a in charged_events
        + [
            [1, 130, "pin.click", Decimal("0"), "1"],
            [1, 140, "pin.openSite", Decimal("0"), "1"],
        ]
    }
    assert len(existing_data_in_db) == len(expected)
    assert set(existing_data_in_db) == expected


async def test_will_periodically_run(ch_client):
    await asyncio.sleep(0.4)  # wait for two runs

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.accepted_sample")
    expected = {
        make_event(*a)
        for a in charged_events
        + [
            [1, 130, "pin.click", Decimal("0"), "1"],
            [1, 140, "pin.openSite", Decimal("0"), "1"],
            [1, 210, "pin.click", Decimal("0"), "2"],
            [1, 240, "pin.openSite", Decimal("0"), "2"],
        ]
    }
    assert set(existing_data_in_db) == expected


async def test_waits_pause_if_crashed(ch_client, mock_find_new_task):
    mock_find_new_task.side_effect = [
        {"id": 1, "timing_from": dt(100), "timing_to": dt(199)},
        Exception(),
        {"id": 2, "timing_from": dt(200), "timing_to": dt(300)},
    ]

    await asyncio.sleep(0.1)  # still wait relaunch time

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.accepted_sample")
    expected = {
        make_event(*a)
        for a in charged_events
        + [
            [1, 130, "pin.click", Decimal("0"), "1"],
            [1, 140, "pin.openSite", Decimal("0"), "1"],
        ]
    }
    assert set(existing_data_in_db) == expected


async def test_continues_after_pause_if_crashed(ch_client, mock_find_new_task):
    mock_find_new_task.side_effect = [
        {"id": 1, "timing_from": dt(100), "timing_to": dt(199)},
        Exception(),
        {"id": 2, "timing_from": dt(200), "timing_to": dt(300)},
    ]

    await asyncio.sleep(0.25)  # after relaunch pause

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.accepted_sample")
    expected = {
        make_event(*a)
        for a in charged_events
        + [
            [1, 130, "pin.click", Decimal("0"), "1"],
            [1, 140, "pin.openSite", Decimal("0"), "1"],
            [1, 210, "pin.click", Decimal("0"), "2"],
            [1, 240, "pin.openSite", Decimal("0"), "2"],
        ]
    }
    assert set(existing_data_in_db) == expected


async def test_ignores_conflicts(ch_client, mock_find_new_task):
    mock_find_new_task.side_effect = [
        {"id": 1, "timing_from": dt(100), "timing_to": dt(199)},
        Conflict(),
        {"id": 2, "timing_from": dt(200), "timing_to": dt(300)},
    ]

    await asyncio.sleep(0.1)  # don't wait relaunch time for Conflicts

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.accepted_sample")
    expected = {
        make_event(*a)
        for a in charged_events
        + [
            [1, 130, "pin.click", Decimal("0"), "1"],
            [1, 140, "pin.openSite", Decimal("0"), "1"],
            [1, 210, "pin.click", Decimal("0"), "2"],
            [1, 240, "pin.openSite", Decimal("0"), "2"],
        ]
    }
    assert set(existing_data_in_db) == expected

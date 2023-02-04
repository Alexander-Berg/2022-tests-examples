from decimal import Decimal

import pytest

from maps_adv.stat_controller.client.lib.collector import TaskStatus
from maps_adv.stat_tasks_starter.lib.collector.pipeline import Pipeline
from maps_adv.stat_tasks_starter.tests.tools import coro_mock, dt, make_event

pytestmark = [pytest.mark.asyncio]


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

expected_events = charged_events + [
    [1, 140, "pin.openSite", Decimal("0"), "1"],
    [1, 210, "pin.click", Decimal("0"), "2"],
    [1, 240, "pin.openSite", Decimal("0"), "2"],
    [1, 330, "pin.tap", Decimal("0"), "3"],
]


@pytest.fixture
def client(loop):
    class ControllerClient:
        find_new_task = coro_mock()
        update_task = coro_mock()

    return ControllerClient()


@pytest.fixture
def pipeline(client):
    return Pipeline("pipeline0", client)


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


async def test_will_transfer_events(ch_client, client, pipeline):
    client.find_new_task.coro.return_value = {
        "timing_from": dt(140),
        "timing_to": dt(400),
        "id": 1,
    }

    await pipeline()

    in_db = ch_client.execute("SELECT * FROM stat.accepted_sample")
    assert set(in_db) == {make_event(*a) for a in expected_events}


async def test_will_notify_controller_about_task_completion(client, pipeline):
    client.find_new_task.coro.return_value = {
        "timing_from": dt(140),
        "timing_to": dt(400),
        "id": 1,
    }

    await pipeline()
    client.update_task.assert_called_with(
        executor_id="pipeline0", task_id=1, status=TaskStatus.completed
    )


async def test_not_notified_if_collector_fails(client, pipeline, mocker):
    mocker.patch(
        "maps_adv.stat_tasks_starter.lib.collector" ".collector.Collector.__call__",
        side_effect=ValueError(),
    )
    client.find_new_task.coro.return_value = {
        "timing_from": dt(140),
        "timing_to": dt(400),
        "id": 1,
    }

    with pytest.raises(ValueError):
        await pipeline()

    assert client.update_task.called is False

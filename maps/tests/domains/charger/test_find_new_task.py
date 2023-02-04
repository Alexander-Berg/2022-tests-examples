import pytest

from maps_adv.stat_controller.server.lib.domains import (
    Charger,
    ChargerStatus,
    NormalizerStatus,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]

_not_important_statuses = [
    ChargerStatus.accepted,
    ChargerStatus.context_received,
    ChargerStatus.calculation_completed,
]

_important_statuses = [ChargerStatus.billing_notified, ChargerStatus.charged_data_sent]


@pytest.fixture
def domain(charger_dm):
    return Charger(charger_dm)


@pytest.fixture
async def normalized(factory):
    return await factory.normalizer(
        "executor0", dt(0), dt(60), NormalizerStatus.completed
    )


async def test_returns_nothing_if_there_is_no_tasks(domain):
    assert await domain.find_new("executor0") is None


async def test_returns_task_updated_from_normalized(normalized, domain):
    got = await domain.find_new("executor0")

    assert got["id"] == normalized


async def test_does_not_use_not_normalized_tasks(factory, domain):
    await factory.normalizer("executor0", dt(100), dt(160))
    await factory.collector("executor1", dt(200), dt(260))

    got = await domain.find_new("executor1")

    assert got is None


@pytest.mark.parametrize("status", [st for st in ChargerStatus])
async def test_returns_nothing_if_only_not_failed_charger_tasks_available(
    status, domain, factory
):
    await factory.charger("executor0", dt(100), dt(160), status)

    got = await domain.find_new("executor1")

    assert got is None


async def test_uses_oldest_normalized_if_there_are_many(normalized, domain, factory):
    await factory.normalizer("executor1", dt(100), dt(160), NormalizerStatus.completed)

    got = await domain.find_new("executor3")

    assert got["id"] == normalized


@pytest.mark.parametrize(
    "status", [st for st in ChargerStatus if st != ChargerStatus.completed]
)
async def test_do_not_give_tasks_if_charging_in_progress(status, domain, factory):
    await factory.charger("executor1", dt(0), dt(60), status)
    await factory.normalizer("executor0", dt(100), dt(160), NormalizerStatus.completed)

    got = await domain.find_new("executor3")

    assert got is None


async def test_updates_status_to_accepted_by_charger(normalized, domain, factory):
    got = await domain.find_new("executor1")

    details = await factory.charger.details(got["id"])
    assert details["status"] == ChargerStatus.accepted


async def test_returns_task_data_for_normalized_task(normalized, domain):
    got = await domain.find_new("executor1")

    assert got == {
        "id": normalized,
        "timing_from": dt(0),
        "timing_to": dt(60),
        "status": ChargerStatus.accepted,
    }


@pytest.mark.parametrize(
    "status", [st for st in ChargerStatus if st != ChargerStatus.completed]
)
async def test_returns_reanimated_if_it_is_oldest(status, domain, factory):
    task_id = await factory.charger("executor0", dt(100), dt(160), status, failed=True)
    await factory.normalizer("executor1", dt(200), dt(260), NormalizerStatus.completed)

    got = await domain.find_new("executor2")

    assert got["id"] == task_id


@pytest.mark.parametrize(
    "status", [st for st in ChargerStatus if st != ChargerStatus.completed]
)
async def test_returns_normalized_if_it_is_oldest(status, domain, factory):
    task_id = await factory.normalizer(
        "executor1", dt(100), dt(160), NormalizerStatus.completed
    )
    await factory.charger("executor0", dt(200), dt(260), status, failed=True)

    got = await domain.find_new("executor2")

    assert got["id"] == task_id


@pytest.mark.parametrize(
    "state", [None, "some_state", ["a", {"int": 10, "decimal-like": "100.35"}]]
)
@pytest.mark.parametrize("status", _important_statuses)
async def test_does_not_update_data_for_important_status(
    status, state, domain, factory
):
    task_id = await factory.charger(
        "executor0", dt(100), dt(160), status=status, failed=True, state=state
    )

    await domain.find_new("executor1")

    current_log_id = await factory.cur_log_id(task_id)
    details = await factory.charger.details(task_id)

    assert details == {
        "current_log_id": current_log_id,
        "executor_id": "executor1",
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "status": status,
        "execution_state": state,
    }


@pytest.mark.parametrize(
    "state", [None, "some_state", ["a", {"int": 10, "decimal-like": "100.35"}]]
)
@pytest.mark.parametrize("status", _not_important_statuses)
async def test_does_not_updates_data_for_not_important_status(
    status, state, domain, factory
):
    task_id = await factory.charger(
        "executor0", dt(100), dt(160), status=status, failed=True, state=state
    )

    await domain.find_new("executor1")

    current_log_id = await factory.cur_log_id(task_id)
    details = await factory.charger.details(task_id)

    assert details == {
        "current_log_id": current_log_id,
        "executor_id": "executor1",
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "status": ChargerStatus.accepted,
        "execution_state": None,
    }


@pytest.mark.parametrize("status", _important_statuses)
async def test_returns_reanimated_task_data_important_status(status, domain, factory):
    task_id = await factory.charger(
        "executor0",
        dt(100),
        dt(160),
        status,
        failed=True,
        state=["a", {"int": 10, "decimal-like": "100.35"}],
    )

    got = await domain.find_new("executor1")

    assert got == {
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "status": status,
        "execution_state": ["a", {"int": 10, "decimal-like": "100.35"}],
    }


@pytest.mark.parametrize("status", _not_important_statuses)
async def test_returns_reanimated_task_data_not_important_status(
    status, domain, factory
):
    task_id = await factory.charger(
        "executor0",
        dt(100),
        dt(160),
        status,
        failed=True,
        state=["a", {"int": 10, "decimal-like": "100.35"}],
    )

    got = await domain.find_new("executor1")

    assert got == {
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "status": ChargerStatus.accepted,
    }

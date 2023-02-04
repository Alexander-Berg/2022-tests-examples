import pytest

from maps_adv.stat_controller.server.lib.data_managers import (
    ChargerStatus,
    NormalizerStatus,
)
from maps_adv.stat_controller.server.lib.db import DbTaskStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


_status_map_list = (
    (ChargerStatus.accepted, DbTaskStatus.accepted_by_charger),
    (ChargerStatus.context_received, DbTaskStatus.charger_received_context),
    (ChargerStatus.calculation_completed, DbTaskStatus.charger_completed_calculation),
    (ChargerStatus.billing_notified, DbTaskStatus.charger_notified_billing),
    (ChargerStatus.charged_data_sent, DbTaskStatus.charger_sent_charged_data),
    (ChargerStatus.completed, DbTaskStatus.charged),
)


@pytest.mark.parametrize("status", list(NormalizerStatus))
@pytest.mark.parametrize("target, db_status", _status_map_list)
async def test_will_update_from_any_normalizer_status(
    status, db_status, target, charger_dm, factory, con
):
    task_id = await factory.normalizer("executor0", dt(100), dt(160), status)

    await charger_dm.update("executor1", task_id, target)

    sql = (
        "SELECT EXISTS(SELECT tasks.id "
        "FROM tasks JOIN tasks_log ON tasks.current_log_id = tasks_log.id "
        "WHERE tasks.id = $1 AND tasks.status = $2 AND tasks_log.status = $2)"
    )
    assert await con.fetchval(sql, task_id, db_status) is True


@pytest.mark.parametrize("status", list(ChargerStatus))
@pytest.mark.parametrize("target, db_status", _status_map_list)
async def test_will_update_from_any_charger_status(
    status, target, db_status, charger_dm, factory, con
):
    task_id = await factory.charger("executor0", dt(100), dt(160), status)

    await charger_dm.update("executor1", task_id, target)

    sql = (
        "SELECT EXISTS(SELECT tasks.id "
        "FROM tasks JOIN tasks_log ON tasks.current_log_id = tasks_log.id "
        "WHERE tasks.id = $1 AND tasks.status = $2 AND tasks_log.status = $2)"
    )
    assert await con.fetchval(sql, task_id, db_status) is True


@pytest.mark.parametrize(
    "current, target",
    [(None, "some_kek"), ("some_kek", "some_makarek"), ("some_makarek", None)],
)
async def test_will_update_execution_state(current, target, factory, charger_dm, con):
    task_id = await factory.charger(
        "executor0", dt(100), dt(160), ChargerStatus.context_received, current
    )

    await charger_dm.update(
        "executor1", task_id, ChargerStatus.calculation_completed, target
    )

    sql = (
        "SELECT tasks_log.execution_state "
        "FROM tasks JOIN tasks_log ON tasks.current_log_id = tasks_log.id "
        "WHERE tasks.id = $1"
    )
    assert await con.fetchval(sql, task_id) == target


async def test_returns_nothing(factory, charger_dm):
    task_id = await factory.normalizer("executor0", dt(100), dt(160))

    got = await charger_dm.update(
        "executor1", task_id, ChargerStatus.accepted, "some_state"
    )

    assert got is None

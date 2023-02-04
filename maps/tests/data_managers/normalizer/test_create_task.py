from datetime import datetime, timezone

import pytest
from asyncpg.exceptions import CheckViolationError

from maps_adv.stat_controller.server.lib.data_managers import UnexpectedNaiveDateTime
from maps_adv.stat_controller.server.lib.db import DbTaskStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "data",
    (
        {"timing_from": dt(100), "timing_to": dt(160)},
        {"timing_from": dt(100), "timing_to": dt(160)},
    ),
)
async def test_task_successfully_created(data, normalizer_dm, con):
    task_id = await normalizer_dm.create(executor_id="lolkek", **data)

    exists = await con.fetchval(
        "SELECT EXISTS(SELECT id FROM tasks WHERE id = $1)", task_id
    )
    assert exists is True


@pytest.mark.parametrize(
    "data",
    (
        {
            "timing_from": datetime(2019, 5, 6, 12, 25, 0, 0),
            "timing_to": datetime(2019, 5, 6, 12, 30, 0, 0, timezone.utc),
        },
        {
            "timing_from": datetime(2019, 5, 6, 12, 25, 0, 0, timezone.utc),
            "timing_to": datetime(2019, 5, 6, 12, 30, 0, 0),
        },
        {
            "timing_from": datetime(2019, 5, 6, 12, 25, 0, 0),
            "timing_to": datetime(2019, 5, 6, 12, 30, 0, 0),
        },
    ),
)
async def test_raises_for_naive_dt(data, normalizer_dm):
    with pytest.raises(UnexpectedNaiveDateTime):
        await normalizer_dm.create(executor_id="lolkek", **data)


@pytest.mark.parametrize(
    "data",
    (
        {"timing_from": dt(101), "timing_to": dt(100)},
        {"timing_from": dt(100), "timing_to": dt(100)},
    ),
)
async def test_raises_if_timing_from_gte_timing_to(data, normalizer_dm):
    with pytest.raises(CheckViolationError):
        await normalizer_dm.create(executor_id="lolkek", **data)


async def test_will_create_record_in_log(normalizer_dm, con):
    task_id = await normalizer_dm.create("lolkek", dt(100), dt(160))

    sql = (
        "SELECT EXISTS(SELECT id FROM tasks_log "
        "WHERE task_id = $1 AND executor_id = 'lolkek' AND status = $2)"
    )
    exists = await con.fetchval(sql, task_id, DbTaskStatus.accepted_by_normalizer)
    assert exists is True


async def test_created_task_references_to_log(normalizer_dm, con):
    task_id = await normalizer_dm.create("lolkek", dt(100), dt(160))

    sql = (
        "SELECT EXISTS(SELECT tasks.id "
        "FROM tasks JOIN tasks_log ON tasks.current_log_id = tasks_log.id "
        "WHERE tasks.id = $1 AND tasks_log.status = $2)"
    )
    exists = await con.fetchval(sql, task_id, DbTaskStatus.accepted_by_normalizer)
    assert exists is True


async def test_created_task_has_denormalized_status(normalizer_dm, con):
    task_id = await normalizer_dm.create("lolkek", dt(100), dt(160))

    sql = (
        "SELECT EXISTS(SELECT tasks.id FROM tasks "
        "WHERE tasks.id = $1 AND tasks.status = $2)"
    )
    exists = await con.fetchval(sql, task_id, DbTaskStatus.accepted_by_normalizer)
    assert exists is True


async def test_does_not_created_if_update_fails(mocker, normalizer_dm, con):
    mocker.patch(
        "maps_adv.stat_controller.server.lib."
        "data_managers.normalizer.TaskManager._update",
        side_effect=ValueError(),
    )

    with pytest.raises(ValueError):
        await normalizer_dm.create("lolkek", dt(100), dt(160))

    assert await con.fetchval("SELECT EXISTS(SELECT id FROM tasks)") is False

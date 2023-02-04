from datetime import datetime, timedelta, timezone

import pytest
from asyncpg.exceptions import (
    CheckViolationError,
    ForeignKeyViolationError,
    NotNullViolationError,
    UniqueViolationError,
)

from maps_adv.stat_controller.server.tests.tools import Around, dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "tz", (timezone(-timedelta(hours=7)), timezone.utc, timezone(timedelta(hours=7)))
)
async def test_returns_aware_datetime(tz, con):
    await con.execute(
        "INSERT INTO tasks (timing_from, timing_to ) VALUES ($1, $2)",
        dt(100, tz),
        dt(160, tz),
    )
    row = await con.fetchrow("SELECT * FROM tasks LIMIT 1")

    assert row["timing_from"].tzinfo
    assert row["timing_to"].tzinfo


@pytest.mark.parametrize(
    "tz", (timezone(-timedelta(hours=7)), timezone.utc, timezone(timedelta(hours=7)))
)
async def test_timestamp_returns_correct_results(tz, con):
    await con.execute(
        "INSERT INTO tasks (timing_from, timing_to ) VALUES ($1, $2)", dt(100), dt(200)
    )
    row = await con.fetchrow("SELECT * FROM tasks LIMIT 1")

    assert row["timing_from"].timestamp() == 100.0
    assert row["timing_to"].timestamp() == 200.0


async def test_created_successfully(con):
    await con.execute(
        "INSERT INTO tasks(created, timing_from, timing_to) " "VALUES($1, $2, $3)",
        dt(100),
        dt(90),
        dt(160),
    )
    row = await con.fetchrow("SELECT * FROM tasks LIMIT 1")

    assert row["created"] == dt(100)
    assert row["current_log_id"] is None
    assert row["timing_from"] == dt(90)
    assert row["timing_to"] == dt(160)


@pytest.mark.parametrize(
    "sql",
    (
        "INSERT INTO tasks (timing_from) VALUES ('2019-04-30 17:02-10')",
        "INSERT INTO tasks (timing_to) VALUES ('2019-04-30 17:02-10')",
    ),
)
async def test_required_columns(sql, con):
    with pytest.raises(NotNullViolationError):
        await con.execute(sql)


@pytest.mark.parametrize(
    "args",
    ((None, dt(100), dt(200)), (dt(120), None, dt(200)), (dt(120), dt(100), None)),
)
async def test_all_columns_are_non_nullable(args, con):
    sql = "INSERT INTO tasks(created, timing_from, timing_to) " "VALUES ($1, $2, $3)"
    with pytest.raises(NotNullViolationError):
        await con.execute(sql, *args)


async def test_created_column_will_set_to_now_by_default(con):
    await con.execute(
        "INSERT INTO tasks (timing_from, timing_to) "
        "VALUES ('2019-04-30 05:00-00', '2019-04-30 10:00-00')"
    )

    result = await con.fetchval("SELECT created FROM tasks LIMIT 1")
    assert result in Around(datetime.now(timezone.utc), timedelta(seconds=1))


async def test_current_log_id_column_is_nullable(con):
    await con.execute(
        "INSERT INTO tasks (timing_from, timing_to) "
        "VALUES ('2019-04-30 05:00', '2019-04-30 10:00')"
    )

    result = await con.fetchval("SELECT current_log_id FROM tasks LIMIT 1")
    assert result is None


async def test_current_log_id_is_foreign_key(con):
    task_id = await con.fetchval(
        "INSERT INTO tasks (timing_from, timing_to) "
        "VALUES ('2019-04-30 05:00', '2019-04-30 10:00')"
        "RETURNING id"
    )
    task_log_id = await con.fetchval(
        "INSERT INTO tasks_log (task_id, executor_id, status) "
        "VALUES ($1, 'lolkek', 'accepted_by_normalizer') RETURNING id",
        task_id,
    )
    await con.execute(
        "UPDATE tasks SET current_log_id = $1 WHERE id = $2", task_log_id, task_id
    )

    with pytest.raises(ForeignKeyViolationError):
        await con.execute("DELETE FROM tasks_log WHERE id = $1", task_log_id)


@pytest.mark.parametrize("timing_from", (dt(100), dt(101)))
async def test_raises_if_timing_from_gte_timing_to(timing_from, con):
    sql = "INSERT INTO tasks (timing_from, timing_to) VALUES ($1, $2)"

    with pytest.raises(CheckViolationError):
        await con.execute(sql, timing_from, dt(100))


async def test_ok_if_timing_from_lt_timing_to(con):
    await con.execute(
        "INSERT INTO tasks (timing_from, timing_to) "
        "VALUES ('2019-04-30 09:59', '2019-04-30 10:00')"
    )


async def test_raises_if_timing_from_duplicates(con):
    with pytest.raises(UniqueViolationError):
        await con.execute(
            "INSERT INTO tasks (timing_from, timing_to) "
            "VALUES "
            "('2019-04-30 05:00', '2019-04-30 10:00'), "
            "('2019-04-30 05:00', '2019-04-30 11:00')"
        )


async def test_raises_if_timing_to_duplicates(con):
    with pytest.raises(UniqueViolationError):
        await con.execute(
            "INSERT INTO tasks (timing_from, timing_to) "
            "VALUES "
            "('2019-04-30 05:00', '2019-04-30 10:00'), "
            "('2019-04-30 06:00', '2019-04-30 10:00')"
        )

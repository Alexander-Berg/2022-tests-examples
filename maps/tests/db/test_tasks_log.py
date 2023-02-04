from datetime import datetime, timedelta, timezone

import pytest
from asyncpg.exceptions import NotNullViolationError, UniqueViolationError

from maps_adv.stat_controller.server.lib.db.enums import TaskStatus
from maps_adv.stat_controller.server.tests.tools import Around, dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
async def task_id(con):
    return await con.fetchval(
        "INSERT INTO tasks (id, timing_from, timing_to)"
        "VALUES (100500, '2019-04-30 16:00', '2019-04-30 16:05')"
        "RETURNING id"
    )


async def test_created_successfully(con, task_id):
    sql = (
        "INSERT INTO tasks_log ("
        "task_id ,created, executor_id, status, execution_state"
        ") VALUES ($1, $2, $3, $4, $5)"
    )
    state = [
        {
            "order_id": 567382,
            "budget_balance": "100",
            "campaigns": [
                {
                    "campaign_id": 4242,
                    "cpm": "3",
                    "budget": "200",
                    "daily_budget": "20",
                    "charged": "8",
                    "charged_daily": "4",
                    "events_count": 2,
                }
            ],
        }
    ]

    await con.execute(
        sql, task_id, dt(100), "lolkek", TaskStatus.accepted_by_normalizer, state
    )

    row = await con.fetchrow("SELECT * FROM tasks_log LIMIT 1")

    assert row["task_id"] == task_id
    assert row["created"] == dt(100)
    assert row["executor_id"] == "lolkek"
    assert row["status"] == TaskStatus.accepted_by_normalizer
    assert row["execution_state"] == [
        {
            "order_id": 567382,
            "budget_balance": "100",
            "campaigns": [
                {
                    "campaign_id": 4242,
                    "cpm": "3",
                    "budget": "200",
                    "daily_budget": "20",
                    "charged": "8",
                    "charged_daily": "4",
                    "events_count": 2,
                }
            ],
        }
    ]


async def test_exectution_state_is_not_required(con, task_id):
    await con.execute(
        "INSERT INTO tasks_log (task_id, created, executor_id, status)"
        "VALUES ($1, $2, $3, $4)",
        task_id,
        dt(100),
        "lolkek",
        TaskStatus.accepted_by_normalizer,
    )

    row = await con.fetchrow("SELECT * from tasks_log LIMIT 1")

    assert row["task_id"] == task_id
    assert row["created"] == dt(100)
    assert row["executor_id"] == "lolkek"
    assert row["status"] == TaskStatus.accepted_by_normalizer
    assert row["execution_state"] is None


@pytest.mark.parametrize(
    "sql",
    (
        "INSERT INTO tasks_log (task_id, executor_id) " "VALUES (100500, 'lol')",
        "INSERT INTO tasks_log (task_id, status) "
        "VALUES (100500, 'accepted_by_normalizer')",
        "INSERT INTO tasks_log (executor_id, status) "
        "VALUES ('lol', 'accepted_by_normalizer')",
    ),
)
async def test_required_columns(sql, con):
    with pytest.raises(NotNullViolationError):
        await con.execute(sql)


@pytest.mark.parametrize(
    "args",
    (
        (100500, dt(100), "lolkek", None),
        (100500, dt(100), None, TaskStatus.accepted_by_normalizer),
        (100500, None, "lolkek", TaskStatus.accepted_by_normalizer),
        (None, dt(100), "lolkek", TaskStatus.accepted_by_normalizer),
    ),
)
async def test_all_columns_except_execution_state_are_non_nullable(args, con):
    sql = (
        "INSERT INTO tasks_log(task_id, created, executor_id, status) "
        "VALUES ($1, $2, $3, $4)"
    )
    with pytest.raises(NotNullViolationError):
        await con.execute(sql, *args)


async def test_created_column_will_set_to_now_by_default(con):
    await con.execute(
        "INSERT INTO tasks_log (task_id, executor_id, status) "
        "VALUES (100500, 'lolkek', 'accepted_by_normalizer')"
    )

    result = await con.fetchval("SELECT created FROM tasks_log LIMIT 1")
    assert result in Around(datetime.now(timezone.utc), timedelta(seconds=1))


async def test_raises_if_executor_id_and_status_duplicates(con):
    with pytest.raises(UniqueViolationError):
        await con.execute(
            "INSERT INTO tasks_log "
            "(task_id, created, executor_id, status)"
            "VALUES "
            "(100500, '2019-04-30 19:35', 'lolkek', 'accepted_by_normalizer'),"
            "(100500, '2019-04-30 19:55', 'lolkek', 'accepted_by_normalizer')"
        )

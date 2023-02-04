import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time(dt("2020-01-01 16:00:00"))]


@pytest.mark.parametrize("pending_count", [0, 1, 2])
async def test_returns_pending_tasks_count(factory, dm, pending_count):
    for idx in range(pending_count):
        await factory.create_order(
            yang_suite_id=str(idx),
            yang_task_created_at=dt("2020-01-01 00:00:00"),
            task_result_got_at=None,
        )

    got = await dm.fetch_pending_yang_tasks_count()

    assert got == pending_count


@pytest.mark.parametrize(
    "order_params",
    [
        dict(yang_task_created_at=None, task_result_got_at=dt("2020-01-01 00:00:00")),
        dict(
            yang_task_created_at=dt("2020-01-01 00:00:00"),
            task_result_got_at=dt("2020-01-01 00:00:00"),
        ),
        dict(yang_task_created_at=None, task_result_got_at=None),
    ],
)
async def test_ignores_not_pending_tasks(factory, dm, order_params):
    await factory.create_order(**order_params)

    got = await dm.fetch_pending_yang_tasks_count()

    assert got == 0

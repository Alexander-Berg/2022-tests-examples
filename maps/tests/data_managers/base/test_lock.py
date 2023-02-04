import pytest

from maps_adv.stat_controller.server.lib.data_managers.base import (
    ConflictOperation,
    lock,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


@pytest.mark.parametrize("task_name", ["normalizer", "charger", "collector"])
async def test_raises_for_second_lock(task_name, db):
    async with lock(db, task_name):

        with pytest.raises(ConflictOperation, match=task_name):
            async with lock(db, task_name):
                pass


@pytest.mark.parametrize(
    "name0, name1",
    (["normalizer", "charger"], ["charger", "collector"], ["collector", "normalizer"]),
)
async def test_did_not_raise_for_different_types(name0, name1, db):
    async with lock(db, name0):
        async with lock(db, name1):
            pass


async def test_lock_unexistent_type_name(db):
    async with lock(db, "unexistent"):
        pass

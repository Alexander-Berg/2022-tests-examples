from unittest.mock import patch

import pytest
from asyncpg import LockNotAvailableError, PostgresSyntaxError

from maps_adv.warden.server.lib.data_managers.tasks import ConflictOperation

pytestmark = [pytest.mark.asyncio]


@pytest.mark.real_db
@pytest.mark.usefixtures("type_id")
async def test_raises_for_second_lock(dm):
    async with dm.lock("task_type"):

        with pytest.raises(ConflictOperation):
            async with dm.lock("task_type"):
                pass


@pytest.mark.real_db
@pytest.mark.usefixtures("type_id")
async def test_did_not_raise_for_different_types(dm):
    async with dm.lock("task_type"):

        async with dm.lock("some"):
            pass


@pytest.mark.real_db
async def test_lock_unexistent_type_name(dm):
    async with dm.lock("task_type"):
        pass


@pytest.mark.parametrize("exc_cls", (LockNotAvailableError, Exception))
async def test_releases_connection(exc_cls, dm):
    with patch("maps_adv.warden.server.lib.db.engine.DB.release") as release:
        with patch("asyncpg.connection.Connection.execute", side_effect=exc_cls()):

            with pytest.raises(Exception):
                async with dm.lock("task_type"):
                    pass

            assert release.called


@pytest.mark.parametrize("exc_cls", (PostgresSyntaxError, Exception))
async def test_raises_for_not_expected_exception(dm, exc_cls):
    with patch("asyncpg.connection.Connection.execute", side_effect=exc_cls("kek")):
        with pytest.raises(exc_cls, match="kek"):
            async with dm.lock("task_type"):
                pass

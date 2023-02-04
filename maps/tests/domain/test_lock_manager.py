from unittest.mock import AsyncMock, MagicMock

import pytest
from aioredis_lock import LockTimeoutError
from smb.common.testing_utils import Any

from maps_adv.geosmb.harmonist.server.lib.domain.lock_manager import LockManager

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def redis_mock(config):
    return AsyncMock()


@pytest.fixture
def lock_manager(redis_mock):
    return LockManager(redis_mock)


@pytest.fixture
def aioredis_lock_mock():
    return MagicMock()


@pytest.fixture(autouse=True)
def redis_lock_constructor_mock(mocker, aioredis_lock_mock):
    return mocker.patch(
        "maps_adv.geosmb.harmonist.server.lib.domain.lock_manager.RedisLock",
        return_value=aioredis_lock_mock,
    )


async def test_uses_redis_lock(
    redis_lock_constructor_mock, aioredis_lock_mock, redis_mock, lock_manager
):
    async with lock_manager.try_lock_creation_entry("some_session"):
        redis_lock_constructor_mock.assert_called_with(
            redis_mock,
            key="session_some_session",
            timeout=Any(int),
            wait_timeout=0,
        )
        aioredis_lock_mock.__aenter__.assert_awaited_once()
        aioredis_lock_mock.__aexit__.assert_not_called()

    aioredis_lock_mock.__aexit__.assert_awaited_once()


async def test_returns_true_if_lock_acquired(lock_manager):
    async with lock_manager.try_lock_creation_entry("some_session") as success:
        assert success is True


async def test_return_false_if_lock_not_acquired(aioredis_lock_mock, lock_manager):
    aioredis_lock_mock.__aenter__.side_effect = LockTimeoutError

    async with lock_manager.try_lock_creation_entry("some_session") as success:
        assert success is False

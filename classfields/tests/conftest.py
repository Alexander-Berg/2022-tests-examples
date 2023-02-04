import asyncio
import os

import pytest
from tortoise.contrib.test import finalizer, initializer

from app.core import cur_tz
from app.core.config import settings


@pytest.fixture(scope="session", autouse=True)
def event_loop():
    """Create an instance of the default event loop for each test case."""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(autouse=True)
def patch_modules(monkeypatch, mocker):
    import aioredis
    import fakeredis.aioredis
    import redis

    monkeypatch.setattr(redis, "Redis", fakeredis.FakeRedis)
    monkeypatch.setattr(aioredis, "Redis", fakeredis.aioredis.FakeRedis)


@pytest.fixture(scope="module", autouse=True)
def database(request, event_loop):
    initializer(
        ["app.db.models"],
        db_url=f"postgres://{settings.POSTGRES_USER}:{settings.POSTGRES_PASSWORD}@"
        f"{settings.POSTGRES_SERVER}:{settings.POSTGRES_PORT}/{settings.POSTGRES_DB}?"
        f"minsize={settings.POSTGRES_POOL_MIN_SIZE}&maxsize={settings.POSTGRES_POOL_MAX_SIZE}",
        app_label="models",
        loop=event_loop,
    )
    os.environ["TIMEZONE"] = cur_tz.zone
    request.addfinalizer(finalizer)

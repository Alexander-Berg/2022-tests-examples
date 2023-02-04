import asyncio

import pytest


@pytest.fixture(scope="session", autouse=True)
def event_loop():
    """Create an instance of the default event loop for each test case."""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(autouse=True)
def patch_modules(monkeypatch, mocker):
    import redis
    from fakeredis.aioredis import FakeRedis

    monkeypatch.setattr(redis, "Redis", FakeRedis)

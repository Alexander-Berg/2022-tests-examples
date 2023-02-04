import pytest

from tasha.core.dbproxy import DBProxy


@pytest.fixture
async def db_proxy():
    db_proxy = await DBProxy.create()
    yield db_proxy
    await db_proxy.close()

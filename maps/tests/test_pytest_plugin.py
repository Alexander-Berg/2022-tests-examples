import pytest

from maps_adv.common.pg_engine import DB

pytestmark = [pytest.mark.asyncio]


class UserDb(DB):
    pass


@pytest.fixture
def pg_engine_db_cls():
    return UserDb


async def test_database_url_returns_actual_value(pg_engine_database_url):
    assert pg_engine_database_url == "postgresql://engine:engine@localhost:5433/engine"


async def test_alembic_ini_returns_actual_value(pg_engine_alembic_ini_path):
    assert pg_engine_alembic_ini_path == "tests/alembic.ini"


@pytest.mark.usefixtures("pg_engine_db_cls")
async def test_returns_user_defined_db_if_fixture_defined(db):
    assert isinstance(db, UserDb)


async def test_returns_default_db(db):
    assert not isinstance(db, UserDb)


async def test_database_is_automigrated(con):
    await con.fetchval(
        """
        SELECT EXISTS(
            SELECT *
            FROM alembic_version
            WHERE version_num = 'lolkekcheburek'
        )
        """
    ) is True


async def test_transactional_db_always_returns_the_same_con(db):
    async with db.acquire() as con_0:
        async with db.acquire() as con_1:
            assert con_0 == con_1


@pytest.mark.real_db
async def test_real_db_returns_new_con(db):
    async with db.acquire() as con_0:
        async with db.acquire() as con_1:
            assert con_0 != con_1

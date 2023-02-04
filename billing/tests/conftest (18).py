import os
from dataclasses import dataclass

import aiopg.sa
import pytest
from pgmigrate import get_config, migrate as pg_migrate
from yatest.common import source_path


@dataclass
class MigrateArgs:
    conn: str
    target: str
    base_dir: str


@pytest.fixture
def dsn():
    return os.environ['DB_DSN']


@pytest.fixture
def migrations_dir():
    return source_path('billing/yandex_pay/postgre')


@pytest.fixture
async def pool(dsn):
    async with aiopg.sa.create_engine(dsn) as pool:
        yield pool


@pytest.fixture
def migrate(dsn, migrations_dir):
    def _migrate(target):
        config = get_config(
            migrations_dir,
            MigrateArgs(
                conn=dsn,
                target=target,
                base_dir=migrations_dir,
            )
        )
        pg_migrate(config)

    return _migrate


@pytest.fixture(autouse=True)
async def drop_schema(pool):
    """
    To enforce clean state for each test
    """
    async with pool.acquire() as conn:
        await conn.execute('drop schema if exists yandex_pay cascade')
        yield
        await conn.execute('truncate public.schema_version')

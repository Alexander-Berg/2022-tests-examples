from maps.pylibs.local_postgres import Database
from maps.pylibs.local_postgres.utils import validate_pg_migrate_files

import pytest
import yatest
from typing import Final


MIGRATIONS_PATH: Final = yatest.common.source_path(
    "maps/bizdir/sps/migrations/migrations"
)


@pytest.fixture(scope="session")
def database() -> Database:
    return Database.create_instance()


def test_run_pg_migrate(database: Database) -> None:
    database.run_pg_migrate(MIGRATIONS_PATH)


def test_validate_pg_migrate_files() -> None:
    validate_pg_migrate_files(MIGRATIONS_PATH)

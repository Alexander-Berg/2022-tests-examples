"""
Functions related to test database creation
"""

from contextlib import contextmanager
from typing import Iterator
import yatest.common

from maps.pylibs.local_postgres import Database

MIGRATIONS_DIR = 'maps/b2bgeo/identity/db/migrations'


@contextmanager
def create_local_pg() -> Iterator[dict[str, str]]:
    postgres = Database().create_instance()
    postgres.run_pg_migrate(yatest.common.source_path(MIGRATIONS_DIR))
    pg_config = {
        'host': postgres.host,
        'port': postgres.port,
        'user': postgres.user,
        'password': postgres.password,
        'dbname': postgres.dbname,
    }
    yield pg_config

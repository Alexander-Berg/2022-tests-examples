import time
from operator import methodcaller

import clickhouse_driver
import pytest

from smb.common.multiruntime import lib as multiruntime


def sql_migrations_real(fixture_names):
    sql_fixtures = []

    for fixture_name in fixture_names:
        text = multiruntime.io.read_file(f"tests/fixtures/{fixture_name}")
        setup, teardown = map(
            methodcaller("strip"), text.split("-- BACKWARD --", maxsplit=1)
        )
        sql_fixtures.append(
            {
                "setup": list(map(methodcaller("strip"), setup.split(";\n"))),
                "teardown": list(map(methodcaller("strip"), teardown.split(";\n"))),
            }
        )

    return sql_fixtures


@pytest.fixture(scope="session")
def sql_migrations():
    fixture_names = [
        "db_stat.sql",
        "accepted_setup.sql",
        "db_sys.sql",
        "query_log.sql",
    ]
    return sql_migrations_real(fixture_names)


@pytest.fixture(scope="session")
def sql_migrations_for_monitorings():
    fixture_names = [
        "db_stat.sql",
        "db_sys.sql",
        "query_log.sql",
        "maps_adv_statistics_raw_metrika_log.sql",
        "mapkit_events.sql",
        "normalized_events.sql",
        "processed_events.sql",
    ]
    return sql_migrations_real(fixture_names)


@pytest.fixture(scope="session")
def ch():
    return clickhouse_driver.Client("localhost", 9001)


@pytest.fixture(scope="session")
def wait_for_ch(ch):
    waited = 0

    while waited < 100:
        try:
            ch.connection.connect()
        except clickhouse_driver.errors.NetworkError:
            time.sleep(1)
            waited += 1
        else:
            ch.disconnect()
            break
    else:
        raise ConnectionRefusedError()


_url = "http://localhost:8124"


def setup_ch_real(ch, sql_migrations):
    try:
        for query in sql_migrations:
            for sql in query["setup"]:
                ch.execute(sql)

        yield

    finally:
        for query in reversed(sql_migrations):
            for sql in query["teardown"]:
                ch.execute(sql)


@pytest.fixture(autouse=True)
def setup_ch(wait_for_ch, ch, sql_migrations, request):
    if "no_setup_ch" in request.keywords:
        yield
    else:
        yield from setup_ch_real(
            ch,
            sql_migrations,
        )


@pytest.fixture
def setup_ch_for_monitorings(wait_for_ch, ch, sql_migrations_for_monitorings):
    yield from setup_ch_real(
        ch,
        sql_migrations_for_monitorings,
    )

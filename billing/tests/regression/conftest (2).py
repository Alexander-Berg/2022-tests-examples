import os
import logging
from copy import deepcopy
from typing import Tuple, List

import pytest
import mock
import yaml
from yatest.common import source_path

from dwh.grocery.targets import OracleTableTarget, YTMapNodeTarget
from dwh.grocery.tools import TEST, create_engine, RW, peek
from . import truncate_table, drop_table, clean_export_changes
from .strategies import YTRecord, YTSchema, PYSchema, rows_with_yt_schema


logger = logging.getLogger(__name__)
workers = [1, 4]


@pytest.fixture(scope='session')
def test_config():
    conf_path = os.path.join(source_path('billing/dwh/src/dwh/conf/remote/etc/dwh/'), f'conf.{TEST}.yaml')
    with open(conf_path) as tf:
        test_conf = yaml.safe_load(tf)
    return test_conf


@pytest.fixture(autouse=True)
def patch_config():
    from dwh.grocery.tools import CONF
    with mock.patch.dict(CONF['LOGBROKER'], {'ENABLED': False}, clear=True):
        yield


@pytest.fixture(scope='session')
def arnold_yt_test_root():
    return make_root("arnold")


@pytest.fixture(scope='session')
def hahn_yt_test_root():
    return make_root("hahn")


@pytest.fixture(scope='session')
def freud_yt_test_root():
    return make_root('freud')


@pytest.fixture(
    scope='session',
    params=['hahn', None]
)
def yt_test_root(request):
    return make_root(request.param)


@pytest.fixture()
def fake_t_shipment():
    table = mimic_table("T_TEST_EXPORT_MANAGER", "T_SHIPMENT")
    yield table
    truncate_table(table)
    clean_export_changes(table)


@pytest.fixture()
def fake_t_product():
    table = mimic_table('t_fake_t_product'.upper(), 't_product'.upper())
    yield table
    truncate_table(table)
    clean_export_changes(table)


@pytest.fixture()
def fake_t_payment():
    """
    Таблица, дифф которой выгружается в 2 запроса
    """
    table = mimic_table('t_fake_t_payment'.upper(), 't_payment'.upper())
    yield table
    truncate_table(table)
    clean_export_changes(table)


@pytest.fixture()
def fake_t_order():
    """
    Таблица, дифф которой выгружается в 1 запрос
    """
    table = mimic_table('t_fake_t_order'.upper(), 't_order'.upper())
    yield table
    truncate_table(table)
    clean_export_changes(table)


@pytest.fixture()
def fake_tables(request):
    """
        Получает список имен для новых таблиц и создает их из t_product
    """
    tables = {}
    for table_name in request.param:
        logger.info("Creating testing table %s", table_name)
        tables[table_name] = mimic_table(table_name.upper(), 't_product'.upper())

    yield tables

    for table in tables.values():
        truncate_table(table)
        clean_export_changes(table)


@pytest.fixture(scope='session')
def table_with_update_dt_yt():
    yb_con_str = "balance/balance"
    schema = "bo"
    name = "t_update_dt_yt_test"

    engine = create_engine(yb_con_str, mode=RW)
    try:
        engine.execute(
            f"""
            CREATE TABLE {schema}.{name}
            (
                update_dt_yt DATE,
                name VARCHAR(50),
                count NUMBER
            )
            """
        )
    except Exception as err:
        logging.warning(err)
    table = OracleTableTarget((name, schema, yb_con_str))
    yield table
    drop_table(table)


@pytest.fixture()
def table_content_with_schema() -> Tuple[List[YTRecord], YTSchema, PYSchema]:
    return rows_with_yt_schema().example()


@pytest.fixture(params=workers)
def workers_fix(request):
    return request.param


def mimic_table(result_table_name, source_table_name):
    SOURCE_TABLE = source_table_name
    TEST_TABLE = result_table_name
    YB_CON_STR = "balance/balance"
    SCHEMA = "bo"

    engine = create_engine(YB_CON_STR, mode=RW)
    source_table = OracleTableTarget((SOURCE_TABLE, SCHEMA, YB_CON_STR))

    head, _ = peek(engine.execute(
        f"""
            SELECT count(1)
            FROM all_tables
            WHERE table_name = '{TEST_TABLE}'
            """
    ))
    table_exists = head[0] == 1
    if not table_exists:
        logging.info("Creating new table %s from %s using schema %s", result_table_name,
                     source_table_name, SCHEMA)
        engine.execute(
            f"""
                CREATE TABLE {SCHEMA}.{TEST_TABLE}
                AS (
                    SELECT *
                    FROM {SCHEMA}.{SOURCE_TABLE}
                    FETCH FIRST 50 ROWS ONLY
                )
                """
        )
    else:
        logging.info("Inserting into existing table %s from %s using schema %s", result_table_name,
                     source_table_name, SCHEMA)
        engine.execute(
            f"""
                INSERT INTO {SCHEMA}.{TEST_TABLE}
                (
                    SELECT *
                    FROM {SCHEMA}.{SOURCE_TABLE}
                    FETCH FIRST 50 ROWS ONLY
                )
                """
        )
    table = OracleTableTarget((TEST_TABLE, SCHEMA, YB_CON_STR))
    table.hints = deepcopy(source_table.hints)
    return table


def make_root(cluster) -> YTMapNodeTarget:
    conf_path = os.path.join(source_path('billing/dwh/src/dwh/conf/remote/etc/dwh/'), f'conf.{TEST}.yaml')
    with open(conf_path) as tf:
        test_conf = yaml.safe_load(tf)
    test_root = YTMapNodeTarget(test_conf['YT']['TEMP'], cluster=cluster) / YTMapNodeTarget("pytest/")
    test_root.clear(True)
    test_root.create()
    return test_root

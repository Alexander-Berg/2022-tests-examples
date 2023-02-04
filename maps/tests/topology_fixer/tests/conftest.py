import os
import glob
import psycopg2
import pytest
from string import split

import yatest.common

from helpers import YAML_EXT, data_path

from maps.pylibs.local_postgres import postgres_instance

from maps.wikimap.mapspro.libs.python import cpplogger
from maps.wikimap.mapspro.libs.python.validator import Validator, ValidatorConfig


def init_db(conn_params):
    conn = psycopg2.connect(conn_params.connection_string)
    cursor = conn.cursor()
    cursor.execute('CREATE EXTENSION postgis')
    cursor.execute('CREATE EXTENSION hstore')
    conn.commit()


@pytest.fixture(scope="session")
def postgres():
    cpplogger.init_logger()
    with postgres_instance() as conn_params:
        init_db(conn_params)
        yield conn_params


@pytest.fixture(scope="session")
def validator_config():
    editor_config_path = yatest.common.source_path('maps/wikimap/mapspro/cfg/editor/editor.xml')
    validator_config = ValidatorConfig(editor_config_path)
    return validator_config


@pytest.fixture(scope="session")
def validator(validator_config):
    validator = Validator(validator_config)
    validator.init_modules()
    return validator


def all_test_names():
    test_files = glob.iglob(os.path.join(data_path('testcases'), '*' + YAML_EXT))
    return [split(os.path.basename(test_file), '.')[0] for test_file in test_files]


def pytest_generate_tests(metafunc):
    if 'test_name' in metafunc.fixturenames:
        test_name = yatest.common.get_param('test_name')
        if test_name:
            metafunc.parametrize("test_name", [test_name])
        else:
            metafunc.parametrize("test_name", all_test_names())

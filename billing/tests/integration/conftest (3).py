# coding=utf-8

import sys
import logging

import pytest
from yt.wrapper import ypath_join

from billing.log_tariffication.py.tests.integration.conftest_partner import (  # noqa: F401
    udf_server_file_url,
)

from billing.library.python.yt_utils.test_utils.utils import (  # noqa: F401
    create_subdirectory,
)
from billing.library.python.yt_utils.test_utils.conftest import *  # noqa: F401, F403
from billing.library.python.yt_utils.test_utils.conftest import fix_yt_logger_fixture  # noqa: F401
from billing.library.python.yql_utils.test_utils.conftest import *  # noqa: F401, F403

from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID
)

logging.basicConfig(
    stream=sys.stdout,
    level=logging.INFO,
    format='%(asctime)s P%(process)-5s T%(thread)d %(levelname)-7s %(name)-15s: %(message)s'
)


@pytest.fixture
def get_fixture(request):
    """
    poor's man pytest-lazy-fixture https://pypi.org/project/pytest-lazy-fixture

    https://github.com/pytest-dev/pytest/issues/349#issuecomment-471400399
    """
    def _get_fixture(name):
        return request.getfixturevalue(name)
    return _get_fixture


@pytest.fixture(name='stripped_log_dir')
def stripped_log_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'stripped_log_dir')


@pytest.fixture(name='stripped_log_path')
def stripped_log_path_fixture(stripped_log_dir):
    return ypath_join(stripped_log_dir, CURR_RUN_ID)


@pytest.fixture(name='orders_table_path')
def orders_table_path_fixture(yt_root):
    return ypath_join(yt_root, 'orders')


@pytest.fixture(name='stream_log_dir')
def stream_log_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'stream_log_dir')


@pytest.fixture(name='tariff_results_dir')
def tariff_results_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'tariff_results')


@pytest.fixture(name='tariffed_dir')
def tariffed_dir_fixture(yt_client, tariff_results_dir):
    return create_subdirectory(yt_client, tariff_results_dir, 'tariffed')


@pytest.fixture(name='curr_tariffed_table_path')
def curr_tariffed_table_path_fixture(yt_root, tariffed_dir):
    return ypath_join(tariffed_dir, CURR_RUN_ID)


@pytest.fixture(name='untariffed_dir')
def untariffed_dir_fixture(yt_client, tariff_results_dir):
    return create_subdirectory(yt_client, tariff_results_dir, 'untariffed')


@pytest.fixture(name='curr_untariffed_table_path')
def curr_untariffed_table_path_fixture(yt_root, untariffed_dir):
    return ypath_join(untariffed_dir, CURR_RUN_ID)


@pytest.fixture(name='published_dir')
def published_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'published')


@pytest.fixture(name='published_untariffed_dir')
def published_untariffed_dir_fixture(yt_client, published_dir):
    return create_subdirectory(yt_client, published_dir, 'stripped_untariffed')


@pytest.fixture(name='prev_published_untariffed_table_path')
def prev_published_untariffed_table_path_fixture(yt_root, published_untariffed_dir):
    return ypath_join(published_untariffed_dir, PREV_RUN_ID)


@pytest.fixture(name='migration_results_dir')
def migration_results_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'migration')

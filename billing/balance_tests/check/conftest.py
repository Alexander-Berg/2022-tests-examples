# coding=utf-8

import os
import socket
import sys
import threading

import pytest

from balance.tests.conftest import pytest_generate_tests as balance_pytest_generate_tests, switch_to_pg, \
    OnlyFailedHandler, TestFilter, XFailChecker, NoParallelLock
from check import defaults
from check.fake_http_server import start_server, FAKE_APP
from check.utils import ensure_data_dir
import btestlib.shared as shared
import btestlib.utils as utils

os.environ['REQUESTS_CA_BUNDLE'] = '/etc/ssl/certs/ca-certificates.crt'

test_list = []
filename = 'check\\check_test_list.dat'


def pytest_addoption(parser):
    parser.addoption("--checklist", action="store", metavar="CHECKLIST",
                     help="only run checks matching the CHECKLIST.")
    parser.addoption("--force", action="store_true",
                     help="re-generate data.")

    OnlyFailedHandler.pytest_addoption(parser)
    shared.pytest_addoption(parser)
    TestFilter.pytest_addoption(parser)


def pytest_configure(config):
    # register an additional marker
    config.addinivalue_line("markers",
                            "categories: mark test to run only on selected categories")
    config.addinivalue_line("markers",
                            "force: force re-generation of input data for checks")

    NoParallelLock.pytest_configure(config)
    TestFilter.pytest_configure(config)


def port_is_busy(port):
    socket_ = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    return socket_.connect_ex(('', port)) == 0


def pytest_namespace():
    return {'active_tests': 0}


@pytest.hookimpl(trylast=True)
def pytest_collection_modifyitems(session, config, items):
    pytest.active_tests = [item.nodeid for item in items if not item.get_marker('xfail')]
    shared.pytest_collection_modifyitems(session, config, items, OnlyFailedHandler.LAST_FAILED_TESTS)


def pytest_runtest_setup(item):
    # SimpleapiVersionSetter.pytest_runtest_setup(item)

    # # Устанавливаем флаг, что мы внутри теста
    # sys._running_test_name = item.nodeid

    shared_state = shared.get_stage(item.session.config)

    # На стадии 'block' запускаем тест игнорируя любые возможные фильтрации. Считаем, что блок всегда корректен.
    if shared_state != shared.BLOCK:
        TestFilter.pytest_runtest_setup(item)
        XFailChecker.pytest_runtest_setup(item)

    NoParallelLock.pytest_runtest_setup(item)

    # Устанавливаем флаг, что мы внутри теста
    sys._running_test_name = item.nodeid


def pytest_runtest_teardown(item, nextitem):
    NoParallelLock.pytest_runtest_teardown(item, nextitem)

    # Убираем, флаг о том, что мы внутри теста
    if utils.is_inside_test():
        del sys._running_test_name


def pytest_generate_tests(metafunc):
    balance_pytest_generate_tests(metafunc)


def pytest_runtest_makereport(item, call):
    shared.pytest_runtest_makereport(item, call)


def pytest_sessionfinish(session, exitstatus):
    shared.pytest_sessionfinish(session, exitstatus)

@pytest.fixture()
def shared_data(request):
    from check import shared as check_shared
    # logger.LOG.debug(u'SHARED_DATA_BY_TEST:\n{}'.format(utils.Presenter.pretty(shared.SHARED_DATA_BY_TEST)))
    sd = check_shared.shared_data_fixture(request=request)
    # logger.LOG.debug(u'shared_data:\n{}'.format(utils.Presenter.pretty(sd)))
    return sd

# coding: utf-8
import os
import sys

import pytest

import btestlib.reporter as reporter
import btestlib.utils as butils
from balance.tests.conftest import ReportHelper
from balance.tests.conftest import XFailChecker, OnlyFailedHandler, ErrorCategories, \
    MutedTestsRunner, MutedTestsHandler, TestsRuntimeSorter, TestStatsCollector, WaitersHandler, MetricsCollector
from balance.tests.conftest import pytest_generate_tests as balance_pytest_generate_tests
from balance.tests.conftest import NoParallelLock
from simpleapi.common.utils import WebDriverProvider
from simpleapi.data import uids_pool

__author__ = 'fellow'


def pytest_addoption(parser):
    OnlyFailedHandler.pytest_addoption(parser)
    MutedTestsRunner.pytest_addoption(parser)
    MutedTestsHandler.pytest_addoption(parser)
    TestsRuntimeSorter.pytest_addoption(parser)
    TestStatsCollector.pytest_addoption(parser)
    MetricsCollector.pytest_addoption(parser)


@pytest.fixture(scope='function', autouse=True)
def log_test_title_autouse(request):
    reporter.log('======= Start test {} ======='.format(request.node.nodeid))


@pytest.fixture(scope='function', autouse=True)
def close_driver_autouse(request):
    def fin():
        WebDriverProvider.get_instance().close_driver()

    request.addfinalizer(fin)


def pytest_generate_tests(metafunc):
    balance_pytest_generate_tests(metafunc)


@pytest.hookimpl(trylast=True)
def pytest_collection_modifyitems(session, config, items):
    TestsRuntimeSorter.pytest_collection_modifyitems(session, config, items)


def pytest_runtest_setup(item):
    OnlyFailedHandler.pytest_runtest_setup(item)
    NoParallelLock.pytest_runtest_setup(item)
    XFailChecker.pytest_runtest_setup(item)
    MutedTestsHandler.pytest_runtest_setup(item)
    MutedTestsRunner.pytest_runtest_setup(item)

    sys._running_test_name = item.nodeid


def pytest_runtest_teardown(item, nextitem):
    NoParallelLock.pytest_runtest_teardown(item, nextitem)
    if butils.is_inside_test():
        del sys._running_test_name

    if uids_pool.users_holder_mode_enabled():
        current_pid = str(os.getpid())
        if uids_pool.users_holder.get(current_pid):
            current_users = list(uids_pool.users_holder[current_pid])
            for user in current_users:
                uids_pool.unmark_user(user)
                uids_pool.users_holder[current_pid].remove(user)

    # trust_url = u"https://st.yandex-team.ru/createTicket?summary=&description=&type=1" \
    #             u"&priority=2&followers=fellow&followers=slppls&bugDetectionMethod=Autotests&queue=TRUST"
    # pcidss_url = u"https://st.yandex-team.ru/createTicket?summary=&description=&type=1" \
    #              u"&priority=2&followers=fellow&followers=slppls&bugDetectionMethod=Autotests&queue=PCIDSS"
    # reporter.report_urls(u'Ссылки на создание тикетов',
    #                      (u'Создать тикет в TRUST', trust_url),
    #                      (u'Создать тикет в PCIDSS', pcidss_url))



def pytest_runtest_makereport(item, call):
    ErrorCategories.pytest_runtest_makereport(item, call)
    ReportHelper.pytest_runtest_makereport(item, call)


def pytest_runtest_logreport(report):
    ReportHelper.pytest_runtest_logreport(report)


def pytest_configure(config):
    OnlyFailedHandler.pytest_configure(config)
    ErrorCategories.pytest_configure(config)
    MutedTestsHandler.pytest_configure(config)
    NoParallelLock.pytest_configure(config)
    TestsRuntimeSorter.pytest_configure(config)
    TestStatsCollector.pytest_configure(config)
    WaitersHandler.pytest_configure(config)
    MetricsCollector.pytest_configure(config)


def pytest_report_teststatus(report):
    OnlyFailedHandler.pytest_report_teststatus(report)
    TestStatsCollector.pytest_report_teststatus(report)
    MetricsCollector.pytest_report_teststatus(report)


def pytest_sessionfinish(session, exitstatus):
    OnlyFailedHandler.pytest_sessionfinish(session, exitstatus)
    ErrorCategories.pytest_sessionfinish(session, exitstatus)
    TestStatsCollector.pytest_sessionfinish(session, exitstatus)
    MetricsCollector.pytest_sessionfinish(session, exitstatus)


def pytest_unconfigure(config):
    WaitersHandler.pytest_unconfigure(config)
    ReportHelper.pytest_unconfigure(config)

# coding: utf-8
# import sys

# from balance.tests.conftest import ParametrizeIdMaker


# def pytest_generate_tests(metafunc):
#     ParametrizeIdMaker.pytest_generate_tests(metafunc)
#
#
# def pytest_runtest_setup(item):
#     # Устанавливаем флаг, что мы внутри теста
#     sys._running_test_name = item.nodeid
#
#
# def pytest_report_teststatus(report):
#     pass


from collections import defaultdict

from balance.tests.conftest import pytest_addoption as balance_pytest_addoption
from balance.tests.conftest import pytest_collection_modifyitems as balance_pytest_collection_modifyitems
from balance.tests.conftest import pytest_configure as balance_pytest_configure
from balance.tests.conftest import pytest_generate_tests as balance_pytest_generate_tests
from balance.tests.conftest import pytest_report_teststatus as balance_pytest_report_teststatus
from balance.tests.conftest import pytest_runtest_logreport as balance_pytest_runtest_logreport
from balance.tests.conftest import pytest_runtest_makereport as balance_runtest_makereport
from balance.tests.conftest import pytest_runtest_setup as balance_pytest_runtest_setup
from balance.tests.conftest import pytest_runtest_teardown as balance_pytest_runtest_teardown
from balance.tests.conftest import pytest_sessionfinish as balance_pytest_sessionfinish
from balance.tests.conftest import pytest_sessionstart as balance_sessionstart
from balance.tests.conftest import pytest_unconfigure as balance_unconfigure
from btestlib import reporter


def pytest_addoption(parser):
    balance_pytest_addoption(parser)


def pytest_configure(config):
    balance_pytest_configure(config)


def pytest_generate_tests(metafunc):
    balance_pytest_generate_tests(metafunc)


def pytest_collection_modifyitems(session, config, items):
    balance_pytest_collection_modifyitems(session, config, items)


def pytest_sessionstart(session):
    balance_sessionstart(session)


def pytest_runtest_setup(item):
    balance_pytest_runtest_setup(item)


def pytest_runtest_teardown(item, nextitem):
    balance_pytest_runtest_teardown(item, nextitem)


def pytest_runtest_makereport(item, call):
    balance_runtest_makereport(item, call)
    PagediffHelper.pytest_runtest_makereport(item, call)


def pytest_runtest_logreport(report):
    balance_pytest_runtest_logreport(report)


def pytest_sessionfinish(session, exitstatus):
    balance_pytest_sessionfinish(session, exitstatus)
    PagediffHelper.pytest_sessionfinish(session, exitstatus)


def pytest_report_teststatus(report):
    balance_pytest_report_teststatus(report)


def pytest_unconfigure(config):
    balance_unconfigure(config)


class PagediffHelper(object):
    STATUSES = defaultdict(set)

    @staticmethod
    def pytest_runtest_makereport(item, call):
        if call.excinfo:
            try:
                diff_descr, unique_name = call.excinfo.value.message.split(';')
                PagediffHelper.STATUSES[diff_descr].add(unique_name)

            except Exception:  # todo-igogor конкретизировать
                pass

    @staticmethod
    def pytest_sessionfinish(session, exitstatus):
        for diff_descr, unique_names in PagediffHelper.STATUSES.iteritems():
            reporter.environment(**{diff_descr: u', '.join(unique_names)})

# @pytest.fixture(scope='session', autouse=True)
# def test_aggregate_report():
#     reporter.attach('aggregated-report', 'aggregated-report body')

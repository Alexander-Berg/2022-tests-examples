# coding: utf-8
__author__ = 'a-vasin'

from balance.tests.conftest import pytest_addoption as balance_pytest_addoption
from balance.tests.conftest import pytest_collection_modifyitems as balance_pytest_collection_modifyitems
from balance.tests.conftest import pytest_configure as balance_pytest_configure
from balance.tests.conftest import pytest_generate_tests as balance_pytest_generate_tests
from balance.tests.conftest import pytest_report_teststatus as balance_pytest_report_teststatus
from balance.tests.conftest import pytest_runtest_makereport as balance_pytest_runtest_makereport
from balance.tests.conftest import pytest_runtest_setup as balance_pytest_runtest_setup
from balance.tests.conftest import pytest_runtest_teardown as balance_pytest_runtest_teardown
from balance.tests.conftest import pytest_sessionfinish as balance_pytest_sessionfinish
from balance.tests.conftest import shared_data as balance_shared_data
from btestlib import reporter


def pytest_generate_tests(metafunc):
    balance_pytest_generate_tests(metafunc)


def pytest_addoption(parser):
    balance_pytest_addoption(parser)


def pytest_configure(config):
    balance_pytest_configure(config)


def pytest_collection_modifyitems(session, config, items):
    balance_pytest_collection_modifyitems(session, config, items)


def pytest_runtest_setup(item):
    balance_pytest_runtest_setup(item)


# ----------------------------------------------------------------------------------------------------------------------

def pytest_runtest_teardown(item, nextitem):
    balance_pytest_runtest_teardown(item, nextitem)


# ----------------------------------------------------------------------------------------------------------------------


def pytest_runtest_makereport(item, call):
    balance_pytest_runtest_makereport(item, call)


# ----------------------------------------------------------------------------------------------------------------------


def pytest_report_teststatus(report):
    balance_pytest_report_teststatus(report)
    reporter.attach(u'Аттач из хука', u'Прям из хука')
    [reporter.attach(name, contents, reporter.allure.attachment_type.TEXT)
     for (name, contents) in dict(report.sections).items()]


def pytest_terminal_summary(terminalreporter, exitstatus):
    pass


def pytest_runtest_logreport(report):
    pass


# ----------------------------------------------------------------------------------------------------------------------

def pytest_sessionfinish(session, exitstatus):
    balance_pytest_sessionfinish(session, exitstatus)


shared_data = balance_shared_data

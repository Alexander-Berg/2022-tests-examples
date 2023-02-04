# coding: utf-8

from collections import defaultdict

from balance.tests.conftest import pytest_addoption as balance_addoption
from balance.tests.conftest import pytest_collection_modifyitems as balance_collection_modifyitems
from balance.tests.conftest import pytest_configure as balance_configure
from balance.tests.conftest import pytest_generate_tests as balance_generate_tests
from balance.tests.conftest import pytest_report_teststatus as balance_report_teststatus
from balance.tests.conftest import pytest_runtest_logreport as balance_runtest_logreport
from balance.tests.conftest import pytest_runtest_makereport as balance_runtest_makereport
from balance.tests.conftest import pytest_runtest_setup as balance_runtest_setup
from balance.tests.conftest import pytest_runtest_teardown as balance_runtest_teardown
from balance.tests.conftest import pytest_sessionfinish as balance_sessionfinish
from balance.tests.conftest import pytest_sessionstart as balance_sessionstart
from balance.tests.conftest import pytest_unconfigure as balance_unconfigure
from btestlib import reporter, utils, pagediff


def pytest_addoption(parser):
    balance_addoption(parser)


def pytest_configure(config):
    balance_configure(config)


def pytest_generate_tests(metafunc):
    balance_generate_tests(metafunc)


def pytest_collection_modifyitems(session, config, items):
    balance_collection_modifyitems(session, config, items)


def pytest_sessionstart(session):
    balance_sessionstart(session)


def pytest_runtest_setup(item):
    balance_runtest_setup(item)


def pytest_runtest_teardown(item, nextitem):
    balance_runtest_teardown(item, nextitem)


def pytest_runtest_makereport(item, call):
    balance_runtest_makereport(item, call)
    PagediffHelper.pytest_runtest_makereport(item, call)


def pytest_runtest_logreport(report):
    balance_runtest_logreport(report)


def pytest_sessionfinish(session, exitstatus):
    balance_sessionfinish(session, exitstatus)
    PagediffHelper.pytest_sessionfinish(session, exitstatus)


def pytest_report_teststatus(report):
    balance_report_teststatus(report)


def pytest_unconfigure(config):
    balance_unconfigure(config)


class PagediffHelper(object):
    STATUSES = {}
    SLAVE_PREFIX = 'pagediff_statuses'

    @staticmethod
    def pytest_runtest_makereport(item, call):
        if call.excinfo and call.excinfo.type == pagediff.PagediffError:
            error = call.excinfo.value
            PagediffHelper.STATUSES[error.unique_name] = error.types_count_str



    @staticmethod
    def pytest_sessionfinish(session, exitstatus):
        if not utils.is_master_node(session.config) and PagediffHelper.STATUSES:
            utils.save_slave_value(utils.make_build_unique_key(PagediffHelper.SLAVE_PREFIX),
                                   PagediffHelper.STATUSES)
        else:
            slave_statuses = utils.collect_slave_values(PagediffHelper.SLAVE_PREFIX) or {0: PagediffHelper.STATUSES}
            combined_statuses = utils.merge_dicts([statuses for _, statuses in slave_statuses.iteritems()])

            grouped_by_status = defaultdict(list)
            for unique_name, diff_status in combined_statuses.iteritems():
                grouped_by_status[diff_status].append(unique_name)

            reporter.environment(**{diff_status: ', '.join(unique_names)
                                    for diff_status, unique_names in grouped_by_status.iteritems()})

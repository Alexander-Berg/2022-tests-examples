import pytest

import temp.aikawa.Balance.wiki as wm
from temp.aikawa.Balance.wiki import WikiMarker as wm

test_list = []
filename = 'test_list.dat'
wiki = 0
docs = 0
actions = {}

# def pytest_addoption(parser):
#     parser.addoption("--docs", action="store", metavar="DOC",
#         help="store test info.")
#
# def pytest_configure(config):
#     # register an additional marker
#     config.addinivalue_line("markers",
#         "docs: doc integration")
#     if config.getoption("--docs") is not None:
#         with open(filename, 'w') as f:
#             f.close()

# def pytest_pycollect_makeitem(collector, name, obj):
#     with open('try.dat', 'a') as f:
#             f.write(str(collector))

PRIORITY_MID = 'mid'


# def pytest_collection_modifyitems(session, config, items):
#     utils.Pytest.format_parametrized_python_item_name(items)
#
#     for item in items:
#         if item.config.getoption("--docs"):
#             global docs
#             docs = 1
#             test_info = {}
#             test_info['item'] = item.nodeid
#             test_info['docs'] = item.keywords._markers['docs'] if 'docs' in item.keywords._markers else None
#             test_info['docpath'] = item.keywords._markers['docpath'] if 'docpath' in item.keywords._markers else None
#             test_info['docstring'] = item.function.__doc__
#             test_list.append(test_info)
#
#     if docs == 1:
#         result_dict = {}
#         for mark in test_list:
#             wm.append_to_result_dict(wm(mark), result_dict)
#         wm.main(result_dict)

# def pytest_runtest_protocol(item):
#
#     if item.config.getoption("--docs"):
#         global docs
#         docs = 1
#         test_info = {}
#         test_info['item'] = item.nodeid
#         test_info['docs'] = item.keywords._markers['docs'] if 'docs' in item.keywords._markers else None
#         test_info['docpath'] = item.keywords._markers['docpath'] if 'docpath' in item.keywords._markers else None
#         test_info['docstring'] = item.function.__doc__
#         test_list.append(test_info)
#
# def pytest_runtest_setup (item):
#     pass
#
def pytest_runtest_makereport(item, call):
    pass


def pytest_report_teststatus(report):
    if report.nodeid not in actions:
        actions[report.nodeid] = []
    actions[report.nodeid].append('{0} : {1}'.format(report.when, report.outcome))


def pytest_sessionfinish(session, exitstatus):
    if docs == 1:
        # wm.main(test_list)
        # # result_dict = collections.defaultdict(dict)
        # #
        # #
        print test_list
        result_dict = {}
        for mark in test_list:
            wm.append_to_result_dict(wm(mark), result_dict)
        print result_dict
        result = wm.main(result_dict)


@pytest.fixture(scope='session')
def get_cached_data():
    return True


@pytest.fixture()
def data_cache(request, get_cached_data):
    request._pyfuncitem._nodeid
    return request._pyfuncitem._nodeid

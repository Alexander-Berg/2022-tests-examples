# coding=utf-8
__author__ = 'torvald'

import pytest
import pymongo
import btestlib.utils as utils
# from balance.tests.conftest import pytest_addoption as balance_pytest_addoption
# from balance.tests.conftest import pytest_collection_modifyitems as balance_pytest_collection_modifyitems
# from balance.tests.conftest import pytest_configure as balance_pytest_configure
# from balance.tests.conftest import pytest_runtest_protocol as balance_pytest_runtest_protocol
from balance.tests.conftest import pytest_sessionfinish as balance_pytest_sessionfinish
from btestlib import environments, config
from apikeys import apikeys_api

from balance.tests.conftest import *


@pytest.fixture(scope='session', autouse=True)

def free_passport(request):
    if hasattr(request.config, 'slaveinput'):
        # Workaround for workers name: 'gw12'
        return int(request.config.slaveinput['slaveid'][2:])
    else:
        return 1


@pytest.fixture(scope='session', autouse=True)
def db_connection():
    client = pymongo.MongoClient('mongo.apikeys-test.paysys.yandex.net:27217')
    client.apikeys.authenticate('bo', 'balalancing')
    return client.apikeys


# def pytest_addoption(parser):
#     balance_pytest_addoption(parser)
#
#
# def pytest_configure(config):
#     balance_pytest_configure(config)
#
#
# def pytest_collection_modifyitems(session, config, items):
#     balance_pytest_collection_modifyitems(session, config, items)
#
#
# def pytest_runtest_protocol(item):
#     balance_pytest_runtest_protocol(item)
#
#
# def pytest_runtest_setup(item):
#     balance_pytest_runtest_protocol(item)


def pytest_sessionfinish(session, exitstatus):
    # удаляем таски без _cls, созданные маппером в результате очистки таскера
    apikeys_api.TEST.mongo_remove('task', {'_cls': {'$exists': False}})
    if config.TEAMCITY_VERSION is not 'LOCAL':
        balance_pytest_sessionfinish(session, exitstatus)

# @pytest.fixture(scope='session', autouse=True)
# def check_notify_params():
#     expected_notify_url = environments.apikeys_env().apikeys_notify_params
#     balance_notify_url = db.BalanceBO().execute(
#         "SELECT url FROM t_service_notify_params WHERE service_id = {}".format(APIKEYS_SERVICE_ID))[0]
#     if not expected_notify_url == balance_notify_url.get('url'):
#         pytest.exit(
#             'Неправильный URL нотификатора в базе баланса\n\tОжидаемый: {}\n\tВ t_service_notify_params: {}'.format(
#                 expected_notify_url, balance_notify_url
#             ))

# TODO: сделать так чтобы фикстура запускалась только 1 раз
# @pytest.fixture(scope='session', autouse=True)
# def update_tariffs_in_ballance():
#     apikeys_api.BO().push_tariffs_to_ballance(ADMIN)


# @pytest.fixture(scope='function', autouse=True)
# def clean_user(free_passport):
#     oper_uid, _ = APIKEYS_LOGIN_POOL[free_passport]
#     clean_up(oper_uid)

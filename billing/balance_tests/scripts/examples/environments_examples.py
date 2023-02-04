# coding: utf-8

import pytest

from btestlib.constants import Services
from btestlib.environments import TrustDbNames, TrustApiUrls, SimpleapiEnvironment

"""Переключение между разными базами и апи траста"""


# 1. Самый простой вариант это использовать фикстуру switch_to_pg - она лежит в balance/tests/conftest.py
# Перед тестом переключит базу и xmlrpc траста на pg. После обратно на оракл
def test_some(switch_to_pg):
    pass


# Чтобы добавить фикстуру всем тестам в модуле или классе надо использовать марку
pytestmark = [pytest.mark.usefixtures('switch_to_pg')]


# 2. Если хочется переключиться на конкретную базу и апи то использовать switch_to_trust
# Фикстура переключает в момент вызова на те параметры которые передашь, а после теста обратно на оракл.

# Можно переключаться по сервису, для этого во все сервисы были добавлены база и апи траста в которые сервис ходит
def test_some_2(switch_to_trust):
    switch_to_trust(service=Services.TAXI)


# Можно переключаться явно.
# Текущие варианты перечислены в классах в environments. Но можно строкой передавать и dbname и урл xmlrpc не из списка
def test_some_3(switch_to_trust):
    switch_to_trust(dbname=TrustDbNames.BS_XG, xmlrpc_url=TrustApiUrls.XMLRPC_ORA)


# 3. Если переключиться надо не в тесте - можно сделать вызовом функции
def some_func():
    # по сервису
    SimpleapiEnvironment.switch_param(service=Services.TAXI)
    # или по параметрам
    SimpleapiEnvironment.switch_param(dbname=TrustDbNames.BS_XG, xmlrpc_url=TrustApiUrls.XMLRPC_ORA)

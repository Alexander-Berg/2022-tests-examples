# coding=utf-8
import pytest
from apikeys import apikeys_steps as steps
from btestlib import utils
from hamcrest import equal_to, has_item
from apikeys.tests_by_typical_flows.common import tariffs
from apikeys.apikeys_utils import get_parameters

__author__ = 'ilya_knysh'

TARIFFS_LIMIT = tariffs.tariffs


@pytest.mark.parametrize(
    'tariff', get_parameters(TARIFFS_LIMIT), ids=lambda x: x.get('cc'))
@pytest.mark.consistency
def test_tarifficator_config(tariff):
    db_config = steps.get_tariff(tariff.get('cc'))[0].get('tarifficator_config')
    standard_config = tariff.get('tarifficator_config')
    try:
        utils.check_that(db_config, equal_to(standard_config), u'Проверяем соответствие tarifficator_config в БД')
    except AssertionError:
        utils.check_that(len(db_config), equal_to(len(standard_config)), u'Проверяем количество юнитов')
        for unit in standard_config:
            utils.check_that(db_config, has_item(unit), u'Проверяем, содержится ли юнит в конфиге БД')


@pytest.mark.parametrize(
    'tariff', get_parameters(TARIFFS_LIMIT), ids=lambda x: x.get('cc'))
@pytest.mark.consistency
def test_client_access(tariff):
    db_config = steps.get_tariff(tariff.get('cc'))[0].get('client_access')
    standard_config = tariff.get('client_access')
    utils.check_that(db_config, equal_to(standard_config), u'Проверяем соответствие client_access в БД')


@pytest.mark.parametrize(
    'tariff', get_parameters(TARIFFS_LIMIT), ids=lambda x: x.get('cc'))
@pytest.mark.consistency
def test_info_for_table(tariff):
    db_config = steps.get_tariff(tariff.get('cc'))[0].get('info_for_table')
    standard_config = tariff.get('info_for_table')
    try:
        utils.check_that(db_config, equal_to(standard_config), u'Проверяем соответствие info_for_table в БД')
    except AssertionError:
        utils.check_that(len(db_config), equal_to(len(standard_config)), u'Проверяем количество нод')
        for unit in db_config:
            utils.check_that(standard_config, has_item(unit), u'Проверяем, содержится ли нода в конфиге БД')


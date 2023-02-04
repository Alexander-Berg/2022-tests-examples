# -*- coding: utf-8 -*-

from collections import namedtuple
from datetime import datetime, timedelta as shift
from apikeys.apikeys_utils import trunc_date
import pytest

from apikeys.tests_by_typical_flows import typical_flows as flow
from apikeys.apikeys_utils import get_parameters, Person_type
from btestlib import utils
from btestlib.utils import aDict

__author__ = 'kostya-karpus'

pytestmark = [pytest.mark.docpath('https://wiki.yandex-team.ru/Testirovanie/functesting/billing/apikeys/spec/back/Test_cover')]

BASE_DT = datetime.utcnow()
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

Tariff = namedtuple('Tariff', ['name', 'service_id', 'price'])
MARKET_SERVICE_CC = 'market'

CLIENT_MINI = pytest.mark.smoke(Tariff('apikeys_market_api_client_mini', MARKET_SERVICE_CC, 20000))
CLIENT_MAXI = Tariff('apikeys_market_api_client_maxi', MARKET_SERVICE_CC, 150000)
VENDOR_MINI = Tariff('apikeys_market_vendor_mini', MARKET_SERVICE_CC, 20000)
VENDOR_MAXI = Tariff('apikeys_market_vendor_maxi', MARKET_SERVICE_CC, 150000)
FREE = Tariff('apikeys_market_api_client_base', MARKET_SERVICE_CC, 0)

ur_market = Person_type('ur', 11101003, 1003, True)
ph_market = Person_type('ph', 11101001, 1001, True)

tariffs_for_test = [
    CLIENT_MINI,
    CLIENT_MAXI,
    VENDOR_MINI,
    VENDOR_MAXI,
]

general_scenarios = [
    # Test-case 0
    {'description': u'1 day',
     'base_dt': BASE_DT,
     'tarifficator_days': [0],
     'expected_days': 1,
     'active_after_scenario': True,
     'close_month': True
     },

    # Test-case 1
    {'description': u'31 days',
     'base_dt': BASE_DT - shift(days=30),
     'tarifficator_days': [0, 29, 30],
     'expected_days': 31,
     'active_after_scenario': True,
     'close_month': True
     },

    # Test-case 2
    {'description': u'10 days',
     'base_dt': BASE_DT - shift(days=9),
     'tarifficator_days': [0, 1, 9],
     'expected_days': 10,
     'active_after_scenario': True,
     },

    # Test-case 3
    {'description': u'32 days, locked link',
     'base_dt': BASE_DT - shift(days=31),
     'tarifficator_days': [0, 1, 31],
     'expected_days': 31,
     'active_after_scenario': False,
     },

    # Test-case 4
    {'description': u'prepaid 1 day, locked link',
     'base_dt': BASE_DT - shift(days=1),
     'tarifficator_days': [0, 1],
     'expected_days': 1,
     'active_after_scenario': False,
     'prepaid_days': 1,
     },

    # Test-case 5
    {'description': u'50 days, double paid',
     'base_dt': BASE_DT - shift(days=49),
     'tarifficator_days': [0, 30, 40, 49],
     'expected_days': 50,
     'active_after_scenario': True,
     'prepaid_days': 62,
     },
]


@pytest.mark.docs(u'API Маркета, оферта с предоплаченным периодом')
@pytest.mark.parametrize('person_type', [ur_market], ids=lambda x: '[{}]'.format(x[0]))
@pytest.mark.parametrize('tariff', get_parameters(tariffs_for_test, ), ids=lambda x: x.name)
@pytest.mark.parametrize(
    'scenario', get_parameters(general_scenarios, )
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_tariff_general(scenario, tariff, person_type, db_connection):
    scenario_copy = aDict(scenario)
    scenario_copy.tariff = tariff.name
    flow.Prepayment.basic(scenario_copy, db_connection, tariff.service_id, tariff, person_type)


two_deposit_scenarios = [
    # Test-case 0
    {'description': u'31 active + 31 active',
     'first_paid': {
         'base_dt': START_PREVIOUS_MONTH - shift(days=31),
         'tarifficator_days': [0, 30],
         'expected_days': 31,
         'active_after_scenario': True},

     'second_paid': {
         'base_dt': START_PREVIOUS_MONTH,
         'tarifficator_days': [0, 30],
         'expected_days': 62,
         'active_after_scenario': True}
     },

    # Test-case 1
    {'description': u'31 locked + 31 active',
     'first_paid': {
         'base_dt': START_PREVIOUS_MONTH - shift(days=31),
         'tarifficator_days': [0, 31],
         'expected_days': 31,
         'active_after_scenario': False},

     'second_paid': {
         'base_dt': START_PREVIOUS_MONTH + shift(days=1),
         'tarifficator_days': [0, 30],
         'expected_days': 62,
         'active_after_scenario': True}
     },

    # Test-case 2
    {'description': u'31 locked + 31 locked',
     'first_paid': {
         'base_dt': START_PREVIOUS_MONTH - shift(days=31),
         'tarifficator_days': [0, 31],
         'expected_days': 31,
         'active_after_scenario': False},

     'second_paid': {
         'base_dt': START_PREVIOUS_MONTH + shift(days=1),
         'tarifficator_days': [0, 31],
         'expected_days': 62,
         'active_after_scenario': False}
     },
]


@pytest.mark.parametrize('person_type', [ur_market], ids=lambda x: '[{}]'.format(x[0]))
@pytest.mark.parametrize('tariff', get_parameters(tariffs_for_test, ), ids=lambda x: x.name)
@pytest.mark.parametrize(
    'scenario', get_parameters(two_deposit_scenarios, 1)
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_two_deposit(scenario, tariff, person_type, db_connection):
    scenario_copy = aDict(scenario)
    scenario_copy.tariff = tariff.name
    flow.Prepayment.two_deposit(scenario_copy, db_connection, tariff.service_id, tariff, person_type)


not_enough_money_scenarios = [
    # Test-case 0
    {'description': u'money for 1 day - 0,01 RUR',
     'base_dt': BASE_DT,
     'tarifficator_days': [0],
     'shift_money': -0.01,
     },

    # Test-case 1
    {'description': u'money for 1 day - 1 RUR',
     'base_dt': BASE_DT,
     'tarifficator_days': [0],
     'shift_money': -1.,
     },
]


@pytest.mark.parametrize('person_type', [ur_market], ids=lambda x: '[{}]'.format(x[0]))
@pytest.mark.parametrize('tariff', get_parameters(tariffs_for_test, ), ids=lambda x: x.name)
@pytest.mark.parametrize(
    'scenario', get_parameters(not_enough_money_scenarios, )
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_not_enough_money(scenario, tariff, person_type, db_connection):
    scenario_copy = aDict(scenario)
    scenario_copy.tariff = tariff.name
    flow.Prepayment.not_enough_money(scenario_copy, db_connection, tariff.service_id, tariff, person_type)


change_tariff_scenarios = [
    # Test-case 0
    {'description': u'15 mini + 2 maxi',
     'first_tariff': {
         'base_dt': BASE_DT - shift(days=16),
         'tarifficator_days': [0, 14],
         'expected_days': 15,
         'active_after_scenario': True,
         'tariff': CLIENT_MINI},

     'second_tariff': {
         'base_dt': BASE_DT - shift(days=2),
         'tarifficator_days': [0, 1],
         'expected_days': 2,
         'active_after_scenario': True,
         'tariff': CLIENT_MAXI}
     },

    # Test-case 1
    {'description': u'10 maxi + 10 mini',
     'first_tariff': {
         'base_dt': BASE_DT - shift(days=20),
         'tarifficator_days': [0, 9],
         'expected_days': 10,
         'active_after_scenario': True,
         'tariff': VENDOR_MAXI},

     'second_tariff': {
         'base_dt': BASE_DT - shift(days=10),
         'tarifficator_days': [0, 9],
         'expected_days': 10,
         'active_after_scenario': True,
         'tariff': VENDOR_MINI}
     },

    # Test-case 2
    {'description': u'15 mini + 2 free',
     'first_tariff': {
         'base_dt': BASE_DT - shift(days=16),
         'tarifficator_days': [0, 14],
         'expected_days': 15,
         'active_after_scenario': True,
         'tariff': CLIENT_MINI},

     'second_tariff': {
         'base_dt': BASE_DT - shift(days=2),
         'tarifficator_days': [0, 1],
         'expected_days': 0,
         'active_after_scenario': True,
         'tariff': FREE}
     },

    # Test-case 3
    {'description': u'15 vendor maxi  + 2 free',
     'first_tariff': {
         'base_dt': BASE_DT - shift(days=16),
         'tarifficator_days': [0, 14],
         'expected_days': 15,
         'active_after_scenario': True,
         'tariff': VENDOR_MAXI},

     'second_tariff': {
         'base_dt': BASE_DT - shift(days=2),
         'tarifficator_days': [0, 1],
         'expected_days': 0,
         'active_after_scenario': True,
         'tariff': FREE}
     },
]


@pytest.mark.parametrize('person_type', [ur_market], ids=lambda x: '[{}]'.format(x[0]))
@pytest.mark.parametrize(
    'scenario', get_parameters(change_tariff_scenarios, )
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_change_tariff(scenario, person_type, db_connection):
    scenario_copy = aDict(scenario)
    flow.Prepayment.change_tariff(scenario_copy, db_connection, person_type)


if __name__ == "__main__":
    pytest.main('-v --docs --collect-only')
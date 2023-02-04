# -*- coding: utf-8 -*-
import datetime

import hamcrest
import pytest

import btestlib.utils as utils
from balance import balance_api as api
from btestlib.constants import ContractCommissionType, PersonTypes, Firms, Currencies, BrandType
from simpleapi.matchers.deep_equals import deep_contains
from temp.aikawa.Balance.contracts.contracts_rules import ContractException, CommonContract, check_param, \
    PARAM_NEEDED_EXCEPTION, collect_contract_params, group_by_contract_state, multiply_by_params_value

to_iso = utils.Date.date_to_iso_format

NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)
TWO_DAYS_AFTER = utils.Date.nullify_time_of_date(NOW + datetime.timedelta(days=2))
HALF_YEAR_AFTER = NOW + datetime.timedelta(days=180)

minimal_attrs = ['CLIENT_ID', 'DT', 'FINISH_DT']

CONTRACT_CONTEXT = CommonContract.new(type=ContractCommissionType.BRAND,
                                      name='BRAND',
                                      minimal_attrs=minimal_attrs,
                                      client_contract=True,
                                      person_type=PersonTypes.UR)


def test_minimal_attrs_set():
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='COMMISSION', changeable=True)
    for param in minimal_attrs:
        check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param,
                    strictly_needed=PARAM_NEEDED_EXCEPTION[param])


hidden_params_list = [
    [{}, {'BANK_DETAILS_ID': 1}],
    [{}, {'MANAGER_CODE': 1}],
    [{}, {'MANAGER_BO_CODE': 1}],
    [{}, {'PAYMENT_TYPE': 1}],
    [{}, {'UNILATERAL': 1}],
    [{}, {'SERVICES': [1]}],
    [{}, {'IS_SUSPENDED': to_iso(NOW_NULLIFIED)}],
    [{}, {'PERSON_ID': 1}],
    [{}, {'IS_FAXED': to_iso(NOW_NULLIFIED)}],
    [{}, {'CURRENCY': Currencies.RUB.iso_num_code}],
    [{}, {'FIRM': Firms.YANDEX_1.id}],
    [{'BRAND_TYPE': [BrandType.DIRECT_TECH, BrandType.AUTO_RU, BrandType.MEDIA],
      'DT': to_iso(TWO_DAYS_AFTER),
      'FINISH_DT': to_iso(HALF_YEAR_AFTER)}, {'DISCARD_MEDIA_DISCOUNT': 1}]
]

restricted_params_list = [
    ({}, {'IS_SUSPENDED': to_iso(TOMORROW_NULLIFIED)},
     ContractException.IS_SUSPENDED_EXCEPTION),

    ({'BRAND_TYPE': BrandType.DIRECT}, {'DT': to_iso(TOMORROW_NULLIFIED)},
     ContractException.BRAND_START_DT_EXCEPTION),

    ({'BRAND_TYPE': BrandType.DIRECT_TECH}, {'DT': to_iso(TOMORROW_NULLIFIED)},
     ContractException.BRAND_START_DT_EXCEPTION),
]

changeable_value_list = [
    [{'BRAND_TYPE': BrandType.DIRECT,
      'DT': to_iso(TWO_DAYS_AFTER),
      'FINISH_DT': to_iso(HALF_YEAR_AFTER)
      }, {'DISCARD_MEDIA_DISCOUNT': [1, 0]}],
]

contract = api.medium().CreateContract(16571028, {'client-id': 81247267, 'commission': 50, 'dt': '2018-04-09T00:00:00',
                                                  'finish-dt': '2018-04-10T00:00:00'})


@pytest.mark.parametrize('contract_state, hidden_params',
                         group_by_contract_state(hidden_params_list)
                         )
def test_hidden_params(contract_state, hidden_params):
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    adds.update(contract_state)
    adds.update(hidden_params)
    contract_id, contract_params = check_param(context=CONTRACT_CONTEXT.new(adds=adds), without_check=True)
    saved_contract_params = collect_contract_params(contract_id)
    for param in hidden_params.keys():
        if param in saved_contract_params.keys():
            utils.check_that(saved_contract_params[param], hamcrest.none(),
                             u'Проверяем, что {} не заполнен'.format(param))


@pytest.mark.parametrize('contract_state, restricted_param, exception',
                         restricted_params_list
                         )
def test_restricted_params(contract_state, restricted_param, exception):
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    adds.update(contract_state)
    adds.update(restricted_param)
    check_param(context=CONTRACT_CONTEXT.new(adds=adds), with_exception=exception)


@pytest.mark.parametrize('contract_state, changeable_params',
                         multiply_by_params_value(changeable_value_list)
                         )
def test_changeable_values(contract_state, changeable_params):
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    adds.update(contract_state)
    adds.update(changeable_params)
    contract_id, contract_params = check_param(context=CONTRACT_CONTEXT.new(adds=adds), without_check=True)
    saved_contract_params = collect_contract_params(contract_id)
    utils.check_that(saved_contract_params, deep_contains(changeable_params))

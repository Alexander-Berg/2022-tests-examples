# -*- coding: utf-8 -*-
import datetime

import hamcrest
import pytest

import btestlib.utils as utils
from btestlib.constants import ContractCommissionType, PersonTypes, Currencies
from simpleapi.matchers.deep_equals import deep_contains
from temp.aikawa.Balance.contracts.contracts_rules import CommonContract, check_param, \
    PARAM_NEEDED_EXCEPTION, collect_contract_params, group_by_contract_state

NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
NOW_NULLIFIED_ISO = utils.Date.date_to_iso_format(NOW_NULLIFIED)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)
TOMORROW_NULLIFIED_ISO = utils.Date.date_to_iso_format(TOMORROW_NULLIFIED)
HALF_YEAR_AFTER = NOW + datetime.timedelta(days=180)

to_iso = utils.Date.date_to_iso_format

minimal_attrs = ['CLIENT_ID', 'DT', 'FINISH_DT']

CONTRACT_CONTEXT = CommonContract.new(type=ContractCommissionType.GARANT_BEL,
                                      name='GARANT_BEL',
                                      minimal_attrs=minimal_attrs,
                                      client_contract=True,
                                      person_type=PersonTypes.UR)

hidden_params_list = [
    [{}, {'BANK_DETAILS_ID': 1}],
    [{}, {'MANAGER_CODE': 1}],
    [{}, {'MANAGER_BO_CODE': 1}],
    [{}, {'PAYMENT_TYPE': 1}],
    [{}, {'UNILATERAL': 1}],
    [{}, {'SERVICES': [1]}],
    [{}, {'IS_SUSPENDED': to_iso(NOW_NULLIFIED)}],
    [{}, {'PERSON_ID': 1}],
    [{}, {'IS_SIGNED': to_iso(NOW_NULLIFIED)}],
]

default_params_list = [
    [{}, {'IS_FAXED': NOW_NULLIFIED}],
    [{}, {'CURRENCY': Currencies.RUB.num_code}]
]

disabled_params_list = [
    # состояние договора, имя параметра, значение/значение по умолчанию
    [{}, {'IS_FAXED': [to_iso(TOMORROW_NULLIFIED), NOW_NULLIFIED]}],
]


def test_minimal_attrs_set():
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='COMMISSION', changeable=True)
    for param in minimal_attrs:
        check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param,
                    strictly_needed=PARAM_NEEDED_EXCEPTION[param])


@pytest.mark.parametrize('contract_state, default_params',
                         group_by_contract_state(default_params_list)
                         )
def test_default_value(contract_state, default_params):
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    adds.update(contract_state)
    contract_id, contract_params = check_param(context=CONTRACT_CONTEXT.new(adds=adds), without_check=True)
    saved_contract_params = collect_contract_params(contract_id)
    utils.check_that(saved_contract_params, deep_contains(default_params))


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
            utils.check_that(saved_contract_params[param], hamcrest.none())


@pytest.mark.parametrize('contract_state, disabled_params',
                         group_by_contract_state(disabled_params_list)
                         )
def test_disabled_params(contract_state, disabled_params):
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    adds.update(contract_state)
    default_params_dict = {k: v[1] for k, v in disabled_params.iteritems()}
    disabled_params = {k: v[0] for k, v in disabled_params.iteritems()}
    adds.update(disabled_params)
    contract_id, contract_params = check_param(context=CONTRACT_CONTEXT.new(adds=adds), without_check=True)
    saved_contract_params = collect_contract_params(contract_id)
    for param in disabled_params.keys():
        utils.check_that(saved_contract_params[param], hamcrest.equal_to(default_params_dict[param]),
                         u'Проверяем, что {} заполнился {} по умолчанию'.format(param, default_params_dict[param]))

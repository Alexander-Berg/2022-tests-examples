# -*- coding: utf-8 -*-
import datetime

import pytest

import btestlib.utils as utils
from temp.aikawa.Balance.contracts import contracts_rules as contracts_defaults
from temp.aikawa.Balance.contracts.contracts_rules import ContractException

to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)

all_contracts = contracts_defaults.all_contracts
is_suspended_hidden_contracts = [contracts_defaults.BRAND, contracts_defaults.GARANT_BEL,
                                 contracts_defaults.GARANT_RU,
                                 contracts_defaults.GARANT_KZT]
is_suspended_optional_contracts = contracts_defaults.diff(all_contracts, is_suspended_hidden_contracts)


@pytest.mark.parametrize('contract_context',
                         is_suspended_optional_contracts,
                         ids=lambda p, v, ep: '{}'.format(p))
@pytest.mark.parametrize('param_name, value, extra_params',
                         [
                             ('IS_SUSPENDED', to_iso(NOW_NULLIFIED), {}),
                         ],
                         ids=lambda p, v, ep: '{}'.format(p))
def test_optional_params(param_name, value, extra_params, contract_context):
    adds = {param_name: value}
    adds.update(extra_params)
    contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=param_name, optional=True)


@pytest.mark.parametrize('contract_context',
                         is_suspended_optional_contracts,
                         ids=lambda p, v, ep: '{}'.format(p))
@pytest.mark.parametrize('param_name, value_list, exception_type, extra_params',
                         [
                             ('IS_SUSPENDED', [to_iso(TOMORROW_NULLIFIED)],
                              ContractException.IS_SUSPENDED_EXCEPTION, {})
                         ],
                         ids=lambda p, v, ep: '{}'.format(p))
def test_denied_value_params(param_name, value_list, exception_type, extra_params, contract_context):
    for value in value_list:
        adds = {param_name: value}
        adds.update(extra_params)
        contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=param_name,
                                       with_exception=exception_type)


@pytest.mark.parametrize('contract_context',
                         is_suspended_hidden_contracts,
                         ids=lambda p, v, ep: '{}'.format(p))
@pytest.mark.parametrize('param_name, value, extra_params',
                         [
                             ('IS_SUSPENDED', to_iso(NOW_NULLIFIED), {}),
                         ],
                         ids=lambda a: '{}'.format(a))
def test_hidden_params(param_name, value, extra_params, contract_context):
    adds = {param_name: value}
    contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=adds.keys()[0],
                                   hidden=True)

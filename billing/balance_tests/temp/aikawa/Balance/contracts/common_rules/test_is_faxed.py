# -*- coding: utf-8 -*-
import datetime

import pytest

import btestlib.utils as utils
from temp.aikawa.Balance.contracts import contracts_rules as contracts_defaults

to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)

all_contracts = contracts_defaults.all_contracts

is_faxed_hidden_contracts = [contracts_defaults.BRAND]

is_faxed_default_contracts = [contracts_defaults.GARANT_BEL,
                              contracts_defaults.GARANT_RU,
                              contracts_defaults.GARANT_KZT]

is_faxed_optional_contracts = contracts_defaults.diff(contracts_defaults.diff(all_contracts, is_faxed_hidden_contracts),
                                                      is_faxed_default_contracts)


@pytest.mark.parametrize('contract_context',
                         is_faxed_optional_contracts,
                         ids=lambda cc: '{}'.format(cc.name))
@pytest.mark.parametrize('param_name, value, extra_params',
                         [
                             ('IS_FAXED', to_iso(NOW_NULLIFIED), {}),
                         ])
def test_optional_params(param_name, value, extra_params, contract_context):
    adds = {param_name: value}
    adds.update(extra_params)
    contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=param_name, optional=True)


@pytest.mark.parametrize('contract_context',
                         is_faxed_default_contracts,
                         ids=lambda cc: '{}'.format(cc.name))
@pytest.mark.parametrize('param_name, default_value, extra_params',
                         [('IS_FAXED', to_iso(NOW_NULLIFIED), {})],
                         ids=lambda: '')
def test_default_params(param_name, default_value, extra_params, contract_context):
    contracts_defaults.check_param(context=contract_context.new(), param_name=param_name,
                                   with_default=default_value)


@pytest.mark.parametrize('contract_context',
                         is_faxed_hidden_contracts,
                         ids=lambda p, v, ep: '{}'.format(p))
@pytest.mark.parametrize('param_name, value, extra_params',
                         [
                             ('IS_FAXED', to_iso(NOW_NULLIFIED), {}),
                         ],
                         ids=lambda a: '{}'.format(a))
def test_hidden_params(param_name, value, extra_params, contract_context):
    adds = {param_name: value}
    contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=adds.keys()[0],
                                   hidden=True)

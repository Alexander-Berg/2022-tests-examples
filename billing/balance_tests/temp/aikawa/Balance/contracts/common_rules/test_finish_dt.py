# -*- coding: utf-8 -*-
import datetime

import pytest

import btestlib.utils as utils
from btestlib.constants import ContractPaymentType
from temp.aikawa.Balance.contracts import contracts_rules as contracts_defaults
from temp.aikawa.Balance.contracts.contracts_rules import ContractException

all_contracts = contracts_defaults.all_contracts
finish_dt_optional_contracts = [contracts_defaults.NO_AGENCY, contracts_defaults.PR_AGENCY, contracts_defaults.OFFER,
                                contracts_defaults.USA_OPT_AGENCY, contracts_defaults.USA_OPT_CLIENT,
                                contracts_defaults.SW_OPT_AGENCY, contracts_defaults.SW_OPT_CLIENT,
                                contracts_defaults.BEL_NO_AGENCY, contracts_defaults.BEL_PR_AGENCY,
                                contracts_defaults.AUTO_NO_AGENCY, contracts_defaults.KZ_NO_AGENCY]

finish_dt_is_optional_when_post_pay = [contracts_defaults.OPT_CLIENT, contracts_defaults.TR_OPT_CLIENT,
                                       contracts_defaults.BRAND, contracts_defaults.GARANT_BEL,
                                       contracts_defaults.GARANT_RU, contracts_defaults.GARANT_KZT,
                                       contracts_defaults.LICENSE]

finish_dt_strictly_needed = contracts_defaults.diff(
    contracts_defaults.diff(all_contracts, finish_dt_optional_contracts), finish_dt_is_optional_when_post_pay)

to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)


@pytest.mark.parametrize('contract_context',
                         finish_dt_optional_contracts,
                         ids=lambda cc: '{}'.format(cc.name))
@pytest.mark.parametrize('param_name, value, extra_params',
                         [
                             ('FINISH_DT', to_iso(NOW_NULLIFIED), {'PAYMENT_TYPE': ContractPaymentType.POSTPAY}),
                             ('FINISH_DT', to_iso(NOW_NULLIFIED), {'PAYMENT_TYPE': ContractPaymentType.PREPAY})
                         ])
def test_optional_params_all_payment_type(param_name, value, extra_params, contract_context):
    adds = {param_name: value}
    adds.update(extra_params)
    contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=param_name, optional=True)


@pytest.mark.parametrize('contract_context',
                         finish_dt_is_optional_when_post_pay,
                         ids=lambda cc: '{}'.format(cc.name))
@pytest.mark.parametrize('param_name, exception_type, extra_params',
                         [

                             ('FINISH_DT', ContractException.FINISH_DT_EXCEPTION,
                              {'PAYMENT_TYPE': ContractPaymentType.PREPAY}),

                         ],
                         ids=lambda a: '{}'.format(a))
def test_strictly_needed_params_when_prepay(param_name, exception_type, extra_params, contract_context):
    adds = extra_params
    contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=param_name,
                                   strictly_needed=exception_type)


@pytest.mark.parametrize('contract_context',
                         finish_dt_is_optional_when_post_pay,
                         ids=lambda cc: '{}'.format(cc.name))
@pytest.mark.parametrize('param_name, value, extra_params',
                         [
                             ('FINISH_DT', to_iso(NOW_NULLIFIED), {'PAYMENT_TYPE': ContractPaymentType.POSTPAY})
                         ],
                         ids=lambda p, v, ep: '{}'.format(p))
def test_optional_params_when_post_pay(param_name, value, extra_params, contract_context):
    adds = {param_name: value}
    adds.update(extra_params)
    contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=param_name, optional=True)


@pytest.mark.parametrize('contract_context',
                         finish_dt_strictly_needed,
                         ids=lambda cc: '{}'.format(cc.name))
@pytest.mark.parametrize('param_name, exception_type, extra_params',
                         [
                             ('FINISH_DT', ContractException.FINISH_DT_EXCEPTION,
                              {'PAYMENT_TYPE': ContractPaymentType.POSTPAY}),

                             ('FINISH_DT', ContractException.FINISH_DT_EXCEPTION,
                              {'PAYMENT_TYPE': ContractPaymentType.PREPAY}),

                         ],
                         ids=lambda a: '{}'.format(a))
def test_strictly_needed_params_all_payment_type(param_name, exception_type, extra_params, contract_context):
    adds = extra_params
    contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=param_name,
                                   strictly_needed=exception_type)

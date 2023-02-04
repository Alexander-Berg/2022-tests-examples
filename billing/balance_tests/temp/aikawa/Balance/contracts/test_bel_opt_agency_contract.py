# -*- coding: utf-8 -*-
import itertools

import btestlib.utils as utils
from btestlib.constants import ContractCommissionType, PersonTypes
from temp.aikawa.Balance.contracts.contracts_rules import CommonContract, check_param, \
    PARAM_NEEDED_EXCEPTION

to_iso = utils.Date.date_to_iso_format

minimal_attrs = ['CLIENT_ID', 'DT', 'MANAGER_CODE', 'PAYMENT_TYPE',
                 'PERSON_ID', 'SERVICES', 'FINISH_DT']

CONTRACT_CONTEXT = CommonContract.new(type=ContractCommissionType.BEL_OPT_AGENCY,
                                      name='BEL_OPT_AGENCY',
                                      minimal_attrs=minimal_attrs,
                                      client_contract=True,
                                      person_type=PersonTypes.BYU)


def group_by_contract_state(data):
    data = sorted(data, key=lambda d: d[2])
    return itertools.groupby(data, lambda d: d[2])


def test_minimal_attrs_set():
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='COMMISSION', changeable=True)
    for param in minimal_attrs:
        check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param,
                    strictly_needed=PARAM_NEEDED_EXCEPTION[param])

# # -*- coding: utf-8 -*-
# from temp.aikawa.Balance.contracts import contracts_rules as contracts_defaults
# import pytest
# import datetime
# import btestlib.utils as utils
# from balance import balance_steps as steps
# from btestlib.constants import Paysyses, PersonTypes, Services, Products, ContractCommissionType, Firms, \
#     ContractPaymentType, Regions, Currencies, ClientCategories, BrandType
# from temp.aikawa.Balance.contracts.contracts_rules import ContractException
#
# to_iso = utils.Date.date_to_iso_format
#
# CONTRACT_CONTEXT = contracts_defaults.BEL_OPT_AGENCY
#
# fill_attrs = contracts_defaults.fill_attrs
#
# NOW = datetime.datetime.now()
# NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
# NOW_NULLIFIED_ISO = utils.Date.date_to_iso_format(NOW_NULLIFIED)
# TOMORROW = NOW + datetime.timedelta(days=1)
# TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)
# TOMORROW_NULLIFIED_ISO = utils.Date.date_to_iso_format(TOMORROW_NULLIFIED)
# HALF_YEAR_AFTER = NOW + datetime.timedelta(days=180)
#
#
# @pytest.mark.parametrize('param_name, value, extra_params',
#                          [
#                              ('IS_FAXED', to_iso(NOW_NULLIFIED), {}),
#                              ('IS_SUSPENDED', to_iso(NOW_NULLIFIED), {}),
#                          ],
#                          ids=lambda p, v, ep: '{}'.format(p))
# def test_optional_params(param_name, value, extra_params):
#     adds = {param_name: value}
#     adds.update(extra_params)
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name, optional=True)
#
#
# @pytest.mark.parametrize('param_name, exception_type, extra_params',
#                          [
#                              ('SERVICES', ContractException.SERVICE_NEEDED_EXCEPTION, {}),
#                              ('PAYMENT_TYPE', ContractException.PAYMENT_TYPE_NEEDED_EXCEPTION, {}),
#                              ('MANAGER_CODE', ContractException.MANAGER_NEEDED_EXCEPTION, {}),
#                              ('PERSON_ID', ContractException.PERSON_NEEDED_EXCEPTION, {}),
#                              ('FINISH_DT', ContractException.FINISH_DT_EXCEPTION,
#                               {'PAYMENT_TYPE': ContractPaymentType.POSTPAY}),
#                              ('FINISH_DT', ContractException.FINISH_DT_EXCEPTION,
#                               {'PAYMENT_TYPE': ContractPaymentType.PREPAY}),
#                          ],
#                          ids=lambda a: '{}'.format(a))
# def test_strictly_needed_params(param_name, exception_type, extra_params):
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT, param_name=param_name,
#                                    strictly_needed=exception_type)
#
#
# @pytest.mark.parametrize('param_name, value, exception_type, extra_params',
#                          [
#                              ('IS_SUSPENDED', to_iso(TOMORROW_NULLIFIED), ContractException.IS_SUSPENDED_EXCEPTION, {})
#                          ],
#                          ids=lambda p, v, ep: '{}'.format(p))
# def test_denied_value_params(param_name, value, exception_type, extra_params):
#     adds = {param_name: value}
#     adds.update(extra_params)
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name,
#                                    with_exception=exception_type)
#
#
# @pytest.mark.parametrize('param_name, default_value, extra_params',
#                          [('FIRM', Firms.REKLAMA_BEL_27.id, {})])
# def test_default_params(param_name, default_value, extra_params):
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(), param_name=param_name,
#                                    with_default=default_value)

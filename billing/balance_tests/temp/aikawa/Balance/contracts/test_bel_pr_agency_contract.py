# -*- coding: utf-8 -*-
import itertools

import btestlib.utils as utils
from btestlib.constants import ContractCommissionType, PersonTypes
from temp.aikawa.Balance.contracts.contracts_rules import CommonContract, check_param, \
    PARAM_NEEDED_EXCEPTION

to_iso = utils.Date.date_to_iso_format

minimal_attrs = ['CLIENT_ID', 'DT', 'MANAGER_CODE', 'PAYMENT_TYPE',
                 'PERSON_ID', 'SERVICES']

CONTRACT_CONTEXT = CommonContract.new(type=ContractCommissionType.BEL_PR_AGENCY,
                                      name='BEL_PR_AGENCY',
                                      minimal_attrs=minimal_attrs,
                                      client_contract=False,
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
# from temp.aikawa.Balance.contracts.contracts_rules import ContractException
# import pytest
# import datetime
# import btestlib.utils as utils
# from balance import balance_steps as steps
# from btestlib.constants import Paysyses, PersonTypes, Services, Products, ContractCommissionType, Firms, \
#     ContractPaymentType, Regions, Currencies, ClientCategories, BrandType
#
# to_iso = utils.Date.date_to_iso_format
#
# CONTRACT_CONTEXT = contracts_defaults.BEL_PR_AGENCY
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
# @pytest.mark.parametrize('adds', [{'IS_FAXED': to_iso(NOW_NULLIFIED)}],
#                          ids=lambda a: '{}'.format(a))
# def test_optional_params(adds):
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=adds.keys()[0], optional=True)
#
#
# @pytest.mark.parametrize('param_name, exception_type, extra_params',
#                          [
#                              ('SERVICES', ContractException.SERVICE_NEEDED_EXCEPTION, {}),
#                              ('PAYMENT_TYPE', ContractException.PAYMENT_TYPE_NEEDED_EXCEPTION, {}),
#                              ('MANAGER_CODE', ContractException.MANAGER_NEEDED_EXCEPTION, {}),
#                              ('PERSON_ID', ContractException.PERSON_NEEDED_EXCEPTION, {})],
#                          ids=lambda a: '{}'.format(a))
# def test_strictly_needed_params(param_name, exception_type, extra_params):
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT, param_name=param_name,
#                                    strictly_needed=exception_type)
#
#
# @pytest.mark.parametrize('payment_type', [ContractPaymentType.POSTPAY,
#                                           ContractPaymentType.PREPAY],
#                          ids=lambda a: '{}'.format(a))
# def test_finish_dt_is_optional_all_payment_type(payment_type):
#     CONTRACT_CONTEXT.payment_type = payment_type
#     adds = {'FINISH_DT': to_iso(NOW_NULLIFIED)}
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='FINISH_DT', optional=True)
#
#
# @pytest.mark.parametrize('suspended_dt', [TOMORROW_NULLIFIED,
#                                           NOW_NULLIFIED],
#                          ids=lambda a: '{}'.format(a))
# def test_is_suspended_value_cannot_be_future_date(suspended_dt):
#     adds = {'IS_SUSPENDED': to_iso(suspended_dt)}
#     if suspended_dt == TOMORROW_NULLIFIED:
#         contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='IS_SUSPENDED',
#                                        with_exception=contracts_defaults.ContractException.IS_SUSPENDED_EXCEPTION)
#     else:
#         contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='IS_SUSPENDED',
#                                        optional=True)
#
#
# @pytest.mark.parametrize('default_firm', [Firms.REKLAMA_BEL_27],
#                          ids=lambda a: '{}'.format(CONTRACT_CONTEXT.name))
# def test_firm_is_default(default_firm):
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(), param_name='FIRM',
#                                    with_default=default_firm.id)
#
#
# @pytest.mark.parametrize('default_firm',
#                          [Firms.REKLAMA_BEL_27],
#                          ids=lambda a: '{}{}'.format(CONTRACT_CONTEXT.name, a.id))
# def test_available_firms(default_firm):
#     adds = {'FIRM': default_firm.id}
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='FIRM',
#                                    changeble=True)

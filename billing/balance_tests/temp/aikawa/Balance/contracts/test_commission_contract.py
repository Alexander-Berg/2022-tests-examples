# -*- coding: utf-8 -*-
import itertools

import btestlib.utils as utils
from btestlib.constants import ContractCommissionType, PersonTypes
from temp.aikawa.Balance.contracts.contracts_rules import CommonContract, check_param, \
    PARAM_NEEDED_EXCEPTION

to_iso = utils.Date.date_to_iso_format

minimal_attrs = ['CLIENT_ID', 'PERSON_ID', 'MANAGER_CODE', 'PAYMENT_TYPE', 'SERVICES', 'COMMISSION_TYPE', 'DT',
                 'FINISH_DT']

CONTRACT_CONTEXT = CommonContract.new(type=ContractCommissionType.COMMISS,
                                      name='COMMISS',
                                      minimal_attrs=minimal_attrs,
                                      client_contract=False,
                                      person_type=PersonTypes.UR)


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
# from btestlib.constants import Firms, ContractPaymentType, CommissionType
#
# to_iso = utils.Date.date_to_iso_format
#
# CONTRACT_CONTEXT = contracts_defaults.COMMISS
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
#                          ids=lambda a: '{}'.format(a))
# def test_optional_params(param_name, value, extra_params):
#     adds = {param_name: value}
#     adds.update(extra_params)
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name, optional=True)
#
#
# @pytest.mark.parametrize('param_name, exception_type, extra_params',
#                          [
#                              ('COMMISSION_TYPE', ContractException.CHOOSE_COMMISSION_EXCEPTION, {}),
#                              ('FINISH_DT', ContractException.FINISH_DT_EXCEPTION,
#                               {'PAYMENT_TYPE': ContractPaymentType.POSTPAY}),
#
#                              ('FINISH_DT', ContractException.FINISH_DT_EXCEPTION,
#                               {'PAYMENT_TYPE': ContractPaymentType.PREPAY}),
#                              ('SERVICES', ContractException.SERVICE_NEEDED_EXCEPTION, {}),
#                              ('PAYMENT_TYPE', ContractException.PAYMENT_TYPE_NEEDED_EXCEPTION, {}),
#                              ('MANAGER_CODE', ContractException.MANAGER_NEEDED_EXCEPTION, {}),
#                              ('PERSON_ID', ContractException.PERSON_NEEDED_EXCEPTION, {})],
#                          ids=lambda a: '{}'.format(a))
# def test_strictly_needed_params(param_name, exception_type, extra_params):
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=extra_params), param_name=param_name,
#                                    strictly_needed=exception_type)
#
#
# @pytest.mark.parametrize('param_name, value, exception_type, extra_params',
#                          [
#                              ('IS_SUSPENDED', to_iso(TOMORROW_NULLIFIED), ContractException.IS_SUSPENDED_EXCEPTION, {}),
#                              ('COMMISSION_TYPE', CommissionType.KZ, ContractException.WRONG_COMMISSION_EXCEPTION, {})
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
#                          [('FIRM', Firms.YANDEX_1.id, {}),
#                           # https://st.yandex-team.ru/BALANCE-27893
#                           # ('COMMISSION_CHARGE_TYPE', 1, {}),
#                           # ('DISCARD_NDS', 1, {})
#                           ])
# def test_default_params(param_name, default_value, extra_params):
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(), param_name=param_name,
#                                    with_default=default_value)
#
#
# available_firms = [Firms.YANDEX_1.id, Firms.KINOPOISK_9.id, Firms.VERTICAL_12.id, Firms.TAXI_13.id, Firms.OFD_18.id,
#                    Firms.ZEN_28.id, Firms.MARKET_111.id, Firms.CLOUD_112.id, Firms.AUTOBUS_113.id, Firms.HEALTH_114.id]
#
# available_comission_type = [CommissionType.INDIVIDUAL_CONDITION,
#                             # CommissionType.AUTO, https://st.yandex-team.ru/BALANCE-27857
#                             CommissionType.BASE, CommissionType.MARKET_CPC_CPA,
#                             CommissionType.IMHO, CommissionType.UK_BASE, CommissionType.NON_RESIDENT,
#                             CommissionType.UK_MARKET_CPC_CPA,
#                             CommissionType.MEDIA_IMHO, CommissionType.HANDBOOK, CommissionType.UK,
#                             CommissionType.PROFESSIONAL, CommissionType.SPECIAL_MEDIA,
#                             CommissionType.NEYM]
#
#
# @pytest.mark.parametrize('param_name, value_list, extra_params',
#                          [
#                              ('FIRM', available_firms, {}),
#                              ('COMMISSION_TYPE', available_comission_type, {})
#
#                          ],
#                          ids=lambda p, v, ep: '{}'.format(p))
# def test_changeable_params(param_name, value_list, extra_params):
#     for value in value_list:
#         adds = extra_params
#         adds.update({param_name: value})
#         contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name,
#                                        changeable=True)
#
#
# non_available_firms = [Firms.YANDEX_UA_2.id, Firms.KAZNET_3, Firms.YANDEX_INC_4, Firms.BELFACTA_5, Firms.EUROPE_BV_6,
#                        Firms.EUROPE_AG_7, Firms.YANDEX_TURKEY_8, Firms.AUTO_RU_10, Firms.AUTO_RU_AG_14,
#                        Firms.SERVICES_AG_16, Firms.TAXI_BV_22, Firms.TAXI_UA_23, Firms.TAXI_KAZ_24, Firms.KZ_25,
#                        Firms.TAXI_AM_26, Firms.REKLAMA_BEL_27, Firms.YANDEX_NV_29]
#
# non_available_commission_type = [CommissionType.KZ]
#
#
# @pytest.mark.parametrize('param_name, value_list, default_value, extra_params',
#                          [
#                              ('FIRM', non_available_firms, Firms.YANDEX_1.id, {})
#                          ],
#                          ids=lambda p, v, ep: '{}'.format(p))
# def test_denied_value_params_default_value_set(param_name, value_list, default_value, extra_params):
#     for value in value_list:
#         adds = {param_name: value}
#         adds.update(extra_params)
#         contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name,
#                                        with_default=default_value)
#
#

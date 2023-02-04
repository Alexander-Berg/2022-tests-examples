# -*- coding: utf-8 -*-
import itertools

import btestlib.utils as utils
from btestlib.constants import ContractCommissionType
from temp.aikawa.Balance.contracts.contracts_rules import CommonContract, check_param, \
    PARAM_NEEDED_EXCEPTION

to_iso = utils.Date.date_to_iso_format

minimal_attrs = ['CLIENT_ID', 'DT', 'MANAGER_CODE', 'PAYMENT_TYPE',
                 'PERSON_ID', 'SERVICES', 'FINISH_DT']

CONTRACT_CONTEXT = CommonContract.new(type=ContractCommissionType.BEL_NO_AGENCY,
                                      name='BEL_NO_AGENCY',
                                      minimal_attrs=minimal_attrs,
                                      client_contract=True)


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
# from btestlib.constants import Firms, ContractPaymentType
# from temp.aikawa.Balance.contracts.contracts_rules import ContractException
#
# to_iso = utils.Date.date_to_iso_format
#
# CONTRACT_CONTEXT = contracts_defaults.BEL_NO_AGENCY
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
#                              ('FINISH_DT', to_iso(NOW_NULLIFIED), {'PAYMENT_TYPE': ContractPaymentType.POSTPAY}),
#                              ('FINISH_DT', to_iso(NOW_NULLIFIED), {'PAYMENT_TYPE': ContractPaymentType.PREPAY}),
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
#                              ('PERSON_ID', ContractException.PERSON_NEEDED_EXCEPTION, {})],
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
#
#
# non_available_firms = [Firms.YANDEX_UA_2.id, Firms.KAZNET_3.id, Firms.YANDEX_INC_4.id, Firms.BELFACTA_5.id,
#                        Firms.EUROPE_BV_6.id, Firms.EUROPE_AG_7.id, Firms.YANDEX_TURKEY_8.id, Firms.AUTO_RU_10.id,
#                        Firms.AUTO_RU_AG_14.id, Firms.SERVICES_AG_16.id, Firms.TAXI_BV_22.id, Firms.TAXI_KAZ_24.id,
#                        Firms.KZ_25.id, Firms.TAXI_AM_26.id, Firms.YANDEX_NV_29.id,
#                        Firms.YANDEX_1.id, Firms.KINOPOISK_9.id, Firms.VERTICAL_12.id, Firms.TAXI_13.id,
#                        Firms.OFD_18.id, Firms.ZEN_28.id, Firms.MARKET_111.id, Firms.CLOUD_112.id, Firms.AUTOBUS_113.id,
#                        Firms.HEALTH_114.id]
#
#
# @pytest.mark.parametrize('param_name, value_list, default_value, extra_params',
#                          [
#                              ('FIRM', non_available_firms, Firms.REKLAMA_BEL_27.id, {})
#                          ],
#                          ids=lambda p, v, ep: '{}'.format(p))
# def test_denied_value_params_default_value_set(param_name, value_list, default_value, extra_params):
#     for value in value_list:
#         adds = {param_name: value}
#         adds.update(extra_params)
#         contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name,
#                                        with_default=default_value)

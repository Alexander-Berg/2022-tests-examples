# -*- coding: utf-8 -*-
import datetime
import itertools

import pytest

import btestlib.utils as utils
from btestlib.constants import ContractCommissionType
from temp.aikawa.Balance.contracts.contracts_rules import ContractException, CommonContract, check_param, \
    PARAM_NEEDED_EXCEPTION

to_iso = utils.Date.date_to_iso_format

NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
NOW_NULLIFIED_ISO = utils.Date.date_to_iso_format(NOW_NULLIFIED)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)
TOMORROW_NULLIFIED_ISO = utils.Date.date_to_iso_format(TOMORROW_NULLIFIED)
HALF_YEAR_AFTER = NOW + datetime.timedelta(days=180)

minimal_attrs = ['CLIENT_ID', 'DT', 'MANAGER_CODE', 'PAYMENT_TYPE',
                 'PERSON_ID', 'SERVICES', 'FINISH_DT']

CONTRACT_CONTEXT = CommonContract.new(type=ContractCommissionType.AUTO_OPT_AGENCY_PREM,
                                      name='AUTO_OPT_AGENCY_PREM',
                                      minimal_attrs=minimal_attrs,
                                      client_contract=False)


def group_by_contract_state(data):
    data = sorted(data, key=lambda d: d[2])
    return itertools.groupby(data, lambda d: d[2])


def test_minimal_attrs_set():
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='COMMISSION', changeable=True)
    for param in minimal_attrs:
        check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param,
                    strictly_needed=PARAM_NEEDED_EXCEPTION[param])


@pytest.mark.parametrize('contract_state, restricted_param, exception',
                         [({}, {'IS_SUSPENDED': to_iso(TOMORROW_NULLIFIED)},
                           ContractException.IS_SUSPENDED_EXCEPTION)]
                         )
def test_restricted_params(contract_state, restricted_param, exception):
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    adds.update(contract_state)
    adds.update(restricted_param)
    check_param(context=CONTRACT_CONTEXT.new(adds=adds),
                with_exception=exception)

    # @pytest.mark.parametrize('param_name, value, extra_params',

#                          [
#                              ('IS_FAXED', to_iso(NOW_NULLIFIED), {}),
#                              ('IS_SUSPENDED', to_iso(NOW_NULLIFIED), {}),
#                              ('AUTORU_Q_PLAN', 1,
#                               {'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': AwardsScaleType.AUTO_RU})
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
#
#                              ('FINISH_DT', ContractException.FINISH_DT_EXCEPTION,
#                               {'PAYMENT_TYPE': ContractPaymentType.POSTPAY}),
#
#                              ('FINISH_DT', ContractException.FINISH_DT_EXCEPTION,
#                               {'PAYMENT_TYPE': ContractPaymentType.POSTPAY}),
#
#                          ],
#                          ids=lambda a: '{}'.format(a))
# def test_strictly_needed_params(param_name, exception_type, extra_params):
#     adds = extra_params
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name,
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
#                          [
#                              ('FIRM', Firms.VERTICAL_12.id, {}),
#                              ('WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE', AwardsScaleType.AUTO_RU, {}),
#                          ])
# def test_default_params(param_name, default_value, extra_params):
#     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(), param_name=param_name,
#                                    with_default=default_value)
#
#
# non_available_firms = [Firms.YANDEX_UA_2.id, Firms.KAZNET_3.id, Firms.YANDEX_INC_4.id, Firms.BELFACTA_5.id,
#                        Firms.EUROPE_BV_6.id, Firms.EUROPE_AG_7.id, Firms.YANDEX_TURKEY_8.id, Firms.AUTO_RU_10.id,
#                        Firms.AUTO_RU_AG_14.id, Firms.SERVICES_AG_16.id, Firms.TAXI_BV_22.id, Firms.TAXI_KAZ_24.id,
#                        Firms.KZ_25.id, Firms.TAXI_AM_26.id, Firms.YANDEX_NV_29.id,
#                        Firms.YANDEX_1.id, Firms.KINOPOISK_9.id, Firms.REKLAMA_BEL_27.id, Firms.TAXI_13.id,
#                        Firms.OFD_18.id, Firms.ZEN_28.id, Firms.MARKET_111.id, Firms.CLOUD_112.id, Firms.AUTOBUS_113.id,
#                        Firms.HEALTH_114.id]
#
# non_available_award_scale = [AwardsScaleType.BASE_2015, AwardsScaleType.PREMIUM_2015, AwardsScaleType.SPEC_PROJECT,
#                              AwardsScaleType.AUTO, AwardsScaleType.GEO,
#                              AwardsScaleType.BASE_REGIONS_2015, AwardsScaleType.AUTO_RU_2015,
#                              AwardsScaleType.REALTY_REGIONS_2017,
#                              AwardsScaleType.AUDIO_ADV_2015, AwardsScaleType.MARKET_2016,
#                              AwardsScaleType.MARKET_REGIONS_2016, AwardsScaleType.REALTY_2017,
#                              AwardsScaleType.MEDIA_VERTICAL_2017,
#                              AwardsScaleType.RADIO_2017, AwardsScaleType.INTERCO_2017,
#                              AwardsScaleType.BEL_PREM, AwardsScaleType.OFD_WO_COUNT,
#                              AwardsScaleType.BASE_SPB, AwardsScaleType.MARKET_SPEC_PROJECT]
#
#
# @pytest.mark.parametrize('param_name, value_list, default_value, extra_params',
#                          [
#                              ('WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE', non_available_award_scale,
#                               AwardsScaleType.AUTO_RU, {}),
#                              ('FIRM', non_available_firms, Firms.VERTICAL_12.id, {})
#                          ],
#                          ids=lambda p, v, ep: '{}'.format(p))
# def test_denied_value_params_default_value_set(param_name, value_list, default_value, extra_params):
#     for value in value_list:
#         adds = {param_name: value}
#         adds.update(extra_params)
#         contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name,
#                                        with_default=default_value)

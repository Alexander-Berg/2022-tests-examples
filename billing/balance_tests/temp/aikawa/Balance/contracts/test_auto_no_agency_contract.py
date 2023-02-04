# -*- coding: utf-8 -*-
import datetime
import itertools

import pytest

import btestlib.utils as utils
from btestlib.constants import ContractCommissionType, Firms
from simpleapi.matchers.deep_equals import deep_contains
from temp.aikawa.Balance.contracts.contracts_rules import ContractException, CommonContract, check_param, \
    PARAM_NEEDED_EXCEPTION, collect_contract_params

to_iso = utils.Date.date_to_iso_format

NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
NOW_NULLIFIED_ISO = utils.Date.date_to_iso_format(NOW_NULLIFIED)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)
TOMORROW_NULLIFIED_ISO = utils.Date.date_to_iso_format(TOMORROW_NULLIFIED)
HALF_YEAR_AFTER = NOW + datetime.timedelta(days=180)

minimal_attrs = ['CLIENT_ID', 'DT', 'MANAGER_CODE', 'PAYMENT_TYPE',
                 'PERSON_ID', 'SERVICES']

CONTRACT_CONTEXT = CommonContract.new(type=ContractCommissionType.AUTO_NO_AGENCY,
                                      name='AUTO_NO_AGENCY',
                                      minimal_attrs=minimal_attrs)


def group_by_contract_state(data):
    data = sorted(data, key=lambda d: d[2])
    return itertools.groupby(data, lambda d: d[2])


def test_minimal_attrs_set():
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='COMMISSION', changeable=True)
    for param in minimal_attrs:
        check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param,
                    strictly_needed=PARAM_NEEDED_EXCEPTION[param])


@pytest.mark.parametrize('contract_state, default_params',
                         [({}, {'FIRM': Firms.VERTICAL_12.id})]
                         )
def test_default_value(contract_state, default_params):
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    adds.update(contract_state)
    contract_id, contract_params = check_param(context=CONTRACT_CONTEXT.new(adds=adds), without_check=True)
    saved_contract_params = collect_contract_params(contract_id)
    utils.check_that(saved_contract_params, deep_contains(default_params))


@pytest.mark.parametrize('contract_state, restricted_param, exception',
                         [({}, {'IS_SUSPENDED': to_iso(TOMORROW_NULLIFIED)}, ContractException.IS_SUSPENDED_EXCEPTION)]
                         )
def test_restricted_params(contract_state, restricted_param, exception):
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    adds.update(contract_state)
    adds.update(restricted_param)
    check_param(context=CONTRACT_CONTEXT.new(adds=adds), with_exception=exception)



    # @pytest.mark.parametrize('param_name, value, extra_params',
    #                          [
    #                              ('IS_SUSPENDED', to_iso(NOW_NULLIFIED), {}),
    #                              ('IS_FAXED', to_iso(NOW_NULLIFIED), {}),
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
    #                              ('PERSON_ID', ContractException.PERSON_NEEDED_EXCEPTION, {}),
    #                              ('MANAGER_CODE', ContractException.MANAGER_NEEDED_EXCEPTION, {})
    #                          ],
    #                          ids=lambda a: '{}'.format(a))
    # def test_strictly_needed_params(param_name, exception_type, extra_params):
    #     contracts_defaults.check_param(context=CONTRACT_CONTEXT, param_name=param_name,
    #                                    strictly_needed=exception_type)
    #
    #
    # available_person_types = [PersonTypes.BYP, PersonTypes.YTPH, PersonTypes.YT_KZU, PersonTypes.YT_KZP, PersonTypes.YT,
    #                           PersonTypes.UR, PersonTypes.PH]
    #
    #
    # @pytest.mark.parametrize('param_name, value_list, extra_params',
    #                          [
    #                              ('PERSON_ID', available_person_types, {})
    #                          ],
    #                          ids=lambda p, v, ep: '{}'.format(p))
    # def test_changeable_params(param_name, value_list, extra_params):
    #     for value in value_list:
    #         if param_name == 'PERSON_ID':
    #             client_id = steps.ClientSteps.create()
    #             person_id = steps.PersonSteps.create(client_id, value.code)
    #             adds = {'CLIENT_ID': client_id, 'PERSON_ID': person_id}
    #         else:
    #             adds = {param_name: value}
    #         adds.update(extra_params)
    #         contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name,
    #                                        changeable=True)
    #
    #
    # @pytest.mark.parametrize('param_name, default_value, extra_params',
    #                          [('FIRM', Firms.VERTICAL_12.id, {})])
    # def test_default_params(param_name, default_value, extra_params):
    #     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(), param_name=param_name,
    #                                    with_default=default_value)
    #
    #
    # non_available_person_types = [PersonTypes.BYP, PersonTypes.BYU, PersonTypes.USU, PersonTypes.USP, PersonTypes.SW_UR,
    #                               PersonTypes.SW_YT, PersonTypes.SW_YTPH, PersonTypes.SW_PH, PersonTypes.BY_YTPH,
    #                               PersonTypes.TRP, PersonTypes.TRU, PersonTypes.KZU, PersonTypes.KZP, PersonTypes.AM_PH,
    #                               PersonTypes.AM_UR]


    # @pytest.mark.parametrize('param_name, value_list, exception_type, extra_params',
    #                          [
    #                              ('PERSON_ID', non_available_person_types,
    #                               ContractException.WRONG_PERSON_CATEGORY_EXCEPTION,
    #                               {}),
    #                              ('IS_SUSPENDED', [to_iso(TOMORROW_NULLIFIED)],
    #                               ContractException.IS_SUSPENDED_EXCEPTION, {})
    #                          ],
    #                          ids=lambda p, v, ep: '{}'.format(p))
    # def test_denied_value_params(param_name, value_list, exception_type, extra_params):
    #     for value in value_list:
    #         if param_name == 'PERSON_ID':
    #             client_id = steps.ClientSteps.create()
    #             person_id = steps.PersonSteps.create(client_id, value.code)
    #             adds = {'CLIENT_ID': client_id, 'PERSON_ID': person_id}
    #         else:
    #             adds = {param_name: value}
    #         adds.update(extra_params)
    #     contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name,
    #                                    with_exception=exception_type)
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
    #
    # @pytest.mark.parametrize('param_name, value_list, default_value, extra_params',
    #                          [
    #                              ('FIRM', non_available_firms, Firms.VERTICAL_12.id, {})
    #                          ],
    #                          ids=lambda p, v, ep: '{}'.format(p))
    # def test_denied_value_params_default_value_set(param_name, value_list, default_value, extra_params):
    #     for value in value_list:
    #         adds = {param_name: value}
    #         adds.update(extra_params)
    #         contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name,
    #                                        with_default=default_value)

# -*- coding: utf-8 -*-
__author__ = 'atkaya'

from hamcrest import empty

import pytest

from btestlib import utils
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib.constants import Services
from btestlib.data.partner_contexts import *

CASES = [
    (CLOUD_MARKETPLACE_CONTEXT, None),
    pytest.param(CORP_TAXI_RU_CONTEXT_SPENDABLE, {'services': [Services.TAXI_CORP_PARTNERS.id]},
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))),
    pytest.param(CORP_TAXI_RU_CONTEXT_SPENDABLE, {'services': [Services.TAXI_CORP_PARTNERS.id, Services.TAXI_CORP.id]},
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))),
    pytest.param(CORP_TAXI_KZ_CONTEXT_SPENDABLE, {'services': [Services.TAXI_CORP_PARTNERS.id, Services.TAXI_CORP.id]},
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))),
    pytest.param(CORP_TAXI_ARM_CONTEXT_SPENDABLE, {'services': [Services.TAXI_CORP_PARTNERS.id, Services.TAXI_CORP.id]},
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))),
    pytest.param(CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP, {'services': [Services.TAXI_CORP_PARTNERS.id]},
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))),
    (BLUE_MARKET_SUBSIDY, None),
    (ZEN_SPENDABLE_CONTEXT, None),
    (INGAME_PURCHASES_CONTEXT, None),
    (CLOUD_REFERAL_CONTEXT, None),
    (NEWS_CONTEXT_SPENDABLE_PH, None),
]

CASES += [(fc.courier_spendable, None) for fc in FOOD_CONTEXTS]

CASES_WITH_LINKED_CONTRACT = [
    (REFUELLER_SPENDABLE_CONTEXT, REFUELLER_CONTEXT),
    (SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT),
    (TELEMEDICINE_SPENDABLE_CONTEXT, TELEMEDICINE_CONTEXT),
    (TAXI_RU_CONTEXT_SPENDABLE, TAXI_RU_CONTEXT),
    (TAXI_UBER_BV_BY_BYN_CONTEXT_SPENDABLE, TAXI_UBER_BV_BY_BYN_CONTEXT),
    (TAXI_UBER_BV_BYN_BY_BYN_CONTEXT_SPENDABLE, TAXI_UBER_BV_BYN_BY_BYN_CONTEXT),
    (TAXI_UBER_BV_AZN_USD_CONTEXT_SPENDABLE, TAXI_UBER_BV_AZN_USD_CONTEXT),
    (TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE, TAXI_UBER_BV_BYN_AZN_USD_CONTEXT),
    (TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, TAXI_BV_GEO_USD_CONTEXT),
    (TAXI_BV_LAT_EUR_CONTEXT_SPENDABLE, TAXI_BV_LAT_EUR_CONTEXT),
    (TAXI_ISRAEL_CONTEXT_SPENDABLE, TAXI_ISRAEL_CONTEXT),
    (TAXI_YANDEX_GO_SRL_CONTEXT_SPENDABLE, TAXI_YANDEX_GO_SRL_CONTEXT),
    (TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT_SPENDABLE, TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT),
    (TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT_SPENDABLE, TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT),
    (TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT_SPENDABLE, TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT),
    (TAXI_GHANA_USD_CONTEXT_SPENDABLE, TAXI_GHANA_USD_CONTEXT),
    (TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE, TAXI_BOLIVIA_USD_CONTEXT),
    (TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT),
    (TAXI_ZA_USD_CONTEXT_SPENDABLE, TAXI_ZA_USD_CONTEXT),
    (TAXI_UBER_BEL_BYN_CONTEXT_SPENDABLE, TAXI_UBER_BEL_BYN_CONTEXT),
    (TAXI_UBER_BEL_BYN_CONTEXT_NDS_SPENDABLE, TAXI_UBER_BEL_BYN_CONTEXT_NDS),
]

start_dt, _ = utils.Date.previous_month_first_and_last_days()

pytestmark = [reporter.feature(Features.PARTNER_ACT, Features.PARTNER, Features.ACT, Features.SPENDABLE)]


@pytest.mark.parametrize('context, spec_params', CASES, ids=lambda *args: args[0].name)
def test_spendable_acts_wo_data(context, spec_params):
    params = {'start_dt': start_dt}
    if spec_params:
        params.update(spec_params)
    partner_integration_params = (None if getattr(context, 'partner_integration', None) is None else
                                  steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT)
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params=params, partner_integration_params=partner_integration_params, )

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), 'Проверяем, что акты не сгенерились и генерация не упала')


@pytest.mark.parametrize('context_spendable, context_general', CASES_WITH_LINKED_CONTRACT, ids=lambda context, c: context.name)
def test_spendable_acts_wo_data_with_linked_contract(context_spendable, context_general):
    client_id, _, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context_general, additional_params={'start_dt': start_dt})

    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context_spendable, client_id=client_id,
        additional_params={'start_dt': start_dt,
                           'link_contract_id': contract_id})

    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    utils.check_that(act_data, empty(), 'Проверяем, что акты не сгенерились и генерация не упала')

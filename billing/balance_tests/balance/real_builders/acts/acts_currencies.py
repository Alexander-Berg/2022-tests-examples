# -*- coding: utf-8 -*-

from . import steps
from balance import balance_steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, ContractCommissionType, Paysyses, PersonTypes, Regions, Products
from btestlib.data.partner_contexts import TAXI_AZARBAYCAN_CONTEXT, TAXI_ARM_CONTEXT, \
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED, TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT
from .. import common_defaults

_, _, contract_start_dt, act_dt_1, _, act_dt_2 = utils.Date.previous_three_months_start_end_dates()

CONTEXT = steps.CONTEXT


def test_rub_act():
    client_id, _, _, _, _, external_act_id, _ = steps.create_base_act(context=CONTEXT)
    return client_id, external_act_id


def test_usd_act():
    context = Contexts.DIRECT_FISH_USD_CONTEXT.new(person_params=common_defaults.FIXED_USU_PARAMS)
    client_id, _, _, _, _, external_act_id, _ = steps.create_base_act(context=context)
    return client_id, external_act_id


def test_byn_act():
    context = Contexts.DIRECT_BYN_BYU_CONTEXT.new(person_params=common_defaults.FIXED_BYU_PARAMS)
    client_id, _, _, _, _, external_act_id, _ = steps.create_base_act(context=context)
    return client_id, external_act_id


def test_kzt_act():
    context = Contexts.DIRECT_FISH_KZ_CONTEXT.new(person_params=common_defaults.FIXED_KZU_PARAMS)
    client_id, _, _, _, _, external_act_id, _ = steps.create_base_act(context=context)
    return client_id, external_act_id


def test_eur_act():
    context = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_params=common_defaults.FIXED_SW_UR_PARAMS)
    client_id, _, _, _, _, external_act_id, _ = steps.create_base_act(context=context)
    return client_id, external_act_id


def test_try_act():
    context = Contexts.DIRECT_FISH_TRY_CONTEXT.new(person_params=common_defaults.FIXED_TRU_PARAMS)
    client_id, _, _, _, _, external_act_id, _ = steps.create_base_act(context=context)
    return client_id, external_act_id


def test_chf_act():
    context = Contexts.DIRECT_FISH_SW_CHF_YT_CONTEXT.new(person_params=common_defaults.FIXED_SW_YT_PARAMS)
    client_id, _, _, _, _, external_act_id, _ = steps.create_base_act(context=context)
    return client_id, external_act_id


def test_uzs_act():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.BY_YTPH,
                                                   paysys=Paysyses.CC_YT_UZS, region=Regions.UZB,
                                                   product=Products.DIRECT_USD,
                                                   person_params=common_defaults.FIXED_BY_YTPH_PARAMS)
    client_id, _, _, _, _, external_act_id, _ = steps.create_base_act(context=context, turn_on=True, paid_invoice=False)
    return client_id, external_act_id


def test_ils_act():
    context = CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED.new(person_params=common_defaults.FIXED_IL_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            additional_params={'start_dt': contract_start_dt})

    _, external_act_id = steps.create_partner_act(context, client_id, contract_id, act_dt_1)
    return client_id, external_act_id


def test_ron_act():
    context = TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT.new(person_params=common_defaults.FIXED_EU_YT_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            additional_params={'start_dt': contract_start_dt})

    _, external_act_id = steps.create_partner_act(context, client_id, contract_id, act_dt_1, client_id=client_id,
                                                  person_id=person_id, contract_id=contract_id)
    return client_id, external_act_id


def test_arm_act():
    context = TAXI_ARM_CONTEXT.new(person_params=common_defaults.FIXED_AM_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            additional_params={'start_dt': contract_start_dt})

    _, external_act_id = steps.create_partner_act(context, client_id, contract_id, act_dt_1, client_id=client_id,
                                                  person_id=person_id, contract_id=contract_id)
    return client_id, external_act_id


def test_azn_act():
    context = TAXI_AZARBAYCAN_CONTEXT.new(person_params=common_defaults.FIXED_AZ_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            additional_params={'start_dt': contract_start_dt})

    _, external_act_id = steps.create_partner_act(context, client_id, contract_id, act_dt_1, client_id=client_id,
                                                  person_id=person_id, contract_id=contract_id)
    return client_id, external_act_id


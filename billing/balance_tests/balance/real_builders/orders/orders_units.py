# -*- coding: utf-8 -*-

from . import steps
from balance import balance_steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Currencies, Firms, Services, PersonTypes, Paysyses, Products, Managers, Regions
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, TAXI_UBER_BV_BY_BYN_CONTEXT
from .. import common_defaults

CONTEXT = steps.CONTEXT


def test_unit_0_order():
    client_id, _, _, _, service_id, service_order_id, _, _, _ = steps.create_base_request()
    return client_id, service_id, service_order_id


def test_unit_850_order():
    context = CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0)
    service_id = context.service.id
    service_order_id = balance_steps.OrderSteps.get_order_id_by_contract(contract_id, service_id)
    return client_id, service_id, service_order_id


def test_unit_862_order():
    context = Contexts.MUSIC_CONTEXT.new(product=Products.MUSIC_503356,
                                         manager=Managers.SOME_MANAGER,
                                         person_params=common_defaults.FIXED_PH_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_900_order():
    context = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_867_order():
    context = Contexts.MUSIC_CONTEXT.new(product=Products.MUSIC_505135,
                                         manager=Managers.SOME_MANAGER,
                                         person_params=common_defaults.FIXED_PH_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_909_order():
    context = Contexts.DIRECT_BYN_BYU_CONTEXT.new(product=Products.DIRECT_BYN,
                                                  person_params=common_defaults.FIXED_BYU_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_903_order():
    context = Contexts.DIRECT_MONEY_USD_CONTEXT.new(person_params=common_defaults.FIXED_USU_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_904_order():
    context = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(product=Products.DIRECT_EUR,
                                                    person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_910_order():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.OFD, product=Products.OFD_YEAR,
                                                   person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_914_order():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(product=Products.DIRECT_KZT_QUASI,
                                                   firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                   paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                   currency=Currencies.KZT,
                                                   person_params=common_defaults.FIXED_KZU_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_799_order():
    context = Contexts.ADFOX_CONTEXT.new(product=Products.ADFOX_505170,
                                         person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_852_order():
    context = CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED.new(person_params=common_defaults.FIXED_KZU_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0)
    service_id = context.service.id
    service_order_id = balance_steps.OrderSteps.get_order_id_by_contract(contract_id, service_id)
    return client_id, service_id, service_order_id


def test_unit_853_order():
    context = TAXI_UBER_BV_BY_BYN_CONTEXT.new(currency=Currencies.USD, paysys=Paysyses.BANK_UR_UBER_USD,
                                              person_params=common_defaults.FIXED_BYU_PARAMS)
    client_id, person_id, contract_id, _ = balance_steps.ContractSteps.create_partner_contract(context, is_postpay=0)
    service_id = Services.TAXI_111.id
    service_order_id = balance_steps.OrderSteps.get_order_id_by_contract(contract_id, service_id)
    return client_id, service_id, service_order_id


def test_unit_912_order():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.CONNECT, product=Products.CONNECT_508516,
                                                   person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_902_order():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT,
                                                   firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                   paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                   currency=Currencies.KZT,
                                                   person_params=common_defaults.FIXED_KZU_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_907_order():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_TRY,
                                                   firm=Firms.YANDEX_TURKEY_8, person_type=PersonTypes.TRU,
                                                   paysys=Paysyses.BANK_TR_UR_TRY, region=Regions.TR,
                                                   currency=Currencies.TRY,
                                                   person_params=common_defaults.FIXED_TRU_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_796_order():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.VZGLYAD, product=Products.VZGLYAD,
                                                   person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id


def test_unit_2_order():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.SHOP, product=Products.CARSHARING_508845,
                                                   person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    return client_id, service_id, service_order_id

# coding: utf-8

import datetime

import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils, reporter
from btestlib.constants import Services
from btestlib.matchers import contains_dicts_with_entries
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions

NOW = datetime.datetime.now()
YESTERDAY = NOW - datetime.timedelta(days=1)

MINIMAL_QTY = 1
QTY = 334

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
MARKET_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                               firm=Firms.YANDEX_1)
MARKET_MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                               firm=Firms.MARKET_111)
DIRECT_BEL_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                            firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                            paysys=Paysyses.BANK_BY_UR_BYN)
DIRECT_KZ_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                           firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                           paysys=Paysyses.BANK_KZ_UR_TG)

DIRECT_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_RUB,
                                                              region=Regions.RU, currency=Currencies.RUB)

DIRECT_BEL_FIRM_BYN = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN,
                                                           firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                           paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                           currency=Currencies.BYN)

DIRECT_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT_508892,
                                                          firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                          paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ)

DIRECT_QUASI_BEL_FIRM_BYN = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN_QUASI,
                                                                 firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                                 paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                                 currency=Currencies.BYN)

DIRECT_QUASI_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT_QUASI,
                                                                firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                                paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ)

AUTO_RU_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.VERTICAL_12, product=Products.AUTORU_505123,
                                                        region=Regions.RU, currency=Currencies.RUB,
                                                        service=Services.AUTORU)

AUTO_RU_FIRM_RUB_508999 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.VERTICAL_12, product=Products.AUTORU_508999,
                                                               region=Regions.RU, currency=Currencies.RUB,
                                                               service=Services.AUTORU)

GEO_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.GEO_509780,
                                                            currency=Currencies.RUB, service=Services.GEO)

GEO_BEL_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.GEO, product=Products.GEO_510792,
                                                         firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                         paysys=Paysyses.BANK_BY_UR_BYN, currency=Currencies.BYN)

GEO_KZ_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.GEO, product=Products.GEO_510794,
                                                        firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                        paysys=Paysyses.BANK_KZ_UR_TG, currency=Currencies.KZT)

VENDORS_MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.MARKET_111, product=Products.VENDOR,
                                                                currency=Currencies.RUB, service=Services.VENDORS)


def get_overdraft_object_id(firm_id, service_id, client_id):
    return str(firm_id * 10 + service_id * 100000 + client_id * 1000000000)


def create_client_overdraft_entry(context_list, client_id, limit, currency=None, iso_currency=None):
    return [{'currency': currency or None,
             'firm_id': context.firm.id,
             'overdraft_limit': limit,
             'client_id': client_id,
             'service_id': context.service.id,
             'iso_currency': iso_currency or None} for context in context_list]


@pytest.mark.parametrize('context, given_overdraft_params', [
    (DIRECT_KZ_FIRM_FISH, [DIRECT_KZ_FIRM_FISH]),
    (DIRECT_BEL_FIRM_FISH, [DIRECT_BEL_FIRM_FISH]),
    (DIRECT_YANDEX_FIRM_FISH, [DIRECT_YANDEX_FIRM_FISH]),
    (MARKET_MARKET_FIRM_FISH, [MARKET_MARKET_FIRM_FISH]),
    (AUTO_RU_FIRM_RUB, [AUTO_RU_FIRM_RUB]),
    (GEO_YANDEX_FIRM_FISH, [GEO_YANDEX_FIRM_FISH]),
    (GEO_BEL_FIRM_FISH, [GEO_BEL_FIRM_FISH]),
    (GEO_KZ_FIRM_FISH, [GEO_KZ_FIRM_FISH]),
    (VENDORS_MARKET_FIRM_FISH, [VENDORS_MARKET_FIRM_FISH]),
])
def test_overdraft_notification_fish_client(context, given_overdraft_params):
    client_id = steps.ClientSteps.create()

    steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, 2000, firm_id=context.firm.id)
    given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
    utils.check_that(given_limit,
                     contains_dicts_with_entries(create_client_overdraft_entry(context_list=given_overdraft_params,
                                                                               client_id=client_id,
                                                                               limit=2000),
                                                 same_length=len(given_overdraft_params)))
    for context in given_overdraft_params:
        object_id = get_overdraft_object_id(firm_id=context.firm.id, service_id=context.service.id,
                                            client_id=client_id)
        steps.CommonSteps.build_notification(11, object_id=object_id)


@pytest.mark.parametrize('context, given_overdraft_params', [
    (DIRECT_KZ_FIRM_KZU, [DIRECT_KZ_FIRM_KZU]),
    (DIRECT_BEL_FIRM_BYN, [DIRECT_BEL_FIRM_BYN]),
    (DIRECT_YANDEX_FIRM_RUB, [DIRECT_YANDEX_FIRM_RUB]),
    (DIRECT_QUASI_KZ_FIRM_KZU, [DIRECT_QUASI_KZ_FIRM_KZU]),
    (DIRECT_QUASI_BEL_FIRM_BYN, [DIRECT_QUASI_BEL_FIRM_BYN]),
])
def test_overdraft_notification_currency_client(context, given_overdraft_params):
    client_id = steps.ClientSteps.create()

    steps.OverdraftSteps.set_overdraft_fair(client_id, context, 100000, currency=True)
    given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
    # utils.check_that(given_limit,
    #                  contains_dicts_with_entries(create_client_overdraft_entry(context_list=given_overdraft_params,
    #                                                                            client_id=client_id,
    #                                                                            limit=8930,
    #                                                                            iso_currency=context.currency.iso_code,
    #                                                                            currency=context.currency.iso_code),
    #                                              same_length=len(given_overdraft_params)))
    for context in given_overdraft_params:
        object_id = get_overdraft_object_id(firm_id=context.firm.id, service_id=context.service.id,
                                            client_id=client_id)
        steps.CommonSteps.wait_and_get_notification(11, object_id, 1)


@pytest.mark.parametrize('context, given_overdraft_params, given_overdraft_params_after_recalc',
                         [(DIRECT_YANDEX_FIRM_FISH,
                           [DIRECT_YANDEX_FIRM_FISH],
                           [DIRECT_YANDEX_FIRM_FISH])])
def test_recalculate_after_migrate_to_currency_notify(context, given_overdraft_params,
                                                      given_overdraft_params_after_recalc):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.set_overdraft(client_id, context.service.id, QTY, firm_id=context.firm.id,
                                    start_dt=NOW,
                                    currency=None, invoice_currency=None)
    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)
    given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
    utils.check_that(given_limit,
                     contains_dicts_with_entries(create_client_overdraft_entry(context_list=given_overdraft_params,
                                                                               client_id=client_id,
                                                                               limit=330,
                                                                               currency=None,
                                                                               iso_currency=None),
                                                 same_length=len(given_overdraft_params)))

    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY', dt=YESTERDAY)
    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

    for context in given_overdraft_params_after_recalc:
        object_id = get_overdraft_object_id(firm_id=context.firm.id, service_id=context.service.id,
                                            client_id=client_id)
        overdraft_limit = steps.CommonSteps.wait_and_get_notification(11, object_id, 1)['OverdraftLimit']
        # ['args'][0]['overdraft_limit']
        # print overdraft_limit
        # while overdraft_limit <> '10020.0000000000':
        #     overdraft_limit = steps.CommonSteps.wait_and_get_notification(11, object_id, 1)['OverdraftLimit']


@pytest.mark.parametrize('context, given_overdraft_params', [
    # pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11)(
    (DIRECT_YANDEX_FIRM_FISH, [DIRECT_YANDEX_FIRM_FISH, DIRECT_BEL_FIRM_FISH])
    # )),
])
def test_overdraft_notification_fish_client_all_limits_null(context, given_overdraft_params):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.set_force_overdraft(client_id=client_id, firm_id=Firms.YANDEX_1.id, service_id=Services.DIRECT.id,
                                          limit=1000)
    steps.ClientSteps.set_force_overdraft(client_id=client_id, firm_id=Firms.REKLAMA_BEL_27.id,
                                          service_id=Services.DIRECT.id, limit=1000)
    given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
    utils.check_that(given_limit,
                     contains_dicts_with_entries(create_client_overdraft_entry(context_list=given_overdraft_params,
                                                                               client_id=client_id,
                                                                               limit=1000),
                                                 same_length=len(given_overdraft_params)))
    print steps.OverdraftSteps.get_limit_by_client(client_id)
    db.balance().execute('UPDATE t_client SET {param_name} = :param_value '
                         'WHERE id = :client_id'.format(param_name='overdraft_ban'),
                         {'param_value': 1, 'client_id': client_id})
    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)
    print steps.OverdraftSteps.get_limit_by_client(client_id)
    for context in given_overdraft_params:
        object_id = get_overdraft_object_id(firm_id=Firms.YANDEX_1.id, service_id=context.service.id,
                                            client_id=client_id)
        steps.CommonSteps.wait_and_get_notification(11, object_id, 1)

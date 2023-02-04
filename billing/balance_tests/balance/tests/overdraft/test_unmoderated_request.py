# coding: utf-8

import pytest
import datetime
from hamcrest import equal_to

NOW = datetime.datetime.now()

from balance import balance_steps as steps
from balance import balance_db as db
from btestlib import utils
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions, Services

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)

DIRECT_YANDEX_FIRM_FISH_NON_RESIDENT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                                            person_type=PersonTypes.YT,
                                                                            paysys=Paysyses.BANK_YT_RUB)

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

DIRECT_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT_QUASI,
                                                          firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                          paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                          currency=Currencies.KZT)


# Кажется, что эта метка тут не нужна
# @pytest.mark.no_parallel('fast_payment_overdraft')
@pytest.mark.parametrize('params', [
    {'overdraft_given_to': DIRECT_YANDEX_FIRM_FISH, 'overdraft_taken_by': DIRECT_YANDEX_FIRM_FISH}
])
@pytest.mark.parametrize('overdraft_value', [
    'enough_limit'
])
def test_overdraft_usage_medium(params, overdraft_value):
    client_id = steps.ClientSteps.create()
    overdraft_given_to_context = params['overdraft_given_to']
    if overdraft_value != 'null':
        if overdraft_value == 'enough_limit':
            limit = 100
        else:
            limit = 50

        steps.OverdraftSteps.set_force_overdraft(client_id, overdraft_given_to_context.service.id, limit,
                                                 overdraft_given_to_context.firm.id)
        actual_limit = db.balance().get_overdraft_limit_by_firm(client_id, overdraft_given_to_context.service.id,
                                                                overdraft_given_to_context.firm.id)
        assert limit == actual_limit

        overdraft_taken_by_context = params['overdraft_taken_by']
        person_id = steps.PersonSteps.create(client_id, overdraft_taken_by_context.person_type.code)
        service_order_id = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
        steps.OrderSteps.create(client_id, service_order_id, service_id=overdraft_taken_by_context.service.id,
                                product_id=overdraft_taken_by_context.product.id, params={'unmoderated': 1})

        orders_list = [
            {'ServiceID': overdraft_taken_by_context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
             'BeginDT': NOW}]
        request_id = steps.RequestSteps.create(client_id, orders_list,
                                               additional_params={'InvoiceDesireDT': NOW,
                                                                  # 'ForceUnmoderated': 1
                                                                  })
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, overdraft_taken_by_context.paysys.id,
                                                     credit=0, overdraft=1, contract_id=None)


# Кажется, что эта метка тут не нужна
# @pytest.mark.no_parallel('fast_payment_overdraft')
@pytest.mark.parametrize('params', [
    {'overdraft_given_to': DIRECT_YANDEX_FIRM_FISH, 'overdraft_taken_by': DIRECT_YANDEX_FIRM_FISH}
])
@pytest.mark.parametrize('overdraft_value', [
    'enough_limit'
])
def test_transfer_to_unmoderated(params, overdraft_value):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id=client_id, login='aikawa-test-10')
    overdraft_given_to_context = params['overdraft_given_to']
    if overdraft_value != 'null':
        if overdraft_value == 'enough_limit':
            limit = 100
        else:
            limit = 50

        steps.OverdraftSteps.set_force_overdraft(client_id, overdraft_given_to_context.service.id, limit,
                                                 overdraft_given_to_context.firm.id)
        actual_limit = db.balance().get_overdraft_limit_by_firm(client_id, overdraft_given_to_context.service.id,
                                                                overdraft_given_to_context.firm.id)
        assert limit == actual_limit

        overdraft_taken_by_context = params['overdraft_taken_by']
        person_id = steps.PersonSteps.create(client_id, overdraft_taken_by_context.person_type.code)
        service_order_id = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
        order_id_from = steps.OrderSteps.create(client_id, service_order_id,
                                                service_id=overdraft_taken_by_context.service.id,
                                                product_id=overdraft_taken_by_context.product.id)

        service_order_id_mod = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
        order_id_from_mod = steps.OrderSteps.create(client_id, service_order_id_mod,
                                                    service_id=overdraft_taken_by_context.service.id,
                                                    product_id=overdraft_taken_by_context.product.id
                                                    )

        orders_list = [
            {'ServiceID': overdraft_taken_by_context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
             'BeginDT': NOW},


        ]
        request_id = steps.RequestSteps.create(client_id, orders_list,
                                               additional_params={'InvoiceDesireDT': NOW,
                                                                  # 'ForceUnmoderated': 1
                                                                  })
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, overdraft_taken_by_context.paysys.id,
                                                     credit=0, overdraft=0, contract_id=None)
        steps.InvoiceSteps.pay(invoice_id)

        service_order_id = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
        order_id_to = steps.OrderSteps.create(client_id, service_order_id,
                                              service_id=overdraft_taken_by_context.service.id,
                                              product_id=overdraft_taken_by_context.product.id)
        steps.OrderSteps.transfer([{'order_id': order_id_from, 'qty_old': 100, 'qty_new': 50, 'all_qty': 0}],
                                  [{'order_id': order_id_to, 'qty_delta': 1}])

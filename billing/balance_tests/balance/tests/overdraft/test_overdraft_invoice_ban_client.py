# -*- coding: utf-8 -*-

# ничего не работает по таймауту

import datetime

import pytest
from hamcrest import equal_to

import balance.balance_db as db
import btestlib.matchers as matchers
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import AuditFeatures
from temp.igogor.balance_objects import Contexts, Firms, Regions, Products, Currencies, Services, Paysyses

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
DIRECT_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_RUB,
                                                              region=Regions.RU, currency=Currencies.RUB)
AUTORU_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.VERTICAL_12, product=Products.AUTORU_505123,
                                                       region=Regions.RU, currency=Currencies.RUB,
                                                       service=Services.AUTORU, paysys=Paysyses.BANK_UR_RUB_VERTICAL)
MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                        firm=Firms.MARKET_111)

DT = datetime.datetime.now()
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
SERVICE_ID = 7
PRODUCT_ID = 1475
OVERDRAFT_LIMIT = 1000
FIRM_ID = 1

@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11))
@pytest.mark.parametrize('context', [
        pytest.param(DIRECT_YANDEX_FIRM_RUB, id='Direct'),
        pytest.param(AUTORU_FIRM_RUB, id='Autoru'),
        pytest.param(MARKET_FIRM_FISH, id='Market'),
                         ])
def test_overdraft_invoice_expired(context):
    if context.service != Services.MARKET:
        client_id = steps.ClientSteps.create_multicurrency(currency_convert_type='COPY',
                                                       service_id=context.service.id,
                                                       region_id=context.region.id,
                                                       currency=context.currency.iso_code)
    else:
        client_id = steps.ClientSteps.create()

    steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, OVERDRAFT_LIMIT,
                                             context.firm.id,
                                             currency=context.currency.iso_code if context.service != Services.MARKET else None)

    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)

    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 10,
         'BeginDT': DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                     credit=0, overdraft=1, contract_id=None)
    steps.OverdraftSteps.expire_overdraft_invoice(invoice_id, 16)
    steps.OverdraftSteps.run_overdraft_ban(client_id)
    request_id = steps.RequestSteps.create(client_id, orders_list)
    try:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                     credit=0, overdraft=1, contract_id=None)
    except Exception, exc:
        with reporter.step(u'Проверяем, что новый счет не выставился, т.к. есть просроченный счет.'):
            utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'NOT_ENOUGH_OVERDRAFT_LIMIT'))
            utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'), equal_to(u'Client has expired invoice'))


@pytest.mark.parametrize('data',
                         [
                                 {'is_invoice_paid': False,
                                  'is_invoice_expired': True,
                                  'is_overdraft_ban': True,
                                  'with_our_fault_bad_debt': False,
                                  'another_act': False
                                  }
                         ]
                         )
def test_overdraft_invoice_ban_overdraft_raising(data):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, FIRM_ID)

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    if data['is_invoice_paid']:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=data['is_invoice_paid'])

    if data['is_invoice_expired']:
        steps.OverdraftSteps.expire_overdraft_invoice(invoice_id, 16)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
                                      campaigns_params={'Bucks': 5}, do_stop=0, campaigns_dt=DT)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT)[0]

    if data['with_our_fault_bad_debt']:
        steps.BadDebtSteps.make_bad_debt(invoice_id, data['with_our_fault_bad_debt']['our_fault'])
        if data['with_our_fault_bad_debt']['hidden']:
            steps.BadDebtSteps.make_bad_debt_hidden(act_id)

    if data['another_act']:
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
                                          campaigns_params={'Bucks': 7}, do_stop=0, campaigns_dt=DT)

        steps.ActsSteps.generate(client_id, force=1, date=DT)

    steps.OverdraftSteps.run_overdraft_ban(client_id)

    client = db.get_client_by_id(client_id)[0]
    reporter.log(client)
    if data['is_overdraft_ban']:
        object_id = get_overdraft_object_id(firm_id=1, service_id=7,
                                            client_id=client_id)
        utils.check_that(client, matchers.has_entries({'overdraft_ban': 1}))
        steps.CommonSteps.build_notification(11, object_id=object_id)
        steps.CommonSteps.wait_and_get_notification(11, object_id, 2, timeout=420)
    else:
        utils.check_that(client, matchers.has_entries({'overdraft_ban': 0}))


def get_overdraft_object_id(firm_id, service_id, client_id):
    return str(firm_id * 10 + service_id * 100000 + client_id * 1000000000)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('data',
                         [
                             ({'is_invoice_paid': False,
                               'is_invoice_expired': True,
                               'is_overdraft_ban': True,
                               'with_our_fault_bad_debt': False,
                               'another_act': False
                               })
                         ]
                         )
def test_overdraft_invoice_unban(data, context):
    client_id = steps.ClientSteps.create({'OVERDRAFT_BAN': True})
    db.balance().execute('UPDATE T_CLIENT SET OVERDRAFT_BAN = 1 WHERE ID = :client_id', {'client_id': client_id})
    steps.ClientSteps.set_force_overdraft(client_id, service_id=context.service.id, firm_id=context.firm.id, limit=100)
    steps.ClientSteps.set_force_overdraft(client_id, service_id=context.service.id, firm_id=Firms.REKLAMA_BEL_27.id,
                                          limit=200)
    steps.OverdraftSteps.run_overdraft_ban(client_id)
    db.get_client_by_id(client_id)
    object_id = get_overdraft_object_id(firm_id=context.firm.id, service_id=context.service.id, client_id=client_id)
    steps.CommonSteps.build_notification(11, object_id=object_id)
    steps.CommonSteps.wait_and_get_notification(11, object_id, 1, timeout=420)

    client = db.get_client_by_id(client_id)[0]
    reporter.log(client)
    utils.check_that(client, matchers.has_entries({'overdraft_ban': 0}))

    object_id = get_overdraft_object_id(firm_id=Firms.REKLAMA_BEL_27.id, service_id=context.service.id,
                                        client_id=client_id)
    steps.CommonSteps.build_notification(11, object_id=object_id)
    steps.CommonSteps.wait_and_get_notification(11, object_id, 1, timeout=420)

    client = db.get_client_by_id(client_id)[0]
    reporter.log(client)
    utils.check_that(client, matchers.has_entries({'overdraft_ban': 0}))

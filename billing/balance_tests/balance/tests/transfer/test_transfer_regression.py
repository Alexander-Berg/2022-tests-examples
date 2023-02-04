# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import has_length, equal_to

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance.snout_steps import cart_steps, invoice_steps, api_steps
from btestlib.data.snout_constants import Handles
from balance.features import AuditFeatures
from btestlib import utils as utils, reporter
from btestlib.constants import ContractPaymentType, ContractCommissionType, Services, Firms, Products
from btestlib.data import defaults
from btestlib.matchers import contains_dicts_with_entries
from temp.igogor.balance_objects import Contexts

SERVICE_ID = 42
PRODUCT_ID = 507130
# PAYSYS_ID = 1601044
PAYSYS_ID = 1601047
QTY = 100
BASE_DT = datetime.datetime.now()

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now())
TODAY_ISO = utils.Date.date_to_iso_format(TODAY)
WEEK_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=7))

TOLOKA_FISH_USD = Contexts.TOLOKA_FISH_USD_CONTEXT.new(region_id='142734')
VENDORS_FISH_RUB = Contexts.VENDORS_FISH_UR_RUB_CONTEXT.new(contract_type=ContractCommissionType.OPT_CLIENT,
                                                         contract_params={
                                                             'SERVICES': [Services.VENDORS.id],
                                                             'FIRM': Firms.MARKET_111.id,
                                                             'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                             'CURRENCY': 810
                                                         },
                                                         region_id=False)
DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
MARKET = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                              firm=Firms.MARKET_111)


def create_contract(client_id, person_id, contract_type, contract_params=None):
    contract_params_default = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        'SERVICES': [Services.DIRECT.id],
        'DT': TODAY_ISO,
        'FINISH_DT': WEEK_AFTER_ISO,
        'IS_SIGNED': TODAY_ISO,
    }
    if contract_params is not None:
        contract_params_default.update(contract_params)
    return steps.ContractSteps.create_contract_new(contract_type, contract_params_default, prevent_oebs_export=True)


@pytest.mark.parametrize('context, params', [
    (TOLOKA_FISH_USD, {'with_contract_and_credit': False}),
    (VENDORS_FISH_RUB, {'with_contract_and_credit': True})
])
def test_toloka_transfer(context, params):
    client_id = None or steps.ClientSteps.create()
    agency_id = None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = None or steps.PersonSteps.create(invoice_owner, context.person_type.code,
                                                 params={'region': context.region_id} if context.region_id else None)

    if params['with_contract_and_credit']:
        contract_id, _ = create_contract(client_id, person_id, context.contract_type, context.contract_params)
    else:
        contract_id = None

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id,
                                       params={'AgencyID': agency_id})
    service_order_id2 = steps.OrderSteps.next_id(context.service.id)
    order_id2 = steps.OrderSteps.create(order_owner, service_order_id2, service_id=context.service.id,
                                        product_id=context.product.id,
                                        params={'AgencyID': agency_id})
    service_order_id3 = steps.OrderSteps.next_id(context.service.id)
    order_id3 = steps.OrderSteps.create(order_owner, service_order_id3, service_id=context.service.id,
                                        product_id=context.product.id,
                                        params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                 credit=params['with_contract_and_credit'], contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    orders = [
        {'ID': order_id, 'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': BASE_DT},
        {'ID': order_id2, 'ServiceID': context.service.id, 'ServiceOrderID': service_order_id2, 'Qty': QTY,
         'BeginDT': BASE_DT},
        {'ID': order_id3, 'ServiceID': context.service.id, 'ServiceOrderID': service_order_id3, 'Qty': QTY,
         'BeginDT': BASE_DT}
    ]

    steps.InvoiceSteps.pay(invoice_id)
    # steps.InvoiceSteps.turn_on(invoice_id)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 85, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

    api.medium().CreateTransferMultiple(defaults.PASSPORT_UID,
                                        [
                                            {"ServiceID": context.service.id,
                                             "ServiceOrderID": orders[0]['ServiceOrderID'],
                                             "QtyOld": 100, "QtyNew": 87}
                                        ],
                                        [
                                            {"ServiceID": context.service.id,
                                             "ServiceOrderID": orders[1]['ServiceOrderID'],
                                             "QtyDelta": 1}
                                            , {"ServiceID": context.service.id,
                                               "ServiceOrderID": orders[2]['ServiceOrderID'],
                                               "QtyDelta": 2}
                                        ], 1, None)

    expected = [{'parent_order_id': orders[0]['ID'], 'current_qty': D('87')},
                {'parent_order_id': orders[1]['ID'], 'current_qty': D('4.333333')},
                {'parent_order_id': orders[2]['ID'], 'current_qty': D('8.666667')}
                ]
    actual = db.get_consumes_by_invoice(invoice_id)
    utils.check_that(actual, has_length(len(expected)))
    utils.check_that(actual, contains_dicts_with_entries(expected))


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4))
@pytest.mark.parametrize('context', [
    pytest.param(DIRECT, id='Direct'),
    pytest.param(MARKET, id='Market'),
])
def test_audit_transfer(context):
    service_id = context.service.id
    qty_1 = 100
    qty_2 = 40

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    with reporter.step(u'Создаем первый заказ, выставляем с ним счет и оплачиваем его.'):
        service_order_id_1 = steps.OrderSteps.next_id(service_id)
        order_id_1 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_1,
                                                  product_id=context.product.id, service_id=service_id)
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id_1, 'Qty': qty_1}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=context.paysys.id)
        steps.InvoiceSteps.pay(invoice_id)

    with reporter.step(u'Создаем второй заказ.'):
        service_order_id_2 = steps.OrderSteps.next_id(service_id)
        order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                                 product_id=context.product.id, service_id=service_id)

    api.medium().CreateTransferMultiple(defaults.PASSPORT_UID,
                                        [
                                            {"ServiceID": service_id,
                                             "ServiceOrderID": service_order_id_1,
                                             "QtyOld": qty_1, "QtyNew": qty_1 - qty_2}
                                        ],
                                        [
                                            {"ServiceID": service_id,
                                             "ServiceOrderID": service_order_id_2,
                                             "QtyDelta": 1}
                                        ], 1, None)
    with reporter.step(u'Получаем информацию по заявкам второго заказа.'):
        consumes = db.get_consumes_by_order(order_id_2)

    with reporter.step(u'Проверяем информацию на заявке второго заказа.'):
        utils.check_that(consumes, contains_dicts_with_entries([{'consume_sum': qty_2 * context.price,
                                                                 'consume_qty': qty_2}]))


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4))
@pytest.mark.parametrize('context', [
    pytest.param(DIRECT, id='Direct'),
    pytest.param(MARKET, id='Market'),
])
def test_audit_transfer_from_unused_funds(context):
    service_id = context.service.id
    original_qty = 100
    original_sum = original_qty * context.price

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    with reporter.step(u'Создаем заказ, выставляем с ним счет и оплачиваем его:'):
        service_order_id = steps.OrderSteps.next_id(service_id)
        order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                                  product_id=context.product.id, service_id=service_id)
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': original_qty}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=context.paysys.id)
        steps.InvoiceSteps.pay(invoice_id)

    with reporter.step(u'Снимаем часть средств на беззаказье:'):
        session, token = invoice_steps.InvoiceSteps.get_session_and_token(invoice_id=invoice_id)
        response = invoice_steps.InvoiceSteps.rollback(session, _csrf=token, invoice_id=invoice_id,
                                                       amount=int(original_sum))
        utils.check_that(response.status_code, equal_to(200))

    with reporter.step(u'Получаем информацию по заявкам заказа:'):
        consumes = db.get_consumes_by_order(order_id)
    with reporter.step(u'Проверяем, что с заказа были сняты средства:'):
        utils.check_that(consumes, contains_dicts_with_entries([{'current_sum': 0,
                                                                 'current_qty': 0}]))

    with reporter.step(u'Зачисляем средства с беззаказья:'):
        response = invoice_steps.InvoiceSteps.transfer_from_unfunds(session, _csrf=token, invoice_id=invoice_id,
                                                       dst_order_id=order_id)
        utils.check_that(response.status_code, equal_to(200))

    with reporter.step(u'Получаем информацию по заявкам заказа:'):
        consumes = db.get_consumes_by_order(order_id)

    with reporter.step(u'Проверяем, что средства зачислились на заказ с беззаказья'):
        utils.check_that(consumes, contains_dicts_with_entries([{'current_sum': original_sum,
                                                                 'current_qty': original_qty}]))
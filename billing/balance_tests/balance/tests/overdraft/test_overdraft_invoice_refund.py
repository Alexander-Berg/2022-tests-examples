# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.tests.conftest import get_free_user
from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib.constants import Firms, Services, Paysyses
from btestlib.matchers import contains_dicts_with_entries
from balance.balance_steps.other_steps import UserSteps

DT = datetime.datetime.now()
PAYMENT_TERM_DT = DT - datetime.timedelta(days=1)
DEFAULT_PAYMENT_TERM_DT = DT + datetime.timedelta(days=15)

PERSON_TYPE = 'ph'
NOT_INSTANT_PAYSYS_ID = Paysyses.BANK_PH_RUB.id
INSTANT_PAYSYS_ID = Paysyses.CC_PH_RUB.id

OVERDRAFT_LIMIT = 1000

DIRECT = Services.DIRECT.id
FIRM_ID = Firms.YANDEX_1.id

DIRECT_PRODUCT = Product(DIRECT, 1475, 'Bucks', 'Money')

UNUSED_FUNDS_LOCK_OFF = 0  # Другое (Нет блокировки)
UNUSED_FUNDS_LOCK_TRANSFER = 1  # Взаимозачет
UNUSED_FUNDS_LOCK_REFUND = 2  # Возврат
UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT = 3  # Блок овердрафта


@pytest.mark.parametrize('unused_funds_lock_status',
                         [
                             pytest.mark.smoke(UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT),
                             UNUSED_FUNDS_LOCK_OFF
                         ]
                         )
@pytest.mark.parametrize('payment_params', [{'final_payment_sum': D('755'),
                                             'expected': {'invoice_info': {'consume_sum': 755},
                                                          'consume_info': [{'consume_sum': 600,
                                                                            'current_qty': '15.111',
                                                                            'act_qty': 15,
                                                                            'completion_qty': 15},

                                                                           {'consume_sum': 300,
                                                                            'current_qty': '10.055667',
                                                                            'act_qty': 10,
                                                                            'completion_qty': 10}]
                                                          }},
                                            {'final_payment_sum': D('1005'),
                                             'expected': {'invoice_info': {'consume_sum': 900},
                                                          'consume_info': [{'consume_sum': 600,
                                                                            'current_qty': '18.333333',
                                                                            'act_qty': 15,
                                                                            'completion_qty': 15},

                                                                           {'consume_sum': 300,
                                                                            'current_qty': '11.666667',
                                                                            'act_qty': 10,
                                                                            'completion_qty': 10}]
                                                          }}
                                            ])
def test_overdraft_invoice_all_conditions_are_true(unused_funds_lock_status, payment_params, get_free_user):
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    PAYMENT_SUM = 900
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT, OVERDRAFT_LIMIT, FIRM_ID)

    product = DIRECT_PRODUCT

    service_order_id = steps.OrderSteps.next_id(product.service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product.id, product.service_id)

    service_order_id_2 = steps.OrderSteps.next_id(product.service_id)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, product.id, product.service_id)

    orders_list = [
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT},
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id_2, 'Qty': 20, 'BeginDT': DT},
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=NOT_INSTANT_PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
                                      campaigns_params={product.shipment_type: 10}, do_stop=0, campaigns_dt=DT)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id_2,
                                      campaigns_params={product.shipment_type: 15}, do_stop=0, campaigns_dt=DT)

    steps.ActsSteps.generate(client_id, force=1, date=DT)

    steps.InvoiceSteps.pay(invoice_id, payment_sum=PAYMENT_SUM)
    steps.OverdraftSteps.set_payment_term_dt(invoice_id, PAYMENT_TERM_DT)

    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=unused_funds_lock_status, amount=150,
                                        order_id=order_id_2, user=user)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=payment_params['final_payment_sum'] - PAYMENT_SUM)

    steps.OverdraftSteps.refund_overdraft_invoices(client_id)
    invoice_after_refund = db.get_invoice_by_id(invoice_id)[0]
    consumes = db.get_consumes_by_invoice(invoice_id)

    utils.check_that(invoice_after_refund, hamcrest.has_entries(payment_params['expected']['invoice_info']))
    utils.check_that(consumes, contains_dicts_with_entries(payment_params['expected']['consume_info']))


@pytest.mark.parametrize('unused_funds_lock_status',
                         [
                             UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT,
                             UNUSED_FUNDS_LOCK_OFF
                         ]
                         )
@pytest.mark.parametrize('payment_params',
                         [
                             {'extra_payment_sum': D('5'), 'expected': {'consume_info': [{'consume_sum': 600,
                                                                                          'current_qty': '15.111',
                                                                                          'act_qty': 15,
                                                                                          'completion_qty': 15},

                                                                                         {'consume_sum': 300,
                                                                                          'current_qty': '10.055667',
                                                                                          'act_qty': 10,
                                                                                          'completion_qty': 10}],

                                                                        'invoice_info': {'consume_sum': 755,
                                                                                         'total_act_sum': 750
                                                                                         }}},

                             {'extra_payment_sum': D('255'), 'expected': {'consume_info': [{'consume_sum': 600,
                                                                                            'current_qty': '18.333333',
                                                                                            'act_qty': 15,
                                                                                            'completion_qty': 15},

                                                                                           {'consume_sum': 300,
                                                                                            'current_qty': '11.666667',
                                                                                            'act_qty': 10,
                                                                                            'completion_qty': 10}],

                                                                          'invoice_info': {'consume_sum': 900,
                                                                                           'total_act_sum': 750
                                                                                           }}}
                         ]
                         )
def test_overdraft_invoice_all_conditions_are_true_instant_payment(unused_funds_lock_status, payment_params):
    PAYMENT_SUM = 900
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT, OVERDRAFT_LIMIT, FIRM_ID)

    product = DIRECT_PRODUCT

    service_order_id = steps.OrderSteps.next_id(product.service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product.id, product.service_id)

    service_order_id_2 = steps.OrderSteps.next_id(product.service_id)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, product.id, product.service_id)

    orders_list = [
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT},
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id_2, 'Qty': 20, 'BeginDT': DT},
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=INSTANT_PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
                                      campaigns_params={product.shipment_type: 10}, do_stop=0, campaigns_dt=DT)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id_2,
                                      campaigns_params={product.shipment_type: 15}, do_stop=0, campaigns_dt=DT)

    steps.ActsSteps.generate(client_id, force=1, date=DT)

    steps.InvoiceSteps.turn_on(invoice_id, PAYMENT_SUM)
    steps.OverdraftSteps.set_payment_term_dt(invoice_id, PAYMENT_TERM_DT)

    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=unused_funds_lock_status, amount=150,
                                        order_id=order_id_2)

    steps.InvoiceSteps.turn_on(invoice_id, payment_params['extra_payment_sum'])

    steps.OverdraftSteps.refund_overdraft_invoices(client_id)

    invoice_after_refund = db.get_invoice_by_id(invoice_id)[0]
    consumes = db.get_consumes_by_invoice(invoice_id)

    utils.check_that(invoice_after_refund, hamcrest.has_entries(payment_params['expected']['invoice_info']))
    utils.check_that(consumes, contains_dicts_with_entries(payment_params['expected']['consume_info']))


@pytest.mark.parametrize('paysys',
                         [
                             NOT_INSTANT_PAYSYS_ID,
                             INSTANT_PAYSYS_ID
                         ]
                         )
def test_overdraft_invoice_check_refund_on_invoice_order_only(paysys):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT, OVERDRAFT_LIMIT, FIRM_ID)

    product = DIRECT_PRODUCT

    service_order_id = steps.OrderSteps.next_id(product.service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product.id, product.service_id)

    service_order_id_2 = steps.OrderSteps.next_id(product.service_id)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, product.id, product.service_id)

    service_order_id_3 = steps.OrderSteps.next_id(product.service_id)
    order_id_3 = steps.OrderSteps.create(client_id, service_order_id_3, product.id, product.service_id)

    orders_list = [
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': 20, 'BeginDT': DT},
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id_2, 'Qty': 40, 'BeginDT': DT},
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
                                      campaigns_params={product.shipment_type: 10}, do_stop=0, campaigns_dt=DT)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id_2,
                                      campaigns_params={product.shipment_type: 15}, do_stop=0, campaigns_dt=DT)

    steps.OrderSteps.transfer([{'order_id': order_id_2, 'qty_old': 40, 'qty_new': 30, 'all_qty': 0}],
                              [{'order_id': order_id_3, 'qty_delta': 1}])

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id_3,
                                      campaigns_params={product.shipment_type: 5}, do_stop=0, campaigns_dt=DT)

    steps.ActsSteps.generate(client_id, force=1, date=DT)

    steps.OverdraftSteps.set_payment_term_dt(invoice_id, PAYMENT_TERM_DT)

    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT, amount=300,
                                        order_id=order_id)
    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT, amount=450,
                                        order_id=order_id_2)
    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT, amount=150,
                                        order_id=order_id_3)

    if paysys == NOT_INSTANT_PAYSYS_ID:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=1800)
    else:
        steps.InvoiceSteps.turn_on(invoice_id, sum=1800)

    steps.OverdraftSteps.refund_overdraft_invoices(client_id)

    invoice_after_refund = db.get_invoice_by_id(invoice_id)[0]
    consumes = db.get_consumes_by_invoice(invoice_id)

    utils.check_that(invoice_after_refund, hamcrest.has_entries({'consume_sum': 1800,
                                                                 'total_act_sum': 900
                                                                 }))
    utils.check_that(consumes, contains_dicts_with_entries([{'consume_sum': 300,
                                                             'current_qty': '5',
                                                             'act_qty': 5,
                                                             'completion_qty': 5},

                                                            {'consume_sum': 1200,
                                                             'current_qty': '35',
                                                             'act_qty': 15,
                                                             'completion_qty': 15},

                                                            {'consume_sum': 600,
                                                             'current_qty': '20',
                                                             'act_qty': 10,
                                                             'completion_qty': 10}]))


@pytest.mark.parametrize('paysys',
                         [
                             NOT_INSTANT_PAYSYS_ID,
                             INSTANT_PAYSYS_ID
                         ]
                         )
def test_overdraft_refund_wrong_unused_funds_lock_status(paysys):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT, OVERDRAFT_LIMIT, FIRM_ID)

    product = DIRECT_PRODUCT

    service_order_id = steps.OrderSteps.next_id(product.service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product.id, product.service_id)

    orders_list = [
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
                                      campaigns_params={product.shipment_type: 4}, do_stop=0, campaigns_dt=DT)

    steps.ActsSteps.generate(client_id, force=1, date=DT)

    if paysys == NOT_INSTANT_PAYSYS_ID:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=290)
    else:
        steps.InvoiceSteps.turn_on(invoice_id, sum=290)

    steps.OverdraftSteps.set_payment_term_dt(invoice_id, PAYMENT_TERM_DT)

    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=UNUSED_FUNDS_LOCK_REFUND, amount=180,
                                        order_id=order_id)

    steps.OverdraftSteps.refund_overdraft_invoices(client_id)

    invoice_after_refund = db.get_invoice_by_id(invoice_id)[0]
    consumes = db.get_consumes_by_invoice(invoice_id)
    consume_after_refund = consumes[0]

    utils.check_that(invoice_after_refund, hamcrest.has_entries({'consume_sum': 120,
                                                                 'total_act_sum': 120
                                                                 }))
    utils.check_that(consume_after_refund, hamcrest.has_entries({'current_sum': 120,
                                                                 'consume_sum': 300,
                                                                 'current_qty': 4,
                                                                 'act_qty': 4,
                                                                 'completion_qty': 4}))


@pytest.mark.parametrize('paysys',
                         [
                             NOT_INSTANT_PAYSYS_ID,
                             INSTANT_PAYSYS_ID
                         ]
                         )
def test_overdraft_refund_act_qty_is_0(paysys):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT, OVERDRAFT_LIMIT, FIRM_ID)

    product = DIRECT_PRODUCT

    service_order_id = steps.OrderSteps.next_id(product.service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product.id, product.service_id)

    orders_list = [
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
                                      campaigns_params={product.shipment_type: 4}, do_stop=0, campaigns_dt=DT)

    if paysys == NOT_INSTANT_PAYSYS_ID:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=290)
    else:
        steps.InvoiceSteps.turn_on(invoice_id, sum=290)

    steps.OverdraftSteps.set_payment_term_dt(invoice_id, PAYMENT_TERM_DT)

    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT, amount=180,
                                        order_id=order_id)

    steps.OverdraftSteps.refund_overdraft_invoices(client_id)

    invoice_after_refund = db.get_invoice_by_id(invoice_id)[0]
    consumes = db.get_consumes_by_invoice(invoice_id)

    consume_after_refund = consumes[0]
    utils.check_that(invoice_after_refund, hamcrest.has_entries({'consume_sum': 120,
                                                                 'total_act_sum': 0
                                                                 }))

    utils.check_that(consume_after_refund, hamcrest.has_entries({'current_sum': 120,
                                                                 'consume_sum': 300,
                                                                 'current_qty': 4,
                                                                 'act_qty': 0,
                                                                 'completion_qty': 4}))


@pytest.mark.parametrize('paysys',
                         [
                             NOT_INSTANT_PAYSYS_ID,
                             INSTANT_PAYSYS_ID
                         ]
                         )
def test_overdraft_refund_invoice_is_not_expired(paysys):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT, OVERDRAFT_LIMIT, FIRM_ID)

    product = DIRECT_PRODUCT

    service_order_id = steps.OrderSteps.next_id(product.service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product.id, product.service_id)

    orders_list = [
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
                                      campaigns_params={product.shipment_type: 4}, do_stop=0, campaigns_dt=DT)

    steps.ActsSteps.generate(client_id, force=1, date=DT)

    if paysys == NOT_INSTANT_PAYSYS_ID:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=290)
    else:
        steps.InvoiceSteps.turn_on(invoice_id, sum=290)

    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT, amount=180,
                                        order_id=order_id)

    steps.OverdraftSteps.refund_overdraft_invoices(client_id)

    invoice_after_refund = db.get_invoice_by_id(invoice_id)[0]
    consumes = db.get_consumes_by_invoice(invoice_id)
    consume_after_refund = consumes[0]

    utils.check_that(invoice_after_refund, hamcrest.has_entries({'consume_sum': 120,
                                                                 'total_act_sum': 120
                                                                 }))
    utils.check_that(consume_after_refund, hamcrest.has_entries({'current_sum': 120,
                                                                 'consume_sum': 300,
                                                                 'current_qty': 4,
                                                                 'act_qty': 4,
                                                                 'completion_qty': 4}))


@pytest.mark.parametrize('paysys',
                         [
                             NOT_INSTANT_PAYSYS_ID,
                             INSTANT_PAYSYS_ID
                         ]
                         )
def test_overdraft_refund_receipt_sum_less_than_consume_sum(paysys):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT, OVERDRAFT_LIMIT, FIRM_ID)

    product = DIRECT_PRODUCT

    service_order_id = steps.OrderSteps.next_id(product.service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product.id, product.service_id)

    orders_list = [
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
                                      campaigns_params={product.shipment_type: 4}, do_stop=0, campaigns_dt=DT)

    steps.ActsSteps.generate(client_id, force=1, date=DT)

    if paysys == NOT_INSTANT_PAYSYS_ID:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=290)

    steps.OverdraftSteps.set_payment_term_dt(invoice_id, PAYMENT_TERM_DT)

    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT, amount=160,
                                        order_id=order_id)

    if paysys == NOT_INSTANT_PAYSYS_ID:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=-160)
    else:
        steps.InvoiceSteps.turn_on(invoice_id, sum=130)

    steps.OverdraftSteps.refund_overdraft_invoices(client_id)

    invoice_after_refund = db.get_invoice_by_id(invoice_id)[0]
    consumes = db.get_consumes_by_invoice(invoice_id)
    consume_after_refund = consumes[0]

    utils.check_that(invoice_after_refund, hamcrest.has_entries({'consume_sum': 140,
                                                                 'total_act_sum': 120
                                                                 }))
    utils.check_that(consume_after_refund, hamcrest.has_entries({'current_sum': 140,
                                                                 'consume_sum': 300,
                                                                 'current_qty': '4.666667',
                                                                 'act_qty': 4,
                                                                 'completion_qty': 4}))


@pytest.mark.parametrize('paysys',
                         [
                             NOT_INSTANT_PAYSYS_ID,
                             INSTANT_PAYSYS_ID
                         ]
                         )
def test_overdraft_refund_receipt_sum_less_than_act_sum(paysys):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT, OVERDRAFT_LIMIT, FIRM_ID)

    product = DIRECT_PRODUCT

    service_order_id = steps.OrderSteps.next_id(product.service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product.id, product.service_id)

    orders_list = [
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
                                      campaigns_params={product.shipment_type: 4}, do_stop=0, campaigns_dt=DT)

    steps.ActsSteps.generate(client_id, force=1, date=DT)

    if paysys == NOT_INSTANT_PAYSYS_ID:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=150)
    else:
        steps.InvoiceSteps.turn_on(invoice_id, sum=150)
    # steps.InvoiceSteps.pay(invoice_id, payment_sum=150)

    steps.OverdraftSteps.set_payment_term_dt(invoice_id, PAYMENT_TERM_DT)

    steps.InvoiceSteps.make_rollback_ai(invoice_id, unused_funds_lock=UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT, amount=180,
                                        order_id=order_id)
    # грязный хак, потому что нельзя(?) снести на беззаказье больше, чем заакчено, пока есть такая бага в reset_invoice
    db.balance().execute('update t_invoice set total_act_sum = 160 where id = :invoice_id', {'invoice_id': invoice_id})

    steps.OverdraftSteps.refund_overdraft_invoices(client_id)

    invoice_after_refund = db.get_invoice_by_id(invoice_id)[0]
    consumes = db.get_consumes_by_invoice(invoice_id)
    consume_after_refund = consumes[0]

    utils.check_that(invoice_after_refund, hamcrest.has_entries({'consume_sum': 120,
                                                                 'total_act_sum': 160
                                                                 }))
    utils.check_that(consume_after_refund, hamcrest.has_entries({'current_sum': 120,
                                                                 'consume_sum': 300,
                                                                 'current_qty': 4,
                                                                 'act_qty': 4,
                                                                 'completion_qty': 4}))

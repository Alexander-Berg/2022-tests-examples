# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest
from hamcrest import equal_to

from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features
from btestlib import reporter, utils
from btestlib.constants import Firms, Paysyses, PersonTypes
from btestlib.data.defaults import Order
from temp.igogor.balance_objects import Contexts

DIRECT_OOO_INSTANT_PH_CC = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                                person_type=PersonTypes.PH.code,
                                                                paysys=Paysyses.CC_PH_RUB.id)
DIRECT_OOO_BANK = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                       person_type=PersonTypes.UR.code,
                                                       paysys=Paysyses.BANK_UR_RUB.id)
DIRECT_KZT_BANK = Contexts.DIRECT_FISH_KZ_CONTEXT.new(firm=Firms.KZ_25,
                                                      person_type=PersonTypes.KZU.code,
                                                      paysys=Paysyses.BANK_KZ_UR_TG.id)
DIRECT_OOO_INSTANT_UR_CC = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                          person_type=PersonTypes.UR.code,
                                                          paysys=Paysyses.CC_UR_RUB.id)

DT = datetime.datetime.now()
INVISIBLE_DT = DT - datetime.timedelta(days=186)
VISIBLE_DT = DT - datetime.timedelta(days=185)

OVERDRAFT_LIMIT = 30000
ORDER_QTY = 100

pytestmark = [
    pytest.mark.tickets('TESTBALANCE-1659'),
    reporter.feature(Features.INVOICE, Features.UI)
]


def data_preparation(context, is_overdraft=False):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id,
                            {'TEXT': 'Py_Test order',
                             'AgencyID': None,
                             'ManagerUID': None})
    orders_list = Order.default_orders_list(service_order_id, service_id=context.service.id, qty=ORDER_QTY)
    request_id = steps.RequestSteps.create(client_id,
                                           orders_list,
                                           additional_params={'InvoiceDesireDT': DT})
    if is_overdraft:
        steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, OVERDRAFT_LIMIT, context.firm.id)
    invoice_id = steps.InvoiceSteps.create(request_id=request_id,
                                           person_id=person_id,
                                           paysys_id=context.paysys,
                                           contract_id=None,
                                           credit=0,
                                           overdraft=1 if is_overdraft else 0)[0]
    return client_id, invoice_id


def check_ai(invoice_id, ai_visible_info):
    with web.Driver() as driver:
        invoice_page = web.AdminInterface.InvoicePage.open(driver, invoice_id=invoice_id)
        payment_btn = invoice_page.is_confirm_payment_button_present()
        print_form_block = invoice_page.is_print_form_block_present()
        utils.check_that(ai_visible_info[0], equal_to(payment_btn))
        utils.check_that(ai_visible_info[1], equal_to(print_form_block))


def check_ci_pages(invoice_id, user, ci_visible_info, page='invoice'):
    with web.Driver(user=user) as driver:
        if page == 'invoice':
            page = web.ClientInterface.InvoicePage.open(driver, invoice_id=invoice_id)
            debt_notify = page.is_debt_notify_present()
            utils.check_that(ci_visible_info[2], equal_to(debt_notify))
        if page == 'success':
            page = web.ClientInterface.SuccessPage.open(driver, invoice_id=invoice_id)
        payment_btn = page.is_payment_button_present()
        utils.check_that(ci_visible_info[0], equal_to(payment_btn))
        print_form_block = page.is_pf_block_present()
        utils.check_that(ci_visible_info[1], equal_to(print_form_block))


@pytest.mark.parametrize(
    "context, shift_dt, ci_visible_info, ai_visible_info, is_overdraft",
    [
        (DIRECT_KZT_BANK, VISIBLE_DT, [False, True, False], [True, True], False),
        (DIRECT_KZT_BANK, INVISIBLE_DT, [False, False, False], [True, True], False),
        (DIRECT_OOO_INSTANT_UR_CC, VISIBLE_DT, [True, False, False], [False, True], False),
        (DIRECT_OOO_INSTANT_UR_CC, INVISIBLE_DT, [False, False, False], [False, True], False),
        (DIRECT_OOO_INSTANT_PH_CC, VISIBLE_DT, [True, False, True], [False, False], True),
        (DIRECT_OOO_INSTANT_PH_CC, INVISIBLE_DT, [True, False, True], [False, False], True),
        (DIRECT_OOO_BANK, VISIBLE_DT, [False, True, True], [True, True], True),
        (DIRECT_OOO_BANK, INVISIBLE_DT, [False, True, True], [True, True], True),

    ],
    ids=[
        'prepayment_bank_paysys_visible',
        'prepayment_bank_paysys_invisible',
        'prepayment_instant_paysys_visible',
        'prepayment_instant_paysys_invisible',
        'overdraft_instant_paysys_visible',
        'overdraft_instant_paysys_invisible',
        'overdraft_bank_paysys_visible',
        'overdraft_bank_paysys_invisible',
    ]
)
def test_invoice_dt_shift(get_free_user, context, shift_dt, ci_visible_info,
                                  ai_visible_info, is_overdraft):
    user = get_free_user()
    client_id, invoice_id = data_preparation(context, is_overdraft)
    steps.ClientSteps.link(client_id, user.login)
    steps.InvoiceSteps.set_dt(invoice_id, shift_dt)
    if is_overdraft:
        steps.InvoiceSteps.set_payment_term_dt(invoice_id, shift_dt)
    check_ci_pages(invoice_id, user, ci_visible_info, 'invoice')
    check_ci_pages(invoice_id, user, ci_visible_info, 'success')
    check_ai(invoice_id, ai_visible_info)

# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D
from balance import balance_api as api
from jsonrpc import dispatcher
from balance import balance_db as db
import uuid

from . import steps
from balance import balance_steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Currencies, Firms, ContractCommissionType, Services, PersonTypes, Paysyses, InvoiceType, \
    Products, Permissions, User, Users, Export
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, ZAXI_RU_CONTEXT
from .. import common_defaults
from balance.tests.conftest import get_free_user
from dateutil.relativedelta import relativedelta

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
YESTERDAY = datetime.datetime.now() - datetime.timedelta(days=2)
TOMORROW = datetime.datetime.now() + datetime.timedelta(days=1)
FUTURE = datetime.datetime.now() + datetime.timedelta(days=5)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW
END_DT = datetime.datetime(year=2025, month=1, day=1)
START_DT = datetime.datetime(year=2020, month=1, day=1)

CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.NO_AGENCY)
QTY = D('250')
COMPLETIONS = D('99.99')


@dispatcher.add_method
def test_prepayment_unpaid_invoice(login=None):
    client_id, _, _, _, _, _, invoice_id, external_id = steps.create_base_invoice(overdraft=0)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, invoice_id, external_id

@dispatcher.add_method
def test_prepayment_unpaid_invoice_with_card_paysys(login=None):
    context = Contexts.DIRECT_FISH_RUB_PH_CONTEXT.new(person_params=common_defaults.FIXED_PH_PARAMS,
                                                      firm=Firms.YANDEX_1,
                                                      paysys=Paysyses.CC_PH_RUB)
    client_id, person_id, _, _, _, _, invoice_id, external_id = steps.create_base_invoice(context=context, overdraft=0)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, invoice_id, person_id, external_id


@dispatcher.add_method
def test_prepayment_fixed_dt_invoices(login=None):
    client_id, person_id, _, _, _, _, invoice_id_1, external_id_1 = steps.create_base_invoice(overdraft=0)
    _, _, _, _, _, _, invoice_id_2, external_id_2 = steps.create_base_invoice(overdraft=0, client_id=client_id,
                                                                              person_id=person_id)
    balance_steps.ClientSteps.link(client_id, login)
    balance_steps.InvoiceSteps.set_dt(invoice_id_1, datetime.datetime(2019, 3, 1))
    balance_steps.InvoiceSteps.set_dt(invoice_id_2, datetime.datetime(2025, 3, 1))
    return client_id, invoice_id_1, external_id_1, invoice_id_2, external_id_2


@dispatcher.add_method
def test_prepayment_underpaid_invoice():
    qty = 50
    receipt_sum = 2250
    client_id, _, _, _, _, _, invoice_id, external_id = steps.create_base_invoice(qty=qty, overdraft=0, orders_amount=3)

    balance_steps.InvoiceSteps.pay(invoice_id, payment_sum=receipt_sum)
    return client_id, invoice_id, external_id


@dispatcher.add_method
def test_prepayment_turned_on_unpaid_invoice():
    qty = 50
    client_id, _, _, _, _, _, invoice_id, external_id = steps.create_base_invoice(qty=qty, overdraft=0, orders_amount=3)

    balance_steps.InvoiceSteps.turn_on(invoice_id)
    return client_id, invoice_id, external_id


@dispatcher.add_method
def test_prepayment_overpaid_invoice(login=None):
    qty = 50
    receipt_sum = 9000
    client_id, _, _, _, _, _, invoice_id, external_id = steps.create_base_invoice(qty=qty, overdraft=0, orders_amount=3)

    balance_steps.InvoiceSteps.pay(invoice_id, payment_sum=receipt_sum)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, invoice_id, external_id


def test_prepayment_paid_invoice_with_act():
    invoice_owner, _, orders_list, _, _, _, invoice_id, external_id = steps.create_base_invoice()
    balance_steps.InvoiceSteps.pay(invoice_id)
    # Отправляем НЕчестные открутки:
    balance_steps.CampaignsSteps.do_campaigns(CONTEXT.product.service.id, orders_list[0]['ServiceOrderID'],
                                      {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)
    # Выставляем акт
    balance_steps.ActsSteps.generate(invoice_owner, force=1, date=ACT_DT)
    return invoice_id, external_id


# неоплаченный предоплатный счет с эндбайером
@dispatcher.add_method
def test_prepayment_unpaid_invoice_with_endbuyer():
    qty = 50
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, invoice_id_1, external_id = \
    steps.create_base_invoice(overdraft=0, orders_amount=3, qty=qty, contract_params=contract_params,
                                  contract_type=ContractCommissionType.COMMISS,
                              with_endbuyer=True, need_agency=True)
    return client_id, external_id, invoice_id_1


def test_overdraft_unpaid_invoice():
    client_id, _, _, _, _, _, invoice_id, external_id = steps.create_base_invoice(overdraft=1)
    return client_id, invoice_id, external_id

@dispatcher.add_method
def test_overdraft_almost_overdue_and_overdue_invoice(login=None):
    client_id, _, _, _, _, _, invoice_id_1, external_id_1 = steps.create_base_invoice(overdraft=1)
    client_id, _, _, _, _, _, invoice_id_2, external_id_2 = steps.create_base_invoice(overdraft=1, client_id=client_id)
    client_id, _, _, _, _, _, invoice_id_3, external_id_3 = steps.create_base_invoice(overdraft=1, client_id=client_id)
    balance_steps.InvoiceSteps.set_payment_term_dt(invoice_id_1, datetime.datetime.today()-datetime.timedelta(days=3))
    balance_steps.InvoiceSteps.set_payment_term_dt(invoice_id_2, datetime.datetime.today()+datetime.timedelta(days=3))
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, invoice_id_1, external_id_1, invoice_id_2, external_id_2


# просроченный овердрафтный счет
@dispatcher.add_method
def test_overdraft_overdue_invoice(login=None):
    client_id, _, orders_list, _, _, _, invoice_id, external_id = steps.create_base_invoice(overdraft=1)
    balance_steps.InvoiceSteps.set_dt(invoice_id, datetime.datetime(2020, 10, 1))
    balance_steps.InvoiceSteps.set_payment_term_dt(invoice_id, datetime.datetime(2020, 10, 15))
    balance_steps.CampaignsSteps.do_campaigns(CONTEXT.product.service.id, orders_list[0]['ServiceOrderID'],
                                              {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)
    balance_steps.ActsSteps.generate(client_id, force=1, date=ACT_DT)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, invoice_id, external_id

# просроченная задолженность
@dispatcher.add_method
def test_overdraft_overdue_invoice_request():
    client_id, person_id, orders_list, _, _, request_id, overdue_invoice_id, overdue_external_id = steps.create_base_invoice(overdraft=1)
    balance_steps.InvoiceSteps.set_dt(overdue_invoice_id, datetime.datetime(2020, 10, 1))
    balance_steps.InvoiceSteps.set_payment_term_dt(overdue_invoice_id, datetime.datetime(2020, 10, 15))
    _, _, _, _, _, request_id, _ = steps.create_base_request(client_id=client_id, person_id=person_id)
    return client_id, request_id, overdue_invoice_id, overdue_external_id


# почти просроченная задолженность
@dispatcher.add_method
def test_almost_overdue_overdraft_invoice_request():
    client_id, _, orders_list, _, _, request_id, invoice_id, external_id = steps.create_base_invoice(overdraft=1)
    balance_steps.InvoiceSteps.set_dt(invoice_id, datetime.datetime(2020, 10, 1))
    balance_steps.InvoiceSteps.set_payment_term_dt(invoice_id, dt=TOMORROW)
    balance_steps.CampaignsSteps.do_campaigns(CONTEXT.product.service.id, orders_list[0]['ServiceOrderID'],
                                              {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)
    balance_steps.ActsSteps.generate(client_id, force=1, date=ACT_DT)
    request_id_2= balance_steps.RequestSteps.create(client_id, orders_list)
    return client_id, request_id, request_id_2, invoice_id


# почти просроченная задолженность, Маркет
@dispatcher.add_method
def test_almost_overdue_overdraft_invoice_market_request():
    context = Contexts.MARKET_RUB_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS,
                                              firm=Firms.MARKET_111)
    client_id, _, orders_list, _, _, _, almost_overdue_invoice_id, almost_overdue_external_id = steps.create_base_invoice(context=context, overdraft=1)
    balance_steps.InvoiceSteps.set_dt(almost_overdue_invoice_id, datetime.datetime(2020, 10, 1))
    balance_steps.InvoiceSteps.set_payment_term_dt(almost_overdue_invoice_id, dt=TOMORROW)
    request_id = balance_steps.RequestSteps.create(client_id, orders_list)
    return client_id, request_id, almost_overdue_invoice_id, almost_overdue_external_id


# есть задолженность
@dispatcher.add_method
def test_overdraft_invoice_request():
    client_id, _, orders_list, _, _, request_id, invoice_id, external_id = steps.create_base_invoice(overdraft=1)
    balance_steps.InvoiceSteps.set_dt(invoice_id, datetime.datetime(2020, 10, 1))
    balance_steps.InvoiceSteps.set_payment_term_dt(invoice_id, dt=FUTURE)
    balance_steps.CampaignsSteps.do_campaigns(CONTEXT.product.service.id, orders_list[0]['ServiceOrderID'],
                                              {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)
    balance_steps.ActsSteps.generate(client_id, force=1, date=ACT_DT)
    request_id_2= balance_steps.RequestSteps.create(client_id, orders_list)
    return client_id, request_id, request_id_2, invoice_id

# Клиент с предоплатным счетом с актом и с неоплаченным овердрафтным счетом на одного плательщика,
# кнопка "Внести оплату" без подсветки
@dispatcher.add_method
def test_prepayment_paid_invoice_with_act_and_overdraft_unpaid():
    invoice_owner, person_id, orders_list, _, _, _, prepayment_invoice_id, prepayment_external_id = steps.create_base_invoice()
    balance_steps.InvoiceSteps.pay(prepayment_invoice_id)
    # Отправляем НЕчестные открутки:
    balance_steps.CampaignsSteps.do_campaigns(CONTEXT.product.service.id, orders_list[0]['ServiceOrderID'],
                                      {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)
    # Выставляем акт
    balance_steps.ActsSteps.generate(invoice_owner, force=1, date=ACT_DT)

    # Выставляем овердрафтнй счет, у кнопки "Внести оплату" не будет подсветки
    _, _, _, _, _, _, overdraft_invoice_id, overdraft_external_id = steps.create_base_invoice(client_id=invoice_owner,
                                                                                              person_id=person_id,
                                                                                              overdraft=1)
    return overdraft_invoice_id, overdraft_external_id


@dispatcher.add_method
def test_overdraft_overpaid_invoice():
    qty = 50
    receipt_sum = 9000
    client_id, _, _, service_order_id_list, _, _, invoice_id, external_id = steps.create_base_invoice(overdraft=1,
                                                                                                      orders_amount=3,
                                                                                                      qty=qty)

    balance_steps.InvoiceSteps.pay(invoice_id, payment_sum=receipt_sum)
    for service_order_id in service_order_id_list:
        balance_steps.InvoiceSteps.free_funds_to_order(invoice_id, CONTEXT.service.id, service_order_id,
                                                       sum=1500, mode=1)

    return client_id, invoice_id, external_id, service_order_id_list[0]


def test_credit_overpaid_invoice():
    qty = 50
    receipt_sum = 4500
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=3, qty=qty, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    repayment_invoice_id_1 = balance_steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_1)[0]

    balance_steps.InvoiceSteps.pay(repayment_invoice_id_1, payment_sum=receipt_sum)
    return client_id, fictive_invoice_id_1, repayment_invoice_id_1


@dispatcher.add_method
def test_fictive_pa_y_invoice():
    qty = 50
    context = CONTEXT.new(person_type=PersonTypes.PH,
                          paysys=Paysyses.BANK_PH_RUB,
                          person_params=common_defaults.FIXED_PH_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'SERVICES': [context.service.id],
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'UNILATERAL': 1,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_pa_invoice_id, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=1, qty=qty, credit=1, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    balance_steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_list[0], {'Bucks': QTY}, 0,
                                              campaigns_dt=COMPLETIONS_DT)
    balance_steps.ActsSteps.generate(client_id, 1, ACT_DT)
    y_invoice_id, _ = balance_steps.InvoiceSteps.get_invoice_ids(client_id, InvoiceType.Y_INVOICE)
    return client_id, fictive_pa_invoice_id, y_invoice_id

# Кредиты. Выставление счета, есть задолженность (Новая схема)
@dispatcher.add_method
def test_request_fictive_pa_debt(login=None):
    qty = 50
    context = CONTEXT.new(person_type=PersonTypes.UR,
                          paysys=Paysyses.BANK_UR_RUB,
                          person_params=common_defaults.FIXED_UR_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'SERVICES': [context.service.id],
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'UNILATERAL': 1,
                       'EXTERNAL_ID': 'кредитный договорушка'
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_pa_invoice_id, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=1, qty=qty, credit=1, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    balance_steps.ClientSteps.link(client_id, login)
    balance_steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_list[0], {'Bucks': QTY}, 0,
                                              campaigns_dt=COMPLETIONS_DT)
    balance_steps.ActsSteps.generate(client_id, 1, ACT_DT)
    request_id_2 = balance_steps.RequestSteps.create(client_id, orders_list)
    return client_id, person_id, contract_id, fictive_pa_invoice_id, request_id_2


# Кредиты. Выставление счета, есть почти просроченная задолженность (Новая схема)
@dispatcher.add_method
def test_request_fictive_pa_almost_overdue_debt(login=None):
    qty = 50
    context = CONTEXT.new(person_type=PersonTypes.PH,
                          paysys=Paysyses.BANK_PH_RUB,
                          person_params=common_defaults.FIXED_PH_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'SERVICES': [context.service.id],
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'UNILATERAL': 1,
                       'EXTERNAL_ID': 'кредитный договорушка'
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_pa_invoice_id, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=1, qty=qty, credit=1, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    balance_steps.ClientSteps.link(client_id, login)
    balance_steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_list[0], {'Bucks': QTY}, 0,
                                              campaigns_dt=COMPLETIONS_DT)
    act_list = balance_steps.ActsSteps.generate(client_id, force=1, date=ACT_DT)
    balance_steps.ActsSteps.set_payment_term_dt(act_list[0], dt=TOMORROW)
    request_id_2 = balance_steps.RequestSteps.create(client_id, orders_list)
    return client_id, fictive_pa_invoice_id, request_id_2

# Кредиты. Выставление счета, есть просроченная задолженность (Новая схема)
@dispatcher.add_method
def test_request_fictive_pa_overdue_debt(login=None):
    qty = 50
    context = CONTEXT.new(person_type=PersonTypes.PH,
                          paysys=Paysyses.BANK_PH_RUB,
                          person_params=common_defaults.FIXED_PH_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'SERVICES': [context.service.id],
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'UNILATERAL': 1,
                       'EXTERNAL_ID': 'кредитный договорушка'
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_pa_invoice_id, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=1, qty=qty, credit=1, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    balance_steps.ClientSteps.link(client_id, login)
    balance_steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_list[0], {'Bucks': QTY}, 0,
                                              campaigns_dt=COMPLETIONS_DT)
    act_list = balance_steps.ActsSteps.generate(client_id, force=1, date=ACT_DT)
    balance_steps.ActsSteps.set_payment_term_dt(act_list[0], dt=YESTERDAY)
    request_id_2 = balance_steps.RequestSteps.create(client_id, orders_list)
    return client_id, fictive_pa_invoice_id, request_id_2

@dispatcher.add_method
def test_credit_pa_no_completions():
    qty = 150
    context = CONTEXT.new(person_type=PersonTypes.PH,
                          paysys=Paysyses.BANK_PH_RUB,
                          person_params=common_defaults.FIXED_PH_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'SERVICES': [context.service.id],
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'UNILATERAL': 1,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_pa_invoice_id, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=3, qty=qty, credit=1, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    return client_id, fictive_pa_invoice_id, service_order_id_list[0]


@dispatcher.add_method
def test_charge_note_and_pa_invoice():
    context = CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0)
    service_id = context.service.id
    service_order_id = api.medium().GetOrdersInfo({'ContractID': contract_id})[0]['ServiceOrderID']
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': NOW}]
    request_id = balance_steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW,
                                                              'InvoiceDesireType': 'charge_note'})
    charge_note_id, _, _ = balance_steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                     credit=0, contract_id=contract_id)
    pa_invoice_id, _ = balance_steps.InvoiceSteps.get_invoice_ids(client_id, InvoiceType.PERSONAL_ACCOUNT)
    return client_id, charge_note_id, pa_invoice_id


@dispatcher.add_method
def test_overpaid_prepayment_with_agency():
    qty = 50
    receipt_sum = 9000
    client_id, _, _, _, _, _, invoice_id, external_id = steps.create_base_invoice(qty=qty, overdraft=0,
                                                                                  orders_amount=3, need_agency=True)

    balance_steps.InvoiceSteps.pay(invoice_id, payment_sum=receipt_sum)
    return client_id, invoice_id, external_id


# кнопка "Возврат" в платежах
@dispatcher.add_method
def test_trust_refund():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(
        service=Services.AUTORU,
        product=Products.AUTORU,
        currency=Currencies.RUB,
        firm=Firms.VERTICAL_12,
        person_type=PersonTypes.UR,
        paysys=Paysyses.BANK_UR_RUB,
        additional_contract_params={},
        person_params=common_defaults.FIXED_UR_PARAMS
    )
    client_id, _, _, _, _, _, invoice_id, external_id = steps.create_base_invoice(context=context, qty=3000)
    transaction_id = str(uuid.uuid4())
    db.balance().execute("INSERT INTO BO.T_PAYMENT (ID , INVOICE_ID, PAYSYS_CODE, DT, AMOUNT, CURRENCY, RESP_CODE, "
                         "PAYMENT_DT, TRANSACTION_ID, RESP_DESC, SERVICE_ID, CREATOR_UID)  "
                         "VALUES (S_PAYMENT_ID.nextval, :invoice_id, 'TRUST_API', :dt, :amount, 'RUR', 'success', "
                         ":payment_dt, :transaction_id, :resp_desc, :service_id, :creator_uid)",
                         {'invoice_id': invoice_id, 'dt': datetime.datetime.now(), 'amount': 10,
                          'payment_dt': datetime.datetime.now(), 'transaction_id': transaction_id,
                          'resp_desc': 'success', 'service_id': '99', 'creator_uid': Users.YB_ADM.uid})
    payment_res, = db.balance().execute(
        "SELECT id FROM bo.t_payment where paysys_code = 'TRUST_API' AND transaction_id = :id",
        {'id': transaction_id}
    )
    cash_fact_id, _ = balance_steps.InvoiceSteps.create_cash_payment_fact(external_id, 3750, datetime.datetime.now(),
                                                                          'ACTIVITY', payment_res['id'], invoice_id)
    return client_id, invoice_id, external_id


# старый ЛС, задолженности + превышение лимита

@dispatcher.add_method
def test_request_old_pa_big_sum_and_debt(login=None):
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                   person_type=PersonTypes.SW_UR,
                                                   paysys=Paysyses.BANK_SW_UR_USD,
                                                   contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                                   person_params=common_defaults.FIXED_SW_UR_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 10,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': D('500'),
                       'SERVICES': [context.service.id],
                       'PERSONAL_ACCOUNT': 1,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.USD.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'контрактушка'
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, old_pa_invoice_id, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=1, qty=1000, credit=1, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    balance_steps.ClientSteps.link(client_id, login)
    balance_steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_list[0], {'Bucks': QTY}, 0,
                                              campaigns_dt=COMPLETIONS_DT)
    balance_steps.ActsSteps.generate(client_id, 1, ACT_DT)
    request_id_2 = balance_steps.RequestSteps.create(client_id, orders_list)
    return client_id, person_id, contract_id, old_pa_invoice_id, request_id_2

@dispatcher.add_method
def test_request_old_pa_almost_overdue_debt(login=None):
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                   person_type=PersonTypes.SW_UR,
                                                   paysys=Paysyses.BANK_SW_UR_USD,
                                                   contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                                   person_params=common_defaults.FIXED_SW_UR_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 10,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': D('500'),
                       'SERVICES': [context.service.id],
                       'PERSONAL_ACCOUNT': 1,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.USD.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'контрактушка'
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_pa_invoice_id, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=1, qty=100, credit=1, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    balance_steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_list[0], {'Bucks': QTY}, 0,
                                              campaigns_dt=COMPLETIONS_DT)
    act_list = balance_steps.ActsSteps.generate(client_id, force=1, date=ACT_DT)
    balance_steps.ActsSteps.set_payment_term_dt(act_list[0], dt=TOMORROW)
    request_id_2 = balance_steps.RequestSteps.create(client_id, orders_list)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, fictive_pa_invoice_id, request_id_2


@dispatcher.add_method
def test_request_old_pa_overdue_debt(login=None):
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4,
                                                   person_type=PersonTypes.USU,
                                                   paysys=Paysyses.BANK_US_UR_USD,
                                                   contract_type=ContractCommissionType.USA_OPT_CLIENT,
                                                   person_params=common_defaults.FIXED_USU_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': D('123123'),
                       'SERVICES': [context.service.id],
                       'PERSONAL_ACCOUNT': 1,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.USD.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'контрактушка'
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_pa_invoice_id, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=1, qty=100, credit=1, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    balance_steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_list[0], {'Bucks': QTY}, 0,
                                              campaigns_dt=COMPLETIONS_DT)
    act_list = balance_steps.ActsSteps.generate(client_id, force=1, date=ACT_DT)
    balance_steps.ActsSteps.set_payment_term_dt(act_list[0], dt=YESTERDAY)
    request_id_2 = balance_steps.RequestSteps.create(client_id, orders_list)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, fictive_pa_invoice_id, request_id_2


@dispatcher.add_method
def test_pay_invoice(client_id):
    invoice_id = balance_steps.invoice_steps.InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)[0]['id']
    balance_steps.invoice_steps.InvoiceSteps.pay(invoice_id)
    return invoice_id

@dispatcher.add_method
def test_get_invoice_id(client_id):
    invoice_id = balance_steps.invoice_steps.InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)[0]['id']
    return invoice_id


# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import datetime
from datetime import timedelta

import pytest
import balance.balance_db as db
from hamcrest import equal_to
from decimal import Decimal
from dateutil.relativedelta import relativedelta
from btestlib.data.defaults import Date

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features
from btestlib.constants import Services
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, ContractCommissionType, \
    Currencies


MAIN_DT = datetime.datetime.now()

DIRECT_CONTEXT_FIRM_1 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                             contract_type=ContractCommissionType.OPT_CLIENT)
DIRECT_CONTEXT_FIRM_4 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4,
                                                             person_type=PersonTypes.USU,
                                                             paysys=Paysyses.BANK_US_UR_USD,
                                                             contract_type=ContractCommissionType.USA_OPT_CLIENT)
DIRECT_CONTEXT_FIRM_7 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                             person_type=PersonTypes.SW_UR,
                                                             paysys=Paysyses.BANK_SW_UR_CHF,
                                                             contract_type=ContractCommissionType.SW_OPT_CLIENT)

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.today())
CONTRACT_START_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=4))
CONTRACT_END_DT = utils.Date.date_to_iso_format(TODAY + relativedelta(months=3))
ALMOST_OVERDUE_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=3) - relativedelta(days=1))
ALMOST_OVERDUE_DT_NOT_RUB = utils.Date.date_to_iso_format(TODAY - relativedelta(months=3) - relativedelta(days=6))
OVERDUE_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=3) - relativedelta(days=21))

CREDIT_LIMIT_RUB = Decimal('5700')
CREDIT_LIMIT_CHF_USD = Decimal('78')
QTY = Decimal('50')
# богомерзкий хардкод
OVERDUE_CHF = '22,08 CHF'
OVERDUE_USD = '20,50 USD'


SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
            Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
            Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]


def test_paystep_overdue_warning_firm_1(get_free_user):
    context = DIRECT_CONTEXT_FIRM_1
    client_id = steps.ClientSteps.create()
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': CONTRACT_START_DT,
                       'FINISH_DT': CONTRACT_END_DT,
                       'IS_SIGNED': CONTRACT_START_DT,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       }

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    collateral_id = steps.ContractSteps.get_collateral_id(contract_id)
    update_personal_account_attributes(contract_id, collateral_id)

    # создаем первый фиктивный счет и счет на погашение, почти просроченный
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': ALMOST_OVERDUE_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': ALMOST_OVERDUE_DT})
    fictive_invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=2,
                                                           contract_id=contract_id)
    repayment_invoice_id_1 = steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_1)[0]
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': QTY}, 0, ALMOST_OVERDUE_DT)
    act_id = steps.ActsSteps.create(repayment_invoice_id_1, ALMOST_OVERDUE_DT)[0]
    steps.ActsSteps.set_payment_term_dt(act_id, TODAY + timedelta(days=1))

    # создаем второй фиктивный счет и счет на погашение, просроченный
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': OVERDUE_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': OVERDUE_DT})
    fictive_invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id, person_id,
                                                           context.paysys.id, credit=2,
                                                           contract_id=contract_id)

    repayment_invoice_id_2 = steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_2)[0]
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': QTY}, 0, OVERDUE_DT)
    steps.ActsSteps.create(repayment_invoice_id_2, OVERDUE_DT)

    # создаем реквест для того, чтобы попасть на пейстеп и проверить все плашки
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': OVERDUE_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': OVERDUE_DT})

    # проверяем предупреждения
    with web.Driver() as driver:
        paypreview_page = web.ClientInterface.PaypreviewPage.open(driver, request_id=request_id, person_id=person_id,
                                                                  paysys_id=context.paysys.id, contract_id=contract_id)
        paypreview_page.turn_off_experiment()
        alert_message_title = paypreview_page.get_alert_message_title()
        alert_message_text = paypreview_page.get_alert_text()
        notify_message_title = paypreview_page.get_notify_message_title()
        notify_message_text = paypreview_page.get_notify_text()

    utils.check_that(alert_message_title, equal_to(u'Внимание: просроченная задолженность'),
                     u'Проверяем заголовок алерта')
    # сумму хардкожу, чтобы не заморачиваться с пробелами
    utils.check_that(alert_message_text, equal_to(u'Сумма просроченной задолженности по кредиту: 1 500,00 руб.\n'
                                                  u'Погасите, пожалуйста, просроченную задолженность '
                                                  u'в максимально короткие сроки.'),
                     u'Проверяем текст алерта')
    utils.check_that(notify_message_title, equal_to(u'Напоминание: истекает срок погашения задолженности'),
                     u'Проверяем заголовок уведомления')
    utils.check_that(notify_message_text, equal_to(u'Подходит крайний срок погашения кредита.\n'
                                                   u'Пожалуйста, не забывайте своевременно погашать задолженность.'),
                     u'Проверяем текст уведомления')


@pytest.mark.parametrize('context, currency, overdue_sum', [
    (DIRECT_CONTEXT_FIRM_7, Currencies.CHF.num_code, OVERDUE_CHF),
    (DIRECT_CONTEXT_FIRM_4, Currencies.USD.num_code, OVERDUE_USD)
                        ],
                         ids=['Firm 7, CHF, SW_UR',
                              'Firm 4, USD, USU'])
def test_paystep_overdue_warning_personal_acc(context, currency, overdue_sum, get_free_user):
    client_id = steps.ClientSteps.create()
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': CONTRACT_START_DT,
                       'FINISH_DT': CONTRACT_END_DT,
                       'IS_SIGNED': CONTRACT_START_DT,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_CHF_USD,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0,
                       'CURRENCY': str(currency),
                       'FIRM': context.firm.id,
                       }

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)

    # создаем счет с двумя заказами
    service_order_id_1 = steps.OrderSteps.next_id(context.service.id)
    service_order_id_2 = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id_1, context.product.id, context.service.id)
    steps.OrderSteps.create(client_id, service_order_id_2, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_1, 'Qty': QTY,
         'BeginDT': ALMOST_OVERDUE_DT_NOT_RUB},
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY,
         'BeginDT': OVERDUE_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=2,
                                                   contract_id=contract_id)

    # закрываемся, чтобы был просроченный и почти просроченный акт
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_1, {'Bucks': QTY}, 0, ALMOST_OVERDUE_DT_NOT_RUB)
    steps.ActsSteps.create(invoice_id_1, ALMOST_OVERDUE_DT_NOT_RUB)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Bucks': QTY}, 0, OVERDUE_DT)
    steps.ActsSteps.create(invoice_id_1, OVERDUE_DT)

    # создаем реквест для того, чтобы попасть на пейстеп и проверить все плашки
    steps.OrderSteps.create(client_id, service_order_id_2, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY,
         'BeginDT': OVERDUE_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': OVERDUE_DT})

    # проверяем предупреждения
    with web.Driver() as driver:
        paypreview_page = web.ClientInterface.PaypreviewPage.open(driver, request_id=request_id, person_id=person_id,
                                                                  paysys_id=context.paysys.id, contract_id=contract_id)
        paypreview_page.turn_off_experiment()
        alert_message_title = paypreview_page.get_alert_message_title()
        alert_message_text = paypreview_page.get_alert_text()
        notify_message_title = paypreview_page.get_notify_message_title()
        notify_message_text = paypreview_page.get_notify_text()

    utils.check_that(alert_message_title, equal_to(u'Внимание: просроченная задолженность'),
                     u'Проверяем заголовок алерта')
    # сумму хардкожу, чтобы не заморачиваться с пробелами
    utils.check_that(alert_message_text, equal_to(u'Сумма просроченной задолженности по кредиту: ' + overdue_sum + '\n'
                                                  u'Погасите, пожалуйста, просроченную задолженность '
                                                  u'в максимально короткие сроки.'),
                     u'Проверяем текст алерта')
    utils.check_that(notify_message_title, equal_to(u'Напоминание: истекает срок погашения задолженности'),
                     u'Проверяем заголовок уведомления')
    utils.check_that(notify_message_text, equal_to(u'Подходит крайний срок погашения кредита.\n'
                                                   u'Пожалуйста, не забывайте своевременно погашать задолженность.'),
                     u'Проверяем текст уведомления')


# хак, т.к. сейчас невозможно создать клиентский оптовый договор без лицевого счета
# схема с фиктивными счетами тем не менее жива, потому апдейтим в базе галку personal_account
def update_personal_account_attributes(contract_id, collateral_id):
    query = 'update t_contract_attributes set value_num = 0 ' \
            'WHERE collateral_id = {collateral_id} and ' \
            '(code = \'PERSONAL_ACCOUNT\' or code = \'PERSONAL_ACCOUNT_FICTIVE\' ' \
            'or code = \'LIFT_CREDIT_ON_PAYMENT\')'.format(collateral_id=collateral_id)
    db.balance().execute(query)
    steps.ContractSteps.refresh_contracts_cache(contract_id)


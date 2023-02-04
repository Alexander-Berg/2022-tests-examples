# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
from decimal import Decimal

import pytest
from hamcrest import equal_to, contains_string

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features
from btestlib.constants import Firms, ContractPaymentType, Permissions
from btestlib.data.defaults import Order
from btestlib.matchers import equal_to_casted_dict
from temp.igogor.balance_objects import Contexts

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT
MARKET = Contexts.MARKET_RUB_CONTEXT
BAYAN = Contexts.BAYAN_FISH_RUB
MEDIA = Contexts.MEDIA_70_SHOWS_RUB

ORDER_QTY = Decimal('100')
CAMPAIGN_QTY = Decimal('30')
DT = datetime.datetime.now()
START_DT = utils.Date.nullify_time_of_date(DT).isoformat()

pytestmark = [pytest.mark.tickets('BALANCE-21219'),
              reporter.feature(Features.UI, Features.INVOICE, Features.REVERSE, Features.CREDIT)
              ]

#### Если сумма указана и
#### XXX - сумма неоткрученных средств заказа (при подтверждении указанная сумма снимается с заказа и возвращается на кредит )
#### на узаказанном заказе не осталось свободных средств, то выводится сообщение "На данном заказе нет свободных средств", ничего не происходит
def test_postpayment_invoice_no_free_funds():
    expected_consume = {'current_qty': ORDER_QTY, 'current_sum': ORDER_QTY * 30}

    client_id, invoice_id, service_order_id, order_id = create_orders_with_campaigns(orders_number=2)
    steps.CampaignsSteps.do_campaigns(DIRECT.service.id, service_order_id, campaigns_params={'Bucks': ORDER_QTY},
                                      campaigns_dt=DT)
    with web.Driver() as driver:
        invoice_page = web.AdminInterface.InvoicePage.open(driver, invoice_id)
        invoice_page.set_amount('1')
        invoice_page.check_return_button_availability()
        invoice_page.set_order(DIRECT.service.id, DIRECT.product.id, service_order_id, client_id)
        invoice_page.rollback_button()
        invoice_page.check_rollback_message(u'На данном заказе нет свободных средств')
        invoice_page.check_return_button_availability(disable=False)

    fact_t_consume = get_consume_values(order_id)
    utils.check_that(fact_t_consume, equal_to_casted_dict(expected_consume), u'Проверяем значения в базе')


#### Проверяем отображение блока возврата в зависимости от наличия прав  WithdrawConsumesPostpay/WithdrawConsumesPrepay
#### https://st.yandex-team.ru/BALANCE-31395
@pytest.mark.parametrize('user_perm, is_visible, is_credit',
                         [
                             (Permissions.WITHDRAW_CONSUMES_PREPAY, True, False),
                             (None, False, False),
                             (Permissions.WITHDRAW_CONSUMES_POSTPAY, True, True),
                             (None, False, True)
                         ])
def test_prepayment_invoice_no_rollback_block_wo_permission(user_perm, is_visible, is_credit, get_free_user):
    default_perm = [Permissions.ADMIN_ACCESS_0,
                    Permissions.VIEW_INVOICES,
                    Permissions.CREATE_BANK_PAYMENTS_3,
                    ]
    if user_perm:  default_perm.append(user_perm)
    user = get_free_user()
    steps.UserSteps.set_role_with_permissions_strict(user, default_perm)
    client_id, invoice_id, service_order_id, order_id = create_orders_with_campaigns(is_credit=is_credit)
    with web.Driver(user=user) as driver:
        invoice_page = web.AdminInterface.InvoicePage.open(driver, invoice_id)
        utils.check_that(invoice_page.is_rollback_block_present_on_page(), equal_to(is_visible))


#### В счетах на медийку(70) нет блока возврата
def test_prepayment_invoice_no_rollback_block_for_media_service():
    client_id, invoice_id, service_order_id, order_id = create_orders_with_campaigns(context=MEDIA,
                                                                                     is_credit=0)
    with web.Driver() as driver:
        invoice_page = web.AdminInterface.InvoicePage.open(driver, invoice_id)
        utils.check_that(invoice_page.is_rollback_block_present_on_page(), equal_to(False))


def get_consume_values(order_id):
    with reporter.step(u'Получаем sum и qty для консьюма с номером заказа: {}'.format(order_id)):
        query = 'SELECT CURRENT_SUM, CURRENT_QTY FROM T_CONSUME WHERE PARENT_ORDER_ID =:order_id'
        params = {'order_id': order_id}

        result = db.balance().execute(query, params)[0]
        reporter.attach(u"Значения консьюма", utils.Presenter.pretty(result))

        return result


def get_reverse_values(order_id):
    with reporter.step(u'Получаем sum и qty для реверса с номером заказа: {}'.format(order_id)):
        query = 'SELECT REVERSE_SUM, REVERSE_QTY FROM T_REVERSE WHERE PARENT_ORDER_ID =:order_id'
        params = {'order_id': order_id}

        result = db.balance().execute(query, params)[0]
        reporter.attach(u"Значения реверса", utils.Presenter.pretty(result))

        return result


# a-vasin: создает переменное количество заказов и возвращает последний
def create_orders_with_campaigns(orders_number=1, is_credit=1, context=DIRECT, firm_id=1, need_campaings=True,
                                 need_payment=True):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})

    invoice_owner = agency_id
    order_owner = client_id
    person_id = steps.PersonSteps.create(invoice_owner, context.person_type.code)
    contract_id = None
    if is_credit:
        contract_id = steps.ContractSteps.create_contract_new('opt_agency_prem',
                                                              {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                               'IS_FAXED': START_DT, 'DT': START_DT,
                                                               'FIRM': firm_id, 'SERVICES': [
                                                                  context.service.id],
                                                               'PAYMENT_TYPE': ContractPaymentType.POSTPAY})[
            0]

    orders_list = []
    for _ in xrange(orders_number):
        service_order_id = steps.OrderSteps.next_id(context.service.id)
        order_id = steps.OrderSteps.create(order_owner, service_order_id, context.product.id, context.service.id,
                                           {'TEXT': 'Py_Test order', 'AgencyID': invoice_owner, 'ManagerUID': None})
        orders_list += Order.default_orders_list(service_order_id, service_id=context.service.id, qty=ORDER_QTY)

    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=is_credit, contract_id=contract_id, overdraft=0,
                                                 endbuyer_id=None)
    if need_payment and is_credit == 0:
        steps.InvoiceSteps.pay(invoice_id)
    if need_campaings:
        for order in orders_list:
            steps.CampaignsSteps.do_campaigns(context.service.id, order['ServiceOrderID'],
                                              campaigns_params={'Bucks': CAMPAIGN_QTY},
                                              campaigns_dt=DT)

    return agency_id, invoice_id, service_order_id, order_id


def check_data_in_base(order_id, expected_consume, expected_reverse):
    fact_t_consume = get_consume_values(order_id)
    fact_t_reverse = get_reverse_values(order_id)
    utils.check_that(fact_t_consume, equal_to_casted_dict(expected_consume), u'Проверяем значения в базе')
    utils.check_that(fact_t_reverse, equal_to_casted_dict(expected_reverse), u'Проверяем значения в базе')

# coding: utf-8

__author__ = 'atkaya'

import time
import pytest
from hamcrest import equal_to

import balance.balance_steps as steps
import balance.balance_web as web
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_db as db
from balance.features import AuditFeatures, Features
from btestlib import secrets
from btestlib.constants import PersonTypes, Services, Products, User, Firms, ContractCommissionType, Cards
from temp.igogor.balance_objects import Contexts

DIRECT_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, contract_type=ContractCommissionType.NO_AGENCY)
MARKET_CONTEXT = DIRECT_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                      firm=Firms.MARKET_111)
QTY = 50
USER_CARD_DIRECT = User(1600636679, 'yb-card-user-1', secrets.get_secret(*secrets.UsersPwd.HERMIONE_CI_PWD_NEW))
USER_CARD_MARKET = User(1600640433, 'yb-card-user-2', secrets.get_secret(*secrets.UsersPwd.HERMIONE_CI_PWD_NEW))


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4))
@reporter.feature(Features.TRUST)
@pytest.mark.parametrize('context, user', [
    pytest.param(DIRECT_CONTEXT, USER_CARD_DIRECT, id='Direct'),
    pytest.param(MARKET_CONTEXT, USER_CARD_MARKET, id='Market'),
])
def test_invoice_ci_pay_by_card(context, user):
    service_id = context.service.id
    product_id = context.product.id

    client_id = steps.ClientSteps.create()
    steps.UserSteps.link_user_and_client(user, client_id)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    service_order_id = steps.OrderSteps.next_id(service_id)

    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id, service_id=service_id)
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY}]
    request_id = steps.RequestSteps.create(client_id, orders_list)

    with web.Driver(user=user) as driver:
        paychoose_page = web.ClientInterface.PaychoosePage.open(driver, request_id)
        paychoose_page.turn_off_experiment()
        paychoose_page.choose_ur()
        paysys = paychoose_page.INPUT_PAYMENT_BY_CARD_1033 if context == DIRECT_CONTEXT \
            else paychoose_page.INPUT_PAYMENT_BY_CARD_11101033
        paypreview_page = paychoose_page.pay_by(paysys)

        success_page = paypreview_page.generate_invoice()

        # процессинг директа - альфа, маркета - траст
        alpha, card = (True, Cards.VALID_3DS) if context == DIRECT_CONTEXT else (False, Cards.VALID_3DS_2_DIGIT_YEAR)
        payment_page = success_page.pay(alpha)
        # не заходим на страницу счета. платеж проверяем в БД
        payment_page.pay(card=card)

    invoice_id = steps.InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)[0]['id']
    receipt_sum, consume_sum = get_receipt_sum(invoice_id)
    utils.check_that(receipt_sum, equal_to(QTY * DIRECT_CONTEXT.price), u'Проверяем, что есть поступления')
    utils.check_that(consume_sum, equal_to(QTY * DIRECT_CONTEXT.price), u'Проверяем, что есть средства на заявках')


@reporter.feature(Features.TRUST)
@pytest.mark.parametrize('context, user', [
    pytest.param(DIRECT_CONTEXT, USER_CARD_DIRECT, id='Direct'),
    pytest.param(MARKET_CONTEXT, USER_CARD_MARKET, id='Market'),
])
def test_invoice_ci_pay_by_card_new_paystep(context, user):
    service_id = context.service.id
    product_id = context.product.id

    client_id = steps.ClientSteps.create()
    steps.UserSteps.link_user_and_client(user, client_id)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    service_order_id = steps.OrderSteps.next_id(service_id)

    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id, service_id=service_id)
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY}]
    request_id = steps.RequestSteps.create(client_id, orders_list)

    with web.Driver(user=user) as driver:
        paystep_page = web.ClientInterface.PaystepPage.open(driver, request_id)
        paystep_page.choose_card()

        # процессинг директа - альфа, маркета - траст
        alpha, card = (True, Cards.VALID_3DS) if context == DIRECT_CONTEXT else (False, Cards.VALID_3DS_2_DIGIT_YEAR)
        payment_page = paystep_page.pay(alpha)

        # не заходим на страницу счета. платеж проверяем в БД
        invoice_page = payment_page.pay(card=card)
        if invoice_page:
            invoice_page.wait_for_data()

            for _i in range(5):
                payment_status = invoice_page.get_payment_status()
                if payment_status == u'Ваш платеж успешно завершен!':
                    break
                time.sleep(2)
                invoice_page.driver._driver.refresh()

    invoice_id = steps.InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)[0]['id']
    receipt_sum, consume_sum = get_receipt_sum(invoice_id)
    utils.check_that(receipt_sum, equal_to(QTY * DIRECT_CONTEXT.price), u'Проверяем, что есть поступления')
    utils.check_that(consume_sum, equal_to(QTY * DIRECT_CONTEXT.price), u'Проверяем, что есть средства на заявках')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4))
@pytest.mark.parametrize('context', [
    pytest.param(DIRECT_CONTEXT, id='Direct'),
    pytest.param(MARKET_CONTEXT, id='Market'),
])
def test_invoice_pay_by_bank(context):
    invoice_id = create_invoice(context)
    steps.InvoiceSteps.pay_fair(invoice_id)
    receipt_sum, consume_sum = get_receipt_sum(invoice_id)
    utils.check_that(receipt_sum, equal_to(QTY * context.price), u'Проверяем, что есть поступления')
    utils.check_that(consume_sum, equal_to(QTY * context.price), u'Проверяем, что есть средства на заявках')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4))
@pytest.mark.parametrize('context', [
    pytest.param(DIRECT_CONTEXT, id='Direct'),
    pytest.param(MARKET_CONTEXT, id='Market'),
])
def test_invoice_turn_on(context):
    invoice_id = create_invoice(context)
    steps.InvoiceSteps.turn_on_ai(invoice_id)
    receipt_sum, consume_sum = get_receipt_sum(invoice_id)
    utils.check_that(receipt_sum, equal_to(QTY * context.price), u'Проверяем, что есть поступления')
    utils.check_that(consume_sum, equal_to(QTY * context.price), u'Проверяем, что есть средства на заявках')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4))
@pytest.mark.parametrize('context', [
    pytest.param(DIRECT_CONTEXT, id='Direct'),
    pytest.param(MARKET_CONTEXT, id='Market'),
])
def test_receipt_overdraft(context):
    invoice_id = create_invoice(context, overdraft=1)
    receipt_sum, consume_sum = get_receipt_sum(invoice_id)
    utils.check_that(receipt_sum, equal_to(0), u'Проверяем, что нет поступлений (т.к. счет овердрафтный)')
    utils.check_that(consume_sum, equal_to(QTY * context.price), u'Проверяем, что есть средства на заявках')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4))
@pytest.mark.parametrize('context', [
    pytest.param(DIRECT_CONTEXT, id='Direct'),
    pytest.param(MARKET_CONTEXT, id='Market'),
])
def test_receipt_credit_fictive(context):
    invoice_id = create_invoice(context, credit=1, fictive=1)
    receipt_sum, consume_sum = get_receipt_sum(invoice_id)
    utils.check_that(receipt_sum, equal_to(QTY * context.price), u'Проверяем, что есть поступления')
    utils.check_that(consume_sum, equal_to(QTY * context.price), u'Проверяем, что есть средства на заявках')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4))
@pytest.mark.parametrize('context', [
    pytest.param(DIRECT_CONTEXT, id='Direct'),
    pytest.param(MARKET_CONTEXT, id='Market'),
])
def test_receipt_credit_pa(context):
    invoice_id = create_invoice(context, credit=1)
    receipt_sum, consume_sum = get_receipt_sum(invoice_id)
    utils.check_that(receipt_sum, equal_to(0), u'Проверяем, что нет поступлений (т.к. счет кредитный)')
    utils.check_that(consume_sum, equal_to(QTY * context.price), u'Проверяем, что есть средства на заявках')


def create_invoice(context, overdraft=0, credit=0, fictive=0):
    service_id = context.service.id
    product_id = context.product.id
    contract_id = None

    client_id = steps.ClientSteps.create()
    if overdraft:
        steps.OverdraftSteps.set_force_overdraft(client_id, service_id, 100000, firm_id=context.firm.id)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    if credit:
        _, _, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(context, client_id, person_id,
                                                                                      postpay=1, fictive_scheme=fictive)

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id, service_id=service_id)
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                 credit=credit, overdraft=overdraft,
                                                 contract_id=contract_id)
    return invoice_id


def get_receipt_sum(invoice_id):
    with reporter.step(u'Получаем количество зачисленных средств на счету: {}'.format(invoice_id)):
        query = "SELECT RECEIPT_SUM, CONSUME_SUM FROM T_INVOICE WHERE id = :invoice_id"
        params = {'invoice_id': invoice_id}

        sums = db.balance().execute(query, params)
        receipt_sum = sums[0]['receipt_sum']
        consume_sum = sums[0]['consume_sum']
        reporter.attach(u'Сумма зачислений', utils.Presenter.pretty(receipt_sum))
        reporter.attach(u'Сумма заявок', utils.Presenter.pretty(consume_sum))
        return receipt_sum, consume_sum

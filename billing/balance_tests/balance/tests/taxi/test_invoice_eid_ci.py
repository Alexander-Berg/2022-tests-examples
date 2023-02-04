# coding: utf-8

__author__ = 'a-vasin'

import pytest
from hamcrest import equal_to

import balance.balance_steps as steps
import balance.balance_web as web
import btestlib.environments as env
import btestlib.passport_steps as passport_steps
import btestlib.reporter as reporter
from balance.features import Features, AuditFeatures
from btestlib.data.defaults import Taxi
from btestlib.data.partner_contexts import *

pytestmark = [
    reporter.feature(Features.UI, Features.TAXI, Features.INVOICE)
]

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now())
ADVANCE_PAYMENT_SUM = Decimal('100')
QTY = Decimal('50')

PARAMS = [
    (TAXI_RU_CONTEXT, 1),
    (TAXI_BV_GEO_USD_CONTEXT, 0),
    (TAXI_BV_LAT_EUR_CONTEXT, 0),
    (TAXI_UBER_BV_BY_BYN_CONTEXT, 1),
    (TAXI_UBER_BV_BYN_BY_BYN_CONTEXT, 1),
    (TAXI_UBER_BV_AZN_USD_CONTEXT, 0),
    (TAXI_UBER_BV_BYN_AZN_USD_CONTEXT, 0),
    (TAXI_ISRAEL_CONTEXT, 0),
    (TAXI_GHANA_USD_CONTEXT, 0),
    (TAXI_BOLIVIA_USD_CONTEXT, 0),
    (TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, 1),  # нужна доработка печатных форм
    (TAXI_ZA_USD_CONTEXT, 0),
]

REGION_TO_HEADER_TEXT = {
     TAXI_RU_CONTEXT.name: u'ЛИЦЕВОЙ СЧЕТ № {}',
     TAXI_BV_GEO_USD_CONTEXT.name: u'{} dated',
     TAXI_BV_LAT_EUR_CONTEXT.name: u'{} dated',
     TAXI_UBER_BV_BY_BYN_CONTEXT.name: u'{} dated',
     TAXI_UBER_BV_BYN_BY_BYN_CONTEXT.name: u'{} dated',
     TAXI_UBER_BV_AZN_USD_CONTEXT.name: u'{} dated',
     TAXI_UBER_BV_BYN_AZN_USD_CONTEXT.name: u'{} dated',
     TAXI_ISRAEL_CONTEXT.name: u'{} dated',
     TAXI_GHANA_USD_CONTEXT.name: u'{} dated',
     TAXI_BOLIVIA_USD_CONTEXT.name: u'{} dated',
     TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT.name: u'{} dated',  # нужна доработка печатных форм
     TAXI_ZA_USD_CONTEXT.name: u'{} dated',
}

REGION_TO_PRODUCT_TEXT = {
     TAXI_RU_CONTEXT.name: u'Предоставление доступа к сервису Яндекс.Такси, лицевой счет номер {}.',
     TAXI_BV_GEO_USD_CONTEXT.name: u'personal account {}.',
     TAXI_BV_LAT_EUR_CONTEXT.name: u'personal account {}.',
     TAXI_UBER_BV_BY_BYN_CONTEXT.name: u'personal account {}.',
     TAXI_UBER_BV_BYN_BY_BYN_CONTEXT.name: u'personal account {}.',
     TAXI_UBER_BV_AZN_USD_CONTEXT.name: u'personal account {}.',
     TAXI_UBER_BV_BYN_AZN_USD_CONTEXT.name: u'personal account {}.',
     TAXI_ISRAEL_CONTEXT.name: u'personal account {}.',
     TAXI_GHANA_USD_CONTEXT.name: u'personal account {}.',
     TAXI_BOLIVIA_USD_CONTEXT.name: u'personal account {}.',
     TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT.name: u'personal account {}.',  # нужна доработка печатных форм
     TAXI_ZA_USD_CONTEXT.name: u'personal account {}.',
}


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4_Taxi))
@pytest.mark.parametrize("context, is_offer", PARAMS, ids=lambda c, u: c.name)
def test_invoice_eid_ci_pay_by_bank(context, is_offer, get_free_user):
    user = get_free_user()

    client_id, _, _, invoice_id, external_invoice_id, request_id = \
        prepare_request_and_invoice(user, context, is_offer)

    with web.Driver(user=user) as driver:
        paychoose_page = web.ClientInterface.PaychoosePage.open(driver, request_id)
        paychoose_page.turn_off_experiment()

        try:
            paypreview_page = paychoose_page.pay_by_bank_1301003(skip_choose=context.currency != Currencies.RUB)
        except web.NoSuchElementException:
            paypreview_page = web.ClientInterface.PaypreviewPage(driver)

        success_page = paypreview_page.generate_invoice()

        page_title = success_page.get_title()
        utils.check_that(page_title, equal_to(u'Выставлен счет № {}'.format(external_invoice_id)),
                         u'Проверяем, что заголовок страницы содержит верный external id счета')

        invoice_page = web.ClientInterface.InvoicePage.open(driver, invoice_id)
        invoice_title = invoice_page.get_title()
        utils.check_that(invoice_title, equal_to(u'Счет {}'.format(external_invoice_id)),
                         u'Проверяем, что заголовок страницы счета содержит верный external id счета')


@reporter.feature(Features.TRUST)
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4_Taxi))
def test_invoice_eid_ci_pay_by_card(get_free_user):
    user = get_free_user()
    context = TAXI_RU_CONTEXT

    _, _, _, invoice_id, external_invoice_id, request_id = \
        prepare_request_and_invoice(user, context)

    with web.Driver(user=user) as driver:
        paychoose_page = web.ClientInterface.PaychoosePage.open(driver, request_id)
        paychoose_page.turn_off_experiment()
        paypreview_page = paychoose_page.pay_by_card_1301033()

        success_page = paypreview_page.generate_invoice()

        page_title = success_page.get_title()
        utils.check_that(page_title, equal_to(u'Выставлен счет № {}'.format(external_invoice_id)),
                         u'Проверяем, что заголовок страницы содержит верный external id счета')

        alpha_bank_payment_page = success_page.pay()
        payment_description = alpha_bank_payment_page.get_payment_description()
        utils.check_that(payment_description, equal_to(u'Оплата счета {}'.format(external_invoice_id)),
                         u'Проверяем, что платеж имеет верное описание')

        invoice_page = alpha_bank_payment_page.pay()
        invoice_title = invoice_page.get_title()
        utils.check_that(invoice_title, equal_to(u'Счет {}'.format(external_invoice_id)),
                         u'Проверяем, что заголовок страницы счета содержит верный external id счета')

        payment_status = invoice_page.get_payment_status()
        utils.check_that(payment_status, equal_to(u'Ваш платеж успешно завершен!'),
                         u'Проверяем, что оплата прошла успешно')

    receipt_sum = get_receipt_sum(invoice_id)
    utils.check_that(receipt_sum, equal_to(QTY), u'Проверяем, что средства зачислены')


# -----------------------------------------------------------
# Utils

def get_personal_account_id(client_id):
    with reporter.step(u'Находим id ЛС такси для клиента: {}'.format(client_id)):
        query = "SELECT inv.id, inv.external_id " \
                "FROM T_INVOICE inv LEFT JOIN T_EXTPROPS prop ON " \
                "inv.ID = prop.OBJECT_ID AND prop.CLASSNAME='PersonalAccount' AND prop.ATTRNAME='service_code' " \
                "WHERE inv.CLIENT_ID=:client_id AND prop.VALUE_STR = 'YANDEX_SERVICE' "

        params = {'client_id': client_id}
        result = db.balance().execute(query, params)[0]
        return result['id'], result['external_id']


def prepare_request_and_invoice(user, context, is_offer=0):
    with reporter.step(u'Подготавливаем заявку на зачисление средств'):
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_offer=is_offer,
                                                                                           is_postpay=0,
                                                                                           additional_params={'start_dt': CONTRACT_START_DT})

        steps.UserSteps.link_user_and_client(user, client_id)

        invoice_id, external_invoice_id = get_personal_account_id(client_id)

        cash_product = Taxi.CURRENCY_TO_PRODUCT[context.currency]['cash']

        service_order_id = steps.OrderSteps.next_id(Taxi.CASH_SERVICE_ID)
        steps.OrderSteps.create(client_id, service_order_id, product_id=cash_product,
                                service_id=Taxi.CASH_SERVICE_ID)

        orders_list = [{'ServiceID': Taxi.CASH_SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                        'BeginDT': CONTRACT_START_DT}]
        request_id = steps.RequestSteps.create(client_id, orders_list)

        return client_id, contract_id, person_id, invoice_id, external_invoice_id, request_id


def get_receipt_sum(invoice_id):
    with reporter.step(u'Получаем количество зачисленных средств на счету: {}'.format(invoice_id)):
        query = "SELECT RECEIPT_SUM FROM T_INVOICE WHERE id = :invoice_id"
        params = {'invoice_id': invoice_id}

        receipt_sum = db.balance().execute(query, params)[0]['receipt_sum']
        reporter.attach(u'Сумма зачислений', utils.Presenter.pretty(receipt_sum))

        return receipt_sum


def get_printable_form_xml(invoice_id, user):
    with reporter.step(u'Получаем печатную форму в xml для invoice: {} и пользователя: {}'
                               .format(invoice_id, user.login)):
        url = '{base_url}:8080/invoice-publish.xml?ft=xml&object_id={invoice_id}'.format(
            base_url=env.balance_env().balance_ci, invoice_id=invoice_id)

        session = passport_steps.auth_session(user)
        form_xml = session.get(url, verify=False).text

        reporter.attach(u'Печатная форма в XML', utils.Presenter.pretty(form_xml), allure_=True)

        return form_xml

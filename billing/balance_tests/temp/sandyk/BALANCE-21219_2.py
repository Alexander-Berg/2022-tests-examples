# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
import time

import pytest
from hamcrest import equal_to
from selenium.common.exceptions import NoAlertPresentException
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.support import expected_conditions as ec
from selenium.webdriver.support.wait import WebDriverWait

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features

DT = datetime.datetime.now()
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
# SERVICE_ID = 37
# PRODUCT_ID = 502917
# Days

# SERVICE_ID = 48
# PRODUCT_ID = 503363

# SERVICE_ID = 11
# PRODUCT_ID = 2136

SERVICE_ID = 7
PRODUCT_ID = 1475
#
# SERVICE_ID = 70
# PRODUCT_ID= 506387


# SERVICE_ID = 67
# PRODUCT_ID = 502660

# SERVICE_ID = 77
# PRODUCT_ID =503024


# SERVICE_ID = 99
# PRODUCT_ID =507470


QTY = 100
START_DT = str(DT.strftime("%Y-%m-%d")) + 'T00:00:00'

pytestmark = [pytest.mark.tickets('BALANCE-21219')
    , reporter.feature(Features.UI, Features.INVOICE, Features.REVERSE, Features.CREDIT)
    , pytest.mark.no_parallel
              ]


def check_alert_text(driver, expected_message):
    try:
        alert = driver.switch_to_alert()
        utils.check_that(alert.text, equal_to(expected_message), u'Проверяем текст алерта')
        alert.accept()
    except NoAlertPresentException:
        print "no alert"


def get_db_values(order_id):
    try:
        t_consume = db.balance().execute(
            'select CURRENT_SUM, CURRENT_QTY from T_CONSUME where PARENT_ORDER_ID =:order_id', {'order_id': order_id})
        t_reverse = db.balance().execute(
            'select REVERSE_SUM, REVERSE_QTY from T_REVERSE where PARENT_ORDER_ID =:order_id', {'order_id': order_id})
    except TimeoutException:
        print "no data"
    return t_consume, t_reverse


@pytest.fixture(scope="module")
def data():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})

    invoice_owner = agency_id or None
    order_owner = client_id

    person_id = steps.PersonSteps.create(invoice_owner, PERSON_TYPE)

    contract_id = steps.ContractSteps.create_contract_new('opt_agency_prem',
                                                          {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                           'IS_FAXED': START_DT, 'DT': START_DT, 'FIRM': 1,
                                                           'SERVICES': [
                                                               SERVICE_ID], 'PAYMENT_TYPE': 3})[0]
    orders_list = []
    order_ids = []
    servise_order_ids = []
    for x in range(0, 6):
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        servise_order_ids.append(service_order_id)
        order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                           {'TEXT': 'Py_Test order', 'AgencyID': invoice_owner, 'ManagerUID': None})
        order_ids.append(order_id)
        orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    for order in orders_list:
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, order['ServiceOrderID'], {'Bucks': 30, 'Days': 0, 'Money': 0}, 0,
                                          campaigns_dt=DT)
    params = {'client_id': agency_id, 'invoice_id': invoice_id, 'service_order_ids': servise_order_ids,
              'order_ids': order_ids}
    return params


#### 1 Если заказ не выбран - выводится сообщение "Заказ не выбран" (поле обязательно для заполнения), ничего не происходит
def test_empty_order(data):
    my_driver = web.Driver()
    with my_driver as driver:
        web.Invoice.open(driver, data['invoice_id'])
        return_button = driver.find_element(*web.Invoice.RETURN_ON_CREDIT)
        return_button.click()
        check_alert_text(driver, u'Заказ не выбран')


#### 2 cумма введена некорректно - выводится сообщение "Некорректная сумма счета"
def test_invalid_sum(data):
    print data
    my_driver = web.Driver()
    with my_driver as driver:
        web.Invoice.open(driver, data['invoice_id'])
        amount_field = driver.find_element(*web.Invoice.AMOUNT_IN_INVOICE_CURRENCY)
        amount_field.send_keys("qwerty")
        order = driver.find_element(*web.Invoice.ORDER)
        order.send_keys(u'{service_id}-{service_order_id}: "Рекламная кампания РРС", клиент: "{client_id}-ba..'.format(
            service_id=SERVICE_ID, service_order_id=data['service_order_ids'][0], client_id=data['client_id']))
        return_button = driver.find_element(*web.Invoice.RETURN_ON_CREDIT)
        return_button.click()
        check_alert_text(driver, u'Некорректная сумма счета.')


#### 3   Если сумма не указана - выводится сообщение "Перенести все средства с выбранного заказа?" и
#### все неоткрученные средства указанного заказа возвращаются на кредит (при подтверждении алерта)
#### ничего не происходит (при отказе)
@pytest.mark.parametrize('test_data', [{'muzzle_operation_amount': '-70,000000',
                                        't_consume': [{'current_qty': 30, 'current_sum': 900}],
                                        't_reverse': [{'reverse_qty': 70, 'reverse_sum': 2100}]}])
def test_return_all_funds_from_order(data, test_data):
    my_driver = web.Driver()
    with my_driver as driver:
        web.Invoice.open(driver, data['invoice_id'])
        order = driver.find_element(*web.Invoice.ORDER)
        order.send_keys(u'{service_id}-{service_order_id}: "Рекламная кампания РРС", клиент: "{client_id}-ba..'.format(
            service_id=SERVICE_ID, service_order_id=data['service_order_ids'][1], client_id=data['client_id']))
        order.click()
        return_button = driver.find_element(*web.Invoice.RETURN_ON_CREDIT)
        return_button.click()
        check_alert_text(driver, u'Перенести все средства с выбранного заказа?')
        order_operation_qty = WebDriverWait(driver, 60).until(ec.presence_of_element_located(
            web.Invoice.ORDER_OPERATION_QTY(service_order_id=data['service_order_ids'][1])))
        utils.check_that(order_operation_qty.text, equal_to(test_data['muzzle_operation_amount']),
                         u'Проверяем веб')

    fact_t_consume, fact_t_reverse = get_db_values(data['order_ids'][1])
    utils.check_that(test_data['t_consume'], equal_to(fact_t_consume), u'Проверяем текст алерта')
    utils.check_that(test_data['t_reverse'], equal_to(fact_t_reverse), u'Проверяем текст алерта')


#### 4 с заказа можно снять дробную сумму, но она введена с количеством десятичных знаков>2
#### - выводится сообщение "Допускается максимум 2 знака после точки"
def test_wrong_fractional_part(data):
    my_driver = web.Driver()
    with my_driver as driver:
        web.Invoice.open(driver, data['invoice_id'])
        amount_field = driver.find_element(*web.Invoice.AMOUNT_IN_INVOICE_CURRENCY)
        amount_field.send_keys("1,345")
        order = driver.find_element(*web.Invoice.ORDER)
        order.send_keys(u'{service_id}-{service_order_id}: "Рекламная кампания РРС", клиент: "{client_id}-ba..'.format(
            service_id=SERVICE_ID, service_order_id=data['service_order_ids'][2], client_id=data['client_id']))
        return_button = driver.find_element(*web.Invoice.RETURN_ON_CREDIT)
        return_button.click()
        check_alert_text(driver, u'Допускается максимум 2 знака после точки')


#### 5 Если сумма указана и она > суммы  неоткрученного  на  заказе, то  выводится  сообщение
#### "Максимальная сумма для возврата - XXX. Вернуть эту сумму?"
@pytest.mark.parametrize('test_data', [{'amount': 2100, 'muzzle_operation_amount': '-70,000000', 't_consume': [
    {'current_qty': 30, 'current_sum': 900}], 't_reverse': [{'reverse_qty': 70, 'reverse_sum': 2100}]}])
def test_max_amount(data, test_data):
    my_driver = web.Driver()
    with my_driver as driver:
        web.Invoice.open(driver, data['invoice_id'])
        amount_field = driver.find_element(*web.Invoice.AMOUNT_IN_INVOICE_CURRENCY)
        amount_field.send_keys("1000000")
        order = driver.find_element(*web.Invoice.ORDER)
        order.send_keys(u'{service_id}-{service_order_id}: "Рекламная кампания РРС", клиент: "{client_id}-ba..'.format(
            service_id=SERVICE_ID, service_order_id=data['service_order_ids'][3], client_id=data['client_id']))
        # order.click()
        return_button = driver.find_element(*web.Invoice.RETURN_ON_CREDIT)
        return_button.click()
        check_alert_text(driver, u'Максимальная сумма для возврата - {amount}. Вернуть эту сумму?'.format(
            amount=test_data['amount']))
        time.sleep(3)
        order_operation_qty = WebDriverWait(driver, 60).until(ec.presence_of_element_located(
            web.Invoice.ORDER_OPERATION_QTY(service_order_id=data['service_order_ids'][3])))
        utils.check_that(order_operation_qty.text, equal_to(test_data['muzzle_operation_amount']),
                         u'Проверяем веб')

    fact_t_consume, fact_t_reverse = get_db_values(data['order_ids'][3])
    utils.check_that(test_data['t_consume'], equal_to(fact_t_consume), u'Проверяем текст алерта')
    utils.check_that(test_data['t_reverse'], equal_to(fact_t_reverse), u'Проверяем текст алерта')


#### 6 Если сумма указана и
#### XXX - сумма неоткрученных средств заказа (при подтверждении указанная сумма снимается с заказа и возвращается на кредит )
#### на узаказанном заказе не осталось свободных средств, то выводится сообщение "На данном заказе нет свободных средств", ничего не происходит
@pytest.mark.parametrize('test_data', [{'t_consume': [{'current_qty': 100, 'current_sum': 3000}]}])
def test_no_free_funds(data, test_data):
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, data['service_order_ids'][4], {'Bucks': 100, 'Days': 0, 'Money': 0},
                                      0, campaigns_dt=DT)
    my_driver = web.Driver()
    with my_driver as driver:
        web.Invoice.open(driver, data['invoice_id'])
        amount_field = driver.find_element(*web.Invoice.AMOUNT_IN_INVOICE_CURRENCY)
        amount_field.send_keys("1")
        order = driver.find_element(*web.Invoice.ORDER)
        order.send_keys(u'{service_id}-{service_order_id}: "Рекламная кампания РРС", клиент: "{client_id}-ba..'.format(
            service_id=SERVICE_ID, service_order_id=data['service_order_ids'][4], client_id=data['client_id']))
        order.click()
        return_button = driver.find_element(*web.Invoice.RETURN_ON_CREDIT)
        return_button.click()
        check_alert_text(driver, u'На данном заказе нет свободных средств.')

    fact_t_consume, fact_t_reverse = get_db_values(data['order_ids'][4])
    utils.check_that(test_data['t_consume'], equal_to(fact_t_consume), u'Проверяем текст алерта')


#### 7 Частичный возврат
@pytest.mark.parametrize('test_data', [
    {'fill_amount_value': '12,55', 'muzzle_operation_amount': '-0,418333', 't_consume': [
        {'current_qty': '99.581667', 'current_sum': '2987.45'}], 't_reverse': [
        {'reverse_qty': '0.418333', 'reverse_sum': '12.55'}]}])
def test_return_part_funds_from_order(data, test_data):
    my_driver = web.Driver()
    with my_driver as driver:
        web.Invoice.open(driver, data['invoice_id'])
        amount_field = driver.find_element(*web.Invoice.AMOUNT_IN_INVOICE_CURRENCY)
        amount_field.send_keys(test_data['fill_amount_value'])
        order = driver.find_element(*web.Invoice.ORDER)
        order.send_keys(u'{service_id}-{service_order_id}: "Рекламная кампания РРС", клиент: "{client_id}-ba..'.format(
            service_id=SERVICE_ID, service_order_id=data['service_order_ids'][5], client_id=data['client_id']))
        order.click()
        return_button = driver.find_element(*web.Invoice.RETURN_ON_CREDIT)
        return_button.click()
        check_alert_text(driver, u'На данном заказе нет свободных средств.')
        order_operation_qty = WebDriverWait(driver, 60).until(ec.presence_of_element_located(
            web.Invoice.ORDER_OPERATION_QTY(service_order_id=data['service_order_ids'][5])))
        utils.check_that(order_operation_qty.text, equal_to(test_data['muzzle_operation_amount']),
                         u'Проверяем веб')

    fact_t_consume, fact_t_reverse = get_db_values(data['order_ids'][5])
    utils.check_that(test_data['t_consume'], equal_to(fact_t_consume), u'Проверяем текст алерта')
    utils.check_that(test_data['t_reverse'], equal_to(fact_t_reverse), u'Проверяем текст алерта')


if __name__ == "__main__":
    pytest.main('BALANCE-21219_2.py -v')

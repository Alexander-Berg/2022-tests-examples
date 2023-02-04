# coding: utf-8
__author__ = 'igogor'

from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

import temp.igogor.balance_steps_2_failed as steps
from temp.igogor.objects import *

# pytestmark = pytest.mark.xfail

PRODUCT_ID = 504601  # TODO через constants Services.Auto.default_product
PRODUCT_PRICE = Decimal(80)
LIMIT_QTY = Decimal(1500) / PRODUCT_PRICE  # todo корректное значение
AUTORU_SERVICE_ID = 99  # TODO через constants


# TODO choose correspond product

# Подготовка данных:
@pytest.fixture
def client_id():
    client = steps.ClientSteps.create(Client.default())
    return client.id  # todo передавать весь объект а не одно значение


@pytest.fixture
def contract(client_id):
    '''
    :param client_id:
    :return:
    '''

    raise NotImplementedError


@pytest.fixture
def service_order_id(client_id):
    order = steps.OrderSteps.create(Order.default(client_id=client_id,
                                                  service_id=AUTORU_SERVICE_ID,
                                                  product_id=PRODUCT_ID))
    return order.service_order_id  # todo возвращать весь объект


@pytest.fixture
def invoice_id(client_id, service_order_id):
    request = steps.RequestSteps.create(Request.default(client_id=client_id,
                                                        lines=[Line.custom(service_id=AUTORU_SERVICE_ID,
                                                                           service_order_id=service_order_id,
                                                                           qty=float(2 * LIMIT_QTY))]))

    person = steps.PersonSteps.create(Person.default(client_id=client_id, type_='ur_autoru'))
    invoice = steps.InvoiceSteps.create(Invoice.default(request_id=request.id, person_id=person.id,
                                                        paysys_id=1091))

    steps.InvoiceSteps.pay(invoice_id=invoice.id)
    return invoice.id  # todo возвращать весь объект


def _act(qty, date=datetime.datetime.now(), back_months=0):  # todo нельзя заполнять дефолтные значения не константами
    return {'qty': qty, 'dt': date - relativedelta(months=back_months)}


# TODO: test_ prefix was removed to hide unfinished test from pytest
@pytest.mark.parametrize('acts_info_list, expected_limit', [
    ([_act(qty=LIMIT_QTY / 2, back_months=1), _act(qty=LIMIT_QTY / 2, back_months=1)], 300)
])
def budget_acts_conditions(client_id, service_order_id, invoice_id, acts_info_list, expected_limit):
    for act_info in acts_info_list:
        steps.OrderSteps.add_campaigns(Campaign.default(service_id=AUTORU_SERVICE_ID, service_order_id=service_order_id,
                                                        unit='Days', qty=act_info['qty'], campaign_dt=act_info['dt']))

        steps.ActsSteps.create(invoice_id, act_dt=act_info['dt'])
    steps.OverdraftSteps.calculate_overdraft(client_id)
    actual_limit = steps.OverdraftSteps.get_limit(client_id, AUTORU_SERVICE_ID)[0]
    assert actual_limit == expected_limit


def yollo_test():
    steps.ActSteps.create_act()


'''
1) Действия
    Создать бюджет овердрафта
        Создать акт
            Создать счет или передать счет/счета
                Создать реквест или передать ревест
                    Создать заказ или передать заказ/заказы
                        Создать клиента или передать клиента
            Оплатить счет
            Сделать открутки

    Получить лимит овердрафта
    Сравнить полученный лимит
2) Параметры
    Сумма акта
    Дата акта
    Количество актов

'''
'''
Проблема - надо просовывать везде значения, которые везде лезут
'''

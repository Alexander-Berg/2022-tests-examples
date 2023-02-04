# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import copy
import pytest
from pytest import param

import balance.balance_api as api
from btestlib import reporter
from btestlib import utils
from btestlib.constants import Services, Products, Currencies
from btestlib.matchers import contains_dicts_equal_to
from simpleapi.matchers.deep_equals import deep_equals_to

# https://st.yandex-team.ru/BALANCE-25950
NEW_TAX_POLICY_PERIOD = datetime(2019, 1, 1, 0, 0) # новый период налоговых политик c 01-01-2019 в связи с изменением ндс
NO_NEW_TAX_POLICY_PERIOD_PRODUCTS = []


SEPTEMBER_CONNECT_PRODUCTS = [
    {'Name': u'Услуги "Яндекс.Коннект"',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 8, 11, 0, 0), 'Price': '1'}],
     'ProductID': 508529,
     'Rate': '1',
     'Unit': u'рубли'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Расширенный 2 пользователя',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 9, 1, 0, 0), 'Price': '285'}],
     'ProductID': 508547,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Для своих',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 8, 9, 0, 0), 'Price': '0'}],
     'ProductID': 508506,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер Для своих',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 8, 9, 0, 0), 'Price': '0'}],
     'ProductID': 508508,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 11–100',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 8, 9, 0, 0), 'Price': '209'}],
     'ProductID': 508510,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 101–500',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 8, 9, 0, 0), 'Price': '163'}],
     'ProductID': 508511,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 501–2000',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 8, 9, 0, 0), 'Price': '81'}],
     'ProductID': 508512,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Премиум',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 8, 9, 0, 0), 'Price': '190'}],
     'ProductID': 508507,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Расширенный 1 пользователь',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 9, 1, 0, 0), 'Price': '570'}],
     'ProductID': 508546,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 1–10',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 8, 9, 0, 0), 'Price': '93'}],
     'ProductID': 508509,
     'Rate': '1',
     'Unit': u'чел/мес'}
]

FULL_CONNECT_PRODUCTS = SEPTEMBER_CONNECT_PRODUCTS + [
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 11–100 (скидка 20%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 12, 11, 0, 0), 'Price': '167'}],
     'ProductID': 508770,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Расширенный 2 пользователя (Скидка 50%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 11, 2, 0, 0), 'Price': '142'}],
     'ProductID': 508659,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Премиум (Скидка 50%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 11, 2, 0, 0), 'Price': '95'}],
     'ProductID': 508660,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 1–10 (скидка 20%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 12, 11, 0, 0), 'Price': '74'}],
     'ProductID': 508769,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 101–500 ( скидка 20 %)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 12, 11, 0, 0), 'Price': '130'}],
     'ProductID': 508771,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Расширенный 1 пользователь (скидка 20%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 12, 11, 0, 0), 'Price': '456'}],
     'ProductID': 508766,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Расширенный 2 пользователя (скидка 20%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 12, 11, 0, 0), 'Price': '228'}],
     'ProductID': 508767,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Премиум (скидка 20%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 12, 11, 0, 0), 'Price': '152'}],
     'ProductID': 508768,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 501–2000 (скидка 20%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 12, 11, 0, 0), 'Price': '64'}],
     'ProductID': 508772,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 1–10 (Скидка 50%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 11, 2, 0, 0), 'Price': '46'}],
     'ProductID': 508661,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 11–100 (Скидка 50%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 11, 2, 0, 0), 'Price': '104'}],
     'ProductID': 508662,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 101–500 (Скидка 50%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 11, 2, 0, 0), 'Price': '81'}],
     'ProductID': 508663,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Трекер 501–2000 (Скидка 50%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 11, 2, 0, 0), 'Price': '40'}],
     'ProductID': 508664,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект - Базовый Коннект Расширенный 1 пользователь (Скидка 50%)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2017, 11, 2, 0, 0), 'Price': '285'}],
     'ProductID': 508658,
     'Rate': '1',
     'Unit': u'чел/мес'},
    {'Name': u'Услуги "Яндекс.Коннект" (активация спец. тарифа)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2018, 7, 1, 0, 0), 'Price': '1000'}],
     'ProductID': 509340,
     'Rate': '1',
     'Unit': u'шт'},
    {'Name': u'Дополнительное место на Диске для сотрудников организации (платные подписки)',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2019, 10, 1, 0, 0), 'Price': '500'}],
     'ProductID': 510830,
     'Rate': '1',
     'Unit': u'шт'},
    {'Name': u'Покупка дополнительного места в Яндекс.Диск',
     'Prices': [{'Currency': 'RUB', 'DT': datetime(2019, 10, 1, 0, 0), 'Price': '1'}],
     'ProductID': 510827,
     'Rate': '1',
     'Unit': u'рубли'},
]

SINGLE_PRODUCT_CONNECT = [product for product in FULL_CONNECT_PRODUCTS if product['ProductID'] == Products.CONNECT.id]


@pytest.mark.parametrize("dt, currency, product, expected", [
    param(None, None, None, FULL_CONNECT_PRODUCTS, id='WITHOUT_PARAMS'),
    param(datetime(2017, 6, 30), None, None, [], id='DT_EMPTY'),
    param(datetime(2017, 9, 15), None, None, SEPTEMBER_CONNECT_PRODUCTS, id='DT_FULL'),
    param(None, Currencies.USD, None, [], id='CURRENCY_EMPTY'),
    param(None, Currencies.RUB, None, FULL_CONNECT_PRODUCTS, id='CURRENCY_FULL'),
    param(None, None, Products.CONNECT, SINGLE_PRODUCT_CONNECT, id='SINGLE_PRODUCT'),
    param(datetime(2017, 9, 15), Currencies.RUB, Products.CONNECT, SINGLE_PRODUCT_CONNECT, id='ALL_PARAMS'),
])
def test_get_products(dt, currency, product, expected):
    products = get_products(Services.CONNECT, product=product, dt=dt, currency=currency)
    if not dt: # если не указана дата, нужны цены за все периоды, добавляем новые периоды цен
        expected = copy.deepcopy(expected)
        add_new_price_period(expected)
    utils.check_that(products, contains_dicts_equal_to(expected, same_length=False), u'Проверяем ответ метода')


def test_get_products_currencies():
    products = get_products(Services.GEO, product=Products.GEO, dt=datetime(2016, 1, 1))
    expected = [{'Name': u'Приоритетное размещение в Яндекс.Справочнике, Казахстан',
                 'Prices': [{'Currency': None, 'DT': datetime(2011, 3, 1, 0, 0), 'Price': '6500'}, #https://st.yandex-team.ru/BALANCE-26296
                            {'Currency': 'RUB', 'DT': datetime(2011, 10, 1, 0, 0), 'Price': '1300'}],
                 'ProductID': 502952,
                 'Rate': '30',
                 'Unit': u'месяц'}]

    utils.check_that(products, deep_equals_to(expected), u'Проверяем ответ метода')


# -----------------------------------
# Utils

def get_products(service, dt=None, currency=None, product=None):
    with reporter.step(u'Вызываем GetProducts для сервиса: {}'.format(service.name, )):
        return api.medium().GetProducts(service.token, utils.remove_empty({
            'DT': dt,
            'Currency': currency.iso_code if currency else None,
            'ProductID': product.id if product else None
        }))


def add_new_price_period(product_list):
    for product in product_list:
        if product['Prices']:
            assert len(product['Prices']) == 1, product
            first_price = product['Prices'][0]
            if first_price['DT'] < NEW_TAX_POLICY_PERIOD:
                second_period_price = copy.deepcopy(first_price)
                second_period_price['DT'] = NEW_TAX_POLICY_PERIOD
                product['Prices'].append(second_period_price)

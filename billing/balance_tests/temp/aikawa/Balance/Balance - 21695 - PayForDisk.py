# -*- coding: utf-8 -*-

import random

import requests as req

import balance.balance_steps as steps
from balance import balance_api as api
from btestlib import secrets

DISK_SERVICE_TOKEN = 'yandex_disk_99bc52562c956690cbec7f0abdac9865'
MUSIC_SERVICE_TOKEN = 'music_039128f74eaa55f94617c329b4c06e65'


def create_order(service_token):
    SERVICE_PRODUCT = '73571' + ('%05d' % random.randint(10, 999999999))
    SERVICE_ORDER_ID = str(random.randint(266500000, 266900000))

    r = req.post('http://oauth-test.yandex.ru/token', data={'grant_type': 'password', 'username': 'trusttestusr5',
                                                            'password': secrets.get_secret(
                                                                *secrets.UsersPwd.TRUSTTESTUSR_PWD),
                                                            'client_id': '339e9a9d5bf346629394d7c8b4625c06', 'client_secret': 'b9f4f6b80b674be8bdc0538ca0eb7b76'})

    access_token = r.json()['access_token']

    api.simpleapi().server.BalanceSimple.CreateServiceProduct(service_token, {
        'prices':
            [
                {'currency': 'RUB', 'price': '11', 'dt': 1347521693, 'region_id': 225}
                , {'currency': 'USD', 'price': '1', 'dt': 1327521693, 'region_id': 84}
            ],
        'service_product_id': SERVICE_PRODUCT, 'product_type': 'app', 'name': 'Super Product'
    })

    order_info = api.simpleapi().server.BalanceSimple.CreateOrder(service_token, {'service_product_id': SERVICE_PRODUCT, 'region_id': 225, 'uid': '3000338163', 'service_order_id': SERVICE_ORDER_ID})
    trust_payment_id = order_info['trust_payment_id']

    response = api.simpleapi().server.BalanceSimple.PayOrder(service_token, {'back_url': 'https://balance.greed-tm1f.yandex.ru/', 'user_ip': '95.108.174.132', 'currency': 'RUB', 'token': access_token,
                                                                             'service_order_id': SERVICE_ORDER_ID, 'paymethod_id': 'trust_web_page'})

    purchase_token = response['payment_form']['purchase_token']

    req.post('https://tmongo1f.yandex.ru/web/start_payment',
             data={
                 'card_number': '5555555555554444',
                 'payment_method': 'new_card',
                 'purchase_token': purchase_token,
                 'expiration_month': '04',
                 'expiration_year': '2017',
                 'cardholder': 'TEST',
                 'cvn': '874'
             })

    check_order_response = api.simpleapi().server.BalanceSimple.CheckOrder(service_token, {'user_ip': '95.108.174.132', 'service_order_id': SERVICE_ORDER_ID, 'uid': '3000338163'})
    service_order_id = check_order_response['service_order_id']

    query = "SELECT RESP_CODE as val FROM V_PAYMENT_TRUST WHERE TRUST_PAYMENT_ID = :trust_payment_id"
    sql_params = {'trust_payment_id': trust_payment_id}
    steps.CommonSteps.wait_for(query, sql_params, value='success')

    return service_order_id


print create_order(MUSIC_SERVICE_TOKEN)

print create_order(DISK_SERVICE_TOKEN)

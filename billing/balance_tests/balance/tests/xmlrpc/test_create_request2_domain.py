# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import pytest
from hamcrest import ends_with

import balance.balance_api as api
import balance.balance_steps as steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.features import Features

UID = 16571028

SERVICE_ID = 132
PRODUCT_ID = 507175

OLD_MARKET_FIRM_ID = 1
NEW_MARKET_FIRM_ID = 111

QTY = 118

NOW = datetime.now()

TOP_LEVEL_DOMAINS = ['ru', 'ua', 'kz', 'by', 'com', 'com.tr', 'tr']


# Получение домена для ссылок вида:
# 'https://passport.yandex.ru/passport' -> 'passport.yandex.ru'
def get_domain(url):
    return url.split('/')[2]


@reporter.feature(Features.REQUEST, Features.XMLRPC, Features.TO_UNIT)
@pytest.mark.tickets('BALANCE-22566')
@pytest.mark.parametrize("top_level_domain", TOP_LEVEL_DOMAINS)
def test_create_request2_domain(top_level_domain):
    client_id = steps.ClientSteps.create()
    order_owner = client_id
    invoice_owner = client_id

    steps.ClientSteps.link(invoice_owner, 'clientuid32')

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID)

    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
    response = api.medium().CreateRequest2(UID, invoice_owner, orders_list, {'Region': top_level_domain})

    utils.check_that(get_domain(response['UserPath']), ends_with(top_level_domain),
                     'Проверяем, что пользовательский путь в правильной доменной зоне')
    utils.check_that(get_domain(response['AdminPath']), ends_with(top_level_domain),
                     'Проверяем, что админский путь в правильной доменной зоне')

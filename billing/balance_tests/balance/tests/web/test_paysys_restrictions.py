# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import datetime

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
QTY = Decimal('50')

@pytest.mark.parametrize('reliable_client, qty, paysys', [
    (False, Decimal('8333333.36'), 'bank'),
    (True, Decimal('8333.36'), 'bank'),
    (False, Decimal('3333.36'), 'yamoney')
], ids=['not reliable client, bank card',
        'reliable clent, bank card',
        'not reliable client, yamoney'])
def test_paystep_restrictions_maxlimit(reliable_client, qty, paysys, get_free_user):
    client_id = steps.ClientSteps.create()
    if reliable_client:
        with reporter.step(u'Делаем плательщика надежным'):
            db.balance().execute("UPDATE T_CLIENT SET RELIABLE_CC_PAYER = 1 "
                                 "WHERE ID= :client_id", {'client_id': client_id})
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)
    service_order_id = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id, Products.DIRECT_FISH.id, Services.DIRECT.id)
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id, 'Qty': qty,
         'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)

    with web.Driver() as driver:
        paypreview_page = web.ClientInterface.PaychoosePage.open(driver, request_id=request_id)
        paypreview_page.turn_off_experiment()
        paypreview_page.choose_ph()
        if paysys == 'bank':
            utils.check_that(paypreview_page.get_bank_card_ph_reason(),
                             equal_to(u'Способ оплаты недоступен из-за превышения максимальной суммы счета.'),
                             u'Проверяем, что на странице нет кнопки "Выставить счет" (под админом)')
        else:
            utils.check_that(paypreview_page.get_yamoney_ph_reason(),
                             equal_to(u'Способ оплаты недоступен из-за превышения максимальной суммы счета.'),
                             u'Проверяем, что на странице нет кнопки "Выставить счет" (под админом)')


def test_paystep_ban_credit_card(get_free_user):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.deny_cc(client_id)
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)
    service_order_id = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id, Products.DIRECT_FISH.id, Services.DIRECT.id)
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)

    with web.Driver(user=user) as driver:
        paypreview_page = web.ClientInterface.PaychoosePage.open(driver, request_id=request_id)
        paypreview_page.turn_off_experiment()
        paypreview_page.choose_ph()
        bank_card_ph = paypreview_page.is_bank_card_ph_available()
        paypreview_page.choose_ur()
        bank_card_ur = paypreview_page.is_bank_card_ur_available()
        utils.check_that(bank_card_ph, equal_to(False),
                         u'Проверяем, что оплата банковской картой (ФЛ) недоступна')
        utils.check_that(bank_card_ur, equal_to(False),
                         u'Проверяем, что оплата банковской картой (ЮЛ) недоступна')




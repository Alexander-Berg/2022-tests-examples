# -*- coding: utf-8 -*-

__author__ = 'torvald'

import datetime
from collections import namedtuple
from decimal import Decimal as D

import pytest
from hamcrest import equal_to, is_in

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import matchers as mtch
from btestlib import utils as utils
from btestlib.data import defaults
from btestlib.constants import NdsNew as nds, Products, Services

Point = namedtuple('Point', 'qty price')

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

ADFOX_SERVICE_ID = Services.ADFOX.id
ADFOX_PRODUCT = Product(ADFOX_SERVICE_ID, 507853, 'Units')  # Специальный продукт для оферты AdFox (media_discount = 35)
BANK_PH = 1001
BANK_UR = 1003
BANK_YT_RUB_WITH_NDS = 11069

NDS = nds.YANDEX_RESIDENT

COMPLETION_PART = D('0.8')

def create_contract(person_type):
    # создаем клиента
    client_id = steps.ClientSteps.create()
    # создаем плательщика
    person_id = steps.PersonSteps.create(client_id, person_type, {})
    # создаем договор на ADFox
    if person_type == 'ur':
        contract_id, _ = steps.ContractSteps.create_contract('adfox_all_products',
                                                             {'CLIENT_ID': client_id,
                                                              'PERSON_ID': person_id,
                                                              'DT': HALF_YEAR_BEFORE_NOW_ISO,})
    else:
        contract_id, _ = steps.ContractSteps.create_contract('adfox_all_products',
                                                             {'CLIENT_ID': client_id,
                                                              'PERSON_ID': person_id,
                                                              'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                              'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO})
    return client_id, person_id, contract_id


def create_and_check_invoice_with_act(client_id, agency_id, person_id, paysys_id, contract_id, point):
    # Создаём заказ
    service_order_id = steps.OrderSteps.next_id(ADFOX_SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=ADFOX_SERVICE_ID,
                                       product_id=ADFOX_PRODUCT.id,
                                       params={'AgencyID': agency_id})

    # Готовим список заказов для выставления счёта
    orders_list = [
        {'ServiceID': ADFOX_SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': point.qty, 'BeginDT': NOW}] \
 \
        # Создаём риквест и счёт
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': NOW})
    invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, paysys_id, overdraft=0,
                                                                   credit=0, contract_id=contract_id)

    # Оплачиваем счёт
    steps.InvoiceSteps.pay(invoice_id)

    # Отправляем открутки
    steps.CampaignsSteps.do_campaigns(ADFOX_SERVICE_ID, service_order_id,
                                      {ADFOX_PRODUCT.shipment_type: point.qty * COMPLETION_PART}, 0, NOW)

    # Проверяем скидку в заявке
    # consume = db.get_consumes_by_invoice(invoice_id)[0]
    # completion_sum = consume['completion_sum']
    # utils.check_that(D(consume['static_discount_pct']), equal_to(scale.discount))

    # Генерируем акты
    steps.ActsSteps.generate(client_id, force=1, date=NOW)
    acts = db.get_acts_by_client(client_id)

    expected_amount = [{'amount': point.price * COMPLETION_PART,
                 #'amount_nds': point.price * COMPLETION_PART / (NDS.koef_on_dt(NOW) * D('100')) * NDS.pct_on_dt(NOW)
                        }]
    utils.check_that(acts, mtch.contains_dicts_with_entries(expected_amount))

    expected_value = utils.dround2(point.price * COMPLETION_PART / (NDS.koef_on_dt(NOW) * D('100')) * NDS.pct_on_dt(NOW))
    expected_nds = [expected_value - D('0.01'), expected_value, expected_value + D('0.01')]
    utils.check_that(D(acts[0]['amount_nds']), is_in(expected_nds))


@pytest.mark.priority('mid')
@reporter.feature(Features.DISCOUNT)
@pytest.mark.tickets('BALANCE-23000')
@pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/discount')
@pytest.mark.docs(u'--group', u'Автотесты для AdFox: скидка на количество заказов')
@pytest.mark.parametrize('point', [# Point(D('1000000'), D('1298')),
                                   # Point(D('1500000'), D('1534')),

                                   # Optimization: comment some tests
                                   Point(D('2000000'), D('1770')),
                                   # Point(D('3000000'), D('2832')),
                                   # Point(D('4500000'), D('3658')),
                                   # Point(D('6000000'), D('4130')),
                                   # Point(D('7500000'), D('5074')),
                                   # Point(D('9000000'), D('5900')),
                                   # Point(D('12000000'), D('7080')),
                                   # Point(D('15000000'), D('8260')),
                                   # Point(D('19500000'), D('9794')),
                                   # Point(D('24000000'), D('11800')),
                                   ]
                         )
def test_2016_AdFox_discount_for_offer_RESIDENT(point):
    # Создаём агентство, клиента и плательщика
    agency_id = None
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, 'ph', {})
    contract_id = None

    create_and_check_invoice_with_act(client_id, agency_id, person_id, BANK_PH, contract_id, point)


@pytest.mark.priority('mid')
@reporter.feature(Features.DISCOUNT)
@pytest.mark.tickets('BALANCE-23000')
@pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/discount')
@pytest.mark.docs(u'--group', u'Автотесты для AdFox: скидка на количество заказов для СНГ')
@pytest.mark.parametrize('scale', [# Point(D('1000000'), D('908.6')),
                                   # Point(D('1500000'), D('1073.8')),
                                   Point(D('2000000'), D('1239')),
                                   # Point(D('3000000'), D('1982.4')),
                                   # Point(D('4500000'), D('2560.6')),
                                   # Point(D('6000000'), D('2891')),
                                   # Point(D('7500000'), D('3551.8')),
                                   # Point(D('9000000'), D('4130')),
                                   # Point(D('12000000'), D('4956')),
                                   # Point(D('15000000'), D('5782')),
                                   # Point(D('19500000'), D('6855.8')),
                                   # Point(D('24000000'), D('8260')),
                                   ]
                         )
def test_2016_AdFox_discount_for_offer_NON_RESIDENT_by_person_type(scale):
    # Создаём агентство, клиента и плательщика
    agency_id = None
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, 'yt', {})
    contract_id = None

    create_and_check_invoice_with_act(client_id, agency_id, person_id, BANK_YT_RUB_WITH_NDS, contract_id, scale)


@pytest.mark.priority('mid')
@reporter.feature(Features.DISCOUNT)
@pytest.mark.tickets('BALANCE-23000')
@pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/discount')
@pytest.mark.docs(u'--group', u'Автотесты для AdFox: скидка на количество заказов для СНГ')
@pytest.mark.parametrize('scale', [Point(D('1000000'), D('908.6'))]
                         )
def test_2016_AdFox_discount_for_offer_NON_RESIDENT_by_person_type_and_region(scale):
    # Создаём агентство, клиента и плательщика
    agency_id = None
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'REGION_ID': 149})
    person_id = steps.PersonSteps.create(client_id, 'yt', {})
    contract_id = None

    create_and_check_invoice_with_act(client_id, agency_id, person_id, BANK_YT_RUB_WITH_NDS, contract_id, scale)


# Тест только для того, чтобы убедиться, что клиент с договором не сможет разместиться воо
@pytest.mark.priority('mid')
@reporter.feature(Features.INVOICE, Features.CONTRACT)
@pytest.mark.tickets('BALANCE-22991')
@pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/discount')
@pytest.mark.docs(u'--group', u'Автотесты для AdFox: запрет на выставления счетов по оферте при наличии договора')
@pytest.mark.parametrize('scale', [Point(D('1000000'), D('1298'))])
def test_2016_AdFox_restrict_client(scale):
    # Создаём агентство, клиента и плательщика и договор
    agency_id = None
    client_id, person_id, contract_id = create_contract('ur')
    other_person_id = steps.PersonSteps.create(client_id, 'ur', {})

    contract_id = None

    try:
        create_and_check_invoice_with_act(client_id, agency_id, person_id, BANK_UR, contract_id, scale)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'INCOMPATIBLE_INVOICE_PARAMS'))


if __name__ == "__main__":
    pytest.main("-v -k 'test_2016_AdFox_discount_for_offer_RESIDENT' -n4")

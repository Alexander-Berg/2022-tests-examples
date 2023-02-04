# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import utils as utils

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
# NOW = datetime.datetime(2016,8,23)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

AUTORU = 99
AUTORU_PRODUCT = Product(AUTORU, 504601, 'Days')

PAYSYS_ID = 1201003
# PAYSYS_ID = 1091
QTY = 100

SPB_AND_REGION_CODE = 10174
MSK_AND_REGION_CODE = 1


# Мультивалютность

@pytest.mark.priority('mid')
@reporter.feature(Features.DISCOUNT)
@pytest.mark.tickets('BALANCE-23438')
@pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/discount')
@pytest.mark.docs(u'--group', u'Автотесты: Авто.Ру')
@pytest.mark.parametrize('scenario', [{'region_id': SPB_AND_REGION_CODE,
                                       'expected_discount_pct': 0},
                                      {'region_id': MSK_AND_REGION_CODE,
                                       'expected_discount_pct': 0},
                                      ])
def test_2016_RU_AUTORU_region_discount_discard_for_SPB(scenario):
    # agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    agency_id = None
    invoice_owner = client_id
    person_id = steps.PersonSteps.create(invoice_owner, 'ur')
    contract_id = None

    product = AUTORU_PRODUCT
    credit = 0

    # Выставляем и оплачиваем целевой счёт
    campaigns_list = [
        {'service_id': product.service_id, 'product_id': product.id, 'qty': QTY, 'begin_dt': NOW}]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=PAYSYS_ID,
                                                                  invoice_dt=NOW,
                                                                  agency_id=agency_id,
                                                                  credit=credit,
                                                                  contract_id=contract_id,
                                                                  overdraft=0,
                                                                  )
    if not credit:
        steps.InvoiceSteps.pay(invoice_id)

    # Проверка скидки в целевом счёте
    consume = db.get_consumes_by_invoice(invoice_id)[0]
    utils.check_that(consume['static_discount_pct'],
                     equal_to(scenario['expected_discount_pct']),
                     step=u'Проверяем сумму и скидку в заявке')


@reporter.feature(Features.DISCOUNT)
@pytest.mark.parametrize('scenario', [{'contract_type': 'auto_ru_post',
                                       'region_id': SPB_AND_REGION_CODE,
                                       'expected_discount_pct': 0},
                                      # {'region_id': MSK_AND_REGION_CODE,
                                      #  'expected_discount_pct': 0},
                                      ])
def test_2016_RU_AUTORU_region_discount_discard_for_SPB_subclient(scenario):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    # agency_id = None
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id or client_id
    order_owner = client_id
    person_id = steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract(scenario['contract_type'],
                                                         {
                                                             'CLIENT_ID': invoice_owner,
                                                             'PERSON_ID': person_id,
                                                             'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                             'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                             'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                             'FIRM': 10
                                                         })

    product = AUTORU_PRODUCT
    credit = 0

    # Выставляем и оплачиваем целевой счёт
    campaigns_list = [
        {'service_id': product.service_id, 'product_id': product.id, 'qty': QTY, 'begin_dt': NOW}]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=PAYSYS_ID,
                                                                  invoice_dt=NOW,
                                                                  agency_id=agency_id,
                                                                  credit=credit,
                                                                  contract_id=contract_id,
                                                                  overdraft=0,
                                                                  )
    if not credit:
        steps.InvoiceSteps.pay(invoice_id)

    # Проверка скидки в целевом счёте
    consume = db.get_consumes_by_invoice(invoice_id)[0]
    utils.check_that(consume['static_discount_pct'],
                     equal_to(scenario['expected_discount_pct']),
                     step=u'Проверяем сумму и скидку в заявке')

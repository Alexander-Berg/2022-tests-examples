# -*- coding: utf-8 -*-

import datetime
import json
import time
from decimal import Decimal as D

import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Services, Products, ProductTypes, Product

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
BASE_DT = datetime.datetime.now()

QTY = 25

DIRECT_FISH = Product(id=1475, type=ProductTypes.BUCKS, multicurrency_type=ProductTypes.MONEY)
DIRECT_RUB = Product(id=503162, type=ProductTypes.MONEY, multicurrency_type=None)

MARKET = Product(id=2136, type=ProductTypes.BUCKS, multicurrency_type=None)

GEO = Product(id=502986, type=ProductTypes.DAYS, multicurrency_type=None)

MEDIA = Product(id=507249, type=ProductTypes.SHOWS, multicurrency_type=None)

PAYSYS_ID = 1003

USD = 840
EUR = 978
CHF = 756

manager_uid = '244916211'


def create_invoice_with_act(client_id, agency_id, person_id, paysys_id, contract_id, service_id, product, qty, dt):
    # Создаём заказ
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product.id,
                                       params={'AgencyID': agency_id})

    # Готовим список заказов для выставления счёта
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}] \
 \
        # Создаём риквест и счёт
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': dt})
    invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, paysys_id, overdraft=0,
                                                                   credit=1, contract_id=contract_id)

    # Отправляем открутки
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id,
                                      {product.type.code: qty}, 0, dt)

    # Генерируем акты
    steps.ActsSteps.generate(agency_id, force=1, date=dt)

    for invoice in db.get_y_invoices_by_fpa_invoice(invoice_id):
        steps.InvoiceSteps.pay(invoice['id'])


@pytest.mark.parametrize('scenario', [
    # Different ActivityType roots (23 <> 17) 150 \ 50
    # {
    #     'client_fish_invoices': [(Services.MEDIA.id, MEDIA, D('300'), utils.add_months_to_date(NOW, -4))],
    #     'client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2))],
    #
    #     'indi_client_fish_invoices': [(Services.MEDIA.id, MEDIA, D('100'), utils.add_months_to_date(NOW, -4))],
    #     'indi_client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2))],
    #
    # },
    # Same ActivityType root (23) 150 \ 50
    # {
    #     'client_fish_invoices': [(Services.GEO.id, GEO, D('300'), utils.add_months_to_date(NOW, -4))],
    #     'client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2))],
    #
    #     'indi_client_fish_invoices': [(Services.GEO.id, GEO, D('100'), utils.add_months_to_date(NOW, -4))],
    #     'indi_client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2))],
    #
    # },
    # Multicurrency case 150 \ 50
    # {
    #     'client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('300'), utils.add_months_to_date(NOW, -4))],
    #     'client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2))],
    #
    #     'indi_client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('100'), utils.add_months_to_date(NOW, -4))],
    #     'indi_client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2))],
    #
    # }
    # Multicurrency case with multiple acts 4800 \ 1600
    # {
    #     'client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('300'), utils.add_months_to_date(NOW, -4)),
    #                              (Services.DIRECT.id, DIRECT_FISH, D('300'), utils.add_months_to_date(NOW, -2))],
    #     'client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2)),
    #                               (Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -1))],
    #
    #     'indi_client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('100'), utils.add_months_to_date(NOW, -4)),
    #                                   (Services.DIRECT.id, DIRECT_FISH, D('100'), utils.add_months_to_date(NOW, -2))],
    #     'indi_client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2)),
    #                                    (Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -1))],
    #
    # },
    #
    {
        'client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('300'), utils.add_months_to_date(NOW, -4))],
        'client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2)),
                                  (Services.GEO.id, GEO, D('300'), utils.add_months_to_date(NOW, -1))],

        'indi_client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('100'), utils.add_months_to_date(NOW, -4))],
        'indi_client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2)),
                                       (Services.GEO.id, GEO, D('100'), utils.add_months_to_date(NOW, -1))],

    }
])
def test_1(scenario):
    client_id = steps.ClientSteps.create()
    indi_client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})

    invoice_owner = agency_id
    # steps.ClientSteps.link(invoice_owner, 'clientuid32')

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('opt_agency_post',
                                                         {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                          'DT': '2015-04-30T00:00:00',
                                                          'FINISH_DT': '2016-12-30T00:00:00',
                                                          'IS_SIGNED': '2015-01-01T00:00:00',
                                                          'CREDIT_TYPE': 1,
                                                          # 'is_signed': None,
                                                          'SERVICES': [Services.DIRECT.id, Services.GEO.id,
                                                                       Services.MEDIA_70.id],
                                                          # 'COMMISSION_TYPE': 48,
                                                          # 'NON_RESIDENT_CLIENTS': 0,
                                                          # # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                          'REPAYMENT_ON_CONSUME': 1,
                                                          'PERSONAL_ACCOUNT': 1,
                                                          'LIFT_CREDIT_ON_PAYMENT': 1,
                                                          'PERSONAL_ACCOUNT_FICTIVE': 1
                                                          })

    individual_limits = json.dumps([{u'client_credit_type': u'1', u'id': u'1', u'client_limit_currency': u'',
                                     u'num': indi_client_id, u'client': indi_client_id,
                                     u'client_payment_term': u'45', u'client_limit': u'50000'}])
    steps.ContractSteps.create_collateral(1035, {'CONTRACT2_ID': contract_id,
                                                 'DT': '2015-04-30T00:00:00',
                                                 'IS_SIGNED': '2015-01-01T00:00:00',
                                                 'CLIENT_LIMITS': individual_limits})

    for client, key in [(client_id, 'client_fish_invoices'), (indi_client_id, 'indi_client_fish_invoices')]:
        for service_id, product, qty, dt in scenario[key]:
            create_invoice_with_act(client, agency_id, person_id, PAYSYS_ID,
                                    contract_id, service_id, product, qty, dt)

    steps.ClientSteps.create(
        {'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'SERVICE_ID': Services.DIRECT.id,
         'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
         'CURRENCY_CONVERT_TYPE': 'MODIFY'})

    steps.ClientSteps.create(
        {'CLIENT_ID': indi_client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'SERVICE_ID': Services.DIRECT.id,
         'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
         'CURRENCY_CONVERT_TYPE': 'MODIFY'})

    time.sleep(5)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', indi_client_id)

    for client, key in [(client_id, 'client_money_invoices'), (indi_client_id, 'indi_client_money_invoices')]:
        for service_id, product, qty, dt in scenario[key]:
            create_invoice_with_act(client, agency_id, person_id, PAYSYS_ID,
                                    contract_id, service_id, product, qty, dt)

    steps.CloseMonth.UpdateLimits(utils.Date.get_last_day_of_previous_month(), 0, [agency_id, indi_client_id])


if __name__ == '__main__':
    pass

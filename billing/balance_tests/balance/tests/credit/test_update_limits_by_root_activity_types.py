# -*- coding: utf-8 -*-

import datetime
import json
import time
from decimal import Decimal as D
from datetime import timedelta
from dateutil.relativedelta import relativedelta

import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import AuditFeatures
from btestlib import utils, reporter
from btestlib.constants import Services, Products, Firms, ContractCommissionType, Collateral
from temp.igogor.balance_objects import Contexts


DIRECT_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
MARKET_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                        firm=Firms.MARKET_111)

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
BASE_DT = datetime.datetime.now()

QTY = 25

DIRECT_FISH = Products.DIRECT_FISH
DIRECT_RUB = Products.DIRECT_RUB

MARKET = Products.MARKET

GEO = Products.GEO_3

MEDIA = Products.MEDIA_4

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
    {
        'description': 'Different ActivityType roots (23 17)',
        'client_fish_invoices': [(Services.MEDIA_70.id, MEDIA, D('300'), utils.add_months_to_date(NOW, -4))],
        'client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2))],

        'indi_client_fish_invoices': [(Services.MEDIA_70.id, MEDIA, D('100'), utils.add_months_to_date(NOW, -4))],
        'indi_client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2))],
        'credit_limit_single': 10000004,
        'credit_limit': 50000

    },
    # Same ActivityType root (23) 150 \ 50
    {
            'description': 'Same ActivityType root (23)',
            'client_fish_invoices': [(Services.GEO.id, GEO, D('300'), utils.add_months_to_date(NOW, -4))],
            'client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2))],

            'indi_client_fish_invoices': [(Services.GEO.id, GEO, D('100'), utils.add_months_to_date(NOW, -4))],
            'indi_client_money_invoices': [
                (Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2))],
            'credit_limit_single': 150,
            'credit_limit': 50
        },
    # Multicurrency case 150 \ 50
    {
        'description': 'Direct multicurrency case',
        'client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('300'), utils.add_months_to_date(NOW, -4))],
        'client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2))],

        'indi_client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('100'), utils.add_months_to_date(NOW, -4))],
        'indi_client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2))],
        'credit_limit_single': 150,
        'credit_limit': 50

    },
    # Multicurrency case with multiple acts 4800 \ 1600
    {
        'description': 'Direct multicurrency case with several acts',
        'client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('300'), utils.add_months_to_date(NOW, -4)),
                                 (Services.DIRECT.id, DIRECT_FISH, D('300'), utils.add_months_to_date(NOW, -2))],
        'client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2)),
                                  (Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -1))],

        'indi_client_fish_invoices': [(Services.DIRECT.id, DIRECT_FISH, D('100'), utils.add_months_to_date(NOW, -4)),
                                      (Services.DIRECT.id, DIRECT_FISH, D('100'), utils.add_months_to_date(NOW, -2))],
        'indi_client_money_invoices': [(Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2)),
                                       (Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -1))],
        'credit_limit_single': 4800,
        'credit_limit': 1600

    },
    # Multicurrency case with multiple acts 97650 \ 32550
    pytest.mark.smoke({'description': 'Different ActivityType roots with multicurrency case and several acts',
                       'client_fish_invoices': [
                           (Services.DIRECT.id, DIRECT_FISH, D('300'), utils.add_months_to_date(NOW, -4))],
                       'client_money_invoices': [
                           (Services.DIRECT.id, DIRECT_RUB, D('300'), utils.add_months_to_date(NOW, -2)),
                           (Services.GEO.id, GEO, D('300'), utils.add_months_to_date(NOW, -1))],

                       'indi_client_fish_invoices': [
                           (Services.DIRECT.id, DIRECT_FISH, D('100'), utils.add_months_to_date(NOW, -4))],
                       'indi_client_money_invoices': [
                           (Services.DIRECT.id, DIRECT_RUB, D('100'), utils.add_months_to_date(NOW, -2)),
                           (Services.GEO.id, GEO, D('100'), utils.add_months_to_date(NOW, -1))],
                       'credit_limit_single': 68400,
                       'credit_limit': 26050
                       })],
                         ids=lambda x: x['description']
                         )
def test_update_limits_by_root_activity_types(scenario):
    client_id = steps.ClientSteps.create()
    indi_client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})

    invoice_owner = agency_id
    # steps.ClientSteps.link(invoice_owner, 'clientuid32')

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('opt_agency_post',
                                                         {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                          'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                          'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                          'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
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
                                                          'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                          'DISCOUNT_POLICY_TYPE': 0
                                                          })

    individual_limits = json.dumps([{u'client_credit_type': u'1', u'id': u'1', u'client_limit_currency': u'',
                                     u'num': indi_client_id, u'client': indi_client_id,
                                     u'client_payment_term': u'45', u'client_limit': u'50000'}])
    steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, {'CONTRACT2_ID': contract_id,
                                                 'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                 'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
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

    steps.CloseMonth.update_limits(utils.Date.get_last_day_of_previous_month(), 0, [agency_id, indi_client_id])
    collaterals = db.get_collaterals_by_contract(contract_id)
    CREDIT_LIMIT_SINGLE = [attr['value_num'] for attr in
                           db.get_attributes_by_batch_id(collaterals[0]['attribute_batch_id']) if
                           attr['code'] == 'CREDIT_LIMIT_SINGLE'][0]
    client_limit = json.loads([attr['value_str'] for attr in
                               db.get_attributes_by_batch_id(collaterals[1]['attribute_batch_id']) if
                               attr['code'] == 'CLIENT_LIMITS'][0])[u'client_limit']
    assert CREDIT_LIMIT_SINGLE == scenario['credit_limit_single']
    assert client_limit == scenario['credit_limit']


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C05_2))
@pytest.mark.parametrize('context, expected_agency_limit, expected_subclient_limit', [
        pytest.param(DIRECT_CONTEXT, 1500, 600, id='Direct'),
        pytest.param(MARKET_CONTEXT, 1500, 600, id='Market'),
                         ])
def test_update_limits(context, expected_agency_limit, expected_subclient_limit):
    qty_for_subclient_limit = 40
    qty_for_agency_limit = 100
    first_act_dt = NOW - timedelta(days=180)
    second_act_dt = NOW - timedelta(days=31)

    subclient_wo_limit = steps.ClientSteps.create()

    subclient_with_limit = steps.ClientSteps.create()

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    params = {'CREDIT_TYPE': 1,
              'REPAYMENT_ON_CONSUME': 1,
              'PERSONAL_ACCOUNT': 1,
              'LIFT_CREDIT_ON_PAYMENT': 1,
              'DISCOUNT_POLICY_TYPE': 0,
              'DECLARED_SUM': 1000500
              }
    _, person_id, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(context, client_id=agency_id,
                                                                                        contract_type=ContractCommissionType.OPT_AGENCY.id,
                                                                                        postpay=True, start_dt= NOW - timedelta(days=180),
                                                                                          finish_dt=NOW + timedelta(days=180),
                                                                                          additional_params=params)
    individual_limits = json.dumps([{u'client_credit_type': u'1', u'id': u'1', u'client_limit_currency': u'',
                                     u'num': subclient_with_limit, u'client': subclient_with_limit,
                                     u'client_payment_term': u'45', u'client_limit': u'50000'}])
    steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, {'CONTRACT2_ID': contract_id,
                                                 'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                 'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                 'CLIENT_LIMITS': individual_limits})

    for dt in [first_act_dt, second_act_dt]:
        create_invoice_with_act(subclient_wo_limit, agency_id, person_id, context.paysys.id,
                                    contract_id, context.service.id, context.product, qty_for_agency_limit, dt)
        create_invoice_with_act(subclient_with_limit, agency_id, person_id, context.paysys.id,
                                    contract_id, context.service.id, context.product, qty_for_subclient_limit, dt)

    steps.CloseMonth.update_limits(utils.Date.get_last_day_of_previous_month(), 0, [agency_id, subclient_with_limit])

    credit_limit_single = db.get_attributes_by_attr_code(contract_id, 'CREDIT_LIMIT_SINGLE')
    client_limit = json.loads(db.get_attributes_by_attr_code(contract_id, 'CLIENT_LIMITS', collateral_num='01'))[u'client_limit']

    with reporter.step(u'Проверяем, что лимит агентства пересчитался:'):
        assert credit_limit_single == expected_agency_limit
    with reporter.step(u'Проверяем, что индивидуальный лимит пересчитался:'):
        assert int(client_limit) == expected_subclient_limit


if __name__ == '__main__':
    pass

# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D
import json
import pytest

from balance import balance_steps as steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts, Services, Paysyses, Products
import balance.balance_api as api
import balance.balance_db as db
from btestlib import reporter

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW
QTY = D('250')
COMPLETIONS = D('99.99')

DIRECT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new()
GEO = DIRECT.new(service=Services.GEO, product=Products.GEO)
MEDIA_70 = DIRECT.new(service=Services.MEDIA_70, product=Products.MEDIA)
METRICA = DIRECT.new(service=Services.METRICA, product=Products.METRICA)


@pytest.mark.parametrize('services', [
    # [DIRECT, GEO, MEDIA_70, METRICA],
    [DIRECT]
])
def test_offer(services):
    client_id = steps.ClientSteps.create({'REGION_ID': 225}, single_account_activated=False,
                                         enable_single_account=True)
    steps.ClientSteps.link(client_id, 'sandyk-yndx-10')
    # steps.ClientSteps.set_force_overdraft(client_id, service_id=7, limit=100)
    person_id = steps.PersonSteps.create(client_id, 'ur')
    # single_account_number = steps.ElsSteps.create_els(client_id)
    now = datetime.datetime.now()
    paysys_id = Paysyses.BANK_UR_RUB.id
    orders_list = []
    for context in services:
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)  # внешний ID заказа

        order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                           product_id=context.product.id, params={'AgencyID': None})
        orders_list.append({'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 5,
                            'BeginDT': NOW})

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': NOW})

    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                           contract_id=None, overdraft=0, endbuyer_id=None)


def _test_create_invoice():
    context = direct_context = Contexts.DIRECT_MONEY_RUB_CONTEXT
    catalog_context = Contexts.CATALOG1_CONTEXT

    # Создаём клиента
    client_id = None or steps.ClientSteps.create(params=None)

    agency_id = None

    # Далее в скрипте будут фигурировать "владелец счёта": агентство или клиент и "владелей заказа": всегда клиент:
    order_owner = client_id
    invoice_owner = agency_id or client_id

    # Привязать клиента к логину
    steps.ClientSteps.link(invoice_owner, 'sandyk-yndx-10')

    # Создаём плательщика
    person_params = {}
    # person_params = {'fias_guid': '807648a6-7adb-4d82-ac78-251dcce950f4',
    #                  'legal_fias_guid': '8e05359f-282e-45a0-8e04-645b1573a06f',
    #                  'is-partner': '1'}
    # person_params = {'inn': '590579876860'}
    person_id = None or steps.PersonSteps.create(invoice_owner, context.person_type.code, person_params)

    orders_list = []

    for _ in xrange(1):
        service_order_id = steps.OrderSteps.next_id(context.service.id)
        steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id,
                                product_id=context.product.id,
                                params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid})
        orders_list.append(
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': ORDER_DT})

    # Создаём риквест
    request_id = steps.RequestSteps.create(invoice_owner, orders_list,
                                           additional_params=dict(InvoiceDesireDT=INVOICE_DT))

    try:
        db.balance().execute(
            '''update bo.t_config set value_json = :services where item = 'SINGLE_ACCOUNT_ENABLED_SERVICES' ''',
            {'services': json.dumps([context.service.id])}
        )

        db.balance().execute(
            '''update bo.t_config set value_dt = :dt where item = 'SINGLE_ACCOUNT_MIN_CLIENT_DT' ''',
            {'dt': datetime.datetime(2019, 1, 1)}
        )

        # Выставляем счёт
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                     contract_id=None, overdraft=0, endbuyer_id=None)
        charge_note_data = db.get_invoice_by_id(invoice_id)[0]

        personal_account_id = db.balance().execute(
            ''' select repayment_invoice_id from bo.T_INVOICE_REPAYMENT where invoice_id = :invoice_id ''',
            {'invoice_id': invoice_id},
            single_row=True
        )['repayment_invoice_id']
        personal_account_data = db.get_invoice_by_id(personal_account_id)[0]

        payment_id = db.balance().execute(
            ''' select id from bo.T_PAYMENT where invoice_id = :invoice_id ''',
            {'invoice_id': invoice_id},
            single_row=True
        )['id']

        reporter.log(personal_account_data)
        reporter.log(charge_note_data)

        # oebs_export_result = api.test_balance().ExportObject('OEBS', 'Invoice', personal_account_id, 0, None, None)
        # Оплачиваем счёт "быстрым" способом
        steps.InvoiceSteps.pay_fair(personal_account_id, payment_sum=charge_note_data['total_sum'], orig_id=payment_id)

        # Отправляем НЕчестные открутки:
        steps.CampaignsSteps.do_campaigns(context.product.service.id, orders_list[0]['ServiceOrderID'],
                                          {'Bucks': 0, 'Money': COMPLETIONS}, 0, COMPLETIONS_DT)

        # Выставляем акт
        steps.ActsSteps.generate(invoice_owner, force=1, date=ACT_DT)

    finally:
        db.balance().execute(
            '''update bo.t_config set value_json = :services where item = 'SINGLE_ACCOUNT_ENABLED_SERVICES' ''',
            {'services': json.dumps([7])}
        )

        db.balance().execute(
            '''update bo.t_config set value_dt = :dt where item = 'SINGLE_ACCOUNT_MIN_CLIENT_DT' ''',
            {'dt': datetime.datetime(2020, 1, 1)}
        )

    reporter.log(personal_account_data['external_id'])
    # reporter.log(oebs_export_result)


"""
Что нужно проверить:
1. Клиент с договором и ЛС по договору
2. Счёт с несколькими сервисами (продукты должны быть с разным discount_type
3. Счёт с несколькими сервисами, некоторые из которых требуют договора
4. Схема сервиса уже подразумевает charge_note
"""

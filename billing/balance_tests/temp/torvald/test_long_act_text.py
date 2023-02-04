# -*- coding: utf-8 -*-

import datetime
import time
from decimal import Decimal as D

from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils
from btestlib.constants import Services, PersonTypes
from temp.igogor.balance_objects import Contexts
import btestlib.config as balance_config

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

context = Contexts.DIRECT_FISH_RUB_CONTEXT
QTY = D('250')
COMPLETIONS = D('100000')

def test_function():
    # Создаём клиента
    client_id = 188819792 or steps.ClientSteps.create(params=None)
    agency_id = None
    if balance_config.TRUST_ME_I_AM_OEBS_QA:
        steps.export_steps.ExportSteps.export_oebs(client_id=client_id)

    # Далее в скрипте будут фигурировать "владелец счёта": агентство или клиент и "владелей заказа": всегда клиент:
    order_owner = client_id
    invoice_owner = agency_id or client_id

    # Создаём плательщика
    person_params = {}
    # person_params = {'fias_guid': '807648a6-7adb-4d82-ac78-251dcce950f4',
    #                  'legal_fias_guid': '8e05359f-282e-45a0-8e04-645b1573a06f',
    #                  'is-partner': '1'}
    # person_params = {'inn': '590579876860'}
    person_id = 14211241 or steps.PersonSteps.create(invoice_owner, 'ur', person_params)

    contract_id = None

    orders_list = [{'ServiceID': 99, 'ServiceOrderID': 10589889160, 'Qty': QTY, 'BeginDT': ORDER_DT}]

    # Создаём риквест
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=INVOICE_DT))

    # Выставляем счёт
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)

    # Оплачиваем счёт "быстрым" способом
    steps.InvoiceSteps.pay(invoice_id)

    # Отправляем НЕчестные открутки:
    steps.CampaignsSteps.do_campaigns(Services.AUTORU.id, orders_list[0]['ServiceOrderID'],
                                      {'Bucks': COMPLETIONS, 'Units': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)

    # Выставляем акт
    steps.ActsSteps.generate(invoice_owner, force=1, date=ACT_DT)
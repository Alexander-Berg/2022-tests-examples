# coding: utf-8
__author__ = 'atkaya'

from decimal import Decimal

import btestlib.reporter as reporter
from balance import balance_steps as steps
from btestlib.constants import Currencies, PersonTypes, Paysyses, Services
from balance.tests.spendable.test_dmp import DEFAULT_PRODUCT_1, DEFAULT_PRODUCT_2, month3_end_dt

CURRENCY_TO_PAYSYS_AND_PERSON = {
    Currencies.USD: {'paysys_id': Paysyses.BANK_SW_YT_USD.id, 'person_type': PersonTypes.SW_YT.code,
                     'qty': Decimal('100')},
    Currencies.EUR: {'paysys_id': Paysyses.BANK_SW_UR_EUR.id, 'person_type': PersonTypes.SW_UR.code,
                     'qty': Decimal('2000')},
    Currencies.RUB: {'paysys_id': Paysyses.BANK_UR_RUB.id, 'person_type': PersonTypes.UR.code, 'qty': Decimal('5500')},
    Currencies.TRY: {'paysys_id': Paysyses.BANK_SW_YT_TRY.id, 'person_type': PersonTypes.SW_YT.code,
                     'qty': Decimal('10000')}
}

def create_act_for_dmp(service_id, product_id, currency):
    paysys = CURRENCY_TO_PAYSYS_AND_PERSON[currency]['paysys_id']
    person_type = CURRENCY_TO_PAYSYS_AND_PERSON[currency]['person_type']
    qty = CURRENCY_TO_PAYSYS_AND_PERSON[currency]['qty']
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type)

    service_order_id = steps.OrderSteps.next_id(service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                                       service_id=service_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': month3_end_dt}, ]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': month3_end_dt})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys, credit=0,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Shows': qty}, 0, month3_end_dt)
    steps.ActsSteps.generate(client_id, force=1, date=month3_end_dt)

# создание данных, которые используются в balance-tests/balance/tests/spendable/test_dmp.py
def test_create_data_for_dmp():
    with reporter.step(u'Создаем акты, которые будут использоваться как данные для DMP'):
        create_act_for_dmp(Services.ADFOX.id, DEFAULT_PRODUCT_1['id'], Currencies.RUB)
        create_act_for_dmp(Services.MEDIA_70.id, DEFAULT_PRODUCT_2['id'], Currencies.USD)
        create_act_for_dmp(Services.MEDIA_70.id, DEFAULT_PRODUCT_1['id'], Currencies.EUR)
        create_act_for_dmp(Services.MEDIA_70.id, DEFAULT_PRODUCT_1['id'], Currencies.TRY)
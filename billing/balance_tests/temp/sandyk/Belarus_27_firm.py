# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import allure
import pytest

import balance.balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services, Products, ContractPaymentType, Currencies, Managers, Firms, PersonTypes, Paysyses


PERSON_TYPE = PersonTypes.BYU.code
PAYSYS_ID = Paysyses.BANK_BY_UR_BYN.id
FIRM_ID = Paysyses.BANK_BY_UR_BYN.firm.id
SERVICE_ID =Services.DIRECT.id
PRODUCT_ID =  Products.DIRECT_FISH.id
CURRENCY_PRODUCT_ID =  Products.DIRECT_BYN.id
PAYMENT_TYPE = ContractPaymentType.PREPAY
CURRENCY = Paysyses.BANK_BY_UR_BYN.currency.num_code

DT = datetime.datetime.now()

QTY = 100
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


@pytest.mark.slow
# @allure.feature(Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-25795')
def test_wo_contract():   ##https://balance-admin.greed-tm1f.yandex.ru/invoice-publish.xml?ft=html&object_id=64685619
    # client_id = steps.ClientSteps.create(params={'IS_AGENCY':0})
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    client_id =56593392
    person_id = 5931437
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID ,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
    ]

    ##выставляем счет
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 65}, 0, campaigns_dt=DT)
    steps.ActsSteps.generate(client_id, 1, DT)
# def test_with_contract():  ##
#     client_id = steps.ClientSteps.create(params={'IS_AGENCY': 0})
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
#     contract_id = steps.ContractSteps.create_contract_new('no_agency', {'CLIENT_ID': client_id,
#                                                          'PERSON_ID': person_id,
#                                                           'IS_FIXED': START_DT, 'DT': START_DT, 'IS_SIGNED': START_DT,
#                                                            'FIRM': FIRM_ID, 'SERVICES':[SERVICE_ID],
#                                                             'PAYMENT_TYPE':PAYMENT_TYPE,
#      'CURRENCY':CURRENCY})[0]
#
#     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
#                                        {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
#     orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}]
#
#     ##выставляем счет
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#                                            additional_params=dict(InvoiceDesireDT=DT))
#
#     # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#     #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
#
#
# def test_currency_product():  ##
#     client_id = steps.ClientSteps.create(params={'IS_AGENCY': 0, 'REGION_ID':149})
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
#     # contract_id = steps.ContractSteps.create_contract_new('no_agency', {'CLIENT_ID': client_id,
#     #                                                      'PERSON_ID': person_id,
#     #                                                       'IS_FIXED': START_DT, 'DT': START_DT, 'IS_SIGNED': START_DT,
#     #                                                        'FIRM': FIRM_ID, 'SERVICES':[SERVICE_ID],
#     #                                                         'PAYMENT_TYPE':PAYMENT_TYPE,
#     #  'CURRENCY':CURRENCY})[0]
#
#     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id, service_order_id, CURRENCY_PRODUCT_ID, SERVICE_ID,
#                                        {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
#     orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}]
#
#     ##выставляем счет
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#                                            additional_params=dict(InvoiceDesireDT=DT))
#
#     # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#     #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

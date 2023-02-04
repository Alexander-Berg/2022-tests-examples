# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

import pytest

from balance import balance_steps as steps

dt = datetime.datetime.now()

# продукт в долларах
SERVICE_ID = 42
# product_param_list = [{'product_id': 506619, 'unit_type': 'USD'}]
#
# MSR = 'Bucks'
# paysys_param_list = [
#                     {'person_type': 'sw_ur', 'paysys_id': 1601044, 'person_category': 'sw_ur', 'currency': 'USD'},
#                     {'person_type': 'sw_yt','paysys_id': 1601047, 'person_category': 'sw_yt', 'currency': 'USD'},
# ]
#
#
# contract_params_list = [
#     {'contract_type': 'sw_opt_client_pre_16', 'credit': 0},
#     {'contract_type': 'sw_opt_client_post_16', 'credit': 0},
#     {'contract_type': 'sw_opt_client_post_16', 'credit': 1}
# ]
#
#
# def create_invoice(service_order_id, client_id, person_id, paysys_id, contract_id, credit):
#     orders_list = [
#             {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
#         ]
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
#     invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys_id,
#                                                           credit=credit, contract_id=contract_id, overdraft=0,endbuyer_id=None)
#     return invoice_id
#
#
#
#
# @pytest.mark.parametrize('product_param', product_param_list)
# @pytest.mark.parametrize('paysys_param', paysys_param_list)
# @pytest.mark.parametrize('contract_param', contract_params_list)
# def test_invoice_toloka_service(paysys_param, product_param, contract_param):
#     client_id = steps.ClientSteps.create()
#     paysys_id = paysys_param['paysys_id']
#     product_id = product_param['product_id']
#     person_type = paysys_param['person_type']
#     contract_type = contract_param['contract_type']
#     credit = contract_param['credit']
#     person_id = steps.PersonSteps.create(client_id, person_type)
#     service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=SERVICE_ID, service_order_id=service_order_id)
#     contract_id, _ = steps.ContractSteps.create(contract_type,{'client_id': client_id, 'person_id': person_id,
#                                                    'dt'       : '2015-04-30T00:00:00',
#                                                    'FINISH_DT': '2016-06-30T00:00:00',
#                                                    'is_signed': '2015-01-01T00:00:00',
#                                                    'SERVICES': [42],
#                                                    'CURRRENCY': 'USD',
#                                                    'FIRM': '16'})
#     invoice_id = create_invoice(service_order_id, client_id, person_id, paysys_id, contract_id, credit)
#     assert invoice_id == 0
#
#
# if __name__ == "__main__":
#     # pytest.main("person_vs_client.py -v")
#     # pytest.main("person_vs_client.py -vk 'test_create_second_person_on_client'")
#     pytest.main("toloka_test.py -vk 'test_invoice_toloka_service'")
#     # pytest.main("person_vs_client.py -vk 'test_change_region_on_client_with_person'")


PERSON_TYPE = 'sw_yt'
PAYSYS_ID = 1601047
# PAYSYS_ID = 1601047
SERVICE_ID = 42
PRODUCT_ID = 506619
MSR = 'Bucks'
contract_type = 'sw_opt_client_pre_16'


client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)


service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)

# contract_id, _ = steps.ContractSteps.create(contract_type,{'client_id': client_id, 'person_id': person_id,
#                                                    'dt'       : '2015-04-30T00:00:00',
#                                                    'FINISH_DT': '2016-06-30T00:00:00',
#                                                    'is_signed': '2015-01-01T00:00:00',
#                                                    'SERVICES': [42],
#                                                    # 'CURRENCY': 'USD',
#                                                    'FIRM': '16'})

order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
        ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))



invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                              credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)
#
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
#
# steps.OrderSteps.ua_enqueue([50537])
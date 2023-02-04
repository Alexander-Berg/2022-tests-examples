# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import balance.balance_db as db
from balance import balance_steps as steps

SERVICE_ID = 7
LOGIN = 'clientuid45'
PRODUCT_ID= 1475
PAYSYS_ID = 1001
PERSON_TYPE ='ph'
QUANT = 100
MAIN_DT = datetime.datetime.now()
OLD_DT = datetime.datetime(2016,3,12)


##### test https://github.yandex-team.ru/Billing/balance-tests/blob/master/balance/tests/promocode/test_unique_urls_and_deny_promocode.py

@pytest.mark.parametrize('params', [{'need_unique_urls': 1, 'deny_promocode': 1, 'rezult': 0},
                                    {'need_unique_urls': 1, 'deny_promocode': 0, 'rezult': 1},
                                    {'need_unique_urls': 0, 'deny_promocode': 1, 'rezult': 1},
                                    {'need_unique_urls': 0, 'deny_promocode': 0, 'rezult': 1},
                                    ]
    , ids=lambda x: 'need_unique_urls' + x['need_unique_urls'] + 'deny_promocode' + x['deny_promocode'])
def unique_urls_and_deny_promocode():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, {'email': 'test-balance-notify@yandex-team.ru'})
    steps.ClientSteps.link(client_id, LOGIN)
    # # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # #
    # # order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
    # #     {'TEXT':'Py_Test order','AgencyID': None, 'ManagerUID': None})
    # #
    # # orders_list = [
    # #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': OLD_DT}
    # # ]
    # # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': OLD_DT})
    # # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    # #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    # #
    # # steps.InvoiceSteps.pay(invoice_id, None, None)
    # # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,{'Bucks': QUANT, 'Days': 0, 'Money': 0}, 0, OLD_DT)
    # # steps.ActsSteps.generate(client_id, 1, OLD_DT)
    #
    # # client_id = 14279564
    # # steps.ClientSteps.link(client_id, LOGIN)
    # # # person_id = 4305933
    # #
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    orders_list = []
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]

    # # steps.PromocodeSteps.reserve(client_id, promocode_id)
    #
    # ##здесь просто проверяем, что наличие/отсутствие DenyPromocode или словаря, в котором он передается, ничего не ломает
    # # tm = xmlrpclib.ServerProxy("http://greed-tm1f.yandex.ru:8002/xmlrpc", allow_none=1, use_datetime=1)
    # # test = xmlrpclib.ServerProxy("http://xmlrpc.balance.greed-tm1f.yandex.ru:30702/xmlrpc", allow_none=1,
    # #                              use_datetime=1)
    # # print tm.Balance.CreateRequest(16571028, client_id, [{'BeginDT': MAIN_DT, 'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT}]
    # # , {'InvoiceDesireDT': MAIN_DT, 'DenyPromocode': 1})
    #
    # #
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': MAIN_DT, 'DenyPromocode': 1})
    # # request_id = steps.RequestSteps.create(client_id, orders_list,
    # #                                        additional_params={'InvoiceDesireDT': MAIN_DT, 'DenyPromocode': 0})
    #
    promocode_id = steps.PromocodeSteps.create(start_dt=datetime.datetime(2016, 9, 20),
                                               end_dt=datetime.datetime(2016, 12, 20), bonus1=20, bonus2=20,
                                               minimal_qty=10, reservation_days=None, need_unique_urls=0)
    promocode_code = db.get_promocode_by_id(promocode_id)[0]['code']
    print promocode_code
    print service_order_id
    print client_id
    # promocode_id = steps.PromocodeSteps.create(start_dt=MAIN_DT, end_dt=None, bonus1=20, bonus2=20,
    #                                            minimal_qty=10, reservation_days=None)
    # promocode_code = db.get_promocode_by_id(promocode_id)[0]['code']
    # # steps.PromocodeSteps.reserve(client_id, promocode_id)
    # # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    # # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={})
    # # request_id = steps.RequestSteps.create(client_id, orders_list)
    # #
    #
    #
    #
    #
    # # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # # order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
    # #     {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    # #
    # # orders_list = [
    # #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    # # ]
    # # request_id = steps.RequestSteps.create(client_id, orders_list)
    #
    #
    # # steps.InvoiceSteps.pay(51050833, 1000, None)
    # # tm = xmlrpclib.ServerProxy("http://greed-tm1f.yandex.ru:8002/xmlrpc", allow_none=1, use_datetime=1)
    # # print tm.Balance.CreateRequest(16571028, client_id, [{'BeginDT': MAIN_DT, 'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT}])
    # steps.InvoiceSteps.pay(53094452)

if __name__ == "__main__":
    unique_urls_and_deny_promocode()

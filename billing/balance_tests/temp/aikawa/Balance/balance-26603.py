# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime
import balance.balance_db as db
import balance.balance_steps as steps
import pytest

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
SERVICE_ID = 7
PRODUCT_ID = 1475
CURRENCY_PRODUCT_ID = 503162
CURRENCY_PRODUCT_ID_ANOTHER = 503163
PAYSYS_ID = 1003


# Миграция на мультивалютность пересчитывает child_ua_type
@pytest.mark.parametrize('child_ua_type_before', [0, 1, None])
def test_child_order_type0(child_ua_type_before):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                             product_id=PRODUCT_ID,
                                             service_id=SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                               product_id=PRODUCT_ID,
                                               service_id=SERVICE_ID)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=PRODUCT_ID, service_id=SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])
    db.balance().execute('''UPDATE t_order SET child_ua_type = :child_ua_type WHERE id = :id''',
                         {'id': child_order_id, 'child_ua_type': child_ua_type_before})
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    assert child_ua_type == 0


@pytest.mark.parametrize('child_ua_type_before', [0, 1, None])
def test_child_order_type2(child_ua_type_before):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                             product_id=PRODUCT_ID,
                                             service_id=SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                               product_id=PRODUCT_ID,
                                               service_id=SERVICE_ID)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=PRODUCT_ID, service_id=SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    db.balance().execute('''UPDATE t_order SET child_ua_type = :child_ua_type WHERE id = :id''',
                         {'id': child_order_id, 'child_ua_type': child_ua_type_before})
    steps.OrderSteps.make_optimized(parent_order_id)
    child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    assert child_ua_type == 1


# признак 0
@pytest.mark.parametrize('child_ua_type_before', [0, 1, None])
def test_child_order_type22343(child_ua_type_before):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                             product_id=PRODUCT_ID,
                                             service_id=SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                               product_id=PRODUCT_ID,
                                               service_id=SERVICE_ID)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=PRODUCT_ID, service_id=SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

    db.balance().execute('''UPDATE t_order SET child_ua_type = :child_ua_type WHERE id = :id''',
                         {'id': child_order_id, 'child_ua_type': child_ua_type_before})
    child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    assert child_ua_type == 0


@pytest.mark.parametrize('child_ua_type_before', [
    # 0,
    # 1,
    None])
def test_child_order_type3aaa(child_ua_type_before):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                             product_id=PRODUCT_ID,
                                             service_id=SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                               product_id=PRODUCT_ID,
                                               service_id=SERVICE_ID)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=PRODUCT_ID, service_id=SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    steps.OrderSteps.ua_enqueue([client_id])
    # steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    # steps.OrderSteps.make_optimized(parent_order_id)
    # child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    # assert child_ua_type == 1
    # service_order_id3 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    # child_order_id3 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id3,
    #                                           product_id=PRODUCT_ID,
    #                                           service_id=SERVICE_ID,
    #                                           params={'GroupServiceOrderID': parent_service_order_id})
    # # db.balance().execute('''UPDATE t_order SET child_ua_type = :child_ua_type WHERE id = :id''',
    # #                      {'id': child_order_id3, 'child_ua_type': child_ua_type_before})
    # # steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id3], group_without_transfer=True)
    # child_ua_type = db.get_order_by_id(child_order_id3)[0]['child_ua_type']

    # assert child_ua_type == 0
    # steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    # child_ua_type = db.get_order_by_id(child_order_id3)[0]['child_ua_type']
    #
    # assert child_ua_type == 1
    # steps.OrderSteps.merge(-1, sub_orders_ids=[child_order_id3])
    # child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    # assert child_ua_type == 1


# не делаем из неоткл откл
@pytest.mark.parametrize('child_ua_type_before', [0, 1, None])
def test_child_order_type33(child_ua_type_before):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                             product_id=PRODUCT_ID,
                                             service_id=SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                               product_id=PRODUCT_ID,
                                               service_id=SERVICE_ID)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=PRODUCT_ID, service_id=SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    steps.OrderSteps.ua_enqueue([client_id])
    # steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    # # steps.OrderSteps.make_optimized(parent_order_id)
    # child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    # assert child_ua_type == 0
    # service_order_id3 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    # child_order_id3 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id3,
    #                                           product_id=PRODUCT_ID,
    #                                           service_id=SERVICE_ID)
    # # db.balance().execute('''UPDATE t_order SET child_ua_type = :child_ua_type WHERE id = :id''',
    # #                      {'id': child_order_id3, 'child_ua_type': child_ua_type_before})
    # steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id3], group_without_transfer=0)
    # child_ua_type = db.get_order_by_id(child_order_id3)[0]['child_ua_type']
    # assert child_ua_type == 0


@pytest.mark.parametrize('cur_product', [CURRENCY_PRODUCT_ID, CURRENCY_PRODUCT_ID_ANOTHER])
def test_child_order_type_equal_product(cur_product):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                             product_id=cur_product,
                                             service_id=SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                               product_id=PRODUCT_ID,
                                               service_id=SERVICE_ID)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=PRODUCT_ID, service_id=SERVICE_ID)

    steps.OrderSteps.make_optimized(parent_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    steps.OrderSteps.make_optimized(parent_order_id)
    child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    assert child_ua_type == 1


@pytest.mark.parametrize('another_product', [2136])
def test_child_order_type_equal_product_another(another_product):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                             product_id=another_product,
                                             service_id=SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                               product_id=another_product,
                                               service_id=SERVICE_ID)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=another_product, service_id=SERVICE_ID)

    steps.OrderSteps.make_optimized(parent_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    steps.OrderSteps.make_optimized(parent_order_id)
    child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    assert child_ua_type == 1


def test_child_order_type_ordinary_case():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                             product_id=CURRENCY_PRODUCT_ID,
                                             service_id=SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                               product_id=508575,
                                               service_id=SERVICE_ID)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=CURRENCY_PRODUCT_ID, service_id=SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])

    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    steps.OrderSteps.make_optimized(parent_order_id)
    child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    # assert child_ua_type == 1
    child_ua_type = db.get_order_by_id(child_order_id_2)[0]['child_ua_type']
    # assert child_ua_type == 0

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': 100}, 0, dt - datetime.timedelta(days=1))
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id_2, {'Money': 50}, 0, dt - datetime.timedelta(days=1))
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)


def test_child_order_type_ordinary_case2():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                             product_id=PRODUCT_ID,
                                             service_id=SERVICE_ID)
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='COPY',
                                          dt=dt - datetime.timedelta(days=1))
    service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                               product_id=508575,
                                               service_id=SERVICE_ID)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=CURRENCY_PRODUCT_ID, service_id=SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])

    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    steps.OrderSteps.make_optimized(parent_order_id)
    child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    # assert child_ua_type == 1
    child_ua_type = db.get_order_by_id(child_order_id_2)[0]['child_ua_type']
    # assert child_ua_type == 0

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': 100}, 0, dt - datetime.timedelta(days=1))
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id_2, {'Money': 50}, 0, dt - datetime.timedelta(days=1))
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

#
#
# def test_child_order_type3():
#     client_id = steps.ClientSteps.create()
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
#
#     service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
#                                              product_id=PRODUCT_ID,
#                                              service_id=SERVICE_ID)
#
#     service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     child_order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
#                                                product_id=PRODUCT_ID,
#                                                service_id=SERVICE_ID)
#
#     parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
#                                               product_id=PRODUCT_ID, service_id=SERVICE_ID)
#
#     orders_list = [
#         {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 200, 'BeginDT': dt}
#     ]
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
#     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
#     steps.InvoiceSteps.pay(invoice_id)
#
#     steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id, child_order_id_2])
#     steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
#                                           dt=dt - datetime.timedelta(days=1))
#     # steps.OrderSteps.ua_enqueue([client_id])
#     print parent_order_id
#     steps.OrderSteps.make_optimized_force(parent_order_id)
#     steps.OrderSteps.ua_enqueue([client_id])
#     steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
#     child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
#     assert child_ua_type == 1

# -*- coding: utf-8 -*-

import pprint
import datetime
from multiprocessing.dummy import Pool

from temp.MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc

service_id = 7;
product_id = 1475  ##503162
##service_id = 11; product_id = 2136
##service_id = 70; product_id = 503273
##service_id = 77; product_id = 2584
##service_id = 99; product_id = 504596 ##504697

qty = 100
qty2 = 200
paysys_id = 1003

after = datetime.datetime(2015, 7, 6, 11, 0, 0)  # datetime.datetime.now()
disc_dt = datetime.datetime(2015, 7, 6, 11, 0, 0)

begin_dt = after
request_dt = after  ##не меняется
invoice_dt = after
payment_dt = after
campaigns_dt = after
act_dt = after
migrate_dt = after

manager_uid = None
##manager_uid = '96446401'
##manager_uid = '176005458'
uid = 'clientuid33'
##------------------------------------------------------------------------------
OPERATION_ONLY = 'OPERATION_ONLY'
WITH_TRANSFER = 'WITH_TRANSFER'
PARALLEL_TRANSFER = 'PARALLEL_TRANSFER'


def data_generator(test_mode=1, passport_uid2=mtl.passport_uid, next_call=False, result_return=True,
                   incorrect_old_qty=False, input_changing=False):
    client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})
    order_owner = client_id
    invoice_owner = agency_id
    if order_owner == invoice_owner: agency_id = None
    mtl.link_client_uid(invoice_owner, 'clientuid32')
    person_id = None or mtl.create_person(invoice_owner, 'ur', {'phone': '234'})

    contract_id = None

    service_order_id = mtl.get_next_service_order_id(service_id)
    ##    service_order_id = 12345678
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    service_order_id2 = mtl.get_next_service_order_id(service_id)
    order_id2 = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id2,
                                           {'TEXT': 'Py_Test order', 'unmoderated': 0}, agency_id=agency_id,
                                           manager_uid=manager_uid)
    service_order_id3 = mtl.get_next_service_order_id(service_id)
    order_id3 = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id3,
                                           {'TEXT': 'Py_Test order', 'unmoderated': 0}, agency_id=agency_id,
                                           manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty2, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=1, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)

    if test_mode == OPERATION_ONLY:
        # <finally>
        operation_id = mtl.create_operation(mtl.passport_uid)
        # <\finally>

    if test_mode == WITH_TRANSFER:
        operation_id = mtl.create_operation(mtl.passport_uid)
        # <finally>
        for i in range(2 if next_call else 1):
            try:
                result = mtl.log(mtl.create_transfer_multiple)(passport_uid2, [
                    {"ServiceID": service_id, "ServiceOrderID": service_order_id,
                     "QtyOld": qty if not incorrect_old_qty else qty + 1, "QtyNew": qty * 0.9}],
                                                               [{"ServiceID": service_id,
                                                                 "ServiceOrderID": service_order_id2 if (
                                                                     (
                                                                         not input_changing) or i != 1) else service_order_id3,
                                                                 "QtyDelta": 1}], result_return, operation_id)
            except Exception, e:
                print (e)
            else:
                print (result)
                # <\finally>

    if test_mode == PARALLEL_TRANSFER:
        operation_id = mtl.create_operation(mtl.passport_uid)
        # <finally>
        def f(x):
            rpc = btestlib.temp.MTestlib.proxy_provider.GetServiceProxy(mtl.host, 0)
            status = rpc.Balance.CreateTransferMultiple(passport_uid2, [
                {"ServiceID": service_id, "ServiceOrderID": service_order_id,
                 "QtyOld": qty if not incorrect_old_qty else qty + 1, "QtyNew": qty * 0.9}],
                                                        [{"ServiceID": service_id, "ServiceOrderID": service_order_id2,
                                                          "QtyDelta": 1}], result_return, operation_id)
            print (status)

        pool = Pool(2)
        pool.map(f, range(2))
        # <\finally>

    mtl.Print(dict(mtl.objects))


# Test_1: simple test for CreateOperation method
def init_create_operation_method():
    data_generator(test_mode=OPERATION_ONLY)
    # RESUILT: null, INPUT: null, ERROR: null


# Test_2: [Transfer_Multiple_Async_API]_incompatiable_passport_uids
def init_incompatiable_passport_uids():
    data_generator(test_mode=WITH_TRANSFER, passport_uid2=16571029, next_call=False, result_return=True,
                   incorrect_old_qty=False)
    # RESUILT: null, INPUT: null, ERROR: null


# Test_2b: [Transfer_Multiple_Async_API]_incompatiable_passport_uids (next call)
def init_incompatiable_passport_uids_next_call():
    data_generator(test_mode=WITH_TRANSFER, passport_uid2=16571029, next_call=True, result_return=True,
                   incorrect_old_qty=False)
    # RESUILT: null, INPUT: null, ERROR: null


# Test_3: [Transfer_Multiple_Async_API]_unexisted_passport_uid
def init_unexisted_passport_uid():
    data_generator(test_mode=WITH_TRANSFER, passport_uid2=-1, next_call=False, result_return=True,
                   incorrect_old_qty=False)
    # RESUILT: null, INPUT: null, ERROR: null


# Test_4: [Transfer_Multiple_Async_API]_successful_transfer
def init_successful_transfer():
    data_generator(test_mode=WITH_TRANSFER, passport_uid2=mtl.passport_uid, next_call=False, result_return=True,
                   incorrect_old_qty=False)
    # RESUILT: result, INPUT: input, ERROR: null


# Test_4b: [Transfer_Multiple_Async_API]_successful_transfer (next call)
def init_successful_transfer_next_call():
    data_generator(test_mode=WITH_TRANSFER, passport_uid2=mtl.passport_uid, next_call=True, result_return=True,
                   incorrect_old_qty=False)
    # RESUILT: result, INPUT: input, ERROR: null


# Test_5 [Transfer_Multiple_Async_API]_successful_transfer_with_no_result_return
def init_with_no_result_return():
    data_generator(test_mode=WITH_TRANSFER, passport_uid2=mtl.passport_uid, next_call=False, result_return=False,
                   incorrect_old_qty=False)
    # RESUILT: null, INPUT: input, ERROR: null


# Test_6 [Transfer_Multiple_Async_API]_transfer_exception_orders_is_not_synchronized
def init_transfer_exception():
    data_generator(test_mode=WITH_TRANSFER, passport_uid2=mtl.passport_uid, next_call=False, result_return=True,
                   incorrect_old_qty=True)
    # RESUILT: null, INPUT: input, ERROR: error


# Test_6b [Transfer_Multiple_Async_API]_transfer_exception_orders_is_not_synchronized (next call)
def init_transfer_exception_next_call():
    data_generator(test_mode=WITH_TRANSFER, passport_uid2=mtl.passport_uid, next_call=True, result_return=True,
                   incorrect_old_qty=True)
    # RESUILT: null, INPUT: input, ERROR: error


# Test_7: [Transfer_Multiple_Async_API]_OPERATION_IN_PROGRESS_exception
def init_operation_in_progress():
    data_generator(test_mode=PARALLEL_TRANSFER, passport_uid2=mtl.passport_uid, next_call=False, result_return=True,
                   incorrect_old_qty=False)
    # RESUILT: result, INPUT: input, ERROR: null


# Test_7: [Transfer_Multiple_Async_API]_input_value_changing
def init_input_value_changing():
    data_generator(test_mode=WITH_TRANSFER, passport_uid2=mtl.passport_uid, next_call=True, result_return=True,
                   incorrect_old_qty=False, input_changing=True)
    # RESUILT: result, INPUT: input, ERROR: null


if __name__ == '__main__':
    init_incompatiable_passport_uids()
    pass

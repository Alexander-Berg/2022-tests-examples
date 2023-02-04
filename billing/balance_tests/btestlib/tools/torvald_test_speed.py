# coding: utf-8

import datetime
import time

import balance.balance_api as api
import btestlib.reporter as reporter
import temp.MTestlib.MTestlib as mtl
from balance import balance_steps as steps
from btestlib.data import defaults

benchmark_series = []
effort = []

# Timer decorator
def time_deco(func):

    global effort
    global benckmark_series

    if func.__name__ == 'stop':
        benchmark_series.append(effort)
        effort = []
        return func

    def time_wrapper(*args, **kwargs):
        start = time.clock()
        res = func(*args, **kwargs)
        end = time.clock()
        effort.append('{0:<16} : {1:>10.6f} |'.format(func.__name__, end - start))
        reporter.log(('{0:<16} : {1:>10.6f} |'.format(func.__name__, end - start)))
        return res

    return time_wrapper

# Signal method
def stop():
    '''
    Just signal for time_deco to stop current iteration and push data to benchmark_series
    '''
    pass

# Result representation
def get_stats ():
    for num, step in enumerate(benchmark_series[0]):
        reporter.log((step + ' '.join([effort[num][20:] for effort in benchmark_series[1:]])))
# ---------------------------------------------------------------------------------------------------------------------

def new():

    for run in xrange(3):
        SERVICE_ID = 7
        PRODUCT_ID = 1475
        PAYSYS_ID = 1003
        QTY = 100.1234
        BASE_DT = datetime.datetime.now()

        client_id = time_deco(steps.ClientSteps.create)()
        agency_id = time_deco(steps.ClientSteps.create)({'IS_AGENCY': 1})
        order_owner = client_id
        invoice_owner = agency_id or client_id
        person_id = time_deco(steps.PersonSteps.create)(invoice_owner, 'ur')

        contract_id, _ = time_deco(steps.ContractSteps.create_contract)('opt_agency_prem_post',
                                                                        {'CLIENT_ID': invoice_owner,
                                                                         'PERSON_ID': person_id,
                                                                         'DT': '2015-04-30T00:00:00',
                                                                         'FINISH_DT': '2016-06-30T00:00:00',
                                                                         'IS_SIGNED': '2015-01-01T00:00:00',
                                                                         'SERVICES': [7],
                                                                         # 'COMMISSION_TYPE': 57,
                                                                         # 'NON_RESIDENT_CLIENTS': 0,
                                                                         # 'REPAYMENT_ON_CONSUME': 0,
                                                                         'PERSONAL_ACCOUNT': 1,
                                                                         'LIFT_CREDIT_ON_PAYMENT': 1,
                                                                         'PERSONAL_ACCOUNT_FICTIVE': 1
                                                                         })

        service_order_id = time_deco(steps.OrderSteps.next_id)(SERVICE_ID)
        time_deco(steps.OrderSteps.create)(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                           params={'AgencyID': agency_id})
        service_order_id2 = time_deco(steps.OrderSteps.next_id)(SERVICE_ID)
        time_deco(steps.OrderSteps.create)(order_owner, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                           params={'AgencyID': agency_id})
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
        request_id = time_deco(steps.RequestSteps.create)(invoice_owner, orders_list)
        invoice_id, _, _ = time_deco(steps.InvoiceSteps.create)(request_id, person_id, PAYSYS_ID, credit=1,
                                                                contract_id=contract_id,
                                                                overdraft=0, endbuyer_id=None)
        time_deco(steps.InvoiceSteps.pay)(invoice_id)

        response = time_deco(api.medium().CreateTransferMultiple)(defaults.PASSPORT_UID,
                                                                  [
                                                                                         {"ServiceID": SERVICE_ID,
                                                                                          "ServiceOrderID": service_order_id,
                                                                                          "QtyOld": 100.1234, "QtyNew": 20}
                                                                                     ],
                                                                  [
                                                                                         {"ServiceID": SERVICE_ID,
                                                                                          "ServiceOrderID": service_order_id2,
                                                                                          "QtyDelta": 1}
                                                                                     ], 1, None)

        time_deco(steps.CampaignsSteps.do_campaigns)(SERVICE_ID, service_order_id, {'Bucks': 20.12, 'Money': 0}, 0, BASE_DT)
        time_deco(steps.CampaignsSteps.do_campaigns)(SERVICE_ID ,service_order_id2, {'Bucks': 19.84, 'Money': 0}, 0, BASE_DT)
        time_deco(steps.ActsSteps.generate)(invoice_owner, force=1, date=BASE_DT)

        time_deco(stop)()


def old():
    SERVICE_ID = 7
    PRODUCT_ID = 503162
    PAYSYS_ID = 1003
    QTY = 100.1234
    BASE_DT = datetime.datetime.now()

    client_id = time_deco(mtl.create_client)({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    person_id = time_deco(mtl.create_person)(client_id, 'ur')

    service_order_id = time_deco(mtl.get_next_service_order_id)(SERVICE_ID)
    order_id = time_deco(mtl.create_or_update_order)(client_id, PRODUCT_ID, SERVICE_ID, service_order_id,
                                                     {'TEXT': 'Py_Test order'})
    service_order_id2 = time_deco(mtl.get_next_service_order_id)(SERVICE_ID)
    order_id = time_deco(mtl.create_or_update_order)(client_id, PRODUCT_ID, SERVICE_ID, service_order_id2,
                                                     {'TEXT': 'Py_Test order'})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = time_deco(mtl.create_request)(client_id, orders_list, BASE_DT)

    invoice_id = time_deco(mtl.create_invoice)(request_id, person_id, PAYSYS_ID, credit=0, contract_id=None,
                                               overdraft=0, endbuyer_id=None)
    time_deco(mtl.OEBS_payment)(invoice_id)

    response = time_deco(mtl.rpc.Balance.CreateTransferMultiple)(defaults.PASSPORT_UID,
                                                                 [
                                                                     {"ServiceID": SERVICE_ID,
                                                                      "ServiceOrderID": service_order_id,
                                                                      "QtyOld": 100.1234, "QtyNew": 0}
                                                                 ],
                                                                 [
                                                                     {"ServiceID": SERVICE_ID,
                                                                      "ServiceOrderID": service_order_id2,
                                                                      "QtyDelta": 1}
                                                                 ], 1, None)


if __name__ == "__main__":
    new()
    get_stats ()

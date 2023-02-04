import datetime

from balance import balance_steps as steps
from btestlib import utils as utils

dt = datetime.datetime.now()
first_day_of_month = utils.Date.first_day_of_month(dt)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

# def test_ua_transfer_check_for_objects():
#     #
#     client_id = steps.ClientSteps.create()
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
#
#     service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#                                        service_order_id=service_order_id)
#     service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     order_id2 = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#                                        service_order_id=service_order_id2)
#     orders_list = [
#         {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt},
#         {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 200, 'BeginDT': dt}
#     ]
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
#
#     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
#     steps.InvoiceSteps.pay(invoice_id)
#
#     steps.OrderSteps.merge(order_id2, [order_id])
#     print db.balance().execute('''select state, export_dt from t_export where type = 'UA_TRANSFER' and object_id = :object_id and classname = 'Client'
# ''', {'object_id': client_id})
#     db.balance().execute('''update t_export set state = 2, export_dt = :dt where type = 'UA_TRANSFER' and object_id = :object_id and classname = 'Client'
#     ''', {'object_id': client_id, 'dt': first_day_of_month})
#     print db.balance().execute('''select state, export_dt from t_export where type = 'UA_TRANSFER' and object_id = :object_id and classname = 'Client'
#     ''', {'object_id': client_id})
#     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
#
#     act_id = steps.ActsSteps.generate(client_id, force=1, date=dt, with_coverage=True)[0]
#
#     operation = db.get_operation_by_act(act_id)
#     parent_operation_id = operation[0]['id']
#     print parent_operation_id
#
# def test_act_enqueuer():
#     client_id = steps.ClientSteps.create()
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
#
#     service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#                                        service_order_id=service_order_id)
#     orders_list = [
#         {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
#     ]
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
#
#     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
#     steps.InvoiceSteps.pay(invoice_id)
#     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
#     api.coverage().server.Coverage.Reset()
#     steps.ActsSteps.enqueue([client_id], force=1, date=dt)
#     print api.coverage().server.Coverage.Collect('aikawa-coverage', True)
#     print db.balance().execute('''select state, export_dt, input from t_export where type = 'MONTH_PROC' and object_id = :object_id and classname = 'Client'
#     ''', {'object_id': client_id})
#     print steps.CommonSteps.get_pickled_value('''select state, export_dt, input from t_export where type = 'MONTH_PROC' and object_id = {0} and classname = 'Client'
#     '''.format(client_id))
#

dt = datetime.datetime.now()
last_day_of_previous_month = utils.Date.first_day_of_month(dt) - datetime.timedelta(days=1)
first_day_of_next_month = utils.Date.last_day_of_month(dt) + datetime.timedelta(days=1)


def test_deferpays():
    to_iso = utils.Date.date_to_iso_format
    NOW = datetime.datetime.now()
    HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
    HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    contract_id, _ = steps.ContractSteps.create_contract_new('opt_agency', {'CLIENT_ID': client_id,
                                                                            'PERSON_ID': person_id,
                                                                            'SERVICES': [7],
                                                                            'PAYMENT_TYPE': 3,
                                                                            'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                            'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                            'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                            'PERSONAL_ACCOUNT_FICTIVE': 0
                                                                            })
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
    repayment_invoice_id = steps.InvoiceSteps.make_repayment_invoice(invoice_id)
    steps.ActsSteps.enqueue([client_id], force=1, date=dt)
    input_ = (steps.CommonSteps.get_pickled_value('''select state, export_dt, input from t_export where type = 'MONTH_PROC' and object_id = {0} and classname = 'Client'
       '''.format(client_id)))
    print input_

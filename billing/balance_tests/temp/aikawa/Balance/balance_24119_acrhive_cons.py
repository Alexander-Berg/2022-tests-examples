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

dt = datetime.datetime.now()

client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

# service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#                                    service_order_id=service_order_id, params={'ManagerUID': 271344331})
# invoice_list = []
#
# orders_list = [
#     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': dt}
# ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
#
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)
#
# invoice_list.append(invoice_id)
#
# orders_list = [
#     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 15, 'BeginDT': dt}
# ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
#
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)
#
# invoice_list.append(invoice_id)
#
# orders_list = [
#     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': dt}
# ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
#
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)
#
# invoice_list.append(invoice_id)
#
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 20}, 0, dt)
#
# act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
#
# consume_list = []
# for invoice_id in invoice_list:
#     consume_list.append(db.get_consumes_by_invoice(invoice_id))
# pprint.pprint(consume_list)
#
# cons_in_process_cache = db.balance().execute('''select * from t_export where type = 'PROCESS_CACHE' and object_id = :order_id''', {'order_id': order_id})
#
# steps.CommonSteps.wait_for_export('PROCESS_CACHE', order_id)
#
# consume_list = []
# for invoice_id in invoice_list:
#     consume_list.append(db.get_consumes_by_invoice(invoice_id)[0])
# pprint.pprint(consume_list)
#
# archive_value = db.get_consume_by_id(consume_list[0]['id'])
# assert archive_value[0]['archive'] == 1
#
# order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#                                    service_order_id=service_order_id, params={'ManagerUID': 235410028})
#
# consume_list = []
# for invoice_id in invoice_list:
#     consume_list.append(db.get_consumes_by_invoice(invoice_id)[0])
# pprint.pprint(consume_list)
#
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 25}, 0)
#
# consume_list = []
# for invoice_id in invoice_list:
#     consume_list.append(db.get_consumes_by_invoice(invoice_id)[0])
# pprint.pprint(consume_list)

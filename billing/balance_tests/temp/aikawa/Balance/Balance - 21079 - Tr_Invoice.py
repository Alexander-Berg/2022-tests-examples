import datetime

from balance import balance_steps as steps
import btestlib.utils as utils
from temp.igogor.balance_objects import Contexts, Regions

dt = datetime.datetime(2016, 11, 1)
SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1050
PERSON_TYPE = 'tru'


DT = datetime.datetime.now()
HALF_YEAR_AFTER_NOW_ISO = utils.Date.date_to_iso_format(DT + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=180))
YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=365))

client_id = steps.ClientSteps.create({'REGION_ID': Regions.TR.id})
steps.ClientSteps.link(client_id, 'aikawa-test-10')
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, {'email': 'not-hidden@not-hidden.rr'})

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
contract_params = {'CLIENT_ID': client_id,
                   'PERSON_ID': person_id,
                   'DT': YEAR_BEFORE_NOW_ISO,
                   'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                   'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                   'SERVICES': [7],
                   'PAYMENT_TYPE': 3,
                   'FIRM': 8,
                   'PERSONAL_ACCOUNT': 1,
                   'LIFT_CREDIT_ON_PAYMENT': 0,
                   'PERSONAL_ACCOUNT_FICTIVE': 1,
                   }
CONTRACT_TYPE = 'TR_OPT_CLIENT'

contract_id, _ = steps.ContractSteps.create_contract_new(CONTRACT_TYPE, contract_params)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
# steps.CommonSteps.export('OEBS', 'Person', person_id)
# steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
#
# print steps.ActsSteps.generate(client_id, force=1, date=dt)
#
# act_id = db.get_acts_by_client(client_id)[0]['id']
# print act_id
# steps.CommonSteps.export('OEBS', 'Act', act_id)

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, {'email': 'hidden@hidden.rr'})
#
# service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
# orders_list = [
#     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
# ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
#
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
#
# print steps.ActsSteps.generate(client_id, force=1, date=dt)
#
# act_id = db.get_acts_by_client(client_id)[0]['id']
# print act_id
#
# db.balance().execute("UPDATE t_act_internal SET type = 'internal' WHERE ID = {0}".format(act_id))

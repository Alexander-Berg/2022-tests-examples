import datetime
from balance import balance_steps as steps
from btestlib.constants import ContractCommissionType, Currencies, ContractPaymentType
from btestlib import utils as utils

to_iso = utils.Date.date_to_iso_format
dt = datetime.datetime.now()
NOW = datetime.datetime.now()
# NOW = datetime.datetime(2016,8,23)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

PRODUCT_FROM = 506669
PRODUCT_TO = 507901
SERVICE_ID = 70
client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, 'ur')

service_order_id = steps.OrderSteps.next_id(service_id=70)

order_id_507901 = steps.OrderSteps.create(client_id=client_id, product_id=507901, service_id=70,
                                          service_order_id=service_order_id, params={'AgencyID': None})

service_order_id = steps.OrderSteps.next_id(service_id=70)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=506669, service_id=70,
                                   service_order_id=service_order_id, params={'AgencyID': None})

orders_list = [
    {'ServiceID': 70, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params={'InvoiceDesireDT': dt})
contract_type = ContractCommissionType.NO_AGENCY

contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, {'CLIENT_ID': client_id,
                                                                         'PERSON_ID': person_id,
                                                                         'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                         'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'FIRM': 12,
                                                                         'SERVICES': [70]},
                                                         prevent_oebs_export=True)

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1201003,
                                             credit=0, contract_id=contract_id, overdraft=0)

steps.InvoiceSteps.pay(invoice_id)

steps.OrderSteps.transfer(from_orders_list=[{'order_id': order_id, 'qty_old': 100, 'qty_new': 50, 'all_qty': 0}],
                          to_orders_list=[{'order_id': order_id_507901, 'qty_delta': 1}])

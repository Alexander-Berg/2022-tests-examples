from btestlib.constants import Paysyses, PersonTypes, Services, Products, ContractCommissionType, Firms, \
    ContractPaymentType, Regions, Currencies
import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
PRODUCT_ID = 1475
FIRM_ID = Firms.YANDEX_1.id
PAYSYS_ID = 1003
TODAY = datetime.datetime.now()

client_id = steps.ClientSteps.create({'IS_AGENCY': 0})

person_id = steps.PersonSteps.create(client_id, 'ur')


service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id, params={'AgencyID': None})
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': TODAY}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params={'InvoiceDesireDT': TODAY})

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0)
steps.InvoiceSteps.pay(invoice_id)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0, TODAY)
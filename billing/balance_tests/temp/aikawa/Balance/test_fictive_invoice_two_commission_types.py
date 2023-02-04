import datetime

from btestlib import utils as utils

NOW = datetime.datetime.now()
from balance import balance_steps as steps
from btestlib.constants import ContractCommissionType, ContractPaymentType, Currencies

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now() - datetime.timedelta(days=90))
TODAY_ISO = utils.Date.date_to_iso_format(TODAY)
WEEK_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=7))

SERVICE_ID = 7
FIRM_ID = 1
PRODUCT_ID = 508594
PRODUCT_ID_2 = 507529

client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
steps.ClientSteps.link(client_id, 'yb-atst-user-17')
person_id = steps.PersonSteps.create(client_id, 'yt')
contract_type = ContractCommissionType.PR_AGENCY
contract_params_default = {
    'CLIENT_ID': client_id,
    'PERSON_ID': person_id,
    'SERVICES': [SERVICE_ID],
    'DT': NOW,
    'IS_SIGNED': TODAY_ISO,
}
contract_params_default.update({
    'FIRM': FIRM_ID,
    'CURRENCY': Currencies.BYN.num_code,
    'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
    'DEAL_PASSPORT': TODAY_ISO,
    'PAYMENT_TERM': 34,
    'REPAYMENT_ON_CONSUME': 1
})
contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params_default,
                                                         prevent_oebs_export=True)

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id, params={'AgencyID': None})

service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id_2 = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID_2, service_id=SERVICE_ID,
                                     service_order_id=service_order_id_2, params={'AgencyID': None})

orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': NOW},
    # {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id_2, 'Qty': 100, 'BeginDT': NOW},
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params={'InvoiceDesireDT': NOW})
invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1100,
                                             credit=1, contract_id=contract_id, overdraft=0)

print steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 50, 'all_qty': 0}],
                                [{'order_id': order_id_2, 'qty_delta': 1}])

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
                                  {'Money': 40}, 0, NOW)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id_2,
                                  {'Money': 30}, 0, NOW)

# client_id = 81070661
print steps.ActsSteps.enqueue([client_id], force=1, date=NOW)
steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)

import datetime

from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib.constants import ContractCommissionType, Firms, \
    ContractPaymentType, Currencies

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now() - datetime.timedelta(days=60))
TODAY_ISO = utils.Date.date_to_iso_format(TODAY)
WEEK_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=7))

PERSON_TYPE = 'ur'

# SERVICE_ID = 82
# PRODUCT_ID = 507211
# FIRM_ID = Firms.VERTICAL_12.id
# PAYSYS_ID = 1201003

# SERVICE_ID = 7
# PRODUCT_ID = 1475
# FIRM_ID = Firms.YANDEX_1.id
# PAYSYS_ID = 1003


SERVICE_ID = 11
PRODUCT_ID = 2136
FIRM_ID = Firms.MARKET_111.id
PAYSYS_ID = 11101003

client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
person_id = steps.PersonSteps.create(client_id, 'ur')
steps.ClientSteps.link(client_id, 'aikawa-test-10')

dt = datetime.datetime.now() - datetime.timedelta(days=25)

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id, params={'AgencyID': None})
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': TODAY}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params={'InvoiceDesireDT': TODAY})
contract_type = ContractCommissionType.NO_AGENCY
contract_params_default = {
    'CLIENT_ID': client_id,
    'PERSON_ID': person_id,
    'SERVICES': [SERVICE_ID],
    'DT': TODAY_ISO,
    'FINISH_DT': WEEK_AFTER_ISO,
    'IS_SIGNED': TODAY_ISO,
}
contract_params_default.update({
    'FIRM': FIRM_ID,
    'CURRENCY': Currencies.RUB.num_code,
    'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
    'PERSONAL_ACCOUNT': 1,
    'PERSONAL_ACCOUNT_FICTIVE': 0,
    'DEAL_PASSPORT': TODAY_ISO,
    'PAYMENT_TERM': 13
})
contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params_default,
                                                         prevent_oebs_export=True)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=1, contract_id=contract_id, overdraft=0)
steps.InvoiceSteps.pay(invoice_id)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, TODAY)
act_id = steps.ActsSteps.generate(client_id, force=1, date=TODAY)[0]
payment_term_dt = datetime.datetime.now()-datetime.timedelta(days=1)
db.balance().execute('UPDATE t_act_internal SET payment_term_dt = :payment_term_dt WHERE id = :act_id ',
                     {'act_id': act_id, 'payment_term_dt': payment_term_dt})
# # print act_id
# print db.get_act_by_id(act_id)[0]['payment_term_dt']
# # print db.get_act_by_id(act_id)[0]['external_id']
# invoice_id = db.get_act_by_id(act_id)[0]['invoice_id']
# print db.get_invoice_by_id(invoice_id)[0]['external_id'], db.get_act_by_id(act_id)[0]['external_id']
# steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id, act_id=act_id, invoice_id=invoice_id)

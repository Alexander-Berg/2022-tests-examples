import datetime

from balance import balance_steps as steps
from btestlib.constants import Paysyses, PersonTypes, Services, Products, ContractCommissionType, Firms, \
    ContractPaymentType, Regions, Currencies
from btestlib import utils as utils

# print steps.RequestSteps.get_request_choices(request_id=390228647)



TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now())
TODAY_ISO = utils.Date.date_to_iso_format(TODAY)
WEEK_AGO_ISO = utils.Date.date_to_iso_format(TODAY - datetime.timedelta(days=7))
WEEK_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=7))
#

SERVICE_ID = 35
PRODUCT_ID = 507842
FIRM_ID = Firms.MARKET_111.id
# PAYSYS_ID = 1003
#
client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
person_id = steps.PersonSteps.create(client_id, 'yt')
contract_type = ContractCommissionType.NO_AGENCY
contract_params_default = {
    'CLIENT_ID': client_id,
    'PERSON_ID': person_id,
    'SERVICES': [SERVICE_ID],
    'DT': WEEK_AGO_ISO,
    'FINISH_DT': WEEK_AFTER_ISO,
    'IS_SIGNED': TODAY_ISO,
}
contract_params_default.update({
    'FIRM': FIRM_ID,
    'CURRENCY': Currencies.EUR.num_code,
    'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
    'PERSONAL_ACCOUNT': 1,
    'PERSONAL_ACCOUNT_FICTIVE': 0,
    'DEAL_PASSPORT': WEEK_AGO_ISO,
    'PAYMENT_TERM': 25
})
contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params_default)

# service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#                                    service_order_id=service_order_id, params={'AgencyID': None})
# orders_list = [
#     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': TODAY}
# ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#                                        additional_params={'InvoiceDesireDT': TODAY})
# print steps.RequestSteps.get_request_choices(request_id=request_id)
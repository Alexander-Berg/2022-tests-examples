from btestlib.constants import PersonTypes, Paysyses
from balance import balance_steps as steps
import datetime
from btestlib.constants import PersonTypes,ContractPaymentType
from btestlib import utils
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()


client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
# contract_id, _ = steps.ContractSteps.create_contract('no_agency_post',
#                                                      {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
#                                                       'DT': datetime.datetime(2021,07,16),
#                                                       'FINISH_DT': to_iso(datetime.datetime(2021,12,7)),
#                                                       'IS_SIGNED': to_iso(NOW),
#                                                       'SERVICES': [1000],
#                                                       'FIRM': 1000,
#                                                       'CURRENCY': 840,
#                                                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
#                                                       'REPAYMENT_ON_CONSUME': 0,
#                                                       'PERSONAL_ACCOUNT': 1,
#                                                       'LIFT_CREDIT_ON_PAYMENT': 0,
#                                                       'CREDIT_LIMIT_SINGLE': 6000
#                                                       })
orders_list = []
service_order_id = steps.OrderSteps.next_id(1000)
steps.OrderSteps.create(client_id, service_order_id, service_id=1000, product_id=512352)
orders_list.append(
    {'ServiceID': 1000, 'ServiceOrderID': service_order_id, 'Qty': 55, 'BeginDT': datetime.datetime.now()})
request_id = steps.RequestSteps.create(client_id, orders_list, {'InvoiceDesireDT': datetime.datetime.now()})
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id)
                                             # contract_id=contract_id)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id)
# steps.InvoiceSteps.pay_fair(invoice_id)
# steps.CampaignsSteps.do_campaigns(35, service_order_id, {'Bucks': 4, 'Money': 0})
# steps.ActsSteps.generate(client_id)https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=145266773
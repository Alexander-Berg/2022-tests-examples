# coding=utf-8
# Создание кредита (старая схема)
from balance import balance_steps as steps
import datetime
from btestlib.constants import PersonTypes,ContractPaymentType
from btestlib import utils
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, PersonTypes.USP.code)
client_id=1349592359
person_id=13532550
contract_id, _ = steps.ContractSteps.create_contract('usa_comm_post',

                       {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                      'DT': datetime.datetime(2020,11,9),
                                                      # 'FINISH_DT': to_iso(datetime.datetime(2021,3,7)),
                                                      'IS_SIGNED': to_iso(NOW),
                          'SERVICES': [7],
                          'FIRM': 4,
                           'CURRENCY': 840,
                          'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                          'PERSONAL_ACCOUNT': 1,
                           })
orders_list = []
service_order_id = steps.OrderSteps.next_id(7)
steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
orders_list.append(
        {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 55, 'BeginDT':datetime.datetime.now() })
request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=1, fictive=1,
#                                              contract_id=contract_id, overdraft=0, endbuyer_id=None)
# invoice_id = create_invoice(context, credit=1, fictive=1)
#     receipt_sum, consume_sum = get_receipt_sum(invoice_id)
#     utils.check_that(receipt_sum, equal_to(QTY * context.price), u'Проверяем, что есть поступления')
#     utils.check_that(consume_sum, equal_to(QTY * context.price), u'Проверяем, что есть средства на заявках')



# from balance import balance_steps as steps
# import datetime
# from btestlib.constants import PersonTypes, Paysyses
# client_id = steps.ClientSteps.create()
# steps.ClientSteps.set_force_overdraft(client_id, 7, 1000)
# person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
# person_id = steps.PersonSteps.create(client_id, PersonTypes.YTPH.code)
# orders_list = []
# service_order_id = steps.OrderSteps.next_id(7)
# steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
# orders_list.append(
#         {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 55, 'BeginDT':datetime.datetime.now() })
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_PH_RUB.id, overdraft=1)
#
# from balance import balance_steps as steps
# import datetime
# from btestlib.constants import PersonTypes, Paysyses
# # client_id = steps.ClientSteps.create()
# client_id= 1349565485
# # person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
# person_id = 13518029
# orders_list = []
# service_order_id = steps.OrderSteps.next_id(7)
# steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
# orders_list.append(
#         {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 55, 'BeginDT':datetime.datetime.now() })
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_PH_RUB.id)
# steps.InvoiceSteps.pay(invoice_id)
# steps.CampaignsSteps.do_campaigns(7, 59007304,  {'Bucks': 4, 'Money': 0})


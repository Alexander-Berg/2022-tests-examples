# coding=utf-8


# Выставление счета и его оплата
from balance import balance_steps as steps
import datetime
from btestlib.constants import Paysyses
# from btestlib.constants import PersonTypes, Paysyses
# client_id = steps.ClientSteps.create()
# client_id= 1349564467
# # person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
# person_id = 13517446
# orders_list = []
# service_order_id = steps.OrderSteps.next_id(7)
# steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
# orders_list.append(
#         {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 55, 'BeginDT':datetime.datetime.now() })
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id)
# steps.InvoiceSteps.pay(invoice_id)
# steps.CampaignsSteps.do_campaigns(7, 59007304,
#                                   {'Bucks': 4, 'Money': 0})



# Создание овердрафта

from balance import balance_steps as steps
import datetime
from btestlib.constants import PersonTypes
client_id = steps.ClientSteps.create()
steps.ClientSteps.set_force_overdraft(client_id, 7, 1000)
person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
orders_list = []
service_order_id = steps.OrderSteps.next_id(7)
steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
orders_list.append(
        {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 55, 'BeginDT':datetime.datetime.now() })
request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
#                                              contract_id=contract_id, overdraft=100, endbuyer_id=None)







# Создание кредита (новая схема)
# from balance import balance_steps as steps
# import datetime
# from btestlib.constants import PersonTypes, Paysyses
# from btestlib import utils
# to_iso = utils.Date.date_to_iso_format
# NOW = datetime.datetime.now()
# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
# contract_id, _ = steps.ContractSteps.create_contract('no_agency_post',
#                                                      {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
#                                                       'DT': datetime.datetime(2020,11,9),
#                                                       'FINISH_DT': to_iso(datetime.datetime(2021,3,7)),
#                                                       'IS_SIGNED': to_iso(NOW),
#                                                       'SERVICES': [7],
#                                                      # 'COMMISSION_TYPE': 48,
#                                                       # 'NON_RESIDENT_CLIENTS': 0,
#                                                       # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                       'REPAYMENT_ON_CONSUME': 0,
#                                                       'PERSONAL_ACCOUNT': 1,
#                                                       'LIFT_CREDIT_ON_PAYMENT': 0,
#                                                       'PERSONAL_ACCOUNT_FICTIVE': 1,
#                                                       'CREDIT_LIMIT_SINGLE': 6000
#                                                  })
# orders_list = []
# service_order_id = steps.OrderSteps.next_id(7)
# steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
# orders_list.append(
#         {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 5, 'BeginDT':datetime.datetime.now() })
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_PH_RUB.id, credit=1,
#                                               contract_id=contract_id, overdraft=0, endbuyer_id=None)
# steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 2})
# steps.ActsSteps.generate(client_id, date=datetime.datetime(2020,11,17))
# orders_list = []
# service_order_id = steps.OrderSteps.next_id(7)
# steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
# orders_list.append(
#         {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 5, 'BeginDT':datetime.datetime.now() })
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_PH_RUB.id, credit=1,
#                                               contract_id=contract_id, overdraft=0, endbuyer_id=None)
# steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 2})
# steps.ActsSteps.generate(client_id, date=datetime.datetime(2020,12,17))
# invoice_id = create_invoice(context, credit=1, fictive=1)
# receipt_sum, consume_sum = get_receipt_sum(invoice_id)
# utils.check_that(receipt_sum, equal_to(QTY * context.price), u'Проверяем, что есть поступления')
# utils.check_that(consume_sum, equal_to(QTY * context.price), u'Проверяем, что есть средства на заявках')




# Создание кредита (новая схема) через контексты
# from balance import balance_steps as steps
# from btestlib.constants import *
# from datetime import datetime
# from temp.igogor.balance_objects import Contexts
# from btestlib import utils
# to_iso = utils.Date.date_to_iso_format
# NOW = datetime.now()
#
# DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, currency=Currencies.RUB, contract_type=ContractCommissionType.NO_AGENCY)
# client_id, person_id, contract_id, external_contract_id=steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX)
# orders_list = []
# service_order_id = steps.OrderSteps.next_id(7)
# steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
# orders_list.append(
#          {'ServiceID': DIRECT_YANDEX.service.id, 'ServiceOrderID': service_order_id, 'Qty': 5, 'BeginDT':datetime.now() })
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_PH_RUB.id, credit=1,
#                                               contract_id=contract_id, overdraft=0, endbuyer_id=None)
# steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 2})
# steps.ActsSteps.generate(client_id=1349296984, date=datetime(2020,11,17))

# steps.InvoiceSteps.pay(invoice_id=126516559)
# steps.CampaignsSteps.do_campaigns(7, 59799233, {'Bucks': 2})
# steps.ActsSteps.generate(client_id=1349296984,)
















# Создание кредита (старая схема)
# from balance import balance_steps as steps
# import datetime
# from btestlib.constants import PersonTypes
# from btestlib import utils
# to_iso = utils.Date.date_to_iso_format
# NOW = datetime.datetime.now()
# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
# contract_id, _ = steps.ContractSteps.create_contract('no_agency_post',
#                                                      {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
#                                                       'DT': datetime.datetime(2020,11,16),
#                                                       'FINISH_DT': to_iso(datetime.datetime(2021,3,7)),
#                                                       'IS_SIGNED': to_iso(NOW),
#                                                       'SERVICES': [7],
#                                                       # 'COMMISSION_TYPE': 48,
#                                                       # 'NON_RESIDENT_CLIENTS': 0,
#                                                       # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                       'REPAYMENT_ON_CONSUME': 0,
#                                                       'PERSONAL_ACCOUNT': 1,
#                                                       'LIFT_CREDIT_ON_PAYMENT': 0,
#                                                       # 'PERSONAL_ACCOUNT_FICTIVE': 0,
#                                                       'CREDIT_LIMIT_SINGLE': 6000
#                                                       })
# orders_list = []
# service_order_id = steps.OrderSteps.next_id(7)
# steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
# orders_list.append(
#         {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 5, 'BeginDT':datetime.datetime.now() })
# request_id = steps.RequestSteps.create(client_id, orders_list)
#
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id, credit=1,
#                                               contract_id=contract_id, overdraft=0, endbuyer_id=None)
# steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 2})
# steps.ActsSteps.generate(client_id, date=datetime.datetime(2020,11,17))


# import balance.balance_db as db
# from balance import balance_steps as steps
# client_id = steps.ClientSteps.create()
# query1 = "SELECT PHONE FROM t_client WHERE id =:id"
# params1 = {'id': client_id}
# result1 = db.balance().execute(query1, params1)
# steps.ClientSteps.create({'CLIENT_ID':client_id, 'PHONE': '444555'})
# query2 = "SELECT PHONE FROM t_client WHERE id =:id"
# params2 = {'id': client_id}
# result2 = db.balance().execute(query2, params2)
# assert result1!=result2
# print('изменился')

# query = "UPDATE PHONE set '444555' FROM t_client WHERE id =:id"
# params = {'id': client_id}
# result = db.balance().execute(query, params)


# query2 = "update t_client set PHONE='444555' WHERE ID =:client_id"
# params2 = {'client_id': client_id}
# result2 = db.balance().execute(query2, params2)


# -*- coding: utf-8 -*-

from datetime import timedelta, datetime
from decimal import Decimal as D
import copy
from balance import balance_api as api

import pytest

from balance import balance_steps as steps
from btestlib import utils
import balance.balance_db as db
from temp.igogor.balance_objects import Contexts
from btestlib.constants import PersonTypes, Services, Paysyses, Products, ContractPaymentType, \
    Currencies, ContractCreditType, ContractCommissionType, Firms, User
import balance.balance_db as db
from btestlib.data.defaults import Date
from balance.real_builders import common_defaults
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT
from dateutil.relativedelta import relativedelta



client_id = steps.ClientSteps.create()

steps.ClientSteps.link(client_id, 'yndx-balance-assessor-100')

person_ph_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code,
                                        {'fname': u'тестовое',
                                         'lname': u'физ',
                                         'mname': u'лицо',
                                         'email': 'test-ph@balance.ru'
                                         })
person_ur_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                                        {'name': u'тестовое юр лицо 1',
                                         'email': 'test-ur-2@balance.ru',
                                         'inn': '7865109488',
                                         'postaddress': u'Льва Толстого, 16',
                                         'postcode': '119021'
                                         })

DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                     contract_type=ContractCommissionType.NO_AGENCY)
GEO = Contexts.GEO_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                     contract_type=ContractCommissionType.NO_AGENCY)
_, _, contract_id, _ = steps.ContractSteps. \
    create_general_contract_by_context(DIRECT_YANDEX, postpay=True, client_id=client_id,
                                       person_id=person_ur_id,
                                       start_dt=datetime(2021, 1, 1),
                                       additional_params={'EXTERNAL_ID': 'договор директ предоплата'})

### ПРЕДОПЛАТА ГЕО
# счет 1 предоплата без договора, неоплаченный
service_order_id_1 = steps.OrderSteps.next_id(Services.DIRECT.id)
steps.OrderSteps.create(client_id, service_order_id_1, product_id=Products.DIRECT_FISH.id,
                        service_id=Services.DIRECT.id)
orders_list_1 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_1, 'Qty': 35}]
request_id_1 = steps.RequestSteps.create(client_id, orders_list_1,
                                         additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 1)))
invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id_1, person_ph_id, Paysyses.CC_PH_RUB.id)

# счет 2 предоплата без договора, недоплаченный
service_order_id_2 = steps.OrderSteps.next_id(Services.DIRECT.id)
steps.OrderSteps.create(client_id, service_order_id_2, product_id=Products.DIRECT_FISH.id,
                        service_id=Services.DIRECT.id)
orders_list_2 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_2, 'Qty': 40}]
request_id_2 = steps.RequestSteps.create(client_id, orders_list_2,
                                         additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 2)))
invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_ur_id, Paysyses.BANK_UR_RUB.id)
steps.InvoiceSteps.pay(invoice_id_2, payment_sum=D(50), payment_dt=datetime(2021, 1, 3))

# счет 3 предоплата без договора, оплаченный
service_order_id_3 = steps.OrderSteps.next_id(Services.DIRECT.id)
steps.OrderSteps.create(client_id, service_order_id_3, product_id=Products.DIRECT_FISH.id,
                        service_id=Services.DIRECT.id)
orders_list_3 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_3, 'Qty': 45}]
request_id_3 = steps.RequestSteps.create(client_id, orders_list_3,
                                         additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 2)))
invoice_id_3, _, _ = steps.InvoiceSteps.create(request_id_3, person_ph_id, Paysyses.BANK_PH_RUB.id)
steps.InvoiceSteps.pay(invoice_id_3, payment_dt=datetime(2021, 1, 4))

# счет 4 предоплата без договора, переплаченный
service_order_id_4 = steps.OrderSteps.next_id(Services.DIRECT.id)
steps.OrderSteps.create(client_id, service_order_id_4, product_id=Products.DIRECT_FISH.id,
                        service_id=Services.DIRECT.id)
orders_list_4 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_4, 'Qty': 50}]
request_id_4 = steps.RequestSteps.create(client_id, orders_list_4,
                                         additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 2)))
invoice_id_4, _, _ = steps.InvoiceSteps.create(request_id_4, person_ph_id, Paysyses.BANK_PH_RUB.id)
steps.InvoiceSteps.pay(invoice_id_4, payment_sum=D(100500), payment_dt=datetime(2021, 1, 4))

# счет 5 предоплата с договором, оплаченный, частично открученный, заакченный
service_order_id_5 = steps.OrderSteps.next_id(Services.DIRECT.id)
steps.OrderSteps.create(client_id, service_order_id_5, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
orders_list_5 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_5, 'Qty': 35}]
request_id_5 = steps.RequestSteps.create(client_id, orders_list_5,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 6)))
invoice_id_5, _, _ = steps.InvoiceSteps.create(request_id_5, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                   contract_id=contract_id)
steps.InvoiceSteps.pay(invoice_id_5, payment_dt=datetime(2021, 1, 6))
steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_5, {'Bucks': 20}, 0,
                                      campaigns_dt=datetime(2021, 1, 6))

# счет 6 предоплата с договором, оплаченный, полностью открученный, заакченный
service_order_id_6 = steps.OrderSteps.next_id(Services.DIRECT.id)
steps.OrderSteps.create(client_id, service_order_id_6, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
orders_list_6 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_6, 'Qty': 15}]
request_id_6 = steps.RequestSteps.create(client_id, orders_list_6,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 6)))
invoice_id_6, _, _ = steps.InvoiceSteps.create(request_id_6, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                   contract_id=contract_id)
steps.InvoiceSteps.pay(invoice_id_6, payment_dt=datetime(2021, 1, 6))
steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_6, {'Bucks': 15}, 0,
                                      campaigns_dt=datetime(2021, 1, 6))

steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 1, 31))



### ОВЕРДРАФТ И ПРЕДОПЛАТА МАРКЕТ
steps.OverdraftSteps.set_force_overdraft(client_id, Services.MARKET.id, 1000000, firm_id=Firms.MARKET_111.id)
# счет 7 предоплата, оплаченный, частично открученный, заакченный, Маркет
service_order_id_7 = steps.OrderSteps.next_id(Services.MARKET.id)
steps.OrderSteps.create(client_id, service_order_id_7, product_id=Products.MARKET.id, service_id=Services.MARKET.id)
orders_list_7 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_7, 'Qty': 10}]
request_id_7 = steps.RequestSteps.create(client_id, orders_list_7,
                                         additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 1)))
invoice_id_7, _, _ = steps.InvoiceSteps.create(request_id_7, person_ur_id, Paysyses.BANK_UR_RUB.id)
steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_7, {'Bucks': 8}, 0,
                                  campaigns_dt=datetime(2021, 2, 1))

# счет 8 овердрафт, оплаченный, частично открученный, заакченный, Маркет
service_order_id_8 = steps.OrderSteps.next_id(Services.MARKET.id)
steps.OrderSteps.create(client_id, service_order_id_8, product_id=Products.MARKET.id,
                        service_id=Services.MARKET.id)
orders_list_8 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_8, 'Qty': 15}]
request_id_8 = steps.RequestSteps.create(client_id, orders_list_8,
                                         additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 2)))
invoice_id_8, _, _ = steps.InvoiceSteps.create(request_id_8, person_ph_id, Paysyses.BANK_PH_RUB.id, overdraft=1)
steps.InvoiceSteps.pay(invoice_id_8)
steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_8, {'Bucks': 1}, 0,
                                  campaigns_dt=datetime(2021, 2, 2))

# счет 10 овердрафт, почти просроченный, частично открученный, заакченный, Маркет
service_order_id_10 = steps.OrderSteps.next_id(Services.MARKET.id)
steps.OrderSteps.create(client_id, service_order_id_10, product_id=Products.MARKET.id,
                        service_id=Services.MARKET.id)
orders_list_10 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_10, 'Qty': 15}]
request_id_10 = steps.RequestSteps.create(client_id, orders_list_10,
                                         additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 3)))
invoice_id_10, _, _ = steps.InvoiceSteps.create(request_id_10, person_ph_id, Paysyses.BANK_PH_RUB.id, overdraft=1)
steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_10, {'Bucks': 1}, 0,
                                  campaigns_dt=datetime(2021, 2, 3))
steps.InvoiceSteps.set_payment_term_dt(invoice_id=invoice_id_10, dt=datetime.today() + relativedelta(days=2))

# счет 11 овердрафт, непросроченный, частично открученный, заакченный, Маркет
service_order_id_11 = steps.OrderSteps.next_id(Services.MARKET.id)
steps.OrderSteps.create(client_id, service_order_id_11, product_id=Products.MARKET.id,
                        service_id=Services.MARKET.id)
orders_list_11 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_11, 'Qty': 15}]
request_id_11 = steps.RequestSteps.create(client_id, orders_list_11,
                                         additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 4)))
invoice_id_11, _, _ = steps.InvoiceSteps.create(request_id_11, person_ph_id, Paysyses.BANK_PH_RUB.id, overdraft=1)
steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_11, {'Bucks': 1}, 0,
                                  campaigns_dt=datetime(2021, 2, 4))
steps.InvoiceSteps.set_payment_term_dt(invoice_id=invoice_id_11, dt=datetime.today() + relativedelta(months=2))


# счет 9 овердрафт, просроченный, частично открученный, заакченный, Маркет
service_order_id_9 = steps.OrderSteps.next_id(Services.MARKET.id)
steps.OrderSteps.create(client_id, service_order_id_9, product_id=Products.MARKET.id,
                        service_id=Services.MARKET.id)
orders_list_9 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_9, 'Qty': 15}]
request_id_9 = steps.RequestSteps.create(client_id, orders_list_9,
                                         additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 5)))
invoice_id_9, _, _ = steps.InvoiceSteps.create(request_id_9, person_ph_id, Paysyses.BANK_PH_RUB.id, overdraft=1)
steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_9, {'Bucks': 1}, 0,
                                  campaigns_dt=datetime(2021, 2, 5))

steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 2, 28))

## ГЕО ПОСТОПЛАТА
person_ur_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                                        {'name': u'тестовое юр лицо 2',
                                         'email': 'test-ur-22@balance.ru',
                                         'inn': '7865109488',
                                         'postaddress': u'Льва Толстого, 16',
                                         'postcode': '119021'
                                         })

_, _, contract_id, _ = steps.ContractSteps. \
    create_general_contract_by_context(GEO, postpay=True, client_id=client_id,
                                       person_id=person_ur_id,
                                       start_dt=datetime(2021, 3, 1),
                                       additional_params={'EXTERNAL_ID': 'договор гео постоплата'})


service_order_id_12 = steps.OrderSteps.next_id(Services.GEO.id)
steps.OrderSteps.create(client_id, service_order_id_12, product_id=Products.GEO.id,
                            service_id=Services.GEO.id)
orders_list_12 = [{'ServiceID': Services.GEO.id, 'ServiceOrderID': service_order_id_12, 'Qty': 40}]
request_id_12 = steps.RequestSteps.create(client_id, orders_list_12,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 3, 6)))
invoice_id_12, _, _ = steps.InvoiceSteps.create(request_id_12, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                   contract_id=contract_id, credit=True)
steps.InvoiceSteps.pay(invoice_id_12, payment_dt=datetime(2021, 3, 6))
steps.CampaignsSteps.do_campaigns(Services.GEO.id, service_order_id_12, {'Days': 20}, 0,
                                      campaigns_dt=datetime(2021, 3, 6))


service_order_id_13 = steps.OrderSteps.next_id(Services.GEO.id)
steps.OrderSteps.create(client_id, service_order_id_13, product_id=Products.GEO.id,
                            service_id=Services.GEO.id)
orders_list_13 = [{'ServiceID': Services.GEO.id, 'ServiceOrderID': service_order_id_13, 'Qty': 4}]
request_id_13 = steps.RequestSteps.create(client_id, orders_list_13,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 3, 6)))
invoice_id_13, _, _ = steps.InvoiceSteps.create(request_id_13, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                   contract_id=contract_id, credit=True)
steps.InvoiceSteps.pay(invoice_id_13, payment_dt=datetime(2021, 3, 6))
steps.CampaignsSteps.do_campaigns(Services.GEO.id, service_order_id_13, {'Days': 4}, 0,
                                      campaigns_dt=datetime(2021, 3, 6))

steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))



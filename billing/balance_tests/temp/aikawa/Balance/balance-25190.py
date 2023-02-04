# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils

dt = datetime.datetime.now()

PAYSYS_ID = 2501020
SERVICE_ID = 7
PRODUCT_ID = 1475


# client_id = steps.ClientSteps.create()
client_id = 38953656
person_id = steps.PersonSteps.create(client_id, 'kzu', {'person_id': '5184152', 'rnn': '', "kbe": '17', 'kz-in': '123456789014'})
# person_id = steps.PersonSteps.create(client_id, 'kzu', {'rnn': '123456789012', "kbe": '17', 'kz-in': '123456789014'})
# person_id = steps.PersonSteps.create(client_id, 'kzu', {'rnn': '123456789012', 'kz-in': '123456789014'})
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id, params={'AgencyID': None})
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay_fair(invoice_id)

steps.CommonSteps.export('OEBS', 'Person', person_id)
steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)


# person_id_2 = steps.PersonSteps.create(client_id, 'kzp', {'person_id': '5184153', 'kz-in': '123456789015'})
person_id_2 = steps.PersonSteps.create(client_id, 'kzp', {'kz-in': '123456789015'})
invoice_id2, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_2, paysys_id=2501021,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay_fair(invoice_id2)

steps.CommonSteps.export('OEBS', 'Person', person_id_2)
steps.CommonSteps.export('OEBS', 'Invoice', invoice_id2)

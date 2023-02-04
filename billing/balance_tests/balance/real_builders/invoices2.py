# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from balance import balance_steps as steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

context = Contexts.DIRECT_FISH_RUB_CONTEXT
QTY = D('250')
COMPLETIONS = D('99.99')

client_id = None or steps.ClientSteps.create(params={'phone': '13131313'})
agency_id = None

# Далее в скрипте будут фигурировать "владелец счёта": агентство или клиент и "владелей заказа": всегда клиент:
order_owner = client_id
invoice_owner = agency_id or client_id

# Создаём плательщика
person_params = {'phone': '1313131313'}
person_id = None or steps.PersonSteps.create(invoice_owner, 'ur', person_params)

# Создаём договор:
contract_id = None

# Создаём список заказов:
orders_list = []

for _ in xrange(1):
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id, product_id=context.product.id,
                            params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid})
    orders_list.append(
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': ORDER_DT})

# Создаём риквест
request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=INVOICE_DT))

print client_id
import sys
sys.exit(0)
# print '98329215'
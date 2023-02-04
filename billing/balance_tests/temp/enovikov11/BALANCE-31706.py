# -*- coding: utf-8 -*-

import datetime
import time
from decimal import Decimal as D

from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils
from btestlib.constants import Services, PersonTypes
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

client_id = steps.ClientSteps.create()
steps.ClientSteps.link(client_id, 'yb-dev-user-29')

service_order_id = steps.OrderSteps.next_id(7)
steps.OrderSteps.create(client_id, service_order_id, 1475, 7)
orders_list = [
    {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 50}]
request_id = steps.RequestSteps.create(client_id, orders_list)

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

# Создаём клиента
client_id = None or steps.ClientSteps.create(params=None)

person_id = None or steps.PersonSteps.create(client_id, context.person_type.code, {})
person_id = None or steps.PersonSteps.create(client_id, context.person_type.code, {})

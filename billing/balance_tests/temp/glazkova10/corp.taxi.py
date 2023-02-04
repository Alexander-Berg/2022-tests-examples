from btestlib.constants import PersonTypes, Paysyses
from balance import balance_steps as steps
import datetime
from btestlib.constants import PersonTypes,ContractPaymentType
from btestlib import utils
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
from btestlib.data.partner_contexts import FOOD_RESTAURANT_KZ_CONTEXT
NOW = datetime.datetime.now()


import json

import attr

from balance.balance_objects import Context
from btestlib.constants import *
from btestlib.data.defaults import *


# client_id = steps.ClientSteps.create()
# steps.ClientSteps.set_force_overdraft(client_id, 7, 1000)
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
client_id, person_id, contract_id, _ = \
steps.ContractSteps.create_partner_contract(FOOD_RESTAURANT_KZ_CONTEXT, is_offer=True)
orders_list = []
service_order_id = steps.OrderSteps.next_id(628)
steps.OrderSteps.create(client_id, service_order_id, service_id=628, product_id=510745)
orders_list.append(
    {'ServiceID': 628, 'ServiceOrderID': service_order_id, 'Qty': 55, 'BeginDT': datetime.now()})
request_id = steps.RequestSteps.create(client_id, orders_list, {'InvoiceDesireDT': datetime.now(), 'InvoiceDesireType': 'charge_note'})
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_KZ_UR_WO_NDS.id, contract_id=contract_id)
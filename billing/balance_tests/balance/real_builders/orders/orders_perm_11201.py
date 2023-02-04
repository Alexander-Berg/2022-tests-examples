# -*- coding: utf-8 -*-

from . import steps
from balance import balance_steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms
from jsonrpc import dispatcher
from .. import common_defaults


# батч 9999999, добавляем клиента в батч, фирма 1, без счета
def test_client_in_batch_order():
    client_id, _, _, _, service_id, service_order_id, _, _, _ = steps.create_base_request()
    balance_steps.ClientSteps.insert_client_into_batch(client_id)
    return client_id, service_id, service_order_id


# батч 9999999, клиент не в батче, фирма 1, без счета
@dispatcher.add_method
def test_client_not_in_batch_order():
    client_id, _, _, _, service_id, service_order_id, _, _, _ = steps.create_base_request()
    return client_id, service_id, service_order_id


# батч 9999999, добавляем клиента в батч, фирма 1, есть включенный счет
def test_client_and_firm_in_constr_order_with_invoice():
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = steps.create_base_request()
    invoice_id = steps.create_base_invoice(request_id, person_id, turn_on=True)
    balance_steps.ClientSteps.insert_client_into_batch(client_id)
    return client_id, service_id, service_order_id


# батч 9999999, добавляем клиента в батч, фирма 111 (не в ограничении), есть включенный счет
def test_client_in_constr_order_with_invoice():
    context = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111, person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    invoice_id = steps.create_base_invoice(request_id, person_id, turn_on=True)
    balance_steps.ClientSteps.insert_client_into_batch(client_id)
    return client_id, service_id, service_order_id


# батч 9999999, клиент не в батче, фирма 1, есть включенный счет
def test_firm_in_constr_order_with_invoice():
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = steps.create_base_request()
    invoice_id = steps.create_base_invoice(request_id, person_id, turn_on=True)
    return client_id, service_id, service_order_id


# батч 9999999, клиент не в батче, фирма 111 (не в ограничении), есть включенный счет
def test_client_and_firm_not_in_constr_order_with_invoice():
    context = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111, person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, service_id, service_order_id, _, _, request_id = \
        steps.create_base_request(context=context)
    invoice_id = steps.create_base_invoice(request_id, person_id, turn_on=True)
    return client_id, service_id, service_order_id

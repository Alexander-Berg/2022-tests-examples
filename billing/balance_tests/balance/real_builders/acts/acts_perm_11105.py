# -*- coding: utf-8 -*-

from . import steps
from balance import balance_steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms
from .. import common_defaults


# батч 9999999, добавляем клиента в батч, фирма 1
def test_client_and_firm_in_constr_act():
    client_id, _, _, _, _, external_id, _ = steps.create_base_act()
    balance_steps.ClientSteps.insert_client_into_batch(client_id)
    return client_id, external_id


# батч 9999999, добавляем клиента в батч, фирма 111 (не в ограничении)
def test_client_in_constr_act():
    context = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111,
                                              person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, _, _, _, external_id, _ = steps.create_base_act(context=context)
    balance_steps.ClientSteps.insert_client_into_batch(client_id)
    return client_id, external_id


# батч 9999999, клиент не в батче, фирма 1
def test_firm_in_constr_act():
    client_id, _, _, _, _, external_id, _ = steps.create_base_act()
    return client_id, external_id


# батч 9999999, клиент не в батче, фирма 111 (не в ограничении), есть включенный счет
def test_client_and_firm_not_in_constr_act():
    context = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111,
                                              person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, _, _, _, external_id, _ =steps. create_base_act(context=context)
    return client_id, external_id

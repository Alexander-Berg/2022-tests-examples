# -*- coding: utf-8 -*-

from . import steps
from balance import balance_steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms
from .. import common_defaults
from jsonrpc import dispatcher

CONTEXT = steps.CONTEXT


# акты на директ и маркет у одного клиента
@dispatcher.add_method
def test_hidden_and_not_hidden_person_act():
    client_id, person_id, _, _, act_id_direct, external_id_direct, _ = steps.create_base_act()
    balance_steps.PersonSteps.hide_person(person_id)
    context = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111,
                                              person_params=common_defaults.FIXED_UR_PARAMS)
    _, _, _, _, act_id_market, external_id_market, _ = steps.create_base_act(context=context, client_id=client_id,
                                                                             act_num=1)
    return client_id, act_id_direct, external_id_direct, act_id_market, external_id_market

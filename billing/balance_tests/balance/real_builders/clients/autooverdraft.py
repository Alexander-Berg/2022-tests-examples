# -*- coding: utf-8 -*-

from . import steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms
from jsonrpc import dispatcher
from .. import common_defaults


CONTEXT = steps.CONTEXT

@dispatcher.add_method
def test_direct_firm_1_autooverdraft():
    client_id, person_id = steps.create_client_and_person()
    steps.set_autooverdraft(client_id, person_id)
    return client_id, person_id

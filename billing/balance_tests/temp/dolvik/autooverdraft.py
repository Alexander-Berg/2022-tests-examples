# -*- coding: utf-8 -*-

from balance.real_builders.clients import steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms
from jsonrpc import dispatcher
from balance.real_builders import common_defaults

CONTEXT = steps.CONTEXT

client_id, person_id = steps.create_client_and_person()
steps.set_autooverdraft(client_id, person_id)
2 + 2

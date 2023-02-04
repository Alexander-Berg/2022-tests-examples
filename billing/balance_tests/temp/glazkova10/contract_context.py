from balance import balance_steps as steps
from btestlib.constants import *
import datetime
from temp.igogor.balance_objects import Contexts
import json

import attr

from balance.balance_objects import Context
from btestlib.constants import *
from btestlib.data.defaults import *

from btestlib import utils
from balance import balance_steps as steps
to_iso = utils.Date.date_to_iso_format
from temp.igogor.balance_objects import Contexts
from btestlib.constants import *
from btestlib.data.defaults import *
from btestlib.data.partner_contexts import LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL


# DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, currency=Currencies.RUB, contract_type=ContractCommissionType.NO_AGENCY)
# client_id, person_id, contract_id, external_contract_id=steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX)



corp_client_id, corp_person_id, corp_contract_id, _ = \
steps.ContractSteps.create_partner_contract(LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL,
                                            additional_params={'start_dt': to_iso(datetime(2021,12,7))})
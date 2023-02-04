# -*- coding: utf-8 -*-

import hamcrest

import json
import os
import balance.balance_steps as steps
import btestlib.config as balance_config
from balance.snout_steps import InvoiceSteps
from btestlib.constants import Roles

try:
    import balance_contracts
    from balance_contracts.contract_utils import utils as contract_utils
    from balance_contracts.contract_utils import deep_equals
    json_contracts_repo_path = os.path.dirname(os.path.abspath(balance_contracts.__file__))
except ImportError as err:
    json_contracts_repo_path = ''

JSON_SNOUT_PATH = '/snout_contracts_json/'


def check_json_contract(actual_json_data, path, json_file, replace_mask):
    steps.ExportSteps.log_json_contract_actions(json_contracts_repo_path,
                                                JSON_SNOUT_PATH + path,
                                                json_file,
                                                balance_config.FIX_CURRENT_JSON_CONTRACT)

    contract_utils.process_json_contract(json_contracts_repo_path,
                                         JSON_SNOUT_PATH + path,
                                         json_file,
                                         actual_json_data,
                                         replace_mask,
                                         balance_config.FIX_CURRENT_JSON_CONTRACT)


def get_snout_invoice_json(client_id, invoice_id, roles=[Roles.ADMIN_0]):
    session = InvoiceSteps.get_session(client_id, required_roles=roles)
    js_snout = json.loads(InvoiceSteps.get_invoice_data(session, invoice_id).content)
    return js_snout


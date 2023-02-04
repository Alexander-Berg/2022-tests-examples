# -*- coding: utf-8 -*-

from balance.real_builders.invoices import invoices
from btestlib.constants import Roles
from balance.snout_contract.snout_contract_utils import get_snout_invoice_json, check_json_contract

try:
    from balance_contracts.snout_contracts_json.invoice import overdraft_overpaid_PH_docs_extra_replace_mask \
        as replace_mask
except ImportError as err:
    json_contracts_repo_path = ''

PATH = 'invoice/'


# TODO: добавить проверку payment_term
def test_overpaid_overdraft_invoice_docs_extra():
    client_id, invoice_id, _, _ = invoices.test_overdraft_overpaid_invoice()
    snout_json = get_snout_invoice_json(client_id, invoice_id, roles=[Roles.DOCS_EXTRA_5])
    check_json_contract(snout_json, PATH, 'overdraft_overpaid_PH_docs_extra.json', replace_mask)



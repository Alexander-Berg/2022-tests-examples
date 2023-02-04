# -*- coding: utf-8 -*-
from balance.real_builders.invoices import invoices
from balance.snout_contract.snout_contract_utils import get_snout_invoice_json, check_json_contract

try:
    from balance_contracts.snout_contracts_json.invoice import fictive_overpaid_PH_admin_replace_mask as replace_mask
except ImportError as err:
    json_contracts_repo_path = ''

PATH = 'invoice/'


def test_fictive_overpaid_invoice():
    client_id, fictive_invoice_id, _ = invoices.test_credit_overpaid_invoice()
    snout_json = get_snout_invoice_json(client_id, fictive_invoice_id)
    check_json_contract(snout_json, PATH, 'fictive_overpaid_PH_admin.json', replace_mask)


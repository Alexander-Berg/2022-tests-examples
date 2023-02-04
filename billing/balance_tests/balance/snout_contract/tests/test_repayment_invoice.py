# -*- coding: utf-8 -*-

from balance.real_builders.invoices import invoices
from btestlib.constants import Roles
from balance.snout_contract.snout_contract_utils import get_snout_invoice_json, check_json_contract

try:
    from balance_contracts.snout_contracts_json.invoice import repayment_overpaid_PH_admin_replace_mask as replace_mask
except ImportError as err:
    json_contracts_repo_path = ''

PATH = 'invoice/'


def test_repayment_overpaid_invoice():
    client_id, _, invoice_id = invoices.test_credit_overpaid_invoice()
    snout_json = get_snout_invoice_json(client_id, invoice_id, roles=[Roles.ADMIN_0])
    check_json_contract(snout_json, PATH, 'repayment_overpaid_PH_admin.json', replace_mask)



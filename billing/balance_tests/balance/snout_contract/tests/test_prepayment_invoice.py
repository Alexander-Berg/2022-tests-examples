# -*- coding: utf-8 -*-
from balance.real_builders.invoices import invoices
from btestlib.constants import Roles
from balance.snout_contract.snout_contract_utils import get_snout_invoice_json, check_json_contract

try:
    from balance_contracts.snout_contracts_json.invoice import prepayment_overdpaid_PH_support_replace_mask \
        as replace_mask_overpaid
    from balance_contracts.snout_contracts_json.invoice import prepayment_underpaid_PH_admin_replace_mask \
        as replace_mask_underpaid
except ImportError as err:
    json_contracts_repo_path = ''

PATH = 'invoice/'


# замена ru.yandex.autotests.balance.tests.paystep.paystepRules.freeFundsButton.ai.BaseInvUnderpaymentAdminTest
def test_underpaid_invoice_admin():
    client_id, invoice_id, _ = invoices.test_prepayment_underpaid_invoice()
    snout_json = get_snout_invoice_json(client_id, invoice_id, roles=[Roles.ADMIN_0])
    check_json_contract(snout_json, PATH, 'prepayment_underpaid_PH_admin.json', replace_mask_underpaid)


# замена ru.yandex.autotests.balance.tests.paystep.paystepRules.freeFundsButton.ai.BaseInvOverpaymentSupportTest
def test_overpaid_invoice_support():
    client_id, invoice_id, _ = invoices.test_prepayment_overpaid_invoice()
    snout_json = get_snout_invoice_json(client_id, invoice_id, roles=[Roles.SUPPORT_17])
    check_json_contract(snout_json, PATH, 'prepayment_overdpaid_PH_support.json', replace_mask_overpaid)






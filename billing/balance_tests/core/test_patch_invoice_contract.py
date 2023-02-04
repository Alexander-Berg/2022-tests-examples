# -*- coding: utf-8 -*-

import pytest

from balance.constants import PermissionCode
from balance import exc

from tests.balance_tests.core.core_common import _init_invoice, _create_contract, _create_person
from tests.balance_tests.payment.common import create_role, create_passport


@pytest.fixture(autouse=True)
def _create_passport(session):
    role = create_role(session, PermissionCode.PATCH_INVOICE_CONTRACT)
    create_passport(session, role)


def test_matching_contract(session, core_obj, client, paysys):
    invoice = _init_invoice(session, client, paysys)
    contract = _create_contract(session, invoice.person)

    core_obj.patch_invoice_contract(invoice.id, contract.id)

    assert invoice.contract == contract


def test_illegal_contract(session, core_obj, client, paysys):
    invoice = _init_invoice(session, client, paysys)
    person = _create_person(client)
    contract = _create_contract(session, person)

    with pytest.raises(exc.ILLEGAL_CONTRACT):
        core_obj.patch_invoice_contract(invoice.id, contract.id)

    assert invoice.contract is None

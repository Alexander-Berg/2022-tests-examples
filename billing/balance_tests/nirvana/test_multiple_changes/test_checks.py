# -*- coding: utf-8 -*-

import pytest

from billing.contract_iface.cmeta import general
from balance.actions.nirvana.operations.multiple_changes.checker import *
from tests import object_builder as ob


@pytest.mark.parametrize(
    'is_cancelled, is_ok',
    [
         (dt.datetime.now() - dt.timedelta(1), False),
         (None, True),
    ]
)
def test_contract_cancelled(session, is_cancelled, is_ok):
    contract = ob.ContractBuilder.construct(session, is_cancelled=is_cancelled)
    assert ContractNotCancelledChecker.check(session, contract, None) is is_ok


@pytest.mark.parametrize(
    'finish_dt, is_ok',
    [
        (dt.datetime.now() - dt.timedelta(1), False),
        (dt.datetime.now() + dt.timedelta(1), True),
        (None, True),
    ]
)
def test_contract_active(session, finish_dt, is_ok):
    contract = ob.ContractBuilder.construct(session, finish_dt=finish_dt)
    assert ContractActiveChecker.check(session, contract, None) is is_ok


@pytest.mark.parametrize(
    'is_signed, is_ok',
    [
        (dt.datetime.now() - dt.timedelta(1), False),
        (None, True)
    ]
)
def test_contract_not_signed(session, is_signed, is_ok):
    contract = ob.ContractBuilder.construct(session, is_signed=is_signed)
    assert ContractNotSignedChecker.check(session, contract, None) is is_ok


@pytest.mark.parametrize(
    'ctype, edt_attr, is_ok',
    [
        ('GENERAL', 'finish_dt', True),
        ('GENERAL', 'end_dt', False),
        ('SPENDABLE', 'end_dt', True),
        ('SPENDABLE', 'finish_dt', False),
        ('GENERAL', None, True),
        ('SPENDABLE', None, True),
    ]
)
def test_valid_edt(session, ctype, edt_attr, is_ok):
    contract = ob.ContractBuilder.construct(session, ctype=ctype)
    attrs_to_change = {}
    if edt_attr:
        attrs_to_change[edt_attr] = dt.datetime.now()
    assert ValidEndDtChecker.check(session, contract.col0, attrs_to_change) is is_ok


@pytest.mark.parametrize(
    'is_cancelled, is_ok',
    [
         (dt.datetime.now() - dt.timedelta(1), False),
         (None, True),
    ]
)
def test_collateral_cancelled(session, is_cancelled, is_ok):
    contract = ob.ContractBuilder.construct(session)
    collateral = ob.CollateralBuilder.construct(session, contract=contract, num='01',
                                                is_cancelled=is_cancelled)
    assert CollateralNotCancelledChecker.check(session, collateral, None) is is_ok


@pytest.mark.parametrize(
    'is_signed, is_ok',
    [
         (dt.datetime.now() - dt.timedelta(1), True),
         (None, False),
    ]
)
def test_collateral_signed(session, is_signed, is_ok):
    contract = ob.ContractBuilder.construct(session)
    collateral = ob.CollateralBuilder.construct(session, contract=contract, num='01',
                                                is_signed=is_signed)
    assert CollateralSignedChecker.check(session, collateral, None) is is_ok


@pytest.mark.parametrize(
    'is_signed, is_ok',
    [
         (dt.datetime.now() - dt.timedelta(1), False),
         (None, True),
    ]
)
def test_collateral_not_signed(session, is_signed, is_ok):
    contract = ob.ContractBuilder.construct(session)
    collateral = ob.CollateralBuilder.construct(session, contract=contract, num='01',
                                                is_signed=is_signed)
    assert CollateralNotSignedChecker.check(session, collateral, None) is is_ok


@pytest.mark.parametrize(
    'is_signed, is_ok',
    [
        (dt.datetime.now() - dt.timedelta(1), True),
        (dt.datetime.now() + dt.timedelta(1), False),
    ]
)
def test_contract_rules(session, is_signed, is_ok):
    contract = ob.ContractBuilder.construct(session, is_signed=is_signed)
    assert ContractRulesChecker.check(session, contract.col0, None) is is_ok


@pytest.mark.parametrize(
    'receipt, hide, is_ok',
    [
        (False, False, True),
        (False, True, True),
        (True, False, True),
        (True, True, False),
    ]
)
def test_invoice_can_be_hidden(session, receipt, hide, is_ok):
    invoice = ob.InvoiceBuilder.construct(session)
    if receipt:
        invoice.create_receipt(42)

    assert InvoiceCanBeHiddenChecker.check(session, invoice, {'hidden': 2} if hide else {}) is is_ok


@pytest.mark.parametrize(
    'contract_type, is_ok',
    [
        ('GENERAL', True),
        ('SPENDABLE', False)
    ]
)
def test_proper_contract_type(session, contract_type, is_ok):
    contract = ob.ContractBuilder.construct(session)
    assert ProperContractTypeChecker.check(session, contract,
                                           {'contract_type': contract_type}) is is_ok


@pytest.mark.parametrize(
    'col_dt, col_type, is_ok',
    [
        (dt.datetime.now(), general.collateral_types[1003], True),
        (dt.datetime.now(), general.collateral_types[90], False)
    ]
)
def test_no_future_collaterals(session, col_dt, col_type, is_ok):
    contract = ob.ContractBuilder.construct(session)
    ob.CollateralBuilder.construct(
        session,
        contract=contract,
        collateral_type=general.collateral_types[90],
        num='01',
        dt=dt.datetime.now() + dt.timedelta(days=10),
        finish_dt=dt.datetime.now() + dt.timedelta(days=10)
    )
    assert NoFutureCollateralsChecker.check(session, contract,
                                     {
                                         'col_dt': col_dt,
                                         'collateral_type': col_type
                                     }) is is_ok

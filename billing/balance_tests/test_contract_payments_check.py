# -*- coding: utf-8 -*-

"test_contract_payments_check"

from __future__ import unicode_literals

import datetime as dt
from decimal import Decimal

import pytest
import mock
import sqlalchemy as sa

import balance.mapper as mapper
import balance.muzzle_util as ut
from balance.actions.contract.contract_payments_check import (
    ContractPaymentsCheck,
    payments_check,
)
from billing.contract_iface.cmeta import general, partners

import tests.object_builder as ob


@pytest.fixture()
def market_contract(session):
    return ob.ContractBuilder.construct(
        session,
        bank_details_id=509,
        commission=9,
        credit_type=1,
        currency=810,
        firm=111,
        netting=1,
        netting_pct=100,
        partner_credit=1,
        payment_term=15,
        payment_type=3,
        personal_account=1,
        services=[610, 612],
        unilateral=1,
        dt=dt.datetime(2020, 10, 1),
        is_signed=dt.datetime(2020, 10, 1),
        print_form_dt=dt.datetime(2020, 10, 1),
    )


@pytest.fixture()
def taxi_contract(session):
    return ob.ContractBuilder.construct(
        session,
        services=[124],
        firm=13,
        country=225,
        partner_commission_pct2=1,
        dt=dt.datetime(2020, 10, 1),
        is_signed=dt.datetime(2020, 10, 1),
    )


@pytest.fixture()
def partner_commission_pct2_col(session, taxi_contract):
    return ob.CollateralBuilder.construct(
        session,
        dt=dt.datetime(2020, 10, 10),
        contract=taxi_contract,
        print_form_type=3,
        partner_commission_pct=66,
        partner_commission_pct2=666,
        partner_commission_type=2,
        partner_min_commission_sum=6,
        partner_max_commission_sum=6666,
        num='новая super дс'
    )


def test_not_tt_contract(session):
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL')

    res, _ = payments_check(contract.col0, {'is_faxed': dt.datetime.now()})
    assert res
    assert not _


def test_not_target_col(session, taxi_contract):
    col = ob.CollateralBuilder.construct(
        session,
        dt=dt.datetime(2020, 10, 1),
        is_signed=dt.datetime.now(),
        contract=taxi_contract,
        print_form_type=3,
        unilateral=1,
        num='новая super дс',
        collateral_type_id=1003
    )

    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 3), service_id=124
    )

    res, _ = payments_check(col, {'unilateral': 0, 'dt': dt.datetime(2010, 10, 3)})
    assert res
    assert not _


def test_not_target_attrs(session, taxi_contract):
    col = ob.CollateralBuilder.construct(
        session,
        dt=dt.datetime(2020, 10, 1),
        is_signed=dt.datetime.now(),
        contract=taxi_contract,
        print_form_type=3,
        unilateral=1,
        partner_commission_pct=66,
        num='новая super дс'
    )

    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 3), service_id=124
    )

    res, _ = payments_check(col, {'unilateral': 0})
    assert res
    assert not _


def test_target_col_becomes_active(session, taxi_contract, partner_commission_pct2_col):

    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 15), service_id=124
    )

    res, _ = payments_check(
        partner_commission_pct2_col, {'is_signed': dt.datetime.now()}
    )
    assert not res
    assert not _


def test_contract_become_not_active(session, taxi_contract):
    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 3), service_id=124
    )

    res, _ = payments_check(
        taxi_contract.col0, {'is_signed': None, 'is_cancelled': dt.datetime.now()}
    )
    assert not res
    assert not _


def test_check_dt_col0_dt_earlier(session, taxi_contract):
    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 3), service_id=124
    )

    res, _ = payments_check(taxi_contract.col0, {'dt': dt.datetime(2020, 10, 5)})
    assert not res
    assert not _


def test_check_dt_col_dt_older(session, taxi_contract, partner_commission_pct2_col):
    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 3), service_id=124
    )

    res, _ = payments_check(
        partner_commission_pct2_col, {'dt': dt.datetime(2020, 10, 2)}
    )
    assert not res
    assert not _


def test_check_dt_col0_dt_older_overlapping_contract(session, taxi_contract):
    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 3), service_id=124
    )

    another_taxi_contract = ob.ContractBuilder.construct(
        session,
        client=taxi_contract.client,
        services=[124],
        firm=13,
        country=225,
        partner_commission_pct2=2,
        dt=dt.datetime(2020, 9, 1),
        finish_dt=dt.datetime(2020, 9, 20),
        is_signed=dt.datetime(2020, 10, 1),
    )

    res, overlapping_contract_ids = payments_check(
        taxi_contract.col0, {'dt': dt.datetime(2020, 9, 10)}
    )
    assert not res
    assert len(overlapping_contract_ids) == 1
    assert another_taxi_contract.id == overlapping_contract_ids[0]


def test_check_end_dt_contract_end_dt_older(session, taxi_contract):
    _dt_now = dt.datetime.now()
    taxi_contract.col0.finish_dt = _dt_now + dt.timedelta(days=10)

    tt = ob.ThirdPartyTransactionBuilder.construct(
        session,
        contract=taxi_contract,
        dt=_dt_now + dt.timedelta(days=6),
        service_id=124,
    )

    res, _ = payments_check(
        taxi_contract.col0, {'finish_dt': _dt_now + dt.timedelta(days=4)}
    )
    assert not res
    assert not _


def test_check_end_dt_contract_end_dt_earlier_overlapping(session, taxi_contract):
    _dt_now = dt.datetime.now()
    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 3), service_id=124
    )
    taxi_contract.col0.finish_dt = dt.datetime(2020, 10, 1)

    another_taxi_contract = ob.ContractBuilder.construct(
        session,
        client=taxi_contract.client,
        services=[124],
        firm=13,
        country=225,
        partner_commission_pct2=3,
        dt=dt.datetime(2020, 10, 10),
        is_signed=dt.datetime(2020, 10, 1),
    )

    res, overlapping_contract_ids = payments_check(
        taxi_contract.col0, {'finish_dt': dt.datetime(2020, 10, 15)}
    )
    assert not res
    assert len(overlapping_contract_ids) == 1
    assert another_taxi_contract.id == overlapping_contract_ids[0]


def test_check_attrs(session, taxi_contract, partner_commission_pct2_col):
    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 15), service_id=124
    )

    res, _ = payments_check(partner_commission_pct2_col, {'partner_commission_pct2': 6})
    assert not res
    assert not _


def test_cols_has_same_num(session, taxi_contract, partner_commission_pct2_col):
    ob.CollateralBuilder.construct(
        session,
        dt=dt.datetime(2020, 10, 2),
        contract=taxi_contract,
        print_form_type=3,
        partner_commission_pct=22,
        partner_commission_pct2=22,
        partner_commission_type=2,
        partner_min_commission_sum=6,
        partner_max_commission_sum=6666,
        num='новая super дс',
        is_signed=dt.datetime(2020, 10, 2)
    )

    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=taxi_contract, dt=dt.datetime(2020, 10, 15), service_id=124
    )

    res, _ = payments_check(
        partner_commission_pct2_col, {'is_signed': dt.datetime.now()}
    )
    assert not res
    assert not _


def test_has_tt_col0_dt(session):
    contract = ob.ContractBuilder.construct(
        session,
        services=[124],
        firm=13,
        country=225,
        dt=dt.datetime(2020, 10, 1),
        is_signed=dt.datetime(2020, 10, 1),
    )
    tt = ob.ThirdPartyTransactionBuilder.construct(
        session, contract=contract, dt=dt.datetime(2020, 10, 15), service_id=124
    )

    res, _ = payments_check(
        contract.col0, {'dt': dt.datetime.now()}
    )
    assert not res
    assert not _

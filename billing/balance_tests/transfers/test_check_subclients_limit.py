# -*- coding: utf-8 -*-

import datetime

import pytest

from balance import mapper
from balance import exc
from balance.actions.transfers_qty import utils as tr_utils
from balance.actions.transfers_qty.interface import (
    TransferMultiple,
    SrcItem,
    DstItem,
)
from balance.providers.personal_acc_manager import PersonalAccountManager
from balance.constants import (
    DIRECT_PRODUCT_ID,
    TransferMode,
)

from tests import object_builder as ob


def _create_pa(session, contract, subclient=None, paysys_id=1003):
    res = (
        PersonalAccountManager(session)
            .for_contract(contract, [subclient] if subclient else None)
            .for_paysys(ob.Getter(mapper.Paysys, paysys_id).build(session).obj)
            .get()
    )
    if subclient is not None:
        assert res.subclient is not None
    return res


def _create_order(session, agency, client=None):
    if client is None:
        client = ob.ClientBuilder(agency=agency).build(session).obj

    return ob.OrderBuilder(
        client=client,
        agency=agency,
        product=ob.Getter(mapper.Product, DIRECT_PRODUCT_ID)
    ).build(session).obj


def _create_contract(session, client, subclients=None):
    person = ob.PersonBuilder(client=client, type='ur').build(session).obj

    contract = ob.ContractBuilder(
        dt=datetime.datetime.now() - datetime.timedelta(days=66),
        client=client,
        person=person,
        commission=1,
        payment_type=3,
        credit_type=1,
        payment_term=30,
        payment_term_max=60,
        personal_account=1,
        personal_account_fictive=1,
        currency=810,
        lift_credit_on_payment=1,
        commission_type=52,
        repayment_on_consume=1,
        credit_limit_single=1666666,
        services={7},
        is_signed=datetime.datetime.now(),
        firm=1,
    ).build(session).obj
    if subclients:
        contract.col0.client_limits = {
            scl.id: {'client_limit': 666666}
            for scl in subclients
        }
    session.flush()

    return contract


@pytest.fixture
def agency(session):
    return ob.ClientBuilder(is_agency=True).build(session).obj


def test_transfer(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    contract = _create_contract(session, agency, [order1.client, order2.client])
    pa = _create_pa(session, contract, order1.client)
    pa.transfer(order1, TransferMode.dst, 10, skip_check=True)

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        TransferMultiple(
            session,
            [SrcItem(10, 10, order1)],
            [DstItem(1, order2)]
        ).do()

    assert 'individual credit limit' in exc_info.value.msg


def test_wo_subclient(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    contract = _create_contract(session, agency)
    pa = _create_pa(session, contract)
    tr_utils.check_subclient_limit([pa], [order1.client, order2.client])


def test_same_subclient(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency, order1.client)
    contract = _create_contract(session, agency, [order1.client])
    pa = _create_pa(session, contract, order1.client)
    assert pa.subclient is not None
    tr_utils.check_subclient_limit([pa], [order1.client, order2.client])


def test_diffrent_order_subclients(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    contract = _create_contract(session, agency, [order1.client, order2.client])
    pa = _create_pa(session, contract, order1.client)
    with pytest.raises(exc.INVALID_PARAM):
        tr_utils.check_subclient_limit([pa], [order1.client, order2.client])


def test_multiple_pas(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    contract = _create_contract(session, agency)
    pa1 = _create_pa(session, contract, paysys_id=1003)
    pa2 = _create_pa(session, contract, paysys_id=1000)
    assert pa1 is not pa2
    tr_utils.check_subclient_limit([pa1, pa2], [order1.client, order2.client])


def test_different_pa_subclients(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    contract = _create_contract(session, agency, [order1.client, order2.client])
    pa1 = _create_pa(session, contract, order1.client)
    pa2 = _create_pa(session, contract, order2.client)
    with pytest.raises(exc.INVALID_PARAM):
        tr_utils.check_subclient_limit([pa1, pa2], [order1.client])


def test_multiple_pas_orders(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    contract = _create_contract(session, agency, [order1.client, order2.client])
    pa1 = _create_pa(session, contract, order1.client)
    pa2 = _create_pa(session, contract, order2.client)
    with pytest.raises(exc.INVALID_PARAM):
        tr_utils.check_subclient_limit([pa1, pa2], [order1.client, order2.client])


@pytest.mark.linked_clients
def test_equivalent_clients(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    contract = _create_contract(session, agency, [order1.client])
    pa = _create_pa(session, contract, order1.client)
    order2.client.make_equivalent(order1.client)
    session.flush()
    session.expire_all()

    tr_utils.check_subclient_limit([pa], [order1.client, order2.client])


@pytest.mark.linked_clients
def test_brand_today(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    contract = _create_contract(session, agency, [order1.client])
    pa = _create_pa(session, contract, order1.client)

    today = datetime.datetime.now()
    ob.create_brand(
        session,
        [(today, [order1.client, order2.client])],
        finish_dt=today + datetime.timedelta(1)
    )

    tr_utils.check_subclient_limit([pa], [order1.client, order2.client])
    tr_utils.check_subclient_limit([pa], [order1.client, order2.client], today)


@pytest.mark.linked_clients
def test_brand_yesterday(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    order3 = _create_order(session, agency)
    contract = _create_contract(session, agency, [order1.client])
    pa = _create_pa(session, contract, order1.client)

    today = datetime.datetime.now()
    yesterday = today - datetime.timedelta(1)

    today = datetime.datetime.now()
    ob.create_brand(
        session,
        [
            (yesterday, [order1.client, order2.client, order3.client]),
            (today, [order1.client, order2.client]),
        ],
        finish_dt=today + datetime.timedelta(1)
    )

    tr_utils.check_subclient_limit([pa], [order1.client, order2.client, order3.client], yesterday)
    with pytest.raises(exc.INVALID_PARAM):
        tr_utils.check_subclient_limit([pa], [order1.client, order2.client, order3.client], today)


@pytest.mark.linked_clients
def test_brand_past(session, agency):
    order1 = _create_order(session, agency)
    order2 = _create_order(session, agency)
    order3 = _create_order(session, agency)
    contract = _create_contract(session, agency, [order1.client])
    pa = _create_pa(session, contract, order1.client)

    today = datetime.datetime.now()
    min_dt = session.execute('select min(dt) from bo.v_contract_dates').scalar()
    prehistoric = min_dt - datetime.timedelta(1)

    ob.create_brand(
        session,
        [(min_dt, [order1.client, order2.client, order3.client])],
        finish_dt=today + datetime.timedelta(1)
    )

    def _call():
        tr_utils.check_subclient_limit([pa], [order1.client, order2.client, order3.client], prehistoric)

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        _call()
    assert 'individual credit limit for' in exc_info.value.msg

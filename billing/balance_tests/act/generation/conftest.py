# -*- coding: utf-8 -*-

import datetime

import pytest

from balance import mapper

from tests import object_builder as ob


@pytest.fixture
def agency(session):
    return ob.ClientBuilder(is_agency=1).build(session).obj


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder(client=client).build(session).obj


@pytest.fixture
def subclient(session, agency):
    return ob.ClientBuilder(agency=agency).build(session).obj


@pytest.fixture
def credit_contract(session, agency):
    contract = ob.ContractBuilder(
        dt=datetime.datetime.now() - datetime.timedelta(days=66),
        client=agency,
        person=ob.PersonBuilder(client=agency, type='ur'),
        commission=1,
        payment_type=3,
        credit_type=1,
        payment_term=30,
        payment_term_max=60,
        personal_account=1,
        personal_account_fictive=1,
        currency=810,
        lift_credit_on_payment=1,
        commission_type=57,
        repayment_on_consume=1,
        credit_limit_single=1666666,
        services={7},
        is_signed=datetime.datetime.now(),
        firm=1,
    ).build(session).obj
    return contract


@pytest.fixture
def endbuyer(session, credit_contract, agency, subclient):
    return ob.PersonBuilder(
        client=agency,
        type='endbuyer_ur',
        name='Конченый покупатель'
    ).build(session).obj


@pytest.fixture
def paysys(session):
    return session.query(mapper.Paysys).filter_by(firm_id=1).getone(cc='ur')


@pytest.fixture(params=[
    pytest.param(True, id='split_act_creation'),
    pytest.param(False, id='dont_split_act_creation')
])
def split_act_creation(request):
    return request.param

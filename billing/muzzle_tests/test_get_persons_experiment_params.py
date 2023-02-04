import pytest

import muzzle.api.client as client_api
from tests import object_builder as ob


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture
def passport(session, client):
    return ob.PassportBuilder.construct(session, client=client)


def test_get_paystep_experiment_params_no_persons_no_region_no_client(session, client):
    assert session.oper_id
    res = client_api.get_persons_experiment_params(session)
    assert res == {}


def test_get_paystep_experiment_params_no_persons_no_region(session, client, passport):
    session.oper_id = passport.passport_id
    session.flush()

    res = client_api.get_persons_experiment_params(session)
    assert res == {}


def test_get_paystep_experiment_params_w_persons_w_region(session, client, passport):
    session.oper_id = passport.passport_id
    person = ob.PersonBuilder.construct(session, client=client)
    client.region_id = 225
    session.flush()

    res = client_api.get_persons_experiment_params(session)
    expected = {person.type: 1,
                'region_id': 225}
    assert res == expected


def test_get_paystep_experiment_params_w_hidden_person(session, client, passport):
    session.oper_id = passport.passport_id
    person = ob.PersonBuilder.construct(session, client=client, type='ph')
    ob.PersonBuilder.construct(session, client=client, hidden=1, type='ur')
    client.region_id = 225
    session.flush()

    res = client_api.get_persons_experiment_params(session)

    expected = {person.type: 1,
                'region_id': 225}
    assert res == expected

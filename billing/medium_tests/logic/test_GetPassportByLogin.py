import random
import pytest

from balance.constants import RoleType
from balance.mapper import Passport, ServiceClient, Service
import balance.mapper.permissions as permissions
from tests import object_builder as ob


def create_passport(session, client=None, dead=0):
    id_ = random.randrange(-666666666, -2)
    _passport = Passport(passport_id=id_, login='login' + str(ob.get_big_number()), client=client, dead=dead)
    session.add(_passport)
    session.flush()
    return _passport


def create_client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


def test_get_passport_empty_passport(xmlrpcserver, session):
    passport = create_passport(session)
    expected_result = {'IsMain': 0,
                       'Login': passport.login,
                       'Uid': passport.passport_id}
    assert xmlrpcserver.GetPassportByLogin(-1, passport.login, {'LimitedClientIds': 1}) == expected_result


def test_get_passport_w_client(xmlrpcserver, session):
    client = create_client(session)
    passport = create_passport(session, client)
    expected_result = {'IsMain': 0,
                       'Login': passport.login,
                       'Uid': passport.passport_id,
                       'ClientId': client.id}
    assert xmlrpcserver.GetPassportByLogin(-1, passport.login) == expected_result


@pytest.mark.parametrize('relations', [None, {}, {'LimitedClientIds': 1}, {'AllClientIds': 1}])
def test_get_passport_w_limited_client(xmlrpcserver, session, relations):
    client = create_client(session)
    limited_client = create_client(session)
    passport = create_passport(session, client)
    passport._passport_client_roles = [permissions.RoleClientPassport(passport=passport, client_id=limited_client.id,
                                                                       role_id=RoleType.DIRECT_LIMITED)]
    session.flush()
    expected = {'IsMain': 0, 'Login': passport.login, 'Uid': passport.passport_id, 'ClientId': client.id}
    if relations:
        expected.update({'ClientId': client.id, 'LimitedClientIds': [limited_client.id]})
    assert xmlrpcserver.GetPassportByLogin(-1, passport.login, relations) == expected


@pytest.mark.parametrize('relations', [None, {}, {'RepresentedClientIds': 1}, {'AllClientIds': 1}])
def test_get_passport_w_represented_client(xmlrpcserver, session, relations):
    client = create_client(session)
    limited_client = create_client(session)
    passport = create_passport(session, client)
    passport._passport_client_roles = [permissions.RoleClientPassport(passport=passport, client_id=limited_client.id,
                                                                       role_id=RoleType.REPRESENTATIVE)]
    session.flush()
    expected = {'IsMain': 0, 'Login': passport.login, 'Uid': passport.passport_id, 'ClientId': client.id}
    if relations:
        expected.update({'ClientId': client.id, 'RepresentedClientIds': [limited_client.id]})
    assert xmlrpcserver.GetPassportByLogin(-1, passport.login, relations) == expected


@pytest.mark.parametrize('relations', [None, {}, {'ServiceClientIds': 1}, {'AllClientIds': 1}])
def test_get_passport_w_service_client(xmlrpcserver, session, relations):
    client = create_client(session)
    passport = create_passport(session, client)
    service_client = ServiceClient(service=ob.Getter(Service, 23).build(session).obj, passport=passport, client=client)
    session.add(service_client)
    session.flush()
    expected = {'IsMain': 0, 'Login': passport.login, 'Uid': passport.passport_id, 'ClientId': client.id}
    if relations:
        expected.update({'ServiceClientIds': [{'ClientID': client.id, 'ServiceID': 23}]})
    assert xmlrpcserver.GetPassportByLogin(-1, passport.login, relations) == expected


@pytest.mark.parametrize('dead', [0, 1])
def test_get_passport_dead(xmlrpcserver, session, dead):
    client = create_client(session)
    passport = create_passport(session, client, dead)
    expected_result = {'IsMain': 0,
                       'Login': passport.login,
                       'Uid': passport.passport_id,
                       'ClientId': client.id,
                       'Dead': dead}
    assert xmlrpcserver.GetPassportByLogin(-1, passport.login, 0, True) == expected_result

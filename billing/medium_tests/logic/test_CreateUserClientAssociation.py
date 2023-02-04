# -*- coding: utf-8 -*-
import pytest
import random
from tests import object_builder as ob

from balance.mapper import Passport
from balance.constants import MARKET_SERVICE_ID
from balance.mapper import ServiceClient

CODE_CLIENT_NOT_FOUND = 1003
CODE_INVALID_PARAM = 2
CODE_ALREADY_LINKED_TO_OTHER_CLIENT = 4008
CODE_NOT_LINKED_TO_CLIENT = 4009


@pytest.fixture
def client(session, **attrs):
    res = ob.ClientBuilder(**attrs).build(session).obj
    return res


@pytest.fixture
def existing_passport(session, is_internal=False):
    id_ = random.randrange(-666666666, -2) - (1000000000 if is_internal else 0)
    _passport = Passport(passport_id=id_, login='login' + str(ob.get_big_number()))
    session.add(_passport)
    session.flush()
    return _passport


@pytest.fixture
def not_existing_passport_id():
    return str(-ob.get_big_number())


@pytest.fixture
def not_existing_client_id(session):
    return session.execute('select bo.s_client_id.nextval from dual').scalar()


def test_create_user_client_association(xmlrpcserver, session, existing_passport, client):
    res = xmlrpcserver.CreateUserClientAssociation(session.oper_id, client.id,
                                                   existing_passport.passport_id)
    assert res == (0, 'SUCCESS')
    assert existing_passport.client == client


def test_create_user_client_association_service_client(xmlrpcserver, session, existing_passport, client):
    client.service_id = MARKET_SERVICE_ID
    res = xmlrpcserver.CreateUserClientAssociation(session.oper_id, client.id,
                                                   existing_passport.passport_id)
    assert res == (0, 'SUCCESS')
    assert existing_passport.service_clients[0].client_id == client.id
    assert existing_passport.service_clients[0].passport_id == existing_passport.passport_id
    assert existing_passport.service_clients[0].service_id == client.service_id


def test_create_user_client_association_service_client_already_linked(xmlrpcserver, session, existing_passport, client):
    client.service_id = MARKET_SERVICE_ID
    service_client = ServiceClient(client_id=client.id, passport_id=existing_passport.passport_id,
                                   service_id=MARKET_SERVICE_ID)
    existing_passport.service_clients = [service_client]
    res = xmlrpcserver.CreateUserClientAssociation(session.oper_id, client.id,
                                                   existing_passport.passport_id)
    assert res == (0, 'SUCCESS')
    assert existing_passport.service_clients[0].client_id == client.id
    assert existing_passport.service_clients[0].passport_id == existing_passport.passport_id
    assert existing_passport.service_clients[0].service_id == client.service_id


def test_create_user_client_association_update_client(xmlrpcserver, session, existing_passport):
    client_ = client(session)
    another_client = client(session)
    existing_passport.client = client_
    res = xmlrpcserver.CreateUserClientAssociation(0, another_client.id, existing_passport.passport_id)
    error_message = 'Passport {login} ({passport_id}) is already linked to OTHER client {client_id}'
    assert res == (CODE_ALREADY_LINKED_TO_OTHER_CLIENT,
                   error_message.format(login=existing_passport.login, passport_id=existing_passport.passport_id,
                                        client_id=client_.id))
    assert existing_passport.client == client_


def test_update_user_client_association_same_client(xmlrpcserver, existing_passport, client):
    existing_passport.client = client
    res = xmlrpcserver.CreateUserClientAssociation(0, client.id, existing_passport.passport_id)
    assert res == (0, 'SUCCESS')
    assert existing_passport.client == client


def test_create_user_client_association_non_existing_passport(xmlrpcserver, not_existing_passport_id, client):
    res = xmlrpcserver.CreateUserClientAssociation(0, client.id, not_existing_passport_id)
    error_message = 'Passport not found\nPassport with ID {passport_id} not found in DB'
    assert res == (CODE_INVALID_PARAM, error_message.format(passport_id=not_existing_passport_id))


def test_create_user_client_association_non_existing_client(xmlrpcserver, not_existing_client_id, existing_passport):
    existing_passport.client = None
    res = xmlrpcserver.CreateUserClientAssociation(0, not_existing_client_id, existing_passport.passport_id)
    error_message = 'Client with ID {client_id} not found in DB'
    assert res == (CODE_CLIENT_NOT_FOUND, error_message.format(client_id=not_existing_client_id))
    assert existing_passport.client is None


def test_remove_user_client_association(xmlrpcserver, client, existing_passport):
    existing_passport.client = client
    res = xmlrpcserver.RemoveUserClientAssociation(0, client.id, existing_passport.passport_id)
    assert res == (0, 'SUCCESS')
    assert existing_passport.client is None


def test_remove_not_linked_client(xmlrpcserver, client, existing_passport):
    res = xmlrpcserver.RemoveUserClientAssociation(0, client.id, existing_passport.passport_id)
    error_message = 'Passport {login} ({passport_id}) is NOT linked to client {client_id}'
    assert res == (CODE_NOT_LINKED_TO_CLIENT,
                   error_message.format(passport_id=existing_passport.passport_id, client_id=client.id,
                                        login=existing_passport.login))
    assert existing_passport.client is None


def test_remove_user_client_association_non_existing_passport(xmlrpcserver, not_existing_passport_id, client):
    res = xmlrpcserver.RemoveUserClientAssociation(0, client.id, not_existing_passport_id)
    error_message = 'Passport not found\nPassport with ID {passport_id} not found in DB'
    assert res == (CODE_INVALID_PARAM, error_message.format(passport_id=not_existing_passport_id))


def test_remove_user_client_association_non_existing_client(xmlrpcserver, not_existing_client_id, existing_passport):
    existing_passport.client = None
    res = xmlrpcserver.RemoveUserClientAssociation(0, not_existing_client_id, existing_passport.passport_id)
    error_message = 'Client with ID {client_id} not found in DB'
    assert res == (CODE_CLIENT_NOT_FOUND, error_message.format(client_id=not_existing_client_id))
    assert existing_passport.client is None


def test_add_representative(session, xmlrpcserver, existing_passport):
    main_client = client(session)
    representative = client(session)
    res = xmlrpcserver.CreateUserClientAssociation(0, main_client.id, existing_passport.passport_id,
                                                   [representative.id])
    assert res == (0, 'SUCCESS')
    assert len(existing_passport.client_roles) == 1
    representative_roles = list(existing_passport.client_roles[representative.id])
    assert representative_roles[0].id == 101
    assert representative_roles[0].name == u'Директ: Ограниченный представитель клиента'


def test_add_representative_empty_list(xmlrpcserver, existing_passport, client):
    res = xmlrpcserver.CreateUserClientAssociation(0, client.id, existing_passport.passport_id, [])
    assert res == (0, 'SUCCESS')
    assert len(existing_passport.client_roles) == 0


def test_delete_representative_with_empty_list(session, xmlrpcserver, existing_passport):
    main_client = client(session)
    representative = client(session)
    xmlrpcserver.CreateUserClientAssociation(0, main_client.id, existing_passport.passport_id, [representative.id])
    xmlrpcserver.CreateUserClientAssociation(0, main_client.id, existing_passport.passport_id, [])
    assert len(existing_passport.client_roles) == 0


def test_update_representative_with_none(session, xmlrpcserver, existing_passport):
    main_client = client(session)
    representative = client(session)
    xmlrpcserver.CreateUserClientAssociation(0, main_client.id, existing_passport.passport_id, [representative.id])
    xmlrpcserver.CreateUserClientAssociation(0, main_client.id, existing_passport.passport_id)
    assert len(existing_passport.client_roles) == 1


def test_update_representative(session, xmlrpcserver, existing_passport):
    main_client = client(session)
    representative = client(session)
    xmlrpcserver.CreateUserClientAssociation(0, main_client.id, existing_passport.passport_id, [representative.id])
    new_representative = client(session)
    res = xmlrpcserver.CreateUserClientAssociation(0, main_client.id, existing_passport.passport_id,
                                                   [new_representative.id])
    assert res == (0, 'SUCCESS')
    assert len(existing_passport.client_roles) == 1
    representative_roles = list(existing_passport.client_roles[new_representative.id])
    assert representative_roles[0].id == 101
    assert representative_roles[0].name == u'Директ: Ограниченный представитель клиента'

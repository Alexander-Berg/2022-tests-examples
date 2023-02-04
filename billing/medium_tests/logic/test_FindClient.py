# -*- coding: utf-8 -*-
from __future__ import with_statement

import pytest
from tests import object_builder as ob

from balance.core import Core
from balance import exc

CODE_SUCCESS = 0
CODE_CLIENT_NOT_FOUND = 1003


@pytest.fixture
def passport(session):
    return ob.PassportBuilder().build(session).obj


@pytest.fixture
def agency(session, **attrs):
    return ob.ClientBuilder(is_agency=1, **attrs).build(session).obj


@pytest.fixture
def client(session, **attrs):
    return ob.ClientBuilder(is_agency=0, **attrs).build(session).obj


def test_find_client(xmlrpcserver, session):
    agency_ = agency(session, city='Москва', name='Пупков', email='email@ru.ru', fax='fax', url='yandex.ru',
                     phone='12345')
    session.flush()
    res = xmlrpcserver.FindClient({'ClientID': agency_.id, 'AgencySelectPolicy': 3, 'PrimaryClients': True,
                                   'Name': agency_.name, 'Phone': agency_.phone, 'Email': agency_.email,
                                   'Fax': agency_.fax, 'Url': agency_.url})
    assert len(res) == 3
    assert (res[0], res[1]) == (0, CODE_SUCCESS)
    assert [clh['CLIENT_ID'] for clh in res[2]] == [agency_.id]


@pytest.mark.parametrize('primary_client_value, expected', [(True, lambda client_, eq_client: [client_.id]),
                                                            (False, lambda client_, eq_client: [eq_client.id])])
def test_find_client_primary(session, xmlrpcserver, primary_client_value, expected):
    client_ = client(session)
    eq_client = client(session)
    eq_client.make_equivalent(client_)
    res = xmlrpcserver.FindClient({'ClientID': eq_client.id, 'AgencySelectPolicy': 2,
                                   'PrimaryClients': primary_client_value})
    assert len(res) == 3
    assert (res[0], res[1]) == (0, CODE_SUCCESS)
    assert [clh['CLIENT_ID'] for clh in res[2]] == expected(client_, eq_client)
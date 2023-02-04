# -*- coding: utf-8 -*-
from __future__ import with_statement

import pytest
from tests import object_builder as ob

from balance.core import Core
from balance import exc


@pytest.fixture
def passport(session):
    return ob.PassportBuilder().build(session).obj


@pytest.fixture
def agency(session, **attrs):
    return ob.ClientBuilder(is_agency=1, **attrs).build(session).obj


@pytest.fixture
def client(session, **attrs):
    return ob.ClientBuilder(is_agency=0, **attrs).build(session).obj


def test_find_client_empty_params(session):
    core = Core(session)
    with pytest.raises(Exception) as exc_info:
        core.find_clients_query(session)
    assert exc_info.type == exc.INVALID_PARAM
    assert exc_info.value.msg == 'Invalid parameter for function: You should provide SOME filtering criteria'


def test_find_client_agency_select_policy(session, agency, client):
    core = Core(session)
    agency.name = 'Пупков'
    client.name = agency.name
    session.flush()
    result = core.find_clients_query(session, attrs={'name': agency.name}, asp=1).all()
    assert agency in result
    assert client in result

    result = core.find_clients_query(session, attrs={'name': agency.name}, asp=None).all()
    assert agency in result
    assert client in result

    result = core.find_clients_query(session, attrs={'name': agency.name}, asp=2).all()
    assert agency not in result
    assert client in result

    result = core.find_clients_query(session, attrs={'name': agency.name}, asp=3).all()
    assert agency in result
    assert client not in result


def test_find_client_wrong_agency_select_policy(session, client):
    core = Core(session)
    with pytest.raises(Exception) as exc_info:
        core.find_clients_query(session, client_id=client.id, asp=0)
    assert exc_info.type == exc.INVALID_PARAM
    error_msg = 'Invalid parameter for function: asp,  should be one of (1, 2, 3, 6, 12, None) (or None, which equals 1)'
    assert exc_info.value.msg == error_msg


def test_find_client_strict_search(session):
    core = Core(session)
    client_ = client(session, name='пупКОВ')
    client_with_opposite_case_name = client(session, name='ПУПков')
    session.flush()

    result = core.find_clients_query(session, attrs={'name': client_.name}, strict=1, asp=None).all()
    assert client_with_opposite_case_name not in result

    result = core.find_clients_query(session, attrs={'name': client_.name}, strict=0, asp=None).all()
    assert client_with_opposite_case_name in result


def test_find_client_empty_strings(session):
    core = Core(session)
    client_empty_fax = client(session, name='Пупков', fax='')
    client_non_empty_fax = client(session, name='Пупков', fax='fax_me')
    client_fax_contains_spaces_only = client(session, name='Пупков', fax='  ')

    result = core.find_clients_query(session, attrs={'name': client_empty_fax.name, 'fax': ''}, strict=1,
                                     asp=None).all()
    assert client_empty_fax in result
    assert client_non_empty_fax not in result
    assert client_fax_contains_spaces_only not in result

    result = core.find_clients_query(session, attrs={'name': client_empty_fax.name, 'fax': ''}, strict=0,
                                     asp=None).all()
    assert client_empty_fax in result
    assert client_non_empty_fax in result
    assert client_fax_contains_spaces_only in result


def test_find_client_class_heads(session, passport):
    client_ = client(session, name='main_client_name')
    eq_client = client(session, name='eq_client_name')
    eq_client.make_equivalent(client_)
    passport.client = eq_client
    session.flush()
    core = Core(session)
    result = core.find_clients_query(session, attrs={'name': eq_client.name}, strict=1, asp=None, class_heads=1,
                                     rep_passport_id=passport.passport_id).all()
    assert client_ in result
    assert eq_client not in result


def test_find_client_with_passport(session, client, passport):
    passport.client = client
    session.flush()
    core = Core(session)
    result = core.find_clients_query(session, attrs={'name': client.name}, strict=1, asp=None,
                                     rep_passport_id=passport.passport_id).all()
    assert client in result
    result = core.find_clients_query(session, attrs={'name': ''}, strict=1, asp=None,
                                     rep_passport_id=passport.passport_id).all()
    assert client not in result

    result = core.find_clients_query(session, attrs={'name': client.name}, strict=1, asp=None,
                                     rep_login=passport.login).all()
    assert client in result


@pytest.mark.parametrize(
    'login',
    [
        'covid-2019',
        u'корона-2019',
        u'корона.2019',
    ],
)
@pytest.mark.parametrize(
    'strict', [False, True],
)
def test_find_clients_by_login(session, client, login, strict):
    passport = ob.create_passport(session, login=login)
    passport.link_to_client(client)
    session.flush()

    core = Core(session)
    result = core.find_clients_query(session, attrs={}, strict=strict, asp=None,
                                     rep_login=login).all()
    assert client in result


@pytest.mark.parametrize(
    'method_name, ans',
    [
        ('find_clients', True),  # medium
        ('find_clients_ex', False),  # muzzle
    ],
)
def test_find_clients_permissions(session, client, passport, method_name, ans):
    ob.set_roles(session, passport, [])
    core = Core(session)
    func = getattr(core, method_name)
    result = func(attrs={'name': client.name}, strict=1)
    assert (client in result) is ans

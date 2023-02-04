# -*- coding: utf-8 -*-
import collections
import hamcrest as hm
import httpretty
import json
import operator
import pytest
import random
import re

from balance.application import getApplication
from balance import constants as cst, mapper
from medium.upravlyator.logic import Upravlyator
from butils import passport, passport_cache
from tests.object_builder import (
    get_big_number,
    ClientBuilder,
    CorrectionTemplateGroupBuilder,
    FirmBuilder,
    RoleClientBuilder,
    RoleClientGroupBuilder,
)


EXTERNAL_CLIENT_ROLE = 3
GROUP_ID = get_big_number()


class UpravlyatorWrapper(object):
    def __init__(self, session):
        self._logic = Upravlyator(session)

    @staticmethod
    def __process_value(key, value):
        if key in ('fields', 'data', 'role'):
            return [json.dumps(value)]
        else:
            return [value]

    def __getattr__(self, item):
        func = getattr(self._logic, item)

        def wrap(**kwargs):
            return func(['idm', 'method'], {k: self.__process_value(k, v) for k, v in kwargs.iteritems()})

        return wrap


def create_passport(session, login, is_internal=False):
    id_ = random.randrange(-666666666, -2) - (1000000000 if is_internal else 0)
    _passport = mapper.Passport(passport_id=id_, login=login + str(get_big_number()))
    session.add(_passport)
    session.flush()
    return _passport


def create_role(session):
    role = mapper.Role(
        id=get_big_number(),
        name='666',
        fields=['passport-login'],  # по умолчанию все наши роли принимают паспортный логин
    )
    session.add(role)
    session.flush()
    return role


@pytest.fixture(name='client')
def create_client(session):
    return ClientBuilder.construct(session)


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return RoleClientBuilder.construct(
        session,
        client=client,
    )


@pytest.fixture(name='role_client_group')
def create_role_client_group(session, clients=None):
    clients = clients or [create_client(session) for _i in range(2)]
    return RoleClientGroupBuilder.construct(session, clients=clients)


@pytest.fixture
def upravlyator(session):
    return UpravlyatorWrapper(session)


@pytest.fixture
def modular_upravlyator(modular_session):
    return UpravlyatorWrapper(modular_session)


@pytest.fixture(autouse=True)
def patch_passports_range():
    passport.INTRA_MAX = lambda: -1000000000
    passport.INTRA_MIN = lambda: -1666666666

    passport_cache.INTRA_MAX = lambda: -1000000000
    passport_cache.INTRA_MIN = lambda: -1666666666


@pytest.fixture(autouse=True)
def admsubscribe_mock():
    call_info = []

    def mock_(passport, unsubscribe=False):
        call_info.append({
            'passport': passport,
            'unsubscribe': unsubscribe,
        })

    passport.passport_admsubscribe = mock_
    return call_info


@pytest.fixture
def domain_passport(session):
    return create_passport(session, login='login', is_internal=True)


@pytest.fixture
def yndx_passports(request, session, domain_passport):
    num = getattr(request, 'param', 2)
    res = []
    for idx in xrange(num):
        _passport = create_passport(session, 'yndx-%s-%s' % (domain_passport.login, idx))
        session.flush()
        res.append(_passport)

    return res


@pytest.fixture
def yndx_passport(session, domain_passport):
    _passport = create_passport(session, 'yndx-%s' % domain_passport.login)
    session.flush()
    return _passport


@pytest.fixture
def role(session):
    return create_role(session)


@pytest.fixture
def roles(request, session):
    num = getattr(request, 'param', 2)
    res = []
    for idx in xrange(num):
        res.append(create_role(session))
    return res


@pytest.fixture
def support_tvms(session):
    def set_support_tvm_in_idm(available):
        # session.config.set('SUPPORT_TVM_APPS_IN_IDM', 1 if available else 0)
        session.config.__dict__['SUPPORT_TVM_APPS_IN_IDM'] = 1 if available else 0
        session.flush()

    return set_support_tvm_in_idm


@pytest.fixture
def modular_support_tvms(modular_session):
    def set_support_tvm_in_idm(available):
        # session.config.set('SUPPORT_TVM_APPS_IN_IDM', 1 if available else 0)
        modular_session.config.__dict__['SUPPORT_TVM_APPS_IN_IDM'] = 1 if available else 0
        modular_session.flush()

    return set_support_tvm_in_idm


def make_staff_response(service_id=None, error_dict=None):
    url = getApplication().get_component_cfg('staff_api')['URL']
    assert operator.xor(service_id is None, error_dict is None)
    if service_id:
        res = {'service': {'id': service_id}, 'type': 'service'}
    else:
        res = error_dict
    httpretty.register_uri(
        httpretty.GET,
        re.compile('%sv3/group/\\d+/' % url),
        status=200,
        content_type="text/json",
        body=json.dumps(res),
    )


parametrize_constraints = pytest.mark.parametrize(
    'constraints', [
        pytest.param([cst.ConstraintTypes.template_group_id], id='Template group constraint'),
        pytest.param([cst.ConstraintTypes.firm_id], id='Firm constraint'),
        pytest.param([cst.ConstraintTypes.firm_id, cst.ConstraintTypes.template_group_id],
                     id='Firm and Template group constraints')
    ])


def get_fields_by_constraints(session, constraints):
    fields = dict()
    if cst.ConstraintTypes.firm_id in constraints:
        fields[cst.ConstraintTypes.firm_id] = FirmBuilder().build(session).obj.id

    if cst.ConstraintTypes.template_group_id in constraints:
        fields[cst.ConstraintTypes.template_group_id] = CorrectionTemplateGroupBuilder().build(session).obj.id

    return fields


def add_role_with_constraints(user_passport, role, constraints):
    session = user_passport.session
    session.add(mapper.RealRolePassport(passport=user_passport, role=role))
    pr = [pr for pr in user_passport.real_passport_roles if pr.role == role][0]
    for key, value in constraints.items():
        setattr(pr, key, value)


RoleTuple = collections.namedtuple('RoleTuple', 'role_id, firm_id, group_id, client_ids, template_group_id')
RoleTuple.__new__.__defaults__ = (None,) * len(RoleTuple._fields)


def assert_passport_roles(passport, *roles):
    role_properties = []
    for role in roles:
        if not isinstance(role, tuple):
            role = role,
        role = role + (None, None, None, None)
        role_id, firm_id, group_id, client_ids, template_group_id = role[:5]
        if client_ids is None:
            client_ids = []
        role_properties.append(hm.has_properties(
            group_id=group_id,
            role_id=role_id,
            firm_id=firm_id,
            template_group_id=template_group_id,
            client_ids=hm.contains_inanyorder(*client_ids),
            create_dt=hm.not_none(),
            update_dt=hm.not_none(),
        ))

    hm.assert_that(
        passport.passport_roles,
        hm.contains_inanyorder(*role_properties),
    )

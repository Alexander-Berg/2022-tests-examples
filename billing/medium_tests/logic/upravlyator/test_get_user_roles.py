# -*- coding: utf-8 -*-
import collections
import hamcrest as hm
import pytest

from balance import mapper

# noinspection PyUnresolvedReferences
from tests.medium_tests.logic.upravlyator.conftest import (
    EXTERNAL_CLIENT_ROLE,
    add_role_with_constraints,

    parametrize_constraints,
    get_fields_by_constraints
)

UserRoleTuple = collections.namedtuple('UserRoleTuple', 'role_id, firm_id, template_group_id, yndx_login')
UserRoleTuple.__new__.__defaults__ = (None,) * len(UserRoleTuple._fields)


def _assert_get_roles(upravlyator, login, with_tvms, *roles):
    def _select_matcher(field, val):
        if val is None:
            return hm.not_(hm.has_key(field))
        return hm.has_entry(field, str(val))

    roles_matcher = []
    for role in roles:
        if not isinstance(role, tuple):
            role = role,
        role = role + (None, None, None)
        role_id, firm_id, template_group_id, yndx_login = role[:4]
        role_matcher = hm.has_entries({'role': str(role_id)})
        if (firm_id or yndx_login or template_group_id) is not None:
            role_matcher = hm.contains(
                role_matcher,
                hm.all_of(
                    _select_matcher('firm_id', firm_id),
                    _select_matcher('template_group_id', template_group_id),
                    _select_matcher('passport-login', yndx_login),
                ),
            )

        roles_matcher.append(role_matcher)

    res = upravlyator.get_user_roles(login=login)

    matcher = {
        'code': 0,
        'login': login,
        'roles': hm.contains_inanyorder(*roles_matcher),
    }

    if with_tvms:
        matcher['subject_type'] = 'user'

    hm.assert_that(res, hm.has_entries(matcher))


@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_domain_role(session, upravlyator, domain_passport, role, support_tvms, with_tvms):
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.flush()

    _assert_get_roles(upravlyator, domain_passport.login, with_tvms, EXTERNAL_CLIENT_ROLE,
                      UserRoleTuple(role_id=role.id))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_domain_role_with_constraints(constraints, session, upravlyator, domain_passport, role, support_tvms, with_tvms):
    constraint_fields = get_fields_by_constraints(session, constraints)
    add_role_with_constraints(domain_passport, role, constraint_fields)
    session.flush()

    _assert_get_roles(upravlyator, domain_passport.login, with_tvms, EXTERNAL_CLIENT_ROLE,
                      UserRoleTuple(role_id=role.id, **constraint_fields))


@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_passport_role(session, upravlyator, domain_passport, yndx_passport, role, support_tvms, with_tvms):
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    session.flush()

    _assert_get_roles(upravlyator, domain_passport.login, with_tvms, EXTERNAL_CLIENT_ROLE,
                      UserRoleTuple(role_id=role.id, yndx_login=yndx_passport.login))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_passport_role_with_constraints(constraints, session, upravlyator, domain_passport, yndx_passport, role, support_tvms, with_tvms):
    constraint_fields = get_fields_by_constraints(session, constraints)
    yndx_passport.master = domain_passport
    add_role_with_constraints(yndx_passport, role, constraint_fields)
    session.flush()

    _assert_get_roles(upravlyator, domain_passport.login, with_tvms, EXTERNAL_CLIENT_ROLE,
                      UserRoleTuple(role_id=role.id, yndx_login=yndx_passport.login, **constraint_fields))


@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_passport_external_role(session, upravlyator, domain_passport, yndx_passport, support_tvms, with_tvms):
    yndx_passport.master = domain_passport
    session.flush()

    _assert_get_roles(upravlyator, domain_passport.login, with_tvms, EXTERNAL_CLIENT_ROLE)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_domain_external_role(upravlyator, domain_passport, support_tvms, with_tvms):
    _assert_get_roles(upravlyator, domain_passport.login, with_tvms, EXTERNAL_CLIENT_ROLE)


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_domain_and_passport_roles(constraints, session, upravlyator, domain_passport, yndx_passport, role, roles, support_tvms, with_tvms):
    role1, role2 = roles

    fst_constraint_fields = get_fields_by_constraints(session, constraints)
    sd_constraint_fields = get_fields_by_constraints(session, constraints)

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1, **fst_constraint_fields))
    session.flush()

    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role1, **sd_constraint_fields))
    session.flush()

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role2))
    session.flush()

    _assert_get_roles(
        upravlyator,
        domain_passport.login,
        with_tvms,
        EXTERNAL_CLIENT_ROLE,
        UserRoleTuple(role_id=role1.id, **fst_constraint_fields),
        UserRoleTuple(role_id=role1.id, yndx_login=yndx_passport.login, **sd_constraint_fields),
        role.id,
        role1.id,
        UserRoleTuple(role_id=role1.id, yndx_login=yndx_passport.login),
        UserRoleTuple(role_id=role2.id, yndx_login=yndx_passport.login),
    )

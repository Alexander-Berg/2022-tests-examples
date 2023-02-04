# -*- coding: utf-8 -*-
import datetime
import pytest
import mock
import hamcrest as hm
import sqlalchemy as sa

from balance import constants as cst, mapper, muzzle_util as ut
import tests.object_builder as ob
from tests.tutils import has_exact_entries
# noinspection PyUnresolvedReferences
from tests.medium_tests.logic.upravlyator.conftest import (
    EXTERNAL_CLIENT_ROLE,
    GROUP_ID,
    RoleTuple,
    add_role_with_constraints,
    assert_passport_roles,
    create_passport,
    create_client,
    create_role_client,
    create_role_client_group,
    make_staff_response,

    parametrize_constraints,
    get_fields_by_constraints,
)

from balance.mapper import TVMACLApp, TVMACLGroupPermission, TVMACLGroup


def _del_all_roles(session):
    session.query(mapper.RealRolePassport).delete()
    session.query(mapper.RoleGroup).delete()
    session.flush()


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_to_domain_login(session, upravlyator, domain_passport, role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_none_constraint_to_domain_login(session, upravlyator, domain_passport, role,
                                                  admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend([cst.ConstraintTypes.firm_id, cst.ConstraintTypes.template_group_id])
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=None,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 0,
    }
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_to_passport_login(session, upravlyator, domain_passport, yndx_passport, role,
                                    admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 0,
        'data': {'passport-login': yndx_passport.login}
    }
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == [
        {'passport': yndx_passport, 'unsubscribe': False}
    ]
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_none_constraint_to_passport_login(session, upravlyator, domain_passport, yndx_passport,
                                                    role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend([cst.ConstraintTypes.firm_id, cst.ConstraintTypes.template_group_id])
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 0,
        'data': {'passport-login': yndx_passport.login}
    }
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == [
        {'passport': yndx_passport, 'unsubscribe': False}
    ]
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_like_domain_to_passport_login(session, upravlyator, domain_passport, yndx_passport, role,
                                                admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 0,
        'data': {'passport-login': yndx_passport.login}
    }
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == [
        {'passport': yndx_passport, 'unsubscribe': False}
    ]
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role.id)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_not_like_domain_to_passport_login(session, upravlyator, domain_passport, yndx_passport,
                                                    roles, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role2.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 0,
        'data': {'passport-login': yndx_passport.login}
    }
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == [
        {'passport': yndx_passport, 'unsubscribe': False}
    ]
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role1.id)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role2.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_another_role_to_passport_login(session, upravlyator, domain_passport, yndx_passport, roles,
                                            admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles

    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role1))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role2.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 0,
        'data': {'passport-login': yndx_passport.login}
    }
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role1.id, role2.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_like_another_passport_to_passport_login(session, upravlyator, domain_passport,
                                                          yndx_passports, role, admsubscribe_mock,
                                                          support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport1, yndx_passport2 = yndx_passports

    yndx_passport1.master = domain_passport
    yndx_passport2.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport2.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 0,
        'data': {'passport-login': yndx_passport2.login}
    }
    assert yndx_passport1.master == domain_passport
    assert yndx_passport2.master == domain_passport
    assert admsubscribe_mock == [{'passport': yndx_passport2, 'unsubscribe': False}, ]
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE, role.id)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE, role.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_not_like_another_passport_to_passport_login(session, upravlyator, domain_passport,
                                                              yndx_passports, roles, admsubscribe_mock,
                                                              support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    yndx_passport1, yndx_passport2 = yndx_passports

    yndx_passport1.master = domain_passport
    yndx_passport2.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role1))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role2.id)},
        fields={'passport-login': yndx_passport2.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 0,
        'data': {'passport-login': yndx_passport2.login}
    }
    assert yndx_passport1.master == domain_passport
    assert yndx_passport2.master == domain_passport
    assert admsubscribe_mock == [{'passport': yndx_passport2, 'unsubscribe': False}, ]
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE, role1.id)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE, role2.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_another_role_to_domain_login(session, upravlyator, domain_passport, roles, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role2.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role1.id, role2.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_like_passport_to_domain_login(session, upravlyator, domain_passport, yndx_passport, role,
                                                admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role.id)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_not_like_passport_to_domain_login(session, upravlyator, domain_passport, yndx_passport,
                                                    roles, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role1))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role2.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role2.id)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role1.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_to_passport_bound_to_another_domain(session, upravlyator, domain_passport, yndx_passport,
                                                      role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    alt_domain_passport = create_passport(session, 'alt-login', is_internal=True)

    yndx_passport.master = alt_domain_passport
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    msg = u'Паспортный логин %s привязан к другому доменному логину %s!' % (
        yndx_passport.login, alt_domain_passport.login)
    hm.assert_that(
        res,
        hm.has_entries(
            code=1,
            error='Upravlyator.WrongLinkedPassport(%s)' % msg.encode('utf-8'),
        ),
    )
    assert yndx_passport.master == alt_domain_passport


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_same_role_to_domain_login(session, upravlyator, domain_passport, role, admsubscribe_mock,
                                       support_tvms, with_tvms):
    support_tvms(with_tvms)

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 1,
        'warning': u'Пользователь уже имеет эту роль.',
    }
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role.id)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_same_role_to_passport_login(session, upravlyator, domain_passport, yndx_passport, role,
                                         admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 1,
        'warning': u'Пользователь уже имеет эту роль.',
        'data': {'passport-login': yndx_passport.login}
    }
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role.id)


@pytest.mark.parametrize('with_tvms', [True, False])
@mock.patch('butils.passport.PassportBlackbox._call_api', return_value={'error': '404'})
def test_add_role_to_unexist_domain_login(session, upravlyator, domain_passport, role, admsubscribe_mock,
                                          support_tvms, with_tvms):
    support_tvms(with_tvms)

    session.begin_nested()
    res = upravlyator.add_role(
        login=str('UnExisTiK'),
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert res['code'] == 1
    assert res['fatal'] == u'Пользователь не найден: UnExisTiK'.encode('utf-8')


@pytest.mark.parametrize('with_tvms', [True, False])
@mock.patch('butils.passport.PassportBlackbox._call_api', return_value={'error': '404'})
def test_add_role_to_unexist_passport_login(session, upravlyator, domain_passport, yndx_passport, role,
                                            admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'passport-login': 'UnExisTiK'},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res['code'] == 1
    assert res['fatal'] == u'Пользователь не найден: UnExisTiK'.encode('utf-8')

    assert domain_passport not in session
    # Проверяем, что при исключении зовется rollback


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_unexist_role_to_domain_login(session, upravlyator, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str('9999')},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res['code'] == 1
    assert res['fatal'] == u'Роль не найдена'.encode('utf-8')

    assert domain_passport not in session
    # Проверяем, что при исключении зовется rollback


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_unexist_role_to_passport_login(session, upravlyator, domain_passport, yndx_passport,
                                            support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str('9999')},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res['code'] == 1
    assert res['fatal'] == u'Роль не найдена'.encode('utf-8')

    assert domain_passport not in session
    # Проверяем, что при исключении зовется rollback


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_with_constraints_to_domain_login(constraints, session, upravlyator, role, domain_passport,
                                                   admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert res == {'code': 0, 'data': constraint_fields}
    assert admsubscribe_mock == []

    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE,
                          RoleTuple(role_id=role.id, **constraint_fields))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_with_constraints_to_passport_login(constraints, session, upravlyator, domain_passport, yndx_passport,
                                                     role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)
    fields = dict(constraint_fields, **{'passport-login': yndx_passport.login})

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0, 'data': fields}
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == [
        {'passport': yndx_passport, 'unsubscribe': False}
    ]
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)

    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE,
                          RoleTuple(role_id=role.id, **constraint_fields))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_with_constraints_same_to_domain_to_passport_login(constraints, session, upravlyator, domain_passport,
                                                                    yndx_passport,
                                                                    role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)
    fields = dict(constraint_fields, **{'passport-login': yndx_passport.login})

    add_role_with_constraints(domain_passport, role, constraint_fields)
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0, 'data': fields}
    expected_roles = [EXTERNAL_CLIENT_ROLE, RoleTuple(role_id=role.id, **constraint_fields)]
    assert_passport_roles(domain_passport, *expected_roles)
    assert_passport_roles(yndx_passport, *expected_roles)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == [
        {'passport': yndx_passport, 'unsubscribe': False}
    ]
    assert_passport_roles(domain_passport, *expected_roles)
    assert_passport_roles(yndx_passport, *expected_roles)


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_with_constraints_same_to_passport_to_domain_login(constraints, session, upravlyator, domain_passport,
                                                                    yndx_passport,
                                                                    role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)

    yndx_passport.master = domain_passport
    add_role_with_constraints(yndx_passport, role, constraint_fields)
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0, 'data': constraint_fields}
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []
    expected_roles = [EXTERNAL_CLIENT_ROLE, RoleTuple(role_id=role.id, **constraint_fields)]
    assert_passport_roles(domain_passport, *expected_roles)
    assert_passport_roles(yndx_passport, *expected_roles)


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_constraint_to_role_to_domain_login(constraints, session, upravlyator, role, domain_passport,
                                                admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.flush()

    _res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert _res == {'code': 0, 'data': constraint_fields}
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, (role.id, None),
                          RoleTuple(role_id=role.id, **constraint_fields))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_constraint_to_role_to_passport_login(constraints, session, upravlyator, domain_passport, yndx_passport,
                                                  role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)
    fields = dict(constraint_fields, **{'passport-login': yndx_passport.login})

    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    yndx_passport.master = domain_passport
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0, 'data': fields}
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []

    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, (role.id, None),
                          RoleTuple(role_id=role.id, **constraint_fields))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_no_constraint_to_role_to_domain_login(constraints, session, upravlyator, role, domain_passport,
                                                   admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)

    add_role_with_constraints(domain_passport, role, constraint_fields)
    session.flush()

    _res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert _res == {'code': 0}
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, (role.id, None),
                          RoleTuple(role_id=role.id, **constraint_fields))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_no_constraint_to_role_to_passport_login(constraints, session, upravlyator, domain_passport, yndx_passport,
                                                     role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)

    yndx_passport.master = domain_passport
    add_role_with_constraints(yndx_passport, role, constraint_fields)
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {
        'code': 0,
        'data': {'passport-login': yndx_passport.login}
    }
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []

    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, (role.id, None),
                          RoleTuple(role_id=role.id, **constraint_fields))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_same_role_with_another_constraints_to_domain(constraints, session, upravlyator, role, domain_passport,
                                                          admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Роли с разными constraints - это разные роли"""
    role.fields.extend(constraints)
    fst_constraint_fields = get_fields_by_constraints(session, constraints)
    sd_constraint_fields = get_fields_by_constraints(session, constraints)
    add_role_with_constraints(domain_passport, role, fst_constraint_fields)
    session.flush()

    _res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=sd_constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )

    role_constraints = [ut.clear_dict_with_none_values({'firm_id': pr.firm_id, 'template_group_id': pr.template_group_id})
                        for pr in domain_passport.passport_roles if pr.role == role]
    hm.assert_that(
        role_constraints,
        hm.contains_inanyorder(fst_constraint_fields, sd_constraint_fields),
    )

    assert admsubscribe_mock == []
    assert _res == {'code': 0, 'data': sd_constraint_fields}

    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE,
                          RoleTuple(role_id=role.id, **fst_constraint_fields),
                          RoleTuple(role_id=role.id, **sd_constraint_fields))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_same_role_with_another_constraints_to_passport(constraints, session, upravlyator, role, domain_passport,
                                                            yndx_passport, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Роли с разными constraints - это разные роли"""
    yndx_passport.master = domain_passport

    role.fields.extend(constraints)
    fst_constraint_fields = get_fields_by_constraints(session, constraints)
    sd_constraint_fields = get_fields_by_constraints(session, constraints)
    fields = dict(sd_constraint_fields, **{'passport-login': yndx_passport.login})
    add_role_with_constraints(yndx_passport, role, fst_constraint_fields)
    session.flush()

    _res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert _res == {'code': 0, 'data': fields}
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE,
                          RoleTuple(role.id, **fst_constraint_fields),
                          RoleTuple(role.id, **sd_constraint_fields))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_replace_role_constraint_with_same_value(constraints, session, upravlyator, role, domain_passport,
                                                 support_tvms, with_tvms):
    support_tvms(with_tvms)

    """При дублировании роли в ответе warning"""
    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)

    domain_passport.set_roles([(role, constraint_fields)])
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'warning': u'Пользователь уже имеет эту роль.',
        }),
    )


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_invalid_constraint_fields(constraints, session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Роль не поддерживает ограничение по firm_id/template_group_id"""
    constraint_fields = get_fields_by_constraints(session, constraints)
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidField(field_names=%s)' % unicode(', '.join(constraints)),
        }),
    )


# Выбрасывается ошибка по первому сбойному полю. Сейчас нет смысла проверять группой
@pytest.mark.parametrize('constraints', [
    pytest.param([cst.ConstraintTypes.template_group_id], id='Template constraint'),
    pytest.param([cst.ConstraintTypes.firm_id], id='Firm constraint'),
])
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_with_fake_constraint_values(constraints, session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)
    fake_constraint_fields = {key: '-' + str(value) for key, value in constraint_fields.items()}
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=fake_constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidFieldValue(field_name=%s, value=%s)'
                     % fake_constraint_fields.popitem(),
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_role_with_fake_constraint(upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    fake_field = u'fake'
    fake_val = 123
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={fake_field: fake_val},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidField(field_names=%s)' % fake_field,
        }),
    )


@pytest.mark.parametrize('is_group', [True, False])
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_undefined_firm(session, upravlyator, role, domain_passport, admsubscribe_mock, support_tvms, is_group, with_tvms):
    support_tvms(with_tvms)

    """firm_id = -1 - это 'все фирмы'"""
    firm_id = cst.FirmId.UNDEFINED
    role.fields.append(cst.ConstraintTypes.firm_id)

    session.config.__dict__['IDM_BATCH_SIZE'] = 5
    _del_all_roles(session)
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=GROUP_ID))

    session.flush()

    params = {
        'role': {'role': str(role.id)},
        'fields': {cst.ConstraintTypes.firm_id: firm_id}
    }
    if is_group:
        params['group'] = GROUP_ID
    else:
        params['login'] = domain_passport.login
    res = upravlyator.add_role(**params)

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'data': hm.has_entry(cst.ConstraintTypes.firm_id, firm_id),
        }),
    )
    roles = role.id, firm_id
    if is_group:
        roles += (GROUP_ID,)
    assert_passport_roles(domain_passport, roles)
    passport_role = domain_passport.passport_roles[0]
    assert passport_role.constraint_values == {}  # не должна использоваться при проверке прав
    assert passport_role.firm_id == cst.FirmId.UNDEFINED  # есть запись в базе

    # если запросить роли пользователя для управлятора, то фирма должна быть
    res = upravlyator.get_roles()

    params_match = {
        'path': '/role/%s/' % role.id,
        'fields': {cst.ConstraintTypes.firm_id: str(firm_id)},
    }
    if is_group:
        params_match['group'] = GROUP_ID
    else:
        params_match['login'] = domain_passport.login

        if with_tvms:
            params_match['subject_type'] = 'user'

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'roles': hm.contains(
                has_exact_entries(params_match),
            ),
        }),
    )


@pytest.mark.parametrize(
    'users_count',
    [1, 3],
)
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_group_role(session, upravlyator, role, users_count, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    passports = []
    for i in range(users_count):
        passport = create_passport(session, 'domain_passport_%s' % i)
        passports.append(passport)
        session.add(mapper.PassportGroup(passport_id=passport.passport_id, group_id=GROUP_ID))
    session.flush()

    res = upravlyator.add_role(
        group=str(GROUP_ID),
        role={'role': str(role.id)},
        fields={'abc_clients': False},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({'code': 0}),
    )
    for passport in passports:
        assert_passport_roles(passport, (EXTERNAL_CLIENT_ROLE,), (role.id, None, GROUP_ID))


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_group_role_w_constraints(constraints, session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=GROUP_ID))
    session.flush()

    constraint_fields = get_fields_by_constraints(session, constraints)

    res = upravlyator.add_role(
        group=GROUP_ID,
        role={'role': str(role.id)},
        fields=constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'data': hm.has_entries(constraint_fields),
        }),
    )
    assert_passport_roles(domain_passport, (EXTERNAL_CLIENT_ROLE,),
                          RoleTuple(role_id=role.id, group_id=GROUP_ID, **constraint_fields))


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_to_existing_group_memberships(session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    member = mapper.PassportGroup(
        passport_id=domain_passport.passport_id,
        group_id=GROUP_ID,
        create_dt=session.now() - datetime.timedelta(seconds=60),
    )
    session.add(member)
    session.flush()

    hm.assert_that(
        domain_passport.passport_roles,
        hm.contains(hm.has_properties(role_id=EXTERNAL_CLIENT_ROLE)),
    )

    res = upravlyator.add_role(
        group=GROUP_ID,
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(res, has_exact_entries({'code': 0}))

    session.refresh(domain_passport)
    hm.assert_that(
        domain_passport.passport_roles,
        hm.has_item(
            hm.has_properties(
                role_id=role.id,
                firm_id=None,
                group_id=GROUP_ID,
                create_dt=hm.greater_than(member.create_dt),
                update_dt=hm.greater_than(member.create_dt),
            )
        ),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_pin_client_w_domain_passport(upravlyator, role, domain_passport, client, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'client_id': str(client.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'data': has_exact_entries({
                'client_id': str(client.id),
            }),
        }),
    )

    assert_passport_roles(
        domain_passport,
        (EXTERNAL_CLIENT_ROLE,),
        (role.id, None, None, [client.id]),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_pin_client_w_yndx_passport(session, upravlyator, role, domain_passport, yndx_passport, client, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    yndx_passport.master = domain_passport
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={'client_id': str(client.id), 'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'data': has_exact_entries({
                'passport-login': yndx_passport.login,
                'client_id': str(client.id),
            }),
        }),
    )

    assert_passport_roles(
        yndx_passport,
        (EXTERNAL_CLIENT_ROLE,),
        (role.id, None, None, [client.id]),
    )


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_pin_client_w_constraints_w_yndx_passport(constraints, session, upravlyator, role, domain_passport,
                                                  yndx_passport, client, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    role.fields.append('client_id')
    yndx_passport.master = domain_passport
    session.flush()

    constraint_fields = get_fields_by_constraints(session, constraints)
    fields = dict(constraint_fields, **{
        'client_id': str(client.id),
        'passport-login': yndx_passport.login,
    })

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'data': has_exact_entries(fields),
        }),
    )

    assert_passport_roles(
        yndx_passport,
        (EXTERNAL_CLIENT_ROLE,),
        RoleTuple(role_id=role.id, client_ids=[client.id], **constraint_fields),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_add_existing_role_w_client_batch_id(session, upravlyator, role, domain_passport, yndx_passport, role_client,
                                             support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(
        passport_id=yndx_passport.passport_id,
        role_id=role.id,
        client_batch_id=role_client.client_batch_id,
    ))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={
            'client_id': str(role_client.client_id),
            'passport-login': yndx_passport.login,
        },
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 1,
            'warning': u'Пользователь уже имеет эту роль.',
            'data': has_exact_entries({
                'client_id': str(role_client.client_id),
                'passport-login': yndx_passport.login,
            }),
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_several_client_batch_rows_exists(session, upravlyator, role, domain_passport, yndx_passport, client,
                                          support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    yndx_passport.master = domain_passport
    for role_client in [create_role_client(session, client=client) for _i in range(2)]:
        session.add(mapper.RealRolePassport(
            passport_id=yndx_passport.passport_id,
            role_id=role.id,
            client_batch_id=role_client.client_batch_id,
        ))
    session.flush()

    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={
            'client_id': str(client.id),
            'passport-login': yndx_passport.login,
        },
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'error': 'Multiple rows were found for one()',
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_client_doesnt_found(session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    client_id = (session.execute(sa.func.next_value(sa.Sequence('s_client_id'))).scalar() + 10)
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={
            'client_id': str(client_id),
        },
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'error': 'Upravlyator.NotFound(Client, %s)' % client_id,
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_client_invalid_client_id(upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields={
            'client_id': '-666',
        },
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidFieldValue(field_name=client_id, value=-666)',
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_abc_clients_w_domain_passport(_mock_tvm, session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=GROUP_ID))
    session.flush()

    make_staff_response(GROUP_ID)

    res = upravlyator.add_role(
        group=GROUP_ID,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'data': has_exact_entries({'abc_clients': True}),
        }),
    )

    assert_passport_roles(
        domain_passport,
        (EXTERNAL_CLIENT_ROLE,),
        (role.id, None, GROUP_ID),  # клиентов нет, т.к. группа новая и для неё еще не притянуты клиенты из ABC
    )
    client_batch_id = (
        session.query(mapper.RolePassport)
        .filter_by(passport_id=domain_passport.passport_id, group_id=GROUP_ID, role_id=role.id)
        .one()
    ).client_batch_id
    hm.assert_that(
        session.query(mapper.RoleClientGroup).filter_by(external_id=GROUP_ID).all(),
        hm.contains(
            hm.has_properties({
                'client_batch_id': client_batch_id,
            }),
        ),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_abc_clients_w_domain_passport_to_existing_group(_mock_staff, session, upravlyator, role, domain_passport, role_client_group,
                                                         support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=role_client_group.external_id))
    session.flush()

    make_staff_response(role_client_group.external_id)

    res = upravlyator.add_role(
        group=role_client_group.external_id,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'data': has_exact_entries({'abc_clients': True}),
        }),
    )

    assert_passport_roles(
        domain_passport,
        (EXTERNAL_CLIENT_ROLE,),
        (role.id, None, role_client_group.external_id, role_client_group.client_ids),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_abc_clients_w_yndx_passport_to_existing_group(_mock_staff, session, upravlyator, role, domain_passport, yndx_passport, role_client_group,
                                                       support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    yndx_passport.master = domain_passport
    session.add(mapper.PassportGroup(passport_id=yndx_passport.passport_id, group_id=role_client_group.external_id))
    session.flush()

    make_staff_response(role_client_group.external_id)

    res = upravlyator.add_role(
        group=role_client_group.external_id,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'data': has_exact_entries({'abc_clients': True}),
        }),
    )

    assert_passport_roles(
        domain_passport,
        (EXTERNAL_CLIENT_ROLE,),
    )
    assert_passport_roles(
        yndx_passport,
        (EXTERNAL_CLIENT_ROLE,),
        (role.id, None, role_client_group.external_id, role_client_group.client_ids),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_abc_clients_w_yndx_passport_double(_mock_staff, session, upravlyator, role, domain_passport, role_client_group,
                                            support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    session.add(mapper.RoleGroup(
        group_id=role_client_group.external_id,
        role_id=role.id,
        client_batch_id=role_client_group.client_batch_id,
    ))
    session.flush()

    make_staff_response(role_client_group.external_id)

    res = upravlyator.add_role(
        group=role_client_group.external_id,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 1,
            'warning': u'Группа уже имеет эту роль.',
            'data': has_exact_entries({'abc_clients': True}),
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_abc_clients_w_yndx_passport_to_existing_group_2(_mock_staff, session, upravlyator, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Пользователь привязан к 2м группам с разным набором клиентов."""
    role = ob.create_role(
        session,
        ('PERM', {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}),
    )
    role.fields = ['abc_clients']
    group_1 = create_role_client_group(session)
    group_2 = create_role_client_group(session)

    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=group_1.external_id))
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=group_2.external_id))
    session.add(mapper.RoleGroup(
        group_id=group_1.external_id,
        role_id=role.id,
        client_batch_id=group_1.client_batch_id,
    ))
    session.flush()

    make_staff_response(group_2.external_id)

    res = upravlyator.add_role(
        group=group_2.external_id,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'data': has_exact_entries({'abc_clients': True}),
        }),
    )

    assert_passport_roles(
        domain_passport,
        (EXTERNAL_CLIENT_ROLE,),
        (role.id, None, group_1.external_id, group_1.client_ids),
        (role.id, None, group_2.external_id, group_2.client_ids),
    )
    hm.assert_that(
        domain_passport.get_perm_constraints('PERM'),
        hm.contains_inanyorder(
            has_exact_entries({
                cst.ConstraintTypes.client_batch_id: hm.contains(group_1.client_batch_id),
            }),
            has_exact_entries({
                cst.ConstraintTypes.client_batch_id: hm.contains(group_2.client_batch_id),
            }),
        ),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.parametrize('is_group', [True, False])
def test_invalid_client_format(upravlyator, role, domain_passport, is_group, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(['client_id', 'abc_clients'])
    kwargs = {
        'login': domain_passport.login,
        'group': GROUP_ID,
        'role': {'role': str(role.id)},
        'fields': {
            'abc_clients': True,
            'client_id': '666',
        },
    }
    del kwargs['group' if not is_group else 'login']

    if with_tvms:
        kwargs['subject_type'] = 'user'

    res = upravlyator.add_role(**kwargs)
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidField(field_names=%s)' % ('client_id' if is_group else 'abc_clients'),
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_abc_client_false_doesnt_matter(upravlyator, role, domain_passport, role_client, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(['client_id', 'abc_clients'])
    fields = {
        u'abc_clients': False,
        u'client_id': unicode(role_client.client_id),
    }
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    assert res == {
        'code': 0,
        'data': fields,
    }
    assert_passport_roles(
        domain_passport,
        EXTERNAL_CLIENT_ROLE,
        (role.id, None, None, [role_client.client_id]),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_abc_client_w_existing_group(session, upravlyator, role, domain_passport, role_client_group, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(['client_id', 'abc_clients'])
    client = role_client_group.clients[0]

    fields = {u'client_id': unicode(client.id)}
    res = upravlyator.add_role(
        login=str(domain_passport.login),
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    assert res == {
        'code': 0,
        'data': fields,
    }
    assert_passport_roles(
        domain_passport,
        EXTERNAL_CLIENT_ROLE,
        (role.id, None, None, [client.id]),
    )
    role_clients = session.query(mapper.RoleClient).filter_by(client_id=client.id).all()
    assert len(role_clients) == 2
    hm.assert_that(
        role_clients,
        hm.contains_inanyorder(
            hm.has_property('client_batch_id', role_client_group.client_batch_id),
            hm.has_property('client_batch_id', domain_passport.passport_roles[1].client_batch_id),
        ),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_require_login_or_group(upravlyator, role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.add_role(
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'error': 'Require login or group_id',
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.parametrize(
    'w_yndx',
    [
        pytest.param(True, id='yndx_passport'),
        pytest.param(False, id='domain_login'),
    ],
)
@pytest.mark.parametrize(
    'to_group',
    [
        pytest.param(True, id='to group'),
        pytest.param(False, id='to passport'),
    ],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_add_client_related_w_group(_mock_staff, session, upravlyator, domain_passport, yndx_passport, role, w_yndx, client, to_group,
                                    support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(['client_id', 'abc_clients'])
    passport = domain_passport
    if w_yndx:
        yndx_passport.master = domain_passport
        passport = yndx_passport

    group = create_role_client_group(session, clients=[client])
    session.add(mapper.PassportGroup(group_id=group.external_id, passport_id=passport.passport_id))
    if to_group:
        role_client = ob.RoleClientBuilder.construct(session, client=client)
        session.add(mapper.RealRolePassport(passport_id=passport.passport_id, role=role, client_batch_id=role_client.client_batch_id))
    else:
        session.add(mapper.RoleGroup(group_id=group.external_id, role=role, client_batch_id=group.client_batch_id))
    session.flush()

    match_role = (role.id, None, None if to_group else group.external_id, [client.id])
    assert_passport_roles(
        passport,
        EXTERNAL_CLIENT_ROLE,
        match_role,
    )

    make_staff_response(group.external_id)

    fields = {
        u'client_id': unicode(client.id),
        u'abc_clients': True,
    }
    params = {
        'login': domain_passport.login,
        'group': group.external_id,
        'role': {'role': str(role.id)},
        'fields': fields,
    }

    if to_group:
        del fields[u'client_id']
        del params['login']
    else:
        del fields[u'abc_clients']
        del params['group']

    if w_yndx:
        fields[u'passport-login'] = yndx_passport.login

    if with_tvms:
        params['subject_type'] = 'user'

    res = upravlyator.add_role(**params)

    assert res == {
        'code': 0,
        'data': fields,
    }

    session.refresh(passport)
    assert_passport_roles(
        passport,
        EXTERNAL_CLIENT_ROLE,
        (role.id, None, group.external_id, [client.id]),
        (role.id, None, None, [client.id]),
    )
    role_clients = session.query(mapper.RoleClient).filter_by(client_id=client.id).all()
    assert len(role_clients) == 2


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.parametrize(
    'w_yndx',
    [
        pytest.param(True, id='yndx_passport'),
        pytest.param(False, id='domain_login'),
    ],
)
def test_add_several_clients(session, upravlyator, role, domain_passport, yndx_passport, w_yndx, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(['client_id', 'abc_clients'])
    passport = domain_passport
    if w_yndx:
        yndx_passport.master = domain_passport
        passport = yndx_passport

    client_1 = create_client(session)
    client_2 = create_client(session)

    role_client = ob.RoleClientBuilder.construct(session, client=client_1)
    session.add(mapper.RealRolePassport(passport_id=passport.passport_id, role=role, client_batch_id=role_client.client_batch_id))
    session.flush()

    fields = {
        u'client_id': unicode(client_2.id),
    }
    if w_yndx:
        fields[u'passport-login'] = yndx_passport.login
    params = {
        'login': domain_passport.login,
        'role': {'role': str(role.id)},
        'fields': fields,
    }

    if with_tvms:
        params['subject_type'] = 'user'

    res = upravlyator.add_role(**params)
    assert res == {
        'code': 0,
        'data': fields,
    }

    session.refresh(passport)
    assert_passport_roles(
        passport,
        EXTERNAL_CLIENT_ROLE,
        (role.id, None, None, [client_1.id]),
        (role.id, None, None, [client_2.id]),
    )
    role_clients = session.query(mapper.RoleClient).filter(mapper.RoleClient.client_id.in_([client_1.id, client_2.id])).all()
    assert len(role_clients) == 2


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.parametrize(
    'w_yndx',
    [
        pytest.param(True, id='yndx_passport'),
        pytest.param(False, id='domain_login'),
    ],
)
def test_add_several_roles_w_one_client(session, upravlyator, domain_passport, yndx_passport, w_yndx, client,
                                        support_tvms, with_tvms):
    support_tvms(with_tvms)

    passport = domain_passport
    if w_yndx:
        yndx_passport.master = domain_passport
        passport = yndx_passport

    role_1 = ob.create_role(session, ('PERM', {cst.ConstraintTypes.client_batch_id: None}))
    role_2 = ob.create_role(session, ('PERM', {cst.ConstraintTypes.client_batch_id: None}))
    for role in [role_1, role_2]:
        role.fields = ['client_id', 'passport-login']

    role_client = ob.RoleClientBuilder.construct(session, client=client)
    session.add(mapper.RealRolePassport(passport_id=passport.passport_id, role=role_1, client_batch_id=role_client.client_batch_id))
    session.flush()

    fields = {
        u'client_id': unicode(client.id),
    }
    if w_yndx:
        fields[u'passport-login'] = yndx_passport.login
    params = {
        'login': domain_passport.login,
        'role': {'role': str(role_2.id)},
        'fields': fields,
    }

    if with_tvms:
        params['subject_type'] = 'user'

    res = upravlyator.add_role(**params)
    assert res == {
        'code': 0,
        'data': fields,
    }

    session.refresh(passport)
    assert_passport_roles(
        passport,
        EXTERNAL_CLIENT_ROLE,
        (role_1.id, None, None, [client.id]),
        (role_2.id, None, None, [client.id]),
    )
    role_client = session.query(mapper.RoleClient).filter_by(client_id=client.id).one()
    assert role_client.client_batch_id == role_client.client_batch_id


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.parametrize(
    'w_clients',
    [True, False],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_different_group_id_and_abc_service_id(_mock_tvm, session, upravlyator, role, domain_passport, w_clients,
                                               support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=GROUP_ID))
    abc_service_id = 1234
    if w_clients:
        group = create_role_client_group(session)
        abc_service_id = group.external_id
    session.flush()

    make_staff_response(abc_service_id)

    res = upravlyator.add_role(
        group=GROUP_ID,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'data': has_exact_entries({'abc_clients': True}),
        }),
    )

    assert_passport_roles(
        domain_passport,
        (EXTERNAL_CLIENT_ROLE,),
        (role.id, None, GROUP_ID, group.client_ids if w_clients else None),
    )
    client_batch_id = (
        session.query(mapper.RolePassport)
        .filter_by(passport_id=domain_passport.passport_id, group_id=GROUP_ID, role_id=role.id)
        .one()
    ).client_batch_id
    hm.assert_that(
        session.query(mapper.RoleClientGroup).filter_by(external_id=abc_service_id).all(),
        hm.contains(
            hm.has_properties({
                'client_batch_id': client_batch_id,
            }),
        ),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_wrong_abc_service_id(_mock_tvm, session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=GROUP_ID))
    session.flush()

    make_staff_response(error_dict={'error': 'invalid request'})

    res = upravlyator.add_role(
        group=GROUP_ID,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidFieldValue(field_name=%s, value=%s)'
                     % ('abc_clients', GROUP_ID),
        }),
    )


def test_unavailable_tvm_roles(session, upravlyator, support_tvms):
    support_tvms(False)

    res = upravlyator.add_role(
        login=str(666),
        role={'role': 'tvms_roles', 'tvms_roles': 'test_group'},
        fields={'env': 'test'},
        subject_type='tvm_app',
    )

    hm.assert_that(
        res,
        hm.has_entries({'code': 1, 'error': 'Unavailable TVMs'})
    )


def test_tvm_role_not_found(session, upravlyator, support_tvms):
    support_tvms(True)

    res = upravlyator.add_role(
        login=str(666),
        role={'role': 'tvms_roles', 'tvms_roles': 'test_group'},
        fields={'env': 'test'},
        subject_type='tvm_app',
    )

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': u'Роль не найдена'.encode('utf-8')
        })
    )


@pytest.mark.parametrize(
    'fields, invalid_fields',
    [
        pytest.param({}, ['env'], id='missing'),
        pytest.param({'env': True, 'abc_clients': True}, ['abc_clients'], id='excess'),
    ],
)
def test_tvm_invalid_fields_in_service(session, upravlyator, fields, invalid_fields, support_tvms):
    support_tvms(True)

    session.add(TVMACLGroup(name='group_as_role'))
    session.flush()

    res = upravlyator.add_role(
        login=str('666'),
        role={'role': 'tvms_roles', 'tvms_roles': 'group_as_role'},
        subject_type='tvm_app',
        fields=fields,
    )

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidField(field_names=%s)' % ', '.join(set(invalid_fields))
        })
    )


def test_tvm_has_role(session, upravlyator, support_tvms):
    support_tvms(True)

    tvm = TVMACLApp(tvm_id=666, env='test')
    session.add(tvm)
    session.flush()

    session.add(TVMACLGroup(name='group_as_role'))
    session.flush()

    session.add(TVMACLGroupPermission(tvm_id=tvm.tvm_id, group_name='group_as_role'))
    session.flush()

    fields = {'env': tvm.env}
    res = upravlyator.add_role(
        login=str(tvm.tvm_id),
        role={'role': 'tvms_roles', 'tvms_roles': 'group_as_role'},
        subject_type='tvm_app',
        fields=fields,
    )

    assert res == {'code': 1, 'data': fields, 'warning': u'TVM уже состоит в этой группе.'}


def test_tvm_with_diff_env(session, upravlyator, support_tvms):
    support_tvms(True)

    tvm = TVMACLApp(tvm_id=666, env='test')
    session.add(tvm)

    session.add(TVMACLGroup(name='group_as_role'))
    session.flush()

    fields = {'env': 'prod'}
    res = upravlyator.add_role(
        login=str(tvm.tvm_id),
        role={'role': 'tvms_roles', 'tvms_roles': 'group_as_role'},
        subject_type='tvm_app',
        fields=fields,
    )

    assert res == {'code': 1, 'data': fields, 'warning': u'Указанный TVM имеент другое значение env'}


def test_tvm_add_new_role(session, upravlyator, support_tvms):
    support_tvms(True)

    tvm = TVMACLApp(tvm_id=666, env='test')
    session.add(tvm)

    role = TVMACLGroup(name='group_as_role')
    session.add(role)
    session.flush()

    fields = {'env': tvm.env}
    res = upravlyator.add_role(
        login=str(tvm.tvm_id),
        role={'role': 'tvms_roles', 'tvms_roles': 'group_as_role'},
        subject_type='tvm_app',
        fields=fields,
    )

    assert res == {'code': 0, 'data': fields}

    roles = session.query(TVMACLGroupPermission).filter(sa.and_(
        TVMACLGroupPermission.group_name == role.name,
        TVMACLGroupPermission.tvm_id == tvm.tvm_id
    )).limit(1).all()

    assert len(roles) == 1


def test_tvm_create_tvm_if_not_exists(session, upravlyator, support_tvms):
    support_tvms(True)

    role = TVMACLGroup(name='group_as_role')
    session.add(role)
    session.flush()

    fields = {'env': 'test'}
    res = upravlyator.add_role(
        login=str('666'),
        role={'role': 'tvms_roles', 'tvms_roles': 'group_as_role'},
        subject_type='tvm_app',
        fields=fields,
    )

    assert res == {'code': 0, 'data': fields}

    tvm = session.query(TVMACLApp).get(666)

    assert tvm.env == 'test'

# -*- coding: utf-8 -*-
import pytest
import mock
import hamcrest as hm
import sqlalchemy as sa

from balance import constants as cst, mapper
import tests.object_builder as ob
from tests.tutils import has_exact_entries
# noinspection PyUnresolvedReferences
from tests.medium_tests.logic.upravlyator.conftest import (
    EXTERNAL_CLIENT_ROLE,
    assert_passport_roles,
    add_role_with_constraints,
    create_passport,
    create_role_client,
    create_role_client_group,
    make_staff_response,

    parametrize_constraints,
    get_fields_by_constraints,
    RoleTuple,
)

from balance.mapper import TVMACLApp, TVMACLGroupPermission, TVMACLGroup


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_from_domain_login(session, upravlyator, domain_passport, role, admsubscribe_mock,
                                            support_tvms, with_tvms):
    support_tvms(with_tvms)

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_from_domain_login(session, upravlyator, domain_passport, roles, admsubscribe_mock,
                                       support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role2))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role2.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role1.id)
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_not_like_passports_from_domain_login(session, upravlyator, domain_passport,
                                                          yndx_passports, roles, role, admsubscribe_mock,
                                                          support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    yndx_passport1, yndx_passport2 = yndx_passports

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    yndx_passport1.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role2))
    yndx_passport2.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport2, role=role2))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role1.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role.id)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE, role2.id)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE, role2.id)
    assert yndx_passport1.master == domain_passport
    assert yndx_passport2.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_not_like_passports_from_domain_login(session, upravlyator, domain_passport,
                                                               yndx_passports, roles, admsubscribe_mock,
                                                               support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    yndx_passport1, yndx_passport2 = yndx_passports

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    yndx_passport1.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role2))
    yndx_passport2.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport2, role=role2))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role1.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE, role2.id)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE, role2.id)
    assert yndx_passport1.master == domain_passport
    assert yndx_passport2.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_like_passport_from_domain_login(session, upravlyator, domain_passport,
                                                     yndx_passports, roles, admsubscribe_mock,
                                                     support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    yndx_passport1, yndx_passport2 = yndx_passports

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role2))
    yndx_passport1.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role1))
    yndx_passport2.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport2, role=role1))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role1.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role2.id)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE, role1.id)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE, role1.id)
    assert yndx_passport1.master == domain_passport
    assert yndx_passport2.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_like_passport_from_domain_login(session, upravlyator, domain_passport,
                                                          yndx_passports, role, admsubscribe_mock,
                                                          support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport1, yndx_passport2 = yndx_passports

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    yndx_passport1.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role))
    yndx_passport2.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport2, role=role))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE, role.id)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE, role.id)
    assert yndx_passport1.master == domain_passport
    assert yndx_passport2.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_roles_from_domain_login_with_bound_passports(session, upravlyator, domain_passport,
                                                                  yndx_passports, role, admsubscribe_mock,
                                                                  support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport1, yndx_passport2 = yndx_passports

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    yndx_passport1.master = domain_passport
    yndx_passport2.master = domain_passport
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport1.master is None
    assert yndx_passport2.master is None

    def kf(d):
        return d['passport'].passport_id

    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_roles_from_domain_login_with_bound_passports(session, upravlyator, domain_passport,
                                                             yndx_passports, roles, admsubscribe_mock,
                                                             support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    yndx_passport1, yndx_passport2 = yndx_passports

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role2))
    yndx_passport1.master = domain_passport
    yndx_passport2.master = domain_passport
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role2.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role1.id)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport1.master == domain_passport
    assert yndx_passport2.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_like_passport_with_bound_passport_from_domain_login(session, upravlyator,
                                                                              domain_passport, yndx_passports,
                                                                              roles, admsubscribe_mock,
                                                                              support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    yndx_passport1, yndx_passport2 = yndx_passports

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    yndx_passport1.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role2))
    yndx_passport2.master = domain_passport
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role1.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE, role2.id)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport1.master == domain_passport
    assert yndx_passport2.master is None
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_from_passport_login_domain_withot_role(session, upravlyator, domain_passport,
                                                                 yndx_passport, role, admsubscribe_mock,
                                                                 support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master is None
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_from_passport_login_domain_withot_role(session, upravlyator, domain_passport,
                                                            yndx_passport, roles, admsubscribe_mock,
                                                            support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role2))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role1.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role2.id)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_like_domain_from_passport(session, upravlyator, domain_passport,
                                                    yndx_passport, role, admsubscribe_mock,
                                                    support_tvms, with_tvms):
    support_tvms(with_tvms)

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role.id)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master is None
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_like_domain_from_passport_login(session, upravlyator, domain_passport, yndx_passport,
                                                     roles, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role2))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role1.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role1.id)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role2.id)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_like_passport_from_passport_login(session, upravlyator, domain_passport,
                                                            yndx_passports, role, admsubscribe_mock,
                                                            support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport1, yndx_passport2 = yndx_passports

    yndx_passport1.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role))
    yndx_passport2.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport2, role=role))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport1.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE, role.id)
    assert yndx_passport1.master is None
    assert yndx_passport2.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_like_passport_from_passport_login(session, upravlyator,
                                                       domain_passport, yndx_passports, roles,
                                                       admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles
    yndx_passport1, yndx_passport2 = yndx_passports

    yndx_passport1.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role1))
    session.add(mapper.RealRolePassport(passport=yndx_passport1, role=role2))
    yndx_passport2.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport2, role=role1))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role1.id)},
        fields={'passport-login': yndx_passport1.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport1, EXTERNAL_CLIENT_ROLE, role2.id)
    assert_passport_roles(yndx_passport2, EXTERNAL_CLIENT_ROLE, role1.id)
    assert yndx_passport1.master == domain_passport
    assert yndx_passport2.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_unexist_role_from_domain_login(session, upravlyator, domain_passport, role,
                                               admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'warning': u'У пользователя нет роли %s с ограничениями {}' % role.id,
        }),
    )
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_unexist_anywhere_role_from_domain_login(session, upravlyator, domain_passport, role,
                                                        admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str('9999')},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0, 'warning': u'Роль не найдена'}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_unexist_role_from_domain_login_with_bounded_passport(session, upravlyator, domain_passport,
                                                                     yndx_passport, role, admsubscribe_mock,
                                                                     support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'warning': u'У пользователя нет роли %s с ограничениями {}' % role.id,
        }),
    )
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_unexist_role_from_passport_login(session, upravlyator, domain_passport, yndx_passport, role,
                                                 admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    msg = u'У пользователя нет роли %s с ограничениями {u\'passport-login\': u\'%s\'}' % (
        role.id, yndx_passport.login.encode('utf-8'))
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'warning': msg,
        }),
    )
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_unexist_anywhere_role_from_passport_login(session, upravlyator, domain_passport,
                                                          yndx_passport, role, admsubscribe_mock,
                                                          support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str('9999')},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0, 'warning': u'Роль не найдена'}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_removed_unexist_domain_role_from_passport_login(session, upravlyator, domain_passport, yndx_passport,
                                                         roles, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role2))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role1.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    msg = u'У пользователя нет роли %s с ограничениями {u\'passport-login\': u\'%s\'}' %\
          (role1.id, yndx_passport.login.encode('utf-8'))
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'warning': msg,
        }),
    )
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role1.id)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, role2.id)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_passport_role_with_wrong_domain(session, upravlyator, domain_passport, yndx_passport, role,
                                                admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    alt_domain = create_passport(session, 'alt-login', is_internal=True)

    yndx_passport.master = alt_domain
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )

    msg = u'Паспортный логин %s привязан к другому доменному логину %s!' % (yndx_passport.login, alt_domain.login)
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.WrongLinkedPassport(%s)' % msg.encode('utf-8'),
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_from_unexist_domain_login(session, upravlyator, role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    _login = 'UnExisTiK'
    res = upravlyator.remove_role(
        login=_login,
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert res == {
        'code': 0,
        'warning': u'Пользователь не найден: {}'.format(_login)
    }


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_from_unexist_passport_login(session, upravlyator, domain_passport, role,
                                                 admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    _login = 'UnExisTiK'

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'passport-login': _login},
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert res == {
        'code': 0,
        'warning': u'Пользователь не найден: {}'.format(_login)
    }
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role.id)
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_old_behaviour_remove_only_role_from_bound_passport_login(session, upravlyator, domain_passport,
                                                                  yndx_passport, role, admsubscribe_mock,
                                                                  support_tvms, with_tvms):
    support_tvms(with_tvms)

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    yndx_passport.master = domain_passport
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master is None
    assert admsubscribe_mock == []


@pytest.mark.parametrize('with_tvms', [True, False])
def test_old_behaviour_remove_role_from_bound_passport_login(session, upravlyator, domain_passport,
                                                             yndx_passport, roles, admsubscribe_mock,
                                                             support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role2))
    yndx_passport.master = domain_passport
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role2.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE, role1.id)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_with_constraint_from_domain_login(constraints, session, upravlyator, domain_passport, role,
                                                       admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    constraint_fields = get_fields_by_constraints(session, constraints)
    pr = [pr for pr in domain_passport.real_passport_roles if pr.role == role][0]
    for field, value in constraint_fields.items():
        setattr(pr, field, value)
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    assert role not in domain_passport.real_roles

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert admsubscribe_mock == []


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_with_constraint_from_passport_login(constraints, session, upravlyator, domain_passport,
                                                              yndx_passport, role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    constraint_fields = get_fields_by_constraints(session, constraints)
    pr = [pr for pr in yndx_passport.real_passport_roles if pr.role == role][0]
    for field, value in constraint_fields.items():
        setattr(pr, field, value)
    session.flush()

    fields = dict(constraint_fields, **{'passport-login': yndx_passport.login})

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master is None
    assert admsubscribe_mock == []


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_with_constraint_like_passport_from_domain_login(constraints, session, upravlyator, domain_passport,
                                                                     role, admsubscribe_mock, yndx_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    constraint_fields = get_fields_by_constraints(session, constraints)
    pr = [pr for pr in domain_passport.real_passport_roles if pr.role == role][0]
    for field, value in constraint_fields.items():
        setattr(pr, field, value)
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    pr = [pr_ for pr_ in yndx_passport.real_passport_roles if pr_.role == role][0]
    for field, value in constraint_fields.items():
        setattr(pr, field, value)
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE,
                          RoleTuple(role_id=role.id, **constraint_fields))
    assert admsubscribe_mock == []


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_with_constraint_like_domain_from_passport_login(constraints, session, upravlyator, domain_passport,
                                                                     role, admsubscribe_mock, yndx_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    constraint_fields = get_fields_by_constraints(session, constraints)
    pr = [pr for pr in domain_passport.real_passport_roles if pr.role == role][0]
    for field, value in constraint_fields.items():
        setattr(pr, field, value)
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    pr = [pr_ for pr_ in yndx_passport.real_passport_roles if pr_.role == role][0]
    for field, value in constraint_fields.items():
        setattr(pr, field, value)
    session.flush()

    fields = dict(constraint_fields, **{'passport-login': yndx_passport.login})

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE,
                          RoleTuple(role.id, **constraint_fields))
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert admsubscribe_mock == []


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_with_one_of_constraints_from_domain_login(constraints, session, upravlyator, domain_passport, role,
                                                               admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)
    constraint_fields_to_remove = get_fields_by_constraints(session, constraints)

    domain_passport.set_roles([(role, constraint_fields),
                               (role, constraint_fields_to_remove)])
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=constraint_fields_to_remove,
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, RoleTuple(role_id=role.id, **constraint_fields))
    assert admsubscribe_mock == []


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_with_one_of_constraints_from_passport_login(constraints, session, upravlyator,
                                                                      domain_passport, yndx_passport, role,
                                                                      admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)
    constraint_fields_to_remove = get_fields_by_constraints(session, constraints)

    yndx_passport.master = domain_passport
    yndx_passport.set_roles([(role, constraint_fields),
                             (role, constraint_fields_to_remove)])
    session.flush()

    assert admsubscribe_mock == [{'passport': yndx_passport, 'unsubscribe': False}]

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=dict(constraint_fields_to_remove, **{'passport-login': yndx_passport.login}),
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, RoleTuple(role_id=role.id, **constraint_fields))
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == [{'passport': yndx_passport, 'unsubscribe': False}]


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_with_one_of_constraint_from_domain_login_1(constraints, session, upravlyator, domain_passport,
                                                                role, admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)
    domain_passport.set_roles([role,
                               (role, constraint_fields)])
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, role.id)
    assert admsubscribe_mock == []


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_with_one_of_constraint_from_passport_login_1(constraints, session, upravlyator,
                                                                       domain_passport,
                                                                       yndx_passport, role, admsubscribe_mock,
                                                                       support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)

    yndx_passport.master = domain_passport
    yndx_passport.set_roles([role,
                             (role, constraint_fields)])
    session.flush()

    assert admsubscribe_mock == [{'passport': yndx_passport, 'unsubscribe': False}]

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=dict(constraint_fields, **{'passport-login': yndx_passport.login}),
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, role.id)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == [{'passport': yndx_passport, 'unsubscribe': False}]


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_with_one_of_constraint_from_domain_login_2(constraints, session, upravlyator, domain_passport,
                                                                role, admsubscribe_mock,
                                                                support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)

    domain_passport.set_roles([role,
                               (role, constraint_fields)])
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, RoleTuple(role_id=role.id, **constraint_fields))
    assert admsubscribe_mock == []


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_only_role_with_one_of_constraint_from_passport_login_2(constraints, session, upravlyator,
                                                                       domain_passport,
                                                                       yndx_passport, role,
                                                                       admsubscribe_mock, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)

    yndx_passport.master = domain_passport
    yndx_passport.set_roles([role,
                             (role, constraint_fields)])
    session.flush()

    assert admsubscribe_mock == [{'passport': yndx_passport, 'unsubscribe': False}]

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, RoleTuple(role_id=role.id, **constraint_fields))
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == [{'passport': yndx_passport, 'unsubscribe': False}]


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_with_wrong_constraint(constraints, session, upravlyator, domain_passport, role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    constraint_fields = get_fields_by_constraints(session, constraints)
    wrong_constraint_fields = get_fields_by_constraints(session, constraints)

    add_role_with_constraints(domain_passport, role, constraint_fields)
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=wrong_constraint_fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'warning': u'У пользователя нет роли {} с ограничениями {}'.format(
                role.id,
                {unicode(k): unicode(v) if isinstance(v, basestring) else v for k, v in wrong_constraint_fields.items()}
            ),
        }),
    )


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_invalid_constraint_field(constraints, session, upravlyator, role, domain_passport,
                                              support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Роль не поддерживает ограничение по firm_id/template_group_id"""
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.flush()

    constraint_fields = get_fields_by_constraints(session, constraints)

    res = upravlyator.remove_role(
        login=domain_passport.login,
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


@pytest.mark.parametrize(
    'w_group_role',
    [True, False],
)
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_w_group_role(session, upravlyator, role, domain_passport, yndx_passport, w_group_role,
                                  support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Удаляем роль и проверяем разрывается ли связь между паспортом и доменом"""
    yndx_passport.master = domain_passport

    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    session.add(mapper.PassportGroup(passport=yndx_passport, group_id=666))
    if w_group_role:
        session.add(mapper.RoleGroup(session, group_id=666, role_id=role.id))

    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'passport-login': yndx_passport.login},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(res, has_exact_entries(code=0))

    required_passport_roles = [hm.has_properties(role_id=EXTERNAL_CLIENT_ROLE)]
    if w_group_role:
        required_passport_roles.append(hm.has_properties(role_id=role.id, group_id=666))

    hm.assert_that(yndx_passport.passport_roles, hm.contains(*required_passport_roles))
    assert (yndx_passport.master == domain_passport) is w_group_role


@pytest.mark.parametrize(
    'type_',
    ['', 'w_fields', 'str'],
)
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_one_role_from_group(session, upravlyator, roles, domain_passport, yndx_passport, type_, support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    role1, role2 = roles
    role1.fields.append('abc_clients')

    session.add(mapper.PassportGroup(group_id=666, passport=yndx_passport))
    session.add(mapper.RoleGroup(session, group_id=666, role_id=role1.id))
    session.add(mapper.RoleGroup(session, group_id=666, role_id=role2.id))
    session.flush()

    kw = {
        'group': '666' if type_ == 'str' else 666,
        'role': {'role': str(role1.id)},
    }

    if with_tvms:
        kw['subject_type'] = 'user'

    if type_ == 'w_fields':
        kw['fields'] = {'abc_clients': False}

    res = upravlyator.remove_role(**kw)
    hm.assert_that(res, has_exact_entries(code=0))

    assert yndx_passport.master is domain_passport
    hm.assert_that(
        yndx_passport.passport_roles,
        hm.contains(
            hm.has_properties(role_id=EXTERNAL_CLIENT_ROLE),
            hm.has_properties(role_id=role2.id, group_id=666),
        ),
    )


@pytest.mark.parametrize(
    'w_real_role',
    [True, False],
)
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_role_w_real_role(session, upravlyator, role, domain_passport, yndx_passport, w_real_role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Удаляем роль и проверяем разрывается ли связь между паспортом и доменом"""
    role.fields.append('client_id')
    yndx_passport.master = domain_passport

    session.add(mapper.PassportGroup(passport=yndx_passport, group_id=666))
    session.add(mapper.RoleGroup(session, group_id=666, role_id=role.id))
    if w_real_role:
        session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))

    session.flush()

    res = upravlyator.remove_role(
        group=666,
        role={'role': str(role.id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(res, has_exact_entries(code=0))

    required_passport_roles = [hm.has_properties(role_id=EXTERNAL_CLIENT_ROLE)]
    if w_real_role:
        required_passport_roles.append(hm.has_properties(role_id=role.id, group_id=None))

    hm.assert_that(yndx_passport.passport_roles, hm.contains_inanyorder(*required_passport_roles))
    assert (yndx_passport.master == domain_passport) is True


@pytest.mark.parametrize(
    'is_group',
    [True, False],
)
@pytest.mark.parametrize('with_tvms', [True, False])
def test_invalid_client_format(upravlyator, role, domain_passport, is_group, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(['client_id', 'abc_clients'])
    kwargs = {
        'login': domain_passport.login,
        'group': 666,
        'role': {'role': str(role.id)},
        'fields': {
            'abc_clients': True,
            'client_id': '666',
        },
    }

    if with_tvms:
        kwargs['subject_type'] = 'user'

    del kwargs['group' if not is_group else 'login']

    res = upravlyator.remove_role(**kwargs)
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidField(field_names=%s)' % ('client_id' if is_group else 'abc_clients'),
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_w_pin_from_domain(session, upravlyator, domain_passport, role, admsubscribe_mock, role_client, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    session.add(mapper.RealRolePassport(passport=domain_passport,
                role=role, client_batch_id=role_client.client_batch_id))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields={'client_id': str(role_client.client_id)},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert admsubscribe_mock == []
    assert session.query(mapper.RoleClient).filter_by(client_id=role_client.client_id).count() == 1


@pytest.mark.parametrize(
    'is_yndx',
    [True, False],
)
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_w_pin(session, upravlyator, domain_passport, yndx_passport, role, admsubscribe_mock, role_client, is_yndx,
                      support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(
        passport=yndx_passport if is_yndx else domain_passport,
        role=role,
        client_batch_id=role_client.client_batch_id,
    ))
    session.flush()

    fields = {'client_id': str(role_client.client_id)}
    if is_yndx:
        fields['passport-login'] = yndx_passport.login

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master is None
    assert admsubscribe_mock == []
    assert session.query(mapper.RoleClient).filter_by(client_id=role_client.client_id).count() == 1


@pytest.mark.parametrize(
    'is_yndx',
    [True, False],
)
@pytest.mark.parametrize('with_tvms', [True, False])
def test_unexist_role_w_pin(session, upravlyator, domain_passport, yndx_passport, role, admsubscribe_mock, role_client, is_yndx,
                            support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    yndx_passport.master = domain_passport
    session.flush()

    fields = {u'client_id': unicode(role_client.client_id)}
    if is_yndx:
        fields[u'passport-login'] = unicode(yndx_passport.login)

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'warning': u'У пользователя нет роли %s с ограничениями %s' % (role.id, fields),
        }),
    )
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)
    assert yndx_passport.master == domain_passport
    assert admsubscribe_mock == []
    assert session.query(mapper.RoleClient).filter_by(
        client_id=role_client.client_id).count() == 1  # role_client, созданные тестом


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_remove_from_yndx_w_pin_n_constraint(constraints, session, upravlyator, domain_passport, yndx_passport,
                                             role, admsubscribe_mock, role_client, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(constraints)
    role.fields.append('client_id')
    constraint_fields = get_fields_by_constraints(session, constraints)

    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(
        passport=yndx_passport,
        role=role,
        **constraint_fields
    ))
    session.add(mapper.RealRolePassport(
        passport=yndx_passport,
        role=role,
        client_batch_id=role_client.client_batch_id,
        **constraint_fields
    ))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=dict(constraint_fields, **{
            'passport-login': yndx_passport.login,
            'client_id': str(role_client.client_id),
        }),
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE, RoleTuple(role.id, **constraint_fields))
    assert yndx_passport.master is domain_passport
    assert admsubscribe_mock == []
    assert session.query(mapper.RoleClient).filter_by(client_id=role_client.client_id).count() == 1


@pytest.mark.parametrize('with_tvms', [True, False])
def test_client_doesnt_found(session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Не нашли клиента, нет role_client => нет роли"""
    role.fields.append('client_id')
    client_id = (session.execute(sa.func.next_value(sa.Sequence('s_client_id'))).scalar() + 10)
    fields = {u'client_id': unicode(client_id)}
    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'warning': u'У пользователя нет роли %s с ограничениями %s' % (role.id, fields),
        }),
    )
    assert session.query(mapper.RoleClient).filter_by(client_id=client_id).count() == 0


@pytest.mark.parametrize('with_tvms', [True, False])
def test_client_invalid_client_id(upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    res = upravlyator.remove_role(
        login=domain_passport.login,
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
def test_client_batch_id_doesn_exist(upravlyator, role, domain_passport, client, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('client_id')
    fields = {u'client_id': unicode(client.id)}
    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        fields=fields,
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'warning': u'У пользователя нет роли %s с ограничениями %s' % (role.id, fields),
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

    res = upravlyator.remove_role(
        login=domain_passport.login,
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
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_abc_clients(_mock_staff, session, upravlyator, role, domain_passport, role_client_group, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=role_client_group.external_id))
    session.add(mapper.RoleGroup(
        group_id=role_client_group.external_id,
        role_id=role.id,
        client_batch_id=role_client_group.client_batch_id,
    ))
    session.flush()

    make_staff_response(role_client_group.external_id)

    res = upravlyator.remove_role(
        group=role_client_group.external_id,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    session.expire_all()

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
        }),
    )
    assert_passport_roles(domain_passport, (EXTERNAL_CLIENT_ROLE))
    assert session.query(mapper.RoleClientGroup).filter_by(external_id=role_client_group.external_id).count() == 1
    assert session.query(mapper.PassportGroup).filter_by(group_id=role_client_group.external_id).count() == 1
    assert session.query(mapper.RoleGroup).filter_by(group_id=role_client_group.external_id).count() == 0


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_abc_clients_doesnt_found(_mock_staff, session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=666))
    session.flush()

    make_staff_response(666)

    res = upravlyator.remove_role(
        group=666,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'warning': u'Нет группы клиентов 666',
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
@parametrize_constraints
def test_abc_clients_w_yndx_passport(_mock_staff, constraints, session, upravlyator, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Пользователь привязан к 2м группам с разным набором клиентов и фирм/шаблонов."""
    role = ob.create_role(
        session,
        ('PERM', dict({cst.ConstraintTypes.client_batch_id: None}, **{c: None for c in constraints})),
    )
    role.fields = ['abc_clients'] + constraints
    group_1 = create_role_client_group(session)
    group_2 = create_role_client_group(session)

    constraint_fields = get_fields_by_constraints(session, constraints)
    constraint_fields_to_delete = get_fields_by_constraints(session, constraints)

    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=group_1.external_id))
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=group_2.external_id))
    session.add(mapper.RoleGroup(
        group_id=group_1.external_id,
        role_id=role.id,
        client_batch_id=group_1.client_batch_id,
    ))
    session.add(mapper.RoleGroup(
        group_id=group_1.external_id,
        role_id=role.id,
        client_batch_id=group_1.client_batch_id,
        **constraint_fields
    ))
    session.add(mapper.RoleGroup(
        group_id=group_2.external_id,
        role_id=role.id,
        client_batch_id=group_2.client_batch_id,
        **constraint_fields
    ))
    session.add(mapper.RoleGroup(
        group_id=group_2.external_id,
        role_id=role.id,
        client_batch_id=group_2.client_batch_id,
        **constraint_fields_to_delete
    ))
    session.add(mapper.RoleGroup(
        group_id=group_2.external_id,
        role_id=role.id,
        client_batch_id=group_2.client_batch_id,
    ))
    session.flush()

    make_staff_response(group_2.external_id)

    res = upravlyator.remove_role(
        group=group_2.external_id,
        role={'role': str(role.id)},
        fields=dict(constraint_fields_to_delete, **{'abc_clients': True}),
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
        }),
    )
    res = upravlyator.remove_role(
        group=group_2.external_id,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
        }),
    )

    assert_passport_roles(
        domain_passport,
        EXTERNAL_CLIENT_ROLE,
        RoleTuple(role_id=role.id, group_id=group_1.external_id, client_ids=group_1.client_ids),
        RoleTuple(role_id=role.id, group_id=group_1.external_id, client_ids=group_1.client_ids, **constraint_fields),
        RoleTuple(role_id=role.id, group_id=group_2.external_id, client_ids=group_2.client_ids, **constraint_fields),
    )

    # если сравниваем по двум полям - то добавится дополнительное разрешение на группу
    # т.к оно не будет отфильтровано по вложенным кобминациям, см get_perm_constraints
    constraint_matchers = [
        has_exact_entries(dict({k: hm.contains(v) for k, v in constraint_fields.items()}, **{
            cst.ConstraintTypes.client_batch_id: hm.contains(batch_id),
        })) for batch_id, _ in zip([group_2.client_batch_id, group_1.client_batch_id], constraints)]

    hm.assert_that(
        domain_passport.get_perm_constraints('PERM'),
        hm.contains_inanyorder(
            has_exact_entries({
                cst.ConstraintTypes.client_batch_id: hm.contains(group_1.client_batch_id),
            }),
            *constraint_matchers
        ),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_abc_client_false_doesnt_matter(session, upravlyator, role, domain_passport, role_client, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(['client_id', 'abc_clients'])
    session.add(mapper.RealRolePassport(passport=domain_passport,
                role=role, client_batch_id=role_client.client_batch_id))
    session.flush()

    kwargs = {
        'login': domain_passport.login,
        'role': {'role': str(role.id)},
        'fields': {
            'abc_clients': False,
            'client_id': str(role_client.client_id),
        },
    }

    if with_tvms:
        kwargs['subject_type'] = 'user'

    res = upravlyator.remove_role(**kwargs)
    assert res == {'code': 0}
    assert_passport_roles(domain_passport, EXTERNAL_CLIENT_ROLE)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_require_login_or_group(upravlyator, role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.remove_role(
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


@pytest.mark.parametrize(
    'w_yndx',
    [
        pytest.param(True, id='yndx_passport'),
        pytest.param(False, id='domain_login'),
    ],
)
@pytest.mark.parametrize(
    'from_group',
    [
        pytest.param(True, id='from group'),
        pytest.param(False, id='from passport'),
    ],
)
@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_remove_client_related_role(_mock_staff, session, upravlyator, domain_passport, yndx_passport, role, w_yndx, client, from_group,
                                    support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.extend(['client_id', 'abc_clients'])
    passport = domain_passport
    if w_yndx:
        yndx_passport.master = domain_passport
        passport = yndx_passport

    group = create_role_client_group(session, clients=[client])
    session.add(mapper.PassportGroup(group_id=group.external_id, passport_id=passport.passport_id))
    session.add(mapper.RoleGroup(group_id=group.external_id, role=role, client_batch_id=group.client_batch_id))
    role_client = ob.RoleClientBuilder.construct(session, client=client)
    session.add(mapper.RealRolePassport(passport_id=passport.passport_id,
                role=role, client_batch_id=role_client.client_batch_id))
    session.flush()
    assert_passport_roles(
        passport,
        EXTERNAL_CLIENT_ROLE,
        (role.id, None, group.external_id, [client.id]),
        (role.id, None, None, [client.id]),
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

    if with_tvms:
        params['subject_type'] = 'user'

    if from_group:
        del fields[u'client_id']
        del params['login']
    else:
        del fields[u'abc_clients']
        del params['group']
    if w_yndx:
        fields[u'passport-login'] = yndx_passport.login
    res = upravlyator.remove_role(**params)
    assert res == {'code': 0}

    session.refresh(passport)
    match_role = (role.id, None, None if from_group else group.external_id, [client.id])
    assert_passport_roles(
        passport,
        EXTERNAL_CLIENT_ROLE,
        match_role,
    )

    role_clients = session.query(mapper.RoleClient).filter_by(client_id=client.id).all()
    assert len(role_clients) == 2


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_different_group_id_and_abc_service_id(_mock_tvm, session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role.fields.append('abc_clients')
    group_id = 666
    group = create_role_client_group(session)
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=group_id))
    session.add(mapper.RoleGroup(group_id=group_id, role=role, client_batch_id=group.client_batch_id))
    session.flush()

    make_staff_response(group.external_id)

    res = upravlyator.remove_role(
        group=group_id,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        has_exact_entries({'code': 0}),
    )

    assert_passport_roles(
        domain_passport,
        (EXTERNAL_CLIENT_ROLE,),
    )
    assert session.query(mapper.RoleClientGroup).filter_by(external_id=group.external_id).one()


@pytest.mark.parametrize('with_tvms', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_wrong_abc_service_id(_mock_tvm, session, upravlyator, role, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    """Несмотря на то, что роль существует, если staff нам не скажет service_id,
    то мы не сможем определить client_batch_id и найти правльную роль для удаления
    """
    role.fields.append('abc_clients')
    group_id = 666
    group = create_role_client_group(session)
    session.add(mapper.PassportGroup(passport_id=domain_passport.passport_id, group_id=group_id))
    session.add(mapper.RoleGroup(group_id=group_id, role=role, client_batch_id=group.client_batch_id))
    session.flush()

    make_staff_response(error_dict={'error': 'invalid request'})

    res = upravlyator.remove_role(
        group=group_id,
        role={'role': str(role.id)},
        fields={'abc_clients': True},
        **({'subject_type': 'user'} if with_tvms else {})
    )
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidFieldValue(field_name=%s, value=%s)'
                     % ('abc_clients', group_id),
        }),
    )


def test_disable_tvm(session, upravlyator, support_tvms):
    support_tvms(False)

    res = upravlyator.remove_role(
        login='test_login',
        role={'role': 'tvms_roles', 'tvms_roles': 'example_id'},
        subject_type='tvm_app'
    )

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'error': 'Unavailable TVMs'
        })
    )


def test_tvm_not_set_login(session, upravlyator, support_tvms):
    support_tvms(True)

    role = TVMACLGroup(name='group_as_role')
    session.add(role)
    session.flush()

    res = upravlyator.remove_role(
        group='test',
        role={'role': 'tvms_roles', 'tvms_roles': role.name},
        fields={'env': 'test'},
        subject_type='tvm_app'
    )

    assert res['code'] == 1
    assert res['error'] == u'Require login'


def test_tvm_role_not_found(session, upravlyator, support_tvms):
    support_tvms(True)

    res = upravlyator.remove_role(
        login='666',
        role={'role': 'tvms_roles', 'tvms_roles': 'not_found'},
        fields={'env': 'test'},
        subject_type='tvm_app'
    )

    assert res == {'code': 0, 'warning': u'Роль не найдена'}


def test_tvm_id_not_found(session, upravlyator, support_tvms):
    support_tvms(True)

    group = TVMACLGroup(name='group_as_role')
    session.add(group)
    session.flush()

    res = upravlyator.remove_role(
        login='666',
        role={'role': 'tvms_roles', 'tvms_roles': group.name},
        fields={'env': 'test'},
        subject_type='tvm_app'
    )

    assert res == {'code': 0, 'warning': u'Не найден TVM с id=%s в среде %s' % ('666', 'test')}


def test_tvm_env_not_found(session, upravlyator, support_tvms):
    support_tvms(True)

    tvm = TVMACLApp(tvm_id=666, env='test')
    session.add(tvm)

    group = TVMACLGroup(name='group_as_role')
    session.add(group)
    session.flush()

    res = upravlyator.remove_role(
        login=tvm.tvm_id,
        role={'role': 'tvms_roles', 'tvms_roles': group.name},
        fields={'env': 'prod'},
        subject_type='tvm_app'
    )

    assert res == {'code': 0, 'warning': u'Не найден TVM с id=%s в среде %s' % (tvm.tvm_id, 'prod')}


def test_tvm_invalid_fields_error(session, upravlyator, support_tvms):
    support_tvms(True)

    tvm = TVMACLApp(tvm_id=666, env='test')
    session.add(tvm)

    group = TVMACLGroup(name='group_as_role')
    session.add(group)
    session.flush()

    res = upravlyator.remove_role(
        login=tvm.tvm_id,
        role={'role': 'tvms_roles', 'tvms_roles': group.name},
        fields={},
        subject_type='tvm_app'
    )

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'fatal': 'Upravlyator.InvalidField(field_names=env)',
        }),
    )


def test_tvm_not_in_group(session, upravlyator, support_tvms):
    support_tvms(True)

    tvm = TVMACLApp(tvm_id=666, env='test')
    session.add(tvm)
    session.flush()

    tvm_group = TVMACLGroup(name='group_as_role')
    session.add(tvm_group)
    session.flush()

    fields = {'env': tvm.env}
    res = upravlyator.remove_role(
        login=tvm.tvm_id,
        role={'role': 'tvms_roles', 'tvms_roles': tvm_group.name},
        subject_type='tvm_app',
        fields=fields,
    )

    assert res == {'code': 0, 'warning': u'TVM %s не состоит в группе %s' % (tvm.tvm_id, tvm_group.name)}


def test_tvm_remove_in_group(session, upravlyator, support_tvms):
    support_tvms(True)

    tvm = TVMACLApp(tvm_id=666, env='test')
    session.add(tvm)
    session.flush()

    tvm_group = TVMACLGroup(name='group_as_role')
    session.add(tvm_group)
    session.flush()

    session.add(TVMACLGroupPermission(tvm_id=tvm.tvm_id, group_name=tvm_group.name))
    session.flush()

    fields = {'env': tvm.env}
    res = upravlyator.remove_role(
        login=tvm.tvm_id,
        role={'role': 'tvms_roles', 'tvms_roles': tvm_group.name},
        subject_type='tvm_app',
        fields=fields,
    )

    assert res == {'code': 0}

    count = session.query(TVMACLGroupPermission).filter(sa.and_(
        TVMACLGroupPermission.group_name == tvm_group.name,
        TVMACLGroupPermission.tvm_id == tvm.tvm_id
    )).count()

    assert count == 0


def test_remove_role_with_dublicates(session, upravlyator, domain_passport, role):
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.flush()

    res = upravlyator.remove_role(
        login=domain_passport.login,
        role={'role': str(role.id)},
        subject_type='user'
    )

    assert res == {'code': 0}

    res = session.query(mapper.RealRolePassport).filter(mapper.RealRolePassport.passport_id ==
                                                        domain_passport.passport_id, mapper.RealRolePassport.role_id == role.id).all()

    assert len(res) == 0

# -*- coding: utf-8 -*-
import pytest
import mock
import hamcrest as hm

from balance import mapper
from tests.tutils import has_exact_entries, mock_transactions

from tests.medium_tests.logic.upravlyator.conftest import EXTERNAL_CLIENT_ROLE, create_passport

GROUP_ID_1 = 111
GROUP_ID_2 = 333
GROUP_ID_3 = 666


def test_remove_domain_passport(session, upravlyator, domain_passport):
    session.add(mapper.PassportGroup(passport=domain_passport, group_id=GROUP_ID_1))
    session.flush()

    res = upravlyator.remove_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': '',
    }])
    hm.assert_that(res, has_exact_entries({'code': 0}))
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_1).count() == 0


def test_remove_yndx_passport(session, upravlyator, domain_passport, yndx_passport):
    yndx_passport.master = domain_passport
    session.add(mapper.PassportGroup(passport=yndx_passport, group_id=GROUP_ID_1))
    session.flush()

    res = upravlyator.remove_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': yndx_passport.login,
    }])
    hm.assert_that(res, has_exact_entries({'code': 0}))
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport, group_id=GROUP_ID_1).count() == 0


@pytest.mark.parametrize(
    'type_', ['login', 'passport_login']
)
@mock.patch('butils.passport.PassportBlackbox._call_api', return_value={'error': '404'})
def test_passport_not_found(session, upravlyator, domain_passport, yndx_passport, type_):
    yndx_passport.master = domain_passport
    session.flush()

    data = {
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': yndx_passport.login,
    }
    data[type_] = 'nonexistent_user_login'

    res = upravlyator.remove_batch_memberships(data=[data])
    hm.assert_that(res, has_exact_entries(code=0))


@pytest.mark.parametrize(
    'is_domain',
    [True, False],
)
def test_not_found(session, upravlyator, domain_passport, yndx_passport, is_domain):
    yndx_passport.master = domain_passport
    session.flush()

    res = upravlyator.remove_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': '' if is_domain else yndx_passport.login,
    }])
    hm.assert_that(res, has_exact_entries(code=0))


def test_wrong_linked_passport(session, upravlyator):
    domain_passport_1 = create_passport(session, login='login_1', is_internal=True)
    domain_passport_2 = create_passport(session, login='login_2', is_internal=True)

    yndx_passport = create_passport(session, 'yndx-%s' % domain_passport_1.login)
    yndx_passport.master = domain_passport_1
    session.flush()

    res = upravlyator.remove_batch_memberships(data=[{
        'login': domain_passport_2.login,
        'group': str(GROUP_ID_1),
        'passport_login': yndx_passport.login,
    }])
    msg = u'Паспортный логин %s привязан к другому доменному логину %s!' % (yndx_passport.login, domain_passport_1.login)
    error_dict = {
        u'login': domain_passport_2.login,
        u'group': unicode(GROUP_ID_1),
        u'passport_login': yndx_passport.login,
        'error': 'Upravlyator.WrongLinkedPassport(%s)' % msg.encode('utf-8'),
    }
    error = 'Upravlyator.MultiStatusError(%s)' % [error_dict]
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'error': error,
        }),
    )


@pytest.mark.parametrize(
    'is_domain',
    [True, False],
)
def test_several_rows(session, upravlyator, domain_passport, yndx_passport, is_domain):
    yndx_passport.master = domain_passport
    session.add(mapper.PassportGroup(passport=domain_passport, group_id=GROUP_ID_1))
    session.add(mapper.PassportGroup(passport=yndx_passport, group_id=GROUP_ID_1))
    session.flush()

    res = upravlyator.remove_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': yndx_passport.login if not is_domain else '',
    }])
    msg = u'Паспорт %s связан с группой %s несколько раз' % (domain_passport.passport_id, GROUP_ID_1)
    error_dict = {
        u'login': domain_passport.login,
        u'group': unicode(GROUP_ID_1),
        u'passport_login': yndx_passport.login if not is_domain else u'',
        'error': 'Upravlyator.TooManyPassportGroups(%s)' % msg.encode('utf-8'),
    }
    error = 'Upravlyator.MultiStatusError(%s)' % [error_dict]
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'error': error,
        }),
    )


def test_del_several_rows(session, upravlyator, domain_passport, yndx_passports, role):
    yndx_passport_1, yndx_passport_2 = yndx_passports

    yndx_passport_1.master = domain_passport
    yndx_passport_2.master = domain_passport
    session.add(mapper.PassportGroup(passport=yndx_passport_1, group_id=GROUP_ID_1))
    session.add(mapper.PassportGroup(passport=yndx_passport_1, group_id=GROUP_ID_2))
    session.add(mapper.PassportGroup(passport=yndx_passport_2, group_id=GROUP_ID_3))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID_1, role_id=role.id))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID_2, role_id=role.id))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID_3, role_id=role.id))
    session.flush()

    res = upravlyator.remove_batch_memberships(data=[
        {
            'login': domain_passport.login,
            'group': str(GROUP_ID_1),
            'passport_login': yndx_passport_1.login,
        },
        {
            'login': domain_passport.login,
            'group': str(GROUP_ID_2),
            'passport_login': yndx_passport_1.login,
        },
        {
            'login': domain_passport.login,
            'group': str(GROUP_ID_3),
            'passport_login': yndx_passport_2.login,
        },
    ])
    hm.assert_that(res, has_exact_entries({'code': 0}))
    assert yndx_passport_1.master is None
    assert yndx_passport_2.master is None
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport_1, group_id=GROUP_ID_1).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport_1, group_id=GROUP_ID_2).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport_2, group_id=GROUP_ID_3).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_1).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_2).count() == 0


def test_del_half_group(session, upravlyator, yndx_passports, role):
    domain_passport_1 = create_passport(session, login='login_1', is_internal=True)
    domain_passport_2 = create_passport(session, login='login_2', is_internal=True)
    domain_passport_3 = create_passport(session, login='login_3', is_internal=True)
    domain_passport_4 = create_passport(session, login='login_4', is_internal=True)
    yndx_passport_1, yndx_passport_2 = yndx_passports

    yndx_passport_1.master = domain_passport_1
    yndx_passport_2.master = domain_passport_2

    session.add(mapper.PassportGroup(passport=yndx_passport_1, group_id=GROUP_ID_1))
    session.add(mapper.PassportGroup(passport=yndx_passport_2, group_id=GROUP_ID_1))
    session.add(mapper.PassportGroup(passport=domain_passport_3, group_id=GROUP_ID_1))
    session.add(mapper.PassportGroup(passport=domain_passport_4, group_id=GROUP_ID_1))
    session.flush()

    res = upravlyator.remove_batch_memberships(data=[
        {
            'login': domain_passport_1.login,
            'group': str(GROUP_ID_1),
            'passport_login': yndx_passport_1.login,
        },
        {
            'login': domain_passport_3.login,
            'group': str(GROUP_ID_1),
            'passport_login': '',
        },
    ])
    hm.assert_that(res, has_exact_entries({'code': 0}))
    hm.assert_that(
        session.query(mapper.PassportGroup).filter_by(group_id=GROUP_ID_1).all(),
        hm.contains_inanyorder(
            hm.has_properties(passport=yndx_passport_2),
            hm.has_properties(passport=domain_passport_4),
        ),
    )
    assert yndx_passport_1.master is None
    assert yndx_passport_2.master is domain_passport_2


def test_del_several_rows_w_207(session, upravlyator, domain_passport, yndx_passport, role):
    yndx_passport.master = domain_passport
    session.add(mapper.PassportGroup(passport=yndx_passport, group_id=GROUP_ID_1))
    session.add(mapper.PassportGroup(passport=yndx_passport, group_id=GROUP_ID_2))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID_1, role_id=role.id))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID_2, role_id=role.id))
    session.flush()

    with mock_transactions():
        res = upravlyator.remove_batch_memberships(data=[
            {
                'login': domain_passport.login,
                'group': str(GROUP_ID_1),
                'passport_login': yndx_passport.login,
            },
            {
                'login': domain_passport.login,
                'group': str(GROUP_ID_2),
                'passport_login': yndx_passport.login,  # связь с доменом будет разорвана
            },
            {
                'login': domain_passport.login,
                'group': str(GROUP_ID_3),
                'passport_login': yndx_passport.login,  # не сможет найти привязанный домен
            },
        ])

    msg = u'Паспортный логин %s привязан к другому доменному логину %s!' % (yndx_passport.login, None)
    error_dict = {
        u'login': domain_passport.login,
        u'group': unicode(GROUP_ID_3),
        u'passport_login': yndx_passport.login,
        'error': 'Upravlyator.WrongLinkedPassport(%s)' % msg.encode('utf-8'),
    }
    error = 'Upravlyator.MultiStatusError(%s)' % [error_dict]
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'error': error,
        }),
    )

    assert yndx_passport.master is domain_passport
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport, group_id=GROUP_ID_1).count() == 1
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport, group_id=GROUP_ID_2).count() == 1
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport, group_id=GROUP_ID_3).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_1).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_2).count() == 0


@pytest.mark.parametrize(
    'w_role',
    [True, False],
)
def test_remove_relation_w_master(session, upravlyator, role, domain_passport, yndx_passport, w_role):
    """Удаляем членство, и, если внешних ролей больше нет, то удаляем связь с доменом"""
    yndx_passport.master = domain_passport

    session.add(mapper.PassportGroup(passport=yndx_passport, group_id=GROUP_ID_1))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID_1, role_id=role.id))
    if w_role:
        session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))

    session.flush()

    res = upravlyator.remove_batch_memberships(data=[
        {
            'login': domain_passport.login,
            'group': str(GROUP_ID_1),
            'passport_login': yndx_passport.login,
        },
    ])
    hm.assert_that(res, has_exact_entries(code=0))

    required_passport_roles = [hm.has_properties(role_id=EXTERNAL_CLIENT_ROLE)]
    if w_role:
        required_passport_roles.append(hm.has_properties(role_id=role.id, group_id=None))

    hm.assert_that(yndx_passport.passport_roles, hm.contains_inanyorder(*required_passport_roles))
    assert (yndx_passport.master == domain_passport) is w_role

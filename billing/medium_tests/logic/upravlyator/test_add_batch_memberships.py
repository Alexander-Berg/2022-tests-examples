# -*- coding: utf-8 -*-
import datetime
import pytest
import hamcrest as hm

from balance import mapper
from tests.tutils import has_exact_entries, mock_transactions

from tests.medium_tests.logic.upravlyator.conftest import (
    EXTERNAL_CLIENT_ROLE,
    create_passport,
    assert_passport_roles,
)


GROUP_ID_1 = 111
GROUP_ID_2 = 666
GROUP_ID_3 = 333


def test_add_batch_to_domain_login(session, upravlyator, domain_passport):
    res = upravlyator.add_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': '',
    }])
    hm.assert_that(res, has_exact_entries({'code': 0}))
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_1).count() == 1


def test_add_same_batch_to_domain_login(session, upravlyator, domain_passport):
    session.add(mapper.PassportGroup(passport=domain_passport, group_id=GROUP_ID_1))
    session.flush()

    res = upravlyator.add_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': '',
    }])
    hm.assert_that(res, has_exact_entries({'code': 0}))
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_1).count() == 1


@pytest.mark.parametrize(
    'linked', [True, False],
)
def test_add_to_passport_login(session, upravlyator, domain_passport, yndx_passport, linked):
    """Если yndx паспорт еще не привязан к доменному паспорту, то надо привязать"""
    if linked:
        yndx_passport.master = domain_passport
        session.flush()

    res = upravlyator.add_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': yndx_passport.login,
    }])
    hm.assert_that(res, has_exact_entries({'code': 0}))
    assert yndx_passport.master == domain_passport
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_1).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport, group_id=GROUP_ID_1).count() == 1


def test_add_same_passport_login(session, upravlyator, domain_passport, yndx_passport):
    yndx_passport.master = domain_passport
    session.add(mapper.PassportGroup(passport=yndx_passport, group_id=GROUP_ID_1))
    session.flush()

    res = upravlyator.add_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': yndx_passport.login,
    }])
    hm.assert_that(res, has_exact_entries({'code': 0}))
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_1).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport, group_id=GROUP_ID_1).count() == 1


@pytest.mark.parametrize(
    'linked_new', [True, False],
)
def test_change_passport_login(session, upravlyator, domain_passport, yndx_passports, linked_new):
    """
    1. Если yndx паспорт еще не привязан к доменному паспорту, то надо привязать.
    2. Если у старого yndx паспорта не осталось больше связей с ролями,
    то надо удалить связь между ним и доменным паспортом.
    """
    yndx_passport_1, yndx_passport_2 = yndx_passports
    yndx_passport_1.master = domain_passport
    session.add(mapper.PassportGroup(passport=yndx_passport_1, group_id=GROUP_ID_1))
    if linked_new:
        yndx_passport_2.master = domain_passport
    session.flush()

    res = upravlyator.add_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': yndx_passport_2.login,
    }])
    hm.assert_that(res, has_exact_entries({'code': 0}))
    assert yndx_passport_1.master is domain_passport
    assert yndx_passport_2.master is domain_passport
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport, group_id=GROUP_ID_1).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport_1, group_id=GROUP_ID_1).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport_2, group_id=GROUP_ID_1).count() == 1


@pytest.mark.parametrize(
    'is_domain',
    [True, False],
)
def test_too_many_links(session, upravlyator, domain_passport, yndx_passport, is_domain):
    """Если доменный паспорт цже связан с группой более 1го раза через внешние паспорта,
    то мы не знаем у какой именно записи нам следует заменить паспорт
    => вызываем исключение
    """
    yndx_passport.master = domain_passport
    for passport in [domain_passport, yndx_passport]:
        session.add(mapper.PassportGroup(
            passport=passport,
            group_id=GROUP_ID_1,
        ))
    session.flush()

    res = upravlyator.add_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': u'' if is_domain else yndx_passport.login,
    }])
    msg = u'Паспорт %s связан с группой %s несколько раз' % (domain_passport.passport_id, GROUP_ID_1)
    error_dict = {
        u'login': domain_passport.login,
        u'group': unicode(GROUP_ID_1),
        u'passport_login': u'' if is_domain else yndx_passport.login,
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


@pytest.mark.parametrize(
    'is_domain',
    [True, False],
)
def test_several_links_with_different_groups(session, upravlyator, yndx_passports, is_domain):
    """А с разными группами можно связывать"""
    domain_passports = [
        create_passport(session, login='login_1', is_internal=True),
        create_passport(session, login='login_2', is_internal=True),
    ]
    for domain_passport, yndx_passport in zip(domain_passports, yndx_passports):
        yndx_passport.master = domain_passport

    passports = domain_passports if is_domain else yndx_passports
    for passport, group_id in zip(passports, [GROUP_ID_1, GROUP_ID_2]):
        session.add(mapper.PassportGroup(passport=passport, group_id=group_id))
    session.flush()

    res = upravlyator.add_batch_memberships(data=[
        {
            'login': domain_passport.login,
            'group': str(GROUP_ID_3),
            'passport_login': yndx_passport.login if not is_domain else u'',
        }
        for domain_passport, yndx_passport in zip(domain_passports, yndx_passports)
    ])
    hm.assert_that(res, has_exact_entries({'code': 0}))

    for passport, group_id in zip(passports, [GROUP_ID_1, GROUP_ID_2]):
        assert session.query(mapper.PassportGroup).filter_by(passport=passport,  group_id=group_id).count() == 1
    for passport in passports:
        assert session.query(mapper.PassportGroup).filter_by(passport=passport, group_id=GROUP_ID_3).count() == 1


def test_wrong_linked_passport(session, upravlyator):
    domain_passport_1 = create_passport(session, login='login_1', is_internal=True)
    domain_passport_2 = create_passport(session, login='login_2', is_internal=True)

    yndx_passport = create_passport(session, 'yndx-%s' % domain_passport_1.login)
    yndx_passport.master = domain_passport_1
    session.flush()

    with mock_transactions():
        res = upravlyator.add_batch_memberships(data=[{
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
    assert yndx_passport.master is domain_passport_1
    assert session.query(mapper.PassportGroup).filter_by(group_id=GROUP_ID_1).count() == 0


@pytest.mark.parametrize(
    'data',
    ['[{}]', [u'aaa'], {u'a': 1}],
)
def test_error_batch(upravlyator, data):
    res = upravlyator.add_batch_memberships(data=data)
    hm.assert_that(
        res,
        hm.has_entries({
            'code': 1,
            'error': 'Upravlyator.BadJsonFormat(%s)' % data,
        }),
    )


def test_full(session, upravlyator):
    domain_passport_1 = create_passport(session, login='login_1', is_internal=True)
    domain_passport_2 = create_passport(session, login='login_2', is_internal=True)
    yndx_passport = create_passport(session, 'yndx-%s' % domain_passport_1.login)
    session.flush()

    with mock_transactions():
        res = upravlyator.add_batch_memberships(data=[
            {
                'login': domain_passport_1.login,
                'group': str(GROUP_ID_1),
                'passport_login': '',
            },
            {
                'login': domain_passport_2.login,
                'group': str(GROUP_ID_2),
                'passport_login': '',
            },
            {
                'login': domain_passport_1.login,
                'group': str(GROUP_ID_2),
                'passport_login': yndx_passport.login,
            },
            {
                'login': domain_passport_2.login,
                'group': str(GROUP_ID_1),
                'passport_login': yndx_passport.login,  # ошибка, т.к. логин уже привязан к другому домену
            },
            {
                'login': domain_passport_1.login,
                'group': str(GROUP_ID_3),
                'passport_login': yndx_passport.login,
            },
        ])

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

    assert yndx_passport.master not in (domain_passport_1, domain_passport_2)
    assert session.query(mapper.PassportGroup).filter_by(passport=yndx_passport).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport_1).count() == 0
    assert session.query(mapper.PassportGroup).filter_by(passport=domain_passport_2).count() == 0


@pytest.mark.parametrize(
    'batch_size',
    [1, 4],
)
def test_many_users_to_group(session, upravlyator, role, batch_size):
    domain_passport_1 = create_passport(session, login='login_1', is_internal=True)
    for i in range(2):
        yndx_passport = create_passport(session, 'yndx-%s-%s' % (domain_passport_1.login, i))
        yndx_passport.master = domain_passport_1

    domain_passport_2 = create_passport(session, login='login_2', is_internal=True)
    for i in range(2):
        yndx_passport = create_passport(session, 'yndx-%s-%s' % (domain_passport_2.login, i))
        yndx_passport.master = domain_passport_2

    domain_passport_3 = create_passport(session, login='login_3', is_internal=True)
    for i in range(2):
        yndx_passport = create_passport(session, 'yndx-%s-%s' % (domain_passport_3.login, i))
        yndx_passport.master = domain_passport_3
    yndx_passport_3 = create_passport(session, 'yndx-%s' % domain_passport_3.login)
    yndx_passport_3.master = domain_passport_3

    domain_passport_4 = create_passport(session, login='login_4', is_internal=True)
    for i in range(2):
        yndx_passport = create_passport(session, 'yndx-%s-%s' % (domain_passport_4.login, i))
        yndx_passport.master = domain_passport_4
    yndx_passport_4 = create_passport(session, 'yndx-%s' % domain_passport_4.login)
    yndx_passport_4.master = domain_passport_4

    session.add(mapper.RoleGroup(session, group_id=GROUP_ID_1, role=role))
    session.flush()

    data = [
        {
            'login': domain_passport_1.login,
            'group': str(GROUP_ID_1),
            'passport_login': '',
        },
        {
            'login': domain_passport_2.login,
            'group': str(GROUP_ID_1),
            'passport_login': '',
        },
        {
            'login': domain_passport_3.login,
            'group': str(GROUP_ID_1),
            'passport_login': yndx_passport_3.login,
        },
        {
            'login': domain_passport_4.login,
            'group': str(GROUP_ID_1),
            'passport_login': yndx_passport_4.login,
        },
    ]

    for step in range(0, len(data), batch_size):
        res = upravlyator.add_batch_memberships(data=data[step:step + batch_size])
        hm.assert_that(res, has_exact_entries({'code': 0}))

    assert len(domain_passport_1.externals) == 2
    assert len(domain_passport_2.externals) == 2
    assert len(domain_passport_3.externals) == 3
    assert len(domain_passport_4.externals) == 3

    assert_passport_roles(domain_passport_1, EXTERNAL_CLIENT_ROLE, (role.id, None, GROUP_ID_1))
    assert_passport_roles(domain_passport_2, EXTERNAL_CLIENT_ROLE, (role.id, None, GROUP_ID_1))
    assert_passport_roles(domain_passport_3, EXTERNAL_CLIENT_ROLE)
    assert_passport_roles(domain_passport_4, EXTERNAL_CLIENT_ROLE)

    for domain_passport in [domain_passport_1, domain_passport_2, domain_passport_3, domain_passport_4]:
        for yndx_passport in set(domain_passport.externals) - {yndx_passport_3, yndx_passport_4}:
            assert_passport_roles(yndx_passport, EXTERNAL_CLIENT_ROLE)

    assert_passport_roles(yndx_passport_3, EXTERNAL_CLIENT_ROLE, (role.id, None, GROUP_ID_1))
    assert_passport_roles(yndx_passport_4, EXTERNAL_CLIENT_ROLE, (role.id, None, GROUP_ID_1))


def test_add_to_existing_group(session, upravlyator, role, domain_passport, yndx_passport):
    role_group = mapper.RoleGroup(
        role_id=role.id,
        group_id=GROUP_ID_1,
        create_dt=session.now() - datetime.timedelta(seconds=1),
        update_dt=session.now() - datetime.timedelta(seconds=1),
    )
    session.add(role_group)
    yndx_passport.master = domain_passport
    session.flush()

    assert session.query(mapper.RolePassport).filter_by(group_id=GROUP_ID_1).count() == 0

    res = upravlyator.add_batch_memberships(data=[{
        'login': domain_passport.login,
        'group': str(GROUP_ID_1),
        'passport_login': yndx_passport.login,
    }])
    hm.assert_that(res, has_exact_entries({'code': 0}))

    assert yndx_passport.master == domain_passport
    session.refresh(role_group)
    hm.assert_that(
        yndx_passport.passport_roles,
        hm.has_item(
            hm.has_properties(
                role_id=role.id,
                firm_id=None,
                template_group_id=None,
                group_id=GROUP_ID_1,
                create_dt=hm.greater_than_or_equal_to(role_group.create_dt),
                update_dt=hm.greater_than_or_equal_to(role_group.update_dt),
            )
        ),
    )

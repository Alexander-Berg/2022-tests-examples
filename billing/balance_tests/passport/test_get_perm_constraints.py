# -*- coding: utf-8 -*-
import pytest
import hamcrest as hm

from balance.constants import (
    FirmId,
    ConstraintTypes,
)
from tests import object_builder as ob
from tests.tutils import has_exact_entries

from tests.balance_tests.passport.passport_common import (
    create_role, create_passport, create_client_batch_id)

pytestmark = [
    pytest.mark.permissions,
]


def test_wo_constraint(session):
    """право не ограничено по фирме, в роли нет ограничения -> пользователь не ограничен в праве по фирме"""
    role = create_role(
        session,
        'Perm1',
        ('Perm2', {ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(session, role)

    assert passport.get_perm_constraints('Perm1') == [{}]


def test_wo_perm_constraint(session):
    """право не ограничено по фирме, в роли есть ограничение -> пользователь не ограничен в праве по фирме"""
    role = create_role(
        session,
        'Perm1',
        ('Perm2', {ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        (role, FirmId.YANDEX_OOO),
    )

    assert passport.get_perm_constraints('Perm1') == [{}]


def test_wo_role_constraint_values(session):
    """право ограничено по фирме, но фирма не указана, в роли нет ограничения -> пользователь не ограничен
    в праве по фирме"""
    role = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None}))
    passport = create_passport(session, role)

    assert passport.get_perm_constraints('Perm1') == [{}]


def test_wo_perm(session):
    """у права не из роли, ограничений нет"""
    role = create_role(
        session,
        'Perm1',
        ('Perm2', {ConstraintTypes.firm_id: (1, 666)}),
    )
    passport = create_passport(session, role)

    assert passport.get_perm_constraints('Perm3') == []


def test_w_constraint(session):
    """право ограничено по фирме, но фирма не указана, в роли есть ограничение -> пользователь ограничен
    в праве по фирме из роли"""
    role = create_role(
        session,
        'Perm1',
        ('Perm2', {ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        (role, FirmId.YANDEX_OOO),
    )

    assert passport.get_perm_constraints('Perm2') == [{ConstraintTypes.firm_id: (FirmId.YANDEX_OOO,)}]


def test_w_role_perm_constraint(session):
    """право ограничено по фирме, фирма указана, в роли нет ограничения -> право ограничено по фирме из права"""
    role = create_role(session, ('Perm1', {'firm_id': [666, 10]}))
    passport = create_passport(session, role)

    assert passport._get_perm_constraints_all('Perm1') == [{'firm_id': (666, 10)}]


def test_multiple_roles(session):
    """право ограничено по фирме в нескольких ролях, но фирма не указана, у пользователя несколько разных ролей с правом
     ограничены по фирме -> пользователь ограничен в праве по фирмам из ролей"""
    role1 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None}))
    role2 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role1, FirmId.YANDEX_OOO),
        (role2, FirmId.YANDEX_EU_AG),
    )

    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(
            {ConstraintTypes.firm_id: (FirmId.YANDEX_OOO,)},
            {ConstraintTypes.firm_id: (FirmId.YANDEX_EU_AG,)},
        )
    )


def test_w_constraint_perm_w_firm_w_role_perm(session):
    """право ограничено по фирме, фирма указана, в роли есть ограничение -> право ограничено по фирме из роли"""
    role = create_role(session, ('Perm1', {'firm_id': [666]}))
    passport = create_passport(session, (role, FirmId.YANDEX_OOO))

    assert passport._get_perm_constraints_all('Perm1') == [{'firm_id': (1,)}]


def test_multiple_role_constraints(session):
    """право ограничено по фирме, но фирма не указана, у пользователя одна и та же роль добавлена несколько раз с
    ограничением по нескольким фирмам -> пользователь ограничен в праве по фирмам из ролей"""
    role = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role, FirmId.YANDEX_OOO),
        (role, FirmId.YANDEX_EU_AG),
    )

    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(
            {ConstraintTypes.firm_id: (FirmId.YANDEX_OOO,)},
            {ConstraintTypes.firm_id: (FirmId.YANDEX_EU_AG,)},
        )
    )


def test_nested_constraints(session):
    """право в одной роли ограничено по фирме, но фирма не указана, в другой роли не ограничено по фирме,
    у пользователя есть роль без ограничения по фирме и ограниченная по фирмам -> пользователь не ограничен
    в праве по фирме"""
    role1 = create_role(session, 'Perm1')
    role2 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        role1,
        (role2, FirmId.YANDEX_OOO),
        (role2, FirmId.YANDEX_EU_AG),
    )

    assert passport.get_perm_constraints('Perm1') == [{}]


@pytest.mark.parametrize(
    'role_perm_firm_id, role_user_firm_id, ans',
    [
        (None, [1, 2], [{u'firm_id': (1,)}, {u'firm_id': (2,)}]),
        ([1, 2], None, [{u'firm_id': (1, 2)}]),
        (None, None, [{}]),
        ([1, 2], [2, 3], [{u'firm_id': (2,)}, {u'firm_id': (3,)}]),
    ],
)
def test_get_perm_constraints(session, role_perm_firm_id, role_user_firm_id, ans):
    """1. право ограничено по фирме, но фирма не указана, у пользователя одна и та же роль добавлена несколько раз с
    ограничением по нескольким фирмам -> пользователь ограничен в праве по фирмам из ролей

    2. право ограничено по нескольким фирмам, у пользователя одна и та же роль добавлена без ограничения по фирме
     -> пользователь ограничен в праве по фирмам из права

    3. право не ограничено по фирме, в роли нет ограничения -> право не ограничено по фирме

    4. право ограничено по нескольким фирмам, у пользователя одна и та же роль добавлена несколько раз с
    ограничением по нескольким фирмам -> право ограничено по фирмам из роли
    """
    role = create_role(session, ('Perm1', {ConstraintTypes.firm_id: role_perm_firm_id}))
    roles_args = [(role, firm_id) for firm_id in role_user_firm_id or []] or [role]
    passport = create_passport(
        session,
        *roles_args
    )
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(*ans),
    )


def test_get_perm_constraints_w_different_roles(session):
    """Проверяем ограничения для несколько ролей связаны с одним правом"""
    role1 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: [1, 2]}))
    role2 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: [2, 3]}))
    role3 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: [4, 5]}))
    passport = create_passport(
        session,
        (role1, 2),
        (role1, 3),
        (role2, 3),
        (role2, 4),
        role3,
    )
    ans = [{u'firm_id': (2,)}, {u'firm_id': (3,)},
           {u'firm_id': (4,)}, {u'firm_id': (4, 5)}]
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(*ans),
    )


def test_w_manager_wo_firm_perm_w_firm_role(session):
    """право с ограничением по фирме, но фирма не указана, и ограничение по менеджеру, роль с ограничением по фирме
    -> пользователь ограничен в праве и по фирме, и как менеджер"""
    role = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None, ConstraintTypes.manager: 1}))
    passport = create_passport(session, (role, 1))
    ans = [{u'manager': 1, u'firm_id': (1,)}]
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(*ans),
    )


def test_w_constraint_perm_w_firm_w_role_perm_w_manager(session):
    """право ограничено по фирме, фирма указана, в роли есть ограничение -> право ограничено по фирме из роли"""
    role = create_role(session, ('Perm1', {'firm_id': [666], 'manager': 1}))
    passport = create_passport(session, (role, FirmId.YANDEX_OOO))

    assert passport._get_perm_constraints_all('Perm1') == [{'firm_id': (1,), 'manager': 1}]


def test_w_constraint_perm_wo_firm_wo_role_perm(session):
    """право ограничено по фирме, фирма не указана, в роли нет ограничения -> право не ограничено по фирме"""
    role = create_role(session, ('Perm1', {'firm_id': None}))
    passport = create_passport(session, role)

    assert passport._get_perm_constraints_all('Perm1') == [{}]


def test_w_constraint_perm_wo_firm_w_role_perm(session):
    """право ограничено по фирме, фирма не указана, в роли есть ограничение -> право ограничено по фирме из роли"""
    role = create_role(session, ('Perm1', {'firm_id': None}))
    passport = create_passport(session, (role, FirmId.YANDEX_OOO))

    assert passport._get_perm_constraints_all('Perm1') == [{'firm_id': (1,)}]


def test_w_manager_w_firm_perm_w_firm_role(session):
    """право с ограничением по одной и той же фирме указано в двух ролях, одна из которых ограничена по менеджеру,
    у пользователя две роли без ограничения по фирмам -> пользователь ограничен в праве по фирме,
    но не ограничен как менеджер"""
    role1 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: [2], ConstraintTypes.manager: 1}))
    role2 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: [2]}))
    passport = create_passport(session, role1, role2)
    ans = [{u'firm_id': (2,)}]
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(*ans),
    )


def test_w_manager_perm_w_firm_perm_wo_firm_role(session):
    """право в одной роли ограничено по фирме, в другой - по менеджеру, у пользователя обе роли -> пользователь
    ограничен в праве по фирме из права  или должен быть менеджером"""
    role1 = create_role(session, ('Perm1', {ConstraintTypes.manager: 1}))
    role2 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: [2]}))
    passport = create_passport(session, role1, role2)
    ans = [{u'manager': 1}, {u'firm_id': (2,)}]
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(*ans),
    )


def test_w_manager_perm_w_firm_perm_w_firm_perm_wo_firm_role(session):
    role1 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None, ConstraintTypes.manager: 1}))
    role2 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role1, 2),
        (role1, 3),
        (role2, 3),
    )
    ans = [{u'firm_id': (3,)}, {u'manager': 1, u'firm_id': (2,)}]
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(*ans),
    )


def test_firm_undefined(session):
    role = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None}))
    passport = create_passport(session)
    ob.set_roles(session, passport, [(role, {ConstraintTypes.firm_id: FirmId.UNDEFINED})])
    assert passport.get_perm_constraints('Perm1') == [{}]
    assert len(passport.passport_roles) == 1
    passport_role = passport.passport_roles[0]
    assert passport_role.firm_id == FirmId.UNDEFINED
    assert passport_role.constraint_values == {}


def test_w_clients_in_role(session):
    """У роли указаны клиенты 'по умолчанию'"""
    role = create_role(session, ('Perm1', {ConstraintTypes.client_batch_id: [123, 666]}))
    passport = create_passport(session, role)
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(
            has_exact_entries({ConstraintTypes.client_batch_id: hm.contains(123, 666)}),
        ),
    )


def test_w_clients_wo_checks(session, client_batch_id):
    """У пользователя указаны клиенты, но в роли не предусмотрена их проверка"""
    role = create_role(session, 'Perm1')
    passport = create_passport(session, (role, None, client_batch_id))

    assert passport.get_perm_constraints('Perm1') == [{}]


def test_w_clients_in_role_user(session, client_batch_id):
    """Роль проверяется на clients без указания дефолтных, к паспорту роль привязана с указанием клиента"""
    role = create_role(session, ('Perm1', {ConstraintTypes.client_batch_id: None}))
    passport = create_passport(session, (role, None, client_batch_id))
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(
            has_exact_entries({ConstraintTypes.client_batch_id: hm.contains(client_batch_id)}),
        ),
    )


def test_w_clients_w_check_w_role_user(session):
    """У роли есть дефолтные проверки на clients, к паспорту тоже привязаны клиенты
    => берем клиетов из паспорта.
    """
    common_client_batch_id = create_client_batch_id(session)
    role_client_batch_id = create_client_batch_id(session)
    user_client_batch_id = create_client_batch_id(session)

    role = create_role(session, ('Perm1', {ConstraintTypes.client_batch_id: [common_client_batch_id, role_client_batch_id]}))
    passport = create_passport(
        session,
        (role, None, common_client_batch_id),
        (role, None, user_client_batch_id),
    )

    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(
            has_exact_entries({ConstraintTypes.client_batch_id: hm.contains(common_client_batch_id)}),
            has_exact_entries({ConstraintTypes.client_batch_id: hm.contains(user_client_batch_id)}),
        ),
    )


def test_several_roles_w_clients(session):
    """Несколько ролей с одним правом, клиенты привязаны к пользователям"""
    client_batch_id_1 = create_client_batch_id(session)
    client_batch_id_2 = create_client_batch_id(session)

    role1 = create_role(session, ('Perm1', {ConstraintTypes.client_batch_id: None}))
    role2 = create_role(session, ('Perm1', {ConstraintTypes.client_batch_id: None}))

    passport = create_passport(
        session,
        (role1, None, client_batch_id_1),
        (role2, None, client_batch_id_2),
    )

    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(
            has_exact_entries({ConstraintTypes.client_batch_id: hm.contains(client_batch_id_1)}),
            has_exact_entries({ConstraintTypes.client_batch_id: hm.contains(client_batch_id_2)}),
        ),
    )


def test_several_role_users_w_clients(session):
    """Одна роль с несколькими правами, которая привязаны к пользователю с набором клиентов"""
    client_batch_ids = [create_client_batch_id(session) for _i in range(2)]
    role = create_role(
        session,
        ('Perm1', {ConstraintTypes.client_batch_id: None}),
        ('Perm2', {ConstraintTypes.client_batch_id: None}),
    )
    passport = create_passport(
        session,
        (role, None, client_batch_ids[0]),
        (role, None, client_batch_ids[1]),
    )

    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(*[
            has_exact_entries({ConstraintTypes.client_batch_id: hm.contains(client_batch_id)})
            for client_batch_id in client_batch_ids
        ]),
    )
    hm.assert_that(
        passport.get_perm_constraints('Perm2'),
        hm.contains_inanyorder(*[
            has_exact_entries({ConstraintTypes.client_batch_id: hm.contains(client_batch_id)})
            for client_batch_id in client_batch_ids
        ]),
    )


def test_w_role_clients_w_role_firms(session):
    """Ограничения прописаны в роли"""
    role = create_role(
        session,
        ('Perm1', {ConstraintTypes.client_batch_id: [1, 2], ConstraintTypes.firm_id: [333, 666]}),
    )
    passport = create_passport(session, role)
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(
            has_exact_entries({
                ConstraintTypes.client_batch_id: hm.contains(1, 2),
                ConstraintTypes.firm_id: hm.contains(333, 666),
            }),
        ),
    )


def test_w_user_clients_w_user_firms_in_same_user_role(session):
    """Фирма и клиент в одной role_user"""
    role = create_role(
        session,
        ('Perm1', {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        (role, 1, 2),
        (role, 3, 4),
    )
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(
            has_exact_entries({
                ConstraintTypes.client_batch_id: hm.contains(2),
                ConstraintTypes.firm_id: hm.contains(1),
            }),
            has_exact_entries({
                ConstraintTypes.client_batch_id: hm.contains(4),
                ConstraintTypes.firm_id: hm.contains(3),
            }),
        ),
    )


def test_w_user_clients_w_user_firms(session):
    """Фирма и клиент в разных role_user"""
    role = create_role(
        session,
        ('Perm1', {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        (role, 111, 222),
        (role, 666, 333),  # это более узкое ограничение не попадет в выборку
        (role, None, 333),
        (role, 666, None),
    )
    hm.assert_that(
        passport.get_perm_constraints('Perm1'),
        hm.contains_inanyorder(
            has_exact_entries({
                ConstraintTypes.client_batch_id: hm.contains(222),
                ConstraintTypes.firm_id: hm.contains(111),
            }),
            has_exact_entries({
                ConstraintTypes.client_batch_id: hm.contains(333),
            }),
            has_exact_entries({
                ConstraintTypes.firm_id: hm.contains(666),
            }),
        ),
    )


def test_w_empty_role(session):
    """Есть роль без ограничений, которая перебивает все роли с ограничениями"""
    role = create_role(
        session,
        ('Perm1', {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        role,
        (role, 666, 333),
    )
    assert passport.get_perm_constraints('Perm1') == [{}]

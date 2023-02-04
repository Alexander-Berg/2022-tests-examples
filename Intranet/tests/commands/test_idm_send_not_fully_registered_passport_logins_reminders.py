# coding: utf-8


import pytest
from django.core import mail
from django.core.management import call_command

from waffle.models import Switch

from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import Role
from idm.tests.utils import clear_mailbox, assert_contains, set_workflow, DEFAULT_WORKFLOW, raw_make_role

pytestmark = [
    pytest.mark.django_db,
]


def test_reminders_by_roles(pt1_system, arda_users, robot_gollum):

    Switch.objects.create(name='idm.send_notications_about_not_fully_registered_logins', active=True)

    frodo = arda_users['frodo']
    robot_gollum.add_responsibles([frodo])
    robot_gollum.save()

    frodo.passport_logins.create(login='yndx-frodo-registered', state='created', is_fully_registered=True)
    frodo.passport_logins.create(login='yndx-frodo-not-registered', state='created', is_fully_registered=False)
    frodo.passport_logins.create(login='yndx-frodo-no-active-role', state='created', is_fully_registered=False)
    frodo.passport_logins.create(login='yndx-frodo-no-role', state='created', is_fully_registered=False)
    robot_gollum.passport_logins.create(login='yndx-gollum-not-registered', state='created', is_fully_registered=False)

    set_workflow(pt1_system, code=DEFAULT_WORKFLOW)

    Role.objects.request_role(
        frodo, robot_gollum, pt1_system, '',
        {'project': 'proj1', 'role': 'admin'}, {'passport-login': 'yndx-gollum-not-registered'}
    )
    Role.objects.request_role(
        frodo, frodo, pt1_system, '',
        {'project': 'proj1', 'role': 'admin'}, {'passport-login': 'yndx-frodo-not-registered'}
    )
    Role.objects.request_role(
        frodo, frodo, pt1_system, '',
        {'project': 'proj1', 'role': 'manager'}, {'passport-login': 'yndx-frodo-not-registered'}
    )
    Role.objects.request_role(
        frodo, frodo, pt1_system, '',
        {'project': 'proj1', 'role': 'admin'}, {'passport-login': 'yndx-frodo-registered'}
    )

    raw_make_role(
        frodo,
        pt1_system,
        {'project': 'proj1', 'role': 'admin'},
        {'passport-login': 'yndx-frodo-no-active-role'},
        state='deprived',
    )

    clear_mailbox()
    call_command('idm_send_not_fully_registered_passport_logins_reminders')

    assert len(mail.outbox) == 2
    mail1, mail2 = sorted(mail.outbox, key=lambda x: x.subject)

    # о логинах робота письмо получат ответственные за него
    assert mail1.to == ['frodo@example.yandex.ru']
    assert mail1.subject == 'Некоторые паспортные логины gollum необходимо дорегистрировать'
    assert_contains(
        ['Пожалуйста, завершите регистрацию логинов gollum, перечисленных ниже:', 'yndx-gollum-not-registered'],
        mail1.body,
    )

    assert mail2.to == ['frodo@example.yandex.ru']
    assert mail2.subject == 'Некоторые паспортные логины необходимо дорегистрировать'
    assert_contains(
        ['Пожалуйста, завершите регистрацию логинов, перечисленных ниже:', 'yndx-frodo-not-registered'],
        mail2.body,
    )
    assert mail2.body.count('yndx-frodo-not-registered') == 1

    assert 'yndx-frodo-registered' not in mail2.body  # о полностью дорегистрированном  логине письмо не шлем
    assert 'yndx-frodo-no-role' not in mail2.body  # о логинах, на которых нет ролей письмо не шлем


def add_passport_login_to_membership(group, user, login):
    membership = group.memberships.get(user=user)
    membership.passport_login = login
    membership.save(update_fields=['passport_login'])


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_reminders_by_memberships(pt1_system, arda_users, robot_gollum, department_structure, group_policy):

    # Подготовка данных для теста
    fellowship = department_structure['fellowship']
    shire = department_structure['shire']
    associations = department_structure['associations']
    valinor = department_structure['valinor']

    # Добавим frodo в ответственные за робота gollum
    frodo = arda_users['frodo']
    robot_gollum.add_responsibles([frodo])
    robot_gollum.save()

    # Уберем лишние членства
    fellowship.memberships.exclude(user=frodo).update(state='inactive')
    fellowship.add_members([robot_gollum])
    shire.memberships.exclude(user=frodo).update(state='inactive')
    associations.memberships.exclude(user=frodo).update(state='inactive')
    valinor.memberships.update(state='inactive')

    # Поменяем групповую политику и групповой workflow у системы
    pt1_system.group_policy = group_policy
    pt1_system.save(update_fields=['group_policy'])
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)

    Switch.objects.create(name='idm.send_notications_about_not_fully_registered_logins', active=True)

    # Создадим паспортные логины для пользователей
    user_registered_login = frodo.passport_logins.create(
        login='yndx-frodo-registered',
        state='created',
        is_fully_registered=True,
    )
    user_not_registered_login = frodo.passport_logins.create(
        login='yndx-frodo-not-registered',
        state='created',
        is_fully_registered=False,
    )
    user_no_active_role_login = frodo.passport_logins.create(
        login='yndx-frodo-no-role',
        state='created',
        is_fully_registered=False,
    )
    rodot_not_registered_login = robot_gollum.passport_logins.create(
        login='yndx-gollum-not-registered',
        state='created',
        is_fully_registered=False,
    )
    user_not_registered_login_inactive_membership = frodo.passport_logins.create(
        login='yndx-frodo-not-registered-inactive-membership',
        state='created',
        is_fully_registered=False,
    )

    # Привяжем паспортные логины к соответствующим членствам
    # fellowship-frodo - паспортный логин дорегистрирован, письмо не должно прийти
    add_passport_login_to_membership(fellowship, frodo, user_registered_login)
    # fellowship-gollum - паспортный логин не дорегистрирован, письмо должно прийти ответственному за робота
    add_passport_login_to_membership(fellowship, robot_gollum, rodot_not_registered_login)
    # shire-frodo - паспортный логин не дорегистрирован, письмо должно прийти пользователю
    add_passport_login_to_membership(fellowship, frodo, user_not_registered_login)
    # associations-frodo - паспортный логин не дорегистрирован, но групповая роль неактивна, письмо не должно прийти
    add_passport_login_to_membership(associations, frodo, user_no_active_role_login)
    # valinor-frodo - паспортный логин не дорегистрирован, групповая роль активна, но членство неактивно, письмо не должно прийти
    add_passport_login_to_membership(valinor, frodo, user_not_registered_login_inactive_membership)

    Role.objects.request_role(
        frodo, fellowship, pt1_system, '',
        {'project': 'proj1', 'role': 'admin'},
    )
    Role.objects.request_role(
        frodo, shire, pt1_system, '',
        {'project': 'proj1', 'role': 'manager'},
    )
    role = Role.objects.request_role(
        arda_users['varda'], valinor, pt1_system, '',
        {'project': 'proj1', 'role': 'manager'},
    )

    raw_make_role(
        associations,
        pt1_system,
        {'project': 'proj1', 'role': 'admin'},
        state='deprived',
    )

    clear_mailbox()

    # Запустим команду и проверим, что письма отправятся корректно
    call_command('idm_send_not_fully_registered_passport_logins_reminders')

    expected_count = (
        2 # одно письмо про логин frodo, другое про логин gollum
        if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
        else 0 # писем нет
    )

    assert len(mail.outbox) == expected_count

    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS:
        mail1, mail2 = sorted(mail.outbox, key=lambda x: x.subject)

        # о логинах робота письмо получат ответственные за него
        assert mail1.to == ['frodo@example.yandex.ru']
        assert mail1.subject == 'Некоторые паспортные логины gollum необходимо дорегистрировать'
        assert_contains(
            ['Пожалуйста, завершите регистрацию логинов gollum, перечисленных ниже:', 'yndx-gollum-not-registered'],
            mail1.body,
        )

        assert mail2.to == ['frodo@example.yandex.ru']
        assert mail2.subject == 'Некоторые паспортные логины необходимо дорегистрировать'
        assert_contains(
            ['Пожалуйста, завершите регистрацию логинов, перечисленных ниже:', 'yndx-frodo-not-registered'],
            mail2.body,
        )

        assert 'yndx-frodo-registered' not in mail2.body  # о полностью дорегистрированном  логине письмо не шлем
        assert 'yndx-frodo-no-role' not in mail2.body  # о логинах, на которых нет ролей письмо не шлем
        assert 'yndx-frodo-not-registered-inactive-membership' not in mail2.body  # о логинах, без активных членств письмо не шлем

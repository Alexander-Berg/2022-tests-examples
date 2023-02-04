# -*- coding: utf-8 -*-
from typing import Dict

import pytest
from django.utils import translation

from idm.core.models import Role, UserPassportLogin
from idm.tests.templates.emails.test_include import approver
from idm.tests.templates.utils import (LANG_CODES, generate_named_user,
                                       render_template, user_mention)
from idm.tests.utils import (create_group, create_system, create_user,
                             random_slug)
from idm.users.models import GroupResponsibility, User

pytestmark = pytest.mark.parametrize('lang', LANG_CODES)

PARENT_ROLE_SLUG = random_slug()
ROLE_SLUG = random_slug()
CHILD_ROLE_SLUG = random_slug()
ROLE_TREE = {
    'code': 0,
    'roles': {
        'slug': 'role',
        'name': {'ru': 'Роль', 'en': 'Role'},
        'values': {
            ROLE_SLUG: {'name': {'ru': 'Ассессор', 'en': 'Accessor'}},
            PARENT_ROLE_SLUG: {'name': {'ru': 'Родитель', 'en': 'Parent'}},
            CHILD_ROLE_SLUG: {'name': {'ru': 'Потомок', 'en': 'Child'}},
        },
    },
}


def generate_passport_login(user: User, role: Role) -> UserPassportLogin:
    login: UserPassportLogin = UserPassportLogin.objects.create(user=user, login=random_slug())
    login.roles.add(role)
    return login


@pytest.mark.parametrize(('add_passport_login', 'passport_login_created'), [
    (False, False),
    (True, False),
    (True, True),
])
def test_role_granted__user_role(lang: str, add_passport_login: bool, passport_login_created: bool):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = generate_named_user()
    user = create_user()
    role = Role.objects.request_role(
        requester=requester,
        subject=user,
        system=system,
        data={'role': ROLE_SLUG},
        comment='',
    )
    passport_login = None
    if add_passport_login:
        passport_login = generate_passport_login(user, role)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Вы получили новую роль в системе "%s":',
                'en': 'You\'ve got a new role in the system "%s":'
            }[lang] % system.get_name(lang),
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            {
                'ru': 'Роль была запрошена пользователем %s.',
                'en': 'Role was requested by user %s.'
            }[lang] % user_mention(requester),
        ]
        if add_passport_login and passport_login_created:
            expected_lines.append(
                {
                    'ru': 'В Паспорте был заведен новый логин: %s. Пароль был отправлен отдельным письмом.',
                    'en': 'New Passport Account was created: %s. The password was sent to you in a separate e-mail.'
                }[lang] % passport_login.login,
            )
            expected_lines.append(
                {
                    'ru': 'Вы можете восстановить пароль при утере по адресу '
                          'https://passport.yandex.ru/passport?mode=restore, используя свой рабочий email.',
                    'en': 'If you forget your password, you can restore it at '
                          'https://passport.yandex.ru/passport?mode=restore (your @yandex-team.ru email address).',
                }[lang],
            )

        assert render_template(
            'emails/role_granted.txt',
            {
                'role': role,
                'user': user,
                'passport_login': passport_login and passport_login.login or None,
                'passport_login_created': passport_login_created,
            }
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize(('add_passport_login', 'passport_login_created'), [
    (False, False),
    (True, False),
    (True, True),
])
def test_role_granted__robot_role(lang: str, add_passport_login: bool, passport_login_created: bool):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = generate_named_user()
    user = create_user()
    user.is_robot = True
    user.save()
    role = Role.objects.request_role(
        requester=requester,
        subject=user,
        system=system,
        data={'role': ROLE_SLUG},
        comment='',
    )
    passport_login = None
    if add_passport_login:
        passport_login = generate_passport_login(user, role)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Робот %s получил новую роль в системе "%s":',
                'en': 'Robot %s got a new role in the system "%s":'
            }[lang] % (user.username, system.get_name(lang)),
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            {
                'ru': 'Роль была запрошена пользователем %s.',
                'en': 'Role was requested by user %s.'
            }[lang] % user_mention(requester),
        ]
        if add_passport_login and passport_login_created:
            expected_lines.append(
                {
                    'ru': 'В Паспорте был заведен новый логин: %s. Пароль был отправлен отдельным письмом.',
                    'en': 'New Passport Account was created: %s. The password was sent to you in a separate e-mail.'
                }[lang] % passport_login.login,
            )
            expected_lines.append(
                {
                    'ru': 'Вы можете восстановить пароль при утере по адресу '
                          'https://passport.yandex.ru/passport?mode=restore, используя свой рабочий email.',
                    'en': 'If you forget your password, you can restore it at '
                          'https://passport.yandex.ru/passport?mode=restore (your @yandex-team.ru email address).',
                }[lang],
            )

        assert render_template(
            'emails/role_granted.txt',
            {
                'role': role,
                'user': user,
                'passport_login': passport_login and passport_login.login or None,
                'passport_login_created': passport_login_created,
            }
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize(('add_passport_login', 'passport_login_created'), [
    (False, False),
    (True, False),
    (True, True),
])
def test_role_granted__user_role__with_parent(
        lang: str,
        add_passport_login: bool,
        passport_login_created: bool,
):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    user = create_user()
    requester = generate_named_user()
    parent_role = Role.objects.request_role(
        requester=requester,
        subject=user,
        system=system,
        data={'role': PARENT_ROLE_SLUG},
        comment='',
    )
    child_role = Role.objects.request_role(
        requester=None,
        subject=user,
        system=system,
        data={'role': CHILD_ROLE_SLUG},
        comment='',
        parent=parent_role,
    )
    passport_login = None
    if add_passport_login:
        passport_login = generate_passport_login(user, child_role)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Вы получили новую роль в системе "%s":',
                'en': 'You\'ve got a new role in the system "%s":'
            }[lang] % system.get_name(lang),
            render_template('emails/includes/role_and_url.txt', {'role': child_role}),
            {
                'ru': 'поскольку у вас есть роль в системе "%s":',
                'en': 'because you have the role in the system "%s":'
            }[lang] % system.get_name(lang),
            render_template('emails/includes/role_and_url.txt', {'role': parent_role}),
            {
                'ru': 'Роль была запрошена пользователем %s.',
                'en': 'Role was requested by user %s.'
            }[lang] % user_mention(requester),
        ]
        if add_passport_login and passport_login_created:
            expected_lines.append(
                {
                    'ru': 'В Паспорте был заведен новый логин: %s. Пароль был отправлен отдельным письмом.',
                    'en': 'New Passport Account was created: %s. The password was sent to you in a separate e-mail.'
                }[lang] % passport_login.login,
            )
            expected_lines.append(
                {
                    'ru': 'Вы можете восстановить пароль при утере по адресу '
                          'https://passport.yandex.ru/passport?mode=restore, используя свой рабочий email.',
                    'en': 'If you forget your password, you can restore it at '
                          'https://passport.yandex.ru/passport?mode=restore (your @yandex-team.ru email address).',
                }[lang],
            )

        assert render_template(
            'emails/role_granted.txt',
            {
                'role': child_role,
                'user': user,
                'passport_login': passport_login and passport_login.login or None,
                'passport_login_created': passport_login_created,
            }
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize(('responsible_sex', 'responsible_word'), [
    ('M', {'ru': 'ответственным', 'en': 'responsible'}),
    ('F', {'ru': 'ответственной', 'en': 'responsible'}),
])
def test_role_granted__group_role(lang: str, responsible_sex: bool, responsible_word: Dict[str, str]):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        group_workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = generate_named_user()
    group = create_group()
    responsible = create_user(sex=responsible_sex)
    GroupResponsibility.objects.create(group=group, user=responsible)
    role = Role.objects.request_role(
        requester=requester,
        subject=group,
        system=system,
        data={'role': ROLE_SLUG},
        comment='',
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Группа "%s", в которой вы являетесь %s, получила новую роль в системе "%s":',
                'en': 'Group "%s" you are %s in got a new role in the system "%s":'
            }[lang] % (
                group.get_name(lang),
                responsible_word[lang],
                system.get_name(lang)
            ),
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            {
                'ru': 'Роль была запрошена пользователем %s.',
                'en': 'Role was requested by user %s.'
            }[lang] % user_mention(requester),
        ]
        assert render_template(
            'emails/role_granted.txt',
            {
                'role': role,
                'user': responsible,
                'passport_login': None,
                'passport_login_created': False,
            }
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize(('responsible_sex', 'responsible_word'), [
    ('M', {'ru': 'ответственным', 'en': 'responsible'}),
    ('F', {'ru': 'ответственной', 'en': 'responsible'}),
])
def test_role_granted__group_role__with_parent(
        lang: str,
        responsible_sex: str,
        responsible_word: Dict[str, str],
):
    system = create_system(role_tree=ROLE_TREE, public=True)
    requester = generate_named_user()
    group = create_group()
    responsible = create_user(sex=responsible_sex)
    GroupResponsibility.objects.create(group=group, user=responsible)
    parent_role = Role.objects.request_role(
        requester=requester,
        subject=group,
        system=system,
        data={'role': PARENT_ROLE_SLUG},
        comment='',
    )
    child_role = Role.objects.request_role(
        requester=None,
        subject=group,
        system=system,
        data={'role': CHILD_ROLE_SLUG},
        comment='',
        parent=parent_role,
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Группа "%s", в которой вы являетесь %s, получила новую роль в системе "%s":',
                'en': 'Group "%s" you are %s in got a new role in the system "%s":'
            }[lang] % (
                group.get_name(lang),
                responsible_word[lang],
                system.get_name(lang)
            ),
            render_template('emails/includes/role_and_url.txt', {'role': child_role}),
            {
                'ru': 'поскольку у вас есть роль в системе "%s":',
                'en': 'because you have the role in the system "%s":'
            }[lang] % system.get_name(lang),
            render_template('emails/includes/role_and_url.txt', {'role': parent_role}),
            {
                'ru': 'Роль была запрошена пользователем %s.',
                'en': 'Role was requested by user %s.'
            }[lang] % user_mention(requester),
        ]

        assert render_template(
            'emails/role_granted.txt',
            {
                'role': child_role,
                'user': responsible,
            }
        ) == '\n'.join(expected_lines)


def test_role_granted_maillist__user_role(lang: str):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = generate_named_user()
    user = create_user()
    role = Role.objects.request_role(
        requester=requester,
        subject=user,
        system=system,
        data={'role': ROLE_SLUG},
        comment='',
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Вы получили новую роль в системе "%s":',
                'en': 'You\'ve got a new role in the system "%s":'
            }[lang] % system.get_name(lang),
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            {
                'ru': 'Роль была запрошена пользователем %s.',
                'en': 'Role was requested by user %s.'
            }[lang] % user_mention(requester),
        ]

        assert render_template(
            'emails/role_granted.txt',
            {
                'role': role,
                'user': user,
            }
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize(('add_passport_login', 'passport_login_created'), [
    (False, False),
    (True, False),
    (True, True),
])
def test_role_granted_maillist__user_role__with_parent(
        lang: str,
        add_passport_login: bool,
        passport_login_created: bool,
):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    user = create_user()
    requester = generate_named_user()
    parent_role = Role.objects.request_role(
        requester=requester,
        subject=user,
        system=system,
        data={'role': PARENT_ROLE_SLUG},
        comment='',
    )
    child_role = Role.objects.request_role(
        requester=None,
        subject=user,
        system=system,
        data={'role': CHILD_ROLE_SLUG},
        comment='',
        parent=parent_role,
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Вы получили новую роль в системе "%s":',
                'en': 'You\'ve got a new role in the system "%s":'
            }[lang] % system.get_name(lang),
            render_template('emails/includes/role_and_url.txt', {'role': child_role}),
            {
                'ru': 'поскольку у вас есть роль в системе "%s":',
                'en': 'because you have the role in the system "%s":'
            }[lang] % system.get_name(lang),
            render_template('emails/includes/role_and_url.txt', {'role': parent_role}),
            {
                'ru': 'Роль была запрошена пользователем %s.',
                'en': 'Role was requested by user %s.'
            }[lang] % user_mention(requester),
        ]

        assert render_template(
            'emails/role_granted.txt',
            {
                'role': child_role,
                'user': user,
            }
        ) == '\n'.join(expected_lines)


def test_role_granted_maillist__group_role(lang: str):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        group_workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = generate_named_user()
    group = create_group()
    responsible = create_user()
    GroupResponsibility.objects.create(group=group, user=responsible)
    role = Role.objects.request_role(
        requester=requester,
        subject=group,
        system=system,
        data={'role': ROLE_SLUG},
        comment='',
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Группа "%s" получила новую роль в системе "%s":',
                'en': 'Group "%s" got a new role in the system "%s":'
            }[lang] % (
                group.get_name(lang),
                system.get_name(lang)
            ),
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            {
                'ru': 'Роль была запрошена %s.',
                'en': 'Role was requested by %s.'
            }[lang] % user_mention(requester),
        ]
        assert render_template(
            'emails/role_granted_maillist.txt',
            {
                'role': role,
                'user': responsible,
            }
        ) == '\n'.join(expected_lines)


def test_role_granted_maillist__group_role__with_parent(lang: str):
    system = create_system(role_tree=ROLE_TREE, public=True)
    requester = generate_named_user()
    group = create_group()
    responsible = create_user()
    GroupResponsibility.objects.create(group=group, user=responsible)
    parent_role = Role.objects.request_role(
        requester=requester,
        subject=group,
        system=system,
        data={'role': PARENT_ROLE_SLUG},
        comment='',
    )
    child_role = Role.objects.request_role(
        requester=None,
        subject=group,
        system=system,
        data={'role': CHILD_ROLE_SLUG},
        comment='',
        parent=parent_role,
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Группа "%s" получила новую роль в системе "%s":',
                'en': 'Group "%s" got a new role in the system "%s":'
            }[lang] % (
                group.get_name(lang),
                system.get_name(lang),
            ),
            render_template('emails/includes/role_and_url.txt', {'role': child_role}),
            {
                'ru': 'поскольку имеется связанная роль в системе "%s":',
                'en': 'because it refers to role in the system "%s":',
            }[lang] % system.get_name(lang),
            render_template('emails/includes/role_and_url.txt', {'role': parent_role}),
            {
                'ru': 'Роль была запрошена %s.',
                'en': 'Role was requested by %s.'
            }[lang] % user_mention(requester),
        ]

        assert render_template(
            'emails/role_granted_maillist.txt',
            {
                'role': child_role,
                'user': responsible,
            }
        ) == '\n'.join(expected_lines)

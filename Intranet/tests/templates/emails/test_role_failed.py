# -*- coding: utf-8 -*-

import pytest
from django.utils import translation

from idm.core.constants.action import ACTION
from idm.core.models import Role, UserPassportLogin
from idm.tests.templates.utils import (LANG_CODES, generate_named_user,
                                       render_template, user_mention)
from idm.tests.utils import (create_system,
                             create_user, random_slug)
from idm.users.models import User

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
def test_role_failed(lang: str, add_passport_login: bool, passport_login_created: bool):
    system = create_system(
        role_tree=ROLE_TREE,
        public=True,
    )
    user = generate_named_user()
    role = Role.objects.request_role(
        requester=create_user(),
        subject=user,
        system=system,
        data={'role': ROLE_SLUG},
        comment='',
    )
    error_message = random_slug()
    action = role.actions.create(action=ACTION.IDM_ERROR, error=error_message)
    passport_login = None
    if add_passport_login:
        passport_login = generate_passport_login(user, role)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'При добавлении роли для %s в систему "%s" произошла ошибка:',
                'en': 'An error occurred while adding role of %s to the system "%s":'
            }[lang] % (user_mention(user), system.get_name(lang)),
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            {
                'ru': 'Ошибка: %s.',
                'en': 'Error: %s.'
            }[lang] % error_message,
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
            'emails/role_failed.txt',
            {
                'role': role,
                'action': action,
                'passport_login': passport_login and passport_login.login or None,
                'passport_login_created': passport_login_created,
            }
        ) == '\n'.join(expected_lines)

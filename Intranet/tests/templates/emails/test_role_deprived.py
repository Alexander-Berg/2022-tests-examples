# -*- coding: utf-8 -*-
import random
from typing import Dict, Optional

import pytest
from django.conf import settings
from django.utils import translation

from idm.core.models import Role
from idm.tests.templates.utils import (LANG_CODES, generate_named_user,
                                       render_template, user_mention)
from idm.tests.utils import (create_group, create_system, create_user,
                             random_slug)
from idm.users.models import GroupResponsibility

pytestmark = pytest.mark.parametrize('lang', LANG_CODES)

ROLE_SLUG = random_slug()
FIELD_SLUG = 'extra_field'
ROLE_TREE = {
    'code': 0,
    'roles': {
        'slug': 'role',
        'name': {'ru': 'Роль', 'en': 'Role'},
        'values': {ROLE_SLUG: {'name': {'ru': 'Ассессор', 'en': 'Accessor'}}},
    },
    'fields': [
        {
            'slug': FIELD_SLUG,
            'name': {'ru': 'Номер счетчика', 'en': 'Counter ID'},
            'required': True,
            'type': 'integerfield'
        },
    ],
}


@pytest.mark.parametrize('add_deprive_comment', [True, False])
def test_role_deprived__self_role(lang: str, add_deprive_comment: bool):
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
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',

    )
    deprive_comment = add_deprive_comment and random_slug() or ''
    role.deprive_or_decline(user, deprive_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Вы отозвали вашу роль в системе "%s":',
                'en': 'You have revoked a role in the system "%s":'
            }[lang] % system.get_name(lang),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if deprive_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % deprive_comment)
        expected_lines.append(
            {
                'ru': 'Повторно запросить роль можно на странице роли.',
                'en': 'You can re-request role on the role page.'
            }[lang]
        )
        assert render_template('emails/role_deprived.txt', {'role': role, 'user': user}) == '\n'.join(expected_lines)


@pytest.mark.parametrize('add_deprive_comment', [True, False])
def test_role_deprived__self_role_of_robot(lang: str, add_deprive_comment: bool):
    system = create_system(
        role_tree=ROLE_TREE,
        public=True,
    )
    user = generate_named_user(is_robot=True)
    role = Role.objects.request_role(
        requester=create_user(),
        subject=user,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',

    )
    deprive_comment = add_deprive_comment and random_slug() or ''
    role.deprive_or_decline(user, deprive_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Вы отозвали роль робота %s в системе "%s":',
                'en': 'You have revoked a role of robot %s in the system "%s":'
            }[lang] % (user.username, system.get_name(lang)),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if deprive_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % deprive_comment)
        expected_lines.append(
            {
                'ru': 'Повторно запросить роль можно на странице роли.',
                'en': 'You can re-request role on the role page.'
            }[lang]
        )
        assert render_template('emails/role_deprived.txt', {'role': role, 'user': user}) == '\n'.join(expected_lines)


@pytest.mark.parametrize('add_deprive_comment', [True, False])
@pytest.mark.parametrize(('deprive_robot', 'depriver_sex', 'deprive_verb'), [
    (True, None, {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'M', {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'F', {'ru': 'отозвала', 'en': 'revoked'}),
])
def test_role_deprived__user_role(
        lang: str,
        add_deprive_comment: bool,
        deprive_robot: bool,
        depriver_sex: Optional[str],
        deprive_verb: Dict[str, str],
):
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
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',

    )
    deprive_comment = add_deprive_comment and random_slug() or ''
    depriver = None
    if not deprive_robot:
        depriver = generate_named_user(sex=depriver_sex)
    role.deprive_or_decline(depriver, deprive_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s вашу роль в системе "%s":',
                'en': '%s %s your role in the system "%s":'
            }[lang] % (
                depriver and user_mention(depriver) or {'ru': 'Робот', 'en': 'Robot'}[lang],
                deprive_verb[lang],
                system.get_name(lang),
            ),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if deprive_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % deprive_comment)
        expected_lines.append(
            {
                'ru': 'Повторно запросить роль можно на странице роли.',
                'en': 'You can re-request role on the role page.'
            }[lang]
        )
        assert render_template('emails/role_deprived.txt', {'role': role, 'user': user}) == '\n'.join(expected_lines)


@pytest.mark.parametrize('add_deprive_comment', [True, False])
@pytest.mark.parametrize(('deprive_robot', 'depriver_sex', 'deprive_verb'), [
    (True, None, {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'M', {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'F', {'ru': 'отозвала', 'en': 'revoked'}),
])
def test_role_deprived__robot_role(
        lang: str,
        add_deprive_comment: bool,
        deprive_robot: bool,
        depriver_sex: Optional[str],
        deprive_verb: Dict[str, str],
):
    system = create_system(
        role_tree=ROLE_TREE,
        public=True,
    )
    user = generate_named_user(is_robot=True)
    role = Role.objects.request_role(
        requester=create_user(),
        subject=user,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',

    )
    deprive_comment = add_deprive_comment and random_slug() or ''
    depriver = None
    if not deprive_robot:
        depriver = generate_named_user(sex=depriver_sex)
    role.deprive_or_decline(depriver, deprive_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s роль робота %s в системе "%s":',
                'en': '%s %s role of robot %s in system "%s":'
            }[lang] % (
                depriver and user_mention(depriver) or {'ru': 'Робот', 'en': 'Robot'}[lang],
                deprive_verb[lang],
                user.username,
                system.get_name(lang),
            ),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if deprive_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % deprive_comment)
        expected_lines.append(
            {
                'ru': 'Повторно запросить роль можно на странице роли.',
                'en': 'You can re-request role on the role page.'
            }[lang]
        )
        assert render_template('emails/role_deprived.txt', {'role': role, 'user': user}) == '\n'.join(expected_lines)


@pytest.mark.parametrize('add_deprive_comment', [True, False])
@pytest.mark.parametrize(('deprive_robot', 'depriver_sex', 'deprive_verb'), [
    (True, None, {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'M', {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'F', {'ru': 'отозвала', 'en': 'revoked'}),
])
@pytest.mark.parametrize(('responsible_sex', 'responsible_word'), [
    ('M', {'ru': 'ответственным', 'en': 'responsible'}),
    ('F', {'ru': 'ответственной', 'en': 'responsible'}),
])
def test_role_deprived__group_role(
        lang: str,
        add_deprive_comment: bool,
        deprive_robot: bool,
        depriver_sex: Optional[str],
        deprive_verb: Dict[str, str],
        responsible_sex: str,
        responsible_word: Dict[str, str],
):
    system = create_system(role_tree=ROLE_TREE, public=True)
    group = create_group(name=random_slug(), name_en=random_slug())
    responsible = create_user(sex=responsible_sex)
    GroupResponsibility.objects.create(group=group, user=responsible)
    role = Role.objects.request_role(
        requester=create_user(),
        subject=group,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',
    )
    deprive_comment = add_deprive_comment and random_slug() or ''
    depriver = None
    if not deprive_robot:
        depriver = generate_named_user(sex=depriver_sex)
    role.deprive_or_decline(depriver, deprive_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s роль в системе "%s" у группы "%s", в которой вы являетесь %s:',
                'en': '%s %s a role in the system "%s" of the group "%s" you are %s in:'
            }[lang] % (
                depriver and user_mention(depriver) or {'ru': 'Робот', 'en': 'Robot'}[lang],
                deprive_verb[lang],
                system.get_name(lang),
                group.get_name(lang),
                responsible_word[lang],
            ),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if deprive_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % deprive_comment)
        expected_lines.append(
            {
                'ru': 'Повторно запросить роль можно на странице роли.',
                'en': 'You can re-request role on the role page.'
            }[lang]
        )
        assert render_template('emails/role_deprived.txt', {'role': role, 'user': responsible}) == '\n'.join(expected_lines)


@pytest.mark.parametrize('add_deprive_comment', [True, False])
@pytest.mark.parametrize(('deprive_robot', 'depriver_sex', 'deprive_verb'), [
    (True, None, {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'M', {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'F', {'ru': 'отозвала', 'en': 'revoked'}),
])
def test_role_deprived_maillist__user_role(
        lang: str,
        add_deprive_comment: bool,
        deprive_robot: bool,
        depriver_sex: Optional[str],
        deprive_verb: Dict[str, str],
):
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
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',

    )
    deprive_comment = add_deprive_comment and random_slug() or ''
    depriver = None
    if not deprive_robot:
        depriver = generate_named_user(sex=depriver_sex)
    role.deprive_or_decline(depriver, deprive_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s роль сотрудника %s в системе "%s":',
                'en': '%s has %s %s\'s role in the system "%s":'
            }[lang] % (
                depriver and user_mention(depriver) or {'ru': 'Робот', 'en': 'Robot'}[lang],
                deprive_verb[lang],
                user_mention(user),
                system.get_name(lang),
            ),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if deprive_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % deprive_comment)
        expected_lines.append(
            {
                'ru': 'Повторно запросить роль можно на странице роли.',
                'en': 'You can re-request role on the role page.'
            }[lang]
        )
        assert render_template('emails/role_deprived_maillist.txt', {'role': role, 'user': user}) == '\n'.join(expected_lines)


@pytest.mark.parametrize('add_deprive_comment', [True, False])
@pytest.mark.parametrize(('deprive_robot', 'depriver_sex', 'deprive_verb'), [
    (True, None, {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'M', {'ru': 'отозвал', 'en': 'revoked'}),
    (False, 'F', {'ru': 'отозвала', 'en': 'revoked'}),
])
@pytest.mark.parametrize(('responsible_sex', 'responsible_word'), [
    ('M', {'ru': 'ответственным', 'en': 'responsible'}),
    ('F', {'ru': 'ответственной', 'en': 'responsible'}),
])
def test_role_deprived_maillist__group_role(
        lang: str,
        add_deprive_comment: bool,
        deprive_robot: bool,
        depriver_sex: Optional[str],
        deprive_verb: Dict[str, str],
        responsible_sex: str,
        responsible_word: Dict[str, str],
):
    system = create_system(role_tree=ROLE_TREE, public=True)
    group = create_group(name=random_slug(), name_en=random_slug())
    responsible = create_user(sex=responsible_sex)
    GroupResponsibility.objects.create(group=group, user=responsible)
    role = Role.objects.request_role(
        requester=create_user(),
        subject=group,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',
    )
    deprive_comment = add_deprive_comment and random_slug() or ''
    depriver = None
    if not deprive_robot:
        depriver = generate_named_user(sex=depriver_sex)
    role.deprive_or_decline(depriver, deprive_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s роль группы "%s" в системе "%s":',
                'en': '%s has %s the role of the group "%s" in the system "%s":'
            }[lang] % (
                depriver and user_mention(depriver) or {'ru': 'Робот', 'en': 'Robot'}[lang],
                deprive_verb[lang],
                group.get_name(lang),
                system.get_name(lang),
            ),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if deprive_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % deprive_comment)
        expected_lines.append(
            {
                'ru': 'Повторно запросить роль можно на странице роли.',
                'en': 'You can re-request role on the role page.'
            }[lang]
        )
        assert render_template('emails/role_deprived_maillist.txt', {'role': role, 'user': responsible}) == '\n'.join(expected_lines)

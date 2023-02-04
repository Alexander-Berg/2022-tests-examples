# -*- coding: utf-8 -*-
import random
from typing import Dict, Optional

import pytest
from django.conf import settings
from django.utils import translation

from idm.core.models import Role
from idm.tests.templates.emails.test_include import approver
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


@pytest.mark.parametrize('add_decline_comment', [True, False])
@pytest.mark.parametrize(('decline_robot', 'decliner_sex', 'decline_verb'), [
    (True, None, {'ru': 'отклонил', 'en': 'declined'}),
    (False, 'M', {'ru': 'отклонил', 'en': 'declined'}),
    (False, 'F', {'ru': 'отклонила', 'en': 'declined'}),
])
def test_role_declined__user_role(
        lang: str,
        add_decline_comment: bool,
        decline_robot: bool,
        decliner_sex: Optional[str],
        decline_verb: Dict[str, str],
):
    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{approver(create_user())}]',
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
    deprive_comment = add_decline_comment and random_slug() or ''
    depriver = None
    if not decline_robot:
        depriver = generate_named_user(sex=decliner_sex)
    role.deprive_or_decline(depriver, deprive_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s запрос роли в системе "%s" для пользователя %s:',
                'en': '%s %s your role request in the system "%s" for user %s:',
            }[lang] % (
                depriver and user_mention(depriver) or {'ru': 'Робот', 'en': 'Robot'}[lang],
                decline_verb[lang],
                system.get_name(lang),
                user_mention(user),
            ),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if deprive_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % deprive_comment)
        assert render_template(
            'emails/role_declined.txt',
            {'role': role, 'user': user, 'action': role.actions.select_related('requester').first()},
        ) == '\n'.join(expected_lines).strip()


@pytest.mark.parametrize('add_decline_comment', [True, False])
@pytest.mark.parametrize(('decline_robot', 'decliner_sex', 'decline_verb'), [
    (True, None, {'ru': 'отклонил', 'en': 'declined'}),
    (False, 'M', {'ru': 'отклонил', 'en': 'declined'}),
    (False, 'F', {'ru': 'отклонила', 'en': 'declined'}),
])
@pytest.mark.parametrize(('responsible_sex', 'responsible_word'), [
    ('M', {'ru': 'ответственным', 'en': 'responsible'}),
    ('F', {'ru': 'ответственной', 'en': 'responsible'}),
])
def test_role_declined__group_role(
        lang: str,
        add_decline_comment: bool,
        decline_robot: bool,
        decliner_sex: Optional[str],
        decline_verb: Dict[str, str],
        responsible_sex: str,
        responsible_word: Dict[str, str],
):
    system = create_system(
        role_tree=ROLE_TREE,
        public=True,
        group_workflow=f'approvers = [{approver(create_user())}]',
    )
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
    decline_comment = add_decline_comment and random_slug() or ''
    decliner = None
    if not decline_robot:
        decliner = generate_named_user(sex=decliner_sex)
    role.deprive_or_decline(decliner, decline_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s запрос роли в системе "%s" на группу "%s", в которой вы являетесь %s:',
                'en': '%s %s a role request in the system "%s" for group "%s" you are %s in:',
            }[lang] % (
                decliner and user_mention(decliner) or {'ru': 'Робот', 'en': 'Robot'}[lang],
                decline_verb[lang],
                system.get_name(lang),
                group.get_name(lang),
                responsible_word[lang],
            ),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if decline_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % decline_comment)
        assert render_template(
            'emails/role_declined.txt',
            {'role': role, 'user': responsible, 'action': role.actions.select_related('requester').first()},
        ) == '\n'.join(expected_lines).strip()


@pytest.mark.parametrize('add_decline_comment', [True, False])
@pytest.mark.parametrize(('decline_robot', 'decliner_sex', 'decline_verb'), [
    (True, None, {'ru': 'отклонил', 'en': 'declined'}),
    (False, 'M', {'ru': 'отклонил', 'en': 'declined'}),
    (False, 'F', {'ru': 'отклонила', 'en': 'declined'}),
])
def test_role_declined_maillist__user_role(
        lang: str,
        add_decline_comment: bool,
        decline_robot: bool,
        decliner_sex: Optional[str],
        decline_verb: Dict[str, str],
):
    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{approver(create_user())}]',
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
    decline_comment = add_decline_comment and random_slug() or ''
    decliner = None
    if not decline_robot:
        decliner = generate_named_user(sex=decliner_sex)
    role.deprive_or_decline(decliner, decline_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s запрос роли сотруднику %s в системе "%s":',
                'en': '%s %s a role request for user %s in the system "%s":'
            }[lang] % (
                decliner and user_mention(decliner) or {'ru': 'Робот', 'en': 'Robot'}[lang],
                decline_verb[lang],
                user_mention(user),
                system.get_name(lang),
            ),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if decline_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % decline_comment)
        assert render_template(
            'emails/role_declined_maillist.txt',
            {'role': role, 'user': user, 'action': role.actions.select_related('requester').first()},
        ) == '\n'.join(expected_lines).strip()


@pytest.mark.parametrize('add_decline_comment', [True, False])
@pytest.mark.parametrize(('decline_robot', 'decliner_sex', 'decline_verb'), [
    (True, None, {'ru': 'отклонил', 'en': 'declined'}),
    (False, 'M', {'ru': 'отклонил', 'en': 'declined'}),
    (False, 'F', {'ru': 'отклонила', 'en': 'declined'}),
])
@pytest.mark.parametrize(('responsible_sex', 'responsible_word'), [
    ('M', {'ru': 'ответственным', 'en': 'responsible'}),
    ('F', {'ru': 'ответственной', 'en': 'responsible'}),
])
def test_role_declined_maillist__group_role(
        lang: str,
        add_decline_comment: bool,
        decline_robot: bool,
        decliner_sex: Optional[str],
        decline_verb: Dict[str, str],
        responsible_sex: str,
        responsible_word: Dict[str, str],
):
    system = create_system(
        role_tree=ROLE_TREE,
        public=True,
        group_workflow=f'approvers = [{approver(create_user())}]',
    )
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
    decline_comment = add_decline_comment and random_slug() or ''
    decliner = None
    if not decline_robot:
        decliner = generate_named_user(sex=decliner_sex)
    role.deprive_or_decline(decliner, decline_comment, bypass_checks=True)

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s запрос роли на группу "%s" в системе "%s":',
                'en': '%s %s a role request for group "%s" in the system "%s":'
            }[lang] % (
                decliner and user_mention(decliner) or {'ru': 'Робот', 'en': 'Robot'}[lang],
                decline_verb[lang],
                group.get_name(lang),
                system.get_name(lang),
            ),
            role.email_humanize(lang) + f' ({settings.IDM_BASE_URL + role.get_absolute_url()})',
            '',
        ]
        if decline_comment:
            expected_lines.append({'ru': 'Комментарий: %s', 'en': 'Comment: %s'}[lang] % decline_comment)
        assert render_template(
            'emails/role_declined_maillist.txt',
            {'role': role, 'user': responsible, 'action': role.actions.select_related('requester').first()},
        ) == '\n'.join(expected_lines).strip()

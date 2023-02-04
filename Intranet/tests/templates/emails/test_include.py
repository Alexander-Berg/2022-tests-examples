# -*- coding: utf-8 -*-
import random
from typing import Any, Dict, List

import pytest
from django.conf import settings
from django.utils import translation

from idm.core.models import Role
from idm.tests.templates.utils import (LANG_CODES, generate_approvers,
                                       generate_named_user, group_mention,
                                       render_template, user_mention)
from idm.tests.utils import (create_group, create_system, create_user,
                             random_slug)
from idm.users.models import User
from idm.utils.i18n import LANG_UNIONS

HIDE_AFFTER = 7

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


def test_approvers__one_approver(lang):
    approvers = generate_approvers(1)
    with translation.override(lang):
        assert render_template(
            'emails/includes/approvers.txt',
            {'approvers': approvers, 'conjunction': LANG_UNIONS['or']}
        ) == user_mention(approvers[0])


def test_approvers__two_approvers(lang):
    approvers = generate_approvers(2)
    with translation.override(lang):
        assert render_template(
            'emails/includes/approvers.txt',
            {'approvers': approvers, 'conjunction': LANG_UNIONS['or']}
        ) == f' {LANG_UNIONS["or"]} '.join(user_mention(user) for user in approvers)


@pytest.mark.parametrize('approvers_count', list(range(3, HIDE_AFFTER)))
def test_approvers__several_approvers(lang: str, approvers_count):
    approvers = generate_approvers(approvers_count)

    with translation.override(lang):
        expected = ', '.join(user_mention(user) for user in approvers[:-1])
        expected += f' {LANG_UNIONS["or"]} {user_mention(approvers[-1])}'

        assert render_template(
            'emails/includes/approvers.txt',
            {'approvers': approvers, 'conjunction': LANG_UNIONS['or']}
        ) == expected


def test_approvers__hiding_one(lang: str):
    yet_another_notice = {
        'ru': ' и ещё 1 человек',
        'en': ' and yet another person'
    }

    approvers = generate_approvers(HIDE_AFFTER)
    with translation.override(lang):
        expected = ', '.join(user_mention(user) for user in approvers[:HIDE_AFFTER - 1]) + yet_another_notice[lang]
        assert render_template(
            'emails/includes/approvers.txt',
            {'approvers': approvers, 'conjunction': LANG_UNIONS['and']}
        ) == expected


@pytest.mark.parametrize('extra_count', (2, 22))
def test_approvers__hiding_several(lang: str, extra_count: int):
    yet_another_notice = {
        'ru': ' и ещё %d человека',
        'en': ' and other %d persons'
    }

    approvers = generate_approvers(HIDE_AFFTER + extra_count)
    with translation.override(lang):
        expected = ', '.join(user_mention(user) for user in approvers[:HIDE_AFFTER - 1]) + yet_another_notice[lang]
        assert render_template(
            'emails/includes/approvers.txt',
            {'approvers': approvers, 'conjunction': LANG_UNIONS['and']}
        ) == expected % (extra_count + 1)


def test_role_and_url(lang: str):
    system = create_system(role_tree=ROLE_TREE, public=True)
    role = Role.objects.request_role(
        requester=create_user(),
        subject=create_user(),
        system=system,
        comment='',
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)}
    )

    with translation.override(lang):
        assert render_template('emails/includes/role_and_url.txt', {'role': role}) == '\n'.join([
            role.email_humanize(lang),
            f'URL: {settings.IDM_BASE_URL + role.get_absolute_url()}',
        ])


def approver(username: str, priority: int = None):
    if priority:
        return f'approver("{username}", priority={priority})'
    return f'"{username}"'


def any_approver(approver_group: Dict[User, int]) -> str:
    return f'any_from([{",".join(approver(user.username, priority) for user, priority in approver_group.items())}])'


VIEW_FULL_LIST = {
        'ru': '(полный список можно посмотреть в карточке запроса)',
        'en': '(see the full list in the request card)',
    }


@pytest.mark.parametrize('users_per_group', (1, 2, HIDE_AFFTER))
def test_approver_groups__one_group(lang: str, users_per_group: int):
    approver_group = {create_user(): 1 for _ in range(users_per_group)}  # две группы подтверждающих
    system = create_system(role_tree=ROLE_TREE, workflow=f'approvers = {any_approver(approver_group)}', public=True)
    role = Role.objects.request_role(
        requester=create_user(),
        subject=create_user(),
        system=system,
        comment='',
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)}
    )

    with translation.override(lang):
        expected_lines = [render_template(
            'emails/includes/approvers.txt', {'approvers': list(approver_group), 'conjunction': LANG_UNIONS["and"]},
        )]

        if users_per_group >= HIDE_AFFTER:
            expected_lines.append(VIEW_FULL_LIST[lang])

        assert render_template(
            'emails/includes/approvers_groups.txt',
            {'groups': role.get_main_approvers_for_all_groups()},
        ) == '\n'.join(expected_lines)


def test_approver_groups__several_groups(lang: str):
    approver_groups = [{create_user(): 1 for _ in range(count)} for count in (1, 2, HIDE_AFFTER)]
    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(any_approver(group) for group in approver_groups)}]',
        public=True,
    )
    role = Role.objects.request_role(
        requester=create_user(),
        subject=create_user(),
        system=system,
        comment='',
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)}
    )

    with translation.override(lang):
        groups = []
        for group in approver_groups:
            value = render_template(
                'emails/includes/approvers.txt',
                {'approvers': list(group), 'conjunction': LANG_UNIONS['or']},
            )
            groups.append(f'[{value}]')

        expected_lines = [' , '.join(groups[:-1]) + f' {LANG_UNIONS["and"]} ' + groups[-1], VIEW_FULL_LIST[lang]]

        assert render_template(
            'emails/includes/approvers_groups.txt',
            {'groups': role.get_main_approvers_for_all_groups()},
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize(('group_members', 'expected_intro'), [
    ([{'sex': 'F'}], {'ru': 'Основная подтверждающая, оповещена', 'en': 'Primary approver, notified'}),
    ([{'sex': 'M'}], {'ru': 'Основной подтверждающий, оповещен', 'en': 'Primary approver, notified'}),
    ([{}], {'ru': 'Основной подтверждающий, оповещен', 'en': 'Primary approver, notified'}),
    ([{'sex': 'F'}, {'sex': 'M'}, ], {'ru': 'Основные подтверждающие, оповещены', 'en': 'Primary approvers, notified'}),
])
@pytest.mark.parametrize('additional', (True, False))
def test_all_approvers__one_group(
        lang: str,
        group_members: List[Dict[str, Any]],
        expected_intro: Dict[str, str],
        additional: bool,
):
    approver_group = {create_user(**user_options): 1 for user_options in group_members}
    if additional:
        approver_group.update({create_user(): priority for priority in (2, 3)})


    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = {any_approver(approver_group)}',
        public=True,
    )
    role = Role.objects.request_role(
        requester=create_user(),
        subject=create_user(),
        system=system,
        comment='',
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)}
    )

    with translation.override(lang):
        expected_lines = [
            f'{expected_intro[lang]}: ' + render_template(
                'emails/includes/approvers_groups.txt',
                {'groups': role.get_main_approvers_for_all_groups()},
            ),
        ]
        if additional:
            expected_lines.append(
                {'ru': 'Кто еще может подтвердить: ', 'en': 'Alternate approvers: '}[lang] +
                render_template(
                    'emails/includes/approvers_groups.txt',
                    {'groups': role.get_additional_approvers_for_all_groups()},
            ))
        else:
            expected_lines.append(
                {'ru': 'Дополнительные подтверждающие отсутствуют', 'en': 'There is no other approvers'}[lang]
            )

        assert render_template(
            'emails/includes/all_approvers.txt',
            {'role': role},
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize('additional', (True, False))
def test_all_approvers__several_group(
        lang: str,
        additional: bool,
):
    approver_groups = [{create_user(): 1 for _ in range(group_size)} for group_size in (1, 2, 3)]
    if additional:
        for approver_group in approver_groups:
            approver_group.update({create_user(): priority for priority in (2, 3)})

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(any_approver(group) for group in approver_groups)}]',
        public=True,
    )
    role = Role.objects.request_role(
        requester=create_user(),
        subject=create_user(),
        system=system,
        comment='',
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)}
    )

    with translation.override(lang):
        expected_lines = [
            {'ru': 'Основные подтверждающие, оповещены: ', 'en': 'Primary approvers, notified: '}[lang] +
            render_template(
                'emails/includes/approvers_groups.txt',
                {'groups': role.get_main_approvers_for_all_groups()},
            ),
        ]
        if additional:
            expected_lines.append(
                {'ru': 'Кто еще может подтвердить: ', 'en': 'Alternate approvers: '}[lang] +
                render_template(
                    'emails/includes/approvers_groups.txt',
                    {'groups': role.get_additional_approvers_for_all_groups()},
            ))
        else:
            expected_lines.append(
                {'ru': 'Дополнительные подтверждающие отсутствуют', 'en': 'There is no other approvers'}[lang]
            )

        assert render_template(
            'emails/includes/all_approvers.txt',
            {'role': role},
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize('add_comment', (True, False))
def test_request_details__user_role(
        lang: str,
        add_comment: bool,
):
    comment = add_comment and random_slug() or ''
    approver_groups = [{create_user(): None for _ in range(group_size)} for group_size in (1, 2, 3)]

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(any_approver(group) for group in approver_groups)}]',
        public=True,
    )
    requester = generate_named_user()
    user = generate_named_user()
    role = Role.objects.request_role(
        requester=requester,
        subject=user,
        system=system,
        comment=comment,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)}
    )

    with translation.override(lang):
        expected_lines = [
            {'ru': 'Система: "%s"', 'en': 'System: "%s"'}[lang] % system.get_name(lang),

            {'ru': 'Кто запросил: ', 'en': 'Requested by: '}[lang] +
            user_mention(requester),

            {'ru': 'Для кого запрошена роль: ', 'en': 'Request for: '}[lang] +
            user_mention(user),

            render_template(
                'emails/includes/role_and_url.txt',
                {'role': role},
            ),
            ]
        if comment:
            expected_lines.append({'ru': 'Комментарий: ', 'en': 'Comment: '}[lang] + comment)
        expected_lines.append(
            render_template(
                'emails/includes/all_approvers.txt',
                {'role': role},
            )
        )
        assert render_template(
            'emails/includes/request_details.txt',
            {'role': role, 'user': user, 'requester': requester, 'comment': comment},
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize('add_comment', (True, False))
def test_request_details__group_role(
        lang: str,
        add_comment: bool,
):
    comment = add_comment and random_slug() or ''
    approver_groups = [{create_user(): None for _ in range(group_size)} for group_size in (1, 2, 3)]

    system = create_system(
        role_tree=ROLE_TREE,
        group_workflow=f'approvers = [{", ".join(any_approver(group) for group in approver_groups)}]',
        public=True,
    )
    requester = generate_named_user()
    group = create_group(name=random_slug(), name_en=random_slug())
    role = Role.objects.request_role(
        requester=requester,
        subject=group,
        system=system,
        comment=comment,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)}
    )

    with translation.override(lang):
        expected_lines = [
            {'ru': 'Система: "%s"', 'en': 'System: "%s"'}[lang] % system.get_name(lang),

            {'ru': 'Кто запросил: ', 'en': 'Requested by: '}[lang] +
            user_mention(requester),

            {'ru': 'Для кого запрошена роль: ', 'en': 'Request for: '}[lang] +
            {'ru': 'Группа "%s"', 'en': 'Group "%s"'}[lang] % group_mention(group),

            render_template(
                'emails/includes/role_and_url.txt',
                {'role': role},
            ),
            ]
        if comment:
            expected_lines.append({'ru': 'Комментарий: ', 'en': 'Comment: '}[lang] + comment)
        expected_lines.append(
            render_template(
                'emails/includes/all_approvers.txt',
                {'role': role},
            )
        )
        assert render_template(
            'emails/includes/request_details.txt',
            {'role': role, 'group': group, 'requester': requester, 'comment': comment},
        ) == '\n'.join(expected_lines)

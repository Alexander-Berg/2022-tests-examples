# -*- coding: utf-8 -*-
import random
from typing import Dict

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
from idm.utils.i18n import LANG_UNIONS

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


@pytest.mark.parametrize(('requester_sex', 'requester_verb'), [
    ('M', {'ru': 'запросил', 'en': 'requested'}),
    ('F', {'ru': 'запросила', 'en': 'requested'}),
])
def test_role_requested__for_user(lang: str, requester_sex: str, requester_verb: Dict[str, str]):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = generate_named_user(sex=requester_sex)
    user = create_user()
    role = Role.objects.request_role(
        requester=requester,
        subject=user,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s для вас роль в системе "%s":',
                'en': '%s %s the role in the system "%s" for you:'
            }[lang] % (user_mention(requester), requester_verb[lang], system.get_name(lang)),
            ' '.join([
                {'ru': 'Запрос отправлен', 'en': 'Request was sent to'}[lang],
                render_template(
                    'emails/includes/approvers.txt',
                    {'approvers': approvers, 'conjunction': LANG_UNIONS['and']},
                ),
                {'ru': 'и ожидает подтверждения.', 'en': 'and waiting for approval.'}[lang]

            ]),
            {'ru': 'С деталями запроса вы можете ознакомиться ниже.', 'en': 'See the request details below.'}[lang],
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            render_template('emails/includes/all_approvers.txt', {'role': role}) + '.',
            {
                'ru': 'Если вам нужно связаться с подтверждающими напрямую, вы можете отправить им эту ссылку:',
                'en': 'If you want to contact approvers directly, send them this link:',
            }[lang],
            f'{settings.IDM_BASE_URL + role.get_absolute_url()}',
        ]
        assert render_template(
            'emails/role_requested.txt',
            {'role': role, 'requester': requester, 'user': user},
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize(('requester_sex', 'requester_verb'), [
    ('M', {'ru': 'запросил', 'en': 'requested'}),
    ('F', {'ru': 'запросила', 'en': 'requested'}),
])
def test_role_requested__for_robot(lang: str, requester_sex: str, requester_verb: Dict[str, str]):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = generate_named_user(sex=requester_sex)
    user = create_user()
    user.is_robot = True
    user.save()
    role = Role.objects.request_role(
        requester=requester,
        subject=user,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s для робота %s роль в системе "%s":',
                'en': '%s %s for the robot %s role in the system "%s":'
            }[lang] % (user_mention(requester), requester_verb[lang], user.username, system.get_name(lang)),
            ' '.join([
                {'ru': 'Запрос отправлен', 'en': 'Request was sent to'}[lang],
                render_template(
                    'emails/includes/approvers.txt',
                    {'approvers': approvers, 'conjunction': LANG_UNIONS['and']},
                ),
                {'ru': 'и ожидает подтверждения.', 'en': 'and waiting for approval.'}[lang]

            ]),
            {'ru': 'С деталями запроса вы можете ознакомиться ниже.', 'en': 'See the request details below.'}[lang],
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            render_template('emails/includes/all_approvers.txt', {'role': role}) + '.',
            {
                'ru': 'Если вам нужно связаться с подтверждающими напрямую, вы можете отправить им эту ссылку:',
                'en': 'If you want to contact approvers directly, send them this link:',
            }[lang],
            f'{settings.IDM_BASE_URL + role.get_absolute_url()}',
        ]
        assert render_template(
            'emails/role_requested.txt',
            {'role': role, 'requester': requester, 'user': user},
        )  == '\n'.join(expected_lines)



@pytest.mark.parametrize(('requester_sex', 'requester_verb'), [
    ('M', {'ru': 'запросил', 'en': 'requested'}),
    ('F', {'ru': 'запросила', 'en': 'requested'}),
])
@pytest.mark.parametrize(('responsible_sex', 'responsible_word'), [
    ('M', {'ru': 'ответственным', 'en': 'responsible'}),
    ('F', {'ru': 'ответственной', 'en': 'responsible'}),
])
def test_role_requested__for_group(
        lang: str,
        requester_sex: str,
        requester_verb: Dict[str, str],
        responsible_sex: str,
        responsible_word: Dict[str, str]
):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        group_workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = generate_named_user(sex=requester_sex)
    group = create_group(name=random_slug(), name_en=random_slug())
    responsible = create_user(sex=responsible_sex)
    GroupResponsibility.objects.create(group=group, user=responsible)
    role = Role.objects.request_role(
        requester=requester,
        subject=group,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': '%s %s роль в системе "%s" для группы "%s", в которой вы являетесь %s:',
                'en': '%s %s a role in the system "%s" for the group "%s" you are %s in:'
            }[lang] % (
                user_mention(requester),
                requester_verb[lang],
                system.get_name(lang),
                group.get_name(lang),
                responsible_word[lang],
            ),
            ' '.join([
                {'ru': 'Запрос отправлен', 'en': 'Request was sent to'}[lang],
                render_template(
                    'emails/includes/approvers.txt',
                    {'approvers': approvers, 'conjunction': LANG_UNIONS['and']},
                ),
                {'ru': 'и ожидает подтверждения.', 'en': 'and waiting for approval.'}[lang]

            ]),
            {'ru': 'С деталями запроса вы можете ознакомиться ниже.', 'en': 'See the request details below.'}[lang],
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            render_template('emails/includes/all_approvers.txt', {'role': role}) + '.',
            {
                'ru': 'Если вам нужно связаться с подтверждающими напрямую, вы можете отправить им эту ссылку:',
                'en': 'If you want to contact approvers directly, send them this link:',
            }[lang],
            f'{settings.IDM_BASE_URL + role.get_absolute_url()}',
        ]
        assert render_template(
            'emails/role_requested.txt',
            {'role': role, 'requester': requester, 'group': group, 'user': responsible},
        ) == '\n'.join(expected_lines)


@pytest.mark.parametrize(('sex', 'requester_verb', 'responsible_word'), [
    ('M', {'ru': 'запросил', 'en': 'requested'}, {'ru': 'ответственным', 'en': 'responsible'}),
    ('F', {'ru': 'запросила', 'en': 'requested'}, {'ru': 'ответственной', 'en': 'responsible'}),
])
def test_role_requested__for_group_as_responsible(
        lang: str,
        sex: str,
        requester_verb: Dict[str, str],
        responsible_word: Dict[str, str]
):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        group_workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = generate_named_user(sex=sex)
    group = create_group(name=random_slug(), name_en=random_slug())
    GroupResponsibility.objects.create(group=group, user=requester)
    role = Role.objects.request_role(
        requester=requester,
        subject=group,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Вы запросили роль в системе "%s" для группы "%s", в которой вы являетесь %s:',
                'en': 'You have requested a role in the system "%s" for the group "%s" you are %s in:'
            }[lang] % (
                system.get_name(lang),
                group.get_name(lang),
                responsible_word[lang],
            ),
            ' '.join([
                {'ru': 'Запрос отправлен', 'en': 'Request was sent to'}[lang],
                render_template(
                    'emails/includes/approvers.txt',
                    {'approvers': approvers, 'conjunction': LANG_UNIONS['and']},
                ),
                {'ru': 'и ожидает подтверждения.', 'en': 'and waiting for approval.'}[lang]

            ]),
            {'ru': 'С деталями запроса вы можете ознакомиться ниже.', 'en': 'See the request details below.'}[lang],
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            render_template('emails/includes/all_approvers.txt', {'role': role}) + '.',
            {
                'ru': 'Если вам нужно связаться с подтверждающими напрямую, вы можете отправить им эту ссылку:',
                'en': 'If you want to contact approvers directly, send them this link:',
            }[lang],
            f'{settings.IDM_BASE_URL + role.get_absolute_url()}',
        ]
        assert render_template(
            'emails/role_requested.txt',
            {'role': role, 'requester': requester, 'group': group, 'user': requester},
        ) == '\n'.join(expected_lines)


def test_role_requested_maillist(lang: str):
    approvers = [create_user(), create_user()]

    system = create_system(
        role_tree=ROLE_TREE,
        workflow=f'approvers = [{", ".join(f"{approver(user)}" for user in approvers)}]',
        public=True,
    )
    requester = create_user()
    role = Role.objects.request_role(
        requester=requester,
        subject=create_user(),
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',
    )

    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Вы получили запрос, он ожидает вашего подтверждения. С деталями вы можете ознакомиться ниже.',
                'en': 'You have received a request that is waiting for your approval. '
                      'See the details below. To approve or reject the request, go to',
            }[lang],
            '',
            render_template(
                'emails/includes/request_details.txt',
                {'role': role, 'requester': requester, 'user': role.user}
            ),
            '',
            {
                'ru': 'Чтобы подтвердить или отклонить запрос, пожалуйста, перейдите по ссылке:',
                'en': 'Follow link to approve or decline role requests:',
            }[lang] + f' {settings.IDM_BASE_URL + role.get_absolute_url()}',
            {
                'ru': 'Если вы не готовы подтвердить или отклонить запрос, пожалуйста, воспользуйтесь вариантом '
                      '//Воздержаться//. Это позволит системе своевременно выбрать другого основного подтверждающего.',
                'en': 'Please, choose option //Ignore// if you not ready to make a decision',
            }[lang],

        ]
        assert render_template(
            'emails/role_requested_maillist.txt',
            {'role': role, 'requester': requester, 'user': role.user},
        ) == '\n'.join(expected_lines)


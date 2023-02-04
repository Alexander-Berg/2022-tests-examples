# -*- coding: utf-8 -*-
import collections
import random

import pytest
from django.conf import settings
from django.utils import translation

from idm.core.models import Role
from idm.tests.templates.utils import (LANG_CODES, generate_named_user,
                                       render_template)
from idm.tests.utils import (create_system, random_slug)

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


@pytest.mark.parametrize('for_email', [True, False])
def test_approve_role(lang: str, for_email: bool):
    system = create_system(role_tree=ROLE_TREE, public=True)
    requester = generate_named_user()
    user = generate_named_user()
    role = Role.objects.request_role(
        requester=requester,
        subject=user,
        system=system,
        comment=random_slug(),
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)}
    )

    with translation.override(lang):
        expected_lines = collections.deque([
            '',
            render_template(
                'emails/includes/request_details.txt',
                {'role': role, 'user': user, 'requester': requester},
            ),
            '',
            {
                'ru': f'Запросы, ожидающие вашего решения: {settings.IDM_BASE_URL}/queue/',
                'en': f'All requests waiting your decision: {settings.IDM_BASE_URL}/queue/',
            }[lang]
        ])

        if for_email:
            expected_lines.appendleft({
                'ru': 'Вы получили запрос, он ожидает вашего подтверждения. С деталями вы можете ознакомиться ниже.'
                      ' Чтобы подтвердить или отклонить запрос, пожалуйста, перейдите по ссылке:',
                'en': 'You have received a request that is waiting for your approval. See the details below. '
                      'To approve or reject the request, go to:',
            }[lang] + f' {settings.IDM_BASE_URL + role.get_absolute_url()}')

            expected_lines.append({
              'ru': 'Если вы не готовы подтвердить или отклонить запрос, пожалуйста, воспользуйтесь вариантом «Воздержаться».'
                    ' Это позволит системе своевременно выбрать другого основного подтверждающего.',
              'en': 'Please, choose option «Ignore» if you not ready to make a decision.',
          }[lang])
        else:
            expected_lines.appendleft({'ru': 'Запрос роли:', 'en': 'Role request:'}[lang])

            expected_lines.append(
                {'ru': 'Вы можете оставить комментарий ниже:', 'en': 'You can left your comments below:'}[lang]
            )

        assert render_template(
            'emails/approve_role.txt',
            {'role': role, 'user': user, 'requester': requester, 'for_email': for_email}
        ) == '\n'.join(expected_lines)


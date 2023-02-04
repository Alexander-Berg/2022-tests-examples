import random

import pytest
from django.conf import settings
from django.utils import translation

from idm.core.models import Role
from idm.tests.templates.utils import (LANG_CODES, generate_named_user,
                                       render_template, user_mention)
from idm.tests.utils import (create_group, create_system, create_user,
                             random_slug)

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
}


def test_role_expired__user_role(lang: str):
    system = create_system(role_tree=ROLE_TREE, public=True)
    user = generate_named_user()
    role = Role.objects.request_role(
        requester=create_user(),
        subject=user,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',

    )
    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Запрошенная для %s роль в системе "%s" не получила подтверждения в срок:',
                'en': 'A role, requested for %s in the system "%s", was not approved in time:',
            }[lang] % (user_mention(user), system.get_name(lang)),
            '',
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            {
                'ru': 'Вы можете запросить роль повторно: %s',
                'en': 'You can re-request role: %s',
            }[lang] % settings.IDM_BASE_URL + '/',
        ]
        assert render_template('emails/role_expired.txt', {'role': role}) == '\n'.join(expected_lines)


def test_role_expired__group_role(lang: str):
    system = create_system(role_tree=ROLE_TREE, public=True)
    group = create_group(name=random_slug(), name_en=random_slug())
    role = Role.objects.request_role(
        requester=create_user(),
        subject=group,
        system=system,
        data={'role': ROLE_SLUG},
        fields_data={FIELD_SLUG: random.randint(0, 10 ** 6)},
        comment='',

    )
    with translation.override(lang):
        expected_lines = [
            {
                'ru': 'Запрошенная для группы "%s" роль в системе "%s" не получила подтверждения в срок:',
                'en': 'A role, requested for the group "%s" in the system "%s"was not approved in time:',
            }[lang] % (group.get_name(lang), system.get_name(lang)),  # FIXME system.get_name(lang)
            '',
            render_template('emails/includes/role_and_url.txt', {'role': role}),
            {
                'ru': 'Вы можете запросить роль повторно: %s',
                'en': 'You can re-request role: %s',
            }[lang] % settings.IDM_BASE_URL + '/',
        ]
        assert render_template('emails/role_expired.txt', {'role': role}) == '\n'.join(expected_lines)

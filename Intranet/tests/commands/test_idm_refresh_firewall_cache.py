from unittest import mock

import pytest
from django.core import management
from django.core.cache import cache

from idm.api.v1 import firewall
from idm.tests.utils import create_user, random_slug


@pytest.mark.parametrize('pass_system', (True, False))
@pytest.mark.parametrize('pass_user', (True, False))
@pytest.mark.parametrize('pass_expand_groups', (True, False))
def test_refresh_firewall_cache(pass_user, pass_system, pass_expand_groups):
    user = create_user(pass_user and random_slug() or firewall.FIREWALL_ROBOT_USERNAME)
    system_slug: str = pass_system and random_slug() or None
    cache_key = firewall.get_cache_key(user, system_slug, expand_groups=pass_expand_groups)
    assert cache.get(cache_key) is None

    call_args = ['idm_refresh_firewall_cache']
    if pass_user:
        call_args.extend(('--user', user.username))
    if pass_system:
        call_args.extend(('--system', system_slug))
    if pass_expand_groups:
        call_args.append('--expand-groups')

    mocked_value = [{
        'system': system_slug,
        'username': user.username,
        'expand_groups': pass_expand_groups,
        'random': random_slug()
    }]
    with mock.patch('idm.api.v1.firewall.get_firewall_rules', return_value=mocked_value) as get_firewall_rules_mock:
        management.call_command(*call_args)

    get_firewall_rules_mock.assert_called_once_with(user, system_slug, expand_groups=pass_expand_groups)
    assert cache.get(cache_key) == mocked_value


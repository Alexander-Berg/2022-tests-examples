import pytest

from django.urls import reverse
from django.contrib.contenttypes.models import ContentType

from intranet.search.core.models import Organization
from intranet.search.tests.helpers import models_helpers as mh


pytestmark = pytest.mark.django_db(transaction=False)


def test_get_all_permissions(api_client):
    ct = ContentType.objects.get_for_model(Organization)
    user = mh.User()
    common_permission = mh.Permission(content_type=ct, codename='common_permission')
    user_permission = mh.Permission(content_type=ct, codename='view_peoplesearch')
    user.user_permissions.add(common_permission, user_permission)

    group = mh.Group()
    group_permission = mh.Permission(content_type=ct, codename='view_suggest_people')
    group.permissions.add(group_permission)
    user.groups.add(group)

    url = reverse('permissions-get')
    r = api_client.get(url, {'user': user.username})

    expected_permissions = {
        'common': ['common_permission'],
        'search': ['peoplesearch'],
        'suggest': ['people'],
    }

    assert r.status_code == 200, r.content
    assert r.json() == expected_permissions


def test_is_superuser_permission(api_client):
    user = mh.User(is_superuser=True)

    url = reverse('permissions-get')
    r = api_client.get(url, {'user': user.username})

    assert r.status_code == 200, r.content
    assert 'is_superuser' in r.json()['common']


def test_user_does_not_exists(api_client):
    url = reverse('permissions-get')
    r = api_client.get(url, {'user': 'unknown_user'})
    assert r.status_code == 404

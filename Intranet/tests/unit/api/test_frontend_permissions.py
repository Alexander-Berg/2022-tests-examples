import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from plan.common.internal_roles import get_internal_roles


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('staff_role', ('not_real_role', 'support'))
def test_get_for_other_internal_roles(staff_factory, client, staff_role):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)

    response = client.json.get(
        reverse('api-frontend:permission-list'),
    )

    assert response.status_code == 403


@pytest.mark.parametrize('staff_role', ('own_only_viewer', 'services_viewer', 'full_access'))
def test_get_for_new_internal_roles(staff_factory, client, staff_role):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)
    full_front_permissions = set(settings.ABC_INTERNAL_ROLES_PERMISSIONS.get('full_access'))

    response = client.json.get(
        reverse('api-frontend:permission-list'),
    )

    assert response.status_code == 200
    results = response.json()

    perms = get_internal_roles(staff_for.user) & full_front_permissions
    for perm in perms:
        assert perm in results['results']


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer', 'services_viewer', 'full_access', 'not_real_role')
)
def test_post_for_new_internal_roles(staff_factory, client, staff_role):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)
    response = client.json.post(
        reverse('api-frontend:permission-list'),
        data=['can_edit'],
    )
    if staff_role in {'full_access'}:
        assert response.status_code == 405
    else:
        assert response.status_code == 403


@pytest.mark.parametrize(
    'staff_role,must_exist',
    [('own_only_viewer', False), ('services_viewer', False), ('full_access', True)]
)
def test_all_contacts(client, staff_factory, staff_role, must_exist):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)
    response = client.json.get(
        reverse('api-frontend:permission-list'),
    )
    results = response.json()['results']
    assert ('view_contacts' in results) == must_exist


@pytest.mark.parametrize(
    'staff_role,permissions',
    [
        ('own_only_viewer', []),
        ('services_viewer', []),
        ('full_access', ['view_description', 'view_department', 'view_activity']),
    ]
)
def test_description_departments_activity(client, staff_factory, staff_role, permissions):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)
    response = client.json.get(
        reverse('api-frontend:permission-list'),
    )
    results = response.json()['results']
    assert all(permission in results for permission in permissions)

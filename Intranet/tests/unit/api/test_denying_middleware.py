import pytest

from django.core.urlresolvers import reverse

DUTY_VIEWERS = {'services_viewer', 'full_access', 'own_only_viewer'}
pytestmark = pytest.mark.django_db


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer', 'services_viewer', 'full_access')
)
@pytest.mark.parametrize(
    'handler',
    ('duty-gap', 'duty-shift', 'allowforduty')
)
def test_v3_get_duty_for_internal_roles(staff_factory, client, staff_role, handler, api='v3'):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)

    response = client.json.get(
        reverse('api-%s:%s-list' % (api, handler)),
    )

    if staff_role in DUTY_VIEWERS:
        assert response.status_code == 400
    else:
        assert response.status_code == 403


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer', 'services_viewer', 'full_access')
)
@pytest.mark.parametrize(
    'handler',
    ('duty-gap', 'duty-shift', 'allowforduty')
)
def test_v4_get_duty_for_internal_roles(staff_factory, client, staff_role, handler, api='v4'):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)

    response = client.json.get(
        reverse('api-%s:%s-list' % (api, handler)),
    )
    if staff_role in DUTY_VIEWERS:
        assert response.status_code == 400
    else:
        assert response.status_code == 403


@pytest.mark.parametrize(
    'staff_role',
    ('services_viewer', 'full_access')
)
@pytest.mark.parametrize(
    'api',
    ('v4', 'v3')
)
def test_get_duty_schedule(staff_factory, client, api, staff_role, handler='duty-schedule'):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)

    response = client.json.get(
        reverse('api-%s:%s-list' % (api, handler)),
    )
    assert response.status_code == 200


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer', 'services_viewer', 'full_access')
)
@pytest.mark.parametrize(
    'api',
    ('v4', 'v3')
)
@pytest.mark.parametrize(
    'handler',
    ('service-department', 'service-member', 'service-responsible', 'service', 'role-scope', )
)
def test_get_service_info(staff_factory, client, api, staff_role, handler):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)

    response = client.json.get(
        reverse('api-%s:%s-list' % (api, handler)),
    )
    assert response.status_code == 200


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer', 'services_viewer', 'full_access')
)
@pytest.mark.parametrize(
    'api',
    ('v4', 'v3')
)
@pytest.mark.parametrize(
    'handler',
    (
        'resource-type-category', 'resource-types', 'resource-tag-category', 'resource-tag',
        'resource-consumer', 'resource-request', 'resource',
        'role', 'complaint', 'appeal', 'service-contact'
    )
)
def test_get_deny(staff_factory, client, api, staff_role, handler):
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)

    response = client.json.get(
        reverse('api-%s:%s-list' % (api, handler)),
    )
    assert response.status_code == 403 if staff_role != 'full_access' else 200

from mock import patch
import pytest

from django.core.urlresolvers import reverse

from common import factories
from utils import MockIdmResponse, Response


pytestmark = pytest.mark.django_db


@pytest.fixture
def patched_session():
    mock_response = MockIdmResponse({
        'meta': {
            'total_count': 1,
            'next': None
        },
        'objects': [
            {
                'user': {
                    'username': 'venom_snake'
                },
                'state': 'requested',
                'id': 1984,
                'node': {
                    'slug': '9001',
                    'value_path': '/services/diamond_dogs/infiltration_unit/*/9001/'
                },
                'granted_at': None
            },
            {
                'group': {
                    'id': 1234,
                },
                'state': 'requested',
                'id': 1985,
                'node': {
                    'slug': '9001',
                    'value_path': '/services/diamond_dogs/infiltration_unit/*/9001/'
                },
                'granted_at': None
            }
        ]
    })

    def mocked_session_send(*args, **kwargs):
        return Response(200, mock_response.response)

    with patch('requests.Session.send') as patched:
        patched.side_effect = mocked_session_send
        yield patched


def test_requested_roles_handle(client, patched_session, patch_tvm):
    ahab = factories.StaffFactory(login='venom_snake')
    big_boss = factories.RoleFactory(id=9001)
    metaservice = factories.ServiceFactory(slug='diamond_dogs')
    service = factories.ServiceFactory(slug='infiltration_unit', parent=metaservice)
    department = factories.DepartmentFactory(staff_id=1234)

    response = client.json.get(
        reverse('services-api:service-requested-roles', args=[service.id])
    )
    assert response.status_code == 200

    results = response.json()
    assert len(results) == 2
    assert results[0]['person']['login'] == ahab.login
    assert results[0]['service']['slug'] == service.slug
    assert results[0]['role']['id'] == big_boss.id
    assert results[0]['state'] == 'requested'

    assert results[1]['department']['id'] == department.id
    assert results[1]['service']['slug'] == service.slug
    assert results[1]['role']['id'] == big_boss.id
    assert results[1]['state'] == 'requested'

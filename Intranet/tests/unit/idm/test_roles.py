from mock import patch
import pytest

from plan.idm import roles

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
            }
        ]
    })

    def mocked_session_send(*args, **kwargs):
        return Response(200, mock_response.response)

    with patch('requests.Session.send') as patched:
        patched.side_effect = mocked_session_send
        yield patched


def test_get_requested_roles_for_service(client, patched_session, patch_tvm):
    ahab = factories.StaffFactory(login='venom_snake')
    big_boss = factories.RoleFactory(id=9001)
    metaservice = factories.ServiceFactory(slug='diamond_dogs')
    service = factories.ServiceFactory(slug='infiltration_unit', parent=metaservice)

    requested_roles = list(roles.get_requested_roles_for_service(service))

    assert requested_roles == [
        roles.Role(
            person=ahab,
            role=big_boss,
            service=service,
            idm_id=1984,
            state='requested',
            granted_at=None
        )
    ]

from mock import patch
import json

import pytest
import pretend

from plan.resources.tasks import sync_with_dispenser

from plan.resources.models import ServiceResource

from common import factories
from utils import Response


pytestmark = [
    pytest.mark.django_db,
    pytest.mark.usefixtures('robot')
]


@pytest.fixture
def data():
    service = factories.ServiceFactory()
    other_service = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(code='dispenser_project')

    return pretend.stub(
        service=service,
        other_service=other_service,
        resource_type=resource_type
    )


@pytest.fixture
def patched_session(data):
    response = {
        'result': [
            {
                'key': 'derp',
                'name': 'Derp project',
                'abcServiceId': data.service.id
            },
            {
                'key': 'herp',
                'name': 'Herp project',
                'abcServiceId': data.other_service.id
            },
            {
                'key': 'herpaderp',
                'name': 'Project without linked abc service'
            }
        ]
    }

    with patch('plan.common.utils.http.Session.get') as patched:
        patched.return_value = Response(200, json.dumps(response))
        yield patched


def test_sync_deletes_projects_removed_from_dispenser(data, patched_session):
    service_resources = [
        factories.ServiceResourceFactory(
            resource=factories.ResourceFactory(type=data.resource_type)
        ) for _ in range(5)
    ]

    sync_with_dispenser()

    for service_resource in service_resources:
        service_resource.refresh_from_db()
        assert service_resource.state == ServiceResource.DEPRIVED


def test_unlinking_project_deprives_serviceresource(data, patched_session):
    resource = factories.ResourceFactory(type=data.resource_type, external_id='herpaderp')
    service_resource = factories.ServiceResourceFactory(resource=resource)

    assert service_resource.state == ServiceResource.REQUESTED

    sync_with_dispenser()

    service_resource.refresh_from_db()
    assert service_resource.state == ServiceResource.DEPRIVED

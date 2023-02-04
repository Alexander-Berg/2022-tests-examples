from mock import patch
import json

import pytest
import pretend

from plan.api.exceptions import IntegrationError
from plan.resources.importers import dispenser
from plan.resources.models import Resource, ServiceResource

from common import factories
from utils import Response


pytestmark = pytest.mark.django_db


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
                'name': 'Project without linked abc service',
                'parentProjectKey': 'herp'
            }
        ]
    }

    with patch('plan.common.utils.http.Session.get') as patched:
        patched.return_value = Response(200, json.dumps(response))
        yield patched


def test_getting_dispenser_projects(data, patched_session):
    projects = list(dispenser.get_projects())

    assert {data.service.id, data.other_service.id} == {project.service.id for project in projects if project.service}


def test_creating_project_resources(data, patched_session):
    for project in dispenser.get_projects():
        resource = project.get_or_create_resource()

        assert resource.external_id == project.key
        assert resource.name == project.name
        assert resource.link == project.link


def test_creating_project_service_resources(data, patched_session):
    for project in dispenser.get_projects():
        service_resource = project.get_or_create_service_resource()
        if service_resource is None:
            continue

        assert service_resource.resource.external_id == project.key
        assert service_resource.resource.name == project.name
        assert service_resource.resource.link == project.link
        assert service_resource.service == project.service


def test_creating_project_resources_does_not_create_duplicates(data, patched_session):
    for _ in range(6):
        for project in dispenser.get_projects():
            service_resource = project.get_or_create_service_resource()

            if service_resource is None:
                continue

            assert service_resource.state == ServiceResource.GRANTED
            assert 1 == (
                Resource
                .objects
                .filter(type=data.resource_type, external_id=project.key)
                .count()
            )


def test_fetching_data_updates_resource_name(data, patched_session):
    project_key = 'herp'
    old_name = 'Hello world'
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id=project_key,
        name=old_name
    )

    projects = {project.key: project for project in dispenser.get_projects()}
    project = projects[project_key]

    project.get_or_create_resource()

    resource.refresh_from_db()
    assert resource.name != old_name
    assert resource.name == project.name


def test_fetching_data_updates_resource_link(data, patched_session):
    old_link = 'triforce of the gods?'
    project_key = 'herp'
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id=project_key,
        link=old_link
    )

    projects = {project.key: project for project in dispenser.get_projects()}
    project = projects[project_key]

    project.get_or_create_resource()

    resource.refresh_from_db()
    assert resource.link != old_link
    assert resource.link == project.link


def test_fetching_data_rerequests_service_resource(data, patched_session):
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='derp'
    )

    service_resource = factories.ServiceResourceFactory(resource=resource)
    assert service_resource.service != data.service

    for project in dispenser.get_projects():
        project.get_or_create_service_resource()

    service_resource.refresh_from_db()
    assert service_resource.state == ServiceResource.DEPRIVED
    assert ServiceResource.objects.filter(resource=resource).count() == 2


def test_fetching_assigns_parent(data, patched_session):
    for project in dispenser.get_projects():
        project.get_or_create_resource()

    resource = Resource.objects.get(external_id='herpaderp')
    assert resource.parent.external_id == 'herp'


def test_fetching_updates_parent(data, patched_session):
    old_parent = factories.ResourceFactory(
        type=data.resource_type,
        external_id='hurrdurr'
    )
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='herpaderp',
        parent=old_parent
    )

    for project in dispenser.get_projects():
        project.get_or_create_resource()

    resource.refresh_from_db()
    assert resource.parent.external_id != old_parent.external_id


def test_fetching_clears_parent(data, patched_session):
    old_parent = factories.ResourceFactory(
        type=data.resource_type,
        external_id='hurrdurr'
    )
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='herp',
        parent=old_parent
    )
    for project in dispenser.get_projects():
        project.get_or_create_resource()

    resource.refresh_from_db()
    assert resource.parent is None


def test_fetching_raises_error(data):
    with pytest.raises(IntegrationError):
        with patch('plan.common.utils.http.Session.get') as patched:
            patched.return_value = Response(500, '')
            list(dispenser.get_projects())

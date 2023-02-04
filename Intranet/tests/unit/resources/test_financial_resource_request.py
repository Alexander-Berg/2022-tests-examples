from unittest.mock import patch

from django.urls import reverse
import pytest

from plan.roles.models import Role

from plan.internal_roles.utils import assign_perms_for_internal_role
from plan.resources.models import Resource, ServiceResource
from plan.resources.tasks import deprive_financial_resource_connected_with_inactive_services
from common import factories
from utils import Response
import plan.resources.suppliers.metrika

pytestmark = [
    pytest.mark.django_db,
    pytest.mark.usefixtures('robot'),
]


@pytest.fixture
def metrika_resource_type(owner_role):
    resource_type = factories.ResourceTypeFactory(
        code='metrika_counter_id', supplier_plugin='metrika', has_multiple_consumers=True, has_automated_grant=True,
    )
    return resource_type


@pytest.fixture(autouse=True)
def patch_create_and_delete(monkeypatch):
    def fake_create(self, service_resource):
        return service_resource.resource.external_id, []

    monkeypatch.setattr(plan.resources.suppliers.metrika.FinancialBasePlugin, 'create', fake_create)
    monkeypatch.setattr(plan.resources.suppliers.metrika.FinancialBasePlugin, 'delete', lambda *args, **kwargs: None)


def test_service_role(client, metrika_resource_type, staff_factory):
    service = factories.ServiceFactory()
    staff = staff_factory()
    client.login(staff.login)
    response = client.json.post(
        reverse('resources-api:financial-resources-list'),
        {'service': service.id, 'resource_type': metrika_resource_type.id, 'obj_id': 123}
    )
    assert response.status_code == 403

    role = factories.RoleFactory(code=Role.SERVICE_FINANCIAL_RESOURCES_REQUESTER)
    factories.ServiceMemberFactory(service=service, staff=staff, role=role)
    response = client.json.post(
        reverse('resources-api:financial-resources-list'),
        {'service': service.id, 'resource_type': metrika_resource_type.id, 'obj_id': 123}
    )
    assert response.status_code == 201


def test_create_resource(client, metrika_resource_type, staff_factory):
    service = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('financial_resources_creator', staff)
    client.login(staff.login)

    response = client.json.post(
        reverse('resources-api:financial-resources-list'),
        {'service': service.id, 'resource_type': metrika_resource_type.id, 'obj_id': 123}
    )

    assert response.status_code == 201

    resource = Resource.objects.get()
    assert resource.type == metrika_resource_type
    assert resource.external_id == '123'

    service_resource = ServiceResource.objects.get()
    assert service_resource.resource == resource
    assert service_resource.service == service
    assert service_resource.state == ServiceResource.GRANTED
    assert service_resource.requester == staff


def test_consume_resource(client, metrika_resource_type, staff_factory):
    service = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('financial_resources_creator', staff)
    client.login(staff.login)
    resource = factories.ResourceFactory(type=metrika_resource_type, external_id=123)

    response = client.json.post(
        reverse('resources-api:financial-resources-list'),
        {'service': service.id, 'resource_type': metrika_resource_type.id, 'obj_id': 123}
    )

    assert response.status_code == 201

    assert Resource.objects.count() == 1

    service_resource = ServiceResource.objects.get()
    assert service_resource.resource == resource
    assert service_resource.service == service
    assert service_resource.state == ServiceResource.GRANTED


def test_restore_resource(client, metrika_resource_type, staff_factory):
    service = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('financial_resources_creator', staff)
    client.login(staff.login)
    resource = factories.ResourceFactory(type=metrika_resource_type, external_id=123)
    service_resource = factories.ServiceResourceFactory(resource=resource, service=service, state='deprived')

    response = client.json.post(
        reverse('resources-api:financial-resources-list'),
        {'service': service.id, 'resource_type': metrika_resource_type.id, 'obj_id': 123}
    )

    assert response.status_code == 201

    assert Resource.objects.count() == 1
    assert ServiceResource.objects.count() == 1

    service_resource.refresh_from_db()
    assert service_resource.state == ServiceResource.GRANTED


def test_edit_resource(client, metrika_resource_type, staff_factory):
    service1 = factories.ServiceFactory()
    service2 = factories.ServiceFactory()
    service3 = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('financial_resources_creator', staff)
    client.login(staff.login)
    resource = factories.ResourceFactory(type=metrika_resource_type, external_id=123, attributes={'x': 1})
    service1_resource = factories.ServiceResourceFactory(resource=resource, service=service1, state='deprived')
    service2_resource = factories.ServiceResourceFactory(resource=resource, service=service2, state='granted')

    response = client.json.post(
        reverse('resources-api:financial-resources-list'),
        {'service': service3.id, 'resource_type': metrika_resource_type.id, 'obj_id': 123, 'attributes': {'x': 2}}
    )

    assert response.status_code == 201

    assert Resource.objects.count() == 2
    assert ServiceResource.objects.count() == 4

    resource.refresh_from_db()
    assert resource.attributes == {'x': 1}

    new_resource = Resource.objects.exclude(pk=resource.pk).get()
    assert new_resource.obsolete == resource
    assert new_resource.external_id == resource.external_id == '123'
    assert new_resource.attributes == {'x': 2}

    service1_resource.refresh_from_db()
    assert service1_resource.state == ServiceResource.DEPRIVED
    assert service1_resource.resource == resource

    service2_resource.refresh_from_db()
    assert service2_resource.state == ServiceResource.OBSOLETE
    assert service2_resource.resource == resource

    new_service2_resource = ServiceResource.objects.exclude(pk=service2_resource.pk).get(service=service2)
    assert new_service2_resource.state == ServiceResource.GRANTING
    assert new_service2_resource.resource == new_resource
    assert new_service2_resource.obsolete == service2_resource

    service3_resource = ServiceResource.objects.get(service=service3)
    assert service3_resource.state == ServiceResource.GRANTED
    assert service3_resource.resource == new_resource


def test_deprive_financial_resource_connected_with_inactive_services(metrika_resource_type):
    active_service = factories.ServiceFactory()
    deleted_service = factories.ServiceFactory(state='deleted')

    other_resource_type = factories.ResourceTypeFactory()
    other_resource = factories.ResourceFactory(type=other_resource_type)
    resource = factories.ResourceFactory(type=metrika_resource_type)

    active_service_resource = factories.ServiceResourceFactory(
        service=active_service, resource=resource, state=ServiceResource.GRANTED
    )
    deleted_service_resource = factories.ServiceResourceFactory(
        service=deleted_service, resource=resource, state=ServiceResource.GRANTED
    )
    requested_service_resource = factories.ServiceResourceFactory(
        service=deleted_service, resource=resource, state=ServiceResource.REQUESTED
    )
    deleted_service_other_resource = factories.ServiceResourceFactory(
        service=deleted_service, resource=other_resource, state=ServiceResource.GRANTED
    )

    deprive_financial_resource_connected_with_inactive_services()

    for x in (active_service_resource, deleted_service_resource, deleted_service_other_resource, requested_service_resource):
        x.refresh_from_db()

    assert active_service_resource.state == ServiceResource.GRANTED
    assert deleted_service_resource.state == ServiceResource.DEPRIVED
    assert deleted_service_other_resource.state == ServiceResource.GRANTED
    assert requested_service_resource.state == ServiceResource.DEPRIVED


def test_create_balance_attrs_client_id(client, staff_factory):
    service = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('financial_resources_creator', staff)
    client.login(staff.login)

    resource_type = factories.ResourceTypeFactory(
        code='balance_client', has_multiple_consumers=True, has_automated_grant=True,
    )

    response = client.json.post(
        reverse('resources-api:financial-resources-list'),
        {
            'service': service.id,
            'resource_type': resource_type.id,
            'obj_id': 12345,
            'attributes': {
                'client_id': 12345,
                'main_manager': '',
                'main_manager_passport_login': '',
            }
        }
    )

    assert response.status_code == 201

    resource = Resource.objects.get(type=resource_type.pk)
    # client_id должен быть строкой
    assert resource.attributes['client_id'] == '12345'
    assert resource.external_id == '12345'


@pytest.mark.parametrize('with_manager', [True, False])
def test_direct_financial(request, client, owner_role, staff_factory, with_manager):
    staff = staff_factory()
    assign_perms_for_internal_role('financial_resources_creator', staff)
    client.login(staff.login)

    service = factories.ServiceFactory(owner=staff)
    resource_type = factories.ResourceTypeFactory(code='direct_client')
    factories.ServiceMemberFactory(service=service, role=owner_role, staff=staff)
    if with_manager:
        manager = 'manager'
        main_manager_passport_login = 'yndx-manager'
    else:
        manager = None
        main_manager_passport_login = None

    attributes = {
        'client_id': 12345,
        'main_manager': manager,
        'main_manager_passport_login': main_manager_passport_login,
    }

    with patch('plan.resources.tasks._send_resource_to_supplier'):
        request.return_value = Response(200, '{"status": "ok", "client_id": 1}')

        response = client.json.post(
            reverse('resources-api:financial-resources-list'),
            {
                'service': service.id,
                'resource_type': resource_type.id,
                'obj_id': 12345,
                'attributes': attributes,
            }
        )
    assert response.status_code == 201

    sr = ServiceResource.objects.get(type=resource_type)
    expected_value = '12345' if with_manager else '12345-nomanager'
    assert sr.resource.name == expected_value
    assert sr.resource.external_id == expected_value
    assert sr.resource.attributes['client_id'] == '12345'
    assert sr.resource.attributes['main_manager'] == manager
    assert sr.resource.attributes['main_manager_passport_login'] == main_manager_passport_login

from textwrap import dedent


import pretend
import pytest
from django.core.urlresolvers import reverse
from django.conf import settings
from mock import patch

from plan.resources.suppliers.base import SupplierPlugin
from plan.common.person import Person
from plan.resources.api.base import make_signature
from plan.resources.models import ServiceResource
from common import factories
from unit.resources.test_request import fake_form_metadata

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, owner_role, staff_factory):
    staff = staff_factory('full_access')
    manager = factories.StaffFactory(user=factories.UserFactory(username='cool-manager'))
    parent = factories.ServiceFactory()
    service = factories.ServiceFactory(owner=staff, parent=parent)
    factories.ServiceMemberFactory(service=service, role=owner_role, staff=staff)
    resource_type = factories.ResourceTypeFactory(
        code='billing_point',
        supplier_plugin='billing_point',
        has_automated_grant=True,
        form_id=1,
        form_handler=dedent('''
        from plan.resources.handlers.billing_point.forward import process_form_forward
        result = process_form_forward(data, form_metadata, cleaned_data)
        '''),
        form_back_handler=dedent('''
        from plan.resources.handlers.billing_point.backward import process_form_backward
        result = process_form_backward(attributes, form_metadata)
        '''),
    )
    resource_type.consumer_roles.add(owner_role)
    fixture = pretend.stub(
        resource_type=resource_type,
        service=service,
        staff=staff,
        manager=manager,
    )
    return fixture


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
@pytest.mark.usefixtures('robot')
@pytest.mark.parametrize('oebs_related', (True, False))
def test_billing_point(client, data, oebs_related):
    client.login(data.staff.user.username)
    tag = factories.ServiceTagFactory(slug=settings.OEBS_BILLING_AGGREGATION_TAG)
    signature = make_signature(
        service=data.service,
        resource_type=data.resource_type,
        user=data.staff.user
    )
    if oebs_related:
        resource_type = factories.ResourceTypeFactory(code=settings.OEBS_PRODUCT_RESOURCE_TYPE_CODE)
        factories.ServiceResourceFactory(
            service=data.service,
            resource=factories.ResourceFactory(type=resource_type),
            state='granted',
        )

    form_data = {
        'field_1': '{"question": {"id": 1, "slug": "upload_choice"}, "value": "yt"}',
    }

    response = client.post(
        reverse('resources-api:request-list'),
        form_data,
        **{
            'HTTP_X_FORM_ANSWER_ID': 1,
            'HTTP_X_ABC_USER': data.staff.login,
            'HTTP_X_ABC_SERVICE': data.service.pk,
            'HTTP_X_ABC_RESOURCE_TYPE': data.resource_type.pk,
            'HTTP_X_ABC_SIGNATURE': signature,
        }
    )
    if oebs_related:
        assert response.status_code == 200, response.content

        sr = ServiceResource.objects.get(type=data.resource_type)
        assert sr.resource.name == 'Billing aggregation point'
        assert sr.service.tags.filter(slug=tag.slug).exists()
        assert sr.resource.attributes['upload_choice'] == 'yt'

        plugin = SupplierPlugin.get_plugin_class(data.resource_type.supplier_plugin)()
        person = Person(data.staff)
        assert plugin.can_delete_resource(person, sr) is False
        sr.service.parent.tags.add(tag)
        assert plugin.can_delete_resource(person, sr) is True
    else:
        assert response.status_code == 400, response.content
        assert response.json()['error']['extra']['en'] == 'The service is not synchronized with OEBS. It cannot be marked as billing aggregation point'
        assert not ServiceResource.objects.filter(type=data.resource_type).count()


def test_deprive_billing_point():
    tag = factories.ServiceTagFactory(slug=settings.OEBS_BILLING_AGGREGATION_TAG)
    service = factories.ServiceFactory()
    service.tags.add(tag)
    resource_type = factories.ResourceTypeFactory(
        code='billing_point', supplier_plugin='billing_point'
    )
    resource = factories.ResourceFactory(
        type=resource_type,
        attributes={'upload_choice': 'yt', 'balance_id': None}
    )
    sr1 = factories.ServiceResourceFactory(resource=resource, service=service, state=ServiceResource.GRANTED)

    plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
    plugin.delete(sr1, None)

    assert not service.tags.count()

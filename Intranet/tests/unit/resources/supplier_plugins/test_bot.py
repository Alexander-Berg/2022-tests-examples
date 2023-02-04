import pytest
from django.core.urlresolvers import reverse
from django.utils import timezone
from freezegun import freeze_time
from mock import patch

from common import factories
from utils import Response

pytestmark = [pytest.mark.django_db]


@pytest.mark.usefixtures('robot')
@freeze_time('2019-01-01T00:00:00')
def test_change_service(client, owner_role, staff_factory):
    staff = staff_factory()
    service = factories.ServiceFactory()
    factories.ServiceMemberFactory(staff=staff, service=service, role=owner_role)
    another_service = factories.ServiceFactory()
    client.login(staff.login)

    resource_type = factories.ResourceTypeFactory(supplier_plugin='bot')
    resource_type.consumer_roles.add(owner_role)
    resource = factories.ResourceFactory(type=resource_type)
    service_resource = factories.ServiceResourceFactory(service=service, resource=resource, state='granted')

    with patch('plan.common.utils.http.Session.post') as post:
        post.return_value = Response(200, {})
        response = client.json.post(
            reverse('resources-api:serviceresources-actions', args=[service_resource.id]),
            {'action': 'change_service', 'data': {'service': another_service.id, 'reason': 'причина'}}
        )
    assert response.status_code == 200
    service_resource.refresh_from_db()
    assert service_resource.state == 'deprived'
    assert service_resource.attributes == {'deprive_reason': 'причина'}
    assert service_resource.depriver == staff
    assert service_resource.deprived_at == timezone.now()

import pytest

from django.core.urlresolvers import reverse
from django.utils import timezone

from plan.resources.models import ServiceResource
from common import factories

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize(
    'hanged, staff_role', (
        (True, 'own_only_viewer'),
        (False, 'own_only_viewer'),
    )
)
def test_resources_granting_too_long(client, hanged, staff_role, staff_factory):
    now = timezone.now()
    threshold = timezone.timedelta(days=2)
    approved_at = now - (threshold + timezone.timedelta(minutes=1)) if hanged else now
    resource_type = factories.ResourceTypeFactory(max_granting_time=threshold)
    resource = factories.ResourceFactory(type=resource_type)
    factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        approved_at=approved_at,
        resource=resource,
    )
    factories.ServiceResourceFactory(
        state=ServiceResource.DEPRIVED,
        approved_at=approved_at,
        resource=resource,
    )
    factories.ServiceResourceFactory(
        state=ServiceResource.APPROVED,
        approved_at=approved_at,
        resource=resource,
    )
    factories.ServiceResourceFactory(
        state=ServiceResource.GRANTING,
        approved_at=approved_at,
        resource=resource,
    )
    client.login(staff_factory(staff_role).login)
    response = client.get(reverse('monitorings:granting-resources'))
    if hanged:
        assert response.status_code == 400
        assert response.content == b'Resources hanging after approving: 2'
    else:
        assert response.status_code == 200
        assert response.content == b'ok'

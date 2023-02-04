import pretend
import pytest
from mock import patch
from django.utils import timezone

from plan.resources import tasks, constants
from plan.resources.models import ServiceResource
from common import factories
from plan.common.utils.dates import datetime_to_str


pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db):
    conductor = factories.ServiceFactory(slug=constants.CONDUCTOR_SUPPLIER_SERVICE_SLUG)
    service1 = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(
        supplier=conductor,
        import_plugin='conductor',
        import_link='http://yandex.ru/',
    )

    fixture = pretend.stub(
        resource_type=resource_type,
        service1=service1,
    )
    return fixture


@pytest.mark.usefixtures('robot')
@pytest.mark.parametrize('created_now', [True, False])
def test_remove_old_resource(data, created_now):
    if created_now:
        created_at = timezone.now()
    else:
        created_at = timezone.now() - timezone.timedelta(days=10)
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.service1,
        state='granted',
    )
    resource.created_at = created_at
    service_resource.created_at = created_at
    resource.save()
    service_resource.save()
    with patch('plan.resources.importers.conductor.get_services_projects') as get_services_projects:
        get_services_projects.return_value = {}

        tasks.sync_with_conductor(start_time=datetime_to_str(timezone.now()))

    service_resource.refresh_from_db()
    if created_now:
        # ресурс не был удален т.к. был создан менее чем за 5 минут до старта синхронизации.
        assert service_resource.state == 'granted'
    else:
        assert service_resource.state == 'deprived'


@pytest.mark.usefixtures('robot')
@pytest.mark.parametrize('state', [ServiceResource.GRANTING, ServiceResource.GRANTED])
def test_dont_deprive_on_granting(data, state):
    delta_time = constants.SERVICE_SYNC_TIME_LIMIT * 10
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
        created_at=timezone.now() - delta_time
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.service1,
        state=state,
        created_at=timezone.now() - delta_time
    )
    with patch('plan.resources.importers.conductor.get_services_projects') as get_services_projects:
        get_services_projects.return_value = {}

        tasks.sync_with_conductor()

    service_resource.refresh_from_db()
    if state == ServiceResource.GRANTING:
        assert service_resource.state == ServiceResource.GRANTING
    else:
        assert service_resource.state == ServiceResource.DEPRIVED

import pytest
import pretend

from plan.resources.models import Resource, ServiceResource
from plan.resources.constants import HR_RESOURCETYPE_CODE
from plan.resources.tasks import sync_with_oebs
from common import factories
from utils import vcr_test

BOT_SOURCE_SLUG = 'test_bot'

pytestmark = [
    pytest.mark.django_db,
    pytest.mark.usefixtures('robot')
]


@pytest.fixture
def data():
    hr_resource_type = factories.ResourceTypeFactory(
        code=HR_RESOURCETYPE_CODE,
        is_important=True,
    )

    mobile_bro = factories.ServiceFactory(
        id=100504,
        name='mobilebrowser',
        slug='mobilebrowser',
    )

    spdaemon = factories.ServiceFactory(
        id=2615,
        name='spdaemon',
        slug='spdaemon'
    )

    return pretend.stub(
        hr_resource_type=hr_resource_type,
        mobile_bro=mobile_bro,
        spdaemon=spdaemon
    )


def test_import_oebs(data):
    cassette_name = 'resources/service_mobilebrowser.json'
    with vcr_test().use_cassette(cassette_name):
        sync_with_oebs(data.mobile_bro.pk)

    resource = Resource.objects.get()
    assert resource.external_id == str(data.mobile_bro.pk)
    assert resource.name == str(data.mobile_bro.pk)
    assert resource.type == data.hr_resource_type
    service_resource = resource.serviceresource_set.get()
    assert service_resource.service == data.mobile_bro
    assert service_resource.state == ServiceResource.GRANTED

    with vcr_test().use_cassette(cassette_name):
        sync_with_oebs(data.mobile_bro.pk)

    service_resource.refresh_from_db()
    assert service_resource.state == ServiceResource.GRANTED


def test_sync_deprives_resource_when_hr_flag_is_removed(data):
    resource = factories.ResourceFactory(type=data.hr_resource_type)
    service_resource = factories.ServiceResourceFactory(service=data.spdaemon, resource=resource)

    cassette_name = 'resources/service_spdaemon.json'
    with vcr_test().use_cassette(cassette_name):
        sync_with_oebs(data.spdaemon.pk)

    service_resource.refresh_from_db()
    assert service_resource.state == ServiceResource.DEPRIVED

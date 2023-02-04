import pytest
import pretend
from mock import patch

from django.conf import settings

from plan.resources.tasks import sync_resource_type
from plan.resources.models import ServiceResource

from common import factories
from utils import vcr_test


@pytest.fixture
def arcadia_data(db):
    resource_type = factories.ResourceTypeFactory(
        code=settings.ARCADIA_RESOURCE_TYPE_CODE,
        import_plugin='arcadia',
        import_link='https://arcanum-test.yandex.net/api/v1/projects?fields=id,name,dirs',
    )

    service = factories.ServiceFactory(slug='abc')
    service_1 = factories.ServiceFactory(slug='abcd')

    sr = factories.ServiceResourceFactory(
        type=resource_type,
        state='granted',
        service=service_1,
        resource=factories.ResourceFactory(
            type=resource_type,
            name='ABCD',
            link='hello.test',
            external_id='abcd',
            attributes={'dirs': ['some/dir']}
        ),
    )

    return pretend.stub(
        resource_type=resource_type,
        service=service,
        service_1=service_1,
        service_resource=sr,
    )


def test_sync_with_arcaida(arcadia_data, patch_lock):
    cassette_name = 'resources/sync_with_arcadia.json'
    with vcr_test().use_cassette(cassette_name):
        with patch('plan.resources.importers.arcadia.get_tvm_ticket') as get_tvm_ticket:
            get_tvm_ticket.return_value = 'some_ticket'
            sync_resource_type(arcadia_data.resource_type.id)

    sr = ServiceResource.objects.alive().get(
        service=arcadia_data.service,
        type=arcadia_data.resource_type
    )

    assert sr.resource.attributes == {'dirs': ['intranet/plan', 'frontend/services/abc']}
    assert sr.resource.name == 'ABC'
    assert sr.resource.external_id == 'abc'
    assert sr.resource.link == 'https://arcanum-test.yandex.net/projects/abc/'

    existing_sr = arcadia_data.service_resource
    existing_sr.refresh_from_db()
    existing_sr.resource.refresh_from_db()

    assert existing_sr.state == ServiceResource.GRANTED

    assert existing_sr.resource.link == 'https://arcanum-test.yandex.net/projects/abcd/'
    assert existing_sr.resource.attributes == {'dirs': ['intranet/abcd']}

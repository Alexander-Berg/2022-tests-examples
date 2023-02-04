import pretend
import pytest

import datetime
from django.utils import timezone

from plan.resources.importers import base
from plan.resources.importers.crt import CrtPlugin
from plan.resources.models import ServiceResource, Resource
from plan.resources.tasks import sync_resource_type, sync_certificates_incremental
from common import factories
from utils import vcr_test

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db):
    service1 = factories.ServiceFactory(id=989, slug='abc')
    service2 = factories.ServiceFactory(id=990, slug='abcd')
    resource_type = factories.ResourceTypeFactory(
        import_plugin='crt',
        import_link='http://yandex.ru/',
        supplier=service1,
        code='cert'
    )
    resource_type2 = factories.ResourceTypeFactory(
        supplier=service2
    )
    fixture = pretend.stub(
        resource_type=resource_type,
        resource_type2=resource_type2,
        service1=service1,
        service2=service2,
    )
    return fixture


def test_get_plugin(data):
    plugin_class = base.Plugin.get_plugin_class(data.resource_type.import_plugin)
    assert plugin_class == CrtPlugin


def test_fetch_resources(data):
    plugin = CrtPlugin(resource_type=data.resource_type)

    cassette_name = 'resources/crt_importer.json'
    with vcr_test().use_cassette(cassette_name):
        result = plugin.fetch()

    assert len(result) == 2

    plugin_data = {rec['service'].slug: rec for rec in result}

    assert len(plugin_data['abc']['resources']) == 3
    assert plugin_data['abc']['resources'][0]['name'] == 'abc-pr1234.test.yandex-team.ru'
    assert plugin_data['abc']['resources'][0]['id'] == '2874A77F00020006EC35'
    assert plugin_data['abc']['resources'][0]['url'] == 'https://crt-api.yandex-team.ru/api/v2/certificate/772734/'
    assert plugin_data['abc']['resources'][0]['attributes']['type'] == 'host'
    assert plugin_data['abc']['resources'][0]['attributes']['username'] == 'robot-qloud-client'
    assert plugin_data['abc']['resources'][0]['attributes']['requester'] == 'robot-qloud-client'

    assert len(plugin_data['abcd']['resources']) == 1
    assert plugin_data['abcd']['resources'][0]['name'] == 'abc-pr-535.test.tools.yandex-team.ru'
    assert plugin_data['abcd']['resources'][0]['id'] == '46C60BBC00020006F990'
    assert plugin_data['abcd']['resources'][0]['url'] == 'https://crt-api.yandex-team.ru/api/v2/certificate/781458/'
    assert plugin_data['abcd']['resources'][0]['attributes']['type'] == 'host'
    assert plugin_data['abcd']['resources'][0]['attributes']['username'] == 'robot-qloud-client'
    assert plugin_data['abcd']['resources'][0]['attributes']['requester'] == 'robot-qloud-client'


def test_sync_resource_type(data, patch_lock):
    resource = factories.ResourceFactory(type=data.resource_type, external_id='46C60BBC00020006F990')
    old_service1_resource = factories.ServiceResourceFactory(
        service=data.service1,
        resource=resource,
        state=ServiceResource.GRANTED
    )

    resource2 = factories.ResourceFactory(type=data.resource_type)
    old_other_resource = factories.ServiceResourceFactory(resource=resource2, state=ServiceResource.GRANTED)

    cassette_name = 'resources/crt_importer.json'
    with vcr_test().use_cassette(cassette_name):
        sync_resource_type(data.resource_type.id)

    assert Resource.objects.count() == 5
    assert ServiceResource.objects.count() == 6

    old_service1_resource.refresh_from_db()
    assert old_service1_resource.state == ServiceResource.DEPRIVED

    old_other_resource.refresh_from_db()
    assert old_other_resource.state == ServiceResource.DEPRIVED

    assert ServiceResource.objects.filter(
        service=data.service1,
        resource__external_id='2874A77F00020006EC35',
        state=ServiceResource.GRANTED
    ).exists()
    assert ServiceResource.objects.filter(
        service=data.service2,
        resource__external_id='46C60BBC00020006F990',
        state=ServiceResource.GRANTED
    ).exists()


def test_fetch_incremental(data):
    plugin = CrtPlugin(resource_type=data.resource_type)

    cassette_name = 'resources/crt_importer_incremental.json'
    with vcr_test().use_cassette(cassette_name):
        result = plugin.fetch_incremental(since=datetime.datetime(2021, 1, 1))

    assert len(result) == 2
    plugin_data = {rec['service'].slug: rec for rec in result}
    assert len(plugin_data['abc']['resources']) == 3

    assert plugin_data['abc']['resources'][0]['name'] == 'abc-pr1234.test.yandex-team.ru'
    assert plugin_data['abc']['resources'][0]['id'] == '2874A77F00020006EC35'
    assert plugin_data['abc']['resources'][0]['url'] == 'https://crt-api.yandex-team.ru/api/v2/certificate/772734/'
    assert plugin_data['abc']['resources'][0]['attributes']['status'] == 'issued'
    assert plugin_data['abc']['resources'][0]['attributes']['type'] == 'host'
    assert plugin_data['abc']['resources'][0]['attributes']['username'] == 'robot-qloud-client'
    assert plugin_data['abc']['resources'][0]['attributes']['requester'] == 'robot-qloud-client'

    assert len(plugin_data['abcd']['resources']) == 1
    assert plugin_data['abcd']['resources'][0]['name'] == 'abc-pr-535.test.tools.yandex-team.ru'
    assert plugin_data['abcd']['resources'][0]['id'] == '46C60BBC00020006F990'
    assert plugin_data['abcd']['resources'][0]['url'] == 'https://crt-api.yandex-team.ru/api/v2/certificate/781458/'
    assert plugin_data['abcd']['resources'][0]['attributes']['status'] == 'issued'
    assert plugin_data['abcd']['resources'][0]['attributes']['type'] == 'host'
    assert plugin_data['abcd']['resources'][0]['attributes']['username'] == 'robot-qloud-client'
    assert plugin_data['abcd']['resources'][0]['attributes']['requester'] == 'robot-qloud-client'


@pytest.mark.parametrize('since', [None, '2021-01-01T00:00:00'])
def test_sync_incremental(data, patch_lock, since):
    factories.TaskMetricFactory(
        task_name='sync_certificates_incremental',
        last_success_start=timezone.make_aware(datetime.datetime(2021, 1, 1, 3, 10)),
    )
    transfer = factories.ServiceResourceFactory(
        service=data.service1,
        state=ServiceResource.GRANTED,
        resource=factories.ResourceFactory(type=data.resource_type, external_id='287D99F800020006EC3D')
    )
    deprive = factories.ServiceResourceFactory(
        service=data.service1,
        state=ServiceResource.GRANTED,
        resource=factories.ResourceFactory(type=data.resource_type, external_id='46C60BBC00020006F990'),
    )
    keep = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        resource=factories.ResourceFactory(type=data.resource_type),
    )

    cassette_name = 'resources/crt_importer_incremental.json'
    with vcr_test().use_cassette(cassette_name):
        sync_certificates_incremental(since)

    assert Resource.objects.count() == 5
    assert ServiceResource.objects.count() == 6
    transfer.refresh_from_db()
    assert transfer.state == ServiceResource.DEPRIVED
    deprive.refresh_from_db()
    assert deprive.state == ServiceResource.DEPRIVED
    keep.refresh_from_db()
    assert keep.state == ServiceResource.GRANTED

    granted_id_list = ServiceResource.objects.filter(
        service=data.service1,
        state=ServiceResource.GRANTED
    ).values_list('resource__external_id', flat=True)
    assert list(granted_id_list) == ['2874A77F00020006EC35', '2876E46F00020006EC3A']

    assert ServiceResource.objects.get(
        service=data.service2,
        state=ServiceResource.GRANTED,
    ).resource.external_id == '46C60BBC00020006F990'

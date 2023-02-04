import pytest
import pretend

from django.conf import settings

from plan.resources.tasks import sync_resource_type
from plan.resources.models import ServiceResource

from common import factories
from utils import vcr_test


@pytest.fixture
def warden_data(db):
    resource_type = factories.ResourceTypeFactory(
        code=settings.WARDEN_RESOURCE_TYPE_CODE,
        import_plugin='warden',
        import_link='https://warden.z.yandex-team.ru/api/warden.Warden/getComponentList',
    )

    service = factories.ServiceFactory(slug='abc')
    tag_b = factories.ServiceTagFactory(slug='tier_b')
    tag_c = factories.ServiceTagFactory(slug='tier_v')

    return pretend.stub(
        resource_type=resource_type,
        service=service,
        tag_b=tag_b,
        tag_c=tag_c,
    )


def test_sync_with_warden(warden_data, patch_lock):
    old_service = factories.ServiceFactory()
    other_tag = factories.ServiceTagFactory()
    old_service.tags.add(other_tag, warden_data.tag_b)
    warden_data.service.tags.add(other_tag, warden_data.tag_c)

    old_sr = factories.ServiceResourceFactory(
        type=warden_data.resource_type,
        service=old_service,
        state='granted',
        resource=factories.ResourceFactory(
            type=warden_data.resource_type,
            external_id='123'
        ),
    )
    cassette_name = 'resources/sync_with_warden.json'
    with vcr_test().use_cassette(cassette_name):
        sync_resource_type(warden_data.resource_type.id)

    sr = ServiceResource.objects.alive().get(
        service=warden_data.service,
        type=warden_data.resource_type
    )

    assert sr.resource.attributes == {
        'goalUrl': 'https://goals.yandex-team.ru/filter?goal=106971',
        'humanReadableName': 'ABC',
        'infraPreset': 'Zft6tfk3eXT',
        'spiChat': {
            'chatLink': 'https://t.me/joinchat/BdtakRQm0yD8dS3x0BGCcQ',
            'name': 'SpiCoordinationChat'},
        'state': 'INVALID',
        'tier': 'B',
    }
    assert sr.resource.name == 'ABC'
    assert sr.resource.external_id == 'abcd/abc'
    assert sr.resource.link == 'https://warden.z.yandex-team.ru/components/abcd/s/abc/'
    old_sr.refresh_from_db()

    assert old_sr.state == ServiceResource.DEPRIVED

    assert old_service.tags.count() == 1
    assert old_service.tags.get() == other_tag
    assert warden_data.service.tags.count() == 2
    assert warden_data.service.tags.filter(slug='tier_b').exists()
    assert not warden_data.service.tags.filter(slug='tier_v').exists()


def test_sync_with_warden_change_attrs(warden_data, patch_lock):
    sr = factories.ServiceResourceFactory(
        type=warden_data.resource_type,
        state='granted',
        service=warden_data.service,
        resource=factories.ResourceFactory(
            type=warden_data.resource_type,
            name='smth',
            link='hello.test',
            external_id='abcd/abc',
            attributes={'infraPreset': 'test'}
        ),
    )
    cassette_name = 'resources/sync_with_warden.json'
    with vcr_test().use_cassette(cassette_name):
        sync_resource_type(warden_data.resource_type.id)

    sr.resource.refresh_from_db()
    assert sr.resource.attributes['infraPreset'] == 'Zft6tfk3eXT'
    assert sr.resource.attributes['goalUrl'] == 'https://goals.yandex-team.ru/filter?goal=106971'

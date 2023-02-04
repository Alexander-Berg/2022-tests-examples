from unittest import mock

import pytest

from django.conf import settings
from waffle.testutils import override_switch
from plan.common.person import Person

from plan.api.exceptions import BadRequest

from plan.resources.models import Resource, ServiceResource

from common import factories

from yp.client import YpClient

pytestmark = [pytest.mark.django_db]


class MockYpClient(object):
    def __init__(self):
        pass

    def select_objects(
        self,
        object_type,
        filter=None,
        selectors=None,
        timestamp=None,
        offset=None,
        limit=None,
        options=None,
        enable_structured_response=False,
        batching_options=None,
    ):
        objects = {
            '[/meta/id] = "abc:service:1"': {
                "resource_limits": {
                    "memory": 100500,
                    "cpu": 100500,
                },
                "immediate_resource_usage": {
                    "memory": 10000,
                    "cpu": 10000,
                }
            },
            '[/meta/id] = "abc:service:3"': {
                "resource_limits": {
                    "memory": 0.5678,
                    "cpu": 100.1234,
                },
                "immediate_resource_usage": {
                    "memory": 0.05,
                    "cpu": 0.0001,
                }
            },
            '[/meta/id] = "abc:service:5"': {
                "resource_limits": {
                    "memory": 0.5678,
                    "cpu": 100.1234,
                },
                "immediate_resource_usage": {
                    "memory": 0.5,
                    "cpu": 1,
                }
            },
            '[/meta/id] = "abc:service:7"': {
                "resource_limits": {
                    "memory": 100500,
                    "cpu": 100500,
                },
                "immediate_resource_usage": {
                    "memory": 0.5,
                    "cpu": 1,
                }
            },
            '[/meta/id] = "abc:service:9"': {
                "resource_limits": {
                    "memory": 100500,
                    "cpu": 100500,
                },
                "immediate_resource_usage": {
                    "memory": 0.5,
                    "cpu": 1,
                }
            },
            '[/meta/id] = "abc:service:11"': {
                "resource_limits": {
                    "memory": 100500,
                    "cpu": 100500,
                },
                "immediate_resource_usage": {
                    "memory": 0.5,
                    "cpu": 1,
                }
            },
            '[/meta/id] = "abc:service:13"': {
                "resource_limits": {
                    "memory": 100,
                    "cpu": 100,
                },
                "immediate_resource_usage": {
                    "memory": 0.5,
                    "cpu": 1,
                }
            },
            '[/meta/id] = "abc:service:15"': {
                "resource_limits": {
                    "memory": 4,
                    "cpu": 11,
                },
                "immediate_resource_usage": {
                    "memory": 0.5,
                    "cpu": 1,
                }
            },
        }
        object_params = objects[filter]
        return [[
            {
                'parent_id': '',
                'resource_limits': {'per_segment': {
                    "common": {
                        'memory': {'capacity': object_params['resource_limits']['memory']},
                        'cpu': {'capacity': object_params['resource_limits']['cpu']},
                        'disk_per_storage_class': {
                            'ssd': {'capacity': 1656461511884, 'bandwidth': 157286400},
                            'hdd': {'capacity': 2929167695872, 'bandwidth': 10000},
                        },
                        'internet_address': {'capacity': 0},
                    },
                }}
            },
            {
                'resource_usage': {},
                'immediate_resource_usage': {
                    'per_segment': {
                        'common': {
                            'memory': {'capacity': object_params['immediate_resource_usage']['memory']},
                            'cpu': {'capacity': object_params['immediate_resource_usage']['cpu']},
                            'disk_per_storage_class': {
                                'ssd': {'capacity': 1656461511, 'bandwidth': 157286},
                                'hdd': {'capacity': 292916769587246666666, 'bandwidth': 10000},
                            },
                            'internet_address': {'capacity': 0}, 'network': {'bandwidth': 0},
                        },
                    }
                }
            }
        ]]


@pytest.fixture
def yp_resource_type():
    return factories.ResourceTypeFactory(code=settings.YP_RESOURCE_TYPE_CODE, has_automated_grant=True)


def create_service_resource(resource_type, service, attributes=None, state=ServiceResource.REQUESTED):
    attributes = attributes or {}
    resource = factories.ResourceFactory(type=resource_type, attributes=attributes)
    service_resource = factories.ServiceResourceFactory(resource=resource, service=service, state=state)
    return service_resource


def test_resource_approve_calls_process_quota_donation_for_yp_resources_with_donor_slug(yp_resource_type, robot):
    service = factories.ServiceFactory()
    yp_quota = create_service_resource(yp_resource_type, service, {'donor_slug': 'slug'}, state=ServiceResource.APPROVED)

    with mock.patch('plan.resources.models.ServiceResource._process_quota_donation') as m:
        yp_quota.grant(Person(robot))

    assert m.call_args_list == [mock.call()]


def test_resource_approve_doesnt_call_process_quota_donation_yp_resources_without_donor_slug(yp_resource_type, robot):
    service = factories.ServiceFactory()
    yp_quota = create_service_resource(yp_resource_type, service, state=ServiceResource.APPROVED)

    with mock.patch('plan.resources.models.ServiceResource._process_quota_donation') as m:
        yp_quota.grant(Person(robot))

    assert m.call_args_list == []


def test_resource_approve_doesnt_call_process_quota_donation_for_other_resources(robot):
    service = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(has_automated_grant=True)
    service_resource = create_service_resource(resource_type, service, {'donor_slug': 'slug'}, state=ServiceResource.APPROVED)

    with mock.patch('plan.resources.models.ServiceResource._process_quota_donation') as m:
        service_resource.grant(Person(robot))

    assert m.call_args_list == []


def test_cant_donate_to_the_same_service(yp_resource_type, robot):
    service = factories.ServiceFactory()
    service_resource = create_service_resource(yp_resource_type, service, {'donor_slug': service.slug})
    with pytest.raises(BadRequest):
        service_resource.grant(Person(robot))


def test_process_quota_donation_one_big_resource(yp_resource_type, robot):
    service = factories.ServiceFactory(id=1)
    granted_quota = create_service_resource(
        yp_resource_type,
        service,
        attributes={
            'cpu': '100500',
            'memory': 100500,  # intentionally left int
            'gpu_qty': '100',
            'gpu_model': '1',
            'location': 'vla',
            'segment': 'common'
        },
        state=ServiceResource.GRANTED,
    )
    new_service = factories.ServiceFactory(id=2)
    new_quota = create_service_resource(
        yp_resource_type,
        new_service,
        attributes={
            'cpu': '500',
            'memory': '1000',
            'gpu_qty': '10',
            'gpu_model': '1',
            'location': 'vla',
            'segment': 'common',
            'donor_slug': service.slug
        },
        state=ServiceResource.APPROVED,
    )
    mock_yp_client = MockYpClient()
    with mock.patch('plan.resources.models.create_issue'):
        with mock.patch.object(YpClient, "select_objects", mock_yp_client.select_objects):
            new_quota.grant(Person(robot))
    assert new_quota.state == ServiceResource.GRANTING

    granted_quota = ServiceResource.objects.get(pk=granted_quota.pk)
    assert granted_quota.state == ServiceResource.OBSOLETE
    assert granted_quota.resource.attributes['donated_to'] == [new_quota.pk]
    new_resource = Resource.objects.get(obsolete=granted_quota.resource)
    assert new_resource.attributes == {
        'cpu': '100000',
        'memory': '99500',
        'gpu_qty': '90',
        'gpu_model': '1',
        'location': 'vla',
        'segment': 'common',
    }
    new_service_resource = ServiceResource.objects.get(resource=new_resource)
    assert new_service_resource.state == ServiceResource.GRANTED


def test_process_quota_donation_old_resource(yp_resource_type, robot):
    """Протестируем, что при отсутствии каких-то полей они дописываются в имя ресурса с 0, а не None"""

    service = factories.ServiceFactory(id=1)
    granted_quota = create_service_resource(
        yp_resource_type,
        service,
        attributes={
            'cpu': '100500',
            'memory': 100500,  # intentionally left int
            'location': 'vla',
            'segment': 'common'
        },
        state=ServiceResource.GRANTED,
    )
    new_service = factories.ServiceFactory(id=2)
    new_quota = create_service_resource(
        yp_resource_type,
        new_service,
        attributes={
            'cpu': '500',
            'memory': '1000',
            'location': 'vla',
            'segment': 'common',
            'donor_slug': service.slug
        },
        state=ServiceResource.APPROVED,
    )
    mock_yp_client = MockYpClient()
    with mock.patch('plan.resources.models.create_issue'):
        with mock.patch.object(YpClient, "select_objects", mock_yp_client.select_objects):
            new_quota.grant(Person(robot))
    assert new_quota.state == ServiceResource.GRANTING

    granted_quota = ServiceResource.objects.get(pk=granted_quota.pk)
    assert granted_quota.state == ServiceResource.OBSOLETE
    new_resource = granted_quota.resource.obsolete_resource.get()
    new_service_resource = new_resource.serviceresource_set.get()
    assert new_resource.name == (
        'loc:vla-seg:common-cpu:100000-mem:99500-hdd:0-ssd:0-ip4:0-net:0-io_ssd:0-io_hdd:0-gpu:0-gpu_q:0'
    )
    assert new_service_resource.state == ServiceResource.GRANTED


def test_process_quota_donation_decimal(yp_resource_type, robot):
    service = factories.ServiceFactory(id=3)
    granted_quota = create_service_resource(
        yp_resource_type,
        service,
        attributes={
            'cpu': '100.1234',
            'memory': '0.5678',
            'location': 'vla',
            'segment': 'common'
        },
        state=ServiceResource.GRANTED,
    )
    new_service = factories.ServiceFactory(id=4)
    new_quota = create_service_resource(
        yp_resource_type,
        new_service,
        attributes={
            'cpu': '100.1233',
            'memory': '0.5',
            'location': 'vla',
            'segment': 'common',
            'donor_slug': service.slug
        },
        state=ServiceResource.APPROVED,
    )
    mock_yp_client = MockYpClient()
    with mock.patch('plan.resources.models.create_issue'):
        with mock.patch.object(YpClient, "select_objects", mock_yp_client.select_objects):
            new_quota.grant(Person(robot))
    assert new_quota.state == ServiceResource.GRANTING

    granted_quota = ServiceResource.objects.get(pk=granted_quota.pk)
    assert granted_quota.state == ServiceResource.OBSOLETE
    assert granted_quota.resource.attributes['donated_to'] == [new_quota.pk]
    new_resource = Resource.objects.get(obsolete=granted_quota.resource)
    assert new_resource.attributes == {
        'cpu': '0',
        'memory': '0.0678',
        'location': 'vla',
        'segment': 'common',
    }
    new_service_resource = ServiceResource.objects.get(resource=new_resource)
    assert new_service_resource.state == ServiceResource.GRANTED


@override_switch('get_free_quota_from_yp', active=True)
def test_process_quota_donation_not_enough_decimal(yp_resource_type, robot):
    service = factories.ServiceFactory(id=5)
    create_service_resource(
        yp_resource_type,
        service,
        attributes={
            'cpu': '100.1234',
            'memory': '0.5678',
            'io_hdd': '0',
            'location': 'vla',
            'segment': 'common'
        },
        state=ServiceResource.GRANTED,
    )
    new_service = factories.ServiceFactory(id=6)
    new_quota = create_service_resource(
        yp_resource_type,
        new_service,
        attributes={
            'cpu': '100.1233',
            'memory': '0.5',
            'io_hdd': '0',
            'location': 'vla',
            'segment': 'common',
            'donor_slug': service.slug
        },
        state=ServiceResource.APPROVED,
    )
    mock_yp_client = MockYpClient()
    with pytest.raises(BadRequest) as exc_info:
        with mock.patch.object(YpClient, 'select_objects', mock_yp_client.select_objects):
            new_quota.grant(Person(robot))
    expected = {
        'ru': (
            'У сервиса-донора не хватает квоты в связи с аллокацией. '
            'Доступно с учётом аллокации: 100.1224 cpu, запрошено 100.1233 cpu'
        ),
        'en': (
            'Donor service does not have enough quota due to its allocation. '
            'Available due to allocation: 100.1224 cpu, requested 100.1233 cpu'
        ),
    }
    assert exc_info.value.extra == expected


def test_process_quota_donation_doesnt_use_other_location(yp_resource_type, robot):
    service = factories.ServiceFactory(id=7)
    create_service_resource(
        yp_resource_type,
        service,
        attributes={
            'cpu': '100500',
            'memory': '100500',
            'location': 'vla',
            'segment': 'common'
        },
        state=ServiceResource.GRANTED,
    )
    new_service = factories.ServiceFactory(id=8)
    new_quota = create_service_resource(
        yp_resource_type,
        new_service,
        attributes={
            'cpu': '500',
            'memory': '1000',
            'location': 'myt',
            'segment': 'common',
            'donor_slug': service.slug
        },
        state=ServiceResource.APPROVED,
    )
    mock_yp_client = MockYpClient()
    with pytest.raises(BadRequest):
        with mock.patch.object(YpClient, "select_objects", mock_yp_client.select_objects):
            new_quota.grant(Person(robot))


def test_process_quota_donation_doesnt_use_other_segment(yp_resource_type, robot):
    service = factories.ServiceFactory(id=9)
    create_service_resource(
        yp_resource_type,
        service,
        attributes={
            'cpu': '100500',
            'memory': '100500',
            'location': 'vla',
            'segment': 'common'
        },
        state=ServiceResource.GRANTED,
    )
    new_service = factories.ServiceFactory(id=10)
    new_quota = create_service_resource(
        yp_resource_type,
        new_service,
        attributes={
            'cpu': '500',
            'memory': '1000',
            'location': 'vla',
            'segment': 'personal',
            'donor_slug': service.slug
        },
        state=ServiceResource.APPROVED,
    )
    mock_yp_client = MockYpClient()
    with pytest.raises(BadRequest):
        with mock.patch.object(YpClient, "select_objects", mock_yp_client.select_objects):
            new_quota.grant(Person(robot))


def test_process_quota_donation_doesnt_use_different_gpu_model(yp_resource_type, robot):
    service = factories.ServiceFactory(id=11)
    create_service_resource(
        yp_resource_type,
        service,
        attributes={
            'gpu_qty': '100500',
            'gpu_model': 'model_1',
            'location': 'vla',
            'segment': 'common'
        },
        state=ServiceResource.GRANTED,
    )
    new_service = factories.ServiceFactory(id=12)
    new_quota = create_service_resource(
        yp_resource_type,
        new_service,
        attributes={
            'gpu_qty': '1',
            'gpu_model': 'model_2',
            'location': 'vla',
            'segment': 'common',
            'donor_slug': service.slug
        },
        state=ServiceResource.APPROVED,
    )
    mock_yp_client = MockYpClient()
    with pytest.raises(BadRequest):
        with mock.patch.object(YpClient, "select_objects", mock_yp_client.select_objects):
            new_quota.grant(Person(robot))


def test_process_quota_donation_not_enough(yp_resource_type, robot):
    service = factories.ServiceFactory(id=13)
    create_service_resource(
        yp_resource_type,
        service,
        attributes={
            'cpu': '100',
            'memory': '100',
            'location': 'vla',
            'segment': 'common'
        },
        state=ServiceResource.GRANTED,
    )
    new_service = factories.ServiceFactory(id=14)
    new_quota = create_service_resource(
        yp_resource_type,
        new_service,
        attributes={
            'cpu': '500',
            'memory': '1000',
            'location': 'vla',
            'segment': 'common',
            'donor_slug': service.slug
        },
        state=ServiceResource.APPROVED,
    )
    mock_yp_client = MockYpClient()
    with pytest.raises(BadRequest):
        with mock.patch.object(YpClient, "select_objects", mock_yp_client.select_objects):
            new_quota.grant(Person(robot))


def test_process_quota_donation_a_lot_of_resources(yp_resource_type, robot):
    service = factories.ServiceFactory()
    location_kwargs = {'location': 'vla', 'segment': 'common'}
    granted_cpu_1 = create_service_resource(
        yp_resource_type,
        service,
        attributes={'cpu': 1.5, **location_kwargs},
        state=ServiceResource.GRANTED,
    )
    granted_cpu_2 = create_service_resource(
        yp_resource_type,
        service,
        attributes={'cpu': '2.5', **location_kwargs},
        state=ServiceResource.GRANTED,
    )
    granted_cpu_and_mem = create_service_resource(
        yp_resource_type,
        service,
        attributes={'cpu': 7, 'memory': 1, **location_kwargs},
        state=ServiceResource.GRANTED,
    )
    granted_mem = create_service_resource(
        yp_resource_type,
        service,
        attributes={'memory': 3, **location_kwargs},
        state=ServiceResource.GRANTED,
    )

    new_service = factories.ServiceFactory()
    new_quota = create_service_resource(
        yp_resource_type,
        new_service,
        attributes={'cpu': '5', 'memory': '2', 'donor_slug': service.slug, **location_kwargs},
        state=ServiceResource.APPROVED,
    )
    mock_yp_client = MockYpClient()
    with mock.patch('plan.resources.models.create_issue') as m:
        with mock.patch.object(YpClient, "select_objects", mock_yp_client.select_objects):
            new_quota.grant(Person(robot))

    assert new_quota.state == ServiceResource.GRANTING

    for x in (granted_cpu_1, granted_cpu_2, granted_cpu_and_mem, granted_mem):
        x.refresh_from_db()

    assert granted_cpu_1.state == ServiceResource.DEPRIVED
    assert granted_cpu_2.state == ServiceResource.DEPRIVED

    assert granted_cpu_and_mem.state == ServiceResource.OBSOLETE
    new_cpu_and_mem = Resource.objects.get(obsolete=granted_cpu_and_mem.resource)
    assert new_cpu_and_mem.attributes == {'cpu': '6', 'memory': '0', **location_kwargs}
    new_cpu_and_mem = ServiceResource.objects.get(resource=new_cpu_and_mem)
    assert new_cpu_and_mem.service == service
    assert new_cpu_and_mem.state == ServiceResource.GRANTED

    assert granted_mem.state == ServiceResource.OBSOLETE
    new_mem = Resource.objects.get(obsolete=granted_mem.resource)
    assert new_mem.attributes == {'memory': '2', **location_kwargs}
    new_mem = ServiceResource.objects.get(resource=new_mem)
    assert new_mem.service == service
    assert new_mem.state == ServiceResource.GRANTED

    expected_description = f'''**Из:** {service.get_link()}
**В:** {new_service.get_link()}
**Автор запроса:** https://staff.yandex-team.ru/

**Лог**
{service.get_link()}
%%
-- {granted_cpu_1.resource.name}
++{' '}

-- {granted_cpu_2.resource.name}
++{' '}

-- {granted_cpu_and_mem.resource.name}
++ {new_cpu_and_mem.resource.name}

-- {granted_mem.resource.name}
++ {new_mem.resource.name}
%%

{new_service.get_link()}
%%
++ {new_quota.resource.name}
%%'''

    assert m.call_args_list == [mock.call(
        queue='TEST',
        description=expected_description,
        summary=f'Перенос YP квоты в ABC {service.id}:{new_service.id}',
        tags=['yp_quota_move', f'yp_quota_move-{service.slug}', f'yp_quota_move-{new_service.slug}']
    )]

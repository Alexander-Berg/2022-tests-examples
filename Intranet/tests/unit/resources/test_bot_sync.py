import copy

import pytest

from django.utils import timezone

from plan.resources import constants
from plan.resources import models, tasks
from common import factories

BOT_SOURCE_SLUG = 'test_bot'

pytestmark = [
    pytest.mark.django_db,
    pytest.mark.usefixtures('robot')
]


def generate_bot_server(number, type):
    from plan.resources.importers.bot import BotServer
    return BotServer({
        'instance_number': '000{0}'.format(number),
        'segment4': 'BOT',
        'segment3': 'TYPE_{0}'.format(type),
        'segment2': 'SERVER',
        'segment1': 'NAME_{0}'.format(number),
        'fqdn': 'bot{0}.yandex.net'.format(number),
        'location': 'location{0}'.format(number),
    })


def sync_service_resource(source, service, bot_data):
    type_names = {data.type_name for data in bot_data}
    types = tasks.create_or_update_bot_types(source, {}, type_names)
    tasks.sync_service_resource(source, service, types, bot_data)


@pytest.fixture
def base_data():
    supplier_data = {
        'id': 99,
        'slug': BOT_SOURCE_SLUG,
        'name': 'Test Bot',
    }
    supplier = factories.ServiceFactory(**supplier_data)
    service1 = factories.ServiceFactory(id=1, name='service1')
    service2 = factories.ServiceFactory(id=2, name='service2')

    return {
        'supplier': supplier,
        'service1': service1,
        'service2': service2,
    }


@pytest.fixture
def base_resources(base_data):
    data = copy.copy(base_data)

    type1 = factories.ResourceTypeFactory(supplier=base_data['supplier'], name='BOT.TYPE_1')
    type2 = factories.ResourceTypeFactory(supplier=base_data['supplier'], name='BOT.TYPE_2')
    type3 = factories.ResourceTypeFactory(supplier=base_data['supplier'], name='BOT.TYPE_3')

    bot_server_types = [type1, type2, type2, type3, type3, type3]
    bot_data = [(generate_bot_server(number, type.id), type) for number, type in
                enumerate(bot_server_types)]

    data.update({
        'type1': type1,
        'type2': type2,
        'type3': type3,
    })

    for server, type in bot_data:
        resource = factories.ResourceFactory(
            type=type,
            external_id=server.external_id,
            name=server.name,
            attributes=server.attributes,
            link=None,
        )

        factories.ServiceResourceFactory(
            service=data['service1'],
            resource=resource,
            state='granted',
        )

        data['resource{0}'.format(resource.id)] = resource

    models.Resource.objects.update(created_at=timezone.now() - constants.SERVICE_SYNC_TIME_LIMIT)
    return data


def test_server_db_save(base_data):
    """Поля из BOT-ответа правильно пропадают в базу"""
    bot_server = generate_bot_server(1, 1)

    sync_service_resource(base_data['supplier'], base_data['service1'], [bot_server])

    resource = models.Resource.objects.get()

    assert resource.type.name == bot_server.type_name
    assert resource.type.supplier.id == base_data['supplier'].id
    assert resource.external_id == bot_server.external_id
    assert resource.name == bot_server.name
    assert resource.attributes == bot_server.attributes


def test_sync(base_data, django_assert_num_queries):
    """"Синхронизация на пустую базу"""
    bot_server_types = [1, 2, 2, 3, 3, 3]
    bot_data = [generate_bot_server(number, type) for number, type in
                enumerate(bot_server_types)]

    with django_assert_num_queries(24):
        sync_service_resource(base_data['supplier'], base_data['service1'], bot_data)

    resources_count = models.Resource.objects \
        .filter(serviceresource__service_id=base_data['service1'].id) \
        .count()
    assert resources_count == 6
    assert models.Resource._closure_model.objects.count() == 6

    type_count = models.Resource.objects \
        .filter(type__name=bot_data[0].type_name) \
        .count()
    assert type_count == 1

    type_count = models.Resource.objects \
        .filter(type__name=bot_data[1].type_name) \
        .count()
    assert type_count == 2

    type_count = models.Resource.objects \
        .filter(type__name=bot_data[3].type_name) \
        .count()
    assert type_count == 3


def test_sync_new_resources(base_resources):
    """Новые сервера корректно попадают в существующую базу"""
    bot_server_types = [1, 2, 2, 3, 3, 3, 1, 2, 3, 4]
    bot_data = [generate_bot_server(number, type) for number, type in
                enumerate(bot_server_types)]

    sync_service_resource(base_resources['supplier'],
                          base_resources['service1'],
                          bot_data)

    resources_count = models.Resource.objects \
        .filter(serviceresource__service_id=base_resources['service1'].id) \
        .count()
    assert resources_count == 10

    type_count = models.Resource.objects \
        .filter(type__name=bot_data[0].type_name) \
        .count()
    assert type_count == 2

    type_count = models.Resource.objects \
        .filter(type__name=bot_data[1].type_name) \
        .count()
    assert type_count == 3

    type_count = models.Resource.objects \
        .filter(type__name=bot_data[3].type_name) \
        .count()
    assert type_count == 4

    type_count = models.Resource.objects \
        .filter(type__name=bot_data[9].type_name) \
        .count()
    assert type_count == 1


@pytest.mark.usefixtures('robot')
def test_sync_delete(base_resources, django_assert_num_queries):
    """Если в BOT удалились ресурсы из сервиса, они должны остаться в базе, но
    при этом удалиться из связи с сервисом"""
    bot_server_types = [1, 2]
    bot_data = [generate_bot_server(number, type) for number, type in
                enumerate(bot_server_types)]

    with django_assert_num_queries(12):
        sync_service_resource(base_resources['supplier'],
                              base_resources['service1'],
                              bot_data)

    resources_count = models.Resource.objects.count()
    assert resources_count == 6

    resources_count = models.Resource.objects \
        .filter(serviceresource__service_id=base_resources['service1'].id,
                serviceresource__state__in=models.ServiceResource.ALIVE_STATES) \
        .count()
    assert resources_count == 2


def test_sync_existed_servers(base_resources):
    """При добавлении серверов в сервис не должно создаваться новых ресурсов,
    если подобные уже существуют"""
    bot_server_types = [1, 2, 2, 3, 3, 3]
    bot_data = [generate_bot_server(number, type) for number, type in
                enumerate(bot_server_types)]

    sync_service_resource(base_resources['supplier'],
                          base_resources['service2'],
                          bot_data)

    assert models.Resource.objects.count() == 6
    assert models.ServiceResource.objects.count() == 12

    resources_count = models.Resource.objects \
        .filter(serviceresource__service_id=base_resources['service1'].id) \
        .count()
    assert resources_count == 6

    resources_count = models.Resource.objects \
        .filter(serviceresource__service_id=base_resources['service2'].id) \
        .count()
    assert resources_count == 6


def test_sync_update(base_resources):
    """При изменении полей у конкретного сервиса данные в базе должны
    обновляться"""
    bot_server_types = [1, 2, 2, 3, 3, 3]
    bot_data = [generate_bot_server(number, type) for number, type in
                enumerate(bot_server_types)]

    bot_data[2]['segment2'] = 'TEST'
    bot_data[2]['segment1'] = 'TEST'

    sync_service_resource(base_resources['supplier'],
                          base_resources['service1'],
                          bot_data)

    assert models.Resource.objects.count() == 6
    assert models.Resource.objects.filter(name=bot_data[2].name).exists()


def test_dont_deprive_granting(base_resources):
    bot_server_types = [1, 2]
    bot_data = [generate_bot_server(number, type) for number, type in
                enumerate(bot_server_types)]

    sync_service_resource(base_resources['supplier'],
                          base_resources['service1'],
                          bot_data)

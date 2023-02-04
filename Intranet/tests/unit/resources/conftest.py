import textwrap

import pretend
import pytest
from django.conf import settings

from plan.resources import tasks
from plan.resources.importers import bot
from plan.services.models import Service
from plan.resources.policies import APPROVE_POLICY
from common import factories
from utils import vcr_test

BOT_SOURCE_SLUG = 'test_bot'
TEST_SOURCE_SLUG = 'test_test'

pytestmark = pytest.mark.django_db


# NOTE (lavrukov): Здесь лучше сделать scope='module', так как ручка использует
# только select запросы к базе, но в данный момент сделать это в pytest
# невозможно, так как база инициализируется для каждого теста заново
@pytest.fixture
def base_data():
    """Построим тестовое дерево BOT сервисов
    mobilebrowser (id:100504, res_count:3)
    |
    |----browser (id:981, res_count:6)
    |    |
    |    |----broauto (id:790, res_count:9)
    |    |
    |    `----helpdesk (id:1048, res_count:3)
    |
    `----skynet (id:593, res_count:4)

    И независимо добавим ещё один сервис источника TEST
    gggg (id: , res_count: )
    """
    supplier_data = {
        'id': 100,
        'slug': BOT_SOURCE_SLUG,
        'name': 'Test Bot',
    }
    bot_supplier = factories.ServiceFactory(**supplier_data)

    test_supplier_data = {
        'id': 200,
        'slug': TEST_SOURCE_SLUG,
        'name': 'Test Source',
    }
    test_supplier = factories.ServiceFactory(**test_supplier_data)

    services_info = {
        'mobilebrowser': 100504,
        'browser': 981,
        'broauto': 790,
        'helpdesk': 1048,
        'skynet': 593,
    }

    services = {}
    for name, id in services_info.items():
        services[name] = factories.ServiceFactory(id=id, name=name, slug=name)

    services['broauto'].parent = services['browser']
    services['helpdesk'].parent = services['browser']

    services['browser'].parent = services['mobilebrowser']
    services['skynet'].parent = services['mobilebrowser']

    for service in services.values():
        service.save()
        cassette_name = 'resources/service_{s.name}.json'.format(s=service)
        with vcr_test().use_cassette(cassette_name):
            external_data = bot.get_associated_data(service.id)['servers']
            type_names = {data.type_name for data in external_data}
            types = tasks.create_or_update_bot_types(bot_supplier, {}, type_names)
            tasks.sync_service_resource(bot_supplier, service, types, external_data)

    test_service = factories.ServiceFactory(name='test', slug='test', id=10001)
    test_type = factories.ResourceTypeFactory(supplier=test_supplier, name='test_type')
    test_resource = factories.ResourceFactory(name='test_resource', external_id='1337', type=test_type)

    services['test'] = test_service

    factories.ServiceResourceFactory(
        service=test_service,
        resource=test_resource,
        state='granted',
    )

    user1 = factories.UserFactory()
    staff1 = factories.StaffFactory(user=user1, first_name='Фродо', last_name='Бэггинс')

    Service.rebuildtable()

    return {
        'bot_supplier': bot_supplier,
        'test_supplier': test_supplier,
        'services': services,
        'staff1': staff1,
        'test_type': test_type,
    }


@pytest.fixture()
def yp_quota_resource_type(owner_role):
    resource_type = factories.ResourceTypeFactory(
        code=settings.YP_RESOURCE_TYPE_CODE,
        form_id=100500,
        form_handler=textwrap.dedent('''
        from plan.resources.handlers.yp.forward import process_form_forward
        result = process_form_forward(data, form_metadata)
        '''),
        form_back_handler=textwrap.dedent('''
        from plan.resources.handlers.yp.backward import process_form_backward
        result = process_form_backward(attributes, form_metadata)
        '''),
    )
    resource_type.supplier_roles.add(owner_role)
    resource_type.consumer_roles.add(owner_role)

    return resource_type


@pytest.fixture
def gdpr_resource_type():
    return factories.ResourceTypeFactory(
        code=settings.GDPR_RESOURCE_TYPE_CODE,
        import_plugin='generic',
        import_link='http://yandex.ru/?consumer=abc',
        form_id=1,
        form_handler=textwrap.dedent('''
        from plan.resources.handlers.gdpr.forward import process_form_forward
        result = process_form_forward(data, form_metadata, cleaned_data)
        '''),
        form_back_handler=textwrap.dedent('''
        from plan.resources.handlers.gdpr.backward import process_form_backward
        result = process_form_backward(attributes, form_metadata)
        '''),
        approve_policy=APPROVE_POLICY.RELATED_SERVICE_SUPPLIER
    )


@pytest.fixture()
def yp_quota_service_resource_data(service, person, owner_role, yp_quota_resource_type):
    resource_attributes = {
        'scenario': 'Перераспределение квоты', 'donor_slug': 'donor',
        'cpu': 1, 'memory': '2', 'location': 'SAS', 'segment': 'default',
        'hdd': '0', 'ip4': '0', 'net': '0', 'ssd': '0', 'io_hdd': '0', 'io_ssd': '0',
        'gencfg-groups': '0',
    }
    resource = factories.ResourceFactory(
        type=yp_quota_resource_type,
        attributes=resource_attributes,
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=service,
    )
    factories.ServiceMemberFactory(staff=person, service=service, role=owner_role)

    expected_form_query = {
        'service': str(service.pk),
        'service_name': str(service.name),
        'service_slug': str(service.slug),
        'service_resource': str(service_resource.pk),
        'resource_type': str(yp_quota_resource_type.pk),
        'service_donor': resource.attributes.get('donor_slug'),
        'cpu-float': '1', 'memory': '2', 'iframe': '1',
        'hdd': '0', 'io_hdd': '0', 'ssd': '0', 'io_ssd': '0',
        'ipv4': '0', 'net': '0', 'gencfg-groups': '0',
    }
    expected_api_attributes = [
        {'name': 'cpu', 'value': 1},
        {'name': 'hdd', 'value': '0'},
        {'name': 'ip4', 'value': '0'},
        {'name': 'net', 'value': '0'},
        {'name': 'ssd', 'value': '0'},
        {'name': 'io_hdd', 'value': '0'},
        {'name': 'io_ssd', 'value': '0'},
        {'name': 'memory', 'value': '2'},
        {'name': 'segment', 'value': 'default'},
        {'name': 'location', 'value': 'SAS'},
        {'name': 'scenario', 'value': 'Перераспределение квоты'},
        {'name': 'donor_slug', 'value': 'donor'},
        {'name': 'gencfg-groups', 'value': '0'},
    ]

    return pretend.stub(
        service_resource=service_resource,
        expected_form_query=expected_form_query,
        expected_api_attributes=expected_api_attributes,
    )


@pytest.fixture
def patch_lock(monkeypatch):
    class Mocked(object):

        def __call__(self, *args, **kwargs):
            return self

        def __enter__(self, *args, **kwargs):
            return True

        def __exit__(self, *args, **kwargs):
            pass
    monkeypatch.setattr('plan.resources.tasks.locked_context', Mocked())

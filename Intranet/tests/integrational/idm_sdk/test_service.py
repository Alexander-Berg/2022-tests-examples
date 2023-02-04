import pytest
from django.core.urlresolvers import reverse
from simplejson import loads
from common import factories
from mock import patch


pytestmark = [pytest.mark.django_db]


@pytest.fixture
def base_data_serv(settings):
    factories.ServiceTypeFactory(code='undefined')
    root = factories.ServiceFactory(name='Root service', slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG,
                                    path='/%s/' % settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)

    service1 = factories.ServiceFactory(name='service1', slug='slug1', path='/slug1/')

    service2 = factories.ServiceFactory(name='service2', slug='slug2', path='/slug1/slug2/')
    service2.parent = service1
    service2.save()

    management_scope = factories.RoleScopeFactory(slug='management')
    product_head = factories.RoleFactory(
        name='Руководитель сервиса',
        name_en='Head',
        scope=management_scope,
        code='product_head',
    )
    development_scope = factories.RoleScopeFactory(slug='development')
    developer = factories.RoleFactory(
        name='Разработчик',
        name_en='Developer',
        scope=development_scope,
        service=service1,
    )
    support_scope = factories.RoleScopeFactory(slug='support')
    support = factories.RoleFactory(name='Поддержка', name_en='Support', scope=support_scope)

    return {
        'root': root,
        'service1': service1,
        'service2': service2,
        'product_head': product_head,
        'support': support,
        'developer': developer,
    }


def test_create_service(base_data_serv, client, person):
    url = reverse('services:service_create')
    with patch('plan.idm.manager.Manager._run_request') as mock_request:
        client.json.post(url, {
            'owner': person.pk,
            'slug': 'service3',
            'name': {'ru': 'Сервис 3', 'en': 'Service 3'},
            'move_to': base_data_serv['service2'].pk,
        }).json()

    assert mock_request.call_count == 10

    # Проверка поддерева ролей
    expected_calls = [
        'https://idm-api.test.yandex-team.ru/api/v1/rolenodes/abc_dev/type/services/services_key/meta_other/meta_other_key/service3/',
        'https://idm-api.test.yandex-team.ru/api/v1/rolenodes/abc_dev/type/services/services_key/meta_other/meta_other_key/service3/service3_key/',
        'https://idm-api.test.yandex-team.ru/api/v1/rolenodes/abc_dev/type/services/services_key/meta_other/meta_other_key/service3/service3_key/*/',
        'https://idm-api.test.yandex-team.ru/api/v1/rolenodes/abc_dev/type/services/services_key/meta_other/meta_other_key/service3/service3_key/*/role/',
        'https://idm-api.test.yandex-team.ru/api/v1/rolenodes/abc_dev/type/services/services_key/meta_other/meta_other_key/service3/service3_key/*/role/1/',
        'https://idm-api.test.yandex-team.ru/api/v1/rolenodes/abc_dev/type/services/services_key/meta_other/meta_other_key/service3/service3_key/*/role/2/',
        'https://idm-api.test.yandex-team.ru/api/v1/rolenodes/abc_dev/type/services/services_key/meta_other/meta_other_key/service3/service3_key/*/role/3/',
        'https://idm-api.test.yandex-team.ru/api/v1/rolenodes/abc_dev/type/services/services_key/meta_other/meta_other_key/service3/service3_key/*/role/4/',
        'https://idm-api.test.yandex-team.ru/api/v1/rolenodes/abc_dev/type/services/services_key/meta_other/meta_other_key/service3/service3_key/*/role/6/',
    ]
    for n, expected_url in enumerate(expected_calls):
        request = mock_request.call_args_list[n][0][0]
        assert request.url == expected_url

    # Запрос роли руководителя сервиса
    request = mock_request.call_args_list[9][0][0]
    role_params = loads(request.data)
    assert role_params['path'] == '/services/meta_other/service3/*/4/'
    assert role_params['system'] == 'abc_dev'
    assert request.url == 'https://idm-api.test.yandex-team.ru/api/v1/rolerequests/'

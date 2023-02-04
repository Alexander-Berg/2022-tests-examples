# coding: utf-8


import pytest

from idm.core.models import Action
from idm.nodes.hashers import Hasher
from idm.services.fetchers import IDSServiceFetcher
from idm.services.models import Service
from idm.tests.utils import LocalFetcherMixin, CountIncreasedContext, setify_changes

pytestmark = pytest.mark.django_db


class LocalFetcher(LocalFetcherMixin, IDSServiceFetcher):
    pass


@pytest.fixture(autouse=True)
def mock_fetcher():
    Service.fetcher = LocalFetcher()
    Service.hasher = Hasher(debug=False)


@pytest.fixture
def service_root():
    root, _ = Service.objects.get_or_create(parent=None, external_id=0)
    root.external_state = 'supported'
    root.save()
    return root


def synchronize(root, context=None):
    if context is None:
        context = CountIncreasedContext()

    updated_before = root.updated_at
    with context as changed_data:
        result, _ = root.synchronize()
        assert result is True
    assert root.updated_at != updated_before

    return changed_data


def lang_field(ru=None, en=None):
    ru = ru or en or ''
    en = en or ru or ''
    return {'ru': ru, 'en': en}


@pytest.fixture
def simple_service_data():
    return [
        {
            'id': 1,
            'parent': None,
            'slug': 'service1',
            'name': lang_field(ru='Сервис 1'),
            'description': lang_field(),
            'state': 'deleted',
            'tags': [],
            'membership_inheritance': True
        },
        {
            'id': 2,
            'parent': {
                'id': 1
            },
            'slug': 'subservice1',
            'name': lang_field(ru='Подсервис 1'),
            'description': lang_field(),
            'state': 'deleted',
            'tags': [],
            'membership_inheritance': False
        },
        {
            'id': 3,
            'parent': None,
            'slug': 'Service2',
            'name': lang_field(ru='Сервис 2'),
            'description': lang_field(),
            'state': 'supported',
            'membership_inheritance': False,
            'tags': [
                {
                    "id": 5
                }
            ],
        },
    ]


@pytest.fixture
def additional_service_data():
    return [
        {
            'id': 6,
            'parent': {
                'id': 2
            },
            'slug': 'service6',
            'name': lang_field(ru='Сервис 6'),
            'description': lang_field(),
            'state': 'supported',
            'membership_inheritance': True,
            'tags': [],
        },
        {
            'id': 7,
            'parent': {
                'id': 6
            },
            'slug': 'service7',
            'name': lang_field(ru='Сервис 7'),
            'description': lang_field(),
            'state': 'supported',
            'membership_inheritance': True,
            'tags': [],
        },
        {
            'id': 8,
            'parent': {
                'id': 1
            },
            'slug': 'service8',
            'name': lang_field(ru='Сервис 8'),
            'description': lang_field(),
            'state': 'supported',
            'membership_inheritance': True,
            'tags': [],
        },
        {
            'id': 9,
            'parent': {
                'id': 8
            },
            'slug': 'service9',
            'name': lang_field(ru='Сервис 9'),
            'description': lang_field(),
            'state': 'supported',
            'membership_inheritance': False,
            'tags': [],
        },
        {
            'id': 10,
            'parent': {
                'id': 8
            },
            'slug': 'service10',
            'name': lang_field(ru='Сервис 10'),
            'description': lang_field(),
            'state': 'supported',
            'membership_inheritance': True,
            'tags': [],
        },
    ]


def test_service_sync(service_root, simple_service_data):
    assert Service.objects.count() == 1  # 1 корень

    # добавление
    service_root.fetcher.set_data(('abc', 'services'), simple_service_data)
    changed_data = synchronize(service_root, CountIncreasedContext((Service, 3), (Action, 3)))

    assert all(action.action == 'service_created' for action in changed_data.get_new_objects(Action))
    subservice = Service.objects.get(external_id=2)
    assert subservice.parent.external_id == 1
    assert subservice.path == '//service1/subservice1/'

    # изменение
    simple_service_data[2]['name']['en'] = 'Service 2'
    service_root.fetcher.set_data(('abc', 'services'), simple_service_data)

    service2 = Service.objects.get(external_id=3)
    assert service2.name_en == 'Сервис 2'
    assert service2.slug == 'service2'
    changed_data = synchronize(service_root, CountIncreasedContext((Service, 0), (Action, 1)))
    assert all(action.action == 'service_changed' for action in changed_data.get_new_objects(Action))
    service2.refresh_from_db()
    assert service2.name_en == 'Service 2'

    # перемещение
    simple_service_data.append({
        'id': 4,
        'parent': None,
        'slug': 'superservice1',
        'name': lang_field(ru='Суперсервис 1'),
        'description': lang_field(),
        'state': 'supported',
        'membership_inheritance': False,
        'tags': [
            {
                "id": 5
            }
        ]
    })
    simple_service_data[2]['parent'] = {'id': 4}
    service_root.fetcher.set_data(('abc', 'services'), simple_service_data)

    assert service2.parent == service_root
    changed_data = synchronize(service_root, CountIncreasedContext((Service, 1), (Action, 2)))
    assert [action.action for action in changed_data.get_new_objects(Action)] == ['service_created', 'service_moved']
    service2.refresh_from_db()
    superservice1 = Service.objects.get(external_id=4)
    assert superservice1.parent == service_root
    assert service2.membership_inheritance is False
    assert service2.parent == superservice1
    assert service2.path == '//superservice1/service2/'


def test_service_sync_with_restore(service_root, simple_service_data):
    assert Service.objects.count() == 1  # 1 корень

    service_root.fetcher.set_data(('abc', 'services'), simple_service_data)
    synchronize(service_root)

    # Удалим сервис
    service_root.fetcher.set_data(('abc', 'services'), simple_service_data[:2])
    changed_data = synchronize(service_root, CountIncreasedContext((Service, 0), (Action, 1)))
    assert all(action.action == 'service_removed' for action in changed_data.get_new_objects(Action))
    service2 = Service.objects.get(external_id=3)
    assert service2.state == 'depriving'
    assert service2.external_state == 'deleted'

    # А теперь восстановим его
    service_root.fetcher.set_data(('abc', 'services'), simple_service_data)
    changed_data = synchronize(service_root, CountIncreasedContext((Service, 0), (Action, 1)))
    assert [action.action for action in changed_data.get_new_objects(Action)] == ['service_restored']
    service2 = Service.objects.get(external_id=3)
    assert service2.state == 'active'
    assert service2.external_state == 'supported'


def test_service_get_inherited_services(service_root, simple_service_data, additional_service_data):
    simple_service_data.extend(additional_service_data)
    service_root.fetcher.set_data(('abc', 'services'), simple_service_data)
    synchronize(service_root)
    service7 = Service.objects.get(external_id=7)
    inherited_services = service7.get_inherited_services()
    assert_services_id_equal_to(inherited_services, {6, 2})


def test_get_descendants_splintered_by_inheritance(service_root, simple_service_data, additional_service_data):
    simple_service_data.extend(additional_service_data)
    service_root.fetcher.set_data(('abc', 'services'), simple_service_data)
    synchronize(service_root)
    service1 = Service.objects.get(external_id=1)
    descendants_with_inheritance, ancestors = service1.get_descendants_splintered_by_inheritance()
    assert_services_id_equal_to(descendants_with_inheritance, {1, 8, 10})
    assert_services_id_equal_to(ancestors[0], {1})
    assert_services_id_equal_to(ancestors[1], {1, 8})
    assert_services_id_equal_to(ancestors[2], {1, 8, 10})


def test_get_changes_for_new_root(service_root, simple_service_data, additional_service_data):
    simple_service_data.extend(additional_service_data)
    service_root.fetcher.set_data(('abc', 'services'), simple_service_data)
    synchronize(service_root)
    service8 = Service.objects.get(external_id=8)
    service7 = Service.objects.get(external_id=7)
    service10 = Service.objects.get(external_id=10)
    lost_services = {Service.objects.get(external_id=1)}
    obtained_services = {
        service7,
        Service.objects.get(external_id=6),
        Service.objects.get(external_id=2),
    }
    changes = service8.get_changes_for_new_root(service7)
    setify_changes(changes)
    assert changes == {
        service8: {
            'lost': lost_services,
            'obtained': obtained_services,
            'ancestors': {service8},
        },
        service10: {
            'lost': lost_services,
            'obtained': obtained_services,
            'ancestors': {service8, service10},
        },
    }


def assert_services_id_equal_to(services, ids):
    assert {service.external_id for service in services} == ids

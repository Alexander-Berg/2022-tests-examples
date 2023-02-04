# coding: utf-8


import pytest

from django.utils import timezone

from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.tests.utils import assert_response, mock_ids_repo, use_proxied_suggest
from idm.users.constants.group import GROUP_TYPES
from idm.users.constants.user import USER_TYPES
from idm.users.models import User, Group, GroupMembership
from idm.utils import reverse


pytestmark = pytest.mark.django_db


def test_subject_suggest_without_intrasearch(client, arda_users, department_structure):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/subjects/all')

    client.login('frodo')
    with use_proxied_suggest(should_use=False):
        response = client.json.get(suggest_url)
        assert response.status_code == 200
        data = response.json()
        expected_keys = {'is_group', 'id', 'login', 'first_name', 'last_name', 'full_name', 'department__name',
                         'subject_type', 'group_type'}
        assert set(data['data'][0].keys()) == expected_keys

        data = response.json()
        assert data['meta'] == {'offset': 0, 'limit': 20}
        logins = {item['login'] for item in data['data']}

        # проверка на подмножество, так как фикстуры могут расширяться, а точное совпадение сделает тест хрупким
        assert logins.issuperset({'varda', 'sam', 'frodo', 'gimli', 'gandalf', 'sauron'})

        response = client.json.get(suggest_url, {'limit': 1})
        assert_response(response, meta={'offset': 0, 'limit': 1}, login={'aragorn'})

        response = client.json.get(suggest_url, {'limit': 1, 'offset': 1})
        assert_response(response, meta={'offset': 1, 'limit': 1}, login={'bilbo'})

        response = client.json.get(suggest_url, {'offset': 30})
        assert_response(response, meta={'offset': 30, 'limit': 20}, login=set())

        response = client.json.get(suggest_url, {'q': 'od'})
        assert_response(response, login={'frodo'})

        response = client.json.get(suggest_url, {'q': 'Fello'})
        assert_response(response, login={'fellowship-of-the-ring'})

        response = client.json.get(suggest_url, {'q': 'ФР БЭ'})
        assert_response(response, login={'frodo'})

        response = client.json.get(suggest_url, {'q': 'Бр ЛЬ Ц'})
        assert_response(response, login={'fellowship-of-the-ring'})

        response = client.json.get(suggest_url, {'id': 'frodo'})
        assert_response(response, login={'frodo'})

        response = client.json.get(suggest_url, {'system': 'simple', 'id': 'xxx'})
        assert_response(response, login=set())


def test_subject_suggest_with_intrasearch(client, arda_users, department_structure):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/subjects/all')

    client.login('frodo')

    objects = [
        {
            'url': 'https://staff.test.yandex-team.ru/frodo',
            'fields': [
                {
                    'type': 'is_active',
                    'value': True
                },
                {
                    'type': 'department_name',
                    'value': 'Some dept'
                },
                {
                    'type': 'first_name',
                    'value': 'Фродо'
                },
                {
                    'type': 'last_name',
                    'value': 'Беггинс'
                },
                {
                    'type': 'email',
                    'value': 'frodo@yandex-team.ru'
                },
                {
                    'type': 'object_type',
                    'value': 'users'
                }
            ],
            'layer': 'idm_subjects',
            'id': 'frodo',
            'title': 'Фродо Беггинс'
        }, {
            'url': 'https://staff.test.yandex-team.ru/groups/test-tanker/',
            'fields': [
                {
                    'type': 'slug',
                    'value': 'test-tanker'
                },
                {
                    'type': 'state',
                    'value': 'active'
                },
                {
                    'type': 'type',
                    'value': 'wiki'
                },
                {
                    'type': 'object_type',
                    'value': 'groups'
                }
            ],
            'layer': 'idm_groups',
            'id': '26986',
            'title': 'test-tanker'
        }
    ]

    with use_proxied_suggest(should_use=True), mock_ids_repo('intrasearch', 'idm_subjects', objects) as repo:
        repo.get.reset_mock()
        response = client.json.get(suggest_url, {'q': 'hello'})
        assert response.json() == {
            'meta': {
                'limit': 20,
                'offset': 0
            },
            'data': [
                {
                    'first_name': 'Фродо',
                    'last_name': 'Беггинс',
                    'is_group': False,
                    'department__name': 'Some dept',
                    'subject_type': 'user',
                    'full_name': 'Фродо Беггинс',
                    'login': 'frodo',
                    'id': 'frodo',
                    'group_type': None
                },
                {
                    'first_name': None,
                    'last_name': None,
                    'is_group': True,
                    'department__name': None,
                    'subject_type': 'group',
                    'full_name': 'test-tanker',
                    'login': 'test-tanker',
                    'id': '26986',
                    'group_type': 'wiki'
                }
            ]
        }

        repo.get.assert_called_once_with({
            'allow_empty': True,
            'idm_subjects.page': 0,
            'idm_subjects.per_page': 20,
            'language': 'ru',
            'layers': 'idm_subjects',
            'text': 'hello',
        })

        response = client.json.get(suggest_url, {'id': 'frodo'})
        assert_response(response, login={'frodo'})

        response = client.json.get(suggest_url, {'limit': 'a', 'offset': '-2'})
        assert response.status_code == 400
        assert response.json() == {
            'error_code': 'BAD_REQUEST',
            'message': 'Invalid data sent',
            'errors': {
                'limit': ['Ожидается число'],
                'offset': ['Значение должно быть неотрицательным'],
            }
        }


def test_subject_with_hyphen(client, simple_system, arda_users):
    """
    Проверяем, что пользователь с дефисом в username находится через саджест subject-ов
    https://st.yandex-team.ru/RULES-2145
    """
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/subjects/all')
    client.login('frodo')
    response = client.json.get(suggest_url, {'system': simple_system.slug, 'q': 'ch-'})
    data = response.json()['data']
    assert len(data) == 1
    assert data[0]['id'] == 'witch-king-of-angmar'
    response = client.json.get(suggest_url, {'system': simple_system.slug, 'q': 'witch'})
    data = response.json()['data']
    assert len(data) == 1
    assert data[0]['id'] == 'witch-king-of-angmar'


def test_suggest_tvm_app_by_service(client):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/subjects/all')
    tvm_app = User.objects.create(username=12345, type=USER_TYPES.TVM_APP)
    service = Group.objects.create(slug='xxxx', type=GROUP_TYPES.TVM_SERVICE)

    response = client.json.get(suggest_url, {'q': 'xxxx', 'type': USER_TYPES.TVM_APP})
    assert len(response.json()['data']) == 0

    GroupMembership.objects.create(user=tvm_app, group=service, is_direct=True, state=GROUPMEMBERSHIP_STATE.ACTIVE)

    response = client.json.get(suggest_url, {'q': 'xxxx', 'type': USER_TYPES.TVM_APP})
    assert len(response.json()['data']) == 1

    response = client.json.get(suggest_url, {'q': 'xxxx'})
    assert len(response.json()['data']) == 0


def test_tvm_subject_suggest(client, arda_users, department_structure):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/subjects/all')
    # Приложение без группы / без активных групп не отдаем в саджесте
    response = client.json.get(suggest_url, {'id': 'tvm_app'})
    assert_response(response, login=set())

    # В качестве department__name отдаем название сервиса, которому принадлежит приложение
    group = Group.objects.create(name='some tvm service', slug='tvm_group', type=GROUP_TYPES.TVM_SERVICE)
    GroupMembership.objects.update_or_create(
        user_id=arda_users.tvm_app.pk, group_id=group.pk, is_direct=True,
        defaults={
            'state': GROUPMEMBERSHIP_STATE.ACTIVE,
            'date_joined': timezone.now(),
            'date_leaved': None,
        })
    response = client.json.get(suggest_url, {'id': arda_users.tvm_app.username})
    assert_response(response, login={arda_users.tvm_app.username}, department__name={'some tvm service'})


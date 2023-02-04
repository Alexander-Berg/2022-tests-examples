# -*- coding: utf-8 -*-
import json
import responses

from bson.objectid import ObjectId
from unittest.mock import (
    patch,
    ANY,
)
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    contains,
    empty,
    not_none,
    anything,
)

from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    prepare_resource,
)
from intranet.yandex_directory.src.yandex_directory.auth.service import Service
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    ActionModel,
    EventModel,
    OrganizationServiceModel,
    ServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.resource import (
    ResourceModel,
    ResourceRelationModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.group import GroupModel
from intranet.yandex_directory.src.yandex_directory.core.views.resources import (
    RESOURCE_OUT_SCHEMA,
)

from testutils import (
    TestCase,
    get_auth_headers,
    get_oauth_headers,
    PaginationTestsMixin,
    create_department,
    create_group,
    create_organization,
    assert_that_not_found,
    oauth_success,
    OAUTH_CLIENT_ID,
    set_auth_uid,
    create_service,
    tvm2_auth_success,
    tvm2_auth,
    assert_not_called,
    assert_called_once,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import only_fields
from intranet.yandex_directory.src.yandex_directory.common.utils import validate_data_by_schema
from intranet.yandex_directory.src.yandex_directory.core.idm.exceptions import IDMB2BException


def prepare_group(group):
    return only_fields(
        group,
        'id',
        'name',
        'type',
    )


def prepare_department(department):
    return only_fields(
        department,
        'id',
        'name',
        'parent_id',
    )


def prepare_user(user):
    return only_fields(
        user,
        'id',
        'nickname',
        'email',
        'name',
        'gender',
        'department_id',
    )


prepare_object_func = {
    'group': prepare_group,
    'department': prepare_department,
    'user': prepare_user,
}


class TestResourceBase(TestCase):
    def setUp(self):
        super(TestResourceBase, self).setUp()
        # запросы будут без заголовка X-UID
        set_auth_uid(None)
        # заголовок для авторизации по токенам
        self.auth_header = get_auth_headers(as_uid=self.user['id'])
        # заголовок для авторизации по OAuth
        self.oauth_auth_header = get_oauth_headers(as_org=self.organization['id'])
        self.clean_actions_and_events()


class TestResourceList__get(PaginationTestsMixin, TestResourceBase):
    entity_list_url = '/resources/?service=autotest'
    entity_model = ResourceModel
    entity_model_prefetch_related = {
        'relations': {
            'user': True,
            'department': True,
            'group': True
        }
    }

    def create_entity(self):
        self.entity_counter += 1

        return ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'department_id': self.departments[0]['id']
                }
            ]
        )

    def get_entity_model_filters(self):
        response = super(TestResourceList__get, self).get_entity_model_filters()
        if 'service' not in response:
            response['service'] = 'autotest'
        return response

    def prepare_entity_for_api_response(self, entity):
        return prepare_resource(entity)

    def setUp(self):
        super(TestResourceList__get, self).setUp()
        set_auth_uid(None)
        self.departments = [
            create_department(
                self.main_connection,
                name={'ru': 'admins'}, org_id=self.organization['id']
            ),
            create_department(
                self.main_connection,
                name={'ru': 'managers'}, org_id=self.organization['id']
            ),
            create_department(
                self.main_connection,
                name={'ru': 'designers'}, org_id=self.organization['id']
            ),
        ]
        self.entity_list_request_headers = get_auth_headers(as_org=self.organization['id'])

    def simple(self, headers, slug):
        # вспомогательный метод для test_simple и test_simple_oauth
        # получаем существующий ресур, на который у нас есть права
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=slug,
            relations=[
                {
                    'name': 'read',
                    'department_id': self.department['id']
                }
            ]
        )

        response_data = self.get_json('/resources/', headers=headers)

        assert_that(
            response_data,
            has_entries(
                result=contains(
                    has_entries(
                        id=resource['id'],
                        service=slug,
                        relations=contains(
                            has_entries(
                                object_type='department',
                                object=has_entries(
                                    id=self.department['id'],
                                )
                            )
                        )
                    )
                )
            )
        )

    def test_simple(self):
        # получаем ресурс при авторизации по токену
        self.simple(self.auth_header, 'autotest')

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_resources,
                                            scope.work_with_any_organization])
    def test_simple_oauth(self):
        # получаем ресурс при авторизации по OAuth
        self.simple(self.oauth_auth_header, self.service['slug'])

    def test_should_return_resouce_by_external_id_instead_of_id(self):
        # проверяем, что получение ресурса работает по external_id, а не по id
        resource = ResourceModel(self.main_connection).create(
            id=str(ObjectId()),
            org_id=self.organization['id'],
            service='autotest',
            relations=[],
            external_id=str(ObjectId()),
        )

        self.get_json('/resources/%s/' % resource['id'], headers=self.auth_header, expected_code=404)

        # теперь создадим ресурс с id = resource.external_id
        ResourceModel(self.main_connection).create(
            id=resource['external_id'],
            org_id=self.organization['id'],
            service='autotest',
            relations=[],
            external_id=str(ObjectId()),
        )

        # ожидаем, что всё равно будет 404, так как нет ресурса с таким external_id
        self.get_json('/resources/%s/' % resource['id'], headers=self.auth_header, expected_code=404)

        # проверим, что возвращается правильный ресурс (созданный в начале теста)
        response_data = self.get_json('/resources/%s/' % resource['external_id'], headers=self.auth_header)
        exp_response = {
            'id': resource['external_id'],
            'service': 'autotest',
            'relations': [],
            'metadata': {},
        }
        assert_that(response_data, equal_to(exp_response))

    def test_should_filter_by_service_by_default(self):
        for i in range(2):
            ResourceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service='autotest',
                relations=[
                    {
                        'name': 'read',
                        'department_id': self.departments[i]['id']
                    }
                ]
            )

        # resource from other service that should not be in response
        yamb_resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'name': 'read',
                    'department_id': self.departments[0]['id']
                }
            ]
        )

        response_data = self.get_json(
            '/resources/',
            headers=self.auth_header
        ).get('result')
        self.assertEqual(len(response_data), 2)
        self.assertNotIn(yamb_resource['external_id'], [i['id'] for i in response_data])

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_resources,
                                            scope.work_with_any_organization])
    def test_should_ignore_specific_service_without_scope(self):
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'name': 'read',
                    'department_id': self.departments[0]['id']
                }
            ]
        )

        self.get_json(
            '/resources/',
            headers=self.oauth_auth_header,
            query_string={'service': 'yamb'},
            expected_code=403,
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_resources,
                                            scope.work_with_any_organization,
                                            scope.read_service_resources])
    def test_should_show_resources_for_specific_service_with_scope(self):
        yamb_resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'name': 'read',
                    'department_id': self.departments[0]['id']
                }
            ]
        )

        response_data = self.get_json(
            '/resources/',
            headers=self.oauth_auth_header,
            query_string={'service': 'yamb'}
        ).get('result')
        self.assertEqual(len(response_data), 1)
        self.assertEqual(response_data[0]['id'], yamb_resource['external_id'])

    def test_should_return_401_without_org_id_and_user(self):
        for i in range(2):
            ResourceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service='autotest',
                relations=[
                    {
                        'name': 'read',
                        'department_id': self.departments[i]['id'],
                    }
                ]
            )

        self.get_json('/resources/', headers={}, expected_code=401)

    def test_should_filter_by_org_id_and_user(self):
        """
        Должны возвращаться только ресурсы для указаной организации
        """
        for i in range(2):
            ResourceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service='autotest',
                relations=[
                    {
                        'name': 'read',
                        'department_id': self.departments[i]['id'],
                    }
                ]
            )

        # resource from other org_id that should not be in response
        another_organization_resource = self._create_resource_with_new_organization()

        response_data = self.get_json(
            '/resources/',
            headers=self.auth_header,
        ).get('result')

        self.assertEqual(len(response_data), 2)
        self.assertNotIn(another_organization_resource['external_id'], [i['id'] for i in response_data])

    def _create_resource_with_new_organization(self):
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='my_organization',
            domain_part='yabiz.ru',
        )
        user = self.create_user(org_id=organization['organization']['id'])
        organization_resource = ResourceModel(self.main_connection).create(
            org_id=organization['organization']['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': user['id'],
                }
            ]
        )
        return organization_resource

    def test_should_filter_by_id_if_it_in_get_params(self):
        resources = []
        for i in range(2):
            resources.append(
                ResourceModel(self.main_connection).create(
                    org_id=self.organization['id'],
                    service='autotest',
                    relations=[
                        {
                            'name': 'read',
                            'department_id': self.departments[0]['id']
                        }
                    ]
                )
            )

        response_data = self.get_json(
            '/resources/?id=%s' % resources[0]['id'],
            headers=self.auth_header).get('result')
        self.assertEqual(len(response_data), 1)
        self.assertEqual(response_data[0]['id'], resources[0]['id'])

    def test_should_filter_by_list_of_ids_if_it_in_get_params(self):
        resources = []
        for i in range(3):
            resources.append(
                ResourceModel(self.main_connection).create(
                    org_id=self.organization['id'],
                    service='autotest',
                    relations=[
                        {
                            'name': 'read',
                            'department_id': self.departments[i]['id']
                        }
                    ]
                )
            )

        ids = [resources[0]['id'], resources[1]['id']]
        url = '/resources/?id=%s,%s' % (ids[0], ids[1])
        response_data = self.get_json(
            url, headers=self.auth_header).get('result')
        self.assertEqual(len(response_data), 2)
        self.assertEqual(
            sorted([i['id'] for i in response_data]),
            sorted(ids)
        )

    def test_should_filter_by_relation_name_if_it_in_get_params(self):
        resources = []
        for i in range(2):
            resources.append(
                ResourceModel(self.main_connection).create(
                    org_id=self.organization['id'],
                    service='autotest',
                    relations=[
                        {
                            'name': 'read_%s' % i,
                            'department_id': self.departments[i]['id']
                        }
                    ]
                )
            )

        response_data = self.get_json(
            '/resources/?relation_name=read_0',
            headers=self.auth_header).get('result')
        self.assertEqual(len(response_data), 1)
        self.assertEqual(response_data[0]['id'], resources[0]['id'])

    def test_should_filter_by_list_of_relation_names_if_it_in_get_params(self):
        resources = []
        for i in range(3):
            resources.append(
                ResourceModel(self.main_connection).create(
                    org_id=self.organization['id'],
                    service='autotest',
                    relations=[
                        {
                            'name': 'read_%s' % i,
                            'department_id': self.departments[i]['id']
                        }
                    ]
                )
            )

        relation_names = ['read_0', 'read_1']
        url = '/resources/?relation_name=%s,%s' % (relation_names[0], relation_names[1])
        response_data = self.get_json(
            url, headers=self.auth_header).get('result')
        self.assertEqual(len(response_data), 2)
        self.assertEqual(
            sorted([i['id'] for i in response_data]),
            sorted([resources[0]['id'], resources[1]['id']])
        )

    def test_should_filter_by_user_if_it_in_get_params(self):
        """
        Проверяет, что по фильтру пользователя отдаются рeсурсы,
        связанные через группу
        """
        resources = []
        group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        GroupModel(self.main_connection).add_member(
            org_id=self.organization['id'],
            group_id=group['id'],
            member={'type': 'user', 'id': self.user['id']}
        )
        resources.append(
            ResourceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service='autotest',
                relations=[
                    {
                        'name': 'read',
                        'group_id': int(group['id']),
                    }
                ]
            )
        )
        response_data = self.get_json(
            '/resources/?user_id=%s' % self.user['id'],
            headers=self.auth_header).get('result', [])
        self.assertEqual(len(response_data), 1)
        self.assertEqual(response_data[0]['id'], resources[0]['id'])


class TestResourceList__post(TestResourceBase):
    def setUp(self):
        super(TestResourceList__post, self).setUp()

        self.groups = [
            GroupModel(self.main_connection).create(
                org_id=self.organization['id'],
                name={'ru': 'admins'}
            ),
            GroupModel(self.main_connection).create(
                org_id=self.organization['id'],
                name={'ru': 'managers'}
            ),
            GroupModel(self.main_connection).create(
                org_id=self.organization['id'],
                name={'ru': 'designers'}
            ),
        ]
        self.department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )

    def post(self, headers, slug):
        # вспомогательный метод для test_post и test_post_oauth
        # проверяем что ручка создания ресурса работает
        response_data = self.post_json('/resources/', data={}, headers=headers)
        assert_that(
            response_data,
            has_entries(
                service=slug,
                relations=empty(),
                id=not_none(),
            )
        )
        assert_that(
            ActionModel(self.main_connection).filter(org_id=self.organization['id'], name=action.resource_add).count(),
            equal_to(1)
        )
        events = EventModel(self.main_connection).filter(org_id=self.organization['id'], name=event.resource_added).all()
        assert_that(
            events,
            contains(
                has_entries(
                    object=has_entries(
                        id=anything(),
                        external_id=anything(),
                        service=slug,
                    )
                )
            )
        )

    def test_post(self):
        # создаем ресурс при авторизации по токену
        self.post(self.auth_header, 'autotest')

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.write_resources,
                                            scope.work_with_any_organization])
    def test_post_oauth(self):
        # создаем ресурс при авторизации по OAuth
        self.post(self.oauth_auth_header, self.service['slug'])

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_resources,
                                            scope.work_with_any_organization])
    def test_oauth_without_scopes(self):
        # пытаемся создать ресурс при авторизации по OAuth, но унас на это нет прав
        self.post_json(
            '/resources/',
            data={},
            headers=self.oauth_auth_header,
            expected_code=403
        )

    def test_post_with_id(self):
        # проверяем, что при создании ресурса мы можем указать кастомный id
        resource_id = 'some-resource-custom-id-for-test'

        response = self.post_json('/resources/', data={'id': resource_id}, headers=self.auth_header)

        assert_that(
            response,
            has_entries(
                service='autotest',
                relations=[],
                id=resource_id,
            ),
        )

        # проверим, что можем получить этот ресурс по id
        response = self.get_json('/resources/%s/' % response['id'], headers=self.auth_header)
        exp_response = {
            'id': resource_id,
            'service': 'autotest',
            'relations': [],
            'metadata': {},
        }
        assert_that(response, equal_to(exp_response))

    def test_duplicate_post(self):
        # При повторной попытке запостить resource с тем же id мы должны выдавать
        # ошибку 409.
        resource_id = 'some-resource-custom-id-for-test'

        response = self.post_json('/resources/', data={'id': resource_id}, headers=self.auth_header)

        # Вторая попытка не должна пройти
        response = self.post_json(
            '/resources/',
            data={'id': resource_id},
            headers=self.auth_header,
            expected_code=409,
            expected_error_code='resource_already_exists',
            expected_message='Resource with such id already exists',
        )

    def test_post_with_relations(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_uids') as mock_user_data:
            mock_user_data.return_value = {
                self.user['id']: {
                    'aliases': [],
                    'birth_date': '2000-01-01',
                    'default_email': 'only_passport_user@khunafin.xyz',
                    'first_name': 'user',
                    'is_maillist': False,
                    'language': 'ru',
                    'last_name': 'user',
                    'login': 'only_passport_user@khunafin.xyz',
                    'sex': '1',
                    'avatar_id': '123445',
                    'uid': self.user['id'],
                },
            }
            response = self.client.post(
                'resources/',
                headers=self.auth_header,
                content_type='application/json',
                data=json.dumps({
                    'relations': [
                        {
                            'name': 'read',
                            'object_type': 'group',
                            'object_id': self.groups[0]['id']
                        },
                        {
                            'name': 'write',
                            'object_type': 'user',
                            'object_id': self.user['id']
                        },
                        {
                            'name': 'delete',
                            'object_type': 'department',
                            'object_id': self.department['id']
                        },
                    ]
                })
            )
        self.assertEqual(response.status_code, 201)
        response_data = json.loads(response.data)
        expected = {
            'service': 'autotest',
            'relations': [
                {
                    'object': {
                        'id': self.groups[0]['id'],
                        'name': self.groups[0]['name'],
                        'type': self.groups[0]['type'],
                        'members_count': self.groups[0]['members_count'],
                    },
                    'name': 'read',
                    'object_type': 'group'
                },
                {
                    'object': {
                        'id': self.user['id'],
                        'nickname': self.user['nickname'],
                        'name': self.user['name'],
                        'gender': self.user['gender'],
                        'department_id': self.user['department_id'],
                        'position': self.user['position'],
                        'avatar_id': '123445',
                    },
                    'name': 'write',
                    'object_type': 'user'
                },
                {
                    'object': {
                        'id': self.department['id'],
                        'name': self.department['name'],
                        'parent_id': self.department['parent_id'],
                        'members_count': self.department['members_count'],
                    },
                    'name': 'delete',
                    'object_type': 'department'
                },
            ]}
        # не проверяем bigserial-id relations
        for r in response_data['relations']:
            del r['id']
        self.assertEqual(response_data['service'], expected['service'])
        self.assertEqual(
            sorted(response_data['relations'], key=lambda item: item['object']['id']),
            sorted(expected['relations'], key=lambda item: item['object']['id'])
        )
        self.assertTrue('id' in response_data)

    def test_post_with_not_existing_id(self):
        # если ресурс с указанным типом не найден в базе то получим 422 ошибку

        not_exists_group_id = 12312312
        response = self.post_json(
            'resources/',
            data={
                'relations': [
                    {
                        'name': 'read',
                        'object_type': 'group',
                        'object_id': not_exists_group_id
                    }

                ]
            },
            expected_code=422,
            headers=self.auth_header,
        )
        assert_that(
            response,
            has_entries(
                code='constraint_validation.objects_not_found',
                message=not_none(),
                params=has_entries(
                    type='group'
                )
            )
        )

    def test_with_custom_param(self):
        response = self.client.post(
            'resources/',
            headers=self.auth_header,
            content_type='application/json',
            data=json.dumps({
                'hello': [1, 2, 3]
            })
        )
        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)

        assert_that(
            response_data,
            has_entries(
                code='schema_validation_error',
            )
        )
        error_message = (
            'Additional properties are not allowed (\'hello\' was unexpected)'
        )
        self.assertTrue(
            response_data['params']['errors'][0]['message'].startswith(error_message)
        )

    def test_with_relations_with_bad_simple_format(self):
        response = self.client.post(
            'resources/',
            headers=self.auth_header,
            content_type='application/json',
            data=json.dumps({
                'relations': 'sosisa'
            })
        )
        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)

        assert_that(
            response_data,
            has_entries(
                code='schema_validation_error',
            )
        )
        error_message = '\'sosisa\' is not of type \'array\''
        self.assertTrue(
            response_data['params']['errors'][0]['message'].startswith(error_message)
        )

    def test_with_relations_with_object_without_name(self):
        response = self.client.post(
            'resources/',
            headers=self.auth_header,
            content_type='application/json',
            data=json.dumps({
                'relations': [
                    {
                        'object_type': 'group',
                        'object_id': self.groups[0]['id'],
                    }
                ]
            })
        )
        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)

        assert_that(
            response_data,
            has_entries(
                code='schema_validation_error',
            )
        )
        error_message = "'name' is a required property"
        self.assertTrue(
            response_data['params']['errors'][0]['message'].startswith(error_message)
        )

    def test_with_relations_with_object_without_type(self):
        response = self.client.post(
            'resources/',
            headers=self.auth_header,
            content_type='application/json',
            data=json.dumps({
                'relations': [
                    {
                        'name': 'read',
                        'object_id': self.groups[0]['id'],
                    }
                ]
            })
        )
        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)

        assert_that(
            response_data,
            has_entries(
                code='schema_validation_error',
            )
        )
        error_message = "'object_type' is a required property"
        self.assertTrue(
            response_data['params']['errors'][0]['message'].startswith(error_message)
        )

    def test_with_relations_with_bad_object(self):
        response = self.client.post(
            'resources/',
            headers=self.auth_header,
            content_type='application/json',
            data=json.dumps({
                'relations': {
                    'one': 'hola',
                    'two': 'amigo'
                }
            })
        )
        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)

        assert_that(
            response_data,
            has_entries(
                code='schema_validation_error',
            )
        )

    def test_post_dismiss_user(self):
        # запрет на создание ресурса с уволенным сотрудником
        dismissed_user = self.create_user()
        UserModel(self.main_connection).dismiss(
            org_id=dismissed_user['org_id'],
            user_id=dismissed_user['id'],
            author_id=dismissed_user['id'],
        )

        self.post_json(
            '/resources/',
            {
                "relations":
                    [
                        {
                            'name': 'write',
                            'object_type': 'user',
                            'object_id': dismissed_user['id']
                        }
                    ]
            },
            headers=self.auth_header,
            expected_code=422
        )


class TestResourceDetail__get(TestResourceBase):
    def test_existing_resource(self):
        resource = ResourceModel(self.main_connection).create(
            id=str(ObjectId()),
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ],
            external_id=str(ObjectId()),
        )
        ResourceModel(self.main_connection).create(
            id=resource['external_id'],
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ],
            external_id=str(ObjectId()),
        )
        response_data = self.get_json(
            '/resources/%s/' % resource['external_id'],
            headers=self.auth_header,
        )
        expected_object = {
            'id': self.user['id'],
            'nickname': self.user['nickname'],
            'name': self.user['name'],
            'gender': self.user['gender'],
            'department_id': self.user['department_id'],
            'position': self.user['position'],
            'avatar_id': None
        }
        self.assertEqual(response_data['id'], resource['external_id'])
        self.assertEqual(response_data['service'], 'autotest')
        self.assertEqual(len(response_data['relations']), 1)
        self.assertEqual(response_data['relations'][0]['object'], expected_object)
        self.assertEqual(response_data['relations'][0]['name'], 'read')
        self.assertEqual(response_data['relations'][0]['object_type'], 'user')
        errors = validate_data_by_schema(
            response_data,
            schema=RESOURCE_OUT_SCHEMA
        )
        assert_that(errors, equal_to([]))

    def test_existing_resource_relations_with_another_service(self):
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                },
            ]
        )

        self.get_json(
            '/resources/%s/relations/' % resource['external_id'],
            expected_code=200,
            headers=self.auth_header,
            query_string={
                'service': 'autotest',
            }
        )

        self.get_json(
            '/resources/%s/relations/' % resource['external_id'],
            headers=get_auth_headers(as_uid=self.user['id']),
            expected_code=404,
            query_string={
                'service': 'autotest2',
            }
        )

    def test_existing_resource_relations_with_another_organization(self):
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='my_org',
            domain_part='yabiz.ru',
        )
        user = self.create_user(org_id=organization['organization']['id'])
        resource = ResourceModel(self.main_connection).create(
            org_id=organization['organization']['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': user['id'],
                },
            ]
        )

        experiments = [
            {
                'headers': get_auth_headers(as_org=self.organization['id']),
                'expected_code': 403,
            },
            {
                'headers': get_auth_headers(as_uid=user['id']),
                'expected_code': 200,
            },
        ]

        for experiment in experiments:
            self.get_json(
                '/resources/%s/relations/' % resource['external_id'],
                expected_code=experiment['expected_code'],
                headers=experiment['headers'],
            )

    def test_not_existing_resource(self):
        response_data = self.get_json(
            '/resources/not-existing-id/',
            headers=self.auth_header,
            expected_code=404,
        )
        assert_that_not_found(response_data)


class TestResourceDetailRelations__get(TestResourceBase):

    def existing_resource(self, headers, slug):
        # вспомогательный метод для test_existing_resource и test_existing_resource_by_oauth
        # получаем отношения для ресурса
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=slug,
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )
        response_data = self.get_json(
            '/resources/%s/relations/' % resource['external_id'],
            headers=headers
        )
        assert_that(
            response_data,
            contains(
                has_entries(
                    object_type='user',
                    name='read',
                    object=has_entries(
                        id=self.user['id']
                    )
                )
            )
        )

    def test_existing_resource(self):
        # получаем отношения для ресурса при авторизации по токену
        self.existing_resource(self.auth_header, 'autotest')

    def test_not_existing_resource(self):
        response_data = self.get_json(
            '/resources/not-existing-id/relations/',
            headers=self.auth_header,
            expected_code=404,
        )
        assert_that_not_found(response_data)

    def test_existing_resource_with_another_service(self):
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )

        self.get_json(
            '/resources/%s/relations/' % resource['external_id'],
            headers=self.auth_header,
            expected_code=200,
            query_string={
                'service': 'autotest'
            }
        )
        service = create_service('autotests2')
        for uri in ['resources/%s/', 'resources/%s/relations/']:
            self.get_json(
                uri % resource['external_id'],
                headers=self.auth_header,
                expected_code=404,
                query_string={
                    'service': service['identity']
                }
            )

    def test_filter_relations_correct(self):
        service_slug = 'metrika'
        service = ServiceModel(self.meta_connection).create(
            slug=service_slug,
            name='Metrika',
            client_id='client_id_metrika_some',
        )

        OrganizationServiceModel(self.main_connection).create(
            self.organization['id'],
            service['id'],
            ready=True,
        )

        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service_slug,
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                },
                {
                    'name': 'own',
                    'user_id': self.user['id'],
                },
            ]
        )

        response = self.get_json(
            '/resources/%s/relations/?service=metrika' % resource['external_id'],
            expected_code=200,
            headers=self.auth_header
        )
        self.assertEqual(len(response), 1)
        assert_that(
            response,
            contains(
                has_entries(
                    name='read',
                    object_type='user',
                )
            )
        )

    def test_not_existing_resource_with_another_resources_external_id(self):
        # нужно проверить, что если мы создадим ресурс с id = external_id другого ресурса,
        # в выдаче API мы его не увидим
        resource = ResourceModel(self.main_connection).create(
            id=str(ObjectId()),
            org_id=self.organization['id'],
            service='autotest',
            relations=[],
            external_id=str(ObjectId()),
        )
        ResourceModel(self.main_connection).create(
            id=resource['external_id'],
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ],
            external_id=str(ObjectId()),
        )

        # при запросе ресурса с id = resource['id'] мы должны получить 404, т.к. ресурсы отдаются по external_id
        self.get_json(
            '/resources/%s/relations/' % resource['id'],
            headers=self.auth_header,
            expected_code=404
        )

        response = self.get_json(
            '/resources/%s/relations/' % resource['external_id'],
            headers=self.auth_header,
            expected_code=200
        )
        assert_that(response, equal_to([]))

    def test_existing_resource_with_another_organization(self):
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='my_org',
            domain_part='yabiz.ru',
        )
        user = self.create_user(org_id=organization['organization']['id'])
        resource = ResourceModel(self.main_connection).create(
            org_id=organization['organization']['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': user['id'],
                },
            ]
        )

        experiments = [
            {
                'headers': get_auth_headers(as_org=self.organization['id']),
                'expected_code': 404,
            },
            {
                'headers': get_auth_headers(as_uid=user['id']),
                'expected_code': 200,
            },
            {
                'headers': get_auth_headers(as_org=organization['organization']['id']),
                'expected_code': 200,
            },
        ]

        for experiment in experiments:
            self.get_json(
                '/resources/%s/' % resource['external_id'],
                expected_code=experiment['expected_code'],
                headers=experiment['headers'],
            )


class TestResourceDetailRelations__put(TestResourceBase):

    def existing_resource(self, headers, slug):
        # вспомогательный метод для test_existing_resource и test_existing_resource_by_oauth
        # обновляем отношения для ресурса

        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            }
        )

        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=slug,
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )

        response = self.put_json(
            '/resources/%s/relations/' % resource['external_id'],
            [
                {
                    'name': 'write',
                    'object_type': 'group',
                    'object_id': group['id']
                }
            ],
            headers=headers,
        )

        assert_that(
            response,
            contains(
                has_entries(
                    name='write',
                    object_type='group',
                    object=has_entries(
                        id=group['id'],
                        name=group['name'],
                        type=group['type'],
                    )
                )
            )
        )

        assert_that(
            ActionModel(self.main_connection).filter(org_id=self.organization['id'], name=action.resource_modify).count(),
            equal_to(1)
        )

        assert_that(
            EventModel(self.main_connection).filter(org_id=self.organization['id'],
                                                    name=event.resource_modified).count(),
            equal_to(1)
        )

    def test_existing_resource(self):
        # обновляем отношения для ресурса при авторизации по токену
        self.existing_resource(self.auth_header, 'autotest')

    def test_put_should_update_resource_by_external_id(self):
        # проверяем, что при обновлении ресурса мы действительно обновим ресурс по external_id, а не по id
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            }
        )

        # создадим два ресурса
        resource = ResourceModel(self.main_connection).create(
            id=str(ObjectId()),
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ],
            external_id=str(ObjectId()),
        )

        # второму ресурсу в качестве id укажем external_id первого
        second_resource = ResourceModel(self.main_connection).create(
            id=resource['external_id'],
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ],
            external_id=str(ObjectId()),
        )

        # проверим, что обновится первый ресурс
        response = self.put_json(
            '/resources/%s/relations/' % resource['external_id'],
            data=[
                {
                    'name': 'write',
                    'object_type': 'group',
                    'object_id': group['id']
                }
            ],
            headers=self.auth_header,
        )

        relations = ResourceRelationModel(self.main_connection).find(filter_data={'resource_id': resource['id']})
        assert_that(len(relations), equal_to(1))

        expected = [
            {
                'object': {
                    'id': group['id'],
                    'name': group['name'],
                    'type': group['type'],
                    'members_count': group['members_count'],
                },
                'name': 'write',
                'object_type': 'group',
                'id': relations[0]['id'],
            }
        ]

        assert_that(response, equal_to(expected))

        # проверим, что второй ресурс не обновился
        response = self.get_json(
            '/resources/%s/relations/' % second_resource['external_id'],
            headers=self.auth_header,
        )

        assert_that(
            response,
            contains(
                has_entries(
                    name='read',
                    object_type='user',
                    object=has_entries(
                        id=self.user['id'],
                        nickname=self.user['nickname'],
                        name=self.user['name'],
                        gender='male',
                        department_id=self.user['department_id'],
                        position=self.user['position'],
                    )
                )
            )
        )

    def test_should_be_allowed_to_erase_relations(self):
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )

        response = self.client.put(
            '/resources/%s/relations/' % resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=json.dumps([])
        )
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        self.assertEqual(response_data, [])

    def test_invalid_data(self):
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )

        response = self.client.put(
            '/resources/%s/relations/' % resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=json.dumps([
                {
                    'object_type': 'user',
                    'name': 'write'
                }
            ])
        )

        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)


        assert_that(
            response_data,
            has_entries(
                code='schema_validation_error',
            )
        )
        error_message = '\'object_id\' is a required property'
        self.assertTrue(
            response_data['params']['errors'][0]['message'].startswith(error_message)
        )

    def test_not_existing_resource(self):
        # Проверим, что по несуществующему resource_id ресурс создается, а существующие relations связываются с ним
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            }
        )
        not_exist_external_id = 'not-existing-id'
        response = self.client.put(
            '/resources/%s/relations/' % not_exist_external_id,
            headers=self.auth_header,
            content_type='application/json',
            data=json.dumps([
                {
                    'object_type': 'group',
                    'object_id': group['id'],
                    'name': 'write'
                }
            ])
        )

        self.assertEqual(response.status_code, 200)
        # тут поправить проверку про external_id: https://st.yandex-team.ru/DIR-2527
        inner_resource_id = self.get_json(
            '/resources/%s/relations/' % not_exist_external_id,
            headers=self.auth_header,
        )[0]['id']
        expected = [
            {
                'name': 'write',
                'object_type': 'group',
                'id': inner_resource_id,
                'object': {
                    'id': group['id'],
                    'name': group['name'],
                    'type': 'generic',
                    'members_count': group['members_count'],
                },
            },
        ]
        response_data = json.loads(response.data)
        assert_that(response_data, equal_to(expected))

    def test_not_existing_relations(self):
        # Проверим, что по не существующим relations - метод отдает ошибку.

        not_exist_external_id = 'not-existing-id'
        response = self.client.put(
            '/resources/%s/relations/' % not_exist_external_id,
            headers=self.auth_header,
            content_type='application/json',
            data=json.dumps([
                {
                    'object_type': 'group',
                    'object_id': 234123,  # не существующий id группы
                    'name': 'read'
                }
            ])
        )

        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)
        assert_that(
            response_data,
            has_entries(
                code='constraint_validation.objects_not_found',
                message='Some objects of type "{type}" were not found in database',
                params={'type': 'group'},
            )
        )

    def test_dismiss_user(self):
        # запрет на обновление с уволенным сотрудником
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )

        dismissed_user = self.create_user()
        UserModel(self.main_connection).dismiss(
            org_id=dismissed_user['org_id'],
            user_id=dismissed_user['id'],
            author_id=dismissed_user['id'],
        )

        self.put_json(
            '/resources/%s/relations/' % resource['external_id'],
            [
                {
                    'name': 'write',
                    'object_type': 'user',
                    'object_id': dismissed_user['id']
                }
            ],
            headers=self.auth_header,
            expected_code=422
        )


class TestResourceBulkUpdateRelations__post(TestResourceBase):
    def setUp(self):
        super(TestResourceBulkUpdateRelations__post, self).setUp()

        self.resource = ResourceModel(self.main_connection).create(
            id=str(ObjectId()),
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ],
            external_id=str(ObjectId()),
        )

        # Создадим еще один ресурс с id=external_id предыдущего,
        # чтобы в тестах проверять, что ресурс изменяется по external_id.
        # Все тесты этого TestCase должны обновлять связи объекта self.resource,
        # а не этого дополнительного ресурса.
        ResourceModel(self.main_connection).create(
            id=self.resource['external_id'],
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ],
            external_id=str(ObjectId()),
        )

    def test_post_no_data(self):
        data = json.dumps({})
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 422)

    def test_post_invalid_params(self):
        data = json.dumps({
            'npoperations': [{}]})
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 422)

    def test_post_add_operation(self):
        admin_group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={'ru': 'admins'}
        )
        department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        new_user_relation = {
            'object_id': self.user['id'],
            'object_type': 'user',
            'name': 'write'
        }
        new_group_relation = {
            'object_id': admin_group['id'],
            'object_type': 'group',
            'name': 'member'
        }
        new_department_relation = {
            'object_id': department['id'],
            'object_type': 'department',
            'name': 'write'
        }
        data = json.dumps({
            'operations': [
                {'operation_type': 'add',
                 'value': new_user_relation, },
                {'operation_type': 'add',
                 'value': new_group_relation, },
                {'operation_type': 'add',
                 'value': new_department_relation, },
            ]
        })
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        self.assertEqual(len(response_data), 4)

        assert_that(
            ActionModel(self.main_connection).filter(org_id=self.organization['id'], name=action.resource_modify).count(),
            equal_to(1)
        )

    def test_post_add_invalid_data(self):
        random_id = 995
        # пробуем добавить пользователя не из этой организации
        new_user_relation = {
            'object_id': random_id,
            'object_type': 'user',
            'name': 'write'
        }
        data = json.dumps({
            'operations': [
                {'operation_type': 'add',
                 'value': new_user_relation, },
            ]
        })
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 422)

    def test_post_delete_operation(self):
        relation_for_delete = {
            'object_id': self.user['id'],
            'object_type': 'user',
            'name': 'read'
        }
        data = json.dumps({
            'operations': [
                {
                    'operation_type': 'delete',
                    'value': relation_for_delete,
                }
            ]
        })
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        self.assertEqual(response_data, [])

        assert_that(
            ActionModel(self.main_connection).filter(org_id=self.organization['id'], name=action.resource_modify).count(),
            equal_to(1)
        )

    def test_post_remove_operation(self):
        relation_for_remove = {
            'object_id': self.user['id'],
            'object_type': 'user',
            'name': 'read'
        }
        data = json.dumps({
            'operations': [
                {
                    'operation_type': 'remove',
                    'value': relation_for_remove,
                }
            ]
        })
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        self.assertEqual(response_data, [])

    def test_post_duplicate(self):
        admin_group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={'ru': 'admins'}
        )
        department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        new_user_relation = {
            'object_id': self.user['id'],
            'object_type': 'user',
            'name': 'write'
        }
        new_group_relation = {
            'object_id': admin_group['id'],
            'object_type': 'group',
            'name': 'member'
        }
        new_department_relation = {
            'object_id': department['id'],
            'object_type': 'department',
            'name': 'write'
        }
        data = json.dumps({
            'operations': [
                {'operation_type': 'add',
                 'value': new_user_relation, },
                {'operation_type': 'add',
                 'value': new_group_relation, },
                {'operation_type': 'add',
                 'value': new_department_relation, },
            ]
        })
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        self.assertEqual(len(response_data), 4)

        # запрашиваем с теми же данными
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        # тут поправить проверку про external_id: https://st.yandex-team.ru/DIR-2527
        self.assertEqual(len(response_data), 4)

    def test_post_invalid_data(self):
        unknown_id = 995
        # пробуем добавить пользователя не из этой организации
        new_user_relation = {
            'object_id': unknown_id,
            'object_type': 'user',
            'name': 'write'
        }
        data = json.dumps({
            'operations': [
                {'operation_type': 'delete',
                 'value': new_user_relation, },
            ]
        })
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)
        assert_that(
            response_data,
            has_entries(
                code='is_not_member',
                message='The {type} with id {id} is not a member of the organization',
                params={'type': 'user', 'id': unknown_id},
            )
        )

    def test_post_dismiss_user(self):
        # запрет на обновление с уволенным сотрудником
        dismissed_user = self.create_user()
        UserModel(self.main_connection).dismiss(
            org_id=dismissed_user['org_id'],
            user_id=dismissed_user['id'],
            author_id=dismissed_user['id'],
        )

        data = {
            'operations': [
                {'operation_type': 'add',
                 'value':
                     {
                         'name': 'write',
                         'object_type': 'user',
                         'object_id': dismissed_user['id']
                     }

                 }
            ]
        }
        self.post_json(
            '/resources/%s/relations/bulk-update/' % self.resource['external_id'],
            data=data,
            headers=self.auth_header,
            expected_code=422
        )

    def test_post_not_exist_resource(self):
        # Проверяем, что ресурсы с несуществующим resource_id - будут обновлены
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={'ru': 'group'}
        )
        department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        new_user_relation = {
            'object_id': self.user['id'],
            'object_type': 'user',
            'name': 'write'
        }
        new_group_relation = {
            'object_id': group['id'],
            'object_type': 'group',
            'name': 'member'
        }
        new_department_relation = {
            'object_id': department['id'],
            'object_type': 'department',
            'name': 'write'
        }
        data = json.dumps({
            'operations': [
                {'operation_type': 'add',
                 'value': new_user_relation,},
                {'operation_type': 'add',
                 'value': new_group_relation,},
                {'operation_type': 'add',
                 'value': new_department_relation,},
            ]
        })
        not_exist_external_id = 'fid:shared:folder'
        response = self.client.post(
            '/resources/%s/relations/bulk-update/' % not_exist_external_id,
            headers=self.auth_header,
            content_type='application/json',
            data=data,
        )
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        # тут поправить проверку про external_id: https://st.yandex-team.ru/DIR-2527
        self.assertEqual(len(response_data), 3)


class TestResourceCount(TestCase):
    def test_counter_for_separate_orgs(self):
        # Проверяем, что фильтр по сервису работает
        org1_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-org1'
        )['organization']['id']

        def create(service, org_id):
            ResourceModel(self.main_connection).create(
                org_id=org_id,
                service=service,
            )

        for i in range(10):
            create('foo', self.organization['id'])

        for i in range(20):
            create('foo', org1_id)

        for i in range(15):
            create('bar', self.organization['id'])

        response = self.get_json('/resources/count/?service=foo')

        assert_that(
            response,
            has_entries(count=10)
        )

    def test_422_error_if_no_service_slug(self):
        # Если сервис не задан, то возвращаем 422 ошибку потому что поле - обязательное
        response = self.get_json(
            '/resources/count/',
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(code='required_fields_missed')
        )


class TestResourceMetadataMetrika__get(TestCase):

    def setUp(self):
        super(TestResourceMetadataMetrika__get, self).setUp()
        tvm.tickets['metrika'] = 'ticket-2000269'

        service_slug = 'metrika'
        service = ServiceModel(self.meta_connection).create(
            slug=service_slug,
            name='Metrika',
            client_id='client_id_metrika',
        )

        OrganizationServiceModel(self.main_connection).create(
            self.organization['id'],
            service['id'],
            ready=True,
        )

        self.resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service_slug,
            relations=[
                {
                    'name': 'own',
                    'user_id': self.user['id'],
                }
            ],
            external_id='metrika_counter_id',
        )

        self.resource_1 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service_slug,
            relations=[
                {
                    'name': 'own',
                    'user_id': self.user['id'],
                }
            ],
            external_id='metrika_counter_id2',
        )

    @responses.activate
    def test_return_metadata_success(self):
        responses.add(
            responses.GET,
            'https://internalapi.test.metrika.yandex.net/connect/get_counters_data?ids=metrika_counter_id%2Cmetrika_counter_id2',
            json={
                'metrika_counter_id': {'name': 'counter_1', 'has_monetization': False},
                'metrika_counter_id2': {'name': 'smth', 'site': 'test.ru', }
            },
        )
        response_data = self.get_json('/resources/?service=metrika')

        assert_that(
            response_data,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.resource['external_id'],
                        metadata={'name': 'counter_1', },
                    ),
                    has_entries(
                        id=self.resource_1['external_id'],
                        metadata={'name': 'smth', 'site': 'test.ru', 'description': 'test.ru', },
                    )
                )
            )
        )

    @responses.activate
    def test_return_metadata_fail(self):
        responses.add(
            responses.GET,
            'https://internalapi.test.metrika.yandex.net/connect/get_counters_data?ids=metrika_counter_id%2Cmetrika_counter_id2',
            status=500,
        )
        response_data = self.get_json('/resources/?service=metrika')

        assert_that(
            response_data,
            has_entries(
                result=[],
            )
        )

    @responses.activate
    def test_return_metadata_detail_success(self):
        responses.add(
            responses.GET,
            'https://internalapi.test.metrika.yandex.net/connect/get_counters_data?ids=metrika_counter_id',
            json={
                'metrika_counter_id': {'name': 'counter_1', 'has_monetization': False},
            },
        )
        response_data = self.get_json('/resources/%s/?service=metrika' % self.resource['external_id'])

        assert_that(
            response_data,
            has_entries(
                id=self.resource['external_id'],
                metadata={'name': 'counter_1', },
            ),
        )

    @responses.activate
    def test_return_metadata_no_data_success(self):
        responses.add(
            responses.GET,
            'https://internalapi.test.metrika.yandex.net/connect/get_counters_data?ids=metrika_counter_id%2Cmetrika_counter_id2',
            json={
                'metrika_counter_id': {'name': 'counter_1', 'has_monetization': False},
            },
        )
        response_data = self.get_json('/resources/?service=metrika')

        assert_that(
            response_data,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.resource['external_id'],
                        metadata={'name': 'counter_1', },
                    ),
                )
            )
        )

    @responses.activate
    def test_return_metadata_detail_fail(self):
        responses.add(
            responses.GET,
            'https://internalapi.test.metrika.yandex.net/connect/get_counters_data?ids=metrika_counter_id',
            status=500,
        )
        response_data = self.get_json(
            '/resources/%s/?service=metrik' % self.resource['external_id'],
            expected_code=404,
        )


class TestResourceMetadataSprav__get(TestCase):

    def setUp(self):
        super(TestResourceMetadataSprav__get, self).setUp()
        tvm.tickets['yandexsprav'] = 'ticket-2011748'

        service_slug = 'yandexsprav'
        service = ServiceModel(self.meta_connection).create(
            slug=service_slug,
            name='yandexsprav',
            client_id='client_id_yandexsprav',
        )

        OrganizationServiceModel(self.main_connection).create(
            self.organization['id'],
            service['id'],
            ready=True,
        )
        self.tvm2_service = Service(
            id=service['id'],
            name=service['name'],
            identity=service['slug'],
            is_internal=True,
            ip='127.0.0.1',
        )

        self.resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service_slug,
            relations=[
                {
                    'name': 'own',
                    'user_id': self.user['id']
                }
            ],
            external_id='yandexsprav_id'
        )

        self.resource_1 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service_slug,
            relations=[
                {
                    'name': 'own',
                    'user_id': self.user['id']
                }
            ],
            external_id='yandexsprav_id2'
        )

    def make_get_request(self, url):
        with tvm2_auth(
                self.user['id'],
                scopes=[scope.work_on_behalf_of_any_user, scope.write_resources, scope.read_service_resources],
                org_id=self.organization['id'],
                service=self.tvm2_service,
                user_ticket='some',
        ):
            return self.get_json(url)

    @responses.activate
    def test_return_metadata_success(self):
        responses.add(
            responses.POST,
            'http://sprav-api-test.yandex.net/v1/companies/search?limit=2',
            json={
                'companies': [
                    {'id': 'yandexsprav_id', 'names': [
                        {'type': 'main', 'value': {'value': 'somename'}},
                    ]},
                    {'id': 'yandexsprav_id2', 'urls': [
                        {'type': 'minor', 'value': 'some_minor_url'},
                        {'type': 'main', 'value': 'some_main_url'}
                    ],
                     'names': [
                         {'type': 'minor', 'value': {'value': 'some_other_name'}}
                     ]
                     },
                ]
            },
        )

        response_data = self.make_get_request('/resources/?service=yandexsprav')

        assert_that(
            response_data,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.resource['external_id'],
                        metadata={'name': 'somename'},
                    ),
                    has_entries(
                        id=self.resource_1['external_id'],
                        metadata={'description': 'some_main_url'},
                    )
                )
            )
        )

    @responses.activate
    def test_return_metadata_fail(self):
        responses.add(
            responses.POST,
            'http://sprav-api-test.yandex.net/v1/companies/search?limit=2',
            status=500,
        )
        response_data = self.make_get_request('/resources/?service=yandexsprav')

        assert_that(
            response_data,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.resource['external_id'],
                        metadata={},
                    ),
                    has_entries(
                        id=self.resource_1['external_id'],
                        metadata={},
                    )
                )
            )
        )

    @responses.activate
    def test_return_metadata_detail_success(self):
        responses.add(
            responses.POST,
            'http://sprav-api-test.yandex.net/v1/companies/search?limit=1',
            json={'companies':
                [
                    {
                        'id': 'yandexsprav_id',
                        'names': [
                            {'type': 'main', 'value': {'value': 'somename'}},
                        ],
                        'urls': [
                            {'type': 'main', 'value': 'some_host'}
                        ]
                    },
                ]
            }
        )
        response_data = self.make_get_request('/resources/%s/?service=yandexsprav' % self.resource['external_id'])

        assert_that(
            response_data,
            has_entries(
                id=self.resource['external_id'],
                metadata={'name': 'somename', 'description': 'some_host'},
            ),
        )


class TestResource__delete(TestCase):

    def setUp(self):
        super(TestResource__delete, self).setUp()
        self.resource = ResourceModel(self.main_connection).create(
            external_id='test_id',
            org_id=self.organization['id'],
            service='metrika',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )
        metrika_service = ServiceModel(self.meta_connection).create(
            slug='metrika',
            name='Metrika',
            client_id='SDdSDdsfdfafasdfd',
        )
        OrganizationServiceModel(self.main_connection).create(
            self.organization['id'], metrika_service['id'], True, self.user['id'])
        direct_service = ServiceModel(self.meta_connection).create(
            slug='direct',
            name='Direct',
            client_id='kjkasjdkakdsadsf',
        )
        OrganizationServiceModel(self.main_connection).create(
            self.organization['id'], direct_service['id'], True, self.user['id'])

    def test_delete_success(self):
        self.delete_json('/resources/%s/?service=metrika' % self.resource['external_id'])

        resources_count = ResourceModel(self.main_connection).filter(
            org_id=self.organization['id'],
            service='autotest',
        ).count()
        self.assertEqual(resources_count, 0)

        relations_count = ResourceRelationModel(self.main_connection).filter(
            org_id=self.organization['id'],
            resource_id=self.resource['id']
        ).count()
        self.assertEqual(relations_count, 0)

        events = EventModel(self.main_connection).find(
            {
                'org_id': self.organization['id'],
                'name': 'resource_deleted',
            },
        )
        assert_that(
            events,
            contains(
                has_entries(
                    object=has_entries(
                        external_id=self.resource['external_id'],
                    )
                )
            )
        )

    def test_delete_fail_wrong_id(self):
        self.delete_json('/resources/%s/' % 'testt', expected_code=404)

        resources_count = ResourceModel(self.main_connection).filter(
            org_id=self.organization['id'],
            service='metrika',
        ).count()
        self.assertEqual(resources_count, 1)

    @tvm2_auth_success(100700, scopes=[scope.write_resources])
    def test_delete_fail_no_permissions(self):
        resource = ResourceModel(self.main_connection).create(
            external_id='test_id_2',
            org_id=self.organization['id'],
            service='metrika',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )
        self.delete_json('/resources/%s/?service=metrika' % resource['external_id'], expected_code=403)

        resources_count = ResourceModel(self.main_connection).filter(
            org_id=self.organization['id'],
            service='metrika',
        ).count()
        self.assertEqual(resources_count, 2)

    def test_delete_success_another_service(self):
        resource = ResourceModel(self.main_connection).create(
            external_id='test_id_2',
            org_id=self.organization['id'],
            service='metrika',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )
        self.delete_json('/resources/%s/?service=metrika' % resource['external_id'])

        resources_count = ResourceModel(self.main_connection).filter(
            org_id=self.organization['id'],
            service='metrika',
        ).count()
        self.assertEqual(resources_count, 1)

    def test_delete_idm_resource_success_as_responsible(self):
        self.mocked_idm_b2b.get_roles.return_value = [{
            'organization': self.organization['id'],
            'id': 'role_id',
        }]
        self.mocked_idm_b2b.revoke_roles.return_value = {"errors": 0, "successes": 1}
        resource = ResourceModel(self.main_connection).create(
            external_id='test_id',
            org_id=self.organization['id'],
            service='direct',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.resources.send_resource_unbind_email') as send_resource_unbind_email:
            self.delete_json('/resources/%s/?service=direct' % resource['external_id'])
            assert_not_called(send_resource_unbind_email)

    def test_delete_idm_resource_success_as_not_responsible(self):
        self.mocked_idm_b2b.get_roles.return_value = [{
            'organization': self.organization['id'],
            'id': 'role_id',
        }]
        self.mocked_idm_b2b.revoke_roles.return_value = {"errors": 0, "successes": 1}
        resource = ResourceModel(self.main_connection).create(
            external_id='test_id',
            org_id=self.organization['id'],
            service='direct',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )

        user = self.create_user(org_id=self.organization['id'], is_outer=True)
        headers = get_auth_headers(user['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.resources.send_resource_unbind_email') as send_resource_unbind_email:
            self.delete_json('/resources/%s/?service=direct' % resource['external_id'], headers=headers)
            assert_called_once(
                send_resource_unbind_email,
                ANY,
                'direct',
                self.organization['id'],
                self.user['email'],
                resource['external_id'],
            )


    def test_delete_idm_resource_fail(self):
        self.mocked_idm_b2b.get_roles.return_value = [{
            'organization': self.organization['id'],
            'id': 'role_id',
        }]
        self.mocked_idm_b2b.revoke_roles.return_value = {"errors": 1, "successes": 0}
        resource = ResourceModel(self.main_connection).create(
            external_id='test_id',
            org_id=self.organization['id'],
            service='direct',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                }
            ]
        )
        self.delete_json(
            '/resources/%s/?service=direct' % resource['external_id'],
            expected_code=424,
        )


class TestResourceMetadataAliceb2b__get(TestCase):
    def setUp(self):
        super(TestResourceMetadataAliceb2b__get, self).setUp()
        tvm.tickets['alice_b2b'] = 'ticket'

        service_slug = 'alice_b2b'
        service = ServiceModel(self.meta_connection).create(
            slug=service_slug,
            name='alice_b2b',
            client_id='client_id_alice_b2b',
        )

        OrganizationServiceModel(self.main_connection).create(
            self.organization['id'],
            service['id'],
            ready=True,
        )
        self.tvm2_service = Service(
            id=service['id'],
            name=service['name'],
            identity=service['slug'],
            is_internal=True,
            ip='127.0.0.1',
        )

        self.resource_1 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service_slug,
            external_id='resource_id_1'
        )

        self.resource_2 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service_slug,
            external_id='resource_id_2'
        )

    @responses.activate
    def test_return_metadata_success(self):
        responses.add(
            responses.GET,
            'https://dialogs.priemka.voicetech.yandex.net/b2b/internal/connect/resources/',
            json={
                "status": "ok",
                "metadata": {
                    'resource_id_1': {'some_field': 'value_1'},
                    'resource_id_2': {'some_field': 'value_2'},
                }
            },
        )
        response_data = self.get_json('/resources/?service=alice_b2b')

        assert_that(
            response_data,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.resource_1['external_id'],
                        metadata={'some_field': 'value_1'},
                    ),
                    has_entries(
                        id=self.resource_2['external_id'],
                        metadata={'some_field': 'value_2'},
                    )
                )
            )
        )

    @responses.activate
    def test_return_metadata_fail(self):
        responses.add(
            responses.GET,
            'https://dialogs.priemka.voicetech.yandex.net/b2b/internal/connect/resources/',
            status=500,
        )
        response_data = self.get_json('/resources/?service=alice_b2b')

        assert_that(
            response_data,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.resource_1['external_id'],
                        metadata={},
                    ),
                    has_entries(
                        id=self.resource_2['external_id'],
                        metadata={},
                    )
                )
            )
        )

    @responses.activate
    def test_return_metadata_detail_success(self):
        responses.add(
            responses.GET,
            'https://dialogs.priemka.voicetech.yandex.net/b2b/internal/connect/resources/',
            json={
                "status": "ok",
                "metadata": {
                    'resource_id_1': {'some_field': 'value_1'},
                    'resource_id_2': {'some_field': 'value_2'},
                }
            },
        )
        response_data = self.get_json('/resources/%s/?service=alice_b2b' % self.resource_1['external_id'])

        assert_that(
            response_data,
            has_entries(
                id=self.resource_1['external_id'],
                metadata={'some_field': 'value_1'},
            ),
        )

    @responses.activate
    def test_return_metadata_no_data_success(self):
        responses.add(
            responses.GET,
            'https://dialogs.priemka.voicetech.yandex.net/b2b/internal/connect/resources/',
            json={
                "status": "ok",
                "metadata": {
                    'resource_id_1': {'some_field': 'value_1'},
                }
            },
        )
        response_data = self.get_json('/resources/?service=alice_b2b')

        assert_that(
            response_data,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.resource_1['external_id'],
                        metadata={'some_field': 'value_1'}
                    ),
                    has_entries(
                        id=self.resource_2['external_id'],
                        metadata={},
                    )
                )
            )
        )

    @responses.activate
    def test_return_metadata_detail_fail(self):
        responses.add(
            responses.GET,
            'https://dialogs.priemka.voicetech.yandex.net/b2b/internal/connect/resources/',
            status=500,
        )
        response_data = self.get_json('/resources/%s/?service=alice_b2b' % self.resource_1['external_id'])

        assert_that(
            response_data,
            has_entries(
                id=self.resource_1['external_id'],
                metadata={},
            ),
        )

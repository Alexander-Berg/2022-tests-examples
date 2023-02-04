# -*- coding: utf-8 -*-
import pytest
from testutils import (
    TestCase,
    create_organization,
)
from unittest import skip

from bson import ObjectId

from intranet.yandex_directory.src.yandex_directory.core.utils import only_fields
from intranet.yandex_directory.src.yandex_directory.core.models.user import (
    UserModel
)
from intranet.yandex_directory.src.yandex_directory.core.models.department import (
    DepartmentModel
)
from intranet.yandex_directory.src.yandex_directory.core.models.resource import (
    ResourceModel,
    ResourceRelationModel
)
from intranet.yandex_directory.src.yandex_directory.core.models.group import GroupModel
from testutils import (
    create_department,
    create_user,
    create_group,
)


class TestResourceModel_create(TestCase):
    def setUp(self):
        super(TestResourceModel_create, self).setUp()

        self.department = create_department(
            self.main_connection,
            name={
                'ru': 'Marketing department'
            },
            org_id=self.organization['id']
        )
        self.group = GroupModel(self.main_connection).create(
            name={
                'ru': 'Admins'
            },
            org_id=self.organization['id']
        )

    def test_simple(self):
        # return value
        resource = ResourceModel(self.main_connection).create(
            service='yamb',
            org_id=self.organization['id']
        )
        self.assertIsNotNone(resource.get('id'))
        self.assertEqual(resource['service'], 'yamb')

        # test data in database
        resource_from_db = ResourceModel(self.main_connection).get(resource['id'])
        self.assertIsNotNone(resource_from_db)
        self.assertEqual(resource_from_db['id'], resource['id'])
        self.assertEqual(resource_from_db['service'], resource['service'])

    def test_id_should_equal_to_external_id(self):
        # Проверим, что если ресурс создается без указания external_id,
        # то он будет равен сгенерированному или переданному id
        resource_without_id = ResourceModel(self.main_connection).create(
            service='yamb',
            org_id=self.organization['id']
        )
        resource_with_id = ResourceModel(self.main_connection).create(
            id=str(ObjectId()),
            service='yamb',
            org_id=self.organization['id']
        )

        for resource in [resource_with_id, resource_without_id]:
            self.assertIsNotNone(resource.get('id'))
            self.assertEqual(resource['service'], 'yamb')
            self.assertEqual(resource['id'], resource['external_id'])

            # test data in database
            resource_from_db = ResourceModel(self.main_connection).get(resource['id'])
            self.assertIsNotNone(resource_from_db)
            self.assertEqual(resource_from_db['id'], resource['id'])
            self.assertEqual(resource_from_db['service'], resource['service'])

    def test_with_user_relation(self):
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'user_id': self.user['id'],
                    'name': 'rwd'
                }
            ]
        )

        self.assertIsNotNone(resource.get('relations'))
        self.assertEqual(len(resource['relations']), 1)
        self.assertEqual(resource['relations'][0]['name'], 'rwd')
        self.assertEqual(resource['relations'][0]['user_id'], self.user['id'])

        # test data in database
        resource_from_db = ResourceModel(self.main_connection).get(
            resource['id'],
            fields=[
                'relations.name',
                'relations.user.*',
            ]
        )

        self.assertIsNotNone(resource_from_db)
        self.assertEqual(len(resource_from_db['relations']), 1)
        relation = resource_from_db['relations'][0]
        self.assertEqual(relation['name'], 'rwd')

        expected_user = {
            'department_id': self.user['department_id'],
            'gender': self.user['gender'],
            'id': self.user['id'],
            'nickname': self.user['nickname'],
            'name': self.user['name'],
            'position': self.user['position'],
        }
        self.assertEqual(relation['user'], expected_user)

    def test_with_department_relation(self):
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'department_id': self.department['id'],
                    'name': 'rwd'
                }
            ]
        )

        self.assertIsNotNone(resource.get('relations'))
        self.assertEqual(len(resource['relations']), 1)
        self.assertEqual(resource['relations'][0]['name'], 'rwd')
        self.assertEqual(resource['relations'][0]['department_id'], self.department['id'])

        # test data in database
        resource_from_db = ResourceModel(self.main_connection).get(
            resource['id'],
            fields=[
                'relations.name',
                'relations.department.*',
                'relations.user.*',
                'relations.group.*',
            ]
        )

        self.assertIsNotNone(resource_from_db)
        self.assertEqual(len(resource_from_db['relations']), 1)
        relation = resource_from_db['relations'][0]
        self.assertEqual(relation['name'], 'rwd')

        expected_department = {
            'id': self.department['id'],
            'name': self.department['name'],
            'parent_id': None,
            'members_count': self.department['members_count'],
        }
        self.assertEqual(relation['department'], expected_department)

    def test_with_group_relation(self):
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'group_id': self.group['id'],
                    'name': 'rwd'
                }
            ]
        )

        self.assertIsNotNone(resource.get('relations'))
        self.assertEqual(len(resource['relations']), 1)
        self.assertEqual(resource['relations'][0]['name'], 'rwd')
        self.assertEqual(resource['relations'][0]['group_id'], self.group['id'])

        # test data in database
        resource_from_db = ResourceModel(self.main_connection).get(
            resource['id'],
            fields=[
                'relations.name',
                'relations.group.name',
                'relations.group.type',
            ]
        )

        self.assertIsNotNone(resource_from_db)
        self.assertEqual(len(resource_from_db['relations']), 1)
        relation = resource_from_db['relations'][0]
        self.assertEqual(relation['name'], 'rwd')

        expected_group = {
            'id': self.group['id'],
            'name': self.group['name'],
            'type': self.group['type'],
        }
        self.assertEqual(relation['group'], expected_group)


class TestResourceModel_find(TestCase):
    def setUp(self):
        super(TestResourceModel_find, self).setUp()

        self.department = create_department(
            self.main_connection,
            name={'en': 'Marketing department'},
            org_id=self.organization['id']
        )
        self.group = GroupModel(self.main_connection).create(
            name={'ru': 'Admins'},
            org_id=self.organization['id']
        )

        # create resources
        self.resource_yamb = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'department_id': self.department['id'],
                    'name': 'rwd'
                }
            ]
        )
        self.resource_disk = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='disk',
            relations=[
                {
                    'group_id': self.group['id'],
                    'name': 'admin'
                }
            ]
        )
        self.resource_portal = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='portal',
            relations=[
                {
                    'user_id': self.user['id'],
                    'name': 'read'
                }
            ]
        )

    def test_all(self):
        response = ResourceModel(self.main_connection).find()
        response = [item for item in response
                    if item['service'] in ('yamb', 'disk', 'portal')]
        self.assertEqual(len(response), 3)
        expected = [
            {
                'id': self.resource_yamb['id'],
                'external_id': self.resource_yamb['external_id'],
                'service': 'yamb',
                'org_id': self.organization['id']
            },
            {
                'id': self.resource_disk['id'],
                'external_id': self.resource_disk['external_id'],
                'service': 'disk',
                'org_id': self.organization['id']
            },
            {
                'id': self.resource_portal['id'],
                'external_id': self.resource_portal['external_id'],
                'service': 'portal',
                'org_id': self.organization['id']
            },
        ]
        self.assertEqual(
            sorted(response, key=lambda x: x['id']),
            sorted(expected, key=lambda x: x['id'])
        )

    def test_find_by_service(self):
        response = ResourceModel(self.main_connection).find(filter_data={'service': 'yamb'})
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.resource_yamb['id'])

    def test_find_by_resource_id(self):
        response = ResourceModel(self.main_connection).find(filter_data={'id': self.resource_disk['id']})
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.resource_disk['id'])

    def test_find_by_resource_id_list(self):
        ids = [self.resource_yamb['id'], self.resource_disk['id']]
        response = ResourceModel(self.main_connection).find(filter_data={'id': ids})
        self.assertEqual(len(response), 2)
        self.assertEqual(sorted([i['id'] for i in response]), sorted(ids))

    def test_find_by_relation_name(self):
        response = ResourceModel(self.main_connection).find(filter_data={'relation_name': 'rwd'})
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.resource_yamb['id'])

    def test_find_by_relation_name_list(self):
        response = ResourceModel(self.main_connection).find(filter_data={'relation_name': ['rwd', 'admin']})
        self.assertEqual(len(response), 2)
        ids = [self.resource_yamb['id'], self.resource_disk['id']]
        self.assertEqual(sorted([i['id'] for i in response]), sorted(ids))

    def test_find_with_inderect_relation(self):
        # В этом тесте проверяем, что мы можем получить ресурсы, которые достаются
        # департаменту от своего предка.

        # Создаём три департамента, каждый следующий потомок предыдущего
        _department_1 = self.create_department(
            org_id=self.organization['id'],
        )
        _department_2 = self.create_department(
            org_id=self.organization['id'],
            parent_id=_department_1['id'],
        )
        _department_3 = self.create_department(
            org_id=self.organization['id'],
            parent_id=_department_2['id'],
        )

        # создаем еще одну организацию и будем проверять на ней,
        # чтобы в базе были несколько записей с одинаковыми department id
        new_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        department_1 = self.create_department(
            org_id=new_org['id'],
        )
        department_2 = self.create_department(
            org_id=new_org['id'],
            parent_id=department_1['id'],
        )
        department_3 = self.create_department(
            org_id=new_org['id'],
            parent_id=department_2['id'],
        )

        # Создаём ресурс для второго департамента. Доступ к нему должны иметь
        # только второй и третий(так как потомок второго).
        resource = ResourceModel(self.main_connection).create(
            org_id=new_org['id'],
            service='test_service',
            relations=[
                {
                    'department_id': department_2['id'],
                    'name': 'read'
                }
            ]
        )

        dep_1_resources = ResourceModel(self.main_connection).find(
            filter_data={
                'org_id': new_org['id'],
                'department_id': department_1['id'],
            },
            fields=['id']
        )
        dep_2_resources = ResourceModel(self.main_connection).find(
            filter_data={
                'org_id': new_org['id'],
                'department_id': department_2['id'],
            },
            fields=['id']
        )
        dep_3_resources = ResourceModel(self.main_connection).find(
            filter_data={
                'org_id': new_org['id'],
                'department_id': department_3['id'],
            },
            fields=['id']
        )

        self.assertEqual(len(dep_1_resources), 0)

        self.assertEqual(len(dep_2_resources), 1)
        self.assertEqual(dep_2_resources[0]['id'], resource['id'])

        self.assertEqual(len(dep_3_resources), 1)
        self.assertEqual(dep_3_resources[0]['id'], resource['id'])

        # Так как для поиска по id департамента и поиска по id пользователя
        # используются разные запросы, то повторяем тест для пользователей.
        user_1 = self.create_user(
            org_id=new_org['id'],
            department_id=department_1['id']
        )
        user_2 = self.create_user(
            org_id=new_org['id'],
            department_id=department_2['id']
        )
        user_3 = self.create_user(
            org_id=new_org['id'],
            department_id=department_3['id']
        )

        user_1_resources = ResourceModel(self.main_connection).find(
            filter_data={
                'org_id': new_org['id'],
                'user_id': user_1['id'],
            },
            fields=['id']
        )
        user_2_resources = ResourceModel(self.main_connection).find(
            filter_data={
                'org_id': new_org['id'],
                'user_id': user_2['id'],
            },
            fields=['id']
        )
        user_3_resources = ResourceModel(self.main_connection).find(
            filter_data={
                'org_id': new_org['id'],
                'user_id': user_3['id'],
            },
            fields=['id']
        )

        self.assertEqual(len(user_1_resources), 0)

        self.assertEqual(len(user_2_resources), 1)
        self.assertEqual(user_2_resources[0]['id'], resource['id'])

        self.assertEqual(len(user_3_resources), 1)
        self.assertEqual(user_3_resources[0]['id'], resource['id'])


class ResourceRelationsBaseTestMixin(object):
    def setUp(self):
        super(ResourceRelationsBaseTestMixin, self).setUp()

        # departments
        self.departments = {
            'tech': create_department(
                self.main_connection,
                name={'en': 'Technology department'},
                org_id=self.organization['id']
            ),
            'business': create_department(
                self.main_connection,
                name={'en': 'Business department'},
                org_id=self.organization['id']
            ),
            'business_development': create_department(
                self.main_connection,
                name={'en': 'Business development department'},
                org_id=self.organization['id']
            ),
        }

        # groups
        self.groups = {
            'python': create_group(
                self.main_connection,
                name={'en': 'Python group'},
                org_id=self.organization['id']
            ),
            'ruby': create_group(
                self.main_connection,
                name={'en': 'Ruby group'},
                org_id=self.organization['id']
            ),
            'normal_programming_languages': create_group(
                self.main_connection,
                name={'en': 'Golang group'},
                org_id=self.organization['id']
            ),
        }

        # users
        self.users = {
            'maximsa': create_user(
                self.meta_connection,
                self.main_connection,
                user_id='1234',
                nickname='maximsa',
                name={'first': {'ru': 'maximsa'}},
                email='maximsa@ya.ru',
                org_id=self.organization['id']
            ),
            'art': create_user(
                self.meta_connection,
                self.main_connection,
                user_id='2345',
                nickname='art',
                name={'first': {'ru': 'art'}},
                email='art@ya.ru',
                org_id=self.organization['id']
            ),
            'akhmetov': create_user(
                self.meta_connection,
                self.main_connection,
                user_id='3456',
                nickname='akhmetov',
                name={'first': {'ru': 'akhmetov'}},
                email='akhmetov@ya.ru',
                org_id=self.organization['id']
            ),
        }

        # resources
        self.resources = {
            'yamb': ResourceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service='yamb',
            ),
            'disk': ResourceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service='disk',
            ),
            'portal': ResourceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service='portal',
            ),
        }
        self.resources_services = [i['service'] for i in list(self.resources.values())]

    def check_response(self, result, expected):
        self.assertEqual(len(result), len(expected))
        self.assertEqual(
            sorted([only_fields(i, 'id') for i in result], key=lambda x: x['id']),
            sorted([only_fields(i, 'id') for i in expected], key=lambda x: x['id']),
        )

    def check_expectations(self, users, resource, other_user, other_resource):
        raise NotImplementedError()

    def test_users_direct_relation_to_resource(self):
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['yamb']['id'],
            name='write',
            user_id=self.users['akhmetov']['id'],
        )
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['portal']['id'],
            name='read',
            user_id=self.users['art']['id'],
        )
        self.check_expectations(
            users=[self.users['akhmetov']],
            resource=self.resources['yamb'],
            other_user=self.users['art'],
            other_resource=self.resources['portal']
        )

    def test_users_direct_membership_in_department(self):
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['yamb']['id'],
            name='write',
            department_id=self.departments['business_development']['id'],
        )
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['portal']['id'],
            name='read',
            department_id=self.departments['tech']['id'],
        )

        # include users into department
        users = [self.users['maximsa'], self.users['art']]
        for user in users:
            UserModel(self.main_connection).update_one(
                update_data={
                    'department_id': self.departments['business_development']['id']
                },
                filter_data={
                    'id': user['id'],
                    'org_id': self.organization['id']
                }
            )

        # make one of users as member of another department
        UserModel(self.main_connection).update_one(
            update_data={
                'department_id': self.departments['tech']['id']
            },
            filter_data={
                'id': self.users['akhmetov']['id'],
                'org_id': self.organization['id']
            }
        )
        self.check_expectations(
            users=users,
            resource=self.resources['yamb'],
            other_user=self.users['akhmetov'],
            other_resource=self.resources['portal']
        )

    def test_users_relation_through_2_depth_department_membership(self):
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['yamb']['id'],
            name='write',
            department_id=self.departments['business']['id'],
        )
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['portal']['id'],
            name='read',
            department_id=self.departments['tech']['id'],
        )

        # make department as child of another department
        DepartmentModel(self.main_connection).update_one(
            id=self.departments['business_development']['id'],
            org_id=self.organization['id'],
            data={
                'parent_id': self.departments['business']['id']
            }
        )

        # include users into child department
        users = [self.users['maximsa'], self.users['art']]
        for user in users:
            UserModel(self.main_connection).update_one(
                update_data={
                    'department_id': self.departments['business_development']['id']
                },
                filter_data={
                    'id': user['id'],
                    'org_id': self.organization['id']
                }
            )

        # make one of users as member of another department
        UserModel(self.main_connection).update_one(
            update_data={
                'department_id': self.departments['tech']['id']
            },
            filter_data={
                'id': self.users['akhmetov']['id'],
                'org_id': self.organization['id']
            }
        )

        self.check_expectations(
            users=users,
            resource=self.resources['yamb'],
            other_user=self.users['akhmetov'],
            other_resource=self.resources['portal']
        )

    def test_users_direct_membership_in_group(self):
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['yamb']['id'],
            name='write',
            group_id=self.groups['python']['id'],
        )
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['portal']['id'],
            name='read',
            group_id=self.groups['ruby']['id'],
        )

        # include users into group
        users = [self.users['maximsa'], self.users['art']]
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.groups['python']['id'],
            data={
                'members': [
                    {
                        'type': 'user',
                        'id': i['id']
                    }
                    for i in users
                ]
            }
        )

        # make one of users as member of another group
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.groups['ruby']['id'],
            data={
                'members': [
                    {
                        'type': 'user',
                        'id': self.users['akhmetov']['id']
                    }
                ]
            }
        )

        self.check_expectations(
            users=users,
            resource=self.resources['yamb'],
            other_user=self.users['akhmetov'],
            other_resource=self.resources['portal']
        )

    def test_users_relation_through_2_depth_group_membership(self):
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['yamb']['id'],
            name='write',
            group_id=self.groups['normal_programming_languages']['id'],
        )
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resources['portal']['id'],
            name='read',
            group_id=self.groups['ruby']['id'],
        )

        # make Python group as member of Normal Programming Languages group
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.groups['normal_programming_languages']['id'],
            data={
                'members': [
                    {
                        'type': 'group',
                        'id': self.groups['python']['id']
                    }
                ]
            }
        )

        # include users into Python group
        users = [self.users['maximsa'], self.users['art']]
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.groups['python']['id'],
            data={
                'members': [
                    {
                        'type': 'user',
                        'id': i['id']
                    }
                    for i in users
                ]
            }
        )

        # make one of users as member of another (Ruby) group
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.groups['ruby']['id'],
            data={
                'members': [
                    {
                        'type': 'user',
                        'id': self.users['akhmetov']['id']
                    }
                ]
            }
        )

        self.check_expectations(
            users=users,
            resource=self.resources['yamb'],
            other_user=self.users['akhmetov'],
            other_resource=self.resources['portal']
        )


class TestResourceModel_find_by_user_id(ResourceRelationsBaseTestMixin, TestCase):
    def check_expectations(self, users, resource, other_user, other_resource):
        for experiment in [
            {
                'users': users,
                'resource': resource,
            },
            {
                'users': [other_user],
                'resource': other_resource,
            },
        ]:
            for u in experiment['users']:
                response = ResourceModel(self.main_connection).find(
                    filter_data={
                        'user_id': u['id']
                    }
                )
                # для связей пользователей с группами используется resource с отношением 'include'
                # поэтому мы оставляем в выборке только ресурсы, относящиеся к сервисам,
                # которые мы используем тестах
                response = [i for i in response if i['service'] in self.resources_services]
                expected = [experiment['resource']]
                self.check_response(response, expected)


class TestResourceModel_find__complex_test(TestCase):
    """
    Diagram https://jing.yandex-team.ru/files/web-chib/2015-05-06_1706.png
    """
    def setUp(self):
        super(TestResourceModel_find__complex_test, self).setUp()

        self.departments = {}
        self.users = {}
        self.resources = {}
        self.departments['not_yandex'] = create_department(
            self.main_connection,
            name={'en': 'NotYandex'},
            org_id=self.organization['id']
        )
        self.departments['developers'] = create_department(
            self.main_connection,
            name={'en': 'Developers'},
            parent_id=self.departments['not_yandex']['id'],
            org_id=self.organization['id']
        )
        self.departments['python'] = create_department(
            self.main_connection,
            name={'en': 'Python'},
            parent_id=self.departments['developers']['id'],
            org_id=self.organization['id']
        )
        self.users['web-chib'] = self.user
        self.users['art'] = create_user(
            self.meta_connection,
            self.main_connection,
            user_id='1234',
            nickname='art',
            name={'first': {'ru': 'sasha'}},
            email='art@ya.ru',
            department_id=self.departments['python']['id'],
            org_id=self.organization['id']
        )
        self.departments['java'] = create_department(
            self.main_connection,
            name={'en': 'Java'},
            parent_id=self.departments['developers']['id'],
            org_id=self.organization['id']
        )
        self.users['ivan'] = create_user(
            self.meta_connection,
            self.main_connection,
            user_id='2345',
            nickname='ivan',
            name={'first': {'ru': 'ivan'}},
            email='ivan@ya.ru',
            department_id=self.departments['java']['id'],
            org_id=self.organization['id']
        )
        self.departments['designers'] = create_department(
            self.main_connection,
            name={'en': 'Designers'},
            parent_id=self.departments['not_yandex']['id'],
            org_id=self.organization['id']
        )
        self.departments['afisha'] = create_department(
            self.main_connection,
            name={'en': 'Afisha'},
            parent_id=self.departments['designers']['id'],
            org_id=self.organization['id']
        )
        self.users['vova'] = create_user(
            self.meta_connection,
            self.main_connection,
            user_id='3456',
            nickname='vova',
            name={'first': {'ru': 'vova'}},
            email='vova@ya.ru',
            department_id=self.departments['afisha']['id'],
            org_id=self.organization['id']
        )
        self.departments['taxi'] = create_department(
            self.main_connection,
            name={'en': 'Taxi'},
            parent_id=self.departments['designers']['id'],
            org_id=self.organization['id']
        )
        self.users['masha'] = create_user(
            self.meta_connection,
            self.main_connection,
            user_id='4567',
            nickname='masha',
            name={'first': {'ru': 'masha'}},
            email='masha@ya.ru',
            department_id=self.departments['taxi']['id'],
            org_id=self.organization['id']
        )

        # resources
        self.resources['chat for developers'] = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'department_id': self.departments['developers']['id'],
                    'name': 'RWD'
                }
            ]
        )
        self.resources['chat about python'] = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'department_id': self.departments['python']['id'],
                    'name': 'RWD'
                }
            ]
        )
        self.resources['chat how bad java is'] = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'user_id': self.users['web-chib']['id'],
                    'name': 'RWD'
                },
                {
                    'user_id': self.users['art']['id'],
                    'name': 'RWD'
                },
                {
                    'user_id': self.users['ivan']['id'],
                    'name': 'Read'
                },
            ]
        )
        self.resources['chat about java for designers'] = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=[
                {
                    'department_id': self.departments['java']['id'],
                    'name': 'RW'
                },
                {
                    'department_id': self.departments['designers']['id'],
                    'name': 'RW'
                },
                {
                    'user_id': self.users['masha']['id'],
                    'name': 'Admin'
                },
            ]
        )

    def test_all_count(self):
        self.assertEqual(
            len(ResourceModel(self.main_connection).find(filter_data={'service': 'yamb'})),
            4
        )

    def test_rwd_in_chat_for_developers(self):
        response = ResourceModel(self.main_connection).find(
            filter_data={
                'id': self.resources['chat for developers']['id'],
                'relation_name': 'RWD'
            }
        )
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.resources['chat for developers']['id'])

    @pytest.mark.skip('user_id as list is not required')
    def test_all_developers_should_be_able_to_write_to_chat_for_developers(self):
        response = ResourceModel(self.main_connection).find(
            filter_data={
                'id': self.resources['chat for developers']['id'],
                'relation_name': 'RWD',
                'user_id': [
                    self.users['web-chib']['id'],
                    self.users['art']['id'],
                    self.users['ivan']['id'],
                ]
            }
        )
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.resources['chat for developers']['id'])

    @pytest.mark.skip('user_id as list is not required')
    def test_all_python_developers_should_be_able_to_write_to_chat_about_python(self):
        response = ResourceModel(self.main_connection).find(
            filter_data={
                'id': self.resources['chat about python']['id'],
                'relation_name': 'RWD',
                'user_id': [
                    self.users['web-chib']['id'],
                    self.users['art']['id'],
                ]
            }
        )
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.resources['chat about python']['id'])

    def test_java_developer_should_not_be_able_to_write_to_chat_about_python(self):
        response = ResourceModel(self.main_connection).find(
            filter_data={
                'id': self.resources['chat about python']['id'],
                'relation_name': 'RWD',
                'user_id': self.users['ivan']['id']
            }
        )
        self.assertEqual(len(response), 0)

    @pytest.mark.skip('user_id as list is not required')
    def test_web_chib__art__and__ivan__should_be_able_to_read_chat_why_java_is_bad(self):
        response = ResourceModel(self.main_connection).find(
            filter_data={
                'id': self.resources['chat how bad java is']['id'],
                'relation_name': [
                    'RWD',
                    'Read'
                ],
                'user_id': [
                    self.users['web-chib']['id'],
                    self.users['art']['id'],
                    self.users['ivan']['id'],
                ]
            }
        )
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.resources['chat how bad java is']['id'])

    def test_ivan_should_not_be_able_to_write_to_chat_why_java_is_bad(self):
        response = ResourceModel(self.main_connection).find(
            filter_data={
                'id': self.resources['chat how bad java is']['id'],
                'relation_name': 'RWD',
                'user_id': self.users['ivan']['id']
            }
        )
        self.assertEqual(len(response), 0)

    def test_ivan_should_be_able_to_read_chat_why_java_is_bad(self):
        response = ResourceModel(self.main_connection).find(
            filter_data={
                'id': self.resources['chat how bad java is']['id'],
                'relation_name': 'Read',
                'user_id': self.users['ivan']['id']
            }
        )
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.resources['chat how bad java is']['id'])

    def test_ivan_should_be_able_to_read_chat_why_java_is_bad__and_read_chat_about_java_for_designers(self):
        response = ResourceModel(self.main_connection).find(
            filter_data={
                'id': [
                    self.resources['chat how bad java is']['id'],
                    self.resources['chat about java for designers']['id'],
                ],
                'relation_name': [
                    'Read',
                    'RW'
                ],
                'user_id': self.users['ivan']['id']
            }
        )
        self.assertEqual(len(response), 2)
        ids = [
            self.resources['chat how bad java is']['id'],
            self.resources['chat about java for designers']['id'],
        ]
        self.assertEqual(sorted([i['id'] for i in response]), sorted(ids))

    def test_ivan_should_have_relations_with_three_chats(self):
        response = ResourceModel(self.main_connection).find(
            filter_data={
                'user_id': self.users['ivan']['id']
            }
        )
        self.assertEqual(len(response), 3)
        ids = [
            self.resources['chat for developers']['id'],
            self.resources['chat how bad java is']['id'],
            self.resources['chat about java for designers']['id'],
        ]
        self.assertEqual(sorted([i['id'] for i in response]), sorted(ids))

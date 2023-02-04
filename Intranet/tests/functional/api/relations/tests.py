# -*- coding: utf-8 -*-
import json

from unittest.mock import patch

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory.core.utils import only_fields
from intranet.yandex_directory.src.yandex_directory.core.models.resource import (
    ResourceRelationModel,
    ResourceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models import ServiceModel, OrganizationServiceModel
from intranet.yandex_directory.src.yandex_directory.core.models.department import DepartmentModel
from intranet.yandex_directory.src.yandex_directory.core.models.group import GroupModel
from intranet.yandex_directory.src.yandex_directory.core.idm.exceptions import IDMB2BConflictRequestError

from hamcrest import (
    assert_that,
    has_entries,
    equal_to,
)
from testutils import (
    get_auth_headers,
    TestCase,
    assert_that_not_found,
    assert_called_once,
)


def get_id_by_relation_objects(relations_data):
    """
    Получаем id relation-a для каждого из объектов в виде словаря {object_type: relation_id}
    """
    data = {}
    for r in relations_data:
        if r.get('user_id'):
            data['user'] = r.get('id')
        elif r.get('group_id'):
            data['group'] = r.get('id')
        elif r.get('department_id'):
            data['department'] = r.get('id')
    return data


class TestRelationDetail__get(TestCase):
    def setUp(self):
        super(TestRelationDetail__get, self).setUp()

        self.department = DepartmentModel(self.main_connection).create(
            name={'en': 'Test Department'},
            org_id=self.organization['id']
        )
        self.group = GroupModel(self.main_connection).get_or_create_admin_group(
            org_id=self.organization['id'],
        )
        self.resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                },
                {
                    'name': 'write',
                    'department_id': self.department['id'],
                },
                {
                    'name': 'whatever',
                    'group_id': self.group['id'],
                }
            ]
        )
        relations_data = ResourceRelationModel(self.main_connection).find(
            filter_data={
                'resource_id': self.resource['id'],
            },
            fields=['*'],
        )
        self.relation_ids = get_id_by_relation_objects(relations_data)

    def test_not_existing_relation(self):
        not_exist_id_relation = 10000
        response = self.client.get('/relations/%s/' % not_exist_id_relation, headers=get_auth_headers())
        self.assertEqual(response.status_code, 404)
        response_data = json.loads(response.data)
        assert_that_not_found(response_data)

    def test_existing_select_related_user(self):
        data = self.get_json('/relations/%s/' % self.relation_ids['user'])
        expected = {
            "department_id": None,
            "group_id": None,
            "name": "read",
            "org_id": self.organization['id'],
            "id": self.relation_ids['user'],
            "resource_id": self.resource['id'],
            "user_id": self.user['id'],
            "user": only_fields(
                self.user,
                'id',
                'nickname',
                'name',
                'gender',
                'department_id',
                'position',
            )
        }
        assert_that(
            data,
            has_entries(**expected)
        )

    def test_select_related_department(self):
        response = self.client.get('/relations/%s/' % self.relation_ids['department'], headers=get_auth_headers())
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        expected = {
            "department_id": self.department['id'],
            "department": {
                'parent_id': None,
                'id': self.department['id'],
                'name': {'en': 'Test Department'},
                'members_count': self.department['members_count'],
            },
            "group_id": None,
            "name": "write",
            "org_id": self.organization['id'],
            "id": self.relation_ids['department'],
            "resource_id": self.resource['id'],
            "user_id": None,
        }
        assert_that(
            response_data,
            equal_to(expected)
        )

    def test_select_related_group(self):
        response = self.client.get('/relations/%s/' % self.relation_ids['group'], headers=get_auth_headers())
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        expected = {
            "department_id": None,
            "group_id": self.group['id'],
            "group": {
                'id': self.group['id'],
                'type': 'organization_admin',
                'name': {
                    'en': 'Organization administrator',
                    'ru': 'Администратор организации'
                },
                'members_count': 1,
            },
            "name": "whatever",
            "org_id": self.organization['id'],
            "id": self.relation_ids['group'],
            "resource_id": self.resource['id'],
            "user_id": None,
        }
        self.assertEqual(response_data, expected)


class TestRelationDetail__delete(TestCase):
    def setUp(self):
        super(TestRelationDetail__delete, self).setUp()

        service = ServiceModel(self.meta_connection).create(
            slug='metrika',
            name='Metrika',
            client_id='kjkasjdkakdsadsf',
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
            ready=True,
            responsible_id=self.admin_uid
        )

        self.resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='metrika',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                },
            ]
        )
        response = self.client.get('/resources/%s/relations/?service=metrika' % self.resource['id'],
                                   headers=get_auth_headers())
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        self.relation_user_id = response_data[0]['id']
        self.not_admin = self.create_user(org_id=self.organization['id'])

    def test_delete_exist_relation(self):
        self.delete_json(
            '/relations/%s/' % self.relation_user_id,
        )

    def test_delete_exist_relation_with_service_passing(self):
        self.delete_json(
            '/relations/metrika/%s/' % self.relation_user_id,
        )

    def test_delete_exist_relation_no_permissions(self):
        self.delete_json(
            '/relations/%s/' % self.relation_user_id,
            as_uid=self.not_admin['id'],
            expected_code=403,
        )

    def test_delete_exist_relation_with_relation(self):
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=self.resource['id'],
            name='own',
            user_id=self.not_admin['id']
        )

        self.delete_json(
            '/relations/%s/' % self.relation_user_id,
            as_uid=self.not_admin['id'],
            expected_code=204,
        )

    def test_delete_not_exist_relation(self):
        not_exist_id_relation = 10000
        self.delete_json(
            '/relations/%s/' % not_exist_id_relation,
            expected_code=404,
        )

    def test_delete_exist_relation_with_service_slug(self):
        self.delete_json(
            '/relations/%s/%s/' % ('unknown_slug', self.relation_user_id),
        )


class TestRelationDetail__delete_from_b2bidm(TestCase):
    def setUp(self):
        super(TestRelationDetail__delete_from_b2bidm, self).setUp()

        service = ServiceModel(self.meta_connection).create(
            slug='direct',
            name='Direct',
            client_id='kjkasjdkakdsadsf',
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
            ready=True
        )
        self.role_id = 1
        self.not_admin = self.create_user(org_id=self.organization['id'])

    def test_delete_not_exist_relation(self):
        self.mocked_idm_b2b.get_roles.return_value = []
        self.delete_json(
            '/relations/%s/%s/' % ('direct', self.role_id),
            expected_code=403
        )

    def test_delete_roles_from_other_organization(self):
        self.mocked_idm_b2b.get_roles.return_value = [{
            'organization': self.organization['id']
        }, {
            'organization': self.organization['id'] + 1
        }]
        self.delete_json(
            '/relations/%s/%s/' % ('direct', self.role_id),
            expected_code=403
        )

    def test_delete_idm_error(self):
        self.mocked_idm_b2b.get_roles.return_value = [{
            'organization': self.organization['id']
        }]
        self.mocked_idm_b2b.revoke_roles.return_value = {"errors": 1, "successes": 0}
        self.delete_json(
            '/relations/%s/%s/' % ('direct', self.role_id),
            expected_code=424
        )

    def test_delete_exists_role(self):
        self.mocked_idm_b2b.get_roles.return_value = [{
            'organization': self.organization['id']
        }]
        self.mocked_idm_b2b.revoke_roles.return_value = {"errors": 0, "successes": 1}
        self.delete_json(
            '/relations/%s/%s/' % ('direct', self.role_id),
            expected_code=204
        )


class TestRelationDetail__post(TestCase):
    def setUp(self):
        super(TestRelationDetail__post, self).setUp()

        self.resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                },
            ]
        )
        response = self.client.get('/resources/%s/relations/' % self.resource['id'], headers=get_auth_headers())
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        self.relation_user_id = response_data[0]['id']

    def test_create_success(self):
        self.post_json(
            '/relations/',
            data={
                'name': 'owner',
                'object_id': self.user['id'],
                'object_type': 'user',
                'resource_id': self.resource['id'],
                'service': 'autotest',
            },
        )

        self.post_json(
            '/relations/',
            data={
                'name': 'owner',
                'object_id': self.user['id'],
                'object_type': 'user',
                'resource_id': self.resource['id'],
                'service': 'autotest',
            },
            expected_code=409,
        )

    def test_create_fail_other_relation(self):
        # Нельзя выдать доступ в Метрике, если у пользователя есть какой-то выданный тип доступа
        service = ServiceModel(self.meta_connection).create(
            slug='metrika',
            name='Metrika',
            client_id='kjkasjdkakdsadsf1',
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
            ready=True
        )

        self.resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='metrika',
        )

        self.post_json(
            '/relations/',
            data={
                'name': 'view',
                'object_id': self.user['id'],
                'object_type': 'user',
                'resource_id': self.resource['id'],
                'service': 'metrika',
            },
        )

        self.post_json(
            '/relations/',
            data={
                'name': 'edit',
                'object_id': self.user['id'],
                'object_type': 'user',
                'resource_id': self.resource['id'],
                'service': 'metrika',
            },
            expected_code=409,
        )

    def test_create_success_no_service_passed(self):
        self.post_json(
            '/relations/',
            data={
                'name': 'owner',
                'object_id': self.user['id'],
                'object_type': 'user',
                'resource_id': self.resource['id'],
            },
        )

    def test_create_fail_wrong_service(self):
        self.post_json(
            '/relations/',
            data={
                'name': 'owner',
                'object_id': self.user['id'],
                'object_type': 'user',
                'resource_id': self.resource['id'],
                'service': 'metrikasmth',
            },
            expected_code=404,
        )

    def test_create_role_for_direct(self):
        tvm.tickets['idm_b2b'] = 'idm-b2b-ticket'
        with patch.object(app.billing_client, 'get_passport_by_uid') as mocked_get_passport:
            mocked_get_passport.return_value = {'ClientId': self.resource['id']}
            with patch('intranet.yandex_directory.src.yandex_directory.connect_services.idm.base_service.app.idm_b2b_client.request_role') as idm_request_role:
                idm_request_role.return_value = {'id': 123456789}

                uid = 333555777
                path = '/user/chief/'

                self.post_json(
                    '/relations/',
                    data={
                        'name': path,
                        'object_id': uid,
                        'object_type': 'user',
                        'resource_id': self.resource['id'],
                        'service': 'direct',
                    },
                )

                assert_called_once(
                    idm_request_role,
                    org_id=self.organization['id'],
                    author_id=self.user['id'],
                    system='direct',
                    user_type=None,
                    path=path,
                    resource_id=self.resource['id'],
                    uid=uid,
                )

    def test_create_role_for_direct_conflict(self):
        tvm.tickets['idm_b2b'] = 'idm-b2b-ticket'
        with patch.object(app.billing_client, 'get_passport_by_uid') as mocked_get_passport:
            mocked_get_passport.return_value = {'ClientId': self.resource['id']}
            with patch(
                    'intranet.yandex_directory.src.yandex_directory.connect_services.idm.base_service.app.idm_b2b_client.request_role') as idm_request_role:
                idm_request_role.side_effect = IDMB2BConflictRequestError

                uid = 333555777
                path = '/user/chief'

                self.post_json(
                    '/relations/',
                    data={
                        'name': path,
                        'object_id': uid,
                        'object_type': 'user',
                        'resource_id': self.resource['id'],
                        'service': 'direct',
                    },
                    expected_code=409,
                )

                assert_called_once(
                    idm_request_role,
                    org_id=self.organization['id'],
                    author_id=self.user['id'],
                    system='direct',
                    path=path,
                    resource_id=self.resource['id'],
                    uid=uid,
                    user_type=None,
                )

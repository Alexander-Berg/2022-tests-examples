# -*- coding: utf-8 -*-
from collections import defaultdict
from django.conf import settings
from django.test import TestCase, override_settings
from guardian.shortcuts import get_perms, assign_perm

from events.accounts.factories import UserFactory, GroupFactory
from events.idm.utils import get_group_id
from events.idm.views import AddRemoveRoleSerializer
from events.surveyme.factories import SurveyFactory, SurveyGroupFactory
from events.yauth_contrib.helpers import (
    TVMAuthTestCase,
    CookieAuthTestCase,
    HeaderAuthTestCase,
    OAuthTestCase,
)


class TestIdmViewSet_tvm_service(TVMAuthTestCase):
    fixtures = ['initial_data.json']

    def test_should_return_info(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 200)

        roles = response.data['roles']['values']
        self.assertEqual(len(roles), 6)
        self.assertSetEqual(set(roles.keys()), {
            settings.ROLE_SUPERUSER,
            settings.ROLE_SUPPORT,
            settings.ROLE_FORM_MANAGER,
            settings.ROLE_GROUP_MANAGER,
            settings.ROLE_FORM_FILEDOWNLOAD,
            settings.ROLE_GROUP_FILEDOWNLOAD,
        })

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_info_for_biz(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 200)

        roles = response.data['roles']['values']
        self.assertEqual(len(roles), 2)
        self.assertSetEqual(set(roles.keys()), {
            settings.ROLE_SUPERUSER,
            settings.ROLE_SUPPORT,
        })

    def test_shouldnt_return_info(self):
        self.client.set_service_ticket('unknown')

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 401)

        self.client.set_service_ticket(settings.ABC_TVM2_CLIENT)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 401)

    def test_should_add_role_superuser_to_user(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        user = UserFactory(is_superuser=False)
        data = {
            'role': {'role': settings.ROLE_SUPERUSER},
            'uid': user.uid,
        }
        response = self.client.post('/idm/add-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        user.refresh_from_db()
        self.assertTrue(user.is_superuser)

    def test_should_remove_role_superuser_from_user(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        user = UserFactory(is_superuser=True)
        data = {
            'role': {'role': settings.ROLE_SUPERUSER},
            'uid': user.uid,
        }
        response = self.client.post('/idm/remove-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertFalse('data' in response.data)

        user.refresh_from_db()
        self.assertFalse(user.is_superuser)

    def test_should_add_role_support_to_user(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        user = UserFactory(is_staff=False)
        data = {
            'role': {'role': settings.ROLE_SUPPORT},
            'uid': user.uid,
        }
        response = self.client.post('/idm/add-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertFalse('data' in response.data)

        user.refresh_from_db()
        self.assertTrue(user.is_staff)

    def test_should_remove_role_support_from_user(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        user = UserFactory(is_staff=True)
        data = {
            'role': {'role': settings.ROLE_SUPPORT},
            'uid': user.uid,
        }
        response = self.client.post('/idm/remove-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        user.refresh_from_db()
        self.assertFalse(user.is_staff)

    def test_should_add_role_form_manager_to_user(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        user = UserFactory()
        survey = SurveyFactory()
        data = {
            'role': {'role': settings.ROLE_FORM_MANAGER},
            'fields': {'object_pk': str(survey.pk)},
            'uid': user.uid,
        }
        response = self.client.post('/idm/add-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertTrue('data' in response.data)
        self.assertEqual(response.data['data']['object_pk'], str(survey.pk))

        self.assertTrue(settings.ROLE_FORM_MANAGER in get_perms(user, survey))

    def test_should_add_role_form_manager_to_group(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        group = GroupFactory()
        survey = SurveyFactory()
        data = {
            'role': {'role': settings.ROLE_FORM_MANAGER},
            'fields': {'object_pk': str(survey.pk)},
            'group': get_group_id(group.name),
        }
        response = self.client.post('/idm/add-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertTrue('data' in response.data)
        self.assertEqual(response.data['data']['object_pk'], str(survey.pk))

        self.assertTrue(settings.ROLE_FORM_MANAGER in get_perms(group, survey))

    def test_should_add_role_group_manager_to_user(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        user = UserFactory()
        survey_group = SurveyGroupFactory()
        data = {
            'role': {'role': settings.ROLE_GROUP_MANAGER},
            'fields': {'object_pk': str(survey_group.pk)},
            'uid': user.uid,
        }
        response = self.client.post('/idm/add-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertTrue(settings.ROLE_GROUP_MANAGER in get_perms(user, survey_group))

    def test_should_add_role_group_manager_to_group(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        group = GroupFactory()
        survey_group = SurveyGroupFactory()
        data = {
            'role': {'role': settings.ROLE_GROUP_MANAGER},
            'fields': {'object_pk': str(survey_group.pk)},
            'group': get_group_id(group.name),
        }
        response = self.client.post('/idm/add-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertTrue(settings.ROLE_GROUP_MANAGER in get_perms(group, survey_group))

    def test_should_remove_role_form_manager_from_user(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        user = UserFactory()
        survey = SurveyFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, user, survey)
        data = {
            'role': {'role': settings.ROLE_FORM_MANAGER},
            'fields': {'object_pk': str(survey.pk)},
            'uid': user.uid,
        }
        response = self.client.post('/idm/remove-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertFalse(settings.ROLE_FORM_MANAGER in get_perms(user, survey))

    def test_should_remove_role_form_manager_from_group(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        group = GroupFactory()
        survey = SurveyFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, group, survey)
        data = {
            'role': {'role': settings.ROLE_FORM_MANAGER},
            'fields': {'object_pk': str(survey.pk)},
            'group': get_group_id(group.name),
        }
        response = self.client.post('/idm/remove-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertFalse(settings.ROLE_FORM_MANAGER in get_perms(group, survey))

    def test_should_remove_role_group_manager_from_user(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        user = UserFactory()
        survey_group = SurveyGroupFactory()
        assign_perm(settings.ROLE_GROUP_MANAGER, user, survey_group)
        data = {
            'role': {'role': settings.ROLE_GROUP_MANAGER},
            'fields': {'object_pk': str(survey_group.pk)},
            'uid': user.uid,
        }
        response = self.client.post('/idm/remove-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertFalse(settings.ROLE_GROUP_MANAGER in get_perms(user, survey_group))

    def test_should_remove_role_group_manager_from_group(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        group = GroupFactory()
        survey_group = SurveyGroupFactory()
        assign_perm(settings.ROLE_GROUP_MANAGER, group, survey_group)
        data = {
            'role': {'role': settings.ROLE_GROUP_MANAGER},
            'fields': {'object_pk': str(survey_group.pk)},
            'group': get_group_id(group.name),
        }
        response = self.client.post('/idm/remove-role/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertFalse(settings.ROLE_GROUP_MANAGER in get_perms(group, survey_group))

    def test_should_return_roles(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        superuser = UserFactory(is_superuser=True, is_staff=True)
        support = UserFactory(is_staff=True)
        user = UserFactory()
        group = GroupFactory()
        survey = SurveyFactory()
        survey_group = SurveyGroupFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, user, survey)
        assign_perm(settings.ROLE_FORM_MANAGER, group, survey)
        assign_perm(settings.ROLE_GROUP_MANAGER, user, survey_group)
        assign_perm(settings.ROLE_GROUP_MANAGER, group, survey_group)

        response = self.client.get('/idm/get-roles/')
        self.assertEqual(response.status_code, 200)

        roles = defaultdict(list)
        for role in response.data['roles']:
            role_path = role.pop('path')
            roles[role_path].append(dict(role))

        self.assertEqual(len(roles), 1)
        for role_path, _roles in roles.items():
            roles[role_path] = sorted(_roles, key=lambda x: x.get('uid', ''))

        role = f'/role/{settings.ROLE_SUPERUSER}/'
        self.assertListEqual(roles[role], [
            {'uid': superuser.uid},
        ])

        next_url = response.data['next-url']
        self.assertTrue(next_url is not None)

        response = self.client.get(next_url)
        self.assertEqual(response.status_code, 200)

        roles = defaultdict(list)
        for role in response.data['roles']:
            role_path = role.pop('path')
            roles[role_path].append(dict(role))

        self.assertEqual(len(roles), 1)
        for role_path, _roles in roles.items():
            roles[role_path] = sorted(_roles, key=lambda x: x.get('uid', ''))

        role = f'/role/{settings.ROLE_SUPPORT}/'
        self.assertListEqual(roles[role], [
            {'uid': superuser.uid},
            {'uid': support.uid},
        ])

        next_url = response.data['next-url']
        self.assertTrue(next_url is not None)

        response = self.client.get(next_url)
        self.assertEqual(response.status_code, 200)

        roles = defaultdict(list)
        for role in response.data['roles']:
            role_path = role.pop('path')
            roles[role_path].append(dict(role))

        self.assertEqual(len(roles), 2)
        for role_path, _roles in roles.items():
            roles[role_path] = sorted(_roles, key=lambda x: x.get('uid', ''))

        role = f'/role/{settings.ROLE_FORM_MANAGER}/'
        self.assertListEqual(roles[role], [
            {'uid': user.uid, 'fields': {'object_pk': str(survey.pk)}},
        ])
        role = f'/role/{settings.ROLE_GROUP_MANAGER}/'
        self.assertListEqual(roles[role], [
            {'uid': user.uid, 'fields': {'object_pk': str(survey_group.pk)}},
        ])

        next_url = response.data['next-url']
        self.assertTrue(next_url is not None)

        response = self.client.get(next_url)
        self.assertEqual(response.status_code, 200)

        roles = defaultdict(list)
        for role in response.data['roles']:
            role_path = role.pop('path')
            roles[role_path].append(dict(role))

        self.assertEqual(len(roles), 2)
        for role_path, _roles in roles.items():
            roles[role_path] = sorted(_roles, key=lambda x: x.get('uid', ''))

        role = f'/role/{settings.ROLE_FORM_MANAGER}/'
        self.assertListEqual(roles[role], [
            {'group': get_group_id(group.name), 'fields': {'object_pk': str(survey.pk)}},
        ])
        role = f'/role/{settings.ROLE_GROUP_MANAGER}/'
        self.assertListEqual(roles[role], [
            {'group': get_group_id(group.name), 'fields': {'object_pk': str(survey_group.pk)}},
        ])

        next_url = response.data['next-url']
        self.assertTrue(next_url is None)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_roles_b2b(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        UserFactory(is_superuser=True, is_staff=True, email='yndx-user1@yandex.ru', first_name='user1')
        UserFactory(is_staff=True, email='yndx-user2@yandex.ru', first_name='user2')
        user = UserFactory()
        group = GroupFactory()
        survey = SurveyFactory()
        survey_group = SurveyGroupFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, user, survey)
        assign_perm(settings.ROLE_FORM_MANAGER, group, survey)
        assign_perm(settings.ROLE_GROUP_MANAGER, user, survey_group)
        assign_perm(settings.ROLE_GROUP_MANAGER, group, survey_group)

        response = self.client.get('/idm/get-roles/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['roles'], [
            {
                'path': '/role/superuser/',
                'login': 'user1',
                'fields': {'passport-login': 'yndx-user1'},
            },
        ])
        next_url = response.data['next-url']
        self.assertTrue(next_url is not None)

        response = self.client.get(next_url)
        self.assertEqual(response.data['roles'], [
            {
                'path': '/role/support/',
                'login': 'user1',
                'fields': {'passport-login': 'yndx-user1'},
            },
            {
                'path': '/role/support/',
                'login': 'user2',
                'fields': {'passport-login': 'yndx-user2'},
            },
        ])
        next_url = response.data['next-url']
        self.assertTrue(next_url is not None)

        response = self.client.get(next_url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['roles'], [])

        next_url = response.data['next-url']
        self.assertTrue(next_url is not None)

        response = self.client.get(next_url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['roles'], [])

        next_url = response.data['next-url']
        self.assertTrue(next_url is None)

    def test_should_return_memberships(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        group1 = GroupFactory()
        group2 = GroupFactory()
        user1 = UserFactory()
        user2 = UserFactory()
        group1.user_set.add(user1)
        group2.user_set.add(user1, user2)

        response = self.client.get('/idm/get-memberships/')
        self.assertEqual(response.status_code, 200)

        memberships = response.data['memberships']
        self.assertEqual(len(memberships), 3)

        groups = defaultdict(list)
        for member in memberships:
            groups[member['group']].append((member['uid'], member['login']))
        self.assertListEqual(sorted(groups[get_group_id(group1.name)]), [
            (user1.uid, user1.username),
        ])
        self.assertListEqual(sorted(groups[get_group_id(group2.name)]), [
            (user1.uid, user1.username),
            (user2.uid, user2.username),
        ])

    def test_should_add_user_to_group_membership(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        group1 = GroupFactory()
        group2 = GroupFactory()
        user1 = UserFactory()
        user2 = UserFactory()

        data = {
            'data': [
                {
                    'group': get_group_id(group1.name),
                    'login': user1.username,
                    'uid': user1.uid,
                },
                {
                    'group': get_group_id(group1.name),
                    'login': user2.username,
                    'uid': user2.uid,
                },
                {
                    'group': get_group_id(group2.name),
                    'login': user1.username,
                    'uid': user1.uid,
                },
            ],
        }
        response = self.client.post('/idm/add-batch-memberships/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertListEqual(list(group1.user_set.values_list('uid', 'username').order_by('uid')), [
            (user1.uid, user1.username),
            (user2.uid, user2.username),
        ])
        self.assertListEqual(list(group2.user_set.values_list('uid', 'username').order_by('uid')), [
            (user1.uid, user1.username),
        ])

    def test_should_remove_user_from_group_membership(self):
        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)

        group1 = GroupFactory()
        group2 = GroupFactory()
        user1 = UserFactory()
        user2 = UserFactory()
        group1.user_set.add(user1, user2)
        group2.user_set.add(user1, user2)

        data = {
            'data': [
                {
                    'group': get_group_id(group1.name),
                    'login': user2.username,
                    'uid': user2.uid,
                },
                {
                    'group': get_group_id(group2.name),
                    'login': user1.username,
                    'uid': user1.uid,
                },
                {
                    'group': get_group_id(group2.name),
                    'login': user2.username,
                    'uid': user2.uid,
                },
            ],
        }
        response = self.client.post('/idm/remove-batch-memberships/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertListEqual(list(group1.user_set.values_list('uid', 'username')), [
            (user1.uid, user1.username),
        ])
        self.assertEqual(group2.user_set.count(), 0)


class TestIdmViewSet_cookie_auth(CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def test_should_return_info(self):
        user = UserFactory(is_superuser=True)
        self.client.set_cookie(user.uid)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_return_info(self):
        user = UserFactory()
        self.client.set_cookie(user.uid)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 403)


class TestIdmViewSet_oauth(OAuthTestCase):
    fixtures = ['initial_data.json']

    def test_should_return_info(self):
        user = UserFactory(is_superuser=True)
        self.client.set_header(user.uid)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_return_info(self):
        user = UserFactory()
        self.client.set_header(user.uid)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 403)


class TestIdmViewSet_header_auth(HeaderAuthTestCase):
    fixtures = ['initial_data.json']

    def test_should_return_info(self):
        user = UserFactory(is_superuser=True)
        self.client.set_header(user.uid)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_return_info(self):
        user = UserFactory()
        self.client.set_header(user.uid)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 403)


class TestIdmViewSet_tvm_user(TVMAuthTestCase):
    fixtures = ['initial_data.json']

    def test_should_return_info(self):
        user = UserFactory(is_superuser=True)
        self.client.set_service_ticket(settings.ADMIN_TVM2_CLIENT)
        self.client.set_user_ticket(user.uid)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_return_info(self):
        user = UserFactory()
        self.client.set_service_ticket(settings.ADMIN_TVM2_CLIENT)
        self.client.set_user_ticket(user.uid)

        response = self.client.get('/idm/info/')
        self.assertEqual(response.status_code, 403)


class TestAddRemoveRoleSerializer(TestCase):
    def test_should_validate_with_uid(self):
        data = {
            'role': {'role': 'superuser'},
            'uid': '123454321',
        }
        serializer = AddRemoveRoleSerializer(data=data)
        self.assertTrue(serializer.is_valid())

        self.assertEqual(serializer.validated_data['uid'], '123454321')
        self.assertEqual(serializer.validated_data['role'], {'role': 'superuser'})
        self.assertIsNone(serializer.validated_data['login'])
        self.assertIsNone(serializer.validated_data['group'])
        self.assertFalse(serializer.validated_data['fired'])
        self.assertEqual(serializer.validated_data['fields'], {})

    def test_should_validate_with_login(self):
        data = {
            'role': {'role': 'superuser'},
            'login': 'test-new-user',
        }
        serializer = AddRemoveRoleSerializer(data=data)
        self.assertTrue(serializer.is_valid())

        self.assertIsNone(serializer.validated_data['uid'])
        self.assertEqual(serializer.validated_data['role'], {'role': 'superuser'})
        self.assertEqual(serializer.validated_data['login'], 'test-new-user')
        self.assertIsNone(serializer.validated_data['group'])
        self.assertFalse(serializer.validated_data['fired'])
        self.assertEqual(serializer.validated_data['fields'], {})

    def test_should_validate_with_group(self):
        data = {
            'role': {'role': 'superuser'},
            'group': 12321,
        }
        serializer = AddRemoveRoleSerializer(data=data)
        self.assertTrue(serializer.is_valid())

        self.assertIsNone(serializer.validated_data['uid'])
        self.assertEqual(serializer.validated_data['role'], {'role': 'superuser'})
        self.assertIsNone(serializer.validated_data['login'])
        self.assertEqual(serializer.validated_data['group'], '12321')
        self.assertFalse(serializer.validated_data['fired'])
        self.assertEqual(serializer.validated_data['fields'], {})

    def test_shouldnt_validate(self):
        data = {
            'role': {'role': 'superuser'},
        }
        serializer = AddRemoveRoleSerializer(data=data)
        self.assertFalse(serializer.is_valid())

    def test_should_validate_data_for_user(self):
        data = {
            'role': {'role': 'change_survey'},
            'uid': '123454321',
            'login': 'test-new-user',
            'fired': '1',
            'fields': {'object_pk': '1234'},
        }
        serializer = AddRemoveRoleSerializer(data=data)
        self.assertTrue(serializer.is_valid())

        self.assertEqual(serializer.validated_data['uid'], '123454321')
        self.assertEqual(serializer.validated_data['role'], {'role': 'change_survey'})
        self.assertEqual(serializer.validated_data['login'], 'test-new-user')
        self.assertIsNone(serializer.validated_data['group'])
        self.assertTrue(serializer.validated_data['fired'])
        self.assertEqual(serializer.validated_data['fields'], {'object_pk': '1234'})

    def test_should_validate_data_for_group(self):
        data = {
            'role': {'role': 'change_survey'},
            'group': 12321,
            'fields': {'object_pk': '1234'},
        }
        serializer = AddRemoveRoleSerializer(data=data)
        self.assertTrue(serializer.is_valid())

        self.assertIsNone(serializer.validated_data['uid'])
        self.assertEqual(serializer.validated_data['role'], {'role': 'change_survey'})
        self.assertIsNone(serializer.validated_data['login'])
        self.assertEqual(serializer.validated_data['group'], '12321')
        self.assertFalse(serializer.validated_data['fired'])
        self.assertEqual(serializer.validated_data['fields'], {'object_pk': '1234'})

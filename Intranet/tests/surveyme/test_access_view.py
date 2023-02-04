# -*- coding: utf-8 -*-
from unittest.mock import patch

from django.contrib.contenttypes.models import ContentType
from django.conf import settings
from django.test import TestCase, override_settings
from guardian.shortcuts import get_perms, assign_perm

from events.accounts.factories import (
    UserFactory,
    GroupFactory,
    OrganizationFactory,
    OrganizationToGroupFactory,
)
from events.accounts.managers import UserManager
from events.idm import utils
from events.staff.factories import StaffGroupFactory
from events.staff.models import StaffGroup
from events.surveyme import tasks
from events.surveyme.factories import SurveyFactory, SurveyGroupFactory, SurveyQuestionFactory
from events.surveyme.logic import access
from events.surveyme.models import Survey, SurveyGroup
from events.surveyme.api_admin.v2.serializers import SurveyAccessDeserializer
from events.yauth_contrib.helpers import CookieAuthTestCase


@override_settings(IS_BUSINESS_SITE=True)
class TestB2bSurveyAccessIntegration(CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        o2g = OrganizationToGroupFactory()
        self.group = o2g.group
        self.org = o2g.org

        self.user = UserFactory()
        self.user1 = UserFactory()

        self.user.groups.add(self.group)
        self.user1.groups.add(self.group)

        self.survey = SurveyFactory(user=self.user, org=self.org)
        assign_perm('change_survey', self.user, self.survey)

        self.survey_group = SurveyGroupFactory(user=self.user, org=self.org)
        assign_perm('change_surveygroup', self.user, self.survey_group)

        self.client.set_cookie(self.user.uid)

    def test_user1_shouldnt_access_to_survey(self):
        data = {'name': 'the form', 'org_id': self.org.dir_id}
        response = self.client.post(
            '/admin/api/v2/surveys/',
            data=data, format='json', HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 201)

        new_survey = Survey.objects.get(pk=response.data['id'])

        self.assertTrue(self.user.has_perm('change_survey', new_survey))
        self.assertFalse(self.user1.has_perm('change_survey', new_survey))

    def test_user1_shouldnt_access_to_survey_group(self):
        data = {'name': 'the group', 'org_id': self.org.dir_id}
        response = self.client.post(
            '/admin/api/v2/survey-groups/',
            data=data, format='json', HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 201)

        new_survey_group = SurveyGroup.objects.get(pk=response.data['id'])
        self.assertEqual(new_survey_group.org, self.org)

        self.assertTrue(self.user.has_perm('change_surveygroup', new_survey_group))
        self.assertFalse(self.user1.has_perm('change_surveygroup', new_survey_group))

    def test_user_should_reach_to_survey_through_common_access(self):
        data = {'type': 'common'}
        response = self.client.post(
            f'/admin/api/v2/surveys/{self.survey.pk}/access/',
            data=data, format='json', HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertTrue(self.user1.has_perm('change_survey', self.survey))

    def test_user_should_reach_to_survey_through_restricted_access(self):
        data = {'type': 'restricted', 'users': [self.user1.uid]}
        response = self.client.post(
            f'/admin/api/v2/surveys/{self.survey.pk}/access/',
            data=data, format='json', HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertTrue(self.user1.has_perm('change_survey', self.survey))

    def test_user_shouldnt_reach_to_survey_through_owner_access(self):
        assign_perm('change_survey', self.user1, self.survey)
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertTrue(self.user1.has_perm('change_survey', self.survey))

        data = {'type': 'owner'}
        response = self.client.post(
            f'/admin/api/v2/surveys/{self.survey.pk}/access/',
            data=data, format='json', HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_user_shouldnt_access_to_survey_group(self):
        self.survey.group = self.survey_group
        self.survey.save()

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertFalse(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_user_should_reach_to_survey_through_common_access_on_survey_group(self):
        self.survey.group = self.survey_group
        self.survey.save()

        data = {'type': 'common'}
        response = self.client.post(
            f'/admin/api/v2/survey-groups/{self.survey_group.pk}/access/',
            data=data, format='json', HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_user_should_reach_to_survey_through_restricted_access_on_survey_group(self):
        self.survey.group = self.survey_group
        self.survey.save()

        data = {'type': 'restricted', 'users': [self.user1.uid]}
        response = self.client.post(
            f'/admin/api/v2/survey-groups/{self.survey_group.pk}/access/',
            data=data, format='json', HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_user_shouldnt_reach_to_survey_through_owner_access_on_survey_group(self):
        self.survey.group = self.survey_group
        self.survey.save()

        assign_perm('change_surveygroup', self.user1, self.survey_group)

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

        data = {'type': 'owner'}
        response = self.client.post(
            f'/admin/api/v2/survey-groups/{self.survey_group.pk}/access/',
            data=data, format='json', HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertFalse(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_should_remove_roles_for_survey(self):
        response = self.client.delete(
            f'/admin/api/v2/surveys/{self.survey.pk}/',
            HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 204)

        self.assertFalse(self.user.has_perm('change_survey', self.survey))

    def test_should_remove_roles_for_survey_group(self):
        with patch('events.surveyme.logic.access.reject_roles') as mock_reject_roles:
            response = self.client.delete(
                f'/admin/api/v2/survey-groups/{self.survey_group.pk}/',
                HTTP_X_ORGS=self.org.dir_id,
            )
        self.assertEqual(response.status_code, 204)

        ct = ContentType.objects.get_for_model(SurveyGroup)
        perm = access.get_change_permission(ct)
        mock_reject_roles.assert_called_once_with(
            self.user, perm, users=[self.user], groups=[],
            content_type=ct, object_pk=self.survey_group.pk,
        )
        with self.assertRaises(SurveyGroup.DoesNotExist):
            SurveyGroup.objects.get(pk=self.survey_group.pk)


@override_settings(IS_INERNAL_SITE=True)
class TestSurveyAccessIntegration(CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.group = access.get_common_group()

        self.user = UserFactory()
        self.user1 = UserFactory()

        self.user.groups.add(self.group)
        self.user1.groups.add(self.group)

        self.survey = SurveyFactory(user=self.user)
        assign_perm('change_survey', self.user, self.survey)

        self.survey_group = SurveyGroupFactory(user=self.user)
        assign_perm('change_surveygroup', self.user, self.survey_group)

        self.client.set_cookie(self.user.uid)

    def test_user1_shouldnt_access_to_survey(self):
        data = {'name': 'the form'}
        response = self.client.post('/admin/api/v2/surveys/', data=data, format='json')
        self.assertEqual(response.status_code, 201)

        new_survey = Survey.objects.get(pk=response.data['id'])

        self.assertTrue(self.user.has_perm('change_survey', new_survey))
        self.assertFalse(self.user1.has_perm('change_survey', new_survey))

    def test_user1_shouldnt_access_to_survey_group(self):
        data = {'name': 'the group'}
        response = self.client.post('/admin/api/v2/survey-groups/', data=data, format='json')
        self.assertEqual(response.status_code, 201)

        new_survey_group = SurveyGroup.objects.get(pk=response.data['id'])

        self.assertTrue(self.user.has_perm('change_surveygroup', new_survey_group))
        self.assertFalse(self.user1.has_perm('change_surveygroup', new_survey_group))

    def test_user_should_reach_to_survey_through_common_access(self):
        data = {'type': 'common'}
        response = self.client.post(
            f'/admin/api/v2/surveys/{self.survey.pk}/access/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertTrue(self.user1.has_perm('change_survey', self.survey))

    def test_user_should_reach_to_survey_through_restricted_access(self):
        data = {'type': 'restricted', 'users': [self.user1.uid]}
        response = self.client.post(
            f'/admin/api/v2/surveys/{self.survey.pk}/access/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertTrue(self.user1.has_perm('change_survey', self.survey))

    def test_user_shouldnt_reach_to_survey_through_owner_access(self):
        assign_perm('change_survey', self.user1, self.survey)
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertTrue(self.user1.has_perm('change_survey', self.survey))

        data = {'type': 'owner'}
        response = self.client.post(
            f'/admin/api/v2/surveys/{self.survey.pk}/access/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_user_shouldnt_access_to_survey_group(self):
        self.survey.group = self.survey_group
        self.survey.save()

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertFalse(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_user_should_reach_to_survey_through_common_access_on_survey_group(self):
        self.survey.group = self.survey_group
        self.survey.save()

        data = {'type': 'common'}
        response = self.client.post(
            f'/admin/api/v2/survey-groups/{self.survey_group.pk}/access/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_user_should_reach_to_survey_through_restricted_access_on_survey_group(self):
        self.survey.group = self.survey_group
        self.survey.save()

        data = {'type': 'restricted', 'users': [self.user1.uid]}
        response = self.client.post(
            f'/admin/api/v2/survey-groups/{self.survey_group.pk}/access/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_user_shouldnt_reach_to_survey_through_owner_access_on_survey_group(self):
        self.survey.group = self.survey_group
        self.survey.save()

        assign_perm('change_surveygroup', self.user1, self.survey_group)

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

        data = {'type': 'owner'}
        response = self.client.post(
            f'/admin/api/v2/survey-groups/{self.survey_group.pk}/access/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue(self.user.has_perm('change_surveygroup', self.survey_group))
        self.assertFalse(self.user1.has_perm('change_surveygroup', self.survey_group))
        self.assertTrue(self.user.has_perm('change_survey', self.survey))
        self.assertFalse(self.user1.has_perm('change_survey', self.survey))

    def test_should_remove_roles_for_survey(self):
        response = self.client.delete(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 204)

        self.assertFalse(self.user.has_perm('change_survey', self.survey))

    def test_should_remove_roles_for_survey_group(self):
        with patch('events.surveyme.logic.access.reject_roles') as mock_reject_roles:
            response = self.client.delete(f'/admin/api/v2/survey-groups/{self.survey_group.pk}/')
        self.assertEqual(response.status_code, 204)

        ct = ContentType.objects.get_for_model(SurveyGroup)
        perm = access.get_change_permission(ct)
        mock_reject_roles.assert_called_once_with(
            self.user, perm, users=[self.user], groups=[],
            content_type=ct, object_pk=self.survey_group.pk,
        )


class TestPreviewAccess(CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(name='Форма', is_published_external=True)
        SurveyQuestionFactory(survey=self.survey)

    def test_unathorized(self):
        response = self.client.get(f'/v1/surveys/{self.survey.pk}/?preview=True')
        self.assertEqual(response.status_code, 200)
        self.assertFalse(response.data['is_user_can_answer'])
        self.assertEqual(response.data['why_user_cant_answer'], {'need_auth': 'is need auth to answer'})

    def test_authorized(self):
        user = UserFactory()
        self.client.set_cookie(user.uid)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/?preview=True')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['is_user_can_answer'])

    def test_authorized_not_created(self):
        self.client.set_cookie('11591999')

        with patch.object(UserManager, 'get_params') as mock_params:
            mock_params.return_value = {}
            response = self.client.get(f'/v1/surveys/{self.survey.pk}/?preview=True')

        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['is_user_can_answer'])
        mock_params.assert_called_once_with(11591999, None, None)


class TestPermissionDeniedDetailed(CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(user=UserFactory())
        self.client.set_cookie(self.user.uid)

    def test_should_return_user_list(self):
        users = []
        for _ in range(2):
            user = UserFactory()
            assign_perm('change_survey', user, self.survey)
            users.append(user)

        with patch.object(UserManager, 'get_params', return_value={}):
            response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/')

        self.assertEqual(response.status_code, 403)
        self.assertTrue('author' in response.data)
        expected = {
            'uid': self.survey.user.uid,
            'cloud_uid': self.survey.user.cloud_uid,
            'display_name': self.survey.user.email,
            'email': self.survey.user.email,
        }
        self.assertDictEqual(response.data['author'], expected)
        self.assertTrue('users' in response.data)
        expected = {
            user.uid: {
                'uid': user.uid,
                'cloud_uid': user.cloud_uid,
                'display_name': user.email,
                'email': user.email,
            }
            for user in users
        }
        result = {
            user['uid']: {
                'uid': user['uid'],
                'cloud_uid': user['cloud_uid'],
                'display_name': user['email'],
                'email': user['email'],
            }
            for user in response.data['users']
        }
        self.assertDictEqual(result, expected)

    def test_shouldnt_return_user_list(self):
        with patch.object(UserManager, 'get_params', return_value={}):
            response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/')

        self.assertEqual(response.status_code, 403)
        self.assertTrue('author' in response.data)
        expected = {
            'uid': self.survey.user.uid,
            'cloud_uid': self.survey.user.cloud_uid,
            'display_name': self.survey.user.email,
            'email': self.survey.user.email,
        }
        self.assertDictEqual(response.data['author'], expected)
        self.assertTrue('users' not in response.data)

    def test_shouldnt_return_user_list_if_shared_for_group(self):
        group = access.get_common_group()
        assign_perm('change_survey', group, self.survey)
        users = []
        for _ in range(2):
            user = UserFactory()
            group.user_set.add(user)
            users.append(user)

        with patch.object(UserManager, 'get_params', return_value={}):
            response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/')

        self.assertEqual(response.status_code, 403)
        self.assertTrue('author' in response.data)
        expected = {
            'uid': self.survey.user.uid,
            'cloud_uid': self.survey.user.cloud_uid,
            'display_name': self.survey.user.email,
            'email': self.survey.user.email,
        }
        self.assertDictEqual(response.data['author'], expected)
        self.assertTrue('users' not in response.data)
        self.assertTrue(users[0].has_perm('change_survey', self.survey))
        self.assertTrue(users[1].has_perm('change_survey', self.survey))


class TestSurveyAccessDeserializer(TestCase):
    def test_should_return_uid_list(self):
        data = {
            'type': 'restricted',
            'users': [123, '124'],
        }
        serializer = SurveyAccessDeserializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertEqual(serializer.validated_data['type'], 'restricted')
        self.assertListEqual(serializer.validated_data['users'], ['123', '124'])

    def test_should_return_list_of_objects(self):
        data = {
            'type': 'restricted',
            'users': [
                {'uid': '123'},
                {'cloud_uid': 'abcd'},
                {'uid': '124', 'cloud_uid': 'abce'},
            ],
        }
        serializer = SurveyAccessDeserializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertEqual(serializer.validated_data['type'], 'restricted')
        self.assertListEqual(serializer.validated_data['users'], [
            {'uid': '123'},
            {'cloud_uid': 'abcd'},
            {'uid': '124', 'cloud_uid': 'abce'},
        ])

    def test_should_return_mixed_list_of_objects(self):
        data = {
            'type': 'restricted',
            'users': [
                121, '122',
                {'uid': '123'},
                {'cloud_uid': 'abcd'},
                {'uid': '124', 'cloud_uid': 'abce'},
            ],
        }
        serializer = SurveyAccessDeserializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertEqual(serializer.validated_data['type'], 'restricted')
        self.assertListEqual(serializer.validated_data['users'], [
            '121', '122',
            {'uid': '123'},
            {'cloud_uid': 'abcd'},
            {'uid': '124', 'cloud_uid': 'abce'},
        ])


class TestLogicAccessUtils(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.request_user = UserFactory()

    def test_get_change_permission(self):
        survey = SurveyFactory()
        perm = access.get_change_permission(survey)
        self.assertIsNotNone(perm)
        self.assertEqual(perm.codename, settings.ROLE_FORM_MANAGER)

        ct = ContentType.objects.get_for_model(survey)
        perm = access.get_change_permission(ct)
        self.assertIsNotNone(perm)
        self.assertEqual(perm.codename, settings.ROLE_FORM_MANAGER)

        survey_group = SurveyGroupFactory()
        perm = access.get_change_permission(survey_group)
        self.assertIsNotNone(perm)
        self.assertEqual(perm.codename, settings.ROLE_GROUP_MANAGER)

        ct = ContentType.objects.get_for_model(survey_group)
        perm = access.get_change_permission(ct)
        self.assertIsNotNone(perm)
        self.assertEqual(perm.codename, settings.ROLE_GROUP_MANAGER)

    def test_get_common_group(self):
        o2g = OrganizationToGroupFactory()
        org = o2g.org

        common_group = access.get_common_group(org)
        self.assertIsNotNone(common_group)
        self.assertEqual(common_group.pk, settings.GROUP_ALLSTAFF_PK)

        common_group = access.get_common_group()
        self.assertIsNotNone(common_group)
        self.assertEqual(common_group.pk, settings.GROUP_ALLSTAFF_PK)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_get_common_group_for_biz(self):
        o2g = OrganizationToGroupFactory()
        org = o2g.org
        group = o2g.group

        common_group = access.get_common_group(org)
        self.assertIsNotNone(common_group)
        self.assertEqual(common_group, group)

        common_group = access.get_common_group()
        self.assertIsNone(common_group)

    def test_get_groups_from_db(self):
        staff_groups = [
            StaffGroupFactory(name='one'),
            StaffGroupFactory(name='two'),
            StaffGroupFactory(name='three'),
        ]
        groups_from_db = access.get_groups_from_db([
            staff_groups[0].staff_id,
            staff_groups[2].staff_id,
            12321,
        ])
        self.assertEqual(len(groups_from_db), 2)
        self.assertSetEqual(
            {groups_from_db[0].id, groups_from_db[1].id},
            {staff_groups[0].staff_id, staff_groups[2].staff_id},
        )

    def test_get_staff_groups(self):
        groups = [
            GroupFactory(),
            GroupFactory(),
            GroupFactory(),
        ]
        staff_groups = [
            StaffGroupFactory(staff_id=utils.get_group_id(groups[0].name), name='one'),
            StaffGroupFactory(staff_id=utils.get_group_id(groups[1].name), name='two'),
        ]
        group_names = [it.name for it in groups]
        staff_groups = access.get_staff_groups(group_names)
        self.assertEqual(len(staff_groups), 2)
        self.assertSetEqual(
            {utils.get_group_id(groups[0].name), utils.get_group_id(groups[1].name)},
            {staff_groups[0].staff_id, staff_groups[1].staff_id},
        )

        staff_groups = access.get_staff_groups([])
        self.assertIsNone(staff_groups)

    def test_request_roles(self):
        survey = SurveyFactory()
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        ct = ContentType.objects.get_for_model(survey)

        perm = access.get_change_permission(survey)
        with patch.object(utils.IdmUtils, 'request_roles') as mock_request_roles:
            access.request_roles(self.request_user, perm, users=users, groups=groups, content_type=ct, object_pk=survey.pk)

        self.assertListEqual(list(get_perms(users[0], survey)), [perm.codename])
        self.assertListEqual(list(get_perms(users[1], survey)), [perm.codename])
        self.assertListEqual(list(get_perms(groups[0], survey)), [perm.codename])
        self.assertListEqual(list(get_perms(groups[1], survey)), [perm.codename])

        mock_request_roles.assert_called_once_with(
            perm.codename,
            users=[it.username for it in users],
            groups=[utils.get_group_id(it.name) for it in groups],
            object_pk=survey.pk,
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_request_roles_for_biz(self):
        survey = SurveyFactory()
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        ct = ContentType.objects.get_for_model(survey)

        perm = access.get_change_permission(survey)
        with patch.object(utils.IdmUtils, 'request_roles') as mock_request_roles:
            access.request_roles(self.request_user, perm, users=users, groups=groups, content_type=ct, object_pk=survey.pk)

        self.assertListEqual(list(get_perms(users[0], survey)), [perm.codename])
        self.assertListEqual(list(get_perms(users[1], survey)), [perm.codename])
        self.assertListEqual(list(get_perms(groups[0], survey)), [perm.codename])
        self.assertListEqual(list(get_perms(groups[1], survey)), [perm.codename])

        mock_request_roles.assert_not_called()

    def test_reject_roles(self):
        survey = SurveyFactory()
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        ct = ContentType.objects.get_for_model(survey)

        perm = access.get_change_permission(survey)
        for user in users:
            assign_perm(perm, user, survey)
        for group in groups:
            assign_perm(perm, group, survey)

        with patch.object(utils.IdmUtils, 'reject_roles') as mock_reject_roles:
            access.reject_roles(self.request_user, perm, users=users, groups=groups, content_type=ct, object_pk=survey.pk)

        self.assertEqual(len(get_perms(users[0], survey)), 0)
        self.assertEqual(len(get_perms(users[1], survey)), 0)
        self.assertEqual(len(get_perms(groups[0], survey)), 0)
        self.assertEqual(len(get_perms(groups[1], survey)), 0)

        mock_reject_roles.assert_called_once_with(
            perm.codename,
            users=[it.username for it in users],
            groups=[utils.get_group_id(it.name) for it in groups],
            object_pk=survey.pk,
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_reject_roles_for_biz(self):
        survey = SurveyFactory()
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        ct = ContentType.objects.get_for_model(survey)

        perm = access.get_change_permission(survey)
        for user in users:
            assign_perm(perm, user, survey)
        for group in groups:
            assign_perm(perm, group, survey)

        with patch.object(utils.IdmUtils, 'reject_roles') as mock_reject_roles:
            access.reject_roles(self.request_user, perm, users=users, groups=groups, content_type=ct, object_pk=survey.pk)

        self.assertEqual(len(get_perms(users[0], survey)), 0)
        self.assertEqual(len(get_perms(users[1], survey)), 0)
        self.assertEqual(len(get_perms(groups[0], survey)), 0)
        self.assertEqual(len(get_perms(groups[1], survey)), 0)

        mock_reject_roles.assert_not_called()

    def test_get_or_create_groups(self):
        group = GroupFactory()
        groups = access.get_or_create_groups([
            utils.get_group_id(group.name),
            12321,
        ])
        self.assertEqual(len(groups), 2)
        self.assertEqual(groups[0], group)
        self.assertEqual(groups[1].name, 'group:12321')

        groups = access.get_or_create_groups(None)
        self.assertEqual(len(groups), 0)

    def test_get_or_create_users(self):
        org = OrganizationFactory()
        user_uids = [
            '123454321',
            {'uid': '543212345', 'cloud_uid': 'abcd'},
        ]
        with patch.object(UserManager, 'get_or_create_user') as mock_create_user:
            users = access.get_or_create_users(user_uids, org)

        self.assertEqual(len(users), 2)
        self.assertEqual(mock_create_user.call_args_list, [
            (('123454321', None, org.dir_id), {'link_organizations': False}),
            (('543212345', 'abcd', org.dir_id), {'link_organizations': False}),
        ])

        with patch.object(UserManager, 'get_or_create_user') as mock_create_user:
            users = access.get_or_create_users(None, org)

        self.assertEqual(len(users), 0)
        mock_create_user.assert_not_called()

    def test_set_owner_access(self):
        author = UserFactory()
        user = UserFactory()
        group = GroupFactory()
        survey = SurveyFactory(user=author)

        perm = access.get_change_permission(survey)
        assign_perm(perm, author, survey)
        assign_perm(perm, user, survey)
        assign_perm(perm, group, survey)

        access.set_owner_access(self.request_user, survey)

        self.assertEqual(get_perms(author, survey), [perm.codename])
        self.assertEqual(get_perms(user, survey), [])
        self.assertEqual(get_perms(group, survey), [])

    def test_set_common_access(self):
        author = UserFactory()
        user = UserFactory()
        common_group = access.get_common_group()
        group = GroupFactory()
        survey = SurveyFactory(user=author)

        perm = access.get_change_permission(survey)
        assign_perm(perm, author, survey)
        assign_perm(perm, user, survey)
        assign_perm(perm, group, survey)

        access.set_common_access(self.request_user, survey)

        self.assertEqual(get_perms(author, survey), [perm.codename])
        self.assertEqual(get_perms(user, survey), [])
        self.assertEqual(get_perms(common_group, survey), [perm.codename])
        self.assertEqual(get_perms(group, survey), [])

    def test_set_restricted_access(self):
        author = UserFactory()
        user = UserFactory()
        common_group = access.get_common_group()
        group = GroupFactory()
        survey = SurveyFactory(user=author)

        perm = access.get_change_permission(survey)
        assign_perm(perm, author, survey)
        assign_perm(perm, common_group, survey)

        access.set_restricted_access(self.request_user, survey, [user], [group])

        self.assertEqual(get_perms(author, survey), [perm.codename])
        self.assertEqual(get_perms(user, survey), [perm.codename])
        self.assertEqual(get_perms(common_group, survey), [])
        self.assertEqual(get_perms(group, survey), [perm.codename])

    def test_set_survey_access(self):
        survey = SurveyFactory()
        with patch('events.surveyme.logic.access.set_owner_access') as mock_access:
            access.set_survey_access(self.request_user, survey, access.FORM_ACCESS_TYPE_OWNER)
        mock_access.assert_called_once_with(self.request_user, survey, None)

        with patch('events.surveyme.logic.access.set_common_access') as mock_access:
            access.set_survey_access(self.request_user, survey, access.FORM_ACCESS_TYPE_COMMON)
        mock_access.assert_called_once_with(self.request_user, survey, None)

        user = UserFactory()
        group = GroupFactory()
        with (
            patch('events.surveyme.logic.access.get_or_create_users') as mock_get_users,
            patch('events.surveyme.logic.access.get_or_create_groups') as mock_get_groups,
            patch('events.surveyme.logic.access.set_restricted_access') as mock_access,
        ):
            mock_get_users.return_value = [user]
            mock_get_groups.return_value = [group]
            access.set_survey_access(
                self.request_user, survey, access.FORM_ACCESS_TYPE_RESTRICTED,
                users=[user.username],
                groups=[utils.get_group_id(group.name)],
            )
        mock_get_users.assert_called_once_with([user.username], survey.org)
        mock_get_groups.assert_called_once_with([utils.get_group_id(group.name)])
        mock_access.assert_called_once_with(self.request_user, survey, [user], [group], None)

    def test_get_survey_access_for_owner(self):
        author = UserFactory()
        survey = SurveyFactory(user=author)

        perm = access.get_change_permission(survey)
        assign_perm(perm, author, survey)
        result = access.get_survey_access(survey)

        self.assertDictEqual(result, {'type': access.FORM_ACCESS_TYPE_OWNER})

    def test_get_survey_access_for_common_group(self):
        author = UserFactory()
        common_group = access.get_common_group()
        survey = SurveyFactory(user=author)

        perm = access.get_change_permission(survey)
        assign_perm(perm, author, survey)
        assign_perm(perm, common_group, survey)
        result = access.get_survey_access(survey)

        self.assertDictEqual(result, {'type': access.FORM_ACCESS_TYPE_COMMON})

    def test_get_survey_access_for_restricted(self):
        author = UserFactory()
        user = UserFactory()
        group = GroupFactory()
        common_group = access.get_common_group()
        survey = SurveyFactory(user=author)

        StaffGroup.objects.get_or_create(
            staff_id=utils.get_group_id(group.name),
            name='group',
        )
        StaffGroup.objects.get_or_create(
            staff_id=utils.get_group_id(common_group.name),
            name='common_group',
        )

        perm = access.get_change_permission(survey)
        assign_perm(perm, author, survey)
        assign_perm(perm, user, survey)
        assign_perm(perm, common_group, survey)
        assign_perm(perm, group, survey)
        result = access.get_survey_access(survey)

        self.assertEqual(result['type'], access.FORM_ACCESS_TYPE_RESTRICTED)
        groups = {group.id for group in result['groups']}
        users = {user.id for user in result['users']}

        self.assertFalse(author.pk in users)
        self.assertTrue(user.pk in users)
        self.assertTrue(utils.get_group_id(common_group.name) in groups)
        self.assertTrue(utils.get_group_id(group.name) in groups)


class TestRejectRolesTask(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.request_user = UserFactory()

    def test_shouldnt_reject_roles_for_survey(self):
        survey = SurveyFactory()
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        for it in users + groups:
            assign_perm('change_survey', it, survey)

        with patch('events.surveyme.logic.access.reject_roles') as mock_reject_roles:
            tasks.reject_roles(self.request_user.pk, Survey, survey.pk, deleted_only=True)

        mock_reject_roles.assect_not_called()

    def test_should_reject_roles_for_deleted_survey(self):
        survey = SurveyFactory()
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        for it in users + groups:
            assign_perm('change_survey', it, survey)

        survey.is_deleted = True
        survey.save()

        with patch('events.surveyme.logic.access.reject_roles') as mock_reject_roles:
            tasks.reject_roles(self.request_user.pk, Survey, survey.pk, deleted_only=True)

        ct = ContentType.objects.get_for_model(Survey)
        perm = access.get_change_permission(ct)
        mock_reject_roles.assert_called_once_with(
            self.request_user, perm, users=users, groups=groups,
            content_type=ct, object_pk=survey.pk,
        )

    def test_should_reject_roles_for_survey(self):
        survey = SurveyFactory()
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        for it in users + groups:
            assign_perm('change_survey', it, survey)

        with patch('events.surveyme.logic.access.reject_roles') as mock_reject_roles:
            tasks.reject_roles(self.request_user.pk, Survey, survey.pk, deleted_only=False)

        ct = ContentType.objects.get_for_model(Survey)
        perm = access.get_change_permission(ct)
        mock_reject_roles.assert_called_once_with(
            self.request_user, perm, users=users, groups=groups,
            content_type=ct, object_pk=survey.pk,
        )

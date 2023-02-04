# -*- coding: utf-8 -*-
from django.conf import settings
from django.contrib.auth.models import Permission, Group
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase, override_settings
from guardian.shortcuts import assign_perm, remove_perm
from unittest.mock import patch

from events.accounts.factories import UserFactory, GroupFactory, OrganizationToGroupFactory
from events.accounts.helpers import YandexClient
from events.accounts.models import User
from events.surveyme.factories import SurveyGroupFactory
from events.surveyme.models import SurveyGroup
from events.staff.factories import StaffGroupFactory
from events.v3.helpers import has_perm


class TestGetSurveyGroupView_check_access(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_unauthorized(self):
        self.client.remove_cookie()
        surveygroup = SurveyGroupFactory()
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 401)

    def test_should_return_not_found(self):
        response = self.client.get('/v3/survey-groups/999999/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_not_permitted(self):
        surveygroup = SurveyGroupFactory()
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 403)

    def test_should_return_success(self):
        surveygroup = SurveyGroupFactory()
        assign_perm(settings.ROLE_GROUP_MANAGER, self.user, surveygroup)
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 200)

        self.user.is_superuser = True
        self.user.save()
        remove_perm(settings.ROLE_GROUP_MANAGER, self.user, surveygroup)
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 200)


@override_settings(IS_BUSINESS_SITE=True)
class TestGetSurveyView_check_access_b2b(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_unauthorized(self):
        self.client.remove_cookie()
        surveygroup = SurveyGroupFactory()
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 401)

    def test_should_return_not_found(self):
        response = self.client.get('/v3/survey-groups/999999/')
        self.assertEqual(response.status_code, 404)

        surveygroup = SurveyGroupFactory()
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 404)

        o2g = OrganizationToGroupFactory()
        self.user.groups.add(o2g.group)
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 404)

        self.user.groups.remove(o2g.group)
        surveygroup.org = o2g.org
        surveygroup.save()
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_not_permitted(self):
        o2g = OrganizationToGroupFactory()
        surveygroup = SurveyGroupFactory(org=o2g.org)
        self.user.groups.add(o2g.group)
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 403)

    def test_should_return_success(self):
        surveygroup = SurveyGroupFactory(user=self.user)
        assign_perm(settings.ROLE_GROUP_MANAGER, self.user, surveygroup)
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 200)

        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/', HTTP_X_ORGS='999')
        self.assertEqual(response.status_code, 200)

        o2g = OrganizationToGroupFactory()
        surveygroup = SurveyGroupFactory(org=o2g.org)
        self.user.groups.add(o2g.group)
        assign_perm(settings.ROLE_GROUP_MANAGER, self.user, surveygroup)
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)

        self.user.is_superuser = True
        self.user.save()
        self.user.groups.remove(o2g.group)
        remove_perm(settings.ROLE_GROUP_MANAGER, self.user, surveygroup)
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 200)


class TestGetSurveyGroupView_response(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)

    def test_should_return_response_for_intranet(self):
        surveygroup = SurveyGroupFactory(name='Test it')
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {
            'id': surveygroup.pk,
            'name': surveygroup.name,
        })

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_response_for_business(self):
        o2g = OrganizationToGroupFactory()
        surveygroup = SurveyGroupFactory(name='Test it', org=o2g.org)
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {
            'id': surveygroup.pk,
            'name': surveygroup.name,
            'dir_id': o2g.org.dir_id,
        })

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_response_for_personal(self):
        surveygroup = SurveyGroupFactory(name='Test it')
        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {
            'id': surveygroup.pk,
            'name': surveygroup.name,
        })


class TestGetSurveyGroupAccessView_response(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        Permission.objects.get_or_create(
            codename='viewfile_surveygroup',
            content_type=ContentType.objects.get_for_model(SurveyGroup),
        )
        self.common_group = Group.objects.get(pk=settings.GROUP_ALLSTAFF_PK)

    def test_should_return_owner_access(self):
        user = UserFactory()
        surveygroup = SurveyGroupFactory(user=user)
        assign_perm('change_surveygroup', user, surveygroup)
        assign_perm('viewfile_surveygroup', user, surveygroup)

        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/access/')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'owner',
            'action': 'change',
        })
        self.assertEqual(result[1], {
            'access': 'owner',
            'action': 'viewfile',
        })

    def test_should_return_common_access(self):
        user = UserFactory()
        surveygroup = SurveyGroupFactory(user=user)
        assign_perm('change_surveygroup', self.common_group, surveygroup)
        assign_perm('viewfile_surveygroup', self.common_group, surveygroup)

        response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/access/')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'common',
            'action': 'change',
        })
        self.assertEqual(result[1], {
            'access': 'common',
            'action': 'viewfile',
        })

    def test_should_return_restricted_access(self):
        surveygroup = SurveyGroupFactory(user=UserFactory())
        user = UserFactory()
        staff_group = StaffGroupFactory(name='Test group', url='svc_test')
        group = GroupFactory(name=f'group:{staff_group.pk}')
        assign_perm('change_surveygroup', user, surveygroup)
        assign_perm('change_surveygroup', group, surveygroup)
        assign_perm('viewfile_surveygroup', user, surveygroup)
        assign_perm('viewfile_surveygroup', group, surveygroup)

        with patch.object(User, 'get_name_and_surname_with_fallback') as mock_get_name:
            mock_get_name.return_value = 'John Doe'
            response = self.client.get(f'/v3/survey-groups/{surveygroup.pk}/access/')
            self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'restricted',
            'action': 'change',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        })
        self.assertEqual(result[1], {
            'access': 'restricted',
            'action': 'viewfile',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        })
        self.assertEqual(mock_get_name.call_count, 2)


class TestPostSurveyGroupAccessView_response(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        Permission.objects.get_or_create(
            codename='viewfile_surveygroup',
            content_type=ContentType.objects.get_for_model(SurveyGroup),
        )
        self.common_group = Group.objects.get(pk=settings.GROUP_ALLSTAFF_PK)

    def test_should_make_owner_access(self):
        user = UserFactory()
        surveygroup = SurveyGroupFactory(user=user)
        data = [
            {'access': 'owner', 'action': 'change'},
            {'access': 'owner', 'action': 'viewfile'},
        ]
        response = self.client.post(f'/v3/survey-groups/{surveygroup.pk}/access/', data, format='json')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'owner',
            'action': 'change',
        })
        self.assertEqual(result[1], {
            'access': 'owner',
            'action': 'viewfile',
        })

        self.assertTrue(has_perm(user, 'change_surveygroup', surveygroup))
        self.assertTrue(has_perm(user, 'viewfile_surveygroup', surveygroup))

    def test_should_make_common_access(self):
        surveygroup = SurveyGroupFactory()
        data = [
            {'access': 'common', 'action': 'change'},
            {'access': 'common', 'action': 'viewfile'},
        ]
        response = self.client.post(f'/v3/survey-groups/{surveygroup.pk}/access/', data, format='json')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'common',
            'action': 'change',
        })
        self.assertEqual(result[1], {
            'access': 'common',
            'action': 'viewfile',
        })

        self.assertTrue(has_perm(self.common_group, 'change_surveygroup', surveygroup))
        self.assertTrue(has_perm(self.common_group, 'viewfile_surveygroup', surveygroup))

    def test_should_make_restricted_access(self):
        user = UserFactory()
        surveygroup = SurveyGroupFactory()
        staff_group = StaffGroupFactory(name='Test group', url='svc_test')
        group = GroupFactory(name=f'group:{staff_group.pk}')
        data = [
            {
                'access': 'restricted', 'action': 'change',
                'users': [user.uid], 'groups': [staff_group.pk],
            },
            {
                'access': 'restricted', 'action': 'viewfile',
                'users': [{'uid': user.uid}], 'groups': [staff_group.pk],
            },
        ]
        with patch.object(User, 'get_name_and_surname_with_fallback') as mock_get_name:
            mock_get_name.return_value = 'John Doe'
            response = self.client.post(f'/v3/survey-groups/{surveygroup.pk}/access/', data, format='json')
            self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'restricted',
            'action': 'change',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        })
        self.assertEqual(result[1], {
            'access': 'restricted',
            'action': 'viewfile',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        })
        self.assertEqual(mock_get_name.call_count, 2)

        self.assertTrue(has_perm(user, 'change_surveygroup', surveygroup))
        self.assertTrue(has_perm(user, 'viewfile_surveygroup', surveygroup))
        self.assertTrue(has_perm(group, 'change_surveygroup', surveygroup))
        self.assertTrue(has_perm(group, 'viewfile_surveygroup', surveygroup))

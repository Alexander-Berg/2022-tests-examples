# -*- coding: utf-8 -*-
from django.test import override_settings
from unittest.mock import patch

from events.accounts.factories import OrganizationToGroupFactory, UserFactory
from events.accounts.managers import UserManager
from events.accounts.models import OrganizationToGroup, User
from events.yauth_contrib.helpers import CookieAuthTestCase


@override_settings(IS_BUSINESS_SITE=True)
class TestCreateBrandNewProfile(CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def test_should_create_new_user_and_organization(self):
        self.client.set_cookie('123454321')

        with patch.object(UserManager, 'get_params') as mock_get_params:
            mock_get_params.return_value = {
                'default_email': 'test-new-user@yandex-team.ru',
                'fields': {
                    'login': 'test-new-user',
                },
            }
            response = self.client.get('/admin/api/v2/users/me/', HTTP_X_ORGS='12321')
            self.assertEqual(response.status_code, 200)

        user = User.objects.get(pk=response.data['id'])
        self.assertEqual(user.uid, '123454321')

        dir_ids = list(
            OrganizationToGroup.objects.filter(group__in=user.groups.all())
            .values_list('org__dir_id', flat=True)
        )
        self.assertEqual(len(dir_ids), 1)
        self.assertTrue('12321' in dir_ids)

    def test_should_link_user_with_organization(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()

        self.client.set_cookie(user.uid)

        response = self.client.get('/admin/api/v2/users/me/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['id'], user.pk)

        dir_ids = list(
            OrganizationToGroup.objects.filter(group__in=user.groups.all())
            .values_list('org__dir_id', flat=True)
        )
        self.assertEqual(len(dir_ids), 1)
        self.assertTrue(o2g.org.dir_id in dir_ids)

    def test_should_add_new_organization_and_link_with_user(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        user.groups.add(o2g.group)

        self.client.set_cookie(user.uid)

        response = self.client.get('/admin/api/v2/users/me/', HTTP_X_ORGS=f'{o2g.org.dir_id},12321')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['id'], user.pk)

        dir_ids = list(
            OrganizationToGroup.objects.filter(group__in=user.groups.all())
            .values_list('org__dir_id', flat=True)
        )
        self.assertEqual(len(dir_ids), 2)
        self.assertTrue('12321' in dir_ids)
        self.assertTrue(o2g.org.dir_id in dir_ids)

    def test_should_update_organization_list_for_user(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        user.groups.add(o2g.group)

        self.client.set_cookie(user.uid)

        response = self.client.get('/admin/api/v2/users/me/', HTTP_X_ORGS='12321')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['id'], user.pk)

        dir_ids = list(
            OrganizationToGroup.objects.filter(group__in=user.groups.all())
            .values_list('org__dir_id', flat=True)
        )
        self.assertEqual(len(dir_ids), 1)
        self.assertTrue('12321' in dir_ids)

    def test_should_create_organization_and_link_with_user(self):
        user = UserFactory()

        self.client.set_cookie(user.uid)

        response = self.client.get('/admin/api/v2/users/me/', HTTP_X_ORGS='12321')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['id'], user.pk)

        dir_ids = list(
            OrganizationToGroup.objects.filter(group__in=user.groups.all())
            .values_list('org__dir_id', flat=True)
        )
        self.assertEqual(len(dir_ids), 1)
        self.assertTrue('12321' in dir_ids)

    def test_should_create_new_user_and_link_with_organization(self):
        self.client.set_cookie('123454321')

        o2g = OrganizationToGroupFactory()

        with patch.object(UserManager, 'get_params') as mock_get_params:
            mock_get_params.return_value = {
                'default_email': 'test-new-user@yandex-team.ru',
                'fields': {
                    'login': 'test-new-user',
                },
            }
            response = self.client.get('/admin/api/v2/users/me/', HTTP_X_ORGS=o2g.org.dir_id)
            self.assertEqual(response.status_code, 200)

        user = User.objects.get(pk=response.data['id'])
        self.assertEqual(user.uid, '123454321')

        dir_ids = list(
            OrganizationToGroup.objects.filter(group__in=user.groups.all())
            .values_list('org__dir_id', flat=True)
        )
        self.assertEqual(len(dir_ids), 1)
        self.assertTrue(o2g.org.dir_id in dir_ids)

from django.conf import settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin

from .factories import GroupFactory, PermissionFactory, UserFactory


class MyProfileDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:my-profile'

    def setUp(self) -> None:
        self.user = UserFactory(is_staff=False)

    def test_url(self):
        self.assertURLNameEqual('my/profile/', base_url=settings.API_BASE_URL)

    def test_unauth(self):
        with self.assertNumQueries(0):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_get(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(2):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.data
        expected = {
            'id': self.user.id,
            'username': self.user.username,
            'first_name': self.user.first_name,
            'last_name': self.user.last_name,
        }
        self.assertEqual(expected, data)


class MyPermissionsListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:my-permissions'

    def test_url(self):
        self.assertURLNameEqual('my/permissions/', base_url=settings.API_BASE_URL)

    def test_list(self):
        user = UserFactory(is_active=True)
        group = GroupFactory()
        user.groups.add(group)
        user_permissions = PermissionFactory.create_batch(3)
        group_permissions = PermissionFactory.create_batch(3)
        common_permissions = PermissionFactory.create_batch(3)
        user.user_permissions.add(*(user_permissions + common_permissions))
        group.permissions.add(*(group_permissions + common_permissions))

        self.client.force_login(user=user)
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.data
        expected = {
            f'{perm.content_type.app_label}.{perm.codename}'
            for perm in set(user_permissions + group_permissions + common_permissions)
        }
        results = set(data)
        self.assertSetEqual(expected, results)

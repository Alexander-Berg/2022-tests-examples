from django.conf import settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin

from .factories import LabUserFactory, UserFactory


class LabMyProfileDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:my-profile'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)

    def test_url(self):
        self.assertURLNameEqual('my/profile/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_get(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(4):
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

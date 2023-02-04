import faker

from django.conf import settings
from django.test import override_settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin

from ..models import TrackerHook, TrackerHookEvent

fake = faker.Faker()

STARTREK_HOOK_TOKEN = fake.pystr(min_chars=10, max_chars=20)


@override_settings(
    STARTREK_HOOK_TOKEN=STARTREK_HOOK_TOKEN,
)
class TrackerHookEventViewTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'tracker:tracker-hook-create'

    def setUp(self):
        pass

    def test_url(self):
        self.assertURLNameEqual('hook/', base_url=settings.TRACKER_API_BASE_URL)

    def test_no_token(self):
        data = {
            'name': 'value',
        }
        with self.assertNumQueries(0):
            response = self.client.post(
                self.get_url(),
                format='json',
                data=data,
            )

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN, msg=response.data)

    def test_not_valid_token(self):
        data = {
            'name': 'value',
        }
        self.client.credentials(**{'HTTP_AUTHORIZATION': fake.pystr(min_chars=10, max_chars=20)})
        with self.assertNumQueries(0):
            response = self.client.post(
                self.get_url(),
                format='json',
                data=data,
            )

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN, msg=response.data)

    def test_create(self):
        data = {
            'name': 'value',
        }
        self.client.credentials(**{'HTTP_AUTHORIZATION': f'Bearer {STARTREK_HOOK_TOKEN}'})
        with self.assertNumQueries(1):
            response = self.client.post(
                self.get_url(),
                format='json',
                data=data,
            )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, msg=response.data)
        self.assertEqual(TrackerHookEvent.objects.filter(request_body=data).count(), 1)


@override_settings(
    STARTREK_HOOK_TOKEN=STARTREK_HOOK_TOKEN,
)
class TrackerHookViewTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'tracker:tracker-hook-v2-create'

    def setUp(self):
        pass

    def test_url(self):
        self.assertURLNameEqual('hook/v2/', base_url=settings.TRACKER_API_BASE_URL)

    def test_no_token(self):
        data = {
            'name': 'value',
        }
        with self.assertNumQueries(0):
            response = self.client.post(
                self.get_url(),
                format='json',
                data=data,
            )

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN, msg=response.data)

    def test_not_valid_token(self):
        data = {
            'name': 'value',
        }
        self.client.credentials(**{'HTTP_AUTHORIZATION': fake.pystr(min_chars=10, max_chars=20)})
        with self.assertNumQueries(0):
            response = self.client.post(
                self.get_url(),
                format='json',
                data=data,
            )

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN, msg=response.data)

    def test_create(self):
        data = {
            'name': 'value',
        }
        self.client.credentials(**{'HTTP_AUTHORIZATION': f'Bearer {STARTREK_HOOK_TOKEN}'})
        with self.assertNumQueries(1):
            response = self.client.post(
                self.get_url(),
                format='json',
                data=data,
            )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, msg=response.data)
        self.assertEqual(TrackerHook.objects.filter(data=data).count(), 1)

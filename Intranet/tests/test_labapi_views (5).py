from django.conf import settings

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin
from lms.users.tests.factories import LabUserFactory, UserFactory

from .factories import ColorThemeFactory


class LabColorThemeListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:color-theme-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.color_themes = ColorThemeFactory.create_batch(3)

    def test_url(self):
        self.assertURLNameEqual('color_themes/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        results = response.data

        expected = [
            {
                'id': ct.id,
                'slug': ct.slug,
                'name': ct.name,
                'is_active': ct.is_active,
                'course_card_gradient_color': ct.course_card_gradient_color,
                'created': serializers.DateTimeField().to_representation(ct.created),
                'modified': serializers.DateTimeField().to_representation(ct.modified),
            } for ct in self.color_themes
        ]
        self.assertEqual(results, expected)

from django.conf import settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin
from lms.users.tests.factories import UserFactory

from .factories import ColorThemeFactory


class ColorThemeListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:color-theme-list'

    def setUp(self) -> None:
        self.user = UserFactory(is_staff=True)
        self.color_themes = ColorThemeFactory.create_batch(3)

    def test_url(self):
        self.assertURLNameEqual('color_themes/', base_url=settings.API_BASE_URL)

    def test_list(self):
        self.client.force_login(user=self.user)
        with self.assertNumQueries(3):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        results = response.data

        expected = [
            {
                'id': ct.id,
                'slug': ct.slug,
                'name': ct.name,
                'course_card_gradient_color': ct.course_card_gradient_color,
            } for ct in self.color_themes
        ]
        self.assertEqual(results, expected)

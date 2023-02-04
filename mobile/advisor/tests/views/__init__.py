import requests_mock
from django.conf import settings
from django.test import Client as DjangoClient
from rest_framework import status
from uuid import uuid4

from yaphone.advisor.advisor.jafar import Jafar
from yaphone.advisor.advisor.tests.fixtures import DatabaseFixturesMixin
from yaphone.advisor.common.mocks.localization_mock import localization_values, UserInfo

HTTP_418_CLIENT_INFO_REQUIRED = 418


# noinspection PyPep8Naming
class BasicAdvisorViewTest(DatabaseFixturesMixin):
    endpoint = None
    default_params = None
    localization_project = settings.LOCALIZATION_PROJECT
    localization_project_translations = settings.LOCALIZATION_TRANSLATIONS_PROJECT

    def setUp(self):
        self.jafar_mock_adapter = requests_mock.Adapter()
        Jafar.http.mount('mock', self.jafar_mock_adapter)

        self.load_fixtures()
        self.client = DjangoClient(
            HTTP_USER_AGENT='com.yandex.launcher/2.00.qa2147483647 (Yandex Nexus 9; Android 9.2.1beta)',
            HTTP_X_YAUUID=self.client_model.uuid.hex,
            HTTP_HOST='localhost'
        )

    def tearDown(self):
        self.cleanup_fixtures()

    # Helpers
    def set_localization_key(self, key, value):
        localization_values[self.localization_project][key] = {UserInfo(): value}

    def set_translation_key(self, key, value):
        localization_values[self.localization_project_translations][key] = {UserInfo(): value}

    def get_localization_key(self, key):
        return localization_values[self.localization_project][key][UserInfo()]

    def get(self, params=None, **kwargs):
        return self.client.get(
            follow=True,
            path=self.endpoint,
            data=params or self.default_params,
            **kwargs
        )

    def request(self, params=None, **kwargs):
        return self.get(params, **kwargs)

    # Custom asserts

    def assertParameterRequired(self, parameter_name):
        params = self.default_params.copy()
        del params[parameter_name]
        response = self.get(params)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def assertParameterNotRequired(self, parameter_name):
        params = self.default_params.copy()
        del params[parameter_name]
        response = self.get(params)
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    # Default tests

    def test_ok(self, *args):
        response = self.get()
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_uuid_is_required(self, *args):
        response = self.get(HTTP_X_YAUUID=None)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_badly_formed_uuid(self, *args):
        response = self.get(HTTP_X_YAUUID='some_string_that_is_not_uuid')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_unknown_uuid(self, *args):
        response = self.get(HTTP_X_YAUUID=uuid4().hex)
        self.assertEqual(response.status_code, HTTP_418_CLIENT_INFO_REQUIRED)

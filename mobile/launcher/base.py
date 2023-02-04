import json
from django.test import TestCase, Client, override_settings
from rest_framework import status
from uuid import uuid4

from yaphone.advisor.advisor.models.client import Client as ClientModel
from yaphone.advisor.common.mocks.localization_mock import UserInfo, localization_values
from yaphone.advisor.common.mocks.test_client_data import SOME_RUSSIAN_IP


# noinspection PyPep8Naming
class APIViewMixin(object):
    # Base test for api view declaring request functions and checking simple request with default params
    endpoint = None
    method = 'GET'
    default_params = None
    localization_project = 'launcher'
    localization_project_translations = 'launcher_translations'

    def setUp(self):
        self.client = self.create_client()
        super(APIViewMixin, self).setUp()

    def set_localization_key(self, key, value, user_info=None):
        user_info = user_info or {}
        localization_values[self.localization_project][key] = {UserInfo(**user_info): value}

    def set_translation_key(self, key, value):
        localization_values[self.localization_project_translations][key] = {UserInfo(): value}

    def get_localization_key(self, key):
        return localization_values[self.localization_project][key][UserInfo()]

    def get(self, params=None, **kwargs):
        return self.client.get(
            path=self.endpoint,
            data=params or self.default_params,
            **kwargs
        )

    def post(self, data=None, **kwargs):
        return self.client.post(
            path=self.endpoint,
            data=json.dumps(data or self.default_params),
            content_type='application/json',
            **kwargs
        )

    def request(self, data=None, **kwargs):
        if self.method == 'GET':
            response = self.get(data, **kwargs)
        elif self.method == 'POST':
            response = self.post(data, **kwargs)
        else:
            raise ValueError('method should be GET or POST')
        return response

    @staticmethod
    def create_client(device_type=None):
        user_agent = 'com.yandex.launcher/2.00.qa2147483647 (Yandex Nexus 9; Android 9.2.1beta)'
        if device_type:
            user_agent = '{} {}'.format(user_agent, device_type)
        return Client(
            HTTP_USER_AGENT=user_agent,
            HTTP_X_YAUUID=uuid4().hex,
            HTTP_HOST='localhost',
            REMOTE_ADDR=SOME_RUSSIAN_IP,
            HTTP_X_YACLID1='250759',
            HTTP_ACCEPT_LANGUAGE='ru_RU',
        )

    def test_ok(self, *args):
        self.assertEqual(self.request().status_code, status.HTTP_200_OK)


# noinspection PyPep8Naming
class RequiredParamsMixin(APIViewMixin):
    # Base test that checks that all required parameters are actually required
    required_params = None
    not_required_params = None

    # Custom asserts

    def assertParameterRequired(self, parameter_name):
        params = self.default_params.copy()
        del params[parameter_name]
        response = self.request(params)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def assertParameterNotRequired(self, parameter_name):
        params = self.default_params.copy()
        del params[parameter_name]
        response = self.request(params)
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    # Required parameters test:

    def test_required_params(self):
        for parameter in self.required_params or []:
            self.assertParameterRequired(parameter)

    def test_not_required_params(self):
        for parameter in self.not_required_params or []:
            self.assertParameterNotRequired(parameter)


# noinspection PyUnusedLocal,PyPep8Naming
class StatelessViewMixin(RequiredParamsMixin):
    # Test for stateless views. Checks that all user info is required.

    def tearDown(self):
        ClientModel.objects.delete()
        super(StatelessViewMixin, self).tearDown()

    # Default tests
    def test_uuid_is_required(self, *args):
        self.assertEqual(self.request(HTTP_X_YAUUID='').status_code, status.HTTP_400_BAD_REQUEST)

    def test_clid_is_required(self, *args):
        self.assertEqual(self.request(HTTP_X_YACLID1='').status_code, status.HTTP_400_BAD_REQUEST)

    def test_language_is_required(self, *args):
        self.assertEqual(self.request(HTTP_ACCEPT_LANGUAGE='').status_code, status.HTTP_400_BAD_REQUEST)

    def test_badly_formed_uuid(self, *args):
        response = self.request(HTTP_X_YAUUID='some_string_that_is_not_uuid')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_badly_formed_clid(self, *args):
        response = self.request(HTTP_X_YACLID1='some_string_that_is_not_casting_to_int')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_badly_formed_language(self, *args):
        self.assertEqual(self.request(HTTP_ACCEPT_LANGUAGE='_-()abde').status_code, status.HTTP_400_BAD_REQUEST)


@override_settings(CACHES={'default': {'BACKEND': 'redis_cache.RedisCache', 'LOCATION': 'redis://localhost:6379/1'}})
class PingTest(TestCase):
    def test_ping(self):
        response = self.client.get('/ping')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

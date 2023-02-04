import functools
import json
import unittest

from faker import Faker

from saas.library.python.api_mixins import JsonAPIException
from saas.library.python.warden import warden_api

from saas.library.python.common_functions.tests.fake import Provider as CommonProvider
from saas.library.python.warden.tests.fake import Provider


fake = Faker()
fake.add_provider(CommonProvider)
fake.add_provider(Provider)


class TestWardenAPI(unittest.TestCase):

    @staticmethod
    def __patch_warden_api(fake_response_data, fake_response_status_code):
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), fake_response_status_code)
        warden_api._session = fake.get_session(fake_response)

    def test_success_request(self):
        fake_response_data = {'key': 'value'}
        fake_response_status_code = 200

        self.__patch_warden_api(fake_response_data, fake_response_status_code)
        response_data = warden_api._make_request('get', 'test')

        self.assertEqual(response_data, fake_response_data)

    def test_error_200_request(self):
        fake_response_data = {'error': 'test-error'}
        fake_response_status_code = 200

        self.__patch_warden_api(fake_response_data, fake_response_status_code)
        with self.assertRaises(JsonAPIException) as context_manager:
            warden_api._make_request('get', 'test')

        self.assertEqual(context_manager.exception.error, fake_response_data['error'])
        self.assertEqual(context_manager.exception.status_code, fake_response_status_code)

    def test_error_400_request(self):
        fake_response_data = {'key': 'value'}
        fake_response_status_code = 400

        self.__patch_warden_api(fake_response_data, fake_response_status_code)
        with self.assertRaises(JsonAPIException) as context_manager:
            warden_api._make_request('get', 'test')

        self.assertEqual(context_manager.exception.error, JsonAPIException.DEFAULT_ERROR)
        self.assertEqual(context_manager.exception.status_code, fake_response_status_code)

    def test_functionality_methods(self):
        fake_response_data = {}
        fake_response_status_code = 200

        self.__patch_warden_api(fake_response_data, fake_response_status_code)

        methods_with_required_kwarg_names = (
            (warden_api.add_functionality, ('functionality',)),
            (functools.partial(warden_api.update_functionality, functionality_id='test'), ('functionality',)),
            (functools.partial(warden_api.delete_functionality, functionality_id='test'), None),
        )

        for method, kwarg_names in methods_with_required_kwarg_names:
            kwargs = {}
            if kwarg_names and 'functionality' in kwarg_names:
                kwargs['functionality'] = fake.get_functionality()
            response_data = method(**kwargs)
            self.assertEqual(fake_response_data, response_data)

    def test_components_methods(self):
        fake_response_data = {}
        fake_response_status_code = 200

        self.__patch_warden_api(fake_response_data, fake_response_status_code)

        methods = (
            warden_api.get_component,
        )

        for method in methods:
            response_data = method(component_name=fake.random_string(10))
            self.assertEqual(fake_response_data, response_data)

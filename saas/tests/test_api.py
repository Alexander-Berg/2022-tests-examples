import json
import unittest

from faker import Faker

from saas.library.python.abc import AbcAPI
from saas.library.python.abc.tests.fake import Provider
from saas.library.python.common_functions.tests.fake import Provider as CommonProvider

fake = Faker()

fake.add_provider(CommonProvider)
fake.add_provider(Provider)


class TestABCAPI(unittest.TestCase):
    def setUp(self):
        self._api = AbcAPI()

    def __patch_api(self, fake_response_data, fake_response_status_code):
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), fake_response_status_code)
        self._api._session = fake.get_session(fake_response)

    def test_get_resources(self):
        fake_resources = []
        fake_resource_id = fake.get_resource_id()
        for _ in range(fake.random.randint(2, 10)):
            fake_resource = fake.get_resource({'external_id': fake_resource_id})
            fake_resources.append(fake_resource)

        fake_response_data = {'results': fake_resources}
        fake_response_status_code = 200

        self.__patch_api(fake_response_data, fake_response_status_code)

        results = self._api.get_resources(external_id=fake_resource_id)

        self.assertEqual(len(fake_resources), len(results))
        self.assertEqual(fake_resources, results)

    def test_get_resource_services(self):
        fake_resources_services = []
        fake_resource = fake.get_resource()

        for _ in range(fake.random.randint(2, 10)):
            fake_service = fake.get_service()
            fake_resource_service = fake.get_resource_service(fake_resource, fake_service)
            fake_resources_services.append(fake_resource_service)

        fake_response_data = {'results': fake_resources_services}
        fake_response_status_code = 200

        self.__patch_api(fake_response_data, fake_response_status_code)

        results = self._api.get_resources_consumers(resource_external_id=fake_resource['external_id'])

        self.assertEqual(len(fake_resources_services), len(results))
        self.assertEqual(fake_resources_services, results)

from faker import Faker

from saas.library.python.api_mixins.tests.fake import Provider as APIMixinsProvider
from saas.library.python.common_functions.tests.fake import Provider as CommonProvider


fake = Faker()
fake.add_provider(CommonProvider)

RESOURCE_MAX_ID = 10 ** 8
CONSUMER_MAX_ID = 10 ** 5


class Provider(APIMixinsProvider):
    @staticmethod
    def get_resource_id():
        return fake.random.randint(1, RESOURCE_MAX_ID)

    @staticmethod
    def get_consumer_id():
        return fake.random.randint(1, CONSUMER_MAX_ID)

    def get_resource(self, data=None):
        result = {
            'id': self.get_resource_id(),
            'external_id': self.get_resource_id(),
            'type': {
                'supplier': {
                    'id': self.get_consumer_id(),
                    'slug': 'passp'
                }
            },
            'name': fake.random_string(32),
            'attributes': []
        }
        result.update(data or {})
        return result

    def get_service(self):
        return {
            'id': self.get_consumer_id(),
            'slug': fake.random_string(16)
        }

    def get_resource_service(self, resource, service):
        dtm = fake.date_time_this_decade()

        return {
            'id': self.get_resource_id(),
            'modified_at': dtm.strftime('%Y-%m-%dT%H:%M:%S.%fZ'),
            'resource': {k: v for k, v in resource.items() if k in ['id', 'external_id', 'name', 'attributes']},
            'service': {k: v for k, v in service.items() if k in ['id', 'slug']},
            'state': 'granted'
        }

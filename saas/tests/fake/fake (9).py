from faker import Faker

from saas.library.python.warden import WardenFunctionality

from saas.library.python.api_mixins.tests.fake import Provider as APIMixinsProvider
from saas.library.python.common_functions.tests.fake import Provider as CommonProvider


fake = Faker()
fake.add_provider(CommonProvider)


class Provider(APIMixinsProvider):
    @staticmethod
    def get_functionality(test_flow: bool = True):
        return WardenFunctionality(
            name=fake.random_string(5),
            slug=fake.random_string(10),
            weight=fake.random.uniform(0, 1),
            target_queue='TESTSPI',
            description=fake.random_string(50),
            instructions=fake.random_string(50),
            test_flow=test_flow,
            component_name=fake.random_string(10)
        )

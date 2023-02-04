from faker import Faker

from saas.library.python.api_mixins.tests.fake import Provider as APIMixinsProvider
from saas.library.python.common_functions.tests.fake import Provider as CommonProvider
from saas.library.python.saas_alerts.golovan import AlertTemplate


fake = Faker()
fake.add_provider(CommonProvider)


class Provider(APIMixinsProvider):
    @staticmethod
    def get_alert_template(key=None, owners=None, content=None):
        return AlertTemplate(
            key or fake.random_string(10),
            owners=owners or [fake.random_string(10)],
            content=content or fake.random_string(50)
        )

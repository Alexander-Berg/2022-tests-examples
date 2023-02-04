import json
import requests

from faker import Faker
from faker.providers import BaseProvider
from mock import Mock

from saas.library.python.common_functions.tests.fake import Provider as CommonProvider


fake = Faker()
fake.add_provider(CommonProvider)


class Provider(BaseProvider):
    @staticmethod
    def get_session(response=None):
        session = Mock()
        session.request = Mock(spec=requests.Session.request, return_value=response)
        return session

    @staticmethod
    def get_response(content, status_code):
        response = Mock()
        response.content = content
        response.json = Mock(return_value=json.loads(content.decode()))
        response.status_code = status_code
        response.ok = status_code < 400
        return response

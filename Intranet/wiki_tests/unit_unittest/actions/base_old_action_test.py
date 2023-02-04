
# DEPRECATED

# Inherit your test cases from classes in wiki/actions/tests/rest_api_error.py
#   * BaseActionTestCase
#   * HttpActionTestCase

import warnings
from urllib.parse import urlencode

from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class ActionClient:
    client = None

    def __init__(self, client):
        self.client = client

    def url(self, name, page=None, counter='1', **kwargs):
        url = '/_actions/%s/?usersettings__%s=0&__counter__=%s'
        return url % (name, counter, counter) + '&' + urlencode(kwargs)

    def get(self, name, page=None, counter='1', action_params=None):
        if action_params is None:
            action_params = {}
        return self.client.get(self.url(name, page, counter, **action_params))

    def post(self, name, page=None, counter='1', post=None, action_params=None):
        if action_params is None:
            action_params = {}
        return self.client.post(self.url(name, page, counter, **action_params), post)


class ActionTestCase(BaseTestCase):
    def __init__(self, *args, **kwargs):
        super(ActionTestCase, self).__init__(*args, **kwargs)
        message = 'Inherit your test cases from classes in ' 'wiki/actions/tests/base_action_deprecated.py'
        warnings.warn(message, DeprecationWarning, stacklevel=2)

    def setUp(self):
        super(ActionTestCase, self).setUp()
        self.action_client = ActionClient(self.client_class())

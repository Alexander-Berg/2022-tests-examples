# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
from hamcrest import assert_that, equal_to, instance_of

from brest.core.auth.direct import DirectAuthMethod
from brest.core.tests import security
from yb_snout_proxy.tests_unit.base import TestCaseProxyAppBase

FAKE_UID = -1
FAKE_URL = '/fake'
FAKE_CONTENT_TYPE = 'fake'


class TestCaseContentType(TestCaseProxyAppBase):
    def test_get_content_type(self):
        from yb_snout_proxy.servant import VALID_CONTENT_TYPES
        from requests.exceptions import ConnectionError

        app = self._get_flask_app()

        with security.switch_auth_methods(app, [DirectAuthMethod(FAKE_UID)]):
            response = self.test_client.post(FAKE_URL, content_type=FAKE_CONTENT_TYPE)

            assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'response code must be BAD_REQUEST')

            for ct in VALID_CONTENT_TYPES:
                try:
                    self.test_client.post(FAKE_URL, content_type=ct)
                except Exception as e:
                    assert_that(e, instance_of(ConnectionError), 'exception must be instance of ConnectionError')

# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
from balance.constants import DIRECT_SERVICE_ID
from hamcrest import assert_that, equal_to

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.service import not_existing_service_id


class TestCaseService(TestCaseApiAppBase):
    BASE_API = u'/v1/service'

    def test_service(self):
        response = self.test_client.get(self.BASE_API, {'service_id': DIRECT_SERVICE_ID})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

    def test_service_without_id(self):
        response = self.test_client.get(self.BASE_API)
        assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'service_id must be required')

    def test_not_found(self, not_existing_id):
        response = self.test_client.get(self.BASE_API, {'service_id': not_existing_id})
        assert_that(response.status_code, equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')

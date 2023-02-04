# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
    has_entry,
    greater_than,
)

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


@pytest.mark.smoke
class TestCreateClient(TestCaseApiAppBase):
    BASE_API = u'/assessor/client/create'

    def test_create_client(self):
        response = self.test_client.secure_post(self.BASE_API, {})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        assert_that(
            response.get_json()['data'],
            has_entry('client_id', greater_than(0)),
        )

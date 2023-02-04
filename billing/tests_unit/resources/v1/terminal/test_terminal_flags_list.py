# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import six
import http.client as http
from hamcrest import assert_that, instance_of, equal_to, only_contains

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class TestCaseTerminalFlagsList(TestCaseApiAppBase):
    BASE_API = '/v1/terminal/flags-list'

    def test_get_terminal_flags_list(self):
        response = self.test_client.get(self.BASE_API)
        response_data = response.get_json().get('data')
        assert_that(response.status_code, equal_to(http.OK), u'Response code must be OK.')
        assert_that(
            response_data, only_contains(instance_of(six.text_type)),
            u"Should be not empty list of unicode strings.",
        )

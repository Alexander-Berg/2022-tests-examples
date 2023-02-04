# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
from hamcrest import assert_that, has_items, equal_to

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.terminal import get_any_existing_terminal_id


class TestCaseTerminal(TestCaseApiAppBase):
    BASE_API = '/v1/terminal'

    def test_get_terminal(self, any_existing_terminal_id):
        response = self.test_client.get(self.BASE_API, {'terminal_id': any_existing_terminal_id})
        assert_that(response.status_code, equal_to(http.OK), u'Response code must be OK.')

        data = response.get_json().get('data')
        assert_that(data.get('id'), equal_to(any_existing_terminal_id), u'Got wrong terminal.')

    def test_try_get_not_existing(self):
        not_existing_id = -1
        response = self.test_client.get(self.BASE_API, {'terminal_id': not_existing_id})
        assert_that(response.status_code, equal_to(http.NOT_FOUND), u'Response code must be NOT_FOUND.')

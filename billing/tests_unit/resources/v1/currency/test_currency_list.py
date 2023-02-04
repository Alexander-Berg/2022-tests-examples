# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
from hamcrest import assert_that, has_items, equal_to

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


class TestCaseCurrencyList(TestCaseApiAppBase):
    BASE_API = '/v1/currency/list'

    def test_get_currency_list(self, admin_role):
        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API)
        response_data = response.get_json().get('data')

        assert_that(response.status_code, equal_to(http.OK), u'Response code must be OK.')
        assert_that(len(response_data), u"Result is empty list.")

        fields_in_response = list(response_data[0])
        expected_fields = ['iso_code', 'iso_num_code']
        unexpected_fields_error = (
            "Unexpected fields in responce, responce fields {}, expected {}."
            .format(fields_in_response, expected_fields)
        )
        assert_that(fields_in_response, has_items(*expected_fields), unexpected_fields_error)

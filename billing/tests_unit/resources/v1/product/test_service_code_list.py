# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
from hamcrest import (
    assert_that,
    empty,
    equal_to,
    has_items,
    is_not,
)

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


class TestServiceCodeList(TestCaseApiAppBase):
    BASE_API = '/v1/product/service-code/list'

    def test_get_service_code_list(self, admin_role):
        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API)
        response_data = response.get_json().get('data')

        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK.')
        assert_that(response_data, is_not(empty), 'Result shouldn\'t be empty.')

        fields_in_response = list(response_data[0])
        expected_fields = ['code', 'description']
        unexpected_fields_error = (
            'Unexpected fields in responce, responce fields {}, expected {}.'
            .format(fields_in_response, expected_fields)
        )
        assert_that(fields_in_response, has_items(*expected_fields), unexpected_fields_error)

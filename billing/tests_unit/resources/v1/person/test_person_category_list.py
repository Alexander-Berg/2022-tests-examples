# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
from hamcrest import assert_that, equal_to, has_items

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class TestCasePersonCategoryList(TestCaseApiAppBase):
    BASE_API = '/v1/person/category/list'

    def test_get_person_category_list(self):
        response = self.test_client.get(self.BASE_API)

        assert_that(response.status_code, equal_to(http.OK), u'Response code must be OK.')
        data = response.get_json()['data']
        assert_that(len(data), u"Result is empty list.")

        fields_in_response = list(data[0])
        expected_fields = ['category', 'name']
        unexpected_fields_error = (
            u"Unexpected fields in response, response fields {}, expected {}."
            .format(fields_in_response, expected_fields)
        )
        assert_that(fields_in_response, has_items(*expected_fields), unexpected_fields_error)

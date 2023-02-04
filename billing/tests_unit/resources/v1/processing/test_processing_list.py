# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
import hamcrest as hm

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


class TestCaseProcessingList(TestCaseApiAppBase):
    BASE_API = '/v1/processing/list'

    def test_get_processing_list(self, admin_role):
        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API)
        response_data = response.get_json().get('data')

        hm.assert_that(response.status_code, hm.equal_to(http.OK), u'Response code must be OK.')
        hm.assert_that(len(response_data), u"Result is empty list.")

        fields_in_response = list(response_data[0])
        expected_fields = ['id', 'cc', 'name']
        unexpected_fields_error = (
            u"Unexpected fields in responce, responce fields {}, expected {}."
            .format(fields_in_response, expected_fields)
        )
        hm.assert_that(fields_in_response, hm.has_items(*expected_fields), unexpected_fields_error)

# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.partners import fixture_mcb_category


@pytest.mark.smoke
class TestCasePlacesMcbCategories(TestCaseApiAppBase):
    BASE_API = u'/v1/places/mcb-categories'

    def test_getting_mkb_categories(self, mcb_category):
        response = self.test_client.get(self.BASE_API)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        data = response.get_json().get('data', [])

        hm.assert_that(
            data,
            hm.has_item(hm.has_entry('id', mcb_category.id)),
            u'Result must contain created mcb category.',
        )

        fields_in_response = list(data[0])
        expected_fields = ['id', 'name']
        hm.assert_that(fields_in_response, hm.has_items(*expected_fields))

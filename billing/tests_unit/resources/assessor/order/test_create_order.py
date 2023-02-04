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

from balance.constants import ServiceId, DIRECT_PRODUCT_ID
from yb_snout_api.tests_unit.base import TestCaseApiAppBase

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import (
    create_client,
    create_client_with_intercompany,
    create_agency,
)


@pytest.mark.smoke
class TestCreateOrder(TestCaseApiAppBase):
    BASE_API = u'/assessor/order/create'

    def test_create_order(self, client):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'service_id': ServiceId.DIRECT,
                'client_id': client.id,
                'agency_id': None,
                'product_id': DIRECT_PRODUCT_ID,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        assert_that(
            response.get_json()['data'],
            has_entry('service_id', equal_to(ServiceId.DIRECT)),
            has_entry('service_order_id', greater_than(0)),
        )

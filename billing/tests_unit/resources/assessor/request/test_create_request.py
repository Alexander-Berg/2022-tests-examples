# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import json

import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    anything,
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
class TestCreateRequest(TestCaseApiAppBase):
    BASE_API = u'/assessor/request/create'
    QTY = 50

    def test_create_request(self, client):

        response = self.test_client.secure_post(u'/assessor/order/create',
                                                data={
                                                    'service_id': ServiceId.DIRECT,
                                                    'client_id': client.id,
                                                    'product_id': DIRECT_PRODUCT_ID,
                                                })

        service_order_id = response.get_json()['data']['service_order_id']

        request_data = {
            'client_id': client.id,
            'orders': json.dumps([{
                'ServiceID': ServiceId.DIRECT,
                'ServiceOrderID': service_order_id,
                'Qty': self.QTY,
            }]),
        }

        response = self.test_client.secure_post(self.BASE_API, data=request_data)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        assert_that(
            response.get_json().get('data', {}),
            has_entries(
                {
                    'request_id': anything(),
                    'user_path': anything(),
                    'admin_path': anything(),
                },
            ),
        )

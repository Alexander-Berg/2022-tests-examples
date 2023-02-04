# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
)

from balance.constants import ServiceId
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.order import create_orders


@pytest.mark.smoke
class TestCampaign(TestCaseApiAppBase):
    BASE_API = u'/assessor/order/do-campaigns'
    QTY = 300

    def test_campaign(self):
        order = create_orders([self.QTY * 2])[0]

        campaign_data = {
            'service_id': ServiceId.DIRECT,
            'service_order_id': order.service_order_id,
            'shipment_type': 'Bucks',
            'shipment_amount': self.QTY,
        }

        response = self.test_client.secure_post(self.BASE_API, data=campaign_data, is_admin=False)

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(order.completion_qty, equal_to(self.QTY))
        assert_that(order.consumes[0].completion_qty, equal_to(self.QTY))

    def test_use_current_shipment(self):
        order = create_orders([self.QTY * 2])[0]
        order.shipment.update(datetime.datetime.today(), {order.shipment_type: self.QTY})

        campaign_data = {
            'service_id': ServiceId.DIRECT,
            'service_order_id': order.service_order_id,
            'use_current_shipment': True,
        }

        response = self.test_client.secure_post(self.BASE_API, data=campaign_data, is_admin=False)

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(order.completion_qty, equal_to(self.QTY))
        assert_that(order.consumes[0].completion_qty, equal_to(self.QTY))

# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import anything, assert_that, equal_to, contains, has_entries

from balance import mapper

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, not_existing_order_id


@pytest.mark.smoke
class TestNotificationInfo(TestCaseApiAppBase):
    BASE_API = u'/assessor/notification/info'

    def test_get_notification_info(self, order):
        from yb_snout_api.resources.assessor.notification import enums

        url = u'{}?notification_opcode={}&object_id={}'.format(
            self.BASE_API,
            enums.NotificationOpcode.ORDER.name,
            order.id,
        )

        response = self.test_client.get(url)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        notify_params = self.test_session.query(mapper.ServiceNotifyParams).getone(service_id=order.service_id)
        assert_that(
            response.get_json()['data'],
            has_entries({
                'url': self.BASE_API,
                'path': contains('BalanceClient', 'NotifyOrder2'),
                'args': anything(),
                'protocol': notify_params.protocol,
                'kwargs': anything(),
            }),
        )

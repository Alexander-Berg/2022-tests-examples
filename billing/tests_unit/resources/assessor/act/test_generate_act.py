# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from datetime import datetime
import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
)

from balance import mapper
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import (
    create_client,
    create_client_with_intercompany,
    create_agency,
)


@pytest.mark.smoke
class TestGenerateActs(TestCaseApiAppBase):
    BASE_API = u'/assessor/act/generate'
    QTY = 1

    def test_generate_acts(self, invoice):
        invoice.turn_on_rows()
        consume = invoice.consumes[0]
        order = consume.order
        order.calculate_consumption(datetime.now(), {order.shipment_type: self.QTY})
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'client_id': order.client_id,
                'force_value': True,
                'time_period': datetime.now(),
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        act = self.test_session.query(mapper.Act).getone(response.get_json()['data'][0])
        assert_that(act._act_sum, equal_to(consume._completion_sum))

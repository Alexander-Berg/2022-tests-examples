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
    has_entry,
    greater_than,
    has_entries,
)

from balance.constants import ServiceId, DIRECT_PRODUCT_ID
from yb_snout_api.tests_unit.base import TestCaseApiAppBase

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.act import create_act


@pytest.mark.smoke
class TestUpdateActPaymentTerm(TestCaseApiAppBase):
    BASE_API = u'/assessor/act/update-payment-term'

    def test_update_payment_term(self, act):

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'act_id': act.id,
                'payment_term_dt': datetime.datetime.now(),
            },
            is_admin=False,
        )

        assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'response code must be BAD_REQUEST')
        assert_that(
            response.get_json(),
            has_entries({
                'description': 'Invalid parameter for function: Payment term dt is not supported for this act',
            }),
        )

    def test_update_payment_term_no_invoice(self, act):

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'act_id': None,
                'payment_term_dt': datetime.datetime.now(),
            },
        )

        assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'response code must be BAD_REQUEST')

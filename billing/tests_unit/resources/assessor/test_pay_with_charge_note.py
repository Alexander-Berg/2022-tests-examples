# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal
import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
)

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.invoice import create_personal_account
from tests import object_builder as ob
from balance import core


@pytest.mark.smoke
class TestPayWithChargeNote(TestCaseApiAppBase):
    BASE_API = u'/assessor/charge_note/pay'
    UR_BANK_PAYSYS_ID = 1003

    def test_pay_with_charge_note(self, personal_account):
        session = self.test_session
        session.config.set('SINGLE_ACCOUNT_ENABLED_SERVICES', True)
        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(client=personal_account.client),
        )
        charge_note, = core.Core(session).create_invoice(
            request_id=request.id,
            paysys_id=self.UR_BANK_PAYSYS_ID,
            person_id=personal_account.person_id,
        )

        expected_sum = Decimal(charge_note.effective_sum)
        invoice_data = {
            'invoice_id': charge_note.id,
            'payment_sum': expected_sum,
        }

        response = self.test_client.secure_post(self.BASE_API, data=invoice_data, is_admin=False)

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(personal_account.receipt_sum, equal_to(expected_sum))
        assert_that(personal_account.receipt_sum_1c, equal_to(expected_sum))

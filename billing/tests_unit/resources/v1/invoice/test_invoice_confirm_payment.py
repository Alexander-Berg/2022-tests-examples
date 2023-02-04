# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal as D
import pytest
import http.client as http
import hamcrest as hm

from balance import constants as cst, muzzle_util

from brest.core.tests import security

from yb_snout_api.resources.enums import Precision
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.utils import match_decimal_value
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role


@pytest.fixture(name='bank_payment_role')
def create_bank_payment_role():
    return create_role(
        (
            cst.PermissionCode.CREATE_BANK_PAYMENTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestConfirmPayment(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/payment/confirm'
    PAYMENT_SUM = D('1234567.89')

    def test_fail(self, invoice):
        invoice.credit = 1  # нарушение условий can_manual_turn_on

        security_data = {
            'invoice_id': invoice.id,
            'payment_sum': self.PAYMENT_SUM,
        }
        response = self.test_client.secure_post(self.BASE_API, data=security_data)
        hm.assert_that(
            response.status_code,
            hm.equal_to(http.UNPROCESSABLE_ENTITY),
            'response code must be not UNPROCESSABLE_ENTITY',
        )

    def test_confirm_payment(self, invoice):
        hm.assert_that(invoice.consumes, hm.has_length(0), 'invoice is not turned on')

        security_data = {
            'invoice_id': invoice.id,
            'payment_sum': self.PAYMENT_SUM,
        }
        response = self.test_client.secure_post(self.BASE_API, data=security_data)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        hm.assert_that(invoice.consumes, hm.is_not(None), 'Invoice has to have consumes')
        hm.assert_that(
            muzzle_util.round00(invoice._receipt_sum),
            hm.equal_to(muzzle_util.round00(self.PAYMENT_SUM)),
        )

        hm.assert_that(
            response.get_json().get('data', {}),
            hm.has_entries({
                'receipt_dt': invoice.receipt_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'manager': self.test_session.passport.gecos,
                'can_manual_turn_on': invoice.can_manual_turn_on,
                'paysys_cc': invoice.paysys.cc,
                'paysys_instant': invoice.paysys.instant,
                'suspect': invoice.suspect,
                'receipt_sum': match_decimal_value(invoice.receipt_sum, decimals=Precision.MONEY.value),
            }),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client, ans',
        [
            pytest.param(None, http.FORBIDDEN),
            pytest.param(False, http.FORBIDDEN),
            pytest.param(True, http.OK),
        ],
    )
    def test_access(self, admin_role, bank_payment_role, client, match_client, ans):
        roles = [admin_role]
        if match_client is not None:
            batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((bank_payment_role, {cst.ConstraintTypes.client_batch_id: batch_id}))
        security.set_roles(roles)

        invoice = create_invoice(client=client)
        security_data = {
            'invoice_id': invoice.id,
            'payment_sum': self.PAYMENT_SUM,
        }
        response = self.test_client.secure_post(self.BASE_API, data=security_data)
        hm.assert_that(response.status_code, hm.equal_to(ans))

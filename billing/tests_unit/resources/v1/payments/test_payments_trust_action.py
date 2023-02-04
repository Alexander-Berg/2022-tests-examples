# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import mock
import http.client as http
import hamcrest as hm

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.payments import (
    create_trust_payment,
)


@pytest.fixture(name='bank_payments_role')
def create_create_bank_payments_role():
    return create_role(
        (
            cst.PermissionCode.CREATE_BANK_PAYMENTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCaseTrustPaymentConfirm(TestCaseApiAppBase):
    BASE_API = '/v1/payments/trust-action/confirm'

    @mock.patch('muzzle.api.payments.call_trust', return_value={'status': 'success'})
    @pytest.mark.parametrize(
        'payment_firm, role_firm, match_client, ans',
        [
            pytest.param(None, cst.SENTINEL, None, http.FORBIDDEN, id='wo role'),
            pytest.param(cst.FirmId.TAXI, None, None, http.OK, id='wo constraint'),
            pytest.param(None, cst.FirmId.TAXI, None, http.OK, id='payment wo firm_id'),
            pytest.param(cst.FirmId.TAXI, cst.FirmId.TAXI, None, http.OK, id='match firm'),
            pytest.param(cst.FirmId.TAXI, cst.FirmId.DRIVE, None, http.FORBIDDEN, id='not match firm'),
            pytest.param(cst.FirmId.DRIVE, cst.FirmId.DRIVE, False, http.FORBIDDEN, id='not match client'),
            pytest.param(cst.FirmId.DRIVE, cst.FirmId.DRIVE, True, http.OK, id='match client'),
            pytest.param(cst.FirmId.DRIVE, cst.FirmId.TAXI, True, http.FORBIDDEN, id='not match firm, match client'),
            pytest.param(cst.FirmId.DRIVE, cst.FirmId.DRIVE, cst.SENTINEL, http.FORBIDDEN, id='wo client'),
        ],
    )
    def test_permission(
        self,
        _mock_api,
        admin_role,
        bank_payments_role,
        client,
        payment_firm,
        role_firm,
        match_client,
        ans,
    ):
        roles = [admin_role]
        if role_firm is not cst.SENTINEL:
            params = {}
            if role_firm is not None:
                params[cst.ConstraintTypes.firm_id] = role_firm
            if match_client is not None:
                client_batch_id = create_role_client(client if match_client else None).client_batch_id
                params[cst.ConstraintTypes.client_batch_id] = client_batch_id
            roles.append((bank_payments_role, params))
        security.set_roles(roles)

        payment = create_trust_payment(client=client, firm_id=payment_firm)
        if match_client is cst.SENTINEL:
            payment.invoice = None
            self.test_session.flush()

        res = self.test_client.secure_post(self.BASE_API, {'payment_id': payment.id})
        hm.assert_that(res.status_code, hm.equal_to(ans))

    def test_error_from_paysys(self, trust_payment):
        return_value = {'status': 'error', 'descr': 'You shall not pass!'}
        with mock.patch('muzzle.api.payments.call_trust', return_value=return_value):
            res = self.test_client.secure_post(self.BASE_API, {'payment_id': trust_payment.id})
        hm.assert_that(res.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'BALANCE_PAYSYS_EXCEPTION',
                'description': 'Error in balance paysys api call: %s' % return_value,
            }),
        )

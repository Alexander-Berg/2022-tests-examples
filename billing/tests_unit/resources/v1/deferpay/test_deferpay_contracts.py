# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import hamcrest as hm

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource


@pytest.fixture(name='view_contract_role')
def create_view_contract_role():
    return create_role((cst.PermissionCode.VIEW_CONTRACTS, {cst.ConstraintTypes.firm_id: None}))


class TestCaseClientDeferpayContracts(TestCaseApiAppBase):
    BASE_API = '/v1/deferpay/contracts'

    def test_admin_get(self, client, admin_role, view_contract_role):
        roles = [
            admin_role,
            (view_contract_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
        ]
        security.set_roles(roles)

        contracts = [
            create_general_contract(
                client=client,
                firm_id=cst.FirmId.YANDEX_OOO,
                payment_type=cst.POSTPAY_PAYMENT_TYPE,
                is_signed=self.test_session.now(),
            )
            for _i in range(2)
        ]

        # invalid contracts
        create_general_contract(
            client=client,
            firm_id=cst.FirmId.YANDEX_OOO,
            payment_type=cst.PREPAY_PAYMENT_TYPE,
            is_signed=self.test_session.now(),
        )
        create_general_contract(
            client=client,
            firm_id=cst.FirmId.MARKET,
            payment_type=cst.POSTPAY_PAYMENT_TYPE,
            is_signed=self.test_session.now(),
        )

        res = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        hm.assert_that(
            data,
            hm.contains_inanyorder(*[
                hm.has_entries(
                    id=contract.id,
                    external_id=contract.external_id,
                    type=contract.type,
                )
                for contract in contracts
            ]),
        )

    @mock_client_resource('yb_snout_api.resources.v1.deferpay.routes.contracts.ClientDeferpayContracts')
    def test_get_as_client(self, client):
        security.set_passport_client(client)
        security.set_roles([])

        client_contract = create_general_contract(
            client=client,
            firm_id=cst.FirmId.YANDEX_OOO,
            payment_type=cst.POSTPAY_PAYMENT_TYPE,
            is_signed=self.test_session.now(),
        )

        # alien contract
        create_general_contract(
            firm_id=cst.FirmId.YANDEX_OOO,
            payment_type=cst.POSTPAY_PAYMENT_TYPE,
            is_signed=self.test_session.now(),
        )

        res = self.test_client.get(self.BASE_API, {'client_id': client.id}, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entries(
                    id=client_contract.id,
                    external_id=client_contract.external_id,
                    type=client_contract.type,
                ),
            ),
        )

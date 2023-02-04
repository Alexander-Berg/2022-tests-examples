# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import get_client_role, create_role, create_admin_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract, create_spendable_contract


@pytest.fixture(name='view_contracts_role')
def create_view_contracts_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_CONTRACTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCaseClientContracts(TestCaseApiAppBase):
    BASE_API = u'/v1/client/contracts'

    @pytest.mark.parametrize('contract_types', [
        pytest.param(None, id='Without contract_types'),
        'GENERAL',
        'SPENDABLE',
        'SPENDABLE,GENERAL'
    ])
    @pytest.mark.parametrize('is_thirdparty', [
        pytest.param(None, id='Without is_thirdparty'),
        pytest.param(False, id='Without thirdparty services'),
        pytest.param(True, id='With thirdparty services'),
    ])
    def test_get_client_contracts(self, client, contract_types, is_thirdparty):
        contracts = {
            'GENERAL': {
                True: create_general_contract(client=client, services=[cst.ServiceId.TICKETS]),
                False: create_general_contract(client=client, services=[cst.ServiceId.ADDAPTER_RET])
            },
            'SPENDABLE': {
                True: create_spendable_contract(client=client, services=[cst.ServiceId.TICKETS]),
                False: create_spendable_contract(client=client, services=[cst.ServiceId.ADDAPTER_RET]),
            }
        }

        request_params = clean_dict({
            'client_id': client.id,
            'contract_types': contract_types,
            'is_thirdparty': is_thirdparty
        })
        response = self.test_client.get(self.BASE_API, request_params)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json()['data']
        contract_types = (contract_types or 'GENERAL').split(',')
        expected_contracts = []
        for contract_type in contract_types:
            if is_thirdparty is None:  # если не указали - берем все
                expected_contracts.extend(contracts[contract_type].values())
            else:
                expected_contracts.append(contracts[contract_type][is_thirdparty])
        hm.assert_that(
            data,
            hm.contains_inanyorder(*[
                hm.has_entry('id', c.id)
                for c in expected_contracts
            ]),
        )

    def test_not_found(self, not_existing_id):
        response = self.test_client.get(self.BASE_API, {'client_id': not_existing_id})
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')


@pytest.mark.permissions
class TestCaseClientContractsPermissions(TestCaseApiAppBase):
    BASE_API = u'/v1/client/contracts'

    def test_own_client(self, client, admin_role):
        security.set_roles([])
        security.set_passport_client(client)

        general_contract = create_general_contract(client=client)
        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entry('id', general_contract.id),
            ),
        )

    def test_nobody(self, client, admin_role):
        security.set_roles([admin_role])
        res = self.test_client.get(self.BASE_API)
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))

    @pytest.mark.parametrize(
        'w_perm',
        [True, False],
    )
    def test_permission(self, admin_role, view_contracts_role, client, w_perm):
        roles = [admin_role]
        if w_perm:
            roles.append(view_contracts_role)
        security.set_roles(roles)

        contract = create_general_contract(client=client)
        res = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        contract_match = hm.contains(hm.has_entry('id', contract.id)) if w_perm else hm.empty()
        hm.assert_that(data, contract_match)

    @pytest.mark.parametrize(
        'match_client',
        [True, False],
    )
    def test_client_constraint(
            self,
            admin_role,
            view_contracts_role,
            match_client,
    ):
        role_client_1 = create_role_client()
        role_client_2 = create_role_client()
        client_batch_id = role_client_1.client_batch_id if match_client else role_client_2.client_batch_id
        roles = [
            admin_role,
            (view_contracts_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]
        security.set_roles(roles)

        contract = create_general_contract(client=role_client_1.client)
        res = self.test_client.get(self.BASE_API, {'client_id': role_client_1.client.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        contract_match = hm.contains(hm.has_entry('id', contract.id)) if match_client else hm.empty()
        hm.assert_that(data, contract_match)

    def test_perm_constraints(
            self,
            admin_role,
            view_contracts_role,
            role_client,
    ):
        role_client_2 = create_role_client()
        roles = [
            admin_role,
            (view_contracts_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id, cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
            (view_contracts_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id, cst.ConstraintTypes.firm_id: cst.FirmId.MARKET}),
            (view_contracts_role, {cst.ConstraintTypes.client_batch_id: role_client_2.client_batch_id, cst.ConstraintTypes.firm_id: cst.FirmId.DRIVE}),
        ]
        security.set_roles(roles)

        required_contracts = [
            create_general_contract(client=role_client.client, firm_id=cst.FirmId.YANDEX_OOO),
            create_general_contract(client=role_client.client, firm_id=cst.FirmId.MARKET),
            create_general_contract(client=role_client.client, firm_id=None),
        ]
        create_general_contract(client=role_client.client, firm_id=cst.FirmId.DRIVE)
        create_general_contract(client=role_client_2.client, firm_id=cst.FirmId.YANDEX_OOO)

        res = self.test_client.get(self.BASE_API, {'client_id': role_client.client.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains_inanyorder(*[
                hm.has_entry('id', c.id)
                for c in required_contracts
            ]),
        )

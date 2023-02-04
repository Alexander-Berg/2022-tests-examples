# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import http.client as http
import pytest
import hamcrest as hm

from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.service import create_service


@pytest.fixture(name='view_contract_role')
def create_view_contract_role():
    return create_role((
        cst.PermissionCode.VIEW_CONTRACTS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


class TestGetContracts(TestCaseApiAppBase):
    BASE_API = '/v1/contract/list'

    @staticmethod
    def _get_roles(roles_w_constraints):
        roles = []
        for role, role_firm_ids in roles_w_constraints:
            if role_firm_ids is cst.SENTINEL:
                continue
            elif not role_firm_ids:
                roles.append(role)
            else:
                roles.extend([
                    (role, {cst.ConstraintTypes.firm_id: firm_id})
                    for firm_id in role_firm_ids
                ])
        return roles

    @pytest.mark.smoke
    def test_get_contracts(self, admin_role, view_contract_role, client):
        from yb_snout_api.resources.enums import SortOrderType
        from yb_snout_api.resources.v1.contract.enums import ContractDateType, SortKeyType

        now = datetime.datetime.now()
        security.set_roles([admin_role, view_contract_role])

        contracts = [
            create_general_contract(
                client=client,
                external_id='snout_test_contract_%s' % delta,
                dt=now + datetime.timedelta(minutes=delta),
                is_signed=now + datetime.timedelta(days=delta),
                w_ui=True,
            )
            for delta in [1, 2, 22, 3]
        ]
        frmt = '%Y-%m-%dT%H:%M:%S'
        parametrizations = [
            ({'client_id': client.id, 'sort_key': SortKeyType.DT.name, 'sort_order': SortOrderType.DESC.name,
              'pagination_pn': 1, 'pagination_ps': 4},
             sorted(contracts, key=lambda c: c.col0.dt, reverse=True)),
            ({'client_id': client.id, 'sort_key': SortKeyType.CONTRACT_EID.name, 'sort_order': SortOrderType.ASC.name},
             contracts),
            ({'client_id': client.id, 'sort_key': SortKeyType.CONTRACT_EID.name, 'sort_order': SortOrderType.DESC.name},
             contracts[::-1]),
            ({'client_id': client.id, 'sort_key': SortKeyType.CONTRACT_EID.name, 'sort_order': SortOrderType.DESC.name,
              'pagination_pn': 2, 'pagination_ps': 2},
             contracts[::-1][2:]),
            ({'client_id': client.id, 'date_type': ContractDateType.DT.name,
              'dt_from': (now + datetime.timedelta(minutes=3)).strftime(frmt),
              'dt_to': (now + datetime.timedelta(minutes=5)).strftime(frmt)},
             contracts[2:][::-1]),
            ({'client_id': client.id, 'date_type': ContractDateType.IS_SIGNED.name,
              'dt_from': (now + datetime.timedelta(days=3)).strftime(frmt)},
             contracts[2:][::-1]),
        ]

        for ind, parametrization in enumerate(parametrizations):
            params, required_contracts = parametrization
            res = self.test_client.get(
                self.BASE_API,
                params=params,
            )
            hm.assert_that(res.status_code, hm.equal_to(http.OK), 'Failed attempt #%s by status' % ind)

            data = res.get_json()
            hm.assert_that(
                data['data'],
                hm.has_entries({
                    'items': hm.contains(*[
                        hm.has_entry('contract_id', contract.id)
                        for contract in required_contracts
                    ]),
                }),
                'Failed attempt #%s' % ind,
            )

    def test_services(self, client):
        contract = create_general_contract(
            client=client,
            ui_services=' Тест 1, Директ: что-то там, Гроза',
            w_ui=True,
        )
        res = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data.get('items', []),
            hm.contains(hm.has_entries({
                'contract_id': contract.id,
                'services': ['Тест 1', 'Директ: что-то там', 'Гроза'],
            })),
        )

    @pytest.mark.parametrize(
        'contract_eid_like',
        [True, False],
    )
    def test_contract_eid_like(self, contract_eid_like):
        """Поиск с лидирующем процентом справа
        """
        contract_eid = '01-unique_eid/%s-01' % ob.get_big_number()
        create_general_contract(external_id=contract_eid, w_ui=True)

        res = self.test_client.get(
            self.BASE_API,
            {'contract_eid': contract_eid[3:-3], 'contract_eid_like': contract_eid_like},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        contracts_match = hm.contains(hm.has_entries({'contract_eid': contract_eid})) if contract_eid_like else hm.empty()
        hm.assert_that(
            res.get_json()['data']['items'],
            contracts_match,
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'contract_firm_ids, role_firm_ids',
        (
            pytest.param([None], cst.SENTINEL, id='wo firm - wo role'),
            pytest.param([None], [], id='wo firm - w role'),
            pytest.param([None], [cst.FirmId.YANDEX_OOO], id='wo firm - w role w constraints'),
            pytest.param([cst.FirmId.YANDEX_OOO], [], id='contracts w firm - w role wo constraints'),
            pytest.param(
                [cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE, cst.FirmId.CLOUD, None],
                [cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE, cst.FirmId.BUS],
                id='contracts w firms - w role w constraints',
            ),
        ),
    )
    def test_filtering_by_user_constraints_firm_id(
            self,
            admin_role,
            view_contract_role,
            service,
            contract_firm_ids,
            role_firm_ids,
    ):
        """Фильтрация по праву ViewContracts и фирме"""
        roles = self._get_roles([(view_contract_role, role_firm_ids)])
        roles.append(admin_role)
        security.set_roles(roles)

        required_contracts = []
        for firm_id in contract_firm_ids:
            contract = create_general_contract(services=[service.id], firm_id=firm_id, w_ui=True)
            if (
                    role_firm_ids is not cst.SENTINEL
                    and
                    (firm_id is None or not role_firm_ids or firm_id in role_firm_ids)
            ):
                required_contracts.append(contract)

        res = self.test_client.get(
            self.BASE_API,
            params={'service_id': service.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        if required_contracts:
            contracts_match = hm.contains_inanyorder(*[
                hm.has_entries({
                    'services': hm.contains('{%s: 1}' % service.id),
                    'contract_id': c.id,
                    'commission': c.commission,
                    'contract_eid': c.external_id,
                    'client_id': c.client_id,
                })
                for c in required_contracts
            ])
        else:
            contracts_match = hm.empty()
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': len(required_contracts),
                'items': contracts_match,
            }),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client, firm_id, ans',
        (
            pytest.param(True, cst.FirmId.YANDEX_OOO, True, id='w client w firm'),
            pytest.param(True, None, True, id='w client wo firm'),
            pytest.param(False, cst.FirmId.YANDEX_OOO, False, id='wo client w firm'),
            pytest.param(True, cst.FirmId.DRIVE, False, id='w client w wrong firm'),
        ),
    )
    def test_filtering_by_client_constraint(
            self,
            client,
            admin_role,
            service,
            view_contract_role,
            match_client,
            firm_id,
            ans,
    ):
        """Фильтрация по праву ViewContracts и клиенту"""
        role_client = create_role_client(client=client if match_client else create_client())

        roles = [
            admin_role,
            (view_contract_role, {
                cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO,
                cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
            }),
        ]
        security.set_roles(roles)

        contract = create_general_contract(client=client, services=[service.id], firm_id=firm_id, w_ui=True)
        res = self.test_client.get(
            self.BASE_API,
            params={'service_id': service.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        contracts_match = hm.contains(hm.has_entry('contract_id', contract.id)) if ans else hm.empty()
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': int(ans),
                'items': contracts_match,
            }),
        )

    @pytest.mark.parametrize(
        'match_client',
        [True, False],
    )
    def test_perm_for_agency(
            self,
            client,
            admin_role,
            service,
            view_contract_role,
            match_client,
    ):
        """По agency_id тоже фильтруем"""
        role_client = create_role_client(client=client if match_client else create_client())

        roles = [
            admin_role,
            (view_contract_role, {
                cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
            }),
        ]
        security.set_roles(roles)

        contract = create_general_contract(client=client, services=[service.id], w_ui=True, ui_agency=True)
        res = self.test_client.get(
            self.BASE_API,
            params={'service_id': service.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        contracts_match = hm.contains(hm.has_entry('contract_id', contract.id)) if match_client else hm.empty()
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': int(match_client),
                'items': contracts_match,
            }),
        )

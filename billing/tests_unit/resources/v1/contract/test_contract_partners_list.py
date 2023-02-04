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
from flask.helpers import url_quote

from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.contract import create_partner_contract, create_distribution_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.firm import create_firm
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


@pytest.mark.smoke
class TestGetPartnerContracts(TestCaseApiAppBase):
    BASE_API = '/v1/contract/partner-contracts'

    def test_get_contracts(self, admin_role, view_contract_role, client):
        from yb_snout_api.resources.enums import SortOrderType
        from yb_snout_api.resources.v1.contract.enums import (
            PartnerContractDateType,
            PartnerContractSortKey,
        )

        now = self.test_session.now()
        security.set_roles([admin_role, view_contract_role])

        contracts = [
            create_partner_contract(
                client=client,
                eid='snout_test_contract_%s' % ob.get_big_number(),
                is_faxed=now - datetime.timedelta(days=i),
                w_ui=True,
                doc_set=2,
            )
            for i in range(3)
        ]
        create_partner_contract(client=client, w_ui=True, doc_set=3)
        create_partner_contract(client=client, is_faxed=(now - datetime.timedelta(days=4)), w_ui=True, doc_set=2)

        res = self.test_client.get(
            self.BASE_API,
            params={
                'client_id': client.id,
                'doc_set': 2,
                'date_type': PartnerContractDateType.FAXED.name,
                'dt_from': (now - datetime.timedelta(days=2)).isoformat(),
                'dt_to': now.isoformat(),
                'sort_key': PartnerContractSortKey.CONTRACT_EID.name,
                'sort_order': SortOrderType.DESC.name,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': 3,
                'items': hm.contains(*[
                    hm.has_entries({
                        'contract_id': c.id,
                        'contract_eid': c.external_id,
                        'doc_set': 2,
                        'bill_intervals': 1,
                    })
                    for c in sorted(contracts[::-1], key=lambda x: x.external_id, reverse=True)
                ]),
            }),
        )

    @pytest.mark.parametrize(
        'contract_eid_like',
        [True, False],
    )
    def test_contract_eid_like(self, contract_eid_like):
        """Поиск с лидирующем процентом справа
        """
        contract_eid = '01-unique_eid/%s-01' % ob.get_big_number()
        create_partner_contract(eid=contract_eid, w_ui=True)

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

    @pytest.mark.parametrize(
        'pass_firm_id',
        [True, False],
    )
    def test_get_contracts_by_firm(self, client, firm, pass_firm_id):
        """
        Поиск по фирме
        """
        yandex_contracts = [
            create_partner_contract(
                client=client,
                w_ui=True,
                firm_id=cst.FirmId.YANDEX_OOO,
            )
            for _ in range(3)
        ]

        other_contracts = [
            create_partner_contract(
                client=client,
                w_ui=True,
                firm_id=firm.id,
            )
            for _ in range(3)
        ]

        params = {
            'client_id': client.id,
        }
        if pass_firm_id:
            params['firm_id'] = firm.id

        res = self.test_client.get(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        expected_contracts = other_contracts if pass_firm_id else yandex_contracts + other_contracts

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': len(expected_contracts),
                'items': hm.contains_inanyorder(*[
                    hm.has_entries({
                        'client_id': client.id,
                        'contract_id': c.id,
                        'contract_eid': c.external_id,
                        'firm': c.firm.id,
                    })
                    for c in expected_contracts
                ]),
            }),
        )

    @pytest.mark.parametrize(
        'pass_platform_type',
        [True, False],
    )
    def test_get_contracts_by_platform_type(self, client, pass_platform_type):
        """
        Поиск по типу платформы
        """
        contracts1 = [
            create_distribution_contract(
                client=client,
                w_ui=True,
                platform_type=1,
            )
            for _ in range(3)
        ]

        contracts2 = [
            create_distribution_contract(
                client=client,
                w_ui=True,
                platform_type=2,
            )
            for _ in range(3)
        ]

        params = {
            'client_id': client.id,
        }
        if pass_platform_type:
            params['platform_type'] = 2

        res = self.test_client.get(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        expected_contracts = contracts2 if pass_platform_type else contracts1 + contracts2

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': len(expected_contracts),
                'items': hm.contains_inanyorder(*[
                    hm.has_entries({
                        'client_id': client.id,
                        'contract_id': c.id,
                        'contract_eid': c.external_id,
                        'platform_type': c.col0.platform_type,
                    })
                    for c in expected_contracts
                ]),
            }),
        )


@pytest.mark.permissions
@pytest.mark.slow
class TestGetPartnerContractsPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/contract/partner-contracts'

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
            contract = create_partner_contract(eid='snout_%s' % ob.get_big_number(), services=[service.id], firm_id=firm_id, w_ui=True)
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
                    'contract_id': c.id,
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

        contract = create_partner_contract(eid='snout_%s' % ob.get_big_number(), client=client, firm_id=firm_id, w_ui=True)
        res = self.test_client.get(
            self.BASE_API,
            params={'contract_eid': contract.external_id},
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


class TestGetPartnerContractsXsl(TestCaseApiAppBase):
    BASE_API = '/v1/contract/partner-contracts/xls'

    @pytest.mark.smoke
    def test_get_contracts(self, admin_role, view_contract_role, client):
        from yb_snout_api.resources import enums

        security.set_roles([admin_role, view_contract_role])

        contract = create_partner_contract(
            eid='snout_%s' % ob.get_big_number(),
            client=client,
            w_ui=True,
        )

        response = self.test_client.get(
            self.BASE_API,
            params={'contract_eid': contract.external_id, 'filename': 'Te$tТесТ'},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        headers = response.headers
        hm.assert_that(response.content_type, hm.equal_to(enums.Mimetype.XLS.value))
        hm.assert_that(
            headers,
            hm.has_items(
                hm.contains(
                    'Content-Disposition',
                    hm.contains_string('filename*=UTF-8\'\'' + url_quote('Te$tТесТ.xls')),
                ),
                hm.contains('Content-Type', enums.Mimetype.XLS.value),
            ),
        )

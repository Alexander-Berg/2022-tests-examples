# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import hamcrest as hm
import http.client as http
import pytest

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.firm import create_firm
from yb_snout_api.tests_unit.fixtures.contract import create_distribution_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role


@pytest.fixture(name='view_contract_role')
def create_view_contract_role():
    return create_role((
        cst.PermissionCode.VIEW_CONTRACTS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


class TestDistributionLinkedContract(TestCaseApiAppBase):
    BASE_API = '/v1/contract/distribution-linked-contracts'

    @pytest.mark.smoke
    def test_linked_contract(self):
        contract = create_distribution_contract(contract_type=cst.DistributionContractType.GROUP_OFFER, external_id='unique 543')
        linked_1 = create_distribution_contract(parent_contract_id=contract.id, external_id='1', contract_type=cst.DistributionContractType.UNIVERSAL, supplements={1: 1, 3: 1})

        res = self.test_client.get(self.BASE_API, {'contract_id': contract.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        hm.assert_that(
            data,
            hm.has_entries({
                'contract': hm.has_entries({
                    'id': contract.id,
                    'external_id': contract.external_id,
                }),
                'linked_contracts_count': 1,
                'linked_contracts': hm.contains(
                    hm.has_entries({
                        'id': linked_1.id,
                        'external_id': '1',
                        'attributes': hm.contains(
                            hm.has_entries({'group': 1, 'name': 'contract_type', 'value': 'Универсальный', 'highlighted': True, 'caption': 'Тип договора', 'type': 'refselect'}),
                            hm.has_entries({'group': 2, 'name': 'supplements', 'value': 'Разделение доходов, Поиски', 'highlighted': False, 'caption': 'Приложения договора', 'type': 'checkboxes'}),
                        ),
                    }),
                ),
            }),
        )


@pytest.mark.slow
@pytest.mark.permissions
class TestDistributionLinkedContractPermission(TestCaseApiAppBase):
    BASE_API = '/v1/contract/distribution-linked-contracts'

    @pytest.mark.parametrize(
        'w_role, match_firm, match_client, ans',
        [
            pytest.param(None, False, False, False, id='wo role'),
            pytest.param(True, True, True, True, id='w role w firm w client'),
            pytest.param(True, True, False, False, id='w role w firm w client'),
            pytest.param(True, False, True, False, id='w role w firm w client'),
        ],
    )
    def test_contract(self, admin_role, view_contract_role, client,
                      w_role, match_firm, match_client, ans):
        firm_id = cst.FirmId.YANDEX_OOO
        roles = [admin_role]
        if w_role:
            client_batch_id = create_role_client(client if match_client else None).client_batch_id
            params = {
                cst.ConstraintTypes.firm_id: firm_id if match_firm else cst.FirmId.MARKET,
                cst.ConstraintTypes.client_batch_id: client_batch_id,
            }
            roles.append((view_contract_role, params))
        security.set_roles(roles)

        contract = create_distribution_contract(firm_id=firm_id, client=client, contract_type=cst.DistributionContractType.OFFER)
        res = self.test_client.get(
            self.BASE_API,
            {'contract_id': contract.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if ans else http.FORBIDDEN))

    def test_linked_contracts(self, admin_role, view_contract_role):
        """Проверяем различные сочетания firm_id + client_id для просмотра связанных договоров
        """
        role_clients = [create_role_client() for _i in range(4)]
        firm_ids = [create_firm().id for _i in range(4)]

        roles = [
            admin_role,
            (view_contract_role, {cst.ConstraintTypes.firm_id: firm_ids[0], cst.ConstraintTypes.client_batch_id: role_clients[0].client_batch_id}),
            (view_contract_role, {cst.ConstraintTypes.firm_id: firm_ids[1]}),
            (view_contract_role, {cst.ConstraintTypes.client_batch_id: role_clients[1].client_batch_id}),
            (view_contract_role, {cst.ConstraintTypes.firm_id: firm_ids[2], cst.ConstraintTypes.client_batch_id: role_clients[2].client_batch_id}),
        ]
        security.set_roles(roles)

        contract = create_distribution_contract(firm_id=firm_ids[0], client=role_clients[0].client, contract_type=cst.DistributionContractType.GROUP_OFFER)

        requred_linked_contracts = [
            create_distribution_contract(firm_id=firm_ids[1], client=role_clients[3].client, parent_contract_id=contract.id, external_id='1'),
            create_distribution_contract(firm_id=firm_ids[3], client=role_clients[1].client, parent_contract_id=contract.id, external_id='2'),
            create_distribution_contract(firm_id=firm_ids[2], client=role_clients[2].client, parent_contract_id=contract.id, external_id='3'),
            create_distribution_contract(firm_id=None, client=role_clients[2].client, parent_contract_id=contract.id, external_id='4'),
            create_distribution_contract(firm_id=None, client=role_clients[3].client, parent_contract_id=contract.id, external_id='5'),
        ]
        create_distribution_contract(firm_id=firm_ids[2], client=role_clients[0].client, parent_contract_id=contract.id)
        create_distribution_contract(firm_id=firm_ids[0], client=role_clients[2].client, parent_contract_id=contract.id)
        create_distribution_contract(firm_id=firm_ids[3], client=role_clients[3].client, parent_contract_id=contract.id)

        res = self.test_client.get(
            self.BASE_API,
            {'contract_id': contract.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'contract': hm.has_entries({'id': contract.id}),
                'linked_contracts_count': len(requred_linked_contracts),
                'linked_contracts': hm.contains(*[
                    hm.has_entries({
                        'id': c.id,
                        'external_id': c.external_id,
                        'idx': idx,
                    })
                    for idx, c in enumerate(requred_linked_contracts)
                ]),
            }),
        )

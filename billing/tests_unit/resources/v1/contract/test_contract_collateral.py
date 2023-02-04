# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import hamcrest as hm
import http.client as http
import pytest

from billing.contract_iface.cmeta import general
from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract, create_collateral
from yb_snout_api.tests_unit.fixtures.common import not_existing_id
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource


@pytest.fixture(name='view_contract_role')
def create_view_contract_role():
    return create_role((
        cst.PermissionCode.VIEW_CONTRACTS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


class TestContract(TestCaseApiAppBase):
    BASE_API = '/v1/contract/collaterals'

    def test_contract_not_found(self):
        res = self.test_client.get(
            self.BASE_API,
            params={'contract_id': not_existing_id(ob.ContractBuilder)},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
        data = res.get_json()
        hm.assert_that(
            data,
            hm.has_entries({'error': 'CONTRACT_NOT_FOUND'}),
        )

    def test_contract_wo_collaterals(self, general_contract):
        res = self.test_client.get(
            self.BASE_API,
            {'contract_id': general_contract.id, 'all_collaterals': True},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json().get('data', {}),
            hm.has_entries({
                'total_count': 0,
                'items': hm.empty(),
                'request_params': hm.has_entries({
                    'all_collaterals': True,
                    'pagination_pn': 1,
                    'pagination_ps': 10,
                    'sort_order': 'asc',
                    'sort_key': 'dt',
                }),
            }),
        )

    @pytest.mark.parametrize(
        'all_collaterals, ps, pn, sort_order, sort_key, res_idx,',
        [
            pytest.param(0, 5, 1, 'DESC', 'DT', [0, 1, 2], id='wo all_collaterals'),
            pytest.param(1, 5, 1, 'DESC', 'DT', [0, 1, 2, 3], id='all_collaterals'),
            pytest.param(1, 5, 1, 'ASC', 'DT', [3, 2, 1, 0], id='reversed'),
            pytest.param(1, 5, 1, 'ASC', 'NUM', [2, 1, 0, 3], id='by num'),
            pytest.param(1, 2, 2, 'DESC', 'DT', [2, 3], id='second page'),
        ],
    )
    def test_all_collaterals(
        self,
        general_contract,
        all_collaterals,
        ps,
        pn,
        sort_order,
        sort_key,
        res_idx,
    ):
        session = self.test_session
        finish_dt = session.now() + datetime.timedelta(days=1)
        c = general_contract

        def now():
            return session.now()

        _collaterals = [  # noqa: F841
            create_collateral(c, num='04', collateral_type=general.collateral_types[80], dt=now(), finish_dt=finish_dt, is_cancelled=now(), memo='Aaaa'),
            create_collateral(c, num='01', collateral_type=general.collateral_types[1001], dt=now(), services={11: True}, is_booked=1, is_booked_dt=now()),
            create_collateral(c, num='02', collateral_type=general.collateral_types[1003], dt=now(), is_signed=now(), memo=u'Test memооо'),
            create_collateral(c, num='03', collateral_type=general.collateral_types[80], dt=now(), finish_dt=finish_dt, is_signed=now(), is_cancelled=now()),
        ]
        session.flush()

        res = self.test_client.get(
            self.BASE_API,
            {
                'contract_id': c.id,
                'all_collaterals': all_collaterals,
                'pagination_pn': pn,
                'pagination_ps': ps,
                'sort_order': sort_order,
                'sort_key': sort_key,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        collaterals_dict = [
            {'id': hm.not_none(), 'num': '03', 'dt': hm.not_none(), 'collateral_type': u'продление договора', 'finish_dt': hm.not_none(), 'memo': u'', 'is_cancelled': hm.not_none(), 'is_signed': hm.not_none(), 'collateral_pn': 1 if all_collaterals else 1},
            {'id': hm.not_none(), 'num': '02', 'dt': hm.not_none(), 'collateral_type': u'прочее', 'finish_dt': hm.not_none(), 'memo': u'Test memооо', 'is_signed': hm.not_none(), 'is_cancelled': None, 'collateral_pn': 2 if all_collaterals else 2},
            {'id': hm.not_none(), 'num': '01', 'dt': hm.not_none(), 'collateral_type': u'изменение сервисов', 'finish_dt': hm.not_none(), 'memo': u'', 'is_booked': hm.not_none(), 'is_signed': None, 'collateral_pn': 3 if all_collaterals else 3},
            {'id': hm.not_none(), 'num': '04', 'dt': hm.not_none(), 'collateral_type': u'продление договора', 'finish_dt': hm.not_none(), 'memo': u'Aaaa', 'is_cancelled': hm.not_none(), 'is_signed': None, 'collateral_pn': 4},
        ]
        collaterals_match = []
        for idx in res_idx:
            d = collaterals_dict[idx].copy()
            collaterals_match.append(hm.has_entries(d))

        hm.assert_that(
            res.get_json().get('data', {}),
            hm.has_entries({
                'request_params': hm.has_entries({
                    'all_collaterals': all_collaterals,
                    'pagination_pn': pn,
                    'pagination_ps': ps,
                    'sort_order': sort_order.lower(),
                    'sort_key': sort_key.lower(),
                }),
                'total_count': 4 if all_collaterals else 3,
                'items': hm.contains(*collaterals_match),
            }),
        )


@pytest.mark.permissions
@pytest.mark.slow
class TestContractCollateralPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/contract/collaterals'

    @pytest.mark.parametrize(
        'same_client_in_role_and_contract, firm_id, allowed',
        (
            pytest.param(True, cst.FirmId.YANDEX_OOO, True, id='w client w firm / OK'),
            pytest.param(False, cst.FirmId.YANDEX_OOO, False, id='wo client w firm / FORBIDDEN'),
            pytest.param(True, cst.FirmId.DRIVE, False, id='w client w wrong firm / OK'),
        ),
    )
    def test_client_constraint(self, client, admin_role, view_contract_role, same_client_in_role_and_contract, firm_id, allowed):
        role_client = create_role_client(client=client if same_client_in_role_and_contract else create_client())

        roles = [
            admin_role,
            (view_contract_role, {
                cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO,
                cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
            }),
        ]
        security.set_roles(roles)

        contract = create_general_contract(client=client, firm_id=firm_id)
        res = self.test_client.get(
            self.BASE_API,
            params={'contract_id': contract.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if allowed else http.FORBIDDEN))

    @pytest.mark.parametrize(
        'owner',
        [True, False],
    )
    @mock_client_resource('yb_snout_api.resources.v1.contract.routes.collaterals.ContractCollaterals')
    def test_client_owner(self, client, owner):
        security.set_roles([])
        if owner:
            security.set_passport_client(client)

        contract = create_general_contract(client=client)
        res = self.test_client.get(
            self.BASE_API,
            {'contract_id': contract.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if owner else http.FORBIDDEN))

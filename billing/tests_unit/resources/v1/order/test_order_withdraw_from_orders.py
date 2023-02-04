# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from builtins import str as text
from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    equal_to,
    empty,
    has_entry,
    has_entries,
    has_item,
    has_length,
)

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import get_attrib_by_name, clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, create_order_w_firm


@pytest.fixture(name='view_order_role')
def create_view_order_role():
    return create_role((
        cst.PermissionCode.VIEW_ORDERS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


class TestWithdrawFromOrders(TestCaseApiAppBase):
    BASE_API = u'/v1/order/withdraw/from-orders'
    invalid_client_id = 1234

    @pytest.mark.smoke
    @pytest.mark.parametrize(
        'parameters, use_real_client',
        [
            ([('client_id', 'client_id')], False),
            ([('service_id', 'service_id'), ('service_order_id_prefix', 'service_order_id')], True),
            ([('service_cc', 'service.cc'), ('service_order_id_prefix', 'service_order_id')], True),
        ],
        ids=[
            'client_id',
            'service_id & service_order_id',
            'service_cc & service_order_id',
        ],
    )
    def test_get_by_order_id(self, order, parameters, use_real_client):
        params = {key: get_attrib_by_name(order, attr_name) for key, attr_name in parameters}
        if 'client_id' not in params:
            params['client_id'] = order.client_id if use_real_client else self.invalid_client_id

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', [])
        assert_that(
            data,
            contains(has_entries({
                'client_id': order.client_id,
                'order_id': order.id,
            })),
        )

    def test_order_id(self, client):
        order1 = create_order(client=client)
        order2 = create_order(client=client)

        response = self.test_client.get(
            self.BASE_API,
            {
                'order_id': order1.id,
                'client_id': self.invalid_client_id,  # должен использоваться клиент из заказа
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', [])
        assert_that(
            data,
            contains(has_entries({
                'client_id': client.id,
                'order_id': order2.id,
            })),
        )

    @pytest.mark.permissions
    def test_get_by_service_order_id(self, admin_role, view_order_role):
        security.set_roles([admin_role, view_order_role])

        order = create_order()
        order_id = order.id
        response = self.test_client.get(
            self.BASE_API,
            {
                'service_id': order.service_id,
                'service_order_id_prefix': text(order.service_order_id)[:3],  # prefix
                'client_id': order.client_id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', [])
        assert_that(data, has_length(1))
        assert_that(
            data,
            has_item(has_entries({'order_id': order_id, 'service_id': order.service_id})),
        )

    def test_get_by_service_order_id_fail(self, order):
        response = self.test_client.get(
            self.BASE_API,
            {
                'service_id': order.service_id,
                'client_id': order.client_id,
            },
        )
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'response code must be INTERNAL_SERVER_ERROR',
        )

    def test_required_client(self, order):
        response = self.test_client.get(self.BASE_API, {'order_id': order.id})
        assert_that(response.status_code, http.BAD_REQUEST, 'response code must be BAD_REQUEST')


@pytest.mark.permissions
class TestWithdrawFromOrdersPermissions(TestCaseApiAppBase):
    BASE_API = u'/v1/order/withdraw/from-orders'

    @staticmethod
    def _get_roles(role, role_firm_ids, role_client, match_client):
        if match_client is None:
            client_batch_id = None
        else:
            client_batch_id = role_client.client_batch_id if match_client else create_role_client().client_batch_id
        return [
            (
                role,
                clean_dict(
                    {cst.ConstraintTypes.firm_id: firm_id, cst.ConstraintTypes.client_batch_id: client_batch_id}),
            )
            for firm_id in role_firm_ids
        ]

    def test_filtered_by_firm(self, client, admin_role, view_order_role):
        roles = [
            admin_role,
            (view_order_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
            (view_order_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD}),
        ]
        security.set_roles(roles)

        required_orders = [
            create_order_w_firm(client=client, firm_ids=[]),
            create_order_w_firm(client=client, firm_ids=[cst.FirmId.YANDEX_OOO]),
            create_order_w_firm(client=client, firm_ids=[cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD]),
        ]
        create_order_w_firm(client=client, firm_ids=[cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD, cst.FirmId.DRIVE])
        response = self.test_client.get(
            self.BASE_API,
            {
                'client_id': client.id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', [])
        assert_that(data, has_length(len(required_orders)))
        assert_that(
            data,
            contains_inanyorder(*[
                has_entry('order_id', order.id)
                for order in required_orders
            ]),
        )

    @pytest.mark.parametrize(
        'has_role',
        [True, False],
    )
    def test_permissions(self, admin_role, view_order_role, has_role):
        roles = [admin_role]
        if has_role:
            roles.append(view_order_role)
        security.set_roles(roles)

        order = create_order_w_firm()
        response = self.test_client.get(
            self.BASE_API,
            {'client_id': order.client.id},
        )
        assert_that(response.status_code, equal_to(http.OK))

        orders_match = contains(has_entry('order_id', order.id)) if has_role else empty()
        data = response.get_json().get('data')
        assert_that(data, orders_match)

    @pytest.mark.parametrize(
        'match_client, firm_ids, role_firm_ids, ans',
        [
            pytest.param(True, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD],
                         True, id='w right client w right firm'),
            pytest.param(True, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [None],
                         True, id='w right client wo role_firm'),
            pytest.param(True, [None], [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD],
                         True, id='w right client wo order_firm'),
            pytest.param(True, [cst.FirmId.YANDEX_OOO], [cst.FirmId.DRIVE],
                         False, id='w right client w wrong role_firm'),
            pytest.param(True, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE],
                         False, id='w right client w wrong role_firm 2'),
            pytest.param(True, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO],
                         False, id='w right client w 1 role_firm'),
            pytest.param(None, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD],
                         True, id='wo client w right firm'),
            pytest.param(False, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD],
                         False, id='w wrong client w right firm'),
        ],
    )
    def test_constraints(
            self,
            admin_role,
            view_order_role,
            role_client,
            match_client,
            firm_ids,
            role_firm_ids,
            ans,
    ):
        roles = self._get_roles(view_order_role, role_firm_ids, role_client, match_client)
        roles.append(admin_role)
        security.set_roles(roles)

        order = create_order_w_firm(firm_ids=firm_ids, client=role_client.client)
        response = self.test_client.get(
            self.BASE_API,
            {'client_id': order.client.id},
        )
        assert_that(response.status_code, equal_to(http.OK))

        orders_match = contains(has_entry('order_id', order.id)) if ans else empty()
        data = response.get_json().get('data')
        assert_that(data, orders_match)

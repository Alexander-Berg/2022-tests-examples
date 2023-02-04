# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from decimal import Decimal as D
from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    equal_to,
    empty,
    has_entry,
    has_entries,
    has_item,
    not_,
)

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.resources import enums as common_enums
from yb_snout_api.resources.v1.order import enums as order_enums
from yb_snout_api.utils import clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_agency, create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.service import create_service
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, create_orders, create_order_w_firm
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role


@pytest.fixture(name='order_roles')
def create_order_roles(admin_role):
    view_role = create_role((
        cst.PermissionCode.VIEW_ORDERS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))
    refunds_role = create_role((
        cst.PermissionCode.DO_INVOICE_REFUNDS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))
    return [
        admin_role,
        view_role,
        refunds_role,
    ]


@pytest.fixture(name='view_order_role')
def create_view_order_role():
    return create_role((
        cst.PermissionCode.VIEW_ORDERS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


@pytest.mark.smoke
class TestCaseOrderList(TestCaseApiAppBase):
    BASE_API = u'/v1/order/list'

    def test_get_list(self, order_roles):
        security.set_roles(order_roles)
        order = create_order()
        params_list = [
            {
                'client_id': order.client.id,
            },
            {
                'service_order_id': order.service_order_id,
                'service_cc': order.service.cc,
            },
            {
                'service_id': order.service_id,
                'service_order_id': order.service_order_id,
            },
        ]

        for params in params_list:
            response = self.test_client.get(self.BASE_API, params)
            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

            data = response.get_json().get('data')
            assert_that(
                data['order_list'],
                contains(
                    has_entry('order_id', order.id),
                ),
                u'Failed with params {}'.format(params),
            )

    def test_conflict_service_params(self, client, order_roles):
        security.set_roles(order_roles)
        order = create_order(client=client)
        response = self.test_client.get(
            self.BASE_API,
            {  # и service_id, и service_cc будут добавлены в запрос с оператором "и"
                'service_id': cst.ServiceId.CLOUD,  # неправильный сервис
                'service_order_id': order.service_order_id,
                'service_cc': order.service.cc,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data['orders_total'], equal_to(0))

    def test_sorting(self, client, order_roles):
        security.set_roles(order_roles)
        orders = create_orders([D('1'), D('10')], client=client)
        params = [
            (common_enums.SortOrderType.ASC.name, orders),
            (common_enums.SortOrderType.DESC.name, orders[::-1]),
        ]
        for sort_order, req_orders in params:
            response = self.test_client.get(
                self.BASE_API,
                {
                    'client_id': client.id,
                    'sort_order': sort_order,
                    'sort_key': order_enums.OrdersSortKeyType.CONSUME_QTY.name,
                },
            )
            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

            data = response.get_json().get('data')
            assert_that(
                data['order_list'],
                contains(*[  # порядок важен
                    has_entry('order_id', order.id)
                    for order in req_orders
                ]),
            )

    @pytest.mark.slow
    def test_filter_by_payment_status(self, client, order_roles):
        security.set_roles(order_roles)
        orders = {
            'paid': create_order(client=client, turn_on=True),
            'unpaid': create_order(client=client, turn_on=False),
        }
        status_map = [
            (common_enums.InvoicePaymentStatus.ALL.name, ['unpaid', 'paid']),
            (common_enums.InvoicePaymentStatus.TURN_OFF.name, ['unpaid']),
            (common_enums.InvoicePaymentStatus.TURN_ON.name, ['paid']),
        ]

        for payment_status, order_names in status_map:
            response = self.test_client.get(
                self.BASE_API,
                {'client_id': client.id, 'payment_status': payment_status},
            )
            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

            data = response.get_json().get('data')
            assert_that(
                data,
                has_entries({
                    'orders_total': len(order_names),
                    'order_list': contains_inanyorder(*[
                        has_entries({'order_id': orders[name].id})
                        for name in order_names
                    ]),
                }),
            )


@pytest.mark.permissions
@pytest.mark.smoke
class TestCaseOrderListPermissions(TestCaseApiAppBase):
    BASE_API = u'/v1/order/list'
    ORDER_FIRM_IDS = [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD]

    @staticmethod
    def _get_roles(role, role_firm_ids, role_client, match_client):
        if match_client is None:
            client_batch_id = None
        else:
            client_batch_id = role_client.client_batch_id if match_client else create_role_client().client_batch_id
        return [
            (
                role,
                clean_dict({cst.ConstraintTypes.firm_id: firm_id, cst.ConstraintTypes.client_batch_id: client_batch_id}),
            )
            for firm_id in role_firm_ids
        ]

    @pytest.mark.parametrize(
        'has_role, ans',
        [
            (True, http.OK),
            (False, http.FORBIDDEN),  # nobody
        ],
    )
    def test_permissions(self, admin_role, view_order_role, has_role, ans):
        roles = [admin_role]
        if has_role:
            roles.append(view_order_role)
        security.set_roles(roles)

        order_w_firm = create_order_w_firm()
        response = self.test_client.get(
            self.BASE_API,
            {'client_id': order_w_firm.client.id},
        )
        assert_that(response.status_code, equal_to(ans))

        if has_role:
            data = response.get_json().get('data')
            assert_that(
                data,
                has_entries({
                    'orders_total': 1,
                    'order_list': has_item(has_entry('order_id', order_w_firm.id)),
                }),
            )

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
            service,
            role_client,
            match_client,
            firm_ids,
            role_firm_ids,
            ans,
    ):
        roles = self._get_roles(view_order_role, role_firm_ids, role_client, match_client)
        roles.append(admin_role)
        security.set_roles(roles)

        order = create_order_w_firm(firm_ids=firm_ids, client=role_client.client, service_id=service.id)
        response = self.test_client.get(
            self.BASE_API,
            {'service_id': service.id},
        )
        assert_that(response.status_code, equal_to(http.OK))

        orders_match = contains(has_entry('order_id', order.id)) if ans else empty()
        data = response.get_json().get('data')
        assert_that(
            data,
            has_entries({
                'orders_total': int(ans),
                'order_list': orders_match,
            }),
        )

    @pytest.mark.parametrize(
        'is_agency',
        [
            pytest.param(True, id='agency'),
            pytest.param(False, id='client'),
        ],
    )
    def test_agency_access(
            self,
            admin_role,
            view_order_role,
            service,
            agency,
            client,
            is_agency,
    ):
        client_batch_id = create_role_client(client=agency if is_agency else client).client_batch_id
        roles = [
            admin_role,
            (view_order_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO,
                               cst.ConstraintTypes.client_batch_id: client_batch_id}),
            (view_order_role, {cst.ConstraintTypes.firm_id: cst.FirmId.DRIVE,
                               cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]
        security.set_roles(roles)

        order = create_order_w_firm(
            client=client,
            service_id=service.id,
            firm_ids=[cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE],
        )
        order.agency = agency
        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            {'service_id': service.id},
        )
        assert_that(response.status_code, equal_to(http.OK))

        orders_match = contains(has_entry('order_id', order.id)) if is_agency else empty()
        data = response.get_json().get('data')
        assert_that(
            data,
            has_entries({
                'orders_total': int(is_agency),
                'order_list': orders_match,
            }),
        )

    def test_constraints_in_different_roles(self, admin_role, view_order_role, role_client, service):
        roles = [
            admin_role,
            (
                view_order_role,
                {cst.ConstraintTypes.client_batch_id: -666, cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO},
            ),
            (
                view_order_role,
                {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id, cst.ConstraintTypes.firm_id: -666},
            ),
        ]
        security.set_roles(roles)

        create_order_w_firm(
            firm_ids=[cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE],
            client=role_client.client,
            service_id=service.id,
        )
        response = self.test_client.get(
            self.BASE_API,
            {'service_id': service.id},
        )
        assert_that(response.status_code, equal_to(http.OK))

        data = response.get_json().get('data')
        assert_that(
            data,
            has_entries({
                'orders_total': 0,
                'order_list': empty(),
            }),
        )

    def test_filter_by_firm(self, admin_role, view_order_role, service):
        """Для админа заказы должны фильтроваться по фирмам.
        Нет фирмы в заказе => есть доступ.
        Если фирмы в заказе есть, то они все должны быть у пользователя в правах.
        """
        roles = [
            admin_role,
            (view_order_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
            (view_order_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD}),
        ]
        security.set_roles(roles)

        required_orders = [
            create_order_w_firm(service_id=service.id, firm_ids=[]),
            create_order_w_firm(service_id=service.id, firm_ids=[cst.FirmId.YANDEX_OOO]),
            create_order_w_firm(service_id=service.id, firm_ids=[cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD]),
        ]
        create_order_w_firm(service_id=service.id, firm_ids=[cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE])
        create_order_w_firm(service_id=service.id, firm_ids=[cst.FirmId.DRIVE, cst.FirmId.CLOUD])
        response = self.test_client.get(
            self.BASE_API,
            {'service_id': service.id},
        )
        assert_that(response.status_code, equal_to(http.OK))

        data = response.get_json().get('data')
        assert_that(data['orders_total'], equal_to(len(required_orders)))
        assert_that(
            data['order_list'],
            contains_inanyorder(*[
                has_entry('order_id', order.id)
                for order in required_orders
            ]),
        )

    @pytest.mark.parametrize(
        'role_funcs, res_count',
        [
            ([get_client_role], 1),
            ([get_client_role, create_view_order_role], 2),  # с админскими правами можно получить не свой заказ

        ],
    )
    @mock_client_resource('yb_snout_api.resources.v1.order.routes.order_list.OrderList')
    def test_client_owns_order(self, client_role, role_funcs, service, res_count):
        """Получаем только заказы, которые принадлежат клиенту из паспорта"""
        firm_id = cst.FirmId.YANDEX_OOO

        client1 = create_client()
        client2 = create_client()

        roles = [role_func() for role_func in role_funcs]
        security.set_passport_client(client1)
        security.set_roles(roles)

        order1 = create_order_w_firm(client=client1, firm_ids=[firm_id], service_id=service.id)
        order2 = create_order_w_firm(client=client2, firm_ids=[firm_id], service_id=service.id)

        response = self.test_client.get(
            self.BASE_API,
            {
                'service_id': service.id,
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data['orders_total'], equal_to(res_count))
        assert_that(
            data['order_list'],
            contains_inanyorder(*[
                has_entries({'order_id': o.id, 'client_id': c.id})
                for c, o in [(client1, order1), (client2, order2)][:res_count]
            ]),
        )

    @mock_client_resource('yb_snout_api.resources.v1.order.routes.order_list.OrderList')
    def test_client_ui_admin_user(self, admin_role, view_order_role, service):
        """В КИ ищем заказы под админом. Да, так можно."""
        roles = [
            admin_role,
            (view_order_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
        ]
        security.set_roles(roles)

        order1 = create_order_w_firm(firm_ids=[cst.FirmId.YANDEX_OOO], service_id=service.id)
        order2 = create_order_w_firm(firm_ids=[cst.FirmId.CLOUD], service_id=service.id)
        response = self.test_client.get(
            self.BASE_API,
            {
                'service_id': service.id,
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data['orders_total'], equal_to(1))
        assert_that(data['order_list'], has_item(has_entry('order_id', order1.id)))
        assert_that(data['order_list'], not_(has_item(has_entry('order_id', order2.id))))

    @pytest.mark.parametrize(
        'hidden',
        [True, None],
    )
    def test_hidden_role_client(self, admin_role, view_order_role, hidden):
        session = self.test_session
        role_client = create_role_client()
        role_client.hidden = hidden
        roles = [
            admin_role,
            (view_order_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id}),
        ]
        security.set_roles(roles)

        order = create_order(client=role_client.client)
        session.flush()
        res = self.test_client.get(
            self.BASE_API,
            params={'client_id': role_client.client.id},
        )
        assert_that(res.status_code, equal_to(http.OK))

        data = res.get_json().get('data', {})
        res_match = contains(has_entry('order_id', order.id)) if hidden is None else empty()
        assert_that(data.get('order_list', []), res_match)

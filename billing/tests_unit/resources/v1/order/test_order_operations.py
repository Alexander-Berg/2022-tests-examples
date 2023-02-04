# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.builtins import str as text

standard_library.install_aliases()

import datetime
import http.client as http
import pytest
from decimal import Decimal as D
import hamcrest as hm

from balance import constants as cst
from tests import object_builder as ob
from balance.actions.consumption import reverse_consume

from brest.core.tests import security

from yb_snout_api.resources import enums
from yb_snout_api.utils import get_attrib_by_name
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.operation import (
    create_order_operations,
    create_removal_operation,
    create_order_operation,
    get_default_operation_ns,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_custom_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client


@pytest.fixture(name='view_order_role')
def create_view_order_role():
    return create_role((cst.PermissionCode.VIEW_ORDERS, {cst.ConstraintTypes.firm_id: None}))


@pytest.mark.smoke
class TestCaseOrderOperations(TestCaseApiAppBase):
    BASE_API = u'/v1/order/operations'

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'parameters',
        [
            [('order_id', 'id')],
            [('service_id', 'service_id'), ('service_order_id', 'service_order_id')],
            [('service_cc', 'service.cc'), ('service_order_id', 'service_order_id')],
        ],
        ids=['order_id', 'service_id & service_order_id', 'service_cc & service_order_id'],
    )
    def test_get_operations(self, default_operation_ns, parameters, admin_role, view_order_role):
        firm_id = cst.FirmId.DRIVE
        security.set_roles([admin_role, (view_order_role, {cst.ConstraintTypes.firm_id: firm_id})])

        order = ob.OrderBuilder.construct(self.test_session)
        invoice = create_custom_invoice({order: D('10')}, client=order.client, firm_id=firm_id)
        operation = create_removal_operation(invoice, order, default_operation_ns)
        self.test_session.flush()

        security.set_roles([admin_role, (view_order_role, {cst.ConstraintTypes.firm_id: invoice.firm_id})])
        response = self.test_client.get(
            self.BASE_API,
            {key: get_attrib_by_name(order, attr_name) for key, attr_name in parameters},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        data = response.get_json()['data']
        hm.assert_that(data['total-row-count'], hm.equal_to(1))
        hm.assert_that(data['limit-overflow'], hm.is_(False))
        hm.assert_that(
            data.get('operations', []),
            hm.contains(hm.has_entries({'id': operation.id})),
        )

    @pytest.mark.permissions
    def test_nobody(self, default_operation_ns, admin_role):
        security.set_roles([admin_role])
        order = create_order()
        response = self.test_client.get(
            self.BASE_API,
            {'order_id': order.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))

    @pytest.mark.permissions
    def test_client_access(self, default_operation_ns, admin_role, client):
        security.set_passport_client(client)
        security.set_roles([admin_role])

        order = create_order(client=client)
        invoice = create_custom_invoice({order: D('10')}, client=order.client)
        invoice.manual_turn_on(D('1000'))
        response = self.test_client.get(
            self.BASE_API,
            {'order_id': order.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        data = response.get_json()['data']
        hm.assert_that(data['total-row-count'], hm.equal_to(2))

    @pytest.mark.parametrize(
        'offset, limit, get_last_page, has_prev_pages, has_next_pages, l_ind, r_ind, reverse',
        [
            (0, 3, False, 0, 2, 0, 3, False),  # общее кол-во операций = 16, отступы по 2 набора
            (1, 3, False, 1, 2, 1, 4, False),
            (4, 3, False, 2, 2, 4, 7, False),
            (0, 3, True, 2, 0, 13, 16, False),
            (4, 3, True, 2, 2, 9, 12, False),
            (8, 2, False, 4, 2, 8, 10, False),
            (2, 2, False, 1, 2, 2, 4, False),
            (2, 2, True, 2, 1, 12, 14, False),
            (0, 3, False, 0, 2, 0, 3, True),
            (0, 3, True, 2, 0, 13, 16, True),
            (4, 3, False, 2, 2, 4, 7, True),
        ],
    )
    @pytest.mark.slow
    def test_limit_offset(
            self,
            offset,
            limit,
            get_last_page,
            has_prev_pages,
            has_next_pages,
            l_ind,
            r_ind,
            reverse,
    ):
        from yb_snout_api.resources.v1.order import enums as order_enums

        order = create_order(turn_on=False)
        invoice = create_custom_invoice({order: D('666')}, client=order.client)
        operations = create_order_operations(order, invoice, get_default_operation_ns())

        response = self.test_client.get(
            self.BASE_API,
            {
                'order_id': order.id,
                'limit': limit,
                'offset': offset,
                'get_last_page': get_last_page,
                'sort_key': order_enums.OperationsSortKeyType.DT.name,
                'sort_order': enums.SortOrderType.DESC.name if reverse else enums.SortOrderType.ASC.name,
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json().get('data', [])
        sorted_operations = sorted(operations, key=lambda op: op.dt, reverse=reverse)

        hm.assert_that(
            data,
            hm.has_entries({
                'operations': hm.contains(*[
                    hm.has_entries({
                        'id': op.id,
                        'external-type-id': text(op.display_type_id),
                        'type-id': op.type_id,
                    })
                    for op in sorted_operations[l_ind:r_ind]
                ]),
                'has_next': bool(has_next_pages),
                'has_prev_pages': has_prev_pages,
                'has_next_pages': has_next_pages,
            }),
        )

    def test_null_operations(self, default_operation_ns):
        """Проверка операций, которых еще нет в базе
        """
        from yb_snout_api.resources.v1.order import enums as order_enums

        session = self.test_session
        order = create_order(turn_on=False)
        invoice = create_custom_invoice({order: D('666')}, client=order.client)
        operations = create_order_operations(order, invoice, default_operation_ns)
        session.flush()

        op_type_id = enums.TransferType.DEST_PTS.value
        op_count = len(operations)
        half_op_count = int(op_count / 2)

        reverse_consume(
            consume=order.consumes[-1],
            operation=None,
            qty=default_operation_ns.unit_qty,
        )
        consume_order = invoice.transfer(  # создает 2 конзюма, один из них получается с операцией
            dest_order=order,
            mode=op_type_id,
            sum=op_count,
            skip_check=True,
        )
        consume_order.consume.operation = None
        session.flush()

        response = self.test_client.get(
            self.BASE_API,
            {
                'order_id': order.id,
                'offset': half_op_count - 1,
                'sort_key': order_enums.OperationsSortKeyType.DT.name,
                'sort_order': enums.SortOrderType.ASC.name,
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json().get('data', [])
        sorted_operations = sorted(operations, key=lambda op: op.dt)

        matchers = [
            hm.has_entries({
                'id': op.id,
                'external-type-id': text(op.display_type_id),
                'type-id': op.type_id,
            })
            for op in sorted_operations[half_op_count:]
        ]
        matchers += [
            hm.has_entries({
                'id': None,
                'external-type-id': text(cst.OperationDisplayType.ENROLLMENT),
                'type-id': op_type_id,
            }),
            hm.has_entries({
                'id': None,
                'external-type-id': text(cst.OperationDisplayType.REMOVAL),
                'type-id': op_type_id,
            }),
        ]

        hm.assert_that(
            data,
            hm.has_entries({
                'operations': hm.has_items(*matchers),
            }),
        )

    def test_dt_boundaries(self):
        order = create_order(turn_on=False)
        invoice = [io.invoice for io in order.invoice_orders][0]

        operations = [
            create_order_operation(order, invoice, get_default_operation_ns(dt=datetime.datetime(2020, 03, 31, 23, 59, 59))),
            create_order_operation(order, invoice, get_default_operation_ns(dt=datetime.datetime(2020, 04, 01, 0, 0, 0))),
            create_order_operation(order, invoice, get_default_operation_ns(dt=datetime.datetime(2020, 04, 2, 0, 0, 0))),
            create_order_operation(order, invoice, get_default_operation_ns(dt=datetime.datetime(2020, 04, 2, 23, 59, 59))),
            create_order_operation(order, invoice, get_default_operation_ns(dt=datetime.datetime(2020, 04, 3, 0, 0, 0))),
        ]
        self.test_session.flush()

        params_options = [
            ({'from_dt': '2020-04-01T00:00:00', 'to_dt': '2020-04-02T00:00:00'}, operations[1:4]),
            ({'from_dt': '2020-04-01T00:00:00'}, operations[1:]),
            ({'to_dt': '2020-04-02T00:00:00'}, operations[:4]),
        ]

        for params, opers in params_options:
            params['order_id'] = order.id
            response = self.test_client.get(self.BASE_API, params)
            hm.assert_that(response.status_code, hm.equal_to(http.OK))

            data = response.get_json().get('data', [])
            hm.assert_that(
                data.get('operations', []),
                hm.contains_inanyorder(*[
                    hm.has_entries({
                        'id': op.id,
                        'external-type-id': text(op.display_type_id),
                        'type-id': op.type_id,
                    })
                    for op in opers
                ]),
                'Filed with params %s' % params,
            )

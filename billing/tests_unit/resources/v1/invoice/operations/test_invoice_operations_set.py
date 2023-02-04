# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

from builtins import str as text
standard_library.install_aliases()

import http.client as http
import datetime
import pytest
import hamcrest as hm

from balance import constants as cst
from balance.actions.consumption import reverse_consume
import tests.object_builder as ob
from brest.core.tests import security

from yb_snout_api.resources import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.operation import (
    create_operations,
    create_order_operation,
    get_default_operation_ns,
)


@pytest.mark.smoke
class TestInvoiceOperations(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/operations'

    @pytest.mark.permissions
    def test_access(self, invoice):
        security.set_roles([])
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})

        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')

    def test_operations(self, invoice, operations):
        u"""
        Проверяем набор различных операций
        """
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', [])
        sorted_operations = sorted(operations, key=lambda op: op.dt, reverse=True)  # сортировка по умолчанию

        hm.assert_that(
            data,
            hm.has_entries({
                'operation': hm.contains(*[
                    hm.has_entries({
                        'external-type-id': text(op.display_type_id),
                        'id': text(op.id),
                        'invoice': hm.has_entries({'id': text(invoice.id)}),
                        'type-id': text(op.type_id),
                    })
                    for op in sorted_operations
                ]),
            }),
        )

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
            invoice,
            operations,
            offset,
            limit,
            get_last_page,
            has_prev_pages,
            has_next_pages,
            l_ind,
            r_ind,
            reverse,
    ):
        from yb_snout_api.resources.v1.invoice import enums as invoice_enums

        response = self.test_client.get(
            self.BASE_API,
            {
                'invoice_id': invoice.id,
                'limit': limit,
                'offset': offset,
                'get_last_page': get_last_page,
                'sort_key': invoice_enums.OperationsSortKeyType.DEFAULT.name,
                'sort_order': enums.SortOrderType.DESC.name if reverse else enums.SortOrderType.ASC.name,
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', [])
        sorted_operations = sorted(operations, key=lambda op: op.dt, reverse=reverse)

        hm.assert_that(
            data,
            hm.has_entries({
                'operation': hm.contains(*[
                    hm.has_entries({
                        'id': text(op.id),
                        'external-type-id': text(op.display_type_id),
                        'type-id': text(op.type_id),
                    })
                    for op in sorted_operations[l_ind:r_ind]
                ]),
                'has_next': bool(has_next_pages),
                'has_prev_pages': has_prev_pages,
                'has_next_pages': has_next_pages,
            }),
        )

    def test_null_operations(self, invoice, operations, default_operation_ns):
        u"""
        Проверка операций, которых еще нет в базе
        """
        from yb_snout_api.resources.v1.invoice import enums as invoice_enums

        op_type_id = enums.TransferType.DEST_PTS.value
        op_count = len(operations)
        half_op_count = int(op_count / 2)

        session = self.test_session
        alt_order = ob.OrderBuilder(
            client=invoice.client,
            product=invoice.invoice_orders[0].order.product,
        ).build(session).obj

        invoice.create_receipt(cst.OperationTypeIDs.support)
        consume_order = invoice.transfer(
            dest_order=alt_order,
            mode=op_type_id,
            sum=op_count,
            skip_check=True,
        )
        consume_order.consume.operation = None
        reverse_consume(
            consume=alt_order.consumes[-1],
            operation=None,
            qty=default_operation_ns.unit_qty,
        )
        session.flush()

        response = self.test_client.get(
            self.BASE_API,
            {
                'invoice_id': invoice.id,
                'limit': op_count,
                'offset': half_op_count,
                'sort_key': invoice_enums.OperationsSortKeyType.DEFAULT.name,
                'sort_order': enums.SortOrderType.ASC.name,
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', [])
        sorted_operations = sorted(operations, key=lambda op: op.dt)

        matchers = [
            hm.has_entries({
                'id': text(op.id),
                'external-type-id': text(op.display_type_id),
                'type-id': text(op.type_id),
            })
            for op in sorted_operations[half_op_count:]
        ]
        matchers = matchers + [
            hm.has_entries({
                'id': None,
                'external-type-id': text(cst.OperationDisplayType.RECEIPT),
                'type-id': text(op_type_id),
            }),
            hm.has_entries({
                'id': None,
                'external-type-id': text(cst.OperationDisplayType.ENROLLMENT),
                'type-id': text(op_type_id),
            }),
            hm.has_entries({
                'id': None,
                'external-type-id': text(cst.OperationDisplayType.REMOVAL),
                'type-id': text(op_type_id),
            }),
        ]

        hm.assert_that(
            data,
            hm.has_entries({
                'operation': hm.contains_inanyorder(*matchers),
            }),
        )

    def test_dt_boundaries(self, invoice):
        order = invoice.invoice_orders[0].order

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
            params['invoice_id'] = invoice.id
            response = self.test_client.get(self.BASE_API, params)
            hm.assert_that(response.status_code, hm.equal_to(http.OK))

            data = response.get_json().get('data', [])
            hm.assert_that(
                data.get('operation', []),
                hm.contains_inanyorder(*[
                    hm.has_entries({
                        'id': text(op.id),
                        'external-type-id': text(op.display_type_id),
                        'type-id': text(op.type_id),
                    })
                    for op in opers
                ]),
                'Filed with params %s' % params,
            )

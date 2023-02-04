# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.builtins import str as text
standard_library.install_aliases()

from decimal import (
    Decimal as D,
    ROUND_HALF_UP,
)
import http.client as http
import pytest
from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    equal_to,
    has_entries,
    has_item,
)

from balance import mapper
from balance import constants as cst
import tests.object_builder as ob

from yb_snout_api.resources import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.utils import match_decimal_value
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_custom_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.operation import (
    create_change_discount_operation,
    create_complicated_operation,
    create_enrollment_operation,
    create_receipt_operation,
    create_removal_operation,
    create_transfer_operation,
    get_default_operation_ns,
)


class TestInvoiceUnitOperations(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/operations'

    @pytest.mark.parametrize(
        'order_count',
        [1, 3],
        ids=lambda or_c: u'receipt for {} orders'.format(or_c),
    )
    def test_receipt(self, order_count, default_operation_ns):

        session = self.test_session

        service = ob.Getter(mapper.Service, cst.DIRECT_SERVICE_ID)
        client_b = ob.ClientBuilder()

        def qty_calc(qty):
            return default_operation_ns.operation_qty + D(qty)

        orders_qty_map = {
            ob.OrderBuilder(service=service, client=client_b).build(session).obj: qty_calc(qty)
            for qty in range(order_count)
        }
        invoice = create_custom_invoice(orders_qty_map, client=client_b)
        operation = create_receipt_operation(invoice, default_operation_ns)

        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        data = response.get_json().get('data')

        transaction_match = [
            has_entries({
                'dst-order': has_entries({'id': text(order.id)}),
                'type': 'consume',
                'transact-qty': match_decimal_value(qty, default_operation_ns.decimals),
                'dynamic-discount-pct': u'0.00',
            }) for order, qty in orders_qty_map.items()
        ]

        payment_sum = match_decimal_value(
            default_operation_ns.payment_sum,
            default_operation_ns.decimals,
        )
        match_dict = {
            'id': text(operation.id),
            'external-type-id': text(cst.OperationDisplayType.RECEIPT),
            'type-id': text(enums.TransferType.DEST_PTS.value),
            'invoice': has_entries({
                'id': text(invoice.id),
                'receipt-sum': payment_sum,
                'hidden': '0',
            }),
            'oper-sum': payment_sum,
            'transaction': contains_inanyorder(*transaction_match),
        }

        assert_that(
            data.get('operation', []),
            contains(has_entries(match_dict)),
        )

    def test_removal_operation(self, default_operation_ns, client):

        session = self.test_session
        order = ob.OrderBuilder(
            client=client,
            service=ob.Getter(mapper.Service, cst.DIRECT_SERVICE_ID),
        ).build(session).obj
        orders_qty_map = {order: default_operation_ns.operation_qty}
        invoice = create_custom_invoice(orders_qty_map, client=client)
        operation = create_removal_operation(invoice, order, default_operation_ns)

        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        data = response.get_json().get('data')

        transact_qty = match_decimal_value(
            default_operation_ns.operation_amount * default_operation_ns.removal_coeff,
            default_operation_ns.decimals,
        )
        oper_amount_free = match_decimal_value(
            default_operation_ns.operation_amount,
            default_operation_ns.decimals,
        )
        transaction_match = has_entries({
            'src-order': has_entries(
                {'id': text(order.id)},
            ),
            'type': u'reverse',
            'transact-qty': transact_qty,
        })
        match_dict = {
            'id': text(operation.id),
            'external-type-id': text(cst.OperationDisplayType.REMOVAL),
            'type-id': text(enums.TransferType.DEST_PTS.value),
            'invoice': has_entries({
                'id': text(invoice.id),
            }),
            'oper-amount-free': oper_amount_free,
            'src-order': has_entries({
                'id': text(order.id),
            }),
            'transaction': contains(transaction_match),
        }
        assert_that(
            data.get('operation', []),
            has_item(has_entries(match_dict)),
        )

    def test_enrollment_operation(self, default_operation_ns):

        session = self.test_session

        service = ob.Getter(mapper.Service, cst.DIRECT_SERVICE_ID)
        client_b = ob.ClientBuilder()
        order = ob.OrderBuilder(service=service, client=client_b).build(session).obj
        orders_qty_map = {order: default_operation_ns.operation_qty}
        invoice = create_custom_invoice(orders_qty_map, client=client_b)
        operation = create_enrollment_operation(invoice)

        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        data = response.get_json().get('data')

        transaction_match = has_entries({
            'dst-order': has_entries({
                'id': text(order.id),
            }),
            'type': 'consume',
        })
        match_dict = {
            'id': text(operation.id),
            'external-type-id': text(cst.OperationDisplayType.ENROLLMENT),
            'type-id': text(enums.TransferType.DEST_PTS.value),
            'invoice': has_entries({
                'id': text(invoice.id),
            }),
            'dst-order': has_entries({
                'id': text(order.id),
            }),
            'transaction': contains_inanyorder(transaction_match),
        }
        assert_that(
            data.get('operation', []),
            has_item(has_entries(match_dict)),
        )

    def test_transfer_operation(self, default_operation_ns):
        session = self.test_session

        service = ob.Getter(mapper.Service, cst.DIRECT_SERVICE_ID)
        client_b = ob.ClientBuilder()
        order = ob.OrderBuilder(service=service, client=client_b).build(session).obj
        dst_order = ob.OrderBuilder(client=client_b, product=order.product).build(session).obj
        orders_qty_map = {order: default_operation_ns.operation_qty}
        invoice = create_custom_invoice(orders_qty_map, client=client_b)
        operation = create_transfer_operation(
            invoice=invoice,
            default_operation_ns=default_operation_ns,
            dst_order=dst_order,
        )

        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        data = response.get_json().get('data')

        transact_qty_reverse = match_decimal_value(
            -1 * default_operation_ns.operation_qty,
            default_operation_ns.decimals,
        )
        transact_qty_consume = match_decimal_value(
            default_operation_ns.operation_qty,
            default_operation_ns.decimals,
        )
        transaction_match = [
            has_entries({
                'src-order': has_entries({
                    'id': text(order.id),
                }),
                'type': u'reverse',
                'transact-qty': transact_qty_reverse,
            }),
            has_entries({
                'dst-order': has_entries({
                    'id': text(dst_order.id),
                }),
                'type': u'consume',
                'transact-qty': transact_qty_consume,
            }),
        ]

        match_dict = {
            'id': text(operation.id),
            'external-type-id': text(cst.OperationDisplayType.TRANSFER),
            'type-id': text(enums.TransferType.SRC_PTS.value),
            'invoice': has_entries({
                'id': text(invoice.id),
            }),
            'dst-order': has_entries({
                'id': text(dst_order.id),
            }),
            'src-order': has_entries({
                'id': text(order.id),
            }),
            'transaction': contains_inanyorder(*transaction_match),
        }

        assert_that(
            data.get('operation', []),
            has_item(has_entries(match_dict)),
        )

    def test_discount_operation(self, default_operation_ns):
        session = self.test_session

        service = ob.Getter(mapper.Service, cst.DIRECT_SERVICE_ID)
        client_b = ob.ClientBuilder()
        order = ob.OrderBuilder(service=service, client=client_b).build(session).obj
        orders_qty_map = {order: default_operation_ns.operation_qty}
        invoice = create_custom_invoice(orders_qty_map, client=client_b)
        operation = create_change_discount_operation(invoice, default_operation_ns)

        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')

        transact_qty = match_decimal_value(-1 * default_operation_ns.operation_qty, default_operation_ns.decimals)
        transaction_match = [
            has_entries({
                'src-order': has_entries({
                    'id': text(order.id),
                }),
                'type': 'reverse',
                'transact-qty': transact_qty,
            }),
            has_entries({
                'dst-order': has_entries({
                    'id': text(order.id),
                }),
                'type': 'consume',
                'static-discount-pct': match_decimal_value(
                    default_operation_ns.discount_pct,
                    default_operation_ns.decimals,
                ),
            }),
        ]
        match_dict = {
            'id': text(operation.id),
            'external-type-id': text(cst.OperationDisplayType.DISCOUNT_RECALC),
            'type-id': text(cst.OperationTypeIDs.support),
            'invoice': has_entries({
                'id': text(invoice.id),
                'receipt-sum': match_decimal_value(
                    default_operation_ns.payment_sum,
                    default_operation_ns.decimals,
                ),
            }),
            'dst-order': has_entries({
                'id': text(order.id),
            }),
            'src-order': has_entries({
                'id': text(order.id),
            }),
            'transaction': contains_inanyorder(*transaction_match),
        }
        assert_that(
            data.get('operation', []),
            has_item(has_entries(match_dict)),
        )


class TestInvoiceComplexOperations(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/operations'

    def _get_transaction_match(
            self,
            dst_orders,
            src_order_count,
            default_operation_ns,
    ):
        u"""Количество транзакций == количеству заказов"""
        transact_qty = match_decimal_value(
            default_operation_ns.operation_qty * src_order_count / len(dst_orders),
            default_operation_ns.decimals,
        )
        return [
            has_entries({
                'dst-order': has_entries({
                    'id': text(dst_order.id),
                }),
                'type': u'consume',
                'transact-qty': transact_qty,
            }) for dst_order in dst_orders
        ]

    def _get_non_symmetric_transaction_match(
            self,
            dst_orders,
            src_order_count,
            default_operation_ns,
    ):
        u"""Для ситуации с 3-х заказов на 1 создается 2 транзакции с разными суммами"""
        dst_order = dst_orders[0]
        return [
            has_entries({
                'dst-order': has_entries({
                    'id': text(dst_order.id),
                }),
                'type': 'reverse',
                'transact-qty': match_decimal_value(
                    default_operation_ns.operation_qty * (src_order_count - 1),
                    default_operation_ns.decimals,
                    ROUND_HALF_UP,
                ),
            }),
            has_entries({
                'dst-order': has_entries({
                    'id': text(dst_order.id),
                }),
                'type': 'consume',
                'transact-qty': match_decimal_value(
                    default_operation_ns.operation_qty,
                    default_operation_ns.decimals,
                    ROUND_HALF_UP,
                ),
            }),
        ]

    @pytest.mark.parametrize(
        'src_order_count, dst_order_count, get_dst_match_func',
        [
            pytest.param(1, 3, _get_transaction_match, id='1 -> 3'),
            pytest.param(3, 1, _get_non_symmetric_transaction_match, id='3 -> 1'),
            pytest.param(3, 3, _get_transaction_match, id='3 -> 3'),
        ],
    )
    def test_complicated_operation(
            self,
            src_order_count,
            dst_order_count,
            get_dst_match_func,
            default_operation_ns,
    ):
        session = self.test_session

        service = ob.Getter(mapper.Service, cst.DIRECT_SERVICE_ID)
        client_b = ob.ClientBuilder()
        src_orders = [
            ob.OrderBuilder(service=service, client=client_b).build(session).obj
            for _i in xrange(src_order_count)
        ]
        dst_orders = [
            ob.OrderBuilder(service=service, client=client_b).build(session).obj
            for _i in xrange(dst_order_count)
        ]
        orders_qty_map = {
            src_order: default_operation_ns.operation_qty
            for src_order in src_orders
        }
        invoice = create_custom_invoice(orders_qty_map, client=client_b)
        invoice.manual_turn_on(default_operation_ns.payment_sum)
        invoice.turn_on_rows()
        operation = create_complicated_operation(
            invoice=invoice,
            src_orders=src_orders,
            default_operation_ns=default_operation_ns,
            dst_orders=dst_orders,
        )

        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        data = response.get_json().get('data')

        src_transact_qty = match_decimal_value(
            -1 * default_operation_ns.operation_qty,
            default_operation_ns.decimals,
        )
        src_matches = [
            has_entries({
                'src-order': has_entries({
                    'id': text(src_order.id),
                }),
                'type': u'reverse',
                'transact-qty': src_transact_qty,
            }) for src_order in src_orders
        ]
        transaction_matches = src_matches + get_dst_match_func(
            self,
            dst_orders,
            src_order_count,
            default_operation_ns,
        )

        match_dict = {
            'id': text(operation.id),
            'external-type-id': text(cst.OperationDisplayType.COMPLEX),
            'type-id': text(cst.OperationTypeIDs.support),
            'invoice': has_entries({
                'id': text(invoice.id),
            }),
            'transaction': contains_inanyorder(*transaction_matches),
        }

        if dst_order_count == 1:
            match_dict['dst-order'] = has_entries({
                'id': text(dst_orders[0].id),
            })
        if src_order_count == 1:
            match_dict['src-order'] = has_entries({
                'id': text(src_orders[0].id),
            })

        assert_that(
            data.get('operation', []),
                has_item(has_entries(match_dict)),
        )

# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import allure
from decimal import Decimal as D

from balance import (
    core,
    constants as cst,
    mapper,
)
from balance.actions.consumption import reverse_consume
from balance.actions.transfers_qty import interface

import tests.object_builder as ob

from brest.core.tests import utils as test_utils
from yb_snout_api.resources import enums
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


class DefaultOperationNS(object):
    u"""
    Operation's default namespace
    """
    decimals = 2  # количество знаков после запятой для количества товара
    dt = datetime.datetime.now()

    payment_sum = D('12345.67')
    order_qty = D('19.12')

    operation_count = 16

    operation_qty = D('7.34')
    operation_amount = D('4.4')
    operation_qty_delta = D('0.034')

    transfer_sum = 100
    unit_qty = 1

    removal_coeff = D('-0.001')

    discount_pct = D('11.3')

    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)


@pytest.fixture(name=u'default_operation_ns')
@allure.step(u'default operation values')
def get_default_operation_ns(**kwargs):
    return DefaultOperationNS(**kwargs)


@pytest.fixture(name=u'receipt_operation')
def create_receipt_operation(invoice, default_operation_ns):
    u"""
    Операция поступления (display_type_id=cst.OperationDisplayType.RECEIPT).
    """
    session = test_utils.get_test_session()
    with allure.step(u'create receipt operation with invoice_id={}'.format(invoice.id)):
        invoice.manual_turn_on(default_operation_ns.payment_sum)
        operation = session.query(mapper.Operation).getone(invoice_id=invoice.id)

        return operation


@pytest.fixture(name=u'removal_operation')
def create_removal_operation(invoice, order, default_operation_ns):
    u"""
    Операция снятия (display_type_id=cst.OperationDisplayType.REMOVAL).
    """
    from yb_snout_api.resources.v1.invoice import enums as invoice_enums

    session = test_utils.get_test_session()

    with allure.step(u'create removal operation with invoice_id={}'.format(invoice.id)):
        invoice.turn_on_rows()

        core.Core(session).invoice_rollback(
            invoice_id=invoice.id,
            amount=default_operation_ns.operation_amount,
            unused_funds_lock=invoice_enums.UnusedFundsLockType.REFUND.value,
            order_id=order.id,
        )
        operation = (
            session
                .query(mapper.Operation)
                .filter_by(invoice_id=invoice.id)
                .order_by(mapper.Operation.id.desc())
                .first()
        )

        return operation


@pytest.fixture(name=u'enrollment_operation')
def create_enrollment_operation(invoice):
    u"""
    Операция зачисления (display_type_id=cst.OperationDisplayType.ENROLLMENT).
    """
    session = test_utils.get_test_session()

    with allure.step(u'create enrollment operation with invoice_id={}'.format(invoice.id)):
        invoice.turn_on_rows()
        operation = (
            session
                .query(mapper.Operation)
                .filter_by(invoice_id=invoice.id)
                .order_by(mapper.Operation.id.desc())
                .first()
        )

        return operation


@pytest.fixture(name=u'transfer_operation')
def create_transfer_operation(invoice, default_operation_ns, dst_order):
    u"""
    Операция переноса (display_type_id=cst.OperationDisplayType.TRANSFER).
    """

    session = test_utils.get_test_session()

    with allure.step(u'create transfer operation with invoice_id={}'.format(invoice.id)):
        create_receipt_operation(invoice, default_operation_ns)  # создаем поступление на заказ

        order = invoice.consumes[0].order
        order.transfer(
            operator_uid=session.oper_id,
            dest_order=dst_order,
            mode=enums.TransferType.ALL.value,
            force_transfer_acted=True,
        )
        operation = session.query(mapper.Operation).getone(dst_order_id=dst_order.id)

        return operation


@pytest.fixture(name=u'complicated_operation')
def create_complicated_operation(invoice, dst_orders, default_operation_ns, src_orders):
    u"""
    Сложная операция (display_type_id=cst.OperationDisplayType.COMPLEX).
    """
    session = test_utils.get_test_session()

    with allure.step(u'create complicated operation with invoice_id={}'.format(invoice.id)):
        src_orders = src_orders or [co.order for co in invoice.consumes]
        dst_orders = dst_orders
        operation = mapper.Operation(
            type_id=cst.OperationTypeIDs.support,
            invoice=invoice,
            dt=datetime.datetime.now(),
        )
        src_info_list = [{
            'QtyOld': src_order.consume_qty,
            'InternalQty': True,
            'QtyNew': default_operation_ns.operation_qty,
            'force_transfer_acted': True,
        } for src_order in src_orders]
        src_items = [
            interface.SrcItem.create(src_info, src_order)
            for src_info, src_order in zip(src_info_list, src_orders)
        ]
        dst_items = [
            interface.DstItem.create({'QtyDelta': default_operation_ns.operation_qty_delta}, dst_order)
            for dst_order in dst_orders
        ]
        tr = interface.TransferMultiple(
            session=session,
            src_items=src_items,
            dst_items=dst_items,
            operation=operation,
            skip_invoice_update=True,
        )
        tr.do(forced=True, to_notify=False)

        return operation


@pytest.fixture(name=u'change_disscount_operation')
def create_change_discount_operation(invoice, default_operation_ns):
    u"""
    Операция изменения скидки (display_type_id=cst.OperationDisplayType.DISCOUNT_RECALC).
    """
    session = test_utils.get_test_session()
    invoice = session.query(mapper.Invoice).getone(invoice.id)

    with allure.step(u'create changed discount operation with invoice_id={}'.format(invoice.id)):
        create_receipt_operation(invoice, default_operation_ns)  # создаем поступление на заказ
        order = invoice.consumes[0].order
        operation = mapper.Operation(
            type_id=cst.OperationTypeIDs.support,
            src_order=order,
            dt=datetime.datetime.now() - datetime.timedelta(1),
        )
        src_info = {
            'QtyOld': order.consume_qty,
            'InternalQty': True,
            'QtyNew': order.completion_qty,
            'force_transfer_acted': True,
        }
        tr = interface.TransferMultiple(
            session=session,
            src_items=[interface.SrcItem.create(src_info, order)],
            dst_items=[interface.DstItem.create({'QtyDelta': D(default_operation_ns.unit_qty)}, order)],
            discount_pct=default_operation_ns.discount_pct,
            operation=operation,
        )
        tr.do(
            forced=True,
            to_notify=False,
        )
        return operation


@pytest.fixture(name=u'operations')
def create_operations(invoice, default_operation_ns):
    session = test_utils.get_test_session()

    with allure.step(u'create set of different operations'):
        order = invoice.invoice_orders[0].order
        alt_order = ob.OrderBuilder(
            client=order.client,
            product=order.product,
        ).build(session).obj

        operations = []
        for idx in range(default_operation_ns.operation_count):
            operation = mapper.Operation(
                cst.OperationTypeIDs.support,
                dt=datetime.datetime.now() - datetime.timedelta(default_operation_ns.operation_count + 1 - idx),
            )
            if idx % 4 == 0:
                invoice.create_receipt(cst.OperationTypeIDs.support, operation)
            elif idx % 4 == 1:
                invoice.transfer(
                    dest_order=order,
                    mode=enums.TransferType.DEST_PTS.value,
                    sum=default_operation_ns.transfer_sum,
                    operation=operation,
                    skip_check=True,
                )
            elif idx % 4 == 2:
                consume = invoice.consumes[-1]
                reverse_consume(
                    consume=consume,
                    operation=operation,
                    qty=default_operation_ns.unit_qty,
                )
            elif idx % 4 == 3:
                invoice.create_receipt(cst.OperationTypeIDs.support, operation)
                consume = invoice.consumes[-1]
                reverse_consume(consume, operation, default_operation_ns.unit_qty)
                invoice.transfer(
                    dest_order=alt_order,
                    mode=enums.TransferType.DEST_PTS.value,
                    sum=default_operation_ns.transfer_sum,
                    operation=operation,
                    skip_check=True,
                )

            operations.append(operation)
        return operations


def create_order_operation(order, invoice, default_operation_ns):
    operation = mapper.Operation(
        cst.OperationTypeIDs.support,
        dt=default_operation_ns.dt,
    )
    invoice.transfer(
        dest_order=order,
        mode=enums.TransferType.DEST_PTS.value,
        sum=default_operation_ns.transfer_sum,
        operation=operation,
        skip_check=True,
    )
    return operation


def create_order_operations(order, invoice, default_operation_ns):
    with allure.step(u'create set of different operations'):
        operations = []
        for idx in range(default_operation_ns.operation_count):
            operation = mapper.Operation(
                cst.OperationTypeIDs.support,
                dt=datetime.datetime.now() - datetime.timedelta(default_operation_ns.operation_count - idx),
            )
            if idx % 2 == 0:
                invoice.transfer(
                    dest_order=order,
                    mode=enums.TransferType.DEST_PTS.value,
                    sum=default_operation_ns.transfer_sum,
                    operation=operation,
                    skip_check=True,
                )
            elif idx % 2 == 1:
                consume = order.consumes[-1]
                reverse_consume(
                    consume=consume,
                    operation=operation,
                    qty=default_operation_ns.unit_qty,
                )
            operations.append(operation)
        return operations

# -*- coding: utf-8 -*-
from datetime import datetime
from decimal import Decimal as D
import xmlrpclib
import random
import pytest
import hamcrest

import balance.muzzle_util as ut
from balance.mapper import *
from balance.application import getApplication
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.constants import *
from balance import discounts
from tests.tutils import get_exception_code

from tests.base import MediumTest
from tests.object_builder import *

CODE_SUCCESS = 0


class TestCreateTransfer(MediumTest):
    def test_CreateTransfer(self):
        # More of a smoke test, really
        inv1 = InvoiceBuilder().build(self.session).obj
        InvoiceTurnOn(inv1, manual=True).do()
        inv2 = InvoiceBuilder().build(self.session).obj
        ord1 = inv1.invoice_orders[0].order
        ord2 = inv2.invoice_orders[0].order
        inv1.client.make_equivalent(inv2.client)
        self.session.flush()
        callhash = {'ServiceID1': ord1.service_id,
                    'ServiceOrderID1': ord1.service_order_id,
                    'ServiceID2': ord2.service_id,
                    'ServiceOrderID2': ord2.service_order_id,
                    'CurrentQty': ord1.consume_qty,
                    'Qty': 0,
                    'Allmoney': True}
        assert ord1.consume_sum != 0
        self.assertEqual(ord2.consume_sum, 0)
        self.xmlrpcserver.CreateTransfer(self.session.oper_id, callhash)
        self.session.expire_all()
        self.assertEqual(ord1.consume_sum, 0)
        assert ord2.consume_sum != 0

    def test_CreateTransferMultiple(self):
        # More of a smoke test, really
        inv1 = InvoiceBuilder().build(self.session).obj
        InvoiceTurnOn(inv1, manual=True).do()

        inv2 = InvoiceBuilder().build(self.session).obj
        InvoiceTurnOn(inv2, manual=True).do()

        invX1 = InvoiceBuilder().build(self.session).obj
        invX2 = InvoiceBuilder().build(self.session).obj
        ord1 = inv1.invoice_orders[0].order
        ord2 = inv2.invoice_orders[0].order
        ordX1 = invX1.invoice_orders[0].order
        ordX2 = invX2.invoice_orders[0].order
        inv1.client.make_equivalent(invX1.client)
        inv1.client.make_equivalent(invX2.client)
        inv2.client.make_equivalent(invX1.client)
        inv2.client.make_equivalent(invX2.client)
        self.session.flush()
        src_list = [{'ServiceID': ord1.service_id, 'ServiceOrderID': ord1.service_order_id,
                      'QtyOld': ord1.consume_qty, 'QtyNew': 0},
                      {'ServiceID': ord2.service_id, 'ServiceOrderID': ord2.service_order_id,
                      'QtyOld': ord2.consume_qty, 'QtyNew': 0}]
        dst_list = [{'ServiceID': ordX1.service_id, 'ServiceOrderID': ordX1.service_order_id,
                      'QtyDelta': (ord1.consume_qty + ord2.consume_qty)/D(2)},
                    {'ServiceID': ordX2.service_id, 'ServiceOrderID': ordX2.service_order_id,
                      'QtyDelta': (ord1.consume_qty + ord2.consume_qty)/D(2)}]
        assert ord1.consume_sum != 0
        assert ord2.consume_sum != 0
        self.assertEqual(ordX1.consume_sum, 0)
        self.assertEqual(ordX2.consume_sum, 0)
        self.xmlrpcserver.CreateTransferMultiple(self.session.oper_id, src_list, dst_list)
        self.session.expire_all()
        self.assertEqual(ord1.consume_sum, 0)
        self.assertEqual(ord2.consume_sum, 0)
        assert ordX1.consume_sum != 0
        assert ordX2.consume_sum != 0
        self.assertEqual(ordX1.consume_sum, ordX2.consume_sum)

    def test_CreateTransferSubclient(self):
        # More of a smoke test, really
        inv1 = InvoiceBuilder().build(self.session).obj
        InvoiceTurnOn(inv1, manual=True).do()
        inv2 = InvoiceBuilder().build(self.session).obj
        ord1 = inv1.invoice_orders[0].order
        ord2 = inv2.invoice_orders[0].order
        #inv1.client.make_equivalent(inv2.client)

        clnt = ClientBuilder(is_agency=1).build(self.session).obj

        inv1.client.attach_to_agency(clnt)
        inv2.client.attach_to_agency(clnt)
        ord1.agency = clnt
        ord2.agency = clnt
        #clnt2 = ClientBuilder().build(self.session).obj
        #inv2.client.make_equivalent(clnt2)
        #clnt2.attach_to_agency(clnt)
#        clnt2.direct25 = 1

        #print '666666666666 Clients classes', clnt.class_, clnt2.class_
        self.session.flush()
        callhash = {'ServiceID1': ord1.service_id,
                    'ServiceOrderID1': ord1.service_order_id,
                    'ServiceID2': ord2.service_id,
                    'ServiceOrderID2': ord2.service_order_id,
                    'CurrentQty': ord1.consume_qty,
                    'Qty': 0,
                    'Allmoney': True}
        assert ord1.consume_sum != 0
        self.assertEqual(ord2.consume_sum, 0)
#        try:
#            self.xmlrpcserver.CreateTransfer(self.session.oper_id, callhash)
#        except Exception, e:
#            assert 'CLIENT_HAS_DIRECT25' in e.faultString
        self.xmlrpcserver.CreateTransfer(PassportBuilder().build(self.session).obj.passport_id, callhash)
        self.session.expire_all()


class TestCorrectTransfers(MediumTest):
    def _create_src(self, old_qty, new_qty):
        orders = [OrderBuilder(product=Getter(Product, DIRECT_PRODUCT_ID), client=self.client) for qty in old_qty]
        request = RequestBuilder(basket=BasketBuilder(
            rows=[
                BasketItemBuilder(order=order, quantity=qty)
                for qty, order in zip(old_qty, orders)
            ]
        ))
        person = PersonBuilder(client=request.b.basket.b.client, type='ph')
        inv = InvoiceBuilder(person=person, paysys=Getter(Paysys, 1001), request=request).build(self.session).obj
        InvoiceTurnOn(inv, manual=True).do()

        order_objs = [order.obj for order in orders]

        src_list = [
            {
                'ServiceID': order.service_id,
                'ServiceOrderID': order.service_order_id,
                'QtyOld': order.consume_qty,
                'QtyNew': qty
            } for qty, order in zip(new_qty, order_objs)
        ]

        return order_objs, src_list

    def _create_dst(self, qty_parts):
        orders = [OrderBuilder(product=Getter(Product, DIRECT_PRODUCT_ID), client=self.client) for p in qty_parts]
        map(lambda order: order.build(self.session), orders)

        order_objs = [order.obj for order in orders]

        dst_list = [
            {
                'ServiceID': order.obj.service_id,
                'ServiceOrderID': order.obj.service_order_id,
                'QtyDelta': qty
            }
            for qty, order in zip(qty_parts, orders)
        ]

        return order_objs, dst_list

    def _test(self, src_parts, dst_parts):
        if isinstance(src_parts, tuple):
            src_parts, src_result_parts = src_parts
        else:
            src_result_parts = [D(0) for q in src_parts]

        src_orders, src_list = self._create_src(src_parts, src_result_parts)
        dst_orders, dst_list = self._create_dst(dst_parts)

        self.xmlrpcserver.CreateTransferMultiple(self.session.oper_id, src_list, dst_list)

        for qty, order in zip(src_result_parts, src_orders):
            self.assertEqual(order.consume_qty, qty)

        for qty, order in zip(dst_parts, dst_orders):
            self.assertEqual(order.consume_qty, qty)
            self.assertEqual(sum(cons.current_qty for cons in order.consumes), qty)

        get_ratio = lambda cons: (cons.current_sum / cons.current_qty)

        self.assertEqual(all(get_ratio(order.consumes[0]) == D(30) for order in dst_orders), True)

    def test_CreateTransferMultiple(self):
        self.client = ClientBuilder()

        src_parts = [D(20)]
        src_result_parts = [D(0)]

        props = [ (p + 1) / 2 * p for p in range(1, 8)]
        dst_parts = [
            ut.rounded_delta(
                sum(props),
                sum(props[:p]),
                sum(props[:p + 1]),
                D(20)
            )
            for p in xrange(len(props))
        ]

        self._test((src_parts, src_result_parts), dst_parts)

        self._test([D('2')], [D('1.5'), D('0.5')])
        self._test([D('1.5'), D('0.5')], [D('2')])
        self._test([D('1'), D('1')], [D('1.5'), D('0.5')])
        self._test([D('2')], [D('1.99'), D('0.01')])


class TestTwoStepTransferMultiple(MediumTest):
    """
    https://st.yandex-team.ru/BALANCE-20227
    TODO: тест на исключение, котрое не пиклится
    """
    def setUp(self):
        super(TestTwoStepTransferMultiple, self).setUp()
        self.client = ClientBuilder().build(self.session).obj
        src_parts = [D('1'), D('1')]
        dst_parts = [D('1'), D('2')]
        src_result_parts = [D(0) for q in src_parts]
        src_orders, src_list = self._create_src(src_parts, src_result_parts)
        dst_orders, dst_list = self._create_dst(dst_parts)
        self.src_list = src_list
        self.dst_list = dst_list
        self.operation_id = self.xmlrpcserver.CreateOperation(self.session.oper_id)

    def _create_src(self, old_qty, new_qty):
        orders = [OrderBuilder(product=Getter(Product, DIRECT_PRODUCT_ID), client=self.client) for qty in old_qty]
        request = RequestBuilder(basket=BasketBuilder(
            rows=[
                BasketItemBuilder(order=order, quantity=qty)
                for qty, order in zip(old_qty, orders)
            ]
        ))

        inv = InvoiceBuilder(paysys=Getter(Paysys, 1001), request=request).build(self.session).obj
        InvoiceTurnOn(inv, manual=True).do()

        order_objs = [order.obj for order in orders]

        src_list = [
            {
                'ServiceID': order.service_id,
                'ServiceOrderID': order.service_order_id,
                'QtyOld': order.consume_qty,
                'QtyNew': qty
            } for qty, order in zip(new_qty, order_objs)
        ]

        return order_objs, src_list

    def _create_dst(self, qty_parts):
        orders = [OrderBuilder(product=Getter(Product, DIRECT_PRODUCT_ID), client=self.client) for p in qty_parts]
        map(lambda order: order.build(self.session), orders)

        order_objs = [order.obj for order in orders]

        dst_list = [
            {
                'ServiceID': order.obj.service_id,
                'ServiceOrderID': order.obj.service_order_id,
                'QtyDelta': qty
            }
            for qty, order in zip(qty_parts, orders)
        ]

        return order_objs, dst_list

    def _get_persistent_operation(self, real_session, op_type_name):
        import uuid
        from balance import mapper

        # уникальный тип операции и сама операция для теста
        op_type_uid = uuid.uuid5(uuid.NAMESPACE_OID, op_type_name)

        with real_session.begin():
            op_type = real_session.query(mapper.OperationType).filter_by(cc=str(op_type_uid)).first()
            if op_type is None:
                op_type = mapper.OperationType(id=op_type_uid.int, cc=str(op_type_uid), name=op_type_name)
                real_session.add(op_type)
                real_session.flush()

            operation = real_session.query(mapper.Operation).filter_by(type_id=op_type.id).one_or_none()
            if operation is None:
                operation = mapper.Operation(op_type_uid.int)
                real_session.add(operation)

            operation.passport_id = self.session.oper_id
        return operation

    def test_CreateOperation(self):
        self.assertIsNotNone(self.operation_id)

    def test_successful_transfer(self):
        """
        Проверяет, что при успешном переносе в status записался success
        и при повторном вызове метода получим тот же результат.
        """
        res = self.xmlrpcserver.CreateTransferMultiple(
            self.session.oper_id, self.src_list, self.dst_list, 1, self.operation_id
        )
        operation = self.session.query(Operation).getone(self.operation_id)
        self.assertEqual(operation.status, "success")
        # self.assertEqual(
        #     res,
        #     self.xmlrpcserver.CreateTransferMultiple(
        #         self.session.oper_id, self.src_list, self.dst_list, 1, self.operation_id
        #     )
        # )

    def test_faulty_transfer(self):
        """
        Проверяет, что при ошибочном переносе получаем исключение и
        в error записалось исключение.
        """
        self.assertFault("CLIENTS_NOT_MATCH", self.xmlrpcserver.CreateTransferMultiple,
                         self.session.oper_id, self.src_list, [], 1, self.operation_id)
        operation = self.session.query(Operation).getone(self.operation_id)
        self.assertEqual(operation.status, "error")
        self.assertIsNotNone(operation.error)
        self.assertFault("CLIENTS_NOT_MATCH", self.xmlrpcserver.CreateTransferMultiple,
                         self.session.oper_id, self.src_list, [], 1, self.operation_id)

    def test_operation_in_progress(self):
        """
        Проверяет, что при попытке выполнить уже выполняющуюся операцию
        получаем исключение. Создаются и сохраняются в базу уникальный тип операции
        и операция из-за необходимости лока операции в другой сессии.
        """
        real_session = getApplication().real_new_session()
        op_type_name = 'TestTwoStepTransferMultiple.test_operation_in_progress'
        operation = self._get_persistent_operation(real_session, op_type_name)

        real_session.begin()
        operation = real_session.query(Operation).with_lockmode(
            'update_nowait').getone(operation.id)
        self.assertFault(
            "OPERATION_IN_PROGRESS", self.xmlrpcserver.CreateTransferMultiple,
            self.session.oper_id, self.src_list, self.dst_list, 1, operation.id
        )
        real_session.rollback()

    def test_permission_denied(self):
        """
        Проверяет, что при попытке вызвать CreateTransferMultiple с
        passport_id отличным от того, с которым была создана операция,
        получим исключение.
        """

        self.assertFault(
            "PERMISSION_DENIED", self.xmlrpcserver.CreateTransferMultiple,
            PassportBuilder().build(self.session).obj.passport_id, self.src_list, self.dst_list, 1, self.operation_id
        )

    def test_input_after_second_call(self):
        """
        Проверяет, что после умешного вызова метода при повторном вызове
        с другими параметрами, input не перезаписывается.
        """
        self.xmlrpcserver.CreateTransferMultiple(
            self.session.oper_id, self.src_list, self.dst_list, 1, self.operation_id
        )
        operation = self.session.query(Operation).getone(self.operation_id)
        input_ = operation.input
        self.xmlrpcserver.CreateTransferMultiple(
            self.session.oper_id, self.src_list, [], 1, self.operation_id
        )
        self.assertEqual(input_, operation.input)


class TestGetDailyConsume(MediumTest):
    # We only need to check if the new python method does the same as c++ method, and the only
    # thing old method does is executes sql, gets result from first row or 0 if no rows were
    # returned and puts it in a stupid (0, 'SUCCESS', <actual result>) tuple.
    sql = """
    select round(a.qty - b.qty,2) qty
        from
        (
          select sum(q.consume_qty) qty
        	from t_consume q, t_order o
    		where q.parent_order_id = o.id
            and q.dt >= to_date(:dt, 'dd.mm.yyyy') and q.dt < to_date(:dt, 'dd.mm.yyyy')+1
    		and o.service_id = 7
        ) a,
        (
            select sum(q.reverse_qty) qty
        		from t_reverse q, t_order o
                where q.parent_order_id = o.id
                and q.dt >= to_date(:dt, 'dd.mm.yyyy') and q.dt < to_date(:dt, 'dd.mm.yyyy')+1
        		and o.service_id = 7
        ) b"""

    def _test_result(self):
        testdates = [
            datetime(2009, 3, 3),
            datetime(2009, 2, 1),
            datetime(2008, 10, 20),
            datetime(2008, 6, 2)]
        for dt in testdates:
            old_res = self.session.execute(self.sql, {'dt': dt.strftime('%d.%m.%Y')})\
                    .fetchone()[0] or 0
            old_res = ut.float2decimal4(old_res)
            new_res = self.xmlrpcserver.GetDailyConsume(dt)
            self.assertEqual(new_res[0], CODE_SUCCESS)
            self.assertEqual(new_res[1], 'SUCCESS')
            self.assertEqual(D(new_res[2]), old_res)


def consume_order(order, consumes_qtys, discount_pct=0, dt=None):
    session = order.session
    client = order.agency or order.client
    person = PersonBuilder(client=client).build(session).obj
    paysys = Getter(mapper.Paysys, 1000).build(session).obj

    def mock_discounts(ns):
        return discounts.DiscountProof('mock', discount=discount_pct, adjust_quantity=False), \
               None, None

    def mock_update_taxes(self, qty, sum_):
        return self._unchanged(qty, sum_)

    patch_tax = mock.patch('balance.actions.taxes_update.TaxUpdater.calculate_updated_parameters', mock_update_taxes)
    patch_discounts = mock.patch('balance.discounts.calc_from_ns', mock_discounts)

    with patch_tax, patch_discounts:
        for qty in consumes_qtys:
            invoice = InvoiceBuilder(
                person=person,
                paysys=paysys,
                request=RequestBuilder(
                    basket=BasketBuilder(
                        client=client,
                        dt=dt,
                        rows=[BasketItemBuilder(order=order, quantity=qty)]
                    )
                )
            ).build(session).obj
            invoice.create_receipt(invoice.effective_sum)
            invoice.turn_on_rows(cut_agava=True)


@pytest.mark.parametrize(
    'reverse, is_agency, same_client, error',
    [
        pytest.param(0, 0, 1, None, id='base case'),
        pytest.param(1, 0, 1, None, id='direct -> zen'),
        pytest.param(0, 1, 1, None, id='w agency and same subclients'),
        pytest.param(0, 1, 0, None, id='w different subclients'),
        pytest.param(0, 0, 0, 'client should be the same class', id='w different clients'),
    ],
)
def test_zen_transfer(session, medium_xmlrpc, reverse, is_agency, same_client, error):
    agency = None
    if is_agency:
        agency = ClientBuilder.construct(session, is_agency=1)
    client1 = client2 = ClientBuilder.construct(session, agency=agency)
    if not same_client:
        client2 = ClientBuilder.construct(session, agency=agency)

    main_order = OrderBuilder.construct(session, client=client1, agency=agency, product_id=DIRECT_PRODUCT_RUB_ID, is_ua_optimize=True)
    main_order.force_log_tariff()
    session.flush()
    zen_order = OrderBuilder.construct(session, client=client2, agency=agency, service_id=cst.ServiceId.ZEN_SALES, product_id=509735)

    consume_order(main_order, [1])
    consume_order(zen_order, [1])

    hamcrest.assert_that(
        zen_order,
        hamcrest.has_properties(
            consume_qty=1,
            consume_sum=D('1.2'),
        ),
    )
    hamcrest.assert_that(
        main_order,
        hamcrest.has_properties(
            consume_qty=1,
            consume_sum=1,
        ),
    )

    params = {
        'CurrentQty': '1',
        'Qty': '0',
        'AllMoney': '1',
    }
    for ind, o in enumerate([zen_order, main_order] if not reverse else [main_order, zen_order]):
        params['ServiceID%s' % (ind + 1)] = o.service_id
        params['ServiceOrderID%s' % (ind + 1)] = o.service_order_id

    if error:
        with pytest.raises(xmlrpclib.Fault) as exc_info:
            medium_xmlrpc.CreateTransfer(
                session.oper_id,
                params,
            )
        assert get_exception_code(exc_info.value, 'msg') == error

    else:
        medium_xmlrpc.CreateTransfer(
            session.oper_id,
            params,
        )

        hamcrest.assert_that(
            zen_order,
            hamcrest.has_properties(
                consume_qty=0 if not reverse else D('1.833333'),
                consume_sum=0 if not reverse else D('2.2'),
            ),
        )
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=D('2.2') if not reverse else 0,
                consume_sum=D('2.2') if not reverse else 0,
            ),
        )

# -*- coding: utf-8 -*-
"""
Tests for funds movement, see BALANCE-4603

Test that *initial* receipts are created and reverted corrrectly (BALANCE-4604):
    TestInvoiceTurnOn

Test that shipments are accepted and processed correctly (BALANCE-4657):
    TestShipments

Simple test for acts creation:
    TestActs

Tests for routines related to invoice.turn_on ported from pl/sql
(BALANCE-4835)
    TestPortedRoutines

Credit-related test
    TestCredits
"""

import pickle
import datetime
from decimal import Decimal as D
import contextlib
import pytest


import sqlalchemy as sa

from balance.mapper import Export, Consume, exportable, Product, Paysys, Act
from balance.mapper import ThirdPartyTransaction, ThirdPartyCorrection
import balance.actions.acts as a_a
from balance.actions.cache import get_cached_consume
from balance.constants import ExportState, PaymentMethodIDs, ServiceId
from balance import queue_processor
from balance import exc

from tests.base_routine import BalanceRoutineTest
from tests.object_builder import ClientBuilder, OrderBuilder, Getter, InvoiceBuilder, BasketBuilder, \
                                 RequestBuilder, TrustPaymentBuilder, BasketItemBuilder, RefundBuilder

from cluster_tools.process_cache_queue import ProcessCacheQueue


class TestExportQueue(BalanceRoutineTest):
    PAYSYS_ID = 1000

    def setUp(self):
        super(TestExportQueue, self).setUp()
        self.process_completions_queue = queue_processor.QueueProcessor('PROCESS_COMPLETION')

    def _order_process_queue(self):
        self.order.enqueue('PROCESS_COMPLETION')  # Гарантируем создание объекта класса Export
        export_object = self.order.exports['PROCESS_COMPLETION']
        self.process_completions_queue.process_one(export_object)
        assert export_object.state == ExportState.exported, (export_object.state, export_object.error)
        self.session.flush()
        self.session.expire_all()

    @contextlib.contextmanager
    def context_check(self, fun, before, after):
        self.assertEqual(fun(), before)
        yield
        self.session.flush()
        self.assertEqual(fun(), after)

    def test_adding_in_queue(self):
        client = ClientBuilder().build(self.session).obj

        q_text = '''begin
            pk_export_queue.enqueue(:id, :cls, :queue, :priority, :input, :reason);
        end;'''
        q = sa.text(q_text, bindparams=[sa.bindparam('input', type_=sa.PickleType.impl)])

        params = {
            'queue': 'OVERDRAFT',
            'id': client.id,
            'priority': 0,
            'cls': 'Client',
            'input': pickle.dumps({'Test data': 'yesyesyes'}),
            'reason': "Tests reason"
        }

        expreq = self.session.query(Export).filter(
            Export.type == 'OVERDRAFT',
            Export.classname == 'Client',
            Export.object_id == client.id
        )

        with self.context_check(lambda: expreq.filter(Export.state == ExportState.enqueued).count(), 0, 1):
            self.session.execute(q, params)
            self.session.flush()

        exp_row = expreq.first()
        self.session.refresh(exp_row)
        self.assertEqual(exp_row.state, ExportState.enqueued)
        self.assertEqual(exp_row.export_dt, None)
        self.assertEqual(exp_row.reason, params['reason'])

        exp_row.skip_export()
        self.session.flush()

        rows = self.session.execute(
            "SELECT * FROM t_export_history_3 WHERE object_id = :object_id AND state = :state",
            {'object_id': client.id, 'state': ExportState.enqueued}
        ).fetchall()
        assert len(rows) == 1, rows
        self.assertEqual(rows[-1].reason, params['reason'])

    def test_cache_archive_consumes(self):
        expreq = self.session.query(Export).filter(
            Export.type == 'PROCESS_CACHE',
            Export.classname == 'Order',
            Export.object_id == self.order.id
        )

        exp_count = lambda: expreq.filter(Export.state == ExportState.enqueued).count()

        with self.context_check(exp_count, 0, 0):
            for _ in range(5):
                self._create_invoice(D('100'))

            for i in [25, 50, 100, 150]:
                self._add_shipment(bucks=D(i))
                self._order_process_queue()

        with self.context_check(exp_count, 0, 1):
            a_a.ActAccounter(self.order.client, a_a.ActMonth(for_month=self.dt), force=1).do()

        with self.context_check(exp_count, 1, 1):
            for i in [5, 100, 200]:
                self._add_shipment(bucks=D(i))
                self._order_process_queue()

        with self.context_check(exp_count, 1, 0):
            self.order.enqueue(ProcessCacheQueue.exporter_type)  # Гарантируем создание объекта класса Export
            pcq = ProcessCacheQueue(self.cfg)
            pcq.session = self.session
            pcq.export_one(self.order.exports[ProcessCacheQueue.exporter_type])
            self.session.flush()

        consumes = list(self.session.query(Consume).filter(
            Consume.order == self.order,
            Consume.current_qty == Consume.completion_qty,
            Consume.act_qty == Consume.completion_qty
        ))

        consumes_archive = list(self.session.query(Consume).filter(
            Consume.order == self.order,
            Consume.archive == True
        ))

        self.assertSetEqual(set(consumes), set(consumes_archive))

        consume_archive_sums = {
            'CURRENT_QTY': sum(c.current_qty for c in consumes),
            'CURRENT_SUM': sum(c.current_sum for c in consumes),
        }

        cache_rows = self.session.execute(
            "SELECT * FROM t_order_cache WHERE order_id = :object_id",
            {'object_id': self.order.id}
        ).fetchall()

        cache_values = dict()
        for row in cache_rows:
            if row.cache_type in consume_archive_sums:
                cache_values[row.cache_type] = row.cache_val

        for cache_type, cache_val in consume_archive_sums.iteritems():
            self.assertEqual(cache_val, cache_values.get(cache_type))

        assert get_cached_consume(self.session, self.order) is not None

        # Legacy. Насколько я понял, здесь просто проверяется relation
        assert self.order.not_archive_consumes is not None


class TestExportOne(object):
    rate = exportable.Export.max_rate

    def test_ok(self, session):
        client = ClientBuilder().build(session).obj
        obj = client.exports['OEBS']

        def process(export_obj):
            pass
        exportable.export_one(obj, process, self.rate)
        assert obj.input is None
        assert obj.output is None
        assert obj.state == 1

    @pytest.mark.parametrize(
        'lambda_enqueue_dt, delay_minutes, max_deferment_time, expected_state',
        [
            (lambda: None,                                                    0, None,   ExportState.enqueued),
            (lambda: datetime.datetime.now(),                                 5,    0, ExportState.failed),
            (lambda: datetime.datetime.now() - datetime.timedelta(days=666),  5, None, ExportState.enqueued),
            (lambda: datetime.datetime.now() - datetime.timedelta(days=666), 60,   -1, ExportState.enqueued),
            (lambda: datetime.datetime.now(),                                 5,    1, ExportState.enqueued),
            (lambda: datetime.datetime.now() - datetime.timedelta(days=666),  5,  667, ExportState.enqueued),
            (lambda: datetime.datetime.now() - datetime.timedelta(days=666),  5,  666, ExportState.failed),
        ]
    )
    def test_deferred_error(self, session, lambda_enqueue_dt, delay_minutes, max_deferment_time, expected_state):
        input = 'Deferred_error_input'
        output = 'Deffered_error_output'
        error_text = 'Таск фейлед саксесфули'
        expected_error_text = u'Грустная ошибка: Таск фейлед саксесфули'
        client = ClientBuilder().build(session).obj
        obj = client.exports['OEBS']

        class SAD_EXCEPTION(exc.DEFERRED_ERROR):
            _format = u'Грустная ошибка: %(msg)s'
            def __init__(self, msg, *args, **kwargs):
                self.msg = msg
                super(SAD_EXCEPTION, self).__init__(*args, **kwargs)

        def process(export_obj):
            e = SAD_EXCEPTION(error_text, input=input, output=output, delay_minutes=delay_minutes)
            raise e
        # лямбды - чтобы datetime.datetime.now() происходило в реальном выполенении теста.
        enqueue_dt = lambda_enqueue_dt()
        obj.enqueue_dt = enqueue_dt
        exportable.export_one(obj, process, self.rate, max_deferment_time=max_deferment_time)
        assert obj.input == input
        assert obj.output == output
        session.flush()

        assert obj.state == expected_state
        # defer должен проставлять enqueue_dt, если его не было (кейс старых экспортов)
        if not enqueue_dt:
            assert obj.enqueue_dt

        if expected_state == ExportState.failed:
            assert obj.error == u'Maximum deferment time is exceeded. Delay message: {}'.format(expected_error_text)
            assert not obj.next_export
        elif expected_state == ExportState.enqueued:
            assert obj.error == expected_error_text
            now = datetime.datetime.now()
            assert now + datetime.timedelta(minutes=delay_minutes-1) < \
                   obj.next_export < now + datetime.timedelta(minutes=delay_minutes+1)


    @pytest.mark.parametrize(
        'exc, input, output, state, rate',
        [
            (Exception(), None, None, ExportState.enqueued, 1),
            (exc.CRITICAL_ERROR(), 'Critical_error_input', 'Critical_error_output', ExportState.failed, 1),
            (exc.EXACT_FINAL_STATE_ERROR(ExportState.support), 'Exact_final_state_input',
             'Exact_final_state_output', ExportState.support, 1),
            (exportable.SkipExport(), None, None, ExportState.exported, 0)

        ]
    )
    def test_state_after_exc(self, session, exc, input, output, state,rate):
        client = ClientBuilder().build(session).obj
        obj = client.exports['OEBS']

        def process(export_obj):
            exc.input = input
            exc.output = output
            raise exc
        exportable.export_one(obj, process, self.rate)
        assert obj.input == input
        assert obj.output == output
        assert obj.state == state
        assert obj.rate == rate

    def test_failure_max_rate(self, session):
        client = ClientBuilder().build(session).obj
        obj = client.exports['OEBS']
        obj.max_rate = 10
        obj.rate = 9

        def process(export_obj):
            raise Exception()
        exportable.export_one(obj, process, self.rate)
        assert obj.input is None
        assert obj.output is None
        assert obj.state == ExportState.failed
        assert obj.rate == 10

    def test_failed_delay(self, session):
        client = ClientBuilder().build(session).obj
        obj = client.exports['OEBS']

        def process(export_obj):
            raise Exception()
        exportable.export_one(obj, process, self.rate, failed_delay=3)
        session.flush()
        assert datetime.timedelta(minutes=2) < obj.next_export - datetime.datetime.now() < datetime.timedelta(minutes=4)
        assert obj.state == ExportState.enqueued


class TestEnqueuer(BalanceRoutineTest):
    def test_enqueue_batch(self):
        orders = [self._create_order() for i in xrange(0, 10)]
        orders[0].enqueue_batch(
            self.session, 'PROCESS_COMPLETION',
            [(o.id, (-1, {'for_dt': datetime.datetime.now(), 'use_completion_history': True}))
             for o in orders]
        )


def test_fix_priority(session):

    payment_match_filter = TrustPaymentBuilder.construct(session)
    payment_match_filter.service_id=ServiceId.ZAXI
    payment_match_filter.payment_method='virtual::deposit'

    assert payment_match_filter._fix_priority(0, 'THIRDPARTY_TRANS') == 0
    assert payment_match_filter._fix_priority(2, 'THIRDPARTY_TRANS') == 2

    payment_not_match_filter = TrustPaymentBuilder.construct(session)

    assert payment_not_match_filter._fix_priority(0, 'THIRDPARTY_TRANS') == 2

    refund_match_filter = RefundBuilder.construct(session, payment=payment_match_filter, amount=200,
                                                    description='', operation=None)
    refund_match_filter.service_id=ServiceId.ZAXI

    assert refund_match_filter._fix_priority(0, 'THIRDPARTY_TRANS') == 0

    refund_not_match_filter = RefundBuilder.construct(session, payment=payment_not_match_filter, amount=200,
                                                        description='', operation=None)

    assert refund_not_match_filter._fix_priority(0, 'THIRDPARTY_TRANS') == 2

    tpt_match_filter = ThirdPartyTransaction(service_id=ServiceId.TAXI_PAYMENT,
                                             payment_type='deposit'
                                             )

    assert tpt_match_filter._fix_priority(0, 'OEBS') == 0
    assert tpt_match_filter._fix_priority(0, 'OEBS_API') == 0

    tpt_not_match_filter = ThirdPartyTransaction(service_id=ServiceId.TAXI_PAYMENT,
                                                 payment_type='deposit_payout'
                                                 )

    assert tpt_not_match_filter._fix_priority(0, 'OEBS') == 2
    assert tpt_not_match_filter._fix_priority(0, 'OEBS_API') == 2

    tpc_match_filter = ThirdPartyCorrection(service_id=ServiceId.TAXI_PAYMENT,
                                            payment_type='deposit'
                                            )

    assert tpc_match_filter._fix_priority(0, 'OEBS') == 0
    assert tpc_match_filter._fix_priority(0, 'OEBS_API') == 0

    tpc_not_match_filter = ThirdPartyCorrection(service_id=ServiceId.TAXI_PAYMENT,
                                                payment_type='deposit_payout'
                                                )

    assert tpc_not_match_filter._fix_priority(0, 'OEBS') == 2
    assert tpc_not_match_filter._fix_priority(0, 'OEBS_API') == 2

    assert Act._fix_priority(0, 'OEBS') == 1
    assert Act._fix_priority(0, 'OEBS_API') == 1


@pytest.mark.parametrize('string', [u'Привет', 'Привет'], ids=['unicode', 'utf8'])
@pytest.mark.parametrize('method', [
    lambda string, export_obj: export_obj.failure(string, string, input_=string, output=string),
    lambda string, export_obj: export_obj.defer(string, string, delay=1, input_=string, output=string),
    lambda string, export_obj: export_obj.success(output=string),
    lambda string, export_obj: export_obj.export(input_=string),
], ids=['failure', 'defer', 'success', 'export'])
def test_string_type_sync(session, string, method):
    """ BALANCE-32417 """
    client = ClientBuilder.construct(session)
    client.enqueue('OEBS', force=True)
    export = client.exports['OEBS']
    export.input = export.output = export.error = export.traceback = u'Юникод'
    session.flush()
    session.refresh(export)
    method(string, export)
    try:
        # There must be no warnings (which are turned to errors in conftest.py)
        session.flush()
    except UnicodeWarning:
        pytest.fail('Improper strings comparison')

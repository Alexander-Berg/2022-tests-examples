# -*- coding: utf-8 -*-

import datetime
import decimal

import pytest
import hamcrest as hm
import mock

from balance import mapper
from balance import scheme
from balance.actions.consumption import reverse_consume
from balance.son_schema.invoices import DailyActRequestSchema
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    TransferMode,
    ExportState,
)
from cluster_tools import log_tariff_daily_acts

from tests import object_builder as ob


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture
def mock_check_start():
    with mock.patch('cluster_tools.log_tariff_daily_acts._check_can_start', return_value=True):
        yield


def create_order(client):
    return ob.OrderBuilder.construct(
        client.session,
        client=client,
        product_id=DIRECT_PRODUCT_RUB_ID,
    )


def create_invoice(client, order_qtys):
    return ob.InvoiceBuilder(
        person=ob.PersonBuilder(client=client),
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(order=order, quantity=qty)
                    for order, qty in order_qtys
                ]
            )
        )
    ).build(client.session).obj


def mk_shipment(o, qty):
    o.calculate_consumption(datetime.datetime.now(), {o.shipment_type: qty})
    o.session.flush()


def mk_log_tariff(o):
    o._is_log_tariff = 1
    o.session.flush()


def assert_export(req, state):
    export = (
        req.session.query(scheme.export_ng)
        .filter_by(object_id=req.invoice_id,
                   type='LOGBROKER-ACT-REQUEST')
        .one_or_none()
    )
    if state is None:
        assert export is None
    else:
        hm.assert_that(
            export,
            hm.has_properties(state=state)
        )


@pytest.mark.usefixtures('mock_check_start')
class TestEnqueue(object):
    @pytest.mark.parametrize(
        'invoice_qty, receipt_sum, consume_qty, completion_qty, act_qty, res_sum',
        [
            pytest.param(10, 10, 10, 10, 0, 10, id='new'),
            pytest.param(10, 10, 10, 10, 1, 9, id='acted'),
            pytest.param(10, 10, 10, 9, 0, None, id='part new'),
            pytest.param(10, 10, 10, 9, 1, None, id='part acted'),
            pytest.param(10, 10, 10, 10, 10, None, id='full acted'),
            pytest.param(10, 10, 10, 9, 10, None, id='overacted'),
            pytest.param(10, 9, 9, 9, 0, 9, id='underpaid'),
            pytest.param(10, 5, 9, 9, 5, 4, id='underpaid acted'),
        ]
    )
    def test_amounts(self, session, client, invoice_qty, receipt_sum, consume_qty, completion_qty, act_qty, res_sum):
        o = create_order(client)
        i = create_invoice(client, [(o, invoice_qty)])
        if receipt_sum:
            i.create_receipt(receipt_sum)
        if consume_qty:
            i.transfer(o, TransferMode.dst, consume_qty, skip_check=True)
        if act_qty:
            mk_shipment(o, act_qty)
            i.generate_act(backdate=datetime.datetime.now(), force=True)
        mk_shipment(o, completion_qty)
        mk_log_tariff(o)

        log_tariff_daily_acts.enqueue_daily_acts(session, client_ids=[client.id])
        req = session.query(mapper.DailyActRequest).get(i.id)

        if res_sum:
            hm.assert_that(
                req,
                hm.has_properties(
                    amount=res_sum,
                    version_id=0
                )
            )
            assert_export(req, ExportState.enqueued)
        else:
            assert req is None

    def test_multiple_consumes(self, session, client):
        o1, o2 = [create_order(client) for _ in range(2)]
        i = create_invoice(client, [(o1, 10), (o2, 10)])
        i.create_receipt(20)
        i.transfer(o1, TransferMode.dst, 16, skip_check=True)
        i.transfer(o2, TransferMode.dst, 10, skip_check=True)

        mk_shipment(o1, 4)
        mk_shipment(o2, 6)
        i.generate_act(backdate=datetime.datetime.now(), force=True)

        mk_shipment(o1, 16)
        mk_shipment(o2, 4)
        reverse_consume(o2.consumes[0], None, 6)

        mk_log_tariff(o1)
        mk_log_tariff(o2)

        log_tariff_daily_acts.enqueue_daily_acts(session, client_ids=[client.id])
        req = session.query(mapper.DailyActRequest).get(i.id)

        assert req.amount == 10

    def test_multiple_invoices(self, session, client):
        o = create_order(client)
        invoices = []
        for _ in range(3):
            i = create_invoice(client, [(o, 10)])
            i.create_receipt(10)
            i.transfer(o, TransferMode.dst, 10)
            invoices.append(i)
        mk_shipment(o, 30)
        mk_log_tariff(o)

        log_tariff_daily_acts.enqueue_daily_acts(session, client_ids=[client.id])
        reqs = session.query(mapper.DailyActRequest).filter(mapper.DailyActRequest.invoice_id.in_(i.id for i in invoices))
        hm.assert_that(
            reqs,
            hm.contains_inanyorder(*[
                hm.has_properties(invoice_id=i.id, amount=10, version_id=0)
                for i in invoices
            ])
        )

    def test_update_existing(self, session, client):
        o = create_order(client)
        i = create_invoice(client, [(o, 10)])
        i.create_receipt(10)
        i.transfer(o, TransferMode.dst, 10)
        mk_shipment(o, 10)
        mk_log_tariff(o)

        req = ob.DailyActRequestBuilder.construct(session, invoice=i, amount=666)

        log_tariff_daily_acts.enqueue_daily_acts(session, client_ids=[client.id])
        session.refresh(req)

        hm.assert_that(
            req,
            hm.has_properties(
                amount=10,
                version_id=1
            )
        )
        assert_export(req, ExportState.enqueued)

    @pytest.mark.parametrize('with_receipt', [False, True])
    def test_overdraft(self, session, client, with_receipt):
        o = create_order(client)
        i = create_invoice(client, [(o, 10)])
        if with_receipt:
            i.create_receipt(10)
        i.transfer(o, TransferMode.dst, 10, skip_check=True)
        i.type = 'overdraft'
        mk_shipment(o, 10)
        mk_log_tariff(o)

        log_tariff_daily_acts.enqueue_daily_acts(session, client_ids=[client.id])
        req = session.query(mapper.DailyActRequest).get(i.id)
        assert req.amount == 10

    def test_wrong_invoice_type(self, session, client):
        o = create_order(client)
        i = create_invoice(client, [(o, 10)])
        i.create_receipt(10)
        i.transfer(o, TransferMode.dst, 10, skip_check=True)
        i.type = 'fictive'
        mk_shipment(o, 10)
        mk_log_tariff(o)

        log_tariff_daily_acts.enqueue_daily_acts(session, client_ids=[client.id])
        assert session.query(mapper.DailyActRequest).get(i.id) is None


@pytest.mark.usefixtures('mock_check_start')
class TestSerialization(object):
    def test_base(self, session, client):
        o = create_order(client)
        i = create_invoice(client, [(o, 10)])
        req = ob.DailyActRequestBuilder.construct(
            session,
            invoice=i,
            amount=decimal.Decimal('6.66'),
            version_id=666,
        )
        session.expire_all()

        data = DailyActRequestSchema().dump(req).data
        assert data == {
            'amount': decimal.Decimal('6.66'),
            'invoice_id': i.id,
            'update_dt': req.update_dt.strftime('%Y-%m-%dT%H:%M:%S+03:00'),
            'version_id': 666
        }


@pytest.mark.parametrize(
    'curr_dt, is_ok',
    [
        pytest.param(datetime.datetime(2021, 1, 30), True, id='ok'),
        pytest.param(datetime.datetime(2021, 1, 31), False, id='last_day'),
    ])
def test_check_start(session, client, curr_dt, is_ok):
        order = create_order(client)
        i = create_invoice(client, [(order, 10)])
        i.create_receipt(10)
        i.transfer(order, TransferMode.all)
        mk_shipment(order, 10)
        mk_log_tariff(order)

        from cluster_tools.log_tariff_daily_acts import _check_can_start

        def _mock_check():
            with mock.patch('datetime.datetime', mock.Mock(now=mock.Mock(return_value=curr_dt))):
                return _check_can_start()

        with mock.patch('cluster_tools.log_tariff_daily_acts._check_can_start', _mock_check):
            log_tariff_daily_acts.enqueue_daily_acts(session, client_ids=[client.id])

        req = session.query(mapper.DailyActRequest).get(i.id)
        if is_ok:
            assert req.amount == 10
        else:
            assert req is None
# mock.patch('datetime.datetime', mock.Mock(today=mock.Mock(return_value=dt.datetime(2021, 1, 1)))):

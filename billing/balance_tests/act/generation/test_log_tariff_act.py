# -*- coding: utf-8 -*-

import datetime
import decimal

import pytest
import hamcrest as hm
import mock

import balance.processors.log_tariff_act
from balance import core
from balance import muzzle_util as ut
from balance import scheme
from balance import mapper
from balance.queue_processor import QueueProcessor
from balance.constants import (
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_MEDIA_PRODUCT_RUB_ID,
    ExportState,
    TAX_POLICY_PCT_ID_18_RUSSIA,
)

from tests.base_routine import consumes_match
from tests.tutils import mock_transactions
from tests import object_builder as ob

from tests.balance_tests.act.generation.common import (
    create_order,
    create_invoice,
    create_consume,
    consume_credit,
    calculate_consumption,
    generate_act,
)

D = decimal.Decimal

TODAY = ut.trunc_date(datetime.datetime.now())
YESTERDAY = TODAY - datetime.timedelta(1)
TOMORROW = TODAY + datetime.timedelta(1)
FUTURE = TODAY + datetime.timedelta(2)


def assert_consumes(invoice, states):
    hm.assert_that(
        invoice.consumes,
        consumes_match(states, forced_params=['id', 'act_qty', 'act_sum'])
    )


def gen_act_eid(session):
    return 'Y-%s' % session.execute('select bo.s_act_external_id.nextval from dual').scalar()


def create_act_row(
    act_eid,
    act_dt,
    consume,
    qty,
    sum_,
    tax_policy_pct_id=None,
    log_type_id=1,
    service_order_id=None,
    hidden=0,
    ticket_id=None
):
    session = consume.session
    params = {
        'log_type_id': log_type_id,
        'act_eid': act_eid,
        'act_dt': ut.datetime2timestamp(act_dt),
        'invoice_id': consume.invoice_id,
        'consume_id': consume.id,
        'acted_qty': qty,
        'acted_sum': sum_,
        'tax_policy_pct_id': tax_policy_pct_id or consume.tax_policy_pct_id,
        'service_order_id': service_order_id,
        'hidden': hidden,
        'ticket_id': ticket_id
    }
    session.execute(scheme.log_tariff_act_row.insert(), params)


def process_act_gen(invoice, log_type_id=1):
    export_type = 'LOG_TARIFF_ACT'
    invoice.enqueue(export_type, input_={'log_type_id': log_type_id}, force=True)
    export_obj = invoice.exports[export_type]
    QueueProcessor(export_type).process_one(export_obj)
    return export_obj


def test_closed_month(session, client):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=66)
    consume, = invoice.consumes
    act_eid = gen_act_eid(session)
    create_act_row(act_eid, TODAY, consume, 900, 900)

    with mock.patch(
        'balance.processors.log_tariff_act.can_close_invoice',
        return_value=False,
    ):
        export_obj = process_act_gen(invoice)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error='Closed month',
            state=ExportState.enqueued,
        )
    )


@pytest.mark.parametrize(
    'act_dt',
    [
        pytest.param(YESTERDAY, id='yesterday'),
        pytest.param(TODAY, id='today'),
        pytest.param(TOMORROW, id='tomorrow'),
    ]
)
def test_act_dt(session, client, person, act_dt):
    order1 = create_order(session, client)
    order2 = create_order(session, client)
    invoice = create_invoice(session, client, [order1], qty=66)
    consume1, = invoice.consumes
    consume2 = create_consume(invoice, order2, qty=66)

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, act_dt, consume1, 900, 900)
    create_act_row(act_eid, act_dt, consume2, 1080, 1080)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act, = invoice.acts

    hm.assert_that(
        act,
        hm.has_properties(
            amount=1980,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid,
            type='generic',
            dt=act_dt,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=30,
                    act_sum=900,
                ),
                hm.has_properties(
                    consume=consume2,
                    act_qty=36,
                    act_sum=1080
                )
            )
        )
    )

    hm.assert_that(consume1.act_money, hm.equal_to(900))
    hm.assert_that(consume2.act_money, hm.equal_to(1080))


def test_multiple_acts(session, client):
    order1 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
    order2 = create_order(session, client, product=DIRECT_PRODUCT_ID)
    invoice = create_invoice(session, client, [order1], qty=900)
    consume1, = invoice.consumes
    consume2 = create_consume(invoice, order2, qty=30)

    act_eid1 = gen_act_eid(session)
    act_eid2 = gen_act_eid(session)
    create_act_row(act_eid1, YESTERDAY, consume1, 600, 600)
    create_act_row(act_eid1, YESTERDAY, consume2, 450, 450)
    create_act_row(act_eid2, TODAY, consume2, 450, 450)
    create_act_row(act_eid2, TODAY, consume1, 300, 300)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act1, act2 = sorted(invoice.acts, key=lambda a: -a.amount)

    hm.assert_that(
        act1,
        hm.has_properties(
            amount=1050,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid1,
            dt=YESTERDAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=600,
                    act_sum=600
                ),
                hm.has_properties(
                    consume=consume2,
                    act_qty=15,
                    act_sum=450
                )
            )
        )
    )
    hm.assert_that(
        act2,
        hm.has_properties(
            amount=750,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid2,
            dt=TODAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=300,
                    act_sum=300
                ),
                hm.has_properties(
                    consume=consume2,
                    act_qty=15,
                    act_sum=450
                )
            )
        )
    )

    hm.assert_that(consume1.act_money, hm.is_(None))
    hm.assert_that(consume2.act_money, hm.equal_to(900))


def test_several_rows_bucks(session, client):
    order = create_order(session, client, product=DIRECT_PRODUCT_ID)
    invoice = create_invoice(session, client, [order], qty=900)
    consume, = invoice.consumes

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, YESTERDAY, consume, 300, 300)
    create_act_row(act_eid, YESTERDAY, consume, 450, 450)
    create_act_row(act_eid, YESTERDAY, consume, 150, 150)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )

    hm.assert_that(
        invoice.acts[0],
        hm.has_properties(
            amount=900,
            tax_policy_pct=consume.tax_policy_pct,
            external_id=act_eid,
            dt=YESTERDAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume,
                    act_qty=30,
                    act_sum=900
                )
            )
        )
    )

    hm.assert_that(consume.act_money, hm.equal_to(900))


def test_bucks_interim_rollback_to_zero(session, client):
    order = create_order(session, client, product=DIRECT_PRODUCT_ID)
    alt_order = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
    invoice = create_invoice(session, client, [order], qty=900)
    consume, = invoice.consumes

    calculate_consumption(order, TODAY, 10)
    generate_act(invoice, TODAY)

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, TOMORROW, consume, 300, 300)
    create_act_row(act_eid, TOMORROW, consume, -300, -300, service_order_id=alt_order.service_order_id)

    base_func = balance.processors.log_tariff_act.create_act

    def mock_func(*args):
        args = list(args)
        args[-1] = sorted(args[-1], key=lambda r: r.acted_qty)
        return base_func(*args)

    with mock.patch('balance.processors.log_tariff_act.create_act', mock_func):
        export_obj = process_act_gen(invoice)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    old_act, act, = sorted(invoice.acts, key=lambda a: a.external_id)

    hm.assert_that(
        act,
        hm.has_properties(
            amount=0,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume,
                    act_qty=10,
                    consume_sum=300,
                    act_money=300,
                ),
                hm.has_properties(
                    consume=consume,
                    act_qty=-10,
                    consume_sum=-300,
                    act_money=-300,
                )
            )
        )
    )
    hm.assert_that(
        consume,
        hm.has_properties(
            act_money=300,
            act_qty=10,
            act_sum=300,
        )
    )


def test_several_eq_rows(session, client):
    order1 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
    order2 = create_order(session, client, product=DIRECT_PRODUCT_ID)
    invoice = create_invoice(session, client, [order1], qty=900)
    consume1, = invoice.consumes
    consume2 = create_consume(invoice, order2, qty=30)

    act_eid1 = gen_act_eid(session)
    act_eid2 = gen_act_eid(session)
    create_act_row(act_eid1, YESTERDAY, consume1, 300, 300)
    create_act_row(act_eid1, YESTERDAY, consume1, 300, 300)
    create_act_row(act_eid1, YESTERDAY, consume1, decimal.Decimal('300.0001'), 300)
    create_act_row(act_eid2, YESTERDAY, consume2, 300, 300)
    create_act_row(act_eid2, YESTERDAY, consume2, 300, 300)
    create_act_row(act_eid2, YESTERDAY, consume2, 300, 300)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act1, act2 = sorted(invoice.acts, key=lambda a: a.external_id)

    hm.assert_that(
        act1,
        hm.has_properties(
            amount=900,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid1,
            dt=YESTERDAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=decimal.Decimal('900.0001'),
                    act_sum=900
                )
            )
        )
    )
    hm.assert_that(
        act2,
        hm.has_properties(
            amount=900,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid2,
            dt=YESTERDAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume2,
                    act_qty=30,
                    act_sum=900
                )
            )
        )
    )

    hm.assert_that(consume1.act_money, hm.is_(None))
    hm.assert_that(consume2.act_money, hm.equal_to(900))


def test_several_rows_eq_product_diff_order(session, client):
    order1 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
    order2 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
    order3 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
    order4 = create_order(session, client, product=DIRECT_MEDIA_PRODUCT_RUB_ID)
    invoice = create_invoice(session, client, [order1], qty=1200)
    consume1, = invoice.consumes

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, YESTERDAY, consume1, 300, 300)
    create_act_row(act_eid, YESTERDAY, consume1, 300, 300, service_order_id=order2.service_order_id)
    create_act_row(act_eid, YESTERDAY, consume1, 300, 300, service_order_id=order3.service_order_id)
    create_act_row(act_eid, YESTERDAY, consume1, 300, 300, service_order_id=order4.service_order_id)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act, = sorted(invoice.acts, key=lambda a: a.external_id)

    hm.assert_that(
        act,
        hm.has_properties(
            amount=1200,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid,
            dt=YESTERDAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=300,
                    act_sum=300,
                    product_id=None,
                ),
                hm.has_properties(
                    consume=consume1,
                    act_qty=600,
                    act_sum=600,
                    product_id=DIRECT_PRODUCT_RUB_ID,
                ),
                hm.has_properties(
                    consume=consume1,
                    act_qty=300,
                    act_sum=300,
                    product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                )
            )
        )
    )


def test_bucks_acted_qty_precision(session, client):
    order1 = create_order(session, client)
    invoice = create_invoice(session, client, [order1], qty=3)
    consume1, = invoice.consumes

    act_eid1 = gen_act_eid(session)
    act_eid2 = gen_act_eid(session)
    act_eid3 = gen_act_eid(session)
    create_act_row(act_eid1, YESTERDAY, consume1, 1, 1)
    create_act_row(act_eid2, TODAY, consume1, 1, 1)
    create_act_row(act_eid3, TOMORROW, consume1, 1, 1)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act1, act2, act3 = sorted(invoice.acts, key=lambda a: a.dt)

    hm.assert_that(
        act1,
        hm.has_properties(
            amount=1,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid1,
            dt=YESTERDAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=D('0.033333'),
                    act_sum=1
                )
            )
        )
    )
    hm.assert_that(
        act2,
        hm.has_properties(
            amount=1,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid2,
            dt=TODAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=D('0.033334'),
                    act_sum=1
                )
            )
        )
    )
    hm.assert_that(
        act3,
        hm.has_properties(
            amount=1,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid3,
            dt=TOMORROW,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=D('0.033333'),
                    act_sum=1
                )
            )
        )
    )
    hm.assert_that(consume1.act_money, hm.equal_to(3))


def test_bucks_init(session, client):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=666)
    consume, = invoice.consumes

    calculate_consumption(order, TODAY, 345)
    generate_act(invoice, TODAY)

    act_eid1 = gen_act_eid(session)
    act_eid2 = gen_act_eid(session)
    create_act_row(act_eid1, TOMORROW, consume, 1, 1)
    create_act_row(act_eid2, FUTURE, consume, 1, 1)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act1, act2, act3 = sorted(invoice.acts, key=lambda a: a.dt)

    hm.assert_that(
        act1,
        hm.has_properties(
            amount=345 * 30,
            tax_policy_pct=consume.tax_policy_pct,
            dt=TODAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume,
                    act_qty=345,
                    act_sum=345 * 30
                )
            )
        )
    )
    hm.assert_that(
        act2,
        hm.has_properties(
            amount=1,
            tax_policy_pct=consume.tax_policy_pct,
            external_id=act_eid1,
            dt=TOMORROW,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume,
                    act_qty=D('0.033333'),
                    act_sum=1
                )
            )
        )
    )
    hm.assert_that(
        act3,
        hm.has_properties(
            amount=1,
            tax_policy_pct=consume.tax_policy_pct,
            external_id=act_eid2,
            dt=FUTURE,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume,
                    act_qty=D('0.033334'),
                    act_sum=1
                )
            )
        )
    )
    hm.assert_that(
        consume,
        hm.has_properties(
            act_money=10352,
            act_qty=D('345.066667'),
            act_sum=10352,
        )
    )


def test_bucks_overacted_split(session, client):
    order = create_order(session, client)
    alt_order_1 = create_order(session, client, DIRECT_MEDIA_PRODUCT_RUB_ID)
    alt_order_2 = create_order(session, client, DIRECT_PRODUCT_RUB_ID)
    invoice = create_invoice(session, client, [order], qty=666)
    consume, = invoice.consumes

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, TOMORROW, consume, -10, -10)
    create_act_row(act_eid, TOMORROW, consume, 3, 3, service_order_id=alt_order_1.service_order_id)
    create_act_row(act_eid, TOMORROW, consume, 8, 8, service_order_id=alt_order_2.service_order_id)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act, = sorted(invoice.acts, key=lambda a: a.dt)

    hm.assert_that(
        act,
        hm.has_properties(
            amount=1,
            rows=hm.only_contains(
                hm.has_properties(
                    consume=consume,
                    act_money=hm.is_not(None)
                )
            )
        )
    )
    assert all(at.act_qty < 0 or at.product_id for at in act.rows)
    assert sum(at.act_money for at in act.rows) == 1
    hm.assert_that(
        consume,
        hm.has_properties(
            act_money=1,
            act_qty=D('0.033333'),
            act_sum=1,
        )
    )


def test_bucks_zero_qty_nonzero_money(session, client):
    fish_order = create_order(session, client, product=DIRECT_PRODUCT_ID)
    money_order = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
    invoice = create_invoice(session, client, [fish_order], qty=3)
    fish_consume, = invoice.consumes
    money_consume = create_consume(invoice, money_order, 666)

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, YESTERDAY, money_consume, 100, 100)
    create_act_row(act_eid, YESTERDAY, fish_consume, D('0.00001'), 0)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act, = invoice.acts

    hm.assert_that(
        act,
        hm.has_properties(
            amount=100,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=money_consume,
                    act_qty=100,
                    act_sum=100
                ),
                hm.has_properties(
                    consume=fish_consume,
                    act_qty=0,
                    act_sum=0,
                    act_money=D('0.00001')
                )
            )
        )
    )
    hm.assert_that(
        fish_consume,
        hm.has_properties(
            act_money=D('0.00001'),
            act_qty=0,
            act_sum=0,
        )
    )


def test_no_acts(session, client):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=66)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )

    hm.assert_that(invoice.consumes[0].act_money, hm.is_(None))


def test_multiple_dates(session, client):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=66)
    consume, = invoice.consumes

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, YESTERDAY, consume, 300, 300)
    create_act_row(act_eid, TODAY, consume, 300, 300)

    with mock_transactions():
        export_obj = process_act_gen(invoice)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=hm.contains_string('Different act dates for'),
            state=ExportState.failed,
        )
    )

    hm.assert_that(consume.act_money, hm.is_(None))


def test_multiple_taxes(session, client):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=66)
    consume, = invoice.consumes

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, TODAY, consume, 300, 300, TAX_POLICY_PCT_ID_18_RUSSIA)
    create_act_row(act_eid, TODAY, consume, 300, 300)

    with mock_transactions():
        export_obj = process_act_gen(invoice)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=hm.contains_string('Different taxes for act'),
            state=ExportState.failed,
        )
    )

    hm.assert_that(consume.act_money, hm.is_(None))


def test_wrong_taxes(session, client):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=66)
    consume, = invoice.consumes

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, TODAY, consume, 300, 300, TAX_POLICY_PCT_ID_18_RUSSIA)

    with mock_transactions():
        export_obj = process_act_gen(invoice)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=hm.contains_string('Generated act with wrong tax for'),
            state=ExportState.failed,
        )
    )

    hm.assert_that(consume.act_money, hm.is_(None))


def test_unknown_consumes(session, client):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=66)
    consume, = invoice.consumes

    consume_id = session.execute('select bo.s_consume_id.nextval from dual').scalar()
    fake_consume = ut.Struct(
        session=consume.session,
        id=consume_id,
        invoice_id=invoice.id,
        tax_policy_pct_id=consume.tax_policy_pct_id
    )

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, TODAY, consume, 300, 300)
    create_act_row(act_eid, TODAY, fake_consume, 300, 300)

    with mock_transactions():
        export_obj = process_act_gen(invoice)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=hm.contains_string('There are some rows with unknown consumes'),
            state=ExportState.failed,
        )
    )

    hm.assert_that(consume.act_money, hm.is_(None))


def test_existing_act_same_invoice(session, client):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=66)
    consume, = invoice.consumes

    invoice.close_invoice(TODAY)
    act, = invoice.acts

    create_act_row(act.external_id, TODAY, consume, 1980, 1980)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    session.expire_all()

    assert invoice.acts == [act]

    hm.assert_that(consume.act_money, hm.is_(None))


@pytest.mark.parametrize(
    'add_params, hidden',
    [
        pytest.param({'act_dt': YESTERDAY}, False, id='dt'),
        pytest.param({'sum_': decimal.Decimal('1980.01')}, False, id='sum'),
        pytest.param({'tax_policy_pct_id': TAX_POLICY_PCT_ID_18_RUSSIA}, False, id='tpp'),
        pytest.param({}, True, id='hidden'),
    ]
)
def test_existing_act_same_invoice_invalid(session, client, add_params, hidden):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=66)
    consume, = invoice.consumes

    invoice.close_invoice(TODAY)
    act, = invoice.acts
    if hidden:
        act.hide()

    params = dict(
        act_dt=TODAY,
        consume=consume,
        qty=1980,
        sum_=1980,
        tax_policy_pct_id=None,
    )
    params.update(add_params)
    create_act_row(act.external_id, **params)

    with mock_transactions():
        export_obj = process_act_gen(invoice)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=hm.contains_string('There is act with same number but different parameters for'),
            state=ExportState.failed,
        )
    )

    hm.assert_that(consume.act_money, hm.is_(None))


def test_existing_act_other_invoice(session, client):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=666)
    consume, = invoice.consumes

    other_invoice = create_invoice(session, client, [order], qty=66)
    other_invoice.close_invoice(TODAY)
    act, = other_invoice.acts

    create_act_row(act.external_id, TODAY, consume, 1980, 1980)

    with mock_transactions():
        export_obj = process_act_gen(invoice)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=hm.contains_string('There is act with same number but different parameters for'),
            state=ExportState.failed,
        )
    )

    hm.assert_that(consume.act_money, hm.is_(None))


def test_existing_act_for_yinvoice(session, client):
    order = create_order(session, client, DIRECT_PRODUCT_RUB_ID)

    contract = ob.create_credit_contract(session, client)
    invoice = consume_credit(contract, [(order, 666)])
    consume, = invoice.consumes

    calculate_consumption(order, TODAY, 666)
    month = mapper.ActMonth(for_month=TODAY)
    act, = generate_act(invoice, month)

    create_act_row(act.external_id, month.document_dt, consume, 666, 666)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )

    hm.assert_that(
        act.invoice,
        hm.all_of(
            hm.instance_of(mapper.YInvoice),
            hm.has_properties(fictives=[invoice])
        )
    )


def test_existing_act_for_yinvoice_other_contract(session, client):
    order = create_order(session, client, DIRECT_PRODUCT_RUB_ID)

    contract = ob.create_credit_contract(session, client)
    other_contract = ob.create_credit_contract(session, client)

    invoice = consume_credit(contract, [(order, 666)])
    other_invoice = consume_credit(other_contract, [(order, 666)])

    other_consume, = other_invoice.consumes

    calculate_consumption(order, TODAY, 666)
    month = mapper.ActMonth(for_month=TODAY)

    act, = generate_act(invoice, month)

    create_act_row(act.external_id, month.document_dt, other_consume, 666, 666)

    export_obj = process_act_gen(other_invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=hm.contains_string('There is act with same number but different parameters for'),
            state=ExportState.failed,
        )
    )


def test_act_month(session, client, person):
    order = create_order(session, client)
    invoice = create_invoice(session, client, [order], qty=66)
    consume, = invoice.consumes

    act_eid = gen_act_eid(session)
    act_month = mapper.ActMonth()
    create_act_row(act_eid, act_month.document_dt, consume, 900, 900)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act, = invoice.acts

    hm.assert_that(
        act,
        hm.has_properties(
            amount=900,
            dt=act_month.document_dt,
            month_dt=act_month.for_month,
        )
    )


def test_multiple_acts_w_diffrent_products(session, agency, subclient):
    """Используем продукты с разными commission_type"""
    person = ob.PersonBuilder.construct(session, client=agency, type='ur')

    order1 = create_order(session, subclient, agency=agency, product=DIRECT_PRODUCT_RUB_ID)
    alt_product = ob.ProductBuilder.construct(session, commission_type=666, media_discount=666)
    order2 = create_order(session, subclient, agency=agency, product=alt_product.id)

    contract = ob.create_credit_contract(
        session,
        agency,
        person,
        dt=datetime.datetime.now() - datetime.timedelta(days=66),
        client_limits={
            subclient.id: {'client_limit': 100500},
        }
    )
    invoice = consume_credit(contract, [(order1, 10), (order2, 20)])
    session.flush()

    consume1, consume2 = invoice.consumes

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, YESTERDAY, consume1, 600, 600)
    create_act_row(act_eid, YESTERDAY, consume2, 450, 450)

    session.expire_all()

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act = session.query(mapper.Act).filter_by(external_id=act_eid).one()
    y_invoice = act.invoice

    assert y_invoice.type == 'y_invoice'
    assert y_invoice.commission_type == 7
    assert y_invoice.discount_type == 7

    hm.assert_that(
        act,
        hm.has_properties(
            amount=1050,
            dt=YESTERDAY,
        )
    )
    hm.assert_that(consume1.act_money, hm.is_(None))
    hm.assert_that(consume2.act_money, hm.is_(None))


@pytest.mark.parametrize('force_use_mapper', [True, False])
def test_hide_acts(session, client, force_use_mapper):
    order1 = create_order(session, client)
    invoice = create_invoice(session, client, [order1], qty=3)
    consume1, = invoice.consumes

    act_eid1 = gen_act_eid(session)
    act_eid2 = gen_act_eid(session)
    create_act_row(act_eid1, YESTERDAY, consume1, 300, 300)
    create_act_row(act_eid1, YESTERDAY, consume1, 450, 450)
    create_act_row(act_eid2, TODAY, consume1, 150, 150)

    export_obj = process_act_gen(invoice)
    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act1, act2 = sorted(invoice.acts, key=lambda a: a.dt)

    hm.assert_that(consume1.act_money, hm.equal_to(900))

    act1.hide(force_use_mapper=force_use_mapper)

    hm.assert_that(act1.hidden, hm.equal_to(4))
    hm.assert_that(consume1.act_money, hm.equal_to(150))

    act1.unhide()

    hm.assert_that(
        act1,
        hm.has_properties(
            hidden=0,
            amount=750,
            tax_policy_pct=consume1.tax_policy_pct,
            external_id=act_eid1,
            dt=YESTERDAY,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=25,
                    act_sum=750
                )
            )
        )
    )

    hm.assert_that(consume1.act_money, hm.equal_to(900))


def test_product_id_from_tariff_act_row_to_act_trans(session, client):
    orders = [
        create_order(session, client)
        for _ in range(2)
    ]
    invoice = create_invoice(session, client, orders)
    co1, co2 = invoice.consumes
    add_order = create_order(session, client, DIRECT_MEDIA_PRODUCT_RUB_ID)

    act_eid1 = gen_act_eid(session)
    create_act_row(act_eid1, YESTERDAY, co1, 10, 10, service_order_id=add_order.service_order_id)
    create_act_row(act_eid1, YESTERDAY, co2, 10, 10, service_order_id=None)

    process_act_gen(invoice)

    products = sorted([row.product_id for row in invoice.acts[0].rows])
    assert products == [None, add_order.service_code]


def test_log_type_filter(session, client):
    order = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
    invoice = create_invoice(session, client, [order])
    co, = invoice.consumes

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, YESTERDAY, co, 1, 1, log_type_id=1)
    create_act_row(act_eid, YESTERDAY, co, 2, 2, log_type_id=2)

    export_obj = process_act_gen(invoice, log_type_id=2)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act, = sorted(invoice.acts, key=lambda a: a.dt)

    hm.assert_that(
        act,
        hm.has_properties(
            amount=2,
            external_id=act_eid,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=co,
                    act_qty=2,
                    act_sum=2,
                ),
            )
        )
    )


class TestEndbuyers(object):
    def _mk_contract(self, client):
        session = client.session

        person = ob.PersonBuilder.construct(session, client=client, type='ur')
        return ob.ContractBuilder.construct(
            session,
            client=client,
            person=person,
            commission=0,
            firm=1,
            postpay=1,
            personal_account=1,
            personal_account_fictive=1,
            payment_type=3,
            payment_term=30,
            credit=3,
            credit_limit_single='9' * 20,
            services={7, 11, 35},
            is_signed=datetime.datetime.now()
        )

    def test_subclient(self, session):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        subclient = ob.ClientBuilder.construct(session, agency=agency)
        contract = self._mk_contract(agency)
        endbuyer = ob.PersonBuilder(
            client=agency,
            type='endbuyer_ur',
            name='Конченый покупатель'
        ).build(session).obj
        budget = mapper.EndbuyerBudget(
            endbuyer=endbuyer,
            contract=contract,
            period_dt=ut.month_first_day(TODAY),
            sum=None
        )
        budget_subclient = mapper.EndbuyerSubclient(
            budget=budget,
            agency=agency,
            client=subclient,
            priority=666
        )
        session.add(budget)
        session.add(budget_subclient)
        session.flush()

        product = ob.ProductBuilder.construct(session, price=1)
        order = ob.OrderBuilder.construct(session, agency=agency, client=subclient, product=product)
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=300)]
            )
        ).build(session).obj

        invoice, = core.Core(session).pay_on_credit(
            request_id=request.id,
            paysys_id=1003,
            person_id=contract.person_id,
            contract_id=contract.id
        )

        q, = invoice.consumes
        create_act_row(gen_act_eid(session), ut.month_last_day(TODAY), q, 300, 300)

        process_act_gen(invoice)

        yinvoice, = invoice.repayments
        assert yinvoice.endbuyer_id == endbuyer.id

    def test_order(self, session):
        client = ob.ClientBuilder.construct(session)
        product = ob.ProductBuilder.construct(session, price=1)
        order = ob.OrderBuilder.construct(session, client=client, product=product)

        contract = self._mk_contract(client)

        endbuyer = ob.PersonBuilder(
            client=client,
            type='endbuyer_ur',
            name='Конченый покупатель'
        ).build(session).obj
        budget = mapper.EndbuyerBudget(
            endbuyer=endbuyer,
            contract=contract,
            period_dt=ut.month_first_day(TODAY),
            sum=None
        )
        budget_order = mapper.EndbuyerOrder(
            budget=budget,
            order=order,
            priority=666
        )
        session.add(budget)
        session.add(budget_order)
        session.flush()

        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=300)]
            )
        ).build(session).obj

        invoice, = core.Core(session).pay_on_credit(
            request_id=request.id,
            paysys_id=1003,
            person_id=contract.person_id,
            contract_id=contract.id
        )

        q, = invoice.consumes
        create_act_row(gen_act_eid(session), ut.month_last_day(TODAY), q, 300, 300)

        process_act_gen(invoice)

        yinvoice, = invoice.repayments
        assert yinvoice.endbuyer_id == endbuyer.id

    def test_wo_endbuyers(self, session):
        client = ob.ClientBuilder.construct(session)
        contract = self._mk_contract(client)

        product = ob.ProductBuilder.construct(session, price=1)
        order = ob.OrderBuilder.construct(session, client=client, product=product)
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=300)]
            )
        ).build(session).obj

        invoice, = core.Core(session).pay_on_credit(
            request_id=request.id,
            paysys_id=1003,
            person_id=contract.person_id,
            contract_id=contract.id
        )

        q, = invoice.consumes
        create_act_row(gen_act_eid(session), ut.month_last_day(TODAY), q, 300, 300)

        export_obj = process_act_gen(invoice)

        hm.assert_that(
            export_obj,
            hm.has_properties(
                error=None,
                state=ExportState.exported,
            )
        )

        yinvoice, = invoice.repayments
        assert yinvoice.total_act_sum == 300

    def test_multiple_budgets(self, session):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        subclient = ob.ClientBuilder.construct(session, agency=agency)
        contract = self._mk_contract(agency)
        endbuyer = ob.PersonBuilder(
            client=agency,
            type='endbuyer_ur',
            name='Конченый покупатель'
        ).build(session).obj
        budget1 = mapper.EndbuyerBudget(
            endbuyer=endbuyer,
            contract=contract,
            period_dt=ut.month_first_day(TODAY),
            sum=None
        )
        budget2 = mapper.EndbuyerBudget(
            endbuyer=endbuyer,
            contract=contract,
            period_dt=ut.month_first_day(TODAY),
            sum=None
        )
        budget_subclient1 = mapper.EndbuyerSubclient(
            budget=budget1,
            agency=agency,
            client=subclient,
            priority=666
        )
        budget_subclient2 = mapper.EndbuyerSubclient(
            budget=budget2,
            agency=agency,
            client=subclient,
            priority=666
        )
        session.add(budget1)
        session.add(budget2)
        session.add(budget_subclient1)
        session.add(budget_subclient2)
        session.flush()

        product = ob.ProductBuilder.construct(session, price=1)
        order = ob.OrderBuilder.construct(session, agency=agency, client=subclient, product=product)
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=300)]
            )
        ).build(session).obj

        invoice, = core.Core(session).pay_on_credit(
            request_id=request.id,
            paysys_id=1003,
            person_id=contract.person_id,
            contract_id=contract.id
        )

        q, = invoice.consumes
        create_act_row(gen_act_eid(session), ut.month_last_day(TODAY), q, 300, 300)

        export_obj = process_act_gen(invoice)
        hm.assert_that(
            export_obj,
            hm.has_properties(state=ExportState.failed, error=hm.contains_string(u'Multiple endbuyer budgets found'))
        )


def test_only_taxes_act(session, client):
    tax_policy = ob.TaxPolicyBuilder.construct(session, tax_pcts=[20])
    product = ob.ProductBuilder.construct(session, taxes=[tax_policy], prices=[(YESTERDAY, 'RUR', 1)])

    order = create_order(session, client, product=product.id)
    invoice = create_invoice(session, client, [order], qty=100)
    co, = invoice.consumes

    calculate_consumption(order, TODAY, D('99.9706'))
    generate_act(invoice, TODAY)

    act_eid = gen_act_eid(session)
    create_act_row(act_eid, TOMORROW, co, D('0.0018'), D('0.01'))

    export_obj = process_act_gen(invoice)

    hm.assert_that(
        export_obj,
        hm.has_properties(
            error=None,
            state=ExportState.exported,
        )
    )
    act_prev, act = sorted(invoice.acts, key=lambda a: a.dt)

    hm.assert_that(
        act,
        hm.has_properties(
            amount=D('0.01'),
            amount_nds=D('0.01'),
            type='internal',
        )
    )


class TestHideActByLogTariffActRows(object):
    ticket_id = 'PAYSUP-666'

    @pytest.mark.parametrize(
        'hidden, ticket_id',
        [
            pytest.param(1, '', id='hidden+empty_ticket_id'),
            pytest.param(1, None, id='hidden+null_ticket_id'),
            pytest.param(0, ticket_id, id='not_hidden+ticket_id'),
            pytest.param(None, ticket_id, id='none_hidden+ticket_id')
        ]
    )
    def test_ticket_id_hidden_mismatch(self, session, client, hidden, ticket_id):
        """
        Есть hidden - нет ticket_id. И наоборот.
        Падаем
        """
        order = create_order(session, client)
        invoice = create_invoice(session, client, [order], qty=66)
        consume, = invoice.consumes

        act_eid = gen_act_eid(session)
        create_act_row(act_eid, TODAY, consume, 300, 300, hidden=hidden, ticket_id=ticket_id)

        export_obj = process_act_gen(invoice)
        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.failed,
                error=hm.contains_string(u'Ticket_id and hidden are mismatch'))
        )

    def test_no_act(self, session, client):
        """
        Удаляется не существующий акт.
        Падаем
        """
        order = create_order(session, client)
        invoice = create_invoice(session, client, [order], qty=66)
        consume, = invoice.consumes

        act_eid = gen_act_eid(session)
        create_act_row(act_eid, TODAY, consume, -666, -666, hidden=1, ticket_id=self.ticket_id)

        export_obj = process_act_gen(invoice)

        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.failed,
                error=hm.matches_regexp(r'Act with eid Y-\d+ not found in DB'))
        )

    def test_act_already_hidden(self, session, client):
        """
        Пытаемся удалить уже удаленный в оракле акт.
        """
        order = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, client, [order], qty=300)
        consume, = invoice.consumes

        calculate_consumption(order, TODAY, 66)
        generate_act(invoice, TODAY)

        act, = invoice.acts
        act.hide('PAYSUP-777')

        create_act_row(act.external_id, TODAY, consume, -66, -66, hidden=1, ticket_id=self.ticket_id)

        export_obj = process_act_gen(invoice)
        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.exported,
                error=None)
        )

    @pytest.mark.parametrize(
        'product_id, act_sum, act_qty',
        [
            pytest.param(DIRECT_PRODUCT_RUB_ID, 100, 100,  id='DIRECT_PRODUCT_RUB_ID'),
            pytest.param(DIRECT_PRODUCT_ID, 300, 10,  id='DIRECT_PRODUCT_ID'),
        ]
    )
    def test_act_amount_mismatch(self, session, client, product_id, act_sum, act_qty):
        """
        Удаляется не удаленный акт, но не сходится сумма acted_sum по строкам.
        Падаем
        """
        order = create_order(session, client, product=product_id)
        invoice = create_invoice(session, client, [order], qty=300)
        consume, = invoice.consumes

        act_eid = gen_act_eid(session)
        create_act_row(act_eid, TODAY, consume, act_qty, act_sum)
        process_act_gen(invoice)
        session.execute("delete from bo.t_log_tariff_act_row where act_eid = '%s'" % act_eid)

        act, = invoice.acts

        create_act_row(act.external_id, TODAY, consume, -act_qty - 1, -act_sum - 1, hidden=1, ticket_id=self.ticket_id)

        export_obj = process_act_gen(invoice)
        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.failed,
                error=hm.contains_string(u'different parameters')
            )
        )

    def test_not_allowed_hidden_value(self, session, client):
        """
        У log_tariff_act_row недопустимый hidden.
        Падаем
        """
        order = create_order(session, client,)
        invoice = create_invoice(session, client, [order], qty=300)
        consume, = invoice.consumes

        act_eid = gen_act_eid(session)
        create_act_row(act_eid, TODAY, consume, 10, 10, hidden=3)

        export_obj = process_act_gen(invoice)
        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.failed,
                error=hm.equal_to(u'Not allowed hidden value for act %s' % act_eid))
        )

    def test_multiple_ticket_id(self, session, client):
        """
        По строкам с одним номером акта несколько разных ticket_id.
        Падаем
        """
        order1 = create_order(session, client)
        invoice = create_invoice(session, client, [order1], qty=66)
        consume1, = invoice.consumes
        consume2 = create_consume(invoice, order1, qty=66)

        act_eid = gen_act_eid(session)
        create_act_row(act_eid, TODAY, consume1, -10, -10, hidden=1, ticket_id=self.ticket_id)
        create_act_row(act_eid, TODAY, consume2, -20, -20, hidden=1, ticket_id='PAYSUP-667')

        export_obj = process_act_gen(invoice)
        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.failed,
                error=hm.contains_string(u'Different ticket_ids'))
        )

    def test_multiple_hidden_value(self, session, client):
        """
        По строкам с одним номером акта несколько разных значений hidden.
        Падаем
        """
        order1 = create_order(session, client)
        invoice = create_invoice(session, client, [order1], qty=66)
        consume1, = invoice.consumes
        consume2 = create_consume(invoice, order1, qty=66)

        act_eid = gen_act_eid(session)
        create_act_row(act_eid, TODAY, consume1, -10, -10, hidden=0, ticket_id=None)
        create_act_row(act_eid, TODAY, consume2, -20, -20, hidden=1, ticket_id='PAYSUP-667')

        export_obj = process_act_gen(invoice)
        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.failed,
                error=hm.contains_string(u'Different hidden values'))
        )

    @pytest.mark.parametrize(
        'product_id, act_sum, act_qty',
        [
            pytest.param(DIRECT_PRODUCT_RUB_ID, 36.6, 36.6, id='DIRECT_PRODUCT_RUB_ID'),
            pytest.param(DIRECT_PRODUCT_ID, 0.04, 1.252, id='DIRECT_PRODUCT_ID'),
        ]
    )
    def test_hide(self, session, client, product_id, act_sum, act_qty):
        """
        Самый обычный случай. Удаляется когда-то сгенеренный акт.
        Успех
        """
        order = create_order(session, client, product=product_id)
        invoice = create_invoice(session, client, [order], qty=300)
        consume, = invoice.consumes

        act_eid = gen_act_eid(session)
        create_act_row(act_eid, TODAY, consume, act_qty, act_sum)

        process_act_gen(invoice)
        act, = invoice.acts
        session.execute("delete from bo.t_log_tariff_act_row where act_eid = '%s'" % act_eid)

        create_act_row(act.external_id, TODAY, consume, -act_qty, -act_sum, hidden=1, ticket_id=self.ticket_id)

        export_obj = process_act_gen(invoice)
        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.exported,
                error=None
            )
        )
        hm.assert_that(
            act,
            hm.has_properties(
                hidden=4,
                jira_id=self.ticket_id
            )
        )

    def test_hide_in_closed_month(self, session, client):
        """
        Акт в закрытом месяце
        """
        order = create_order(session, client)
        invoice = create_invoice(session, client, [order], qty=300)
        consume, = invoice.consumes

        act_eid = gen_act_eid(session)
        create_act_row(act_eid, TODAY, consume, 10, 10)

        process_act_gen(invoice)
        act, = invoice.acts
        session.execute("delete from bo.t_log_tariff_act_row where act_eid = '%s'" % act_eid)

        create_act_row(act.external_id, TODAY, consume, -10, -10, hidden=1, ticket_id=self.ticket_id)

        with mock.patch(
            'balance.processors.log_tariff_act.can_close_invoice',
            return_value=False,
        ):
            export_obj = process_act_gen(invoice)

        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.enqueued,
                error='Closed month'
            )
        )

    @pytest.mark.parametrize(
        'product_id, act_sum, act_qty',
        [
            pytest.param(DIRECT_PRODUCT_RUB_ID, 100, 100, id='DIRECT_PRODUCT_RUB_ID'),
            pytest.param(DIRECT_PRODUCT_ID, 300, 10, id='DIRECT_PRODUCT_ID'),
        ]
    )
    def test_hide_with_positive_rows(self, session, client, product_id, act_qty, act_sum):
        """
        В актах есть и плюсы, и минусы
        """
        order1 = create_order(session, client, product=product_id)
        order2 = create_order(session, client, product=product_id)
        invoice = create_invoice(session, client, [order1, order2], qty=300)
        consume1, consume2 = invoice.consumes

        act_eid = gen_act_eid(session)
        create_act_row(act_eid, TODAY, consume1, act_qty, act_sum)
        create_act_row(act_eid, TODAY, consume2, -1, -1)

        process_act_gen(invoice)
        act, = invoice.acts
        session.execute("delete from bo.t_log_tariff_act_row where act_eid = '%s'" % act_eid)

        create_act_row(act.external_id, TODAY, consume1, -act_qty, -act_sum, hidden=1, ticket_id=self.ticket_id)
        create_act_row(act.external_id, TODAY, consume2, 1, 1, hidden=1, ticket_id=self.ticket_id)

        export_obj = process_act_gen(invoice)
        hm.assert_that(
            export_obj,
            hm.has_properties(
                state=ExportState.exported,
                error=None
            )
        )
        hm.assert_that(
            act,
            hm.has_properties(
                hidden=4,
                jira_id=self.ticket_id
            )
        )

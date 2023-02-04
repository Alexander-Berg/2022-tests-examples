# -*- coding: utf-8 -*-

import datetime
import decimal

import pytest
import hamcrest as hm

from balance import muzzle_util as ut
from balance import mapper
from balance import exc
from balance.actions import consumption as a_c
from balance.actions.act_create import ActFactory
from balance.actions.acts.row import (
    ActRow,
)
from balance.constants import (
    TAX_POLICY_PCT_ID_18_RUSSIA,
    TAX_POLICY_PCT_ID_20_RUSSIA,
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_MEDIA_PRODUCT_RUB_ID,
    DIRECT_VIDEO_PRODUCT_RUB_ID,
    DIRECT_PRODUCT_ID,
)

from tests import object_builder as ob
from tests.base_routine import consumes_match

from tests.balance_tests.act.generation.common import (
    create_order,
    create_invoice,
    create_consume,
    calculate_consumption,
    generate_act,
    consume_credit,
)

D = decimal.Decimal

TODAY = ut.trunc_date(datetime.datetime.now())
YESTERDAY = TODAY - datetime.timedelta(1)


def assert_consumes(invoice, states):
    hm.assert_that(
        invoice.consumes,
        consumes_match(states, forced_params=['id', 'act_qty', 'act_sum'])
    )


def test_base(session, client):
    order1 = create_order(session, client)
    order2 = create_order(session, client)
    invoice = create_invoice(session, client, [order1], qty=66)
    consume1, = invoice.consumes
    consume2 = create_consume(invoice, order2, qty=66)

    act = ActFactory.create_from_external(
        invoice,
        [
            ActRow(consume1, 30, 900),
            ActRow(consume2, 36, 1080)
        ],
        YESTERDAY,
    )
    session.flush()

    hm.assert_that(
        act,
        hm.has_properties(
            amount=1980,
            amount_nds=330,
            tax_policy_pct=consume1.tax_policy_pct,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=30,
                    amount=900,
                    amount_nds=150,
                    netting=None,
                ),
                hm.has_properties(
                    consume=consume2,
                    act_qty=36,
                    amount=1080,
                    amount_nds=180,
                    netting=None,
                )
            )
        )
    )
    assert_consumes(
        invoice,
        [
            (consume1.id, 30, 900),
            (consume2.id, 36, 1080),
        ]
    )


def test_overact_compensation(session, client):
    order1 = create_order(session, client)
    order2 = create_order(session, client)
    invoice = create_invoice(session, client, [order1], qty=66)
    consume1, = invoice.consumes
    invoice.close_invoice(TODAY)
    session.flush()

    consume2 = create_consume(invoice, order2, qty=66)

    act = ActFactory.create_from_external(
        invoice,
        [
            ActRow(consume1, -23, -690),
            ActRow(consume2, 66, 1980)
        ],
        YESTERDAY,
    )

    hm.assert_that(
        act,
        hm.has_properties(
            amount=1290,
            amount_nds=215,
            tax_policy_pct=consume1.tax_policy_pct,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    act_qty=-23,
                    amount=-690,
                    amount_nds=-115,
                    netting=1,
                ),
                hm.has_properties(
                    consume=consume2,
                    act_qty=23,
                    amount=690,
                    amount_nds=115,
                    netting=1,
                ),
                hm.has_properties(
                    consume=consume2,
                    act_qty=43,
                    amount=1290,
                    amount_nds=215,
                    netting=None,
                )
            )
        )
    )
    assert_consumes(
        invoice,
        [
            (consume1.id, 43, 1290),
            (consume2.id, 66, 1980),
        ]
    )


@pytest.mark.parametrize(
    'with_prev_act',
    [
        pytest.param(True, id='with_prev_act'),
        pytest.param(False, id='wo_prev_act'),
    ]
)
def test_taxes_adjust(session, client, with_prev_act):
    order = create_order(session, client, DIRECT_PRODUCT_RUB_ID)
    invoice = create_invoice(session, client, [order], qty=1000)
    consume, = invoice.consumes

    if with_prev_act:
        calculate_consumption(order, TODAY, 200)
        generate_act(invoice, TODAY)

    act = ActFactory.create_from_external(invoice, [ActRow(consume, 200, 200)], TODAY)

    hm.assert_that(
        act,
        hm.has_properties(
            amount=200,
            amount_nds=D('33.34') if with_prev_act else D('33.33'),
            tax_policy_pct=consume.tax_policy_pct,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume,
                    act_qty=200,
                    amount=200,
                    amount_nds=D('33.34') if with_prev_act else D('33.33'),
                ),
            )
        )
    )
    if with_prev_act:
        assert_consumes(invoice, [(consume.id, 400, 400)])
    else:
        assert_consumes(invoice, [(consume.id, 200, 200)])


def test_y_invoice(session, credit_contract, subclient):
    order = create_order(session, subclient, DIRECT_PRODUCT_RUB_ID, credit_contract.client)
    pa = consume_credit(credit_contract, [(order, 1000)])
    consume, = pa.consumes

    act = ActFactory.create_from_external(pa, [ActRow(consume, 200, 200)], TODAY)

    hm.assert_that(
        act,
        hm.has_properties(
            amount=200,
            amount_nds=D('33.33'),
            tax_policy_pct=consume.tax_policy_pct,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume,
                    act_qty=200,
                    amount=200,
                    amount_nds=D('33.33'),
                ),
            ),
            invoice=hm.all_of(
                hm.instance_of(mapper.YInvoice),
                hm.has_properties(
                    total_sum=200,
                    fictives=[pa],
                )
            )
        )
    )
    assert_consumes(pa, [(consume.id, 200, 200)])


@pytest.mark.taxes_update
def test_multiple_taxes(session, client, person):
    order1 = create_order(session, client, DIRECT_PRODUCT_RUB_ID)
    order2 = create_order(session, client, DIRECT_PRODUCT_RUB_ID)
    invoice = create_invoice(session, client, [order1], qty=66)
    consume1, = invoice.consumes
    consume1.tax_policy_pct_id = TAX_POLICY_PCT_ID_18_RUSSIA
    consume2 = create_consume(invoice, order2, qty=66)
    session.expire_all()

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, 30, 900),
                ActRow(consume2, 36, 1080)
            ],
            YESTERDAY,
        )
    assert 'Ambiguous act taxes' in exc_info.value.msg


@pytest.mark.taxes_update
@pytest.mark.parametrize(
    'tpp_id, total_sum, nds_sum',
    [
        (TAX_POLICY_PCT_ID_20_RUSSIA, 120, 20),
        (TAX_POLICY_PCT_ID_18_RUSSIA, 118, 18),
    ]
)
def test_overact_taxes_compensation(session, client, person, tpp_id, total_sum, nds_sum):
    order1 = create_order(session, client, DIRECT_PRODUCT_RUB_ID)
    order2 = create_order(session, client, DIRECT_PRODUCT_RUB_ID)
    invoice = create_invoice(session, client, [order1], qty=666)
    consume1, = invoice.consumes
    invoice.close_invoice(TODAY)
    session.flush()

    consume2 = create_consume(invoice, order2, qty=666)
    consume1.tax_policy_pct_id = TAX_POLICY_PCT_ID_18_RUSSIA
    consume2.tax_policy_pct_id = tpp_id
    session.flush()
    session.expire_all()

    act = ActFactory.create_from_external(
        invoice,
        [
            ActRow(consume1, -total_sum, -total_sum),
            ActRow(consume2, total_sum * 2, total_sum * 2)
        ],
        YESTERDAY,
    )

    hm.assert_that(
        act,
        hm.has_properties(
            amount=total_sum,
            amount_nds=nds_sum,
            tax_policy_pct=consume2.tax_policy_pct,
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    consume=consume1,
                    amount=-total_sum,
                    amount_nds=-nds_sum,
                    netting=1,
                ),
                hm.has_properties(
                    consume=consume2,
                    amount=total_sum,
                    amount_nds=nds_sum,
                    netting=1,
                ),
                hm.has_properties(
                    consume=consume2,
                    amount=total_sum,
                    amount_nds=nds_sum,
                    netting=None,
                )
            )
        )
    )


def test_commission_type_from_product(session, client):
    """
    Проверяем, что тип комиссии берется из продукта в строке акта, если указан, иначе - из продукта заказа
    """
    order1 = create_order(session, client, product=DIRECT_PRODUCT_ID)
    order2 = create_order(session, client, product=DIRECT_PRODUCT_ID)
    invoice = create_invoice(session, client, [order1], qty=66)
    consume1, = invoice.consumes
    consume2 = create_consume(invoice, order2, qty=66)
    rows = [
        ActRow(consume1, 30, 900, product_id=None),
        ActRow(consume2, 36, 1080, product_id=DIRECT_VIDEO_PRODUCT_RUB_ID)
    ]
    act = ActFactory.create_from_external(
        invoice, rows, YESTERDAY,
    )
    session.flush()

    hm.assert_that(
        act,
        hm.has_properties(
            rows=hm.contains_inanyorder(
                hm.has_properties(
                    commission_type=45,
                    product_id=DIRECT_VIDEO_PRODUCT_RUB_ID
                ),
                hm.has_properties(
                    commission_type=7,
                    product_id=None
                ),
            )
        )
    )


class TestZeroSumNegQty(object):
    def test_total_neg(self, session, client):
        order1, order2, order3, order4 = [
            create_order(session, client)
            for _ in range(4)
        ]
        invoice = create_invoice(session, client, [order1, order2], qty=66)
        consume1, consume2 = invoice.consumes
        invoice.close_invoice(TODAY)
        session.flush()

        consume3 = create_consume(invoice, order3, qty=66)
        consume4 = create_consume(invoice, order4, qty=66)

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, -D('0.002'), 0),
                ActRow(consume2, -D('0.002'), 0),
                ActRow(consume3, D('0.001'), 0),
                ActRow(consume4, D('0.001'), 0),
            ],
            YESTERDAY,
        )

        hm.assert_that(
            act,
            hm.has_properties(
                amount=0,
                rows=hm.only_contains(
                    hm.has_properties(
                        netting=hm.is_not(None),
                    ),
                )
            )
        )
        assert sum(at.act_qty for at in act.rows) < 0

    def test_total_zero(self, session, client):
        order1, order2, order3 = [
            create_order(session, client)
            for _ in range(3)
        ]
        invoice = create_invoice(session, client, [order1, order2], qty=66)
        consume1, consume2 = invoice.consumes
        invoice.close_invoice(TODAY)
        session.flush()

        consume3 = create_consume(invoice, order3, qty=66)

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, -D('0.001'), 0),
                ActRow(consume2, -D('0.001'), 0),
                ActRow(consume3, D('0.002'), 0),
            ],
            YESTERDAY,
        )

        hm.assert_that(
            act,
            hm.has_properties(
                amount=0,
                rows=hm.only_contains(
                    hm.has_properties(
                        netting=hm.is_not(None),
                    ),
                )
            )
        )
        assert sum(at.act_qty for at in act.rows) == 0

    def test_different_taxes(self, session, client):
        order1, order2 = [create_order(session, client, DIRECT_PRODUCT_RUB_ID) for _ in range(2)]
        invoice = create_invoice(session, client, [order1], qty=66)
        consume1, = invoice.consumes
        consume1.tax_policy_pct_id = TAX_POLICY_PCT_ID_18_RUSSIA
        consume2 = create_consume(invoice, order2, qty=66)
        session.expire_all()

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, D('0.01'), D('0.01')),
                ActRow(consume2, -D('0.01'), -D('0.01'))
            ],
            YESTERDAY,
            external_id='YB-%s' % ob.generate_int(10)
        )

        hm.assert_that(
            act,
            hm.has_properties(
                amount=0,
                rows=hm.only_contains(
                    hm.has_properties(
                        netting=hm.is_not(None),
                    ),
                )
            )
        )
        assert sum(at.act_qty for at in act.rows) == 0


class TestZeroQtyNonzeroSum(object):
    def test_pos(self, session, client):
        order1 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        order2 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, client, [order1], qty=666)
        consume1, = invoice.consumes
        consume2 = create_consume(invoice, order2, 666)

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, 0, 1, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume1, 10, 10, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume1, 7, 7, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume1, 667, 667, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume2, 666, 666, product_id=DIRECT_PRODUCT_RUB_ID)
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=10,
                        amount=11,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=7,
                        amount=7,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=666,
                        amount=666,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=667,
                        amount=667,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                    ),
                )
            )
        )

    def test_pos_other_product(self, session, client):
        order = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, client, [order], qty=666)
        consume, = invoice.consumes

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume, 0, 1, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume, 10, 9, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume,
                        act_qty=10,
                        amount=10,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                    ),
                )
            )
        )

    def test_neg(self, session, client):
        order1 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        order2 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, client, [order1], qty=666)
        consume1, = invoice.consumes
        consume2 = create_consume(invoice, order2, 666)

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, 666, 666, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume2, -12, -12, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume1, -11, -11, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume1, -10, -10, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume1, -7, -7, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume1, 0, -1, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        assert act.amount == 625
        assert sum(at.act_qty for at in act.rows) == 626
        assert not any(at.act_qty == 0 for at in act.rows)

    def test_neg_pos(self, session, client):
        order = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, client, [order], qty=666)
        consume, = invoice.consumes

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume, 0, -1, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume, -2, -1, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume, 4, 4, product_id=DIRECT_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume,
                        act_qty=-2,
                        amount=-2,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume,
                        act_qty=2,
                        amount=2,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume,
                        act_qty=2,
                        amount=2,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=None
                    ),
                )
            )
        )

    def test_only_zero_qty(self, session, client):
        order1 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        order2 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, client, [order1], qty=666)
        consume1, = invoice.consumes
        consume2 = create_consume(invoice, order2, 666)

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, 0, 1, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume2, 10, 10, product_id=DIRECT_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=D('0.0001'),
                        amount=1,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-D('0.0001'),
                        amount=0,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=10,
                        amount=10,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=None
                    ),
                )
            )
        )

    def test_only_zero_qty_split(self, session, client):
        order1 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        order2 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, client, [order1], qty=666)
        consume1, = invoice.consumes
        consume2 = create_consume(invoice, order2, 666)

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, 0, 1, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume2, 10, 10, product_id=DIRECT_PRODUCT_RUB_ID),
                ActRow(consume2, -4, -4, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=D('0.0001'),
                        amount=1,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-D('0.0001'),
                        amount=0,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=2
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=6,
                        amount=6,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=4,
                        amount=4,
                        product_id=DIRECT_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=-4,
                        amount=-4,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=1
                    ),
                )
            )
        )

    def test_bucks_nonzero_money(self, session, client):
        order = create_order(session, client, product=DIRECT_PRODUCT_ID)
        invoice = create_invoice(session, client, [order], qty=666)
        consume, = invoice.consumes

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume, 3, 90, act_money=90, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume, 0, D('0.01'), act_money=D('0.00001'), product_id=DIRECT_VIDEO_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume,
                        act_qty=3,
                        amount=D('90.01'),
                        act_money=D('90.00001'),
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                    ),
                )
            )
        )


class TestSplitOveract(object):
    @staticmethod
    def _force_new_consume(order):
        order.manager_code = ob.SingleManagerBuilder.construct(order.session).manager_code
        order.session.flush()

    def test_order_product(self, session, agency):
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        order1 = create_order(session, client1, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        order2 = create_order(session, client2, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        invoice = create_invoice(session, agency, [order1, order2], qty=666)

        self._force_new_consume(order1)
        create_consume(invoice, order1, 100)
        consume1, consume2 = order1.consumes
        consume3, = order2.consumes

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, 100, 100, product_id=DIRECT_VIDEO_PRODUCT_RUB_ID),
                ActRow(consume2, 100, 100, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume1, -10, -10, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume3, 100, 100, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-10,
                        amount=-10,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=10,
                        amount=10,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=100,
                        amount=100,
                        product_id=DIRECT_VIDEO_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=90,
                        amount=90,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume3,
                        act_qty=100,
                        amount=100,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=None
                    ),
                )
            )
        )

    def test_client_product(self, session, agency):
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        order1 = create_order(session, client1, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        order2 = create_order(session, client1, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        order3 = create_order(session, client2, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        invoice = create_invoice(session, agency, [order1, order2, order3], qty=666)

        consume1, = order1.consumes
        consume2, = order2.consumes
        consume3, = order3.consumes

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, 100, 100, product_id=DIRECT_VIDEO_PRODUCT_RUB_ID),
                ActRow(consume2, 100, 100, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume3, 100, 100, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume1, -10, -10, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-10,
                        amount=-10,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=10,
                        amount=10,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=100,
                        amount=100,
                        product_id=DIRECT_VIDEO_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=90,
                        amount=90,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume3,
                        act_qty=100,
                        amount=100,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=None
                    ),
                )
            )
        )

    def test_client_order(self, session, agency):
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        order1 = create_order(session, client1, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        order2 = create_order(session, client1, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        order3 = create_order(session, client2, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        invoice = create_invoice(session, agency, [order1, order2, order3], qty=666)

        consume1, = order1.consumes
        consume2, = order2.consumes
        consume3, = order3.consumes

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, 100, 100, product_id=DIRECT_VIDEO_PRODUCT_RUB_ID),
                ActRow(consume2, 100, 100, product_id=DIRECT_VIDEO_PRODUCT_RUB_ID),
                ActRow(consume1, -10, -10, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume3, 100, 100, product_id=DIRECT_VIDEO_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-10,
                        amount=-10,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=10,
                        amount=10,
                        product_id=DIRECT_VIDEO_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=90,
                        amount=90,
                        product_id=DIRECT_VIDEO_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=100,
                        amount=100,
                        product_id=DIRECT_VIDEO_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume3,
                        act_qty=100,
                        amount=100,
                        product_id=DIRECT_VIDEO_PRODUCT_RUB_ID,
                        netting=None
                    ),
                )
            )
        )

    def test_client(self, session, agency):
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        order1 = create_order(session, client1, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        order2 = create_order(session, client1, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        order3 = create_order(session, client2, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        invoice = create_invoice(session, agency, [order1, order2, order3], qty=666)

        consume1, = order1.consumes
        consume2, = order2.consumes
        consume3, = order3.consumes

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume1, -10, -10, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume2, 100, 100, product_id=DIRECT_VIDEO_PRODUCT_RUB_ID),
                ActRow(consume3, 100, 100, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-10,
                        amount=-10,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=10,
                        amount=10,
                        product_id=DIRECT_VIDEO_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=90,
                        amount=90,
                        product_id=DIRECT_VIDEO_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume3,
                        act_qty=100,
                        amount=100,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=None
                    ),
                )
            )
        )

    def test_product(self, session, agency):
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        order1 = create_order(session, client1, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        order2 = create_order(session, client2, product=DIRECT_PRODUCT_RUB_ID, agency=agency)
        invoice = create_invoice(session, agency, [order1, order2], qty=666)

        consume1, = order1.consumes
        consume2, = order2.consumes

        act = ActFactory.create_from_external(
            invoice,
            [
                ActRow(consume2, 100, 100, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume1, -10, -10, product_id=DIRECT_MEDIA_PRODUCT_RUB_ID),
                ActRow(consume2, 100, 100, product_id=DIRECT_VIDEO_PRODUCT_RUB_ID),
            ],
            YESTERDAY
        )
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-10,
                        amount=-10,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=10,
                        amount=10,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=1
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=90,
                        amount=90,
                        product_id=DIRECT_MEDIA_PRODUCT_RUB_ID,
                        netting=None
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=100,
                        amount=100,
                        product_id=DIRECT_VIDEO_PRODUCT_RUB_ID,
                        netting=None
                    ),
                )
            )
        )


class TestConsumeZeroSums(object):
    def test_currency(self, session, client):
        order1 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        order2 = create_order(session, client, product=DIRECT_PRODUCT_ID)
        invoice = create_invoice(session, client, [order1], qty=10)
        consume1, = invoice.consumes
        consume2 = create_consume(invoice, order2, 1)

        a_c.reverse_consume(consume1, None, D('9.9999'))

        rows = [
            ActRow(consume1, 10, 10),
            ActRow(consume1, -5, -5),
            ActRow(consume1, D('-4.9999'), -5),
            ActRow(consume2, 1, 30),
        ]
        act = ActFactory.create_from_external(invoice, rows, YESTERDAY)
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                act_sum=30,
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=5,
                        act_sum=5,
                        netting=hm.is_not(None),
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=5,
                        act_sum=5,
                        netting=hm.is_not(None),
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-5,
                        act_sum=-5,
                        netting=hm.is_not(None),
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-D('4.9999'),
                        act_sum=-5,
                        netting=hm.is_not(None),
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=1,
                        act_sum=30,
                        netting=hm.is_(None),
                    ),
                )
            )
        )

    def test_fish(self, session, client):
        order1 = create_order(session, client, product=DIRECT_PRODUCT_ID)
        order2 = create_order(session, client, product=DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, client, [order1], qty=10)
        consume1, = invoice.consumes
        consume2 = create_consume(invoice, order2, 1)

        a_c.reverse_consume(consume1, None, D('9.9999'))

        rows = [
            ActRow(consume1, 10, 300, act_money=300),
            ActRow(consume1, -5, -150, act_money=-150),
            ActRow(consume1, D('-4.9999'), -150, act_money=-D('149.997')),
            ActRow(consume2, 1, 1),
        ]
        act = ActFactory.create_from_external(invoice, rows, YESTERDAY)
        session.flush()

        hm.assert_that(
            act,
            hm.has_properties(
                act_sum=1,
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        consume=consume1,
                        act_qty=5,
                        act_sum=150,
                        act_money=150,
                        netting=hm.is_not(None),
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=5,
                        act_sum=150,
                        act_money=150,
                        netting=hm.is_not(None),
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-5,
                        act_sum=-150,
                        act_money=-150,
                        netting=hm.is_not(None),
                    ),
                    hm.has_properties(
                        consume=consume1,
                        act_qty=-D('4.9999'),
                        act_sum=-150,
                        act_money=-D('149.997'),
                        netting=hm.is_not(None),
                    ),
                    hm.has_properties(
                        consume=consume2,
                        act_qty=1,
                        act_sum=1,
                        netting=hm.is_(None),
                    ),
                )
            )
        )


class TestSeparatedDocuments(object):

    @staticmethod
    def _set_up_invoice_and_act_rows(session, agency_is_docs_separated,
                                     client_is_docs_separated):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)
        order1 = create_order(session, client1, product=DIRECT_PRODUCT_ID,
                              agency=client1.agency)
        order2 = create_order(session, client2, product=DIRECT_PRODUCT_ID,
                              agency=client1.agency)
        invoice = create_invoice(session, agency, [order1, order2], qty=10)
        consume1, consume2 = invoice.consumes

        # This should be done after invoice creation
        # so the creation itself does not raise
        agency.is_docs_separated = agency_is_docs_separated
        if client_is_docs_separated is not None:
            client1.agencies_printable_doc_types = {
                str(agency.id): (True, client_is_docs_separated)}

        act_rows = [
            ActRow(consume1, 10, 300, act_money=300),
            ActRow(consume2, 10, 300, act_money=300),
        ]

        return invoice, act_rows, client1, client2

    @pytest.mark.parametrize(
        'from_log_tariff',
        [
            pytest.param(True, id='from_log_tariff_true'),
            pytest.param(False, id='from_log_tariff_false'),
        ]
    )
    @pytest.mark.parametrize(
        'is_docs_separated',
        [
            pytest.param(True, id='is_docs_separated_true'),
            pytest.param(False, id='is_docs_separated_false'),
        ]
    )
    def test_does_not_raise_for_client_without_agency(self, session, from_log_tariff,
                                                      is_docs_separated):
        client = ob.ClientBuilder.construct(session)

        order1 = create_order(session, client, product=DIRECT_PRODUCT_ID)
        order2 = create_order(session, client, product=DIRECT_PRODUCT_ID)
        invoice = create_invoice(session, client, [order1, order2], qty=10)
        consume1, consume2 = invoice.consumes

        act_rows = [
            ActRow(consume1, 10, 300, act_money=300),
            ActRow(consume2, 10, 300, act_money=300),
        ]

        client.is_docs_separated = is_docs_separated

        try:
            ActFactory.create_from_external(invoice, act_rows, YESTERDAY,
                                            from_log_tariff=from_log_tariff)
        except exc.NONSEPARATED_DOCUMENTS:
            pytest.fail(
                "Should not raise NONSEPARATED_DOCUMENTS for for single subclient")

    @pytest.mark.parametrize(
        'from_log_tariff',
        [
            pytest.param(True, id='from_log_tariff_true'),
            pytest.param(False, id='from_log_tariff_false'),
        ]
    )
    @pytest.mark.parametrize(
        'agency_is_docs_separated',
        [
            pytest.param(True, id='agency_is_docs_separated_true'),
            pytest.param(False, id='agency_is_docs_separated_false'),
        ]
    )
    @pytest.mark.parametrize(
        'client_is_docs_separated',
        [
            pytest.param(True, id='client_is_docs_separated_true'),
            pytest.param(False, id='client_is_docs_separated_false'),
            pytest.param(None, id='client_is_docs_separated_none'),
        ]
    )
    def test_does_not_raise_for_single_subclient(self, session, from_log_tariff,
                                                 agency_is_docs_separated,
                                                 client_is_docs_separated):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        client = ob.ClientBuilder.construct(session, agency=agency)

        order1 = create_order(session, client, product=DIRECT_PRODUCT_ID, agency=agency)
        order2 = create_order(session, client, product=DIRECT_PRODUCT_ID, agency=agency)
        invoice = create_invoice(session, agency, [order1, order2], qty=10)
        consume1, consume2 = invoice.consumes

        act_rows = [
            ActRow(consume1, 10, 300, act_money=300),
            ActRow(consume2, 10, 300, act_money=300),
        ]

        agency.is_docs_separated = agency_is_docs_separated
        if client_is_docs_separated is not None:
            client.agencies_printable_doc_types = {
                str(agency.id): (True, client_is_docs_separated)}

        try:
            ActFactory.create_from_external(invoice, act_rows, YESTERDAY,
                                            from_log_tariff=from_log_tariff)
        except exc.NONSEPARATED_DOCUMENTS:
            pytest.fail(
                "Should not raise NONSEPARATED_DOCUMENTS for for single subclient")

    @pytest.mark.parametrize(
        'from_log_tariff',
        [
            pytest.param(True, id='from_log_tariff_true'),
            pytest.param(False, id='from_log_tariff_false'),
        ]
    )
    @pytest.mark.parametrize(
        'client_is_docs_separated',
        [
            pytest.param(False, id='client_is_docs_separated_false'),
            pytest.param(None, id='client_is_docs_separated_none'),
        ]
    )
    def test_does_not_raise_if_documents_are_not_separated(
        self,
        session,
        client_is_docs_separated,
        from_log_tariff
    ):
        invoice, act_rows, client, _ = self._set_up_invoice_and_act_rows(
            session,
            agency_is_docs_separated=False,
            client_is_docs_separated=False
        )
        if client_is_docs_separated is not None:
            client.agencies_printable_doc_types = {
                str(client.agency.id): (True, client_is_docs_separated)}

        try:
            ActFactory.create_from_external(invoice, act_rows, YESTERDAY,
                                            from_log_tariff=from_log_tariff)
        except exc.NONSEPARATED_DOCUMENTS:
            pytest.fail("Should not raise NONSEPARATED_DOCUMENTS if docs not separated")

    @pytest.mark.parametrize(
        ('agency_is_docs_separated', 'client_is_docs_separated'),
        [
            pytest.param(True, None, id='agency_is_docs_separated_true_1'),
            pytest.param(True, False, id='agency_is_docs_separated_true_2'),
            pytest.param(False, True, id='client_is_docs_separated_true'),
            pytest.param(True, True, id='all_is_docs_separated_true'),
        ]
    )
    def test_raises_if_separated_doc_for_multiple_subclients_for_non_tariff_acts(
        self, session, agency_is_docs_separated, client_is_docs_separated
    ):
        invoice, act_rows, client1, client2 = self._set_up_invoice_and_act_rows(
            session,
            agency_is_docs_separated=agency_is_docs_separated,
            client_is_docs_separated=client_is_docs_separated
        )

        with pytest.raises(exc.NONSEPARATED_DOCUMENTS) as exc_info:
            ActFactory.create_from_external(invoice, act_rows, YESTERDAY)

        assert sorted(exc_info.value.clients) == sorted([client1, client2])

    @pytest.mark.parametrize(
        ('agency_is_docs_separated', 'client_is_docs_separated'),
        [
            pytest.param(True, None, id='agency_is_docs_separated_true_1'),
            pytest.param(True, False, id='agency_is_docs_separated_true_2'),
            pytest.param(False, True, id='client_is_docs_separated_true'),
            pytest.param(True, True, id='all_is_docs_separated_true'),
        ]
    )
    def test_does_not_raise_if_separated_doc_for_multiple_subclients_for_tariff_acts(
        self, session, agency_is_docs_separated, client_is_docs_separated
    ):
        invoice, act_rows, _, _ = self._set_up_invoice_and_act_rows(
            session,
            agency_is_docs_separated=agency_is_docs_separated,
            client_is_docs_separated=client_is_docs_separated
        )

        try:
            ActFactory.create_from_external(invoice, act_rows, YESTERDAY,
                                            from_log_tariff=True)
        except exc.NONSEPARATED_DOCUMENTS:
            pytest.fail("Should not raise NONSEPARATED_DOCUMENTS for log tariff acts")

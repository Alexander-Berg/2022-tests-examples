# coding: utf-8

import datetime
from dateutil.relativedelta import relativedelta
from decimal import Decimal as D

import pytest
import hamcrest as hm

from balance import mapper, constants as cst
from balance.actions.cashback import (
    auto_charge,
    tear_off
)
import balance.muzzle_util as ut

from tests.balance_tests.cashback.common import (
    create_cashback,
    create_client,
    create_invoice,
    create_order,
    create_person,
    create_request,
    reverse_invoice,
    reserve_promo_code,
    round_func
)

pytestmark = [
    pytest.mark.cashback,
]


def test_base_tear_off(session, client):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(session, client, orders=[(order, 1)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback = create_cashback(client, bonus=55000, finish_dt=finish_dt)
    assert cashback.bonus == 55000

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert cashback.bonus == 45001

    cashback.finish_dt = ut.trunc_date(datetime.datetime.now())
    session.flush()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # consume with cashback
            hm.has_properties(
                current_sum=D('1'),
                current_qty=D('10000'),
                cashback_base=D('1'),
                cashback_bonus=D('9999'),
                current_cashback_bonus=D('9999'),
            ),
        ),
    )

    tear_off.tear_cashback_off(session, client)
    session.expire_all()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # consume with cashback
            hm.has_properties(
                current_sum=D('0'),
                current_qty=D('0'),
                cashback_base=D('1'),
                cashback_bonus=D('9999'),
                current_cashback_bonus=D('0'),
            ),
            # consume without cashback
            hm.has_properties(
                current_sum=D('1'),
                current_qty=D('1'),
                cashback_base=None,
                cashback_bonus=None,
                current_cashback_bonus=D('0'),
            ),
        ),
    )

    assert cashback.bonus == 55000


def test_overacted(session, client):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(session, client, orders=[(order, 1)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback = create_cashback(client, bonus=55000, finish_dt=finish_dt)
    assert cashback.bonus == 55000

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert cashback.bonus == 45001

    order.calculate_consumption(session.now(), {'Money': D('10000'), 'Bucks': 0})
    invoice.generate_act(force=1, backdate=datetime.datetime.now())
    session.flush()

    order.calculate_consumption(session.now(), {'Money': D('0'), 'Bucks': 0})

    cashback.finish_dt = ut.trunc_date(datetime.datetime.now())
    session.flush()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # consume with cashback
            hm.has_properties(
                current_sum=D('1'),
                current_qty=D('10000'),
                cashback_base=D('1'),
                cashback_bonus=D('9999'),
                current_cashback_bonus=D('9999'),
            ),
        ),
    )

    tear_off.tear_cashback_off(session, client)
    session.expire_all()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # consume with cashback
            hm.has_properties(
                current_sum=D('1'),
                current_qty=D('10000'),
                cashback_base=D('1'),
                cashback_bonus=D('9999'),
                current_cashback_bonus=D('9999'),
            ),
        ),
    )


def test_overacted_one_consume(session, client):
    order_1 = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    order_2 = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(session, client, orders=[(order_1, 1), (order_2, 1)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback = create_cashback(client, bonus=10009, finish_dt=finish_dt)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert cashback.bonus == 0

    order_1.calculate_consumption(session.now(), {'Money': D('10000'), 'Bucks': 0})
    invoice.generate_act(force=1, backdate=datetime.datetime.now())
    session.flush()

    order_1.calculate_consumption(session.now(), {'Money': D('0'), 'Bucks': 0})
    order_2.calculate_consumption(session.now(), {'Money': D('5.005'), 'Bucks': 0})

    cashback.finish_dt = ut.trunc_date(datetime.datetime.now())
    session.flush()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consumes
            hm.has_properties(current_sum=0, current_qty=0),
            hm.has_properties(current_sum=0, current_qty=0),
            # consumes with cashback
            hm.has_properties(
                act_sum=D('1'),
                act_qty=D('10000'),
                completion_sum=D('0'),
                completion_qty=D('0'),
                current_sum=D('1'),
                current_qty=D('10000'),
                cashback_base=D('1'),
                cashback_bonus=D('9999'),
                current_cashback_bonus=D('9999'),
            ),
            hm.has_properties(
                act_sum=D('0'),
                act_qty=D('0'),
                completion_sum=D('0.01'),
                completion_qty=D('5.005'),
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # consume without cashback
            hm.has_properties(
                act_sum=D('0'),
                act_qty=D('0'),
                completion_sum=D('0'),
                completion_qty=D('0'),
                current_sum=D('0.99'),
                current_qty=D('0.99'),
                cashback_base=None,
                cashback_bonus=None,
                current_cashback_bonus=D('0'),
            ),
        ),
    )

    tear_off.tear_cashback_off(session, client)
    session.expire_all()

    assert sum([co.current_sum for co in invoice.consumes]) == D('2')
    assert sum([co.current_qty for co in invoice.consumes]) == D('10006')
    assert sum([co.current_cashback_bonus for co in invoice.consumes]) == D('10004')
    assert cashback.bonus == 5

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consumes
            hm.has_properties(current_sum=0, current_qty=0),
            hm.has_properties(current_sum=0, current_qty=0),
            # consumes with cashback
            hm.has_properties(
                act_sum=D('1'),
                act_qty=D('10000'),
                completion_sum=D('0'),
                completion_qty=D('0'),
                current_sum=D('1'),
                current_qty=D('10000'),
                cashback_base=D('1'),
                cashback_bonus=D('9999'),
                current_cashback_bonus=D('9999'),
            ),
            hm.has_properties(
                act_sum=D('0'),
                act_qty=D('0'),
                completion_sum=D('0.01'),
                completion_qty=D('5.005'),
                current_sum=D('0.01'),
                current_qty=D('5.005'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('5'),
            ),
            # consumes without cashback
            hm.has_properties(
                act_sum=D('0'),
                act_qty=D('0'),
                completion_sum=D('0'),
                completion_qty=D('0'),
                current_sum=D('0.99'),
                current_qty=D('0.99'),
                cashback_base=None,
                cashback_bonus=None,
                current_cashback_bonus=D('0'),
            ),
            hm.has_properties(
                consume_sum=D('0'),
                consume_qty=D('0.005'),
                current_sum=D('0'),
                current_qty=D('0.005'),
                cashback_base=None,
                cashback_bonus=None,
                current_cashback_bonus=D('0'),
            ),
        ),
    )

    reverse_operation_id = order_2.consumes[1].reverses[0].operation.id
    # Check that consume without cashback hasn't been reversed
    assert order_2.consumes[2].operation.id != reverse_operation_id
    # Check that reverses and consumes are created in the same operation
    assert order_2.consumes[3].operation.id == reverse_operation_id


def test_tear_off_with_promocode(session, client):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(session, client, orders=[(order, 1)])
    invoice = create_invoice(session, client, request_=request)
    reserve_promo_code(client, bonus=1)
    invoice.turn_on_rows(apply_promocode=True)
    promocode_pct = invoice.consumes[0].discount_obj.promo_code_pct
    assert promocode_pct > 0

    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback = create_cashback(client, bonus=55000, finish_dt=finish_dt)
    assert cashback.bonus == 55000

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    cashback.finish_dt = ut.trunc_date(datetime.datetime.now())
    session.flush()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # new consume with cashback
            hm.has_properties(
                current_sum=D('1'),
                current_qty=D('9999.99'),  # qty + promo_code + cashback
                cashback_base=D('2.2'),  # qty + promo_code
                cashback_bonus=D('9997.79'),
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=hm.greater_than(0),
                )
            ),
        ),
    )

    tear_off.tear_cashback_off(session, client)
    session.expire_all()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            hm.has_properties(
                current_sum=0,
                current_qty=0,
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=hm.greater_than(0),
                )),
            hm.has_properties(
                current_sum=D('1'),
                current_qty=D('2.2'),  # qty + promo_code
                cashback_base=None,
                cashback_bonus=None,
                discount_obj=hm.has_properties(
                    pct=promocode_pct,
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=0,
                )
            ),
        ),
    )

    assert cashback.bonus == 55000


def test_tear_off_with_promocode_consumption(session, client):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(session, client, orders=[(order, 1)])
    invoice = create_invoice(session, client, request_=request)
    reserve_promo_code(client, bonus=1)
    invoice.turn_on_rows(apply_promocode=True)
    promocode_pct = invoice.consumes[0].discount_obj.promo_code_pct
    assert promocode_pct > 0

    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback = create_cashback(client, bonus=55000, finish_dt=finish_dt)
    assert cashback.bonus == 55000

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    order.calculate_consumption(session.now(), {'Money': D('1000'), 'Bucks': 0})

    cashback.finish_dt = ut.trunc_date(datetime.datetime.now())
    session.flush()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # new consume with cashback
            hm.has_properties(
                current_sum=D('1'),
                current_qty=D('9999.99'),  # qty + promo_code + cashback
                cashback_base=D('2.2'),  # qty + promo_code
                cashback_bonus=D('9997.79'),
                completion_qty=D('1000'),
                completion_sum=D('0.1'),
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=hm.greater_than(0),
                )
            ),
        ),
    )

    tear_off.tear_cashback_off(session, client)
    session.expire_all()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            hm.has_properties(
                current_sum=D('0.1'),
                current_qty=D('1000'),
                completion_qty=D('1000'),
                completion_sum=D('0.1'),
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=hm.greater_than(0),
                )),
            hm.has_properties(
                current_sum=D('0.9'),
                current_qty=D('1.98'),  # qty + promo_code
                cashback_base=None,
                cashback_bonus=None,
                discount_obj=hm.has_properties(
                    pct=promocode_pct,
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=0,
                )
            ),
        ),
    )

    assert cashback.bonus == D('54000.22')


def test_tear_finish_dt(session, client):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(session, client, orders=[(order, 100)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    cashback_1 = create_cashback(
        client,
        bonus=10,
        finish_dt=ut.trunc_date(datetime.datetime.now()) + relativedelta(days=1)
    )
    cashback_2 = create_cashback(
        client,
        bonus=10,
        finish_dt=ut.trunc_date(datetime.datetime.now()) + relativedelta(days=2)
    )
    cashback_3 = create_cashback(
        client,
        bonus=10,
        finish_dt=ut.trunc_date(datetime.datetime.now()) + relativedelta(days=3)
    )
    cashback_4 = create_cashback(client, bonus=10, finish_dt=None)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert cashback_1.bonus == 0
    assert cashback_2.bonus == 0
    assert cashback_3.bonus == 0
    assert cashback_4.bonus == 0

    cashback_2.finish_dt = ut.trunc_date(datetime.datetime.now())
    cashback_3.finish_dt = ut.trunc_date(datetime.datetime.now()) - relativedelta(days=1)
    session.flush()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # consume with first cashback
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # consume with second cashback
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # consume with third cashback
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # consume with forth cashback
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # consume with remaining money
            hm.has_properties(current_sum=D('99.96'), current_qty=D('99.96')),
        ),
    )

    tear_off.tear_cashback_off(session, client)
    session.expire_all()

    assert cashback_1.bonus == 0
    assert cashback_2.bonus == 10
    assert cashback_3.bonus == 10
    assert cashback_4.bonus == 0

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # consume with first cashback
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # consume with second cashback
            hm.has_properties(
                current_sum=D('0'),
                current_qty=D('0'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('0'),
            ),
            # consume with third cashback
            hm.has_properties(
                current_sum=D('0'),
                current_qty=D('0'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('0'),
            ),
            # consume with forth cashback
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # consume with remaining money
            hm.has_properties(
                consume_sum=D('99.96'),
                consume_qty=D('99.96'),
                current_sum=D('99.98'),
                current_qty=D('99.98')
            ),
        ),
    )

    assert invoice.consumes[0].cashback_usage is None
    assert invoice.consumes[1].cashback_usage.client_cashback.id == cashback_1.id
    assert invoice.consumes[2].cashback_usage.client_cashback.id == cashback_2.id
    assert invoice.consumes[3].cashback_usage.client_cashback.id == cashback_3.id
    assert invoice.consumes[4].cashback_usage.client_cashback.id == cashback_4.id
    assert invoice.consumes[5].cashback_usage is None


def test_tear_one_of_several_consumption(session, client):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(session, client, orders=[(order, 100)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    cashback_1 = create_cashback(client, bonus=10, finish_dt=None)
    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_2 = create_cashback(client, bonus=10, finish_dt=finish_dt)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert cashback_1.bonus == 0
    assert cashback_2.bonus == 0

    order.calculate_consumption(session.now(), {'Money': D('5.005'), 'Bucks': 0})

    cashback_2.finish_dt = ut.trunc_date(datetime.datetime.now())
    session.flush()

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # consume with cashback
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # new consume with cashback. One penny is a minimum sum.
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # consume with remaining money
            hm.has_properties(current_sum=D('99.98'), current_qty=D('99.98')),
        ),
    )

    tear_off.tear_cashback_off(session, client)
    session.expire_all()

    assert cashback_1.bonus == 0
    assert cashback_2.bonus == 5

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0, discount_obj=mapper.DiscountObj()),
            # consume with cashback
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('5.005'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('5'),
                completion_sum=D('0.01'),
                completion_qty=D('5.005')
            ),
            # consume with cashback.
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
            ),
            # consume with remaining money
            hm.has_properties(
                consume_sum=D('99.98'),
                consume_qty=D('99.98'),
                current_sum=D('99.98'),
                current_qty=D('99.98'),
                discount_obj=mapper.DiscountObj()
            ),
            hm.has_properties(
                consume_sum=D('0'),
                consume_qty=D('0.005'),
                current_sum=D('0'),
                current_qty=D('0.005'),
                discount_obj=mapper.DiscountObj()
            ),
        ),
    )

    assert sum([co.current_sum for co in invoice.consumes]) == D('100')
    assert sum([co.current_qty for co in invoice.consumes]) == D('115')
    assert sum([co.current_cashback_bonus for co in invoice.consumes]) == D('15')

    assert invoice.consumes[1].cashback_usage.client_cashback.id == cashback_2.id
    assert invoice.consumes[2].cashback_usage.client_cashback.id == cashback_1.id

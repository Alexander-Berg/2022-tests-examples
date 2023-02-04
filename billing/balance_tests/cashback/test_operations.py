# coding: utf-8

import datetime
from dateutil.relativedelta import relativedelta
from decimal import Decimal as D
import mock

import pytest
import hamcrest as hm

from balance import mapper, constants as cst, muzzle_util as ut
from balance.mapper.invoices import DiscountObj
from balance.actions.cashback import auto_charge, operations
from balance.actions.cashback.auto_charge import CashbackBonus
from butils.decimal_unit import DecimalUnit as DU

from tests import object_builder as ob
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


def test_calc_qty_huge_bonus():
    do = DiscountObj(base_pct=10)
    qty = 1
    bonus = 99999999
    mb = operations.calc_max_bonus(qty, bonus, do)
    assert mb < bonus
    qty_for_bonus = auto_charge.calc_qty_for_bonus(mb, do, round_func)
    assert qty == qty_for_bonus
    assert do.with_cashback(qty, mb, None).pct > D('99.9')

    qty_for_bonus = auto_charge.calc_qty_for_bonus(bonus, do, round_func)
    assert qty_for_bonus > qty


def test_calc_qty_small_bonus():
    do = DiscountObj(base_pct=10)
    qty = 10000
    bonus = 10
    mb = operations.calc_max_bonus(qty, bonus, do)
    assert mb == bonus
    qty_for_bonus = auto_charge.calc_qty_for_bonus(mb, do, round_func)
    assert qty_for_bonus < qty
    assert do.with_cashback(qty_for_bonus, mb, None).pct > D('99.9')


@pytest.mark.parametrize(
    ['qty', 'sum_', 'bonus', 'res_qty_with_bonus', 'res_remaining_qty'],
    [
        (D('0.01'), D('0.01'), D('0.000001'), D('0.01'), 0),
        (D('0.05'), D('0.05'), 1, D('0.01'), D('0.04')),
        # Test price calculation
        (D('2'), D('0.02'), 1, 1, 1),
        # For 2 pennies we need 200 rub bonus. With 170 rub bonus we would not spend all the pennies,
        # but we cannot create a consume with qty which is equivalent to 30 rub bonus, because
        # it is less than one penny. So we have to transfer all the money to the consume with bonus.
        (D('0.02'), D('0.02'), 170, D('0.02'), 0),
        # proper min_qty calculation
        (D('0.299999'), D('0.03'), 1, D('0.1'), D('0.199999')),
        # We do not return more than qty.
        (D('0.01'), D('0.01'), 999999, D('0.01'), 0),
    ]
)
def test_calc_qty_parts_with_max_bonus(qty, sum_, bonus, res_qty_with_bonus, res_remaining_qty):
    assert auto_charge.calc_qty_parts_with_max_bonus(
        qty, sum_, mapper.DiscountObj(), bonus, round_func, qty
    ) == (res_qty_with_bonus, res_remaining_qty)


def test_add_available_cashback_bonus_no_split(session, invoice):
    """

    """
    reverse = reverse_invoice(invoice)
    available_bonus = 9999 * reverse.consume.consume_qty
    cashback = create_cashback(invoice.client, bonus=available_bonus)
    res = auto_charge.add_available_cashback_bonus(
        session=session,
        row=reverse,
        qty=reverse.reverse_qty,
        sum_=reverse.reverse_sum,
        discount_obj=reverse.consume.discount_obj,
        cashback_bonuses=[CashbackBonus(cashback, cashback.bonus)]
    )
    assert len(res) == 1
    res_qty, res_sum, res_do = res[0]
    assert res_sum == reverse.reverse_sum
    assert res_do.pct == D('99.99')
    assert res_do.cashback_base == reverse.reverse_qty
    assert res_do.cashback_bonus == available_bonus
    assert res_qty == reverse.reverse_qty + res_do.cashback_bonus


def test_add_available_cashback_bonus_split(session, invoice):
    reverse = reverse_invoice(invoice)
    available_bonus = 9999 * reverse.consume.consume_qty / 2
    cashback = create_cashback(invoice.client, bonus=available_bonus)

    res = auto_charge.add_available_cashback_bonus(
        session=session,
        row=reverse,
        qty=reverse.reverse_qty,
        sum_=reverse.reverse_sum,
        discount_obj=reverse.consume.discount_obj,
        cashback_bonuses=[CashbackBonus(cashback, cashback.bonus)]
    )
    assert len(res) == 2
    res_qty1, res_sum1, res_do1 = res[0]  # with cashback
    res_qty2, res_sum2, res_do2 = res[1]  # without cashback
    assert res_sum1 + res_sum2 == reverse.reverse_sum
    assert res_qty1 + res_qty2 == reverse.reverse_qty + available_bonus

    assert res_do1.pct == D('99.99')
    assert res_do1.cashback_bonus == available_bonus

    assert res_do2 == reverse.consume.discount_obj


# TODO: Write a more complete test checking that cashback is not applied if it is disallowed for a client.
# This is a silly test, but it is very important not to auto charge forbidden clients or services.
def test_client_auto_charge_services(session, client):
    client.cashback_settings[cst.MARKET_SERVICE_ID] = mapper.ClientCashbackSettings(
        client_id=client.id,
        service_id=cst.MARKET_SERVICE_ID,
        is_auto_charge_enabled=True,
    )
    session.config.__dict__['AUTO_CASHBACK_CHARGING_SERVICES'] = [
        cst.DIRECT_SERVICE_ID,
        cst.ServiceId.TAXI_CASH,
    ]
    assert client.cashback_auto_charge_services == {cst.DIRECT_SERVICE_ID}


def check_operation(op):
    assert op.type_id == cst.OperationTypeIDs.cashback_auto_charge
    assert sum(-r.reverse_qty for r in op.reverses) + sum(q.current_qty for q in op.consumes) >= 0
    assert sum(-r.reverse_sum for r in op.reverses) + sum(q.current_sum for q in op.consumes) == 0
    # Base qty must not change.
    assert sum(-r.reverse_qty for r in op.reverses) + sum(
        q.current_qty if q.cashback_usage_id is None else q.discount_obj.cashback_base
        for q in op.consumes
    ) == 0


def test_auto_charge_client(session, client):
    invoices = {}
    orders = {
        'RUB1': create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID),
        'RUB2': create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID),
        'Bucks': create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID),
    }

    # This invoice will be processed last
    request = create_request(session, client, orders=[(orders['RUB1'], 1)])
    invoices['with_pc'] = create_invoice(session, client, request_=request)
    reserve_promo_code(client, bonus=1)
    invoices['with_pc'].turn_on_rows(apply_promocode=True)
    promocode_pct = invoices['with_pc'].consumes[0].discount_obj.promo_code_pct
    assert promocode_pct == D('54.5455')

    # This invoice will be processed first
    request = create_request(session, client, orders=[
        # This consume will be in the first group because its product id is smaller.
        (orders['Bucks'], 1),
        # These two consumes must end up in the same group and same operation.
        (orders['RUB1'], 1), (orders['RUB2'], 1),
    ])
    invoices['wo_pc'] = create_invoice(session, client, request_=request)
    invoices['wo_pc'].turn_on_rows()

    orders['Bucks'].calculate_consumption(session.now(), {'Money': 27, 'Bucks': 0})

    cashback = create_cashback(client, bonus=55000)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)
    assert cashback.bonus == 0

    hm.assert_that(
        invoices['with_pc'].consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # new consume with cashback
            # Reverse is split into two consumes because there is not enough bonus to spend all available qty.
            hm.has_properties(
                current_sum=D('0.5'),
                current_qty=D('5006.1014'),
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=hm.greater_than(0),
                )
            ),
            # consume with remaining money
            hm.has_properties(
                current_sum=D('0.5'),
                current_qty=D('1.0986'),
                discount_obj=hm.has_properties(
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=0,
                )
            ),
        ),
    )

    # Check sum rounded delta.
    assert invoices['with_pc'].consumes[1].current_sum + invoices['with_pc'].consumes[2].current_sum == 1

    hm.assert_that(
        invoices['wo_pc'].consumes,
        hm.contains_inanyorder(
            # initial consumes
            hm.has_properties(
                # Bucks order. Completed qty was not reversed.
                current_sum=27, current_qty=D('0.9')
            ),
            hm.has_properties(current_sum=0, current_qty=0, parent_order_id=orders['RUB1'].id),
            hm.has_properties(current_sum=0, current_qty=0, parent_order_id=orders['RUB2'].id),

            # new consumes with cashback
            hm.has_properties(
                # Bucks order.
                current_sum=3,
                current_qty=1000,
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    cashback_base=D('0.1'),
                )
            ),
            hm.has_properties(
                parent_order_id=orders['RUB1'].id,
                current_sum=D('1'),
                current_qty=D('10000'),
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    cashback_bonus=hm.greater_than(0),
                )
            ),
            hm.has_properties(
                parent_order_id=orders['RUB2'].id,
                current_sum=D('1'),
                current_qty=D('10000'),
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    cashback_bonus=hm.greater_than(0),
                )
            ),
        ),
    )

    assert len({
        orders['Bucks'].consumes[0].reverses[0].operation.id,
        orders['RUB1'].consumes[0].reverses[0].operation.id,
        orders['RUB1'].consumes[1].reverses[0].operation.id,
    }) == 3

    # These two are supposed to be in the same group.
    assert orders['RUB2'].consumes[0].reverses[0].operation.id == orders['RUB1'].consumes[1].reverses[0].operation.id

    check_operation(orders['Bucks'].consumes[0].reverses[0].operation)
    check_operation(orders['RUB1'].consumes[0].reverses[0].operation)
    check_operation(orders['RUB1'].consumes[1].reverses[0].operation)

    # Check that reverses and consumes are created in the same operation
    assert orders['Bucks'].consumes[1].operation.id == orders['Bucks'].consumes[0].reverses[0].operation.id


def test_auto_charge_client_several_small_bonuses(session, client):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(session, client, orders=[(order, 100)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    cashback_1 = create_cashback(client, bonus=10, finish_dt=None)
    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_2 = create_cashback(client, bonus=10, finish_dt=finish_dt)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert sum([co.current_sum for co in invoice.consumes]) == D('100')
    assert sum([co.current_qty for co in invoice.consumes]) == D('120')
    assert sum([co.current_cashback_bonus for co in invoice.consumes]) == D('20')
    assert cashback_1.bonus == 0
    assert cashback_2.bonus == 0

    operation = order.consumes[0].reverses[0].operation
    check_operation(operation)

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # new consumes with cashback. One penny is a minimum sum.
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
                operation_id=operation.id,
                client_cashback=cashback_2,
            ),
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('10.01'),
                cashback_base=D('0.01'),
                cashback_bonus=D('10'),
                current_cashback_bonus=D('10'),
                operation_id=operation.id,
                client_cashback=cashback_1,
            ),
            # consume with remaining money
            hm.has_properties(current_sum=D('99.98'), current_qty=D('99.98'), operation_id=operation.id),
        ),
    )


def test_auto_charge_client_several_small_bonuses_fish(session, client):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)

    request = create_request(session, client, orders=[(order, 1)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    cashback_1 = create_cashback(client, bonus=10, finish_dt=None)
    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_2 = create_cashback(client, bonus=10, finish_dt=finish_dt)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert sum([co.current_sum for co in invoice.consumes]) == D('30')
    assert sum([co.current_qty for co in invoice.consumes]) == D('1.666666')
    assert sum([co.current_cashback_bonus for co in invoice.consumes]) == D('0.666666')
    assert cashback_1.bonus == D('0.00001')
    assert cashback_2.bonus == D('0.00001')

    operation = order.consumes[0].reverses[0].operation
    check_operation(operation)

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # new consumes with cashback. One penny is a minimum sum.
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('0.333667'),
                cashback_base=D('0.000334'),
                cashback_bonus=D('0.333333'),
                current_cashback_bonus=D('0.333333'),
                operation_id=operation.id,
                client_cashback=cashback_2,
            ),
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('0.333667'),
                cashback_base=D('0.000334'),
                cashback_bonus=D('0.333333'),
                current_cashback_bonus=D('0.333333'),
                operation_id=operation.id,
                client_cashback=cashback_1,
            ),
            # consume with remaining money
            hm.has_properties(current_sum=D('29.98'), current_qty=D('0.999332'), operation_id=operation.id),
        ),
    )


def test_auto_charge_client_several_bonuses(session, client):
    # This consume will be in the first group because its product id is smaller.
    order_fish = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)
    order_rub = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(
        session,
        client,
        [
            (order_fish, D('1')),
            (order_rub, D('1')),
        ]
    )
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_time_limit = create_cashback(client, bonus=5000, finish_dt=finish_dt)
    finish_dt_2 = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=61)
    cashback_time_limit_2 = create_cashback(client, bonus=295000, finish_dt=finish_dt_2)
    cashback = create_cashback(client, bonus=5000, finish_dt=None)

    total_bonus = cashback_time_limit.bonus + cashback_time_limit_2.bonus + cashback.bonus
    assert total_bonus == 305000

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert (
        sum([co.current_sum for co in order_fish.consumes]) + sum([co.current_sum for co in order_rub.consumes])
           ) == D('31')
    assert (
        sum([co.current_qty * D('30') for co in order_fish.consumes]) +
        sum([co.current_qty for co in order_rub.consumes])
           ) == D('305030.99995')
    assert (
        sum([co.current_cashback_bonus * D('30') for co in order_fish.consumes]) +
        sum([co.current_cashback_bonus for co in order_rub.consumes])
           ) == D('304999.99995')
    assert cashback.bonus == D('0')
    assert cashback_time_limit.bonus == D('0.00002')
    assert cashback_time_limit_2.bonus == D('0.00003')

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial order_fish consume
            hm.has_properties(current_sum=0, current_qty=0),
            # initial order_rub consume
            hm.has_properties(current_sum=0, current_qty=0),
            # new order_fish consume with cashback for first cashback.
            hm.has_properties(
                current_sum=D('0.5'),
                current_qty=D('166.683335'),
                cashback_base=D('0.016669'),
                cashback_bonus=D('166.666666'),
                current_cashback_bonus=D('166.666666'),
                client_cashback=cashback_time_limit,
            ),
            # new order_fish consume with cashback for second cashback.
            hm.has_properties(
                current_sum=D('29.5'),
                current_qty=D('9833.31'),
                cashback_base=D('0.983331'),
                cashback_bonus=D('9832.326669'),
                current_cashback_bonus=D('9832.326669'),
                client_cashback=cashback_time_limit_2,
            ),
            # new order_rub consume with cashback for second cashback.
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('30.2099'),
                cashback_base=D('0.01'),
                cashback_bonus=D('30.1999'),
                current_cashback_bonus=D('30.1999'),
                client_cashback=cashback_time_limit_2,
            ),
            # new order_rub consume with cashback for third cashback.
            hm.has_properties(
                current_sum=D('0.5'),
                current_qty=D('5000.5001'),
                cashback_base=D('0.5001'),
                cashback_bonus=D('5000'),
                current_cashback_bonus=D('5000'),
                client_cashback=cashback,
            ),
            # consume with remaining money
            hm.has_properties(current_sum=D('0.49'), current_qty=D('0.4899')),
        ),
    )

    # Check that reverses and consumes are created in the same operation
    assert order_fish.consumes[1].operation.id == order_fish.consumes[0].reverses[0].operation.id
    assert order_fish.consumes[2].operation.id == order_fish.consumes[0].reverses[0].operation.id

    # Check that reverses and consumes are created in the same operation
    assert order_rub.consumes[1].operation.id == order_rub.consumes[0].reverses[0].operation.id
    assert order_rub.consumes[2].operation.id == order_rub.consumes[0].reverses[0].operation.id
    assert order_rub.consumes[3].operation.id == order_rub.consumes[0].reverses[0].operation.id

    check_operation(order_fish.consumes[0].reverses[0].operation)
    check_operation(order_rub.consumes[0].reverses[0].operation)


def test_auto_charge_client_several_bonuses_promocode(session, client):
    # This consume will be in the first group because its product id is smaller.
    order_fish = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)
    order_rub = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(
        session,
        client,
        [
            (order_fish, D('1')),
            (order_rub, D('1')),
        ]
    )
    invoice = create_invoice(session, client, request_=request)
    reserve_promo_code(client, bonus=100)
    invoice.turn_on_rows(apply_promocode=True)

    promocode_pct = invoice.consumes[0].discount_obj.promo_code_pct
    assert promocode_pct == D('79.4702')

    assert (
               sum([co.current_qty * D('30') for co in order_fish.consumes]) +
               sum([co.current_qty for co in order_rub.consumes])
           ) == D('151.00004')

    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_time_limit = create_cashback(client, bonus=5000, finish_dt=finish_dt)
    finish_dt_2 = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=61)
    cashback_time_limit_2 = create_cashback(client, bonus=295000, finish_dt=finish_dt_2)
    cashback = create_cashback(client, bonus=5000, finish_dt=None)

    total_bonus = cashback_time_limit.bonus + cashback_time_limit_2.bonus + cashback.bonus
    assert total_bonus == 305000

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert (
               sum([co.current_sum for co in order_fish.consumes]) + sum([co.current_sum for co in order_rub.consumes])
           ) == D('31')
    assert (
               sum([co.current_qty * D('30') for co in order_fish.consumes]) +
               sum([co.current_qty for co in order_rub.consumes])
           ) == D('305150.99997')
    assert (
               sum([co.current_cashback_bonus * D('30') for co in order_fish.consumes]) +
               sum([co.current_cashback_bonus for co in order_rub.consumes])
           ) == D('304999.99993')
    assert cashback.bonus == D('0')
    assert cashback_time_limit.bonus == D('0.00002')
    assert cashback_time_limit_2.bonus == D('0.00005')

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial order_fish consume
            hm.has_properties(current_sum=0, current_qty=0, discount_pct=promocode_pct),
            # initial order_rub consume
            hm.has_properties(current_sum=0, current_qty=0, discount_pct=promocode_pct),
            # new order_fish consume with cashback for first cashback.
            hm.has_properties(
                current_sum=D('0.5'),
                current_qty=D('166.747889'),
                cashback_base=D('0.081223'),
                cashback_bonus=D('166.666666'),
                current_cashback_bonus=D('166.666666'),
                client_cashback=cashback_time_limit,
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=D('166.666666'),
                )
            ),
            # new order_fish consume with cashback for second cashback.
            hm.has_properties(
                current_sum=D('29.5'),
                current_qty=D('9833.250690'),
                cashback_base=D('4.789745'),
                cashback_bonus=D('9828.460945'),
                current_cashback_bonus=D('9828.460945'),
                client_cashback=cashback_time_limit_2,
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=D('9828.460945'),
                )
            ),
            # new order_rub consume with cashback for second cashback.
            hm.has_properties(
                current_sum=D('0.01'),
                current_qty=D('146.2429'),
                cashback_base=D('0.0713'),
                cashback_bonus=D('146.1716'),
                current_cashback_bonus=D('146.1716'),
                client_cashback=cashback_time_limit_2,
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=D('146.1716'),
                )
            ),
            # new order_rub consume with cashback for third cashback.
            hm.has_properties(
                current_sum=D('0.5'),
                current_qty=D('5002.4367'),
                cashback_base=D('2.4367'),
                cashback_bonus=D('5000'),
                current_cashback_bonus=D('5000'),
                client_cashback=cashback,
                discount_obj=hm.has_properties(
                    pct=D('99.99'),
                    promo_code_pct=promocode_pct,  # promo code bonus must stay in place
                    cashback_bonus=D('5000'),
                )
            ),
            # consume with remaining money
            hm.has_properties(current_sum=D('0.49'), current_qty=D('2.3630'), discount_pct=promocode_pct),
        ),
    )

    # Check that reverses and consumes are created in the same operation
    assert order_fish.consumes[1].operation.id == order_fish.consumes[0].reverses[0].operation.id
    assert order_fish.consumes[2].operation.id == order_fish.consumes[0].reverses[0].operation.id

    # Check that reverses and consumes are created in the same operation
    assert order_rub.consumes[1].operation.id == order_rub.consumes[0].reverses[0].operation.id
    assert order_rub.consumes[2].operation.id == order_rub.consumes[0].reverses[0].operation.id
    assert order_rub.consumes[3].operation.id == order_rub.consumes[0].reverses[0].operation.id

    check_operation(order_fish.consumes[0].reverses[0].operation)
    check_operation(order_rub.consumes[0].reverses[0].operation)


def test_small_bonus(session, client):
    """
    Regression test for BALANCE-37984

    You need at least 100 RUB bonus to spend 0.01 RUB consume sum with 99.99% discount.
    0.001 RUB consume sum is sufficient for 10 RUB bonus, but before BALANCE-37984
    it would be rounded down to 0 RUB consume sum, and we would skip that row,
    and spill 0.001 RUB qty on the ground.
    """
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    request = create_request(session, client, orders=[(order, 100)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    initial_bonus = 10
    cashback = create_cashback(client, bonus=initial_bonus)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)
    check_operation(invoice.consumes[0].reverses[0].operation)
    assert cashback.bonus == 0
    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # new consume with cashback. One penny is a minimum sum.
            hm.has_properties(current_sum=D('0.01'), current_qty=D('10.01')),
            # consume with remaining money
            hm.has_properties(current_sum=D('99.99'), current_qty=D('99.99')),
        ),
    )


def test_small_consume_wo_bonus(session, client):
    """
    Here situation is similar to test_small_bonus, but in this case previously
    we would skip consume without cashback, and spill it's remaining qty on the ground.
    190 RUB bonus is equivalent to 0.019 RUB qty.
    """
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    request = create_request(session, client, orders=[(order, D(0.02))])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()
    cashback = create_cashback(client, bonus=190)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)
    check_operation(invoice.consumes[0].reverses[0].operation)
    assert cashback.bonus == 0
    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # initial consume
            hm.has_properties(current_sum=0, current_qty=0),
            # new consume with cashback
            hm.has_properties(current_sum=D('0.02'), current_qty=D('190.02')),
        ),
    )


@pytest.mark.parametrize('product_id', [cst.DIRECT_PRODUCT_RUB_ID, cst.DIRECT_PRODUCT_ID,])
def test_bonus_exceeding_limit(session, client, product_id):
    """
    Regression test for BALANCE-38018

    there is a limit for auto cashback charging and we check that no extra bonuses were added
    and all bonuses are charged in right way
    """

    product_price = 1 if product_id == cst.DIRECT_PRODUCT_RUB_ID else cst.DIRECT_PRODUCT_PRICE
    max_cashback_sum = 19998 * product_price
    session.config.__dict__['MAX_AUTO_CHARGE_CASHBACK_SUM'] = {'RUB': max_cashback_sum}

    orders1 = [
        (create_order(session, client, product_id=product_id), 2),
        (create_order(session, client, product_id=product_id), 2),
    ]
    request1 = create_request(session, client, orders=orders1)
    invoice1 = create_invoice(session, client, request_=request1)
    invoice1.turn_on_rows()

    orders2 = [
        (create_order(session, client, product_id=product_id), 1),
    ]
    request2 = create_request(session, client, orders=orders2)
    invoice2 = create_invoice(session, client, request_=request2)
    invoice2.turn_on_rows()

    extra_bonus_sum = 100000
    initial_bonus = max_cashback_sum + extra_bonus_sum
    cashback = create_cashback(client, bonus=initial_bonus)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    check_operation(invoice1.consumes[0].reverses[0].operation)
    check_operation(invoice2.consumes[0].reverses[0].operation)
    assert cashback.bonus == DU(extra_bonus_sum, 'QTY')
    # second invoice is processed first
    hm.assert_that(
        invoice2.consumes,
        hm.contains(
            # initial consume is reversed
            hm.has_properties(current_sum=0, current_qty=0),
            # new consume with cashback
            hm.has_properties(current_sum=D("{}".format(1 * product_price)), current_qty=D('10000'), cashback_bonus=D('9999'))
        ),
    )
    hm.assert_that(
        invoice1.consumes,
        hm.contains(
            # 1st order: initial consume is reversed
            hm.has_properties(current_sum=0, current_qty=0),
            # 2nd order: initial consume is not reversed
            hm.has_properties(current_sum=D("{}".format(2 * product_price)), current_qty=D('2'), cashback_bonus=None),
            # 1st order: new consume with cashback
            hm.has_properties(current_sum=D("{}".format(1 * product_price)), current_qty=D('10000'), cashback_bonus=D('9999')),
            # 1st order: consume with remaining money
            hm.has_properties(current_sum=D("{}".format(1 * product_price)), current_qty=D('1'), cashback_bonus=None),
        ),
    )


@pytest.mark.parametrize('product_id', [cst.DIRECT_PRODUCT_RUB_ID, cst.DIRECT_PRODUCT_ID,])
def test_bonus_exceeding_limit_several_cashbacks(session, client, product_id):
    """
    Regression test for BALANCE-38018

    there is a limit for auto cashback charging and we check that no extra bonuses were added
    and all bonuses are charged in right way
    """

    product_price = 1 if product_id == cst.DIRECT_PRODUCT_RUB_ID else cst.DIRECT_PRODUCT_PRICE
    max_cashback_sum = 19998 * product_price
    session.config.__dict__['MAX_AUTO_CHARGE_CASHBACK_SUM'] = {'RUB': max_cashback_sum}

    orders1 = [
        (create_order(session, client, product_id=product_id), 2),
        (create_order(session, client, product_id=product_id), 2),
    ]
    request1 = create_request(session, client, orders=orders1)
    invoice1 = create_invoice(session, client, request_=request1)
    invoice1.turn_on_rows()

    orders2 = [
        (create_order(session, client, product_id=product_id), 1),
    ]
    request2 = create_request(session, client, orders=orders2)
    invoice2 = create_invoice(session, client, request_=request2)
    invoice2.turn_on_rows()

    extra_bonus_sum = 100000
    # initial_bonus = max_cashback_sum + extra_bonus_sum

    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_1 = create_cashback(client, bonus=max_cashback_sum/2, finish_dt=finish_dt)
    cashback_2 = create_cashback(client, bonus=max_cashback_sum/2 + extra_bonus_sum)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    check_operation(invoice1.consumes[0].reverses[0].operation)
    check_operation(invoice2.consumes[0].reverses[0].operation)
    assert cashback_1.bonus == 0
    assert cashback_2.bonus == DU(extra_bonus_sum, 'QTY')
    # second invoice is processed first
    hm.assert_that(
        invoice2.consumes,
        hm.contains(
            # initial consume is reversed
            hm.has_properties(current_sum=0, current_qty=0),
            # new consume with cashback
            hm.has_properties(current_sum=D("{}".format(1 * product_price)), current_qty=D('10000'), cashback_bonus=D('9999'))
        ),
    )
    hm.assert_that(
        invoice1.consumes,
        hm.contains(
            # 1st order: initial consume is reversed
            hm.has_properties(current_sum=0, current_qty=0),
            # 2nd order: initial consume is not reversed
            hm.has_properties(current_sum=D("{}".format(2 * product_price)), current_qty=D('2'), cashback_bonus=None),
            # 1st order: new consume with cashback
            hm.has_properties(current_sum=D("{}".format(1 * product_price)), current_qty=D('10000'), cashback_bonus=D('9999')),
            # 1st order: consume with remaining money
            hm.has_properties(current_sum=D("{}".format(1 * product_price)), current_qty=D('1'), cashback_bonus=None),
        ),
    )


def test_bonus_exceeding_limit_for_invoice_with_both_fish_and_rub_products(session, client):
    """
    Regression test for BALANCE-38018
    there is a limit for auto cashback charging and we check that no extra bonuses were added

    invoices for auto charging are sorted by id descending
    consumes of invoice are sorted by product id ascending
    """

    max_cashback_sum = 9999 + 9999 * cst.DIRECT_PRODUCT_PRICE + 9999 + 999.9
    session.config.__dict__['MAX_AUTO_CHARGE_CASHBACK_SUM'] = {'RUB': max_cashback_sum}

    orders1 = [
        (create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID), 1),
        (create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID), 1),
        (create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID), 1),
        (create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID), 1),
    ]
    request1 = create_request(session, client, orders=orders1)
    invoice1 = create_invoice(session, client, request_=request1)
    invoice1.turn_on_rows()

    orders2 = [
        (create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID), 1),
    ]
    request2 = create_request(session, client, orders=orders2)
    invoice2 = create_invoice(session, client, request_=request2)
    invoice2.turn_on_rows()

    extra_bonus_sum = 100000
    initial_bonus = max_cashback_sum + extra_bonus_sum
    cashback = create_cashback(client, bonus=initial_bonus)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    check_operation(invoice1.consumes[0].reverses[0].operation)
    check_operation(invoice2.consumes[0].reverses[0].operation)
    # check we use only max_cashback_sum bonus
    assert cashback.bonus == DU(extra_bonus_sum, 'QTY')
    # second invoice is processed first
    hm.assert_that(
        invoice2.consumes,
        hm.contains(
            # initial consume is reversed
            hm.has_properties(current_sum=0, current_qty=0),
            # new consume with cashback
            hm.has_properties(current_sum=D("1"), current_qty=D('10000'), cashback_bonus=D('9999'))
        ),
    )
    hm.assert_that(
        invoice1.consumes,
        hm.contains(
            # 1st order: initial consume is reversed
            hm.has_properties(current_sum=0, current_qty=0),
            # 2nd order: initial consume is reversed
            hm.has_properties(current_sum=0, current_qty=0),
            # 3rd order: initial consume is  reversed
            hm.has_properties(current_sum=0, current_qty=0),
            # 4th order: initial consume is not reversed
            hm.has_properties(current_sum=D("1"), current_qty=D('1'), cashback_bonus=None),

            # fish consume is processed first because its product id is smaller
            # 1st order: new consume with cashback
            hm.has_properties(current_sum=D("30"), current_qty=D('10000'), cashback_bonus=D('9999')),
            # 2nd order: new consume with cashback 2
            hm.has_properties(current_sum=D("1"), current_qty=D('10000'), cashback_bonus=D('9999')),
            # 3rd order: new consume with cashback 3
            hm.has_properties(current_sum=D("0.1"), current_qty=D('1000'), cashback_bonus=D('999.9')),
            # 3rd order: new consume with remaining money
            hm.has_properties(current_sum=D("0.9"), current_qty=D('0.9'), cashback_bonus=None),
        ),
    )


def test_cashback_in_disallowed_currency(session, client):
    """
    Regression test for BALANCE-38018
    cashbacks with currency which doesn't exist in config are not allowed to auto charge
    so we check that we skip them (no changes at all)
    """
    max_cashback_sum = 15000
    # allow only cashback in KZT
    session.config.__dict__['MAX_AUTO_CHARGE_CASHBACK_SUM'] = {'KZT': max_cashback_sum}

    # try to process cashback for order in RUB
    orders = [
        (create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID), 1),
        (create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID), 1),
    ]
    request = create_request(session, client, orders=orders)
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    initial_bonus = max_cashback_sum + 10
    cashback = create_cashback(client, bonus=initial_bonus)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    # check that nothing changes
    assert cashback.bonus == DU(initial_bonus, 'QTY')
    assert len(invoice.consumes) == 2
    hm.assert_that(
        invoice.consumes,
        hm.contains(
            # first initial consume
            hm.has_properties(current_sum=D('1'), current_qty=D('1'), cashback_bonus=None),
            # second initial consume
            hm.has_properties(current_sum=D('1'), current_qty=D('1'), cashback_bonus=None),
        ),
    )


def test_multiple_invoices(session):
    """
    We should process only invoices of specified service
    Other invoices shouldn't be touched
    """
    client = ob.ClientBuilder.construct(session)

    request1 = create_request(session, client,
                              orders=[(create_order(session, client=client, product_id=cst.DIRECT_PRODUCT_ID), 1)])
    invoice1 = create_invoice(session, client, request_=request1)
    request2 = create_request(session, client,
                              orders=[(create_order(session, client=client, product_id=cst.MARKET_FISH_PRODUCT_ID), 1)])
    invoice2 = create_invoice(session, client, request_=request2)
    invoice1.turn_on_rows()
    invoice2.turn_on_rows()

    bonus = 10000 * cst.DIRECT_PRODUCT_PRICE
    cashback = create_cashback(client, bonus=bonus)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)
    check_operation(invoice1.consumes[0].reverses[0].operation)
    assert cashback.bonus == DU('30', 'QTY')
    hm.assert_that(
        invoice1.consumes,
        hm.contains(
            # first consume is reversed
            hm.has_properties(current_sum=0, current_qty=0),
            # new consume with cashback
            hm.has_properties(current_sum=D('30'), current_qty=D('10000'), cashback_bonus=D('9999')),
        ),
    )
    assert len(invoice2.consumes) == 1


def test_not_enough_free_money_for_cashback_base(session):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session)

    orders = [
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), 50),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), 50),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), 150),
    ]
    request = create_request(session, agency, orders=orders)
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    bonus = 10000
    cashback = create_cashback(client, bonus=bonus)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    assert cashback.bonus == bonus
    assert len(invoice.consumes) == len(orders)
    for c in invoice.consumes:
        assert c.reverses == []


@pytest.mark.parametrize(
    'descr, initial_bonus, remaining_bonus, qtys_n_bonuses', [
        [
            "bonus too small to split it to consumes", D('0.01'), D('0.01'),
            [
                (D('150'), D('0')),
                (D('50'), D('0')),
                (D('150'), D('0'))
            ]
        ],
        [
            "bonus is distributed evenly", D('33409.23'), D('0'),
            [
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('14446.8128'), D('14318.2414')),
                (D('4815.6044'), D('4772.7472')),
                (D('14446.8128'), D('14318.2414')),
                (D('21.4286'), D('0')),
                (D('7.1428'), D('0')),
                (D('21.4286'), D('0'))
            ]
        ],
        [
            "bonus is charged sequentially", D('300') * 9999 - D('3.98'), D('0'),
            [
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('1500000'), D('1499850.0000')),
                (D('500000'), D('499950.0000')),
                (D('999996.02'), D('999896.0200')),
                (D('50'), D('0'))
            ]
        ],
        [
            "max bonus for cashback_base", D('300') * 9999, D('0'),
            [
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('1500000'), D('1499850.0000')),
                (D('500000'), D('499950.0000')),
                (D('1000000'), D('999900.0000')),
                (D('50'), D('0'))]],
        [
            "no cashback_base for extra bonus", D('3345924.94'), D('3345924.94') - D('300') * 9999,
            [
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('1500000'), D('1499850.0000')),
                (D('500000'), D('499950.0000')),
                (D('1000000'), D('999900.0000')),
                (D('50'), D('0'))]],
    ], ids=lambda x: x if isinstance(x, str) else " "
)
def test_auto_cashback_charging_for_agency_subclients(session, initial_bonus, remaining_bonus, descr, qtys_n_bonuses):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session)

    orders = [
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), 150),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), 50),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), 150),
    ]
    request = create_request(session, agency, orders=orders)
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    bonus = initial_bonus
    cashback = create_cashback(client, bonus=bonus)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    session.expire_all()

    assert cashback.bonus == remaining_bonus

    assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == 300 * (len(qtys_n_bonuses) > len(orders))

    hm.assert_that(
        invoice.consumes,
        hm.contains(*[
            hm.has_properties(
                current_qty=el[0],
                current_cashback_bonus=el[1],
                cashback_base=el[0] - el[1] if el[1] > 0 else None,
            ) for el in qtys_n_bonuses
        ])
    )

    assert sum(c.current_cashback_bonus for c in invoice.consumes) == initial_bonus - remaining_bonus


@pytest.mark.parametrize(
    'descr, initial_bonuses, remaining_bonuses, qtys_n_bonuses', [
        [
            "bonus too small to split it to consumes", [D('0.01'), D('0.01')], [D('0.01'), D('0.01')],
            [
                (D('150'), D('0')),
                (D('50'), D('0')),
                (D('150'), D('0'))
            ]
        ],
        [
            "bonus is distributed evenly", [D('16409.23'), D('17000')], [D('0'), D('0')],
            [
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('14446.8128'), D('14318.2414')),
                (D('2705.8394'), D('2681.7586')),
                (D('2109.765'), D('2090.9886')),
                (D('14446.8128'), D('14318.2414')),
                (D('21.4286'), D('0')),
                (D('7.1428'), D('0')),
                (D('21.4286'), D('0'))]],
        [
            "bonus is charged sequentially", [D('300') * 9, D('300') * 9990 - D('3.98')], [D('0'), D('0')],
            [
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('1500000'), D('1499850.0000')),
                (D('500000'), D('499950.0000')),
                (D('997295.7499'), D('997196.0200')),
                (D('2700.2701'), D('2700.0000')),
                (D('50'), D('0'))
            ]
        ],
        [
            "max bonus for cashback_base", [D('99.99'), D('300') * 9999 - D('99.99')], [D('0'), D('0')],
            [
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('1500000'), D('1499850.0000')),
                (D('500000'), D('499950.0000')),
                (D('999900'), D('999800.01')), (D('100'), D('99.99')),
                (D('50'), D('0'))]
        ],
        [
            "min bonus for cashback_base", [D('99.98'), D('300') * 9999 - D('99.98')], [D('99.98'), D('0')],
            [
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('1500000'), D('1499850.0000')),
                (D('500000'), D('499950.0000')),
                (D('999900.02'), D('999800.02')),
                (D('50'), D('0'))]
        ],
        [
            "no cashback_base for extra bonus", [D('3345824.94'), D('100')], [D('346225.9299'), D('0')],
            [
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('0'), D('0')),
                (D('100.0101'), D('100')),
                (D('1499899'), D('1499749.0101')),
                (D('500000'), D('499950.0000')),
                (D('1000000'), D('999900.0000')),
                (D('50'), D('0'))]],
    ], ids=lambda x: x if isinstance(x, str) else " "
)
def test_auto_cashback_charging_for_agency_subclients_several_cashbacks(session, initial_bonuses, remaining_bonuses, descr, qtys_n_bonuses):
    session.config.__dict__['MAX_AUTO_CHARGE_CASHBACK_SUM'] = {'RUB': '3000000'}

    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session)

    orders = [
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), 150),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), 50),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), 150),
    ]
    request = create_request(session, agency, orders=orders)
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    bonus, time_limit_bonus = initial_bonuses
    cashback = create_cashback(client, bonus=bonus)
    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_time_limit = create_cashback(client, bonus=time_limit_bonus, finish_dt=finish_dt)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    session.expire_all()

    assert cashback.bonus == remaining_bonuses[0]
    assert cashback_time_limit.bonus == remaining_bonuses[1]

    assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == 300 * (len(qtys_n_bonuses) > len(orders))

    hm.assert_that(
        invoice.consumes,
        hm.contains(*[
            hm.has_properties(
                current_qty=el[0],
                current_cashback_bonus=el[1],
                cashback_base=el[0] - el[1] if el[1] > 0 else None,
            ) for el in qtys_n_bonuses
        ])
    )
    assert sum(c.current_qty - c.current_cashback_bonus for c in invoice.consumes) == 350
    assert sum(c.current_cashback_bonus for c in invoice.consumes) == sum(initial_bonuses) - sum(remaining_bonuses)


@pytest.mark.parametrize(
    'descr, initial_bonus, remaining_bonus, order_sum, qtys_n_bonuses', [
        [
            "cashback base is less than minimal", D('33409.23'), D('33409.23'), D('298.79'),
            [
                (D('0.1'),    D('0')),  # no reversed consumes
                (D('0.1'),    D('0')),
                (D('0.1'),    D('0')),
                (D('0.1'),    D('0')),
                (D('0.1'),    D('0')),
                (D('0.1'),    D('0')),
                (D('0.1'),    D('0')),
                (D('0.1'),    D('0')),
                (D('0.1'),    D('0')),
                (D('0.1'),    D('0')),
                (D('298.79'), D('0')),
                (D('0.1'),    D('0')),
                (D('0.1'),    D('0'))
            ]
        ],
        [
            "minimal amount for cashback base", D('33409.23'), D('0'), D('298.8'),
            [
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('0'),          D('0')),
                (D('11.2364'),    D('11.1364')),
                (D('11.2364'),    D('11.1364')),
                (D('11.2364'),    D('11.1364')),
                (D('11.2364'),    D('11.1364')),
                (D('11.2365'),    D('11.1365')),
                (D('11.2364'),    D('11.1364')),
                (D('11.2364'),    D('11.1364')),
                (D('11.2364'),    D('11.1364')),
                (D('11.2364'),    D('11.1364')),
                (D('11.2364'),    D('11.1364')),
                (D('33574.3931'), D('33275.5931')),
                (D('11.2364'),    D('11.1364')),
                (D('11.2364'),    D('11.1364'))
            ]
        ],
        [
            "cancers test", D('100'), D('0'), D('298.8'),
            [
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0'),      D('0')),
                (D('0.1333'), D('0.0333')),
                (D('0.1334'), D('0.0334')),
                (D('0.1333'), D('0.0333')),
                (D('0.1333'), D('0.0333')),
                (D('0.1334'), D('0.0334')),
                (D('0.1333'), D('0.0333')),
                (D('0.1333'), D('0.0333')),
                (D('0.1334'), D('0.0334')),
                (D('0.1333'), D('0.0333')),
                (D('0.1333'), D('0.0333')),
                (D('398.4'),  D('99.6000')),
                (D('0.1334'), D('0.0334')),
                (D('0.1333'), D('0.0333'))
            ]
        ],
    ], ids=lambda x: x if isinstance(x, str) else " "
)
def test_small_agency_orders(session, initial_bonus, remaining_bonus, order_sum, descr, qtys_n_bonuses):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session)

    orders = [
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), D('0.1'))
        for _ in range(10)
    ] + [
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), D(order_sum)),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), D('0.1')),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID), D('0.1'))
    ]
    request = create_request(session, agency, orders=orders)
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    bonus = initial_bonus
    cashback = create_cashback(client, bonus=bonus)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    session.expire_all()

    assert cashback.bonus == remaining_bonus
    assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == 300 * (len(qtys_n_bonuses) > len(orders))

    hm.assert_that(
        invoice.consumes,
        hm.contains(*[
            hm.has_properties(
                current_qty=el[0],
                current_cashback_bonus=el[1],
                cashback_base=el[0] - el[1] if el[1] > 0 else None,
            ) for el in qtys_n_bonuses
        ])
    )

    assert sum(c.current_cashback_bonus for c in invoice.consumes) == initial_bonus - remaining_bonus


@pytest.mark.parametrize(
    'descr, initial_bonus, remaining_bonus, order_sum, qtys_n_bonuses', [
        [
            "cashback base is less than minimal", D('33409.23'), D('33409.23'), D('8.7'),
            [
                (D('0.1'), D('0')),
                (D('0.1'), D('0')),
                (D('0.1'), D('0')),
                (D('0.1'), D('0')),
                (D('0.1'), D('0')),
                (D('0.1'), D('0')),
                (D('0.1'), D('0')),
                (D('0.1'), D('0')),
                (D('0.1'), D('0')),
                (D('0.1'), D('0')),
                (D('8.7'), D('0')),
                (D('0.1'), D('0')),
                (D('0.1'), D('0'))
            ]
        ],
        [
            "minimal amount for cashback base", D('33409.23'), D('0'), D('8.8'),
            [
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410')),
                (D('988.80408'), D('980.004080')),
                (D('11.23641'),  D('11.136410')),
                (D('11.23641'),  D('11.136410'))
            ]
        ],
        [
            "cancers test", D('100'), D('0.00001'), D('8.8'),
            [
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0.133333'),  D('0.033333')),
                (D('0.133334'),  D('0.033334')),
                (D('0.133333'),  D('0.033333')),
                (D('0.133333'),  D('0.033333')),
                (D('0.133334'),  D('0.033334')),
                (D('0.133333'),  D('0.033333')),
                (D('0.133333'),  D('0.033333')),
                (D('0.133334'),  D('0.033334')),
                (D('0.133333'),  D('0.033333')),
                (D('0.133333'),  D('0.033333')),
                (D('11.733333'), D('2.933333')),
                (D('0.133334'),  D('0.033334')),
                (D('0.133333'),  D('0.033333'))
            ]
        ],
    ], ids=lambda x: x if isinstance(x, str) else " "
)
def test_fish_product(session, initial_bonus, remaining_bonus, order_sum, descr, qtys_n_bonuses):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session)

    orders = [
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID), D('0.1'))
        for _ in range(10)
    ] + [
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID),
        D(order_sum)),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID),
        D('0.1')),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID),
        D('0.1'))
    ]
    request = create_request(session, agency, orders=orders)
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    bonus = initial_bonus
    cashback = create_cashback(client, bonus=bonus)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    session.expire_all()

    assert cashback.bonus == remaining_bonus
    assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == 300 / cst.DIRECT_PRODUCT_PRICE * (len(qtys_n_bonuses) > len(orders))

    hm.assert_that(
        invoice.consumes,
        hm.contains(*[
            hm.has_properties(
                current_qty=el[0],
                current_cashback_bonus=el[1],
                cashback_base=el[0] - el[1] if el[1] > 0 else None,
            ) for el in qtys_n_bonuses
        ])
    )

    assert sum(c.current_cashback_bonus * 30 for c in invoice.consumes) == initial_bonus - remaining_bonus


@pytest.mark.parametrize(
    'initial_bonuses, remaining_bonuses, qtys_n_bonuses', [
        [
            [D('80'), D('20')],
            [D('0.00002'), D('0.00002')],
            [
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('4.166666'),  D('0.666666')),
                (D('5.166667'),  D('1.666667')),
                (D('2.666667'),  D('0.666667')),
                (D('1.333332'),  D('0.333332')),
            ]
        ],
    ]
)
def test_fish_product_several_cashbacks(session, initial_bonuses, remaining_bonuses, qtys_n_bonuses):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session)

    orders = [
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID), D('7')),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID), D('2')),
        (create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID), D('1'))
    ]
    request = create_request(session, agency, orders=orders)
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    bonus, time_limit_bonus = initial_bonuses
    cashback = create_cashback(client, bonus=bonus)
    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_time_limit = create_cashback(client, bonus=time_limit_bonus, finish_dt=finish_dt)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    session.expire_all()

    assert cashback.bonus == remaining_bonuses[0]
    assert cashback_time_limit.bonus == remaining_bonuses[1]

    assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == 300 / cst.DIRECT_PRODUCT_PRICE * (len(qtys_n_bonuses) > len(orders))

    hm.assert_that(
        invoice.consumes,
        hm.contains(*[
            hm.has_properties(
                current_qty=el[0],
                current_cashback_bonus=el[1],
                cashback_base=el[0] - el[1] if el[1] > 0 else None,
            ) for el in qtys_n_bonuses
        ])
    )
    assert sum(c.current_qty - c.current_cashback_bonus for c in invoice.consumes) == 10
    assert sum(c.current_cashback_bonus * 30 for c in invoice.consumes) == sum(initial_bonuses) - sum(remaining_bonuses)


@pytest.mark.parametrize(
    'initial_bonuses, remaining_bonuses, qtys_n_bonuses', [
        [
            [D('80'), D('20')],
            [D('0.00002'), D('0.00002')],
            [
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('5.566664'),  D('0.666666')),
                (D('6.566665'),  D('1.666667')),
                (D('3.466666'),  D('0.666667')),
                (D('1.733331'),  D('0.333332')),
            ]
        ],
    ]
)
def test_fish_product_several_cashbacks_discount(session, initial_bonuses, remaining_bonuses, qtys_n_bonuses):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session)

    order_1 = create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID)
    order_2 = create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID)
    order_3 = create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID)
    orders = [order_1, order_2, order_3]

    request = create_request(
        session,
        agency,
        [
            (order_1, D('7')),
            (order_2, D('2')),
            (order_3, D('1'))
        ])
    invoice = create_invoice(session, client, request_=request)
    reserve_promo_code(agency, bonus=100)

    with mock.patch('balance.actions.promocodes.checker.PromoCodeChecker.do_checks'):
        invoice.turn_on_rows(apply_promocode=True)

    promocode_pct = invoice.consumes[0].discount_obj.promo_code_pct
    assert promocode_pct == D('28.5714')

    assert sum([co.current_sum for order in orders for co in order.consumes]) == D('300')
    assert sum([co.current_qty * D('30') for order in orders for co in order.consumes]) == D('419.999820')

    bonus, time_limit_bonus = initial_bonuses
    cashback = create_cashback(client, bonus=bonus)
    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_time_limit = create_cashback(client, bonus=time_limit_bonus, finish_dt=finish_dt)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    session.expire_all()

    assert sum([co.current_sum for order in orders for co in order.consumes]) == D('300')
    assert sum([co.current_qty * D('30') for order in orders for co in order.consumes]) == D('519.999780')
    assert sum([co.current_cashback_bonus * D('30') for order in orders for co in order.consumes]) == D('99.99996')
    assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == D('13.999994')

    assert cashback.bonus == remaining_bonuses[0]
    assert cashback_time_limit.bonus == remaining_bonuses[1]

    hm.assert_that(
        invoice.consumes,
        hm.contains(*[
            hm.has_properties(
                current_qty=el[0],
                current_cashback_bonus=el[1],
                cashback_base=el[0] - el[1] if el[1] > 0 else None,
            ) for el in qtys_n_bonuses
        ])
    )

    assert sum(c.current_qty - c.current_cashback_bonus for c in invoice.consumes) == D('13.999994')
    assert sum(c.current_cashback_bonus * 30 for c in invoice.consumes) == sum(initial_bonuses) - sum(remaining_bonuses)


@pytest.mark.parametrize(
    'initial_bonuses, qtys_n_bonuses', [
        [
            [D('80'), D('20')],
            [
                (D('0'),         D('0')),
                (D('0'),         D('0')),
                (D('7'),         D('0')),
                (D('2'),         D('0')),
            ]
        ],
    ]
)
def test_fish_product_several_cashbacks_discount_not_enough_sum(session, initial_bonuses, qtys_n_bonuses):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session)

    order_1 = create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID)
    order_2 = create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_ID)
    orders = [order_1, order_2]

    request = create_request(
        session,
        agency,
        [
            (order_1, D('7')),
            (order_2, D('2')),
        ])
    invoice = create_invoice(session, client, request_=request)
    reserve_promo_code(agency, bonus=100)

    with mock.patch('balance.actions.promocodes.checker.PromoCodeChecker.do_checks'):
        invoice.turn_on_rows(apply_promocode=True)

    promocode_pct = invoice.consumes[0].discount_obj.promo_code_pct
    assert promocode_pct == D('30.7692')

    assert sum([co.current_sum for order in orders for co in order.consumes]) == D('270')
    assert sum([co.current_qty * D('30') for order in orders for co in order.consumes]) == D('389.99985')

    bonus, time_limit_bonus = initial_bonuses
    cashback = create_cashback(client, bonus=bonus)
    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_time_limit = create_cashback(client, bonus=time_limit_bonus, finish_dt=finish_dt)
    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)

    session.expire_all()

    assert sum([co.current_sum for order in orders for co in order.consumes]) == D('270')
    assert sum([co.current_qty * D('30') for order in orders for co in order.consumes]) == D('389.99985')
    assert sum([co.current_cashback_bonus for order in orders for co in order.consumes]) == D('0')
    assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == D('0')

    assert cashback.bonus == initial_bonuses[0]
    assert cashback_time_limit.bonus == initial_bonuses[1]


@pytest.mark.parametrize(
    'w_agency',
    [
        True,
        False,
    ],
)
def test_many_small_consumes(session, w_agency):
    if w_agency:
        session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = [{'iso_currency': 'RUB', 'amount_w_cashback': '0.1'}]
        agency = ob.ClientBuilder.construct(session, is_agency=1)
    else:
        agency = None
    client = ob.ClientBuilder.construct(session)

    orders = [create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID) for _i in range(10)]

    request = create_request(
        session,
        agency or client,
        [(o, D('0.01')) for o in orders],
    )
    invoice = create_invoice(session, agency or client, request_=request)
    invoice.turn_on_rows()

    bonus, time_limit_bonus = (70, 30)
    cashback = create_cashback(client, bonus=bonus)
    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_time_limit = create_cashback(client, bonus=time_limit_bonus, finish_dt=finish_dt)

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)
    session.expire_all()

    if w_agency:
        assert cashback.bonus == 0
        assert cashback_time_limit.bonus == 0

        assert sum([co.current_sum for order in orders for co in order.consumes]) == D('0.1')
        assert sum([co.current_qty for order in orders for co in order.consumes]) == D('100.1')
        assert sum([co.current_cashback_bonus for order in orders for co in order.consumes]) == D('100')
        assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == D('0.1')
    else:
        # так работает, потому что мы пытаемся зачислить 2 кешбэка на конзюм в копейку
        # но побить такой конзюм на части не можем, поэтому зачисляем только первый кешбэк
        assert cashback.bonus == D('69.99')
        assert cashback_time_limit.bonus == 0

        assert sum([co.current_sum for order in orders for co in order.consumes]) == D('0.1')
        assert sum([co.current_qty for order in orders for co in order.consumes]) == D('30.11')
        assert sum([co.current_cashback_bonus for order in orders for co in order.consumes]) == D('30.01')
        assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == D('0.02')


@pytest.mark.parametrize(
    'w_agency',
    [
        True,
        False,
    ],
)
def test_many_small_cashbacks(session, w_agency):
    if w_agency:
        session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = [{'iso_currency': 'RUB', 'amount_w_cashback': '0.1'}]
        agency = ob.ClientBuilder.construct(session, is_agency=1)
    else:
        agency = None
    client = ob.ClientBuilder.construct(session)

    orders = [create_order(session, client=client, agency=agency, product_id=cst.DIRECT_PRODUCT_RUB_ID)]

    request = create_request(
        session,
        agency or client,
        [(o, D('0.1')) for o in orders],
    )
    invoice = create_invoice(session, agency or client, request_=request)
    invoice.turn_on_rows()

    cashbacks = []
    for dt_delta in range(1, 11):
        finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=dt_delta)
        cashbacks.append(create_cashback(client, bonus=D('0.01'), finish_dt=finish_dt))

    auto_charge.auto_charge_client(client, cst.DIRECT_SERVICE_ID)
    session.expire_all()

    assert sum(c.bonus for c in cashbacks) == D('0')

    assert sum([co.current_sum for order in orders for co in order.consumes]) == D('0.1')
    assert sum([co.current_qty for order in orders for co in order.consumes]) == D('0.2')
    assert sum([co.current_cashback_bonus for order in orders for co in order.consumes]) == D('0.1')
    assert sum(c.discount_obj.cashback_base for c in invoice.consumes) == D('0.1')

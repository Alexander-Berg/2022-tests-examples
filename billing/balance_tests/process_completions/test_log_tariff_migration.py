# -*- coding: utf-8 -*-
import pytest
import hamcrest as hm
import mock
import itertools
import datetime
from decimal import Decimal as D

from balance import (
    constants as cst,
    exc,
    mapper,
    muzzle_util as ut,
)
import balance.actions.unified_account as a_ua
from balance.actions.process_completions.tariff_migration import ProcessTariffMigration

from tests import object_builder as ob
from tests.balance_tests.process_completions.common import (
    create_orders,
    consume_order,
    mk_shipment,
    assert_consumes,
    calc_log_tariff_state,
    migrate_client,
    LogExceptionInterceptHandler,
)

pytestmark = [
    pytest.mark.log_tariff,
]

TASK_TYPE = 'test_migration_%s' % ob.get_big_number()
RUB_CURRENCY_ID = 643
BYN_CURRENCY_ID = 933
FISH_CURRENCY_ID = 0


@pytest.fixture(name='task_type')
def create_task_type(session):
    return ob.LogTariffTypeBuilder.construct(session, id=TASK_TYPE)


@pytest.fixture(name='task')
def create_task(session, task_type):
    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=cst.NirvanaProcessingTaskState.IN_PROGRESS,
    )
    return task


def create_migration_order(session, task, order):
    return ob.LogTariffMigrationOrderBuilder.construct(
        session,
        task=task,
        order=order,
    )


def create_input(session, task, migration_order_id, order, qty=666, currency_id=RUB_CURRENCY_ID, tariff_dt=None):
    return ob.LogTariffMigrationInputBuilder.construct(
        session,
        task_id=task.id,
        migration_order_id=migration_order_id,
        service_id=order.service_id,
        service_order_id=order.service_order_id,
        tariff_dt=tariff_dt or session.now(),
        completion_qty_delta=qty,
        currency_id=currency_id,
    )


def assert_migration_consumes(task, migration_order, consumes, res_consumes):
    if consumes:
        consumes_match = []
        for co, vals in zip(consumes, res_consumes):
            if vals is not None:
                qty, sum_, co_qty, co_sum = vals
                consumes_match.append(
                    hm.has_properties(
                        task_id=task.id,
                        consume_id=co.id,
                        product_id=co.order.service_code,
                        service_id=co.order.service_id,
                        service_order_id=co.order.service_order_id,
                        tariff_dt=co.order.shipment_dt,
                        qty=qty,
                        sum=sum_,
                        consume_qty=co_qty,
                        consume_sum=co_sum,
                    )
                )
        consumes_match = hm.contains_inanyorder(*consumes_match)
    else:
        consumes_match = hm.empty()

    hm.assert_that(
        migration_order.tariff_consumes,
        consumes_match,
    )


def assert_migration_untariffed(task, migration_order, overcompletion, currency_id=RUB_CURRENCY_ID):
    if overcompletion:
        untariffed_match = hm.contains_inanyorder(*[
            hm.has_properties(
                task_id=task.id,
                service_id=order.service_id,
                service_order_id=order.service_order_id,
                tariff_dt=order.shipment_dt,
                currency_id=currency_id,
                product_id=order.service_code,
                overcompletion_qty=qty,
            )
            for order, qty in overcompletion
        ])
    else:
        untariffed_match = hm.empty()
    hm.assert_that(
        migration_order.tariff_untariffed,
        untariffed_match,
    )


def test_completions_multiple_orders(session, client, task):
    """Исторические открутки, полученные от init_migration, совпадают с теми, что у нас уже есть"""
    main_order, (o1, o2, o3) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_RUB_ID] * 3,
        turn_on_log_tariff=False
    )
    consume_order(main_order, [42])
    co_main, = main_order.consumes

    consume_order(o1, [666])
    mk_shipment(o1, 666, force=False)
    co1, = o1.consumes

    consume_order(o2, [50])
    mk_shipment(o2, 74, force=False)
    co2, = o2.consumes

    consume_order(o3, [100])
    mk_shipment(o3, 66, force=False)
    co3, = o3.consumes

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, o1, 666)
    create_input(session, task, migration_order.id, o2, 74)
    create_input(session, task, migration_order.id, o3, 66)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=24,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )
    assert main_order.shipment.money == D('24')
    assert_consumes(
        main_order,
        [
            (42, 42, 24, 24),
            (34, 34, 0, 0),
        ]
    )

    assert_migration_untariffed(task, migration_order, None)
    assert_migration_consumes(
        task,
        migration_order,
        [co_main, co1, co2, co3],
        [
            (24, 24, co_main.consume_qty, co_main.consume_sum),
            (666, 666, co1.consume_qty, co1.consume_sum),
            (50, 50, co2.consume_qty, co2.consume_sum),
            (66, 66, co3.consume_qty, co3.consume_sum),
        ]
    )


def test_completions_multiple_orders_w_new_shipments(session, client, task):
    main_order, (o1, o2) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_RUB_ID] * 2,
        turn_on_log_tariff=False
    )
    consume_order(main_order, [42])
    co_main, = main_order.consumes

    consume_order(o1, [666])
    mk_shipment(o1, 660, force=False)
    co1, = o1.consumes

    consume_order(o2, [50])
    mk_shipment(o2, 74, force=False)
    co2, = o2.consumes

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, o1, 666)  # докручиваем
    create_input(session, task, migration_order.id, o2, 100)  # откручиваем ещё сильнее

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=50,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )
    assert main_order.shipment.money == D('50')
    assert_consumes(
        main_order,
        [
            (42, 42, 42, 42),
        ]
    )
    assert co1.completion_qty == 666
    assert co2.completion_qty == 50

    assert_migration_untariffed(
        task,
        migration_order,
        [(main_order, 8)]
    )
    assert_migration_consumes(
        task,
        migration_order,
        [co_main, co1, co2],
        [
            (42, 42, co_main.consume_qty, co_main.consume_sum),
            (666, 666, co1.consume_qty, co1.consume_sum),
            (50, 50, co2.consume_qty, co2.consume_sum),
        ]
    )


@pytest.mark.parametrize(
    'params',
    [
        pytest.param(
            dict(
                main_consume_qty=10, main_completion_qty=5,
                child_consume_qty=20, child_completion_qty=25,
                input_qty=27, res_qty=7,
                res_consumes=[
                    (D(7), D(7), D(10), D(10)),
                    (D(20), D(20), D(20), D(20)),
                ],
            ),
            id='completions',
        ),
        pytest.param(
            dict(
                main_consume_qty=10, main_completion_qty=7,
                child_consume_qty=20, child_completion_qty=27,
                input_qty=25, res_qty=5,
                res_consumes=[
                    (D(5), D(5), D(10), D(10)),
                    (D(20), D(20), D(20), D(20)),
                ],
            ),
            id='rollback'
        ),
        pytest.param(
            dict(
                main_consume_qty=D('11.1111'), main_completion_qty=5,
                child_consume_qty=20, child_completion_qty=25,
                input_qty=37, res_qty=17,
                res_consumes=[
                    (D('11.1111'), D('11.11'), D('11.1111'), D('11.11')),
                    (D(20), D(20), D(20), D(20)),
                ],
                res_overcompletion_qty=D('5.8889'),
            ),
            id='overcompletion',
        ),
        pytest.param(
            dict(
                main_consume_qty=10, main_completion_qty=13,
                child_consume_qty=20, child_completion_qty=33,
                input_qty=37, res_qty=17,
                res_consumes=[
                    (D(10), D(10), D(10), D(10)),
                    (D(20), D(20), D(20), D(20)),
                ],
                res_overcompletion_qty=D(7),
            ),
            id='add_overcompletion',
        ),
        pytest.param(
            dict(
                main_consume_qty=D('12.2223'), main_completion_qty=D('7.0209'),
                child_consume_qty=10, child_completion_qty=D('17.0209'),
                input_qty=D('22.2222'), res_qty=D('12.2222'),
                res_consumes=[
                    (D('12.2222'), D('12.22'), D('12.2223'), D('12.22')),
                    (D(10), D(10), D(10), D(10)),
                ],
            ),
            id='precision',
        ),
    ]
)
def test_money(session, client, task, params):
    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_RUB_ID])
    consume_order(main_order, [params['main_consume_qty']])
    mk_shipment(main_order, params['main_completion_qty'])

    consume_order(child_order, [params['child_consume_qty']])
    mk_shipment(child_order, params['child_completion_qty'])

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, params['input_qty'])

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=params['res_qty'],
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )
    assert main_order.shipment.money == params['res_qty']
    assert_consumes(
        main_order,
        [
            (
                params['main_consume_qty'],
                ut.round00(params['main_consume_qty']),
                min(params['res_qty'], params['main_consume_qty']),
                ut.round00(min(params['res_qty'], params['main_consume_qty'])),
            ),
        ]
    )
    res_state = calc_log_tariff_state(main_order.consumes[0].id, [(main_order.consumes[-1], min(params['res_qty'], params['main_consume_qty']))])
    assert migration_order.state == res_state
    assert main_order.completion_consumed_money is None

    assert_migration_consumes(task, migration_order, itertools.chain(main_order.consumes, child_order.consumes), params['res_consumes'])
    res_overcompletion_qty = params.get('res_overcompletion_qty')
    assert_migration_untariffed(task, migration_order, [(main_order, res_overcompletion_qty)] if res_overcompletion_qty else None)


@pytest.mark.parametrize(
    'params',
    [
        pytest.param(
            dict(
                main_consume_qty=300, main_completion_qty=150,
                child_consume_qty=[None], child_bucks=[4], child_money=[30],
                input_vals=[(40, D('10.000001'))],
                res_qty=340,
                res_consumes=[
                    (300, 300, 300, 300),
                ],
                res_overcompletion_qty=40,
            ),
            id='fish_overcompletion',
        ),
        pytest.param(
            dict(
                main_consume_qty=66, main_completion_qty=30,
                child_consume_qty=[10], child_bucks=[9], child_money=[60],
                input_vals=[(90, 9)],
                res_qty=60,
                res_consumes=[
                    (60, 60, 66, 66),
                    (300, 300, 300, 300),
                ],
            ),
            id='money_completions',
        ),
        pytest.param(
            dict(
                main_consume_qty=66, main_completion_qty=30,
                child_consume_qty=[10], child_bucks=[0], child_money=[60],
                input_vals=[(90, None)],
                res_qty=0,
                res_main_order_consumes=[
                    (66, 66, 0, 0),
                    (210, 210, 0, 0),
                ],
                res_consumes=[
                    (90, 90, 300, 300),
                ],
            ),
            id='money_completions_wo_bucks',
        ),
        pytest.param(
            dict(
                main_consume_qty=66, main_completion_qty=30,
                child_consume_qty=[10], child_bucks=[9], child_money=[60],
                input_vals=[(45, 9)],
                res_qty=15,
                res_consumes=[
                    (15, 15, 66, 66),
                    (300, 300, 300, 300),
                ],
            ),
            id='money_rollback',
        ),
        pytest.param(
            dict(
                main_consume_qty=200, main_completion_qty=30,
                child_consume_qty=[10], child_bucks=[9], child_money=[60],
                input_vals=[(60, 10)],
                res_qty=60,
                res_consumes=[
                    (60, 60, 200, 200),
                    (300, 300, 300, 300),
                ],
            ),
            id='fish_completions',
        ),
        pytest.param(
            dict(
                main_consume_qty=200, main_completion_qty=60,
                child_consume_qty=[10], child_bucks=[8], child_money=[120],
                input_vals=[(1, 10)],
                res_qty=1,
                res_consumes=[
                    (1, 1, 200, 200),
                    (300, 300, 300, 300),
                ],
            ),
            id='fish_complete_money_rollback',
        ),
        pytest.param(
            dict(
                main_consume_qty=200, main_completion_qty=60,
                child_consume_qty=[10], child_bucks=[8], child_money=[120],
                input_vals=[(61, 8)],
                res_qty=1,
                res_consumes=[
                    (1, 1, 200, 200),
                    (300, 300, 300, 300),
                ],
            ),
            id='fish_money_rollback',
        ),
        pytest.param(
            dict(
                main_consume_qty=200, main_completion_qty=30,
                child_consume_qty=[10], child_bucks=[10], child_money=[30],
                input_vals=[(263, 8)],
                res_qty=203,
                res_consumes=[
                    (200, 200, 200, 200),
                    (300, 300, 300, 300),
                ],
                res_overcompletion_qty=3,
            ),
            id='fish_rollback_money_overcomplete',
        ),
        pytest.param(
            dict(
                main_consume_qty=200, main_completion_qty=D('13.4222'),
                child_consume_qty=[10], child_bucks=[D('2.98674')], child_money=[D('1.002')],
                input_vals=[(D('52.0429'), D('9.080685'))],
                res_qty=D('24.4634'),
                res_consumes=[
                    (D('24.4634'), D('24.46'), 200, 200),
                    (300, 300, 300, 300),
                ],
            ),
            id='precision',
        ),
        pytest.param(
            dict(
                main_consume_qty=200, main_completion_qty=10,
                child_consume_qty=[1] * 10, child_bucks=[1] * 10, child_money=[1] * 10,
                task_bucks=D('10.00004'), task_money=D('0.0011'),
                input_vals=[(D('0.5979'), D('0.989999'))] * 10,
                res_qty=D('2.9787'),
                res_consumes=[(D('2.9787'), D('2.98'), 200, 200)] + [(30, 30, 30, 30)] * 10,
            ),
            id='precision_multiple_children',
        ),
    ]
)
def test_main_money_child_bucks(session, client, task, params):
    children_count = len(params['child_consume_qty'])
    main_order, children = create_orders(client, cst.DIRECT_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_ID] * children_count)
    consume_order(main_order, [params['main_consume_qty']])
    mk_shipment(main_order, params['main_completion_qty'])
    main_consume, = main_order.consumes

    children_params = zip(children, params['child_consume_qty'], params['child_bucks'], params['child_money'])
    for child_order, qty, bucks, money in children_params:
        if qty is not None:
            consume_order(child_order, [qty])
        mk_shipment(child_order, bucks, money=money)

    migration_order = create_migration_order(session, task, main_order)
    for child_order, (money, bucks) in zip(children, params['input_vals']):
        create_input(session, task, migration_order.id, child_order, money)
        if bucks is not None:
            create_input(session, task, migration_order.id, child_order, bucks, currency_id=FISH_CURRENCY_ID)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    consume_compl_qty = min(params['res_qty'], params['main_consume_qty'])
    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED

    assert main_order.completion_qty == params['res_qty']
    assert main_order.shipment.money == params['res_qty']
    assert migration_order.state == calc_log_tariff_state(main_consume.id, [(main_consume, consume_compl_qty)] if consume_compl_qty else [])
    assert main_order.completion_consumed_money is None

    main_order_consumes_match = params.get(
        'res_main_order_consumes',
        [
            (
                params['main_consume_qty'],
                ut.round00(params['main_consume_qty']),
                consume_compl_qty,
                ut.round00(consume_compl_qty)
            ),
        ]
    )
    assert_consumes(main_order, main_order_consumes_match)

    consumes = itertools.chain(main_order.consumes, *[ch_order.consumes for ch_order in children])
    consumes = filter(lambda x: x.completion_qty, consumes)
    assert_migration_consumes(task, migration_order, consumes, params['res_consumes'])
    res_overcompletion_qty = params.get('res_overcompletion_qty')
    assert_migration_untariffed(task, migration_order, [(main_order, res_overcompletion_qty)] if res_overcompletion_qty else None)


@pytest.mark.parametrize(
    'params',
    [
        pytest.param(
            dict(
                main_consume_qty=10, main_bucks=11, main_money=30,
                child_consume_qty=[None], child_completion_qty=[360],
                completion_fixed_qty=11,
                input_vals=[360],
                res_qty=12,
                res_consumed_money=0,
                res_money=30,
                res_consumes=[
                    (300, 300, 300, 300),
                ],
                res_overcompletion_qty=60,
            ),
            id='overcompletion',
        ),
        pytest.param(
            dict(
                main_consume_qty=10, main_bucks=0, main_money=60,
                child_consume_qty=[10], child_completion_qty=[70],
                input_vals=[100],
                res_qty=3,
                res_consumed_money=90,
                res_money=90,
                res_consumes=[
                    (90, 90, 300, 300),
                    (10, 10, 10, 10),
                ],
            ),
            id='init_money_completions',
        ),
        pytest.param(
            dict(
                main_consume_qty=10, main_bucks=0, main_money=60,
                child_consume_qty=[10], child_completion_qty=[70],
                input_vals=[40],
                res_qty=1,
                res_consumed_money=30,
                res_money=30,
                res_consumes=[
                    (30, 30, 300, 300),
                    (10, 10, 10, 10),
                ],
            ),
            id='init_money_rollback',
        ),
        pytest.param(
            dict(
                main_consume_qty=10, main_bucks=1, main_money=30,
                child_consume_qty=[10], child_completion_qty=[70],
                input_vals=[100],
                res_qty=3,
                res_consumed_money=60,
                res_money=60,
                res_consumes=[
                    (90, 90, 300, 300),
                    (10, 10, 10, 10),
                ],
            ),
            id='init_bucks_completions',
        ),
        pytest.param(
            dict(
                main_consume_qty=10, main_bucks=1, main_money=90,
                child_consume_qty=[10], child_completion_qty=[100],
                input_vals=[70],
                res_qty=2,
                res_consumed_money=30,
                res_money=30,
                res_consumes=[
                    (60, 60, 300, 300),
                    (10, 10, 10, 10),
                ],
            ),
            id='init_bucks_rollback',
        ),
        pytest.param(
            dict(
                main_consume_qty=10, main_bucks=1, main_money=D('10.0010'),
                child_consume_qty=[1] * 10, child_completion_qty=[1] * 10,
                input_vals=[D('5.00011')] * 10,
                res_qty=D('1.33337'),
                res_consumed_money=D('10.0011'),
                res_money=D('10.0011'),
                res_consumes=[(D('40.0011'), D('40'), 300, 300)] + [(1, 1, 1, 1)] * 10,
            ),
            id='precision',
        ),
    ]
)
def test_main_bucks_child_money(session, client, params, task):
    main_qty = params['main_consume_qty']
    children_count = len(params['child_consume_qty'])

    main_order, children = create_orders(client, cst.DIRECT_PRODUCT_ID, [cst.DIRECT_PRODUCT_RUB_ID] * children_count)
    consume_order(main_order, [main_qty])
    main_consume, = main_order.consumes
    mk_shipment(main_order, params['main_bucks'], money=params['main_money'])
    if 'completion_fixed_qty' in params:
        main_order.completion_fixed_qty = params['completion_fixed_qty']

    children_params = zip(children, params['child_consume_qty'], params['child_completion_qty'])
    for child_order, consume_qty, completion_qty in children_params:
        if consume_qty is not None:
            consume_order(child_order, [consume_qty])
        mk_shipment(child_order, completion_qty)

    migration_order = create_migration_order(session, task, main_order)
    for child_order, money in zip(children, params['input_vals']):
        create_input(session, task, migration_order.id, child_order, money)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    consume_compl_qty = min(params['res_qty'], params['main_consume_qty'])
    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.completion_qty == params['res_qty']
    assert main_order.completion_consumed_money == params['res_consumed_money']
    assert migration_order.state == calc_log_tariff_state(main_consume.id, [(main_consume, consume_compl_qty)])
    hm.assert_that(
        main_order.shipment,
        hm.has_properties(
            money=params['res_money'],
            bucks=params['main_bucks'],
        )
    )
    main_money = ut.round00(main_qty * 30)
    assert_consumes(
        main_order,
        [
            (main_qty, main_money, min(params['res_qty'], main_qty), min(ut.round00(params['res_qty'] * 30), main_money)),
        ]
    )

    consumes = itertools.chain(main_order.consumes, *[o.consumes for o in children])
    assert_migration_consumes(task, migration_order, consumes, params['res_consumes'])
    ov_qty = params.get('res_overcompletion_qty')
    assert_migration_untariffed(task, migration_order, [(main_order, ov_qty)] if ov_qty else None)


def test_main_money_mixed(session, client, task):
    main_order, (child_bucks, child_money) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_ID, cst.DIRECT_PRODUCT_RUB_ID]
    )
    consume_order(main_order, [666])
    co_main, = main_order.consumes

    consume_order(child_bucks, [D('1.111112')])
    mk_shipment(child_bucks, D('0.000009'), money=D('0.0009'))
    co_bucks, = child_bucks.consumes

    consume_order(child_money, [1])
    mk_shipment(child_money, D('1.1234'))
    co_money, = child_money.consumes

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_bucks, D('1.111111'), currency_id=FISH_CURRENCY_ID)
    create_input(session, task, migration_order.id, child_money, D('2.2346'))

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.completion_consumed_money is None
    assert migration_order.state == calc_log_tariff_state(co_main.id, [(co_main, D('1.2355'))])

    assert main_order.completion_qty == D('1.2355')
    assert main_order.shipment.money == D('1.2355')

    assert_consumes(main_order, [(666, 666, D('1.2355'), D('1.24'))])
    assert_migration_consumes(
        task, migration_order,
        [co_main, co_bucks, co_money],
        [
            (D('1.2355'), D('1.24'), 666, 666),
            (D('33.33336'), D('33.33'), D('33.33336'), D('33.33')),
            (1, 1, 1, 1),
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_main_bucks_mixed(session, client, task):
    main_order, (child_bucks, child_money) = create_orders(
        client,
        cst.DIRECT_PRODUCT_ID,
        [cst.DIRECT_PRODUCT_ID, cst.DIRECT_PRODUCT_RUB_ID]
    )
    consume_order(main_order, [666])
    mk_shipment(main_order, D('0.100101'), money=D('1.5648'))
    co_main, = main_order.consumes

    consume_order(child_bucks, [1])
    mk_shipment(child_bucks, D('0.0001'))
    co_bucks, = child_bucks.consumes

    consume_order(child_money, [1])
    mk_shipment(child_money, D('1.1234'))
    co_money, = child_money.consumes

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_bucks, D('0.0001'), currency_id=FISH_CURRENCY_ID)
    create_input(session, task, migration_order.id, child_bucks, D('29.1111'))
    create_input(session, task, migration_order.id, child_money, D('638.0256'))

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.completion_consumed_money == D('634.02258')
    assert migration_order.state == calc_log_tariff_state(co_main.id, [(co_main, '21.234187')])

    assert main_order.completion_qty == D('21.234187')
    hm.assert_that(
        main_order.shipment,
        hm.has_properties(
            money=D('634.02258'),
            bucks=D('0.100101'),
        )
    )

    assert_consumes(
        main_order,
        [
            (666, 666 * 30, D('21.234187'), D('637.03')),
            (D('0.02953'), D('0.89'), 0, 0),
        ],
    )
    assert_migration_consumes(
        task, migration_order,
        [co_main, co_money, co_bucks],
        [
            (D('637.02561'), D('637.03'), 19980, 19980),
            (1, 1, 1, 1),
            (D('29.1141'), D('29.11'), 30, 30),
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_main_bucks_mixed_overcompletion(session, client, task):
    """Перекрут в рублях, так что можно"""
    main_order, (child_bucks, child_money) = create_orders(
        client,
        cst.DIRECT_PRODUCT_ID,
        [cst.DIRECT_PRODUCT_ID, cst.DIRECT_PRODUCT_RUB_ID]
    )
    consume_order(main_order, [5])
    mk_shipment(main_order, D('4.111111'), money=10)
    co_main, = main_order.consumes

    consume_order(child_bucks, [1])
    mk_shipment(child_bucks, D('0.4'))
    co_bucks, = child_bucks.consumes

    consume_order(child_money, [D('1.0001')])
    mk_shipment(child_money, 6)
    co_money, = child_money.consumes

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_bucks, D('0.9'), currency_id=FISH_CURRENCY_ID)
    create_input(session, task, migration_order.id, child_bucks, D('3'))
    create_input(session, task, migration_order.id, child_money, D('170.1111'))

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=D('5.637033'),
            completion_consumed_money=D('26.66667'),
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
            shipment=hm.has_properties(
                money=D('45.77766'),
                bucks=D('4.111111')
            )
        )
    )
    assert migration_order.state == calc_log_tariff_state(co_main.id, [(co_main, D('5'))])

    assert_consumes(main_order, [(5, 150, D('5'), D('150'))])
    assert_migration_consumes(
        task, migration_order,
        [co_main, co_bucks, co_money],
        [
            (D('150'), D('150'), 150, 150),
            (30, 30, 30, 30),
            (D('1.0001'), D('1'), D('1.0001'), D('1')),
        ],
    )
    assert_migration_untariffed(task, migration_order, [(main_order, D('19.110990'))])


def test_copy_migration(session, task):
    client = ob.ClientBuilder.construct(session)
    migrate_client(client, 'RUB', cst.CONVERT_TYPE_COPY)

    main_order = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=cst.DIRECT_PRODUCT_RUB_ID,
        main_order=1,
        is_ua_optimize=1,
        _is_log_tariff=cst.OrderLogTariffState.INIT,
    )

    main_order_bucks = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=cst.DIRECT_PRODUCT_ID,
        parent_group_order=main_order
    )
    child_order = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=cst.DIRECT_PRODUCT_RUB_ID,
        parent_group_order=main_order
    )
    child_order_bucks = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=cst.DIRECT_PRODUCT_ID,
        parent_group_order=child_order
    )

    consume_order(main_order, [100])
    mk_shipment(main_order, 11)
    co_main, = main_order.consumes

    consume_order(child_order, [20])
    mk_shipment(child_order, 31)

    consume_order(child_order_bucks, [1])
    mk_shipment(child_order_bucks, 1)

    consume_order(main_order_bucks, [2])
    mk_shipment(main_order_bucks, 2)

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 31)
    create_input(session, task, migration_order.id, child_order_bucks, 1, currency_id=FISH_CURRENCY_ID)
    create_input(session, task, migration_order.id, main_order_bucks, 2, currency_id=FISH_CURRENCY_ID)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.completion_qty == 11
    assert main_order.shipment.money == 11
    assert migration_order.state == calc_log_tariff_state(co_main.id, [(co_main, D('11'))])

    assert_consumes(
        main_order,
        [
            (100, 100, 11, 11),
        ]
    )
    assert_migration_consumes(
        task, migration_order,
        itertools.chain([co_main], *[o.consumes for o in [main_order_bucks, child_order, child_order_bucks]]),
        [
            (11, 11, 100, 100),
            (60, 60, 60, 60),
            (20, 20, 20, 20),
            (30, 30, 30, 30),
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_copy_migration_overcompletion(session, task):
    """Не нужна перекрутка по заказам, котороые смигрированы копированием"""
    client = ob.ClientBuilder.construct(session)
    migrate_client(client, 'RUB', cst.CONVERT_TYPE_COPY)
    main_order, (child_order,) = create_orders(client, cst.DIRECT_MEDIA_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_ID])

    consume_order(main_order, [100])
    mk_shipment(main_order, 30)
    co_main, = main_order.consumes

    consume_order(child_order, [2])
    mk_shipment(child_order, 2, money=30)
    co_ch, = child_order.consumes

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 2, currency_id=FISH_CURRENCY_ID)
    create_input(session, task, migration_order.id, child_order, 110)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.completion_qty == 30
    assert (child_order.shipment.bucks, child_order.shipment.money) == (2, 110)

    assert_migration_consumes(
        task, migration_order,
        [co_main, co_ch],
        [
            (30, 30, 100, 100),
            (60, 60, 60, 60),
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


@pytest.mark.parametrize(
    'params',
    [
        pytest.param(
            dict(
                input_qty=(2, 110),
                res_comp_qty=30,
                res_over=D('10.00001'),
            ),
            id='money',
        ),
        pytest.param(
            dict(
                input_qty=(10, 30),
                res_comp_qty=300,
                res_over=D('170.00001'),
            ),
            id='bucks',
        ),
    ],
)
def test_overcompletion_in_child(session, client, task, params):
    main_order, (child_order,) = create_orders(client, cst.DIRECT_MEDIA_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_ID])

    consume_order(main_order, [100])
    mk_shipment(main_order, 30)

    consume_order(child_order, [2])
    mk_shipment(child_order, 2, money=30)

    migration_order = create_migration_order(session, task, main_order)
    buck_qty, money_qty = params['input_qty']
    create_input(session, task, migration_order.id, child_order, buck_qty, currency_id=FISH_CURRENCY_ID)
    create_input(session, task, migration_order.id, child_order, money_qty)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    co_ch_1, co_ch_2 = child_order.consumes

    hm.assert_that(
        main_order,
        hm.has_properties(
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
            completion_qty=0,
            consume_qty=0
        )
    )
    assert (child_order.shipment.bucks, child_order.shipment.money) == params['input_qty']

    assert_migration_consumes(
        task, migration_order,
        [co_ch_1, co_ch_2],
        [
            (60, 60, 60, 60),
            (D('99.99999'), 100, D('99.99999'), 100),
        ],
    )
    assert_migration_untariffed(task, migration_order, [(child_order, params['res_over'])])


def test_init_currency_client(session, task):
    """Клиент мигрирован без типа, но оба заказа рублевых, так что можно всё"""
    client = ob.ClientBuilder.construct(session)
    migrate_client(client, 'RUB', convert_type=None)  # здесь не проблема, т.к. не фишки

    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_RUB_ID])

    consume_order(main_order, [12])
    mk_shipment(main_order, 11)
    co_main, = main_order.consumes

    consume_order(child_order, [20])
    mk_shipment(child_order, 31)
    co, = child_order.consumes

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 33)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.completion_qty == 13
    assert main_order.shipment.money == 13
    assert migration_order.state == calc_log_tariff_state(co_main.id, [(co_main, D('12'))])

    assert_migration_consumes(
        task, migration_order,
        [co_main, co],
        [
            (12, 12, 12, 12),
            (20, 20, 20, 20),
        ],
    )
    assert_migration_untariffed(task, migration_order, [(main_order, 1)])


def test_init_currency_client_child_bucks(session, task):
    """Клиент мигрирован без указания типа, один из дочерних заказов фишечный -> низзя так
    """
    client = ob.ClientBuilder.construct(session)
    migrate_client(client, 'RUB', convert_type=None)

    main_order, (child_order, child_order_bucks) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_RUB_ID, cst.DIRECT_PRODUCT_ID]
    )

    consume_order(main_order, [100])
    mk_shipment(main_order, 11)

    consume_order(child_order, [20])
    mk_shipment(child_order, 31)

    consume_order(child_order_bucks, [1])
    mk_shipment(child_order_bucks, 1)

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 33)
    create_input(session, task, migration_order.id, child_order_bucks, 1, currency_id=FISH_CURRENCY_ID)

    with LogExceptionInterceptHandler() as log_intercept:
        ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    msg = 'unexpected child order type in migration!'
    log_intercept.assert_exc(exc.LOG_TARIFF_CANT_PROCESS, msg)
    assert msg in migration_order.error
    session.refresh(migration_order)
    session.refresh(main_order)

    assert main_order._is_log_tariff == cst.OrderLogTariffState.INIT
    assert migration_order.state is None

    assert main_order.completion_qty == 11
    assert main_order.shipment.money == 11


def test_init_currency_client_main_order_bucks(session, task):
    """Клиент мигрирован без указания типа, ОС фишечный -> низзя так
    """
    client = ob.ClientBuilder.construct(session)
    migrate_client(client, 'RUB', convert_type=None)

    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_ID, [cst.DIRECT_PRODUCT_RUB_ID])
    consume_order(main_order, [100])
    mk_shipment(main_order, 11)

    consume_order(child_order, [20])
    mk_shipment(child_order, 31)

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 33)

    with LogExceptionInterceptHandler() as log_intercept:
        ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    msg = 'log tariff task %s for order %s is invalid: client currency doesn\'t match order: RUB != None' % (task.id, main_order.id)
    log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
    assert migration_order.error == msg


def test_old_orders_w_fish(session, task, client):
    """Клиент смигрирован на валюту, но заказ старый и он только в фишках.
    Не должны падать при таких условиях.
    """
    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_ID, [cst.DIRECT_PRODUCT_ID])

    consume_order(main_order, [30])
    mk_shipment(main_order, 1)

    consume_order(child_order, [30])
    mk_shipment(child_order, 1)
    co, = child_order.consumes

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 2, currency_id=FISH_CURRENCY_ID)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.completion_qty == 0
    assert main_order.shipment.money == 0

    assert_migration_consumes(
        task, migration_order,
        [co],
        [
            (60, 60, 900, 900),
        ],
    )
    assert_migration_untariffed(task, migration_order, [])


def test_mixed_ua(session, client, task):
    main_order, (child_order, child_order_media) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_RUB_ID, cst.DIRECT_MEDIA_PRODUCT_RUB_ID]
    )

    consume_order(main_order, [20])
    mk_shipment(main_order, 23)
    co_main, = main_order.consumes

    consume_order(child_order, [20])
    mk_shipment(child_order, 31)

    consume_order(child_order_media, [13])
    mk_shipment(child_order_media, 25)

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 49)  # открутка перенесется на ОС
    create_input(session, task, migration_order.id, child_order_media, 26)  # открутка останется на месте

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=29,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )
    assert main_order.shipment.money == 29
    assert main_order.completion_consumed_money is None
    assert migration_order.state == calc_log_tariff_state(co_main.id, [(co_main, D('20'))])
    assert_consumes(
        main_order,
        [
            (20, 20, 20, 20),
        ]
    )

    consumes = itertools.chain([co_main], *[o.consumes for o in [child_order, child_order_media]])
    assert_migration_consumes(
        task, migration_order,
        consumes,
        [
            (20, 20, 20, 20),
            (20, 20, 20, 20),
            (13, 13, 13, 13),
        ],
    )
    assert_migration_untariffed(task, migration_order, [(main_order, 9), (child_order_media, 13)])


def test_state_archive_consumes(session, client, task):
    main_order, (child_order,) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_RUB_ID],
        turn_on_log_tariff=False
    )
    consume_order(main_order, [10, 20, 30])
    mk_shipment(main_order, 24)
    co1, co2, co3 = main_order.consumes
    co1.invoice.generate_act(force=True)

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 31)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=31,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )
    assert main_order.shipment.money == 31
    assert migration_order.state == calc_log_tariff_state(co2.id, [(co2, 20), (co3, 1)])

    assert_migration_consumes(
        task, migration_order,
        [co2, co3],
        [
            (20, 20, 20, 20),
            (1, 1, 30, 30),
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_state_all_archive_consumes(session, client, task):
    main_order, (child_order,) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_RUB_ID],
        turn_on_log_tariff=False
    )
    consume_order(main_order, [10, 20])
    mk_shipment(main_order, 30)

    co1, co2, = main_order.consumes
    co1.invoice.generate_act(force=True)
    co2.invoice.generate_act(force=True)

    mk_shipment(main_order, 29)
    mk_shipment(child_order, 29)

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 30)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.shipment.money == 30
    assert migration_order.state == calc_log_tariff_state(co2.id, [(co2, 20)])

    assert_migration_consumes(
        task, migration_order,
        [], [],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_state_no_consumes(session, client, task):
    """У ОС нет конзюма, так что это всё перекрутка по ОС"""
    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_RUB_ID])
    mk_shipment(main_order, 14)
    consume_order(child_order, [10])
    mk_shipment(child_order, 24)

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 25)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    co_ch, = child_order.consumes

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.shipment.money == 15
    assert migration_order.state == calc_log_tariff_state(0, [])

    assert_migration_consumes(
        task, migration_order,
        [co_ch],
        [
            (10, 10, 10, 10),
        ],
    )
    assert_migration_untariffed(task, migration_order, [(main_order, 15)])


def test_state_no_consumes_no_completion(session, client, task):
    """Вообще нет конзюмов -> всё на перекрут"""
    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_RUB_ID])
    mk_shipment(main_order, 14)
    mk_shipment(child_order, 15)

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 16)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.shipment.money == 16
    assert migration_order.state == calc_log_tariff_state(0, [])

    assert_migration_consumes(
        task, migration_order,
        [], [],
    )
    assert_migration_untariffed(task, migration_order, [(main_order, 16)])


def test_rollback_into_children(session, client, task):
    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_RUB_ID])
    consume_order(main_order, [666])
    mk_shipment(main_order, 14)
    co_main, = main_order.consumes

    consume_order(child_order, [7])
    mk_shipment(child_order, 21)
    co_ch, = child_order.consumes

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 6)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.shipment.money == 0
    assert child_order.shipment.money == 6
    assert migration_order.state == calc_log_tariff_state(co_main.id, [])

    assert_migration_consumes(
        task, migration_order,
        [co_ch], [(6, 6, 7, 7)],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_rollback_into_bucks(session, client, task):
    main_order, (child_order,) = create_orders(
        client,
        cst.DIRECT_PRODUCT_ID,
        [cst.DIRECT_PRODUCT_RUB_ID],
        turn_on_log_tariff=False
    )

    consume_order(main_order, [666])
    mk_shipment(main_order, 1, money=60, force=False)
    co_main, = main_order.consumes

    consume_order(child_order, [30])
    mk_shipment(child_order, 120, force=False)
    co_ch, = child_order.consumes

    main_order.turn_on_log_tariff()

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 59)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert (main_order.shipment.money, main_order.shipment.bucks) == (0, D('0.966667'))
    assert child_order.shipment.money == D(59)
    assert migration_order.state == calc_log_tariff_state(co_main.id, [(co_main, D('0.966667'))])

    assert_migration_consumes(
        task, migration_order,
        [co_main, co_ch],
        [
            (D('29.00001'), D('29'), 19980, 19980),
            (30, 30, 30, 30),
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_wo_shipment_money(session, client, task):
    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_RUB_ID])
    consume_order(main_order, [100])
    co_main, = main_order.consumes

    consume_order(child_order, [10])
    mk_shipment(child_order, 10)
    co_ch, = child_order.consumes

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 76)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    assert main_order._is_log_tariff == cst.OrderLogTariffState.MIGRATED
    assert main_order.shipment.money == D('66')
    assert child_order.shipment.money == D(76)
    assert migration_order.state == calc_log_tariff_state(co_main.id, [(co_main, D('66'))])

    assert_migration_consumes(
        task, migration_order,
        [co_main, co_ch],
        [
            (66, 66, 100, 100),
            (10, 10, 10, 10),
        ],
    )


def test_partial_acted(session, client, task):
    main_order, (child_order,) = create_orders(
        client,
        cst.DIRECT_PRODUCT_ID,
        [cst.DIRECT_PRODUCT_RUB_ID],
        turn_on_log_tariff=False
    )
    consume_order(main_order, [5])
    mk_shipment(main_order, 2)
    co_main, = main_order.consumes
    co_main.invoice.generate_act(force=True)

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 95)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=D('3.166667'),
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )
    assert main_order.shipment.money == D('35.00001')
    assert migration_order.state == calc_log_tariff_state(co_main.id, [(co_main, '3.166667')])

    assert_migration_consumes(
        task, migration_order,
        [co_main],
        [
            (D('35.00001'), D('35'), 150, 150),
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


@pytest.mark.parametrize(
    'state',
    [
        cst.OrderLogTariffState.OFF,
        cst.OrderLogTariffState.MIGRATED,
    ],
)
def test_invalid_task_state(session, client, task, state):
    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_RUB_ID])
    main_order._is_log_tariff = state
    consume_order(main_order, [10])
    mk_shipment(main_order, 9)
    consume_order(child_order, [19])

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 360)

    with LogExceptionInterceptHandler() as log_intercept:
        ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    msg = 'log tariff is not enabled' if state == cst.OrderLogTariffState.OFF else 'task without state for migrated order'
    msg = 'log tariff task %s for order %s is invalid: ' % (task.id, main_order.id) + msg
    log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
    assert migration_order.error == msg


@pytest.mark.parametrize(
    'input_qty, res_qty, res_co_qtys, res_ov_qty',
    [
        pytest.param(
            50, 40,
            [None, (10, 10, 10, 10)],
            10,
            id='overcompletion',
        ),
        pytest.param(
            40, 30,
            [None, (10, 10, 10, 10)],
            None,
            id='all_completed',
        ),
        pytest.param(
            35, 25,
            [
                (-5, -5, 30, 30),
                (10, 10, 10, 10),
            ],
            None,
            id='main rollback',
        ),
        pytest.param(
            5, 0,
            [
                (-30, -30, 30, 30),
                (5, 5, 10, 10),
            ],
            None,
            id='child rollback',
        ),
        pytest.param(
            0, 0,
            [
                (-30, -30, 30, 30),
                None,
            ],
            None,
            id='rollback all',
        ),
    ],
)
def test_overact(session, client, task, input_qty, res_qty, res_co_qtys, res_ov_qty):
    main_order, (child_order,) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_RUB_ID],
        turn_on_log_tariff=False
    )
    consume_order(main_order, [30])
    mk_shipment(main_order, 30)
    co_main, = main_order.consumes

    consume_order(child_order, [10])
    mk_shipment(child_order, 40)
    co_child, = child_order.consumes

    co_main.invoice.generate_act(force=True)

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, input_qty)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=res_qty,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )

    assert_migration_consumes(
        task, migration_order,
        [co_main, co_child],
        res_co_qtys,
    )
    assert_migration_untariffed(task, migration_order, [(main_order, res_ov_qty)] if res_ov_qty else None)


def test_completion_fixed_qty(session, task):
    from balance.queue_processor import process_object

    client = ob.ClientBuilder.construct(session)
    main_order, (child_order,) = create_orders(
        client,
        cst.DIRECT_PRODUCT_ID,
        [cst.DIRECT_PRODUCT_RUB_ID],
        turn_on_log_tariff=False,
        is_ua_optimize=False
    )
    consume_order(main_order, [5])
    consume_order(child_order, [1])

    migrate_client(client)
    process_object(session, 'MIGRATE_TO_CURRENCY', 'Client', client.id)

    mk_shipment(child_order, 2, force=False)
    assert main_order.completion_fixed_qty == 0
    main_order.completion_fixed_qty = None

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 3, currency_id=FISH_CURRENCY_ID)
    create_input(session, task, migration_order.id, child_order, 120, currency_id=RUB_CURRENCY_ID)
    session.flush()

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert migration_order.error is None
    assert main_order.shipment.bucks == 0
    assert main_order.shipment.money == 120
    assert main_order.completion_qty == 4
    assert main_order.completion_fixed_qty == 0

    assert_migration_consumes(
        task, migration_order,
        main_order.consumes + child_order.consumes,
        [
            (120, 120, 150, 150),
            None,
            None,
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_tariff_dt_before_optimize_processing(session, client, task):
    """Исторические открутки перед датой разбора ОС
    """
    now = session.now()
    consume_dt = now - datetime.timedelta(days=7)
    shipment_dt = now - datetime.timedelta(days=5)
    main_dt = now - datetime.timedelta(days=1)

    main_order, (child_order,) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_RUB_ID],
        turn_on_log_tariff=False
    )
    consume_order(main_order, [10], dt=consume_dt)
    consume_order(child_order, [30], dt=consume_dt)

    co_main, = main_order.consumes
    co_child, = child_order.consumes

    mk_shipment(child_order, 40, dt=shipment_dt)
    a_ua.handle_orders(session, [main_order], main_dt)
    assert main_order.completion_qty == 10

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 40, tariff_dt=shipment_dt)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert migration_order.error is None
    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=10,
            completion_fixed_qty=0,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )

    assert_migration_consumes(
        task, migration_order,
        [co_main, co_child],
        [
            (10, 10, 10, 10),
            (30, 30, 30, 30),
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_tariff_dt_before_currency_migration(session, task):
    """Исторические открутки перед миграцией на мультивалютность
    """
    now = session.now()
    migration_dt = now - datetime.timedelta(days=1)
    shipment_dt = now - datetime.timedelta(days=2)

    client = ob.ClientBuilder.construct(session)

    main_order, (child_order,) = create_orders(
        client,
        cst.DIRECT_PRODUCT_ID,
        [cst.DIRECT_PRODUCT_ID],
        turn_on_log_tariff=False
    )
    consume_order(main_order, [10])
    consume_order(child_order, [30])

    co_main, = main_order.consumes
    co_child, = child_order.consumes
    mk_shipment(child_order, 40, dt=shipment_dt)

    client.set_currency(
        cst.ServiceId.DIRECT,
        'RUB',
        migration_dt,
        cst.CONVERT_TYPE_MODIFY,
        force=1
    )
    main_order.turn_on_log_tariff()

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 40, currency_id=None, tariff_dt=shipment_dt)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert migration_order.error is None
    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=10,
            completion_fixed_qty=0,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
            shipment=hm.has_properties(
                money=300,
                bucks=0,
            )
        )
    )

    assert_migration_consumes(
        task, migration_order,
        [co_main, co_child],
        [
            (300, 300, 300, 300),
            (900, 900, 900, 900),
        ],
    )
    assert_migration_untariffed(task, migration_order, None)


@pytest.mark.parametrize(
    'is_bucks',
    [False, True],
)
def test_main_wo_historical(session, client, task, is_bucks):
    """Нет откруток в historical -> получаем 0 за древнюю дату.
    """
    main_order, children = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_ID if is_bucks else cst.DIRECT_PRODUCT_RUB_ID],
    )
    child_order, = children

    consume_order(main_order, [10])
    co, = main_order.consumes

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 0, tariff_dt=datetime.datetime.fromtimestamp(0), currency_id=None)
    assert child_order.shipment.dt is None

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=0,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )
    assert migration_order.state == calc_log_tariff_state(co.id, [])

    if is_bucks:
        assert child_order.shipment.money is None
        assert child_order.shipment.bucks == 0
    else:
        assert child_order.shipment.money == 0
        assert child_order.shipment.bucks is None
    assert child_order.shipment.dt == datetime.datetime(1970, 1, 1, 3, 0)

    assert_migration_consumes(
        task, migration_order,
        [], [],
    )
    assert_migration_untariffed(task, migration_order, None)


@pytest.mark.parametrize(
    'params',
    [
        pytest.param({'product_id': cst.DIRECT_PRODUCT_ID,
                      'main_consume_qty': 10,
                      'child_consume_qty': 1,
                      'main_completion_qty': 10,
                      'child_completion_qty': 1,
                      'input_bucks': 1,
                      'res_completion_consumed_money': 0},
                     id='only bucks'),
        pytest.param({'product_id': cst.DIRECT_PRODUCT_RUB_ID,
                      'main_consume_qty': 2,
                      'child_consume_qty': 10,
                      'main_completion_qty': 1,
                      'child_completion_qty': 40,
                      'main_completion_money': 40,
                      'input_bucks': 1,
                      'input_money': 50,
                      'res_completion_consumed_money': D('9.99999')},
                     id='completed_money'),
        pytest.param({'product_id': cst.DIRECT_PRODUCT_RUB_ID,
                      'main_consume_qty': 2,
                      'child_consume_qty': 10,
                      'main_completion_qty': 1,
                      'child_completion_qty': 40,
                      'input_money': 100,
                      'res_completion_consumed_money': 30},
                     id='overcompletion'),
    ],
)
def test_zero_completion_consumed_money(session, client, task, params):
    main_order, (child_order,) = create_orders(client, cst.DIRECT_PRODUCT_ID, [params['product_id']])

    consume_order(main_order, [params['main_consume_qty']])
    mk_shipment(main_order, params['main_completion_qty'], money=params.get('main_completion_money'))

    consume_order(child_order, [params['child_consume_qty']])
    mk_shipment(child_order, params['child_completion_qty'])

    migration_order = create_migration_order(session, task, main_order)
    if 'input_bucks' in params:
        create_input(session, task, migration_order.id, child_order, params['input_bucks'], currency_id=FISH_CURRENCY_ID)
    if 'input_money' in params:
        create_input(session, task, migration_order.id, child_order, params['input_money'])

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert main_order.completion_consumed_money == params['res_completion_consumed_money']


def test_skip_consume_w_fictive_invoice(session, task):
    from balance import core

    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session, agency=agency)
    migrate_client(agency)
    migrate_client(client)

    main_order = ob.OrderBuilder.construct(session, agency=agency, client=client, product_id=cst.DIRECT_PRODUCT_RUB_ID, main_order=1, is_ua_optimize=1)
    child_order = ob.OrderBuilder.construct(session, agency=agency, client=client, product_id=cst.DIRECT_PRODUCT_RUB_ID, parent_group_order=main_order)
    session.flush()

    contract = ob.create_credit_contract(session, agency, personal_account=0, personal_account_fictive=0, commission_type=None)
    paysys = ob.PaysysBuilder.construct(
        session,
        firm_id=contract.firm.id,
        payment_method_id=cst.PaymentMethodIDs.bank,
        iso_currency='RUB',
        currency=mapper.fix_crate_base_cc('RUB'),
        extern=1,
    )
    request = ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=agency,
            rows=[ob.BasketItemBuilder(quantity=30, order=main_order), ob.BasketItemBuilder(quantity=10, order=child_order)],
        ),
    )
    core.Core(session).pay_on_credit(request.id, paysys.id, contract.person_id, contract.id)

    mk_shipment(child_order, 40)
    co_main, = main_order.consumes
    co_child, = child_order.consumes
    main_order.turn_on_log_tariff()

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 40)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    assert migration_order.error is None
    hm.assert_that(
        main_order,
        hm.has_properties(
            completion_qty=40,
            completion_fixed_qty=0,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )

    assert_migration_consumes(
        task, migration_order,
        [co_main, co_child],
        [],
    )
    assert_migration_untariffed(task, migration_order, None)


def test_product_quasi_currency(session, task):
    client = ob.ClientBuilder.construct(session)
    migrate_client(client, 'BYN', cst.CONVERT_TYPE_COPY)

    main_order, (child_order_1, child_order_2) = create_orders(
        client,
        cst.DIRECT_PRODUCT_QUASI_BYN_ID,
        [
            cst.DIRECT_PRODUCT_QUASI_BYN_ID,
            cst.DIRECT_PRODUCT_QUASI_BYN_ID,
        ],
        turn_on_log_tariff=False,
    )
    consume_order(main_order, [30])
    mk_shipment(main_order, 30)

    consume_order(child_order_1, [10])
    mk_shipment(child_order_1, 60)
    co_child_1, = child_order_1.consumes

    consume_order(child_order_2, [10])
    co_child_2, = child_order_2.consumes  # по конзюму нет и не будет отчислений

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order_1, 60, currency_id=BYN_CURRENCY_ID)
    create_input(session, task, migration_order.id, child_order_2, 0, currency_id=None, tariff_dt=0)  # нет записи в historical_aggregates

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    co_main_1, co_main_2 = main_order.consumes

    hm.assert_that(
        main_order,
        hm.has_properties(
            consume_qty=40,
            completion_qty=50,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )

    assert_migration_consumes(
        task, migration_order,
        [co_main_1, co_main_2, co_child_1],
        [
            (30, D('1057.52'), 30, D('1057.52')),
            (10, D('352.51'), 10, D('352.51')),
            (10, D('352.51'), 10, D('352.51')),
        ],
    )
    assert_migration_untariffed(task, migration_order, [(main_order, 10)], currency_id=BYN_CURRENCY_ID)


@pytest.mark.dont_mock_mnclose
@pytest.mark.parametrize('is_month_closed', [False, True])
def test_closed_month_tariff_dt(session, client, task, is_month_closed):
    on_dt = datetime.datetime.now().replace(microsecond=0)
    tariff_dt = on_dt - datetime.timedelta(10)

    main_order, (child_order, ) = create_orders(
        client,
        cst.DIRECT_PRODUCT_RUB_ID,
        [cst.DIRECT_PRODUCT_RUB_ID],
        turn_on_log_tariff=False,
    )

    consume_order(child_order, [100])
    mk_shipment(child_order, 66, dt=tariff_dt, force=False)

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, child_order, 70, tariff_dt=tariff_dt)

    with mock.patch('balance.mncloselib.is_month_closed', return_value=is_month_closed):
        ProcessTariffMigration(main_order, task.id).do(migration_order.id)
        session.expire_all()

    tariff_consume, = migration_order.tariff_consumes
    assert migration_order.tariff_untariffed == []
    hm.assert_that(
        tariff_consume,
        hm.has_properties(
            qty=70,
            tariff_dt=child_order.shipment_dt
        )
    )
    assert tariff_consume.qty == 70
    assert tariff_consume.tariff_dt == child_order.shipment_dt
    if is_month_closed:
        assert child_order.shipment_dt >= on_dt > tariff_dt
    else:
        assert child_order.shipment_dt == tariff_dt


def test_completions_for_main_order(session, client, task):
    main_order, _ = create_orders(client, cst.DIRECT_PRODUCT_RUB_ID, [cst.DIRECT_PRODUCT_RUB_ID])
    main_order.turn_on_log_tariff()

    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, main_order, 666)

    with LogExceptionInterceptHandler() as log_intercept:
        ProcessTariffMigration(main_order, task.id).do(migration_order.id)

    msg = "log tariff task %s for order %s can't be processed: completions for unified account" % (task.id, main_order.id)
    log_intercept.assert_exc(exc.LOG_TARIFF_CANT_PROCESS, msg)
    assert migration_order.error == msg


def test_transfer2main_overcompletion(session, client, task):
    main_order, (o1, o2) = create_orders(
        client,
        cst.DIRECT_PRODUCT_ID,
        [cst.DIRECT_PRODUCT_ID, cst.DIRECT_PRODUCT_RUB_ID],
        turn_on_log_tariff=False
    )
    consume_order(main_order, [10])
    mk_shipment(main_order, 0, money=390, force=False)

    consume_order(o1, [3])
    mk_shipment(o1, 1, force=False)
    co1, = o1.consumes

    consume_order(o2, [300])
    mk_shipment(o2, 630, force=False)
    co2, = o2.consumes

    main_order.turn_on_log_tariff()
    migration_order = create_migration_order(session, task, main_order)
    create_input(session, task, migration_order.id, o1, 1, currency_id=FISH_CURRENCY_ID)
    create_input(session, task, migration_order.id, o2, 690, currency_id=RUB_CURRENCY_ID)

    ProcessTariffMigration(main_order, task.id).do(migration_order.id)
    session.expire_all()

    hm.assert_that(
        main_order,
        hm.has_properties(
            consume_qty=12,
            completion_qty=13,
            completion_consumed_money=360,
            _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
        )
    )
    assert main_order.shipment.money == 390
    co_main1, co_main2 = main_order.consumes

    assert_consumes(
        main_order,
        [
            (10, 300, 10, 300),
            (2, 60, 2, 60),
        ]
    )

    assert_migration_untariffed(
        task,
        migration_order,
        [(main_order, 30)]
    )
    assert_migration_consumes(
        task,
        migration_order,
        [co_main1, co_main2, co1, co2],
        [
            (300, 300, co_main1.consume_qty * 30, co_main1.consume_sum),
            (60, 60, co_main2.consume_qty * 30, co_main2.consume_sum),
            (30, 30, co1.consume_qty * 30, co1.consume_sum),
            (300, 300, co2.consume_qty, co2.consume_sum),
        ]
    )


class TestParentlessOrder(object):
    @pytest.mark.parametrize(
        'task_qty, untariffed_qty, consumes_qty',
        [
            pytest.param(41, None, 41, id='unacted'),
            pytest.param(44, 2, 42, id='overcompletion'),
        ]
    )
    def test_currency(self, session, task, task_qty, untariffed_qty, consumes_qty):
        client = ob.ClientBuilder.construct(session)
        migrate_client(client, 'RUB', None)

        order = ob.OrderBuilder.construct(session, client=client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
        consume_order(order, [42])
        mk_shipment(order, 40, force=False)
        co, = order.consumes

        order.turn_on_log_tariff()
        migration_order = create_migration_order(session, task, order)
        create_input(session, task, migration_order.id, order, task_qty)

        ProcessTariffMigration(order, task.id).do(migration_order.id)
        session.expire_all()

        hm.assert_that(
            order,
            hm.has_properties(
                consume_qty=42,
                completion_qty=task_qty,
                _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
                shipment=hm.has_properties(money=task_qty)
            )
        )
        assert_consumes(
            order,
            [
                (42, 42, consumes_qty, consumes_qty),
            ]
        )

        assert_migration_untariffed(
            task, migration_order,
            [(order, untariffed_qty)] if untariffed_qty else None
        )
        assert_migration_consumes(
            task, migration_order,
            [co],
            [(consumes_qty, consumes_qty, 42, 42)]
        )

    @pytest.mark.parametrize(
        'task_qty, order_qty, consume_qty, res_untariffed_qty, res_consumes_qty, res_consumes_money',
        [
            pytest.param(44, D('41.466667'), D('41.466667'), None, D('44.00001'), 44, id='unacted'),
            pytest.param(66, D('42.2'), 42, 6, 60, 60, id='overcompletion'),
        ]
    )
    def test_fish(self, session, task, task_qty, order_qty, consume_qty, res_untariffed_qty, res_consumes_qty, res_consumes_money):
        client = ob.ClientBuilder.construct(session)

        order = ob.OrderBuilder.construct(session, client=client, product_id=cst.DIRECT_PRODUCT_ID)
        consume_order(order, [42])
        mk_shipment(order, 40, force=False)
        migrate_client(client, 'RUB', cst.CONVERT_TYPE_MODIFY)
        mk_shipment(order, 40, force=False)

        co, = order.consumes

        order.turn_on_log_tariff()
        migration_order = create_migration_order(session, task, order)
        create_input(session, task, migration_order.id, order, 40, currency_id=FISH_CURRENCY_ID)
        create_input(session, task, migration_order.id, order, task_qty)

        ProcessTariffMigration(order, task.id).do(migration_order.id)
        session.expire_all()

        hm.assert_that(
            order,
            hm.has_properties(
                consume_qty=42,
                completion_qty=order_qty,
                completion_fixed_qty=40,
                completion_consumed_money=res_consumes_money,
                _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
                shipment=hm.has_properties(
                    bucks=40,
                    money=task_qty
                )
            )
        )
        assert_consumes(
            order,
            [
                (42, 1260, consume_qty, 1200 + res_consumes_money),
            ]
        )

        assert_migration_untariffed(
            task, migration_order,
            [(order, res_untariffed_qty)] if res_untariffed_qty else None
        )
        assert_migration_consumes(
            task, migration_order,
            [co],
            [(1200 + res_consumes_qty, 1200 + res_consumes_money, 1260, 1260)]
        )

    @pytest.mark.parametrize(
        'task_qty, untariffed_qty, consumes_qty',
        [
            pytest.param(D('41.6666'), None, D('41.6666'), id='unacted'),
            pytest.param(43, 1, 42, id='overcompletion'),
        ]
    )
    def test_copy_migrated(self, session, task, task_qty, untariffed_qty, consumes_qty):
        client = ob.ClientBuilder.construct(session)

        order = ob.OrderBuilder.construct(session, client=client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
        old_order = ob.OrderBuilder.construct(
            session,
            client=client,
            product_id=cst.DIRECT_PRODUCT_ID,
            group_order_id=order.id
        )

        consume_order(old_order, [42])
        mk_shipment(old_order, 43, force=False)
        old_co, = old_order.consumes

        consume_order(order, [42])
        mk_shipment(order, 40, force=False)
        co, = order.consumes

        migrate_client(client, 'RUB', cst.CONVERT_TYPE_COPY)

        order.turn_on_log_tariff()
        migration_order = create_migration_order(session, task, order)
        create_input(session, task, migration_order.id, old_order, 666, currency_id=FISH_CURRENCY_ID)
        create_input(session, task, migration_order.id, order, task_qty)

        ProcessTariffMigration(order, task.id).do(migration_order.id)
        session.expire_all()

        hm.assert_that(
            order,
            hm.has_properties(
                consume_qty=42,
                completion_qty=task_qty,
                _is_log_tariff=cst.OrderLogTariffState.MIGRATED,
                shipment=hm.has_properties(money=task_qty)
            )
        )
        hm.assert_that(
            old_order,
            hm.has_properties(
                consume_qty=42,
                completion_qty=666,
                child_ua_type=cst.UAChildType.LOG_TARIFF,
                shipment=hm.has_properties(money=None, bucks=666)
            )
        )
        assert_consumes(
            order,
            [
                (42, 42, consumes_qty, ut.round(consumes_qty, 2)),
            ]
        )
        assert_consumes(
            old_order,
            [
                (42, 1260, 42, 1260),
            ]
        )

        assert_migration_untariffed(
            task, migration_order,
            [(order, untariffed_qty)] if untariffed_qty else None
        )
        assert_migration_consumes(
            task, migration_order,
            [co, old_co],
            [
                (consumes_qty, ut.round(consumes_qty, 2), 42, 42),
                (1260, 1260, 1260, 1260),
            ]
        )

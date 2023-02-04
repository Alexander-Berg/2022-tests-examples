# -*- coding: utf-8 -*-

import itertools
import copy

import pytest
import hamcrest

from yt.wrapper import TablePath, ypath_join, ypath_split
from yt.wrapper.common import GB

from billing.log_tariffication.py.jobs.core_tariff import (
    tariff_events,
    tariff_metrics,
)
from billing.library.python.yql_utils import query_metrics
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)
from billing.library.python.logmeta_utils.meta import (
    set_log_tariff_meta,
    get_log_tariff_meta,
)
from billing.log_tariffication.py.lib.schema import (
    STRIPPED_LOG_TABLE_SCHEMA,
    CONSUMES_TABLE_SCHEMA,
    STRIPPED_LOG_TABLE_SORTED_BY,
    CORRECTIONS_LOG_TABLE_SCHEMA,
)
from billing.log_tariffication.py.lib.constants import (
    CORRECTIONS_LOG_INTERVAL_KEY,
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.log_tariffication.py.tests.utils import (
    OrderDataBuilder,
    check_node_is_locked,
    dict2matcher,
)
from billing.log_tariffication.py.tests.constants import (
    NEXT_LOG_TARIFF_META,
    CURR_LOG_TARIFF_META,
    PREV_LOG_TARIFF_META,
    OLD_LOG_TARIFF_META,
    NEXT_RUN_ID,
    CURR_RUN_ID,
    PREV_RUN_ID,
    ZERO_CORRECTIONS_INTERVAL,
    PREV_CORRECTIONS_INTERVAL,
    CURR_CORRECTIONS_INTERVAL,
    NEXT_CORRECTIONS_INTERVAL,
)


@pytest.fixture(name='orders_dir')
def orders_dir_fixture(yt_client, tariff_results_dir):
    return create_subdirectory(yt_client, tariff_results_dir, 'orders')


@pytest.fixture(name='corrections_dir')
def corrections_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'corrections')


@pytest.fixture(name='consumes_table_path')
def consumes_table_path_fixture(yt_root):
    return ypath_join(yt_root, 'consumes')


def curr_metadata():
    cur_meta = copy.deepcopy(CURR_LOG_TARIFF_META)
    cur_meta[CORRECTIONS_LOG_INTERVAL_KEY] = CURR_CORRECTIONS_INTERVAL.to_meta()
    return cur_meta


@pytest.fixture
def zero_metadata():
    cur_meta = copy.deepcopy(CURR_LOG_TARIFF_META)
    cur_meta[CORRECTIONS_LOG_INTERVAL_KEY] = ZERO_CORRECTIONS_INTERVAL.to_meta()
    return cur_meta


def create_default_tables_fixture(
    yt_client, consumes_table_path, prev_published_untariffed_table_path, stripped_log_path,
    tariffed_dir, untariffed_dir, orders_dir, corrections_dir, zero_metadata
):
    prev_meta = copy.deepcopy(PREV_LOG_TARIFF_META)

    yt_client.create('table', consumes_table_path,
                     attributes={'schema': CONSUMES_TABLE_SCHEMA})
    set_log_tariff_meta(yt_client, consumes_table_path, zero_metadata)

    yt_client.create('table', prev_published_untariffed_table_path,
                     attributes={'schema': STRIPPED_LOG_TABLE_SCHEMA})
    set_log_tariff_meta(yt_client, prev_published_untariffed_table_path, prev_meta)

    yt_client.create('table', stripped_log_path,
                     attributes={'schema': STRIPPED_LOG_TABLE_SCHEMA})
    set_log_tariff_meta(yt_client, stripped_log_path, zero_metadata)

    yt_client.create('table', ypath_join(corrections_dir, PREV_RUN_ID),
                     attributes={'schema': CORRECTIONS_LOG_TABLE_SCHEMA})
    set_log_tariff_meta(yt_client, ypath_join(corrections_dir, PREV_RUN_ID), zero_metadata)

    for results_dir in (tariffed_dir, untariffed_dir, orders_dir):
        table_path = ypath_join(results_dir, PREV_RUN_ID)
        yt_client.create('table', table_path)
        set_log_tariff_meta(yt_client, table_path, prev_meta)


@pytest.fixture
def create_default_tables(
    yt_client, consumes_table_path, prev_published_untariffed_table_path, stripped_log_path,
    tariffed_dir, untariffed_dir, orders_dir, corrections_dir, zero_metadata
):
    return create_default_tables_fixture(
        yt_client, consumes_table_path, prev_published_untariffed_table_path, stripped_log_path,
        tariffed_dir, untariffed_dir, orders_dir, corrections_dir, zero_metadata
    )


@pytest.fixture(name='prev_tariffed_table_path')
def prev_tariffed_table_path_fixture(yt_root, tariffed_dir):
    return ypath_join(tariffed_dir, PREV_RUN_ID)


@pytest.fixture(name='curr_orders_table_path')
def curr_orders_table_path_fixture(yt_root, orders_dir):
    return ypath_join(orders_dir, CURR_RUN_ID)


@pytest.fixture(name='create_curr_tables')
def create_curr_tables_fixture(
    yt_client, curr_orders_table_path, curr_untariffed_table_path,
    curr_tariffed_table_path, zero_metadata
):
    for path in (curr_orders_table_path, curr_untariffed_table_path,
                 curr_tariffed_table_path):
        yt_client.create('table', path)
        set_log_tariff_meta(yt_client, path, zero_metadata)


def get_result(yt_client, res_paths):
    path_names = ['tariffed', 'untariffed', 'orders']

    res = {}
    for name, path in zip(path_names, res_paths):
        for row in yt_client.read_table(path):
            order_eid = '%s-%s' % (row['ServiceID'], row['EffectiveServiceOrderID'])
            res.setdefault(order_eid, {}).setdefault(name, []).append(row)

    return res


@pytest.fixture(name='run_job')
def run_job_fixture(
    yql_client,
    stripped_log_path,
    published_untariffed_dir,
    consumes_table_path,
    tariffed_dir,
    untariffed_dir,
    orders_dir,
    corrections_dir
):

    def _wrapped(yt_client, transaction, **kwargs):
        return tariff_events.TariffEventsProcessor(
            yt_client,
            yql_client,
            stripped_log_path,
            published_untariffed_dir,
            consumes_table_path,
            tariffed_dir,
            untariffed_dir,
            orders_dir,
            transaction.transaction_id,
            corrections_dir,
            **kwargs
        ).do()

    return _wrapped


@pytest.fixture(name='currencies_ref_path')
def currency_ref_path_fixture(yt_client, yt_root):
    path = TablePath(ypath_join(yt_root, 'currencies_ref'), schema=[
        {'name': 'iso_num_code', 'type': 'int64'},
        {'name': 'iso_code', 'type': 'utf8'},
    ])
    yt_client.write_table(path, [
        {'iso_num_code': 666, 'iso_code': 'RUB'},
    ])
    return str(path)


@pytest.mark.usefixtures("create_default_tables")
@pytest.mark.parametrize('batch_size', [3, 100])
def test_tariff_events(yt_client, yql_client, stripped_log_path, consumes_table_path,
                       tariffed_dir, curr_orders_table_path, curr_untariffed_table_path,
                       curr_tariffed_table_path, prev_published_untariffed_table_path,
                       currencies_ref_path, zero_metadata, run_job, batch_size):

    consumes_counter = itertools.count(1)
    events_counter = itertools.count(1)

    orders = [
        OrderDataBuilder(1, consumes_counter, events_counter)
        .add_consume(10)
        .add_consume(12)
        .add_untariffed_event(1)
        .add_event(7)
        .add_event(5)
        .add_event(9),
        OrderDataBuilder(2, consumes_counter, events_counter)
        .add_consume(-10)
        .add_consume(-12)
        .add_untariffed_event(-1)
        .add_event(-7)
        .add_event(-5)
        .add_event(-9),
        OrderDataBuilder(3, consumes_counter, events_counter)
        .add_consume(6)
        .add_event(3)
        .add_event(6),
        OrderDataBuilder(4, consumes_counter, events_counter)
        .add_consume(-2)
        .add_consume(-3)
        .add_untariffed_event(-3)
        .add_event(-4),
        OrderDataBuilder(5, consumes_counter, events_counter)
        .add_consume(9)
        .add_consume(1)
        .add_untariffed_event(4)
        .add_untariffed_event(-3)
        .add_untariffed_event(11)
        .add_untariffed_event(-3)
        .add_untariffed_event(1),
        OrderDataBuilder(6, consumes_counter, events_counter)
        .add_consume(-5)
        .add_event(-5)
        .add_event(3)
        .add_event(-4)
        .add_event(1),
        OrderDataBuilder(7, consumes_counter, events_counter)
        .add_consume(4)
        .add_consume(3)
        .add_event(4)
        .add_event(-3)
        .add_event(12)
        .add_event(-1),
        OrderDataBuilder(8, consumes_counter, events_counter)
        .add_consume(2)
        .add_consume(8)
        .add_event(2)
        .add_event(-1)
        .add_event(-3)
        .add_event(1)
        .add_event(14),
        OrderDataBuilder(9, consumes_counter, events_counter, '666', 666)
        .add_consume(222)
        .add_consume(444)
        .add_event(333)
        .add_event(333),
    ]

    crawfish_order = OrderDataBuilder(10, consumes_counter, events_counter)
    crawfish_order.add_consume(16, 2.29, 7, 1)
    for idx in range(16):
        crawfish_order.add_event(1)

    orders.append(crawfish_order)

    orders.extend([
        OrderDataBuilder(11, consumes_counter, events_counter)
        .add_event(33)
        .add_untariffed_event(44),
        OrderDataBuilder(12, consumes_counter, events_counter)
        .add_event(34)
        .add_untariffed_event(43)
        .skip(),
        OrderDataBuilder(13, consumes_counter, events_counter, currency_id=0, state='aaa')
        .add_consume(10)
        .add_event(4)
        .add_untariffed_event(7),
        OrderDataBuilder(13, consumes_counter, events_counter, currency_id=666, state='aaa')
        .add_consume(7)
        .add_event(2)
        .add_untariffed_event(6.5),
        OrderDataBuilder(14, consumes_counter, events_counter)
        .add_consume(10, None, None, None, None)
        .add_consume(2)
        .add_consume(3, None, None, None, None)
        .add_untariffed_event(10)
        .add_untariffed_event(6.66),
        OrderDataBuilder(15, consumes_counter, events_counter, currency_id=0)
        .add_event(10)
        .skip(),
        OrderDataBuilder(15, consumes_counter, events_counter, currency_id=666)
        .add_consume(7)
        .add_event(7),
    ])

    crawfish_overflow_order = OrderDataBuilder(16, consumes_counter, events_counter)
    crawfish_overflow_order.add_consume(7, 1, 7, 1)
    for _ in range(10):
        crawfish_overflow_order.add_event(1)
    for _ in range(15):
        crawfish_overflow_order.add_event(-1)
    for _ in range(12):
        crawfish_overflow_order.add_event(1)

    orders.append(crawfish_overflow_order)

    orders.extend([
        OrderDataBuilder(17, consumes_counter, events_counter)
            .add_consume(10)
            .add_event(4)
            .add_untariffed_event(7),
        OrderDataBuilder(17, consumes_counter, events_counter, group_dt=172800)
            .add_consume(7)
            .add_event(2, 172800)
            .add_untariffed_event(6.5, 172800),
        OrderDataBuilder(17, consumes_counter, events_counter, currency_id=0, group_dt=172800)
            .add_consume(10)
            .add_event(4, 172801)
            .add_untariffed_event(7, 172801),
        OrderDataBuilder(18, consumes_counter, events_counter)
            .add_consume(5)
            .add_consume(5)
            .add_event(5)
            .add_event(4)
            .add_event(2)
            .add_event(-6)
            .add_event(-5)
            .add_event(5)
            .add_event(1),
        OrderDataBuilder(19, consumes_counter, events_counter)
            .add_consume(-5)
            .add_consume(-5)
            .add_event(-5)
            .add_event(-2)
            .add_event(-2)
            .add_event(-5)
            .add_event(3)
            .add_event(2)
            .add_event(5)
            .add_event(-5)
            .add_event(-1),
        OrderDataBuilder(20, consumes_counter, events_counter)
            .add_consume(5)
            .add_event(10)
            .add_event(-10)
            .add_event(5)
            .add_event(1),
        OrderDataBuilder(21, consumes_counter, events_counter)
            .add_consume(-5)
            .add_event(-10)
            .add_event(10)
            .add_event(-5)
            .add_event(-1)

    ])

    inaccurate_sum_order = OrderDataBuilder(22, consumes_counter, events_counter)
    inaccurate_sum_order.add_consume(24000, 240.5, 12000, 120.25)
    inaccurate_sum_order.add_event(+12000)
    inaccurate_sum_order.add_event(+3600)
    inaccurate_sum_order.add_event(+3360)
    inaccurate_sum_order.add_event(+3360)
    inaccurate_sum_order.add_event(+1680)

    orders.append(inaccurate_sum_order)

    yt_client.write_table(
        TablePath(consumes_table_path, schema=CONSUMES_TABLE_SCHEMA),
        [o.sync_info for o in orders if not o.is_skipped]
    )
    yt_client.run_sort(consumes_table_path, sort_by=['ServiceID', 'EffectiveServiceOrderID'])

    yt_client.write_table(
        TablePath(prev_published_untariffed_table_path, schema=STRIPPED_LOG_TABLE_SCHEMA),
        itertools.chain.from_iterable(o.untariffed_events for o in orders)
    )
    yt_client.run_sort(prev_published_untariffed_table_path, sort_by=STRIPPED_LOG_TABLE_SORTED_BY)

    yt_client.write_table(
        TablePath(stripped_log_path, schema=STRIPPED_LOG_TABLE_SCHEMA),
        itertools.chain.from_iterable(o.new_events for o in orders)
    )
    yt_client.run_sort(stripped_log_path, sort_by=STRIPPED_LOG_TABLE_SORTED_BY)

    with yt_client.Transaction(ping=False) as transaction:
        res_paths = run_job(
            yt_client,
            transaction,
            data_size_per_job=(1 * GB),
            batch_size=batch_size
        )
        check_node_is_locked(tariffed_dir)

    assert res_paths == (
        str(curr_tariffed_table_path),
        str(curr_untariffed_table_path),
        str(curr_orders_table_path)
    )

    assert yt_client.get(ypath_join(curr_untariffed_table_path, '@sorted_by')) == STRIPPED_LOG_TABLE_SORTED_BY

    for path in res_paths:
        assert get_log_tariff_meta(yt_client, path) == zero_metadata

    tariff_metrics.add_metrics(
        yt_client, yql_client,
        ypath_split(curr_tariffed_table_path)[0],
        ypath_split(curr_untariffed_table_path)[0],
        currencies_ref_path,
        data_size_per_job=str(1 * GB),
    )

    tariffed_metrics = query_metrics.get_table_metrics_data(
        yt_client, curr_tariffed_table_path
    )

    untariffed_metrics = query_metrics.get_table_metrics_data(
        yt_client, curr_untariffed_table_path
    )

    tariffed_metrics_matchers = map(dict2matcher, [
        {'labels': {'service_id': 'TOTAL'}, 'name': 'tariffed_sum',
         'type': 'float', 'value': 985.79},
        {'labels': {'service_id': 'TOTAL'}, 'name': 'tariffed_qty',
         'type': 'float', 'value': 24778.0},
        {'labels': {'service_id': '7'}, 'name': 'tariffed_sum',
         'type': 'float', 'value': 985.79},
        {'labels': {'service_id': '7'}, 'name': 'tariffed_qty',
         'type': 'float', 'value': 24778.0},
    ])

    untariffed_metrics_matchers = map(dict2matcher, [
        {'labels': {'currency': 'TOTAL'}, 'name': 'cost_cur',
         'type': 'float', 'value': 99.66},
        {'labels': {'currency': 'pcs'}, 'name': 'cost_cur',
         'type': 'float', 'value': 12.0},
        {'labels': {'currency': 'RUB'}, 'name': 'cost_cur',
         'type': 'float', 'value': 87.66},
    ])

    hamcrest.assert_that(
        tariffed_metrics,
        hamcrest.contains_inanyorder(*tariffed_metrics_matchers),
    )
    hamcrest.assert_that(
        untariffed_metrics,
        hamcrest.contains_inanyorder(*untariffed_metrics_matchers),
    )

    return get_result(yt_client, res_paths)


@pytest.mark.usefixtures("yt_transaction", "create_default_tables", "create_curr_tables")
def test_already_done(
        yt_client,
        caplog,
        curr_orders_table_path,
        curr_untariffed_table_path,
        curr_tariffed_table_path,
        run_job,
        yt_transaction
):
    assert run_job(yt_client, yt_transaction) == (
        str(curr_tariffed_table_path),
        str(curr_untariffed_table_path),
        str(curr_orders_table_path)
    )
    assert "Already done" in caplog.text


@pytest.mark.parametrize(
    ["table_path_fixture", "bad_meta", "preparation_fixtures"],
    [
        pytest.param(
            "curr_tariffed_table_path", PREV_LOG_TARIFF_META, ["create_curr_tables"],
            id="curr_tariffed"
        ),
        pytest.param(
            "curr_untariffed_table_path", PREV_LOG_TARIFF_META, ["create_curr_tables"],
            id="curr_untariffed"
        ),
        pytest.param(
            "curr_orders_table_path", PREV_LOG_TARIFF_META, ["create_curr_tables"],
            id="curr_orders"
        ),
        pytest.param(
            "prev_tariffed_table_path", OLD_LOG_TARIFF_META, [],
            id="prev_tariffed old"
        ),
        pytest.param(
            "prev_tariffed_table_path", NEXT_LOG_TARIFF_META, [],
            id="prev_tariffed bad"
        ),
        pytest.param(
            "consumes_table_path", PREV_LOG_TARIFF_META, [],
            id="consumes"
        ),
        pytest.param(
            "prev_published_untariffed_table_path", OLD_LOG_TARIFF_META, [],
            id="untariffed old"
        ),
        pytest.param(
            "prev_published_untariffed_table_path", NEXT_LOG_TARIFF_META, [],
            id="untariffed bad"
        ),
        pytest.param(
            "curr_orders_table_path", curr_metadata(), ['create_curr_tables'],
            id="corrections_bad",
        ),
    ]
)
@pytest.mark.usefixtures("yt_transaction", "create_default_tables")
def test_bad_meta_in_tables(yt_client, table_path_fixture, bad_meta, get_fixture, preparation_fixtures, run_job, yt_transaction):
    list(map(get_fixture, preparation_fixtures))
    set_log_tariff_meta(yt_client, get_fixture(table_path_fixture), bad_meta)
    with pytest.raises(AssertionError):
        run_job(yt_client, yt_transaction)


@pytest.mark.usefixtures("yt_transaction", "create_default_tables")
def test_next_result_exists(yt_client, tariffed_dir, run_job, yt_transaction):
    yt_client.create('table', ypath_join(tariffed_dir, NEXT_RUN_ID))
    with pytest.raises(AssertionError):
        run_job(yt_client, yt_transaction)


@pytest.mark.parametrize(
    'corrections_meta, orders_skipped',
    [
        pytest.param(CURR_CORRECTIONS_INTERVAL, [True, False, True], id='current'),
        pytest.param(NEXT_CORRECTIONS_INTERVAL, [True, True, False], id='next'),
        pytest.param(LogInterval([  # sum of intervals
            Subinterval('cor_c1', 'cor_t1', 0, 1, 3),
            Subinterval('cor_c1', 'cor_t1', 1, 15, 35),
        ]), [True, False, False], id='whole period'),
    ],
)
def test_add_corrections_data(
    yt_client, stripped_log_path, consumes_table_path, tariffed_dir, untariffed_dir, orders_dir,
    prev_published_untariffed_table_path, corrections_dir, corrections_meta, orders_skipped, run_job
):
    metadata = copy.deepcopy(CURR_LOG_TARIFF_META)
    metadata[CORRECTIONS_LOG_INTERVAL_KEY] = corrections_meta.to_meta()

    create_default_tables_fixture(
        yt_client, consumes_table_path, prev_published_untariffed_table_path, stripped_log_path,
        tariffed_dir, untariffed_dir, orders_dir, corrections_dir, metadata
    )

    consumes_counter = itertools.count(1)
    events_counter = itertools.count(1)

    orders = [
        OrderDataBuilder(1, consumes_counter, events_counter, corrections_skipped=orders_skipped[0])
            .add_consume(1)
            .add_event(1)
            .add_corrections_untariffed(9),
        OrderDataBuilder(2, consumes_counter, events_counter, corrections_skipped=orders_skipped[1])
            .add_consume(1 if orders_skipped[1] else 100)
            .add_event(1)
            .add_corrections_untariffed(99),
        OrderDataBuilder(3, consumes_counter, events_counter, corrections_skipped=orders_skipped[2])
            .add_consume(1 if orders_skipped[2] else 1000)
            .add_event(1)
            .add_corrections_untariffed(999),
    ]

    yt_client.write_table(
        TablePath(consumes_table_path, schema=CONSUMES_TABLE_SCHEMA),
        [o.sync_info for o in orders if not o.is_skipped]
    )
    yt_client.run_sort(consumes_table_path, sort_by=['ServiceID', 'EffectiveServiceOrderID'])

    yt_client.write_table(
        TablePath(prev_published_untariffed_table_path, schema=STRIPPED_LOG_TABLE_SCHEMA),
        itertools.chain.from_iterable(o.untariffed_events for o in orders)
    )
    yt_client.run_sort(prev_published_untariffed_table_path, sort_by=STRIPPED_LOG_TABLE_SORTED_BY)

    yt_client.write_table(
        TablePath(stripped_log_path, schema=STRIPPED_LOG_TABLE_SCHEMA),
        itertools.chain.from_iterable(o.new_events for o in orders)
    )
    yt_client.run_sort(stripped_log_path, sort_by=STRIPPED_LOG_TABLE_SORTED_BY)

    data = zip(
        [PREV_RUN_ID, CURR_RUN_ID, NEXT_RUN_ID],
        [PREV_CORRECTIONS_INTERVAL, CURR_CORRECTIONS_INTERVAL, NEXT_CORRECTIONS_INTERVAL],
        orders,
    )
    for run_id, interval, order in data:
        corrections_path = TablePath(ypath_join(corrections_dir, run_id), schema=CORRECTIONS_LOG_TABLE_SCHEMA)
        yt_client.write_table(
            corrections_path,
            order.corrections_untariffed,
        )
        cor_metadata = copy.deepcopy(CURR_LOG_TARIFF_META)
        cor_metadata[CORRECTIONS_LOG_INTERVAL_KEY] = interval.to_meta()
        set_log_tariff_meta(yt_client, ypath_join(corrections_dir, run_id), cor_metadata)

    with yt_client.Transaction(ping=False) as transaction:
        res_paths = run_job(yt_client, transaction)
    return get_result(yt_client, res_paths)

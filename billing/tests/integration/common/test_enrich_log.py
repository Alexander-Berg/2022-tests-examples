from operator import attrgetter
from itertools import chain
from typing import Sequence

import pytest
from hamcrest import assert_that, contains_inanyorder

from yt.wrapper import ypath_join, TablePath

from billing.log_tariffication.py.jobs.common import enrich_log
from billing.library.python.logmeta_utils.meta import (
    get_log_tariff_meta,
    set_log_tariff_meta,
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.log_tariffication.py.tests.utils import (
    check_node_is_locked,
)
from billing.log_tariffication.py.tests.constants import (
    CURR_RUN_ID, PREV_RUN_ID, CURR_LOG_INTERVAL,
    CURR_LOG_TARIFF_META, STREAM_LOG_TABLE_SCHEMA,
    PREV_LOG_TARIFF_META, NEXT_RUN_ID,
)
from billing.library.python.logfeller_utils.log_interval import (
    LB_META_ATTR,
)
from billing.library.python.logfeller_utils.tests.utils import (
    generate_stream_log_table_name,
)


TARIFFED_TABLE_SCHEMA = [
    {'name': 'OrderID', 'type': 'int64'},
    {'name': 'LBMessageUID', 'type': 'string'},
    {'name': 'consume_id', 'type': 'int64'},
    {'name': 'tariffed_qty', 'type': 'double'},
    {'name': 'tariffed_sum', 'type': 'double'},
    {'name': 'tariff_dt', 'type': 'uint64'},
]

ENRICHED_UNTARIFFED_TABLE_SCHEMA = STREAM_LOG_TABLE_SCHEMA + [
    {'name': 'LBMessageUID', 'type': 'string'},
]


@pytest.fixture(name='enrich_results_dir')
def enrich_results_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'enrich_results')


@pytest.fixture(name='enriched_log_dir')
def enriched_log_dir_fixture(yt_client, enrich_results_dir):
    return create_subdirectory(yt_client, enrich_results_dir, 'enriched_log')


@pytest.fixture(name='curr_enriched_log_path')
def curr_enriched_log_path_fixture(enriched_log_dir):
    return ypath_join(enriched_log_dir, CURR_RUN_ID)


@pytest.fixture(name='prev_enriched_log_path')
def prev_enriched_log_path_fixture(enriched_log_dir):
    return ypath_join(enriched_log_dir, PREV_RUN_ID)


@pytest.fixture(name='next_enriched_log_path')
def next_enriched_log_path_fixture(enriched_log_dir):
    return ypath_join(enriched_log_dir, NEXT_RUN_ID)


@pytest.fixture(name='published_full_untariffed_dir')
def published_full_untariffed_dir_fixture(yt_client, published_dir):
    return create_subdirectory(yt_client, published_dir, 'full_untariffed')


@pytest.fixture(name='prev_published_full_untariffed_table_path')
def prev_published_full_untariffed_table_path_fixture(yt_root, published_full_untariffed_dir):
    return ypath_join(published_full_untariffed_dir, PREV_RUN_ID)


@pytest.fixture(name='full_untariffed_out_dir')
def full_untariffed_out_dir_fixure(yt_client, enrich_results_dir):
    return create_subdirectory(yt_client, enrich_results_dir, 'full_untariffed_log')


@pytest.fixture(name='curr_full_untariffed_out_path')
def curr_full_untariffed_out_path_fixture(full_untariffed_out_dir):
    return ypath_join(full_untariffed_out_dir, CURR_RUN_ID)


class TariffedRow(object):
    def __init__(self, tariff_dt, consume_id, tariffed_qty, tariffed_sum):
        self.tariff_dt = tariff_dt
        self.consume_id = consume_id
        self.tariffed_qty = tariffed_qty
        self.tariffed_sum = tariffed_sum


class Row(object):
    def __init__(self, order_id, event_time, bucks,
                 topic_cluster, topic, partition, offset,
                 chunk_record_index, tariffed_rows=None,
                 fully_tariffed=True, has_full_part=True):
        self.order_id = order_id
        self.event_time = event_time
        self.bucks = bucks
        self.topic_cluster = topic_cluster
        self.topic = topic
        self.partition = partition
        self.offset = offset
        self.chunk_record_index = chunk_record_index
        self.tariffed_rows = tariffed_rows or []
        self.fully_tariffed = fully_tariffed
        self.has_full_part = has_full_part

    @property
    def lb_uid(self):
        return '@'.join(map(str, (
            self.topic_cluster, self.topic, self.partition,
            self.offset, self.chunk_record_index
        )))

    @property
    def log_table_rows(self):
        if self.has_full_part:
            yield next(self._log_table_rows)

    @property
    def _log_table_rows(self):
        yield {
            'OrderID': self.order_id,
            'EventTime': self.event_time,
            'Bucks': self.bucks,
            '_topic_cluster': self.topic_cluster,
            '_topic': self.topic,
            '_partition': self.partition,
            '_offset': self.offset,
            '_chunk_record_index': self.chunk_record_index,
        }

    @property
    def tariffed_table_rows(self):
        for row in self.tariffed_rows:
            yield {
                'OrderID': self.order_id,
                'tariff_dt': row.tariff_dt,
                'consume_id': row.consume_id,
                'tariffed_qty': row.tariffed_qty,
                'tariffed_sum': row.tariffed_sum,
                'LBMessageUID': self.lb_uid,
            }

    @property
    def has_untariffed(self):
        return len(self.tariffed_rows) == 0 or not self.fully_tariffed

    @property
    def enriched_table_rows(self):
        log_table_row = next(self._log_table_rows)
        if not self.has_full_part:
            for key in log_table_row:
                log_table_row[key] = None
        for tariffed_row in self.tariffed_table_rows:
            row = log_table_row.copy()
            row.update(tariffed_row)
            yield row

    @property
    def full_untariffed_out_table_rows(self):
        if self.has_full_part:
            yield {
                'OrderID': self.order_id,
                'EventTime': self.event_time,
                'Bucks': self.bucks,
                '_topic_cluster': self.topic_cluster,
                '_topic': self.topic,
                '_partition': self.partition,
                '_offset': self.offset,
                '_chunk_record_index': self.chunk_record_index,
                'LBMessageUID': self.lb_uid,
            }


@pytest.mark.parametrize(
    ['with_checks'],
    [
        pytest.param(True, id="with_checks"),
        pytest.param(False, id="without_checks"),
    ]
)
def test_run_query(yt_client, yql_client, stream_log_dir, curr_tariffed_table_path,
                   prev_published_full_untariffed_table_path, enriched_log_dir,
                   curr_enriched_log_path, prev_enriched_log_path, with_checks,
                   published_full_untariffed_dir):
    log_rows = [
        # with 2 consumes
        Row(1, 1587456560, 40, 'c1', 't1', 0, 16, 9,
            [TariffedRow(1587456565, 5, 50.0, 10.6),
             TariffedRow(1587456567, 6, 30.1, 15.2)]),
        # same order, not tariffed event
        Row(1, 1587456560, 20, 'c1', 't1', 0, 16, 10),
        # other order, part of consume from first order
        Row(2, 1587456561, 30, 'c1', 't1', 1, 15, 1,
            [TariffedRow(1587456567, 6, 50.1, 25.2)],
            fully_tariffed=False),
    ]

    full_untariffed_in_rows = [
        Row(1, 1587451561, 45, 'c1', 't1', 3, 10, 4,
            [TariffedRow(1587456567, 5, 53.1, 21.2)],
            fully_tariffed=False),
        # fully tariffed
        Row(3, 1587451561, 45, 'c1', 't1', 3, 10, 5,
            [TariffedRow(1587456566, 5, 53.1, 21.2)]),
        # remains untariffed
        Row(5, 1587456562, 20, 'c1', 't1', 0, 13, 10),
        # migration rows
        Row(6, 1587456565, 500, 'migr', 't2', 1, 2, 0,
            [TariffedRow(1587456565, 5, 500.0, 10.8)],
            has_full_part=False),
        Row(7, 1587456465, 600, 'migr', 't3', 1, 2, 0,
            has_full_part=False),
    ]

    all_rows = log_rows + full_untariffed_in_rows

    def get_rows_by_attr(attr: str, source_rows: Sequence[Row]) -> Sequence[dict]:
        return list(chain.from_iterable(map(attrgetter(attr), source_rows)))

    # Prepare log table
    log_table_name = generate_stream_log_table_name()
    log_table_path = TablePath(
        ypath_join(stream_log_dir, log_table_name),
        schema=STREAM_LOG_TABLE_SCHEMA
    )
    yt_client.create('table', log_table_path, attributes={
        LB_META_ATTR: CURR_LOG_INTERVAL.to_meta(),
        'schema': STREAM_LOG_TABLE_SCHEMA
    })
    yt_client.write_table(log_table_path,
                          get_rows_by_attr('log_table_rows', log_rows))

    # Prepare full untariffed incoming table
    prev_published_full_untariffed_table_path = TablePath(
        prev_published_full_untariffed_table_path,
        schema=ENRICHED_UNTARIFFED_TABLE_SCHEMA
    )
    yt_client.create('table', prev_published_full_untariffed_table_path, attributes={
        'schema': ENRICHED_UNTARIFFED_TABLE_SCHEMA
    })
    set_log_tariff_meta(yt_client, prev_published_full_untariffed_table_path, PREV_LOG_TARIFF_META)
    yt_client.write_table(prev_published_full_untariffed_table_path,
                          get_rows_by_attr('full_untariffed_out_table_rows',
                                           full_untariffed_in_rows))

    # Prepare tariffed table
    curr_tariffed_table_path = TablePath(
        curr_tariffed_table_path,
        schema=TARIFFED_TABLE_SCHEMA
    )
    yt_client.create('table', curr_tariffed_table_path, attributes={
        'schema': TARIFFED_TABLE_SCHEMA
    })
    set_log_tariff_meta(yt_client, curr_tariffed_table_path, CURR_LOG_TARIFF_META)
    yt_client.write_table(curr_tariffed_table_path,
                          get_rows_by_attr('tariffed_table_rows', all_rows))

    if not with_checks:
        # Will be deleted by job
        yt_client.create('table', curr_enriched_log_path, attributes={
            'schema': TARIFFED_TABLE_SCHEMA
        })
        set_log_tariff_meta(yt_client, curr_enriched_log_path, PREV_LOG_TARIFF_META)

    # Valid previous result
    yt_client.create('table', prev_enriched_log_path)
    set_log_tariff_meta(yt_client, prev_enriched_log_path, PREV_LOG_TARIFF_META)

    with yt_client.Transaction(ping=False) as transaction:
        assert enrich_log.run_job(
            yt_client, yql_client,
            log_dir=stream_log_dir,
            tariffed_path=str(curr_tariffed_table_path),
            enriched_log_dir=enriched_log_dir,
            full_untariffed_dir=published_full_untariffed_dir,
            transaction=transaction,
            lock_wait_seconds=1,
            with_checks=with_checks
        ) == curr_enriched_log_path
        check_node_is_locked(enriched_log_dir)

    assert get_log_tariff_meta(yt_client, curr_enriched_log_path) == CURR_LOG_TARIFF_META

    assert_that(
        list(yt_client.read_table(curr_enriched_log_path)),
        contains_inanyorder(*get_rows_by_attr('enriched_table_rows', all_rows))
    )


def test_already_done(
    yt_client, yql_client, stream_log_dir, curr_tariffed_table_path,
    enriched_log_dir, curr_enriched_log_path, yt_transaction, caplog,
    published_full_untariffed_dir,
    prev_published_full_untariffed_table_path,
    curr_untariffed_table_path
):
    for path in (curr_tariffed_table_path, curr_enriched_log_path):
        yt_client.create('table', path)
        set_log_tariff_meta(yt_client, path, CURR_LOG_TARIFF_META)

    yt_client.create('table', prev_published_full_untariffed_table_path)
    set_log_tariff_meta(yt_client, prev_published_full_untariffed_table_path, PREV_LOG_TARIFF_META)

    assert enrich_log.run_job(
        yt_client, yql_client,
        log_dir=stream_log_dir,
        tariffed_path=curr_tariffed_table_path,
        enriched_log_dir=enriched_log_dir,
        full_untariffed_dir=published_full_untariffed_dir,
        transaction=yt_transaction,
    ) == curr_enriched_log_path

    assert "Already done" in caplog.text


@pytest.mark.parametrize(
    ['table_meta_pairs'],
    [
        pytest.param((
            ('curr_enriched_log_path', PREV_LOG_TARIFF_META),
            ('curr_full_untariffed_out_path', CURR_LOG_TARIFF_META)
        ),),
        # pytest.param((
        #     ('curr_enriched_log_path', CURR_LOG_TARIFF_META),
        #     ('curr_full_untariffed_out_path', PREV_LOG_TARIFF_META)
        # ),),
        pytest.param((
            ('next_enriched_log_path', CURR_LOG_TARIFF_META),
        ),),
        pytest.param((
            ('prev_enriched_log_path', CURR_LOG_TARIFF_META),
        ),),
        pytest.param((
            ('prev_published_full_untariffed_table_path', CURR_LOG_TARIFF_META),
        ),),
    ]
)
def test_checks(yt_client, enriched_log_dir, get_fixture,
                table_meta_pairs, prev_published_full_untariffed_table_path):
    yt_client.create('table', prev_published_full_untariffed_table_path)
    set_log_tariff_meta(yt_client, prev_published_full_untariffed_table_path,
                        PREV_LOG_TARIFF_META)

    for table_path_fixture, meta in table_meta_pairs:
        table_path = get_fixture(table_path_fixture)
        yt_client.create('table', table_path, force=True)
        set_log_tariff_meta(yt_client, table_path, meta)

    with pytest.raises(AssertionError):
        enrich_log.do_checks(
            yt_client, enriched_log_dir, CURR_LOG_TARIFF_META,
            prev_published_full_untariffed_table_path
        )


def test_processed_log(
    yt_client, yql_client, yt_root, curr_tariffed_table_path, curr_untariffed_table_path,
    prev_published_full_untariffed_table_path, enriched_log_dir, full_untariffed_out_dir, published_full_untariffed_dir,
):
    # Preparing processed log
    processed_log_dir = create_subdirectory(yt_client, yt_root, 'processed_log')
    processed_log_path = ypath_join(processed_log_dir, PREV_RUN_ID)
    processed_log_schema = [
        {'name': 'UID', 'type': 'int64'},
        {'name': 'qty', 'type': 'double'},
        {'name': 'prop1', 'type': 'string'},
    ]
    yt_client.create('table', processed_log_path, attributes={
        'schema': processed_log_schema
    })
    processed_log_rows = [
        {'UID': 3, 'qty': 0.3, 'prop1': 'c'},
        {'UID': 4, 'qty': 0.4, 'prop1': 'd'},
    ]
    yt_client.write_table(processed_log_path, processed_log_rows)
    set_log_tariff_meta(yt_client, processed_log_path, CURR_LOG_TARIFF_META)

    # Preparing tariffed table
    tariffed_table_schema = [
        {'name': 'UID', 'type': 'int64'},
        {'name': 'sum', 'type': 'double'},
    ]
    yt_client.create('table', curr_tariffed_table_path, attributes={
        'schema': tariffed_table_schema
    })
    tariffed_rows = [
        {'UID': 1, 'sum': 0.1},
        {'UID': 2, 'sum': 0.2}
    ]
    yt_client.write_table(curr_tariffed_table_path, tariffed_rows)
    set_log_tariff_meta(yt_client, curr_tariffed_table_path, CURR_LOG_TARIFF_META)

    # Preparing untariffed table
    # untariffed_table_schema = [
    #     {'name': 'UID', 'type': 'int64'},
    #     {'name': 'qty', 'type': 'double'},
    # ]
    # yt_client.create('table', curr_untariffed_table_path, attributes={
    #     'schema': untariffed_table_schema
    # })
    # untariffed_rows = [
    #     {'UID': 1, 'qty': 0.1},
    #     {'UID': 3, 'qty': 0.3}
    # ]
    # yt_client.write_table(curr_untariffed_table_path, untariffed_rows)
    # set_log_tariff_meta(yt_client, curr_untariffed_table_path, CURR_LOG_TARIFF_META)

    # Preparing published_full_untariffed_dir
    yt_client.create('table', prev_published_full_untariffed_table_path, attributes={
        'schema': processed_log_schema
    })
    full_untariffed_rows = [
        {'UID': 1, 'qty': 0.1, 'prop1': 'a'},
        {'UID': 2, 'qty': 0.2, 'prop1': 'b'},
    ]
    yt_client.write_table(prev_published_full_untariffed_table_path, full_untariffed_rows)
    set_log_tariff_meta(yt_client, prev_published_full_untariffed_table_path, PREV_LOG_TARIFF_META)

    with yt_client.Transaction(ping=False) as transaction:
        curr_enriched_log_path = enrich_log.run_job(
            yt_client, yql_client,
            log_dir=processed_log_dir,
            tariffed_path=str(curr_tariffed_table_path),
            enriched_log_dir=enriched_log_dir,
            full_untariffed_dir=published_full_untariffed_dir,
            transaction=transaction,
            log_type=enrich_log.LogType.processed,
            join_column='UID',
        )

    expected_enriched_rows = [
        {'UID': 1, 'sum': 0.1, 'qty': 0.1, 'prop1': 'a'},
        {'UID': 2, 'sum': 0.2, 'qty': 0.2, 'prop1': 'b'},
    ]

    assert_that(
        list(yt_client.read_table(curr_enriched_log_path)),
        contains_inanyorder(*expected_enriched_rows)
    )

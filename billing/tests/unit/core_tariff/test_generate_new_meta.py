# -*- coding: utf-8 -*-
import contextlib
import pytest
import arrow
import copy
import hamcrest as hm
from unittest import mock

from billing.log_tariffication.py.jobs.core_tariff import generate_new_meta
from billing.log_tariffication.py.jobs.core_tariff.generate_new_meta import GenerateAutoOverdraftState
from billing.log_tariffication.py.lib.constants import MSK_TZ
from billing.library.python.logfeller_utils.log_interval import LB_META_ATTR
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)

from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
    CURR_LOG_INTERVAL,
    NEXT_LOG_INTERVAL,
    PREV_LOG_TARIFF_META,
    CURR_LOG_TARIFF_META,
    TARIFF_DATE_START_HOUR,
)
from billing.log_tariffication.py.tests.utils import (
    patch_generate_run_id,
)


NOW = arrow.get('2020-06-06 12:00:00').replace(tzinfo=MSK_TZ)
TODAY = NOW.floor('day')
PAST_MONTH = TODAY.shift(months=-1)
OLD_MONTH = TODAY.shift(months=-2)

LOG_DIR = '//logs/bs-billable-event-log/stream/5min/'
MIGRATION_DIR = '//home/migration/'
MIGRATION_CLUSTER = 'test/migration/direct'
MIGRATION_META_KEY = 'corrections_log_interval'

ZERO_MIGRATION_INTERVAL = LogInterval([
    Subinterval(MIGRATION_CLUSTER, 'iva', 0, 0, 0),
    Subinterval(MIGRATION_CLUSTER, 'vla', 0, 10, 10),
])

PREV_MIGRATION_INTERVAL = LogInterval([
    Subinterval(MIGRATION_CLUSTER, 'iva', 0, 0, 1),
    Subinterval(MIGRATION_CLUSTER, 'vla', 0, 0, 10),
])
CURR_MIGRATION_INTERVAL = LogInterval([
    Subinterval(MIGRATION_CLUSTER, 'iva', 0, 1, 2),
    Subinterval(MIGRATION_CLUSTER, 'vla', 0, 10, 20),
])
NEXT_MIGRATION_INTERVAL = LogInterval([
    Subinterval(MIGRATION_CLUSTER, 'iva', 0, 2, 3),
    Subinterval(MIGRATION_CLUSTER, 'vla', 0, 20, 30),
])

MIGRATION_ZERO_LOG_TARIFF_META = copy.deepcopy(PREV_LOG_TARIFF_META)
MIGRATION_ZERO_LOG_TARIFF_META[MIGRATION_META_KEY] = ZERO_MIGRATION_INTERVAL.to_meta()

MIGRATION_PREV_LOG_TARIFF_META = copy.deepcopy(PREV_LOG_TARIFF_META)
MIGRATION_PREV_LOG_TARIFF_META[MIGRATION_META_KEY] = PREV_MIGRATION_INTERVAL.to_meta()

MIGRATION_CURR_LOG_TARIFF_META = copy.deepcopy(CURR_LOG_TARIFF_META)
MIGRATION_CURR_LOG_TARIFF_META[MIGRATION_META_KEY] = CURR_MIGRATION_INTERVAL.to_meta()


def get_new_migration_meta(table):
    return {'table': MIGRATION_DIR + table}


def get_migration_meta(table, interval=CURR_MIGRATION_INTERVAL):
    return {'table': MIGRATION_DIR + table, MIGRATION_META_KEY: interval.to_meta()}


def mnclose_state(dt=arrow.get('2020-06-30 00:00:00').replace(tzinfo=MSK_TZ), state='closed'):
    res = {
        'dt': dt.strftime('%Y-%m-%d'),
        'status': state,
    }
    return res


@contextlib.contextmanager
def patch_now(dt):
    with mock.patch('arrow.now', lambda: dt):
        yield


@contextlib.contextmanager
def yt_client_mock():
    with mock.patch('yt.wrapper.YtClient') as m:
        m.exists.return_value = True
        yield m


class Table:
    def __init__(self, name, attributes):
        self.name: str = name
        self.attributes: dict = attributes

    def __str__(self):
        return self.name


class TestGenerateNewMeta:
    def test_unprocessed_migration(self):
        with yt_client_mock() as yt_client:
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                MIGRATION_CURR_LOG_TARIFF_META,
                MIGRATION_CURR_LOG_TARIFF_META,
                get_new_migration_meta('2020-11-09'),
                get_migration_meta('2020-11-08'),
                [get_migration_meta('2020-11-08'), get_migration_meta('2020-11-07')],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state(),
            )
        assert new_tariff_meta is None
        assert new_migration_meta is None

    def test_unprocessed_tariffication(self):
        with yt_client_mock() as yt_client:
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                MIGRATION_CURR_LOG_TARIFF_META,
                MIGRATION_PREV_LOG_TARIFF_META,
                get_new_migration_meta('2020-11-09'),
                get_migration_meta('2020-11-08'),
                [get_migration_meta('2020-11-08')],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state(),
            )
        assert new_tariff_meta is None
        assert new_migration_meta is None

    def test_no_migration_no_tariff(self):
        with yt_client_mock() as yt_client:
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                MIGRATION_CURR_LOG_TARIFF_META,
                MIGRATION_CURR_LOG_TARIFF_META,
                get_new_migration_meta('2020-11-08'),
                get_migration_meta('2020-11-08'),
                [get_migration_meta('2020-11-08')],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state(),
            )
        assert new_tariff_meta is None
        assert new_migration_meta is None

    @pytest.mark.parametrize(
        'tariff_meta, res_log_interval',
        [
            pytest.param(MIGRATION_PREV_LOG_TARIFF_META, PREV_LOG_TARIFF_META, id='prev'),
            pytest.param(MIGRATION_CURR_LOG_TARIFF_META, CURR_LOG_TARIFF_META, id='curr'),
        ],
    )
    def test_new_migration(self, tariff_meta, res_log_interval):
        """Create new corrections interval"""
        with patch_generate_run_id(NEXT_RUN_ID), yt_client_mock() as yt_client:
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                tariff_meta,
                tariff_meta,
                get_new_migration_meta('2020-11-09'),
                get_migration_meta('2020-11-08', CURR_MIGRATION_INTERVAL),
                [get_migration_meta('2020-11-08', CURR_MIGRATION_INTERVAL)],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state(),
            )
        assert new_tariff_meta is None

        match_data = {
            'table': MIGRATION_DIR + '2020-11-09',
            'run_id': NEXT_RUN_ID,
            'log_interval': hm.has_entries(res_log_interval['log_interval']),
            MIGRATION_META_KEY: hm.has_entries({
                'topics': hm.contains(
                    hm.has_entries({
                        'cluster': MIGRATION_CLUSTER,
                        'topic': 'iva',
                        'partitions': hm.contains(
                            hm.has_entries({
                                'partition': 0,
                                'first_offset': 2,
                                'next_offset': 3,
                            }),
                        ),
                    }),
                    hm.has_entries({
                        'cluster': MIGRATION_CLUSTER,
                        'topic': 'vla',
                        'partitions': hm.contains(
                            hm.has_entries({
                                'partition': 0,
                                'first_offset': 20,
                                'next_offset': 21,
                            }),
                        ),
                    }),
                ),
            }),
        }
        hm.assert_that(
            new_migration_meta,
            hm.has_entries(match_data),
        )

    def test_first_migration_error(self):
        with patch_generate_run_id(NEXT_RUN_ID), yt_client_mock() as yt_client:
            with pytest.raises(AssertionError) as exc_info:
                generate_new_meta.run_job(
                    yt_client,
                    CURR_LOG_TARIFF_META,
                    CURR_LOG_TARIFF_META,
                    get_new_migration_meta('2020-11-09'),
                    get_migration_meta('2020-11-08', CURR_MIGRATION_INTERVAL),
                    [get_migration_meta('2020-11-08')],
                    LOG_DIR,
                    TARIFF_DATE_START_HOUR,
                    mnclose_state(),
                )
        assert exc_info.value.args == ('There is no a correction interval for the first migration.',)

    def test_invalid_tariff_correction_interval(self):
        with patch_generate_run_id(NEXT_RUN_ID), yt_client_mock() as yt_client:
            with pytest.raises(AssertionError) as exc_info:
                generate_new_meta.run_job(
                    yt_client,
                    MIGRATION_CURR_LOG_TARIFF_META,
                    MIGRATION_CURR_LOG_TARIFF_META,
                    get_new_migration_meta('2020-11-09'),
                    get_migration_meta('2020-11-08', PREV_MIGRATION_INTERVAL),
                    [get_migration_meta('2020-11-08', PREV_MIGRATION_INTERVAL)],
                    LOG_DIR,
                    TARIFF_DATE_START_HOUR,
                    mnclose_state(),
                )
        assert exc_info.value.args == ('Invalid correction interval in tariffication meta.',)

    def test_migration_table_not_exist(self):
        with yt_client_mock() as yt_client:
            yt_client.exists.return_value = False
            with pytest.raises(AssertionError) as exc_info:
                generate_new_meta.run_job(
                    yt_client,
                    MIGRATION_CURR_LOG_TARIFF_META,
                    MIGRATION_CURR_LOG_TARIFF_META,
                    get_new_migration_meta('2020-11-09'),
                    get_migration_meta('2020-11-08', CURR_MIGRATION_INTERVAL),
                    [get_migration_meta('2020-11-08')],
                    LOG_DIR,
                    TARIFF_DATE_START_HOUR,
                    mnclose_state(),
                )
        assert exc_info.value.args == ('Invalid table for migration.',)

    @pytest.mark.parametrize(
        'tariff_meta',
        [PREV_LOG_TARIFF_META, MIGRATION_PREV_LOG_TARIFF_META],
    )
    def test_empty_migration_meta(self, tariff_meta):
        with yt_client_mock() as yt_client:
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                tariff_meta,
                tariff_meta,
                {},
                {},
                [],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state(),
            )
        assert new_tariff_meta is None
        assert new_migration_meta is None

    @pytest.mark.parametrize(
        'tariff_meta, has_migration',
        [
            pytest.param(PREV_LOG_TARIFF_META, False, id='wo migration meta'),
            pytest.param(MIGRATION_ZERO_LOG_TARIFF_META, True, id='w start migration meta'),
        ],
    )
    def test_tariffication(self, tariff_meta, has_migration):
        with patch_now(NOW), yt_client_mock() as yt_client:
            yt_client.list.return_value = [
                Table(CURR_RUN_ID, {LB_META_ATTR: CURR_LOG_INTERVAL.to_meta()}),
                Table(NEXT_RUN_ID, {LB_META_ATTR: NEXT_LOG_INTERVAL.to_meta()}),
            ]
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                tariff_meta,
                tariff_meta,
                {},
                {},
                [],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state(),
            )
        assert new_migration_meta is None

        match_data = {
            'run_id': NOW.isoformat().split('+')[0],
            'prev_run_id': PREV_RUN_ID,
            'tariff_date': 1591477200,
            'generate_auto_overdraft': GenerateAutoOverdraftState.FINISHED.value,
            'auto_overdraft_dt': mnclose_state()['dt'],
            'log_interval': hm.has_entries({
                'topics': hm.contains(
                    hm.has_entries({
                        'topic': 't1',
                        'cluster': 'c1',
                        'partitions': hm.contains(
                            hm.has_entries({'partition': 0, 'first_offset': 10, 'next_offset': 30}),
                            hm.has_entries({'partition': 1, 'first_offset': 15, 'next_offset': 35}),
                        ),
                    }),
                ),
            }),
        }
        if has_migration:
            match_data[MIGRATION_META_KEY] = hm.has_entries({
                'topics': hm.contains(
                    hm.has_entries({
                        'cluster': MIGRATION_CLUSTER,
                        'topic': 'iva',
                        'partitions': hm.contains(
                            hm.has_entries({'partition': 0, 'first_offset': 0, 'next_offset': 0}),
                        ),
                    }),
                    hm.has_entries({
                        'cluster': MIGRATION_CLUSTER,
                        'topic': 'vla',
                        'partitions': hm.contains(
                            hm.has_entries({'partition': 0, 'first_offset': 10, 'next_offset': 10}),
                        ),
                    }),
                ),
            })
        hm.assert_that(
            new_tariff_meta,
            hm.all_of(
                hm.has_length(len(match_data)),
                hm.has_entries(match_data),
            ),
        )

    @pytest.mark.parametrize(
        'migration_interval, res_log',
        [
            pytest.param(ZERO_MIGRATION_INTERVAL, (0, 0, 10, 10), id='zero'),
            pytest.param(PREV_MIGRATION_INTERVAL, (0, 1, 10, 10), id='prev'),
            pytest.param(NEXT_MIGRATION_INTERVAL, (0, 3, 10, 30), id='next'),
        ],
    )
    def test_tariffication_w_empty_migration(self, migration_interval, res_log):
        """В тарификации пока нулевой интервал и теперь добавляется новый интервал корректировок.
        """
        with patch_now(NOW), yt_client_mock() as yt_client:
            yt_client.list.return_value = [
                Table(CURR_RUN_ID, {LB_META_ATTR: CURR_LOG_INTERVAL.to_meta()}),
                Table(NEXT_RUN_ID, {LB_META_ATTR: NEXT_LOG_INTERVAL.to_meta()}),
            ]
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                MIGRATION_ZERO_LOG_TARIFF_META,
                MIGRATION_ZERO_LOG_TARIFF_META,
                get_new_migration_meta('2020-11-08'),
                get_migration_meta('2020-11-08', migration_interval),
                [get_migration_meta('2020-11-08', migration_interval)],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state(),
            )
        assert new_migration_meta is None

        v1, v2, v3, v4 = res_log
        match_data = {
            'run_id': NOW.isoformat().split('+')[0],
            'prev_run_id': PREV_RUN_ID,
            MIGRATION_META_KEY: hm.has_entries({
                'topics': hm.contains(
                    hm.has_entries({
                        'cluster': MIGRATION_CLUSTER,
                        'topic': 'iva',
                        'partitions': hm.contains(
                            hm.has_entries({'partition': 0, 'first_offset': v1, 'next_offset': v2}),
                        ),
                    }),
                    hm.has_entries({
                        'cluster': MIGRATION_CLUSTER,
                        'topic': 'vla',
                        'partitions': hm.contains(
                            hm.has_entries({'partition': 0, 'first_offset': v3, 'next_offset': v4}),
                        ),
                    }),
                ),
            }),
        }

        hm.assert_that(
            new_tariff_meta,
            hm.has_entries(match_data),
        )

    @pytest.mark.parametrize(
        'migration_interval, log_res',
        [
            pytest.param(PREV_MIGRATION_INTERVAL, (1, 1, 10, 10), id='no new migration'),
            pytest.param(CURR_MIGRATION_INTERVAL, (1, 2, 10, 20), id='w new migration'),
            pytest.param(NEXT_MIGRATION_INTERVAL, (1, 3, 10, 30), id='w new migration 2'),
        ],
    )
    def test_tariffication_w_migration(self, migration_interval, log_res):
        """Include all migrated intervals to the next tariffication cycle.
        """
        with patch_now(NOW), yt_client_mock() as yt_client:
            yt_client.list.return_value = [
                Table(CURR_RUN_ID, {LB_META_ATTR: CURR_LOG_INTERVAL.to_meta()}),
                Table(NEXT_RUN_ID, {LB_META_ATTR: NEXT_LOG_INTERVAL.to_meta()}),
            ]
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                MIGRATION_PREV_LOG_TARIFF_META,
                MIGRATION_PREV_LOG_TARIFF_META,
                get_new_migration_meta('2020-11-08'),
                get_migration_meta('2020-11-08', migration_interval),
                [get_migration_meta('2020-11-08', migration_interval)],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state(),
            )
        assert new_migration_meta is None

        v1, v2, v3, v4 = log_res
        match_data = {
            'run_id': NOW.isoformat().split('+')[0],
            'prev_run_id': PREV_RUN_ID,
            'log_interval': hm.has_entries({
                'topics': hm.contains(
                    hm.has_entries({
                        'topic': 't1',
                        'cluster': 'c1',
                        'partitions': hm.contains(
                            hm.has_entries({'partition': 0, 'first_offset': 10, 'next_offset': 30}),
                            hm.has_entries({'partition': 1, 'first_offset': 15, 'next_offset': 35}),
                        ),
                    }),
                ),
            }),
            MIGRATION_META_KEY: hm.has_entries({
                'topics': hm.contains(
                    hm.has_entries({
                        'cluster': MIGRATION_CLUSTER,
                        'topic': 'iva',
                        'partitions': hm.contains(
                            hm.has_entries({'partition': 0, 'first_offset': v1, 'next_offset': v2}),
                        ),
                    }),
                    hm.has_entries({
                        'cluster': MIGRATION_CLUSTER,
                        'topic': 'vla',
                        'partitions': hm.contains(
                            hm.has_entries({'partition': 0, 'first_offset': v3, 'next_offset': v4}),
                        ),
                    }),
                ),
            }),
        }

        hm.assert_that(
            new_tariff_meta,
            hm.has_entries(match_data),
        )

    def test_wo_new_interval(self):
        """Run tariffication only w new real interval"""
        with patch_now(NOW), yt_client_mock() as yt_client:
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                MIGRATION_PREV_LOG_TARIFF_META,
                MIGRATION_PREV_LOG_TARIFF_META,
                get_new_migration_meta('2020-11-08'),
                get_migration_meta('2020-11-08', NEXT_MIGRATION_INTERVAL),
                [get_migration_meta('2020-11-08', NEXT_MIGRATION_INTERVAL)],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state(),
            )
        assert new_tariff_meta is None
        assert new_migration_meta is None

    def test_broken_corrections_interval(self):
        with yt_client_mock() as yt_client:
            yt_client.exists.return_value = False
            with pytest.raises(AssertionError) as exc_info:
                yt_client.list.return_value = [
                    Table(NEXT_RUN_ID, {LB_META_ATTR: NEXT_LOG_INTERVAL.to_meta()}),
                ]
                generate_new_meta.run_job(
                    yt_client,
                    MIGRATION_CURR_LOG_TARIFF_META,
                    MIGRATION_CURR_LOG_TARIFF_META,
                    get_new_migration_meta('2020-11-08'),
                    get_migration_meta('2020-11-08', PREV_MIGRATION_INTERVAL),
                    [get_migration_meta('2020-11-08')],
                    LOG_DIR,
                    TARIFF_DATE_START_HOUR,
                    mnclose_state(),
                )
        assert exc_info.value.args == ('Broken corrections interval.',)

    @pytest.mark.parametrize(
        'current_time, generate_auto_overdraft, expected_tariff_date',
        [
            pytest.param(
                arrow.get('2020-07-01 01:00:00').replace(tzinfo=MSK_TZ),
                GenerateAutoOverdraftState.NOT_STARTED,
                1593550800,
                id='before tariff date start hour not_started'
            ),
            pytest.param(
                arrow.get('2020-07-01 01:00:00').replace(tzinfo=MSK_TZ),
                GenerateAutoOverdraftState.IN_PROGRESS,
                1593550800,
                id='before tariff date start hour in_progress'
            ),
            pytest.param(
                arrow.get('2020-07-01 01:00:00').replace(tzinfo=MSK_TZ),
                GenerateAutoOverdraftState.FINISHED,
                1593550800,
                id='before tariff date start hour finished'
            ),
            pytest.param(
                arrow.get('2020-07-01 06:00:00').replace(tzinfo=MSK_TZ),
                GenerateAutoOverdraftState.NOT_STARTED,
                1593550800,
                id='after tariff date start hour not_started'
            ),
            pytest.param(
                arrow.get('2020-07-01 06:00:00').replace(tzinfo=MSK_TZ),
                GenerateAutoOverdraftState.IN_PROGRESS,
                1593550800,
                id='after tariff date start hour in_progress'
            ),
            pytest.param(
                arrow.get('2020-07-01 06:00:00').replace(tzinfo=MSK_TZ),
                GenerateAutoOverdraftState.FINISHED,
                1593637200,
                id='after tariff date start hour finished'
            ),
            pytest.param(
                arrow.get('2020-07-02 01:00:00').replace(tzinfo=MSK_TZ),
                GenerateAutoOverdraftState.NOT_STARTED,
                1593550800,
                id='next day before tariff date start hour not_started'
            ),
            pytest.param(
                arrow.get('2020-07-02 01:00:00').replace(tzinfo=MSK_TZ),
                GenerateAutoOverdraftState.IN_PROGRESS,
                1593550800,
                id='next day before tariff date start hour in_progress'
            ),
            pytest.param(
                arrow.get('2020-07-02 01:00:00').replace(tzinfo=MSK_TZ),
                GenerateAutoOverdraftState.FINISHED,
                1593637200,
                id='next day before tariff date start hour finished'
            ),
        ],
    )
    def test_tariff_date(self, current_time, generate_auto_overdraft, expected_tariff_date):
        with patch_now(current_time):
            tariff_date = generate_new_meta.get_tariff_date_meta(
                tariff_date_start_hour=TARIFF_DATE_START_HOUR, generate_auto_overdraft=generate_auto_overdraft
            )
        hm.assert_that(
            tariff_date,
            hm.equal_to(expected_tariff_date),
        )

    @pytest.mark.parametrize(
        'mnclose_state, last_auto_overdraft_dt, expected_generate_auto_overdraft',
        [
            pytest.param(
                dict(dt='2020-05-31', status='open'),
                '2020-05-31',
                GenerateAutoOverdraftState.NOT_STARTED.value,
                id='before previous months'
            ),
            pytest.param(
                dict(dt='2020-06-30', status='open'),
                '2020-05-31',
                GenerateAutoOverdraftState.IN_PROGRESS.value,
                id='generate auto_overdraft'
            ),
            pytest.param(
                dict(dt='2020-06-30', status='open'),
                '2020-06-30',
                GenerateAutoOverdraftState.FINISHED.value,
                id='auto_overdraft generated'
            ),
            pytest.param(
                dict(dt='2020-06-30', status='closed'),
                '2020-05-31',
                GenerateAutoOverdraftState.FINISHED.value,
                id='closed period'
            ),
        ]
    )
    def test_auto_overdraft_meta(self, mnclose_state, last_auto_overdraft_dt, expected_generate_auto_overdraft):
        AUTO_OVERDRAFT_PREV_LOG_TARIFF_META = copy.deepcopy(PREV_LOG_TARIFF_META)
        AUTO_OVERDRAFT_PREV_LOG_TARIFF_META['auto_overdraft_dt'] = last_auto_overdraft_dt
        now = arrow.get('2020-07-01 12:00:00').replace(tzinfo=MSK_TZ)
        with patch_now(now), yt_client_mock() as yt_client:
            yt_client.list.return_value = [
                Table(CURR_RUN_ID, {LB_META_ATTR: CURR_LOG_INTERVAL.to_meta()}),
                Table(NEXT_RUN_ID, {LB_META_ATTR: NEXT_LOG_INTERVAL.to_meta()}),
            ]
            new_tariff_meta, new_migration_meta = generate_new_meta.run_job(
                yt_client,
                AUTO_OVERDRAFT_PREV_LOG_TARIFF_META,
                AUTO_OVERDRAFT_PREV_LOG_TARIFF_META,
                {},
                {},
                [],
                LOG_DIR,
                TARIFF_DATE_START_HOUR,
                mnclose_state,
            )
        assert new_migration_meta is None

        expected_tariff_date = 1593637200 if expected_generate_auto_overdraft == GenerateAutoOverdraftState.FINISHED.value else 1593550800

        match_data = {
            'run_id': now.isoformat().split('+')[0],
            'prev_run_id': PREV_RUN_ID,
            'tariff_date': expected_tariff_date,
            'generate_auto_overdraft': expected_generate_auto_overdraft,
            'auto_overdraft_dt': mnclose_state['dt'],
            'log_interval': hm.has_entries({
                'topics': hm.contains(
                    hm.has_entries({
                        'topic': 't1',
                        'cluster': 'c1',
                        'partitions': hm.contains(
                            hm.has_entries({'partition': 0, 'first_offset': 10, 'next_offset': 30}),
                            hm.has_entries({'partition': 1, 'first_offset': 15, 'next_offset': 35}),
                        ),
                    }),
                ),
            }),
        }
        hm.assert_that(
            new_tariff_meta,
            hm.all_of(
                hm.has_length(len(match_data)),
                hm.has_entries(match_data),
            ),
        )

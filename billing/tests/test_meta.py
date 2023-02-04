# -*- coding: utf-8 -*-

import pytest
import arrow

from yt.wrapper import (
    ypath_join,
)

from billing.library.python.logmeta_utils.meta import (
    RangeBorderSelectType,
    ResultChecker,
    check_last_table_path,
    get_tables_range,
    generate_run_id,
)
from billing.library.python.logmeta_utils.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    RUN_ID_KEY,
)
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.library.python.logfeller_utils.tests.utils import (
    mk_interval,
)

CURR_RUN_ID = generate_run_id(arrow.Arrow(2020, 6, 6, 6))
PREV_RUN_ID = generate_run_id(arrow.Arrow(2020, 6, 6, 6).shift(minutes=-5))
NEXT_RUN_ID = generate_run_id(arrow.Arrow(2020, 6, 6, 6).shift(minutes=5))

PREV_LOG_INTERVAL = LogInterval([
    Subinterval('c1', 't1', 0, 0, 10),
    Subinterval('c1', 't1', 1, 10, 15),
])

CURR_LOG_INTERVAL = LogInterval([
    Subinterval('c1', 't1', 0, 10, 20),
    Subinterval('c1', 't1', 1, 15, 25),
])

NEXT_LOG_INTERVAL = LogInterval([
    Subinterval('c1', 't1', 0, 20, 30),
    Subinterval('c1', 't1', 1, 25, 35),
])


def create_table(yt_client, path, meta):
    yt_client.create(
        'table',
        path,
        attributes={LOG_TARIFF_META_ATTR: meta}
    )


class TestResultsChecker:
    def test_empty(self, yt_client, yt_root):
        dir1 = create_subdirectory(yt_client, yt_root, 'dir1')
        dir2 = create_subdirectory(yt_client, yt_root, 'dir2')

        res_checker = ResultChecker(
            yt_client,
            {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID}
        )
        (path1, path2), is_all_processed = res_checker.get_paths(dir1, dir2)
        assert is_all_processed is False
        assert path1 == ypath_join(dir1, CURR_RUN_ID)
        assert path2 == ypath_join(dir2, CURR_RUN_ID)

    @pytest.mark.parametrize('strict_sequence', [False, True])
    def test_precedes(self, yt_client, yt_root, strict_sequence):
        dir1 = create_subdirectory(yt_client, yt_root, 'dir1')
        create_table(yt_client, ypath_join(dir1, PREV_RUN_ID), {LOG_INTERVAL_KEY: PREV_LOG_INTERVAL.to_meta()})

        dir2 = create_subdirectory(yt_client, yt_root, 'dir2')
        create_table(yt_client, ypath_join(dir2, PREV_RUN_ID), {LOG_INTERVAL_KEY: PREV_LOG_INTERVAL.to_meta()})

        res_checker = ResultChecker(
            yt_client,
            {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID},
            strict_sequence
        )
        (path1, path2), is_all_processed = res_checker.get_paths(dir1, dir2)
        assert is_all_processed is False
        assert path1 == ypath_join(dir1, CURR_RUN_ID)
        assert path2 == ypath_join(dir2, CURR_RUN_ID)

    def test_processed(self, yt_client, yt_root):
        dir1 = create_subdirectory(yt_client, yt_root, 'dir1')
        create_table(
            yt_client,
            ypath_join(dir1, CURR_RUN_ID),
            {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID}
        )

        dir2 = create_subdirectory(yt_client, yt_root, 'dir2')
        create_table(
            yt_client,
            ypath_join(dir2, CURR_RUN_ID),
            {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID}
        )

        res_checker = ResultChecker(
            yt_client,
            {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID},
        )
        (path1, path2), is_all_processed = res_checker.get_paths(dir1, dir2)
        assert is_all_processed is True
        assert path1 == ypath_join(dir1, CURR_RUN_ID)
        assert path2 == ypath_join(dir2, CURR_RUN_ID)

    def test_part_processed(self, yt_client, yt_root):
        dir1 = create_subdirectory(yt_client, yt_root, 'dir1')
        create_table(
            yt_client,
            ypath_join(dir1, PREV_RUN_ID),
            {LOG_INTERVAL_KEY: PREV_LOG_INTERVAL.to_meta(), RUN_ID_KEY: PREV_RUN_ID}
        )

        dir2 = create_subdirectory(yt_client, yt_root, 'dir2')
        create_table(
            yt_client,
            ypath_join(dir2, CURR_RUN_ID),
            {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID}
        )

        res_checker = ResultChecker(
            yt_client,
            {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID},
        )
        with pytest.raises(AssertionError) as exc_info:
            res_checker.get_paths(dir1, dir2)
        assert 'Partially formed result!' in exc_info.value.args[0]

    @pytest.mark.parametrize(
        'tbl_run_id, tbl_meta, check_meta, strict_sequence, error_msg',
        [
            pytest.param(
                PREV_RUN_ID,
                {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: PREV_RUN_ID},
                {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID},
                True,
                'Previous table is not preceding for',
                id='wrong_name'
            ),
            pytest.param(
                PREV_RUN_ID,
                {LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 1, 10)]).to_meta(), RUN_ID_KEY: PREV_RUN_ID},
                {LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 9, 11)]).to_meta(), RUN_ID_KEY: CURR_RUN_ID},
                True,
                'Previous table is not preceding for',
                id='wrong_interval'
            ),
            pytest.param(
                PREV_RUN_ID,
                {LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 1, 10)]).to_meta(), RUN_ID_KEY: PREV_RUN_ID},
                {LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 9, 11)]).to_meta(), RUN_ID_KEY: CURR_RUN_ID},
                False,
                'Previous table is not preceding for',
                id='wrong_interval_nonstrict'
            ),
            pytest.param(
                NEXT_RUN_ID,
                {LOG_INTERVAL_KEY: NEXT_LOG_INTERVAL.to_meta(), RUN_ID_KEY: NEXT_RUN_ID},
                {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID},
                True,
                'Next table exists for',
                id='next_table'
            ),
            pytest.param(
                CURR_RUN_ID,
                {LOG_INTERVAL_KEY: PREV_LOG_INTERVAL.to_meta(), RUN_ID_KEY: PREV_RUN_ID},
                {LOG_INTERVAL_KEY: NEXT_LOG_INTERVAL.to_meta(), RUN_ID_KEY: NEXT_RUN_ID},
                True,
                'Previous table is not preceding for',
                id='broken_sequence'
            ),
            pytest.param(
                CURR_RUN_ID,
                {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID, 'some': 'crap'},
                {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(), RUN_ID_KEY: CURR_RUN_ID},
                True,
                'Bad meta in current table for',
                id='wrong_meta'
            ),
        ]
    )
    def test_wrong_meta(self, yt_client, yt_root, tbl_run_id, tbl_meta, check_meta, strict_sequence, error_msg):
        dir1 = create_subdirectory(yt_client, yt_root, 'dir1')
        create_table(yt_client, ypath_join(dir1, tbl_run_id), tbl_meta)

        with pytest.raises(AssertionError) as exc_info:
            ResultChecker(yt_client, check_meta, strict_sequence).get_paths(dir1)
        assert error_msg in exc_info.value.args[0]

    def test_non_strict_sequence_prev_table(self, yt_client, yt_root):
        dir1 = create_subdirectory(yt_client, yt_root, 'dir1')
        create_table(
            yt_client,
            ypath_join(dir1, PREV_RUN_ID),
            {LOG_INTERVAL_KEY: PREV_LOG_INTERVAL.to_meta(), RUN_ID_KEY: PREV_RUN_ID}
        )

        res_checker = ResultChecker(
            yt_client,
            {LOG_INTERVAL_KEY: NEXT_LOG_INTERVAL.to_meta(), RUN_ID_KEY: NEXT_RUN_ID},
            strict_sequence=False
        )
        (path1,), is_all_processed = res_checker.get_paths(dir1)
        assert is_all_processed is False
        assert path1 == ypath_join(dir1, NEXT_RUN_ID)


class TestCheckLastTablePath:
    @pytest.fixture
    def dir_name(self, yt_client, yt_root):
        return create_subdirectory(yt_client, yt_root, 'dir')

    def test_empty(self, yt_client, dir_name):
        with pytest.raises(AssertionError) as exc_info:
            check_last_table_path(yt_client, dir_name, CURR_LOG_INTERVAL)

        assert 'directory is empty' in exc_info.value.args[0]

    def test_precedes(self, yt_client, dir_name):
        create_table(yt_client, ypath_join(dir_name, 'a'), {LOG_INTERVAL_KEY: PREV_LOG_INTERVAL.to_meta()})
        create_table(yt_client, ypath_join(dir_name, 'b'), {LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta()})

        res_path = check_last_table_path(yt_client, dir_name, NEXT_LOG_INTERVAL)

        assert res_path == ypath_join(dir_name, 'b')

    def test_mismatch(self, yt_client, dir_name):
        create_table(
            yt_client,
            ypath_join(dir_name, 'b'),
            {LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 0, 10)]).to_meta()}
        )

        with pytest.raises(AssertionError) as exc_info:
            check_last_table_path(yt_client, dir_name, LogInterval([Subinterval('a', 'a', 0, 11, 20)]))

        assert 'interval mismatch for' in exc_info.value.args[0]


class TestGetTablesRange:
    @pytest.fixture
    def dir_name(self, yt_client, yt_root):
        return create_subdirectory(yt_client, yt_root, 'dir')

    def test_empty(self, yt_client, dir_name):
        with pytest.raises(AssertionError) as exc_info:
            get_tables_range(yt_client, dir_name, CURR_LOG_INTERVAL)
        assert 'Not found any intersecting intervals' in exc_info.value.args[0]

    @pytest.mark.parametrize(
        'interval, beginning_type, end_type, ans',
        [
            pytest.param((5, 10), RangeBorderSelectType.eq, RangeBorderSelectType.eq, ('t2', 't2'), id='eq'),
            pytest.param((5, 667), RangeBorderSelectType.eq, RangeBorderSelectType.le, ('t2', 't5'), id='end_le'),
            pytest.param((6, 15), RangeBorderSelectType.ge, RangeBorderSelectType.eq, ('t2', 't3'), id='beginning_ge'),
        ]
    )
    def test_ok(self, yt_client, dir_name, interval, beginning_type, end_type, ans):
        create_table(yt_client, ypath_join(dir_name, 't1'), {LOG_INTERVAL_KEY: mk_interval(0, 5).to_meta()})
        create_table(yt_client, ypath_join(dir_name, 't2'), {LOG_INTERVAL_KEY: mk_interval(5, 10).to_meta()})
        create_table(yt_client, ypath_join(dir_name, 't3'), {LOG_INTERVAL_KEY: mk_interval(10, 15).to_meta()})
        create_table(yt_client, ypath_join(dir_name, 't4'), {LOG_INTERVAL_KEY: mk_interval(15, 20).to_meta()})
        create_table(yt_client, ypath_join(dir_name, 't5'), {LOG_INTERVAL_KEY: mk_interval(20, 666).to_meta()})

        res = get_tables_range(
            yt_client,
            dir_name,
            mk_interval(*interval),
            beginning_type=beginning_type,
            end_type=end_type,
        )
        assert res == ans

    def test_border_select_error(self, yt_client, dir_name):
        create_table(yt_client, ypath_join(dir_name, 't2'), {LOG_INTERVAL_KEY: mk_interval(5, 10).to_meta()})
        create_table(yt_client, ypath_join(dir_name, 't3'), {LOG_INTERVAL_KEY: mk_interval(10, 15).to_meta()})

        with pytest.raises(AssertionError, match="got wrong covering interval"):
            get_tables_range(yt_client, dir_name, mk_interval(4, 15))

    def test_new_zero_partition(self, yt_client, dir_name):
        """ BALANCE-35969 - проверяем появление партиции в 4 срезе. """
        create_table(yt_client, ypath_join(dir_name, 't1'), {LOG_INTERVAL_KEY: LogInterval([
            Subinterval('a', 'a', 1, 0, 10),
        ]).to_meta()})

        assert get_tables_range(yt_client, dir_name, LogInterval([
            Subinterval('a', 'a', 1, 0, 10),
            Subinterval('a', 'a', 2, 0, 0),
        ])) == ('t1', 't1')

    def test_gap(self, yt_client, dir_name):
        create_table(yt_client, ypath_join(dir_name, 't1'), {LOG_INTERVAL_KEY: mk_interval(0, 5).to_meta()})
        create_table(yt_client, ypath_join(dir_name, 't3'), {LOG_INTERVAL_KEY: mk_interval(6, 15).to_meta()})

        with pytest.raises(AssertionError) as exc_info:
            get_tables_range(yt_client, dir_name, mk_interval(0, 15))
        assert 'gap in offsets at' in exc_info.value.args[0]

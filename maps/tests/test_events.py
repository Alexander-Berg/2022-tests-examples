import pytest

from datetime import date
import os

from yt.wrapper import ypath_join, YtOperationFailedError

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
import maps.analyzer.toolkit.lib as tk
import maps.analyzer.pylibs.envkit as envkit

import maps.analyzer.sandbox.prepare_matched_data.lib.operations as ops
import maps.analyzer.sandbox.prepare_matched_data.lib.sources as sources


os.environ["ALZ_API_YQL_TOKEN"] = "test"
envkit.config.SVN_REVISION = "test"


def test_split_assessors_with_event_log(ytc_events):
    result = ops.split_assessors_with_event_log(
        ytc_events,
        '//split_assessors_with_event_log/travel_times.in.1',
        '//split_assessors_with_event_log/events.in.1',
    )
    assert_equal_tables(ytc_events, '//split_assessors_with_event_log/result.out.1', result, null_equals_unexistant=True)


def test_split_assessors_with_event_log_start_timeout(ytc_events):
    result = ops.split_assessors_with_event_log(
        ytc_events,
        '//split_assessors_with_event_log/travel_times.in.2',
        '//split_assessors_with_event_log/events.in.2',
        start_timeout=180
    )
    assert_equal_tables(ytc_events, '//split_assessors_with_event_log/result.out.2', result, null_equals_unexistant=True)


def test_split_assessors_with_event_log_no_split(ytc_events):
    result = ops.split_assessors_with_event_log(
        ytc_events,
        '//split_assessors_with_event_log/travel_times.in.3',
        '//split_assessors_with_event_log/events.in.3',
    )
    assert_equal_tables(ytc_events, '//split_assessors_with_event_log/result.out.3', result, null_equals_unexistant=True)


def test_split_assessors_with_event_log_incorrectness_policy(ytc_events):
    result = ops.split_assessors_with_event_log(
        ytc_events,
        '//split_assessors_with_event_log/travel_times.in.4',
        '//split_assessors_with_event_log/events.in.4',
        incorrectness_policy='IGNORE'
    )
    assert_equal_tables(ytc_events, '//split_assessors_with_event_log/result.out.4', result, null_equals_unexistant=True)

    result = ops.split_assessors_with_event_log(
        ytc_events,
        '//split_assessors_with_event_log/travel_times.in.4',
        '//split_assessors_with_event_log/events.in.4',
        incorrectness_policy='SKIP'
    )
    assert_equal_tables(ytc_events, ytc_events.create_temp_table(), result)

    with pytest.raises(YtOperationFailedError):
        ops.split_assessors_with_event_log(
            ytc_events,
            '//split_assessors_with_event_log/travel_times.in.4',
            '//split_assessors_with_event_log/events.in.4',
            incorrectness_policy='THROW'
        )


def test_split_assessors_with_event_log_dates(ytc_events):
    results = ops.split_assessors_with_event_log(
        ytc_events,
        '//split_assessors_with_event_log/travel_times.in.5',
        '//split_assessors_with_event_log/events.in.5',
        dates=[date(2019, 1, 1), date(2019, 1, 2)],
    )
    assert len(results) == 2
    assert_equal_tables(ytc_events, '//split_assessors_with_event_log/result.1.out.5', results[0], null_equals_unexistant=True)
    assert_equal_tables(ytc_events, '//split_assessors_with_event_log/result.2.out.5', results[1], null_equals_unexistant=True)


def test_sources_get_events(ytc_events):
    def cnt(t):
        return ytc_events.get(ypath_join(t, '@row_count'))
    tk.config.read_json_config('/prepare_matched_data.json')

    target_events = [
        '//router-events/events/2017-10-05',
        '//router-events/events/2017-10-06',
        '//router-events/events/2017-10-07',
    ]
    expected = sum(map(cnt, target_events))
    actual_events = sources.get_events(
        ytc_events,
        [date(2017, 10, 5), date(2017, 10, 6), date(2017, 10, 7)],
        tk.paths.Common.ROUTER_EVENTS_LOGS,
    )

    assert cnt(actual_events) == expected

    with pytest.raises(AssertionError):
        sources.get_events(ytc_events, [date(2017, 10, 8)])

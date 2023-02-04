# -*- coding: utf-8 -*-

import pytest

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
    intervals_processed,
    get_greatest_interval,
    get_delta_interval,
)


LOG_INTERVAL_1 = LogInterval([Subinterval('a', 'a', 0, 0, 10)])
LOG_INTERVAL_2 = LogInterval([Subinterval('a', 'a', 0, 10, 20)])
LOG_INTERVAL_3 = LogInterval([Subinterval('a', 'a', 0, 20, 30)])


class TestIntervalsProcessed:
    @pytest.mark.parametrize(
        'interval, processed_interval, req_res',
        [
            pytest.param(LOG_INTERVAL_1, LOG_INTERVAL_1, True),
            pytest.param(LOG_INTERVAL_2, LOG_INTERVAL_1, False)
        ]

    )
    def test_base(self, interval, processed_interval, req_res):
        result = intervals_processed(interval, processed_interval)
        assert result == req_res

    def test_intervals_mismatch(self):
        with pytest.raises(AssertionError) as exc_info:
            intervals_processed(LOG_INTERVAL_1, LogInterval([Subinterval('a', 'a', 0, 0, 11)]))
        assert 'intervals mismatch' in exc_info.value.args[0]


class TestGetGreatestInterval:
    @pytest.mark.parametrize(
        'interval_1, interval_2, req_res',
        [
            pytest.param(LOG_INTERVAL_1, LOG_INTERVAL_2, LOG_INTERVAL_2),
            pytest.param(LOG_INTERVAL_2, LOG_INTERVAL_1, LOG_INTERVAL_2),
            pytest.param(LOG_INTERVAL_2, LOG_INTERVAL_2, LOG_INTERVAL_2)
        ]

    )
    def test_base(self, interval_1, interval_2, req_res):
        result = get_greatest_interval(interval_1, interval_2)
        assert result == req_res


class TestGetDeltaInterval:
    @pytest.mark.parametrize(
        'processed_interval, last_interval, req_res',
        [
            pytest.param(LOG_INTERVAL_2, LOG_INTERVAL_1, None),
            pytest.param(LOG_INTERVAL_2, LOG_INTERVAL_3, LOG_INTERVAL_3),
            pytest.param(LOG_INTERVAL_2, LOG_INTERVAL_2, None),
            pytest.param(LogInterval([Subinterval('a', 'a', 0, 0, 10)]), LogInterval([Subinterval('a', 'a', 0, 0, 9)]), None)
        ]

    )
    def test_base(self, processed_interval, last_interval, req_res):
        result = get_delta_interval(processed_interval, last_interval)
        assert result == req_res

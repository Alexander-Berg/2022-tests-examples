import functools
import sys
import threading
import time

import pytest

from walle.models import monkeypatch_timestamp
from walle.stats import (
    IntegerLinearHistogram,
    LogarithmicHistogram,
    Counter,
    Age,
    DELTA,
    ABSOLUTE,
    ABSOLUTE_MINIMUM,
    StatsManager,
    Timing,
)


class TestCounter:
    def test_empty(self):
        counter = Counter(DELTA)
        assert counter.value == 0
        assert counter.aggregation_method == DELTA

        counter.add(1)
        assert counter.value == 1

        counter.set(0)
        assert counter.value == 0

    def test_invalid_merge(self):
        counter = Counter(DELTA)
        with pytest.raises(ValueError):
            counter.merge(Counter(ABSOLUTE))

    def test_delta_merge(self):
        counter = Counter(DELTA)
        counter.add(2)
        counter.merge(Counter(DELTA, 2))
        assert counter.value == 4

    def test_absolute_merge(self):
        counter = Counter(ABSOLUTE)
        counter.add(3)
        counter.merge(Counter(ABSOLUTE, 2))
        assert counter.value == 3


class TestAge:
    def test_empty(self):
        age = Age(ABSOLUTE)
        assert age.last_timestamp == 0
        assert age.aggregation_method == ABSOLUTE

        age.set(1)
        assert age.last_timestamp == 1

        age.set(0)
        assert age.last_timestamp == 0

    @pytest.mark.parametrize(
        "real_time,time,expected", [(10010, 10000, 10), (10015, 10000, 15), (10010, 10005, 5), (10015, 10005, 10)]
    )
    def test_age(self, monkeypatch, real_time, time, expected):
        monkeypatch_timestamp(monkeypatch, real_time)
        age = Age()
        age.set(time)
        assert age.age() == expected

    def test_invalid_merge(self):
        age = Age()
        with pytest.raises(ValueError):
            age.merge(Age(ABSOLUTE_MINIMUM))

    @pytest.mark.parametrize("aggregator,expected", [(ABSOLUTE, 12), (ABSOLUTE_MINIMUM, 10)])
    def test_merge(self, aggregator, expected):
        age = Age(aggregator)
        age.set(10)
        age.merge(Age(aggregator, 12))
        assert age.last_timestamp == expected


class TestIntegerLinearHistogram:
    def test_only_zeroes(self):
        assert IntegerLinearHistogram.from_list([0, 0, 0]).to_yasm_format()[1] == [[0, 3]]

    def test_only_nonzeroes(self):
        hist = IntegerLinearHistogram.from_list([1, 1, 3])
        assert hist.to_yasm_format()[1] == [[1, 2], [2, 0], [3, 1]]

        hist = IntegerLinearHistogram.from_list([0.5])
        assert hist.to_yasm_format()[1] == [[0, 1]]

    def test_very_large(self):
        hist = IntegerLinearHistogram.from_list([sys.maxsize])
        assert hist.to_yasm_format()[1] == [[hist.MAX, 1]]

    def test_very_small(self):
        hist = IntegerLinearHistogram.from_list([sys.float_info.min])
        assert hist.to_yasm_format()[1] == [[0, 1]]

    def test_storage(self):
        hist = IntegerLinearHistogram.from_list([0, 1, 1, 3, 5, 5, 12])
        assert hist.to_yasm_format() == IntegerLinearHistogram.load(hist.dump()).to_yasm_format()

    def test_subtract(self):
        left = IntegerLinearHistogram.from_list([1, 1, 2, 3])
        right = IntegerLinearHistogram.from_list([1, 2, 3])
        assert (left.get_difference_from(right).to_yasm_format()[1]) == [[1, 1]]

    def test_merge(self):
        hist = IntegerLinearHistogram.from_list([1, 1, 3])
        hist.merge(IntegerLinearHistogram.from_list([2, 3]))
        assert hist.to_yasm_format()[1] == [[1, 2], [2, 1], [3, 2]]

    def test_percentile(self):
        hist = IntegerLinearHistogram.from_list([1, 1, 2, 2, 10])
        assert hist.get_percentile(1.0) == 10
        assert round(hist.get_percentile(0.0), 8) == 0
        assert hist.get_percentile(0.1) == 1
        assert hist.get_percentile(0.5) == 2
        assert hist.get_percentile(0.75) == 2
        assert hist.get_percentile(0.95) == 10
        assert IntegerLinearHistogram.from_list([sys.maxsize]).get_percentile(1.0) == hist.MAX


class TestLogarithmicHistogram:
    def test_only_zeroes(self):
        assert LogarithmicHistogram.from_list([0, 0, 0]).to_yasm_format()[1] == [[0, 3]]

    def test_only_nonzeroes(self):
        hist = LogarithmicHistogram.from_list([1, 1, 3])
        assert hist.to_yasm_format()[1] == [[1.0, 2], [1.5, 0], [2.25, 1]]

        hist = LogarithmicHistogram.from_list([0.5])
        assert hist.to_yasm_format()[1] == [[0.4444444444444444, 1]]

    def test_very_large(self):
        hist = LogarithmicHistogram.from_list([sys.maxsize])
        assert hist.to_yasm_format()[1] == [[637621500.2140496, 1]]

    def test_very_small(self):
        hist = LogarithmicHistogram.from_list([sys.float_info.min])
        assert hist.to_yasm_format()[1] == [[0.0, 1]]

    def test_storage(self):
        hist = LogarithmicHistogram.from_list([0, 1, 12.34, 0.000003, 23.4])
        assert hist.to_yasm_format() == LogarithmicHistogram.load(hist.dump()).to_yasm_format()

    def test_subtract(self):
        left = LogarithmicHistogram.from_list([1, 1, 2, 3])
        right = LogarithmicHistogram.from_list([1, 2, 3])
        assert (left.get_difference_from(right).to_yasm_format()[1]) == [[1.0, 1]]

    def test_percentile(self):
        hist = LogarithmicHistogram.from_list([1, 1, 2.25, 2.25, 3.375])
        assert hist.get_percentile(1.0) == 4.21875
        assert round(hist.get_percentile(0.0), 8) == 0
        assert hist.get_percentile(0.1) == 1.25
        assert hist.get_percentile(0.5) == 2.8125
        assert hist.get_percentile(0.75) == 2.8125
        assert hist.get_percentile(0.95) == 4.21875
        assert LogarithmicHistogram.from_list([sys.maxsize]).get_percentile(1.0) == 637621500.2140496

    def test_merge(self):
        hist = LogarithmicHistogram.from_list([1, 1, 2.25])
        hist.merge(LogarithmicHistogram.from_list([2.25, 3.375]))
        assert hist.get_percentile(0.1) == 1.25
        assert hist.get_percentile(0.5) == 2.8125
        assert hist.get_percentile(0.75) == 2.8125
        assert hist.get_percentile(0.95) == 4.21875
        assert hist.get_percentile(1.0) == 4.21875


class TestStatsManager:
    def test_dump_load(self, monkeypatch):
        monkeypatch_timestamp(monkeypatch, 10010)
        original_manager = StatsManager()
        original_manager.increment_counter("counter")
        original_manager.set_age_timestamp("age", 10000)
        original_manager.add_sample("sample", 1)
        original_manager.add_sample("sample-lin", 1, IntegerLinearHistogram)
        original_manager.add_sample("sample-log", 1, LogarithmicHistogram)
        state = original_manager.dump()

        restored_manager = StatsManager.load(state)
        assert restored_manager.dump() == state
        assert sorted(restored_manager.to_yasm_format()) == sorted(original_manager.to_yasm_format())

    def test_age_now(self, monkeypatch):
        monkeypatch_timestamp(monkeypatch, 10010)
        first_manager = StatsManager()
        first_manager.set_age_timestamp("age_max", 10000)
        first_manager.set_age_timestamp("age_now")
        assert sorted(first_manager.to_yasm_format()) == [['age_max_age_axxx', 10], ['age_now_age_axxx', 0]]

    def test_merge(self, monkeypatch):
        monkeypatch_timestamp(monkeypatch, 10010)
        first_manager = StatsManager()
        first_manager.increment_counter("counter")
        first_manager.set_age_timestamp("age_max", 10000)
        first_manager.set_age_timestamp("age_min", 10000, ABSOLUTE_MINIMUM)
        first_manager.add_sample("sample", 1)
        first_manager.add_sample("sample-lin", 1, IntegerLinearHistogram)
        first_manager.add_sample("sample-log", 1, LogarithmicHistogram)

        second_manager = StatsManager()
        second_manager.increment_counter("counter")
        second_manager.set_age_timestamp("age_max", 10002)
        second_manager.set_age_timestamp("age_min", 10002, ABSOLUTE_MINIMUM)
        second_manager.add_sample("sample", 2)
        second_manager.add_sample("sample-lin", 2, IntegerLinearHistogram)
        second_manager.add_sample("sample-log", 2, LogarithmicHistogram)

        first_manager.merge(second_manager)

        assert sorted(first_manager.to_yasm_format()) == [
            ['age_max_age_axxx', 8],
            ['age_min_age_annn', 10],
            ['counter_count_summ', 2],
            ['sample-lin_hgram_dhhh', [[1, 1], [2, 1]]],
            ['sample-log_hgram_dhhh', [[1.0, 1], [1.5, 1]]],
            ['sample_hgram_dhhh', [[1.0, 1], [1.5, 1]]],
        ]


class TestConcurrency:
    @staticmethod
    def _run_in_threads(function, threads_count=10):
        start_time = time.time() + 0.1

        def delayed_run():
            time.sleep(start_time - time.time())
            function()

        threads = [threading.Thread(target=delayed_run) for _ in range(threads_count)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

    def test_counter_increment(self):
        meter = Counter(aggregation_method=DELTA)

        def increment():
            for _ in range(100):
                meter.add(1)

        self._run_in_threads(increment, 10)

        assert meter.value == 1000

    @pytest.mark.parametrize("hist_cls", [IntegerLinearHistogram, LogarithmicHistogram])
    def test_histogram_set(self, hist_cls):
        hist = hist_cls()

        def update_value():
            for _ in range(100):
                hist.add(1.0)

        self._run_in_threads(update_value, 10)

        assert hist.to_yasm_format()[1] == [[1.0, 1000]]

    def test_stats_manager_counters(self):
        manager = StatsManager()

        def increment(key):
            manager.increment_counter(str(key), 1)

        for idx in range(5):
            self._run_in_threads(functools.partial(increment, idx), 5)

        for idx in range(5):
            assert manager._counters[str(idx)].value == 5

    def test_stats_manager_histograms(self):
        manager = StatsManager()

        def add_sample(key):
            manager.add_sample(str(key), 1.0)

        for idx in range(5):
            self._run_in_threads(functools.partial(add_sample, idx), 5)

        for idx in range(5):
            assert manager._histograms[str(idx)].to_yasm_format()[1] == [[1.0, 5]]


class TestTiming:
    class MockStopWatch:
        def __init__(self, get=5, split=10, reset=20):
            self._get = get
            self._split = split
            self._reset = reset

        def get(self):
            return self._get

        def split(self):
            return self._split

        def reset(self):
            return self._reset

    def test_split(self):
        stats_manager = StatsManager()
        stats_manager.get_sample(("timing", "lap"), IntegerLinearHistogram)  # set hist class for this key

        timer = Timing("timing", self.MockStopWatch(), stats_manager)
        timer.split("lap")

        assert stats_manager.to_yasm_format() == [['timing.lap_hgram_dhhh', [[10, 1]]]]

    def test_reset(self):
        stats_manager = StatsManager()
        stats_manager.get_sample(("timing", "total"), IntegerLinearHistogram)  # set hist class for this key

        timer = Timing("timing", self.MockStopWatch(), stats_manager)
        timer.reset("total")

        assert stats_manager.to_yasm_format() == [['timing.total_hgram_dhhh', [[20, 1]]]]

    def test_submit(self):
        stats_manager = StatsManager()
        stats_manager.get_sample(("timing", "intermediate"), IntegerLinearHistogram)  # set hist class for this key

        timer = Timing("timing", self.MockStopWatch(), stats_manager)
        timer.submit("intermediate")

        assert stats_manager.to_yasm_format() == [['timing.intermediate_hgram_dhhh', [[5, 1]]]]

    def test_submit_with_longer_key(self):
        stats_manager = StatsManager()
        # set hist class for this key
        stats_manager.get_sample(("timing", "submit", "longer", "value"), IntegerLinearHistogram)
        stats_manager.get_sample(("timing", "submit", "value"), IntegerLinearHistogram)

        timer = Timing(("timing", "submit"), self.MockStopWatch(), stats_manager)
        timer.submit("value")
        timer.submit(("longer", "value"))

        assert sorted(stats_manager.to_yasm_format()) == [
            ['timing.submit.longer.value_hgram_dhhh', [[5, 1]]],
            ['timing.submit.value_hgram_dhhh', [[5, 1]]],
        ]

    def test_measure_total(self):
        stats_manager = StatsManager()
        stats_manager.get_sample(("timing", "total"), IntegerLinearHistogram)  # set hist class for this key

        timer = Timing("timing", self.MockStopWatch(), stats_manager)
        with timer.measure("total"):
            pass

        assert stats_manager.to_yasm_format() == [['timing.total_hgram_dhhh', [[10, 1]]]]

    def test_measure_success(self):
        stats_manager = StatsManager()
        # set hist class for the keys
        stats_manager.get_sample(("timing", "measure"), IntegerLinearHistogram)
        stats_manager.get_sample(("timing", "measure", "success"), IntegerLinearHistogram)

        timer = Timing("timing", self.MockStopWatch(get=7, split=8), stats_manager)
        with timer.measure("measure", success="success", error="error"):
            pass

        assert sorted(stats_manager.to_yasm_format()) == [
            ['timing.measure.success_hgram_dhhh', [[7, 1]]],
            ['timing.measure_hgram_dhhh', [[8, 1]]],
        ]

    def test_measure_error(self):
        stats_manager = StatsManager()
        # set hist class for the keys
        stats_manager.get_sample(("timing", "measure"), IntegerLinearHistogram)
        stats_manager.get_sample(("timing", "measure", "error"), IntegerLinearHistogram)

        timer = Timing("timing", self.MockStopWatch(get=7, split=8), stats_manager)
        try:
            with timer.measure("measure", success="success", error="error"):
                raise RuntimeError
        except RuntimeError:
            pass

        assert sorted(stats_manager.to_yasm_format()) == [
            ['timing.measure.error_hgram_dhhh', [[7, 1]]],
            ['timing.measure_hgram_dhhh', [[8, 1]]],
        ]

    def test_measure_with_longer_key(self):
        stats_manager = StatsManager()
        # set hist class for the keys
        stats_manager.get_sample(("timing", "measure", "total"), IntegerLinearHistogram)

        timer = Timing(("timing", "measure"), self.MockStopWatch(split=18), stats_manager)
        with timer.measure("total"):
            pass

        assert sorted(stats_manager.to_yasm_format()) == [
            ['timing.measure.total_hgram_dhhh', [[18, 1]]],
        ]

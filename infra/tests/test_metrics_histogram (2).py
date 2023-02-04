import six

from sepelib.metrics.meters import Histogram
from sepelib.metrics.stats import Snapshot

from .utils_test import TimedTestCase


class HistogramTestCase(TimedTestCase):
    def test__a_sample_of_100_from_1000(self):
        hist = Histogram(100, 0.99)
        for i in range(1000):
            hist.add(i)

        self.assertEqual(1000, hist.get_count())
        self.assertEqual(100, hist.sample.get_size())
        snapshot = hist.get_snapshot()
        self.assertEqual(100, snapshot.get_size())

        for i in snapshot.values:
            self.assertTrue(0 <= i and i <= 1000)

        self.assertEqual(999, hist.get_max())
        self.assertEqual(0, hist.get_min())
        self.assertEqual(499.5, hist.get_mean())
        self.assertAlmostEqual(83416.6666, hist.get_var(), delta=0.0001)

    def test__a_sample_of_100_from_10(self):
        hist = Histogram(100, 0.99)
        for i in range(10):
            hist.add(i)

        self.assertEqual(10, hist.get_count())
        self.assertEqual(10, hist.sample.get_size())
        snapshot = hist.get_snapshot()
        self.assertEqual(10, snapshot.get_size())

        for i in snapshot.values:
            self.assertTrue(0 <= i <= 10)

        self.assertEqual(9, hist.get_max())
        self.assertEqual(0, hist.get_min())
        self.assertEqual(4.5, hist.get_mean())
        self.assertAlmostEqual(9.1666, hist.get_var(), delta=0.0001)

    def test__a_long_wait_should_not_corrupt_sample(self):
        hist = Histogram(10, 0.015, self.clock)

        for i in range(1000):
            hist.add(1000 + i)
            self.clock.add(0.1)

        self.assertEqual(hist.get_snapshot().get_size(), 10)
        for i in hist.sample.get_snapshot().values:
            self.assertTrue(1000 <= i <= 2000)

        self.clock.add(15 * 3600)  # 15 hours, should trigger rescale
        hist.add(2000)
        self.assertEqual(hist.get_snapshot().get_size(), 2)
        for i in hist.sample.get_snapshot().values:
            self.assertTrue(1000 <= i <= 3000)

        for i in range(1000):
            hist.add(3000 + i)
            self.clock.add(0.1)
        self.assertEqual(hist.get_snapshot().get_size(), 10)
        for i in hist.sample.get_snapshot().values:
            self.assertTrue(3000 <= i <= 4000)


def test_snapshot_histogram():
    def round_hist(hist):
        return [[round(k, 2), v] for k, v in hist]

    # Simple case
    s = Snapshot([i for i in six.moves.xrange(20)])
    histogram = round_hist(s.get_histogram(5))
    assert histogram == [[0.0, 6], [5.06, 2], [7.59, 4], [11.39, 6], [17.09, 2]]
    # Empty case
    s = Snapshot([])
    histogram = s.get_histogram(20)
    assert histogram == [[0, 0]]
    # Another case
    s = Snapshot([100] * 100)
    histogram = round_hist(s.get_histogram(20))
    assert histogram == [[100.0, 100]]
    # all zeroes case
    s = Snapshot([0, 0, 0, 0, 0])
    histogram = s.get_histogram()
    assert histogram == [[0, 5]]
    # outlier case
    s = Snapshot([0.1, 0.1, 0.1, 0.1, 1000])
    histogram = round_hist(s.get_histogram(20))
    assert len(histogram) == 20
    for border, weight in histogram:
        if border == 0.1:
            assert weight == 4
        elif border == 985.26:
            assert weight == 1
        else:
            assert weight == 0, "border {0}, weight {1}, should be 0".format(border, weight)
    # trimming case
    s = Snapshot([i for i in six.moves.xrange(20)])
    histogram = s.get_histogram(2)
    assert round_hist(histogram) == [[0.0, 18], [17.09, 2]]

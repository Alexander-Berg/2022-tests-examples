import unittest
from hbfagent import metrics


class TestUgram(unittest.TestCase):
    def test_on_create(self):
        u = metrics.Ugram(-100)
        assert u.value == [[0, 1], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 0], [60000, 0]]
        u = metrics.Ugram(0)
        assert u.value == [[0, 1], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 0], [60000, 0]]
        u = metrics.Ugram(100)
        assert u.value == [[0, 0], [1, 0], [6, 0], [60, 1], [600, 0], [6000, 0], [60000, 0]]
        u = metrics.Ugram(1)
        assert u.value == [[0, 0], [1, 1], [6, 0], [60, 0], [600, 0], [6000, 0], [60000, 0]]
        u = metrics.Ugram(6000)
        assert u.value == [[0, 0], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 1], [60000, 0]]
        u = metrics.Ugram(99999999999)
        assert u.value == [[0, 0], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 0], [60000, 1]]

    def test_update(self):
        u = metrics.Ugram()
        assert u.value == [[0, 1], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 0], [60000, 0]]
        u.update(10)
        assert u.value == [[0, 0], [1, 0], [6, 1], [60, 0], [600, 0], [6000, 0], [60000, 0]]
        u.update(59000)
        assert u.value == [[0, 0], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 1], [60000, 0]]

    def test_complicated_update(self):
        u = metrics.Ugram()
        u.update(10)
        assert u.value == [[0, 0], [1, 0], [6, 1], [60, 0], [600, 0], [6000, 0], [60000, 0]]
        u.update(550)
        assert u.value == [[0, 0], [1, 0], [6, 0], [60, 1], [600, 0], [6000, 0], [60000, 0]]
        u.update(550)
        assert u.value == [[0, 1], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 0], [60000, 0]]
        u.update(1550)
        assert u.value == [[0, 0], [1, 0], [6, 0], [60, 0], [600, 1], [6000, 0], [60000, 0]]
        u.update(1600)
        assert u.value == [[0, 0], [1, 0], [6, 1], [60, 0], [600, 0], [6000, 0], [60000, 0]]
        u.update(7600)
        assert u.value == [[0, 0], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 1], [60000, 0]]
        u.update(7600)
        assert u.value == [[0, 1], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 0], [60000, 0]]
        u.update(999999999)
        assert u.value == [[0, 0], [1, 0], [6, 0], [60, 0], [600, 0], [6000, 0], [60000, 1]]

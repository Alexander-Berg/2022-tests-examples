import unittest
import numpy as np
from numpy.testing import assert_almost_equal
from datacloud.dev_utils.geo.addrs_resolve import BadAddrTypes
from datacloud.dev_utils.geo.geo_features import get_features, sigmoid


class TestGeoFeatures(unittest.TestCase):
    def test_usual(self):
        assert_almost_equal(
            get_features([(37.5, 55.7)], ['ucfsv7fsj2']),
            np.array([1., 0., 0., 0., 0., 1.])
        )
        assert_almost_equal(
            get_features([(37.507, 55.7)], ['ucfsv7fsj2']),
            np.array([1., 0., 0., 0., 0., 1.])
        )
        assert_almost_equal(
            get_features([(37.509, 55.7)], ['ucfsv7fsj2']),
            np.array([0., 0., 0., 0., 0., 1.])
        )
        assert_almost_equal(
            get_features([(37.587093, 55.733969)], []),
            np.array([0, 0, 0, 0, 0, 1])
        )

    def test_bad_addr(self):
        assert_almost_equal(
            get_features([BadAddrTypes.empty_addr], ['ucfsv7fsj2']),
            np.array([0, 0, 0, 0, 0, 0])
        )
        assert_almost_equal(
            get_features(
                [BadAddrTypes.empty_addr, BadAddrTypes.empty_addr],
                ['7zzzzzzzzz', 'zzzzzzzzzz']
            ),
            np.array([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0])
        )
        assert_almost_equal(
            get_features(
                [(90., 90.), (0., 0.), BadAddrTypes.empty_addr],
                ['vzzzzzzzzz', '7zzzzzzzzz', '7zzzzzzzzz', '7zzzzzzzzz']
            ),
            np.array([
                1., 0., 0., 0., 0.,
                1., 1., 1., 0., 0.,
                0., 0., 0., 0., 0.,
                1., 1., 0.
            ])
        )

    def test_sigmoid(self):
        self.assertAlmostEqual(sigmoid(0), 0.5)
        self.assertAlmostEqual(sigmoid(1), 0.7310585786300049)
        self.assertAlmostEqual(sigmoid(1e5), 1)
        self.assertAlmostEqual(sigmoid(-100), 0.)

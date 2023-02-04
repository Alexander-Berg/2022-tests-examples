# -*- coding: utf-8 -*-
import unittest
import numpy as np
from datacloud.stability import stability
from datacloud.dev_utils.data.data_utils import array_tostring


class test_BuildBinsMapper(unittest.TestCase):

    def test_stability_random(self):
        np.random.seed(42)
        left_border = np.random.randint(-100, 0)
        right_border = np.random.randint(0, 100)
        n_features = np.random.randint(10)
        n_bins = np.random.randint(10, 1000)
        range_ = right_border - left_border
        test_array = np.random.random((1000, n_features)) * \
            range_ + left_border
        test_recs = []
        for i in range(1000):
            test_rec = dict()
            test_rec['features'] = array_tostring(test_array[i, :])
            test_recs.append(test_rec)

        test_mapper = stability.BuildBinsMapper(
            n_bins=n_bins,
            left_border=left_border,
            right_border=right_border)

        test_rec[test_mapper._column]
        expected_hist = np.zeros((n_bins, n_features))
        for i in range(n_features):
            bins = np.linspace(left_border, right_border, n_bins + 1)
            expected_hist[:, i], bins_ = np.histogram(test_array[:, i],
                                                      bins=bins)

        result_hist = np.zeros((n_bins, n_features))
        for feature in list(test_mapper(test_recs)):
            result_hist[feature['bin'], feature['feature']] = feature['count']
        test_sum = np.sum(np.abs(expected_hist - result_hist))
        self.assertTrue(test_sum < 10)

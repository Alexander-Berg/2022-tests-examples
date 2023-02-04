# -*- coding: utf-8 -*-
import unittest
from datacloud.dev_utils.yt import features


class TestYtFeatures(unittest.TestCase):

    def test_cloud_nodes_spec_true(self):
        received = features.cloud_nodes_spec(True)
        expected = {
            'pool_trees': ['physical'],
            'tentative_pool_trees': ['cloud'],
        }
        self.assertEqual(received, expected)

    def test_cloud_nodes_spec_false(self):
        received = features.cloud_nodes_spec(False)
        expected = {}
        self.assertEqual(received, expected)


if __name__ == '__main__':
    unittest.main()

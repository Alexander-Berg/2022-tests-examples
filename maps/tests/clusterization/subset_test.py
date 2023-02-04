import unittest

import maps.carparks.tools.carparks_miner.lib.clusterization.subset as tested_module


class SubsetTests(unittest.TestCase):
    def test_subset_filter(self):
        data = [i for i in range(9)]
        subset = tested_module.Subset(data, lambda x: x % 3 == 1)

        to_filter = [2 * i for i in range(9)]
        filtered = subset.filter(to_filter)

        self.assertListEqual(filtered, [2, 8, 14])

        self.assertRaises(Exception, subset.filter, [])

    def test_subset_replace(self):
        data = [i for i in range(9)]
        subset = tested_module.Subset(data, lambda x: x % 3 == 1)

        original = [2 * i for i in range(9)]
        original_copy = original[:]
        replace = [-1, -2, -3]
        replaced = subset.replace_for_subset(replace, original)

        self.assertListEqual(original, original_copy)
        self.assertListEqual(replaced, [0, -1, 4, 6, -2, 10, 12, -3, 16])

        self.assertRaises(Exception, subset.replace_for_subset, [], original)
        self.assertRaises(Exception, subset.replace_for_subset, [0] * 10, original)
        self.assertRaises(Exception, subset.replace_for_subset, replace, [])

from unittest import TestCase

from plan.common.utils.collection import mapping


class GetMultipleTest(TestCase):
    def setUp(self):
        self.sample = {'a': 1, 'b': 2, 'c': 3}

    def test_subset_of_keys(self):
        self.assertEqual(
            mapping.get_multiple(
                mapping=self.sample,
                keys=['a', 'c']
            ),
            [1, 3]
        )

    def test_all_keys(self):
        self.assertEqual(
            mapping.get_multiple(
                mapping=self.sample,
                keys=['a', 'b', 'c']
            ),
            [1, 2, 3]
        )

    def test_all_keys_order(self):
        self.assertEqual(
            mapping.get_multiple(
                mapping=self.sample,
                keys=['c', 'b', 'a']
            ),
            [3, 2, 1]
        )

    def test_keys_superset(self):
        self.assertEqual(
            mapping.get_multiple(
                mapping=self.sample,
                keys=['a', 'b', 'c', 'x']
            ),
            [1, 2, 3]
        )

    def test_keys_superset_default(self):
        self.assertEqual(
            mapping.get_multiple(
                mapping=self.sample,
                keys=['a', 'b', 'c', 'x'],
                default='XXX'
            ),
            [1, 2, 3, 'XXX']
        )

    def test_keys_superset_random_order(self):
        self.assertEqual(
            mapping.get_multiple(
                mapping=self.sample,
                keys=['a', 'c', 'x', 'b'],
                default='XXX'
            ),
            [1, 3, 'XXX', 2]
        )

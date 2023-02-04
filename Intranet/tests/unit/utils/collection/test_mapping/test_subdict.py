from unittest import TestCase

from plan.common.utils.collection import mapping


class SubdictTestCase(TestCase):
    def setUp(self):
        self.sample = {
            'user': 'batman',
            'gender': 'M',
            'hobby': 'justice',
        }

    def test_all_keys(self):
        self.assertEqual(
            mapping.filter_dict(
                mapping=self.sample,
                keys=['user', 'gender', 'hobby']
            ),
            self.sample,
        )

    def test_subset_of_keys(self):
        self.assertEqual(
            mapping.filter_dict(
                mapping=self.sample,
                keys=['user', 'gender']
            ),
            {
                'user': 'batman',
                'gender': 'M',
            }
        )

    def test_superset_of_keys_without_default(self):
        self.assertEqual(
            mapping.filter_dict(
                mapping=self.sample,
                keys=['user', 'gender', 'nonexistent']
            ),
            {
                'user': 'batman',
                'gender': 'M',
            }
        )

    def test_superset_with_random_order(self):
        self.assertEqual(
            mapping.filter_dict(
                mapping=self.sample,
                keys=['gender', 'nonexistent', 'user']
            ),
            {
                'user': 'batman',
                'gender': 'M',
            }
        )

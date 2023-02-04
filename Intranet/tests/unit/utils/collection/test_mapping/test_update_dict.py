from unittest import TestCase

from plan.common.utils.collection import mapping


class UpdatedDictTest(TestCase):
    def setUp(self):
        self.sample = {
            'user': 'joker',
            'gender': 'M',
        }

    def test_blank(self):
        # Look mom, no kwargs!
        self.assertEqual(
            mapping.updated_dict(
                mapping={}
            ),
            {}
        )

    def test_no_kwargs(self):
        # Look mom, no kwargs!
        self.assertEqual(
            mapping.updated_dict(
                mapping=self.sample
            ),
            self.sample
        )

    def test_some_kwargs(self):
        self.assertEqual(
            mapping.updated_dict(
                mapping=self.sample,
                hobby='have fun',
                talent='magic tricks',
            ),
            {
                'user': 'joker',
                'gender': 'M',
                'hobby': 'have fun',
                'talent': 'magic tricks',
            }
        )

from unittest import TestCase

from plan.common.utils.collection import mapping


class ConvertToIterable(TestCase):
    def test_convert_mapping(self):
        converted = mapping.convert_mapping_to_iterable({
            'a': 1,
            'b': 2
        }, key_name='letter', value_name='number')
        self.assertIn({'letter': 'a', 'number': 1}, converted)
        self.assertIn({'letter': 'b', 'number': 2}, converted)

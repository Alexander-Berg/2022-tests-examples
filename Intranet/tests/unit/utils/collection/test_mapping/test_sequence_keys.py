from unittest import TestCase

from plan.common.utils.collection import mapping


class SequenceKeys(TestCase):
    def test_has_key_really_has(self):
        inputs = [
            ({'x': 10}, ['x']),
            ({'x': {'y': 10}}, ['x', 'y']),
            ({'x': {'y': {'z': 10}}}, ['x', 'y']),
            ({'x': {'y': {'z': 10}}}, ['x', 'y', 'z']),
        ]

        for input in inputs:
            self.assertTrue(mapping.has_key_by_sequence(*input))

    def test_has_key_really_has_not(self):
        inputs = [
            ({'x': 10}, ['a']),
            ({'x': {'y': 10}}, ['x', 'a']),
            ({'x': {'y': {'z': 10}}}, ['x', 'y', 'w']),
            ({'x': {'y': {'z': 10}}}, ['x', 'y', 'z', 'w']),
        ]

        for input in inputs:
            self.assertFalse(mapping.has_key_by_sequence(*input))

    def test_get_by_sequence_has_key(self):
        inputs_outputs = [
            (({'x': 10}, ['x']), 10),
            (({'x': 10}, ['a']), None),
            (({'x': 10}, ['a'], 'DEFAULT'), 'DEFAULT'),
            (({'x': {'y': 10}}, ['x', 'y']), 10),
        ]

        for input, output in inputs_outputs:
            self.assertEqual(mapping.get_by_sequence(*input), output)

    def test_set_by_sequence_has_inner_key(self):
        input = {'a': {'b': 10}}
        output = {'a': {'b': 300}}

        mapping.set_by_sequence(input, ['a', 'b'], 300)
        self.assertEqual(input, output)

    def test_set_by_sequence_one_key(self):
        input = {'a': 'old_val'}
        output = {'a': 'new_val'}

        mapping.set_by_sequence(input, ['a'], 'new_val')
        self.assertEqual(input, output)

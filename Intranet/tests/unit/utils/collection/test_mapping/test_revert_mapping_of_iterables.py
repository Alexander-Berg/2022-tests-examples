from django.test import TestCase

from plan.common.utils.collection import mapping


class RevertMappingOfItersTest(TestCase):

    func = staticmethod(mapping.revert_mapping_of_iterables)

    def test_simple_case(self):
        input = {
            1: ['x', 'y'],
            2: ['z', 'u'],
        }
        expected_output = {
            'x': 1,
            'y': 1,
            'z': 2,
            'u': 2,
        }
        assert_input_expected(
            self.func,
            input,
            expected_output,
            self,
        )


class RevertMappingOfItersListValuesTest(TestCase):

    func = staticmethod(mapping.revert_mapping_of_iterables_list_values)

    def test_simple_case(self):
        input = {
            1: ['x', 'y'],
            2: ['z', 'y'],
        }
        expected_output = {
            'x': [1],
            'y': [1, 2],
            'z': [2],
        }
        assert_input_expected(
            self.func,
            input,
            expected_output,
            self,
        )


# потом на параметризованный тест pytest заменим
def assert_input_expected(func, input, output, test_cls_obj):
    test_cls_obj.assertEqual(func(input), output)

from django.test import TestCase

from plan.common.utils.collection.mapping import DotAccessedDict


class DataAccessDictTest(TestCase):

    def test_simple_init(self):
        example_dict = {'a': 100}

        dot_accessed = DotAccessedDict(example_dict)

        self.assertTrue(hasattr(dot_accessed, 'a'))
        self.assertEqual(dot_accessed.a, 100)

    def test_simple_set_value(self):
        example_dict = {'a': 100}

        dot_accessed = DotAccessedDict(example_dict)

        dot_accessed.a = 200

        self.assertEqual(dot_accessed.a, 200)

    def test_init_with_nested(self):
        example_dict = {'a': {'b': 100}}

        dot_accessed = DotAccessedDict(example_dict)

        self.assertTrue(hasattr(dot_accessed.a, 'b'))
        self.assertEqual(dot_accessed.a.b, 100)

    def test_setattr_nested_dict(self):
        example_dict = {'a': None}

        dot_accessed = DotAccessedDict(example_dict)

        dot_accessed.a = {'b': 100}

        self.assertTrue(hasattr(dot_accessed.a, 'b'))
        self.assertEqual(dot_accessed.a.b, 100)

    def test_setitem_nested_dict(self):
        example_dict = {'a': None}

        dot_accessed = DotAccessedDict(example_dict)

        dot_accessed['a'] = {'b': 100}

        self.assertTrue(hasattr(dot_accessed.a, 'b'))
        self.assertEqual(dot_accessed.a.b, 100)

# coding: utf-8

import unittest

from at.common import Context


class TestContext(unittest.TestCase):

    def setUp(self):
        self.context = Context.Context()
        self.bindings = self.context._Context__bindings
        self.default_values = self.context._Context__default_values

    def testDefaultValues(self):
        """
        Should return default value for keys from
        __default_values and allow to override it
        """
        for key, value in list(self.default_values.items()):
            self.assertEqual(value, getattr(self.context, key), "No default value!")
        key = list(self.default_values.keys())[0]
        new_value = "some-value"
        self.context.set(key, new_value)
        self.assertEqual(new_value, getattr(self.context, key))

    def testStoreBindings(self):
        """Each new attribute should store in __bindings"""
        key = 'somekey'
        key2 = 'somekey2'
        self.context.set(key, 'value')
        self.context.set(key2, 'value2')
        self.assertEqual(
            self.bindings, set([key, key2]),
            "Doesn't store bindings on update!"
        )
        self.context.unset(key)
        self.assertEqual(self.bindings, set([key2]))

    def testGetBindings(self):
        """Return dictionary with all non-default bindings"""
        data = [('key', 'value'), ('key2', 'value2'), ('key3', 'value3')]
        for key, value in data:
            self.context.set(key, value)
        self.assertEqual(self.context.get_bindings(), dict(data))

    def testBuildDefaultValueFromFactory(self):
        """.get should build value if no binding exists and have factory"""
        factory = lambda: 'value'
        key = 'key'
        self.assertRaises(AttributeError, self.context.get, key)
        res = self.context.get(key, factory)
        self.assertEqual(res, factory())
        self.assertEqual(res, self.context.get(key))

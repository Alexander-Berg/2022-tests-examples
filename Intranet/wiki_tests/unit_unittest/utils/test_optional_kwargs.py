
from functools import wraps

from wiki.utils.optional_kwargs import optional_kwargs
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class OptionalKwargsTest(BaseApiTestCase):
    def setUp(self):
        """
        Use @optional_kwargs to create a decorator that increments the result
        of a target function. The default value to increment by is 1.
        """
        global inc

        @optional_kwargs
        def inc(f, by=1):
            @wraps(f)  # It's not required, but we're testing it works anyway
            def _f(*args):
                result = f(*args)
                return result + by

            return _f

    # Tests

    def test_without_kwargs(self):
        @inc
        def f(x, y):
            return x + y

        self.assertEqual(f(2, 2), 5)

    def test_with_kwargs(self):
        @inc(by=10)
        def f(x, y):
            return x + y

        self.assertEqual(f(2, 2), 14)

    def test_properly_wraps_without_kwargs(self):
        @inc
        def f(x, y):
            'Some help'
            return x + y

        self.assertEqual(f.__name__, 'f')
        self.assertEqual(f.__doc__, 'Some help')

    def test_properly_wraps_with_kwargs(self):
        @inc(by=10)
        def f(x, y):
            'Some help'
            return x + y

        self.assertEqual(f.__name__, 'f')
        self.assertEqual(f.__doc__, 'Some help')

    def test_rejects_unknown_kwarg(self):

        with self.assertRaises(TypeError) as cm:

            @inc(byebyebye=10)
            def f(x, y):
                return x + y

        self.assertIn('unexpected keyword argument', repr(cm.exception))

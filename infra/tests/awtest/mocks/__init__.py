import collections
import inspect

import abc
import mock


class AwtestMockMixin(object):
    """
    How to use:
    1. Mock class (e.g. L3MgrClientMock) should inherit from base class (e.g. L3MgrClient) and from AwtestMockMixin.
    2. Mock class must have AwtestMockMeta as a metaclass.

    Functionality:
    1. Raises an error if test calls a method that exists in a base class, but is not implemented in the mock class.
    2. Wraps all methods of base classes that are implemented in the mock class into mock.Mock(wraps=<base_method>).
    3. Provides method ```awtest_reset_mocks``` that calls reset_mock() on all mocks from [2].
    """

    def __getattribute__(self, item):
        if item in object.__getattribute__(self, '__unimplemented_methods'):
            raise NotImplementedError(u'Method "{}" of the base class is not implemented in mock "{}.{}"'.format(
                item, self.__class__.__module__, self.__class__.__name__))
        rv = object.__getattribute__(self, item)
        if item not in object.__getattribute__(self, '__base_methods'):
            return rv
        if inspect.ismethod(rv) and not isinstance(rv, mock.Mock):
            m = mock.Mock(wraps=rv)
            setattr(self, item, m)
            return m
        return rv

    def awtest_reset_mocks(self):
        for method_name in object.__getattribute__(self, '__base_methods'):
            method = object.__getattribute__(self, method_name)
            if isinstance(method, mock.Mock):
                method.reset_mock()


class AwtestMockMeta(abc.ABCMeta):
    def __new__(mcs, name, bases, attrs):
        attrs[u'__base_methods'] = set()
        attrs[u'__unimplemented_methods'] = set()
        for base in bases:
            if type(base) == object or issubclass(base, AwtestMockMixin):
                continue  # skip irrelevant bases
            # lists only classmethods bound to the class itself:
            base_method_names = {m[0] for m in inspect.getmembers(base, predicate=inspect.ismethod)}
            # lists all methods:
            base_method_names.update([m[0] for m in inspect.getmembers(base, predicate=callable)])
            attrs[u'__base_methods'] |= base_method_names
            attrs[u'__unimplemented_methods'] |= base_method_names - set(attrs)
        return abc.ABCMeta.__new__(mcs, name, bases, attrs)

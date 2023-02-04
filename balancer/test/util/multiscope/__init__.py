# -*- coding: utf-8 -*-
import sys
import pytest
import inspect
import types


class FixtureType(object):
    PYTEST = 'pytest'
    MULTISCOPE = 'mutiscope'


_OLD_PYTEST_FIXTURE = pytest.fixture

_FIXTURE_WRAPPER = '''
def {func_name}({args}):
    return func({args})
'''

_LEAST_FIXTURE_WRAPPER = '''
def {func_name}({fixture_name}):
    return {fixture_name}
'''

_SCOPES = ['function', 'class', 'module', 'session']


class FixtureException(Exception):
    pass


def least_scope(scope_list):
    for scope in _SCOPES:
        if scope in scope_list:
            return scope
    raise Exception('Error in util library')


def _fixture_name(scope, name):
    return '{scope}_{name}'.format(scope=scope, name=name)


class MultiScopeFixture(object):
    def __init__(self, params, autouse, ids, pytest_fixtures, parent, name, func_dict):
        super(MultiScopeFixture, self).__init__()
        self.__params = params
        self.__autouse = autouse
        self.__ids = ids
        self.__pytest_fixtures = pytest_fixtures
        self.__parent = parent
        self.__name = name
        self.__func_dict = func_dict

    def build(self):
        for scope in _SCOPES:
            self.__build_scope(scope)
        return self.__build_least_scope()

    def __old_fixture(self, scope, fixture_func):
        return _OLD_PYTEST_FIXTURE(
            scope=scope,
            params=self.__params,
            autouse=self.__autouse,
            ids=self.__ids,
        )(fixture_func)

    def __build_least_scope(self):
        scope = least_scope(_SCOPES)
        func_name = self.__name
        fixture_name = _fixture_name(scope, func_name)
        namespace = dict()
        exec(_LEAST_FIXTURE_WRAPPER.format(func_name=func_name, fixture_name=fixture_name), namespace)
        fixture_func = namespace[func_name]
        if isinstance(self.__parent, types.ModuleType):
            fixture_func.__module__ = self.__parent.__name__
        else:
            pass
        return _OLD_PYTEST_FIXTURE(
            scope=scope,
            autouse=self.__autouse,
        )(fixture_func)

    def __build_scope(self, scope):
        func = self.__func_dict[scope]
        argnames = self.__argnames(scope, func)
        func_name = _fixture_name(scope, self.__name)
        namespace = dict(func=func)
        exec(_FIXTURE_WRAPPER.format(func_name=func_name, args=', '.join(argnames)), namespace)
        fixture_func = namespace[func_name]
        if isinstance(self.__parent, types.ModuleType):
            fixture_func.__module__ = self.__parent.__name__
        else:
            pass
        fixture = self.__old_fixture(scope, fixture_func)
        setattr(self.__parent, func_name, fixture)

    def __argnames(self, scope, func):
        def proc_name(name):
            if name in self.__pytest_fixtures:
                return name
            else:
                return _fixture_name(scope, name)

        argnames = inspect.getargspec(func).args
        return [proc_name(name) for name in argnames]


class MultiScopeFixtureDecorator(object):
    def __init__(self, params, autouse, ids, pytest_fixtures, parent):
        super(MultiScopeFixtureDecorator, self).__init__()
        self.__params = params
        self.__autouse = autouse
        self.__ids = ids
        if pytest_fixtures is not None:
            self.__pytest_fixtures = pytest_fixtures
        else:
            self.__pytest_fixtures = list()
        self.__parent = parent

    def __call__(self, func):
        if self.__parent is not None:
            parent = self.__parent
        elif hasattr(func, '__module__'):
            parent = sys.modules[func.__module__]
        else:
            parent = func.__class__

        if hasattr(func, '__fixturename__'):
            name = func.__fixturename__
            func_dict = {scope: getattr(func, _fixture_name(scope, name)) for scope in _SCOPES}
        else:
            name = func.__name__
            func_dict = {scope: func for scope in _SCOPES}

        least_fixture = MultiScopeFixture(
            params=self.__params,
            autouse=self.__autouse,
            ids=self.__ids,
            pytest_fixtures=self.__pytest_fixtures,
            parent=parent,
            name=name,
            func_dict=func_dict,
        ).build()

        if hasattr(func, '__fixturename__'):
            setattr(parent, name, least_fixture)
            return func
        else:
            return least_fixture


def fixture(params=None, autouse=False, ids=None, pytest_fixtures=None, parent=None):
    if (callable(params) or hasattr(params, '__fixturename__')) and not autouse:
        # direct decoration
        func = params
        params = None
        return MultiScopeFixtureDecorator(
            params, autouse, ids, pytest_fixtures, parent)(func)

    if params is not None and not isinstance(params, (list, tuple)):
        params = list(params)

    return MultiScopeFixtureDecorator(params, autouse, ids, pytest_fixtures, parent)

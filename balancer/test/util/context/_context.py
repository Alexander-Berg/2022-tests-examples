# -*- coding: utf-8 -*-
from balancer.test.util import multiscope


class ManagerFixture(object):
    def __init__(self, manager_name, fixture_name, fixture_type=None):
        super(ManagerFixture, self).__init__()
        self.__manager_name = manager_name
        self.__fixture_name = fixture_name
        if fixture_type is not None:
            self.__fixture_type = fixture_type
        else:
            self.__fixture_type = multiscope.FixtureType.MULTISCOPE

    @property
    def manager_name(self):
        return self.__manager_name

    @property
    def fixture_name(self):
        return self.__fixture_name

    @property
    def fixture_type(self):
        return self.__fixture_type

    def as_kwarg(self):
        return '{}={}'.format(self.manager_name, self.fixture_name)

    def __repr__(self):
        return 'ManagerFixture({}, {}, {})'.format(self.manager_name, self.fixture_name, self.fixture_type)


class StateException(Exception):
    pass


class State(object):
    def __init__(self, parent_state=None):
        super(State, self).__init__()
        self.__values = dict()
        self.__parent_state = parent_state

    @staticmethod
    def __is_allowed(name):
        return not name.startswith('_')

    def register(self, name):
        if not self.__is_allowed(name):
            raise StateException('name "{}" is not allowed'.format(name))
        # FIXME: get rid of register method

    def __getattr__(self, name):
        result = self.__values.get(name)
        if result is None and self.__parent_state is not None:
            result = getattr(self.__parent_state, name)
        return result

    def __setattr__(self, name, value):
        if not self.__is_allowed(name):
            super(State, self).__setattr__(name, value)
        else:
            self.__values[name] = value

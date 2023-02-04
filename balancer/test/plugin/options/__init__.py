# -*- coding: utf-8 -*-
import importlib
from balancer.test.util import options as mod_opts
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture


__FIELD_NAME = 'OPTIONS'


def _get_options(obj, parent):
    if obj is not None and hasattr(obj, __FIELD_NAME):
        return mod_opts.Options(getattr(obj, __FIELD_NAME), parent)
    else:
        return parent


@multiscope.fixture(pytest_fixtures=['request', 'session_options', 'module_options', 'class_options', 'function_options'])
class Options(object):
    __fixturename__ = 'options'

    @staticmethod
    def session_options():
        try:
            obj = importlib.import_module('options')
            return mod_opts.Options(obj)
        except ImportError:
            return None

    @staticmethod
    def module_options(request, session_options):
        return _get_options(request.module, session_options)

    @staticmethod
    def class_options(request, module_options):
        return _get_options(request.cls, module_options)

    @staticmethod
    def function_options():
        try:
            obj = importlib.import_module('options')
            return mod_opts.Options(obj)
        except ImportError:
            return None


class OptionsContext(object):
    @property
    def options(self):
        """
        Options fixture value
        """
        return self.manager.options


MANAGERS = [ManagerFixture('options', 'options')]
CONTEXTS = [OptionsContext]

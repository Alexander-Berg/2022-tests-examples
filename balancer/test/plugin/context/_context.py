# -*- coding: utf-8 -*-
import sys
import importlib
import itertools
import pytest
from collections import namedtuple
from balancer.test.util import multiscope
import balancer.test.util.context as mod_ctx

from balancer.test.util import asserts  # noqa: F401
from balancer.test.util.predef import http  # noqa: F401
from balancer.test.util import dnsfake  # noqa: F401
from balancer.test.util.predef.http.response import custom  # noqa: F401
from balancer.test.util.predef.handler.server.http import SimpleConfig, DummyConfig  # noqa: F401


def __known_plugin(name):
    try:
        return importlib.import_module(name)
    except ImportError:
        return None


def __filter_plugins(all_plugins):
    return [p for p in map(__known_plugin, all_plugins) if p]


__KNOWN_PLUGINS = __filter_plugins([
    'balancer.test.plugin.awacs',
    'balancer.test.plugin.awacs_config',
    'balancer.test.plugin.backend',
    'balancer.test.plugin.balancer',
    'balancer.test.plugin.cachedaemon',
    'balancer.test.plugin.certs',
    'balancer.test.plugin.connection',
    'balancer.test.plugin.dnsfake',
    'balancer.test.plugin.fs',
    'balancer.test.plugin.logger',
    'balancer.test.plugin.options',
    'balancer.test.plugin.port',
    'balancer.test.plugin.process',
    'balancer.test.plugin.resource',
    'balancer.test.plugin.server',
    'balancer.test.plugin.static',
    'balancer.test.plugin.stream',
    'balancer.test.plugin.sync',
    'balancer.test.plugin.tcpdump',
])


__MANAGERS_ATTR = 'MANAGERS'
__CONTEXTS_ATTR = 'CONTEXTS'


def __get_attrs(attr_name):
    return list(itertools.chain(*[
        getattr(plugin, attr_name) for plugin in __KNOWN_PLUGINS if hasattr(plugin, attr_name)
    ]))


__ALL_MANAGERS = __get_attrs(__MANAGERS_ATTR)
__ALL_CONTEXTS = __get_attrs(__CONTEXTS_ATTR)


Manager = namedtuple('Manager', [m.manager_name for m in __ALL_MANAGERS])


__MANAGER_FIXTURE_TEMPLATE = '''\
def manager({args}):
    return Manager({args_map})
'''


def __gen_manager_fixture():
    namespace = dict(
        Manager=Manager,
    )
    args_str = ', '.join([m.fixture_name for m in __ALL_MANAGERS])
    args_map_str = ', '.join(['{}={}'.format(m.manager_name, m.fixture_name) for m in __ALL_MANAGERS])
    code_str = __MANAGER_FIXTURE_TEMPLATE.format(args=args_str, args_map=args_map_str)
    exec(code_str, namespace)
    return namespace['manager']


manager = multiscope.fixture(pytest_fixtures=[
    m.fixture_name for m in __ALL_MANAGERS if m.fixture_type == multiscope.FixtureType.PYTEST
], parent=sys.modules[__name__])(__gen_manager_fixture())


@multiscope.fixture(pytest_fixtures=['request', 'session_state', 'module_state', 'class_state'])
class StateFixture(object):
    __fixturename__ = 'state'

    @staticmethod
    def session_state():
        return mod_ctx.State()

    @staticmethod
    def module_state(session_state):
        return mod_ctx.State(session_state)

    @staticmethod
    def class_state(module_state, request):
        if request.cls is not None:
            return mod_ctx.State(module_state)
        else:
            return None

    @staticmethod
    def function_state(module_state, class_state):
        parent_state = class_state if class_state is not None else module_state
        return mod_ctx.State(parent_state)


def __ctx_init(self, request, manager, state):
    self.__request = request
    self.__manager = manager
    self.__state = state
    super(Context, self).__init__()


@property
def __ctx_request(self):
    return self.__request


@property
def __ctx_manager(self):
    return self.__manager


@property
def __ctx_state(self):
    return self.__state


Context = type('Context', tuple(__ALL_CONTEXTS), dict(
    __init__=__ctx_init,
    request=__ctx_request,
    manager=__ctx_manager,
    state=__ctx_state,
))


@multiscope.fixture(pytest_fixtures=['request'])
def ctx(request, manager, state):
    return Context(request, manager, state)


def create_fixture(mixin_cls, params=None, ids=None):
    class LocalContext(Context, mixin_cls):
        pass

    @pytest.fixture(params=params, ids=ids)
    def fixture_func(request, manager, state):
        return LocalContext(request, manager, state)

    return fixture_func


pytest_plugins = [p.__name__ for p in __KNOWN_PLUGINS]

# -*- coding: utf-8 -*-
import importlib
import balancer.test.plugin.context as mod_ctx


def __known_plugin(name):
    try:
        importlib.import_module(name)
        return True
    except ImportError:
        return False


def __filter_plugins(all_plugins):
    return [name for name in all_plugins if __known_plugin(name)]


pytest_plugins = __filter_plugins([
    'balancer.test.plugin.context',
    'balancer.test.plugin.dolbilo',
    'balancer.test.plugin.settings',
]) + mod_ctx.pytest_plugins

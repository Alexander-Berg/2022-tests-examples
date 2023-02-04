# -*- coding: utf-8 -*-
import pytest

from balancer.test.util.config._config import BaseBalancerConfig, BalancerFunctionalConfig, BalancerConfig, \
    gen_config_class, gen_cachedaemon_config_class


parametrize_thread_mode = pytest.mark.parametrize(
    'thread_mode',
    [False, True],
    ids=['threads_off', 'threads_on'])

__all__ = [
    BaseBalancerConfig,
    BalancerFunctionalConfig,
    BalancerConfig,
    gen_config_class,
    gen_cachedaemon_config_class,
]

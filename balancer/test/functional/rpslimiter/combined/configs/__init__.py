# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class(
    'RpslimiterCombinedConfig', 'rpslimiter_combined.lua',
    backends=['backend'], logs=['access_log', 'error_log', 'instance_log'],
    listen_ports=['limiter_port'],
    kwargs={
        'quota1': 1,
        'interval1': 1000,
        'quota2': 2,
        'interval2': 1000,
    }
)

# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class(
    'RpslimiterSimpleConfig', 'rpslimiter_instance.lua',
    logs=['access_log', 'error_log', 'instance_log'],
    backends=['peer1', 'peer2'],
    kwargs={
        'quota1': 1,
        'interval1': 1000,
        'quota2': 2,
        'interval2': 1000,
        'sync_interval': '200s'
    }
)

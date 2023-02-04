# -*- coding: utf-8 -*-

from balancer.test.util.config import gen_config_class

gen_config_class('ReopenlogSimple', 'reopenlog_simple.lua', backends=['backend', 'dummy_backend'],
        logs=['instance_log', 'error_log', 'access_log', 'sd_log'],
        kwargs={'workers': 1, 'pinger_log': 'pinger_log'})

gen_config_class('DynamicReopenlogSimple', 'dynamic_simple.lua', backends=['backend1', 'backend2'],
        logs=['dynamic_balancing_log'],
        kwargs={'backends_blacklist': None})

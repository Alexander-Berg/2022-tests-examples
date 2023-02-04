#!/usr/bin/env python
# -*- coding: utf-8 -*-

from balancer.test.util.config import gen_config_class

gen_config_class(
    'RequestReplierConfig',
    'request_replier.lua',
    backends=['main_backend', 'sink_backend'],
    logs=['errorlog', 'accesslog'],
    kwargs={
        'workers': None,
        'rate': None,
        'rate_file': None,
        'connect_timeout': '30ms',
        'sink_backend_timeout': '5s',
        'main_backend_timeout': '5s',
        'enable_failed_requests_replication': None,
    }
)

gen_config_class(
    'RequestReplierHashConfig',
    'request_replier_hash.lua',
    backends=['main_backend', 'sink_backend1', 'sink_backend2'],
    logs=['errorlog', 'accesslog'],
    kwargs={
        'workers': None,
        'rate': None,
        'rate_file': None,
        'connect_timeout': '30ms',
        'sink_backend_timeout': '5s',
        'main_backend_timeout': '5s',
        'enable_failed_requests_replication': None,
    }
)

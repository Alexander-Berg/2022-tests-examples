# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('SmartPingerConfig', 'smart_pinger.lua', args=['backend_port'], kwargs={
    'ping_request_data': 'GET /ping HTTP/1.1\r\n\r\n',
    'keepalive_count': None,
    'on_disable': True,
    'delay': None,
    'ttl': None,
    'min_samples_to_disable': None,
    'lo': None,
    'hi': None,
    'ping_disable_file': None,
    'connection_manager_required': None,
})

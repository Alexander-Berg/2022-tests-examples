# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('PingerConfig', 'pinger.lua', args=['backend_port'], kwargs={
    'backend_timeout': '5s',
    'delay': '5s',
    'histtime': '10s',
    'ping_request': 'GET /ping HTTP/1.1\nConnection: Close\r\n\r\n',
    'admin_uri': '/pinger_admin',
    'lo': 0.5,
    'hi': 0.7,
    'keepalive': False,
    'keepalive_count': 0,
    'check_file': None,
    'switch_off_file': None,
    'switch_off_key': None,
    'admin_error_replier_status': None,
    'status_codes': None,
    'status_codes_exceptions': None,
    'connection_manager_required': None,
})

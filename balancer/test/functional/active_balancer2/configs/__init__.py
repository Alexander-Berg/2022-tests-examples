# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ActiveEasyConfig', 'active.lua', args=['backend_port1', 'backend_port2'], kwargs={
    'attempts': None,
    'active_skip_attempts': None,
    'steady': True,
    'delay': 0.7,
    'weight1': None,
    'weight2': None,
    'quorum': None,
    'hysteresis': None,
    'workers': 1,
    'request': 'GET / HTTP/1.1\r\n\r\n',
    'tcp_check': None,
    'keepalive_count': None,
    'connection_manager_required': None,
})
gen_config_class('HashingActiveEasyConfig', 'hashing_active.lua', args=['backend_port1', 'backend_port2'], kwargs={
    'attempts': None,
    'active_skip_attempts': None,
    'steady': True,
    'request': 'GET / HTTP/1.1\r\n\r\n',
    'delay': 0.7
})
gen_config_class('ActiveConfig', 'active.lua', args=['backend_port1', 'backend_port2', 'request'], kwargs={
    'delay': '1s',
    'attempts': 1,
    'workers': 1,
    'active_skip_attempts': None,
    'steady': True,
    'pinger': False,
    'use_backend_weight': False,
})
gen_config_class('ActiveEasyConfigWithPinger', 'active.lua', args=['backend_port1', 'backend_port2'], kwargs={
    'attempts': None,
    'active_skip_attempts': None,
    'steady': True,
    'delay': 0.7,
    'weight1': None,
    'weight2': None,
    'quorum': None,
    'hysteresis': None,
    'workers': 1,
    'pinger': True,
    'request': 'GET / HTTP/1.1\r\n\r\n',
    'tcp_check': None,
    'use_backend_weight': None,
    'backend_weight_disable_file': None,
    'keepalive_count': None,
    'connection_manager_required': None,
})
gen_config_class('ActiveConfigWithPinger', 'active.lua', args=['backend_port1', 'backend_port2', 'request'], kwargs={
    'delay': '1s',
    'attempts': 1,
    'active_skip_attempts': None,
    'workers': 1,
    'steady': True,
    'pinger': True
})
gen_config_class('HashingActiveConfig', 'hashing_active.lua', args=['backend_port1', 'backend_port2', 'request'], kwargs={
    'delay': '1s',
    'attempts': 1,
    'active_skip_attempts': None,
    'steady': True,
})
gen_config_class('ActiveOnErrorConfig', 'active_on_error.lua', args=['backend_port1', 'backend_port2'])
gen_config_class('ActiveWithPingerOnErrorConfig', 'active_on_error.lua', args=['backend_port1', 'backend_port2'], kwargs={
    'workers': 1,
    'pinger': True
})
gen_config_class('HashingActiveOnErrorConfig', 'hashing_active_on_error.lua', args=['backend_port1', 'backend_port2'])
gen_config_class('ActiveNoWorkersConfig', 'active_no_workers.lua', args=['backend_port1', 'backend_port2'])
gen_config_class('HashingActiveNoWorkersConfig', 'hashing_active_no_workers.lua', args=['backend_port1', 'backend_port2'])
gen_config_class('ActiveThreeBackends', 'active_three_backends.lua', args=['backend_port1', 'backend_port2', 'backend_port3'], kwargs={
    'attempts': None,
    'active_skip_attempts': None,
    'steady': True,
    'request': None,
    'delay': 0.7,
    'weight1': None,
    'weight2': None,
    'quorum': None,
    'hysteresis': None,
    'workers': 1,
})
gen_config_class('IgnoreActiveBackends', 'active_ignore_quorum.lua', args=['backend_port1', 'backend_port2', 'backend_port3'], kwargs={
    'attempts': None,
    'active_skip_attempts': None,
    'steady': True,
    'request': None,
    'delay': 0.7,
    'weight1': None,
    'weight2': None,
    'quorum': None,
    'hysteresis': None,
    'workers': 1,
})

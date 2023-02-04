# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('RpcRewriteConfig', 'rpcrewrite.lua', backends=['rpc', 'backend'], logs=['log', 'errorlog', 'accesslog'],
                 kwargs={'timeout': '5s', 'url': '/proxy/', 'host': 'localhost', 'dry_run': 0,
                         'keepalive_count': None, 'rpc_success_header': None,
                         'file_switch': None})

gen_config_class('RpcRewriteOnErrorConfig', 'rpcrewrite_on_error.lua', backends=['rpc', 'backend', 'on_error_backend'],
                 logs=['log', 'errorlog', 'accesslog'],
                 kwargs={'timeout': '5s', 'url': '/proxy/', 'host': 'localhost', 'dry_run': 0,
                         'keepalive_count': None, 'rpc_success_header': None,
                         'file_switch': None})
gen_config_class(
    'RpcRewriteWeightedConfig', 'rpcrewrite_weighted.lua', backends=['rpc', 'backend1', 'backend2'],
    logs=['log', 'errorlog', 'accesslog'], kwargs={
        'timeout': '5s', 'url': '/proxy/', 'host': 'localhost', 'dry_run': 0,
        'keepalive_count': None, 'rpc_success_header': None
    }
)

gen_config_class(
    'RpcRewriteOnErrorWeightedConfig', 'rpcrewrite_weighted_on_error.lua', backends=['rpc', 'on_error_backend', 'backend1', 'backend2'],
    logs=['log', 'errorlog', 'accesslog'], kwargs={
        'timeout': '5s', 'url': '/proxy/', 'host': 'localhost', 'dry_run': 0,
        'keepalive_count': None, 'rpc_success_header': None
    }
)

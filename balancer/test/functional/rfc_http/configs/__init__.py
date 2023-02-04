# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

__PARAMS = {
    'args': ['backend_port'],
    'logs': ['log', 'errorlog', 'accesslog'],
    'kwargs': {'keepalive_count': None, 'backend_timeout': None, 'keepalive': None, 'connection_manager_required': None}
}


gen_config_class('ProxyConfig', 'proxy.lua', **__PARAMS)
gen_config_class('ResponseHeadersConfig', 'response_headers.lua', **__PARAMS)
gen_config_class('ReportConfig', 'report.lua', **__PARAMS)
gen_config_class('CacheConfig', 'cache.lua', **__PARAMS)
gen_config_class('HeadersConfig', 'headers.lua', **__PARAMS)

# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('CacheConfig', 'cache.lua', backends=['backend1'], logs=['errorlog', 'accesslog'],
kwargs={
    'cache_ttl': '10s',
    'backend_timeout': '5s',
    'keepalive': True,
    'ignore_cache_control': False,
}),


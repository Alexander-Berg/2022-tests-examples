# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('CacheConfig', 'cache.lua', backends=['backend'], logs=['errorlog'], kwargs={
    'ttl': '10s',
    'mem': 1024 * 1024,
    'check': True,
    'match': '/\\?m=(\\d+)&n=(\\d+).*',
    'backend_timeout': '5s',
    'keepalive': True,
})


gen_config_class('CacheWorkersConfig', 'cache_workers.lua', backends=['backend'], logs=['errorlog', 'log'], kwargs={
    'ttl': '10s',
    'mem': 1024 * 1024,
    'check': True,
    'match': '/\\?m=(\\d+)&n=(\\d+).*',
    'async_init': False,
})

gen_config_class('CacheErrordocConfig', 'cache_errordoc.lua')

gen_config_class('CacheServerConfig', 'cache_server.lua')

gen_config_class('CacheClientConfig', 'cache_client.lua', backends=['server', 'backend'], logs=['errorlog'], kwargs={
    'ttl': '10s',
    'mem': 1024 * 1024,
    'check': True,
    'match': '/\\?m=(\\d+)&n=(\\d+).*',
    'timeout': '5s',
    'keepalive': True,
})
gen_config_class('CacheClientResponseHeadersConfig', 'cache_client_response_headers.lua', backends=['server', 'backend'], logs=['errorlog'], kwargs={
    'ttl': '10s',
    'mem': 1024 * 1024,
    'check': True,
    'match': '/\\?m=(\\d+)&n=(\\d+).*',
    'timeout': '5s',
    'keepalive': True,
})
gen_config_class('CacheClientExplicitServerConfig', 'cache_client.lua', args=['server_port'],
                 backends=['backend'], logs=['errorlog'], kwargs={
                     'ttl': '10s',
                     'mem': 1024 * 1024,
                     'check': True,
                     'match': '/\\?m=(\\d+)&n=(\\d+).*',
                     'timeout': '5s',
                     'keepalive': True,
                 })

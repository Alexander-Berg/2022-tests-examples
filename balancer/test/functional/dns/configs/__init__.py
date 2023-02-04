# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('DnsConfig', 'dns.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'dns_async_resolve': 'false',
    'reset_dns_cache_file': None,
    'cached_ip': None,
    'dns_timeout': None,
    'dns_ttl': None,
    'dns_ip': None,
    'dns_port': None,
    'dns_its_switch_file': None,
    'resolve_timeout': None,
    'connect_timeout': None,
    'backend_timeout': None,
    'host': 'localhost',
    'dns_its_switch_check': None,
    'dns_resolve_cached_ip_if_not_set': 'false'
})

gen_config_class('AsyncDnsConfig', 'dns.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'dns_async_resolve': 'true',
    'reset_dns_cache_file': None,
    'cached_ip': None,
    'dns_timeout': None,
    'dns_ttl': None,
    'dns_ip': None,
    'dns_port': None,
    'dns_its_switch_file': None,
    'resolve_timeout': None,
    'connect_timeout': None,
    'backend_timeout': None,
    'host': 'pbcznloiqpakow2g.man.yp-c.yandex.net',
    'dns_its_switch_check': None,
    'dns_resolve_cached_ip_if_not_set': 'false'
})
# -*- coding: utf-8 -*-

from balancer.test.util.config import gen_config_class

gen_config_class('SrcRwrConfig', 'srcrwr.lua', backends=['default_backend', 'srcrwr_backend'],
                 logs=['log', 'errorlog', 'accesslog'], kwargs={
        'id': None,
        'dns_resolve_cached_ip_if_not_set': 'false',
        'keepalive_count': None,
        'connection_manager_required': None,
})

gen_config_class('UntrustedSrcRwrConfig', 'srcrwr_untrusted.lua', backends=['default_backend', 'srcrwr_backend'],
                 logs=['log', 'errorlog', 'accesslog'], kwargs={
    'id': None,
    'dns_resolve_cached_ip_if_not_set': 'false'
})

gen_config_class('AnySrcRwrConfig', 'srcrwr_any.lua', backends=['default_backend', 'srcrwr_backend'],
                 logs=['log', 'errorlog', 'accesslog'], kwargs={
    'id': None,
    'dns_resolve_cached_ip_if_not_set': 'false'
})

gen_config_class('NestedSrcRwrConfig', 'srcrwr_nested.lua', backends=['fake_backend', 'default_backend', 'srcrwr_backend'],
                 logs=['log', 'errorlog', 'accesslog'], kwargs={
    'id': None,
    'dns_resolve_cached_ip_if_not_set': 'false'
})

gen_config_class('SrcRwrMultiConfig', 'srcrwr_multi.lua', backends=['default_backend', 'default_backend2', 'default_backend3', 'srcrwr_backend'],
                 logs=['log', 'errorlog', 'accesslog'], kwargs={
    'id': None,
    'attempts': 2,
    'dns_resolve_cached_ip_if_not_set': 'false'
})

gen_config_class('SrcRwrMultiConfig2', 'srcrwr_multi.lua', backends=['default_backend', 'default_backend2', 'default_backend3', 'srcrwr_backend', 'srcrwr_backend2'],
                 logs=['log', 'errorlog', 'accesslog'], kwargs={
    'id': None,
    'attempts': 2,
    'dns_resolve_cached_ip_if_not_set': 'false'
})

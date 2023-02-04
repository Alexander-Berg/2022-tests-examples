# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('HTTPBalancerConfig', 'http.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'maxlen': 65536,
    'maxreq': 65536,
    'maxheaders': None,
    'keepalive_count': None,
    'keepalive': None,
    'no_keepalive_file': None,
    'allow_trace': None,
    'allow_webdav': None,
    'allow_webdav_file': None,
    'multiple_hosts_enabled': None,
    'stats_attr': None,
    'keepalive_requests': None,
    'keepalive_timeout': None,
    'keepalive_drop_probability': None,
    'allow_connection_upgrade_without_connection_header': None,
    'allow_client_hints_restore': None,
    'client_hints_ua_header': None,
    'client_hints_ua_proto_header': None,
    'disable_client_hints_restore_file': None,
})

gen_config_class('HTTPStaticConfig', 'http_static.lua',  logs=['log', 'errorlog', 'accesslog'], kwargs={
    'maxlen': 65536,
    'maxreq': 65536,
    'maxheaders': None,
    'keepalive_count': None,
    'keepalive': None,
    'no_keepalive_file': None,
    'allow_trace': None,
    'multiple_hosts_enabled': None,
    'stats_attr': None,
    'keepalive_requests': None,
    'keepalive_timeout': None,
    'keepalive_drop_probability': None,
    'allow_connection_upgrade_without_connection_header': None,
    'ban_requests_file': None,
})

gen_config_class('WSAdditionalConfig', 'ws_additional.lua', backends=['backend1', 'backend2'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'maxlen': 65536,
    'maxreq': 65536,
    'maxheaders': None,
    'keepalive_count': None,
    'keepalive': None,
    'no_keepalive_file': None,
    'allow_trace': None,
    'multiple_hosts_enabled': None,
    'stats_attr': None,
    'attempts': None,
    'mode': None,
    'hedged_delay': None,
})

gen_config_class('WSAdditionalAntirobotConfig', 'ws_additional.lua', backends=['backend1', 'antirobot1'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'maxlen': 65536,
    'maxreq': 65536,
    'maxheaders': None,
    'keepalive_count': None,
    'keepalive': None,
    'no_keepalive_file': None,
    'allow_trace': None,
    'multiple_hosts_enabled': None,
    'stats_attr': None,
    'attempts': None,
    'mode': None,
})

gen_config_class(
    'CompressorConfig', 'compressor.lua',
    logs=['accesslog', 'errorlog'],
    backends=['backend'],
    kwargs={
        'enable_compression': None,
        'enable_decompression': None,
        'compression_codecs': None,
    }
)

gen_config_class('CyclesConfig', 'cycles.lua', backends=['default_backend'],
    logs=['log', 'errorlog', 'accesslog'],
    kwargs={
        'enable_cycles_protection': None,
        'max_cycles': None,
        'cycles_header_len_alert': None,
        'disable_cycles_protection_file': None,
    }
)
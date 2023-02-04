# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('AccesslogConfig', 'accesslog.lua', backends=['backend'], logs=['accesslog'], kwargs={
    'maxreq': None,
    'maxlen': None,
    'additional_ip_header': None,
    'additional_port_header': None,
    'log_instance_dir': None,
    'custom_accesslog': None,
})

gen_config_class('AccesslogHeadersConfig', 'accesslog_headers.lua', backends=['backend'], logs=['accesslog'], kwargs={
    'additional_ip_header': None,
    'additional_port_header': None,
})

gen_config_class('AccesslogExplicitLogConfig', 'accesslog.lua', backends=['backend'], args=['accesslog'])
gen_config_class('NestedAccesslogConfig', 'nested_accesslog.lua', logs=['common_log', 'led_log', 'zeppelin_log'])
gen_config_class('CacheAccesslogConfig', 'cache_accesslog.lua', backends=['backend'], logs=['accesslog'])
gen_config_class('AccesslogAttemptsConfig', 'accesslog_attempts.lua', backends=['backend'], logs=['accesslog'])
gen_config_class('AccesslogAttemptsOnErrorConfig', 'accesslog_attempts_on_error.lua', backends=['backend'], logs=['accesslog'])
gen_config_class('AccesslogAttemptsOnError2Config', 'accesslog_attempts_on_error2.lua', backends=['backend'], logs=['accesslog'])
gen_config_class('Http2AccesslogConfig', 'http2_accesslog.lua', backends=['backend'], args=['certs_dir', 'accesslog'])

gen_config_class('AccesslogPPConfig', 'accesslog_prefix_path.lua', backends=['backend'], logs=['accesslog'], kwargs={
    'errors_only': None,
    'logged_share': None,
})

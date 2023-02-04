# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('SSLClientConfig', 'ssl_client.lua', logs=['log', 'errorlog', 'accesslog'], args=['backend_port'], kwargs={
    'keepalive_count': None,
    'keepalive_timeout': None,
    'keepalive_check_for_unexpected_data': None,
    'backend_timeout': None,
    'ciphers': None,
    'ca_file': None,
    'sni_on': None,
    'sni_host': None,
    'verify_depth': None,
    'watch_client_close': None,
    'connection_manager_required': None,
})

gen_config_class('SSLClientTcpConfig', 'ssl_client_tcp.lua', logs=['log', 'errorlog'], args=['backend_port'], kwargs={
    'keepalive_count': None,
    'keepalive_timeout': None,
    'backend_timeout': None,
    'ciphers': None,
    'ca_file': None,
    'sni_on': None,
    'sni_host': None,
    'verify_depth': None,
    'watch_client_close': None,
    'connection_manager_required': None,
})

gen_config_class('SSLServerConfig', 'ssl_server.lua', logs=['log', 'errorlog', 'accesslog'], args=['priv', 'cert', 'ca'])

gen_config_class('ProxyConfig', 'proxy.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'host': 'localhost',
    'use_only_ipv4': None,
    'use_only_ipv6': None,
    'keepalive_count': None,
    'keepalive_timeout': None,
    'tcp_keep_intvl': None,
    'tcp_keep_idle': None,
    'tcp_keep_cnt': None,
    'backend_timeout': None,
    'fail_on_5xx': None,
    'need_resolve': None,
    'cached_ip': None,
    'dns_async_resolve': None,
    'status_code_blacklist': None,
    'status_code_blacklist_exceptions': None,
    'backend_read_timeout': None,
    'backend_write_timeout': None,
    'client_read_timeout': None,
    'client_write_timeout': None,
    'allow_connection_upgrade': None,
    'buffer': None,
    'dns_resolve_cached_ip_if_not_set': 'false',
    'watch_client_close': None,
    'socket_out_buffer': None,
    'connection_manager_required': None,
})

gen_config_class('ProxyConfigAsyncDns', 'proxy_async_dns.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'host': 'localhost',
    'use_only_ipv4': None,
    'use_only_ipv6': None,
    'backend_timeout': None,
    'need_resolve': None,
    'cached_ip': None,
    'dns_async_resolve': 'true',
    'dns_ip': None,
    'dns_port': None,
    'dns_timeout': None,
    'dns_ttl': None,
    'dns_resolve_cached_ip_if_not_set': 'false',
    'watch_client_close': None,
})

gen_config_class('ProxyStatusCodeBlacklistConfig', 'proxy_status_code_blacklist.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'status_code_blacklist': None,
    'status_code_blacklist_exceptions': None,
    'watch_client_close': None,
})

gen_config_class('ProxyDefaultsConfig', 'proxy_defaults.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'watch_client_close': None,
})
gen_config_class('ProxyRetryConfig', 'proxy_retry.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'watch_client_close': None,
})
gen_config_class('ProxyBufferingConfig', 'proxy_buffering.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'watch_client_close': None,
})
gen_config_class('BugNotWaitingResponseConfig', 'bug_not_waiting_response.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'backend_timeout': None,
    'watch_client_close': None,
})

gen_config_class('ProxyResolutionErrorConfig', 'proxy_resolution_error.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'cached_ip': None,
    'log_level': None,
    'watch_client_close': None,
})

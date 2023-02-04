# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('HeadersConfig', 'headers.lua', backends=['backend'], kwargs={
    'enable_delete': None,
    'delete_regexp': None,
    'enable_create': None,
    'enable_create_custom': None,
    'enable_create_multiple': None,
    'enable_create_func': None,
    'enable_create_from_file': None,
    'header': None,
    'value': None,
    'func': None,
    'filename': None,
    'enable_create_weak': None,
    'enable_create_func_weak': None,
    'enable_create_from_file_weak': None,
    'multiple_hosts_enabled': None,
    'enable_append': None,
    'enable_append_weak': None,
    'enable_append_func': None,
    'enable_append_func_weak': None,
    'enable_copy': None,
    'enable_copy_weak': None,
    'copy_src_header': None,
    'copy_dst_header': None,
    'delimiter': None,
    'p0f_enabled': None,
    'rules_file': None
})
gen_config_class('HeadersNamesakeConfig', 'headers_namesake.lua', backends=['backend'])
gen_config_class('HeadersProtoSchemeConfig', 'headers_proto_scheme.lua', backends=['backend'],
                 listen_ports=['http2_port'], args=['cert_dir'])
gen_config_class('HeadersCreateMultipleConfig', 'headers_create_multiple.lua', backends=['backend'],
                 args=['create_mod_name', 'realip_header', 'realport_header', 'url_header'])


gen_config_class('HeadersOpenSSLSimpleClientConfig', 'headers_openssl_simple_client.lua',
    args=['cert_dir'], logs=['errorlog', 'log'], backends=['backend'],
    kwargs={
        'workers': None,
        'timeout': None,
        'ciphers': None,
        'no_ca': None,
        'ca': None,
        'cert': None,
        'priv': None,
        'ocsp': None,
        'force_ssl': None,
        'verify_depth': None,
        'verify_peer': None,
        'verify_once': None,
        'fail_if_no_peer_cert': None,
        'crl': None,
        'ticket': None,
        'ja3_enabled': None,
    }
)

# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class(
    'OpenSSLSimpleConfig', 'openssl_simple.lua', args=['cert_dir'],
    logs=['errorlog', 'log'],
    kwargs={
        'workers': None,
        'timeout': None,
        'ciphers': None,
        'no_ca': None,
        'ca': None,
        'cert': None,
        'priv': None,
        'ocsp': None,
        'ocsp_file_switch': None,
        'force_ssl': None,
        'validate_cert_date': None,
    }
)

gen_config_class(
    'OpenSSLDualCertsConfig', 'openssl_dualcerts.lua', args=['cert_dir'],
    logs=['errorlog', 'log'],
    kwargs={
        'workers': None,
        'timeout': None,
        'ciphers': None,
        'no_ca': None,
        'ca': None,
        'cert': None,
        'priv': None,
        'ocsp': None,
        'ocsp_file_switch': None,
        'old_ca': None,
        'old_cert': None,
        'old_priv': None,
        'old_ocsp': None,
        'old_ocsp_file_switch': None,
        'force_ssl': None,
    }
)

gen_config_class(
    'OpenSSLComplexConfig', 'openssl_complex.lua',
    logs=[
        'errorlog',
        'default_log',
        'detroit_log',
        'vegas_log',
        'default_secrets_log',
        'detroit_secrets_log',
        'vegas_secrets_log'
    ],
    args=['cert_dir'],
    kwargs={
        'timeout': None,
        'ciphers': None,
        'default_ticket_prio_1': None,
        'default_ticket_prio_2': None,
        'detroit_server_name': None,
        'vegas_server_name': None,
        'default_reload_ocsp': None,
        'detroit_reload_ocsp': None,
        'vegas_reload_ocsp': None,
        'default_reload_tickets': None,
        'detroit_reload_tickets': None,
        'vegas_reload_tickets': None,
        'force_ssl': None,
        'ssl_protocols': None,
        'tickets_validate': None,
        'secrets_log_freq': None,
        'secrets_log_freq_file': None,
        'log_ciphers_stats': None,
        'early_data': None,
        'default_item_early_data': None,
        'alpn_freq': None,
    }
)

gen_config_class(
    'OpenSSLComplexDualCertsConfig', 'openssl_complex_dualcerts.lua',
    logs=[
        'errorlog',
        'default_log',
        'detroit_log',
        'vegas_log',
        'default_secrets_log',
        'detroit_secrets_log',
        'vegas_secrets_log'
    ],
    args=['cert_dir'],
    kwargs={
        'timeout': None,
        'ciphers': None,
        'default_ticket_prio_1': None,
        'default_ticket_prio_2': None,
        'detroit_server_name': None,
        'vegas_server_name': None,
        'default_reload_ocsp': None,
        'detroit_reload_ocsp': None,
        'vegas_reload_ocsp': None,
        'default_reload_tickets': None,
        'detroit_reload_tickets': None,
        'vegas_reload_tickets': None,
        'force_ssl': None,
        'ssl_protocols': None,
        'secrets_log_freq': None,
        'secrets_log_freq_file': None,
        'log_ciphers_stats': None,
    }
)

gen_config_class(
    'OpenSSLH2Config', 'openssl_h2.lua', logs=['log'], args=['cert_dir'], kwargs={
        'workers': None,
        'timeout': None,
        'ciphers': None,
        'force_ssl': None,
        'http2_alpn_file': None,
        'http2_alpn_freq': None,
        'http2_alpn_rand_mode': None,
        'http2_alpn_rand_mode_file': None,
        'http2_alpn_exp_id': None,
        'content': None,
        'max_concurrent_streams': None,
        'exp_rate_file': None,
    }
)

gen_config_class(
    'OpenSSLSimpleClientConfig', 'openssl_simple_client.lua', args=['cert_dir'],
    logs=['errorlog', 'log'],
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
    }
)

gen_config_class(
    'OpenSSLDualCertsClientConfig', 'openssl_dualcerts_client.lua', args=['cert_dir'],
    logs=['errorlog', 'log'],
    kwargs={
        'workers': None,
        'timeout': None,
        'ciphers': None,
        'no_ca': None,
        'ca': None,
        'cert': None,
        'priv': None,
        'ocsp': None,
        'old_ca': None,
        'old_cert': None,
        'old_priv': None,
        'old_ocsp': None,
        'force_ssl': None,
        'verify_depth': None,
        'verify_peer': None,
        'verify_once': None,
        'fail_if_no_peer_cert': None,
        'crl': None,
    }
)

gen_config_class(
    'OpenSSLMultictxClientConfig', 'openssl_multictx_client.lua', args=['cert_dir'],
    logs=['errorlog', 'log'],
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
        'other_ca': None,
        'other_cert': None,
        'other_priv': None,
        'other_ocsp': None,
        'other_crl': None,
        'erase_default_client': None,
        'erase_other_client': None,
    }
)

gen_config_class(
    'OpenSSLMaxSendFragmentConfig', 'openssl_max_send_fragment.lua', args=['cert_dir'],
    logs=['errorlog', 'log'],
    kwargs={
        'workers': None,
        'timeout': None,
        'ciphers': None,
        'no_ca': None,
        'ca': None,
        'cert': None,
        'priv': None,
        'ocsp': None,
        'ocsp_file_switch': None,
        'force_ssl': None,
        'content': None,
        'max_send_fragment': None,
        'client_write_delay': None,
        'client_read_delay': None,
        'client_write_size': None,
        'client_read_size': None,
    }
)

gen_config_class(
    'OpenSSLExpContextsConfig', 'openssl_exp_contexts.lua',
    logs=[
        'errorlog',
        'default_log',
        'detroit_log',
        'vegas_log',
        'default_secrets_log',
        'detroit_secrets_log',
        'vegas_secrets_log'
    ],
    args=['cert_dir'],
    kwargs={
        'timeout': None,
        'ciphers': None,
        'default_ticket_prio_1': None,
        'default_ticket_prio_2': None,
        'detroit_server_name': None,
        'vegas_server_name': None,
        'default_reload_ocsp': None,
        'detroit_reload_ocsp': None,
        'vegas_reload_ocsp': None,
        'default_reload_tickets': None,
        'detroit_reload_tickets': None,
        'vegas_reload_tickets': None,
        'force_ssl': None,
        'exp_id': None,
        'cont_id': None,
        'exp_id_nested': None,
        'cont_id_nested': None,
        'salt': None,
        'slots_count': None,
        'exp_rate_file': None,
        'ssl_protocols': None,
    }
)

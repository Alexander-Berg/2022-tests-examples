# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class(
    'HTTP2Config', 'http2.lua',
    backends=['backend'],
    args=['certs_dir'],
    logs=['error', 'http2_errorlog.log', 'http2_accesslog.log'],
    kwargs={
        'force_ssl': True,
        'allow_http2_without_ssl': False,
        'max_concurrent_streams': 128,
        'backend_timeout': 5,
        'streams_closed_max': 10,
        'streams_prio_queue_type': 'nginx',
        'edge_prio_fix_enabled': 1,
        'stream_send_queue_size_max': 2**16,
        'header_table_size': 2**12,
        'header_list_size': 2**18,
        'maxconn': None,
        'max_req': 2**16,
        'max_frame_size': 2**14,
        'initial_window_size': 2**16 - 1,
        'refused_stream_file_switch': None,
        'check_http_client_read_error': None,
    }
)

gen_config_class(
    'HTTP2BadConfig', 'http2_bad.lua',
    args=['certs_dir', 'mod'],
)

gen_config_class(
    'RpcRewriteConfig', 'rpcrewrite.lua',
    args=['certs_dir'],
    backends=['rpc', 'backend'],
    logs=['accesslog'],
    kwargs={
        'rpc_timeout': '5s',
        'backend_timeout': '5s',
        'url': '/proxy/',
        'host': 'localhost',
        'dry_run': 0,
        'keepalive_count': None,
        'rpc_success_header': None,
        'file_switch': None,
    }
)

gen_config_class(
    'ThresholdConfig', 'threshold.lua',
    args=['certs_dir'],
    backends=['backend'],
    logs=['accesslog', 'errorlog'],
    kwargs={
        'backend_timeout': '5s',
        'pass_timeout': '5s',
        'recv_timeout': '1s',
        'lo_bytes': 100,
        'hi_bytes': 1024 * 1024,
        'workers': None
    }
)

gen_config_class(
    'AntirobotConfig', 'antirobot.lua',
    args=['certs_dir'],
    backends=['antirobot', 'backend'],
    logs=['accesslog', 'errorlog'],
    kwargs={
        'antirobot_timeout': '5s',
        'backend_timeout': '5s',
        'workers': None
    }
)

gen_config_class(
    'CutterConfig', 'cutter.lua',
    args=['certs_dir'],
    backends=['backend'],
    logs=['accesslog', 'errorlog'],
    kwargs={
        'backend_timeout': '5s',
        'cutter_bytes': 1024 * 1024,
        'cutter_timeout': '5s',
        'workers': None
    }
)

gen_config_class(
    'ErrorDocumentConfig', 'errordocument.lua',
    args=['certs_dir'],
    logs=['accesslog', 'errorlog'],
    kwargs={
        'workers': None,
        'e_file': None,
    }
)

gen_config_class(
    'CpuLimiterConfig', 'cpu_limiter.lua',
    args=['certs_dir'],
    logs=['error', 'http2_errorlog.log', 'http2_accesslog.log'],
    kwargs={
        'max_concurrent_streams': 128,
        'backend_timeout': 5,
        'streams_closed_max': 10,
        'streams_prio_queue_type': 'nginx',
        'edge_prio_fix_enabled': 1,
        'stream_send_queue_size_max': 65536,
        'header_table_size': 65536,
        'maxconn': None,
        'enable_http2_drop': None,
        'http2_drop_lo': None,
        'http2_drop_hi': None,
        'disable_file': None,
        'disable_http2_file': None,
    }
)

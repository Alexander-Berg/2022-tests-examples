# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class(
    'ErrordocumentConfig',
    'errordocument.lua',
    logs=['errorlog', 'accesslog'],
    args=['status', ],
    kwargs={
        'content': None,
        'c_base64': None,
        'c_file': None,
        'socket_buffer': None,
        'force_conn_close': None,
        'remain_headers': None,
        'headers': None,
        'bad_header_name': None,
        'bad_header_value': None,
    })

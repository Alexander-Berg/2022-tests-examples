# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('HeadersForwarderConfig', 'headers_forwarder.lua', backends=['backend'], kwargs={
    'request_header': 'x-led',
    'response_header': 'x-zeppelin',
    'erase_from_request': None,
    'erase_from_response': None,
    'weak': None,
})


gen_config_class('HeadersForwarderRewriteConfig', 'headers_forwarder_rewrite.lua', backends=['backend'], kwargs={
    'request_header': None,
    'response_header': None,
    'header_value': None,
    'regexp': None,
    'rewrite': None,
    'erase_from_request': None,
})


gen_config_class('Http2HeadersForwarderConfig', 'http2_headers_forwarder.lua', backends=['backend'], kwargs={
    'certs_dir': None,
    'request_header': 'x-led',
    'response_header': 'x-zeppelin',
    'erase_from_request': None,
    'erase_from_response': None,
    'weak': None,
})


gen_config_class('HeadersForwarderMultipleConfig', 'headers_forwarder_multiple.lua', backends=['backend'], kwargs={
    'first_req_header': None,
    'first_resp_header': None,
    'second_req_header': None,
    'second_resp_header': None,
    'third_req_header': None,
    'third_resp_header': None,
})


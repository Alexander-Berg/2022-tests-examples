# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('HdrcgiConfig', 'hdrcgi.lua', backends=['backend'], kwargs={
    'cgi_1': 'cgi1',
    'cgi_hdr_1': 'X-Param-1',
    'cgi_2': 'cgi2',
    'cgi_hdr_2': 'X-Param-2',
    'hdr_1': 'X-Header-1',
    'hdr_cgi_1': 'header1',
    'hdr_2': 'X-Header-2',
    'hdr_cgi_2': 'header2',
    'body_scan_limit': 0
})

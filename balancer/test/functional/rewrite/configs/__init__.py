# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('RewriteConfig', 'rewrite.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'regexp': None,
    'rewrite': None,
    'literal': None,
    'case': None,
    'glob': None,
    'split': None,
    'header_name': None,
})
gen_config_class('RewriteSchemeConfig', 'rewrite_scheme.lua', listen_ports=['ssl_port'], logs=['log', 'errorlog', 'accesslog'], args=['cert_dir'])
gen_config_class('SeveralActionsConfig', 'several_actions.lua', backends=['backend'], logs=['log', 'errorlog', 'accesslog'], kwargs={
    'regexp1': None,
    'rewrite1': None,
    'header_name1': None,
    'regexp2': None,
    'rewrite2': None,
    'header_name2': None,
    'regexp3': None,
    'rewrite3': None,
    'header_name3': None,
})

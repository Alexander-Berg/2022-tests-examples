#!/usr/bin/enb python
# -*- coding: utf-8 -*-

from balancer.test.util.config import gen_config_class

gen_config_class('CgiHasherConfig', 'cgi_hasher.lua', kwargs={
    'param_prev': 'XXX',
    'param1': None,
    'param2': None,
    'param3': None,
    'randomize_empty_match': None,
    'combine_hashes': None,
    'case_insensitive': None,
    'mode': None,
    'backends_count': None,
})

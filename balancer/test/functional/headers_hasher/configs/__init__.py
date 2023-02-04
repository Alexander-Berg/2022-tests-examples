#!/usr/bin/enb python
# -*- coding: utf-8 -*-

from balancer.test.util.config import gen_config_class

gen_config_class('HeadersHasherConfig', 'headers_hasher.lua', kwargs={
    'header_name': None,
    'header_name_prev': 'XXX',
    'randomize_empty_match': None,
    'combine_hashes': None,
    'backends_count': None,
    'surround': None,
    'file_switch': None,
})

gen_config_class('HeadersHasherByHashConfig', 'headers_hasher_by_hash.lua', kwargs={
    'header_name': None,
    'randomize_empty_match': None,
    'backends_count': None,
    'surround': None,
    'file_switch': None,
})

#!/usr/bin/enb python
# -*- coding: utf-8 -*-

from balancer.test.util.config import gen_config_class

gen_config_class('CookieHasherConfig', 'cookie_hasher.lua', kwargs={
    'cookie': None,
    'cookie_prev': 'XXX',
    'combine_hashes': None,
    'randomize_empty_match': None,
    'backends_count': None,
    'file_switch': None,
})

gen_config_class('CookieHasherByHashConfig', 'cookie_hasher_by_hash.lua', kwargs={
    'cookie': None,
    'randomize_empty_match': None,
    'backends_count': None,
    'file_switch': None,
})

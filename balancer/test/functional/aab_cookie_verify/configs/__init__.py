# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('ModAabCookieVerifyConfig', 'aab_cookie_verify.lua', kwargs={
    'workers': None,
    'aes_key_path': None,
    'disable_antiadblock_file': None,
    'cookie': None,
    'cookie_lifetime': None,
    'ip_header': None,
})

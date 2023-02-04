#!/usr/bin/enb python
# -*- coding: utf-8 -*-

from balancer.test.util.config import gen_config_class

gen_config_class(
    'CookiePolicyConfig', 'cookie_policy.lua',
    logs=['accesslog', 'errorlog'],
    backends=['backend'],
    kwargs={
        'ssn_policy': None,
        'ssn_policy_name': 'ssn_policy',
        'ssn_policy_mode': None,
        'ssn_policy_override': None,
        'ssn_override': None,
        'ssn_name_re': None,

        'epl_policy': None,
        'epl_policy_name': 'epl_policy',
        'epl_policy_mode': None,
        'epl_policy_override': None,
        'epl_override': None,

        'prc_policy': None,
        'prc_policy_name': 'prc_policy',
        'prc_policy_mode': None,
        'prc_policy_override': None,
        'prc_override': None,
        'prc_name_re': None,

        'parser_mode': None,
        'default_yandex_policies': None,
        'file_switch': None,
        'gdpr_file_switch': None,
    }
)

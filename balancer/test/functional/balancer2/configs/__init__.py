# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ModBalancerDefaultConfig', 'mod_balancer_default.lua', backends=['backend'], kwargs={
    'backend_host': None,
})

gen_config_class('ModBalancerFailConfig', 'mod_balancer_fail.lua', backends=['backend1', 'backend2'], kwargs={
    'retry_non_idempotent': None,
    'attempts': None,
    'connection_attempts': None,
    'backend_host': None,
    'not_retryable_methods': None,
    'hedged_delay': None,
    'backend_timeout': 10,
    'backend_count': 2,
    'status_code_blacklist': None,
    'return_last_5xx': False,
    'on_error_status': None,
    'use_on_error_for_non_idempotent': None,
})

gen_config_class('BalancerSDConfig', 'mod_balancer_sd.lua', backends=['backend1', 'backend2'], kwargs={
    'algo': None,
    'backends_file': "",
    'active_quorum': None,
    'active_hysteresis': None,
    'termination_delay': None,
})

gen_config_class('ModBalancerDelayConfig', 'mod_balancer_delay.lua', backends=['backend'], kwargs={
    'attempts': None,
    'fast_attempts': None,
    'first_delay': None,
    'delay_multiplier': None,
    'delay_on_fast': None,
    'max_random_delay': None,
    'backend_timeout': None,
    'fast_503': None,
})

gen_config_class('ModBalancerErrorConfig', 'mod_balancer_errors.lua',
kwargs={
    'backend_host': None,
    'status': 200,
    'policy': 'simple_policy',
    'fast_503': None,
    'status_code_blacklist': None,
    'status_code_blacklist_exceptions': None,
    'on_error_status': None,
    'return_last_5xx': None,
    'return_last_blacklisted_http_code': None,
    'on_status_code': None,
    'on_status_code_content': None,
})

gen_config_class('ModBalancerHedgedConfig', 'mod_balancer_hedged.lua', backends=['backend1', 'backend2'], kwargs={
    'attempts': None,
    'hedged_delay': None,
    'accesslog': None,
})

gen_config_class('ModBalancerTwoLevelConfig', 'mod_balancer_two_level.lua', backends=['backend'], kwargs={})

gen_config_class('ModBalancerTwoLevelReturnLast5xxConfig', 'mod_balancer_two_level_return_last_5xx.lua',
kwargs={
    'inner_status_code_blacklist': None,
    'inner_return_last_5xx': None,
    'outer_status_code_blacklist': None,
    'outer_return_last_5xx': None,
})

gen_config_class('ModBalancerRrWeightsFile', 'mod_balancer_rr_weights_file.lua', backends=['backend'], logs=['log'], kwargs={
    'backend_host': None,
    'weights_file': None,
})

gen_config_class('ModBalancerRendezvousHashingWeighsFile', 'mod_balancer_rendezvous_hashing_weights_file.lua', backends=['backend'], logs=['log'], kwargs={
    'backend_host': None,
    'weights_file': None,
})

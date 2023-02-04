# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ModBalancerConfig', 'mod_balancer.lua', backends=['backend'], kwargs={
    'attempts': None,
    'attempts_file': None,
    'rewind_limit': None,
    'backend_timeout': None,
    'backend_host': None,
})

gen_config_class('ModBalancer2OnErrorConfig', 'mod_balancer2_on_error.lua', backends=['backend'], kwargs={
    'attempts': None,
    'attempts_file': None,
    'rewind_limit': None,
    'backend_timeout': None,
    'backend_host': None,
    'dns_async_resolve': None,
    'dns_timeout': None,
    'dns_ip': None,
    'dns_port': None,
    'dns_ttl': None,
    'dns_resolve_cached_ip_if_not_set': 'false'
})

gen_config_class('LeastconnConfig', 'leastconn.lua', backends=['backend1', 'backend2'])
gen_config_class('Weighted2Config', 'weighted2.lua', logs=['errorlog'], kwargs={
    'slow_reply_time': None,
    'weights_file': None,
})
gen_config_class('TwoBackendsConfig', 'two_backends.lua', backends=['backend1', 'backend2'], kwargs={
    'fail_on_5xx': None,
})
gen_config_class('RrobinConfig', 'rrobin.lua', logs=['errorlog'], kwargs={'weights_file': None})
gen_config_class('RrobinNamedConfig', 'rrobin_named.lua', kwargs={
    'weights_file': None,
    'first_weight': None,
    'second_weight': None,
    'use_randomize_initial_state': None,
    'corrowa': None,
    'use_balancer2': None,
})
gen_config_class('RrobinCorrowaConfig', 'rrobin_corrowa.lua', kwargs={
    'weights_file': None,
    'first_weight': None,
    'second_weight': None,
    'use_randomize_initial_state': None,
    'corrowa': None,
    'use_balancer2': None,
    'rr_count': None,
})
gen_config_class('ReportBackendConnectionErrorsConfig', 'report_backend_connection_errors.lua',
                 backends=['backend', 'second_backend', 'third_backend'],
                 kwargs={
                     'use_balancer2': None,
                 }
                 )
gen_config_class('Balancer2WatermarkPolicyConfig', 'b2_watermark_policy.lua', backends=['backend'], logs=['errorlog'], kwargs={
    'lo': None,
    'hi': None,
    'attempts': None,
    'backend_timeout': None,
    'params_file': None,
    'workers': None,
    'shared': None,
    'switch_key': None,
    'switch_file': None,
    'switch_default': None,
})
gen_config_class('Balancer2HashPolicyConfig', 'b2_by_hash_policy.lua')
gen_config_class('Balancer2HashPolicyStabilityConfig', 'b2_by_hash_policy_stability.lua', kwargs={
    'weights_file': None,
})
gen_config_class('Balancer2HashPolicyBackendsConfig', 'b2_by_hash_policy_backends.lua', backends=['first_backend', 'second_backend', 'third_backend'], kwargs={
    'attempts': None,
})
gen_config_class('Balancer2NamePolicyConfig', 'b2_by_name_policy.lua', backends=['first_backend', 'second_backend'], kwargs={
    'attempts': None,
    'name': None,
    'weights_file': None,
    'strict': None,
    'allow_zero_weights': None
})
gen_config_class('Balancer2NameFromDefaultHeaderPolicyConfig', 'b2_by_name_from_default_header_policy.lua', backends=['first_backend', 'second_backend'], kwargs={
    'attempts': None,
    'name': None,
    'weights_file': None,
    'strict': None,
    'allow_zero_weights': None
})
gen_config_class('Balancer2NameFromHeaderPolicyConfig', 'b2_by_name_from_header_policy.lua', backends=['first_backend', 'second_backend'], kwargs={
    'header_name': None,
    'attempts': None,
    'name': None,
    'weights_file': None,
    'strict': None,
    'allow_zero_weights': None
})
gen_config_class('Balancer2SimplePolicyConfig', 'b2_simple_policy.lua', backends=['first_backend', 'second_backend'])
gen_config_class('Balancer2TimeoutPolicyConfig', 'b2_timeout_policy.lua', backends=['backend'], logs=['errorlog'], kwargs={
    'timeout': None,
    'attempts': None,
    'backend_timeout': None,
    'params_file': None,
})
gen_config_class('Balancer2BackoffPolicyConfig', 'b2_backoff_policy.lua', backends=['backend'], kwargs={
    'attempts': None,
    'backend_timeout': None,
    'prob1': None,
    'prob2': None,
})
gen_config_class('Balancer2ConnectionAttemptsConfig', 'b2_connection_attempts.lua', backends=['backend', 'second_backend'], kwargs={
    'attempts': None,
    'connection_attempts': None,
    'fast_attempts': None,
    'fast_503': None,
})
gen_config_class('Balancer2ConnectionAttemptsReturnLast5xxConfig', 'b2_connection_attempts_return_last_5xx.lua', backends=['backend_503_1', 'backend_503_2', 'backend_503_3', 'backend_last_200'], kwargs={
    'attempts': None,
    'connection_attempts': None,
    'fast_attempts': None,
    'fast_503': None,
    'return_last_5xx': None,
})
gen_config_class('Balancer2TwoLevelsConfig', 'b2_two_levels.lua', backends=['backend_timeouted', 'backend_503', 'backend_fake'], kwargs={
    'connection_attempts': None,
    'fast_attempts': None,
    'fast_503': None,
})
gen_config_class('Balancer2AttemptsRateLimiterConfig', 'b2_attempts_rate_limiter.lua', backends=['backend'], logs=['errorlog'], kwargs={
    'attempts': None,
    'backend_timeout': None,
    'workers': None,
    'limit': None,
    'coeff': None,
    'max_budget': None,
    'fast_attempts': None,
    'connection_attempts': None,
    'hedged_delay': None,
    'return_last_5xx': None,
})
gen_config_class('Balancer2AttemptsRateLimiterTwoBackendsConfig', 'b2_attempts_rate_limiter_two_backends.lua', backends=['backend1', 'backend2'], logs=['errorlog'], kwargs={
    'attempts': None,
    'backend_timeout': None,
    'workers': None,
    'limit': None,
    'coeff': None,
    'max_budget': None,
    'fast_attempts': None,
    'connection_attempts': None,
    'hedged_delay': None,
})
gen_config_class('OnFastErrorConfig', 'on_fast_error.lua', backends=['backend'], logs=['accesslog', 'errorlog'], kwargs={
    'fast_503': None,
    'backend_weight': None,
})
gen_config_class('ByLocationConfig', 'by_location.lua', logs=['errorlog'], kwargs={
    'quorums_file': None,
    'weights_file': None,
    'preferred_location': None,
    'preferred_location_switch': None,
    'policy': None,
})
gen_config_class('ByLocation2LvlConfig', 'by_location_two_levels.lua', logs=['errorlog'], kwargs={
    'quorums_file': None,
    'weights_file': None,
    'preferred_location': None,
    'preferred_location_switch': None,
    'root_quorum': None,
    'root_amount_quorum': None,
    'amount_quorum1': None,
    'amount_quorum2': None,
    'amount_quorum3': None,
})

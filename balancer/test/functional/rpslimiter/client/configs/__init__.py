# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('RpsLimiterConfig', 'rps_limiter.lua', backends=['checker'], kwargs={
    'disable_file': None,
    'skip_on_error': None,
    'quota_name': None,
    'namespace': None,
    'log_quota': None,
})

gen_config_class('RpsLimiterOnErrorConfig', 'rps_limiter_on_error.lua', backends=['checker'], kwargs={
    'disable_file': None,
    'skip_on_error': None,
    'quota_name': None,
})

gen_config_class('RpsLimiterOnLimitedConfig', 'rps_limiter_on_limited.lua', backends=['checker'], kwargs={
    'disable_file': None,
    'skip_on_error': None,
    'quota_name': None,
})

gen_config_class('RpsLimiterAsyncConfig', 'rps_limiter_async.lua', backends=['checker'], kwargs={
    'namespace': None,
})

gen_config_class('RpsLimiterBalancerConfig', 'rps_limiter_balancer.lua', backends=['checker', 'backend'], kwargs={
    'attempts': None,
    'register_only': None,
    'register_backend_attempts': None,
    'namespace': None,
})

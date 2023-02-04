from balancer.test.util.config import gen_config_class


gen_config_class('RateLimiterConfig', 'rate_limiter.lua', logs=['errorlog', 'accesslog'], args=['backend_port'], kwargs={
    'interval': None,
    'max_requests': None,
    'max_requests_in_queue': None,
})

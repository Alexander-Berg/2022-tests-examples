# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class, BalancerConfig


gen_config_class('InstanceConfig', 'instance.lua', logs=['instance_log'], kwargs={
    'enable_reuse_port': None,
    'affinity_mask': None,
    'dns_timeout': None,
    'workers': None,
    'tcp_keep_idle': None,
    'tcp_keep_intvl': None,
    'tcp_keep_cnt': None,
    'private_address': None,
    'maxconn': None,
    'sosndbuf': None,
    'buffer': None,
    'set_no_file': False,
    'bad_admin_port': None,
    'bad_stats_port': None,
})


gen_config_class('CpuLimiterConfig', 'cpu_limiter.lua', logs=['instance_log', 'errorlog', 'accesslog'] ,kwargs={
    'enable_conn_reject': None,
    'enable_keepalive_close': None,
    'active_check_subnet': None,
    'active_check_subnet_default': None,
    'active_check_subnet_file': None,
    'conn_reject_lo': None,
    'conn_reject_hi': None,
    'conn_hold_count': None,
    'conn_hold_duration': None,
    'keepalive_close_lo': None,
    'keepalive_close_hi': None,
    'disable_file': None,
    'push_checker_address': None,
    'checker_address_cache_size': None,
})


gen_config_class('BadIpAddrConfig', 'ip_addrs_no_brackets.lua', logs=['instance_log'])


class CoroutineStackConfig(BalancerConfig):
    NAME = 'coroutine_stack.lua'

    def __init__(self, coro_stack_size=None):
        super(CoroutineStackConfig, self).__init__()
        self.coro_stack_size = coro_stack_size

    def build_opts(self):
        opts = super(CoroutineStackConfig, self).build_opts()
        if self.coro_stack_size is not None:
            opts.extend(['-C', str(self.coro_stack_size)])
        return opts


gen_config_class('LogQueueConfig', 'log_queue.lua', logs=['instance_log'], kwargs={
    'log_queue_max_size': None,
    'log_queue_submit_attempts_count': None,
    'log_queue_flush_interval': None
})

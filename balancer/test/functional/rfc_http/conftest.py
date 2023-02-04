# -*- coding: utf-8 -*-
from configs import ProxyConfig, ResponseHeadersConfig, ReportConfig, CacheConfig, HeadersConfig

import balancer.test.plugin.context as mod_ctx


class RFCContext(object):
    def __init__(self):
        super(RFCContext, self).__init__()
        self.__config_type = self.request.param

    def start_rfc_backend(self, config):
        return self.start_backend(config)

    def start_rfc_balancer(self, **balancer_kwargs):
        if hasattr(self, 'backend'):
            balancer_kwargs['backend_port'] = self.backend.server_config.port
        else:
            balancer_kwargs['backend_port'] = self.manager.port.get_port()
        return self.start_balancer(self.__config_type(**balancer_kwargs))


rfc_ctx = mod_ctx.create_fixture(
    RFCContext,
    params=[
        ProxyConfig,
        ResponseHeadersConfig,
        ReportConfig,
        CacheConfig,
        HeadersConfig,
    ],
    ids=[
        'proxy',
        'response_headers',
        'report',
        'cache',
        'headers',
    ],
)

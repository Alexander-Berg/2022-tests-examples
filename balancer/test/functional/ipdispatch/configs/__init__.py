# -*- coding: utf-8 -*-
from balancer.test.util.config import BalancerFunctionalConfig
from balancer.test.util.config import gen_config_class


class IPDispatchConfig(BalancerFunctionalConfig):
    NAME = 'ipdispatch.lua'

    def __init__(self):
        super(IPDispatchConfig, self).__init__()
        self.add_listen_port('led_port')
        self.add_listen_port('zeppelin_port')
        self.add_listen_port('port')


class IPDispatchPortOnlyConfig(BalancerFunctionalConfig):
    NAME = 'ipdispatch_port_only.lua'

    def __init__(self):
        super(IPDispatchPortOnlyConfig, self).__init__()
        self.add_listen_port('led_port')
        self.add_listen_port('zeppelin_port')
        self.add_listen_port('port')


class IpsPortsIPDispatchConfig(BalancerFunctionalConfig):
    NAME = 'ips_ports_ipdispatch.lua'

    def __init__(self, enable_ips, enable_ip, enable_ports, enable_port):
        super(IpsPortsIPDispatchConfig, self).__init__()
        self.add_listen_port('electric_port')
        self.add_listen_port('light_port')
        self.add_listen_port('orchestra_port')
        self.add_listen_port('port')

        self.add_param('enable_ips', enable_ips)
        self.add_param('enable_ip', enable_ip)
        self.add_param('enable_ports', enable_ports)
        self.add_param('enable_port', enable_port)


class IPDispatchInsideHttpConfig(BalancerFunctionalConfig):
    NAME = 'ipdispatch_inside_http.lua'

    def __init__(self, led_disabled=None, zeppelin_disabled=None):
        super(IPDispatchInsideHttpConfig, self).__init__()
        self.add_listen_port('led_port')
        self.add_listen_port('zeppelin_port')
        self.add_listen_port('port')

        self.add_param('led_disabled', led_disabled)
        self.add_param('zeppelin_disabled', zeppelin_disabled)


class IPDispatchBindHttpConfig(BalancerFunctionalConfig):
    NAME = 'ipdispatch_bind.lua'

    def __init__(self):
        super(IPDispatchBindHttpConfig, self).__init__()
        self.add_listen_port('port')


gen_config_class('DisabledConfig', 'disabled.lua', kwargs={
    'disabled': None,
})

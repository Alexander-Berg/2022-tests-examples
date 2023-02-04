# -*- coding: utf-8 -*-
from balancer.test.util.resource import AbstractResource


class ServerConfig(AbstractResource):
    def __init__(self):
        super(ServerConfig, self).__init__()
        self.__params = list()
        self.__listen_ports = list()
        self.__logs = list()
        self.__backends = list()
        self.__filled = False

    def register_param(self, name):
        self.__params.append(name)

    def add_param(self, name, value):
        self.__setattr__(name, value)
        self.register_param(name)

    def register_listen_port(self, name):
        self.__listen_ports.append(name)

    def add_listen_port(self, name):
        self.__setattr__(name, None)
        self.register_listen_port(name)

    def register_log(self, name):
        self.__logs.append(name)

    def add_log(self, name):
        self.__setattr__(name, None)
        self.register_log(name)

    def register_backend(self, name):
        self.__backends.append(name)
        self.register_param('{}_port'.format(name))

    def add_backend(self, name):
        self.__setattr__('{}_port'.format(name), None)
        self.register_backend(name)

    @property
    def params(self):
        return self.__params

    @property
    def listen_ports(self):
        return self.__listen_ports

    @property
    def logs(self):
        return self.__logs

    @property
    def backends(self):
        return self.__backends

    def _dump(self):
        pass

    @property
    def filled(self):
        self._dump()
        return self.__filled

    @filled.setter
    def filled(self, value):
        self.__filled = value

    def _finish(self):
        for port_attr in self.listen_ports:
            self.__getattribute__(port_attr).finish()


class ConfigManager(object):
    def __init__(self, port_manager, fs_manager):
        super(ConfigManager, self).__init__()
        self.__port_manager = port_manager
        self.__fs_manager = fs_manager
        self.__servers = dict()

    def __fill_ports(self, config, prev_config=None):
        def get_port(port_attr):
            if prev_config is not None:
                return getattr(prev_config, port_attr)
            else:
                return self.__port_manager.get_port()

        for port_attr in config.listen_ports:
            setattr(config, port_attr, get_port(port_attr))

    def __fill_logs(self, config):
        for log_attr in config.logs:
            setattr(config, log_attr, self.__fs_manager.create_file('{}.log'.format(log_attr)))

    def __fill_backends(self, config):
        for name in config.backends:
            if name not in self.__servers:
                continue
            setattr(config, '{}_port'.format(name), self.__servers[name].port)

    def add_server(self, name, config):  # FIXME: remember servers on config filling
        self.__servers[name] = config

    def fill(self, config, prev_config=None):
        if not config.filled:
            self.__fill_ports(config, prev_config)
            self.__fill_logs(config)
            self.__fill_backends(config)
            config.filled = True

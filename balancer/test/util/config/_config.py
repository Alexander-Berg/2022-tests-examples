# -*- coding: utf-8 -*-
import inspect
import os
import sys
import lua_to_json
from abc import ABCMeta, abstractmethod
from balancer.test.util.server import ServerConfig
from balancer.test.util import settings


def parse_thread_mode(thread_mode):
    return True if thread_mode == 'yes' else False


class BaseConfig(ServerConfig):
    __metaclass__ = ABCMeta
    NEW_OPT = None

    def build_opts(self):
        opts = list()
        for name, value in self.__all_opts():
            opts.append(self.NEW_OPT)
            opts.append('{}={}'.format(name, value))
        return opts

    def build_cgi_opts(self):
        opts = list()
        for name, value in self.__all_opts():
            opts.append('&V_{}={}'.format(name, value))
        return "".join(opts)

    @abstractmethod
    def get_path(self):
        raise NotImplementedError()

    def as_json(self):
        with open(self.get_path()) as f:
            return lua_to_json.load(f, dict(self.__all_opts()))

    def __all_opts(self):
        param_names = self.listen_ports + self.logs + self.params
        result = list()
        for name in param_names:
            value = self.__getattribute__(name)
            if value is not None:
                if isinstance(value, bool):
                    value = 'true' if value else 'false'
                result.append((name, value))
        return result


class BaseBalancerConfig(BaseConfig):
    NEW_OPT = '-V'


class BaseCacheDaemonConfig(BaseConfig):
    NEW_OPT = '-v'


class BalancerFunctionalConfig(BaseBalancerConfig):
    NAME = None

    def __init__(self):
        super(BalancerFunctionalConfig, self).__init__()
        self.add_listen_port('admin_port')

    @classmethod
    def get_path(cls):
        cls_path = os.path.abspath(sys.modules[cls.__module__].__file__)
        return os.path.join(os.path.dirname(cls_path), cls.NAME)


class BalancerConfig(BalancerFunctionalConfig):
    def __init__(self):
        super(BalancerConfig, self).__init__()
        self.add_listen_port('port')
        self.add_listen_port('stats_port')


class NginxConfig(BaseConfig):
    NAME = None
    NGINX_LOGS = [
        'errorlog',
        'pid_file',
        'client_body_temp_path',
        'proxy_temp_path',
        'fastcgi_temp_path',
        'uwsgi_temp_path',
        'scgi_temp_path',
    ]

    def __init__(self):
        super(NginxConfig, self).__init__()
        self.add_listen_port('admin_port')
        self.add_listen_port('port')
        for log in self.NGINX_LOGS:
            self.add_log(log)
        import jinja2
        tmpl_name = self.__calc_path(self.NAME)[:-3] + 'conf.tmpl'
        with open(tmpl_name) as f:
            self.__tmpl = jinja2.Template(f.read())
        self.__path = None

    def __calc_path(self, name):
        cls_path = os.path.abspath(sys.modules[self.__class__.__module__].__file__)
        return os.path.join(os.path.dirname(cls_path), name)

    def get_path(self):
        if self.__path is None:
            env = dict()
            for name in self.listen_ports + self.logs + self.params:
                value = self.__getattribute__(name)
                if value is not None:
                    env[name] = value
            self.__path = os.path.join(os.path.dirname(self.pid_file), self.NAME[:-3] + 'conf')
            with open(self.__path, 'w') as f:
                f.write(self.__tmpl.render(**env))
        return self.__path


class CacheDaemonFunctionalConfig(BaseCacheDaemonConfig):
    NAME = None

    def __init__(self):
        super(CacheDaemonFunctionalConfig, self).__init__()
        self.add_listen_port('port')

    @classmethod
    def get_path(cls):
        cls_path = os.path.abspath(sys.modules[cls.__module__].__file__)
        return os.path.join(os.path.dirname(cls_path), cls.NAME)


# class generation done like in collections.namedtuple
__CLASS_TEMPLATE = '''
class {class_name}({base_class_name}):
    NAME = '{config_name}'

    def __init__({init_args}):
        super({class_name}, self).__init__()

        for port in {listen_ports}:
            self.add_listen_port(port)

        for log in {logs}:
            self.add_log(log)

        for backend in {backends}:
            self.add_backend(backend)

'''
__PARAM_TEMPLATE = '        self.add_param(\'{param_name}\', {param_name})'


def gen_config_class(class_name, config_name,
                     args=None, kwargs=None, listen_ports=None, logs=None, backends=None):
    """
    Generates balancer config class. Must be called from __init__ file in configs directory.

    :param str class_name: generated class name
    :param str name: config file name
    :param list args: config positional arguments names
    :type kwargs: dict or list of pairs
    :param kwargs: config keyword arguments
    :param list listen_ports: names of listen ports
    :param list logs: names of logs
    """
    if args is None:
        args = list()
    if kwargs is None:
        kwargs = list()
    if isinstance(kwargs, dict):
        kwargs = kwargs.items()
    if listen_ports is None:
        listen_ports = list()
    if logs is None:
        logs = list()
    if backends is None:
        backends = list()

    frm = inspect.stack()[1]
    mod = inspect.getmodule(frm[0])

    if 'thread_mode' not in [kwarg[0] for kwarg in kwargs]:
        kwargs.append(('thread_mode', None))

    init_args = ', '.join(['self'] + args + ['{}={}'.format(k, repr(v)) for k, v in kwargs])
    template = __CLASS_TEMPLATE.format(
        class_name=class_name,
        base_class_name='BalancerConfig',
        config_name=config_name,
        init_args=init_args,
        listen_ports=str(listen_ports),
        logs=str(logs),
        backends=str(backends),
    )
    optional_params = [p[0] for p in kwargs]
    params = args + optional_params
    add_params = '\n'.join([__PARAM_TEMPLATE.format(param_name=param) for param in params])
    template += add_params

    if settings.flags.USE_NGINX:
        conf_cls = NginxConfig
    else:
        conf_cls = BalancerConfig
    namespace = dict(
        BalancerConfig=conf_cls,
    )
    exec(template, namespace)
    cls = namespace[class_name]
    cls.__module__ = mod.__name__
    setattr(mod, class_name, cls)


# TODO: get rid of copypaste. WARNING: black magic - cls.__module__ is changed
def gen_cachedaemon_config_class(class_name, config_name,
                                 args=None, kwargs=None, listen_ports=None, logs=None, backends=None):
    """
    Generates balancer config class. Must be called from __init__ file in configs directory.

    :param str class_name: generated class name
    :param str name: config file name
    :param list args: config positional arguments names
    :type kwargs: dict or list of pairs
    :param kwargs: config keyword arguments
    :param list listen_ports: names of listen ports
    :param list logs: names of logs
    """
    if args is None:
        args = list()
    if kwargs is None:
        kwargs = list()
    if isinstance(kwargs, dict):
        kwargs = kwargs.items()
    if listen_ports is None:
        listen_ports = list()
    if logs is None:
        logs = list()
    if backends is None:
        backends = list()

    frm = inspect.stack()[1]
    mod = inspect.getmodule(frm[0])

    init_args = ', '.join(['self'] + args + ['{}={}'.format(k, repr(v)) for k, v in kwargs])
    template = __CLASS_TEMPLATE.format(
        class_name=class_name,
        base_class_name='CacheDaemonFunctionalConfig',
        config_name=config_name,
        init_args=init_args,
        listen_ports=str(listen_ports),
        logs=str(logs),
        backends=str(backends),
    )
    optional_params = [p[0] for p in kwargs]
    params = args + optional_params
    add_params = '\n'.join([__PARAM_TEMPLATE.format(param_name=param) for param in params])
    template += add_params

    namespace = dict(
        CacheDaemonFunctionalConfig=CacheDaemonFunctionalConfig,
    )
    exec(template, namespace)
    cls = namespace[class_name]
    cls.__module__ = mod.__name__
    setattr(mod, class_name, cls)

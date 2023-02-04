# -*- coding: utf-8 -*-
import pytest
import os
import json
import xml.etree.ElementTree as et

from balancer.test.util import settings
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture
from balancer.test.util.predef import http

from balancer.test.util.balancer import BalancerManager


# FIXME
pytest_plugins = [
    'balancer.test.plugin.resource',
    'balancer.test.plugin.logger',
    'balancer.test.plugin.server',
    'balancer.test.plugin.stream',
    'balancer.test.plugin.process',
    'balancer.test.plugin.fs',
    'balancer.test.plugin.settings',
]


__BALANCER = settings.Tool(
    pytest_option_name='balancer',
    yatest_option_name='balancer',
    yatest_path='balancer/daemons/balancer/balancer',
)
__NGINX = settings.Tool(
    yatest_path='nginx/bin/nginx',
)


def pytest_addoption(parser):
    parser.addoption('-B', '--balancer', dest=__BALANCER.pytest_option_name,
                     default=None, help='path to balancer executable')
    debug = parser.getgroup('debug', description='debugging options')
    debug.addoption(
        '-I', '--instance', dest='instances', default=list(), action='append',
        help=(
            'use existing instance of balancer. Must be used with --start_port option. '
            'Format: <mater pid> or <master pid>:<admin port> for tests with multiple balancer instances'
        )
    )


@pytest.fixture(scope='session')
def balancer_path(test_tools):
    if not settings.flags.USE_NGINX:
        tool = __BALANCER
    else:
        tool = __NGINX
    path = test_tools.get_tool(tool)

    if not os.access(path, os.X_OK):
        raise OSError('[Errno 13] Balancer executable is not executable (check file permissions)')

    return path


class BalancerOptionsException(Exception):
    pass


@multiscope.fixture(pytest_fixtures=['balancer_path', 'logger', 'request'])
def balancer_manager(balancer_path, resource_manager, logger, fs_manager, connection_manager, config_manager, request):
    if request.config.getoption('instances'):
        if request.config.getoption('start_port') is None:
            raise BalancerOptionsException('start_port must be specified to use instance option')

    def parse_instance(inst):
        if ':' in inst:
            pid, port = inst.split(':')
            return int(pid), int(port)
        else:
            return int(inst), None
    instances = [parse_instance(inst) for inst in request.config.getoption('instances')]
    return BalancerManager(
        resource_manager=resource_manager,
        logger=logger,
        fs_manager=fs_manager,
        http_connection_manager=connection_manager.http,
        config_manager=config_manager,
        balancer_path=balancer_path,
        instances=instances,
    )


class BalancerContext(object):
    STATS_REQUEST = http.request.get('/admin?action=stats')
    STATS_REPORT_REQUEST = http.request.get('/admin/events/xmlwrapcall/report')
    UNISTAT_REQUEST = http.request.get('/unistat')
    SOLOMON_REQUEST = http.request.get('/solomon')

    def __init__(self):
        super(BalancerContext, self).__init__()
        self.state.register('balancer')  # FIXME: do not need to store balancer in state

    @property
    def balancer(self):
        return self.state.balancer

    def start_balancer(
        self,
        balancer_config,
        ping_request=None,
        env=None,
        timeout=None,
        check_workers=True,
        debug=False
    ):
        """
        Start balancer instance. The last started balancer is stored in context.
        """
        balancer = self.manager.balancer.start(
            balancer_config,
            ping_request=ping_request,
            env=env,
            timeout=timeout,
            check_workers=check_workers,
            debug=debug,
        )
        self.state.balancer = balancer
        if hasattr(balancer.config, 'port'):
            def get_port():
                return balancer.config.port
            self.state.default_port_func = get_port
        return balancer

    def get_unistat(self, port=None):
        if port is None:
            port = self.balancer.config.stats_port

        response = self.perform_request(self.UNISTAT_REQUEST, port)
        stats = json.loads(response.data.content)
        return dict(stats)

    def get_solomon(self, port=None):
        if port is None:
            port = self.balancer.config.stats_port

        response = self.perform_request(self.SOLOMON_REQUEST, port)
        stats = json.loads(response.data.content)

        res = dict()
        for sensor in stats['sensors']:
            name = sensor['labels']['sensor']
            res[name] = sensor
        return res

    def get_statistics(self, port=None):
        """
        :rtype: ElementTree
        """
        if port is None:
            port = self.balancer.config.admin_port
        response = self.perform_request(self.STATS_REQUEST, port)
        return et.fromstring(response.data.content)

    def call_json_event(self, event_name):
        resp = self.perform_request(http.request.get('/json/{}'.format(event_name)), self.balancer.config.stats_port)
        assert resp.status == 200
        return json.loads(resp.data.content)

    def get_worker_count(self):
        return self.get_unistat()['childs-alive_ammv']

    def get_stats_report(self, port=None):
        """
        :rtype: ElementTree
        """
        return et.fromstring(self.get_stats_report_raw(port))

    def get_stats_report_raw(self, port=None):
        if port is None:
            port = self.balancer.config.admin_port
        response = self.perform_request(self.STATS_REPORT_REQUEST, port)
        return response.data.content

    def graceful_shutdown(self, cooldown=None, timeout=None, close_timeout=None):
        if cooldown is None:
            cooldown = '0s'
        if timeout is None:
            timeout = '0s'
        if close_timeout is None:
            close_timeout = '0s'

        request = '/admin?action=graceful_shutdown&cooldown={}&timeout={}&close_timeout={}'.format(
            cooldown, timeout, close_timeout
        )

        self.perform_request(http.request.get(request), port=self.balancer.config.admin_port)


MANAGERS = [ManagerFixture('balancer', 'balancer_manager')]
CONTEXTS = [BalancerContext]

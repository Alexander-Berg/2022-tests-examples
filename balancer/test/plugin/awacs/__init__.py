# -*- coding: utf-8 -*-
import pytest
import shutil
import socket

from balancer.test.util import awacs
from balancer.test.util import settings as mod_settings
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture
from balancer.test.util.stream.ssl.stream import SSLClientOptions
from balancer.test.util.proto.http.stream import HTTPReaderException


pytest_plugins = [
    'balancer.test.plugin.awacs_config',
]


@pytest.fixture(scope='function')
def __backends_json(test_tools, awacs_config_path_bundle, port_manager):
    backends_json_path = test_tools.get_tool(mod_settings.Tool(
        yatest_option_name='backends_json',
    ))
    if backends_json_path is None:
        assert awacs_config_path_bundle is not None, 'backends.json not found'
        backends_json_path = awacs_config_path_bundle.abs_path('backends.json')
    return awacs.BackendsJson(backends_json_path, port_manager)


@pytest.fixture(scope='function')
def __awacs_manager_obj(awacs_config_path_bundle, __backends_json, awacs_config_path, function_fs_manager):
    return awacs.AwacsManager(awacs_config_path_bundle, __backends_json, awacs_config_path, function_fs_manager)


@multiscope.fixture(pytest_fixtures=['__awacs_manager_obj'])
def awacs_manager(__awacs_manager_obj):
    return __awacs_manager_obj


class AwacsContext(object):
    def start_awacs_backend(self, name, config, exclude=None):
        if isinstance(name, str):
            name = [name]
        return self.manager.backend.start(
            config,
            port=list(self.manager.awacs.backends_json.get_ports(name, exclude))
        )

    def _do_perform_awacs_request(self, addr, request, ssl=None, xfail=False):
        port = self.balancer.config.find_port(addr)
        if not ssl:
            if xfail:
                return self.perform_request_xfail(request, port)
            else:
                return self.perform_request(request, port)
        else:
            try:
                conn = self.manager.connection.http.create_pyssl(
                    host='localhost',
                    port=port,
                    ssl_options=SSLClientOptions(),
                )
                return conn.perform_request(request)
            except (socket.error, HTTPReaderException):
                if not xfail:
                    raise
            else:
                if xfail:
                    assert False, 'request performed successfully'

    def perform_awacs_request(self, addr, request, ssl=None):
        return self._do_perform_awacs_request(addr, request, ssl=ssl)

    def perform_awacs_request_xfail(self, addr, request, ssl=None):
        return self._do_perform_awacs_request(addr, request, ssl=ssl, xfail=True)


@pytest.fixture(scope='function')
def __certs_dirs(function_ctx):
    private_cert_dir = function_ctx.manager.fs.create_dir('private_cert_dir')
    public_cert_dir = function_ctx.manager.fs.create_dir('public_cert_dir')

    key_file = function_ctx.certs.abs_path('default.key')
    cert_file = function_ctx.certs.abs_path('default.crt')
    # ocsp_file = function_ctx.certs.abs_path('default_ocsp.0.der')

    if hasattr(function_ctx.options, 'ssl_sni_contexts'):
        for ssl_sni_ctx in function_ctx.options.ssl_sni_contexts:
            shutil.copyfile(key_file, '{}/{}.pem'.format(private_cert_dir, ssl_sni_ctx))
            shutil.copyfile(cert_file, '{}/allCAs-{}.pem'.format(public_cert_dir, ssl_sni_ctx))
            # TODO: support ocsp

    return private_cert_dir, public_cert_dir


@pytest.fixture(scope='function')
def __custom_params(function_ctx):
    result = dict()
    if hasattr(function_ctx.options, 'params'):
        for name, value in function_ctx.options.params.iteritems():
            if issubclass(value, awacs.special_values.SpecialValue):
                value = value.get_value(function_ctx)
            result[name] = value
    return result


MANAGERS = [ManagerFixture('awacs', 'awacs_manager')]
CONTEXTS = [AwacsContext]

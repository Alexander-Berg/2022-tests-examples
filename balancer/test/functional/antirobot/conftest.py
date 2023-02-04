# -*- coding: utf-8 -*-
import pytest
import re

import configs

import balancer.test.plugin.context as mod_ctx
from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig


class BaseAntirobotContext(object):
    ANTIROBOT_DATA = 'captcha'

    def start_antirobot_backend(self, config):
        return self.start_backend(config, name='antirobot_backend')

    def start_additional_backends(self):
        pass

    def start_antirobot_balancer(self, **balancer_kwargs):
        raise NotImplementedError()

    def check_accesslog(self, decision):
        for run in Multirun():
            with run:
                accesslog = self.manager.fs.read_file(self.balancer.config.accesslog)
                result = re.match(r'.*(\[sub_antirobot *\[[^\[\]]*\] %s)' % decision, accesslog)
                assert result is not None

    def do_robot_request(self, request):
        response = self.perform_request(request)
        self.check_accesslog('robot')
        asserts.content(response, self.ANTIROBOT_DATA)
        return response

    def do_not_robot_request(self, request):
        response = self.perform_request(request)
        self.check_accesslog('not_robot')
        return response

    def start_all(self, antirobot_headers=None, **balancer_kwargs):
        self.start_antirobot_backend(SimpleConfig(
            http.response.ok(headers=antirobot_headers, data=self.ANTIROBOT_DATA)))
        self.start_additional_backends()
        return self.start_antirobot_balancer(**balancer_kwargs)


class AntirobotContext(BaseAntirobotContext):
    BACKEND_DATA = 'OK'

    def start_submodule_backend(self, config):
        return self.start_backend(config)

    def start_additional_backends(self):
        return self.start_submodule_backend(SimpleConfig(http.response.ok(data=self.BACKEND_DATA)))

    def do_robot_request(self, request):
        response = super(AntirobotContext, self).do_robot_request(request)
        assert self.backend.state.requests.empty()
        return response

    def do_not_robot_request(self, request):
        response = super(AntirobotContext, self).do_not_robot_request(request)
        asserts.content(response, self.BACKEND_DATA)
        return response

    def start_antirobot_balancer(self, **balancer_kwargs):
        return self.start_balancer(configs.AntirobotTimeoutConfig(
            self.backend.server_config.port,
            self.antirobot_backend.server_config.port,
            **balancer_kwargs))


ar_ctx = mod_ctx.create_fixture(AntirobotContext)


class AntirobotWrapperContext(BaseAntirobotContext):
    def start_antirobot_balancer(self, **balancer_kwargs):
        return self.start_balancer(configs.AntirobotWrapperConfig(
            self.antirobot_backend.server_config.port,
            **balancer_kwargs))


wrap_ctx = mod_ctx.create_fixture(AntirobotWrapperContext)


@pytest.fixture(
    params=[
        'antirobot',
        'antirobot_wrapper',
    ],
)
def common_ctx(request, ar_ctx, wrap_ctx):
    if request.param == 'antirobot':
        return ar_ctx
    else:
        return wrap_ctx

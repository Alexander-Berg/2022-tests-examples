# -*- coding: utf-8 -*-
import time
import requests

import configs

from balancer.test.util.proto.handler.server import State
from balancer.test.util.proto.handler.server.http import StaticResponseHandler, StaticResponseConfig
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.balancer import asserts


TEST_URL = '/test.html'


class OneShotPingHandler(StaticResponseHandler):

    def handle_parsed_request(self, raw_request, stream):
        if raw_request.request_line.path == TEST_URL:
            stream.write_response(self.config.response)
            self.finish_response()
            return

        if raw_request.request_line.path == '/change_state':
            self.state.change_state()
            stream.write_response(self.config.response)
            self.finish_response()
            return

        if self.state.working:
            stream.write_response(self.config.response)
            self.finish_response()
        else:
            self.force_close()


class ActiveHandler(StaticResponseHandler):

    def handle_parsed_request(self, raw_request, stream):
        if raw_request.request_line.path == '/change_state':
            self.state.change_state()
            stream.write_response(self.config.response)
            self.finish_response()
            return

        if self.state.working:
            stream.write_response(self.config.response)
            self.finish_response()
        else:
            self.force_close()


class ActiveState(State):

    def __init__(self, config):
        super(ActiveState, self).__init__(config)
        self.__working = True

    def change_state(self):
        self.__working = not self.__working

    @property
    def working(self):
        return self.__working


class OneShotState(State):
    def __init__(self, config):
        super(OneShotState, self).__init__(config)
        self.__working = True

    def change_state(self):
        self.__working = not self.__working

    @property
    def working(self):
        if self.__working:
            return self.__working
        self.__working = True
        return False


class ActiveConfig(StaticResponseConfig):
    HANDLER_TYPE = ActiveHandler
    STATE_TYPE = ActiveState

    def __init__(self):
        super(ActiveConfig, self).__init__(response=http.response.ok())


class OneShotPingConfig(StaticResponseConfig):
    HANDLER_TYPE = OneShotPingHandler
    STATE_TYPE = OneShotState

    def __init__(self):
        super(OneShotPingConfig, self).__init__(response=http.response.ok())


def test_quorum_stability(ctx):
    '''
        Test that if one backend failed request, quorum is not loosing
    '''
    workers_count, worker_start_delay, ping_delay = 1, 0.01, 0.2
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(ActiveConfig(), name='backend2')
    ctx.start_backend(OneShotPingConfig(), name='backend3')
    ctx.start_balancer(configs.ActiveThreeBackends(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        backend_port3=ctx.backend3.server_config.port,
        request="GET /test.html HTTP/1.1\r\n\r\n",
        delay=ping_delay,
        attempts=2,
        quorum=2, hysteresis=0.5,
        workers=workers_count
    ))

    # Wait until all workers are on
    time.sleep(2 * ping_delay + workers_count * worker_start_delay)

    # Disable 2'd backend, wait until all workers knows about it
    requests.get('http://localhost:%s/change_state' % ctx.backend2.server_config.port)
    time.sleep(2 * ping_delay)

    # Disable 3'd backend for one request
    requests.get('http://localhost:%s/change_state' % ctx.backend3.server_config.port)
    time.sleep(2 * ping_delay)

    for i in xrange(20):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)

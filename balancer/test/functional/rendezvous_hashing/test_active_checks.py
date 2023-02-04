
# -*- coding: utf-8 -*-
import md5
import time

import balancer.test.plugin.context as mod_ctx

from configs import RendezvousHashingActiveConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http


class RendezvousHashingActiveCtx(object):
    def __init__(self):
        super(RendezvousHashingActiveCtx, self).__init__()

    def start_backends(self):
        self.start_backend(SimpleConfig(), name='backend1')
        self.start_backend(SimpleConfig(), name='backend2')
        self.start_backend(SimpleConfig(), name='backend3')

    def start_balancer_and_backends(self, **balancer_kwargs):
        self.start_backends()
        return self.start_balancer(RendezvousHashingActiveConfig(
            **balancer_kwargs))


active_ch_ctx = mod_ctx.create_fixture(RendezvousHashingActiveCtx)


def send_requests(connetion, request_count):
    for i in range(0, request_count):
        m = md5.new(str(i))
        id = m.hexdigest()
        request = http.request.get(path='/i?id=%sn=21' % id)
        resp = connetion.perform_request(request)
        asserts.status(resp, 200)


def test_active_checks(active_ch_ctx):
    """
    Проверяем, что если бэкенд не отвечает, то идём в следующий
    """
    active_ch_ctx.start_balancer_and_backends()
    active_ch_ctx.backend3.stop()
    time.sleep(3)  # wait untill host is pinged

    request_count = 10

    with active_ch_ctx.create_http_connection() as conn:
        send_requests(conn, request_count)
        assert active_ch_ctx.backend1.state.requests.qsize() + active_ch_ctx.backend2.state.requests.qsize() >= request_count

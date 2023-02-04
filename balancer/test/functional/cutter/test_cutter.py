# -*- coding: utf-8 -*-
import time
import datetime
import string

import balancer.test.plugin.context as mod_ctx

from configs import CutterConfig, CutterSSLConfig

from balancer.test.util.stream.ssl.stream import SSLClientOptions
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util import asserts
from balancer.test.util.predef import http


class CutContext(object):
    def __init__(self):
        super(CutContext, self).__init__()
        self.__ssl = self.request.param

    def start_cut_balancer(self, **balancer_kwargs):
        if self.__ssl:
            config = CutterSSLConfig(backend_port=self.backend.server_config.port,
                                     cert_dir=self.certs.root_dir,
                                     **balancer_kwargs)
        else:
            config = CutterConfig(backend_port=self.backend.server_config.port, **balancer_kwargs)
        return self.start_balancer(config)

    def start_all(self, **balancer_kwargs):
        self.start_backend(SimpleConfig())
        self.start_cut_balancer(**balancer_kwargs)
        return self.balancer

    def create_conn(self):
        if self.__ssl:
            return self.manager.connection.http.create_ssl(
                self.balancer.config.port,
                SSLClientOptions(ca_file=self.certs.root_ca, quiet=True)
            )
        else:
            return self.create_http_connection()


cut_ctx = mod_ctx.create_fixture(CutContext, params=[False, True], ids=['nossl', 'ssl'])


def test_complete_request_passes(cut_ctx):
    """
    BALANCER-483
    Full request passes through cutter immediately
    """
    cut_ctx.start_all(cutter_bytes=1024, cutter_timeout='10s')
    response = cut_ctx.create_conn().perform_request(http.request.post(data='data'))
    asserts.status(response, 200)


def test_time_pass_no_write(cut_ctx):
    """
    BALANCER-483
    Incomplete request (0 bytes) passes through cutter after timeout seconds
    """
    timeout = 2.0
    cut_ctx.start_all(cutter_bytes=1024, cutter_timeout='%fs' % timeout)

    req = http.request.post(headers={'Expect': '100-continue'}, data='data').to_raw_request()

    starttime = datetime.datetime.today()
    with cut_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request_line(req.request_line)
        stream.write_headers(req.headers)
        time.sleep(timeout)
        stream.write_data(req.data)
        resp = stream.read_response()
        asserts.status(resp, 200)
        accepttime = cut_ctx.backend.state.requests.get().start_time
        timediff = accepttime - starttime
        assert timediff > datetime.timedelta(seconds=timeout)


def test_time_pass_no_write_keepalive_session(cut_ctx):
    """
    BALANCER-483
    Incomplete request (0 bytes) passes through cutter after timeout seconds.
    Keepalive session works as expected
    """
    timeout = 2.0
    cut_ctx.start_all(cutter_bytes=1024, cutter_timeout='%fs' % timeout)

    req = http.request.post(headers={'Expect': '100-continue'}, data='data').to_raw_request()

    starttime = datetime.datetime.today()
    with cut_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request_line(req.request_line)
        stream.write_headers(req.headers)
        time.sleep(timeout)
        stream.write_data(req.data)
        resp = stream.read_response()
        asserts.status(resp, 200)
        accepttime = cut_ctx.backend.state.requests.get().start_time
        timediff = accepttime - starttime
        assert timediff > datetime.timedelta(seconds=timeout)

        stream.write_request_line(req.request_line)
        stream.write_headers(req.headers)
        time.sleep(timeout)
        stream.write_data(req.data)
        resp = stream.read_response()
        asserts.status(resp, 200)
        backend_request = cut_ctx.backend.state.requests.get()
        assert backend_request is not None


def test_time_pass_partial_write(cut_ctx):
    """
    BALANCER-483
    BALANCER-549
    Incomplete request (some bytes < bytes) passes through cutter
    after timeout seconds
    """
    timeout = 2.0
    cut_ctx.start_all(cutter_bytes=1024, cutter_timeout='%fs' % timeout)

    req = http.request.post(headers={'Expect': '100-continue'}, data=['data']).to_raw_request()

    starttime = datetime.datetime.today()
    with cut_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request_line(req.request_line)
        stream.write_headers(req.headers)

        chunks = req.data.chunks
        assert len(chunks) == 2
        stream.write_chunk(chunks[0])
        time.sleep(timeout)
        stream.write_chunk(chunks[1])

        resp = stream.read_response()
        asserts.status(resp, 200)
        accepttime = cut_ctx.backend.state.requests.get().start_time
        timediff = accepttime - starttime
        assert timediff > datetime.timedelta(seconds=timeout)


def test_bytes_pass(cut_ctx):
    """
    BALANCER-483
    Incomplete request with "bytes" bytes received passes through cutter
    faster than timeout reached
    """
    timeout = 2.0
    cut_ctx.start_all(cutter_bytes=4, cutter_timeout='%fs' % timeout)

    req = http.request.post(headers={'Expect': '100-continue'}, data=['data']).to_raw_request()

    starttime = datetime.datetime.today()
    with cut_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request_line(req.request_line)
        stream.write_headers(req.headers)

        chunks = req.data.chunks
        assert len(chunks) == 2
        stream.write_chunk(chunks[0])
        time.sleep(timeout)
        stream.write_chunk(chunks[1])

        resp = stream.read_response()
        asserts.status(resp, 200)
        accepttime = cut_ctx.backend.state.requests.get().start_time
        timediff = accepttime - starttime
        assert timediff < datetime.timedelta(seconds=timeout)


def test_bytes_body_reassembles(cut_ctx):
    """
    BALANCER-531
    If fast client sends more than 'bytes' bytes of body, then
    balancer should pass request to backend and the body should be
    reassembled as is.
    """
    timeout = 2.0
    cutter_bytes = 4
    request_data = string.ascii_lowercase
    assert cutter_bytes < len(request_data)
    cut_ctx.start_all(cutter_bytes=4, cutter_timeout='%fs' % timeout)

    req = http.request.post(data=request_data)

    resp = cut_ctx.create_conn().perform_request(req)
    asserts.status(resp, 200)

    req = cut_ctx.backend.state.get_request()
    asserts.content(req, request_data)


def test_cutter_fail_counter(cut_ctx):
    """
    BALANCER-2814
    Cutter should correctly maintain error counters.
    """
    cut_ctx.start_all(cutter_bytes=1024, cutter_timeout='10s')

    req = http.request.post(data=['a'] * 2).to_raw_request()
    with cut_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request_line(req.request_line)
        stream.write_headers(req.headers)
        stream.write_chunk('a')
        time.sleep(1)

    time.sleep(1)
    unistat = cut_ctx.get_unistat()
    assert unistat['report-total-succ_summ'] == 0
    assert unistat['report-total-fail_summ'] == 1
    assert unistat['report-total-client_fail_summ'] == 1
    assert unistat['report-total-other_fail_summ'] == 0

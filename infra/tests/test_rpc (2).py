# coding: utf-8
from __future__ import print_function

import abc
import socket
import random
import zlib

import tornado.web
import tornado.httpserver

import mock
import pytest

from infra.netmon.agent.idl import common_pb2 as common
from infra.netmon.agent.idl import api_pb2 as api

from agent import application
from agent import settings


class ProtoHandler(tornado.web.RequestHandler):

    __metaclass__ = abc.ABCMeta

    @abc.abstractproperty
    def Request(self):
        raise NotImplementedError()

    @abc.abstractproperty
    def Response(self):
        raise NotImplementedError()

    def _do_work(self, request, response):
        pass

    def post(self):
        request = self.Request()
        response = self.Response()
        request.ParseFromString(zlib.decompress(self.request.body))
        self._do_work(request, response)
        self.write(response.SerializeToString())
        self.finish()


class ExpandGroupsHandler(ProtoHandler):

    Request = api.TExpandGroupRequest
    Response = api.TExpandGroupResponse

    def _do_work(self, request, response):
        if request.Expression == "NOT_READY":
            response.Group.Ready = False
        elif request.Expression == "READY":
            hosts = [str(i) for i in xrange(100)]
            # always return random result to check that cache works properly
            random.shuffle(hosts)
            response.Group.Ready = True
            response.Group.Hosts.extend(hosts)
        else:
            raise RuntimeError()


class SendReportsHandler(ProtoHandler):

    Request = api.TSendReportsRequest
    Response = api.TSendReportsResponse

    def _do_work(self, request, response):
        assert len(request.Reports) == int(self.request.headers['X-Netmon-Reports-Count'])
        response.Accepted = len(request.Reports)


@pytest.yield_fixture(scope="module", autouse=True)
def http_server():
    app = tornado.web.Application([
        (r"/api/client/v1/expand_groups", ExpandGroupsHandler),
        (r"/api/client/v1/send_reports", SendReportsHandler),
    ], compress_response=True)
    server = tornado.httpserver.HTTPServer(app)

    sock = socket.socket(socket.AF_INET6)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.setsockopt(socket.IPPROTO_IPV6, socket.IPV6_V6ONLY, 1)
    sock.setblocking(0)
    sock.bind(("::", 0))
    sock.listen(128)
    server.add_sockets([sock])

    current_settings = settings.current()
    old_url = current_settings.netmon_url
    current_settings.netmon_url = "http://[::1]:{0}".format(sock.getsockname()[1])
    try:
        yield
    finally:
        current_settings.netmon_url = old_url
        server.stop()
        sock.close()


@pytest.yield_fixture()
def backend_maintainer_mock(agent_app):
    attrs = {
        'get_noc_sla_backends.return_value': ['url1', 'url2', 'url3'],
        'get_noc_sla_master.return_value': 'url1'
    }
    mocked = mock.MagicMock(**attrs)
    with mock.patch.dict(agent_app._services, {application.BackendMaintainerService: mocked}):
        yield mocked


def test_expand_group(agent_app, rpc_client):
    with pytest.raises(Exception):
        agent_app.run_sync(lambda: rpc_client.expand_group("NOT_READY"))

    hosts = agent_app.run_sync(lambda: rpc_client.expand_group("READY"))
    assert len(hosts) == 100

    cached_hosts = agent_app.run_sync(lambda: rpc_client.expand_group("READY"))
    assert hosts == cached_hosts


def test_send_report(agent_app, rpc_client):
    accepted = agent_app.run_sync(lambda: rpc_client.send_reports([common.TProbeReport()], settings.current().netmon_url))
    assert accepted == 1


def test_scheduled_probes_exception(agent_app, rpc_client, backend_maintainer_mock):
    request_targets = []
    bad_url = 'url2'

    def make_request_mock(self, *args, **kwargs):
        url = kwargs['server_url']
        request_targets.append(url)
        if url == bad_url:
            raise tornado.httpclient.HTTPError(599)
        return tornado.gen.maybe_future(None)

    with mock.patch.object(rpc_client, '_make_request', make_request_mock):
        agent_app.run_sync(lambda: rpc_client.scheduled_probes(common.NOC_SLA_PROBE))
        assert sorted(request_targets) == ['url1', 'url2', 'url3']

        request_targets = []
        bad_url = 'url1'
        with pytest.raises(RuntimeError):
            agent_app.run_sync(lambda: rpc_client.scheduled_probes(common.NOC_SLA_PROBE))
        assert sorted(request_targets) == ['url1', 'url2', 'url3']

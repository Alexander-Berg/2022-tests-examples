# coding: utf-8
from __future__ import print_function

import tornado.gen
import tornado.httpclient

import mock
import pytest

from infra.netmon.agent.idl import common_pb2 as common

from agent import application
from agent import rpc
from agent import sender
from agent import settings
from agent import utils


@pytest.yield_fixture(scope="module", autouse=True)
def check_interval_mock():
    # check_interval should be nonzero, so just set it to some small value
    with mock.patch.object(settings.current(), 'check_interval', (0.001, 0.001)) as mocked:
        yield mocked


@pytest.yield_fixture(autouse=True)
def register_backend_maintainer(agent_app):
    agent_app.register(application.BackendMaintainerService())


@pytest.yield_fixture()
def send_reports_mock():
    with mock.patch.object(rpc.RpcClient, "send_reports", return_value=tornado.gen.maybe_future(None)) as mocked:
        yield mocked


@pytest.yield_fixture()
def noc_sla_urls_mock(agent_app, register_backend_maintainer):
    noc_sla_urls = (
        'http://test-netmon-aggregator-1',
        'http://test-netmon-aggregator-2',
        'http://test-netmon-aggregator-3'
    )
    backend_maintainer_mock = mock.MagicMock()
    backend_maintainer_mock.get_noc_sla_backends.return_value = noc_sla_urls
    backend_maintainer_mock.get_noc_sla_master.return_value = noc_sla_urls[0]

    with mock.patch.dict(agent_app._services, {application.BackendMaintainerService: backend_maintainer_mock}):
        with mock.patch.object(settings.current(), 'noc_sla_urls', noc_sla_urls) as urls_mock:
            yield urls_mock


def test_sending_something(agent_app, rpc_client, send_reports_mock):
    service = sender.SenderService()
    agent_app.register(service)

    now = utils.timestamp()
    service.enqueue([common.TProbeReport(Generated=now)])
    agent_app.run_sync(service._loop.wait)

    send_reports_mock.assert_called_with([common.TProbeReport(Generated=now)], settings.current().netmon_url)


def test_sending_nothing(agent_app, rpc_client, send_reports_mock):
    service = sender.SenderService()
    agent_app.register(service)

    agent_app.run_sync(service._loop.wait)

    assert send_reports_mock.call_count == 0


def test_sending_limit(agent_app, rpc_client):
    def send_reports_limited_mock(report_list, server_url):
        if len(report_list) > 3:
            raise tornado.httpclient.HTTPError(429)
        else:
            return tornado.gen.maybe_future(None)

    with mock.patch.object(rpc.RpcClient, "send_reports", side_effect=send_reports_limited_mock) as mocked:
        ttl = utils.timestamp() + 3600 * utils.US
        service = sender.SenderService(report_ttl_sec=ttl)
        agent_app.register(service)

        for x in xrange(1, 6):
            service.enqueue([common.TProbeReport(Generated=x)])

        # Run at least 3 iterations, each run_sync runs one or more
        agent_app.run_sync(service._loop.wait)
        agent_app.run_sync(service._loop.wait)
        agent_app.run_sync(service._loop.wait)

        calls = [
            mock.call([common.TProbeReport(Generated=x) for x in xrange(5, 0, -1)], settings.current().netmon_url),
            # Received HTTP 429, reduce request limit
            mock.call([common.TProbeReport(Generated=x) for x in xrange(5, 2, -1)], settings.current().netmon_url),
            # Received HTTP 200, send the rest
            mock.call([common.TProbeReport(Generated=x) for x in xrange(2, 0, -1)], settings.current().netmon_url),
        ]

        mocked.assert_has_calls([calls[0]])
        # Because of logging, calls to __str__() go between calls[0] and calls[1].
        # Don't check those.
        mocked.assert_has_calls([calls[1], calls[2]])


def test_sending_queue_ttl(agent_app, rpc_client, send_reports_mock):
    ttl = 300
    service = sender.SenderService(report_ttl_sec=ttl)
    agent_app.register(service)

    cutoff_time = utils.timestamp() - ttl * utils.US
    service.enqueue([common.TProbeReport(Generated=cutoff_time - 100 * utils.US)])

    agent_app.run_sync(service._loop.wait)
    assert send_reports_mock.call_count == 0

    cutoff_time = utils.timestamp() - ttl * utils.US
    service.enqueue([common.TProbeReport(Generated=cutoff_time - 100 * utils.US)])
    service.enqueue([common.TProbeReport(Generated=cutoff_time + 100 * utils.US)])

    agent_app.run_sync(service._loop.wait)
    send_reports_mock.assert_called_with([common.TProbeReport(Generated=cutoff_time + 100 * utils.US)], settings.current().netmon_url)


def test_report_mirroring_success(agent_app, rpc_client, send_reports_mock, noc_sla_urls_mock):
    service = sender.SenderService()
    agent_app.register(service)

    now = utils.timestamp()
    service.enqueue([common.TProbeReport(Generated=now, Type=common.NOC_SLA_PROBE)])
    agent_app.run_sync(service._loop.wait)

    calls = [
        mock.call([common.TProbeReport(Generated=now, Type=common.NOC_SLA_PROBE)], url)
        for url in settings.current().noc_sla_urls
    ]
    send_reports_mock.assert_has_calls(calls, any_order=True)


def test_report_mirroring_partial_fail(agent_app, rpc_client, noc_sla_urls_mock):
    # sanity check
    assert len(settings.current().noc_sla_urls) > 1

    effects = [tornado.gen.maybe_future(None) for _ in settings.current().noc_sla_urls]
    effects[0] = tornado.httpclient.HTTPError(503)
    with mock.patch.object(rpc.RpcClient, "send_reports", side_effect=effects) as mocked:
        service = sender.SenderService()
        agent_app.register(service)

        now = utils.timestamp()
        service.enqueue([common.TProbeReport(Generated=now, Type=common.NOC_SLA_PROBE)])
        agent_app.run_sync(service._loop.wait)

        calls = [
            mock.call([common.TProbeReport(Generated=now, Type=common.NOC_SLA_PROBE)], url)
            for url in settings.current().noc_sla_urls
        ]
        mocked.assert_has_calls(calls, any_order=True)

        mocked.reset_mock()
        agent_app.run_sync(service._loop.wait)

        # report was sent (to some backends) successfully, so we don't resend it
        assert mocked.call_count == 0


def test_report_mirroring_fail(agent_app, rpc_client, noc_sla_urls_mock):
    with mock.patch.object(rpc.RpcClient, "send_reports", side_effect=tornado.httpclient.HTTPError(429)) as mocked:
        service = sender.SenderService()
        agent_app.register(service)

        now = utils.timestamp()
        service.enqueue([common.TProbeReport(Generated=now, Type=common.NOC_SLA_PROBE)])
        agent_app.run_sync(service._loop.wait)

        calls = [
            mock.call([common.TProbeReport(Generated=now, Type=common.NOC_SLA_PROBE)], url)
            for url in settings.current().noc_sla_urls
        ]
        mocked.assert_has_calls(calls, any_order=True)

        mocked.reset_mock()
        agent_app.run_sync(service._loop.wait)

        mocked.assert_has_calls(calls, any_order=True)

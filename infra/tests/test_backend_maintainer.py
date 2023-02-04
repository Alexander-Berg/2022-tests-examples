# coding: utf-8
from __future__ import print_function

import mock
import socket

import tornado.concurrent
import tornado.gen
import tornado.httpclient
import tornado.testing

from agent import utils
from agent import backend_maintainer
from agent import settings

from infra.yp_service_discovery.api import api_pb2 as sd_api


def create_sd_response(timestamp, addresses, resolve_status=sd_api.EResolveStatus.OK):
    # simulate random order of endpoints in SD responses
    shuffle = lambda gen: list(reversed(list(gen)))

    return sd_api.TRspResolveEndpoints(
        timestamp=timestamp,
        endpoint_set=sd_api.TEndpointSet(
            endpoint_set_id='test-endpoint-set-id',
            endpoints=shuffle(
                sd_api.TEndpoint(
                    id=str(i),
                    protocol='TCP',
                    fqdn='test-netmon-aggregator-{}'.format(i),
                    ip6_address=address
                )
                for i, address in enumerate(addresses)
            )
        ),
        resolve_status=resolve_status,
        host='',
        ruid=''
    )


def make_request_closure(responses):
    it = iter(responses)

    @tornado.gen.coroutine
    def make_request(request, response, url):
        response.CopyFrom(it.next())
        raise tornado.gen.Return(None)

    return make_request


@tornado.gen.coroutine
def resolve_mock(host):
    address_dict = {
        'test-netmon-aggregator-1': [(socket.AF_INET6, '::1')],
        'test-netmon-aggregator-2': [(socket.AF_INET6, '::2')],
        'test-netmon-aggregator-3': [(socket.AF_INET6, '::3')],
    }
    raise tornado.gen.Return(address_dict[host])


@tornado.gen.coroutine
def failed_resolve_mock(host):
    address_dict = {
        'test-netmon-aggregator-1': [(socket.AF_INET6, '::1')],
        'test-netmon-aggregator-2': [(socket.AF_INET6, '::2')],
        'test-netmon-aggregator-3': [],
    }
    raise tornado.gen.Return(address_dict[host])


class TestBackendMaintainer(tornado.testing.AsyncTestCase):

    def setUp(self):
        super(TestBackendMaintainer, self).setUp()

        current = settings.current()
        current.yp_sd_url = 'http://localhost/'
        current.yp_sd_cluster_name = 'sas'
        current.yp_sd_endpoint_set_id = 'test-endpoint-set-id'
        current.noc_sla_urls = (
            'http://test-netmon-aggregator-1',
            'http://test-netmon-aggregator-3',
            'http://test-netmon-aggregator-2'
        )
        # get_backends() should return ips in lexicographic order of corresponding hostnames
        # i.e. [<ip for aggregator-1>, <ip for aggregator-2>, <ip for aggregator-3>]

        self.dns_resolver_mock = mock.Mock()
        self.dns_resolver_mock.try_resolve.side_effect = resolve_mock

        self.maintainer = backend_maintainer.NocSlaBackendMaintainer(True)

    @classmethod
    def teardown_class(cls):
        current = settings.current()
        current.yp_sd_url = None
        current.yp_sd_cluster_name = None
        current.yp_sd_endpoint_set_id = None
        current.noc_sla_urls = ()

    @tornado.testing.gen_test
    def test_sd_success(self):
        responses = [
            create_sd_response(1, ['::dead:beef']),
            create_sd_response(2, ['::dead:beef', '::cafe:babe']),
            create_sd_response(3, ['::cafe:babe']),
        ]
        with mock.patch.object(utils, 'make_protobuf_request', side_effect=make_request_closure(responses)):
            yield self.maintainer.update_backends(self.dns_resolver_mock)
            assert self.maintainer.get_backends() == (
                'http://[::dead:beef]',
            )
            assert self.dns_resolver_mock.resolve.call_count == 0
            yield self.maintainer.update_backends(self.dns_resolver_mock)
            assert self.maintainer.get_backends() == (
                'http://[::dead:beef]', 'http://[::cafe:babe]'
            )
            assert self.dns_resolver_mock.resolve.call_count == 0
            yield self.maintainer.update_backends(self.dns_resolver_mock)
            assert self.maintainer.get_backends() == (
                'http://[::cafe:babe]',
            )
            assert self.dns_resolver_mock.resolve.call_count == 0

    @tornado.testing.gen_test
    def test_sd_stale_response(self):
        # second response should be ignored due to older timestamp
        responses = [
            create_sd_response(2, ['::dead:beef']),
            create_sd_response(1, ['::cafe:babe'])
        ]
        with mock.patch.object(utils, 'make_protobuf_request', side_effect=make_request_closure(responses)):
            yield self.maintainer.update_backends(self.dns_resolver_mock)
            assert self.maintainer.get_backends() == ('http://[::dead:beef]',)
            yield self.maintainer.update_backends(self.dns_resolver_mock)
            assert self.maintainer.get_backends() == ('http://[::dead:beef]',)
            assert self.dns_resolver_mock.resolve.call_count == 0

    @tornado.testing.gen_test
    def test_sd_http_error(self):
        # SD fails, resolve via DNS
        with mock.patch.object(utils, 'make_protobuf_request',
                               side_effect=tornado.httpclient.HTTPError(503)):
            yield self.maintainer.update_backends(self.dns_resolver_mock)
            assert self.maintainer.get_backends() == ('http://[::1]', 'http://[::2]', 'http://[::3]')

    @mock.patch.object(settings.current(), 'noc_sla_fallback_urls', ('http://test-fallback-aggregator',))
    @tornado.testing.gen_test
    def test_sd_and_dns_failure_with_fallback(self):
        failing_resolver_mock = mock.Mock()
        failing_resolver_mock.try_resolve.side_effect = failed_resolve_mock
        with mock.patch.object(utils, 'make_protobuf_request',
                               side_effect=socket.error(111, 'Connection refused')):
            # initialize new maintainer with modified settings
            maintainer = backend_maintainer.NocSlaBackendMaintainer(True)

            yield maintainer.update_backends(failing_resolver_mock)
            assert maintainer.get_backends() == ('http://test-fallback-aggregator',)

    @tornado.testing.gen_test
    def test_sd_and_dns_failure_without_fallback(self):
        failing_resolver_mock = mock.Mock()
        failing_resolver_mock.try_resolve.side_effect = failed_resolve_mock
        with mock.patch.object(utils, 'make_protobuf_request',
                               side_effect=socket.error(111, 'Connection refused')):

            # backends successfully resolved via DNS
            yield self.maintainer.update_backends(self.dns_resolver_mock)
            assert self.maintainer.get_backends() == ('http://[::1]', 'http://[::2]', 'http://[::3]')

            # DNS fails, no fallback urls are specified, return last result
            yield self.maintainer.update_backends(failing_resolver_mock)
            assert self.maintainer.get_backends() == ('http://[::1]', 'http://[::2]', 'http://[::3]')

            # and keep returning it in subsequent calls
            yield self.maintainer.update_backends(failing_resolver_mock)
            assert self.maintainer.get_backends() == ('http://[::1]', 'http://[::2]', 'http://[::3]')

        # SD starts working, return new resolved urls
        responses = [
            create_sd_response(1, ['::dead:beef']),
        ]
        with mock.patch.object(utils, 'make_protobuf_request', side_effect=make_request_closure(responses)):
            yield self.maintainer.update_backends(self.dns_resolver_mock)
            assert self.maintainer.get_backends() == ('http://[::dead:beef]',)

    @mock.patch.object(settings.current(), 'noc_sla_urls', (
        'http://test-netmon-aggregator-1',
        'http://test-netmon-aggregator-3',
        'https://test-netmon-aggregator-2',
    ))
    @tornado.testing.gen_test
    def test_url_scheme_preservation(self):
        # initialize new maintainer with modified settings
        maintainer = backend_maintainer.NocSlaBackendMaintainer(True)
        responses = [
            create_sd_response(1, ['::dead:beef', '::cafe:babe', '::cafe:b0ba']),
        ]
        with mock.patch.object(utils, 'make_protobuf_request', side_effect=make_request_closure(responses)):
            yield maintainer.update_backends(self.dns_resolver_mock)
            assert maintainer.get_backends() == (
                'http://[::dead:beef]', 'https://[::cafe:babe]', 'http://[::cafe:b0ba]'
            )
            assert self.dns_resolver_mock.resolve.call_count == 0

        # SD fails, resolve via DNS
        with mock.patch.object(utils, 'make_protobuf_request',
                               side_effect=tornado.httpclient.HTTPError(503)):
            yield maintainer.update_backends(self.dns_resolver_mock)
            assert maintainer.get_backends() == ('http://[::1]', 'https://[::2]', 'http://[::3]')

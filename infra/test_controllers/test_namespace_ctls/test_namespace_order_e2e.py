import copy

import flaky
import inject
import mock
import pytest
from sepelib.core import config

from awacs import resolver
from awacs.lib import nannyclient, nannyrpcclient, ypliterpcclient, staffclient, ypclient, juggler_client
from awacs.lib.order_processor.runner import StateRunner
from awacs.model import staffcache
from awacs.model.backend import BackendCtlManager, BackendCtl
from awacs.model.balancer import generator
from awacs.model.balancer.ctl import BalancerCtl
from awacs.model.balancer.manager import BalancerCtlManager, BalancerOrderCtlManager
from awacs.model.balancer.order.order_ctl import BalancerOrderCtl
from awacs.model.balancer.order.processors import CreatingEndpointSet, CreatingUserEndpointSet
from awacs.model.balancer.order.util import make_awacs_balancer_id
from awacs.model.dns_records.ctl import DnsRecordCtl
from awacs.model.dns_records.manager import DnsRecordCtlManager
from awacs.model.namespace.order.ctl import NamespaceOrderCtl
from awacs.model.namespace.manager import NamespaceOrderCtlManager
from infra.awacs.proto import model_pb2, api_pb2, internals_pb2
from awacs.web import namespace_service, validation
from infra.swatlib import httpgridfsclient
from infra.swatlib.auth import abc
from awtest.api import call
from awtest import wait_until


NS_ID = 'awacs_namespace_wizard_e2e_test'

CONFIG = {
    'nanny': {
        'url': 'https://nanny.yandex-team.ru/',
        'token': 'AQAD-xxx',
    },
    'yp_lite': {
        'url': 'https://yp-lite-ui.nanny.yandex-team.ru/',
        'token': 'AQAD-xxx',
    },
    'staff': {
        'api_url': 'https://staff-api.yandex-team.ru/v3/',
        'oauth_token': 'AQAD-xxx',
        'verify_ssl': False,
    },
    'abc': {
        'api_url': 'https://abc-back.yandex-team.ru/',
        'oauth_token': 'AQAD-xxx',
    },
    'juggler_client': {
        'namespace_prefix': 'awacs.',
        'url': 'http://juggler-api.search.yandex.net',
        'token': 'AQAD-xxx',
    },
    'gridfs': {
        'url': 'http://dev-sepe-gridfs.yandex-team.ru/',
    },
    'yp': {
        'use_grpc': False,
        'oauth_token': 'AQAD-xxx',
        'clusters': [
            {
                'cluster': 'SAS',
                'rpc_url': 'https://sas.yp.yandex.net:8443/ObjectService',
            },
            {
                'cluster': 'MAN',
                'rpc_url': 'https://man.yp.yandex.net:8443/ObjectService',
            },
            {
                'cluster': 'VLA',
                'rpc_url': 'https://vla.yp.yandex.net:8443/ObjectService',
            },
        ]
    },
}


@pytest.fixture(autouse=True)
def deps(binder):
    config.set_value('nanny', CONFIG['nanny'])
    config.set_value('gridfs', CONFIG['gridfs'])

    def configure(b):
        cfg = copy.deepcopy(CONFIG)
        nanny_config = copy.deepcopy(cfg['nanny'])
        nanny_config['url'] = nanny_config['url'].rstrip('/') + '/v2'
        b.bind(nannyclient.INannyClient,
               nannyclient.NannyClient.from_config(nanny_config))
        b.bind(nannyrpcclient.INannyRpcClient,
               nannyrpcclient.NannyRpcClient.from_config(nanny_config))
        yp_lite_config = cfg['yp_lite']
        b.bind(ypliterpcclient.IYpLiteRpcClient,
               ypliterpcclient.YpLiteRpcClient.from_config(yp_lite_config))

        staff_cache = staffcache.StaffCache('/cache/staff/')
        staff_client = staffclient.StaffClient.from_config(cfg['staff'], cache=staff_cache)
        b.bind(staffclient.IStaffClient, staff_client)

        abc_client = abc.AbcClient.from_config(cfg['abc'])
        b.bind(abc.IAbcClient, abc_client)

        b.bind(httpgridfsclient.IHttpGridfsClient,
               httpgridfsclient.HttpGridfsClient.from_config(cfg['gridfs']))

        yp_client_factory = ypclient.YpObjectServiceClientFactory.from_config(cfg['yp'])
        b.bind(ypclient.IYpObjectServiceClientFactory, yp_client_factory)

        gencfg_cache = resolver.GencfgGroupInstancesCache(mem_maxsize=100)
        gencfg_client = resolver.GencfgClient.from_config({}, cache=gencfg_cache)
        b.bind(resolver.IGencfgGroupInstancesCache, gencfg_cache)
        b.bind(resolver.IGencfgClient, gencfg_client)

        b.bind(juggler_client.IJugglerClient,
               juggler_client.JugglerClient.from_config(cfg['juggler_client']))

        nanny_cache = resolver.NannyInstancesCache(mem_maxsize=100)
        nanny_client = resolver.NannyClient.from_config(
            nanny_config, gencfg_client, yp_client_factory, cache=nanny_cache)
        b.bind(resolver.INannyInstancesCache, nanny_cache)
        b.bind(resolver.INannyClient, nanny_client)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture(autouse=True)
def default_ns(deps, dao):
    dao.create_default_name_servers()


@pytest.fixture
def managers(deps, zk, cache):
    dns_manager = DnsRecordCtlManager(coord=zk, cache=cache)
    dns_manager.start()
    # TODO: vault_client refuses to work with VCR
    # cert_order_manager = CertOrderCtlManager(coord=zk, cache=cache)
    # cert_order_manager.start()
    balancer_manager = BalancerCtlManager(coord=zk, cache=cache)
    balancer_manager.start()
    balancer_order_manager = BalancerOrderCtlManager(coord=zk, cache=cache)
    balancer_order_manager.start()
    namespace_manager = NamespaceOrderCtlManager(coord=zk, member_id='testmemberid', party_suffix='p', cache=cache)
    namespace_manager.start()
    backend_manager = BackendCtlManager(coord=zk, cache=cache, member_id='test-e2e', party_suffix='')
    backend_manager.start()
    yield
    dns_manager.stop()
    balancer_manager.stop()
    balancer_order_manager.stop()
    namespace_manager.stop()
    backend_manager.stop()


@pytest.fixture(autouse=True)
def create_default_dns_zone(deps, dao):
    dao.create_default_name_servers()


@pytest.fixture
def namespace_order():
    meta_pb = model_pb2.NamespaceMeta()
    meta_pb.id = NS_ID
    meta_pb.category = 'awacs'
    meta_pb.abc_service_id = 1821  # rclb
    meta_pb.auth.type = meta_pb.auth.STAFF
    req_pb = api_pb2.CreateNamespaceRequest(meta=meta_pb)

    req_pb.order.flow_type = model_pb2.NamespaceOrder.Content.YP_LITE

    req_pb.order.yp_lite_allocation_request.network_macro = '_SEARCHSAND_'
    req_pb.order.yp_lite_allocation_request.locations.extend(['SAS', 'MAN', 'VLA'])
    req_pb.order.yp_lite_allocation_request.nanny_service_id_slug = 'not-used-yet'
    req_pb.order.yp_lite_allocation_request.type = model_pb2.NamespaceOrder.Content.YpLiteAllocationRequest.PRESET
    req_pb.order.yp_lite_allocation_request.preset.instances_count = 1
    req_pb.order.yp_lite_allocation_request.preset.type = \
        model_pb2.NamespaceOrder.Content.YpLiteAllocationRequest.Preset.MICRO

    req_pb.order.backends['test'].type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    req_pb.order.backends['test'].yp_endpoint_sets.add(endpoint_set_id='nanny-watchdog', cluster='sas')

    # TODO: vault_client refuses to work with VCR
    # req_pb.order.certificate_order_content.common_name = 'awacs-wizard-e2e-test.in.yandex-team.ru'
    # req_pb.order.certificate_order_content.ca_name = 'InternalTestCA'
    # req_pb.order.certificate_order_content.abc_service_id = 1821  # rclb

    req_pb.order.dns_record_request.type = model_pb2.NamespaceOrder.Content.DnsRecordRequest.DEFAULT
    req_pb.order.dns_record_request.default.zone = 'awacs-wizard-e2e-test'
    req_pb.order.dns_record_request.default.name_server.namespace_id = 'infra'
    req_pb.order.dns_record_request.default.name_server.id = 'in.yandex-team.ru'

    req_pb.order.alerting_simple_settings.notify_staff_group_id = 24836  # svc_rclb

    # TODO: VCR doesn't write episodes for them (???)
    with mock.patch.object(validation.namespace, 'validate_yp_acl'):
        call(namespace_service.create_namespace, req_pb, 'disafonov')


@pytest.mark.vcr
@flaky.flaky(max_runs=3, min_passes=1)
@mock.patch.object(BackendCtl, 'PROCESS_INTERVAL', 0.2)
@mock.patch.object(BackendCtl, 'EVENTS_QUEUE_GET_TIMEOUT', 0.2)
@mock.patch.object(BalancerCtl, 'PROCESS_INTERVAL', 0.2)
@mock.patch.object(BalancerCtl, 'EVENTS_QUEUE_GET_TIMEOUT', 0.2)
@mock.patch.object(DnsRecordCtl, 'PROCESS_INTERVAL', 0.2)
@mock.patch.object(DnsRecordCtl, 'EVENTS_QUEUE_GET_TIMEOUT', 0.2)
@mock.patch.object(NamespaceOrderCtl, 'EVENTS_QUEUE_GET_TIMEOUT', 0.2)
@mock.patch.object(NamespaceOrderCtl, 'PROCESSING_INTERVAL', 0.2)
@mock.patch.object(BalancerOrderCtl, 'EVENTS_QUEUE_GET_TIMEOUT', 0.2)
@mock.patch.object(BalancerOrderCtl, 'PROCESSING_INTERVAL', 0.2)
@mock.patch.object(StateRunner, 'processing_interval', 0.2)
@mock.patch.object(generator, 'resolve_yp_endpoint_set_pbs', return_value=[
    internals_pb2.Instance(  # TODO: returns empty instances list for awacs-created DNS backend "_sas_man_vla" (???)
        host='test.fqdn',
        port=80,
        ipv6_addr='2a02:6b8:0:3400:0:97f:0:1',
        weight=1
    )])
@mock.patch.object(CreatingEndpointSet, '_endpoint_set_exists', return_value=False)  # to avoid the need for new vcr ep
@mock.patch.object(CreatingUserEndpointSet, '_endpoint_set_exists', return_value=False)
def test_create_namespace_e2e(_1, _2, dao, zk_storage, cache, managers, namespace_order):
    """
    use "log" fixture for debugging
    """
    assert wait_until(lambda: cache.get_namespace(NS_ID).order.progress.state.id == 'FINISHED', timeout=60, interval=1)
    for location in ('SAS', 'MAN', 'VLA'):
        balancer_id = make_awacs_balancer_id(NS_ID, location)
        assert zk_storage.get_balancer(NS_ID, balancer_id).order.progress.state.id == 'FINISH'

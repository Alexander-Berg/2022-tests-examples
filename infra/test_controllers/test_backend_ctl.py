# coding: utf-8
import functools

import inject
import time
import logging
import mock
import pytest
import six

from sepelib.core import config as appconfig
from infra.swatlib import metrics
from infra.swatlib.auth import abc

from awacs.lib.rpc import exceptions
from awacs.lib import ypclient, nannyrpcclient, yp_service_discovery
from awacs.model.backend import BackendCtl
from awacs.model.balancer import generator
from infra.awacs.proto import model_pb2, internals_pb2
from awacs.resolver import yp, ResolvingError

from awtest.mocks.nanny_client import NannyMockClient
from awtest.mocks.nanny_rpc_client import NannyRpcMockClient
from awtest import check_log, wait_until
from awtest.api import Api


@pytest.fixture
def empty_namespace(binder_with_nanny_client):
    Api.create_namespace(NS_ID)


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, caplog):
    def configure(b):
        b.bind(abc.IAbcClient, mock.Mock())
        b.bind(nannyrpcclient.INannyRpcClient, NannyRpcMockClient(u'https://api/repo/'))
        b.bind(ypclient.IYpObjectServiceClientFactory,
               ypclient.YpObjectServiceClientFactory.from_config({
                   u'use_grpc': False,
                   u'oauth_token': u'AQAD-XXX',
                   u'clusters': [
                       {
                           u'cluster': u'SAS',
                           u'rpc_url': u'https://sas.yp.yandex.net:8443/ObjectService',
                       },
                   ],
               }))
        b.bind(yp_service_discovery.IResolver,
               yp_service_discovery.Resolver.from_config({
                   u'url': u'http://sd.yandex.net:8080/',
                   u'client_name': u'awacs-local',
               }))
        binder_with_nanny_client(b)

    caplog.set_level(logging.DEBUG)
    inject.clear_and_configure(configure)
    yield
    inject.clear()


NS_ID = 'namespace-id'
BACKEND_ID = 'backend-id'

SAS_STUB = object()
MAN_STUB = object()

YP_CLUSTERS = {
    'sas': SAS_STUB,
    'man': MAN_STUB,
}

SAS_TEST_ENDPOINT_SET_INSTANCES = [
    internals_pb2.Instance(host='two.yandex.net', port=80, ipv6_addr='7.8.8.8'),
    internals_pb2.Instance(host='one.yandex.net', port=80, ipv6_addr='8.8.8.8'),
    internals_pb2.Instance(host='three.yandex.net', port=80, ipv6_addr='9.8.8.8')  # duplicate,
]

MAN_TEST_ENDPOINT_SET_INSTANCES = [
    internals_pb2.Instance(host='four.yandex.net', port=70, ipv6_addr='1.8.8.8'),
    internals_pb2.Instance(host='three.yandex.net', port=80, ipv6_addr='9.8.8.8'),  # duplicate
]


def list_endpoint_set_instances(stub, endpoint_set_id):
    if stub is SAS_STUB and endpoint_set_id == 'test':
        return list(SAS_TEST_ENDPOINT_SET_INSTANCES)
    if stub is MAN_STUB and endpoint_set_id == 'test':
        return list(MAN_TEST_ENDPOINT_SET_INSTANCES)
    raise AssertionError()


@mock.patch.object(yp, 'does_endpoint_set_exist', return_value=True)
@mock.patch.object(ypclient.YpObjectServiceClientFactory, 'get', side_effect=YP_CLUSTERS.get)
@mock.patch.object(yp, 'list_endpoint_set_instances', side_effect=list_endpoint_set_instances)
def test_resolve(_1, _2, _3, ctx):
    ctl = BackendCtl('namespace-id', 'balancer-id')

    pb = model_pb2.BackendSelector()
    with pytest.raises(RuntimeError) as e:
        ctl._resolve(ctx, pb, {})
    e.match('unsupported backend selector type')

    pb.type = pb.YP_ENDPOINT_SETS
    with pytest.raises(ResolvingError) as e:
        ctl._resolve(ctx, pb, {})
    e.match('got an empty instances list')

    pb.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='test')
    pb.yp_endpoint_sets.add(cluster='man', endpoint_set_id='test')

    resolution = ctl._resolve(ctx, pb, {})
    instance_pbs = resolution.instance_pbs

    expected_instance_pbs = SAS_TEST_ENDPOINT_SET_INSTANCES + MAN_TEST_ENDPOINT_SET_INSTANCES
    assert (sorted(instance_pbs, key=functools.cmp_to_key(generator.instances_cmp)) ==
            sorted(expected_instance_pbs, key=functools.cmp_to_key(generator.instances_cmp)))


def list_endpoint_set_instances_with_empty_man(stub, endpoint_set_id):
    if stub is SAS_STUB and endpoint_set_id == 'test':
        return list(SAS_TEST_ENDPOINT_SET_INSTANCES)
    if stub is MAN_STUB and endpoint_set_id == 'test':
        return []
    raise AssertionError()


@mock.patch.object(yp, 'does_endpoint_set_exist', return_value=True)
@mock.patch.object(ypclient.YpObjectServiceClientFactory, 'get', side_effect=YP_CLUSTERS.get)
@mock.patch.object(yp, 'list_endpoint_set_instances', side_effect=list_endpoint_set_instances_with_empty_man)
def test_resolve_empty_endpoint_sets(_1, _2, _3, ctx):
    metrics_before = dict(metrics.ROOT_REGISTRY.items())

    ctl = BackendCtl('namespace-id', 'balancer-id')

    pb = model_pb2.BackendSelector()
    with pytest.raises(RuntimeError) as e:
        ctl._resolve(ctx, pb, {})
    e.match('unsupported backend selector type')

    pb.type = pb.YP_ENDPOINT_SETS
    with pytest.raises(ResolvingError) as e:
        ctl._resolve(ctx, pb, {})
    e.match('got an empty instances list')

    pb.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='test')
    pb.yp_endpoint_sets.add(cluster='man', endpoint_set_id='test')

    resolution = ctl._resolve(ctx, pb, {})
    instance_pbs = resolution.instance_pbs

    expected_instance_pbs = SAS_TEST_ENDPOINT_SET_INSTANCES
    assert (sorted(instance_pbs, key=functools.cmp_to_key(generator.instances_cmp)) ==
            sorted(expected_instance_pbs, key=functools.cmp_to_key(generator.instances_cmp)))

    appconfig.set_value('run.reject_empty_yp_endpoint_sets', True)
    try:
        with pytest.raises(ResolvingError) as e:
            ctl._resolve(ctx, pb, {})
        e.match('man:test resolved to an empty list of endpoints')
        pb.allow_empty_yp_endpoint_sets = True

        resolution = ctl._resolve(ctx, pb, {})
        instance_pbs = resolution.instance_pbs
        expected_instance_pbs = SAS_TEST_ENDPOINT_SET_INSTANCES
        assert (sorted(instance_pbs, key=functools.cmp_to_key(generator.instances_cmp)) ==
                sorted(expected_instance_pbs, key=functools.cmp_to_key(generator.instances_cmp)))
    finally:
        appconfig.set_value('run.reject_empty_yp_endpoint_sets', False)

    ctl._resolve(ctx, pb, {})
    pb.allow_empty_yp_endpoint_sets = False
    ctl._resolve(ctx, pb, {})

    metrics_after = dict(metrics.ROOT_REGISTRY.items())
    expected_diffs = {
        'backend-ctl-yp-endpoint-set-is-empty-error_summ': 1,
        'backend-ctl-instances-list-is-empty-error_summ': 1,
    }
    for k, expected_diff in six.iteritems(expected_diffs):
        actual_diff = metrics_after[k] - metrics_before.get(k, 0)
        assert actual_diff == expected_diff, 'actual ({}) != expected ({}) for {}'.format(actual_diff, expected_diff, k)


def test_process_manual(ctx, caplog, empty_namespace):
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.MANUAL
    Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'backend is MANUAL, nothing to process' in log.records_text()


def test_process_yp_service_discovery(ctx, caplog, empty_namespace):
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    spec_pb.selector.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='xxx')
    Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'backend is YP_ENDPOINT_SETS_SD, nothing to process' in log.records_text()


def test_process_deleted(ctx, caplog, empty_namespace):
    spec_pb = model_pb2.BackendSpec()
    spec_pb.deleted = True
    spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    spec_pb.selector.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='xxx')
    Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Backend is deleted, nothing to process' in log.records_text()


@pytest.mark.parametrize('is_global', [True, False])
@pytest.mark.parametrize('used_in', [False, 'L3 balancers', 'L7 balancers', 'DNS records'])
def test_self_delete(ctx, caplog, empty_namespace, is_global, used_in):
    appconfig.set_value('run.root_users', [Api.TEST_LOGIN])  # to be able to create global backend via API
    spec_pb = model_pb2.BackendSpec()
    spec_pb.deleted = True
    spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    spec_pb.selector.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='xxx')
    spec_pb.is_global.value = is_global
    Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)
    ctl.SELF_DELETION_COOLDOWN_PERIOD = 0
    ctl._is_backend_used_in_all_l7_balancers = mock.Mock(return_value=used_in == 'L7 balancers')
    ctl._is_backend_used_in_l7_balancers = mock.Mock(return_value=used_in == 'L7 balancers')
    ctl._is_backend_used_in_l3_balancers = mock.Mock(return_value=used_in == 'L3 balancers')
    ctl._is_backend_used_in_dns_balancers = mock.Mock(return_value=used_in == 'DNS records')

    if not used_in:
        with check_log(caplog) as log:
            ctl._maybe_self_delete(ctx)
            assert 'starting cached self deletion checks' in log.records_text()
            assert 'starting full self deletion checks' in log.records_text()
        ctl._is_backend_used_in_l3_balancers.assert_called_once()
        ctl._is_backend_used_in_dns_balancers.assert_called_once()
        if is_global:
            ctl._is_backend_used_in_l7_balancers.assert_not_called()
            ctl._is_backend_used_in_all_l7_balancers.assert_called_once()
        else:
            ctl._is_backend_used_in_all_l7_balancers.assert_not_called()
            ctl._is_backend_used_in_l7_balancers.assert_called_once()
    else:
        with pytest.raises(RuntimeError,
                           match="Critical error: would delete a referenced backend if it wasn't for this raise. "
                                 "Backend is used in some {}".format(used_in)):
            ctl._maybe_self_delete(ctx)


def test_process_nanny_snapshots_not_found(ctx, caplog, empty_namespace):
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.NANNY_SNAPSHOTS
    spec_pb.selector.nanny_snapshots.add(service_id='test')
    Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'failed to read current Nanny snapshot identifiers' in log.records_text()


@mock.patch.object(NannyMockClient, 'get_current_runtime_attrs_id', return_value='123')
def test_process_nanny_snapshots(_1, ctx, caplog, empty_namespace):
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.NANNY_SNAPSHOTS
    spec_pb.selector.nanny_snapshots.add(service_id='test')
    Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'failed to read current Nanny snapshot identifiers' in log.records_text()


def test_process_yp_endpoint_sets(ctx, caplog, empty_namespace):
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    spec_pb.selector.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='xxx')
    Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'endpoint set does not exist, use_sd: False, creating...' in log.records_text()


@mock.patch.object(generator, 'resolve_nanny_snapshot_pbs',
                   return_value=[internals_pb2.Instance(host='x', port=80, weight=1, ipv6_addr='::1')])
@mock.patch.object(NannyMockClient, 'get_target_runtime_attrs_id', return_value='456')
@mock.patch.object(NannyMockClient, 'get_current_runtime_attrs_id', return_value='123')
def test_process_nanny_snapshots2(get_current_runtime_attrs_id_stub,
                                  get_target_runtime_attrs_id_stub,
                                  resolve_nanny_snapshot_pbs_stub,
                                  ctx, caplog, empty_namespace):
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.NANNY_SNAPSHOTS
    spec_pb.selector.nanny_snapshots.add(service_id='test-service-id')
    Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'endpoint set does not exist, use_sd: False, creating...' in log.records_text()

    get_target_runtime_attrs_id_stub.assert_called_with('test-service-id')
    get_current_runtime_attrs_id_stub.assert_not_called()

    resolve_nanny_snapshot_pbs_stub.assert_called_once()
    _, args, kwargs = resolve_nanny_snapshot_pbs_stub.mock_calls[0]
    snapshot_pbs = args[0]
    assert len(snapshot_pbs) == 1
    assert snapshot_pbs[0].service_id == 'test-service-id'
    assert snapshot_pbs[0].snapshot_id == '456'


class ReqidGenerator(object):
    def __init__(self):
        self.c = 0

    def generate_reqid(self):
        self.c += 1
        return u'test-reqid-{}'.format(self.c)


@pytest.mark.vcr
@mock.patch.object(yp_service_discovery.sd_resolver, 'generate_reqid',
                   wraps=ReqidGenerator().generate_reqid)
def test_yp_sd_resolving_es_not_changed(_, ctx, caplog, empty_namespace):
    """
    Tests a case when endpoints do not change and we do not discover it through SD.
    """
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    spec_pb.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'swat-httpbin')
    backend_pb = Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'endpoint set does not exist, use_sd: False, creating...' in log.records_text()
        assert u'processed' in log.records_text()

    assert wait_until(lambda: Api.get_backend(NS_ID, BACKEND_ID).resolver_status.last_attempt.revision_id ==
                              backend_pb.meta.version, timeout=1)

    backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
    assert backend_pb.resolver_status.last_attempt == backend_pb.resolver_status.last_successful_attempt
    assert not backend_pb.resolver_status.last_attempt.yp_sd_timestamps

    endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
    expected_es_spec_pb = model_pb2.EndpointSetSpec()
    expected_es_spec_pb.is_global.SetInParent()
    expected_es_spec_pb.instances.add(
        host=u'httpbin-2.sas.yp-c.yandex.net',
        port=80,
        weight=1.0,
        ipv6_addr=u'2a02:6b8:c08:4403:0:696:d07c:0'
    )
    assert endpoint_set_pb.spec == expected_es_spec_pb
    curr_endpoint_set_version = endpoint_set_pb.meta.version
    prev_sd_powered_namespace_ids = appconfig.get_value(u'run.enable_sd.namespace_ids', [])

    appconfig.set_value(u'run.enable_sd.namespace_ids', [NS_ID])
    try:
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'resolving instances' in log.records_text()
            assert u'use_sd: True' in log.records_text()
            assert u'processed' in log.records_text()

        endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
        assert endpoint_set_pb.meta.version == curr_endpoint_set_version

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        backend_pb.spec.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'chaos-service-slave')
        backend_pb = Api.update_backend(NS_ID, BACKEND_ID, backend_pb.meta.version, backend_pb.spec)

        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'resolving instances' in log.records_text()
            assert u'use_sd: True' in log.records_text()
            assert u'processed' in log.records_text()

        assert wait_until(lambda: Api.get_backend(NS_ID, BACKEND_ID).resolver_status.last_attempt.revision_id ==
                                  backend_pb.meta.version, timeout=1)

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        assert backend_pb.resolver_status.last_attempt == backend_pb.resolver_status.last_successful_attempt
        assert dict(backend_pb.resolver_status.last_attempt.yp_sd_timestamps) == {
            u'sas/chaos-service-slave': 1704825392700856950,
            u'sas/swat-httpbin': 1704825391627116598,
        }

        endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
        prev_endpoint_set_pb = endpoint_set_pb
        assert ({instance_pb.host for instance_pb in endpoint_set_pb.spec.instances} == {
            u'chaos-1704825244524482624.sas.yp-c.yandex.net', u'httpbin-2.sas.yp-c.yandex.net',
            u'chaos-1704824817175236323.sas.yp-c.yandex.net', u'chaos-1704825004006312894.sas.yp-c.yandex.net'})

        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'resolving instances from' in log.records_text()
            assert u'use_sd: True' in log.records_text()
            assert u'processed' in log.records_text()

        assert wait_until(lambda: Api.get_backend(NS_ID, BACKEND_ID).resolver_status.last_attempt.revision_id ==
                                  backend_pb.meta.version, timeout=1)

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        assert backend_pb.resolver_status.last_attempt == backend_pb.resolver_status.last_successful_attempt
        assert dict(backend_pb.resolver_status.last_attempt.yp_sd_timestamps) == {
            u'sas/chaos-service-slave': 1704825392700856950,
            u'sas/swat-httpbin': 1704825391627116598,
        }
        endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
        assert endpoint_set_pb.spec == prev_endpoint_set_pb.spec

        assert ({instance_pb.host for instance_pb in endpoint_set_pb.spec.instances} ==
                {u'chaos-1704825244524482624.sas.yp-c.yandex.net',
                 u'httpbin-2.sas.yp-c.yandex.net',
                 u'chaos-1704824817175236323.sas.yp-c.yandex.net',
                 u'chaos-1704825004006312894.sas.yp-c.yandex.net'})
    finally:
        appconfig.set_value(u'run.enable_sd.namespace_ids', prev_sd_powered_namespace_ids)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'resolving instances' in log.records_text()
        assert u'use_sd: False' in log.records_text()
        assert u'processed' in log.records_text()


@pytest.mark.vcr
@mock.patch.object(yp_service_discovery.sd_resolver, 'generate_reqid',
                   wraps=ReqidGenerator().generate_reqid)
def test_yp_sd_resolving_es_changed(_, ctx, caplog, empty_namespace):
    """
    Tests a case when endpoints change and we discover it through SD.
    """
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    spec_pb.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'swat-httpbin')
    backend_pb = Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert u'endpoint set does not exist, use_sd: False, creating...' in log.records_text()
    assert u'processed' in log.records_text()

    assert wait_until(lambda: Api.get_backend(NS_ID, BACKEND_ID).resolver_status.last_attempt.revision_id ==
                              backend_pb.meta.version, timeout=1)

    backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
    assert backend_pb.resolver_status.last_attempt == backend_pb.resolver_status.last_successful_attempt
    assert not backend_pb.resolver_status.last_attempt.yp_sd_timestamps

    endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
    expected_es_spec_pb = model_pb2.EndpointSetSpec()
    expected_es_spec_pb.is_global.SetInParent()
    expected_es_spec_pb.instances.add(
        host=u'httpbin-2.sas.yp-c.yandex.net',
        port=80,
        weight=1.0,
        ipv6_addr=u'2a02:6b8:c08:4403:0:696:d07c:0'
    )
    assert endpoint_set_pb.spec == expected_es_spec_pb
    curr_endpoint_set_version = endpoint_set_pb.meta.version
    prev_sd_powered_namespace_ids = appconfig.get_value(u'run.enable_sd.namespace_ids', [])

    appconfig.set_value(u'run.enable_sd.namespace_ids', [NS_ID])
    try:
        with check_log(caplog) as log:
            ctl._process(ctx)
        assert u'resolving instances' in log.records_text()
        assert u'use_sd: True' in log.records_text()
        assert u'processed' in log.records_text()

        endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
        assert endpoint_set_pb.meta.version == curr_endpoint_set_version

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        backend_pb.spec.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'chaos-service-slave')
        backend_pb = Api.update_backend(NS_ID, BACKEND_ID, backend_pb.meta.version, backend_pb.spec)

        with check_log(caplog) as log:
            ctl._process(ctx)
        assert u'resolving instances' in log.records_text()
        assert u'use_sd: True' in log.records_text()
        assert u'processed' in log.records_text()

        def check():
            backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
            assert backend_pb.resolver_status.last_attempt.revision_id == backend_pb.meta.version
            assert backend_pb.resolver_status.last_attempt == backend_pb.resolver_status.last_successful_attempt
            assert dict(backend_pb.resolver_status.last_attempt.yp_sd_timestamps) == {
                u'sas/chaos-service-slave': 1704824703358603453,
                u'sas/swat-httpbin': 1704824703358603453,
            }

        wait_until(check)

        def check():
            endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
            assert {instance_pb.host for instance_pb in endpoint_set_pb.spec.instances} == {
                u'chaos-1704824252387039236.sas.yp-c.yandex.net', u'chaos-1704824009721393212.sas.yp-c.yandex.net',
                u'httpbin-2.sas.yp-c.yandex.net', u'chaos-1704824440291852765.sas.yp-c.yandex.net',
            }

        wait_until(check)
        prev_endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)

        with check_log(caplog) as log:
            ctl._process(ctx)
        assert u'resolving instances from' in log.records_text()
        assert u'use_sd: True' in log.records_text()
        assert u'processed' in log.records_text()

        def check():
            backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
            assert backend_pb.resolver_status.last_attempt.revision_id == backend_pb.meta.version
            assert backend_pb.resolver_status.last_attempt == backend_pb.resolver_status.last_successful_attempt
            assert dict(backend_pb.resolver_status.last_attempt.yp_sd_timestamps) == {
                u'sas/chaos-service-slave': 1704824767783116221,
                u'sas/swat-httpbin': 1704824767783112143,
            }

        wait_until(check)
        prev_backend_pb = Api.get_backend(NS_ID, BACKEND_ID)

        endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
        assert endpoint_set_pb.spec != prev_endpoint_set_pb.spec
        prev_endpoint_set_pb = endpoint_set_pb
        assert {instance_pb.host for instance_pb in endpoint_set_pb.spec.instances} == {
            u'chaos-1704824252387039236.sas.yp-c.yandex.net', u'chaos-1704824630344157936.sas.yp-c.yandex.net',
            u'httpbin-2.sas.yp-c.yandex.net', u'chaos-1704824440291852765.sas.yp-c.yandex.net'}
    finally:
        appconfig.set_value(u'run.enable_sd.namespace_ids', prev_sd_powered_namespace_ids)

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert u'resolving instances' in log.records_text()
    assert u'use_sd: False' in log.records_text()
    assert u'processed' in log.records_text()

    def check():
        endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
        assert prev_endpoint_set_pb.spec == endpoint_set_pb.spec

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        assert (backend_pb.resolver_status.last_attempt.yp_sd_timestamps ==
                prev_backend_pb.resolver_status.last_attempt.yp_sd_timestamps)

    wait_until(check)


@pytest.mark.vcr
@mock.patch.object(yp_service_discovery.sd_resolver, 'generate_reqid',
                   wraps=ReqidGenerator().generate_reqid)
def test_yp_sd_resolving_ignore_outdated(_, ctx, caplog, empty_namespace):
    """
    Tests a case when we receive an outdated response from SD and silently ignore it.
    """
    metrics_before = dict(metrics.ROOT_REGISTRY.items())

    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    spec_pb.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'swat-httpbin')
    backend_pb = Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'endpoint set does not exist, use_sd: False, creating...' in log.records_text()
        assert u'processed' in log.records_text()

    assert wait_until(lambda: Api.get_backend(NS_ID, BACKEND_ID).resolver_status.last_attempt.revision_id ==
                              backend_pb.meta.version, timeout=1)

    backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
    assert backend_pb.resolver_status.last_attempt == backend_pb.resolver_status.last_successful_attempt
    assert not backend_pb.resolver_status.last_attempt.yp_sd_timestamps

    prev_endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
    prev_sd_powered_namespace_ids = appconfig.get_value(u'run.enable_sd.namespace_ids', [])

    appconfig.set_value(u'run.enable_sd.namespace_ids', [NS_ID])
    try:
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'resolving instances' in log.records_text()
            assert u'use_sd: True' in log.records_text()
            assert u'processed' in log.records_text()

        endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
        assert endpoint_set_pb.spec == prev_endpoint_set_pb.spec

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        backend_pb.spec.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'chaos-service-slave')
        backend_pb = Api.update_backend(NS_ID, BACKEND_ID, backend_pb.meta.version, backend_pb.spec)

        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'resolving instances' in log.records_text()
            assert u'use_sd: True' in log.records_text()
            assert u'processed' in log.records_text()

        assert wait_until(lambda: Api.get_backend(NS_ID, BACKEND_ID).resolver_status.last_attempt.revision_id ==
                                  backend_pb.meta.version, timeout=1)

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        assert backend_pb.resolver_status.last_attempt == backend_pb.resolver_status.last_successful_attempt
        assert dict(backend_pb.resolver_status.last_attempt.yp_sd_timestamps) == {
            u'sas/chaos-service-slave': 1704825424913109287,
            u'sas/swat-httpbin': 1704825391627116598,
        }
        prev_endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)

        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'resolving instances from' in log.records_text()
            assert u'use_sd: True' in log.records_text()
            assert (u'Received an obsolete response for YP endpoint set sas:chaos-service-slave '
                    u'(req id: test-reqid-5, current ts: 1704825424913109287, '
                    u'received ts: 1704825392700856950)' in log.records_text())

        time.sleep(1)

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        assert backend_pb.resolver_status.last_attempt == backend_pb.resolver_status.last_successful_attempt

        endpoint_set_pb = Api.get_endpoint_set(NS_ID, BACKEND_ID)
        assert endpoint_set_pb.spec == prev_endpoint_set_pb.spec
    finally:
        appconfig.set_value(u'run.enable_sd.namespace_ids', prev_sd_powered_namespace_ids)

    metrics_after = dict(metrics.ROOT_REGISTRY.items())
    expected_diffs = {
        u'backend-ctl-yp-sd-obsolete-response-error_summ': 1,
    }
    for k, expected_diff in six.iteritems(expected_diffs):
        actual_diff = metrics_after[k] - metrics_before[k]
        assert actual_diff == expected_diff, \
            u'actual ({}) != expected ({}) for {}'.format(actual_diff, expected_diff, k)


@pytest.mark.vcr(match_on=("method", "scheme", "host", "port", "path", "query", "body"))
@mock.patch.object(yp_service_discovery.sd_resolver, 'generate_reqid',
                   wraps=ReqidGenerator().generate_reqid)
def test_yp_sd_treat_not_exists_as_empty_false(_, ctx, caplog, empty_namespace):
    """
    Tests run.enable_sd.treat_not_exists_as_empty == False (default).
    """
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    spec_pb.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'swat-httpbin')
    spec_pb.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'i-do-not-exist')
    with pytest.raises(exceptions.BadRequestError, match='YP endpoint set "sas:i-do-not-exist" does not exist'):
        Api.create_backend(NS_ID, BACKEND_ID, spec_pb, validate_yp_endpoint_sets=True)

    backend_pb = Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    prev_sd_powered_namespace_ids = appconfig.get_value(u'run.enable_sd.namespace_ids', [])
    appconfig.set_value(u'run.enable_sd.namespace_ids', [NS_ID])
    try:
        with check_log(caplog) as log:
            ctl._process(ctx)
        assert u'YP endpoint set sas:i-do-not-exist does not exist (req id: test-reqid-4)' in log.records_text()

        assert wait_until(lambda: Api.get_backend(NS_ID, BACKEND_ID).resolver_status.last_attempt.revision_id ==
                                  backend_pb.meta.version, timeout=1)

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        assert backend_pb.resolver_status.last_attempt.succeeded.status == u'False'
        assert (backend_pb.resolver_status.last_attempt.succeeded.message ==
                u'Failed to resolve instances: YP endpoint set sas:i-do-not-exist does not exist')
    finally:
        appconfig.set_value(u'run.enable_sd.namespace_ids', prev_sd_powered_namespace_ids)


@pytest.mark.vcr
@mock.patch.object(yp_service_discovery.sd_resolver, 'generate_reqid',
                   wraps=ReqidGenerator().generate_reqid)
def test_yp_sd_treat_not_exists_as_empty_true(_, ctx, caplog, empty_namespace):
    """
    Tests run.enable_sd.treat_not_exists_as_empty == True.
    """
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    spec_pb.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'swat-httpbin')
    spec_pb.selector.yp_endpoint_sets.add(cluster=u'sas', endpoint_set_id=u'i-do-not-exist')
    backend_pb = Api.create_backend(NS_ID, BACKEND_ID, spec_pb)
    ctl = BackendCtl(NS_ID, BACKEND_ID)

    prev_sd_powered_namespace_ids = appconfig.get_value(u'run.enable_sd.namespace_ids', [])
    prev_treat_not_exists_as_empty = appconfig.get_value(u'run.enable_sd.treat_not_exists_as_empty', False)
    appconfig.set_value(u'run.enable_sd.namespace_ids', [NS_ID])
    appconfig.set_value(u'run.enable_sd.treat_not_exists_as_empty', True)
    try:
        with check_log(caplog) as log:
            ctl._process(ctx)
        assert u'processed' in log.records_text()

        assert wait_until(lambda: Api.get_backend(NS_ID, BACKEND_ID).resolver_status.last_attempt.revision_id ==
                                  backend_pb.meta.version, timeout=1)

        backend_pb = Api.get_backend(NS_ID, BACKEND_ID)
        assert backend_pb.resolver_status.last_attempt.succeeded.status == u'True'
    finally:
        appconfig.set_value(u'run.enable_sd.namespace_ids', prev_sd_powered_namespace_ids)
        appconfig.set_value(u'run.enable_sd.treat_not_exists_as_empty', prev_treat_not_exists_as_empty)

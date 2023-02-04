import os
import logging

import inject
import mock
import pytest
import six
from sepelib.core import config

from awacs.lib import juggler_client
from awacs.lib.nannyclient import INannyClient
from awacs.lib.nannyrpcclient import INannyRpcClient
from awacs.lib.staffclient import IStaffClient, StaffClient
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.lib.strutils import flatten_full_id2
from awacs.model import alerting
from awacs.model import util
from awacs.model.balancer.order.util import make_awacs_balancer_id
from awacs.model.namespace.order import processors as p
from awacs.model.namespace.order.processors import NamespaceOrder
from infra.awacs.proto import model_pb2
from awtest.mocks.staff_client import StaffMockClient
from awtest.mocks.yp_lite_client import YpLiteMockClient
from awtest.mocks.nanny_client import NannyMockClient
from awtest.mocks.nanny_rpc_client import NannyRpcMockClient
from awtest import wait_until, wait_until_passes, check_log

NS_ID = 'namespace-id'


@pytest.fixture
def nanny_client():
    return NannyMockClient(url='https://nanny.yandex-team.ru/v2/', token='DUMMY')


@pytest.fixture
def juggler_client_mock():
    m = mock.Mock(juggler_client.JugglerClient)
    m.get_telegram_subscribers.side_effect = lambda logins: {login: None for login in logins}
    return m


@pytest.fixture
def real_juggler_client():
    return juggler_client.JugglerClient(
        # NOTE set your own oauth token if you're rebuilding vcr cassettes
        oauth_token=os.environ.get('JUGGLER_CLIENT_TOKEN', 'DUMMY'),
        namespace_prefix='test_awacs.',
    )


@pytest.fixture
def staff_client_mock():
    return StaffMockClient()


@pytest.fixture
def privileged_user():
    config.set_value('ns_easy_mode_constraints.any_upstream_mode_whitelist', ['privileged_user'])
    yield 'privileged_user'
    config.set_value('ns_easy_mode_constraints', {})


@pytest.fixture
def real_staff_client():
    return StaffClient.from_config({
        'api_url': 'https://staff-api.yandex-team.ru/v3/',
        # NOTE set your own oauth token if you're rebuilding vcr cassettes
        'oauth_token': os.environ.get('STAFF_CLIENT_TOKEN', 'DUMMY'),
        'req_timeout': 5,
        'verify_ssl': False,
    })


@pytest.fixture(autouse=True)
def deps(binder, caplog, nanny_client, juggler_client_mock, staff_client_mock):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(IYpLiteRpcClient, YpLiteMockClient())
        b.bind(INannyRpcClient, NannyRpcMockClient())
        b.bind(IStaffClient, staff_client_mock)
        b.bind(INannyClient, nanny_client)
        b.bind(juggler_client.IJugglerClient, juggler_client_mock)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_ns_order(cache, zk_storage, use_cert=True, use_dns_record=True, manual_backends=False, use_alerting=True,
                    preset=model_pb2.NamespaceSpec.PR_DEFAULT, notify_staff_group_id=999,
                    flow_type=model_pb2.NamespaceOrder.Content.YP_LITE):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.meta.auth.staff.owners.logins.extend(['original-ns-owner'])
    ns_pb.meta.auth.staff.owners.group_ids.extend(['original-ns-owner-group'])
    ns_pb.order.content.flow_type = flow_type
    if flow_type != model_pb2.NamespaceOrder.Content.EMPTY:
        ns_pb.order.content.yp_lite_allocation_request.locations.append('SAS')
        ns_pb.order.content.endpoint_sets.add(cluster='SAS', id='es')
        if manual_backends:
            ns_pb.order.content.backends['backend'].type = model_pb2.BackendSelector.MANUAL
        else:
            ns_pb.order.content.backends['backend'].type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
            ns_pb.order.content.backends['backend'].yp_endpoint_sets.add(cluster='SAS', endpoint_set_id='b')
        if use_cert:
            ns_pb.order.content.certificate_order_content.ca_name = 'Internal'
            ns_pb.order.content.certificate_order_content.common_name = 'a.ya.ru'
        if use_dns_record:
            ns_pb.order.content.dns_record_request.default.zone = 'my'
            ns_pb.order.content.dns_record_request.default.name_server.namespace_id = 'infra'
            ns_pb.order.content.dns_record_request.default.name_server.id = 'in.yandex-team.ru'
        if use_alerting:
            ns_pb.order.content.alerting_simple_settings.notify_staff_group_id = notify_staff_group_id
    ns_pb.spec.incomplete = True
    ns_pb.spec.preset = preset
    zk_storage.create_namespace(namespace_id=NS_ID,
                                namespace_pb=ns_pb)
    assert wait_until(lambda: cache.get_namespace(NS_ID), timeout=1)
    return NamespaceOrder(ns_pb)


def update_ns(cache, zk_storage, ns_pb, check):
    for pb in zk_storage.update_namespace(NS_ID):
        pb.CopyFrom(ns_pb)
    wait_ns(cache, check)


def wait_ns(cache, check):
    assert wait_until(lambda: check(cache.get_namespace(NS_ID)), timeout=1)
    return cache.get_namespace(NS_ID)


def check_indices(ns_order, entity_pb):
    assert len(entity_pb.meta.indices) == 1
    assert entity_pb.meta.indices[0].id == entity_pb.meta.version
    assert entity_pb.meta.indices[0].ctime == entity_pb.meta.mtime
    included_backend_ids = entity_pb.meta.indices[0].included_backend_ids
    def get_flatten_full_id(b_id): return flatten_full_id2((entity_pb.meta.namespace_id,b_id))
    assert sorted(included_backend_ids) == sorted(map(get_flatten_full_id, ns_order.pb.order.content.backends))


def test_start(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage, use_cert=False)
    ns_order.pb.order.content.flow_type = model_pb2.NamespaceOrder.Content.GENCFG
    with pytest.raises(RuntimeError, match='Gencfg balancers are not supported'):
        p.Start(ns_order).process(ctx)

    ns_order.pb.order.content.flow_type = model_pb2.NamespaceOrder.Content.YP_LITE
    assert p.Start(ns_order).process(ctx).name == 'CREATING_BALANCERS'

    ns_order.pb.order.content.flow_type = model_pb2.NamespaceOrder.Content.QUICK_START
    with pytest.raises(RuntimeError, match='QUICK_START namespace order must contain cert order'):
        p.Start(ns_order).process(ctx)

    ns_order.pb.order.content.certificate_order_content.ca_name = 'Internal'
    ns_order.pb.order.content.certificate_order_content.common_name = 'a.ya.ru'
    assert p.Start(ns_order).process(ctx).name == 'CREATING_BALANCERS'


def test_creating_balancers(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    assert p.CreatingBalancers(ns_order).process(ctx).name == 'WAITING_FOR_BALANCERS_TO_ALLOCATE'
    balancer_id = make_awacs_balancer_id(NS_ID, 'SAS')
    balancer_pb = wait_until(lambda: cache.get_balancer(NS_ID, balancer_id), timeout=1)
    assert set(balancer_pb.meta.auth.staff.owners.logins) == {'original-ns-owner', util.NANNY_ROBOT_LOGIN}
    assert set(balancer_pb.meta.auth.staff.owners.group_ids) == {'original-ns-owner-group'}
    assert balancer_pb.meta.location.type == balancer_pb.meta.location.YP_CLUSTER
    assert balancer_pb.meta.location.yp_cluster == 'SAS'
    assert balancer_pb.meta.transport_paused.value
    assert balancer_pb.order.content.mode == model_pb2.BalancerOrder.Content.YP_LITE
    assert balancer_pb.order.content.abc_service_id == ns_order.pb.meta.abc_service_id
    assert balancer_pb.order.content.cert_id == ns_order.pb.order.content.certificate_order_content.common_name
    assert not balancer_pb.order.content.activate_balancer

    check_indices(ns_order, balancer_pb)
    assert len(balancer_pb.meta.indices) == 1

    req = balancer_pb.order.content.allocation_request
    assert req.location == 'SAS'
    ns_allocation_pb = ns_order.pb.order.content.yp_lite_allocation_request
    assert req.nanny_service_id_slug == ns_allocation_pb.nanny_service_id_slug
    assert req.network_macro == ns_allocation_pb.network_macro
    assert req.type == ns_allocation_pb.type
    assert req.preset.type == ns_allocation_pb.preset.type
    assert req.preset.instances_count == ns_allocation_pb.preset.instances_count
    assert not ns_order.context['completed_balancer_ids'][balancer_id]


def test_waiting_for_balancers_to_allocate(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    balancer_id = make_awacs_balancer_id(NS_ID, 'SAS')
    ns_order.context['completed_balancer_ids'] = {balancer_id: False}

    assert p.WaitingForBalancersToAllocate(ns_order).process(ctx).name == 'WAITING_FOR_BALANCERS_TO_ALLOCATE'

    balancer_id = make_awacs_balancer_id(NS_ID, 'SAS')
    balancer_pb = model_pb2.Balancer()
    balancer_pb.spec.incomplete = True
    balancer_pb.meta.id = balancer_id
    balancer_pb.meta.namespace_id = NS_ID
    balancer_pb.order.content.wait_for_approval_after_allocation = True
    balancer_pb.order.progress.state.id = 'WAITING_FOR_APPROVAL_AFTER_ALLOCATION'
    zk_storage.create_balancer(NS_ID, balancer_id, balancer_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, balancer_id))

    assert p.WaitingForBalancersToAllocate(ns_order).process(ctx).name == 'WAITING_FOR_BALANCERS'
    balancer_pb = zk_storage.get_balancer(NS_ID, balancer_id)
    assert balancer_pb.order.approval.after_allocation


def test_waiting_for_balancers(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    balancer_id = make_awacs_balancer_id(NS_ID, 'SAS')
    ns_order.context['completed_balancer_ids'] = {balancer_id: False}

    assert p.WaitingForBalancers(ns_order).process(ctx).name == 'WAITING_FOR_BALANCERS'
    assert not ns_order.context['completed_balancer_ids'][balancer_id]

    balancer_id = make_awacs_balancer_id(NS_ID, 'SAS')
    balancer_pb = model_pb2.Balancer()
    balancer_pb.spec.incomplete = False
    balancer_pb.meta.id = balancer_id
    balancer_pb.meta.namespace_id = NS_ID
    zk_storage.create_balancer(NS_ID, balancer_id, balancer_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, balancer_id))

    assert p.WaitingForBalancers(ns_order).process(ctx).name == 'CREATING_BACKENDS'
    assert ns_order.context['completed_balancer_ids'][balancer_id]


def test_creating_backends_manual(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage, manual_backends=True)
    assert p.CreatingBackends(ns_order).process(ctx).name == 'CREATING_UPSTREAMS'
    assert ns_order.context['completed_backend_ids']['backend']

    backend_pb = wait_until(lambda: cache.get_backend(NS_ID, 'backend'), timeout=1)
    assert len(cache.list_all_backends(NS_ID)) == 1
    assert set(backend_pb.meta.auth.staff.owners.logins) == {'original-ns-owner', util.NANNY_ROBOT_LOGIN}
    assert set(backend_pb.meta.auth.staff.owners.group_ids) == {'original-ns-owner-group'}
    assert backend_pb.spec.selector == ns_order.pb.order.content.backends['backend']
    endpoint_pb = wait_until(lambda: cache.get_endpoint_set(NS_ID, 'backend'), timeout=1)
    assert len(cache.list_all_endpoint_sets(NS_ID)) == 1
    assert set(endpoint_pb.meta.auth.staff.owners.logins) == {'original-ns-owner', util.NANNY_ROBOT_LOGIN}
    assert set(endpoint_pb.meta.auth.staff.owners.group_ids) == {'original-ns-owner-group'}
    assert len(endpoint_pb.spec.instances) == 1
    assert endpoint_pb.spec.instances[0].host == 'please-change-me'


def test_creating_backends(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    backend_id = 'backend'
    assert p.CreatingBackends(ns_order).process(ctx).name == 'CREATING_UPSTREAMS'
    assert ns_order.context['completed_backend_ids'][backend_id]

    backend_pb = wait_until(lambda: cache.get_backend(NS_ID, backend_id), timeout=1)
    assert len(cache.list_all_backends(NS_ID)) == 1
    assert set(backend_pb.meta.auth.staff.owners.logins) == {'original-ns-owner', util.NANNY_ROBOT_LOGIN}
    assert set(backend_pb.meta.auth.staff.owners.group_ids) == {'original-ns-owner-group'}
    assert ns_order.context['completed_backend_ids'][backend_id]
    assert backend_pb.spec.selector == ns_order.pb.order.content.backends[backend_id]
    assert zk_storage.get_endpoint_set(NS_ID, backend_id) is None


def test_creating_upstreams(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.pb.order.content.endpoint_sets.add(cluster='SAS', id='es2')
    upstream_id = 'default'
    assert p.CreatingUpstreams(ns_order).process(ctx).name == 'CREATING_CERTS'
    assert ns_order.context['completed_upstream_ids'][upstream_id]

    upstream_pb = wait_until(lambda: cache.get_upstream(NS_ID, upstream_id), timeout=1)
    assert len(cache.list_all_upstreams(NS_ID)) == 1
    assert set(upstream_pb.meta.auth.staff.owners.logins) == {'original-ns-owner', util.NANNY_ROBOT_LOGIN}
    assert set(upstream_pb.meta.auth.staff.owners.group_ids) == {'original-ns-owner-group'}
    assert upstream_pb.spec.yandex_balancer.mode == upstream_pb.spec.yandex_balancer.EASY_MODE2
    assert upstream_pb.spec.labels['order'] == '99999999'

    check_indices(ns_order, upstream_pb)
    assert len(upstream_pb.meta.indices) == 1


def test_creating_upstreams_without_cert(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage, use_cert=False)
    assert p.CreatingUpstreams(ns_order).process(ctx).name == 'CREATING_CERTS'


def test_creating_certs(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    assert p.CreatingCerts(ns_order).process(ctx).name == 'WAITING_FOR_CERTS'
    assert not ns_order.context['completed_cert_ids']['a.ya.ru']

    cert_pb = wait_until(lambda: cache.get_cert(NS_ID, 'a.ya.ru'), timeout=1)
    assert len(cache.list_all_certs(NS_ID)) == 1
    assert set(cert_pb.meta.auth.staff.owners.logins) == {'original-ns-owner', util.NANNY_ROBOT_LOGIN}
    assert set(cert_pb.meta.auth.staff.owners.group_ids) == {'original-ns-owner-group'}
    assert cert_pb.order.content == ns_order.pb.order.content.certificate_order_content


def test_waiting_for_certs(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_cert_ids'] = {'a.ya.ru': False}
    assert p.WaitingForCerts(ns_order).process(ctx).name == 'WAITING_FOR_CERTS'
    assert not ns_order.context['completed_cert_ids']['a.ya.ru']

    cert_pb = model_pb2.Certificate()
    cert_pb.spec.incomplete = False
    cert_pb.meta.id = 'a.ya.ru'
    cert_pb.meta.namespace_id = NS_ID
    zk_storage.create_cert(NS_ID, 'a.ya.ru', cert_pb)
    wait_until_passes(lambda: cache.must_get_cert(NS_ID, 'a.ya.ru'))

    assert p.WaitingForCerts(ns_order).process(ctx).name == 'VALIDATING_AWACS_NAMESPACE'
    assert ns_order.context['completed_cert_ids']['a.ya.ru']


def test_creating_domains(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    assert p.CreatingDomains(ns_order).process(ctx).name == 'WAITING_FOR_DOMAINS'
    assert not ns_order.context['completed_domain_ids']['a.ya.ru']

    domain_pb = wait_until(lambda: cache.get_domain(NS_ID, 'a.ya.ru'), timeout=1)
    assert len(cache.list_all_domains(NS_ID)) == 1
    cert_order_pb = domain_pb.order.content.cert_order.content
    assert cert_order_pb.ca_name == ns_order.pb.order.content.certificate_order_content.ca_name
    assert cert_order_pb.common_name == ns_order.pb.order.content.certificate_order_content.common_name
    assert domain_pb.order.content.protocol == model_pb2.DomainSpec.Config.HTTP_AND_HTTPS


def test_waiting_for_domains(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_domain_ids'] = {'a.ya.ru': False}
    assert p.WaitingForDomains(ns_order).process(ctx).name == 'WAITING_FOR_DOMAINS'
    assert not ns_order.context['completed_domain_ids']['a.ya.ru']

    domain_pb = model_pb2.Domain()
    domain_pb.spec.incomplete = False
    domain_pb.meta.id = 'a.ya.ru'
    domain_pb.meta.namespace_id = NS_ID
    zk_storage.create_domain(NS_ID, 'a.ya.ru', domain_pb)
    wait_until_passes(lambda: cache.must_get_domain(NS_ID, 'a.ya.ru'))

    assert p.WaitingForDomains(ns_order).process(ctx).name == 'VALIDATING_AWACS_NAMESPACE'
    assert ns_order.context['completed_domain_ids']['a.ya.ru']


def test_validating_namespace(caplog, ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_balancer_ids'] = {'balancer': False}
    ns_order.context['completed_upstream_ids'] = {'upstream': False}
    ns_order.context['completed_domain_ids'] = {'domain': False}
    ns_order.context['completed_backend_ids'] = {'backend': False}
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = 'balancer'
    balancer_pb.meta.namespace_id = NS_ID
    zk_storage.create_balancer(NS_ID, 'balancer', balancer_pb)
    state_pb = model_pb2.BalancerState()
    state_pb.balancer_id = 'balancer'
    state_pb.namespace_id = NS_ID
    zk_storage.create_balancer_state(NS_ID, 'balancer', state_pb)
    upstream_pb = model_pb2.Upstream()
    upstream_pb.meta.id = 'upstream'
    upstream_pb.meta.namespace_id = NS_ID
    zk_storage.create_upstream(NS_ID, 'upstream', upstream_pb)
    domain_pb = model_pb2.Domain()
    domain_pb.meta.id = 'domain'
    domain_pb.meta.namespace_id = NS_ID
    zk_storage.create_domain(NS_ID, 'domain', domain_pb)
    backend_pb = model_pb2.Backend()
    backend_pb.meta.id = 'backend'
    backend_pb.meta.namespace_id = NS_ID
    zk_storage.create_backend(NS_ID, 'backend', backend_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, 'balancer'))
    wait_until_passes(lambda: cache.must_get_balancer_state(NS_ID, 'balancer'))
    wait_until_passes(lambda: cache.must_get_upstream(NS_ID, 'upstream'))
    wait_until_passes(lambda: cache.must_get_domain(NS_ID, 'domain'))
    wait_until_passes(lambda: cache.must_get_backend(NS_ID, 'backend'))
    with check_log(caplog) as log:
        assert p.ValidatingNamespace(ns_order).process(ctx).name == 'VALIDATING_AWACS_NAMESPACE'
        assert 'Unresolved backends: backend' in log.records_text()
        assert ns_order.context['resolved_backend_ids'] == {'backend': False}

    es_pb = model_pb2.EndpointSet()
    es_pb.meta.id = 'backend'
    es_pb.meta.namespace_id = NS_ID
    zk_storage.create_endpoint_set(NS_ID, 'backend', es_pb)
    wait_until_passes(lambda: cache.must_get_endpoint_set(NS_ID, 'backend'))
    with check_log(caplog) as log:
        assert p.ValidatingNamespace(ns_order).process(ctx).name == 'VALIDATING_AWACS_NAMESPACE'
        assert 'All backends are resolved' in log.records_text()
        assert 'Unresolved backends' not in log.records_text()
        assert 'Not all balancers are valid' in log.records_text()
        assert 'Not all upstreams are valid' in log.records_text()
        assert 'Not all domains are valid' in log.records_text()
        assert 'Not all backends are valid' in log.records_text()
        assert 'Not all endpoint sets are valid' in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()
        assert ns_order.context['resolved_backend_ids'] == {'backend': True}

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.upstreams['upstream'].statuses.add()
        status.validated.status = 'True'
    assert wait_until(
        lambda: (cache.get_balancer_state(NS_ID, 'balancer').upstreams['upstream'].statuses[0].validated.status ==
                 'True'),
        timeout=1)
    with check_log(caplog) as log:
        assert p.ValidatingNamespace(ns_order).process(ctx).name == 'VALIDATING_AWACS_NAMESPACE'
        assert 'Not all balancers are valid' in log.records_text()
        assert 'Not all upstreams are valid' not in log.records_text()
        assert 'Not all domains are valid' in log.records_text()
        assert 'Not all backends are valid' in log.records_text()
        assert 'Not all endpoint sets are valid' in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.domains['domain'].statuses.add()
        status.validated.status = 'True'
    assert wait_until(
        lambda: (cache.get_balancer_state(NS_ID, 'balancer').domains['domain'].statuses[0].validated.status ==
                 'True'),
        timeout=1)
    with check_log(caplog) as log:
        assert p.ValidatingNamespace(ns_order).process(ctx).name == 'VALIDATING_AWACS_NAMESPACE'
        assert 'Not all balancers are valid' in log.records_text()
        assert 'Not all upstreams are valid' not in log.records_text()
        assert 'Not all domains are valid' not in log.records_text()
        assert 'Not all backends are valid' in log.records_text()
        assert 'Not all endpoint sets are valid' in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.backends['backend'].statuses.add()
        status.validated.status = 'True'
    assert wait_until(
        lambda: cache.get_balancer_state(NS_ID, 'balancer').backends['backend'].statuses[0].validated.status == 'True',
        timeout=1)
    with check_log(caplog) as log:
        assert p.ValidatingNamespace(ns_order).process(ctx).name == 'VALIDATING_AWACS_NAMESPACE'
        assert 'Not all balancers are valid' in log.records_text()
        assert 'Not all upstreams are valid' not in log.records_text()
        assert 'Not all domains are valid' not in log.records_text()
        assert 'Not all backends are valid' not in log.records_text()
        assert 'Not all endpoint sets are valid' in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.endpoint_sets['backend'].statuses.add()
        status.validated.status = 'True'
    assert wait_until(
        lambda: (cache.get_balancer_state(NS_ID, 'balancer').endpoint_sets['backend'].statuses[0].validated.status ==
                 'True'),
        timeout=1)
    with check_log(caplog) as log:
        assert p.ValidatingNamespace(ns_order).process(ctx).name == 'VALIDATING_AWACS_NAMESPACE'
        assert 'Not all balancers are valid' in log.records_text()
        assert 'Not all upstreams are valid' not in log.records_text()
        assert 'Not all domains are valid' not in log.records_text()
        assert 'Not all backends are valid' not in log.records_text()
        assert 'Not all endpoint sets are valid' not in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.balancer.statuses.add()
        status.validated.status = 'True'
    assert wait_until(
        lambda: cache.get_balancer_state(NS_ID, 'balancer').balancer.statuses[0].validated.status == 'True',
        timeout=1)
    with check_log(caplog) as log:
        assert p.ValidatingNamespace(ns_order).process(ctx).name == 'UNPAUSING_BALANCER_CONFIG_UPDATES'
        assert 'Not all balancers are valid' not in log.records_text()
        assert 'Not all upstreams are valid' not in log.records_text()
        assert 'Not all domains are valid' not in log.records_text()
        assert 'Not all backends are valid' not in log.records_text()
        assert 'Not all endpoint sets are valid' not in log.records_text()
        assert 'Not everything inside balancer is ready' not in log.records_text()


def test_validating_namespace_qs(caplog, ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_balancer_ids'] = {'balancer': False}
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = 'balancer'
    balancer_pb.meta.namespace_id = NS_ID
    zk_storage.create_balancer(NS_ID, 'balancer', balancer_pb)
    state_pb = model_pb2.BalancerState()
    state_pb.balancer_id = 'balancer'
    state_pb.namespace_id = NS_ID
    zk_storage.create_balancer_state(NS_ID, 'balancer', state_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, 'balancer'))
    wait_until_passes(lambda: cache.must_get_balancer_state(NS_ID, 'balancer'))
    with check_log(caplog) as log:
        assert p.ValidatingNamespace(ns_order).process(ctx).name == 'VALIDATING_AWACS_NAMESPACE'
        assert 'Unresolved backends: backend' not in log.records_text()
        assert ns_order.context['resolved_backend_ids'] == {}
        assert 'Not all balancers are valid' in log.records_text()
        assert 'Not all upstreams are valid' not in log.records_text()
        assert 'Not all domains are valid' not in log.records_text()
        assert 'Not all backends are valid' not in log.records_text()
        assert 'Not all endpoint sets are valid' not in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.balancer.statuses.add()
        status.validated.status = 'True'
    assert wait_until(
        lambda: cache.get_balancer_state(NS_ID, 'balancer').balancer.statuses[0].validated.status == 'True',
        timeout=1)
    with check_log(caplog) as log:
        assert p.ValidatingNamespace(ns_order).process(ctx).name == 'UNPAUSING_BALANCER_CONFIG_UPDATES'
        assert 'Not all balancers are valid' not in log.records_text()
        assert 'Not all upstreams are valid' not in log.records_text()
        assert 'Not all domains are valid' not in log.records_text()
        assert 'Not all backends are valid' not in log.records_text()
        assert 'Not all endpoint sets are valid' not in log.records_text()
        assert 'Not everything inside balancer is ready' not in log.records_text()


def test_unpausing_balancer_config_updates(ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_balancer_ids'] = {'balancer': True}
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = 'balancer'
    balancer_pb.meta.namespace_id = NS_ID
    balancer_pb.meta.transport_paused.value = True
    zk_storage.create_balancer(NS_ID, 'balancer', balancer_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, 'balancer'))
    assert p.UnpausingBalancerConfigUpdates(ns_order).process(ctx).name == 'WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS'
    assert not zk_storage.get_balancer(NS_ID, 'balancer').meta.transport_paused.value


def test_waiting_for_balancers_to_be_in_progress(caplog, ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_balancer_ids'] = {'balancer': False}
    ns_order.context['completed_upstream_ids'] = {'upstream': False}
    ns_order.context['completed_domain_ids'] = {'domain': False}
    ns_order.context['completed_backend_ids'] = {'backend': False}
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = 'balancer'
    balancer_pb.meta.namespace_id = NS_ID
    zk_storage.create_balancer(NS_ID, 'balancer', balancer_pb)
    state_pb = model_pb2.BalancerState()
    state_pb.balancer_id = 'balancer'
    state_pb.namespace_id = NS_ID
    zk_storage.create_balancer_state(NS_ID, 'balancer', state_pb)
    upstream_pb = model_pb2.Upstream()
    upstream_pb.meta.id = 'upstream'
    upstream_pb.meta.namespace_id = NS_ID
    zk_storage.create_upstream(NS_ID, 'upstream', upstream_pb)
    domain_pb = model_pb2.Domain()
    domain_pb.meta.id = 'domain'
    domain_pb.meta.namespace_id = NS_ID
    zk_storage.create_domain(NS_ID, 'domain', domain_pb)
    backend_pb = model_pb2.Backend()
    backend_pb.meta.id = 'backend'
    backend_pb.meta.namespace_id = NS_ID
    zk_storage.create_backend(NS_ID, 'backend', backend_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, 'balancer'))
    wait_until_passes(lambda: cache.must_get_balancer_state(NS_ID, 'balancer'))
    wait_until_passes(lambda: cache.must_get_upstream(NS_ID, 'upstream'))
    wait_until_passes(lambda: cache.must_get_domain(NS_ID, 'domain'))
    wait_until_passes(lambda: cache.must_get_backend(NS_ID, 'backend'))
    with check_log(caplog) as log:
        assert p.WaitingForBalancersToBeInProgress(ns_order).process(ctx).name == \
            'WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS'
        assert 'Unresolved backends: backend' in log.records_text()

    es_pb = model_pb2.EndpointSet()
    es_pb.meta.id = 'backend'
    es_pb.meta.namespace_id = NS_ID
    zk_storage.create_endpoint_set(NS_ID, 'backend', es_pb)
    wait_until_passes(lambda: cache.must_get_endpoint_set(NS_ID, 'backend'))
    with check_log(caplog) as log:
        assert p.WaitingForBalancersToBeInProgress(ns_order).process(ctx).name == \
            'WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS'
        assert 'All backends are resolved' in log.records_text()
        assert 'Unresolved backends' not in log.records_text()
        assert 'Not all balancers are in_progress' in log.records_text()
        assert 'Not all upstreams are in_progress' in log.records_text()
        assert 'Not all domains are in_progress' in log.records_text()
        assert 'Not all backends are in_progress' in log.records_text()
        assert 'Not all endpoint sets are in_progress' in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.upstreams['upstream'].statuses.add()
        status.in_progress.status = 'True'
    assert wait_until(
        lambda: (cache.get_balancer_state(NS_ID, 'balancer').upstreams['upstream'].statuses[0].in_progress.status ==
                 'True'),
        timeout=1)
    with check_log(caplog) as log:
        assert p.WaitingForBalancersToBeInProgress(ns_order).process(ctx).name == \
            'WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS'
        assert 'Not all balancers are in_progress' in log.records_text()
        assert 'Not all upstreams are in_progress' not in log.records_text()
        assert 'Not all domains are in_progress' in log.records_text()
        assert 'Not all backends are in_progress' in log.records_text()
        assert 'Not all endpoint sets are in_progress' in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.domains['domain'].statuses.add()
        status.in_progress.status = 'True'
    assert wait_until(
        lambda: (cache.get_balancer_state(NS_ID, 'balancer').domains['domain'].statuses[0].in_progress.status ==
                 'True'),
        timeout=1)
    with check_log(caplog) as log:
        assert p.WaitingForBalancersToBeInProgress(ns_order).process(ctx).name == \
            'WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS'
        assert 'Not all balancers are in_progress' in log.records_text()
        assert 'Not all upstreams are in_progress' not in log.records_text()
        assert 'Not all domains are in_progress' not in log.records_text()
        assert 'Not all backends are in_progress' in log.records_text()
        assert 'Not all endpoint sets are in_progress' in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.backends['backend'].statuses.add()
        status.in_progress.status = 'True'
    assert wait_until(
        lambda: (cache.get_balancer_state(NS_ID, 'balancer').backends['backend'].statuses[0].in_progress.status ==
                 'True'),
        timeout=1)
    with check_log(caplog) as log:
        assert p.WaitingForBalancersToBeInProgress(ns_order).process(ctx).name == \
            'WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS'
        assert 'Not all balancers are in_progress' in log.records_text()
        assert 'Not all upstreams are in_progress' not in log.records_text()
        assert 'Not all domains are in_progress' not in log.records_text()
        assert 'Not all backends are in_progress' not in log.records_text()
        assert 'Not all endpoint sets are in_progress' in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.endpoint_sets['backend'].statuses.add()
        status.in_progress.status = 'True'
    assert wait_until(
        lambda: cache.get_balancer_state(NS_ID, 'balancer').endpoint_sets['backend'].statuses[
            0].in_progress.status == 'True',
        timeout=1)
    with check_log(caplog) as log:
        assert p.WaitingForBalancersToBeInProgress(ns_order).process(ctx).name == \
            'WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS'
        assert 'Not all balancers are in_progress' in log.records_text()
        assert 'Not all upstreams are in_progress' not in log.records_text()
        assert 'Not all domains are in_progress' not in log.records_text()
        assert 'Not all backends are in_progress' not in log.records_text()
        assert 'Not all endpoint sets are in_progress' not in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.balancer.statuses.add()
        status.in_progress.status = 'True'
    assert wait_until(
        lambda: cache.get_balancer_state(NS_ID, 'balancer').balancer.statuses[0].in_progress.status == 'True',
        timeout=1)
    with check_log(caplog) as log:
        assert p.WaitingForBalancersToBeInProgress(ns_order).process(ctx).name == 'ACTIVATING_NANNY_SERVICES'
        assert 'Not all balancers are in_progress' not in log.records_text()
        assert 'Not all upstreams are in_progress' not in log.records_text()
        assert 'Not all domains are in_progress' not in log.records_text()
        assert 'Not all backends are in_progress' not in log.records_text()
        assert 'Not all endpoint sets are in_progress' not in log.records_text()
        assert 'Not everything inside balancer is ready' not in log.records_text()


def test_waiting_for_balancers_to_be_in_progress_qs(caplog, ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_balancer_ids'] = {'balancer': False}
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = 'balancer'
    balancer_pb.meta.namespace_id = NS_ID
    zk_storage.create_balancer(NS_ID, 'balancer', balancer_pb)
    state_pb = model_pb2.BalancerState()
    state_pb.balancer_id = 'balancer'
    state_pb.namespace_id = NS_ID
    zk_storage.create_balancer_state(NS_ID, 'balancer', state_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, 'balancer'))
    wait_until_passes(lambda: cache.must_get_balancer_state(NS_ID, 'balancer'))
    with check_log(caplog) as log:
        assert p.WaitingForBalancersToBeInProgress(ns_order).process(ctx).name == \
            'WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS'
        assert 'Unresolved backends' not in log.records_text()
        assert 'All backends are resolved' in log.records_text()
        assert 'Not all balancers are in_progress' in log.records_text()
        assert 'Not all upstreams are in_progress' not in log.records_text()
        assert 'Not all domains are in_progress' not in log.records_text()
        assert 'Not all backends are in_progress' not in log.records_text()
        assert 'Not all endpoint sets are in_progress' not in log.records_text()
        assert 'Not everything inside balancer is ready' in log.records_text()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.balancer.statuses.add()
        status.in_progress.status = 'True'
    assert wait_until(
        lambda: cache.get_balancer_state(NS_ID, 'balancer').balancer.statuses[0].in_progress.status == 'True',
        timeout=1)
    with check_log(caplog) as log:
        assert p.WaitingForBalancersToBeInProgress(ns_order).process(ctx).name == 'ACTIVATING_NANNY_SERVICES'
        assert 'Not all balancers are in_progress' not in log.records_text()
        assert 'Not all upstreams are in_progress' not in log.records_text()
        assert 'Not all domains are in_progress' not in log.records_text()
        assert 'Not all backends are in_progress' not in log.records_text()
        assert 'Not all endpoint sets are in_progress' not in log.records_text()
        assert 'Not everything inside balancer is ready' not in log.records_text()


def test_activating_nanny_services(ctx, cache, zk_storage, nanny_client):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_balancer_ids'] = {'balancer': True}
    state_pb = model_pb2.BalancerState()
    state_pb.balancer_id = 'balancer'
    state_pb.namespace_id = NS_ID
    zk_storage.create_balancer_state(NS_ID, 'balancer', state_pb)
    wait_until_passes(lambda: cache.must_get_balancer_state(NS_ID, 'balancer'))

    nanny_client.set_snapshot_state = mock.Mock()
    assert p.ActivatingNannyServices(ns_order).process(ctx).name == 'ACTIVATING_NANNY_SERVICES'
    assert not ns_order.context['balancer_snapshots_info']
    nanny_client.set_snapshot_state.assert_not_called()

    for state_pb in zk_storage.update_balancer_state(NS_ID, 'balancer', state_pb):
        status = state_pb.balancer.statuses.add()
        status.in_progress.meta.nanny_static_file.snapshots.add(service_id='service', snapshot_id='snapshot')
    wait_until_passes(
        lambda: (
            cache.get_balancer_state(NS_ID, 'balancer').balancer.statuses[
                -1].in_progress.meta.nanny_static_file.snapshots[-1]
        ))
    assert p.ActivatingNannyServices(ns_order).process(ctx).name == 'CREATING_DNS_RECORD'
    assert ns_order.context['balancer_snapshots_info']['balancer'] == ('service', 'snapshot')
    nanny_client.set_snapshot_state.assert_called_once_with(
        comment=u'Activating L7 balancer',
        recipe=u'common',
        service_id=u'service',
        snapshot_id=u'snapshot',
        state=u'ACTIVE'
    )


def test_creating_dns_record_with_balancers(ctx, dao, cache, zk_storage):
    dao.create_default_name_servers()
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_balancer_ids'] = {'name.with.periods_sas': True}
    assert p.CreatingDnsRecord(ns_order).process(ctx).name == 'VALIDATING_DNS_RECORD'
    assert ns_order.context['completed_dns_record_ids']['my.in.yandex-team.ru']
    dns_record_pb = zk_storage.get_dns_record(NS_ID, 'my.in.yandex-team.ru')
    assert set(dns_record_pb.meta.auth.staff.owners.logins) == {'original-ns-owner', util.NANNY_ROBOT_LOGIN}
    assert set(dns_record_pb.meta.auth.staff.owners.group_ids) == {'original-ns-owner-group'}
    assert dns_record_pb.spec.name_server.namespace_id == 'infra'
    assert dns_record_pb.spec.name_server.id == 'in.yandex-team.ru'
    assert dns_record_pb.spec.address.zone == 'my'
    assert len(dns_record_pb.spec.address.backends.backends) == 0
    assert len(dns_record_pb.spec.address.backends.balancers) == 1
    assert dns_record_pb.spec.address.backends.type == dns_record_pb.spec.address.backends.BALANCERS
    balancer = dns_record_pb.spec.address.backends.balancers[0]
    assert balancer.id == 'name.with.periods_sas'


def test_validating_dns_record(caplog, ctx, cache, zk_storage):
    ns_order = create_ns_order(cache, zk_storage)
    ns_order.context['completed_balancer_ids'] = {'backend': True}
    ns_order.context['completed_dns_record_ids'] = {'dns-record': True}
    state_pb = model_pb2.DnsRecord()
    state_pb.meta.id = 'dns-record'
    state_pb.meta.namespace_id = NS_ID
    zk_storage.create_dns_record(NS_ID, 'dns-record', state_pb)
    state_pb = model_pb2.DnsRecordState()
    state_pb.dns_record_id = 'dns-record'
    state_pb.namespace_id = NS_ID
    zk_storage.create_dns_record_state(NS_ID, 'dns-record', state_pb)
    wait_until_passes(lambda: cache.must_get_dns_record(NS_ID, 'dns-record'))
    wait_until_passes(lambda: cache.must_get_dns_record_state(NS_ID, 'dns-record'))
    with check_log(caplog) as log:
        assert p.ValidatingDnsRecord(ns_order).process(ctx).name == 'VALIDATING_DNS_RECORD'
        assert 'Backend "backend" is not resolved yet' in log.records_text()

    backend_pb = model_pb2.Backend()
    backend_pb.meta.id = 'backend'
    backend_pb.meta.namespace_id = NS_ID
    zk_storage.create_backend(NS_ID, 'backend', backend_pb)
    es_pb = model_pb2.EndpointSet()
    es_pb.meta.id = 'backend'
    es_pb.meta.namespace_id = NS_ID
    zk_storage.create_endpoint_set(NS_ID, 'backend', es_pb)
    wait_until_passes(lambda: cache.must_get_backend(NS_ID, 'backend'))
    wait_until_passes(lambda: cache.must_get_endpoint_set(NS_ID, 'backend'))

    with check_log(caplog) as log:
        assert p.ValidatingDnsRecord(ns_order).process(ctx).name == 'VALIDATING_DNS_RECORD'
        assert 'Backend "backend" is not resolved yet' not in log.records_text()
        assert 'Not all DNS records are ready' in log.records_text()
        assert 'Not all backends are ready' in log.records_text()
        assert 'Not all endpoint sets are ready' in log.records_text()
        assert 'Not everything inside DNS records is ready' in log.records_text()

    for state_pb in zk_storage.update_dns_record_state(NS_ID, 'dns-record', state_pb):
        status = state_pb.backends['backend'].statuses.add()
        status.validated.status = 'True'
    assert wait_until(
        lambda: (cache.get_dns_record_state(NS_ID, 'dns-record').backends['backend'].statuses[0].validated.status ==
                 'True'),
        timeout=1)
    with check_log(caplog) as log:
        assert p.ValidatingDnsRecord(ns_order).process(ctx).name == 'VALIDATING_DNS_RECORD'
        assert 'Not all DNS records are ready' in log.records_text()
        assert 'Not all backends are ready' not in log.records_text()
        assert 'Not all endpoint sets are ready' in log.records_text()
        assert 'Not everything inside DNS records is ready' in log.records_text()

    for state_pb in zk_storage.update_dns_record_state(NS_ID, 'dns-record', state_pb):
        status = state_pb.endpoint_sets['backend'].statuses.add()
        status.validated.status = 'True'
    assert wait_until(
        lambda: (cache.get_dns_record_state(NS_ID, 'dns-record').endpoint_sets['backend'].statuses[
            0].validated.status ==
            'True'),
        timeout=1)
    with check_log(caplog) as log:
        assert p.ValidatingDnsRecord(ns_order).process(ctx).name == 'VALIDATING_DNS_RECORD'
        assert 'Not all DNS records are ready' in log.records_text()
        assert 'Not all backends are ready' not in log.records_text()
        assert 'Not all endpoint sets are ready' not in log.records_text()
        assert 'Not everything inside DNS records is ready' in log.records_text()

    for state_pb in zk_storage.update_dns_record_state(NS_ID, 'dns-record', state_pb):
        status = state_pb.dns_record.statuses.add()
        status.validated.status = 'True'
    assert wait_until(
        lambda: cache.get_dns_record_state(NS_ID, 'dns-record').dns_record.statuses[0].validated.status == 'True',
        timeout=1)
    with check_log(caplog) as log:
        assert p.ValidatingDnsRecord(ns_order).process(ctx).name == 'ENABLING_NAMESPACE_ALERTING'
        assert 'All DNS records are ready' in log.records_text()
        assert 'Not all DNS records are ready' not in log.records_text()
        assert 'Not all backends are ready' not in log.records_text()
        assert 'Not all endpoint sets are ready' not in log.records_text()
        assert 'Not everything inside DNS records is ready' not in log.records_text()


@pytest.mark.parametrize('preset', [model_pb2.NamespaceSpec.PR_DEFAULT])
@pytest.mark.parametrize("staff_group_users", list(range(1, 10)))
@pytest.mark.parametrize("telegram_users", list(range(10)))
def test_enabling_namespace_alerting_default(ctx, cache, zk_storage, nanny_client, preset, staff_client_mock, juggler_client_mock, staff_group_users, telegram_users):
    staff_client_mock.get_group_members = lambda group_id: ['{}_{}'.format(group_id, idx) for idx in range(staff_group_users)]
    juggler_client_mock.get_telegram_subscribers.side_effect = lambda logins: {login: "ololo" for login in logins[:telegram_users]}

    ns_order = create_ns_order(cache, zk_storage)
    assert p.EnablingNamespaceAlerting(ns_order).process(ctx).name == 'FINALIZING'
    ns_pb = zk_storage.get_namespace(NS_ID)
    assert ns_pb.spec.incomplete
    assert ns_pb.spec.alerting.version == six.text_type(alerting.CURRENT_VERSION)
    assert ns_pb.spec.alerting.juggler_raw_downtimers.staff_group_ids == [999]
    assert len(ns_pb.spec.alerting.juggler_raw_notify_rules.balancer) == 2
    assert not ns_pb.spec.alerting.balancer_checks_disabled
    assert ns_pb.spec.preset == preset

    if (
        staff_group_users < alerting.SMS_RECEIVERS_THRESHOLD
        or telegram_users < alerting.MIN_TELEGRAM_RECEIVERS
        or float(telegram_users) / staff_group_users < alerting.MIN_TELEGRAM_RECEIVERS_RATIO
    ):
        assert not all('telegram' in rule.template_kwargs for rule in ns_pb.spec.alerting.juggler_raw_notify_rules.balancer)
        assert any('sms' in rule.template_kwargs for rule in ns_pb.spec.alerting.juggler_raw_notify_rules.balancer), (staff_group_users,
                                                                                                                      telegram_users, ns_pb.spec.alerting.juggler_raw_notify_rules)
    else:
        assert not all('sms' in rule.template_kwargs for rule in ns_pb.spec.alerting.juggler_raw_notify_rules.balancer)
        assert any('telegram' in rule.template_kwargs for rule in ns_pb.spec.alerting.juggler_raw_notify_rules.balancer)


@pytest.mark.vcr
@pytest.mark.parametrize('group_id,telegram_enabled', [
    (24836, False),  # svc_skynet
    (103591, True),  # svc_drug
],)
def test_enabling_namespace_alerting_default_vcr(ctx, cache, zk_storage, nanny_client, group_id, telegram_enabled,
                                                 binder, real_staff_client, real_juggler_client):
    def configure(b):
        b.bind(IYpLiteRpcClient, YpLiteMockClient())
        b.bind(INannyRpcClient, NannyRpcMockClient())
        b.bind(IStaffClient, real_staff_client)
        b.bind(INannyClient, nanny_client)
        b.bind(juggler_client.IJugglerClient, real_juggler_client)
        binder(b)

    inject.clear_and_configure(configure)

    ns_order = create_ns_order(cache, zk_storage, notify_staff_group_id=group_id)
    assert p.EnablingNamespaceAlerting(ns_order).process(ctx).name == 'FINALIZING'
    ns_pb = zk_storage.get_namespace(NS_ID)
    assert ns_pb.spec.incomplete
    assert ns_pb.spec.alerting.version == six.text_type(alerting.CURRENT_VERSION)
    assert ns_pb.spec.alerting.juggler_raw_downtimers.staff_group_ids == [group_id]
    assert len(ns_pb.spec.alerting.juggler_raw_notify_rules.balancer) == 2
    assert not ns_pb.spec.alerting.balancer_checks_disabled
    assert ns_pb.spec.preset == model_pb2.NamespaceSpec.PR_DEFAULT

    if telegram_enabled:
        assert not all('sms' in rule.template_kwargs for rule in ns_pb.spec.alerting.juggler_raw_notify_rules.balancer)
        assert any('telegram' in rule.template_kwargs for rule in ns_pb.spec.alerting.juggler_raw_notify_rules.balancer)
    else:
        assert not all('telegram' in rule.template_kwargs for rule in ns_pb.spec.alerting.juggler_raw_notify_rules.balancer)
        assert any('sms' in rule.template_kwargs for rule in ns_pb.spec.alerting.juggler_raw_notify_rules.balancer)


@pytest.mark.parametrize('preset', [model_pb2.NamespaceSpec.PR_WITHOUT_NOTIFICATIONS])
def test_enabling_namespace_alerting_taxi(ctx, cache, zk_storage, nanny_client, preset):
    ns_order = create_ns_order(cache, zk_storage, preset=preset)
    assert p.EnablingNamespaceAlerting(ns_order).process(ctx).name == 'FINALIZING'
    ns_pb = zk_storage.get_namespace(NS_ID)
    assert ns_pb.spec.incomplete
    assert ns_pb.spec.alerting.version == six.text_type(alerting.CURRENT_VERSION)
    assert ns_pb.spec.alerting.juggler_raw_downtimers.staff_group_ids == [999]
    assert ns_pb.spec.alerting.WhichOneof('notify_rules') == 'notify_rules_disabled'
    assert ns_pb.spec.alerting.notify_rules_disabled
    assert not ns_pb.spec.alerting.balancer_checks_disabled
    assert ns_pb.spec.preset == preset


@pytest.mark.parametrize('preset', [model_pb2.NamespaceSpec.PR_PLATFORM_CHECKS_ONLY])
def test_enabling_namespace_alerting_maps(ctx, cache, zk_storage, nanny_client, preset):
    ns_order = create_ns_order(cache, zk_storage, preset=preset)
    assert p.EnablingNamespaceAlerting(ns_order).process(ctx).name == 'FINALIZING'
    ns_pb = zk_storage.get_namespace(NS_ID)
    assert ns_pb.spec.incomplete
    assert ns_pb.spec.alerting.version == six.text_type(alerting.CURRENT_VERSION)
    assert ns_pb.spec.alerting.juggler_raw_downtimers.staff_group_ids == [999]
    assert len(ns_pb.spec.alerting.juggler_raw_notify_rules.balancer) == 2
    assert ns_pb.spec.alerting.balancer_checks_disabled
    assert ns_pb.spec.preset == preset


def test_finalizing(ctx, cache, zk_storage, nanny_client):
    ns_order = create_ns_order(cache, zk_storage)
    assert p.Finalizing(ns_order).process(ctx).name == 'FINISHED'
    ns_pb = zk_storage.get_namespace(NS_ID)
    assert not ns_pb.spec.incomplete


def test_finalizing_empty_order(ctx, cache, zk_storage, nanny_client):
    ns_order = create_ns_order(cache, zk_storage, flow_type=model_pb2.NamespaceOrder.Content.EMPTY)
    assert p.Finalizing(ns_order).process(ctx).name == 'FINISHED'
    ns_pb = zk_storage.get_namespace(NS_ID)
    assert not ns_pb.spec.incomplete
    for object_type in ('balancer', 'upstream', 'domain', 'knob', 'certificate', 'l7heavy_config', 'weight_section'):
        assert ns_pb.spec.object_upper_limits.HasField(object_type)
        assert getattr(ns_pb.spec.object_upper_limits, object_type).value == 0


def test_cancelling(ctx, cache, zk_storage, nanny_client):
    ns_order = create_ns_order(cache, zk_storage)
    balancer_id = make_awacs_balancer_id(NS_ID, 'SAS')
    balancer_pb = model_pb2.Balancer()
    balancer_pb.spec.incomplete = True
    balancer_pb.meta.id = balancer_id
    balancer_pb.meta.namespace_id = NS_ID
    balancer_pb.order.cancelled.value = False
    zk_storage.create_balancer(NS_ID, balancer_id, balancer_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, balancer_id))
    assert p.Cancelling(ns_order).process(ctx).name == 'CANCELLED'
    ns_pb = zk_storage.get_namespace(NS_ID)
    assert not ns_pb.spec.incomplete
    balancer_pb = zk_storage.get_balancer(NS_ID, balancer_id)
    assert balancer_pb.order.cancelled.value

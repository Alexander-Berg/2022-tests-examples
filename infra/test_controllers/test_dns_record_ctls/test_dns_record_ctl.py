import logging

import inject
import mock
import monotonic
import pytest
import six
import flaky

from awacs.lib import nannyrpcclient, l3mgrclient
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.model.backend import BackendCtl as BaseBackendCtl
from awacs.model.dao import INFRA_NAMESPACE_ID, DEFAULT_NAME_SERVERS
from awacs.model.dns_records.ctl import DnsRecordCtl as BaseDnsRecordCtl
from infra.awacs.proto import api_pb2, model_pb2
from infra.swatlib.auth import abc
from infra.swatlib.rpc.exceptions import NotFoundError
from awtest.dns_record_util import DnsRecordState, f as d_f, t as d_t
from awtest import wait_until, wait_until_passes
from awtest.mocks.resolver import ResolverStub
from awtest.api import Api, make_backend_spec_pb


MAX_RUNS = 3

L7_MACRO_BALANCER_YML = """
l7_macro:
  version: 0.1.0
  http: {}
"""


class BackendCtl(BaseBackendCtl):
    PROCESS_INTERVAL = 9999
    SELF_DELETION_CHECK_INTERVAL = 0.01
    SLEEP_AFTER_EXCEPTION_TIMEOUT = 9999
    EVENTS_QUEUE_GET_TIMEOUT = 9999
    SELF_DELETION_COOLDOWN_PERIOD = 0.01

    def _fill_snapshot_ids(self, selector_pb):
        for nanny_snapshot_pb in selector_pb.nanny_snapshots:
            nanny_snapshot_pb.snapshot_id = 'xxx'


class DnsRecordCtl(BaseDnsRecordCtl):
    def _should_process(self, curr_time):  # ignore timeouts for tests
        return True


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, caplog, yp_lite_mock_client, nanny_rpc_mock_client, abc_client, l3_mgr_client):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(abc.IAbcClient, abc_client)
        b.bind(l3mgrclient.IL3MgrClient, l3_mgr_client)
        b.bind(nannyrpcclient.INannyRpcClient, nanny_rpc_mock_client)
        b.bind(IYpLiteRpcClient, yp_lite_mock_client)
        binder_with_nanny_client(b)

    caplog.set_level(logging.DEBUG)
    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def default_nses(deps, dao, cache):
    dao.create_default_name_servers()
    for ns in DEFAULT_NAME_SERVERS:
        wait_until_passes(lambda: cache.must_get_name_server(INFRA_NAMESPACE_ID, ns))


def fill_dns_record(target_pb, full_name_server_id, zone, backend_ids, addressing_type):
    target_pb.name_server.namespace_id = full_name_server_id[0]
    target_pb.name_server.id = full_name_server_id[1]
    target_pb.address.zone = zone
    target_pb.address.backends.type = addressing_type
    if addressing_type == model_pb2.DnsBackendsSelector.EXPLICIT:
        for backend_id in backend_ids:
            target_pb.address.backends.backends.add(id=backend_id)
    elif addressing_type == model_pb2.DnsBackendsSelector.BALANCERS:
        for backend_id in backend_ids:
            target_pb.address.backends.balancers.add(id=backend_id)
    else:
        for backend_id in backend_ids:
            target_pb.address.backends.l3_balancers.add(id=backend_id)
    return target_pb


def make_dns_record_order_pb(full_name_server_id, zone, backend_ids, addressing_type):
    order_pb = model_pb2.DnsRecordOrder.Content()
    return fill_dns_record(order_pb, full_name_server_id, zone, backend_ids, addressing_type)


def make_dns_record_spec_pb(full_name_server_id, zone, backend_ids, addressing_type):
    spec_pb = model_pb2.DnsRecordSpec()
    return fill_dns_record(spec_pb, full_name_server_id, zone, backend_ids, addressing_type)


def make_balancers_backend_spec_pb(balancer_id):
    """
    :type balancer_id: six.text_type
    :rtype: models_pb2.BackendSpec
    """
    backend_spec_pb = model_pb2.BackendSpec()
    backend_spec_pb.selector.type = backend_spec_pb.selector.BALANCERS
    backend_spec_pb.selector.balancers.add(id=balancer_id)
    return backend_spec_pb


def make_l3_balancer_spec_pb(l3mgr_service_id, backend_ids_to_include):
    """
    :type l3mgr_service_id: six.text_type
    :type backend_ids_to_include: list[six.text_type]
    :rtype: model_pb2.L3BalancerSpec
    """
    spec_pb = model_pb2.L3BalancerSpec()
    spec_pb.l3mgr_service_id = l3mgr_service_id
    spec_pb.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    spec_pb.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS
    for backend_id in backend_ids_to_include:
        spec_pb.real_servers.backends.add(id=backend_id)
    return spec_pb


def create_namespace(namespace_id, dns_record_ids, backend_ids,
                     dns_record_backend_links, balancer_backend_ids, l3_balancer_ids,
                     dns_records_params=None, backend_params=None, l3_balancers_params=None,
                     l3_balancer_backend_links=None):
    """
    :param six.text_type namespace_id:
    :param list[six.text_type] dns_record_ids:
    :param list[six.text_type] backend_ids:
    :param list[six.text_type] balancer_backend_ids:
    :param list[six.text_type] l3_balancer_ids:
    :param dict[six.text_type, list[six.text_type]] dns_record_backend_links:
    :param dict[six.text_type, dict] dns_records_params:
    :param dict[six.text_type, dict] backend_params:
    :param dict[six.text_type, dict] l3_balancers_params:
    :param dict[six.text_type, list[six.text_type]] l3_balancer_backend_links:
    """
    assert not (balancer_backend_ids and backend_ids and l3_balancer_ids)
    assert balancer_backend_ids or backend_ids or l3_balancer_ids
    if balancer_backend_ids:
        addressing_type = model_pb2.DnsBackendsSelector.BALANCERS
    elif l3_balancer_ids:
        addressing_type = model_pb2.DnsBackendsSelector.L3_BALANCERS
    else:
        addressing_type = model_pb2.DnsBackendsSelector.EXPLICIT
    dns_records_params = dns_records_params or {}
    backend_params = backend_params or {}

    Api.create_namespace(namespace_id=namespace_id)

    for i, backend_id in enumerate(backend_ids):
        params = backend_params.get(backend_id, {})
        backend_spec_pb = make_backend_spec_pb(nanny_service_ids=['service_{}'.format(i)])
        backend_spec_pb.is_global.value = params.get('is_global', False)
        Api.create_backend(namespace_id=namespace_id, backend_id=backend_id, spec_pb=backend_spec_pb)

    for backend_id in balancer_backend_ids:
        balancer_spec_pb = model_pb2.BalancerSpec()
        balancer_spec_pb.config_transport.type = model_pb2.NANNY_STATIC_FILE
        balancer_spec_pb.config_transport.nanny_static_file.service_id = backend_id
        balancer_spec_pb.type = model_pb2.YANDEX_BALANCER
        balancer_spec_pb.yandex_balancer.yaml = L7_MACRO_BALANCER_YML
        balancer_spec_pb.yandex_balancer.mode = model_pb2.YandexBalancerSpec.EASY_MODE
        Api.create_balancer(namespace_id=namespace_id, balancer_id=backend_id, spec_pb=balancer_spec_pb)
        backend_spec_pb = make_balancers_backend_spec_pb(balancer_id=backend_id)
        Api.create_backend(namespace_id=namespace_id, backend_id=backend_id, spec_pb=backend_spec_pb,
                           is_system=True)

    for l3_balancer_id in l3_balancer_ids:
        params = l3_balancers_params[l3_balancer_id]
        included_backend_ids = sorted(l3_balancer_backend_links.get(l3_balancer_id, []))
        l3mgr_service_id = params['l3mgr_service_id']
        l3_balancer_spec_pb = make_l3_balancer_spec_pb(l3mgr_service_id, included_backend_ids)
        Api.create_l3_balancer(namespace_id=namespace_id, l3_balancer_id=l3_balancer_id, spec_pb=l3_balancer_spec_pb)

    for dns_record_id in dns_record_ids:
        params = dns_records_params.get(dns_record_id, {})
        included_backend_ids = sorted(dns_record_backend_links.get(dns_record_id, []))
        full_name_server_id = params['full_name_server_id']
        zone = params['zone']
        if addressing_type == model_pb2.DnsBackendsSelector.L3_BALANCERS:
            order_pb = make_dns_record_order_pb(full_name_server_id, zone, included_backend_ids, addressing_type)
            Api.create_dns_record(namespace_id=namespace_id, dns_record_id=dns_record_id, order_pb=order_pb)
        else:
            spec_pb = make_dns_record_spec_pb(full_name_server_id, zone, included_backend_ids, addressing_type)
            Api.create_dns_record(namespace_id=namespace_id, dns_record_id=dns_record_id, spec_pb=spec_pb)


@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@pytest.mark.parametrize(
    'addressing_type', (model_pb2.DnsBackendsSelector.BALANCERS, model_pb2.DnsBackendsSelector.EXPLICIT))
@mock.patch.object(BackendCtl, '_resolve', side_effect=ResolverStub())
def test_full(_1, default_nses, zk_storage, cache, ctx, ctlrunner, caplog, dao, addressing_type, checker):
    # get not nameserver with not L3_ONLY backends
    name_server_pb = cache.must_get_name_server(
        namespace_id=INFRA_NAMESPACE_ID,
        name_server_id="in.yandex-team.ru")
    full_name_server_id = (name_server_pb.meta.namespace_id, name_server_pb.meta.id)

    namespace_id_1 = 'test_namespace_1'
    dns_record_1_id = 'test_dns_record_1'
    dns_record_2_id = 'test_dns_record_2'

    if addressing_type == model_pb2.DnsBackendsSelector.BALANCERS:
        balancer_backends = ['sas.backend_1', 'sas.backend_2']
        backends = []
    else:
        balancer_backends = []
        backends = ['sas.backend_1', 'sas.backend_2']

    create_namespace(
        namespace_id=namespace_id_1,
        dns_record_ids=[dns_record_1_id, dns_record_2_id],
        backend_ids=backends,
        balancer_backend_ids=balancer_backends,
        l3_balancer_ids=[],
        dns_record_backend_links={
            dns_record_1_id: ['sas.backend_1', 'sas.backend_2'],
            dns_record_2_id: ['sas.backend_2'],
        },
        dns_records_params={
            dns_record_1_id: {
                'full_name_server_id': full_name_server_id,
                'zone': 'netmon',
            },
            dns_record_2_id: {
                'full_name_server_id': full_name_server_id,
                'zone': 'nanny',
            }
        },
    )

    ctx.log.info('Step 1: unresolved backends')
    dns_record_1_ctl = DnsRecordCtl(namespace_id_1, dns_record_1_id)
    dns_record_1_ctl._init_processors()
    dns_record_2_ctl = DnsRecordCtl(namespace_id_1, dns_record_2_id)
    dns_record_2_ctl._init_processors()

    for a in checker:
        with a:
            dns_record_1_ctl._process_empty_queue(ctx)
            dns_record_2_ctl._process_empty_queue(ctx)

            actual = DnsRecordState.from_api(namespace_id_1, dns_record_1_id)
            expected = DnsRecordState(
                dns_record=[d_f],
                backends={
                    'sas.backend_1': [d_f],
                    'sas.backend_2': [d_f],
                }
            )
            assert actual == expected
            if six.PY3:
                assert ('backend "(\'test_namespace_1\', \'sas.backend_1\')" is not resolved yet'
                        in actual.backends['sas.backend_1'].last_rev.v_message)
                assert ('backend "(\'test_namespace_1\', \'sas.backend_2\')" is not resolved yet'
                        in actual.backends['sas.backend_2'].last_rev.v_message)
            else:
                assert ('backend "(u\'test_namespace_1\', u\'sas.backend_1\')" is not resolved yet'
                        in actual.backends['sas.backend_1'].last_rev.v_message)
                assert ('backend "(u\'test_namespace_1\', u\'sas.backend_2\')" is not resolved yet'
                        in actual.backends['sas.backend_2'].last_rev.v_message)

            config_pb_ = Api.get_name_server_config(*full_name_server_id)
            expected_config_pb_ = api_pb2.GetNameServerConfigResponse(zone='in.yandex-team.ru')
            assert config_pb_ == expected_config_pb_

            actual = DnsRecordState.from_api(namespace_id_1, dns_record_2_id)
            expected = DnsRecordState(
                dns_record=[d_f],
                backends={
                    'sas.backend_2': [d_f],
                }
            )
            assert actual == expected
            if six.PY3:
                assert ('backend "(\'test_namespace_1\', \'sas.backend_2\')" is not resolved yet'
                        in actual.backends['sas.backend_2'].last_rev.v_message)
            else:
                assert ('backend "(u\'test_namespace_1\', u\'sas.backend_2\')" is not resolved yet'
                        in actual.backends['sas.backend_2'].last_rev.v_message)

    ctx.log.info('Step 2: let backends be resolved')
    backend_1_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='sas.backend_1')
    backend_1_ctl._process(ctx)
    backend_2_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='sas.backend_2')
    backend_2_ctl._process(ctx)

    for a in checker:
        with a:
            dns_record_1_ctl._process_empty_queue(ctx)
            dns_record_2_ctl._process_empty_queue(ctx)

            actual_dns_record_state = DnsRecordState.from_api(namespace_id_1, dns_record_1_id)
            expected_dns_record_state = DnsRecordState(
                dns_record=[d_t],
                backends={
                    'sas.backend_1': [d_t],
                    'sas.backend_2': [d_t],
                },
                endpoint_sets={
                    'sas.backend_1': [d_t],
                    'sas.backend_2': [d_t],
                }
            )
            assert actual_dns_record_state == expected_dns_record_state

            actual_dns_record_state = DnsRecordState.from_api(namespace_id_1, dns_record_2_id)
            expected_dns_record_state = DnsRecordState(
                dns_record=[d_t],
                backends={
                    'sas.backend_2': [d_t],
                },
                endpoint_sets={
                    'sas.backend_2': [d_t],
                }
            )
            assert actual_dns_record_state == expected_dns_record_state

            config_pb = Api.get_name_server_config(*full_name_server_id)
            expected_config_pb = api_pb2.GetNameServerConfigResponse(zone='in.yandex-team.ru')
            record_pb = expected_config_pb.records.add()
            record_pb.address.zone = 'netmon'
            record_pb.address.ipv6_addrs.add(value=':::1')
            record_pb = expected_config_pb.records.add()
            record_pb.address.zone = 'nanny'
            record_pb.address.ipv6_addrs.add(value=':::1')
            assert config_pb == expected_config_pb

    ctx.log.info('Step 3: check that DNS record state correctly '
                 'updates after backend is removed from spec (and new one is added)')
    dns_record = Api.get_dns_record(namespace_id=namespace_id_1, dns_record_id=dns_record_2_id)
    Api.update_dns_record(namespace_id=namespace_id_1,
                          dns_record_id=dns_record_2_id,
                          version=dns_record.meta.version,
                          spec_pb=make_dns_record_spec_pb(full_name_server_id=full_name_server_id,
                                                          backend_ids=['sas.backend_1'],
                                                          zone='nanny',
                                                          addressing_type=addressing_type))

    for a in checker:
        with a:
            dns_record_2_ctl._process_empty_queue(ctx)
            actual_dns_record_state = DnsRecordState.from_api(namespace_id=namespace_id_1,
                                                              dns_record_id=dns_record_2_id)
            expected_dns_record_state = DnsRecordState(
                dns_record=[d_t],
                backends={
                    'sas.backend_1': [d_t],
                },
                endpoint_sets={
                    'sas.backend_1': [d_t],
                }
            )
            assert actual_dns_record_state == expected_dns_record_state

    ctx.log.info('Step 4: check backend removal')
    if addressing_type == model_pb2.DnsBackendsSelector.EXPLICIT:
        backend_pb = Api.get_backend(namespace_id_1, 'sas.backend_1')
        backend_pb.spec.deleted = True
        Api.update_backend(namespace_id_1, 'sas.backend_1', backend_pb.meta.version, backend_pb.spec)

        for a in checker:
            with a:
                dns_record_1_ctl._process_empty_queue(ctx)
                backend_1_ctl._maybe_self_delete(ctx)
                assert Api.get_backend(namespace_id_1, 'sas.backend_1').spec.deleted

                actual_dns_record_state = DnsRecordState.from_api(namespace_id=namespace_id_1,
                                                                  dns_record_id=dns_record_1_id)
                expected_dns_record_state = DnsRecordState(
                    dns_record=[d_t],
                    backends={
                        'sas.backend_1': [d_t, d_f],
                        'sas.backend_2': [d_t]
                    },
                    endpoint_sets={
                        'sas.backend_1': [d_t],
                        'sas.backend_2': [d_t]
                    }
                )
                assert actual_dns_record_state == expected_dns_record_state

    dns_record = Api.get_dns_record(namespace_id=namespace_id_1, dns_record_id=dns_record_1_id)
    Api.update_dns_record(namespace_id=namespace_id_1,
                          dns_record_id=dns_record_1_id,
                          version=dns_record.meta.version,
                          spec_pb=make_dns_record_spec_pb(full_name_server_id=full_name_server_id,
                                                          backend_ids=['sas.backend_2'],
                                                          zone='netmon',
                                                          addressing_type=addressing_type))

    dns_record = Api.get_dns_record(namespace_id=namespace_id_1, dns_record_id=dns_record_2_id)
    Api.update_dns_record(namespace_id=namespace_id_1,
                          dns_record_id=dns_record_2_id,
                          version=dns_record.meta.version,
                          spec_pb=make_dns_record_spec_pb(full_name_server_id=full_name_server_id,
                                                          backend_ids=['sas.backend_2'],
                                                          zone='nanny',
                                                          addressing_type=addressing_type))

    for a in checker:
        with a:
            dns_record_1_ctl._process_empty_queue(ctx)
            dns_record_2_ctl._process_empty_queue(ctx)
            backend_1_ctl._maybe_self_delete(ctx)
            actual_dns_record_state = DnsRecordState.from_api(namespace_id=namespace_id_1,
                                                              dns_record_id=dns_record_1_id)
            expected_dns_record_state = DnsRecordState(
                dns_record=[d_t],
                backends={
                    'sas.backend_2': [d_t],
                },
                endpoint_sets={
                    'sas.backend_2': [d_t],
                }
            )
            assert actual_dns_record_state == expected_dns_record_state

            if addressing_type == model_pb2.DnsBackendsSelector.EXPLICIT:
                with pytest.raises(NotFoundError):
                    Api.get_backend(namespace_id_1, 'sas.backend_1')  # should be deleted


def test_l3_backend(zk_storage, cache, ctx, ctlrunner, caplog, dao, checker):
    dao.create_default_name_servers()
    full_name_server_id = ('infra', 'yandex-team.ru')
    wait_until_passes(lambda: cache.must_get_name_server(*full_name_server_id))

    namespace_id_1 = 'test_namespace_1'
    dns_record_1_id = 'test_dns_record_1'

    create_namespace(
        namespace_id=namespace_id_1,
        dns_record_ids=[dns_record_1_id],
        backend_ids=['sas.backend_1'],
        balancer_backend_ids=[],
        l3_balancer_ids=['l3_bal'],
        l3_balancers_params={'l3_bal': {'l3mgr_service_id': 'test'}},
        l3_balancer_backend_links={'l3_bal': ['sas.backend_1']},
        dns_record_backend_links={
            dns_record_1_id: ['l3_bal'],
        },
        dns_records_params={
            dns_record_1_id: {
                'full_name_server_id': full_name_server_id,
                'zone': 'l3l3',
            }
        },
    )

    for dns_record_pb in zk_storage.update_dns_record(namespace_id_1, dns_record_1_id):
        dns_record_pb.order.status.status = u'FINISHED'
        dns_record_pb.spec.incomplete = False
        dns_record_pb.spec.CopyFrom(make_dns_record_spec_pb(full_name_server_id=full_name_server_id,
                                                            backend_ids=['l3_bal'],
                                                            zone='netmon',
                                                            addressing_type=model_pb2.DnsBackendsSelector.L3_BALANCERS))
    wait_until(lambda: not cache.must_get_dns_record(namespace_id_1, dns_record_1_id).spec.incomplete)

    dns_record_1_ctl = DnsRecordCtl(namespace_id_1, dns_record_1_id)
    dns_record_1_ctl._init_processors()

    for a in checker:
        with a:
            dns_record_1_ctl._process_empty_queue(ctx)
            actual = DnsRecordState.from_api(namespace_id_1, dns_record_1_id)
            expected = DnsRecordState(
                dns_record=[d_t],
                backends=[]
            )
            assert actual == expected


def test_deadlines(ctx):
    ctl = BaseDnsRecordCtl(namespace_id='ns', dns_record_id='dns')
    ctl.FORCE_PROCESS_INTERVAL_JITTER = 0

    curr_time = monotonic.monotonic()
    assert ctl._should_process(curr_time)

    # test processing interval & deadline
    ctl.PROCESS_INTERVAL = 0.1
    ctl._reset_processing_timers()
    ctl._process_event(ctx, None)
    assert ctl._waiting_for_processing_since is not None
    assert ctl._processing_deadline is not None

    curr_time = ctl._waiting_for_processing_since
    assert not ctl._should_process(curr_time)
    curr_time += 0.11
    assert ctl._should_process(curr_time)

    # test force processing interval & deadline
    curr_time = monotonic.monotonic()
    ctl.FORCE_PROCESS_INTERVAL = 1
    ctl.PROCESS_INTERVAL = 999
    ctl._reset_processing_timers()
    assert ctl._processing_deadline is None
    assert ctl._force_processing_deadline is not None
    assert not ctl._should_process(curr_time)
    curr_time += 1.01
    assert ctl._should_process(curr_time)


@pytest.mark.parametrize(
    'addressing_type', (model_pb2.DnsBackendsSelector.BALANCERS, model_pb2.DnsBackendsSelector.EXPLICIT))
@mock.patch.object(BackendCtl, '_resolve', side_effect=ResolverStub())
def test_backend_restoration(_1, default_nses, zk_storage, cache, ctx, ctlrunner, caplog, dao, addressing_type, checker):
    # get not nameserver with not L3_ONLY backends
    name_server_pb = cache.must_get_name_server(
        namespace_id=INFRA_NAMESPACE_ID,
        name_server_id="in.yandex-team.ru")
    full_name_server_id = (name_server_pb.meta.namespace_id, name_server_pb.meta.id)

    namespace_id_1 = 'test_namespace_1'
    dns_record_id = 'test_dns_record_1'

    if addressing_type == model_pb2.DnsBackendsSelector.BALANCERS:
        balancer_backends = ['sas.backend_1', 'sas.backend_2']
        backends = []
    else:
        balancer_backends = []
        backends = ['sas.backend_1', 'sas.backend_2']

    create_namespace(
        namespace_id=namespace_id_1,
        dns_record_ids=[dns_record_id],
        backend_ids=backends,
        balancer_backend_ids=balancer_backends,
        l3_balancer_ids=[],
        dns_record_backend_links={
            dns_record_id: ['sas.backend_1', 'sas.backend_2'],
        },
        dns_records_params={
            dns_record_id: {
                'full_name_server_id': full_name_server_id,
                'zone': 'netmon',
            },
        },
    )
    backend_1_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='sas.backend_1')
    backend_1_ctl._process(ctx)
    backend_2_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='sas.backend_2')
    backend_2_ctl._process(ctx)
    dns_record_ctl = DnsRecordCtl(namespace_id_1, dns_record_id)
    dns_record_ctl._init_processors()

    ctx.log.info('Step 1: Both backends are fine')
    for a in checker:
        with a:
            dns_record_ctl._process_empty_queue(ctx)
            actual_dns_record_state = DnsRecordState.from_api(namespace_id_1, dns_record_id)
            expected_dns_record_state = DnsRecordState(
                dns_record=[d_t],
                backends={
                    'sas.backend_1': [d_t],
                    'sas.backend_2': [d_t],
                },
                endpoint_sets={
                    'sas.backend_1': [d_t],
                    'sas.backend_2': [d_t],
                }
            )
            assert actual_dns_record_state == expected_dns_record_state

    ctx.log.info('Step 2: Mark backend as deleted')
    backend_pb = Api.get_backend(namespace_id_1, 'sas.backend_1')
    backend_pb.spec.deleted = True
    for b_pb in zk_storage.update_backend(namespace_id_1, backend_pb.meta.id):
        b_pb.spec.deleted = True
    assert wait_until(lambda: cache.get_backend(namespace_id_1, backend_pb.meta.id).spec.deleted)

    for a in checker:
        with a:
            dns_record_ctl._process_empty_queue(ctx)
            actual_dns_record_state = DnsRecordState.from_api(namespace_id=namespace_id_1,
                                                              dns_record_id=dns_record_id)
            expected_dns_record_state = DnsRecordState(
                dns_record=[d_t],
                backends={
                    'sas.backend_1': [d_t],
                    'sas.backend_2': [d_t]
                },
                endpoint_sets={
                    'sas.backend_1': [d_t],
                    'sas.backend_2': [d_t]
                }
            )
            assert actual_dns_record_state == expected_dns_record_state

    ctx.log.info('Step 3: Restore backend')
    backend_pb.spec.deleted = False
    dao.update_backend(namespace_id_1, backend_pb.meta.id,
                       login=u'test',
                       comment=u'Restoring backend',
                       updated_spec_pb=backend_pb.spec,
                       version=backend_pb.meta.version,
                       allow_restoration=True)
    assert wait_until(lambda: not cache.get_backend(namespace_id_1, backend_pb.meta.id).spec.deleted)
    backend_1_ctl._process(ctx)

    for a in checker:
        with a:
            dns_record_ctl._process_empty_queue(ctx)
            actual_dns_record_state = DnsRecordState.from_api(namespace_id=namespace_id_1,
                                                              dns_record_id=dns_record_id)
            expected_dns_record_state = DnsRecordState(
                dns_record=[d_t],
                backends={
                    'sas.backend_1': [d_t],
                    'sas.backend_2': [d_t]
                },
                endpoint_sets={
                    'sas.backend_1': [d_t],
                    'sas.backend_2': [d_t]
                }
            )
            assert actual_dns_record_state == expected_dns_record_state

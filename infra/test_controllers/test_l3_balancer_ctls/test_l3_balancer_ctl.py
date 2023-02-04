# coding: utf-8
import contextlib
import copy
import logging

import inject
import mock
import monotonic
import pytest
import six
from six.moves import http_client as httplib

import awtest
from awacs.lib import l3mgrclient, nannyrpcclient
from awacs.lib.rpc.exceptions import NotFoundError
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.model.backend import BackendCtl as BaseBackendCtl
from awacs.model.backend.ctl import Resolution
from awacs.model.l3_balancer import l3_balancer
from awacs.model.l3_balancer.cacheutil import skip_in_progress_l3mgr_config
from awacs.model.l3_balancer.ctl import L3BalancerCtl as BaseL3BalancerCtl
from awacs.model.l3_balancer.ctl_v2 import L3BalancerCtlV2 as BaseL3BalancerCtlV2
from awacs.model.l3_balancer.errors import L3BalancerTransportError
from awacs.model.l3_balancer.l3_balancer import L3BalancerStateHandler
from awacs.web import validation
from awtest import wait_until_passes, check_log
from awtest.api import Api
from awtest.l3util import L3State, f_f_f, t_f_f, t_t_f, t_t_t, t_f_t, u_f_f
from infra.awacs.proto import model_pb2, internals_pb2
from infra.swatlib.auth import abc


NS_ID = 'test_namespace_1'
BALANCER_ID = 'test_balancer_1_sas'

L7_MACRO_BALANCER_YML = """
l7_macro:
  version: 0.1.0
  http: {}
"""


class BackendCtl(BaseBackendCtl):
    PROCESS_INTERVAL = 9999
    EVENTS_QUEUE_GET_TIMEOUT = 9999
    SELF_DELETION_COOLDOWN_PERIOD = 0.01
    SELF_DELETION_CHECK_INTERVAL = 0.01

    def _fill_snapshot_ids(self, selector_pb):
        for nanny_snapshot_pb in selector_pb.nanny_snapshots:
            nanny_snapshot_pb.snapshot_id = 'xxx'

    def mock_process(self, ctx, instance_pbs):
        with mock.patch.object(self, '_resolve',
                               return_value=Resolution(instance_pbs=instance_pbs, yp_sd_timestamps={})):
            self._process(ctx)
        wait_until_passes(lambda: Api.get_endpoint_set(self._namespace_id, self._backend_id))


class L3BalancerCtl(BaseL3BalancerCtl):
    POLL_INTERVAL = 9999
    PROCESS_INTERVAL = 9999
    EVENTS_QUEUE_GET_TIMEOUT = 9999
    FORCE_PROCESS_INTERVAL = 9999
    FORCE_PROCESS_INTERVAL_JITTER = 0

    def _should_poll(self, curr_time):
        return True

    def _should_process(self, curr_time):
        return True


class L3BalancerCtlV2(BaseL3BalancerCtlV2):
    POLL_INTERVAL = 9999
    PROCESS_INTERVAL = 9999
    EVENTS_QUEUE_GET_TIMEOUT = 9999
    FORCE_PROCESS_INTERVAL = 9999
    FORCE_PROCESS_INTERVAL_JITTER = 0

    def _should_poll(self, curr_time):
        return True

    def _should_process(self, curr_time):
        return True


def get_in_progress_full_config_ids(ctl, state_pb):
    if isinstance(ctl, L3BalancerCtl):
        return ctl._transport._get_in_progress_full_config_ids(state_pb)
    else:
        l3mgr_config_pb = l3_balancer.L3BalancerStateHandler(state_pb).get_in_progress_l3mgr_config_pb()
        return {(l3mgr_config_pb.service_id, l3mgr_config_pb.config_id): l3mgr_config_pb.ctime.ToMicroseconds()}


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, l3_mgr_client, caplog, yp_lite_mock_client, nanny_rpc_mock_client, abc_client):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(abc.IAbcClient, abc_client)
        b.bind(l3mgrclient.IL3MgrClient, l3_mgr_client)
        b.bind(nannyrpcclient.INannyRpcClient, nanny_rpc_mock_client)
        b.bind(IYpLiteRpcClient, yp_lite_mock_client)
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def make_l3_balancer_spec_pb(l3mgr_service_id, backend_ids, addressing_type, allow_foreign_rs=False,
                             virtual_servers=None, ctl_version=1):
    """
    :type l3mgr_service_id: six.text_type
    :type backend_ids: list[six.text_type]
    :type addressing_type: model_pb2.L3BalancerRealServersSelector.Type
    :type allow_foreign_rs: bool
    :type virtual_servers: list[model_pb2.L3BalancerSpec.VirtualServer]
    :type ctl_version: int
    :rtype: models_pb2.L3BalancerSpec
    """
    spec_pb = model_pb2.L3BalancerSpec()
    spec_pb.l3mgr_service_id = l3mgr_service_id
    spec_pb.ctl_version = ctl_version
    if ctl_version >= 2:
        spec_pb.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS
    spec_pb.real_servers.type = addressing_type
    spec_pb.preserve_foreign_real_servers = allow_foreign_rs
    for id_ in backend_ids:
        if addressing_type == model_pb2.L3BalancerRealServersSelector.BALANCERS:
            spec_pb.real_servers.balancers.add(id=id_)
        else:
            spec_pb.real_servers.backends.add(id=id_)
    if virtual_servers:
        for vs in virtual_servers:
            spec_pb.virtual_servers.add().CopyFrom(vs)
    return spec_pb


def make_backend_spec_pb(nanny_service_ids):
    """
    :type nanny_service_ids: list[six.text_type]
    :rtype: models_pb2.BackendSpec
    """
    backend_spec_pb = model_pb2.BackendSpec()
    backend_spec_pb.selector.type = backend_spec_pb.selector.NANNY_SNAPSHOTS
    for service_id in nanny_service_ids:
        backend_spec_pb.selector.nanny_snapshots.add(service_id=service_id)
    return backend_spec_pb


def make_balancers_backend_spec_pb(balancer_id):
    """
    :type balancer_id: six.text_type
    :rtype: models_pb2.BackendSpec
    """
    backend_spec_pb = model_pb2.BackendSpec()
    backend_spec_pb.selector.type = backend_spec_pb.selector.BALANCERS
    backend_spec_pb.selector.balancers.add(id=balancer_id)
    return backend_spec_pb


@contextlib.contextmanager
def unpaused_l3_balancer_transport(namespace_id, l3_balancer_id):
    Api.unpause_l3_balancer_transport(namespace_id, l3_balancer_id)
    yield
    Api.pause_l3_balancer_transport(namespace_id, l3_balancer_id)


def create_namespace(namespace_id, l3_balancer_ids, backend_ids, l3_balancer_backend_links, balancer_backend_ids,
                     allow_foreign_rs=False, l3_vs=None, l3_ctl_version=1):
    """
    :type namespace_id: six.text_type
    :type l3_balancer_ids: list[six.text_type]
    :type backend_ids: list[six.text_type]
    :type balancer_backend_ids: list[six.text_type]
    :type l3_balancer_backend_links: dict[six.text_type, list[six.text_type]]
    :type allow_foreign_rs: bool
    :type l3_vs: list[model_pb2.L3BalancerSpec.VirtualServer]
    :type l3_ctl_version: int
    """
    assert not (balancer_backend_ids and backend_ids)
    if balancer_backend_ids:
        addressing_type = model_pb2.L3BalancerRealServersSelector.BALANCERS
    else:
        addressing_type = model_pb2.L3BalancerRealServersSelector.BACKENDS

    Api.create_namespace(namespace_id=namespace_id)

    for i, backend_id in enumerate(backend_ids):
        backend_spec_pb = make_backend_spec_pb(nanny_service_ids=['service_{}'.format(i)])
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
        l3_balancer_spec_pb = make_l3_balancer_spec_pb(
            l3mgr_service_id=l3_balancer_id,
            backend_ids=l3_balancer_backend_links.get(l3_balancer_id, []),
            addressing_type=addressing_type,
            allow_foreign_rs=allow_foreign_rs,
            virtual_servers=l3_vs,
            ctl_version=l3_ctl_version,
        )
        Api.create_l3_balancer(namespace_id=namespace_id, l3_balancer_id=l3_balancer_id, spec_pb=l3_balancer_spec_pb)
        Api.pause_l3_balancer_transport(namespace_id, l3_balancer_id)


def relink_l3_balancer(namespace_id, l3_balancer_id, backend_ids, allow_foreign_rs=False):
    """
    :type namespace_id: six.text_type
    :type l3_balancer_id: six.text_type
    :type backend_ids: list[six.text_type]
    :type allow_foreign_rs: bool
    """
    l3_balancer_pb = Api.get_l3_balancer(namespace_id=namespace_id, l3_balancer_id=l3_balancer_id)
    version = l3_balancer_pb.meta.version
    l3_balancer_spec_pb = l3_balancer_pb.spec
    pb = make_l3_balancer_spec_pb(l3mgr_service_id=l3_balancer_id, backend_ids=backend_ids,
                                  addressing_type=l3_balancer_pb.spec.real_servers.type,
                                  allow_foreign_rs=allow_foreign_rs)
    l3_balancer_spec_pb.real_servers.CopyFrom(pb.real_servers)

    with mock.patch.object(validation.l3_balancer, 'validate_request'):
        return Api.update_l3_balancer(namespace_id=namespace_id, l3_balancer_id=l3_balancer_id,
                                      version=version, spec_pb=l3_balancer_spec_pb)


def assert_l3_state(checker, expected_state, ns_id=NS_ID, l3_balancer_id=BALANCER_ID, l3_cfg_pb=None):
    for a in checker:
        with a:
            actual_state = L3State.from_api(namespace_id=ns_id, l3_balancer_id=l3_balancer_id)
            assert actual_state == expected_state
            if l3_cfg_pb is not None:
                h = L3BalancerStateHandler(actual_state.pb)
                for ver, rev_pb in h.iter_in_progress_versions_and_rev_pbs():
                    actual_cfg_pb = rev_pb.in_progress.meta.l3mgr.configs[0]
                    assert actual_cfg_pb.service_id == l3_cfg_pb.service_id, ver
                    assert actual_cfg_pb.config_id == l3_cfg_pb.config_id, ver
            return actual_state


@pytest.mark.parametrize('addressing_type',
                         (
                             model_pb2.L3BalancerRealServersSelector.BALANCERS,
                             model_pb2.L3BalancerRealServersSelector.BACKENDS,
                         ))
def test_ctl_v1_transport_rs(ctx, l3_mgr_client, cache, checker, dao, addressing_type):
    l3_mgr_client.awtest_set_default_config()
    if addressing_type == model_pb2.L3BalancerRealServersSelector.BALANCERS:
        balancer_backends = ['sas.backend_1', 'sas.backend_2', 'sas.backend_3', 'sas.backend_4']
        backends = []
    else:
        balancer_backends = []
        backends = ['sas.backend_1', 'sas.backend_2', 'sas.backend_3', 'sas.backend_4']
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=backends,
        balancer_backend_ids=balancer_backends,
        l3_balancer_backend_links={
            BALANCER_ID: ['sas.backend_1']
        }
    )

    ctl = L3BalancerCtl(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)

    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[u_f_f],
                backends={
                    'sas.backend_1': [f_f_f]
                },
            )
            assert actual_state == expected_state

    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='ya.ru', port=80, weight=1, ipv4_addr='127.0.0.1', ipv6_addr='::1'),
    ])

    ctx.log.info('Step 1: resolved backend, start transporting')
    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                actual_state = L3State.from_pb(state_pb)
                expected_state = L3State(
                    balancer=[t_t_f],
                    backends={
                        'sas.backend_1': [t_t_f],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_t_f],
                    }
                )
                assert actual_state == expected_state
    assert get_in_progress_full_config_ids(ctl, state_pb) == {('test_balancer_1_sas', '1'): 1517553426846000}
    l3_mgr_client.create_config_with_rs.assert_called_once_with(groups=[u'ya.ru=::1'],
                                                                svc_id=BALANCER_ID,
                                                                use_etag=False)
    l3_mgr_client.awtest_reset_mocks()

    for vs in l3_mgr_client.vs:
        vs[u'group'] = ['...']

    ctx.log.info('Step 2: config is marked as active in L3mgr')
    l3_mgr_client.awtest_activate_config(1)
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t],
                backends={
                    'sas.backend_1': [t_f_t],
                },
                endpoint_sets={
                    'sas.backend_1': [t_f_t],
                },
            )
            assert actual_state == expected_state

    ctx.log.info('Step 3: add 2 new backends')
    l3_balancer_pb = Api.get_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    l3_balancer_pb.spec.use_endpoint_weights = True
    Api.update_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID,
                           version=l3_balancer_pb.meta.version, spec_pb=l3_balancer_pb.spec)

    relink_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID,
                       backend_ids=['sas.backend_2', 'sas.backend_3'])

    ctx.log.info('Step 4: discover new backends')
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t, u_f_f],
                backends={
                    'sas.backend_1': [t_f_t],
                    'sas.backend_2': [u_f_f],
                    'sas.backend_3': [u_f_f],
                },
                endpoint_sets={
                    'sas.backend_1': [t_f_t]
                },
            )
            assert actual_state == expected_state

    ctx.log.info('Step 5: validate new backends')
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t, f_f_f],
                backends={
                    'sas.backend_1': [t_f_t],
                    'sas.backend_2': [f_f_f],
                    'sas.backend_3': [f_f_f],
                },
                endpoint_sets={
                    'sas.backend_1': [t_f_t],
                },
            )
            assert actual_state == expected_state

    ctx.log.info('Step 6: resolve backend_2 and validate -> transport config')
    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_2').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='google.com', port=80, weight=1, ipv4_addr='127.0.0.2', ipv6_addr='::2'),
    ])

    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                expected_state = L3State(
                    balancer=[t_t_t, f_f_f],
                    backends={
                        'sas.backend_1': [t_t_t],
                        'sas.backend_2': [t_t_f],
                        'sas.backend_3': [f_f_f],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_t_t],
                        'sas.backend_2': [t_t_f],
                    },
                )
                assert actual_state == expected_state
                for status_pb in (
                    actual_state.pb.l3_balancer.l3_statuses[-2],
                    actual_state.pb.backends['sas.backend_1'].l3_statuses[-1],
                    actual_state.pb.backends['sas.backend_2'].l3_statuses[-1],
                    actual_state.pb.endpoint_sets['sas.backend_1'].l3_statuses[-1],
                    actual_state.pb.endpoint_sets['sas.backend_2'].l3_statuses[-1],
                ):
                    assert status_pb.in_progress.meta.l3mgr.configs[-1].config_id == '2'

    l3_mgr_client.create_config_with_rs.assert_called_with(groups=[u'ya.ru=::1'], svc_id=BALANCER_ID, use_etag=False)
    l3_mgr_client.awtest_reset_mocks()

    ctx.log.info('Step 7: new config is active in L3mgr')
    l3_mgr_client.awtest_activate_config(2)
    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        ctl._process_empty_queue(ctx)
        for a in checker:
            with a:
                actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                expected_state = L3State(
                    balancer=[t_f_t, f_f_f],
                    backends={
                        'sas.backend_1': [t_f_t],
                        'sas.backend_2': [t_f_t],
                        'sas.backend_3': [f_f_f],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_f_t],
                        'sas.backend_2': [t_f_t],
                    },
                )
                assert actual_state == expected_state

    ctx.log.info('Step 8: mark backend_1 as deleted, resolve backend_3, then discover -> validate')
    backend_pb = Api.get_backend(NS_ID, 'sas.backend_1')
    backend_pb.spec.deleted = True
    dao.update_backend(NS_ID, 'sas.backend_1', login=Api.TEST_LOGIN, comment='-',
                       updated_spec_pb=backend_pb.spec, version=backend_pb.meta.version)

    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_3').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='mail.ru', port=80, weight=100.5, ipv4_addr='127.0.0.3', ipv6_addr='::3'),
        internals_pb2.Instance(host='mail.com', port=80, weight=-25, ipv4_addr='127.0.0.4'),
    ])

    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t, t_f_f],
                backends={
                    'sas.backend_1': [t_f_t, t_f_f],
                    'sas.backend_2': [t_f_t],
                    'sas.backend_3': [t_f_f],
                },
                endpoint_sets={
                    'sas.backend_1': [t_f_t],
                    'sas.backend_2': [t_f_t],
                    'sas.backend_3': [t_f_f],
                },
            )
            assert actual_state == expected_state

    ctx.log.info('Step 9: transport new config')
    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                expected_state = L3State(
                    balancer=[t_f_t, t_t_f],
                    backends={
                        'sas.backend_1': [t_f_t, t_t_f],
                        'sas.backend_2': [t_t_t],
                        'sas.backend_3': [t_t_f],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_t_t],
                        'sas.backend_2': [t_t_t],
                        'sas.backend_3': [t_t_f],
                    },
                )
                assert actual_state == expected_state

    l3_mgr_client.create_config_with_rs.assert_called_with(groups=[u'google.com=::2 weight=1',
                                                                   u'mail.com=127.0.0.4 weight=0',
                                                                   u'mail.ru=::3 weight=100'],
                                                           svc_id=BALANCER_ID,
                                                           use_etag=False)

    ctx.log.info('Step 10: new config is active in L3mgr')
    l3_mgr_client.awtest_activate_config(3)
    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                expected_state = L3State(
                    balancer=[t_f_t],
                    backends={
                        'sas.backend_2': [t_f_t],
                        'sas.backend_3': [t_f_t],
                    },
                    endpoint_sets={
                        'sas.backend_2': [t_f_t],
                        'sas.backend_3': [t_f_t],
                    },
                )
                assert actual_state == expected_state
                Api.get_backend(NS_ID, 'sas.backend_1')

    backend_ctl = BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_1')

    ctx.log.info('Step 11: backend_1 should be deleted')
    for a in checker:
        with a:
            backend_ctl._maybe_self_delete(ctx)
            with awtest.raises(NotFoundError):
                Api.get_backend(NS_ID, 'sas.backend_1')

    l3_balancer_pb = relink_l3_balancer(NS_ID, BALANCER_ID, ['sas.backend_3'])
    relinked_l3_balancer_version = l3_balancer_pb.meta.version

    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t, t_f_f],
                backends={
                    'sas.backend_2': [t_f_t],
                    'sas.backend_3': [t_f_t],
                },
                endpoint_sets={
                    'sas.backend_2': [t_f_t],
                    'sas.backend_3': [t_f_t],
                },
            )
            assert actual_state == expected_state

    ctx.log.info('Step 12: delete backend_2')
    backend_pb = Api.get_backend(NS_ID, 'sas.backend_2')
    backend_pb.spec.deleted = True
    dao.update_backend(NS_ID, 'sas.backend_2', login=Api.TEST_LOGIN, comment='-',
                       updated_spec_pb=backend_pb.spec, version=backend_pb.meta.version)

    ctx.log.info('Step 13: discover -> validate -> transport new config')
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t, t_f_f],
                backends={
                    'sas.backend_2': [t_f_t],
                    'sas.backend_3': [t_f_t],
                },
                endpoint_sets={
                    'sas.backend_2': [t_f_t],
                    'sas.backend_3': [t_f_t],
                },
            )
            assert actual_state == expected_state

    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                expected_state = L3State(
                    balancer=[t_f_t, t_t_f],
                    backends={
                        'sas.backend_2': [t_t_t],
                        'sas.backend_3': [t_t_t],
                    },
                    endpoint_sets={
                        'sas.backend_2': [t_t_t],
                        'sas.backend_3': [t_t_t],
                    },
                )
                assert actual_state == expected_state

    ctx.log.info('Step 14: new config is active in L3mgr')
    l3_mgr_client.awtest_activate_config(4)
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t],
                backends={
                    'sas.backend_3': [t_f_t],
                },
                endpoint_sets={
                    'sas.backend_3': [t_f_t],
                },
            )
            assert actual_state.pb.l3_balancer.l3_statuses[0].revision_id == relinked_l3_balancer_version
            assert actual_state == expected_state

    if addressing_type == model_pb2.L3BalancerRealServersSelector.BACKENDS:
        l3_balancer_pb = relink_l3_balancer(NS_ID, BALANCER_ID, ['sas.backend_1'])
        relinked_l3_balancer_version = l3_balancer_pb.meta.version

        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                expected_state = L3State(
                    balancer=[t_f_t, f_f_f],
                    backends={
                        'sas.backend_3': [t_f_t],
                    },
                    endpoint_sets={
                        'sas.backend_3': [t_f_t],
                    },
                )
                assert actual_state == expected_state

    if addressing_type == model_pb2.L3BalancerRealServersSelector.BALANCERS:
        backend_spec_pb = make_balancers_backend_spec_pb(balancer_id='sas.backend_1')
    else:
        backend_spec_pb = make_backend_spec_pb(nanny_service_ids=['service_1'])

    Api.create_backend(namespace_id=NS_ID, backend_id='sas.backend_1', spec_pb=backend_spec_pb,
                       is_system=addressing_type == model_pb2.L3BalancerRealServersSelector.BALANCERS)
    wait_until_passes(lambda: cache.must_get_backend(namespace_id=NS_ID, backend_id='sas.backend_1'))

    if addressing_type == model_pb2.L3BalancerRealServersSelector.BALANCERS:
        l3_balancer_pb = relink_l3_balancer(NS_ID, BALANCER_ID, ['sas.backend_1'])
        relinked_l3_balancer_version = l3_balancer_pb.meta.version

    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t, f_f_f],
                backends={
                    'sas.backend_1': [f_f_f],
                    'sas.backend_3': [t_f_t],
                },
                endpoint_sets={
                    'sas.backend_3': [t_f_t],
                },
            )
            assert actual_state == expected_state

    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='mail.ru', port=80, weight=1, ipv4_addr='127.0.0.3', ipv6_addr='::3'),
    ])

    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                expected_state = L3State(
                    balancer=[t_f_t, t_t_f],
                    backends={
                        'sas.backend_1': [t_t_f],
                        'sas.backend_3': [t_t_t],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_t_f],
                        'sas.backend_3': [t_t_t],
                    },
                )
                assert actual_state == expected_state

    l3_mgr_client.awtest_activate_config(5)
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t],
                backends={
                    'sas.backend_1': [t_f_t],
                },
                endpoint_sets={
                    'sas.backend_1': [t_f_t],
                },
            )
            assert actual_state.pb.l3_balancer.l3_statuses[0].revision_id == relinked_l3_balancer_version
            assert actual_state == expected_state


def test_ctl_v2_transport_vs_and_rs(ctx, l3_mgr_client, cache, checker, dao):
    vs1 = model_pb2.L3BalancerSpec.VirtualServer(ip=u'127.0.0.1', port=80)
    vs1.traffic_type = vs1.TT_EXTERNAL
    vs1.health_check_settings.url = u'/ping1'
    vs1.health_check_settings.check_type = vs1.health_check_settings.CT_HTTP_GET
    vs2 = model_pb2.L3BalancerSpec.VirtualServer(ip=u'::1', port=443)
    vs2.traffic_type = vs2.TT_EXTERNAL
    vs2.health_check_settings.url = u'/ping2'
    vs2.health_check_settings.check_type = vs1.health_check_settings.CT_SSL_GET
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=[],
        balancer_backend_ids=[u'sas.backend_1'],
        l3_balancer_backend_links={BALANCER_ID: [u'sas.backend_1']},
        l3_vs=[vs1, vs2],
        l3_ctl_version=2,
    )
    Api.unpause_l3_balancer_transport(NS_ID, BALANCER_ID)
    BackendCtl(namespace_id=NS_ID, backend_id=u'sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host=u'ya.ru', port=80, weight=1, ipv4_addr=u'127.0.0.1', ipv6_addr=u'::1'),
    ])
    ctl = L3BalancerCtlV2(NS_ID, BALANCER_ID)

    ctx.log.info(u'Step 1: Make first valid config in L3mgr')
    ctx.log.info(u'Step 1.1: discovery')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[u_f_f],
                                     backends={u'sas.backend_1': [u_f_f]},
                                     endpoint_sets={u'sas.backend_1': [u_f_f]}))

    ctx.log.info(u'Step 1.2: validation')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_f],
                                     backends={u'sas.backend_1': [t_f_f]},
                                     endpoint_sets={u'sas.backend_1': [t_f_f]}))

    ctx.log.info(u'Step 1.3: transport VS + RS')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_t_f],
                                     backends={u'sas.backend_1': [t_t_f]},
                                     endpoint_sets={u'sas.backend_1': [t_t_f]}),
                    l3_cfg_pb=model_pb2.L3mgrConfig(service_id=u'test_balancer_1_sas',
                                                    config_id=u'1'))
    l3_mgr_client.create_config_with_vs.assert_called_with(comment=u'Add virtual servers',
                                                           svc_id=u'test_balancer_1_sas',
                                                           vs_ids=[1, 2],
                                                           use_etag=False)
    l3_mgr_client.create_config_with_rs.assert_not_called()
    assert l3_mgr_client.create_virtual_server.call_count == 2
    vs1_kwargs = l3_mgr_client.create_virtual_server.call_args_list[0][1]
    vs2_kwargs = l3_mgr_client.create_virtual_server.call_args_list[1][1]
    assert vs1_kwargs == {u'svc_id': u'test_balancer_1_sas',
                          u'protocol': u'TCP',
                          u'config': {u'CHECK_URL': u'/ping1',
                                      u'CHECK_TYPE': u'HTTP_GET',
                                      u'DC_FILTER': True,
                                      u'DYNAMICWEIGHT': True,
                                      u'METHOD': u'TUN',
                                      u'CONNECT_PORT': 80,
                                      u'DYNAMICWEIGHT_RATIO': 30,
                                      u'SCHEDULER': u'wrr',
                                      u'STATUS_CODE': 200,
                                      u'ANNOUNCE': True,
                                      u'DYNAMICWEIGHT_ALLOW_ZERO': True},
                          u'port': 80,
                          u'groups': [u'ya.ru=::1'],
                          u'ip': u'127.0.0.1'}
    assert vs2_kwargs == {u'svc_id': u'test_balancer_1_sas',
                          u'protocol': u'TCP',
                          u'config': {u'CHECK_URL': u'/ping2',
                                      u'CHECK_TYPE': u'SSL_GET',
                                      u'DC_FILTER': True,
                                      u'DYNAMICWEIGHT': True,
                                      u'METHOD': u'TUN',
                                      u'CONNECT_PORT': 443,
                                      u'DYNAMICWEIGHT_RATIO': 30,
                                      u'SCHEDULER': u'wrr',
                                      u'STATUS_CODE': 200,
                                      u'ANNOUNCE': True,
                                      u'DYNAMICWEIGHT_ALLOW_ZERO': True},
                          u'port': 443,
                          u'groups': [u'ya.ru=::1'],
                          u'ip': u'::1'}
    l3_mgr_client.awtest_reset_mocks()

    ctx.log.info(u'Step 1.4: update VS once again, make sure new vector is not being processed')
    l3_pb = Api.get_l3_balancer(NS_ID, BALANCER_ID)
    vs = l3_pb.spec.virtual_servers.add(ip=u'168.0.0.9', port=222)
    vs.traffic_type = vs.TT_INTERNAL
    vs.health_check_settings.url = u'/ping0'
    vs.health_check_settings.check_type = vs.health_check_settings.CT_HTTP_GET
    Api.update_l3_balancer(NS_ID, BALANCER_ID, version=l3_pb.meta.version, spec_pb=l3_pb.spec)
    ctl._process_empty_queue(ctx)  # discover
    ctl._process_empty_queue(ctx)  # validate
    ctl._process_empty_queue(ctx)  # transport
    assert_l3_state(checker, L3State(balancer=[t_t_f, t_f_f],
                                     backends={u'sas.backend_1': [t_t_f]},
                                     endpoint_sets={u'sas.backend_1': [t_t_f]}),
                    l3_cfg_pb=model_pb2.L3mgrConfig(service_id=u'test_balancer_1_sas',
                                                    config_id=u'1'))
    l3_mgr_client.create_virtual_server.assert_not_called()
    l3_mgr_client.create_config_with_vs.assert_not_called()
    l3_mgr_client.create_config_with_rs.assert_not_called()
    l3_mgr_client.awtest_reset_mocks()

    ctx.log.info(u'Step 1.6: poll to activate vector')
    l3_mgr_client.awtest_activate_config(1)
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t, t_f_f],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}))
    l3_mgr_client.create_virtual_server.assert_not_called()
    l3_mgr_client.create_config_with_rs.assert_not_called()
    l3_mgr_client.awtest_reset_mocks()

    ctx.log.info(u'Step 1.7: now spec from 1.5 should be transported')
    ctl._process_empty_queue(ctx)  # discover
    ctl._process_empty_queue(ctx)  # validate
    ctl._process_empty_queue(ctx)  # transport
    assert_l3_state(checker, L3State(balancer=[t_f_t, t_t_f],
                                     backends={u'sas.backend_1': [t_t_t]},
                                     endpoint_sets={u'sas.backend_1': [t_t_t]}),
                    l3_cfg_pb=model_pb2.L3mgrConfig(service_id=u'test_balancer_1_sas',
                                                    config_id=u'2'))
    l3_mgr_client.create_virtual_server.assert_called_with(
        config={u'CHECK_URL': u'/ping0', u'CHECK_TYPE': 'HTTP_GET', u'DC_FILTER': True,
                u'DYNAMICWEIGHT': True, u'METHOD': u'TUN', u'CONNECT_PORT': 222, u'DYNAMICWEIGHT_RATIO':
                    30, u'SCHEDULER': u'wrr', u'STATUS_CODE': 200, u'ANNOUNCE': True,
                u'DYNAMICWEIGHT_ALLOW_ZERO': True},
        ip=u'168.0.0.9',
        port=222,
        groups=[u'ya.ru=::1'],
        protocol=u'TCP',
        svc_id=u'test_balancer_1_sas')
    l3_mgr_client.create_config_with_rs.assert_not_called()
    l3_mgr_client.awtest_reset_mocks()

    ctx.log.info(u'Step 1.8: activate')
    l3_mgr_client.awtest_activate_config(2)
    ctl._process_empty_queue(ctx)  # poll (still in progress)
    ctl._process_empty_queue(ctx)  # discover -> validate -> transport -> poll (activate)
    assert_l3_state(checker, L3State(balancer=[t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}),
                    l3_cfg_pb=model_pb2.L3mgrConfig(service_id=u'test_balancer_1_sas',
                                                    config_id=u'2'))

    ctx.log.info(u'Step 2: update only real servers')
    BackendCtl(namespace_id=NS_ID, backend_id=u'sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host=u'ya.ru', port=80, weight=1, ipv4_addr=u'127.0.0.2', ipv6_addr=u'::2'),
    ])
    ctx.log.info(u'Step 2.1: discovery')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t, u_f_f]}))

    ctx.log.info(u'Step 2.2: validation')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t, t_f_f]}))

    ctx.log.info(u'Step 2.3: transport')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker,
                    L3State(balancer=[t_t_t],
                            backends={u'sas.backend_1': [t_t_t]},
                            endpoint_sets={u'sas.backend_1': [t_f_t, t_t_f]}),
                    l3_cfg_pb=model_pb2.L3mgrConfig(service_id=u'test_balancer_1_sas',
                                                    config_id=u'3'))
    l3_mgr_client.awtest_reset_mocks()

    ctx.log.info(u'Step 2.4: config is active, vector is active')
    l3_mgr_client.awtest_activate_config(3)
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t, t_f_t]}))
    l3_mgr_client.create_virtual_server.assert_not_called()
    l3_mgr_client.create_config_with_rs.assert_not_called()

    ctx.log.info(u'Step 2.5: cleanup old versions')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}))
    l3_mgr_client.create_virtual_server.assert_not_called()
    l3_mgr_client.create_config_with_rs.assert_not_called()

    ctx.log.info(u'Step 3: update only VS')
    l3_pb = Api.get_l3_balancer(NS_ID, BALANCER_ID)
    vs = l3_pb.spec.virtual_servers.add(ip=u'168.0.0.10', port=333)
    vs.traffic_type = vs.TT_EXTERNAL
    vs.health_check_settings.url = u'/ping3'
    vs.health_check_settings.check_type = vs1.health_check_settings.CT_SSL_GET
    Api.update_l3_balancer(NS_ID, BALANCER_ID, version=l3_pb.meta.version, spec_pb=l3_pb.spec)
    ctx.log.info(u'Step 3.1: discovery')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t, u_f_f],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}))

    ctx.log.info(u'Step 3.2: validation')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t, t_f_f],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}))

    ctx.log.info(u'Step 3.3: transport')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t, t_t_f],
                                     backends={u'sas.backend_1': [t_t_t]},
                                     endpoint_sets={u'sas.backend_1': [t_t_t]}),
                    l3_cfg_pb=model_pb2.L3mgrConfig(service_id=u'test_balancer_1_sas',
                                                    config_id=u'4'))
    l3_mgr_client.create_virtual_server.assert_called_with(
        config={u'CHECK_URL': u'/ping3', u'CHECK_TYPE': 'SSL_GET', u'DC_FILTER': True,
                u'DYNAMICWEIGHT': True, u'METHOD': u'TUN', u'CONNECT_PORT': 333, u'DYNAMICWEIGHT_RATIO':
                    30, u'SCHEDULER': u'wrr', u'STATUS_CODE': 200, u'ANNOUNCE': True,
                u'DYNAMICWEIGHT_ALLOW_ZERO': True},
        ip=u'168.0.0.10',
        port=333,
        groups=[u'ya.ru=::2'],
        protocol=u'TCP',
        svc_id=u'test_balancer_1_sas')
    l3_mgr_client.create_config_with_rs.assert_not_called()
    l3_mgr_client.awtest_reset_mocks()
    for vs in l3_mgr_client.vs:
        vs[u'group'] = [u'ya.ru=::2']  # in prod this is done by L3mgr

    ctx.log.info(u'Step 3.4: config is active, vector is active')
    l3_mgr_client.awtest_activate_config(4)
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t, t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}),
                    l3_cfg_pb=model_pb2.L3mgrConfig(service_id=u'test_balancer_1_sas',
                                                    config_id=u'4'))
    l3_mgr_client.create_virtual_server.assert_not_called()
    l3_mgr_client.create_config_with_rs.assert_not_called()

    ctx.log.info(u'Step 3.5: cleanup old versions')
    ctl._process_empty_queue(ctx)
    assert_l3_state(checker, L3State(balancer=[t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}))
    l3_mgr_client.create_virtual_server.assert_not_called()
    l3_mgr_client.create_config_with_rs.assert_not_called()


def test_ctl_v2_no_infinite_configs(ctx, l3_mgr_client, cache, checker):
    vs = model_pb2.L3BalancerSpec.VirtualServer(ip=u'127.0.0.1', port=80)
    vs.traffic_type = vs.TT_INTERNAL
    vs.health_check_settings.url = u'/ping1'
    vs.health_check_settings.check_type = vs.health_check_settings.CT_HTTP_GET
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=[u'sas.backend_1'],
        balancer_backend_ids=[],
        l3_balancer_backend_links={
            BALANCER_ID: [u'sas.backend_1']
        },
        l3_vs=[vs],
        l3_ctl_version=2,
    )
    Api.unpause_l3_balancer_transport(NS_ID, BALANCER_ID)

    ctl = L3BalancerCtlV2(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    BackendCtl(namespace_id=NS_ID, backend_id=u'sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host=u'ya.ru', port=80, weight=1, ipv4_addr=u'127.0.0.1', ipv6_addr=u'::1'),
    ])

    ctx.log.info(u'Step 1. Transport (creates new L3mgr config)')
    ctl._process_empty_queue(ctx)  # discover
    ctl._process_empty_queue(ctx)  # validate
    ctl._process_empty_queue(ctx)  # transport
    assert_l3_state(checker,
                    L3State(balancer=[t_t_f],
                            backends={u'sas.backend_1': [t_t_f]},
                            endpoint_sets={u'sas.backend_1': [t_t_f]}),
                    l3_cfg_pb=model_pb2.L3mgrConfig(service_id=u'test_balancer_1_sas',
                                                    config_id=u'1'))
    l3_mgr_client.create_virtual_server.assert_called_once()
    l3_mgr_client.create_config_with_vs.assert_called_once()
    l3_mgr_client.awtest_reset_mocks()

    ctx.log.info(u'Step 2. Make sure that repeated processing does not create new configs')
    for _ in range(10):
        ctl._process_empty_queue(ctx)
    l3_mgr_client.create_virtual_server.assert_not_called()
    l3_mgr_client.create_config_with_vs.assert_not_called()
    l3_mgr_client.awtest_reset_mocks()
    state = assert_l3_state(checker, L3State(balancer=[t_t_f],
                                             backends={u'sas.backend_1': [t_t_f]},
                                             endpoint_sets={u'sas.backend_1': [t_t_f]}))

    ctx.log.info(u'Step 3. Emulate situation when state got corrupted (or we failed to save it)')
    h = L3BalancerStateHandler(state.pb)
    vectors = h.generate_vectors()
    h.reset_in_progress_vector(vectors.in_progress, ignore_l3mgr_config=False)
    assert_l3_state(checker, L3State(balancer=[t_f_f],
                                     backends={u'sas.backend_1': [t_f_f]},
                                     endpoint_sets={u'sas.backend_1': [t_f_f]}))
    current_config = copy.deepcopy(l3_mgr_client.config)
    l3_mgr_client.awtest_set_default_config()
    l3_mgr_client.get_latest_config.return_value = current_config

    ctx.log.info(u'Step 4. Transport again (does not create new config, saves current cfg to state)')
    ctl._process_empty_queue(ctx)
    state = assert_l3_state(checker, L3State(balancer=[t_t_f],
                                             backends={u'sas.backend_1': [t_t_f]},
                                             endpoint_sets={u'sas.backend_1': [t_t_f]}),
                            l3_cfg_pb=model_pb2.L3mgrConfig(service_id=u'test_balancer_1_sas',
                                                            config_id=u'1'))
    l3_mgr_client.create_virtual_server.assert_not_called()
    l3_mgr_client.create_config_with_vs.assert_not_called()
    l3_mgr_client.awtest_reset_mocks()

    ctx.log.info(u'Step 5: activate config and corrupt the state once more')
    l3_mgr_client.awtest_activate_config(1)
    h = L3BalancerStateHandler(state.pb)
    vectors = h.generate_vectors()
    h.reset_in_progress_vector(vectors.in_progress, ignore_l3mgr_config=False)
    assert_l3_state(checker, L3State(balancer=[t_f_f],
                                     backends={u'sas.backend_1': [t_f_f]},
                                     endpoint_sets={u'sas.backend_1': [t_f_f]}))
    current_config_2 = copy.deepcopy(l3_mgr_client.config)
    l3_mgr_client.config = current_config
    l3_mgr_client.get_latest_config.return_value = current_config_2

    ctx.log.info(u'Step 6. Transport VS to L3mgr again (does not create new config, saves current cfg to state)')
    ctl._process_empty_queue(ctx)  # transport
    ctl._process_empty_queue(ctx)  # poll (activate)
    assert_l3_state(checker, L3State(balancer=[t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}))
    l3_mgr_client.create_virtual_server.assert_not_called()
    l3_mgr_client.create_config_with_vs.assert_not_called()


def test_ctl_v2_state_cleanup(ctx, l3_mgr_client, cache, checker):
    vs = model_pb2.L3BalancerSpec.VirtualServer(ip=u'127.0.0.1', port=80)
    vs.health_check_settings.url = u'/ping1'
    vs.health_check_settings.check_type = vs.health_check_settings.CT_HTTP_GET
    vs.traffic_type = vs.TT_EXTERNAL
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=[u'sas.backend_1', u'sas.backend_2'],
        balancer_backend_ids=[],
        l3_balancer_backend_links={
            BALANCER_ID: [u'sas.backend_1']
        },
        l3_vs=[vs],
        l3_ctl_version=2,
    )
    Api.unpause_l3_balancer_transport(NS_ID, BALANCER_ID)

    ctl = L3BalancerCtlV2(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    BackendCtl(namespace_id=NS_ID, backend_id=u'sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host=u'ya.ru', port=80, weight=1, ipv4_addr=u'127.0.0.1', ipv6_addr=u'::1'),
    ])

    ctx.log.info(u'1. Transport VS and RS to L3mgr')
    ctl._process_empty_queue(ctx)  # discover
    ctl._process_empty_queue(ctx)  # validate
    ctl._process_empty_queue(ctx)  # transport
    l3_mgr_client.awtest_activate_config(1)
    ctl._process_empty_queue(ctx)  # poll (vector not active yet)
    ctl._process_empty_queue(ctx)  # transport (should_activate_vector -> True)
    ctl._process_empty_queue(ctx)  # poll (activate vector)
    assert_l3_state(checker, L3State(balancer=[t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}))

    ctx.log.info(u'2. Add unresolved backend_2')
    relink_l3_balancer(NS_ID, BALANCER_ID, [u'sas.backend_1', u'sas.backend_2'])
    ctl._process_empty_queue(ctx)  # discover
    ctl._process_empty_queue(ctx)  # validate
    assert_l3_state(checker, L3State(balancer=[t_f_t, f_f_f],
                                     backends={u'sas.backend_1': [t_f_t], u'sas.backend_2': [f_f_f]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}))

    ctx.log.info(u'3. Remove unresolved backend_2 from spec and check that it is removed from state')
    relink_l3_balancer(NS_ID, BALANCER_ID, [u'sas.backend_1'])
    ctl._process_empty_queue(ctx)  # discover
    ctx.log.info(u'3.1')
    ctl._process_empty_queue(ctx)  # validate
    ctx.log.info(u'3.2')
    ctl._process_empty_queue(ctx)  # transport
    ctx.log.info(u'3.3')
    ctl._process_empty_queue(ctx)  # poll (activate)
    ctx.log.info(u'3.4')
    ctl._process_empty_queue(ctx)  # cleanup state
    assert_l3_state(checker, L3State(balancer=[t_f_t],
                                     backends={u'sas.backend_1': [t_f_t]},
                                     endpoint_sets={u'sas.backend_1': [t_f_t]}))


@pytest.mark.parametrize('ctl_class', (BaseL3BalancerCtl, BaseL3BalancerCtlV2))
def test_deadlines(ctx, l3_mgr_client, ctl_class):
    ctl = ctl_class(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    ctl.FORCE_PROCESS_INTERVAL_JITTER = 0

    curr_time = monotonic.monotonic()
    assert ctl._should_process(curr_time)
    assert ctl._should_poll(curr_time)

    # test polling interval
    ctl.POLL_INTERVAL = 0.1

    curr_time += 0.01
    ctl._polling_deadline = curr_time + .1
    assert not ctl._should_poll(curr_time)
    curr_time += 0.11
    assert ctl._should_poll(curr_time)
    ctl._reset_polling_timers()

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


@pytest.mark.parametrize('ctl_class', (L3BalancerCtl, L3BalancerCtlV2))
def test_l3mgr_exceptions(ctx, l3_mgr_client, caplog, checker, ctl_class):
    l3_mgr_client.awtest_set_default_config()
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=['sas.backend_1', 'sas.backend_2', 'sas.backend_3', 'sas.backend_4'],
        balancer_backend_ids=[],
        l3_balancer_backend_links={
            BALANCER_ID: ['sas.backend_1']
        }
    )

    ctl = ctl_class(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)

    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            l3_state = [f_f_f] if ctl_class is L3BalancerCtlV2 else [u_f_f]
            expected_state = L3State(
                balancer=l3_state,
                backends={
                    'sas.backend_1': [f_f_f]
                },
            )
            assert actual_state == expected_state

    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='ya.ru', port=80, weight=1, ipv4_addr='127.0.0.1', ipv6_addr='::1'),
    ])
    Api.unpause_l3_balancer_transport(NS_ID, BALANCER_ID)

    exc = l3mgrclient.L3MgrException()
    with mock.patch.object(l3_mgr_client, 'get_service', side_effect=exc):
        with check_log(caplog) as log:
            def check():
                ctl._process_empty_queue(ctx)
                assert 'failed to call get_service' in log.records_text()

            wait_until_passes(check)
    l3_mgr_client.awtest_set_default_config()

    exc.resp = mock.Mock()
    exc.resp.status_code = httplib.NOT_FOUND
    with mock.patch.object(l3_mgr_client, 'get_service', side_effect=exc):
        with check_log(caplog) as log:
            for a in checker:
                with a:
                    ctl._process_empty_queue(ctx)
                    assert 'is missing from l3mgr for some reason' in log.records_text()
    l3_mgr_client.awtest_set_default_config()

    if ctl_class is L3BalancerCtl:
        with mock.patch.object(l3_mgr_client, 'create_config_with_rs', side_effect=exc):
            with check_log(caplog) as log:
                for a in checker:
                    with a:
                        with awtest.raises(l3mgrclient.L3MgrException):
                            ctl._process_empty_queue(ctx)
                        assert 'failed to call create_config_with_rs' in log.records_text()
        l3_mgr_client.awtest_set_default_config()

        with mock.patch.object(l3_mgr_client, 'process_config', side_effect=exc):
            with check_log(caplog) as log:
                for a in checker:
                    with a:
                        with awtest.raises(l3mgrclient.L3MgrException):
                            ctl._process_empty_queue(ctx)
                        assert 'failed to call process_config' in log.records_text()
        l3_mgr_client.awtest_set_default_config()

        with mock.patch.object(l3_mgr_client, 'get_config', side_effect=exc):
            with check_log(caplog) as log:
                for a in checker:
                    with a:
                        with awtest.raises(l3mgrclient.L3MgrException):
                            ctl._process_empty_queue(ctx)
                        assert 'failed to get active service config from L3mgr' in log.records_text()
        l3_mgr_client.awtest_set_default_config()

    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            actual_state = L3State.from_pb(state_pb)
            expected_state = L3State(
                balancer=[t_t_f],
                backends={
                    'sas.backend_1': [t_t_f],
                },
                endpoint_sets={
                    'sas.backend_1': [t_t_f],
                }
            )
            assert actual_state == expected_state
    assert get_in_progress_full_config_ids(ctl, state_pb) == {('test_balancer_1_sas', '1'): 1517553426846000}

    with mock.patch.object(l3_mgr_client, 'get_config', side_effect=exc):
        with check_log(caplog) as log:
            for a in checker:
                with a:
                    ctl._process_empty_queue(ctx)
                    assert 'is missing from l3mgr' in log.records_text()
    l3_mgr_client.awtest_set_default_config()

    if ctl_class is L3BalancerCtl:
        exc.resp.status_code = None
        with mock.patch.object(l3_mgr_client, 'get_config', side_effect=exc):
            with check_log(caplog) as log:
                for a in checker:
                    with a:
                        with awtest.raises(l3mgrclient.L3MgrException):
                            ctl._process_empty_queue(ctx)
                        assert 'failed to get active service config from L3mgr' in log.records_text()


@pytest.mark.parametrize('ctl_class', (L3BalancerCtl, L3BalancerCtlV2))
def test_manual_skip_config(zk_storage, ctx, caplog, checker, l3_mgr_client, ctl_class):
    l3_mgr_client.awtest_set_default_config()
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=['sas.backend_1', 'sas.backend_2', 'sas.backend_3', 'sas.backend_4'],
        balancer_backend_ids=[],
        l3_balancer_backend_links={
            BALANCER_ID: ['sas.backend_1']
        }
    )
    Api.unpause_l3_balancer_transport(NS_ID, BALANCER_ID)

    ctl = ctl_class(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='ya.ru', port=80, weight=1, ipv4_addr='127.0.0.1', ipv6_addr='::1'),
    ])
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            actual_state = L3State.from_pb(state_pb)
            expected_state = L3State(
                balancer=[t_t_f],
                backends={
                    'sas.backend_1': [t_t_f],
                },
                endpoint_sets={
                    'sas.backend_1': [t_t_f],
                }
            )
            assert actual_state == expected_state
    assert get_in_progress_full_config_ids(ctl, state_pb) == {('test_balancer_1_sas', '1'): 1517553426846000}

    relink_l3_balancer(NS_ID, BALANCER_ID, ['sas.backend_2'])
    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_2').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='ya2.ru', port=80, weight=1, ipv4_addr='127.0.0.1', ipv6_addr='::1'),
    ])

    with check_log(caplog) as log:
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                actual_state = L3State.from_pb(state_pb)
                expected_state = L3State(
                    balancer=[t_t_f, t_f_f],
                    backends={
                        'sas.backend_1': [t_t_f],
                        'sas.backend_2': [t_f_f],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_t_f],
                        'sas.backend_2': [t_f_f],
                    }
                )
                assert actual_state == expected_state
                if ctl_class is L3BalancerCtl:
                    assert 'not proceeding: waiting for the activation of in-progress' in log.records_text()
                elif ctl_class is L3BalancerCtlV2:
                    assert 'not transporting: previous config is already in progress' in log.records_text()

    if ctl_class is L3BalancerCtl:
        skip_in_progress_l3mgr_config(NS_ID, BALANCER_ID,
                                      l3mgr_config_id_to_skip='1', l3mgr_service_id_to_skip='test_balancer_1_sas')
    else:
        state_pb = zk_storage.must_get_l3_balancer_state(NS_ID, BALANCER_ID)
        h = L3BalancerStateHandler(state_pb)
        h.reset_in_progress_vector(vector=h.generate_vectors().in_progress,
                                   author=u'test',
                                   comment=u'Manually forcing creation of fresh config in L3Mgr')

    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            actual_state = L3State.from_pb(state_pb)
            expected_state = L3State(
                balancer=[t_f_f, t_t_f],
                backends={
                    'sas.backend_1': [t_t_f],
                    'sas.backend_2': [t_t_f],
                },
                endpoint_sets={
                    'sas.backend_1': [t_t_f],
                    'sas.backend_2': [t_t_f],
                }
            )
            assert actual_state == expected_state
    if ctl_class == L3BalancerCtl:
        assert get_in_progress_full_config_ids(ctl, state_pb) == {('test_balancer_1_sas', '2'): 1517553726846000}


@pytest.mark.parametrize('ctl_class', (L3BalancerCtl, L3BalancerCtlV2))
def test_automatic_skip_config(ctx, l3_mgr_client, caplog, checker, ctl_class):
    vs = model_pb2.L3BalancerSpec.VirtualServer(ip=u'127.0.0.1', port=80)
    vs.health_check_settings.url = u'/ping1'
    vs.health_check_settings.check_type = vs.health_check_settings.CT_HTTP_GET
    vs.traffic_type = vs.TT_EXTERNAL
    l3_mgr_client.awtest_set_default_config()
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=['sas.backend_1', 'sas.backend_2', 'sas.backend_3', 'sas.backend_4'],
        balancer_backend_ids=[],
        l3_balancer_backend_links={
            BALANCER_ID: ['sas.backend_1']
        },
        l3_vs=[vs] if ctl_class is L3BalancerCtlV2 else None,
        l3_ctl_version=2 if ctl_class is L3BalancerCtlV2 else 1,
    )

    ctl = ctl_class(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)

    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='ya.ru', port=80, weight=1, ipv4_addr='127.0.0.1', ipv6_addr='::1'),
    ])

    skipped_vector_hashes = set()
    Api.unpause_l3_balancer_transport(NS_ID, BALANCER_ID)
    timestamp = 1517553426846000
    for attempt, status in enumerate([u'VCS_FAIL', u'TEST_FAIL', u'VCS_FAIL', u'ACTIVE']):
        config_id = attempt + 1
        ctx.log.info(u'Test: Attempt #%s.1, status NEW, config_id %s', attempt, config_id)
        with check_log(caplog) as log:
            for a in checker:
                with a:
                    ctl._process_empty_queue(ctx)
                    state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                    actual_state = L3State.from_pb(state_pb)
                    expected_state = L3State(
                        balancer=[t_t_f],
                        backends={
                            'sas.backend_1': [t_t_f],
                        },
                        endpoint_sets={
                            'sas.backend_1': [t_t_f],
                        }
                    )
                    assert actual_state == expected_state
                    if ctl_class is L3BalancerCtl:
                        assert u'test_balancer_1_sas:{} state is NEW, ' \
                               u'continue waiting'.format(config_id) in log.records_text()
                    elif ctl_class is L3BalancerCtlV2:
                        assert u'Forcing creation of fresh config' not in log.records_text()

        assert get_in_progress_full_config_ids(ctl, state_pb) == {
            ('test_balancer_1_sas', six.text_type(config_id)): timestamp,
        }
        timestamp += 5 * 60 * 1000000

        ctx.log.info('Test: Attempt #%s.2, status %s, config_id %s', attempt, status, config_id)
        l3_mgr_client.awtest_set_config_state(config_id, status)
        with check_log(caplog) as log:
            if status == u'ACTIVE':
                l3_mgr_client.awtest_activate_config(config_id)
                for a in checker:
                    with a:
                        ctl._process_empty_queue(ctx)
                        state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                        actual_state = L3State.from_pb(state_pb)
                        expected_state = L3State(
                            balancer=[t_f_t],
                            backends={
                                'sas.backend_1': [t_f_t],
                            },
                            endpoint_sets={
                                'sas.backend_1': [t_f_t],
                            }
                        )
                        assert actual_state == expected_state

                state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                assert not state_pb.skip_counts
            else:
                for a in checker:
                    with a:
                        ctl._process_empty_queue(ctx)
                        state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                        actual_state = L3State.from_pb(state_pb)

                        expected_state = L3State(
                            balancer=[t_t_f],
                            backends={
                                'sas.backend_1': [t_t_f],
                            },
                            endpoint_sets={
                                'sas.backend_1': [t_t_f],
                            }
                        )
                        assert actual_state == expected_state
                        if ctl_class is L3BalancerCtl:
                            assert (u'test_balancer_1_sas:{} state is {}, '
                                    u'skipping').format(config_id, status) in log.records_text()
                        else:
                            assert (u'Forcing creation of fresh config, '
                                    u'because config "test_balancer_1_sas:{}" has bad state "{}"'.format(
                                config_id, status) in log.records_text())

                state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                assert len(state_pb.skip_counts) == 1
                skipped_vector_hash, skip_count = list(state_pb.skip_counts.items())[0]
                skipped_vector_hashes.add(skipped_vector_hash)

    assert len(skipped_vector_hashes) == 1


def test_ctl_v2_unprocessed_latest_config(ctx, l3_mgr_client, caplog, checker):
    vs = model_pb2.L3BalancerSpec.VirtualServer(ip=u'127.0.0.1', port=80)
    vs.health_check_settings.url = u'/ping1'
    vs.health_check_settings.check_type = vs.health_check_settings.CT_HTTP_GET
    vs.traffic_type = vs.TT_EXTERNAL
    l3_mgr_client.awtest_set_default_config()
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=['sas.backend_1'],
        balancer_backend_ids=[],
        l3_balancer_backend_links={
            BALANCER_ID: ['sas.backend_1']
        },
        l3_vs=[vs],
        l3_ctl_version=2,
    )

    ctl = L3BalancerCtlV2(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='ya.ru', port=80, weight=1, ipv4_addr='127.0.0.1', ipv6_addr='::1'),
    ])
    Api.unpause_l3_balancer_transport(NS_ID, BALANCER_ID)

    ctx.log.info(u'Step 1: save config, but fail to process it')
    l3_mgr_client.process_config.side_effect = L3BalancerTransportError
    for a in checker:
        with a:
            try:
                ctl._process_empty_queue(ctx)
            except L3BalancerTransportError:
                pass
            l3_mgr_client.create_config_with_vs.assert_called_once()
    l3_mgr_client.awtest_reset_mocks()

    ctx.log.info(u'Step 2: config is not processed, so process it')
    l3_mgr_client.config[u'state'] = u'NEW'
    l3_mgr_client.process_config.side_effect = None

    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            actual_state = L3State.from_pb(state_pb)
            expected_state = L3State(
                balancer=[t_t_f],
                backends={
                    'sas.backend_1': [t_t_f],
                },
                endpoint_sets={
                    'sas.backend_1': [t_t_f],
                }
            )
            assert actual_state == expected_state
            l3_mgr_client.process_config.assert_called_once()


@pytest.mark.parametrize('allow_foreign_rs', (False, True))
def test_insignificant_rs_differences(ctx, l3_mgr_client, allow_foreign_rs):
    l3_mgr_client.awtest_set_default_config()
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=[u'sas.backend_1'],
        balancer_backend_ids=[],
        l3_balancer_backend_links={
            BALANCER_ID: [u'sas.backend_1']
        },
        allow_foreign_rs=allow_foreign_rs
    )

    ctl = L3BalancerCtl(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)

    BackendCtl(namespace_id=NS_ID, backend_id=u'sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host=u'ya.ru', port=80, weight=1, ipv4_addr=u'127.0.0.1', ipv6_addr=u'::1'),
    ])

    vs_1 = copy.deepcopy(l3_mgr_client.vs[0])
    vs_2 = copy.deepcopy(l3_mgr_client.vs[0])
    vs_1[u'id'] = 1
    vs_2[u'id'] = 2

    Api.unpause_l3_balancer_transport(NS_ID, BALANCER_ID)

    l3_mgr_client.awtest_reset_mocks()
    ctx.log.info(u'Step 1: VS in L3mgr have significant differences')
    vs_1[u'group'] = [u'some-test-domain.yandex.net']
    vs_2[u'group'] = [u'unrelated-domain.yandex.net']
    l3_mgr_client.vs = [vs_1, vs_2]
    ctl._process_empty_queue(ctx)  # discover
    ctl._process_empty_queue(ctx)  # validate
    if six.PY3:
        with awtest.raises(L3BalancerTransportError,
                           text=(u"RS groups are not the same: {"
                                 u"vs[1]: ['some-test-domain.yandex.net'], "
                                 u"vs[2]: ['unrelated-domain.yandex.net']}")):
            ctl._process_empty_queue(ctx)
    else:
        with awtest.raises(L3BalancerTransportError,
                           text=(u"RS groups are not the same: {"
                                 u"vs[1]: [u'some-test-domain.yandex.net'], "
                                 u"vs[2]: [u'unrelated-domain.yandex.net']}")):
            ctl._process_empty_queue(ctx)

    l3_mgr_client.awtest_reset_mocks()
    vs_1[u'group'] = [u'some-test-domain.yandex.net']
    vs_2[u'group'] = [u'some-test-domain.yandex.net weight=1']
    l3_mgr_client.vs = [vs_1, vs_2]
    if six.PY3:
        with awtest.raises(L3BalancerTransportError, text=(u"RS groups are not the same: {"
                                                           u"vs[1]: ['some-test-domain.yandex.net'], "
                                                           u"vs[2]: ['some-test-domain.yandex.net weight=1']}")):
            ctl._process_empty_queue(ctx)
    else:
        with awtest.raises(L3BalancerTransportError, text=(u"RS groups are not the same: {"
                                                           u"vs[1]: [u'some-test-domain.yandex.net'], "
                                                           u"vs[2]: [u'some-test-domain.yandex.net weight=1']}")):
            ctl._process_empty_queue(ctx)

    l3_mgr_client.awtest_reset_mocks()
    ctx.log.info(u'Step 2: unsupported RS param')
    vs_1[u'group'] = [u'some-test-domain.yandex.net']
    vs_2[u'group'] = [u'some-test-domain.yandex.net something=X']
    l3_mgr_client.vs = [vs_1, vs_2]
    if allow_foreign_rs:
        msg = u'Unsupported RS parameter "something"'
    else:
        if six.PY3:
            msg = (u"RS groups are not the same: {"
                   u"vs[1]: ['some-test-domain.yandex.net'], "
                   u"vs[2]: ['some-test-domain.yandex.net something=X']}")
        else:
            msg = (u"RS groups are not the same: {"
                   u"vs[1]: [u'some-test-domain.yandex.net'], "
                   u"vs[2]: [u'some-test-domain.yandex.net something=X']}")
    with awtest.raises(L3BalancerTransportError, text=msg):
        ctl._process_empty_queue(ctx)

    vs_1[u'group'] = [u'some-test-domain.yandex.net something=X']
    vs_2[u'group'] = [u'some-test-domain.yandex.net something=X']
    l3_mgr_client.vs = [vs_1, vs_2]
    if allow_foreign_rs:
        with awtest.raises(L3BalancerTransportError, text=u'Unsupported RS parameter "something"'):
            ctl._process_empty_queue(ctx)
    else:
        ctl._process_empty_queue(ctx)  # ignore unknown param in L3mgr RS, we'll overwrite it anyway

    # cleanup
    l3_mgr_client.awtest_reset_mocks()
    l3_mgr_client.awtest_set_default_config()
    l3_balancer_state_pb, _ = skip_in_progress_l3mgr_config(NS_ID, BALANCER_ID,
                                                            l3mgr_config_id_to_skip=u'1',
                                                            l3mgr_service_id_to_skip=u'test_balancer_1_sas',
                                                            ignore_existing_config=False)
    ctx.log.info(u'Step 3: VS in L3mgr have insignificant differences')
    if allow_foreign_rs:
        vs_1[u'group'] = [u'some-test-domain.yandex.net', u'ya.ru']
        vs_2[u'group'] = [u'some-test-domain.yandex.net=::1', u'ya.ru=8.8.8.8']
    else:
        vs_1[u'group'] = [u'ya.ru']
        vs_2[u'group'] = [u'ya.ru=8.8.8.8']
    l3_mgr_client.vs = [vs_1, vs_2]
    ctl._transport.transport(ctx, l3_balancer_state_pb)  # no error
    if allow_foreign_rs:
        groups = [u'some-test-domain.yandex.net', u'ya.ru=::1']
    else:
        groups = [u'ya.ru=::1']
    l3_mgr_client.create_config_with_rs.assert_called_with(groups=groups, svc_id=BALANCER_ID, use_etag=False)

    # cleanup
    l3_mgr_client.awtest_reset_mocks()
    l3_mgr_client.awtest_set_default_config()
    l3_balancer_state_pb, _ = skip_in_progress_l3mgr_config(NS_ID, BALANCER_ID,
                                                            l3mgr_config_id_to_skip=u'1',
                                                            l3mgr_service_id_to_skip=u'test_balancer_1_sas',
                                                            ignore_existing_config=False)
    ctx.log.info(u'Step 4: make sure we correctly update IP address if not present')
    vs_1[u'group'] = [u'ya.ru']
    vs_2[u'group'] = [u'ya.ru']
    l3_mgr_client.vs = [vs_1, vs_2]
    ctl._transport.transport(ctx, l3_balancer_state_pb)
    l3_mgr_client.create_config_with_rs.assert_called_with(groups=[u'ya.ru=::1'],
                                                           svc_id=BALANCER_ID,
                                                           use_etag=False)


@pytest.mark.parametrize('allow_foreign_rs', (False, True))
def test_foreign_rs(ctx, l3_mgr_client, checker, allow_foreign_rs):
    l3_mgr_client.awtest_set_default_config()
    create_namespace(
        namespace_id=NS_ID,
        l3_balancer_ids=[BALANCER_ID],
        backend_ids=[],
        balancer_backend_ids=['sas.backend_1', 'sas.backend_2'],
        l3_balancer_backend_links={
            BALANCER_ID: ['sas.backend_1']
        },
        allow_foreign_rs=allow_foreign_rs,
    )

    ctl = L3BalancerCtl(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)

    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_1').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='ya.ru', port=80, weight=1, ipv4_addr='127.0.0.1', ipv6_addr='::1'),
    ])
    BackendCtl(namespace_id=NS_ID, backend_id='sas.backend_2').mock_process(ctx, instance_pbs=[
        internals_pb2.Instance(host='another.yandex.net', port=80, weight=0.5, ipv4_addr='127.0.0.2', ipv6_addr='::2'),
    ])
    l3_balancer_pb = Api.get_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    l3_balancer_pb.spec.use_endpoint_weights = True
    Api.update_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID,
                           version=l3_balancer_pb.meta.version, spec_pb=l3_balancer_pb.spec)

    ctx.log.info('Step 1: L3mgr already has some RS, so we need to preserve them if L3 spec says so')
    l3_mgr_client.vs[0]['group'] = ['some-existing-host.yandex.net']
    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                actual_state = L3State.from_pb(state_pb)
                expected_state = L3State(
                    balancer=[t_t_f],
                    backends={
                        'sas.backend_1': [t_t_f],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_t_f],
                    }
                )
                assert actual_state == expected_state
    assert get_in_progress_full_config_ids(ctl, state_pb) == {('test_balancer_1_sas', '1'): 1517553426846000}
    if allow_foreign_rs:
        groups = [u'some-existing-host.yandex.net', u'ya.ru=::1 weight=1']
    else:
        groups = [u'ya.ru=::1 weight=1']
    l3_mgr_client.create_config_with_rs.assert_called_with(groups=groups, svc_id=BALANCER_ID, use_etag=False)

    l3_mgr_client.awtest_activate_config(1)
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t],
                backends={
                    'sas.backend_1': [t_f_t],
                },
                endpoint_sets={
                    'sas.backend_1': [t_f_t],
                },
            )
            assert actual_state == expected_state

    ctx.log.info('Step 2: modify L3 spec and make sure that foreign RS is still present')
    relink_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID,
                       backend_ids=['sas.backend_1', 'sas.backend_2'])
    # whitespaces to check config parsing
    l3_mgr_client.vs[0]['group'] = [u' some-existing-host.yandex.net = ::1    weight = 80 ', u'   ya.ru=::1   ']
    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                actual_state = L3State.from_pb(state_pb)
                expected_state = L3State(
                    balancer=[t_f_t, t_t_f],
                    backends={
                        'sas.backend_1': [t_t_t],
                        'sas.backend_2': [t_t_f],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_t_t],
                        'sas.backend_2': [t_t_f],
                    }
                )
                assert actual_state == expected_state
    assert get_in_progress_full_config_ids(ctl, state_pb) == {('test_balancer_1_sas', '2'): 1517553726846000}

    if allow_foreign_rs:
        groups = [u'another.yandex.net=::2 weight=1',
                  u'some-existing-host.yandex.net=::1 weight=80',
                  u'ya.ru=::1 weight=1']
    else:
        groups = [u'another.yandex.net=::2 weight=1', u'ya.ru=::1 weight=1']

    l3_mgr_client.create_config_with_rs.assert_called_with(groups=groups, svc_id=BALANCER_ID, use_etag=False)

    l3_mgr_client.awtest_activate_config(2)
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t],
                backends={
                    'sas.backend_1': [t_f_t],
                    'sas.backend_2': [t_f_t],
                },
                endpoint_sets={
                    'sas.backend_1': [t_f_t],
                    'sas.backend_2': [t_f_t],
                },
            )
            assert actual_state == expected_state

    if not allow_foreign_rs:
        return

    ctx.log.info('Step 3: disallow foreign RS and check that awacs removes them from L3mgr')
    l3_balancer_pb = Api.get_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    l3_balancer_pb.spec.preserve_foreign_real_servers = False
    Api.update_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID,
                           version=l3_balancer_pb.meta.version, spec_pb=l3_balancer_pb.spec)
    l3_mgr_client.vs[0]['group'] = [u'another.yandex.net=::2 weight=1',
                                    u'some-existing-host.yandex.net',
                                    u'ya.ru=::1 weight=1']
    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                actual_state = L3State.from_pb(state_pb)
                expected_state = L3State(
                    balancer=[t_f_t, t_t_f],
                    backends={
                        'sas.backend_1': [t_t_t],
                        'sas.backend_2': [t_t_t],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_t_t],
                        'sas.backend_2': [t_t_t],
                    }
                )
                assert actual_state == expected_state
    assert get_in_progress_full_config_ids(ctl, state_pb) == {('test_balancer_1_sas', '3'): 1517554026846000}
    l3_mgr_client.create_config_with_rs.assert_called_with(
        groups=[u'another.yandex.net=::2 weight=1', u'ya.ru=::1 weight=1'], svc_id=BALANCER_ID, use_etag=False)

    l3_mgr_client.awtest_activate_config(3)
    for a in checker:
        with a:
            ctl._process_empty_queue(ctx)
            actual_state = L3State.from_api(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
            expected_state = L3State(
                balancer=[t_f_t],
                backends={
                    'sas.backend_1': [t_f_t],
                    'sas.backend_2': [t_f_t],
                },
                endpoint_sets={
                    'sas.backend_1': [t_f_t],
                    'sas.backend_2': [t_f_t],
                },
            )
            assert actual_state == expected_state

    ctx.log.info('Step 4: check that empty RS list in L3mgr is processed correctly')
    l3_balancer_pb = Api.get_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    l3_balancer_pb.spec.preserve_foreign_real_servers = True
    Api.update_l3_balancer(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID,
                           version=l3_balancer_pb.meta.version, spec_pb=l3_balancer_pb.spec)
    l3_mgr_client.vs[0]['group'] = []
    with unpaused_l3_balancer_transport(NS_ID, BALANCER_ID):
        for a in checker:
            with a:
                ctl._process_empty_queue(ctx)
                state_pb = Api.get_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
                actual_state = L3State.from_pb(state_pb)
                expected_state = L3State(
                    balancer=[t_f_t, t_t_f],
                    backends={
                        'sas.backend_1': [t_t_t],
                        'sas.backend_2': [t_t_t],
                    },
                    endpoint_sets={
                        'sas.backend_1': [t_t_t],
                        'sas.backend_2': [t_t_t],
                    }
                )
                assert actual_state == expected_state
    assert get_in_progress_full_config_ids(ctl, state_pb) == {('test_balancer_1_sas', '4'): 1517554326846000}
    l3_mgr_client.create_config_with_rs.assert_called_with(
        groups=[u'another.yandex.net=::2 weight=1', u'ya.ru=::1 weight=1'], svc_id=BALANCER_ID, use_etag=False)

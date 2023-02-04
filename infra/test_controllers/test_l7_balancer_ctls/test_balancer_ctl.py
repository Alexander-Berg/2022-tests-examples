# coding: utf-8
import contextlib
import logging

import datetime
import flaky
import inject
import mock
import pytest
import six
import ujson
import yaml
from six.moves import range

from awacs.lib import OrderedDict
from awacs.lib import nannyrpcclient, l3mgrclient
from awacs.lib.nannyclient import INannyClient
from awacs.lib.rpc.exceptions import NotFoundError, ConflictError, BadRequestError
from awacs.lib.strutils import to_full_id
from awacs.model import dao, events, components
from awacs.model.backend import BackendCtl as BaseBackendCtl
from awacs.model.balancer.component_transports import awacslet
from awacs.model.balancer.ctl import BalancerCtl as BaseBalancerCtl, transport
from awacs.model.l3_balancer.ctl import L3BalancerCtl as BaseL3BalancerCtl
from awacs.model.l3_balancer.transport import Transport as L3BalancerTransport
from awacs.model.l3_balancer.validator import Validator as L3BalancerValidator
from awacs.model.namespace.ctl import NamespaceCtl as BaseNamespaceCtl
from awacs.model.util import clone_pb
from awacs.web import component_service, validation
from awacs.yamlparser.util import KTag, CTag
from awacs.wrappers.base import Holder
from awtest import wait_until_passes, wait_until, freeze_time, t
from awtest.api import (Api, create_namespace_func, make_balancer_yml, make_upstream_yml, make_balancer_spec_pb,
                        make_upstream_spec_pb, create_cert, create_domain, update_domain)
from awtest.mocks.nanny_rpc_client import NannyRpcMockClient
from awtest.mocks.resolver import ResolverStub
from awtest.network import mock_resolve_host
from awtest.l3util import L3State, u_f_f as l3_u_f_f, f_f_f as l3_f_f_f, t_f_f as l3_t_f_f
from awtest.l7util import L7State, p_f_f, f_f_f, t_f_f, u_f_f, t_f_t, t_t_f, t_t_t
from infra.awacs.proto import model_pb2, api_pb2
from infra.swatlib import httpgridfsclient, metrics
from infra.swatlib.auth import abc

MAX_RUNS = 3


class L3BalancerCtl(BaseL3BalancerCtl):
    POLL_INTERVAL = 0.1
    PROCESS_INTERVAL = 0.1
    FORCE_PROCESS_INTERVAL = 1
    FORCE_PROCESS_INTERVAL_JITTER = 0
    EVENTS_QUEUE_GET_TIMEOUT = 0.1


class BalancerCtl(BaseBalancerCtl):
    TRANSPORT_PROCESSING_INTERVAL = 0.01
    TRANSPORT_POLLING_INTERVAL = 0.01
    TRANSPORT_MAIN_LOOP_FREQ = 0.01

    PROCESS_INTERVAL = 9999
    FORCE_PROCESS_INTERVAL = 9999
    EVENTS_QUEUE_GET_TIMEOUT = 9999
    SLEEP_AFTER_EXCEPTION_TIMEOUT = 0.01

    def _process_empty_queue(self, *_, **__):
        pass


class NamespaceCtl(BaseNamespaceCtl):
    TRANSPORT_PROCESSING_INTERVAL = 0.01
    TRANSPORT_POLLING_INTERVAL = 0.01
    TRANSPORT_MAIN_LOOP_FREQ = 0.01

    PROCESS_INTERVAL = 9999
    FORCE_PROCESS_INTERVAL = 9999
    EVENTS_QUEUE_GET_TIMEOUT = 9999
    SLEEP_AFTER_EXCEPTION_TIMEOUT = 0.01

    def _process_empty_queue(self, *_, **__):
        pass


class BackendCtl(BaseBackendCtl):
    PROCESS_INTERVAL = 9999
    SELF_DELETION_CHECK_INTERVAL = 0.01
    SLEEP_AFTER_EXCEPTION_TIMEOUT = 9999
    EVENTS_QUEUE_GET_TIMEOUT = 9999
    SELF_DELETION_COOLDOWN_PERIOD = 0.01

    def _fill_snapshot_ids(self, selector_pb):
        for nanny_snapshot_pb in selector_pb.nanny_snapshots:
            nanny_snapshot_pb.snapshot_id = 'xxx'


BALANCER_YAML_FOR_CERTS = r'''instance_macro:
  buffer: 65535
  maxconn: 50
  workers: 10
  log_dir: /place/db/www/logs/
  sections:
    admin:
      ips:
        - 127.0.0.1
      ports:
        - 1021
      http:
        maxlen: 65536
        maxreq: 65536
        admin: {}
    local_ips:
      ips:
        - '2a02:6b8:0:3400::107e'
      ports:
        - 1025
      extended_http_macro:
        port: 1025
        maxlen: 65530
        maxreq: 65535
        regexp:
          include_upstreams:
            filter:
              any: true'''

BALANCER_YML_FOR_KNOBS = r'''instance_macro:
      buffer: 65535
      maxconn: 50
      workers: 10
      log_dir: /place/db/www/logs/
      sections:
        admin:
          ips:
            - 127.0.0.1
          ports:
            - 1021
          http:
            maxlen: 65536
            maxreq: 65536
            admin: {}
        local_ips:
          ips:
            - !f get_ip_by_iproute("v6")
          ports:
            - 1025
          extended_http_macro:
            port: 1025
            maxlen: 65530
            maxreq: 65535
            regexp:
              include_upstreams:
                filter:
                  any: true'''


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, caplog, abc_client, l3_mgr_client):
    def configure(b):
        b.bind(abc.IAbcClient, abc_client)
        b.bind(l3mgrclient.IL3MgrClient, l3_mgr_client)
        b.bind(nannyrpcclient.INannyRpcClient, NannyRpcMockClient('https://api/repo/'))
        b.bind(httpgridfsclient.IHttpGridfsClient, mock.Mock())
        binder_with_nanny_client(b)

    caplog.set_level(logging.DEBUG)
    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_empty_namespace(namespace_id):
    Api.create_namespace(namespace_id=namespace_id)


def create_resolved_manual_backends(namespace_id, n):
    rv = []
    for i in range(1, n + 1):
        backend_id = 'backend_{}'.format(i)
        backend_spec_pb = model_pb2.BackendSpec()
        backend_spec_pb.selector.type = model_pb2.BackendSelector.MANUAL
        backend_pb = Api.create_backend(
            namespace_id=namespace_id,
            backend_id=backend_id,
            spec_pb=backend_spec_pb)
        es_spec_pb = model_pb2.EndpointSetSpec()
        es_spec_pb.instances.add(host='{}-instance.yandex.ru'.format(i), port=80, weight=1, ipv6_addr='::1')
        Api.create_endpoint_set(
            namespace_id=namespace_id,
            endpoint_set_id=backend_id,
            spec_pb=es_spec_pb,
            backend_version=backend_pb.meta.version)
        rv.append(backend_id)
    return rv


def check_included_backends(namespace_id, upstream_ids, upstream_backend_links):
    for up_id in upstream_ids:
        upstream_pb = Api.get_upstream(namespace_id, up_id)
        for rev in Api.list_upstream_revisions(namespace_id, up_id).revisions:
            if rev.meta.id != upstream_pb.meta.version:
                continue
            holder = Holder(rev.spec.yandex_balancer.config)
            for module in holder.walk_chain(visit_branches=True):
                if module.includes_backends():
                    included_full_backend_ids = module.include_backends.get_included_full_backend_ids(namespace_id)
                    expected_full_backend_ids = [to_full_id(namespace_id, b_id)
                                                 for b_id in upstream_backend_links[up_id]]
                    assert sorted(included_full_backend_ids) == sorted(expected_full_backend_ids)


@contextlib.contextmanager
def unpaused_balancer_transport(namespace_id, balancer_id, prevent_polling=False):
    Api.unpause_balancer_transport(namespace_id, balancer_id)
    if prevent_polling:
        with mock.patch.object(transport.BalancerTransport, 'poll_snapshots'):
            yield
    else:
        yield
    Api.pause_balancer_transport(namespace_id, balancer_id)


def m(rs):
    return ujson.loads(rs.v_message)['message']


def update_namespace(namespace_id, balancer_ids=frozenset(), balancer_upstream_links=None, balancer_ctl_version=0):
    """
    :param str namespace_id:
    :type balancer_ids: list[str] | set[str]
    :param balancer_upstream_links:
    :return:
    """
    balancer_upstream_links = balancer_upstream_links or {}

    for balancer_id in balancer_ids:
        included_upstream_ids = sorted(balancer_upstream_links.get(balancer_id, []))
        balancer_yml = make_balancer_yml(upstream_ids_to_include=included_upstream_ids)
        balancer_pb = Api.get_balancer(namespace_id=namespace_id, balancer_id=balancer_id)
        nanny_service_id = balancer_pb.spec.config_transport.nanny_static_file.service_id
        spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=balancer_ctl_version)
        balancer_pb = Api.update_balancer(namespace_id=namespace_id,
                                          balancer_id=balancer_id,
                                          version=balancer_pb.meta.version,
                                          spec_pb=spec_pb)
        wait_until(lambda: Api.get_balancer(namespace_id, balancer_id).meta.version == balancer_pb.meta.version,
                   timeout=1)


class NannyStub(object):
    def __init__(self):
        self.curr_snapshot_ids = {}
        self.curr_snapshot_ctimes = {}
        self.snapshot_states = {}
        self.volumes = []
        self.last_lua_config = None

    def set_current_snapshot(self, service_id, snapshot_id, ctime):
        self.curr_snapshot_ids[service_id] = snapshot_id
        self.curr_snapshot_ctimes[service_id] = ctime
        self.snapshot_states[(service_id, snapshot_id)] = False

    def mark_active(self, service_id, snapshot_id):
        self.snapshot_states[(service_id, snapshot_id)] = True

    # Stubs:

    def save_config_to_snapshot(self, ctx, service_id, to_vector, from_vector, config_bundle):
        self.last_lua_config = config_bundle.lua_config

        for cert_spec_pb in six.itervalues(config_bundle.new_cert_spec_pbs):
            if cert_spec_pb.storage.type == model_pb2.CertificateSpec.Storage.NANNY_VAULT:
                self.volumes.append(
                    {
                        'name': 'some_secret_yav',
                        'type': 'VAULT_SECRET',
                        'vaultSecretVolume': {
                            'vaultSecret': {
                                'secretName': 'some_secret_yav',
                                'secretId': cert_spec_pb.storage.ya_vault_secret.secret_id,
                                'secretVer': cert_spec_pb.storage.ya_vault_secret.secret_ver,
                                'delegationToken': cert_spec_pb.storage.ya_vault_secret.delegation_token,
                            },
                        },
                    })
            else:
                self.volumes.append({
                    'name': 'some_secret_nanny',
                    'type': 'SECRET',
                    'secretVolume': {
                        'secretName': 'some_secret_nanny',
                        'keychainSecret': {
                            'keychainId': cert_spec_pb.storage.nanny_vault_secret.keychain_id,
                            'secretId': cert_spec_pb.storage.nanny_vault_secret.secret_id,
                            'secretRevisionId': cert_spec_pb.storage.nanny_vault_secret.secret_revision_id,
                        },
                    },
                })
        return self.curr_snapshot_ids[service_id], self.curr_snapshot_ctimes[service_id]

    def has_snapshot_been_active(self, service_id, snapshot_id):
        return self.snapshot_states.get((service_id, snapshot_id), False)


resolver_stub_1 = ResolverStub()
resolver_stub_2 = ResolverStub()
resolver_stub_3 = ResolverStub()
nanny_stub = NannyStub()


@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(BackendCtl, '_resolve', side_effect=resolver_stub_1)
def test_awacs_491(ctx, ctlrunner, checker, cache):
    # reset fixtures in case of flaky run
    resolver_stub_1.__init__()
    nanny_stub.__init__()

    namespace_id = 'test_namespace'
    balancer_id = 'test_balancer_sas'
    backend_id_1 = 'backend_1'
    backend_id_2 = 'backend_2'
    upstream_ids = ['upstream_1']
    upstream_backend_links = {
        'upstream_1': [backend_id_1, backend_id_2],
    }
    create_namespace_func(
        namespace_id=namespace_id,
        balancer_ids=[balancer_id],
        l3_balancer_ids=[],
        dns_record_ids=[],
        upstream_ids=upstream_ids,
        backend_ids=[backend_id_1, backend_id_2],
        knob_ids=[],
        cert_ids=[],
        domain_ids=[],
        balancer_upstream_links={},
        l3_balancer_backend_links={},
        dns_record_backend_links={},
        upstream_backend_links=upstream_backend_links,
        upstream_knob_links={},
        balancers_params={
            balancer_id: {
                'nanny_service_id': balancer_id,
                'mode': model_pb2.YandexBalancerSpec.EASY_MODE,
                'yp_cluster': 'SAS'
            },
        },
        cert_params={},
        backend_params={
            backend_id_2: {'is_system': True},
        },
        domain_params={}
    )
    check_included_backends(namespace_id, upstream_ids, upstream_backend_links)
    Api.pause_balancer_transport(namespace_id, balancer_id)
    ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(ctl)

    for backend_id in [backend_id_1, backend_id_2]:
        backend_ctl = BackendCtl(namespace_id=namespace_id, backend_id=backend_id)
        ctlrunner.run_ctl(backend_ctl)
        backend_ctl._process(ctx)

    for a in checker:
        with a:
            ctl._force_process(ctx)
            actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            assert m(actual.upstreams['upstream_1'].last_rev) == 'Can not remove last upstream from balancer'
            assert backend_id_1 in actual.backends
            assert backend_id_2 not in actual.backends

    check_included_backends(namespace_id, upstream_ids, upstream_backend_links)

    upstream_pb = Api.get_upstream(namespace_id, 'upstream_1')
    upstream_pb.spec.yandex_balancer.yaml = make_upstream_yml(backend_ids_to_include=[backend_id_1])
    Api.update_upstream(namespace_id, 'upstream_1', upstream_pb.meta.version, upstream_pb.spec)

    upstream_backend_links['upstream_1'] = [backend_id_1]
    check_included_backends(namespace_id, upstream_ids, upstream_backend_links)

    expected = L7State(
        balancer=[t_f_f],
        upstreams={
            u'upstream_1': [p_f_f, t_f_f],
        },
        backends={
            u'backend_1': [t_f_f],
        },
        endpoint_sets={
            u'backend_1': [t_f_f],
        }
    )

    for a in checker:
        with a:
            ctl._force_process(ctx)
            actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            assert actual == expected


@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
def test_cert_discoverability(ctx, ctlrunner, checker, cache):
    # reset fixtures in case of flaky run
    resolver_stub_1.__init__()
    nanny_stub.__init__()

    balancer_sas_id = 'test_balancer_sas'
    balancer_vla_id = 'test_balancer_vla'
    balancer_man_id = 'test_balancer_man'
    namespace_id = 'test_namespace'
    cert_1_id = 'cert_1'
    cert_2_id = 'cert_2'
    cert_3_id = 'cert_3'
    domain_1_id = 'domain_1'
    domain_2_id = 'domain_2'
    domain_3_id = 'domain_3'
    balancer_ids = [balancer_sas_id, balancer_vla_id, balancer_man_id]
    upstream_ids = ['upstream_1']
    upstream_backend_links = {
        'upstream_1': ['backend_1', 'backend_2'],
    }
    create_namespace_func(
        namespace_id=namespace_id,
        balancer_ids=balancer_ids,
        l3_balancer_ids=[],
        dns_record_ids=[],
        upstream_ids=upstream_ids,
        backend_ids=['backend_1', 'backend_2'],
        knob_ids=[],
        cert_ids=[cert_1_id, cert_2_id],
        domain_ids=[domain_1_id, domain_2_id, domain_3_id],
        balancer_upstream_links={},
        l3_balancer_backend_links={},
        dns_record_backend_links={},
        upstream_backend_links=upstream_backend_links,
        upstream_knob_links={},
        balancers_params={
            balancer_sas_id: {
                'nanny_service_id': balancer_sas_id,
                'mode': model_pb2.YandexBalancerSpec.EASY_MODE,
                'yp_cluster': 'SAS'
            },
            balancer_man_id: {
                'nanny_service_id': balancer_man_id,
                'mode': model_pb2.YandexBalancerSpec.EASY_MODE,
                'yp_cluster': 'MAN'
            },
            balancer_vla_id: {
                'nanny_service_id': balancer_vla_id,
                'mode': model_pb2.YandexBalancerSpec.EASY_MODE,
                'yp_cluster': 'VLA'
            },
        },
        cert_params={
            cert_1_id: {
                'domains': ('1st.my.y-t.ru', '2nd.my.y-t.ru'),
                'type': 'yav',
                'incomplete': False
            },
            cert_2_id: {
                'domains': ('1st.our.y-t.ru',),
                'type': 'yav',
                'incomplete': False
            },
        },
        backend_params={
            'backend_1': {'is_sd': True},
            'backend_2': {'is_sd': True},
        },
        domain_params={
            domain_1_id: {
                'fqdns': ('1st.my.y-t.ru', '2nd.my.y-t.ru'),
                'cert_id': cert_1_id,
                'upstream_id': 'upstream_1',
                'incomplete': False
            },
            domain_2_id: {
                'fqdns': ('1st.our.y-t.ru',),
                'cert_id': cert_2_id,
                'upstream_id': 'upstream_1',
                'incomplete': False
            },
            domain_3_id: {
                'fqdns': (u'kek.ru',),
                'cert_id': cert_3_id,
                'upstream_id': 'upstream_1',
                'incomplete': False
            },
        }
    )
    check_included_backends(namespace_id, upstream_ids, upstream_backend_links)

    ctls = []

    Api.pause_balancer_transport(namespace_id, balancer_sas_id)
    balancer_sas_ctl = BalancerCtl(namespace_id, balancer_sas_id)
    ctlrunner.run_ctl(balancer_sas_ctl)
    ctls.append(balancer_sas_ctl)

    Api.pause_balancer_transport(namespace_id, balancer_man_id)
    balancer_man_ctl = BalancerCtl(namespace_id, balancer_man_id)
    ctlrunner.run_ctl(balancer_man_ctl)
    ctls.append(balancer_man_ctl)

    Api.pause_balancer_transport(namespace_id, balancer_vla_id)
    balancer_vla_ctl = BalancerCtl(namespace_id, balancer_vla_id)
    ctlrunner.run_ctl(balancer_vla_ctl)
    ctls.append(balancer_vla_ctl)

    for ctl in ctls:
        ctl._force_process(ctx)

    expected = L7State(
        balancer=[t_f_f],
        domains={
            domain_1_id: [t_f_f],
            domain_2_id: [t_f_f],
            domain_3_id: [f_f_f],  # invalidated due to absence of cert
        },
        certs={
            cert_1_id: [t_f_f],
            cert_2_id: [t_f_f],
        },
        upstreams={
            u'upstream_1': [t_f_f],
        },
        backends={
            u'backend_1': [t_f_f],
            u'backend_2': [t_f_f],
        }
    )
    for balancer_id in (balancer_sas_id, balancer_vla_id, balancer_man_id):
        for a in checker:
            with a:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                assert actual == expected

    def update_cert(cert_id, discoverable=True):
        cert_pb = Api.get_cert(namespace_id, cert_id)

        spec_pb = clone_pb(cert_pb.spec)
        spec_pb.storage.ya_vault_secret.secret_ver += 'x'

        meta_pb = clone_pb(cert_pb.meta)
        discoverability_pb = meta_pb.discoverability
        discoverability_pb.default.value = discoverable
        discoverability_pb.default.mtime.GetCurrentTime()

        return Api.update_cert(namespace_id=namespace_id,
                               cert_id=cert_id,
                               version=meta_pb.version,
                               meta_pb=meta_pb,
                               spec_pb=spec_pb)

    def make_cert_discoverable(cert_id, locations, make_discoverable_by_default=False):
        cert_pb = Api.get_cert(namespace_id, cert_id)

        meta_pb = clone_pb(cert_pb.meta)
        discoverability_pb = meta_pb.discoverability
        if discoverability_pb.default.value != make_discoverable_by_default:
            discoverability_pb.default.value = make_discoverable_by_default
            discoverability_pb.default.mtime.GetCurrentTime()
        for location in locations:
            per_location_cond_pb = discoverability_pb.per_location.values[location]
            per_location_cond_pb.value = True
            per_location_cond_pb.mtime.GetCurrentTime()

        return Api.update_cert_meta(namespace_id=namespace_id,
                                    cert_id=cert_id,
                                    meta_pb=meta_pb)

    update_cert(cert_1_id, discoverable=False)
    update_cert(cert_2_id, discoverable=False)

    make_cert_discoverable(cert_1_id, [u'SAS'])
    for ctl in ctls:
        ctl._force_process(ctx)

    def is_cert_change_valid(balancer_id, cert_id):
        l7_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        return l7_state.certs[cert_id] == [t_f_f, t_f_f]

    for a in checker:
        with a:
            assert is_cert_change_valid(balancer_sas_id, cert_1_id)
            assert not is_cert_change_valid(balancer_man_id, cert_1_id)
            assert not is_cert_change_valid(balancer_vla_id, cert_1_id)
            for balancer_id in balancer_ids:
                assert not is_cert_change_valid(balancer_id, cert_2_id)

    make_cert_discoverable(cert_2_id, [u'VLA'])
    for ctl in ctls:
        ctl._force_process(ctx)

    for a in checker:
        with a:
            assert len(cache.list_all_certs()) == 2

    cache.reset_proxy_counters_cache()
    m = dict(metrics.ROOT_REGISTRY.items())
    assert m['awacs-cache-indiscoverable-too-long-certs-counter_axxx'] == 0

    with freeze_time(datetime.datetime.utcnow() + cache.CERT_INDISCOVERABILITY_THRESHOLD * 2):
        cache.reset_proxy_counters_cache()
        m = dict(metrics.ROOT_REGISTRY.items())
        assert m['awacs-cache-indiscoverable-too-long-certs-counter_axxx'] == 2

    for a in checker:
        with a:
            assert is_cert_change_valid(balancer_sas_id, cert_1_id)
            assert not is_cert_change_valid(balancer_man_id, cert_1_id)
            assert not is_cert_change_valid(balancer_vla_id, cert_1_id)

            assert not is_cert_change_valid(balancer_sas_id, cert_2_id)
            assert not is_cert_change_valid(balancer_man_id, cert_2_id)
            assert is_cert_change_valid(balancer_vla_id, cert_2_id)

    make_cert_discoverable(cert_1_id, [u'VLA'])
    make_cert_discoverable(cert_2_id, [u'MAN'])
    create_cert(namespace_id, cert_3_id, (u'kek.ru',), storage_type=u'yav', incomplete=False,
                discoverable=False)

    for ctl in ctls:
        ctl._force_process(ctx)

    def is_domain_3_valid(balancer_id):
        l7_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        return l7_state.domains[domain_3_id] == [t_f_f]

    for a in checker:
        with a:
            assert is_cert_change_valid(balancer_sas_id, cert_1_id)
            assert not is_cert_change_valid(balancer_man_id, cert_1_id)
            assert is_cert_change_valid(balancer_vla_id, cert_1_id)

            assert not is_cert_change_valid(balancer_sas_id, cert_2_id)
            assert is_cert_change_valid(balancer_man_id, cert_2_id)
            assert is_cert_change_valid(balancer_vla_id, cert_2_id)

            for balancer_id in balancer_ids:
                assert not is_domain_3_valid(balancer_id)

    make_cert_discoverable(cert_3_id, [u'MAN'])
    for ctl in ctls:
        ctl._force_process(ctx)

    for a in checker:
        with a:
            assert is_cert_change_valid(balancer_sas_id, cert_1_id)
            assert not is_cert_change_valid(balancer_man_id, cert_1_id)
            assert is_cert_change_valid(balancer_vla_id, cert_1_id)

            assert not is_cert_change_valid(balancer_sas_id, cert_2_id)
            assert is_cert_change_valid(balancer_man_id, cert_2_id)
            assert is_cert_change_valid(balancer_vla_id, cert_2_id)

            assert not is_domain_3_valid(balancer_sas_id)
            assert is_domain_3_valid(balancer_man_id)
            assert not is_domain_3_valid(balancer_vla_id)

    make_cert_discoverable(cert_1_id, [], make_discoverable_by_default=True)
    make_cert_discoverable(cert_2_id, [], make_discoverable_by_default=True)
    make_cert_discoverable(cert_3_id, [], make_discoverable_by_default=True)
    for ctl in ctls:
        ctl._force_process(ctx)

    cache.reset_proxy_counters_cache()
    m = dict(metrics.ROOT_REGISTRY.items())
    assert m['awacs-cache-indiscoverable-too-long-certs-counter_axxx'] == 0

    for a in checker:
        with a:
            assert is_cert_change_valid(balancer_sas_id, cert_1_id)
            assert is_cert_change_valid(balancer_man_id, cert_1_id)
            assert is_cert_change_valid(balancer_vla_id, cert_1_id)

            assert is_cert_change_valid(balancer_sas_id, cert_2_id)
            assert is_cert_change_valid(balancer_man_id, cert_2_id)
            assert is_cert_change_valid(balancer_vla_id, cert_2_id)

            assert is_domain_3_valid(balancer_sas_id)
            assert is_domain_3_valid(balancer_man_id)
            assert is_domain_3_valid(balancer_vla_id)


@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
def test_expired_soon_certs(ctx, ctlrunner, checker, cache):
    # reset fixtures in case of flaky run
    resolver_stub_1.__init__()
    nanny_stub.__init__()

    namespace_id = 'test_namespace'
    cert_1_id = 'cert_1'
    cert_2_id = 'cert_2'
    cert_3_id = 'cert_3'
    create_namespace_func(
        namespace_id=namespace_id,
        balancer_ids=[],
        l3_balancer_ids=[],
        dns_record_ids=[],
        upstream_ids=[],
        backend_ids=[],
        knob_ids=[],
        cert_ids=[cert_1_id, cert_2_id, cert_3_id],
        domain_ids=[],
        balancer_upstream_links={},
        l3_balancer_backend_links={},
        dns_record_backend_links={},
        upstream_backend_links={},
        upstream_knob_links={},
    )

    def update_cert(cert_id, not_after):
        cert_pb = Api.get_cert(namespace_id, cert_id)
        spec_pb = clone_pb(cert_pb.spec)
        spec_pb.fields.validity.not_after.FromDatetime(not_after)
        return Api.update_cert(namespace_id=namespace_id,
                               cert_id=cert_id,
                               version=cert_pb.meta.version,
                               meta_pb=cert_pb.meta,
                               spec_pb=spec_pb)

    update_cert(cert_1_id, datetime.datetime.utcnow() + datetime.timedelta(hours=168))  # Expires in a week - OK
    update_cert(cert_2_id, datetime.datetime.utcnow() + datetime.timedelta(hours=24))  # Expires in a day - CRIT
    update_cert(cert_3_id, datetime.datetime.utcnow() - datetime.timedelta(hours=24))  # Expired - CRIT!!!

    cache.reset_proxy_counters_cache()
    m = dict(metrics.ROOT_REGISTRY.items())
    assert m['awacs-cache-expires-soon-certs-counter_axxx'] == 2

    with freeze_time(datetime.datetime.utcnow() - datetime.timedelta(days=7)):
        cache.reset_proxy_counters_cache()
        m = dict(metrics.ROOT_REGISTRY.items())
        assert m['awacs-cache-expires-soon-certs-counter_axxx'] == 0

    with freeze_time(datetime.datetime.utcnow() + datetime.timedelta(days=7)):
        cache.reset_proxy_counters_cache()
        m = dict(metrics.ROOT_REGISTRY.items())
        assert m['awacs-cache-expires-soon-certs-counter_axxx'] == 3


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(BackendCtl, '_resolve', side_effect=resolver_stub_1)
@mock.patch.object(L3BalancerTransport, 'transport', return_value=False)
@mock.patch.object(L3BalancerTransport, 'poll_configs', return_value=False)
@mock.patch.object(L3BalancerValidator, '_validate_l3mgr_service_exists')
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
@mock.patch.object(NannyRpcMockClient, 'has_snapshot_been_active', side_effect=nanny_stub.has_snapshot_been_active)
def test_full(_1, _2, _3, _4, _5, ctx, ctlrunner, checker, ctl_version):
    # reset fixtures in case of flaky run
    resolver_stub_1.__init__()
    nanny_stub.__init__()

    nanny_service_id_1 = 's_1'
    nanny_service_id_2 = 's_2'
    l3_balancer_1_id = 'test_l3_balancer_1'
    l3_balancer_2_id = 'test_l3_balancer_2'
    balancer_1_id = 'test_balancer_1_sas'
    balancer_2_id = 'test_balancer_2_sas'
    namespace_id_1 = 'test_namespace_1'
    namespace_id_2 = 'test_namespace_2'
    knob_1_id = 'knob_1'
    knob_2_id = 'knob_2'
    cert_1_id = 'cert_1'
    cert_2_id = 'cert_2'

    upstream_backend_links = {
        'upstream_1': ['backend_1', 'backend_2'],
        'upstream_2': ['backend_3'],
    }
    create_namespace_func(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id, balancer_2_id],
        dns_record_ids=[],
        domain_ids=[],
        l3_balancer_ids=[l3_balancer_1_id, l3_balancer_2_id],
        upstream_ids=['upstream_1', 'upstream_2', 'upstream_3', 'upstream_4', 'upstream_5'],
        backend_ids=['backend_1', 'backend_2', 'backend_3', 'backend_4', 'backend_5', 'backend_6'],
        knob_ids=[knob_1_id, knob_2_id],
        balancer_upstream_links={
            balancer_1_id: ['upstream_1', 'upstream_2', 'upstream_3']
        },
        l3_balancer_backend_links={
            l3_balancer_1_id: ['backend_1', 'backend_5'],
            l3_balancer_2_id: ['backend_5'],
        },
        dns_record_backend_links={},
        upstream_backend_links=upstream_backend_links,
        upstream_knob_links={},
        balancers_params={
            balancer_1_id: {'nanny_service_id': nanny_service_id_1, 'ctl_version': ctl_version},
            balancer_2_id: {'nanny_service_id': nanny_service_id_2, 'ctl_version': ctl_version},
        },
        l3_balancers_params={
            l3_balancer_1_id: {'l3mgr_service_id': nanny_service_id_1},
            l3_balancer_2_id: {'l3mgr_service_id': nanny_service_id_2},
        },
        knob_params={
            knob_1_id: {'allowed_for': [balancer_1_id]},
            knob_2_id: {'allowed_for': [balancer_2_id]},
        },
        cert_ids=[cert_1_id, cert_2_id],
        cert_params={
            cert_1_id: {'domains': 'my1.y-t.ru', 'type': 'yav'},
            cert_2_id: {'domains': 'my2.y-t.ru', 'type': 'nanny'},
        },
    )

    upstream_ids = []
    upstream_backend_links = {}
    create_namespace_func(
        namespace_id=namespace_id_2,
        balancer_ids=[],
        l3_balancer_ids=[],
        dns_record_ids=[],
        domain_ids=[],
        upstream_ids=upstream_ids,
        backend_ids=['backend_1', 'backend_2'],
        knob_ids=[knob_1_id],
        balancer_upstream_links={},
        l3_balancer_backend_links={},
        dns_record_backend_links={},
        upstream_backend_links=upstream_backend_links,
        upstream_knob_links={},
        balancers_params={},
        cert_ids=[],
        backend_params={
            'backend_1': {'is_global': True},
            'backend_2': {'is_global': True},
        },
    )
    check_included_backends(namespace_id_2, upstream_ids, upstream_backend_links)

    state_pb = Api.get_balancer_state(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
    assert not state_pb.HasField('balancer')
    assert not state_pb.upstreams
    assert not state_pb.backends
    assert not state_pb.endpoint_sets

    Api.pause_balancer_transport(namespace_id_1, balancer_1_id)
    balancer_1_ctl = BalancerCtl(namespace_id_1, balancer_1_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)

    l3_balancer_1_ctl = L3BalancerCtl(namespace_id_1, l3_balancer_1_id)
    ctlrunner.run_ctl(l3_balancer_1_ctl)

    l3_balancer_2_ctl = L3BalancerCtl(namespace_id_1, l3_balancer_2_id)
    ctlrunner.run_ctl(l3_balancer_2_ctl)

    for a in checker:
        with a:
            actual_l3 = L3State.from_api(namespace_id_1, l3_balancer_1_id)
            expected_l3 = L3State(
                balancer=[l3_u_f_f],
                backends={
                    u'backend_1': [l3_f_f_f],
                    u'backend_5': [l3_f_f_f],
                }
            )
            assert (u'backend "backend_1" is not resolved yet' in
                    actual_l3.backends[u'backend_1'].last_rev.v_message)
            assert (u'backend "backend_5" is not resolved yet' in
                    actual_l3.backends[u'backend_5'].last_rev.v_message)
            assert actual_l3 == expected_l3

    for a in checker:
        with a:
            actual_l3 = L3State.from_api(namespace_id_1, l3_balancer_2_id)
            expected_l3 = L3State(
                balancer=[l3_u_f_f],
                backends={
                    u'backend_5': [l3_f_f_f],
                }
            )
            assert (u'backend "backend_5" is not resolved yet' in
                    actual_l3.backends[u'backend_5'].last_rev.v_message)
            assert actual_l3 == expected_l3

    for a in checker:
        with a:
            actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected = L7State(
                balancer=[t_f_f],
                upstreams={
                    u'upstream_1': [f_f_f],
                    u'upstream_2': [f_f_f],
                    u'upstream_3': [t_f_f],
                },
                backends={
                    u'backend_1': [f_f_f],
                    u'backend_2': [f_f_f],
                    u'backend_3': [f_f_f]
                }
            )
            assert actual == expected
            assert (m(actual.upstreams[u'upstream_1'].last_rev) ==
                    u'Upstream "upstream_1": some of the included backends are missing: '
                    u'"test_namespace_1/backend_1", "test_namespace_1/backend_2"')
            assert (m(actual.upstreams[u'upstream_2'].last_rev) ==
                    u'Upstream "upstream_2": some of the included backends are missing: "test_namespace_1/backend_3"')

    update_namespace(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id],
        balancer_upstream_links={
            balancer_1_id: ['upstream_3'],
        },
        balancer_ctl_version=ctl_version,
    )
    balancer_1_ctl._process(ctx)

    def check_1():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f],
            upstreams={
                'upstream_3': [t_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_1)

    backend_1_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_1')
    ctlrunner.run_ctl(backend_1_ctl)
    backend_5_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_5')
    ctlrunner.run_ctl(backend_5_ctl)

    update_namespace(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id],
        balancer_upstream_links={
            balancer_1_id: ['upstream_1', 'upstream_2', 'upstream_3'],
        },
        balancer_ctl_version=ctl_version,
    )
    balancer_1_ctl._process(ctx)

    ctx.log.info('Step 2')

    def check_2():
        actual_l3_state = L3State.from_api(namespace_id=namespace_id_1, l3_balancer_id=l3_balancer_1_id)
        expected_l3_state = L3State(
            balancer=[l3_t_f_f],
            backends={
                'backend_1': [l3_t_f_f],
                'backend_5': [l3_t_f_f],
            },
            endpoint_sets={
                'backend_1': [l3_t_f_f],
                'backend_5': [l3_t_f_f],
            }
        )
        assert actual_l3_state == expected_l3_state

        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f, t_f_f],
            upstreams={
                'upstream_1': [f_f_f],
                'upstream_2': [f_f_f],
                'upstream_3': [t_f_f]
            },
            backends={
                'backend_1': [u_f_f],
                'backend_2': [f_f_f],
                'backend_3': [f_f_f]
            },
            endpoint_sets={
                'backend_1': [u_f_f],
            }
        )
        assert actual_state == expected_state
        assert m(actual_state.upstreams['upstream_1'].last_rev) == \
               'Upstream "upstream_1": some of the included backends are missing: "test_namespace_1/backend_2"'
        assert m(actual_state.upstreams['upstream_2'].last_rev) == \
               'Upstream "upstream_2": some of the included backends are missing: "test_namespace_1/backend_3"'

    wait_until_passes(check_2)

    backend_2_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_2')
    ctlrunner.run_ctl(backend_2_ctl)
    balancer_1_ctl._process(ctx)

    def check_3():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f, t_f_f],
            upstreams={
                'upstream_1': [f_f_f],
                'upstream_2': [f_f_f],
                'upstream_3': [t_f_f]
            },
            backends={
                'backend_1': [u_f_f],
                'backend_2': [f_f_f],
                'backend_3': [f_f_f]
            },
            endpoint_sets={
                'backend_1': [u_f_f],
                'backend_2': [u_f_f],
            }
        )
        assert actual_state == expected_state
        assert m(actual_state.upstreams['upstream_2'].last_rev) == \
               'Upstream "upstream_2": some of the included backends are missing: "test_namespace_1/backend_3"'

    wait_until_passes(check_3)

    backend_3_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_3')
    ctlrunner.run_ctl(backend_3_ctl)
    balancer_1_ctl._process(ctx)

    def check_4():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f, t_f_f],
            upstreams={
                'upstream_1': [f_f_f],
                'upstream_2': [t_f_f],
                'upstream_3': [t_f_f]
            },
            backends={
                'backend_1': [u_f_f],
                'backend_2': [f_f_f],
                'backend_3': [t_f_f]
            },
            endpoint_sets={
                'backend_1': [u_f_f],
                'backend_2': [u_f_f],
                'backend_3': [t_f_f],
            }
        )
        assert actual_state == expected_state
        assert m(actual_state.upstreams['upstream_1'].last_rev) == (
            'instance_macro -> sections[local_ips] -> extended_http_macro -> regexp -> '
            'sections[upstream_1] -> modules[0] -> balancer2 -> generated_proxy_backends -> '
            'instances[1]: duplicate host and port: ya.ru:80')

    wait_until_passes(check_4)

    resolver_stub_1.increment_port()
    backend_pb = Api.get_backend(namespace_id_1, 'backend_2')
    backend_pb.spec.selector.port.override = 1000
    backend_pb.spec.selector.port.policy = backend_pb.spec.selector.port.OVERRIDE
    Api.update_backend(namespace_id_1, 'backend_2', backend_pb.meta.version, backend_pb.spec)

    backend_2_ctl._process(ctx)
    balancer_1_ctl._process(ctx)

    def check_5():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f, t_f_f],
            upstreams={
                'upstream_1': [t_f_f],
                'upstream_2': [t_f_f],
                'upstream_3': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
                'backend_2': [f_f_f, t_f_f],
                'backend_3': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
                'backend_2': [u_f_f, t_f_f],
                'backend_3': [t_f_f],
            }
        )
        assert actual_state == expected_state

    wait_until_passes(check_5)

    backend_pb = Api.get_backend(namespace_id_1, 'backend_2')
    backend_pb.spec.deleted = True
    Api.update_backend(namespace_id_1, 'backend_2', backend_pb.meta.version, backend_pb.spec)

    backend_2_ctl._process(ctx)
    balancer_1_ctl._process(ctx)

    def check_6():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f, t_f_f],
            upstreams={
                'upstream_1': [t_f_f],
                'upstream_2': [t_f_f],
                'upstream_3': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
                'backend_2': [f_f_f, t_f_f, f_f_f],
                'backend_3': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
                'backend_2': [u_f_f, t_f_f],
                'backend_3': [t_f_f],
            }
        )
        assert (m(actual_state.backends['backend_2'].last_rev) ==
                'Upstream "upstream_1": some of the included backends are missing: "test_namespace_1/backend_2"')
        assert actual_state == expected_state

    wait_until_passes(check_6)

    upstream_pb = Api.get_upstream(namespace_id_1, 'upstream_1')
    upstream_pb.spec.deleted = True
    Api.update_upstream(namespace_id_1, 'upstream_1', upstream_pb.meta.version, upstream_pb.spec)

    balancer_1_ctl._process(ctx)

    def check_7():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f, t_f_f],
            upstreams={
                'upstream_1': [t_f_f, t_f_f],
                'upstream_2': [t_f_f],
                'upstream_3': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
                'backend_2': [f_f_f, t_f_f, t_f_f],
                'backend_3': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
                'backend_2': [u_f_f, t_f_f],
                'backend_3': [t_f_f],
            }
        )
        assert actual_state == expected_state

    wait_until_passes(check_7)

    nanny_stub.set_current_snapshot(nanny_service_id_1, 'a', 1)
    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        balancer_1_ctl._process(ctx)

        def check_8():
            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_f, t_f_f, t_t_f],
                upstreams={
                    'upstream_1': [t_f_f, t_t_f],
                    'upstream_2': [t_t_f],
                    'upstream_3': [t_t_f],
                },
                backends={
                    'backend_1': [t_t_f],
                    'backend_2': [f_f_f, t_f_f, t_t_f],
                    'backend_3': [t_t_f],
                },
                endpoint_sets={
                    'backend_1': [t_t_f],
                    'backend_2': [u_f_f, t_t_f],
                    'backend_3': [t_t_f],
                }
            )
            assert actual_state == expected_state

        wait_until_passes(check_8)

        nanny_stub.mark_active(nanny_service_id_1, 'a')
        balancer_1_ctl._process(ctx)

        def check_9():
            assert 'upstream_2 = {' in nanny_stub.last_lua_config
            assert 'upstream_3 = {' in nanny_stub.last_lua_config
            for i in (1, 4, 5):
                assert 'upstream_{} = {{'.format(i) not in nanny_stub.last_lua_config
            for instance_pb in Api.get_endpoint_set(namespace_id_1, 'backend_3').spec.instances:
                assert '"{}"; {};', format(instance_pb.host, instance_pb.port) in nanny_stub.last_lua_config

            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_2': [t_f_t],
                    'upstream_3': [t_f_t]
                },
                backends={
                    'backend_3': [t_f_t],
                },
                endpoint_sets={
                    'backend_3': [t_f_t]
                }
            )
            assert actual_state == expected_state

        wait_until_passes(check_9)

    upstream_pb = Api.get_upstream(namespace_id_1, 'upstream_3')
    upstream_pb.spec.deleted = True
    Api.update_upstream(namespace_id_1, 'upstream_3', upstream_pb.meta.version, upstream_pb.spec)

    balancer_1_ctl._process(ctx)

    def check_10():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_2': [t_f_t],
                'upstream_3': [t_f_t, t_f_f]
            },
            backends={
                'backend_3': [t_f_t],
            },
            endpoint_sets={
                'backend_3': [t_f_t]
            }
        )
        assert actual_state == expected_state

    wait_until_passes(check_10)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        def check_11():
            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_2': [t_f_t],
                },
                backends={
                    'backend_3': [t_f_t],
                },
                endpoint_sets={
                    'backend_3': [t_f_t]
                }
            )
            assert actual_state == expected_state

        wait_until_passes(check_11)

    upstream_pb = Api.get_upstream(namespace_id_1, 'upstream_2')
    upstream_pb.spec.deleted = True
    Api.update_upstream(namespace_id_1, 'upstream_2', upstream_pb.meta.version, upstream_pb.spec)

    balancer_1_ctl._process(ctx)

    def check_12():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_2': [t_f_t, f_f_f],
            },
            backends={
                'backend_3': [t_f_t],
            },
            endpoint_sets={
                'backend_3': [t_f_t]
            }
        )
        assert actual_state == expected_state
        assert (m(actual_state.upstreams['upstream_2'].last_rev) ==
                'instance_macro -> sections[local_ips] -> extended_http_macro -> regexp: '
                'at least one of the "include_upstreams", "sections" must be specified')

    wait_until_passes(check_12)

    update_namespace(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id],
        balancer_upstream_links={
            balancer_1_id: ['upstream_4'],
        },
        balancer_ctl_version=ctl_version,
    )

    def check_13():
        balancer_1_ctl._process(ctx)
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t, t_f_f],
            upstreams={
                'upstream_2': [t_f_t, t_f_f],
                'upstream_4': [t_f_f],
            },
            backends={
                'backend_3': [t_f_t],
            },
            endpoint_sets={
                'backend_3': [t_f_t]
            }
        )
        assert actual_state == expected_state

    wait_until_passes(check_13)

    nanny_stub.set_current_snapshot(nanny_service_id_1, 'b', 2)
    with unpaused_balancer_transport(namespace_id_1, balancer_1_id, prevent_polling=True):
        def check_14():
            balancer_1_ctl._process(ctx)
            assert 'upstream_4 = {' in nanny_stub.last_lua_config
            for j in (1, 2, 3, 5):
                assert 'upstream_{} = {{'.format(j) not in nanny_stub.last_lua_config

            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t, t_t_f],
                upstreams={
                    'upstream_2': [t_f_t, t_t_f],
                    'upstream_4': [t_t_f],
                },
                backends={
                    'backend_3': [t_t_t],
                },
                endpoint_sets={
                    'backend_3': [t_t_t]
                }
            )
            assert actual_state == expected_state

        wait_until_passes(check_14, timeout=30)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        nanny_stub.mark_active(nanny_service_id_1, 'b')

        def check_15():
            balancer_1_ctl._process(ctx)
            assert 'upstream_4 = {' in nanny_stub.last_lua_config
            for i in (1, 2, 3, 5):
                assert 'upstream_{} = {{'.format(i) not in nanny_stub.last_lua_config

            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_4': [t_f_t],
                },
                backends={},
                endpoint_sets={},
            )
            assert actual_state == expected_state

        wait_until_passes(check_15, timeout=5)

    upstream_pb = Api.get_upstream(namespace_id_1, 'upstream_4')
    upstream_pb.spec.deleted = True
    Api.update_upstream(namespace_id_1, 'upstream_4', upstream_pb.meta.version, upstream_pb.spec)

    balancer_1_ctl._process(ctx)

    def check_16():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_4': [t_f_t, p_f_f],
            },
            backends={},
            endpoint_sets={},
        )
        assert actual_state == expected_state
        assert m(actual_state.upstreams['upstream_4'].last_rev) == 'Can not remove last upstream from balancer'

        with pytest.raises(NotFoundError):
            Api.get_upstream(balancer_1_id, 'upstream_3')

    wait_until_passes(check_16)

    update_namespace(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id],
        balancer_upstream_links={},
        balancer_ctl_version=ctl_version,
    )
    balancer_1_ctl._process(ctx)

    def check_17():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t, p_f_f],
            upstreams={
                'upstream_4': [t_f_t, p_f_f],
            },
            backends={},
            endpoint_sets={},
        )
        assert actual_state == expected_state
        assert (m(actual_state.balancer.last_rev) ==
                'Can not validate balancer spec due to absence of valid upstreams and backends')

    wait_until_passes(check_17)

    update_namespace(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id],
        balancer_upstream_links={
            balancer_1_id: ['upstream_5'],
        },
        balancer_ctl_version=ctl_version,
    )
    balancer_1_ctl._process(ctx)

    def check_18():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t, p_f_f, t_f_f],
            upstreams={
                'upstream_4': [t_f_t, t_f_f],
                'upstream_5': [t_f_f],
            },
            backends={},
            endpoint_sets={},
        )
        assert actual_state == expected_state

    wait_until_passes(check_18)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        def check_19():
            assert 'upstream_5 = {' in nanny_stub.last_lua_config
            for i in (1, 2, 3, 4):
                assert 'upstream_{} = {{'.format(i) not in nanny_stub.last_lua_config

            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_5': [t_f_t],
                },
                backends={},
                endpoint_sets={},
            )
            assert actual_state == expected_state

        wait_until_passes(check_19)

    backend_ids_to_include = [namespace_id_2 + '/backend_2']
    upstream_yml = make_upstream_yml(backend_ids_to_include=backend_ids_to_include)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
    Api.update_upstream(namespace_id=namespace_id_1,
                        upstream_id='upstream_5',
                        version=Api.get_upstream(namespace_id_1, 'upstream_5').meta.version,
                        spec_pb=upstream_spec_pb)

    upstream_backend_links = {'upstream_5': backend_ids_to_include}
    check_included_backends(namespace_id_1, ['upstream_5'], upstream_backend_links)

    balancer_1_ctl._process(ctx)

    def check_20():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_5': [t_f_t, f_f_f],
            },
            backends={
                'test_namespace_2/backend_2': [f_f_f],
            },
            endpoint_sets={},
        )
        assert actual_state == expected_state
        assert (m(actual_state.upstreams['upstream_5'].last_rev) ==
                'Upstream "upstream_5": some of the included backends are missing: "test_namespace_2/backend_2"')
        assert (m(actual_state.backends['test_namespace_2/backend_2'].last_rev) ==
                'Backend "test_namespace_2/backend_2" is not resolved yet. '
                'In some cases resolution can take up to a couple of minutes.')

    wait_until_passes(check_20)

    backend_2_2_ctl = BackendCtl(namespace_id=namespace_id_2, backend_id='backend_2')
    ctlrunner.run_ctl(backend_2_2_ctl)
    balancer_1_ctl._process(ctx)

    def check_21():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_5': [t_f_t, t_f_f],
            },
            backends={
                'test_namespace_2/backend_2': [t_f_f],
            },
            endpoint_sets={
                'test_namespace_2/backend_2': [t_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_21)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        def check_22():
            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_5': [t_f_t],
                },
                backends={
                    'test_namespace_2/backend_2': [t_f_t],
                },
                endpoint_sets={
                    'test_namespace_2/backend_2': [t_f_t],
                },
            )
            assert actual_state == expected_state

        wait_until_passes(check_22)

    backend_pb = Api.get_backend(namespace_id_2, 'backend_2')
    backend_pb.spec.deleted = True
    Api.update_backend(namespace_id_2, 'backend_2', backend_pb.meta.version, backend_pb.spec)

    balancer_1_ctl._process(ctx)

    def check_23():
        backend_2_2_ctl._maybe_self_delete(ctx)
        Api.get_backend(namespace_id_2, 'backend_2')  # still exists
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_5': [t_f_t],
            },
            backends={
                'test_namespace_2/backend_2': [t_f_t, f_f_f],
            },
            endpoint_sets={
                'test_namespace_2/backend_2': [t_f_t],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_23)

    backend_ids_to_include = []
    upstream_yml = make_upstream_yml(backend_ids_to_include=backend_ids_to_include)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
    Api.update_upstream(namespace_id=namespace_id_1,
                        upstream_id='upstream_5',
                        version=Api.get_upstream(namespace_id_1, 'upstream_5').meta.version,
                        spec_pb=upstream_spec_pb)

    upstream_backend_links = {'upstream_5': backend_ids_to_include}
    check_included_backends(namespace_id_1, ['upstream_5'], upstream_backend_links)

    balancer_1_ctl._process(ctx)

    def check_24():
        backend_2_2_ctl._maybe_self_delete(ctx)
        Api.get_backend(namespace_id_2, 'backend_2')  # still exists
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_5': [t_f_t, t_f_f],
            },
            backends={
                'test_namespace_2/backend_2': [t_f_t, t_f_f],
            },
            endpoint_sets={
                'test_namespace_2/backend_2': [t_f_t],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_24)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        def check_25():
            backend_2_2_ctl._maybe_self_delete(ctx)
            with pytest.raises(NotFoundError):
                Api.get_backend(namespace_id_2, 'backend_2')  # should be deleted

            assert 'upstream_5 = {' in nanny_stub.last_lua_config
            for i in (1, 2, 3, 4):
                assert 'upstream_{} = {{'.format(i) not in nanny_stub.last_lua_config

            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_5': [t_f_t],
                },
                backends={},
                endpoint_sets={},
            )
            assert actual_state == expected_state

        wait_until_passes(check_25)

    backend_pb = Api.get_backend(namespace_id_1, 'backend_1')
    backend_pb.spec.deleted = True
    Api.update_backend(namespace_id_1, 'backend_1', backend_pb.meta.version, backend_pb.spec)

    balancer_1_ctl._process(ctx)
    l3_balancer_1_ctl._process_empty_queue(ctx=ctx)
    backend_1_ctl._maybe_self_delete(ctx)

    def check_26():
        Api.get_backend(namespace_id_1, 'backend_1')  # still exists

        # here's why:
        actual_l3_state = L3State.from_api(namespace_id=namespace_id_1, l3_balancer_id=l3_balancer_1_id)
        expected_l3_state = L3State(
            balancer=[l3_t_f_f],
            backends={
                'backend_1': [l3_t_f_f, l3_f_f_f],
                'backend_5': [l3_t_f_f]
            },
            endpoint_sets={
                'backend_1': [l3_t_f_f],
                'backend_5': [l3_t_f_f]
            }
        )
        assert actual_l3_state == expected_l3_state

    wait_until_passes(check_26)

    Api.delete_l3_balancer(namespace_id_1, l3_balancer_1_id)

    def check_not_found():
        with pytest.raises(NotFoundError):
            Api.get_l3_balancer(namespace_id_1, l3_balancer_1_id)

    wait_until_passes(check_not_found)
    balancer_1_ctl._process(ctx)

    def check_27():
        backend_1_ctl._maybe_self_delete(ctx)
        with pytest.raises(NotFoundError):
            Api.get_backend(namespace_id_1, 'backend_1')  # should be deleted

        # let's get back to our `balancer_1_id`
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_5': [t_f_t],
            },
            backends={},
            endpoint_sets={},
        )
        assert actual_state == expected_state

    wait_until_passes(check_27)

    backend_6_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_6')
    ctlrunner.run_ctl(backend_6_ctl)

    # let's include our knob in upstream_5
    backend_ids_to_include = ['backend_6']
    upstream_yml = make_upstream_yml(backend_ids_to_include=backend_ids_to_include,
                                     knob_ids_to_include=['knob_1'])
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
    Api.update_upstream(namespace_id=namespace_id_1,
                        upstream_id='upstream_5',
                        version=Api.get_upstream(namespace_id_1, 'upstream_5').meta.version,
                        spec_pb=upstream_spec_pb)

    upstream_backend_links = {'upstream_5': backend_ids_to_include}
    check_included_backends(namespace_id_1, ['upstream_5'], upstream_backend_links)

    balancer_1_ctl._process(ctx)

    def check_28():
        # make sure knob is not included (cause we haven't allowed knobs yet)
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_5': [t_f_t, f_f_f],
            },
            backends={
                'backend_6': [u_f_f],
            },
            endpoint_sets={
                'backend_6': [u_f_f],
            },
        )
        assert (m(actual_state.upstreams['upstream_5'].last_rev) ==
                'instance_macro -> sections[local_ips] -> extended_http_macro -> regexp -> sections[upstream_5] -> '
                'modules[0] -> balancer2 -> attempts_file: knobs are not allowed for this balancer')
        assert actual_state == expected_state

    wait_until_passes(check_28)

    balancer_pb = Api.get_balancer(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
    balancer_pb.spec.validator_settings.knobs_mode = balancer_pb.spec.validator_settings.KNOBS_ALLOWED
    Api.update_balancer(namespace_id=namespace_id_1, balancer_id=balancer_1_id,
                        version=balancer_pb.meta.version, spec_pb=balancer_pb.spec)

    balancer_1_ctl._process(ctx)

    def check_29():
        # make sure knob is included
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_t, t_f_f],
            upstreams={
                'upstream_5': [t_f_t, t_f_f],
            },
            backends={
                'backend_6': [t_f_f],
            },
            endpoint_sets={
                'backend_6': [t_f_f],
            },
            knobs={
                'knob_1': [t_f_f],
            }
        )
        assert actual_state == expected_state

    wait_until_passes(check_29)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        wait_until(lambda: 'attempts_file = get_its_control_path("knob_1");' in nanny_stub.last_lua_config, timeout=1)

        # let's include incompatible knob in upstream_5
        upstream_yml = make_upstream_yml(backend_ids_to_include=['backend_6'],
                                         knob_ids_to_include=['knob_2'])
        upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
        Api.update_upstream(namespace_id=namespace_id_1,
                            upstream_id='upstream_5',
                            version=Api.get_upstream(namespace_id_1, 'upstream_5').meta.version,
                            spec_pb=upstream_spec_pb)

        upstream_backend_links = {'upstream_5': ['backend_6']}
        check_included_backends(namespace_id_1, ['upstream_5'], upstream_backend_links)

        balancer_1_ctl._process(ctx)

        def check_30():
            # make sure knob is not included and upstream is not valid
            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_5': [t_f_t, f_f_f],
                },
                backends={
                    'backend_6': [t_f_t],
                },
                endpoint_sets={
                    'backend_6': [t_f_t],
                },
                knobs={
                    'knob_1': [t_f_t],
                    'knob_2': [u_f_f],
                }
            )
            assert (m(actual_state.upstreams['upstream_5'].last_rev) ==
                    'knob "knob_2" does not match balancer "test_balancer_1_sas"')
            assert actual_state == expected_state

        wait_until_passes(check_30, timeout=3)

    backend_spec_pb = model_pb2.BackendSpec()
    backend_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    backend_spec_pb.selector.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='abc')
    Api.create_backend(
        namespace_id=namespace_id_1,
        backend_id='backend_7',
        spec_pb=backend_spec_pb)

    backend_7_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_7')
    ctlrunner.run_ctl(backend_7_ctl)

    # let's remove incompatible knob from upstream_5 and add new backend_7
    backend_ids_to_include = ['backend_6', 'backend_7']
    upstream_yml = make_upstream_yml(backend_ids_to_include=backend_ids_to_include,
                                     knob_ids_to_include=['knob_1'])
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
    Api.update_upstream(namespace_id=namespace_id_1,
                        upstream_id='upstream_5',
                        version=Api.get_upstream(namespace_id_1, 'upstream_5').meta.version,
                        spec_pb=upstream_spec_pb)

    upstream_backend_links = {'upstream_5': backend_ids_to_include}
    check_included_backends(namespace_id_1, ['upstream_5'], upstream_backend_links)

    balancer_1_ctl._process(ctx)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        def check_31():
            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_5': [t_f_t, f_f_f, f_f_f],
                },
                backends={
                    'backend_6': [t_f_t],
                    'backend_7': [u_f_f],
                },
                endpoint_sets={
                    'backend_6': [t_f_t],
                },
                knobs={
                    'knob_1': [t_f_t],
                }
            )
            assert (
                m(actual_state.upstreams['upstream_5'].last_rev) ==
                'YP_ENDPOINT_SETS_SD-backends ("backend_7") can not be used '
                'together with backends of different types ("backend_6")')
            assert actual_state == expected_state

        wait_until_passes(check_31)

    backend_pb = Api.get_backend(namespace_id_1, 'backend_6')
    backend_pb.spec.ClearField('selector')
    backend_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    backend_pb.spec.selector.yp_endpoint_sets.add(cluster='vla', endpoint_set_id='abc')
    Api.update_backend(namespace_id_1, 'backend_6', backend_pb.meta.version, backend_pb.spec)

    balancer_1_ctl._process(ctx)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        def check_32():
            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_5': [t_f_t, f_f_f, f_f_f],
                },
                backends={
                    'backend_6': [t_f_t, f_f_f],
                    'backend_7': [u_f_f],
                },
                endpoint_sets={
                    'backend_6': [t_f_t],
                },
                knobs={
                    'knob_1': [t_f_t],
                }
            )
            assert (
                m(actual_state.backends['backend_6'].last_rev) ==
                'instance_macro -> sections[local_ips] -> extended_http_macro -> regexp -> '
                'sections[upstream_5] -> modules[0] -> balancer2 -> generated_proxy_backends -> '
                'endpoint_sets: can only be used if preceded by l7_macro, '
                'instance_macro or main module with enabled SD'
            )
            assert actual_state == expected_state

        wait_until_passes(check_32)

    balancer_pb = Api.get_balancer(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
    instance_macro_pb = balancer_pb.spec.yandex_balancer.config.instance_macro
    sd_pb = instance_macro_pb.sd
    sd_pb.client_name = 'test-balancers'
    sd_pb.host = '127.0.9999'
    sd_pb.port = 8080
    instance_macro_pb.unistat.SetInParent()
    balancer_pb.spec.yandex_balancer.yaml = ''
    Api.update_balancer(namespace_id=namespace_id_1,
                        balancer_id=balancer_1_id,
                        version=balancer_pb.meta.version,
                        spec_pb=balancer_pb.spec)

    balancer_1_ctl._process(ctx)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        def check_33():
            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_5': [t_f_t],
                },
                backends={
                    'backend_6': [t_f_t],
                    'backend_7': [t_f_t],
                },
                endpoint_sets={
                    'backend_6': [t_f_t],  # should be removed actually
                },
                knobs={
                    'knob_1': [t_f_t],
                }
            )
            assert actual_state == expected_state

        wait_until_passes(check_33)

    backend_ids_to_include = ['backend_6']
    upstream_cfg = {
        'regexp_section': {
            'matcher': {'match_fsm': {'host': r'test\\.yandex-team\\.ru'}},
            'balancer2': {
                'dynamic': {
                    'max_pessimized_share': 0.2
                },
                'attempts': 2,
                'generated_proxy_backends': {
                    'proxy_options': {},
                    'include_backends': {'type': 'BY_ID', 'ids': backend_ids_to_include},
                },
            },
        },
    }
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
    Api.update_upstream(namespace_id=namespace_id_1,
                        upstream_id='upstream_5',
                        version=Api.get_upstream(namespace_id_1, 'upstream_5').meta.version,
                        spec_pb=upstream_spec_pb)

    upstream_backend_links = {'upstream_5': backend_ids_to_include}
    check_included_backends(namespace_id_1, ['upstream_5'], upstream_backend_links)

    balancer_1_ctl._process(ctx)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        def check_34():
            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_5': [t_f_t, f_f_f],
                },
                backends={
                    'backend_6': [t_f_t],
                    'backend_7': [t_f_t],
                },
                endpoint_sets={
                    'backend_6': [t_f_t],
                },
                knobs={
                    'knob_1': [t_f_t],
                }
            )
            assert (
                m(actual_state.upstreams['upstream_5'].last_rev) ==
                'instance_macro -> sections[local_ips] -> extended_http_macro -> regexp -> '
                'sections[upstream_5] -> balancer2 -> dynamic: can only be used if preceded by instance_macro '
                'or main module with state_directory, or l7_macro of version 0.0.3+'
            )
            assert actual_state == expected_state

    wait_until_passes(check_34)

    balancer_pb = Api.get_balancer(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
    balancer_pb.spec.yandex_balancer.config.instance_macro.state_directory = '/dev/shm/balancer-state'
    balancer_pb.spec.yandex_balancer.yaml = ''
    Api.update_balancer(namespace_id=namespace_id_1,
                        balancer_id=balancer_1_id,
                        version=balancer_pb.meta.version,
                        spec_pb=balancer_pb.spec)

    balancer_1_ctl._process(ctx)

    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        def check_35():
            actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'upstream_5': [t_f_t],
                },
                backends={
                    'backend_6': [t_f_t],
                },
                endpoint_sets={
                    'backend_6': [t_f_t],  # should be removed actually
                }
            )
            assert actual_state == expected_state

        wait_until_passes(check_35)


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(BackendCtl, '_resolve', side_effect=resolver_stub_2)
def test_global_backends(_1, ctx, ctlrunner, zk_storage, cache, ctl_version):
    balancer_1_1_id = 'test_balancer_1_sas'
    balancer_1_2_id = 'test_balancer_2_sas'
    balancer_2_1_id = 'test_balancer_3_sas'
    balancer_2_2_id = 'test_balancer_4_sas'
    namespace_id_1 = 'test_namespace_1'
    namespace_id_2 = 'test_namespace_2'

    create_namespace_func(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_1_id, balancer_1_2_id],
        l3_balancer_ids=[],
        dns_record_ids=[],
        domain_ids=[],
        upstream_ids=['upstream_1'],
        backend_ids=['backend_1', 'backend_2'],
        knob_ids=[],
        cert_ids=[],
        balancer_upstream_links={
            balancer_1_1_id: ['upstream_1']
        },
        l3_balancer_backend_links={},
        dns_record_backend_links={},
        upstream_backend_links={
            'upstream_1': ['backend_1', 'backend_2'],
        },
        upstream_knob_links={},
        balancers_params={
            balancer_1_1_id: {'nanny_service_id': 's_11', 'ctl_version': ctl_version},
            balancer_1_2_id: {'nanny_service_id': 's_21', 'ctl_version': ctl_version},
        },
    )

    backend_1_1_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_1')
    ctlrunner.run_ctl(backend_1_1_ctl)
    resolver_stub_2.increment_port()
    wait_until_passes(lambda: cache.must_get_endpoint_set(namespace_id_1, 'backend_1'))
    backend_1_2_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_2')
    ctlrunner.run_ctl(backend_1_2_ctl)
    resolver_stub_2.increment_port()
    wait_until_passes(lambda: cache.must_get_endpoint_set(namespace_id_1, 'backend_2'))
    balancer_1_ctl = BalancerCtl(namespace_id_1, balancer_1_1_id)

    ctx.log.info('Step 0: everything is valid')
    Api.pause_balancer_transport(namespace_id_1, balancer_1_1_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)

    def check_start():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_1_id)
        expected = L7State(
            balancer=[t_f_f],
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
                'backend_2': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
                'backend_2': [t_f_f],
            },
        )
        assert actual == expected

    wait_until_passes(check_start)

    create_namespace_func(
        namespace_id=namespace_id_2,
        balancer_ids=[balancer_2_1_id, balancer_2_2_id],
        l3_balancer_ids=[],
        upstream_ids=['upstream_1'],
        backend_ids=['backend_1', 'backend_2'],
        knob_ids=[],
        domain_ids=[],
        cert_ids=[],
        dns_record_ids=[],
        balancer_upstream_links={
            balancer_2_1_id: ['upstream_1']
        },
        l3_balancer_backend_links={},
        upstream_backend_links={
            'upstream_1': [namespace_id_1 + '/backend_1', namespace_id_1 + '/backend_2'],
        },
        dns_record_backend_links={},
        upstream_knob_links={},
        balancers_params={
            balancer_2_1_id: {'nanny_service_id': 's_3', 'ctl_version': ctl_version},
            balancer_2_2_id: {'nanny_service_id': 's_4', 'ctl_version': ctl_version},
        },
    )

    ctx.log.info('Step 1: non-global backends from another namespace')
    balancer_2_ctl = BalancerCtl(namespace_id_2, balancer_2_1_id)
    Api.pause_balancer_transport(namespace_id_2, balancer_2_1_id)
    ctlrunner.run_ctl(balancer_2_ctl)
    balancer_2_ctl._force_process(ctx)

    def check_non_global():
        actual_state = L7State.from_api(namespace_id=namespace_id_2, balancer_id=balancer_2_1_id)
        expected_state = L7State(
            balancer=[p_f_f],
            upstreams={
                'upstream_1': [f_f_f],
            },
            backends={
                namespace_id_1 + '/backend_1': [f_f_f],
                namespace_id_1 + '/backend_2': [f_f_f],
            },
            endpoint_sets={
                namespace_id_1 + '/backend_1': [u_f_f],
                namespace_id_1 + '/backend_2': [u_f_f],
            },
        )
        assert actual_state == expected_state
        assert (m(actual_state.upstreams['upstream_1'].last_rev) ==
                u'Upstream "upstream_1": some of the included backends are missing: '
                u'"test_namespace_1/backend_1", "test_namespace_1/backend_2"')

    wait_until_passes(check_non_global)

    ctx.log.info('Step 2: mark backends from another namespace as global')
    resolver_stub_2.__init__()  # so resolve results will be the same as at the start
    for backend_id in ('backend_1', 'backend_2'):
        backend_pb = cache.must_get_backend(namespace_id_1, backend_id)
        backend_pb.spec.is_global.value = True
        dao.IDao.instance().update_backend(namespace_id_1, backend_id,
                                           login='a', updated_spec_pb=backend_pb.spec, comment='-',
                                           version=backend_pb.meta.version)
        wait_until(lambda: cache.must_get_endpoint_set(namespace_id_1, backend_id).spec.is_global.value)
        resolver_stub_2.increment_port()

    balancer_1_ctl._process(ctx)
    balancer_2_ctl._process(ctx)

    def check_global():
        actual_state = L7State.from_api(namespace_id=namespace_id_2, balancer_id=balancer_2_1_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                namespace_id_1 + '/backend_1': [f_f_f, t_f_f],
                namespace_id_1 + '/backend_2': [f_f_f, t_f_f],
            },
            endpoint_sets={
                namespace_id_1 + '/backend_1': [u_f_f, t_f_f],
                namespace_id_1 + '/backend_2': [u_f_f, t_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_global)

    for backend_id in ('backend_1', 'backend_2'):
        backend_pb = Api.get_backend(namespace_id_1, backend_id)
        assert len(backend_pb.statuses) == 2
        per_balancer_statuses_pb = backend_pb.statuses[-1]
        assert len(per_balancer_statuses_pb.validated) == 2
        assert per_balancer_statuses_pb.validated[namespace_id_1 + ':' + balancer_1_1_id].status == 'True'
        assert per_balancer_statuses_pb.validated[namespace_id_2 + ':' + balancer_2_1_id].status == 'True'
        assert per_balancer_statuses_pb.in_progress[namespace_id_1 + ':' + balancer_1_1_id].status == 'False'
        assert per_balancer_statuses_pb.in_progress[namespace_id_2 + ':' + balancer_2_1_id].status == 'False'
        assert per_balancer_statuses_pb.active[namespace_id_1 + ':' + balancer_1_1_id].status == 'False'
        assert per_balancer_statuses_pb.active[namespace_id_2 + ':' + balancer_2_1_id].status == 'False'

    ctx.log.info('Step 3: mark backend_2 as deleted')
    backend_pb = Api.get_backend(namespace_id_1, 'backend_2')
    backend_pb.spec.deleted = True
    Api.update_backend(namespace_id_1, 'backend_2', backend_pb.meta.version, backend_pb.spec)

    balancer_1_ctl._process(ctx)

    def check_2():
        b_pb = Api.get_backend(namespace_id_1, 'backend_2')
        assert b_pb.spec.deleted
        assert len(b_pb.statuses) == 3
        per_b_statuses_pb = b_pb.statuses[-1]
        assert len(per_b_statuses_pb.validated) == 1
        assert per_b_statuses_pb.validated[namespace_id_1 + ':' + balancer_1_1_id].status == 'False'

    wait_until_passes(check_2)

    ctx.log.info('Step 4: replace backend_2 with backend_1')
    for ns_id in (namespace_id_1, namespace_id_2):
        upstream_yml = make_upstream_yml(backend_ids_to_include=[namespace_id_1 + '/backend_1'])
        upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
        Api.update_upstream(namespace_id=ns_id,
                            upstream_id='upstream_1',
                            version=Api.get_upstream(ns_id, 'upstream_1').meta.version,
                            spec_pb=upstream_spec_pb)
    balancer_1_ctl._process(ctx)
    balancer_2_ctl._process(ctx)

    def check_3():
        backend_1_2_ctl._maybe_self_delete(ctx)
        Api.get_backend(namespace_id_1,
                        'backend_2')  # should NOT be deleted: https://st.yandex-team.ru/AWACS-828#60b8d40fb2e6956bdbdf812f

        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_1_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'upstream_1': [t_f_f, t_f_f],
            },
            backends={
                'backend_1': [t_f_f, t_f_f],
                'backend_2': [t_f_f, t_f_f, t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f, t_f_f],
                'backend_2': [t_f_f, t_f_f],
            },
        )
        assert actual_state == expected_state

        actual_state = L7State.from_api(namespace_id=namespace_id_2, balancer_id=balancer_2_1_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'upstream_1': [t_f_f, t_f_f],
            },
            backends={
                namespace_id_1 + '/backend_1': [f_f_f, t_f_f],
                namespace_id_1 + '/backend_2': [f_f_f, t_f_f],
            },
            endpoint_sets={
                namespace_id_1 + '/backend_1': [u_f_f, t_f_f],
                namespace_id_1 + '/backend_2': [u_f_f, t_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_3, timeout=3)

    def check_4():
        nanny_stub.set_current_snapshot('s_11', 'a', 1)
        nanny_stub.mark_active('s_11', 'a')
        with unpaused_balancer_transport(namespace_id_1, balancer_1_1_id):
            balancer_1_ctl._force_process(ctx)

        nanny_stub.set_current_snapshot('s_3', 'b', 1)
        nanny_stub.mark_active('s_3', 'b')
        with unpaused_balancer_transport(namespace_id_2, balancer_2_1_id):
            balancer_2_ctl._force_process(ctx)

        backend_1_2_ctl._maybe_self_delete(ctx)
        with pytest.raises(NotFoundError):
            Api.get_backend(namespace_id_1, 'backend_2')  # should be deleted

        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_1': [t_f_t],
            },
            backends={
                'backend_1': [t_f_t],
            },
            endpoint_sets={
                'backend_1': [t_f_t],
            },
        )
        assert actual_state == expected_state

        actual_state = L7State.from_api(namespace_id=namespace_id_2, balancer_id=balancer_2_1_id)
        expected_state = L7State(
            balancer=[t_f_t],
            upstreams={
                'upstream_1': [t_f_t],
            },
            backends={
                namespace_id_1 + '/backend_1': [t_f_t],
            },
            endpoint_sets={
                namespace_id_1 + '/backend_1': [t_f_t],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_4, timeout=3)


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(BackendCtl, '_resolve', side_effect=resolver_stub_3)
def test_endpoint_sets(_1, ctx, ctlrunner, ctl_version):
    balancer_1_id = 'test_balancer_1_sas'
    balancer_2_id = 'test_balancer_2_sas'
    namespace_id_1 = 'test_namespace_1'

    create_namespace_func(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id, balancer_2_id],
        l3_balancer_ids=[],
        domain_ids=[],
        upstream_ids=['upstream_1'],
        backend_ids=['backend_1', 'backend_2'],
        knob_ids=[],
        cert_ids=[],
        dns_record_ids=[],
        balancer_upstream_links={
            balancer_1_id: ['upstream_1']
        },
        l3_balancer_backend_links={},
        upstream_backend_links={
            'upstream_1': ['backend_1', 'backend_2'],
            'upstream_2': ['backend_2'],
        },
        dns_record_backend_links={},
        upstream_knob_links={},
        balancers_params={
            balancer_1_id: {'nanny_service_id': 's_12', 'ctl_version': ctl_version},
            balancer_2_id: {'nanny_service_id': 's_22', 'ctl_version': ctl_version},
        },
        l3_balancers_params={},
    )

    Api.pause_balancer_transport(namespace_id_1, balancer_1_id)
    balancer_1_ctl = BalancerCtl(namespace_id_1, balancer_1_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)

    def check_start():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected = L7State(
            balancer=[p_f_f],
            upstreams={
                'upstream_1': [f_f_f],
            },
            backends={
                'backend_1': [f_f_f],
                'backend_2': [f_f_f],
            },
            endpoint_sets={},
        )
        assert actual == expected

    wait_until_passes(check_start)

    backend_1_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_1')
    ctlrunner.run_ctl(backend_1_ctl)

    resolver_stub_3.increment_port()
    backend_2_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_2')
    ctlrunner.run_ctl(backend_2_ctl)

    balancer_1_ctl._process(ctx)

    def check_1():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
                'backend_2': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
                'backend_2': [t_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_1)

    backend_pb = Api.get_backend(namespace_id=namespace_id_1, backend_id='backend_1')
    prev_backend_version = backend_pb.meta.version
    spec_pb = backend_pb.spec
    spec_pb.selector.Clear()
    spec_pb.selector.type = spec_pb.selector.MANUAL
    backend_pb = Api.update_backend(namespace_id=namespace_id_1,
                                    backend_id='backend_1',
                                    version=prev_backend_version,
                                    spec_pb=spec_pb)

    balancer_1_ctl._process(ctx)

    def check_2():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f, f_f_f],
                'backend_2': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
                'backend_2': [t_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_2)

    es_version = Api.get_endpoint_set(namespace_id=namespace_id_1, endpoint_set_id='backend_1').meta.version
    es_spec_pb = model_pb2.EndpointSetSpec()
    es_spec_pb.instances.add(
        host='yahoo.com',
        port=80,
        weight=1,
        ipv4_addr='8.8.8.8',
        ipv6_addr='::1'
    )
    Api.update_endpoint_set(namespace_id=namespace_id_1, endpoint_set_id='backend_1',
                            version=es_version,
                            backend_version=backend_pb.meta.version, spec_pb=es_spec_pb)

    balancer_1_ctl._process(ctx)

    def check_3():
        actual_state = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f, t_f_f],
                'backend_2': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f, t_f_f],
                'backend_2': [t_f_f],
            },
        )
        assert actual_state == expected_state
        backend_2_pb = Api.get_backend(namespace_id=namespace_id_1, backend_id='backend_2')
        with pytest.raises(ConflictError) as e:
            Api.create_endpoint_set(namespace_id=namespace_id_1, endpoint_set_id='backend_2',
                                    backend_version=backend_2_pb.meta.version, spec_pb=es_spec_pb)
        e.match('Backend type is not MANUAL, endpoint set can not be created.')

    wait_until_passes(check_3)


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(BackendCtl, '_resolve', return_value=ResolverStub())
def test_knobs(_1, ctx, ctlrunner, ctl_version):
    namespace_id = 'test_namespace'
    balancer_id = 'test_balancer_sas'
    upstream_id = 'test_upstream'

    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id, 4)

    nanny_service_id = 'nanny_service_id_for_' + balancer_id
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, BALANCER_YML_FOR_KNOBS, ctl_version=ctl_version)
    balancer_spec_pb.validator_settings.knobs_mode = balancer_spec_pb.validator_settings.KNOBS_ENABLED
    balancer_pb = Api.create_balancer(namespace_id=namespace_id, balancer_id=balancer_id, spec_pb=balancer_spec_pb)

    attempts_knob_id = 'attempts'
    knob_spec_pb = model_pb2.KnobSpec()
    knob_spec_pb.type = knob_spec_pb.INTEGER
    Api.create_knob(namespace_id=namespace_id, knob_id=attempts_knob_id, spec_pb=knob_spec_pb)

    switch_knob_id = 'switch'
    knob_spec_pb = model_pb2.KnobSpec()
    knob_spec_pb.type = knob_spec_pb.BOOLEAN
    Api.create_knob(namespace_id=namespace_id, knob_id=switch_knob_id, spec_pb=knob_spec_pb)

    upstream_cfg = {
        'regexp_section': {
            'matcher': {'match_fsm': {'host': r'test\\.yandex-team\\.ru'}},
            'balancer2': {
                'rr': {},
                'attempts': 2,
                'generated_proxy_backends': {
                    'proxy_options': {},
                    'include_backends': {'type': 'BY_ID', 'ids': ['backend_1']},
                },
            },
        },
    }
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
    upstream_pb = Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_id, spec_pb=upstream_spec_pb)

    Api.pause_balancer_transport(namespace_id, balancer_id)
    balancer_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._force_process(ctx)

    def check_1():
        actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected = L7State(
            balancer=[t_f_f],
            upstreams={
                'test_upstream': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            }
        )
        assert actual == expected

    wait_until_passes(check_1)

    upstream_balancer2 = upstream_cfg['regexp_section']['balancer2']  # type: dict
    upstream_balancer2['attempts_file'] = KTag('xxx')
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_upd_spec_pb = make_upstream_spec_pb(upstream_yml)
    upstream_pb = Api.update_upstream(
        namespace_id=namespace_id, upstream_id=upstream_id,
        version=upstream_pb.meta.version, spec_pb=upstream_upd_spec_pb)

    balancer_ctl._process(ctx)

    def check_2():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'test_upstream': [t_f_f, f_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
        )
        assert (m(actual_state.upstreams['test_upstream'].last_rev) ==
                'instance_macro -> sections[local_ips] -> extended_http_macro -> regexp -> sections[test_upstream] -> '
                'balancer2 -> attempts_file: knob "xxx" is missing')
        assert actual_state == expected_state

    wait_until_passes(check_2)

    # create a default knob with incorrect type (INTEGER instead of BOOLEAN)
    switch_knob_id = 'reset_dns_cache'
    knob_spec_pb = model_pb2.KnobSpec()
    knob_spec_pb.type = knob_spec_pb.INTEGER
    Api.create_knob(namespace_id=namespace_id, knob_id=switch_knob_id, spec_pb=knob_spec_pb)

    # include a regular knob with incorrect type
    upstream_balancer2['attempts_file'] = KTag('switch')
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_upd_spec_pb = make_upstream_spec_pb(upstream_yml)
    upstream_pb = Api.update_upstream(
        namespace_id=namespace_id, upstream_id=upstream_id,
        version=upstream_pb.meta.version, spec_pb=upstream_upd_spec_pb)

    balancer_ctl._process(ctx)

    def check_3():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        assert (m(actual_state.knobs['reset_dns_cache'].last_rev) ==
                'reset_dns_cache_file: expected type is BOOLEAN; actual type of knob "reset_dns_cache" is INTEGER')
        # TODO: the error message is misleading
        assert (m(actual_state.upstreams['test_upstream'].last_rev) ==
                'instance_macro -> sections[local_ips] -> extended_http_macro -> regexp -> sections[test_upstream] -> '
                'balancer2 -> attempts_file: knob "switch" is missing')
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'test_upstream': [t_f_f, f_f_f, f_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
            knobs={
                'reset_dns_cache': [f_f_f],
                'switch': [u_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_3)

    del upstream_balancer2['attempts_file']
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_upd_spec_pb = make_upstream_spec_pb(upstream_yml)
    upstream_pb = Api.update_upstream(
        namespace_id=namespace_id, upstream_id=upstream_id,
        version=upstream_pb.meta.version, spec_pb=upstream_upd_spec_pb)
    balancer_upd_spec_pb = clone_pb(balancer_pb.spec)
    balancer_upd_spec_pb.validator_settings.knobs_mode = balancer_upd_spec_pb.validator_settings.KNOBS_DISABLED
    balancer_pb = Api.update_balancer(namespace_id=namespace_id,
                                      balancer_id=balancer_id,
                                      version=balancer_pb.meta.version,
                                      spec_pb=balancer_upd_spec_pb)

    balancer_ctl._process(ctx)

    def check_4():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f],
            upstreams={
                'test_upstream': [t_f_f, f_f_f, f_f_f, t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            }
        )
        assert actual_state == expected_state

    wait_until_passes(check_4)

    # recreate default "reset_dns_cache" with correct type
    switch_knob_id = 'reset_dns_cache'
    Api.delete_knob(namespace_id, switch_knob_id)
    knob_spec_pb = model_pb2.KnobSpec()
    knob_spec_pb.type = knob_spec_pb.BOOLEAN
    Api.create_knob(namespace_id=namespace_id, knob_id=switch_knob_id, spec_pb=knob_spec_pb)

    # enable knobs
    balancer_upd_spec_pb = clone_pb(balancer_pb.spec)
    balancer_upd_spec_pb.validator_settings.knobs_mode = balancer_upd_spec_pb.validator_settings.KNOBS_ENABLED
    Api.update_balancer(namespace_id=namespace_id,
                        balancer_id=balancer_id,
                        version=balancer_pb.meta.version,
                        spec_pb=balancer_upd_spec_pb)

    # make a correct reference
    upstream_balancer2['attempts_file'] = KTag('attempts')
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_upd_spec_pb = make_upstream_spec_pb(upstream_yml)
    Api.update_upstream(
        namespace_id=namespace_id, upstream_id=upstream_id,
        version=upstream_pb.meta.version, spec_pb=upstream_upd_spec_pb)

    balancer_ctl._process(ctx)

    def check_5():
        # everything's great again
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f, t_f_f],
            upstreams={
                'test_upstream': [t_f_f, f_f_f, f_f_f, t_f_f, t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
            knobs={
                'reset_dns_cache': [t_f_f],
                'attempts': [t_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_5)


@pytest.mark.parametrize('ctl_version', (0, 4,))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(BackendCtl, '_resolve', side_effect=ResolverStub())
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
@mock.patch.object(NannyRpcMockClient,
                   'has_snapshot_been_active', side_effect=nanny_stub.has_snapshot_been_active)
def test_certs(_1, _2, _3, ctx, ctlrunner, ctl_version):
    # reset fixtures in case of flaky run
    nanny_stub.__init__()

    namespace_id = 'test_namespace'
    balancer_id = 'test_balancer_sas'
    upstream_id = 'test_upstream'
    nanny_service_id = 's_23'
    cert_id = 'primary_cert'
    secondary_cert_id = 'secondary_cert'

    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id, 4)
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, BALANCER_YAML_FOR_CERTS, ctl_version=ctl_version)
    Api.create_balancer(namespace_id=namespace_id, balancer_id=balancer_id, spec_pb=balancer_spec_pb)
    cert_pb = create_cert(namespace_id, cert_id, ('my1.y-t.ru',), storage_type='yav', incomplete=True)
    create_cert(namespace_id, secondary_cert_id, ('my1.y-t.ru',), storage_type='nanny', incomplete=False)
    upstream_cfg = {
        'regexp_section': {
            'matcher': {'match_fsm': {'host': r'test\\.yandex-team\\.ru'}},
            'ssl_sni': {
                'contexts': {
                    'default': {'cert': 'abc', 'priv': 'def'},
                },
                'modules': [{
                    'balancer2': {
                        'rr': {},
                        'attempts': 2,
                        'generated_proxy_backends': {
                            'proxy_options': {},
                            'include_backends': {'type': 'BY_ID', 'ids': ['backend_1']},
                        },
                    },
                }],
            },
        },
    }
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
    upstream_pb = Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_id, spec_pb=upstream_spec_pb)

    Api.pause_balancer_transport(namespace_id, balancer_id)
    balancer_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._force_process(ctx)

    def check_start():
        actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected = L7State(
            balancer=[t_f_f],
            upstreams={
                'test_upstream': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            }
        )
        assert actual == expected

    wait_until_passes(check_start)

    default_context = upstream_cfg['regexp_section']['ssl_sni']['contexts'].get('default', {})  # type: dict
    default_context['cert'] = CTag('xxx')
    del default_context['priv']
    upstream_upd_spec_pb = make_upstream_spec_pb(yaml.dump(upstream_cfg))
    upstream_pb = Api.update_upstream(namespace_id=namespace_id, upstream_id=upstream_id,
                                      version=upstream_pb.meta.version, spec_pb=upstream_upd_spec_pb)

    balancer_ctl._process(ctx)

    def check_1():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'test_upstream': [t_f_f, f_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
        )
        assert actual_state == expected_state
        assert (m(actual_state.upstreams['test_upstream'].last_rev) ==
                'instance_macro -> sections[local_ips] -> extended_http_macro -> regexp -> sections[test_upstream] -> '
                'ssl_sni -> contexts[default] -> cert: cert "xxx" is missing')

    wait_until_passes(check_1)

    # make a correct reference
    default_context['cert'] = CTag(cert_id)
    default_context['secondary'] = {'cert': CTag(secondary_cert_id)}
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_upd_spec_pb = make_upstream_spec_pb(upstream_yml)
    upstream_pb = Api.update_upstream(
        namespace_id=namespace_id, upstream_id=upstream_id,
        version=upstream_pb.meta.version, spec_pb=upstream_upd_spec_pb)

    # cert_1 is still incomplete
    balancer_ctl._process(ctx)

    def check_2():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'test_upstream': [t_f_f, f_f_f, f_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
            certs={
                secondary_cert_id: [u_f_f],
            },
        )
        assert actual_state == expected_state
        assert (m(actual_state.upstreams['test_upstream'].last_rev) ==
                'instance_macro -> sections[local_ips] -> extended_http_macro -> regexp -> sections[test_upstream] -> '
                'ssl_sni -> contexts[default] -> cert: cert "{}" is missing'.format(cert_id))

    wait_until_passes(check_2)

    # mark cert as complete
    cert_pb.spec.incomplete = False
    cert_pb = Api.update_cert(namespace_id=namespace_id,
                              cert_id=cert_id,
                              version=cert_pb.meta.version,
                              spec_pb=cert_pb.spec)

    balancer_ctl._process(ctx)

    def check_3():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'test_upstream': [t_f_f, f_f_f, t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
            certs={
                cert_id: [t_f_f],
                secondary_cert_id: [t_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_3)

    # test cert removal
    cert_pb.spec.state = model_pb2.CertificateSpec.REMOVED_FROM_AWACS
    Api.update_cert(namespace_id=namespace_id,
                    cert_id=cert_id,
                    version=cert_pb.meta.version,
                    spec_pb=cert_pb.spec)

    balancer_ctl._process(ctx)

    def check_4():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                'test_upstream': [t_f_f, f_f_f, t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
            certs={
                cert_id: [t_f_f, f_f_f],
                secondary_cert_id: [t_f_f],
            },
        )
        assert actual_state == expected_state

    wait_until_passes(check_4)

    nanny_stub.set_current_snapshot(nanny_service_id, 'a', 1)
    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_ctl._process(ctx)

        def check_5():
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[t_t_f],
                upstreams={
                    'test_upstream': [t_f_f, f_f_f, t_t_f],
                },
                backends={
                    'backend_1': [t_t_f],
                },
                endpoint_sets={
                    'backend_1': [t_t_f],
                },
                certs={
                    cert_id: [t_t_f, f_f_f],
                    secondary_cert_id: [t_t_f],
                },
            )
            assert actual_state == expected_state

        wait_until_passes(check_5)

        nanny_stub.mark_active(nanny_service_id, 'a')
        balancer_ctl._process(ctx)

        def check_6():
            assert cert_id in nanny_stub.last_lua_config
            assert secondary_cert_id in nanny_stub.last_lua_config
            assert any(v['name'] == 'some_secret_yav' for v in nanny_stub.volumes)
            assert any(v['name'] == 'some_secret_nanny' for v in nanny_stub.volumes)

            # make vector active
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'test_upstream': [t_f_t],
                },
                backends={
                    'backend_1': [t_f_t],
                },
                endpoint_sets={
                    'backend_1': [t_f_t],
                },
                certs={
                    cert_id: [t_f_t, f_f_f],
                    secondary_cert_id: [t_f_t],
                },
            )
            assert actual_state == expected_state

        wait_until_passes(check_6)

        # now remove cert from upstream config and check that it's removed from balancer state
        default_context['cert'] = 'test_cert'
        default_context['priv'] = 'test_priv'
        upstream_yml = yaml.dump(upstream_cfg)
        upstream_upd_spec_pb = make_upstream_spec_pb(upstream_yml)
        Api.update_upstream(
            namespace_id=namespace_id, upstream_id=upstream_id,
            version=upstream_pb.meta.version, spec_pb=upstream_upd_spec_pb)

        balancer_ctl._process(ctx)

        def check_7():
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[t_f_t],
                upstreams={
                    'test_upstream': [t_f_t],
                },
                backends={
                    'backend_1': [t_f_t],
                },
                endpoint_sets={
                    'backend_1': [t_f_t],
                },
                certs={
                    secondary_cert_id: [t_f_t],
                },
            )
            assert actual_state == expected_state
            assert cert_id not in nanny_stub.last_lua_config

        wait_until_passes(check_7)


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(BackendCtl, '_resolve', side_effect=ResolverStub())
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
@mock.patch.object(NannyRpcMockClient, 'has_snapshot_been_active', side_effect=nanny_stub.has_snapshot_been_active)
def test_domains(_1, _2, _3, ctlrunner, ctx, ctl_version):
    nanny_service_id = 's_3'
    balancer_1_id = 'test_balancer_1_sas'
    namespace_id_1 = 'test_namespace_1'
    cert_1_id = 'cert_1'
    cert_2_id = 'cert_2'
    domain_1_id = 'domain_1'
    domain_2_id = 'domain_2'

    create_namespace_func(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id],
        dns_record_ids=[],
        domain_ids=[domain_1_id, domain_2_id],
        l3_balancer_ids=[],
        upstream_ids=['upstream_1', 'upstream_2'],
        backend_ids=['backend_1', 'backend_2'],
        knob_ids=[],
        balancer_upstream_links={},
        l3_balancer_backend_links={},
        dns_record_backend_links={},
        upstream_backend_links={
            'upstream_1': ['backend_1'],
            'upstream_2': ['backend_2'],
        },
        upstream_knob_links={},
        balancers_params={
            balancer_1_id: {
                'nanny_service_id': nanny_service_id,
                'mode': model_pb2.YandexBalancerSpec.EASY_MODE,
                'ctl_version': ctl_version
            },
        },
        l3_balancers_params={},
        knob_params={},
        cert_ids=[cert_1_id, cert_2_id],
        cert_params={
            cert_1_id: {'domains': ('1st.my.y-t.ru', '2nd.my.y-t.ru'), 'type': 'yav', 'incomplete': False},
            cert_2_id: {'domains': ('another.y-t.ru', '*.ya.ru'), 'type': 'nanny', 'incomplete': False},
        },
        domain_params={
            domain_1_id: {'fqdns': ('1st.my.y-t.ru', '2nd.my.y-t.ru'),
                          'cert_id': cert_1_id,
                          'upstream_id': 'upstream_1',
                          'incomplete': False},
            domain_2_id: {'fqdns': ('another.y-t.ru', '*.ya.ru'),
                          'cert_id': cert_2_id,
                          'upstream_id': 'upstream_2',
                          'incomplete': False},
        }
    )

    ctx.log.info('Step 0: both backends are not resolved, balancer is invalid')
    Api.pause_balancer_transport(namespace_id_1, balancer_1_id)
    balancer_1_ctl = BalancerCtl(namespace_id_1, balancer_1_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)

    def check_start():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected = L7State(
            balancer=[p_f_f],
            domains={
                'domain_1': [f_f_f],
                'domain_2': [f_f_f],
            },
            upstreams={
                'upstream_1': [f_f_f],
                'upstream_2': [f_f_f],
            },
            backends={
                'backend_1': [f_f_f],
                'backend_2': [f_f_f],
            },
            certs={
                'cert_1': [f_f_f],
                'cert_2': [f_f_f],
            }
        )
        assert actual == expected
        assert (m(actual.upstreams['upstream_1'].last_rev) ==
                u'Upstream "upstream_1": some of the included backends are missing: "test_namespace_1/backend_1"')
        assert (m(actual.upstreams['upstream_2'].last_rev) ==
                u'Upstream "upstream_2": some of the included backends are missing: "test_namespace_1/backend_2"')

    wait_until_passes(check_start)

    ctx.log.info('Step 1: backend_1 is resolved, balancer is valid')
    backend_1_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_1')
    ctlrunner.run_ctl(backend_1_ctl)
    backend_1_ctl._process(ctx)
    balancer_1_ctl._process(ctx)

    def check_resolve_backend_1():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected = L7State(
            balancer=[t_f_f],
            domains={
                'domain_1': [t_f_f],
                'domain_2': [f_f_f],
            },
            upstreams={
                'upstream_1': [t_f_f],
                'upstream_2': [f_f_f],
            },
            backends={
                'backend_1': [t_f_f],
                'backend_2': [f_f_f],
            },
            certs={
                'cert_1': [t_f_f],
                'cert_2': [f_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            }
        )
        assert actual == expected
        assert (m(actual.upstreams['upstream_2'].last_rev) ==
                u'Upstream "upstream_2": some of the included backends are missing: "test_namespace_1/backend_2"')

    wait_until_passes(check_resolve_backend_1, timeout=3)

    ctx.log.info('Step 2: backend_2 is resolved, balancer is valid')
    backend_2_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_2')
    ctlrunner.run_ctl(backend_2_ctl)
    backend_2_ctl._process(ctx)
    balancer_1_ctl._process(ctx)

    def check_resolve_backend_2():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected = L7State(
            balancer=[t_f_f],
            domains={
                'domain_1': [t_f_f],
                'domain_2': [t_f_f],
            },
            upstreams={
                'upstream_1': [t_f_f],
                'upstream_2': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
                'backend_2': [t_f_f],
            },
            certs={
                'cert_1': [t_f_f],
                'cert_2': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
                'backend_2': [t_f_f],
            }
        )
        assert actual == expected

    wait_until_passes(check_resolve_backend_2)

    domain_pb = Api.get_domain(namespace_id_1, domain_2_id)
    domain_pb.spec.deleted = True
    update_domain(namespace_id_1, domain_2_id, domain_pb.spec)

    balancer_1_ctl._process(ctx)

    def check_deleted_domain():
        actual_ = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected_ = L7State(
            balancer=[t_f_f],
            domains={
                'domain_1': [t_f_f],
                'domain_2': [t_f_f, t_f_f],
            },
            upstreams={
                'upstream_1': [t_f_f],
                'upstream_2': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
                'backend_2': [t_f_f],
            },
            certs={
                'cert_1': [t_f_f],
                'cert_2': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
                'backend_2': [t_f_f],
            }
        )
        assert actual_ == expected_

    wait_until_passes(check_deleted_domain)

    nanny_stub.set_current_snapshot(nanny_service_id, 'a', 1)
    with unpaused_balancer_transport(namespace_id_1, balancer_1_id):
        balancer_1_ctl._process(ctx)

        def check_in_progress():
            actual_ = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_ = L7State(
                balancer=[t_t_f],
                domains={
                    'domain_1': [t_t_f],
                    'domain_2': [t_f_f, t_t_f],
                },
                upstreams={
                    'upstream_1': [t_t_f],
                    'upstream_2': [t_t_f],
                },
                backends={
                    'backend_1': [t_t_f],
                    'backend_2': [t_t_f],
                },
                certs={
                    'cert_1': [t_t_f],
                    'cert_2': [t_t_f],
                },
                endpoint_sets={
                    'backend_1': [t_t_f],
                    'backend_2': [t_t_f],
                }
            )
            assert actual_ == expected_

        wait_until_passes(check_in_progress)

        nanny_stub.mark_active(nanny_service_id, 'a')
        balancer_1_ctl._process(ctx)

        def check_active():
            actual_ = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
            expected_ = L7State(
                balancer=[t_f_t],
                domains={
                    'domain_1': [t_f_t],
                },
                upstreams={
                    'upstream_1': [t_f_t],
                },
                backends={
                    'backend_1': [t_f_t],
                },
                certs={
                    'cert_1': [t_f_t],
                },
                endpoint_sets={
                    'backend_1': [t_f_t],
                }
            )
            assert actual_ == expected_

        wait_until_passes(check_active)


@pytest.mark.parametrize('ctl_version', (0, 4))
@mock.patch.object(BackendCtl, '_resolve', side_effect=ResolverStub())
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
@mock.patch.object(NannyRpcMockClient, 'has_snapshot_been_active', side_effect=nanny_stub.has_snapshot_been_active)
def test_https_domain_without_https(_1, _2, _3, cache, ctlrunner, ctx, ctl_version):
    nanny_service_id = 's_3'
    balancer_1_id = 'test_balancer_1_sas'
    namespace_id_1 = 'test_namespace_1'
    cert_1_id = 'cert_1'
    domain_1_id = 'domain_1'

    create_namespace_func(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id],
        dns_record_ids=[],
        domain_ids=[],
        l3_balancer_ids=[],
        upstream_ids=['upstream_1'],
        backend_ids=['backend_1'],
        knob_ids=[],
        balancer_upstream_links={},
        l3_balancer_backend_links={},
        dns_record_backend_links={},
        upstream_backend_links={
            'upstream_1': ['backend_1'],
        },
        upstream_knob_links={},
        balancers_params={
            balancer_1_id: {
                'nanny_service_id': nanny_service_id,
                'mode': model_pb2.YandexBalancerSpec.EASY_MODE,
                'ctl_version': ctl_version
            },
        },
        l3_balancers_params={},
        knob_params={},
        cert_ids=[cert_1_id],
        cert_params={
            cert_1_id: {'domains': ('1st.my.y-t.ru', '2nd.my.y-t.ru'), 'type': 'yav', 'incomplete': False},
        },
        domain_params={}
    )
    backend_1_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_1')
    ctlrunner.run_ctl(backend_1_ctl)
    backend_1_ctl._process(ctx)

    ctx.log.info('Step 0: no domains')
    Api.pause_balancer_transport(namespace_id_1, balancer_1_id)
    balancer_1_ctl = BalancerCtl(namespace_id_1, balancer_1_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)

    def check_start():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected = L7State(
            balancer=[t_f_f],
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
        )
        assert actual == expected

    wait_until_passes(check_start)

    ctx.log.info('Step 1: HTTPS domain without https:{} in balancer config')
    create_domain(namespace_id_1, domain_1_id,
                  fqdns=('1st.my.y-t.ru', '2nd.my.y-t.ru'),
                  cert_id=cert_1_id,
                  upstream_id='upstream_1',
                  incomplete=False)
    cache.must_get_domain(namespace_id_1, domain_1_id)
    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.1',
            'http': {},
            'announce_check_reply': {
                'url_re': '/ping',
            },
            'health_check_reply': {},
            'include_domains': {},
        },
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = upd_balancer_spec_pb.yandex_balancer.EASY_MODE
    balancer_pb = Api.update_balancer(namespace_id=namespace_id_1,
                                      balancer_id=balancer_1_id,
                                      version=Api.get_balancer(namespace_id_1, balancer_1_id).meta.version,
                                      spec_pb=upd_balancer_spec_pb)
    balancer_1_ctl._process(ctx)

    def check_no_https():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected = L7State(
            balancer=[t_f_f, f_f_f],
            domains={
                'domain_1': [u_f_f],
            },
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
        )
        assert actual == expected
        assert (m(actual.balancer.last_rev) == u'l7_macro -> https: must be set because HTTPS domains are present')

    wait_until_passes(check_no_https)

    ctx.log.info('Step 2: enable https:{} in balancer config')
    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.1',
            'http': {},
            'https': {},
            'announce_check_reply': {
                'url_re': '/ping',
            },
            'health_check_reply': {},
            'include_domains': {},
        },
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = upd_balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.update_balancer(namespace_id=namespace_id_1,
                        balancer_id=balancer_1_id,
                        version=balancer_pb.meta.version,
                        spec_pb=upd_balancer_spec_pb)
    balancer_1_ctl._force_process(ctx)

    def check_https():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected = L7State(
            balancer=[t_f_f, f_f_f, t_f_f],
            domains={
                'domain_1': [t_f_f],
            },
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
            certs={
                cert_1_id: [t_f_f],
            },
        )
        assert actual == expected

    wait_until_passes(check_https)


@mock.patch.object(BackendCtl, '_resolve', side_effect=ResolverStub())
def test_http_domain_with_global_https_redirect(_1, cache, ctlrunner, ctx):
    nanny_service_id = 's_4'
    balancer_1_id = 'test_balancer_1_sas'
    namespace_id_1 = 'test_namespace_1'
    cert_1_id = 'cert_1'
    cert_2_id = 'cert_2'
    domain_1_id = 'domain_1'
    domain_2_id = 'domain_2'

    create_namespace_func(
        namespace_id=namespace_id_1,
        balancer_ids=[balancer_1_id],
        dns_record_ids=[],
        domain_ids=[],
        l3_balancer_ids=[],
        upstream_ids=['upstream_1'],
        backend_ids=['backend_1'],
        knob_ids=[],
        balancer_upstream_links={},
        l3_balancer_backend_links={},
        dns_record_backend_links={},
        upstream_backend_links={
            'upstream_1': ['backend_1'],
        },
        upstream_knob_links={},
        balancers_params={
            balancer_1_id: {
                'nanny_service_id': nanny_service_id,
                'mode': model_pb2.YandexBalancerSpec.EASY_MODE,
                'ctl_version': 5
            },
        },
        l3_balancers_params={},
        knob_params={},
        cert_ids=[cert_1_id, cert_2_id],
        cert_params={
            cert_1_id: {'domains': ('1st.my.y-t.ru', '2nd.my.y-t.ru'), 'type': 'yav', 'incomplete': False},
            cert_2_id: {'domains': ('3rd.my.y-t.ru',), 'type': 'yav', 'incomplete': False},
        },
        domain_params={}
    )
    Api.pause_balancer_transport(namespace_id_1, balancer_1_id)
    backend_1_ctl = BackendCtl(namespace_id=namespace_id_1, backend_id='backend_1')
    ctlrunner.run_ctl(backend_1_ctl)
    backend_1_ctl._process(ctx)

    create_domain(namespace_id_1, domain_1_id,
                  fqdns=('1st.my.y-t.ru', '2nd.my.y-t.ru'),
                  cert_id=cert_1_id,
                  upstream_id='upstream_1',
                  incomplete=False)
    create_domain(namespace_id_1, domain_2_id,
                  fqdns=('3rd.my.y-t.ru',),
                  upstream_id='upstream_1',
                  incomplete=False)

    ctx.log.info(u'Step 1. Set up http.redirect_to_https and include HTTP-only domain')
    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.3.9',
            'http': {'redirect_to_https': {}},
            'https': {},
            'announce_check_reply': {
                'url_re': '/ping',
            },
            'health_check_reply': {},
            'include_domains': {},
        },
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=5)
    upd_balancer_spec_pb.yandex_balancer.mode = upd_balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.update_balancer(namespace_id=namespace_id_1,
                        balancer_id=balancer_1_id,
                        version=Api.get_balancer(namespace_id_1, balancer_1_id).meta.version,
                        spec_pb=upd_balancer_spec_pb)
    balancer_1_ctl = BalancerCtl(namespace_id_1, balancer_1_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._process(ctx)

    def check_error():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected = L7State(
            balancer=[t_f_f],
            domains={
                domain_1_id: [t_f_f],
                domain_2_id: [f_f_f],
            },
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
            certs={
                cert_1_id: [t_f_f],
            },
        )
        assert actual == expected
        assert (m(actual.domains[domain_2_id].last_rev) == u'l7_macro -> include_domains: "http.redirect_to_https" '
                                                           u'is configured, but some domains are HTTP-only '
                                                           u'and will be inaccessible: "domain_2"')

    wait_until_passes(check_error)

    ctx.log.info(u'Step 2. Update domain_2 to be HTTPS_ONLY')
    domain_pb = Api.get_domain(namespace_id_1, domain_2_id)
    domain_pb.spec.yandex_balancer.config.protocol = model_pb2.DomainSpec.Config.HTTPS_ONLY
    domain_pb.spec.yandex_balancer.config.cert.id = cert_2_id
    update_domain(namespace_id_1, domain_2_id, domain_pb.spec)

    balancer_1_ctl._force_process(ctx)

    def check():
        actual = L7State.from_api(namespace_id=namespace_id_1, balancer_id=balancer_1_id)
        expected = L7State(
            balancer=[t_f_f],
            domains={
                domain_1_id: [t_f_f],
                domain_2_id: [f_f_f, t_f_f],
            },
            upstreams={
                'upstream_1': [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
            certs={
                cert_1_id: [t_f_f],
                cert_2_id: [t_f_f],
            },
        )
        assert actual == expected

    wait_until_passes(check)


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(BackendCtl, '_resolve', side_effect=ResolverStub())
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
@mock.patch.object(NannyRpcMockClient, 'has_snapshot_been_active', side_effect=nanny_stub.has_snapshot_been_active)
def test_domains_migrate_from_upstreams(_1, _2, _3, ctlrunner, ctx, ctl_version):
    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id-0'
    upstream_id = 'upstream-1-id'
    domain_id = 'domain-1-id'
    domain_id_2 = 'domain-2-id'
    cert_id = 'cert-1-id'
    cert_id_2 = 'cert-2-id'
    create_empty_namespace(namespace_id)
    balancer_yml = '''l7_macro: {version: "0.0.1",http: {}}'''
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    balancer_pb = Api.create_balancer(namespace_id=namespace_id,
                                      balancer_id=balancer_id,
                                      spec_pb=balancer_spec_pb)

    Api.pause_balancer_transport(namespace_id, balancer_id)
    balancer_1_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)

    def check_start():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(balancer=[p_f_f])
        assert actual_state == expected_state

    wait_until_passes(check_start)

    upstream_cfg = {
        'regexp_section': {
            'matcher': {'match_fsm': {'host': r'test\\.yandex-team\\.ru'}},
            'errordocument': {'status': 200}
        },
    }
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
    upstream_spec_pb.labels['order'] = '01'
    Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_id, spec_pb=upstream_spec_pb)

    balancer_1_ctl._force_process(ctx)

    def check_upstream():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                upstream_id: [t_f_f],
            }
        )
        assert actual_state == expected_state

    wait_until_passes(check_upstream)

    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.1',
            'http': {},
            'https': {},
            'announce_check_reply': {
                'url_re': '/ping',
            },
            'health_check_reply': {},
            'include_domains': {},
        },
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    b_pb = Api.update_balancer(namespace_id=namespace_id,
                               balancer_id=balancer_id,
                               version=balancer_pb.meta.version,
                               spec_pb=upd_balancer_spec_pb)
    balancer_1_ctl._process(ctx)

    def check_include_domains_without_domains():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_f, f_f_f],
            upstreams={
                upstream_id: [t_f_f],
            }
        )
        assert (m(actual_state_.balancer.last_rev) == u'l7_macro -> include_domains: no domains found')
        assert actual_state_ == expected_state_

    wait_until_passes(check_include_domains_without_domains)

    ctx.log.info('Step 1: two domains, one with missing cert')
    create_domain(namespace_id, domain_id,
                  fqdns=['a.ya.ru', 'b.ya.ru'],
                  upstream_id=upstream_id,
                  cert_id=cert_id,
                  incomplete=False)
    create_domain(namespace_id, domain_id_2,
                  fqdns=['zzz.ya.ru'],
                  upstream_id=upstream_id,
                  cert_id=cert_id_2,
                  incomplete=False)
    create_cert(namespace_id, cert_id_2, domains=['zzz.ya.ru'], storage_type='yav', incomplete=False)
    balancer_1_ctl._process(ctx)

    def check_domain_without_cert():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_f, t_f_f],
            domains={
                domain_id: [f_f_f],
                domain_id_2: [t_f_f],
            },
            upstreams={
                upstream_id: [t_f_f],
            },
            certs={
                cert_id_2: [t_f_f],
            }
        )
        assert (m(actual_state_.domains[domain_id].last_rev) ==
                u'Domain "domain-1-id": some of the included certs are missing: "cert-1-id"')
        assert actual_state_ == expected_state_

    wait_until_passes(check_domain_without_cert)

    ctx.log.info('Step 2: create missing cert')
    create_cert(namespace_id, cert_id, domains=['a.ya.ru', 'b.ya.ru'], storage_type='yav', incomplete=False)
    balancer_1_ctl._process(ctx)

    def check_domain_with_cert():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_f, t_f_f],
            domains={
                domain_id: [t_f_f],
                domain_id_2: [t_f_f],
            },
            upstreams={
                upstream_id: [t_f_f],
            },
            certs={
                cert_id: [t_f_f],
                cert_id_2: [t_f_f],
            }
        )
        assert actual_state_ == expected_state_

    wait_until_passes(check_domain_with_cert)

    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.1',
            'http': {},
            'announce_check_reply': {
                'url_re': '/ping',
            },
            'health_check_reply': {},
            'include_domains': {},
        },
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    b_pb = Api.update_balancer(namespace_id=namespace_id,
                               balancer_id=balancer_id,
                               version=b_pb.meta.version,
                               spec_pb=upd_balancer_spec_pb)
    balancer_1_ctl._process(ctx)

    def check_https_error():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_f, t_f_f, f_f_f],
            domains={
                domain_id: [t_f_f],
                domain_id_2: [t_f_f],
            },
            upstreams={
                upstream_id: [t_f_f],
            },
            certs={
                cert_id: [t_f_f],
                cert_id_2: [t_f_f],
            }
        )
        assert actual_state_ == expected_state_
        assert m(actual_state_.balancer.last_rev) == u'l7_macro -> https: must be set because HTTPS domains are present'

    wait_until_passes(check_https_error)

    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.1',
            'http': {},
            'announce_check_reply': {
                'url_re': '/ping',
            },
            'health_check_reply': {},
        },
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    b_pb = Api.update_balancer(namespace_id=namespace_id,
                               balancer_id=balancer_id,
                               version=b_pb.meta.version,
                               spec_pb=upd_balancer_spec_pb)
    balancer_1_ctl._process(ctx)

    def check_without_domains():
        nanny_stub.set_current_snapshot(nanny_service_id, 'a', 1)
        nanny_stub.mark_active(nanny_service_id, 'a')
        with unpaused_balancer_transport(namespace_id, balancer_id):
            balancer_1_ctl._force_process(ctx)

        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_t],
            upstreams={
                upstream_id: [t_f_t],
            }
        )
        assert actual_state_ == expected_state_

    wait_until_passes(check_without_domains, timeout=3)

    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.1',
            'http': {},
            'announce_check_reply': {
                'url_re': '/ping',
            },
            'health_check_reply': {},
            'https': {'certs': [{'id': cert_id}]}
        },
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    b_pb = Api.update_balancer(namespace_id=namespace_id,
                               balancer_id=balancer_id,
                               version=b_pb.meta.version,
                               spec_pb=upd_balancer_spec_pb)
    balancer_1_ctl._process(ctx)

    def check_without_domains_with_https():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_t, t_f_f],
            upstreams={
                upstream_id: [t_f_t],
            },
            certs={
                cert_id: [t_f_f],
            }
        )
        assert actual_state_ == expected_state_

    wait_until_passes(check_without_domains_with_https)

    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.1',
            'http': {},
            'https': {},
            'announce_check_reply': {
                'url_re': '/ping',
            },
            'health_check_reply': {},
            'include_domains': {},
        },
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.update_balancer(namespace_id=namespace_id,
                        balancer_id=balancer_id,
                        version=b_pb.meta.version,
                        spec_pb=upd_balancer_spec_pb)
    balancer_1_ctl._process(ctx)

    def check_with_domains_again():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_t, t_f_f, t_f_f],
            domains={
                domain_id: [t_f_f],
                domain_id_2: [t_f_f],
            },
            upstreams={
                upstream_id: [t_f_t],
            },
            certs={
                cert_id: [t_f_f],
                cert_id_2: [t_f_f],
            }
        )
        assert actual_state_ == expected_state_

    wait_until_passes(check_with_domains_again)


@pytest.mark.parametrize('ctl_version', (0, 4))
@mock.patch.object(BackendCtl, '_resolve', side_effect=ResolverStub())
@mock.patch.object(NannyRpcMockClient, 'has_snapshot_been_active', side_effect=nanny_stub.has_snapshot_been_active)
def test_remove_last_upstream(_1, _2, ctlrunner, ctx, ctl_version):
    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id-0'
    upstream_1_id = 'upstream-1-id'
    upstream_2_id = 'upstream-2-id'
    backend_1_id = 'backend_1'
    backend_2_id = 'backend_2'
    create_namespace_func(
        namespace_id=namespace_id,
        balancer_ids=[balancer_id],
        dns_record_ids=[],
        domain_ids=[],
        l3_balancer_ids=[],
        upstream_ids=[upstream_1_id, upstream_2_id],
        backend_ids=[backend_1_id, backend_2_id],
        knob_ids=[],
        balancer_upstream_links={
            balancer_id: [upstream_1_id, upstream_2_id]
        },
        l3_balancer_backend_links={},
        dns_record_backend_links={},
        upstream_backend_links={
            upstream_1_id: [backend_1_id],
            upstream_2_id: [backend_2_id],
        },
        upstream_knob_links={},
        balancers_params={
            balancer_id: {
                'nanny_service_id': nanny_service_id,
                'mode': model_pb2.YandexBalancerSpec.EASY_MODE,
                'ctl_version': ctl_version
            },
        },
        l3_balancers_params={},
        knob_params={},
        cert_ids=[],
        cert_params={},
        domain_params={}
    )
    backend_1_ctl = BackendCtl(namespace_id=namespace_id, backend_id=backend_1_id)
    ctlrunner.run_ctl(backend_1_ctl)
    backend_1_ctl._process(ctx)

    backend_2_ctl = BackendCtl(namespace_id=namespace_id, backend_id=backend_2_id)
    ctlrunner.run_ctl(backend_2_ctl)
    backend_2_ctl._process(ctx)

    Api.pause_balancer_transport(namespace_id, balancer_id)
    balancer_1_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)

    ctx.log.info('Step 0: all valid')

    def check_start():
        balancer_1_ctl._process(ctx)
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f],
            upstreams={
                upstream_1_id: [t_f_f],
                upstream_2_id: [t_f_f],
            },
            backends={
                backend_1_id: [t_f_f],
                backend_2_id: [t_f_f],
            },
            endpoint_sets={
                backend_1_id: [t_f_f],
                backend_2_id: [t_f_f],
            }
        )
        assert actual_state == expected_state

    wait_until_passes(check_start)

    ctx.log.info('Step 1: update balancer spec so it has 2 versions')

    balancer_pb = Api.get_balancer(namespace_id, balancer_id)
    balancer_pb.spec.yandex_balancer.yaml += '\n'
    Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, spec_pb=balancer_pb.spec)

    def check_update_bal():
        balancer_1_ctl._process(ctx)
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f],
            upstreams={
                upstream_1_id: [t_f_f],
                upstream_2_id: [t_f_f],
            },
            backends={
                backend_1_id: [t_f_f],
                backend_2_id: [t_f_f],
            },
            endpoint_sets={
                backend_1_id: [t_f_f],
                backend_2_id: [t_f_f],
            }
        )
        assert actual_state == expected_state

    wait_until_passes(check_update_bal)

    ctx.log.info('Step 2: remove both upstreams')
    Api.remove_upstream(namespace_id, upstream_1_id, Api.get_upstream(namespace_id, upstream_1_id).meta.version)
    Api.remove_upstream(namespace_id, upstream_2_id, Api.get_upstream(namespace_id, upstream_2_id).meta.version)

    def check_remove_upstreams():
        balancer_1_ctl._process(ctx)
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(
            balancer=[t_f_f, t_f_f],
            upstreams={
                upstream_1_id: [t_f_f, t_f_f],
                upstream_2_id: [t_f_f, f_f_f],
            },
            backends={
                backend_1_id: [t_f_f],
                backend_2_id: [t_f_f],
            },
            endpoint_sets={
                backend_1_id: [t_f_f],
                backend_2_id: [t_f_f],
            }
        )
        assert actual_state == expected_state
        assert m(actual_state.upstreams[upstream_2_id].last_rev) == "l7_macro can't find any upstreams to include"

    wait_until_passes(check_remove_upstreams)


L7_MACRO_BALANCER_YML = """
l7_macro:
  version: 0.0.3
  http: {}
"""

L7_UPSTREAM_MACRO_BALANCER_YML = """
l7_upstream_macro:
  version: 0.0.1
  id: {id}
  matcher:
    {matcher}
  by_dc_scheme:
    dc_balancer:
      weights_section_id: 'hey2'
      method: BY_DC_WEIGHT
      attempt_all_dcs: true
    balancer:
      attempts: 2
      backend_timeout: 1s
      connect_timeout: 100ms
      do_not_retry_http_responses: true
      max_reattempts_share: 0.5
      max_pessimized_endpoints_share: 0.5
    dcs:
      - name: man
        backend_ids:
        - {backend_id}
    on_error:
      rst: true
"""

L7_FAST_UPSTREAM_YML = """
prefix_path_router_section:
  route: /adapter
  l7_fast_upstream_macro:
    id: adapter
    outer_balancing_options:
      attempts: 1
    inner_balancing_options:
      attempts: 2
      connect_timeout: 100ms
      backend_timeout: 28s
      fail_on_5xx: true
    destinations:
    - id: adapter
      include_backends:
        type: BY_ID
        ids:
        - backend_1
"""


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
def test_balancer_ctl_easy_mode(_1, ctx, ctlrunner, ctl_version):
    # reset fixtures in case of flaky run
    nanny_stub.__init__()

    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'
    upstream_1_id = 'upstream-1-id'
    upstream_2_id = 'slbping'
    upstream_3_id = 'default'
    upstream_4_id = 'fast'

    create_empty_namespace(namespace_id)
    backend_ids = create_resolved_manual_backends(namespace_id=namespace_id, n=1)

    balancer_yml = L7_MACRO_BALANCER_YML
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    balancer_pb = Api.create_balancer(namespace_id=namespace_id,
                                      balancer_id=balancer_id,
                                      spec_pb=balancer_spec_pb)

    Api.pause_balancer_transport(namespace_id, balancer_id)

    balancer_1_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)

    def check_start():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state = L7State(balancer=[p_f_f])
        assert actual_state == expected_state

    wait_until_passes(check_start)

    upstream_yml = L7_UPSTREAM_MACRO_BALANCER_YML.format(id=upstream_1_id, backend_id=backend_ids[0],
                                                         matcher='any: true')
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
    upstream_spec_pb.labels['order'] = '01'
    upstream_1_pb = Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_1_id,
                                        spec_pb=upstream_spec_pb,
                                        login='very-root-user')

    upstream_cfg_2 = {
        'regexp_section': {
            'matcher': {'match_fsm': {'host': r'test-2\\.yandex-team\\.ru'}},
            'errordocument': {'status': 200}
        },
    }
    upstream_yml_2 = yaml.dump(upstream_cfg_2)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml_2)
    upstream_spec_pb.labels['order'] = '00'
    upstream_2_pb = Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_2_id,
                                        spec_pb=upstream_spec_pb)

    balancer_1_ctl._force_process(ctx)

    def check_upstreams():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_f],
            upstreams={
                upstream_1_id: [t_f_f],
                upstream_2_id: [t_f_f],
            },
            backends={
                'backend_1': [t_f_f],
            },
            endpoint_sets={
                'backend_1': [t_f_f],
            },
        )
        assert actual_state_ == expected_state_

    wait_until_passes(check_upstreams)

    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.4',
            'http': {},
            'announce_check_reply': {
                'compat': {
                    'replaced_upstream_id': upstream_2_id,
                },
                'url_re': '/ping',
            },
        }
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    balancer_pb = Api.update_balancer(namespace_id=namespace_id,
                                      balancer_id=balancer_id,
                                      version=balancer_pb.meta.version,
                                      spec_pb=upd_balancer_spec_pb)

    nanny_stub.set_current_snapshot(balancer_spec_pb.config_transport.nanny_static_file.service_id, 'a', 1)
    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_1_ctl._process(ctx)

        def check():
            actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state_ = L7State(
                balancer=[t_f_t],
                upstreams={
                    upstream_1_id: [t_f_t],
                },
                backends={
                    'backend_1': [t_f_t],
                },
                endpoint_sets={
                    'backend_1': [t_f_t],
                },
            )
            assert actual_state_ == expected_state_

        wait_until_passes(check)
        with open(t('test_controllers/fixtures/test_balancer_ctl_easy_mode/expected-balancer-config.lua')) as f:
            expected_lua = f.read()
        lua = nanny_stub.last_lua_config
        if lua != expected_lua:
            with open(t('test_controllers/fixtures/test_balancer_ctl_easy_mode/expected-balancer-config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua

    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'http': {},
            'version': '0.0.4',
            'health_check_reply': {
                'compat': {
                    'replaced_upstream_id': upstream_1_id,
                },
            },
            'announce_check_reply': {
                'compat': {
                    'replaced_upstream_id': upstream_2_id,
                },
                'url_re': '/ping',
            },
        }
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    balancer_pb = Api.update_balancer(namespace_id=namespace_id,
                                      balancer_id=balancer_id,
                                      version=balancer_pb.meta.version,
                                      spec_pb=upd_balancer_spec_pb)

    balancer_1_ctl._force_process(ctx)

    def check():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_t, f_f_f],
            upstreams={
                upstream_1_id: [t_f_t],
            },
            backends={
                'backend_1': [t_f_t],
            },
            endpoint_sets={
                'backend_1': [t_f_t],
            },
        )
        assert actual_state_ == expected_state_
        assert m(actual_state_.balancer.last_rev) == u"l7_macro can't find any upstreams to include"

    wait_until_passes(check)

    upstream_cfg_3 = {
        'regexp_section': {
            'matcher': {},
            'errordocument': {'status': 200}
        },
    }
    upstream_yml_3 = yaml.dump(upstream_cfg_3)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml_3)
    upstream_spec_pb.labels['order'] = '100'
    Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_3_id, spec_pb=upstream_spec_pb)

    balancer_1_ctl._process(ctx)

    def check():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_t, t_f_f],
            upstreams={
                upstream_1_id: [t_f_t],
                upstream_3_id: [t_f_f],
            },
            backends={
                'backend_1': [t_f_t],
            },
            endpoint_sets={
                'backend_1': [t_f_t],
            },
        )
        assert actual_state_ == expected_state_

    wait_until_passes(check)

    upstream_spec_pb = make_upstream_spec_pb(L7_FAST_UPSTREAM_YML)
    upstream_spec_pb.labels['order'] = '090'
    upstream_4_pb = Api.create_upstream(
        namespace_id=namespace_id, upstream_id=upstream_4_id, spec_pb=upstream_spec_pb)

    balancer_1_ctl._process(ctx)

    def check():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_t, t_f_f],
            upstreams={
                upstream_1_id: [t_f_t],
                upstream_3_id: [t_f_f],
                upstream_4_id: [f_f_f],
            },
            backends={
                'backend_1': [t_f_t],
            },
            endpoint_sets={
                'backend_1': [t_f_t],
            },
        )
        assert actual_state_ == expected_state_
        assert (m(actual_state_.upstreams[upstream_4_id].last_rev) ==
                'can not attach upstream "fast" to balancer, it is not one of RegexpSection, L7UpstreamMacro')

    wait_until_passes(check)

    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.4',
            'http': {},
            'health_check_reply': {
            },
            'announce_check_reply': {
                'url_re': '/ping',
            },
        }
    })
    Api.remove_upstream(namespace_id=namespace_id, upstream_id=upstream_1_id,
                        version=upstream_1_pb.meta.version)
    Api.remove_upstream(namespace_id=namespace_id, upstream_id=upstream_2_id,
                        version=upstream_2_pb.meta.version)
    Api.remove_upstream(namespace_id=namespace_id, upstream_id=upstream_4_id,
                        version=upstream_4_pb.meta.version)

    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    balancer_pb = Api.update_balancer(namespace_id=namespace_id,
                                      balancer_id=balancer_id,
                                      version=balancer_pb.meta.version,
                                      spec_pb=upd_balancer_spec_pb)

    balancer_1_ctl._force_process(ctx)

    def check():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_t, t_f_f, t_f_f],
            upstreams={
                upstream_1_id: [t_f_t, t_f_f],
                upstream_3_id: [t_f_f],
                upstream_4_id: [f_f_f, t_f_f],
            },
            backends={
                'backend_1': [t_f_t],
            },
            endpoint_sets={
                'backend_1': [t_f_t],
            },
        )
        assert actual_state_ == expected_state_

    wait_until_passes(check)

    upd_balancer_yml = yaml.dump({
        'l7_macro': {
            'version': '0.0.1',
            'http': {},
            'health_check_reply': {
            },
            'announce_check_reply': {
                'url_re': '/ping',
            },
        }
    })
    upd_balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, upd_balancer_yml, ctl_version=ctl_version)
    upd_balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    balancer_pb = Api.update_balancer(namespace_id=namespace_id,
                                      balancer_id=balancer_id,
                                      version=balancer_pb.meta.version,
                                      spec_pb=upd_balancer_spec_pb)

    balancer_1_ctl._force_process(ctx)

    def check():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_t, t_f_f, t_f_f, t_f_f],
            upstreams={
                upstream_1_id: [t_f_t, t_f_f],
                upstream_3_id: [t_f_f],
                upstream_4_id: [f_f_f, t_f_f],
            },
            backends={
                'backend_1': [t_f_t],
            },
            endpoint_sets={
                'backend_1': [t_f_t],
            },
        )
        assert actual_state_ == expected_state_

    wait_until_passes(check)

    namespace_ctl = NamespaceCtl(namespace_id, {'name_prefix': ''})
    nanny_stub.set_current_snapshot(balancer_spec_pb.config_transport.nanny_static_file.service_id, 'b', 1)
    nanny_stub.mark_active(balancer_spec_pb.config_transport.nanny_static_file.service_id, 'b')
    with unpaused_balancer_transport(namespace_id, balancer_id):
        def check():
            namespace_ctl._process(ctx, events.BalancerStateUpdate(None, None))
            balancer_1_ctl._process(ctx)
            actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state_ = L7State(
                balancer=[t_f_t],
                upstreams={
                    upstream_3_id: [t_f_t],
                },
                backends={},
                endpoint_sets={},
            )
            assert actual_state_ == expected_state_

        wait_until_passes(check)

    upstream_yml = L7_UPSTREAM_MACRO_BALANCER_YML.format(id=upstream_1_id, backend_id=backend_ids[0],
                                                         matcher='url_re: "/ok"')
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
    upstream_spec_pb.labels['order'] = '01'
    upstream_1_pb = Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_1_id,
                                        spec_pb=upstream_spec_pb,
                                        login='very-root-user')

    balancer_1_ctl._force_process(ctx)

    def check():
        actual_state_ = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        expected_state_ = L7State(
            balancer=[t_f_t],
            upstreams={
                upstream_1_id: [f_f_f],
                upstream_3_id: [t_f_t],
            },
            backends={
                'backend_1': [u_f_f],
            },
            endpoint_sets={
                'backend_1': [u_f_f],
            },
        )
        assert actual_state_ == expected_state_
        assert (m(actual_state_.upstreams[upstream_1_id].last_rev) ==
                'instance_macro -> sections[http_section] -> extended_http_macro -> modules[0] -> regexp -> '
                'sections[upstream-1-id] -> l7_upstream_macro -> by_dc_scheme -> balancer: '
                'can only be used without "compat.method" if preceded by instance_macro or '
                'main module with state_directory, or l7_macro of version 0.0.3+')

    wait_until_passes(check)


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(component_service, 'check_and_complete_sandbox_resource')
@mock.patch.object(NannyRpcMockClient, 'has_snapshot_been_active', side_effect=nanny_stub.has_snapshot_been_active)
def test_components(_1, _2, ctx, ctlrunner, checker, ctl_version):
    # reset fixtures in case of flaky run
    nanny_stub.__init__()

    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    upstream_id = 'upstream-id'
    nanny_service_id = 'components_test'

    create_empty_namespace(namespace_id)
    balancer_yml = '''l7_macro: {version: "0.0.1",http: {}}'''
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    balancer_pb = Api.create_balancer(namespace_id=namespace_id,
                                      balancer_id=balancer_id,
                                      spec_pb=balancer_spec_pb)
    Api.pause_balancer_transport(namespace_id, balancer_id)

    upstream_cfg = {
        'regexp_section': {
            'matcher': {'match_fsm': {'path': '/'}},
            'modules': [{'errordocument': {'status': 504}}]
        },
    }
    upstream_yml = yaml.dump(upstream_cfg)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
    upstream_spec_pb.labels['order'] = '01'
    Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_id, spec_pb=upstream_spec_pb)

    balancer_1_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)
    nanny_stub.set_current_snapshot(nanny_service_id, 'a', 1)

    for check in checker:
        with check:
            actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected = L7State(
                balancer=[t_f_f],
                upstreams={
                    upstream_id: [t_f_f]
                }
            )
            assert actual == expected

    balancer_pb = Api.get_balancer(namespace_id, balancer_id)

    instancectl_pb = balancer_pb.spec.components.instancectl
    instancectl_pb.state = instancectl_pb.SET
    instancectl_pb.version = '2.1'

    with pytest.raises(NotFoundError):
        Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)

    Api.create_published_component(model_pb2.ComponentMeta.INSTANCECTL, '2.1')
    Api.create_published_component(model_pb2.ComponentMeta.INSTANCECTL, '2.7')
    Api.create_published_component(model_pb2.ComponentMeta.INSTANCECTL, '2.8')
    Api.create_published_component(model_pb2.ComponentMeta.PGINX_BINARY, '185-4')

    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_pb = Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)
        balancer_1_ctl._process(ctx)

        for check in checker:
            with check:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected = L7State(
                    balancer=[t_t_f, ] * 2,
                    upstreams={
                        upstream_id: [t_t_f]
                    }
                )
                assert actual == expected

    runtime_attrs_content = INannyClient.instance().last_runtime_attrs[nanny_service_id]['content']
    assert runtime_attrs_content['instance_spec']['instancectl']['version'] == '2.1'
    assert not runtime_attrs_content['resources']['sandbox_files']

    pginx_binary_pb = balancer_pb.spec.components.pginx_binary
    pginx_binary_pb.state = pginx_binary_pb.SET
    pginx_binary_pb.version = '185-4'
    instancectl_pb = balancer_pb.spec.components.instancectl
    instancectl_pb.state = instancectl_pb.UNKNOWN
    with pytest.raises(BadRequestError,
                       match='"spec.components.instancectl.version" must not be set if state is UNKNOWN'):
        Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)
    instancectl_pb.version = ''
    balancer_pb = Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)

    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_1_ctl._process(ctx)

        for check in checker:
            with check:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected = L7State(
                    balancer=[t_t_f, ] * 3,
                    upstreams={
                        upstream_id: [t_t_f]
                    }
                )
                assert actual == expected

    runtime_attrs_content = INannyClient.instance().last_runtime_attrs[nanny_service_id]['content']
    assert runtime_attrs_content['instance_spec']['instancectl']['version'] == '2.1'
    assert runtime_attrs_content['resources']['sandbox_files'] == [{
        'extract_path': '',
        'is_dynamic': False,
        'local_path': 'balancer',
        'resource_id': '123456789',
        'resource_type': 'BALANCER_EXECUTABLE',
        'task_id': '123456789',
        'task_type': 'BUILD_BALANCER_BUNDLE'
    }]

    instancectl_pb = balancer_pb.spec.components.instancectl
    instancectl_pb.state = instancectl_pb.SET
    instancectl_pb.version = '2.7'
    Api.create_published_component(model_pb2.ComponentMeta.JUGGLER_CHECKS_BUNDLE, '0.0.1')
    Api.create_published_component(model_pb2.ComponentMeta.GET_WORKERS_PROVIDER, '0.0.1')
    Api.create_published_component(model_pb2.ComponentMeta.INSTANCECTL_CONF, '0.0.1')
    for component_pb in (balancer_pb.spec.components.juggler_checks_bundle,
                         balancer_pb.spec.components.get_workers_provider,
                         balancer_pb.spec.components.instancectl_conf):
        component_pb.state = component_pb.SET
        component_pb.version = '0.0.1'
    pginx_binary_pb = balancer_pb.spec.components.pginx_binary
    pginx_binary_pb.state = pginx_binary_pb.REMOVED
    pginx_binary_pb.version = ''
    with mock.patch.object(validation.balancer, 'validate_balancer_component'):
        balancer_pb = Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)

    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_1_ctl._process(ctx)

        for check in checker:
            with check:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected = L7State(
                    balancer=[t_t_f, ] * 4,
                    upstreams={
                        upstream_id: [t_t_f]
                    }
                )
                assert actual == expected

    runtime_attrs_content = INannyClient.instance().last_runtime_attrs[nanny_service_id]['content']
    assert runtime_attrs_content['instance_spec']['instancectl']['version'] == '2.7'
    assert runtime_attrs_content['resources']['sandbox_files'] == [
        {
            'extract_path': '',
            'is_dynamic': False,
            'local_path': 'instancectl.conf',
            'resource_id': '123456789',
            'resource_type': 'RTC_MTN_BALANCER_INSTANCECTL_CONF',
            'task_id': '123456789',
            'task_type': 'NANNY_REMOTE_COPY_RESOURCE'
        },
        {
            'extract_path': '{JUGGLER_CHECKS_PATH}/juggler-check-bundle-rtc-balancers.tar.gz',
            'is_dynamic': False,
            'local_path': 'juggler-check-bundle-rtc-balancers.tar.gz',
            'resource_id': '123456789',
            'resource_type': 'JUGGLER_CHECKS_BUNDLE',
            'task_id': '123456789',
            'task_type': 'BUILD_JUGGLER_CHECKS_BUNDLE'
        },
        {
            'extract_path': '',
            'is_dynamic': False,
            'local_path': 'dump_json_get_workers_provider.lua',
            'resource_id': '123456789',
            'resource_type': 'GET_WORKERS_PROVIDER',
            'task_id': '123456789',
            'task_type': 'NANNY_REMOTE_COPY_RESOURCE'
        }
    ]

    Api.create_published_component(model_pb2.ComponentMeta.GET_WORKERS_PROVIDER, '0.0.2')
    balancer_pb.spec.components.get_workers_provider.version = '0.0.2'
    pginx_binary_pb = balancer_pb.spec.components.pginx_binary
    pginx_binary_pb.state = pginx_binary_pb.SET
    pginx_binary_pb.version = '185-4'
    balancer_pb = Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)

    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_1_ctl._process(ctx)

        for check in checker:
            with check:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected = L7State(
                    balancer=[t_t_f] * 5,
                    upstreams={
                        upstream_id: [t_t_f]
                    }
                )
                assert actual == expected

    runtime_attrs_content = INannyClient.instance().last_runtime_attrs[nanny_service_id]['content']
    assert not runtime_attrs_content['instance_spec'].get('containers')
    assert runtime_attrs_content['instance_spec']['instancectl']['version'] == '2.7'
    assert runtime_attrs_content['resources']['sandbox_files'] == [
        {
            'extract_path': '',
            'is_dynamic': False,
            'local_path': 'instancectl.conf',
            'resource_id': '123456789',
            'resource_type': 'RTC_MTN_BALANCER_INSTANCECTL_CONF',
            'task_id': '123456789',
            'task_type': 'NANNY_REMOTE_COPY_RESOURCE'
        },
        {
            'extract_path': '{JUGGLER_CHECKS_PATH}/juggler-check-bundle-rtc-balancers.tar.gz',
            'is_dynamic': False,
            'local_path': 'juggler-check-bundle-rtc-balancers.tar.gz',
            'resource_id': '123456789',
            'resource_type': 'JUGGLER_CHECKS_BUNDLE',
            'task_id': '123456789',
            'task_type': 'BUILD_JUGGLER_CHECKS_BUNDLE'
        },
        {
            'extract_path': '',
            'is_dynamic': False,
            'local_path': 'dump_json_get_workers_provider.lua',
            'resource_id': '123456789',
            'resource_type': 'GET_WORKERS_PROVIDER',
            'task_id': '123456789',
            'task_type': 'NANNY_REMOTE_COPY_RESOURCE'
        },
        {
            'extract_path': '',
            'is_dynamic': False,
            'local_path': 'balancer',
            'resource_id': '123456789',
            'resource_type': 'BALANCER_EXECUTABLE',
            'task_id': '123456789',
            'task_type': 'BUILD_BALANCER_BUNDLE'
        }
    ]

    Api.create_published_component(model_pb2.ComponentMeta.AWACSLET, '0.0.2')
    Api.create_published_component(model_pb2.ComponentMeta.AWACSLET, '0.0.3-pushclient')
    Api.create_published_component(model_pb2.ComponentMeta.AWACSLET_GET_WORKERS_PROVIDER, '0.0.0')

    awacslet_pb = balancer_pb.spec.components.awacslet
    awacslet_pb.state = awacslet_pb.SET
    awacslet_pb.version = '0.0.3-pushclient'

    awacslet_get_workers_provider_pb = balancer_pb.spec.components.awacslet_get_workers_provider
    awacslet_get_workers_provider_pb.state = awacslet_get_workers_provider_pb.SET
    awacslet_get_workers_provider_pb.version = '0.0.0'

    get_workers_provider_pb = balancer_pb.spec.components.get_workers_provider
    get_workers_provider_pb.state = get_workers_provider_pb.REMOVED
    get_workers_provider_pb.version = ''

    instancectl_conf_pb = balancer_pb.spec.components.instancectl_conf
    instancectl_conf_pb.state = instancectl_pb.REMOVED
    instancectl_conf_pb.version = ''

    instancectl_pb = balancer_pb.spec.components.instancectl
    instancectl_pb.state = instancectl_pb.SET
    instancectl_pb.version = u'2.8'

    balancer_pb.spec.ctl_version = 5
    balancer_pb = Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)

    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_1_ctl._process(ctx)

        for check in checker:
            with check:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected = L7State(
                    balancer=[t_t_f] * 6,
                    upstreams={
                        upstream_id: [t_t_f]
                    }
                )
                assert actual == expected

    runtime_attrs_content = INannyClient.instance().last_runtime_attrs[nanny_service_id]['content']

    # due to ctl_version == 5:
    instances = runtime_attrs_content['instances']
    assert instances['iss_settings']['hooks_time_limits']['iss_hook_stop']['max_execution_time'] == 100

    instance_spec = runtime_attrs_content['instance_spec']
    volumes = instance_spec['volume']
    assert len(volumes) == 1
    assert volumes[0]['name'] == 'controls'
    containers = instance_spec['containers']
    assert len(containers) == 2
    assert containers[0]['command'] == ['./awacslet', 'start', 'balancer']
    assert containers[1]['command'] == ['./awacslet', 'start', 'push-client']
    assert {u'task_type': u'YA_MAKE',
            u'task_id': u'123456789',
            u'resource_id': u'123456789',
            u'extract_path': '',
            u'is_dynamic': False,
            u'local_path': u'awacslet',
            u'resource_type': u'AWACSLET_BINARY'} in runtime_attrs_content[u'resources'][u'sandbox_files']

    awacslet_pb = balancer_pb.spec.components.awacslet
    awacslet_pb.state = awacslet_pb.SET
    awacslet_pb.version = '0.0.2'
    balancer_pb = Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)

    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_1_ctl._process(ctx)
        for check in checker:
            with check:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected = L7State(
                    balancer=[t_t_f] * 7,
                    upstreams={
                        upstream_id: [t_t_f]
                    }
                )
                assert actual == expected

    runtime_attrs_content = INannyClient.instance().last_runtime_attrs[nanny_service_id]['content']
    instance_spec = runtime_attrs_content['instance_spec']
    volumes = instance_spec['volume']
    assert len(volumes) == 1
    assert volumes[0]['name'] == 'controls'
    containers = instance_spec['containers']
    assert len(containers) == 1
    assert containers[0]['command'] == ['./awacslet', 'start']

    awacslet_pb = balancer_pb.spec.components.awacslet
    awacslet_pb.state = awacslet_pb.REMOVED
    awacslet_pb.version = ''

    awacslet_get_workers_provider_pb = balancer_pb.spec.components.awacslet_get_workers_provider
    awacslet_get_workers_provider_pb.state = awacslet_get_workers_provider_pb.REMOVED
    awacslet_get_workers_provider_pb.version = ''

    get_workers_provider_pb = balancer_pb.spec.components.get_workers_provider
    get_workers_provider_pb.state = get_workers_provider_pb.SET
    get_workers_provider_pb.version = '0.0.1'

    instancectl_conf_pb = balancer_pb.spec.components.instancectl_conf
    instancectl_conf_pb.state = instancectl_pb.SET
    instancectl_conf_pb.version = '0.0.1'

    balancer_pb = Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)
    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_1_ctl._process(ctx)
        for check in checker:
            with check:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected = L7State(
                    balancer=[t_t_f] * 8,
                    upstreams={
                        upstream_id: [t_t_f]
                    }
                )
                assert actual == expected

    runtime_attrs_content = INannyClient.instance().last_runtime_attrs[nanny_service_id]['content']
    instance_spec = runtime_attrs_content['instance_spec']
    volumes = instance_spec['volume']
    containers = instance_spec['containers']
    assert len(volumes) == 0
    assert len(containers) == 0

    assert {u'task_type': u'YA_MAKE',
            u'task_id': u'123456789',
            u'resource_id': u'123456789',
            u'extract_path': '',
            u'is_dynamic': False,
            u'local_path': u'awacslet',
            u'resource_type': u'AWACSLET_BINARY'} not in runtime_attrs_content[u'resources'][u'sandbox_files']

    Api.create_published_component(model_pb2.ComponentMeta.SHAWSHANK_LAYER, '0.0.1')
    balancer_pb.spec.components.shawshank_layer.version = '0.0.1'
    balancer_pb.spec.components.shawshank_layer.state = balancer_pb.spec.components.shawshank_layer.SET
    balancer_pb = Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)
    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_1_ctl._process(ctx)
        for check in checker:
            with check:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected = L7State(
                    balancer=[t_t_f] * 9,
                    upstreams={
                        upstream_id: [t_t_f]
                    }
                )
                assert actual == expected

    runtime_attrs_content = INannyClient.instance().last_runtime_attrs[nanny_service_id]['content']
    layers = runtime_attrs_content['instance_spec']['layersConfig']['layer']
    assert len(layers) == 1
    assert layers[0] == {
        'fetchableMeta': {
            'type': 'SANDBOX_RESOURCE',
            'sandboxResource': {
                'taskId': u'123456789',
                'resourceType': u'SHAWSHANK_LAYER',
                'resourceId': u'123456789',
                'taskType': u'YA_PACKAGE',
            }
        },
        'url': ['rbtorrent:0000000000000000000000000000000000000000'],
    }

    balancer_pb.spec.components.shawshank_layer.version = ''
    balancer_pb.spec.components.shawshank_layer.state = balancer_pb.spec.components.shawshank_layer.REMOVED
    balancer_pb = Api.update_balancer(namespace_id, balancer_id, balancer_pb.meta.version, balancer_pb.spec)
    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_1_ctl._process(ctx)
        for check in checker:
            with check:
                actual = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected = L7State(
                    balancer=[t_t_f] * 10,
                    upstreams={
                        upstream_id: [t_t_f]
                    }
                )
                assert actual == expected

    runtime_attrs_content = INannyClient.instance().last_runtime_attrs[nanny_service_id]['content']
    layers = runtime_attrs_content['instance_spec']['layersConfig']['layer']
    assert len(layers) == 0


@pytest.mark.parametrize(u'ctl_version', (0, 4))
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
@mock_resolve_host({u'laas.yandex.ru': u'::1',
                    u'uaas.yandex.ru': u'::1',
                    u'uaas.search.yandex.net': u'::1'})
def test_uaas_laas(_1, ctx, ctlrunner, checker, ctl_version):
    L7_MACRO_BALANCER_UAAS_YML = """
    l7_macro:
      version: 0.2.1
      http: {}
      headers:
        - uaas:
            service_name: test1
        - laas: {}
    """
    L7_UPSTREAM_MACRO_UAAS_YML = """
    l7_upstream_macro:
      version: 0.1.0
      id: upstream-id
      matcher:
        any: true
      headers:
        - uaas:
            service_name: test2
      by_dc_scheme:
        dc_balancer:
          weights_section_id: 'hey2'
          method: BY_DC_WEIGHT
          attempt_all_dcs: true
        balancer:
          attempts: 2
          backend_timeout: 1s
          connect_timeout: 100ms
          do_not_retry_http_responses: true
          max_reattempts_share: 0.5
          max_pessimized_endpoints_share: 0.5
        dcs:
          - name: man
            backend_ids:
            - {backend_id}
        on_error:
          rst: true
    """
    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'
    upstream_id = 'upstream-id'
    backend_id = 'backend_1'
    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id=namespace_id, n=1)

    balancer_yml = L7_MACRO_BALANCER_UAAS_YML
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.create_balancer(namespace_id=namespace_id,
                        balancer_id=balancer_id,
                        spec_pb=balancer_spec_pb)

    Api.pause_balancer_transport(namespace_id, balancer_id)

    balancer_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._force_process(ctx)

    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(balancer=[p_f_f])
            assert actual_state == expected_state

    upstream_yml = L7_UPSTREAM_MACRO_UAAS_YML.format(backend_id=backend_id)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
    upstream_spec_pb.labels['order'] = '01'
    Api.create_upstream(namespace_id=namespace_id,
                        upstream_id=upstream_id,
                        spec_pb=upstream_spec_pb,
                        login='very-root-user')
    balancer_ctl._force_process(ctx)

    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[f_f_f],
                upstreams={
                    upstream_id: [p_f_f],
                },
                backends={
                    backend_id: [p_f_f],
                },
                endpoint_sets={
                    backend_id: [u_f_f],
                },
            )
            assert actual_state == expected_state

    create_empty_namespace('uaas.search.yandex.net')
    for location in ('man', 'sas', 'vla',):
        backend_spec_pb = model_pb2.BackendSpec()
        backend_spec_pb.selector.type = model_pb2.BackendSelector.MANUAL
        backend_spec_pb.is_global.value = True
        backend_pb = Api.create_backend(
            namespace_id='uaas.search.yandex.net',
            backend_id='usersplit_{}'.format(location),
            spec_pb=backend_spec_pb)
        es_spec_pb = model_pb2.EndpointSetSpec()
        es_spec_pb.is_global.value = True
        es_spec_pb.instances.add(host='{}-instance.yandex.ru'.format(location), port=80, weight=1, ipv6_addr='::1')
        Api.create_endpoint_set(
            namespace_id='uaas.search.yandex.net',
            endpoint_set_id='usersplit_{}'.format(location),
            spec_pb=es_spec_pb,
            backend_version=backend_pb.meta.version)

    balancer_ctl._force_process(ctx)

    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[t_f_f],
                upstreams={
                    upstream_id: [t_f_f],
                },
                backends={
                    backend_id: [t_f_f],
                    'uaas.search.yandex.net/usersplit_man': [t_f_f],
                    'uaas.search.yandex.net/usersplit_sas': [t_f_f],
                    'uaas.search.yandex.net/usersplit_vla': [t_f_f],
                },
                endpoint_sets={
                    backend_id: [t_f_f],
                    'uaas.search.yandex.net/usersplit_man': [t_f_f],
                    'uaas.search.yandex.net/usersplit_sas': [t_f_f],
                    'uaas.search.yandex.net/usersplit_vla': [t_f_f],
                },
            )
            assert actual_state == expected_state


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
def test_antirobot(_1, ctx, ctlrunner, checker, ctl_version):
    L7_MACRO_BALANCER_ANTIROBOT_YML = """
    l7_macro:
      version: 0.2.1
      http: {}
      antirobot: {}
      announce_check_reply:
        url_re: /ping
      health_check_reply: {}
    """
    L7_UPSTREAM_MACRO_ANTIROBOT_YML = """
    l7_upstream_macro:
      version: 0.1.0
      id: upstream-id
      matcher:
        any: true
      by_dc_scheme:
        dc_balancer:
          weights_section_id: 'hey2'
          method: BY_DC_WEIGHT
          attempt_all_dcs: true
        balancer:
          attempts: 2
          backend_timeout: 1s
          connect_timeout: 100ms
          do_not_retry_http_responses: true
          max_reattempts_share: 0.5
          max_pessimized_endpoints_share: 0.5
        dcs:
          - name: man
            backend_ids:
            - {backend_id}
        on_error:
          rst: true
    """
    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'
    upstream_id = 'upstream-id'
    backend_id = 'backend_1'

    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id=namespace_id, n=1)

    balancer_yml = L7_MACRO_BALANCER_ANTIROBOT_YML
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.create_balancer(namespace_id=namespace_id,
                        balancer_id=balancer_id,
                        spec_pb=balancer_spec_pb)

    Api.pause_balancer_transport(namespace_id, balancer_id)

    balancer_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._force_process(ctx)

    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(balancer=[p_f_f])
            assert actual_state == expected_state

    upstream_yml = L7_UPSTREAM_MACRO_ANTIROBOT_YML.format(backend_id=backend_id)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
    upstream_spec_pb.labels['order'] = '01'
    Api.create_upstream(namespace_id=namespace_id,
                        upstream_id=upstream_id,
                        spec_pb=upstream_spec_pb,
                        login='very-root-user')
    balancer_ctl._force_process(ctx)

    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[f_f_f],
                upstreams={
                    upstream_id: [p_f_f],
                },
                backends={
                    backend_id: [p_f_f],
                },
                endpoint_sets={
                    backend_id: [u_f_f],
                },
            )
            assert actual_state == expected_state

    create_empty_namespace('common-antirobot')
    for location in ('man', 'sas', 'vla',):
        backend_spec_pb = model_pb2.BackendSpec()
        backend_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
        backend_spec_pb.selector.yp_endpoint_sets.add(
            cluster=location,
            endpoint_set_id='antirobot_{}_yp'.format(location)
        )
        backend_spec_pb.is_global.value = True
        Api.create_backend(
            namespace_id='common-antirobot',
            backend_id='antirobot_{}_yp'.format(location),
            spec_pb=backend_spec_pb)

    balancer_ctl._force_process(ctx)

    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[t_f_f],
                upstreams={
                    upstream_id: [t_f_f],
                },
                backends={
                    backend_id: [t_f_f],
                    'common-antirobot/antirobot_man_yp': [t_f_f],
                    'common-antirobot/antirobot_sas_yp': [t_f_f],
                    'common-antirobot/antirobot_vla_yp': [t_f_f],
                },
                endpoint_sets={
                    backend_id: [t_f_f],
                },
            )
            assert actual_state == expected_state


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
def test_rps_limiter_external(_1, ctx, ctlrunner, checker, ctl_version):
    L7_MACRO_BALANCER_RPS_LIMITER_YML = """
    l7_macro:
      version: 0.2.1
      http: {}
      rps_limiter: {external: {record_name: test}}
      announce_check_reply:
        url_re: /ping
      health_check_reply: {}
    """
    L7_UPSTREAM_MACRO_RPS_LIMITER_YML = """
    l7_upstream_macro:
      version: 0.1.0
      id: upstream-id
      rps_limiter: {{external: {{record_name: test}}}}
      matcher:
        any: true
      by_dc_scheme:
        dc_balancer:
          weights_section_id: 'hey2'
          method: BY_DC_WEIGHT
          attempt_all_dcs: true
        balancer:
          attempts: 2
          backend_timeout: 1s
          connect_timeout: 100ms
          do_not_retry_http_responses: true
          max_reattempts_share: 0.5
          max_pessimized_endpoints_share: 0.5
        dcs:
          - name: man
            backend_ids:
            - {backend_id}
        on_error:
          rst: true
    """
    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'
    upstream_id = 'upstream-id'
    backend_id = 'backend_1'

    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id=namespace_id, n=1)

    balancer_yml = L7_MACRO_BALANCER_RPS_LIMITER_YML
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.create_balancer(namespace_id=namespace_id,
                        balancer_id=balancer_id,
                        spec_pb=balancer_spec_pb)

    Api.pause_balancer_transport(namespace_id, balancer_id)

    balancer_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._force_process(ctx)

    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(balancer=[p_f_f])
            assert actual_state == expected_state

    upstream_yml = L7_UPSTREAM_MACRO_RPS_LIMITER_YML.format(backend_id=backend_id)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
    upstream_spec_pb.labels['order'] = '01'
    Api.create_upstream(namespace_id=namespace_id,
                        upstream_id=upstream_id,
                        spec_pb=upstream_spec_pb,
                        login='very-root-user')
    balancer_ctl._force_process(ctx)

    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[f_f_f],
                upstreams={
                    upstream_id: [p_f_f],
                },
                backends={
                    backend_id: [p_f_f],
                },
                endpoint_sets={
                    backend_id: [u_f_f],
                },
            )
            assert actual_state == expected_state

    create_empty_namespace('common-rpslimiter')
    for location in ('man', 'sas', 'vla',):
        backend_spec_pb = model_pb2.BackendSpec()
        backend_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
        backend_spec_pb.selector.yp_endpoint_sets.add(
            cluster=location,
            endpoint_set_id='rpslimiter-serval-{}-sd'.format(location)
        )
        backend_spec_pb.is_global.value = True
        Api.create_backend(
            namespace_id='common-rpslimiter',
            backend_id='rpslimiter-serval-{}-sd'.format(location),
            spec_pb=backend_spec_pb)

    balancer_ctl._force_process(ctx)

    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[t_f_f],
                upstreams={
                    upstream_id: [t_f_f],
                },
                backends={
                    backend_id: [t_f_f],
                    'common-rpslimiter/rpslimiter-serval-man-sd': [t_f_f],
                    'common-rpslimiter/rpslimiter-serval-sas-sd': [t_f_f],
                    'common-rpslimiter/rpslimiter-serval-vla-sd': [t_f_f],
                },
                endpoint_sets={
                    backend_id: [t_f_f],
                },
            )
            assert actual_state == expected_state


def test_awacslet_get_filler():
    fillers = {
        u'0.0.1': u'0.0.1',
        u'0.1.0': u'0.1.0',
        u'1.10.0': u'1.10.0',
    }

    awacslet_config = components.get_component_config(model_pb2.ComponentMeta.AWACSLET)
    ver = awacslet_config.parse_version
    fillers_by_sorted_version = OrderedDict(sorted((ver(v), f) for v, f in six.iteritems(fillers)))

    assert awacslet.get_filler(ver('0.0.0'), fillers_by_sorted_version=fillers_by_sorted_version) == '0.0.1'
    assert awacslet.get_filler(ver('0.0.1'), fillers_by_sorted_version=fillers_by_sorted_version) == '0.0.1'
    assert awacslet.get_filler(ver('0.0.9'), fillers_by_sorted_version=fillers_by_sorted_version) == '0.0.1'
    assert awacslet.get_filler(ver('0.1.1'), fillers_by_sorted_version=fillers_by_sorted_version) == '0.1.0'
    assert awacslet.get_filler(ver('1.9.0'), fillers_by_sorted_version=fillers_by_sorted_version) == '0.1.0'
    assert awacslet.get_filler(ver('1.10.0-pushclient'),
                               fillers_by_sorted_version=fillers_by_sorted_version) == '1.10.0'
    assert awacslet.get_filler(ver('1.10.1'), fillers_by_sorted_version=fillers_by_sorted_version) == '1.10.0'
    assert awacslet.get_filler(ver('1.11.0'), fillers_by_sorted_version=fillers_by_sorted_version) == '1.10.0'
    with pytest.raises(AssertionError):
        awacslet.get_filler(ver('1.11.0'), fillers_by_sorted_version={})


@mock.patch.object(component_service, 'check_and_complete_sandbox_resource')
@pytest.mark.parametrize('supervisor_component_type', [model_pb2.ComponentMeta.INSTANCECTL_CONF,
                                                       model_pb2.ComponentMeta.AWACSLET])
def test_find_pushclient_component_version(_1, supervisor_component_type, cache):
    Api.create_published_component(supervisor_component_type, '0.0.1')
    Api.create_published_component(supervisor_component_type, '0.0.1-pushclient')
    Api.create_published_component(supervisor_component_type, '0.0.2')
    Api.create_published_component(supervisor_component_type, '0.0.2-pushclient')
    Api.create_published_component(supervisor_component_type, '0.0.3')
    Api.create_published_component(supervisor_component_type, '0.1.0')
    Api.create_published_component(supervisor_component_type, '0.1.2-pushclient')
    Api.create_published_component(supervisor_component_type, '0.1.5')
    Api.create_published_component(supervisor_component_type, '0.1.5-pushclient')

    for old_version, expected_version in (
        ('0.0.1', '0.0.2-pushclient'),
        ('0.0.2', '0.0.2-pushclient'),
        ('0.0.3', '0.0.2-pushclient'),
        ('0.1.0', '0.1.5-pushclient'),
        ('0.1.5', '0.1.5-pushclient'),
    ):
        assert components.find_pushclient_supervisor_component_version(cache, supervisor_component_type, old_version,
                                                                       with_pushclient=True) == expected_version

    for old_version, expected_version in (
        ('0.0.1-pushclient', '0.0.3'),
        ('0.0.2-pushclient', '0.0.3'),
        ('0.1.2-pushclient', '0.1.5'),
        ('0.1.5-pushclient', '0.1.5'),
    ):
        assert components.find_pushclient_supervisor_component_version(cache, supervisor_component_type, old_version,
                                                                       with_pushclient=False) == expected_version

    assert components.find_pushclient_supervisor_component_version(cache, supervisor_component_type, '0.2.0',
                                                                   with_pushclient=True) is None
    assert components.find_pushclient_supervisor_component_version(cache, supervisor_component_type, '0.2.0-pushclient',
                                                                   with_pushclient=False) is None


@mock.patch.object(component_service, 'check_and_complete_sandbox_resource')
def test_enable_disable_pushclient(_1, cache):
    supervisor_component_type = model_pb2.ComponentMeta.INSTANCECTL_CONF
    Api.create_published_component(supervisor_component_type, '0.0.1')
    Api.create_published_component(supervisor_component_type, '0.0.1-pushclient')
    Api.create_published_component(supervisor_component_type, '0.0.2')
    Api.create_published_component(supervisor_component_type, '0.0.2-pushclient')
    Api.create_published_component(model_pb2.ComponentMeta.PUSHCLIENT, '6.74.0')
    Api.create_published_component(model_pb2.ComponentMeta.PUSHCLIENT, '6.14.4')

    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'

    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id=namespace_id, n=1)

    balancer_yml = L7_MACRO_BALANCER_YML
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    balancer_pb = Api.create_balancer(namespace_id=namespace_id,
                                      balancer_id=balancer_id,
                                      spec_pb=balancer_spec_pb)

    pb_field_name = components.get_component_config(supervisor_component_type).pb_field_name
    cmp_pb = getattr(balancer_pb.spec.components, pb_field_name)
    cmp_pb.state = cmp_pb.SET
    cmp_pb.version = '0.0.1'
    balancer_pb = Api.update_balancer(
        namespace_id=namespace_id, balancer_id=balancer_id, version=balancer_pb.meta.version, spec_pb=balancer_pb.spec
    )

    Api.enable_pushclient(namespace_id, balancer_id, balancer_pb.meta.version)
    balancer_pb = cache.get_balancer(namespace_id, balancer_id)
    assert getattr(balancer_pb.spec.components, pb_field_name).version == '0.0.2-pushclient'
    assert balancer_pb.spec.components.pushclient.version == '6.74.0'

    with pytest.raises(BadRequestError, match='Pushclient is already enabled'):
        Api.enable_pushclient(namespace_id, balancer_id, balancer_pb.meta.version)

    Api.disable_pushclient(namespace_id, balancer_id, balancer_pb.meta.version)
    balancer_pb = cache.get_balancer(namespace_id, balancer_id)
    assert getattr(balancer_pb.spec.components, pb_field_name).version == '0.0.2'
    assert balancer_pb.spec.components.pushclient.state == model_pb2.BalancerSpec.ComponentsSpec.Component.REMOVED
    assert balancer_pb.spec.components.pushclient.version == ''

    with pytest.raises(BadRequestError, match='Pushclient is already disabled'):
        Api.disable_pushclient(namespace_id, balancer_id, balancer_pb.meta.version)


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
def test_move_fqdn_between_domains(_1, ctx, ctlrunner, checker, ctl_version):
    balancer_yml = """
    l7_macro:
      version: 0.2.2
      http: {}
      https: {}
      include_domains: {}
      announce_check_reply:
        url_re: /ping
      health_check_reply: {}
    """
    upstream_yml = """
    l7_upstream_macro:
      version: 0.1.1
      id: upstream-id
      matcher:
        any: true
      flat_scheme:
        balancer:
          attempts: 2
          backend_timeout: 1s
          connect_timeout: 100ms
          do_not_retry_http_responses: true
          max_reattempts_share: 0.5
          max_pessimized_endpoints_share: 0.5
        backend_ids:
          - {backend_id}
        on_error:
          rst: true
    """
    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'
    upstream_id = 'upstream-id'
    backend_id = 'backend_1'

    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id=namespace_id, n=1)

    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.create_balancer(namespace_id=namespace_id,
                        balancer_id=balancer_id,
                        spec_pb=balancer_spec_pb)
    Api.pause_balancer_transport(namespace_id, balancer_id)

    upstream_yml = upstream_yml.format(backend_id=backend_id)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
    upstream_spec_pb.labels['order'] = '01'
    Api.create_upstream(namespace_id=namespace_id,
                        upstream_id=upstream_id,
                        spec_pb=upstream_spec_pb,
                        login='very-root-user')

    create_cert(namespace_id, 'xxx', domains=['a.ya.ru', 'b.ya.ru', 'c.ya.ru'],
                storage_type='yav', incomplete=False)
    create_cert(namespace_id, 'yyy', domains=['a.ya.ru', 'b.ya.ru', 'c.ya.ru'],
                storage_type='yav', incomplete=False)
    create_domain(namespace_id, 'd1',
                  fqdns=['a.ya.ru', 'b.ya.ru'],
                  upstream_id=upstream_id,
                  cert_id='xxx',
                  incomplete=False)
    create_domain(namespace_id, 'd2',
                  fqdns=['c.ya.ru'],
                  upstream_id=upstream_id,
                  cert_id='xxx',
                  incomplete=False)

    balancer_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_ctl)

    ctx.log.info('Starting state: domains with non-intersecting fqdns are valid')
    nanny_stub.set_current_snapshot(nanny_service_id, 'a', 1)
    nanny_stub.mark_active(nanny_service_id, 'a')
    with unpaused_balancer_transport(namespace_id, balancer_id):
        balancer_ctl._force_process(ctx)
        for a in checker:
            with a:
                actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
                expected_state = L7State(
                    balancer=[t_f_t],
                    domains={
                        'd1': [t_f_t],
                        'd2': [t_f_t],
                    },
                    certs={
                        'xxx': [t_f_t],
                    },
                    upstreams={
                        upstream_id: [t_f_t],
                    },
                    backends={
                        backend_id: [t_f_t],
                    },
                    endpoint_sets={
                        backend_id: [t_f_t],
                    },
                )
                assert actual_state == expected_state

    ctx.log.info('Step 1: add fqdn from d1 to shadow_fqdns of d2')
    d2_pb = Api.get_domain(namespace_id, 'd2')
    d2_pb.spec.yandex_balancer.config.shadow_fqdns.append('a.ya.ru')
    d2_pb.spec.yandex_balancer.config.cert.id = 'yyy'
    update_domain(namespace_id, 'd2', d2_pb.spec)
    balancer_ctl._process(ctx)
    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[t_f_t],
                domains={
                    'd1': [t_f_t],
                    'd2': [t_f_t, f_f_f],
                },
                certs={
                    'xxx': [t_f_t],
                    'yyy': [u_f_f],
                },
                upstreams={
                    upstream_id: [t_f_t],
                },
                backends={
                    backend_id: [t_f_t],
                },
                endpoint_sets={
                    backend_id: [t_f_t],
                },
            )
            assert actual_state == expected_state

    ctx.log.info('Step 2: delete d1, expect both domains to validate')
    d1_pb = Api.get_domain(namespace_id, 'd1')
    d1_pb.spec.deleted = True
    update_domain(namespace_id, 'd1', d1_pb.spec)
    balancer_ctl._process(ctx)
    for a in checker:
        with a:
            actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
            expected_state = L7State(
                balancer=[t_f_t],
                domains={
                    'd1': [t_f_t, t_f_f],
                    'd2': [t_f_t, t_f_f],
                },
                upstreams={
                    upstream_id: [t_f_t],
                },
                certs={
                    'xxx': [t_f_t],
                    'yyy': [t_f_f],
                },
                backends={
                    backend_id: [t_f_t],
                },
                endpoint_sets={
                    backend_id: [t_f_t],
                },
            )
            assert actual_state == expected_state


@pytest.mark.parametrize('ctl_version', (0, 4))
def test_update_l7_macro_to_0_3_0_with_yandex_tld(cache, ctx, ctlrunner, checker, ctl_version):
    balancer_yml = """
    l7_macro:
      version: 0.2.2
      http: {}
      include_domains: {}
      announce_check_reply:
        url_re: /ping
      health_check_reply: {}
    """
    upstream_yml = """
    l7_upstream_macro:
      version: 0.1.1
      id: upstream-id
      matcher:
        any: true
      flat_scheme:
        balancer:
          attempts: 2
          backend_timeout: 1s
          connect_timeout: 100ms
          do_not_retry_http_responses: true
          max_reattempts_share: 0.5
          max_pessimized_endpoints_share: 0.5
        backend_ids:
          - {backend_id}
        on_error:
          rst: true
    """
    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'
    upstream_id = 'upstream-id'
    backend_id = 'backend_1'

    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id=namespace_id, n=1)

    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.create_balancer(
        namespace_id=namespace_id, balancer_id=balancer_id, spec_pb=balancer_spec_pb)
    Api.pause_balancer_transport(namespace_id, balancer_id)

    upstream_yml = upstream_yml.format(backend_id=backend_id)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
    upstream_spec_pb.labels['order'] = '01'
    Api.create_upstream(
        namespace_id=namespace_id, upstream_id=upstream_id,
        spec_pb=upstream_spec_pb, login='very-root-user')

    create_domain(
        namespace_id, 'd1',
        fqdns=['a.ya.ru', 'b.ya.ru'], upstream_id=upstream_id, incomplete=False)
    create_domain(
        namespace_id, 'd2',
        fqdns=['yandex.tld'], upstream_id=upstream_id, incomplete=False,
        yandex_tld=True)

    balancer_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._process(ctx)

    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_f_f],
            domains={
                'd1': [t_f_f],
                'd2': [t_f_f],
            },
            upstreams={
                upstream_id: [t_f_f],
            },
            backends={
                backend_id: [t_f_f],
            },
            endpoint_sets={
                backend_id: [t_f_f],
            },
        ))

    # try upgrading to 0.3.0
    balancer_yml_030 = """
    l7_macro:
      version: 0.3.0
      http: {}
      include_domains: {}
      announce_check_reply:
        url_re: /ping
      health_check_reply: {}
    """

    balancer_pb = Api.get_balancer(namespace_id=namespace_id, balancer_id=balancer_id)
    balancer_pb.spec.yandex_balancer.yaml = balancer_yml_030
    Api.update_balancer(
        namespace_id=namespace_id, balancer_id=balancer_id,
        version=balancer_pb.meta.version, spec_pb=balancer_pb.spec)
    balancer_ctl._process(ctx)

    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_f_f, f_f_f],
            domains={
                'd1': [t_f_f],
                'd2': [t_f_f],
            },
            upstreams={
                upstream_id: [t_f_f],
            },
            backends={
                backend_id: [t_f_f],
            },
            endpoint_sets={
                backend_id: [t_f_f],
            },
        ),
        message_key=lambda actual_state: actual_state.balancer.last_rev,
        message='You must set the `l7_macro.core.trust_x_forwarded_for_y: true` '
                'because your service is behind yandex.tld (namespace has a YANDEX_TLD '
                'domain configured)'
    )

    # try upgrading to 0.3.0 with core.trust_x_forwarded_for_y
    balancer_yml_030_with_trust_xffy = """
    l7_macro:
      version: 0.3.0
      core:
        trust_x_forwarded_for_y: true
      http: {}
      include_domains: {}
      announce_check_reply:
        url_re: /ping
      health_check_reply: {}
    """
    balancer_pb = Api.get_balancer(namespace_id=namespace_id, balancer_id=balancer_id)
    balancer_pb.spec.yandex_balancer.yaml = balancer_yml_030_with_trust_xffy
    Api.update_balancer(
        namespace_id=namespace_id, balancer_id=balancer_id,
        version=balancer_pb.meta.version, spec_pb=balancer_pb.spec)
    balancer_ctl._process(ctx)

    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_f_f, f_f_f, t_f_f],
            domains={
                'd1': [t_f_f],
                'd2': [t_f_f],
            },
            upstreams={
                upstream_id: [t_f_f],
            },
            backends={
                backend_id: [t_f_f],
            },
            endpoint_sets={
                backend_id: [t_f_f],
            },
        ))


@pytest.mark.parametrize('ctl_version', (0, 4))
def test_update_l7_macro_to_0_3_0_add_yandex_tld(cache, ctx, ctlrunner, checker, ctl_version):
    balancer_yml = """
    l7_macro:
      version: 0.3.0
      http: {}
      include_domains: {}
      announce_check_reply:
        url_re: /ping
      health_check_reply: {}
    """
    upstream_yml = """
    l7_upstream_macro:
      version: 0.1.1
      id: upstream-id
      matcher:
        any: true
      flat_scheme:
        balancer:
          attempts: 2
          backend_timeout: 1s
          connect_timeout: 100ms
          do_not_retry_http_responses: true
          max_reattempts_share: 0.5
          max_pessimized_endpoints_share: 0.5
        backend_ids:
          - {backend_id}
        on_error:
          rst: true
    """
    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'
    upstream_id = 'upstream-id'
    backend_id = 'backend_1'

    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id=namespace_id, n=1)

    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.create_balancer(
        namespace_id=namespace_id, balancer_id=balancer_id, spec_pb=balancer_spec_pb)
    Api.pause_balancer_transport(namespace_id, balancer_id)

    upstream_yml = upstream_yml.format(backend_id=backend_id)
    upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
    upstream_spec_pb.labels['order'] = '01'
    Api.create_upstream(
        namespace_id=namespace_id, upstream_id=upstream_id,
        spec_pb=upstream_spec_pb, login='very-root-user')

    create_domain(
        namespace_id, 'd1',
        fqdns=['a.ya.ru', 'b.ya.ru'], upstream_id=upstream_id, incomplete=False)

    balancer_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._process(ctx)

    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_f_f],
            domains={
                'd1': [t_f_f],
                # 'd2': [t_f_f],
            },
            upstreams={
                upstream_id: [t_f_f],
            },
            backends={
                backend_id: [t_f_f],
            },
            endpoint_sets={
                backend_id: [t_f_f],
            },
        ))

    create_domain(
        namespace_id, 'd2',
        fqdns=['yandex.tld'], upstream_id=upstream_id, incomplete=False,
        yandex_tld=True)

    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._process(ctx)

    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_f_f],
            domains={
                'd1': [t_f_f],
                'd2': [f_f_f],
            },
            upstreams={
                upstream_id: [t_f_f],
            },
            backends={
                backend_id: [t_f_f],
            },
            endpoint_sets={
                backend_id: [t_f_f],
            },
        ),
        message_key=lambda actual_state: actual_state.domains['d2'].last_rev,
        message='You must set the `l7_macro.core.trust_x_forwarded_for_y: true` '
                'because your service is behind yandex.tld (namespace has a YANDEX_TLD '
                'domain configured)'
    )


def test_internal_upstreams(cache, ctx, ctlrunner, checker):
    balancer_yml = """
    l7_macro:
      version: 0.3.0
      http: {}
      announce_check_reply:
        url_re: /ping
      health_check_reply: {}
    """

    internal_upstream1_yml = """
    l7_upstream_macro:
      version: 0.0.2
      id: _i1
      by_dc_scheme:
        dc_balancer:
          weights_section_id: bygeo
          attempts: 1
          method: BY_DC_WEIGHT
        balancer:
          max_pessimized_endpoints_share: 0.5
          attempts: 1
          do_not_limit_reattempts: true
          do_not_retry_http_responses: true
          backend_timeout: 10s
          connect_timeout: 70ms
        dcs:
          - name: man
            backend_ids:
            - backend_1
        on_error:
          rst: true
    """
    internal_upstream2_yml = """
    l7_upstream_macro:
      version: 0.0.2
      id: _i2
      monitoring:
        uuid: payments_requests_to_qloud
      flat_scheme:
        balancer:
          max_pessimized_endpoints_share: 0.5
          attempts: 1
          do_not_limit_reattempts: true
          do_not_retry_http_responses: true
          backend_timeout: 15s
          connect_timeout: 70ms
        backend_ids:
        - backend_2
        on_error:
          rst: true
        """
    internal_upstream3_yml = """
    l7_upstream_macro:
      version: 0.0.1
      id: _i3
      flat_scheme:
        balancer:
          backend_timeout: 20s
          connect_timeout: 100ms
          attempts: 3
          fast_attempts: 2
          do_not_retry_http_responses: true
          max_reattempts_share: 0.2
          max_pessimized_endpoints_share: 0.2
        backend_ids:
        - backend_3
        on_error:
          static:
            status: 504
            content: 'Service unavailable'
        """
    traffic_split_upstream_yml = """
    l7_upstream_macro:
      version: 0.0.2
      id: tf
      headers:
        - create: {target: Host, value: payments-test.mail.yandex.net}
      matcher:
        path_re: '(/.*)?'
      traffic_split:
        weights_section_id: byenvpayments
        attempts: 2
        routes:
          - name: qloud
            upstream_id: _i1
          - name: deploy
            upstream_id: _i2
    """

    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'

    upstreams = {
        '_i1': internal_upstream1_yml,
        '_i2': internal_upstream2_yml,
        '_i3': internal_upstream3_yml,
        'tf': traffic_split_upstream_yml,
    }

    backend_ids = ['backend_1', 'backend_2', 'backend_3']
    create_empty_namespace(namespace_id)
    create_resolved_manual_backends(namespace_id=namespace_id, n=3)

    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.create_balancer(
        namespace_id=namespace_id, balancer_id=balancer_id, spec_pb=balancer_spec_pb)
    Api.pause_balancer_transport(namespace_id, balancer_id)

    for upstream_id, upstream_yml in six.iteritems(upstreams):
        upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
        upstream_spec_pb.labels['order'] = '01'
        upstream_spec_pb.yandex_balancer.type = (model_pb2.YandexBalancerUpstreamSpec.INTERNAL if upstream_id.startswith('_')
                                                 else model_pb2.YandexBalancerUpstreamSpec.COMMON)
        Api.create_upstream(
            namespace_id=namespace_id, upstream_id=upstream_id,
            spec_pb=upstream_spec_pb, login='very-root-user')

    balancer_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._process(ctx)

    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_f_f],
            upstreams={
                'tf': [t_f_f],
                '_i1': [t_f_f],
                '_i2': [t_f_f],
            },
            backends={
                backend_ids[0]: [t_f_f],
                backend_ids[1]: [t_f_f],
            },
            endpoint_sets={
                backend_ids[0]: [t_f_f],
                backend_ids[1]: [t_f_f],
            },
        ))


@pytest.mark.parametrize('ctl_version', (0, 4))
@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
@mock.patch.object(transport.BalancerTransport, '_save_config_to_snapshot',
                   side_effect=nanny_stub.save_config_to_snapshot)
@mock.patch.object(NannyRpcMockClient, 'has_snapshot_been_active', side_effect=nanny_stub.has_snapshot_been_active)
def test_removing_upstreams_used_in_balancer_states(_1, ctx, ctlrunner, ctl_version):
    nanny_stub.__init__()

    namespace_id = 'test-namespace'
    balancer_id = 'balancer-id_sas'
    nanny_service_id = 'test-service-id'
    upstream_1_id = 'upstream-1-id'
    upstream_2_id = 'upstream-2-id'

    create_empty_namespace(namespace_id)
    backend_ids = create_resolved_manual_backends(namespace_id=namespace_id, n=1)

    balancer_yml = L7_MACRO_BALANCER_YML
    balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, ctl_version=ctl_version)
    balancer_spec_pb.yandex_balancer.mode = balancer_spec_pb.yandex_balancer.EASY_MODE
    Api.create_balancer(namespace_id=namespace_id,
                        balancer_id=balancer_id,
                        spec_pb=balancer_spec_pb)
    nanny_stub.set_current_snapshot(balancer_spec_pb.config_transport.nanny_static_file.service_id, 'b', 1)

    for upstream_id in (upstream_1_id, upstream_2_id):
        upstream_yml = L7_UPSTREAM_MACRO_BALANCER_YML.format(id=upstream_id, backend_id=backend_ids[0],
                                                             matcher='url_re: "/ok"')
        upstream_spec_pb = make_upstream_spec_pb(upstream_yml, easy_mode=True)
        upstream_spec_pb.labels['order'] = '01'
        Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_id,
                            spec_pb=upstream_spec_pb,
                            login='very-root-user')

    balancer_1_ctl = BalancerCtl(namespace_id, balancer_id)
    ctlrunner.run_ctl(balancer_1_ctl)
    balancer_1_ctl._force_process(ctx)

    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_f_f],
            upstreams={
                upstream_1_id: [t_f_f],
                upstream_2_id: [t_f_f],
            },
            backends={
                backend_ids[0]: [t_f_f],
            },
            endpoint_sets={
                backend_ids[0]: [t_f_f],
            },
        )
    )

    nanny_stub.mark_active(balancer_spec_pb.config_transport.nanny_static_file.service_id, 'b')
    balancer_1_ctl._force_process(ctx)

    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_f_t],
            upstreams={
                upstream_1_id: [t_f_t],
                upstream_2_id: [t_f_t],
            },
            backends={
                backend_ids[0]: [t_f_t],
            },
            endpoint_sets={
                backend_ids[0]: [t_f_t],
            },
        )
    )

    nanny_stub.set_current_snapshot(balancer_spec_pb.config_transport.nanny_static_file.service_id, 'c', 1)
    upstream_1_pb = Api.get_upstream(namespace_id, upstream_1_id)
    Api.remove_upstream(namespace_id, upstream_1_id, upstream_1_pb.meta.version)

    def assert_upstream_is_deleted():
        assert Api.get_upstream(namespace_id, upstream_1_id).spec.deleted

    wait_until_passes(assert_upstream_is_deleted)

    balancer_1_ctl._force_process(ctx)
    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_t_t],
            upstreams={
                upstream_1_id: [t_f_t, t_t_f],
                upstream_2_id: [t_t_t],
            },
            backends={
                backend_ids[0]: [t_t_t],
            },
            endpoint_sets={
                backend_ids[0]: [t_t_t],
            },
        )
    )

    namespace_ctl = NamespaceCtl(namespace_id, {'name_prefix': ''})
    namespace_ctl._process(ctx, events.BalancerStateUpdate(None, None))
    Api.get_upstream(namespace_id, upstream_1_id, consistency=api_pb2.STRONG)

    nanny_stub.mark_active(balancer_spec_pb.config_transport.nanny_static_file.service_id, 'c')
    balancer_1_ctl._force_process(ctx)
    _check_l7_state(
        namespace_id, balancer_id,
        L7State(
            balancer=[t_f_t],
            upstreams={
                upstream_2_id: [t_f_t],
            },
            backends={
                backend_ids[0]: [t_f_t],
            },
            endpoint_sets={
                backend_ids[0]: [t_f_t],
            },
        )
    )

    namespace_ctl._process(ctx, events.BalancerStateUpdate(None, None))

    def check_upstream_removed():
        with pytest.raises(NotFoundError):
            Api.get_upstream(namespace_id, upstream_1_id)

    wait_until_passes(check_upstream_removed)
    Api.get_upstream(namespace_id, upstream_2_id, consistency=api_pb2.STRONG)


def _check_l7_state(namespace_id, balancer_id, expected_state,
                    message_key=None, message=None):
    """
    Raise AssertionError if actual state doesn't match the expected state.
    `message_key` should be  a function that accepts `actual_state` and returns
    the argument for the :func:`m` function.

    :type namespace_id: str
    :type balancer_id: str
    :type expected_state: L7State
    :type message_key: Callable
    :type message: str
    """

    def check():
        actual_state = L7State.from_api(namespace_id=namespace_id, balancer_id=balancer_id)
        assert actual_state == expected_state

        if not all([message, message_key]) and any([message, message_key]):
            raise RuntimeError('If you pass message_key or message, you must pass both of them')

        if message:
            assert m(message_key(actual_state)) == message

    wait_until_passes(check)

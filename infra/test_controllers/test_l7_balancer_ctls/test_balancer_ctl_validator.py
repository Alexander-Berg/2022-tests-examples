import copy

import inject
import mock
import pytest
import six
import ujson
import yaml
from flaky import flaky
from sepelib.core import config

from awacs.lib import nannyrpcclient
from awacs.model import balancer
from awacs.model.balancer import transport, transport_config_bundle
from awacs.model.balancer.ctl import BalancerCtl as BaseBalancerCtl
from awacs.model.balancer.generator import ValidationResult
from awacs.model.balancer.validator import BalancerValidator
from awacs.model.balancer.vector import UpstreamVersion, Vector, BalancerVersion, VersionName
from awtest.mocks.nanny_rpc_client import NannyRpcMockClient
from infra.awacs.proto import model_pb2
from awtest import wait_until
from awtest.api import Api


VALID_BALANCER_CONFIG = {
    'main': {
        'addrs': [
            {'ip': '127.0.0.1', 'port': 80},
            {'ip': '127.0.0.2', 'port': 80},
        ],
        'maxconn': 4000,
        'workers': 1,
        'buffer': 65536,
        'log': '/usr/local/www/logs/current-childs_log-balancer-16020',
        'events': {'stats': 'report'},
        'ipdispatch': {
            'include_upstreams': {
                'type': 'ALL',
            },
        },
    }
}

VALID_BALANCER_CONFIG_2 = copy.deepcopy(VALID_BALANCER_CONFIG)
VALID_BALANCER_CONFIG_2['main']['log'] += '_test'

INVALID_UPSTREAM_1_CONFIG = {
    # is not an ipdispatch section
    'errorlog': {
        'log': 'xxx.txt'
    }
}

VALID_UPSTREAM_1_CONFIG = {
    'ipdispatch_section': {
        'ips': ['127.0.0.1'],
        'ports': [80],
        'errorlog': {
            'log': 'xxx.txt',
            'errordocument': {'status': 200}
        }
    }
}

VALID_UPSTREAM_2_CONFIG_1 = {
    'ipdispatch_section': {
        'ips': ['127.0.0.2'],
        'ports': [80],
        'errorlog': {
            'log': 'yyy.txt',
            'errordocument': {'status': 200}
        }
    }
}

VALID_UPSTREAM_2_CONFIG_2 = {
    'ipdispatch_section': {
        'ips': ['127.0.0.2'],
        'ports': [80],
        'errorlog': {
            'log': 'zzz.txt',
            'errordocument': {'status': 200}
        }
    }
}

NAMESPACE_ID = 'test_balancer'
BALANCER_ID = 'test_balancer_service_sas'
UPSTREAM_1_ID = 'u_1'
UPSTREAM_2_ID = 'u_2'


class BalancerCtl(BaseBalancerCtl):
    TRANSPORT_MAIN_LOOP_FREQ = 0.01
    PROCESS_INTERVAL = 0.01
    FORCE_PROCESS_INTERVAL = 0.01
    EVENTS_QUEUE_GET_TIMEOUT = 0.01

    _get_sleep_after_exception_timeout = lambda *args, **kwargs: 0


@pytest.fixture
def enable_large_balancers():
    from awacs.model import util
    util.LARGE_NAMESPACE_IDS, util.LARGE_BALANCER_IDS = None, None
    config.set_value('run.large_balancer_ids', [['ns', 'large_id'], ])
    config.set_value('run.large_namespace_ids', ['large_ns'])
    yield
    config.set_value('run.large_balancer_ids', [])
    config.set_value('run.large_namespace_ids', [])


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client):
    def configure(b):
        b.bind(nannyrpcclient.INannyRpcClient, NannyRpcMockClient('https://api/repo/'))
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def vector_to_versions(vector):
    """
    :type vector: Vector
    :rtype: (str, dict[(str, str), str], dict[(str, str), str], dict[(str, str), str])
    """
    rv = (
        vector.balancer_version.version,
        {full_upstream_id: upstream_version.version for full_upstream_id, upstream_version in
         six.iteritems(vector.upstream_versions)},
        {full_backend_id: backend_version.version
         for full_backend_id, backend_version in six.iteritems(vector.backend_versions)},
        {full_endpoint_set_id: endpoint_set_version.version
         for full_endpoint_set_id, endpoint_set_version in six.iteritems(vector.endpoint_set_versions)}
    )
    for d in (rv[1], rv[2], rv[3]):
        for k, v in six.iteritems(d):
            assert isinstance(k, tuple) and len(k) == 2
    return rv


def get_revision_status(pb, version):
    for rev_status in pb.status.revisions:
        if rev_status.id == version:
            return rev_status


def set_upstream_validation_status(zk_storage, cache, upstream_id, version, status, message='OK'):
    def update():
        for balancer_state_pb in zk_storage.update_balancer_state(NAMESPACE_ID, BALANCER_ID):
            for rev_pb in balancer_state_pb.upstreams[upstream_id].statuses:
                if rev_pb.revision_id == version:
                    if rev_pb.validated.status != status or rev_pb.validated.message != message:
                        rev_pb.validated.status = status
                        rev_pb.validated.message = message
        for rev_pb in cache.must_get_balancer_state(NAMESPACE_ID, BALANCER_ID).upstreams[upstream_id].statuses:
            if rev_pb.revision_id == version:
                assert rev_pb.validated.status == status
                assert rev_pb.validated.message == message
                return UpstreamVersion.from_rev_status_pb((NAMESPACE_ID, upstream_id), rev_pb)

    return wait_until(update, must_get_value=True)


def set_balancer_validation_status(zk_storage, cache, version, status, message='OK'):
    def update():
        for balancer_state_pb in zk_storage.update_balancer_state(NAMESPACE_ID, BALANCER_ID):
            for rev_pb in balancer_state_pb.balancer.statuses:
                if rev_pb.revision_id == version:
                    if rev_pb.validated.status != status or rev_pb.validated.message != message:
                        rev_pb.validated.status = status
                        rev_pb.validated.message = message
        for rev_pb in cache.must_get_balancer_state(NAMESPACE_ID, BALANCER_ID).balancer.statuses:
            if rev_pb.revision_id == version:
                assert rev_pb.validated.status == status
                assert rev_pb.validated.message == message
                return BalancerVersion.from_rev_status_pb((NAMESPACE_ID, BALANCER_ID), rev_pb)

    return wait_until(update, must_get_value=True)


def test_balancer_ctl_is_large(enable_large_balancers):
    assert BalancerCtl('ns', 'large_id')._is_large
    assert not BalancerCtl('ns', 'another_id')._is_large
    assert BalancerCtl('large_ns', 'any_id')._is_large


def test_transport_v2_is_large(enable_large_balancers):
    assert transport.BalancerTransport('ns', 'large_id', None)._is_large
    assert not transport.BalancerTransport('ns', 'another_id', None)._is_large
    assert transport.BalancerTransport('large_ns', 'any_id', None)._is_large


def test_validator_is_large(enable_large_balancers):
    assert BalancerValidator('ns', 'large_id')._is_large
    assert not BalancerValidator('ns', 'another_id')._is_large
    assert BalancerValidator('large_ns', 'any_id')._is_large


@flaky(max_runs=3, min_passes=1)
def test_validator(ctx, cache, checker, ctlrunner):
    valid_versions = []

    def _validate_vector_stub(ctx, vector, valid_vector):
        if not vector.upstream_versions and not vector.backend_versions:
            return ValidationResult(balancer=None,
                                    included_full_upstream_ids=set(vector.upstream_versions),
                                    included_full_backend_ids=set(vector.backend_versions),
                                    included_full_endpoint_set_ids=set(vector.backend_versions),
                                    included_full_cert_ids=set(),
                                    included_full_knob_ids=set(),
                                    included_full_domain_ids=set(vector.domain_versions),
                                    included_full_weight_section_ids=set())
        versions = vector_to_versions(vector)
        if versions not in valid_versions:
            raise balancer.errors.ConfigValidationError('_validate_vector_stub: vector is not valid')
        return ValidationResult(balancer=None,
                                included_full_upstream_ids=set(vector.upstream_versions),
                                included_full_backend_ids=set(vector.backend_versions),
                                included_full_endpoint_set_ids=set(vector.backend_versions),
                                included_full_cert_ids=set(),
                                included_full_knob_ids=set(),
                                included_full_domain_ids=set(vector.domain_versions),
                                included_full_weight_section_ids=set())

    Api.create_namespace(NAMESPACE_ID)
    b_spec_pb = model_pb2.BalancerSpec()
    b_spec_pb.config_transport.type = model_pb2.NANNY_STATIC_FILE
    b_spec_pb.config_transport.nanny_static_file.service_id = 'service_id'
    b_spec_pb.type = model_pb2.YANDEX_BALANCER
    b_spec_pb.yandex_balancer.yaml = yaml.dump(VALID_BALANCER_CONFIG)
    Api.create_balancer(NAMESPACE_ID, BALANCER_ID, b_spec_pb)

    with mock.patch.object(balancer.validator.BalancerValidator, '_validate_vector', side_effect=_validate_vector_stub):
        ctl = BalancerCtl(NAMESPACE_ID, BALANCER_ID)
        ctlrunner.run_ctl(ctl)

        for a in checker:
            with a:
                balancer_pb_1 = Api.get_balancer(NAMESPACE_ID, BALANCER_ID)
                assert balancer_pb_1.status.validated.status == 'Pending'
                assert (balancer_pb_1.status.validated.message ==
                        u'{"message":"Can not validate balancer spec due to absence of valid upstreams and backends"}')
                assert not ctl._validator._state_proxy.valid_vector.balancer_version
                assert not ctl._validator._state_proxy.valid_vector.upstream_versions

        # balancer_pb_1 is valid
        valid_versions.append((balancer_pb_1.meta.version, {}, {}, {}))

        # balancer_pb_1 together with upstream_1_pb_1 are not valid
        u_spec_pb = model_pb2.UpstreamSpec()
        u_spec_pb.yandex_balancer.yaml = yaml.dump(INVALID_UPSTREAM_1_CONFIG)
        upstream_1_pb_1 = Api.create_upstream(NAMESPACE_ID, UPSTREAM_1_ID, u_spec_pb, disable_validation=True)

        for a in checker:
            with a:
                balancer_pb_1 = Api.get_balancer(NAMESPACE_ID, BALANCER_ID)
                assert balancer_pb_1.status.validated.status == 'Pending'
                assert (balancer_pb_1.status.validated.message ==
                        u'{"message":"Can not validate balancer spec due to absence of valid upstreams and backends"}')
                assert not ctl._validator._state_proxy.valid_vector.balancer_version
                assert not ctl._validator._state_proxy.valid_vector.upstream_versions
                assert ctl._validator._state_proxy.curr_vector.balancer_version.version == balancer_pb_1.meta.version
                full_id = (upstream_1_pb_1.meta.namespace_id, upstream_1_pb_1.meta.id)
                assert ctl._validator._state_proxy.curr_vector.upstream_versions[full_id].version == upstream_1_pb_1.meta.version

        u_spec_pb_2 = model_pb2.UpstreamSpec()
        u_spec_pb_2.yandex_balancer.yaml = yaml.dump(VALID_UPSTREAM_2_CONFIG_1)
        upstream_2_pb_1 = Api.create_upstream(NAMESPACE_ID, UPSTREAM_2_ID, u_spec_pb_2, disable_validation=True)
        # balancer_pb_1 together with upstream_2_pb_1 are valid
        valid_versions.append((balancer_pb_1.meta.version, {
            (NAMESPACE_ID, UPSTREAM_2_ID): upstream_2_pb_1.meta.version,
        }, {}, {}))
        ctl._force_revalidation = True

        for a in checker:
            with a:
                assert ctl._validator._state_proxy.valid_vector.balancer_version.version == balancer_pb_1.meta.version
                assert (
                    {full_upstream_id: upstream_version.version for full_upstream_id, upstream_version in
                     six.iteritems(ctl._validator._state_proxy.valid_vector.upstream_versions)} ==
                    {(NAMESPACE_ID, UPSTREAM_2_ID): upstream_2_pb_1.meta.version}
                )

        u_spec_pb.yandex_balancer.yaml = yaml.dump(VALID_UPSTREAM_1_CONFIG)
        upstream_1_pb_2 = Api.update_upstream(NAMESPACE_ID, UPSTREAM_1_ID,
                                              version=upstream_1_pb_1.meta.version, spec_pb=u_spec_pb,
                                              disable_validation=True)
        # balancer_pb_1 together with upstream_1_pb_2 and upstream_2_pb_1 are valid
        valid_versions.append((balancer_pb_1.meta.version, {
            (NAMESPACE_ID, UPSTREAM_1_ID): upstream_1_pb_2.meta.version,
            (NAMESPACE_ID, UPSTREAM_2_ID): upstream_2_pb_1.meta.version,
        }, {}, {}))
        ctl._force_revalidation = True

        for a in checker:
            with a:
                assert (
                    {
                        full_upstream_id: upstream_version.version for full_upstream_id, upstream_version in
                        six.iteritems(ctl._validator._state_proxy.valid_vector.upstream_versions)
                    } == {
                        (NAMESPACE_ID, UPSTREAM_1_ID): upstream_1_pb_2.meta.version,
                        (NAMESPACE_ID, UPSTREAM_2_ID): upstream_2_pb_1.meta.version
                    }
                )

    u_spec_pb_2.yandex_balancer.yaml = yaml.dump(VALID_UPSTREAM_2_CONFIG_1) + ' '
    upstream_2_pb_2 = Api.update_upstream(NAMESPACE_ID, UPSTREAM_2_ID,
                                          version=upstream_2_pb_1.meta.version, spec_pb=u_spec_pb_2,
                                          disable_validation=True)
    u_spec_pb.yandex_balancer.yaml = yaml.dump(VALID_UPSTREAM_1_CONFIG) + ' '
    upstream_1_pb_3 = Api.update_upstream(NAMESPACE_ID, UPSTREAM_1_ID,
                                          version=upstream_1_pb_2.meta.version, spec_pb=u_spec_pb,
                                          disable_validation=True)

    valid_versions.append((balancer_pb_1.meta.version, {
        (NAMESPACE_ID, UPSTREAM_1_ID): upstream_1_pb_3.meta.version,
        (NAMESPACE_ID, UPSTREAM_2_ID): upstream_2_pb_2.meta.version,
    }, {}, {}))

    u_spec_pb_2.yandex_balancer.yaml = yaml.dump(VALID_UPSTREAM_2_CONFIG_1) + '  '

    with mock.patch.object(balancer.validator.BalancerValidator, '_validate_vector', side_effect=RuntimeError):
        # break _validate_vector to prevent premature validation
        upstream_2_pb_3 = Api.update_upstream(NAMESPACE_ID, UPSTREAM_2_ID,
                                              version=upstream_2_pb_2.meta.version, spec_pb=u_spec_pb_2,
                                              disable_validation=True)
        version_to_rollback = UpstreamVersion(ctime=upstream_2_pb_3.meta.ctime,
                                              upstream_id=(upstream_2_pb_3.meta.namespace_id, upstream_2_pb_3.meta.id),
                                              version=upstream_2_pb_3.meta.version,
                                              deleted=True)

        def _validate_vector_stub(ctx, vector, valid_vector):
            versions = vector_to_versions(vector)
            if versions not in valid_versions:
                raise balancer.errors.ConfigValidationError('_validate_vector_stub: vector is not valid: qqq',
                                                            cause=version_to_rollback)
            return ValidationResult(balancer=None,
                                    included_full_upstream_ids=set(vector.upstream_versions),
                                    included_full_backend_ids=set(vector.backend_versions),
                                    included_full_endpoint_set_ids=set(vector.backend_versions),
                                    included_full_cert_ids=set(),
                                    included_full_knob_ids=set(),
                                    included_full_domain_ids=set(vector.domain_versions),
                                    included_full_weight_section_ids=set())

    with mock.patch.object(balancer.validator.BalancerValidator, '_validate_vector',
                           side_effect=_validate_vector_stub):
        for a in checker:
            with a:
                u = Api.get_upstream(NAMESPACE_ID, UPSTREAM_1_ID)
                assert u.meta.version == upstream_1_pb_3.meta.version
                assert u.statuses[-1].id == upstream_1_pb_3.meta.version
                val_pb = u.statuses[-1].validated[NAMESPACE_ID + ':' + BALANCER_ID]
                assert val_pb.status == 'True'

                u = Api.get_upstream(NAMESPACE_ID, UPSTREAM_2_ID)
                assert u.meta.version == upstream_2_pb_3.meta.version
                val_pb = u.statuses[-1].validated[NAMESPACE_ID + ':' + BALANCER_ID]
                assert val_pb.status == 'False'
                assert ujson.loads(val_pb.message)['message'] == '_validate_vector_stub: vector is not valid: qqq'

    for upstream_pb in Api.list_upstreams(NAMESPACE_ID).upstreams:
        if upstream_pb.meta.id == upstream_1_pb_3.meta.id:
            assert upstream_pb.statuses[-1].validated[NAMESPACE_ID + ':' + BALANCER_ID].status == 'True'
        elif upstream_pb.meta.id == upstream_2_pb_3.meta.id:
            s_pb = upstream_pb.statuses[-1].validated[NAMESPACE_ID + ':' + BALANCER_ID]
            assert s_pb.status == 'False'
            assert ujson.loads(s_pb.message)['message'] == '_validate_vector_stub: vector is not valid: qqq'
        else:
            raise AssertionError('Cannot get here')


@flaky(max_runs=5, min_passes=1)
@mock.patch.object(BalancerValidator, 'validate')
def test_transport(_, ctx, cache, zk_storage, checker, log, ctlrunner):
    validated_pbs = {v: {} for v in VersionName.__members__.values()}
    Api.create_namespace(NAMESPACE_ID)
    b_spec_pb = model_pb2.BalancerSpec()
    b_spec_pb.config_transport.type = model_pb2.NANNY_STATIC_FILE
    b_spec_pb.config_transport.nanny_static_file.service_id = 'service_id'
    b_spec_pb.type = model_pb2.YANDEX_BALANCER
    b_spec_pb.yandex_balancer.yaml = yaml.dump(VALID_BALANCER_CONFIG)
    b_pb = Api.create_balancer(NAMESPACE_ID, BALANCER_ID, b_spec_pb)

    ctl = BalancerCtl(NAMESPACE_ID, BALANCER_ID)
    ctlrunner.run_ctl(ctl)

    for a in checker:
        with a:
            balancer_pb_1 = Api.get_balancer(NAMESPACE_ID, BALANCER_ID)
            assert balancer_pb_1.status.validated.status == 'Unknown'
            assert balancer_pb_1.status.in_progress.status == 'False'
            assert get_revision_status(balancer_pb_1, balancer_pb_1.meta.version).validated.status == 'Unknown'

            actual_in_progress_vector = ctl._transport._balancer_state_holder.in_progress_vector
            assert actual_in_progress_vector == Vector.empty()

    # create first upstream and mark it as invalid
    u_spec_pb = model_pb2.UpstreamSpec()
    u_spec_pb.yandex_balancer.yaml = yaml.dump(INVALID_UPSTREAM_1_CONFIG)
    upstream_1_pb_1 = Api.create_upstream(NAMESPACE_ID, UPSTREAM_1_ID, u_spec_pb, disable_validation=True)

    set_upstream_validation_status(
        zk_storage, cache,
        upstream_id=UPSTREAM_1_ID, version=upstream_1_pb_1.meta.version,
        status='False', message='Not OK')

    actual_valid_vector = ctl._transport._balancer_state_holder.valid_vector
    assert actual_valid_vector == Vector.empty()

    # update first upstream and mark it as valid
    u_spec_pb.yandex_balancer.yaml = yaml.dump(VALID_UPSTREAM_1_CONFIG)
    upstream_1_pb_2 = Api.update_upstream(NAMESPACE_ID, UPSTREAM_1_ID,
                                          version=upstream_1_pb_1.meta.version,
                                          spec_pb=u_spec_pb,
                                          disable_validation=True)

    updated_upstream_1_version_2 = set_upstream_validation_status(
        zk_storage, cache,
        upstream_id=UPSTREAM_1_ID,
        version=upstream_1_pb_2.meta.version, status='True', message='OK')

    # mark balancer as valid
    updated_balancer_version_1 = set_balancer_validation_status(
        zk_storage, cache,
        version=balancer_pb_1.meta.version, status='True', message='')

    expected_valid_vector = Vector(balancer_version=updated_balancer_version_1,
                                   upstream_versions={(NAMESPACE_ID, UPSTREAM_1_ID): updated_upstream_1_version_2},
                                   backend_versions={},
                                   endpoint_set_versions={},
                                   knob_versions={},
                                   cert_versions={},
                                   domain_versions={},
                                   weight_section_versions={},
                                   validated_pbs=validated_pbs,
                                   )

    snapshot_id = 'bbb'
    snapshot_ctime = 2
    config_bundle = transport_config_bundle.ConfigBundle('config',
                                                         model_pb2.BalancerContainerSpec(), {}, {},
                                                         'svc', {}, set(), 0, None,
                                                         model_pb2.BalancerSpec.CustomServiceSettings())
    with mock.patch.object(ctl._transport, '_save_config_to_snapshot',
                           return_value=(snapshot_id, snapshot_ctime)), \
        mock.patch.object(ctl._transport, '_generate_lua_config', return_value=config_bundle):  # noqa
        ctl._transport.process(ctx)
        for a in checker:
            with a:
                actual_valid_vector = ctl._transport._balancer_state_holder.valid_vector
                assert actual_valid_vector == expected_valid_vector

                actual_in_progress_vector = ctl._transport._balancer_state_holder.in_progress_vector
                assert actual_in_progress_vector == actual_valid_vector

    for a in checker:
        with a:
            upstream_1_pb = Api.get_upstream(NAMESPACE_ID, UPSTREAM_1_ID)
            in_progress_pb = upstream_1_pb.statuses[-1].in_progress[NAMESPACE_ID + ':' + BALANCER_ID]
            assert in_progress_pb.status == 'True'
            snapshot_pbs = in_progress_pb.meta.nanny_static_file.snapshots
            assert len(snapshot_pbs) == 1
            snapshot_pb = snapshot_pbs[0]
            assert snapshot_pb.service_id == 'service_id'
            assert snapshot_pb.snapshot_id == snapshot_id
            assert snapshot_pb.ctime.ToNanoseconds() == 2 * 10 ** 6

            balancer_pb = Api.get_balancer(NAMESPACE_ID, BALANCER_ID)
            assert balancer_pb.status.in_progress.status == 'True'
            snapshot_pbs = balancer_pb.status.in_progress.meta.nanny_static_file.snapshots
            assert len(snapshot_pbs) == 1
            assert snapshot_pbs[0] == snapshot_pb

    # create second upstream and mark it as valid
    u_spec_pb_2 = model_pb2.UpstreamSpec()
    u_spec_pb_2.yandex_balancer.yaml = yaml.dump(VALID_UPSTREAM_2_CONFIG_1)
    upstream_2_pb_1 = Api.create_upstream(NAMESPACE_ID, UPSTREAM_2_ID, u_spec_pb_2, disable_validation=True)

    updated_upstream_2_version_1 = set_upstream_validation_status(
        zk_storage, cache,
        upstream_id=UPSTREAM_2_ID,
        version=upstream_2_pb_1.meta.version, status='True', message='OK')

    # update balancer and mark it as valid
    b_spec_pb.yandex_balancer.yaml = yaml.dump(VALID_BALANCER_CONFIG_2)
    balancer_pb_2 = Api.update_balancer(NAMESPACE_ID, BALANCER_ID, version=b_pb.meta.version, spec_pb=b_spec_pb)

    updated_balancer_version_2 = set_balancer_validation_status(
        zk_storage, cache,
        version=balancer_pb_2.meta.version, status='True', message='OK')

    def has_snapshot_been_active(service_id, sn_id):
        return service_id == 'service_id' and sn_id == 'bbb'

    snapshot_id = 'ccc'
    snapshot_ctime = 3
    with mock.patch.object(ctl._transport, '_save_config_to_snapshot',
                           return_value=(snapshot_id, snapshot_ctime)), \
        mock.patch.object(ctl._transport, '_generate_lua_config', return_value=config_bundle):  # noqa
        for a in checker:
            with a:
                ctl._transport.process(ctx)

                balancer_pb = Api.get_balancer(NAMESPACE_ID, BALANCER_ID)
                assert len(balancer_pb.status.in_progress.meta.nanny_static_file.snapshots) == 1

                upstream_1_pb = Api.get_upstream(NAMESPACE_ID, UPSTREAM_1_ID)
                in_progress_pb = upstream_1_pb.statuses[-1].in_progress[NAMESPACE_ID + ':' + BALANCER_ID]
                assert len(in_progress_pb.meta.nanny_static_file.snapshots) == 2

                upstream_2_pb = Api.get_upstream(NAMESPACE_ID, UPSTREAM_2_ID)
                in_progress_pb = upstream_2_pb.statuses[-1].in_progress[NAMESPACE_ID + ':' + BALANCER_ID]
                assert len(in_progress_pb.meta.nanny_static_file.snapshots) == 1

    expected_valid_vector = Vector(balancer_version=updated_balancer_version_2,
                                   upstream_versions={
                                       (NAMESPACE_ID, UPSTREAM_1_ID): updated_upstream_1_version_2,
                                       (NAMESPACE_ID, UPSTREAM_2_ID): updated_upstream_2_version_1},
                                   backend_versions={},
                                   endpoint_set_versions={},
                                   knob_versions={},
                                   cert_versions={},
                                   domain_versions={},
                                   weight_section_versions={},
                                   validated_pbs=validated_pbs,
                                   )
    expected_active_vector = Vector(balancer_version=updated_balancer_version_1,
                                    upstream_versions={
                                        (NAMESPACE_ID, UPSTREAM_1_ID): updated_upstream_1_version_2},
                                    backend_versions={},
                                    endpoint_set_versions={},
                                    knob_versions={},
                                    cert_versions={},
                                    domain_versions={},
                                    weight_section_versions={},
                                    )
    with mock.patch.object(ctl._transport.nanny_rpc_client, 'has_snapshot_been_active',
                           return_value=has_snapshot_been_active), \
        mock.patch.object(ctl._transport.nanny_client, 'get_current_runtime_attrs_id_and_ctime',
                          return_value=(snapshot_id, snapshot_ctime)):
        for a in checker:
            with a:
                ctl._transport.poll_snapshots(ctx)

                actual_valid_vector = ctl._transport._balancer_state_holder.valid_vector
                assert actual_valid_vector == expected_valid_vector

                actual_in_progress_vector = ctl._transport._balancer_state_holder.in_progress_vector
                assert actual_in_progress_vector == expected_valid_vector

                actual_active_vector = ctl._transport._balancer_state_holder.active_vector
                assert actual_active_vector == expected_active_vector

    for a in checker:
        with a:
            balancer_pb = Api.get_balancer(NAMESPACE_ID, BALANCER_ID)
            assert len(balancer_pb.status.in_progress.meta.nanny_static_file.snapshots) == 1

            upstream_1_pb = Api.get_upstream(NAMESPACE_ID, UPSTREAM_1_ID)
            in_progress_pb = upstream_1_pb.statuses[-1].in_progress[NAMESPACE_ID + ':' + BALANCER_ID]
            assert len(in_progress_pb.meta.nanny_static_file.snapshots) == 1

            upstream_2_pb = Api.get_upstream(NAMESPACE_ID, UPSTREAM_2_ID)
            in_progress_pb = upstream_2_pb.statuses[-1].in_progress[NAMESPACE_ID + ':' + BALANCER_ID]
            assert len(in_progress_pb.meta.nanny_static_file.snapshots) == 1

    def has_snapshot_been_active(service_id, sn_id):
        return service_id == 'service_id' and sn_id == 'ccc'

    expected_active_vector = Vector(balancer_version=updated_balancer_version_2,
                                    upstream_versions={
                                        (NAMESPACE_ID, UPSTREAM_1_ID): updated_upstream_1_version_2,
                                        (NAMESPACE_ID, UPSTREAM_2_ID): updated_upstream_2_version_1,
                                    },
                                    backend_versions={},
                                    endpoint_set_versions={},
                                    knob_versions={},
                                    cert_versions={},
                                    domain_versions={},
                                    weight_section_versions={},
                                    )

    with mock.patch.object(ctl._transport.nanny_rpc_client, 'has_snapshot_been_active',
                           return_value=has_snapshot_been_active), \
        mock.patch.object(ctl._transport.nanny_client, 'get_current_runtime_attrs_id_and_ctime',
                          return_value=(snapshot_id, snapshot_ctime)):
        for a in checker:
            with a:
                ctl._transport.poll_snapshots(ctx)
                # ctl._transport.process(ctx)
                actual_in_progress_vector = ctl._transport._balancer_state_holder.in_progress_vector
                assert actual_in_progress_vector.is_empty()

                actual_active_vector = ctl._transport._balancer_state_holder.active_vector
                assert actual_active_vector == expected_active_vector

    for a in checker:
        with a:
            balancer_pb = Api.get_balancer(NAMESPACE_ID, BALANCER_ID)
            assert len(balancer_pb.status.in_progress.meta.nanny_static_file.snapshots) == 0

            upstream_1_pb = Api.get_upstream(NAMESPACE_ID, UPSTREAM_1_ID)
            in_progress_pb = upstream_1_pb.statuses[-1].in_progress[NAMESPACE_ID + ':' + BALANCER_ID]
            assert len(in_progress_pb.meta.nanny_static_file.snapshots) == 0

            upstream_2_pb = Api.get_upstream(NAMESPACE_ID, UPSTREAM_2_ID)
            in_progress_pb = upstream_2_pb.statuses[-1].in_progress[NAMESPACE_ID + ':' + BALANCER_ID]
            assert len(in_progress_pb.meta.nanny_static_file.snapshots) == 0

    # update second upstream
    u_spec_pb_2.yandex_balancer.yaml = yaml.dump(VALID_UPSTREAM_2_CONFIG_2)
    upstream_2_pb_2 = Api.update_upstream(NAMESPACE_ID, UPSTREAM_2_ID,
                                          version=upstream_2_pb_1.meta.version, spec_pb=u_spec_pb_2,
                                          disable_validation=True)
    # and mark it as invalid
    set_upstream_validation_status(
        zk_storage, cache,
        upstream_id=UPSTREAM_2_ID, version=upstream_2_pb_2.meta.version, status='False', message='OK')

    for a in checker:
        with a:
            actual_valid_vector = ctl._transport._balancer_state_holder.valid_vector
            assert actual_valid_vector == expected_valid_vector
            actual_in_progress_vector = ctl._transport._balancer_state_holder.in_progress_vector
            assert actual_in_progress_vector.is_empty()

    updated_upstream_2_version_2 = set_upstream_validation_status(
        zk_storage, cache,
        upstream_id=UPSTREAM_2_ID, version=upstream_2_pb_2.meta.version, status='True', message='OK')

    snapshot_id = 'ddd'
    with mock.patch.object(ctl._transport, '_save_config_to_snapshot',
                           return_value=(snapshot_id, snapshot_ctime)), \
        mock.patch.object(ctl._transport, '_generate_lua_config', return_value=config_bundle):  # noqa
        for a in checker:
            with a:
                ctl._transport.process(ctx)

                actual_valid_vector = ctl._transport._balancer_state_holder.valid_vector
                expected_valid_vector = expected_valid_vector.replace_upstream_version((NAMESPACE_ID, UPSTREAM_2_ID),
                                                                                       updated_upstream_2_version_2)
                assert actual_valid_vector == expected_valid_vector

                actual_in_progress_vector = ctl._transport._balancer_state_holder.valid_vector
                assert actual_in_progress_vector == actual_valid_vector

    for a in checker:
        with a:
            balancer_pb = Api.get_balancer(NAMESPACE_ID, BALANCER_ID)
            assert len(balancer_pb.status.in_progress.meta.nanny_static_file.snapshots) == 1

            upstream_1_pb = Api.get_upstream(NAMESPACE_ID, UPSTREAM_1_ID)
            in_progress_pb = upstream_1_pb.statuses[-1].in_progress[NAMESPACE_ID + ':' + BALANCER_ID]
            assert len(in_progress_pb.meta.nanny_static_file.snapshots) == 1

            upstream_2_pb = Api.get_upstream(NAMESPACE_ID, UPSTREAM_2_ID)
            in_progress_pb = upstream_2_pb.statuses[-1].in_progress[NAMESPACE_ID + ':' + BALANCER_ID]
            assert len(in_progress_pb.meta.nanny_static_file.snapshots) == 1

    def has_snapshot_been_active(service_id, sn_id):
        return service_id == 'service_id' and sn_id == 'eee'

    with mock.patch.object(ctl._transport.nanny_rpc_client, 'has_snapshot_been_active',
                           side_effect=has_snapshot_been_active), \
        mock.patch.object(ctl._transport.nanny_client, 'get_current_runtime_attrs_id_and_ctime',
                          return_value=('eee', 5)), \
        mock.patch.object(ctl._transport, '_save_config_to_snapshot',
                          return_value=(snapshot_id, snapshot_ctime)), \
        mock.patch.object(ctl._transport, '_generate_lua_config', return_value=config_bundle):  # noqa
        for a in checker:
            with a:
                ctl._transport.process(ctx)
                ctl._transport.poll_snapshots(ctx)

                actual_in_progress_vector = ctl._transport._balancer_state_holder.in_progress_vector
                assert actual_in_progress_vector.is_empty()

                expected_active_vector._validated_pbs = validated_pbs
                expected_active_vector = expected_active_vector.replace_upstream_version((NAMESPACE_ID, UPSTREAM_2_ID),
                                                                                         updated_upstream_2_version_2)
                actual_active_vector = ctl._transport._balancer_state_holder.active_vector
                assert actual_active_vector == expected_active_vector

        ctl._transport.cleanup(ctx)

    for a in checker:
        with a:
            # make sure _cleanup() has removed unneeded revisions from the state:
            balancer_state_pb = Api.get_balancer_state(NAMESPACE_ID, BALANCER_ID)
            for upstream_id, statuses_pb in six.iteritems(balancer_state_pb.upstreams):
                assert len(statuses_pb.statuses) == 1

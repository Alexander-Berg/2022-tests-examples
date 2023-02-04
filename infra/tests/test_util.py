# coding: utf-8
from __future__ import unicode_literals
import inject
import json

import mock
import pytest
import six
import socket
import ujson
from infra.swatlib import pbutil
from datetime import datetime, timedelta
from google.protobuf import timestamp_pb2
from google.protobuf import json_format
from six.moves import range as xrange

from awacs.lib.rpc import exceptions
from awacs.lib.vectors import version
from awacs.model.balancer.errors import ConfigValidationError
from awacs.model.balancer.generator import resolve_nanny_snapshot_pbs, resolve_gencfg_group_pbs, get_yandex_config_pb
from awacs.model.balancer.vector import (
    Vector,
    BalancerVersion,
    UpstreamVersion,
    BackendVersion,
    EndpointSetVersion,
    DomainVersion,
    get_human_readable_diff,
)
from infra.awacs.proto import modules_pb2, internals_pb2, model_pb2
from awacs.resolver import INannyClient, IGencfgClient, resolve_host
from awacs.model.components import DotSeparatedMajorMinorVersion, DashSeparatedMajorMinorVersion, SemanticVersion
from awacs.model.util import set_condition, check_condition
from awacs.web.util import GeventFriendlyPrinter
from awacs.web.validation.util import validate_unwhitelisted_modules_nonexistence
from awtest.core import wait_until_passes


def create_instance(seed, port_shift=0, port_override=None, weight_override=None):
    n = sum(map(ord, seed))
    return internals_pb2.Instance(
        host='{}.yandex.ru'.format(seed),
        port=sum(map(ord, seed)) + port_shift if port_override is None else port_override,
        weight=(n % 13 + 1) if weight_override is None else weight_override,
        ipv4_addr='{}.0.0.1'.format(n % 250 + 1)
    )


class NannyClientStub(object):
    def list_nanny_snapshot_instances(self, service_id, snapshot_id, use_mtn):
        rv = [
            create_instance(service_id)
        ]
        if service_id == 'service_0':
            instance = create_instance(service_id + '_b')
            rv.append(instance)
        return rv


class GencfgClientStub(object):
    def list_group_instances(self, name, version, use_mtn):
        rv = [
            create_instance(name)
        ]
        if name == 'group_0':
            instance = create_instance(name + '_b')
            rv.append(instance)
        return rv


@pytest.fixture(autouse=True)
def deps(binder):
    def configure(b):
        b.bind(INannyClient, NannyClientStub())
        b.bind(IGencfgClient, GencfgClientStub())
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def test_resolve_nanny_snapshot_pbs():
    snapshot_pbs = [model_pb2.BackendSelector.NannySnapshot(service_id='service_{}'.format(i),
                                                            snapshot_id='snapshot_{}'.format(i)) for i in xrange(3)]
    instance_pbs = resolve_nanny_snapshot_pbs(snapshot_pbs)
    assert instance_pbs == [
        create_instance('service_0'),
        create_instance('service_0_b'),
        create_instance('service_1'),
        create_instance('service_2'),
    ]

    instance_pbs = resolve_nanny_snapshot_pbs(snapshot_pbs, default_port=model_pb2.Port(policy=model_pb2.Port.SHIFT, shift=666))
    assert instance_pbs == [
        create_instance('service_0', port_shift=666),
        create_instance('service_0_b', port_shift=666),
        create_instance('service_1', port_shift=666),
        create_instance('service_2', port_shift=666),
    ]

    snapshot_pbs[0].port.policy = snapshot_pbs[1].port.SHIFT
    snapshot_pbs[0].port.shift = 100
    snapshot_pbs[0].weight.SetInParent()

    snapshot_pbs[1].port.policy = snapshot_pbs[1].port.OVERRIDE
    snapshot_pbs[1].port.override = 1024

    snapshot_pbs[2].port.SetInParent()
    snapshot_pbs[2].weight.policy = snapshot_pbs[2].weight.OVERRIDE
    snapshot_pbs[2].weight.override = 999

    instance_pbs = resolve_nanny_snapshot_pbs(snapshot_pbs, default_port=model_pb2.Port(policy=model_pb2.Port.SHIFT, shift=666))
    assert instance_pbs == [
        create_instance('service_0', port_shift=100),
        create_instance('service_0_b', port_shift=100),
        create_instance('service_1', port_override=1024),
        create_instance('service_2', port_shift=666, weight_override=999),
    ]


def test_swat_4358_regression():
    # See https://st.yandex-team.ru/SWAT-4358 for details.
    service_id = 'service'
    snapshot_id = 'snapshot'
    s_1_pb = model_pb2.BackendSelector.NannySnapshot(service_id=service_id, snapshot_id=snapshot_id)
    s_2_pb = model_pb2.BackendSelector.NannySnapshot(service_id=service_id, snapshot_id=snapshot_id)
    s_2_pb.port.policy = s_2_pb.port.SHIFT
    s_2_pb.port.shift = 1

    snapshot_pbs = [s_1_pb, s_2_pb]
    instance_pbs = resolve_nanny_snapshot_pbs(snapshot_pbs)

    assert instance_pbs == [
        create_instance(service_id, port_shift=0),
        create_instance(service_id, port_shift=1),
    ]

    name = 'name'
    version = 'version'
    g_1_pb = model_pb2.BackendSelector.GencfgGroup(name=name, version=version)
    g_2_pb = model_pb2.BackendSelector.GencfgGroup(name=name, version=version)
    g_2_pb.port.policy = s_2_pb.port.OVERRIDE
    g_2_pb.port.override = 31337

    group_pbs = [g_1_pb, g_2_pb]
    instance_pbs = resolve_gencfg_group_pbs(group_pbs)

    assert instance_pbs == [
        create_instance(name),
        create_instance(name, port_override=31337),
    ]


def test_resolve_gencfg_group_pbs():
    gencfg_group_pbs = [model_pb2.BackendSelector.GencfgGroup(name='group_{}'.format(i),
                                                              version='tags/{}'.format(i)) for i in xrange(3)]
    instance_pbs = resolve_gencfg_group_pbs(gencfg_group_pbs)
    assert instance_pbs == [
        create_instance('group_0'),
        create_instance('group_0_b'),
        create_instance('group_1'),
        create_instance('group_2'),
    ]

    instance_pbs = resolve_gencfg_group_pbs(gencfg_group_pbs, default_port=model_pb2.Port(policy=model_pb2.Port.SHIFT, shift=666))
    assert instance_pbs == [
        create_instance('group_0', port_shift=666),
        create_instance('group_0_b', port_shift=666),
        create_instance('group_1', port_shift=666),
        create_instance('group_2', port_shift=666),
    ]

    gencfg_group_pbs[0].port.policy = gencfg_group_pbs[1].port.SHIFT
    gencfg_group_pbs[0].port.shift = 100
    gencfg_group_pbs[0].weight.SetInParent()

    gencfg_group_pbs[1].port.policy = gencfg_group_pbs[1].port.OVERRIDE
    gencfg_group_pbs[1].port.override = 1024
    gencfg_group_pbs[1].weight.policy = gencfg_group_pbs[0].weight.OVERRIDE
    gencfg_group_pbs[1].weight.override = 999

    gencfg_group_pbs[2].port.SetInParent()

    instance_pbs = resolve_gencfg_group_pbs(gencfg_group_pbs, default_port=model_pb2.Port(policy=model_pb2.Port.SHIFT, shift=666))
    assert instance_pbs == [
        create_instance('group_0', port_shift=100),
        create_instance('group_0_b', port_shift=100),
        create_instance('group_1', port_override=1024, weight_override=999),
        create_instance('group_2', port_shift=666),
    ]

    with mock.patch.object(GencfgClientStub, 'list_group_instances') as m:
        resolve_gencfg_group_pbs(gencfg_group_pbs, default_use_mtn=True)
    for g in gencfg_group_pbs:
        m.assert_any_call(g.name, g.version, use_mtn=True)

    gencfg_group_pbs[1].use_mtn.value = False

    with mock.patch.object(GencfgClientStub, 'list_group_instances') as m:
        resolve_gencfg_group_pbs(gencfg_group_pbs, default_use_mtn=True)
    for g in gencfg_group_pbs:
        use_mtn = g.use_mtn.value if g.HasField('use_mtn') else True
        m.assert_any_call(g.name, g.version, use_mtn=use_mtn)

    gencfg_group_pbs[1].ClearField(b'use_mtn')
    gencfg_group_pbs[2].use_mtn.value = True

    with mock.patch.object(GencfgClientStub, 'list_group_instances') as m:
        resolve_gencfg_group_pbs(gencfg_group_pbs, default_use_mtn=False)
    for g in gencfg_group_pbs:
        use_mtn = g.use_mtn.value if g.HasField('use_mtn') else False
        m.assert_any_call(g.name, g.version, use_mtn=use_mtn)


def test_get_yandex_config_pb():
    b_spec_pb = model_pb2.BalancerSpec()
    b_spec_pb.yandex_balancer.yaml = 'not-a-yaml-doc'

    u_spec_pb = model_pb2.UpstreamSpec()
    u_spec_pb.yandex_balancer.yaml = 'regexp_section: {matcher: {}, errordoc: {}}'

    with pytest.raises(ConfigValidationError) as e:
        get_yandex_config_pb(b_spec_pb)
    exc = e.value
    assert exc.to_dict() == {
        'message': 'Failed to read balancer config',
        'error': {
            'snippet': 'not-a-yaml-doc\n^',
            'position': {'column': 1, 'line': 1},
            'message': 'scalar is not accepted here, Holder expected'
        }
    }


def test_get_human_readable_diff():
    n_id_1 = 'n-id-1'
    n_id_2 = 'n-id-2'
    from_vector = Vector(
        balancer_version=BalancerVersion(ctime=0, balancer_id=(n_id_1, 'b-id'), version='a'),
        upstream_versions={
            (n_id_1, 'u-id-1'): UpstreamVersion(ctime=0, upstream_id=(n_id_1, 'u-id-1'), version='a', deleted=False),
            (n_id_1, 'u-id-3'): UpstreamVersion(ctime=0, upstream_id=(n_id_1, 'u-id-3'), version='a', deleted=False),
        },
        backend_versions={
            (n_id_1, 'back-id-1'): BackendVersion(ctime=0, backend_id=(n_id_1, 'back-id-1'), version='a',
                                                  deleted=False),
            (n_id_2, 'back-id-3'): BackendVersion(ctime=0, backend_id=(n_id_2, 'back-id-3'), version='a',
                                                  deleted=False),
        },
        endpoint_set_versions={
            (n_id_1, 'es-id-1'): EndpointSetVersion(ctime=0, endpoint_set_id=(n_id_1, 'es-id-1'), version='a',
                                                    deleted=False),
        },
        knob_versions={},
        cert_versions={},
        domain_versions={},
        weight_section_versions={},
    )
    to_vector = Vector(
        balancer_version=BalancerVersion(ctime=1, balancer_id=(n_id_1, 'b-id'), version='b'),
        upstream_versions={
            (n_id_1, 'u-id-1'): UpstreamVersion(ctime=1, upstream_id=(n_id_1, 'u-id-1'), version='b', deleted=False),
            (n_id_1, 'u-id-2'): UpstreamVersion(ctime=0, upstream_id=(n_id_1, 'u-id-2'), version='a', deleted=False),
            (n_id_1, 'u-id-3'): UpstreamVersion(ctime=1, upstream_id=(n_id_1, 'u-id-3'), version='b', deleted=True),
        },
        backend_versions={
            (n_id_1, 'back-id-1'): BackendVersion(ctime=1, backend_id=(n_id_1, 'back-id-1'), version='b',
                                                  deleted=False),
            (n_id_1, 'back-id-2'): BackendVersion(ctime=0, backend_id=(n_id_1, 'back-id-2'), version='a',
                                                  deleted=False),
        },
        endpoint_set_versions={
            (n_id_1, 'es-id-1'): EndpointSetVersion(ctime=1, endpoint_set_id=(n_id_1, 'es-id-1'), version='b',
                                                    deleted=False),
            (n_id_1, 'es-id-2'): EndpointSetVersion(ctime=0, endpoint_set_id=(n_id_1, 'es-id-2'), version='a',
                                                    deleted=False),
        },
        knob_versions={},
        cert_versions={},
        domain_versions={(n_id_1, 'd-id-1'): DomainVersion(ctime=0, domain_id=(n_id_1, 'd-id-1'), version='d',
                                                           deleted=False, incomplete=False)},
        weight_section_versions={(n_id_1, 'ws-id-1'): version.WeightSectionVersion(
            ctime=0, version_id=(n_id_1, 'ws-id-1'), version='x', deleted=False, incomplete=False)},
    )

    actual_diff = get_human_readable_diff(n_id_1, from_vector, to_vector)
    expected_diff = '''Updated:
 * balancer
 * upstream "u-id-1"
 * backend "back-id-1"
 * endpoint set "es-id-1"
Added:
 * domain "d-id-1"
 * upstream "u-id-2"
 * backend "back-id-2"
 * endpoint set "es-id-2"
 * weight section "ws-id-1"
Removed:
 * upstream "u-id-3"
 * backend "n-id-2/back-id-3"'''
    assert actual_diff == expected_diff

    actual_diff = get_human_readable_diff(n_id_1, from_vector, to_vector, show_revision_ids=True)
    expected_diff = '''Updated:
 * balancer (a -> b)
 * upstream "u-id-1" (a -> b)
 * backend "back-id-1" (a -> b)
 * endpoint set "es-id-1" (a -> b)
Added:
 * domain "d-id-1" (d)
 * upstream "u-id-2" (a)
 * backend "back-id-2" (a)
 * endpoint set "es-id-2" (a)
 * weight section "ws-id-1" (x)
Removed:
 * upstream "u-id-3" (del@b)
 * backend "n-id-2/back-id-3" (a)'''
    assert actual_diff == expected_diff


@pytest.mark.parametrize('cls,separator,expected_err', [
    (DashSeparatedMajorMinorVersion, '-', r'is not ^[0-9]+-[0-9]+$'),
    (DotSeparatedMajorMinorVersion, '.', r'is not ^[0-9]+\.[0-9]+$'),
])
def test_component_versions_major_minor(cls, separator, expected_err):
    for val in (
            '2#8',
            separator.join(['2', '8', '1']),
    ):
        with pytest.raises(ValueError) as e:
            cls.validate('2#8')
        assert six.text_type(e.value) == expected_err

    cls.validate('2{}8'.format(separator))

    v_1 = cls.parse('2{}8'.format(separator))
    assert v_1.to_key() == (2, 8)

    v_2 = cls.parse('2{}8'.format(separator))
    assert v_1 == v_2
    assert not (v_1 != v_2)
    assert v_1 >= v_2
    assert v_1 <= v_2

    v_3 = cls.parse('30{}1'.format(separator))
    assert v_3 > v_2
    assert v_2 <= v_3
    assert v_2 < v_3


def test_component_versions_semver():
    for val in ('2.8', '2', '2.8.#'):
        with pytest.raises(ValueError) as e:
            SemanticVersion.validate(val)
        assert six.text_type(e.value) == 'is not valid semantic version'

    v_1 = SemanticVersion.parse('2.1.0')
    v_2 = SemanticVersion.parse('2.1.0')

    assert v_1 == v_2
    assert not (v_1 != v_2)
    assert v_1 >= v_2
    assert v_1 <= v_2

    v_3 = SemanticVersion.parse('3.0.1')
    assert v_3 > v_2
    assert v_2 <= v_3
    assert v_2 < v_3

    v_4 = SemanticVersion.parse('0.0.1')
    v_4_pushclient = SemanticVersion.parse('0.0.1-pushclient')
    assert v_4 == v_4_pushclient


def test_gevent_friendly_printer():
    class TestGeventFriendlyPrinter(GeventFriendlyPrinter):
        MIN_IDLE_PERIOD = 10
        MAX_IDLE_PERIOD = 20

    n = 100
    pb = modules_pb2.Holder()

    curr_pb = pb
    for _ in range(n):
        curr_pb = curr_pb.accesslog.nested
    curr_pb.errordocument.status = 503

    d = pbutil.pb_to_jsondict(pb, including_default_value_fields=True,
                              pb_json_printer_cls=TestGeventFriendlyPrinter)
    with pytest.raises(TypeError) as e:
        assert json.dumps(d).count('accesslog') == n
    assert 'is not JSON serializable' in str(e.value)

    assert ujson.dumps(d).count('accesslog') == n

    # _MessageToJsonObject(message) which is used in pbutil.pb_to_jsondict
    # should return json_format._Printer()._MessageToJsonObject(message)
    # if type(json_format._Printer()._MessageToJsonObject(message)) != dict
    # So only dicts should be wrapped into GeventIdler.
    #
    # Such case happens, for example, when type(message) = google.protobuf.timestamp_pb2.Timestamp
    # cause type(json_format._Printer()._MessageToJsonObject(message)) = str

    # make idle period = 1
    class TestGeventFriendlyPrinter(GeventFriendlyPrinter):
        MIN_IDLE_PERIOD = 1
        MAX_IDLE_PERIOD = 1

    pb = model_pb2.BackendMeta(mtime=timestamp_pb2.Timestamp(seconds=100))
    d = pbutil.pb_to_jsondict(pb, including_default_value_fields=True,
                              pb_json_printer_cls=TestGeventFriendlyPrinter)
    assert d.toDict()["mtime"] == json_format._Printer(including_default_value_fields=True)._MessageToJsonObject(pb)["mtime"]


def test_conditions():
    utcnow = datetime.utcnow()
    bc = model_pb2.BoolCondition()
    assert not check_condition(bc, utcnow)

    bc2 = model_pb2.BoolCondition()
    bc2.author = 'romanovich'
    bc2.value = True

    set_condition(bc, bc2, 'ferenets', utcnow)
    assert check_condition(bc, utcnow)
    assert bc.author == 'ferenets'
    assert bc.mtime.ToDatetime() == utcnow

    tbc = model_pb2.TimedBoolCondition()
    assert not check_condition(tbc, utcnow)

    tbc2 = model_pb2.TimedBoolCondition()
    tbc2.author = 'romanovich'
    tbc2.value = True

    set_condition(tbc, tbc2, 'ferenets', utcnow)  # from -inf to +inf
    assert check_condition(tbc, utcnow)
    assert bc.author == 'ferenets'
    assert bc.mtime.ToDatetime() == utcnow

    tbc2.not_before.FromDatetime(utcnow + timedelta(hours=1))
    set_condition(tbc, tbc2, 'ferenets', utcnow)  # from utcnow + 1 hour to +inf
    assert not check_condition(tbc, utcnow)
    assert check_condition(tbc, utcnow + timedelta(hours=1))
    assert check_condition(tbc, utcnow + timedelta(hours=2))

    tbc2.not_after.FromDatetime(utcnow + timedelta(hours=3))
    set_condition(tbc, tbc2, 'ferenets', utcnow)  # from utcnow + 1 hour to utcnow + 3 hours
    assert not check_condition(tbc, utcnow)
    assert check_condition(tbc, utcnow + timedelta(hours=1))
    assert check_condition(tbc, utcnow + timedelta(hours=2))
    assert check_condition(tbc, utcnow + timedelta(hours=3))
    assert not check_condition(tbc, utcnow + timedelta(hours=3, seconds=1))


def test_validate_unwhitelisted_modules_nonexistence(cache, zk_storage, create_default_namespace):
    namespace_id = 'ferenets-test'
    create_default_namespace(namespace_id)
    validate_unwhitelisted_modules_nonexistence(namespace_id, {'webauth', 'srcrwr_ext'})

    b_pb = model_pb2.Balancer()
    b_pb.meta.namespace_id = namespace_id
    b_pb.meta.id = namespace_id + '_sas'
    b_pb.spec.config_transport.nanny_static_file.service_id = 'sid'

    section_pb = b_pb.spec.yandex_balancer.config.instance_macro.sections.add()
    section_pb.key = 'http'
    section_pb.value.nested.webauth.SetInParent()

    zk_storage.create_balancer(namespace_id=namespace_id,
                               balancer_id=b_pb.meta.id,
                               balancer_pb=b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(namespace_id, b_pb.meta.id), timeout=1)

    with pytest.raises(exceptions.BadRequestError) as e:
        validate_unwhitelisted_modules_nonexistence(namespace_id, {'webauth', 'srcrwr_ext'})
    assert str(e.value) == ('"spec.modules_whitelist": "webauth" can not be removed from whitelist, it is used in balancer'
                            ' ferenets-test:ferenets-test_sas')

    validate_unwhitelisted_modules_nonexistence(namespace_id, {'srcrwr_ext'})

    u_pb = model_pb2.Upstream()
    u_pb.meta.namespace_id = namespace_id
    u_pb.meta.id = 'default'

    u_pb.spec.yandex_balancer.config.regexp_section.matcher.match_fsm.host = 'aaa'
    srcrwr_ext_pb = u_pb.spec.yandex_balancer.config.regexp_section.nested.srcrwr_ext
    srcrwr_ext_pb.remove_prefix = 'm'
    srcrwr_ext_pb.domains = 'yp-c.yandex.net'
    srcrwr_ext_pb.nested.errordocument.status = 502

    zk_storage.create_upstream(namespace_id, u_pb.meta.id, u_pb)
    wait_until_passes(lambda: cache.must_get_upstream(namespace_id, u_pb.meta.id), timeout=1)

    with pytest.raises(exceptions.BadRequestError) as e:
        validate_unwhitelisted_modules_nonexistence(namespace_id, {'srcrwr_ext'})
    assert str(e.value) == ('"spec.modules_whitelist": "srcrwr_ext" can not be removed from whitelist, it is used in '
                            'upstream ferenets-test:default')


def test_resolve_host():
    # to test py23 regression
    with pytest.raises(Exception, match='kek'):
        with mock.patch.object(socket, 'getaddrinfo', side_effect=Exception('kek')):
            resolve_host('lapapam')

import errno
import glob
import logging
import pickle
import shutil
import tempfile

import gevent
import gevent.threadpool
import inject
import mock
import os
import pytest
import six
import time
import ujson
from flaky import flaky
from google.protobuf import json_format
from sepelib.core import config

from awacs import yamlparser
from awacs.lib import OrderedDict
from awacs.model import objects
from awacs.model.balancer import generator
from awacs.model.balancer.errors import ConfigValidationError
from awacs.model.balancer.generator import get_would_be_injected_full_backend_ids, run_heavy_operation
from awacs.model.balancer.vector import (BalancerVersion, UpstreamVersion, EndpointSetVersion, KnobVersion,
                                         CertVersion, BackendVersion, DomainVersion)
from awacs.resolver import INannyClient
from awacs.wrappers.base import Holder, ANY_MODULE, ValidationCtx
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.main import DEFAULT_KNOB_TYPES, Call
from awtest import wait_until_passes, check_log, t
from awtest.balancer import wait_for_balancer_start, safe_balancer_process
from awtest.network import is_ipv4_only, mocked_resolve_host
from infra.awacs.proto import modules_pb2, internals_pb2, model_pb2
from infra.swatlib import metrics


IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ

IPADDRS = {
    'mobile_heroism.yandex.ru': '2a02:6b8::3',
    'antirobot.man.yandex.ru': '2a02:6b8::4',
    'antirobot_iss_prestable.yandex.ru': '2a02:6b8::5',
    'bolver.yandex-team.ru': '2a02:6b8:0:3400::32',
    'laas.yandex.ru': '2a02:6b8::91',
    'sinkadm.priemka.yandex.ru': '2a02:6b8:0:3400::eeee:20',
    'search-history.yandex.net': '2a02:6b8:0:3400::3:36',
    'localhost': '::1',
    'uaas.search.yandex.net': '2a02:6b8:0:3400::120',
    'antirobot.yandex.ru': '2a02:6b8:0:3400::121',
    'google.com': '2a00:1450:4010:c0d::8b',
    'mtn.google.com': '2a00:1450:4010:c0d::8b',
    'ya.ru': '2a02:6b8::3',
    'mtn.ya.ru': '2a02:6b8::3',
    'pdb_backend_test.yandex.ru': '2a02:6b8::4',
    'pdb_nodejs_test.yandex.ru': '2a02:6b8::5',
    'ws39-438.search.yandex.net': '2a02:6b8:0:2502::2509:50dd',
    'ws39-386.search.yandex.net': '2a02:6b8:0:2502::2509:50c3',
    'ws39-272.search.yandex.net': '2a02:6b8:0:2502::2509:508a',
    'laas.yandex-team.ru': '2a02:6b8:0:2502::2509:1234',
    'laas2.yandex.net': '2a02:6b8:0:3400::1022',
    'gobabygo.yandex.net': '8.8.8.8',
    'api.sport.yandex.ru': '2a02:6b8::e',
}


def upstream_id_to_upstream_version(upstream_id):
    return UpstreamVersion(0, upstream_id, '', False)


def domain_id_to_domain_version(domain_id):
    return DomainVersion(0, domain_id, '', False, False)


def resolve_hostname_stub(hostname, **__):
    try:
        return IPADDRS[hostname]
    except KeyError:
        raise RuntimeError('Unknown hostname: {!r}'.format(hostname))


def parse2(yaml):
    def _parse(x, cls=modules_pb2.Holder):
        return yamlparser.parse(cls, x, ensure_ascii=True)

    pb = _parse(yaml)
    parsed_unparsed_pb = _parse(yamlparser.dump(pb))
    assert pb == parsed_unparsed_pb
    return pb


AM = [ANY_MODULE]


def test_call_hashability():
    call_pb = modules_pb2.Call()
    call_pb.type = modules_pb2.Call.GET_PORT_VAR
    call_pb.get_port_var_params.var = "port1"
    c = Call(call_pb)
    assert hash(c)

    rv = OrderedDict([(c, "123")])
    assert rv


def test_awacs662():
    with open(t('fixtures/awacs662/balancer.yml')) as f:
        balancer_yml = f.read()
    balancer_pb = parse2(balancer_yml)
    h = Holder(balancer_pb)

    with pytest.raises(ValidationError) as e:
        h.validate(ctx=ValidationCtx(config_type=ValidationCtx.CONFIG_TYPE_BALANCER))
    assert six.text_type(e.value) == (u'instance_macro -> sections[http_section] -> extended_http_macro -> '
                                      u'modules[0] -> regexp -> sections[default] -> '
                                      u'modules[2] -> regexp -> include_upstreams -> filter: is required')


def test_config_with_include_upstreams_1(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_version = BalancerVersion(0, (namespace_id, 'balancer_id'), '')

    with open(t('fixtures/config_1/ping_regexp_section.yml')) as f:
        ping_regexp_section_yml = f.read()
    ping_regexp_section_pb = parse2(ping_regexp_section_yml)
    ping_regexp_section = Holder(ping_regexp_section_pb)
    ping_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_1/gobabygo_regexp_section.yml')) as f:
        gobabygo_regexp_section_yml = f.read()
    gobabygo_regexp_section_pb = parse2(gobabygo_regexp_section_yml)
    gobabygo_regexp_section = Holder(gobabygo_regexp_section_pb)
    gobabygo_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_1/atomsearch_regexp_section.yml')) as f:
        atomsearch_regexp_section_yml = f.read()
    atomsearch_regexp_section_pb = parse2(atomsearch_regexp_section_yml)
    atomsearch_regexp_section = Holder(atomsearch_regexp_section_pb)
    atomsearch_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_1/captcha_regexp_section.yml')) as f:
        captcha_regexp_section_yml = f.read()
    captcha_regexp_section_pb = parse2(captcha_regexp_section_yml)
    captcha_regexp_section = Holder(captcha_regexp_section_pb)
    captcha_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_1/clck_regexp_section.yml')) as f:
        clck_regexp_section_yml = f.read()
    clck_regexp_section_pb = parse2(clck_regexp_section_yml)
    clck_regexp_section = Holder(clck_regexp_section_pb)
    clck_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_1/rtfront_regexp_section.yml')) as f:
        rtfront_regexp_section_yml = f.read()
    rtfront_regexp_section_pb = parse2(rtfront_regexp_section_yml)
    rtfront_regexp_section = Holder(rtfront_regexp_section_pb)
    rtfront_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_1/balancer.yml')) as f:
        balancer_template_yml = f.read()

    ping_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'ping'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = ping_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(ping_regexp_section_pb)

    gobabygo_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'gobabygo'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = gobabygo_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(gobabygo_regexp_section_pb)

    atomsearch_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'atomsearch'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = atomsearch_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(atomsearch_regexp_section_pb)

    captcha_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'captcha'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = captcha_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(captcha_regexp_section_pb)

    clck_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'clck'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = clck_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(clck_regexp_section_pb)

    rtfront_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'rtfront'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = rtfront_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(rtfront_regexp_section_pb)

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = 'balancer'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    def resolve_nanny_snapshots(snapshot_pbs):
        if snapshot_pbs[0].pb.use_mtn.value:
            return [
                internals_pb2.Instance(host='mtn.ya.ru', port=8080, weight=1),
                internals_pb2.Instance(host='mtn.google.com', port=9090, weight=2),
            ]
        else:
            return [
                internals_pb2.Instance(host='ya.ru', port=80, weight=1),
                internals_pb2.Instance(host='google.com', port=90, weight=2),
            ]

    with mock.patch.object(generator, 'resolve_nanny_snapshots', side_effect=resolve_nanny_snapshots), \
        mock.patch.object(generator.resolver.util, '_resolve_host', side_effect=resolve_hostname_stub):  # noqa
        u_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
            (namespace_id, 'ping'): ping_pb.spec,
            (namespace_id, 'gobabygo'): gobabygo_pb.spec,
            (namespace_id, 'atomsearch'): atomsearch_pb.spec,
            (namespace_id, 'captcha'): captcha_pb.spec,
            (namespace_id, 'clck'): clck_pb.spec,
            (namespace_id, 'rtfront'): rtfront_pb.spec,
        })}

        balancer = generator.validate_config(ns_pb, namespace_id, balancer_version, balancer_pb.spec, u_spec_pbs, {},
                                             {}, {}, {}).balancer

    with open(t('fixtures/config_1/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/config_1/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_config_with_include_upstreams_2(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_version = BalancerVersion(0, (namespace_id, 'balancer_id'), '')

    with open(t('fixtures/config_2/ping_regexp_section.yml')) as f:
        ping_regexp_section_yml = f.read()
    ping_regexp_section_pb = parse2(ping_regexp_section_yml)
    ping_regexp_section = Holder(ping_regexp_section_pb)
    ping_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_2/gobabygo_regexp_section.yml')) as f:
        gobabygo_regexp_section_yml = f.read()
    gobabygo_regexp_section_pb = parse2(gobabygo_regexp_section_yml)
    gobabygo_regexp_section = Holder(gobabygo_regexp_section_pb)
    gobabygo_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_2/pdb_regexp_section.yml')) as f:
        pdb_regexp_section_yml = f.read()
    pdb_regexp_section_pb = parse2(pdb_regexp_section_yml)
    pdb_regexp_section = Holder(pdb_regexp_section_pb)
    pdb_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_2/balancer.yml')) as f:
        balancer_template_yml = f.read()

    ping_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'ping'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = ping_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(ping_regexp_section_pb)
    pb.spec.labels['order'] = '010'

    gobabygo_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'gobabygo'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = gobabygo_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(gobabygo_regexp_section_pb)
    pb.spec.labels['order'] = '020'

    pdb_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'gobabygo'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = pdb_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(pdb_regexp_section_pb)
    pb.spec.labels['order'] = '030'

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = 'balancer'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    def resolve_nanny_snapshots_stub(snapshots):
        instances = []
        for snapshot in snapshots:
            s_id = snapshot.pb.service_id
            if s_id == 'pdb_backend_test':
                instances.append(
                    internals_pb2.Instance(host='pdb_backend_test.yandex.ru', port=80, weight=1),
                )
            elif s_id == 'pdb_nodejs_test':
                instances.append(
                    internals_pb2.Instance(host='pdb_nodejs_test.yandex.ru', port=80, weight=1),
                )
            else:
                instances.extend([
                    internals_pb2.Instance(host='ya.ru', port=80, weight=1),
                    internals_pb2.Instance(host='google.com', port=90, weight=2),
                ])
        return instances

    def resolve_gencfg_groups_stub(gencfg_groups):
        instances = []
        for group in gencfg_groups:
            name = group.pb.name
            version = group.pb.version
            if name == 'MSK_ANTIROBOT_ANTIROBOT' and version == 'tags/stable-92-r105':
                instances.append(
                    internals_pb2.Instance(host='antirobot.yandex.ru', port=80, weight=1),
                )
            else:
                raise AssertionError('unexpected gencfg group {}:{}'.format(name, version))
        return instances

    with mock.patch.object(generator, 'resolve_nanny_snapshots', side_effect=resolve_nanny_snapshots_stub), \
        mock.patch.object(generator, 'resolve_gencfg_groups', side_effect=resolve_gencfg_groups_stub), \
        mock.patch.object(generator.resolver.util, '_resolve_host', side_effect=resolve_hostname_stub):  # noqa
        u_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
            (namespace_id, 'ping'): ping_pb.spec,
            (namespace_id, 'gobabygo'): gobabygo_pb.spec,
            (namespace_id, 'pdb'): pdb_pb.spec,
        })}
        balancer = generator.validate_config(ns_pb, namespace_id, balancer_version, balancer_pb.spec, u_spec_pbs, {},
                                             {}, {}, {}).balancer

    with open(t('fixtures/config_2/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/config_2/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_config_with_include_upstreams_3(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_version = BalancerVersion(0, (namespace_id, 'balancer_id'), '')

    with open(t('fixtures/config_3/configsearch_regexp_section.yml')) as f:
        configsearch_regexp_section_yml = f.read()
    configsearch_regexp_section_pb = parse2(configsearch_regexp_section_yml)
    configsearch_regexp_section = Holder(configsearch_regexp_section_pb)
    configsearch_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_3/jsonproxy_regexp_section.yml')) as f:
        jsonproxy_regexp_section_yml = f.read()
    jsonproxy_regexp_section_pb = parse2(jsonproxy_regexp_section_yml)
    jsonproxy_regexp_section = Holder(jsonproxy_regexp_section_pb)
    jsonproxy_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_3/by_name_policy_func_section.yml')) as f:
        by_name_policy_func_section_yml = f.read()
    by_name_policy_func_section_pb = parse2(by_name_policy_func_section_yml)
    by_name_policy_func_section = Holder(by_name_policy_func_section_pb)
    by_name_policy_func_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_3/balancer.yml')) as f:
        balancer_template_yml = f.read()

    configsearch_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'configsearch'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = configsearch_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(configsearch_regexp_section_pb)
    pb.spec.labels['order'] = '010'

    jsonproxy_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'jsonproxy'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = jsonproxy_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(jsonproxy_regexp_section_pb)
    pb.spec.labels['order'] = '020'

    by_name_policy_func_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'by_name_policy_func'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = by_name_policy_func_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(by_name_policy_func_section_pb)
    pb.spec.labels['order'] = '030'

    cert_spec_pb = model_pb2.CertificateSpec()
    cert_version = CertVersion(0, (namespace_id, 'adm-nanny.yandex-team.ru'), 'xxx', False, False)

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = 'balancer'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    def resolve_nanny_snapshots_stub(snapshots):
        instances = []
        for snapshot in snapshots:
            s_id = snapshot.pb.service_id
            if s_id == 'mobile_heroism':
                instances.append(
                    internals_pb2.Instance(host='mobile_heroism.yandex.ru', port=80, weight=1),
                )
            elif s_id == 'production_antirobot_iss_prestable':
                instances.append(
                    internals_pb2.Instance(host='antirobot_iss_prestable.yandex.ru', port=80, weight=1),
                )
            else:
                raise AssertionError('unexpected nanny service {}'.format(s_id))
        return instances

    def resolve_gencfg_groups_stub(gencfg_groups):
        instances = []
        for group in gencfg_groups:
            name = group.pb.name
            version = group.pb.version
            if name == 'MAN_ANTIROBOT_ANTIROBOT' and version == 'tags/stable-92-r105':
                instances.append(
                    internals_pb2.Instance(host='antirobot.man.yandex.ru', port=80, weight=1),
                )
            else:
                raise AssertionError('unexpected gencfg group {}:{}'.format(name, version))
        return instances

    with mock.patch.object(generator, 'resolve_nanny_snapshots', side_effect=resolve_nanny_snapshots_stub), \
        mock.patch.object(generator, 'resolve_gencfg_groups', side_effect=resolve_gencfg_groups_stub), \
        mock.patch.object(generator.resolver.util, '_resolve_host', side_effect=resolve_hostname_stub):  # noqa
        u_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
            (namespace_id, 'configsearch'): configsearch_pb.spec,
            (namespace_id, 'jsonproxy'): jsonproxy_pb.spec,
            (namespace_id, 'by_name_policy_func'): by_name_policy_func_pb.spec,
        })}
        cert_spec_pbs = {cert_version: cert_spec_pb}
        balancer = generator.validate_config(
            namespace_pb=ns_pb,
            namespace_id=namespace_id,
            balancer_version=balancer_version,
            balancer_spec_pb=balancer_pb.spec,
            upstream_spec_pbs=u_spec_pbs,
            backend_spec_pbs={},
            endpoint_set_spec_pbs={},
            knob_spec_pbs={},
            cert_spec_pbs=cert_spec_pbs).balancer

    with open(t('fixtures/config_3/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/config_3/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_config_with_include_domains(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_version = BalancerVersion(0, (namespace_id, 'balancer_id'), '')

    with open(t('fixtures/config_with_domains/configsearch_regexp_section.yml')) as f:
        configsearch_regexp_section_yml = f.read()
    configsearch_regexp_section_pb = parse2(configsearch_regexp_section_yml)
    configsearch_regexp_section = Holder(configsearch_regexp_section_pb)
    configsearch_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_with_domains/jsonproxy_regexp_section.yml')) as f:
        jsonproxy_regexp_section_yml = f.read()
    jsonproxy_regexp_section_pb = parse2(jsonproxy_regexp_section_yml)
    jsonproxy_regexp_section = Holder(jsonproxy_regexp_section_pb)
    jsonproxy_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_with_domains/easy_section.yml')) as f:
        easy_section_yml = f.read()
    easy_section_pb = parse2(easy_section_yml)
    easy_section = Holder(easy_section_pb)
    easy_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_with_domains/balancer.yml')) as f:
        balancer_template_yml = f.read()

    configsearch_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'configsearch'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = configsearch_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(configsearch_regexp_section_pb)
    pb.spec.labels['order'] = '010'

    jsonproxy_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'jsonproxy'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = jsonproxy_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(jsonproxy_regexp_section_pb)
    pb.spec.labels['order'] = '020'

    easy_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'by_name_policy_func'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = easy_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(easy_section_pb)
    pb.spec.labels['order'] = '030'

    cert_spec_pb = model_pb2.CertificateSpec()
    cert_version = CertVersion(0, (namespace_id, 'adm-nanny.yandex-team.ru'), 'xxx', False, False)

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = 'balancer'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    def resolve_nanny_snapshots_stub(snapshots):
        instances = []
        for snapshot in snapshots:
            s_id = snapshot.pb.service_id
            if s_id == 'mobile_heroism':
                instances.append(
                    internals_pb2.Instance(host='mobile_heroism.yandex.ru', port=80, weight=1),
                )
            elif s_id == 'production_antirobot_iss_prestable':
                instances.append(
                    internals_pb2.Instance(host='antirobot_iss_prestable.yandex.ru', port=80, weight=1),
                )
            else:
                raise AssertionError('unexpected nanny service {}'.format(s_id))
        return instances

    def resolve_gencfg_groups_stub(gencfg_groups):
        instances = []
        for group in gencfg_groups:
            name = group.pb.name
            version = group.pb.version
            if name == 'MAN_ANTIROBOT_ANTIROBOT' and version == 'tags/stable-92-r105':
                instances.append(
                    internals_pb2.Instance(host='antirobot.man.yandex.ru', port=80, weight=1),
                )
            else:
                raise AssertionError('unexpected gencfg group {}:{}'.format(name, version))
        return instances

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend(['http_and_https_{}'.format(i) for i in range(101)])  # 101 is a case for AWACS-822
    domain_config_pb.shadow_fqdns.extend(['shadow_http_and_https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_pb.cert.id = 'adm-nanny.yandex-team.ru'
    domain_config_pb.verify_client_cert.SetInParent()
    domain_config_pb.include_upstreams.type = modules_pb2.ALL

    domain_spec_2_pb = model_pb2.DomainSpec()
    domain_config_2_pb = domain_spec_2_pb.yandex_balancer.config
    domain_config_2_pb.fqdns.extend(['http_and_https_1000'])
    domain_config_2_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_2_pb.cert.id = 'adm-nanny.yandex-team.ru'
    domain_config_2_pb.include_upstreams.type = modules_pb2.ALL

    backend_spec_pb = model_pb2.BackendSpec()
    backend_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    backend_spec_pb.selector.yp_endpoint_sets.add(cluster='man', endpoint_set_id='xxx')
    backend_version = BackendVersion(
        ctime='2018-02-02t09:27:31.216017z',
        backend_id=(namespace_id, 'xxx'),
        version=1, deleted=False
    )
    u_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
        (namespace_id, 'configsearch'): configsearch_pb.spec,
        (namespace_id, 'jsonproxy'): jsonproxy_pb.spec,
        (namespace_id, 'easy'): easy_pb.spec,
    })}
    domain_spec_pbs = {
        DomainVersion(domain_id=('ns', 'domain'),
                      version=1,
                      deleted=False,
                      incomplete=False,
                      ctime=1): domain_spec_pb,
        DomainVersion(domain_id=('ns', 'domain2'),
                      version=1,
                      deleted=False,
                      incomplete=False,
                      ctime=1): domain_spec_2_pb
    }
    cert_spec_pbs = {cert_version: cert_spec_pb}

    with pytest.raises(ConfigValidationError) as e:
        generator.validate_config(
            namespace_pb=ns_pb,
            namespace_id=namespace_id,
            balancer_version=balancer_version,
            balancer_spec_pb=balancer_pb.spec,
            upstream_spec_pbs=u_spec_pbs,
            backend_spec_pbs={
                backend_version: backend_spec_pb,
            },
            endpoint_set_spec_pbs={},
            knob_spec_pbs={},
            cert_spec_pbs=cert_spec_pbs,
            domain_spec_pbs=domain_spec_pbs)
    assert e.value.message == (u'l7_macro -> include_domains: "verify_client_cert": can not be different in domains '
                               u'using same certificate: ("domain", "domain2")')
    domain_config_2_pb.verify_client_cert.SetInParent()

    with mock.patch.object(generator, 'resolve_nanny_snapshots', side_effect=resolve_nanny_snapshots_stub), \
        mock.patch.object(generator, 'resolve_gencfg_groups', side_effect=resolve_gencfg_groups_stub), \
        mock.patch.object(generator.resolver.util, '_resolve_host', side_effect=resolve_hostname_stub):  # noqa
        balancer = generator.validate_config(
            namespace_pb=ns_pb,
            namespace_id=namespace_id,
            balancer_version=balancer_version,
            balancer_spec_pb=balancer_pb.spec,
            upstream_spec_pbs=u_spec_pbs,
            backend_spec_pbs={
                backend_version: backend_spec_pb,
            },
            endpoint_set_spec_pbs={},
            knob_spec_pbs={},
            cert_spec_pbs=cert_spec_pbs,
            domain_spec_pbs=domain_spec_pbs).balancer

    with open(t('fixtures/config_with_domains/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/config_with_domains/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_config_with_knobs(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_id = 'balancer_id'
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    with open(t('fixtures/config_with_knobs/gobabygo_regexp_section.yml')) as f:
        gobabygo_regexp_section_yml = f.read()
    gobabygo_regexp_section_pb = parse2(gobabygo_regexp_section_yml)

    with open(t('fixtures/config_with_knobs/exp_regexp_section.yml')) as f:
        exp_regexp_section_yml = f.read()
    exp_regexp_section_pb = parse2(exp_regexp_section_yml)

    with open(t('fixtures/config_with_knobs/balancer.yml')) as f:
        balancer_template_yml = f.read()

    gobabygo_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'gobabygo'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = gobabygo_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(gobabygo_regexp_section_pb)
    pb.spec.labels['order'] = '010'

    exp_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'exp'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = exp_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(exp_regexp_section_pb)
    pb.spec.labels['order'] = '020'

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = 'balancer'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml
    pb.spec.validator_settings.knobs_mode = model_pb2.BalancerValidatorSettings.KNOBS_ALLOWED

    def resolve_nanny_snapshots_stub(snapshots):
        instances = []
        for snapshot in snapshots:
            s_id = snapshot.pb.service_id
            if s_id == 'gobabygo':
                instances.append(
                    internals_pb2.Instance(host='gobabygo.yandex.net', port=80, weight=1),
                )
            else:
                raise AssertionError('unexpected nanny service {}'.format(s_id))
        return instances

    def resolve_gencfg_groups_stub(gencfg_groups):
        instances = []
        for group in gencfg_groups:
            name = group.pb.name
            version = group.pb.version
            if name == 'MSK_ANTIROBOT_ANTIROBOT' and version == 'tags/stable-92-r105':
                instances.append(
                    internals_pb2.Instance(host='antirobot.yandex.ru', port=80, weight=1),
                )
            else:
                raise AssertionError('unexpected gencfg group {}:{}'.format(name, version))
        return instances

    knob_spec_pbs = {}
    knob_ids = {
        'reset-dns-cache-file': model_pb2.KnobSpec.BOOLEAN,
        'attempts': model_pb2.KnobSpec.INTEGER,
        'between-locations-attempts': model_pb2.KnobSpec.INTEGER,
        'no-keepalive-file': model_pb2.KnobSpec.BOOLEAN,
        'active-check-reply-weight-file': model_pb2.KnobSpec.INTEGER,
        'active-check-reply-disable-file': model_pb2.KnobSpec.BOOLEAN,
        'rr-weights-file': model_pb2.KnobSpec.YB_BACKEND_WEIGHTS,
        'weighted2-weights-file': model_pb2.KnobSpec.YB_BACKEND_WEIGHTS,
        'rendezvous-hashing-weights-file': model_pb2.KnobSpec.YB_BACKEND_WEIGHTS,
        'ocsp-file-switch': model_pb2.KnobSpec.BOOLEAN,
        'antirobot-no-cut-request-file': model_pb2.KnobSpec.BOOLEAN,
        'antirobot-file-switch': model_pb2.KnobSpec.BOOLEAN,
        'antirobot-macro-file-switch': model_pb2.KnobSpec.BOOLEAN,
        'geobase-macro-file-switch': model_pb2.KnobSpec.BOOLEAN,
        'rpcrewrite-file-switch': model_pb2.KnobSpec.BOOLEAN,
        'rpcrewrite-macro-file-switch': model_pb2.KnobSpec.BOOLEAN,
        'request-replier-rate-file': model_pb2.KnobSpec.RATE,
        'cookie-hasher-file-switch': model_pb2.KnobSpec.BOOLEAN,
        'icookie-hasher-file-switch': model_pb2.KnobSpec.BOOLEAN,
        'antirobot-wrapper-no-cut-request-file': model_pb2.KnobSpec.BOOLEAN,
        'exp-halting-points-file': model_pb2.KnobSpec.YB_EXP_HALTING_POINTS,
        'exp-testing-switch-file': model_pb2.KnobSpec.BOOLEAN,
        'exp-switch-pre': model_pb2.KnobSpec.BOOLEAN,
        'no-remote-log-file': model_pb2.KnobSpec.BOOLEAN,
        'staff-login-checker-file-switch': model_pb2.KnobSpec.BOOLEAN,
        'staff-login-checker-no-blackbox-file': model_pb2.KnobSpec.BOOLEAN,
    }
    for id_, type_ in six.iteritems(knob_ids):
        knob_version = KnobVersion(0, (namespace_id, id_), '', False)
        knob_spec_pb = model_pb2.KnobSpec()
        knob_spec_pb.type = type_
        knob_spec_pb.its_watched_state.filename = id_
        knob_spec_pb.its_watched_state.ruchka_id = id_
        knob_spec_pb.its_watched_state.its_location_paths[balancer_id] = 'its/location'
        knob_spec_pbs[knob_version] = knob_spec_pb

    def generate_balancer():
        with mock.patch.object(generator, 'resolve_nanny_snapshots', side_effect=resolve_nanny_snapshots_stub), \
            mock.patch.object(generator, 'resolve_gencfg_groups', side_effect=resolve_gencfg_groups_stub), \
            mock.patch.object(generator.resolver.util, '_resolve_host', side_effect=resolve_hostname_stub):  # noqa
            upstream_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
                (namespace_id, 'gobabygo'): gobabygo_pb.spec,
                (namespace_id, 'exp'): exp_pb.spec,
            })}
            b = generator.validate_config(
                namespace_pb=ns_pb,
                namespace_id=namespace_id,
                balancer_version=balancer_version,
                balancer_spec_pb=balancer_pb.spec,
                upstream_spec_pbs=upstream_spec_pbs,
                backend_spec_pbs={},
                endpoint_set_spec_pbs={},
                knob_spec_pbs=knob_spec_pbs,
                cert_spec_pbs={},
            ).balancer
            return b

    balancer = generate_balancer()

    with open(t('fixtures/config_with_knobs/config-knobs-allowed.lua')) as f:
        expected_lua = f.read()

    for i in range(3):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/config_with_knobs/config-knobs-allowed.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua

    gobabygo_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'gobabygo'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = gobabygo_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(gobabygo_regexp_section_pb)
    pb.spec.labels['order'] = '010'

    exp_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'exp'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = exp_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(exp_regexp_section_pb)
    pb.spec.labels['order'] = '020'

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = 'balancer'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml
    pb.spec.validator_settings.knobs_mode = model_pb2.BalancerValidatorSettings.KNOBS_ENABLED

    balancer = generate_balancer()
    with open(t('fixtures/config_with_knobs/config-knobs-enabled.lua')) as f:
        expected_lua = f.read()

    for i in range(3):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/config_with_knobs/config-knobs-enabled.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua

    default_knob_ids = {
        'balancer_antirobot_module_switch',
        'no_cut_request_file',
        'reset_dns_cache',
        'balancer_disable_keepalive',
        'common_watermark_policy_params_file',
        'balancer_expgetter_switch',
        'balancer_ocsp_switch',
        'http2_request_rate',
        'service_balancer_off',
        'balancer_geolib_switch',
        'balancer_disable_rpcrewrite_module',
        'common_request_replier_rate',
        'balancer_disable_cookie_hasher',
        'balancer_tcp_check_on',
        'service_balancer_off',
        'balancer_remote_log_switch',
    }
    for id_ in default_knob_ids:
        knob_version = KnobVersion(0, (namespace_id, id_), '', False)
        knob_spec_pb = model_pb2.KnobSpec()
        knob_spec_pb.type = DEFAULT_KNOB_TYPES[id_]
        knob_spec_pb.its_watched_state.filename = id_
        knob_spec_pb.its_watched_state.ruchka_id = id_
        knob_spec_pb.its_watched_state.its_location_paths[balancer_id] = 'its/location'
        knob_spec_pbs[knob_version] = knob_spec_pb

    gobabygo_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'gobabygo'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = gobabygo_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(gobabygo_regexp_section_pb)
    pb.spec.labels['order'] = '010'

    exp_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'exp'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = exp_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(exp_regexp_section_pb)
    pb.spec.labels['order'] = '020'

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = 'balancer'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml
    pb.spec.validator_settings.knobs_mode = model_pb2.BalancerValidatorSettings.KNOBS_ENABLED

    balancer = generate_balancer()
    with open(t('fixtures/config_with_knobs/config-knobs-enabled-with-default-knobs.lua')) as f:
        expected_lua = f.read()

    for i in range(3):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/config_with_knobs/config-knobs-enabled-with-default-knobs.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_config_with_certs(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_id = 'balancer_id'
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    with open(t('fixtures/config_with_certs/ping_regexp_section.yml')) as f:
        ping_regexp_section_yml = f.read()
    ping_regexp_section_pb = parse2(ping_regexp_section_yml)
    ping_regexp_section = Holder(ping_regexp_section_pb)
    ping_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/config_with_certs/balancer.yml')) as f:
        balancer_template_yml = f.read()

    ping_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'ping'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = ping_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(ping_regexp_section_pb)

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = 'balancer'
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    cert_spec_pbs = {}
    for cert_id in ('rcss-ext.search.yandex.net', 'rcss-ext.search.yandex.net_secondary',
                    'exp.yandex-team.ru', 'ab.yandex-team.ru', 'exp1.yandex-team.ru',
                    'ec.search.yandex.net'):
        cert_version = CertVersion(0, (namespace_id, cert_id), '', False, False)
        cert_spec_pb = model_pb2.CertificateSpec()
        cert_spec_pb.fields.subject_alternative_names.append(cert_id)
        if cert_id == 'ec.search.yandex.net':
            cert_spec_pb.fields.public_key_info.algorithm_id = 'ec'
        cert_spec_pbs[cert_version] = cert_spec_pb

    def resolve_nanny_snapshots(snapshot_pbs):
        if snapshot_pbs[0].pb.use_mtn.value:
            return [
                internals_pb2.Instance(host='mtn.ya.ru', port=8080, weight=1),
                internals_pb2.Instance(host='mtn.google.com', port=9090, weight=2),
            ]
        else:
            return [
                internals_pb2.Instance(host='ya.ru', port=80, weight=1),
                internals_pb2.Instance(host='google.com', port=90, weight=2),
            ]

    def generate_balancer():
        with mock.patch.object(generator, 'resolve_nanny_snapshots', side_effect=resolve_nanny_snapshots), \
            mock.patch.object(generator.resolver.util, '_resolve_host', side_effect=resolve_hostname_stub):  # noqa
            u_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
                (namespace_id, 'ping'): ping_pb.spec,
            })}
            b = generator.validate_config(
                namespace_pb=ns_pb,
                namespace_id=namespace_id,
                balancer_version=balancer_version,
                balancer_spec_pb=balancer_pb.spec,
                upstream_spec_pbs=u_spec_pbs,
                backend_spec_pbs={},
                endpoint_set_spec_pbs={},
                knob_spec_pbs={},
                cert_spec_pbs=cert_spec_pbs,
            ).balancer
            return b

    balancer = generate_balancer()

    with open(t('fixtures/config_with_certs/config.lua')) as f:
        expected_lua = f.read()

    for i in range(3):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/config_with_certs/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_on_status_code_with_backends(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_id = 'balancer_id'
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    with open(t('fixtures/swat7129/default.yml')) as f:
        upstream_yml = f.read()
    upstream_pb = parse2(upstream_yml)
    upstream = Holder(upstream_pb)
    upstream.validate(preceding_modules=AM)

    with open(t('fixtures/swat7129/balancer.yml')) as f:
        balancer_template_yml = f.read()

    upstream_spec_pb = model_pb2.UpstreamSpec()
    upstream_spec_pb.type = model_pb2.YANDEX_BALANCER
    upstream_spec_pb.yandex_balancer.yaml = upstream_yml
    upstream_spec_pb.labels['order'] = '0'
    upstream_spec_pb.yandex_balancer.config.CopyFrom(upstream_pb)

    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = 'balancer'
    balancer_pb.spec.type = model_pb2.YANDEX_BALANCER
    balancer_pb.spec.yandex_balancer.yaml = balancer_template_yml

    backend_spec_pb = model_pb2.BackendSpec()
    backend_spec_pb.selector.type = model_pb2.BackendSelector.MANUAL

    es_spec_pb = model_pb2.EndpointSetSpec()
    instance_pb = es_spec_pb.instances.add()
    instance_pb.ipv6_addr = "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"
    instance_pb.host = "man1-0234.search.yandex.net"
    instance_pb.port = 13512
    instance_pb.weight = 1293.0

    u_spec_pbs = {upstream_id_to_upstream_version((namespace_id, 'default')): upstream_spec_pb}
    backend_spec_pbs = {BackendVersion(0, (namespace_id, 'backend_id'), '', False): backend_spec_pb}
    es_spec_pbs = {EndpointSetVersion(0, (namespace_id, 'backend_id'), '', False): es_spec_pb}

    def resolve_nanny_snapshots(snapshot_pbs):
        if snapshot_pbs[0].pb.use_mtn.value:
            return [
                internals_pb2.Instance(host='mtn.ya.ru', port=8080, weight=1),
                internals_pb2.Instance(host='mtn.google.com', port=9090, weight=2),
            ]
        else:
            return [
                internals_pb2.Instance(host='ya.ru', port=80, weight=1),
                internals_pb2.Instance(host='google.com', port=90, weight=2),
            ]

    def generate_balancer():
        with mock.patch.object(generator, 'resolve_nanny_snapshots', side_effect=resolve_nanny_snapshots), \
            mock.patch.object(generator.resolver.util, '_resolve_host', side_effect=resolve_hostname_stub):  # noqa
            b = generator.validate_config(
                namespace_pb=ns_pb,
                namespace_id=namespace_id,
                balancer_version=balancer_version,
                balancer_spec_pb=balancer_pb.spec,
                upstream_spec_pbs=u_spec_pbs,
                backend_spec_pbs=backend_spec_pbs,
                endpoint_set_spec_pbs=es_spec_pbs,
                knob_spec_pbs={},
                cert_spec_pbs={},
            ).balancer
            return b

    balancer = generate_balancer()

    with open(t('fixtures/swat7129/config.lua')) as f:
        expected_lua = f.read()

    for i in range(3):
        # make sure we _always_ get the same resulting Lua
        cfg = (balancer.module or balancer.chain).to_config()
        lua = cfg.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/swat7129/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_services_balancer_config(binder, create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_version = BalancerVersion(0, (namespace_id, 'balancer_id'), '')

    with open(t('fixtures/services_balancer/gobabygo_regexp_section.yml')) as f:
        gobabygo_regexp_section_yml = f.read()
    gobabygo_regexp_section_pb = parse2(gobabygo_regexp_section_yml)
    gobabygo_regexp_section = Holder(gobabygo_regexp_section_pb)
    gobabygo_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/services_balancer/gobabygo2_regexp_section.yml')) as f:
        gobabygo2_regexp_section_yml = f.read()
    gobabygo2_regexp_section_pb = parse2(gobabygo2_regexp_section_yml)
    gobabygo2_regexp_section = Holder(gobabygo2_regexp_section_pb)
    gobabygo2_regexp_section.validate(preceding_modules=AM)

    with open(t('fixtures/services_balancer/balancer.yml')) as f:
        balancer_template_yml = f.read()

    gobabygo_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'gobabygo'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = gobabygo_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(gobabygo_regexp_section_pb)
    pb.spec.labels['order'] = '010'

    gobabygo2_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'gobabygo2'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = gobabygo2_regexp_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(gobabygo2_regexp_section_pb)
    pb.spec.labels['order'] = '010'

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = 'balancer'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    def list_nanny_snapshot_instances(service_id, snapshot_id, use_mtn=False):
        instances = []
        if service_id == 'gobabygo':
            instances.extend([
                internals_pb2.Instance(host='ws39-438.search.yandex.net', port=1029, weight=1.),
                internals_pb2.Instance(host='ws39-386.search.yandex.net', port=1034, weight=1.),
                internals_pb2.Instance(host='ws39-272.search.yandex.net', port=1034, weight=1.),
            ])
        elif service_id == 'geobase':
            instances.extend([
                internals_pb2.Instance(host='laas.yandex-team.ru', port=80, weight=1.),
            ])
        else:
            raise AssertionError('unexpected nanny service {}'.format(service_id))
        return instances

    def resolve_gencfg_groups_stub(gencfg_groups):
        instances = []
        for group in gencfg_groups:
            pass
            # FIXME local variable 'name' is assigned to but never used
            # name = group.pb.name
            # FIXME local variable 'version' is assigned to but never used
            # version = group.pb.version
        return instances

    def configure(b):
        nanny_client = mock.Mock()
        nanny_client.list_nanny_snapshot_instances = list_nanny_snapshot_instances
        b.bind(INannyClient, nanny_client)
        binder(b)

    inject.clear_and_configure(configure)

    with mock.patch.object(generator, 'resolve_gencfg_groups', side_effect=resolve_gencfg_groups_stub), \
        mock.patch.object(generator.resolver.util, '_resolve_host', side_effect=resolve_hostname_stub):  # noqa
        u_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
            (namespace_id, 'gobabygo'): gobabygo_pb.spec,
            (namespace_id, 'gobabygo2'): gobabygo2_pb.spec,
        })}
        balancer = generator.validate_config(
            ns_pb, namespace_id, balancer_version, balancer_pb.spec, u_spec_pbs, {}, {}, {}, {}).balancer

    with open(t('fixtures/services_balancer/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        balancer.expand_macroses()
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/services_balancer/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def get_childs_log_path(dir_path):
    for filename in os.listdir(dir_path):
        if filename.startswith('current-childs_log-balancer-'):
            return os.path.join(dir_path, filename)


def render(namespace_id, balancer_id, balancer):
    """
    :type namespace_id: six.string_types
    :type balancer_id: six.string_types
    :type balancer: Holder
    :rtype: six.string_types
    """
    ctx = ValidationCtx(sd_client_name='awacs-l7-balancer({}:{})'.format(namespace_id, balancer_id),
                        namespace_id=namespace_id)
    config = (balancer.module or balancer.chain).to_config(ctx=ctx)
    lua = config.to_top_level_lua()
    # hack to test non-standard certs location in !c
    # normally we would control where we put certs, but can't do it during test
    for cert_name in ('beta.mobsearch.yandex.ru.pem', 'beta.mobsearch.yandex.ru_secondary.pem'):
        lua = lua.replace('get_private_cert_path("{}", "/dev/shm/balancer");'.format(cert_name),
                          'get_private_cert_path("{}", "./env/");'.format(cert_name))
        lua = lua.replace('get_public_cert_path("allCAs-{}", "/dev/shm/balancer/priv");'.format(cert_name),
                          'get_public_cert_path("allCAs-{}", "./env/");'.format(cert_name))
    return lua


def test_swat3931(create_default_namespace):
    namespace_id = 'n_id'
    create_default_namespace(namespace_id)

    fixture_dir_path = os.path.join(t('fixtures'), 'swat3931')
    yml_path = os.path.join(fixture_dir_path, 'upstream.yml')
    with open(yml_path) as f:
        holder_pb = parse2(f.read())
        holder = Holder(holder_pb)
        holder.validate(preceding_modules=AM)

    expected_full_backend_ids = {
        (namespace_id, 'antirobot2'),
        (namespace_id, 'images_search_test_man'),
        (namespace_id, 'images_search_test_sas'),
        (namespace_id, 'images_search_test_vla'),
        (namespace_id, 'pumpkin'),
        ('uaas.search.yandex.net', 'usersplit_sas'),
        ('uaas.search.yandex.net', 'usersplit_man'),
        ('uaas.search.yandex.net', 'usersplit_vla'),
    }

    assert set(get_would_be_injected_full_backend_ids(namespace_id, holder)) == expected_full_backend_ids


@flaky(max_runs=3, min_passes=1)
@pytest.mark.slow
@pytest.mark.parametrize('fixture_dirname,extra_launch_params,env_vars,enable_sd_stub', [
    ('beta.mobsearch.yandex.ru', [
        '-Vport=11111',
        '-Vdisable_external=1',
    ], {}, True),
    ('hamster.yandex.com.tr', [
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {'TOKEN': '123'}, False),
    ('swat3381', [
        '-Vworkers=1',
        '-Vdisable_external=1',
    ], {}, False),
    ('awacs462', [
        '-Vworkers=1',
        '-Vdisable_external=1',
    ], {}, False),
    ('swat4452', [
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, False),
    ('swat6229', [
        '-Vport=11111',
        '-Vdisable_external=1',
    ], {}, False),
    ('l7', [
        '-Vport=11111',
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, True),
    ('l7-2', [
        '-Vport=11111',
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, True),
    ('l7_check_replies', [
        '-Vport=11111',
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, True),
    ('l7-3', [
        '-Vport=11111',
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, True),
    ('l7-3_trust_xffy', [
        '-Vport=11111',
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, True),
    ('l7-3_trust_xffy_w_antirobot', [
        '-Vport=11111',
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, True),
    ('l7-3_dont_trust_xffy_w_antirobot', [
        '-Vport=11111',
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, True),
    ('l7-3_2_trust_xffy_w_antirobot', [
        '-Vport=11111',
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, True),
    ('l7-3_3_x_yandex_ja3_antirobot', [
        '-Vport=11111',
        '-Vget_workers_provider=./dump_json_get_workers_provider.lua',
        '-Vdisable_external=1',
    ], {}, True),
])
@pytest.mark.usefixtures('sd_stub')
def test_old_style_fixtures(request, fixture_dirname, extra_launch_params, env_vars, balancer_executable_path,
                            enable_sd_stub,
                            create_default_namespace, zk_storage, cache, ctx):
    fixture_dir_path = os.path.join(t('fixtures/old_style'), fixture_dirname)
    namespace_id = fixture_dirname

    ns_pb = create_default_namespace(namespace_id)
    if fixture_dirname == 'beta.mobsearch.yandex.ru':
        ns_pb.spec.modules_whitelist.modules.append('srcrwr')

    config.set_value('run.modules_blacklist', [{'name': 'srcrwr'}])
    if namespace_id in ('beta.mobsearch.yandex.ru',):
        def check():
            assert cache.must_get_namespace(namespace_id).spec.modules_whitelist.modules

        for namespace_pb in zk_storage.update_namespace(namespace_id):
            namespace_pb.spec.modules_whitelist.modules.append('srcrwr')
        wait_until_passes(check)

    upstream_pbs = {}
    upstream_holders = {}

    labels_path = os.path.join(fixture_dir_path, 'LABELS')
    with open(labels_path) as f:
        labels = ujson.load(f)

    cache_path = os.path.join(fixture_dir_path, 'CACHE')
    with open(cache_path) as f:
        cache = ujson.load(f)

    for yml_path in glob.glob(os.path.join(fixture_dir_path, '*.yml')):
        with open(yml_path) as f:
            if yml_path.endswith('balancer.yml'):
                balancer_pb = pb = model_pb2.Balancer()
                pb.meta.id = 'balancer'
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.yandex_balancer.yaml = f.read()
                if namespace_id in ('l7-3',):
                    pb.spec.env_type = pb.spec.L7_ENV_TESTING
            else:
                upstream_id = yml_path[:-len('.yml')].rsplit('/', 1)[1]
                pb = model_pb2.Upstream()
                pb.meta.id = upstream_id
                pb.meta.namespace_id = namespace_id
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.yandex_balancer.yaml = f.read()

                section_pb = parse2(pb.spec.yandex_balancer.yaml)
                section = Holder(section_pb)
                section.validate(preceding_modules=AM,
                                 ctx=ValidationCtx(config_type=ValidationCtx.CONFIG_TYPE_UPSTREAM,
                                                   namespace_id=namespace_id))

                pb.spec.yandex_balancer.config.CopyFrom(section_pb)
                pb.spec.labels.update(labels[upstream_id])

                upstream_pbs[(namespace_id, upstream_id)] = pb
                upstream_holders[(namespace_id, upstream_id)] = section

    assert balancer_pb
    balancer_id = balancer_pb.meta.id
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    def resolve_nanny_snapshots_stub(snapshots):
        instances = []
        for snapshot in snapshots:
            service_id = snapshot.pb.service_id
            snapshot_id = snapshot.pb.snapshot_id
            msg = internals_pb2.InstancesList()
            json_format.Parse(cache['nanny_snapshots'][service_id][snapshot_id], msg)
            instances.extend(msg.instances)
        return instances

    def resolve_gencfg_groups_stub(gencfg_groups):
        instances = []
        for group in gencfg_groups:
            name = group.pb.name
            version = group.pb.version
            msg = internals_pb2.InstancesList()
            json_format.Parse(cache['gencfg_groups'][name][version], msg)
            instances.extend(msg.instances)
        return instances

    with open(os.path.join(fixture_dir_path, 'config.lua')) as f:
        expected_lua = f.read()

    endpoint_set_common_antirobot_man_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-antirobot', 'antirobot_man'),
        version=1, deleted=False
    )
    endpoint_set_common_antirobot_sas_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-antirobot', 'antirobot_sas'),
        version=1, deleted=False
    )
    endpoint_set_common_antirobot_vla_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-antirobot', 'antirobot_vla'),
        version=1, deleted=False
    )
    endpoint_set_common_antirobot_man_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_common_antirobot_man_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:b000:6030:92e2:baff:fe74:7b88',
        host='man1-0234.search.yandex.net', ipv4_addr='', port=13512, weight=1293
    )
    endpoint_set_common_antirobot_man_spec_pb.is_global.value = True
    endpoint_set_common_antirobot_sas_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_common_antirobot_sas_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:b000:151:225:90ff:fe83:8d4',
        host='sas1-0281.search.yandex.net', ipv4_addr='', port=13512, weight=435
    )
    endpoint_set_common_antirobot_sas_spec_pb.is_global.value = True
    endpoint_set_common_antirobot_vla_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_common_antirobot_vla_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:c0e:a2:0:604:db7:9b9d',
        host='vla1-2571.search.yandex.net', ipv4_addr='', port=13512, weight=2042
    )
    endpoint_set_common_antirobot_vla_spec_pb.is_global.value = True

    endpoint_set_common_antirobot_man_sd_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-antirobot', 'antirobot_man_yp'),
        version=1, deleted=False
    )
    endpoint_set_common_antirobot_sas_sd_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-antirobot', 'antirobot_sas_yp'),
        version=1, deleted=False
    )
    endpoint_set_common_antirobot_vla_sd_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-antirobot', 'antirobot_vla_yp'),
        version=1, deleted=False
    )
    endpoint_set_common_rpslimiter_man_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-rpslimiter', 'rpslimiter-serval-man'),
        version=1, deleted=False
    )
    endpoint_set_common_rpslimiter_sas_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-rpslimiter', 'rpslimiter-serval-sas'),
        version=1, deleted=False
    )
    endpoint_set_common_rpslimiter_vla_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-rpslimiter', 'rpslimiter-serval-vla'),
        version=1, deleted=False
    )
    endpoint_set_common_rpslimiter_man_sd_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-rpslimiter', 'rpslimiter-serval-man-sd'),
        version=1, deleted=False
    )
    endpoint_set_common_rpslimiter_sas_sd_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-rpslimiter', 'rpslimiter-serval-sas-sd'),
        version=1, deleted=False
    )
    endpoint_set_common_rpslimiter_vla_sd_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('common-rpslimiter', 'rpslimiter-serval-vla-sd'),
        version=1, deleted=False
    )
    endpoint_set_common_rpslimiter_man_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_common_rpslimiter_man_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:b000:6030:92e2:baff:fe74:7b88',
        host='man1-1.search.yandex.net', ipv4_addr='', port=1234, weight=1
    )
    endpoint_set_common_rpslimiter_man_spec_pb.is_global.value = True
    endpoint_set_common_rpslimiter_sas_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_common_rpslimiter_sas_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:b000:151:225:90ff:fe83:8d4',
        host='sas1-2.search.yandex.net', ipv4_addr='', port=1234, weight=1
    )
    endpoint_set_common_rpslimiter_sas_spec_pb.is_global.value = True
    endpoint_set_common_rpslimiter_vla_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_common_rpslimiter_vla_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:c0e:a2:0:604:db7:9b9d',
        host='vla1-3.search.yandex.net', ipv4_addr='', port=1234, weight=1
    )
    endpoint_set_common_rpslimiter_vla_spec_pb.is_global.value = True

    backend_common_global_spec_pb = model_pb2.BackendSpec()
    backend_common_global_spec_pb.is_global.value = True

    backend_common_rpslimiter_man_sd_spec_pb = model_pb2.BackendSpec()
    backend_common_rpslimiter_man_sd_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    backend_common_rpslimiter_man_sd_spec_pb.selector.yp_endpoint_sets.add(cluster='man',
                                                                           endpoint_set_id='rpslimiter-serval-man-sd')
    backend_common_rpslimiter_man_sd_spec_pb.is_global.value = True

    backend_common_rpslimiter_sas_sd_spec_pb = model_pb2.BackendSpec()
    backend_common_rpslimiter_sas_sd_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    backend_common_rpslimiter_sas_sd_spec_pb.selector.yp_endpoint_sets.add(cluster='sas',
                                                                           endpoint_set_id='rpslimiter-serval-sas-sd')
    backend_common_rpslimiter_sas_sd_spec_pb.is_global.value = True

    backend_common_rpslimiter_vla_sd_spec_pb = model_pb2.BackendSpec()
    backend_common_rpslimiter_vla_sd_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    backend_common_rpslimiter_vla_sd_spec_pb.selector.yp_endpoint_sets.add(cluster='vla',
                                                                           endpoint_set_id='rpslimiter-serval-vla-sd')
    backend_common_rpslimiter_vla_sd_spec_pb.is_global.value = True

    backend_common_antirobot_man_sd_spec_pb = model_pb2.BackendSpec()
    backend_common_antirobot_man_sd_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    backend_common_antirobot_man_sd_spec_pb.selector.yp_endpoint_sets.add(cluster='man',
                                                                          endpoint_set_id='prod-antirobot-yp-man')
    backend_common_antirobot_man_sd_spec_pb.is_global.value = True

    backend_common_antirobot_sas_sd_spec_pb = model_pb2.BackendSpec()
    backend_common_antirobot_sas_sd_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    backend_common_antirobot_sas_sd_spec_pb.selector.yp_endpoint_sets.add(cluster='sas',
                                                                          endpoint_set_id='prod-antirobot-yp-sas')
    backend_common_antirobot_sas_sd_spec_pb.is_global.value = True

    backend_common_antirobot_vla_sd_spec_pb = model_pb2.BackendSpec()
    backend_common_antirobot_vla_sd_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    backend_common_antirobot_vla_sd_spec_pb.selector.yp_endpoint_sets.add(cluster='vla',
                                                                          endpoint_set_id='prod-antirobot-yp-vla')
    backend_common_antirobot_vla_sd_spec_pb.is_global.value = True

    endpoint_set_uaas_man_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('uaas.search.yandex.net', 'usersplit_man'),
        version=1, deleted=False
    )
    endpoint_set_uaas_sas_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('uaas.search.yandex.net', 'usersplit_sas'),
        version=1, deleted=False
    )
    endpoint_set_uaas_vla_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('uaas.search.yandex.net', 'usersplit_vla'),
        version=1, deleted=False
    )
    endpoint_set_uaas_man_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_uaas_man_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:c0b:3ae1:100:1101::3c78',
        host='man1-4336-man-uaas-balancer-15480.gencfg-c.yandex.net', ipv4_addr='', port=15480, weight=1293
    )
    endpoint_set_uaas_man_spec_pb.is_global.value = True
    endpoint_set_uaas_sas_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_uaas_sas_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:c08:6a21:100:47b::3c78',
        host='balancer-sas25-sas-uaas-balancer-15480.gencfg-c.yandex.net', ipv4_addr='', port=15480, weight=1293
    )
    endpoint_set_uaas_sas_spec_pb.is_global.value = True
    endpoint_set_uaas_vla_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_uaas_vla_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:c0d:4fa5:10b:2909::3c78',
        host='vla1-0032-vla-uaas-balancer-15480.gencfg-c.yandex.net', ipv4_addr='', port=15480, weight=1293
    )
    endpoint_set_uaas_vla_spec_pb.is_global.value = True

    endpoint_set_usersplit_man_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('uaas.search.yandex.net', 'usersplit_man'),
        version=1, deleted=False
    )
    endpoint_set_usersplit_sas_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('uaas.search.yandex.net', 'usersplit_sas'),
        version=1, deleted=False
    )
    endpoint_set_usersplit_vla_version = EndpointSetVersion(
        ctime='2018-02-02t09:27:31.216017z',
        endpoint_set_id=('uaas.search.yandex.net', 'usersplit_vla'),
        version=1, deleted=False
    )
    endpoint_set_usersplit_man_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_usersplit_man_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:c0b:3ae1:100:1101::1111',
        host='usersplit-1.gencfg-c.yandex.net', ipv4_addr='', port=80, weight=1
    )
    endpoint_set_usersplit_man_spec_pb.is_global.value = True
    endpoint_set_usersplit_sas_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_usersplit_sas_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:c08:6a21:100:47b::1111',
        host='usersplit-2.gencfg-c.yandex.net', ipv4_addr='', port=80, weight=1
    )
    endpoint_set_usersplit_sas_spec_pb.is_global.value = True
    endpoint_set_usersplit_vla_spec_pb = model_pb2.EndpointSetSpec()
    endpoint_set_usersplit_vla_spec_pb.instances.add(
        ipv6_addr='2a02:6b8:c0d:4fa5:10b:2909::1111',
        host='usersplit-3.gencfg-c.yandex.net', ipv4_addr='', port=80, weight=1
    )
    endpoint_set_usersplit_vla_spec_pb.is_global.value = True

    cert_hamster_version = CertVersion(
        ctime='2018-02-02t09:27:31.216017z',
        cert_id=(namespace_id, 'hamster.yandex.tld'),
        version=1, deleted=False, incomplete=False
    )
    cert_mobsearch_version = CertVersion(
        ctime='2018-02-02t09:27:31.216017z',
        cert_id=(namespace_id, 'beta.mobsearch.yandex.ru'),
        version=1, deleted=False, incomplete=False
    )
    cert_mobsearch_sec_version = CertVersion(
        ctime='2018-02-02t09:27:31.216017z',
        cert_id=(namespace_id, 'beta.mobsearch.yandex.ru_secondary'),
        version=1, deleted=False, incomplete=False
    )

    cert_nanny_version = CertVersion(
        ctime='2018-02-02t09:27:31.216017z',
        cert_id=(namespace_id, 'nanny.yandex-team.ru'),
        version=1, deleted=False, incomplete=False
    )

    cert_hq_version = CertVersion(
        ctime='2018-02-02t09:27:31.216017z',
        cert_id=(namespace_id, 'hq.yandex-team.ru'),
        version=1, deleted=False, incomplete=False
    )

    ws_hey_version = objects.WeightSection.version(
        ctime='2018-02-02t09:27:31.216017z',
        version_id=(namespace_id, 'hey'),
        version=1, deleted=False, incomplete=False
    )
    ws_hey_spec_pb = model_pb2.WeightSectionSpec()
    ws_hey_spec_pb.locations.add(name='SAS', default_weight=30)

    with mock.patch.object(generator, 'resolve_nanny_snapshots', side_effect=resolve_nanny_snapshots_stub), \
        mock.patch.object(generator, 'resolve_gencfg_groups', side_effect=resolve_gencfg_groups_stub), \
        mock.patch.object(generator.resolver, 'resolve_host', side_effect=resolve_hostname_stub):  # noqa
        upstream_spec_pbs = {upstream_id_to_upstream_version(upstream_id): upstream_pb.spec
                             for upstream_id, upstream_pb in six.iteritems(upstream_pbs)}
        b = time.time()
        result = generator.validate_config(
            ns_pb, namespace_id, balancer_version, balancer_pb.spec, upstream_spec_pbs,
            {
                BackendVersion(*endpoint_set_common_antirobot_man_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_common_antirobot_sas_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_common_antirobot_vla_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_common_antirobot_man_sd_version): backend_common_antirobot_man_sd_spec_pb,
                BackendVersion(*endpoint_set_common_antirobot_sas_sd_version): backend_common_antirobot_sas_sd_spec_pb,
                BackendVersion(*endpoint_set_common_antirobot_vla_sd_version): backend_common_antirobot_vla_sd_spec_pb,
                BackendVersion(*endpoint_set_common_rpslimiter_man_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_common_rpslimiter_sas_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_common_rpslimiter_vla_version): backend_common_global_spec_pb,
                BackendVersion(
                    *endpoint_set_common_rpslimiter_man_sd_version): backend_common_rpslimiter_man_sd_spec_pb,
                BackendVersion(
                    *endpoint_set_common_rpslimiter_sas_sd_version): backend_common_rpslimiter_sas_sd_spec_pb,
                BackendVersion(
                    *endpoint_set_common_rpslimiter_vla_sd_version): backend_common_rpslimiter_vla_sd_spec_pb,
                BackendVersion(*endpoint_set_uaas_man_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_uaas_sas_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_uaas_vla_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_usersplit_man_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_usersplit_sas_version): backend_common_global_spec_pb,
                BackendVersion(*endpoint_set_usersplit_vla_version): backend_common_global_spec_pb,
            },
            {
                endpoint_set_common_antirobot_man_version: endpoint_set_common_antirobot_man_spec_pb,
                endpoint_set_common_antirobot_sas_version: endpoint_set_common_antirobot_sas_spec_pb,
                endpoint_set_common_antirobot_vla_version: endpoint_set_common_antirobot_vla_spec_pb,

                endpoint_set_common_rpslimiter_man_version: endpoint_set_common_rpslimiter_man_spec_pb,
                endpoint_set_common_rpslimiter_sas_version: endpoint_set_common_rpslimiter_sas_spec_pb,
                endpoint_set_common_rpslimiter_vla_version: endpoint_set_common_rpslimiter_vla_spec_pb,

                endpoint_set_uaas_man_version: endpoint_set_uaas_man_spec_pb,
                endpoint_set_uaas_sas_version: endpoint_set_uaas_sas_spec_pb,
                endpoint_set_uaas_vla_version: endpoint_set_uaas_vla_spec_pb,

                endpoint_set_usersplit_man_version: endpoint_set_usersplit_man_spec_pb,
                endpoint_set_usersplit_sas_version: endpoint_set_usersplit_sas_spec_pb,
                endpoint_set_usersplit_vla_version: endpoint_set_usersplit_vla_spec_pb,
            },
            {},
            {
                cert_hamster_version: model_pb2.CertificateSpec(),
                cert_mobsearch_version: model_pb2.CertificateSpec(),
                cert_mobsearch_sec_version: model_pb2.CertificateSpec(),
                cert_nanny_version: model_pb2.CertificateSpec(),
                cert_hq_version: model_pb2.CertificateSpec(),
            },
            {},
            {
                ws_hey_version: ws_hey_spec_pb
            }
        )
        balancer = result.balancer
        a = time.time()
        ctx.log.info('\n{}: validation time = {}, validation ticks = {}'.format(fixture_dirname, (a - b),
                                                                                result.validation_ctx._tick_counter))

        n = 3
        b = time.time()
        for i in range(n):
            # make sure we _always_ get the same resulting Lua
            lua = render(namespace_id, balancer_id, balancer)
            if lua != expected_lua:
                if IS_ARCADIA:
                    from yatest import common
                    config_new_dir = common.output_path('oldstyle')
                    if not os.path.exists(config_new_dir):
                        os.mkdir(config_new_dir)
                    fixture_dir_path = os.path.join(config_new_dir, namespace_id)
                    if not os.path.exists(config_new_dir):
                        os.mkdir(fixture_dir_path)
                with open(os.path.join(fixture_dir_path, 'config.lua.new'), 'w') as f:
                    f.write(lua)
            assert lua == expected_lua
        a = time.time()
    ctx.log.info('{}: config generation time = {}\n'.format(fixture_dirname, (a - b) / n))

    if not balancer_executable_path:
        return

    if not os.path.exists(balancer_executable_path):
        pytest.fail('{} does not exist'.format(balancer_executable_path))

    with tempfile.NamedTemporaryFile(suffix='.lua') as f:
        try:
            if enable_sd_stub:
                sd_stub = request.getfixturevalue('sd_stub')
                lua = lua.replace('sd.yandex.net', '127.0.0.1' if is_ipv4_only() else '::1')
                lua = lua.replace('port = 8080;', 'port = {};'.format(sd_stub.port))
            f.write(lua.encode('utf-8'))
            f.flush()
            env_dir = os.path.abspath(os.path.join(fixture_dir_path, 'env'))

            if IS_ARCADIA:
                from yatest import common
                from infra.swatlib.logutil import rndstr
                writable_env_dir = common.output_path('envtestgenerator' + rndstr())
                if os.path.exists(env_dir):
                    shutil.copytree(env_dir, writable_env_dir)
                else:
                    os.mkdir(writable_env_dir)
                env_dir = writable_env_dir
                log_dir = os.path.join(env_dir, 'logs')
                os.mkdir(log_dir)
            else:
                if not os.path.exists(env_dir):
                    os.mkdir(env_dir)
                log_dir = tempfile.mkdtemp()

            args = [
                f.name,
                '-Vlog_dir={}'.format(log_dir),
                '-Vprivate_cert_dir={}'.format(env_dir),
                '-Vpublic_cert_dir={}'.format(env_dir),
                '-Vca_cert_dir={}'.format(env_dir),
            ]
            if extra_launch_params:
                args.extend(extra_launch_params)

            timeout = 5
            if enable_sd_stub:
                time.sleep(1)  # XXX by romanovich@
            with safe_balancer_process(ctx, balancer_executable_path,
                                       args, fixture_dirname, timeout, env_dir, env_vars) as balancer_process:
                balancer_started = wait_for_balancer_start(ctx, balancer_process, timeout, log_dir,
                                                           cert_is_expired=fixture_dirname == 'swat6229')
                if not balancer_started:
                    pytest.fail('{}\'s balancer did not start'.format(fixture_dirname))
        finally:
            try:
                shutil.rmtree(log_dir)  # delete directory
            except OSError as exc:
                if exc.errno != errno.ENOENT:  # ENOENT - no such file or directory
                    raise  # re-raise exception


def test_validate_shared_and_report_refs():
    yml_template = '''---
instance_macro:
  buffer: 65536
  maxconn: 1000
  workers: 1
  log_dir: /place/db/www/logs/
  sections:
    admin:
      ips: [127.0.0.1, '::1']
      ports: [21520]
      shared:
        uuid: %s
        http:
          maxlen: 65536
          maxreq: 65536
          admin: {}
    stats_storage:
      ips: [127.0.0.4]
      ports: [21520]
      modules:
        - shared:
            uuid: %s'''

    balancer_pb = parse2(yml_template % ('uuid1', 'uuid1'))
    balancer = Holder(balancer_pb)
    balancer.expand_macroses()
    balancer.validate()
    balancer.validate_shared_and_report_refs()

    balancer_pb = parse2(yml_template % ('uuid1', 'uuid2'))
    balancer = Holder(balancer_pb)
    balancer.expand_macroses()
    balancer.validate()
    with pytest.raises(ValidationError) as e:
        balancer.validate_shared_and_report_refs()
    assert e.match('the following referenced "shared" uuids do not exist: "uuid2"')

    yml_template = '''---
instance_macro:
  buffer: 65536
  maxconn: 1000
  workers: 1
  log_dir: /place/db/www/logs/
  sections:
    admin:
      ips: [127.0.0.1, '::1']
      ports: [21520]
      report:
        uuid: %s
        ranges: default
        http:
          maxlen: 65536
          maxreq: 65536
          admin: {}
    stats_storage:
      ips: [127.0.0.4]
      ports: [21520]
      modules:
        - report:
            refers: %s
            ranges: default
        - errordocument: {status: 200}'''

    balancer_pb = parse2(yml_template % ('uuid1', 'uuid1'))
    balancer = Holder(balancer_pb)
    balancer.expand_macroses()
    balancer.validate()
    balancer.validate_shared_and_report_refs()

    balancer_pb = parse2(yml_template % ('uuid1', 'uuid2'))
    balancer = Holder(balancer_pb)
    balancer.expand_macroses()
    balancer.validate()
    with pytest.raises(ValidationError) as e:
        balancer.validate_shared_and_report_refs()
    assert e.match('the following referenced "report" uuids do not exist: "uuid2"')

    yml_template = '''---
instance_macro:
  buffer: 65536
  maxconn: 1000
  workers: 1
  log_dir: /place/db/www/logs/
  sections:
    admin_1:
      ips: [127.0.0.1, '::1']
      ports: [21520]
      shared:
        uuid: {first_uuid}
        http:
          maxlen: {first_maxlen}
          maxreq: 65536
          admin: {{}}
    admin_2:
      ips: [127.0.0.2, '::1']
      ports: [22520]
      shared:
        uuid: {second_uuid}
        http:
          maxlen: {second_maxlen}
          maxreq: 65536
          admin: {{}}
    stats_storage:
      ips: [127.0.0.4]
      ports: [21520]
      modules:
        - shared:
            uuid: {third_uuid}'''
    yml = yml_template.format(first_uuid='uuid1',
                              second_uuid='uuid1',
                              third_uuid='uuid1',
                              first_maxlen=1,
                              second_maxlen=1)
    balancer_pb = parse2(yml)
    balancer = Holder(balancer_pb)
    balancer.expand_macroses()
    balancer.validate()
    balancer.validate_shared_and_report_refs()

    yml = yml_template.format(first_uuid='uuid1',
                              second_uuid='uuid1',
                              third_uuid='uuid1',
                              first_maxlen=1,
                              second_maxlen=2)
    balancer_pb = parse2(yml)
    balancer = Holder(balancer_pb)
    balancer.expand_macroses()
    balancer.validate()
    with pytest.raises(ValidationError) as e:
        balancer.validate_shared_and_report_refs()
    assert e.match('the following shared uuids defined more than once: "uuid1"')

    yml = yml_template.format(first_uuid='uuid1',
                              second_uuid='uuid1',
                              third_uuid='uuid2',
                              first_maxlen=1,
                              second_maxlen=1)
    balancer_pb = parse2(yml)
    balancer = Holder(balancer_pb)
    balancer.expand_macroses()
    balancer.validate()
    with pytest.raises(ValidationError) as e:
        balancer.validate_shared_and_report_refs()
    assert e.match('the following referenced "shared" uuids do not exist: "uuid2"')


def test_config_with_regexp_path(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_id = 'b_id'
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    with open(t('fixtures/regexp_path/led_regexp_path_section.yml')) as f:
        led_section_yml = f.read()
    led_section_pb = parse2(led_section_yml)
    led_section = Holder(led_section_pb)
    led_section.validate(preceding_modules=AM)

    with open(t('fixtures/regexp_path/zeppelin_regexp_path_section.yml')) as f:
        zeppelin_section_yml = f.read()
    zeppelin_section_pb = parse2(zeppelin_section_yml)
    zeppelin_section = Holder(zeppelin_section_pb)
    zeppelin_section.validate(preceding_modules=AM)

    with open(t('fixtures/regexp_path/default_regexp_path_section.yml')) as f:
        default_section_yml = f.read()
    default_section_pb = parse2(default_section_yml)
    default_section = Holder(default_section_pb)
    default_section.validate(preceding_modules=AM)

    with open(t('fixtures/regexp_path/balancer.yml')) as f:
        balancer_template_yml = f.read()

    led_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'led'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = led_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(led_section_pb)

    zeppelin_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'zeppelin'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = zeppelin_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(zeppelin_section_pb)

    default_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'default'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = default_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(default_section_pb)

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = balancer_id
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    u_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
        (namespace_id, 'led'): led_pb.spec,
        (namespace_id, 'zeppelin'): zeppelin_pb.spec,
        (namespace_id, 'default'): default_pb.spec,
    })}

    balancer = generator.validate_config(ns_pb, namespace_id, balancer_version, balancer_pb.spec, u_spec_pbs, {},
                                         {}, {}, {}).balancer

    with open(t('fixtures/regexp_path/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/regexp_path/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_config_with_prefix_path_router(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_id = 'b_id'
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    with open(t('fixtures/prefix_path_router/led_prefix_path_router_section.yml')) as f:
        led_section_yml = f.read()
    led_section_pb = parse2(led_section_yml)
    led_section = Holder(led_section_pb)
    led_section.validate(preceding_modules=AM)

    with open(t('fixtures/prefix_path_router/zeppelin_prefix_path_router_section.yml')) as f:
        zeppelin_section_yml = f.read()
    zeppelin_section_pb = parse2(zeppelin_section_yml)
    zeppelin_section = Holder(zeppelin_section_pb)
    zeppelin_section.validate(preceding_modules=AM)

    with open(t('fixtures/prefix_path_router/default_prefix_path_router_section.yml')) as f:
        default_section_yml = f.read()
    default_section_pb = parse2(default_section_yml)
    default_section = Holder(default_section_pb)
    default_section.validate(preceding_modules=AM)

    with open(t('fixtures/prefix_path_router/balancer.yml')) as f:
        balancer_template_yml = f.read()

    led_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'led'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = led_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(led_section_pb)

    zeppelin_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'zeppelin'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = zeppelin_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(zeppelin_section_pb)

    default_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'default'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = default_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(default_section_pb)

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = balancer_id
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    u_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
        (namespace_id, 'led'): led_pb.spec,
        (namespace_id, 'zeppelin'): zeppelin_pb.spec,
        (namespace_id, 'default'): default_pb.spec,
    })}

    balancer = generator.validate_config(ns_pb, namespace_id, balancer_version, balancer_pb.spec, u_spec_pbs, {},
                                         {}, {}, {}).balancer

    with open(t('fixtures/prefix_path_router/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/prefix_path_router/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_config_with_regexp_host(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_id = 'b_id'
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    with open(t('fixtures/regexp_host/led_regexp_host_section.yml')) as f:
        led_section_yml = f.read()
    led_section_pb = parse2(led_section_yml)
    led_section = Holder(led_section_pb)
    led_section.validate(preceding_modules=AM)

    with open(t('fixtures/regexp_host/zeppelin_regexp_host_section.yml')) as f:
        zeppelin_section_yml = f.read()
    zeppelin_section_pb = parse2(zeppelin_section_yml)
    zeppelin_section = Holder(zeppelin_section_pb)
    zeppelin_section.validate(preceding_modules=AM)

    with open(t('fixtures/regexp_host/default_regexp_host_section.yml')) as f:
        default_section_yml = f.read()
    default_section_pb = parse2(default_section_yml)
    default_section = Holder(default_section_pb)
    default_section.validate(preceding_modules=AM)

    with open(t('fixtures/regexp_host/balancer.yml')) as f:
        balancer_template_yml = f.read()

    led_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'led'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = led_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(led_section_pb)

    zeppelin_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'zeppelin'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = zeppelin_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(zeppelin_section_pb)

    default_pb = pb = model_pb2.Upstream()
    pb.meta.id = 'default'
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = default_section_yml
    pb.spec.yandex_balancer.config.CopyFrom(default_section_pb)

    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = balancer_id
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    u_spec_pbs = {upstream_id_to_upstream_version(upstream_id): u for upstream_id, u in six.iteritems({
        (namespace_id, 'led'): led_pb.spec,
        (namespace_id, 'zeppelin'): zeppelin_pb.spec,
        (namespace_id, 'default'): default_pb.spec,
    })}

    balancer = generator.validate_config(ns_pb, namespace_id, balancer_version, balancer_pb.spec, u_spec_pbs, {},
                                         {}, {}, {}).balancer

    with open(t('fixtures/regexp_host/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/regexp_host/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_config_with_asterisk(create_default_namespace):
    namespace_id = 'n_id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_id = 'b_id'
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    with open(t('fixtures/asterisk/balancer.yml')) as f:
        balancer_template_yml = f.read()
    balancer_pb = pb = model_pb2.Balancer()
    pb.meta.id = balancer_id
    pb.meta.namespace_id = namespace_id
    pb.spec.type = model_pb2.YANDEX_BALANCER
    pb.spec.yandex_balancer.yaml = balancer_template_yml

    balancer = generator.validate_config(ns_pb, namespace_id, balancer_version, balancer_pb.spec, {}, {}, {}, {},
                                         {}).balancer

    with open(t('fixtures/asterisk/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (balancer.module or balancer.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/asterisk/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_swat3639():
    with open(t('fixtures/swat3639/balancer.yml')) as f:
        yml = f.read()
    pb = parse2(yml)
    h = Holder(pb)
    h.validate()
    h.expand_macroses()
    h.validate()

    with open(t('fixtures/swat3639/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (h.module or h.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/swat3639/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_pwr2():
    with open(t('fixtures/old_style/pwr2/balancer.yml')) as f:
        yml = f.read()
    pb = parse2(yml)
    h = Holder(pb)
    h.validate()
    h.expand_macroses()
    h.validate()

    with open(t('fixtures/old_style/pwr2/config.lua')) as f:
        expected_lua = f.read()

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        config = (h.module or h.chain).to_config()
        lua = config.to_top_level_lua()
        if lua != expected_lua:
            with open(t('fixtures/old_style/pwr2/config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_swat4278(create_default_namespace):
    fixture_dir_path = os.path.join(t('fixtures'), 'swat4278')
    namespace_id = 'n-id'
    ns_pb = create_default_namespace(namespace_id)

    balancer_1_pb = None
    balancer_2_pb = None
    upstream_pbs = {}
    upstream_holders = {}

    labels_path = os.path.join(fixture_dir_path, 'LABELS')
    with open(labels_path) as f:
        labels = ujson.load(f)

    for yml_path in glob.glob(os.path.join(fixture_dir_path, '*.yml')):
        with open(yml_path) as f:
            if yml_path.endswith('balancer-1.yml'):
                balancer_1_pb = pb = model_pb2.Balancer()
                pb.meta.id = 'balancer-1'
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.yandex_balancer.yaml = f.read()
                balancer = Holder(parse2(pb.spec.yandex_balancer.yaml))
                balancer.validate()
            elif yml_path.endswith('balancer-2.yml'):
                balancer_2_pb = pb = model_pb2.Balancer()
                pb.meta.id = 'balancer-2'
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.yandex_balancer.yaml = f.read()
                balancer = Holder(parse2(pb.spec.yandex_balancer.yaml))
                balancer.validate()
            else:
                upstream_id = yml_path[:-len('.yml')].rsplit('/', 1)[1]
                pb = model_pb2.Upstream()
                pb.meta.id = upstream_id
                pb.meta.namespace_id = namespace_id
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.yandex_balancer.yaml = f.read()

                section_pb = parse2(pb.spec.yandex_balancer.yaml)
                section = Holder(section_pb)
                section.validate(preceding_modules=AM)

                pb.spec.yandex_balancer.config.CopyFrom(section_pb)
                pb.spec.labels.update(labels[upstream_id])

                upstream_pbs[(namespace_id, upstream_id)] = pb
                upstream_holders[(namespace_id, upstream_id)] = section

    assert balancer_1_pb
    assert balancer_2_pb
    balancer_1_id = balancer_1_pb.meta.id
    balancer_2_id = balancer_2_pb.meta.id
    balancer_1_version = BalancerVersion(0, (namespace_id, balancer_1_id), '')
    balancer_2_version = BalancerVersion(0, (namespace_id, balancer_2_id), '')

    upstream_spec_pbs = {upstream_id_to_upstream_version(upstream_id): upstream_pb.spec
                         for upstream_id, upstream_pb in six.iteritems(upstream_pbs)}

    with pytest.raises(ConfigValidationError) as e:
        generator.validate_config(ns_pb, namespace_id, balancer_2_version, balancer_2_pb.spec, upstream_spec_pbs,
                                  {}, {}, {}, {})
    e.match('can not sort upstreams: upstream "muse" does not have required label "xxx"')

    balancer = generator.validate_config(
        ns_pb, namespace_id, balancer_1_version, balancer_1_pb.spec, upstream_spec_pbs, {}, {}, {}, {}).balancer

    with open(os.path.join(fixture_dir_path, 'config.lua')) as f:
        expected_lua = f.read()

    n = 3
    for i in range(n):
        # make sure we _always_ get the same resulting Lua
        lua = render(namespace_id, balancer_1_id, balancer)
        if lua != expected_lua:
            with open(os.path.join(fixture_dir_path, 'config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


def test_swat6288(create_default_namespace):
    fixture_dir_path = t('fixtures/swat6288')
    namespace_id = 'namespace-id'
    ns_pb = create_default_namespace(namespace_id)
    balancer_id = 'balancer'

    upstream_pbs = {}
    backend_spec_pbs = {}
    endpoint_set_spec_pbs = {}

    labels_path = os.path.join(fixture_dir_path, 'LABELS')
    with open(labels_path) as f:
        labels = ujson.load(f)

    yml_paths = glob.glob(os.path.join(fixture_dir_path, '*.yml'))
    for yml_path in yml_paths:
        with open(yml_path) as f:
            name = yml_path[:-len('.yml')].rsplit('/', 1)[1]
            if name == 'balancer':
                balancer_pb = pb = model_pb2.Balancer()
                pb.meta.id = balancer_id
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.yandex_balancer.yaml = f.read()
            elif name.startswith('upstream'):
                upstream_id = name.split('---')[1]
                pb = model_pb2.Upstream()
                pb.meta.id = upstream_id
                pb.meta.namespace_id = namespace_id
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.yandex_balancer.yaml = f.read()

                section_pb = parse2(pb.spec.yandex_balancer.yaml)
                pb.spec.yandex_balancer.config.CopyFrom(section_pb)
                pb.spec.labels.update(labels[upstream_id])

                upstream_pbs[(namespace_id, upstream_id)] = pb
            elif name.startswith('backend'):
                backend_id = name.split('---')[1]
                if '---' in backend_id:
                    backend_namespace_id, backend_id = backend_id.split('---')
                else:
                    backend_namespace_id = namespace_id
                pb = model_pb2.BackendSpec()
                json_format.Parse(f.read(), pb)

                backend_version = BackendVersion(0, (backend_namespace_id, backend_id), 'xxx', False)
                backend_spec_pbs[backend_version] = pb
            elif name.startswith('endpoint-set'):
                backend_id = name.split('---')[1]
                if '---' in backend_id:
                    backend_namespace_id, backend_id = backend_id.split('---')
                else:
                    backend_namespace_id = namespace_id
                pb = model_pb2.EndpointSetSpec()
                json_format.Parse(f.read(), pb)

                version = EndpointSetVersion(0, (backend_namespace_id, backend_id), 'xxx', False)
                endpoint_set_spec_pbs[version] = pb

                backend_version = BackendVersion(0, (backend_namespace_id, backend_id), 'xxx', False)
                backend_spec_pbs[backend_version] = model_pb2.BackendSpec()

    assert balancer_pb
    balancer_id = balancer_pb.meta.id
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    with open(os.path.join(fixture_dir_path, 'config.lua')) as f:
        expected_lua = f.read()

    upstream_spec_pbs = {upstream_id_to_upstream_version(upstream_id): upstream_pb.spec
                         for upstream_id, upstream_pb in six.iteritems(upstream_pbs)}

    balancer = generator.validate_config(
        namespace_pb=ns_pb,
        namespace_id=namespace_id,
        balancer_version=balancer_version,
        balancer_spec_pb=balancer_pb.spec,
        upstream_spec_pbs=upstream_spec_pbs,
        backend_spec_pbs=backend_spec_pbs,
        endpoint_set_spec_pbs=endpoint_set_spec_pbs,
    ).balancer

    for i in range(5):
        # make sure we _always_ get the same resulting Lua
        lua = render(namespace_id, balancer_id, balancer)
        if lua != expected_lua:
            with open(os.path.join(fixture_dir_path, 'config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua


@pytest.mark.slow
@pytest.mark.parametrize('fixture_dirname', [
    'man.news.yandex.ru',
    'man.noapache.yandex.net',
    'arcanum.yandex-team.ru_man',
    'collections.test.yandex.ru',
    'man.mobilereport.yandex.ru',
    'knoss_images_sas',
    'knoss_images_man',
    'knoss_video_man',
    'sas.yt.yandex.net',
    'sas.answer.yandex.net',
    'man.bar-navig.yandex.ru',
    'yaldi.alice.yandex.net_vla',
    'sas.nanny-lb.yandex-team.ru',
    'vla.mobilereport.yandex.ru',
    'sas.juggler-api.search.yandex.net',
    'sas.mapsuggest.yandex.net',
    'man.sba.search.yandex.net',
    'yasmbiller.yandex-team.ru_man',
    'adm-nanny.yandex-team.ru_man',
    'uniproxy.alice.yandex.net-yp-man',
    'vla.saas-indexerproxy-maps-kv.yandex.net',
    'pumpkin.yandex.ru_vla',
    'man.catalogapi.site.yandex.net',
    'district-public.yandex.net_sas',
    'pumpkin.yandex.ru_man',
    'sas.api.captcha.yandex.net',
    'http-adapter-imgs.yandex.ru_man',
    'core-jams-matcher.maps.yandex.net_man',
    'yp.man.geoexport.yandex.ru',
    'fintech-admin.yandex-team.ru_sas',
    'betastatic.yastatic.net_iva',
    'zen-rc.yandex.ru_sas',
    'district-public.yandex.net_man',
    'l7_macro_antirobot',
    'l7_macro_antirobot_0.0.3',
    'l7_macro_0.3.14_antirobot_hasher_subnets',
    'l7_macro_0.3.15_antirobot_hasher_use_xffy',
    'l7_macro_0.3.17_antirobot_new_logic_for_iva_myt',
    'l7_macro_0.3.18_antirobot_captcha_reply_new_logic_for_iva_myt',
    'l7_macro_webauth',
    'l7_macro_rps_limiter_local',
    'l7_macro_rps_limiter_external',
    'l7_upstream_macro_rps_limiter_local',
    'l7_upstream_macro_rps_limiter_external',
    'l7_uem_0_1_1',
    'bs.yandex.ru_sas',
    'l7_upstream_macro_compression',
    'l7_upstream_macro_traffic_split',
])
@pytest.mark.parametrize('knobs_mode', [
    # model_pb2.BalancerValidatorSettings.KNOBS_ALLOWED,
    model_pb2.BalancerValidatorSettings.KNOBS_DISABLED,
])
def test_new_style_fixtures(fixture_dirname, knobs_mode, binder, create_default_namespace, ctx):
    new_style_fixtures_dir = t('fixtures/new_style')
    fixture_dir_path = os.path.join(new_style_fixtures_dir, fixture_dirname)
    sd_full_balancer_ids = {
        'adm-nanny.yandex-team.ru_man': ('adm-nanny.yandex-team.ru', 'adm-nanny.yandex-team.ru_man'),
        'district-public.yandex.net_man': ('district-public.yandex.net', 'district-public.yandex.net_man'),
        'district-public.yandex.net_sas': ('district-public.yandex.net', 'district-public.yandex.net_sas'),
        'betastatic.yastatic.net_iva': ('betastatic.yastatic.net', 'betastatic.yastatic.net_iva'),
        'zen-rc.yandex.ru_sas': ('zen-rc.yandex.ru', 'zen-rc.yandex.ru_sas'),
        'yasmbiller.yandex-team.ru_man': ('yasmbiller.yandex-team.ru', 'yasmbiller.yandex-team.ru_man'),
        'sas.nanny-lb.yandex-team.ru': ('nanny-lb.yandex-team.ru', 'sas.nanny-lb.yandex-team.ru'),
        'export.yandex.ru_sas': ('export.yandex.ru', 'export.yandex.ru_sas'),
        'export.yandex.ru_man': ('export.yandex.ru', 'export.yandex.ru_man'),
        'pumpkin.yandex.ru_vla': ('pumpkin.yandex.ru', 'pumpkin.yandex.ru_vla'),
        'man.catalogapi.site.yandex.net': ('catalogapi.site.yandex.net', 'man.catalogapi.site.yandex.net'),
        'sas.api.captcha.yandex.net': ('api.captcha.yandex.net', 'sas.api.captcha.yandex.net'),
        'http-adapter-imgs.yandex.ru_man': ('http-adapter-imgs.yandex.ru', 'http-adapter-imgs.yandex.ru_man'),
        'core-jams-matcher.maps.yandex.net_man': (
            'core-jams-matcher.maps.yandex.net', 'core-jams-matcher.maps.yandex.net_man'),
        'yp.man.geoexport.yandex.ru': ('geoexport.yandex.ru', 'yp.man.geoexport.yandex.ru'),
        'fintech-admin.yandex-team.ru_sas': ('fintech-admin.yandex-team.ru', 'fintech-admin.yandex-team.ru_sas'),
        'bs.yandex.ru_sas': ('bs.yandex.ru', 'bs.yandex.ru_sas'),
        'uniproxy.alice.yandex.net-yp-man': ('uniproxy.alice.yandex.net', 'uniproxy.alice.yandex.net-yp-man'),
    }
    if fixture_dirname in ('yasmbiller.yandex-team.ru_man',
                           'adm-nanny.yandex-team.ru_man'):
        knobs_mode = model_pb2.BalancerValidatorSettings.KNOBS_ENABLED
    if fixture_dirname in sd_full_balancer_ids:
        namespace_id, balancer_id = sd_full_balancer_ids[fixture_dirname]
    else:
        namespace_id = 'namespace-id'
        balancer_id = 'balancer'
    ns_pb = create_default_namespace(namespace_id)

    upstream_pbs = {}
    domain_pbs = {}
    backend_spec_pbs = {}
    endpoint_set_spec_pbs = {}
    cert_spec_pbs = {}
    knob_spec_pbs = {}

    labels_path = os.path.join(fixture_dir_path, 'LABELS')
    with open(labels_path) as f:
        labels = ujson.load(f)

    cache_data = {}
    cache_path = os.path.join(fixture_dir_path, 'CACHE')
    if os.path.exists(cache_path):
        with open(cache_path) as f:
            cache_data = pickle.load(f)

    cache = {}
    for key, data in six.iteritems(cache_data):
        rv = internals_pb2.InstancesList()
        rv.ParseFromString(data)
        cache[key] = rv.instances

    yml_paths = glob.glob(os.path.join(fixture_dir_path, '*.yml'))
    for yml_path in yml_paths:
        with open(yml_path) as f:
            name = yml_path[:-len('.yml')].rsplit('/', 1)[1]
            if name == 'balancer':
                balancer_pb = pb = model_pb2.Balancer()
                pb.meta.id = balancer_id
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.validator_settings.knobs_mode = knobs_mode
                pb.spec.yandex_balancer.yaml = f.read()
            elif name.startswith('domain'):
                domain_id = name.split('---')[1]
                from google.protobuf import text_format
                domain_pb = model_pb2.Domain()
                text_format.Parse(f.read(), domain_pb)
                domain_pbs[(namespace_id, domain_id)] = domain_pb
            elif name.startswith('upstream'):
                upstream_id = name.split('---')[1]
                pb = model_pb2.Upstream()
                pb.meta.id = upstream_id
                pb.meta.namespace_id = namespace_id
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.yandex_balancer.yaml = f.read()

                section_pb = parse2(pb.spec.yandex_balancer.yaml)
                pb.spec.yandex_balancer.config.CopyFrom(section_pb)
                pb.spec.labels.update(labels[upstream_id])

                upstream_pbs[(namespace_id, upstream_id)] = pb
            elif name.startswith('backend'):
                pb = model_pb2.BackendSpec()
                json_format.Parse(f.read(), pb)
                backend_id = name.split('---', 1)[1]
                if '---' in backend_id:
                    backend_namespace_id, backend_id = backend_id.split('---')
                    pb.is_global.value = True
                else:
                    backend_namespace_id = namespace_id

                backend_version = BackendVersion(0, (backend_namespace_id, backend_id), 'xxx', False)
                backend_spec_pbs[backend_version] = pb
            elif name.startswith('endpoint-set'):
                pb = model_pb2.EndpointSetSpec()
                json_format.Parse(f.read(), pb)
                backend_id = name.split('---', 1)[1]
                if '---' in backend_id:
                    backend_namespace_id, backend_id = backend_id.split('---')
                    pb.is_global.value = True
                else:
                    backend_namespace_id = namespace_id

                version = EndpointSetVersion(0, (backend_namespace_id, backend_id), 'xxx', False)
                endpoint_set_spec_pbs[version] = pb

                backend_version = BackendVersion(0, (backend_namespace_id, backend_id), 'xxx', False)
                spec_pb = model_pb2.BackendSpec()
                spec_pb.is_global.value = pb.is_global.value
                backend_spec_pbs[backend_version] = spec_pb
            elif name.startswith('cert'):
                cert_id = name.split('---')[1]
                if '---' in cert_id:
                    cert_namespace_id, cert_id = cert_id.split('---')
                else:
                    cert_namespace_id = namespace_id
                pb = model_pb2.CertificateSpec()
                json_format.Parse(f.read(), pb)

                cert_version = CertVersion(0, (cert_namespace_id, cert_id), 'xxx', False, False)
                cert_spec_pbs[cert_version] = pb
            elif name.startswith('knob'):
                knob_id = name.split('---')[1]
                if '---' in knob_id:
                    knob_namespace_id, knob_id = knob_id.split('---')
                else:
                    knob_namespace_id = namespace_id
                pb = model_pb2.KnobSpec()
                json_format.Parse(f.read(), pb)

                knob_version = KnobVersion(0, (knob_namespace_id, knob_id), 'xxx', False)
                knob_spec_pbs[knob_version] = pb
    assert balancer_pb
    balancer_id = balancer_pb.meta.id
    balancer_version = BalancerVersion(0, (namespace_id, balancer_id), '')

    with open(os.path.join(fixture_dir_path, 'config.lua')) as f:
        expected_lua = f.read()

    upstream_spec_pbs = {upstream_id_to_upstream_version(upstream_id): upstream_pb.spec
                         for upstream_id, upstream_pb in six.iteritems(upstream_pbs)}
    domain_spec_pbs = {domain_id_to_domain_version(domain_id): domain_pb.spec
                       for domain_id, domain_pb in six.iteritems(domain_pbs)}

    def list_nanny_snapshot_instances(service_id, snapshot_id, use_mtn):
        return cache[(service_id, snapshot_id, use_mtn)]

    def configure(b):
        nanny_client = mock.Mock()
        nanny_client.list_nanny_snapshot_instances = list_nanny_snapshot_instances
        b.bind(INannyClient, nanny_client)
        binder(b)

    inject.clear_and_configure(configure)

    hostnames = {
        'laas.yandex.ru': '2a02:6b8:0:3400::1022',
        'news.sinkadm.priemka.yandex.ru': '2a02:6b8:0:3400::eeee:2',
        'cryprox.yandex.net': '2a02:6b8::402',
        'uaas.search.yandex.net': '2a02:6b8:0:3400::2:48',
        'sinkadm.priemka.yandex.ru': '2a02:6b8:0:3400::eeee:20',
        'internal.collections.yandex.ru': '2a02:6b8:0:3400::103c',
        'yastatic.net': '2a02:6b8:20::215',
        'bolver.yandex-team.ru': '2a02:6b8:0:3400::32',
        'atom-web-0.haze.yandex.net': '2a02:6b8:0:1a79::7df',
        'misc.mobile.yandex.net': '2a02:6b8::1:58',
        'search-history.yandex.net': '2a02:6b8:0:3400::3:36',
        'blackbox.yandex.net': '2a02:6b8:0:3400::1:35',
        'router-dev.qloud.yandex.net': '2a02:6b8:0:3400::4:51',
        'api.sport.yandex.ru': '2a02:6b8::e',
        'cryprox-test.yandex.net': '2a02:6b8::197',
        'yastatic.s3.yandex.net': '2a02:6b8::2:158',
        's3.mdst.yandex.net': '2a02:6b8:0:3400::40f',
        'news.s3.mds.yandex.net': '2a02:6b8:0:3400::3:147',
        'localhost': '::1',
        'ext-router.qloud.yandex.net': '2a02:6b8:0:3400::4:67',
        'mc-internal.metrika.yandex.net': '2a02:6b8:0:3400::735',
        'juggler-testing-api.search.yandex.net': '2a02:6b8:0:3400:0:1b7:0:2',
        'saas-searchproxy-maps-prestable.yandex.net': '2a02:6b8:0:3400::1097',
        'cdn-router.stable.qloud-b.yandex.net': '2a02:6b8:0:3400:0:2e5:1:803d',
        'pcode-static.yabs.yandex.net': '2a02:6b8:0:3400:0:71d:0:3da',
        'lermontov.vla.yp-c.yandex.net': '2a02:6b8:c1d:299c:0:696:1a71:0',
    }

    b = time.time()
    ctx.log.info('validating {}\'s config'.format(fixture_dirname))
    with mocked_resolve_host(hostnames):
        result = generator.validate_config(
            namespace_pb=ns_pb,
            namespace_id=namespace_id,
            balancer_version=balancer_version,
            balancer_spec_pb=balancer_pb.spec,
            upstream_spec_pbs=upstream_spec_pbs,
            domain_spec_pbs=domain_spec_pbs,
            backend_spec_pbs=backend_spec_pbs,
            endpoint_set_spec_pbs=endpoint_set_spec_pbs,
            cert_spec_pbs=cert_spec_pbs,
            knob_spec_pbs=knob_spec_pbs
        )
    balancer = result.balancer
    a = time.time()
    ctx.log.info('{}: validation time = {}, validation ticks = {}'.format(fixture_dirname, (a - b),
                                                                          result.validation_ctx._tick_counter))

    n = 1
    b = time.time()
    for i in range(n):
        # make sure we _always_ get the same resulting Lua
        lua = render(namespace_id, balancer_id, balancer)
        if lua != expected_lua:
            with open(os.path.join(fixture_dir_path, 'config.lua.new'), 'w') as f:
                f.write(lua)
        assert lua == expected_lua
    a = time.time()
    ctx.log.info('{}: config generation time = {}\n'.format(fixture_dirname, (a - b) / n))


def test_run_heavy_operation(caplog, log):
    registry = metrics.ROOT_REGISTRY.path('awacs', 'test')
    test_timer = registry.get_histogram('test_timer')
    pool = gevent.threadpool.ThreadPool(1)

    def success():
        pass

    def success_with_sleep():
        time.sleep(0.1)

    def failure():
        raise RuntimeError('run: failure')

    def failure_with_sleep():
        time.sleep(0.1)
        raise RuntimeError('run: failure')

    with check_log(caplog) as log:
        run_heavy_operation(log=logging,
                            threadpool=None,
                            func=success,
                            func_name='success()',
                            timer=test_timer,
                            interval=0.1)
        assert 'success() started, threadpool: False, 0' in log.records_text()
        assert 'success() is not ready' not in log.records_text()
        assert 'success() finished' in log.records_text()

    with check_log(caplog) as log:
        run_heavy_operation(log=logging,
                            threadpool=pool,
                            func=success_with_sleep,
                            func_name='success_with_sleep()',
                            timer=test_timer,
                            interval=0.1)
        assert 'success_with_sleep() started, threadpool: True, 0' in log.records_text()
        assert 'success_with_sleep() is not ready' in log.records_text()
        assert 'success_with_sleep() finished' in log.records_text()

    with pytest.raises(RuntimeError, match='run: failure'):
        with check_log(caplog) as log:
            run_heavy_operation(log=logging,
                                threadpool=None,
                                func=failure,
                                func_name='failure()',
                                timer=test_timer,
                                interval=0.1)
            assert 'failure() started, threadpool: False, 0' in log.records_text()
            assert 'failure() is not ready' not in log.records_text()
            assert 'failure() finished' not in log.records_text()

    with pytest.raises(RuntimeError, match='run: failure'):
        with check_log(caplog) as log:
            run_heavy_operation(log=logging,
                                threadpool=pool,
                                func=failure_with_sleep,
                                func_name='failure_with_sleep()',
                                timer=test_timer,
                                interval=0.1)
            assert 'failure_with_sleep() started, threadpool: True, 0' in log.records_text()
            assert 'failure_with_sleep() is not ready' in log.records_text()
            assert 'failure_with_sleep() finished' not in log.records_text()

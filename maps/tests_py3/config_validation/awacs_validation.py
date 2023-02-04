import json

import pytest
import yatest.common
from awacs.wrappers.base import ValidationCtx
from awacs.model import validation
from awacs.model.balancer.generator import validate_config
from awacs.model.balancer.vector import (
    BalancerVersion, UpstreamVersion, BackendVersion, EndpointSetVersion, CertVersion)
from infra.awacs.proto import model_pb2
from google.protobuf import text_format
from pathlib import Path
from sepelib.core import config

config.load()
root_path = Path(yatest.common.source_path('maps/config/balancer'))


def fqdns_list():
    return [dir.name for dir in root_path.iterdir() if not dir.is_file()]


@pytest.mark.parametrize('fqdn', fqdns_list())
def test_awacs_namespace(fqdn):
    data = NamespaceData(fqdn=fqdn)
    balancer = data.read_top_leve_pb()

    upstreams = data.read_upstreams_pb()
    upstream_spec_pbs = {data.upstream_version(spec=spec, id=namespace_id): spec
                         for namespace_id, spec in upstreams.items()}

    backends = data.mock_antirobot_backends_pb()
    backend_spec_pbs = {data.backend_version(spec=spec, id=backend_id, fqdn='common-antirobot'): spec
                        for backend_id, spec in backends.items()}
    backends = data.read_backends_pb()
    backend_spec_pbs.update({data.backend_version(spec=spec, id=backend_id): spec
                             for backend_id, spec in backends.items()})

    endpoint_sets = data.dummy_endpoint_sets()
    endpoint_set_spec_pbs = {data.dummy_endpointsets_version(id=endpoint_set_id): spec
                             for endpoint_set_id, spec in endpoint_sets.items()}

    endpoint_sets = data.dummy_robots_endpoint_sets()
    endpoint_set_spec_pbs.update({data.dummy_endpointsets_version(id=endpoint_set_id, fqdn='common-antirobot'): spec
                                  for endpoint_set_id, spec in endpoint_sets.items()})

    certs_names = data.retrieve_certs_names(balancer)
    cert_spec_pbs = {data.dummy_cert_version(id=cert_id): data.dummy_cert_spec() for cert_id in certs_names}

    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = fqdn
    validate_config(
        namespace_pb=ns_pb,
        namespace_id=fqdn, balancer_version=data.balancer_version(spec=balancer), balancer_spec_pb=balancer,
        upstream_spec_pbs=upstream_spec_pbs,
        backend_spec_pbs=backend_spec_pbs,
        endpoint_set_spec_pbs=endpoint_set_spec_pbs,
        cert_spec_pbs=cert_spec_pbs
    )


class NamespaceData:
    def __init__(self, fqdn):
        self.fqdn = fqdn
        self.sequential_port_number = 1

    def read_top_leve_pb(self):
        balancer_pb = model_pb2.BalancerSpec()
        balancer_pb.yandex_balancer.mode = model_pb2.YandexBalancerSpec.ConfigMode.EASY_MODE
        top_level_filename = Path(root_path, self.fqdn, 'top-level')
        data = top_level_filename.read_text()
        if not data:
            raise Exception(top_level_filename)
        balancer_pb.yandex_balancer.yaml = data
        validation.validate_and_parse_yaml_balancer_config(spec_pb=balancer_pb,
                                                           full_balancer_id=(self.fqdn, self.fqdn),
                                                           namespace_pb=model_pb2.Namespace())
        return balancer_pb

    def read_upstreams_pb(self):
        upstreams = {}
        orders = self.read_orders()
        for upstream_file in Path(root_path, self.fqdn).iterdir():
            if upstream_file.name.endswith('.upstream'):
                upstream_id = upstream_file.stem
                upstream_path = Path(root_path, self.fqdn, '{upstream_id}.upstream'.format(upstream_id=upstream_id))

                upstream = model_pb2.UpstreamSpec()
                upstreams[upstream_id] = upstream
                upstream.yandex_balancer.yaml = upstream_path.read_text()
                upstream.labels.update({'order': str(orders.get(upstream_id))})
                ctx = ValidationCtx(config_type=ValidationCtx.CONFIG_TYPE_UPSTREAM, namespace_id=self.fqdn)
                validation.validate_and_parse_yaml_upstream_config(
                    namespace_id=self.fqdn,
                    upstream_id=upstream_id,
                    spec_pb=upstream,
                    ctx=ctx)
        return upstreams

    def read_orders(self):
        order_file = Path(root_path, self.fqdn, 'upstreams.order')
        return json.loads(order_file.read_text())

    def read_backends_pb(self):
        backends = {}
        for backend_file in Path(root_path, self.fqdn, 'backends').iterdir():
            if backend_file.name.endswith('.backend'):
                backend_id = backend_file.stem
                upstream_path = Path(root_path, self.fqdn, 'backends', '{backend_id}.backend'.format(backend_id=backend_id))
                spec = model_pb2.BackendSpec()
                text_format.Parse(upstream_path.read_text(), spec.selector)
                backends[backend_id] = spec
        return backends

    def balancer_version(self, spec):
        balancer = model_pb2.Balancer()
        balancer.spec.CopyFrom(spec)
        balancer.meta.namespace_id = self.fqdn
        return BalancerVersion.from_pb(balancer)

    def upstream_version(self, spec, id):
        upstream = model_pb2.Upstream()
        upstream.spec.CopyFrom(spec)
        upstream.meta.id = id
        upstream.meta.namespace_id = self.fqdn
        return UpstreamVersion.from_pb(upstream)

    def backend_version(self, spec, id, fqdn=None):
        backend = model_pb2.Backend()
        backend.spec.CopyFrom(spec)
        backend.meta.id = id
        if not fqdn:
            fqdn = self.fqdn
        backend.meta.namespace_id = fqdn
        return BackendVersion.from_pb(backend)

    def mock_antirobot_backends_pb(self):
        '''
            shared backends: https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/common-antirobot/backends/list/
        '''
        backends = {}
        for dc in ['man', 'vla', 'sas']:
            backend_id = 'antirobot_{}_yp'.format(dc)
            spec = model_pb2.BackendSpec()
            spec.is_global.value = True
            backends[backend_id] = spec
        return backends

    def dummy_endpoint_spec(self):
        dummy_endpoint_spec = model_pb2.EndpointSetSpec()
        instance = model_pb2.EndpointSetSpec.Instance()
        instance.port = self.sequential_port_number
        self.sequential_port_number += 1
        # random ip, must be in dns record to execute bash cmd: 'host ip'
        instance.ipv6_addr = '2a02:6b8:0:3400:0:71d:0:88'
        dummy_endpoint_spec.instances.extend([instance])
        return dummy_endpoint_spec

    def dummy_endpoint_sets(self):
        backends = self.read_backends_pb()
        endpoint_sets = {}
        for id, backend in backends.items():
            for es in backend.selector.yp_endpoint_sets:
                endpoint_sets[es.endpoint_set_id] = self.dummy_endpoint_spec()
        return endpoint_sets

    def dummy_endpointsets_version(self, id, fqdn=None):
        es = model_pb2.EndpointSet()
        if not fqdn:
            fqdn = self.fqdn
        es.meta.namespace_id = fqdn
        es.meta.id = id.replace('-', '_')
        es.spec.CopyFrom(self.dummy_endpoint_spec())
        return EndpointSetVersion.from_pb(es)

    def dummy_robots_endpoint_sets(self):
        backends = self.mock_antirobot_backends_pb()
        endpoint_sets = {}
        for id, backend in backends.items():
            for dc in ['vla', 'man', 'sas']:
                spec = self.dummy_endpoint_spec()
                spec.is_global.value = True
                endpoint_sets['antirobot_{}_yp'.format(dc)] = spec
        return endpoint_sets

    def dummy_cert_spec(self):
        return model_pb2.CertificateSpec()

    def dummy_cert_version(self, id):
        cert = model_pb2.Certificate()
        cert.meta.id = id
        cert.meta.namespace_id = self.fqdn
        return CertVersion.from_pb(cert)

    def retrieve_certs_names(self, balancer):
        return [cert.id for cert in balancer.yandex_balancer.config.l7_macro.https.certs]

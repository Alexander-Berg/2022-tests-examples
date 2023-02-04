from infra.awacs.proto import model_pb2
from awacs.model.l3_balancer import vector as l3_vector
from awacs.model.balancer import vector as l7_vector


def test_l3_vector():
    empty_vector = l3_vector.L3Vector(l3_balancer_version=None,
                                      backend_versions={},
                                      endpoint_set_versions={})
    assert empty_vector.get_weak_hash() == b'00000000'

    l3_balancer_version = l3_vector.L3BalancerVersion.from_rev_status_pb(
        l3_balancer_id=u'balancer-1', pb=model_pb2.L3BalancerState.RevisionL3Status(revision_id=u'1'))
    vector = l3_vector.L3Vector(l3_balancer_version=l3_balancer_version,
                                backend_versions={},
                                endpoint_set_versions={})
    assert vector.get_weak_hash() == b'09b7e3a9'

    backend_version_1 = l3_vector.BackendVersion.from_rev_status_pb(
        backend_id=u'backend-1', pb=model_pb2.L3BalancerState.RevisionL3Status(revision_id=u'1'))
    backend_version_2 = l3_vector.BackendVersion.from_rev_status_pb(
        backend_id=u'backend-2', pb=model_pb2.L3BalancerState.RevisionL3Status(revision_id=u'2'))

    endpoint_set_version_1 = l3_vector.EndpointSetVersion.from_rev_status_pb(
        endpoint_set_id=u'endpoint-set-1', pb=model_pb2.L3BalancerState.RevisionL3Status(revision_id=u'1'))
    endpoint_set_version_2 = l3_vector.EndpointSetVersion.from_rev_status_pb(
        endpoint_set_id=u'endpoint-set-2', pb=model_pb2.L3BalancerState.RevisionL3Status(revision_id=u'2'))

    vector = l3_vector.L3Vector(l3_balancer_version=l3_balancer_version,
                                backend_versions={
                                    u'backend-1': backend_version_1,
                                    u'backend-2': backend_version_2,
                                },
                                endpoint_set_versions={
                                    u'endpoint-set-1': endpoint_set_version_1,
                                    u'endpoint-set-2': endpoint_set_version_2,
                                })
    assert vector.get_weak_hash() == b'bff765d2'


def test_l7_vector():
    ns_id = u'ns-1'

    empty_vector = l7_vector.Vector(balancer_version=None,
                                    upstream_versions={},
                                    backend_versions={},
                                    endpoint_set_versions={},
                                    knob_versions={},
                                    cert_versions={},
                                    domain_versions={},
                                    weight_section_versions={},
                                    )
    assert empty_vector.get_weak_hash() == b'00000000'

    balancer_version = l7_vector.BalancerVersion.from_rev_status_pb(
        full_balancer_id=(ns_id, u'balancer-1'),
        pb=model_pb2.BalancerState.RevisionStatus(revision_id=u'1'))
    vector = l7_vector.Vector(balancer_version=balancer_version,
                              upstream_versions={},
                              backend_versions={},
                              endpoint_set_versions={},
                              knob_versions={},
                              cert_versions={},
                              domain_versions={},
                              weight_section_versions={},
                              )
    assert vector.get_weak_hash() == b'00d61e51'

    upstream_version_1 = l7_vector.UpstreamVersion.from_rev_status_pb(
        full_upstream_id=(ns_id, u'upstream-1'),
        pb=model_pb2.BalancerState.RevisionStatus(revision_id=u'1'))
    backend_version_1 = l7_vector.BackendVersion.from_rev_status_pb(
        full_backend_id=(ns_id, u'backend-1'),
        pb=model_pb2.BalancerState.RevisionStatus(revision_id=u'1'))
    endpoint_set_version_1 = l7_vector.EndpointSetVersion.from_rev_status_pb(
        full_endpoint_set_id=(ns_id, u'endpoint-set-1'),
        pb=model_pb2.BalancerState.RevisionStatus(revision_id=u'1'))
    knob_version_1 = l7_vector.KnobVersion.from_rev_status_pb(
        full_knob_id=(ns_id, u'knob-1'),
        pb=model_pb2.BalancerState.RevisionStatus(revision_id=u'1'))
    cert_version_1 = l7_vector.CertVersion.from_rev_status_pb(
        full_cert_id=(ns_id, u'cert-1'),
        pb=model_pb2.BalancerState.RevisionStatus(revision_id=u'1'))
    domain_version_1 = l7_vector.DomainVersion.from_rev_status_pb(
        full_domain_id=(ns_id, u'domain-1'),
        pb=model_pb2.BalancerState.RevisionStatus(revision_id=u'1'))

    vector = l7_vector.Vector(balancer_version=balancer_version,
                              upstream_versions={
                                  upstream_version_1.upstream_id: upstream_version_1,
                              },
                              backend_versions={
                                  backend_version_1.backend_id: backend_version_1,
                              },
                              endpoint_set_versions={
                                  endpoint_set_version_1.endpoint_set_id: endpoint_set_version_1,
                              },
                              knob_versions={
                                  knob_version_1.knob_id: knob_version_1,
                              },
                              cert_versions={
                                  cert_version_1.cert_id: cert_version_1,
                              },
                              domain_versions={
                                  domain_version_1.domain_id: domain_version_1,
                              },
                              weight_section_versions={},
                              )
    assert vector.get_weak_hash() == b'e7badf7b'

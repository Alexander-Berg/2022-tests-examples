from awacs.model import storage, storage_modern, objects


def test_structure():
    modern_awacsworkerd_structure = storage_modern.construct_awacsworkerd_zk_structure()
    modern_awacsworkerd_structure._nested.pop(objects.NamespaceOperation.desc.zk_prefix)
    modern_awacsworkerd_structure._nested.pop(objects.WeightSection.desc.zk_prefix)
    modern_awacsworkerd_structure._nested.pop(objects.L7HeavyConfig.desc.zk_prefix)
    modern_awacsworkerd_structure._nested.pop(objects.L7HeavyConfig.state.desc.zk_prefix)
    assert storage.construct_awacsworkerd_zk_structure() == modern_awacsworkerd_structure

    modern_awacsstatusd_structure = storage_modern.construct_awacsstatusd_zk_structure()
    modern_awacsstatusd_structure._nested.pop(objects.NamespaceOperation.desc.zk_prefix)
    modern_awacsstatusd_structure._nested.pop(objects.WeightSection.desc.zk_prefix)
    modern_awacsstatusd_structure._nested.pop(objects.L7HeavyConfig.desc.zk_prefix)
    assert storage.construct_awacsstatusd_zk_structure() == modern_awacsstatusd_structure

    modern_awacsresolverd_structure = storage_modern.construct_awacsresolverd_zk_structure()
    modern_awacsresolverd_structure._nested.pop(objects.WeightSection.desc.zk_prefix)
    modern_awacsresolverd_structure._nested.pop(objects.L7HeavyConfig.desc.zk_prefix)
    modern_awacsresolverd_structure._nested.pop(objects.L7HeavyConfig.state.desc.zk_prefix)
    assert storage.construct_awacsresolverd_zk_structure() == modern_awacsresolverd_structure

    modern_full_structure = storage_modern.construct_full_zk_structure()
    modern_full_structure._nested.pop(objects.NamespaceOperation.desc.zk_prefix)
    modern_full_structure._nested.pop(objects.WeightSection.desc.zk_prefix)
    modern_full_structure._nested.pop(objects.L7HeavyConfig.desc.zk_prefix)
    modern_full_structure._nested.pop(objects.L7HeavyConfig.state.desc.zk_prefix)
    assert storage.ZK_STRUCTURE == modern_full_structure


def test_node_names():
    assert storage.BALANCERS_NODE == objects.L7BalancerDescriptor.zk_prefix
    assert storage.BALANCER_STATES_NODE == objects.L7BalancerStateDescriptor.zk_prefix
    assert storage.BALANCER_ASPECTS_SET_NODE == objects.L7BalancerAspectSetDescriptor.zk_prefix
    assert storage.NAMESPACES_NODE == objects.NamespaceDescriptor.zk_prefix
    assert storage.NAMESPACE_ASPECTS_SET_NODE == objects.NamespaceAspectSetDescriptor.zk_prefix
    assert storage.COMPONENTS_NODE == objects.ComponentDescriptor.zk_prefix
    assert storage.KNOBS_NODE == objects.KnobDescriptor.zk_prefix
    assert storage.CERTS_NODE == objects.CertDescriptor.zk_prefix
    assert storage.CERT_RENEWALS_NODE == objects.CertRenewalDescriptor.zk_prefix
    assert storage.L3_BALANCERS_NODE == objects.L3BalancerDescriptor.zk_prefix
    assert storage.L3_BALANCER_STATES_NODE == objects.L3BalancerStateDescriptor.zk_prefix
    assert storage.DNS_RECORD_OPERATIONS_NODE == objects.DnsRecordOperationDescriptor.zk_prefix
    assert storage.DNS_RECORD_STATES_NODE == objects.DnsRecordStateDescriptor.zk_prefix
    assert storage.NAME_SERVERS_NODE == objects.NameServerDescriptor.zk_prefix
    assert storage.BACKENDS_NODE == objects.BackendDescriptor.zk_prefix
    assert storage.ENDPOINT_SETS_NODE == objects.EndpointSetDescriptor.zk_prefix
    assert storage.UPSTREAMS_NODE == objects.UpstreamDescriptor.zk_prefix
    assert storage.DOMAINS_NODE == objects.DomainDescriptor.zk_prefix
    assert storage.DOMAIN_OPERATIONS_NODE == objects.DomainOperationDescriptor.zk_prefix
    assert storage.BALANCER_OPERATIONS_NODE == objects.L7BalancerOperationDescriptor.zk_prefix

    assert storage.EXCLUSIVE_SERVICES_NODE == storage_modern.EXCLUSIVE_SERVICES_NODE_ZK_PREFIX
    assert storage.PARTIES_NODE == storage_modern.PARTIES_NODE_ZK_PREFIX

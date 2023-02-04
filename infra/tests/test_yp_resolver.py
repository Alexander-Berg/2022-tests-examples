# coding: utf-8
import mock
import pytest

from awacs import resolver
from awacs.lib import ypclient, nannyclient, yp_service_discovery
from infra.awacs.proto import internals_pb2
from awacs.resolver.yp.util import (list_allocation_instances, list_endpoint_set_instances,
                                    list_endpoint_set_instances_sd, does_endpoint_set_exist)


@pytest.fixture(scope='module')
def grpc_yp_client_factory():
    yield ypclient.YpObjectServiceClientFactory.from_config({
        'use_grpc': True,
        'clusters': [
            {
                'cluster': 'sas',
                'rpc_url': 'https://sas-yp.n.yandex-team.ru/ObjectService',
            },
            {
                'cluster': 'man',
                'rpc_url': 'https://man-yp.n.yandex-team.ru/ObjectService',
            },
        ],
    })


@pytest.fixture(scope='module')
def yp_client_factory():
    yield ypclient.YpObjectServiceClientFactory.from_config({
        'use_grpc': False,
        'clusters': [
            {
                'cluster': 'sas',
                'rpc_url': 'https://sas-yp.n.yandex-team.ru/ObjectService',
            },
            {
                'cluster': 'man',
                'rpc_url': 'https://man-yp.n.yandex-team.ru/ObjectService',
            },
        ],
    })


@pytest.mark.vcr
def test_list_allocation_instances(yp_client_factory):
    pod_filter = '[/meta/pod_set_id] = "risenberg-yp-test2" AND ' \
                 '[/labels/nanny/nanny_allocation_id] = "677bb3fc-a6e0-4c6c-b6e4-58432a9808dd"'

    stub = yp_client_factory.get('sas')
    instances = list_allocation_instances(stub, pod_filter)

    expected_instances = [
        internals_pb2.Instance(
            host='6kqlftv5l4ff4.sas.yp-c.yandex.net',
            port=80,
            ipv6_addr='2a02:6b8:c08:9208:100:0:ccb0:0',
            weight=1
        ),
        internals_pb2.Instance(
            host='q0f5ugc8n725r.sas.yp-c.yandex.net',
            port=80,
            ipv6_addr='2a02:6b8:c08:8e82:100:0:760d:0',
            weight=1
        ),
        internals_pb2.Instance(
            host='uwtx0yv23995c.sas.yp-c.yandex.net',
            port=80,
            ipv6_addr='2a02:6b8:c08:8b14:100:0:285f:0',
            weight=1
        ),
        internals_pb2.Instance(
            host='v5as3l8yjw8at.sas.yp-c.yandex.net',
            port=80,
            ipv6_addr='2a02:6b8:c08:8415:100:0:d2fe:0',
            weight=1
        ),
    ]

    assert instances == expected_instances


@pytest.mark.vcr
def test_list_allocation_instances_with_pod_ids(yp_client_factory):
    pod_filter = '[/meta/pod_set_id] = "reddi-test-yp-pods"'
    stub = yp_client_factory.get('sas')

    # Case 1: list all pods in service
    instances = list_allocation_instances(stub, pod_filter)
    expected_instances = [
        internals_pb2.Instance(
            host='0114zqjob0qv8.sas.yp-c.yandex.net',
            port=80,
            ipv6_addr='2a02:6b8:c08:8124:100:0:d016:0',
            weight=1
        ),
        internals_pb2.Instance(
            host='eoveclihm3554.sas.yp-c.yandex.net',
            port=80,
            ipv6_addr='2a02:6b8:c08:8d98:100:0:46b0:0',
            weight=1
        ),
    ]
    assert instances == expected_instances

    # Case 2: filter pods with pod_ids
    instances = list_allocation_instances(stub, pod_filter, pod_ids={'0114zqjob0qv8'})
    expected_instances = [
        internals_pb2.Instance(
            host='0114zqjob0qv8.sas.yp-c.yandex.net',
            port=80,
            ipv6_addr='2a02:6b8:c08:8124:100:0:d016:0',
            weight=1
        ),
    ]
    assert instances == expected_instances


@pytest.mark.vcr
def test_yp_resolver(yp_client_factory):
    nanny_client = nannyclient.NannyClient(url='https://nanny.yandex-team.ru/v2')
    c = resolver.NannyClient(
        nanny_client=nanny_client,
        gencfg_client=mock.Mock(),
        yp_client_factory=yp_client_factory
    )

    instances_pbs = c.list_nanny_snapshot_instances(service_id='reddi-test-yp-again',
                                                    snapshot_id='b0781874ed5c9aaf0c50891520ce8b30143a292b',
                                                    use_mtn=False)

    expected_instance_pbs = [
        internals_pb2.Instance(
            host='owhx4qahojqlqgvu.sas.yp-c.yandex.net',
            port=80,
            weight=1.0,
            ipv6_addr="2a02:6b8:c08:9b8c:0:43e6:d806:0",
        ),
        internals_pb2.Instance(
            host='satbmlnflsi223ne.sas.yp-c.yandex.net',
            port=80,
            weight=1.0,
            ipv6_addr="2a02:6b8:c08:7b8f:0:43e6:cfb1:0",
        ),
        internals_pb2.Instance(
            host='djqzb3i2x7mmbldm.man.yp-c.yandex.net',
            port=80,
            weight=1.0,
            ipv6_addr="2a02:6b8:c0b:4081:0:43e6:5fc2:0",
        ),
    ]
    assert instances_pbs == expected_instance_pbs


@pytest.mark.vcr
def test_list_endpoint_set_instances(yp_client_factory):
    stub = yp_client_factory.get('man')

    endpoint_set_id = 'hamster-tunneller-yp-man-web'
    instance_pbs = list_endpoint_set_instances(stub, endpoint_set_id)
    expected_instance_pbs = [
        internals_pb2.Instance(
            host='dymjdlw9w05pn.man.yp-c.yandex.net',
            port=14685,
            weight=1.0,
            ipv6_addr='2a02:6b8:c0b:f0f:10d:298f:8851:0',
        ),
        internals_pb2.Instance(
            host='3m7p38i6r9q58.man.yp-c.yandex.net',
            port=14685,
            weight=1.0,
            ipv6_addr='2a02:6b8:c0b:5a5c:10d:298f:5cc2:0',
        ),
        internals_pb2.Instance(
            host='8xbpbgpbdauib.man.yp-c.yandex.net',
            port=14685,
            weight=1.0,
            ipv6_addr='2a02:6b8:c0b:5a20:10d:298f:fdc0:0'
        )
    ]
    assert instance_pbs == expected_instance_pbs


@pytest.mark.vcr
def test_get_endpoint_set_1(yp_client_factory):
    stub = yp_client_factory.get('man')
    endpoint_set_id = 'hamster-tunneller-yp-man-web'
    assert does_endpoint_set_exist(stub, endpoint_set_id)


@pytest.mark.skip('need correct stub')
@pytest.mark.vcr
def test_get_endpoint_set_1_grpc(grpc_yp_client_factory):
    stub = grpc_yp_client_factory.get('man')
    endpoint_set_id = 'hamster-tunneller-yp-man-web'
    assert does_endpoint_set_exist(stub, endpoint_set_id)


@pytest.mark.vcr
def test_get_endpoint_set_2(yp_client_factory):
    stub = yp_client_factory.get('man')
    endpoint_set_id = 'xxx-123'
    assert not does_endpoint_set_exist(stub, endpoint_set_id)


@pytest.mark.vcr
def test_list_endpoint_set_instances_sd():
    resolver = yp_service_discovery.Resolver('test_list_endpoint_set_instances')
    req_pb = internals_pb2.TReqResolveEndpoints(cluster_name='sas', endpoint_set_id='prod-deploy-ui')
    resp_pb, instance_pbs = list_endpoint_set_instances_sd(resolver, req_pb)
    expected_instance_pbs = [
        internals_pb2.Instance(
            host='prod-deploy-ui-2.sas.yp-c.yandex.net',
            port=80,
            weight=1.0,
            ipv6_addr='2a02:6b8:c1c:282:0:472a:5f79:0',
        ),
        internals_pb2.Instance(
            host='prod-deploy-ui-1.sas.yp-c.yandex.net',
            port=80,
            weight=1.0,
            ipv6_addr='2a02:6b8:c08:40aa:0:472a:3cdb:0',
        )
    ]
    assert instance_pbs == expected_instance_pbs


@pytest.mark.vcr
def test_sd_resolver():
    resolver = yp_service_discovery.Resolver('test_list_endpoint_set_instances')
    req_pb = internals_pb2.TReqResolveEndpoints(cluster_name='sas', endpoint_set_id='prod-deploy-ui')
    expected_resp_pb = internals_pb2.TRspResolveEndpoints(
        timestamp=1692214444228084557,
        resolve_status=internals_pb2.OK,
        watch_token="0",
        host="vla1-0598-vla-yp-service-discovery-28456.gencfg-c.yandex.net",
        ruid="157599752309226410109987921112193351"
    )
    expected_resp_pb.endpoint_set.endpoint_set_id = "prod-deploy-ui"
    expected_resp_pb.endpoint_set.endpoints.add(
        id='in5yah25ziaarbsh',
        protocol='TCP',
        fqdn='prod-deploy-ui-2.sas.yp-c.yandex.net',
        ip6_address='2a02:6b8:c1c:282:0:472a:5f79:0',
        port=80
    )
    expected_resp_pb.endpoint_set.endpoints.add(
        id='v44yumx7cpmzfbyn',
        protocol='TCP',
        fqdn='prod-deploy-ui-1.sas.yp-c.yandex.net',
        ip6_address='2a02:6b8:c08:40aa:0:472a:3cdb:0',
        port=80
    )

    resp_pb = resolver.resolve_endpoints(req_pb)
    assert resp_pb == expected_resp_pb


@pytest.mark.vcr
def test_sd_resolver_empty_es():
    resolver = yp_service_discovery.Resolver(u'test_sd_resolver_empty_es')
    req_pb = internals_pb2.TReqResolveEndpoints(
        cluster_name=u'myt',
        endpoint_set_id=u'awacs-rtc_balancer_romanovich_in_yandex-team_ru_myt')
    expected_resp_pb = internals_pb2.TRspResolveEndpoints(
        resolve_status=internals_pb2.EMPTY,
        host=u'myt1-1717-msk-yp-service-discovery-20075.gencfg-c.yandex.net',
        ruid=u'1594633255322-ff5dce2f-1023-4ef2-94b0-44efd4a5a20e',
        timestamp=1712224417687275396,
    )
    expected_resp_pb.endpoint_set.endpoint_set_id = u'awacs-rtc_balancer_romanovich_in_yandex-team_ru_myt'
    resp_pb = resolver.resolve_endpoints(req_pb)
    assert resp_pb == expected_resp_pb


@pytest.mark.vcr
def test_sd_resolver_missing_es():
    resolver = yp_service_discovery.Resolver(u'test_sd_resolver_empty_es')
    req_pb = internals_pb2.TReqResolveEndpoints(
        cluster_name=u'myt',
        endpoint_set_id=u'xxx-qwerty-31337')
    expected_resp_pb = internals_pb2.TRspResolveEndpoints(
        resolve_status=internals_pb2.NOT_EXISTS,
        host=u'myt1-1711-msk-yp-service-discovery-20075.gencfg-c.yandex.net',
        ruid=u'1594633720215-cbb88efe-c061-4add-a7b0-31d0dccdb997',
        timestamp=1712224918050963611,
    )
    resp_pb = resolver.resolve_endpoints(req_pb)
    assert resp_pb == expected_resp_pb

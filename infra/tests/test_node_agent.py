# coding: utf-8

import pytest
import cppzoom


@pytest.fixture
def unistat_endpoints():
    return [
        cppzoom.UnistatEndpoint(
            port=8080,
            path='/unistat',
            prefix='',
            labels={
                'itype': 'deploy',
                'deploy_unit': 'MultiClusterReplicaSet',
                'ctype': 'stagectl-testing'
            },
            container_fqdn='zgiwdfj5nd3mdmu7.vla.yp-c.yandex.net'
        ),
        cppzoom.UnistatEndpoint(
            port=1,
            path='/sensors',
            prefix='',
            labels={
                'itype': 'deploy',
                'deploy_unit': 'MultiClusterReplicaSet',
                'ctype': 'stagectl-testing'
            },
            container_fqdn='zgiwdfj5nd3mdmu7.vla.yp-c.yandex.net'
        ),
    ]


@pytest.fixture
def pods_info():
    return [
        cppzoom.PodInfo(
            container_id='ISS-AGENT--zgiwdfj5nd3mdmu7',
            prefix='pod',
            labels={
                'itype': 'deploy',
                'deploy_unit': 'MultiClusterReplicaSet',
                'ctype': 'stagectl-testing'
            },
            container_fqdn='zgiwdfj5nd3mdmu7.vla.yp-c.yandex.net'
        ),
    ]


@pytest.fixture
def workloads_info():
    return [
        cppzoom.WorkloadInfo(
            container_id='ISS-AGENT--zgiwdfj5nd3mdmu7/pod_agent_box_stagectl-testing-box-0/workload0',
            prefix='workload',
            labels={
                'itype': 'deploy',
                'deploy_unit': 'MultiClusterReplicaSet',
                'ctype': 'stagectl-testing',
                'workload': 'workload0'
            },
            container_fqdn=''
        ),
        cppzoom.WorkloadInfo(
            container_id='ISS-AGENT--zgiwdfj5nd3mdmu7/pod_agent_box_stagectl-testing-box-0/workload1',
            prefix='workload',
            labels={
                'itype': 'deploy',
                'deploy_unit': 'MultiClusterReplicaSet',
                'ctype': 'stagectl-testing',
                'workload': 'workload1'
            },
            container_fqdn=''
        )
    ]


@pytest.fixture
def system_containers_info():
    return [
        cppzoom.ContainerInfo(
            container_id='ISS-AGENT--zgiwdfj5nd3mdmu7/pod_agent',
            labels={
                'itype': 'pod_agent',
                'deploy_unit': 'MultiClusterReplicaSet',
                'ctype': 'stagectl-testing',
                'container': 'pod_agent'
            },
            container_fqdn=''
        ),
        cppzoom.ContainerInfo(
            container_id='ISS-AGENT--zgiwdfj5nd3mdmu7/pod_agent_resource_gang_meta',
            labels={
                'itype': 'pod_agent',
                'deploy_unit': 'MultiClusterReplicaSet',
                'ctype': 'stagectl-testing',
                'container': 'pod_agent_resource_gang_meta'
            },
            container_fqdn=''
        )
    ]


def test_response_load(unistat_endpoints, pods_info, workloads_info, system_containers_info):
    with open('monitoring.data', 'rb') as f:
        loader = cppzoom.ZNodeAgentResponseLoader()

        loader.load(f.read())
        assert loader.get_unistat_endpoints() == unistat_endpoints
        assert loader.get_subagent_endpoints() == []
        assert loader.get_pods_info() == pods_info
        assert loader.get_workloads_info() == workloads_info
        assert loader.get_system_containers_info() == system_containers_info


def test_multiple_responses_load(unistat_endpoints, pods_info, workloads_info, system_containers_info):
    with open('monitoring.data', 'rb') as f:
        loader = cppzoom.ZNodeAgentResponseLoader()

        data = f.read()

        loader.load(data)
        assert loader.get_unistat_endpoints() == unistat_endpoints
        assert loader.get_subagent_endpoints() == []
        assert loader.get_pods_info() == pods_info
        assert loader.get_workloads_info() == workloads_info
        assert loader.get_system_containers_info() == system_containers_info

        loader.load(data)
        assert loader.get_unistat_endpoints() == unistat_endpoints
        assert loader.get_subagent_endpoints() == []
        assert loader.get_pods_info() == pods_info
        assert loader.get_workloads_info() == workloads_info
        assert loader.get_system_containers_info() == system_containers_info


def test_multiple_requests(unistat_endpoints, pods_info, workloads_info, system_containers_info):
    with open('monitoring.data', 'rb') as f:
        loader = cppzoom.ZNodeAgentResponseLoader()

        loader.load(f.read())
        assert loader.get_unistat_endpoints() == unistat_endpoints
        assert loader.get_subagent_endpoints() == []
        assert loader.get_pods_info() == pods_info
        assert loader.get_workloads_info() == workloads_info
        assert loader.get_system_containers_info() == system_containers_info

        assert loader.get_unistat_endpoints() == unistat_endpoints
        assert loader.get_subagent_endpoints() == []
        assert loader.get_pods_info() == pods_info
        assert loader.get_workloads_info() == workloads_info
        assert loader.get_system_containers_info() == system_containers_info


def test_no_response():
    loader = cppzoom.ZNodeAgentResponseLoader()

    assert loader.get_unistat_endpoints() == []
    assert loader.get_subagent_endpoints() == []
    assert loader.get_pods_info() == []
    assert loader.get_workloads_info() == []
    assert loader.get_system_containers_info() == []


def test_empty_response():
    loader = cppzoom.ZNodeAgentResponseLoader()

    loader.load('')

    assert loader.get_unistat_endpoints() == []
    assert loader.get_subagent_endpoints() == []
    assert loader.get_workloads_info() == []
    assert loader.get_system_containers_info() == []

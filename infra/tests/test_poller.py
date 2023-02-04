import itertools
import collections
from unittest import mock

from infra.yp_drp import yp_client, yp_poller

import pytest


pytestmark = pytest.mark.asyncio

POD_SETS = 10
PODS_PER_POD_SET = 5
RESOURCES_PER_POD_SET = 5


@pytest.fixture
def with_pod_sets(yp_env):
    client = yp_env
    return [
        client.create_object(object_type="pod_set", attributes={
            "meta": {
                "id": f"test_pod_set_{idx}",
                'acl': [{
                    'action': 'allow',
                    'subjects': ['test'],
                    'permissions': ['write'],
                }],
            },
        })
        for idx in range(POD_SETS)
    ]


@pytest.fixture
def with_resources(yp_env, with_pod_sets):
    client = yp_env
    resources = {}
    for pod_set_id in with_pod_sets:
        resources[pod_set_id] = [
            client.create_object(object_type="dynamic_resource", attributes={
                "meta": {
                    "pod_set_id": pod_set_id,
                    'acl': [{
                        'action': 'allow',
                        'subjects': ['test'],
                        'permissions': ['write'],
                    }],
                },
            })
            for idx in range(RESOURCES_PER_POD_SET)
        ]
    return resources


@pytest.fixture
def with_pods(yp_env, with_resources):
    client = yp_env
    pods = {}
    for pod_set_id, resource_ids in with_resources.items():
        pods[pod_set_id] = [
            client.create_object(object_type='pod', attributes={
                "meta": {
                    "pod_set_id": pod_set_id,
                    "id": f"{pod_set_id}-{pod_id}".replace('_', '-'),
                    'acl': [{
                        'action': 'allow',
                        'subjects': ['test'],
                        'permissions': ['write'],
                    }],
                },
                "spec": {
                    "dynamic_resources": [{
                        "id": resource_id
                    } for resource_id in resource_ids],
                },
            })
            for pod_id in range(PODS_PER_POD_SET)
        ]
    return pods


@pytest.fixture
def poller(yp_env):
    client = yp_client.YpClient(yp_env.create_grpc_object_stub())
    poller = yp_poller.YpPoller(client, 5000, 'test', statistics=mock.Mock())
    yield poller


@pytest.mark.usefixtures("with_pod_sets", "with_resources")
class TestPoller:
    async def test_pod_sets_exist(self, yp_env):
        yp_client = yp_env
        result = yp_client.get_object("pod_set", "test_pod_set_1", selectors=['/meta/id'])
        assert result[0] == 'test_pod_set_1'

    async def test_resources_exist(self, yp_env):
        yp_client = yp_env
        results = [
            r[0] for r in yp_client.select_objects(
                "dynamic_resource",
                selectors=['/meta/pod_set_id'],
                limit=POD_SETS * RESOURCES_PER_POD_SET,
            )
        ]
        assert len(results) == POD_SETS * RESOURCES_PER_POD_SET
        results = {key: len(list(value))
                   for key, value in itertools.groupby(sorted(results))
                   }
        assert len(results) == POD_SETS
        assert all(results[key] == RESOURCES_PER_POD_SET
                   for key in results
                   )

    async def test_collect_resources(self, poller):
        resources = collections.defaultdict(yp_poller.Resource)
        pod_sets = {f'test_pod_set_{idx}': set() for idx in range(POD_SETS)}
        ts = await poller.yp_client.generate_timestamp()
        assert ts > 0

        await poller._poll_resources(resources, pod_sets, ts)
        assert len(resources) == POD_SETS * RESOURCES_PER_POD_SET
        assert all(not r.expired for r in resources.values())

        resources.clear()
        del pod_sets['test_pod_set_0']
        await poller._poll_resources(resources, pod_sets, ts)
        assert len(resources) == POD_SETS * RESOURCES_PER_POD_SET
        expired = [r for r in resources.values() if r.expired]
        assert len(expired) == RESOURCES_PER_POD_SET
        assert all(r.resource.meta.pod_set_id == 'test_pod_set_0' for r in expired)

    async def test_collect_pods(self, poller, with_pods, with_resources):
        resources = collections.defaultdict(yp_poller.Resource)
        ts = await poller.yp_client.generate_timestamp()
        assert ts > 0

        pod_sets = {}
        expired_pods = []
        await poller._poll_pod_sets(pod_sets, ts)
        assert set(pod_sets.keys()) == set(with_pods.keys())

        await poller._poll_resources(resources, pod_sets, ts)
        await poller._poll_pods(resources, pod_sets, expired_pods, ts)

        assert not expired_pods
        for resource in resources.values():
            assert not resource.expired
            assert len(resource.pods) == PODS_PER_POD_SET
            assert all(pod.meta.pod_set_id == resource.resource.meta.pod_set_id
                       and any(dr['id'] == resource.resource.meta.id
                               for dr in pod.spec_dynamic_resources)
                       for pod in resource.pods
                       )

    async def test_get_expired_pods(self, poller, with_pods, with_resources):
        resources = collections.defaultdict(yp_poller.Resource)
        ts = await poller.yp_client.generate_timestamp()
        assert ts > 0

        pod_sets = {}
        expired_pods = []
        await poller._poll_pod_sets(pod_sets, ts)
        assert set(pod_sets.keys()) == set(with_pods.keys())

        await poller._poll_resources(resources, pod_sets, ts)
        await poller._poll_pods(resources, pod_sets, expired_pods, ts)

        assert not expired_pods
        resource_id, resource = next(iter(resources.items()))
        await poller.yp_client.remove_resources([resource_id], 1)

        pod_sets = {}
        resources = collections.defaultdict(yp_poller.Resource)
        ts = await poller.yp_client.generate_timestamp()
        assert ts > 0

        await poller._poll_pod_sets(pod_sets, ts)
        await poller._poll_resources(resources, pod_sets, ts)
        await poller._poll_pods(resources, pod_sets, expired_pods, ts)
        assert resource_id not in resources, str(resources[resource_id])
        assert (
            {pod.meta.id for pod in resource.pods}
            ==
            {pod.meta.id for pod in expired_pods}
        )

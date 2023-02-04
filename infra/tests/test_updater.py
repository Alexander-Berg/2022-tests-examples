import asyncio
import collections
from unittest import mock

import pytest

from infra.yp_drp import yp_poller, yp_client, dru


pytestmark = pytest.mark.asyncio

POD_SETS = 10
PODS_PER_POD_SET = 30
RESOURCES_PER_POD_SET = 5
CHUNK_SIZE = 3


def count_scheduled(resource_id, resource, revision):
    return sum(
        1 for pod in resource.pods
        if next(
            filter(lambda spec: spec.get('id') == resource_id, pod.spec_dynamic_resources),
            {}
        ).get('revision', None) == revision
    )


def update_all_scheduled(yp_env, resource_id, resource, revision):
    for pod in resource.pods:
        if (
            next(
                filter(lambda res: res.get('id') == resource_id, pod.spec_dynamic_resources),
                {}
            ).get('revision', None) == revision
        ):
            status = next(
                filter(lambda res: res.get('id') == resource_id, pod.status_dynamic_resources),
                None
            )
            if status is None:
                status = {}
                pod.status_dynamic_resources.append(status)

            status['id'] = resource_id
            status['revision'] = revision
            status.setdefault('ready', {})['status'] = 'true'
            status.setdefault('in_progress', {})['status'] = 'false'
            status.setdefault('error', {})['status'] = 'false'

            yp_env.update_object('pod', pod.meta.id, [
                {
                    'path': '/status/dynamic_resources',
                    'value': pod.status_dynamic_resources,
                }
            ])


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
                    "id": f"pod-{pod_set_id}-{pod_id}".replace('_', '-'),
                    'acl': [{
                        'action': 'allow',
                        'subjects': ['test'],
                        'permissions': ['write'],
                    }],
                },
            })
            for pod_id in range(PODS_PER_POD_SET)
        ]
        for pod_id in pods[pod_set_id]:
            client.update_object('pod', pod_id, [
                {
                    'path': '/spec/dynamic_resources',
                    'value': [{
                        "id": resource_id
                    } for resource_id in resource_ids]
                }
            ])
    return pods


@pytest.fixture
def with_in_progress_update(with_resources, yp_env):
    client = yp_env
    pod_set_id, resources = next(iter(with_resources.items()))
    resource_id = resources[0]
    client.update_object('dynamic_resource', resource_id, [
        {
            'path': '/spec',
            'value': {
                'revision': 5,
                'update_window': CHUNK_SIZE,
                'deploy_groups': [{}],
            }
        }
    ])
    pods = [
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
        })
        for pod_id in range(PODS_PER_POD_SET)
    ]

    # never_scheduled_pods = pods[:CHUNK_SIZE]
    old_scheduled_not_updated_pods = pods[CHUNK_SIZE:2 * CHUNK_SIZE]
    old_scheduled_old_updated_pods = pods[2 * CHUNK_SIZE:3 * CHUNK_SIZE]
    old_scheduled_and_updated_pods = pods[3 * CHUNK_SIZE:4 * CHUNK_SIZE]
    old_scheduled_and_error_pods = pods[4 * CHUNK_SIZE:5 * CHUNK_SIZE]
    new_scheduled_not_updated_pods = pods[5 * CHUNK_SIZE:6 * CHUNK_SIZE]
    new_scheduled_old_updated_pods = pods[6 * CHUNK_SIZE:7 * CHUNK_SIZE]
    new_scheduled_and_updated_pods = pods[7 * CHUNK_SIZE:8 * CHUNK_SIZE]
    new_scheduled_and_error_pods = pods[8 * CHUNK_SIZE:9 * CHUNK_SIZE]
    new_scheduled_in_progress_pods = pods[9 * CHUNK_SIZE:]
    for pod in old_scheduled_not_updated_pods:
        client.update_object('pod', pod, [
            {
                'path': '/spec/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 3,
                }]
            }
        ])
    for pod in old_scheduled_old_updated_pods:
        client.update_object('pod', pod, [
            {
                'path': '/spec/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 3,
                }]
            },
            {
                'path': '/status/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 1,
                    'ready': {
                        'status': 1,
                    }
                }]
            }
        ])
    for pod in old_scheduled_and_updated_pods:
        client.update_object('pod', pod, [
            {
                'path': '/spec/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 3,
                }]
            },
            {
                'path': '/status/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 3,
                    'ready': {
                        'status': 1,
                    }
                }]
            }
        ])
    for pod in old_scheduled_and_error_pods:
        client.update_object('pod', pod, [
            {
                'path': '/spec/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 3,
                }]
            },
            {
                'path': '/status/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 3,
                    'error': {
                        'status': 1,
                    }
                }]
            }
        ])
    for pod in new_scheduled_not_updated_pods:
        client.update_object('pod', pod, [
            {
                'path': '/spec/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 5,
                }]
            },
        ])
    for pod in new_scheduled_old_updated_pods:
        client.update_object('pod', pod, [
            {
                'path': '/spec/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 5,
                }]
            },
            {
                'path': '/status/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 3,
                    'ready': {
                        'status': 1,
                    }
                }]
            }
        ])
    for pod in new_scheduled_and_updated_pods:
        client.update_object('pod', pod, [
            {
                'path': '/spec/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 5,
                }]
            },
            {
                'path': '/status/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 5,
                    'ready': {
                        'status': 1,
                    }
                }]
            }
        ])
    for pod in new_scheduled_and_error_pods:
        client.update_object('pod', pod, [
            {
                'path': '/spec/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 5,
                }]
            },
            {
                'path': '/status/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 5,
                    'error': {
                        'status': 1,
                    }
                }]
            }
        ])
    for pod in new_scheduled_in_progress_pods:
        client.update_object('pod', pod, [
            {
                'path': '/spec/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 5,
                }]
            },
            {
                'path': '/status/dynamic_resources',
                'value': [{
                    'id': resource_id,
                    'revision': 5,
                    'in_progress': {
                        'status': 1,
                    }
                }]
            }
        ])
    return resource_id


@pytest.fixture
def client(yp_env):
    yield yp_client.YpClient(yp_env.create_grpc_object_stub())


@pytest.fixture
def updater(client):
    yield dru.DynamicResourceUpdater(client)


@pytest.fixture
def poller(client):
    yield yp_poller.YpPoller(client, 5000, 'test', statistics=mock.Mock())


class TestUpdater:
    async def get_current_state(self, poller):
        pod_sets = {}
        expired_pods = []
        resources = collections.defaultdict(yp_poller.Resource)
        ts = await poller.yp_client.generate_timestamp()

        await poller._poll_pod_sets(pod_sets, ts)
        await poller._poll_resources(resources, pod_sets, ts)
        await poller._poll_pods(resources, pod_sets, expired_pods, ts)

        return resources

    async def test_remove_expired(self, poller, updater, with_pods):
        resources = await self.get_current_state(poller)
        removal = asyncio.Future()

        def mocked_apply(fn, *args, **kwargs):
            nonlocal removal
            removal.set_result((fn, args, kwargs))
            res = asyncio.Future()
            res.set_result(None)
            return res

        with mock.patch.object(updater.client, '_apply') as _apply:
            _apply.side_effect = mocked_apply
            for res_id, r in resources.items():
                await updater.update_allocations(res_id, r.resource.spec, r.pods, True)
                await updater.commit(100)
                assert removal.done()
                fn, args, kwargs = await removal
                removed_resource = args[0].subrequests[0]
                assert removed_resource.object_id == r.resource.meta.id

                removal = asyncio.Future()

    async def test_sync_pod_status(self, yp_env, poller, updater, with_in_progress_update):
        resource_id = with_in_progress_update
        resources = await self.get_current_state(poller)
        r = resources[resource_id]
        pod_id = r.pods[0].meta.id

        yp_env.update_object('pod', pod_id, [
            {
                'path': '/spec/pod_agent_payload',
                'value': {
                    'spec': {
                        'workloads': [
                            {
                                'id': '__dru',
                            }
                        ]
                    }
                }
            },
            {
                'path': '/status/agent',
                'value': {
                    'pod_agent_payload': {
                        'status': {
                            'workloads': [
                                {
                                    'id': '__dru',
                                    'start': {
                                        'current': {
                                            'stdout_tail': (
                                                b'{"kind": "state", "states": [{"resource_id": "%b", "revision": 1, "in_progress": false, "ready": false, '
                                                b'"error": true, "reason": "Failed to update resource to revision 1: storage dir /basesearch/dru does not exist", '
                                                b'"mark": null}]}'
                                            ) % resource_id.encode(),
                                        }
                                    }
                                }
                            ]
                        }
                    }
                },
            }
        ])

        resources = await self.get_current_state(poller)
        r = resources[resource_id]

        pod = next(iter(filter(lambda pod: pod.meta.id == pod_id, r.pods)))
        await updater.sync_pod_status(pod)

        status = next(iter(filter(lambda status: status.get('id') == resource_id, pod.status_dynamic_resources)))
        assert status['revision'] == 1
        assert status['ready']['status'] == 'false'
        assert status['in_progress']['status'] == 'false'
        assert status['error']['status'] == 'true'
        assert status['error']['reason'] == 'Failed to update resource to revision 1: storage dir /basesearch/dru does not exist'

    async def test_sync_resource_status(self, poller, updater, with_in_progress_update):
        resource_id = with_in_progress_update
        resources = await self.get_current_state(poller)
        r = resources[resource_id]

        statuses = {}
        for pod in r.pods:
            await updater.count_resource_statuses(pod, statuses)

        await updater.sync_resource_status(
            resource_id,
            r.resource.status,
            statuses[resource_id],
            r.resource.spec['update_window'],
            len(r.pods),
        )
        await updater.commit(100)
        resources = await self.get_current_state(poller)
        r = resources[resource_id]

        status = sorted(r.resource.status['revisions'], key=lambda res: res['revision'])
        # we have only revisions 1, 3, 5
        assert len(status) == 3

        rev1 = status[0]
        assert rev1['ready']['pod_count'] == CHUNK_SIZE
        assert rev1['ready']['condition']['status'] == 'false'
        assert rev1['in_progress'].get('pod_count', 0) == 0
        assert rev1['in_progress']['condition']['status'] == 'false'
        assert rev1['error'].get('pod_count', 0) == 0
        assert rev1['error']['condition']['status'] == 'false'

        rev3 = status[1]
        assert rev3['ready']['pod_count'] == CHUNK_SIZE * 2
        assert rev3['ready']['condition']['status'] == 'false'
        assert rev3['in_progress'].get('pod_count', 0) == 0
        assert rev3['in_progress']['condition']['status'] == 'false'
        assert rev3['error']['pod_count'] == CHUNK_SIZE
        assert rev3['error']['condition']['status'] == 'true'

        rev5 = status[2]
        assert rev5['ready']['pod_count'] == CHUNK_SIZE
        assert rev5['ready']['condition']['status'] == 'false'
        assert rev5['in_progress']['pod_count'] == CHUNK_SIZE
        assert rev5['in_progress']['condition']['status'] == 'true'
        assert rev5['error']['pod_count'] == CHUNK_SIZE
        assert rev5['error']['condition']['status'] == 'true'

    async def test_scheduling(self, yp_env, poller, updater, with_in_progress_update):
        resource_id = with_in_progress_update
        resources = await self.get_current_state(poller)
        r = resources[resource_id]

        statuses = {}
        for pod in r.pods:
            await updater.count_resource_statuses(pod, statuses)

        await updater.sync_resource_status(
            resource_id,
            r.resource.status,
            statuses[resource_id],
            r.resource.spec['update_window'],
            len(r.pods),
        )
        await updater.commit(100)

        assert len(r.pods) == PODS_PER_POD_SET

        scheduled = count_scheduled(resource_id, r, 5)
        # without changes nothing should be changed
        await updater.update_allocations(resource_id, r.resource.spec, r.pods, False)
        await updater.commit(100)
        assert count_scheduled(resource_id, r, 5) == scheduled

        steps = (PODS_PER_POD_SET - scheduled) // CHUNK_SIZE
        for step in range(steps):
            update_all_scheduled(yp_env, resource_id, r, 5)
            await updater.update_allocations(resource_id, r.resource.spec, r.pods, False)
            await updater.commit(100)
            new_scheduled = count_scheduled(resource_id, r, 5)
            assert new_scheduled == scheduled + CHUNK_SIZE
            scheduled = new_scheduled

    async def test_unmanaged_resources(self, yp_env, poller, updater, with_in_progress_update):
        resource_id = with_in_progress_update
        resources = await self.get_current_state(poller)
        r = resources[resource_id]

        assert len(r.pods) == PODS_PER_POD_SET

        yp_env.update_object('dynamic_resource', resource_id, [
            {
                'path': '/labels',
                'value': {
                    'deploy_engine': 'manual',
                },
            }
        ])

        scheduled = count_scheduled(resource_id, r, 5)
        await poller._iterate()

        assert scheduled == count_scheduled(resource_id, r, 5)

        resources = await self.get_current_state(poller)
        r = resources[resource_id]
        assert scheduled == count_scheduled(resource_id, r, 5)

    async def test_labels(self, yp_env, poller, updater, with_resources, with_pods):
        pod_set_id, resources = next(iter(with_resources.items()))
        resource_id = resources[0]
        pods = with_pods[pod_set_id]
        chunk_size = len(pods) // 5

        abcd_pods = pods[:chunk_size]
        adcb_pods = pods[chunk_size:chunk_size * 2]
        ef_pods = pods[chunk_size * 2:chunk_size * 3]
        abcdef_pods = pods[chunk_size * 3:chunk_size * 4]
        # empty_pods = pods[chunk_size * 4:]

        yp_env.update_object('dynamic_resource', resource_id, [
            {
                'path': '/spec',
                'value': {
                    'update_window': len(pods),
                    'revision': 10,
                    'deploy_groups': [
                        {
                            'mark': 'A=B,C=D',
                            'required_labels': {
                                'A': 'B',
                                'C': 'D',
                            },
                            'urls': ['raw:bd'],
                        },
                        {
                            'mark': 'A=D,C=B',
                            'required_labels': {
                                'A': 'D',
                                'C': 'B',
                            },
                            'urls': ['raw:db'],
                        },
                        {
                            'mark': 'E=F',
                            'required_labels': {
                                'E': 'F',
                            },
                            'urls': ['raw:f'],
                        },
                    ],
                },
            },
        ])

        for pod_id in abcd_pods:
            yp_env.update_object('pod', pod_id, [
                {
                    'path': '/labels',
                    'value': {
                        'A': 'B',
                        'C': 'D',
                    }
                },
            ])

        for pod_id in adcb_pods:
            yp_env.update_object('pod', pod_id, [
                {
                    'path': '/labels',
                    'value': {
                        'A': 'D',
                        'C': 'B',
                    }
                },
            ])

        for pod_id in ef_pods:
            yp_env.update_object('pod', pod_id, [
                {
                    'path': '/labels',
                    'value': {
                        'E': 'F',
                    }
                },
            ])

        for pod_id in abcdef_pods:
            yp_env.update_object('pod', pod_id, [
                {
                    'path': '/labels',
                    'value': {
                        'A': 'B',
                        'C': 'D',
                        'E': 'F',
                    }
                },
            ])

        resources = await self.get_current_state(poller)
        r = resources[resource_id]

        statuses = {}
        for pod in r.pods:
            await updater.count_resource_statuses(pod, statuses)

        await updater.sync_resource_status(
            resource_id,
            r.resource.status,
            statuses.get(resource_id, {}),
            r.resource.spec['update_window'],
            len(r.pods),
        )
        await updater.update_allocations(resource_id, r.resource.spec, r.pods, False)

        assert chunk_size == sum(
            1 for pod in r.pods
            if next(iter(pod.spec_dynamic_resources), {}).get('urls') == ['raw:bd']
        )

        assert chunk_size == sum(
            1 for pod in r.pods
            if next(iter(pod.spec_dynamic_resources), {}).get('urls') == ['raw:db']
        )

        assert chunk_size == sum(
            1 for pod in r.pods
            if next(iter(pod.spec_dynamic_resources), {}).get('urls') == ['raw:f']
        )

        assert len(pods) - chunk_size * 3 == sum(
            1 for pod in r.pods
            if not next(iter(pod.spec_dynamic_resources), {}).get('urls')
        )

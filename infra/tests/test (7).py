from math import ceil
from random import randint
import logging
import pytest
import sys

from infra.service_controller.controller import StandaloneController


COMMON_WHITELIST = {
    "Selectors": [
        {
            "Path": "/meta/pod_set_id",
            "WatchEnabled": False
        },
        {
            "Path": "/labels/a",
            "WatchEnabled": True
        }
    ],
    "KeyField": "/meta/pod_set_id"
}

CLUSTER_WHITELISTS = [
    {
        "Cluster": "localhost",
        "Selectors": [
            {
                "Path": "/meta/pod_set_id",
                "WatchEnabled": False
            },
            {
                "Path": "/labels/a",
                "WatchEnabled": True
            }
        ],
        "KeyField": "/meta/pod_set_id"
    }
]


def create_controller(yp_instance, common_whitelist=COMMON_WHITELIST, cluster_whitelists=CLUSTER_WHITELISTS):
    return StandaloneController({
        "Controller": {
            "YpClient": {
                "Address": yp_instance.yp_client_grpc_address,
                "EnableSsl": False
            },
            "LeadingInvader": {
                "Path": "//home",
                "Proxy": yp_instance.create_yt_client().config["proxy"]["url"]
            },
            "Logger": {
                "Level": "DEBUG",
                "Backend": "STDERR",
            },
            "UpdatableConfigOptions": {
                "WatchPatchConfig": {
                    "Path": "controls/config_patch.json",
                    "ValidPatchPath": "backup/valid_patch.json",
                    "Frequency": "10s"
                },
                "ConfigUpdatesLoggerConfig": {
                    "Path": "current-config-updates-eventlog",
                    "Backend": "FILE",
                    "Level": "DEBUG"
                },
                "Enabled": True
            }
        },
        "EndpointSetManagerFactory": {
            "ClientFilterConfig": {
                "CommonWhitelist": common_whitelist,
                "ClusterWhitelists": cluster_whitelists,
                "Enabled": True,
                "WatchSelectorsEnabled": True,
                "WhitelistEnabled": True,
                "MergeLabels": True,
                "WatchSelectorsEnabled": True,
                "WatchBannedSelectors": [
                    "/meta/id"
                ]
            }
        }
    })


@pytest.mark.usefixtures("ctl_env")
class TestEndpointSetController(object):
    def test_sync(self, ctl_env):
        yp_client = ctl_env.yp_client

        node_id = ctl_env.create_node()

        controller = create_controller(ctl_env.yp_instance, cluster_whitelists=[])

        pod_set_id1 = yp_client.create_object("pod_set")
        pod1 = ctl_env.create_yplite_pod(node_id, pod_set_id1)

        pod_set_id2 = yp_client.create_object("pod_set")
        pod2 = ctl_env.create_yplite_pod(node_id, pod_set_id2)

        pod3 = ctl_env.create_yplite_pod(node_id, pod_set_id2, "PREPARED", [{"targetState": "PREPARED"}, {"targetState": "REMOVED"}, {"targetState": "ACTIVE"}])

        pod4 = ctl_env.create_yplite_pod(node_id, pod_set_id2, "PREPARED", [{"targetState": "PREPARED"}, {"targetState": "REMOVED"}])

        ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id1), 1234)
        ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id2), 1234)
        controller.sync()

        before = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(before) == 3

        yp_client.remove_object("pod", pod1.id)
        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        before = after[:]
        yp_client.remove_object("pod", pod4.id)
        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2
        assert sorted(before) == sorted(after)

        yp_client.update_object("pod", pod3.id, [
            {
                "path": "/spec/iss/instances",
                "value": [{
                    "targetState": "REMOVED",
                }]
            }
        ])
        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 1

        yp_client.update_object("pod", pod3.id, [
            {
                "path": "/spec/iss/instances",
                "value": [{
                    "targetState": "ACTIVE",
                }]
            }
        ])
        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        yp_client.remove_object("pod", pod2.id)
        pod5 = ctl_env.create_yplite_pod(node_id, pod_set_id1)
        controller.sync()

        before = after[:]
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        common_cnt = 0
        for it in before:
            if it in after:
                common_cnt += 1

        assert(common_cnt == 1)

        yp_client.remove_object("pod", pod5.id)
        yp_client.remove_object("pod", pod3.id)
        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert not after


@pytest.mark.usefixtures("ctl_env")
class TestEndpointSetsSelectionSupervisor(object):
    def test_sync(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = create_controller(ctl_env.yp_instance, cluster_whitelists=[])

        node_id = ctl_env.create_node()

        pod_set_id1 = yp_client.create_object("pod_set")
        ctl_env.create_yplite_pod(node_id, pod_set_id1)

        pod_set_id2 = yp_client.create_object("pod_set")
        ctl_env.create_yplite_pod(node_id, pod_set_id2)

        pod_set_id3 = yp_client.create_object("pod_set")
        ctl_env.create_yplite_pod(node_id, pod_set_id3)

        pod_set_id4 = yp_client.create_object("pod_set")
        ctl_env.create_yplite_pod(node_id, pod_set_id4)

        endpoint_set_id1 = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id1), 1234)
        endpoint_set_id2 = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id2), 1234)
        endpoint_set_id3 = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id3), 1234)
        endpoint_set_id4 = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id4), 1234)

        yp_client.update_object("endpoint_set", endpoint_set_id1, [
            {
                "path": "/labels",
                "value": {
                    "supervisor": "something",
                },
            }
        ])
        yp_client.update_object("endpoint_set", endpoint_set_id2, [
            {
                "path": "/labels",
                "value": {
                    "supervisor": "service-controller",
                },
            }
        ])
        yp_client.update_object("endpoint_set", endpoint_set_id3, [
            {
                "path": "/labels",
                "value": {
                    "supervisor": {"key1": [1, 2, 3], "key2": False},
                },
            }
        ])

        controller.sync()

        status = yp_client.get_object("endpoint_set", endpoint_set_id1, selectors=["/status/controller/error"])[0]
        assert status["message"] == "Sync is ommitted."
        assert status["inner_errors"][0]["message"] == "Supervisor value is unsuitable. https://docs.yandex-team.ru/service-controller/"

        assert not yp_client.get_object("endpoint_set", endpoint_set_id2, selectors=["/status/controller/error"])[0]

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        yp_client.remove_object("endpoint_set", endpoint_set_id1)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        yp_client.remove_object("endpoint_set", endpoint_set_id3)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        yp_client.remove_object("endpoint_set", endpoint_set_id2)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 1

        yp_client.remove_object("endpoint_set", endpoint_set_id4)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert not after


@pytest.mark.usefixtures("ctl_env")
class TestEndpointSetsSelectionFiltersWithHash(object):
    # Add more specific tests after (YP-3472)
    # Currently needs MergeLabels=True to pass Match correctly
    def test_sync(self, ctl_env):
        superuser = ctl_env.create_user("service_controller_superuser", {}, superuser=True, grant_permissions=[("endpoint_set", "write"), ("pod", "write")])
        yp_client = ctl_env.yp_instance.create_client(config={"user": superuser})

        controller = create_controller(ctl_env.yp_instance, cluster_whitelists=[])

        node_id = ctl_env.create_node()

        pod_set_id = yp_client.create_object("pod_set")
        ctl_env.create_yplite_pod(node_id, pod_set_id)
        ctl_env.create_yplite_pod(node_id, pod_set_id)
        labeled_pod_id = ctl_env.create_yplite_pod(node_id, pod_set_id).id

        yp_client.update_object("pod", labeled_pod_id, [
            {
                "path": "/labels/a",
                "value": "1",
            }
        ])

        endpoint_set_id = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}" AND [/labels/a] = #'.format(pod_set_id), 1234)

        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        yp_client.remove_object("endpoint_set", endpoint_set_id)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert not after


@pytest.mark.usefixtures("ctl_env")
class TestEndpointSetsSelectionAttributeWithHash(object):
    def test_sync(self, ctl_env):
        superuser = ctl_env.create_user("service_controller_superuser", {}, superuser=True, grant_permissions=[("endpoint_set", "write"), ("pod", "write")])
        yp_client = ctl_env.yp_instance.create_client(config={"user": superuser})

        controller = create_controller(ctl_env.yp_instance, cluster_whitelists=[])

        node_id = ctl_env.create_node()

        pod_set_id = yp_client.create_object("pod_set")
        ctl_env.create_yplite_pod(node_id, pod_set_id)
        ctl_env.create_yplite_pod(node_id, pod_set_id)
        labeled_pod_id = ctl_env.create_yplite_pod(node_id, pod_set_id).id

        yp_client.update_object("pod", labeled_pod_id, [
            {
                "path": "/labels/a",
                "value": "1",
            }
        ])

        endpoint_set_id = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}" AND try_get_string([/labels/a], "") != "1"'.format(pod_set_id), 1234)

        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        yp_client.remove_object("endpoint_set", endpoint_set_id)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert not after


@pytest.mark.usefixtures("ctl_env")
class TestEndpointSetsSelectionPodFilter(object):
    def test_sync(self, ctl_env):
        superuser = ctl_env.create_user("service_controller_superuser", {}, superuser=True, grant_permissions=[("endpoint_set", "write")])
        yp_client = ctl_env.yp_instance.create_client(config={"user": superuser})

        controller = create_controller(ctl_env.yp_instance, cluster_whitelists=[])

        node_id = ctl_env.create_node()

        pod_set_id1 = yp_client.create_object("pod_set")
        ctl_env.create_yplite_pod(node_id, pod_set_id1)

        pod_set_id2 = yp_client.create_object("pod_set")
        ctl_env.create_yplite_pod(node_id, pod_set_id2)

        pod_set_id3 = yp_client.create_object("pod_set")
        ctl_env.create_yplite_pod(node_id, pod_set_id3)

        pod_set_id4 = yp_client.create_object("pod_set")
        pod_id4 = ctl_env.create_yplite_pod(node_id, pod_set_id4).id

        pod_set_id5 = yp_client.create_object("pod_set")
        pod_id5 = ctl_env.create_yplite_pod(node_id, pod_set_id5).id

        pod_set_id6 = yp_client.create_object("pod_set", attributes={"meta": {"id": "pod_set_id_substring"}})
        pod_id6 = ctl_env.create_yplite_pod(node_id, pod_set_id6).id

        endpoint_set_id1 = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id1), 1234)
        endpoint_set_id2 = ctl_env.create_endpoint_set('[/meta/pod_set_id] = ""', 1234)
        endpoint_set_id3 = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id3), 1234)
        endpoint_set_id4 = ctl_env.create_endpoint_set('[/meta/id] = "{}"'.format(pod_id4), 1234)
        endpoint_set_id5 = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}" AND [/labels/a] != #'.format(pod_set_id5), 1234)
        endpoint_set_id6 = ctl_env.create_endpoint_set('is_substr("id_substring", [/meta/pod_set_id]) AND [/labels/a] = "2"', 1234)

        yp_client.update_object("endpoint_set", endpoint_set_id1, [
            {
                "path": "/spec",
                "value": {},
            }
        ])

        yp_client.update_object("pod", pod_id5, [
            {
                "path": "/labels/a",
                "value": "1",
            }
        ])

        yp_client.update_object("pod", pod_id6, [
            {
                "path": "/labels/a",
                "value": "2",
            }
        ])

        controller.sync()

        status = yp_client.get_object("endpoint_set", endpoint_set_id1, selectors=["/status/controller/error"])[0]
        assert status["message"] == "Sync is ommitted."
        assert status["inner_errors"][0]["message"] == "Pod filter is empty. https://docs.yandex-team.ru/service-controller/#validaciya-endpoint-setov-na-urovne-sc"

        assert not yp_client.get_object("endpoint_set", endpoint_set_id3, selectors=["/status/controller/error"])[0]

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 3

        yp_client.remove_object("endpoint_set", endpoint_set_id1)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 3

        yp_client.remove_object("endpoint_set", endpoint_set_id2)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 3

        yp_client.remove_object("endpoint_set", endpoint_set_id4)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 3

        yp_client.remove_object("endpoint_set", endpoint_set_id3)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        yp_client.remove_object("endpoint_set", endpoint_set_id5)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 1

        yp_client.remove_object("endpoint_set", endpoint_set_id6)
        controller.sync()
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert not after


@pytest.mark.usefixtures("ctl_env")
class TestLivenessLimitRatio(object):
    def test_sync(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = create_controller(ctl_env.yp_instance, cluster_whitelists=[])

        node_id = ctl_env.create_node()

        pod_set_id = yp_client.create_object("pod_set")

        ready_pods_count = 15
        for i in range(ready_pods_count):
            if i % 2 == 0:
                ctl_env.create_yplite_pod(node_id, pod_set_id)
            else:
                ctl_env.create_yplite_pod(node_id, pod_set_id, "PREPARED", [{"targetState": "PREPARED"}, {"targetState": "REMOVED"}, {"targetState": "ACTIVE"}])

        not_ready_pods_count = 20
        for i in range(not_ready_pods_count):
            ctl_env.create_yplite_pod(node_id, pod_set_id, "PREPARED", [{"targetState": "PREPARED"}, {"targetState": "REMOVED"}])

        endpoint_set_id = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id), 1234)

        liveness_limit_ratio = 0.0
        step = 0.05
        while liveness_limit_ratio <= 1:
            yp_client.update_object("endpoint_set", endpoint_set_id, set_updates=[{"path": "/spec/liveness_limit_ratio", "value": liveness_limit_ratio}])
            controller.sync()

            endpoints = yp_client.select_objects("endpoint", selectors=["/status/ready"], filter="[/meta/endpoint_set_id]=\"{}\"".format(endpoint_set_id))
            pods_count = ready_pods_count + not_ready_pods_count
            assert len(endpoints) >= ceil(liveness_limit_ratio * pods_count)

            ready_endpoints_count = sum([i[0] for i in endpoints])
            assert ready_endpoints_count == ready_pods_count

            liveness_limit_ratio += step


@pytest.mark.usefixtures("ctl_env")
class TestPodAgentTargetStateImpactWithLivenessLimitRatio(object):
    def test_sync(self, ctl_env):
        logger = logging.getLogger()
        logger.handlers = [logging.StreamHandler(sys.stderr)]
        logger.setLevel(logging.DEBUG)

        yp_client = ctl_env.yp_client
        controller = create_controller(ctl_env.yp_instance, cluster_whitelists=[])

        node_id = ctl_env.create_node()

        pod_set_id = yp_client.create_object("pod_set")

        ready_pods_count = randint(10, 15)
        removed_pods_count = randint(10, 15)
        suspended_pods_count = randint(10, 15)
        logger.info("randomly choose pod count: ready = {}, removed = {}, suspended = {}".format(ready_pods_count, removed_pods_count, suspended_pods_count))

        for i in range(ready_pods_count):
            ctl_env.create_deploy_pod(node_id, pod_set_id)
        for i in range(removed_pods_count):
            ctl_env.create_deploy_pod(node_id, pod_set_id, "removed", workloads=[{"target_state": "active"}])
        for i in range(suspended_pods_count):
            ctl_env.create_deploy_pod(node_id, pod_set_id, "suspended", workloads=[{"target_state": "active"}])

        endpoint_set_id = ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id), 1234)

        liveness_limit_ratio = 0.0
        step = 0.05
        while liveness_limit_ratio <= 1:
            yp_client.update_object("endpoint_set", endpoint_set_id, set_updates=[{"path": "/spec/liveness_limit_ratio", "value": liveness_limit_ratio}])
            controller.sync()

            endpoints = yp_client.select_objects("endpoint", selectors=["/status/ready"], filter="[/meta/endpoint_set_id]=\"{}\"".format(endpoint_set_id))
            pods_count = ready_pods_count + removed_pods_count + suspended_pods_count
            assert len(endpoints) >= ceil(liveness_limit_ratio * pods_count)

            ready_endpoints_count = sum([i[0] for i in endpoints])
            assert ready_endpoints_count == ready_pods_count

            liveness_limit_ratio += step


@pytest.mark.usefixtures("ctl_env")
class TestEndpointSetControllerForPodAgentPods(object):
    def test_sync(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = create_controller(ctl_env.yp_instance, cluster_whitelists=[])

        node_id = ctl_env.create_node()

        pod_set_id1 = yp_client.create_object("pod_set")
        pod1 = ctl_env.create_deploy_pod(node_id, pod_set_id1)

        pod_set_id2 = yp_client.create_object("pod_set")
        pod2 = ctl_env.create_deploy_pod(node_id, pod_set_id2, workloads=[{"target_state": "removed"}])
        pod3 = ctl_env.create_deploy_pod(node_id, pod_set_id2, ready=False)
        pod4 = ctl_env.create_deploy_pod(node_id, pod_set_id2, ready=False, workloads=[{"target_state": "removed"}])

        ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id1), 1234)
        ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id2), 1234)
        controller.sync()

        before = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(before) == 2

        yp_client.remove_object("pod", pod1.id)
        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 1

        before = after[:]
        yp_client.remove_object("pod", pod4.id)
        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 1
        assert sorted(before) == sorted(after)

        yp_client.update_objects([{"object_type": "pod", "object_id": pod2.id, "set_updates": [
            {"path": "/status/agent/pod_agent_payload/status/ready/status", "value": "false", "recursive": True},
        ]}])

        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert not after

        yp_client.update_objects([{"object_type": "pod", "object_id": pod3.id, "set_updates": [
            {"path": "/status/agent/pod_agent_payload/status/ready/status", "value": "true", "recursive": True},
        ]}])

        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 1

        yp_client.remove_object("pod", pod2.id)
        pod5 = ctl_env.create_yplite_pod(node_id, pod_set_id1)
        yp_client.update_objects([{"object_type": "pod", "object_id": pod5.id, "set_updates": [
            {"path": "/status/agent/pod_agent_payload/status/ready/status", "value": "true", "recursive": True},
        ]}])

        controller.sync()

        before = after[:]
        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 2

        common_cnt = 0
        for it in before:
            if it in after:
                common_cnt += 1

        assert(common_cnt == 1)

        yp_client.remove_object("pod", pod5.id)
        yp_client.remove_object("pod", pod3.id)
        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert not after


@pytest.mark.usefixtures("ctl_env")
class TestDuplicatedEndpoints(object):
    def test_sync(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = create_controller(ctl_env.yp_instance, cluster_whitelists=[])

        node_id = ctl_env.create_node()

        pod_set_id = yp_client.create_object("pod_set")
        ctl_env.create_pod(node_id, pod_set_id, "ACTIVE")

        ctl_env.create_endpoint_set('[/meta/pod_set_id] = "{}"'.format(pod_set_id), 1234)
        controller.sync()

        before = yp_client.select_objects("endpoint", selectors=["/meta", "/spec", "/status"])
        assert len(before) == 1

        endpoint = before[0]
        yp_client.create_object(
            "endpoint",
            attributes={
                "meta": {
                    "endpoint_set_id": endpoint[0]["endpoint_set_id"]
                },
                "spec": endpoint[1],
                "status": endpoint[2]
            }
        )

        assert len(yp_client.select_objects("endpoint", selectors=["/meta", "/spec", "/status"])) == 2
        # Must delete duplicated endpoint
        controller.sync()

        after = yp_client.select_objects("endpoint", selectors=["/meta/id"])
        assert len(after) == 1

import logging
import time
import typing

from freezegun import freeze_time

from infra.rtc_sla_tentacles.backend.lib.config.interface import ConfigInterface
from infra.rtc_sla_tentacles.backend.lib.yp_lite.pods_manager import YpLitePodsManager, \
    YpLitePodsManagerPodData as PodData, PodInfo, PodDiskReq

yp_nodes = [
    "a-node.y.n",
    # "b-node.y.n",
    "c-node.y.n",
    "d-node.y.n",
    "e-node.y.n",
    "f-node.y.n",
    # "g-node.y.n",
    "h-node.y.n",
    # "i-node.y.n"
    # "j-node.y.n"
]
pods = {
    "a-pod": PodInfo("a-pod.y.n", "a-node.y.n", "a-node.y.n", PodDiskReq.PodDiskReqHdd),
    "b-pod": PodInfo("b-pod.y.n", "", "b-node.y.n", PodDiskReq.PodDiskReqHdd),
    "c-pod": PodInfo("c-pod.y.n", "c-node.y.n", "c-node.y.n", PodDiskReq.PodDiskReqHdd),
    "d-pod": PodInfo("d-pod.y.n", "d-node.y.n", "d-node.y.n", PodDiskReq.PodDiskReqHdd),
    "e-pod": PodInfo("e-pod.y.n", "e-node.y.n", "e-node.y.n", PodDiskReq.PodDiskReqHdd),
    "f-pod": PodInfo("f-pod.y.n", "", "f-node.y.n", PodDiskReq.PodDiskReqHdd),
    # "g-pod":
    # "h-pod":
    "i-pod": PodInfo("i-pod.y.n", "", "i-node.y.n", PodDiskReq.PodDiskReqHdd),
    "j-pod": PodInfo("j-pod.y.n", "j-node.y.n", "j-node.y.n", PodDiskReq.PodDiskReqHdd),
}
nanny_instances = [
    "a-pod",
    "b-pod",
    # "c-pod",
    "d-pod",
    "e-pod",
    "f-pod",
    "g-pod",
    # "h-pod",
    # "i-pod",
    "j-pod",
]
yp_nodes_resources = {
    "a-pod": {"hdd"},
    "b-pod": {"ssd"},
    "c-pod": {"hdd", "ssd"},
    "d-pod": {"other"},
    "e-pod": {},
    "f-pod": {},
    # "g-pod": {},
    # "h-pod": {},
    "i-pod": {},
    # "j-pod": {},
}


expected_pods_data = [
    # Normal pod.
    PodData(yp_node_id="a-node.y.n", yp_pod_id="a-pod", yp_pod_fqdn="a-pod.y.n", nanny_instance_pod_id="a-pod",
            scheduling_hint_node_id="a-node.y.n", yp_pod_disk_req=PodDiskReq.PodDiskReqHdd, yp_node_hfsm_state="up",
            yp_node_matches_node_filter=True),
    # Pod without node, no such node, but pod is in deploy system. Nanny instance must be removed from deploy system.
    PodData(yp_pod_id="b-pod", yp_pod_fqdn="b-pod.y.n", nanny_instance_pod_id="b-pod",
            scheduling_hint_node_id="b-node.y.n", yp_pod_disk_req=PodDiskReq.PodDiskReqHdd,
            yp_node_matches_node_filter=False),
    # Pod not in Nanny instances list.
    PodData(yp_node_id="c-node.y.n", yp_pod_id="c-pod", yp_pod_fqdn="c-pod.y.n",
            scheduling_hint_node_id="c-node.y.n", yp_pod_disk_req=PodDiskReq.PodDiskReqHdd, yp_node_hfsm_state="up",
            yp_node_matches_node_filter=True),
    # Normal pods.
    PodData(yp_node_id="d-node.y.n", yp_pod_id="d-pod", yp_pod_fqdn="d-pod.y.n", nanny_instance_pod_id="d-pod",
            scheduling_hint_node_id="d-node.y.n", yp_pod_disk_req=PodDiskReq.PodDiskReqHdd, yp_node_hfsm_state="up",
            yp_node_matches_node_filter=True),
    PodData(yp_node_id="e-node.y.n", yp_pod_id="e-pod", yp_pod_fqdn="e-pod.y.n", nanny_instance_pod_id="e-pod",
            scheduling_hint_node_id="e-node.y.n", yp_pod_disk_req=PodDiskReq.PodDiskReqHdd, yp_node_hfsm_state="up",
            yp_node_matches_node_filter=True),
    # "f-pod.y.n" pod without node, node exists, pod must be left inact.
    PodData(yp_pod_id="f-pod", yp_pod_fqdn="f-pod.y.n", nanny_instance_pod_id="f-pod",
            scheduling_hint_node_id="f-node.y.n", yp_pod_disk_req=PodDiskReq.PodDiskReqHdd,
            yp_node_matches_node_filter=False),
    # "f-node.y.n" without pod.
    PodData(yp_node_id="f-node.y.n", yp_node_hfsm_state="up", yp_node_matches_node_filter=True),
    # Pod left only in Nanny instances list.
    PodData(nanny_instance_pod_id="g-pod"),
    # Node without pods.
    PodData(yp_node_id="h-node.y.n", yp_node_hfsm_state="up", yp_node_matches_node_filter=True),
    # Pod scheduled to non-existent node and not in deploy system. Pod must be removed.
    PodData(yp_pod_id="i-pod", yp_pod_fqdn="i-pod.y.n", scheduling_hint_node_id="i-node.y.n",
            yp_pod_disk_req=PodDiskReq.PodDiskReqHdd, yp_node_matches_node_filter=False),
    # NOTE(rocco66): node was moved to other segment, see TENTACLES-375
    PodData(yp_node_id='j-node.y.n', yp_pod_id="j-pod", yp_pod_fqdn="j-pod.y.n",
            scheduling_hint_node_id="j-node.y.n", nanny_instance_pod_id='j-pod',
            yp_pod_disk_req=PodDiskReq.PodDiskReqHdd, yp_node_matches_node_filter=False),
]


# noinspection PyProtectedMember
@freeze_time()
def test_pods_manager(monkeypatch: typing.Any, config_interface: ConfigInterface):
    def _mocked_fetch_yp_nodes(*args, **kwargs):
        return [(n, "up") for n in yp_nodes]

    def _mocked_fetch_pods(*args, **kwargs):
        return pods

    def _mocked_fetch_nanny_instances(*args, **kwargs):
        return nanny_instances

    def _mocked_fetch_yp_nodes_resources(*args, **kwargs):
        return yp_nodes_resources

    monkeypatch.setattr(YpLitePodsManager, "_fetch_pods", _mocked_fetch_pods)
    monkeypatch.setattr(YpLitePodsManager, "fetch_yp_nodes", _mocked_fetch_yp_nodes)
    monkeypatch.setattr(YpLitePodsManager, "_fetch_nanny_instances", _mocked_fetch_nanny_instances)
    monkeypatch.setattr(YpLitePodsManager, "_fetch_nanny_instances", _mocked_fetch_nanny_instances)
    monkeypatch.setattr(YpLitePodsManager, "_fetch_yp_nodes_resources", _mocked_fetch_yp_nodes_resources)

    with YpLitePodsManager(nanny_service_name="rtc_sla_tentacles_testing_yp_lite",
                           yp_cluster="FAKE",
                           logger=logging.getLogger(),
                           config_interface=config_interface) as pods_manager:

        pods_manager.load_data_from_api()
        for record in pods_manager._pods_data:
            assert record in expected_pods_data
        for record in expected_pods_data:
            assert record in pods_manager._pods_data

        assert set(pods_manager._get_actual_allocated_pods()) == {
            "a-pod",
            "c-pod",
            "d-pod",
            "e-pod",
        }
        assert set(pods_manager._get_nodes_without_pods()) == {
            "f-node.y.n",
            "h-node.y.n"
        }
        assert set(pods_manager._get_pods_scheduled_on_non_existing_nodes()) == {
            "i-pod",
            "j-pod",
        }
        assert set(pods_manager._get_nodes_without_pods_and_not_in_scheduling_hints()) == {
            "h-node.y.n",
        }
        assert pods_manager.get_stats() == {
            "data_freshness": int(time.time()),
            "pods": {
                "pods_seen": 8,
                "pods_allocated": 4,
                "pods_not_allocated": 3,
                "pods_in_deploy_system": 6,
                "pods_not_in_deploy_system": 2,
                "pods_allocated_and_in_deploy_system": 4,
                "pods_not_allocated_and_not_in_deploy_system": 1,
                "pods_scheduled_on_non_existing_nodes": 2,
                'incorrect_scheduling_hints': 1,
            },
            "nodes": {
                "nodes_seen": 7,
                "nodes_up_seen": 6,
                "nodes_without_pods": 2,
                "nodes_without_pods_and_not_in_scheduling_hints": 1,
            },
            "nanny_instances": {
                "nanny_instances_seen": 7,
                "nanny_instances_without_pods": 1,
            }
        }

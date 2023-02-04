import pytest
import time

from infra.horizontal_pod_autoscaler_controller.controller import HorizontalPodAutoscalerStandaloneController

SCALE_TIME_IN_SECONDS = 300


class HorizontalPodAutoscalerTest:
    def create_controller(self, yp_instance):
        raise NotImplementedError()

    def create_horizontal_pod_autoscaler(self, ctl_env, replica_set_id, lower_bound, upper_bound, now_time):
        raise NotImplementedError()

    def create_replica_set(self, ctl_env, account_id, replica_count, pods_total, ready_status):
        ctl_env.create_account(
            object_id=account_id,
            acl=[],
            labels={},
            spec={}
        )

        return ctl_env.create_replica_set(
            meta={},
            spec={
                "replica_count": replica_count,
                "pod_template_spec": {
                    "spec": {
                        "host_infra": {
                            "monitoring": {
                                "labels": {
                                    "stage": "test_stage",
                                    "deploy_unit": "DeployUnit1",
                                },
                            },
                        },
                    },
                },
                "account_id": account_id
            },
            status={
                "deploy_status": {
                    "details": {
                        "current_revision_progress": {
                            "pods_total": pods_total,
                        },
                    },
                },
                "ready_condition": {
                    "status": "true",
                },
            }
        )

    def test_sync_two_hpa_with_scale(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = self.create_controller(ctl_env.yp_instance)

        replica_set_id1 = self.create_replica_set(
            ctl_env=ctl_env,
            account_id="account_id1",
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        replica_set_id2 = self.create_replica_set(
            ctl_env=ctl_env,
            account_id="account_id2",
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        hpa_id1 = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id1,
            lower_bound=20.0,
            upper_bound=22.0,
            now_time={
                "seconds": int(time.time()) - SCALE_TIME_IN_SECONDS
            }
        )

        hpa_id2 = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id2,
            lower_bound=80.0,
            upper_bound=88.0,
            now_time={
                "seconds": int(time.time()) - SCALE_TIME_IN_SECONDS
            }
        )

        ctl_env.update_replica_set_horizontal_pod_autoscaler_id(
            replica_set_id=replica_set_id1,
            horizontal_pod_autoscaler_id=hpa_id1
        )

        ctl_env.update_replica_set_horizontal_pod_autoscaler_id(
            replica_set_id=replica_set_id2,
            horizontal_pod_autoscaler_id=hpa_id2
        )

        controller.sync()

        after = yp_client.select_objects("horizontal_pod_autoscaler", selectors=["/meta/id", "/status"])
        assert len(after) == 2
        for hpa in after:
            if hpa[0] == hpa_id1:
                assert hpa[1]["ready"]["status"] == "false"
                assert hpa[1]["ready"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["in_progress"]["status"] == "true"
                assert hpa[1]["in_progress"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["failed"]["status"] == "false"
                assert hpa[1]["failed"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["replica_set"]["current_replicas"] == 2L
                assert hpa[1]["replica_set"]["desired_replicas"] == 3L
                assert abs(hpa[1]["replica_set"]["metric_value"] - 42) < 0.001
                assert hpa[1]["replica_set"]["last_upscale_time"]["seconds"] > int(time.time()) - SCALE_TIME_IN_SECONDS // 2
                assert hpa[1]["spec_timestamp"] > 0
            elif hpa[0] == hpa_id2:
                assert hpa[1]["ready"]["status"] == "false"
                assert hpa[1]["ready"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["in_progress"]["status"] == "true"
                assert hpa[1]["in_progress"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["failed"]["status"] == "false"
                assert hpa[1]["failed"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["replica_set"]["current_replicas"] == 2L
                assert hpa[1]["replica_set"]["desired_replicas"] == 1L
                assert abs(hpa[1]["replica_set"]["metric_value"] - 42) < 0.001
                assert hpa[1]["replica_set"]["last_downscale_time"]["seconds"] > int(time.time()) - SCALE_TIME_IN_SECONDS // 2
                assert hpa[1]["spec_timestamp"] > 0
            else:
                raise "invalid hpa_id=%s", hpa[0]

        after = yp_client.select_objects("replica_set", selectors=["/meta/id", "/spec/replica_count"])
        assert len(after) == 2
        for replica_set in after:
            if replica_set[0] == replica_set_id1:
                assert replica_set[1] == 3L
            elif replica_set[0] == replica_set_id2:
                assert replica_set[1] == 1L
            else:
                raise "invalid replica_set_id=%s", replica_set[0]

    def test_sync_two_hpa_without_scale(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = self.create_controller(ctl_env.yp_instance)

        replica_set_id1 = self.create_replica_set(
            ctl_env=ctl_env,
            account_id="account_id1",
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        replica_set_id2 = self.create_replica_set(
            ctl_env=ctl_env,
            account_id="account_id2",
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        hpa_id1 = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id1,
            lower_bound=20.0,
            upper_bound=22.0,
            now_time={
                "seconds": int(time.time())
            }
        )

        hpa_id2 = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id2,
            lower_bound=80.0,
            upper_bound=88.0,
            now_time={
                "seconds": int(time.time())
            }
        )

        ctl_env.update_replica_set_horizontal_pod_autoscaler_id(
            replica_set_id=replica_set_id1,
            horizontal_pod_autoscaler_id=hpa_id1
        )

        ctl_env.update_replica_set_horizontal_pod_autoscaler_id(
            replica_set_id=replica_set_id2,
            horizontal_pod_autoscaler_id=hpa_id2
        )

        controller.sync()

        after = yp_client.select_objects("horizontal_pod_autoscaler", selectors=["/meta/id", "/status"])
        assert len(after) == 2
        for hpa in after:
            if hpa[0] == hpa_id1:
                assert hpa[1]["ready"]["status"] == "false"
                assert hpa[1]["ready"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["in_progress"]["status"] == "true"
                assert hpa[1]["in_progress"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["failed"]["status"] == "false"
                assert hpa[1]["failed"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["replica_set"]["current_replicas"] == 2L
                assert hpa[1]["replica_set"]["desired_replicas"] == 3L
                assert abs(hpa[1]["replica_set"]["metric_value"] - 42) < 0.001
                assert hpa[1]["spec_timestamp"] > 0
            elif hpa[0] == hpa_id2:
                assert hpa[1]["ready"]["status"] == "false"
                assert hpa[1]["ready"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["in_progress"]["status"] == "true"
                assert hpa[1]["in_progress"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["failed"]["status"] == "false"
                assert hpa[1]["failed"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
                assert hpa[1]["replica_set"]["current_replicas"] == 2L
                assert hpa[1]["replica_set"]["desired_replicas"] == 1L
                assert abs(hpa[1]["replica_set"]["metric_value"] - 42) < 0.001
                assert hpa[1]["spec_timestamp"] > 0
            else:
                raise "invalid hpa_id=%s", hpa[0]

        after = yp_client.select_objects("replica_set", selectors=["/meta/id", "/spec/replica_count"])
        assert len(after) == 2
        for replica_set in after:
            if replica_set[0] == replica_set_id1:
                assert replica_set[1] == 2L
            elif replica_set[0] == replica_set_id2:
                assert replica_set[1] == 2L
            else:
                raise "invalid replica_set_id=%s", replica_set[0]

    def test_simple_upscale(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = self.create_controller(ctl_env.yp_instance)

        replica_set_id = self.create_replica_set(
            account_id="account_id",
            ctl_env=ctl_env,
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        hpa_id = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id,
            lower_bound=20.0,
            upper_bound=22.0,
            now_time={
                "seconds": int(time.time()) - SCALE_TIME_IN_SECONDS
            }
        )

        ctl_env.update_replica_set_horizontal_pod_autoscaler_id(
            replica_set_id=replica_set_id,
            horizontal_pod_autoscaler_id=hpa_id
        )

        controller.sync()

        after = yp_client.select_objects("horizontal_pod_autoscaler", selectors=["/meta/id", "/status"])
        assert len(after) == 1
        assert after[0][0] == hpa_id
        assert after[0][1]["ready"]["status"] == "false"
        assert after[0][1]["ready"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["in_progress"]["status"] == "true"
        assert after[0][1]["in_progress"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["failed"]["status"] == "false"
        assert after[0][1]["failed"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["replica_set"]["current_replicas"] == 2L
        assert after[0][1]["replica_set"]["desired_replicas"] == 3L
        assert abs(after[0][1]["replica_set"]["metric_value"] - 42) < 0.001
        assert after[0][1]["replica_set"]["last_upscale_time"]["seconds"] > int(time.time()) - SCALE_TIME_IN_SECONDS // 2
        assert after[0][1]["spec_timestamp"] > 0

        after = yp_client.select_objects("replica_set", selectors=["/meta/id", "/spec/replica_count"])
        assert len(after) == 1
        assert after[0][0] == replica_set_id
        assert after[0][1] == 3L

    def test_simple_not_upscale(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = self.create_controller(ctl_env.yp_instance)

        replica_set_id = self.create_replica_set(
            account_id="account_id",
            ctl_env=ctl_env,
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        hpa_id = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id,
            lower_bound=20.0,
            upper_bound=22.0,
            now_time={
                "seconds": int(time.time())
            }
        )

        ctl_env.update_replica_set_horizontal_pod_autoscaler_id(
            replica_set_id=replica_set_id,
            horizontal_pod_autoscaler_id=hpa_id
        )

        controller.sync()

        after = yp_client.select_objects("horizontal_pod_autoscaler", selectors=["/meta/id", "/status"])
        assert len(after) == 1
        assert after[0][0] == hpa_id
        assert after[0][1]["ready"]["status"] == "false"
        assert after[0][1]["ready"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["in_progress"]["status"] == "true"
        assert after[0][1]["in_progress"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["failed"]["status"] == "false"
        assert after[0][1]["failed"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["replica_set"]["current_replicas"] == 2L
        assert after[0][1]["replica_set"]["desired_replicas"] == 3L
        assert abs(after[0][1]["replica_set"]["metric_value"] - 42) < 0.001
        assert after[0][1]["spec_timestamp"] > 0

        after = yp_client.select_objects("replica_set", selectors=["/meta/id", "/spec/replica_count"])
        assert len(after) == 1
        assert after[0][0] == replica_set_id
        assert after[0][1] == 2L

    def test_simple_not_upscale_in_dry_run_mode(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = self.create_controller(ctl_env.yp_instance)

        replica_set_id = self.create_replica_set(
            account_id="account_id",
            ctl_env=ctl_env,
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        hpa_id = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id,
            lower_bound=20.0,
            upper_bound=22.0,
            now_time={
                "seconds": int(time.time()) - SCALE_TIME_IN_SECONDS
            }
        )

        controller.sync()

        after = yp_client.select_objects("horizontal_pod_autoscaler", selectors=["/meta/id", "/status"])
        assert len(after) == 1
        assert after[0][0] == hpa_id
        assert after[0][1]["ready"]["status"] == "true"
        assert after[0][1]["ready"]["reason"] == "DRY_RUN_MODE"
        assert after[0][1]["in_progress"]["status"] == "false"
        assert after[0][1]["in_progress"]["reason"] == "DRY_RUN_MODE"
        assert after[0][1]["failed"]["status"] == "false"
        assert after[0][1]["failed"]["reason"] == "DRY_RUN_MODE"
        assert after[0][1]["replica_set"]["current_replicas"] == 3L
        assert after[0][1]["replica_set"]["desired_replicas"] == 3L
        assert abs(after[0][1]["replica_set"]["metric_value"] - 28) < 0.001
        assert after[0][1]["spec_timestamp"] > 0

        after = yp_client.select_objects("replica_set", selectors=["/meta/id", "/spec/replica_count"])
        assert len(after) == 1
        assert after[0][0] == replica_set_id
        assert after[0][1] == 2L

    def test_simple_downscale(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = self.create_controller(ctl_env.yp_instance)

        replica_set_id = self.create_replica_set(
            account_id="account_id",
            ctl_env=ctl_env,
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        hpa_id = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id,
            lower_bound=80.0,
            upper_bound=88.0,
            now_time={
                "seconds": int(time.time()) - SCALE_TIME_IN_SECONDS
            }
        )

        ctl_env.update_replica_set_horizontal_pod_autoscaler_id(
            replica_set_id=replica_set_id,
            horizontal_pod_autoscaler_id=hpa_id
        )

        controller.sync()

        after = yp_client.select_objects("horizontal_pod_autoscaler", selectors=["/meta/id", "/status"])
        assert len(after) == 1
        assert after[0][0] == hpa_id
        assert after[0][1]["ready"]["status"] == "false"
        assert after[0][1]["ready"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["in_progress"]["status"] == "true"
        assert after[0][1]["in_progress"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["failed"]["status"] == "false"
        assert after[0][1]["failed"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["replica_set"]["current_replicas"] == 2L
        assert after[0][1]["replica_set"]["desired_replicas"] == 1L
        assert abs(after[0][1]["replica_set"]["metric_value"] - 42) < 0.001
        assert after[0][1]["replica_set"]["last_downscale_time"]["seconds"] > int(time.time()) - SCALE_TIME_IN_SECONDS // 2
        assert after[0][1]["spec_timestamp"] > 0

        after = yp_client.select_objects("replica_set", selectors=["/meta/id", "/spec/replica_count"])
        assert len(after) == 1
        assert after[0][0] == replica_set_id
        assert after[0][1] == 1L

    def test_simple_not_downscale(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = self.create_controller(ctl_env.yp_instance)

        replica_set_id = self.create_replica_set(
            account_id="account_id",
            ctl_env=ctl_env,
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        hpa_id = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id,
            lower_bound=80.0,
            upper_bound=88.0,
            now_time={
                "seconds": int(time.time())
            }
        )

        ctl_env.update_replica_set_horizontal_pod_autoscaler_id(
            replica_set_id=replica_set_id,
            horizontal_pod_autoscaler_id=hpa_id
        )

        controller.sync()

        after = yp_client.select_objects("horizontal_pod_autoscaler", selectors=["/meta/id", "/status"])
        assert len(after) == 1
        assert after[0][0] == hpa_id
        assert after[0][1]["ready"]["status"] == "false"
        assert after[0][1]["ready"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["in_progress"]["status"] == "true"
        assert after[0][1]["in_progress"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["failed"]["status"] == "false"
        assert after[0][1]["failed"]["reason"] == "CURRENT_REPLICAS_NOT_EQUALS_DESIRED_REPLICAS"
        assert after[0][1]["replica_set"]["current_replicas"] == 2L
        assert after[0][1]["replica_set"]["desired_replicas"] == 1L
        assert abs(after[0][1]["replica_set"]["metric_value"] - 42) < 0.001
        assert after[0][1]["spec_timestamp"] > 0

        after = yp_client.select_objects("replica_set", selectors=["/meta/id", "/spec/replica_count"])
        assert len(after) == 1
        assert after[0][0] == replica_set_id
        assert after[0][1] == 2L

    def test_simple_not_downscale_in_dry_run_mode(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = self.create_controller(ctl_env.yp_instance)

        replica_set_id = self.create_replica_set(
            account_id="account_id",
            ctl_env=ctl_env,
            replica_count=2,
            pods_total=1,
            ready_status="true"
        )

        hpa_id = self.create_horizontal_pod_autoscaler(
            ctl_env=ctl_env,
            replica_set_id=replica_set_id,
            lower_bound=80.0,
            upper_bound=88.0,
            now_time={
                "seconds": int(time.time()) - SCALE_TIME_IN_SECONDS
            }
        )

        controller.sync()

        after = yp_client.select_objects("horizontal_pod_autoscaler", selectors=["/meta/id", "/status"])
        assert len(after) == 1
        assert after[0][0] == hpa_id
        assert after[0][1]["ready"]["status"] == "true"
        assert after[0][1]["ready"]["reason"] == "DRY_RUN_MODE"
        assert after[0][1]["in_progress"]["status"] == "false"
        assert after[0][1]["in_progress"]["reason"] == "DRY_RUN_MODE"
        assert after[0][1]["failed"]["status"] == "false"
        assert after[0][1]["failed"]["reason"] == "DRY_RUN_MODE"
        assert after[0][1]["replica_set"]["current_replicas"] == 1L
        assert after[0][1]["replica_set"]["desired_replicas"] == 1L
        assert abs(after[0][1]["replica_set"]["metric_value"] - 84) < 0.001
        assert after[0][1]["spec_timestamp"] > 0

        after = yp_client.select_objects("replica_set", selectors=["/meta/id", "/spec/replica_count"])
        assert len(after) == 1
        assert after[0][0] == replica_set_id
        assert after[0][1] == 2L


@pytest.mark.usefixtures("ctl_env")
class TestHorizontalPodAutoscalerControllerWithCpuMetric(HorizontalPodAutoscalerTest):
    def create_controller(self, yp_instance):
        return HorizontalPodAutoscalerStandaloneController(
            {
                "Controller": {
                    "YpClient": {
                        "Address": yp_instance.yp_client_grpc_address,
                        "EnableSsl": False
                    },
                    "LeadingInvader": {
                        "Path": "//home",
                        "Proxy": yp_instance.create_yt_client().config["proxy"]["url"]
                    }
                }
            },
            "{\"status\":\"ok\",\"response\":{\"signal\":{\"content\":{\"values\":{\"itype=deploy;geo=cluster;deploy_unit=DeployUnit1;stage=test_stage:havg(portoinst-cpu_usage_slot_hgram)\":[42]}}}}}"
        )

    def create_horizontal_pod_autoscaler(self, ctl_env, replica_set_id, lower_bound, upper_bound, now_time):
        return ctl_env.create_horizontal_pod_autoscaler(
            meta={
                "replica_set_id": replica_set_id,
            },
            spec={
                "replica_set": {
                    "min_replicas": 1,
                    "max_replicas": 3,
                    "cpu": {
                        "lower_bound": lower_bound,
                        "upper_bound": upper_bound,
                    },
                    "upscale_delay": {
                        "seconds": SCALE_TIME_IN_SECONDS,
                    },
                    "downscale_delay": {
                        "seconds": SCALE_TIME_IN_SECONDS,
                    },
                },
            },
            status={
                "ready": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "true",
                    "last_transition_time": now_time,
                },
                "in_progress": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "false",
                    "last_transition_time": now_time,
                },
                "failed": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "false",
                    "last_transition_time": now_time,
                },
                "replica_set": {
                    "last_upscale_time": now_time,
                    "last_downscale_time": now_time
                }
            }
        )


@pytest.mark.usefixtures("ctl_env")
class TestHorizontalPodAutoscalerControllerWithCpuUtilizationMetric(HorizontalPodAutoscalerTest):
    def create_controller(self, yp_instance):
        return HorizontalPodAutoscalerStandaloneController(
            {
                "Controller": {
                    "YpClient": {
                        "Address": yp_instance.yp_client_grpc_address,
                        "EnableSsl": False
                    },
                    "LeadingInvader": {
                        "Path": "//home",
                        "Proxy": yp_instance.create_yt_client().config["proxy"]["url"]
                    }
                }
            },
            "{\"status\":\"ok\",\"response\":{\"signal\":{\"content\":{\"values\":{\"itype=deploy;\
geo=cluster;deploy_unit=DeployUnit1;stage=test_stage:havg(portoinst-cpu_limit_usage_perc_hgram)\":[42]}}}}}"
        )

    def create_horizontal_pod_autoscaler(self, ctl_env, replica_set_id, lower_bound, upper_bound, now_time):
        return ctl_env.create_horizontal_pod_autoscaler(
            meta={
                "replica_set_id": replica_set_id,
            },
            spec={
                "replica_set": {
                    "min_replicas": 1,
                    "max_replicas": 3,
                    "cpu_utilization": {
                        "lower_bound_percent": int(lower_bound),
                        "upper_bound_percent": int(upper_bound),
                    },
                    "upscale_delay": {
                        "seconds": SCALE_TIME_IN_SECONDS,
                    },
                    "downscale_delay": {
                        "seconds": SCALE_TIME_IN_SECONDS,
                    },
                },
            },
            status={
                "ready": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "true",
                    "last_transition_time": now_time,
                },
                "in_progress": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "false",
                    "last_transition_time": now_time,
                },
                "failed": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "false",
                    "last_transition_time": now_time,
                },
                "replica_set": {
                    "last_upscale_time": now_time,
                    "last_downscale_time": now_time
                }
            }
        )


@pytest.mark.usefixtures("ctl_env")
class TestHorizontalPodAutoscalerControllerWithCustomGolovanMetric(HorizontalPodAutoscalerTest):
    def create_controller(self, yp_instance):
        return HorizontalPodAutoscalerStandaloneController(
            {
                "Controller": {
                    "YpClient": {
                        "Address": yp_instance.yp_client_grpc_address,
                        "EnableSsl": False
                    },
                    "LeadingInvader": {
                        "Path": "//home",
                        "Proxy": yp_instance.create_yt_client().config["proxy"]["url"]
                    }
                }
            },
            "{\"status\":\"ok\",\"response\":{\"signal\":{\"content\":{\"values\":\
            {\"itype=deploy;mytag=value;deploy_unit=DeployUnit1;stage=test_stage:havg(portoinst-memory_limit_usage_perc_hgram)\":[42]}}}}}"
        )

    def create_horizontal_pod_autoscaler(self, ctl_env, replica_set_id, lower_bound, upper_bound, now_time):
        return ctl_env.create_horizontal_pod_autoscaler(
            meta={
                "replica_set_id": replica_set_id,
            },
            spec={
                "replica_set": {
                    "min_replicas": 1,
                    "max_replicas": 3,
                    "custom": {
                        "golovan_signal": {
                            "signal": "havg(portoinst-memory_limit_usage_perc_hgram)",
                            "tags": {
                                "mytag": "value",
                                "itype": "deploy",
                                "deploy_unit": "DeployUnit1",
                                "stage": "test_stage"
                            }
                        },
                        "lower_bound": lower_bound,
                        "upper_bound": upper_bound,
                    },
                    "upscale_delay": {
                        "seconds": SCALE_TIME_IN_SECONDS,
                    },
                    "downscale_delay": {
                        "seconds": SCALE_TIME_IN_SECONDS,
                    },
                },
            },
            status={
                "ready": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "true",
                    "last_transition_time": now_time,
                },
                "in_progress": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "false",
                    "last_transition_time": now_time,
                },
                "failed": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "false",
                    "last_transition_time": now_time,
                },
                "replica_set": {
                    "last_upscale_time": now_time,
                    "last_downscale_time": now_time
                }
            }
        )


@pytest.mark.usefixtures("ctl_env")
class TestHorizontalPodAutoscalerControllerWithCustomSolomonMetric(HorizontalPodAutoscalerTest):
    def create_controller(self, yp_instance):
        return HorizontalPodAutoscalerStandaloneController(
            {
                "Controller": {
                    "YpClient": {
                        "Address": yp_instance.yp_client_grpc_address,
                        "EnableSsl": False
                    },
                    "LeadingInvader": {
                        "Path": "//home",
                        "Proxy": yp_instance.create_yt_client().config["proxy"]["url"]
                    }
                }
            },
            "{\"status\":\"ok\",\"response\":{\"signal\":{\"content\":{\"values\":\
            {\"itype=deploy;mytag=value;deploy_unit=DeployUnit1;stage=test_stage:havg(portoinst-memory_limit_usage_perc_hgram)\":[42]}}}}}"
        )

    def create_horizontal_pod_autoscaler(self, ctl_env, replica_set_id, lower_bound, upper_bound, now_time):
        return ctl_env.create_horizontal_pod_autoscaler(
            meta={
                "replica_set_id": replica_set_id,
            },
            spec={
                "replica_set": {
                    "min_replicas": 1,
                    "max_replicas": 3,
                    "custom": {
                        "solomon_signal": {
                            "project": "horizontal_pod_autoscaler_controller",
                            "service": "man_yp",
                            "cluster": "man_yp",
                            "host": "cluster",
                            "sensor": "horizontal_pod_autoscaler_controller.hpa_ctl.yanddmi-test-stage.rs.current_replicas",
                            "period": {
                                "seconds": 5 * 60
                            },
                            "aggregation": "avg"
                        },
                        "lower_bound": lower_bound,
                        "upper_bound": upper_bound,
                    },
                    "upscale_delay": {
                        "seconds": SCALE_TIME_IN_SECONDS,
                    },
                    "downscale_delay": {
                        "seconds": SCALE_TIME_IN_SECONDS,
                    },
                },
            },
            status={
                "ready": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "true",
                    "last_transition_time": now_time,
                },
                "in_progress": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "false",
                    "last_transition_time": now_time,
                },
                "failed": {
                    "reason": "DEFAULT",
                    "message": "Test",
                    "status": "false",
                    "last_transition_time": now_time,
                },
                "replica_set": {
                    "last_upscale_time": now_time,
                    "last_downscale_time": now_time
                }
            }
        )

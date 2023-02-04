import pytest
import time

from infra.resource_cache_controller.controller import StandaloneController


def create_controller(yp_instance):
    return StandaloneController({
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
    })


@pytest.mark.usefixtures("ctl_env")
class TestResourceCacheManagerFactory(object):
    def test_sync_simple(self, ctl_env):
        # TODO(YP-2430) remove sleep
        time.sleep(10)

        yp_client = ctl_env.yp_client
        controller = create_controller(ctl_env.yp_instance)

        node_id = ctl_env.create_node()

        pod_set_id1 = yp_client.create_object("pod_set")
        pod_set_id2 = yp_client.create_object("pod_set")

        pod_id1 = ctl_env.create_pod(node_id, pod_set_id1)
        pod_id2 = ctl_env.create_pod(node_id, pod_set_id1)
        pod_id3 = ctl_env.create_pod(node_id, pod_set_id2)
        pod_id4 = ctl_env.create_pod(node_id, pod_set_id2)

        resource_cache_id1 = ctl_env.create_resource_cache(pod_set_id1)
        resource_cache_id2 = ctl_env.create_resource_cache(pod_set_id2)

        resource_cache_spec1 = {
            "revision": 42,
            "cached_resources": [
                {
                    "id": "my_layer",
                    "layer": {
                        "url": "my_layer_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                },
                {
                    "id": "my_static_resource",
                    "static_resource": {
                        "url": "my_static_resource_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                }
            ]
        }

        resource_cache_spec2 = {
            "revision": 43,
            "cached_resources": [
                {
                    "id": "my_layer",
                    "layer": {
                        "url": "other_my_layer_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                },
                {
                    "id": "my_static_resource",
                    "static_resource": {
                        "url": "other_my_static_resource_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                },
                {
                    "id": "my_layer2",
                    "layer": {
                        "id": "my_layer2_id",
                        "url": "my_layer2_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                },
                {
                    "id": "my_static_resource2",
                    "static_resource": {
                        "id": "my_static_resource2_id",
                        "url": "my_static_resource2_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                }
            ]
        }

        # test that id will be cleaned
        assert resource_cache_spec2["cached_resources"][2]["id"] != resource_cache_spec2["cached_resources"][2]["layer"]["id"]
        assert resource_cache_spec2["cached_resources"][3]["id"] != resource_cache_spec2["cached_resources"][3]["static_resource"]["id"]

        controller.sync()

        resource_caches_empty = yp_client.select_objects("resource_cache", selectors=["/meta/id", "/spec", "/status"])
        assert len(resource_caches_empty) == 2
        for resource_cache in resource_caches_empty:
            assert resource_cache[1] == {}

            assert resource_cache[2]["all_ready"]["condition"]["status"] == "true"
            assert resource_cache[2]["latest_ready"]["condition"]["status"] == "true"

            assert resource_cache[2]["all_in_progress"]["condition"]["status"] == "false"
            assert resource_cache[2]["latest_in_progress"]["condition"]["status"] == "false"

            assert resource_cache[2]["all_failed"]["condition"]["status"] == "false"
            assert resource_cache[2]["latest_failed"]["condition"]["status"] == "false"

            assert resource_cache[2].get("cached_resource_status", None) is None

        pods_empty = yp_client.select_objects("pod", selectors=["/meta/id", "/spec/resource_cache"])
        assert len(pods_empty) == 4
        for pod in pods_empty:
            assert pod[0] in {pod_id1, pod_id2, pod_id3, pod_id4}
            assert pod[1]["spec"] == {}

        yp_client.update_object("resource_cache", resource_cache_id1, [
            {
                "path": "/spec",
                "value": resource_cache_spec1
            }
        ])

        controller.sync()

        resource_caches_spec1 = yp_client.select_objects("resource_cache", selectors=["/meta/id", "/spec", "/status"])
        assert len(resource_caches_spec1) == 2
        for resource_cache in resource_caches_spec1:
            if resource_cache[0] == resource_cache_id1:
                assert resource_cache[1] == resource_cache_spec1
                assert resource_cache[2]["revision"] == resource_cache_spec1["revision"]

                assert resource_cache[2]["all_ready"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_ready"]["condition"]["status"] == "false"

                assert resource_cache[2]["all_in_progress"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_in_progress"]["condition"]["status"] == "false"

                assert resource_cache[2]["all_failed"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_failed"]["condition"]["status"] == "false"

                assert len(resource_cache[2]["cached_resource_status"]) == 2
                assert {resource_cache[2]["cached_resource_status"][0]["id"], resource_cache[2]["cached_resource_status"][1]["id"]} == {"my_layer", "my_static_resource"}
                assert len(resource_cache[2]["cached_resource_status"][0]["revisions"]) == 1
                assert len(resource_cache[2]["cached_resource_status"][1]["revisions"]) == 1
            else:
                assert resource_cache[1] == {}

                assert resource_cache[2]["all_ready"]["condition"]["status"] == "true"
                assert resource_cache[2]["latest_ready"]["condition"]["status"] == "true"

                assert resource_cache[2]["all_in_progress"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_in_progress"]["condition"]["status"] == "false"

                assert resource_cache[2]["all_failed"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_failed"]["condition"]["status"] == "false"

                assert resource_cache[2].get("cached_resource_status", None) is None

        pods_spec1 = yp_client.select_objects("pod", selectors=["/meta/id", "/spec/resource_cache"])
        assert len(pods_spec1) == 4
        for pod in pods_spec1:
            assert pod[0] in {pod_id1, pod_id2, pod_id3, pod_id4}
            if pod[0] in {pod_id1, pod_id2}:
                assert len(pod[1]["spec"]["layers"]) == 1
                assert pod[1]["spec"]["layers"][0]["revision"] == resource_cache_spec1["revision"]
                assert pod[1]["spec"]["layers"][0]["layer"]["id"] == resource_cache_spec1["cached_resources"][0]["id"]
                assert pod[1]["spec"]["layers"][0]["layer"]["url"] == resource_cache_spec1["cached_resources"][0]["layer"]["url"]
                assert len(pod[1]["spec"]["static_resources"]) == 1
                assert pod[1]["spec"]["static_resources"][0]["revision"] == resource_cache_spec1["revision"]
                assert pod[1]["spec"]["static_resources"][0]["resource"]["id"] == resource_cache_spec1["cached_resources"][1]["id"]
                assert pod[1]["spec"]["static_resources"][0]["resource"]["url"] == resource_cache_spec1["cached_resources"][1]["static_resource"]["url"]
            else:
                assert pod[1]["spec"] == {}

        yp_client.update_object("resource_cache", resource_cache_id1, [
            {
                "path": "/spec",
                "value": resource_cache_spec2
            }
        ])

        yp_client.update_object("resource_cache", resource_cache_id2, [
            {
                "path": "/spec",
                "value": resource_cache_spec1
            }
        ])

        controller.sync()

        resource_caches_spec2 = yp_client.select_objects("resource_cache", selectors=["/meta/id", "/spec", "/status"])
        assert len(resource_caches_spec2) == 2
        for resource_cache in resource_caches_spec2:
            if resource_cache[0] == resource_cache_id1:
                assert resource_cache[1] == resource_cache_spec2
                assert resource_cache[2]["revision"] == resource_cache_spec2["revision"]

                assert resource_cache[2]["all_ready"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_ready"]["condition"]["status"] == "false"

                assert resource_cache[2]["all_in_progress"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_in_progress"]["condition"]["status"] == "false"

                assert resource_cache[2]["all_failed"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_failed"]["condition"]["status"] == "false"

                assert len(resource_cache[2]["cached_resource_status"]) == 4
                assert {resource_cache[2]["cached_resource_status"][i]["id"] for i in range(4)} == {"my_layer", "my_static_resource", "my_layer2", "my_static_resource2"}
                for i in range(4):
                    if resource_cache[2]["cached_resource_status"][i]["id"] in {"my_layer", "my_static_resource"}:
                        assert len(resource_cache[2]["cached_resource_status"][i]["revisions"]) == 2
                    else:
                        assert len(resource_cache[2]["cached_resource_status"][i]["revisions"]) == 1
            else:
                assert resource_cache[1] == resource_cache_spec1
                assert resource_cache[2]["revision"] == resource_cache_spec1["revision"]

                assert resource_cache[2]["all_ready"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_ready"]["condition"]["status"] == "false"

                assert resource_cache[2]["all_in_progress"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_in_progress"]["condition"]["status"] == "false"

                assert resource_cache[2]["all_failed"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_failed"]["condition"]["status"] == "false"

                assert len(resource_cache[2]["cached_resource_status"]) == 2
                assert {resource_cache[2]["cached_resource_status"][0]["id"], resource_cache[2]["cached_resource_status"][1]["id"]} == {"my_layer", "my_static_resource"}
                assert len(resource_cache[2]["cached_resource_status"][0]["revisions"]) == 1
                assert len(resource_cache[2]["cached_resource_status"][1]["revisions"]) == 1

        pods_spec2 = yp_client.select_objects("pod", selectors=["/meta/id", "/spec/resource_cache"])
        assert len(pods_spec2) == 4
        for pod in pods_spec2:
            assert pod[0] in {pod_id1, pod_id2, pod_id3, pod_id4}
            if pod[0] in {pod_id1, pod_id2}:
                assert len(pod[1]["spec"]["layers"]) == 3
                assert pod[1]["spec"]["layers"][0]["revision"] == resource_cache_spec1["revision"]
                assert pod[1]["spec"]["layers"][0]["layer"]["id"] == resource_cache_spec1["cached_resources"][0]["id"]
                assert pod[1]["spec"]["layers"][0]["layer"]["url"] == resource_cache_spec1["cached_resources"][0]["layer"]["url"]
                assert pod[1]["spec"]["layers"][1]["revision"] == resource_cache_spec2["revision"]
                assert pod[1]["spec"]["layers"][1]["layer"]["id"] == resource_cache_spec2["cached_resources"][0]["id"]
                assert pod[1]["spec"]["layers"][1]["layer"]["url"] == resource_cache_spec2["cached_resources"][0]["layer"]["url"]
                assert pod[1]["spec"]["layers"][2]["revision"] == resource_cache_spec2["revision"]
                assert pod[1]["spec"]["layers"][2]["layer"]["id"] == resource_cache_spec2["cached_resources"][2]["id"]
                assert pod[1]["spec"]["layers"][2]["layer"]["url"] == resource_cache_spec2["cached_resources"][2]["layer"]["url"]

                assert len(pod[1]["spec"]["static_resources"]) == 3
                assert pod[1]["spec"]["static_resources"][0]["revision"] == resource_cache_spec1["revision"]
                assert pod[1]["spec"]["static_resources"][0]["resource"]["id"] == resource_cache_spec1["cached_resources"][1]["id"]
                assert pod[1]["spec"]["static_resources"][0]["resource"]["url"] == resource_cache_spec1["cached_resources"][1]["static_resource"]["url"]
                assert pod[1]["spec"]["static_resources"][1]["revision"] == resource_cache_spec2["revision"]
                assert pod[1]["spec"]["static_resources"][1]["resource"]["id"] == resource_cache_spec2["cached_resources"][1]["id"]
                assert pod[1]["spec"]["static_resources"][1]["resource"]["url"] == resource_cache_spec2["cached_resources"][1]["static_resource"]["url"]
                assert pod[1]["spec"]["static_resources"][2]["revision"] == resource_cache_spec2["revision"]
                assert pod[1]["spec"]["static_resources"][2]["resource"]["id"] == resource_cache_spec2["cached_resources"][3]["id"]
                assert pod[1]["spec"]["static_resources"][2]["resource"]["url"] == resource_cache_spec2["cached_resources"][3]["static_resource"]["url"]
            else:
                assert len(pod[1]["spec"]["layers"]) == 1
                assert pod[1]["spec"]["layers"][0]["revision"] == resource_cache_spec1["revision"]
                assert pod[1]["spec"]["layers"][0]["layer"]["id"] == resource_cache_spec1["cached_resources"][0]["id"]
                assert pod[1]["spec"]["layers"][0]["layer"]["url"] == resource_cache_spec1["cached_resources"][0]["layer"]["url"]
                assert len(pod[1]["spec"]["static_resources"]) == 1
                assert pod[1]["spec"]["static_resources"][0]["revision"] == resource_cache_spec1["revision"]
                assert pod[1]["spec"]["static_resources"][0]["resource"]["id"] == resource_cache_spec1["cached_resources"][1]["id"]
                assert pod[1]["spec"]["static_resources"][0]["resource"]["url"] == resource_cache_spec1["cached_resources"][1]["static_resource"]["url"]

    def test_sync_with_conditions(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = create_controller(ctl_env.yp_instance)

        node_id = ctl_env.create_node()

        pod_set_id1 = yp_client.create_object("pod_set")
        pod_set_id2 = yp_client.create_object("pod_set")

        number_of_ready_pods = 7
        number_of_in_progress_pods = 8
        number_of_failed_pods = 5
        pod_set1_ready_pods = []
        pod_set1_in_progress_pods = []
        pod_set1_failed_pods = []
        pod_set2_ready_pods = []
        pod_set2_in_progress_pods = []
        pod_set2_failed_pods = []
        for i in range(number_of_ready_pods):
            pod_set1_ready_pods.append(ctl_env.create_pod(node_id, pod_set_id1))
            pod_set2_ready_pods.append(ctl_env.create_pod(node_id, pod_set_id2))
        for i in range(number_of_in_progress_pods):
            pod_set1_in_progress_pods.append(ctl_env.create_pod(node_id, pod_set_id1))
            pod_set2_in_progress_pods.append(ctl_env.create_pod(node_id, pod_set_id2))
        for i in range(number_of_failed_pods):
            pod_set1_failed_pods.append(ctl_env.create_pod(node_id, pod_set_id1))
            pod_set2_failed_pods.append(ctl_env.create_pod(node_id, pod_set_id2))

        resource_cache_id1 = ctl_env.create_resource_cache(pod_set_id1)
        resource_cache_id2 = ctl_env.create_resource_cache(pod_set_id2)

        resource_cache_spec = {
            "revision": 42,
            "cached_resources": [
                {
                    "id": "my_layer",
                    "layer": {
                        "url": "my_layer_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                },
                {
                    "id": "my_static_resource",
                    "static_resource": {
                        "url": "my_static_resource_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                }
            ]
        }

        ready_status = {
            "status": {
                "resource_cache": {
                    "layers": [
                        {
                            "id": "my_layer",
                            "revision": 42,
                            "ready": {
                                "status": "true"
                            },
                            "in_progress": {
                                "status": "false"
                            },
                            "failed": {
                                "status": "false"
                            }
                        }
                    ],
                    "static_resources": [
                        {
                            "id": "my_static_resource",
                            "revision": 42,
                            "ready": {
                                "status": "true"
                            },
                            "in_progress": {
                                "status": "false"
                            },
                            "failed": {
                                "status": "false"
                            }
                        }
                    ]
                }
            }
        }

        in_progress_status = {
            "status": {
                "resource_cache": {
                    "layers": [
                        {
                            "id": "my_layer",
                            "revision": 42,
                            "ready": {
                                "status": "false"
                            },
                            "in_progress": {
                                "status": "true"
                            },
                            "failed": {
                                "status": "false"
                            }
                        }
                    ],
                    "static_resources": [
                        {
                            "id": "my_static_resource",
                            "revision": 42,
                            "ready": {
                                "status": "false"
                            },
                            "in_progress": {
                                "status": "true"
                            },
                            "failed": {
                                "status": "false"
                            }
                        }
                    ]
                }
            }
        }

        failed_status = {
            "status": {
                "resource_cache": {
                    "layers": [
                        {
                            "id": "my_layer",
                            "revision": 42,
                            "ready": {
                                "status": "false"
                            },
                            "in_progress": {
                                "status": "false"
                            },
                            "failed": {
                                "status": "true"
                            }
                        }
                    ],
                    "static_resources": [
                        {
                            "id": "my_static_resource",
                            "revision": 42,
                            "ready": {
                                "status": "false"
                            },
                            "in_progress": {
                                "status": "false"
                            },
                            "failed": {
                                "status": "true"
                            }
                        }
                    ]
                }
            }
        }

        yp_client.update_object("resource_cache", resource_cache_id1, [
            {
                "path": "/spec",
                "value": resource_cache_spec
            }
        ])

        for i in range(number_of_ready_pods):
            yp_client.update_object("pod", pod_set1_ready_pods[i], [
                {
                    "path": "/status/agent/pod_agent_payload",
                    "value": ready_status
                }
            ])
            yp_client.update_object("pod", pod_set2_ready_pods[i], [
                {
                    "path": "/status/agent/pod_agent_payload",
                    "value": ready_status
                }
            ])

        for i in range(number_of_in_progress_pods):
            yp_client.update_object("pod", pod_set1_in_progress_pods[i], [
                {
                    "path": "/status/agent/pod_agent_payload",
                    "value": in_progress_status
                }
            ])
            yp_client.update_object("pod", pod_set2_in_progress_pods[i], [
                {
                    "path": "/status/agent/pod_agent_payload",
                    "value": in_progress_status
                }
            ])

        for i in range(number_of_failed_pods):
            yp_client.update_object("pod", pod_set1_failed_pods[i], [
                {
                    "path": "/status/agent/pod_agent_payload",
                    "value": failed_status
                }
            ])
            yp_client.update_object("pod", pod_set2_failed_pods[i], [
                {
                    "path": "/status/agent/pod_agent_payload",
                    "value": failed_status
                }
            ])

        controller.sync()

        resource_caches_spec_status = yp_client.select_objects("resource_cache", selectors=["/meta/id", "/spec", "/status"])
        assert len(resource_caches_spec_status) == 2
        assert {resource_caches_spec_status[0][0], resource_caches_spec_status[1][0]} == {resource_cache_id1, resource_cache_id2}
        for resource_cache in resource_caches_spec_status:
            if resource_cache[0] == resource_cache_id1:
                assert resource_cache[1] == resource_cache_spec
                assert resource_cache[2]["revision"] == resource_cache_spec["revision"]

                assert resource_cache[2]["all_ready"]["condition"]["status"] == "false"
                assert resource_cache[2]["all_ready"].get("pod_count", 0) == number_of_ready_pods
                assert resource_cache[2]["latest_ready"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_ready"].get("pod_count", 0) == number_of_ready_pods

                assert resource_cache[2]["all_in_progress"]["condition"]["status"] == "true"
                assert resource_cache[2]["all_in_progress"].get("pod_count", 0) == number_of_in_progress_pods
                assert resource_cache[2]["latest_in_progress"]["condition"]["status"] == "true"
                assert resource_cache[2]["latest_in_progress"].get("pod_count", 0) == number_of_in_progress_pods

                assert resource_cache[2]["all_failed"]["condition"]["status"] == "true"
                assert resource_cache[2]["all_failed"].get("pod_count", 0) == number_of_failed_pods
                assert resource_cache[2]["latest_failed"]["condition"]["status"] == "true"
                assert resource_cache[2]["latest_failed"].get("pod_count", 0) == number_of_failed_pods

                assert len(resource_cache[2]["cached_resource_status"]) == 2
                for i in range(2):
                    assert len(resource_cache[2]["cached_resource_status"][i]["revisions"]) == 1

                    assert resource_cache[2]["cached_resource_status"][i]["revisions"][0]["ready"]["condition"]["status"] == "false"
                    assert resource_cache[2]["cached_resource_status"][i]["revisions"][0]["ready"].get("pod_count", 0) == number_of_ready_pods

                    assert resource_cache[2]["cached_resource_status"][i]["revisions"][0]["in_progress"]["condition"]["status"] == "true"
                    assert resource_cache[2]["cached_resource_status"][i]["revisions"][0]["in_progress"].get("pod_count", 0) == number_of_in_progress_pods

                    assert resource_cache[2]["cached_resource_status"][i]["revisions"][0]["failed"]["condition"]["status"] == "true"
                    assert resource_cache[2]["cached_resource_status"][i]["revisions"][0]["failed"].get("pod_count", 0) == number_of_failed_pods
            else:
                assert resource_cache[1] == {}

                assert resource_cache[2]["all_ready"]["condition"]["status"] == "true"
                assert resource_cache[2]["all_ready"].get("pod_count", 0) == 20
                assert resource_cache[2]["latest_ready"]["condition"]["status"] == "true"
                assert resource_cache[2]["latest_ready"].get("pod_count", 0) == 20

                assert resource_cache[2]["all_in_progress"]["condition"]["status"] == "false"
                assert resource_cache[2]["all_in_progress"].get("pod_count", 0) == 0
                assert resource_cache[2]["latest_in_progress"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_in_progress"].get("pod_count", 0) == 0

                assert resource_cache[2]["all_failed"]["condition"]["status"] == "false"
                assert resource_cache[2]["all_failed"].get("pod_count", 0) == 0
                assert resource_cache[2]["latest_failed"]["condition"]["status"] == "false"
                assert resource_cache[2]["latest_failed"].get("pod_count", 0) == 0


@pytest.mark.usefixtures("ctl_env")
class TestResourceCacheGCManagerFactory(object):
    def test_sync(self, ctl_env):
        yp_client = ctl_env.yp_client
        controller = create_controller(ctl_env.yp_instance)

        node_id = ctl_env.create_node()

        pod_set_id1 = yp_client.create_object("pod_set")
        pod_set_id2 = yp_client.create_object("pod_set")

        pod_id1 = ctl_env.create_pod(node_id, pod_set_id1)
        pod_id2 = ctl_env.create_pod(node_id, pod_set_id2)

        resource_cache_id1 = ctl_env.create_resource_cache(pod_set_id1)
        resource_cache_id2 = ctl_env.create_resource_cache(pod_set_id2)

        resource_cache_spec = {
            "revision": 42,
            "cached_resources": [
                {
                    "id": "my_layer",
                    "layer": {
                        "url": "my_layer_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                },
                {
                    "id": "my_static_resource",
                    "static_resource": {
                        "url": "my_static_resource_url"
                    },
                    "basic_strategy": {
                        "max_latest_revisions": 5
                    }
                }
            ]
        }

        for resource_cache_id in {resource_cache_id1, resource_cache_id2}:
            yp_client.update_object("resource_cache", resource_cache_id, [
                {
                    "path": "/spec",
                    "value": resource_cache_spec
                }
            ])

        controller.sync()

        pods_spec1 = yp_client.select_objects("pod", selectors=["/meta/id", "/spec/resource_cache"])
        assert len(pods_spec1) == 2
        for pod in pods_spec1:
            assert pod[0] in {pod_id1, pod_id2}
            assert len(pod[1]["spec"]["layers"]) == 1
            assert len(pod[1]["spec"]["static_resources"]) == 1

        yp_client.remove_object("resource_cache", resource_cache_id2)

        controller.sync()

        pods_spec2 = yp_client.select_objects("pod", selectors=["/meta/id", "/spec/resource_cache"])
        assert len(pods_spec2) == 2
        for pod in pods_spec2:
            assert pod[0] in {pod_id1, pod_id2}
            if pod[0] == pod_id1:
                assert len(pod[1]["spec"]["layers"]) == 1
                assert len(pod[1]["spec"]["static_resources"]) == 1
            else:
                assert pod[1].get("spec", {}) == {}

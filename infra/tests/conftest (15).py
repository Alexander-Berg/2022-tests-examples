import pytest
import time
import yatest.common
import local_yp

from yp.common import wait
from yp.local import reset_yp, DEFAULT_IP4_ADDRESS_POOL_ID

from yt.wrapper.common import generate_uuid
from yt.wrapper.retries import run_with_retries
from yt.wrapper.errors import YtTabletTransactionLockConflict
from yp_proto.yp.client.api.proto import cluster_api_pb2


class TestEnv(object):
    def __init__(self, yp_instance):
        self.yp_instance = yp_instance
        self.yp_client = yp_instance.create_client()

    def create_node(self, id=None, spec=None, labels=None, with_resources=True):
        yp_client = self.yp_client

        # default
        attributes = {
            "spec": {
                "ip6_subnets": [
                    {"vlan_id": "backbone", "subnet": "1:2:3:4::/64"}
                ]
            },
            "labels": {
                "location": {
                    "line": "SAS"
                }
            }
        }

        if id:
            attributes["meta"] = {
                "id": id
            }
        if spec:
            attributes["spec"] = spec
        if labels:
            attributes["labels"] = labels

        node_id = yp_client.create_object("node", attributes=attributes)

        if with_resources:
            yp_client.create_object("resource", attributes={
                "meta": {"node_id": node_id},
                "spec": {
                    "cpu": {"total_capacity": 5000},
                }
            })

            yp_client.create_object("resource", attributes={
                "meta": {"node_id": node_id},
                "spec": {
                    "memory": {"total_capacity": 10 * 2**30}
                }
            })

            yp_client.create_object("network_project", attributes={
                "meta": {"id": "SOMENET"},
                "spec": {
                    "project_id": 123
                }
            })

            yp_client.create_object("resource", attributes={
                "meta": {"node_id": node_id},
                "spec": {
                    "slot": {"total_capacity": 300}
                }
            })

        return node_id

    class DummyPod(object):
        def __init__(self):
            self.id = None

        def create_dummy_pod(self, yp_client, node_id, pod_set_id, enable_internet, enable_dns):
            enable_scheduling = node_id is None
            spec = {
                "ip6_address_requests": [{
                    "vlan_id": "backbone",
                    "network_id": "SOMENET",
                    "enable_internet": enable_internet,
                    "enable_dns": enable_dns,
                }],
                "resource_requests": {
                    "vcpu_limit": 100,
                    "vcpu_guarantee": 100,
                    "memory_limit": 2000,
                    "memory_guarantee": 2000,
                },
                "enable_scheduling": enable_scheduling,
            }
            if not enable_scheduling:
                spec["node_id"] = node_id

            self.id = yp_client.create_object("pod", attributes={
                "meta": {
                    "pod_set_id": pod_set_id,
                },
                "spec": spec
            })

        def _upgrade_to_yplite_pod(self, yp_client, current_state, instances, create_iss_payload):
            spec = {
                "iss": {
                    "instances": instances,
                },
            }

            yp_client.update_object("pod", self.id, set_updates=[
                {
                    "path": "/status/agent/iss_payload",
                    "value": create_iss_payload(current_state),
                },
                {
                    "path": "/spec",
                    "value": spec,
                },
            ])

        def _upgrade_to_deploy_pod(self, yp_client, pod_target_state, ready, workloads):
            yp_client.update_object("pod", self.id, set_updates=[
                {
                    "path": "/status/agent/pod_agent_payload/status/ready/status",
                    "value": "true" if ready else "false",
                    "recursive": True
                },
                {
                    "path": "/status/agent/pod_agent_payload/status/workloads",
                    "value": workloads,
                    "recursive": True
                },
                {
                    "path": "/spec/pod_agent_payload/spec/target_state",
                    "value": pod_target_state,
                    "recursive": True
                },
                {
                    "path": "/spec/pod_agent_payload/meta",
                    "value": {},
                },
            ])

    def _create_dummy_pod(self, node_id, pod_set_id, enable_internet, enable_scheduling, enable_dns):
        pod = self.DummyPod()
        run_with_retries(
            lambda: pod.create_dummy_pod(self.yp_client,
                                         node_id,
                                         pod_set_id,
                                         enable_internet,
                                         enable_dns),
            exceptions=(YtTabletTransactionLockConflict,)
        )
        return pod

    # Parameters `node_id` and `enable_scheduling` are mutually exclusive. When `enable_scheduling`=True, `node_id` is ignored.
    def create_pod(self, node_id, pod_set_id, current_state="ACTIVE", instances=[], enable_internet=False, enable_scheduling=False, enable_dns=False):
        return self.create_yplite_pod(node_id, pod_set_id, current_state, instances, enable_internet, enable_scheduling, enable_dns).id

    def create_yplite_pod(self, node_id, pod_set_id, current_state="ACTIVE", instances=[], enable_internet=False, enable_scheduling=False, enable_dns=False):
        pod = self._create_dummy_pod(node_id, pod_set_id, enable_internet, enable_scheduling, enable_dns)

        run_with_retries(
            lambda: pod._upgrade_to_yplite_pod(self.yp_client,
                                               current_state,
                                               instances,
                                               lambda state: self.create_iss_payload(state)),
            exceptions=(YtTabletTransactionLockConflict,)
        )

        return pod

    def create_deploy_pod(self, node_id, pod_set_id, pod_target_state="active", ready=True, workloads=[{"target_state": "active"}]):
        pod = self._create_dummy_pod(node_id, pod_set_id, False, False, False)

        run_with_retries(
            lambda: pod._upgrade_to_deploy_pod(self.yp_client, pod_target_state, ready, workloads),
            exceptions=(YtTabletTransactionLockConflict,)
        )

        return pod

    def create_inet_addr(self, network_module_id, ip4_addr, ip4_pool=DEFAULT_IP4_ADDRESS_POOL_ID):
        yp_client = self.yp_client

        inet_addr_id = yp_client.create_object(
            object_type="internet_address",
            attributes={
                "meta": {
                    "ip4_address_pool_id": ip4_pool,
                },
                "spec": {
                    "ip4_address": ip4_addr,
                    "network_module_id": network_module_id,
                },
            }
        )
        return inet_addr_id

    def create_endpoint_set(self, filter, port, protocol="HTTP", client=None):
        endpoint_set_id = "endpoint-set-{}".format("%.20f" % time.time())

        if not client:
            return self.yp_client.create_object("endpoint_set", attributes={
                "spec": {
                    "pod_filter": filter,
                    "port": port,
                    "protocol": protocol,
                },
                "meta": {
                    "id": endpoint_set_id
                }
            })
        return client.create_object("endpoint_set", attributes={
            "spec": {
                "pod_filter": filter,
                "port": port,
                "protocol": protocol,
            },
            "meta": {
                "id": endpoint_set_id
            }
        })

    def create_resource_cache(self, pod_set_id):
        return self.yp_client.create_object("resource_cache", attributes={
            "meta": {
                "pod_set_id": pod_set_id
            }
        })

    def create_user(self, object_id, labels, ignore_existing=False, superuser=False, grant_permissions=None):
        if ignore_existing:
            user = self.yp_client.select_objects("user", selectors=["/meta/id"], filter="[/meta/id] = '{}'".format(object_id))
            if user:
                return object_id

        user = self.yp_client.create_object("user", attributes={
            "meta": {
                "id": object_id
            },
            "labels": labels
        })

        if grant_permissions:
            self.yp_client.update_objects(
                [
                    dict(
                        object_type="schema",
                        object_id=object_type,
                        set_updates=[
                            dict(
                                path="/meta/acl/end",
                                value=dict(
                                    action="allow",
                                    permissions=[permission],
                                    subjects=[object_id],
                                ),
                            ),
                        ],
                    )
                    for object_type, permission in grant_permissions
                ]
            )
            local_yp.sync_access_control(self.yp_instance)

        if superuser:
            self.yp_client.update_object(
                "group", "superusers", set_updates=[{"path": "/spec/members", "value": [object_id]}]
            )
            local_yp.sync_access_control(self.yp_instance)

        return user

    def create_group(self, object_id, labels, spec):
        return self.yp_client.create_object("group", attributes={
            "meta": {
                "id": object_id
            },
            "labels": labels,
            "spec": spec
        })

    def create_account(self, object_id, acl, labels, spec):
        return self.yp_client.create_object("account", attributes={
            "meta": {
                "id": object_id,
                "acl": acl
            },
            "labels": labels,
            "spec": spec
        })

    def create_network_project(self, object_id, acl, labels, spec):
        return self.yp_client.create_object("network_project", attributes={
            "meta": {
                "id": object_id,
                "acl": acl
            },
            "labels": labels,
            "spec": spec
        })

    def create_resource(self, meta, labels, spec):
        return self.yp_client.create_object("resource", attributes={
            "meta": meta,
            "labels": labels,
            "spec": spec,
        })

    def create_iss_payload(self, current_state="ACTIVE"):
        host_current_state = cluster_api_pb2.HostCurrentState()
        detailed_current_state = host_current_state.currentStates.add()
        detailed_current_state.currentState = current_state
        return host_current_state.SerializeToString()

    def wait_pods_scheduled_state(self, pod_id, state):
        yp_client = self.yp_client

        def func():
            if yp_client.get_object("pod", pod_id, selectors=["/status/scheduling/state"])[0] == state:
                return True
            return False

        wait(func)

    def create_virtual_service(self, object_id, labels, spec):
        return self.yp_client.create_object("virtual_service", attributes={
            "meta": {
                "id": object_id,
            },
            "labels": labels,
            "spec": spec
        })

    def create_node_segment(self, object_id):
        return self.yp_client.create_object("node_segment", attributes={
            "meta": {
                "id": object_id,
            },
            "spec": {
                "node_filter": "[/labels/segment] = '{}'".format(object_id)
            }
        })

    def create_horizontal_pod_autoscaler(self, meta, spec, status):
        return self.yp_client.create_object("horizontal_pod_autoscaler", attributes={
            "meta": meta,
            "spec": spec,
            "status": status,
        })

    def create_replica_set(self, meta, spec, status):
        return self.yp_client.create_object("replica_set", attributes={
            "meta": meta,
            "spec": spec,
            "status": status,
        })

    def update_replica_set_horizontal_pod_autoscaler_id(self, replica_set_id, horizontal_pod_autoscaler_id):
        return self.yp_client.update_object("replica_set", replica_set_id, set_updates=[
            {
                "path": "/labels",
                "value": {
                    "deploy": {
                        "horizontal_pod_autoscaler_id": horizontal_pod_autoscaler_id
                    }
                }
            }
        ])


def test_teardown(yp_instance):
    reset_yp(yp_instance.create_client())


@pytest.fixture(scope="session")
def yp_instance(request):
    YP_MASTER_CONFIG = {
        "watch_manager": {
            "query_selector_enabled_per_type": {
                "pod": True
            },
            "changed_attributes_paths_per_type": {
                "pod": [
                    "/status/agent/iss_summary",
                    "/status/agent/pod_agent_payload/status/ready/status",
                    "/status/ip6_address_allocations",
                    "/status/iss_conf_summaries",
                    "/spec/pod_agent_payload/spec/target_state",
                    "/labels/a",
                ]
            }
        },
        "object_manager": {
            "pod_type_handler": {
                "collect_iss_conf_summaries": True,
                "collect_iss_summary": True
            },
        }
    }

    yp_instance = local_yp.get_yp_instance(yatest.common.output_path(), 'yp_{}'.format(generate_uuid()), start_proxy=True, enable_ssl=True, yp_master_config=YP_MASTER_CONFIG)
    yp_instance.start()
    local_yp.sync_access_control(yp_instance)

    request.addfinalizer(lambda: yp_instance.stop())
    return yp_instance


@pytest.fixture(scope="function")
def ctl_env(request, yp_instance):
    request.addfinalizer(lambda: test_teardown(yp_instance))
    return TestEnv(yp_instance)

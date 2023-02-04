import re


def mock_select_objects(build_yp_client_mock, accounts=(), pod_sets=(), nodes=()):
    account_id_to_account = {account["id"]: account for account in accounts}
    node_id_to_node = {node["id"]: node for node in nodes}

    pod_set_by_id = {pod_set["id"]: pod_set for pod_set in pod_sets}

    pod_set_ids_by_account_segment = {}
    for pod_set in pod_sets:
        key = (pod_set["account_id"], pod_set["node_segment_id"])
        if key not in pod_set_ids_by_account_segment:
            pod_set_ids_by_account_segment[key] = []
        pod_set_ids_by_account_segment[key].append(pod_set["id"])

    account_filter_pattern = r"^\[/meta/id\] = \"(.*)\"$"
    pod_filter_pattern = r"^\[/meta/pod_set_id\] = \"(.*)\"$"
    pod_set_filter_pattern = r"^\(\[/spec/account_id\] = \"(.*)\"\) AND \(\[/spec/node_segment_id\] = \"(.*)\"\)$"
    resource_filter_pattern = r"^\(\[/meta/node_id\] = \"(.*)\"\)$"

    def select_objects(*args, **kwargs):
        object_type = args[0]
        selectors = kwargs["selectors"]
        filter_ = kwargs.get("filter")

        if object_type == "internet_address":
            assert filter_ is None
            if selectors == ["/meta/id", "/status/pod_id"]:
                response = []
                for pod_set in pod_sets:
                    for i in range(pod_set.get("usage", {}).get("ipv4", 0)):
                        response.append(["{}_{}".format(pod_set["id"], i), pod_set["id"]])
                return response

            assert selectors[:3] == ["/meta/id", "/spec/network_module_id", "/status/pod_id"]
            response = []
            for node in nodes:
                total_addresses = node.get("total_resources", {}).get("ipv4", 0)
                free_addresses = node.get("free_resources", {}).get("ipv4", 0)
                used_addresses = total_addresses - free_addresses

                for i in range(used_addresses):
                    id_ = "used_{}_{}".format(node["id"], i)
                    response.append([id_, "default", "fictional_pod"])

                for i in range(free_addresses):
                    id_ = "free_{}_{}".format(node["id"], i)
                    response.append([id_, "default", None])

            if len(selectors) > 3:
                assert selectors[3:] == ["/meta/id"]
                for row in response:
                    row.append(row[0])

            return response
        elif object_type == "pod_set":
            assert selectors == ["/meta/id"]

            match = re.match(pod_set_filter_pattern, filter_)
            assert match, "Failed to extract account id and node segment id from filter \"{}\"".format(filter_)

            account_id = match.group(1)
            node_segment_id = match.group(2)
            pod_set_ids = pod_set_ids_by_account_segment[(account_id, node_segment_id)]

            return [[pod_set_id] for pod_set_id in pod_set_ids]
        elif object_type == "pod":
            assert selectors == [
                "/meta/id",
                "/meta/pod_set_id",
                "/meta/account_id",
                "/spec/resource_requests",
                "/spec/disk_volume_requests",
                "/spec/gpu_requests",
            ]

            match = re.match(pod_filter_pattern, filter_)
            assert match, "Failed to extract pod set id from filter \"{}\"".format(filter_)

            pod_set_id = match.group(1)
            pod_set = pod_set_by_id.get(pod_set_id, {})
            usage = pod_set.get("usage", {})

            return [[
                pod_set["id"],
                pod_set["id"],
                "",
                dict(
                    vcpu_guarantee=usage.get("vcpu", 0),
                    memory_limit=usage.get("memory", 0),
                    network_bandwidth_guarantee=usage.get("net_bw", 0),
                ),
                [
                    dict(
                        storage_class="hdd",
                        quota_policy=dict(
                            capacity=usage.get("hdd", 0),
                            bandwidth_guarantee=usage.get("hdd_bw", 0),
                        ),
                    ),
                    dict(
                        storage_class="ssd",
                        quota_policy=dict(
                            capacity=usage.get("ssd", 0),
                            bandwidth_guarantee=usage.get("ssd_bw", 0),
                        ),
                    ),
                ],
                [],
            ]]
        elif object_type == "account":
            assert selectors == ["/spec", "/status"]

            match = re.match(account_filter_pattern, filter_)
            assert match, "Failed to extract account id from filter \"{}\"".format(filter_)

            account_id = match.group(1)
            account = account_id_to_account.get(account_id, {})
            limits_per_segment = account.get("limits_per_segment", {})
            usage_per_segment = account.get("usage_per_segment", {})

            spec = dict(
                resource_limits=dict(
                    per_segment={
                        node_segment_id: dict(
                            cpu=dict(capacity=limits.get("vcpu", 0)),
                            memory=dict(capacity=limits.get("memory", 0)),
                            internet_address=dict(capacity=limits.get("ipv4", 0)),
                            network=dict(bandwidth=limits.get("net_bw", 0)),
                            disk_per_storage_class=dict(
                                ssd=dict(
                                    capacity=limits.get("ssd", 0),
                                    bandwidth=limits.get("ssd_bw", 0),
                                ),
                                hdd=dict(
                                    capacity=limits.get("hdd", 0),
                                    bandwidth=limits.get("hdd_bw", 0),
                                ),
                            )
                        )
                        for node_segment_id, limits in limits_per_segment.items()
                    }
                )
            )

            status = dict(
                immediate_resource_usage=dict(
                    per_segment={
                        node_segment_id: dict(
                            cpu=dict(capacity=usage.get("vcpu", 0)),
                            memory=dict(capacity=usage.get("memory", 0)),
                            internet_address=dict(capacity=usage.get("ipv4", 0)),
                            network=dict(bandwidth=usage.get("net_bw", 0)),
                            disk_per_storage_class=dict(
                                ssd=dict(
                                    capacity=usage.get("ssd", 0),
                                    bandwidth=usage.get("ssd_bw", 0),
                                ),
                                hdd=dict(
                                    capacity=usage.get("hdd", 0),
                                    bandwidth=usage.get("hdd_bw", 0),
                                ),
                            )
                        )
                        for node_segment_id, usage in usage_per_segment.items()
                    }
                )
            )

            return [[spec, status]]
        elif object_type == "node":
            assert selectors[:4] == ["/meta/id", "/labels", "/spec/network_module_id", "/status/alerts/0"]
            response = [
                [
                    node["id"],
                    node.get("labels", dict(segment=node.get("segment", "default"))),
                    node.get("network_module_id"),
                    None,
                ]
                for node in nodes
            ]
            if len(selectors) > 4:
                assert selectors[4:] == ["/meta/id"]
                for row in response:
                    row.append(row[0])
            return response
        elif object_type == "resource":
            assert selectors == ["/meta/node_id", "/spec", "/status/free", "/meta/kind", "/status/used"]

            match = re.match(resource_filter_pattern, filter_)
            assert match, "Failed to extract node id from filter \"{}\"".format(filter_)

            node_id = match.group(1)
            node = node_id_to_node[node_id]
            total_resources = node["total_resources"]
            free_resources = node["free_resources"]

            resources = []
            for key, resource, unit in (
                ("vcpu", "cpu", "capacity"),
                ("memory", "memory", "capacity"),
                ("net_bw", "network", "bandwidth"),
            ):
                spec = {"total_" + unit: total_resources[key]}
                if resource == "cpu":
                    spec["cpu_to_vcpu_factor"] = 1.0
                resources.append([
                    node_id,
                    {resource: spec},
                    {resource: {unit: free_resources[key]}},
                    resource,
                    {
                        resource: {
                            unit: total_resources[key] - free_resources[key],
                        },
                    },
                ])

            for storage_class in ("hdd", "ssd"):
                for storage_provisioner in ("shared", "lvm"):
                    key_prefix = storage_class if storage_provisioner == "shared" else storage_class + "_" + storage_provisioner
                    capacity_key = key_prefix
                    bandwidth_key = key_prefix + "_bw"
                    resources.append([
                        node_id,
                        {
                            "disk": {
                                "total_capacity": total_resources[capacity_key],
                                "total_bandwidth": total_resources[bandwidth_key],
                                "storage_class": storage_class,
                                "storage_provisioner": storage_provisioner,
                            },
                        },
                        {
                            "disk": {
                                "capacity": free_resources[capacity_key],
                                "bandwidth": free_resources[bandwidth_key],
                            },
                        },
                        "disk",
                        {
                            "disk": {
                                "capacity": total_resources[capacity_key] - free_resources[capacity_key],
                                "bandwidth": total_resources[bandwidth_key] - free_resources[bandwidth_key],
                            },
                        },
                    ])

            return resources

        raise RuntimeError("Unsupported object type \"{}\" for mock".format(object_type))

    build_yp_client_mock().select_objects.side_effect = select_objects

from yp.common import wait

import pytest

from infra.box_dns_controller.controller import StandaloneController


def create_pod_with_boxes(yp_client, pod_id, boxes, needed_number_of_boxes, ruin_boxes_addresses=False):
    pod_persistent_fqdn = "{}.{}.yp-c.yandex.net".format(pod_id, "sas-test")

    network_id = yp_client.create_object(
        "network_project",
        attributes={
            "spec": {
                "project_id": 123,
            }
        }
    )
    node_id = yp_client.create_object(
        "node",
        attributes={
            "spec": {
                "ip6_subnets": [
                    {
                        "vlan_id": "backbone",
                        "subnet": "2a02:6b8:0:0::/64",
                    }
                ]
            }
        }
    )
    pod_set_id = yp_client.create_object("pod_set")
    yp_client.create_object(
        "pod",
        attributes={
            "spec": {
                "node_id": node_id,
                "ip6_subnet_requests": [
                    {
                        "network_id": network_id,
                        "labels": {
                            "id": "boxes_subnet",
                        },
                        "vlan_id": "backbone",
                    },
                ]
            },
            "meta": {
                "id": pod_id,
                "pod_set_id": pod_set_id
            },
            "status": {
                "dns": {"persistent_fqdn": pod_persistent_fqdn},
            }
        }
    )

    wait(
        lambda: yp_client.get_object("pod", pod_id, selectors=["/status/ip6_subnet_allocations"])[0]
    )

    subnet_address = yp_client.get_object("pod", pod_id, selectors=["/status/ip6_subnet_allocations"])[0][0]["subnet"]

    pod_agent_boxes = []

    for box in boxes:
        if not ruin_boxes_addresses:
            pod_agent_boxes.append({"id": box["id"], "ip6_address": subnet_address[:-5] + box["address_suffix"]})
        else:
            pod_agent_boxes.append({"id": box["id"], "ip6_address": "1" + subnet_address[1:-5] + box["address_suffix"]})

    yp_client.update_object(
        "pod",
        pod_id,
        set_updates=[
            {
                "path": "/status/agent/pod_agent_payload",
                "value": {"status": {"boxes": pod_agent_boxes}}
            }
        ]
    )

    wait(
        lambda: (len(yp_client.get_object("pod", pod_id, selectors=["/status/agent/pod_agent_payload/status/boxes"])[0]) == needed_number_of_boxes
                 if needed_number_of_boxes
                 else True)
    )

    return yp_client.get_object("pod", pod_id, selectors=["/status/agent/pod_agent_payload/status/boxes"])


def create_controller(yp_instance):
    config = {
        "Controller": {
            "YpClient": {
                "Address": yp_instance.yp_client_grpc_address,
                "EnableSsl": False,
            },
            "LeadingInvader": {
                "Path": "//home",
                "Proxy": yp_instance.create_yt_client().config["proxy"]["url"],
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
        }
    }

    return StandaloneController(config)


@pytest.mark.usefixtures("ctl_env")
def test_lowercase_fqdn(ctl_env):
    yp_instance = ctl_env
    yp_client = yp_instance.create_client()
    controller = create_controller(yp_instance)

    pod_id = "test"
    boxes_info = [
        {
            "id": "UPPERCASE-ID",
            "address_suffix": "1"
        }
    ]

    create_pod_with_boxes(yp_client, pod_id, boxes_info, 1)

    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set",
                                           selectors=["/meta/id", "/spec/records"],
                                           order_by=[{"expression": "/meta/id"}])

    assert len(record_sets) == 4

    assert str(record_sets[0][0]).startswith("0.0.0.0.") and \
        str(record_sets[0][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.")
    assert (record_sets[0][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'box_subnet_root.test.sas-test.yp-c.yandex.net'}
    ])

    assert str(record_sets[1][0]).startswith("1.0.0.0.") and \
        str(record_sets[1][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.") and \
        str(record_sets[1][0])[8:16] == str(record_sets[1][0])[8:16]
    assert (record_sets[1][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'uppercase-id.test.sas-test.yp-c.yandex.net'}
    ])

    assert record_sets[2][0] == "box_subnet_root.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[2][1]) == 1
    assert record_sets[2][1][0]['class'] == 'IN' and record_sets[2][1][0]['type'] == 'AAAA' and \
        str(record_sets[2][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[2][1][0]['data']).endswith(':0')

    assert record_sets[3][0] == "uppercase-id.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[3][1]) == 1

    nonce_last_pos = 14
    while str(record_sets[3][1][0]['data'])[nonce_last_pos] != ':':
        nonce_last_pos += 1

    assert record_sets[3][1][0]['class'] == 'IN' and record_sets[3][1][0]['type'] == 'AAAA' and \
        str(record_sets[3][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[3][1][0]['data']).endswith(':1') and \
        str(record_sets[3][1][0]['data'])[13:nonce_last_pos + 1] == str(record_sets[2][1][0]['data'])[13:nonce_last_pos + 1]

    yp_client.remove_object("pod", pod_id)

    controller.sync()

    assert yp_client.select_objects("dns_record_set",
                                    selectors=["/meta/id"]) == []


@pytest.mark.usefixtures("ctl_env")
def test_invalid_box_id(ctl_env):
    yp_instance = ctl_env
    yp_client = yp_instance.create_client()
    controller = create_controller(yp_instance)

    pod_id = "test"
    boxes_info = [
        {
            "id": "invalid@id",
            "address_suffix": "1"
        }
    ]

    create_pod_with_boxes(yp_client, pod_id, boxes_info, 0)

    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set",
                                           selectors=["/meta/id", "/spec/records"],
                                           order_by=[{"expression": "/meta/id"}])

    assert len(record_sets) == 2

    assert str(record_sets[0][0]).startswith("0.0.0.0.") and \
        str(record_sets[0][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.")
    assert (record_sets[0][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'box_subnet_root.test.sas-test.yp-c.yandex.net'}
    ])

    assert record_sets[1][0] == "box_subnet_root.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[1][1]) == 1
    assert record_sets[1][1][0]['class'] == 'IN' and record_sets[1][1][0]['type'] == 'AAAA' and \
        str(record_sets[1][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[1][1][0]['data']).endswith(':0')


@pytest.mark.usefixtures("ctl_env")
def test_collisions(ctl_env):
    yp_instance = ctl_env
    yp_client = yp_instance.create_client()
    controller = create_controller(yp_instance)

    pod_id = "test"
    boxes_info = [
        {
            "id": "collision-ID",
            "address_suffix": "1"
        },
        {
            "id": "COLLISION-ID",
            "address_suffix": "2"
        }
    ]

    create_pod_with_boxes(yp_client, pod_id, boxes_info, 2)

    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set",
                                           selectors=["/meta/id", "/spec/records"],
                                           order_by=[{"expression": "/meta/id"}])

    assert len(record_sets) == 4

    assert str(record_sets[0][0]).startswith("0.0.0.0.") and \
        str(record_sets[0][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.")
    assert (record_sets[0][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'box_subnet_root.test.sas-test.yp-c.yandex.net'}
    ])

    assert str(record_sets[1][0]).startswith("1.0.0.0.") and \
        str(record_sets[1][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.") and \
        str(record_sets[1][0])[8:16] == str(record_sets[1][0])[8:16]
    assert (record_sets[1][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'collision-id.test.sas-test.yp-c.yandex.net'}
    ])

    assert record_sets[2][0] == "box_subnet_root.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[2][1]) == 1
    assert record_sets[2][1][0]['class'] == 'IN' and record_sets[2][1][0]['type'] == 'AAAA' and \
        str(record_sets[2][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[2][1][0]['data']).endswith(':0')

    assert record_sets[3][0] == "collision-id.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[3][1]) == 1

    nonce_last_pos = 14
    while str(record_sets[3][1][0]['data'])[nonce_last_pos] != ':':
        nonce_last_pos += 1

    assert record_sets[3][1][0]['class'] == 'IN' and record_sets[3][1][0]['type'] == 'AAAA' and \
        str(record_sets[3][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[3][1][0]['data']).endswith(':1') and \
        str(record_sets[3][1][0]['data'])[13:nonce_last_pos + 1] == str(record_sets[2][1][0]['data'])[13:nonce_last_pos + 1]


@pytest.mark.usefixtures("ctl_env")
def test_correct_sync_with_some_invalid_boxes(ctl_env):
    yp_instance = ctl_env
    yp_client = yp_instance.create_client()
    controller = create_controller(yp_instance)

    pod_id = "test"

    boxes_info = [
        {
            "id": "good-id",
            "address_suffix": "1"
        },
        {
            "id": "bad@id",
            "address_suffix": "2"
        }
    ]

    create_pod_with_boxes(yp_client, pod_id, boxes_info, 2)

    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set",
                                           selectors=["/meta/id", "/spec/records"],
                                           order_by=[{"expression": "/meta/id"}])

    assert len(record_sets) == 4

    assert str(record_sets[0][0]).startswith("0.0.0.0.") and \
        str(record_sets[0][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.")
    assert (record_sets[0][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'box_subnet_root.test.sas-test.yp-c.yandex.net'}
    ])

    assert str(record_sets[1][0]).startswith("1.0.0.0.") and \
        str(record_sets[1][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.") and \
        str(record_sets[1][0])[8:16] == str(record_sets[1][0])[8:16]
    assert (record_sets[1][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'good-id.test.sas-test.yp-c.yandex.net'}
    ])

    assert record_sets[2][0] == "box_subnet_root.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[2][1]) == 1
    assert record_sets[2][1][0]['class'] == 'IN' and record_sets[2][1][0]['type'] == 'AAAA' and \
        str(record_sets[2][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[2][1][0]['data']).endswith(':0')

    assert record_sets[3][0] == "good-id.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[3][1]) == 1

    nonce_last_pos = 14
    while str(record_sets[3][1][0]['data'])[nonce_last_pos] != ':':
        nonce_last_pos += 1

    assert record_sets[3][1][0]['class'] == 'IN' and record_sets[3][1][0]['type'] == 'AAAA' and \
        str(record_sets[3][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[3][1][0]['data']).endswith(':1') and \
        str(record_sets[3][1][0]['data'])[13:nonce_last_pos + 1] == str(record_sets[2][1][0]['data'])[13:nonce_last_pos + 1]


@pytest.mark.usefixtures("ctl_env")
def test_reserved_subnet_fqdn(ctl_env):
    yp_instance = ctl_env
    yp_client = yp_instance.create_client()
    controller = create_controller(yp_instance)

    pod_id = "test"
    boxes_info = [
        {
            "id": "box_subnet_root",
            "address_suffix": "1"
        }
    ]

    create_pod_with_boxes(yp_client, pod_id, boxes_info, 1)

    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set",
                                           selectors=["/meta/id", "/spec/records"],
                                           order_by=[{"expression": "/meta/id"}])

    assert len(record_sets) == 4

    assert str(record_sets[0][0]).startswith("0.0.0.0.") and \
        str(record_sets[0][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.")
    assert (record_sets[0][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'box_subnet_root_1.test.sas-test.yp-c.yandex.net'}
    ])

    assert str(record_sets[1][0]).startswith("1.0.0.0.") and \
        str(record_sets[1][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.") and \
        str(record_sets[1][0])[8:16] == str(record_sets[1][0])[8:16]
    assert (record_sets[1][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'box_subnet_root.test.sas-test.yp-c.yandex.net'}
    ])

    assert record_sets[2][0] == "box_subnet_root.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[2][1]) == 1
    assert record_sets[2][1][0]['class'] == 'IN' and record_sets[2][1][0]['type'] == 'AAAA' and \
        str(record_sets[2][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[2][1][0]['data']).endswith(':1')

    assert record_sets[3][0] == "box_subnet_root_1.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[3][1]) == 1

    nonce_last_pos = 14
    while str(record_sets[3][1][0]['data'])[nonce_last_pos] != ':':
        nonce_last_pos += 1

    assert record_sets[3][1][0]['class'] == 'IN' and record_sets[3][1][0]['type'] == 'AAAA' and \
        str(record_sets[3][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[3][1][0]['data']).endswith(':0') and \
        str(record_sets[3][1][0]['data'])[13:nonce_last_pos + 1] == str(record_sets[2][1][0]['data'])[13:nonce_last_pos + 1]


@pytest.mark.usefixtures("ctl_env")
def test_many_boxes(ctl_env):
    yp_instance = ctl_env
    yp_client = yp_instance.create_client()
    controller = create_controller(yp_instance)

    pod_id = "test"
    boxes_info = [
        {
            "id": "id1",
            "address_suffix": "1"
        },
        {
            "id": "id2",
            "address_suffix": "2"
        },
        {
            "id": "id3",
            "address_suffix": "3"
        },
        {
            "id": "id4",
            "address_suffix": "4"
        },
        {
            "id": "id5",
            "address_suffix": "5"
        },
        {
            "id": "id6",
            "address_suffix": "6"
        },
        {
            "id": "id7",
            "address_suffix": "7"
        },
        {
            "id": "id8",
            "address_suffix": "8"
        },
        {
            "id": "id9",
            "address_suffix": "9"
        },
        {
            "id": "id10",
            "address_suffix": "10"
        },
        {
            "id": "id11",
            "address_suffix": "11"
        }
    ]

    create_pod_with_boxes(yp_client, pod_id, boxes_info, 11)

    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set",
                                           selectors=["/meta/id", "/spec/records"],
                                           order_by=[{"expression": "/meta/id"}])

    assert len(record_sets) == 24

    assert str(record_sets[0][0]).startswith("0.0.0.0.") and \
        str(record_sets[0][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.")
    assert (record_sets[0][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'box_subnet_root.test.sas-test.yp-c.yandex.net'}
    ])

    assert record_sets[12][0] == "box_subnet_root.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[12][1]) == 1
    assert record_sets[12][1][0]['class'] == 'IN' and record_sets[12][1][0]['type'] == 'AAAA' and \
        str(record_sets[12][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[12][1][0]['data']).endswith(':0')


@pytest.mark.usefixtures("ctl_env")
def test_box_ip_not_from_subnet(ctl_env):
    yp_instance = ctl_env
    yp_client = yp_instance.create_client()
    controller = create_controller(yp_instance)

    pod_id = "test"
    boxes_info = [
        {
            "id": "ip_not_from_subnet",
            "address_suffix": "1"
        }
    ]

    create_pod_with_boxes(yp_client, pod_id, boxes_info, 1, ruin_boxes_addresses=True)

    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set",
                                           selectors=["/meta/id", "/spec/records"],
                                           order_by=[{"expression": "/meta/id"}])

    assert len(record_sets) == 2

    assert str(record_sets[0][0]).startswith("0.0.0.0.") and \
        str(record_sets[0][0]).endswith(".b.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.")
    assert (record_sets[0][1] == [
        {'class': 'IN', 'type': 'PTR', 'data': 'box_subnet_root.test.sas-test.yp-c.yandex.net'}
    ])

    assert record_sets[1][0] == "box_subnet_root.test.sas-test.yp-c.yandex.net"
    assert len(record_sets[1][1]) == 1
    assert record_sets[1][1][0]['class'] == 'IN' and record_sets[1][1][0]['type'] == 'AAAA' and \
        str(record_sets[1][1][0]['data']).startswith('2a02:6b8::7b:') and \
        str(record_sets[1][1][0]['data']).endswith(':0')

import pytest
from infra.rtc_sla_tentacles.backend.lib.harvesters.hq import HarvesterHQ


class Dict2Obj:
    def __init__(self, data: dict):
        for K, V in data.items():
            if isinstance(V, (list, tuple)):
                setattr(self, K, [Dict2Obj(x) if isinstance(x, dict) else x for x in V])
            else:
                setattr(self, K, Dict2Obj(V) if isinstance(V, dict) else V)


extract_data_item = {
    "meta": {
        "id": "some-id"
    },
    "spec": {
        "hostname": "host",
        "node_name": "node_name"
    },
}


@pytest.fixture
def harvester_hq(config_interface, harvesters_snapshot_manager):
    type_config = config_interface.get_harvester_config("hq")
    harvester = next(HarvesterHQ.build_instances(config_interface, harvesters_snapshot_manager, type_config))
    return harvester


def test_hq_transform(harvester_hq):
    expected_meta = {
        "length": 1
    }
    expected_data_item = {
        "meta": {
            "id": "some-id"
        },
        "spec": {
            "hostname": "host",
            "node_name": "node_name"
        },
    }
    expected_data = (
        expected_meta,
        {"values": [expected_data_item]}
    )
    assert expected_data == harvester_hq.transform(1, [Dict2Obj(extract_data_item)])

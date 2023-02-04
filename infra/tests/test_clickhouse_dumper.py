import pytest

from infra.rtc_sla_tentacles.backend.lib.harvesters.clickhouse_dumper import ClickhouseDumper
from infra.rtc_sla_tentacles.backend.lib.harvesters_snapshots.snapshots import HarvesterSnapshotLabel, HarvesterSnapshot


@pytest.fixture
def clickhouse_dumper(config_interface, harvesters_snapshot_manager):
    return ClickhouseDumper(
        "", {"harvesters": ["walle", "hq", "juggler"]}, {}, {}, harvesters_snapshot_manager, config_interface,
        several_harvesters=False
    )


def fill_mongomock_with_dumpable_snapshots(harvesters_snapshot_manager):
    labels = [
        {"ts": 0, "harvester_type": "walle", "harvester_name": "walle", "meta": {}},
        {"ts": 10, "harvester_type": "walle", "harvester_name": "walle", "meta": {}},  # must be dumped
        {"ts": 20, "harvester_type": "walle", "harvester_name": "walle", "meta": None},
        {"ts": 0, "harvester_type": "hq", "harvester_name": "az1", "meta": {}},
        {"ts": 10, "harvester_type": "hq", "harvester_name": "az1", "meta": {}},  # must be dumped
        {"ts": 20, "harvester_type": "hq", "harvester_name": "az2", "meta": {}},  # must be dumped
        {"ts": 0, "harvester_type": "juggler", "harvester_name": "az1", "meta": {}}  # must be dumped
    ]
    for _l in labels:
        label = HarvesterSnapshotLabel(
            ts=_l["ts"],
            harvester_type=_l["harvester_type"],
            harvester_name=_l["harvester_name"],
            data_list_path="values"
        )
        snapshot = HarvesterSnapshot(
            label=label,
            debug_info={},
            meta=_l["meta"],
            data={"values": [{_l["harvester_name"]: _l["ts"]}]}
        )
        harvesters_snapshot_manager.write_snapshot(snapshot)

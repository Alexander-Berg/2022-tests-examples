import pytest

from infra.rtc_sla_tentacles.backend.lib.harvesters.manager import HarvestersManager
from infra.rtc_sla_tentacles.backend.lib.harvesters_snapshots.snapshots import HarvesterSnapshotLabel


def test_harvesters_manager_get_harvester_groups(harvesters_manager):
    assert {param.harvester_type for param in harvesters_manager.get_harvester_groups()} == {
        "dummy",
        "juggler",
        "nanny_state_dumper",
        "hq",
        "resource_maker",
        "yp_lite_switcher",
        "yp_lite_pod_count_tuner",
        "yp_lite_reallocator",
        "clickhouse_dumper",
        "clickhouse_dropper",
        "clickhouse_optimizer",
        "yp_lite_pods_tracker",
    }


def test_rotate_snapshots(harvesters_manager):
    # Determine now() from records in mocked database.
    newest_label = harvesters_manager._snapshot_manager.find_labels(
        harvester_type="dummy", harvester_name="dummy_foo", limit=1)
    now = newest_label[0].ts + 60

    groups = harvesters_manager.get_harvester_groups()
    for group in groups:
        if group.harvester_type == "dummy":
            dummy_foo_harvester = group.harvesters["dummy_foo"]
            dummy_foo_harvester._rotate_snapshots(ts=now)

    foo_labels_after_rotation_actual = harvesters_manager._snapshot_manager.find_labels(
        harvester_type="dummy", harvester_name="dummy_foo")
    assert foo_labels_after_rotation_actual == [
        HarvesterSnapshotLabel(ts=now - 60,
                               harvester_type="dummy",
                               harvester_name="dummy_foo"),
        HarvesterSnapshotLabel(ts=now - 120,
                               harvester_type="dummy",
                               harvester_name="dummy_foo"),
        HarvesterSnapshotLabel(ts=now - 180,
                               harvester_type="dummy",
                               harvester_name="dummy_foo"),
    ]


def test_unknown_harvester_type(config_interface):
    # noinspection PyProtectedMember
    config_interface.harvesters_config._config['harvesters']['unknown'] = {}
    with pytest.raises(TypeError):
        HarvestersManager(config_interface)

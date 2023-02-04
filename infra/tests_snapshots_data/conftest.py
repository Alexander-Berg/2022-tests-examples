import time

import pytest

from infra.rtc_sla_tentacles.backend.lib.harvesters_snapshots.manager import HarvesterSnapshotManager
from infra.rtc_sla_tentacles.backend.lib.harvesters_snapshots.snapshots import HarvesterSnapshotLabel, HarvesterSnapshot
from infra.rtc_sla_tentacles.backend.lib.harvesters.manager import HarvestersManager


def sample_harvesters_labels_dicts():
    now = int(time.time())
    labels = [
        {"ts": 0, "harvester_type": "dummy", "harvester_name": "dummy_foo",
         "meta": {"some-meta": "0-M-dummy-M-dummy_foo"}},
        {"ts": 0, "harvester_type": "dummy", "harvester_name": "dummy_bar",
         "meta": {"some-meta": "0-M-dummy-M-dummy_bar"}},
        {"ts": 0, "harvester_type": "dummy", "harvester_name": "dummy_baz",
         "meta": {"some-meta": "0-M-dummy-M-dummy_baz"}},

        {"ts": 10, "harvester_type": "dummy", "harvester_name": "dummy_foo",
         "meta": {"some-meta": "10-M-dummy-M-dummy_foo"}},
        {"ts": 10, "harvester_type": "dummy", "harvester_name": "dummy_bar",
         "meta": {"some-meta": "10-M-dummy-M-dummy_bar"}},

        {"ts": 20, "harvester_type": "dummy", "harvester_name": "dummy_foo",
         "meta": {"some-meta": "20-M-dummy-M-dummy_foo"}},
        {"ts": 20, "harvester_type": "dummy", "harvester_name": "dummy_baz",
         "meta": {"some-meta": "20-M-dummy-M-dummy_baz"}},

        {"ts": 30, "harvester_type": "dummy", "harvester_name": "dummy_bar",
         "meta": {"some-meta": "30-M-dummy-M-dummy_bar"}},
        {"ts": 30, "harvester_type": "dummy", "harvester_name": "dummy_baz",
         "meta": {"some-meta": "30-M-dummy-M-dummy_baz"}},

        {"ts": 999, "harvester_type": "to_be_deleted", "harvester_name": "to_be_deleted_bar",
         "meta": {"some-meta": "999-M-to_be_deleted-M-to_be_deleted_bar"}},

        {"ts": now - 600, "harvester_type": "dummy", "harvester_name": "dummy_foo",
         "meta": {"some-meta": now - 600}},
        {"ts": now - 540, "harvester_type": "dummy", "harvester_name": "dummy_foo",
         "meta": {"some-meta": now - 540}},
        # Emulate absence of snapshot at all.
        # {"ts": now - 480, "harvester_type": "dummy", "harvester_name": "dummy_foo",
        #  "meta": {"some-meta": now - 480}},
        # Emulate failed job snapshot.
        {"ts": now - 420, "harvester_type": "dummy", "harvester_name": "dummy_foo", "meta": None},
        {"ts": now - 360, "harvester_type": "dummy", "harvester_name": "dummy_foo",
         "meta": {"some-meta": now - 360}},
        {"ts": now - 300, "harvester_type": "dummy", "harvester_name": "dummy_foo", "meta": None},
        # This one must stay after rotation (`rotate_snapshots_older_than_sec == 180` in Dummy harvesters config): is
        # is older than limit, but it is successful (`meta` is not `None`).
        {"ts": now - 240, "harvester_type": "dummy", "harvester_name": "dummy_foo",
         "meta": {"some-meta": now - 240}},
        {"ts": now - 180, "harvester_type": "dummy", "harvester_name": "dummy_foo", "meta": None},
        {"ts": now - 120, "harvester_type": "dummy", "harvester_name": "dummy_foo", "meta": None},
        {"ts": now - 60, "harvester_type": "dummy", "harvester_name": "dummy_foo", "meta": None},
    ]
    return labels


@pytest.fixture(name="sample_harvesters_labels_dicts")
def sample_harvesters_labels_dicts_fixture():
    return sample_harvesters_labels_dicts()


def fill_mongomock_database_with_harvesters_snapshots(harvester_snapshot_manager: HarvesterSnapshotManager) -> None:
    for _l in sample_harvesters_labels_dicts():
        ts = _l["ts"]
        harvester_type = _l["harvester_type"]
        harvester_name = _l["harvester_name"]
        meta = _l["meta"]
        label = HarvesterSnapshotLabel(ts=ts,
                                       harvester_type=harvester_type,
                                       harvester_name=harvester_name)
        data_sep = "-D-"
        data = {
            "values": [data_sep.join([str(ts), harvester_type, harvester_name])]
        }

        snapshot = HarvesterSnapshot(label=label,
                                     debug_info={"ts_plus_1": ts + 1},
                                     meta=meta,
                                     data=data)
        harvester_snapshot_manager.write_snapshot(snapshot)


@pytest.fixture
def harvesters_snapshot_manager(mongomock_client) -> HarvesterSnapshotManager:
    """
        Creates and returns an instance of 'HarvestersSnapshotManager'
        with mocked MongoClient.
    """
    harvester_snapshot_manager = HarvesterSnapshotManager(mongomock_client)
    fill_mongomock_database_with_harvesters_snapshots(harvester_snapshot_manager)
    return harvester_snapshot_manager


@pytest.fixture
def harvesters_manager(config_interface, harvesters_snapshot_manager) -> HarvestersManager:
    """
        Creates and returns an instance of 'HarvestersManager'.
    """
    return HarvestersManager(config_interface, harvesters_snapshot_manager)

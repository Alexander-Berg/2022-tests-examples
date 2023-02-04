import dataclasses
import time
import typing

import pytest

from infra.rtc_sla_tentacles.backend.lib.harvesters_snapshots.snapshots import HarvesterSnapshot, HarvesterSnapshotLabel


def get_dummy_snapshot_label(ts: int) -> HarvesterSnapshotLabel:
    return HarvesterSnapshotLabel(ts=ts,
                                  harvester_type="dummy",
                                  harvester_name="dummy_foo")


def get_chunked_snapshot_label(ts: int) -> HarvesterSnapshotLabel:
    return HarvesterSnapshotLabel(ts=ts,
                                  harvester_type="chunked",
                                  harvester_name="chunked_foo",
                                  chunk_size=1,
                                  data_list_path="values")


def get_dummy_snapshot(ts: int,
                       meta: typing.Dict,
                       data: typing.Any,
                       label: HarvesterSnapshotLabel = None) -> HarvesterSnapshot:
    if not label:
        label = get_dummy_snapshot_label(ts)
    debug_info = {
        "extract_start_time": ts + 1,
        "extract_end_time": ts + 2,
        "extract_exception": None,
        "transform_start_time": ts + 5,
        "transform_end_time": ts + 7,
        "transform_exception": None
    }
    meta = meta
    data = data

    return HarvesterSnapshot(label, debug_info, meta, data)


def compare_snapshots_metadata(sn1: HarvesterSnapshot, sn2: HarvesterSnapshot) -> bool:
    """
         Compare two snapshot's metadatas. Used only in tests.
    """
    return sn1.label == sn2.label and \
        sn1.debug_info == sn2.debug_info and \
        sn1.meta == sn2.meta


def test_harvesters_snapshot_manager_write_chunked_snapshot(harvesters_snapshot_manager):
    ts = int(time.time())
    meta = {
        "value": "some-meta"
    }
    data = {
        "values": [f"chunk_{i}" for i in range(10)]
    }
    label = get_chunked_snapshot_label(ts)
    snapshot = get_dummy_snapshot(ts, meta, data, label)
    harvesters_snapshot_manager.write_snapshot(snapshot)

    collection = harvesters_snapshot_manager._get_collection_handlers("some")
    expected_chunk_list = [{"values": [f"chunk_{i}"]} for i in range(10)]
    assert harvesters_snapshot_manager._split_snapshot_on_chunks(snapshot) == expected_chunk_list
    for i, chunk in enumerate(collection.find({})):
        del chunk["_id"]
        assert chunk == expected_chunk_list[i]


def test_harvesters_snapshot_manager_read_chunked_snapshot(harvesters_snapshot_manager):
    ts = int(time.time())
    meta = {
        "value": "some-meta"
    }
    data = {
        "values": [f"chunk_{i}" for i in range(10)]
    }
    label = get_chunked_snapshot_label(ts)
    snapshot = get_dummy_snapshot(ts, meta, data, label)
    harvesters_snapshot_manager.write_snapshot(snapshot)

    data_actual = harvesters_snapshot_manager.read_snapshot(label).data
    assert data_actual == data


def test_harvesters_snapshot_manager_delete_chunked_snapshot(harvesters_snapshot_manager):
    ts = int(time.time())
    meta = {
        "value": "some-meta"
    }
    data = {
        "values": [f"chunk_{i}" for i in range(10)]
    }
    label = get_chunked_snapshot_label(ts)
    snapshot = get_dummy_snapshot(ts, meta, data, label)
    harvesters_snapshot_manager.write_snapshot(snapshot)

    collection = harvesters_snapshot_manager._get_collection_handlers(snapshot.label.harvester_type)
    assert collection.count({}) == 10
    harvesters_snapshot_manager.clean_old_snapshots(label.harvester_type, label.harvester_name, label.ts + 1)
    assert collection.count({}) == 0


def test_harvesters_snapshot_manager_write_snapshot(harvesters_snapshot_manager):
    ts = int(time.time())
    meta = {
        "value": "some-meta"
    }
    data = {
        "values": ["some-data"]
    }

    label = get_dummy_snapshot_label(ts)
    snapshot = get_dummy_snapshot(ts, meta, data, label=label)
    harvesters_snapshot_manager.write_snapshot(snapshot)

    collection = harvesters_snapshot_manager._get_collection_handlers(snapshot.label.harvester_type)
    db_response = collection.find_one({"label.ts": {"$eq": ts}})
    assert db_response
    assert db_response["data"] == data
    assert db_response["label"] == dataclasses.asdict(snapshot.label)


def test_harvesters_snapshot_manager_read_snapshot(harvesters_snapshot_manager, sample_harvesters_labels_dicts):
    for _l in sample_harvesters_labels_dicts:
        ts = _l["ts"]
        harvester_type = _l["harvester_type"]
        harvester_name = _l["harvester_name"]
        label = HarvesterSnapshotLabel(ts=ts,
                                       harvester_type=harvester_type,
                                       harvester_name=harvester_name)

        meta_sep = "-M-"
        if "meta" in _l:
            meta_expected = _l["meta"]
        else:
            meta_expected = {
                "some-meta": meta_sep.join([str(ts), harvester_type, harvester_name])
            }

        debug_info_expected = {"ts_plus_1": ts + 1}
        snapshot_only_meta_expected = HarvesterSnapshot(label=label,
                                                        debug_info=debug_info_expected,
                                                        meta=meta_expected,
                                                        data=None)
        snapshot_only_meta_actual = harvesters_snapshot_manager.read_snapshot(label=label)
        assert compare_snapshots_metadata(snapshot_only_meta_expected, snapshot_only_meta_actual)

        data_sep = "-D-"
        data_expected = {
            "values": [data_sep.join([str(ts), harvester_type, harvester_name])]
        }
        snapshot_with_data_expected = HarvesterSnapshot(label=label,
                                                        debug_info=debug_info_expected,
                                                        meta=meta_expected,
                                                        data=data_expected)
        snapshot_with_data_actual = harvesters_snapshot_manager.read_snapshot(label=label)
        assert compare_snapshots_metadata(snapshot_with_data_expected, snapshot_with_data_actual)
        assert snapshot_with_data_expected.data == snapshot_with_data_actual.data


def get_find_labels_test_parameters() -> typing.List[typing.Tuple[dict, typing.List[HarvesterSnapshotLabel]]]:
    """
        Generates pairs of kw-arguments and corresponding
        list of `HarvesterSnapshotLabel`s.
    """
    result = []

    # Query by 'meta' dictionary -> one snapshot label.
    meta_eq_arguments: dict = {
        "harvester_type": "dummy",
        "harvester_name": "dummy_foo",
        "meta_query": {"$eq": {"some-meta": "10-M-dummy-M-dummy_foo"}}
    }
    meta_eq_labels_expected = [
        HarvesterSnapshotLabel(ts=10,
                               harvester_type="dummy",
                               harvester_name="dummy_foo"),
    ]
    result.append((meta_eq_arguments, meta_eq_labels_expected))

    # Query 'harvester_name', limit 1 -> one youngest label of that type.
    name_dummy_bar_limit_1_arguments: dict = {
        "harvester_type": "dummy",
        "harvester_name": "dummy_bar",
        "limit": 1
    }
    name_dummy_bar_limit_1_labels_expected = [
        HarvesterSnapshotLabel(ts=30,
                               harvester_type="dummy",
                               harvester_name="dummy_bar"),
    ]
    result.append((name_dummy_bar_limit_1_arguments, name_dummy_bar_limit_1_labels_expected))

    return result


@pytest.mark.parametrize("arguments,labels_expected", get_find_labels_test_parameters())
def test_harvesters_snapshot_manager_find_labels(harvesters_snapshot_manager, arguments, labels_expected):
    labels_actual = harvesters_snapshot_manager.find_labels(**arguments)
    assert labels_expected == labels_actual


def test_harvesters_snapshot_manager_delete_snapshot(harvesters_snapshot_manager):
    collection = harvesters_snapshot_manager._get_collection_handlers("to_be_deleted")
    query = {
        "label.ts": 999,
        "label.harvester_type": "to_be_deleted",
        "label.harvester_name": "to_be_deleted_bar"
    }
    data_expected_before_deletion = {
        "values": ["999-D-to_be_deleted-D-to_be_deleted_bar"]
    }

    assert collection.find_one(query)["data"] == data_expected_before_deletion

    label_to_delete = HarvesterSnapshotLabel(ts=999,
                                             harvester_type="to_be_deleted",
                                             harvester_name="to_be_deleted_bar")
    harvesters_snapshot_manager.clean_old_snapshots(
        label_to_delete.harvester_type, label_to_delete.harvester_name, ts_border=1000
    )
    assert collection.find_one(query) is None

import yt.wrapper as yt
from ads_pytorch.yt.table_path import TablePath
from ads_pytorch.yt.snapshotter import SnapshotInfo


def test_create_yt_snapshot_info():
    t1 = SnapshotInfo([], {})
    assert t1.processed_uris == []
    t2 = SnapshotInfo([TablePath("//home/ahaha")], {})
    assert t2.processed_uris == [TablePath("//home/ahaha")]


def test_save_load_snapshot_info():
    t = SnapshotInfo(
        [
            TablePath("//home/ahaha"),
            TablePath("//home/ahaha2", start_index=10, end_index=300),
            TablePath("//home/ahaha2", lower_key="1", upper_key="4"),
        ],
        {}
    )
    string = t.readable_dumps()
    assert isinstance(string, str)
    loaded = SnapshotInfo.readable_loads(string)
    assert loaded.processed_uris == [
        TablePath("//home/ahaha"),
        TablePath("//home/ahaha2", start_index=10, end_index=300),
        TablePath("//home/ahaha2", lower_key="1", upper_key="4"),
    ]

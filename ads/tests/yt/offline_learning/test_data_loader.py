import pytest
import yt.wrapper as yt
from ads_pytorch.yt.table_path import TablePath
from ads_pytorch.yt.offline_learning.data_loader import (
    chunk_table,
    make_periodic_checkpoints,
    has_checkpoint_mark,
    YTMinibatchRecordStreamReaderTable
)


@pytest.fixture
def table():
    return TablePath("//home/table1")


def test_chunk_table(table):
    columns = ['a', 'b', 'c']
    res = chunk_table(table_path=table, chunk_size=10, enforced_stop_checkpoints={}, end_index=31, features=columns, targets=[])
    assert res == [
        YTMinibatchRecordStreamReaderTable(table=TablePath(table, start_index=0, end_index=10), features=columns, targets=[]),
        YTMinibatchRecordStreamReaderTable(table=TablePath(table, start_index=10, end_index=20), features=columns, targets=[]),
        YTMinibatchRecordStreamReaderTable(table=TablePath(table, start_index=20, end_index=30), features=columns, targets=[]),
        YTMinibatchRecordStreamReaderTable(table=TablePath(table, start_index=30, end_index=31), features=columns, targets=[])
    ]


def test_chunk_table_with_start(table):
    columns = ['a', 'b', 'c']
    res = chunk_table(table_path=table, chunk_size=10, enforced_stop_checkpoints={}, end_index=31, start_index=15, features=columns, targets=[])
    assert res == [
        YTMinibatchRecordStreamReaderTable(TablePath(table, start_index=15, end_index=25), features=columns, targets=[]),
        YTMinibatchRecordStreamReaderTable(TablePath(table, start_index=25, end_index=31), features=columns, targets=[])
    ]


def test_chunk_table_with_checkpoints(table):
    columns = ['a', 'b', 'c']
    res = chunk_table(
        table_path=table,
        chunk_size=10,
        enforced_stop_checkpoints={
            "x1": [10, 10, 13, 29],
            "x2": [10, 29]
        },
        end_index=31,
        features=columns,
        targets=[]
    )

    assert res == [
        YTMinibatchRecordStreamReaderTable(TablePath(table, start_index=0, end_index=10), features=columns, targets=[]),
        YTMinibatchRecordStreamReaderTable(TablePath(table, start_index=10, end_index=13), features=columns, targets=[]),
        YTMinibatchRecordStreamReaderTable(TablePath(table, start_index=13, end_index=20), features=columns, targets=[]),
        YTMinibatchRecordStreamReaderTable(TablePath(table, start_index=20, end_index=29), features=columns, targets=[]),
        YTMinibatchRecordStreamReaderTable(TablePath(table, start_index=29, end_index=30), features=columns, targets=[]),
        YTMinibatchRecordStreamReaderTable(TablePath(table, start_index=30, end_index=31), features=columns, targets=[])
    ]

    # check checkpoint marks

    assert has_checkpoint_mark(res[0], mark="x1")
    assert has_checkpoint_mark(res[0], mark="x2")

    assert has_checkpoint_mark(res[1], mark="x1")
    assert not has_checkpoint_mark(res[1], mark="x2")

    assert not has_checkpoint_mark(res[2], mark="x1")
    assert not has_checkpoint_mark(res[2], mark="x2")

    assert has_checkpoint_mark(res[3], mark="x1")
    assert has_checkpoint_mark(res[3], mark="x2")

    assert not has_checkpoint_mark(res[4], mark="x1")
    assert not has_checkpoint_mark(res[4], mark="x2")

    assert not has_checkpoint_mark(res[5], mark="x1")
    assert not has_checkpoint_mark(res[5], mark="x2")


# validation


def test_make_validation_checkpoints_only_end():
    assert make_periodic_checkpoints(start=0, end=100, frequency=None, call_on_epoch_done=True) == [100]


def test_make_validation_checkpoints_only_end_no_final():
    with pytest.raises(Exception):
        make_periodic_checkpoints(start=0, end=100, frequency=None, call_on_epoch_done=False)


def test_make_validation_checkpoints():
    assert make_periodic_checkpoints(start=0, end=31, frequency=10, call_on_epoch_done=True) == [10, 20, 30, 31]


@pytest.mark.parametrize('add_final', [True, False])
def test_make_validation_checkpoints_too_big_frequency(add_final):
    assert make_periodic_checkpoints(start=0, end=7, frequency=10, call_on_epoch_done=add_final) == [7]


# total uri selection

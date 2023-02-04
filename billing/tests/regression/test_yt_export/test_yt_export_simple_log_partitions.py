import os
from typing import (
    List,
    Tuple,
)
import unittest.mock as mock
from datetime import (
    datetime as dt,
    date,
)
from collections import defaultdict
import json
import pytest

import luigi
from yatest.common import source_path


from dwh.grocery.tools import to_iso
from dwh.grocery.targets.yt_table_target import YTMapNodeTarget
from dwh.grocery.task.yt_export_task import(
    GetSimpleLogPartitions,
    YTShagrenExportTaskV3,
)
from dwh.grocery.yt_export import MonoClusterYTExport


def modify_at(index, function):
    def map_(v):
        return tuple(x if i != index else function(x) for i, x in enumerate(v))
    return map_


def load_simple_log():
    simple_log_path = os.path.join(source_path('billing/dwh/tests/regression/'), 'simple_log.json')
    with open(simple_log_path) as sl:
        simple_log = json.load(sl)
    return [modify_at(1, lambda x: dt.strptime(x, "%Y-%m-%dT%H:%M:%S"))(r) for r in simple_log]


PartitionPair = Tuple[date, dt]


def choose_updated_partitions(
        simple_log_partitions: List[PartitionPair],
        exported_partitions: List[PartitionPair],
):
    exported_partitions = defaultdict(lambda: dt.max, exported_partitions)
    return [
        (partition, update_dt)
        for partition, update_dt in simple_log_partitions
        if update_dt >= exported_partitions[partition]
    ]


# def test_shagren_tables_in_bunker_detection():
#     shagren_tables = GetSimpleLogPartitions.get_shagren_from_bunker()
#     assert shagren_tables == [
#         "bo.group_order_act_div_t",
#         "biee.mv_f_sales_dayly_t",
#     ]


def test_simple_log_parsing():
    simple_log = load_simple_log()
    task = GetSimpleLogPartitions()
    with mock.patch.object(task, 'get_simple_log_records', return_value=simple_log):
        task.parse_simple_log()


def test_updated_partitions():
    simple_log = load_simple_log()
    task = GetSimpleLogPartitions()
    with mock.patch.object(task, 'get_simple_log_records', return_value=simple_log):
        task.updated_partitions()


def test_put_get():
    simple_log = load_simple_log()
    task = GetSimpleLogPartitions()
    with mock.patch.object(task, 'get_simple_log_records', return_value=simple_log):
        updated_partitions = task.updated_partitions()
        task.run()
    output = task.output()
    assert list(map(tuple, output.read())) == updated_partitions


def test_shagren_full_power(freud_yt_test_root, workers_fix):
    """
    FEEL THE POWER OF SHAGREN
    """
    update_id = to_iso(dt.now())
    simple_log = load_simple_log()
    task_partitions = GetSimpleLogPartitions(
        cluster=freud_yt_test_root.cluster,
        update_id=update_id
    )
    with mock.patch.object(task_partitions, 'get_simple_log_records', return_value=simple_log):
        updated_partitions = task_partitions.updated_partitions()
    output = task_partitions.output()
    output.write(updated_partitions[:1:])
    output.touch()

    task = YTShagrenExportTaskV3(
        source_uri="meta/meta:bo.group_order_act_div_t",
        target=(freud_yt_test_root / YTMapNodeTarget("export_brand_new_shagren/")).path,
        cluster=freud_yt_test_root.cluster,
        update_id=update_id,
    )
    is_success = luigi.build([task], local_scheduler=True, workers=workers_fix)
    assert is_success
    assert task.complete()


def test_shagren_req_partiotions_on_get_input(freud_yt_test_root):
    update_id = to_iso(dt.now())
    task = YTShagrenExportTaskV3(
        source_uri="meta/meta:bo.group_order_act_div_t",
        target=(freud_yt_test_root / YTMapNodeTarget("export_brand_new_shagren/")).path,
        cluster=freud_yt_test_root.cluster,
        update_id=update_id,
    )
    assert len(task.output()) > 0


@pytest.mark.skip(reason="not fixed yet")
def test_entrypoint_no_partitions(freud_yt_test_root):
    update_id = to_iso(dt.now())
    task_partitions = GetSimpleLogPartitions(
        cluster=freud_yt_test_root.cluster,
        update_id=update_id
    )
    output = task_partitions.output()
    output.write([])
    output.touch()

    task = MonoClusterYTExport(
        cluster=freud_yt_test_root.real_cluster(),
        update_id=update_id,
        tables=["group_order_act_div_t"],
        targets={
            "group_order_act_div_t": (freud_yt_test_root / YTMapNodeTarget("no_group_order/")).path,
        },
    )
    assert task.output() == {'group_order_act_div_t': []}
    assert task.complete()


@pytest.mark.skip(reason="not fixed yet")
def test_entrypoint_some_partitions(freud_yt_test_root):
    update_id = to_iso(dt.now())
    simple_log = load_simple_log()
    task_partitions = GetSimpleLogPartitions(
        cluster=freud_yt_test_root.cluster,
        update_id=update_id
    )
    with mock.patch.object(task_partitions, 'get_simple_log_records', return_value=simple_log):
        updated_partitions = task_partitions.updated_partitions()
    output = task_partitions.output()
    output.write(updated_partitions[:1:])
    output.touch()

    task = MonoClusterYTExport(
        cluster=freud_yt_test_root.real_cluster(),
        update_id=update_id,
        tables=["group_order_act_div_t"],
        targets={
            "group_order_act_div_t": (freud_yt_test_root / YTMapNodeTarget("some_group_order/")).path,
        },
    )
    assert len(task.output()['group_order_act_div_t']) == 1
    assert not task.complete()
    is_success = luigi.build([task], local_scheduler=True, workers=1)
    assert is_success
    assert task.complete()

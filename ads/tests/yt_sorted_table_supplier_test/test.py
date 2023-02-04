import pytest
import yatest.common

import os
import re
import json
import math
import copy

import jinja2

import yt.wrapper
import yt.yson

from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    create_yt_queue,
    launch_bullied_processes_reading_queue,
    make_json_file,
    make_namedtuple,
)


def get_schama_without_order(schema):
    new_schema = []
    for item in schema:
        item = copy.deepcopy(item)
        item.pop("sort_order", None)
        new_schema.append(item)
    return new_schema


def create_data_table(yt_client, path, keys, dynamic=True):
    hash_expression = "farm_hash(StrKey, IntKey)"
    schema = [
        {"name": "Hash", "type": "uint64", "sort_order": "ascending", "expression": hash_expression},
        {"name": "StrKey", "type": "string", "sort_order": "ascending"},
        {"name": "IntKey", "type": "uint64", "sort_order": "ascending"},
        {"name": "MyFullState", "type": "string"},
    ]

    data = [{"StrKey": str(i), "IntKey": i, "MyFullState": "%s some data" % i} for i in keys]

    if dynamic:
        yt_client.create("table", path, attributes=dict(dynamic=True, schema=schema), force=True)
        yt_client.mount_table(path, sync=True)
        yt_client.insert_rows(path, data)
    else:
        yt_client.create("table", path=path, attributes={"schema": get_schama_without_order(schema)})
        yt_client.write_table(path, data)
        yt_client.run_sort(path, sort_by=["Hash", "StrKey", "IntKey"])

    return data


@pytest.fixture()
def stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    output_shards_count = 3
    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    output_queue_path = "//tmp/output_queue_" + test_id
    consuming_system_path = "//tmp/test_consuming_system_" + test_id

    source_dynamic_table_path = "//tmp/source_dynamic_table_" + test_id
    source_static_table_path = "//tmp/source_static_table_" + test_id

    master_path = "//tmp/master_state_" + test_id

    output_yt_queue = create_yt_queue(standalone_yt_cluster.get_yt_client(), output_queue_path, output_shards_count)
    yt_data_client = yt.wrapper.YtClient(os.environ["YT_PROXY"])

    return make_namedtuple("SimpleShardingTestStand", **locals())


class ShardingProcess(BulliedProcess):
    def __init__(self, config_path):
        super(ShardingProcess, self).__init__(
            launch_cmd=[
                yatest.common.binary_path("ads/bsyeti/big_rt/demo/sharding/sharding"),
                "--config-json",
                config_path,
            ]
        )


def make_sorted_table_supplier_config(params):
    with open(
        yatest.common.source_path("ads/bsyeti/big_rt/demo/sharding/read_from_yt_sorted_join_fullstates.conf")
    ) as f:
        conf_s = jinja2.Template(f.read()).render(params)
    return make_namedtuple(
        "ShardingConfig",
        path=make_json_file(conf_s, name_template="sharding_config_{json_hash}.json"),
    )


def sharder_launch_k_process(
    stand,
    shards_count,
    epochs_count,
    processes_count,
    worker_name,
    check_func,
    restart_randmax=None,
    timeout=600,
):
    max_shards = int(math.ceil(shards_count / float(processes_count)))

    configs = [
        make_sorted_table_supplier_config(
            dict(
                service_log_config=json.dumps(
                    {
                        "Fetcher": {
                            "Source": {
                                "Path": stand.source_dynamic_table_path,
                                "Clusters": [os.environ["YT_PROXY"]],
                            }
                        },
                        "FullStates": [
                            {
                                "Fetcher": {
                                    "Source": {
                                        "Path": stand.source_static_table_path,
                                        "Clusters": [os.environ["YT_PROXY"]],
                                        "Columns": ["*"],
                                    }
                                },
                                "Name": "MyFullState",
                                "AllowSkipOnLag": False,
                            },
                        ],
                    }
                ),
                shards_count=shards_count,
                max_shards=max_shards,
                port=stand.port_manager.get_port(),
                consuming_system_main_path=stand.consuming_system_path,
                output_queue=stand.output_yt_queue["path"],
                output_shards_count=stand.output_yt_queue["shards"],
                yt_master_cluster=os.environ["YT_PROXY"],
                global_log=os.path.join(
                    yatest.common.output_path(), "global_{}.log".format("{}-{}".format(worker_name, i))
                ),
                worker_minor_name="{}-{}".format(worker_name, i),
                master_path=stand.master_path,
                random_shard_on_parse_error="true",
                max_epochs_count=epochs_count,
            )
        )
        for i in range(processes_count)
    ]
    sharders = [ShardingProcess(config.path) for config in configs]
    launch_bullied_processes_reading_queue(
        sharders,
        data_or_check_func=check_func,
        restart_randmax=restart_randmax,
        problems_max_count=2,
        timeout=timeout,
        gen_coredump_on_timeout=True,
    )
    return sharders


def get_rows_in_queue(stand, row_count_to_read):
    rows = []
    for shard in range(stand.output_yt_queue["shards"]):
        shard_result = stand.output_yt_queue["queue"].read(shard, 0, row_count_to_read)
        assert shard_result["offset_from"] == 0
        rows += shard_result["rows"]
    return rows


def create_read_complete_checker(stand, expected_rows_count):
    def check_data_read():
        rows_in_out_queue_count = len(get_rows_in_queue(stand, 10000000))
        return rows_in_out_queue_count == expected_rows_count

    return check_data_read


@pytest.mark.parametrize("shards_count", [5])
@pytest.mark.parametrize("yt_data_rows_count", [1000])
@pytest.mark.parametrize("processes_count", [3])
@pytest.mark.parametrize("epochs_count", [2])
@pytest.mark.parametrize("test_type", ["stable", "unstable"])
def test_service_log_fullstates_join(stand, shards_count, processes_count, yt_data_rows_count, epochs_count, test_type):
    keys = list(range(yt_data_rows_count))

    servicelog_keys = keys
    fullstate_keys = keys[::2]

    create_data_table(stand.yt_data_client, stand.source_dynamic_table_path, servicelog_keys, dynamic=True)
    create_data_table(stand.yt_data_client, stand.source_static_table_path, fullstate_keys, dynamic=False)

    sharder_launch_k_process(
        stand,
        shards_count,
        epochs_count,
        processes_count,
        stand.test_id,
        create_read_complete_checker(stand, epochs_count * yt_data_rows_count),
        restart_randmax=15 if test_type == "unstable" else None,
        timeout=300,
    )

    data_from_out_queue = [yt.yson.loads(d.rsplit(b" ", 1)[0]) for d in get_rows_in_queue(stand, 10000000)]

    keys_in_queue = [r["IntKey"] for r in data_from_out_queue]

    assert sorted(keys_in_queue) == sorted(epochs_count * servicelog_keys)

    for item in data_from_out_queue:
        if item["IntKey"] in fullstate_keys:
            assert item["FullStates"]["MyFullState"]["ExistState"] == 1
            assert item["FullStates"]["MyFullState"]["Data"] == "%s some data" % item["IntKey"]
        else:
            assert item["FullStates"]["MyFullState"]["ExistState"] == 2
            assert "Data" not in item["FullStates"]["MyFullState"]

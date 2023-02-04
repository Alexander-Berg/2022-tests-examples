from __future__ import print_function

import json
import logging
import os
import re
import sys

import jinja2
import pytest
import yatest.common
from ads.bsyeti.big_rt.py_test_lib import (
    create_yt_queue,
    execute_cli,
    launch_bullied_processes_reading_queue,
    make_json_file,
    make_namedtuple,
    wait_for_expected_count_is_written,
)
from ads.bsyeti.big_rt.py_test_lib.resharder import ReshardingProcess
from ads.bsyeti.big_rt.lib.events.proto.event_pb2 import TEventMessage
from library.python.framing.unpacker import Unpacker
from library.python.sanitizers import asan_is_on

# Can be enabled with --test-param sharder_path="/home/.../sharding/sharding"
# It is useful for tests with sanitizers applied only for big_rt binary
BINARY_PATH = yatest.common.get_param("sharder_path") or yatest.common.binary_path("ads/bsyeti/resharder/bin/resharder")


@pytest.fixture()
def stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    if not asan_is_on():
        input_shards_count = 10
        output_shards_count = 10
        data_part_length = 700
    else:
        input_shards_count = 3
        output_shards_count = 3
        data_part_length = 100

    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    input_queue_path = "//tmp/input_queue_" + test_id
    output_queue_path = "//tmp/output_queue_" + test_id
    consuming_system_path = "//tmp/test_consuming_system_" + test_id
    master_path = "//tmp/master_state_" + test_id

    input_yt_queue = create_yt_queue(standalone_yt_cluster.get_yt_client(), input_queue_path, input_shards_count)
    output_yt_queue = create_yt_queue(standalone_yt_cluster.get_yt_client(), output_queue_path, output_shards_count)

    queue_consumer = "test_cow"
    execute_cli(["consumer", "create", input_yt_queue["path"], queue_consumer, "--ignore-in-trimming", "0"])

    return make_namedtuple("SimpleShardingTestStand", **locals())


def make_exactly_once_test_config(stand, max_shards, worker_minor_name):
    with open(yatest.common.source_path("ads/bsyeti/tests/resharder/b2b/data/exactly_once_test_config.json")) as f:
        conf_s = jinja2.Template(f.read()).render(
            shards_count=stand.input_yt_queue["shards"],
            max_shards=max_shards,
            port=stand.port_manager.get_port(),
            main_path=stand.consuming_system_path,
            consumer=stand.queue_consumer,
            input_queue=stand.input_yt_queue["path"],
            output_queue=stand.output_yt_queue["path"],
            yt_cluster=os.environ["YT_PROXY"],
            global_log=os.path.join(yatest.common.output_path(), "global_{}.log".format(worker_minor_name)),
            yt_log=os.path.join(yatest.common.output_path(), "yt_{}.log".format(worker_minor_name)),
            worker_minor_name=worker_minor_name,
        )
    json_data = json.loads(conf_s)
    select_type_file = json_data["ResourcesReloaderConfig"]["SelectTypeFile"]
    assert os.path.isfile(select_type_file), "'%s' not exists" % select_type_file
    return make_namedtuple(
        "ShardingConfig",
        path=make_json_file(conf_s, name_template="resharder_config_{json_hash}.json"),
    )


def resharder_launch_k_process(stand, prefix, data, k, restart_randmax=None, timeout=600):
    max_shards = stand.input_yt_queue["shards"] // k + 1
    configs = [
        make_exactly_once_test_config(stand, max_shards, worker_minor_name="{}{}_{}".format(prefix, k, i))
        for i in range(k)
    ]
    sharders = [ReshardingProcess(BINARY_PATH, config.path) for config in configs]
    launch_bullied_processes_reading_queue(
        sharders,
        stand.input_yt_queue,
        stand.queue_consumer,
        data,
        restart_randmax=restart_randmax,
        timeout=timeout,
    )


def resharder_launch_one_process_stable(**args):
    resharder_launch_k_process(prefix="stable", k=1, timeout=180, **args)


def resharder_launch_two_process_stable(**args):
    resharder_launch_k_process(prefix="stable", k=2, timeout=240, **args)


def resharder_launch_two_process_unstable(**args):
    resharder_launch_k_process(prefix="unstable", k=2, restart_randmax=30, timeout=360, **args)


@pytest.mark.parametrize(
    "resharder_launcher",
    [
        resharder_launch_one_process_stable,
        resharder_launch_two_process_stable,
        resharder_launch_two_process_unstable,
    ],
)
def test_exactly_once(stand, resharder_launcher):
    logging.basicConfig(
        format="%(filename)s[LINE:%(lineno)d]# %(levelname)-8s [%(asctime)s]  %(message)s",
        level=logging.INFO,
    )
    logging.info("Start test")
    print(
        "Output dir: "
        + yatest.common.output_path()
        + " "
        + "cwd: "
        + os.getcwd()
        + " "
        + "stand.data_part_length="
        + str(stand.data_part_length),
        file=sys.stderr,
    )

    data = {
        input_shard: [
            json.dumps(
                {
                    "ProfileID": "y%d" % output_shard,
                    "TimeStamp": 1569499675,
                    "Slice": 1,
                    "StandVersion": "%02d%07d" % (input_shard, i),
                }
            )
            for i in range(stand.data_part_length)
            for output_shard in range(stand.output_yt_queue["shards"])
        ]
        for input_shard in range(stand.input_yt_queue["shards"])
    }
    stand.input_yt_queue["queue"].write(data)
    logging.info("Input queue was prepared")

    resharder_launcher(stand=stand, data=data)
    logging.info("Resharder launcher finished")

    # wait when data will be available for reading
    expected_rows = stand.data_part_length * stand.input_yt_queue["shards"]
    wait_for_expected_count_is_written(stand.output_yt_queue, expected_rows, timeout=60)
    logging.info("Expected rows checked")

    expected_splitted_ints = list(range(stand.data_part_length))
    for shard in range(stand.output_yt_queue["shards"]):
        shard_result = stand.output_yt_queue["queue"].read(shard, 0, expected_rows + 10)
        comment = " shard=%d" % shard
        assert shard_result["offset_from"] == 0, comment
        assert shard_result["offset_to"] + 1 == expected_rows, comment

        parsed_rows = []
        for raw_row in shard_result["rows"]:
            unpacker = Unpacker(raw_row)
            tmp_msg = TEventMessage()
            while True:
                tmp_msg, skip_data = unpacker.next_frame_proto(tmp_msg)
                if tmp_msg is None:
                    break
                parsed_rows.append((tmp_msg.ProfileID, int(tmp_msg.StandVersion)))

        expected_profile_id = ("y%d" % shard).encode("utf-8")
        result_splitted_ints = {input_shard: [] for input_shard in range(stand.input_yt_queue["shards"])}
        for profile_id, timestamp in parsed_rows:
            assert profile_id == expected_profile_id
            result_input_shard = (timestamp // (10**7)) % 100
            result_splitted_ints[result_input_shard].append(timestamp % (10**7))
        for input_shard in range(stand.input_yt_queue["shards"]):
            assert len(result_splitted_ints[input_shard]) == stand.data_part_length
            assert result_splitted_ints[input_shard] == expected_splitted_ints

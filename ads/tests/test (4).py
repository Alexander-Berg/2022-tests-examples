from __future__ import print_function, unicode_literals

import logging
import os
import re
import sys

import pytest
import yatest.common
from ads.bsyeti.big_rt import py_test_lib
from crypta.lib.python.jinja_resource import resource_render
from library.python.sanitizers import asan_is_on

# Can be enabled with --test-param sharder_path="/home/.../sharding/sharding"
# It is useful for tests with sanitizers applied only for big_rt binary
BINARY_PATH = yatest.common.get_param("sharder_path") or yatest.common.binary_path(
    "ads/bsyeti/big_rt/demo/resharder/bin/resharder"
)


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
    input_path = "//tmp/input_queue_{}".format(test_id)
    output_path = "//tmp/output_queue_{}".format(test_id)
    consuming_system_path = "//tmp/test_consuming_system_{}".format(test_id)
    master_path = "//tmp/master_state_{}".format(test_id)

    input_yt_queue = py_test_lib.create_yt_queue(standalone_yt_cluster.get_yt_client(), input_path, input_shards_count)
    output_yt_queue = py_test_lib.create_yt_queue(standalone_yt_cluster.get_yt_client(), output_path, output_shards_count)

    queue_consumer = "test_cow"
    py_test_lib.execute_cli(["consumer", "create", input_yt_queue["path"], queue_consumer, "--ignore-in-trimming", "0"])

    return py_test_lib.make_namedtuple("SimpleShardingTestStand", **locals())


class ShardingProcess(py_test_lib.BulliedProcess):
    def __init__(self, config_path):
        super(ShardingProcess, self).__init__(launch_cmd=[BINARY_PATH, "-c", config_path])


class ResharderTest(object):

    """Check reshrader"""

    def make_exactly_once_test_config(self, stand, max_shards, worker_minor_name):
        context = dict(
            shards_count=stand.input_yt_queue["shards"],
            max_shards=max_shards,
            port=stand.port_manager.get_port(),
            consuming_system_main_path=stand.consuming_system_path,
            consumer=stand.queue_consumer,
            input_queue=stand.input_yt_queue["path"],
            output_queue=stand.output_yt_queue["path"],
            output_shards_count=stand.output_yt_queue["shards"],
            yt_cluster=os.environ["YT_PROXY"],
            global_log=os.path.join(yatest.common.output_path(), "global_{}.log".format(worker_minor_name)),
            worker_minor_name=worker_minor_name,
            master_path=stand.master_path,
            parser="FromProto",
        )
        assert context["yt_cluster"].startswith("localhost:")
        return py_test_lib.make_namedtuple(
            "ShardingConfig",
            path=py_test_lib.make_json_file(
                resource_render("config.json.j2", **context),
                name_template="sharding_config_{json_hash}.json",
            ),
        )

    def sharder_launch_k_process(self, stand, data, k, restart_randmax=None, timeout=600):
        max_shards = stand.input_yt_queue["shards"] / k + 1
        configs = [self.make_exactly_once_test_config(stand, max_shards, worker_minor_name=str(i)) for i in range(k)]
        sharders = [ShardingProcess(config.path) for config in configs]
        py_test_lib.launch_bullied_processes_reading_queue(
            sharders,
            stand.input_yt_queue,
            stand.queue_consumer,
            data,
            restart_randmax=restart_randmax,
            timeout=timeout,
        )

    def sharder_launch_one_process_stable(self, **kwargs):
        self.sharder_launch_k_process(k=1, restart_randmax=None, timeout=180, **kwargs)

    def sharder_launch_two_process_stable(self, **kwargs):
        self.sharder_launch_k_process(k=2, restart_randmax=None, timeout=240, **kwargs)

    def sharder_launch_two_process_unstable(self, **kwargs):
        self.sharder_launch_k_process(k=2, restart_randmax=30, timeout=360, **kwargs)

    @pytest.mark.parametrize(
        "sharder_launcher",
        [
            sharder_launch_one_process_stable,
            sharder_launch_two_process_stable,
            sharder_launch_two_process_unstable,
        ],
    )
    def test_exactly_once(self, stand, sharder_launcher):
        logging.basicConfig(
            format="%(filename)s[LINE:%(lineno)d]# %(levelname)-8s [%(asctime)s]  %(message)s",
            level=logging.INFO,
        )
        logging.info("Start test")
        print("Output dir: {}".format(yatest.common.output_path()), file=sys.stderr)

        b64data = "ChoIG1IWChQI75+67qq9lc20ARCzi7nB3dCOARIcCAaCARcKFQiLq6yt/p7g8owBEMrrzavHr9mFBxgVIAU="
        data = {
            input_shard: [
                "{0} {1} {2} {3}".format(output_shard, input_shard, index, b64data)
                for index in range(stand.data_part_length)
                for output_shard in range(stand.output_yt_queue["shards"])
            ]
            for input_shard in range(stand.input_yt_queue["shards"])
        }
        print(data)
        stand.input_yt_queue["queue"].write(data)

        sharder_launcher(stand=stand, data=data)

        # wait when data will be available for reading
        expected_rows = stand.data_part_length * stand.input_yt_queue["shards"]
        py_test_lib.wait_for_expected_count_is_written(stand.output_yt_queue, expected_rows, timeout=60)

        expected_splitted_ints = list(range(stand.data_part_length))
        for shard in range(stand.output_yt_queue["shards"]):
            shard_result = stand.output_yt_queue["queue"].read(shard, 0, expected_rows + 10)
            comment = " shard=%d" % shard
            assert shard_result["offset_from"] == 0, comment
            assert shard_result["offset_to"] + 1 == expected_rows, comment

            logging.info("DDD>>> %r", shard_result["rows"][0])

            result_splitted_ints = {input_shard: [] for input_shard in range(stand.input_yt_queue["shards"])}
            for raw_row in shard_result["rows"]:
                output_shard, input_shard, original_i, some_data, ts = raw_row.split()
                assert int(output_shard) == shard
                result_splitted_ints[int(input_shard)].append(int(original_i))
            for input_shard in range(stand.input_yt_queue["shards"]):
                assert len(result_splitted_ints[input_shard]) == stand.data_part_length
                assert result_splitted_ints[input_shard] == expected_splitted_ints

from __future__ import print_function

import json
import logging
import math
import os
import re
from datetime import datetime, timedelta, timezone

import cyson
import jinja2
import pytest
import yatest.common
import yt.wrapper
import yt.yson
from google.protobuf import text_format
from ads.bsyeti.big_rt.lib.supplier.yt_directory.state.state_pb2 import TShardTranslateList
from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    create_yt_queue,
    launch_bullied_processes_reading_queue,
    make_json_file,
    make_namedtuple,
)


def create_data_tables(
    path,
    yt_client,
    yt_data_tables_count,
    yt_data_rows_per_table_count,
    start_dt=datetime(year=2020, month=6, day=7, tzinfo=timezone.utc),
):
    tables_names = [(start_dt + timedelta(days=i)).strftime("%Y-%m-%dT%H:%M:%SZ") for i in range(yt_data_tables_count)]

    for table in tables_names:
        yt_client.write_table(
            os.path.join(path, table),
            [{"Data": "%s some data" % i} for i in range(yt_data_rows_per_table_count)],
        )


@pytest.fixture()
def stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    output_shards_count = 3
    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    output_queue_path = "//tmp/output_queue_" + test_id
    consuming_system_path = "//tmp/test_consuming_system_" + test_id
    yt_tables_storage_path = "//tmp/tables_storage_" + test_id
    translate_path = consuming_system_path + "/suppliers/yt_directory_log"
    yt_data_static_tables_path = "//tmp/data_for_static_tables_yt_directory_supplier_" + test_id
    yt_data_tables_path = "//tmp/data_for_yt_directory_supplier_" + test_id
    master_path = "//tmp/master_state_" + test_id
    output_yt_queue = create_yt_queue(standalone_yt_cluster.get_yt_client(), output_queue_path, output_shards_count)
    yt_data_client = yt.wrapper.YtClient(os.environ["YT_PROXY"])

    yt_data_client.create("map_node", yt_data_tables_path)
    yt_data_client.create("map_node", yt_data_static_tables_path)
    yt_data_client.create("map_node", yt_tables_storage_path)

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


def make_yt_directory_supplier_config(
    stand,
    shards_count,
    max_shards,
    worker_minor_name,
    processes_count,
    tables=None,
    columns=None,
    parse_yson_as_string=False,
    no_storage_path=False,
    use_table_names_instead_of_timestamps=False,
    max_table_datetime=None,
):
    with open(yatest.common.source_path("ads/bsyeti/big_rt/demo/sharding/read_from_yt_directory.conf")) as f:
        conf_s = jinja2.Template(f.read()).render(
            shards_count=shards_count,
            max_shards=max_shards,
            enable_fake_balancer=(processes_count == 1),
            port=stand.port_manager.get_port(),
            consuming_system_main_path=stand.consuming_system_path,
            output_queue=stand.output_yt_queue["path"],
            output_shards_count=stand.output_yt_queue["shards"],
            yt_master_cluster=os.environ["YT_PROXY"],
            yt_cluster=os.environ["YT_PROXY"],
            yt_data_path=stand.yt_data_tables_path,
            tables_storage_path=stand.yt_tables_storage_path if not no_storage_path else "",
            tables="[%s]" % ",".join(['"%s"' % t for t in tables or []]),
            global_log=os.path.join(yatest.common.output_path(), "global_{}.log".format(worker_minor_name)),
            worker_minor_name=worker_minor_name,
            master_path=stand.master_path,
            start_timeout=100,
            acquire_lock_timeout=100,
            on_error_timeout=100,
            no_tables_timeout=100,
            random_shard_on_parse_error="true",
            columns=json.dumps(columns) if columns else "[]",
            parse_yson_as_string="true" if parse_yson_as_string else "false",
            use_table_names_instead_of_timestamps="true" if use_table_names_instead_of_timestamps else "false",
            max_table_datetime=max_table_datetime,
        )
    return make_namedtuple(
        "ShardingConfig",
        path=make_json_file(conf_s, name_template="sharding_config_{json_hash}.json"),
    )


def sharder_launch_k_process(
    stand,
    shards_count,
    processes_count,
    worker_name,
    check_func,
    restart_randmax=None,
    timeout=600,
    tables=None,
    columns=None,
    parse_yson_as_string=False,
    no_storage_path=False,
    use_table_names_instead_of_timestamps=False,
    max_table_datetime=None,
):
    max_shards = int(math.ceil(shards_count / float(processes_count)))
    configs = [
        make_yt_directory_supplier_config(
            stand,
            shards_count,
            max_shards,
            "{}-{}".format(worker_name, i),
            processes_count=processes_count,
            tables=tables,
            columns=columns,
            parse_yson_as_string=parse_yson_as_string,
            no_storage_path=no_storage_path,
            use_table_names_instead_of_timestamps=use_table_names_instead_of_timestamps,
            max_table_datetime=max_table_datetime,
        )
        for i in range(processes_count)
    ]
    sharders = [ShardingProcess(config.path) for config in configs]
    launch_bullied_processes_reading_queue(
        sharders,
        data_or_check_func=check_func,
        restart_randmax=restart_randmax,
        problems_max_count=1,
        timeout=timeout,
        gen_coredump_on_timeout=True,
    )
    return sharders


def get_offsets_sum(stand):
    if not stand.yt_data_client.exists(stand.translate_path + "/translate_table_offsets"):
        return 0

    def extract_offset(state):
        return int(re.search(r"Offset: (\d+)", state).group(1))

    offsets = stand.yt_data_client.select_rows("* from [{}]".format(stand.translate_path + "/translate_table_offsets"))

    return sum([extract_offset(row["Offset"]) for row in offsets])


def create_read_complete_checker(stand, rows_count):
    def check_data_read():
        actual_offset = get_offsets_sum(stand)
        logging.debug("Check yt directory read rows: actual=%d, expected=%d", actual_offset, rows_count)
        return actual_offset == rows_count

    return check_data_read


def get_rows_in_queue(stand, yt_data_rows_count):
    rows = []
    for shard in range(stand.output_yt_queue["shards"]):
        shard_result = stand.output_yt_queue["queue"].read(shard, 0, yt_data_rows_count)
        assert shard_result["offset_from"] == 0
        rows += shard_result["rows"]
    return rows


def do_test(
    stand,
    shards_count,
    yt_data_tables_count,
    yt_data_rows_per_table_count,
    processes_count,
    is_unstable=False,
    use_table_names_instead_of_timestamps=False,
):
    yt_data_rows_count = yt_data_tables_count * yt_data_rows_per_table_count

    create_data_tables(
        stand.yt_data_tables_path,
        stand.yt_data_client,
        yt_data_tables_count,
        yt_data_rows_per_table_count,
    )

    sharder_launch_k_process(
        stand,
        shards_count,
        processes_count,
        stand.test_id,
        create_read_complete_checker(stand, yt_data_rows_count),
        restart_randmax=15 if is_unstable else None,
        timeout=300,
        use_table_names_instead_of_timestamps=use_table_names_instead_of_timestamps,
    )

    rows_in_queue_count = len(get_rows_in_queue(stand, yt_data_rows_count))
    assert rows_in_queue_count == yt_data_rows_count


@pytest.mark.parametrize("shards_count", [15])
@pytest.mark.parametrize("yt_data_tables_count", [5])
@pytest.mark.parametrize("yt_data_rows_per_table_count", [1000])
@pytest.mark.parametrize("processes_count", [2])
@pytest.mark.parametrize("use_table_names_instead_of_timestamps", [True, False])
def test_yt_directory_supplier_unstable(
    stand,
    shards_count,
    yt_data_tables_count,
    yt_data_rows_per_table_count,
    processes_count,
    use_table_names_instead_of_timestamps,
):
    do_test(
        stand,
        shards_count,
        yt_data_tables_count,
        yt_data_rows_per_table_count,
        processes_count,
        is_unstable=True,
        use_table_names_instead_of_timestamps=use_table_names_instead_of_timestamps,
    )


@pytest.mark.parametrize("shards_count", [1, 10])
@pytest.mark.parametrize("yt_data_tables_count", [1, 5])
@pytest.mark.parametrize("yt_data_rows_per_table_count", [1, 10000])
@pytest.mark.parametrize("processes_count", [1])
@pytest.mark.parametrize("use_table_names_instead_of_timestamps", [True, False])
def test_yt_directory_supplier(
    stand,
    shards_count,
    yt_data_tables_count,
    yt_data_rows_per_table_count,
    processes_count,
    use_table_names_instead_of_timestamps,
):
    do_test(
        stand,
        shards_count,
        yt_data_tables_count,
        yt_data_rows_per_table_count,
        processes_count,
        is_unstable=False,
        use_table_names_instead_of_timestamps=use_table_names_instead_of_timestamps,
    )


@pytest.mark.parametrize("is_unstable", [True, False])
@pytest.mark.parametrize("columns", [[], ["Data"], ["AnotherData", "Data"], ["InvalidColumnName"]])
def test_yt_directory_supplier_static_table(stand, is_unstable, columns):
    shards_count = 5
    yt_data_rows_count = 5000
    processes_count = 1

    table_path = os.path.join(stand.yt_data_static_tables_path, "table")

    stand.yt_data_client.write_table(
        table_path,
        [{"Data": "%s some data" % i, "AnotherData": "another %s data" % i} for i in range(yt_data_rows_count)],
    )
    valid_columns = [b"Data", b"AnotherData"]

    sharder_launch_k_process(
        stand,
        shards_count,
        processes_count,
        stand.test_id,
        create_read_complete_checker(stand, yt_data_rows_count),
        restart_randmax=15 if is_unstable else None,
        timeout=300,
        tables=[table_path],
        columns=columns,
    )

    rows_in_queue = get_rows_in_queue(stand, yt_data_rows_count)
    columns = [c.encode() for c in columns]
    for row_in_queue in rows_in_queue:
        data = b" ".join(row_in_queue.split(b" ")[:-1])  # '{"a"="b"} timestamp'
        assert set(cyson.loads(data).keys()) == set(valid_columns).intersection(
            columns if len(columns) > 0 else valid_columns
        )

    rows_in_queue_count = len(rows_in_queue)
    assert rows_in_queue_count == yt_data_rows_count


@pytest.mark.parametrize("parse_yson_as_string", [True, False])
@pytest.mark.parametrize("columns", [[], ["Data", "AnotherData"], ["AnotherData", "Data"]])
def test_yt_directory_supplier_parse_yson_as_string(stand, parse_yson_as_string, columns):
    shards_count = 5
    yt_data_rows_count = 1
    processes_count = 1

    table_path = os.path.join(stand.yt_data_static_tables_path, "table")

    stand.yt_data_client.write_table(table_path, [{"Data": "some data", "AnotherData": {"a": 1, "b": "abacaba"}}])

    sharder_launch_k_process(
        stand,
        shards_count,
        processes_count,
        stand.test_id,
        create_read_complete_checker(stand, yt_data_rows_count),
        None,
        timeout=300,
        tables=[table_path],
        parse_yson_as_string=parse_yson_as_string,
        columns=columns,
    )

    rows_in_queue = get_rows_in_queue(stand, yt_data_rows_count)
    assert len(rows_in_queue) == 1, rows_in_queue
    for row_in_queue in rows_in_queue:
        data_yson = b" ".join(row_in_queue.split(b" ")[:-1])  # '{"a"="b"} timestamp'
        data = cyson.loads(data_yson)
        if parse_yson_as_string:
            assert data.get(b"Data") == b"some data"
            assert cyson.loads(data.get(b"AnotherData")) == {b"a": 1, b"b": b"abacaba"}
        else:
            assert data == cyson.loads('{"Data"="some data";"AnotherData"={"a"=1;"b"="abacaba"}}')

    rows_in_queue_count = len(rows_in_queue)
    assert rows_in_queue_count == yt_data_rows_count


def test_deleting_tables(stand):
    yt_data_tables_count = 3
    yt_data_rows_per_table_count = 100

    yt_data_rows_count = yt_data_tables_count * yt_data_rows_per_table_count

    empty_tables_count = 3

    start_dt = datetime(year=2020, month=6, day=5, tzinfo=timezone.utc)

    create_data_tables(
        stand.yt_data_tables_path,
        stand.yt_data_client,
        yt_data_tables_count,
        yt_data_rows_per_table_count,
        start_dt=start_dt,
    )

    # create empty tables
    create_data_tables(
        stand.yt_data_tables_path,
        stand.yt_data_client,
        yt_data_tables_count=empty_tables_count,
        yt_data_rows_per_table_count=0,
        start_dt=start_dt + timedelta(days=yt_data_tables_count),
    )

    assert len(stand.yt_data_client.list(stand.yt_data_tables_path)) == yt_data_tables_count + empty_tables_count

    read_checker = create_read_complete_checker(stand, yt_data_rows_count)

    def delete_tables_checker():
        if stand.yt_data_client.list(stand.yt_data_tables_path) == []:
            logging.debug("All tables were deleted")
            return True
        logging.debug("Not all tables were deleted")
        return False

    def translate_table_checker():
        try:
            state = list(stand.yt_data_client.select_rows("* from [{}]".format(stand.translate_path + "/translate_table")))
            assert state != []  # but tables were not deleted from state
            assert all("2020-06-07T00:00:00" in item["OffsetsTranslateList"] for item in state)
            return True
        except Exception:
            logging.exception("Translate table check failed.")
            return False

    def checker():
        return read_checker() and delete_tables_checker() and translate_table_checker()

    sharder_launch_k_process(
        stand,
        1,
        1,
        stand.test_id,
        checker,
        restart_randmax=None,
        timeout=300,
        no_storage_path=True,
    )


def test_without_max_table_datetime(stand):
    yt_data_tables_count = 30
    yt_data_rows_per_table_count = 5

    yt_data_rows_count = yt_data_tables_count * yt_data_rows_per_table_count

    start_dt = datetime(year=2020, month=6, day=5, tzinfo=timezone.utc)

    create_data_tables(
        stand.yt_data_tables_path,
        stand.yt_data_client,
        yt_data_tables_count,
        yt_data_rows_per_table_count,
        start_dt=start_dt,
    )

    assert len(stand.yt_data_client.list(stand.yt_data_tables_path)) == yt_data_tables_count

    read_checker = create_read_complete_checker(stand, yt_data_rows_count)

    def translate_table_checker():
        try:
            state = list(stand.yt_data_client.select_rows("* from [{}]".format(stand.translate_path + "/translate_table")))
            assert len(state) == 1
            items = text_format.Parse(state[0]["OffsetsTranslateList"], TShardTranslateList())
            desired_time = start_dt + timedelta(days=yt_data_tables_count - 1)
            assert any(
                datetime.fromtimestamp(item.TableTimeStamp, timezone.utc) == desired_time
                for item in items.Items
            ), f"No correct item. items={items}, desired_time={desired_time}"
            logging.debug("Translate table check succeeded.")
            return True
        except Exception:
            logging.exception("Translate table check failed.")
            return False

    def checker():
        return read_checker() and translate_table_checker()

    sharder_launch_k_process(
        stand,
        1,
        1,
        stand.test_id,
        checker,
        restart_randmax=None,
        timeout=300,
        no_storage_path=True,
    )


def test_with_max_table_datetime(stand):
    yt_data_tables_count = 30
    yt_data_rows_per_table_count = 5

    start_dt = datetime(year=2020, month=6, day=5, tzinfo=timezone.utc)
    end_dt = datetime(year=2020, month=7, day=2, tzinfo=timezone.utc)

    yt_data_rows_count = ((end_dt - start_dt).days + 1) * yt_data_rows_per_table_count

    create_data_tables(
        stand.yt_data_tables_path,
        stand.yt_data_client,
        yt_data_tables_count,
        yt_data_rows_per_table_count,
        start_dt=start_dt,
    )

    assert len(stand.yt_data_client.list(stand.yt_data_tables_path)) == yt_data_tables_count

    read_checker = create_read_complete_checker(stand, yt_data_rows_count)

    def translate_table_checker():
        try:
            state = list(stand.yt_data_client.select_rows("* from [{}]".format(stand.translate_path + "/translate_table")))
            assert len(state) == 1
            items = text_format.Parse(state[0]["OffsetsTranslateList"], TShardTranslateList())
            assert any(datetime.fromtimestamp(item.TableTimeStamp, timezone.utc) == end_dt for item in items.Items)
            logging.debug("Translate table check succeeded.")
            return True
        except Exception:
            logging.exception("Translate table check failed.")
            return False

    def checker():
        return read_checker() and translate_table_checker()

    sharder_launch_k_process(
        stand,
        1,
        1,
        stand.test_id,
        checker,
        restart_randmax=None,
        timeout=300,
        no_storage_path=True,
        max_table_datetime=end_dt.strftime("%Y-%m-%dT%H:%M:%SZ"),
    )

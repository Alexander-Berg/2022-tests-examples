import json
import re
from collections import defaultdict

import pytest
import yatest.common
from yt.wrapper import yson

from ads.bsyeti.big_rt.cli import lib as bigrt_cli
from ads.bsyeti.big_rt.py_lib import YtQueuePath
from ads.bsyeti.big_rt.py_test_lib import (
    create_yt_queue,
    execute_cli,
    suppressing_context,
    waiting_iterable,
)


def get_queue_path(request):
    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    queue_path = "//tmp/queue" + test_id
    return queue_path


def create_queue_through_cli(queue_path, yt_ready_env, options=None):
    args = [
        "queue",
        "create",
        queue_path,
        "--shards",
        "1",
        "--medium",
        "default",
        "--max-ttl",
        "100000",
        "--commit-ordering",
        "weak",
    ]
    if options is not None:
        args.extend(["--queue-options", options])
    execute_cli(args)


@pytest.fixture()
def yt_queue(request, primary_yt_cluster, yt_ready_env):
    queue_path = get_queue_path(request)
    return create_yt_queue(primary_yt_cluster.get_yt_client(), queue_path, 3)


@pytest.mark.parametrize("swift", [False, True])
def test_create_remove_queue(request, primary_yt_cluster, swift):
    qpath = YtQueuePath("//tmp/queue" + re.sub(r"[^\w\d]", "_", request.node.name))
    client = primary_yt_cluster.get_yt_client()
    bigrt_cli.create_queue(
        queue_path=qpath.path,
        shards=1,
        medium="default",
        max_ttl=100000,
        commit_ordering="weak",
        queue_options="{}",
        swift=swift,
        client=client,
    )
    bigrt_cli.remove_queue(queue_path=qpath.path, force=False, client=client)


def test_consumers(yt_queue):
    queue_path = yt_queue["path"]
    shards_count = yt_queue["shards"]
    null_offset = yt_queue["queue"].null_offset
    assert shards_count == yt_queue["queue"].get_shard_count()
    consumer_name = "cow_and_rabbit"
    execute_cli(["consumer", "create", queue_path, consumer_name, "--ignore-in-trimming", "0"])
    consumers = yt_queue["queue"].get_consumers()
    assert [consumer_name] == consumers
    offsets = yt_queue["queue"].get_consumer_offsets(consumer_name)
    assert [null_offset] * shards_count == offsets

    execute_cli(["consumer", "set_options", queue_path, consumer_name, '{"IgnoreInTrimming": true}'])
    assert {"IgnoreInTrimming": True} == yt_queue["queue"].get_consumer_options(consumer_name)

    execute_cli(["consumer", "update_options", queue_path, consumer_name, "{}"])
    assert {"IgnoreInTrimming": True} == yt_queue["queue"].get_consumer_options(consumer_name)

    consumer_options = execute_cli(["consumer", "get_options", queue_path, consumer_name]).std_out
    assert b'{"IgnoreInTrimming": true}' == consumer_options.strip()

    execute_cli(["consumer", "update_options", queue_path, consumer_name, '{"IgnoreInTrimming": false}'])
    assert {"IgnoreInTrimming": False} == yt_queue["queue"].get_consumer_options(consumer_name)

    execute_cli(["consumer", "update_options", queue_path, consumer_name, "{}"])
    assert {"IgnoreInTrimming": False} == yt_queue["queue"].get_consumer_options(consumer_name)

    consumer_options = execute_cli(["consumer", "get_options", queue_path, consumer_name]).std_out
    assert b'{"IgnoreInTrimming": false}' == consumer_options.strip()

    with pytest.raises(Exception):  # bad field
        execute_cli(["consumer", "set_options", queue_path, consumer_name, '{"not_existing_field": "oha"}'])

    offsets = execute_cli(["consumer", "get_offsets", queue_path, consumer_name]).std_out
    assert json.loads(offsets) == {str(i): null_offset for i in range(shards_count)}

    execute_cli(
        [
            "consumer",
            "update_offsets",
            queue_path,
            consumer_name,
            "--value",
            "10 if shard < 2 else old",
        ]
    )
    offsets = execute_cli(["consumer", "get_offsets", queue_path, consumer_name]).std_out
    assert json.loads(offsets) == {str(i): (10 if i < 2 else null_offset) for i in range(shards_count)}

    consumer_name_2 = "some_mammal"
    execute_cli(["consumer", "create", queue_path, consumer_name_2, "--ignore-in-trimming", "1"])
    assert {"IgnoreInTrimming": True} == yt_queue["queue"].get_consumer_options(consumer_name_2)
    consumer_list = execute_cli(["consumer", "list", queue_path, "--json"]).std_out
    assert set(json.loads(consumer_list)) == {consumer_name, consumer_name_2}

    execute_cli(["consumer", "remove", queue_path, consumer_name])
    consumer_list = execute_cli(["consumer", "list", queue_path, "--json"]).std_out
    assert set(json.loads(consumer_list)) == {consumer_name_2}


def test_readme():
    readme_text = execute_cli(["gen_readme", "--output", "/dev/stdout"]).std_out.decode()
    assert (
        readme_text == open(yatest.common.source_path("ads/bsyeti/big_rt/cli/README.md")).read()
    ), "Regenerate README.md: ./big_rt_cli gen_readme"


@pytest.mark.parametrize("codec", [None, "null", "zstd_6"])
def test_queue_io(yt_queue, codec):
    writings = [
        {"1": ["rec_1_0", "rec_1_1"], "0": ["rec_0_0"]},
        {"1": ["rec_1_2"], "0": ["rec_0_1"], "2": ["rec_2_0"]},
    ]
    result = defaultdict(list)

    for writing in writings:
        cmd = ["queue", "write", yt_queue["path"], json.dumps(writing)]
        if codec is not None:
            cmd += ["--codec", codec]
        execute_cli(cmd)
        for shard, records in writing.items():
            result[shard].extend(records)

    for waiting_state in waiting_iterable(timeout=20):
        with suppressing_context(do_suppress=not waiting_state.is_last):
            for shard, expected_records in result.items():
                shard_records = execute_cli(
                    [
                        "queue",
                        "read",
                        yt_queue["path"],
                        "--raw",
                        "--shard",
                        shard,
                        "--start-offset",
                        "0",
                        "--limit",
                        str(len(expected_records) * 10),
                    ]
                ).std_out
                shard_records = yson.loads(shard_records)
                assert shard_records["rows"] == expected_records
                assert shard_records["offset_from"] == 0
                assert shard_records["offset_to"] == len(expected_records) - 1
            break

    shard_records = execute_cli(
        [
            "queue",
            "read",
            yt_queue["path"],
            "--raw",
            "--shard",
            "1",
            "--start-offset",
            "1",
            "--limit",
            "1",
        ]
    ).std_out
    shard_records = yson.loads(shard_records)
    assert shard_records["rows"] == ["rec_1_1"]
    assert shard_records["offset_from"] == 1
    assert shard_records["offset_to"] == 1


def test_queue_trim(yt_queue):
    queue_path = yt_queue["path"]
    queue = yt_queue["queue"]

    consumers = ["bob", "alice", "ted"]
    for c in consumers:
        execute_cli(["consumer", "create", queue_path, c, "--ignore-in-trimming", "0"])

    writings = {
        "0": ["r1", "r2", "r3", "r4"],
        "1": ["r11", "r12"],
        "2": ["r21", "r22", "r23"],
    }

    execute_cli(["queue", "write", queue_path, json.dumps(writings)])
    consumer_offsets = queue.get_consumers_offsets(consumers, [0, 1, 2])
    # check initial offsets
    for _, offsets in consumer_offsets.items():
        assert [queue.null_offset] * 3 == offsets

    execute_cli(["consumer", "update_offsets", queue_path, "bob", "--shards", "[0, 1]", "--value", "0"])
    execute_cli(["consumer", "update_offsets", queue_path, "alice", "--shards", "[0, 2]", "--value", "1"])
    execute_cli(["consumer", "update_offsets", queue_path, "ted", "--shards", "[0, 1, 2]", "--value", "2"])

    consumer_offsets = queue.get_consumers_offsets(consumers, [0, 1, 2])
    assert [0, 0, queue.null_offset] == consumer_offsets["bob"]
    assert [1, queue.null_offset, 1] == consumer_offsets["alice"]
    assert [2, 2, 2] == consumer_offsets["ted"]

    infos = queue.get_shard_infos()
    for x in infos:
        assert 0 == x["trimmed_row_count"]

    # trim (trims nothing because of large --retained-messages-per-shard option)
    execute_cli(["queue", "trim", queue_path, "--retained-messages-per-shard", "100"])
    infos = queue.get_shard_infos()
    for x in infos:
        assert 0 == x["trimmed_row_count"]

    # trim
    execute_cli(["queue", "trim", queue_path])

    infos = queue.get_shard_infos()
    assert 1 == infos[0]["trimmed_row_count"]
    assert 0 == infos[1]["trimmed_row_count"]
    assert 0 == infos[2]["trimmed_row_count"]

    # ignore offsets for bob
    execute_cli(["consumer", "update_options", queue_path, "bob", '{"IgnoreInTrimming": true}'])

    # trim (ignoring bob, but trims less because of --retained-messages-per-shard option)
    execute_cli(["queue", "trim", queue_path, "--retained-messages-per-shard", "2"])
    infos = queue.get_shard_infos()
    assert 2 == infos[0]["trimmed_row_count"]
    assert 0 == infos[1]["trimmed_row_count"]
    assert 1 == infos[2]["trimmed_row_count"]  # because of --retained-messages-per-shard option

    # trim (ignoring bob, trims all possible messages)
    execute_cli(["queue", "trim", queue_path])
    infos = queue.get_shard_infos()
    assert 2 == infos[0]["trimmed_row_count"]
    assert 0 == infos[1]["trimmed_row_count"]
    assert 2 == infos[2]["trimmed_row_count"]

    # check actual first available record records
    for shard, offset in [(0, 2), (1, 0), (2, 2)]:
        shard_records = yson.loads(
            execute_cli(
                [
                    "queue",
                    "read",
                    queue_path,
                    "--raw",
                    "--shard",
                    str(shard),
                    "--start-offset",
                    "0",
                    "--limit",
                    "1",
                ]
            ).std_out
        )
        assert writings[str(shard)][offset : offset + 1] == shard_records["rows"]

    stat = json.loads(execute_cli(["queue", "trim", queue_path, "--ignore-consumers"]).std_out)
    assert 5 == stat["trimmed_rows"]
    assert stat["total_shard_rows"] == stat["trimmed_shard_rows"]
    assert consumer_offsets == stat["consumers_offsets"]

    # trim all except last row
    infos = queue.get_shard_infos()
    assert 4 == infos[0]["trimmed_row_count"]
    assert 2 == infos[1]["trimmed_row_count"]
    assert 3 == infos[2]["trimmed_row_count"]

    for shard in [0, 1, 2]:
        shard_records = yson.loads(
            execute_cli(
                [
                    "queue",
                    "read",
                    queue_path,
                    "--raw",
                    "--shard",
                    str(shard),
                    "--start-offset",
                    "0",
                    "--limit",
                    "1024",
                ]
            ).std_out
        )
        assert [] == shard_records["rows"]


def test_queue_offsets_history(yt_queue):
    queue = yt_queue["queue"]
    shards = yt_queue["shards"]
    queue_path = yt_queue["path"]
    consumers = ["bob", "alice"]
    for c in consumers:
        execute_cli(["consumer", "create", queue_path, c, "--ignore-in-trimming", "0"])

    offsets = {c: [i] * shards for i, c in enumerate(consumers)}
    assert queue.try_acquire_service_lock()
    queue.save_consumers_offsets("1", offsets)
    queue.save_consumers_offsets("2", offsets)

    assert ["1", "2"] == queue.get_offsets_history()

    assert offsets == queue.get_consumers_offsets_from_history("1")
    queue.remove_consumers_offsets("1")
    assert ["2"] == queue.get_offsets_history()
    queue.remove_consumers_offsets("2")
    assert [] == queue.get_offsets_history()
    queue.release_service_lock()


def test_validate_queue_options(yt_ready_env):
    options = '{"OffsetsLength": 123, "DebugShards": [4], "RetainedMessagesPerShard": 5000}'
    with pytest.raises(Exception) as excinfo:
        bigrt_cli.parse_queue_options(options)
    assert 'has no field named "OffsetsLength"' in str(excinfo.value)

    options = (
        '{"OffsetsHistoryLength": 123, "DebugShards": [4], "RetainedMessagesPerShard": 5000, "IgnoreConsumers": false}'
    )
    parsed_options = bigrt_cli.parse_queue_options(options)
    assert isinstance(parsed_options["DebugShards"], list)
    assert isinstance(parsed_options["DebugShards"][0], int)
    assert isinstance(parsed_options["RetainedMessagesPerShard"], int)
    assert isinstance(parsed_options["OffsetsHistoryLength"], int)
    assert isinstance(parsed_options["IgnoreConsumers"], bool)
    assert parsed_options == {
        "DebugShards": [4],
        "OffsetsHistoryLength": 123,
        "RetainedMessagesPerShard": 5000,
        "IgnoreConsumers": False,
    }


def test_write_queue_options_on_create(request, yt_ready_env):
    options = (
        '{"OffsetsHistoryLength": 123, "DebugShards": [4], "RetainedMessagesPerShard": 5000, "IgnoreConsumers": false}'
    )
    queue_path = get_queue_path(request)
    create_queue_through_cli(queue_path, yt_ready_env, options)
    queue_options = json.loads(execute_cli(["queue", "get_options", queue_path]).std_out)
    options = bigrt_cli.parse_queue_options(options)
    assert queue_options == options


def test_use_default_queue_options_on_create(request, yt_ready_env):
    queue_path = get_queue_path(request)
    create_queue_through_cli(queue_path, yt_ready_env)
    queue_options = json.loads(execute_cli(["queue", "get_options", queue_path]).std_out)
    assert queue_options == {}


def test_write_empty_queue_options_on_create(request, yt_ready_env):
    options = "{}"
    queue_path = get_queue_path(request)
    create_queue_through_cli(queue_path, yt_ready_env, options)
    queue_options = json.loads(execute_cli(["queue", "get_options", queue_path]).std_out)
    assert queue_options == {}


def test_overwrite_queue_options_on_update(request, yt_ready_env):
    options = '{"OffsetsHistoryLength": 161, "DebugShards": [7]}'
    queue_path = get_queue_path(request)
    create_queue_through_cli(queue_path, yt_ready_env, options)
    queue_options = json.loads(execute_cli(["queue", "get_options", queue_path]).std_out)
    options = bigrt_cli.parse_queue_options(options)
    assert queue_options == options


def test_do_not_overwrite_queue_options_on_update(request, yt_ready_env):
    options = '{"OffsetsHistoryLength": 161, "DebugShards": [7]}'
    queue_path = get_queue_path(request)
    create_queue_through_cli(queue_path, yt_ready_env, options)
    execute_cli(["queue", "update", queue_path, "--medium", "default", "--max-ttl", "100000"])
    queue_options = json.loads(execute_cli(["queue", "get_options", queue_path]).std_out)
    options = bigrt_cli.parse_queue_options(options)
    assert queue_options == options


def test_clear_queue_options_on_update(request, yt_ready_env):
    options = '{"OffsetsHistoryLength": 161, "DebugShards": [7]}'
    queue_path = get_queue_path(request)
    create_queue_through_cli(queue_path, yt_ready_env, options)
    new_options = "{}"
    execute_cli(
        [
            "queue",
            "update",
            queue_path,
            "--medium",
            "default",
            "--max-ttl",
            "100000",
            "--queue-options",
            new_options,
        ]
    )
    queue_options = json.loads(execute_cli(["queue", "get_options", queue_path]).std_out)
    assert queue_options == {}

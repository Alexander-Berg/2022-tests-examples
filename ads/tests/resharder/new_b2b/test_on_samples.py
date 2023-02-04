from __future__ import print_function

import json
import logging
import os
import re
import string
import sys
import time

from datetime import datetime, timedelta, timezone

import pytest
import yatest.common
from ads.bsyeti.big_rt.py_test_lib import (
    FileSupplierInput,
    create_yt_queue,
    launch_bullied_processes,
    make_json_file,
    make_namedtuple,
)

from ads.bsyeti.big_rt.py_lib.events import Event
from ads.bsyeti.big_rt.py_test_lib.logsampler.utils.utils import filter_sources_by_samples, read_sample
import ads.bsyeti.big_rt.py_test_lib.resharder as resharder_common
from ads.bsyeti.tests.tools.buzzard_logsampler.config.sources import get_sources
from ads.bsyeti.tests.tools.buzzard_logsampler.config.config import config as samplers_config
from ads.bsyeti.libs.py_test_yt.helpers import prepare_dynamic_table
from library.python.framing.unpacker import Unpacker
import yt.wrapper as yt


# Can be enabled with --test-param sharder_path="/home/.../sharding/sharding"
# It is useful for tests with sanitizers applied only for big_rt binary
BINARY_PATH = yatest.common.get_param("sharder_path") or yatest.common.binary_path("ads/bsyeti/resharder/bin/resharder")

# To run without identification requester use: --test-param disable_identification_requester=1
# To collect vulture dump for using in normal mode use:
#   --test-param production_vulture_table_cluster=seneca-sas --test-param dump_vulture_yt_requests_path=<path>
DISABLE_IDENTIFICATION_REQUESTER = bool(int(yatest.common.get_param("disable_identification_requester", 0)))
PRODUCTION_VULTURE_TABLE_CLUSTER = yatest.common.get_param("production_vulture_table_cluster")
VULTURE_YT_REQUESTS_DUMP_PATH = yatest.common.get_param("dump_vulture_yt_requests_path")

VULTURE_TABLE_PATH = "//home/bigb/production/VultureCrypta"

not_printable_pattern = re.compile(f"[^{re.escape(string.printable)}]")


def replace_not_printable(s):
    return not_printable_pattern.sub("?", s.decode("utf-8", "replace"))


def get_destination(yt_cluster, yt_path, output_shards):
    return {
        "DestinationName": "default",
        "ShardsCount": output_shards,
        "ReshardingMode": "RM_MODULE",
        "ReshardingHashFunction": "BigbHash",
        "Writer": {
            "Queues": [
                {
                    "Cluster": yt_cluster,
                    "Path": yt_path,
                }
            ],
        },
    }


def prepare_vulture_table(yt_client):
    with open("./vulture.log", "rb") as f:
        log_records = list(yt.yson.parser.load(f, yson_type="list_fragment"))
    state = {}
    for record in log_records:
        request = record.get("request")
        if request is None or request.get("path") != VULTURE_TABLE_PATH:
            continue
        for row in request["result"]:
            if not row:
                continue
            logging.info("FFF %r", row)
            state.setdefault(str(row["UniqId"]), row)
    prepare_dynamic_table(
        yt_client=yt_client,
        table=VULTURE_TABLE_PATH,
        # TODO: Use table schema from common code/config.
        schema=[
            {
                "name": "Hash",
                "expression": "bigb_hash(UniqId) % 768",
                "type": "uint64",
                "sort_order": "ascending",
            },
            {"name": "UniqId", "type": "string", "sort_order": "ascending"},
            {"name": "CompressedInfo", "type": "string"},
            {"name": "EraTs", "type": "uint64"},
            {"name": "RtTs", "type": "uint64"},
        ]
    )
    yt_client.insert_rows(
        VULTURE_TABLE_PATH,
        state.values(),
        format="yson",
    )


@pytest.fixture()
def stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    input_file_path_prefix = os.path.join(yatest.common.output_path(), "input_{}".format(test_id))
    output_shards_count = 2
    output_queue_path = "//tmp/output_queue_" + test_id
    consuming_system_path_prefix = "//tmp/test_consuming_system_" + test_id
    master_path = "//tmp/master_state_" + test_id

    output_yt_queue = create_yt_queue(
        standalone_yt_cluster.get_yt_client(), output_queue_path, output_shards_count, commit_order="strong"
    )

    sources = filter_sources_by_samples(samplers_config, get_sources())

    # Time is hard coded because it is not used in any 'if' statements.
    os.environ["Y_TEST_FIXED_TIME"] = datetime(year=2022, month=1, day=1, tzinfo=timezone.utc).strftime(
        "%Y-%m-%dT%H:%M:%SZ"
    )

    prepare_vulture_table(standalone_yt_cluster.get_yt_client())

    return make_namedtuple("ResharderTestStand", **locals())


def make_exactly_once_test_config(stand):
    conf = {
        "Logs": {
            "Rules": [
                {
                    "FilePath": os.path.join(yatest.common.output_path(), "error.log"),
                    "MinLevel": "Debug",
                    "IncludeCategories": ["Main", "BigRT", "Http"],
                },
                {
                    "FilePath": os.path.join(yatest.common.output_path(), "yt.log"),
                    "MinLevel": "Info",
                    "ExcludeCategories": ["AUTO"],
                },
            ]
        },
        "YtCluster": os.environ["YT_PROXY"],
        "ResourcesReloaderConfig": {
            "ReloadPeriodS": 300,
            "SelectTypeFile": "./bsyeti-configs/select_type.json",
            "AbExperimentsLongConfig": "./just_default_and_predefault.json",
        },
        "HttpServer": {
            "Port": stand.port_manager.get_port(),
        },
        "HeavyWorkerThreads": 2,
        "SharedTransactionPeriod": 100,
        "Sources": list(
            resharder_common.format_resharder_sources(
                stand.sources, file_supplier_path_prefix=stand.input_file_path_prefix
            ).values()
        ),
        "Destinations": [
            get_destination(
                yt_cluster=os.environ["YT_PROXY"],
                yt_path=stand.output_queue_path,
                output_shards=stand.output_shards_count,
            )
        ],
    }

    if not DISABLE_IDENTIFICATION_REQUESTER:
        if PRODUCTION_VULTURE_TABLE_CLUSTER:
            vulture_cluster = "seneca-sas"
            yt_token_env = "VULTURE_YT_TOKEN"
        else:
            vulture_cluster = os.environ["YT_PROXY"]
            yt_token_env = ""  # use common local yt token
        conf["IdentificationConfig"] = {
            "Table": VULTURE_TABLE_PATH,
            "YtTokenEnv": yt_token_env,
            "TimeoutMs": 150,
            "MaxCacheDurationMs": timedelta(hours=1).total_seconds() * 1000,
            "Retries": 10,
            "HedgingClientConfig": {
                "Clients": [
                    {
                        "ClientConfig": {"ChannelPoolSize": 9, "ClusterName": vulture_cluster, "EnableRetries": True},
                    }
                ]
            }
        }
        if VULTURE_YT_REQUESTS_DUMP_PATH:
            conf["IdentificationConfig"]["LookupLogPath"] = VULTURE_YT_REQUESTS_DUMP_PATH

    exp_file = conf["ResourcesReloaderConfig"]["AbExperimentsLongConfig"]
    assert os.path.isfile(exp_file), "'%s' not exists" % exp_file
    return make_namedtuple(
        "ResharderConfig",
        path=make_json_file(
            json.dumps(conf, indent=4, sort_keys=True), name_template=f"resharder_config_{stand.test_id}.json"
        ),
    )


def resharder_launch_one_process_stable(stand, run_until_func):
    configs = [make_exactly_once_test_config(stand)]
    sharders = [resharder_common.ReshardingProcess(BINARY_PATH, config.path) for config in configs]
    launch_bullied_processes(
        sharders,
        run_until_func=run_until_func,
        restart_randmax=15,
        problems_max_count=1,
        timeout=400,
    )


def prepare_file_suppliers(stand):
    """Returns function checking that all suppliers read all data."""
    checkers = []
    for source in stand.sources:
        lines = read_sample(samplers_config, source)
        supplier_input = FileSupplierInput(path=os.path.join(stand.input_file_path_prefix, source), data={0: lines})
        checkers.append(supplier_input.check_read)

    def checker(checkers=checkers):
        while len(checkers) > 0:
            if checkers[0]():
                checkers.pop(0)  # Do not repeat successful checks.
            else:
                return False
        return True

    return checker


def read_all_data_from_queue(stand):
    """
    If we know that all data was commited, but don't know that it is available
    we can write markers and repeat reading all rows until we find our markers.
    Strong commit order protects us from seeing markers before any other message.
    """
    queue = stand.output_yt_queue
    marker = f"__QUEUE_END_MARKER_{stand.test_id}__"
    marker_data = {input_shard: [marker] for input_shard in range(queue["shards"])}
    queue["queue"].write(marker_data)  # Rely on strong commit order.

    result = {}

    for attempt in range(30):
        success = True
        for shard in range(queue["shards"]):
            shard_result = queue["queue"].read(shard, 0, 10**7)
            assert shard_result["offset_from"] == 0, " shard=%d" % shard
            rows = shard_result["rows"]
            last_row = rows[-1]
            if last_row.decode("utf-8") != marker:
                logging.info("Written markers are unavailable yet %s != %s", marker, last_row[: len(marker) + 10])
                success = False
                break
            result[shard] = rows[: len(rows) - 1]  # cut last row (marker)
        if success:
            return result
        time.sleep(1)
    raise Exception("Can't read all messages from queue")


@pytest.mark.parametrize(
    "resharder_launcher",
    [resharder_launch_one_process_stable],
)
def test_exactly_once(stand, resharder_launcher):
    logging.basicConfig(
        format="%(filename)s[LINE:%(lineno)d]# %(levelname)-8s [%(asctime)s]  %(message)s",
        level=logging.INFO,
    )
    logging.info("Start test")
    print(f"Output dir: {yatest.common.output_path()} cwd: {os.getcwd()}", file=sys.stderr)

    run_until_func = prepare_file_suppliers(stand)

    logging.info("Input file suppliers was prepared")

    resharder_launcher(stand=stand, run_until_func=run_until_func)
    logging.info("Resharder launcher finished")

    raw_data = read_all_data_from_queue(stand)
    all_rows = []
    profile_id_per_shard = {}
    for shard, rows in raw_data.items():
        profile_ids = profile_id_per_shard.setdefault(shard, [])
        for raw_row in rows:
            unpacker = Unpacker(raw_row)
            while True:
                tmp_msg, skip_data = unpacker.next_frame()
                if tmp_msg is None:
                    break
                event = Event(tmp_msg)
                row = replace_not_printable(event.as_json(True))
                try:
                    row_json = json.loads(row)
                except:
                    logging.error("Bad message: %s", repr(row))
                    raise
                all_rows.append((row, row_json))
                profile_ids.append(row_json["ProfileID"])
        profile_ids.sort()

    def extract_key(row):
        row_json = row[1]
        return (row_json["Type"], row_json["TimeStamp"], row_json["ProfileID"], row[0])

    all_rows.sort(key=extract_key)

    result = {
        "profile_id_per_shard": profile_id_per_shard,
        "all_rows": [r[1] for r in all_rows],
    }

    fname = os.path.join(yatest.common.output_path(), "result.json")
    with open(fname, "w") as f:
        json.dump(result, f, sort_keys=True, indent=4)

    return yatest.common.canonical_file(fname)

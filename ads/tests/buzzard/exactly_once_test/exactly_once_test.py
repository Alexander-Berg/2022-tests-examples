from __future__ import print_function

import base64
import collections
import itertools
import json
import logging
import os
import random
import re
import sys
import time

import ads.bsyeti.libs.py_test_yt  # noqa
import jinja2
import pytest
import yatest.common
from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    create_yt_queue,
    execute_cli,
    launch_bullied_processes_reading_queue,
    make_json_file,
    make_namedtuple,
    suppressing_context,
    waiting_iterable,
)
from library.python import resource
from library.python.sanitizers import asan_is_on


with open(yatest.common.source_path("ads/bsyeti/tests/buzzard/exactly_once_test/data/row_template.txt")) as f:
    EVENT_LOG_STR = jinja2.Template(f.read().strip()).render(
        uniqid="0",
        eventtime=str(int(time.time())),
        typeid="1",
        countertype="2",
        pageid="354326",
    )


def format_resharded_event_log_str(uid):
    return json.dumps({"LogType": "eventmyd", "Uid": uid}) + "\t" + EVENT_LOG_STR


def extract_all_uniqids_from_yt(replica_cluster, profiles_table):
    for waiting_state in waiting_iterable(timeout=60, period=8):
        with suppressing_context(not waiting_state.is_last):
            uniq_ids = replica_cluster.get_yt_client().select_rows("UniqID from [{table}]".format(table=profiles_table))
            return list(sorted(row["UniqID"] for row in uniq_ids))


def clean_profiles_table(primary_cluster, replica_cluster, profiles_table):
    uniqs = extract_all_uniqids_from_yt(replica_cluster, profiles_table)
    primary_cluster.get_yt_client().delete_rows(
        profiles_table, [{"UniqID": uniq} for uniq in uniqs], require_sync_replica=True
    )


def clean_offsets_table(primary_cluster, offsets_table, shards_count):
    primary_cluster.get_yt_client().delete_rows(
        offsets_table,
        [{"ShardId": shard} for shard in range(shards_count)],
        require_sync_replica=True,
    )


@pytest.fixture()
def stand(
    request,
    fully_ready_yt,
    yt_ready_env,
    port_manager,
    profiles_table,
    offsets_table,
    config_test_default_enabled,
):
    if not asan_is_on():
        input_shards_count = 10
        data_part_length = 500
        restart_max_seconds = 40
    else:
        input_shards_count = 3
        data_part_length = 100
        restart_max_seconds = 100

    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    input_queue_path = "//tmp/input_queue_" + test_id
    consuming_system_path = "//home/bigb/test/buzzard_consuming_system_" + test_id

    yt_cluster_family = fully_ready_yt["clusters"]
    primary_cluster = yt_cluster_family[0]
    replica_cluster = yt_cluster_family[1]

    input_yt_queue = create_yt_queue(primary_cluster.get_yt_client(), input_queue_path, input_shards_count)

    queue_consumer = "buzzard"
    execute_cli(["consumer", "create", input_yt_queue["path"], queue_consumer, "--ignore-in-trimming", "0"])

    clean_profiles_table(primary_cluster, replica_cluster, profiles_table)
    clean_offsets_table(primary_cluster, offsets_table, input_shards_count)

    return make_namedtuple("SimpleShardingTestStand", **locals())


def load_profiles(replica_cluster, profiles_table):
    uniqs = extract_all_uniqids_from_yt(replica_cluster, profiles_table)
    logging.info("total uniqs: %d" % len(uniqs))
    dumper_cmd = [
        yatest.common.binary_path("ads/bsyeti/tools/yt_profiles_dumper/yt_profiles_dumper"),
        "--yt-cluster",
        replica_cluster.get_proxy_address(),
        "--yt-table",
        profiles_table,
    ]
    assert len(uniqs) > 0
    for uniq in uniqs:
        dumper_cmd.extend(["--uid", uniq])
    result = yatest.common.execute(dumper_cmd)
    json_bytes = result.stdout  # subprocess.check_output(dumper_cmd)
    json_str = json_bytes.decode("ascii", errors="backslashreplace").replace(r"\x", r"\\x").replace(r"\u", r"\\u")
    return json.loads(json_str)


def gen_shard_testing_schemas(length):
    for cnt in range(3, 3000, 3):
        yield [i % cnt for i in range(length)]
        yield [random.randint(0, cnt - 1) for i in range(length)]


def gen_testing_shard_uids(shards_count, length):
    schemas = itertools.islice(gen_shard_testing_schemas(length), shards_count)

    data = {shard: ["y%d" % ((u + 1) * shards_count + shard) for u in schema] for shard, schema in enumerate(schemas)}
    test_uids = collections.Counter(uid for uids in data.values() for uid in uids)

    flag_uids = ["y%d" % int(shards_count * 1e9 + shard) for shard in range(shards_count)]
    for i, flag_uid in enumerate(flag_uids):
        data[i].append(flag_uid)
    return data, dict(test_uids), set(flag_uids)


def gen_hedging_client_config(yt_cluster):
    return {
        "BanDuration": 50,
        "BanPenalty": 3,
        "Clients": [
            {
                "ClientConfig": {
                    "ChannelPoolSize": 9,
                    "ClusterName": yt_cluster,
                    "EnableRetries": True,
                },
                "InitialPenalty": 0,
            }
        ],
    }


def buzzard_config(stand, cache_hi, cache_lo, cold_cache_size, worker, enable_async_commit, last_epochs_store_count):
    conf = {
        "CollectorsConfig": {
            "BroadMatchSettings": {},
            # disable collectors writing to logbroker and requesting to broadmatch
            "Disabled": [
                "TFactorLogClickCollector",
                "TEcomFactorLogCollector",  # -
                "TFactorLogCollector",
                "TProfileRegularLogCollector",  # -
                "TSendRedirCollector",  # -
                "TBroadMatchCollector",  # -
                "TGeminiOfferExtractorCollector",  # -
            ],
            "DJUnityProcessingConfig": {
                "AnswersRedirLogConfigPath": yatest.common.source_path(
                    "dj/unity/projects/answers/redir_log_actions/redir_log_to_actions_bigrt_config.pbtxt"
                ),
                "MarketFrontEventsConfigPath": yatest.common.source_path(
                    "dj/unity/projects/market/front_events_to_actions_bigrt_config.pbtxt"
                ),
                "MarketPersHistoryConfigPath": yatest.common.source_path(
                    "dj/unity/projects/market/pers_history_to_actions_bigrt_config.pbtxt"
                ),
            },
        },
        "DataConfig": {
            "InfoKeeperSettings": {
                "ActiveGoalsFile": "./bigb-bases-buzzard-hourly/bigb-bases/active_goals.vinyl",
                "ActiveSelectTypeFile": "./bsyeti-configs/active_select_types.json",
                "BmCategoryFile": "./bsyeti-bases/bm_category_data.json",
                "CounterInfoFile": "./bsyeti-configs/counter_info.json",
                "KeywordInfoFile": "./bsyeti-configs/keyword_info.json",
                "LemmerCacheSize": 8192,
                "LinearDumpPath": "",
                "MatrixnetDumpsFolder": "",
                "MxModelsFolder": "",
                "PagesFile": "./bsyeti-configs/page.json",
                "SelectTypeFile": "",
                "WordStatFile": "./bsyeti-bases/wordstat_hash.bin",
                "CodecFolder": "./dicts_cache/",
                "NeedLockResources": False,
                "MobileAppsBaseFile": "./bsyeti-mobile-apps/mobile_apps_v1.vinyl",
                "MobileAppsDomainFile": "./bsyeti-mobile-apps/mobile_apps_domains_v1.vinyl",
                "IncrementalDssmCtrModel": "./incremental_dssm/model",
                "BannerAppsBaseFile": "./bigb-bases-buzzard-daily/bigb-bases/banner_app.vinyl",
            },
        },
        "RandomSleepAtStartMaxMs": 0,
        "FlushPeriodMs": 50,
        "EnableAsyncCommit": enable_async_commit,
        "LastEpochsStoreCount": last_epochs_store_count,
        "GraphThreads": 16,
        "IdentificationConfig": {
            "HedgingClientConfig": gen_hedging_client_config(stand.replica_cluster.get_proxy_address())
        },
        "BannedHostsPath": "//home/bigb/test/buzzard_banned_hosts",
        "ConsumingSystem": {
            "Cluster": stand.primary_cluster.get_proxy_address(),
            "MainPath": stand.consuming_system_path,
            "Shards": {
                "Range": {
                    "Begin": 0,
                    "End": stand.input_yt_queue["shards"],
                }
            },
            "MaxShards": stand.input_yt_queue["shards"],
            "MaxShardsToCheck": 2,
            "WorkerMinorName": worker,
            "LaunchShardProcessorInFiber": False,
            "MasterBalancing": {"MasterPath": "//home/bigb/test/buzzard_master_balancing"},
        },
        "SuppliersChunkType": "",
        "Suppliers": [
            {
                "Alias": "resharded_yt_events_log",
                "YtSupplier": {
                    "Cluster": stand.primary_cluster.get_proxy_address(),
                    "QueuePath": stand.input_yt_queue["path"],
                    "QueueConsumer": stand.queue_consumer,
                    "MaxOutChunkSize": 5,
                },
            }
        ],
        "ShardProcessorConfig": {
            "MaxQueue": 0,
            "MaxTasks": 100,
            "MaxUids": 10,  # corresponds with shard testing schemas
            "ProfilesTable": stand.profiles_table,
            "ShardContextConfig": {
                "CacheHi": cache_hi,
                "CacheLo": cache_lo,
                "MaxColdCacheSize": cold_cache_size,
            },
            "SubShards": 8,
        },
        "ShardsNumber": stand.input_yt_queue["shards"],
        "StandVersion": "i",
        "LogWriter": {},
        "YtConfig": {
            "Master": stand.primary_cluster.yt_id,
            "Replicas": [stand.replica_cluster.yt_id],
            "DictsHedgingClientConfig": gen_hedging_client_config(stand.replica_cluster.get_proxy_address()),
        },
        "Logs": {
            "Rules": [
                {
                    "MinLevel": "Debug",
                    "IncludeCategories": ["Main", "BigRT", "Http"],
                },
                {
                    "MinLevel": "Debug",
                    "ExcludeCategories": ["AUTO"],
                },
            ]
        },
    }
    return make_namedtuple("BuzzardConfig", path=make_json_file(conf, name_template="buzzard_config_{json_hash}.json"))


class BuzzardProcess(BulliedProcess):
    def __init__(self, config_path):
        env = dict(os.environ)
        encrypted_key = base64.b64encode(resource.find("keyboard_test_private_key")).decode("utf-8")
        env.update(
            {
                "KEYBOARD_DECRYPTOR_KEY": encrypted_key,
            }
        )
        super(BuzzardProcess, self).__init__(
            launch_cmd=[yatest.common.binary_path("ads/bsyeti/buzzard/buzzard"), "--config-json", config_path],
            env=env,
        )


def stateful_launch_k_process(
    stand,
    data,
    k,
    stable=True,
    enable_async_commit=True,
    timeout=600,
    cache_hi=6,
    cache_lo=3,
    cold_cache_size=0,
    last_epochs_store_count=2,
):
    configs = [
        buzzard_config(
            stand,
            cache_hi=cache_hi,
            cache_lo=cache_lo,
            cold_cache_size=cold_cache_size,
            worker=str(worker),
            enable_async_commit=enable_async_commit,
            last_epochs_store_count=last_epochs_store_count,
        )
        for worker in range(k)
    ]
    processes = [BuzzardProcess(config.path) for config in configs]
    restart_randmax = None if stable else stand.restart_max_seconds
    launch_bullied_processes_reading_queue(
        processes,
        stand.input_yt_queue,
        stand.queue_consumer,
        data,
        restart_randmax=restart_randmax,
        timeout=timeout,
    )


def buzzard_launch_one_process_stable(**args):
    stateful_launch_k_process(k=1, **args)


def buzzard_launch_one_process_unstable(**args):
    stateful_launch_k_process(k=1, stable=False, **args)


def buzzard_launch_two_process_stable(**args):
    stateful_launch_k_process(k=2, **args)


def buzzard_launch_two_process_unstable(**args):
    stateful_launch_k_process(k=2, stable=False, **args)


def buzzard_launch_one_process_stable_no_async_commit(**args):
    stateful_launch_k_process(k=1, enable_async_commit=False, **args)


def buzzard_launch_one_process_stable_cold_cache(**args):
    stateful_launch_k_process(k=1, cache_hi=0, cache_lo=0, cold_cache_size=42000000, **args)


def buzzard_launch_two_process_unstable_cold_cache(**args):
    stateful_launch_k_process(k=2, cache_hi=0, cache_lo=0, cold_cache_size=500, stable=False, **args)


def buzzard_launch_one_process_stable_store_3_epochs(**args):
    stateful_launch_k_process(k=1, cold_cache_size=0, last_epochs_store_count=3, **args)


def buzzard_launch_two_process_unstable_store_3_epochs(**args):
    stateful_launch_k_process(k=2, cold_cache_size=0, last_epochs_store_count=3, stable=False, **args)


@pytest.mark.parametrize(
    "buzzard_launcher",
    [
        buzzard_launch_one_process_stable,
        buzzard_launch_one_process_unstable,
        buzzard_launch_two_process_stable,
        buzzard_launch_two_process_unstable,
        buzzard_launch_one_process_stable_no_async_commit,
        buzzard_launch_one_process_stable_cold_cache,
        buzzard_launch_two_process_unstable,
        buzzard_launch_one_process_stable_store_3_epochs,
        buzzard_launch_two_process_unstable_store_3_epochs,
    ],
)
def test_buzzard(stand, buzzard_launcher):
    print(">>> OUTPUT DIR: " + yatest.common.output_path(), file=sys.stderr)

    # generate and upload test data to yt
    shard_uids, test_uids, flag_uids = gen_testing_shard_uids(
        shards_count=stand.input_yt_queue["shards"], length=stand.data_part_length
    )
    data = {shard: [format_resharded_event_log_str(uid=uid) for uid in uids] for shard, uids in shard_uids.items()}
    stand.input_yt_queue["queue"].write(data)

    # launch and wait buzzard
    buzzard_launcher(stand=stand, data=data)
    logging.info("Buzzard read all messages and was finished")

    # wait for data in yt
    for waiting_state in waiting_iterable(timeout=60):
        with suppressing_context(not waiting_state.is_last):
            profiles = load_profiles(stand.replica_cluster, stand.profiles_table)
            with open(os.path.join(yatest.common.output_path(), "profiles_dump.json"), "w") as f:
                json.dump(profiles, f, indent=4, sort_keys=True)
            assert flag_uids < set(profiles)
            break

    # extract 7 counter
    profiles_target_counter = {}
    for uniq, proto_pack in profiles.items():
        # because 2 - PSP_COUNTERS
        for counter in proto_pack["2"]["CounterPack"]:
            for i in range(len(counter["counter_ids"])):
                if counter["counter_ids"][i] == 7:
                    assert counter["keys"][0] == 0
                    profiles_target_counter[uniq] = counter["values"][i]["float_values"]["value"][0]

    with open(os.path.join(yatest.common.output_path(), "target_counter.json"), "w") as f_p:
        json.dump(profiles_target_counter, f_p, indent=4, sort_keys=True)

    # check counter
    exception = None
    errors = collections.defaultdict(int)
    for uid, expected_count in test_uids.items():
        shard = int(uid[1:]) % stand.input_yt_queue["shards"]
        try:
            assert uid in profiles
            assert profiles_target_counter[uid] == expected_count, "uid=%s (shard=%d)" % (
                uid,
                shard,
            )
        except Exception as e:
            errors[shard] += 1
            exception = e
    if exception is not None:
        logging.error("Errors in shards: %r", dict(errors))
        raise exception

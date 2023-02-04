import base64
import binascii
import collections
import json
import logging
import os
import time
import urllib.parse
from collections import defaultdict

import ads.bsyeti.libs.py_test_yt  # noqa
from library.python import resource
import pytest
import yatest.common
from ads.bsyeti.big_rt.py_test_lib import BulliedProcess, create_yt_queue, execute_cli
from ads.bsyeti.libs.py_test_yt import prepare_dynamic_table
from ads.bsyeti.libs.py_test_yt.load_yt_data import load_yt_data
from ads.bsyeti.tests.test_lib.data_collector.config import (
    ALL_YT_TABLES_CONFIG,
    BUZZARD_QYT_TESTDATA,
    BUZZARD_YT_DATA_DIR,
    TIMESTAMP_PATH,
    YT_INPUT_QUEUE_PATH,
    YT_URL_TO_OFFER_EXPORT_PATH,
)

from ads.bsyeti.caesar.libs.profiles.proto.mobile_app_pb2 import TMobileAppData

# from ads.bsyeti.libs.py_test_broad_match import broad_match_service
from ads.bsyeti.tests.test_lib.eagle_daemon import get_eagle_protos
from yatest.common.network import PortManager

MAX_MESSAGES_TO_WRITE = 5000
BUZZARD_CONSUMER = "buzzard"
INPUT_SHARDS_COUNT = 2  # this also used in ya.make
SHARD_TO_SHARD_NO = {
    444: 0,
    1337: 1,
}  # i guess there is more smart method to do this, but we'll do this later


@pytest.fixture(scope="module")
def fix_table_relocations(fully_ready_yt, ready_testdata):  # noqa
    replica_cluster = fully_ready_yt["clusters"][1]
    yt_client = replica_cluster.get_yt_client()

    def check_table_exists(table):
        return yt_client.exists(table)

    def check_table_empty(table):
        return 0 == len(list(yt_client.select_rows("* from [{table}]".format(table=table))))

    old_table = "//yabs/Dicts/StoreApplication"
    new_table = "//home/bigb/caesar/stable/MobileAppsDict"
    if not check_table_exists(new_table):
        schema = [
            {"name": "BundleId", "type": "string", "sort_order": "ascending"},
            {"name": "SourceID", "type": "uint32", "sort_order": "ascending"},
            {"name": "Data", "type": "string"},
        ]
        prepare_dynamic_table(yt_client, new_table, schema)

    if check_table_empty(new_table):
        if check_table_exists(old_table) and not check_table_empty(old_table):
            data_to_relocate = []
            for row in yt_client.select_rows("* from [{table}]".format(table=old_table)):
                new_row = {}
                new_row["BundleId"] = row["BundleID"]
                new_row["SourceID"] = 1 if row["StoreID"] else 2
                data = TMobileAppData()
                data.BMCategories[:] = [int(i) for i in json.loads(row["Category"])]
                data.MobileInterests[:] = [int(i) for i in json.loads(row["Goal"])]
                new_row["Data"] = data.SerializeToString()
                data_to_relocate.append(new_row)
            yt_client.insert_rows(new_table, data_to_relocate)
    return


@pytest.fixture(scope="module")
def ready_qyt(request, fully_ready_yt, yt_ready_env):
    primary_yt_cluster = fully_ready_yt["clusters"][0]
    return create_yt_queue(primary_yt_cluster.get_yt_client(), YT_INPUT_QUEUE_PATH, INPUT_SHARDS_COUNT)


@pytest.fixture(scope="module")
def buzzard_ready_queue_consumer(ready_qyt):
    execute_cli(["consumer", "create", ready_qyt["path"], BUZZARD_CONSUMER, "--ignore-in-trimming", "0"])


@pytest.fixture(scope="module")
def write_data_to_qyt(ready_qyt):
    to_write = defaultdict(list)
    messages_count = defaultdict(int)
    last_codec = "zstd_6"
    logging.info("Start writing testdata to qyt")
    with open(BUZZARD_QYT_TESTDATA, "rb") as qyt_testdata_file:
        bsize_packed = qyt_testdata_file.read(4)
        while bsize_packed:
            csize_packed = qyt_testdata_file.read(4)
            bsize = int(binascii.hexlify(bsize_packed), 16)
            blob = qyt_testdata_file.read(bsize)
            csize = int(binascii.hexlify(csize_packed), 16)
            codec = qyt_testdata_file.read(csize)
            shard_packed = qyt_testdata_file.read(4)
            shard = int(binascii.hexlify(shard_packed), 16)
            shard_no = SHARD_TO_SHARD_NO[shard]
            if blob and codec:  # message not empty
                if codec != last_codec or sum((len(v) for v in to_write.values()), 0) >= MAX_MESSAGES_TO_WRITE:
                    if to_write:
                        logging.info(
                            "Writing %d batches to qyt",
                            sum((len(v) for v in to_write.values()), 0),
                        )
                        ready_qyt["queue"].write(to_write, last_codec)
                        for shard, messages in to_write.items():
                            messages_count[shard] += len(messages)
                    to_write = defaultdict(list)
                    last_codec = codec
                to_write[shard_no].append(blob)
            bsize_packed = qyt_testdata_file.read(4)
    if to_write:
        logging.info("Writing %d batches to qyt", sum((len(v) for v in to_write.values()), 0))
        ready_qyt["queue"].write(to_write, last_codec)
        for shard, messages in to_write.items():
            messages_count[shard] += len(messages)
    logging.info("All testdata written to qyt")
    return messages_count


@pytest.fixture(scope="module")
def ready_testdata(fully_ready_yt, all_yt_created_tables, write_data_to_qyt):
    for cluster in fully_ready_yt["clusters"]:
        load_yt_data(BUZZARD_YT_DATA_DIR, cluster.get_yt_client())
    return {"qyt_messages": write_data_to_qyt}


@pytest.fixture(scope="module")
def testing_data_time():
    with open(TIMESTAMP_PATH) as file_pointer:
        return {"TEST_TIME": int(file_pointer.read())}


def gen_hedging_client_config(yt_cluster):
    return {
        "BanDuration": 50,
        "BanPenalty": 3,
        "Clients": [
            {
                "ClientConfig": {"ClusterName": yt_cluster, "EnableRetries": True},
                "InitialPenalty": 0,
            }
        ],
    }


def buzzard_config(yt_cluster_family, ready_qyt, port, time):
    # TODO: take settings from production config and make patch to work in tests
    primary_cluster = yt_cluster_family[0]
    replica_cluster = yt_cluster_family[1]

    # Its because local lb very slow and we want this test run fast.
    gemini_config = {"Enabled": False}
    broad_match_settings = {}

    # TODO(bulatman) unify yt cluster settings.
    # for YtConfig need to specify yt_id, which is alias of yt cluster (primary, master, etc)
    # for DataSources need to specify the cluster url (like localhost:1234),
    # because bigrt uses ytex/client directly to make connections

    conf = {
        "BallocOptions": {"AngryReclaimDivisor": 1000000, "SoftReclaimDivisor": 1000000},
        "BannedHostsPath": "//home/bigb/test/buzzard_banned_hosts",
        "CollectorsConfig": {
            "BroadMatchSettings": broad_match_settings,
            "Disabled": ["TBroadMatchCollector"],
            "GeminiConfig": gemini_config,
            # "PureDicts": [
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.arm",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.blr",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.bul",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.cze",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.eng",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.fre",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.ger",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.ind",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.ita",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.pol",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.por",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.rum",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.rus",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.spa",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.tur",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.ukr",
            #     "/storage/db-0/key_1/srvs/buzzard_85860ed47adcb1ffaf56837fed6d9b17/internal/pure_dicts_tgz/pure.lg.groupedtrie.uzb"
            # ],
            # "SimilarBannersSettings": {
            #     "ChunkSize": 1000,
            #     "TimeoutMs": 1000,
            #     "Url": "http://rsya-similar-banners.yandex.net:80/banners"
            # },
            # "SortPart": 0.5,
            # "YtDictTimeoutMilliSeconds": 200
            "CaesarBannerTable": "//home/bigb/caesar/stable/Banners",
            "CaesarPhraseTable": "//home/bigb/caesar/stable/Phrases",
            "OfferUrlDictTable": "//yabs/Dicts/ApplicationStoreData",
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
                "BayesianMediationModelPriorFile": "./bsyeti-bases/mediation_prior.json",
                "BmCategoryFile": "./bsyeti-bases/bm_category_data.json",
                "CatalogiaSettings": {
                    "NeuralSettings": {
                        "CategoriesPath": "./catalogia_neural_data/categories",
                        "ModelPath": "./catalogia_neural_data/model",
                    },
                    # "WebServiceSettings": {
                    #     "Attempts": 3,
                    #     "ChunkSize": 500,
                    #     "TimeoutMs": 8000,
                    #     "Url": "full://bscatalogia.yandex.ru/cgi-bin/get_phrases_categs.pl"
                    # }
                },
                "NeedLockResources": False,
                "ClidCatalogFile": "./bsyeti-configs/clid_catalog.json",
                "CodecFolder": "./dicts_cache/",
                "CounterInfoFile": "./bsyeti-configs/counter_info.json",
                "DistributionProductFile": "./bsyeti-configs/distrib_soft.json",
                "DomainMeansFile": "./bsyeti-bases/domain_means.json",
                "EComCountersFile": "./bigb-bases-buzzard-every-10-minutes/bigb-bases/ecom_counters.vinyl",
                "EntityExtractorDataPrefix": "./adv-machine-entity-extractor-dicts/index",
                "WordStatFile": "./bsyeti-bases/wordstat.sthash",
                # wordstat.sthash would be removed later
                # "WordStatFile": "./bsyeti-bases/wordstat_hash.bin",
                "GoalQualityDataFile": "./bigb-bases-buzzard-daily/bigb-bases/goal_quality.vinyl",
                "KeywordInfoFile": "./bsyeti-configs/keyword_info.json",
                "LemmerCacheSize": 8192,
                "LinearDumpPath": "./bsyeti-bases/lm_dumps",
                "MatrixnetDumpsFolder": "./bsyeti-bases/mx_dumps/",
                "MxModelsFolder": "./bsyeti-bases/mx-models/",
                "MeaningfulGoalsDataFile": "./bigb-bases-buzzard-hourly/bigb-bases/meaningful_goals.vinyl",
                "MobileAppsBaseFile": "./bsyeti-mobile-apps/mobile_apps_v1.vinyl",
                "MobileAppsDomainFile": "./bsyeti-mobile-apps/mobile_apps_domains_v1.vinyl",
                "OrderTypesDataFile": "./bsyeti-bases/order_types.sthash",
                "OrganizationDictPrefix": "./permalink_dict/permalink_dict",  # diff in prod
                "PagesFile": "./bsyeti-configs/page.json",
                "RobotsTxtFile": "./robots/opt/yacontext/data/robotstxt/yandexbot_robotstxt.dump",  # diff in prod
                "SelectTypeFile": "",
                "AbExperimentsLongConfig": "./bigb_ab_production_config.json",
                "SovetnikPathPrefix": "./sovetnik/sovetnik",  # diff in prod
                "TragicPhrasesFile": "./tragic/data/tragic/manual_phrases.json",  # diff in prod
                "UniwordFile": "./bigb-bases-buzzard-every-10-minutes/bigb-bases/uniword.vinyl",
                "UsedLmFeaturesFile": "./used_lm_features/data",
                "WhiteListFolder": "./bsyeti-bases/whitelists/",
                "BannerAppsBaseFile": "./bigb-bases-buzzard-daily/bigb-bases/banner_app.vinyl",
                "MarketSku2ModelMappingFile": "./bigb-bases-buzzard-daily/bigb-bases/market_sku_mapping.vinyl",
                "ContextDssmSettings": {
                    "BasePath": "./context_dssm/model",
                    "Model": "inference/quantized",
                    "Document": "vs_random",
                    "Index": "index/top_queries",
                    "IndexSearchNeighborhoodSize": 50,
                },
                "GeobaseFile": "./geodata6.bin",
                "NeedLockGeobaseMemory": False,
            }
        },
        "ConsumingSystem": {
            "Cluster": primary_cluster.get_proxy_address(),
            "MainPath": "//home/bigb/test/buzzard_consuming_system",
            "Shards": {
                "Range": {
                    "Begin": 0,
                    "End": INPUT_SHARDS_COUNT,
                }
            },
            "MaxShards": INPUT_SHARDS_COUNT,
            "MaxShardsToCheck": 2,
            "LaunchShardProcessorInFiber": False,
            "MasterBalancing": {"MasterPath": "//home/bigb/test/buzzard_master_balancing"},
        },
        "SuppliersChunkType": "bigb_events",
        "Suppliers": [
            {
                "Alias": "resharded_yt_bigb_events_log",
                "YtSupplier": {
                    "ChunkSize": 1000,  # we have big chunks, but because MaxUids=100 - we have flush after every chunk.
                    "MaxOutChunkSize": 1000,
                    "Cluster": primary_cluster.get_proxy_address(),
                    "QueueConsumer": BUZZARD_CONSUMER,
                    "QueuePath": ready_qyt["path"],
                },
            }
        ],
        "EnableAsyncCommit": True,
        "FlushPeriodMs": 0,
        "GraphThreads": 16,
        "IdentificationConfig": {"HedgingClientConfig": gen_hedging_client_config(replica_cluster.get_proxy_address())},
        "LastEpochsStoreCount": 3,
        "ShardProcessorConfig": {
            "MaxQueue": 0,
            "MaxTasks": 100,
            "MaxUids": 100,
            "OutputQueuesConfig": {"Queues": [{"Alias": "schindler_machine", "Path": YT_INPUT_QUEUE_PATH}]},
            "RowsInserterConfig": {"Tables": [{"Alias": "UrlToOfferExport", "Path": YT_URL_TO_OFFER_EXPORT_PATH}]},
            "ProfilesTable": ALL_YT_TABLES_CONFIG["Profiles"][0],
            "RemoteProfilesTables": [
                {
                    "Location": "ERPL_TEST",
                    "Path": ALL_YT_TABLES_CONFIG["RemoteTestProfiles"][0]
                }
            ],
            "ShardContextConfig": {"CacheHi": 6, "CacheLo": 3, "MaxColdCacheSize": 500},
            "SubShards": 8,
        },
        "ShardsNumber": INPUT_SHARDS_COUNT,
        "StandVersion": "buzzard_production",
        "LogWriter": {},
        "YtConfig": {
            "Master": primary_cluster.yt_id,
            "Replicas": [replica_cluster.yt_id],
            "DictsHedgingClientConfig": gen_hedging_client_config(replica_cluster.get_proxy_address()),
        },
        "HttpServer": {"Port": port},
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
        "CurrentTime": time,
    }
    conf_name = os.path.join(yatest.common.output_path(), "buzzard_config_{}.json".format(hash(json.dumps(conf))))

    with open(conf_name, "w") as f_p:
        json.dump(conf, f_p, indent=4)

    return collections.namedtuple("BuzzardConfig", ["path"])(path=conf_name)


class BuzzardProcess(BulliedProcess):
    def __init__(self, config_path, yt_rpc_proxy_conf_path):
        env = dict(os.environ)
        encrypted_key = base64.b64encode(resource.find("keyboard_test_private_key")).decode("utf-8")
        env.update(
            {
                "YT_USER": "root",
                "YT_TOKEN": "yt_token",
                "YTRPC_CLUSTERS_CONFIG": yt_rpc_proxy_conf_path,
                "KEYBOARD_DECRYPTOR_KEY": encrypted_key,
            }
        )
        super(BuzzardProcess, self).__init__(
            launch_cmd=[
                yatest.common.binary_path("ads/bsyeti/buzzard/buzzard"),
                "--config-json",
                config_path,
            ],
            env=env,
        )


@pytest.yield_fixture(scope="module")  # noqa
def launch_buzzard(
    request,
    fully_ready_yt,
    all_yt_created_tables,
    ready_qyt,
    ready_testdata,
    testing_data_time,
    buzzard_ready_queue_consumer,
    config_test_default_enabled,
    fix_table_relocations,
):
    with PortManager() as port_manager:
        config = buzzard_config(
            fully_ready_yt["clusters"], ready_qyt, port_manager.get_port(), testing_data_time["TEST_TIME"]
        )
        rpc_proxy_conf = fully_ready_yt["yt_rpc_proxy_conf_path"]
        with BuzzardProcess(config.path, rpc_proxy_conf) as proc:
            yield proc


def wait_all_qyt_data_read(buzzard, queue, timeout=900):
    logging.info("waiting buzzard to read all data in qyt")
    deadline = time.time() + timeout
    all_read = False
    while not all_read and time.time() < deadline:
        consumer_offsets = queue.get_consumer_offsets(BUZZARD_CONSUMER)
        # we check every 10 seconds
        # also buzzard can add new data to queue - so we also wait to see this data in total_row_count
        time.sleep(10)
        total_rows = [s["total_row_count"] for s in queue.get_shard_infos()]
        all_read = True
        for shard in range(INPUT_SHARDS_COUNT):
            should_be = total_rows[shard]
            current_offset = consumer_offsets[shard] + 1
            is_done = should_be == current_offset
            if not is_done:
                logging.info(
                    "shard %d: current offset is %d (wait until %d)",
                    shard,
                    current_offset,
                    should_be,
                )
            else:
                logging.info("shard %d read done", shard)
            all_read = all_read and is_done

        assert buzzard.running, "buzzard process is dead."

    assert all_read, "Buzzard haven't read all qyt data in avaliable period"
    logging.info("buzzard have read all data in qyt")


def extract_all_uniqids_from_yt(client):
    translate_map = (
        ("y", "bigb-uid"),
        ("p", "puid"),
        ("gaid/", "gaid"),
        ("idfa/", "idfa"),
        ("uuid/", "uuid"),
        ("mmdi/", "device-id"),
        ("oaid/", "oaid"),
        ("mac/", "mac"),
        ("mem/", "mac-ext-md5"),
        ("duid/", "duid"),
        ("ifv/", "ifv"),
        ("mm_device_id/", "mm-device-id"),
    )

    uniqs = None

    for _ in range(5):
        try:
            uniqs = sorted(
                [
                    row["UniqID"]
                    for row in client.select_rows("UniqID from [{table}]".format(table=ALL_YT_TABLES_CONFIG["Profiles"][0]))
                ] + [
                    row["UniqID"]
                    for row in client.select_rows("UniqID from [{table}]".format(table=ALL_YT_TABLES_CONFIG["RemoteTestProfiles"][0]))
                ]
            )
        except Exception as e:
            logging.error("failed to select uniqids from ytlocal table: %s", e)
            time.sleep(10)

    if not uniqs:
        raise Exception("failed to select uids!")

    for i, e in enumerate(uniqs):
        found = False
        for pref, url_param in translate_map:
            if e.startswith(pref):
                found = True
                uniqs[i] = (url_param, e[len(pref) :])
                break
        assert found, "unknown prefix of uniq {uniq}".format(uniq=e)
    return uniqs


def dump_eagle_profiles(test_time, balancer_ports, replica_cluster, test_file_name):
    experiment_parameters = {
        "EagleSettings": {
            "GlueByDefault": False,
        }
    }

    url_body = (
        "/bigb"
        "?{{url_param}}={{uniq}}"
        "&strict=1"
        "&seed=100"
        "&time={time}"
        "&glue=0"
        "&timeout=10000"
        "&hit-log-id=1"
        "&format=protobuf"
        "&client=debug"
        "&exp-json={exp_json}"
    ).format(time=test_time, exp_json=urllib.parse.quote(json.dumps(experiment_parameters)))

    with open(test_file_name, "wb") as f_p:
        for index, port in enumerate(balancer_ports):
            url_template = "http://localhost:{port}".format(port=port)

            prepared_requests = [
                url_body.format(url_param=url_param, uniq=uniq)
                for url_param, uniq in extract_all_uniqids_from_yt(replica_cluster.get_yt_client())
            ]
            logging.info("total uniqs: %d", len(prepared_requests))

            test_case = test_file_name
            if index > 0:
                test_case += "_" + str(index)
            checkdata = get_eagle_protos(prepared_requests, url_template, test_case)

            f_p.write(checkdata)

    return


@pytest.yield_fixture(scope="module")  # noqa
def buzzard_finished(launch_buzzard, ready_qyt):
    # step one: we wait buzzard reads all qyt data for all partitions
    wait_all_qyt_data_read(launch_buzzard, ready_qyt["queue"])

    # step two: we wait another 15 seconds for buzzard to process last chunk and for replicatoin reasons
    time.sleep(15)  # tie up your camel


def test_buzzard(
    eagle_balancer_module,
    eagle_remote_balancer_module,
    fully_ready_yt,
    all_yt_created_tables,
    testing_data_time,
    buzzard_finished,
):
    # step three: we take all uniqs from yt and ask eagle for them
    test_time = testing_data_time["TEST_TIME"]
    balancer_port = eagle_balancer_module["BALANCER_PORT"]
    remote_balancer_port = eagle_remote_balancer_module["BALANCER_PORT"]
    replica_cluster = fully_ready_yt["clusters"][1]
    test_file_name = "eagle_answers.json"
    dump_eagle_profiles(test_time, [balancer_port, remote_balancer_port], replica_cluster, test_file_name)

    # step four: check the result
    diff_tool = [yatest.common.binary_path("ads/bsyeti/tests/test_lib/eagle_compare/bin/compare")]
    return yatest.common.canonical_file(test_file_name, diff_tool=diff_tool, diff_tool_timeout=600)

import os

import yatest.common
from ads.bsyeti.libs.py_test_yt import prepare_replicated_table, prepare_rpc_config


def test_yt_saveload(yt_cluster_family):
    primary_cluster = yt_cluster_family[0]
    replica_cluster = yt_cluster_family[1]
    table = "//test/Profiles"
    schema = [
        {
            "name": "Hash",
            "expression": "farm_hash(UniqID) % 768",
            "type": "uint64",
            "sort_order": "ascending",
        },
        {"name": "UniqID", "type": "string", "sort_order": "ascending"},
        {"name": "CodecID", "type": "uint64"},
        {"name": "Main", "type": "string"},
        {"name": "MainPatch", "type": "string"},
        {"name": "UserItems", "type": "string"},
        {"name": "UserItemsPatch", "type": "string"},
        {"name": "Counters", "type": "string"},
        {"name": "CountersPatch", "type": "string"},
        {"name": "Applications", "type": "string"},
        {"name": "ApplicationsPatch", "type": "string"},
        {"name": "Banners", "type": "string"},
        {"name": "BannersPatch", "type": "string"},
        {"name": "Dmps", "type": "string"},
        {"name": "DmpsPatch", "type": "string"},
        {"name": "Queries", "type": "string"},
        {"name": "QueriesPatch", "type": "string"},
        {"name": "Aura", "type": "string"},
        {"name": "AuraPatch", "type": "string"},
        {"name": "DjProfiles", "type": "string"},
        {"name": "DjProfilesPatch", "type": "string"},
    ]

    prepare_replicated_table(table, schema, primary_cluster, [replica_cluster], "async")

    yt_config_path = os.path.join(yatest.common.output_path(), "yt_rpc_proxy.cfg")
    prepare_rpc_config(yt_cluster_family, yt_config_path)

    env = {}
    env["YTRPC_CLUSTERS_CONFIG"] = yt_config_path
    env["YT_USER"] = "root"
    env["YT_TOKEN"] = "yt_token"
    yatest.common.execute(
        [
            yatest.common.binary_path("ads/bsyeti/libs/yt_storage/tests/manual_test_runner/manual_test_runner"),
            "--master",
            primary_cluster.yt_id,
            "--replicas",
            replica_cluster.yt_id,
            "--table",
            table,
            "--counters",
            "./bsyeti-configs/counter_info.json",
        ],
        env=env,
        check_exit_code=True,
    )

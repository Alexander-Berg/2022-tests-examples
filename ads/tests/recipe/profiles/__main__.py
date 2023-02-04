import argparse

from ads.bsyeti.libs.py_test_yt import prepare_dynamic_table, prepare_replicated_table
from yt.recipe.multi_cluster.lib import get_yt_cluster
from library.python.testing import recipe

_TABLES = [
    {
        "name": "Profiles",
        "schema": [
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
        ],
    },
    {
        "name": "VultureCrypta",
        "schema": [
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
        ],
    },
    {
        "name": "CryptaExperimental",
        "schema": [
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
        ],
    },
    {
        "name": "Cookies",
        "schema": [
            {
                "name": "Hash",
                "expression": "bigb_hash(UniqId) % 768",
                "type": "uint64",
                "sort_order": "ascending",
            },
            {"name": "UniqId", "type": "string", "sort_order": "ascending"},
            {"name": "KeywordId", "type": "uint64", "sort_order": "ascending"},
            {"name": "ValueId", "type": "uint64", "sort_order": "ascending"},
            {"name": "ItemId", "type": "uint64", "sort_order": "ascending"},
            {"name": "Data", "type": "string"},
            {"name": "UpdateTime", "type": "uint64"},
        ],
    },
    {
        "name": "SearchPers",
        "schema": [
            {
                "name": "Hash",
                "expression": "bigb_hash(Id) % 256",
                "type": "uint64",
                "sort_order": "ascending",
            },
            {"name": "Id", "type": "string", "sort_order": "ascending"},
            {"name": "Codec", "type": "uint64"},
            {"name": "State", "type": "string"},
            {"name": "StatePatch", "type": "string"},
            {"name": "CodecString", "type": "string"},
        ],
    },
    {
        "name": "SearchPersAlternative",
        "schema": [
            {
                "name": "Hash",
                "expression": "bigb_hash(Id) % 256",
                "type": "uint64",
                "sort_order": "ascending",
            },
            {"name": "Id", "type": "string", "sort_order": "ascending"},
            {"name": "Codec", "type": "uint64"},
            {"name": "State", "type": "string"},
            {"name": "StatePatch", "type": "string"},
            {"name": "CodecString", "type": "string"},
        ],
    },
]


def _create_tables(master, replicas, attributes):
    for table in _TABLES:
        name = table["name"]
        schema = table["schema"]
        path = "//test/%s" % name
        if replicas:
            prepare_replicated_table(path, schema, master, replicas, attributes=attributes)
        else:
            prepare_dynamic_table(master.get_yt_client(), path, schema, attributes)

        recipe.set_env("YT_TABLE_%s" % name.upper(), path)


def start(args):
    """recipe entry point (start services)."""
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--enable-balancer",
        action="store_true",
        default=False,
        help="enable tablet balancer (disabled by default)",
    )

    parsed_args, _ = parser.parse_known_args(args)

    if parsed_args.enable_balancer:
        additional_attributes = {}
    else:
        additional_attributes = {
            "tablet_balancer_config": {
                "enable_auto_reshard": False,
                "enable_auto_tablet_move": False,
            }
        }

    cluster = get_yt_cluster()
    _create_tables(cluster.primary_cluster, cluster.replica_clusters, additional_attributes)


def stop(_):
    """recipe entry point (stop services)."""
    pass


if __name__ == "__main__":
    recipe.declare_recipe(start, stop)

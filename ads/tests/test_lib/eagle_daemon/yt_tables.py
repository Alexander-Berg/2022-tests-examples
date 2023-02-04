import logging

import pytest
import yt.yson as yson
from ads.bsyeti.libs.py_test_yt import prepare_dynamic_table, prepare_replicated_table
from ads.bsyeti.tests.test_lib.data_collector.config import (
    ALL_YT_TABLES_CONFIG,
    PROFILES_SCHEMA,
    YT_URL_TO_OFFER_EXPORT_PATH,
    SEARCH_PERS_SCHEMA,
)

logging.getLogger("urllib3").setLevel(logging.CRITICAL)


def read_rows(rows_file):
    with open(rows_file, "rb") as f_p:
        for row in yson.load(f_p):
            row.pop("Hash")
            yield row


@pytest.fixture(scope="module")
def profiles_table(yt_cluster_family):
    primary_cluster = yt_cluster_family[0]
    replica_cluster = yt_cluster_family[1]

    alias = "Profiles"
    logging.info("loading %s table", alias)

    table, _, _ = ALL_YT_TABLES_CONFIG[alias]
    # temporary solution to add new column
    #
    # with open(schema_path, "rb") as file_pointer:
    #     schema = yson.load(file_pointer)
    prepare_replicated_table(table, PROFILES_SCHEMA, primary_cluster, replica_cluster)
    logging.info("load complete: %s table", alias)
    return table


@pytest.fixture(scope="module")
def remote_test_profiles_table(yt_cluster_family):
    # sorry for copypaste but code is bad
    primary_cluster = yt_cluster_family[0]
    replica_cluster = yt_cluster_family[1]

    alias = "RemoteTestProfiles"
    logging.info("loading %s table", alias)

    table, _, _ = ALL_YT_TABLES_CONFIG[alias]
    # temporary solution to add new column
    #
    # with open(schema_path, "rb") as file_pointer:
    #     schema = yson.load(file_pointer)
    prepare_replicated_table(table, PROFILES_SCHEMA, primary_cluster, replica_cluster)
    logging.info("load complete: %s table", alias)
    return table


@pytest.fixture(scope="module")
def vulture_crypta_table(yt_cluster_family):
    primary_cluster = yt_cluster_family[0]
    replica_cluster = yt_cluster_family[1]

    alias = "VultureCrypta"
    logging.info("loading %s table", alias)

    table, schema_path, _ = ALL_YT_TABLES_CONFIG[alias]
    with open(schema_path, "rb") as file_pointer:
        schema = yson.load(file_pointer)
        prepare_replicated_table(table, schema, primary_cluster, replica_cluster)
    logging.info("load complete: %s table", alias)
    return table


@pytest.fixture(scope="module")
def user_shows_table(yt_cluster_family):
    replica_cluster = yt_cluster_family[1]

    alias = "UserShows"
    logging.info("loading %s table", alias)

    table, schema_path, _ = ALL_YT_TABLES_CONFIG[alias]
    schema = [
        {
            "name": "UniqId",
            "sort_order": "ascending",
            "type": "string",
        },
        {
            "name": "KeywordId",
            "sort_order": "ascending",
            "type": "uint64",
        },
        {
            "name": "ItemId",
            "sort_order": "ascending",
            "type": "uint64",
        },
        {
            "name": "AggregationKey",
            "sort_order": "ascending",
            "type": "uint64",
        },
        {
            "aggregate": "max",
            "name": "MaxTs",
            "type": "uint64",
        },
        {
            "aggregate": "min",
            "name": "MinTs",
            "type": "uint64",
        },
        {
            "aggregate": "sum",
            "name": "Count",
            "type": "uint64",
        },
        {
            "name": "Data",
            "type": "string",
        },
    ]
    prepare_dynamic_table(replica_cluster.get_yt_client(), table, schema)
    logging.info("load complete: %s table", alias)
    return table


@pytest.fixture(scope="module")
def cookies_table(yt_cluster_family):
    replica_cluster = yt_cluster_family[1]

    alias = "Cookies"
    logging.info("loading %s table", alias)

    table, schema_path, _ = ALL_YT_TABLES_CONFIG[alias]
    with open(schema_path, "rb") as file_pointer:
        schema = yson.load(file_pointer)
        prepare_dynamic_table(replica_cluster.get_yt_client(), table, schema)
    logging.info("load complete: %s table", alias)
    return table


@pytest.fixture(scope="module")
def offsets_table(yt_cluster_family):
    primary_cluster = yt_cluster_family[0]

    alias = "Offsets"
    logging.info("loading %s table", alias)

    table, _, _ = ALL_YT_TABLES_CONFIG[alias]
    schema = [
        {"name": "ShardId", "type": "uint64", "sort_order": "ascending"},
        {"name": "State", "type": "string"},
    ]
    prepare_dynamic_table(primary_cluster.get_yt_client(), table, schema)
    logging.info("load complete: %s table", alias)
    return table


@pytest.fixture(scope="module")
def shindler_table(yt_cluster_family):
    replica_cluster = yt_cluster_family[1]

    alias = "CryptaReplicas"
    logging.info("loading %s table", alias)

    table, _, _ = ALL_YT_TABLES_CONFIG[alias]
    schema = [
        {"name": "key", "type": "string", "sort_order": "ascending"},
        {"name": "value", "type": "string"},
    ]
    prepare_dynamic_table(replica_cluster.get_yt_client(), table, schema)
    logging.info("load complete: %s table", alias)
    return table


@pytest.fixture(scope="module")
def search_pers_table(yt_cluster_family):
    replica_cluster = yt_cluster_family[1]

    alias = "SearchPers"
    logging.info("loading %s table", alias)

    table, _, __ = ALL_YT_TABLES_CONFIG[alias]

    schema = SEARCH_PERS_SCHEMA
    prepare_dynamic_table(replica_cluster.get_yt_client(), table, schema)

    logging.info("load complete: %s table", alias)
    return table


@pytest.fixture(scope="module")
def url_to_offer_export_table(yt_cluster_family):
    primary_cluster = yt_cluster_family[0]
    replica_cluster = yt_cluster_family[1]

    alias = "UrlToOfferExport"
    logging.info("loading %s table", alias)

    table = YT_URL_TO_OFFER_EXPORT_PATH
    schema = [
        {"name": "NormalizeUrl", "type": "string", "sort_order": "ascending"},
        {"name": "CounterID", "type": "int64", "sort_order": "ascending"},
        {"name": "OfferID", "type": "string"},
        {"name": "UpdateTime", "type": "int64"},
    ]
    prepare_replicated_table(table, schema, primary_cluster, replica_cluster, "async")
    logging.info("load complete: %s table", alias)
    return table


@pytest.fixture(scope="module")
def filled_profiles_table(yt_cluster_family, profiles_table):
    alias = "Profiles"
    logging.info("pushing data to %s table", alias)
    part_size = 30000
    data = list(read_rows(ALL_YT_TABLES_CONFIG[alias][2]))
    for part in range(0, len(data), part_size):
        logging.info("pushing rows to %s table: %d rows", alias, part_size)
        yt_cluster_family[0].get_yt_client().insert_rows(profiles_table, data[part : part + part_size])
        logging.info("%d rows to %s table pushed", part_size, alias)
    logging.info("Table %s filled with data: %d rows", alias, len(data))
    return {"table": profiles_table}


@pytest.fixture(scope="module")
def filled_vulture_crypta_table(yt_cluster_family, vulture_crypta_table):
    alias = "VultureCrypta"
    logging.info("pushing data to %s table", alias)
    part_size = 30000
    data = list(read_rows(ALL_YT_TABLES_CONFIG[alias][2]))
    for part in range(0, len(data), part_size):
        logging.info("pushing rows to %s table: %d rows", alias, part_size)
        yt_cluster_family[0].get_yt_client().insert_rows(vulture_crypta_table, data[part : part + part_size])
        logging.info("%d rows to %s table pushed", part_size, alias)
    logging.info("Table %s filled with data: %d rows", alias, len(data))
    return {"table": vulture_crypta_table}


@pytest.fixture(scope="module")
def filled_user_shows_table(yt_cluster_family, user_shows_table):
    alias = "UserShows"
    logging.info("pushing data to %s table", alias)
    part_size = 30000
    data = list(read_rows(ALL_YT_TABLES_CONFIG[alias][2]))
    for part in range(0, len(data), part_size):
        logging.info("pushing rows to %s table: %d rows", alias, part_size)
        yt_cluster_family[1].get_yt_client().insert_rows(user_shows_table, data[part : part + part_size])
        logging.info("%d rows to %s table pushed", part_size, alias)
    logging.info("table %s filled with data: %d rows", alias, len(data))
    return {"table": user_shows_table}


@pytest.fixture(scope="module")
def filled_cookies_table(yt_cluster_family, cookies_table):
    alias = "Cookies"
    logging.info("pushing data to %s table", alias)
    part_size = 30000
    data = list(read_rows(ALL_YT_TABLES_CONFIG[alias][2]))
    for part in range(0, len(data), part_size):
        logging.info("pushing rows to %s table: %d rows", alias, part_size)
        yt_cluster_family[1].get_yt_client().insert_rows(cookies_table, data[part : part + part_size])
        logging.info("%d rows to %s table pushed", part_size, alias)
    logging.info("Table %s filled with data: %d rows", alias, len(data))
    return {"table": cookies_table}


@pytest.fixture(scope="module")
def filled_search_pers_table(yt_cluster_family, search_pers_table):
    alias = "SearchPers"
    logging.info("pushing data to %s table", alias)
    part_size = 30000
    data = list(read_rows(ALL_YT_TABLES_CONFIG[alias][2]))
    for part in range(0, len(data), part_size):
        logging.info("pushing rows to %s table: %d rows", alias, part_size)
        yt_cluster_family[1].get_yt_client().insert_rows(search_pers_table, data[part : part + part_size])
        logging.info("%d rows to %s table pushed", part_size, alias)
    logging.info("Table %s filled with data: %d rows", alias, len(data))
    return {"table": search_pers_table}


@pytest.fixture(scope="module")
def all_yt_created_tables(
    profiles_table,
    remote_test_profiles_table,
    vulture_crypta_table,
    cookies_table,
    user_shows_table,
    offsets_table,
    search_pers_table,
    shindler_table,
    url_to_offer_export_table,
):
    return {
        "profiles_table": profiles_table,
        "remote_test_profiles_table": remote_test_profiles_table,
        "vulture_crypta_table": vulture_crypta_table,
        "cookies_table": cookies_table,
        "user_shows_table": user_shows_table,
        "offsets_table": offsets_table,
        "shindler_table": shindler_table,
        "search_pers_table": search_pers_table,
        "url_to_offer_export_table": url_to_offer_export_table,
    }


@pytest.fixture(scope="module")
def all_yt_filled_tables(
    filled_profiles_table,
    filled_vulture_crypta_table,
    filled_cookies_table,
    user_shows_table,
    offsets_table,
    filled_search_pers_table,
    shindler_table,
    url_to_offer_export_table,
):
    return {
        "profiles_table": filled_profiles_table["table"],
        "remote_test_profiles_table": remote_test_profiles_table,
        "vulture_crypta_table": filled_vulture_crypta_table["table"],
        "cookies_table": filled_cookies_table["table"],
        "user_shows_table": user_shows_table,
        "offsets_table": offsets_table,
        "shindler_table": shindler_table,
        "search_pers_table": filled_search_pers_table["table"],
        "url_to_offer_export_table": url_to_offer_export_table,
    }

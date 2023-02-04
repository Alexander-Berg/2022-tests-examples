from functools import partial
from multiprocessing import Pool
from time import sleep

import yabs.logger as logger
import yt.wrapper as yt
import yt.yson as yson
from ads.bsyeti.protos.vulture_messages_pb2 import TAssociatedUids
from ads.bsyeti.tests.test_lib.data_collector.codec_decompressor import get_decompressed_object
from ads.bsyeti.tests.test_lib.data_collector.config import (
    ALL_YT_TABLES_CONFIG,
    EAGLE_TO_YT_PREFIXES,
    YT_CLUSTER,
    YT_TOKEN,
)
from mapreduce.yt.python.table_schema import extract_column_attributes
from yabs.proto.user_profile_pb2 import Profile


def create_yt_client():
    return yt.YtClient(proxy=YT_CLUSTER, token=YT_TOKEN)


def get_save_table_schema(table_path, schema_path):
    yt_client = create_yt_client()
    schema = extract_column_attributes(yt_client.get_attribute(table_path, "schema"))
    with open(schema_path, "wb") as f_p:
        logger.debug("schema for %s table: %s", table_path, schema)
        yson.dump(schema, f_p)
    logger.info("Dumped schema for %s table", table_path)
    return [elem["name"] for elem in schema if elem.get("sort_order") == "ascending"]


def get_uniq_ids(pair):
    return [prefix + pair[1] for prefix in EAGLE_TO_YT_PREFIXES[pair[0]]]


def get_all_uniqids(rows):
    return set(uniq_id for row in rows for pair in row for uniq_id in get_uniq_ids(pair))


def get_vulture_uniqids(rows):
    result = set()
    prefixes = {
        Profile.TSourceUniq.YANDEX_UID: "y",
        Profile.TSourceUniq.CRYPTA_ID1: "y",
        Profile.TSourceUniq.XUNIQ_GUID: "y",
        Profile.TSourceUniq.CRYPTA_ID2: "y",
        Profile.TSourceUniq.PRIVATE_YANDEX_UID: "y",
        Profile.TSourceUniq.PUID: "p",
        Profile.TSourceUniq.UUID: "uuid/",
        Profile.TSourceUniq.GAID: "gaid/",
        Profile.TSourceUniq.IDFA: "idfa/",
        Profile.TSourceUniq.MM_DEVICE_ID: "mmdi/",
        Profile.TSourceUniq.MAC: "mac/",
        Profile.TSourceUniq.OAID: "oaid/",
        Profile.TSourceUniq.DUID: "duid/",
        Profile.TSourceUniq.MAC_EXT_MD5: "mem/",
        Profile.TSourceUniq.EMAIL_MD5: "emdh/",
        Profile.TSourceUniq.PHONE_MD5: "pmdh/",
    }
    for row in rows:
        decompressed = get_decompressed_object(row["CompressedInfo"], "zlib-6")
        proto_obj = TAssociatedUids().FromString(decompressed)
        for record in proto_obj.ValueRecords:
            suffix = record.user_id
            prefix = prefixes[record.id_type]
            result.add(prefix + suffix)
    return result


def get_yt_rows(chunk, table=None, field_name=None):
    yt_client = create_yt_client()
    group_uniq_ids = ", ".join(
        "'{uniq_id}'".format(uniq_id=uniq_id.replace("\\", "\\\\").replace("'", "\\'")) for uniq_id in chunk
    )
    iteration = 0
    while iteration < 10:
        try:
            result = yt_client.select_rows(
                "* FROM [{table}] WHERE {field_name} in ({uniq_ids})".format(
                    table=table,
                    field_name=field_name,
                    uniq_ids=group_uniq_ids,
                ),
                # raw=True,
                format="yson",
            )
            break
        except Exception as exp:
            iteration += 1
            logger.error("YT request failed, iteration %d group %s : %s", iteration, group_uniq_ids, exp)
            if iteration >= 10:
                raise Exception("YT request error! %s" % exp)
            logger.warning("Sleeping for %d seconds", iteration / 2)
            sleep(iteration / 2)
    return list(result)


def get_yt_data(table, uniqids, chunksize=100, field_name="UniqId"):
    ordered_uniqids = list(str(elem) for elem in uniqids)
    uniqids_chunked = [ordered_uniqids[ind : ind + chunksize] for ind in range(0, len(ordered_uniqids), chunksize)]
    target_func = partial(get_yt_rows, table=str(table), field_name=str(field_name))
    proc_pool = Pool(len(uniqids_chunked))
    return sum(proc_pool.imap_unordered(target_func, uniqids_chunked), [])


def get_save_rows_from_yt(config_name, field_name, uniqids):
    yt_path, schema_path, data_path = ALL_YT_TABLES_CONFIG[config_name]
    sort_order = get_save_table_schema(yt_path, schema_path)
    logger.info("Parsing %s table from YT", config_name)
    yt_result = get_yt_data(yt_path, uniqids, field_name=field_name)
    # for i in yt_result[:10]:
    #     print list(yson.loads(i, yson_type="list_fragment"))
    sorted_yt_result = sorted(
        yt_result,
        key=lambda x: tuple(x[key] for key in sort_order)
        # key=lambda x: sort_order(yson.loads(x, yson_type="list_fragment")[field_name])
    )
    with open(data_path, "wb") as f_p:
        yson.dump(sorted_yt_result, f_p)
    logger.info("%s table parsed, total %s rows", config_name, len(sorted_yt_result))
    return sorted_yt_result


def get_save_profiles_from_yt(uniqids):
    get_save_rows_from_yt("Profiles", "UniqID", uniqids)
    return


def get_save_vulture_crypta_from_yt(uniqids):
    result = get_save_rows_from_yt("VultureCrypta", "UniqId", uniqids)
    return get_vulture_uniqids(result)


def get_save_cookies_from_yt(uniqids):
    get_save_rows_from_yt("Cookies", "UniqId", uniqids)
    return


def get_save_search_pers_from_yt(uniqids):
    get_save_rows_from_yt("SearchPers", "Id", uniqids)
    return

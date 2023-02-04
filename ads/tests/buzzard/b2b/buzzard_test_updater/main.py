import json
import os
from collections import defaultdict
from functools import partial
from math import log as logarithm
from subprocess import PIPE, Popen
from time import time

import yabs.logger as logger
import yt.wrapper as yt
from ads.bsyeti.libs.py_yt_operations import get_spec_with_bigb_acl
from ads.bsyeti.tests.test_lib.data_collector.codec_decompressor import get_decompressed_object
from ads.bsyeti.tests.test_lib.data_collector.config import (
    ALL_YT_TABLES_CONFIG,
    BUZZARD_QYT_TESTDATA,
    BUZZARD_YT_DATA_DIR,
    BUZZARD_YT_REQUESTS_DIR,
    SANDBOX_BUZZARD_OUTPUT_RESOURCE_TYPE,
    TESTDATA_DIR,
    TIMESTAMP_PATH,
    YT_QUEUES_TESTDATA_FOLDER,
)
from ads.bsyeti.tests.test_lib.data_collector.yamake_updater import update_yamakeinc
from ads.bsyeti.tests.test_lib.data_collector.yt_requester import get_save_table_schema
from .b2b_lb2yt_data import create_local_tables
from .bindings import parse_qyt_protopack
from tqdm import tqdm
from yt.wrapper import YtClient


def _get_decompressed_object(data, codec):
    if isinstance(codec, str):
        codec = codec.encode()
    return get_decompressed_object(data, codec)


def dump_timestamp():
    curent_time = int(time())
    with open(TIMESTAMP_PATH, "w") as f_p:
        f_p.write(str(curent_time))


def take_last_two_weeks(yt_client, path_prefix):
    path_prefix = path_prefix.rstrip("/")
    tables = yt_client.list(path_prefix)
    tables = sorted(tables, reverse=True)[:3]
    return [yt.ypath_join(path_prefix, table) for table in tables]


def parse_profileid_field(field):
    if field[0] == "{" and field[-1] == "}":  # logbroker type
        # format like "{\"LogType\":\"watch\",\"Uid\":\"y6319575091575266748\"}"
        parsed_json = json.loads(field)
        log_type = parsed_json["LogType"]
        uid = parsed_json["Uid"]
    else:  # qyt type
        # format like "y1901060435847239993"
        log_type = "qyt_batch"  # temporary hack, we need to take type from yt events queue at log_dumper
        uid = field
    return log_type, uid


def parse_packed_data(data, codec="zstd_6"):
    try:
        blob = _get_decompressed_object(data, codec)
    except Exception:
        # if we cant decompress, we think that no encoding here
        blob = data
    return parse_qyt_protopack(blob)


def take_prefix(uid):
    if uid.startswith("y"):
        return "y"
    elif uid.startswith("p"):
        return "p"
    pref = uid.split("/", 1)[0]
    assert pref != uid
    return pref


def map_filter(record, whitelist):
    profile_ids = record["ProfileIDs"]
    if any(parse_profileid_field(profile_id)[1] in whitelist for profile_id in profile_ids):
        yield record


def read_records(records):
    for record in records:
        yield {k.decode(): v for k, v in dict(record).items()}


def choose_uids(yt_client, input_tables):
    @yt.aggregator
    def mapper(records):
        log_uids = defaultdict(lambda: defaultdict(int))
        uid_logs = defaultdict(lambda: defaultdict(int))
        log_counts = defaultdict(lambda: defaultdict(int))
        for record in records:
            record = dict(record)
            record = {k.decode(): v for k, v in record.items()}
            if record.get("codec"):
                uid_logtype_pairs = parse_packed_data(record["PackedData"], codec=record["codec"])
            else:
                uid_logtype_pairs = parse_packed_data(record["PackedData"])
            uid_logtype_pairs = [(k.decode(), v.decode()) for k, v in uid_logtype_pairs]
            for uid, logtype in uid_logtype_pairs:
                log_uids[logtype][uid] += 1
                uid_logs[uid][logtype] += 1
                log_counts[logtype][take_prefix(uid)] += 1
            if len(uid_logs) > 5000:  # to much uids, we need yield it now
                for uid, dct in uid_logs.items():
                    yield {"primary_key": "uid_logs", "secondary_key": uid, "dict": list(dct.items())}
                uid_logs = defaultdict(lambda: defaultdict(int))
            if sum((len(dct) for dct in log_uids.values()), 0) > 5000:  # to much uids, we need yield it now
                for log, dct in log_uids.items():
                    yield {"primary_key": "log_uids", "secondary_key": log, "dict": list(dct.items())}
                log_uids = defaultdict(lambda: defaultdict(int))

        for uid, dct in uid_logs.items():
            yield {"primary_key": "uid_logs", "secondary_key": uid, "dict": list(dct.items())}
        for log, dct in log_uids.items():
            yield {"primary_key": "log_uids", "secondary_key": log, "dict": list(dct.items())}
        for log, dct in log_counts.items():
            yield {"primary_key": "log_counts", "secondary_key": log, "dict": list(dct.items())}

    def reducer(key, records):
        # we use this reduce for two main purposes: to find which count of records for each user, find users from each log
        key = {k.decode(): v.decode() for k, v in key.items()}
        records = read_records(records)
        if key["primary_key"] == "uid_logs":
            uid = key["secondary_key"]
            dct = defaultdict(int)
            for record in records:
                for log, count in record["dict"]:
                    dct[log] += count
                if len(dct) > 1000:
                    yield {"primary_key": "uid_logs", "secondary_key": uid, "dict": list(dct.items())}
                    dct.clear()
            yield {"primary_key": "uid_logs", "secondary_key": uid, "dict": list(dct.items())}

        elif key["primary_key"] == "log_uids":
            log = key["secondary_key"]
            dct = defaultdict(int)
            for record in records:
                for uid, count in record["dict"]:
                    dct[uid] += count
                if len(dct) > 1000:
                    yield {"primary_key": "uid_logs", "secondary_key": uid, "dict": list(dct.items())}
                    dct.clear()

            yield {"primary_key": "log_uids", "secondary_key": log, "dict": list(dct.items())}

        elif key["primary_key"] == "log_counts":
            log = key["secondary_key"]
            dct = defaultdict(int)
            for record in records:
                for prefix, count in record["dict"]:
                    dct[prefix] += count
                if len(dct) > 1000:
                    yield {"primary_key": "log_counts", "secondary_key": log, "dict": list(dct.items())}
                    dct.clear()
            yield {"primary_key": "log_counts", "secondary_key": log, "dict": list(dct.items())}
        else:
            assert False, "UNKNOWN 'primary_key' {field} FIELD!".format(field=key["primary_key"])

    chosen_users = set()
    spec = get_spec_with_bigb_acl()
    cpu_limit = 0.15
    spec["mapper"] = {}
    spec["reducer"] = {}
    spec["mapper"]["cpu_limit"] = cpu_limit
    spec["reducer"]["cpu_limit"] = cpu_limit
    spec["pool"] = "bigb"
    with yt_client.TempTable() as stats_table:
        yt_client.run_map_reduce(
            mapper=mapper,
            reducer=reducer,
            source_table=input_tables,
            destination_table=stats_table,
            reduce_by=[b"primary_key", b"secondary_key"],
            spec=spec,
            map_input_format=yt.YsonFormat(encoding=None),
            reduce_input_format=yt.YsonFormat(encoding=None),
        )

        logger.info("Prepared to analyse created records...")
        log_uids = dict()
        uid_logs = dict()
        log_counts = dict()
        logger.info("Reading stats table...")
        for row in tqdm(yt_client.read_table(stats_table), total=yt_client.row_count(stats_table)):
            if row["primary_key"] == "uid_logs":
                uid_logs[row["secondary_key"]] = dict(row["dict"])
            elif row["primary_key"] == "log_uids":
                log_uids[row["secondary_key"]] = dict(row["dict"])
            elif row["primary_key"] == "log_counts":
                log_counts[row["secondary_key"]] = dict(row["dict"])
            else:
                assert False, "UNKNOWN 'primary_key' {field} FIELD!".format(field=row["primary_key"])
        logger.info("Log prefix statistics:")
        for log, dct in sorted(log_counts.items()):
            for prefix, count in sorted(dct.items()):
                logger.info("Log: %s, prefix: %s, count: %d messages", log, prefix, count)

        sorted_uids = sorted(
            uid_logs.items(),
            key=lambda item: sum((logarithm(i) for i in item[1].values() if i > 3), 0),
            reverse=True,
        )

        target_fatties = 20
        logger.info("Taking %d most fat uids", target_fatties)
        for i, e in enumerate(sorted_uids):
            if i > target_fatties:
                break
            chosen_users.add(e[0])

        target_each_log = 500
        threshold_each_log = 2000
        logger.info(
            "Now for each log we take %d uids, that have no more than %d messages in that log",
            target_each_log,
            threshold_each_log,
        )
        for log, dct in tqdm(log_uids.items()):
            sorted_log_uids = sorted(
                dct.items(),
                key=lambda item: (item[1] if item[1] < threshold_each_log else -1),
                reverse=True,
            )
            prefixes = log_counts[log].keys()
            for prefix in prefixes:
                uids = set([uid[0] for uid in sorted_log_uids if uid[0].startswith(prefix)][:target_each_log])
                was_chosen_count = len(chosen_users)
                chosen_users |= uids
                logger.info(
                    "Log %s: Chosen %d uids with prefix %s (%d new)",
                    log,
                    len(uids),
                    prefix,
                    len(chosen_users) - was_chosen_count,
                )

    return chosen_users


def pipeline():
    os.mkdir(TESTDATA_DIR)

    # if there is no env variable token, default token from ~/.yt/token will be used
    yt_client = YtClient(proxy="hahn", token=os.environ.get("YT_TOKEN"))
    logger.info("YtClient created")

    qyt_tables = take_last_two_weeks(yt_client, YT_QUEUES_TESTDATA_FOLDER)
    logger.info("Taken %s tables for qyt testdata: %s", len(qyt_tables), qyt_tables)
    logger.info("Choose uids for testing: taking uids from different logs")
    chunks_to_analyse = list()
    uids = choose_uids(yt_client, qyt_tables)
    logger.info("Chosen %s users for testing", len(uids))

    logger.info("Prepared for filter qyt testdata")
    with yt_client.TempTable() as dst_table:
        logger.info("Starting map filter")
        yt_client.run_map(
            partial(map_filter, whitelist=uids),
            source_table=qyt_tables,
            destination_table=dst_table,
            spec=get_spec_with_bigb_acl(),
        )
        logger.info(
            "map filter done, successfully grepped %s lines",
            yt_client.row_count(dst_table),
        )
        yt_client.run_sort(
            source_table=dst_table,
            destination_table=dst_table,
            sort_by=["TimeStamp"],
            spec=get_spec_with_bigb_acl(),
        )
        logger.info("sort done")

        logger.info("Dumping qyt testdata table to localfile")
        good = 0
        total = 0
        with open(
            BUZZARD_QYT_TESTDATA, "wb"
        ) as f_p:  # here we dumping all blobs of data. 4 bytes for blob size, 4 bytes for codec name size, then blob, then codec
            for row in read_records(tqdm(yt_client.read_table(dst_table, format=yt.YsonFormat(encoding=None)))):
                total += 1
                check_pos = f_p.tell()
                if "codec" in row:
                    codec_name = row["codec"]
                    blob = row["PackedData"]
                else:
                    codec_name = "zstd_6"
                    try:
                        blob = _get_decompressed_object(row["PackedData"], "zstd_6")
                    except:
                        logger.error("Cannot decompress %s!", [row["PackedData"]])
                        blob = row["PackedData"]
                if not blob:
                    continue
                chunks_to_analyse.append(blob)
                good += 1
                bsize = len(blob)
                csize = len(codec_name)
                shard = row["Shard"]
                # packing uint32 to 4 bytes
                bsize_packed = bytes.fromhex(("0" * 8 + hex(bsize)[2:].rstrip("L"))[-8:])
                # packing uint32 to 4 bytes
                csize_packed = bytes.fromhex(("0" * 8 + hex(csize)[2:].rstrip("L"))[-8:])
                # packing uint32 to 4 bytes
                shard_packed = bytes.fromhex(("0" * 8 + hex(shard)[2:].rstrip("L"))[-8:])
                f_p.write(bsize_packed)
                f_p.write(csize_packed)
                f_p.write(blob)
                f_p.write(codec_name.encode())
                f_p.write(shard_packed)
                assert f_p.tell() == check_pos + 4 + 4 + len(blob) + len(codec_name) + 4
        logger.info("Qyt tempdata read, %s good and %s total (%s skipped)", good, total, total - good)
    logger.info("Qyt testdata dumped, local size is %s bytes", os.stat(BUZZARD_QYT_TESTDATA).st_size)

    dump_timestamp()

    logger.info("Dumping schemas for some production tables")
    for table in ALL_YT_TABLES_CONFIG.keys():
        table_path, schema_file = ALL_YT_TABLES_CONFIG[table][:2]
        if schema_file:
            get_save_table_schema(table_path, schema_file)

    logger.info("Filling yt_data folder, %d chunks to analyse", len(chunks_to_analyse))
    create_local_tables(chunks_to_analyse, BUZZARD_YT_DATA_DIR, BUZZARD_YT_REQUESTS_DIR)

    logger.info("Uploading all testdata to sandbox")
    shell_proc = Popen(
        [
            "ya",
            "upload",
            TESTDATA_DIR,
            "-T=" + SANDBOX_BUZZARD_OUTPUT_RESOURCE_TYPE,
            "--ttl",
            "inf",
            "-d",
            '"Buzzard b2b tests data"',
            "--owner",
            "BSYETI",
        ],
        stdout=PIPE,
        stderr=PIPE,
    )
    shell_proc.wait()

    pattern = b"Download link: https://proxy.sandbox.yandex-team.ru/"
    resource_id = ""
    for line in shell_proc.stdout.readlines() + shell_proc.stderr.readlines():
        if line.strip().startswith(pattern):
            resource_id = line.strip().strip(pattern).decode()
            break
    assert resource_id
    logger.info("Resource uploaded to sandbox, resource id is sbr://%s", resource_id)

    logger.info("Almost all done, removing junk...")
    os.system("rm -rf " + TESTDATA_DIR)
    return


def main():
    os.system("rm -rf " + TESTDATA_DIR)  # remove old artifacts
    pipeline()
    logger.info("Updating buzzard/b2b ya.make ...")
    update_yamakeinc("arcadia/ads/bsyeti/tests/buzzard/ya.inc")
    update_yamakeinc("arcadia/ads/bsyeti/libs/py_bt_profile/ut/ya.make.inc")
    update_yamakeinc(
        "arcadia/ads/bsyeti/tests/test_lib/eagle_daemon/ya.inc"
    )  # This resources are used simultaneously, so we should keep them synchronized
    logger.info("All done.")
    return


if __name__ == "__main__":
    main()

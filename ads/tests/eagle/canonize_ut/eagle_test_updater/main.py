import os
import pickle
from multiprocessing import Pipe, Process
from subprocess import PIPE, Popen
from tempfile import mkdtemp
from time import time
from urllib.parse import quote

import yabs.logger as logger
from ads.bsyeti.tests.test_lib.data_collector.access_log_downloader import fill_access_logs_dir
from ads.bsyeti.tests.test_lib.data_collector.access_log_parser import parse_access_logs
from ads.bsyeti.tests.test_lib.data_collector.config import (
    COMMON_REQUESTS,
    REQUESTS_DIR,
    SANDBOX_OUTPUT_RESOURCE_TYPE,
    TESTDATA_DIR,
    TIMESTAMP_PATH,
)
from ads.bsyeti.tests.test_lib.data_collector.filter_requests import (
    filter_global_requests,
    filter_per_client_requests,
)
from ads.bsyeti.tests.test_lib.data_collector.yamake_updater import update_yamakeinc
from ads.bsyeti.tests.test_lib.data_collector.yt_requester import (
    get_all_uniqids,
    get_save_cookies_from_yt,
    get_save_profiles_from_yt,
    get_save_search_pers_from_yt,
    get_save_vulture_crypta_from_yt,
)


def check_env_variable(variable):
    if not os.environ.get(variable) or not os.environ[variable].strip():
        return False
    return True


def dump_timestamp():
    curent_time = int(time())
    with open(TIMESTAMP_PATH, "w") as f_p:
        f_p.write(str(curent_time))


def vlt_proc_func(connection, uniq_ids):
    vult_crypta_resp = get_save_vulture_crypta_from_yt(uniq_ids)
    connection.send(vult_crypta_resp)
    connection.close()


def save_clients_requests(rows):
    os.mkdir(REQUESTS_DIR)
    for client, queries in rows.items():
        str_client = (
            "/bigb?keyword-set=" + client.strip("ks_") if client.startswith("ks_") else "?client=" + client
        ) + "&"
        str_queries = [
            str_client + "&".join(pair[0] + "=" + quote(pair[1]) for pair in query) for query in queries if query
        ]
        with open(REQUESTS_DIR + "/" + client, "wb") as f_p:
            pickle.dump(str_queries, f_p)
    logger.info("Dumped all client requests to disk")


def save_common_requests(queries):
    prefix = "/bigb?"
    str_queries = [prefix + "&".join(pair[0] + "=" + quote(pair[1]) for pair in query) for query in queries if query]
    with open(COMMON_REQUESTS, "wb") as f_p:
        pickle.dump(str_queries, f_p)
    logger.info("Dumped all common requests to disk")


# def update_resource_id(resource_id):
# yamake_path = "/arcadia/ads/bsyeti/eagle/canonize_ut/ya.make.inc"
# replace_str = "    sbr://" + resource_id + "  # " + SANDBOX_OUTPUT_RESOURCE_TYPE


def pipeline():
    logs_dir = mkdtemp("access_logs")
    lognames = fill_access_logs_dir(logs_dir)
    rows = parse_access_logs(lognames)

    os.mkdir(TESTDATA_DIR)

    mlb_parent_conn, mlb_child_conn = Pipe()
    vlt_parent_conn, vlt_child_conn = Pipe()

    final_rows = filter_per_client_requests(rows)  # requests from access log

    common_queries = filter_global_requests(rows)  # common requests for every client

    all_queries = common_queries + sum(final_rows.values(), [])

    uniq_ids = get_all_uniqids(all_queries)

    vlt_proc = Process(target=vlt_proc_func, args=(vlt_child_conn, uniq_ids))
    vlt_proc.start()  # Vulture Crypta thread

    cookies_proc = Process(target=get_save_cookies_from_yt, args=(uniq_ids,))
    cookies_proc.start()  # Cookies table thread

    search_pers = Process(target=get_save_search_pers_from_yt, args=(uniq_ids,))
    search_pers.start()  # Cookies table thread

    save_clients_requests(final_rows)
    save_common_requests(common_queries)

    mlbonding_resp = mlb_parent_conn.recv()
    vult_crypta_resp = vlt_parent_conn.recv()
    vlt_proc.join()
    search_pers.join()

    logger.info(
        "Stat: %s primary uniq_ids, %s mlbonding ids, %s vulture ids",
        len(uniq_ids),
        len(mlbonding_resp),
        len(vult_crypta_resp),
    )

    uniq_ids |= set(mlbonding_resp) | set(vult_crypta_resp)
    logger.info("Total: %s unique uniq_ids", len(uniq_ids))
    get_save_profiles_from_yt(uniq_ids)  # downloading Profiles

    cookies_proc.join()

    dump_timestamp()

    logger.info("Uploading testdata to sandbox")
    shell_proc = Popen(
        [
            "ya",
            "upload",
            TESTDATA_DIR,
            "-T=" + SANDBOX_OUTPUT_RESOURCE_TYPE,
            "--ttl",
            "365",
            "-d",
            '"Eagle canonize_ut tests data"',
            "--owner",
            "BSYETI",
        ],
        stdout=PIPE,
        stderr=PIPE,
    )
    shell_proc.wait()

    pattern = "Download link: https://proxy.sandbox.yandex-team.ru/"
    resource_id = ""
    for line in shell_proc.stdout.readlines() + shell_proc.stderr.readlines():
        if line.strip().startswith(pattern):
            resource_id = line.strip().strip(pattern)
            break
    assert resource_id
    logger.info("Resource uploaded to sandbox, resuorce id is sbr://%s", resource_id)

    os.system("rm -rf " + TESTDATA_DIR)
    os.system("rm -rf " + logs_dir)
    return


def main():
    for var in ("YT_USER", "YT_TOKEN"):
        if not check_env_variable(var):
            logger.error("Environment variable %s empty, but must be filled", var)
            exit(1)
    pipeline()
    logger.info("Updating eagle/canonize_ut/ya.make.inc ...")
    update_yamakeinc("arcadia/ads/bsyeti/tests/eagle/canonize_ut/ya.make.inc")
    logger.info("Updating eagle_daemon/ya.inc ...")
    update_yamakeinc("arcadia/ads/bsyeti/tests/test_lib/eagle_daemon/ya.inc")
    logger.info("All done.")


if __name__ == "__main__":
    main()

import os
from argparse import ArgumentParser
from subprocess import PIPE, Popen

import yabs.logger as logger
from ads.bsyeti.tests.test_lib.data_collector.config import (
    ALL_YT_TABLES_CONFIG,
    SANDBOX_OUTPUT_RESOURCE_TYPE,
    TESTDATA_DIR,
    TIMESTAMP_PATH,
)
from ads.bsyeti.tests.test_lib.data_collector.yt_requester import get_save_table_schema


def check_env_variable(variable):
    if not os.environ.get(variable) or not os.environ[variable].strip():
        return False
    return True


def pipeline(args):
    if not os.path.exists(TESTDATA_DIR) or not os.path.exists(TIMESTAMP_PATH):
        logger.error(
            "To update schemas, we need table with testdata: %s. "
            + "Download it from SB: https://sandbox.yandex-team.ru/resources?type=%s",
            TESTDATA_DIR,
            SANDBOX_OUTPUT_RESOURCE_TYPE,
        )
        exit(1)

    for table in args.table if args.table else ALL_YT_TABLES_CONFIG.keys():
        table_path, schema_file = ALL_YT_TABLES_CONFIG[table][:2]
        if args.contour:
            table_path = table_path.replace("/production/", "/{}/".format(args.contour))
        get_save_table_schema(table_path, schema_file)

    if not args.upload:
        return

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
    return


def main():
    for var in ("YT_USER", "YT_TOKEN"):
        if not check_env_variable(var):
            logger.error("Environment variable %s empty, but must be filled", var)
            exit(1)

    parser = ArgumentParser(description="Get fresh table schemas from YT")
    parser.add_argument(
        "-c",
        "--contour",
        action="store",
        choices=["test", "prestable", "production"],
        default="production",
        help="Contour to use tables from",
    )
    parser.add_argument(
        "-t",
        "--table",
        action="append",
        choices=ALL_YT_TABLES_CONFIG.keys(),
        help="Tables to update",
    )
    parser.add_argument(
        "-u",
        "--upload",
        action="store_true",
        help="Use this flag to upload updated data to Sandbox",
    )

    args = parser.parse_args()

    pipeline(args)


if __name__ == "__main__":
    main()

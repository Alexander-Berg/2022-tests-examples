import os
import socket

import boto3

from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as proto_command
from maps.garden.tools.yt_module_executor.lib import log_writer

ENVIRONMENT_CONFIG_S3 = {
    "logs_storage": {
        "type": "s3",
        "bucket": "bucket-for-logs",
        "key_prefix": ""
    },
    "s3": {
        "host": "localhost",
        "port": int(os.environ["S3MDS_PORT"]),
        "access_key": "not_very_secret_access_key",
        "secret_key": "the_top_secret_secret_key"
    }
}


def _prepare_log_file():
    log_file = "log_file.txt"
    with open(log_file, "wt") as f:
        f.write("log")
    return log_file


def test_upload_log_to_s3():
    environment_settings = ENVIRONMENT_CONFIG_S3
    log_file = _prepare_log_file()

    # Bucket for logs must exists before uploading logs
    s3_settings = ENVIRONMENT_CONFIG_S3["s3"]
    logs_settings = ENVIRONMENT_CONFIG_S3["logs_storage"]
    s3_client = boto3.resource(
        "s3",
        endpoint_url=f"http://{s3_settings['host']}:{s3_settings['port']}",
        aws_access_key_id=s3_settings["access_key"],
        aws_secret_access_key=s3_settings["secret_key"]
    )
    s3_client.Bucket(logs_settings["bucket"]).create()

    log_writer.upload_log(logfile=log_file, environment_settings=environment_settings)

    log_object = s3_client.Object(
        logs_settings["bucket"],
        os.path.join(logs_settings["key_prefix"], log_file)
    )
    log = log_object.get()["Body"].read()
    expected_log = open(log_file, "rb").read()
    assert log == expected_log


def test_reporting_crash():
    expected_exit_status = 123
    output_proto = log_writer.prepare_output(
        local_output="just_random_nonexistent_output_file",
        exit_status=expected_exit_status,
    )

    result = proto_command.InvokeTaskOutput()
    result.ParseFromString(output_proto)

    assert result.IsInitialized()
    assert result.timings.HasField("mainProcessFinishedAt")
    assert result.context.hostname == socket.gethostname()
    assert not result.HasField("result")
    assert not result.HasField("exception")
    assert result.HasField("crash")

    assert result.crash.exitStatus == expected_exit_status

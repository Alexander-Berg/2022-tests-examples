import click
import logging
import os
import time
import pymongo
import json
import sys
from functools import lru_cache

import yt.wrapper as yt

from maps.garden.sdk.core import RetryTaskError
from maps.garden.libs_server.task.task_storage import TaskStorage, TaskInvocationStatus
from maps.garden.sdk.module_rpc import common as rpc_common
from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as proto_command
from maps.pylibs.utils.lib import common
from maps.pylibs.utils.lib import json_utils


TEST_MONGO_CONFIG_YT = "//tmp/test_mongo_config.json"
_ONE_TRY_NODE = "//tmp/one_try"
_STOP_WORK_NODE = "//tmp/stop_work"

logger = logging.getLogger("executor.test")

POSSIBLE_INPUT_STARTS = {
    "good input",
    "bad input",
    "force exit",
    "retry once",
    "retry always",
    "work until condition then success",
    "work until condition then retry",
    "silent until condition then success",
}


@lru_cache(maxsize=1)
def get_operation_and_job_id() -> tuple[str, str]:
    operation_id = os.environ.get("YT_OPERATION_ID")
    job_id = os.environ.get("YT_JOB_ID")
    assert operation_id
    assert job_id
    return operation_id, job_id


def create_yt_client() -> yt.YtClient:
    # this file will be uploaded by YtHandler
    config = json_utils.load_json("environment_settings.json")
    return yt.YtClient(config=config["yt_servers"]["hahn"]["yt_config"])


def get_mongo_db(yt_client: yt.YtClient):
    if not yt_client.exists(TEST_MONGO_CONFIG_YT):
        raise Exception("Not found mongo config")

    mongo_config = json.loads(yt_client.read_file(TEST_MONGO_CONFIG_YT).read().decode("utf8"))
    if "mongo_path" not in mongo_config:
        raise Exception("Not found mongo_path in mongo config")

    client = pymongo.MongoClient(mongo_config["mongo_path"])

    return client.get_database()


def notify_task_still_running(task_storage: TaskStorage, task_key: str):
    logger.error("Notify task still running")
    task_storage.update(
        task_key=task_key,
        invocation_status=TaskInvocationStatus.RUNNING,
    )


def download_module_input(task_storage: TaskStorage, task_key: str) -> str:
    logger.error("Try to download module input")
    operation_id, job_id = get_operation_and_job_id()
    task_record = task_storage.pick_by_key_and_update(task_key, operation_id, job_id)
    if not task_record:
        raise Exception(f"Task with key '{task_key}' is not found")

    if task_record.input_was_used:
        return "restarted by YT"

    raw_input = task_record.input_proto.decode("utf8")
    # input should be different for every test because they can use the same yt instance
    # if input is different we could list operations filtered by input for every test
    # so we use specified start for input and any other symbols after it for every test case
    for possible_start in POSSIBLE_INPUT_STARTS:
        if raw_input.startswith(possible_start):
            return possible_start

    return raw_input


def upload_module_output(task_storage: TaskStorage, task_key: str, task_input: str):
    task_storage.update(
        task_key=task_key,
        invocation_status=TaskInvocationStatus.FINISHED,
        output_proto=generate_output_proto(task_input)
    )


def generate_output_proto(input_data: str) -> bytes:
    output = proto_command.InvokeTaskOutput()
    output.timings.mainProcessStartedAt.GetCurrentTime()
    output.context.hostname = "localhost"

    try:
        if input_data in ("good input", "work until condition then success"):
            # Pretend that task creates no resources
            creates = {}
            rpc_common.convert_task_invocation_arguments_to_proto(creates, output.result.creates)
        elif input_data == "bad input":
            raise ValueError("Wrong input_data")
        elif input_data == "force exit":
            output.crash.exitStatus = 1
        elif input_data in ("retry always", "work until condition then retry"):
            raise RetryTaskError()
    except Exception:
        _, ex, traceback = sys.exc_info()
        rpc_common.convert_exception_to_proto(ex, traceback, output.exception)

    return output.SerializeToString()


@click.command()
@click.option("--module-binary", required=True, type=click.Path(exists=True), help="Module binary file")
@click.option("--environment-settings", type=click.Path(), help="File to read environment settings from")
@click.option("--logfile", default=None, help="Path to local log file to be uploaded to logs_storage")
@click.option("--task-key", help="Task identifier in collection tasks")
def main(
    module_binary,
    environment_settings,
    logfile,
    task_key,
):
    """
    Emulates real yt module executor from maps/garden/tools/yt_module_executor
    """
    logger.error("Start module_executor")
    yt_client = create_yt_client()
    db = get_mongo_db(yt_client)
    task_storage = TaskStorage(db)

    task_input = download_module_input(task_storage, task_key)
    logger.error(f"{task_input=}")
    if task_input in ("good input", "bad input", "force exit", "retry always"):
        notify_task_still_running(task_storage, task_key)
        time.sleep(1)
        upload_module_output(task_storage, task_key, task_input)
    elif task_input == "work until condition then success":
        def work():
            notify_task_still_running(task_storage, task_key)
            return yt_client.exists(_STOP_WORK_NODE)
        assert common.wait_until(work, check_interval=1, timeout=360)
        upload_module_output(task_storage, task_key, task_input)
    elif task_input == "work until condition then retry":
        def work():
            notify_task_still_running(task_storage, task_key)
            return yt_client.exists(_STOP_WORK_NODE)
        assert common.wait_until(work, check_interval=1, timeout=360)
        if not yt_client.exists(_ONE_TRY_NODE):
            yt_client.create("map_node", _ONE_TRY_NODE)
            upload_module_output(task_storage, task_key, task_input)
        else:
            upload_module_output(task_storage, task_key, "good input")
    elif task_input == "retry once":
        if not yt_client.exists(_ONE_TRY_NODE):
            yt_client.create("map_node", _ONE_TRY_NODE)
            upload_module_output(task_storage, task_key, "retry always")
        else:
            upload_module_output(task_storage, task_key, "good input")
    elif task_input == "silent until condition then success":
        def silence():
            return yt_client.exists(_STOP_WORK_NODE)
        assert common.wait_until(silence, check_interval=1, timeout=360)
        upload_module_output(task_storage, task_key, "good input")
    else:
        raise Exception(f"Not known {task_input=}")

    logger.error("End module_executor")

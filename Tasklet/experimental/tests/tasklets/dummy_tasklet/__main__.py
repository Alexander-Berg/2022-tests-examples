import json
import sys
import time
import os
from typing import Any

from google.protobuf import json_format

from tasklet.api.v2 import executor_service_pb2
from tasklet.api.v2 import executor_service_pb2_grpc
from tasklet.api.v2 import well_known_structures_pb2

from tasklet.experimental.sdk.py.dummy import interface as dummy_sdk

NAME = "dummy_tasklet"


def handle_ok(payload: dict[str, Any], tasklet_io: dummy_sdk.TaskletInterface):
    time.sleep(payload.get("lifespan", 0))

    output = well_known_structures_pb2.GenericBinary()
    output.payload = payload.get("result_data", "").encode("utf-8")

    tasklet_io.write_output(output)


def handle_crash(payload: dict[str, Any], tasklet_io: dummy_sdk.TaskletInterface):
    output = well_known_structures_pb2.GenericBinary()
    output.payload = payload.get("result_data", "").encode("utf-8")

    serialized = output.SerializeToString()
    tasklet_io.write_raw_output(serialized[:len(serialized) // 2])
    time.sleep(0.5)

    sys.exit(1)


def handle_stop_flag(payload: dict[str, Any], tasklet_io: dummy_sdk.TaskletInterface):
    time.sleep(payload.get("lifespan", 0))

    output = well_known_structures_pb2.GenericBinary()
    output.payload = payload.get("result_data", "").encode("utf-8")
    stop_flag_path = payload.get("taint_payload")
    assert stop_flag_path
    while not os.path.exists(stop_flag_path):
        print(f"waiting for stop flag at {stop_flag_path}")
        time.sleep(0.1)
    tasklet_io.write_output(output)


def handle_timeout(_: dict[str, Any], tasklet_io: dummy_sdk.TaskletInterface):
    time.sleep(3000)


def handle_user_error(_: dict[str, Any], tasklet_io: dummy_sdk.TaskletInterface):
    msg = well_known_structures_pb2.UserError()
    msg.description = "ошибка ошибка, 世界 is wrong"
    msg.is_transient = True
    l = msg.details.get_or_create_list("list")
    l.extend([1, 2, 3])
    msg.details["foo"] = "bar"
    tasklet_io.write_error(msg)


# see internal/executor/tests/test_executor.py
dispatcher = {
    "": handle_ok,
    "CRASH": handle_crash,
    "TIMEOUT": handle_timeout,
    "STOP_FLAG": handle_stop_flag,
    "USER_ERROR": handle_user_error,
}


def main():
    tasklet_io = dummy_sdk.TaskletInterface()

    input_message = tasklet_io.read_input(well_known_structures_pb2.GenericBinary())

    ctx = tasklet_io.get_context()
    assert ctx.meta.id
    assert ctx.meta.tasklet_id
    assert ctx.meta.build_id

    assert ctx.schema.simple_proto.input_message == "tasklet.api.v2.GenericBinary"
    assert ctx.schema.simple_proto.output_message == "tasklet.api.v2.GenericBinary"
    assert ctx.schema.simple_proto.schema_hash != ""

    sys.stdout.write(f"{NAME} STDOUT\n")
    sys.stdout.write(json_format.MessageToJson(ctx) + "\n")
    sys.stdout.write(json_format.MessageToJson(input_message))
    sys.stderr.write(f"{NAME} STDERR\n")

    executor_service: executor_service_pb2_grpc.ExecutorServiceStub = tasklet_io.executor_client

    resp = executor_service.GetContext(executor_service_pb2.GetContextRequest())
    assert ctx.meta.tasklet_id == resp.context.meta.tasklet_id
    assert ctx.executor.address == resp.context.executor.address

    payload = json.loads(input_message.payload)
    dispatcher[payload.get("taint", "").upper()](payload, tasklet_io)


if __name__ == "__main__":
    main()

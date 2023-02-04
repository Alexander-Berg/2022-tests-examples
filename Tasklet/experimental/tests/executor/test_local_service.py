from typing import List

import grpc
import time
import os

from tasklet.api.v2 import context_pb2
from tasklet.api.v2 import executor_service_pb2
from tasklet.api.v2 import executor_service_pb2_grpc
from tasklet.api.v2 import tasklet_service_pb2

from tasklet.experimental.tests.common import models as test_models
from tasklet.experimental.tests.common import server_mock
from tasklet.experimental.tests.executor import conftest

import yatest.common as ya_test

from . import test_executor


def _local_service_stubs(context: context_pb2.Context) -> List[executor_service_pb2_grpc.ExecutorServiceStub]:
    provided_addr, port = context.executor.address.rsplit(":", 1)

    return [
        executor_service_pb2_grpc.ExecutorServiceStub(
            grpc.insecure_channel("{}:{}".format(addr, port))
        )
        for addr in (
            "[::1]",
            "127.0.0.1",
            provided_addr,
        )
    ]


def test__local_service_ping(
    tasklet_server: server_mock.TaskletServer,
    dummy_tasklet_env: conftest.TaskletEnv,
    dummy_tasklet_model: test_models.ConfiguredTasklet,
):
    flag = ya_test.test_output_path("stop.flag")
    opts = test_executor._gen_new_execution_opts(2, test_executor.Taints.STOP_FLAG, str(flag))

    execution = test_executor._register_execution_in_service(tasklet_server, dummy_tasklet_model, opts)
    with test_executor._run_executor_process(
        dummy_tasklet_env, execution, tasklet_server
    ) as process:  # type: ya_test.process._Execution

        sleep_ix = 0
        while not os.path.exists(dummy_tasklet_env.saved_context_path()):
            time.sleep(0.1)
            sleep_ix += 1
            assert sleep_ix < 10

        with open(dummy_tasklet_env.saved_context_path(), "rb") as context_file:
            ctx = context_pb2.Context()
            ctx.ParseFromString(context_file.read())

        for local_service in _local_service_stubs(ctx):
            resp = local_service.GetContext(executor_service_pb2.GetContextRequest())
            assert ctx.meta.tasklet_id == resp.context.meta.tasklet_id
            assert ctx.executor.address == resp.context.executor.address

        with open(flag, "wb") as f:
            f.write(b"done")

        process.wait(check_exit_code=True, timeout=60)

    api = tasklet_server.grpc_stub
    resp = api.GetExecution(tasklet_service_pb2.GetExecutionRequest(id=execution.meta.id))
    status = resp.execution.status
    test_executor._validate_generic_binary_successful_execution(opts, status)

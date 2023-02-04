import dataclasses
import datetime as dt
import json
import os
import uuid
import yatest.common as yatest
import typing
import contextlib

from tasklet.api.v2 import data_model_pb2
from tasklet.api.v2 import tasklet_service_pb2
from tasklet.api.v2 import well_known_structures_pb2
from tasklet.experimental.tests.common import models
from tasklet.experimental.tests.common import server_mock
from tasklet.experimental.tests.executor import conftest


class Taints:
    NONE = ""
    TIMEOUT = "TIMEOUT"
    CRASH = "CRASH"
    STOP_FLAG = "STOP_FLAG"
    USER_ERROR = "USER_ERROR"


@dataclasses.dataclass
class TaskletExecutionOpts:
    execution_id: str
    expected_output: str
    started_at: dt.datetime
    lifespan: int
    taint: str
    taint_payload: typing.Any

    def _raw_tasklet_input(self) -> bytes:
        return json.dumps({
            "lifespan": self.lifespan,
            "result_data": self.expected_output,
            "taint": self.taint,
            "taint_payload": self.taint_payload,
        }).encode()

    def generate_generic_binary_input(self) -> data_model_pb2.ExecutionInput:
        typed_input = well_known_structures_pb2.GenericBinary(
            payload=self._raw_tasklet_input(),
        )
        execution_input = data_model_pb2.ExecutionInput(
            serialized_data=typed_input.SerializeToString(),
        )
        return execution_input


def _register_execution_in_service(
    tasklet_server: server_mock.TaskletServer,
    test_tasklet: models.ConfiguredTasklet,
    opts: TaskletExecutionOpts,
) -> data_model_pb2.Execution:
    resp = tasklet_server.grpc_stub.Execute(tasklet_service_pb2.ExecuteRequest(
        namespace=test_tasklet.namespace.name,
        tasklet=test_tasklet.tasklet.Name,
        label=test_tasklet.label.Name,
        input=opts.generate_generic_binary_input(),
    ))
    meta = resp.execution.meta
    assert meta.tasklet_id == test_tasklet.tasklet.ID
    assert meta.build_id == test_tasklet.build.ID

    return resp.execution


@contextlib.contextmanager
def _run_executor_process(
    tasklet_env: conftest.TaskletEnv,
    execution: data_model_pb2.Execution,
    tasklet_server: server_mock.TaskletServer,
):
    os.makedirs(tasklet_env.root_path, exist_ok=True)
    process: yatest.process._Execution = yatest.execute(
        [
            tasklet_env.executor_binary_path,
            "--execution-id", execution.meta.id,
            "--endpoint-address", tasklet_server.grpc_address,
            "--tasklet-path", tasklet_env.executable_path,
            "--java-binary", tasklet_env.java_binary_path,
            "--logs-dir", tasklet_env.logs_dir(),
            "--disable-auth",
        ],
        stdout=yatest.test_output_path("executor.stdout"),
        stderr=yatest.test_output_path("executor.stderr"),
        cwd=tasklet_env.root_path,
        wait=False,
    )
    try:
        yield process
    finally:
        if process.running:
            process.terminate()


def _run_executor_in_service_mode(
    tasklet_env: conftest.TaskletEnv,
    execution: data_model_pb2.Execution,
    tasklet_server: server_mock.TaskletServer,
) -> data_model_pb2.ExecutionStatus:
    with _run_executor_process(tasklet_env, execution, tasklet_server) as process:  # type: yatest.process._Execution
        process.wait(check_exit_code=True, timeout=60)
        assert not process.returncode
    api = tasklet_server.grpc_stub
    resp = api.GetExecution(tasklet_service_pb2.GetExecutionRequest(id=execution.meta.id))
    return resp.execution.status


def _validate_generic_binary_successful_execution(opts: TaskletExecutionOpts, status: data_model_pb2.ExecutionStatus):
    # NB: proto has UTC timestamp. opts.started_at is local time
    finished_at = status.stats.finished_at.ToDatetime() + (dt.datetime.now() - dt.datetime.utcnow())

    assert opts.started_at < finished_at < opts.started_at + dt.timedelta(seconds=opts.lifespan + 5)

    assert status.stats.exit_code == 0
    assert status.error.description == ""

    # check compat
    output = well_known_structures_pb2.GenericBinary()
    output.ParseFromString(status.result.serialized_output)
    assert output.payload.decode("utf-8") == opts.expected_output

    # check newstyle
    assert status.processing_result.WhichOneof("kind") == "output"
    output = well_known_structures_pb2.GenericBinary()
    output.ParseFromString(status.processing_result.output.serialized_output)


def _gen_new_execution_opts(lifespan: int, taint: str, taint_payload: typing.Any) -> TaskletExecutionOpts:
    return TaskletExecutionOpts(
        execution_id=str(uuid.uuid4()),
        expected_output=str(uuid.uuid4()),
        started_at=dt.datetime.now(),
        lifespan=lifespan,
        taint=taint,
        taint_payload=taint_payload,
    )


def test__executor__dummy_tasklet(
    tasklet_server: server_mock.TaskletServer,
    dummy_tasklet_env: conftest.TaskletEnv,
    dummy_tasklet_model: models.ConfiguredTasklet,
):
    opts = _gen_new_execution_opts(2, Taints.NONE, None)
    execution = _register_execution_in_service(tasklet_server, dummy_tasklet_model, opts)
    status = _run_executor_in_service_mode(dummy_tasklet_env, execution, tasklet_server)
    _validate_generic_binary_successful_execution(opts, status)


def test__executor__dummy_tasklet_crash(
    tasklet_server: server_mock.TaskletServer,
    dummy_tasklet_env: conftest.TaskletEnv,
    dummy_tasklet_model: models.ConfiguredTasklet,
):
    opts = _gen_new_execution_opts(2, Taints.CRASH, None)
    execution = _register_execution_in_service(tasklet_server, dummy_tasklet_model, opts)
    status = _run_executor_in_service_mode(dummy_tasklet_env, execution, tasklet_server)
    # NB: proto has UTC timestamp. opts.started_at is local time
    finished_at = status.stats.finished_at.ToDatetime() + (dt.datetime.now() - dt.datetime.utcnow())

    assert opts.started_at < finished_at < opts.started_at + dt.timedelta(seconds=opts.lifespan + 5)
    assert status.stats.exit_code == 1
    # check compat
    assert status.result.serialized_output == b""
    assert "job error" in status.error.description

    # check newstyle
    assert status.processing_result.WhichOneof("kind") == "server_error"
    assert "job error" in status.processing_result.server_error.description
    assert status.processing_result.server_error.code == data_model_pb2.ErrorCodes.ERROR_CODE_CRASHED


def test__executor__dummy_tasklet_user_error(
    tasklet_server: server_mock.TaskletServer,
    dummy_tasklet_env: conftest.TaskletEnv,
    dummy_tasklet_model: models.ConfiguredTasklet,
):
    opts = _gen_new_execution_opts(2, Taints.USER_ERROR, None)
    execution = _register_execution_in_service(tasklet_server, dummy_tasklet_model, opts)
    status = _run_executor_in_service_mode(dummy_tasklet_env, execution, tasklet_server)
    # NB: proto has UTC timestamp. opts.started_at is local time
    finished_at = status.stats.finished_at.ToDatetime() + (dt.datetime.now() - dt.datetime.utcnow())

    assert opts.started_at < finished_at < opts.started_at + dt.timedelta(seconds=opts.lifespan + 5)
    assert status.stats.exit_code == 0
    # Check compat:
    assert status.result.serialized_output == b""
    assert "User error" in status.error.description
    assert "is wrong" in status.error.description

    # Check newstyle
    assert status.processing_result.WhichOneof("kind") == "user_error"
    error = status.processing_result.user_error
    assert "wrong" in error.description
    assert error.is_transient is True
    assert len(error.details["list"]) == 3
    assert error.details["foo"] == "bar"


def test__executor__dummy_go_tasklet(
    tasklet_server: server_mock.TaskletServer,
    dummy_go_tasklet_env: conftest.TaskletEnv,
    dummy_go_tasklet_model: models.ConfiguredTasklet,
):
    opts = _gen_new_execution_opts(2, Taints.NONE, None)
    execution = _register_execution_in_service(tasklet_server, dummy_go_tasklet_model, opts)
    status = _run_executor_in_service_mode(dummy_go_tasklet_env, execution, tasklet_server)
    _validate_generic_binary_successful_execution(opts, status)


def test__executor__dummy_java_tasklet(
    tasklet_server: server_mock.TaskletServer,
    dummy_java_tasklet_env: conftest.TaskletEnv,
    dummy_java_tasklet_model: models.ConfiguredTasklet,
):
    opts = _gen_new_execution_opts(2, Taints.NONE, None)
    execution = _register_execution_in_service(tasklet_server, dummy_java_tasklet_model, opts)
    status = _run_executor_in_service_mode(dummy_java_tasklet_env, execution, tasklet_server)
    _validate_generic_binary_successful_execution(opts, status)

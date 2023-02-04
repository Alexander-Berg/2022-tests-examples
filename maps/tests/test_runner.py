from unittest import mock
import socket
import time

import yatest.common
import pytest
import subprocess

from maps.pylibs.utils.lib.common import RetryFailedException

from maps.garden.sdk.module_rpc.common import Capabilities
from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as module_rpc
from maps.garden.libs_server.module import communicate
from maps.garden.libs_server.module.runner import ModuleRunner


MODULE_TO_TEST = 'maps/garden/libs_server/test_utils/test_module/test_module'
MODULE_INFO_COMMAND = communicate.make_module_info_command()


def test_execute_proto():
    module_path = yatest.common.binary_path(MODULE_TO_TEST)

    with ModuleRunner(module_path, Capabilities.ALL, "contour_name", "test_module") as runner:
        module_info = runner.execute_proto(
            input_message=MODULE_INFO_COMMAND,
            output_message_type=module_rpc.ModuleInfoOutput,
            operation_name="module_info",
        )

        assert module_info.name == "test_module"
        assert module_info.capabilities == Capabilities.ALL + [Capabilities.HANDLE_BUILD_STATUS]


def test_kill_and_restart():
    """
    ModuleRunner restarts the module process in case it is killed by any reason.
    """
    module_path = yatest.common.binary_path(MODULE_TO_TEST)

    with ModuleRunner(module_path, Capabilities.ALL, "contour_name", "test_module") as runner:
        runner.execute_proto(
            input_message=MODULE_INFO_COMMAND,
            output_message_type=module_rpc.ModuleInfoOutput,
            operation_name="module_info",
        )

        assert runner._process.is_alive()

        old_pid = runner._process._module_process.pid

        runner._process._module_process.kill()
        runner._process._module_process.wait(5)
        assert runner._process._module_process.returncode is not None

        runner.execute_proto(
            input_message=MODULE_INFO_COMMAND,
            output_message_type=module_rpc.ModuleInfoOutput,
            operation_name="module_info",
        )

        assert runner._process.is_alive()

        new_pid = runner._process._module_process.pid

        assert old_pid != new_pid


@mock.patch("maps.garden.libs_server.module.runner.MODULE_PROCESS_LIFETIME_SEC", 1)
def test_stop_process_by_timer():
    """
    Module process should live no longer than 'process_lifetime' parameter.
    ModuleRunner kills the module process itself.
    """
    module_path = yatest.common.binary_path(MODULE_TO_TEST)

    with ModuleRunner(module_path, Capabilities.ALL, "contour_name", "test_module") as runner:
        runner.execute_proto(
            input_message=MODULE_INFO_COMMAND,
            output_message_type=module_rpc.ModuleInfoOutput,
            operation_name="module_info",
        )

        time.sleep(2)

        assert runner._process is None


def test_run_two_runners_sequentially():
    """
    ModuleRunner cleans any unix socket remained from previous runs.
    """
    module_path = yatest.common.binary_path(MODULE_TO_TEST)

    with ModuleRunner(module_path, Capabilities.ALL, "contour_name", "test_module") as runner:
        runner.execute_proto(
            input_message=MODULE_INFO_COMMAND,
            output_message_type=module_rpc.ModuleInfoOutput,
            operation_name="module_info",
        )

    with ModuleRunner(module_path, Capabilities.ALL, "contour_name", "test_module") as runner:
        runner.execute_proto(
            input_message=MODULE_INFO_COMMAND,
            output_message_type=module_rpc.ModuleInfoOutput,
            operation_name="module_info",
        )


def test_nonexistent_module():
    """
    ModuleRunner throws if it fails to start the process.
    """
    with ModuleRunner('wrong_path', Capabilities.ALL, "contour_name", "test_module") as runner:
        with pytest.raises(OSError):
            runner.execute_proto(
                input_message=MODULE_INFO_COMMAND,
                output_message_type=module_rpc.ModuleInfoOutput,
                operation_name="module_info",
            )


@mock.patch("maps.garden.libs_server.module.runner.MODULE_SOCKET_TIMEOUT_SEC", 1)
def test_socket_timeout():
    """
    'echo' is an example of a command that does not connect to any unix socket.
    Listener.accept() waits for an incoming connection no longer than parameter `socket_timeout`.
    """
    with ModuleRunner('/bin/echo', Capabilities.ALL, "contour_name", "test_module") as runner:
        with pytest.raises(RetryFailedException) as e:
            runner.execute_proto(
                input_message=MODULE_INFO_COMMAND,
                output_message_type=module_rpc.ModuleInfoOutput,
                operation_name="module_info",
            )

        assert isinstance(e.value.last_error, socket.timeout)


@mock.patch("subprocess.Popen", wraps=subprocess.Popen)
def test_environment_name(popen_mock):
    module_path = yatest.common.binary_path(MODULE_TO_TEST)

    with ModuleRunner(module_path, Capabilities.ALL, "contour_name", "test_module") as runner:
        runner.execute_proto(
            input_message=MODULE_INFO_COMMAND,
            output_message_type=module_rpc.ModuleInfoOutput,
            operation_name="module_info",
        )

        popen_mock.assert_called_once()

        assert popen_mock.call_args.kwargs["env"] == {"ENVIRONMENT_NAME": "unstable"}

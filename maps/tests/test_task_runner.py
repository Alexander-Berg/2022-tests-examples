import json
import pytest

from yt.wrapper.ypath import ypath_join
from yt.wrapper import operation_commands

from maps.garden.libs_server.task.yt_task import ContourSettings, YtTask
from maps.garden.libs_server.yt_task_handler.pymod.task_runner import PyTaskRunner

from maps.pylibs.utils.lib.common import wait_until

from .conftest import MODULE_EXECUTOR_YT_PATH, TEST_MODULE_YT_PATH


# TODO: use mocks instead
class FakeTask(YtTask):

    @property
    def task_class_name(self):
        return "fake_task"

    @property
    def executable(self):
        return TEST_MODULE_YT_PATH

    def make_task_call_stdin(self):
        return "Hello, world!"

    @property
    def retry_attempt(self):
        return 0


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress_simple")
def test_run_task(yt_stuff, server_config, environment_settings):
    client = yt_stuff.get_yt_client()
    working_dir = "//home/some_dir"
    client.create("map_node", working_dir, recursive=True)

    environment_settings_path = ypath_join(server_config["yt"]["configs_ypath"], "environment_settings.json")
    client.write_file(environment_settings_path, json.dumps(environment_settings).encode())

    runner = PyTaskRunner(server_config["yt"], "instance", MODULE_EXECUTOR_YT_PATH, poll_period=1, thread_count=2)

    task = FakeTask(
        task_id=1234,
        contour_name="contour_name",
        module_name="test_module",
        build_id=1,

        log_file_basename="log_file",

        consumption={"cpu": 1, "ram": 100*1024**2},

        contour_settings=ContourSettings(
            environment_settings=environment_settings,
            environment_settings_ypath=environment_settings_path,
            yt_work_dir=working_dir,
        ),
        task_key="force_exit",
    )

    runner.enqueue_task_start(task)
    operations = []

    def pop_started_tasks(operations):
        operations += runner.pop_started_tasks()
        return len(operations) > 0

    assert wait_until(lambda: pop_started_tasks(operations))

    # check both statuses for quick exit
    assert wait_until(lambda: client.get_operation_state(operations[0][1]) in ["completed", "failed"])

    operation = client.get_operation(operations[0][1])
    state = operation_commands.OperationState(operation["state"])
    job_infos = operation_commands.get_jobs_with_error_or_stderr(
        operations[0][1],
        only_failed_jobs=False,
        client=client
    )
    stderr = "\n".join(job_info["stderr"] for job_info in job_infos)
    operation_result = json.dumps(operation['result'], indent=2)
    assert state != "failed", f"operation result: {operation_result}; stderr: {stderr}"

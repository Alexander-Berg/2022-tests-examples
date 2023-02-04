import datetime as dt
import pytest

from maps.pylibs.utils.lib.common import wait_until

from maps.garden.libs_server.common.param_storage import ParamStorage
from maps.garden.libs_server.task.operation_storage import OperationStorage
from maps.garden.libs_server.task.task_log_manager import TaskLogManager
from maps.garden.tools.operation_fetcher.lib import fetcher

import common as cm


@pytest.fixture
def task_log_manager(db):
    return TaskLogManager(db)


@pytest.fixture
def operation_fetcher(patched_server_config, db, task_log_manager, mocker):
    mocker.patch.object(fetcher, "OPERATION_FINALIZATION_DELAY", dt.timedelta())
    operation_fetcher = fetcher.OperationFetcher(
        yt_config=patched_server_config["yt"]["config"],
        param_storage=ParamStorage(db),
        operation_storage=OperationStorage(db),
        task_log_manager=task_log_manager,
    )
    operation_fetcher.fetch_recent_operations()
    return operation_fetcher


@pytest.mark.use_local_mongo
def test_task_statistics(garden_client_helper, task_log_manager, operation_fetcher):
    """
    Initial conditions:
    1. Two remote modules on YT ('source' and 'map' types).
    2. The 'map' module is configured for autostart on completion of the 'source' one.

    Actions:
    1. Post a new build for the 'source' module.
    2. Wait until the 'source' module is completed.
    3. Wait until the 'map' module is completed for the system contour.
    4. Normalize task logs.
    7. Get full build info with task and resource statistics.
    """
    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    garden_client_helper.build_src_module(cm.SYSTEM_CONTOUR_NAME)
    autostarted_build = garden_client_helper.wait_autostarted_build(cm.SYSTEM_CONTOUR_NAME)
    autostarted_build_resources = garden_client_helper.get_resources(
        module_name=cm.TEST_MODULE,
        build_id=autostarted_build["id"],
        contour_name=cm.SYSTEM_CONTOUR_NAME,
    )
    assert set(r["name"] for r in autostarted_build_resources) == {"output_resource"}

    tasks = task_log_manager.get_task_logs()
    task_log_manager.save_normalized_tasks(tasks)
    operation_fetcher.fetch_recent_operations()

    def all_operations_finished():
        operation_fetcher.update_finished_operations()
        return all(
            op.result
            for op in task_log_manager.get_operations(cm.TEST_MODULE, autostarted_build["id"])
        )

    assert wait_until(all_operations_finished, timeout=120, check_interval=1)

    result = garden_client_helper.get_build_full_info(
        module_name=cm.TEST_MODULE,
        build_id=autostarted_build['id'],
        contour_name=cm.SYSTEM_CONTOUR_NAME,
    )

    # Check and pop fields that are changed between test runs

    assert result.pop("startedAt", None)
    assert result.pop("finishedAt", None)
    longest_path = result.pop("longestPath", None)
    assert len(longest_path["tasks"]) == 1
    assert longest_path["durationS"]
    assert result.pop("durationS", None)

    assert set(r["name"] for r in result.pop("resources", [])) == {"input_resource", "output_resource"}

    for module_version_info in result["moduleVersions"]:
        assert module_version_info.pop("displayedName", None)

    for task in result["tasks"]:
        assert task.pop("key")
        assert task.pop("finishedAt")
        assert task.pop("startedAt")
        assert task.pop("schedulerEvents")
        assert task.pop("moduleEvents")
        assert task.pop("insertTraceback", None)
        assert task.pop("logUrl", None)
        task.pop("alerts", None)

        resource_durations = task.pop("resourceDurations", None)
        assert resource_durations
        assert len(resource_durations["keyToCommitS"]) == 1
        assert len(resource_durations["keyToPreparationS"]) == 1

        main_op_id = task.pop("mainOperationId", None)
        assert main_op_id
        job_id = task.pop("jobId", None)
        assert job_id

        task_durations = task.pop("taskDurations", None)
        assert task_durations

        for op in task["operations"]:
            assert op.pop("startedAt", None)
            assert op.pop("finishedAt", None)
            assert op.pop("stats", None)
            assert op.pop("id", None) == main_op_id
            assert op.pop("url", None) == f"https://yt.yandex-team.ru/hahn/operations/{main_op_id}"
            op.pop("alerts", None)

    # Verify the rest against canondata
    return result

import pytest
import common as cm

from maps.pylibs.utils.lib.common import wait_until


@pytest.mark.use_local_mongo
def test_simple_build(garden_client_helper):
    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    src_build = garden_client_helper.build_src_module(cm.USER_CONTOUR_NAME)
    build = garden_client_helper.start_long_build(src_build, cm.USER_CONTOUR_NAME)
    garden_client_helper.complete_long_build()
    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"])


@pytest.mark.use_local_mongo
def test_complete_build_after_restart_scheduler(garden_client_helper, scheduler):
    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    src_build = garden_client_helper.build_src_module(cm.USER_CONTOUR_NAME)
    build = garden_client_helper.start_long_build(src_build, cm.USER_CONTOUR_NAME)

    scheduler.restart()

    garden_client_helper.complete_long_build()
    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"])


@pytest.mark.use_local_mongo
def test_complete_build_when_scheduler_restart(garden_client_helper, scheduler, db):
    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    src_build = garden_client_helper.build_src_module(cm.USER_CONTOUR_NAME)
    build = garden_client_helper.start_long_build(src_build, cm.USER_CONTOUR_NAME)

    assert wait_until(
        lambda: db.tasks.count_documents({"operation_id": {"$ne": None}}) > 0,
        timeout=60, check_interval=1)

    scheduler.stop()

    operation_ids = [str(r["operation_id"]) for r in db.tasks.find()]
    assert len(operation_ids) == 1, "Only one task should be running"
    assert operation_ids[0], f"operation_id:{operation_ids[0]} should be not None"

    garden_client_helper.complete_long_build()

    yt_client = garden_client_helper.get_yt_client()

    assert wait_until(
        lambda: yt_client.get_operation_state(operation_ids[0]).is_finished(),
        timeout=60, check_interval=1)

    scheduler.start()
    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"])


@pytest.mark.use_local_mongo
def test_cancel_build(garden_client_helper):
    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    src_build = garden_client_helper.build_src_module(cm.USER_CONTOUR_NAME)
    build = garden_client_helper.start_long_build(src_build, cm.USER_CONTOUR_NAME)

    garden_client_helper.cancel_build(build["id"])

    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"], status="cancelled")


@pytest.mark.use_local_mongo
def test_build_restart(garden_client_helper, db):
    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    src_build = garden_client_helper.build_src_module(cm.USER_CONTOUR_NAME)
    build = garden_client_helper.start_long_build(src_build, cm.USER_CONTOUR_NAME)

    garden_client_helper.restart_build(build["id"])
    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"], status="in_progress")

    def _wait_action():
        actions = [r for r in db.build_actions.find({"operation": "restart"})]
        return len(actions) > 0 and actions[0]["action_status"] == "completed"

    assert wait_until(
        _wait_action,
        timeout=60, check_interval=1)

    garden_client_helper.complete_long_build()
    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"], status="completed")


@pytest.mark.use_local_mongo
def test_build_cancel_and_restart(garden_client_helper):
    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    src_build = garden_client_helper.build_src_module(cm.USER_CONTOUR_NAME)
    build = garden_client_helper.start_long_build(src_build, cm.USER_CONTOUR_NAME)

    garden_client_helper.cancel_build(build["id"])
    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"], status="cancelled")
    garden_client_helper.restart_build(build["id"])
    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"], status="in_progress")
    garden_client_helper.complete_long_build()
    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"], status="completed")


@pytest.mark.use_local_mongo
def test_remove_build(garden_client_helper, db):
    resources = [r for r in db.resources.find({"name": cm.TEST_MODULE})]
    assert not resources, "old resources found"

    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    garden_client_helper.build_src_module(cm.SYSTEM_CONTOUR_NAME)
    build = garden_client_helper.wait_autostarted_build(cm.SYSTEM_CONTOUR_NAME)

    garden_client_helper.delete_build(build["id"])

    assert wait_until(
        lambda: len(garden_client_helper.get_builds(module_name=cm.TEST_MODULE, contour_name=cm.SYSTEM_CONTOUR_NAME)) == 0,
        timeout=60, check_interval=1)

    resources = [r for r in db.resources.find({"name": cm.TEST_MODULE})]
    assert not resources, "Resources have not delete"


@pytest.mark.use_local_mongo
def test_datavalidation_warning(garden_client_helper, db):
    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    src_build = garden_client_helper.build_src_module(cm.USER_CONTOUR_NAME)
    build = garden_client_helper.start_long_build(src_build, cm.USER_CONTOUR_NAME)
    garden_client_helper.fail_long_build("datavalidation_warning")
    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"], status="failed")

    builds = [t for t in db.builds.find({"name": cm.TEST_MODULE, "id": int(build["id"])})]
    assert builds, "builds not found in mongo"
    tasks = builds[0]["status"]["failed_tasks"]
    assert len(tasks) == 1
    assert "DataValidationWarning" in tasks[0]["exception"]["type"]
    task_id = tasks[0]["task_id"]

    garden_client_helper.ignore_task(module_name=cm.TEST_MODULE, build_id=build["id"], task_id=task_id)

    garden_client_helper.wait_build(cm.TEST_MODULE, build["id"])

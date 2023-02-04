import mongoengine
import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle import default_cms
from walle.clients.cms import CmsTaskType, CmsTaskAction, CmsApiVersion
from walle.cms_models import CmsProject, CmsTask
from walle.projects import DEFAULT_CMS_NAME


@pytest.fixture
def test(request):
    return TestCase.create(request)


def test_unique_cms_task_index(test):
    """can't insert two CMS tasks with the same task_id and project_id"""
    test.mock_project({"id": "test-project"})
    kwargs = {
        "id": "task-1",
        "project_id": "test-project",
        "status": default_cms.CmsTaskStatus.OK,
        "hosts": ["host-1"],
    }
    default_cms.CmsTask(**kwargs).save(force_insert=True)
    try:
        default_cms.CmsTask(**kwargs).save(force_insert=True)
    except mongoengine.NotUniqueError:
        assert True
    else:
        assert False, "NonUniqueError wasn't raised"


def test_reschedule_tasks_project_doesnt_exist(test):
    """reschedule_tasks should raise an exception for non-existing project"""
    with pytest.raises(default_cms.CmsProjectDoesntExist):
        default_cms.reschedule_tasks("non-existing-project")


def test_get_max_busy_hosts_is_set(test):
    """get_max_busy_hosts returns value defined in project's properties"""
    test.mock_project({"id": "test-project", "cms_max_busy_hosts": 15})
    assert default_cms.get_max_busy_hosts("test-project") == 15


def test_add_task_no_hosts(test):
    """add_task accepts task with no hosts, though doesn't save it"""
    test.mock_project({"id": "test-project"})
    response = default_cms.add_task("test-project", _extend({"id": "task", "hosts": []}))
    assert response["status"] == default_cms.CmsTaskStatus.OK
    assert default_cms.get_tasks("test-project") == []


def test_add_task_same_host_greater_than_limit(test):
    """add_task treats repetitive hosts as the same host"""
    test.mock_project({"id": "test-project", "cms_max_busy_hosts": 10})
    hosts = ["host"] * 11
    response = default_cms.add_task("test-project", _extend({"id": "task", "hosts": hosts}))
    assert response["status"] == default_cms.CmsTaskStatus.OK
    assert response["hosts"] == ["host"]


def test_add_task_hosts_greater_than_limit(test):
    """add_task rejects tasks with hosts greater than cms_max_busy_hosts"""
    test.mock_project({"id": "test-project", "cms_max_busy_hosts": 10})
    hosts = [("host-%d" % i) for i in range(11)]
    expected = {"id": "task", "hosts": hosts, "status": default_cms.CmsTaskStatus.REJECTED}
    actual = default_cms.add_task("test-project", _extend({"id": "task", "hosts": hosts}))
    assert "message" in actual
    actual_message = actual.pop("message")
    assert expected == actual
    assert str(len(hosts)) in actual_message and "10" in actual_message


def test_add_task_custom_cms(test):
    """add_task should refuse to add tasks for a project that uses custom CMS"""
    test.mock_project({"id": "test-project", "cms": "http://foo.bar/zar"})
    hosts = [("host-%d" % i) for i in range(11)]
    expected = {"id": "task", "hosts": hosts, "status": default_cms.CmsTaskStatus.REJECTED}
    actual = default_cms.add_task("test-project", _extend({"id": "task", "hosts": hosts}))
    assert "message" in actual
    actual_message = actual.pop("message")
    assert expected == actual
    assert "test-project" in actual_message and "doesn't use default CMS" in actual_message


def test_get_task(test):
    """get_task returns relevant info with ok and in-process task"""
    test.mock_project({"id": "test-project", "cms_max_busy_hosts": 3})
    default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1", "host-2"]}))
    default_cms.add_task("test-project", _extend({"id": "task-2", "hosts": ["host-3", "host-4"]}))
    expected_1 = {"id": "task-1", "hosts": ["host-1", "host-2"], "status": default_cms.CmsTaskStatus.OK}
    expected_2 = {"id": "task-2", "hosts": ["host-3", "host-4"], "status": default_cms.CmsTaskStatus.IN_PROCESS}
    actual_1 = default_cms.get_task("test-project", "task-1")
    actual_2 = default_cms.get_task("test-project", "task-2")
    assert actual_1 == expected_1
    assert actual_2 == expected_2


def test_get_tasks_in_process(test):
    """get_tasks returns relevant info with ok and in-process tasks"""
    test.mock_project({"id": "test-project", "cms_max_busy_hosts": 3})
    test.mock_project({"id": "another-project", "cms_max_busy_hosts": 3})
    default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1", "host-2"]}))
    default_cms.add_task("test-project", _extend({"id": "task-2", "hosts": ["host-3", "host-4"]}))
    default_cms.add_task("another-project", _extend({"id": "task-3", "hosts": ["host-5", "host-6"]}))
    expected = [
        {"id": "task-1", "hosts": ["host-1", "host-2"], "status": default_cms.CmsTaskStatus.OK},
        {"id": "task-2", "hosts": ["host-3", "host-4"], "status": default_cms.CmsTaskStatus.IN_PROCESS},
    ]
    actual = default_cms.get_tasks("test-project")
    assert actual == expected


def test_reschedule_tasks(test):
    """Tasks state should change from in-process to ok"""
    test.mock_project({"id": "test-project", "cms_max_busy_hosts": 3})
    task_1 = default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1", "host-2"]}))
    task_2 = default_cms.add_task("test-project", _extend({"id": "task-2", "hosts": ["host-3", "host-4"]}))
    assert task_1 == {"id": "task-1", "hosts": ["host-1", "host-2"], "status": default_cms.CmsTaskStatus.OK}
    assert task_2 == {"id": "task-2", "hosts": ["host-3", "host-4"], "status": default_cms.CmsTaskStatus.IN_PROCESS}
    default_cms.delete_task("test-project", "task-1")
    actual = default_cms.get_tasks("test-project")
    expected = [
        {"id": "task-2", "hosts": ["host-3", "host-4"], "status": default_cms.CmsTaskStatus.OK},
    ]
    assert actual == expected


def test_reschedule_tasks_ignore(test):
    """A project with no cms_max_busy_hosts should be ignored"""
    project_id = "test-project"
    task_id = "task-id"
    test.mock_project({"id": project_id, "cms_max_busy_hosts": None})
    default_cms.CmsProject(id=project_id).modify(add_to_set__tasks=task_id, upsert=True)
    default_cms.CmsProject.objects(id=project_id).update_one(pull__tasks=task_id)
    assert default_cms.CmsProject.objects(id=project_id).only("tasks")[0].tasks == []
    default_cms.reschedule_tasks(project_id)


def test_reschedule_tasks_multiple(test):
    """Multiple tasks' state should change from in-process to ok"""
    test.mock_project({"id": "test-project", "cms_max_busy_hosts": 3})
    default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1", "host-2", "host-3"]}))
    default_cms.add_task("test-project", _extend({"id": "task-2", "hosts": ["host-4"]}))
    default_cms.add_task("test-project", _extend({"id": "task-3", "hosts": ["host-5"]}))
    default_cms.add_task("test-project", _extend({"id": "task-4", "hosts": ["host-6"]}))
    default_cms.add_task("test-project", _extend({"id": "task-5", "hosts": ["host-7"]}))
    default_cms.delete_task("test-project", "task-1")
    actual = default_cms.get_tasks("test-project")
    expected = [
        {"id": "task-2", "hosts": ["host-4"], "status": default_cms.CmsTaskStatus.OK},
        {"id": "task-3", "hosts": ["host-5"], "status": default_cms.CmsTaskStatus.OK},
        {"id": "task-4", "hosts": ["host-6"], "status": default_cms.CmsTaskStatus.OK},
        {"id": "task-5", "hosts": ["host-7"], "status": default_cms.CmsTaskStatus.IN_PROCESS},
    ]
    assert actual == expected


def test_reschedule_tasks_same_host(test):
    """Same hosts should not be counted twice during rescheduling"""
    test.mock_project({"id": "test-project", "cms_max_busy_hosts": 3})
    default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1", "host-2"]}))
    default_cms.add_task("test-project", _extend({"id": "task-2", "hosts": ["host-2"]}))
    default_cms.add_task("test-project", _extend({"id": "task-3", "hosts": ["host-3"]}))
    default_cms.add_task("test-project", _extend({"id": "task-4", "hosts": ["host-4"]}))

    actual = default_cms.get_tasks("test-project")
    expected = [
        {"id": "task-1", "hosts": ["host-1", "host-2"], "status": default_cms.CmsTaskStatus.OK},
        {"id": "task-2", "hosts": ["host-2"], "status": default_cms.CmsTaskStatus.OK},
        {"id": "task-3", "hosts": ["host-3"], "status": default_cms.CmsTaskStatus.OK},
        {"id": "task-4", "hosts": ["host-4"], "status": default_cms.CmsTaskStatus.IN_PROCESS},
    ]
    assert actual == expected


def test_reschedule_tasks_deleted_project(test):
    """reschedule_tasks cleans up CMS-related models if project ceased to exist"""
    project = test.mock_project({"id": "test-project", "cms_max_busy_hosts": 3, "cms": DEFAULT_CMS_NAME})
    default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1", "host-2"]}))

    project.delete()

    assert CmsProject.objects.count() == 1
    assert CmsTask.objects.count() == 1

    default_cms.reschedule_tasks("test-project")

    assert CmsProject.objects.count() == 0
    assert CmsTask.objects.count() == 0


def test_cms_maintenance_drop_projects(test):
    """cms_projects_maintenance should drop CmsProject's for projects that don't use default CMS"""
    project = test.mock_project({"id": "test-project", "cms": DEFAULT_CMS_NAME})
    default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1"]}))
    assert CmsProject.objects(id="test-project").count() == 1  # CmsProject exists

    project.cms = "http://foo.bar.zar/"
    project.save()

    default_cms.cms_maintenance_drop_stale_projects()
    assert CmsProject.objects(id="test-project").count() == 1  # Busy hosts don't let CmsProject get deleted
    default_cms.delete_task("test-project", "task-1")  # Free host-1
    default_cms.cms_maintenance_drop_stale_projects()
    assert CmsProject.objects(id="test-project").count() == 0  # CmsProject gets deleted


def test_cms_maintenance_drop_projects_dont_drop(test):
    """cms_projects_maintenance should not drop stale CmsProjects with tasks"""
    test.mock_project({"id": "test-project", "cms": DEFAULT_CMS_NAME})
    default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1"]}))
    CmsTask.objects(project_id="test-project").delete()
    default_cms.cms_maintenance_drop_stale_projects()
    assert CmsProject.objects(id="test-project").count() == 1  # Still alive, cause CmsProject.tasks hold task-1
    default_cms.reschedule_tasks("test-project")  # releases task-1
    default_cms.cms_maintenance_drop_stale_projects()  # drop CmsProject with no tasks
    assert CmsProject.objects(id="test-project").count() == 0  # at last!


@pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
def test_reschedule_after_changing_cms(
    test, monkeypatch_production_env, api_version, mock_service_tvm_app_ids, mock_get_planner_id_by_bot_project_id
):
    tvm_app_id = 500
    mock_service_tvm_app_ids([tvm_app_id])
    mock_get_planner_id_by_bot_project_id()

    test.mock_project({"id": "test-project", "cms": DEFAULT_CMS_NAME})

    default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1"]}))

    result = test.api_client.post(
        "/v1/projects/test-project",
        data={
            "name": "test-project",
            "cms": {"url": "http://not.default/cms", "api_version": api_version, "tvm_app_id": tvm_app_id},
        },
    )
    assert result.status_code == http.client.OK

    # must not raise any exception and drop all cms-related stuff for the project
    default_cms.reschedule_tasks("test-project")

    assert CmsProject.objects.count() == 0
    assert CmsTask.objects.count() == 0


def test_drop_cms_project(test):
    test.mock_project({"id": "test-project", "cms": DEFAULT_CMS_NAME})
    default_cms.add_task("test-project", _extend({"id": "task-1", "hosts": ["host-1"]}))
    default_cms.add_task("test-project", _extend({"id": "task-2", "hosts": ["host-2", "host-3"]}))

    assert CmsProject.objects.count() == 1
    assert CmsTask.objects.count() == 2

    default_cms.drop_cms_project("test-project")

    assert CmsProject.objects.count() == 0
    assert CmsTask.objects.count() == 0


def _extend(task):
    """Extends task object with fields required for add task request."""

    task.setdefault("type", CmsTaskType.MANUAL)
    task.setdefault("issuer", "issuer-mock")
    task.setdefault("action", CmsTaskAction.REBOOT)

    return task

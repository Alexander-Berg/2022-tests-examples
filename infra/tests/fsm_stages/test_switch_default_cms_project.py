import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
)
from sepelib.core.exceptions import LogicalError
from walle.clients.cms import CmsTaskStatus
from walle.default_cms import CmsTask, CmsProject
from walle.stages import Stages, Stage


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.SWITCH_DEFAULT_CMS_PROJECT))


def _mock_project_switch_params(test, cms_task_id):
    source_project = test.mock_project({"id": "source-project"})
    target_project = test.mock_project({"id": "target-project"})
    host = test.mock_host(
        {
            "name": "test-host",
            "project": source_project.id,
            "cms_task_id": cms_task_id,
            "task": mock_task(
                stage=Stages.SWITCH_DEFAULT_CMS_PROJECT,
                stage_params={
                    "cms_task_id": cms_task_id,
                    "source_project_id": source_project.id,
                    "target_project_id": target_project.id,
                },
            ),
        }
    )
    return source_project, target_project, host


def test_switch_project(test):
    cms_task_id = "test-cms-task-id"
    source_project, target_project, host = _mock_project_switch_params(test, cms_task_id)

    CmsTask(id=host.cms_task_id, project_id=source_project.id, hosts=[host.name], status=CmsTaskStatus.IN_PROCESS).save(
        force_insert=True
    )
    CmsProject(id=source_project.id, tasks=[host.cms_task_id]).save()
    CmsProject(id=target_project.id, tasks=["other-cms-task-id-2"]).save()

    handle_host(host)
    mock_complete_current_stage(host, inc_revision=1)

    assert CmsTask.objects(id=host.cms_task_id).get().project_id == target_project.id
    assert CmsProject.objects(id=source_project.id).get().tasks == []
    assert CmsProject.objects(id=target_project.id).get().tasks == ["other-cms-task-id-2", host.cms_task_id]


def test_cms_task_does_not_exist(test):
    cms_task_id = "test-cms-task-id"
    source_project, target_project, host = _mock_project_switch_params(test, cms_task_id)

    handle_host(host)
    # We don't fail the stage if CMS task does not exist
    mock_complete_current_stage(host, inc_revision=1)


def test_task_multiple_hosts(test):
    cms_task_id = "test-cms-task-id"
    source_project, target_project, host = _mock_project_switch_params(test, cms_task_id)

    cms_task = CmsTask(
        id=host.cms_task_id,
        project_id=source_project.id,
        hosts=[host.name, "test-host-2"],
        status=CmsTaskStatus.IN_PROCESS,
    ).save(force_insert=True)

    with pytest.raises(LogicalError):
        handle_host(host)

    mock_complete_current_stage(host, inc_revision=1)
    cms_task.reload()
    assert cms_task.project_id == source_project.id

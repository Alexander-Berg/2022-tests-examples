import pytest

from infra.walle.server.tests.lib.dns import dns_box, dns_box_fixture  # noqa NOTE(rocco66): fixture
from infra.walle.server.tests.lib.util import mock_response, monkeypatch_request, BOT_PROJECT_ID
from walle.clients import inventory, abc, staff
from walle.idm import project_push, project_role_managers

pytest.MOCKED_SERVICE_SLUG = "some_service"
pytest.MOCKED_PLANNER_ID = 500


@pytest.fixture
def project_idm_request_ownership_mock(mp):
    return mp.method(project_role_managers.OwnerManager.request_add_member, obj=project_role_managers.OwnerManager)


@pytest.fixture
def project_idm_add_node_mock(mp, batch_request_execute_mock):
    return mp.function(project_push.add_project_role_tree_nodes)


@pytest.fixture
def monkeypatch_staff_get_user_groups(mp):
    mp.function(staff.get_user_groups, return_value={"@svc_some_service", "@svc_another_service"})


@pytest.fixture
def monkeypatch_abc_get_service_by_id(mp):
    resp = {
        "slug": pytest.MOCKED_SERVICE_SLUG,
        "id": pytest.MOCKED_PLANNER_ID,
    }
    return mp.function(abc.get_service_by_id, return_value=resp)


@pytest.fixture
def monkeypatch_bot_projects(mp, disable_caches):
    resp = mock_response(
        [
            {
                "group_id": str(BOT_PROJECT_ID),
                "ru_description": "Some description in Russian",
                "us_description": "Some description in English",
                "s3_group_id": None,
                "s2_group_id": None,
                "s1_group_id": str(BOT_PROJECT_ID),
                "planner_id": str(pytest.MOCKED_PLANNER_ID),
            }
        ]
    )
    return monkeypatch_request(mp, return_value=resp)


@pytest.fixture
def monkeypatch_check_deploy_conf(mp):
    mp.function(inventory.check_deploy_configuration)


@pytest.fixture
def project_idm_add_role_nodes_mock(mp):
    return mp.function(project_push.add_project_role_tree_nodes)


@pytest.fixture
def project_idm_request_project_roles_mock(mp, batch_request_execute_mock):
    return mp.function(project_push.request_project_role_clones)

"""Tests user API."""

from collections import defaultdict

import pytest
import http.client

from infra.walle.server.tests.lib.api_util import delete_default_project
from infra.walle.server.tests.lib.util import TestCase
from walle.clients import staff
from walle.expert.automation_plot import AutomationPlot
from walle.idm.project_role_managers import ProjectRole

USER = TestCase.api_user
USER_GROUP = "@user-group"
USER_GROUP2 = "@more-user-groups"

OTHER_USER = "some-other-user"
OTHER_USER2 = "some-more-user"
OTHER_GROUP = "@some-other-group"


@pytest.fixture
def test(request):
    test = TestCase.create(request)
    delete_default_project(test)

    test.mock_project(
        {
            "id": "first",
            "roles": {
                ProjectRole.OWNER: [OTHER_USER, OTHER_GROUP],
                ProjectRole.SUPERUSER: [USER, OTHER_USER],
                ProjectRole.SSH_REBOOTER: [USER, OTHER_USER],
            },
        }
    )
    test.mock_project(
        {
            "id": "second",
            "roles": {
                ProjectRole.OWNER: [USER],
                ProjectRole.SUPERUSER: [USER_GROUP, OTHER_GROUP],
                ProjectRole.USER: [USER],
                ProjectRole.NOC_ACCESS: [USER],
                ProjectRole.SSH_REBOOTER: [USER_GROUP],
            },
        }
    )
    test.mock_project(
        {
            "id": "third",
            "roles": {
                ProjectRole.OWNER: [OTHER_USER, USER, OTHER_USER2],
                ProjectRole.SUPERUSER: [USER_GROUP2],
                ProjectRole.USER: [USER_GROUP],
            },
        }
    )
    test.mock_project(
        {
            "id": "fourth",
            "roles": {
                ProjectRole.OWNER: [OTHER_USER, USER_GROUP, OTHER_GROUP],
                ProjectRole.USER: [USER, USER_GROUP2],
                ProjectRole.NOC_ACCESS: [USER_GROUP],
                ProjectRole.SSH_REBOOTER: [USER_GROUP, USER_GROUP2],
            },
        }
    )
    test.mock_project(
        {
            "id": "fifth",
            "roles": {
                ProjectRole.OWNER: [OTHER_USER, USER, USER_GROUP],
                ProjectRole.SUPERUSER: [USER_GROUP, USER_GROUP2],
                ProjectRole.NOC_ACCESS: [USER_GROUP, USER_GROUP2],
            },
        }
    )

    AutomationPlot.objects.delete()
    test.automation_plot.mock({"id": "first", "name": "AP1", "owners": [OTHER_USER, OTHER_GROUP]})
    test.automation_plot.mock({"id": "second", "name": "AP2", "owners": [USER]})
    test.automation_plot.mock({"id": "third", "name": "AP3", "owners": [OTHER_USER, USER, OTHER_USER2]})
    test.automation_plot.mock({"id": "fourth", "name": "AP4", "owners": [OTHER_USER, USER_GROUP, OTHER_GROUP]})
    test.automation_plot.mock({"id": "fifth", "name": "AP5", "owners": [OTHER_USER, USER, USER_GROUP]})

    return test


@pytest.fixture(autouse=True)
def mock_user_groups(mp):
    return mp.function(staff.get_user_groups, return_value=[USER_GROUP, USER_GROUP2])


@pytest.fixture
def response(test):
    return test.api_client.get("/v1/user")


def test_unauthenticated(test, unauthenticated, response):
    assert response.status_code == http.client.UNAUTHORIZED


def test_status_code_is_ok(test, response):
    assert response.status_code == http.client.OK


def test_keys_in_response(test, response):
    assert set(response.json.keys()) == {
        "admin",
        "automation_plots",
        "groups",
        "login",
        "project_roles",
        "projects",
        "iam_user_info",
    }


def test_login(test, response):
    assert response.json["login"] == USER


def test_is_admin_as_user(test, response):
    assert response.json["admin"] is False


def test_is_admin_as_admin(test, authorized_admin, response):
    assert response.json["admin"] is True


def test_owner_of_projects(test, response):
    assert sorted(response.json["projects"]) == sorted(["second", "third", "fourth", "fifth"])


def test_owner_of_automation_plots(test, response):
    assert sorted(response.json["automation_plots"]) == sorted(["second", "third", "fourth", "fifth"])


def test_project_roles(test, response):
    assert response.status_code == http.client.OK
    role_to_projects = defaultdict(set)
    for project, roles in response.json["project_roles"].items():
        for role in roles:
            role_to_projects[role].add(project)

    assert role_to_projects[ProjectRole.OWNER] == {"second", "third", "fourth", "fifth"}
    assert role_to_projects[ProjectRole.SUPERUSER] == {"first", "second", "third", "fifth"}
    assert role_to_projects[ProjectRole.USER] == {"second", "third", "fourth"}
    assert role_to_projects[ProjectRole.NOC_ACCESS] == {"second", "fourth", "fifth"}
    assert role_to_projects[ProjectRole.SSH_REBOOTER] == {"first", "second", "fourth"}

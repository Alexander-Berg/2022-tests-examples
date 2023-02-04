import http.client

import pytest

from infra.walle.server.tests.lib.util import TestCase
from sepelib.core import config
from tests.api.common import project_settings
from walle.expert.automation_plot import AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.expert.types import CheckType, CheckSets
from walle.models import timestamp
from walle.projects import Project


@pytest.fixture
def test(request, monkeypatch_production_env, monkeypatch_timestamp):
    test = TestCase.create(request)
    return test


CUSTOM_PLOT = "custom-plot"
CUSTOM_CHECK = "custom-check"


def mock_automation_plot(test, automation_plot_id):
    test.automation_plot.mock(
        {
            "id": automation_plot_id,
            "checks": [
                {
                    "name": CUSTOM_CHECK,
                    "enabled": True,
                    "reboot": True,
                    "redeploy": True,
                    "start_time": timestamp() - 100,
                }
            ],
        }
    )


@pytest.mark.parametrize(
    "automation_plot_id, has_infiniband, expected_checks",
    [
        (None, False, CheckSets.BASIC),
        (AUTOMATION_PLOT_FULL_FEATURED_ID, False, CheckSets.FULL_FEATURED - set(CheckType.ALL_IB)),
        (CUSTOM_PLOT, False, (CheckSets.FULL_FEATURED | {CUSTOM_CHECK}) - set(CheckType.ALL_IB)),
        (AUTOMATION_PLOT_FULL_FEATURED_ID, True, CheckSets.FULL_FEATURED),
        (CUSTOM_PLOT, True, CheckSets.FULL_FEATURED | {CUSTOM_CHECK}),
    ],
)
def test_get_all_available_checks_for_project(test, automation_plot_id, has_infiniband, expected_checks):
    if automation_plot_id == CUSTOM_PLOT:
        mock_automation_plot(test, automation_plot_id)

    tags = []
    if has_infiniband:
        tags.append(config.get_value("infiniband.involvement_tag"))
    project = test.mock_project(
        {"id": "some-id", "name": "Some name", "tags": tags, "automation_plot_id": automation_plot_id}
    )
    result = test.api_client.get(
        f"/v1/projects/{project.id}",
        query_string={"fields": Project.ALL_AVAILABLE_PROJECT_CHECKS_FIELD},
    )
    assert result.status_code == http.client.OK
    assert frozenset(result.json[Project.ALL_AVAILABLE_PROJECT_CHECKS_FIELD]) == expected_checks


@pytest.mark.usefixtures(
    "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf", "monkeypatch_staff_get_user_groups"
)
@pytest.mark.parametrize(
    "automation_plot_id, disabled_check",
    [
        (None, CheckType.NETMON),
        (AUTOMATION_PLOT_FULL_FEATURED_ID, CheckType.DISK),
        (CUSTOM_PLOT, CUSTOM_CHECK),
    ],
)
def test_create_new_project_with_manual_disabled_check(mp, test, automation_plot_id, disabled_check):
    mp.config("authorization.admins", [test.api_user])
    if automation_plot_id == CUSTOM_PLOT:
        mock_automation_plot(test, automation_plot_id)

    project_settings_kwargs = {"manually_disabled_checks": [disabled_check]}
    if automation_plot_id:
        project_settings_kwargs["automation_plot_id"] = automation_plot_id
    project_json = project_settings(**project_settings_kwargs)
    create_result = test.api_client.post("/v1/projects", data=project_json)
    assert create_result.status_code == http.client.CREATED

    result = test.api_client.get(
        f"/v1/projects/{create_result.json['id']}",
        query_string={"fields": "manually_disabled_checks"},
    )
    assert result.json["manually_disabled_checks"] == [disabled_check]


@pytest.mark.parametrize(
    "automation_plot_id, disabled_check",
    [
        (None, CheckType.NETMON),
        (AUTOMATION_PLOT_FULL_FEATURED_ID, CheckType.DISK),
        (CUSTOM_PLOT, CUSTOM_CHECK),
    ],
)
def test_disable_and_enable_check(test, automation_plot_id, disabled_check):
    if automation_plot_id == CUSTOM_PLOT:
        mock_automation_plot(test, automation_plot_id)

    project = test.mock_project({"id": "some-id", "name": "Some name", "automation_plot_id": automation_plot_id})

    result = test.api_client.post(f"/v1/projects/{project.id}", data={"manually_disabled_checks": [disabled_check]})
    assert result.status_code == http.client.OK
    result = test.api_client.get(f"/v1/projects/{project.id}", query_string={"fields": "manually_disabled_checks"})
    assert result.json["manually_disabled_checks"] == [disabled_check]

    result = test.api_client.post(f"/v1/projects/{project.id}", data={"manually_disabled_checks": []})
    assert result.status_code == http.client.OK
    result = test.api_client.get(f"/v1/projects/{project.id}", query_string={"fields": "manually_disabled_checks"})
    assert result.json["manually_disabled_checks"] == []


@pytest.mark.parametrize(
    "automation_plot_id, disabled_check",
    [
        (None, CheckType.DISK),
        (AUTOMATION_PLOT_FULL_FEATURED_ID, CUSTOM_CHECK),
        (CUSTOM_PLOT, "totally_unknown_check"),
    ],
)
def test_unknown_check(test, automation_plot_id, disabled_check):
    if automation_plot_id == CUSTOM_PLOT:
        mock_automation_plot(test, automation_plot_id)
    project = test.mock_project(
        {"id": "some-id", "name": "Some name", "automation_plot_id": AUTOMATION_PLOT_FULL_FEATURED_ID}
    )

    result = test.api_client.post(f"/v1/projects/{project.id}", data={"manually_disabled_checks": [CUSTOM_CHECK]})
    assert result.status_code == http.client.BAD_REQUEST
    result = test.api_client.get(f"/v1/projects/{project.id}", query_string={"fields": "manually_disabled_checks"})
    assert result.json["manually_disabled_checks"] == []

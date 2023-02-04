"""Tests for projects' modify CMS API."""

import http.client as httplib

import pytest

from infra.walle.server.tests.lib.util import TestCase, drop_none
from walle import projects
from walle.clients.cms import CmsApiVersion

SERVICE_SLUG = "some_service"
CMS_URL = "http://project.yandex-team.ru/cms"
CMS_TVM_APP_ID = 500


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


def set_cms_settings_to_project(project, url=None, max_busy_hosts=None, api_version=None, tvm_app_id=None):
    project.cms = url
    project.cms_max_busy_hosts = max_busy_hosts
    project.cms_api_version = api_version
    project.cms_tvm_app_id = tvm_app_id
    project.cms_settings = [
        drop_none(
            {
                "cms": url,
                "cms_tvm_app_id": tvm_app_id,
                "cms_api_version": api_version,
                "cms_max_busy_hosts": max_busy_hosts,
                "temporary_unreachable_enabled": False,
            }
        )
    ]


@pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
def test_set_one_non_default_cms(test, api_version, mock_service_tvm_app_ids, mock_get_planner_id_by_bot_project_id):
    mock_service_tvm_app_ids([CMS_TVM_APP_ID])
    mock_get_planner_id_by_bot_project_id()

    project = test.mock_project({"id": "some-id", "name": "Some name"})
    set_cms_settings_to_project(project, url=CMS_URL, api_version=api_version, tvm_app_id=CMS_TVM_APP_ID)
    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={"cms_settings": [{"url": CMS_URL, "api_version": api_version, "tvm_app_id": CMS_TVM_APP_ID}]},
    )
    assert result.status_code == httplib.OK
    test.projects.assert_equal()


def test_set_one_default_cms(test):
    MAX_BUSY_HOST = 1
    project = test.mock_project({"id": "some-id", "name": "Some name"})
    set_cms_settings_to_project(project, url=projects.DEFAULT_CMS_NAME, max_busy_hosts=MAX_BUSY_HOST)

    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={"cms_settings": [{"url": project.cms, "max_busy_hosts": MAX_BUSY_HOST}]},
    )
    assert result.status_code == httplib.OK
    test.projects.assert_equal()


@pytest.mark.parametrize("cur_api_version", CmsApiVersion.ALL_CMS_API)
@pytest.mark.parametrize("prev_api_version", CmsApiVersion.ALL_CMS_API)
@pytest.mark.parametrize(["prev_cms_tvm_app_id", "cur_cms_tvm_app_id"], [(None, 500), (500, 600)])
@pytest.mark.parametrize(["prev_url", "cur_url"], [(CMS_URL, CMS_URL), ("prev_bad_name", CMS_URL)])
def test_modify_any_non_default_cms_settings(
    test,
    mock_get_planner_id_by_bot_project_id,
    mock_service_tvm_app_ids,
    prev_url,
    cur_url,
    prev_api_version,
    cur_api_version,
    prev_cms_tvm_app_id,
    cur_cms_tvm_app_id,
):
    mock_get_planner_id_by_bot_project_id()
    mock_service_tvm_app_ids([cur_cms_tvm_app_id])

    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "cms": prev_url,
            "cms_max_busy_hosts": None,
            "cms_api_version": prev_api_version,
            "cms_tvm_app_id": prev_cms_tvm_app_id,
            "cms_settings": [
                {
                    "cms": prev_url,
                    "cms_api_version": prev_api_version,
                    "cms_tvm_app_id": prev_cms_tvm_app_id,
                    "temporary_unreachable_enabled": False,
                }
            ],
        }
    )

    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={"cms_settings": [{"url": cur_url, "api_version": cur_api_version, "tvm_app_id": cur_cms_tvm_app_id}]},
    )
    assert result.status_code == httplib.OK

    set_cms_settings_to_project(project, url=cur_url, tvm_app_id=cur_cms_tvm_app_id, api_version=cur_api_version)

    test.projects.assert_equal()


@pytest.mark.parametrize(["prev_max_busy_host", "cur_max_busy_hosts"], [(1, 10), (10, 1)])
def test_modify_max_busy_hosts(test, prev_max_busy_host, cur_max_busy_hosts):
    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "cms": projects.DEFAULT_CMS_NAME,
            "cms_max_busy_hosts": prev_max_busy_host,
            "cms_settings": [
                {
                    "cms": projects.DEFAULT_CMS_NAME,
                    "cms_max_busy_hosts": prev_max_busy_host,
                    "temporary_unreachable_enabled": False,
                }
            ],
        }
    )

    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={"cms_settings": [{"url": projects.DEFAULT_CMS_NAME, "max_busy_hosts": cur_max_busy_hosts}]},
    )

    set_cms_settings_to_project(project, url=projects.DEFAULT_CMS_NAME, max_busy_hosts=cur_max_busy_hosts)

    assert result.status_code == httplib.OK
    test.projects.assert_equal()


@pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
@pytest.mark.parametrize(["prev_cms_tvm_app_id", "cur_cms_tvm_app_id"], [(None, 500), (500, 600)])
def test_cms_tvm_app_id_not_belonging_to_service(
    test,
    api_version,
    prev_cms_tvm_app_id,
    cur_cms_tvm_app_id,
    mock_get_planner_id_by_bot_project_id,
    mock_service_tvm_app_ids,
    mock_abc_get_service_slug,
):
    mock_get_planner_id_by_bot_project_id()
    mock_service_tvm_app_ids()
    mock_abc_get_service_slug()

    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "cms": CMS_URL,
            "cms_max_busy_hosts": None,
            "cms_api_version": api_version,
            "cms_tvm_app_id": prev_cms_tvm_app_id,
            "cms_settings": [
                {
                    "cms": CMS_URL,
                    "cms_api_version": api_version,
                    "cms_tvm_app_id": prev_cms_tvm_app_id,
                    "temporary_unreachable_enabled": False,
                }
            ],
        }
    )

    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={"cms_settings": [{"url": CMS_URL, "api_version": api_version, "tvm_app_id": cur_cms_tvm_app_id}]},
    )

    assert result.status_code == httplib.BAD_REQUEST
    assert result.json["message"] == "CMS TVM app id {} is not registered in ABC service {}".format(
        cur_cms_tvm_app_id, SERVICE_SLUG
    )

    test.projects.assert_equal()


def test_modify_with_custom_cms_and_max_busy_hosts(test):
    project = test.mock_project({"id": "some-id", "name": "Some name"})
    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={"cms_settings": [{"url": "http://foo.bar/cms", "max_busy_hosts": 10}]},
    )
    assert result.status_code == httplib.BAD_REQUEST
    test.projects.assert_equal()


@pytest.mark.parametrize("max_busy_hosts", [None, 0, "", "-"])
def test_modify_default_cms_invalid_max_busy_hosts(test, max_busy_hosts):
    project = test.mock_project({"id": "some-id", "name": "Some name"})
    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={"cms_settings": [{"url": "http://foo.bar/cms", "max_busy_hosts": max_busy_hosts}]},
    )
    assert result.status_code == httplib.BAD_REQUEST
    test.projects.assert_equal()


@pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
def test_modify_default_cms_to_custom_without_cms_tvm_app_id(test, api_version):
    project = test.mock_project(
        {"id": "some-id", "name": "Some name", "cms": projects.DEFAULT_CMS_NAME, "cms_max_busy_hosts": 5}
    )
    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={"cms_settings": [{"url": "http://foo.bar/cms", "api_version": api_version}]},
    )
    assert result.status_code == httplib.BAD_REQUEST
    assert (
        result.json["message"] == "CMS TVM app id must be set for non-default CMSes. If your CMS does not support "
        "TVM yet, please contact Wall-e administrators"
    )
    test.projects.assert_equal()


@pytest.mark.parametrize("count_of_added_cms", [1, 2, 3, 10])
def test_add_n_non_default_cms(
    test, mock_get_planner_id_by_bot_project_id, mock_service_tvm_app_ids, count_of_added_cms
):
    mock_get_planner_id_by_bot_project_id()
    project = test.mock_project({"id": "some-id", "name": "Some name"})

    cms_tvm_app_ids = []
    all_cms_settings_request = []
    all_cms_settings_document = []

    for i in range(count_of_added_cms):
        new_tvm_app_id = CMS_TVM_APP_ID + i
        cms_tvm_app_ids.append(new_tvm_app_id)
        settings_request = {"url": CMS_URL + str(i), "api_version": CmsApiVersion.V1_4, "tvm_app_id": new_tvm_app_id}
        settings_document = {
            "cms": CMS_URL + str(i),
            "cms_tvm_app_id": new_tvm_app_id,
            "cms_api_version": CmsApiVersion.V1_4,
            "temporary_unreachable_enabled": False,
        }
        all_cms_settings_request.append(settings_request)
        all_cms_settings_document.append(settings_document)

    mock_service_tvm_app_ids(cms_tvm_app_ids)

    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id), data={"cms_settings": all_cms_settings_request}
    )

    project.cms_settings = all_cms_settings_document
    project.cms = all_cms_settings_document[-1]["cms"]
    project.cms_api_version = all_cms_settings_document[-1]["cms_api_version"]
    project.cms_tvm_app_id = all_cms_settings_document[-1]["cms_tvm_app_id"]
    project.cms_max_busy_hosts = None

    assert result.status_code == httplib.OK
    test.projects.assert_equal()


def test_error_on_set_empty_cms_settings(test):
    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "cms_settings": [
                {
                    "cms": "some-url",
                    "cms_api_version": CmsApiVersion.V1_4,
                    "cms_tvm_app_id": 11110000,
                    "temporary_unreachable_enabled": False,
                }
            ],
        }
    )
    result = test.api_client.post("/v1/projects/{}/cms_settings".format(project.id), data={"cms_settings": []})
    assert result.status_code == httplib.BAD_REQUEST
    assert result.json["message"] == "You must specify at least one cms settings"
    test.projects.assert_equal()


def test_unauthorized(test, unauthorized_project):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={"cms_settings": [{"url": projects.DEFAULT_CMS_NAME, "max_busy_hosts": 1}]},
    )
    assert result.status_code == httplib.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize(
    "data",
    [
        {"cms_settings": [{"url": projects.DEFAULT_CMS_NAME, "max_busy_hosts": 1, "api_version": CmsApiVersion.V1_4}]},
        {"cms_settings": [{"url": projects.DEFAULT_CMS_NAME, "max_busy_hosts": 1, "tvm_app_id": 10002000}]},
        {"cms_settings": [{"url": "some-custom", "max_busy_hosts": 1, "api_version": CmsApiVersion.V1_4}]},
        {"cms_settings": [{"url": "some-custom", "tvm_app_id": 10002000}]},
        {},
    ],
)
def test_get_error_when_send_not_valid_data(test, data):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.post("/v1/projects/{}/cms_settings".format(project.id), data=data)
    assert result.status_code == httplib.BAD_REQUEST

    test.projects.assert_equal()


def test_get_error_when_send_duplicated_settings(test):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={
            "cms_settings": [
                {"url": projects.DEFAULT_CMS_NAME, "max_busy_hosts": 1},
                {"url": projects.DEFAULT_CMS_NAME, "max_busy_hosts": 1},
            ]
        },
    )
    assert result.status_code == httplib.BAD_REQUEST
    assert result.json["message"] == "All cms settings must be different"

    test.projects.assert_equal()


def test_get_error_when_send_default_cms_and_some_custom(
    test, mock_service_tvm_app_ids, mock_get_planner_id_by_bot_project_id
):
    mock_service_tvm_app_ids([CMS_TVM_APP_ID])
    mock_get_planner_id_by_bot_project_id()

    project = test.mock_project({"id": "some-id"})

    result = test.api_client.post(
        "/v1/projects/{}/cms_settings".format(project.id),
        data={
            "cms_settings": [
                {"url": projects.DEFAULT_CMS_NAME, "max_busy_hosts": 1},
                {"url": CMS_URL, "api_version": CmsApiVersion.V1_4, "tvm_app_id": CMS_TVM_APP_ID},
            ]
        },
    )
    assert result.status_code == httplib.BAD_REQUEST
    assert result.json["message"] == "Default cms can be the only one cms of project"

    test.projects.assert_equal()

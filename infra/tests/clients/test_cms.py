"""Tests CMS clients for different versions."""

import json
from unittest.mock import Mock, call

import pytest

from infra.walle.server.tests.lib.util import mock_response, monkeypatch_config
from walle.clients import juggler, cms, tvm
from walle.projects import CmsSettings
from walle.stats import StatsManager
from .test_tvm import service_ticket


@pytest.fixture(autouse=True)
def test(mp):
    monkeypatch_config(mp, "cms.namespace", "unit-test")
    mp.function(tvm.get_ticket_for_service, side_effect=service_ticket)


@pytest.fixture(autouse=True)
def mocked_project(walle_test):
    return walle_test.mock_project({"id": "project-id-mock"})


class JsonStr:
    def __init__(self, data):
        self._data = data

    def __eq__(self, other):
        return isinstance(other, str) and json.loads(other) == self._data

    def __ne__(self, other):
        return not isinstance(other, str) or json.loads(other) != self._data

    def __repr__(self):
        return "<JsonStr({})>".format(json.dumps(self._data))


@pytest.fixture()
def mock_juggler_sender(mp):
    return mp.function(juggler.send_event)


@pytest.fixture
def mock_stats_manager(mp):
    stats_manager = StatsManager()
    mp.setattr(cms, "stats_manager", stats_manager)
    return stats_manager


class TestCmsApiClient:
    @pytest.mark.parametrize("version", cms.CmsApiVersion.ALL_CMS_API)
    def test_version_select(self, version):
        # Test we can actually get clients for all supported versions
        client = cms.get_cms_client(version)
        assert client.api_version == version

    @pytest.mark.parametrize("version", cms.CmsApiVersion.ALL_CMS_API)
    @pytest.mark.parametrize("cms_task_type", cms.CmsTaskType.ALL)
    def test_extra_stripped(self, version, cms_task_type, mock_juggler_sender, mock_stats_manager):
        task = self._get_minimal_cms_api_task(cms_task_type, extra={"extra": "mock"})

        expected = task.copy()
        if version not in [cms.CmsApiVersion.V1_3, cms.CmsApiVersion.V1_4]:
            del expected["extra"]

        client, mock_url_opener = self._get_stubbed_cms_client(
            version, dict(expected, status=cms.CmsTaskStatus.IN_PROCESS)
        )
        client.add_task(**task)

        self._assert_task_posted(mock_url_opener, expected)
        self._assert_juggler_event_sent(mock_juggler_sender, client)
        self._assert_yasm_stats_collected(mock_stats_manager, "add_task", "success")

    @pytest.mark.parametrize("version", cms.CmsApiVersion.ALL_CMS_API)
    @pytest.mark.parametrize("cms_task_type", cms.CmsTaskType.ALL)
    def test_comment_stripped(self, cms_task_type, version, mock_juggler_sender, mock_stats_manager):
        task = self._get_minimal_cms_api_task(cms_task_type, comment="comment-mock")

        expected = task.copy()
        if version in {cms.CmsApiVersion.V1_0, cms.CmsApiVersion.V1_1}:
            del expected["comment"]

        client, mock_url_opener = self._get_stubbed_cms_client(
            version, dict(expected, status=cms.CmsTaskStatus.IN_PROCESS)
        )
        client.add_task(**task)

        self._assert_task_posted(mock_url_opener, expected)
        self._assert_juggler_event_sent(mock_juggler_sender, client)
        self._assert_yasm_stats_collected(mock_stats_manager, "add_task", "success")

    @pytest.mark.parametrize("version", cms.CmsApiVersion.ALL_CMS_API)
    @pytest.mark.parametrize("action", cms.CmsTaskAction.ALL)
    @pytest.mark.parametrize("cms_task_type", cms.CmsTaskType.ALL)
    def test_supported_actions_handled(self, version, action, cms_task_type, mock_juggler_sender, mock_stats_manager):
        task = self._get_minimal_cms_api_task(cms_task_type, action=action)

        client, mock_url_opener = self._get_stubbed_cms_client(version, dict(task, status=cms.CmsTaskStatus.IN_PROCESS))

        if action not in client._supported_actions:
            return

        client.add_task(**task)

        self._assert_task_posted(mock_url_opener, task)
        self._assert_juggler_event_sent(mock_juggler_sender, client)
        self._assert_yasm_stats_collected(mock_stats_manager, "add_task", "success")

    @pytest.mark.parametrize("version", cms.CmsApiVersion.ALL_CMS_API)
    @pytest.mark.parametrize("action", cms.CmsTaskAction.ALL)
    @pytest.mark.parametrize("cms_task_type", cms.CmsTaskType.ALL)
    def test_unsupported_actions_downscaled(
        self, version, action, cms_task_type, mock_juggler_sender, mock_stats_manager
    ):
        if version == cms.CmsApiVersion.V1_0 and action in [cms.CmsTaskAction.PREPARE, cms.CmsTaskAction.DEACTIVATE]:
            # check test_v1_0_returns_fake_allow for it
            return

        task = self._get_minimal_cms_api_task(cms_task_type, comment="comment-mock")

        expected = task.copy()
        if version in {cms.CmsApiVersion.V1_0, cms.CmsApiVersion.V1_1}:
            del expected["comment"]

        client, mock_url_opener = self._get_stubbed_cms_client(
            version, dict(expected, status=cms.CmsTaskStatus.IN_PROCESS)
        )

        if action in client._supported_actions:
            # we only care about unsupported actions
            return

        client.add_task(**task)

        self._assert_task_posted(mock_url_opener, expected)
        self._assert_juggler_event_sent(mock_juggler_sender, client)
        self._assert_yasm_stats_collected(mock_stats_manager, "add_task", "success")

    @pytest.mark.parametrize("action", [cms.CmsTaskAction.PREPARE, cms.CmsTaskAction.DEACTIVATE])
    @pytest.mark.parametrize("cms_task_type", cms.CmsTaskType.ALL)
    def test_v1_0_returns_fake_allow(self, action, cms_task_type, mock_juggler_sender, mock_stats_manager):
        task = self._get_minimal_cms_api_task(cms_task_type, action=action)

        client, mock_url_opener = self._get_stubbed_cms_client(
            cms.CmsApiVersion.V1_0, dict(task, status=cms.CmsTaskStatus.IN_PROCESS)
        )
        client.add_task(**task)

        assert not mock_url_opener.open.called
        assert not mock_juggler_sender.called
        assert {} == mock_stats_manager.dump()

    @pytest.mark.parametrize("version", set(cms.CmsApiVersion.ALL_CMS_API) - {cms.CmsApiVersion.V1_0})
    @pytest.mark.parametrize("action", cms.CmsTaskAction.ALL)
    @pytest.mark.parametrize("cms_task_type", cms.CmsTaskType.ALL)
    def test_oauth_token_is_sent(self, mp, version, action, cms_task_type):
        task = self._get_minimal_cms_api_task(cms_task_type, action=action)

        client, mock_url_opener = self._get_stubbed_cms_client(version, dict(task, status=cms.CmsTaskStatus.IN_PROCESS))

        token = "AQAD-SOME-TOKEN"
        monkeypatch_config(mp, "cms.access_token", token)

        client.add_task(**task)
        headers = mock_url_opener.open.call_args[1]["headers"]
        assert headers["Authorization"] == "OAuth {}".format(token)
        assert "X-Ya-Service-Ticket" not in headers

    @pytest.mark.parametrize("version", set(cms.CmsApiVersion.ALL_CMS_API) - {cms.CmsApiVersion.V1_0})
    @pytest.mark.parametrize("action", cms.CmsTaskAction.ALL)
    @pytest.mark.parametrize("cms_task_type", cms.CmsTaskType.ALL)
    def test_tvm_service_ticket_is_sent_if_cms_tvm_app_id_is_set(self, mocked_project, version, action, cms_task_type):
        task = self._get_minimal_cms_api_task(cms_task_type, action=action)

        client, mock_url_opener = self._get_stubbed_cms_client(
            version, dict(task, status=cms.CmsTaskStatus.IN_PROCESS), use_tvm=True
        )

        mocked_project.cms_tvm_app_id = 10001000
        mocked_project.save()

        client.add_task(**task)
        headers = mock_url_opener.open.call_args[1]["headers"]
        assert headers["X-Ya-Service-Ticket"] == "cms:project-id-mock:cms-api-mock-service-ticket"
        assert "Authorization" not in headers

    @staticmethod
    def _get_minimal_cms_api_task(cms_task_type=cms.CmsTaskType.MANUAL, **fields):
        return dict(
            {
                "id": "cms-task-mock",
                "issuer": "issuer-mock",
                "type": cms_task_type,
                "action": cms.CmsTaskAction.REBOOT,
                "hosts": ["host-mock"],
            },
            **fields
        )

    @staticmethod
    def _get_stubbed_cms_client(version, response_data, use_tvm=False):
        mock_url_opener = Mock()
        mock_url_opener.open.return_value = mock_response(response_data)

        client_class = cms.get_cms_client(version)

        client = client_class(
            "cms-mock", "project-id-mock", "cms-api-mock", url_opener=mock_url_opener, use_tvm=use_tvm
        )

        return client, mock_url_opener

    @staticmethod
    def _assert_task_posted(mock_url_opener, task):
        mock_url_opener.open.assert_called_once_with(
            "POST",
            "cms-api-mock/tasks",
            data=JsonStr(task),
            headers={"Content-Type": "application/json"},
            params=None,
            timeout=None,
        )

    @staticmethod
    def _assert_juggler_event_sent(mock_juggler_sender, client):
        message = "Request to add_task to CMS cms-mock was successful."
        tags = [
            "wall-e.cms.cms-mock",
            "wall-e.cms.version.{}".format(client.api_version),
            "wall-e.cms.add_task",
            "wall-e.cms.project-id-mock",
            "wall-e.project.project-id-mock",
        ]

        assert mock_juggler_sender.mock_calls == [
            call("wall-e-cms-request", "OK", host_name="wall-e.cms.cms-mock", message=message, tags=tags),
            call("wall-e-cms-request", "OK", host_name="wall-e.cms.add_task.cms-mock", message=message, tags=tags),
        ]

    @staticmethod
    def _assert_yasm_stats_collected(stats_manager, action, result):
        stats = stats_manager.dump()
        expected_keys = {
            "cms.{}.cms-mock.{}.count".format(action, result),
            "cms.{}.cms-mock.{}.time".format(action, result),
            "cms.cms-mock.{}.count".format(result),
            "cms.cms-mock.{}.time".format(result),
        }

        assert expected_keys == set(stats)


@pytest.mark.parametrize(
    "api_url, api_version, supports_tvm",
    [("default", v, False) for v in cms.CmsApiVersion.ALL_CMS_API]
    + [("http://some-custom-cms.y-t.ru", v, True) for v in cms.CmsApiVersion.ALL_CMS_API],
)
def test_cms_supports_tvm(api_url, api_version, supports_tvm):
    cms_settings = CmsSettings(
        api_url, api_version=api_version, max_busy_hosts=None, tvm_app_id=None, temporary_unreachable_enabled=False
    )
    assert cms_settings.supports_tvm() == supports_tvm

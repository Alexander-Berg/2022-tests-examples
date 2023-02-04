import pytest

from infra.walle.server.tests.lib.util import (
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
    monkeypatch_function,
    any_task_status,
    monkeypatch_config,
    mock_stage_internal_error,
    mock_fail_current_stage,
    monkeypatch_request,
    mock_response,
)
from walle.clients import cauth
from walle.clients.utils import check_certs_exist
from walle.fsm_stages import add_host_to_cauth
from walle.fsm_stages.common import get_current_stage
from walle.hosts import HostState
from walle.models import timestamp
from walle.projects import CauthSettingsDocument
from walle.stages import Stages, Stage


@pytest.fixture()
def push_enabled(monkeypatch, monkeypatch_timestamp):
    monkeypatch_config(monkeypatch, "cauth.add_hosts_via_push", True)


@pytest.fixture()
def host_mock(walle_test):
    host = walle_test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "task": mock_task(stage=Stages.ADD_HOST_TO_CAUTH, ignore_cms=True),
        }
    )
    return host


@pytest.fixture()
def prepare_client(monkeypatch):
    monkeypatch_config(monkeypatch, "cauth.api_url", "some-url")
    monkeypatch_config(monkeypatch, "cauth.cert_path", "/some/path.cert")
    monkeypatch_config(monkeypatch, "cauth.key_path", "/some/path.key")
    monkeypatch_function(monkeypatch, check_certs_exist, module=cauth)


def test_stage_initialization(walle_test):
    check_stage_initialization(walle_test, Stage(name=Stages.ADD_HOST_TO_CAUTH))


@pytest.mark.parametrize("push_enabled", [True, False])
def test_config_option_matters(walle_test, monkeypatch, host_mock, push_enabled):
    monkeypatch_config(monkeypatch, "cauth.add_hosts_via_push", push_enabled)

    push_host_mock = monkeypatch_function(monkeypatch, add_host_to_cauth._push_host)

    handle_host(host_mock)

    if push_enabled:
        push_host_mock.assert_called_once_with(host_mock)
    else:
        assert not push_host_mock.called

    mock_complete_current_stage(host_mock)
    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("push_enabled")
def test_non_cauth_exceptions_fails_stage(walle_test, monkeypatch, host_mock):
    monkeypatch_function(monkeypatch, add_host_to_cauth._push_host, side_effect=Exception("some error"))
    log_exception_mock = monkeypatch_function(monkeypatch, add_host_to_cauth._log_exception)

    handle_host(host_mock)

    assert log_exception_mock.called

    mock_fail_current_stage(host_mock, "Push host to CAuth failed: some error")
    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("push_enabled")
def test_retries_on_cauth_error(walle_test, host_mock, monkeypatch):
    monkeypatch_function(monkeypatch, add_host_to_cauth._push_host, side_effect=cauth.CAuthError("some error"))
    get_current_stage(host_mock).status_time = timestamp() - add_host_to_cauth.STAGE_TIMEOUT + 1
    host_mock.save()

    with pytest.raises(cauth.CAuthError):
        handle_host(host_mock)

    mock_stage_internal_error(host_mock, "Error in communication with CAuth: some error")
    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("push_enabled")
def test_failing_stage_on_cauth_error_with_timeout(walle_test, host_mock, monkeypatch):
    monkeypatch_function(monkeypatch, add_host_to_cauth._push_host, side_effect=cauth.CAuthError("some error"))
    get_current_stage(host_mock).status_time = timestamp() - add_host_to_cauth.STAGE_TIMEOUT - 1
    host_mock.save()

    handle_host(host_mock)

    mock_fail_current_stage(
        host_mock, "Push host to CAuth stage timed out. Error in communication with CAuth: some error"
    )
    walle_test.hosts.assert_equal()


def test_client_passes_cauth_params_from_project(push_enabled, host_mock, monkeypatch, prepare_client):
    request_mock = monkeypatch_request(monkeypatch, mock_response({"status": "added", "srv": host_mock.name}))

    cauth_settings = CauthSettingsDocument(
        flow_type=cauth.CauthFlowType.BACKEND_SOURCES,
        trusted_sources=[cauth.CauthSource.WALLE, cauth.CauthSource.IDM],
    )
    project = host_mock.get_project()
    project.cauth_settings = cauth_settings
    project.save()

    handle_host(host_mock)

    request_kwargs = request_mock.call_args[1]
    assert request_kwargs["data"] == {
        "srv": host_mock.name,
        "grp": "walle.{}".format(host_mock.project),
        "flow": cauth.CauthFlowType.BACKEND_SOURCES,
        "trusted_sources": ",".join([cauth.CauthSource.WALLE, cauth.CauthSource.IDM]),
        "key_sources": cauth.CAuthKeySources.STAFF,
    }

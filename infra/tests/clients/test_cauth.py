import pytest
from requests import RequestException

import walle.clients.utils
from infra.walle.server.tests.lib.util import (
    monkeypatch_config,
    monkeypatch_function,
    monkeypatch_request,
    mock_response,
)
from walle.clients import cauth

API_URL = "some-endpoint.cauth.y-t.ru"
CERT_PATH = "/some/path.cert"
KEY_PATH = "/some/path.key"
HOST = "sas1-1234.search.yandex.net"
PROJECT = "testing"


@pytest.fixture(autouse=True)
def prepare_config(monkeypatch):
    monkeypatch_config(monkeypatch, "cauth.api_url", API_URL)
    monkeypatch_config(monkeypatch, "cauth.cert_path", CERT_PATH)
    monkeypatch_config(monkeypatch, "cauth.key_path", KEY_PATH)


@pytest.fixture(autouse=True)
def mock_check_certs_exist(monkeypatch):
    return monkeypatch_function(monkeypatch, walle.clients.utils.check_certs_exist, module=cauth)


class TestCAuthClient:
    def test_request_params(self, monkeypatch):
        request_mock = monkeypatch_request(monkeypatch, mock_response({"status": "added", "srv": HOST}))

        cauth.add_host_via_push(HOST, PROJECT)

        request_args = request_mock.call_args[0]
        assert request_args == ("POST", "https://{}/add_server/".format(API_URL))

        request_kwargs = request_mock.call_args[1]
        assert request_kwargs["data"] == {"srv": HOST, "grp": "walle.{}".format(PROJECT)}
        assert request_kwargs["cert"] == (CERT_PATH, KEY_PATH)

    def test_request_with_cauth_params(self, monkeypatch):
        request_mock = monkeypatch_request(monkeypatch, mock_response({"status": "added", "srv": HOST}))
        cauth_params = {
            "flow": cauth.CauthFlowType.BACKEND_SOURCES,
            "trusted_sources": cauth.CauthSource.WALLE,
        }
        cauth.add_host_via_push(HOST, PROJECT, cauth_params)

        request_args = request_mock.call_args[0]
        assert request_args == ("POST", "https://{}/add_server/".format(API_URL))

        request_kwargs = request_mock.call_args[1]
        assert request_kwargs["data"] == {
            "srv": HOST,
            "grp": "walle.{}".format(PROJECT),
            "flow": cauth.CauthFlowType.BACKEND_SOURCES,
            "trusted_sources": cauth.CauthSource.WALLE,
        }
        assert request_kwargs["cert"] == (CERT_PATH, KEY_PATH)

    @pytest.mark.parametrize(
        "status_code, content_type", [(200, "text/plain"), (403, "text/plain"), (200, "text/plain; charset=utf-8")]
    )
    def test_recognizes_error_response(self, monkeypatch, status_code, content_type):
        error_text = "Some childish error in plain text"
        monkeypatch_request(
            monkeypatch,
            mock_response(error_text, status_code=status_code, headers={"Content-Type": content_type}, as_json=False),
        )

        with pytest.raises(cauth.CAuthError) as exc_info:
            cauth.add_host_via_push(HOST, PROJECT)

        assert str(exc_info.value) == "Error in communication with CAuth: {}".format(error_text)

    def test_validates_json(self, monkeypatch):
        wrong_status = "who knows? i am just a poor service"
        monkeypatch_request(monkeypatch, mock_response({"status": wrong_status, "srv": HOST}))

        with pytest.raises(cauth.CAuthError) as exc_info:
            cauth.add_host_via_push(HOST, PROJECT)

        expected_message = (
            "The server returned an invalid JSON response: result["
            "'status'] has an invalid value: '{}'.".format(wrong_status)
        )
        assert str(exc_info.value) == str(cauth.CAuthError(expected_message))

    def test_wraps_request_exceptions(self, monkeypatch):
        error = "mysterious error from the deep"
        monkeypatch_request(monkeypatch, side_effect=RequestException(error))

        with pytest.raises(cauth.CAuthError) as exc_info:
            cauth.add_host_via_push(HOST, PROJECT)
        assert str(exc_info.value) == "Error in communication with CAuth: {}".format(error)

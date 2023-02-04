"""Test certificate issuing."""

import json
from collections import namedtuple
from unittest.mock import ANY

import pytest
import requests

import walle.clients.utils
import walle.util.misc
from infra.walle.server.tests.lib.util import (
    TestCase,
    check_stage_initialization,
    mock_task,
    handle_host,
    mock_commit_stage_changes,
    mock_fail_current_stage,
    mock_stage_internal_error,
    mock_complete_current_stage,
    load_mock_data,
)
from sepelib.core import config
from walle.clients import certificator
from walle.fsm_stages.certificate import (
    _STATUS_PREPARE,
    _STATUS_REQUESTING,
    _STATUS_WAITING,
    _STATUS_FETCHING,
    _CERTIFICATOR_POLLING_TIMEOUT,
    _CERTIFICATE_REPLICA_WAIT_TIMEOUT,
)
from walle.fsm_stages.common import get_current_stage, get_parent_stage
from walle.hosts import DeployConfiguration
from walle.models import timestamp
from walle.stages import Stage, Stages


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.fixture(scope="module")
def certificator_mock_response():
    return json.loads(load_mock_data("mocks/certificator-response.json"))


@pytest.fixture(scope="module")
def certificator_mock_certificate():
    return load_mock_data("mocks/certificator-certificate-response.txt")


@pytest.fixture
def certificator_config(mp):
    print(mp)
    mp.setitem(config.get_value("certificator"), "access_token", "access-token-mock")


def _certificate_info(certificator_response):
    return {
        "url": certificator_response["url"],
        "status": certificator_response["status"],
        "download": certificator._get_download_url(certificator_response),
    }


def test_initiate_stage(test):
    check_stage_initialization(test, Stage(name=Stages.ISSUE_CERTIFICATE), status=_STATUS_PREPARE)


def conf_mock(certificate):
    return DeployConfiguration(None, None, None, certificate, None, True, None)


@pytest.mark.parametrize(
    ["parent_stage_params", "parent_stage_data"],
    [
        ({"config": conf_mock(certificate=True)}, None),
        ({"config": conf_mock(certificate=False)}, {"config_override": conf_mock(certificate=True)}),
    ],
)
def test_prepare_not_skip(test, parent_stage_params, parent_stage_data):
    task = mock_task(
        stage=Stage(name=Stages.ISSUE_CERTIFICATE, status=_STATUS_PREPARE),
        parent_stage_params=parent_stage_params,
        parent_stage_data=parent_stage_data,
    )
    host = test.mock_host({"task": task})

    handle_host(host)
    mock_complete_current_stage(host)


@pytest.mark.parametrize(
    ["parent_stage_params", "parent_stage_data"],
    [
        ({"config": conf_mock(certificate=False)}, None),  # certificate not required
        (None, {"config_override": conf_mock(certificate=False)}),  # certificate not required
        ({"config": conf_mock(certificate=True)}, {"certificate": "certificate mock"}),  # certificate already issued
    ],
)
def test_prepare_skip(test, parent_stage_params, parent_stage_data):
    task = mock_task(
        stage=Stage(name=Stages.ISSUE_CERTIFICATE, status=_STATUS_PREPARE),
        parent_stage_params=parent_stage_params,
        parent_stage_data=parent_stage_data,
    )
    host = test.mock_host({"task": task})

    handle_host(host)
    mock_complete_current_stage(host)


@pytest.mark.parametrize(
    "params",
    [
        {
            "certificate_status": certificator.CERTIFICATE_STATUS_ISSUED,
            "stage_kwargs": {"status": _STATUS_FETCHING, "check_now": True},
        },
        {
            "certificate_status": certificator.CERTIFICATE_STATUS_REQUESTED,
            "stage_kwargs": {"status": _STATUS_WAITING, "check_after": _CERTIFICATOR_POLLING_TIMEOUT},
        },
    ],
)
def test_certificate_issue_success(mp, test, certificator_mock_response, params):
    host = test.mock_host({"task": mock_task(stage=Stage(name=Stages.ISSUE_CERTIFICATE, status=_STATUS_REQUESTING))})
    certificator_response = dict(certificator_mock_response, status=params["certificate_status"])
    mp.function(certificator.json_request, return_value=certificator_response)

    handle_host(host)

    stage = get_current_stage(host)
    stage.set_temp_data("certificate_info", _certificate_info(certificator_response))
    mock_commit_stage_changes(host, **params["stage_kwargs"])
    test.hosts.assert_equal()


def test_certificate_issue_skip(test):
    host = test.mock_host({"task": mock_task(stage=Stage(name=Stages.ISSUE_CERTIFICATE, status=_STATUS_REQUESTING))})
    get_parent_stage(host).set_data("certificate", "certificate mock")
    host.save()

    handle_host(host)
    mock_complete_current_stage(host)


@pytest.mark.parametrize(
    "params",
    [
        {
            "certificate_status": certificator.CERTIFICATE_STATUS_ISSUED,
            "stage_kwargs": {"status": _STATUS_FETCHING, "check_now": True},
        },
        {
            "certificate_status": certificator.CERTIFICATE_STATUS_REQUESTED,
            "stage_kwargs": {"check_after": _CERTIFICATOR_POLLING_TIMEOUT},
        },
    ],
)
def test_certificate_wait_success(mp, test, certificator_mock_response, params):
    initial_certificate_info = _certificate_info(
        dict(certificator_mock_response, status=certificator.CERTIFICATE_STATUS_REQUESTED)
    )

    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(
                    name=Stages.ISSUE_CERTIFICATE,
                    status=_STATUS_WAITING,
                    temp_data={"certificate_info": initial_certificate_info},
                )
            )
        }
    )

    certificator_response = dict(certificator_mock_response, status=params["certificate_status"])
    mp.function(certificator.json_request, return_value=certificator_response)

    handle_host(host)

    stage = get_current_stage(host)
    stage.set_temp_data("certificate_info", _certificate_info(certificator_response))
    mock_commit_stage_changes(host, **params["stage_kwargs"])
    test.hosts.assert_equal()


def test_certificate_primary_fetch_success(mp, test, certificator_mock_response, certificator_mock_certificate):
    initial_certificate_info = _certificate_info(
        dict(certificator_mock_response, status=certificator.CERTIFICATE_STATUS_ISSUED)
    )

    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(
                    name=Stages.ISSUE_CERTIFICATE,
                    status=_STATUS_FETCHING,
                    temp_data={"certificate_info": initial_certificate_info},
                )
            )
        }
    )

    mp.function(certificator.raw_request, return_value=certificator_mock_certificate)
    mock_fetch = mp.method(
        certificator.Certificate.fetch, obj=certificator.Certificate, side_effect=certificator.Certificate.fetch
    )

    handle_host(host)

    parent_stage = get_parent_stage(host)
    parent_stage.set_data("certificate", certificator_mock_certificate)
    mock_complete_current_stage(host)

    mock_fetch.assert_called_once_with(ANY, force_primary=True)
    test.hosts.assert_equal()


def test_certificate_secondary_fetch_success(mp, test, certificator_mock_response, certificator_mock_certificate):
    initial_certificate_info = _certificate_info(
        dict(certificator_mock_response, status=certificator.CERTIFICATE_STATUS_ISSUED)
    )

    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(
                    name=Stages.ISSUE_CERTIFICATE,
                    status=_STATUS_FETCHING,
                    temp_data={"certificate_info": initial_certificate_info, "offload_fetching_to_secondary": True},
                )
            )
        }
    )

    mp.function(certificator.raw_request, return_value=certificator_mock_certificate)
    mock_fetch = mp.method(
        certificator.Certificate.fetch, obj=certificator.Certificate, side_effect=certificator.Certificate.fetch
    )

    handle_host(host)

    parent_stage = get_parent_stage(host)
    parent_stage.set_data("certificate", certificator_mock_certificate)
    mock_complete_current_stage(host)

    mock_fetch.assert_called_once_with(ANY, force_primary=False)
    test.hosts.assert_equal()


@pytest.mark.parametrize("error_code", [400, 403, 404, 500, 502])
@pytest.mark.parametrize("stage_status", [_STATUS_REQUESTING, _STATUS_WAITING, _STATUS_FETCHING])
def test_certificate_request_failure(
    mp, test, certificator_mock_response, certificator_config, error_code, stage_status
):
    initial_certificate_info = _certificate_info(
        dict(
            certificator_mock_response,
            status=certificator.CERTIFICATE_STATUS_REQUESTED,
            url=certificator._api_url("/certificate/"),
            download2=certificator._api_url("/certificate/43857/download"),
        )
    )

    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(
                    name=Stages.ISSUE_CERTIFICATE,
                    status=stage_status,
                    status_time=timestamp(),
                    temp_data={"certificate_info": initial_certificate_info},
                )
            )
        }
    )

    mock_response = namedtuple("MockResponse", ["status_code", "text"])(error_code, "RESPONSE MOCK BODY")
    mp.function(
        walle.clients.utils.request, side_effect=requests.RequestException("error-mock", response=mock_response)
    )

    error_msg = "Error in communication with Certificator: Failed to fetch {}: error-mock RESPONSE MOCK BODY"

    if error_code >= 500:
        with pytest.raises(certificator.CertificatorInternalError):
            handle_host(host)
        if stage_status == _STATUS_FETCHING:
            error_msg = error_msg.format(initial_certificate_info["download"])
        else:
            error_msg = error_msg.format(initial_certificate_info["url"])

        mock_stage_internal_error(host, error_msg)
    elif stage_status == _STATUS_FETCHING and error_code == 404:
        handle_host(host)

        error_msg = error_msg.format(initial_certificate_info["download"])

        get_current_stage(host).set_temp_data("offload_fetching_to_secondary", True)
        mock_commit_stage_changes(host, error=error_msg, check_after=_CERTIFICATOR_POLLING_TIMEOUT)
    else:
        handle_host(host)

        if stage_status == _STATUS_FETCHING:
            error_msg = error_msg.format(initial_certificate_info["download"])
        else:
            error_msg = error_msg.format(initial_certificate_info["url"])

        mock_fail_current_stage(host, reason=error_msg)

    test.hosts.assert_equal()


def test_certificate_download_fail(mp, test, certificator_mock_response, certificator_config):
    initial_certificate_info = _certificate_info(
        dict(
            certificator_mock_response,
            status=certificator.CERTIFICATE_STATUS_REQUESTED,
            url=certificator._api_url("/certificate/"),
            download2=certificator._api_url("/certificate/43857/download"),
        )
    )

    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(
                    name=Stages.ISSUE_CERTIFICATE,
                    status=_STATUS_FETCHING,
                    status_time=timestamp() - _CERTIFICATE_REPLICA_WAIT_TIMEOUT,
                    temp_data={"certificate_info": initial_certificate_info},
                )
            )
        }
    )

    mock_response = namedtuple("MockResponse", ["status_code", "text"])(404, "RESPONSE MOCK BODY")
    mp.function(
        walle.clients.utils.request, side_effect=requests.RequestException("error-mock", response=mock_response)
    )

    handle_host(host)

    reason = "Error in communication with Certificator: Failed to fetch {}: error-mock RESPONSE MOCK BODY"
    reason = reason.format(initial_certificate_info["download"])
    mock_fail_current_stage(host, reason=reason)
    test.hosts.assert_equal()

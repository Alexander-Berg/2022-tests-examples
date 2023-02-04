import pytest
import unittest.mock as mock

from maps.garden.modules.carparks_validator.lib import graph
from maps.garden.modules.carparks_validator.lib.task import (
    ValidationTask, _get_mail_subject, _get_error_report)
from maps.garden.sdk.core import DataValidationWarning, GardenError, Version
from maps.garden.sdk.resources import PythonResource


RUN_VALIDATION = \
    "maps.garden.modules.carparks_validator.lib.task._run_validation"
SEND_REPORT = \
    "maps.garden.modules.carparks_validator.lib.task._send_report"
UPLOAD_REPORT_TO_SANDBOX = \
    "maps.garden.modules.carparks_validator.lib.task.upload_report_to_sandbox"

DATA_VERSION = "20010101"


def run_validation_task(environment_settings):
    dataset_marker = PythonResource(graph.DATASET_MARKER_RESOURCE_NAME)
    dataset_marker.version = Version(properties={
        "data_version": DATA_VERSION})
    dataset_marker.load_environment_settings(environment_settings)

    validation_marker = PythonResource(graph.VALIDATION_MARKER_RESOURCE_NAME)
    validation_marker.version = Version(properties={
        "data_version": DATA_VERSION})
    validation_marker.load_environment_settings(environment_settings)

    task = ValidationTask()
    task.load_environment_settings(environment_settings)
    task(dataset_marker, validation_marker)


@mock.patch(RUN_VALIDATION)
@mock.patch(SEND_REPORT)
def test_validation_success(
        send_report_mock,
        run_validation_mock,
        environment_settings):

    run_validation_mock.return_value = (True, "report")

    run_validation_task(environment_settings)

    run_validation_mock.assert_called_once_with(
        "[]\n",
        "http://__no_such_carparks_testing_renderer__.yandex.ru",
        "http://__no_such_carparks_stable_renderer__.yandex.ru")
    send_report_mock.assert_called_once_with(
        "__no_such_smtp_server__.yandex.net",
        "garden-modules@yandex-team.ru",
        ["__no_such_mail_2__@yandex-team.ru"],
        _get_mail_subject("SUCCEEDED", DATA_VERSION),
        "report")


@mock.patch(RUN_VALIDATION)
@mock.patch(SEND_REPORT)
@mock.patch(UPLOAD_REPORT_TO_SANDBOX)
def test_validation_failure(
        upload_report_to_sandbox_mock,
        send_report_mock,
        run_validation_mock,
        environment_settings):

    run_validation_mock.return_value = (False, "report")
    upload_report_to_sandbox_mock.return_value = "https://sandbox"

    with pytest.raises(DataValidationWarning):
        run_validation_task(environment_settings)

    upload_report_to_sandbox_mock.assert_called_once_with(
        "report", "carparks_validator", environment_settings
    )
    run_validation_mock.assert_called_once_with(
        "[]\n",
        "http://__no_such_carparks_testing_renderer__.yandex.ru",
        "http://__no_such_carparks_stable_renderer__.yandex.ru")
    send_report_mock.assert_called_once_with(
        "__no_such_smtp_server__.yandex.net",
        "garden-modules@yandex-team.ru", [
            "__no_such_mail_1__@yandex-team.ru",
            "__no_such_mail_2__@yandex-team.ru"],
        _get_mail_subject("FAILED", DATA_VERSION),
        "report")


@mock.patch(RUN_VALIDATION)
@mock.patch(SEND_REPORT)
def test_validation_error(
        send_report_mock,
        run_validation_mock,
        environment_settings):

    err = RuntimeError("error")
    run_validation_mock.return_value = (False, "report")
    run_validation_mock.side_effect = err

    with pytest.raises(GardenError):
        run_validation_task(environment_settings)

    run_validation_mock.assert_called_once_with(
        "[]\n",
        "http://__no_such_carparks_testing_renderer__.yandex.ru",
        "http://__no_such_carparks_stable_renderer__.yandex.ru")
    send_report_mock.assert_called_once_with(
        "__no_such_smtp_server__.yandex.net",
        "garden-modules@yandex-team.ru", [
            "__no_such_mail_1__@yandex-team.ru",
            "__no_such_mail_2__@yandex-team.ru"],
        _get_mail_subject("ERROR", DATA_VERSION),
        _get_error_report(err))

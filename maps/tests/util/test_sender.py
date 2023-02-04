import json
import pytest
import retrying
import contextlib
from dataclasses import fields
from ya_courier_backend.util.company_info import CompanyInfo
from ya_courier_backend.util.sender import send_welcome_letter, send_internal_letter
from flask import Flask

import maps.b2bgeo.test_lib.sender_values as sender_values
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


@pytest.fixture(scope='module')
def _flask_mock_app():
    app = Flask(__name__)
    with app.app_context():
        app.config["INTERNAL_EMAIL"] = sender_values.INTERNAL_EMAIL
        app.config["WELCOME_MAILING_ID"] = sender_values.WELCOME_MAILING_ID
        app.config["WELCOME_MAILING_ID_TR"] = sender_values.WELCOME_MAILING_ID_TR
        app.config["INTERNAL_MAILING_ID"] = sender_values.INTERNAL_MAILING_ID
        app.config["SENDER_BASIC_AUTH_USER"] = sender_values.SENDER_BASIC_AUTH_USER
        yield app


@contextlib.contextmanager
def _flask_request_context(app):
    with app.test_request_context('/create-company', base_url="http://courier.yandex.ru"):
        yield


@contextlib.contextmanager
def _flask_request_context_turkey(app):
    with app.test_request_context('/create-company', base_url="http://courier.yandex.com.tr"):
        yield


@contextlib.contextmanager
def _flask_request_context_com(app):
    with app.test_request_context('/create-company', base_url="http://courier.yandex.com"):
        yield


@skip_if_remote
def test_welcome_letter_turkey(system_env_with_db, _flask_mock_app):
    email = "testcouriernew@yandex.ru"
    with _flask_request_context_turkey(_flask_mock_app):
        resp = send_welcome_letter(system_env_with_db.mock_sender_url, email)

    assert resp.status_code == 200


@skip_if_remote
def test_welcome_letter_com(system_env_with_db, _flask_mock_app):
    email = "testcouriernew@yandex.ru"
    with _flask_request_context_com(_flask_mock_app):
        resp = send_welcome_letter(system_env_with_db.mock_sender_url, email)

    assert resp is None


@skip_if_remote
def test_welcome_letter(system_env_with_db, _flask_mock_app):
    email = "testcouriernew@yandex.ru"
    with _flask_request_context(_flask_mock_app):
        resp = send_welcome_letter(system_env_with_db.mock_sender_url, email)

    assert resp.status_code == 200
    resp_json = resp.json()
    expected_json = {
        "params": {
            "control": {
                "async": True,
                "countdown": None,
                "expires": 86400,
                "for_testing": False
            },
            "source": {
                "to_email": email,
                "header": [],
                "ignore_empty_email": False
            }
        },
        "result": {
            "status": "OK",
            "message_id": resp_json["result"]["message_id"],
            "task_id": resp_json["result"]["task_id"]
        }
    }

    assert expected_json == resp_json


@skip_if_remote
def test_welcome_letter_without_email(system_env_with_db, _flask_mock_app):
    with _flask_request_context(_flask_mock_app):
        resp = send_welcome_letter(system_env_with_db.mock_sender_url, "")

    assert resp.status_code == 400
    expected_json = {
        "result": {
            "status": "ERROR",
            "error": {
                "non_field_errors": [
                    "to_email or valid to_yandex_puid argument required: no to_email"
                ]
            }
        }
    }
    assert resp.json() == expected_json


@skip_if_remote
def test_welcome_letter_simulated_error(system_env_with_db, _flask_mock_app):
    with _flask_request_context(_flask_mock_app):
        with pytest.raises(retrying.RetryError):
            send_welcome_letter(system_env_with_db.mock_sender_url, sender_values.EMAIL_SIMULATE_INTERNAL_ERROR)


@skip_if_remote
def test_internal_letter(system_env_with_db, _flask_mock_app):
    company_info = {
        **{field.name: "test" for field in fields(CompanyInfo)},
        **{
            "company_id": 6,
            "name": "Flash Logistics"
        }
    }

    with _flask_request_context(_flask_mock_app):
        resp = send_internal_letter(system_env_with_db.mock_sender_url, CompanyInfo(**company_info))
    assert resp.status_code == 200
    resp_json = resp.json()
    expected_json = {
        "params": {
            "control": {
                "async": True,
                "countdown": None,
                "expires": 86400,
                "for_testing": False
            },
            "source": {
                "to_email": sender_values.INTERNAL_EMAIL,
                "args": json.dumps(company_info),
                "header": [],
                "ignore_empty_email": False
            }
        },
        "result": {
            "status": "OK",
            "message_id": resp_json["result"]["message_id"],
            "task_id": resp_json["result"]["task_id"]
        }
    }
    assert resp_json == expected_json


def test_internal_letter_invalid_args(system_env_with_db, _flask_mock_app):
    letter_arguments = [1, 2, 3]
    with _flask_request_context(_flask_mock_app):
        with pytest.raises(AssertionError):
            send_internal_letter(system_env_with_db.mock_sender_url, letter_arguments)

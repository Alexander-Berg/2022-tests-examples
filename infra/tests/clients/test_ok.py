"""Tests for OK client."""

from unittest import mock

import pytest
import requests

from sepelib.core import config
from walle.clients import ok

MOCK_STATUS = "MOCK-status"
MOCK_TICKET_KEY = "MOCK-1234"
MOCK_OK_TOKEN = "mock"
MOCK_OK_ID = 1234
MOCK_OK_UID = "uid"
MOCK_OK_TEXT = "Mock."
MOCK_OK_STAGES = [
    {
        "approver": "mock1",
        "is_approved": None,
        "approved_by": "",
    },
    {
        "approver": "mock2",
        "is_approved": True,
        "approved_by": "mock2",
    },
    {
        "approver": "mock3",
        "is_approved": True,
        "approved_by": "mock1",
    },
]


@pytest.fixture
def ok_config(monkeypatch):
    monkeypatch.setitem(config.get_value("ok"), "api", "testing")
    monkeypatch.setitem(config.get_value("ok"), "access_token", MOCK_OK_TOKEN)


def get_clean_ok_stages(stages):
    return [{"approver": stage["approver"], "approved_by": stage["approved_by"]} for stage in stages]


def mock_approvement_response():
    response_dict = {
        "id": MOCK_OK_ID,
        "stages": MOCK_OK_STAGES,
        "author": "mocker",
        "text": MOCK_OK_TEXT,
        "status": "in_progress",
        "resolution": "",
        "is_parallel": None,
        "url": "https://st.yandex-team.ru/MOCK-1234",
        "actions": {"approve": None, "suspend": None, "resume": False, "close": None},
        "created": "2019-09-19T16:01:06.507162Z",
        "modified": "2019-09-19T16:01:06.507162Z",
        "stats": {"duration": {"total": 0, "active": 0, "suspended": 0}},
        "tracker_queue": {"name": "MOCK", "has_triggers": False},
        "user_roles": {"responsible": None, "approver": None},
    }
    return mock.Mock(ok=True, status_code=200, json=mock.Mock(return_value=response_dict))


def get_client_with_mocked_session():
    client = ok.get_client()
    client._session = mock.Mock()
    client._session.request.return_value = mock_approvement_response()
    return client


def test_get_client(ok_config):
    client_expected = ok.OkClient(ok.OkClient.TESTING_BASE_URL, MOCK_OK_TOKEN)
    client_actual = ok.get_client()

    assert client_actual._base_url == client_expected._base_url
    assert client_actual._token == client_expected._token


def test_connection_error(ok_config):
    client = get_client_with_mocked_session()
    client._session.request.side_effect = requests.RequestException("connection failed", request=mock.Mock())

    with pytest.raises(ok.OKConnectionError) as e:
        client.get_approvement(MOCK_OK_ID)
        assert str(e) == "Error while connecting to OK server: connection failed."


def test_bad_request(ok_config):
    client = get_client_with_mocked_session()
    client._session.request.return_value.ok = False
    client._session.request.return_value.status_code = 400
    client._session.request.return_value.text = "Mock error text"
    client._session.request.return_value.raise_for_status.side_effect = requests.HTTPError(
        response=mock.Mock(status_code=400)
    )

    with pytest.raises(ok.OKConnectionError) as e:
        client.get_approvement(MOCK_OK_ID)
        assert str(e) == "Error while communicating with OK: {}.".format(MOCK_OK_ID)


def test_not_found(ok_config):
    client = get_client_with_mocked_session()
    client._session.request.return_value.ok = False
    client._session.request.return_value.status_code = 404
    client._session.request.return_value.raise_for_status.side_effect = requests.HTTPError(
        response=mock.Mock(status_code=404)
    )

    with pytest.raises(ok.OKApprovementNotFound) as e:
        client.get_approvement(MOCK_OK_ID)
        assert str(e) == "Approvement with id {} does not exist.".format(MOCK_OK_ID)


def test_create_approvement(ok_config):
    client = get_client_with_mocked_session()
    expected_approvement = ok.Approvement(
        approvement_id=MOCK_OK_ID,
        text=MOCK_OK_TEXT,
        stages=get_clean_ok_stages(MOCK_OK_STAGES),
        ticket_key=MOCK_TICKET_KEY,
        uid=MOCK_OK_UID,
        is_approved=False,
    )
    actual_approvement = client.create_approvement(
        approvers=[s["approver"] for s in MOCK_OK_STAGES], ticket_key=MOCK_TICKET_KEY, text=MOCK_OK_TEXT
    )

    assert client._session.request.called_once_with("POST", "/approvements/")

    for key in ("id", "text", "stages"):
        assert getattr(actual_approvement, key) == getattr(expected_approvement, key)


def test_get_approvement(ok_config):
    client = get_client_with_mocked_session()
    expected_approvement = ok.Approvement(
        approvement_id=MOCK_OK_ID, text=MOCK_OK_TEXT, stages=get_clean_ok_stages(MOCK_OK_STAGES)
    )
    actual_approvement = client.get_approvement(MOCK_OK_ID)

    assert client._session.request.called_once_with("GET", "/approvements/{}/".format(MOCK_OK_ID))

    for key in ("id", "text", "stages"):
        assert getattr(actual_approvement, key) == getattr(expected_approvement, key)


def test_approvement_from_dict():
    data = {
        "id": MOCK_OK_ID,
        "text": MOCK_OK_TEXT,
        "resolution": "approved",
        "stages": [
            {"approver": "1", "garbage": "1111", "approved_by": "2"},
            {"approver": "2", "garbage": "1111", "approved_by": ""},
        ],
        "uid": MOCK_OK_UID,
        "object_id": MOCK_TICKET_KEY,
        "status": MOCK_STATUS,
    }
    expected = ok.Approvement(
        approvement_id=MOCK_OK_ID,
        text=MOCK_OK_TEXT,
        stages=get_clean_ok_stages(data["stages"]),
        is_approved=True,
        uid=MOCK_OK_UID,
        ticket_key=MOCK_TICKET_KEY,
        status=MOCK_STATUS,
        resolution="approved",
    )
    actual = ok.Approvement.from_dict(data)

    assert expected.__dict__ == actual.__dict__


def test_generate_uid():
    assert ok.OkClient._generate_new_uid() != ok.OkClient._generate_new_uid()


def test_iframe():
    uuid = 1
    approvement = ok.Approvement(
        approvement_id=MOCK_OK_ID,
        text=MOCK_OK_TEXT,
        stages=get_clean_ok_stages(MOCK_OK_STAGES),
        is_approved=True,
        uid=MOCK_OK_UID,
        ticket_key=MOCK_TICKET_KEY,
        uuid=uuid,
    )
    assert (
        approvement.iframe == "{{iframe src=\"https://ok.yandex-team.ru/approvements/1?_embedded=1\""
        " frameborder=0 width=100% height=400px scrolling=no}}"
    )

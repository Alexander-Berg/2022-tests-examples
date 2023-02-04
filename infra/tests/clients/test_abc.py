import pytest

from infra.walle.server.tests.lib.util import monkeypatch_request, mock_response
from walle.clients import abc

OAUTH_TOKEN_MOCK = "AQAD-much-serious-token"
ABC_HOST = "abc-back.y-t.ru"
DEFAULT_FIELDS = "id,path,slug,state"

SERVICE_ID = 37
SERVICE_SLUG = "specprojects"


@pytest.fixture(autouse=True)
def mock_abc_config(mp):
    mp.config("abc.host", ABC_HOST)
    mp.config("abc.access_token", OAUTH_TOKEN_MOCK)


def test_oauth_token_is_passed(monkeypatch):
    resp = mock_response({"results": []})
    mock = monkeypatch_request(monkeypatch, resp)

    abc.get_services()

    passed_headers = mock.call_args[1]["headers"]
    assert passed_headers.get("Authorization") == "OAuth {}".format(OAUTH_TOKEN_MOCK)


def test_non200_responses_raise(monkeypatch):
    status_code, reason = 403, "Because sword lily"
    resp = mock_response(
        {"error": {"message": "", "code": "permission_denied"}}, status_code=status_code, reason=reason
    )
    monkeypatch_request(monkeypatch, resp)

    with pytest.raises(abc.ABCInternalError) as exc:
        abc.get_services()
    assert str(exc.value) == "Error in communication with ABC: {} Client Error: {} for url: None".format(
        status_code, reason
    )


def test_exc_reraised(monkeypatch):
    monkeypatch_request(monkeypatch, side_effect=Exception("Some exception"))
    with pytest.raises(Exception) as exc:
        abc.get_services()
    assert str(exc.value) == "Some exception"


def assert_url_requested(json_request_mock, path, parameters):
    assert json_request_mock.call_args[0] == ("GET", "https://{}/api/v4/{}".format(ABC_HOST, path))
    assert json_request_mock.call_args[1]["params"] == parameters


@pytest.mark.parametrize("filters_dict", [{}, {"id": 13}, {"slug": "walle", "state": "develop"}])
def test_get_services(monkeypatch, filters_dict):
    mock = monkeypatch_request(monkeypatch, mock_response({"results": []}))

    abc.get_services(**filters_dict)
    assert_url_requested(mock, "services/", dict(filters_dict, fields=DEFAULT_FIELDS, page_size=abc.PAGE_SIZE))


ABC_SERVICE_RESP = {
    "count": 1,
    "next": None,
    "previous": None,
    "total_pages": 1,
    "results": [
        {
            "readonly_state": None,
            "id": SERVICE_ID,
            "path": "/meta_search/websearch/buki/specprojects/",
            "slug": SERVICE_SLUG,
            "state": "develop",
        }
    ],
}


def test_get_service_by_id(monkeypatch):
    mock = monkeypatch_request(monkeypatch, mock_response(ABC_SERVICE_RESP))

    service = abc.get_service_by_id(SERVICE_ID)
    assert_url_requested(mock, "services/", {"id": SERVICE_ID, "fields": DEFAULT_FIELDS, "page_size": abc.PAGE_SIZE})
    assert service == ABC_SERVICE_RESP["results"][0]


def test_get_service_by_slug(monkeypatch):
    mock = monkeypatch_request(monkeypatch, mock_response(ABC_SERVICE_RESP))

    service = abc.get_service_by_slug(SERVICE_SLUG)
    assert_url_requested(
        mock, "services/", {"slug": SERVICE_SLUG, "fields": DEFAULT_FIELDS, "page_size": abc.PAGE_SIZE}
    )
    assert service == ABC_SERVICE_RESP["results"][0]


def test_get_service_slug(monkeypatch):
    mock = monkeypatch_request(monkeypatch, mock_response(ABC_SERVICE_RESP))

    slug = abc.get_service_slug(SERVICE_ID)
    assert_url_requested(mock, "services/", {"id": SERVICE_ID, "fields": DEFAULT_FIELDS, "page_size": abc.PAGE_SIZE})
    assert slug == SERVICE_SLUG


def test_get_service_on_duty_logins(monkeypatch):
    on_duty_response_mock = [
        {
            "person": {"login": "foo"},
            "start_datetime": "2021-03-15T00:00:00+03:00",
            "end_datetime": "2021-03-22T00:00:00+03:00",
        },
        {
            "person": {"login": "bar"},
            "start_datetime": "2021-03-15T00:55:00+03:00",
            "end_datetime": "2021-03-22T00:55:00+03:00",
        },
    ]

    mock = monkeypatch_request(monkeypatch, mock_response(on_duty_response_mock))

    test_service_slug = "test_slug"
    on_duty_logins = abc.get_service_on_duty_logins(test_service_slug)
    assert_url_requested(
        mock,
        "duty/on_duty/",
        {
            "service__slug": test_service_slug,
            "fields": "person.login",
        },
    )
    assert on_duty_logins == ["foo", "bar"]

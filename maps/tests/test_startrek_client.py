import http
import datetime as dt

import pytest

from maps.garden.libs_server.infrastructure_clients.startrek import Client, OnClosedAction, Issue, StartrekException


BASE_URL = "http://base_url/v2"
QUEUE = "MAPSGARDEN"
TOKEN = "<wrong token>"
NOW = dt.datetime(2020, 12, 25, 17, 45, 00)


@pytest.mark.freeze_time(NOW)
def test_upsert_issue_with_components(requests_mock):
    module_name = "test_module_name"
    components = [
        {
            "id": 1,
            "name": "modules",
            "queue": QUEUE,
        },
        {
            "id": 2,
            "name": f"modules-{module_name}",
            "queue": QUEUE,
        },
    ]
    requests_mock.get(
        f"{BASE_URL}/queues/{QUEUE}/components/",
        json=components,
    )

    client = Client(base_url=BASE_URL, token=TOKEN)

    requests_mock.get(f"{BASE_URL}/issues", json=[])
    req = requests_mock.post(
        f"{BASE_URL}/issues",
        status_code=http.client.CREATED,
        json={"id": 1, "key": f"{QUEUE}-1"},
    )

    issue = Issue(
        queue=QUEUE,
        summary="summary",
        description="description",
        components=[1, 2],
    )

    client.upsert_issue(issue)
    return req.last_request.json()


@pytest.mark.freeze_time(NOW)
def test_create_issue(requests_mock):
    issue = Issue(
        queue=QUEUE,
        summary="summary",
        description="description",
        components=[1, 2],
    )
    requests_mock.post(
        f"{BASE_URL}/issues",
        json={"key": "ISSUE-123"},
    )

    client = Client(base_url=BASE_URL, token=TOKEN)

    return client.create_issue(issue)


@pytest.mark.freeze_time(NOW)
def test_create_issue_conflict(requests_mock):
    issue = Issue(
        queue=QUEUE,
        summary="summary",
        description="description",
        components=[1, 2],
    )
    requests_mock.post(
        f"{BASE_URL}/issues",
        text="resp2",
        status_code=409,
        headers={"X-Ticket-Key": "ISSUE-123"}
    )

    client = Client(base_url=BASE_URL, token=TOKEN)

    return client.create_issue(issue)


@pytest.mark.parametrize(
    "on_closed",
    [
        OnClosedAction.CREATE_NEW,
        OnClosedAction.REOPEN,
    ]
)
def test_upsert_issue_on_closed(requests_mock, on_closed):
    client = Client(base_url=BASE_URL, token=TOKEN)

    st_key = f"{QUEUE}-1"
    get_issues_req = requests_mock.get(f"{BASE_URL}/issues", json=[
        {
            "key": st_key,
            "queue": QUEUE,
            "summary": "summary",
            "status": {"key": "closed"},
        },
    ])

    reopen_req = requests_mock.post(
        f"{BASE_URL}/issues/{st_key}/transitions/reopen/_execute",
    )
    comments_req = requests_mock.post(
        f"{BASE_URL}/issues/{QUEUE}-1/comments",
    )
    requests_mock.patch(
        f"{BASE_URL}/issues/{QUEUE}-1",
    )

    issue = Issue(
        queue=QUEUE,
        summary="summary",
        components=[],
        description="Must be in the comment",
    )
    client.upsert_issue(issue, on_closed)
    assert get_issues_req.called
    assert reopen_req.called == (on_closed == OnClosedAction.REOPEN)
    assert comments_req.called
    return {
        "query": get_issues_req.last_request.qs.get("query"),
        "comment": comments_req.last_request.json(),
    }


@pytest.mark.parametrize(
    "on_closed",
    [
        OnClosedAction.CREATE_NEW,
        OnClosedAction.REOPEN,
    ]
)
def test_update_on_closed(requests_mock, on_closed):
    client = Client(base_url=BASE_URL, token=TOKEN)

    st_key = f"{QUEUE}-1"
    get_issue_req = requests_mock.get(f"{BASE_URL}/issues/{QUEUE}-1", json={
        "key": st_key,
        "queue": QUEUE,
        "summary": "summary",
        "status": {"key": "closed"},
    })

    reopen_req = requests_mock.post(
        f"{BASE_URL}/issues/{st_key}/transitions/reopen/_execute",
    )
    comments_req = requests_mock.post(
        f"{BASE_URL}/issues/{QUEUE}-1/comments",
    )
    requests_mock.patch(
        f"{BASE_URL}/issues/{QUEUE}-1",
    )

    issue = Issue(
        queue=QUEUE,
        summary="summary",
        components=[],
        description="Must be in the comment",
    )
    client.update_issue(st_key, issue, on_closed)
    assert get_issue_req.called
    assert reopen_req.called == (on_closed == OnClosedAction.REOPEN)
    assert comments_req.called
    return {
        "comment": comments_req.last_request.json(),
    }


def test_upsert_components_failed(requests_mock):
    """
    The notifier must raise an exception in case of access failure
    """
    requests_mock.get(
        f"{BASE_URL}/queues/{QUEUE}/components/",
        json={"errors": {}},
        status_code=http.HTTPStatus.FORBIDDEN,
    )

    client = Client(base_url=BASE_URL, token=TOKEN)
    with pytest.raises(StartrekException):
        client.upsert_component(QUEUE, "component_name")

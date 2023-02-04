import http.client

import pytest

from walle import constants
from walle.hosts import HostMessage
from infra.walle.server.tests.lib.util import TestCase, mock_startrek_client


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.fixture
def st(mp):
    return mock_startrek_client(mp)


def test_create_ticket_for_hosts(test, st):
    host1 = test.mock_host({"inv": 1, "messages": {"dmc": [HostMessage.error("some1"), HostMessage.error("some2")]}})
    host2 = test.mock_host({"inv": 2, "messages": {"dmc": [HostMessage.error("some")]}})
    host3 = test.mock_host({"inv": 3})
    summary = "some problem"
    description = "some description"
    queue = "QUEUE"
    response = test.api_client.post(
        "/v1/create-ticket",
        data={
            "hosts": [host1.inv, host2.inv, host3.inv],
            "queue": queue,
            "summary": summary,
            "description": description,
        },
    )
    assert response.status_code == http.client.OK
    st_call_kwargs = st.create_issue.call_args.args[0]
    assert st_call_kwargs["queue"] == queue
    assert summary in st_call_kwargs["summary"]
    assert description in st_call_kwargs["description"]
    for host in [host1, host2, host3]:
        assert host.name in st_call_kwargs["description"]
    assert st_call_kwargs["tags"] == [constants.CREATED_FROM_WALLE_TRACKER_TAG]
    assert st_call_kwargs["followers"][0]["login"] == constants.ROBOT_WALLE_OWNER
    assert st_call_kwargs["createdBy"] == TestCase.api_user


def test_create_ticket_exp(test, st):
    response = test.api_client.post(
        "/v1/create-ticket",
        data={
            "hosts": [test.mock_host({"inv": 1, "messages": {"dmc": [HostMessage.error("some1")]}}).inv],
            "queue": constants.TicketTrackerQueue.EXP,
            "summary": "some problem",
        },
    )
    assert response.status_code == http.client.OK
    st_call_kwargs = st.create_issue.call_args.args[0]
    assert st_call_kwargs["queue"] == constants.TicketTrackerQueue.EXP
    assert st_call_kwargs["type"] == constants.INCIDENT_TICKET_TYPE
    assert st_call_kwargs["components"] == [constants.HARDWARE_AND_FIRMWARE_EXP_COMPONENT]
    assert st_call_kwargs["fixVersions"] == [constants.OTHER_EXP_VERSION]


def test_no_hosts(test):
    response = test.api_client.post(
        "/v1/create-ticket",
        data={
            "hosts": [],
            "queue": constants.TicketTrackerQueue.EXP,
            "summary": "foo",
        },
    )
    assert response.status_code == http.client.BAD_REQUEST


def test_unknown_host(test):
    response = test.api_client.post(
        "/v1/create-ticket",
        data={
            "hosts": ["fqdn.y-t.net"],
            "queue": constants.TicketTrackerQueue.EXP,
            "summary": "foo",
        },
    )
    assert response.status_code == http.client.BAD_REQUEST

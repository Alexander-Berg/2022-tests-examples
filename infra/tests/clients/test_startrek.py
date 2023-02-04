from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import TestCase
from sepelib.yandex.startrek import Relationship
from walle.clients import startrek
from walle.clients.startrek import StartrekClientError


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_link_tickets(test, mp):
    ticket_id = "TEST-1"
    other_ticket_ids = ["TEST-2", "TEST-3"]
    client_mock = Mock()
    client_mock.get_relationships.return_value = [{"object": {"key": other_ticket_ids[1]}}]
    mp.function(startrek.get_client, return_value=client_mock)

    startrek.link_tickets(ticket_id, other_ticket_ids)
    client_mock.add_relationship.assert_called_once_with(
        ticket_id, relationship=Relationship.RELATES, other_issue_id=other_ticket_ids[0]
    )


@pytest.mark.parametrize("silent", [True, False])
def test_link_tickets_fail_silence(test, mp, silent):
    ticket_id = "TEST-1"
    other_ticket_ids = ["TEST-2", "TEST-3"]
    client_mock = Mock()
    client_mock.get_relationships.return_value = [{"object": {"key": other_ticket_ids[1]}}]
    client_mock.add_relationship.side_effect = StartrekClientError("Something went wrong")
    mp.function(startrek.get_client, return_value=client_mock)

    if silent:
        startrek.link_tickets(ticket_id, other_ticket_ids, silent=True)
    else:
        with pytest.raises(StartrekClientError):
            startrek.link_tickets(ticket_id, other_ticket_ids, silent=False)

    client_mock.add_relationship.assert_called_once_with(
        ticket_id, relationship=Relationship.RELATES, other_issue_id=other_ticket_ids[0]
    )

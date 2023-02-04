import pytest
from mock import patch

from intranet.hrdb_ext.src.amo.core.create import process_ticket_request
from intranet.hrdb_ext.src.amo.exceptions import BadRequestException
from intranet.hrdb_ext.tests.amo.factories import FakeIssue


@pytest.mark.django_db
@patch('intranet.hrdb_ext.src.amo.core.create.client.issues.create')
def test_create_success(fake_issue_create):
    fake_issue_create.return_value = FakeIssue('KEY-123', 'open')
    body = {'field': 'value'}

    request = process_ticket_request(body)
    assert request.uuid
    assert request.created_at
    assert request.body == body
    assert not request.error
    assert request.ticket.key == 'KEY-123'
    assert request.ticket.status == 'open'
    assert request.ticket.created_at
    assert request.ticket.status_changed_at
    assert request.ticket.synced_at


@pytest.mark.django_db
@patch('intranet.hrdb_ext.src.amo.core.create.client.issues.create')
def test_create_error(fake_issue_create):
    fake_issue_create.return_value = FakeIssue('KEY-123', 'open')
    body = {'queue': 'UNKNOWNQUEUE'}

    with pytest.raises(BadRequestException):
        process_ticket_request(body)

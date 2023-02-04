import pytest
from django.test import Client
from django.urls import reverse
from mock import patch
from datetime import datetime

from intranet.hrdb_ext.src.amo.models import TicketRequest
from intranet.hrdb_ext.tests.amo.factories import FakeIssue, TicketRequestFactory


def check_ticket_request_serialization_format(data):
    assert data['uuid']
    assert data['external_id']
    assert data['created_at']
    assert data['ticket']

    assert data['ticket']['key']
    assert data['ticket']['status']
    assert data['ticket']['created_at']
    assert data['ticket']['updated_at']
    assert data['ticket']['synced_at']


@pytest.mark.django_db
@patch('intranet.hrdb_ext.src.amo.core.create.client.issues.create')
def test_create_issue(fake_issue_create):
    c = Client()
    assert TicketRequest.objects.count() == 0

    fake_issue_create.return_value = FakeIssue('KEY-123', 'open')

    data = {
        'external_id': '123',
        'body': {
            'field': 'value',
        }
    }

    response = c.post(
        reverse('create-issue'),
        data=data,
        content_type='application/json',
    )
    assert response.status_code == 201
    assert TicketRequest.objects.count() == 1

    data = response.json()
    check_ticket_request_serialization_format(data)
    assert data['ticket']['key'] == 'KEY-123'
    assert data['ticket']['status'] == 'open'

    request = TicketRequest.objects.get(uuid=data['uuid'])
    assert str(request.uuid) == data['uuid']
    assert request.external_id == data['external_id']
    assert request.ticket.key == data['ticket']['key'] == 'KEY-123'
    assert request.ticket.status == data['ticket']['status'] == 'open'


@pytest.mark.django_db
def test_get_request():
    c = Client()
    assert TicketRequest.objects.count() == 0

    requests = [
        TicketRequestFactory(created_at=datetime(2022, 1, 1)),
        TicketRequestFactory(created_at=datetime(2022, 1, 2)),
        TicketRequestFactory(created_at=datetime(2022, 1, 3)),
    ]

    test_request = requests[0]
    response = c.get(reverse('get-status', args=(test_request.uuid, )))
    assert response.status_code == 200

    data = response.json()
    check_ticket_request_serialization_format(data)
    assert data['uuid'] == test_request.uuid
    assert data['ticket']['key'] == test_request.ticket.key

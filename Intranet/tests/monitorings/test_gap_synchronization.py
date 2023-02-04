# coding: utf-8


import pytest
from mock import patch

from idm.core.models import Action
from idm.tests.commands.test_idm_sync_gaps import get_gaps
from idm.users.sync.gaps import sync_gaps_for_all_users

from requests.exceptions import ReadTimeout

from django.db.models import Q

pytestmark = [pytest.mark.django_db]


@patch('idm.users.sync.gaps.get_gaps')
def test_gap_synchronization_with_no_errors(mocked_gaps, arda_users, client):

    mocked_gaps.return_value = get_gaps()

    for _ in range(10):
        sync_gaps_for_all_users()
        response = client.get('/monitorings/gap-synchronization-errors/')
        assert response.status_code == 200


@patch('idm.users.sync.gaps.get_gaps')
def test_gap_synchronization_with_errors(mocked_gap, arda_users, client):
    exception_message = "The server did not send any data in the allotted amount of time."
    mocked_gap.side_effect = ReadTimeout(exception_message)

    for _ in range(5):
        sync_gaps_for_all_users()

    failed_gap_sync_actions = Q(action='gap_synchronization_completed') & ~Q(error__exact='')

    assert Action.objects.count() == 10 and Action.objects.filter(failed_gap_sync_actions).count() == 5
    assert Action.objects.filter(error__contains=exception_message).count() == 5

    response = client.get('/monitorings/gap-synchronization-errors/')

    assert response.status_code == 400
    assert b'The server did not send any data in the allotted amount of time.' in response.content

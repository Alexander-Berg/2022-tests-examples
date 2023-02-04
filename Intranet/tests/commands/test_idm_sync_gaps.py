# coding: utf-8
import json

import pytest
from mock import patch

from idm.core.models import Action
from idm.users.models import User
from idm.users.sync.gaps import sync_gaps_for_all_users

pytestmark = pytest.mark.django_db

from requests.exceptions import ReadTimeout


def get_gaps():
    json_data = """
        {   
            "meta": {
                "now": "2016-05-19T11:58:13",
                "date_from": "2016-05-05T10:20:30",
                "date_to": "2016-05-06T00:00:00",
                "working_hour_from": null,
                "working_hour_to": null,
                "use_calendar": false
            },
            "persons": {
                "frodo": {
                      "available_now": false,
                      "total_seconds": 86400,
                      "available_seconds": 60000,
                      "available_to_total": 82,
                      "tz": "Europe/Moscow",
                      "availability": [
                        {
                          "date_from": "2016-05-05T10:20:30",
                          "date_to": "2016-05-06T00:00:00"
                        }
                      ]      
                 },
                 "sam": {
                      "available_now": false,
                      "total_seconds": 86400,
                      "available_seconds": 50000,
                      "available_to_total": 82,
                      "tz": "Europe/Moscow",
                      "availability": [
                        {
                          "date_from": "2016-05-05T10:20:30",
                          "date_to": "2016-05-06T00:00:00"
                        }
                      ]      
                 },
                 "bilbo": {
                      "available_now": false,
                      "total_seconds": 86400,
                      "available_seconds": 0,
                      "available_to_total": 0,
                      "tz": "Europe/Moscow",
                      "availability": [
                        {
                          "date_from": "2016-05-05T10:20:30",
                          "date_to": "2016-05-06T00:00:00"
                        }
                      ]      
                 },
                 "manve": {
                      "available_now": false,
                      "total_seconds": 86400,
                      "available_seconds": 70000,
                      "available_to_total": 82,
                      "tz": "Europe/Moscow",
                      "availability": [
                        {
                          "date_from": "2016-05-05T10:20:30",
                          "date_to": "2016-05-06T00:00:00"
                        }
                      ]      
                 }
            }
        }
    """
    return json.loads(json_data)


@patch('idm.users.sync.gaps.get_gaps')
def test_idm_sync_gap(mocked_gaps, arda_users):
    """
        Проверяет что после синхронизации отсутствия выставлены правильно
    """
    mocked_gaps.return_value = get_gaps()

    sync_gaps_for_all_users()

    assert (set(User.objects.active().filter(is_absent=True).values_list('username', flat=True)) ==
            {'bilbo', 'sam'})

    # все остальные присутствуют
    assert (set(User.objects.active().filter(is_absent=False).values_list('username', flat=True)) ==
            set(User.objects.active().
                exclude(username__in=['bilbo', 'sam']).values_list('username', flat=True)))

    # gap synchronization started + gap synchronization completed
    assert Action.objects.count() == 2
    assert set([action.action for action in Action.objects.all()]) == {'gap_synchronization_started',
                                                                       'gap_synchronization_completed'}


@patch('idm.users.sync.gaps.get_gaps')
def test_idm_sync_gap_exception(mocked_gaps, arda_users):
    mocked_gaps.side_effect = ReadTimeout("The server did not send any data in the allotted amount of time.")

    sync_gaps_for_all_users()

    assert Action.objects.count() == 2
    assert set([action.action for action in Action.objects.all()]) == {'gap_synchronization_started',
                                                                       'gap_synchronization_completed'}

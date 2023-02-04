import json
from datetime import datetime
from pytz import timezone

import pytest
from mock import Mock, patch

from django.test.client import RequestFactory
from staff.gap.api.availability import Availability
from staff.gap.workflows.utils import find_workflow
from staff.gap.api.views.availability_views import (
    _gaps_params_get,
    _gaps_params_post,
    get_availabilities
)


data = [
    {
        'from': {
            'l': ['test1', 'test2'],
            'person_logins': ['test1', 'test2'],
            'date_from': '2019-02-22T16:52:24',
            'date_to': '2019-02-24T16:52:24',
            'now': '2019-02-22T17:52:24',
        },
        'to': {
            'person_logins': ['test1', 'test2'],
            'date_from': datetime(2019, 2, 22, 16, 52, 24),
            'date_to': datetime(2019, 2, 24, 16, 52, 24),
            'now': datetime(2019, 2, 22, 17, 52, 24),
        }
    },
    {
        'from': {
            'l': ['test1', 'test2'],
            'person_logins': ['test1', 'test2'],
            'date_from': '2019-02-22T16:52:24Z',
            'date_to': '2019-02-24T16:52:24-06:00',
            'now': '2019-02-22T18:52:24+01:00',
        },
        'to': {
            'person_logins': ['test1', 'test2'],
            'date_from': datetime(2019, 2, 22, 16, 52, 24),
            'date_to': datetime(2019, 2, 24, 22, 52, 24),
            'now': datetime(2019, 2, 22, 17, 52, 24),
        }
    },
]


def get_params(data):
    for param in data:
        yield RequestFactory().get('/', data=param['from']), param['to']
        yield RequestFactory().post('/', data=json.dumps(param['from']), content_type='application/json'), param['to']


@pytest.mark.parametrize('req, data', get_params(data))
def test_params(req, data):
    if req.GET.keys():
        res = _gaps_params_get(req.GET)
    else:
        res = _gaps_params_post(req.body)

    for key, value in data.items():
        assert value == res[key]


def expand_availabilities(availabilities):
    return {
        person_login: [{'date_from': link.date_from, 'date_to': link.date_to} for link in availability]
        for person_login, availability in availabilities.items()
    }


@pytest.mark.django_db
def test_availability(gap_test):
    gap_test.test_person.tz = 'Europe/Moscow'
    gap_test.test_person.work_email = '{}@yandex-team.ru'.format(gap_test.test_person.login)
    gap_test.test_person.save()

    person_logins = [gap_test.test_person.login, 'bad_login']
    date_from = datetime(2019, 3, 27, 7, 00)
    date_to = datetime(2019, 3, 27, 23, 00)
    include_holidays = False
    include_calendar = False
    working_hour_from = 9
    working_hour_to = 18
    ignore_work_in_absence = True

    result = get_availabilities(
        person_logins, date_from, date_to,
        include_holidays, include_calendar,
        working_hour_from, working_hour_to,
        ignore_work_in_absence,
    )

    assert expand_availabilities(result) == {
        gap_test.test_person.login: [
            {'date_from': datetime(2019, 3, 27, 10, 0), 'date_to': datetime(2019, 3, 27, 18, 0)},
        ]
    }

    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    base_gap['date_from'] = datetime(2019, 3, 27, 12, 0)
    base_gap['date_to'] = datetime(2019, 3, 27, 13, 0)

    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    result = get_availabilities(
        person_logins, date_from, date_to,
        include_holidays, include_calendar,
        working_hour_from, working_hour_to,
        ignore_work_in_absence,
    )

    assert expand_availabilities(result) == {
        gap_test.test_person.login: [
            {'date_from': datetime(2019, 3, 27, 10, 0), 'date_to': datetime(2019, 3, 27, 12, 0)},
            {'date_from': datetime(2019, 3, 27, 13, 0), 'date_to': datetime(2019, 3, 27, 18, 0)},
        ]
    }

    calendar_mock = Mock(return_value=Mock(**{
        'status_code': 200,
        'json.return_value': {'subjectAvailabilities': [{
            'email': gap_test.test_person.work_email,
            'intervals': [{
                'start': '2019-03-27T11:30:00',
                'end': '2019-03-27T12:30:00',
                'availability': 'busy'}],
            'status': 'ok'}]}
    }))

    include_calendar = True

    with patch('staff.gap.api.views.availability_views._ask_calendar', calendar_mock):
        result = get_availabilities(
            person_logins, date_from, date_to,
            include_holidays, include_calendar,
            working_hour_from, working_hour_to,
            ignore_work_in_absence,
        )

    assert expand_availabilities(result) == {
        gap_test.test_person.login: [
            {'date_from': datetime(2019, 3, 27, 10, 0), 'date_to': datetime(2019, 3, 27, 12, 0)},
            {'date_from': datetime(2019, 3, 27, 13, 0), 'date_to': datetime(2019, 3, 27, 14, 30)},
            {'date_from': datetime(2019, 3, 27, 15, 30), 'date_to': datetime(2019, 3, 27, 18, 0)},
        ]
    }


@pytest.mark.timeout(10)
def test_apply_gaps_timezone_dont_cause_infinity_loop():
    gaps = [
        {
            'workflow': 'absence',
            'person_login': 'xxx',
            'date_from': datetime(2021, 6, 2, 21, 1),
            'date_to': datetime(2021, 6, 3, 11, 0),
            'full_day': False,
            'work_in_absence': False,
            'id': 1456472,
        },
        {
            'workflow': 'remote_work',
            'person_login': 'xxx',
            'date_from': datetime(2021, 6, 3, 0, 0),
            'date_to': datetime(2021, 6, 4, 0, 0),
            'full_day': True,
            'work_in_absence': True,
            'id': 1457262,
        },
    ]
    date_from = datetime(2021, 6, 3)
    date_to = datetime(2021, 6, 6)
    person_timezone = timezone('Europe/Moscow')
    target = Availability(date_from=date_from, date_to=date_to, timezone=person_timezone)

    for gap in gaps:
        target.add_gap(gap)

    target.apply_gaps()

    assert len(target.gaps) == 2

import pytest

from unittest.mock import MagicMock, patch

from datetime import date, datetime

from staff.trip_questionary.controller.conditions import TripConditions as TC, ISSUE_STATUS, ISSUE_RESOLUTION


def test_are_taxi_dates_correct():
    trip = MagicMock(
        data={
            'trip_date_from': date(2019, 1, 1),
            'trip_date_to': date(2019, 1, 30),
        }
    )
    employee_data = {'trip_issue': {
        'taxiStartDate': datetime(2019, 1, 10),
        'taxiEndDate': datetime(2019, 1, 20),
    }}

    is_trip_employee_issue_created_mock = MagicMock(return_value=True)
    with patch.object(TC, 'is_trip_employee_issue_created', is_trip_employee_issue_created_mock):
        tc = TC(trip, employee_data=employee_data)
        assert tc.are_taxi_dates_correct


@pytest.mark.parametrize(
    'status,resolution,result', [
        (ISSUE_STATUS.APPROVED, None, True),
        (ISSUE_STATUS.CHECK_IN, None, True),
        (ISSUE_STATUS.TRIP_READY, None, True),
        (ISSUE_STATUS.NEED_AGREEMENT, None, True),
        (ISSUE_STATUS.NEED_INFO, None, True),
        (ISSUE_STATUS.IN_PROGRESS, None, True),
        (ISSUE_STATUS.REQUEST, None, True),
        (ISSUE_STATUS.VERIFIED, None, True),
        (ISSUE_STATUS.RECIEVED, None, True),
        (ISSUE_STATUS.ADVANCE_PAID, None, True),
        (ISSUE_STATUS.PAYMENT_OF_ADVANCE, None, True),
        (ISSUE_STATUS.PARTIALLY_REPORTED, None, True),
        (ISSUE_STATUS.PREPARATION, None, True),
        (ISSUE_STATUS.OPEN, None, False),
        (ISSUE_STATUS.HR_READY, None, False),
        (ISSUE_STATUS.CLOSED, ISSUE_RESOLUTION.FIXED, True),
        (ISSUE_STATUS.CLOSED, ISSUE_RESOLUTION.WONTFIX, False),
    ]
)
def test_is_ticket_approved(status, resolution, result):
    trip = MagicMock(data={'event_type': 'conf'})
    employee_data = {'conf_issue': {
        'status': {'key': status},
        'resolution': {'key': resolution},
    }}
    assert TC(trip, employee_data=employee_data).is_ticket_approved == result

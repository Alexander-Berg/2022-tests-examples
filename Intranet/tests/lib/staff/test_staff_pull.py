import pytest
from datetime import date

from intranet.trip.src.lib.staff.gateway import StaffGateway
from intranet.trip.src.lib.staff.sync import StaffTripPull
from intranet.trip.src.enums import PTStatus


pytestmark = pytest.mark.asyncio


raw_staff_trips = [
    {
        'uuid': '123',
        'trip_issue': {
            'key': 'TRAVEL-123',
        },
        'conf_issue': {
            'key': 'CONF-123',
        },
        'employee_list': [
            {
                'trip_issue': {
                    'key': 'TRAVEL-124',
                    'employee': {
                        'id': 'login',
                    },
                    'approvementStatus': 'Согласовано',
                },
                'conf_issue': {
                    'key': 'CONF-124',
                    'employee': {
                        'id': 'login',
                    },
                    'approvementStatus': 'Согласовано',
                },
            },
        ]
    }
]


class MockedStaffGateway(StaffGateway):

    async def get_trips_by_uuids(self, staff_trip_uuids: list[str]):
        return raw_staff_trips


async def test_staff_pull(f, uow):
    staff_pull = StaffTripPull(uow, staff_gateway=MockedStaffGateway())

    await f.create_city(city_id=1)
    await f.create_person(person_id=1, login='login')
    await f.create_trip(trip_id=1, staff_trip_uuid='123', issue_travel=None)
    await f.create_conf_details(trip_id=1, tracker_issue=None)
    await f.create_person_trip(
        trip_id=1,
        person_id=1,
        is_approved=False,
        status=PTStatus.draft,
        gap_date_from=date(2022, 10, 10),
        gap_date_to=date(2022, 10, 20),
        is_offline=True,
    )
    await f.create_person_conf_details(trip_id=1, person_id=1, tracker_issue=None)
    await f.create_travel_details(trip_id=1, person_id=1, tracker_issue=None)

    await staff_pull.pull_staff_trips()

    trip = await uow.trips.get_detailed_trip(trip_id=1, person_id=1)
    person_trip = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)

    assert trip.issue_travel == 'TRAVEL-123'
    assert trip.conf_details.tracker_issue == 'CONF-123'
    assert person_trip.travel_details.tracker_issue == 'TRAVEL-124'
    assert person_trip.conf_details.tracker_issue == 'CONF-124'
    assert person_trip.is_approved is True

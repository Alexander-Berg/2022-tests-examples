import pytest

from intranet.trip.src.enums import TripStatus, PTStatus
from intranet.trip.src.lib.staff.converters import StaffToTripConverter
from intranet.trip.src.models import Person


pytestmark = pytest.mark.asyncio

person_id = 123
purpose_id = 456
assignment_value = '30100'
city_from = 'Москва'
city_to = 'Санкт-Петербург'


def get_trip_raw(status: str = None, resolution: str = None):
    status = status or 'open'
    trip_issue = {
        "key": "TRAVEL-250951",
        "cityFrom": city_from,
        "cityTo": city_to,
        "countryTo": "Россия",
        "status": {
            "key": status,
        },
    }
    if resolution is not None:
        trip_issue['resolution'] = {
            'key': resolution,
        }
    return {
        "uuid": "1111111-2222-3333-4444-5555555",
        "author": person_id,
        "event_type": "trip",
        "purpose": [
            purpose_id
        ],
        "comment": "",
        "employee_list": [
            {
                "employee": str(person_id),
                "employee_assignment": assignment_value,
                "passport_number": "1111 1111111",
                "passport_name": "Тест Тест Тест",
                "compensation": "money",
                "mobile_packages": False,
                "need_mobile_additional_packages": "",
                "mobile_date_from": None,
                "mobile_date_to": None,
                "corporate_mobile_no": "",
                "need_taxi": True,
                "mobile_number_for_taxi": "+79999999999",
                "need_copy_of_insurance": False,
                "need_visa": False,
                "has_holidays": False,
                "holidays_comment": "",
                "custom_dates": False,
                "departure_date": None,
                "return_date": None,
                "transfer": "",
                "is_private": False,
                "trip_info": "",
                "comment": "",
                "trip_issue": trip_issue,
            }
        ],
        "event_date_from": None,
        "event_date_to": None,
        "trip_date_from": "2020-02-03T00:00:00",
        "trip_date_to": "2020-02-05T00:00:00",
        "receiver_side": "",
        "city_list": [
            {
                "is_return_route": False,
                "city": city_from,
                "country": "Россия",
            },
            {
                "is_return_route": True,
                "city": city_to,
                "country": "Россия",
            }
        ],
        "trip_issue": trip_issue,
    }


persons_map = {
    person_id: Person(
        uid='1123',
        first_name='Test',
        last_name='Test'
    )
}


@pytest.mark.parametrize('status, trip_status, person_trip_status', (
    ('open', TripStatus.new, PTStatus.new),
    ('closed', TripStatus.closed, PTStatus.closed),
    ('recieved', TripStatus.closed, PTStatus.closed),
    ('cancelled', TripStatus.cancelled, PTStatus.cancelled)
))
async def test_status_convertion_trip_from_staff(status, trip_status, person_trip_status):
    trips = StaffToTripConverter().convert(
        staff_trips=[get_trip_raw(status=status)],
        persons_map=persons_map,
    )
    assert trips[0].status == trip_status
    assert trips[0].person_trips[0].status == person_trip_status


@pytest.mark.parametrize('resolution, trip_status, person_trip_status', (
    ('fixed', TripStatus.closed, PTStatus.closed),
    ("won'tFix", TripStatus.cancelled, PTStatus.cancelled)
))
async def test_resolution_convertion_trip_from_staff(resolution, trip_status, person_trip_status):
    trips = StaffToTripConverter().convert(
        staff_trips=[get_trip_raw(resolution=resolution)],
        persons_map=persons_map,
    )
    assert trips[0].status == trip_status
    assert trips[0].person_trips[0].status == person_trip_status


async def test_city_convertion_trip_from_staff():
    trips = StaffToTripConverter().convert(
        staff_trips=[get_trip_raw()],
        persons_map=persons_map,
    )
    assert trips[0].city_from == city_from
    assert trips[0].city_to == city_to

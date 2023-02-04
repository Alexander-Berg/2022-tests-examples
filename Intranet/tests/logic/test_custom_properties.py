import pytest

from intranet.trip.src.logic.aeroclub.custom_properties import (
    GradeFetcher,
    FirstPurposeFetcher,
    SecondPurposeFetcher,
    AssignmentFetcher,
    IssueFetcher,
    fetch_custom_properties,
)
from intranet.trip.src.lib.aeroclub.models.custom_properties import CustomProperty


pytestmark = pytest.mark.asyncio


GRADE_1_AC_ID = 101
GRADE_2_AC_ID = 102
PURPOSE_1_AC_ID = 201
PURPOSE_2_AC_ID = 202
PURPOSE_3_AC_ID = 301
PURPOSE_NO_AC_ID = 302
PURPOSE_1_NAME = 'Кампус'
PURPOSE_2_NAME = 'Обучение'
PURPOSE_3_NAME = 'Конференция Яндекса'
PURPOSE_NO_NAME = 'Нет'
DEFAULT_ISSUE = 'TRAVEL-654321'
DEFAULT_ASSIGNMENT = 123


grade_data = {
    'id': 1,
    'name': {'en': 'Grade'},
    'is_list': True,
    'values': [
        {
            'id': GRADE_1_AC_ID,
            'name': {'ru': '1'},
        },
        {
            'id': GRADE_2_AC_ID,
            'name': {'ru': '2'},
        },
    ]
}

assignment_data = {
    'id': 2,
    'name': {'en': 'PURPOSE'},
    'is_list': False,
    'values': [],
}

issue_data = {
    'id': 3,
    'name': {'en': 'TRAVEL TICKET'},
    'is_list': False,
    'values': [],
}

first_purpose_data = {
    'id': 4,
    'name': {'en': 'PURPOSE OF TRIP 1'},
    'is_list': True,
    'values': [
        {
            'id': 201,
            'name': {'ru': PURPOSE_1_NAME},
        },
        {
            'id': 202,
            'name': {'ru': PURPOSE_2_NAME},
        },
    ]
}

second_purpose_data = {
    'id': 5,
    'name': {'en': 'PURPOSE OF TRIP 2'},
    'is_list': True,
    'values': [
        {
            'id': PURPOSE_3_AC_ID,
            'name': {'ru': PURPOSE_3_NAME},
        },
        {
            'id': PURPOSE_NO_AC_ID,
            'name': {'ru': PURPOSE_NO_NAME},
        },
    ]
}


def _build_aeroclub_data(custom_properties):
    return {
        'data': {
            'business_trip_custom_properties': [{
                'custom_properties': custom_properties,
            }]
        }
    }


async def _create_person_trip(f, uow, purpose_ids, tracker_issue):
    await f.create_purpose(purpose_id=1, aeroclub_grade=1, name=PURPOSE_1_NAME)
    await f.create_purpose(purpose_id=2, aeroclub_grade=1, name=PURPOSE_2_NAME)
    await f.create_purpose(purpose_id=3, aeroclub_grade=2, name=PURPOSE_3_NAME)
    await f.create_purpose(purpose_id=4, aeroclub_grade=3, name='Broken')

    await f.create_city(city_id=1)
    await f.create_person(person_id=1)
    await f.create_trip(trip_id=1, author_id=1)
    await f.create_person_trip(trip_id=1, person_id=1)

    for purpose_id in purpose_ids:
        await f.create_person_trip_purpose(trip_id=1, person_id=1, purpose_id=purpose_id)
    await f.create_travel_details(trip_id=1, person_id=1, city_id=1, tracker_issue=tracker_issue)

    person_trip = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)
    person_trip.assignment = DEFAULT_ASSIGNMENT
    return person_trip


fetcher_to_data = {
    GradeFetcher: grade_data,
    FirstPurposeFetcher: first_purpose_data,
    SecondPurposeFetcher: second_purpose_data,
    AssignmentFetcher: assignment_data,
    IssueFetcher: issue_data,
}


@pytest.mark.parametrize('purpose_ids, issue, fetcher_class, expected_value', (
    # Grade
    ([1, 3], DEFAULT_ISSUE, GradeFetcher, GRADE_2_AC_ID),
    ([1, 4], DEFAULT_ISSUE, GradeFetcher, GRADE_1_AC_ID),
    ([], DEFAULT_ISSUE, GradeFetcher, GRADE_1_AC_ID),
    # Issue
    ([1, 2], None, IssueFetcher, IssueFetcher.fallback_value),
    ([1, 2], 'TRAVEL-111333', IssueFetcher, 'TRAVEL-111333'),
    # Assignment
    ([1, 2], DEFAULT_ISSUE, AssignmentFetcher, DEFAULT_ASSIGNMENT),
    # First Purpose
    ([2, 3], DEFAULT_ISSUE, FirstPurposeFetcher, PURPOSE_2_AC_ID),
    ([], DEFAULT_ISSUE, FirstPurposeFetcher, PURPOSE_1_AC_ID),
    ([3], DEFAULT_ISSUE, FirstPurposeFetcher, PURPOSE_1_AC_ID),
    # Second Purpose
    ([1, 3], DEFAULT_ISSUE, SecondPurposeFetcher, PURPOSE_3_AC_ID),
    ([3], DEFAULT_ISSUE, SecondPurposeFetcher, PURPOSE_NO_AC_ID),
    ([1, 2], DEFAULT_ISSUE, SecondPurposeFetcher, PURPOSE_NO_AC_ID),
))
async def test_fetcher(f, uow, purpose_ids, issue, fetcher_class, expected_value):
    person_trip = await _create_person_trip(
        f,
        uow,
        purpose_ids=purpose_ids,
        tracker_issue=issue,
    )
    fetcher = fetcher_class(person_trip, CustomProperty(**fetcher_to_data[fetcher_class]))
    assert fetcher.fetch() == expected_value


async def test_fetcher_custom_properties_ok(f, uow):
    person_trip = await _create_person_trip(f, uow, purpose_ids=[1, 2], tracker_issue=DEFAULT_ISSUE)
    aeroclub_data = _build_aeroclub_data(custom_properties=[
        grade_data,
        issue_data,
        assignment_data,
        first_purpose_data,
        second_purpose_data,
    ])
    result = fetch_custom_properties(person_trip, aeroclub_data)
    assert result == {
        'id_properties': {
            1: GRADE_1_AC_ID,
            4: PURPOSE_1_AC_ID,
            5: PURPOSE_NO_AC_ID,
        },
        'text_properties': {
            2: DEFAULT_ASSIGNMENT,
            3: DEFAULT_ISSUE,
        },
    }


async def test_fetcher_missed_property(f, uow):
    person_trip = await _create_person_trip(f, uow, purpose_ids=[], tracker_issue=DEFAULT_ISSUE)
    custom_properties = [
        {
            'id': 1,
            'name': {'en': 'MISSED1'},
            'is_list': False,
            'values': [],
        },
        {
            'id': 2,
            'name': {'en': 'MISSED2'},
            'is_list': True,
            'values': [
                {
                    'id': 501,
                    'name': {'ru': '501'},
                },
                {
                    'id': 501,
                    'name': {'ru': '502'},
                },
            ],
        },
    ]
    aeroclub_data = _build_aeroclub_data(custom_properties)
    result = fetch_custom_properties(person_trip, aeroclub_data)
    assert result == {
        'id_properties': {2: 501},
        'text_properties': {1: AssignmentFetcher.fallback_value},
    }

from staff.trip_questionary.sorted import get_trip_list
from staff.lib.utils.date import parse_datetime


def test_filter_by_participants(company, create_trips, mocked_mongo):
    emp1 = company.persons['dep1-person']
    emp2 = company.persons['dep2-person']
    emp3 = company.persons['dep11-person']

    assert len(
        get_trip_list(
            participants=[emp1.id],
            date_from=parse_datetime('2010-01-01T00:00:00').date(),
            date_to=parse_datetime('2030-01-01T00:00:00').date(),
        )[0]
    ) == 1
    assert len(
        get_trip_list(
            participants=[emp1.id, emp2.id],
            date_from=parse_datetime('2010-01-01T00:00:00').date(),
            date_to=parse_datetime('2030-01-01T00:00:00').date(),
        )[0]
    ) == 2
    assert len(
        get_trip_list(
            participants=[emp1.id, emp2.id, emp3.id],
            date_from=parse_datetime('2010-01-01T00:00:00').date(),
            date_to=parse_datetime('2030-01-01T00:00:00').date(),
        )[0]
    ) == 3


def test_filter_by_date(company, create_trips):
    emp1 = company.persons['dep1-person']
    emp2 = company.persons['dep2-person']
    emp3 = company.persons['dep11-person']

    assert len(
        get_trip_list(
            participants=[emp1.id, emp2.id, emp3.id],
            date_from=parse_datetime('2010-01-01T00:00:00').date(),
            date_to=parse_datetime('2030-01-01T00:00:00').date(),
        )[0]
    ) == 3
    assert len(
        get_trip_list(
            participants=[emp1.id, emp2.id, emp3.id],
            date_from=parse_datetime('2018-02-01T00:00:00').date(),
            date_to=parse_datetime('2030-01-01T00:00:00').date(),
        )[0]
    ) == 2
    assert len(
        get_trip_list(
            participants=[emp1.id, emp2.id, emp3.id],
            date_from=parse_datetime('2018-03-06T00:00:00').date(),
            date_to=parse_datetime('2030-01-01T00:00:00').date(),
        )[0]
    ) == 1


def test_filter_by_type(company, create_trips):
    emp1 = company.persons['dep1-person']
    emp2 = company.persons['dep2-person']
    emp3 = company.persons['dep11-person']

    assert len(
        get_trip_list(
            participants=[emp1.id, emp2.id, emp3.id],
            date_from=parse_datetime('2000-01-01T00:00:00').date(),
            date_to=parse_datetime('2030-01-01T00:00:00').date(),
            event_type='trip',
        )[0]
    ) == 1
    assert len(
        get_trip_list(
            participants=[emp1.id, emp2.id, emp3.id],
            date_from=parse_datetime('2000-01-01T00:00:00').date(),
            date_to=parse_datetime('2030-01-01T00:00:00').date(),
            event_type='conf',
        )[0]
    ) == 1
    assert len(
        get_trip_list(
            participants=[emp1.id, emp2.id, emp3.id],
            date_from=parse_datetime('2000-01-01T00:00:00').date(),
            date_to=parse_datetime('2030-01-01T00:00:00').date(),
            event_type='trip_conf',
        )[0]
    ) == 1


def test_filter_by_limit(company, create_trips):
    emp1 = company.persons['dep1-person']
    emp2 = company.persons['dep2-person']
    emp3 = company.persons['dep11-person']

    trip_list = get_trip_list(
        participants=[emp1.id, emp2.id, emp3.id],
        date_from=parse_datetime('2010-01-01T00:00:00').date(),
        date_to=parse_datetime('2030-01-01T00:00:00').date(),
        limit=1,
    )
    assert len(trip_list[0]) == 1
    assert trip_list[0][0]['event_date_from'] == parse_datetime('2018-03-03T00:00:00')

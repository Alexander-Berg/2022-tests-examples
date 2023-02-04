# coding: utf-8

from __future__ import unicode_literals

from easymeeting.core import resource_event
from easymeeting.core import persons_availability
from easymeeting.lib import datetimes

INTERVAL_EXAMPLE = (
    datetimes.utc_datetime(2018, 3, 10, 12),
    datetimes.utc_datetime(2018, 3, 10, 13),
)
PERSONS_AVAILABILITY_EXAMPLE = persons_availability.PersonsAvailability(
    available=['mokhov'],
    unavailable=['m-smirnov'],
)


def test_create_resource_event_from_raw_event_with_interval():
    actual = resource_event.ResourceEvent.from_raw_event(
        interval=INTERVAL_EXAMPLE,
        persons_availability=PERSONS_AVAILABILITY_EXAMPLE,
        eventId=1111,
    )
    assert actual.interval == INTERVAL_EXAMPLE
    assert actual.persons_availability == PERSONS_AVAILABILITY_EXAMPLE
    assert actual.event_id == 1111


def test_create_resource_event_from_raw_event_with_start_end():
    actual = resource_event.ResourceEvent.from_raw_event(
        start='2018-03-10T12:00:00',
        end='2018-03-10T13:00:00',
        persons_availability=PERSONS_AVAILABILITY_EXAMPLE,
        eventId=1111,
    )
    assert actual.interval == INTERVAL_EXAMPLE
    assert actual.persons_availability == PERSONS_AVAILABILITY_EXAMPLE
    assert actual.event_id == 1111


def test_create_resource_event_from_raw_event_with_default_values():
    actual = resource_event.ResourceEvent.from_raw_event(
        interval=INTERVAL_EXAMPLE,
    )
    assert actual.persons_availability is None
    assert actual.event_id is None


def test_resource_hashability():
    first = dict(
        interval=INTERVAL_EXAMPLE,
        persons_availability=PERSONS_AVAILABILITY_EXAMPLE,
        event_id=1,
    )
    second = dict(
        interval=INTERVAL_EXAMPLE,
        persons_availability=PERSONS_AVAILABILITY_EXAMPLE,
        event_id=2,
    )
    obj_one = resource_event.ResourceEvent(**first)
    obj_two_same = resource_event.ResourceEvent(**first)
    obj_three = resource_event.ResourceEvent(**second)

    obj_set = {obj_one, obj_two_same, obj_three}
    assert sorted(obj_set) == sorted([
        obj_one,
        obj_three
    ])

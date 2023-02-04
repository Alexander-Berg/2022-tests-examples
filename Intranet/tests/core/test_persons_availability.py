# coding: utf-8

from __future__ import unicode_literals, division

from easymeeting.core import persons_availability


def test_persons_availability():
    sut = persons_availability.PersonsAvailability(
        available=['zhigalov', 'sibirev'],
        unavailable=['mokhov']
    )
    assert sut.available_count == 2
    assert sut.unavailable_count == 1
    assert sut.total_count == 3
    assert sut.availability == 2 / 3


def test_persons_availability_get_availability_without_persons():
    sut = persons_availability.PersonsAvailability()
    assert sut.availability == 0


def test_persons_availability_hashability():
    first = dict(
        available=('zhigalov', 'sibirev'),
        unavailable=('mokhov',),
    )
    second = dict(
        available=('zhigalov',),
        unavailable=('mokhov',),
    )
    obj_one = persons_availability.PersonsAvailability(**first)
    obj_two_same = persons_availability.PersonsAvailability(**first)
    obj_three = persons_availability.PersonsAvailability(**second)

    obj_set = {obj_one, obj_two_same, obj_three}
    assert sorted(obj_set) == sorted([
        obj_one,
        obj_three
    ])

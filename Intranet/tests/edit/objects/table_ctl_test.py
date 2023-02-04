import pytest

import datetime
from random import random

from staff.lib.testing import (
    ContactFactory,
    ContactTypeFactory,
    FloorFactory,
    TableBookFactory,
    TableFactory,
    StaffFactory,
    get_random_date,
    get_random_datetime,
)
from staff.person.models.contacts import ContactTypeId

from staff.map.edit.objects import TableCtl
from staff.map.errors import MultipleTableError
from staff.map.models import TableBook


@pytest.mark.django_db
def test_add_booking_no_table():
    author = StaffFactory()
    person = StaffFactory()
    date_from = get_random_date()
    date_to = get_random_date()

    target = TableCtl(None, author)

    with pytest.raises(AssertionError):
        target.add_booking(person, date_from, date_to)


@pytest.mark.django_db
def test_add_booking_has_booking_in_this_office():
    table = TableFactory()
    author = StaffFactory()
    person = StaffFactory()
    time_from = get_random_datetime(datetime.datetime.now() + datetime.timedelta(days=1))
    date_from = time_from.date()
    date_to = get_random_date(time_from)

    current_table = TableFactory(floor=FloorFactory(office=table.floor.office))
    TableBookFactory(
        created_at=get_random_datetime(),
        modified_at=get_random_datetime(),
        staff=person,
        table=current_table,
        date_from=date_from,
        date_to=date_to,
    )

    target = TableCtl(table, author)

    with pytest.raises(MultipleTableError) as exception_info:
        target.add_booking(person, date_from, date_to)

    expected_error = {
        'other_table_id': current_table.id,
        'overlap_finish': date_to,
        'overlap_start': date_from,
        'staff_id': person.id,
        'staff_login': person.login,
    }
    assert exception_info.value.errors == [{'code': 'staff-booked-another-table', 'data': expected_error}]
    assert TableBook.objects.filter(table=table).count() == 0, 'Table should not be booked'


@pytest.mark.django_db
def test_add_booking_on_overbooked_table():
    table = TableFactory()
    author = StaffFactory()
    person = StaffFactory()
    time_from = get_random_datetime(datetime.datetime.now() + datetime.timedelta(days=1))
    date_from = time_from.date()
    date_to = get_random_date(time_from)
    telegram = f'telegram {random()}'

    current_booking = TableBookFactory(
        created_at=get_random_datetime(),
        modified_at=get_random_datetime(),
        table=table,
        date_from=date_from,
        date_to=date_to,
    )
    telegram_type_contact = ContactTypeFactory(name='Telegram', id=ContactTypeId.telegram.value)
    ContactFactory(person=current_booking.staff, contact_type=telegram_type_contact, account_id=telegram)

    target = TableCtl(table, author)

    with pytest.raises(MultipleTableError) as exception_info:
        target.add_booking(person, date_from, date_to)

    expected_error = {
        'newer_staff_id': person.id,
        'newer_staff_login': person.login,
        'older_staff_id': current_booking.staff.id,
        'older_staff_login': current_booking.staff.login,
        'overlap_finish': date_to,
        'overlap_start': date_from,
        'telegram': telegram,
    }
    assert exception_info.value.errors == [{'code': 'book-overlap-different-person', 'data': expected_error}]
    assert TableBook.objects.filter(table=table, staff=person).count() == 0, 'Table should not be booked for person'


@pytest.mark.django_db
def test_add_booking_on_overbooked_table_no_telegram():
    table = TableFactory()
    author = StaffFactory()
    person = StaffFactory()
    time_from = get_random_datetime(datetime.datetime.now() + datetime.timedelta(days=1))
    date_from = time_from.date()
    date_to = get_random_date(time_from)

    current_booking = TableBookFactory(
        created_at=get_random_datetime(),
        modified_at=get_random_datetime(),
        table=table,
        date_from=date_from,
        date_to=date_to,
    )

    target = TableCtl(table, author)

    with pytest.raises(MultipleTableError) as exception_info:
        target.add_booking(person, date_from, date_to)

    expected_error = {
        'newer_staff_id': person.id,
        'newer_staff_login': person.login,
        'older_staff_id': current_booking.staff.id,
        'older_staff_login': current_booking.staff.login,
        'overlap_finish': date_to,
        'overlap_start': date_from,
        'telegram': None,
    }
    assert exception_info.value.errors == [{'code': 'book-overlap-different-person', 'data': expected_error}]
    assert TableBook.objects.filter(table=table, staff=person).count() == 0, 'Table should not be booked for person'

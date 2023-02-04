import pytest
from mock import patch, MagicMock

from random import random, randint

from staff.lib.testing import StaffFactory, TableFactory, get_random_date, FloorFactory

from staff.map.controllers.table import book_table_for_person_controller
from staff.map.errors import MultipleTableError


@pytest.mark.django_db
def test_book_table_for_person_controller_occupied_table():
    person = StaffFactory()
    table = TableFactory()
    date_from = get_random_date()
    date_to = get_random_date()
    occupied_by = [f'occupied_by {random()}' for _ in range(randint(2, 5))]

    table_ctl = MagicMock()
    table_ctl.occupied_by.values_list.return_value = occupied_by
    table_class = MagicMock(return_value=table_ctl)

    with patch('staff.map.controllers.table.TableCtl', table_class):
        with pytest.raises(MultipleTableError) as exception_info:
            book_table_for_person_controller(person, table, date_from, date_to)

    assert exception_info.value.errors == [{'code': 'reserved', 'occupied_by': occupied_by}]
    table_class.assert_called_once_with(table)
    table_ctl.occupied_by.values_list.assert_called_once_with('login', flat=True)


@pytest.mark.django_db
def test_book_table_for_person_controller_has_table_in_office():
    person = StaffFactory(table=TableFactory())
    table = TableFactory(floor=FloorFactory(office=person.table.floor.office))
    date_from = get_random_date()
    date_to = get_random_date()

    table_ctl = MagicMock()
    table_ctl.occupied_by.values_list.return_value = []
    table_class = MagicMock(return_value=table_ctl)

    with patch('staff.map.controllers.table.TableCtl', table_class):
        with pytest.raises(MultipleTableError) as exception_info:
            book_table_for_person_controller(person, table, date_from, date_to)

    assert exception_info.value.errors == [{'code': 'has_occupation_in_office', 'occupied': person.table.num}]
    table_class.assert_called_once_with(table)
    table_ctl.occupied_by.values_list.assert_called_once_with('login', flat=True)


@pytest.mark.django_db
def test_book_table_for_person_controller_has_no_table():
    person = StaffFactory()
    table = TableFactory()
    date_from = get_random_date()
    date_to = get_random_date()

    table_ctl = MagicMock()
    table_ctl.occupied_by.values_list.return_value = []
    table_class = MagicMock(return_value=table_ctl)

    with patch('staff.map.controllers.table.TableCtl', table_class):
        book_table_for_person_controller(person, table, date_from, date_to)

    table_class.assert_called_once_with(table)
    table_ctl.occupied_by.values_list.assert_called_once_with('login', flat=True)
    table_ctl.add_booking.assert_called_once_with(person, date_from, date_to)


@pytest.mark.django_db
def test_book_table_for_person_controller():
    person = StaffFactory(table=TableFactory())
    table = TableFactory()
    date_from = get_random_date()
    date_to = get_random_date()

    table_ctl = MagicMock()
    table_ctl.occupied_by.values_list.return_value = []
    table_class = MagicMock(return_value=table_ctl)

    with patch('staff.map.controllers.table.TableCtl', table_class):
        book_table_for_person_controller(person, table, date_from, date_to)

    table_class.assert_called_once_with(table)
    table_ctl.occupied_by.values_list.assert_called_once_with('login', flat=True)
    table_ctl.add_booking.assert_called_once_with(person, date_from, date_to)

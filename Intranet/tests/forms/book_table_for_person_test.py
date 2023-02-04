import datetime

import pytest

from django.conf import settings

from staff.lib.testing import verify_forms_error_code, TableFactory, StaffFactory

from staff.map.forms.book_table_for_person import BookTableForPersonForm


@pytest.mark.django_db
def test_no_person():
    target = BookTableForPersonForm(
        data={
            'person': None,
            'table': TableFactory().num,
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'person', 'required')


@pytest.mark.django_db
def test_invalid_person():
    target = BookTableForPersonForm(
        data={
            'person': 'Staff',
            'table': TableFactory().num,
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'person', 'invalid_choice')


@pytest.mark.django_db
def test_no_table():
    target = BookTableForPersonForm(
        data={
            'person': StaffFactory().login,
            'table': None,
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'table', 'required')


@pytest.mark.django_db
def test_invalid_table():
    target = BookTableForPersonForm(
        data={
            'person': StaffFactory().login,
            'table': 666,
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'table', 'invalid_choice')


@pytest.mark.django_db
def test_no_date_from():
    target = BookTableForPersonForm(
        data={
            'person': StaffFactory().login,
            'table': TableFactory().num,
        },
    )

    result = target.is_valid()

    assert result is True, target.errors
    assert target.cleaned_data['date_from'] == datetime.date.today()


@pytest.mark.django_db
def test_invalid_date_from():
    target = BookTableForPersonForm(
        data={
            'person': StaffFactory().login,
            'table': TableFactory().num,
            'date_from': 'nai',
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'date_from', 'invalid')


@pytest.mark.django_db
def test_date_from_in_the_past():
    target = BookTableForPersonForm(
        data={
            'person': StaffFactory().login,
            'table': TableFactory().num,
            'date_from': datetime.date(2020, 1, 1),
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'date_from', 'past')


@pytest.mark.django_db
def test_no_date_to():
    target = BookTableForPersonForm(
        data={
            'person': StaffFactory().login,
            'table': TableFactory().num,
        },
    )

    result = target.is_valid()

    assert result is True, target.errors
    assert target.cleaned_data['date_to'] == datetime.date.today()


@pytest.mark.django_db
def test_invalid_date_to():
    target = BookTableForPersonForm(
        data={
            'person': StaffFactory().login,
            'table': TableFactory().num,
            'date_to': 'nai',
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'date_to', 'invalid')


@pytest.mark.django_db
def test_date_to_before_date_from():
    target = BookTableForPersonForm(
        data={
            'person': StaffFactory().login,
            'table': TableFactory().num,
            'date_from': datetime.date.today() + datetime.timedelta(days=1),
            'date_to': datetime.date.today(),
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, '', 'invalid_time_interval')


@pytest.mark.django_db
def test_date_to_far_future():
    far_future = datetime.date.today() + datetime.timedelta(days=settings.MAP_TABLE_MAX_BOOK_PERIOD_FROM_TODAY + 1)

    target = BookTableForPersonForm(
        data={
            'person': StaffFactory().login,
            'table': TableFactory().num,
            'date_to': far_future,
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'date_to', 'two_weeks_exceeded')

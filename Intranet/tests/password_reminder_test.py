from datetime import datetime, timedelta

import pytest

from staff.lib.testing import PersonADInformationFactory

from staff.person.password_reminder import get_persons_to_remind

TODAY = datetime(year=2021, month=4, day=19)
ONE_YEAR = timedelta(days=365)
ONE_YEAR_AGO = TODAY.replace(year=2020)


@pytest.mark.django_db
@pytest.mark.parametrize('days_before_expiration', (1, 2, 3, 4, 5, 6, 7))
def test_will_remind_every_day_week_before_expiration(days_before_expiration):
    person = PersonADInformationFactory(password_expires_at=TODAY + timedelta(days_before_expiration))

    result = list(get_persons_to_remind([person.person.login], TODAY))

    assert [person_data['login'] for _, person_data in result] == [person.person.login]


@pytest.mark.django_db
@pytest.mark.parametrize('days_before_expiration, reminder', (
    (8, True),
    (9, True),
    (10, True),
    (11, True),
    (12, True),
    (13, True),
    (14, True),
    (15, False),
    (16, False),
    (17, False),
    (18, False),
    (19, False),
    (20, False),
    (21, False),
))
def test_will_remind_two_weeks_before(days_before_expiration, reminder):
    today = TODAY  # monday
    person = PersonADInformationFactory(password_expires_at=TODAY + timedelta(days_before_expiration))

    result = list(get_persons_to_remind([person.person.login], today))

    if reminder:
        assert [person_data['login'] for _, person_data in result] == [person.person.login]
    else:
        assert not [person_data['login'] for _, person_data in result]


@pytest.mark.django_db
@pytest.mark.parametrize('days_before_expiration, reminder', (
    (14, False),
    (15, True),
    (16, True),
    (17, True),
    (18, True),
    (19, True),
    (20, True),
    (21, True),
    (22, False),
))
def test_will_remind_three_weeks_before(days_before_expiration, reminder):
    today = TODAY + timedelta(days=1)  # tuesday
    person = PersonADInformationFactory(password_expires_at=today + timedelta(days_before_expiration))

    result = list(get_persons_to_remind([person.person.login], today))

    if reminder:
        assert [person_data['login'] for _, person_data in result] == [person.person.login]
    else:
        assert not [person_data['login'] for _, person_data in result]


@pytest.mark.django_db
@pytest.mark.parametrize('days_before_expiration, reminder', (
    (21, False),
    (22, True),
    (23, True),
    (24, True),
    (25, True),
    (26, True),
    (27, True),
    (28, True),
    (29, False),
    (30, False),
))
def test_will_remind_four_weeks_before(days_before_expiration, reminder):
    today = TODAY + timedelta(days=2)  # wednesday
    person = PersonADInformationFactory(password_expires_at=today + timedelta(days_before_expiration))

    result = list(get_persons_to_remind([person.person.login], today))

    if reminder:
        assert [person_data['login'] for _, person_data in result] == [person.person.login]
    else:
        assert not [person_data['login'] for _, person_data in result]

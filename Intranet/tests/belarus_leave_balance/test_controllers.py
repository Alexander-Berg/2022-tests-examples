import pytest
from mock import patch, MagicMock

import random
from datetime import date

from staff.lib.testing import StaffFactory, OrganizationFactory
from staff.person.models import Staff, StaffExtraFields
from staff.syncs.manual.belarus_leave_balance.controllers import not_belarus, upload_leave_balance


@pytest.mark.parametrize(
    'country_code, expected_result',
    [
        ('by', False),
        ('BY', False),
        ('ru', True),
        ('RU', True),
    ],
)
def test_not_belarus(country_code, expected_result):
    assert not_belarus(country_code) == expected_result


@pytest.mark.django_db
def test_upload_leave_balance_missing_login():
    login = f'test{random.random()}'

    data = {
        login: random.randint(0, 19986),
    }

    result = upload_leave_balance(data)
    assert result['missing'] == [login]


@pytest.mark.django_db
def test_upload_leave_balance_not_belarus_login():
    login = f'test{random.random()}'
    country_code = f'{random.randint(0, 999)}'
    old_vacation = random.randint(0, 100)

    data = {
        login: random.randint(0, 19986),
    }

    not_belarus_mock = MagicMock(return_value=True)
    StaffFactory(login=login, organization=OrganizationFactory(country_code=country_code), vacation=old_vacation)

    with patch('staff.syncs.manual.belarus_leave_balance.controllers.not_belarus', not_belarus_mock):
        result = upload_leave_balance(data)
        assert result['invalid_country'] == [login]
        not_belarus_mock.assert_called_once_with(country_code)

        person = Staff.objects.get(login=login)
        assert person.vacation == old_vacation


@pytest.mark.django_db
def test_upload_leave_balance():
    login = f'test{random.random()}'
    country_code = f'{random.randint(0, 999)}'
    old_vacation = random.randint(0, 100)

    data = {
        login: random.randint(0, 19986),
    }

    not_belarus_mock = MagicMock(return_value=False)
    StaffFactory(login=login, organization=OrganizationFactory(country_code=country_code), vacation=old_vacation)
    today = date.today()
    date_mock = MagicMock(today=MagicMock(return_value=today))

    with patch('staff.syncs.manual.belarus_leave_balance.controllers.date', date_mock):
        with patch('staff.syncs.manual.belarus_leave_balance.controllers.not_belarus', not_belarus_mock):
            result = upload_leave_balance(data)
            assert result['success'] == [login]

            person = Staff.objects.get(login=login)
            assert person.vacation == data[login]


@pytest.mark.django_db
def test_upload_leave_balance_staff_with_extra():
    login = f'test{random.random()}'
    country_code = f'{random.randint(0, 999)}'
    old_vacation = random.randint(0, 100)

    data = {
        login: random.randint(0, 19986),
    }

    not_belarus_mock = MagicMock(return_value=False)
    staff = StaffFactory(
        login=login,
        organization=OrganizationFactory(country_code=country_code),
        vacation=old_vacation,
    )
    StaffExtraFields.objects.create(staff=staff).save()
    today = date.today()
    date_mock = MagicMock(today=MagicMock(return_value=today))

    with patch('staff.syncs.manual.belarus_leave_balance.controllers.date', date_mock):
        with patch('staff.syncs.manual.belarus_leave_balance.controllers.not_belarus', not_belarus_mock):
            result = upload_leave_balance(data)
            assert result['success'] == [login]

            person = Staff.objects.get(login=login)
            assert person.vacation == data[login]
            assert person.extra.last_vacation_accrual_at == today

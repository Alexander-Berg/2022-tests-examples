import pytest
from mock import MagicMock, patch

import random
from datetime import datetime

from staff.lib.testing import StaffFactory, get_random_datetime
from staff.whistlah.models import StaffLastOffice
from staff.whistlah.utils import parse_postgres_activity, remove_already_existing_activities


@pytest.mark.django_db
def test_remove_already_existing_in_database():
    # given
    denis = StaffFactory(login='denis-p')
    terrmit = StaffFactory(login='terrmit')

    StaffLastOffice.objects.create(staff=denis, updated_at=datetime(year=2022, month=3, day=15, hour=0, minute=0))
    activities = {
        denis.id: {'updated_at': datetime(year=2022, month=3, day=15, hour=0, minute=0)},
        terrmit.id: {'updated_at': datetime(year=2022, month=3, day=15, hour=0, minute=0)},
    }

    # when
    result = remove_already_existing_activities(activities)

    # then
    assert result == {
        terrmit.id: {'updated_at': datetime(year=2022, month=3, day=15, hour=0, minute=0)},
    }


def test_parse_postgres_activity_unknown_ip():
    updated_at = get_random_datetime()
    host = random.random()
    login = f'login-{random.randint(1, 9999)}'

    get_offices_mock = MagicMock(return_value={host: None})
    get_office_names_mock = MagicMock(return_value=dict())
    raw_record_to_result_mock = MagicMock()

    with patch('staff.whistlah.utils.get_offices', get_offices_mock):
        with patch('staff.whistlah.utils.get_office_names', get_office_names_mock):
            with patch('staff.whistlah.utils.raw_record_to_result', raw_record_to_result_mock):
                result = parse_postgres_activity([(updated_at, host, login)])

    raw_record_to_result_mock.assert_called_once_with(updated_at, None, None)

    assert login in result
    assert result[login] == raw_record_to_result_mock.return_value

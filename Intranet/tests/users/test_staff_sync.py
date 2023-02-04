import datetime

import mock
import pytest

from intranet.table_flow.src.users import staff_sync, models
from intranet.table_flow.tests.helpers import factories


def _create_staff_api_answer_unit(
    username, staff_id, email=None,
    is_active=True, date_joined=None,
    first_name=None, last_name=None,
):
    first_name = first_name or username
    last_name = last_name or username
    return {
        'login': username,
        'id': staff_id,
        'work_email': email or f'{username}@yandex-team.ru',
        'official': {
            'is_dismissed': not is_active,
            'join_at': (date_joined or datetime.datetime.now()).strftime('%Y-%m-%d'),
        },
        'name': {
            'first': {'en': first_name, 'ru': first_name},
            'last': {'en': last_name, 'ru': last_name},
        }
    }


def _staff_mock(return_values):
    iterator = iter(return_values)

    def inner(*args, **kwargs):
        try:
            return [next(iterator)]
        except StopIteration:
            return []

    return inner


@pytest.mark.django_db
def test_staff_create():
    some_db_user = factories.User()
    new_username, new_staff_id = 'someuser', 2
    staff_answer = [
        _create_staff_api_answer_unit(new_username, new_staff_id),
    ]

    with mock.patch(
        'intranet.table_flow.src.users.staff_sync._call_api', _staff_mock(staff_answer)
    ):
        res = staff_sync.update_users_info()

    some_db_user.refresh_from_db()
    assert models.User.objects.get(username=new_username, staff_id=new_staff_id)
    assert res == 1


@pytest.mark.django_db
def test_staff_update():
    some_db_user = factories.User()
    new_first_name, new_last_name = 'new_first_name', 'new_last_name'
    staff_answer = [
        _create_staff_api_answer_unit(
            username=some_db_user.username,
            staff_id=some_db_user.staff_id,
            email=some_db_user.email,
            date_joined=some_db_user.date_joined,
            first_name=new_first_name,
            last_name=new_last_name,
            is_active=False,
        ),
    ]

    with mock.patch(
        'intranet.table_flow.src.users.staff_sync._call_api', _staff_mock(staff_answer)
    ):
        res = staff_sync.update_users_info(update_users=True)

    some_db_user.refresh_from_db()
    assert some_db_user.first_name == new_first_name
    assert some_db_user.last_name == new_last_name
    assert not some_db_user.is_active
    assert res == 0

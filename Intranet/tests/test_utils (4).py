import datetime
import random
from typing import List
from unittest import mock

import freezegun
import pytest
from django.conf import settings
from django.utils import timezone

from idm.core.models import Action
from idm.tests.utils import random_slug, create_user
from idm.utils import calendar, chunkify, events
from idm.utils.actions import ActionWrapManager

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('pass_dt', [True, False])
@pytest.mark.parametrize('with_time', [None, datetime.time(3)])
@pytest.mark.parametrize('holidays_in_row', [10, 15])
@mock.patch('idm.integration.calendar.get_holidays', return_value=[])
def test_calendar_get_next_workday(
        get_holidays_mock: mock.Mock,
        pass_dt: bool,
        with_time: datetime.time,
        holidays_in_row: int,
):
    dt = timezone.now()
    kwargs = {}
    if pass_dt:
        kwargs['dt'] = dt
    if with_time:
        kwargs['with_time'] = with_time
    holidays = [dt.date() + timezone.timedelta(days=i) for i in range(holidays_in_row + 2)]
    # промежуток между выходными - рабочий день
    expected_workday = datetime.datetime.combine(holidays.pop(-2), with_time or dt.time(), tzinfo=dt.tzinfo)
    get_holidays_mock.side_effect = chunkify(holidays, chunk_size=14)

    with freezegun.freeze_time(dt):
        assert expected_workday == calendar.get_next_workday(**kwargs)

    if holidays_in_row > 14:
        assert get_holidays_mock.call_count == 2
        get_holidays_mock.assert_has_calls([
            mock.call(dt.date()),
            mock.call(dt.date() + timezone.timedelta(days=14))
        ])
    else:

        get_holidays_mock.assert_called_once_with(dt.date())


@pytest.mark.parametrize('pass_dt', [True, False])
@pytest.mark.parametrize('with_time', [None, datetime.time(3)])
@mock.patch('idm.integration.calendar.get_holidays', return_value=[])
def test_calendar_get_next_workday__no_holidays(
        get_holidays_mock: mock.Mock,
        pass_dt: bool,
        with_time: datetime.time,
):
    dt = timezone.now()
    kwargs = {}
    if pass_dt:
        kwargs['dt'] = dt
    if with_time:
        kwargs['with_time'] = with_time
    expected_workday = datetime.datetime.combine(dt.date(), with_time or dt.time(), tzinfo=dt.tzinfo)

    with freezegun.freeze_time(dt):
        assert expected_workday == calendar.get_next_workday(**kwargs)

    get_holidays_mock.assert_called_once_with(dt.date())


def test_events__get_collection(mongo_mock):
    assert events._get_collection() is getattr(mongo_mock, settings.MONGO_EVENTS_COLLECTION)


@pytest.mark.parametrize('pass_role_id', [True, False])
def test_events__add_event(mongo_mock, pass_role_id):
    kwargs = {
        'event_type': events.EventType.YT_EXPORT_REQUIRED,
        'system_id': random.randint(1, 10**6),
    }
    if pass_role_id:
        kwargs['role_id'] = (random.randint(1, 10**6))

    freeze_time = timezone.now()
    with freezegun.freeze_time(freeze_time):
        events.add_event(**kwargs)

    event = {
        '_id': mock.ANY,
        'added': freeze_time,
        'system_id': kwargs['system_id'],
        'event': kwargs['event_type'].value,
        'role_id': kwargs.get('role_id'),
    }
    assert list(mongo_mock[settings.MONGO_EVENTS_COLLECTION]) == [event]


def test_events__remove_events(mongo_mock):
    event_collection = mongo_mock[settings.MONGO_EVENTS_COLLECTION]
    event_list = [
        {'event': events.EventType.YT_EXPORT_REQUIRED.value, 'system_id': random.randint(1, 10 ** 6)}
        for _ in range(5)
    ]
    event_ids = event_collection.insert_many(*event_list)
    ids_to_remove = random.sample(event_ids, k=3)

    events.remove_events(ids_to_remove)
    assert set(item['_id'] for item in event_list) - set(ids_to_remove) == set(item['_id'] for item in event_collection)


def test_actions__action_wrap_manager():
    start_key = random_slug()
    finish_key = random_slug()
    extra = {'user': create_user()}
    success_call = mock.MagicMock()
    success_data = {'data': {random_slug(): random_slug()}}
    fail_call = mock.MagicMock()
    fail_data = {'data': {random_slug(): random_slug()}}
    with ActionWrapManager(start_key, finish_key, extra=extra) as manager:
        manager.on_success(lambda mngr: success_call(mngr))
        manager.on_success(success_data)
        manager.on_failure(lambda mngr: fail_call(mngr))
        manager.on_failure(fail_data)

    start_action, finish_action = Action.objects.select_related('parent', 'user').order_by('added')  # type: Action
    assert start_action.action == start_key
    assert finish_action.action == finish_key
    for attribute, value in {**extra, **success_data}.items():
        assert getattr(finish_action, attribute) == value
    success_call.assert_called_once_with(manager)
    fail_call.assert_not_called()


@pytest.mark.parametrize('pass_fail_key', [True, False])
def test_actions__action_wrap_manager_failure(pass_fail_key: bool):
    start_key = random_slug()
    finish_key = random_slug()
    fail_key = pass_fail_key and random_slug() or None
    extra = {'user': create_user()}
    success_call = mock.MagicMock()
    success_data = {'data': {random_slug(): random_slug()}}
    fail_call = mock.MagicMock()
    fail_data = {'data': {random_slug(): random_slug()}}
    traceback = random_slug()
    with mock.patch('idm.utils.actions.format_error_traceback', return_value=traceback),\
            ActionWrapManager(start_key, finish_key, fail_action=fail_key, extra=extra) as manager:
        manager.on_success(lambda mngr: success_call(mngr))
        manager.on_success(success_data)
        manager.on_failure(lambda mngr: fail_call(mngr))
        manager.on_failure(fail_data)
        raise Exception

    start_action, finish_action = Action.objects.select_related('parent', 'user').order_by('added')  # type: Action
    assert start_action.action == start_key
    assert finish_action.action == (pass_fail_key and fail_key or finish_key)

    for attribute, value in {**extra, **fail_data}.items():
        if attribute == 'data':
            value['traceback'] = traceback
        assert getattr(finish_action, attribute) == value
    fail_call.assert_called_once_with(manager)
    success_call.assert_not_called()
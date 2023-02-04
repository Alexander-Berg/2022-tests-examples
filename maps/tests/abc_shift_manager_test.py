import pytest
from datetime import datetime

from maps.pylibs.infrastructure_api.abc.abc_duty_api import AbcDutyApi
from maps.infra.duty.generator.lib.shift_manager import AbcShiftManager, Shift, CommitedShift


def test_shifts_no_replaces(abc_duty_mock) -> None:
    abc_duty_mock.add_shifts_preset(
        abc_service='maps-duty-infra',
        schedule_slug='primary',
        shifts=[
            {
                "id": 2409869,
                "person": {"id": 30204, "login": "alexey-savin"},
                "schedule": {"id": 3794, "slug": "primary"},
                "is_approved": True,
                "start_datetime": "2021-08-09T09:00:00+03:00",
                "end_datetime": "2021-08-13T21:00:00+03:00",
            }
        ]
    )

    duty_api = AbcDutyApi(oauth_token='fake')
    shift_manager = AbcShiftManager(abc_duty=duty_api,
                                    service_slug='maps-duty-infra',
                                    schedule_slug='primary')
    # NB: query interval in the middle of the shift returns whole shift
    shifts = shift_manager.shifts(since=datetime(2021, 8, 9), till=datetime(2021, 8, 10))
    assert shifts == [
        CommitedShift(login='alexey-savin', id='2409869',
                      name='maps-duty-infra primary: @alexey-savin',
                      since=datetime(2021, 8, 9, 9, 0),
                      till=datetime(2021, 8, 13, 21, 0))
    ]


def test_shifts_with_replaces(abc_duty_mock) -> None:
    abc_duty_mock.add_shifts_preset(
        abc_service='maps-duty-infra',
        schedule_slug='secondary',
        shifts=[
            {
                "id": 2409869,
                "person": {"id": 30204, "login": "alexey-savin"},
                "schedule": {"id": 3794, "slug": "secondary"},
                "is_approved": True,
                "start_datetime": "2021-08-09T09:00:00+03:00",
                "end_datetime": "2021-08-13T21:00:00+03:00",
                "replaces": [
                    {
                        "id": 2409870,
                        "person": {"id": 36981, "login": "comradeandrew"},
                        "schedule": {"id": 3794, "slug": "secondary"},
                        "is_approved": True,
                        "start_datetime": "2021-08-10T00:00:00+03:00",
                        "end_datetime": "2021-08-11T00:00:00+03:00"
                    },
                    {
                        "id": 2409871,
                        "person": {"id": 36981, "login": "comradeandrew"},
                        "schedule": {"id": 3794, "slug": "secondary"},
                        "is_approved": True,
                        "start_datetime": "2021-08-12T00:00:00+03:00",
                        "end_datetime": "2021-08-13T00:00:00+03:00"
                    },
                ]
            }
        ]
    )

    duty_api = AbcDutyApi(oauth_token='fake')
    shift_manager = AbcShiftManager(abc_duty=duty_api,
                                    service_slug='maps-duty-infra',
                                    schedule_slug='secondary')
    shifts = shift_manager.shifts(since=datetime(2021, 8, 9), till=datetime(2021, 8, 13))
    assert shifts == [
        CommitedShift(login='alexey-savin', id='2409869',
                      name='maps-duty-infra secondary: @alexey-savin',
                      since=datetime(2021, 8, 9, 9, 0),
                      till=datetime(2021, 8, 10, 0, 0)),
        CommitedShift(login='comradeandrew', id='2409870',
                      name='maps-duty-infra secondary: @comradeandrew replaces @alexey-savin',
                      since=datetime(2021, 8, 10, 0, 0),
                      till=datetime(2021, 8, 11, 0, 0)),
        CommitedShift(login='alexey-savin', id='2409869',
                      name='maps-duty-infra secondary: @alexey-savin',
                      since=datetime(2021, 8, 11, 0, 0),
                      till=datetime(2021, 8, 12, 0, 0)),
        CommitedShift(login='comradeandrew', id='2409871',
                      name='maps-duty-infra secondary: @comradeandrew replaces @alexey-savin',
                      since=datetime(2021, 8, 12, 0, 0),
                      till=datetime(2021, 8, 13, 0, 0)),
        CommitedShift(login='alexey-savin', id='2409869',
                      name='maps-duty-infra secondary: @alexey-savin',
                      since=datetime(2021, 8, 13, 0, 0),
                      till=datetime(2021, 8, 13, 21, 0)),
    ]

    # Filter out first and last shifts by inteval
    shifts = shift_manager.shifts(since=datetime(2021, 8, 11), till=datetime(2021, 8, 12))
    assert shifts == [
        CommitedShift(login='comradeandrew', id='2409870',
                      name='maps-duty-infra secondary: @comradeandrew replaces @alexey-savin',
                      since=datetime(2021, 8, 10, 0, 0),
                      till=datetime(2021, 8, 11, 0, 0)),
        CommitedShift(login='alexey-savin', id='2409869',
                      name='maps-duty-infra secondary: @alexey-savin',
                      since=datetime(2021, 8, 11, 0, 0),
                      till=datetime(2021, 8, 12, 0, 0)),
        CommitedShift(login='comradeandrew', id='2409871',
                      name='maps-duty-infra secondary: @comradeandrew replaces @alexey-savin',
                      since=datetime(2021, 8, 12, 0, 0),
                      till=datetime(2021, 8, 13, 0, 0)),
    ]


def test_shifts_whole_replaced(abc_duty_mock) -> None:
    abc_duty_mock.add_shifts_preset(
        abc_service='maps-duty-infra',
        schedule_slug='secondary',
        shifts=[
            {
                "id": 2409869,
                "person": {"id": 30204, "login": "alexey-savin"},
                "schedule": {"id": 3794, "slug": "secondary"},
                "is_approved": True,
                "start_datetime": "2021-08-09T09:00:00+03:00",
                "end_datetime": "2021-08-13T21:00:00+03:00",
                "replaces": [
                    {
                        "id": 2409870,
                        "person": {"id": 36981, "login": "comradeandrew"},
                        "schedule": {"id": 3794, "slug": "secondary"},
                        "is_approved": True,
                        "start_datetime": "2021-08-09T09:00:00+03:00",
                        "end_datetime": "2021-08-12T09:00:00+03:00"
                    },
                    {
                        "id": 2409871,
                        "person": {"id": 36981, "login": "nikitonsky"},
                        "schedule": {"id": 3794, "slug": "secondary"},
                        "is_approved": True,
                        "start_datetime": "2021-08-12T09:00:00+03:00",
                        "end_datetime": "2021-08-13T21:00:00+03:00"
                    },
                ]
            }
        ]
    )

    duty_api = AbcDutyApi(oauth_token='fake')
    shift_manager = AbcShiftManager(abc_duty=duty_api,
                                    service_slug='maps-duty-infra',
                                    schedule_slug='secondary')
    shifts = shift_manager.shifts(since=datetime(2021, 8, 9), till=datetime(2021, 8, 13))
    assert shifts == [
        CommitedShift(login='comradeandrew', id='2409870',
                      name='maps-duty-infra secondary: @comradeandrew replaces @alexey-savin',
                      since=datetime(2021, 8, 9, 9, 0),
                      till=datetime(2021, 8, 12, 9, 0)),
        CommitedShift(login='nikitonsky', id='2409871',
                      name='maps-duty-infra secondary: @nikitonsky replaces @alexey-savin',
                      since=datetime(2021, 8, 12, 9, 0),
                      till=datetime(2021, 8, 13, 21, 0)),
    ]


def test_shifts_with_nobody_assigned(abc_duty_mock) -> None:
    abc_duty_mock.add_shifts_preset(
        abc_service='maps-duty-infra',
        schedule_slug='primary',
        shifts=[
            {   # Nobody's shift
                "id": 2409869,
                "person": None,
                "schedule": {"id": 3794, "slug": "primary"},
                "start_datetime": "2021-08-01T09:00:00+03:00",
                "end_datetime": "2021-08-08T21:00:00+03:00",
            },
            {   # Shift with replacement by nobody
                "id": 2409870,
                "person": {"id": 30204, "login": "alexey-savin"},
                "schedule": {"id": 3794, "slug": "primary"},
                "start_datetime": "2021-08-09T09:00:00+03:00",
                "end_datetime": "2021-08-10T21:00:00+03:00",
                "replaces": [
                    {
                        "id": 2409871,
                        "person": None,
                        "schedule": {"id": 3794, "slug": "primary"},
                        "start_datetime": "2021-08-09T09:00:00+03:00",
                        "end_datetime": "2021-08-09T09:00:00+03:00"
                    }
                ]
            },
            {   # Nobody's shift with replacement
                "id": 2409872,
                "person": None,
                "schedule": {"id": 3794, "slug": "primary"},
                "start_datetime": "2021-08-11T09:00:00+03:00",
                "end_datetime": "2021-08-12T21:00:00+03:00",
                "replaces": [
                    {
                        "id": 2409873,
                        "person": {"id": 36981, "login": "comradeandrew"},
                        "schedule": {"id": 3794, "slug": "primary"},
                        "start_datetime": "2021-08-12T09:00:00+03:00",
                        "end_datetime": "2021-08-12T21:00:00+03:00"
                    },
                ]
            },
        ]
    )

    duty_api = AbcDutyApi(oauth_token='fake')
    shift_manager = AbcShiftManager(abc_duty=duty_api,
                                    service_slug='maps-duty-infra',
                                    schedule_slug='primary')
    shifts = shift_manager.shifts(since=datetime(2021, 8, 1), till=datetime(2021, 8, 25))
    # Expect shifts with real assignees only
    assert shifts == [
        CommitedShift(login='alexey-savin', id='2409870',
                      since=datetime(2021, 8, 9, 9, 0),
                      till=datetime(2021, 8, 10, 21, 0),
                      name='maps-duty-infra primary: @alexey-savin'),
        CommitedShift(login='comradeandrew', id='2409873',
                      since=datetime(2021, 8, 12, 9, 0),
                      till=datetime(2021, 8, 12, 21, 0),
                      name='maps-duty-infra primary: @comradeandrew replaces @None'),
    ]


def test_publish_unknown_schedule(abc_duty_mock) -> None:
    duty_api = AbcDutyApi(oauth_token='fake')
    shift_manager = AbcShiftManager(abc_duty=duty_api,
                                    service_slug='maps-duty-infra',
                                    schedule_slug='primary')
    with pytest.raises(Exception, match='primary not found in maps-duty-infra'):
        shift_manager.publish(
            [Shift(login='alexey-savin', since=datetime(2021, 8, 10, 9, 0), till=datetime(2021, 8, 11, 9, 0))]
        )


def test_publish(abc_duty_mock) -> None:
    abc_duty_mock.add_schedule_preset(
        abc_service='maps-core-ratelimiter-probe',
        schedule={
            "id": 3794,
            "recalculate": False,
            "name": "secondary",
            "service": {"id": 3559, "slug": "maps-core-ratelimiter-probe", "parent": 2178},
            "duration": "5 00:00:00",
            "start_date": "2021-08-09",
            "slug": "primary"
        }
    )
    abc_duty_mock.add_shifts_preset(
        abc_service='maps-core-ratelimiter-probe',
        schedule_slug='primary',
        shifts=[
            {
                "id": 1,
                "person": {"id": 30204, "login": "alexey-savin"},
                "schedule": {"id": 3794, "slug": "primary"},
                "is_approved": True,
                "start_datetime": "2021-08-09T09:00:00+03:00",
                "end_datetime": "2021-08-10T09:00:00+03:00",
            },
            # gap
            {
                "id": 2,
                "person": {"id": 30204, "login": "alexey-savin"},
                "schedule": {"id": 3794, "slug": "primary"},
                "is_approved": True,
                "start_datetime": "2021-08-23T09:00:00+03:00",
                "end_datetime": "2021-08-24T09:00:00+03:00",
            },
        ]
    )

    duty_api = AbcDutyApi(oauth_token='fake')
    shift_manager = AbcShiftManager(abc_duty=duty_api,
                                    service_slug='maps-core-ratelimiter-probe',
                                    schedule_slug='primary')
    # Publish shifts into the existing gap
    shift_manager.publish(
        [Shift(login='alexey-savin', since=datetime(2021, 8, 10, 9, 0), till=datetime(2021, 8, 11, 9, 0)),
         Shift(login='alexey-savin', since=datetime(2021, 8, 11, 9, 0), till=datetime(2021, 8, 12, 9, 0)),
         Shift(login='alexey-savin', since=datetime(2021, 8, 12, 9, 0), till=datetime(2021, 8, 13, 9, 0)),
         Shift(login='comradeandrew', since=datetime(2021, 8, 13, 9, 0), till=datetime(2021, 8, 14, 9, 0)),
         Shift(login='comradeandrew', since=datetime(2021, 8, 14, 9, 0), till=datetime(2021, 8, 15, 21, 0)),
         # gap
         Shift(login='alexey-savin', since=datetime(2021, 8, 17, 9, 0), till=datetime(2021, 8, 18, 9, 0)),
         Shift(login='alexey-savin', since=datetime(2021, 8, 18, 9, 0), till=datetime(2021, 8, 19, 9, 0)),
         # gap
         Shift(login='alexey-savin', since=datetime(2021, 8, 22, 9, 0), till=datetime(2021, 8, 23, 9, 0))]
    )

    # Expect no deletes of the existing shifts:
    assert not abc_duty_mock.abc_duty_storage.deleted_shifts
    assert abc_duty_mock.abc_duty_storage.appended_shifts == {
        3794: [
            {
                'person': 'alexey-savin',
                'start_datetime': '2021-08-10T09:00:00',
                'end_datetime': '2021-08-13T09:00:00',
                'schedule': 3794, 'is_approved': True
            },
            {
                'person': 'comradeandrew',
                'start_datetime': '2021-08-13T09:00:00',
                'end_datetime': '2021-08-15T21:00:00',
                'schedule': 3794, 'is_approved': True
            },
            # gap
            {
                'person': 'alexey-savin',
                'start_datetime': '2021-08-17T09:00:00',
                'end_datetime': '2021-08-19T09:00:00',
                'schedule': 3794, 'is_approved': True
            },
            # gap
            {
                'person': 'alexey-savin',
                'start_datetime': '2021-08-22T09:00:00',
                'end_datetime': '2021-08-23T09:00:00',
                'schedule': 3794, 'is_approved': True
            },
        ]
    }


def test_publish_over_existing(abc_duty_mock) -> None:
    abc_duty_mock.add_schedule_preset(
        abc_service='maps-core-ratelimiter-probe',
        schedule={
            "id": 3794,
            "recalculate": False,
            "service": {"id": 3559, "slug": "maps-core-ratelimiter-probe", "parent": 2178},
            "duration": "5 00:00:00",
            "start_date": "2021-08-09",
            "slug": "primary"
        }
    )
    abc_duty_mock.add_shifts_preset(
        abc_service='maps-core-ratelimiter-probe',
        schedule_slug='primary',
        shifts=[
            {
                "id": 1,
                "person": {"id": 30204, "login": "alexey-savin"},
                "schedule": {"id": 3794, "slug": "primary"},
                "is_approved": True,
                "start_datetime": "2021-08-09T09:00:00+03:00",
                "end_datetime": "2021-08-10T09:00:00+03:00",
            },
            # gap
            {
                "id": 2,
                "person": {"id": 30204, "login": "alexey-savin"},
                "schedule": {"id": 3794, "slug": "primary"},
                "is_approved": True,
                "start_datetime": "2021-08-12T09:00:00+03:00",
                "end_datetime": "2021-08-13T09:00:00+03:00",
            },
            # gap
            {
                "id": 3,
                "person": {"id": 30204, "login": "alexey-savin"},
                "schedule": {"id": 3794, "slug": "primary"},
                "is_approved": True,
                "start_datetime": "2021-08-15T09:00:00+03:00",
                "end_datetime": "2021-08-20T21:00:00+03:00",
            }
        ]
    )

    duty_api = AbcDutyApi(oauth_token='fake')
    shift_manager = AbcShiftManager(abc_duty=duty_api,
                                    service_slug='maps-core-ratelimiter-probe',
                                    schedule_slug='primary')

    new_shifts = [
        Shift(login='comradeandrew', since=datetime(2021, 8, 10, 9, 0), till=datetime(2021, 8, 11, 9, 0)),
        Shift(login='comradeandrew', since=datetime(2021, 8, 11, 9, 0), till=datetime(2021, 8, 12, 9, 0)),
        Shift(login='comradeandrew', since=datetime(2021, 8, 12, 9, 0), till=datetime(2021, 8, 13, 9, 0)),
        Shift(login='comradeandrew', since=datetime(2021, 8, 13, 9, 0), till=datetime(2021, 8, 14, 9, 0)),
        Shift(login='comradeandrew', since=datetime(2021, 8, 14, 9, 0), till=datetime(2021, 8, 15, 21, 0))
    ]

    shift_manager.publish(new_shifts)
    assert abc_duty_mock.abc_duty_storage.appended_shifts == {
        3794: [
            {
                'person': 'comradeandrew',
                'start_datetime': '2021-08-10T09:00:00',
                'end_datetime': '2021-08-15T21:00:00',
                'schedule': 3794, 'is_approved': True
            }
        ]
    }
    # Expect 2 deleted: last shift and the one in the middle
    assert abc_duty_mock.abc_duty_storage.deleted_shifts == [2, 3]

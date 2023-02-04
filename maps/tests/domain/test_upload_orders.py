from unittest import mock

import pytest
from smb.common.http_client import BaseHttpClientException
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.booking_yang.server.lib.domains.exceptions import (
    OrgNotFound,
    OrgWithoutPhone,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    # Wednesday, 14:00 Europe/Moscow
    pytest.mark.freeze_time(dt("2019-12-25 11:00:00")),
]


@pytest.fixture(autouse=True)
def dm(dm):
    dm.list_pending_orders.coro.return_value = [
        {
            "id": id_,
            "permalink": 12345,
            "reservation_datetime": dt("2020-01-01 07:30:00"),
            "reservation_timezone": "Europe/Moscow",
            "person_count": 3,
            "customer_name": "Customer",
            "customer_phone": "+7 (000) 000-00-00",
            "comment": "",
            "created_at": dt("2020-11-11 11:11:11"),
        }
        for id_ in range(2)
    ]

    return dm


@pytest.fixture
def yang(yang):
    yang.create_task_suite.coro.side_effect = [
        {"id": "111", "created_at": dt("2020-11-11 11:11:11")},
        {"id": "222", "created_at": dt("2020-12-12 12:12:12")},
    ]

    return yang


async def test_creates_task(domain, dm, yang):
    await domain.upload_orders()

    assert (
        yang.create_task_suite.call_args_list[0][0][0]
        == yang.create_task_suite.call_args_list[1][0][0]
        == {
            "reservation_date": "01.01.2020",
            "reservation_time": "10:30",
            "cafe_name": "Кафе с едой",
            "customer_fio": "Customer",
            "comment": "",
            "person_cnt": "3",
            "rubric_name": "Общепит",
            "meta": {
                "ts": int(dt("2020-11-11 11:11:11").timestamp() * 1000),
                "permalink": 12345,
                "cafe_adress": "Город, Улица, 1",
                "pipeline": "maps-adv-bookings-yang",
            },
            "customer_phone": "+7 (000) 000-00-00",
            "phone1": "+7 (000) 000-00-99",
            "phone2": "+7 (495) 739-70-00",
            "phone3": "+7 (495) 739-70-22",
            "phone4": None,
            "phone5": None,
        }
    )


@pytest.mark.config(NEW_YANG_FORMAT=True)
async def test_creates_task_in_new_format(domain, dm, yang):
    await domain.upload_orders()

    assert (
        yang.create_task_suite.call_args_list[0][0][0]
        == yang.create_task_suite.call_args_list[1][0][0]
        == {
            "reservation_date": "01.01.2020",
            "reservation_time": "10:30",
            "cafe_name": "Кафе с едой",
            "customer_fio": "Customer",
            "comment": "",
            "person_cnt": "3",
            "rubric_name": "Общепит",
            "meta": {
                "ts": int(dt("2020-11-11 11:11:11").timestamp() * 1000),
                "permalink": 12345,
                "cafe_adress": "Город, Улица, 1",
                "pipeline": "maps-adv-bookings-yang",
            },
            "customer_phone": "+7 (000) 000-00-00",
            "phones": [
                "+7 (000) 000-00-99",
                "+7 (495) 739-70-00",
                "+7 (495) 739-70-22",
            ],
        }
    )


async def test_updates_order_with_created_task(domain, dm, yang):
    await domain.upload_orders()

    dm.update_orders.assert_has_calls(
        [
            mock.call(
                order_ids=[0],
                yang_suite_id="111",
                yang_task_created_at=dt("2020-11-11 11:11:11"),
                task_created_at=dt("2019-12-25 11:00:00"),
                booking_meta={
                    "org_name": "Кафе с едой",
                    "org_phone": "+7 (000) 000-00-99",
                },
                con=Any(object),
            ),
            mock.call(
                order_ids=[1],
                yang_suite_id="222",
                yang_task_created_at=dt("2020-12-12 12:12:12"),
                task_created_at=dt("2019-12-25 11:00:00"),
                booking_meta={
                    "org_name": "Кафе с едой",
                    "org_phone": "+7 (000) 000-00-99",
                },
                con=Any(object),
            ),
        ]
    )


@pytest.mark.parametrize(
    "open_hours",
    [
        [
            # Wednesday, 8-18
            (201600, 237600)
        ],
        [
            # Wednesday, all day
            (201600, 259200)
        ],
        [
            # Monday, Wednesday, Friday 8-18
            (28800, 64800),
            (201600, 237600),
            (374400, 410400),
        ],
        [
            # Wednesday, 9-11, 13-15, 17-19
            (205200, 212400),
            (219600, 226800),
            (234000, 241200),
        ],
        [
            # Monday 8-11, Wednesday 8-18
            (28800, 39600),
            (201600, 237600),
        ],
        [
            # Tuesday 22 - Wednesday 18
            (165600, 237600)
        ],
    ],
)
async def test_creates_task_during_call_center_and_org_working_hours(
    domain, geosearch, yang, open_hours
):
    geosearch.resolve_org.coro.return_value.open_hours = open_hours

    await domain.upload_orders()

    yang.create_task_suite.assert_called()


async def test_does_not_update_order_on_failure(domain, dm, yang):
    yang.create_task_suite.coro.side_effect = [
        BaseHttpClientException(),
        {"id": "222", "created_at": dt("2020-12-12 12:12:12")},
    ]

    await domain.upload_orders()

    dm.update_orders.assert_called_once_with(
        order_ids=[1],
        yang_suite_id="222",
        yang_task_created_at=dt("2020-12-12 12:12:12"),
        task_created_at=dt("2019-12-25 11:00:00"),
        booking_meta={"org_name": "Кафе с едой", "org_phone": "+7 (000) 000-00-99"},
        con=Any(object),
    )


async def test_does_not_create_task_if_org_not_resolved(domain, geosearch, dm, yang):
    dm.list_pending_orders.coro.return_value = [
        {
            "id": 1,
            "permalink": 12345,
            "reservation_datetime": dt("2020-01-01 07:30:00"),
            "reservation_timezone": "Europe/Moscow",
            "person_count": 3,
            "customer_name": "Customer",
            "customer_phone": "+7 (000) 000-00-00",
            "comment": "",
            "created_at": dt("2020-11-11 11:11:11"),
        }
    ]
    geosearch.resolve_org.coro.return_value = None

    with pytest.raises(OrgNotFound):
        await domain.upload_orders()

    yang.create_task_suite.assert_not_called()


async def test_does_not_create_task_if_org_has_no_phones(
    domain, geosearch, geoproduct, dm, yang
):
    dm.list_pending_orders.coro.return_value = [
        {
            "id": 1,
            "permalink": 12345,
            "reservation_datetime": dt("2020-01-01 07:30:00"),
            "reservation_timezone": "Europe/Moscow",
            "person_count": 3,
            "customer_name": "Customer",
            "customer_phone": "+7 (000) 000-00-00",
            "comment": "",
            "created_at": dt("2020-11-11 11:11:11"),
        }
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geoproduct.list_reservations.coro.return_value = []

    with pytest.raises(OrgWithoutPhone):
        await domain.upload_orders()

    yang.create_task_suite.assert_not_called()


@pytest.mark.parametrize("exception_cls", [BaseHttpClientException, Exception])
async def test_does_not_create_task_on_geosearch_error(
    domain, geosearch, yang, exception_cls
):
    geosearch.resolve_org.coro.side_effect = exception_cls()

    with pytest.raises(exception_cls):
        await domain.upload_orders()

    yang.create_task_suite.assert_not_called()


@pytest.mark.parametrize("exception_cls", [BaseHttpClientException, Exception])
async def test_does_not_create_task_on_geoproduct_error(
    domain, geoproduct, yang, exception_cls
):
    geoproduct.list_reservations.coro.side_effect = exception_cls()

    with pytest.raises(exception_cls):
        await domain.upload_orders()

    yang.create_task_suite.assert_not_called()

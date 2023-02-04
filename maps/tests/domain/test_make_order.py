import asyncio
from datetime import datetime, timezone
from unittest import mock

import pytest
from smb.common.http_client import BaseHttpClientException
from smb.common.testing_utils import Any, dt

from maps_adv.common.helpers import AsyncContextManagerMock
from maps_adv.geosmb.booking_yang.server.lib.domains.exceptions import (
    InsufficientTimeToConfirm,
    OrgWithoutPhone,
)
from maps_adv.geosmb.doorman.client import OrderEvent, Source

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.freeze_time(dt("2019-12-25 11:00:00")),
]


def make_order_params(**overrides):
    return {
        "permalink": 12345,
        "reservation_datetime": dt("2020-01-01 13:00:00"),
        "reservation_timezone": "Europe/Moscow",
        "person_count": 3,
        "customer_name": "Customer",
        "customer_phone": "+7 (000) 000-00-00",
        "customer_passport_uid": 67893,
        "comment": "",
        "call_agreement_accepted": True,
        "biz_id": 123,
        **overrides,
    }


def make_task_params(**overrides):
    return {
        "cafe_name": "Кафе с едой",
        "comment": "",
        "customer_fio": "Customer",
        "customer_phone": "+7 (000) 000-00-00",
        "meta": {
            "cafe_adress": "Город, Улица, 1",
            "permalink": 12345,
            "pipeline": "maps-adv-bookings-yang",
            "ts": int(datetime.now(timezone.utc).timestamp() * 1000),
        },
        "person_cnt": "3",
        "reservation_date": "01.01.2020",
        "reservation_time": "16:00",
        "rubric_name": "Общепит",
        **overrides,
    }


@pytest.fixture(autouse=True)
def org_works_24_7(geosearch):
    geosearch.resolve_org.coro.return_value.open_hours = [(0, 604800)]


@pytest.mark.parametrize(
    "org_open_hours, time_to_call",
    [
        ([(0, 604800)], dt("2019-12-25 11:00:00")),
        ([(226800, 234000)], dt("2019-12-25 12:00:00")),
    ],
)
async def test_returns_expected_time_to_call(
    org_open_hours, time_to_call, domain, dm, geosearch
):
    geosearch.resolve_org.coro.return_value.open_hours = org_open_hours

    got = await domain.make_order(**make_order_params())

    assert got == time_to_call


@pytest.mark.parametrize(
    "org_open_hours, time_to_call",
    [
        ([(0, 604800)], dt("2019-12-25 11:00:00")),
        ([(226800, 234000)], dt("2019-12-25 12:00:00")),
    ],
)
async def test_creates_order(org_open_hours, time_to_call, domain, dm, geosearch):
    geosearch.resolve_org.coro.return_value.open_hours = org_open_hours

    await domain.make_order(**make_order_params())

    dm.create_order.assert_called_with(
        permalink=12345,
        reservation_datetime=dt("2020-01-01 13:00:00"),
        reservation_timezone="Europe/Moscow",
        person_count=3,
        customer_name="Customer",
        customer_phone="+7 (000) 000-00-00",
        customer_passport_uid=67893,
        comment="",
        call_agreement_accepted=True,
        biz_id=123,
        time_to_call=time_to_call,
        created_at=datetime.now(timezone.utc),
    )


async def test_creates_task(domain, geosearch, yang):
    await domain.make_order(**make_order_params())

    await asyncio.sleep(0.1)

    yang.create_task_suite.assert_called_with(
        make_task_params(
            phone1="+7 (000) 000-00-99",
            phone2="+7 (495) 739-70-00",
            phone3="+7 (495) 739-70-22",
            phone4=None,
            phone5=None,
        )
    )


@pytest.mark.config(NEW_YANG_FORMAT=True)
async def test_creates_task_in_new_format(domain, yang):
    await domain.make_order(**make_order_params())

    await asyncio.sleep(0.1)

    yang.create_task_suite.assert_called_with(
        make_task_params(
            phones=["+7 (000) 000-00-99", "+7 (495) 739-70-00", "+7 (495) 739-70-22"]
        )
    )


async def test_does_not_create_task_if_time_to_call_gt_now(domain, dm, geosearch, yang):
    geosearch.resolve_org.coro.return_value.open_hours = [(226800, 234000)]

    await domain.make_order(**make_order_params())

    yang.create_task_suite.assert_not_called()


async def test_updates_order_with_yang_task_details(domain, dm, yang):
    yang.create_task_suite.coro.return_value = {
        "id": "67890",
        "created_at": dt("2020-01-01 00:00:00"),
    }

    await domain.make_order(**make_order_params())

    await asyncio.sleep(0.1)

    dm.update_orders.assert_called_with(
        order_ids=[Any(int)],
        yang_suite_id="67890",
        yang_task_created_at=dt("2020-01-01 00:00:00"),
        task_created_at=dt("2019-12-25 11:00:00"),
        booking_meta={"org_name": "Кафе с едой", "org_phone": "+7 (000) 000-00-99"},
        con=Any(object),
    )


async def test_does_not_update_order_with_yang_task_details_if_time_to_call_gt_now(
    domain, dm, yang, geosearch
):
    geosearch.resolve_org.coro.return_value.open_hours = [(226800, 234000)]

    await domain.make_order(**make_order_params())

    dm.update_orders.assert_not_called()


async def test_raises_if_insufficient_time_to_call(domain, dm, yang):
    with pytest.raises(InsufficientTimeToConfirm):
        await domain.make_order(
            **make_order_params(reservation_datetime=dt("2019-12-25 11:30:00"))
        )


async def test_creates_order_with_none_as_time_to_call_if_insufficient_time_to_call(
    domain, dm, yang
):
    try:
        await domain.make_order(
            **make_order_params(reservation_datetime=dt("2019-12-25 11:30:00"))
        )
    except Exception:
        pass

    dm.create_order.assert_called_with(
        permalink=12345,
        reservation_datetime=dt("2019-12-25 11:30:00"),
        reservation_timezone="Europe/Moscow",
        person_count=3,
        customer_name="Customer",
        customer_phone="+7 (000) 000-00-00",
        customer_passport_uid=67893,
        comment="",
        call_agreement_accepted=True,
        biz_id=123,
        time_to_call=None,
        created_at=datetime.now(timezone.utc),
    )


async def test_does_not_create_task_if_insufficient_time_to_call(domain, yang):
    try:
        await domain.make_order(
            **make_order_params(reservation_datetime=dt("2019-12-25 11:30:00"))
        )
    except Exception:
        pass

    yang.create_task_suite.assert_not_called()


@pytest.mark.parametrize("exception", [BaseHttpClientException(), Exception()])
async def test_creates_order_on_yang_error(exception, domain, dm, yang):
    yang.create_task_suite.coro.side_effect = exception

    try:
        await domain.make_order(**make_order_params())
    except Exception:
        pass

    dm.create_order.assert_called_once()


@pytest.mark.parametrize("exception", [BaseHttpClientException(), Exception()])
async def test_does_not_update_order_with_task_details_on_yang_error(
    domain, dm, yang, exception
):
    yang.create_task_suite.coro.side_effect = exception

    try:
        await domain.make_order(**make_order_params())
    except Exception:
        pass

    dm.update_orders.assert_not_called()


@pytest.mark.parametrize(
    "reservation_datetime", [dt("2019-12-25 11:30:00"), dt("2020-01-01 15:00:00")]
)
async def test_raises_if_org_is_resolved_without_phone(
    reservation_datetime, domain, geoproduct, geosearch
):
    geoproduct.list_reservations.coro.return_value = []
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []

    with pytest.raises(OrgWithoutPhone):
        await domain.make_order(
            **make_order_params(reservation_datetime=reservation_datetime)
        )


@pytest.mark.parametrize(
    "reservation_datetime", [dt("2019-12-25 11:30:00"), dt("2020-01-01 15:00:00")]
)
@pytest.mark.parametrize("exception_cls", [BaseHttpClientException, Exception])
async def test_does_not_create_order_on_geosearch_error(
    domain, dm, geosearch, exception_cls, reservation_datetime
):
    geosearch.resolve_org.coro.side_effect = exception_cls("Client exception")

    with pytest.raises(exception_cls, match="Client exception"):
        await domain.make_order(
            **make_order_params(reservation_datetime=reservation_datetime)
        )

    dm.create_order.assert_not_called()


@pytest.mark.parametrize(
    "reservation_datetime", [dt("2019-12-25 11:30:00"), dt("2020-01-01 15:00:00")]
)
@pytest.mark.parametrize("exception_cls", [BaseHttpClientException, Exception])
async def test_does_not_create_order_on_geoproduct_error(
    domain, dm, geoproduct, exception_cls, reservation_datetime
):
    geoproduct.list_reservations.coro.side_effect = exception_cls("Client exception")

    with pytest.raises(exception_cls, match="Client exception"):
        await domain.make_order(
            **make_order_params(reservation_datetime=reservation_datetime)
        )

    dm.create_order.assert_not_called()


@pytest.mark.parametrize(
    "reservation_datetime", [dt("2019-12-25 11:30:00"), dt("2020-01-01 15:00:00")]
)
@pytest.mark.parametrize("exception_cls", [BaseHttpClientException, Exception])
async def test_does_not_create_task_on_geosearch_error(
    domain, geosearch, yang, exception_cls, reservation_datetime
):
    geosearch.resolve_org.coro.side_effect = exception_cls()

    with pytest.raises(exception_cls):
        await domain.make_order(
            **make_order_params(reservation_datetime=reservation_datetime)
        )

    yang.create_task_suite.assert_not_called()


@pytest.mark.parametrize(
    "reservation_datetime", [dt("2019-12-25 11:30:00"), dt("2020-01-01 15:00:00")]
)
@pytest.mark.parametrize("exception_cls", [BaseHttpClientException, Exception])
async def test_does_not_create_task_on_geoproduct_error(
    domain, geoproduct, yang, exception_cls, reservation_datetime
):
    geoproduct.list_reservations.coro.side_effect = exception_cls()

    with pytest.raises(exception_cls):
        await domain.make_order(
            **make_order_params(reservation_datetime=reservation_datetime)
        )

    yang.create_task_suite.assert_not_called()


@pytest.mark.parametrize(
    "geoproduct_reservations, geosearch_phones, task_phones",
    [
        ([{"data": {"phone_number": "001"}}], [], ["001", None, None, None, None]),
        ([], ["001"], ["001", None, None, None, None]),
        (
            [{"data": {"phone_number": "001"}}],
            ["002"],
            ["001", "002", None, None, None],
        ),
        (
            [],
            ["001", "002", "003", "004", "005", "006"],
            ["001", "002", "003", "004", "005"],
        ),
        (
            [{"data": {"phone_number": "001"}}],
            ["002", "003", "004", "005", "006", "007"],
            ["001", "002", "003", "004", "005"],
        ),
        (
            [{"data": {"phone_number": "001"}}],
            ["001", "002"],
            ["001", "002", None, None, None],
        ),
        (
            [{"data": {"phone_number": "002"}}],
            ["001"],
            ["002", "001", None, None, None],
        ),  # reservation phones are always first
    ],
)
async def test_creates_task_with_valid_phones(
    geoproduct_reservations,
    geosearch_phones,
    task_phones,
    domain,
    geoproduct,
    geosearch,
    yang,
):
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = geosearch_phones
    geoproduct.list_reservations.coro.return_value = geoproduct_reservations

    await domain.make_order(**make_order_params())

    await asyncio.sleep(0.1)

    yang.create_task_suite.assert_called_with(
        make_task_params(
            **{"phone{}".format(ind): phone for ind, phone in enumerate(task_phones, 1)}
        )
    )


@pytest.mark.parametrize(
    "geoproduct_reservations, geosearch_phones, task_phones",
    [
        ([{"data": {"phone_number": "001"}}], [], ["001"]),
        ([], ["001"], ["001"]),
        ([{"data": {"phone_number": "001"}}], ["002"], ["001", "002"]),
        (
            [],
            ["001", "002", "003", "004", "005", "006"],
            ["001", "002", "003", "004", "005", "006"],
        ),
        (
            [{"data": {"phone_number": "001"}}],
            ["002", "003", "004", "005", "006", "007"],
            ["001", "002", "003", "004", "005", "006", "007"],
        ),
        ([{"data": {"phone_number": "001"}}], ["001", "002"], ["001", "002"]),
        (
            [{"data": {"phone_number": "002"}}],
            ["001"],
            ["002", "001"],
        ),  # reservation phones are always first
    ],
)
@pytest.mark.config(NEW_YANG_FORMAT=True)
async def test_creates_task_with_valid_phones_in_new_format(
    geoproduct_reservations,
    geosearch_phones,
    task_phones,
    domain,
    geoproduct,
    geosearch,
    yang,
):
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = geosearch_phones
    geoproduct.list_reservations.coro.return_value = geoproduct_reservations

    await domain.make_order(**make_order_params())

    await asyncio.sleep(0.1)

    yang.create_task_suite.assert_called_with(make_task_params(phones=task_phones))


@pytest.mark.parametrize("org_open_hours", [[(0, 604800)], [(226800, 234000)]])
@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_creates_client_in_doorman(org_open_hours, doorman, domain, geosearch):
    geosearch.resolve_org.coro.return_value.open_hours = org_open_hours

    await domain.make_order(**make_order_params())

    await asyncio.sleep(0.1)

    doorman.create_client.assert_called_with(
        biz_id=123,
        source=Source.BOOKING_YANG,
        phone=70000000000,
        first_name="Customer",
        passport_uid=67893,
    )


@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_creates_client_in_doorman_if_insufficient_time_to_call(doorman, domain):
    try:
        await domain.make_order(
            **make_order_params(reservation_datetime=dt("2020-01-01 15:00:00"))
        )
    except Exception:
        pass

    await asyncio.sleep(0.1)

    doorman.create_client.assert_called_with(
        biz_id=123,
        source=Source.BOOKING_YANG,
        phone=70000000000,
        first_name="Customer",
        passport_uid=67893,
    )


@pytest.mark.parametrize("org_open_hours", [[(0, 604800)], [(226800, 234000)]])
@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_saves_doorman_client_id(org_open_hours, domain, dm, geosearch):
    geosearch.resolve_org.coro.return_value.open_hours = org_open_hours
    dm.create_order.coro.return_value = 1234567

    await domain.make_order(**make_order_params())

    await asyncio.sleep(0.1)

    dm.update_orders.assert_any_call(
        con=Any(AsyncContextManagerMock), order_ids=[1234567], client_id=100500
    )


@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_saves_doorman_client_id_if_insufficient_time_to_call(domain, dm):
    dm.create_order.coro.return_value = 1234567

    try:
        await domain.make_order(
            **make_order_params(reservation_datetime=dt("2019-12-25 11:30:00"))
        )
    except Exception:
        pass

    await asyncio.sleep(0.1)

    dm.update_orders.assert_any_call(
        con=Any(AsyncContextManagerMock),
        order_ids=[1234567],
        client_id=100500,
    )


@pytest.mark.parametrize("org_open_hours", [[(0, 604800)], [(226800, 234000)]])
@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_adds_event_about_newly_created_order_in_doorman(
    org_open_hours, doorman, domain, dm, geosearch
):
    geosearch.resolve_org.coro.return_value.open_hours = org_open_hours
    dm.create_order.coro.return_value = 1234567

    await domain.make_order(**make_order_params())
    await asyncio.sleep(0.1)

    doorman.add_order_event.assert_called_with(
        biz_id=123,
        client_id=100500,
        event_type=OrderEvent.CREATED,
        event_timestamp=Any(datetime),
        source=Source.BOOKING_YANG,
        order_id=1234567,
    )


@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_adds_events_about_order_in_doorman_if_insufficient_time_to_call(
    doorman, domain, dm
):
    dm.create_order.coro.return_value = 1234567
    try:
        await domain.make_order(
            **make_order_params(reservation_datetime=dt("2019-12-25 11:30:00"))
        )
    except Exception:
        pass

    await asyncio.sleep(0.1)

    doorman.add_order_event.assert_has_calls(
        [
            mock.call(
                biz_id=123,
                client_id=100500,
                event_type=OrderEvent.CREATED,
                event_timestamp=Any(datetime),
                source=Source.BOOKING_YANG,
                order_id=1234567,
            ),
            mock.call(
                biz_id=123,
                client_id=100500,
                event_type=OrderEvent.REJECTED,
                event_timestamp=Any(datetime),
                source=Source.BOOKING_YANG,
                order_id=1234567,
            ),
        ]
    )


async def test_does_not_create_client_in_doorman_if_not_configured(doorman, domain):
    await domain.make_order(**make_order_params())
    await asyncio.sleep(0.1)

    assert doorman.create_client.called is False


async def test_does_not_add_event_about_created_order_in_doorman_if_not_configured(
    doorman, domain, dm
):
    dm.create_order.coro.return_value = 1234567

    await domain.make_order(**make_order_params())

    await asyncio.sleep(0.1)

    assert doorman.add_order_event.called is False

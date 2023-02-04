import asyncio
from datetime import datetime, timezone
from unittest import mock

import pytest
from smb.common.http_client import BaseHttpClientException
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.booking_yang.proto import errors_pb2, orders_pb2
from maps_adv.geosmb.booking_yang.server.tests.utils import make_pb_order_input
from maps_adv.geosmb.doorman.client import OrderEvent, Source

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.real_db,
    # Wednesday, 14:00 Europe/Moscow
    pytest.mark.freeze_time(dt("2019-12-25 11:00:00")),
]

url = "/v1/orders/"


@pytest.mark.parametrize(
    "org_open_hours, time_to_call",
    [
        ([(0, 604800)], dt("2019-12-25 11:00:00", as_proto=True)),
        ([(226800, 234000)], dt("2019-12-25 12:00:00", as_proto=True)),
    ],
)
async def test_returns_expected_time_to_call(
    org_open_hours, time_to_call, api, geosearch
):
    geosearch.resolve_org.coro.return_value.open_hours = org_open_hours

    got = await api.post(
        url,
        proto=make_pb_order_input(),
        decode_as=orders_pb2.OrderOutput,
        expected_status=201,
    )

    assert got == orders_pb2.OrderOutput(processing_time=time_to_call)


@pytest.mark.parametrize("customer_passport_uid", [654322, None])
async def test_creates_order(customer_passport_uid, api, con):
    await api.post(
        url,
        proto=make_pb_order_input(customer_passport_uid=customer_passport_uid),
        expected_status=201,
    )

    await asyncio.sleep(0.1)

    row = dict(await con.fetchrow("SELECT * FROM orders"))
    assert row == {
        "id": Any(int),
        "call_agreement_accepted": True,
        "comment": "Комментарий",
        "customer_name": "Клиент",
        "customer_phone": "+7 (000) 000-00-00",
        "customer_passport_uid": customer_passport_uid,
        "permalink": 12345,
        "person_count": 3,
        "reservation_datetime": dt("2020-01-01 13:00:00"),
        "reservation_timezone": "Europe/Moscow",
        "yang_suite_id": "suite_id_1",
        "client_id": None,
        "biz_id": 123,
        "time_to_call": dt("2019-12-25 11:00:00"),
        "yang_task_created_at": dt("2019-12-25 11:00:00"),
        "task_created_at": dt("2019-12-25 11:00:00"),
        "task_result_got_at": None,
        "sms_sent_at": None,
        "sent_result_event_at": None,
        "exported_as_created_at": None,
        "exported_as_processed_at": None,
        "exported_as_notified_at": None,
        "booking_verdict": None,
        "created_at": dt("2019-12-25 11:00:00"),
        "booking_meta": {"org_name": "Название", "org_phone": "+7 (000) 000-00-99"},
    }


async def test_creates_order_if_time_to_call_gt_now(api, geosearch, con):
    geosearch.resolve_org.coro.return_value.open_hours = [(226800, 234000)]

    await api.post(url, proto=make_pb_order_input(), expected_status=201)

    row = dict(await con.fetchrow("SELECT * FROM orders"))
    assert row == {
        "id": Any(int),
        "call_agreement_accepted": True,
        "comment": "Комментарий",
        "customer_name": "Клиент",
        "customer_phone": "+7 (000) 000-00-00",
        "customer_passport_uid": 65432,
        "permalink": 12345,
        "person_count": 3,
        "reservation_datetime": dt("2020-01-01 13:00:00"),
        "reservation_timezone": "Europe/Moscow",
        "yang_suite_id": None,
        "client_id": None,
        "biz_id": 123,
        "time_to_call": dt("2019-12-25 12:00:00"),
        "yang_task_created_at": None,
        "task_created_at": None,
        "task_result_got_at": None,
        "sms_sent_at": None,
        "sent_result_event_at": None,
        "exported_as_created_at": None,
        "exported_as_processed_at": None,
        "exported_as_notified_at": None,
        "booking_verdict": None,
        "created_at": dt("2019-12-25 11:00:00"),
        "booking_meta": None,
    }


async def test_creates_yang_task(yang_mock, api):
    await api.post(url, proto=make_pb_order_input(), expected_status=201)

    await asyncio.sleep(0.1)

    yang_mock.create_task_suite.assert_called_with(
        {
            "reservation_date": "01.01.2020",
            "reservation_time": "16:00",
            "cafe_name": "Название",
            "customer_fio": "Клиент",
            "comment": "Комментарий",
            "person_cnt": "3",
            "rubric_name": "Кафе",
            "meta": {
                "ts": int(datetime.now(timezone.utc).timestamp() * 1000),
                "permalink": 12345,
                "cafe_adress": "Город, Улица, 1",
                "pipeline": "maps-adv-bookings-yang",
            },
            "customer_phone": "+7 (000) 000-00-00",
            "phone1": "+7 (000) 000-00-99",
            "phone2": "+7 (000) 000-00-01",
            "phone3": "+7 (000) 000-00-02",
            "phone4": None,
            "phone5": None,
        }
    )


@pytest.mark.config(NEW_YANG_FORMAT=True)
async def test_creates_yang_task_in_new_format(yang_mock, api, con):
    await api.post(url, proto=make_pb_order_input(), expected_status=201)

    await asyncio.sleep(0.1)

    yang_mock.create_task_suite.assert_called_with(
        {
            "reservation_date": "01.01.2020",
            "reservation_time": "16:00",
            "cafe_name": "Название",
            "customer_fio": "Клиент",
            "comment": "Комментарий",
            "person_cnt": "3",
            "rubric_name": "Кафе",
            "meta": {
                "ts": int(datetime.now(timezone.utc).timestamp() * 1000),
                "permalink": 12345,
                "cafe_adress": "Город, Улица, 1",
                "pipeline": "maps-adv-bookings-yang",
            },
            "customer_phone": "+7 (000) 000-00-00",
            "phones": [
                "+7 (000) 000-00-99",
                "+7 (000) 000-00-01",
                "+7 (000) 000-00-02",
            ],
        }
    )


async def test_does_not_create_task_if_time_to_call_gt_now(api, geosearch, yang_mock):
    geosearch.resolve_org.coro.return_value.open_hours = [(226800, 234000)]

    await api.post(url, proto=make_pb_order_input(), expected_status=201)

    yang_mock.create_task_suite.assert_not_called()


async def test_requests_geoproduct_for_details(geosearch, api):
    await api.post(url, proto=make_pb_order_input(), expected_status=201)

    assert geosearch.resolve_org.called


async def test_requests_geosearch_for_details(geosearch, api):
    await api.post(url, proto=make_pb_order_input(), expected_status=201)

    assert geosearch.resolve_org.called


async def test_errored_if_insufficient_time_to_call(api):
    got = await api.post(
        url,
        proto=make_pb_order_input(
            reservation_datetime=dt("2019-12-25 11:30:00", as_proto=True)
        ),
        decode_as=errors_pb2.Error,
        expected_status=409,
    )

    assert got == errors_pb2.Error(code=errors_pb2.Error.INSUFFICIENT_TIME_TO_CONFIRM)


async def test_creates_order_if_insufficient_time_to_call(api, con):
    await api.post(
        url,
        proto=make_pb_order_input(
            reservation_datetime=dt("2019-12-25 11:30:00", as_proto=True)
        ),
        expected_status=409,
    )

    row = dict(await con.fetchrow("SELECT * FROM orders"))
    assert row == {
        "id": Any(int),
        "call_agreement_accepted": True,
        "comment": "Комментарий",
        "customer_name": "Клиент",
        "customer_phone": "+7 (000) 000-00-00",
        "customer_passport_uid": 65432,
        "permalink": 12345,
        "person_count": 3,
        "reservation_datetime": dt("2019-12-25 11:30:00"),
        "reservation_timezone": "Europe/Moscow",
        "yang_suite_id": None,
        "client_id": None,
        "biz_id": 123,
        "time_to_call": None,
        "yang_task_created_at": None,
        "task_created_at": None,
        "task_result_got_at": None,
        "sms_sent_at": None,
        "sent_result_event_at": None,
        "exported_as_created_at": None,
        "exported_as_processed_at": None,
        "exported_as_notified_at": None,
        "booking_verdict": None,
        "created_at": Any(datetime),
        "booking_meta": None,
    }


async def test_does_not_create_yang_task_if_insufficient_time_to_call(api, yang_mock):
    await api.post(
        url,
        proto=make_pb_order_input(
            reservation_datetime=dt("2019-12-25 11:30:00", as_proto=True)
        ),
    )

    yang_mock.create_task_suite.assert_not_called()


async def test_saved_for_future_processing_if_yang_errored(yang_mock, api, con):
    yang_mock.create_task_suite.coro.side_effect = BaseHttpClientException()

    await api.post(url, proto=make_pb_order_input(), expected_status=201)

    row = await con.fetchval("SELECT EXISTS(SELECT 1 FROM orders)")
    assert row is True


async def test_errored_if_org_not_found(api, geosearch):
    geosearch.resolve_org.coro.return_value = None

    got = await api.post(
        url,
        proto=make_pb_order_input(),
        decode_as=errors_pb2.Error,
        expected_status=404,
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.ORG_NOT_FOUND, description="Failed to resolve org 12345."
    )


async def test_errored_if_org_resolved_without_phones(api, geosearch, geoproduct_mock):
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geoproduct_mock.list_reservations.coro.return_value = []

    got = await api.post(
        url,
        proto=make_pb_order_input(),
        decode_as=errors_pb2.Error,
        expected_status=404,
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.ORG_WITHOUT_PHONE,
        description="Organization 12345 resolves without phone",
    )


@pytest.mark.parametrize(
    "kw, err",
    (
        ({"person_count": 0}, '{"person_count": ["Must be at least 1."]}'),
        (
            {"customer_name": ""},
            '{"customer_name": ["Shorter than minimum length 1."]}',
        ),
        (
            {"customer_phone": ""},
            '{"customer_phone": ["Shorter than minimum length 1."]}',
        ),
        (
            {"customer_passport_uid": 0},
            '{"customer_passport_uid": ["Must be at least 1."]}',
        ),
    ),
)
async def test_order_input_schema_raises_on_invalid_input(kw, err, api):
    got = await api.post(
        url,
        proto=make_pb_order_input(**kw),
        decode_as=errors_pb2.Error,
        expected_status=400,
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.DATA_VALIDATION_ERROR, description=err
    )


@pytest.mark.parametrize("org_open_hours", [[(0, 604800)], [(226800, 234000)]])
@pytest.mark.config(DOORMAN_URL="http://doorman.server")
@pytest.mark.parametrize("customer_passport_uid", [None, 65432])
async def test_creates_client_in_doorman(
    customer_passport_uid, org_open_hours, doorman, geosearch, api
):
    geosearch.resolve_org.coro.return_value.open_hours = org_open_hours

    await api.post(
        url,
        proto=make_pb_order_input(
            biz_id=123, customer_passport_uid=customer_passport_uid
        ),
    )
    await asyncio.sleep(0.1)

    doorman.create_client.assert_called_with(
        biz_id=123,
        source=Source.BOOKING_YANG,
        phone=70000000000,
        first_name="Клиент",
        passport_uid=customer_passport_uid,
    )


@pytest.mark.config(DOORMAN_URL="http://doorman.server")
@pytest.mark.parametrize("customer_passport_uid", [None, 65432])
async def test_creates_client_in_doorman_if_insufficient_time_to_call(
    customer_passport_uid, doorman, api
):
    await api.post(
        url,
        proto=make_pb_order_input(
            biz_id=123,
            reservation_datetime=dt("2019-12-25 11:30:00", as_proto=True),
            customer_passport_uid=customer_passport_uid,
        ),
    )
    await asyncio.sleep(0.1)

    doorman.create_client.assert_called_with(
        biz_id=123,
        source=Source.BOOKING_YANG,
        phone=70000000000,
        first_name="Клиент",
        passport_uid=customer_passport_uid,
    )


@pytest.mark.parametrize("org_open_hours", [[(0, 604800)], [(226800, 234000)]])
@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_saves_doorman_client_id(org_open_hours, geosearch, api, con):
    geosearch.resolve_org.coro.return_value.open_hours = org_open_hours

    await api.post(url, proto=make_pb_order_input(biz_id=123))
    await asyncio.sleep(0.1)

    assert await con.fetchval("SELECT client_id FROM orders") == 100500


@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_saves_doorman_client_id_if_insufficient_time_to_call(api, con):
    await api.post(
        url,
        proto=make_pb_order_input(
            biz_id=123, reservation_datetime=dt("2019-12-25 11:30:00", as_proto=True)
        ),
    )
    await asyncio.sleep(0.1)

    assert await con.fetchval("SELECT client_id FROM orders") == 100500


@pytest.mark.parametrize("org_open_hours", [[(0, 604800)], [(226800, 234000)]])
@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_adds_event_about_newly_created_order_in_doorman(
    org_open_hours, doorman, geosearch, api, con
):
    geosearch.resolve_org.coro.return_value.open_hours = org_open_hours

    await api.post(url, proto=make_pb_order_input(biz_id=123))
    await asyncio.sleep(0.1)

    order_id = await con.fetchval("SELECT id FROM orders")
    doorman.add_order_event.assert_called_with(
        biz_id=123,
        client_id=100500,
        event_type=OrderEvent.CREATED,
        event_timestamp=Any(datetime),
        source=Source.BOOKING_YANG,
        order_id=order_id,
    )


@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_adds_events_about_order_in_doorman_if_insufficient_time_to_call(
    doorman, api, con
):
    await api.post(
        url,
        proto=make_pb_order_input(
            biz_id=123, reservation_datetime=dt("2019-12-25 11:30:00", as_proto=True)
        ),
    )
    await asyncio.sleep(0.1)

    order_id = await con.fetchval("SELECT id FROM orders")
    doorman.add_order_event.assert_has_calls(
        [
            mock.call(
                biz_id=123,
                client_id=100500,
                event_type=OrderEvent.CREATED,
                event_timestamp=Any(datetime),
                source=Source.BOOKING_YANG,
                order_id=order_id,
            ),
            mock.call(
                biz_id=123,
                client_id=100500,
                event_type=OrderEvent.REJECTED,
                event_timestamp=Any(datetime),
                source=Source.BOOKING_YANG,
                order_id=order_id,
            ),
        ]
    )


async def test_does_not_create_client_in_doorman_if_not_configured(doorman, api):
    await api.post(url, proto=make_pb_order_input(biz_id=123))
    await asyncio.sleep(0.1)

    assert doorman.create_client.called is False


async def test_does_not_add_event_about_created_order_in_doorman_if_not_configured(
    doorman, api
):
    await api.post(url, proto=make_pb_order_input(biz_id=123))
    await asyncio.sleep(0.1)

    assert doorman.add_order_event.called is False


@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_does_not_save_doorman_client_id_if_add_event_fails(doorman, api, con):
    doorman.create_client.coro.return_value = 999
    doorman.add_order_event.coro.side_effect = Exception()

    await api.post(url, proto=make_pb_order_input(biz_id=123))
    await asyncio.sleep(0.1)

    client_id = await con.fetchval("SELECT client_id FROM orders WHERE id = $1", 999)
    assert client_id is None

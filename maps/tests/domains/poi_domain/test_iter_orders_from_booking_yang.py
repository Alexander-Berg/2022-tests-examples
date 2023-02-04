import logging

import pytest
from smb.common.testing_utils import dt

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time(dt("2019-12-25 11:00:00")),
]


@pytest.fixture(autouse=True)
async def booking_yang(booking_yang):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            client_id=111,
            passport_uid=6543,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
        dict(
            client_id=222,
            passport_uid=4567,
            permalink=222222,
            reservation_datetime=dt("2020-01-25 11:00:00"),
            reservation_timezone="Europe/Kaliningrad",
        ),
        dict(
            client_id=333,
            passport_uid=9876,
            permalink=3333,
            reservation_datetime=dt("2020-01-13 12:00:00"),
            reservation_timezone="Asia/Kamchatka",
        ),
    ]

    return booking_yang


@pytest.fixture(autouse=True)
async def market_int(market_int):
    market_int.fetch_actual_orders.coro.return_value = []

    return market_int


async def test_iterator_is_empty_if_there_are_no_data(domain, booking_yang):
    booking_yang.fetch_actual_orders.coro.return_value = []

    iter_counts = 0
    async for _ in domain.iter_orders_poi_data_for_export():
        iter_counts += 1

    assert iter_counts == 0


@pytest.mark.parametrize("client_id", [None, 111])
async def test_returns_data_for_orders_with_passport(domain, booking_yang, client_id):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            client_id=client_id,
            passport_uid=6543,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
    ]

    records = []
    async for data in domain.iter_orders_poi_data_for_export():
        records.extend(data)

    assert records == [
        {
            "experiment_tag": "booking_yang",
            "passport_uid": 6543,
            "permalink": 111111,
            "subscript": [("RU", "Стол на 14:00"), ("EN", "Reservation for 14:00")],
        },
    ]


async def test_does_not_call_doorman_for_orders_with_passport(
    domain, booking_yang, doorman
):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            client_id=111,
            passport_uid=6543,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
    ]

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    doorman.list_contacts.assert_not_called()


async def test_calls_doorman_for_orders_without_passport(domain, booking_yang, doorman):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            client_id=111,
            passport_uid=6543,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
        dict(
            client_id=222,
            passport_uid=None,
            permalink=222222,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
        dict(
            client_id=333,
            permalink=333333,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
    ]

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    doorman.list_contacts.assert_called_with(client_ids={222, 333})


async def test_resolves_passport_for_orders_without_passport(
    domain, booking_yang, doorman
):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            client_id=111,
            passport_uid=None,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
    ]
    doorman.list_contacts.coro.return_value = {111: {"passport_uid": 456}}

    records = []
    async for data in domain.iter_orders_poi_data_for_export():
        records.extend(data)

    assert records == [
        {
            "experiment_tag": "booking_yang",
            "passport_uid": 456,
            "permalink": 111111,
            "subscript": [("RU", "Стол на 14:00"), ("EN", "Reservation for 14:00")],
        },
    ]


async def test_does_not_return_orders_if_client_has_no_passport(
    domain, booking_yang, doorman
):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            client_id=111,
            passport_uid=None,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
    ]
    doorman.list_contacts.coro.return_value = {
        111: {"passport_uid": None},
    }

    records = []
    async for data in domain.iter_orders_poi_data_for_export():
        records.extend(data)

    assert records == []


async def test_does_not_return_orders_for_unknown_client(domain, booking_yang, doorman):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            client_id=111,
            passport_uid=None,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
    ]
    doorman.list_contacts.coro.return_value = dict()

    records = []
    async for data in domain.iter_orders_poi_data_for_export():
        records.extend(data)

    assert records == []


async def test_logs_failed_doorman(domain, booking_yang, doorman, caplog):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            client_id=111,
            passport_uid=None,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
    ]
    doorman.list_contacts.coro.side_effect = Exception("boom!")

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert [error.message for error in errors] == [
        "Unhandled exception while fetching data from doorman"
    ]


async def test_logs_unknown_clients(domain, booking_yang, doorman, caplog):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            client_id=111,
            passport_uid=None,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
    ]
    doorman.list_contacts.coro.return_value = dict()

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert [error.message for error in errors] == [
        "Failed resolve passports for unknown client_ids={111}."
    ]


@pytest.mark.parametrize(
    "reservation_datetime, expected_ru_subscript, expected_en_subscript",
    [
        # reservation for today
        ("2019-12-25 11:00:00", "Стол на 11:00", "Reservation for 11:00"),
        # if reservation not for today -> add date
        (
            "2020-01-01 01:00:00",
            "Стол на 01:00, 1 янв.",
            "Reservation for 01:00, 1 Jan",
        ),
        (
            "2020-02-02 02:02:00",
            "Стол на 02:02, 2 февр.",
            "Reservation for 02:02, 2 Feb",
        ),
        (
            "2020-03-12 03:03:03",
            "Стол на 03:03, 12 мар.",
            "Reservation for 03:03, 12 Mar",
        ),
        (
            "2020-04-15 04:20:00",
            "Стол на 04:20, 15 апр.",
            "Reservation for 04:20, 15 Apr",
        ),
        (
            "2020-05-20 12:30:00",
            "Стол на 12:30, 20 мая",
            "Reservation for 12:30, 20 May",
        ),
        (
            "2020-06-21 12:45:45",
            "Стол на 12:45, 21 июн.",
            "Reservation for 12:45, 21 Jun",
        ),
        (
            "2020-07-22 20:00:00",
            "Стол на 20:00, 22 июл.",
            "Reservation for 20:00, 22 Jul",
        ),
        (
            "2020-08-23 21:00:00",
            "Стол на 21:00, 23 авг.",
            "Reservation for 21:00, 23 Aug",
        ),
        (
            "2020-09-24 22:00:00",
            "Стол на 22:00, 24 сент.",
            "Reservation for 22:00, 24 Sep",
        ),
        (
            "2020-10-25 23:00:00",
            "Стол на 23:00, 25 окт.",
            "Reservation for 23:00, 25 Oct",
        ),
        (
            "2020-11-30 23:30:00",
            "Стол на 23:30, 30 нояб.",
            "Reservation for 23:30, 30 Nov",
        ),
        (
            "2020-12-31 23:59:59",
            "Стол на 23:59, 31 дек.",
            "Reservation for 23:59, 31 Dec",
        ),
    ],
)
async def test_formats_subscript_as_expected(
    reservation_datetime,
    expected_ru_subscript,
    expected_en_subscript,
    domain,
    booking_yang,
):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            permalink=111111,
            passport_uid=111,
            reservation_datetime=dt(reservation_datetime),
            reservation_timezone="Etc/UTC",
        )
    ]

    result = []
    async for data in domain.iter_orders_poi_data_for_export():
        result.extend(data)

    assert result[0]["subscript"] == [
        ("RU", expected_ru_subscript),
        ("EN", expected_en_subscript),
    ]


@pytest.mark.parametrize(
    "reservation_timezone, expected_ru_subscript, expected_en_subscript",
    [
        ("Europe/Kaliningrad", "Стол на 15:00", "Reservation for 15:00"),
        ("Europe/Moscow", "Стол на 16:00", "Reservation for 16:00"),
        ("Asia/Kamchatka", "Стол на 01:00", "Reservation for 01:00"),
    ],
)
async def test_formats_subscript_time_as_expected(
    reservation_timezone,
    expected_ru_subscript,
    expected_en_subscript,
    domain,
    booking_yang,
):
    booking_yang.fetch_actual_orders.coro.return_value = [
        dict(
            permalink=111111,
            passport_uid=111,
            reservation_datetime=dt("2019-12-25 13:00:00"),
            reservation_timezone=reservation_timezone,
        )
    ]

    result = []
    async for data in domain.iter_orders_poi_data_for_export():
        result.extend(data)

    assert result[0]["subscript"] == [
        ("RU", expected_ru_subscript),
        ("EN", expected_en_subscript),
    ]


@pytest.mark.parametrize(
    "iter_size, expected_chunk_sizes",
    [(None, [3]), (0, [3]), (1, [1, 1, 1]), (2, [2, 1]), (100, [3])],
)
async def test_chunks_data_by_requested_size(
    domain, booking_yang, iter_size, expected_chunk_sizes
):
    chunk_sizes = []
    async for data_chunk in domain.iter_orders_poi_data_for_export(iter_size=iter_size):
        chunk_sizes.append(len(data_chunk))

    assert chunk_sizes == expected_chunk_sizes


@pytest.mark.parametrize("iter_size", [None, 0, 1, 2, 100])
async def test_returns_all_records_eventually(domain, booking_yang, iter_size):
    records = []
    async for data in domain.iter_orders_poi_data_for_export(iter_size=iter_size):
        records.extend(data)

    assert [record["passport_uid"] for record in records] == [6543, 4567, 9876]


async def test_iterator_is_empty_if_booking_yang_fails(domain, booking_yang, caplog):
    caplog.set_level(logging.ERROR)
    booking_yang.fetch_actual_orders.side_effect = Exception("Boom!")

    iter_counts = 0
    async for _ in domain.iter_orders_poi_data_for_export():
        iter_counts += 1

    assert iter_counts == 0


async def test_logs_booking_yang_exceptions(domain, booking_yang, caplog):
    caplog.set_level(logging.ERROR)
    booking_yang.fetch_actual_orders.side_effect = Exception("Boom!")

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    assert (
        "Unhandled exception while fetching data from booking_yang" in caplog.messages
    )

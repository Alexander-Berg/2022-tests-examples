from datetime import timedelta
from unittest import mock

import pytest
from smb.common.testing_utils import dt

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time(dt("2019-12-25 11:00:00")),
]


class GeoSearchOrgResultMock:
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)


@pytest.fixture(autouse=True)
async def booking_yang(booking_yang):
    booking_yang.fetch_actual_orders.coro.return_value = []

    return booking_yang


@pytest.fixture(autouse=True)
async def market_int(market_int):
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt("2019-12-25 11:00:00"),
        },
        {
            "client_id": 22,
            "biz_id": 222,
            "reservation_datetime": dt("2020-01-01 12:00:00"),
        },
    ]

    return market_int


@pytest.fixture(autouse=True)
async def doorman(doorman):
    doorman.list_contacts.coro.return_value = {
        11: {"passport_uid": 123},
        22: {"passport_uid": 456},
    }

    return doorman


@pytest.fixture(autouse=True)
async def bvm(bvm):
    bvm.fetch_permalinks_by_biz_ids.coro.return_value = {111: [111111], 222: [222222]}

    return bvm


async def test_returns_data_in_expected_format(domain):
    records = []
    async for data in domain.iter_orders_poi_data_for_export():
        records.extend(data)

    assert records == [
        {
            "experiment_tag": "market_int",
            "passport_uid": 123,
            "permalink": 111111,
            "subscript": [("RU", "Запись на 14:00"), ("EN", "Appointment at 14:00")],
        },
        {
            "experiment_tag": "market_int",
            "passport_uid": 456,
            "permalink": 222222,
            "subscript": [
                ("RU", "Запись на 15:00, 1 янв."),
                ("EN", "Appointment at 15:00, 1 Jan"),
            ],
        },
    ]


async def test_iterator_is_empty_if_there_are_no_data(domain, market_int):
    market_int.fetch_actual_orders.coro.return_value = []

    iter_counts = 0
    async for _ in domain.iter_orders_poi_data_for_export():
        iter_counts += 1

    assert iter_counts == 0


async def test_iterator_is_empty_if_market_fails(domain, market_int):
    market_int.fetch_actual_orders.side_effect = Exception("Boom!")

    iter_counts = 0
    async for _ in domain.iter_orders_poi_data_for_export():
        iter_counts += 1

    assert iter_counts == 0


async def test_logs_market_exceptions(domain, market_int, caplog):
    market_int.fetch_actual_orders.side_effect = Exception("Boom!")

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert [error.message for error in errors] == [
        "Unhandled exception while fetching data from market-int"
    ]


async def test_calls_doorman(domain, market_int, doorman):
    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    doorman.list_contacts.assert_called_with(client_ids={11, 22})


async def test_iterator_is_empty_if_doorman_fails(domain, doorman):
    doorman.list_contacts.coro.side_effect = Exception("boom!")

    iter_counts = 0
    async for _ in domain.iter_orders_poi_data_for_export():
        iter_counts += 1

    assert iter_counts == 0


async def test_logs_doorman_exceptions(domain, market_int, doorman, caplog):
    doorman.list_contacts.coro.side_effect = Exception("boom!")

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert [error.message for error in errors] == [
        "Unhandled exception while fetching data from doorman"
    ]


async def test_logs_unknown_clients(domain, market_int, doorman, caplog):
    doorman.list_contacts.coro.return_value = {}

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert [error.message for error in errors] == [
        "Failed resolve passports for unknown client_ids={11, 22}."
    ]


async def test_calls_bvm_for_fetching_permalinks(
    domain, market_int, doorman, bvm, geosearch
):
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt("2019-12-25 11:00:00"),
        },
        {
            "client_id": 22,
            "biz_id": 222,
            "reservation_datetime": dt("2020-01-01 12:00:00"),
        },
    ]
    doorman.list_contacts.coro.return_value = {
        11: {"passport_uid": 123},
        22: {"passport_uid": 456},
    }

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    bvm.fetch_permalinks_by_biz_ids.assert_called_once_with(biz_ids=[111, 222])


async def test_calls_bvm_only_for_orders_with_passport(
    domain, market_int, doorman, bvm
):
    doorman.list_contacts.coro.return_value = {
        11: {"passport_uid": None},
        22: {"passport_uid": 222222},
    }

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    bvm.fetch_permalinks_by_biz_ids.assert_called_once_with(biz_ids=[222])


async def test_does_not_call_bvm_if_no_orders_with_passport(
    domain, market_int, doorman, bvm
):
    doorman.list_contacts.coro.return_value = {11: {"passport_uid": None}}

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    bvm.fetch_permalinks_by_biz_ids.assert_not_called()


async def test_logs_bvm_exceptions(domain, market_int, bvm, caplog):
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt("2019-12-25 11:00:00"),
        }
    ]
    bvm.fetch_permalinks_by_biz_ids.side_effect = Exception("boom!")

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert [error.message for error in errors] == [
        "Unhandled exception while fetching permalinks from bvm for biz_ids=[111]"
    ]


async def test_calls_geosearch_for_fetching_org_timezones(
    domain, market_int, doorman, bvm, geosearch
):
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt("2019-12-25 11:00:00"),
        },
        {
            "client_id": 22,
            "biz_id": 222,
            "reservation_datetime": dt("2020-01-01 12:00:00"),
        },
    ]
    doorman.list_contacts.coro.return_value = {
        11: {"passport_uid": 123},
        22: {"passport_uid": 456},
    }

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    geosearch.resolve_org.assert_has_calls(
        [
            mock.call(permalink=111111),
            mock.call(permalink=222222),
        ]
    )


async def test_does_not_call_geosearch_if_permalink_was_not_resolved(
    domain, market_int, bvm, geosearch
):
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt("2019-12-25 11:00:00"),
        }
    ]
    bvm.fetch_permalinks_by_biz_ids.side_effect = Exception("boom!")

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    geosearch.resolve_org.assert_not_called()


async def test_logs_geosearch_exceptions(domain, market_int, geosearch, caplog):
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt("2019-12-25 11:00:00"),
        }
    ]
    geosearch.resolve_org.coro.side_effect = Exception("Boom!")

    async for _ in domain.iter_orders_poi_data_for_export():
        pass

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert [error.message for error in errors] == [
        "Unhandled exception while fetching tz_offset "
        "from geosearch for biz_id=111, permalink=111111"
    ]


async def test_does_not_return_orders_without_permalink(domain, market_int, bvm):
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt("2019-12-25 11:00:00"),
        },
        {
            "client_id": 22,
            "biz_id": 222,
            "reservation_datetime": dt("2020-01-01 12:00:00"),
        },
    ]
    bvm.fetch_permalinks_by_biz_ids.coro.return_value = {222: [222222]}

    records = []
    async for data in domain.iter_orders_poi_data_for_export():
        records.extend(data)

    assert records == [
        {
            "experiment_tag": "market_int",
            "passport_uid": 456,
            "permalink": 222222,
            "subscript": [
                ("RU", "Запись на 15:00, 1 янв."),
                ("EN", "Appointment at 15:00, 1 Jan"),
            ],
        },
    ]


async def test_does_not_return_orders_without_tz_offset(domain, market_int, geosearch):
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt("2019-12-25 11:00:00"),
        },
        {
            "client_id": 22,
            "biz_id": 222,
            "reservation_datetime": dt("2020-01-01 12:00:00"),
        },
    ]
    geosearch.resolve_org.coro.side_effect = [
        Exception("boom!"),
        GeoSearchOrgResultMock(tz_offset=timedelta(seconds=10800)),
    ]

    records = []
    async for data in domain.iter_orders_poi_data_for_export():
        records.extend(data)

    assert records == [
        {
            "experiment_tag": "market_int",
            "passport_uid": 456,
            "permalink": 222222,
            "subscript": [
                ("RU", "Запись на 15:00, 1 янв."),
                ("EN", "Appointment at 15:00, 1 Jan"),
            ],
        },
    ]


@pytest.mark.parametrize(
    "reservation_datetime, expected_ru_subscript, expected_en_subscript",
    [
        # reservation for today
        ("2019-12-25 11:00:00", "Запись на 11:00", "Appointment at 11:00"),
        # if reservation not for today -> add date
        (
            "2020-01-01 01:00:00",
            "Запись на 01:00, 1 янв.",
            "Appointment at 01:00, 1 Jan",
        ),
        (
            "2020-02-02 02:02:00",
            "Запись на 02:02, 2 февр.",
            "Appointment at 02:02, 2 Feb",
        ),
        (
            "2020-03-12 03:03:03",
            "Запись на 03:03, 12 мар.",
            "Appointment at 03:03, 12 Mar",
        ),
        (
            "2020-04-15 04:20:00",
            "Запись на 04:20, 15 апр.",
            "Appointment at 04:20, 15 Apr",
        ),
        (
            "2020-05-20 12:30:00",
            "Запись на 12:30, 20 мая",
            "Appointment at 12:30, 20 May",
        ),
        (
            "2020-06-21 12:45:45",
            "Запись на 12:45, 21 июн.",
            "Appointment at 12:45, 21 Jun",
        ),
        (
            "2020-07-22 20:00:00",
            "Запись на 20:00, 22 июл.",
            "Appointment at 20:00, 22 Jul",
        ),
        (
            "2020-08-23 21:00:00",
            "Запись на 21:00, 23 авг.",
            "Appointment at 21:00, 23 Aug",
        ),
        (
            "2020-09-24 22:00:00",
            "Запись на 22:00, 24 сент.",
            "Appointment at 22:00, 24 Sep",
        ),
        (
            "2020-10-25 23:00:00",
            "Запись на 23:00, 25 окт.",
            "Appointment at 23:00, 25 Oct",
        ),
        (
            "2020-11-30 23:30:00",
            "Запись на 23:30, 30 нояб.",
            "Appointment at 23:30, 30 Nov",
        ),
        (
            "2020-12-31 23:59:59",
            "Запись на 23:59, 31 дек.",
            "Appointment at 23:59, 31 Dec",
        ),
    ],
)
async def test_formats_subscript_as_expected(
    reservation_datetime,
    expected_ru_subscript,
    expected_en_subscript,
    domain,
    market_int,
    geosearch,
):
    geosearch.resolve_org.coro.return_value.tz_offset = timedelta(seconds=0)
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt(reservation_datetime),
        },
    ]

    result = []
    async for data in domain.iter_orders_poi_data_for_export():
        result.extend(data)

    assert result[0]["subscript"] == [
        ("RU", expected_ru_subscript),
        ("EN", expected_en_subscript),
    ]


@pytest.mark.parametrize(
    "tz_offset, expected_ru_subscript, expected_en_subscript",
    [
        # "Europe/Kaliningrad"
        (7200, "Запись на 15:00", "Appointment at 15:00"),
        # "Europe/Moscow"
        (10800, "Запись на 16:00", "Appointment at 16:00"),
        # "Asia/Kamchatka"
        (43200, "Запись на 01:00", "Appointment at 01:00"),
    ],
)
async def test_formats_subscript_time_as_expected(
    tz_offset,
    expected_ru_subscript,
    expected_en_subscript,
    domain,
    market_int,
    geosearch,
):
    geosearch.resolve_org.coro.return_value.tz_offset = timedelta(seconds=tz_offset)
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 11,
            "biz_id": 111,
            "reservation_datetime": dt("2019-12-25 13:00:00"),
        },
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
    [(None, [2]), (0, [2]), (1, [1, 1]), (100, [2])],
)
async def test_chunks_data_by_requested_size(domain, iter_size, expected_chunk_sizes):
    chunk_sizes = []
    async for data_chunk in domain.iter_orders_poi_data_for_export(iter_size=iter_size):
        chunk_sizes.append(len(data_chunk))

    assert chunk_sizes == expected_chunk_sizes


@pytest.mark.parametrize("iter_size", [None, 0, 1, 100])
async def test_returns_all_records_eventually(domain, iter_size):
    records = []
    async for data in domain.iter_orders_poi_data_for_export(iter_size=iter_size):
        records.extend(data)

    assert [record["passport_uid"] for record in records] == [123, 456]

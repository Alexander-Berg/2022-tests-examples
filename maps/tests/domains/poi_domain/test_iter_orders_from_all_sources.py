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
            client_id=11,
            passport_uid=123,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        )
    ]

    return booking_yang


@pytest.fixture(autouse=True)
async def market_int(market_int):
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 22,
            "biz_id": 222,
            "reservation_datetime": dt("2019-12-25 12:00:00"),
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
    bvm.fetch_permalinks_by_biz_id.coro.return_value = [222222]

    return bvm


async def test_returns_data_from_all_sources(domain):
    records = []
    async for data in domain.iter_orders_poi_data_for_export():
        records.extend(data)

    assert records == [
        {
            "experiment_tag": "booking_yang",
            "passport_uid": 123,
            "permalink": 111111,
            "subscript": [("RU", "Стол на 14:00"), ("EN", "Reservation for 14:00")],
        },
        {
            "experiment_tag": "market_int",
            "passport_uid": 456,
            "permalink": 222222,
            "subscript": [
                ("RU", "Запись на 15:00"),
                ("EN", "Appointment at 15:00"),
            ],
        },
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

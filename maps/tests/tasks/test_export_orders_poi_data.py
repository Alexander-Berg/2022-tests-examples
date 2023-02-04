import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.crane_operator.server.lib.tasks import OrdersPoiYtExportTask

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def task(config, poi_domain):
    return OrdersPoiYtExportTask(config=config, poi_domain=poi_domain)


async def test_creates_expected_table(task, mock_yt):
    await task

    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/table-for-orders-poi-data"


async def test_creates_table_with_expected_schema(task, mock_yt):
    await task

    assert mock_yt["create"].call_args[1] == {
        "attributes": {
            "schema": [
                {"name": "passport_uid", "required": False, "type": "uint64"},
                {"name": "experiment_tag", "required": True, "type": "string"},
                {"name": "permalink", "required": True, "type": "uint64"},
                {
                    "name": "subscript",
                    "required": True,
                    "type_v3": {
                        "key": "string",
                        "type_name": "dict",
                        "value": "string",
                    },
                },
            ]
        }
    }


async def test_writes_data_as_expected(
    task, mock_yt, config, booking_yang, market_int, doorman, bvm
):
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
            reservation_timezone="Europe/Moscow",
        ),
        dict(
            client_id=333,
            passport_uid=9876,
            permalink=3333,
            reservation_datetime=dt("2020-01-13 12:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
    ]
    market_int.fetch_actual_orders.coro.return_value = [
        {
            "client_id": 22,
            "biz_id": 222,
            "reservation_datetime": dt("2019-12-25 12:00:00"),
        },
    ]
    doorman.list_contacts.coro.return_value = {
        11: {"passport_uid": 123},
        22: {"passport_uid": 456},
    }
    bvm.fetch_permalinks_by_biz_id.coro.return_value = [222222]

    await task

    mock_yt["write_table"].assert_called_with(
        config["YT_TABLE_FOR_ORDERS_POI"],
        [
            {
                "experiment_tag": "booking_yang",
                "passport_uid": 6543,
                "permalink": 111111,
                "subscript": [
                    ("RU", "Стол на 14:00, 25 дек."),
                    ("EN", "Reservation for 14:00, 25 Dec"),
                ],
            },
            {
                "experiment_tag": "booking_yang",
                "passport_uid": 4567,
                "permalink": 222222,
                "subscript": [
                    ("RU", "Стол на 14:00, 25 янв."),
                    ("EN", "Reservation for 14:00, 25 Jan"),
                ],
            },
            {
                "experiment_tag": "booking_yang",
                "passport_uid": 9876,
                "permalink": 3333,
                "subscript": [
                    ("RU", "Стол на 15:00, 13 янв."),
                    ("EN", "Reservation for 15:00, 13 Jan"),
                ],
            },
            {
                "experiment_tag": "market_int",
                "passport_uid": 456,
                "permalink": 222222,
                "subscript": [
                    ("RU", "Запись на 15:00, 25 дек."),
                    ("EN", "Appointment at 15:00, 25 Dec"),
                ],
            },
        ],
    )

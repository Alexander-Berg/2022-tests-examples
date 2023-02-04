import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_future_successful_orders(domain, dm):
    dm.fetch_actual_orders.coro.return_value = [
        dict(
            permalink=111111,
            client_id=1111,
            passport_uid=111,
            reservation_datetime=dt("2020-01-25 12:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
        dict(
            permalink=222222,
            client_id=2222,
            passport_uid=222,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Asia/Yekaterinburg",
        ),
    ]

    got = await domain.fetch_actual_orders(actual_on=dt("2019-12-25 11:00:00"))

    assert got == [
        dict(
            passport_uid=111,
            client_id=1111,
            permalink=111111,
            reservation_datetime=dt("2020-01-25 12:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
        dict(
            passport_uid=222,
            client_id=2222,
            permalink=222222,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Asia/Yekaterinburg",
        ),
    ]


async def test_asks_for_data_correctly(domain, dm):
    dm.fetch_actual_orders.coro.return_value = []

    await domain.fetch_actual_orders(actual_on=dt("2019-12-25 11:00:00"))

    dm.fetch_actual_orders.assert_called_with(actual_on=dt("2019-12-25 11:00:00"))


async def test_returns_empty_list_if_nothing_found_in_dm(dm, domain):
    dm.fetch_actual_orders.coro.return_value = []

    got = await domain.fetch_actual_orders(actual_on=dt("2019-12-25 11:00:00"))

    assert got == []

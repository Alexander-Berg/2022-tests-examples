import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.booking_yang.server.lib.enums import OrderStatus

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_requests_dm_correctly(domain, dm):
    await domain.list_client_orders(
        biz_id=888,
        client_id=777,
        datetime_from=dt("2020-01-11 18:00:00"),
        datetime_to=dt("2020-01-14 18:00:00"),
    )

    dm.list_client_orders.assert_called_with(
        biz_id=888,
        client_id=777,
        datetime_from=dt("2020-01-11 18:00:00"),
        datetime_to=dt("2020-01-14 18:00:00"),
    )


async def test_returns_orders_data(domain, dm):
    dm_response = dict(
        events_before=3,
        events_after=5,
        orders=[
            dict(
                order_id=111,
                created_at=dt("2019-12-13 18:00:00"),
                reservation_datetime=dt("2020-01-14 18:00:00"),
                reservation_timezone="Europe/Moscow",
                person_count=1,
                status=OrderStatus.UNPROCESSED,
            )
        ],
    )

    dm.list_client_orders.coro.return_value = dm_response

    got = await domain.list_client_orders(biz_id=888, client_id=777)

    assert got == dm_response

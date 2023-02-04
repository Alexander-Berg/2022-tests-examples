from unittest import mock

import pytest
from smb.common.http_client import BaseHttpClientException
from smb.common.testing_utils import Any, dt

from maps_adv.common.helpers import AsyncContextManagerMock
from maps_adv.geosmb.doorman.client import OrderEvent, Source

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.config(DOORMAN_URL="http://doorman.server"),
]


@pytest.fixture(autouse=True)
def dm(dm):
    dm.list_orders_without_client.coro.return_value = [
        {
            "id": 1,
            "biz_id": 123,
            "customer_name": "Customer",
            "customer_phone": "+7 (000) 000-00-00",
            "customer_passport_uid": 7654332,
            "created_at": dt("2019-01-01 00:00:00"),
        },
        {
            "id": 2,
            "biz_id": 456,
            "customer_name": "Customer 2",
            "customer_phone": "+7 (111) 111-11-11",
            "customer_passport_uid": None,
            "created_at": dt("2019-02-02 00:00:00"),
        },
    ]

    return dm


async def test_creates_clients_in_doorman(domain, doorman):
    await domain.create_missed_clients()

    doorman.create_client.assert_has_calls(
        [
            mock.call(
                biz_id=123,
                source=Source.BOOKING_YANG,
                phone=70000000000,
                first_name="Customer",
                passport_uid=7654332,
            ),
            mock.call(
                biz_id=456,
                source=Source.BOOKING_YANG,
                phone=71111111111,
                first_name="Customer 2",
                passport_uid=None,
            ),
        ]
    )


async def test_sends_created_order_events_to_doorman(domain, doorman):
    doorman.create_client.coro.side_effect = ({"id": 11}, {"id": 22})

    await domain.create_missed_clients()

    doorman.add_order_event.assert_has_calls(
        [
            mock.call(
                client_id=11,
                order_id=1,
                biz_id=123,
                event_type=OrderEvent.CREATED,
                event_timestamp=dt("2019-01-01 00:00:00"),
                source=Source.BOOKING_YANG,
            ),
            mock.call(
                client_id=22,
                order_id=2,
                biz_id=456,
                event_type=OrderEvent.CREATED,
                event_timestamp=dt("2019-02-02 00:00:00"),
                source=Source.BOOKING_YANG,
            ),
        ]
    )


async def test_saves_doorman_client_ids(doorman, domain, dm):
    doorman.create_client.coro.side_effect = ({"id": 11}, {"id": 22})

    await domain.create_missed_clients()

    dm.update_orders.assert_has_calls(
        [
            mock.call(con=Any(AsyncContextManagerMock), order_ids=[1], client_id=11),
            mock.call(con=Any(AsyncContextManagerMock), order_ids=[2], client_id=22),
        ]
    )


async def test_failed_doorman_request_does_not_affect_other_orders(domain, doorman):
    doorman.create_client.coro.side_effect = (
        BaseHttpClientException("boom!"),  # fails for first order
        {"id": 22},
    )

    await domain.create_missed_clients()

    # process second order independently of the failed first one
    doorman.create_client.assert_called_with(
        biz_id=456,
        source=Source.BOOKING_YANG,
        phone=71111111111,
        first_name="Customer 2",
        passport_uid=None,
    )
    doorman.add_order_event.assert_called_once_with(
        client_id=22,
        order_id=2,
        biz_id=456,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2019-02-02 00:00:00"),
        source=Source.BOOKING_YANG,
    )


async def test_raises_for_unknown_doorman_error(domain, doorman):
    doorman.create_client.coro.side_effect = (Exception("boom!"),)

    with pytest.raises(Exception, match="boom!"):
        await domain.create_missed_clients()

    doorman.create_client.assert_called_once()
    doorman.add_order_event.assert_not_called()


async def test_logs_failed_doorman_request(domain, dm, doorman, caplog):
    dm.list_orders_without_client.coro.return_value = [
        {
            "id": 1,
            "biz_id": 123,
            "customer_name": "Customer",
            "customer_phone": "+7 (000) 000-00-00",
            "customer_passport_uid": 7654332,
            "created_at": dt("2019-01-01 00:00:00"),
        },
    ]
    doorman.create_client.coro.side_effect = BaseHttpClientException("boom!")

    await domain.create_missed_clients()

    warnings = [r for r in caplog.records if r.levelname == "WARNING"]
    assert len(warnings) == 1
    assert warnings[0].message == "Fails to create client for order_id=1: boom!"


@pytest.mark.config(DOORMAN_URL=None)
async def test_does_not_request_doorman_if_client_is_not_setup(domain, doorman):
    await domain.create_missed_clients()

    doorman.create_client.assert_not_called()
    doorman.add_order_event.assert_not_called()


async def test_does_not_request_doorman_if_no_orders(domain, dm, doorman):
    dm.list_orders_without_client.coro.return_value = []

    await domain.create_missed_clients()

    doorman.create_client.assert_not_called()
    doorman.add_order_event.assert_not_called()


async def test_does_not_send_event_to_doorman_if_db_update_fails(domain, dm, doorman):
    dm.list_orders_without_client.coro.return_value = [
        {
            "id": 1,
            "biz_id": 123,
            "customer_name": "Customer",
            "customer_phone": "+7 (000) 000-00-00",
            "customer_passport_uid": 7654332,
            "created_at": dt("2019-01-01 00:00:00"),
        },
    ]
    dm.update_orders.side_effect = Exception()

    with pytest.raises(Exception):
        await domain.create_missed_clients()

    doorman.create_client.assert_called()
    doorman.add_order_event.assert_not_called()

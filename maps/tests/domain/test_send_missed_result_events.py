from unittest import mock

import pytest
from smb.common.http_client import BaseHttpClientException
from smb.common.testing_utils import Any, dt

from maps_adv.common.helpers import AsyncContextManagerMock
from maps_adv.geosmb.doorman.client import OrderEvent, Source

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.freeze_time(dt("2019-12-25 11:00:00")),
    pytest.mark.config(DOORMAN_URL="http://doorman.server"),
]


@pytest.fixture(autouse=True)
def dm(dm):
    dm.list_orders_for_sending_result_event.coro.return_value = [
        {"id": 1, "biz_id": 11, "client_id": 111, "verdict": "booked"},
        {"id": 2, "biz_id": 22, "client_id": 222, "verdict": "no_place"},
    ]

    return dm


async def test_sends_order_events_to_doorman(domain, doorman):
    await domain.send_missed_result_events()

    doorman.add_order_event.assert_has_calls(
        [
            mock.call(
                order_id=1,
                biz_id=11,
                client_id=111,
                event_type=OrderEvent.ACCEPTED,
                event_timestamp=dt("2019-12-25 11:00:00"),
                source=Source.BOOKING_YANG,
            ),
            mock.call(
                order_id=2,
                biz_id=22,
                client_id=222,
                event_type=OrderEvent.REJECTED,
                event_timestamp=dt("2019-12-25 11:00:00"),
                source=Source.BOOKING_YANG,
            ),
        ]
    )


async def test_marks_orders_as_sent(doorman, domain, dm):
    doorman.create_client.coro.side_effect = ({"id": 11}, {"id": 22})

    await domain.send_missed_result_events()

    dm.update_orders.assert_has_calls(
        [
            mock.call(
                con=Any(AsyncContextManagerMock),
                order_ids=[1],
                sent_result_event_at=dt("2019-12-25 11:00:00"),
            ),
            mock.call(
                con=Any(AsyncContextManagerMock),
                order_ids=[2],
                sent_result_event_at=dt("2019-12-25 11:00:00"),
            ),
        ]
    )


async def test_failed_doorman_request_does_not_affect_other_orders(domain, doorman):
    doorman.add_order_event.coro.side_effect = (
        BaseHttpClientException("boom!"),
        None,
    )

    await domain.send_missed_result_events()

    doorman.add_order_event.assert_has_calls(
        [
            mock.call(
                order_id=1,
                biz_id=11,
                client_id=111,
                event_type=OrderEvent.ACCEPTED,
                event_timestamp=dt("2019-12-25 11:00:00"),
                source=Source.BOOKING_YANG,
            ),
            mock.call(
                order_id=2,
                biz_id=22,
                client_id=222,
                event_type=OrderEvent.REJECTED,
                event_timestamp=dt("2019-12-25 11:00:00"),
                source=Source.BOOKING_YANG,
            ),
        ]
    )


async def test_raises_for_unknown_doorman_error(domain, doorman):
    doorman.add_order_event.coro.side_effect = (Exception("boom!"),)

    with pytest.raises(Exception, match="boom!"):
        await domain.send_missed_result_events()

    doorman.add_order_event.assert_called_once()


async def test_logs_failed_doorman_request(domain, dm, doorman, caplog):
    dm.list_orders_for_sending_result_event.coro.return_value = [
        {"id": 1, "biz_id": 11, "client_id": 111, "verdict": "booked"},
    ]
    doorman.add_order_event.coro.side_effect = BaseHttpClientException("boom!")

    await domain.send_missed_result_events()

    warnings = [r for r in caplog.records if r.levelname == "WARNING"]
    assert len(warnings) == 1
    assert (
        warnings[0].message == "Fails to send order result event for order_id=1: boom!"
    )


@pytest.mark.config(DOORMAN_URL=None)
async def test_does_not_request_doorman_if_client_is_not_setup(domain, doorman):
    await domain.send_missed_result_events()

    doorman.add_order_event.assert_not_called()


async def test_does_not_request_doorman_if_no_orders(domain, dm, doorman):
    dm.list_orders_for_sending_result_event.coro.return_value = []

    await domain.send_missed_result_events()

    doorman.add_order_event.assert_not_called()

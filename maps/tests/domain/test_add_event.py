import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, Source
from maps_adv.geosmb.doorman.server.lib.exceptions import (
    InvalidEventParams,
    UnknownClient,
    UnsupportedEventType,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_create_order_event_if_order_event_passed(domain, dm):
    await domain.add_event(
        client_id=111,
        biz_id=222,
        event_timestamp=dt("2020-01-01 00:00:00"),
        source=Source.BOOKING_YANG,
        order_event=dict(event_type=OrderEvent.ACCEPTED, order_id=687),
    )

    dm.add_order_event.assert_called_once_with(
        client_id=111,
        biz_id=222,
        order_id=687,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2020-01-01 00:00:00"),
        source=Source.BOOKING_YANG,
    )


async def test_calls_create_call_event_if_call_event_passed(domain, dm):
    dm.client_exists.coro.return_value = True

    await domain.add_event(
        client_id=111,
        biz_id=222,
        event_timestamp=dt("2020-01-01 00:00:00"),
        source=Source.GEOADV_PHONE_CALL,
        call_event=dict(
            event_type=CallEvent.INITIATED, event_value="abc", session_id=333
        ),
    )

    dm.add_call_event.assert_called_once_with(
        client_id=111,
        biz_id=222,
        event_timestamp=dt("2020-01-01 00:00:00"),
        source=Source.GEOADV_PHONE_CALL,
        event_type=CallEvent.INITIATED,
        event_value="abc",
        session_id=333,
    )


@pytest.mark.parametrize(
    "event_params",
    [
        dict(call_event=dict(event_type=CallEvent.INITIATED, session_id=333)),
        dict(order_event=dict(event_type=OrderEvent.ACCEPTED, order_id=687)),
    ],
)
async def test_raises_if_client_doesnt_exists(event_params, domain, dm):
    dm.client_exists.coro.return_value = False

    with pytest.raises(UnknownClient) as exc:
        await domain.add_event(
            client_id=111,
            biz_id=222,
            event_timestamp=dt("2020-01-01 00:00:00"),
            source=Source.BOOKING_YANG,
            **event_params,
        )

    assert exc.value.search_fields == {"id": 111, "biz_id": 222}


async def test_raises_for_invalid_event_params(domain):
    with pytest.raises(InvalidEventParams):
        await domain.add_event(
            client_id=111,
            biz_id=222,
            event_timestamp=dt("2020-01-01 00:00:00"),
            source=Source.BOOKING_YANG,
        )


async def test_raises_for_unsupported_call_event(domain):
    with pytest.raises(
        UnsupportedEventType, match="Event of type FINISHED can't be added via API."
    ):
        await domain.add_event(
            client_id=111,
            biz_id=222,
            event_timestamp=dt("2020-01-01 00:00:00"),
            source=Source.BOOKING_YANG,
            call_event=dict(event_type=CallEvent.FINISHED),
        )

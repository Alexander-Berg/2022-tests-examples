from datetime import datetime

import pytest
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.doorman.proto.common_pb2 import Source
from maps_adv.geosmb.doorman.proto.errors_pb2 import Error
from maps_adv.geosmb.doorman.proto.events_pb2 import (
    AddEventInput,
    CallEvent,
    OrderEvent as OrderEventPb,
    OrderEventType,
)
from maps_adv.geosmb.doorman.server.lib.enums import (
    CallEvent as CallEventEnum,
    OrderEvent as OrderEventEnum,
)

pytestmark = [pytest.mark.asyncio]

url = "v1/add_event/"


@pytest.mark.parametrize(
    "event_type_pb, event_type_enum",
    [
        (OrderEventType.CREATED, OrderEventEnum.CREATED),
        (OrderEventType.REJECTED, OrderEventEnum.REJECTED),
        (OrderEventType.ACCEPTED, OrderEventEnum.ACCEPTED),
    ],
)
@pytest.mark.parametrize(
    "source_pb, source_str",
    [
        (Source.BOOKING_YANG, "BOOKING_YANG"),
        (Source.CRM_INTERFACE, "CRM_INTERFACE"),
        (Source.GEOADV_PHONE_CALL, "GEOADV_PHONE_CALL"),
        (Source.LOYALTY_COUPONS, "LOYALTY_COUPONS"),
        (Source.BOOKING_WIDGET, "BOOKING_WIDGET"),
        (Source.BOOKING_REQUEST, "BOOKING_REQUEST"),
    ],
)
async def test_creates_order_event(
    event_type_pb, event_type_enum, source_pb, source_str, factory, api
):
    client_id = await factory.create_client(biz_id=345)

    await api.post(
        url,
        proto=AddEventInput(
            client_id=client_id,
            biz_id=345,
            timestamp=dt("2020-01-01 00:00:00", as_proto=True),
            source=source_pb,
            order_event=OrderEventPb(type=event_type_pb, order_id=687),
        ),
        expected_status=201,
    )

    events = await factory.retrieve_order_events(client_id=client_id, biz_id=345)
    assert events == [
        dict(
            client_id=client_id,
            biz_id=345,
            order_id=687,
            event_type=event_type_enum,
            event_timestamp=dt("2020-01-01 00:00:00"),
            source=source_str,
        )
    ]


async def test_creates_call_event(factory, api):
    client_id = await factory.create_client(biz_id=345)

    await api.post(
        url,
        proto=AddEventInput(
            client_id=client_id,
            biz_id=345,
            timestamp=dt("2020-01-01 00:00:00", as_proto=True),
            source=Source.GEOADV_PHONE_CALL,
            call_event=CallEvent(type=CallEvent.INITIATED, value="abc", session_id=111),
        ),
        expected_status=201,
    )

    events = await factory.retrieve_call_events(client_id=client_id, biz_id=345)
    assert events == [
        dict(
            id=Any(int),
            client_id=client_id,
            biz_id=345,
            event_type=CallEventEnum.INITIATED,
            event_value="abc",
            event_timestamp=dt("2020-01-01 00:00:00"),
            source="GEOADV_PHONE_CALL",
            session_id=111,
            record_url=None,
            await_duration=None,
            talk_duration=None,
            geoproduct_id=None,
            created_at=Any(datetime),
        )
    ]


@pytest.mark.parametrize(
    "event_params",
    [
        dict(order_event=OrderEventPb(type=OrderEventType.CREATED, order_id=687)),
        dict(call_event=CallEvent(type=CallEvent.INITIATED, session_id=987)),
    ],
)
async def test_returns_nothing(event_params, factory, api):
    client_id = await factory.create_client(biz_id=345)

    got = await api.post(
        url,
        proto=AddEventInput(
            client_id=client_id,
            biz_id=345,
            timestamp=dt("2020-01-01 00:00:00", as_proto=True),
            source=Source.GEOADV_PHONE_CALL,
            **event_params,
        ),
        expected_status=201,
    )

    assert got == b""


@pytest.mark.parametrize("client_id, biz_id", [(111, 999), (999, 222)])
@pytest.mark.parametrize(
    "event_params",
    [
        dict(order_event=OrderEventPb(type=OrderEventType.CREATED, order_id=687)),
        dict(call_event=CallEvent(type=CallEvent.INITIATED, session_id=987)),
    ],
)
async def test_returns_404_for_unknown_client(
    event_params, client_id, biz_id, factory, api
):
    await factory.create_client(client_id=111, biz_id=222)

    got = await api.post(
        url,
        proto=AddEventInput(
            client_id=client_id,
            biz_id=biz_id,
            timestamp=dt("2020-01-01 00:00:00", as_proto=True),
            source=Source.GEOADV_PHONE_CALL,
            **event_params,
        ),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_CLIENT,
        description=f"Unknown client with biz_id={biz_id}, id={client_id}",
    )


@pytest.mark.parametrize("field_name", ["biz_id", "client_id"])
@pytest.mark.parametrize(
    "event_params",
    [
        dict(order_event=OrderEventPb(type=OrderEventType.CREATED, order_id=687)),
        dict(call_event=CallEvent(type=CallEvent.INITIATED, session_id=987)),
    ],
)
async def test_returns_400_for_wrong_client_input(event_params, field_name, api):
    input_params = dict(
        client_id=123,
        biz_id=345,
        timestamp=dt("2020-01-01 00:00:00", as_proto=True),
        source=Source.GEOADV_PHONE_CALL,
        **event_params,
    )
    input_params.update(**{field_name: 0})

    got = await api.post(
        url, proto=AddEventInput(**input_params), decode_as=Error, expected_status=400
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description=f"{field_name}: ['Must be at least 1.']",
    )


async def test_returns_400_for_wrong_order_id_input(api):
    input_params = dict(
        client_id=123,
        biz_id=345,
        timestamp=dt("2020-01-01 00:00:00", as_proto=True),
        source=Source.GEOADV_PHONE_CALL,
        order_event=OrderEventPb(type=OrderEventType.CREATED, order_id=0),
    )

    got = await api.post(
        url, proto=AddEventInput(**input_params), decode_as=Error, expected_status=400
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description="order_event: {'order_id': ['Must be at least 1.']}",
    )


async def test_returns_400_for_unsupported_call_event_type(factory, api):
    await factory.create_client(client_id=123, biz_id=345)
    input_params = dict(
        client_id=123,
        biz_id=345,
        timestamp=dt("2020-01-01 00:00:00", as_proto=True),
        source=Source.GEOADV_PHONE_CALL,
        call_event=CallEvent(type=CallEvent.FINISHED, session_id=987),
    )

    got = await api.post(
        url, proto=AddEventInput(**input_params), decode_as=Error, expected_status=400
    )

    assert got == Error(
        code=Error.UNSUPPORTED_EVENT_TYPE,
        description="Event of type FINISHED can't be added via API.",
    )

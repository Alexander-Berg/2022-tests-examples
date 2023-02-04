from datetime import datetime

import pytest
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.client.lib import UnexpectedNaiveDatetime
from maps_adv.geosmb.doorman.client.lib.enums import (
    ENUM_TO_PROTO_MAP,
    OrderEvent as OrderEventEnum,
    Source as SourceEnum,
)
from maps_adv.geosmb.doorman.proto.events_pb2 import (
    AddEventInput,
    OrderEvent as OrderEventPb,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("order_event_type", OrderEventEnum)
@pytest.mark.parametrize("source", SourceEnum)
async def test_sends_correct_request_for_order_event(
    order_event_type, source, client, mock_add_event
):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=201)

    mock_add_event(_handler)

    await client.add_order_event(
        client_id=111,
        biz_id=222,
        event_timestamp=dt("2020-02-05 11:30:45"),
        source=source,
        event_type=order_event_type,
        order_id=687,
    )

    assert request_path == "/v1/add_event/"
    proto_body = AddEventInput.FromString(request_body)
    assert proto_body == AddEventInput(
        client_id=111,
        biz_id=222,
        timestamp=dt("2020-02-05 11:30:45", as_proto=True),
        source=ENUM_TO_PROTO_MAP["source"][source],
        order_event=OrderEventPb(
            type=ENUM_TO_PROTO_MAP["order_event_type"][order_event_type], order_id=687
        ),
    )


async def test_returns_nothing(client, mock_add_event):
    mock_add_event(Response(status=201))

    got = await client.add_order_event(
        client_id=111,
        biz_id=222,
        event_timestamp=dt("2020-02-05 11:30:45"),
        source=SourceEnum.CRM_INTERFACE,
        event_type=OrderEventEnum.CREATED,
        order_id=687,
    )

    assert got is None


async def test_raises_if_naive_datetime_passed(client):
    with pytest.raises(UnexpectedNaiveDatetime) as exc:
        await client.add_order_event(
            client_id=111,
            biz_id=222,
            event_timestamp=datetime(2020, 2, 2, 12, 00),
            source=SourceEnum.CRM_INTERFACE,
            event_type=OrderEventEnum.CREATED,
            order_id=687,
        )

    assert exc.value.args == (datetime(2020, 2, 2, 12, 00),)

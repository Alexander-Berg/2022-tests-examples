from datetime import timedelta

import pytest

from maps_adv.geosmb.doorman.client import NotFound
from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.exceptions import NoOrginfo, NoOrgsForBizId
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    AddressNotAllowed,
    NoAddress,
    SendFailed,
)
from maps_adv.geosmb.telegraphist.server.tests.helpers import make_order

pytestmark = [pytest.mark.asyncio]


async def test_returns_effective_addresses_on_success(domain, notification_router):
    notification_router.send_client_notification.coro.return_value = [
        {Transport.SMS: {"phone": 322223}}
    ]

    got = await domain.send_notification_v2(
        recipient=dict(client_id=100500, biz_id=1),
        transports=[Transport.SMS],
        order_created=dict(order=make_order()),
    )

    assert got == [{Transport.SMS: {"phone": 322223}}]


@pytest.mark.parametrize(
    ("returned_exc_cls", "expected_text"),
    [
        (NoAddress, "No address to send to"),
        (SendFailed, "Send failed"),
        (AddressNotAllowed, "Address not allowed"),
    ],
)
async def test_returns_error_texts_on_errors(
    domain, notification_router, returned_exc_cls, expected_text
):
    notification_router.send_client_notification.coro.return_value = [
        {Transport.SMS: returned_exc_cls()}
    ]

    got = await domain.send_notification_v2(
        recipient=dict(client_id=100500, biz_id=15),
        transports=[Transport.SMS],
        order_created=dict(order=make_order()),
    )

    assert got == [{Transport.SMS: {"error": expected_text}}]


async def test_raises_doorman_exceptions_from_router(domain, notification_router):
    notification_router.send_client_notification.coro.side_effect = NotFound(
        "Client not found."
    )

    with pytest.raises(NotFound, match="Client not found."):
        await domain.send_notification_v2(
            recipient=dict(client_id=100500, biz_id=15),
            transports=[Transport.SMS],
            order_created=dict(order=make_order()),
        )


async def test_returns_values_by_transport(domain, notification_router):
    notification_router.send_client_notification.coro.return_value = [
        {Transport.SMS: {"phone": 322223}},
        {Transport.EMAIL: NoAddress()},
    ]

    got = await domain.send_notification_v2(
        recipient=dict(client_id=100500, biz_id=15),
        transports=[Transport.SMS],
        order_created=dict(order=make_order()),
    )

    assert got == [
        {Transport.SMS: {"phone": 322223}},
        {Transport.EMAIL: {"error": "No address to send to"}},
    ]


async def test_uses_clients_to_fetch_org_data(domain, bvm, geosearch):
    await domain.send_notification_v2(
        recipient=dict(client_id=100500, biz_id=15, phone=71234567890),
        transports=[Transport.SMS],
        order_created=dict(order=make_order()),
    )

    bvm.fetch_permalinks_by_biz_id.assert_called_with(biz_id=15)
    geosearch.resolve_org.assert_called_with(permalink=54321)


async def test_raises_if_no_permalinks_for_biz_id(domain, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.return_value = []

    with pytest.raises(NoOrgsForBizId):
        await domain.send_notification_v2(
            recipient=dict(client_id=100500, biz_id=15, phone=71234567890),
            transports=[Transport.SMS],
            order_created=dict(order=make_order()),
        )


async def test_raises_if_no_org_info_from_geosearch(domain, geosearch):
    geosearch.resolve_org.coro.return_value = None

    with pytest.raises(NoOrginfo):
        await domain.send_notification_v2(
            recipient=dict(client_id=100500, biz_id=15, phone=71234567890),
            transports=[Transport.SMS],
            order_created=dict(order=make_order()),
        )


@pytest.mark.parametrize(
    ("notification_type", "expected_notification_type"),
    [
        ("order_created", NotificationType.ORDER_CREATED),
        ("order_reminder", NotificationType.ORDER_REMINDER),
        ("order_changed", NotificationType.ORDER_CHANGED),
        ("order_cancelled", NotificationType.ORDER_CANCELLED),
    ],
)
@pytest.mark.parametrize(
    "transports",
    [
        [Transport.SMS],
        [Transport.EMAIL],
        [Transport.PUSH],
        [Transport.SMS, Transport.EMAIL, Transport.PUSH],
    ],
)
async def test_uses_notification_router(
    notification_router,
    domain,
    notification_type,
    expected_notification_type,
    transports,
):
    expected_notification_details = {
        "org": {
            "permalink": "54321",
            "name": "Кафе с едой",
            "phone": "+7 (495) 739-70-00",
            "formatted_address": "Город, Улица, 1",
            "url": "http://cafe.ru",
            "categories": ["Общепит", "Ресторан"],
            "tz_offset": timedelta(seconds=10800),
        },
        "order": make_order(),
    }

    await domain.send_notification_v2(
        recipient=dict(client_id=100500, biz_id=15, phone=71234567890),
        transports=transports,
        **{notification_type: dict(order=make_order())},
    )

    notification_router.send_client_notification.assert_called_with(
        recipient=dict(client_id=100500, biz_id=15, phone=71234567890),
        transports=transports,
        notification_type=expected_notification_type,
        notification_details=expected_notification_details,
    )


async def test_no_org_tz_offset_in_details_if_not_returned_from_geosearch(
    notification_router, domain, geosearch
):
    geosearch.resolve_org.coro.return_value.tz_offset = None

    await domain.send_notification_v2(
        recipient=dict(client_id=100500, biz_id=15, phone=71234567890),
        transports=[Transport.SMS],
        order_created=dict(order=make_order()),
    )

    call_kwargs = notification_router.send_client_notification.call_args.kwargs
    assert "org_tz_offset" not in call_kwargs["notification_details"]


async def test_no_org_phone_in_details_if_org_has_no_phones(
    notification_router, domain, geosearch
):
    geosearch.resolve_org.coro.return_value.phones = []

    await domain.send_notification_v2(
        recipient=dict(client_id=100500, biz_id=15, phone=71234567890),
        transports=[Transport.SMS],
        order_created=dict(order=make_order()),
    )

    call_kwargs = notification_router.send_client_notification.call_args.kwargs
    assert "org_phone" not in call_kwargs["notification_details"]

import pytest

from maps_adv.geosmb.telegraphist.server.lib.enums import Transport
from maps_adv.geosmb.telegraphist.server.lib.exceptions import (
    AddressNotAllowed,
    NoAddress,
    SendFailed,
)

from maps_adv.geosmb.telegraphist.server.tests.helpers import (
    make_input_params,
    make_request_created,
    make_cart_order_created
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ('notification_type', 'get_args'), [
        ('request_created', make_request_created),
        ('cart_order_created', make_cart_order_created)
    ]
)
async def test_returns_effective_addresses_on_success(domain, notification_type, get_args):
    params = dict()
    params[notification_type] = get_args()

    got = await domain.send_notification_v3(
        **make_input_params(**params)
    )

    assert got == [
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
        {Transport.EMAIL: {"email": "cheburekkek@yandex.ru"}},
    ]


@pytest.mark.parametrize(
    ("returned_exc_cls", "expected_error"),
    [
        (NoAddress, "No address to send to"),
        (SendFailed, "Send failed"),
        (AddressNotAllowed, "Address not allowed"),
    ],
)
@pytest.mark.parametrize(
    ('notification_type', 'get_args'), [
        ('request_created', make_request_created),
        ('cart_order_created', make_cart_order_created)
    ]
)
async def test_returns_error_texts_on_errors(
    domain, notification_router_v3, returned_exc_cls, expected_error, notification_type, get_args
):
    notification_router_v3.send_notification.coro.return_value = [
        {Transport.EMAIL: returned_exc_cls()}
    ]
    params = dict()
    params[notification_type] = get_args()

    got = await domain.send_notification_v3(
        **make_input_params(**params),
    )

    assert got == [{Transport.EMAIL: {"error": expected_error}}]


@pytest.mark.parametrize(
    ('notification_type', 'get_args'), [
        ('request_created', make_request_created),
        ('cart_order_created', make_cart_order_created)
    ]
)
async def test_returns_expected_if_not_all_emails_succeeded(
    domain, notification_router_v3, notification_type, get_args
):
    notification_router_v3.send_notification.coro.return_value = [
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
        {Transport.EMAIL: AddressNotAllowed()},
    ]
    params = dict()
    params[notification_type] = get_args()

    got = await domain.send_notification_v3(
        **make_input_params(**params),
    )

    assert got == [
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
        {Transport.EMAIL: {"error": "Address not allowed"}},
    ]

import pytest

from maps_adv.geosmb.telegraphist.server.lib.enums import Transport
from maps_adv.geosmb.telegraphist.server.lib.exceptions import NoOrginfo, NoOrgsForBizId
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    AddressNotAllowed,
    NoAddress,
    SendFailed,
)
from maps_adv.geosmb.telegraphist.server.tests.helpers import (
    make_order_for_business_notification,
)
from maps_adv.geosmb.tuner.client import UnknownBizId

pytestmark = [pytest.mark.asyncio]


def make_input_params(**updates):
    input_params = dict(
        recipient=dict(
            biz_id=15,
            cabinet_link="http://cabinet.link",
            company_link="http://company.link",
            email="passed_email@yandex.ru",
        ),
        transports=[Transport.EMAIL],
    )
    input_params.update(updates)

    return input_params


async def test_returns_effective_addresses_on_success(domain, notification_router):
    got = await domain.send_notification_for_business(
        **make_input_params(
            order_created=dict(
                client=dict(client_id=160),
                details_link="http://details.link",
                order=make_order_for_business_notification(),
            ),
        )
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
async def test_returns_error_texts_on_errors(
    domain, notification_router, returned_exc_cls, expected_error
):
    notification_router.send_business_notification.coro.return_value = [
        {Transport.EMAIL: returned_exc_cls()}
    ]

    got = await domain.send_notification_for_business(
        **make_input_params(
            order_created=dict(
                client=dict(client_id=160),
                details_link="http://details.link",
                order=make_order_for_business_notification(),
            ),
        )
    )

    assert got == [{Transport.EMAIL: {"error": expected_error}}]


async def test_returns_expected_if_not_all_emails_succeeded(
    domain, notification_router
):
    notification_router.send_business_notification.coro.return_value = [
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
        {Transport.EMAIL: AddressNotAllowed()},
    ]

    got = await domain.send_notification_for_business(
        **make_input_params(
            order_created=dict(
                client=dict(client_id=160),
                details_link="http://details.link",
                order=make_order_for_business_notification(),
            ),
        )
    )

    assert got == [
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
        {Transport.EMAIL: {"error": "Address not allowed"}},
    ]


@pytest.mark.parametrize(
    "notification_type", ["order_created", "order_changed", "order_cancelled"]
)
async def test_uses_clients_to_fetch_org_data_if_required(
    domain, bvm, geosearch, doorman, notification_type
):
    await domain.send_notification_for_business(
        **make_input_params(
            **{
                notification_type: dict(
                    client=dict(client_id=160),
                    details_link="http://details.link",
                    order=make_order_for_business_notification(),
                ),
            }
        )
    )

    bvm.fetch_permalinks_by_biz_id.assert_called_with(biz_id=15)
    geosearch.resolve_org.assert_called_with(permalink=54321)


@pytest.mark.parametrize(
    "notification_type",
    [
        "certificate_expiring",
        "certificate_expired",
        "certificate_connect_payment",
        "certificate_rejected",
        "first_certificate_approved",
        "subsequent_certificate_approved",
        "certificate_created",
        "certificate_purchased",
    ],
)
async def test_does_not_use_clients_to_fetch_org_data_if_not_required(
    domain, bvm, geosearch, doorman, notification_type
):
    await domain.send_notification_for_business(
        **make_input_params(
            **{
                notification_type: dict(
                    client=dict(client_id=160),
                    details_link="http://details.link",
                    order=make_order_for_business_notification(),
                ),
            }
        )
    )

    bvm.fetch_permalinks_by_biz_id.assert_not_called()
    geosearch.resolve_org.assert_not_called()


async def test_raises_if_no_permalinks_for_biz_id_when_orginfo_required(domain, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.return_value = []

    with pytest.raises(NoOrgsForBizId):
        await domain.send_notification_for_business(
            **make_input_params(
                order_created=dict(
                    client=dict(client_id=160),
                    details_link="http://details.link",
                    order=make_order_for_business_notification(),
                ),
            )
        )


async def test_raises_if_no_org_info_from_geosearch(domain, geosearch):
    geosearch.resolve_org.coro.return_value = None

    with pytest.raises(NoOrginfo):
        await domain.send_notification_for_business(
            **make_input_params(
                order_created=dict(
                    client=dict(client_id=160),
                    details_link="http://details.link",
                    order=make_order_for_business_notification(),
                ),
            )
        )


async def test_raises_tuner_exceptions_from_router(domain, notification_router):
    notification_router.send_business_notification.side_effect = UnknownBizId(
        "Business not found."
    )

    with pytest.raises(UnknownBizId, match="Business not found."):
        await domain.send_notification_for_business(
            **make_input_params(
                order_created=dict(
                    client=dict(client_id=160),
                    details_link="http://details.link",
                    order=make_order_for_business_notification(),
                ),
            )
        )

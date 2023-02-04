import pytest

from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.tests.helpers import (
    make_base_certificate,
    make_certificate_for_expiration_notification,
    make_full_certificate,
)

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


@pytest.mark.parametrize(
    "notification_type, expected_notification_type",
    [
        ("certificate_expiring", NotificationType.CERTIFICATE_EXPIRING),
        ("certificate_expired", NotificationType.CERTIFICATE_EXPIRED),
    ],
)
@pytest.mark.parametrize("sales", [None, "200.45"])
async def test_uses_notification_router_for_certificate_expiring_events(
    notification_router,
    domain,
    notification_type,
    expected_notification_type,
    sales,
):
    await domain.send_notification_for_business(
        **make_input_params(
            **{
                notification_type: dict(
                    create_new_link="http://create.new/certificate",
                    certificate=make_certificate_for_expiration_notification(
                        sales=sales
                    ),
                ),
            },
        )
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15, email="passed_email@yandex.ru"),
        transports=[Transport.EMAIL],
        notification_type=expected_notification_type,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            certificate=make_certificate_for_expiration_notification(sales=sales),
            create_new_link="http://create.new/certificate",
        ),
    )


@pytest.mark.parametrize(
    "notification_type", ["certificate_expiring", "certificate_expired"]
)
@pytest.mark.parametrize("sales", [None, "200.45"])
async def test_does_not_use_doorman_for_certificate_expiring_events(
    sales, domain, notification_type, doorman
):
    await domain.send_notification_for_business(
        **make_input_params(
            **{
                notification_type: dict(
                    create_new_link="http://create.new/certificate",
                    certificate=make_certificate_for_expiration_notification(
                        sales=sales
                    ),
                ),
            },
        )
    )

    doorman.retrieve_client.assert_not_called()


@pytest.mark.parametrize(
    "notification_type, expected_notification_type",
    [
        (
            "first_certificate_approved",
            NotificationType.FIRST_CERTIFICATE_APPROVED,
        ),
        (
            "subsequent_certificate_approved",
            NotificationType.SUBSEQUENT_CERTIFICATE_APPROVED,
        ),
        ("certificate_created", NotificationType.CERTIFICATE_CREATED),
        ("certificate_rejected", NotificationType.CERTIFICATE_REJECTED),
    ],
)
async def test_uses_notification_router_for_certificate_moderation(
    notification_router, domain, notification_type, expected_notification_type
):
    await domain.send_notification_for_business(
        **make_input_params(
            **{
                notification_type: dict(certificate=make_base_certificate()),
            },
        )
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15, email="passed_email@yandex.ru"),
        transports=[Transport.EMAIL],
        notification_type=expected_notification_type,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            certificate=make_base_certificate(),
        ),
    )


@pytest.mark.parametrize(
    "notification_type",
    [
        "first_certificate_approved",
        "subsequent_certificate_approved",
        "certificate_created",
        "certificate_rejected",
    ],
)
async def test_does_not_use_doorman_for_certificate_creation_and_moderation_approvement(
    doorman, domain, notification_type
):
    await domain.send_notification_for_business(
        **make_input_params(
            **{
                notification_type: dict(certificate=make_base_certificate()),
            },
        )
    )

    doorman.retrieve_client.assert_not_called()


async def test_uses_notification_router_for_certificate_purchased(
    notification_router, domain
):
    await domain.send_notification_for_business(
        **make_input_params(
            certificate_purchased=dict(
                certificate=make_full_certificate(), cashier_link="http://cashier.link"
            ),
        )
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(
            biz_id=15,
            email="passed_email@yandex.ru",
        ),
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_PURCHASED,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            certificate=make_full_certificate(),
            cashier_link="http://cashier.link",
        ),
    )


async def test_does_not_use_doorman_for_certificate_purchased(doorman, domain):
    await domain.send_notification_for_business(
        **make_input_params(
            certificate_purchased=dict(
                certificate=make_full_certificate(), cashier_link="http://cashier.link"
            ),
        )
    )

    doorman.retrieve_client.assert_not_called()


async def test_uses_notification_router_for_certificate_payment_connection(
    notification_router, domain
):
    await domain.send_notification_for_business(
        **make_input_params(
            certificate_connect_payment=dict(
                payment_setup_link="http://payment.setup/link"
            ),
        )
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(
            biz_id=15,
            email="passed_email@yandex.ru",
        ),
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CONNECT_PAYMENT,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            payment_setup_link="http://payment.setup/link",
        ),
    )


async def test_does_not_use_doorman_for_certificate_payment_connection(doorman, domain):
    await domain.send_notification_for_business(
        **make_input_params(
            certificate_connect_payment=dict(
                payment_setup_link="http://payment.setup/link"
            ),
        )
    )

    doorman.retrieve_client.assert_not_called()

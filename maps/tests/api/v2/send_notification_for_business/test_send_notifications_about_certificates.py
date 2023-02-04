import pytest

from maps_adv.geosmb.telegraphist.proto.errors_pb2 import Error
from maps_adv.geosmb.telegraphist.proto.v2.common_pb2 import Transport
from maps_adv.geosmb.telegraphist.proto.v2.notifications_for_business_pb2 import (
    CertificateConnectPayment,
    CertificateCreated,
    CertificateExpired,
    CertificateExpiring,
    CertificatePurchased,
    CertificateRejected,
    FirstCertificateApproved,
    Notification,
    Recipient,
    SubsequentCertificateApproved,
)
from maps_adv.geosmb.telegraphist.server.lib.enums import (
    NotificationType,
    Transport as TransportEnum,
)
from maps_adv.geosmb.telegraphist.server.tests.helpers import (
    make_base_certificate,
    make_certificate_for_expiration_notification,
    make_full_certificate,
)

pytestmark = [pytest.mark.asyncio]

url = "/api/v2/send-notification-for-business/"


def notification_proto(**updates) -> Notification:
    kwargs = dict(
        recipient=Recipient(
            biz_id=15,
            cabinet_link="http://cabinet.link",
            company_link="http://company.link",
        ),
        transports=[Transport.EMAIL],
    )
    kwargs.update(updates)

    return Notification(**kwargs)


@pytest.mark.parametrize(
    "pb_field, pb_cls, expected_notification_type",
    [
        (
            "certificate_expiring",
            CertificateExpiring,
            NotificationType.CERTIFICATE_EXPIRING,
        ),
        (
            "certificate_expired",
            CertificateExpired,
            NotificationType.CERTIFICATE_EXPIRED,
        ),
    ],
)
async def test_will_notify_about_unsold_certificate_expiring_events(
    notification_router, api, pb_field, pb_cls, expected_notification_type
):
    await api.post(
        url,
        proto=notification_proto(
            **{
                pb_field: pb_cls(
                    create_new_link="http://create.new/certificate",
                    certificate=make_certificate_for_expiration_notification(
                        as_proto=True, sales=None
                    ),
                ),
            },
        ),
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15),
        transports=[TransportEnum.EMAIL],
        notification_type=expected_notification_type,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            certificate=make_certificate_for_expiration_notification(sales=None),
            create_new_link="http://create.new/certificate",
        ),
    )


@pytest.mark.parametrize(
    "pb_field, pb_cls, expected_notification_type",
    [
        (
            "certificate_expiring",
            CertificateExpiring,
            NotificationType.CERTIFICATE_EXPIRING,
        ),
        (
            "certificate_expired",
            CertificateExpired,
            NotificationType.CERTIFICATE_EXPIRED,
        ),
    ],
)
async def test_will_notify_about_sold_certificate_expiring_events(
    notification_router, api, pb_field, pb_cls, expected_notification_type
):
    await api.post(
        url,
        proto=notification_proto(
            **{
                pb_field: pb_cls(
                    create_new_link="http://create.new/certificate",
                    certificate=make_certificate_for_expiration_notification(
                        as_proto=True
                    ),
                ),
            },
        ),
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15),
        transports=[TransportEnum.EMAIL],
        notification_type=expected_notification_type,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            certificate=make_certificate_for_expiration_notification(),
            create_new_link="http://create.new/certificate",
        ),
    )


@pytest.mark.parametrize(
    "pb_field, pb_cls, expected_notification_type",
    [
        (
            "first_certificate_approved",
            FirstCertificateApproved,
            NotificationType.FIRST_CERTIFICATE_APPROVED,
        ),
        (
            "subsequent_certificate_approved",
            SubsequentCertificateApproved,
            NotificationType.SUBSEQUENT_CERTIFICATE_APPROVED,
        ),
        (
            "certificate_created",
            CertificateCreated,
            NotificationType.CERTIFICATE_CREATED,
        ),
        (
            "certificate_rejected",
            CertificateRejected,
            NotificationType.CERTIFICATE_REJECTED,
        ),
    ],
)
async def test_will_notify_about_certificate_creation_and_moderation_results(
    notification_router, api, pb_field, pb_cls, expected_notification_type
):
    await api.post(
        url,
        proto=notification_proto(
            **{
                pb_field: pb_cls(
                    certificate=make_base_certificate(as_proto=True),
                ),
            },
        ),
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15),
        transports=[TransportEnum.EMAIL],
        notification_type=expected_notification_type,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            certificate=make_base_certificate(),
        ),
    )


async def test_will_notify_about_certificate_purchase(notification_router, api):
    await api.post(
        url,
        proto=notification_proto(
            certificate_purchased=CertificatePurchased(
                cashier_link="http://ceshier.link",
                certificate=make_full_certificate(as_proto=True),
            ),
        ),
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15),
        transports=[TransportEnum.EMAIL],
        notification_type=NotificationType.CERTIFICATE_PURCHASED,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            certificate=make_full_certificate(),
            cashier_link="http://ceshier.link",
        ),
    )


async def test_will_notify_about_certificate_payment_connection(
    notification_router, api
):
    await api.post(
        url,
        proto=notification_proto(
            certificate_connect_payment=CertificateConnectPayment(
                payment_setup_link="http://payment.setup/link"
            )
        ),
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15),
        transports=[TransportEnum.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CONNECT_PAYMENT,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            payment_setup_link="http://payment.setup/link",
        ),
    )


async def test_errored_on_incorrect_connect_payment_input(notification_router, api):
    got = await api.post(
        url,
        proto=notification_proto(
            certificate_connect_payment=CertificateConnectPayment(payment_setup_link="")
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description="certificate_connect_payment: {'payment_setup_link': ['Shorter than minimum length 1.']}",  # noqa
    )


@pytest.mark.parametrize(
    "pb_field, pb_cls",
    [
        ("first_certificate_approved", FirstCertificateApproved),
        ("subsequent_certificate_approved", SubsequentCertificateApproved),
        ("certificate_rejected", CertificateRejected),
    ],
)
@pytest.mark.parametrize(
    "incorrect_params, expected_error",
    [
        (
            dict(name=""),
            "{'certificate': {'name': ['Shorter than minimum length 1.']}}",
        ),
        (
            dict(link=""),
            "{'certificate': {'link': ['Shorter than minimum length 1.']}}",
        ),
    ],
)
async def test_errored_on_moderation_approvement_input(
    notification_router, api, pb_field, pb_cls, incorrect_params, expected_error
):
    got = await api.post(
        url,
        proto=notification_proto(
            **{
                pb_field: pb_cls(
                    certificate=make_base_certificate(as_proto=True, **incorrect_params)
                )
            }
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR, description=f"{pb_field}: {expected_error}"
    )


@pytest.mark.parametrize(
    "pb_field, pb_cls",
    [
        ("certificate_expiring", CertificateExpiring),
        ("certificate_expired", CertificateExpired),
    ],
)
@pytest.mark.parametrize(
    "incorrect_params, expected_error",
    [
        (
            dict(create_new_link=""),
            "{'create_new_link': ['Shorter than minimum length 1.']}",
        ),
        (
            dict(
                certificate=make_certificate_for_expiration_notification(
                    name="", as_proto=True
                )
            ),
            "{'certificate': {'base_details': {'name': ['Shorter than minimum length 1.']}}}",  # noqa
        ),
        (
            dict(
                certificate=make_certificate_for_expiration_notification(
                    link="", as_proto=True
                )
            ),
            "{'certificate': {'base_details': {'link': ['Shorter than minimum length 1.']}}}",  # noqa
        ),
    ],
)
async def test_errored_on_incorrect_expiring_input(
    notification_router, api, pb_field, pb_cls, incorrect_params, expected_error
):
    input_params = {
        "create_new_link": "http://create.new/certificate",
        "certificate": make_certificate_for_expiration_notification(as_proto=True),
    }
    input_params.update(**incorrect_params)

    got = await api.post(
        url,
        proto=notification_proto(**{pb_field: pb_cls(**input_params)}),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description=f"{pb_field}: {expected_error}",
    )

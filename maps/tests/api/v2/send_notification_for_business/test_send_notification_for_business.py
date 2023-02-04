import pytest

from maps_adv.geosmb.clients.bvm import BvmNotFound
from maps_adv.geosmb.telegraphist.proto.errors_pb2 import Error
from maps_adv.geosmb.telegraphist.proto.v2.common_pb2 import (
    NotificationResult,
    NotificationTransportResult,
    Transport,
)
from maps_adv.geosmb.telegraphist.proto.v2.notifications_for_business_pb2 import (
    CertificateConnectPayment,
    Client,
    Notification,
    OrderChanged,
    OrderCreated,
    Recipient,
)
from maps_adv.geosmb.telegraphist.server.lib.enums import Transport as TransportEnum
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    NoAddress,
    SendFailed,
)
from maps_adv.geosmb.telegraphist.server.tests.helpers import (
    make_order_for_business_notification,
)
from maps_adv.geosmb.tuner.client import UnknownBizId

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
    ("notification_router_return", "expected_pb"),
    [
        (
            [{TransportEnum.EMAIL: NoAddress()}],
            NotificationResult(
                results=[
                    NotificationTransportResult(
                        transport=Transport.EMAIL, error="No address to send to"
                    )
                ]
            ),
        ),
        (
            [
                {TransportEnum.EMAIL: {"email": "passed_email@yandex.ru"}},
                {TransportEnum.EMAIL: {"email": "another_email@yandex.ru"}},
            ],
            NotificationResult(
                results=[
                    NotificationTransportResult(
                        transport=Transport.EMAIL, email="passed_email@yandex.ru"
                    ),
                    NotificationTransportResult(
                        transport=Transport.EMAIL, email="another_email@yandex.ru"
                    ),
                ]
            ),
        ),
        (
            [
                {TransportEnum.EMAIL: {"email": "passed_email@yandex.ru"}},
                {TransportEnum.EMAIL: SendFailed()},
            ],
            NotificationResult(
                results=[
                    NotificationTransportResult(
                        transport=Transport.EMAIL, email="passed_email@yandex.ru"
                    ),
                    NotificationTransportResult(
                        transport=Transport.EMAIL, error="Send failed"
                    ),
                ]
            ),
        ),
    ],
)
async def test_returns_notification_result(
    api, notification_router, notification_router_return, expected_pb
):
    notification_router.send_business_notification.coro.return_value = (
        notification_router_return
    )

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(
                client=dict(client_id=160),
                details_link="http://details.link",
                order=make_order_for_business_notification(as_proto=True),
            )
        ),
        decode_as=NotificationResult,
        expected_status=200,
    )

    assert got == expected_pb


@pytest.mark.parametrize(
    "incorrect_params, expected_error",
    (
        [dict(biz_id=0), "{'biz_id': ['Must be at least 1.']}"],
        [dict(cabinet_link=""), "{'cabinet_link': ['Shorter than minimum length 1.']}"],
        [dict(company_link=""), "{'company_link': ['Shorter than minimum length 1.']}"],
    ),
)
async def test_errored_on_wrong_recipient_input(incorrect_params, expected_error, api):
    input_params = dict(
        biz_id=15,
        company_link="http://company.link",
        cabinet_link="http://cabinet.link",
    )
    input_params.update(incorrect_params)

    got = await api.post(
        url,
        proto=notification_proto(
            recipient=Recipient(**input_params),
            order_created=OrderCreated(
                client=Client(client_id=160),
                order=make_order_for_business_notification(as_proto=True),
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description=f"recipient: {expected_error}",
    )


@pytest.mark.parametrize(
    ("transports", "expected_description"),
    [
        (
            [Transport.SMS],
            "notification_type=order_created_for_business, unsupported_transports=SMS",
        ),
        (
            [Transport.PUSH],
            "notification_type=order_created_for_business, unsupported_transports=PUSH",
        ),
        (
            [Transport.SMS, Transport.PUSH],
            "notification_type=order_created_for_business, unsupported_transports=PUSH,SMS",  # noqa
        ),
    ],
)
async def test_errored_on_unsupported_transport(api, transports, expected_description):
    got = await api.post(
        url,
        proto=notification_proto(
            transports=transports,
            order_created=OrderCreated(
                client=Client(client_id=160),
                order=make_order_for_business_notification(as_proto=True),
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.UNSUPPORTED_TRANSPORT, description=expected_description
    )


async def test_errored_on_unknown_biz_id(api, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.side_effect = BvmNotFound(
        "Some BVM error description"
    )

    got = await api.post(
        url,
        proto=notification_proto(
            order_changed=OrderChanged(
                client=Client(client_id=160),
                order=make_order_for_business_notification(as_proto=True),
            )
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.UNKNOWN_BIZ_ID, description="Some BVM error description"
    )


async def test_errored_on_business_without_orgs(api, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.return_value = []

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(
                client=Client(client_id=160),
                order=make_order_for_business_notification(as_proto=True),
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.NO_ORGS_FOR_BIZ_ID, description="No permalinks for biz_id 15 in BVM."
    )


async def test_errored_on_org_without_info(api, geosearch):
    geosearch.resolve_org.coro.return_value = None

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(
                client=Client(client_id=160),
                order=make_order_for_business_notification(as_proto=True),
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.NO_ORG_INFO,
        description="Can't resolve org with permalink 54321 in Geosearch.",
    )


async def test_no_error_on_unknown_biz_id_if_no_orginfo_required(api, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.side_effect = BvmNotFound(
        "Some BVM error description"
    )

    await api.post(
        url,
        proto=notification_proto(
            certificate_connect_payment=CertificateConnectPayment(
                payment_setup_link="http://some.url"
            )
        ),
        expected_status=200,
    )


async def test_no_error_on_business_without_orgsif_no_orginfo_required(api, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.return_value = []

    await api.post(
        url,
        proto=notification_proto(
            certificate_connect_payment=CertificateConnectPayment(
                payment_setup_link="http://some.url"
            )
        ),
        expected_status=200,
    )


async def test_no_error_on_org_without_info_if_no_orginfo_required(api, geosearch):
    geosearch.resolve_org.coro.return_value = None

    await api.post(
        url,
        proto=notification_proto(
            certificate_connect_payment=CertificateConnectPayment(
                payment_setup_link="http://some.url"
            )
        ),
        expected_status=200,
    )


async def test_errored_on_tuner_exceptions_from_router(api, notification_router):
    notification_router.send_business_notification.side_effect = UnknownBizId(
        "Business not found."
    )

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(
                client=Client(client_id=160),
                order=make_order_for_business_notification(as_proto=True),
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.UNKNOWN_BIZ_ID, description="Business not found.")

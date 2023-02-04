import pytest

from maps_adv.geosmb.telegraphist.proto.v2.common_pb2 import Transport
from maps_adv.geosmb.telegraphist.proto.v2.notifications_for_business_pb2 import (
    Notification,
    Recipient,
    RequestCreated,
)
from maps_adv.geosmb.telegraphist.server.lib.enums import (
    NotificationType,
    Transport as TransportEnum,
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
    )
    kwargs.update(updates)

    return Notification(**kwargs)


@pytest.mark.parametrize(
    "pb_transports, expected_transports",
    [
        ([Transport.EMAIL], [TransportEnum.EMAIL]),
        ([Transport.SMS], [TransportEnum.SMS]),
        ([Transport.EMAIL, Transport.SMS], [TransportEnum.EMAIL, TransportEnum.SMS]),
    ],
)
async def test_will_notify_about_request_creation(
    notification_router,
    api,
    pb_transports,
    expected_transports,
):
    await api.post(
        url,
        proto=notification_proto(
            request_created=RequestCreated(
                details_link="http://request-link.ru",
            ),
            transports=pb_transports,
        ),
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15),
        transports=expected_transports,
        notification_type=NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        notification_details=dict(
            org=dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            details_link="http://request-link.ru",
        ),
    )

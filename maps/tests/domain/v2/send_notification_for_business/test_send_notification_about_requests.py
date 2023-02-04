import pytest

from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport

pytestmark = [pytest.mark.asyncio]


def make_input_params(**updates):
    input_params = dict(
        recipient=dict(
            biz_id=15,
            cabinet_link="http://cabinet.link",
            company_link="http://company.link",
            email="passed_email@yandex.ru",
        ),
        transports=[Transport.EMAIL, Transport.SMS],
    )
    input_params.update(updates)

    return input_params


async def test_uses_notification_router(notification_router, domain):
    expected_notification_details = {
        "org": {
            "cabinet_link": "http://cabinet.link",
            "company_link": "http://company.link",
        },
        "details_link": "http://details.link",
    }

    await domain.send_notification_for_business(
        **make_input_params(
            request_created=dict(details_link="http://details.link"),
        )
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15, email="passed_email@yandex.ru"),
        transports=[Transport.EMAIL, Transport.SMS],
        notification_type=NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        notification_details=expected_notification_details,
    )

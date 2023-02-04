from copy import deepcopy
from datetime import timedelta

import pytest

from maps_adv.geosmb.telegraphist.server.lib.enums import Transport
from maps_adv.geosmb.telegraphist.server.lib.notification_router_v3 import (
    NotificationRouterV3,
    NotificationType,
)


@pytest.fixture
def email_template_codes():
    return {
        "request_created_for_business": "request_created_email_for_business",
        "cart_order_created": "cart_order_created"
    }


@pytest.fixture
def email_transport():
    return {
        "type": Transport.EMAIL,
        "emails": ["foo@yandex-team.ru", "kek@yandex.ru"]
    }


@pytest.fixture
def sms_transport():
    return {
        "type": Transport.SMS,
        "phones": [123456789, 987654321]
    }


@pytest.fixture
def telegram_transport():
    return {
        "type": Transport.TELEGRAM,
        "telegram_uids": [496329590, 496329591]
    }


@pytest.fixture
def notification_router_v3(
    config,
    email_client,
    yasms,
    notify_me,
    yav_client,
    email_template_codes,
):
    return NotificationRouterV3(
        email_client=email_client,
        yasms=yasms,
        email_template_codes=email_template_codes,
        telegram_client=notify_me,
        yav_client=yav_client,
        yav_secret_id="kek-id",
        limit_recipients=config["LIMIT_RECIPIENTS"],
    )


@pytest.fixture
def notification_details():
    org_for_business_notifications = {
        "formatted_address": "Город, Улица, 1",
        "cabinet_link": "http://cabinet.link",
        "company_link": "http://company.link",
        "tz_offset": timedelta(seconds=0),
    }
    return {
        NotificationType.REQUEST_CREATED_FOR_BUSINESS: {
            'org': deepcopy(org_for_business_notifications),
            "details_link": "http://details.link",
        },
        NotificationType.CERTIFICATE_CONNECT_PAYMENT: {},
        NotificationType.CART_ORDER_CREATED: {
            'biz_id': 15,
            'org': dict(
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
            ),
            'details_link': "http://details.link",
            'landing': dict(
                domain="domain",
                url="https://domain.ru"
            ),
            'order': dict(
                id=1,
                final_price="0",
                final_count=0,
                created_at="2020-02-02",
                contacts=dict(
                    name="Name",
                    phone="+78904093221"
                ),
                cart_items=[
                    dict(
                        name="Good",
                        price="1000",
                        min_price="900",
                        description="Description",
                        count=1
                    ),
                    dict(
                        name="Good2",
                        price="2000",
                        min_price="1900",
                        description="Description",
                        count=1
                    )
                ]
            )
        }
    }

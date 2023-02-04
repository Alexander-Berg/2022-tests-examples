from copy import deepcopy
from datetime import timedelta

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    NotificationRouter,
    NotificationType,
)


@pytest.fixture
def email_template_codes():
    return {
        "client": {
            "order_created": "order_created_email",
            "order_reminder": "order_reminder_email",
            "order_changed": "order_changed_email",
            "order_cancelled": "order_cancelled_email",
        },
        "business": {
            "order_created_for_business": "order_created_email_for_business",
            "order_changed_for_business": "order_changed_email_for_business",
            "order_cancelled_for_business": "order_cancelled_email_for_business",
            "certificate_expiring": "certificate_expiring",
            "certificate_expired": "certificate_expired",
            "certificate_connect_payment": "certificate_connect_payment",
            "certificate_rejected": "certificate_rejected",
            "first_certificate_approved": "first_certificate_approved",
            "subsequent_certificate_approved": "subsequent_certificate_approved",
            "certificate_created": "certificate_created",
            "certificate_purchased": "certificate_purchased",
            "request_created_for_business": "request_created_email_for_business",
        },
    }


@pytest.fixture
def notification_router(
    config,
    doorman,
    email_client,
    yasms,
    yav_client,
    sup_client,
    tuner_client,
    email_template_codes,
):
    return NotificationRouter(
        doorman_client=doorman,
        email_client=email_client,
        yasms=yasms,
        yav_client=yav_client,
        tuner_client=tuner_client,
        sup_client=sup_client,
        email_template_codes=email_template_codes,
        limit_recipients=config["LIMIT_RECIPIENTS"],
        yav_secret_id="kek-id",
    )


@pytest.fixture
def notification_details():
    org_for_client_notifications = {
        "permalink": "123456789",
        "name": "Кафе с едой",
        "phone": "+7 (495) 739-70-00",
        "formatted_address": "Город, Улица, 1",
        "url": "http://cafe.ru",
        "categories": ["Общепит", "Ресторан"],
        "tz_offset": timedelta(seconds=0),
    }
    org_for_business_notifications = {
        "formatted_address": "Город, Улица, 1",
        "cabinet_link": "http://cabinet.link",
        "company_link": "http://company.link",
        "tz_offset": timedelta(seconds=0),
    }
    order = {
        "booking_code": "codeofbooking",
        "items": [
            {
                "name": "Штука",
                "booking_timestamp": dt("2020-02-03 14:00:00"),
                "employee_name": "Ваня",
                "cost": {"final_cost": "22.33"},
            }
        ],
        "total_cost": {
            "final_cost": "22.33",
            "cost_before_discounts": "33.44",
            # Values made random intentionally
            "discount": {"percent": "5.5", "value": "10.5"},
        },
    }
    certificate = {
        "name": "Скидочка на стрижечку",
        "link": "http://certificate.link",
        "sales": "100.500",
        "price": "1200.40",
        "validity_period": {
            "valid_from": dt("2020-11-25 18:00:00"),
            "valid_to": dt("2021-11-25 18:00:00"),
        },
    }
    client = {"client_id": 1422}

    return {
        NotificationType.ORDER_CREATED: {
            "org": deepcopy(org_for_client_notifications),
            "order": deepcopy(order),
            "details_link": "localhost/?order_id=123",
        },
        NotificationType.ORDER_REMINDER: {
            "org": deepcopy(org_for_client_notifications),
            "order": deepcopy(order),
            "details_link": "localhost/?order_id=123",
        },
        NotificationType.ORDER_CHANGED: {
            "org": deepcopy(org_for_client_notifications),
            "order": deepcopy(order),
            "details_link": "localhost/?order_id=123",
        },
        NotificationType.ORDER_CANCELLED: {
            "org": deepcopy(org_for_client_notifications),
            "order": deepcopy(order),
            "details_link": "localhost/?order_id=123",
        },
        NotificationType.ORDER_CREATED_FOR_BUSINESS: {
            "org": deepcopy(org_for_business_notifications),
            "order": deepcopy(order),
            "client": deepcopy(client),
            "details_link": "localhost/?order_id=123",
        },
        NotificationType.ORDER_CHANGED_FOR_BUSINESS: {
            "org": deepcopy(org_for_business_notifications),
            "order": deepcopy(order),
            "client": deepcopy(client),
            "details_link": "localhost/?order_id=123",
        },
        NotificationType.ORDER_CANCELLED_FOR_BUSINESS: {
            "org": deepcopy(org_for_business_notifications),
            "order": deepcopy(order),
            "client": deepcopy(client),
            "details_link": "localhost/?order_id=123",
        },
        NotificationType.CERTIFICATE_CONNECT_PAYMENT: {
            "org": deepcopy(org_for_business_notifications),
            "payment_setup_link": "http://payment.setup/link",
        },
        NotificationType.CERTIFICATE_REJECTED: {
            "org": deepcopy(org_for_business_notifications),
            "certificate": deepcopy(certificate),
        },
        NotificationType.CERTIFICATE_EXPIRED: {
            "org": deepcopy(org_for_business_notifications),
            "certificate": deepcopy(certificate),
        },
        NotificationType.CERTIFICATE_EXPIRING: {
            "org": deepcopy(org_for_business_notifications),
            "certificate": deepcopy(certificate),
        },
        NotificationType.FIRST_CERTIFICATE_APPROVED: {
            "org": deepcopy(org_for_business_notifications),
            "certificate": deepcopy(certificate),
        },
        NotificationType.SUBSEQUENT_CERTIFICATE_APPROVED: {
            "org": deepcopy(org_for_business_notifications),
            "certificate": deepcopy(certificate),
        },
        NotificationType.CERTIFICATE_CREATED: {
            "org": deepcopy(org_for_business_notifications),
            "certificate": deepcopy(certificate),
        },
        NotificationType.CERTIFICATE_PURCHASED: {
            "org": deepcopy(org_for_business_notifications),
            "certificate": deepcopy(certificate),
            "cashier_link": "http://cashier.link",
        },
        NotificationType.REQUEST_CREATED_FOR_BUSINESS: {
            "org": deepcopy(org_for_business_notifications),
            "details_link": "http://details.link",
        },
    }

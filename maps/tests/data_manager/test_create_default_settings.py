from datetime import datetime

import pytest
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "emails, expected_emails, phone",
    (
        (None, [], None),
        (["email1@ya.ru", "email2@ya.ru"], ["email1@ya.ru", "email2@ya.ru"], 900),
    ),
)
async def test_returns_created_settings(emails, expected_emails, phone, dm):
    result = await dm.create_default_settings(biz_id=123, emails=emails, phone=phone)

    assert result == dict(
        emails=expected_emails,
        phone=phone,
        notifications=dict(
            order_created=True,
            order_cancelled=True,
            order_changed=True,
            certificate_notifications=True,
            request_created=True,
        ),
        sms_notifications=dict(
            request_created=True,
        ),
        booking=dict(
            enabled=True,
            slot_interval=30,
            online_settings=dict(export_to_ya_services=False),
        ),
        requests=dict(
            enabled=True,
            button_text="Отправить заявку",
        ),
        general_schedule_id=None,
    )


@pytest.mark.parametrize(
    "emails, expected_emails, phone",
    (
        (None, [], None),
        (["email1@ya.ru", "email2@ya.ru"], ["email1@ya.ru", "email2@ya.ru"], 900),
    ),
)
async def test_creates_record_in_db(emails, expected_emails, phone, dm, factory):
    await dm.create_default_settings(biz_id=123, emails=emails, phone=phone)

    row = await factory.fetch_org_settings(biz_id=123)
    assert row == dict(
        id=Any(int),
        biz_id=123,
        emails=expected_emails,
        phone=phone,
        contacts=dict(),
        notifications=dict(
            order_created=True,
            order_cancelled=True,
            order_changed=True,
            certificate_notifications=True,
            request_created=True,
        ),
        sms_notifications=dict(
            request_created=True,
        ),
        booking_enabled=True,
        booking_slot_interval=30,
        booking_export_to_ya_services=False,
        requests_enabled=True,
        requests_button_text="Отправить заявку",
        general_schedule_id=None,
        created_at=Any(datetime),
    )


async def test_returns_existing_settings_if_settings_already_exist(dm, factory):
    await factory.create_org_settings(
        biz_id=123, emails=["email1@ya.ru", "email2@ya.ru"], phone=900
    )

    got = await dm.create_default_settings(
        biz_id=123, emails=["email@ya.ru"], phone=None
    )

    assert got == {
        "booking": {
            "enabled": False,
            "online_settings": {"export_to_ya_services": True},
            "slot_interval": 45,
        },
        "requests": {
            "enabled": False,
            "button_text": "Кнопка",
        },
        "emails": ["email1@ya.ru", "email2@ya.ru"],
        "phone": 900,
        "notifications": {
            "certificate_notifications": False,
            "order_cancelled": False,
            "order_changed": True,
            "order_created": True,
            "request_created": True,
        },
        "sms_notifications": {
            "request_created": False,
        },
        "general_schedule_id": None,
    }

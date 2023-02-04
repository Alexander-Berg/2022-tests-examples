import pytest
from smb.common.testing_utils import Any, dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_new_org_settings(dm, factory):
    await factory.create_org_settings(biz_id=123)

    result = await dm.update_settings(
        biz_id=123,
        emails=["email3@ya.ru"],
        phone=800,
        notifications=dict(
            order_created=False,
            order_cancelled=True,
            order_changed=False,
            certificate_notifications=True,
            request_created=False,
        ),
        sms_notifications=dict(
            request_created=True,
        ),
        booking=dict(
            enabled=True,
            slot_interval=20,
            online_settings=dict(export_to_ya_services=False),
        ),
        requests=dict(
            enabled=True,
            button_text="Другая кнопка",
        ),
        general_schedule_id=100500,
    )

    assert result == dict(
        emails=["email3@ya.ru"],
        phone=800,
        notifications=dict(
            order_created=False,
            order_cancelled=True,
            order_changed=False,
            certificate_notifications=True,
            request_created=False,
        ),
        sms_notifications=dict(
            request_created=True,
        ),
        booking=dict(
            enabled=True,
            slot_interval=20,
            online_settings=dict(export_to_ya_services=False),
        ),
        requests=dict(
            enabled=True,
            button_text="Другая кнопка",
        ),
        general_schedule_id=100500,
    )


@pytest.mark.parametrize(
    "data, updated",
    (
        [{"emails": ["email3@ya.ru"]}, {"emails": ["email3@ya.ru"]}],
        [{"phone": 800}, {"phone": 800}],
        [{"general_schedule_id": 100500}, {"general_schedule_id": 100500}],
        [
            {
                "notifications": dict(
                    order_created=False,
                    order_cancelled=True,
                    order_changed=False,
                    certificate_notifications=True,
                    request_created=False,
                )
            },
            {
                "notifications": dict(
                    order_created=False,
                    order_cancelled=True,
                    order_changed=False,
                    certificate_notifications=True,
                    request_created=False,
                )
            },
        ],
        [
            {"sms_notifications": dict(request_created=True)},
            {"sms_notifications": dict(request_created=True)},
        ],
        [
            {
                "booking": dict(
                    enabled=True,
                    slot_interval=20,
                    online_settings=dict(export_to_ya_services=False),
                )
            },
            {
                "booking_enabled": True,
                "booking_slot_interval": 20,
                "booking_export_to_ya_services": False,
            },
        ],
        [
            {"requests": dict(enabled=True, button_text="Другая кнопка")},
            {"requests_enabled": True, "requests_button_text": "Другая кнопка"},
        ],
    ),
)
async def test_updates_only_passed_columns(data, updated, dm, factory):
    await factory.create_org_settings(biz_id=123, created_at=dt("2020-11-03 11:00:00"))

    await dm.update_settings(biz_id=123, **data)

    expected = dict(
        id=Any(int),
        biz_id=123,
        emails=["email1@ya.ru", "email2@ya.ru"],
        phone=900,
        contacts=dict(),
        notifications=dict(
            order_created=True,
            order_cancelled=False,
            order_changed=True,
            certificate_notifications=False,
            request_created=True,
        ),
        sms_notifications=dict(
            request_created=False,
        ),
        booking_enabled=False,
        booking_slot_interval=45,
        booking_export_to_ya_services=True,
        requests_enabled=False,
        requests_button_text="Кнопка",
        created_at=dt("2020-11-03 11:00:00"),
        general_schedule_id=None,
    )
    expected.update(updated)
    result = await factory.fetch_org_settings(biz_id=123)
    assert result == expected


async def test_updates_all_columns_if_passed(dm, factory):
    await factory.create_org_settings(biz_id=123, created_at=dt("2020-11-03 11:00:00"))

    await dm.update_settings(
        biz_id=123,
        emails=["email3@ya.ru"],
        phone=800,
        notifications=dict(
            order_created=False,
            order_cancelled=True,
            order_changed=False,
            certificate_notifications=True,
            request_created=False,
        ),
        sms_notifications=dict(
            request_created=True,
        ),
        booking=dict(
            enabled=True,
            slot_interval=20,
            online_settings=dict(export_to_ya_services=False),
        ),
        requests=dict(
            enabled=True,
            button_text="Другая кнопка",
        ),
        general_schedule_id=100500,
    )

    result = await factory.fetch_org_settings(biz_id=123)
    assert result == dict(
        id=Any(int),
        biz_id=123,
        emails=["email3@ya.ru"],
        contacts=dict(),
        phone=800,
        notifications=dict(
            order_created=False,
            order_cancelled=True,
            order_changed=False,
            certificate_notifications=True,
            request_created=False,
        ),
        sms_notifications=dict(
            request_created=True,
        ),
        booking_enabled=True,
        booking_slot_interval=20,
        booking_export_to_ya_services=False,
        requests_enabled=True,
        requests_button_text="Другая кнопка",
        created_at=dt("2020-11-03 11:00:00"),
        general_schedule_id=100500,
    )

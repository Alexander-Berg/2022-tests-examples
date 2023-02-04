from datetime import datetime, timezone
from typing import Dict, Iterable, List, Optional, Sequence

import pytest
from asyncpg import Connection

from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag


class Factory:
    _con: Connection

    def __init__(self, con: Connection):
        self._con = con

    async def create_org_settings(
        self,
        biz_id: int = 123,
        emails: Sequence[str] = ("email1@ya.ru", "email2@ya.ru"),
        phone: Optional[int] = 900,
        notifications: Dict[str, bool] = None,
        sms_notifications: Dict[str, bool] = None,
        booking_enabled: bool = False,
        booking_slot_interval: int = 45,
        booking_export_to_ya_services: bool = True,
        requests_enabled: bool = False,
        requests_button_text: str = "Кнопка",
        general_schedule_id: Optional[int] = None,
        created_at: Optional[datetime] = None,
    ):
        if created_at is None:
            created_at = datetime.now(tz=timezone.utc)
        if notifications is None:
            notifications = dict(
                order_created=True,
                order_cancelled=False,
                order_changed=True,
                certificate_notifications=False,
                request_created=True,
            )
        if sms_notifications is None:
            sms_notifications = dict(
                request_created=False,
            )

        await self._con.execute(
            """
            INSERT INTO settings_data (
                biz_id, emails, phone, notifications, sms_notifications,
                booking_enabled, booking_slot_interval, booking_export_to_ya_services,
                requests_enabled, requests_button_text, general_schedule_id, created_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
            """,
            biz_id,
            emails,
            phone,
            notifications,
            sms_notifications,
            booking_enabled,
            booking_slot_interval,
            booking_export_to_ya_services,
            requests_enabled,
            requests_button_text,
            general_schedule_id,
            created_at,
        )

    async def create_org_settings_v2(
        self,
        biz_id: int = 123,
        emails: Sequence[str] = ("email1@ya.ru", "email2@ya.ru"),
        phones: Sequence[int] = (900),
        telegram_logins: Sequence[str] = ("test", "foo"),
        notifications: Dict[str, bool] = None,
        sms_notifications: Dict[str, bool] = None,
        booking_enabled: bool = False,
        booking_slot_interval: int = 45,
        booking_export_to_ya_services: bool = True,
        requests_enabled: bool = False,
        requests_button_text: str = "Кнопка",
        general_schedule_id: Optional[int] = None,
        created_at: Optional[datetime] = None,
    ):
        if created_at is None:
            created_at = datetime.now(tz=timezone.utc)
        if notifications is None:
            notifications = dict(
                order_created=True,
                order_cancelled=False,
                order_changed=True,
                certificate_notifications=False,
                request_created=True,
            )
        if sms_notifications is None:
            sms_notifications = dict(
                request_created=False,
            )

        await self._con.execute(
            """
            INSERT INTO settings_data (
                biz_id, contacts, notifications, sms_notifications,
                booking_enabled, booking_slot_interval, booking_export_to_ya_services,
                requests_enabled, requests_button_text, general_schedule_id, created_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
            """,
            biz_id,
            dict(emails=emails, phones=phones, telegram_logins=telegram_logins),
            notifications,
            sms_notifications,
            booking_enabled,
            booking_slot_interval,
            booking_export_to_ya_services,
            requests_enabled,
            requests_button_text,
            general_schedule_id,
            created_at,
        )

    async def fetch_org_settings(self, biz_id: int = 123):
        row = await self._con.fetchrow(
            """
            SELECT *
            FROM settings_data
            WHERE biz_id = $1
            """,
            biz_id,
        )

        return dict(row) if row else None

    async def create_permission(
        self,
        biz_id: int = 123,
        passport_uid: int = 456,
        flags: Iterable[PermissionFlag] = [PermissionFlag.READ_REQUESTS]
    ):
        await self._con.execute(
            """
            INSERT INTO user_permissions (
                biz_id, passport_uid, flags
            )
            VALUES ($1, $2, $3)
            """,
            biz_id,
            passport_uid,
            flags,
        )

    async def fetch_permissions(self, biz_id: int = 123):
        rows = await self._con.fetch(
            """
            SELECT *
            FROM user_permissions
            WHERE biz_id = $1
            """,
            biz_id,
        )

        return [dict(row) for row in rows]

    async def create_telegram_user(
        self,
        user_login: str = 'test',
        user_id: int = 123456
    ):
        await self._con.execute(
            """
            INSERT INTO telegram_users_data (
                user_login, user_id
            )
            VALUES ($1, $2)
            """,
            user_login,
            user_id
        )

    async def fetch_telegram_users(self, user_logins: List[str] = 123):
        rows = await self._con.fetch(
            """
            SELECT user_id, user_login, active
            FROM telegram_users_data
            WHERE user_login = ANY($1)
            """,
            user_logins,
        )

        return [dict(row) for row in rows]


@pytest.fixture
def factory(con):
    return Factory(con)

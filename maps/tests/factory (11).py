from datetime import datetime, timezone
from typing import List, Optional

import pytest
from asyncpg import Connection

from maps_adv.geosmb.scenarist.server.lib.enums import (
    MessageType,
    ScenarioName,
    SubscriptionStatus,
)


class Factory:
    _con: Connection

    def __init__(self, con: Connection):
        self._con = con

    async def create_subscription(
        self,
        subscription_id: Optional[int] = None,
        scenario_name: ScenarioName = ScenarioName.DISCOUNT_FOR_LOST,
        biz_id: int = 123,
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
        coupon_id: int = 456,
        created_at: Optional[datetime] = None,
    ) -> int:
        sql = """
            INSERT INTO subscriptions (
                id,
                scenario_name,
                biz_id,
                status,
                coupon_id,
                created_at
            )
            SELECT COALESCE($1, nextval('subscriptions_id_seq')), $2, $3, $4, $5, $6
            RETURNING id
        """
        created_at = created_at if created_at else datetime.now(tz=timezone.utc)
        sub_id = await self._con.fetchval(
            sql, subscription_id, scenario_name, biz_id, status, coupon_id, created_at
        )
        await self.create_subscription_version(
            subscription_id=sub_id,
            coupon_id=coupon_id,
            status=status,
            created_at=created_at,
        )

        return sub_id

    async def create_subscription_version(
        self,
        subscription_id: int,
        coupon_id: int = 456,
        status: Optional[SubscriptionStatus] = SubscriptionStatus.ACTIVE,
        created_at: Optional[datetime] = None,
    ):
        sql = """
            INSERT INTO subscription_versions (
                subscription_id,
                coupon_id,
                status,
                created_at
            )
            SELECT $1, $2, $3, $4
        """
        await self._con.execute(
            sql,
            subscription_id,
            coupon_id,
            status,
            created_at if created_at else datetime.now(tz=timezone.utc),
        )

    async def retrieve_business_subscriptions(self, biz_id: int) -> List[dict]:
        sql = """
            SELECT id,
                biz_id,
                scenario_name,
                status,
                coupon_id
            FROM subscriptions
            WHERE biz_id=$1
            ORDER BY scenario_name
        """

        got = await self._con.fetch(sql, biz_id)

        return list(map(dict, got))

    async def retrieve_subscription(self, subscription_id: int) -> dict:
        sql = """
            SELECT id,
                biz_id,
                scenario_name,
                status,
                coupon_id
            FROM subscriptions
            WHERE id=$1
        """

        got = await self._con.fetchrow(sql, subscription_id)

        return dict(got)

    async def retrieve_subscription_versions(self, subscription_id: int) -> List[dict]:
        sql = """
            SELECT *
            FROM subscription_versions
            WHERE subscription_id=$1
            ORDER BY id DESC
        """

        got = await self._con.fetch(sql, subscription_id)

        return list(map(dict, got))

    async def retrieve_all_messages(self) -> List[dict]:
        sql = """
            SELECT *
            FROM messages
            ORDER BY created_at, id
        """

        got = await self._con.fetch(sql)

        return list(map(dict, got))

    async def retrieve_message_by_id(
        self, message_id: int, fields: Optional[List[str]] = None
    ) -> Optional[dict]:
        select_fields = ", ".join(fields) if fields is not None else "*"

        sql = f"""
            SELECT {select_fields}
            FROM messages
            WHERE id = $1
        """

        got = await self._con.fetchrow(sql, message_id)

        return dict(got) if got else None

    async def create_message(
        self,
        time_to_send: Optional[datetime] = None,
        message_anchor: str = "message_anchor_0",
        message_type: MessageType = MessageType.EMAIL,
        message_meta: Optional[dict] = None,
        doorman_ids: Optional[List[int]] = None,
        promoter_ids: Optional[List[int]] = None,
        scenario_names: Optional[List[ScenarioName]] = None,
        created_at: Optional[datetime] = None,
        processed_at: Optional[datetime] = None,
        processed_meta: Optional[dict] = None,
        error: Optional[str] = None,
    ) -> dict:
        sql = """
            INSERT INTO messages (
                time_to_send, message_anchor, message_type, message_meta,
                doorman_ids, promoter_ids, scenario_names,
                created_at, processed_at, processed_meta, error
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
            RETURNING *
        """

        got = await self._con.fetchrow(
            sql,
            _coalesce(time_to_send, datetime.now(tz=timezone.utc)),
            message_anchor,
            message_type,
            _coalesce(message_meta, {}),
            _coalesce(doorman_ids, []),
            _coalesce(promoter_ids, []),
            list(
                _coalesce(scenario_names, [ScenarioName.THANK_THE_LOYAL]),
            ),
            _coalesce(created_at, datetime.now(tz=timezone.utc)),
            processed_at,
            processed_meta,
            error,
        )
        return dict(got)

    async def create_certificate_mailing_stats(
        self,
        *,
        biz_id: int = 123,
        coupon_id: int = 456,
        scenario_name: ScenarioName = ScenarioName.DISCOUNT_FOR_DISLOYAL,
        sent: int = 15,
        clicked: int = 3,
        opened: int = 8,
        sent_date: str = "2020-05-10",
        created_at: Optional[datetime] = None,
    ) -> int:
        sql = """
            INSERT INTO certificate_mailing_stats (
                biz_id, coupon_id, scenario_name, sent,
                clicked, opened, sent_date, created_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            RETURNING id
        """

        return await self._con.fetchval(
            sql,
            biz_id,
            coupon_id,
            scenario_name,
            sent,
            clicked,
            opened,
            sent_date,
            _coalesce(created_at, datetime.now(tz=timezone.utc)),
        )


@pytest.fixture
async def factory(con):
    return Factory(con)


def _coalesce(*args):
    for arg in args:
        if arg is not None:
            return arg

from datetime import datetime, timezone
from typing import List, Optional, Union

import pytest
from asyncpg import Connection

from maps_adv.geosmb.promoter.server.lib.enums import (
    EventDataSource,
    EventType,
    LeadDataSource,
    Source,
)

default_ts = datetime.now(tz=timezone.utc)


class Factory:
    _con: Connection

    def __init__(self, con: Connection):
        self._con = con

    async def create_lead(
        self,
        *,
        lead_id: Optional[int] = None,
        biz_id: int = 123,
        passport_uid: Optional[str] = "235291",
        device_id: Optional[str] = None,
        yandex_uid: Optional[str] = None,
        data_source: LeadDataSource = LeadDataSource.YT,
        source: Optional[Source] = Source.EXTERNAL_ADVERT,
        name: str = "username_1",
        created_at: Optional[datetime] = None,
    ) -> int:
        created_lead_id = await self._create_lead(
            lead_id=lead_id,
            biz_id=biz_id,
            passport_uid=passport_uid,
            device_id=device_id,
            yandex_uid=yandex_uid,
            data_source=data_source,
            name=name,
            source=source,
            created_at=created_at,
        )

        await self._create_revision(
            lead_id=created_lead_id,
            biz_id=biz_id,
            passport_uid=passport_uid,
            device_id=device_id,
            yandex_uid=yandex_uid,
            data_source=data_source,
            name=name,
        )

        return created_lead_id

    async def create_event(
        self,
        *,
        lead_id: int,
        event_type: EventType,
        events_amount: int = 1,
        event_value: Optional[Union[int, str]] = None,
        event_timestamp: datetime = default_ts,
        data_source: EventDataSource = EventDataSource.SPRAV,
        source: Optional[Source] = Source.EXTERNAL_ADVERT,
        refresh_stats: bool = True,
    ) -> None:
        biz_id = await self._fetch_lead_biz_id(lead_id)

        sql = """
            INSERT INTO lead_events (
                lead_id,
                biz_id,
                event_type,
                events_amount,
                event_value,
                event_timestamp,
                data_source,
                source,
                created_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9);
        """
        await self._con.execute(
            sql,
            lead_id,
            biz_id,
            event_type,
            events_amount,
            str(event_value) if event_value is not None else event_value,
            event_timestamp,
            data_source,
            source,
            datetime.now(tz=timezone.utc),
        )

        if refresh_stats:
            await self.recalculate_events_stat()

    async def create_lead_with_event(
        self,
        *,
        lead_id: Optional[int] = None,
        biz_id: int = 123,
        passport_uid: Optional[str] = "235291",
        device_id: Optional[str] = None,
        yandex_uid: Optional[str] = None,
        lead_data_source: LeadDataSource = LeadDataSource.YT,
        name: str = "username_1",
        event_type: EventType = EventType.OPEN_SITE,
        event_value: Optional[Union[int, str]] = None,
        events_amount: int = 1,
        event_timestamp: datetime = default_ts,
        event_data_source: EventDataSource = EventDataSource.SPRAV,
        refresh_stats: bool = True,
    ) -> int:
        lead_id = await self.create_lead(
            lead_id=lead_id,
            biz_id=biz_id,
            passport_uid=passport_uid,
            device_id=device_id,
            yandex_uid=yandex_uid,
            data_source=lead_data_source,
            name=name,
        )
        await self.create_event(
            lead_id=lead_id,
            event_type=event_type,
            event_value=event_value,
            events_amount=events_amount,
            event_timestamp=event_timestamp,
            data_source=event_data_source,
            refresh_stats=refresh_stats,
        )

        return lead_id

    async def create_lead_with_events(
        self,
        *,
        lead_id: Optional[int] = None,
        biz_id: int = 123,
        passport_uid: Optional[str] = "235291",
        device_id: Optional[str] = None,
        yandex_uid: Optional[str] = None,
        lead_data_source: LeadDataSource = LeadDataSource.YT,
        lead_source: Source = Source.EXTERNAL_ADVERT,
        name: str = "username_1",
        review_rating: Optional[Union[int, str]] = None,
        clicks_on_phone: Optional[int] = 0,
        site_opens: Optional[int] = 0,
        make_routes: Optional[int] = 0,
        view_working_hours: Optional[int] = 0,
        view_entrances: Optional[int] = 0,
        cta_button_click: Optional[int] = 0,
        favourite_click: Optional[int] = 0,
        location_sharing: Optional[int] = 0,
        booking_section_interaction: Optional[int] = 0,
        showcase_product_click: Optional[int] = 0,
        promo_to_site: Optional[int] = 0,
        geoproduct_button_click: Optional[int] = 0,
        last_activity_timestamp: Optional[datetime] = default_ts,
        refresh_stats: bool = True,
    ) -> int:
        lead_id = await self.create_lead(
            lead_id=lead_id,
            biz_id=biz_id,
            passport_uid=passport_uid,
            device_id=device_id,
            yandex_uid=yandex_uid,
            data_source=lead_data_source,
            source=lead_source,
            name=name,
        )
        await self.generate_events_for_lead(
            lead_id=lead_id,
            review_rating=review_rating,
            clicks_on_phone=clicks_on_phone,
            site_opens=site_opens,
            make_routes=make_routes,
            view_working_hours=view_working_hours,
            view_entrances=view_entrances,
            cta_button_click=cta_button_click,
            favourite_click=favourite_click,
            location_sharing=location_sharing,
            booking_section_interaction=booking_section_interaction,
            showcase_product_click=showcase_product_click,
            promo_to_site=promo_to_site,
            geoproduct_button_click=geoproduct_button_click,
            last_activity_timestamp=last_activity_timestamp,
            refresh_stats=refresh_stats,
        )

        return lead_id

    async def generate_events_for_lead(
        self,
        *,
        lead_id: int,
        review_rating: Optional[Union[int, str]] = None,
        last_activity_timestamp: Optional[datetime] = default_ts,
        refresh_stats: bool = True,
        **events,
    ) -> None:
        events_map = {
            "clicks_on_phone": EventType.CLICK_ON_PHONE,
            "site_opens": EventType.OPEN_SITE,
            "make_routes": EventType.MAKE_ROUTE,
            "view_working_hours": EventType.VIEW_WORKING_HOURS,
            "view_entrances": EventType.VIEW_ENTRANCES,
            "cta_button_click": EventType.CTA_BUTTON_CLICK,
            "favourite_click": EventType.FAVOURITE_CLICK,
            "location_sharing": EventType.LOCATION_SHARING,
            "booking_section_interaction": EventType.BOOKING_SECTION_INTERACTION,
            "showcase_product_click": EventType.SHOWCASE_PRODUCT_CLICK,
            "promo_to_site": EventType.PROMO_TO_SITE,
            "geoproduct_button_click": EventType.GEOPRODUCT_BUTTON_CLICK,
        }

        if review_rating:
            await self.create_event(
                lead_id=lead_id,
                event_type=EventType.REVIEW,
                event_value=review_rating,
                event_timestamp=last_activity_timestamp,
                refresh_stats=refresh_stats,
            )

        for event_name, event_count in events.items():
            event_type = events_map[event_name]
            for _ in range(event_count):
                await self.create_event(
                    lead_id=lead_id,
                    event_type=event_type,
                    event_timestamp=last_activity_timestamp,
                    refresh_stats=refresh_stats,
                )

    async def list_leads(self) -> List[dict]:
        rows = await self._con.fetch(
            """
                SELECT *
                FROM leads
                ORDER BY id
            """
        )

        return [dict(row) for row in rows]

    async def retrieve_lead(self, lead_id: int) -> dict:
        sql = """
            SELECT
                biz_id,
                passport_uid,
                device_id,
                yandex_uid,
                data_source,
                source,
                name
            FROM leads
            WHERE id=$1
        """
        row = await self._con.fetchrow(sql, lead_id)

        return dict(row) if row else None

    async def retrieve_last_revision(self, lead_id: int) -> dict:
        sql = """
            SELECT
                lead_id,
                biz_id,
                passport_uid,
                device_id,
                yandex_uid,
                data_source,
                name
            FROM lead_revisions
            WHERE lead_id = $1
            ORDER BY created_at DESC
            LIMIT 1
        """
        row = await self._con.fetchrow(sql, lead_id)

        return dict(row) if row else None

    async def list_lead_events(self, lead_id: int) -> List[dict]:
        sql = """
            SELECT *
            FROM lead_events
            WHERE lead_id = $1
        """
        rows = await self._con.fetch(sql, lead_id)

        return [dict(row) for row in rows]

    async def _create_lead(
        self,
        lead_id: Optional[int],
        biz_id: int,
        passport_uid: Optional[str],
        device_id: Optional[str],
        yandex_uid: Optional[str],
        data_source: LeadDataSource,
        source: Optional[Source],
        name: str,
        created_at: Optional[datetime],
    ) -> int:
        sql = """
            INSERT INTO leads (
                id,
                biz_id,
                passport_uid,
                device_id,
                yandex_uid,
                data_source,
                source,
                name,
                created_at
            )
            VALUES (
                coalesce($1, nextval('leads_id_seq')),
                $2, $3, $4, $5, $6, $7, $8, $9
            )
            RETURNING id
        """

        return await self._con.fetchval(
            sql,
            lead_id,
            biz_id,
            passport_uid,
            device_id,
            yandex_uid,
            data_source,
            source,
            name,
            created_at if created_at else datetime.now(tz=timezone.utc),
        )

    async def _create_revision(
        self,
        lead_id: Optional[int],
        biz_id: int,
        passport_uid: Optional[str],
        device_id: Optional[str],
        yandex_uid: Optional[str],
        data_source: LeadDataSource,
        name: str,
    ) -> None:
        sql = """
            INSERT INTO lead_revisions (
                lead_id,
                biz_id,
                passport_uid,
                device_id,
                yandex_uid,
                data_source,
                name,
                created_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
        """

        await self._con.execute(
            sql,
            lead_id,
            biz_id,
            passport_uid,
            device_id,
            yandex_uid,
            data_source,
            name,
            datetime.now(tz=timezone.utc),
        )

    async def list_events(self) -> List[dict]:
        rows = await self._con.fetch(
            """
                SELECT *
                FROM lead_events
                ORDER BY created_at, event_timestamp
            """
        )

        return [dict(row) for row in rows]

    async def fetch_all_events_stat(self) -> List[dict]:
        rows = await self._con.fetch(
            "SELECT * FROM events_stat_precalced ORDER BY lead_id"
        )

        return [dict(row) for row in rows] if rows else []

    async def fetch_lead_events_stat(self, lead_id) -> dict:
        row = await self._con.fetchrow(
            "SELECT * FROM events_stat_precalced WHERE lead_id = $1", lead_id
        )

        return dict(row) if row else None

    async def recalculate_events_stat(self):
        await self._con.execute(
            """
            TRUNCATE events_stat_precalced;

            WITH lead_events_sumed AS (
                SELECT
                    lead_id,
                    event_timestamp AS ts,
                    sum(events_amount) 
                        OVER (PARTITION BY lead_id ORDER BY event_timestamp DESC)
                        AS events_amount_sum
                FROM lead_events
            ),
            lead_last_3_events_ts AS (
                SELECT
                    lead_id,
                    max(ts) AS last_3_events_timestamp
                FROM lead_events_sumed
                WHERE events_amount_sum >= 3
                GROUP BY lead_id
            )
            INSERT INTO events_stat_precalced (
                biz_id,
                lead_id,
                total_clicks_on_phone,
                total_site_opens,
                total_make_routes,
                total_view_working_hours,
                total_view_entrances,
                total_cta_button_click,
                total_favourite_click,
                total_location_sharing,
                total_booking_section_interaction,
                total_showcase_product_click,
                total_promo_to_site,
                total_geoproduct_button_click,
                last_review_rating,
                last_activity_timestamp,
                last_3_events_timestamp
            )
            SELECT
                biz_id,
                lead_id,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='CLICK_ON_PHONE'),0
                ) AS total_clicks_on_phone,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='OPEN_SITE'), 0
                ) AS total_site_opens,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='MAKE_ROUTE'), 0
                ) AS total_make_routes,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='VIEW_WORKING_HOURS'), 0
                ) AS total_view_working_hours,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='VIEW_ENTRANCES'), 0
                ) AS total_view_entrances,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='CTA_BUTTON_CLICK'), 0
                ) AS total_cta_button_click,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='FAVOURITE_CLICK'), 0
                ) AS total_favourite_click,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='LOCATION_SHARING'), 0
                ) AS total_location_sharing,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='BOOKING_SECTION_INTERACTION'), 0
                ) AS total_booking_section_interaction,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='SHOWCASE_PRODUCT_CLICK'), 0
                ) AS total_showcase_product_click,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='PROMO_TO_SITE'), 0
                ) AS total_promo_to_site,
                COALESCE(
                    SUM(events_amount) FILTER (WHERE event_type='GEOPRODUCT_BUTTON_CLICK'), 0
                ) AS total_geoproduct_button_click,
                (
                    array_agg(event_value ORDER BY event_timestamp DESC NULLS LAST)
                    FILTER (WHERE event_type = 'REVIEW')
                )[1] AS last_review_rating,
                max(event_timestamp) AS last_activity_timestamp,
                max(lead_last_3_events_ts.last_3_events_timestamp) AS last_3_events_timestamp
            FROM lead_events
            LEFT JOIN lead_last_3_events_ts USING (lead_id)
            GROUP BY biz_id, lead_id
        """  # noqa
        )

    async def _fetch_lead_biz_id(self, lead_id: int) -> Optional[int]:
        sql = """
            SELECT biz_id
            FROM leads
            WHERE id = $1
        """

        return await self._con.fetchval(sql, lead_id)


@pytest.fixture
async def factory(con):
    return Factory(con)

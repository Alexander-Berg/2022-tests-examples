from datetime import datetime, timezone
from decimal import Decimal
from operator import attrgetter
from typing import List, Optional

import pytest
import pytz
from asyncpg import Connection

from maps_adv.adv_store.api.schemas.enums import (
    ActionTypeEnum,
    CampaignDirectModerationStatusEnum,
    CampaignDirectModerationWorkflowEnum,
    CampaignEventTypeEnum,
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    OrderSizeEnum,
    PlatformEnum,
    PublicationEnvEnum,
    ResolveUriTargetEnum,
    RubricEnum,
)
from maps_adv.common.helpers import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum


class Factory:
    def __init__(self, con: Connection):
        self.con = con
        self._codecs_set = False

    async def _insert_billing_data(
        self,
        cpm: Optional[dict] = None,
        cpa: Optional[dict] = None,
        fix: Optional[dict] = None,
    ) -> int:
        if len(list(filter(None, [cpm, cpa, fix]))) != 1:
            raise Exception("Exactly one of cpm, fix and cpa required")

        if cpm:
            return await self._insert_billing_cpm(cpm)
        elif cpa:
            return await self._insert_billing_cpa(cpa)
        elif fix:
            return await self._insert_billing_fix(fix)

    async def _insert_billing_cpm(self, cpm):
        return await self.con.fetchval(
            """
            WITH cpm_rows AS (
                INSERT INTO billing_cpm (cost, budget, daily_budget, auto_daily_budget)
                VALUES ($1, $2, $3, $4)
                RETURNING id
            )
            INSERT INTO campaign_billing (cpm_id)
            SELECT id FROM cpm_rows
            RETURNING id
            """,
            cpm["cost"],
            cpm["budget"],
            cpm["daily_budget"],
            cpm["auto_daily_budget"],
        )

    async def _insert_billing_cpa(self, cpa):
        return await self.con.fetchval(
            """
            WITH cpa_rows AS (
                INSERT INTO billing_cpa (cost, budget, daily_budget, auto_daily_budget)
                VALUES ($1, $2, $3, $4)
                RETURNING id
            )
            INSERT INTO campaign_billing (cpa_id)
            SELECT id FROM cpa_rows
            RETURNING id
            """,
            cpa["cost"],
            cpa["budget"],
            cpa["daily_budget"],
            cpa["auto_daily_budget"],
        )

    async def _insert_billing_fix(self, fix):
        return await self.con.fetchval(
            """
            WITH fix_rows AS (
                INSERT INTO billing_fix (time_interval, cost)
                VALUES ($1, $2)
                RETURNING id
            )
            INSERT INTO campaign_billing (fix_id)
            SELECT id FROM fix_rows
            RETURNING id
            """,
            fix["time_interval"].name,
            fix["cost"],
        )

    async def _insert_actions(self, campaign_id: int, actions: list):
        funcs = {
            "search": self._insert_action_search,
            "open_site": self._insert_action_open_site,
            "phone_call": self._insert_action_phone_call,
            "download_app": self._insert_action_download_app,
            "promocode": self._insert_action_promocode,
            "resolve_uri": self._insert_action_resolve_uri,
            "add_point_to_route": self._insert_action_add_point_to_route,
        }

        for action in actions:
            action_data = action.copy()
            action_type = action_data.pop("type_")
            await funcs[action_type](campaign_id, action_data)

    async def _insert_action_search(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO action_search
                (campaign_id, title, organizations, history_text, main)
            VALUES ($1, $2, $3, $4, $5)
            """,
            campaign_id,
            data["title"],
            data["organizations"],
            data["history_text"],
            data["main"],
        )

    async def _insert_action_open_site(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO action_open_site (campaign_id, title, url, main)
            VALUES ($1, $2, $3, $4)
            """,
            campaign_id,
            data["title"],
            data["url"],
            data["main"],
        )

    async def _insert_action_phone_call(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO action_phone_call (campaign_id, title, phone, main)
            VALUES ($1, $2, $3, $4)
            """,
            campaign_id,
            data["title"],
            data["phone"],
            data["main"],
        )

    async def _insert_action_download_app(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO action_download_app
                (campaign_id, title, google_play_id, app_store_id, url, main)
            VALUES ($1, $2, $3, $4, $5, $6)
            """,
            campaign_id,
            data["title"],
            data["google_play_id"],
            data["app_store_id"],
            data["url"],
            data["main"],
        )

    async def _insert_action_promocode(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO action_promocode (campaign_id, promocode, main)
            VALUES ($1, $2, $3)
            """,
            campaign_id,
            data["promocode"],
            data["main"],
        )

    async def _insert_action_resolve_uri(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO action_resolve_uri
            (campaign_id, uri, action_type, target, dialog, main)
            VALUES ($1, $2, $3::actiontypeenum, $4::resolveuritargetenum, $5, $6)
            """,
            campaign_id,
            data["uri"],
            data["action_type"].name,
            data["target"].name,
            data.get("dialog"),
            data["main"],
        )

    async def _insert_action_add_point_to_route(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO action_add_point_to_route
            (campaign_id, latitude, longitude, main)
            VALUES ($1, $2, $3, $4)
            """,
            campaign_id,
            data["latitude"],
            data["longitude"],
            data["main"],
        )

    async def create_campaign(
        self,
        *,
        name: str = "campaign0",
        author_id: int = 123,
        publication_envs: Optional[List[PublicationEnvEnum]] = None,
        campaign_type: CampaignTypeEnum = CampaignTypeEnum.ZERO_SPEED_BANNER,
        start_datetime: Optional[datetime] = None,
        end_datetime: Optional[datetime] = None,
        timezone: str = "UTC",
        platforms: Optional[List[PlatformEnum]] = None,
        rubric: Optional[RubricEnum] = None,
        order_size: Optional[OrderSizeEnum] = None,
        comment: Optional[str] = "",
        user_display_limit: Optional[int] = None,
        user_daily_display_limit: Optional[int] = None,
        targeting: Optional[dict] = None,
        order_id: Optional[int] = None,
        manul_order_id: Optional[int] = None,
        cpm: Optional[dict] = None,
        cpa: Optional[dict] = None,
        fix: Optional[dict] = None,
        actions: Optional[List[dict]] = None,
        creatives: Optional[List[dict]] = None,
        organizations: Optional[dict] = None,
        area: Optional[dict] = None,
        week_schedule: Optional[List[dict]] = None,
        status: Optional[CampaignStatusEnum] = None,
        discounts: Optional[List[dict]] = None,
        display_probability: Optional[Decimal] = None,
        display_probability_auto: Optional[Decimal] = None,
        datatesting_expires_at: Optional[datetime] = None,
        settings: Optional[dict] = None,
        paid_till: Optional[datetime] = None,
    ) -> dict:
        if not any([order_id, manul_order_id]):
            order_id = 10

        publication_envs = publication_envs or [PublicationEnvEnum.DATA_TESTING]
        platforms = platforms or [PlatformEnum.NAVI]
        status = status if status is not None else CampaignStatusEnum.DRAFT
        targeting = targeting or {}
        settings = dict(settings) if settings is not None else {}
        if "overview_position" in settings:
            settings["overview_position"] = settings["overview_position"].name

        if not any([cpm, cpa, fix]):
            cpm = {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        billing_id = await self._insert_billing_data(cpm, cpa, fix)

        campaign_data = await self.con.fetchrow(
            """
            INSERT INTO campaign (
                name,
                author_id,
                publication_envs,
                campaign_type,
                start_datetime,
                end_datetime,
                timezone,
                platforms,
                rubric,
                order_size,
                comment,
                user_display_limit,
                user_daily_display_limit,
                targeting,
                billing_id,
                order_id,
                manul_order_id,
                display_probability,
                display_probability_auto,
                datatesting_expires_at,
                settings,
                paid_till
            )
            VALUES (
                $1, $2, $3, $4, $5, $6, $7, $8, $9, $10,
                $11, $12, $13, $14, $15, $16, $17, $18,
                $19, $20, $21, $22
            )
            RETURNING *
            """,
            name,
            author_id,
            list(map(attrgetter("name"), publication_envs)),
            campaign_type.name,
            start_datetime or dt("2019-01-01 00:00:00"),
            end_datetime or dt("2019-02-01 00:00:00"),
            timezone,
            list(map(attrgetter("name"), platforms)),
            rubric.name if rubric else None,
            order_size.name if order_size else None,
            comment,
            user_display_limit,
            user_daily_display_limit,
            targeting,
            billing_id,
            order_id,
            manul_order_id,
            display_probability,
            display_probability_auto,
            datatesting_expires_at,
            settings,
            paid_till,
        )

        if organizations is None and area is None:
            organizations = {"permalinks": [123, 345]}

        if actions:
            await self._insert_actions(campaign_data["id"], actions)
        if creatives:
            await self._insert_creatives(campaign_data["id"], creatives)
        if organizations:
            await self._insert_organizations(campaign_data["id"], organizations)
        if area:
            await self._insert_areas(campaign_data["id"], **area)
        if week_schedule:
            await self._insert_schedules(campaign_data["id"], week_schedule)
        if discounts:
            await self._insert_discounts(campaign_data["id"], discounts)

        await self.set_status(
            campaign_data["id"],
            status=status,
            author_id=author_id,
            changed_datetime=datetime.fromtimestamp(0, tz=pytz.utc),
        )

        return campaign_data

    async def set_status(
        self,
        campaign_id: int,
        *,
        author_id: int = 1234,
        status: CampaignStatusEnum = CampaignStatusEnum.PAUSED,
        metadata: Optional[dict] = None,
        changed_datetime: Optional[datetime] = None,
    ) -> None:
        if metadata is None:
            metadata = {}
        if changed_datetime is None:
            changed_datetime = datetime.now(tz=timezone.utc)

        await self.con.execute(
            """
            INSERT INTO status_history
                (campaign_id, author_id, status, metadata, changed_datetime)
            VALUES ($1, $2, $3, $4, $5)
            """,
            campaign_id,
            author_id,
            status.name,
            metadata,
            changed_datetime,
        )

    async def _insert_creatives(self, campaign_id: int, creatives: List[dict]):
        funcs = {
            "pin": self._insert_creative_pin,
            "billboard": self._insert_creative_billboard,
            "icon": self._insert_creative_icon,
            "pin_search": self._insert_creative_pin_search,
            "logo_and_text": self._insert_creative_logo_and_text,
            "text": self._insert_creative_text,
            "via_point": self._insert_creative_via_point,
            "banner": self._insert_creative_banner,
            "audio_banner": self._insert_creative_audio_banner,
        }

        for creative in creatives:
            creative_data = creative.copy()
            creative_type = creative_data.pop("type_")
            await funcs[creative_type](campaign_id, creative_data)

    async def _insert_creative_pin(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO creative_pin (campaign_id, images, title, subtitle)
            VALUES ($1, $2, $3, $4)
            """,
            campaign_id,
            data["images"],
            data["title"],
            data["subtitle"],
        )

    async def _insert_creative_billboard(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO creative_billboard (campaign_id, images, images_v2, title, description)
            VALUES ($1, $2, $3, $4, $5)
            """,
            campaign_id,
            data["images"],
            data.get("images_v2", []),
            data.get("title"),
            data.get("description"),
        )

    async def _insert_creative_icon(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO creative_icon (campaign_id, images, position, title)
            VALUES ($1, $2, $3, $4)
            """,
            campaign_id,
            data["images"],
            data["position"],
            data["title"],
        )

    async def _insert_creative_pin_search(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO creative_pin_search (campaign_id, images, title, organizations)
            VALUES ($1, $2, $3, $4)
            """,
            campaign_id,
            data["images"],
            data["title"],
            data["organizations"],
        )

    async def _insert_creative_logo_and_text(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO creative_logo_and_text (campaign_id, images, text)
            VALUES ($1, $2, $3)
            """,
            campaign_id,
            data["images"],
            data["text"],
        )

    async def _insert_creative_text(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO creative_text (campaign_id, text, disclaimer)
            VALUES ($1, $2, $3)
            """,
            campaign_id,
            data["text"],
            data["disclaimer"],
        )

    async def _insert_creative_via_point(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO creative_via_point (
                campaign_id,
                images,
                button_text_active,
                button_text_inactive,
                description
            )
            VALUES ($1, $2, $3, $4, $5)
            """,
            campaign_id,
            data["images"],
            data["button_text_active"],
            data["button_text_inactive"],
            data["description"],
        )

    async def _insert_creative_banner(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO creative_banner (
                campaign_id,
                images,
                disclaimer,
                show_ads_label,
                description,
                title,
                terms
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            """,
            campaign_id,
            data["images"],
            data["disclaimer"],
            data["show_ads_label"],
            data["description"],
            data["title"],
            data.get("terms", ""),
        )

    async def _insert_creative_audio_banner(self, campaign_id: int, data: dict):
        await self.con.execute(
            """
            INSERT INTO creative_audio_banner (
                campaign_id,
                images,
                left_anchor,
                audio_file_url
            )
            VALUES ($1, $2, $3, $4)
            """,
            campaign_id,
            data["images"],
            data["left_anchor"],
            data["audio_file_url"],
        )

    async def _insert_organizations(self, campaign_id: int, organizations: dict):
        await self.con.execute(
            """
            INSERT INTO campaign_placing_organizations (campaign_id, permalinks)
            VALUES ($1, $2)
            """,
            campaign_id,
            organizations["permalinks"],
        )

    async def _insert_areas(self, campaign_id: int, areas: list, version: int):
        await self.con.execute(
            """
            INSERT INTO campaign_placing_area (campaign_id, areas, version)
            VALUES ($1, $2, $3)
            """,
            campaign_id,
            areas,
            version,
        )

    async def _insert_schedules(self, campaign_id: int, schedules: List[dict]):
        await self.con.execute(
            """
            INSERT INTO campaign_week_schedule (campaign_id, start, "end")
            (SELECT
                r.campaign_id, r.start, r."end"
             FROM unnest($1::campaign_week_schedule[]) AS r
            )
            """,
            list(
                (campaign_id, schedule["start"], schedule["end"])
                for schedule in schedules
            ),
        )

    async def _insert_discounts(self, campaign_id: int, discounts: List[dict]):
        for discount in discounts:
            await self.con.execute(
                """
                INSERT INTO campaign_discounts (
                    campaign_id, start_datetime, end_datetime, cost_multiplier
                )
                VALUES ($1, $2, $3, $4)
                """,
                campaign_id,
                discount["start_datetime"],
                discount["end_datetime"],
                discount["cost_multiplier"],
            )

    async def get_campaign_creation_dt(self, campaign_id: int):
        return await self.con.fetchval(
            """
                SELECT changed_datetime
                FROM status_history
                WHERE campaign_id = $1
                ORDER BY changed_datetime
                LIMIT 1
            """,
            campaign_id,
        )

    async def list_campaign_creatives(self, campaign_id: int):
        data = await self.con.fetch(
            """
                SELECT
                    (jsonb_build_object('type_', 'pin') || row_to_json(creative_pin)::jsonb) AS data
                FROM creative_pin
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'billboard') || row_to_json(creative_billboard)::jsonb) AS data
                FROM creative_billboard
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'icon') || row_to_json(creative_icon)::jsonb) AS data
                FROM creative_icon
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'pin_search') || row_to_json(creative_pin_search)::jsonb) AS data
                FROM creative_pin_search
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'text') || row_to_json(creative_text)::jsonb) AS data
                FROM creative_text
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'logo_and_text') || row_to_json(creative_logo_and_text)::jsonb) AS data
                FROM creative_logo_and_text
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'via_point') || row_to_json(creative_via_point)::jsonb) AS data
                FROM creative_via_point
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'banner') || row_to_json(creative_banner)::jsonb) AS data
                FROM creative_banner
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'audio_banner') || row_to_json(creative_audio_banner)::jsonb) AS data
                FROM creative_audio_banner
                WHERE campaign_id = $1
            """,  # noqa: E501
            campaign_id,
        )

        creatives = []
        for creative in data:
            creatives.append(dict(creative)["data"])
            del creatives[-1]["campaign_id"]

        return creatives

    async def list_campaign_actions(self, campaign_id: int):
        data = await self.con.fetch(
            """
                SELECT
                    (jsonb_build_object('type_', 'search') || row_to_json(action_search)::jsonb) AS data
                FROM action_search
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'open_site') || row_to_json(action_open_site)::jsonb) AS data
                FROM action_open_site
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'phone_call') || row_to_json(action_phone_call)::jsonb) AS data
                FROM action_phone_call
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'download_app') || row_to_json(action_download_app)::jsonb) AS data
                FROM action_download_app
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'promocode') || row_to_json(action_promocode)::jsonb) AS data
                FROM action_promocode
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'resolve_uri') || row_to_json(action_resolve_uri)::jsonb) AS data
                FROM action_resolve_uri
                WHERE campaign_id = $1
                UNION ALL
                SELECT
                    (jsonb_build_object('type_', 'add_point_to_route') || row_to_json(action_add_point_to_route)::jsonb) AS data
                FROM action_add_point_to_route
                WHERE campaign_id = $1
            """,  # noqa: E501
            campaign_id,
        )

        actions = []
        for action in data:
            act = dict(action)["data"]
            del act["campaign_id"]

            if act["type_"] == "resolve_uri":
                act["action_type"] = ActionTypeEnum[act["action_type"]]
                act["target"] = ResolveUriTargetEnum[act["target"]]

            actions.append(act)

        return actions

    async def list_campaign_week_schedule(self, campaign_id: int):
        data = await self.con.fetch(
            """
                SELECT start, "end"
                FROM campaign_week_schedule
                WHERE campaign_id = $1
                ORDER BY start, "end"
            """,
            campaign_id,
        )

        return list(map(dict, data))

    async def find_campaign_placing(self, campaign_id: int):
        result = {}
        organizations = await self.con.fetchrow(
            """
                SELECT permalinks
                FROM campaign_placing_organizations
                WHERE campaign_id = $1
            """,
            campaign_id,
        )
        if organizations:
            result["organizations"] = dict(organizations)

        areas = await self.con.fetchrow(
            """
                SELECT areas, version
                FROM campaign_placing_area
                WHERE campaign_id = $1
            """,
            campaign_id,
        )
        if areas:
            result["area"] = dict(areas)

        return result

    async def list_campaign_discounts(self, campaign_id: int):
        data = await self.con.fetch(
            """
                SELECT start_datetime, end_datetime, cost_multiplier
                FROM campaign_discounts
                WHERE campaign_id = $1
                ORDER BY start_datetime
            """,
            campaign_id,
        )

        return list(map(dict, data))

    async def list_campaign_status_history(self, campaign_id: int):
        data = await self.con.fetch(
            """
                SELECT *
                FROM status_history
                WHERE campaign_id = $1
                ORDER BY changed_datetime
            """,
            campaign_id,
        )

        return list(map(dict, data))

    async def fetch_last_campaign_status_data(self, campaign_id: int):
        data = await self.con.fetchrow(
            """
                SELECT *
                FROM status_history
                WHERE campaign_id = $1
                ORDER BY changed_datetime DESC
                LIMIT 1
            """,
            campaign_id,
        )

        return dict(data)

    async def find_campaign_billing(self, campaign_id: int):
        data = await self.con.fetchrow(
            """
                SELECT
                    billing_cpm.cost AS _cpm_cost,
                    billing_cpm.budget AS _cpm_budget,
                    billing_cpm.daily_budget AS _cpm_daily_budget,
                    billing_cpm.auto_daily_budget AS _cpm_auto_daily_budget,
                    billing_cpa.cost AS _cpa_cost,
                    billing_cpa.budget AS _cpa_budget,
                    billing_cpa.daily_budget AS _cpa_daily_budget,
                    billing_cpa.auto_daily_budget AS _cpa_auto_daily_budget,
                    billing_fix.cost AS _fix_cost,
                    billing_fix.time_interval AS _fix_time_interval
                FROM campaign
                LEFT JOIN campaign_billing ON campaign.billing_id = campaign_billing.id
                LEFT JOIN billing_cpm ON campaign_billing.cpm_id = billing_cpm.id
                LEFT JOIN billing_cpa ON campaign_billing.cpa_id = billing_cpa.id
                LEFT JOIN billing_fix ON campaign_billing.fix_id = billing_fix.id
                WHERE campaign.id = $1
            """,
            campaign_id,
        )

        result = {}
        if data["_cpm_cost"] is not None:
            result["cpm"] = {
                "cost": data["_cpm_cost"],
                "budget": data["_cpm_budget"],
                "daily_budget": data["_cpm_daily_budget"],
                "auto_daily_budget": data["_cpm_auto_daily_budget"],
            }
        elif data["_cpa_cost"] is not None:
            result["cpa"] = {
                "cost": data["_cpa_cost"],
                "budget": data["_cpa_budget"],
                "daily_budget": data["_cpa_daily_budget"],
                "auto_daily_budget": data["_cpa_auto_daily_budget"],
            }
        elif data["_fix_cost"] is not None:
            result["fix"] = {
                "cost": data["_fix_cost"],
                "time_interval": FixTimeIntervalEnum[data["_fix_time_interval"]],
            }

        return result

    async def create_campaign_event(
        self,
        campaign_id: int,
        event_type: CampaignEventTypeEnum,
        event_data: str,
        timestamp: datetime = None,
    ):

        timestamp = timestamp or datetime.now()

        event = dict(
            await self.con.fetchrow(
                """
                INSERT INTO campaign_event (
                    timestamp, campaign_id, event_type, event_data
                )
                VALUES ($1, $2, $3, $4)
                RETURNING *
            """,
                timestamp,
                campaign_id,
                event_type.name,
                event_data,
            )
        )

        event["event_type"] = CampaignEventTypeEnum[event["event_type"]]
        return dict(event)

    async def retrieve_campaign_events(self, campaign_id: int) -> dict:

        sql = "SELECT * FROM campaign_event WHERE campaign_id = $1"

        return list(map(dict, await self.con.fetch(sql, campaign_id)))

    async def retrieve_campaign_full_state_view_entry(self, campaign_id: int) -> dict:
        data = await self.con.fetchrow(
            """
                SELECT *
                FROM campaigns_full_state_view
                WHERE id = $1
            """,
            campaign_id,
        )

        return dict(data)

    async def list_campaign_change_log(self, campaign_id: int) -> List[dict]:
        data = await self.con.fetch(
            """
                SELECT *
                FROM campaigns_change_log
                WHERE campaign_id = $1
                ORDER BY created_at
            """,
            campaign_id,
        )

        return list(map(dict, data))

    async def create_campaign_change_log(
        self,
        *,
        campaign_id: Optional[int] = None,
        author_id: Optional[int] = 132,
        status: CampaignStatusEnum = CampaignStatusEnum.DRAFT,
        created_at: datetime = dt("2019-01-01 00:00:00"),  # noqa: B008
        system_metadata: dict = dict(),  # noqa: B008
        state_before: dict = dict(),  # noqa: B008
        state_after: dict = dict(),  # noqa: B008
        is_latest: bool = False,
    ) -> dict:
        if campaign_id is None:
            campaign_id = (await self.create_campaign())["id"]

        data = await self.con.fetchrow(
            """
                INSERT INTO campaigns_change_log (
                    campaign_id,
                    created_at,
                    author_id,
                    status,
                    system_metadata,
                    state_before,
                    state_after,
                    is_latest
                )
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
                RETURNING *
            """,
            campaign_id,
            created_at,
            author_id,
            status.name,
            system_metadata,
            state_before,
            state_after,
            is_latest,
        )

        return dict(data)

    async def create_campaign_direct_moderation(
        self,
        campaign_id: int,
        created_at: int = None,
        reviewer_uid: int = 1234567,
        status: CampaignDirectModerationStatusEnum = (
            CampaignDirectModerationStatusEnum.NEW
        ),
        verdicts: List[int] = None,
    ) -> int:
        created_at = created_at or datetime.now()

        return (
            await self.con.fetchrow(
                """
            INSERT INTO campaign_direct_moderation (
                created_at, campaign_id, status, reviewer_uid, workflow, verdicts
            )
            VALUES ($1, $2, $3, $4, 'COMMON', $5)
            RETURNING id
        """,
                created_at,
                campaign_id,
                status.name,
                reviewer_uid,
                verdicts or [],
            )
        )["id"]

    async def get_campaign_direct_moderation(self, moderation_id: int) -> dict:

        sql = "SELECT * FROM campaign_direct_moderation WHERE id = $1"

        return dict(await self.con.fetchrow(sql, moderation_id))

    async def retrieve_actual_campaign_direct_moderation(
        self, campaign_id: int
    ) -> dict:

        sql = """
            SELECT * FROM campaign_direct_moderation WHERE campaign_id = $1
            ORDER BY created_at DESC
            LIMIT 1
        """

        result = dict(await self.con.fetchrow(sql, campaign_id))
        result["workflow"] = CampaignDirectModerationWorkflowEnum[result["workflow"]]
        return result

    async def retrieve_campaign(self, campaign_id: int) -> dict:

        sql = "SELECT * FROM campaign WHERE id = $1"

        return dict(await self.con.fetchrow(sql, campaign_id))


@pytest.fixture
async def factory(con):
    return Factory(con)

import json
from collections import defaultdict
from datetime import datetime, timezone
from operator import itemgetter
from typing import List, Optional

from sqlalchemy import insert, select

from maps_adv.adv_store.api.schemas import enums
from maps_adv.adv_store.v2.lib.db import tables
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum
from maps_adv.common.helpers import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum


class Factory:
    STARTING_ID = 100

    def __init__(self, db, faker):
        self._db = db
        self._faker = faker
        self._counter = defaultdict(lambda: self.STARTING_ID)

    def _next_id(self, key):
        self._counter[key] += 1
        return self._counter[key]

    async def create_download_app(self, **kwargs):
        faker = self._faker
        values = {
            "title": faker.optional(faker.sentence()),
            "google_play_id": faker.optional(faker.ean()),
            "app_store_id": faker.optional(faker.ean()),
            "url": faker.url(),
        }
        values.update(kwargs)

        return await self._create_action(tables.download_app, values)

    async def create_phone_call(self, **kwargs):
        faker = self._faker
        values = {
            "title": faker.optional(faker.sentence()),
            "phone": faker.phone_number(),
        }
        values.update(kwargs)

        return await self._create_action(tables.phone_call, values)

    async def create_search(self, **kwargs):
        faker = self._faker
        values = {
            "title": faker.optional(faker.sentence()),
            "organizations": faker.permalinks(),
            "history_text": faker.sentence(),
        }
        values.update(kwargs)

        return await self._create_action(tables.search, values)

    async def create_open_site(self, **kwargs):
        faker = self._faker
        values = {"title": faker.optional(faker.sentence()), "url": faker.url()}
        values.update(kwargs)

        return await self._create_action(tables.open_site, values)

    async def _create_action(self, table, values):
        return await self._create_campaign_many_relations(table, values)

    async def _create_campaign_many_relations(self, table, values):
        if "campaign_id" not in values:
            campaign = await self.create_campaign()
            values["campaign_id"] = campaign["id"]

        return await self._db.rw.fetch_one(
            insert(table).values(values).returning(table)
        )

    async def _create_creative(self, table, values):
        return await self._create_campaign_many_relations(table, values)

    async def create_organizations(self, **kwargs):
        faker = self._faker
        values = faker.organizations_from_domain()
        values.update(kwargs)
        return await self._create_campaign_many_relations(tables.organizations, values)

    async def create_area(self, **kwargs):
        faker = self._faker
        values = faker.area_from_domain()
        values.update(kwargs)
        return await self._create_campaign_many_relations(tables.area, values)

    async def create_billboard(self, **kwargs):
        faker = self._faker
        values = {"images": faker.images()}
        values.update(kwargs)

        return await self._create_creative(tables.billboard, values)

    async def create_logo_and_text(self, **kwargs):
        faker = self._faker
        values = {"images": faker.images(), "text": faker.paragraph()}
        values.update(kwargs)

        return await self._create_creative(tables.logo_and_text, values)

    async def create_banner(self, **kwargs):
        faker = self._faker
        values = {
            "images": faker.images(),
            "disclaimer": faker.paragraph(),
            "show_ads_label": faker.pybool(),
            "title": faker.paragraph(),
            "description": faker.paragraph(),
        }
        values.update(kwargs)

        return await self._create_creative(tables.banner, values)

    async def create_pin(self, **kwargs):
        faker = self._faker
        values = {
            "images": faker.images(),
            "title": faker.sentence(),
            "subtitle": faker.sentence(),
        }
        values.update(kwargs)

        return await self._create_creative(tables.pin, values)

    async def create_text(self, **kwargs):
        faker = self._faker
        values = {"text": faker.paragraph(), "disclaimer": faker.paragraph()}
        values.update(kwargs)

        return await self._create_creative(tables.text, values)

    async def create_icon(self, **kwargs):
        faker = self._faker
        values = {
            "images": faker.images(),
            "title": faker.paragraph(),
            "position": faker.int32(),
        }
        values.update(kwargs)

        return await self._create_creative(tables.icon, values)

    async def create_via_point(self, **kwargs):
        faker = self._faker
        values = {
            "images": faker.images(),
            "button_text_active": faker.paragraph(),
            "button_text_inactive": faker.paragraph(),
            "description": faker.paragraph(),
        }
        values.update(kwargs)

        return await self._create_creative(tables.via_point, values)

    async def create_pin_search(self, **kwargs):
        faker = self._faker
        values = {
            "images": faker.images(),
            "title": faker.paragraph(),
            "organizations": faker.permalinks(),
        }
        values.update(kwargs)

        return await self._create_creative(tables.pin_search, values)

    async def create_week_schedule(self, **kwargs):
        faker = self._faker
        start = faker.random_int(min=0, max=7 * 24 * 60 - 1)
        end = faker.random_int(min=start + 1, max=7 * 24 * 60)
        values = {"start": start, "end": end}
        values.update(kwargs)

        return await self._create_campaign_many_relations(tables.week_schedule, values)

    async def create_campaign(self, **kwargs):
        faker = self._faker
        values = {
            "author_id": faker.u64(),
            "name": faker.sentence(),
            "publication_envs": faker.enum_list(enums.PublicationEnvEnum),
            "campaign_type": faker.enum(CampaignTypeEnum),
            "start_datetime": faker.past_datetime(),
            "end_datetime": faker.future_datetime(),
            "timezone": faker.timezone(),
            "platforms": faker.enum_list(enums.PlatformEnum),
            "order_id": faker.u64(),
            "manul_order_id": None,
            "comment": faker.sentence(),
            "user_display_limit": faker.random_int(),  # can end up zero
            "user_daily_display_limit": faker.random_int(),  # can end up zero
            "targeting": faker.targeting_from_domain(),
            "rubric": faker.enum(enums.RubricEnum),
            "changed_datetime": faker.date_time(),
            "display_probability": faker.probability(),
            "display_probability_auto": faker.probability(),
        }
        values.update(kwargs)
        if "billing_id" not in values:
            billing = await self.create_billing()
            values["billing_id"] = billing["id"]

        campaign = dict(
            await self._db.rw.fetch_one(
                insert(tables.campaign).values(values).returning(tables.campaign)
            )
        )
        return self._convert_enum_array_for_campaign(campaign)

    async def create_campaign_with_any_status(self, *, status=None, **kwargs):
        faker = self._faker
        campaign = await self.create_campaign(**kwargs)
        status = {
            "campaign_id": campaign["id"],
            "author_id": campaign["author_id"],
            "status": status or faker.enum(enums.CampaignStatusEnum),
            "metadata": {},
        }
        await self._db.rw.execute(insert(tables.status_history).values(status))
        return campaign

    async def create_manul_campaign_with_any_status(self, *, status=None, **kwargs):
        kwargs["order_id"] = None
        kwargs["manul_order_id"] = kwargs.get("manul_order_id", 9999)
        return await self.create_campaign_with_any_status(status=status, **kwargs)

    async def create_billing(self, *, billing_type=None, **kwargs):
        values = kwargs.copy()
        has_fix = "fix_id" in kwargs
        has_cpm = "cpm_id" in kwargs
        has_cpa = "cpa_id" in kwargs
        if not any([has_fix, has_cpm, has_cpa]):
            billing_type = billing_type or self._faker.random.choice(
                ["fix", "cpm", "cpa"]
            )
            if billing_type == "fix":
                values["fix_id"] = (await self.create_fix())["id"]
            elif billing_type == "cpm":
                values["cpm_id"] = (await self.create_cpm())["id"]
            else:
                values["cpa_id"] = (await self.create_cpa())["id"]

        return await self._db.rw.fetch_one(
            insert(tables.billing).values(values).returning(tables.billing)
        )

    async def create_placing(self, *, placing_type=None, **kwargs):
        faker = self._faker
        placing_type = placing_type or faker.random.choice(["organizations", "area"])
        if placing_type == "organizations":
            await self.create_organizations(**kwargs)
        elif placing_type == "area":
            await self.create_area(**kwargs)

    async def create_fix(self, **kwargs):
        faker = self._faker
        values = {
            "time_interval": faker.enum(enums.FixTimeIntervalEnum),
            "cost": faker.pydecimal(min_value=1, right_digits=4),
        }
        values.update(kwargs)
        return await self._db.rw.fetch_one(
            insert(tables.fix).values(values).returning(tables.fix)
        )

    async def create_cpm(self, **kwargs):
        faker = self._faker
        values = {
            "cost": faker.pydecimal(min_value=1, right_digits=4),
            "budget": faker.pydecimal(min_value=1, right_digits=4),
            "daily_budget": faker.pydecimal(min_value=1, right_digits=4),
        }
        values.update(kwargs)
        return await self._db.rw.fetch_one(
            insert(tables.cpm).values(values).returning(tables.cpm)
        )

    async def create_cpa(self, **kwargs):
        faker = self._faker
        values = {
            "cost": faker.pydecimal(min_value=1, right_digits=4),
            "budget": faker.pydecimal(min_value=1, right_digits=4),
            "daily_budget": faker.pydecimal(min_value=1, right_digits=4),
        }
        values.update(kwargs)
        return await self._db.rw.fetch_one(
            insert(tables.cpa).values(values).returning(tables.cpa)
        )

    async def get_all_actions(self):
        data = dict()
        data["open_site"] = await self.get_all_actions_by_table(tables.open_site)
        data["search"] = await self.get_all_actions_by_table(tables.search)
        data["download_app"] = await self.get_all_actions_by_table(tables.download_app)
        data["phone_call"] = await self.get_all_actions_by_table(tables.phone_call)
        return data

    async def get_actions_by_campaign_id(self, campaign_id):
        retval = []
        for type_, actions in (await self.get_all_actions()).items():
            for a in actions:
                if a["campaign_id"] == campaign_id:
                    del a["campaign_id"]
                    retval.append({type_: a})
        return retval

    async def get_all_actions_by_table(self, table):
        data = await self._db.ro.fetch_all(table.select())
        return [dict(entry) for entry in data]

    async def get_all_creative_logo_and_texts(self):
        return await self.get_all_creatives_by_table(tables.logo_and_text)

    async def get_all_creative_banners(self):
        return await self.get_all_creatives_by_table(tables.banner)

    async def get_all_creative_pins(self):
        return await self.get_all_creatives_by_table(tables.pin)

    async def get_all_creative_billboards(self):
        return await self.get_all_creatives_by_table(tables.billboard)

    async def get_all_creative_icons(self):
        return await self.get_all_creatives_by_table(tables.icon)

    async def get_all_creative_pins_search(self):
        return await self.get_all_creatives_by_table(tables.pin_search)

    async def get_all_creative_texts(self):
        return await self.get_all_creatives_by_table(tables.text)

    async def get_all_creative_via_points(self):
        return await self.get_all_creatives_by_table(tables.via_point)

    async def get_all_creatives_by_table(self, table):
        data = await self._db.ro.fetch_all(table.select())
        return [dict(entry) for entry in data]

    async def get_all_creatives(self):
        data = dict()
        data["logo_and_text"] = await self.get_all_creative_logo_and_texts()
        data["banner"] = await self.get_all_creative_banners()
        data["pin"] = await self.get_all_creative_pins()
        data["billboard"] = await self.get_all_creative_billboards()
        data["icon"] = await self.get_all_creative_icons()
        data["pin_search"] = await self.get_all_creative_pins_search()
        data["text"] = await self.get_all_creative_texts()
        data["via_point"] = await self.get_all_creative_via_points()
        return data

    async def get_creatives_by_campaign_id(self, campaign_id):
        retval = []
        for type_, creatives in (await self.get_all_creatives()).items():
            for c in creatives:
                if c["campaign_id"] == campaign_id:
                    del c["campaign_id"]
                    retval.append({type_: c})
        return retval

    async def get_all_schedules(self):
        data = await self._db.ro.fetch_all(tables.week_schedule.select())
        return [dict(entry) for entry in data]

    async def get_all_placings(self):
        retval = {
            "area": await self.get_all_placing_area(),
            "organizations": await self.get_all_placing_organizations(),
        }
        return retval

    async def get_placings_by_campaign_id(self, campaign_id):
        retval = dict()
        for type_, placings in (await self.get_all_placings()).items():
            placings = [p for p in placings if p["campaign_id"] == campaign_id]
            if len(placings) == 1:
                p = placings[0]
                del p["campaign_id"]
                retval[type_] = p
            elif len(placings) > 1:
                raise RuntimeError("Campaign has several placings")
        return retval

    async def get_all_placing_area(self):
        return await self.get_all_placing_entries(tables.area)

    async def get_all_placing_organizations(self):
        return await self.get_all_placing_entries(tables.organizations)

    async def get_all_placing_entries(self, table):
        data = await self._db.ro.fetch_all(table.select())
        return [dict(entry) for entry in data]

    async def get_all_fix_methods(self):
        data = await self._db.ro.fetch_all(tables.fix.select())
        return [dict(entry) for entry in data]

    async def get_all_cpm_methods(self):
        data = await self._db.ro.fetch_all(tables.cpm.select())
        return [dict(entry) for entry in data]

    async def get_all_cpa_methods(self):
        data = await self._db.ro.fetch_all(tables.cpa.select())
        return [dict(entry) for entry in data]

    async def get_all_billing_methods(self):
        data = []
        data.extend(await self.get_all_fix_methods())
        data.extend(await self.get_all_cpm_methods())
        data.extend(await self.get_all_cpa_methods())
        return [dict(entry) for entry in data]

    async def get_all_billings(self):
        data = await self._db.ro.fetch_all(tables.billing.select())
        return [dict(entry) for entry in data]

    @staticmethod
    def _convert_enum_array_for_campaign(campaign):
        publication_env_map = {enum.name: enum for enum in enums.PublicationEnvEnum}
        platform_map = {enum.name: enum for enum in enums.PlatformEnum}
        campaign["publication_envs"] = [
            publication_env_map[env] for env in campaign["publication_envs"]
        ]
        campaign["platforms"] = [platform_map[env] for env in campaign["platforms"]]
        return campaign

    async def get_all_campaigns(self):
        retval = []
        data = await self._db.ro.fetch_all(tables.campaign.select())

        for entry in map(dict, data):
            retval.append(self._convert_enum_array_for_campaign(entry))

        return retval

    async def get_all_status_history(self):
        data = await self._db.ro.fetch_all(
            tables.status_history.select().order_by(
                tables.status_history.c.changed_datetime
            )
        )
        return [dict(entry) for entry in data]

    async def get_all_status_history_for_campaign(self, campaign_id):
        data = await self.get_all_status_history()
        return [s for s in data if s["campaign_id"] == campaign_id]

    async def get_campaign_statuses(self, campaign_id):
        data = await self._db.ro.fetch_all(
            select([tables.status_history.c.status])
            .where(tables.status_history.c.campaign_id == campaign_id)
            .order_by(tables.status_history.c.changed_datetime)
        )
        return list(map(itemgetter("status"), data))

    async def create_status_history(
        self,
        *,
        campaign_id: int,
        status: enums.CampaignStatusEnum = enums.CampaignStatusEnum.DRAFT,
        changed_datetime: Optional[datetime] = None,
        metadata: dict = dict(),  # noqa: B006
        author_id: int = 123,
    ) -> dict:
        result = await self._db.rw.fetch_one(
            """
            INSERT INTO status_history(
                campaign_id, author_id, status, metadata, changed_datetime
            )
            VALUES (:campaign_id, :author_id, :status, :metadata, :changed_datetime)
            RETURNING *
            """,
            dict(
                campaign_id=campaign_id,
                author_id=author_id,
                status=status.name,
                metadata=json.dumps(metadata),
                changed_datetime=changed_datetime or datetime.now(tz=timezone.utc),
            ),
        )

        return self._parse_json_fields(result._row, "metadata")

    async def create_status_entry(self, **kwargs):
        faker = self._faker
        values = {
            "status": faker.enum(enums.CampaignStatusEnum),
            "metadata": {},
            "author_id": faker.u64(),
        }
        values.update(kwargs)

        return await self._create_campaign_many_relations(tables.status_history, values)

    async def list_campaign_change_log(self, campaign_id: int) -> List[dict]:
        records = await self._db.rw.fetch_all(
            """
                SELECT *
                FROM campaigns_change_log
                WHERE campaign_id = :campaign_id
                ORDER BY created_at
            """,
            dict(campaign_id=campaign_id),
        )

        return [
            self._parse_json_fields(
                data._row, "system_metadata", "state_before", "state_after"
            )
            for data in records
        ]

    async def create_campaign_change_log(
        self,
        *,
        campaign_id: Optional[int] = None,
        author_id: Optional[int] = 132,
        status: CampaignStatusEnum = enums.CampaignStatusEnum.DRAFT,
        created_at: datetime = dt("2019-01-01 00:00:00"),  # noqa: B008
        system_metadata: dict = dict(),  # noqa: B008
        state_before: dict = dict(),  # noqa: B008
        state_after: dict = dict(),  # noqa: B008
        is_latest: bool = False,
    ) -> dict:
        if campaign_id is None:
            campaign_id = (
                await self.create_campaign_with_any_status(author_id=123, status=status)
            )["id"]

        data = await self._db.rw.fetch_one(
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
                VALUES (
                    :campaign_id,
                    :created_at,
                    :author_id,
                    :status,
                    :system_metadata,
                    :state_before,
                    :state_after,
                    :is_latest
                )
                RETURNING *
            """,
            dict(
                campaign_id=campaign_id,
                created_at=created_at,
                author_id=author_id,
                status=status.name,
                system_metadata=json.dumps(system_metadata),
                state_before=json.dumps(state_before),
                state_after=json.dumps(state_after),
                is_latest=is_latest,
            ),
        )

        return self._parse_json_fields(
            data._row, "system_metadata", "state_before", "state_after"
        )

    @classmethod
    def _parse_json_fields(cls, data: dict, *fields) -> dict:
        result = {}
        for key, value in data.items():
            if key in fields:
                result[key] = json.loads(value)
            else:
                result[key] = value
        return result


__all__ = ["Factory"]

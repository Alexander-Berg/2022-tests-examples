from datetime import timezone

import pytest
from sqlalchemy import insert

from maps_adv.adv_store.api.schemas import enums as db_enums
from maps_adv.adv_store.v2.lib.db import tables
from maps_adv.common.helpers.enums import CampaignTypeEnum


@pytest.fixture
def status_history_data(faker, campaign_id):
    return {
        "campaign_id": campaign_id,
        "author_id": faker.u64(),
        "status": faker.enum(db_enums.CampaignStatusEnum),
        "metadata": faker.json_obj(),
        "changed_datetime": faker.date_time(tzinfo=timezone.utc),
    }


@pytest.fixture
def download_app_data(faker, campaign_id):
    return {
        "campaign_id": campaign_id,
        "title": faker.sentence(),
        "google_play_id": faker.ean(),
        "app_store_id": faker.ean(),
        "url": faker.url(),
    }


@pytest.fixture
def phone_call_data(faker, campaign_id):
    return {
        "campaign_id": campaign_id,
        "title": faker.sentence(),
        "phone": faker.phone_number(),
    }


@pytest.fixture
def search_data(faker, campaign_id):
    return {
        "campaign_id": campaign_id,
        "title": faker.sentence(),
        "organizations": faker.permalinks(),
        "history_text": faker.sentence(),
    }


@pytest.fixture
def open_site_data(faker, campaign_id):
    return {"campaign_id": campaign_id, "title": faker.sentence(), "url": faker.url()}


@pytest.fixture
def creative_images(faker):
    return faker.json_arr()


@pytest.fixture
def icon_data(faker, campaign_id, creative_images):
    return {
        "campaign_id": campaign_id,
        "images": creative_images,
        "position": faker.int32(),
        "title": faker.paragraph(),
    }


@pytest.fixture
def pin_search_data(faker, campaign_id, creative_images):
    return {
        "campaign_id": campaign_id,
        "images": creative_images,
        "title": faker.paragraph(),
        "organizations": faker.permalinks(),
    }


@pytest.fixture
def text_data(faker, campaign_id):
    return {
        "campaign_id": campaign_id,
        "text": faker.paragraph(),
        "disclaimer": faker.paragraph(),
    }


@pytest.fixture
def logo_and_text_data(faker, campaign_id, creative_images):
    return {
        "campaign_id": campaign_id,
        "images": creative_images,
        "text": faker.paragraph(),
    }


@pytest.fixture
def banner_data(faker, campaign_id, creative_images):
    return {
        "campaign_id": campaign_id,
        "images": creative_images,
        "disclaimer": faker.paragraph(),
        "show_ads_label": faker.pybool(),
        "title": faker.paragraph(),
        "description": faker.paragraph(),
    }


@pytest.fixture
def pin_data(faker, campaign_id, creative_images):
    return {
        "campaign_id": campaign_id,
        "images": creative_images,
        "title": faker.sentence(),
        "subtitle": faker.sentence(),
    }


@pytest.fixture
def billboard_data(faker, campaign_id, creative_images):
    return {"campaign_id": campaign_id, "images": creative_images}


@pytest.fixture
def week_schedule_data(faker, campaign_id):
    start = faker.random_int(min=0, max=7 * 24 * 60 - 1)
    end = faker.random_int(min=start + 1, max=7 * 24 * 60)
    return {"campaign_id": campaign_id, "start": start, "end": end}


@pytest.fixture
def organizations_data(faker, campaign_id):
    return {"campaign_id": campaign_id, "permalinks": faker.permalinks()}


@pytest.fixture
def area_data(faker, campaign_id):
    return {
        "campaign_id": campaign_id,
        "areas": faker.json_arr(),
        "version": faker.pyint(),
    }


@pytest.fixture
def cpm_data(faker):
    return {
        "cost": faker.pydecimal(min_value=1, right_digits=4),
        "budget": faker.pydecimal(min_value=1, right_digits=4),
        "daily_budget": faker.pydecimal(min_value=1, right_digits=4),
    }


@pytest.fixture
def cpa_data(faker):
    return {
        "cost": faker.pydecimal(min_value=1, right_digits=4),
        "budget": faker.pydecimal(min_value=1, right_digits=4),
        "daily_budget": faker.pydecimal(min_value=1, right_digits=4),
    }


@pytest.fixture
def fix_data(faker):
    return {
        "time_interval": faker.enum(db_enums.FixTimeIntervalEnum),
        "cost": faker.pydecimal(min_value=1, right_digits=4),
    }


@pytest.fixture
async def cpm_id(db, cpm_data):
    return await db.rw.fetch_val(
        insert(tables.cpm).values(cpm_data).returning(tables.cpm.c.id)
    )


@pytest.fixture
async def cpa_id(db, cpa_data):
    return await db.rw.fetch_val(
        insert(tables.cpa).values(cpa_data).returning(tables.cpa.c.id)
    )


@pytest.fixture
async def fix_id(db, fix_data):
    return await db.rw.fetch_val(
        insert(tables.fix).values(fix_data).returning(tables.fix.c.id)
    )


@pytest.fixture
def billing_data(faker, cpm_id, cpa_id, fix_id):

    return faker.random.choice(
        [
            {"fix_id": fix_id, "cpm_id": None, "cpa_id": None},
            {"fix_id": None, "cpm_id": cpm_id, "cpa_id": None},
            {"fix_id": None, "cpm_id": None, "cpa_id": cpa_id},
        ]
    )


@pytest.fixture
async def billing_id(db, billing_data):
    return await db.rw.fetch_val(
        insert(tables.billing).values(billing_data).returning(tables.billing.c.id)
    )


@pytest.fixture
def db_campaign_data(faker, billing_id):
    return {
        "author_id": faker.u64(),
        "name": faker.sentence(),
        "publication_envs": faker.enum_list(db_enums.PublicationEnvEnum),
        "campaign_type": faker.enum(CampaignTypeEnum),
        "start_datetime": faker.past_datetime(),
        "end_datetime": faker.future_datetime(),
        "timezone": faker.timezone(),
        "billing_id": billing_id,
        "platforms": faker.enum_list(db_enums.PlatformEnum),
        "order_id": faker.u64(),
        # can't be set simultaneously with order_id
        # "manul_order_id": faker.u64(),
        "comment": faker.sentence(),
        "user_display_limit": faker.random_int(),  # can end up zero
        "user_daily_display_limit": faker.random_int(),  # can end up zero
        "targeting": faker.json_obj(),
        "rubric": faker.enum(db_enums.RubricEnum),
        "changed_datetime": faker.date_time(tzinfo=timezone.utc),
    }


@pytest.fixture
async def campaign_id(db, db_campaign_data):
    return await db.rw.fetch_val(
        insert(tables.campaign).values(db_campaign_data).returning(tables.campaign.c.id)
    )

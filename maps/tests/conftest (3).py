import time
from decimal import Decimal
from unittest.mock import patch

import psycopg2
import pytest
from alembic import command, config as alembic_config
from faker import Faker

from smb.common.multiruntime.lib.basics import is_arcadia_python

from maps_adv.adv_store.lib.application import Application
from maps_adv.adv_store.lib.config import config as app_config
from maps_adv.adv_store.lib.db import db as app_db
from maps_adv.adv_store.tests.utils import dt
from maps_adv.adv_store.api.schemas import enums

from .factory import Factory
from .faker_provider import AdvStoreProvider

if is_arcadia_python:
    import aiohttp.pytest_plugin

    del aiohttp.pytest_plugin.loop

pytest_plugins = ["aiohttp.pytest_plugin"]


_config = {
    "DATABASE_URI": {
        "default": "postgresql://advstore:advstore@localhost:13000/advstore"
    },
    "DATABASE_URI_RO": {"default": None},
    "BILLING_API_URL": {"default": "http://localhost/"},
    "WARDEN_API_URL": {"default": "http://example.com/"},
    "TASKS_TO_START": {"default": []},
    "EXPERIMENT_CALCULATE_DISPLAY_CHANCE_FROM_CPM": {"default": True},
}


@pytest.fixture
async def loop(event_loop):
    return event_loop


@pytest.fixture
async def app(loop):
    app = Application()
    await app.setup()
    yield app


@pytest.fixture(scope="session")
def config():
    with patch.dict(app_config.options_map, _config):
        app_config.init()
        yield app_config


@pytest.fixture
def faker():
    fake = Faker()
    fake.add_provider(AdvStoreProvider)
    return fake


@pytest.fixture(scope="session")
def wait_for_db(config):
    """To wait for db to setup during arcadia testing"""
    waited = 0

    while waited < 100:
        try:
            con = psycopg2.connect(config.DATABASE_URI)
        except psycopg2.OperationalError:
            time.sleep(1)
            waited += 1
        else:
            con.close()
            break
    else:
        raise ConnectionRefusedError


@pytest.fixture(scope="session")
def migrate_db(config, wait_for_db):
    cfg = alembic_config.Config("alembic.ini")
    cfg.set_main_option("sqlalchemy.url", config["DATABASE_URI"])

    command.upgrade(cfg, "head")
    yield
    command.downgrade(cfg, "base")


@pytest.fixture(autouse=True)
async def db(migrate_db, loop, config):
    # 'force_rollback' rollbacks the transaction when 'disconnect' is called
    await app_db.setup(config.DATABASE_URI, force_rollback=True)
    yield app_db
    await app_db.disconnect()


@pytest.fixture
def factory(db, faker):
    return Factory(db, faker)


@pytest.fixture
async def client(loop, app, aiohttp_client):
    return await aiohttp_client(app.api)


@pytest.fixture
def campaign_data():
    return {
        "author_id": 1,
        "name": "campaign_1",
        "publication_envs": [enums.PublicationEnvEnum.PRODUCTION],
        "status": enums.CampaignStatusEnum.ACTIVE,
        "campaign_type": enums.CampaignTypeEnum.ROUTE_BANNER,
        "start_datetime": dt("2019-01-01 00:00:00"),
        "end_datetime": dt("2019-02-01 00:00:00"),
        "timezone": "UTC",
        "billing": {
            "fix": {
                "time_interval": enums.FixTimeIntervalEnum.DAILY,
                "cost": Decimal("1.2345"),
            }
        },
        "placing": {"organizations": {"permalinks": [1, 2]}},
        "creatives": [
            {
                "banner": {
                    "images": [
                        {
                            "type": "image_type",
                            "group_id": "image_group_id",
                            "image_name": "image_name",
                            "alias_template": "image_alias_template",
                            "metadata": {},
                        }
                    ],
                    "disclaimer": "banner_disclaimer",
                    "show_ads_label": True,
                    "title": "banner_title",
                    "description": "banner_description",
                    "terms": "banner_terms",
                }
            }
        ],
        "actions": [{"phone_call": {"title": "action_title", "phone": "3-4-5"}}],
        "platforms": [enums.PlatformEnum.NAVI],
        "week_schedule": [{"start": 1, "end": 2}],
        "order_id": None,
        "manul_order_id": None,
        "comment": "",
        "user_display_limit": None,
        "user_daily_display_limit": None,
        "targeting": {},
        "rubric": None,
        "display_probability": None,
        "display_probability_auto": None,
    }

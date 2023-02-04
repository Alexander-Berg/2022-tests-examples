import json
from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.adv_store.v2.lib.data_managers.campaigns import WrongBillingParameters
from maps_adv.adv_store.api.schemas.enums import (
    ActionTypeEnum,
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    OrderSizeEnum,
    OverviewPositionEnum,
    PlatformEnum,
    PublicationEnvEnum,
    ResolveUriTargetEnum,
    RubricEnum,
)
from maps_adv.common.helpers import Any, dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def campaign_creation_kwargs():
    def func(**overrides):
        kwargs = dict(
            name="campaign0",
            author_id=123,
            publication_envs=[PublicationEnvEnum.DATA_TESTING],
            campaign_type=CampaignTypeEnum.ZERO_SPEED_BANNER,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2019-02-01 00:00:00"),
            timezone="UTC",
            platforms=[PlatformEnum.METRO],
            creatives=[],
            actions=[],
            week_schedule=[],
            placing={},
            status=CampaignStatusEnum.DRAFT,
            rubric=RubricEnum.COMMON,
            order_size=OrderSizeEnum.SMALL,
            targeting={},
            user_daily_display_limit=2,
            user_display_limit=3,
            discounts=[],
            datatesting_expires_at=dt("2019-04-07 00:00:00"),
            settings={
                "custom_page_id": "abacaba",
                "overview_position": OverviewPositionEnum.FINISH,
                "forced_product_version_datetime": dt("2021-01-01 00:00:00"),
                "verification_data": [
                    {"platform": "weborama", "params": {"a": "b", "x": "y"}},
                    {"platform": "dcm", "params": {"url": "https://dcm.url"}},
                ],
            },
        )
        kwargs.update(overrides)

        if not any(billing in overrides for billing in ("cpm", "cpa", "fix")):
            kwargs["cpm"] = {
                "cost": Decimal("100.00"),
                "budget": None,
                "daily_budget": None,
                "auto_daily_budget": False,
            }
        if not any(
            order_id in overrides for order_id in ("order_id", "manul_order_id")
        ):
            kwargs["order_id"] = 10

        return kwargs

    return func


@pytest.mark.parametrize(
    "order_extra, order_expected",
    (
        [{"order_id": 10, "manul_order_id": None}, (10, None)],
        [{"order_id": None, "manul_order_id": 20}, (None, 20)],
    ),
)
@pytest.mark.parametrize(
    "billing_extra",
    (
        {
            "cpm": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": False,
            }
        },
        {
            "cpa": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": True,
            }
        },
        {
            "fix": {
                "time_interval": FixTimeIntervalEnum.DAILY,
                "cost": Decimal("10.1234"),
            }
        },
    ),
)
async def test_creates_campaign(
    order_extra,
    order_expected,
    billing_extra,
    campaigns_dm,
    con,
    campaign_creation_kwargs,
):
    kwargs = campaign_creation_kwargs(**order_extra, **billing_extra)

    got = await campaigns_dm.create_campaign(**kwargs)

    sql = """
        SELECT EXISTS(
            SELECT id
            FROM campaign
            WHERE id = $1
                AND name = 'campaign0'
                AND author_id = 123
                AND publication_envs = '{DATA_TESTING}'
                AND campaign_type = 'ZERO_SPEED_BANNER'
                AND start_datetime = '2019-01-01 00:00:00'
                AND end_datetime = '2019-02-01 00:00:00'
                AND timezone = 'UTC'
                AND platforms = '{METRO}'
                AND (order_id = $2 OR manul_order_id = $3)
                AND rubric = $4
                AND order_size = $5
                AND user_daily_display_limit = 2
                AND user_display_limit = 3
        )
    """
    assert (
        await con.fetchval(
            sql,
            got["id"],
            *order_expected,
            RubricEnum.COMMON.name,
            OrderSizeEnum.SMALL.name,
        )
        is True
    )


@pytest.mark.parametrize("budget", [Decimal("1000"), None])
@pytest.mark.parametrize("daily_budget", [Decimal("5000"), None])
async def test_creates_valid_cpm_billing(
    budget, daily_budget, campaigns_dm, con, campaign_creation_kwargs
):
    cpm_kwargs = {
        "budget": budget,
        "daily_budget": daily_budget,
        "cost": Decimal("12.3456"),
        "auto_daily_budget": True,
    }
    kwargs = campaign_creation_kwargs(cpm=cpm_kwargs)

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = """
        SELECT
            billing_cpm.cost,
            billing_cpm.budget,
            billing_cpm.daily_budget,
            billing_cpm.auto_daily_budget
        FROM billing_cpm
        JOIN campaign_billing
        ON billing_cpm.id = campaign_billing.cpm_id
        JOIN campaign
            ON campaign_billing.id = campaign.billing_id
        WHERE campaign.id = $1
        AND campaign_billing.cpa_id IS NULL
        AND campaign_billing.fix_id IS NULL
    """
    assert dict(await con.fetchrow(sql, campaign_id)) == cpm_kwargs


@pytest.mark.parametrize("budget", [Decimal("1000"), None])
@pytest.mark.parametrize("daily_budget", [Decimal("5000"), None])
async def test_creates_valid_cpa_billing(
    budget, daily_budget, campaigns_dm, con, campaign_creation_kwargs
):
    cpa_kwargs = {
        "budget": budget,
        "daily_budget": daily_budget,
        "cost": Decimal("12.3456"),
        "auto_daily_budget": True,
    }
    kwargs = campaign_creation_kwargs(cpa=cpa_kwargs)

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = """
        SELECT
            billing_cpa.cost,
            billing_cpa.budget,
            billing_cpa.daily_budget,
            billing_cpa.auto_daily_budget
        FROM billing_cpa
        JOIN campaign_billing
             ON billing_cpa.id = campaign_billing.cpa_id
        JOIN campaign
            ON campaign_billing.id = campaign.billing_id
        WHERE campaign.id = $1
             AND campaign_billing.cpm_id IS NULL
             AND campaign_billing.fix_id IS NULL
     """

    assert dict(await con.fetchrow(sql, campaign_id)) == cpa_kwargs


@pytest.mark.parametrize("time_interval", list(FixTimeIntervalEnum))
async def test_creates_valid_fix_billing(
    time_interval, campaigns_dm, con, campaign_creation_kwargs
):
    kwargs = campaign_creation_kwargs(
        fix={"time_interval": time_interval, "cost": Decimal("12.3456")}
    )

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = """
         SELECT EXISTS(
             SELECT billing_fix.id
            FROM billing_fix
            JOIN campaign_billing
                 ON billing_fix.id = campaign_billing.fix_id
            JOIN campaign
                ON campaign_billing.id = campaign.billing_id
            WHERE campaign.id = $1
                 AND campaign_billing.cpm_id IS NULL
                 AND campaign_billing.cpa_id IS NULL
                 AND billing_fix.time_interval = $2
                 AND billing_fix.cost = '12.3456'
         )
     """

    assert await con.fetchval(sql, campaign_id, time_interval.name) is True


@pytest.mark.parametrize(
    "order_extra",
    [{"order_id": 4, "manul_order_id": None}, {"order_id": None, "manul_order_id": 5}],
)
@pytest.mark.parametrize(
    "billing_extra",
    (
        {
            "cpm": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": True,
            }
        },
        {
            "cpa": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": False,
            }
        },
        {
            "fix": {
                "time_interval": FixTimeIntervalEnum.DAILY,
                "cost": Decimal("10.1234"),
            }
        },
    ),
)
async def test_returns_campaign_details(
    order_extra, billing_extra, campaigns_dm, campaign_creation_kwargs
):
    kwargs = campaign_creation_kwargs(**order_extra, **billing_extra)

    got = await campaigns_dm.create_campaign(**kwargs)

    assert got == dict(
        id=Any(int),
        name="campaign0",
        author_id=123,
        created_datetime=Any(datetime),
        publication_envs=[PublicationEnvEnum.DATA_TESTING],
        campaign_type=CampaignTypeEnum.ZERO_SPEED_BANNER,
        start_datetime=dt("2019-01-01 00:00:00"),
        end_datetime=dt("2019-02-01 00:00:00"),
        timezone="UTC",
        platforms=[PlatformEnum.METRO],
        billing=billing_extra,
        comment="",
        actions=[],
        creatives=[],
        placing={},
        rubric=RubricEnum.COMMON,
        order_size=OrderSizeEnum.SMALL,
        status=CampaignStatusEnum.DRAFT,
        targeting={},
        user_daily_display_limit=2,
        user_display_limit=3,
        week_schedule=[],
        discounts=[],
        datatesting_expires_at=dt("2019-04-07 00:00:00"),
        settings={
            "custom_page_id": "abacaba",
            "overview_position": OverviewPositionEnum.FINISH,
            "forced_product_version_datetime": dt("2021-01-01 00:00:00"),
            "verification_data": [
                {"platform": "weborama", "params": {"a": "b", "x": "y"}},
                {"platform": "dcm", "params": {"url": "https://dcm.url"}},
            ],
        },
        moderation_verdicts=None,
        direct_moderation_id=None,
        paid_till=None,
        **order_extra,
    )


@pytest.mark.parametrize(
    "billing_extra",
    (
        {
            "cpm": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
            },
            "cpa": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
            },
        },
        {
            "cpa": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
            },
            "fix": {
                "time_interval": FixTimeIntervalEnum.DAILY,
                "cost": Decimal("10.1234"),
            },
        },
        {
            "cpm": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
            },
            "fix": {
                "time_interval": FixTimeIntervalEnum.DAILY,
                "cost": Decimal("10.1234"),
            },
        },
    ),
)
async def test_raises_for_many_billing_arguments(
    billing_extra, campaigns_dm, campaign_creation_kwargs
):
    kwargs = campaign_creation_kwargs(**billing_extra)

    with pytest.raises(WrongBillingParameters):
        await campaigns_dm.create_campaign(**kwargs)


@pytest.mark.parametrize("status", list(CampaignStatusEnum))
async def test_campaign_created_in_valid_status(
    campaigns_dm, con, status, campaign_creation_kwargs
):
    kwargs = campaign_creation_kwargs(status=status)

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = """
        SELECT *
        FROM status_history
        WHERE campaign_id = $1
    """
    status_entries = list(map(dict, await con.fetch(sql, campaign_id)))
    assert status_entries == [
        {
            "campaign_id": campaign_id,
            "author_id": 123,
            "status": status.name,
            "metadata": {},
            "changed_datetime": Any(datetime),
        }
    ]


@pytest.mark.parametrize(
    "creative",
    [
        {
            "type_": "pin",
            "title": "Заголовок",
            "subtitle": "Подзаголовок",
            "images": [{"file1": "filename1"}, {"file2": "filename2"}],
        },
        {
            "type_": "billboard",
            "title": "Заголовок",
            "description": "Подзаголовок",
            "images": [{"file1": "filename1"}, {"file2": "filename2"}],
            "images_v2": [],
        },
        {
            "type_": "icon",
            "images": [{"file1": "filename1"}, {"file2": "filename2"}],
            "position": 8,
            "title": "Заголовок",
        },
        {
            "type_": "pin_search",
            "images": [{"file1": "filename1"}, {"file2": "filename2"}],
            "title": "Заголовок",
            "organizations": [1, 2, 4],
        },
        {
            "type_": "logo_and_text",
            "images": [{"file1": "filename1"}, {"file2": "filename2"}],
            "text": "Текст",
        },
        {"type_": "text", "text": "Текст", "disclaimer": "Дисклеймер"},
        {
            "type_": "via_point",
            "images": [{"file1": "filename1"}, {"file2": "filename2"}],
            "button_text_active": "Кнопкотекст",
            "button_text_inactive": "Неактивнокнопкотекст",
            "description": "Описание",
        },
        {
            "type_": "banner",
            "images": [{"file1": "filename1"}, {"file2": "filename2"}],
            "disclaimer": "Дисклеймер",
            "show_ads_label": True,
            "description": "Описание",
            "title": "Заголовок",
            "terms": "Условия пользования",
        },
        {
            "type_": "audio_banner",
            "images": [{"file1": "filename1"}, {"file2": "filename2"}],
            "left_anchor": 0.5,
            "audio_file_url": "http://filestorage.ru/my_file.mp3",
        },
    ],
)
async def test_creates_creative(campaigns_dm, con, creative, campaign_creation_kwargs):
    kwargs = campaign_creation_kwargs(creatives=[creative])

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = f"""
        SELECT *
        FROM creative_{creative["type_"]}
        WHERE campaign_id = $1
     """
    created_creatives = list(map(dict, await con.fetch(sql, campaign_id)))

    expected_creatives = [creative.copy()]
    del expected_creatives[0]["type_"]
    expected_creatives[0]["campaign_id"] = campaign_id

    assert created_creatives == expected_creatives


@pytest.mark.parametrize(
    "creatives",
    [
        [],
        [
            {"type_": "text", "text": "Текст 1", "disclaimer": "Дисклеймер 1"},
            {"type_": "text", "text": "Текст 2", "disclaimer": "Дисклеймер 2"},
            {
                "type_": "banner",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "terms": "",
            },
        ],
    ],
)
async def test_creates_multiple_creatives(
    campaigns_dm, con, creatives, campaign_creation_kwargs
):
    kwargs = campaign_creation_kwargs(creatives=creatives)

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = """
        SELECT row_to_json(creative_pin) FROM creative_pin WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(creative_billboard) FROM creative_billboard WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(creative_icon) FROM creative_icon WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(creative_pin_search) FROM creative_pin_search WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(creative_text) FROM creative_text WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(creative_logo_and_text) FROM creative_logo_and_text WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(creative_via_point) FROM creative_via_point WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(creative_banner) FROM creative_banner WHERE campaign_id = $1
    """  # noqa: E501
    got_creatives = list(map(dict, await con.fetch(sql, campaign_id)))

    created_creatives = []
    for creative in got_creatives:
        created_creatives.append(json.loads(creative["row_to_json"]))
        del created_creatives[-1]["campaign_id"]
    expected_creatives = []
    for creative in creatives:
        expected_creatives.append(creative)
        del expected_creatives[-1]["type_"]

    assert created_creatives == expected_creatives


@pytest.mark.parametrize(
    "action",
    [
        {
            "type_": "search",
            "title": "Поискать",
            "organizations": [1, 2, 4],
            "history_text": "Поискал",
            "main": False,
        },
        {"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False},
        {
            "type_": "phone_call",
            "title": "Позвонить",
            "phone": "322-223",
            "main": False,
        },
        {
            "type_": "download_app",
            "title": "Позвонить",
            "google_play_id": "qwerty",
            "app_store_id": "ytrewq",
            "url": "yandex.ru",
            "main": False,
        },
        {"type_": "promocode", "promocode": "12345", "main": False},
        {
            "type_": "resolve_uri",
            "uri": "magic://ya.ru",
            "action_type": ActionTypeEnum.OPEN_SITE,
            "target": ResolveUriTargetEnum.WEB_VIEW,
            "dialog": None,
            "main": False,
        },
        {
            "type_": "resolve_uri",
            "uri": "magic://ya.ru",
            "action_type": ActionTypeEnum.OPEN_SITE,
            "target": ResolveUriTargetEnum.BROWSER,
            "dialog": {
                "title": "title",
                "content": "content",
                "ok": "ok",
                "cancel": "cancel",
                "event_ok": "event_ok",
                "event_cancel": "event_cancel",
            },
            "main": False,
        },
        {
            "type_": "add_point_to_route",
            "latitude": 1.2,
            "longitude": 3.4,
            "main": False,
        },
        {
            "type_": "download_app",
            "title": "Позвонить",
            "google_play_id": "qwerty",
            "app_store_id": "ytrewq",
            "url": "yandex.ru",
            "main": True,
        },
    ],
)
async def test_creates_action(campaigns_dm, con, action, campaign_creation_kwargs):
    kwargs = campaign_creation_kwargs(actions=[action])

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = f"""
        SELECT *
        FROM action_{action["type_"]}
        WHERE campaign_id = $1
    """
    created_actions = list(map(dict, await con.fetch(sql, campaign_id)))

    expected_actions = [action.copy()]
    if expected_actions[0]["type_"] == "resolve_uri":
        expected_actions[0]["action_type"] = expected_actions[0]["action_type"].name
        expected_actions[0]["target"] = expected_actions[0]["target"].name
    del expected_actions[0]["type_"]
    expected_actions[0]["campaign_id"] = campaign_id

    assert created_actions == expected_actions


@pytest.mark.parametrize(
    "actions",
    [
        [],
        [
            {
                "type_": "open_site",
                "title": "Перейти туда",
                "url": "ya.ru",
                "main": False,
            },
            {
                "type_": "open_site",
                "title": "Перейти сюда",
                "url": "yandex.ru",
                "main": False,
            },
            {
                "type_": "phone_call",
                "title": "Позвонить",
                "phone": "322-223",
                "main": False,
            },
        ],
    ],
)
async def test_creates_multiple_actions(
    campaigns_dm, con, actions, campaign_creation_kwargs
):
    kwargs = campaign_creation_kwargs(actions=actions)

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = """
        SELECT row_to_json(action_search) FROM action_search WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(action_open_site) FROM action_open_site WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(action_phone_call) FROM action_phone_call WHERE campaign_id = $1
        UNION ALL SELECT row_to_json(action_download_app) FROM action_download_app WHERE campaign_id = $1
    """  # noqa: E501
    got_actions = list(map(dict, await con.fetch(sql, campaign_id)))

    created_actions = []
    for action in got_actions:
        created_actions.append(json.loads(action["row_to_json"]))
        del created_actions[-1]["campaign_id"]
    expected_actions = []
    for action in actions:
        expected_actions.append(action)
        del expected_actions[-1]["type_"]

    assert created_actions == expected_actions


@pytest.mark.parametrize(
    ("placing", "expected_organizations", "expected_areas"),
    [
        (
            {"organizations": {"permalinks": [213, 456]}},
            [{"permalinks": [213, 456]}],
            [],
        ),
        (
            {
                "area": {
                    "areas": [
                        {
                            "points": [
                                {"lon": 12.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 25.567},
                                {"lon": 12.345, "lat": 23.567},
                            ],
                            "name": "Танковый полигон",
                        },
                        {
                            "points": [
                                {"lon": 22.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 35.567},
                                {"lon": 22.345, "lat": 33.567},
                            ],
                            "name": "Ядерный полигон",
                        },
                    ],
                    "version": 1,
                }
            },
            [],
            [
                {
                    "areas": [
                        {
                            "points": [
                                {"lon": 12.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 25.567},
                                {"lon": 12.345, "lat": 23.567},
                            ],
                            "name": "Танковый полигон",
                        },
                        {
                            "points": [
                                {"lon": 22.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 35.567},
                                {"lon": 22.345, "lat": 33.567},
                            ],
                            "name": "Ядерный полигон",
                        },
                    ],
                    "version": 1,
                }
            ],
        ),
        (
            {
                "organizations": {"permalinks": [213, 456]},
                "area": {
                    "areas": [
                        {
                            "points": [
                                {"lon": 12.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 25.567},
                                {"lon": 12.345, "lat": 23.567},
                            ],
                            "name": "Танковый полигон",
                        },
                        {
                            "points": [
                                {"lon": 22.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 35.567},
                                {"lon": 22.345, "lat": 33.567},
                            ],
                            "name": "Ядерный полигон",
                        },
                    ],
                    "version": 1,
                },
            },
            [{"permalinks": [213, 456]}],
            [
                {
                    "areas": [
                        {
                            "points": [
                                {"lon": 12.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 25.567},
                                {"lon": 12.345, "lat": 23.567},
                            ],
                            "name": "Танковый полигон",
                        },
                        {
                            "points": [
                                {"lon": 22.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 35.567},
                                {"lon": 22.345, "lat": 33.567},
                            ],
                            "name": "Ядерный полигон",
                        },
                    ],
                    "version": 1,
                }
            ],
        ),
        ({}, [], []),
    ],
)
async def test_creates_placing(
    campaigns_dm,
    con,
    placing,
    expected_organizations,
    expected_areas,
    campaign_creation_kwargs,
):
    kwargs = campaign_creation_kwargs(placing=placing)

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = """
        SELECT permalinks
        FROM campaign_placing_organizations
        WHERE campaign_id = $1
    """
    organizations = list(map(dict, await con.fetch(sql, campaign_id)))
    sql = """
        SELECT areas, version
        FROM campaign_placing_area
        WHERE campaign_id = $1
    """
    areas = list(map(dict, await con.fetch(sql, campaign_id)))

    assert organizations == expected_organizations
    assert areas == expected_areas


@pytest.mark.parametrize(
    "week_schedule",
    [
        [{"start": 60 * 24 * 3, "end": 60 * 24 * 4}],
        [
            {"start": 60 * 24 * 3, "end": 60 * 24 * 4},
            {"start": 60 * 24 * 5, "end": 60 * 24 * 6},
        ],
        [],
    ],
)
async def test_creates_week_schedules(
    campaigns_dm, con, week_schedule, campaign_creation_kwargs
):
    kwargs = campaign_creation_kwargs(week_schedule=week_schedule)

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = """
        SELECT *
        FROM campaign_week_schedule
        WHERE campaign_id = $1
        ORDER BY start, "end"
    """
    got_schedule = list(map(dict, await con.fetch(sql, campaign_id)))
    created_schedule = []
    for schedule in got_schedule:
        created_schedule.append(schedule)
        del created_schedule[-1]["campaign_id"]

    assert created_schedule == week_schedule


@pytest.mark.parametrize(
    "discounts",
    [
        [],
        [
            {
                "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                "cost_multiplier": Decimal("0.75"),
            }
        ],
        [
            {
                "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                "cost_multiplier": Decimal("0.75"),
            },
            {
                "start_datetime": datetime(2019, 3, 3, tzinfo=timezone.utc),
                "end_datetime": datetime(2019, 4, 4, tzinfo=timezone.utc),
                "cost_multiplier": Decimal("1.35"),
            },
        ],
    ],
)
async def test_creates_discounts(
    campaigns_dm, con, discounts, campaign_creation_kwargs
):
    kwargs = campaign_creation_kwargs(discounts=discounts)

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    sql = """
        SELECT start_datetime, end_datetime, cost_multiplier
        FROM campaign_discounts
        WHERE campaign_id = $1
        ORDER BY start_datetime
    """
    created_discounts = list(map(dict, await con.fetch(sql, campaign_id)))
    assert created_discounts == discounts


async def test_adds_change_log_record_for_created_campaign(
    factory, campaigns_dm, campaign_creation_kwargs
):
    kwargs = campaign_creation_kwargs()

    campaign_id = (await campaigns_dm.create_campaign(**kwargs))["id"]

    result = await factory.list_campaign_change_log(campaign_id)
    assert result == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign_id,
            "author_id": 123,
            "status": "DRAFT",
            "system_metadata": {"action": "campaign.created"},
            "state_before": {},
            "state_after": Any(dict),
            "is_latest": True,
        }
    ]

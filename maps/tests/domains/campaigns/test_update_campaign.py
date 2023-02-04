import itertools
from datetime import datetime, timezone
from decimal import Decimal
from unittest.mock import ANY

import pytest

from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    OrderSizeEnum,
    PlatformEnum,
    PublicationEnvEnum,
    RubricEnum,
)
from maps_adv.adv_store.v2.lib.domains.campaigns import (
    BillingTypeChange,
    CampaignNotFound,
    CampaignsDomain,
    InvalidCustomPageId,
    OrderIdChange,
    WrongCampaignTypeForPlatform,
    WrongCreativesForCampaignType,
    WrongPlacingForCampaignType,
)
from maps_adv.adv_store.v2.tests import all_creative_type_combinations
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def domain(campaigns_dm, events_dm, moderation_dm, config, billing_proxy_client):
    return CampaignsDomain(
        campaigns_dm,
        events_dm,
        moderation_dm,
        config["DASHBOARD_API_URL"],
        billing_proxy_client,
    )


@pytest.fixture
def campaign_update_data():
    def func(**overrides):
        data = dict(
            name="Новое имя",
            author_id=321,
            publication_envs=[PublicationEnvEnum.PRODUCTION],
            start_datetime=datetime(2020, 3, 3, tzinfo=timezone.utc),
            end_datetime=datetime(2020, 4, 4, tzinfo=timezone.utc),
            timezone="Europe/Moscow",
            platforms=[PlatformEnum.NAVI],
            order_id=15,
            manul_order_id=None,
            rubric=RubricEnum.REALTY,
            order_size=OrderSizeEnum.BIG,
            targeting={"tag": "gender", "content": "male"},
            user_daily_display_limit=8,
            user_display_limit=9,
            comment="Новый коммент",
            datatesting_expires_at=datetime(2020, 4, 7, tzinfo=timezone.utc),
            settings={},
            creatives=[
                {
                    "type_": "pin",
                    "title": "Заголовок",
                    "subtitle": "Подзаголовок",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                },
                {
                    "type_": "banner",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "disclaimer": "Дисклеймер",
                    "show_ads_label": True,
                    "description": "Описание",
                    "title": "Заголовок",
                },
            ],
            actions=[],
            week_schedule=[],
            placing={"organizations": {"permalinks": [1000, 2000]}},
            status=CampaignStatusEnum.REVIEW,
            discounts=[],
        )

        data.update(overrides)

        if not any(billing in overrides for billing in ("cpm", "cpa", "fix")):
            data["cpm"] = {
                "cost": Decimal("10"),
                "budget": Decimal("3000"),
                "dauly_budget": Decimal("1000"),
                "auto_daily_budget": False,
            }

        return data

    return func


@pytest.fixture
def campaign_dm_update_data_return():
    def func(**overrides):
        data = dict(
            id=1,
            name="Новое имя",
            author_id=321,
            publication_envs=[PublicationEnvEnum.PRODUCTION],
            start_datetime=datetime(2020, 3, 3, tzinfo=timezone.utc),
            end_datetime=datetime(2020, 4, 4, tzinfo=timezone.utc),
            timezone="Europe/Moscow",
            platforms=[PlatformEnum.NAVI],
            order_id=15,
            manul_order_id=None,
            rubric=RubricEnum.REALTY,
            order_size=OrderSizeEnum.BIG,
            targeting={"tag": "gender", "content": "male"},
            user_daily_display_limit=8,
            user_display_limit=9,
            comment="Новый коммент",
            datatesting_expires_at=datetime(2020, 4, 7, tzinfo=timezone.utc),
            settings={},
            creatives=[
                {
                    "type_": "pin",
                    "title": "Заголовок",
                    "subtitle": "Подзаголовок",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                },
                {
                    "type_": "banner",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "disclaimer": "Дисклеймер",
                    "show_ads_label": True,
                    "description": "Описание",
                    "title": "Заголовок",
                },
            ],
            actions=[],
            week_schedule=[],
            placing={"organizations": {"permalinks": [1000, 2000]}},
            status=CampaignStatusEnum.REVIEW,
            discounts=[],
            billing={
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "dauly_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
        )

        data.update(overrides)

        return data

    return func


@pytest.fixture(autouse=True)
def common_dm_mocks(campaigns_dm):
    campaigns_dm.retrieve_campaign.coro.return_value = {
        "id": 15,
        "author_id": 123,
        "created_datetime": datetime(2018, 2, 3, tzinfo=timezone.utc),
        "name": "Название кампании",
        "timezone": "UTC",
        "comment": "Коммент",
        "user_display_limit": 2,
        "user_daily_display_limit": 3,
        "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
        "end_datetime": datetime(2019, 2, 1, tzinfo=timezone.utc),
        "targeting": {},
        "publication_envs": [PublicationEnvEnum.DATA_TESTING],
        "campaign_type": CampaignTypeEnum.PIN_ON_ROUTE,
        "platforms": [PlatformEnum.NAVI],
        "rubric": None,
        "order_size": None,
        "order_id": 15,
        "manul_order_id": None,
        "billing": {
            "cpm": {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        "actions": [],
        "creatives": [],
        "placing": {"organizations": {"permalinks": [123, 345]}},
        "week_schedule": [],
        "status": CampaignStatusEnum.DRAFT,
        "discounts": [],
        "datatesting_expires_at": datetime(2019, 4, 7, tzinfo=timezone.utc),
        "settings": {},
    }


@pytest.mark.parametrize(
    "billing_extra",
    [
        {
            "cpm": {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        {
            "cpa": {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": True,
            }
        },
        {
            "fix": {
                "cost": Decimal("12.3456"),
                "time_interval": FixTimeIntervalEnum.MONTHLY,
            }
        },
    ],
)
async def test_uses_dm(
    domain,
    campaigns_dm,
    campaign_update_data,
    campaign_dm_update_data_return,
    billing_extra,
    billing_proxy_client,
):
    cpm = Decimal(0)
    if "cpm" in billing_extra:
        cpm = billing_extra["cpm"]["cost"]
    if "cpa" in billing_extra:
        cpm = billing_extra["cpa"]["cost"]
    billing_proxy_client.calculate_product_cpm.coro.return_value = cpm
    campaigns_dm.retrieve_campaign.coro.return_value["billing"] = billing_extra
    billing_data = dict.fromkeys(["cpm", "cpa", "fix"])
    billing_data.update(billing_extra)
    campaigns_dm.update_campaign.coro.return_value = campaign_dm_update_data_return(
        billing=billing_data
    )

    result = await domain.update_campaign(
        15, **campaign_update_data(**billing_extra, product_id=1)
    )

    campaigns_dm.update_campaign.assert_called_with(
        15, **campaign_update_data(**billing_data)
    )
    assert result == campaigns_dm.update_campaign.coro.return_value


@pytest.mark.parametrize(
    "rubric, order_size", itertools.zip_longest(RubricEnum, list(OrderSizeEnum))
)
async def test_calls_billing_client(
    domain,
    campaigns_dm,
    campaign_update_data,
    campaign_dm_update_data_return,
    billing_proxy_client,
    rubric,
    order_size,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(10)
    campaigns_dm.update_campaign.coro.return_value = campaign_dm_update_data_return()

    kwargs = campaign_update_data(rubric=rubric, order_size=order_size)
    result = await domain.update_campaign(15, **kwargs, product_id=1)

    expected_kwargs = kwargs.copy()
    expected_kwargs["cpa"] = None
    expected_kwargs["fix"] = None
    campaigns_dm.update_campaign.assert_called_with(
        15, **campaign_update_data(**expected_kwargs)
    )
    assert result == campaigns_dm.update_campaign.coro.return_value
    billing_proxy_client.calculate_product_cpm.coro.assert_called_with(
        1,
        rubric=ANY,
        targeting_query=kwargs["targeting"],
        dt=kwargs["start_datetime"],
        order_size=ANY,
        creative_types=ANY,
    )


async def test_raises_for_nonexistent_campaign(
    domain, campaigns_dm, campaign_update_data
):
    campaigns_dm.retrieve_campaign.coro.side_effect = CampaignNotFound

    with pytest.raises(CampaignNotFound):
        await domain.update_campaign(15, **campaign_update_data())


async def test_supports_multiple_platforms(
    campaigns_dm,
    domain,
    campaign_update_data,
    campaign_dm_update_data_return,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(10.0)
    campaigns_dm.update_campaign.coro.return_value = campaign_dm_update_data_return()
    await domain.update_campaign(
        15,
        **campaign_update_data(
            platforms=[PlatformEnum.NAVI, PlatformEnum.MAPS], product_id=1
        ),
    )


async def test_raises_for_unsupported_campaign_type_and_platform_navi(
    domain, campaign_update_data
):
    with pytest.raises(WrongCampaignTypeForPlatform):
        await domain.update_campaign(
            15, **campaign_update_data(platforms=[PlatformEnum.METRO])
        )


@pytest.mark.parametrize(
    "creative_types",
    all_creative_type_combinations - {("pin", "logo_and_text"), ("pin", "banner")},
)
async def test_raises_for_invalid_creative_types_for_pin_on_route_navi(
    domain, campaigns_dm, campaign_update_data, creative_types
):
    campaigns_dm.retrieve_campaign.coro.return_value[
        "campaign_type"
    ] = CampaignTypeEnum.PIN_ON_ROUTE

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=[PlatformEnum.NAVI],
                creatives=list(
                    {"type_": creative_type, "field": "value"}
                    for creative_type in creative_types
                ),
            ),
        )


@pytest.mark.parametrize(
    "creative_types",
    all_creative_type_combinations
    - {("pin", "logo_and_text", "banner"), ("pin", "banner")},
)
async def test_raises_for_invalid_creative_types_for_pin_on_route_maps(
    domain, campaigns_dm, campaign_update_data, creative_types
):
    campaigns_dm.retrieve_campaign.coro.return_value[
        "campaign_type"
    ] = CampaignTypeEnum.PIN_ON_ROUTE

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=[PlatformEnum.MAPS],
                creatives=list(
                    {"type_": creative_type, "field": "value"}
                    for creative_type in creative_types
                ),
                product_id=1,
            ),
        )


@pytest.mark.parametrize(
    "creative_types", all_creative_type_combinations - {("billboard", "banner")}
)
async def test_raises_for_invalid_creative_types_for_billboard(
    domain, campaigns_dm, campaign_update_data, creative_types
):
    campaigns_dm.retrieve_campaign.coro.return_value[
        "campaign_type"
    ] = CampaignTypeEnum.BILLBOARD

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=[PlatformEnum.NAVI],
                creatives=list(
                    {"type_": creative_type, "field": "value"}
                    for creative_type in creative_types
                ),
            ),
        )


@pytest.mark.parametrize(
    "creative_types",
    all_creative_type_combinations - {("banner",), ("banner", "audio_banner")},
)
async def test_raises_for_invalid_creative_types_for_zero_speed_banner(
    domain, campaigns_dm, campaign_update_data, creative_types
):
    campaigns_dm.retrieve_campaign.coro.return_value[
        "campaign_type"
    ] = CampaignTypeEnum.ZERO_SPEED_BANNER

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=[PlatformEnum.NAVI],
                creatives=list(
                    {"type_": creative_type, "field": "value"}
                    for creative_type in creative_types
                ),
            ),
        )


@pytest.mark.parametrize(
    "creative_types", all_creative_type_combinations - {("banner",)}
)
async def test_raises_for_invalid_creative_types_for_overview_banner(
    domain, campaigns_dm, campaign_update_data, creative_types
):
    campaigns_dm.retrieve_campaign.coro.return_value[
        "campaign_type"
    ] = CampaignTypeEnum.OVERVIEW_BANNER

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=[PlatformEnum.NAVI],
                creatives=list(
                    {"type_": creative_type, "field": "value"}
                    for creative_type in creative_types
                ),
            ),
        )


@pytest.mark.parametrize("platforms", [[PlatformEnum.NAVI], [PlatformEnum.MAPS]])
@pytest.mark.parametrize(
    "creative_types", all_creative_type_combinations - {("icon", "pin_search", "text")}
)
async def test_raises_for_invalid_creative_types_for_category_icon(
    domain,
    campaigns_dm,
    campaign_update_data,
    platforms,
    creative_types,
):
    campaigns_dm.retrieve_campaign.coro.return_value[
        "campaign_type"
    ] = CampaignTypeEnum.CATEGORY_SEARCH

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=platforms,
                creatives=list(
                    {"type_": creative_type, "field": "value"}
                    for creative_type in creative_types
                ),
            ),
        )


@pytest.mark.parametrize(
    "creative_types", all_creative_type_combinations - {("banner",)}
)
async def test_raises_for_invalid_creative_types_for_route_banner(
    domain, campaigns_dm, campaign_update_data, creative_types
):
    campaigns_dm.retrieve_campaign.coro.return_value[
        "campaign_type"
    ] = CampaignTypeEnum.ROUTE_BANNER

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=[PlatformEnum.METRO],
                creatives=list(
                    {"type_": creative_type, "field": "value"}
                    for creative_type in creative_types
                ),
            ),
        )


@pytest.mark.parametrize(
    "creative_types", [all_creative_type_combinations - {("via_point",)}]
)
async def test_raises_for_invalid_creative_types_for_via_points(
    domain, campaigns_dm, campaign_update_data, creative_types
):
    campaigns_dm.retrieve_campaign.coro.return_value[
        "campaign_type"
    ] = CampaignTypeEnum.VIA_POINTS

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=[PlatformEnum.NAVI],
                creatives=list(
                    {"type_": creative_type, "field": "value"}
                    for creative_type in creative_types
                ),
            ),
        )


@pytest.mark.parametrize(
    "creative_types", [all_creative_type_combinations - {("banner",)}]
)
async def test_raises_for_invalid_creative_types_for_promocode(
    domain, campaigns_dm, campaign_update_data, creative_types
):
    campaigns_dm.retrieve_campaign.coro.return_value[
        "campaign_type"
    ] = CampaignTypeEnum.PROMOCODE

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=[PlatformEnum.METRO],
                creatives=list(
                    {"type_": creative_type, "field": "value"}
                    for creative_type in creative_types
                ),
            ),
        )


@pytest.mark.parametrize(
    ("campaign_type", "creative_types", "placing"),
    [
        (
            CampaignTypeEnum.PIN_ON_ROUTE,
            ["pin", "banner"],
            {"area": {"areas": [], "version": 1}},
        ),
        (
            CampaignTypeEnum.CATEGORY_SEARCH,
            ["icon", "pin_search", "text"],
            {"area": {"areas": [], "version": 1}},
        ),
        (
            CampaignTypeEnum.VIA_POINTS,
            ["via_point"],
            {"area": {"areas": [], "version": 1}},
        ),
        (
            CampaignTypeEnum.BILLBOARD,
            ["billboard", "banner"],
            {"organizations": {"permalinks": [123, 456]}},
        ),
        (
            CampaignTypeEnum.ZERO_SPEED_BANNER,
            ["banner"],
            {"organizations": {"permalinks": [123, 456]}},
        ),
        (
            CampaignTypeEnum.OVERVIEW_BANNER,
            ["banner"],
            {"organizations": {"permalinks": [123, 456]}},
        ),
        (
            CampaignTypeEnum.ROUTE_BANNER,
            ["banner"],
            {"organizations": {"permalinks": [123, 456]}},
        ),
        (
            CampaignTypeEnum.PROMOCODE,
            ["banner"],
            {"organizations": {"permalinks": [123, 456]}},
        ),
    ],
)
async def test_raises_for_invalid_placing(
    domain,
    campaigns_dm,
    campaign_update_data,
    campaign_type,
    creative_types,
    placing,
):
    campaigns_dm.retrieve_campaign.coro.return_value["campaign_type"] = campaign_type

    if campaign_type in (CampaignTypeEnum.ROUTE_BANNER, CampaignTypeEnum.PROMOCODE):
        platforms = [PlatformEnum.METRO]
    else:
        platforms = [PlatformEnum.NAVI]
    with pytest.raises(WrongPlacingForCampaignType):
        await domain.update_campaign(
            15,
            **campaign_update_data(
                platforms=platforms,
                placing=placing,
                creatives=list({"type_": type_} for type_ in creative_types),
            ),
        )


# discounts are ignored on update yet
# async def test_raises_for_date_intersecting_discounts():
#     pass


@pytest.mark.parametrize(
    ("old_billing", "new_billing"),
    itertools.permutations(
        (
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            {
                "fix": {
                    "cost": Decimal("10"),
                    "time_interval": FixTimeIntervalEnum.DAILY,
                }
            },
        ),
        r=2,
    ),
)
async def test_raises_if_billing_type_changed(
    domain,
    campaigns_dm,
    campaign_update_data,
    old_billing,
    new_billing,
):
    campaigns_dm.retrieve_campaign.coro.return_value["billing"] = old_billing

    billing_data = dict.fromkeys(["cpm", "cpa", "fix"])
    billing_data.update(new_billing)
    with pytest.raises(BillingTypeChange):
        await domain.update_campaign(15, **campaign_update_data(**billing_data))


@pytest.mark.parametrize(
    ("old_order_id", "new_order_id"), [(5, 10), (5, None), (None, 10)]
)
async def test_raises_if_order_id_changed(
    domain,
    campaigns_dm,
    campaign_update_data,
    old_order_id,
    new_order_id,
):
    campaigns_dm.retrieve_campaign.coro.return_value["order_id"] = old_order_id

    with pytest.raises(OrderIdChange):
        await domain.update_campaign(15, **campaign_update_data(order_id=new_order_id))


@pytest.mark.parametrize(
    ("old_end_datetime", "new_end_datetime"),
    [
        (
            datetime(2020, 4, 4, tzinfo=timezone.utc),
            datetime(2020, 4, 5, tzinfo=timezone.utc),
        ),
        (
            datetime(2020, 4, 4, tzinfo=timezone.utc),
            datetime(2020, 4, 3, tzinfo=timezone.utc),
        ),
    ],
)
async def test_calls_event_dm_end_data_changed(
    domain,
    campaigns_dm,
    events_dm,
    campaign_update_data,
    campaign_dm_update_data_return,
    old_end_datetime,
    new_end_datetime,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(10.0)
    campaigns_dm.retrieve_campaign.coro.return_value["end_datetime"] = old_end_datetime
    campaigns_dm.update_campaign.coro.return_value = campaign_dm_update_data_return(
        id=15, end_datetime=new_end_datetime, status=CampaignStatusEnum.REVIEW
    )
    campaigns_dm.retrieve_campaign.coro.return_value[
        "status"
    ] = CampaignStatusEnum.DRAFT

    await domain.update_campaign(
        15,
        **campaign_update_data(
            author_id=333,
            end_datetime=new_end_datetime,
            status=CampaignStatusEnum.REVIEW,
        ),
        product_id=1,
    )

    events_dm.create_event_end_datetime_changed.assert_called_once_with(
        campaign_id=15, initiator_id=333, prev_datetime=old_end_datetime
    )


@pytest.mark.parametrize(
    ("old_billing", "new_billing"),
    [
        (
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("2000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("2000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
        ),
        (
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": None,
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("2000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
        ),
    ],
)
async def test_calls_event_dm_budget_decreased(
    domain,
    campaigns_dm,
    events_dm,
    campaign_update_data,
    campaign_dm_update_data_return,
    old_billing,
    new_billing,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(10.0)
    campaigns_dm.retrieve_campaign.coro.return_value["billing"] = old_billing
    campaigns_dm.retrieve_campaign.coro.return_value["end_datetime"] = datetime(
        2020, 4, 4, tzinfo=timezone.utc
    )
    campaigns_dm.retrieve_campaign.coro.return_value[
        "status"
    ] = CampaignStatusEnum.DRAFT

    billing_data = dict.fromkeys(["cpm", "cpa", "fix"])
    billing_data.update(new_billing)
    campaigns_dm.update_campaign.coro.return_value = campaign_dm_update_data_return(
        id=15, billing=billing_data, status=CampaignStatusEnum.REVIEW
    )

    await domain.update_campaign(
        15,
        **campaign_update_data(
            **billing_data,
            author_id=321,
            end_datetime=datetime(2020, 4, 4, tzinfo=timezone.utc),
        ),
        product_id=1,
    )

    events_dm.create_event_budget_decreased.assert_called_once_with(
        campaign_id=15, initiator_id=321, prev_budget=Decimal("3000")
    )


@pytest.mark.parametrize(
    ("old_billing", "new_billing"),
    [
        (
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("4000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("2000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("4000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
        ),
        (
            {
                "fix": {
                    "cost": Decimal("20"),
                    "time_interval": FixTimeIntervalEnum.DAILY,
                }
            },
            {
                "fix": {
                    "cost": Decimal("10"),
                    "time_interval": FixTimeIntervalEnum.DAILY,
                }
            },
        ),
        (
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": None,
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": None,
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": None,
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": None,
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
        ),
        (
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": None,
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("1000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": None,
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("1000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
        ),
    ],
)
async def test_does_not_calls_event_dm_budget_not_decreased(
    domain,
    campaigns_dm,
    events_dm,
    campaign_update_data,
    campaign_dm_update_data_return,
    old_billing,
    new_billing,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(10.0)
    campaigns_dm.retrieve_campaign.coro.return_value["billing"] = old_billing
    campaigns_dm.retrieve_campaign.coro.return_value["end_datetime"] = datetime(
        2020, 4, 4, tzinfo=timezone.utc
    )
    campaigns_dm.retrieve_campaign.coro.return_value[
        "status"
    ] = CampaignStatusEnum.DRAFT

    billing_data = dict.fromkeys(["cpm", "cpa", "fix"])
    billing_data.update(new_billing)
    campaigns_dm.update_campaign.coro.return_value = campaign_dm_update_data_return(
        id=15, billing=billing_data, status=CampaignStatusEnum.REVIEW
    )

    await domain.update_campaign(
        15,
        **campaign_update_data(
            **billing_data,
            author_id=321,
            end_datetime=datetime(2020, 4, 4, tzinfo=timezone.utc),
            status=CampaignStatusEnum.REVIEW,
        ),
        product_id=1,
    )

    events_dm.create_event.assert_not_called()


@pytest.mark.parametrize(
    ("old_billing", "new_billing"),
    [
        (
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {
                "cpm": {
                    "cost": Decimal("10"),
                    "budget": Decimal("2000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("3000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            {
                "cpa": {
                    "cost": Decimal("10"),
                    "budget": Decimal("2000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
        ),
    ],
)
async def test_calls_event_dm_both_budget_decreased_and_endtime_changed(
    domain,
    campaigns_dm,
    events_dm,
    campaign_update_data,
    campaign_dm_update_data_return,
    old_billing,
    new_billing,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(10.0)
    campaigns_dm.retrieve_campaign.coro.return_value["billing"] = old_billing
    campaigns_dm.retrieve_campaign.coro.return_value["end_datetime"] = datetime(
        2020, 4, 4, tzinfo=timezone.utc
    )
    campaigns_dm.retrieve_campaign.coro.return_value[
        "status"
    ] = CampaignStatusEnum.DRAFT

    billing_data = dict.fromkeys(["cpm", "cpa", "fix"])
    billing_data.update(new_billing)
    campaigns_dm.update_campaign.coro.return_value = campaign_dm_update_data_return(
        id=15,
        end_datetime=datetime(2020, 4, 5, tzinfo=timezone.utc),
        billing=billing_data,
        status=CampaignStatusEnum.REVIEW,
    )

    await domain.update_campaign(
        15,
        **campaign_update_data(
            **billing_data,
            author_id=321,
            end_datetime=datetime(2020, 4, 5, tzinfo=timezone.utc),
            status=CampaignStatusEnum.REVIEW,
        ),
        product_id=1,
    )

    events_dm.create_event_end_datetime_changed.assert_called_once_with(
        campaign_id=15,
        initiator_id=321,
        prev_datetime=datetime(2020, 4, 4, tzinfo=timezone.utc),
    )
    events_dm.create_event_budget_decreased.assert_called_once_with(
        campaign_id=15, initiator_id=321, prev_budget=Decimal("3000")
    )


@pytest.mark.parametrize(
    ("prev_status", "new_status"),
    [
        (CampaignStatusEnum.ACTIVE, CampaignStatusEnum.DONE),
        (CampaignStatusEnum.PAUSED, CampaignStatusEnum.ACTIVE),
        (CampaignStatusEnum.PAUSED, CampaignStatusEnum.DONE),
        (CampaignStatusEnum.PAUSED, CampaignStatusEnum.DRAFT),
        (CampaignStatusEnum.PAUSED, CampaignStatusEnum.ARCHIVED),
        (CampaignStatusEnum.DRAFT, CampaignStatusEnum.ACTIVE),
        (CampaignStatusEnum.DRAFT, CampaignStatusEnum.ARCHIVED),
        (CampaignStatusEnum.REVIEW, CampaignStatusEnum.REJECTED),
    ],
)
async def test_does_not_calls_event_dm_if_status_is_not_draft_to_review(
    domain,
    campaigns_dm,
    events_dm,
    campaign_update_data,
    campaign_dm_update_data_return,
    prev_status,
    new_status,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(10.0)
    campaigns_dm.retrieve_campaign.coro.return_value["billing"] = {
        "cpm": {
            "cost": Decimal("10"),
            "budget": Decimal("3000"),
            "daily_budget": Decimal("1000"),
            "auto_daily_budget": False,
        }
    }
    campaigns_dm.retrieve_campaign.coro.return_value["end_datetime"] = datetime(
        2020, 4, 4, tzinfo=timezone.utc
    )
    campaigns_dm.retrieve_campaign.coro.return_value["status"] = prev_status

    billing_data = dict.fromkeys(["cpm", "cpa", "fix"])
    billing_data.update(
        {
            "cpm": {
                "cost": Decimal("10"),
                "budget": Decimal("2000"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": False,
            }
        }
    )
    campaigns_dm.update_campaign.coro.return_value = campaign_dm_update_data_return(
        id=15,
        billing=billing_data,
        end_datetime=datetime(2020, 4, 5, tzinfo=timezone.utc),
        status=new_status,
    )

    await domain.update_campaign(
        15,
        **campaign_update_data(
            **billing_data,
            author_id=321,
            end_datetime=datetime(2020, 4, 5, tzinfo=timezone.utc),
            status=new_status,
        ),
        product_id=1,
    )

    events_dm.create_event.assert_not_called()


async def test_raises_for_invalid_custom_page_id(
    domain, campaigns_dm, campaign_update_data
):
    with pytest.raises(InvalidCustomPageId):
        await domain.update_campaign(
            15, **campaign_update_data(settings={"custom_page_id": "BAD_PAGE_ID"})
        )

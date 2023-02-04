from datetime import datetime, timezone
from decimal import Decimal
from unittest.mock import ANY

import itertools
import pytest

from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    OrderSizeEnum,
    PlatformEnum,
    PublicationEnvEnum,
    RubricEnum,
)
from maps_adv.adv_store.v2.lib.domains.campaigns import (
    CampaignsDomain,
    PeriodIntersectingDiscounts,
    WrongCampaignTypeForPlatform,
    WrongCreativesForCampaignType,
    WrongPlacingForCampaignType,
    InvalidBilling,
    InvalidCustomPageId,
)
from maps_adv.adv_store.v2.tests import all_creative_type_combinations, dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


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
            actions=[],
            week_schedule=[],
            placing={
                "organizations": {"permalinks": [123, 456]},
                "area": {"areas": [], "version": 1},
            },
            status=CampaignStatusEnum.DRAFT,
            rubric=RubricEnum.COMMON,
            order_size=OrderSizeEnum.SMALL,
            targeting={},
            user_daily_display_limit=2,
            user_display_limit=3,
            comment="",
            datatesting_expires_at=dt("2019-04-07 00:00:00"),
            settings={},
            discounts=[],
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

        if "platforms" not in kwargs:
            if kwargs["campaign_type"] in (
                CampaignTypeEnum.ROUTE_BANNER,
                CampaignTypeEnum.PROMOCODE,
            ):
                kwargs["platforms"] = [PlatformEnum.METRO]
            else:
                kwargs["platforms"] = [PlatformEnum.NAVI]

        if "creatives" not in kwargs:
            creative_types = {
                CampaignTypeEnum.PIN_ON_ROUTE: ["pin", "banner"],
                CampaignTypeEnum.BILLBOARD: ["billboard", "banner"],
                CampaignTypeEnum.ZERO_SPEED_BANNER: ["banner"],
                CampaignTypeEnum.CATEGORY_SEARCH: ["icon", "pin_search", "text"],
                CampaignTypeEnum.VIA_POINTS: ["via_point"],
                CampaignTypeEnum.ROUTE_BANNER: ["banner"],
                CampaignTypeEnum.OVERVIEW_BANNER: ["banner"],
                CampaignTypeEnum.PROMOCODE: ["banner"],
            }[kwargs["campaign_type"]]

            kwargs["creatives"] = list({"type_": type_} for type_ in creative_types)

        return kwargs

    return func


@pytest.fixture
def domain(campaigns_dm, events_dm, moderation_dm, config, billing_proxy_client):
    return CampaignsDomain(
        campaigns_dm,
        events_dm,
        moderation_dm,
        config["DASHBOARD_API_URL"],
        billing_proxy_client,
    )


async def test_calls_data_manager(
    domain,
    campaigns_dm,
    campaign_creation_kwargs,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(100.0)

    kwargs = campaign_creation_kwargs()

    await domain.create_campaign(**kwargs, product_id=1)

    expected_kwargs = kwargs.copy()
    expected_kwargs["cpa"] = None
    expected_kwargs["fix"] = None
    expected_kwargs["manul_order_id"] = None

    campaigns_dm.create_campaign.assert_called_with(**expected_kwargs)


@pytest.mark.parametrize(
    "rubric, order_size", itertools.zip_longest(RubricEnum, OrderSizeEnum)
)
async def test_calls_billing_client(
    domain,
    campaigns_dm,
    campaign_creation_kwargs,
    billing_proxy_client,
    rubric,
    order_size,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(100.0)

    kwargs = campaign_creation_kwargs(rubric=rubric, order_size=order_size)

    await domain.create_campaign(**kwargs, product_id=1)

    expected_kwargs = kwargs.copy()
    expected_kwargs["cpa"] = None
    expected_kwargs["fix"] = None
    expected_kwargs["manul_order_id"] = None

    campaigns_dm.create_campaign.assert_called_with(**expected_kwargs)
    billing_proxy_client.calculate_product_cpm.coro.assert_called_with(
        1,
        rubric=ANY,
        targeting_query=kwargs["targeting"],
        dt=kwargs["start_datetime"],
        order_size=ANY,
        creative_types=ANY,
    )


async def test_returns_data_from_data_manager(
    domain, campaigns_dm, campaign_creation_kwargs, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(100.0)

    kwargs = campaign_creation_kwargs()
    campaigns_dm.create_campaign.coro.return_value = {"id": 12, "many": "fields"}

    got = await domain.create_campaign(**kwargs, product_id=1)

    assert got == {"id": 12, "many": "fields"}


async def test_supports_multiple_platform(
    domain, campaign_creation_kwargs, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(100.0)

    kwargs = campaign_creation_kwargs(
        platforms=[PlatformEnum.NAVI, PlatformEnum.MAPS],
        campaign_type=CampaignTypeEnum.PIN_ON_ROUTE,
        product_id=1,
    )

    await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "campaign_type", [CampaignTypeEnum.ROUTE_BANNER, CampaignTypeEnum.PROMOCODE]
)
async def test_raises_for_unsupported_campaign_type_and_platform_navi(
    domain, campaign_creation_kwargs, campaign_type
):
    kwargs = campaign_creation_kwargs(
        platforms=[PlatformEnum.NAVI], campaign_type=campaign_type
    )

    with pytest.raises(WrongCampaignTypeForPlatform):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "creative_types",
    all_creative_type_combinations - {("pin", "logo_and_text"), ("pin", "banner")},
)
async def test_raises_for_invalid_creative_types_for_pin_on_route_navi(
    domain, campaign_creation_kwargs, creative_types
):
    kwargs = campaign_creation_kwargs(
        campaign_type=CampaignTypeEnum.PIN_ON_ROUTE,
        platforms=[PlatformEnum.NAVI],
        creatives=list(
            {"type_": creative_type, "field": "value"}
            for creative_type in creative_types
        ),
    )

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "creative_types",
    all_creative_type_combinations
    - {("pin", "logo_and_text", "banner"), ("pin", "banner")},
)
async def test_raises_for_invalid_creative_types_for_pin_on_route_maps(
    domain, campaign_creation_kwargs, creative_types
):
    kwargs = campaign_creation_kwargs(
        campaign_type=CampaignTypeEnum.PIN_ON_ROUTE,
        platforms=[PlatformEnum.MAPS],
        creatives=list(
            {"type_": creative_type, "field": "value"}
            for creative_type in creative_types
        ),
    )

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "creative_types", all_creative_type_combinations - {("billboard", "banner")}
)
async def test_raises_for_invalid_creative_types_for_billboard(
    domain, campaign_creation_kwargs, creative_types
):
    kwargs = campaign_creation_kwargs(
        campaign_type=CampaignTypeEnum.BILLBOARD,
        platforms=[PlatformEnum.NAVI],
        creatives=list(
            {"type_": creative_type, "field": "value"}
            for creative_type in creative_types
        ),
    )

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "creative_types",
    all_creative_type_combinations - {("banner",), ("banner", "audio_banner")},
)
async def test_raises_for_invalid_creative_types_for_zero_speed_banner(
    domain, campaign_creation_kwargs, creative_types
):
    kwargs = campaign_creation_kwargs(
        campaign_type=CampaignTypeEnum.ZERO_SPEED_BANNER,
        platforms=[PlatformEnum.NAVI],
        creatives=list(
            {"type_": creative_type, "field": "value"}
            for creative_type in creative_types
        ),
    )

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "creative_types", all_creative_type_combinations - {("banner",)}
)
async def test_raises_for_invalid_creative_types_for_overview_banner(
    domain, campaign_creation_kwargs, creative_types
):
    kwargs = campaign_creation_kwargs(
        campaign_type=CampaignTypeEnum.OVERVIEW_BANNER,
        platforms=[PlatformEnum.NAVI],
        creatives=list(
            {"type_": creative_type, "field": "value"}
            for creative_type in creative_types
        ),
    )

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize("platforms", [[PlatformEnum.NAVI], [PlatformEnum.MAPS]])
@pytest.mark.parametrize(
    "creative_types", all_creative_type_combinations - {("icon", "pin_search", "text")}
)
async def test_raises_for_invalid_creative_types_for_category_icon(
    domain, campaign_creation_kwargs, platforms, creative_types
):
    kwargs = campaign_creation_kwargs(
        campaign_type=CampaignTypeEnum.CATEGORY_SEARCH,
        platforms=platforms,
        creatives=list(
            {"type_": creative_type, "field": "value"}
            for creative_type in creative_types
        ),
    )

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "creative_types", all_creative_type_combinations - {("banner",)}
)
async def test_raises_for_invalid_creative_types_for_route_banner(
    domain, campaign_creation_kwargs, creative_types
):
    kwargs = campaign_creation_kwargs(
        campaign_type=CampaignTypeEnum.ROUTE_BANNER,
        platforms=[PlatformEnum.METRO],
        creatives=list(
            {"type_": creative_type, "field": "value"}
            for creative_type in creative_types
        ),
    )

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "creative_types", [all_creative_type_combinations - {("via_point",)}]
)
async def test_raises_for_invalid_creative_types_for_via_points(
    domain, campaign_creation_kwargs, creative_types
):
    kwargs = campaign_creation_kwargs(
        campaign_type=CampaignTypeEnum.VIA_POINTS,
        platforms=[PlatformEnum.NAVI],
        creatives=list(
            {"type_": creative_type, "field": "value"}
            for creative_type in creative_types
        ),
    )

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "creative_types", [all_creative_type_combinations - {("banner",)}]
)
async def test_raises_for_invalid_creative_types_for_promocode(
    domain, campaign_creation_kwargs, creative_types
):
    kwargs = campaign_creation_kwargs(
        campaign_type=CampaignTypeEnum.PROMOCODE,
        platforms=[PlatformEnum.METRO],
        creatives=list(
            {"type_": creative_type, "field": "value"}
            for creative_type in creative_types
        ),
    )

    with pytest.raises(WrongCreativesForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    ("campaign_type", "placing"),
    [
        (CampaignTypeEnum.PIN_ON_ROUTE, {"area": {"areas": [], "version": 1}}),
        (CampaignTypeEnum.CATEGORY_SEARCH, {"area": {"areas": [], "version": 1}}),
        (CampaignTypeEnum.VIA_POINTS, {"area": {"areas": [], "version": 1}}),
        (CampaignTypeEnum.BILLBOARD, {"organizations": {"permalinks": [123, 456]}}),
        (
            CampaignTypeEnum.ZERO_SPEED_BANNER,
            {"organizations": {"permalinks": [123, 456]}},
        ),
        (
            CampaignTypeEnum.OVERVIEW_BANNER,
            {"organizations": {"permalinks": [123, 456]}},
        ),
        (CampaignTypeEnum.ROUTE_BANNER, {"organizations": {"permalinks": [123, 456]}}),
        (CampaignTypeEnum.PROMOCODE, {"organizations": {"permalinks": [123, 456]}}),
    ],
)
async def test_raises_for_invalid_placing(
    domain, campaign_creation_kwargs, campaign_type, placing
):
    kwargs = campaign_creation_kwargs(campaign_type=campaign_type, placing=placing)

    with pytest.raises(WrongPlacingForCampaignType):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "discounts",
    [
        [
            {
                "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                "end_datetime": datetime(2019, 3, 3, tzinfo=timezone.utc),
                "cost_multiplier": Decimal("0.75"),
            },
            {
                "start_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                "end_datetime": datetime(2019, 4, 4, tzinfo=timezone.utc),
                "cost_multiplier": Decimal("1.35"),
            },
        ],
        [
            {
                "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                "end_datetime": datetime(2019, 4, 4, tzinfo=timezone.utc),
                "cost_multiplier": Decimal("0.75"),
            },
            {
                "start_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                "end_datetime": datetime(2019, 3, 3, tzinfo=timezone.utc),
                "cost_multiplier": Decimal("1.35"),
            },
        ],
    ],
)
async def test_raises_for_date_intersecting_discounts(
    domain, campaign_creation_kwargs, discounts
):
    kwargs = campaign_creation_kwargs(discounts=discounts)

    with pytest.raises(PeriodIntersectingDiscounts):
        await domain.create_campaign(**kwargs)


@pytest.mark.parametrize(
    "billing",
    [
        {
            "cpm": {
                "cost": Decimal("100.00"),
                "budget": Decimal("1000.0"),
                "daily_budget": None,
                "auto_daily_budget": True,
            }
        },
        {
            "cpa": {
                "cost": Decimal("100.00"),
                "budget": Decimal("1000.0"),
                "daily_budget": None,
                "auto_daily_budget": True,
            }
        },
    ],
)
async def test_raises_for_auto_daily_zero_buget(
    domain, campaign_creation_kwargs, billing, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(100.0)

    kwargs = campaign_creation_kwargs(**billing)

    with pytest.raises(InvalidBilling):
        await domain.create_campaign(**kwargs)


async def test_raises_for_invalid_custom_page_id(
    domain, campaign_creation_kwargs, billing_proxy_client
):
    kwargs = campaign_creation_kwargs(settings={"custom_page_id": "BAD_PAGE_ID"})

    with pytest.raises(InvalidCustomPageId):
        await domain.create_campaign(**kwargs)

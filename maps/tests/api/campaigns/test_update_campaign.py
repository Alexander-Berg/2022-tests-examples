import json
from datetime import datetime, timezone
from decimal import Decimal
from operator import itemgetter

import pytest
from google.protobuf.message import Message

from maps_adv.adv_store.api.proto.action_pb2 import (
    Action,
    ActionType,
    AddPointToRoute,
    DownloadApp,
    OpenSite,
    PhoneCall,
    ResolveUri,
    Search,
)
from maps_adv.adv_store.api.proto.billing_pb2 import Billing, Cpa, Cpm, Fix, Money
from maps_adv.adv_store.api.proto.campaign_pb2 import (
    Campaign,
    CampaignData,
    CampaignSettings,
    OrderSize,
    Platform,
    PublicationEnv,
    Rubric,
    WeekScheduleItem,
)
from maps_adv.adv_store.api.proto.campaign_status_pb2 import CampaignStatus
from maps_adv.adv_store.api.proto.creative_pb2 import (
    Banner,
    Billboard,
    Creative,
    Icon,
    Image,
    LogoAndText,
    Pin,
    PinSearch,
    ViaPoint,
)
from maps_adv.adv_store.api.proto.discount_pb2 import Discount
from maps_adv.adv_store.api.proto.error_pb2 import Error
from maps_adv.adv_store.api.proto.placing_pb2 import (
    Area,
    Organizations,
    Placing,
    Point,
    Polygon,
)
from maps_adv.adv_store.api.schemas.enums import (
    ActionTypeEnum,
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    OrderSizeEnum,
    PlatformEnum,
    PublicationEnvEnum,
    ResolveUriTargetEnum,
    RubricEnum,
)
from maps_adv.adv_store.v2.tests import Any, dt, dt_to_proto
from maps_adv.common.helpers.enums import CampaignTypeEnum
from maps_adv.common.proto.campaign_pb2 import (
    CampaignType,
)

pytestmark = [pytest.mark.asyncio]
url = "/campaigns/{}/"
_type_getter = itemgetter("type_")


_enums_map = {
    PublicationEnvEnum.DATA_TESTING: PublicationEnv.DATA_TESTING,
    PublicationEnvEnum.PRODUCTION: PublicationEnv.PRODUCTION,
    PlatformEnum.METRO: Platform.METRO,
    PlatformEnum.NAVI: Platform.NAVI,
    PlatformEnum.MAPS: Platform.MAPS,
    CampaignTypeEnum.PIN_ON_ROUTE: CampaignType.PIN_ON_ROUTE,
    CampaignTypeEnum.BILLBOARD: CampaignType.BILLBOARD,
    CampaignTypeEnum.ZERO_SPEED_BANNER: CampaignType.ZERO_SPEED_BANNER,
    CampaignTypeEnum.OVERVIEW_BANNER: CampaignType.OVERVIEW_BANNER,
    CampaignTypeEnum.CATEGORY_SEARCH: CampaignType.CATEGORY_SEARCH,
    CampaignTypeEnum.VIA_POINTS: CampaignType.VIA_POINTS,
    CampaignTypeEnum.ROUTE_BANNER: CampaignType.ROUTE_BANNER,
    CampaignTypeEnum.PROMOCODE: CampaignType.PROMOCODE,
    RubricEnum.COMMON: Rubric.COMMON,
    RubricEnum.AUTO: Rubric.AUTO,
    RubricEnum.REALTY: Rubric.REALTY,
    OrderSizeEnum.SMALL: OrderSize.SMALL,
    OrderSizeEnum.BIG: OrderSize.BIG,
    OrderSizeEnum.VERY_BIG: OrderSize.VERY_BIG,
    CampaignStatusEnum.DRAFT: CampaignStatus.DRAFT,
    CampaignStatusEnum.REVIEW: CampaignStatus.REVIEW,
    CampaignStatusEnum.REJECTED: CampaignStatus.REJECTED,
    CampaignStatusEnum.ACTIVE: CampaignStatus.ACTIVE,
    CampaignStatusEnum.PAUSED: CampaignStatus.PAUSED,
    CampaignStatusEnum.DONE: CampaignStatus.DONE,
    CampaignStatusEnum.ARCHIVED: CampaignStatus.ARCHIVED,
    FixTimeIntervalEnum.DAILY: Fix.TimeIntervalEnum.DAILY,
    FixTimeIntervalEnum.WEEKLY: Fix.TimeIntervalEnum.WEEKLY,
    FixTimeIntervalEnum.MONTHLY: Fix.TimeIntervalEnum.MONTHLY,
    ActionTypeEnum.OPEN_SITE: ActionType.OPEN_SITE,
    ResolveUriTargetEnum.WEB_VIEW: ResolveUri.Target.WEB_VIEW,
    ResolveUriTargetEnum.BROWSER: ResolveUri.Target.BROWSER,
}

_creatives_pb_types_map = {
    "pin": Pin,
    "billboard": Billboard,
    "icon": Icon,
    "pin_search": PinSearch,
    "logo_and_text": LogoAndText,
    "via_point": ViaPoint,
    "banner": Banner,
    "image": Image,
}

_actions_pb_type_map = {
    "download_app": DownloadApp,
    "open_site": OpenSite,
    "phone_call": PhoneCall,
    "search": Search,
    "resolve_uri": ResolveUri,
    "add_point_to_route": AddPointToRoute,
}


@pytest.fixture
def campaign_creation_params():
    def func(**additional):
        data = {
            "campaign_type": CampaignTypeEnum.PIN_ON_ROUTE,
            "creatives": [
                {
                    "type_": "pin",
                    "title": "Заголовок 2",
                    "subtitle": "Подзаголовок 2",
                    "images": [
                        {
                            "type": "img1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                            "alias_template": "alias_template1",
                            "metadata": "{}",
                        },
                        {
                            "type": "img1",
                            "group_id": "group_id2",
                            "image_name": "image_name2",
                            "alias_template": "alias_template2",
                            "metadata": "{}",
                        },
                    ],
                },
                {
                    "type_": "banner",
                    "disclaimer": "Дисклеймер",
                    "show_ads_label": True,
                    "description": "Описание",
                    "title": "Заголовок",
                    "images": [
                        {
                            "type": "img1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                            "alias_template": "alias_template1",
                            "metadata": "{}",
                        }
                    ],
                },
            ],
        }

        data.update(additional)
        return data

    return func


@pytest.fixture
def campaign_update_pb(con, factory):
    async def func(campaign, **overrides):
        pb = CampaignData(
            name=campaign["name"],
            author_id=campaign["author_id"],
            campaign_type=_enums_map[CampaignTypeEnum[campaign["campaign_type"]]],
            start_datetime=dt_to_proto(campaign["start_datetime"]),
            end_datetime=dt_to_proto(campaign["end_datetime"]),
            timezone=campaign["timezone"],
            targeting=json.dumps(campaign["targeting"]),
            user_daily_display_limit=campaign["user_daily_display_limit"],
            user_display_limit=campaign["user_display_limit"],
            comment=campaign["comment"],
        )

        pb.publication_envs.extend(
            _enums_map[PublicationEnvEnum[pe]] for pe in campaign["publication_envs"]
        )
        pb.platforms.extend(_enums_map[PlatformEnum[p]] for p in campaign["platforms"])
        if campaign["rubric"]:
            pb.rubric = _enums_map[RubricEnum[campaign["rubric"]]]
        if campaign["order_size"]:
            pb.order_size = _enums_map[OrderSizeEnum[campaign["order_size"]]]

        if campaign["order_id"] is not None:
            pb.order_id = campaign["order_id"]
        if campaign["manul_order_id"] is not None:
            pb.manul_order_id = campaign["manul_order_id"]

        pb.product_id = campaign.get("product_id", 1)

        if campaign["datatesting_expires_at"] is not None:
            pb.datatesting_expires_at.CopyFrom(
                dt_to_proto(campaign["datatesting_expires_at"])
            )

        creatives = []
        for creative in await factory.list_campaign_creatives(campaign["id"]):
            cr_type = creative.pop("type_")
            inner_creative_pb = _creatives_pb_types_map[cr_type](**creative)
            creatives.append(Creative(**{cr_type: inner_creative_pb}))
        pb.creatives.extend(creatives)

        actions = []
        for action in await factory.list_campaign_actions(campaign["id"]):
            if action["type_"] == "resolve_uri":
                action["action_type"] = _enums_map[action["action_type"]]
                action["target"] = _enums_map[action["target"]]
            action_type, action_title, action_main = (
                action.pop("type_"),
                (action.pop("title") if action.get("title") else None),
                action.pop("main"),
            )
            inner_action_pb = _actions_pb_type_map[action_type](**action)
            actions.append(
                Action(
                    title=action_title,
                    **{action_type: inner_action_pb},
                    main=action_main,
                )
            )
        pb.actions.extend(actions)

        pb.week_schedule.extend(
            WeekScheduleItem(**ws)
            for ws in await factory.list_campaign_week_schedule(campaign["id"])
        )

        placing = await factory.find_campaign_placing(campaign["id"])
        placing_type, placing_data = list(placing.items())[0]
        if placing_type == "organizations":
            pb.placing.organizations.CopyFrom(
                Organizations(permalinks=placing_data["permalinks"])
            )
        elif placing_type == "area":
            pb.placing.area.CopyFrom(
                Area(
                    version=placing_data["version"],
                    areas=list(
                        Polygon(
                            name=pol["name"],
                            points=list(
                                Point(lat=p["lat"], lon=p["lon"]) for p in pol["points"]
                            ),
                        )
                        for pol in placing_data["areas"]
                    ),
                )
            )

        pb.status = _enums_map[
            CampaignStatusEnum[
                await con.fetchval(
                    """
                    SELECT status
                    FROM status_history
                    WHERE campaign_id = $1
                    ORDER BY changed_datetime DESC
                    LIMIT 1
                """,
                    campaign["id"],
                )
            ]
        ]

        pb.discounts.extend(
            list(
                Discount(
                    start_datetime=dt_to_proto(discount["start_datetime"]),
                    end_datetime=dt_to_proto(discount["end_datetime"]),
                    cost_multiplier=str(discount["cost_multiplier"]),
                )
                for discount in await factory.list_campaign_discounts(campaign["id"])
            )
        )

        campaign_billing = await factory.find_campaign_billing(campaign["id"])
        billing_type, billing_data = list(campaign_billing.items())[0]
        if billing_type == "cpm":
            pb.billing.CopyFrom(
                Billing(
                    cpm=Cpm(
                        cost=Money(value=int(billing_data["cost"] * 10000)),
                        budget=Money(value=int(billing_data["budget"] * 10000)),
                        daily_budget=Money(
                            value=int(billing_data["daily_budget"] * 10000)
                        ),
                        auto_daily_budget=billing_data["auto_daily_budget"],
                    )
                )
            )
        elif billing_type == "cpa":
            pb.billing.CopyFrom(
                Billing(
                    cpa=Cpa(
                        cost=Money(value=int(billing_data["cost"] * 10000)),
                        budget=Money(value=int(billing_data["budget"] * 10000)),
                        daily_budget=Money(
                            value=int(billing_data["daily_budget"] * 10000)
                        ),
                        auto_daily_budget=billing_data["auto_daily_budget"],
                    )
                )
            )
        elif billing_type == "fix":
            pb.billing.CopyFrom(
                Billing(
                    fix=Fix(
                        cost=Money(value=int(billing_data["cost"] * 10000)),
                        time_interval=_enums_map[billing_data["time_interval"]],
                    )
                )
            )

        for key, value in overrides.items():

            if isinstance(value, list):
                field = getattr(pb, key)
                for _ in range(len(field)):
                    field.pop()
                field.extend(value)
            elif value is None:
                pb.ClearField(key)
            elif isinstance(value, Message):
                getattr(pb, key).CopyFrom(value)
            else:
                setattr(pb, key, value)

        return pb

    return func


@pytest.mark.parametrize(
    ("field", "old_value", "new_pb_value", "expected_new_value"),
    [
        ("name", "Old name", "New name", "New name"),
        (
            "publication_envs",
            [PublicationEnvEnum.DATA_TESTING],
            [PublicationEnv.PRODUCTION],
            ["PRODUCTION"],
        ),
        (
            "start_datetime",
            datetime(2018, 1, 1, tzinfo=timezone.utc),
            dt_to_proto(datetime(2018, 2, 2, tzinfo=timezone.utc)),
            datetime(2018, 2, 2, tzinfo=timezone.utc),
        ),
        (
            "end_datetime",
            datetime(2020, 3, 3, tzinfo=timezone.utc),
            dt_to_proto(datetime(2020, 4, 4, tzinfo=timezone.utc)),
            datetime(2020, 4, 4, tzinfo=timezone.utc),
        ),
        ("timezone", "Europe/Moscow", "Asia/Novosibirsk", "Asia/Novosibirsk"),
        (
            "platforms",
            [PlatformEnum.NAVI],
            [Platform.NAVI, Platform.MAPS],
            ["NAVI", "MAPS"],
        ),
        ("rubric", RubricEnum.COMMON, Rubric.REALTY, "REALTY"),
        ("order_size", OrderSizeEnum.SMALL, OrderSize.VERY_BIG, "VERY_BIG"),
        (
            "targeting",
            {"tag": "gender", "content": "male"},
            json.dumps({"tag": "gender", "content": "female"}),
            {"tag": "gender", "content": "female"},
        ),
        ("user_daily_display_limit", 2, 3, 3),
        ("user_display_limit", 3, 4, 4),
        ("comment", "Old comment", "New comment", "New comment"),
        (
            "datatesting_expires_at",
            datetime(2019, 4, 1, tzinfo=timezone.utc),
            dt_to_proto(datetime(2019, 4, 2, tzinfo=timezone.utc)),
            datetime(2019, 4, 2, tzinfo=timezone.utc),
        ),
        (
            "settings",
            {},
            CampaignSettings(custom_page_id="abc"),
            {"custom_page_id": "abc", "verification_data": []},
        ),
    ],
)
async def test_updates_simple_fields(
    api,
    factory,
    con,
    campaign_creation_params,
    campaign_update_pb,
    field,
    old_value,
    new_pb_value,
    expected_new_value,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(**{field: old_value})
    )

    update_pb = await campaign_update_pb(campaign, **{field: new_pb_value})
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    actual_new_value = await con.fetchval(
        f"""
            SELECT {field}
            FROM campaign
            WHERE id = $1
        """,
        campaign["id"],
    )

    assert actual_new_value == expected_new_value


@pytest.mark.parametrize(("field", "old_value", "new_value"), [("author_id", 123, 321)])
async def test_not_updates_some_fields(
    api,
    factory,
    con,
    campaign_creation_params,
    campaign_update_pb,
    field,
    old_value,
    new_value,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(**{field: old_value})
    )

    update_pb = await campaign_update_pb(campaign, **{field: new_value})
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    actual_new_value = await con.fetchval(
        f"""
            SELECT {field}
            FROM campaign
            WHERE id = $1
        """,
        campaign["id"],
    )

    assert actual_new_value == old_value


async def test_returns_campaign_details(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(order_id=15, **campaign_creation_params())

    update_pb = await campaign_update_pb(
        campaign,
        name="New name",
        author_id=321,
        publication_envs=[PublicationEnv.PRODUCTION],
        start_datetime=dt("2019-03-03 00:00:00", as_proto=True),
        end_datetime=dt("2019-04-04 00:00:00", as_proto=True),
        timezone="Europe/Moscow",
        platforms=[Platform.MAPS],
        billing=Billing(
            cpm=Cpm(
                cost=Money(value=200000),
                budget=Money(value=300000000),
                daily_budget=Money(value=100000000),
                auto_daily_budget=False,
            )
        ),
        comment="New comment",
        actions=[Action(title="Перейти", open_site=OpenSite(url="ya.ru"), main=False)],
        creatives=[
            Creative(
                pin=Pin(
                    title="Заголовок 2",
                    subtitle="Подзаголовок 2",
                    images=[
                        Image(
                            type="img1_new",
                            group_id="group_id1_new",
                            image_name="image_name1_new",
                            alias_template="alias_template1_new",
                            metadata="[]",
                        ),
                        Image(
                            type="img2_new",
                            group_id="group_id2_new",
                            image_name="image_name2_new",
                            alias_template="alias_template2_new",
                            metadata="[]",
                        ),
                    ],
                )
            ),
            Creative(
                banner=Banner(
                    disclaimer="Дисклеймер ещё",
                    show_ads_label=False,
                    description="Описание ещё",
                    title="Заголовок",
                    terms="Условия",
                    images=[
                        Image(
                            type="img1_new",
                            group_id="group_id1_new",
                            image_name="image_name1_new",
                            alias_template="alias_template1_new",
                            metadata="[]",
                        )
                    ],
                )
            ),
        ],
        placing=Placing(organizations=Organizations(permalinks=[1000, 3000])),
        rubric=Rubric.REALTY,
        order_size=OrderSize.BIG,
        status=CampaignStatus.REVIEW,
        targeting=json.dumps({"tag": "gender", "content": "male"}),
        user_daily_display_limit=5,
        user_display_limit=6,
        week_schedule=[WeekScheduleItem(start=60 * 24 * 3, end=60 * 24 * 4)],
        order_id=15,
    )
    got = await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    expected_pb = Campaign(id=campaign["id"])
    expected_pb.data.CopyFrom(update_pb)
    expected_pb.data.author_id = 123
    expected_pb.data.created_datetime.CopyFrom(got.data.created_datetime)
    expected_pb.data.settings.CopyFrom(CampaignSettings())
    expected_pb.data.ClearField("product_id")
    assert got == expected_pb


@pytest.mark.parametrize(
    ("old_billing_data", "new_billing_data_pb", "expected_new_billing_data"),
    [
        (
            {
                "cpm": {
                    "cost": Decimal("10.0"),
                    "budget": Decimal("5000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            Billing(
                cpm=Cpm(
                    cost=Money(value=200000),
                    budget=Money(value=60000000),
                    daily_budget=Money(value=20000000),
                    auto_daily_budget=False,
                )
            ),
            {
                "cpm": {
                    "cost": Decimal("20.0"),
                    "budget": Decimal("6000"),
                    "daily_budget": Decimal("2000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10.0"),
                    "budget": Decimal("5000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            Billing(
                cpa=Cpa(
                    cost=Money(value=200000),
                    budget=Money(value=60000000),
                    daily_budget=Money(value=20000000),
                    auto_daily_budget=False,
                )
            ),
            {
                "cpa": {
                    "cost": Decimal("20.0"),
                    "budget": Decimal("6000"),
                    "daily_budget": Decimal("2000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "fix": {
                    "cost": Decimal("10.0"),
                    "time_interval": FixTimeIntervalEnum.DAILY,
                }
            },
            Billing(
                fix=Fix(
                    cost=Money(value=200000), time_interval=Fix.TimeIntervalEnum.WEEKLY
                )
            ),
            {
                "fix": {
                    "cost": Decimal("20.0"),
                    "time_interval": FixTimeIntervalEnum.WEEKLY,
                }
            },
        ),
        (
            {
                "cpm": {
                    "cost": Decimal("10.0"),
                    "budget": Decimal("5000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            Billing(
                cpm=Cpm(
                    cost=Money(value=200000),
                    budget=Money(value=60000000),
                    daily_budget=Money(value=20000000),
                )
            ),
            {
                "cpm": {
                    "cost": Decimal("20.0"),
                    "budget": Decimal("6000"),
                    "daily_budget": Decimal("2000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10.0"),
                    "budget": Decimal("5000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            Billing(
                cpa=Cpa(
                    cost=Money(value=200000),
                    budget=Money(value=60000000),
                    daily_budget=Money(value=20000000),
                )
            ),
            {
                "cpa": {
                    "cost": Decimal("20.0"),
                    "budget": Decimal("6000"),
                    "daily_budget": Decimal("2000"),
                    "auto_daily_budget": False,
                }
            },
        ),
    ],
)
async def test_updates_billing(
    api,
    factory,
    campaign_creation_params,
    campaign_update_pb,
    old_billing_data,
    new_billing_data_pb,
    expected_new_billing_data,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(), **old_billing_data
    )

    update_pb = await campaign_update_pb(campaign, billing=new_billing_data_pb)
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    assert (
        await factory.find_campaign_billing(campaign["id"]) == expected_new_billing_data
    )


@pytest.mark.parametrize(
    ("new_status_pb", "expected_status"),
    [
        (CampaignStatus.DRAFT, CampaignStatusEnum.DRAFT),
        (CampaignStatus.REVIEW, CampaignStatusEnum.REVIEW),
        (CampaignStatus.REJECTED, CampaignStatusEnum.REJECTED),
        (CampaignStatus.ACTIVE, CampaignStatusEnum.ACTIVE),
        (CampaignStatus.PAUSED, CampaignStatusEnum.PAUSED),
        (CampaignStatus.DONE, CampaignStatusEnum.DONE),
        (CampaignStatus.ARCHIVED, CampaignStatusEnum.ARCHIVED),
    ],
)
@pytest.mark.real_db
async def test_adds_status_log_entry(
    api,
    factory,
    campaign_creation_params,
    campaign_update_pb,
    new_status_pb,
    expected_status,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(**campaign_creation_params())

    update_pb = await campaign_update_pb(campaign, author_id=321, status=new_status_pb)
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    status_entries = await factory.list_campaign_status_history(campaign["id"])
    assert status_entries == [
        {
            "campaign_id": campaign["id"],
            "author_id": 123,
            "status": CampaignStatusEnum.DRAFT.name,
            "metadata": {},
            "changed_datetime": Any(datetime),
        },
        {
            "campaign_id": campaign["id"],
            "author_id": 321,
            "status": expected_status.name,
            "metadata": {},
            "changed_datetime": Any(datetime),
        },
    ]


async def test_updates_creatives(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    creation_data = campaign_creation_params(
        creatives=[
            {
                "type_": "pin",
                "title": "Заголовок",
                "subtitle": "Подзаголовок",
                "images": [
                    {
                        "type": "img1",
                        "group_id": "group_id1",
                        "image_name": "image_name1",
                        "alias_template": "alias_template1",
                        "metadata": "{}",
                    },
                    {
                        "type": "img1",
                        "group_id": "group_id2",
                        "image_name": "image_name2",
                        "alias_template": "alias_template2",
                        "metadata": "{}",
                    },
                ],
            },
            {
                "type_": "banner",
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "terms": "Условия",
                "images": [
                    {
                        "type": "img1",
                        "group_id": "group_id1",
                        "image_name": "image_name1",
                        "alias_template": "alias_template1",
                        "metadata": "{}",
                    }
                ],
            },
        ]
    )
    campaign = await factory.create_campaign(**creation_data)

    update_pb = await campaign_update_pb(
        campaign,
        creatives=[
            Creative(
                pin=Pin(
                    title="Заголовок 2",
                    subtitle="Подзаголовок 2",
                    images=[
                        Image(
                            type="img1_new",
                            group_id="group_id1_new",
                            image_name="image_name1_new",
                            alias_template="alias_template1_new",
                            metadata=json.dumps([]),
                        ),
                        Image(
                            type="img2_new",
                            group_id="group_id2_new",
                            image_name="image_name2_new",
                            alias_template="alias_template2_new",
                            metadata=json.dumps([]),
                        ),
                    ],
                )
            ),
            Creative(
                banner=Banner(
                    disclaimer="Дисклеймер ещё",
                    show_ads_label=True,
                    description="Описание ещё",
                    title="Заголовок ещё",
                    terms="Условия",
                    images=[
                        Image(
                            type="img1_new",
                            group_id="group_id1_new",
                            image_name="image_name1_new",
                            alias_template="alias_template1_new",
                            metadata=json.dumps([]),
                        )
                    ],
                )
            ),
        ],
    )
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    expected_new_creatives = [
        {
            "type_": "pin",
            "title": "Заголовок 2",
            "subtitle": "Подзаголовок 2",
            "images": [
                {
                    "type": "img1_new",
                    "group_id": "group_id1_new",
                    "image_name": "image_name1_new",
                    "alias_template": "alias_template1_new",
                    "metadata": [],
                },
                {
                    "type": "img2_new",
                    "group_id": "group_id2_new",
                    "image_name": "image_name2_new",
                    "alias_template": "alias_template2_new",
                    "metadata": [],
                },
            ],
        },
        {
            "type_": "banner",
            "disclaimer": "Дисклеймер ещё",
            "show_ads_label": True,
            "description": "Описание ещё",
            "title": "Заголовок ещё",
            "terms": "Условия",
            "images": [
                {
                    "type": "img1_new",
                    "group_id": "group_id1_new",
                    "image_name": "image_name1_new",
                    "alias_template": "alias_template1_new",
                    "metadata": [],
                }
            ],
        },
    ]
    creatives = await factory.list_campaign_creatives(campaign["id"])
    assert sorted(creatives, key=_type_getter) == sorted(
        expected_new_creatives, key=_type_getter
    )


@pytest.mark.parametrize(
    ("old_actions", "new_actions_pb", "expected_new_actions"),
    [
        (
            [{"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}],
            [Action(title="Подойти", open_site=OpenSite(url="yandex.ru"), main=False)],
            [
                {
                    "type_": "open_site",
                    "title": "Подойти",
                    "url": "yandex.ru",
                    "main": False,
                }
            ],
        ),
        (
            [{"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}],
            [
                Action(
                    title="Поискать",
                    search=Search(organizations=[1, 2, 4], history_text="Поискал"),
                    main=False,
                )
            ],
            [
                {
                    "type_": "search",
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "main": False,
                }
            ],
        ),
        (
            [],
            [
                Action(title="Перейти", open_site=OpenSite(url="ya.ru"), main=False),
                Action(
                    title="Подойти", open_site=OpenSite(url="yandex.ru"), main=False
                ),
                Action(
                    title="Позвонить", phone_call=PhoneCall(phone="322-223"), main=False
                ),
                Action(
                    title="Поискать",
                    search=Search(organizations=[1, 2, 4], history_text="Поискал"),
                    main=False,
                ),
                Action(
                    title="Позвонить",
                    download_app=DownloadApp(
                        google_play_id="qwerty", app_store_id="ytrewq", url="yandex.ru"
                    ),
                    main=False,
                ),
                Action(
                    resolve_uri=ResolveUri(
                        uri="magic://ya.ru",
                        action_type=ActionType.OPEN_SITE,
                        target=ResolveUri.Target.WEB_VIEW,
                    ),
                    main=False,
                ),
                Action(
                    add_point_to_route=AddPointToRoute(latitude=1.2, longitude=3.4),
                    main=False,
                ),
            ],
            [
                {
                    "type_": "open_site",
                    "title": "Перейти",
                    "url": "ya.ru",
                    "main": False,
                },
                {
                    "type_": "open_site",
                    "title": "Подойти",
                    "url": "yandex.ru",
                    "main": False,
                },
                {
                    "type_": "phone_call",
                    "title": "Позвонить",
                    "phone": "322-223",
                    "main": False,
                },
                {
                    "type_": "search",
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
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
                {
                    "type_": "resolve_uri",
                    "uri": "magic://ya.ru",
                    "action_type": ActionTypeEnum.OPEN_SITE,
                    "target": ResolveUriTargetEnum.WEB_VIEW,
                    "dialog": None,
                    "main": False,
                },
                {
                    "type_": "add_point_to_route",
                    "latitude": 1.2,
                    "longitude": 3.4,
                    "main": False,
                },
            ],
        ),
        (
            [
                {
                    "type_": "open_site",
                    "title": "Перейти",
                    "url": "ya.ru",
                    "main": False,
                },
                {
                    "type_": "open_site",
                    "title": "Подойти",
                    "url": "yandex.ru",
                    "main": False,
                },
                {
                    "type_": "phone_call",
                    "title": "Позвонить",
                    "phone": "322-223",
                    "main": False,
                },
                {
                    "type_": "search",
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
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
                {
                    "type_": "resolve_uri",
                    "uri": "magic://ya.ru",
                    "action_type": ActionTypeEnum.OPEN_SITE,
                    "target": ResolveUriTargetEnum.WEB_VIEW,
                    "dialog": None,
                    "main": False,
                },
                {
                    "type_": "add_point_to_route",
                    "latitude": 1.2,
                    "longitude": 3.4,
                    "main": False,
                },
            ],
            [],
            [],
        ),
    ],
)
async def test_updates_actions(
    api,
    factory,
    campaign_creation_params,
    campaign_update_pb,
    old_actions,
    new_actions_pb,
    expected_new_actions,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(actions=old_actions)
    )

    update_pb = await campaign_update_pb(campaign, actions=new_actions_pb)
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    actions = await factory.list_campaign_actions(campaign["id"])
    assert sorted(actions, key=_type_getter) == sorted(
        expected_new_actions, key=_type_getter
    )


async def test_updates_placing_organizations(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(organizations={"permalinks": [213, 456]})
    )

    update_pb = await campaign_update_pb(
        campaign, placing=Placing(organizations=Organizations(permalinks=[777, 888]))
    )
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    expected_new_placing = {"organizations": {"permalinks": [777, 888]}}
    assert await factory.find_campaign_placing(campaign["id"]) == expected_new_placing


async def test_updates_placing_areas(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    creation_data = campaign_creation_params(
        campaign_type=CampaignTypeEnum.ZERO_SPEED_BANNER,
        creatives=[
            {
                "type_": "banner",
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "images": [
                    {
                        "type": "img1",
                        "group_id": "group_id1",
                        "image_name": "image_name1",
                        "alias_template": "alias_template1",
                        "metadata": "{}",
                    }
                ],
            }
        ],
        area={
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
    )
    campaign = await factory.create_campaign(**creation_data)

    update_pb = await campaign_update_pb(
        campaign,
        placing=Placing(
            area=Area(
                version=2,
                areas=[
                    Polygon(
                        name="Танковый полигон 2",
                        points=[
                            Point(lon=112.345, lat=123.567),
                            Point(lon=114.345, lat=123.567),
                            Point(lon=114.345, lat=125.567),
                            Point(lon=112.345, lat=123.567),
                        ],
                    ),
                    Polygon(
                        name="Ядерный полигон 2",
                        points=[
                            Point(lon=122.345, lat=133.567),
                            Point(lon=124.345, lat=133.567),
                            Point(lon=124.345, lat=135.567),
                            Point(lon=122.345, lat=133.567),
                        ],
                    ),
                ],
            )
        ),
    )
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    expected_new_placing = {
        "areas": [
            {
                "points": [
                    {"lon": 112.345, "lat": 123.567},
                    {"lon": 114.345, "lat": 123.567},
                    {"lon": 114.345, "lat": 125.567},
                    {"lon": 112.345, "lat": 123.567},
                ],
                "name": "Танковый полигон 2",
                "preset_id": None,
            },
            {
                "points": [
                    {"lon": 122.345, "lat": 133.567},
                    {"lon": 124.345, "lat": 133.567},
                    {"lon": 124.345, "lat": 135.567},
                    {"lon": 122.345, "lat": 133.567},
                ],
                "name": "Ядерный полигон 2",
                "preset_id": None,
            },
        ],
        "version": 2,
    }
    assert await factory.find_campaign_placing(campaign["id"]) == {
        "area": expected_new_placing
    }


@pytest.mark.parametrize(
    ("old_week_schedule", "new_week_schedule_pb", "expected_new_week_schedule"),
    [
        (
            [{"start": 60 * 24 * 3, "end": 60 * 24 * 4}],
            [WeekScheduleItem(start=60 * 24 * 5, end=60 * 24 * 6)],
            [{"start": 60 * 24 * 5, "end": 60 * 24 * 6}],
        ),
        (
            [{"start": 60 * 24 * 3, "end": 60 * 24 * 4}],
            [
                WeekScheduleItem(start=60 * 24 * 3, end=60 * 24 * 4),
                WeekScheduleItem(start=60 * 24 * 5, end=60 * 24 * 6),
            ],
            [
                {"start": 60 * 24 * 3, "end": 60 * 24 * 4},
                {"start": 60 * 24 * 5, "end": 60 * 24 * 6},
            ],
        ),
        (
            [],
            [
                WeekScheduleItem(start=60 * 24 * 3, end=60 * 24 * 4),
                WeekScheduleItem(start=60 * 24 * 5, end=60 * 24 * 6),
            ],
            [
                {"start": 60 * 24 * 3, "end": 60 * 24 * 4},
                {"start": 60 * 24 * 5, "end": 60 * 24 * 6},
            ],
        ),
        (
            [
                {"start": 60 * 24 * 3, "end": 60 * 24 * 4},
                {"start": 60 * 24 * 5, "end": 60 * 24 * 6},
            ],
            [],
            [],
        ),
    ],
)
async def test_updates_week_schedules(
    api,
    factory,
    campaign_creation_params,
    campaign_update_pb,
    old_week_schedule,
    new_week_schedule_pb,
    expected_new_week_schedule,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(week_schedule=old_week_schedule)
    )

    update_pb = await campaign_update_pb(campaign, week_schedule=new_week_schedule_pb)
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    assert (
        await factory.list_campaign_week_schedule(campaign["id"])
        == expected_new_week_schedule
    )


@pytest.mark.parametrize(
    ("old_discounts", "new_discounts_pb"),
    [
        (
            [],
            [
                Discount(
                    start_datetime=dt_to_proto(
                        datetime(2019, 1, 1, tzinfo=timezone.utc)
                    ),
                    end_datetime=dt_to_proto(datetime(2019, 2, 2, tzinfo=timezone.utc)),
                    cost_multiplier="0.75",
                )
            ],
        ),
        (
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
            [],
        ),
        (
            [
                {
                    "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("0.75"),
                }
            ],
            [
                Discount(
                    start_datetime=dt_to_proto(
                        datetime(2019, 3, 3, tzinfo=timezone.utc)
                    ),
                    end_datetime=dt_to_proto(datetime(2019, 4, 4, tzinfo=timezone.utc)),
                    cost_multiplier="0.75",
                )
            ],
        ),
    ],
)
async def test_not_updates_discounts(
    api,
    factory,
    campaign_creation_params,
    campaign_update_pb,
    old_discounts,
    new_discounts_pb,
    billing_proxy_client,
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(discounts=old_discounts)
    )

    update_pb = await campaign_update_pb(campaign, discounts=new_discounts_pb)
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )

    assert await factory.list_campaign_discounts(campaign["id"]) == old_discounts


async def test_returns_404_for_nonexistent_campaign(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(**campaign_creation_params())

    update_pb = await campaign_update_pb(campaign)
    await api.put(
        url.format(campaign["id"] + 1),
        data=update_pb.SerializeToString(),
        expected_status=404,
    )


async def test_supports_multiple_platforms(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(**campaign_creation_params())

    update_pb = await campaign_update_pb(
        campaign, platforms=[Platform.NAVI, Platform.MAPS]
    )
    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Error,
        expected_status=200,
    )


async def test_returns_error_for_unsupported_campaign_type_and_platform_navi(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(campaign_type=CampaignTypeEnum.PIN_ON_ROUTE)
    )

    update_pb = await campaign_update_pb(campaign, platforms=[Platform.METRO])
    error = await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Error,
        expected_status=400,
    )

    assert error.code == Error.INVALID_CAMPAIGN_TYPE_FOR_PLATFORM


async def test_returns_error_for_invalid_creative_types(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(campaign_type=CampaignTypeEnum.PIN_ON_ROUTE)
    )

    update_pb = await campaign_update_pb(
        campaign,
        creatives=[
            Creative(
                billboard=Billboard(
                    images=[
                        Image(
                            type="img1_new",
                            group_id="group_id1_new",
                            image_name="image_name1_new",
                            alias_template="alias_template1_new",
                            metadata="[]",
                        )
                    ]
                )
            )
        ],
    )
    error = await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Error,
        expected_status=400,
    )

    assert error.code == Error.WRONG_SET_OF_CREATIVES_FOR_THE_CAMPAIGN_TYPE


async def test_returns_error_for_invalid_placing(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(campaign_type=CampaignTypeEnum.PIN_ON_ROUTE)
    )

    update_pb = await campaign_update_pb(
        campaign,
        placing=Placing(
            area=Area(
                version=2,
                areas=[
                    Polygon(
                        name="Танковый полигон 2",
                        points=[
                            Point(lon=112.345, lat=123.567),
                            Point(lon=114.345, lat=123.567),
                            Point(lon=114.345, lat=125.567),
                            Point(lon=112.345, lat=123.567),
                        ],
                    )
                ],
            )
        ),
    )
    error = await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Error,
        expected_status=400,
    )

    assert error.code == Error.INVALID_PLACING_FOR_CAMPAIGN_TYPE


async def test_returns_error_if_billing_type_changed(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(
        **campaign_creation_params(
            cpm={
                "cost": Decimal("10.0"),
                "budget": Decimal("3000.0"),
                "daily_budget": Decimal("1000.0"),
                "auto_daily_budget": False,
            }
        )
    )

    update_pb = await campaign_update_pb(
        campaign,
        billing=Billing(
            cpa=Cpa(
                cost=Money(value=100000),
                budget=Money(value=30000000),
                daily_budget=Money(value=10000000),
                auto_daily_budget=False,
            )
        ),
    )
    error = await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Error,
        expected_status=400,
    )

    assert error.code == Error.BILLING_TYPED_CHANGED


async def test_returns_error_if_order_id_changed(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(**campaign_creation_params(order_id=15))

    update_pb = await campaign_update_pb(campaign, order_id=20)
    error = await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Error,
        expected_status=400,
    )

    assert error.code == Error.ORDER_ID_CHANGED


async def test_ignores_optional_fields(
    api, factory, campaign_creation_params, campaign_update_pb, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(20.0)
    campaign = await factory.create_campaign(**campaign_creation_params())
    update_pb = await campaign_update_pb(campaign)
    update_pb.created_datetime.CopyFrom(
        dt_to_proto(datetime(2018, 1, 1, tzinfo=timezone.utc))
    )

    await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Campaign,
        expected_status=200,
    )


async def test_returns_error_for_invalid_custom_page_id(
    api, factory, campaign_creation_params, campaign_update_pb
):

    campaign = await factory.create_campaign(
        **campaign_creation_params(campaign_type=CampaignTypeEnum.PIN_ON_ROUTE)
    )
    update_pb = await campaign_update_pb(
        campaign, settings=CampaignSettings(custom_page_id="BAD_PAGE_ID")
    )

    got = await api.put(
        url.format(campaign["id"]),
        data=update_pb.SerializeToString(),
        decode_as=Error,
        expected_status=400,
    )

    assert got.code == Error.INVALID_CUSTOM_PAGE_ID

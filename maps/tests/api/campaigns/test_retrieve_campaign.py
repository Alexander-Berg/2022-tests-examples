import json
from datetime import datetime, timezone
from decimal import Decimal

import pytest
from google.protobuf.timestamp_pb2 import Timestamp

from maps_adv.adv_store.api.proto.action_pb2 import (
    Action,
    ActionType,
    AddPointToRoute,
    DownloadApp,
    OpenSite,
    PhoneCall,
    Promocode,
    ResolveUri,
    Search,
)
from maps_adv.adv_store.api.proto.billing_pb2 import Billing, Cpa, Cpm, Fix, Money
from maps_adv.adv_store.api.proto.campaign_pb2 import (
    Campaign,
    CampaignData,
    CampaignSettings,
    OrderSize,
    OverviewPosition,
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
    Text,
    ViaPoint,
)
from maps_adv.adv_store.api.proto.discount_pb2 import Discount
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
    OverviewPositionEnum,
    PlatformEnum,
    PublicationEnvEnum,
    ResolveUriTargetEnum,
    RubricEnum,
)
from maps_adv.adv_store.v2.tests import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum
from maps_adv.common.proto.campaign_pb2 import CampaignType

pytestmark = [pytest.mark.asyncio]

url = "/campaigns/{}/"


async def test_return_data(api, factory):
    campaign = await factory.create_campaign()

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got == Campaign(
        id=campaign["id"],
        data=CampaignData(
            name="campaign0",
            author_id=123,
            created_datetime=Timestamp(seconds=0),
            publication_envs=[PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            status=CampaignStatus.DRAFT,
            start_datetime=Timestamp(
                seconds=int(datetime(2019, 1, 1, tzinfo=timezone.utc).timestamp())
            ),
            end_datetime=Timestamp(
                seconds=int(datetime(2019, 2, 1, tzinfo=timezone.utc).timestamp())
            ),
            timezone="UTC",
            billing=Billing(
                cpm=Cpm(
                    cost=Money(value=123456),
                    budget=Money(value=10000000),
                    daily_budget=Money(value=50000000),
                    auto_daily_budget=False,
                )
            ),
            placing=Placing(organizations=Organizations(permalinks=[123, 345])),
            platforms=[Platform.NAVI],
            creatives=[],
            actions=[],
            week_schedule=[],
            comment="",
            targeting="{}",
            order_id=10,
            discounts=[],
            settings=CampaignSettings(),
        ),
    )


@pytest.mark.parametrize(
    ("field", "value"),
    [
        ("author_id", 22),
        ("name", "Название кампании"),
        ("timezone", "Asia/Novosibirsk"),
        ("comment", "Комментарий текстом"),
        ("user_display_limit", 10),
        ("user_daily_display_limit", 2),
    ],
)
async def test_returns_common_campaign_data(api, factory, field, value):
    campaign = await factory.create_campaign(**{field: value})

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert getattr(got.data, field) == value


async def test_returns_targeting_data(api, factory):
    campaign = await factory.create_campaign(targeting={"age": "25-34"})

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.targeting == json.dumps({"age": "25-34"})


@pytest.mark.parametrize(
    ("value", "expected_value"),
    [
        ([PublicationEnvEnum.PRODUCTION], [PublicationEnv.PRODUCTION]),
        ([PublicationEnvEnum.DATA_TESTING], [PublicationEnv.DATA_TESTING]),
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            [PublicationEnv.DATA_TESTING, PublicationEnv.PRODUCTION],
        ),
    ],
)
async def test_returns_publications_envs(api, factory, value, expected_value):
    campaign = await factory.create_campaign(publication_envs=value)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.publication_envs == expected_value


@pytest.mark.parametrize(
    ("value", "expected_value"),
    [
        (CampaignTypeEnum.PIN_ON_ROUTE, CampaignType.PIN_ON_ROUTE),
        (CampaignTypeEnum.BILLBOARD, CampaignType.BILLBOARD),
        (CampaignTypeEnum.ZERO_SPEED_BANNER, CampaignType.ZERO_SPEED_BANNER),
        (CampaignTypeEnum.CATEGORY_SEARCH, CampaignType.CATEGORY_SEARCH),
        (CampaignTypeEnum.VIA_POINTS, CampaignType.VIA_POINTS),
        (CampaignTypeEnum.ROUTE_BANNER, CampaignType.ROUTE_BANNER),
        (CampaignTypeEnum.OVERVIEW_BANNER, CampaignType.OVERVIEW_BANNER),
        (CampaignTypeEnum.PROMOCODE, CampaignType.PROMOCODE),
    ],
)
async def test_returns_campaign_type(api, factory, value, expected_value):
    campaign = await factory.create_campaign(campaign_type=value)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.campaign_type == expected_value


@pytest.mark.parametrize(
    ("value", "expected_value"),
    [
        ([PlatformEnum.NAVI], [Platform.NAVI]),
        ([PlatformEnum.MAPS], [Platform.MAPS]),
        ([PlatformEnum.METRO], [Platform.METRO]),
    ],
)
async def test_returns_platforms(api, factory, value, expected_value):
    campaign = await factory.create_campaign(platforms=value)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.platforms == expected_value


@pytest.mark.parametrize(
    ("value", "expected_value"),
    [
        (RubricEnum.COMMON, Rubric.COMMON),
        (RubricEnum.AUTO, Rubric.AUTO),
        (RubricEnum.REALTY, Rubric.REALTY),
    ],
)
async def test_returns_rubric(api, factory, value, expected_value):
    campaign = await factory.create_campaign(rubric=value)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.rubric == expected_value


@pytest.mark.parametrize(
    ("value", "expected_value"),
    [
        (OrderSizeEnum.SMALL, OrderSize.SMALL),
        (OrderSizeEnum.BIG, OrderSize.BIG),
        (OrderSizeEnum.VERY_BIG, OrderSize.VERY_BIG),
    ],
)
async def test_returns_order_size(api, factory, value, expected_value):
    campaign = await factory.create_campaign(order_size=value)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.order_size == expected_value


@pytest.mark.parametrize(
    ("value", "expected_value"),
    [
        (
            datetime(2020, 4, 7, tzinfo=timezone.utc),
            Timestamp(
                seconds=int(datetime(2020, 4, 7, tzinfo=timezone.utc).timestamp())
            ),
        )
    ],
)
async def test_returns_datatesting_expires_at(api, factory, value, expected_value):
    campaign = await factory.create_campaign(datatesting_expires_at=value)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.datatesting_expires_at == expected_value


async def test_returns_empty_rubric(api, factory):
    campaign = await factory.create_campaign(rubric=None)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert not got.data.HasField("rubric")


@pytest.mark.parametrize("field", ["order_id", "manul_order_id"])
async def test_returns_order_id(api, factory, field):
    campaign = await factory.create_campaign(**{field: 11})

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert getattr(got.data, field) == 11


async def test_returns_billing_cpm_data(api, factory):
    campaign = await factory.create_campaign(
        cpm={
            "cost": Decimal("10.20"),
            "budget": Decimal("12.3456"),
            "daily_budget": Decimal("56.1234"),
            "auto_daily_budget": False,
        }
    )

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.billing.cpm == Cpm(
        cost=Money(value=102000),
        budget=Money(value=123456),
        daily_budget=Money(value=561234),
        auto_daily_budget=False,
    )


async def test_returns_billing_cpm_data_wtih_empty_fields(api, factory):
    campaign = await factory.create_campaign(
        cpm={
            "cost": Decimal("10.20"),
            "budget": None,
            "daily_budget": None,
            "auto_daily_budget": False,
        }
    )

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.billing.cpm.cost == Money(value=102000)
    assert got.data.billing.cpm.auto_daily_budget is False
    assert not got.data.billing.cpm.HasField("budget")
    assert not got.data.billing.cpm.HasField("daily_budget")


async def test_returns_billing_cpa_data(api, factory):
    campaign = await factory.create_campaign(
        cpa={
            "cost": Decimal("10.20"),
            "budget": Decimal("12.3456"),
            "daily_budget": Decimal("56.1234"),
            "auto_daily_budget": False,
        }
    )

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.billing.cpa == Cpa(
        cost=Money(value=102000),
        budget=Money(value=123456),
        daily_budget=Money(value=561234),
        auto_daily_budget=False,
    )


async def test_returns_billing_cpa_data_wtih_empty_fields(api, factory):
    campaign = await factory.create_campaign(
        cpa={
            "cost": Decimal("10.20"),
            "budget": None,
            "daily_budget": None,
            "auto_daily_budget": False,
        }
    )

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.billing.cpa.cost == Money(value=102000)
    assert got.data.billing.cpa.auto_daily_budget is False
    assert not got.data.billing.cpa.HasField("budget")
    assert not got.data.billing.cpa.HasField("daily_budget")


@pytest.mark.parametrize(
    ("time_interval", "expected_time_interval"),
    [
        (FixTimeIntervalEnum.DAILY, Fix.TimeIntervalEnum.DAILY),
        (FixTimeIntervalEnum.WEEKLY, Fix.TimeIntervalEnum.WEEKLY),
        (FixTimeIntervalEnum.MONTHLY, Fix.TimeIntervalEnum.MONTHLY),
    ],
)
async def test_returns_billing_fix_data(
    api, factory, time_interval, expected_time_interval
):
    campaign = await factory.create_campaign(
        fix={"cost": Decimal("10.20"), "time_interval": time_interval}
    )

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.billing.fix == Fix(
        cost=Money(value=102000), time_interval=expected_time_interval
    )


@pytest.mark.parametrize(
    ("actions", "expected_actions"),
    [
        ([], []),
        (
            [
                {
                    "type_": "search",
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "main": False,
                }
            ],
            [
                Action(
                    title="Поискать",
                    search=Search(organizations=[1, 2, 4], history_text="Поискал"),
                    main=False,
                )
            ],
        ),
        (
            [{"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}],
            [Action(title="Перейти", open_site=OpenSite(url="ya.ru"), main=False)],
        ),
        (
            [
                {
                    "type_": "phone_call",
                    "title": "Позвонить",
                    "phone": "322-223",
                    "main": False,
                }
            ],
            [
                Action(
                    title="Позвонить", phone_call=PhoneCall(phone="322-223"), main=False
                )
            ],
        ),
        (
            [
                {
                    "type_": "download_app",
                    "title": "Установить",
                    "google_play_id": "qwerty",
                    "app_store_id": "ytrewq",
                    "url": "yandex.ru",
                    "main": False,
                }
            ],
            [
                Action(
                    title="Установить",
                    download_app=DownloadApp(
                        google_play_id="qwerty", app_store_id="ytrewq", url="yandex.ru"
                    ),
                    main=False,
                )
            ],
        ),
        (
            [{"type_": "promocode", "promocode": "123", "main": False}],
            [Action(promocode=Promocode(promocode="123"), main=False)],
        ),
        (
            [
                {
                    "type_": "resolve_uri",
                    "uri": "magic://ya.ru",
                    "action_type": ActionTypeEnum.OPEN_SITE,
                    "target": ResolveUriTargetEnum.WEB_VIEW,
                    "main": False,
                }
            ],
            [
                Action(
                    resolve_uri=ResolveUri(
                        uri="magic://ya.ru",
                        action_type=ActionType.OPEN_SITE,
                        target=ResolveUri.Target.WEB_VIEW,
                        dialog=None,
                    ),
                    main=False,
                )
            ],
        ),
        (
            [
                {
                    "type_": "add_point_to_route",
                    "latitude": 1.2,
                    "longitude": 3.4,
                    "main": False,
                }
            ],
            [
                Action(
                    add_point_to_route=AddPointToRoute(latitude=1.2, longitude=3.4),
                    main=False,
                )
            ],
        ),
        (
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
            [
                Action(
                    title="Перейти туда", open_site=OpenSite(url="ya.ru"), main=False
                ),
                Action(
                    title="Перейти сюда",
                    open_site=OpenSite(url="yandex.ru"),
                    main=False,
                ),
                Action(
                    title="Позвонить", phone_call=PhoneCall(phone="322-223"), main=False
                ),
            ],
        ),
        (
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
                    "main": True,
                },
            ],
            [
                Action(
                    title="Перейти туда", open_site=OpenSite(url="ya.ru"), main=False
                ),
                Action(
                    title="Перейти сюда", open_site=OpenSite(url="yandex.ru"), main=True
                ),
            ],
        ),
    ],
)
async def test_returns_actions_data(api, factory, actions, expected_actions):
    campaign = await factory.create_campaign(actions=actions)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert list(got.data.actions) == expected_actions


@pytest.mark.parametrize(
    ("actions", "expected_actions"),
    [
        ([], []),
        (
            [
                {
                    "type_": "search",
                    "title": None,
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "main": False,
                }
            ],
            [
                Action(
                    search=Search(organizations=[1, 2, 4], history_text="Поискал"),
                    main=False,
                )
            ],
        ),
        (
            [{"type_": "open_site", "title": None, "url": "ya.ru", "main": False}],
            [Action(open_site=OpenSite(url="ya.ru"), main=False)],
        ),
        (
            [{"type_": "phone_call", "title": None, "phone": "322-223", "main": False}],
            [Action(phone_call=PhoneCall(phone="322-223"), main=False)],
        ),
        (
            [
                {
                    "type_": "download_app",
                    "title": None,
                    "google_play_id": "qwerty",
                    "app_store_id": "ytrewq",
                    "url": "yandex.ru",
                    "main": False,
                }
            ],
            [
                Action(
                    download_app=DownloadApp(
                        google_play_id="qwerty", app_store_id="ytrewq", url="yandex.ru"
                    ),
                    main=False,
                )
            ],
        ),
    ],
)
async def test_actions_empty_title(api, factory, actions, expected_actions):
    campaign = await factory.create_campaign(actions=actions)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert list(got.data.actions) == expected_actions


@pytest.mark.parametrize(
    ("creatives", "expected_creatives"),
    [
        ([], []),
        (
            [
                {
                    "type_": "pin",
                    "title": "Заголовок",
                    "subtitle": "Подзаголовок",
                    "images": [
                        {
                            "type": "type1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                        },
                        {
                            "type": "type2",
                            "group_id": "group_id2",
                            "image_name": "image_name2",
                        },
                    ],
                }
            ],
            [
                Creative(
                    pin=Pin(
                        title="Заголовок",
                        subtitle="Подзаголовок",
                        images=[
                            Image(
                                type="type1",
                                group_id="group_id1",
                                image_name="image_name1",
                            ),
                            Image(
                                type="type2",
                                group_id="group_id2",
                                image_name="image_name2",
                            ),
                        ],
                    )
                )
            ],
        ),
        (
            [
                {
                    "type_": "billboard",
                    "images": [
                        {
                            "type": "type1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                        },
                        {
                            "type": "type2",
                            "group_id": "group_id2",
                            "image_name": "image_name2",
                        },
                    ],
                    "title": "Заголовок",
                    "description": "Подзаголовок",
                }
            ],
            [
                Creative(
                    billboard=Billboard(
                        title="Заголовок",
                        description="Подзаголовок",
                        images=[
                            Image(
                                type="type1",
                                group_id="group_id1",
                                image_name="image_name1",
                            ),
                            Image(
                                type="type2",
                                group_id="group_id2",
                                image_name="image_name2",
                            ),
                        ],
                    )
                )
            ],
        ),
        (
            [
                {
                    "type_": "icon",
                    "images": [
                        {
                            "type": "type1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                        },
                        {
                            "type": "type2",
                            "group_id": "group_id2",
                            "image_name": "image_name2",
                        },
                    ],
                    "position": 8,
                    "title": "Заголовок",
                }
            ],
            [
                Creative(
                    icon=Icon(
                        position=8,
                        title="Заголовок",
                        images=[
                            Image(
                                type="type1",
                                group_id="group_id1",
                                image_name="image_name1",
                            ),
                            Image(
                                type="type2",
                                group_id="group_id2",
                                image_name="image_name2",
                            ),
                        ],
                    )
                )
            ],
        ),
        (
            [
                {
                    "type_": "pin_search",
                    "images": [
                        {
                            "type": "type1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                        },
                        {
                            "type": "type2",
                            "group_id": "group_id2",
                            "image_name": "image_name2",
                        },
                    ],
                    "title": "Заголовок",
                    "organizations": [1, 2, 4],
                }
            ],
            [
                Creative(
                    pin_search=PinSearch(
                        title="Заголовок",
                        organizations=[1, 2, 4],
                        images=[
                            Image(
                                type="type1",
                                group_id="group_id1",
                                image_name="image_name1",
                            ),
                            Image(
                                type="type2",
                                group_id="group_id2",
                                image_name="image_name2",
                            ),
                        ],
                    )
                )
            ],
        ),
        (
            [
                {
                    "type_": "logo_and_text",
                    "images": [
                        {
                            "type": "type1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                        },
                        {
                            "type": "type2",
                            "group_id": "group_id2",
                            "image_name": "image_name2",
                        },
                    ],
                    "text": "Текст",
                }
            ],
            [
                Creative(
                    logo_and_text=LogoAndText(
                        text="Текст",
                        images=[
                            Image(
                                type="type1",
                                group_id="group_id1",
                                image_name="image_name1",
                            ),
                            Image(
                                type="type2",
                                group_id="group_id2",
                                image_name="image_name2",
                            ),
                        ],
                    )
                )
            ],
        ),
        (
            [{"type_": "text", "text": "Текст", "disclaimer": "Дисклеймер"}],
            [Creative(text=Text(text="Текст", disclaimer="Дисклеймер"))],
        ),
        (
            [
                {
                    "type_": "via_point",
                    "images": [
                        {
                            "type": "type1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                        },
                        {
                            "type": "type2",
                            "group_id": "group_id2",
                            "image_name": "image_name2",
                        },
                    ],
                    "button_text_active": "Кнопкотекст",
                    "button_text_inactive": "Неактивнокнопкотекст",
                    "description": "Описание",
                }
            ],
            [
                Creative(
                    via_point=ViaPoint(
                        button_text_active="Кнопкотекст",
                        button_text_inactive="Неактивнокнопкотекст",
                        description="Описание",
                        images=[
                            Image(
                                type="type1",
                                group_id="group_id1",
                                image_name="image_name1",
                            ),
                            Image(
                                type="type2",
                                group_id="group_id2",
                                image_name="image_name2",
                            ),
                        ],
                    )
                )
            ],
        ),
        (
            [
                {
                    "type_": "banner",
                    "images": [
                        {
                            "type": "type1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                        },
                        {
                            "type": "type2",
                            "group_id": "group_id2",
                            "image_name": "image_name2",
                        },
                    ],
                    "disclaimer": "Дисклеймер",
                    "show_ads_label": True,
                    "description": "Описание",
                    "title": "Заголовок",
                    "terms": "Условия",
                }
            ],
            [
                Creative(
                    banner=Banner(
                        disclaimer="Дисклеймер",
                        show_ads_label=True,
                        description="Описание",
                        title="Заголовок",
                        terms="Условия",
                        images=[
                            Image(
                                type="type1",
                                group_id="group_id1",
                                image_name="image_name1",
                            ),
                            Image(
                                type="type2",
                                group_id="group_id2",
                                image_name="image_name2",
                            ),
                        ],
                    )
                )
            ],
        ),
        (
            [
                {"type_": "text", "text": "Текст 1", "disclaimer": "Дисклеймер 1"},
                {"type_": "text", "text": "Текст 2", "disclaimer": "Дисклеймер 2"},
                {
                    "type_": "banner",
                    "images": [
                        {
                            "type": "type1",
                            "group_id": "group_id1",
                            "image_name": "image_name1",
                        },
                        {
                            "type": "type2",
                            "group_id": "group_id2",
                            "image_name": "image_name2",
                        },
                    ],
                    "disclaimer": "Дисклеймер",
                    "show_ads_label": True,
                    "description": "Описание",
                    "title": "Заголовок",
                    "terms": "Условия",
                },
            ],
            [
                Creative(text=Text(text="Текст 1", disclaimer="Дисклеймер 1")),
                Creative(text=Text(text="Текст 2", disclaimer="Дисклеймер 2")),
                Creative(
                    banner=Banner(
                        disclaimer="Дисклеймер",
                        show_ads_label=True,
                        description="Описание",
                        title="Заголовок",
                        terms="Условия",
                        images=[
                            Image(
                                type="type1",
                                group_id="group_id1",
                                image_name="image_name1",
                            ),
                            Image(
                                type="type2",
                                group_id="group_id2",
                                image_name="image_name2",
                            ),
                        ],
                    )
                ),
            ],
        ),
    ],
)
async def test_returns_creatives_data(api, factory, creatives, expected_creatives):
    campaign = await factory.create_campaign(creatives=creatives)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert list(got.data.creatives) == expected_creatives


async def test_returns_placing_organizations_data(api, factory):
    campaign = await factory.create_campaign(organizations={"permalinks": [123, 345]})

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.placing == Placing(
        organizations=Organizations(permalinks=[123, 345])
    )


async def test_returns_placing_area_data(api, factory):
    campaign = await factory.create_campaign(
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
        }
    )

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.placing == Placing(
        area=Area(
            areas=[
                Polygon(
                    points=[
                        Point(lon=12.345, lat=23.567),
                        Point(lon=14.345, lat=23.567),
                        Point(lon=14.345, lat=25.567),
                        Point(lon=12.345, lat=23.567),
                    ],
                    name="Танковый полигон",
                ),
                Polygon(
                    points=[
                        Point(lon=22.345, lat=33.567),
                        Point(lon=24.345, lat=33.567),
                        Point(lon=24.345, lat=35.567),
                        Point(lon=22.345, lat=33.567),
                    ],
                    name="Ядерный полигон",
                ),
            ],
            version=1,
        )
    )


async def test_returns_week_schedule_data(api, factory):
    campaign = await factory.create_campaign(
        week_schedule=[
            {"start": 60 * 24 * 3, "end": 60 * 24 * 4},
            {"start": 60 * 24 * 5, "end": 60 * 24 * 6},
        ]
    )

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert list(got.data.week_schedule) == [
        WeekScheduleItem(start=60 * 24 * 3, end=60 * 24 * 4),
        WeekScheduleItem(start=60 * 24 * 5, end=60 * 24 * 6),
    ]


@pytest.mark.parametrize(
    ("statuses", "expected_status"),
    [
        ([CampaignStatusEnum.DRAFT], CampaignStatus.DRAFT),
        ([CampaignStatusEnum.ACTIVE], CampaignStatus.ACTIVE),
        ([CampaignStatusEnum.REVIEW], CampaignStatus.REVIEW),
        ([CampaignStatusEnum.REJECTED], CampaignStatus.REJECTED),
        ([CampaignStatusEnum.PAUSED], CampaignStatus.PAUSED),
        ([CampaignStatusEnum.DONE], CampaignStatus.DONE),
        ([CampaignStatusEnum.ARCHIVED], CampaignStatus.ARCHIVED),
        ([CampaignStatusEnum.DONE, CampaignStatusEnum.ACTIVE], CampaignStatus.ACTIVE),
        (
            [
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.ACTIVE,
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.ACTIVE,
                CampaignStatusEnum.PAUSED,
            ],
            CampaignStatus.PAUSED,
        ),
    ],
)
async def test_returns_status_data(api, factory, statuses, expected_status):
    campaign = await factory.create_campaign()
    for ts, status in enumerate(statuses, start=1):
        await factory.set_status(
            campaign["id"],
            status=status,
            changed_datetime=datetime.fromtimestamp(ts, tz=timezone.utc),
        )

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.status == expected_status


@pytest.mark.parametrize(
    ("discounts", "expected_discounts"),
    [
        ([], []),
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
                    start_datetime=Timestamp(
                        seconds=int(
                            datetime(2019, 1, 1, tzinfo=timezone.utc).timestamp()
                        )
                    ),
                    end_datetime=Timestamp(
                        seconds=int(
                            datetime(2019, 2, 2, tzinfo=timezone.utc).timestamp()
                        )
                    ),
                    cost_multiplier="0.7500",
                )
            ],
        ),
        (
            [
                {
                    "start_datetime": datetime(2019, 3, 3, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 4, 4, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("1.35"),
                },
                {
                    "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("0.75"),
                },
            ],
            [
                Discount(
                    start_datetime=Timestamp(
                        seconds=int(
                            datetime(2019, 1, 1, tzinfo=timezone.utc).timestamp()
                        )
                    ),
                    end_datetime=Timestamp(
                        seconds=int(
                            datetime(2019, 2, 2, tzinfo=timezone.utc).timestamp()
                        )
                    ),
                    cost_multiplier="0.7500",
                ),
                Discount(
                    start_datetime=Timestamp(
                        seconds=int(
                            datetime(2019, 3, 3, tzinfo=timezone.utc).timestamp()
                        )
                    ),
                    end_datetime=Timestamp(
                        seconds=int(
                            datetime(2019, 4, 4, tzinfo=timezone.utc).timestamp()
                        )
                    ),
                    cost_multiplier="1.3500",
                ),
            ],
        ),
    ],
)
async def test_returns_discounts_data(api, factory, discounts, expected_discounts):
    campaign = await factory.create_campaign(discounts=discounts)

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert list(got.data.discounts) == expected_discounts


async def test_returns_created_datetime(api, factory, con):
    campaign = await factory.create_campaign()
    await con.execute(
        "DELETE FROM status_history WHERE campaign_id = $1", campaign["id"]
    )
    await factory.set_status(campaign["id"], changed_datetime=dt("2000-01-02 21:34:00"))
    await factory.set_status(campaign["id"], changed_datetime=dt("2000-05-05 21:34:00"))
    await factory.set_status(campaign["id"], changed_datetime=dt("2000-05-05 10:30:00"))

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.created_datetime == dt("2000-01-02 21:34:00", as_proto=True)


async def test_returns_settings(api, factory, con):
    campaign = await factory.create_campaign(
        campaign_type=CampaignTypeEnum.OVERVIEW_BANNER,
        settings={
            "custom_page_id": "abc",
            "overview_position": OverviewPositionEnum.FINISH,
            "forced_product_version_datetime": (
                datetime(2021, 1, 1, tzinfo=timezone.utc).timestamp()
            ),
            "verification_data": [
                {"platform": "weborama", "params": {"a": "b", "x": "y"}}
            ],
            "auto_prolongation": True,
        },
    )

    got = await api.get(
        url.format(campaign["id"]), decode_as=Campaign, expected_status=200
    )

    assert got.data.settings.custom_page_id == "abc"
    assert got.data.settings.overview_position == OverviewPosition.Enum.FINISH
    assert got.data.settings.forced_product_version_datetime == (
        dt("2021-01-01 00:00:00", as_proto=True)
    )
    assert len(got.data.settings.verification_data) == 1
    assert got.data.settings.verification_data[0].platform == "weborama"
    assert got.data.settings.verification_data[0].params == {"a": "b", "x": "y"}
    assert got.data.settings.auto_prolongation is True


async def test_returns_404_if_campaign_not_exists(api):
    await api.get(url.format(111), expected_status=404)

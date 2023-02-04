import json

import pytest

from decimal import Decimal

from maps_adv.adv_store.api.proto.action_pb2 import (
    Action,
    ActionType,
    AddPointToRoute,
    OpenSite,
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
    VerificationData,
    WeekScheduleItem,
)
from maps_adv.adv_store.api.proto.campaign_status_pb2 import CampaignStatus
from maps_adv.adv_store.api.proto.creative_pb2 import (
    AudioBanner,
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
from maps_adv.adv_store.v2.tests import dt
from maps_adv.common.proto.campaign_pb2 import CampaignType

pytestmark = [pytest.mark.asyncio]

url = "/campaigns/"


__campaign_proto_kwargs = dict(
    name="campaign0",
    author_id=100500,
    publication_envs=[PublicationEnv.PRODUCTION],
    campaign_type=CampaignType.VIA_POINTS,
    status=CampaignStatus.DRAFT,
    start_datetime=dt("2019-01-01 00:00:00", as_proto=True),
    end_datetime=dt("2019-01-31 23:59:59", as_proto=True),
    timezone="Europe/Moscow",
    order_id=10,
    billing=Billing(
        cpm=Cpm(
            cost=Money(value=6000),
            budget=Money(value=600000),
            daily_budget=Money(value=24000),
            auto_daily_budget=False,
        )
    ),
    placing=Placing(organizations=Organizations(permalinks=[100, 101, 102])),
    platforms=[Platform.NAVI],
    creatives=[
        Creative(
            via_point=ViaPoint(
                images=[Image(type="type", group_id="123", image_name="qwerty")],
                button_text_active="Жми",
                button_text_inactive="Не жми",
                description="Зачем жать",
            )
        )
    ],
    actions=[
        Action(open_site=OpenSite(url="http://yandex.ru"), title="Текст", main=False),
        Action(
            resolve_uri=ResolveUri(
                uri="magic://yandex.ru",
                action_type=ActionType.OPEN_SITE,
                target=ResolveUri.Target.WEB_VIEW,
                dialog=None,
            ),
            main=False,
        ),
        Action(
            add_point_to_route=AddPointToRoute(latitude=1.2, longitude=3.4), main=True
        ),
    ],
    week_schedule=[WeekScheduleItem(start=0, end=60)],
    comment="Коммент",
    rubric=Rubric.COMMON,
    order_size=OrderSize.SMALL,
    targeting='{"tag": "and", "items": ['
    '{"tag": "age", "content": ["25-34"]}, '
    '{"tag": "gender", "content": "male"}]}',
    user_daily_display_limit=2,
    user_display_limit=3,
    discounts=[
        Discount(
            start_datetime=dt("2019-01-01 00:00:00", as_proto=True),
            end_datetime=dt("2019-01-31 23:59:59", as_proto=True),
            cost_multiplier="0.5000",
        )
    ],
    datatesting_expires_at=dt("2020-04-07 00:00:00", as_proto=True),
    settings=CampaignSettings(
        custom_page_id="abc",
        forced_product_version_datetime=dt("2021-01-01 00:00:00", as_proto=True),
        verification_data=[
            VerificationData(platform="weborama", params={"a": "b", "x": "y"}),
            VerificationData(platform="weborama", params={"a": "1", "x": "2"}),
            VerificationData(platform="dcm", params={"url": "https://dcm.url"}),
        ],
    ),
    product_id=1,
)


@pytest.fixture
def campaign_proto():
    def _make(**kwargs):
        proto_kwargs = __campaign_proto_kwargs.copy()
        proto_kwargs.update(kwargs)
        return CampaignData(**proto_kwargs)

    return _make


async def test_returns_created_campaign_details(
    api, campaign_proto, factory, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(0.6)

    pb_input = campaign_proto(
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[Platform.NAVI, Platform.MAPS],
        creatives=[
            Creative(
                pin=Pin(
                    images=[Image(type="type", group_id="123", image_name="qwerty")],
                    title="Заголовок",
                    subtitle="Подзаголовок",
                ),
            ),
            Creative(
                banner=Banner(
                    images=[Image(type="type", group_id="123", image_name="qwerty")],
                    disclaimer="Дисклеймер",
                    show_ads_label=True,
                    terms="",
                    description="",
                    title="",
                ),
            ),
        ],
    )

    got = await api.post(url, proto=pb_input, decode_as=Campaign, expected_status=201)

    expected_pb_output = Campaign(id=got.id)
    expected_pb_output.data.CopyFrom(pb_input)
    creation_ts = (await factory.get_campaign_creation_dt(got.id)).timestamp()
    expected_pb_output.data.created_datetime.seconds = int(creation_ts)
    expected_pb_output.data.created_datetime.nanos = (
        int("{:.6f}".format(creation_ts).split(".")[-1]) * 1000
    )
    expected_pb_output.data.ClearField("product_id")
    assert got == expected_pb_output


async def test_creates_campaign(con, api, campaign_proto, billing_proxy_client):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(0.6)

    pb_input = campaign_proto()

    got = await api.post(url, proto=pb_input, decode_as=Campaign, expected_status=201)

    sql = """SELECT EXISTS (SELECT * FROM campaign WHERE id = $1)"""
    assert await con.fetchval(sql, got.id)


async def test_errored_if_no_orders_specified(api, campaign_proto):
    pb_input = campaign_proto(order_id=None, manul_order_id=None)

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.NO_ORDERS_SPECIFIED)


@pytest.mark.parametrize(
    "input_kw, code",
    (
        [{"name": ""}, Error.INVALID_CAMPAIGN_NAME],
        [
            {
                "start_datetime": dt("2019-01-31 23:59:59", as_proto=True),
                "end_datetime": dt("2019-01-01 00:00:00", as_proto=True),
            },
            Error.SHOWING_PERIOD_START_LATER_THAN_END,
        ],
        [{"targeting": "lol kek"}, Error.TARGETING_DOES_NOT_MATCH_SCHEMA],
        [
            {"targeting": '{"unexpected": "field"}'},
            Error.TARGETING_DOES_NOT_MATCH_SCHEMA,
        ],
        [{"timezone": "Asia/Europe"}, Error.INVALID_TIMEZONE_NAME],
        [{"name": ""}, Error.INVALID_CAMPAIGN_NAME],
        [{"publication_envs": []}, Error.PUBLICATION_ENVS_ARE_EMPTY],
        [{"platforms": []}, Error.PLATFORMS_ARE_EMPTY],
    ),
)
async def test_errored_for_wrong_input(input_kw, code, api, campaign_proto):
    pb_input = campaign_proto(**input_kw)

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=code)


@pytest.mark.parametrize(
    ("field", "expected_default"),
    [
        ("comment", ""),
        ("user_display_limit", None),
        ("user_daily_display_limit", None),
        ("rubric", None),
        ("order_size", None),
        ("targeting", {}),
        ("settings", {}),
    ],
)
async def test_optional_campaign_fields_may_be_skipped(
    field, expected_default, api, campaign_proto, con, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(0.6)

    pb_input = campaign_proto()
    pb_input.ClearField(field)

    await api.post(url, proto=pb_input, expected_status=201)

    sql = f"""SELECT {field} FROM campaign"""
    assert await con.fetchval(sql) == expected_default


@pytest.mark.parametrize(
    "input_billing_kw",
    [
        {
            "cpm": Cpm(
                cost=Money(value=0),
                budget=Money(value=600000),
                daily_budget=Money(value=24000),
                auto_daily_budget=False,
            )
        },
        {
            "cpm": Cpm(
                cost=Money(value=100),
                budget=Money(value=0),
                daily_budget=Money(value=24000),
                auto_daily_budget=False,
            )
        },
        {
            "cpm": Cpm(
                cost=Money(value=10000),
                budget=Money(value=600000),
                daily_budget=Money(value=0),
                auto_daily_budget=False,
            )
        },
        {
            "cpa": Cpa(
                cost=Money(value=0),
                budget=Money(value=600000),
                daily_budget=Money(value=24000),
                auto_daily_budget=False,
            )
        },
        {
            "cpa": Cpa(
                cost=Money(value=10000),
                budget=Money(value=0),
                daily_budget=Money(value=24000),
                auto_daily_budget=False,
            )
        },
        {
            "cpa": Cpa(
                cost=Money(value=10000),
                budget=Money(value=600000),
                daily_budget=Money(value=0),
                auto_daily_budget=False,
            )
        },
        {"fix": Fix(cost=Money(value=0), time_interval=Fix.TimeIntervalEnum.DAILY)},
    ],
)
async def test_errored_for_zero_billing_costs(
    input_billing_kw, api, campaign_proto, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(0.6)

    pb_input = campaign_proto(billing=Billing(**input_billing_kw))

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.MONEY_QUANTITY_NOT_POSITIVE)


@pytest.mark.parametrize(
    "input_billing_kw",
    [
        {
            "cpm": Cpm(
                cost=Money(value=10000),
                budget=Money(value=600000),
                daily_budget=None,
                auto_daily_budget=True,
            )
        },
        {
            "cpa": Cpa(
                cost=Money(value=10000),
                budget=Money(value=600000),
                daily_budget=None,
                auto_daily_budget=True,
            )
        },
    ],
)
async def test_errored_for_auto_daily_budget(
    input_billing_kw, api, campaign_proto, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(0.6)

    pb_input = campaign_proto(billing=Billing(**input_billing_kw))

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.INVALID_BILLING)


async def test_errored_for_wrong_billing(api, campaign_proto, billing_proxy_client):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(0.6)

    pb_input = campaign_proto(billing=Billing())

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.INVALID_BILLING)


@pytest.mark.parametrize(
    ("campaign_type", "input_placing_kw", "code"),
    [
        (CampaignType.PIN_ON_ROUTE, {}, Error.INVALID_PLACING),
        (
            CampaignType.PIN_ON_ROUTE,
            {"organizations": Organizations(permalinks=[])},
            Error.PLACING_PERMALINKS_ARE_EMPTY,
        ),
        (
            CampaignType.BILLBOARD,
            {"area": Area(areas=[], version=1)},
            Error.PLACING_AREAS_ARE_EMPTY,
        ),
        (
            CampaignType.BILLBOARD,
            {
                "area": Area(
                    areas=[Polygon(points=[], name="Ядерный полигон")], version=1
                )
            },
            Error.PLACING_AREA_POLYGON_IS_EMPTY,
        ),
        (
            CampaignType.BILLBOARD,
            {
                "area": Area(
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
                        Polygon(points=[], name="Ядерный полигон"),
                    ],
                    version=1,
                )
            },
            Error.PLACING_AREA_POLYGON_IS_EMPTY,
        ),
    ],
)
async def test_errored_for_wrong_placing(
    campaign_type, input_placing_kw, code, api, campaign_proto
):
    pb_input = campaign_proto(placing=Placing(**input_placing_kw))

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=code)


async def test_errored_for_search_has_action_empty_organizations(api, campaign_proto):
    pb_input = campaign_proto(
        actions=[
            Action(
                search=Search(history_text="Поискал", organizations=[]),
                title="Поискать",
            )
        ]
    )

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.ACTION_SEARCH_ORGANIZATIONS_ARE_EMPTY)


@pytest.mark.parametrize(
    "creative_pb",
    [
        Creative(pin=Pin(images=[], title="Заголовок", subtitle="Подзаголовок")),
        Creative(billboard=Billboard(images=[], images_v2=[])),
        Creative(icon=Icon(images=[], position=2, title="Подпись")),
        Creative(
            pin_search=PinSearch(images=[], title="Подпись", organizations=[123, 456])
        ),
        Creative(logo_and_text=LogoAndText(images=[], text="Текст")),
        Creative(
            via_point=ViaPoint(
                images=[],
                button_text_active="Жми",
                button_text_inactive="Не жми",
                description="Зачем жать",
            )
        ),
        Creative(
            banner=Banner(
                images=[], disclaimer="Дисклеймер", show_ads_label=True, terms=""
            )
        ),
        Creative(
            audio_banner=AudioBanner(
                images=[], left_anchor="1111.222", audio_file_url="http://somesite.com"
            )
        ),
    ],
)
async def test_errored_for_creative_with_empty_images(creative_pb, api, campaign_proto):
    pb_input = campaign_proto(creatives=[creative_pb])

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.CREATIVE_IMAGES_ARE_EMPTY)


@pytest.mark.parametrize(
    "creative_pb, campaign_type",
    [
        (
            [
                Creative(
                    billboard=Billboard(
                        images=[
                            Image(type="type", group_id="123", image_name="qwerty")
                        ],
                    )
                ),
                Creative(
                    banner=Banner(
                        images=[
                            Image(type="type", group_id="321", image_name="ytrewq")
                        ],
                        description="description",
                        disclaimer="disclaimer",
                        show_ads_label=True,
                    )
                ),
            ],
            CampaignType.BILLBOARD,
        ),
    ],
)
async def test_errored_for_inconsistent_creatives(
    creative_pb, campaign_type, api, campaign_proto
):
    pb_input = campaign_proto(creatives=creative_pb, campaign_type=campaign_type)

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.INCONSISTENT_CREATIVES)


async def test_errored_for_wrong_billboard(api, campaign_proto):
    pb_input = campaign_proto(
        creatives=[
            Creative(
                billboard=Billboard(
                    images=[Image(type="type", group_id="123", image_name="qwerty")],
                    title="title",
                )
            )
        ],
    )

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(
        code=Error.INVALID_INPUT_DATA,
        description=json.dumps(
            {
                "creatives": {
                    "0": {
                        "billboard": {
                            "_schema": [
                                "No image specified for new billboard format, but title and/or description is present"
                            ]
                        }
                    }
                }
            }
        ),
    )


@pytest.mark.parametrize(
    "week_schedule_items",
    [
        [WeekScheduleItem(start=20, end=10)],
        [WeekScheduleItem(start=100, end=100000)],
        [WeekScheduleItem(start=120, end=240), WeekScheduleItem(start=180, end=360)],
    ],
)
async def test_errored_for_invalid_week_schedule(
    week_schedule_items, api, campaign_proto
):
    pb_input = campaign_proto(week_schedule=week_schedule_items)

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.INVALID_WEEK_SCHEDULE)


async def test_errored_for_wrong_creatives_for_campaign_type(api, campaign_proto):
    pb_input = campaign_proto(
        campaign_type=CampaignType.PIN_ON_ROUTE,
        creatives=[
            Creative(
                billboard=Billboard(
                    images=[Image(type="type", group_id="123", image_name="qwerty")]
                )
            )
        ],
    )

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.WRONG_SET_OF_CREATIVES_FOR_THE_CAMPAIGN_TYPE)


async def test_errored_for_wrong_placing_for_campaign_type(api, campaign_proto):
    pb_input = campaign_proto(
        campaign_type=CampaignType.VIA_POINTS,
        placing=Placing(
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
                    )
                ],
                version=1,
            )
        ),
    )

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.INVALID_PLACING_FOR_CAMPAIGN_TYPE)


@pytest.mark.parametrize(
    ("campaign_type", "platform"),
    [
        (CampaignType.BILLBOARD, Platform.MAPS),
        (CampaignType.BILLBOARD, Platform.METRO),
        (CampaignType.ZERO_SPEED_BANNER, Platform.MAPS),
        (CampaignType.VIA_POINTS, Platform.MAPS),
        (CampaignType.ROUTE_BANNER, Platform.NAVI),
        (CampaignType.VIA_POINTS, Platform.MAPS),
        (CampaignType.OVERVIEW_BANNER, Platform.MAPS),
        (CampaignType.PROMOCODE, Platform.NAVI),
    ],
)
async def test_errored_for_wrong_campaign_type_for_platform(
    campaign_type, platform, api, campaign_proto
):
    pb_input = campaign_proto(campaign_type=campaign_type, platforms=[platform])

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.INVALID_CAMPAIGN_TYPE_FOR_PLATFORM)


@pytest.mark.parametrize(
    "discounts",
    [
        [
            Discount(
                start_datetime=dt("2019-1-1 00:00:00", as_proto=True),
                end_datetime=dt("2019-3-3 00:00:00", as_proto=True),
                cost_multiplier="0.7500",
            ),
            Discount(
                start_datetime=dt("2019-2-2 00:00:00", as_proto=True),
                end_datetime=dt("2019-4-4 00:00:00", as_proto=True),
                cost_multiplier="1.3500",
            ),
        ],
        [
            Discount(
                start_datetime=dt("2019-1-1 00:00:00", as_proto=True),
                end_datetime=dt("2019-4-4 00:00:00", as_proto=True),
                cost_multiplier="0.7500",
            ),
            Discount(
                start_datetime=dt("2019-2-2 00:00:00", as_proto=True),
                end_datetime=dt("2019-3-3 00:00:00", as_proto=True),
                cost_multiplier="1.3500",
            ),
        ],
    ],
)
async def test_errored_for_date_intersecting_discounts(discounts, api, campaign_proto):
    pb_input = campaign_proto(discounts=discounts)

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.DISCOUNTS_WITH_INTERSECTING_DATES)


async def test_optional_resolve_uri_fields_may_be_skipped(
    api, campaign_proto, billing_proxy_client
):
    billing_proxy_client.calculate_product_cpm.coro.return_value = Decimal(0.6)

    pb_input = campaign_proto(
        actions=[
            Action(
                resolve_uri=ResolveUri(
                    uri="magic://yandex.ru", action_type=ActionType.OPEN_SITE
                )
            )
        ]
    )

    await api.post(url, proto=pb_input, decode_as=Error, expected_status=201)


async def test_errored_for_resolve_uri_browser_action_without_target(
    api, campaign_proto
):
    pb_input = campaign_proto(
        actions=[
            Action(
                resolve_uri=ResolveUri(
                    uri="magic://yandex.ru",
                    action_type=ActionType.OPEN_SITE,
                    target=ResolveUri.Target.BROWSER,
                    dialog=None,
                )
            )
        ]
    )

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.INVALID_INPUT_DATA)


async def test_errored_for_wrong_campaign_type_with_overview_position(
    api, campaign_proto
):
    pb_input = campaign_proto(
        settings={"overview_position": OverviewPosition.Enum.FINISH}
    )

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(
        code=Error.INVALID_INPUT_DATA,
        description=json.dumps(
            {
                "settings": [
                    "overview_position setting can only be used with OVERVIEW_BANNER "
                    "campaign_type"
                ]
            }
        ),
    )


async def test_errored_for_multiple_main_actions(api, campaign_proto):
    pb_input = campaign_proto(
        actions=[
            Action(
                open_site=OpenSite(url="http://yandex.ru"), title="Текст", main=True
            ),
            Action(
                resolve_uri=ResolveUri(
                    uri="magic://yandex.ru",
                    action_type=ActionType.OPEN_SITE,
                    target=ResolveUri.Target.WEB_VIEW,
                    dialog=None,
                ),
                main=True,
            ),
        ]
    )

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.MULTIPLE_MAIN_ACTIONS)


async def test_errored_for_invalid_custom_page_id(api, campaign_proto):
    pb_input = campaign_proto(settings={"custom_page_id": "BAD_PAGE_ID"})

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(
        code=Error.INVALID_CUSTOM_PAGE_ID,
    )

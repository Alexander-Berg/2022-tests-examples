import decimal
import json
from datetime import timezone
from functools import partial

from faker.providers import BaseProvider
from faker.providers.barcode import Provider as BarcodeProvider
from faker.providers.date_time import Provider as DateTimeProvider
from faker.providers.internet import Provider as InternetProvider
from faker.providers.lorem.ru_RU import Provider as LoremProvider
from faker.providers.phone_number import Provider as PhoneProvider
from faker.providers.python import Provider as PythonProvider
from google.protobuf import timestamp_pb2

from maps_adv.adv_store.api.proto import (
    action_pb2,
    billing_pb2,
    campaign_pb2,
    campaign_status_pb2,
    creative_pb2,
    placing_pb2,
)
from maps_adv.adv_store.api.schemas import enums
from maps_adv.common.helpers.enums import CampaignTypeEnum
from maps_adv.common.proto.campaign_pb2 import (
    CampaignType,
)

ENUM_DB_TO_PB = {
    "status": {
        enums.CampaignStatusEnum.ACTIVE: campaign_status_pb2.CampaignStatus.ACTIVE,
        enums.CampaignStatusEnum.DRAFT: campaign_status_pb2.CampaignStatus.DRAFT,
        enums.CampaignStatusEnum.REVIEW: campaign_status_pb2.CampaignStatus.REVIEW,
        enums.CampaignStatusEnum.REJECTED: campaign_status_pb2.CampaignStatus.REJECTED,
        enums.CampaignStatusEnum.PAUSED: campaign_status_pb2.CampaignStatus.PAUSED,
        enums.CampaignStatusEnum.DONE: campaign_status_pb2.CampaignStatus.DONE,
        enums.CampaignStatusEnum.ARCHIVED: campaign_status_pb2.CampaignStatus.ARCHIVED,
    },
    "campaign_type": {
        CampaignTypeEnum.PIN_ON_ROUTE: CampaignType.PIN_ON_ROUTE,
        CampaignTypeEnum.BILLBOARD: CampaignType.BILLBOARD,
        CampaignTypeEnum.ZERO_SPEED_BANNER: CampaignType.ZERO_SPEED_BANNER,  # noqa: E501
        CampaignTypeEnum.CATEGORY_SEARCH: CampaignType.CATEGORY_SEARCH,  # noqa: E501
        CampaignTypeEnum.ROUTE_BANNER: CampaignType.ROUTE_BANNER,
        CampaignTypeEnum.VIA_POINTS: CampaignType.VIA_POINTS,
        CampaignTypeEnum.OVERVIEW_BANNER: CampaignType.OVERVIEW_BANNER,  # noqa: E501
        CampaignTypeEnum.PROMOCODE: CampaignType.PROMOCODE,
    },
    "platforms": {
        enums.PlatformEnum.NAVI: campaign_pb2.Platform.NAVI,
        enums.PlatformEnum.MAPS: campaign_pb2.Platform.MAPS,
        enums.PlatformEnum.METRO: campaign_pb2.Platform.METRO,
    },
    "publication_envs": {
        enums.PublicationEnvEnum.PRODUCTION: campaign_pb2.PublicationEnv.PRODUCTION,
        enums.PublicationEnvEnum.DATA_TESTING: campaign_pb2.PublicationEnv.DATA_TESTING,
    },
    "fix_time_interval": {
        enums.FixTimeIntervalEnum.DAILY: billing_pb2.Fix.DAILY,
        enums.FixTimeIntervalEnum.WEEKLY: billing_pb2.Fix.WEEKLY,
        enums.FixTimeIntervalEnum.MONTHLY: billing_pb2.Fix.MONTHLY,
    },
    "rubric": {
        enums.RubricEnum.COMMON: campaign_pb2.Rubric.COMMON,
        enums.RubricEnum.AUTO: campaign_pb2.Rubric.AUTO,
        enums.RubricEnum.REALTY: campaign_pb2.Rubric.REALTY,
    },
}

ENUM_PB_TO_DB = {
    k: dict(reversed(forward) for forward in ENUM_DB_TO_PB[k].items())
    for k in ENUM_DB_TO_PB.keys()
}


class EnumFakerProvider(BaseProvider):
    def enum(self, enum_class):
        try:
            return self.generator.random.choice(list(enum_class))
        except IndexError:
            raise IndexError("Enum class in Faker is empty")

    def enum_list(self, enum_class) -> list:
        """Returns random length list of enums"""
        enums = list(enum_class)
        if not enums:
            raise IndexError("Enum class in Faker is empty")

        to_take = self.random_int(1, len(enums))
        self.generator.random.shuffle(enums)
        return [enums.pop() for _ in range(to_take)]


class JsonFakerProvider(PythonProvider):
    VALUE_TYPES = [int, str, float]

    def json_obj(self, *args, **kwargs):
        return self.pydict(10, True, *self.VALUE_TYPES)

    def json_arr(self, *args, **kwargs):
        return self.pylist(10, True, *self.VALUE_TYPES)


class OptionalProvider(BaseProvider):
    def optional(self, val):
        return self.generator.random.choice([val, None])


class AdvStoreProvider(
    LoremProvider,
    EnumFakerProvider,
    JsonFakerProvider,
    BarcodeProvider,
    OptionalProvider,
    PhoneProvider,
    InternetProvider,
    PythonProvider,
    DateTimeProvider,
):
    def u64(self):
        return self.random_int(0, 1 << 64 - 1)

    def int32(self):
        return self.random_int(min=-(2 ** 31), max=2 ** 31 - 1)

    def probability(self):
        # todo (shpak-vadim): have Decimal rounding issue
        #   for random in this place, so temporary replace by const
        #   issue is unstable and hard to reproduce (only by multiple test runs)
        #   it's needed to research
        #   issue was catched in this test:
        #   adv_store/tests/domain/test_create_campaign_domain.py::test_domain_create_campaign
        # noqa: E501
        return decimal.Decimal("0.051751")
        # prob = self.generator.random.random()
        # return decimal.Context(prec=6).create_decimal_from_float(prob)

    def permalink(self):
        return self.random_int(1, 1 << 64 - 1)

    def permalinks(self, n=10):
        return [self.permalink() for _ in range(n)]

    def action_title(self):
        return self.optional(self.sentence())

    def download_app_from_domain(self):
        return {
            "title": self.action_title(),
            "google_play_id": self.ean(),
            "app_store_id": self.ean(),
            "url": self.url(),
            "main": False,
        }

    def phone_call_from_domain(self):
        return {
            "title": self.action_title(),
            "phone": self.phone_number(),
            "main": False,
        }

    def search_from_domain(self):
        return {
            "title": self.action_title(),
            "organizations": self.permalinks(),
            "history_text": self.sentence(),
            "main": False,
        }

    def open_site_from_domain(self):
        return {"title": self.action_title(), "url": self.url(), "main": False}

    def actions_from_domain(self, types=None):
        actions = []
        options = {
            "download_app": self.download_app_from_domain,
            "phone_call": self.phone_call_from_domain,
            "search": self.search_from_domain,
            "open_site": self.open_site_from_domain,
        }

        if types is None:
            for action_name, action_faker in options.items():
                actions.append({action_name: action_faker()})
        else:
            for type_ in types:
                actions.append({type_: options[type_]()})
        return actions

    def image_from_domain(self):
        return {
            "type": self.sentence(),
            "group_id": self.sentence(),
            "image_name": self.sentence(),
            "alias_template": self.sentence(),
            "metadata": self.json_obj(),
        }

    def images(self, num=2):
        return [self.image_from_domain() for _ in range(num)]

    def pin_from_domain(self):
        return {
            "images": self.images(),
            "title": self.sentence(),
            "subtitle": self.sentence(),
        }

    def billboard_from_domain(self):
        return {"images": self.images()}

    def logo_and_text_from_domain(self):
        return {"images": self.images(), "text": self.paragraph()}

    def banner_from_domain(self):
        return {
            "images": self.images(),
            "disclaimer": self.paragraph(),
            "show_ads_label": self.pybool(),
            "title": self.paragraph(),
            "description": self.paragraph(),
            "terms": self.paragraph(),
        }

    def icon_from_domain(self):
        return {
            "images": self.images(),
            "title": self.paragraph(),
            "position": self.int32(),
        }

    def pin_search_from_domain(self):
        return {
            "images": self.images(),
            "title": self.paragraph(),
            "organizations": self.permalinks(),
        }

    def text_from_domain(self):
        return {"text": self.paragraph(), "disclaimer": self.paragraph()}

    def via_point_domain(self):
        return {
            "images": self.images(),
            "button_text_active": self.paragraph(),
            "button_text_inactive": self.paragraph(),
            "description": self.paragraph(),
        }

    def creatives_from_domain(self, types=None):
        creatives = []
        options = {
            "pin": self.pin_from_domain,
            "logo_and_text": self.logo_and_text_from_domain,
            "banner": self.banner_from_domain,
            "billboard": self.billboard_from_domain,
            "icon": self.icon_from_domain,
            "pin_search": self.pin_search_from_domain,
            "text": self.text_from_domain,
            "via_point": self.via_point_domain,
        }

        if types is None:
            for creative_name, creative_faker in options.items():
                creatives.append({creative_name: creative_faker()})
        else:
            for type_ in types:
                creatives.append({type_: options[type_]()})
        return creatives

    def organizations_from_domain(self):
        return {"permalinks": self.permalinks()}

    def point_from_domain(self):
        return {"lon": self.pyfloat(), "lat": self.pyfloat()}

    def polygon_from_domain(self, num=10):
        points = []
        for _ in range(num):
            points.append(self.point_from_domain())
        return {"points": points, "name": self.paragraph()}

    def area_from_domain(self, num=2):
        areas = []
        for _ in range(num):
            areas.append(self.polygon_from_domain())
        return {"areas": areas, "version": self.random_int(1, 10)}

    def week_schedule_from_domain(self, num=2):
        schedules = []
        min_val = 0
        max_val = 7 * 24 * 60 - 1

        distinct_values = set()
        while len(distinct_values) != num * 2:
            distinct_values.add(self.random_int(min_val, max_val))

        times = sorted(distinct_values)
        for start, end in zip(times[::2], times[1::2]):
            schedules.append({"start": start, "end": end})
        return schedules

    def money(self):
        return self.pydecimal(min_value=1, right_digits=4)

    def fix_from_domain(self):
        return {
            "time_interval": self.enum(enums.FixTimeIntervalEnum),
            "cost": self.money(),
        }

    def cpm_from_domain(self):
        return {
            "cost": self.money(),
            "budget": self.optional(self.money()),
            "daily_budget": self.optional(self.money()),
        }

    def cpa_from_domain(self):
        return {
            "cost": self.money(),
            "budget": self.optional(self.money()),
            "daily_budget": self.optional(self.money()),
        }

    def billing_from_domain(self, with_none=False, billing_type=None):
        billing = dict()
        options = {
            "fix": self.fix_from_domain,
            "cpm": self.cpm_from_domain,
            "cpa": self.cpa_from_domain,
        }
        if billing_type is None:
            billing_type = self.generator.random.choice(list(options.keys()))

        billing[billing_type] = options[billing_type]()

        if with_none:
            none_keys = dict()
            for k in (k for k in options.keys() if k != billing_type):
                none_keys[k] = None
            billing.update(none_keys)

        return billing

    def placing_from_domain(self, type_=None):
        options = {
            "organizations": self.organizations_from_domain,
            "area": self.area_from_domain,
        }
        if type_ is None:
            type_ = self.generator.random.choice(list(options.keys()))

        return {type_: options[type_]()}

    def targeting_from_domain(self):
        return {
            "not": False,
            "tag": "or",
            "items": [
                {
                    "not": False,
                    "tag": "and",
                    "items": [
                        {"not": False, "tag": "age", "content": ["18-24", "25-34"]},
                        {"not": False, "tag": "gender", "content": "male"},
                        {
                            "not": True,
                            "tag": "audience",
                            "attributes": {"id": "111222333"},
                        },
                    ],
                },
                {
                    "not": False,
                    "tag": "and",
                    "items": [
                        {"not": False, "tag": "gender", "content": ["female"]},
                        {
                            "not": False,
                            "tag": "or",
                            "items": [
                                {
                                    "not": False,
                                    "tag": "segment",
                                    "attributes": {"id": "111", "keywordId": "601"},
                                },
                                {
                                    "not": False,
                                    "tag": "segment",
                                    "attributes": {"id": "222", "keywordId": "602"},
                                },
                            ],
                        },
                    ],
                },
            ],
        }

    def campaign_from_domain(
        self, campaign_type=None, billing_type=None, with_probabilities=True
    ):
        campaign_type = campaign_type or self.enum(enums.CampaignTypeEnum)
        creative_options = {
            enums.CampaignTypeEnum.ROUTE_BANNER: partial(
                self.creatives_from_domain, types=["banner"]
            ),
            enums.CampaignTypeEnum.PROMOCODE: partial(
                self.creatives_from_domain, types=["banner"]
            ),
            enums.CampaignTypeEnum.CATEGORY_SEARCH: partial(
                self.creatives_from_domain, types=["icon", "pin_search", "text"]
            ),
            enums.CampaignTypeEnum.ZERO_SPEED_BANNER: partial(
                self.creatives_from_domain, types=["banner"]
            ),
            enums.CampaignTypeEnum.OVERVIEW_BANNER: partial(
                self.creatives_from_domain, types=["banner"]
            ),
            enums.CampaignTypeEnum.VIA_POINTS: partial(
                self.creatives_from_domain, types=["via_point"]
            ),
            enums.CampaignTypeEnum.BILLBOARD: partial(
                self.creatives_from_domain,
                types=[
                    "billboard",
                    self.generator.random.choice(["logo_and_text", "banner"]),
                ],
            ),
            enums.CampaignTypeEnum.PIN_ON_ROUTE: partial(
                self.creatives_from_domain,
                types=[
                    "pin",
                    self.generator.random.choice(["logo_and_text", "banner"]),
                ],
            ),
        }

        data = {
            "author_id": self.u64(),
            "name": self.sentence(),
            "publication_envs": self.enum_list(enums.PublicationEnvEnum),
            "status": self.get_random_campaign_status(),
            "campaign_type": campaign_type,
            "start_datetime": self.past_datetime(tzinfo=timezone.utc),
            "end_datetime": self.future_datetime(tzinfo=timezone.utc),
            "timezone": self.timezone(),
            "billing": self.billing_from_domain(billing_type=billing_type),
            "placing": self.placing_from_domain(),
            "platforms": self.enum_list(enums.PlatformEnum),
            "creatives": creative_options[campaign_type](),
            "actions": self.actions_from_domain(),
            "week_schedule": self.week_schedule_from_domain(),
            "order_id": self.optional(self.u64()),
            "manul_order_id": None,
            "comment": self.sentence(),
            "user_display_limit": self.optional(self.random_int()),
            "user_daily_display_limit": self.optional(self.random_int()),
            "targeting": self.targeting_from_domain(),
            "rubric": self.optional(self.enum(enums.RubricEnum)),
        }
        if with_probabilities:
            data["display_probability"] = self.probability()
            data["display_probability_auto"] = self.probability()
        return data

    def get_random_campaign_status(self):
        return self.enum(enums.CampaignStatusEnum)

    def get_random_campaign_status_except(self, not_acceptable):
        return self.generator.random.choice(
            [s for s in enums.CampaignStatusEnum if s is not not_acceptable]
        )

    @staticmethod
    def prototify_images(images):
        return [
            creative_pb2.Image(
                type=image["type"],
                group_id=image["group_id"],
                image_name=image["image_name"],
                alias_template=image["alias_template"],
                metadata=json.dumps(image["metadata"]),
            )
            for image in images
        ]

    @staticmethod
    def prototify_enum(name, val):
        if isinstance(val, (tuple, list)):
            return [ENUM_DB_TO_PB[name][i] for i in val]
        return ENUM_DB_TO_PB[name][val]

    @staticmethod
    def prototify_money(val):
        return billing_pb2.Money(value=int(val * 4)) if val is not None else val

    @staticmethod
    def prototify_dt(val):
        seconds, micros = map(int, f"{val.timestamp():.6f}".split("."))
        nanos = micros * 1000
        return timestamp_pb2.Timestamp(seconds=seconds, nanos=nanos)

    def pin_proto(self):
        data = self.pin_from_domain()
        return creative_pb2.Creative(
            pin=creative_pb2.Pin(
                images=self.prototify_images(data["images"]),
                title=data["title"],
                subtitle=data["subtitle"],
            )
        )

    def banner_proto(self):
        data = self.banner_from_domain()
        return creative_pb2.Creative(
            banner=creative_pb2.Banner(
                images=self.prototify_images(data["images"]),
                disclaimer=data["disclaimer"],
                show_ads_label=data["show_ads_label"],
                title=data["title"],
                description=data["description"],
                terms=data["terms"],
            )
        )

    def billboard_proto(self):
        data = self.billboard_from_domain()
        return creative_pb2.Creative(
            billboard=creative_pb2.Billboard(
                images=self.prototify_images(data["images"])
            )
        )

    def icon_proto(self):
        data = self.icon_from_domain()
        return creative_pb2.Creative(
            icon=creative_pb2.Icon(
                images=self.prototify_images(data["images"]),
                title=data["title"],
                position=data["position"],
            )
        )

    def pin_search_proto(self):
        data = self.pin_search_from_domain()

        return creative_pb2.Creative(
            pin_search=creative_pb2.PinSearch(
                images=self.prototify_images(data["images"]),
                title=data["title"],
                organizations=data["organizations"],
            )
        )

    def text_proto(self):
        data = self.text_from_domain()
        return creative_pb2.Creative(
            text=creative_pb2.Text(text=data["text"], disclaimer=data["disclaimer"])
        )

    def via_point_proto(self):
        data = self.via_point_domain()
        return creative_pb2.Creative(
            via_point=creative_pb2.ViaPoint(
                images=self.prototify_images(data["images"]),
                button_text_active=data["button_text_active"],
                button_text_inactive=data["button_text_inactive"],
                description=data["description"],
            )
        )

    def creatives_proto(self, types=None):
        creatives = []
        options = {
            "pin": self.pin_proto,
            "banner": self.banner_proto,
            "billboard": self.billboard_proto,
            "icon": self.icon_proto,
            "pin_search": self.pin_search_proto,
            "text": self.text_proto,
            "via_point": self.via_point_proto,
        }
        if types is None:
            for creative_faker in options.values():
                creatives.append(creative_faker())
        else:
            for type_ in types:
                creatives.append(options[type_]())
        return creatives

    def campaign_proto(self, *args, **kwargs):
        data = self.campaign_proto_data(*args, **kwargs)
        return campaign_pb2.CampaignData(**data)

    def campaign_proto_data(
        self,
        *,
        campaign_type=None,
        billing_type=None,
        publication_envs=None,
        platforms=None,
        status=None,
        placing=None,
        rubric=None,
    ):
        campaign_type = campaign_type or self.enum(enums.CampaignTypeEnum)
        creative_options = {
            enums.CampaignTypeEnum.ROUTE_BANNER: partial(
                self.creatives_proto, types=["banner"]
            ),
            enums.CampaignTypeEnum.PROMOCODE: partial(
                self.creatives_from_domain, types=["banner"]
            ),
            enums.CampaignTypeEnum.CATEGORY_SEARCH: partial(
                self.creatives_proto, types=["icon", "pin_search", "text"]
            ),
            enums.CampaignTypeEnum.ZERO_SPEED_BANNER: partial(
                self.creatives_proto, types=["banner"]
            ),
            enums.CampaignTypeEnum.OVERVIEW_BANNER: partial(
                self.creatives_proto, types=["banner"]
            ),
            enums.CampaignTypeEnum.BILLBOARD: partial(
                self.creatives_proto, types=["billboard", "banner"]
            ),
            enums.CampaignTypeEnum.PIN_ON_ROUTE: partial(
                self.creatives_proto, types=["pin", "banner"]
            ),
            enums.CampaignTypeEnum.VIA_POINTS: partial(
                self.creatives_proto, types=["via_point"]
            ),
        }
        data = self.campaign_from_domain(campaign_type=campaign_type)
        publication_envs = publication_envs or data["publication_envs"]
        platforms = platforms or data["platforms"]
        status = status or data["status"]
        rubric = rubric or data["rubric"]

        return dict(
            author_id=data["author_id"],
            name=data["name"],
            publication_envs=self.prototify_enum("publication_envs", publication_envs),
            status=self.prototify_enum("status", status),
            campaign_type=self.prototify_enum("campaign_type", data["campaign_type"]),
            start_datetime=self.prototify_dt(data["start_datetime"]),
            end_datetime=self.prototify_dt(data["end_datetime"]),
            timezone=data["timezone"],
            billing=self.billing_proto(billing_type),
            placing=self.placing_from_domain(type_=placing),
            platforms=self.prototify_enum("platforms", platforms),
            creatives=creative_options[campaign_type](),
            actions=self.actions_proto(),
            week_schedule=self.week_schedule_proto(),
            order_id=data["order_id"],
            # don't setup multiple fields from the same oneof section.
            # Only one will be really stored in protobuf!
            # manul_order_id=data.get('manul_order_id', 0),
            comment=data["comment"],
            user_display_limit=data["user_display_limit"],
            user_daily_display_limit=data["user_daily_display_limit"],
            targeting=json.dumps(data["targeting"]),
            rubric=self.prototify_enum("rubric", rubric) if rubric else None,
        )

    def billing_proto(self, billing_type=None):
        data = self.billing_from_domain(billing_type=billing_type)
        if "fix" in data:
            return billing_pb2.Billing(
                fix=billing_pb2.Fix(
                    time_interval=self.prototify_enum(
                        "fix_time_interval", data["fix"]["time_interval"]
                    ),
                    cost=self.prototify_money(data["fix"]["cost"]),
                )
            )
        elif "cpm" in data:
            return billing_pb2.Billing(
                cpm=billing_pb2.Cpm(
                    cost=self.prototify_money(data["cpm"]["cost"]),
                    budget=self.prototify_money(data["cpm"]["budget"]),
                    daily_budget=self.prototify_money(data["cpm"]["daily_budget"]),
                )
            )
        elif "cpa" in data:
            return billing_pb2.Billing(
                cpa=billing_pb2.Cpa(
                    cost=self.prototify_money(data["cpa"]["cost"]),
                    budget=self.prototify_money(data["cpa"]["budget"]),
                    daily_budget=self.prototify_money(data["cpa"]["daily_budget"]),
                )
            )
        raise KeyError("cpm/fix/cpa, give me one")

    def placing_proto(self, type_=None):
        data = self.placing_from_domain(type_)
        if "area" in data:
            areas = []
            for polygon in data["area"]["areas"]:
                points = [
                    placing_pb2.Point(lon=p["lon"], lat=p["lat"])
                    for p in polygon["points"]
                ]
                areas.append(placing_pb2.Polygon(points=points))
            return placing_pb2.Placing(
                area=placing_pb2.Area(areas=areas, version=data["area"]["version"])
            )
        elif "organizations" in data:
            return placing_pb2.Placing(
                organizations=placing_pb2.Organizations(
                    permalinks=data["organizations"]["permalinks"]
                )
            )
        raise KeyError("area/organizations, pick one")

    def actions_proto(self, types=None):
        actions = []
        options = {
            "download_app": self.download_app_proto,
            "phone_call": self.phone_call_proto,
            "search": self.search_proto,
            "open_site": self.open_site_proto,
        }

        if types is None:
            for action_faker in options.values():
                actions.append(action_faker())
        else:
            for type_ in types:
                actions.append(options[type_]())
        return actions

    def download_app_proto(self):
        data = self.download_app_from_domain()
        return action_pb2.Action(
            download_app=action_pb2.DownloadApp(
                google_play_id=data["google_play_id"],
                app_store_id=data["app_store_id"],
                url=data["url"],
            ),
            title=data["title"],
        )

    def phone_call_proto(self):
        data = self.phone_call_from_domain()
        return action_pb2.Action(
            phone_call=action_pb2.PhoneCall(phone=data["phone"]), title=data["title"]
        )

    def search_proto(self):
        data = self.search_from_domain()
        return action_pb2.Action(
            search=action_pb2.Search(
                organizations=data["organizations"], history_text=data["history_text"]
            ),
            title=data["title"],
        )

    def open_site_proto(self):
        data = self.open_site_from_domain()
        return action_pb2.Action(
            open_site=action_pb2.OpenSite(url=data["url"]), title=data["title"]
        )

    def week_schedule_proto(self, num=2):
        data = self.week_schedule_from_domain(num)
        return [
            campaign_pb2.WeekScheduleItem(start=sch["start"], end=sch["end"])
            for sch in data
        ]

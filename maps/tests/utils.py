import json
import time
from datetime import datetime, timezone
from typing import List, Optional, Union

from google.protobuf.timestamp_pb2 import Timestamp

from maps_adv.adv_store.api.proto import action_pb2, billing_pb2, creative_pb2, placing_pb2
from maps_adv.adv_store.tests.faker_provider import ENUM_DB_TO_PB


def time_interval_db_to_pb(db_enum):
    return ENUM_DB_TO_PB["fix_time_interval"][db_enum]


def publication_envs_db_to_pb(db_enum):
    return ENUM_DB_TO_PB["publication_envs"][db_enum]


def status_db_to_pb(db_enum):
    return ENUM_DB_TO_PB["status"][db_enum]


def campaign_type_db_to_pb(db_enum):
    return ENUM_DB_TO_PB["campaign_type"][db_enum]


def platform_db_to_pb(db_enum):
    return ENUM_DB_TO_PB["platforms"][db_enum]


def modertationresolution_db_to_pb(db_enum):
    return ENUM_DB_TO_PB["moderation_resolution"][db_enum]


def pb_datetime(dt):
    ts = dt.timestamp()
    return Timestamp(
        seconds=int(ts // 1), nanos=int(ts * 1_000_000 - int(ts) * 1_000_000) * 1_000
    )


def pb_money(val):
    return billing_pb2.Money(value=int(val * 10_000)) if val else None


def pin_json_to_proto(creative_data):
    return creative_pb2.Creative(
        pin=creative_pb2.Pin(
            images=[
                creative_pb2.Image(
                    type=i["type"],
                    group_id=i["group_id"],
                    image_name=i["image_name"],
                    alias_template=i["alias_template"],
                    metadata=json.dumps(i["metadata"]),
                )
                for i in creative_data["images"]
            ],
            title=creative_data["title"],
            subtitle=creative_data["subtitle"],
        )
    )


def billboard_json_to_proto(creative_data):
    return creative_pb2.Creative(
        billboard=creative_pb2.Billboard(
            images=[
                creative_pb2.Image(
                    type=i["type"],
                    group_id=i["group_id"],
                    image_name=i["image_name"],
                    alias_template=i["alias_template"],
                    metadata=json.dumps(i["metadata"]),
                )
                for i in creative_data["images"]
            ]
        )
    )


def logo_and_text_to_proto(creative_data):
    return creative_pb2.Creative(
        logo_and_text=creative_pb2.LogoAndText(
            images=[
                creative_pb2.Image(
                    type=i["type"],
                    group_id=i["group_id"],
                    image_name=i["image_name"],
                    alias_template=i["alias_template"],
                    metadata=json.dumps(i["metadata"]),
                )
                for i in creative_data["images"]
            ],
            text=creative_data["text"],
        )
    )


def banner_json_to_proto(creative_data):
    return creative_pb2.Creative(
        banner=creative_pb2.Banner(
            images=[
                creative_pb2.Image(
                    type=i["type"],
                    group_id=i["group_id"],
                    image_name=i["image_name"],
                    alias_template=i["alias_template"],
                    metadata=json.dumps(i["metadata"]),
                )
                for i in creative_data["images"]
            ],
            disclaimer=creative_data["disclaimer"],
            show_ads_label=creative_data["show_ads_label"],
            title=creative_data["title"],
            description=creative_data["description"],
            terms=creative_data["terms"],
        )
    )


def icon_json_to_proto(creative_data):
    return creative_pb2.Creative(
        icon=creative_pb2.Icon(
            images=[
                creative_pb2.Image(
                    type=i["type"],
                    group_id=i["group_id"],
                    image_name=i["image_name"],
                    alias_template=i["alias_template"],
                    metadata=json.dumps(i["metadata"]),
                )
                for i in creative_data["images"]
            ],
            title=creative_data["title"],
            position=creative_data["position"],
        )
    )


def pin_search_json_to_proto(creative_data):
    return creative_pb2.Creative(
        pin_search=creative_pb2.PinSearch(
            images=[
                creative_pb2.Image(
                    type=i["type"],
                    group_id=i["group_id"],
                    image_name=i["image_name"],
                    alias_template=i["alias_template"],
                    metadata=json.dumps(i["metadata"]),
                )
                for i in creative_data["images"]
            ],
            title=creative_data["title"],
            organizations=creative_data["organizations"],
        )
    )


def text_json_to_proto(creative_data):
    return creative_pb2.Creative(
        text=creative_pb2.Text(
            text=creative_data["text"], disclaimer=creative_data["disclaimer"]
        )
    )


def via_point_json_to_proto(creative_data):
    return creative_pb2.Creative(
        via_point=creative_pb2.ViaPoint(
            images=[
                creative_pb2.Image(
                    type=i["type"],
                    group_id=i["group_id"],
                    image_name=i["image_name"],
                    alias_template=i["alias_template"],
                    metadata=json.dumps(i["metadata"]),
                )
                for i in creative_data["images"]
            ],
            button_text_active=creative_data["button_text_active"],
            button_text_inactive=creative_data["button_text_inactive"],
            description=creative_data["description"],
        )
    )


def creative_json_to_proto(creative_data, type_):
    if type_ == "pin":
        return pin_json_to_proto(creative_data)
    elif type_ == "billboard":
        return billboard_json_to_proto(creative_data)
    elif type_ == "logo_and_text":
        return logo_and_text_to_proto(creative_data)
    elif type_ == "banner":
        return banner_json_to_proto(creative_data)
    elif type_ == "icon":
        return icon_json_to_proto(creative_data)
    elif type_ == "pin_search":
        return pin_search_json_to_proto(creative_data)
    elif type_ == "text":
        return text_json_to_proto(creative_data)
    elif type_ == "via_point":
        return via_point_json_to_proto(creative_data)
    else:
        raise RuntimeError("Unknown creative type")


def download_app_json_to_proto(action_data):
    return action_pb2.Action(
        download_app=action_pb2.DownloadApp(
            google_play_id=action_data["google_play_id"],
            app_store_id=action_data["app_store_id"],
            url=action_data["url"],
        ),
        title=action_data["title"],
    )


def open_site_json_to_proto(action_data):
    return action_pb2.Action(
        open_site=action_pb2.OpenSite(url=action_data["url"]),
        title=action_data["title"],
    )


def phone_call_json_to_proto(action_data):
    return action_pb2.Action(
        phone_call=action_pb2.PhoneCall(phone=action_data["phone"]),
        title=action_data["title"],
    )


def search_json_to_proto(action_data):
    return action_pb2.Action(
        search=action_pb2.Search(
            organizations=[o for o in action_data["organizations"]],
            history_text=action_data["history_text"],
        ),
        title=action_data["title"],
    )


def action_json_to_proto(action_data, type_):
    if type_ == "download_app":
        return download_app_json_to_proto(action_data)
    elif type_ == "open_site":
        return open_site_json_to_proto(action_data)
    elif type_ == "phone_call":
        return phone_call_json_to_proto(action_data)
    elif type_ == "search":
        return search_json_to_proto(action_data)
    else:
        raise RuntimeError("Unknown action type")


def placing_json_to_proto(placing_data):
    if len(placing_data.keys()) != 1:
        raise RuntimeError("Only one option for placing can be chosen")
    if "organizations" in placing_data:
        orgs = placing_data["organizations"]
        return placing_pb2.Placing(
            organizations=placing_pb2.Organizations(
                permalinks=[p for p in orgs["permalinks"]]
            )
        )
    elif "area" in placing_data:
        area = placing_data["area"]
        return placing_pb2.Placing(
            area=placing_pb2.Area(
                version=area["version"],
                areas=[
                    placing_pb2.Polygon(
                        points=[
                            placing_pb2.Point(lon=p["lon"], lat=p["lat"])
                            for p in polygon["points"]
                        ],
                        name=polygon["name"],
                    )
                    for polygon in area["areas"]
                ],
            )
        )
    else:
        raise RuntimeError("Unknown placing type")


def normalisation_images_in_creative(creative):
    for _, field in creative.ListFields():
        if hasattr(field, "images"):
            for image in field.images:
                image.metadata = json.dumps(json.loads(image.metadata), sort_keys=True)
    return creative


class AiohttpClientResponseMock:
    def __init__(self, body=b""):
        self._body = body

    async def read(self):
        return self._body


class ContextManagerMock:
    def __init__(self, return_value):
        self._return_value = return_value

    async def __aenter__(self):
        return self._return_value

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        pass


class Any:
    def __init__(self, _type):
        self._type = _type

    def __eq__(self, another):
        return isinstance(another, self._type)


def dt(value: Union[int, str], tz: Optional[timezone] = timezone.utc) -> datetime:
    return datetime(*time.strptime(value, "%Y-%m-%d %H:%M:%S")[:6], tzinfo=tz)


def sorted_pb_objects(protobuf_list) -> List:
    return sorted(protobuf_list, key=lambda item: item.SerializeToString())

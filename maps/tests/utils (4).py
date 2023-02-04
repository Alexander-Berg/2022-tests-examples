from typing import Iterable, List

from maps_adv.geosmb.doorman.proto.common_pb2 import (
    ClientGender as ClientGenderPb,
    Source as SourcePb,
)
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import (
    ClientGender as ClientGenderEnum,
    SegmentType as SegmentTypeEnum,
    Source as SourceEnum,
)

ENUM_MAPS_TO_PB = {
    "source": {
        SourceEnum.CRM_INTERFACE: SourcePb.CRM_INTERFACE,
        SourceEnum.BOOKING_YANG: SourcePb.BOOKING_YANG,
        SourceEnum.GEOADV_PHONE_CALL: SourcePb.GEOADV_PHONE_CALL,
        SourceEnum.LOYALTY_COUPONS: SourcePb.LOYALTY_COUPONS,
        SourceEnum.BOOKING_WIDGET: SourcePb.BOOKING_WIDGET,
        SourceEnum.BOOKING_REQUEST: SourcePb.BOOKING_REQUEST
    },
    "client_gender": {
        ClientGenderEnum.MALE: ClientGenderPb.MALE,
        ClientGenderEnum.FEMALE: ClientGenderPb.FEMALE,
    },
    "segment_type": {
        SegmentTypeEnum.REGULAR: SegmentTypePb.REGULAR,
        SegmentTypeEnum.ACTIVE: SegmentTypePb.ACTIVE,
        SegmentTypeEnum.LOST: SegmentTypePb.LOST,
        SegmentTypeEnum.UNPROCESSED_ORDERS: SegmentTypePb.UNPROCESSED_ORDERS,
        SegmentTypeEnum.NO_ORDERS: SegmentTypePb.NO_ORDERS,
        SegmentTypeEnum.SHORT_LAST_CALL: SegmentTypePb.SHORT_LAST_CALL,
        SegmentTypeEnum.MISSED_LAST_CALL: SegmentTypePb.MISSED_LAST_CALL,
    },
}


def extract_ids(data: Iterable) -> List[int]:
    return [row["id"] if isinstance(row, dict) else row.id for row in data]


def make_ivan(**kwargs) -> dict:
    params = dict(
        first_name="Иван",
        last_name="Волков",
        phone="1111111111",
        email="ivan@yandex.ru",
        comment="ivan comment",
    )
    params.update(kwargs)

    return params


def make_alex(**kwargs) -> dict:
    params = dict(
        first_name="Алекс",
        last_name="Зайцев",
        phone="2222222222",
        email="alex@yandex.ru",
        comment="alex comment",
    )
    params.update(kwargs)

    return params


def make_fedor(**kwargs) -> dict:
    params = dict(
        first_name="Фёдор",
        last_name="Лисицын",
        phone="3333333333",
        email="fedor@yandex.ru",
        comment="fedor comment",
    )
    params.update(kwargs)

    return params

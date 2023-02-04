import random
import urllib.parse
from asyncio import coroutine
from typing import Union
from unittest.mock import Mock

from google.protobuf import timestamp_pb2

from maps_adv.billing_proxy.lib.db.enums import (
    BillingType,
    CampaignType,
    CurrencyType,
    FixTimeIntervalType,
    PaymentType,
    PlatformType,
)
from maps_adv.billing_proxy.proto import common_pb2, products_pb2


def get_another_enum_member(member):
    """Returns another member from enum argument belongs to"""
    enum_class = type(member)
    other_values = list(m for m in enum_class if m is not member)

    if len(other_values) == 0:
        raise Exception("Enum must have at least 2 members")

    return random.choice(other_values)


def coro_mock():
    coro = Mock(name="CoroutineResult")
    corofunc = Mock(name="CoroutineFunction", side_effect=coroutine(coro))
    corofunc.coro = coro
    return corofunc


def urljoin(*fragments: Union[str, int], append_slash: bool = True) -> str:
    result = ""
    for fragment in fragments:
        if not result.endswith("/"):
            result += "/"
        result = urllib.parse.urljoin(result, str(fragment))

    if append_slash and not result.endswith("/"):
        result += "/"

    return result


class Any:
    def __init__(self, _type):
        self._type = _type

    def __eq__(self, another):
        return isinstance(another, self._type)


def true_for_all_orders(orders, _):
    services = set()
    for value in orders.values():
        services.add(value["service_id"])

    result = {}
    for service_id in services:
        order_ids = [
            order_id
            for order_id, value in orders.items()
            if value["service_id"] == service_id
        ]
        result.update({service_id: {order_id: True for order_id in order_ids}})

    return result


def mock_find_by_uid_clients(uid: int):
    if uid == 10001:
        return {
            "id": 55,
            "name": "Имя клиента",
            "email": "email@example.com",
            "phone": "8(499)123-45-67",
            "is_agency": False,
            "partner_agency_id": 1,
        }
    elif uid == 10002:
        return {
            "id": 56,
            "name": "Имя клиента2",
            "email": "email2@example.com",
            "phone": "8(499)223-45-67",
            "is_agency": True,
            "partner_agency_id": 2,
        }
    elif uid == 10003:
        return {
            "id": 56,
            "name": "Имя клиента3",
            "email": "email3@example.com",
            "phone": "8(499)323-45-67",
            "is_agency": True,
            "partner_agency_id": 3,
        }
    return None


def get_user_ticket(uid: int):
    if uid == 10001:
        return (
            "3:user:CA0Q__________9_GhAKAwiRThCRTiDShdjMBCgB:LoVpjo_D9DNqjrG1Ds1R242le"
            "MkwY29R_8MYUe5okb0nngWcpp-hWBWHE9FeX0-ToemZDY7JKNhElqDsvbI_P48awczo1aFT3V"
            "2lQczMuD9eJAvWCwqxRCD8AWcAePpLl0QzbJJAwgED0NKbzeqsda0jwInbI5176vhaGzISnZo"
        )
    elif uid == 10003:
        return (
            "3:user:CA0Q__________9_GhAKAwiTThCTTiDShdjMBCgB:HsdlwfGiBCOQAqw-O7-4FpLFO"
            "95uKoSFBA9gYu2Xc4ypTEIKbfzuvabmY8JWxIjgtqAqAaNYveSpEOYkpydMI6FPnHRDD7I1wF"
            "0xWLmmS0WOTgB9Qo7N_E7SlTg8RZoLAUJHu6IlIut2nyPoCsfSqM-RAgFz2y-aKQYUDIBKacI"
        )
    elif uid == 123:
        return (
            "3:user:CA0Q__________9_Gg4KAgh7"
            "EHsg0oXYzAQoAQ:AT8fQJuO8xqkDiI2"
            "--TlTk3WeVUqL0CEuzRVHrDh4wlOCIo"
            "imfqjXeE8FJuFl_HS7Q3qoLVOSRarB5"
            "B0yAMxeOWJynNZcRvbeiYgA5S6R6q6-"
            "1cCBjqPZK4_Wlyoc5xAaNRiH9wH6_F_"
            "GT7SZriSTMmq7AqipgZeKa2ja6jki9U"
        )


_enum_map = {
    BillingType.CPM: common_pb2.BillingType.Value("CPM"),
    BillingType.FIX: common_pb2.BillingType.Value("FIX"),
    FixTimeIntervalType.DAILY: products_pb2.Fix.TimeIntervalType.Value("DAILY"),
    FixTimeIntervalType.WEEKLY: products_pb2.Fix.TimeIntervalType.Value("WEEKLY"),
    FixTimeIntervalType.MONTHLY: products_pb2.Fix.TimeIntervalType.Value("MONTHLY"),
    CampaignType.PIN_ON_ROUTE: common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
    CampaignType.BILLBOARD: common_pb2.CampaignType.Value("BILLBOARD"),
    CampaignType.ZERO_SPEED_BANNER: common_pb2.CampaignType.Value("ZERO_SPEED_BANNER"),
    CampaignType.CATEGORY_SEARCH_PIN: common_pb2.CampaignType.Value(
        "CATEGORY_SEARCH_PIN"
    ),
    CampaignType.ROUTE_BANNER: common_pb2.CampaignType.Value("ROUTE_BANNER"),
    CampaignType.VIA_POINTS: common_pb2.CampaignType.Value("VIA_POINTS"),
    CurrencyType.RUB: common_pb2.CurrencyType.Value("RUB"),
    CurrencyType.BYN: common_pb2.CurrencyType.Value("BYN"),
    CurrencyType.TRY: common_pb2.CurrencyType.Value("TRY"),
    CurrencyType.KZT: common_pb2.CurrencyType.Value("KZT"),
    CurrencyType.EUR: common_pb2.CurrencyType.Value("EUR"),
    CurrencyType.USD: common_pb2.CurrencyType.Value("USD"),
    PaymentType.PRE: common_pb2.PaymentType.Value("PRE_PAYMENT"),
    PaymentType.POST: common_pb2.PaymentType.Value("POST_PAYMENT"),
    PlatformType.MAPS: common_pb2.PlatformType.Value("MAPS"),
    PlatformType.METRO: common_pb2.PlatformType.Value("METRO"),
    PlatformType.NAVI: common_pb2.PlatformType.Value("NAVI"),
}


def convert_internal_enum_to_proto(internal_enum_value):
    if isinstance(internal_enum_value, list):
        return list(map(convert_internal_enum_to_proto, internal_enum_value))
    else:
        return _enum_map[internal_enum_value]


def dt_to_proto(dt):
    seconds, micros = map(int, "{:.6f}".format(dt.timestamp()).split("."))
    return timestamp_pb2.Timestamp(seconds=seconds, nanos=micros * 1000)


class AsyncContextManagerMock:
    def __init__(self, retval=None):
        self._retval = retval

    async def __aenter__(self):
        return self._retval

    async def __aexit__(self, exc_type, *args, **kwargs):
        pass

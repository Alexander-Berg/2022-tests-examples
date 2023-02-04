import calendar
import time
from asyncio import coroutine
from datetime import date, datetime, timezone
from decimal import Decimal
from typing import Optional, Union
from unittest.mock import Mock

from google.protobuf import timestamp_pb2


def dt(
    value: Union[int, str], *, as_proto: bool = False
) -> Union[datetime, date, timestamp_pb2.Timestamp]:

    if isinstance(value, int):
        res = datetime.fromtimestamp(value, tz=timezone.utc)
    elif len(value) > 10:
        res = datetime(
            *time.strptime(value, "%Y-%m-%d %H:%M:%S")[:6], tzinfo=timezone.utc
        )
    else:
        res = date(*time.strptime(value, "%Y-%m-%d")[:3])

    if as_proto:
        seconds = (
            calendar.timegm(res.timetuple())
            if isinstance(res, date)
            else int(res.timestamp())
        )
        res = timestamp_pb2.Timestamp(seconds=seconds)

    return res


def dt_timestamp(value: Union[str, int]) -> int:
    return int(dt(value).timestamp())


def coro_mock():
    coro = Mock(name="CoroutineResult")
    corofunc = Mock(name="CoroutineFunction", side_effect=coroutine(coro))
    corofunc.coro = coro
    return corofunc


def make_event(
    campaign_id: int,
    timestamp: int,
    name: Optional[str] = "pin.show",
    cost: Optional[Decimal] = None,
    event_group_id: str = "37b36bd03bf84fcfe8f95ab43191653c",
) -> tuple:
    return (
        datetime.utcfromtimestamp(timestamp),
        campaign_id,
        event_group_id,
        4,
        "4CE92B30-6A33-457D-A7D4-1B8CBAD54597",
        "iOS",
        "1112",
        11476,
        55.718732876522175,
        37.40151579701865,
        f"geoadv.bb.{name}",
    ) + ((cost,) if cost is not None else ())


def make_charged_event(*args):
    return make_event(campaign_id=args[0], timestamp=args[1], cost=args[2])


def setup_normalized_db(ch_client, events_args):
    normalized_existing = [make_event(*args) for args in events_args]
    ch_client.execute("INSERT INTO stat.normalized_sample VALUES", normalized_existing)


def setup_charged_db(ch_client, events_args):
    charged_existing = [make_charged_event(*args) for args in events_args]
    ch_client.execute("INSERT INTO stat.accepted_sample VALUES", charged_existing)


class Any:
    def __init__(self, _type):
        self._type = _type

    def __eq__(self, another):
        return isinstance(another, self._type)


def squash_whitespaces(source: str):
    return " ".join(source.split())

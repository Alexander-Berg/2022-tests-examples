import time
from asyncio import coroutine
from datetime import datetime, timezone
from typing import Optional, Union
from unittest.mock import Mock

from google.protobuf import timestamp_pb2

__all__ = ["Any", "coro_mock", "dt"]


class Any:
    def __init__(self, _type):
        self._type = _type

    def __eq__(self, another):
        return isinstance(another, self._type)


def coro_mock():
    coro = Mock(name="CoroutineResult")
    corofunc = Mock(name="CoroutineFunction", side_effect=coroutine(coro))
    corofunc.coro = coro
    return corofunc


def dt(value: Union[int, str], tz: Optional[timezone] = timezone.utc) -> datetime:
    return datetime(*time.strptime(value, "%Y-%m-%d %H:%M:%S")[:6], tzinfo=tz)


def dt_to_proto(dt):
    seconds, micros = map(int, "{:.6f}".format(dt.timestamp()).split("."))
    return timestamp_pb2.Timestamp(seconds=seconds, nanos=micros * 1000)

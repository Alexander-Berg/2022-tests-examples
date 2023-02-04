import time
from asyncio import coroutine
from datetime import datetime, timezone
from typing import Optional, Union
from unittest.mock import Mock

__all__ = ["Around", "Any", "dt", "coro_mock"]


class Around:
    def __init__(self, around_of, deviation=1):
        self.low = around_of - deviation
        self.high = around_of + deviation

    def __contains__(self, value):
        return self.low < value < self.high


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
    if isinstance(value, int):
        return datetime.fromtimestamp(value, tz=tz)

    return datetime(*time.strptime(value, "%Y-%m-%d %H:%M:%S")[:6], tzinfo=tz)

import time
from datetime import datetime, timezone
from typing import Optional, Union

from google.protobuf import timestamp_pb2

__all__ = ["dt", "dt_to_proto"]


def dt(value: Union[int, str], tz: Optional[timezone] = timezone.utc) -> datetime:
    if isinstance(value, int):
        return datetime.fromtimestamp(value, tz=tz)

    return datetime(*time.strptime(value, "%Y-%m-%d %H:%M:%S")[:6], tzinfo=tz)


def dt_to_proto(dt):
    seconds, micros = map(int, "{:.6f}".format(dt.timestamp()).split("."))
    return timestamp_pb2.Timestamp(seconds=seconds, nanos=micros * 1000)

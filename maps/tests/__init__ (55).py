import time
from datetime import datetime, timezone
from typing import Optional, Union

__all__ = ["dt"]


def dt(value: Union[int, str], tz: Optional[timezone] = timezone.utc) -> datetime:
    if isinstance(value, int):
        return datetime.fromtimestamp(value, tz=tz)

    return datetime(*time.strptime(value, "%Y-%m-%d %H:%M:%S")[:6], tzinfo=tz)

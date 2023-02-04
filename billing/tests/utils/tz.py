import datetime as dt
from typing import Optional

import pytz


def utc_tz():
    return pytz.utc


def to_utc_tz(datetime: dt.datetime) -> Optional[dt.datetime]:
    return datetime.replace(tzinfo=utc_tz()) if datetime else None

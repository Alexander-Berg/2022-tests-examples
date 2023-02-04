from datetime import datetime
from dateutil import tz


def now_timestamp_utc(date: datetime = None):
    if date is None:
        date = datetime.utcnow()
    else:
        date.astimezone(tz.gettz('UTC'))
    return date.timestamp()


def now_dt_second(date: datetime = None):
    if date is None:
        date = datetime.now()
    return round(date.timestamp())


def now_dt_ms(date: datetime = None):
    if date is None:
        date = datetime.now()
    return round(date.timestamp() * 1_000)


def shifted_now_dt_ms(shift_s: int):
    return round((datetime.now().timestamp() + shift_s) * 1_000)

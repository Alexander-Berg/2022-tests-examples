import datetime
from dateutil import relativedelta


def date() -> datetime.datetime:
    return datetime.datetime.now().astimezone()


def date_iso() -> str:
    return date().isoformat()


def shifted_date(**kwargs) -> datetime.datetime:
    return datetime.datetime.now().astimezone() + relativedelta.relativedelta(**kwargs)


def shifted_date_iso(**kwargs) -> str:
    return shifted_date(**kwargs).isoformat()


def date_iso_seconds() -> str:
    return datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')


def shifted_date_iso_seconds(**kwargs) -> str:
    return shifted_date(**kwargs).strftime('%Y-%m-%d %H:%M:%S')

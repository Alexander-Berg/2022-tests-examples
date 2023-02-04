from dateutil.parser import isoparse
from dateutil.tz import gettz
from dateutil.utils import default_tzinfo


def isotime_normalizer(default_tz='Europe/Moscow'):
    tz_info = gettz(default_tz)  # gettz takes roughly x2~8 more time than isoparse

    def to_iso(value):
        dt = default_tzinfo(isoparse(value), tz_info)
        return dt.isoformat(timespec='seconds').encode('utf-8')
    return to_iso

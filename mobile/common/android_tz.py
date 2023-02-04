import re

from operator import itemgetter
from pytz import _FixedOffset, timezone

# according https://developer.android.com/reference/java/util/TimeZone
re_timezone = re.compile(r'''
    ^GMT
    (
        (?P<hours>[ +-]
            (?:
                (?:[01]?[0-9]) | (?:2[0-3])
            )
        )
        (?: [:]? (?P<minutes>[0-5][0-9]) )?
    )?
    $''', re.X)

tm_getter = itemgetter('hours', 'minutes')


class InvalidAndroidTimezone(ValueError):
    pass


class _AndroidTimezone(_FixedOffset):
    def __init__(self, android_timezone):
        match = re_timezone.match(android_timezone)
        if not match:
            raise InvalidAndroidTimezone('Invalid android timezone passed (%s).' % android_timezone)

        hours, minutes = tm_getter(match.groupdict())
        if not hours and not minutes:
            raise InvalidAndroidTimezone('Invalid android timezone passed (%s).' % android_timezone)

        offset, minutes = int(hours) * 60, int(minutes or 0)
        offset += -minutes if offset < 0 else minutes
        if -1440 >= offset >= 1440:
            raise InvalidAndroidTimezone('Invalid android timezone passed (%s).' % android_timezone)

        super(_AndroidTimezone, self).__init__(offset)

    def utcoffset(self, dt, is_dst=None):
        return super(_AndroidTimezone, self).utcoffset(dt)

    def __repr__(self):
        sign = '-' if self._minutes < 0 else '+'
        h, m = divmod(abs(self._minutes), 60)
        return 'GMT%s%02d:%02d' % (sign, h, m)


_tz_cache = {}


def AndroidTimezone(timezone_name):
    try:
        tz = _tz_cache.get(timezone_name)
        if tz is None:
            tz = _tz_cache.setdefault(timezone_name, _AndroidTimezone(timezone_name))
        return tz
    except InvalidAndroidTimezone:
        return timezone(timezone_name)

import datetime
import time

from hamcrest.core.base_matcher import BaseMatcher

from btestlib.utils import Date


def date_ts_msec_after(expected):
    return _IsDateTsMsecAfter(expected)


def date_string_after(expected, _format='%Y-%m-%dT%H:%M:%S+03:00'):
    return _IsDateStringAfter(expected, _format)


class _IsDateStringAfter(BaseMatcher):
    def __init__(self, expected, _format):
        self.expected = expected
        self.format = _format
        self.actual = None

    def _matches(self, item):
        try:
            self.actual = datetime.datetime.strptime(item, self.format)
        except ValueError:
            return False

        return self.expected < self.actual

    def describe_to(self, description):
        description.append_text('date string is after {}'
                                .format(datetime.datetime.strftime(self.expected, self.format)))

    def describe_mismatch(self, item, mismatch_description):
        if not self.actual:
            mismatch_description.append_text("date string '{}' doesn't match to format: '{}'"
                                             .format(item, self.format))
        else:
            super(_IsDateStringAfter, self).describe_mismatch(item, mismatch_description)


class _IsDateTsMsecAfter(BaseMatcher):
    def __init__(self, expected):
        self.expected = expected
        self.actual = None

    def _matches(self, item):
        self.actual = datetime.datetime.fromtimestamp(int(item) / 1000.0)

        return self.expected < self.actual

    def describe_to(self, description):
        description.append_text("date in msecs after '{}'"
                                .format(int(time.mktime(self.expected.timetuple())) * 1000))


def later_on_timedelta_than_(expected, seconds, days, months, years):
    return _IsDateLaterOnTimedelta(expected, seconds, days, months, years)


class _IsDateLaterOnTimedelta(BaseMatcher):
    def __init__(self, expected, seconds=0, days=0, months=0, years=0):
        self.expected = expected
        self.actual = None
        self.seconds = -seconds
        self.days = -days
        self.months = -months
        self.years = -years

    def _matches(self, item):
        self.actual = Date.shift_date(item, years=self.years, months=self.months, days=self.days, seconds=self.seconds)

        return self.expected == self.actual

    def describe_to(self, description):
        description.append_text("date in msecs after '{}'"
                                .format(int(time.mktime(self.expected.timetuple())) * 1000))

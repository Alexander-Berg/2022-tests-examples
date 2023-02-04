from datetime import datetime, timedelta
from typing import Any

from hamcrest.core.base_matcher import BaseMatcher
from hamcrest.core.description import Description


class IsCloseToDateTime(BaseMatcher):
    def __init__(self, value: datetime, delta: timedelta):
        if not isinstance(value, datetime):
            raise TypeError('IsCloseTo value must be datetime')
        if not isinstance(delta, timedelta):
            raise TypeError('IsCloseTo delta must be timedelta')

        self.value = value
        self.delta = delta

    def _get_actual_delta(self, item: datetime) -> timedelta:
        zero = timedelta()
        delta = item - self.value
        if delta < zero:
            delta = self.value - item
        return delta

    def _matches(self, item: Any) -> bool:
        if not isinstance(item, datetime):
            return False
        actual_delta = self._get_actual_delta(item)
        return actual_delta <= self.delta

    def describe_mismatch(self, item: Any, mismatch_description: Description) -> None:
        if not isinstance(item, datetime):
            super().describe_mismatch(item, mismatch_description)
        else:
            actual_delta = self._get_actual_delta(item)
            mismatch_description.append_description_of(item)            \
                                .append_text(' differed by ')           \
                                .append_description_of(actual_delta)

    def describe_to(self, description: Description) -> None:
        description.append_text('a value within ')  \
                   .append_description_of(self.delta)       \
                   .append_text(' of ')                     \
                   .append_description_of(self.value)


def close_to_datetime(value: datetime, delta: timedelta) -> IsCloseToDateTime:
    return IsCloseToDateTime(value, delta)

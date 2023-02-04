
from decimal import Decimal

from hamcrest.core.base_matcher import BaseMatcher


class LambdaMatcher(BaseMatcher):
    """
    Simple matcher that accepts function (probably lambda),
    which accepts single argument and returns True (success) or False (failure).
    """

    def __init__(self, f):
        self.f = f

    def _matches(self, item):
        return self.f(item)

    def describe_to(self, description):
        description.append_description_of(self.f)


def string_as_number_equals_to(num):
    """ Cast string value to Decimal and compare it num. """
    return LambdaMatcher(lambda v: Decimal(v) == num)

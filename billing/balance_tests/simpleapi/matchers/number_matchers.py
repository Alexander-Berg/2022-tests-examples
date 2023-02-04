# coding=utf-8
import operator
import re
from decimal import Decimal

from hamcrest.library.number.ordering_comparison import OrderingComparison

BASE_DECIMAL_PATTERN = "\d+\.\d{2}$"  # 10.00


class StrOrderingComparison(OrderingComparison):
    """
    Матчер действует аналогично хамкрестовским
    greater_than, greater_than_or_equal_to, less_than, less_than_or_equal_to
    но на вход принимает число в виде строки
    и проверяет, что строка соответствеует формату,
    заданному регулярным выражением pattern
    """

    def __init__(self, value, comparison_function, comparison_description, pattern):
        super(StrOrderingComparison, self).__init__(value, comparison_function,
                                                    comparison_description)
        self.pattern = pattern
        self.match_pattern = True
        self.is_string = True

    def _matches(self, item):
        if isinstance(item, basestring):
            if re.match(self.pattern, item) is None:
                self.match_pattern = False
                return False
        else:
            self.is_string = False
            return False

        self.value = Decimal(self.value)
        item = Decimal(item)

        return super(StrOrderingComparison, self)._matches(item)

    def describe_to(self, description):
        if not self.match_pattern:
            description.append_text("number matches to format: '{}'"
                                    .format(self.pattern))
        else:
            super(StrOrderingComparison, self).describe_to(description)
            description.append_text(' and match format: ') \
                .append_text("'{}'".format(self.pattern))

    def describe_mismatch(self, item, mismatch_description):
        if not self.is_string:
            mismatch_description.append_text("given value is not string but '{}'".format(type(item)))
        elif not self.match_pattern:
            mismatch_description.append_text("got '{}'".format(item))
        else:
            super(StrOrderingComparison, self).describe_mismatch(item, mismatch_description)


def greater_than(value, pattern=BASE_DECIMAL_PATTERN):
    return StrOrderingComparison(value, operator.gt, 'greater than', pattern)


def greater_than_or_equal_to(value, pattern=BASE_DECIMAL_PATTERN):
    return StrOrderingComparison(value, operator.ge, 'greater than or equal to', pattern)


def less_than(value, pattern=BASE_DECIMAL_PATTERN):
    return StrOrderingComparison(value, operator.lt, 'less than', pattern)


def less_than_or_equal_to(value, pattern=BASE_DECIMAL_PATTERN):
    return StrOrderingComparison(value, operator.le, 'less than or equal to', pattern)


def equals_to(value, pattern=BASE_DECIMAL_PATTERN):
    return StrOrderingComparison(value, operator.eq, 'equals to', pattern)

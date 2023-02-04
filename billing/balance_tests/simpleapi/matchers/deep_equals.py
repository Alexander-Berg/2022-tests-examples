# coding=utf-8
import pprint

from deepdiff import DeepDiff
from hamcrest.core.base_matcher import BaseMatcher


class DeepEquals(BaseMatcher):
    def __init__(self, expected, ignore_order=False, report_repetitions=False):
        self.expected = expected
        self.diff = {}
        self.ignore_order = ignore_order
        self.report_repetitions = report_repetitions

    def _matches(self, item):
        self.diff = DeepDiff(self.expected, item, ignore_order=self.ignore_order,
                             report_repetition=self.report_repetitions)

        if self.diff == {}:
            return True

        return False

    def describe_to(self, description):
        description.append_text('dict deep equals to: \n{}'.format(pprint.pformat(self.expected)))

    def describe_mismatch(self, item, mismatch_description):
        mismatch_description.append_text('actual and expected dicts have differences:\n{}'
                                         .format(pprint.pformat(self.diff)))


class DeepContains(BaseMatcher):
    def __init__(self, expected, ignore_order=False, report_repetitions=False):
        self.expected = expected
        self.diff = {}
        self.ignore_order = ignore_order
        self.report_repetitions = report_repetitions

    def _matches(self, item):
        if isinstance(item, dict):
            contained_item = {key: item.get(key) for key in self.expected if item.get(key) is not None}
        else:
            contained_item = item

        self.diff = DeepDiff(self.expected, contained_item, ignore_order=self.ignore_order,
                             report_repetition=self.report_repetitions)

        if self.diff == {}:
            return True

        return False

    def describe_to(self, description):
        description.append_text('dict deep contains: \n{}'.format(pprint.pformat(self.expected)))

    def describe_mismatch(self, item, mismatch_description):
        mismatch_description.append_text('actual dict is not contains expected, differences:\n{}'
                                         .format(pprint.pformat(self.diff)))


def deep_equals_to(obj):
    return DeepEquals(obj)


def deep_equals_ignore_order_to(obj, report_repetitions=True):
    return DeepEquals(obj, ignore_order=True, report_repetitions=report_repetitions)


def deep_contains(obj):
    return DeepContains(obj)


def deep_contains_ignore_order(obj, report_repetitions=True):
    return DeepContains(obj, ignore_order=True, report_repetitions=report_repetitions)

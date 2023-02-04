__author__ = 'torvald'

import datetime
from collections import defaultdict, Counter
from decimal import Decimal as D

from hamcrest import *
from hamcrest.core.base_matcher import BaseMatcher

from btestlib import utils

# Issue: xmlrpc receives Decimal value as strings
# To fix that we will simply convert values in actual data with types of values of correspond key from expected
convert_mapping = {float: float
    , int: int
    , str: str
    , D: lambda x: D(str(x))
    , datetime.datetime: lambda x: x
    , type(None): lambda x: x
                   }


class EndsMatch(BaseMatcher):
    def __init__(self, list_of_actual, partial=True):
        self.list_of_actual = list_of_actual
        self.list_of_expected = []
        self.partial = partial
        self.comb_dict = defaultdict(list)

    def _matches(self, item):
        # reverse both lists: we compare it from the end
        exp = self.list_of_expected = item[::-1]
        act = self.list_of_actual[::-1]
        # calculate minimal len to iterate on it
        min_len = len(exp) if len(exp) <= len(act) else len(act)
        # matching:
        for num in range(min_len):
            exp_item = exp[num]
            for key in exp_item:
                if key in act[num]:
                    # if decimalize(act[num][key]) != decimalize(exp_item[key]):
                    # if convert_mapping[type(exp_item[key])](act[num][key]) != exp_item[key]:
                    # Convert values in <actual> with types of values of correspond keys from <expected>
                    if convert_mapping.get(type(exp_item[key]), type(exp_item[key]))(act[num][key]) != exp_item[key]:
                        self.comb_dict[-num].append((key, exp_item[key], act[num][key]))
                else:
                    self.comb_dict[-num].append((key, exp_item[key], '...'))

        if len(exp) > len(act) or (len(exp) < len(act) and not self.partial):
            tail, code = (exp[min_len:], '<missed>') if len(exp) > len(act) else (act[min_len:], '<extra>')
            for num, row in enumerate(tail):
                self.comb_dict[-min_len - num].append(code)
                for key in row:
                    self.comb_dict[-min_len - num].append((key, row[key], '...'))

        if self.comb_dict:
            return False
        return True

    def describe_mismatch(self, item, mismatch_description):
        mismatch_description.append_text('\n')
        if not self.partial:
            max_len = len(self.list_of_expected) if len(self.list_of_expected) > len(self.list_of_actual) else len(
                self.list_of_actual)
        else:
            max_len = len(self.list_of_expected)
        for key in range(max_len - 1, -1, -1):
            mismatch_description.append_text('{0:>2}: {1}\n'.format(-key, self.comb_dict[-key]))

    def describe_to(self, description):
        description.append_text('\n')
        for key in range(len(self.list_of_expected) - 1, -1, -1):
            description.append_text(('{0:>2}: {1}\n'.format(-key, self.list_of_expected[key])))


class FullMatch(EndsMatch):
    def __init__(self, list_of_actual):
        EndsMatch.__init__(self, list_of_actual, partial=False)


class PartialMatchInAnyOrder(BaseMatcher):
    def __init__(self, list_of_actual, partial=True):
        self.list_of_actual = list_of_actual
        self.list_of_expected = []
        self.partial = partial
        self.comb_dict = dict()

    def _matches(self, item):
        exp = self.list_of_expected = item
        act = self.list_of_actual

        # pop extra keys from actual
        for line in act:
            for key in line.keys():
                if key not in exp[0]:
                    line.pop(key)
                else:
                    # Convert values in <actual> with types of values of correspond keys from <expected>
                    line[key] = convert_mapping.get(type(exp[0][key]), type(exp[0][key]))(line[key])

        # Convert every dict to tuple of tuples
        # act_modified = [tuple((key, decimalize(line[key])) for key in sorted(line.keys())) for line in act]
        # exp_modified = [tuple((key, decimalize(line[key])) for key in sorted(line.keys())) for line in exp]
        act_modified = [tuple((key, line[key]) for key in sorted(line.keys())) for line in act]
        exp_modified = [tuple((key, line[key]) for key in sorted(line.keys())) for line in exp]

        # Convert to Counters, substruct actual by expected to get value for every expected row:
        # >0: actual contains expected row multiple times (n+1) (Full match will be failed)
        # =0: actual contains expected row
        # <0: actual doesn't contain expected row (Partial and Full matches wil be failed)
        act_cnt = Counter(act_modified)
        exp_cnt = Counter(exp_modified)
        act_cnt.subtract(exp_cnt)
        self.comb_dict = dict(act_cnt)

        for item in act_cnt.values():
            if item < 0 or (item > 0 and not self.partial):
                return False
        return True

    def describe_mismatch(self, item, mismatch_description):
        mismatch_description.append_text('\n')
        result = self.comb_dict
        for key in result:
            if result[key] < 0:
                mismatch_description.append_text('<missed> [{0}]: {1}\n'.format(result[key], dict(key)))
        if not self.partial:
            for key in result:
                if result[key] > 0:
                    mismatch_description.append_text('<extra> [{0}]: {1}\n'.format(result[key], dict(key)))

    def describe_to(self, description):
        description.append_text('\n')
        for line in self.list_of_expected:
            description.append_text('{0}\n'.format(line))


class FullMatchInAnyOrder(PartialMatchInAnyOrder):
    def __init__(self, list_of_actual):
        PartialMatchInAnyOrder.__init__(self, list_of_actual, partial=False)


if __name__ == "__main__":
    a = [{'pre': 1, 'post': 10},
         {'pre': 2, 'post': 20},
         {'pre': 3, 'post': 30},
         {'pre': 4, 'post': 40}
         ]
    b = [{'pre': 1, 'post': 11},
         {'pre': 2, 'post': 20},
         {'pre': 3, 'post': 30}
         ]
    c = [{'pre': 1},
         {'pre': 2, 'post': 20},
         {'pre': 30, 'post': 30}
         ]
    d = [{'pre': D('3'), 'post': 30},
         {'pre': 4, 'post': 40}
         ]
    e = [{'pre': 2, 'post': 21},
         {'pre': 3, 'post': 30}
         ]
    f = [{'pre': 1},
         {'pre': 3, 'post': 30},
         {'pre': 30, 'post': 30}
         ]
    g = [{'test1': 1, 'test2': None}]
    g1 = [{'test1': 1, 'test2': None}]
    act = [{'pre': 1, 'post': '1.25', 'sup': 2},
           {'pre': 2, 'post': 2.5, 'sup': 2},
           {'pre': 3, 'post': D('3.0'), 'sup': 2},
           {'pre': 4, 'post': D('4.123'), 'sup': 2}
           ]
    exp = [{'pre': 1, 'post': D('1.25'), 'sup': 2},
           {'pre': 3, 'post': 3, 'sup': 2},
           {'pre': 2, 'post': D('2.5'), 'sup': 2},
           {'pre': 4, 'post': D('4.123'), 'sup': 2}
           ]
    exp2 = [{'pre': 1, 'post': D('1.25'), 'sup': 2},
            {'pre': 3, 'post': D('3.0'), 'sup': 2},
            {'pre': 2, 'post': D('2.5'), 'sup': 2},
            ]
    exp3 = [{'pre': 1, 'post': D('1.33'), 'sup': 2},
            {'pre': 2, 'post': D('2.5'), 'sup': 2},
            {'pre': 3, 'post': D('3.0'), 'sup': 2},
            {'pre': 4, 'post': D('4.123'), 'sup': 2}
            ]
    utils.check_that(g, FullMatch(g1))
    utils.check_that(exp, is_(PartialMatchInAnyOrder(act)))
    utils.check_that(d, is_(EndsMatch(b)))

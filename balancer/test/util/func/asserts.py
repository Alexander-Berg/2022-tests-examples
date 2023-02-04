# -*- coding: utf-8 -*-
from balancer.test.util.func._func import Interval, join_intervals, break_interval, minimum, maximum


class AssertOptions(object):
    def __init__(self, interval, x_tolerance_left, x_tolerance_right, y_tolerance):
        super(AssertOptions, self).__init__()
        self.interval = interval
        self.x_tolerance_left = x_tolerance_left
        self.x_tolerance_right = x_tolerance_right
        self.y_tolerance = y_tolerance
        self.assert_interval = Interval(interval.start + x_tolerance_left, interval.fin - x_tolerance_right)


class FunctionAssertionError(AssertionError):
    pass


def maxdiff(left, right):
    return max((abs(left - right)).y_values)


def __lesser(left, right):
    intervals = list()
    error_start = None
    le_func_points = (left < right)
    prev_x = None
    for x, y in (left < right).points:
        if y:
            if error_start is not None:  # error interval finished
                intervals.append(Interval(error_start, x))
                error_start = None
        else:
            if error_start is None:  # new error
                if prev_x is None:  # error at the beginning of assert_interval
                    error_start = le_func_points.interval.start
                else:
                    error_start = prev_x
        prev_x = x
    if error_start is not None:  # error at the end of assert_interval
        intervals.append(Interval(error_start, le_func_points.interval.fin))
    return intervals


def lesser(left, right):  # TODO: need interval and toleance options
    intervals = __lesser(left, right)
    if intervals:
        raise FunctionAssertionError(intervals)


def minmax(func, fixed_func, options, step):
    curshift = -options.x_tolerance_right
    result_func = func.restrict(options.assert_interval)
    result_diff = maxdiff(fixed_func, func)
    while curshift <= options.x_tolerance_left:
        shifted_func = func.shift(curshift).restrict(options.assert_interval)
        shifted_diff = maxdiff(fixed_func, shifted_func)
        if shifted_diff < result_diff:
            result_func = shifted_func
            result_diff = shifted_diff
        curshift = curshift + step
    return result_func


def equal(expected, actual, interval, x_tolerance_left, x_tolerance_right, y_tolerance):
    options = AssertOptions(interval, x_tolerance_left, x_tolerance_right, y_tolerance)
    expected_name = expected.name
    expected = minmax(expected, actual, options, 0.1)

    lo_expected = expected - y_tolerance
    high_expected = expected + y_tolerance

    lo_intervals = __lesser(lo_expected, actual)
    high_intervals = __lesser(actual, high_expected)

    fail = join_intervals(lo_intervals + high_intervals)
    ok = break_interval(options.assert_interval, fail)

    min_func = minimum(lo_expected, actual)
    max_func = maximum(actual, high_expected)

    for i in fail:
        actual.plot.add_fail_area(min_func.restrict(i), max_func.restrict(i))
    for i in ok:
        actual.plot.add_ok_area(lo_expected.restrict(i), high_expected.restrict(i), name=expected_name)

    if fail:
        raise FunctionAssertionError(
            '{} ({}) is not equal to {} in interval {}'
            .format(actual.name, actual.points, expected_name, fail)
        )

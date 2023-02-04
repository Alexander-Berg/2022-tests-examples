# -*- coding: utf-8 -*-
import sys
import math
import numbers


EPS = 0.01


class IntervalException(Exception):
    pass


class Interval(object):
    def __init__(self, start, fin):
        if fin is not None and start > fin:
            raise IntervalException('start should not be greater than fin ({} > {})'.format(start, fin))
        super(Interval, self).__init__()
        self.__start = start
        self.__fin = fin

    @property
    def start(self):
        return self.__start

    @property
    def duration(self):
        return self.__fin - self.__start

    @property
    def fin(self):
        return self.__fin

    def __hash__(self):
        return hash((self.__start, self.__fin))

    def __eq__(self, other):
        return \
            isinstance(other, Interval) and\
            self.start == other.start and\
            self.fin == other.fin

    def __ne__(self, other):
        return not self == other

    def __repr__(self):
        return '[{}, {}]'.format(self.start, self.fin)


def join_intervals(intervals):
    if not intervals:
        return list()

    intervals = sorted(intervals, key=lambda x: x.start)
    result = list()
    cur_start = intervals[0].start
    cur_fin = intervals[0].fin
    for interval in intervals[1:]:
        if interval.start <= cur_fin:
            cur_fin = max(cur_fin, interval.fin)
        else:
            result.append(Interval(cur_start, cur_fin))
            cur_start = interval.start
            cur_fin = interval.fin
    result.append(Interval(cur_start, cur_fin))
    return result


def break_interval(interval, break_intervals):
    cur_start = interval.start
    break_intervals = sorted(break_intervals, key=lambda x: x.start)
    result = list()
    for b_int in break_intervals:
        if cur_start < b_int.start:
            result.append(Interval(cur_start, min(b_int.start, interval.fin)))
        cur_start = b_int.fin
        if interval.fin <= cur_start:
            break
    if cur_start < interval.fin:
        result.append(Interval(cur_start, interval.fin))
    return result


class AbstractIntervalHolder(object):
    @property
    def interval(self):
        raise NotImplementedError()

    @interval.setter
    def interval(self, value):
        raise NotImplementedError()


class IntervalHolder(AbstractIntervalHolder):
    def __init__(self):
        super(IntervalHolder, self).__init__()
        self.__interval = None

    @property
    def interval(self):
        return self.__interval

    @interval.setter
    def interval(self, value):
        if self.__interval is None:
            self.__interval = value


class FunctionException(Exception):
    pass


class FunctionType(object):
    CONTINUOUS = 'continuous'
    SCATTER = 'scatter'


class NumericFunction(AbstractIntervalHolder):
    def __init__(self):
        super(NumericFunction, self).__init__()
        self.__name = None
        self.__plot = None

    def __str__(self):
        return str(self.name)

    @property
    def plot(self):
        return self.__plot

    @plot.setter
    def plot(self, value):
        if self.__plot is None:
            self.__plot = value
        else:
            raise FunctionException('plot has been already set')

    @property
    def name(self):
        if self.__name is not None:
            return self.__name
        else:
            return self._default_name

    @name.setter
    def name(self, value):
        self.__name = value
        if self.__plot is not None:
            self.__plot.name = value

    @property
    def _default_name(self):
        return None

    @property
    def function_type(self):
        raise NotImplementedError()

    def __add__(self, other):
        return AddFunction(self, self.__make_func(other))

    def __radd__(self, other):
        return self + other

    def __neg__(self):
        return NegFunction(self)

    def __sub__(self, other):
        return self + self.__make_func(-other)

    def __rsub__(self, other):
        return -self + self.__make_func(other)

    def __mul__(self, other):
        return MulFunction(self, self.__make_func(other))

    def __rmul__(self, other):
        return self * other

    def __div__(self, other):
        return DivFunction(self, self.__make_func(other))

    def __rdiv__(self, other):
        return DivFunction(self.__make_func(other), self)

    def __le__(self, other):
        return LeFunction(self, self.__make_func(other))

    def __lt__(self, other):
        return LtFunction(self, self.__make_func(other))

    def __eq__(self, other):
        return EqFunction(self, self.__make_func(other))

    def __abs__(self):
        return AbsFunction(self)

    def shift(self, value):
        return ShiftedFunction(self, value)

    def restrict(self, interval):
        self.__check_interval(interval)
        return RestrictedFunction(self, interval)

    def __call__(self, x):
        self.__check_x(x)
        return self._call(x)

    def _call(self, x):
        raise NotImplementedError()

    def call_fuzzy(self, x):
        """
        Used while building plots
        """
        return self(x)

    @property
    def y_values(self):
        return [self(x) for x in self.x_values]

    @property
    def x_values(self):
        return self._x_values(self.interval)

    @property
    def points(self):
        return zip(self.x_values, self.y_values)

    @property
    def plot_points(self):
        return self._plot_points(self.interval)

    def _plot_points(self, interval):
        raise NotImplementedError()

    def _x_values(self, interval):
        raise NotImplementedError()

    def __make_func(self, value):
        if isinstance(value, numbers.Number):
            result = Const(value)
            result.interval = Interval(-sys.maxint - 1, sys.maxint)
            return result
        elif isinstance(value, NumericFunction):
            return value
        else:
            return NotImplemented

    def __check_x(self, x):
        if self.interval.start > x or self.interval.fin < x:
            raise FunctionException(
                'value {} is out of funciton domain {}'
                .format(x, self.interval)
            )

    def __check_interval(self, interval):
        if self.interval.start > interval.start or self.interval.fin < interval.fin:
            raise FunctionException(
                'interval {} should be a subset of function domain {}'
                .format(interval, self.interval)
            )


class FunctionWrapper(NumericFunction):
    def __init__(self, func):
        super(FunctionWrapper, self).__init__()
        self._func = func

    @property
    def function_type(self):
        return self._func.function_type

    def _call(self, x):
        return self._func(x)

    def call_fuzzy(self, x):
        return self._func.call_fuzzy(x)

    def _x_values(self, interval):
        return self._func._x_values(interval)

    @property
    def interval(self):
        return self._func.interval

    @interval.setter
    def interval(self, value):
        self._func.interval = value

    def _plot_points(self, interval):
        return self._func._plot_points(interval)


class RestrictedFunction(FunctionWrapper):
    def __init__(self, func, interval):
        super(RestrictedFunction, self).__init__(func)
        self.__interval = interval

    @property
    def interval(self):
        return self.__interval

    @interval.setter
    def interval(self, value):
        pass

    @property
    def _default_name(self):
        return 'restrict(func={}, domain={})'.format(self._func.name, self.interval)

    def _plot_points(self, interval):
        points = self._func._plot_points(interval)
        if points[0][0] != interval.start:
            points = [(interval.start, self.call_fuzzy(interval.start))] + points
        if points[-1][0] != interval.fin:
            points = points + [(interval.fin, self.call_fuzzy(interval.fin))]
        return points


class ShiftedFunction(FunctionWrapper):
    def __init__(self, func, shift_value):
        super(ShiftedFunction, self).__init__(func)
        self.__shift_value = shift_value
        self.__interval = None
        if self._func.interval is not None:
            self.__update_interval()

    @property
    def interval(self):
        return self.__interval

    @interval.setter
    def interval(self, value):
        self._func.interval = Interval(value.start - self.__shift_value, value.fin - self.__shift_value)
        self.__update_interval()

    def _call(self, x):
        return self._func(x - self.__shift_value)

    def call_fuzzy(self, x):
        return self._func.call_fuzzy(x - self.__shift_value)

    @property
    def _default_name(self):
        return 'shift(func={}, value={})'.format(self._func.name, self.__shift_value)

    def _x_values(self, interval):
        new_interval = Interval(interval.start - self.__shift_value, interval.fin - self.__shift_value)
        return [x + self.__shift_value for x in self._func._x_values(new_interval)]

    def __update_interval(self):
        if self.__interval is None:
            self.__interval = Interval(
                self._func.interval.start + self.__shift_value,
                self._func.interval.fin + self.__shift_value
            )

    def _plot_points(self, interval):
        new_interval = Interval(interval.start - self.__shift_value, interval.fin - self.__shift_value)
        return [(x + self.__shift_value, y) for x, y in self._func._plot_points(new_interval)]


class UnaryOperator(FunctionWrapper):
    def _call(self, x):
        return self._call_unary(self._func(x))

    def _call_unary(self, value):
        raise NotImplementedError()

    def call_fuzzy(self, x):
        return self._call_unary(self._func.call_fuzzy(x))

    def _plot_points(self, interval):
        return [(x, self.call_fuzzy(x)) for x, y in self._func._plot_points(interval)]


class BinaryOperator(NumericFunction):
    def __init__(self, left, right):
        super(BinaryOperator, self).__init__()
        self._left = left
        self._right = right

    @property
    def interval(self):
        return Interval(
            max(self._left.interval.start, self._right.interval.start),
            min(self._left.interval.fin, self._right.interval.fin),
        )

    @interval.setter
    def interval(self, value):
        self._left.interval = value
        self._right.interval = value

    @property
    def function_type(self):
        if self._left.function_type == FunctionType.SCATTER or self._right.function_type == FunctionType.SCATTER:
            return FunctionType.SCATTER
        else:
            return FunctionType.CONTINUOUS

    def _call(self, x):
        return self._call_binary(self._left(x), self._right(x))

    def _call_binary(self, left, right):
        raise NotImplementedError()

    def call_fuzzy(self, x):
        return self._call_binary(self._left.call_fuzzy(x), self._right.call_fuzzy(x))

    def _x_values(self, interval):
        if self._left.function_type == FunctionType.SCATTER and self._right.function_type == FunctionType.SCATTER:
            return self.__join_scatters(self._left._x_values(interval), self._right._x_values(interval))
        elif self._left.function_type == FunctionType.SCATTER:
            return self._left._x_values(interval)
        elif self._right.function_type == FunctionType.SCATTER:
            return self._right._x_values(interval)
        else:
            raise NotImplementedError()

    def _plot_points(self, interval):
        all_x = sorted(list(set(
            [p[0] for p in self._left._plot_points(interval)] + [p[0] for p in self._right._plot_points(interval)]
        )))
        return [(x, self.call_fuzzy(x)) for x in all_x]

    @staticmethod
    def __join_scatters(left, right):
        if len(left) != len(right):
            raise FunctionException('points numbers don\'t match: {} != {}'.format(len(left), len(right)))
        for left_x, right_x in zip(left, right):
            if abs(left_x - right_x) > EPS:
                raise FunctionException(
                    'points are located too far away from each other: {}, {}'
                    .format(left_x, right_x)
                )
        return left


class NegFunction(UnaryOperator):
    @staticmethod
    def _call_unary(value):
        return -value

    @property
    def _default_name(self):
        if isinstance(self._func, BinaryOperator):
            return '-({})'.format(self._func.name)
        else:
            return '-{}'.format(self._func.name)


class AbsFunction(UnaryOperator):
    @staticmethod
    def _call_unary(value):
        return abs(value)

    @property
    def _default_name(self):
        return 'abs({})'.format(self._func.name)


class AddFunction(BinaryOperator):
    @staticmethod
    def _call_binary(left, right):
        return left + right

    @property
    def _default_name(self):
        return '{} + {}'.format(self._left.name, self._right.name)


class MulFunction(BinaryOperator):
    @staticmethod
    def _call_binary(left, right):
        return left * right

    @property
    def _default_name(self):
        return '{} * {}'.format(self.__gen_mul_name(self._left), self.__gen_mul_name(self._right))

    @staticmethod
    def __gen_mul_name(value):
        if not isinstance(value, BinaryOperator) or isinstance(value, MulFunction):
            return value.name
        else:
            return '({})'.format(value.name)


class DivFunction(BinaryOperator):
    @staticmethod
    def _call_binary(left, right):
        return left / right

    @property
    def _default_name(self):
        if not isinstance(self._left, BinaryOperator) or isinstance(self._left, MulFunction):
            left_name = self._left.name
        else:
            left_name = '({})'.format(self._left.name)
        if not isinstance(self._right, BinaryOperator):
            right_name = self._right.name
        else:
            right_name = '({})'.format(self._right.name)
        return '{} / {}'.format(left_name, right_name)


class LeFunction(BinaryOperator):
    @staticmethod
    def _call_binary(left, right):
        return left <= right


class LtFunction(BinaryOperator):
    @staticmethod
    def _call_binary(left, right):
        return left < right


class EqFunction(BinaryOperator):
    @staticmethod
    def _call_binary(left, right):
        return left == right


class MaximumFunction(BinaryOperator):
    @staticmethod
    def _call_binary(left, right):
        return max(left, right)


maximum = MaximumFunction


class MinimumFunction(BinaryOperator):
    @staticmethod
    def _call_binary(left, right):
        return min(left, right)


minimum = MinimumFunction


class Const(IntervalHolder, NumericFunction):
    def __init__(self, value):
        super(Const, self).__init__()
        self.__value = value

    @property
    def _default_name(self):
        return str(self.__value)

    def _plot_points(self, interval):
        return [(interval.start, self.__value), (interval.fin, self.__value)]

    function_type = FunctionType.CONTINUOUS

    def _call(self, x):
        return self.__value

    def rps_str(self):
        return 'const({value}, {duration})'.format(
            value=self.__value,
            duration=self.interval.duration,
        )


class Line(IntervalHolder, NumericFunction):
    def __init__(self, y0, y1):
        super(Line, self).__init__()
        self.__y0 = y0
        self.__y1 = y1

    @property
    def x0(self):
        return self.interval.start

    @property
    def y0(self):
        return self.__y0

    @property
    def x1(self):
        return self.interval.fin

    @property
    def y1(self):
        return self.__y1

    def _plot_points(self, interval):
        return [
            (interval.start, self.call_fuzzy(interval.start)),
            (interval.fin, self.call_fuzzy(interval.fin)),
        ]

    @property
    def _default_name(self):
        return 'line(({}, {}) -- ({}, {}))'.format(
            self.x0, self.y0, self.x1, self.y1,
        )

    function_type = FunctionType.CONTINUOUS

    @property
    def __tan(self):
        return 1.0 * (self.y1 - self.y0) / (self.x1 - self.x0)

    def _call(self, x):
        return self.y0 + x * self.__tan

    def rps_str(self):
        return 'line({start_value}, {fin_value}, {duration})'.format(
            start_value=self.y0,
            fin_value=self.y1,
            duration=self.x1 - self.x0,
        )


class DataFunction(NumericFunction):
    def __init__(self):
        super(DataFunction, self).__init__()
        self.__data = None
        self.__start = None
        self.__step = None
        self.__x_values = None
        self.__interval = None

    @property
    def data(self):
        return self.__data

    @property
    def start(self):
        return self.__start

    @property
    def step(self):
        return self.__step

    @property
    def interval(self):
        return self.__interval

    @interval.setter
    def interval(self, value):
        pass

    def set_info(self, data, start, step):
        self.__data = data
        self.__start = start
        self.__step = step
        self.__interval = Interval(start, start + (len(data) - 1) * step)

    function_type = FunctionType.SCATTER

    def _call(self, x):
        float_index = 1.0 * (x - self.start) / self.step
        index = int(float_index)
        x_index = self.start + self.step * index
        if abs(x_index - x) > EPS:
            raise FunctionException('no value at {}'.format(x))
        return self.data[index]

    def call_fuzzy(self, x):  # TODO: get rid of copypaste
        float_index = 1.0 * (x - self.start) / self.step
        index = int(float_index)
        x_index = self.start + self.step * index
        if abs(x_index - x) > EPS:
            floor_index = int(math.floor(float_index))
            ceil_index = floor_index + 1
            line = Line(self.data[floor_index], self.data[ceil_index])
            line.interval = Interval(self.x_values[floor_index], self.x_values[ceil_index])
            return line(x)
        else:
            return self.data[index]

    def _plot_points(self, interval):
        return [p for p in self.points if interval.start <= p[0] <= interval.fin]

    @property
    def y_values(self):
        return self.data

    def _x_values(self, interval):
        if self.__x_values is None:
            self.__x_values = [self.start + count * self.step for count in range(len(self.data))]
        return [x for x in self.__x_values if interval.start <= x <= interval.fin]

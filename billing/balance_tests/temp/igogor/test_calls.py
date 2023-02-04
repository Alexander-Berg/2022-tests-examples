# coding: utf-8
__author__ = 'igogor'

import pytest
from _pytest.mark import MarkInfo

import btestlib.reporter as reporter

#temp

def ids(x):
    return ""


@pytest.mark.parametrize("first, second",
                         [[1, "a"],
                          pytest.mark.xfail([2, "b"])])
@pytest.mark.params_format("param1={first} and param2={second}",
                           first=lambda x: str(x) + '_lambda', second=lambda x: 'lambda_' + str(x))
# @pytest.mark.params_format("param1={first} and param2={second}", first=lambda x: str(x) + '_lambda')

# @pytest.mark.params_format(first=lambda x: str(x) + '_lambda')

# from btestlib.utils import Pytest
# @Pytest.params_format_mark(first=lambda x: str(x) + '_lambda')

def test_params_format(first, second):
    with reporter.step("With parameters {} {}".format(first, second)):
        pass


def test_with_no_parametrization():
    pass


@pytest.fixture(params=[[1, "a"],
                        pytest.mark.xfail([2, "b"])])
def just_fixture(request):
    return "just_fixture_value"


def test_with_fixture(just_fixture):
    with reporter.step("With fixture " + just_fixture):
        pass


@pytest.mark.parametrize("number, string",
                         [
                             (1, "One"),
                             (2, "Two"),
                             (3, "Three")
                         ])
@pytest.mark.params_format('number={number} string={string}',
                           number=lambda x: str(x),
                           string=lambda x: x[0])
def test_stupid(number, string):
    pass


class Type(object):
    def foo2(self, param):
        # type: (_pytest.python.Function) -> str
        return 'foo'


def test_types(a, b, c):
    # type: (str, MarkInfo, Function) -> dict
    d = c.foo2(a)

    return a.capitalize()


def test_log():
    print 'ebana'
    reporter.log('Aloha brothers')
    if reporter.logger() is reporter.logger():
        reporter.logger().info('So it seems')

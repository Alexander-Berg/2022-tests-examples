# coding=utf-8
__author__ = 'torvald'
import time

import pytest

# from balance.features import Features

# Значения меток по-умолчанию
pytestmark = [pytest.mark.priority('mid')  # Параметризированная метка
    , pytest.mark.slow  # Непараметризированная метка
              ]

slow = pytest.mark.slow  # Для краткости можно и так


@pytest.mark.parametrize('expected', [('correct')
    , pytest.mark.xfail(('xfailed without parenteeze'))
    , pytest.mark.xfail()((9, 9, 9))
    , pytest.mark.xfail(reason='BALANCE-0')((2, 2, 5))
    , pytest.mark.skipif(reason='1==1')((3, 3, 6))
                                      ]
                         )
def test_xfail_in_parametrization(expected, data_cache):
    '''
    Parametrized test with xfailed and skipped params
    '''
    print data_cache
    time.sleep(1)
    assert 1 == 2


@slow
@pytest.mark.priority('low')
# @reporter.feature(Features.XMLRPC, Features.CLIENT)
def test_simple(data_cache):
    '''
    Simple test with marks
    '''
    time.sleep(1)
    assert 1 == 1


# @reporter.feature(Features.XMLRPC, Features.CORE)
def test_failed():
    '''
    Failed test, new bug
    '''
    time.sleep(1)
    assert 1 == 2


params = [(1, 2), (2, 3)]


@pytest.mark.priority('mid')
# @reporter.feature(Features.XMLRPC, Features.CORE)
@pytest.mark.parametrize('x, y', params)
def test_parametrized(x, y):
    '''
    Parametrized test.
    '''
    time.sleep(x)
    assert 1 == 1


@pytest.mark.xfail
# @reporter.feature(Features.PERSON)
def test_xfail_failed():
    '''
    Test marked like known-issue. Still broken
    '''
    time.sleep(1)
    assert 1 == 2


@pytest.mark.xfail
@pytest.mark.categories('xmlrpc, core')
@pytest.mark.ticket('BALANCE-12345')
def test_xfail_fixed_without_reason():
    '''
    Test marked like known-issue. Fixed
    '''
    time.sleep(1)
    assert 1 == 1


@pytest.mark.xfail(reason='BALANCE-20000')
# @reporter.feature(Features.XMLRPC, Features.CORE)
@pytest.mark.ticket('BALANCE-12345')
def test_xfail_fixed_with_reason():
    '''
    Test marked like known-issue. Fixed
    '''
    time.sleep(1)
    assert 1 == 1


def test_xfail_inside_fixed():
    '''
    Test marked like known-issue. Fixed
    '''
    pytest.mark.xfail()
    time.sleep(1)
    assert 1 == 1


# @reporter.feature(Features.XMLRPC, Features.CORE)
@pytest.mark.skipif()
def test_skipped_without_reason():
    '''
    Test skipped by condition
    '''
    time.sleep(1)
    assert 1 == 2


# @reporter.feature(Features.XMLRPC, Features.CORE)
@pytest.mark.skipif(reason='1==1')
def test_skipped_with_reason():
    '''
    Test skipped by condition
    '''
    time.sleep(1)
    assert 1 == 2


@pytest.mark.parametrize('x, y, expected', [(1, 2, 3)
    , (7, 8, 15)
    , pytest.mark.xfail((1, 1, 2))
    , pytest.mark.xfail()((9, 9, 9))
    , pytest.mark.xfail(reason='BALANCE-0')((2, 2, 5))
    , pytest.mark.skipif(reason='1==1')((3, 3, 6))
                                            ]
                         )
def test_more_parametrized(x, y, expected):
    '''
    Parametrized test with xfailed and skipped params
    '''
    time.sleep(1)
    assert (x + y) == expected


@pytest.mark.parametrize('x', [1, 2])
@pytest.mark.parametrize('y', ['a', 'b'])
def test_most_parametrized(x, y):
    '''
    Parametrized test with all combinations: a-1, a-2, b-1, b-2
    '''
    time.sleep(1)
    assert 1 == 1


@pytest.mark.parametrize('x', [1, 2, 3], ids=['one', 'two', 'three'])
def test_with_param_names(x):
    '''
    Parametrized test with names
    '''
    time.sleep(1)
    assert 1 == 1


if __name__ == '__main__':
    pytest.main('test_pytest_examples.py -v --alluredir="C:\\torvald\\allure_test"')

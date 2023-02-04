# coding: utf-8
__author__ = 'igogor'

import pytest

import btestlib.reporter as reporter


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


@pytest.fixture
def just_fixture():
    return "just_fixture_value"


@pytest.mark.format_params("fixture={just_fixture}")
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


import balance.balance_steps as steps


def test_convert_client():
    client_id = 17516055
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY')


def test_longest_palindrome():
    def longestPalindrome(s):
        """
        :type s: str
        :rtype: str
        """

        def is_palindrome(ss):
            for i in range(len(ss) // 2):
                if ss[i] != ss[-i - 1]:
                    return False
            else:
                return bool(ss)

        if not s:
            return ""

        longest = s[0]
        queue = [len(s) // 2]
        while queue:
            i = queue.pop(0)

            j = 1
            while i - j >= 0 and i + j - 1 < len(s) and s[i - j] == s[i + j - 1]:
                if 2 * j > len(longest):
                    longest = s[i - j: i + j]
                j += 1

            j = 1
            while i - j >= 0 and i + j < len(s) and s[i - j] == s[i + j]:
                if 2 * j + 1 > len(longest):
                    longest = s[i - j: i + j + 1]
                j += 1

            if i - 1 >= 0 and i - 1 < len(s) // 2 and len(longest) < 2 * (i - 1) + 1:
                queue.append(i - 1)
            if i + 1 < len(s) and i + 1 > len(s) // 2 and len(longest) < 2 * (len(s) - i + 1) - 1:
                queue.append(i + 1)

        return longest

    res = longestPalindrome("ab")
    print res

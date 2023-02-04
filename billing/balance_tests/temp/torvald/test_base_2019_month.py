# -*- coding: utf-8 -*-

import pytest
from hamcrest import equal_to

from btestlib import utils

from agent_reward import read_sheet, prof_2019_month_other_KO_inv, get_results


@pytest.fixture(scope='module')
def data():
    # Забор данных из .xls
    book = 'C:\\torvald\_TEST_TOOLS\\balance-tests\\temp\\torvald\NewCommissionTypes_2019.xlsx'
    sheet = 'base_2019(month)'
    data = read_sheet(book, sheet)

    # Вставка данных в Метабазу
    # base_2019_month(data)

    # Расчёт
    calculate = get_results
    actual_data = calculate(data)

    # Забор ожидаемых результатов расчёта
    result_data = get_results(data)

    return actual_data, result_data


test_cases = [
    1,  # comment1
    2,  # comment2
    3,  # comment3
    4,  # comment4
    5,  # comment5
    6,  # comment6
    7,  # comment7
    8,  # comment8
    9,  # comment9
    10,  # comment10
    11,  # comment11
]


# test_cases = [1]

@pytest.mark.parametrize('test_case', test_cases)
def test_base_2019_month(test_case, data):
    actual_data, expected_data = data

    utils.check_that(actual_data[test_case], equal_to(expected_data[test_case]))

# -*- coding: utf-8 -*-

import pytest
from hamcrest import equal_to

from btestlib import utils
from btestlib.matchers import contains_dicts_equal_to

from agent_reward import get_etalon_results, get_actual_results, insert_test_data, read_sheet, make_inserts

@pytest.fixture(scope='module')
def read_data_from_xls():
    data, case_contracts, const = read_sheet(book, sheet)
    etalon_data = get_etalon_results(book, sheet_results)
    return data, case_contracts, const, etalon_data


def get_actual_data(calc_type):
    insert_test_data()
    return get_actual_results(calc_type)


test_cases = list(range(1, 46))
book = 'C:\\balance-tests-new\\temp\\sandyk\\yb-ar\\NewCommissionTypes_2019_test.xlsx'
sheet = 'bel_2019(month)_new_scheme'
sheet_results = 'bel_etalon'


@pytest.mark.parametrize('test_case', test_cases)
def test_belarus_2019_month(test_case, read_data_from_xls):
    data, case_contracts, const, etalon_data = read_data_from_xls
    make_inserts(data[test_case], const)
    actual_data = get_actual_data('belarus')
    utils.check_that(actual_data[test_case],
                     contains_dicts_equal_to(etalon_data[test_case], same_length=True, in_order=False))

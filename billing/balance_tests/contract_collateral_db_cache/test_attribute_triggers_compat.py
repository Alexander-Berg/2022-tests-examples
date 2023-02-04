import pytest

from balance.mapper import fill_currency_alphanum
from .test_db_compat import create_contract_with_old_rub, create_contract_new_rub, create_contract_with_rub_str


def get_currency_attribute(contract):
    for raw_attr in contract.col0.attributes:
        if raw_attr.code.upper() == 'CURRENCY':
            return raw_attr
    else:
        raise ValueError('Given contract has no attribute CURRENCY')


@pytest.mark.parametrize(
    ['contract_func', 'expected_num', 'expected_str'],
    [
        (create_contract_with_old_rub, 810, 'RUB'),
        (create_contract_new_rub, 643, 'RUB'),
        (create_contract_with_rub_str, 643, 'RUB'),
    ]
)
def test_currency_attr_fill(session, contract_func, expected_num, expected_str):
    contract = contract_func(session, None)
    attr = get_currency_attribute(contract)
    processed_attr = fill_currency_alphanum(session, attr)

    assert processed_attr.value_num == expected_num
    assert processed_attr.value_str == expected_str

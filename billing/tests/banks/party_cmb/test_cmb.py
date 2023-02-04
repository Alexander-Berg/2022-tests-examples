from datetime import datetime, date
from decimal import Decimal

from bcl.banks.registry import Cmb
from bcl.core.models import Direction


def test_parse_statement(parse_statement_fixture, get_source_payment):
    statement_data = parse_statement_fixture(
        'cmb_statement.xls', Cmb, '110936764410901', 'CNY'
    )

    assert statement_data[0][0].is_valid
    assert statement_data[0][0].statement_date == date(2021, 12, 22)
    assert len(statement_data[0][1]) == 3
    assert statement_data[0][1][1].summ == Decimal('7294848.46')

    assert statement_data[1][0].is_valid
    assert statement_data[1][0].statement_date == date(2021, 12, 23)
    assert len(statement_data[1][1]) == 1

    assert statement_data[2][0].is_valid
    assert statement_data[2][0].statement_date == date(2021, 12, 24)
    assert len(statement_data[2][1]) == 9
    assert statement_data[2][1][1].direction == Direction.OUT

    assert statement_data[3][0].is_valid
    assert statement_data[3][0].statement_date == date(2021, 12, 27)
    assert len(statement_data[3][1]) == 4


def test_parse_empty_statement(parse_statement_fixture):

    result = parse_statement_fixture(
        'cmb_empty_statement.xls', Cmb, '110936764410901', 'CNY'

    )
    assert len(result) == 0

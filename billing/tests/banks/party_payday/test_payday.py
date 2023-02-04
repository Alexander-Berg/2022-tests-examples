from decimal import Decimal

from bcl.banks.registry import Payday


def test_statement_parser(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement.txt', Payday, '10000000000000000001', 'RUB')[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 1

    pay = payments[0]
    assert pay.is_out
    assert pay.summ == Decimal('50')

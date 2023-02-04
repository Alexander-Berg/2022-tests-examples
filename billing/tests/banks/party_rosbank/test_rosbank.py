from bcl.banks.registry import Rosbank


def test_payment_creator(get_payment_bundle, get_source_payment):
    compiled = Rosbank.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment()])).create_bundle()

    assert len(compiled.split('\r\n')) == 43
    assert 'ПоказательДаты' in compiled


def test_statement_parser(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement_rosbank.txt', Rosbank, '40702810500000061294', 'RUB')[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 2

    pay = payments[0]
    assert pay.is_out
    assert pay.summ == 1100.00

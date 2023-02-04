from bcl.banks.registry import Gazprom


def test_payment_creator(get_payment_bundle, get_source_payment):
    compiled = Gazprom.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment()])).create_bundle()

    assert len(compiled.split('\r\n')) == 44
    assert '\r\nДокумент=Платежное поручение' in compiled


def test_statement_parser(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement_gazprom.txt', Gazprom, '40602810100040000015', 'RUB')[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 2

    pay = payments[0]
    assert pay.is_out
    assert pay.summ == 24681.00


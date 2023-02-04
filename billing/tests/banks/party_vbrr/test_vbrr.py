from bcl.banks.registry import Vbrr


def test_payment_creator(get_payment_bundle, get_source_payment):
    compiled = Vbrr.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment()])).create_bundle()

    assert 'ПоказательДаты' in compiled


def test_statement_parser_allday(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement_vbrr.txt', associate=Vbrr, encoding=Vbrr.statement_dispatcher.parsers[0].encoding,
        acc='40702810400000007539', curr='RUB'
    )[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 1

    pay = payments[0]
    assert pay.is_out
    assert pay.info == {
        '06': 'Комиссия за регистрацию одного брелока для генерации одноразовых паролей. НДС не облагается.',
        '18': '17', '03': '47423810500001012792', '01': 'Акционерное общество "Всероссийский банк развития регионов"',
        '02': '7736153344', '04': '044525880'
    }

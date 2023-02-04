from bcl.banks.registry import VtbKz


def test_payment_creator(get_payment_bundle, get_source_payment):
    attrs = {'f_kbe': '17', 't_kbe': '17', 'knp': '858', 'head_of_company': 'test'}
    compiled = VtbKz.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment(attrs)])).create_bundle()

    assert 'ПоказательДаты' not in compiled
    assert 'ИИК' in compiled
    assert 'Документ=ПлатежноеПоручение' in compiled
    assert 'ДатаВалютирования' in compiled
    assert 'КодНазначенияПлатежа=858' in compiled
    assert 'КодНазПлатежа=' not in compiled


def test_statement_parser_allday(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement_vtb_kz.txt', associate=VtbKz, encoding=VtbKz.statement_dispatcher.parsers[0].encoding,
        acc='KZ344322203398A01259', curr='KZT'
    )[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 5

    pay = payments[-1]
    assert pay.is_in
    assert pay.info == {
        '06': 'Переводы клиентом денег со своего текущего счета в одном банке на свой текущий счет в другом банке',
        '04': 'ALFAKZKA', '03': 'KZ119470398991063968',
        '01': ' Товарищество с ограниченной ответственностью "Яндекс.Такси К ', '02': '160540014930'
    }


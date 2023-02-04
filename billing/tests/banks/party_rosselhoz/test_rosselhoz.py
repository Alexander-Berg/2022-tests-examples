from decimal import Decimal

from bcl.banks.registry import Rosselhoz


def test_payment_creator(get_payment_bundle, get_source_payment):
    compiled = Rosselhoz.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment()])).create_bundle()

    assert 'Получатель=ООО "Кинопортал"' in compiled
    assert 'НазначениеПлатежа1=Назначение' in compiled
    assert 'Сумма=152.00' in compiled
    assert len(compiled.split('\r\n')) == 45


def test_statement_parser(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement_stub.txt', Rosselhoz, '10000000000000000001', 'RUB')[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 1

    pay = payments[0]
    assert pay.is_out
    assert pay.summ == Decimal('5')

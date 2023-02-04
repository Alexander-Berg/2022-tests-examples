from decimal import Decimal

from bcl.banks.registry import Nbu


def test_payment_creator(get_payment_bundle, get_source_payment):
    compiled = Nbu.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment({
        'knp': '00717',
    })])).create_bundle()

    assert compiled == (
        '1*40702810800000007671*40702810301400002360*044525593*7725713770*ООО "Кинопортал"*152.00*00717*Назначение'
    )


def test_statement_parser(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement_nbu.xlsx', Nbu, '20208840905400877001', 'RUB')[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 2

    pay = payments[0]
    assert pay.is_out
    assert pay.summ == Decimal('5600')
    assert pay.get_info_purpose() == (
        '61011 Свободная покупка у юр лиц  По курсу 10862.00 '
        'Сумма к оплате\\получению 5 600.00 Сумма к оплате\\получению 60 827 200.00'
    )

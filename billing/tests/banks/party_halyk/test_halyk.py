from decimal import Decimal

import pytest

from bcl.banks.registry import Halyk
from bcl.core.models import Currency, BCL_INVALIDATED
from bcl.exceptions import ValidationError


@pytest.mark.parametrize('empty_field', ['f_kbe', 't_kbe', 'knp', 't_bankname'])
def test_payment_creator( empty_field, get_payment_bundle, get_source_payment):

    associate = Halyk

    attrs = {
        'f_iban': '123456',
        't_iban': '654321',
        'f_swiftcode': '1234567890',
        't_swiftcode': '0987654321',
        'knp': '119',
        'currency_id': Currency.by_code['KZT'],
        'f_kbe': '12',
        't_kbe': '21'
    }
    compiled = associate.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment(attrs)])).create_bundle()

    assert 'ПоказательДаты' not in compiled
    assert 'ИИК' in compiled
    assert 'Валюта=KZT' in compiled
    assert 'ДатаВалютирования' in compiled
    assert 'КодНазначенияПлатежа=119' in compiled
    assert 'КодНазПлатежа=' not in compiled
    assert '1234567890' not in compiled
    assert '0987654321' not in compiled
    assert 'Сумма=152.00\r' in compiled

    attrs.update({
        empty_field: ''
    })
    payment = get_source_payment(attrs)

    with pytest.raises(ValidationError) as e:
        associate.payment_dispatcher.get_creator(get_payment_bundle([payment])).create_bundle()

    payment.refresh_from_db()

    assert empty_field in e.value.msg
    assert payment.status == BCL_INVALIDATED


statement_params = [
    dict(
        associate=Halyk, encoding=Halyk.statement_dispatcher.parsers[0].encoding,
        acc='KZ95601A861003387661', curr='KZT'
    ),
]


@pytest.mark.parametrize('statement_params', statement_params)
def test_statement_final(statement_params, parse_statement_fixture, read_fixture):
    Halyk.statement_dispatcher.parsers[0].validate_file_format(read_fixture('halyk_final.txt'))

    register, pays = parse_statement_fixture(
        'halyk_final.txt', **statement_params
    )[0]
    assert register.is_valid
    assert register.summ == 110750.00

    assert len(pays) == 2
    assert pays[0].summ == 150.00
    assert pays[1].summ == 110600.00


@pytest.mark.parametrize('statement_params', statement_params)
def test_statement_intraday(statement_params, parse_statement_fixture, read_fixture):
    Halyk.statement_dispatcher.parsers[0].validate_file_format(read_fixture('halyk_intraday.txt'))

    register, pays = parse_statement_fixture(
        'halyk_intraday.txt', **statement_params
    )[0]
    assert register.is_valid
    assert register.intraday == 1
    assert register.summ == Decimal('-14975277.11')

    assert len(pays) == 2
    assert pays[0].summ == Decimal('15000000.00')
    assert pays[1].summ == Decimal('24722.89')


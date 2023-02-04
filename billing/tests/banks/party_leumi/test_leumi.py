from decimal import Decimal

import pytest

from bcl.banks.registry import Leumi
from bcl.core.models import Currency
from bcl.exceptions import BclException


def test_payment_creator(get_payment_bundle, get_source_payment):

    attrs = {
        'f_bic': '78106',
        'f_name': 'yandexx',
        't_name': 'somerecipientmorethan16',
        't_iban': 'IL580176680000075722323',
        'currency_id': Currency.ILS,
        'ground': 'PAY(13159228)Payment',
    }

    compiled = Leumi.payment_dispatcher.get_creator(
        get_payment_bundle([
            get_source_payment(attrs),
            get_source_payment(attrs)
        ])
    ).create_bundle()

    assert '999999' in compiled
    assert 'yandexx' in compiled
    assert 'somerecipientmor0' in compiled
    assert '000176680000075722323' in compiled
    assert '78106' in compiled
    assert '0304' in compiled  # сумма сумм платежей
    assert 'PAY(13159228)Payme ' in compiled


def test_statement_foreign(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'stat_foreign.dat', Leumi, '80099955642093', 'ILS',
        encoding=Leumi.statement_dispatcher.parsers[0].encoding)[0]

    assert register.is_valid
    assert register.summ == Decimal('-71.75')
    assert str(register.statement_date) == '2020-03-12'
    assert len(payments) == 17


def test_statement_bogus(parse_statement_fixture):

    with pytest.raises(BclException) as e:
        parse_statement_fixture(
            'bogus,data,\n', Leumi, '86411019180026', 'ILS',
            from_file=False
        )[0]

    assert 'Unable to parse row 1:' in f'{e.value}'


def test_statement_local(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'stat_local.dat', Leumi, '80011055642093', 'ILS',
        encoding=Leumi.statement_dispatcher.parsers[0].encoding)[0]

    assert register.is_valid
    assert register.summ == Decimal('451.85')
    assert str(register.statement_date) == '2020-03-12'
    assert len(payments) == 30

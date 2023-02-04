from decimal import Decimal
from typing import *

from bcl.banks.registry import Seb
from bcl.core.models import Currency, StatementRegister, StatementPayment


def test_statement_parser(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement_ing_external_mt940.txt', Seb, 'NL35INGB0650977351', 'EUR'
    )[0]  # type: StatementRegister, List[StatementPayment]

    assert len(payments) == 13
    assert register.is_valid
    assert register.statement_date.isoformat() == '2017-08-15'

    assert 'HOTELS COMBINED PTY LTD' in payments[0].get_info_purpose()
    assert 'Invoice N TNV015235 dd 12.05.2017.' in payments[12].get_info_purpose()


def test_swift_xt_sepa(get_payment_bundle, get_source_payment):
    payment_attrs = {
        'ground': 'test sepa payment',
        'summ': '1.00',
        'currency_id': Currency.EUR,

        'f_name': 'Yandex NV',
        'f_swiftcode': 'ESSEFIHX',
        'f_iban': 'LU370670006550031621',

        't_name': 'Yandex Europe AG',
        't_swiftcode': 'BBRUCHGT',
        't_iban': 'CH4708387000001280377',
        't_address': 'here',
    }

    result = Seb.payment_dispatcher.get_creator(
        get_payment_bundle([get_source_payment(payment_attrs)])).create_bundle()

    assert 'ESSEFIHX' in result
    assert '<InstdAmt Ccy="EUR">1.00</InstdAmt>' in result


def test_empty_statement(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'seb_azn_empty_statement.txt', Seb, '22334455667788991122', 'AZN',
        encoding=Seb.statement_dispatcher.parsers[0].encoding
    )[0]

    assert len(payments) == 0

    assert register.opening_balance == Decimal('10000.00')
    assert register.closing_balance == Decimal('10000.00')

    assert register.is_valid

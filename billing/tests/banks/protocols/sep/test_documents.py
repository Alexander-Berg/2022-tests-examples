import datetime
from decimal import Decimal

from bcl.banks.party_prior_by import PriorByStatementParser
from bcl.banks.protocols.sep.documents import StatementRub, PaymentOrderRub


def test_payment():

    document = PaymentOrderRub()
    document.header_account = '1234'
    document.date = datetime.datetime(2017, 5, 4)
    document.number = '111'
    document.amount = Decimal('20.33')
    document.purpose = 'Назначение'

    document.payer_bank_mfo = '98765'
    document.payer_account = '2222222'
    document.payer_name = 'Плательщик'
    document.payer_unp = '333'

    document.payee_bank_mfo = '56789'
    document.payee_account = '4444444'
    document.payee_name = 'Получатель'

    composed = document.compose()

    assert '^DatePlt=04.05.2017^' in composed
    assert '^IsQuick=0^' in composed


def test_statement_multi(read_fixture):
    data = read_fixture('sep_statement_multi.txt')

    statements = StatementRub.spawn(data.decode(PriorByStatementParser.encoding))  # type: StatementRub
    assert len(statements) == 1

    statement = statements[0]
    parsed = statement.data_parsed
    assert len(parsed) == 19
    assert len(parsed['payments']) == 4
    assert len(statement.payments) == 4


def test_statement(read_fixture):
    data = read_fixture('sep_statement.txt')

    statements = StatementRub.spawn(data.decode(PriorByStatementParser.encoding))  # type: StatementRub

    assert len(statements) == 1
    statement = statements[0]

    assert len(statement.payments) == 1
    payment = statement.payments[0]

    assert payment['number'] == '9168'
    assert payment['code'] == '749'
    assert payment['unp'] == '101356064'

    assert statement.header_type == 3
    assert statement.header_account == '3012005917011'
    assert statement.turn_ct == Decimal('10.45')
    assert statement.rem_date_in == datetime.datetime(2016, 6, 13).date()


def test_statement_only_required_fields(read_fixture):
    data = read_fixture('sep_statement_only_required.txt')

    statements = StatementRub.spawn(data.decode(PriorByStatementParser.encoding))  # type: StatementRub

    assert len(statements) == 1
    statement = statements[0]

    assert len(statement.payments) == 1
    payment = statement.payments[0]

    assert payment.get('unp') is None
import datetime
import re
from decimal import Decimal
from typing import *

import pytest

from bcl.banks.party_prior_by import PriorByStatementParser
from bcl.banks.registry import PriorBy
from bcl.core.models import StatementPayment, StatementRegister, states, PaymentsBundle, Direction
from bcl.exceptions import ValidationError, FileFormatError, DirectionIdentificationError
from bcl.toolbox.utils import DateUtils


def test_payment_creator(get_payment_bundle, get_source_payment, sitemessages):
    attrs = {
        'f_swiftcode': '',
        't_swiftcode': '',
        'f_inn': '',
        't_inn': ''
    }

    with pytest.raises(ValidationError):
        PriorBy.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment(attrs)])).create_bundle()
    incorrect_payment = get_source_payment(attrs)

    attrs = {
        'f_swiftcode': '111111',
        't_swiftcode': '22222222XXX'
    }
    bundle = get_payment_bundle([get_source_payment(attrs), incorrect_payment])

    compiled = PriorBy.payment_dispatcher.get_creator(bundle).create_bundle()

    bundle = PaymentsBundle.objects.get(id=bundle.id)
    incorrect_payment.refresh_from_db()

    assert len(bundle.payments) == 1
    assert incorrect_payment.status == states.BCL_INVALIDATED
    assert 'Значение обязательно, но не указано' in incorrect_payment.processing_notes
    assert all([a in incorrect_payment.processing_notes for a in ['t_swiftcode', 'f_swiftcode', 'f_inn', 't_inn']])

    messages = sitemessages()
    assert len(messages) == 2
    subject = messages[0].context['subject']
    assert 'Приорбанк Белоруссия' in subject
    assert 'Информация по обработке платежей' in subject

    assert '^UNN=7705713772^' in compiled
    assert '^UNNRec=7725713770^' in compiled
    assert '^MFO1=111111^' in compiled
    assert '^MFO2=22222222^' in compiled

    assert '^OchPlat=22^' in compiled


def test_statement_parser(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement_prior_by_A.txt', PriorBy, '3012005917011', 'RUB',
        encoding=PriorBy.statement_dispatcher.parsers[0].encoding)[0]  # type: StatementRegister, List[StatementPayment]

    assert not register.intraday
    assert register.is_valid
    assert len(payments) == 1
    assert payments[0].get_info_inn() == '101356064'
    assert payments[0].get_info_name() == ''
    assert payments[0].is_in


def test_currency_statement_parser(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement_prior_by_cur.txt', PriorBy, 'BY27PJCB30120232901020000840', 'USD',
        encoding=PriorBy.statement_dispatcher.parsers[0].encoding)[0]
    register: StatementRegister
    payments: List[StatementPayment]

    assert not register.intraday
    assert register.is_valid
    assert register.statement_date == datetime.date(2021, 2, 10)

    assert len(payments) == 1
    assert payments[0].get_info_inn() == ''
    assert payments[0].get_info_name() == 'РАСЧЕТЫ ПО ОПЕРАЦИЯМ С ЧЕКАМИ, БПК И ЭЛ.ДЕНЬГАМИ (ЗАЧИСЛЕНИЯ)'
    assert payments[0].is_out


def test_empty_statement_parser(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement_prior_by_empty.txt', PriorBy, 'BY27PJCB30120232901020000840', 'USD',
        encoding=PriorBy.statement_dispatcher.parsers[0].encoding)[0]
    register: StatementRegister
    payments: List[StatementPayment]

    assert not register.intraday
    assert register.is_valid
    assert len(payments) == 0


def test_statement_parser_intraday(read_fixture, get_assoc_acc_curr, get_statement):
    associate, acc, _ = get_assoc_acc_curr(PriorBy.id, account='BY68PJCB30120532631000000933')

    def assert_parse(text, payments_count):
        text = text.replace(b'06.09.2017', DateUtils.today().strftime('%d.%m.%Y').encode('ascii'))
        statement = get_statement(text, associate)

        parser = PriorBy.statement_dispatcher.get_parser(statement)
        register, payments = parser.process()[0]  # type: StatementRegister, List[StatementPayment]

        assert register.intraday
        assert register.is_valid
        assert len(payments) == payments_count

    assert_parse(read_fixture('statement_prior_by_I.txt'), payments_count=61)
    assert len(StatementPayment.objects.all()) == 61

    assert_parse(read_fixture('statement_prior_by_I.txt').replace(b'Num=899', b'Num=988'), payments_count=1)
    assert len(StatementPayment.objects.all()) == 62


def test_statement_parser_get_direction_by_amount():
    get_direction = PriorBy.statement_dispatcher.parsers[0].get_direction_by_amount

    dt = Decimal(0.0)
    ct = Decimal(10.0)

    assert get_direction(dt, ct) == Direction.IN

    dt = Decimal(10.0)
    ct = Decimal(0.0)
    assert get_direction(dt, ct) == Direction.OUT

    dt = Decimal(0.0)
    ct = Decimal(0.0)
    with pytest.raises(DirectionIdentificationError):
        get_direction(dt, ct)

    dt = Decimal(10.0)
    ct = Decimal(10.0)
    with pytest.raises(DirectionIdentificationError):
        get_direction(dt, ct)

    dt = Decimal(10.0)
    ct = Decimal(5.0)
    with pytest.raises(DirectionIdentificationError):
        get_direction(dt, ct)


def test_statement_parser_validate_file_format(read_fixture, get_assoc_acc_curr, get_statement):

    associate, acc, _ = get_assoc_acc_curr(PriorBy.id, account='3012005917011')

    text = read_fixture('statement_prior_by_A.txt')

    # valid
    assert PriorByStatementParser.validate_file_format(text) is None

    # invalid
    with pytest.raises(FileFormatError):
        PriorByStatementParser.validate_file_format(
            text.decode(PriorByStatementParser.encoding).replace('*****', '-').encode(PriorByStatementParser.encoding)
        )


def test_statement_balance_check(parse_statement_fixture):
    """Проверяет сверку начального сальдо текущей выписики с конечным предыдущей."""
    def replace_and_parse(date, opening_balance, closing_balance):
        def replace(text):
            params = {
                'Date': date,
                'DateIn': date,
                'DateOut': date,
                'DocDate': date,
                'CrIn': opening_balance,
                'CrOut': closing_balance
            }
            for param, value in params.items():
                text = re.sub(
                    r'({}=)[\d\.]+'.format(param), r'\g<1>{}'.format(value),
                    text,
                    flags=re.DOTALL
                )
            return text

        parse_statement_fixture(
            'statement_prior_by_A.txt', encoding=PriorBy.statement_dispatcher.parsers[0].encoding,
            mutator_func=replace,
            associate=PriorBy, acc='3012005917011', curr='RUB')

    replace_and_parse(date='13.06.2016', opening_balance='53836.86', closing_balance='53847.31')
    replace_and_parse(date='14.06.2016', opening_balance='53847.31', closing_balance='53857.76')
    with pytest.raises(ValidationError):
        replace_and_parse(date='15.06.2016', opening_balance='10.45', closing_balance='0.00')

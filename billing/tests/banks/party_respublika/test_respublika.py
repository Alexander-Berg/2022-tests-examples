from datetime import datetime, date

import pytest

from bcl.banks.registry import Respublika
from bcl.banks.party_respublika.statement_parser import RespublikaStatementParserXls
from bcl.core.models import Currency, states
from bcl.exceptions import ValidationError


def test_parse_statement(parse_statement_fixture, get_source_payment, mocker):
    now_date = datetime.now()
    attrs = {
        't_iban': 'AZ26PAHA40050AZNHC0100224951',
        'summ': '209.79',
        'status': states.EXPORTED_ONLINE,
        'date': datetime(2019, 3, 14),
        'f_iban': 'AZ46BRES00000000000000000101',
        'f_acc': 'AZ46BRES00000000000000000101',
    }
    payment = get_source_payment(attrs, associate=Respublika)
    old_matcher = RespublikaStatementParserXls.add_payments_to_register

    def add_payments_to_register(
        cls, register, payments, statement_datetime, balance_tuple, func_match_filter=None, with_salary=False,
        with_sbp=False
    ):
        if statement_datetime == datetime(2019, 3, 15, 0, 0):
            pay = payments[3]
            pay.date = now_date

        return old_matcher(
            register, payments, statement_datetime, balance_tuple, func_match_filter, with_salary, with_sbp
        )

    mocker.patch(
        'bcl.banks.party_respublika.statement_parser.RespublikaStatementParserXls.add_payments_to_register',
        add_payments_to_register
    )

    statament_data = parse_statement_fixture(
        'respublika_statement.xlsx', Respublika, 'AZ46BRES00000000000000000101', 'AZN'

    )

    assert statament_data[0][0].is_valid
    assert statament_data[0][0].statement_date == date(2019, 3, 1)
    assert len(statament_data[0][1]) == 1
    assert statament_data[0][1][0].number == '002IB0219059035'

    assert statament_data[1][0].is_valid
    assert statament_data[1][0].statement_date == date(2019, 3, 14)
    assert len(statament_data[1][1]) == 2

    assert statament_data[2][0].is_valid
    assert statament_data[2][0].statement_date == date(2019, 3, 15)
    assert len(statament_data[2][1]) == 8

    payment.refresh_from_db()
    linked_payments = list(payment.statementpayment_set.all())

    assert len(linked_payments) == 1
    assert linked_payments[0] in statament_data[2][1]
    assert linked_payments[0].number == str(payment.number)


def test_parse_empty_statement(parse_statement_fixture):

    result = parse_statement_fixture(
        'respublika_empty_statement.xlsx', Respublika, 'AZ21BRES00380394400273735304', 'AZN'

    )
    assert len(result) == 0


def test_payment_creator(get_payment_bundle, get_source_payment, response_mock, read_fixture):
    bankinfo = read_fixture('bank_data.xml', decode='utf8')

    def create_bundle(
        t_swiftcode='NABZAZ2CXXX', t_iban='AZ46BRES00000000000000000101', summ='152.00', t_inn='7725713770',
        t_bank_code=''
    ):

        with response_mock(
            f'GET https://www.cbar.az/bankinfonew/banks.xml -> 200 :{bankinfo}'
        ):
            attrs = {
                'f_bic': '78106',
                'f_name': 'yandexx',
                't_name': 'Anar Şükürov',
                't_swiftcode': t_swiftcode,
                't_iban': t_iban,
                'currency_id': Currency.AZN,
                'ground': 'PAY(131592/28)Payment',
                'summ': summ,
                't_inn': t_inn,
                'params': {'t_bank_code': t_bank_code},
            }
            payment = get_source_payment(attrs, associate=Respublika)

            compiled = Respublika.payment_dispatcher.get_creator(
                get_payment_bundle([payment], associate=Respublika)
            ).create_bundle()
        return compiled, payment

    compiled, payment = create_bundle()

    assert (
        f'XO,{payment.number},Anar Shukurov,{payment.t_iban},{payment.t_inn},501004,,,,{payment.summ},PAY(13159228)Payment\r\n'
    ) in compiled

    compiled, payment = create_bundle(summ='41000.10')

    assert (
        f'AZ,{payment.number},Anar Shukurov,{payment.t_iban},{payment.t_inn},501004,,,,{payment.summ},PAY(13159228)Payment\r\n'
    ) in compiled

    compiled, payment = create_bundle(t_iban='AZ48CTRE00000000000000014001')

    assert (
        f'XV,{payment.number},Anar Shukurov,{payment.t_iban},{payment.t_inn},501004,,,,{payment.summ},PAY(13159228)Payment\r\n'
    ) in compiled

    compiled, payment = create_bundle(t_iban='AZ48CTRE00000000000000014001', t_inn='1401555071')

    assert (
        f'XT,{payment.number},Anar Shukurov,{payment.t_iban},{payment.t_inn},501004,,,,{payment.summ},PAY(13159228)Payment\r\n'
    ) in compiled

    bankinfo = bankinfo.replace('NABZAZ2C', Respublika.bid[:8])

    compiled, payment = create_bundle(Respublika.bid, summ='41000.11')

    assert (
        f'IT,{payment.number},Anar Shukurov,{payment.t_iban},{payment.t_inn},501004,,,,{payment.summ},PAY(13159228)Payment\r\n'
    ) in compiled

    attrs = {
        'f_bic': '78106',
        'f_name': 'yandexx',
        't_name': 'name',
        't_swiftcode': Respublika.bid,
        't_iban': '122222',
        'currency_id': Currency.AZN,
        'ground': 'PAY(13159228)Payment',
        'summ': '10.11',
        'params': {'kbu': '3', 't_bank_code': '12345'},
    }
    payment = get_source_payment(attrs, associate=Respublika)

    with response_mock(f'GET https://www.cbar.az/bankinfonew/banks.xml -> 200 :{bankinfo}'):
        compiled = Respublika.payment_dispatcher.get_creator(
            get_payment_bundle([payment], associate=Respublika)
        ).create_bundle()

    assert (
        f'IT,{payment.number},name,{payment.t_iban},{payment.t_inn},12345,,3,,{payment.summ},PAY(13159228)Payment\r\n'
    ) in compiled

    with pytest.raises(ValidationError):
        create_bundle()

    bankinfo = 'Error'

    with pytest.raises(ValidationError):
        create_bundle()

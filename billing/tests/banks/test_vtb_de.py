from typing import *

import pytest

from bcl.banks.registry import VtbDe
from bcl.core.models import StatementRegister, StatementPayment
from bcl.core.views.rpc import Rpc
from bcl.exceptions import BclException, ValidationError


def test_create(
    get_payment_bundle, get_source_payment, get_assoc_acc_curr,  sitemessages, get_payment_creator):

    associate = VtbDe
    payment_dict = {
        'f_name': 'from name',
        'f_swiftcode': 'ABCDEFGH',
        't_swiftcode': 'HGFEDCBA',
        't_name': 'VTB test',
        't_address': 'Мос—кв—а–',  # Здесь разные символы тире.
        'ground': 'purpose',
    }

    payment_dict['f_iban'] = ''
    payment = get_source_payment(payment_dict, associate=associate)
    associate, acc, currency_id = get_assoc_acc_curr(
        associate.id, account={'number': payment.f_acc, 'currency_code': 'USD'}
    )

    def get_creator(payments):
        creator = get_payment_creator(associate, get_payment_bundle(payments, associate, acc))
        return creator

    # Если невалидный платёж всего один, поднимается исключение, пакет не формируется.
    with pytest.raises(BclException):
        creator = get_creator([payment])
        creator.create_bundle()

    # Платёж помечен невалидным.
    payment.refresh_from_db()
    assert payment.is_invalidated
    assert len(creator.preparation_notes[payment.pk]) == 2

    assert len(sitemessages()) == 1

    payment1 = get_source_payment(payment_dict, associate=VtbDe)
    f_iban = 'DE85503200000206701419'
    payment_dict['f_iban'] = f_iban
    payment2 = get_source_payment(payment_dict, associate=VtbDe)
    creator = get_creator([payment1, payment2])
    compiled = creator.create_bundle()

    # Если платежей несколько, исключение для невалидных платежей не поднимается,
    # формируется пакет с валидными платежами.
    payment1.refresh_from_db()
    assert payment1.is_invalidated  # Отсутствует f_iban
    assert not payment2.is_invalidated
    assert len(creator.preparation_notes[payment1.pk]) == 2
    assert len(creator.preparation_notes[payment2.pk]) == 1

    assert '50H' in compiled
    assert ':23E:URGP' not in compiled
    assert 'VTB test' in compiled
    assert 'MOSKVA' in compiled
    assert 'OUR' in compiled
    assert f_iban in compiled

    def create(payment_dict):
        creator = get_creator([get_source_payment(payment_dict, associate=VtbDe)])
        return creator.create_bundle()

    # Незаполненный paid_by -> OUR
    payment_dict['paid_by'] = ''
    assert 'OUR' in create(payment_dict)

    # Недопустимый символ в нетранслитерированном тексте.
    payment_dict['t_address'] = 'Moscow–1-'
    compiled = create(payment_dict)
    assert 'Moscow 1 ' in compiled

    # Код банка-посредника.
    assert ':56' not in compiled

    payment_dict['i_swiftcode'] = 'SWIFTXXX'
    payment_dict['urgent'] = True

    compiled = create(payment_dict)
    assert ':56A:SWIFTXXX' in compiled
    assert ':23E:URGP' in compiled


def test_payment_prefix(get_payment_bundle, get_source_payment, get_assoc_acc_curr, get_payment_creator):
    """Формируем пакеты платежей """

    associate = VtbDe

    payment_dict = {
        'f_name': 'from name',
        'f_swiftcode': 'ABCDEFGH',
        't_swiftcode': 'HGFEDCBA',
        't_name': 'VTB test',
        't_address': 'Мос—кв—а–',  # Здесь разные символы тире.
        'ground': 'purpose',
        'f_iban': 'DE85503200000206701419',
    }
    payment = get_source_payment(payment_dict, associate=associate)
    _, acc, currency_id = get_assoc_acc_curr(associate.id, account={'number': payment.f_acc, 'currency_code': 'USD'})

    payment1 = get_source_payment(payment_dict, associate=associate)
    payment2 = get_source_payment(payment_dict, associate=associate)

    creator = get_payment_creator(associate, get_payment_bundle([payment1, payment2], associate, acc))
    compiled = creator.create_bundle()
    assert ':21:YNDX2' in compiled
    assert payment_dict['f_name'] in compiled

    payment_dict['f_acc'] = 'TECH' + payment.f_acc
    _, acc, currency_id = get_assoc_acc_curr(
        associate.id, account={'number': payment_dict['f_acc'], 'currency_code': 'USD'}
    )

    payment = get_source_payment(payment_dict, associate=associate)

    creator = get_payment_creator(associate, get_payment_bundle([payment], associate, acc))
    compiled = creator.create_bundle()
    assert 'TECH' not in compiled
    assert payment_dict['f_name'] not in compiled
    assert payment.from_account.org.name in compiled


def test_parse_many(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'mt940_vtb_de_many.txt', VtbDe, '50320000/0206701419', 'EUR'
    )[0]  # type: StatementRegister, List[StatementPayment]

    assert len(payments) == 32
    assert register.is_valid


def test_parse(parse_statement_fixture):
    register, payments = parse_statement_fixture('mt940_vtb_de.txt', VtbDe, '50320000/0206701013', 'EUR')[0]

    assert len(payments) == 3
    assert payments[-2].get_info_purpose() == 'Interestforlas\r\nt period'

    assert register.is_valid

    # Тест раскладки назначений платежей.
    register, payments = parse_statement_fixture('mt940_vtb_de_purposes.txt', VtbDe, '50320000/0206701419', 'EUR')[0]

    assert len(payments) == 11
    assert register.is_valid

    assert '206  1/EASY CAB LLC' in payments[0].get_info_purpose()
    assert '61,01 USD' in payments[5].get_info_purpose()
    assert 'TRANSFER' in payments[6].get_info_purpose()
    assert 'MINDIA' in payments[7].get_info_purpose()


def test_statement_balance_check(parse_statement_fixture):
    """Проверяет сверку начального сальдо текущей выписики с конечным предыдущей."""
    def replace_and_parse(date, opening_balance, closing_balance):
        def replace(text):
            return text.replace('170330', date).replace('10,00', opening_balance).replace('15,50', closing_balance)

        result = parse_statement_fixture(
            fixture='mt940_vtb_de_closing_date.txt', mutator_func=replace, encoding='utf-8',
            associate=VtbDe, acc='50320000/0206701419', curr='USD'
        )
        assert result[0][1][0].number == '11'


    replace_and_parse(date='170328', opening_balance='4,50', closing_balance='10,00')
    replace_and_parse(date='170329', opening_balance='10,00', closing_balance='15,50')

    with pytest.raises(ValidationError):
        replace_and_parse(date='170330', opening_balance='120,00', closing_balance='120,50')


def test_statement_date(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'mt940_vtb_de_closing_date.txt', VtbDe, '50320000/0206701419', 'USD')[0]

    assert register.is_valid
    assert len(payments) == 1

    assert str(register.statement_date) == '2017-03-31'


def test_statement_purpose_parsing(parse_statement_fixture):
    """Подробнее в BCL-1194"""
    register, payments = parse_statement_fixture(
        'mt940_vtb_de_special_purposes.txt', VtbDe, '50320000/0206701419', 'USD')[0]

    assert register.is_valid
    assert len(payments) == 5

    to_dict = Rpc.to_dict_proved_pay

    assert to_dict(payments[0])['ground'] != ''
    assert to_dict(payments[1])['ground'] == ''
    assert to_dict(payments[2])['ground'] == 'Charges'
    assert to_dict(payments[2])['direction'] == 'OUT-CHARGE'
    assert to_dict(payments[3])['ground'] == 'Interest for las\r\nt period'
    assert to_dict(payments[3])['direction'] == 'OUT-CHARGE'
    assert to_dict(payments[4])['ground'] != ''

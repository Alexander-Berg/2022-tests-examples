from datetime import datetime
from decimal import Decimal

import pytest

from bcl.banks.protocols.swift.mt.exceptions import FieldValidationError
from bcl.banks.registry import Ing
from bcl.core.models import Currency, StatementPayment, StatementRegister, PaymentsBundle, NEW, BCL_INVALIDATED
from bcl.core.views.rpc import Rpc
from bcl.exceptions import ValidationError, UserHandledException


class TestStatementParser:

    def test_basic(self, read_fixture, get_statement, get_assoc_acc_curr):
        associate, account, _ = get_assoc_acc_curr(Ing.id, account='40702810300001005386')

        body = read_fixture('ing_final.txt')

        statement = get_statement(body, Ing.id)

        parser = Ing.statement_dispatcher.get_parser(statement)

        parser.validate_permissions([account], final=True)
        result = parser.process()

        assert len(result) == 1

        reg, payments = result[0]

        assert len(payments) == 2
        assert payments[0].number == 'Некийтекст'

    def test_check_process(self, parse_statement_fixture):
        _, payments = parse_statement_fixture(
            'ing_final.txt', Ing, '40702810300001005386', 'RUB',
            encoding=Ing.statement_dispatcher.parsers[0].encoding)[0]

        for p in payments:
            assert p.associate_id == Ing.id

    def test_statement_balance_check(self, parse_statement_fixture):
        """Проверяет сверку начального сальдо текущей выписики с конечным предыдущей."""
        def replace_and_parse(date, opening_balance, closing_balance):
            def replace(text):
                text = text.replace('8279239,85', closing_balance)
                text = text.replace('8291733,87', opening_balance)
                text = text.replace('170127', date)
                return text

            parse_statement_fixture(
                fixture='ing_final.txt', encoding=Ing.statement_dispatcher.parsers[0].encoding, mutator_func=replace,
                associate=Ing, acc='40702810300001005386', curr='RUB'
            )

        replace_and_parse(date='170127', opening_balance='8291733,87', closing_balance='8279239,85')
        replace_and_parse(date='170128', opening_balance='8279239,85', closing_balance='8266745,83')

        with pytest.raises(ValidationError):
            replace_and_parse(date='170129', opening_balance='12494,02', closing_balance='0,00')

    def test_statement_check_end_amount(self, parse_statement_fixture):
        """Проверяет сверку начального сальдо текущей выписики с конечным"""
        def replace(text):
            return text.replace('8279239', '8279236')

        register, payments = parse_statement_fixture(
            fixture='ing_final.txt', encoding=Ing.statement_dispatcher.parsers[0].encoding, mutator_func=replace,
            associate=Ing, acc='40702810300001005386', curr='RUB'
        )[0]
        statement = register.statement
        assert statement.status == StatementRegister.STATUS_ERROR
        assert not register.is_valid

    def test_statement_parse_intraday(self, parse_statement_fixture):
        register, payments = parse_statement_fixture(
            'ing_ru_mt942-intraday.txt', Ing, '40702810300001003838', 'RUB',
            encoding=Ing.statement_dispatcher.parsers[0].encoding
        )[0]
        statement = register.statement

        assert len(payments) == 1
        assert register.is_valid
        assert statement.type == statement.TYPE_INTRADAY

        payment = payments[0]  # type: StatementPayment
        assert payment.get_info_account() == '30114978400001000417'
        assert 'Комиссии третьих банков за валютный перевод' in payment.get_info_purpose()

    def test_unrecognized_account(self, parse_statement_fixture):

        with pytest.raises(UserHandledException) as e:
            parse_statement_fixture(
                'ing_final.txt', Ing, '', 'RUB', encoding=Ing.statement_dispatcher.parsers[0].encoding)

        assert 'Счёт 40702810300001005386 не зарегистрирован' in e.value.msg

    def test_intraday_zero_payment(self, parse_statement_fixture):
        register, payments = parse_statement_fixture(
            'ing_rub_intraday_zero_payment.txt', Ing, '40702810300001003838', 'RUB',
            encoding=Ing.statement_dispatcher.parsers[0].encoding
        )[0]
        statement = register.statement

        assert statement.type == statement.TYPE_INTRADAY
        assert len(payments) == 1
        assert register.is_valid

        payment = payments[0]  # type: StatementPayment
        assert payment.summ == Decimal(0)

    def test_statement_parse_allday_empty(self, parse_statement_fixture):
        register, payments = parse_statement_fixture(
            'ing_ru_mt940_empty.txt', Ing, '40702810800001005530', 'RUB',
            encoding=Ing.statement_dispatcher.parsers[0].encoding
        )[0]

        assert len(payments) == 0

        assert register.opening_balance == Decimal('472003.94')
        assert register.closing_balance == Decimal('472003.94')

        assert register.is_valid

    def test_statement_parse_many_acc(self, parse_statement_fixture):

        result = parse_statement_fixture(
            'ing_ru_many.txt', Ing, [
                '40702978600091005527',
                '40702978600091005530',
                '40702978000001005379',
                '40702978700001005378',
                '40702978600091005378',
            ], 'RUB',
            encoding=Ing.statement_dispatcher.parsers[0].encoding
        )

        assert len(result) == 5  # счетов/реестров

        # Количество платежей и валидность реестров.
        pay_counts = [2, 1, 0, 0, 1]

        for idx, pay_count in enumerate(pay_counts):
            assert len(result[idx][1]) == pay_count
            assert result[idx][0].is_valid

    def test_charge_payment(self, read_fixture, get_statement, get_assoc_acc_curr):
        """Подробнее в BCL-687"""
        associate, account, _ = get_assoc_acc_curr(Ing.id, account='40702810300001003838')

        body = read_fixture('ing_charge_payment.txt')

        statement = get_statement(body, Ing.id)

        parser = Ing.statement_dispatcher.get_parser(statement)

        parser.validate_permissions([account], final=True)
        result = parser.process()

        assert len(result) == 1

        reg, payments = result[0]

        assert len(payments) == 1
        assert '-CHARGE' in Rpc.to_dict_proved_pay(payments[0])['direction']

    def test_statement_missing_tag(self, parse_statement_fixture):
        with pytest.raises(FieldValidationError):
            parse_statement_fixture(
                'ing_missing_tag.txt', Ing, '40702810300001003838', 'RUB'
            )

    def test_statement_86tag_start_line_at_colon(self, parse_statement_fixture):
        reg, pays = parse_statement_fixture(
            'ing_incorrect_tag_86.txt', Ing, '40702810300001003838', 'RUB',
            encoding=Ing.statement_dispatcher.parsers[0].encoding
        )[0]

        assert reg.status == 1
        assert ':3:' in pays[0].info['04']


class TestPaymentCreator:

    def test_payment_creator(self, get_payment_bundle, get_source_payment):

        bundle = get_payment_bundle([get_source_payment()])
        creator = Ing.payment_dispatcher.get_creator(bundle)
        compiled = creator.create_bundle()

        expected = '\r\n'.join((
            f':01:{bundle.number}', ':02:152.00', ':03:1', ':04:', ':05:', ':06:', f':07:mt103_{bundle.number}.txt',
            ':20:1', ':23B:LCY', f":32A:{datetime.now().strftime('%y%m%d')}RUB152.00", ':50K:OOO Яндекс', '',
            ':52A:/D/40702810800000007671', '044525700', ':57A:044525593', ':59:/40702810301400002360',
            'ООО "Кинопортал"', '', ':70:{22534}Назначение', ':72:/PRIORITY/5/T/1/ICD/', '/TAX//INN/7725713770',
            '/OKPP/123456789/BKPP/987654321', '/CBC//OKATO/7766', '/BASIS//PERIOD/5', '/NO//DATE//TYPE/', ':77B:/CODE/'
        ))
        assert expected in compiled

    def test_create_tax_payment(self, get_payment_bundle, get_source_payment):

        creator = Ing.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment(
            attrs={
                'n_kbk': '1', 'n_okato': '45383000', 'n_status': '02', 'n_ground': 'ТП', 'n_period': 'МС.08.2020',
                'n_doc_num': '0', 'n_type': '0', 'n_doc_date': '31-08-2020'
            }
        )]))
        compiled = creator.create_bundle()

        assert 'DATE/200831' in compiled
        assert ':59:/' in compiled

    @pytest.mark.parametrize('income_type', [None, '', '2'])
    def test_income_type(self, income_type, get_payment_bundle, get_source_payment):

        creator = Ing.payment_dispatcher.get_creator(
            get_payment_bundle(
                [get_source_payment(attrs={'income_type': income_type} if income_type is not None else {})]
            )
        )
        compiled = creator.create_bundle()
        income_type = income_type or ''

        assert f':72:/PRIORITY/5/T/1/ICD/{income_type}' in compiled

    def test_compose_payment_foreign_dict_default_paid_by(self, get_payment_bundle, get_source_payment):

        payment = get_source_payment(attrs={'paid_by': None, 'contract_num': 'Номер контракта'})
        payment_dict = Ing.payment_dispatcher.get_creator(get_payment_bundle([payment])).compose_payment_foreign_dict(payment)

        assert 'OUR' in payment_dict['71A']

    def test_foreign(self, get_payment_bundle, get_source_payment):

        attrs = {
            'f_acc': '40702840800000007671',
            'currency_id': Currency.USD,
            'contract_currency_id': Currency.USD,
            'ground': 'some',
        }

        payment_raw = Ing.payment_dispatcher.get_creator(
            get_payment_bundle([get_source_payment(attrs=attrs)])).create()

        assert '/CCYAMT/USD300.3' in payment_raw

    def test_payment_validation(
            self, get_payment_bundle, get_source_payment, get_source_payment_dict, get_assoc_acc_curr):

        _, account, _ = get_assoc_acc_curr(Ing, account='123456')

        attrs = get_source_payment_dict({
            'currency_id': Currency.USD, 'ground': 'some',
            'f_acc': account.number, 'currency_op_docs': ''}, associate=Ing, number=1)

        incorrect_payment = get_source_payment(attrs)

        with pytest.raises(ValidationError) as e:
            Ing.payment_dispatcher.bundle_compose([str(incorrect_payment.id)])

        assert 'Документы валютной операции' in e.value.msg
        assert incorrect_payment.status == NEW

        incorrect_payment = get_source_payment(attrs)

        attrs.update({
            'currency_op_docs': '2',
            'number': '2',
            'number_src': '2-1c',
            'expected_dt': datetime(2021, 6, 5),
            'advance_return_dt': datetime(2021, 6, 5),
        })
        payment = get_source_payment(attrs)
        bundle = get_payment_bundle([payment, incorrect_payment], associate=Ing)
        Ing.payment_dispatcher.get_creator(bundle).create_bundle()

        bundle = PaymentsBundle.objects.get(id=bundle.id)
        payment.refresh_from_db()
        incorrect_payment.refresh_from_db()

        assert bundle.payments_count == 1
        assert incorrect_payment.status == BCL_INVALIDATED
        assert payment.status == NEW

        bundle = get_payment_bundle([payment], associate=Ing)
        compiled = Ing.payment_dispatcher.get_creator(bundle).create_bundle()
        assert '/EXPD/05.06.2021' in compiled
        assert '/DOC/2' in str(compiled)
        assert len(bundle.payments) == 1
        assert payment in bundle.payments

    def test_rub_payment_validation(self, get_payment_bundle, get_source_payment):

        payment = get_source_payment({'currency_id': Currency.RUB, 'currency_op_docs': ''}, associate=Ing)

        bundle = get_payment_bundle([payment], associate=Ing)
        compiled = Ing.payment_dispatcher.get_creator(bundle).create_bundle()
        assert '/DOC/' not in str(compiled)
        assert len(bundle.payments) == 1
        assert payment in bundle.payments


def test_get_non_service_accounts(get_assoc_acc_curr):

    get_assoc_acc_curr(Ing, account='1'*20)
    get_assoc_acc_curr(Ing, account='2'*20)
    get_assoc_acc_curr(Ing, account='USD.' + '3'*20)

    accounts = list(Ing.get_accounts(include_service=False))

    assert len(accounts) == 2
    for acc in accounts:
        assert acc.number[0].isdigit()

from datetime import datetime, timedelta

import pytest

from bcl.banks.registry import Tinkoff
from bcl.core.models import states, Payment, StatementPayment, Currency, PaymentsBundle
from bcl.exceptions import UserHandledException


def test_payment_creator(get_payment_bundle, get_source_payment, read_fixture):

    get_creator = Tinkoff.payment_dispatcher.get_creator

    attrs = {}

    compiled = get_creator(get_payment_bundle([get_source_payment(attrs)])).create_bundle()

    def check_by_line(filename):
        expected_text = read_fixture(filename).decode('cp1251').replace(
            '17.07.2019', datetime.now().strftime('%d.%m.%Y')
        )

        for line in expected_text.split('\r\n'):
            assert line in compiled

    check_by_line('expected_payment.txt')
    assert len(compiled.split('\r\n')) == 43
    assert 'Очередность=' in compiled

    attrs.update({
        'payout_type': Payment.PAYOUT_TYPE_TINKOFF_REGISTRY,
    })

    compiled = get_creator(get_payment_bundle([get_source_payment(attrs)])).create_bundle()

    check_by_line('expected_registry.txt')
    assert len(compiled.split('\r\n')) == 19
    assert 'Очередность=' not in compiled


def test_payment_validation(get_payment_bundle, get_source_payment, sitemessages):
    get_creator = Tinkoff.payment_dispatcher.get_creator
    incorrect_payment = get_source_payment({'currency_id': Currency.USD, 'ground': 'Назначение платежа'})
    bundle = get_payment_bundle([get_source_payment({}), incorrect_payment])

    get_creator(bundle).create_bundle()

    bundle = PaymentsBundle.objects.get(id=bundle.id)
    incorrect_payment.refresh_from_db()

    assert len(bundle.payments) == 1
    assert incorrect_payment.status == states.BCL_INVALIDATED
    assert 'Кириллица не допускается в валютных платежах' in incorrect_payment.processing_notes

    messages = sitemessages()
    assert len(messages) == 1
    subject = messages[0].context['subject']
    assert 'Тинькофф' in subject
    assert 'Информация по обработке платежей' in subject


def test_statement_parser(parse_statement_fixture, get_source_payment):

    # Выписка на суммовой платёж.
    register, payments = parse_statement_fixture(
        'tinkoff_statement_sum.txt', Tinkoff, '40702810500001400742', 'RUB')[0]

    assert not register.intraday
    assert len(payments) == 1

    args = {
        'number': '10',
        'f_acc': '123123',
        'summ': '100',
        'date': '2018-06-20',
    }
    payment1 = get_source_payment(args, associate=Tinkoff)

    args.update({
        'number': '12',
        'summ': '200',
    })
    payment2 = get_source_payment(args, associate=Tinkoff)

    # Выписка на веерные платежи.
    register, payments = parse_statement_fixture('tinkoff_statement_fan.json', Tinkoff, '123123', 'RUB')[0]

    assert register.intraday
    assert register.is_valid
    assert len(payments) == 2

    payment1.refresh_from_db()
    payment2.refresh_from_db()

    proved1, proved2 = payments  # type: StatementPayment
    proved1.refresh_from_db()
    proved2.refresh_from_db()

    assert proved1.payment == payment1
    assert proved1.get_info_inn() == '1234567890'
    assert proved1.get_info_bik() == '044525974'

    assert proved2.payment == payment2
    assert proved2.get_info_inn() == '0987654321'
    assert proved1.get_info_bik() == '044525974'

    assert payment1.is_complete
    assert payment2.is_declined_by_bank
    assert not payment1.processing_notes
    assert payment2.processing_notes == 'some unhandled error'


def test_check_data_statement(parse_statement_fixture):
    def replace_date(text):
        return text.replace('30.06.2018', (datetime.now() + timedelta(days=1)).strftime('%d.%m.%Y'))

    register, payments = parse_statement_fixture(
        'tinkoff_tomorrow_statement.txt', Tinkoff, '40702810138110105942', 'RUB', mutator_func=replace_date,
        encoding=Tinkoff.statement_dispatcher.parsers[0].encoding
    )[0]
    statement = register.statement
    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert register.statement_date == (datetime.now() + timedelta(days=1)).date()


def test_statement_check_end_amount(parse_statement_fixture):
    """Проверяет сверку начального сальдо текущей выписики с конечным"""

    def replace_end_amount(text):
        return text.replace('10002', '10009')

    register, payments = parse_statement_fixture(
        'tinkoff_tomorrow_statement.txt', Tinkoff, '40702810138110105942', 'RUB', mutator_func=replace_end_amount,
        encoding=Tinkoff.statement_dispatcher.parsers[0].encoding
    )[0]
    statement = register.statement
    assert statement.status == states.ERROR
    assert not register.is_valid


def test_selfemployed(check_selfemployed):
    associate = Tinkoff

    selfempl = Payment.PAYOUT_TYPE_SELFEMPLOYED

    doubled = {
        'payout_type': selfempl, 't_name': 'Некий Кто То', 'payroll_num': '1020',
        't_acc': '200',
        't_fio': 'Некий|Кто|То',
    }

    payments, bundle = check_selfemployed(
        associate, payment_dicts=[doubled, doubled, doubled]
    )
    assert len(payments) == 13

    for pay in payments:
        assert pay.status == pay.payment.status == states.COMPLETE

    bundle.refresh_from_db()

    for payment in bundle.payments:

        if payment.t_fio == 'Некий|Кто|То':
            assert payment.is_complete

        if payment.t_fio == 'Иванов|Иван|Иванович3':
            assert payment.is_declined_by_bank
            assert payment.error_code == (
                'Не удалось добавить получателя в Выплаты: проверьте ФИО, номер счета или наличие карты, на которую '
                'можно платить'
            )

    def mutate_fixture(text: str) -> str:
        return text.replace('РасчетныйСчетОрганизации="000777"', '', 1)

    with pytest.raises(UserHandledException) as e:
        check_selfemployed(associate, func_mutate_fixture=mutate_fixture)

    assert 'не содержит информации о счёте' in f'{e.value}'


def test_selfemployeed_compiled(get_salary_contract, build_payment_bundle):
    selfempl = Payment.PAYOUT_TYPE_SELFEMPLOYED
    payment_params = {
        'payout_type': selfempl, 't_name': 'Иванов Иван Иванович ', 'ground': 'тестовое назначение',
        't_fio': 'Иванов|Иван|Иванович'
    }
    bundle = build_payment_bundle(
        associate=Tinkoff, account={'number': 'TECH000777'}, payment_dicts=[payment_params]
    )
    compiled = bundle.tst_compiled
    assert '<НазначениеПлатежа>тестовое назначение</НазначениеПлатежа>' in compiled
    assert 'КодВидаДохода' not in compiled
    assert 'КодВО' not in compiled

    payment_params.update({'income_type': '3', 't_resident': '1', 'oper_code': '12345'})

    bundle = build_payment_bundle(
        associate=Tinkoff, account={'number': 'TECH000777'}, payment_dicts=[payment_params]
    )
    compiled = bundle.tst_compiled
    assert '<НазначениеПлатежа>тестовое назначение</НазначениеПлатежа>' in compiled
    assert '<КодВидаДохода>3</КодВидаДохода>' in compiled
    assert 'КодВО' not in compiled

    payment_params.update({'income_type': '3', 't_resident': '0', 'oper_code': '12345'})

    bundle = build_payment_bundle(
        associate=Tinkoff, account={'number': 'TECH000777'}, payment_dicts=[payment_params]
    )
    compiled = bundle.tst_compiled
    assert '<НазначениеПлатежа>тестовое назначение</НазначениеПлатежа>' in compiled
    assert '<КодВидаДохода>3</КодВидаДохода>' in compiled
    assert '<КодВО>12345</КодВО>' in compiled


def test_partitial_selfemployed(build_payment_bundle):
    selfempl = Payment.PAYOUT_TYPE_SELFEMPLOYED
    pay_dict = [
        {
            'payout_type': selfempl, 't_name': 'Иванов Иван Иванович ', 'payroll_num': '1020',
            't_acc': '10',
            't_fio': 'Неиванов|Неиван|Неиванович',
            'income_type': '1',
            't_resident': '1',
            'oper_code': '12345',
            't_contract_type': '3',
        },
        {
            'payout_type': selfempl, 't_name': 'Иванов Иван Иванович ', 'payroll_num': '1020',
            't_acc': '40',
            't_fio': 'Иванов|Неиван|Неиванович',
            'income_type': '1',
            't_resident': '1',
            'oper_code': '12345',
            't_contract_type': '3',
        },
        {
            'payout_type': selfempl, 't_name': 'Петров Пётр  Петрович', 'payroll_num': '1020',
            't_acc': '20',
            't_fio': 'Непетров|Непётр|Непетрович',
            'income_type': '1',
            't_resident': '0',
            'oper_code': '12345',
            't_contract_type': '3',
        },
        {
            'payout_type': selfempl, 't_name': 'Ильин Илья Ильич', 't_fio': 'Ильин|Илья|Ильич',
            'payroll_num': '3040',
            't_acc': '30',
            'income_type': '1',
            't_resident': '1',
            'oper_code': '12345',
            't_contract_type': '3',
        },
        {
            'payout_type': selfempl, 't_name': 'Сидоров Сидор', 't_fio': 'Сидоров|Сидор|Сидорович',
            'payroll_num': '1020',
            'income_type': '3',
            't_resident': '1',
            'oper_code': '12345',
            't_contract_type': '3',
        },
        {
            'payout_type': selfempl, 't_name': 'Иванов Иван Иванович ', 'payroll_num': '1020',
            't_acc': '10',
            't_fio': 'Неиванов|Неиван|Неиванович',
            'income_type': '1',
            't_resident': '1',
            'oper_code': '12346',
            't_contract_type': '3',
        },
        {
            'payout_type': selfempl, 't_name': 'Иванов Иван Иванович ', 'payroll_num': '1020',
            't_acc': '10',
            't_fio': 'Неиванов|Неиван|Неиванович',
            'income_type': '1',
            't_resident': '1',
            'oper_code': '12345',
            't_contract_type': '1',
        },
    ]

    bundles = build_payment_bundle(associate=Tinkoff, account={'number': 'TECH000777'}, payment_dicts=pay_dict)

    assert len(bundles) == 6
    assert len(bundles[0].payments) == 2
    assert all([len(b.payments) == 1 for b in bundles[1:]])

import operator

import itertools
from collections import namedtuple
from unittest.mock import MagicMock

import pytest
from datetime import datetime, timedelta, date
from decimal import Decimal
from typing import List, Iterable

from bcl.banks.base import Associate
from bcl.banks.party_sber.payment_creator import SberFactorPaymentCreator
from bcl.banks.party_sber.statement_parser import SberFactorStatementParser
from bcl.banks.registry import Sber, VtbRu, SberSpb, SberKdr
from bcl.core.models import Service
from bcl.core.models import states, StatementPayment, Statement, Payment, Currency, PaymentsBundle, Direction
from bcl.core.views.rpc import Rpc
from bcl.exceptions import UserHandledException
from bcl.toolbox.xls import XlsReader

SBER_BANKS = [Sber, SberSpb, SberKdr]

SWIFT_DATA = {
    'OWHBDEFF': {
        'countryCode': 'DE', 'instName': 'VTB BANK (EUROPE) SE', 'addr': '60325 FRANKFURT AM MAIN RUESTERSTRASSE 7-9',
        'city': 'FRANKFURT AM MAIN', 'street': 'RUESTERSTRASSE 7-9'
    },

}


@pytest.mark.parametrize('associate', SBER_BANKS)
def test_salary_statements(associate, parse_statement_fixture, get_source_payment):

    def run(purpose, salary_id, *, linked=True):

        source_payment = get_source_payment({
            'payroll_num': '279131',
            'summ': '15.00' if linked else '14.00',
            'ground': purpose,
            'status': states.EXPORTED_H2H,

        }, associate=associate)

        associate.register_payment_hook_before_save(source_payment)
        source_payment.save()

        assert source_payment.salary_id == salary_id

        register, payments = parse_statement_fixture(
            'sber_salary_payments.txt', associate, '40702810500001400742', 'RUB',
            mutator_func=lambda text: text.replace('Налог на доходы физических лиц', purpose),
            encoding=associate.statement_dispatcher.parsers[0].encoding)[0]

        payment = payments[0]
        source_payment.refresh_from_db()

        assert len(source_payment.statementpayment_set.all()) == (1 if linked else 0)

        if linked:
            assert source_payment.statementpayment_set.all()[0].number == payment.number
        assert source_payment.status == (states.COMPLETE if linked else states.EXPORTED_H2H)

        register.statement.deep_delete()

    run('Для зачисления заработной платы по реестру 1136 от 12.10.2017 цель платежа 01 - '
        'Заработная плата в соответствии с Договором 38141087 от 09.06.2016', '1136')

    run('Заработная плата по реестру №1137 от 25.12.2017 в соответствии с Договором 38143037 от 16.06.2017 '
        'Для зачисления по реестру 100 UUID 846d29d66a794b44ad32325fc80ddc9e', '1137')

    run('Заработная плата по реестру №1137 от 25.12.2017 в соответствии с Договором 38143037 от 16.06.2017 '
        'Для зачисления по реестру 100 UUID 846d29d66a794b44ad32325fc80ddc9e', '1137', linked=False)


@pytest.mark.parametrize('associate', SBER_BANKS)
def test_charge_payment(associate, read_fixture, get_statement, get_assoc_acc_curr):
    """Подробнее в BCL-687"""
    _, account, _ = get_assoc_acc_curr(associate, account='40702810138110105942')

    body = read_fixture('sber_charge_payment.txt')

    statement = get_statement(body, associate)

    parser = associate.statement_dispatcher.get_parser(statement)

    parser.validate_permissions([account], final=True)
    result = parser.process()

    assert len(result) == 1

    reg, payments = result[0]

    assert len(payments) == 1
    assert '-CHARGE' in Rpc.to_dict_proved_pay(payments[0])['direction']


@pytest.mark.parametrize('associate', SBER_BANKS)
def test_statement_parser_intraday(associate, parse_statement_fixture, get_source_payment):
    source_payment = get_source_payment({
        'ground': 'Payment purpose',
        'summ': '5.00',
        'status': states.BUNDLED,
        'f_acc': '40702840500091003838',
        'number': '42975'

    }, associate=associate)

    def replace_date(text):
        return text.replace('11.07.2018', datetime.now().strftime('%d.%m.%Y'))

    register, payments = parse_statement_fixture(
        'sber_intraday.txt', mutator_func=replace_date, associate=associate,
        encoding=associate.statement_dispatcher.parsers[0].encoding, acc='40702840500091003838', curr='RUB'
    )[0]
    statement = register.statement
    source_payment.refresh_from_db()

    assert statement.type == statement.TYPE_INTRADAY
    assert register.is_valid
    assert len(payments) == 1
    assert len(StatementPayment.objects.all()) == 1
    assert source_payment.status == states.BUNDLED
    assert source_payment.statementpayment_set.all()[0] == payments[0]


@pytest.mark.parametrize('associate', SBER_BANKS)
def test_statement_parser_allday(associate, parse_statement_fixture, get_source_payment):

    source_payment = get_source_payment({
        'ground': 'Payment purpose',
        'summ': '5.00',
        'status': states.BUNDLED,
        'f_acc': '40702840500091003838',
        'number': '42975'

    }, associate=associate)

    def replace_date(text):
        return text.replace(
            'ДатаСоздания=11.07.2018', 'ДатаСоздания=' + datetime.now().strftime('%d.%m.%Y')).replace(
            '11.07.2018', (datetime.now() - timedelta(days=1)).strftime('%d.%m.%Y'))

    register, payments = parse_statement_fixture(
        'sber_intraday.txt', mutator_func=replace_date, associate=associate,
        encoding=associate.statement_dispatcher.parsers[0].encoding, acc='40702840500091003838', curr='RUB'
    )[0]
    statement = register.statement
    source_payment.refresh_from_db()

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 1
    assert len(StatementPayment.objects.all()) == 1
    assert source_payment.is_complete
    assert source_payment.statementpayment_set.all()[0] == payments[0]


@pytest.mark.parametrize('associate', SBER_BANKS)
def test_parse_mt(associate, parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'sber_mt940.txt', associate=associate, acc='40702933238001000124', curr='BYN'
    )[0]

    assert register.is_valid
    assert len(payments) == 2

    assert payments[0].get_info_purpose() == (
        "OPLATA ZA REKLAMNYE USLUGI ANDEKS.MARKET PO SCETU 'j'BR-943838132-1 'j'OT 28.06.2018 "
        "G. 'j(j'DOGOVOR OFERTY'j). j'KOD 'j'VO20100'j'")


@pytest.mark.parametrize('associate', SBER_BANKS)
def test_parse_from_other_associate(associate, parse_statement_fixture, get_assoc_acc_curr):

    get_assoc_acc_curr(VtbRu, account='40702933238001000124')

    results = parse_statement_fixture('sber_mt940.txt', associate=associate, acc='40702933238001000124', curr='BYN')

    assert not results

    statement = Statement.objects.all()[0]
    assert statement.status == states.ERROR
    assert statement.processing_notes == 'Нет данных для формирования регистров по счетам.'


@pytest.mark.parametrize('associate', SBER_BANKS)
def test_unrecognized_account(associate, parse_statement_fixture):

    with pytest.raises(UserHandledException) as e:
        parse_statement_fixture(
            'sber_charge_payment.txt', associate, '', 'RUB',
            encoding=associate.statement_dispatcher.parsers[0].encoding)

    assert 'Счёт 40702810138110105942 не зарегистрирован' in e.value.msg


def test_selfemployed(check_selfemployed, time_shift):

    with time_shift(60 * 60 * 24 * 2, backwards=True):
        payments, _ = check_selfemployed(Sber)

    # 2 так как один платеж уже был загружен предыдущей выпиской
    assert len(payments) == 2
    assert payments[0].status == payments[0].payment.status == states.COMPLETE
    assert payments[1].status == payments[1].payment.status == states.DECLINED_BY_BANK
    assert payments[1].problem == payments[1].payment.processing_notes == 'Что-то пошло не так'


def test_selfemployed_with_error(check_selfemployed):

    with pytest.raises(Exception) as e:
        payments, _ = check_selfemployed(Sber, file_alias='tinkoff')

    assert 'Количество записей в ответном реестре ID' in e.value.msg
    assert ' не совпадает с количеством в пакете платежей ID ' in e.value.msg


@pytest.mark.parametrize('associate', [Sber, SberKdr])
def test_currency(associate, build_payment_bundle, get_salary_contract, mocker):

    selfempl = Payment.PAYOUT_TYPE_SELFEMPLOYED

    get_salary_contract(associate, account='TECH000777', number='38143037')

    mocker.patch('bcl.toolbox.utils.Swift.get_bic_info', lambda swiftcode: SWIFT_DATA.get(swiftcode, {}))

    bundles = build_payment_bundle(associate=associate, account={'number': 'TECH000777'}, payment_dicts=[
        {
            'payout_type': selfempl,
            't_fio': 'Иванов|Иван|Иванович',
            't_acc': '10',
            'currency_id': Currency.USD,
            'ground': 'test',
            'income_type': '01',
        },
        {'payout_type': selfempl, 't_name': 'Иванов Иван Петрович ', 't_acc': '20'},
        {
            # Собственно валютный платёж.
            'f_name': 'Yandex Market',
            't_fio': 'Ivanov|Ivan|',
            't_address': 'Some address here',
            't_acc': '10203040501020304050',
            'currency_id': Currency.USD,
            'ground': 'currency payment test',
        },
        {'t_acc': '40'},
    ])

    idx_selfemployed = 0
    idx_foreign = 1
    idx_rubles = 2
    bundles_count_expected = 3

    if associate is SberKdr:
        idx_selfemployed = None
        idx_foreign = 0
        idx_rubles = 1
        bundles_count_expected = 2

    assert len(bundles) == bundles_count_expected

    if idx_selfemployed is not None:
        assert bundles[idx_selfemployed].tst_compiled.startswith('<?')
        assert '<КодВидаДохода>01</КодВидаДохода>' in bundles[idx_selfemployed].tst_compiled

    currency_bundle_contents = bundles[idx_foreign].tst_compiled
    assert currency_bundle_contents.startswith('docnum')
    assert 'USD	840	152.00	USD	840	152.00	OUR	Yandex Market	TECH000777' in currency_bundle_contents

    assert bundles[idx_rubles].tst_compiled.startswith('1C')


def test_selfemployeed_without_contract(build_payment_bundle, get_salary_contract):
    associate = Sber

    with pytest.raises(UserHandledException) as e:

        build_payment_bundle(
            associate=associate, account={'number': 'TECH000777'}, payment_dicts=[
                {
                    'payout_type': Payment.PAYOUT_TYPE_SELFEMPLOYED,
                    't_fio': 'Иванов|Иван|Иванович',
                }
            ]
        )
    assert 'не найден договор или найдено больше одного' in e.value.msg


def test_factoring_generated_file(build_payment_bundle, get_assoc_acc_curr, make_org):
    assoc: Associate = Sber
    org = make_org(name='ООО "Боб Марли Корпорейтед"', inn=7766699900)
    _, acc, _ = get_assoc_acc_curr(assoc, account='TECH12345666999', org=org)
    bundle = build_payment_bundle(assoc, account=acc, payment_dicts=[
        {
            'payout_type': Payment.PAYOUT_TYPE_FACTOR,
            'ground': (
                'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями '
                'Договора № 2937991/21 от 06.09.2020 по поручению ООО ЯНДЕКС.Перечисление денежных средств, полученных '
                'в связи с исполнением поручения в соответствии с условиями Договора № 2937991/21 от 06.09.2020 '
                'по поручению ООО ЯНДЕКС.'
            ),
            'summ': Decimal('123.45')
        }, {
            'payout_type': Payment.PAYOUT_TYPE_FACTOR,
            'summ': Decimal('567.89')
        }
    ])

    assert isinstance(bundle, PaymentsBundle)

    bundle_file = XlsReader.from_bytes(bundle.tst_compiled)
    rows = list(bundle_file.iter_rows())

    creator: SberFactorPaymentCreator = assoc.payment_dispatcher.get_creator(bundle)
    assert isinstance(creator, SberFactorPaymentCreator)
    assert len(rows) == len(creator.header) + 1 + bundle.payments.count() + len(creator.footer)

    first_data_row = rows[len(creator.header) + 1]
    assert bundle.payments.get(number=first_data_row[0]).ground == first_data_row[11]

    assert first_data_row[7] == '2937991/21'  # парсится из ground
    assert first_data_row[8] == '06.09.2020'  # парсится из ground

    assert rows[len(creator.header)][12] == 'Примечания'

    assert 'Боб Марли Корпорейтед' in rows[6][1] and 'Боб Марли Корпорейтед' in rows[17][7]
    assert '7766699900' in rows[6][1]


def test_different_destinations(build_payment_bundle, get_assoc_acc_curr):
    assoc = Sber
    _, acc, _ = get_assoc_acc_curr(assoc, account={'number': '11122333455556666666'})
    bundles = build_payment_bundle(
        assoc,
        account=acc,
        payment_dicts=[
            {'payout_type': Payment.PAYOUT_TYPE_FACTOR},
            {'payout_type': None, 'f_acc': '11122333455556666666', 't_ogrn': '1122334455667'}
        ]
    )

    assert len(bundles) == 2
    assert set([
        bundle.destination for bundle in bundles
    ]) == {bundles[0].DESTINATION_FACTOR, bundles[0].DESTINATION_ONLINE}


@pytest.mark.parametrize('ground, expected_contract_num, expected_contract_dt', (
    pytest.param('Договора № 2937991/21 от 06.09.2020 по поручению', '2937991/21', '06.09.2020', id='normal'),
    pytest.param('Договора № 2937991/21 по поручению',
                 '2937991/21', (datetime.now() - timedelta(days=1)).strftime('%d.%m.%Y'),
                 id='wo-date'),
    pytest.param('Договора №2937991/21   от    06.09.2020 по поручению', '2937991/21', '06.09.2020', id='spaces'),
    pytest.param('Договора № 2937991/21 от 06.09.202 по поручению', '2937991/21', '06.09.20', id='2digit-year'),
    pytest.param('Договора 2937991/21 от 06.09.20 по поручению', '', '', id='wo-№'),
    pytest.param('№ 2937991/21 от 06.09.2020 № 666666/21 от 66.66.6666 ', '2937991/21', '06.09.2020', id='first'),
    pytest.param('', '', '', id='empty'),
))
def test_contract_num_and_dt_from_ground(ground, expected_contract_num, expected_contract_dt):
    bundle_mock = MagicMock(dt=datetime.now())
    creator = SberFactorPaymentCreator(bundle_mock)

    actual = creator._get_first_contract_num_and_dt_from_ground(ground)
    assert actual == (expected_contract_num, expected_contract_dt)


def test_sber_factor_statement_parser(django_assert_num_queries, build_payment_bundle, parse_statement_fixture):
    assoc = Sber
    account: str = 'TECH40702810438000034726'
    bundle1_payments = [
        {'number': 544259, 't_name': 'ООО ХЬЮМАНФУД', 't_inn': '7814762666', 'summ': Decimal('61159.14'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 2666777/21 по поручению ООО ЯНДЕКС'}
    ]
    bundle1 = build_payment_bundle(assoc, account=account, payment_dicts=bundle1_payments)

    bundle2_payments = [
        {'number': 544243, 't_name': 'ООО Аквалор', 't_inn': '5005066662', 'summ': Decimal('5639.00'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 1846666/21 от 12.01.2019 по поручению ООО ЯНДЕКС'},
        {'number': 544244, 't_name': 'Индивидуальный предприниматель Васюнин Никита Иванович', 't_inn': '501304766669', 'summ': Decimal('8740.00'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 1966666/21 от 12.06.2019 по поручению ООО ЯНДЕКС'},
        {'number': 544245, 't_name': 'ООО НДС-ТРИКОТАЖ', 't_inn': '3702216666', 'summ': Decimal('4340.00'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 1998666/21 от 22.01.2020 по поручению ООО ЯНДЕКС'},
        {'number': 544246, 't_name': 'ООО ТД "ПОЛИ-ГЛОТ""', 't_inn': '7726666679', 'summ': Decimal('4190.00'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 1566687/21 от 14.11.2021 по поручению ООО ЯНДЕКС'},
        {'number': 544251, 't_name': 'ООО РЖЖД', 't_inn': '7447287666', 'summ': Decimal('15000.00'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 2089666/21 от 02.03.2020 по поручению ООО ЯНДЕКС'},
        {'number': 544252, 't_name': 'Индивидуальный предприниматель Савинов Махмуд Багирович', 't_inn': '165900135666', 'summ': Decimal('311.00'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 1817666/21 от 05.01.2018 по поручению ООО ЯНДЕКС'},
        {'number': 544253, 't_name': 'Индивидуальный предприниматель Иванов Иван Иванович', 't_inn': '780434455666', 'summ': Decimal('5685.00'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 2088666/21 от 30.05.2021 по поручению ООО ЯНДЕКС'},
        {'number': 544254, 't_name': 'Индивидуальный предприниматель Потапов Михаил Валерьевич', 't_inn': '780525346666', 'summ': Decimal('1833.00'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 1072666/20 от 15.02.2020 по поручению ООО ЯНДЕКС'},
        {'number': 544258, 't_name': 'Индивидуальный предприниматель Агаджанян Ашот Павлович', 't_inn': '770207781666', 'summ': Decimal('692.72'), 'ground': 'Перечисление денежных средств, полученных в связи с исполнением поручения в соответствии с условиями Договора № 3031666/22 от 18.09.2021 по поручению ООО ЯНДЕКС'},
    ]
    bundle2 = build_payment_bundle(assoc, account=account, payment_dicts=bundle2_payments)

    ExpectedPayment = namedtuple('ExpectedPayment', 'status problem direction remote_id payment_number date')
    expected_payments: List[ExpectedPayment] = [
        ExpectedPayment(states.COMPLETE, '', Direction.OUT, '1111', 544243, datetime(2022, 5, 16)),
        ExpectedPayment(states.COMPLETE, '', Direction.OUT, '1115', 544251, datetime(2022, 5, 16)),
        ExpectedPayment(states.COMPLETE, '', Direction.OUT, '1116', 544252, datetime(2022, 5, 16)),
        ExpectedPayment(states.COMPLETE, '', Direction.OUT, '1117', 544253, datetime(2022, 5, 16)),
        ExpectedPayment(states.COMPLETE, '', Direction.OUT, '1118', 544254, datetime(2022, 5, 16)),
        ExpectedPayment(states.COMPLETE, '', Direction.OUT, '1119', 544258, datetime(2022, 5, 16)),
        ExpectedPayment(states.COMPLETE, '', Direction.OUT, '1120', 544259, datetime(2022, 5, 15)),
        ExpectedPayment(states.RETURNED, '', Direction.IN, '1120', 544259, datetime(2022, 5, 15)),
    ]
    rejected_expected_payments = [
        ExpectedPayment(states.REVOKED, '[ERROR_1002] Creditor not valid', Direction.OUT, '1112', '544244', datetime(2022, 5, 16)),
        ExpectedPayment(states.REVOKED, '[ERROR_1005] Creditor not found', Direction.OUT, '1113', '544245', datetime(2022, 5, 16)),
        ExpectedPayment(states.DECLINED_BY_BANK, '[ERROR_1001] Отклонен банком', Direction.OUT, '1114', '544246', datetime(2022, 5, 16)),
    ]

    with django_assert_num_queries(38):
        res = parse_statement_fixture('factoring_statement.xls', Sber, account, 'RUB')

    assert len(res) == 1
    register, proved_pays = res[0]
    assert len(proved_pays) == len(expected_payments)
    assert register.statement.status == states.STATEMENT_PROCESSED
    assert register.is_valid

    assert register.statement_date == date(2022, 7, 10)

    # проверяем содержимое выписки
    for proved_pay, expected in zip(proved_pays, expected_payments):
        assert proved_pay.status == expected.status
        assert proved_pay.problem == expected.problem
        assert proved_pay.direction == expected.direction
        assert proved_pay.date == expected.date

        if expected.direction == Direction.OUT:
            assert proved_pay.payment is not None
            assert proved_pay.payment.remote_id == expected.remote_id
            assert proved_pay.payment.number == expected.payment_number

            if proved_pay.payment.status != states.RETURNED:
                # complete statementPayment может иметь returned платёж, проставленный возвратным statementPayment'ом
                assert proved_pay.payment.status == proved_pay.status
            else:
                # удостоверяемся, есть ли такой statementPayment
                assert StatementPayment.objects.filter(
                    number=proved_pay.payment.number,
                    direction=Direction.IN,
                    register__account__number=proved_pay.payment.f_acc,
                    status=states.RETURNED
                ).count() == 1

            assert proved_pay.payment.processing_notes == expected.problem
        else:
            assert proved_pay.payment is None
            assert proved_pay.status == states.RETURNED  # других таких быть не должно

            # ищем, проставился ли статус у Payment
            assert Payment.objects.filter(
                number=proved_pay.number,
                f_acc=proved_pay.register.account.number,
                status=states.RETURNED
            ).count() == 1

    # проверяем проигноренные строки, синхронизированы ли статусы
    for expected in rejected_expected_payments:
        actual = Payment.objects.get(number=expected.payment_number, f_acc=account)
        assert actual.status == expected.status
        assert actual.processing_notes == expected.problem
        assert actual.remote_id == expected.remote_id
        assert actual.statementpayment_set.count() == 0

    # проверяем суммы в регистре
    def get_sums_by_direction(elements: Iterable, direction_getter, sum_getter):
        result = {
            direction: sum(Decimal(sum_getter(elem)) for elem in group)
            for direction, group in itertools.groupby(
                sorted(elements, key=direction_getter),
                direction_getter
            )
        }
        return result

    attr = operator.attrgetter
    proved_pays_sum = get_sums_by_direction(proved_pays, attr('direction'), attr('summ'))

    assert register.debet_turnover == proved_pays_sum[Direction.OUT]
    assert register.credit_turnover == proved_pays_sum[Direction.IN]


@pytest.mark.parametrize('value, expected', (
    (44767.0, datetime(2022, 7, 25)),
    ('10.07.2022', datetime(2022, 7, 10)),
))
def test_excel_date_to_datetime(value, expected):
    xls = MagicMock()
    xls.book.datemode = 0

    actual = SberFactorStatementParser.get_date_from_excel_value(xls, value)
    assert actual == expected

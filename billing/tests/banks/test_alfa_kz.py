import re
from datetime import datetime
from decimal import Decimal

import pytest

from bcl.banks.registry import AlfaKz
from bcl.core.models import PaymentsBundle, StatementPayment, Currency, BCL_INVALIDATED
from bcl.exceptions import ValidationError


@pytest.mark.parametrize('empty_field', ['f_kbe', 't_kbe', 'knp', 't_bankname'])
def test_payment_creator(empty_field, get_payment_bundle, get_source_payment):

    associate = AlfaKz

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


@pytest.mark.parametrize('empty_field', ['write_off_account', 'head_of_company', 'knp', 't_bankname'])
def test_payment_validation(empty_field, get_payment_bundle, get_source_payment, get_source_payment_dict, sitemessages):

    attrs = get_source_payment_dict({
        'f_iban': '123456',
        't_iban': '654321',
        'f_swiftcode': '1234567890',
        't_swiftcode': '0987654321',
        'knp': '119',
        'currency_id': Currency.by_code['USD'],
        'f_kbe': '12',
        't_kbe': '21',
        't_country': '643',
        'ground': 'Ground',
        empty_field: ''
    }, associate=AlfaKz, number=1)

    incorrect_payment = get_source_payment(attrs)

    with pytest.raises(ValidationError) as e:
        AlfaKz.payment_dispatcher.get_creator(get_payment_bundle([incorrect_payment])).create_bundle()

    incorrect_payment.refresh_from_db()

    assert empty_field in e.value.msg
    assert incorrect_payment.status == BCL_INVALIDATED

    incorrect_payment = get_source_payment(attrs)

    attrs.update({
        'write_off_account': 'KZ549470398990511652',
        'head_of_company': 'Шапиев Тамирлан Жандосулы',
        'knp': '13',
        't_bankname': 'Alfa',
        'number': '2',
        'number_src': '2-1c',
        't_acc': '123456789012345678901',
    })
    payment = get_source_payment(attrs)
    bundle = get_payment_bundle([payment, incorrect_payment])
    compiled = AlfaKz.payment_dispatcher.get_creator(bundle).create_bundle()

    bundle = PaymentsBundle.objects.filter(id=bundle.id)[0]
    incorrect_payment.refresh_from_db()

    assert b'BEN_BNK_NAME' in compiled
    assert b'BEN_BNK_ACC/123456789012345678901' in compiled
    assert b'BEN_ACC_CODE/123456789012345678901' in compiled
    assert len(bundle.payments) == 1
    assert payment in bundle.payments
    assert incorrect_payment.status == BCL_INVALIDATED

    messages = sitemessages()
    assert len(messages) == 2
    subject = messages[0].context['subject']
    assert 'Альфа-Банк Казахстан' in subject
    assert 'Информация по обработке платежей' in subject


def test_create_multicurrency_bundle(get_source_payment):

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
    payment_kzt = get_source_payment(attrs, associate=AlfaKz)
    payment_kzt2 = get_source_payment(attrs, associate=AlfaKz)
    attrs.update({'currency_id': Currency.by_code['USD']})
    payment_usd = get_source_payment(attrs, associate=AlfaKz)
    bundles = AlfaKz.payment_dispatcher.partition_payments([payment_kzt, payment_usd, payment_kzt2])
    assert len(bundles) == 2
    assert [payment_usd] in bundles


statement_params = [
    dict(
        associate=AlfaKz, encoding=AlfaKz.statement_dispatcher.parsers[0].encoding,
        acc='KZ123456789012345678', curr='KZT'
    ),
]


@pytest.mark.parametrize('statement_params', statement_params)
def test_statement_parser_allday(statement_params, parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement_alfa_kz.txt', **statement_params
    )[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 2


@pytest.mark.parametrize('statement_params', statement_params)
def test_statement_parser_intraday(statement_params, parse_statement_fixture):

    def replace_date(text):
        return text.replace('02.03.2017', datetime.now().strftime('%d.%m.%Y'))

    associate = statement_params['associate']

    register, payments = parse_statement_fixture(
        'statement_alfa_kz_intra_1.txt', mutator_func=replace_date, **statement_params
    )[0]
    statement = register.statement

    assert statement.type == statement.TYPE_INTRADAY
    assert register.is_valid
    assert len(payments) == 1
    assert len(StatementPayment.objects.all()) == 1

    register, payments = parse_statement_fixture(
        'statement_alfa_kz_intra_2.txt',
        mutator_func=replace_date,
        associate=associate,
        encoding=associate.statement_dispatcher.parsers[0].encoding,
        acc='KZ669470643000313623',
        curr='KZT'
    )[0]
    statement = register.statement

    assert statement.type == statement.TYPE_INTRADAY
    assert register.is_valid
    assert len(payments) == 1
    assert len(StatementPayment.objects.all()) == 2


@pytest.mark.parametrize('statement_params', statement_params)
def test_statement_intraday_zero_payment(statement_params, parse_statement_fixture):

    def replace_date(text):
        return text.replace('29.06.2018', datetime.now().strftime('%d.%m.%Y'))

    register, payments = parse_statement_fixture(
        'alfa_kzt_intraday_zero_amount.txt', mutator_func=replace_date, **statement_params
    )[0]
    statement = register.statement

    assert statement.type == statement.TYPE_INTRADAY
    assert register.is_valid
    assert len(payments) == 1
    assert len(StatementPayment.objects.all()) == 1
    assert payments[0].summ == Decimal(0)


@pytest.mark.skip('Сверка сальдо загружаемой и предыдущей выписок отключена для Альфабанка Казахстана')
def test_statement_balance_check(parse_statement_fixture):
    """Проверяет сверку начального сальдо текущей выписики с конечным предыдущей."""

    def replace_and_parse(statement_date, opening_balance, closing_balance):
        def replace(text):
            params = {
                'ДатаНачала': statement_date,
                'ДатаКонца': statement_date,
                'ДатаОперации': statement_date,
                'НачальныйОстаток': opening_balance,
                'КонечныйОстаток': closing_balance
            }
            for param, value in params.items():
                text = re.sub(r'({}=)[^\s]*'.format(param), r'\g<1>{}'.format(value), text)
            return text

        return parse_statement_fixture(
            'statement_alfa_kz.txt', AlfaKz, 'KZ123456789012345678', 'KZT', mutator_func=replace, encoding='utf-8'
        )

    replace_and_parse(statement_date='03.03.2017', opening_balance='0.12', closing_balance='2.12')
    replace_and_parse(statement_date='04.03.2017', opening_balance='2.12', closing_balance='4.12')

    with pytest.raises(ValidationError):
        replace_and_parse(statement_date='05.03.2017', opening_balance='25.33', closing_balance='27.33')


def test_international_payment_creator(get_source_payment, get_payment_bundle):
    payment = get_source_payment(attrs={
        'number': '4444',
        'knp': '33',
        'f_kbe': '12',
        't_kbe': '14',
        'ground': '(VO20101) oplata po schety 20090917/01 ot 08/12/21',
        'head_of_company': 'ИВАНОВ ИВАН ИВАНЫЧ',
        'write_off_account': '0000000000000003',
        'date': datetime(2017, 12, 8),
        't_country': '643',
        't_bankname': 'Наименование банка, в котором более 35 символов не должно разбиваться по строкам',
    })

    result = AlfaKz.payment_dispatcher.get_creator(get_payment_bundle([payment]))

    # ВНИМАНИЕ Строки ниже содержат неразрывные пробелы
    expected = '\r\n'.join((
        '{1:F01ALFAKZKAAXXX0000000000}',
        '{2:OK06CHASUS33XXXXN}',
        '{4:',
        ':50:',
        '/ACC_CODE/0000000000000001',
        '/BNK_CODE/ALFAKZKAXXX',
        '/COMIS_ACC_CODE/0000000000000003',
        '/CLI_NAME/БИН7705713772 OOO Яндекс',
        '/CODE_OD/',
        '/TXT_HEAD/ИВАНОВ ИВАН ИВАНЫЧ',
        '/TXT_BUCH/',
        '/RNN_PAY/',
        ':32A:20171208RUB152,00',
        ':56:',
        '/MID_BNK_CODE/',
        '/MID_BNK_NAME/',
        '/MID_ACC_CODE/',
        ':57:',
        '/BEN_BNK_CODE/044525593',
        '/BEN_BNK_ACC/40702810301400002360',
        '/BEN_BNK_NAME/Наименование банка, в котором более 35 символов не должно разбиваться по строкам',
        ':59:',
        '/BEN_ACC_CODE/40702810301400002360',
        '/BEN_NAME/ИНН7725713770.КПП987654321 ООО "Кинопортал" г Москва, ул. Краснопролетарская, д. 1, стр. 3',
        '/CODE_BE/14',
        ':70:',
        '/TXT_DSCR/(VO20101) oplata po schety 20090917/01 ot 08/12/21',
        '/KNP/33',
        '/KBE/12',
        '/CODE_COMM/OUR',
        '/BEN_LND/RU',
        '/CON_CODE/',
        '/CON_DATE/',
        '/CON_CODE_PASS/',
        '/NUM/4444',
        ':90:/URGENTFL/',
        '/COMISFL/',
        '/TRNLIT/1',
        '-}',
        ''
    ))

    current = result.create_bundle().decode('cp1251')

    assert current == expected

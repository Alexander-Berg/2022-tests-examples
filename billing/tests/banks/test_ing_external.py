import re
from datetime import datetime
from functools import partial
from typing import *

import pytest

from bcl.banks.protocols.swift.mt.exceptions import FieldValidationError
from bcl.banks.registry import IngCh, IngNl, Seb, IngRo, IngDe, IngTr
from bcl.core.models import Currency, states, PaymentsBundle, StatementRegister, StatementPayment, BCL_INVALIDATED
from bcl.exceptions import BclException, ValidationError

SWIFT_DATA = {
    'HGFEDCBAXXX': {
        'countryCode': 'DE', 'instName': 'VTB BANK (EUROPE) SE', 'addr': '60325 FRANKFURT AM MAIN RUESTERSTRASSE 7-9',
        'city': 'FRANKFURT AM MAIN', 'street': 'RUESTERSTRASSE 7-9'
    },
    'NBFAUZ2XXXX': {
        'countryCode': 'UZ', 'instName': 'NATIONAL BANK FOR FOREIGN ECONOMIC ACTIVITY OF THE REPUBLIC OF UZBEKISTAN',
        'addr': '100047 TASHKENT INTERBANK SETTLEMENT CENTRE ISTIQBOL STR 23', 'city': 'TASHKENT',
        'street': 'INTERBANK SETTLEMENT CENTRE'
    },
    'INGBROBU': {
        'countryCode': 'RO', 'instName': 'ING BANK N.V., BUCHAREST BRANCH',
        'addr': '012095 BUCHAREST EXPO BUSINESS PARK NO. 3 AVIATOR POPISTEANU STREET 54A', 'city': 'BUCHAREST',
        'street': 'EXPO BUSINESS PARK'
    },
    'DEUTGB2LXXX': {
        'countryCode': 'GB', 'instName': 'DEUTSCHE BANK AG',
        'addr': 'LONDON EC2N 2DB WINCHESTER HOUSE 1 GREAT WINCHESTER STREET', 'city': 'LONDON',
        'street': 'WINCHESTER HOUSE'
    },
    'RNCBROBUXXX': {
        'countryCode': 'RO', 'instName': 'BANCA COMERCIALA ROMANA S.A',
        'addr': '060013 BUCHAREST BUSINESS GARDEN BUCHAREST BUILDING A FLOOR 6 CALEA PLEVNEI 159', 'city': 'BUCHAREST',
        'street': 'BUSINESS GARDEN BUCHAREST'
    },
}

def test_seb(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement_ing_seb.txt', Seb, '33010006105035', 'EUR'
    )[0]  # type: StatementRegister, List[StatementPayment]

    assert register.is_valid
    assert not payments


def test_statement_parser(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement_ing_external_mt940.txt', IngCh, 'NL35INGB0650977351', 'EUR'
    )[0]  # type: StatementRegister, List[StatementPayment]

    assert len(payments) == 13
    assert register.is_valid
    assert register.statement_date.isoformat() == '2017-08-15'

    assert 'HOTELS COMBINED PTY LTD' in payments[0].get_info_purpose()
    assert 'TRANS. REFERENCE: REF2441185' in payments[1].get_info_purpose()
    assert 'Invoice N TNV015235 dd 12.05.2017.' in payments[12].get_info_purpose()

    assert payments[10].get_info_account() == ''
    assert payments[10].get_info_bik() == 'BARCGB22XXX'
    assert payments[10].get_info_name() == 'UNIQUE DIGITAL MARKETING LIMITED'

    assert payments[12].get_info_account() == 'FI2310183000074064'
    assert payments[12].get_info_bik() == 'NDEAFIHH'
    assert payments[12].get_info_name() == 'Tournorruss OY'


def test_statement_parser_many(parse_statement_fixture, get_assoc_acc_curr):

    get_assoc_acc_curr(IngCh.id, account={'number': 'NL89INGB0020118449', 'currency_code': Currency.USD})

    data = parse_statement_fixture(
        'statement_ing_external_mt940_many.txt', IngCh, ['NL04INGB0007811649', 'NL04INGB0007811648'], 'EUR'
    )

    assert len(data) == 3

    register1, payments1 = data[0]
    register2, payments2 = data[1]
    register3, payments3 = data[2]

    assert register1.is_valid
    assert register2.is_valid
    assert register3.is_valid

    assert len(payments1) == 5
    assert len(payments2) == 0
    assert len(payments3) == 4

    assert not payments1[0].get_info_purpose()  # У транзакции нет тега с описанием.
    assert payments1[1].get_info_purpose()


def test_statement_parser_bogus61(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement_ing_ext_bogus61.txt', IngCh, 'NL51INGB0020015925', 'EUR'
    )[0]

    assert len(payments) == 4
    assert register.is_valid
    assert register.statement_date.isoformat() == '2018-02-15'

    pay_info = payments[0].get_info_purpose()

    assert 'DEPO Taken 24159938' in pay_info


@pytest.mark.parametrize('associate', [IngCh, IngNl, IngRo, Seb, IngTr, IngDe])
def test_create(associate, get_payment_bundle, get_source_payment, get_assoc_acc_curr, mocker):

    get_assoc_acc_curr(associate, account={'number': '40702810800000007671', 'currency_code': Currency.GBP})

    payment_dict = {
        'f_name': 'from name',
        'f_iban': 'NL0000000001',
        't_swiftcode': 'HGFEDCBAXXX',
        't_name': 'This is a very long name of the recipient',
        't_address': 'Mos—co—w–',
        'paid_by': 'BEN',
        'ground': 'purpose',
        'f_swiftcode': '',
        't_bankname': 'Test bankname very looooooooooonggggggg',
        'i_swiftcode': 'NBFAUZ2XXXX',
    }

    mocker.patch('bcl.toolbox.utils.Swift.get_bic_info', lambda swiftcode: SWIFT_DATA.get(swiftcode, {}))

    get_source_payment = partial(get_source_payment, associate=associate)

    payment = get_source_payment(payment_dict)

    def get_creator(payments):
        creator = associate.payment_dispatcher.get_creator(get_payment_bundle(payments, h2h=True))
        creator.preparation_monitor_enabled = False
        return creator

    def create(payments=None):

        if payments is None:
            payments = [get_source_payment(payment_dict)]

        creator = get_creator(payments)
        compiled = creator.create_bundle()
        return creator, compiled

    # Если невалидный платёж всего один, поднимается исключение, пакет не формируется.
    with pytest.raises(BclException):
        creator = get_creator([payment])
        creator.create_bundle()

    payment.refresh_from_db()

    # Платёж помечен невалидным.
    assert payment.is_invalidated
    assert len(creator.preparation_notes[payment.pk]) == 1

    payment1 = get_source_payment(payment_dict)
    payment_dict['f_swiftcode'] = 'CHASLULX'
    payment2 = get_source_payment(payment_dict)
    payment_dict['i_swiftcode'] = 'XXXZZZYY'
    payment3 = get_source_payment(payment_dict)
    creator, compiled = create([payment1, payment2, payment3])

    # Если платежей несколько, исключение для невалидных платежей не поднимается,
    # формируется пакет с валидными платежами.
    payment1.refresh_from_db()
    assert payment1.is_invalidated  # Отсутствует f_iban

    payment3.refresh_from_db()
    assert payment3.status == BCL_INVALIDATED
    assert 'Используется неактивный или несуществующий SWIFT-код' in payment3.processing_notes
    assert not payment2.is_invalidated
    assert len(creator.preparation_notes[payment1.pk]) == 1

    assert '<BtchBookg>false</BtchBookg>' in compiled
    assert '<Ccy>GBP</Ccy>' in compiled
    assert '<Ctry>NL</Ctry>' in compiled
    assert '<Nm>This is a very long name of the rec</Nm>' in compiled
    assert 'DEBT' in compiled
    assert '<AdrLine>ipient Mos—co—w–</AdrLine>' in compiled
    assert '<Nm>Test bankname very looooooooooonggg</Nm>' in compiled
    assert '<Nm>NATIONAL BANK FOR FOREIGN ECONOMIC </Nm>' in compiled

    with pytest.raises(ValidationError):
        create()

    payment_dict.update({
        'f_iban': 'CH123',
        't_iban': 'RO456',
        'currency_id': Currency.EUR,
        'i_swiftcode': '',
    })
    creator, compiled = create()
    assert 'SLEV' in compiled

    # Проверка устранения пробелов из адреса получателя.
    payment_dict['t_address'] = ' \n '
    with pytest.raises(ValidationError):
        create()

    # Проверка на наличие кириллицы.
    payment_dict['t_address'] = 'кириллица '
    with pytest.raises(ValidationError):
        create()


def test_create_ing_ro(get_assoc_acc_curr, get_source_payment, get_payment_bundle, read_fixture, mocker):
    get_assoc_acc_curr(IngRo, account={'number': 'RO79INGB5001008229458910', 'currency_code': Currency.RON})

    payment_dict = {
        'f_name': 'from name',
        'f_iban': 'RO79INGB5001008229458910',
        't_iban': 'RO20BREL0002001547100100',
        't_swiftcode': 'RNCBROBUXXX',
        't_name': 'test name',
        't_address': 'JUD. ILFOV, SAT DOMNESTI COM. DOMNESTI, DOMNESTI, NR.1373, PARTER, CAMERA 1',
        'paid_by': 'BEN',
        'ground': 'purpose',
        'f_swiftcode': IngRo.bid,
        't_bankname': 'Test bankname',
        'date': datetime(2020, 2, 11),
        't_bank_city': 'BUCHAREST',
    }

    get_source_payment = partial(get_source_payment, associate=IngRo)

    payment = get_source_payment(payment_dict)

    mocker.patch('bcl.toolbox.utils.Swift.get_bic_info', lambda swiftcode: SWIFT_DATA.get(swiftcode, {}))

    assert payment.is_domestic

    compiled = IngRo.payment_dispatcher.get_creator(get_payment_bundle([payment])).create_bundle()

    expected = read_fixture('ing_ro_correct_payment.xml').decode('cp1251')

    compiled = re.sub(r'<MsgId>?.*</MsgId>', '', compiled)
    assert re.sub(r'<CreDtTm>?.*</CreDtTm>', '', compiled) == expected

    payment_dict['t_iban'] = 'RO10TREZ0400007XXX010007'
    payment = get_source_payment(payment_dict)
    compiled = IngRo.payment_dispatcher.get_creator(get_payment_bundle([payment])).create_bundle()

    assert payment.is_domestic
    assert '<PmtTpInf><SvcLvl><prtry>RO-BDG</prtry></SvcLvl></PmtTpInf>' in compiled

    payment_dict['t_swiftcode'] = 'CHASLULX'
    payment = get_source_payment(payment_dict)
    compiled = IngRo.payment_dispatcher.get_creator(get_payment_bundle([payment])).create_bundle()

    assert not payment.is_domestic
    assert '<PmtTpInf><SvcLvl><prtry>RO-FCY</prtry></SvcLvl></PmtTpInf>' in compiled

    payment_dict['ground'] = '1'*141
    payment = get_source_payment(payment_dict)
    with pytest.raises(ValidationError):
        IngRo.payment_dispatcher.get_creator(get_payment_bundle([payment])).create_bundle()


def test_ing_ro_statement_parser(parse_statement_fixture):
    res = parse_statement_fixture(
        'ing_ro_multiple_accs.txt', IngRo,
        [
            '91470040/8229450710--EUR',
            '91470040/8229458910--EUR',
            '91470040/8229454010--USD',
            '91470040/8229457710--RUB',
        ]
    )

    assert len(res) == 4
    assert len(res[0][1]) == 0
    assert len(res[1][1]) == 1
    assert len(res[2][1]) == 0

    register, payments = res[3]
    assert len(payments) == 6
    assert register.is_valid
    assert register.statement_date.isoformat() == '2020-02-17'

    assert 'RO06BTRLRONCRT0466124901' in payments[0].get_info_account()
    assert payments[0].number == '141'
    assert payments[0].get_info_bik() == ''
    assert payments[0].get_info_name() == 'VH GLOBAL ENTERPRISE SRL FI'
    assert 'PAYMENT UNDER AGREEMENT 272723/19 28594.18RON' in payments[0].get_info_purpose()

    assert payments[-1].get_info_account() == ''
    assert payments[-1].get_info_bik() == ''
    assert payments[-1].get_info_name() == ''
    assert payments[-1].number == '34682992'
    assert 'RON 430550,00              EXCHANGE RATE: 4,305500000 FX/SCHIMB VALUTAR' in payments[-1].get_info_purpose()


def test_ing_ro_separate_statement_by_dates(parse_statement_fixture):
    res = parse_statement_fixture(
        'ing_ro_multiple_dates.txt', IngRo,
        [
            '91470040/8229450710--EUR',
            '91470040/8229458910--EUR',
            '91470040/8229454010--USD',
            '91470040/8229457710--RUB',
        ]
    )

    assert len(res) == 2
    assert res[0][0].statement_date.isoformat() == '2020-02-06'
    assert res[1][0].statement_date.isoformat() == '2020-02-07'


def test_swift_xt_sepa(get_payment_bundle, get_source_payment, mocker):

    payment_attrs = {
        'ground': 'test sepa payment',
        'summ': '1.00',
        'currency_id': Currency.EUR,

        'f_name': 'Yandex NV',
        'f_swiftcode': 'CHASLULX',
        'f_iban': 'LU370670006550031621',

        't_name': 'Yandex Europe AG',
        't_swiftcode': 'BBRUCHGT',
        't_iban': 'CH4708387000001280377',
        't_address': 'there',
    }

    mocker.patch('bcl.toolbox.utils.Swift.get_bic_info', lambda swiftcode: SWIFT_DATA.get(swiftcode, {}))

    payment = get_source_payment(payment_attrs)
    mandatory_check = [
        'ground', 'f_swiftcode', 'f_name', 'f_iban', 't_swiftcode', 't_acc', 't_name',
        't_bank_city', 't_address'
    ]
    not_mandatory_check = ['t_bankname']

    for check in mandatory_check + not_mandatory_check:
        payment_attrs[check] = ''

    incorrect_payment = get_source_payment(payment_attrs)
    bundle = get_payment_bundle([payment, incorrect_payment])
    creator = IngCh.payment_dispatcher.get_creator(bundle)
    result = creator.create_bundle()

    bundle = PaymentsBundle.objects.get(id=bundle.id)
    incorrect_payment.refresh_from_db()

    assert len(bundle.payments) == 1
    assert incorrect_payment.status == states.BCL_INVALIDATED
    assert 'Значение обязательно, но не указано' in incorrect_payment.processing_notes
    for check in mandatory_check:
        assert check in incorrect_payment.processing_notes
    for check in not_mandatory_check:
        assert check not in incorrect_payment.processing_notes

    assert '<BtchBookg>false</BtchBookg>' not in result
    assert '<Ccy>EUR</Ccy>' in result
    assert '<SvcLvl><Cd>SEPA</Cd></SvcLvl>' in result
    assert '<ChrgBr>SLEV</ChrgBr>' in result
    assert '<DbtrAcct><Id><IBAN>LU370670006550031621</IBAN>' in result
    assert '<CdtrAcct><Id><IBAN>CH4708387000001280377</IBAN>' in result
    assert '<RmtInf><Ustrd>{VO 22534} test sepa payment</Ustrd>' in result
    assert '<InstdAmt Ccy="EUR">1.00</InstdAmt>' in result
    assert 'IntrmyAgt1' not in result

    payment_attrs = {
        'ground': 'test wired payment usd',
        'summ': '1.00',
        'currency_id': Currency.USD,

        't_name': 'Yandex NV',
        't_swiftcode': 'CHASLULX',
        't_iban': 'LU640670006550031620',

        'f_name': 'Yandex INC',
        'f_swiftcode': 'BOFAUS3N',
        'f_acc': '898055873999',
        'f_iban': '',

        't_bankname': 'ING BELGIUM',
        't_address': ' there',

        'i_swiftcode': 'DEUTGB2LXXX',
    }
    payment = get_source_payment(payment_attrs)

    mandatory_check = [
        'ground', 'f_swiftcode', 'f_name', 't_swiftcode', 't_acc', 't_name', 't_bankname',
        't_bank_city', 't_address'
    ]
    not_mandatory_check = ['f_iban', 't_iban']

    for check in mandatory_check + not_mandatory_check:
        payment_attrs[check] = ''
    incorrect_payment = get_source_payment(payment_attrs)
    bundle = get_payment_bundle([payment, incorrect_payment])
    creator = IngCh.payment_dispatcher.get_creator(bundle)
    result = creator.create_bundle()

    bundle = PaymentsBundle.objects.get(id=bundle.id)
    incorrect_payment.refresh_from_db()

    assert len(bundle.payments) == 1
    assert incorrect_payment.status == states.BCL_INVALIDATED
    assert 'Значение обязательно, но не указано' in incorrect_payment.processing_notes
    assert all([a in incorrect_payment.processing_notes for a in mandatory_check])
    assert all([a not in incorrect_payment.processing_notes for a in not_mandatory_check])

    assert '<SvcLvl><Cd>NURG</Cd></SvcLvl>' in result
    assert '<ChrgBr>DEBT</ChrgBr>' in result
    assert '<CdtrAcct><Id><IBAN>LU640670006550031620</IBAN>' in result
    assert '<DbtrAcct><Id><Othr><Id>898055873999</Id></Othr></Id>' in result
    assert '<RmtInf><Ustrd>{VO 22534} test wired payment usd</Ustrd>' in result
    assert '<InstdAmt Ccy="USD">1.00</InstdAmt>' in result
    assert '<IntrmyAgt1><FinInstnId><BIC>DEUTGB2L</BIC><Nm>DEUTSCHE BANK AG</Nm><PstlAdr><Ctry>GB</Ctry>' in result


def test_statement_missing_tag(parse_statement_fixture):
    with pytest.raises(FieldValidationError):
        parse_statement_fixture(
            'ing_nl_missing_tag.txt', IngNl, 'NL35INGB0650977351', 'EUR'
        )


def test_statement_tag_86_purpose(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'ing_ch_tag_86_purpose.txt', IngCh, 'CH5208387000001180377', 'EUR'
    )[0]

    payment = payments[0]

    assert payment.info["06"] == "123/qwe/INV/123/INV/123/ULTD/qqq"

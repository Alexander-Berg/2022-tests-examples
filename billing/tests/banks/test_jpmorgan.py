from datetime import datetime

import pytest

from bcl.banks.common.swift_mx import Statuses
from bcl.banks.registry import JpMorgan
from bcl.core.models import Currency, Payment, PaymentsBundle, PROCESSING, EXPORTED_H2H, BCL_INVALIDATED, \
    ACCEPTED_H2H, DECLINED_BY_BANK
from bcl.exceptions import ValidationError

SWIFT_DATA = {
    'DEUTGB2LXXX': {
        'countryCode': 'GB', 'instName': 'DEUTSCHE BANK AG',
        'addr': 'LONDON EC2N 2DB WINCHESTER HOUSE 1 GREAT WINCHESTER STREET', 'city': 'LONDON',
        'street': 'WINCHESTER HOUSE'
    },
    'CHASLULX': {
        'countryCode': 'LU', 'instName': 'J.P. MORGAN BANK LUXEMBOURG S.A.',
        'addr': '2663 SENNINGERBERG 6 ROUTE DE TREVES', 'city': 'SENNINGERBERG',
        'street': '6 ROUTE DE TREVES'
    },
}


def test_payment_synchronizer():

    class MockCaller:

        associate = JpMorgan
        on_date = datetime(2017, 11, 21)

    filter_func = JpMorgan.payment_synchronizer.connector(
        caller=MockCaller()
    ).status_filename_get_filter()

    fnames = [
        'YANDEXNV.ACK.ISO20022_PAIN_01Ver3.321.20171121040537456',
        'YANDEXNV.ACK.ISO20022_PAIN_01Ver3.321.20171120040537456',
        'YANDEXNV.PSOURCE.MRCNFRM.2221.20171121040537456',
        'YANDEXNV.GACH.REJ-CREDIT.0.20171121000000000',
        'YANDEXNV.GACH.RET-CREDIT.0.20171121000000000',
        'YANDEXNV.SOME.0.20171121000000000',
    ]

    names = list(map(filter_func, fnames))
    assert names == [
        'ACK.ISO20022_PAIN_01Ver3',
        False,
        'PSOURCE.MRCNFRM',
        'GACH.REJ-CREDIT',
        'GACH.RET-CREDIT',
        False
    ]


TYPE_SEPA = 1
TYPE_WIRED_EUR = 2
TYPE_WIRED_RUB = 3
TYPE_WIRED_USD = 4


def get_payment_dict(type_id, attrs=None):
    attrs = attrs or {}

    if type_id == TYPE_SEPA:
        # SEPA (платёж по еврозоне)
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
            't_address': 'Moscow, Streer, Building 1',
        }

    elif type_id == TYPE_WIRED_EUR:
        # Wired (классический платёж)
        payment_attrs = {
            'ground': 'test wired pay\r\nment eur',
            'summ': '1.00',
            'currency_id': Currency.EUR,

            'f_name': 'Yandex NV',
            'f_swiftcode': 'CHASLULX',
            'f_iban': 'LU370670006550031621',

            't_name': 'Yandex Services AG',
            't_swiftcode': 'BBRUCHGT',
            't_iban': 'CH2508387000001180378',

            't_bankname': 'ING BELGIUM',
            't_address': 'Moscow, Streer, Building 1',
        }

    elif type_id == TYPE_WIRED_RUB:
        # Wired (классический платёж) в рублях
        payment_attrs = {
            'ground': 'тестовый платёж в рублях',
            'summ': '1.00',
            'currency_id': Currency.RUB,

            'f_name': 'Yandex NV',
            'f_swiftcode': 'CHASLULX',
            'f_iban': 'LU800670006550031623',

            't_name': 'Yandex NV',
            't_swiftcode': 'INGBNL2A',
            't_iban': 'NL85INGB0020003382',

            't_bankname': 'ING BANK N.V.',
            't_address': 'Moscow, Streer, Building 1',
        }

    elif type_id == TYPE_WIRED_USD:
        # Wired (классический платёж) в долларах
        payment_attrs = {
            'ground': 'test wired payment usd',
            'summ': '1.00',
            'currency_id': Currency.USD,

            'f_name': 'Yandex NV',
            'f_swiftcode': 'CHASLULX',
            'f_iban': 'LU640670006550031620',

            't_name': 'Yandex INC',
            't_swiftcode': 'BOFAUS3N',
            't_acc': '898055873999',
            't_iban': '',

            't_bankname': 'ING BELGIUM',
            't_address': 'Moscow, Streer, Building 1',
        }

    else:
        raise Exception

    payment_attrs['ground'] += ' some&some'
    payment_attrs.update(attrs)

    return payment_attrs


def test_payment_creator(build_payment_bundle, get_assoc_acc_curr, mocker):
    associate = JpMorgan

    # перевод EUR
    compiled = build_payment_bundle(associate, payment_dicts=[get_payment_dict(TYPE_WIRED_EUR)]).tst_compiled

    assert '<Id>9692940EU</Id>' in compiled
    assert 'pay ment eur' in compiled
    assert 'IntrmyAgt1' not in compiled
    assert '<Id><IBAN>CH2508387000001180378</IBAN></Id>' in compiled
    assert 'some&amp;some' in compiled
    assert '<DbtrAgt><FinInstnId><BIC>CHASLULX</BIC>' in compiled

    _, account, _ = get_assoc_acc_curr(associate)
    account.org_remote_id = 'BRANDNEWORGID'
    account.save()

    # проверка sepa платежа с посредником
    mocker.patch('bcl.toolbox.utils.Swift.get_bic_info', lambda swiftcode: SWIFT_DATA.get(swiftcode, {}))

    payment_dict = get_payment_dict(TYPE_SEPA, {'i_swiftcode': 'DEUTGB2LXXX'})

    compiled = build_payment_bundle(
        associate,
        payment_dicts=[payment_dict],
        account=account,
    ).tst_compiled

    assert '<Id>9692940EU</Id>' not in compiled
    assert f'<Id>{account.org_remote_id}</Id>' in compiled
    assert '<IntrmyAgt1><FinInstnId><BIC>DEUTGB2LXXX</BIC><Nm>DEUTSCHE BANK AG</Nm><PstlAdr><Ctry>GB</Ctry>' in compiled
    assert '<Id><IBAN>CH4708387000001280377</IBAN></Id>' in compiled
    assert 'some&amp;some' in compiled

    # sepa платёж, но не заполнен iban отправителя
    payment_dict['f_iban'] = ''
    compiled = build_payment_bundle(associate, payment_dicts=[payment_dict], account=account).tst_compiled
    assert '<DbtrAcct><Id><Othr><Id>fakeacc' in compiled

    # перевод USD
    compiled = build_payment_bundle(
        associate,
        payment_dicts=[
            get_payment_dict(TYPE_WIRED_USD), get_payment_dict(TYPE_WIRED_USD, {'i_swiftcode': 'XXXZZZYY'})
        ]
    ).tst_compiled
    assert '<Id><Othr><Id>898055873999</Id></Othr></Id>' in compiled
    assert 'some&amp;some' in compiled

    incorrect_payment = Payment.getone(**get_payment_dict(TYPE_WIRED_USD, {'i_swiftcode': 'XXXZZZYY'}))
    assert incorrect_payment.status == BCL_INVALIDATED
    assert 'Используется неактивный или несуществующий SWIFT-код' in incorrect_payment.processing_notes

    compiled = build_payment_bundle(associate, payment_dicts=[get_payment_dict(TYPE_WIRED_RUB)]).tst_compiled
    assert '<Id><IBAN>NL85INGB0020003382</IBAN></Id>' in compiled
    assert '<Ustrd>VO22534' in compiled
    assert '<AdrLine>INN7725713770</AdrLine><AdrLine>KPP987654321</AdrLine>' in compiled
    assert 'some&amp;some' in compiled

    # перевод RUB
    compiled = build_payment_bundle(
        associate,
        payment_dicts=[get_payment_dict(TYPE_WIRED_RUB, {'f_swiftcode': 'LUMIILITBSC', 't_swiftcode': 'INGBRUMMXXX'})],
    ).tst_compiled

    assert '<Id>9692940EU</Id>' not in compiled
    assert f'<Id>{account.org_remote_id}</Id>' not in compiled
    assert '<DbtrAgt><FinInstnId><BIC>LUMIILITBSC</BIC>' in compiled
    assert '<CdtrAgt><FinInstnId><BIC>INGBRUMMXXX</BIC>' in compiled

    # Невалидный swift посредника.
    with pytest.raises(ValidationError):
        build_payment_bundle(associate, payment_dicts=[get_payment_dict(TYPE_WIRED_EUR, {'i_swiftcode': 'XXXZZZYY'})])

    # Валидный swift посредника.
    build_payment_bundle(associate, payment_dicts=[get_payment_dict(TYPE_WIRED_EUR, {'i_swiftcode': 'CHASLULX'})])

    # Невалидный адрес получателя.
    with pytest.raises(ValidationError):
        build_payment_bundle(associate, payment_dicts=[get_payment_dict(TYPE_WIRED_EUR, {'t_address': 'Москва'})])


def test_validate_payments(build_payment_bundle, sitemessages):
    payment_attrs = {}

    mandatory_check = [
        'f_swiftcode', 'f_name', 't_swiftcode', 't_acc', 't_name', 'ground'
    ]

    for check in mandatory_check:
        payment_attrs[check] = ''

    payment_attrs['t_bankname'] = ''
    bundle = build_payment_bundle(
        JpMorgan, payment_dicts=[get_payment_dict(TYPE_WIRED_USD), get_payment_dict(TYPE_WIRED_USD, payment_attrs)])

    bundle = PaymentsBundle.objects.get(id=bundle.id)
    assert len(bundle.payments) == 1

    incorrect_payment = Payment.getone(**get_payment_dict(TYPE_WIRED_USD, payment_attrs))
    assert incorrect_payment.status == BCL_INVALIDATED
    assert 'Значение обязательно, но не указано' in incorrect_payment.processing_notes
    assert all([a in incorrect_payment.processing_notes for a in mandatory_check])
    assert 't_bankname' in incorrect_payment.processing_notes

    bundle = build_payment_bundle(
        JpMorgan, payment_dicts=[get_payment_dict(TYPE_SEPA), get_payment_dict(TYPE_SEPA, payment_attrs)])

    bundle = PaymentsBundle.objects.get(id=bundle.id)
    assert len(bundle.payments) == 1

    incorrect_payment = Payment.getone(**get_payment_dict(TYPE_SEPA, payment_attrs))
    assert incorrect_payment.status == BCL_INVALIDATED
    assert 'Значение обязательно, но не указано' in incorrect_payment.processing_notes
    assert all([a in incorrect_payment.processing_notes for a in mandatory_check])
    assert 't_bankname' not in incorrect_payment.processing_notes

    messages = sitemessages()
    assert len(messages) == 2
    subject = messages[0].context['subject']
    assert 'J.P. Morgan' in subject
    assert 'Информация по обработке платежей' in subject


def test_payment_creator_validation(get_source_payment, get_payment_bundle):
    payment = get_source_payment({'t_bankname': ''})
    creator = JpMorgan.payment_dispatcher.get_creator(get_payment_bundle([payment]))

    with pytest.raises(ValidationError):
        creator.create_bundle()


def test_sepa_partioning(build_payment_bundle):
    bundles = build_payment_bundle(JpMorgan, payment_dicts=[
        get_payment_dict(TYPE_WIRED_USD),
        get_payment_dict(TYPE_WIRED_RUB),
        get_payment_dict(TYPE_SEPA),
    ])

    assert len(bundles) == 2
    assert len(bundles[0].payments) == 2
    assert len(bundles[1].payments) == 1


def test_sender(build_payment_bundle, monkeypatch, mocker):

    monkeypatch.setattr(
        'bcl.banks.party_jpmorgan.payment_sender.JpMorganSftpConnector.get_client', mocker.Mock())

    # Эмулируем удачную подпись PGP.
    dummy_communicate = mocker.MagicMock()
    dummy_communicate.return_value = b'-----BEGIN PGP MESSAGE-----\nsigned', b''
    monkeypatch.setattr('bcl.toolbox.signatures.Popen.communicate', dummy_communicate)

    bundle = build_payment_bundle(JpMorgan, payment_dicts=[get_payment_dict(TYPE_WIRED_EUR)], h2h=True)

    JpMorgan.payment_sender(bundle).prepare_send_sync()

    # Симулируем невалидный документ.
    bundle.file.zip_raw = bundle.file.zip_raw.decode('utf-8').replace('GrpHdr', 'Bogus').encode('utf-8')
    bundle.file.save()

    with pytest.raises(ValidationError) as einfo:
        sender = JpMorgan.payment_sender(bundle)
        contents = sender.prepare_contents()
        sender.validate_contents(contents)

    assert "Bogus': This element is not expected." in einfo.value.msg


def test_statements(get_assoc_acc_curr, parse_statement_fixture):

    associate = JpMorgan

    get_assoc_acc_curr(associate, account={'number': '6550031621', 'currency_code': 'EUR'})
    get_assoc_acc_curr(associate, account='6550031623')

    result = parse_statement_fixture('jpmorgan_A_statement.xml', associate, '6550031620', 'USD')

    register, pays = result[0]
    assert not len(pays)
    assert str(register.statement_date) == '2017-12-01'

    register, pays = result[1]
    assert len(pays) == 2
    assert register.is_valid

    register, pays = result[2]
    assert not len(pays)

    # Далее проверка нахождения нужного счёта, если в выписке указан его remote_id.

    _, acc, _ = get_assoc_acc_curr(associate, account={'number': '1086411016770018', 'currency_code': 'ILS'})
    acc.remote_id = '1086411016770018ILS'
    acc.save()

    result = parse_statement_fixture('jpmorgan_A_statement_custom_id.xml', associate, acc.number, 'ILS')
    assert len(result) == 1

    register, payments = result[0]
    assert register.account == acc
    assert register.is_valid

    assert len(payments) == 2
    assert payments[0].get_info_name() == 'test3'


def test_two_days_statements(get_assoc_acc_curr, parse_statement_fixture):

    associate = JpMorgan

    get_assoc_acc_curr(associate, account={'number': 'IL550108640000019180026', 'currency_code': 'EUR', 'remote_id': '1086411019180026ILS'})
    get_assoc_acc_curr(associate, account={'number': 'IL520108640000016770018', 'currency_code': 'EUR', 'remote_id': '1086411016770018ILS'})

    result = parse_statement_fixture('jpmorgan_two_days_statement.xml', associate, 'IL550108640000019180026', 'EUR')

    assert len(result) == 4

    register1, pays = result[0]
    assert len(pays) == 0
    assert register1.is_valid
    assert str(register1.statement_date) == '2020-10-23'

    register2, pays = result[1]
    assert len(pays) == 1
    assert register2.is_valid
    assert str(register2.statement_date) == '2020-10-25'
    assert register1.account == register2.account

    register1, pays = result[2]
    assert len(pays) == 1
    assert register1.is_valid
    assert str(register1.statement_date) == '2020-10-23'
    assert pays[0].get_info_name() == 'test2'

    register2, pays = result[3]
    assert len(pays) == 1
    assert register2.is_valid
    assert str(register2.statement_date) == '2020-10-25'
    assert register1.account == register2.account


@pytest.mark.parametrize(
    'external_status, internal_status',
    [
        (Statuses.STATUS_VERIFIED_FORMAT, ACCEPTED_H2H),
        (Statuses.STATUS_VERIFIED_REQ, PROCESSING),
        (Statuses.STATUS_REJECTED, DECLINED_BY_BANK),
    ]
)
def test_status_parser_level1(external_status, internal_status, read_fixture, get_payment_bundle, get_source_payment):

    bundle = get_payment_bundle([{'number': '42835', 'status': EXPORTED_H2H}], associate=JpMorgan, id=859628)

    xml = read_fixture('jpmorgan_statuses_l1.xml', decode='utf-8').replace('ACTC', external_status)
    syncer = JpMorgan.payment_synchronizer()
    status = syncer.parse_statuses_xml(xml)
    syncer.update_payments(status)

    assert len(status['payments']) == 1

    bundle.refresh_from_db()
    assert bundle.status == internal_status

    assert bundle.payments[0].status == PROCESSING


def test_status_parser_level2(read_fixture):

    xml = read_fixture('jpmorgan_statuses_l2.xml', decode='utf-8')
    syncer = JpMorgan.payment_synchronizer()
    status = syncer.parse_statuses_xml(xml)

    assert len(status['payments']) == 1
    assert status['bundle_status_reason'] == 'NARR CREDIT'
    assert status['payments'][0]['payment_id'] == '6259.1'
    assert status['payments'][0]['reason_code'] == 'BE04'
    assert 'MANDATORY' in status['payments'][0]['reason_info']

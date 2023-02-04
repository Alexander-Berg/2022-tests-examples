import re
from datetime import datetime

import pytest

from bcl.banks.party_raiff.payment_creator import RaiffUpgPaymentCreator
from bcl.banks.party_raiff.tasks import *  # Позволяет зарегистрировать фоновые задачи для тестов.
from bcl.banks.protocols.upg.raiffeisen.base import RaiffRequest
from bcl.banks.registry import Raiffeisen
from bcl.core.models import Payment, states, Request, Currency, PaymentsBundle, BCL_INVALIDATED
from bcl.exceptions import ValidationError
from bcl.toolbox.tasks import get_registered_task


SWIFT_DATA = {
    'INGBNL2A': {
        'countryCode': 'NL', 'instName': 'ING BANK N.V.', 'addr': '1102 CT AMSTERDAM BIJLMERDREEF 106',
        'city': 'AMSTERDAM', 'street': 'BIJLMERDREEF 106'
    },
    'OWHBDEFF': {
        'countryCode': 'DE', 'instName': 'VTB BANK (EUROPE) SE', 'addr': '60325 FRANKFURT AM MAIN RUESTERSTRASSE 7-9',
        'city': 'FRANKFURT AM MAIN', 'street': 'RUESTERSTRASSE 7-9'
    },
    'AGCAAM22': {
        'countryCode': 'AM', 'instName': 'ACBA-CREDIT AGRICOLE BANK CJSC', 'addr': '0002 YEREVAN 82-84 ARAMI STREET',
        'city': 'YEREVAN', 'street': '82-84 ARAMI STREET'
    },
    'BOFANLNX': {
        'countryCode': 'NL', 'addr': '1096 HA AMSTERDAM REMBRANDT TOWER FLOOR 11 AMSTELPLEIN 1',
        'instName': 'BANK OF AMERICA MERRILL LYNCH INTERNATIONAL DESIGNATED ACTIVITY COMPANY, AMSTERDAM BRANCH',
        'city': 'AMSTERDAM', 'street': 'REMBRANDT TOWER'
    }

}


def test_raiff_1c(build_payment_bundle):

    bundle = build_payment_bundle(
        Raiffeisen,
        payment_dicts=[{
            't_name': 'Акционерное общество «Бизнес Аналитика – Маркет Контур»',
        }])

    # Проверка замены тире и кавычек.
    assert 'Получатель1=Акционерное общество "Бизнес Аналитика - Маркет Контур"' in bundle.tst_compiled
    assert 'None' not in bundle.tst_compiled


def test_prep_rules(build_payment_bundle):

    bundle = build_payment_bundle(Raiffeisen, payment_dicts=[
        {'ground': '0' * 211},
        {'ground': '9' * 210},  # Длинное назначение.
        {
            'n_kbk': '102030',  'n_okato': '45383000', 'n_status': '02', 'n_ground': 'ТП', 'n_period': 'МС.08.2020',
            'n_doc_num': '0', 'n_doc_date': '31-08-2020'
        },
        {'n_kbk': '102030', 'n_status': '1', 'n_ground': '0', 'n_doc_num': 'xxx', 'n_okato': ''},  # Не указан ОКАТО.
    ], account={'number': '1' * 20}, h2h=True)

    pays = list(Payment.objects.order_by('id').all())
    assert len(pays) == 4

    assert pays[0].is_invalidated
    assert 'ground' in pays[0].processing_notes
    assert not pays[1].is_invalidated
    assert not pays[2].is_invalidated
    assert pays[3].is_invalidated
    assert 'n_okato' in pays[3].processing_notes


def test_payment_creator_currency(
    get_bundle, get_source_payment, mock_signer, mock_post, read_fixture, init_user, mocker, get_assoc_acc_curr
):
    _, account, _ = get_assoc_acc_curr(Raiffeisen, account='40702840800001400742', curr=Currency.by_num[Currency.USD])
    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')  # Фикстура подписи фиктивная!

    init_user()

    payment_attrs = dict(
        number='869386',
        date=datetime(2019, 3, 12),
        currency_id=Currency.USD,
        expected_dt=datetime(2019, 12, 17),

        f_acc='40702840800001400742',
        f_inn='7736207543',
        f_name='YANDEX',
        f_cacc='30101810200000000700',
        f_bic='044525700',
        f_bankname='АО «РАЙФФАЙЗЕНБАНК»',
        f_kpp='',

        t_acc='NL03INGB0662303938',
        t_iban='NL03INGB0662303938',
        t_inn='000000000000',
        t_name='Ledeneva Valeria Andreevna',
        t_bic='INGBNL2A',
        t_swiftcode='INGBNL2A',
        t_bankname='ING BANK N.V.',
        t_bank_city='AMSTERDAM',
        t_cacc='',
        t_kpp='',
        t_address='Somewhere far far away',

        ground='Payment to the Act dd 05/11/2018, Contract 10191583 dd 01/08/2018',
        summ='23.31',
        trans_pass='18010023/2495/0000/4/1',
        oper_code='21200',
        t_country='528',
        official_name='Elena Bezborodova',
        official_phone='+7 495 739-70-00 4658',
        currency_op_docs='1',
    )
    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)

    mocker.patch('bcl.toolbox.utils.Swift.get_bic_info', lambda swiftcode: SWIFT_DATA.get(swiftcode, {}))

    payment_bundle, signature = get_bundle(payment, account)

    digest = payment_bundle.digests[0].decode('utf-8')
    assert digest == read_fixture('payment/digest_cur.txt', decode='utf-8')

    payment_attrs = {
        't_bic': '',
        'currency_id': Currency.USD,
        'ground': 'Some ground',
        'i_swiftcode': 'AGCAAM22',
        'summ': '23.10',
        'expected_dt': datetime(2019, 12, 17),
    }

    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)

    with pytest.raises(ValidationError) as e:
        get_bundle(payment, account)
    assert 'страны получателя' in e.value.msg

    payment_attrs['t_country'] = '051'
    payment_attrs['currency_op_docs'] = '3'

    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)
    payment_bundle, signature = get_bundle(payment, account)
    now_date_str = datetime.now().strftime('%Y-%m-%d')

    digest = payment_bundle.digests[0].decode('utf-8')
    assert '[Поручение на перевод валюты]' in digest
    assert 'Расчетная сумма списания=23.1\n' in digest
    assert 'Сумма перевода=23.1\n' in digest
    assert 'Номер паспорта сделки=12345678/1234/1234/1/1' in digest
    assert 'Адрес бенефициара=г Москва, ул. Краснопролетарская, д. 1, стр. 3' in digest
    assert 'Тип контрагента (0 - не резидент, 1 - резидент)=0' in digest
    assert 'Документы не требуются=0' in digest
    assert '72: Дополнительная информация для банков посредников=\n' in digest

    request = mock_post.last_request
    assert 'PayDocCurRaif' in request
    assert '<upg:Vo>22534</upg:Vo>' in request
    assert '<upg:DealPassData><upg:Num>12345678/1234/1234/1/1</upg:Num><upg:ExpTerm>2019-12-17</upg:ExpTerm>' in request
    assert 'AccCommis bic="044525700" name="АО БАНК1 Г. МОСКВА">40702810800000007671<' in request
    assert '<upg:Country name="ГЕРМАНИЯ" digital="276" iso2="DE"/>' in request
    assert '<upg:Country name="АРМЕНИЯ" digital="051" iso2="AM"/>' in request
    assert '<upg:Address>г Москва, ул. Краснопролетарская, д. 1, стр. 3</upg:Address>' in request
    assert '<upg:Res>0</upg:Res>' in request
    assert '<upg:Nodocs>0</upg:Nodocs>' in request

    payment_attrs['t_country'] = '643'
    payment_attrs['summ'] = '23.0'
    payment_attrs.pop('currency_op_docs')
    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)
    payment_bundle, signature = get_bundle(payment, account)

    assert 'Тип контрагента (0 - не резидент, 1 - резидент)=1' in payment_bundle.digests[0].decode('utf-8')
    assert 'Документы не требуются=1' in payment_bundle.digests[0].decode('utf-8')
    assert 'Расчетная сумма списания=23\n' in payment_bundle.digests[0].decode('utf-8')

    request = mock_post.last_request
    assert '<upg:Res>1</upg:Res>' in request
    assert '<upg:Nodocs>1</upg:Nodocs>' in request
    assert 'AddInfo_72' not in request
    assert '<upg:DocSum_33B><upg:WriteOffSum sum="23.00" code="840" codeISO="USD"/></upg:DocSum_33B>' in request
    assert '<upg:DocSum_32A MultiCurr="0"><upg:TransSum sum="23.00" code="840" codeISO="USD"/></upg:DocSum_32A>' in request

    payment_attrs['t_swiftcode'] = 'BOFANLNX'
    payment_attrs['currency_id'] = Currency.EUR
    payment_attrs['summ'] = '23.10'

    # используем адрес из данных организации
    account.org.address_en = 'myorgmy'
    account.org.save()

    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)
    payment_bundle, signature = get_bundle(payment, account)

    digest = payment_bundle.digests[0].decode('utf-8')
    assert 'Наименование банка бенефициара=BANK OF AMERICA MERRILL LYNCH INTERNATIONAL DESIGNATED ACTIVITY COMPAN\n' in digest
    assert 'Расчетная сумма списания=\n' in digest

    request = mock_post.last_request
    assert '<upg:Address>myorgmy</upg:Address>' in request
    assert '<upg:Name>BANK OF AMERICA MERRILL LYNCH INTERNATIONAL DESIGNATED ACTIVITY COMPAN</upg:Name>' in request
    assert '<upg:DocSum_33B><upg:WriteOffSum code="840" codeISO="USD"/></upg:DocSum_33B>' in request
    assert '<upg:DocSum_32A MultiCurr="1"><upg:TransSum sum="23.10" code="978" codeISO="EUR"/></upg:DocSum_32A>' in request

    payment_attrs['i_swiftcode'] = 'XXXZZZYY'
    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)
    with pytest.raises(ValidationError) as e:
        get_bundle(payment)

    payment.refresh_from_db()
    assert 'Используется неактивный или несуществующий SWIFT-код' in e.value.msg
    assert payment.status == BCL_INVALIDATED

    payment_attrs['trans_pass'] = ''
    payment_attrs['contract_dt'] = None
    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)

    with pytest.raises(ValidationError) as e:
        get_bundle(payment)
    assert 'указать УНК' in e.value.msg


def test_payment_creator_currency_acc_comiss(
    get_bundle, get_source_payment, mock_signer, mock_post, read_fixture, mocker
):
    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')

    payment_attrs = {
        't_bic': '',
        'currency_id': Currency.USD,
        'ground': 'Some ground',
        'i_swiftcode': 'AGCAAM22',
        'summ': '23.10',
        't_country': '051',
        'expected_dt': None,
        'i_info': 'test info',
    }

    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)
    payment.paid_by = 'BEN'
    payment.save()

    mocker.patch('bcl.toolbox.utils.Swift.get_bic_info', lambda swiftcode: SWIFT_DATA.get(swiftcode, {}))
    payment_bundle, signature = get_bundle(payment)

    digest = payment_bundle.digests[0].decode('utf-8')
    assert '71А: Вид комиссии за перевод=BEN' in digest
    assert 'Расходы банка списать со счета=40702810301400002360' in digest
    assert '72: Дополнительная информация для банков посредников=test info' in digest

    request = mock_post.last_request
    assert 'PayDocCurRaif' in request
    assert '<upg:Vo>22534</upg:Vo>' in request
    assert '<upg:DealPassData><upg:Num>12345678/1234/1234/1/1</upg:Num></upg:DealPassData></upg:VoSum></upg:VoSumInfo>' in request
    assert 'AccCommis bic="044525700" name="АО БАНК1 Г. МОСКВА">40702810301400002360<' in request
    assert '<upg:AddInfo_72>test info</upg:AddInfo_72>' in request
    assert '<upg:DocSum_33B><upg:WriteOffSum code="643" codeISO="RUB"/></upg:DocSum_33B>' in request


@pytest.mark.parametrize(
    'response_status, pay_status',
    [
        ('IMPLEMENTED', states.COMPLETE),
        ('ACCEPTED_BY_CFE', states.PROCESSING),
    ]
)
def test_payment_creator(response_status, pay_status,
        get_bundle, get_payment_bundle, get_source_payment, read_fixture, mock_post, mock_signer, get_signing_right):
    """Проверяет создание и отправку платежа host-to-host и обновление его статуса."""

    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')

    # Ответ на запрос GetStatus для идентификатора из ответа sendRequests
    get_status_delivered = read_fixture('payment/get_status_delivered.xml', decode='utf-8')

    # Ответ на запрос DocIds
    send_doc_response = read_fixture('payment/sendrequest_response.xml', decode='utf-8')

    # Ответ на запрос GetStatus для идентификатора из ответа DocIds
    doc_get_status_implemented = read_fixture(
        'payment/doc_get_status_implemented.xml', decode='utf-8'
    ).replace('IMPLEMENTED', response_status)

    payment_attrs = dict(
        number='7675',
        date=datetime(2017, 10, 9),

        f_acc='40702810800002400742',
        f_inn='7736207543',
        f_name='ООО ЯНДЕКС',
        f_cacc='30101810200000000700',
        f_bic='044525700',
        f_bankname='АО «РАЙФФАЙЗЕНБАНК»',
        f_kpp='123456789',

        t_acc='40702810600001400744',
        t_inn='7708764126',
        t_name='ООО «Получатель»',
        t_bic='040173604',
        t_bankname='АЛТАЙСКОЕ ОТДЕЛЕНИЕ N8644 ПАО СБЕРБАНК',
        t_bank_city='БАРНАУЛ',
        t_cacc='30101810200000000604',
        t_kpp='',

        ground='{VO01010} В том числе «НДС» 18.00 % - 1.53.',
        summ='100',
        n_kod='0189',
    )

    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)

    payment_bundle, signature = get_bundle(payment)
    digest = payment_bundle.digests[0].decode('utf-8')
    assert digest == read_fixture('payment/digest.txt', decode='utf-8')

    request = mock_post.last_request
    assert '<upg:Name>АО «РАЙФФАЙЗЕНБАНК»</upg:Name>' in request
    assert '<upg:Name>ООО "Получатель"</upg:Name>' in request
    assert 'PayDocRu' in request
    assert 'docSum="100.00"' in request
    assert 'uip="0189"' in request
    assert 'personalAcc="40702810800002400742"' in request
    assert 'correspAcc="30101810200000000604"' in request
    assert 'correspAcc="30101810200000000700"' in request
    assert '<upg:SN>216f0a8c000100001380</upg:SN>' in request
    assert '<upg:SignType>Единственная подпись</upg:SignType>' in request
    assert f'<upg:Value>{signature.as_text()}</upg:Value>' in request
    assert 'purpose="{VO01010} В том числе &quot;НДС&quot; 18.00 % - 1.53."'

    # Проходит время и запускаются задачи, которые обновляют статусы всем платежам
    mock_post(get_status_delivered, send_doc_response, url_filter=lambda url: 'upg' in url)
    get_registered_task('raiff_payment_status_sync_prepare').func()

    mock_post(doc_get_status_implemented, url_filter=lambda url: 'upg' in url)
    get_registered_task('raiff_payment_status_sync').func()

    assert '<requests>867299b9-1c10-4c65-b001-1b72431e905c</requests>' in mock_post.last_request
    assert Payment.objects.get(id=payment.id).status == pay_status

    request = Request.objects.get()
    if pay_status == states.COMPLETE:
        assert request.processed
        assert request.status == states.COMPLETE
    else:
        assert not request.processed
        assert request.status == states.PROCESSING

    # Проверяем формирование пакета без коррсчетов.
    payment_attrs['t_cacc'] = ''
    payment_attrs['f_cacc'] = ''

    # Создаётся пакет
    payment_bundle = get_payment_bundle([
        get_source_payment(associate=Raiffeisen, attrs=payment_attrs)], h2h=True)  # type: PaymentsBundle

    compiled = Raiffeisen.payment_dispatcher.get_creator(payment_bundle).create_bundle()
    assert 'correspAcc' not in compiled


def test_payment_not_processed(read_fixture, mock_post):
    """Проверяет обработку ответа на запрос статуса, когда банк еще не успел обработать заявку."""
    mock_post(read_fixture(
        'payment/get_status_not_processed.xml', decode='utf-8'), url_filter=lambda url: 'upg' in url)

    Request(
        type=Request.TYPE_PAYMENT,
        associate_id=Raiffeisen.id,
        remote_id='5c970c2b-a978-4d97-b34a-fcd05021efcf'
    ).save()

    get_registered_task('raiff_payment_status_sync_prepare').func()


def test_invalid_payer_account(read_fixture, mock_post, get_source_payment, get_payment_bundle):
    """Проверяет случай, когда указан неверный счет плательщика.

    Банк уведомляет об этом сразу в ответе на getStatus.
    """
    mock_post(read_fixture(
        'payment/get_status_invalid_payer_account.xml', decode='utf-8'), url_filter=lambda url: 'upg' in url)

    payment = get_source_payment(associate=Raiffeisen)
    bundle = get_payment_bundle([payment])

    Request(
        type=Request.TYPE_PAYMENT,
        associate_id=Raiffeisen.id,
        remote_id='b70e14cc-a0dd-452d-a2fb-daa0842e497c',
        object_id=payment.id,
        bundle_id=bundle.id
    ).save()

    get_registered_task('raiff_payment_status_sync_prepare').func()

    payment.refresh_from_db()
    bundle.refresh_from_db()
    assert payment.status == states.DECLINED_BY_BANK
    assert bundle.status == states.DECLINED_BY_BANK
    assert payment.processing_notes == '00004: В поле "Счет клиента" счет указан неверно'


def test_invalid_payee_account(mock_post, get_source_payment, read_fixture, get_payment_bundle):
    """Проверяет случай, когда указан неверный счет получателя.

    В этом случае ошибка приходит в квитке после запроса статуса по docId.
    """
    mock_post(read_fixture(
        'payment/doc_get_status_invalid_payee_account.xml', decode='utf-8'), url_filter=lambda url: 'upg' in url)

    payment = get_source_payment(associate=Raiffeisen, attrs={'number': '8782'})
    bundle = get_payment_bundle([payment])

    Request(
        type=Request.TYPE_PAYMENT,
        associate_id=Raiffeisen.id,
        remote_id='11dbdd3c-6e3c-4269-b33d-1939a26642d1',
        doc_id='04a56712-654d-4809-8ba2-65c6f54b4cb5',
        doc_request_id='11dbdd3c-6e3c-4269-b33d-1939a26642d1',
        object_id=payment.id,
        bundle_id=bundle.id
    ).save()

    get_registered_task('raiff_payment_status_sync').func()

    payment = Payment.objects.get(id=payment.id)
    bundle.refresh_from_db()
    assert payment.status == states.DECLINED_BY_BANK
    assert bundle.status == states.DECLINED_BY_BANK
    assert payment.processing_notes == (
        '\nОшибка: Код валюты счета получателя не найден в справочнике валют'
        '\nОшибка: Код валюты счета получателя не соответствует национальной валюте 810'
    )


@pytest.mark.parametrize('empty_field', ['t_cacc', 'f_cacc', None])
def test_payment_xml_valid(empty_field, get_payment_bundle, get_source_payment, validate_xml):

    oebs_payment1 = get_source_payment(
        {'f_bic': Raiffeisen.bid, empty_field: ''} if empty_field else {'f_bic': Raiffeisen.bid})

    compiled = Raiffeisen.payment_dispatcher.get_creator(
        get_payment_bundle([oebs_payment1], h2h=True)
    ).create_bundle()

    valid, log, err_count = validate_xml('request.xsd', RaiffRequest(compiled).to_xml(), [])
    assert err_count == 0, log


def test_payment_empty_fields(get_payment_bundle, get_source_payment):

    oebs_payment1 = get_source_payment({'f_bic': Raiffeisen.bid, 't_bic': ''})

    with pytest.raises(ValidationError) as e:
        Raiffeisen.payment_dispatcher.get_creator(
            get_payment_bundle([oebs_payment1], h2h=True)
        ).create_bundle()
    assert 'БИК получателя' in e.value.msg

    params = {'f_bic': Raiffeisen.bid}
    correct_payment = get_source_payment(params)

    mandatory_fields = ['f_bankname', 'f_inn', 't_name', 't_bankname', 't_bank_city', 't_acc', 't_bic']
    for check in mandatory_fields:
        params[check] = ''

    params['f_inn'] = '1234567891234'
    params['t_inn'] = '1234567891234'
    params['t_kpp'] = '111'
    incorrect_payment = get_source_payment(params)
    bundle = get_payment_bundle([correct_payment, incorrect_payment], h2h=True)

    Raiffeisen.payment_dispatcher.get_creator(bundle).create_bundle()

    bundle = PaymentsBundle.objects.get(id=bundle.id)
    incorrect_payment.refresh_from_db()

    assert len(bundle.payments) == 1
    assert incorrect_payment.status == states.BCL_INVALIDATED
    assert all([a in incorrect_payment.processing_notes for a in mandatory_fields])
    assert 'Значение обязательно, но не указано' in incorrect_payment.processing_notes
    assert 't_kpp' in incorrect_payment.processing_notes


def test_payments_keep_order(get_payment_bundle, get_source_payment):
    payment1 = get_source_payment({'number': '1111', 'f_bic': Raiffeisen.bid})
    payment2 = get_source_payment({'number': '2222', 'f_bic': Raiffeisen.bid})
    payment3 = get_source_payment({'number': '3333', 'f_bic': Raiffeisen.bid})

    compiled = Raiffeisen.payment_dispatcher.get_creator(
        get_payment_bundle([payment1, payment2, payment3], h2h=True)
    ).create_bundle()

    assert re.match('.*docNum="1111".*docNum="2222".*docNum="3333"', compiled)


def test_tax_digest_and_payment(
        validate_xml, get_payment_bundle, get_source_payment,
        read_fixture, mock_post, mock_signer, get_signing_right
):
    """Проверяет создание и отправку платежа host-to-host и обновление его статуса."""
    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')
    get_signing_right(Raiffeisen.id, 'serial')

    payment_attrs = dict(
        number='7675',
        date=datetime(2017, 10, 9),

        f_acc='40702810800002400742',
        f_inn='7736207543',
        f_name='ООО ЯНДЕКС',
        f_cacc='30101810200000000700',
        f_bic='044525700',
        f_bankname='АО «РАЙФФАЙЗЕНБАНК»',

        t_acc='40702810600001400744',
        t_inn='7708764126',
        t_name='ООО «Получатель»',
        t_bic='040173604',
        t_bankname='АЛТАЙСКОЕ ОТДЕЛЕНИЕ N8644 ПАО СБЕРБАНК',
        t_bank_city='БАРНАУЛ',
        t_cacc='30101810200000000604',

        n_kbk='0',
        n_doc_num='8',
        n_type='1',
        n_ground='1',
        n_period='ГД.00.2012',
        n_okato='11',
        n_status = '2',
        n_doc_date='29-10-2018',

        ground='{VO01010} В том числе «НДС» 18.00 % - 1.53.',
        summ='100'
    )

    def create_bundle(pay_attrs):
        payment = get_source_payment(associate=Raiffeisen, attrs=pay_attrs)
        payment_bundle = get_payment_bundle((payment,), h2h=True)  # type: PaymentsBundle
        compiled = Raiffeisen.payment_dispatcher.get_creator(payment_bundle).create_bundle()
        digest = payment_bundle.digests[0].decode('utf-8')
        return digest, compiled

    digest, compiled = create_bundle(payment_attrs)

    assert 'КБК=0' in digest  # n_okato
    assert 'ОКАТО=11' in digest  # n_kbk
    assert 'Основание платежа=1' in digest  # n_ground

    # n_period
    assert 'Налоговый период (день)=ГД' in digest
    assert 'Налоговый период (месяц)=00' in digest
    assert 'Налоговый период (год)=2012' in digest

    # n_doc_date
    assert 'Дата налогового документа (день)=29' in digest
    assert 'Дата налогового документа (месяц)=10' in digest
    assert 'Дата налогового документа (год)=2018' in digest

    assert 'Тип налогового платежа=1' in digest  # n_type
    assert 'Номер налогового документа=8' in digest  # n_doc_num

    assert 'Наз. пл.=' not in digest

    xml = RaiffRequest(compiled).to_xml()
    valid, log, err_count = validate_xml('request.xsd', xml, [])
    assert 'docDate="29.10.2018"' in xml
    assert 'incomeCode' not in xml
    assert err_count == 0, log

    payment_attrs['income_type'] = '3'
    digest, compiled = create_bundle(payment_attrs)
    xml = RaiffRequest(compiled).to_xml()
    assert 'Наз. пл.=3\n' in digest
    assert 'incomeCode="3"' in xml


@pytest.mark.parametrize('n_period', ['0', '10000010'])
def test_tax_payment_state_duty(
        n_period, validate_xml, get_payment_bundle, get_source_payment,
        read_fixture, mock_post, mock_signer, get_signing_right
):
    """Проверка налогогового платежа (госпошлина)"""
    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')
    get_signing_right(Raiffeisen.id, 'serial')

    payment_attrs = dict(
        number='7675',
        date=datetime(2017, 10, 9),

        f_acc='40702810800002400742',
        f_inn='7736207543',
        f_name='ООО ЯНДЕКС',
        f_cacc='30101810200000000700',
        f_bic='044525700',
        f_bankname='АО «РАЙФФАЙЗЕНБАНК»',

        t_acc='40702810600001400744',
        t_inn='7708764126',
        t_name='ООО «Получатель»',
        t_bic='040173604',
        t_bankname='АЛТАЙСКОЕ ОТДЕЛЕНИЕ N8644 ПАО СБЕРБАНК',
        t_bank_city='БАРНАУЛ',
        t_cacc='30101810200000000604',

        n_kbk='0',
        n_doc_num='8',
        n_type='1',
        n_ground='1',
        n_period=n_period,
        n_okato='11',
        n_status='2',
        n_doc_date='0',

        ground='{VO01010} В том числе «НДС» 18.00 % - 1.53.',
        summ='100'
    )
    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)
    payment_bundle = get_payment_bundle((payment,), h2h=True)  # type: PaymentsBundle
    compiled = Raiffeisen.payment_dispatcher.get_creator(payment_bundle).create_bundle()
    digest = payment_bundle.digests[0].decode('utf-8')

    assert 'КБК=0' in digest  # n_okato
    assert 'ОКАТО=11' in digest  # n_kbk
    assert 'Основание платежа=1' in digest  # n_ground

    # n_period
    assert f'Налоговый период (день)={n_period}' in digest
    assert 'Налоговый период (месяц)=' in digest
    assert 'Налоговый период (год)=' in digest

    # n_doc_date
    assert 'Дата налогового документа (день)=0' in digest
    assert 'Дата налогового документа (месяц)=' in digest
    assert 'Дата налогового документа (год)=' in digest

    assert 'Тип налогового платежа=1' in digest  # n_type
    assert 'Номер налогового документа=8' in digest  # n_doc_num

    xml = RaiffRequest(compiled).to_xml()
    valid, log, err_count = validate_xml('request.xsd', xml, [])
    assert 'docDate="0"' in xml
    assert f'taxPeriod="{n_period}"' in xml
    assert err_count == 0, log


def test_check_doc_type_cur(mock_post, get_source_payment, read_fixture, mock_signer, get_bundle, mocker):
    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')

    # Ответ на запрос GetStatus для идентификатора из ответа sendRequests
    get_status_delivered = read_fixture('payment/get_status_delivered.xml', decode='utf-8')

    # Ответ на запрос DocIds
    send_doc_response = read_fixture('payment/sendrequest_response.xml', decode='utf-8')

    payment_attrs = {
        't_bic': '',
        'currency_id': Currency.USD,
        'ground': 'Some ground',
        'i_swiftcode': 'AGCAAM22',
        'summ': '23.10',
        't_country': '528'
    }

    payment = get_source_payment(associate=Raiffeisen, attrs=payment_attrs)

    mocker.patch('bcl.toolbox.utils.Swift.get_bic_info', lambda swiftcode: SWIFT_DATA.get(swiftcode, {}))

    get_bundle(payment)

    mock_post(get_status_delivered, send_doc_response)
    PaymentSyncer(associate=Raiffeisen).prepare()
    assert 'docType="Заявление на перевод средств в иностранной валюте"' in mock_post.last_request


def test_check_doc_type_rub(mock_post, get_source_payment, read_fixture, mock_signer, get_bundle):
    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')

    # Ответ на запрос GetStatus для идентификатора из ответа sendRequests
    get_status_delivered = read_fixture('payment/get_status_delivered.xml', decode='utf-8')

    # Ответ на запрос DocIds
    send_doc_response = read_fixture('payment/sendrequest_response.xml', decode='utf-8')

    payment = get_source_payment(associate=Raiffeisen, attrs={'currency_id': Currency.RUB})

    get_bundle(payment)

    mock_post(get_status_delivered, send_doc_response)
    PaymentSyncer(associate=Raiffeisen).prepare()
    assert 'docType="Платежное поручение"' in mock_post.last_request


def test_get_swift_info(monkeypatch):
    assert not RaiffUpgPaymentCreator.get_info_swift('1233455')

    def mock_func(*args, **kwargs):

        class Test:
            def get_bics_info(self, *args, **kwargs):
                return {
                    'GENODEM1GLS': {
                        'countryName': 'ГЕРМАНИЯ', 'instName': 'GLS GEMEINSCHAFTSBANK EG', 'branchInfo': '',
                        'addrOpArea': '', 'addrOpCity': 'BOCHUM', 'addrOpZip': '', 'addrOpRegion': '44789 BOCHUM CHRISTSTRASSE 9',
                        'bic8': 'GENODEM1', 'bicBranch': 'GLS', 'addrOpStreet': '', 'addrOpStreetNumber': '',
                        'addrOpBuilding': '', 'addrRegStreet': 'CHRISTSTRASSE 9', 'countryCode': 'DE'
                    }
                }

        return Test()

    monkeypatch.setattr('bcl.toolbox.utils.Swift.get_swift_ref', mock_func)

    swift_info = RaiffUpgPaymentCreator.get_info_swift('GENODEM1GLS')

    assert swift_info['name'] == 'GLS GEMEINSCHAFTSBANK EG'
    assert swift_info['city'] == 'BOCHUM'
    assert swift_info['street'] == 'CHRISTSTRASSE 9'
    assert swift_info['country']['name'] == 'ГЕРМАНИЯ'
    assert swift_info['addr'] == '44789 BOCHUM CHRISTSTRASSE 9'

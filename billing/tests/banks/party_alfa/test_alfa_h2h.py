from datetime import datetime
from typing import List
from uuid import uuid4

import pytest

from bcl.banks.registry import Alfa
from bcl.core.models import Request, states, Statement, Payment, RequestLog, PaymentsBundle
from bcl.exceptions import RemoteError
from bcl.toolbox.utils import DateUtils

ACC_NUM = '40702810001300013144'

URL_PAYMENTS = 'https://grampus-int.alfabank.ru/API/v1/ISO20022/Payments'
URL_STATEMENTS = 'https://grampus-int.alfabank.ru/API/v1/ISO20022/Statements'


@pytest.fixture
def get_alfa_bundle(build_payment_bundle, get_assoc_acc_curr):

    def get_alfa_bundle_(payment_dicts=None, *, exported=False):
        _, acc, _ = get_assoc_acc_curr(Alfa, account=ACC_NUM)
        get_uuid = lambda: str(uuid4()).upper()

        bundle = build_payment_bundle(
            Alfa, payment_dicts=payment_dicts or [{'number_src': get_uuid()}, {'number_src': get_uuid()}], account=acc,
            h2h=True
        )
        if exported:
            bundle.status = states.EXPORTED_H2H
            bundle.sent = True
            bundle.save()

            for payment in bundle.payments:
                payment.status = states.EXPORTED_H2H

            Payment.objects.bulk_update(bundle.payments, fields=['status'])

        return bundle

    return get_alfa_bundle_


@pytest.fixture
def get_positive_status_response(read_fixture):

    def add_prefix(value):
        if isinstance(value, int):
            return Alfa.payment_num_prefix_add(value)
        return value  # Если здесь строка, то собиратель в пакет уже добавил префикс.

    def get_positive_status_response_(bundle):
        response = read_fixture('alfa_mx_statuses_positive.xml', decode='utf-8')

        response = response.replace(
            '{bundle_num}', add_prefix(bundle.id)).replace(
            '{pay1}', add_prefix(bundle.payments[0].number_src.replace('-', ''))).replace(
            '{pay2}', add_prefix(bundle.payments[1].number_src.replace('-', '')))

        return response

    return get_positive_status_response_


def test_payment_candidates(get_alfa_bundle, response_mock, get_automation_bundle, get_positive_status_response):
    def create_bundle_with_dt(date=DateUtils.today()):

        bundle = get_alfa_bundle(exported=True)
        bundle.dt = date
        bundle.save()
        return bundle

    # Проверяем фильтр по дате
    # сегодняшний пакет
    bundle_1 = create_bundle_with_dt()
    # два дня назад
    bundle_2 = create_bundle_with_dt(DateUtils.days_from_now(2))
    # неделю назад
    bundle_3 = create_bundle_with_dt(DateUtils.days_from_now(7))

    # проверяем, что пакет в конечном статусе, в котором все платежи проведены, не будет синхронизирован
    bundle_4 = get_alfa_bundle(exported=True)
    bundle_4.set_status(states.ACCEPTED_H2H, propagate_to_payments=False)
    bundle_4.update_payments_status(states.COMPLETE)

    # проверяем, что пакет в конечном статусе, в котором не все платежи проведены, будет синхронизирован
    bundle_5 = get_alfa_bundle(exported=True)
    bundle_5.update_payments_status(states.COMPLETE, exclude={'id': bundle_5.payments[0].id})
    bundle_5.update_payments_status(states.PROCESSING, exclude={'id': bundle_5.payments[1].id})
    bundle_5.set_status(states.ACCEPTED_H2H, propagate_to_payments=False)

    request_list = [
        f'GET {URL_PAYMENTS}/{Alfa.payment_num_prefix_add(bundle.id)} -> 200 :{get_positive_status_response(bundle)}'
        for bundle in [bundle_1, bundle_2, bundle_5]
    ]

    with response_mock(request_list):
        Alfa.automate_payments_sync()


@pytest.mark.parametrize('bundle_status', [states.PROCESSING, states.EXPORTED_H2H])
def test_payment_synchronization(
    bundle_status, get_alfa_bundle, dss_signing_right, read_fixture, run_task, response_mock, get_positive_status_response):

    bundle = get_alfa_bundle(exported=True)
    bundle_num = Alfa.payment_num_prefix_add(bundle.id)
    bundle.set_status(bundle_status, propagate_to_payments=False)

    # Негатив. Запрашиваем неизвестный серверу пакет.
    with response_mock(
        f'GET {URL_PAYMENTS}/{bundle_num} -> 404 :'
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.002.001.06">'
        '<CstmrPmtStsRpt><GrpHdr><MsgId>1b930ad7008f4296b858a3ca71935bf9</MsgId>'
        '<CreDtTm>2020-01-14T08:48:39.278+03:00</CreDtTm><InitgPty><Id><OrgId><AnyBIC>ALFARUMM</AnyBIC>'
        '<Othr><Id>044525593</Id></Othr></OrgId></Id></InitgPty></GrpHdr><OrgnlGrpInfAndSts>'
        '<OrgnlMsgId>time_id:1578980919278</OrgnlMsgId><OrgnlMsgNmId>pain.001.001.06</OrgnlMsgNmId>'
        '<OrgnlCreDtTm>2020-01-14T08:48:39.278+03:00</OrgnlCreDtTm><StsRsnInf><Rsn><Cd>NARR</Cd></Rsn>'
        '<AddtlInf>Request has not been found by MsgId</AddtlInf></StsRsnInf></OrgnlGrpInfAndSts>'
        '</CstmrPmtStsRpt></Document>'
    ):
        run_task('alfa_statuses')

    log = list(RequestLog.objects.all())
    assert len(log) == 1
    log_entry = log[0]
    assert log_entry.associate_id == Alfa.id
    assert 'Request has not been found' in log_entry.data
    assert 'Authorization: <cut>' in log_entry.data

    bundle.refresh_from_db()
    assert bundle.sent
    assert bundle.status == bundle_status

    # Позитив.
    with response_mock(f'GET {URL_PAYMENTS}/{bundle_num} -> 200 :{get_positive_status_response(bundle)}'):
        Alfa.automate_payments_sync(bundle=bundle)

    bundle.refresh_from_db()
    assert bundle.sent
    assert bundle.status == states.ACCEPTED_H2H
    assert bundle.processing_notes == 'Принято к исполнению частично:'

    payment = bundle.payments[0]
    assert payment.is_complete
    assert payment.processing_notes == 'Исполнено\nNARR: Выполнен передан в банк получателя'

    payment = bundle.payments[1]
    assert payment.is_declined_by_bank
    assert payment.processing_notes == 'Отклонено\nNARR: Technical exception. Please call service desk.'


@pytest.fixture
def get_automation_bundle(get_alfa_bundle, dss_signing_right, dss_signing_mock, response_mock, read_fixture):

    def get_automation_bundle_(*, fixture_name: str, status: str, payment_dicts: List[dict] = None) -> PaymentsBundle:
        dss_signing_right(associate=Alfa, serial='12000d3ece1cbf936c80c766fb0000000d3ece', autosigning=True, level=1)
        dss_signing_mock(signed='xmlsig')

        bundle = get_alfa_bundle(payment_dicts=payment_dicts)
        bundle_num = Alfa.payment_num_prefix_add(bundle.id)

        org = bundle.account.org
        org.connection_id = 'test'
        org.save()

        with response_mock(
            f'POST {URL_PAYMENTS} -> {status} :' +
            read_fixture(fixture_name).decode()
                .replace('<OrgnlMsgId>1</OrgnlMsgId>', f'<OrgnlMsgId>{bundle_num}</OrgnlMsgId>')
                .replace('{msgid}', f'{bundle_num}')
        ):
            bundle = Alfa.automate_payments(bundle=bundle)

        return bundle

    return get_automation_bundle_


def test_unhandled_exception(get_automation_bundle):
    # Исключение на этапе разбора ответе на отсылку пакета.
    bundle = get_automation_bundle(fixture_name='alfa_mx_statuses_bogus.xml', status='200')
    assert bundle.sent
    assert bundle.status == states.EXPORTED_H2H
    assert bundle.processing_notes == ''
    responses = bundle.remote_responses

    assert len(responses) == 1
    assert 'Exception: Opening and ending tag mismatch' in responses[0]['bcl_msg']


def test_no_acceptance_date(get_automation_bundle):
    bundle = get_automation_bundle(
        fixture_name='alfa_mx_statuses_rcvd.xml', status='200',
        payment_dicts=[
            {'number_src': 'B46D9251-2770-3964-E053-047BA8C09BBB'},
            {'number_src': 'B46D9251-2860-3964-E053-047BA8C09BBB'}
        ]
    )
    assert bundle.sent
    assert bundle.status == states.EXPORTED_H2H
    assert bundle.processing_notes == ''
    assert bundle.remote_responses == [{'status': 'RCVD', 'reason': ''}]

    payments = list(bundle.payments)
    assert payments[0].processing_notes == 'Исполнено\nNARR: Выполнен передан в банк получателя'
    assert payments[1].processing_notes == 'Отклонено\nNARR: Technical exception. Please call service desk.'


def test_bundle_already_exists(get_automation_bundle):

    # Обработка повторной отправки пакета.
    bundle = get_automation_bundle(fixture_name='alfa_mx_statuses_exists.xml', status='400')

    assert bundle.remote_responses == []
    assert bundle.status == states.EXPORTED_H2H

    payment = bundle.payments[0]
    assert payment.is_exported_h2h


def test_payment_automation(get_alfa_bundle, response_mock, get_automation_bundle, get_positive_status_response):

    # Негатив. Запрет на подпись.
    bundle = get_automation_bundle(fixture_name='alfa_mx_statuses_negative.xml', status='403')

    assert bundle.remote_responses == [
        {'status': 'RJCT', 'reason': 'DS0G Signer is not allowed to sign this operation type'}
    ]
    assert bundle.sent
    assert bundle.status == states.DECLINED_BY_BANK
    assert bundle.processing_notes == 'Отклонено: DS0G Signer is not allowed to sign this operation type'

    # Позитив.
    bundle = get_alfa_bundle()

    with response_mock(f'POST {URL_PAYMENTS} -> 200 :{get_positive_status_response(bundle)}'):
        bundle = Alfa.automate_payments(bundle=bundle)
        bundle.refresh_from_db()

    assert bundle.status == states.EXPORTED_H2H

    payment = bundle.payments[0]
    assert payment.is_complete
    assert payment.processing_notes == 'Исполнено\nNARR: Выполнен передан в банк получателя'

    payment = bundle.payments[1]
    assert payment.is_declined_by_bank
    assert payment.processing_notes == 'Отклонено\nNARR: Technical exception. Please call service desk.'


def test_statement_processing(
    get_assoc_acc_curr, dss_signing_right, init_user, monkeypatch, dss_signing_mock, read_fixture, run_task,
    response_mock, time_freeze,
):

    dss_signing_mock()

    dss_signing_right(associate=Alfa, serial='01de325f0033ab80b942e1a141795f0a75')

    _, acc1, _ = get_assoc_acc_curr(Alfa, account=ACC_NUM)
    get_assoc_acc_curr(Alfa, account='2')

    with time_freeze(datetime.utcnow()):

        # Негатив.
        with response_mock(f'POST {URL_STATEMENTS} -> 403 :Signer is not allowed to sign this operation type'):

            with pytest.raises(RemoteError) as e:
                run_task('alfa_statements_prepare')

            assert 'Signer is not allowed to sign this operation type' in e.value.msg
            assert 'Account 40702810001300013144. RemoteError: ' in e.value.msg
            assert 'Account 2. RemoteError: ' in e.value.msg

        # Запуск забора выписок, когда ещё не зарегистрированы запросы на них.
        run_task('alfa_statements')

        # Позитив.
        with response_mock(f'POST {URL_STATEMENTS} -> 200 :OK'):
            run_task('alfa_statements_prepare')

        requests = list(Request.unprocessed.order_by('id').all())
        assert len(requests) == 2
        assert requests[0].object_id == acc1.id

        _, acc_unprocessed, _ = get_assoc_acc_curr(Alfa, account='3')

        Request(
            associate_id=Alfa.id,
            type=Request.TYPE_STATEMENT_REQUEST,
            object_id=acc_unprocessed.id
        ).save()

        requests_alfa = list(Request.unprocessed.order_by('id').all())

        # Проверяем, что не запрашиваем выписки по уже запрошенным счетам
        with response_mock(''):  # удостоверимся, что не ходим наружу
            run_task('alfa_statements_prepare')

        requests = list(Request.unprocessed.order_by('id').all())
        assert requests == requests_alfa

        with response_mock([
            f'GET {URL_STATEMENTS}/{requests[0].remote_id} -> 200 :' +
            read_fixture('alfa_mx_statement.xml').decode(),

            f'GET {URL_STATEMENTS}/{requests[1].remote_id} -> 404 :'
            'Request has not been found by MsgId',
        ]):
            init_user(robot=True)
            run_task('alfa_statements')

    requests = list(Request.objects.order_by('id').all())
    assert len(requests) == 3

    request = requests[0]
    assert request.object_id == acc1.id
    assert request.is_ok
    assert request.processed

    request = requests[1]
    assert request.is_error
    assert not request.processed
    assert request.error_message == (
        'Account 2. RemoteError: 404 Request has not been found by MsgId')

    request = requests[2]
    assert request.object_id == acc_unprocessed.id
    assert request.is_error
    assert not request.processed
    assert 'ConnectionError' in request.error_message

    statements = list(Statement.objects.all())
    assert len(statements) == 1
    statement = statements[0]
    assert statement.is_type_final
    register, proved = statement.process()[0]
    assert register.is_valid
    assert len(proved) == 1


def test_statement_intraday(
    get_assoc_acc_curr, dss_signing_right, init_user, dss_signing_mock, run_task, response_mock, statement_parse):

    dss_signing_mock()

    dss_signing_right(associate=Alfa, serial='01de325f0033ab80b942e1a141795f0a75')

    _, acc1, _ = get_assoc_acc_curr(Alfa, account='40702810001300013144')

    with response_mock(f'POST {URL_STATEMENTS} -> 200 :OK'):
        run_task('alfa_statements_intraday_prepare')

    requests = list(Request.unprocessed.order_by('id').all())
    assert len(requests) == 1
    assert requests[0].object_id == acc1.id

    with response_mock(
        f'GET {URL_STATEMENTS}/{requests[0].remote_id} -> 200 :'
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.053.001.05" '
        'xmlns:ns2="urn:iso:std:iso:20022:tech:xsd:camt.060.001.03"><BkToCstmrStmt><GrpHdr>'
        '<MsgId>05b8816789fc49e3b4dbbc146a120c1c</MsgId><CreDtTm>2020-01-17T06:56:00.391+03:00</CreDtTm>'
        '</GrpHdr><Stmt><Id>05250559cadf47e5a3d1870b55ce2525</Id><CreDtTm>2020-01-17T06:56:00.391+03:00'
        '</CreDtTm><FrToDt><FrDtTm>2020-01-17T03:00:00</FrDtTm><ToDtTm>2020-01-17T03:00:00</ToDtTm></FrToDt>'
        '<Acct><Id><Othr><Id>40702810001300013144</Id></Othr></Id><Ownr><Nm>ООО "Мир Технологий"</Nm>'
        '<Id><OrgId><Othr><Id>6315016823</Id><SchmeNm><Cd>TXID</Cd></SchmeNm></Othr></OrgId></Id></Ownr>'
        '<Svcr><FinInstnId><BICFI>ALFARUMMXXX</BICFI><ClrSysMmbId><ClrSysId><Cd>RUCBC</Cd></ClrSysId>'
        '<MmbId>044525593</MmbId></ClrSysMmbId><Nm>ДО "ЦЕНТР ОБСЛУЖИВ.КРУПНЫХ КОРПОР.КЛИЕНТОВ" г.МОСКВА '
        'АО"АЛЬФА-БАНК"</Nm><PstlAdr><AdrLine>107078,Россия, г.Москва, ул.Маши   Порываевой д.34</AdrLine>'
        '</PstlAdr><Othr><Id>7728168971</Id></Othr></FinInstnId></Svcr></Acct><Bal><Tp><CdOrPrtry>'
        '<Cd>OPBD</Cd></CdOrPrtry></Tp><Amt Ccy="RUR">18819386.30</Amt><CdtDbtInd>CRDT</CdtDbtInd>'
        '<Dt><Dt>2020-01-17</Dt></Dt></Bal><Bal><Tp><CdOrPrtry><Cd>CLBD</Cd></CdOrPrtry></Tp>'
        '<Amt Ccy="RUR">18819386.30</Amt><CdtDbtInd>DBIT</CdtDbtInd><Dt><Dt>2020-01-17</Dt></Dt></Bal>'
        '<TxsSummry><TtlCdtNtries><Sum>0.00</Sum></TtlCdtNtries><TtlDbtNtries><Sum>0.00</Sum></TtlDbtNtries>'
        '</TxsSummry></Stmt></BkToCstmrStmt></Document>'
    ):
        init_user(robot=True)

        run_task('alfa_statements_intraday')

    statements = list(Statement.objects.all())
    assert len(statements) == 1
    statement = statements[0]
    assert statement.is_type_intraday
    assert statement.is_processing_ready

    result = statement_parse(statement)
    register = result[0][0]
    assert register.is_valid
    assert register.intraday


@pytest.mark.skip('Тест для ручной отладки интеграции с банком.')
def test_manual_bundle(get_alfa_bundle, dss_signing_right, monkeypatch, get_assoc_acc_curr, build_payment_bundle):
    """Реквизиты платежа в данном тесте специально предложены банком для тестов в Песочнице."""

    _, acc, _ = get_assoc_acc_curr(
        Alfa, account=ACC_NUM, org='Общество с ограниченной ответственностью "Мир технологий"'
    )

    number_prefix = f"yadev-{datetime.now().strftime('%Y%m%d%H%M')}-"

    monkeypatch.setattr('bcl.banks.party_alfa.Alfa.payment_num_prefix', number_prefix)

    dss_signing_right(associate=Alfa, serial='01de325f0033ab80b942e1a141795f0a75', autosigning=True, level=1)

    bundle = build_payment_bundle(Alfa, payment_dicts=[
        # Ожидается прохождение платежа.
        {
            'f_inn': '6315016823',
            't_inn': '5036045205',
            't_acc': '40702810901850000008',
            'f_cacc': '30101810200000000593',
            't_cacc': '30101810200000000593',
            'f_name': 'Общество с ограниченной ответственностью "Мир технологий"',
            't_name': 'АО "ДИКСИ Юг"',
            'f_bic': '044525593',
            'f_bankname': 'АО "АЛЬФА-БАНК" Г МОСКВА',
            't_bankname': 'АО "АЛЬФА-БАНК" Г МОСКВА',
            'f_kpp': '',
            't_kpp': '',
            'n_kod': '0',
            'summ': '1.2',
            'ground': 'Тестирование',
            'paid_by': 'OUR',
            'oper_code': '',
        },
        # Ожидается отказ платежа.
        {
            'summ': '1.1',
            'f_name': 'ООО "Тест"',
        },
    ], account=acc, h2h=True)

    create = True

    if create:
        result = Alfa.automate_payments(bundle=bundle)

    # Здесь неплохо было бы остановиться на некоторое время, подождать,
    # пока банк переварит, а за одно ещё и запомнить на всякий случай,
    # что лежит number_prefix (это может понадобиться, если захочется
    # получить данные по платежу в отдельно прогоне для следующей строки).
    Alfa.automate_payments_sync(bundle=bundle)

    bundle.refresh_from_db()
    payments = list(bundle.payments)

    print(number_prefix)  # На этой строке удобно ставить точку остановки.


def test_contents(get_alfa_bundle, validate_xml):

    associate = Alfa

    bundle = get_alfa_bundle(payment_dicts=[{
        'income_type': '3',
        'number_src': '43115587-E3E8-32F6-E055-000000B50DB1',
    }])
    compiled = bundle.tst_compiled

    associate.payment_sender.validate_contents(compiled)

    # тип зачисления
    assert '<Tp>PTCD</Tp>' in compiled
    assert '<Cd>3</Cd>' in compiled

    # использование number_src для обеспечения уникальности
    assert f'<InstrId>43115587E3E832F6E055000000B50DB1' in compiled
    assert f'g43115587E3E832F6E055000000B50DB1' in compiled

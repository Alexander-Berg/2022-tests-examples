import datetime
from decimal import Decimal

import pytest

from bcl.banks.common.letters import IncomingLetter
from bcl.banks.common.letters import ProveLetter
from bcl.banks.party_raiff.statement_downloader import RaiffStatementDownloader
from bcl.banks.party_raiff_spb import RaiffeisenSpb
from bcl.banks.registry import Raiffeisen
from bcl.core.models import (
    StatementRegister, Organization, Request, Currency, StatementPayment, states, Direction,
    Letter, Attachment,
)


@pytest.fixture(autouse=True)
def mock_objects(monkeypatch, mock_signer, get_signing_right, read_fixture):
    get_signing_right(Raiffeisen.id, 'serial')
    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')


@pytest.mark.parametrize('has_payment', [True, False])
def test_incoming_processing(has_payment, get_assoc_acc_curr, read_fixture, mock_post, init_user):
    """Проверяет загрузку внутридневной выписки."""

    associate = Raiffeisen

    init_user(robot=True)
    testuser = init_user('testuser')

    _, account, _ = get_assoc_acc_curr(
        associate.id, account='30601810800000000006', org=Organization(id=7, name='ООО ЯНДЕКС', inn='7736207543')
    )

    # Проходит время - забираем входящие документы и запрашиваем.
    send_incoming_response = (
        '''<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
        <ns1:sendRequestsResponse xmlns:ns1="http://upg.sbns.bssys.com/">
        <return xmlns="http://upg.sbns.bssys.com/">d5454355-74aa-4749-bfd0-d2ce956706f1</return>
        </ns1:sendRequestsResponse></soap:Body></soap:Envelope>'''
    )

    get_status_response = read_fixture('statement/statement_intraday.xml')

    # Создадим исходящее письмо, на которое ждём ответ.
    letter_out = Letter(
        associate_id=associate.id,
        user=testuser,
        subject='outgoing',
        type_id=ProveLetter.id,
    )
    letter_out.save()
    # подменим ид исходящего письма на актуальный
    get_status_response = get_status_response.replace(b'25963', f'{letter_out.id}'.encode())

    if has_payment:
        # ветка, где платёж по выписке существует
        date_now = datetime.datetime.now().date()
        get_status_response = get_status_response.decode('utf-8').replace(
            'operDate=&quot;2017-10-10&quot;',
            f"operDate=&quot;{date_now}&quot;").encode('utf-8')

    # Первый раз отправляется запрос входящих документов.
    mock_post(send_incoming_response)
    downloader = RaiffStatementDownloader(final=False)
    downloader.process(final=False)

    # Проходит время - забираем входящие документы и запрашиваем.
    mock_post(get_status_response, send_incoming_response)
    statements = downloader.process(final=False)

    assert len(statements) == 1

    # ввиду того, что другие типы документов забираются вместе с выписками проверим их
    letters = list(Letter.objects.exclude(id=letter_out.id).order_by('id'))
    assert len(letters) == 2

    for idx, letter in enumerate(letters):
        assert not letter.hidden
        assert not letter.requires_answer
        assert letter.associate_id == associate.id
        assert letter.is_dir_in
        assert letter.is_complete
        assert letter.user_id == testuser.id
        assert letter.remote_id
        assert letter.subject == 'Ответ на запрос справки'
        assert letter.body.startswith('Уважаемый Клиент')

        Attachment.objects.get(oid=letter.id)

        if idx == 0:
            assert letter.account_id == account.id
            assert letter.important
            assert letter.type_id == IncomingLetter.id
            assert not letter.initial_id

        else:
            assert not letter.important
            assert letter.initial_id == letter_out.id
            assert letter.type_id == ProveLetter.id

    # далее пробуем разобрать выписку
    statement = statements[0]
    statement.user = testuser
    statement.save()

    parser = associate.statement_dispatcher.get_parser(statement)
    parser.process()

    payments = StatementPayment.objects.all()
    payments = sorted(payments, key=lambda p: int(p.summ))
    assert statement.is_type_intraday

    if not has_payment:
        assert len(payments) == 0
        return

    payment = payments[0]
    assert len(payments) == 3
    assert [payments[0].summ, payments[1].summ, payments[2].summ] == [Decimal('1.00'), Decimal('3.00'), Decimal('5.00')]

    assert payment.direction == payments[1].direction == Direction.IN
    assert payments[2].direction == Direction.OUT
    assert payment.get_info_account() == '40702810100020001384'
    assert payments[2].info['03'] == '40702810023000404101'
    assert payment.get_info_purpose() == (
        'Оплата за обслуживание 1С по сч. N 56 от 30.04.2015 г.Сумма 2000-00Без налога (НДС)'
    )
    assert payment.get_info_name() == 'ООО "Коммунальная система"'
    assert payment.get_info_inn() == '6317084970'
    assert payment.get_info_bik() == '043601863'
    assert payment.get_operation_type() == '01'

    assert payment.currency_id == Currency.RUB
    assert payment.funds_code == 'B'
    assert payment.trans_type == 'S'
    assert payment.date_valuated == date_now
    assert payment.intraday


def test_statement_downloader(
        get_assoc_acc_curr, read_fixture, get_source_payment, mock_post, init_user):
    """Проверяет загрузку итоговой выписки."""
    init_user(robot=True)
    testuser = init_user('testuser')

    _, account, _ = get_assoc_acc_curr(Raiffeisen.id, account='40702810500001400742')

    # Регистрируем второй счет из выписки. Нужно это, чтобы убедиться что вторая выписка на другую дату не грузится.
    get_assoc_acc_curr(Raiffeisen.id, account='40702810900000021641')

    source_payment_salary = get_source_payment({
        'salary_id': '279132',
        'summ': '100.00',
        'status': states.EXPORTED_H2H
    }, associate=Raiffeisen)

    source_payment = get_source_payment({
        'ground': '{VO01010} В том числе НДС 18.00 % - 1.53.',
        'summ': '200.00',
        'number': '6139',
        'f_acc': '40702810500001400742',
        'status': states.EXPORTED_ONLINE
    }, associate=Raiffeisen)

    send_incoming_response = read_fixture('statement/incoming_response.xml')
    get_status_incoming_response = read_fixture('statement/statement.xml')

    # Первый раз отправляется запрос входящих документов.
    mock_post(send_incoming_response)
    downloader = RaiffStatementDownloader(final=True)
    downloader.process()
    assert Request.objects.count() == 1

    # Проходит время - забираем входящие документы и запрашиваем.
    mock_post(get_status_incoming_response, send_incoming_response)
    statements = downloader.process()

    assert len(statements) == 3

    for statement_ in statements:
        if b'docDate="2017-10-09"' in statement_.zip_raw:
            statement = statement_
            break

    statement.user = testuser
    statement.save()

    parser = Raiffeisen.statement_dispatcher.get_parser(statement)
    parser.process()

    register: StatementRegister = StatementRegister.objects.all()[0]

    assert register.statement_date == datetime.datetime(2017, 10, 9).date()
    assert register.closing_balance == 8688
    assert register.account.number == '40702810500001400742'
    assert register.status == 1

    payments = StatementPayment.objects.all()
    payments = sorted(payments, key=lambda p: int(p.summ))

    assert len(payments) == 2
    payment = payments[0]  # type: StatementPayment

    assert [payments[0].summ, payments[1].summ] == [Decimal('100'), Decimal('200')]

    assert payment.is_out == payments[1].is_out

    assert payment.get_info_account() == payments[1].info['03'] == '40702810600001400744'
    assert payment.get_info_purpose() == (
        '{VO70060} Перечисление зарплаты за октябрь 2017 г. согласно платежной ведомости №279132'
    )
    assert payment.get_info_name() == 'ООО Получатель'
    assert payment.get_info_inn() == '7708764126'
    assert payment.get_info_bik() == '040173604'
    assert payment.get_operation_type() == '01'

    assert payment.currency_id == Currency.RUB
    assert payment.funds_code == 'B'
    assert payment.trans_type == 'NTRF'
    assert payment.date_valuated == datetime.date(2017, 10, 9)
    assert not payment.intraday

    def check_payment(payment_oebs, provedpay):
        payment_oebs.refresh_from_db()

        assert payment_oebs.statementpayment_set.all()[0].number == provedpay.number
        assert payment_oebs.is_complete

    check_payment(source_payment_salary, payment)
    check_payment(source_payment, payments[1])


def test_no_acc_statement_parser(parse_statement_fixture):
    register, _ = parse_statement_fixture(
        'statement/statement_extracted_no_accs.xml', Raiffeisen, '40702840700000009215', 'RUB',
        encoding='utf-8')[0]
    assert register.is_valid


def test_check_associate(get_assoc_acc_curr, read_fixture, get_source_payment, mock_post, init_user):
    """Проверяет загрузку итоговой выписки в райффайзен спб"""
    init_user(robot=True)
    init_user('testuser')

    _, account, _ = get_assoc_acc_curr(Raiffeisen.id, account='40702810500001400742')
    _, account_spb, _ = get_assoc_acc_curr(RaiffeisenSpb.id, account='40702810900000021649')

    send_incoming_response = read_fixture('statement/incoming_response.xml')
    get_status_incoming_response = read_fixture('statement/statement_raiff_and_raiffspb.xml')

    mock_post(send_incoming_response)
    downloader = RaiffStatementDownloader(final=True)
    downloader.process()
    assert Request.objects.count() == 1

    mock_post(get_status_incoming_response, send_incoming_response)
    statements = downloader.process()

    assert len(statements) == 3

    for statement_ in statements:
        if statement_.is_type_final and b'docDate="2017-10-10"' in statement_.zip_raw:
            assert statement_.associate_id == RaiffeisenSpb.id

        else:
            assert statement_.associate_id == Raiffeisen.id

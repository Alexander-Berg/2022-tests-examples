import json
from collections import namedtuple
from decimal import Decimal

import pytest

from bcl.banks.party_paypal import (
    PayPalPaymentCreator, PayPalPaymentSender, PayPalStatementParser,
    PayPalStatementSender, PayPalConnnector,
)
from bcl.banks.party_paypal.common import ALIAS_CH2, ALIAS_CH1, StatusBase, ALIAS_RU_ZEN
from bcl.banks.registry import PayPal
from bcl.core.models import Currency, Service, states, StatementRegister, PaymentsBundle
from bcl.core.tasks import process_bundles
from bcl.banks.party_paypal.utils import send_disputes
from bcl.exceptions import BclException
from bcl.toolbox.utils import DateUtils, XmlUtils


@pytest.mark.parametrize(
    'number, currency', [
        ('billingtest3@yandex.ru', Currency.USD),
        ('valeriya.kostina@gmail.com', Currency.RUB),
    ]
)
def test_balance_getter(number, currency, get_assoc_acc_curr, response_mock):
    associate = PayPal
    bypass_mock = False

    _, acc, _ = get_assoc_acc_curr(associate=associate, account={'number': number, 'currency_code': currency})

    with response_mock(
        'POST https://api-3t.sandbox.paypal.com/nvp ->200:'
        f'ACK=Success&L_AMT0=123.4&L_CURRENCYCODE0={Currency.by_num[currency]}',
        bypass=bypass_mock
    ):
        balance = PayPal.balance_getter(accounts=[acc]).run(account=acc)

    if bypass_mock:
        assert balance[0]
    else:
        assert balance[0] == '123.4'


def test_balance_getter_task(run_task, get_assoc_acc_curr, response_mock):

    _, acc, _ = get_assoc_acc_curr(associate=PayPal, account={'number': '111', 'currency_code': Currency.RUB})

    with response_mock(
        'POST https://api-3t.sandbox.paypal.com/nvp ->200:ACK=Error&L_SHORTMESSAGE0=bad&L_LONGMESSAGE0=damn',
    ):
        with pytest.raises(BclException) as e:
            run_task('update_accounts_balance')

    assert f'{e.value}' == '111 -> PayPal error [bad] damn'


def test_get_bundle_contents(build_payment_bundle):
    associate = PayPal
    bundle = build_payment_bundle(associate, service=Service.TOLOKA, h2h=True)
    contents = associate.payment_dispatcher.get_creator(bundle).get_bundle_contents()
    assert isinstance(contents, dict)


def test_prepare_send_sync(build_payment_bundle, monkeypatch, time_shift):

    def mock_exception(*args, **kwargs):
        raise Exception('Unhandled')

    def get_post_mock(*, status='', sync_mode=True, json_resp=None):
        def mock_post(*args, **kwargs):
            resp = {
                'batch_header': {
                    'payout_batch_id': 'testid',
                },
                'items': [
                    {
                        'errors': {'message': 'myfail'} if status == 'ERROR' else {},
                    },
                ]
            }
            if sync_mode:
                resp['items'][0].update({'transaction_status': status})
            return json_resp or resp

        return mock_post

    def make_bundle(*, acc_data=None):
        bundle = build_payment_bundle(
            PayPal, service=Service.TOLOKA, h2h=True, account=acc_data or {'remote_id': 'sag'}
        )
        assert bundle.status == states.FOR_DELIVERY
        return bundle

    time_gap = 0

    def process(bundle, *, mock_func, connector=None):
        nonlocal time_gap
        time_gap += 100
        connector_obj = PayPal.connector_dispatcher.get_connector()

        client_api =connector_obj.get_client(connector or connector_obj.settings_alias_default).Api

        # Запросы синхронизатора.
        monkeypatch.setattr(client_api, 'get', mock_func)

        # Запросы отправителя.
        monkeypatch.setattr(client_api, 'post', mock_func)

        with time_shift(time_gap):
            process_bundles(None)
        bundle.refresh_from_db()

    bundle = make_bundle()
    json_resp = {
        'error': {
            'name': 'VALIDATION_ERROR', 'message': 'Invalid request - see details', 'debug_id': '82a47248d6221',
            'information_link': 'https://developer.paypal.com/docs/api/payments.payouts-batch/#errors',
            'details': [
                {
                    'field': 'items[0].receiver', 'location': 'body',
                    'issue': 'Receiver is invalid or does not match with type'
                }
            ], 'links': []
        }
    }
    process(bundle, mock_func=get_post_mock(json_resp=json_resp))

    assert bundle.status == states.FOR_DELIVERY
    assert len(bundle.remote_responses) == 1
    assert 'VALIDATION_ERROR' in bundle.processing_notes

    bundle.processing_retries = 8
    bundle.save()

    process(bundle, mock_func=get_post_mock(json_resp=json_resp))
    assert bundle.status == states.ERROR
    assert len(bundle.remote_responses) == 2

    payment = bundle.payments[0]
    assert payment.status == states.ERROR
    assert payment.error_code is not None
    assert 'Invalid request' in payment.error_message

    bundle = make_bundle()

    # Проверка подхвата исключения.
    process(bundle, mock_func=mock_exception)

    assert bundle.status == states.FOR_DELIVERY
    assert len(bundle.remote_responses) == 1
    assert 'зафиксировали' in bundle.processing_notes
    assert not bundle.sent
    payment = bundle.payments[0]
    assert payment.status == states.BUNDLED
    assert payment.error_code is None
    assert payment.error_message == ''

    # Проверка обработки ответа, указывающего на ошибку в платеже
    process(bundle, mock_func=get_post_mock(status='ERROR'))

    assert bundle.status == states.ERROR
    assert bundle.processing_notes == ''
    assert len(bundle.remote_responses) == 2
    assert bundle.sent
    payment = bundle.payments[0]
    assert payment.status == states.ERROR
    assert payment.error_code == 'ERROR'
    assert payment.error_message == 'myfail'
    assert payment.processing_notes == '[ERROR] myfail'

    def check_success(bundle, connector=None, resp_count=2):
        process(bundle, mock_func=get_post_mock(status='SUCCESS'), connector=connector)

        assert bundle.status == states.EXPORTED_H2H
        assert len(bundle.remote_responses) == resp_count
        assert bundle.processing_notes == ''
        payment = bundle.payments[0]
        assert payment.status == states.EXPORTED_H2H
        assert payment.processing_notes == '[SUCCESS]'
        assert payment.error_code == 'SUCCESS'
        assert payment.error_message == ''

    # Проверка c выключенным sync_mode.
    bundle = make_bundle()
    process(bundle, mock_func=get_post_mock(status='PENDING', sync_mode=False))

    assert bundle.remote_id == 'testid'
    assert bundle.status == states.FOR_DELIVERY
    assert 'внешней системой' in bundle.processing_notes
    assert bundle.sent
    assert len(bundle.remote_responses) == 1
    payment = bundle.payments[0]
    assert payment.status == states.PROCESSING
    assert not payment.error_code
    assert payment.error_message == ''
    assert payment.processing_notes == '[]'

    check_success(bundle)

    # Проверка синхронизации.
    bundle = make_bundle()
    process(bundle, mock_func=get_post_mock(status='PENDING'))

    assert bundle.remote_id == 'testid'
    assert bundle.status == states.FOR_DELIVERY
    assert 'внешней системой' in bundle.processing_notes
    assert bundle.sent
    assert len(bundle.remote_responses) == 1
    payment = bundle.payments[0]
    assert payment.status == states.PROCESSING
    assert payment.error_code == 'PENDING'
    assert payment.error_message == ''
    assert payment.processing_notes == '[PENDING]'

    check_success(bundle)

    # проверяем отправку под новым remote_id
    bundle = make_bundle(acc_data={'number': '111222333', 'remote_id': 'sag_toloka'})
    check_success(bundle, connector='sag_toloka', resp_count=1)
    assert bundle.sent

    # проверяем отправку под неизвестным remote_id
    bundle = make_bundle(acc_data={'number': '111222336', 'remote_id': 'sag_test'})
    process(bundle, mock_func=get_post_mock(status='SUCCESS'))
    assert bundle.status == states.FOR_DELIVERY
    assert bundle.processing_notes == 'Unknown connection alias provided for PayPalConnnector: sag_test'
    assert not bundle.sent

    # Проверка ранней остановки обработки и хранения статуса.
    bundle = make_bundle()
    process(bundle, mock_func=get_post_mock(status='UNCLAIMED'))

    assert bundle.status == states.FOR_DELIVERY
    assert bundle.processing_notes == 'Обрабатывается внешней системой.'
    assert bundle.sent
    assert len(bundle.remote_responses) == 1
    payment = bundle.payments[0]
    assert payment.status == states.OTHER
    assert payment.processing_notes == '[UNCLAIMED]'
    assert payment.error_code == 'UNCLAIMED'
    assert payment.error_message == ''

    # Проверка ранней остановки обработки и хранения статуса.
    bundle = make_bundle()
    process(bundle, mock_func=get_post_mock(status='ONHOLD'))

    assert bundle.status == states.FOR_DELIVERY
    assert 'брабатывается' in bundle.processing_notes
    assert bundle.sent
    assert len(bundle.remote_responses) == 1
    payment = bundle.payments[0]
    assert payment.status == states.PROCESSING
    assert payment.processing_notes == '[ONHOLD]'
    assert payment.error_code == 'ONHOLD'
    assert payment.error_message == ''


@pytest.mark.parametrize(
    'status_code, error, bunlde_status, payment_status, on_sync',
    [
        (422, 'INSUFFICIENT_FUNDS', states.ERROR, states.ERROR, False),
        (429, 'RATE_LIMIT_REACHED', states.FOR_DELIVERY, states.BUNDLED, False),
        (429, 'RATE_LIMIT_REACHED', states.FOR_DELIVERY, states.BUNDLED, True),
    ]
)
def test_check_client_error(
    status_code, error, bunlde_status, payment_status, on_sync, build_payment_bundle, monkeypatch, time_shift):

    def mock_func(new_status, new_content):

        def mock_it(self, url, method, **kwargs):

            if '/token' in url:
                # Имитация авторизации.
                response = {
                    'scope': '',
                    'token_type': 'Bearer',
                    'access_token': 'Q',
                    'nonce': 'E',
                    'app_id': 'APP-80W284485P519543T',
                    'expires_in': 32400
                }

            else:
                response = namedtuple('FakeResponse', ['status_code'])(new_status)
                response = handle_response_old(self, response, new_content)

            return response

        return mock_it

    monkeypatch.setattr(
        'paypalrestsdk.Api.http_call',
        mock_func(status_code, '{"name":"%s",' % error +
                       '"message":"Sender does not have sufficient funds. Please add funds and retry.",'
                       '"debug_id":"364ae11a71d08",'
                       '"information_link":"https://developer.paypal.com/docs/api/payments.payouts-batch/#errors",'
                       '"links":[]}'))

    # Запомним, где был handle_response, он нам понадобится в mock_func
    handle_response_old = monkeypatch._setattr[-1][0].handle_response

    bundle = build_payment_bundle(PayPal, service=Service.TOLOKA, h2h=True, account={'remote_id': 'sag'})
    if on_sync:
        bundle.remote_id = 'test'
        bundle.save()

    with time_shift(100):
        process_bundles(None)

    bundle.refresh_from_db()
    assert bundle.status == bunlde_status

    payment = bundle.payments[0]

    assert payment.processing_notes == (
        f'[{error}] Sender does not have sufficient funds. Please add funds and retry.')
    assert payment.error_message == 'Sender does not have sufficient funds. Please add funds and retry.'
    assert payment.status == payment_status


def test_retry_delay():
    associate = PayPal

    get_delay = associate.get_requests_retry_delay

    assert get_delay(2) == 40
    assert get_delay(9) == 180
    assert get_delay(10) == 3600
    assert get_delay(50) == 3600
    assert get_delay(51) == 21600
    assert get_delay(99) == 21600
    assert get_delay(101) == 43200


def test_payment_creator(get_source_payment, get_payment_bundle):
    payment = get_source_payment(service=True)
    bundle = get_payment_bundle([payment])

    creator = PayPal.payment_dispatcher.get_creator(bundle)
    compiled = creator.create_bundle()

    expected_id = creator.generate_sender_batch_id(bundle.number)

    assert len(expected_id) == 30
    assert compiled['sender_batch_header']['sender_batch_id'] == expected_id
    assert str(bundle.number) in expected_id


@pytest.fixture
def bootstrap_db(get_assoc_acc_curr, init_user):

    def bootstrap_db_(account_num=None, currency=None):
        init_user(robot=True)

        account_num = account_num or 'TP5CKP49PXXTS'
        currency = currency or [Currency.RUB]

        if not isinstance(currency, list):
            currency = list(currency)

        len_currency = len(currency)

        for idx, currency_item in enumerate(currency, 1):
            acc_num = account_num

            if len_currency > 1 and idx > 1:
                acc_num = f'{acc_num}/{Currency.by_num[currency_item]}'

            get_assoc_acc_curr(PayPal, account={'number': acc_num, 'currency_code': currency_item})

    return bootstrap_db_


def test_mam_statement(read_fixture, sftp_client, init_user, get_assoc_acc_curr, sitemessages, mailoutbox):
    init_user(robot=True)
    now_date = DateUtils.yesterday().strftime('%Y%m%d')

    account_list = ['37ZY3NUB6BYAN', 'SMU8H7A33MLCE', 'VD22P5AQJLY2Y']
    for acc_num in account_list:
        get_assoc_acc_curr(PayPal.id, account={'number': acc_num, 'currency_code': Currency.USD})

    get_assoc_acc_curr(PayPal.id, account={'number': '37ZY3NUB6BYAN/CHF', 'currency_code': Currency.CHF})
    account_list.append('37ZY3NUB6BYAN/CHF')

    sftp_client(
        files_contents={f'STL-{now_date}.R.01.01.009.CSV': read_fixture('paypal_mam_report.csv', decode='utf-8-sig')}
    )
    statements = PayPal.automate_statements(alias='ch2', **{'mam_settings': True})
    result = statements[0].process()
    assert len(result) == 4
    for statements in result:
        assert statements[0].is_valid
        account_list.remove(statements[0].account.number)
    assert not account_list

    assert len(mailoutbox) == 3
    assert 'PayPal STL 37ZY3NUB6BYAN [CH2] 2020-01-23' in mailoutbox[0].subject
    assert len(json.loads(mailoutbox[0].attachments[0][1])['transactions']) == 1

    assert 'PayPal STL SMU8H7A33MLCE [CH2] 2020-01-23' in mailoutbox[1].subject
    assert len(json.loads(mailoutbox[1].attachments[0][1])['transactions']) == 1

    assert 'PayPal STL VD22P5AQJLY2Y [CH2] 2020-01-23' in mailoutbox[2].subject
    assert not json.loads(mailoutbox[2].attachments[0][1])['transactions']

    messages = sitemessages()
    assert len(messages) == 1
    assert 'Paypal - неизвестные платежи' in messages[0].context['subject']


def test_send_disputes(sftp_client, mailoutbox):
    now_date = DateUtils.yesterday().strftime('%Y%m%d')
    common_file = f'DDR-{now_date}.01.008.CSV'
    mam_file = f'DDR-{now_date}.R.01.01.008.CSV'

    sftp_client(
        files_contents={common_file: 'test', mam_file: 'test2'}, check_filters=True
    )

    send_disputes()

    assert len(mailoutbox) == 6

    for letter in mailoutbox:
        assert letter.attachments[0][0] == common_file
        if 'CH2' in letter.subject:
            assert letter.attachments[0][1] == 'test2'
        else:
            assert letter.attachments[0][1] == 'test'


def test_downloader(read_fixture, sftp_client, init_user):
    init_user(robot=True)

    sftp_client(files_contents={'one': read_fixture('STL-20161018.01.009.CSV', decode='utf-8-sig')})

    statements = PayPal.statement_downloader.process()

    parsed = PayPalStatementParser(statements[0]).parse()

    assert len(parsed) == 1

    parsed = list(parsed.values())[0]

    assert len(statements) == 1
    assert parsed['account_id'] == 'TP5CKP49PXXTS'
    assert str(parsed['period_end']) == '2016-10-18 23:59:59+04:00'

    assert statements[0].status == states.STATEMENT_FOR_PROCESSING


@pytest.fixture
def process_and_get_stetement(read_fixture, sftp_client, bootstrap_db, init_user):

    def process_and_get_stetement_(filename, account_num, currency=None, country=None):
        sftp_client(files_contents={'one': read_fixture(filename, decode='utf-8-sig')})

        bootstrap_db(account_num, currency)

        statements = PayPal.statement_downloader.process(alias=country)
        stat_file = statements[0]

        parser = PayPal.statement_dispatcher.get_parser(stat_file)

        results = parser.register_payments(parser.parse())

        return results, statements

    return process_and_get_stetement_


def test_fee_change(parse_statement_fixture):
    register, payments = parse_statement_fixture('ppal_fee_changed.txt', PayPal, '37ZY3NUB6BYAN', 'USD')[0]

    assert register.is_valid
    assert len(payments) == 2


def test_masspay_invert(parse_statement_fixture):
    register, payments = parse_statement_fixture('ppal_masspay_invert.txt', PayPal, 'YRJQEWS6KJDR2', 'USD')[0]

    assert register.is_valid
    assert len(payments) == 3


def test_complex(process_and_get_stetement):

    results, statements = process_and_get_stetement('STL-20161219.01.009.CSV', 'YARUPAYPALLINKEDTORFB')

    (register, payments) = results[0]

    payments.sort(key=lambda p: p.summ)

    assert register.is_valid
    assert len(payments) == 7
    assert payments[1].get_info_inn() == '7750005796'
    assert payments[1].get_info_purpose() == 'PAYPAL_TRANSFER по договору 00056384.0 от 18.04.2016'
    assert payments[1].get_info_account() == 'YARUPAYPALLINKEDTORFB'

    parsed = PayPalStatementParser(statements[0]).parse()
    assert len(parsed) == 1

    transformed = PayPalStatementSender.transform_data(list(parsed.values())[0])

    assert len(transformed['transactions']) == 5

    assert set(
        trans['event_code'] for trans in transformed['transactions']
    ) == {'T0006', 'T0007', 'T1201', 'T1106', 'T1107'}


def test_multi_currency1(process_and_get_stetement):

    results, statements = process_and_get_stetement(
        'STL-20170331.01.009.CSV', 'EVERVCDGBV3A2',
        currency=[Currency.USD, Currency.CHF, Currency.EUR], country=ALIAS_CH2)

    results.sort(key=lambda x: x[0].currency_id)

    assert len(results) == 3
    assert results[0][0].is_valid
    assert len(results[0][1]) == 5  # CHF
    assert results[1][0].is_valid
    assert len(results[1][1]) == 5  # USD
    assert results[2][0].is_valid
    assert len(results[2][1]) == 3  # EUR


def test_multi_currency2(process_and_get_stetement):

    results, statements = process_and_get_stetement(
        'STL-20160926.01.009.CSV', '37ZY3NUB6BYAN',
        currency=[Currency.USD, Currency.CHF], country=ALIAS_CH1)

    results.sort(key=lambda x: x[0].currency_id)

    assert len(results) == 2
    assert results[0][0].is_valid
    assert len(results[0][1]) == 1  # CHF
    assert results[1][0].is_valid
    assert len(results[1][1]) == 3  # USD


def test_payments_summing(process_and_get_stetement):

    results, statements = process_and_get_stetement(
        'STL-20170528.01.009.CSV', '37ZY3NUB6BYAN',
        currency=[Currency.USD, Currency.CHF], country=ALIAS_CH1)

    assert results[0][0].is_valid
    assert results[1][0].is_valid


def test_empty_statement(process_and_get_stetement):

    results, statements = process_and_get_stetement(
        'paypal_statement_empty.cvs', 'CK9ULDEKVPBPY',
        currency=[Currency.USD, Currency.RUB], country=ALIAS_RU_ZEN)

    register = results[0][0]
    assert register.is_valid

    registers = list(StatementRegister.objects.all())
    assert len(registers) == 1

    assert len(statements) == 1
    statement = statements[0]
    assert registers[0].account.number == 'CK9ULDEKVPBPY'
    assert statement.payment_cnt == 0
    assert str(registers[0].statement_date) == '2018-08-07'


def test_t0805(process_and_get_stetement):

    results, statements = process_and_get_stetement(
        'paypal_t0805.csv', 'TP5CKP49PXXTS',
        currency=[Currency.USD, Currency.RUB], country=ALIAS_RU_ZEN)

    register = results[0][0]
    assert register.is_valid
    assert register.payment_cnt == 4


def test_t1200(process_and_get_stetement):

    results, statements = process_and_get_stetement(
        'paypal_t1200.csv', 'YRJQEWS6KJDR2',
        currency=[Currency.USD], country=ALIAS_CH1)

    assert results[0][0].is_valid
    assert results[0][0].payment_cnt == 3


def test_t1501(process_and_get_stetement):

    results, statements = process_and_get_stetement(
        'paypal_t1501.csv', '37ZY3NUB6BYAN',
        currency=[Currency.USD, Currency.CHF], country=ALIAS_CH1)

    assert results[0][0].is_valid
    assert results[0][0].payment_cnt == 1


def test_t0003(process_and_get_stetement):

    results, statements = process_and_get_stetement(
        'paypal_t0003.csv', '37ZY3NUB6BYAN',
        currency=[Currency.USD, Currency.CHF], country=ALIAS_CH1)

    assert results[0][0].is_valid
    assert results[0][0].payment_cnt == 1


def test_payments_tcodes_variety(process_and_get_stetement):

    results, statements = process_and_get_stetement(
        'STL-20170818.01.009.CSV', '37ZY3NUB6BYAN',
        currency=[Currency.USD, Currency.CHF], country=ALIAS_CH1)

    assert results[0][0].is_valid
    assert results[1][0].is_valid


def test_parse(read_fixture, sftp_client, bootstrap_db):
    sftp_client(files_contents={'one': read_fixture('STL-20161018.01.009.CSV', decode='utf-8-sig')})

    bootstrap_db()

    statements = PayPal.statement_downloader.process()
    stat_file = statements[0]

    parser = PayPal.statement_dispatcher.get_parser(stat_file)

    parsed_statement = parser.parse()

    assert str(list(parsed_statement.values())[0]['period_end']) == '2016-10-18 23:59:59+04:00'

    results = parser.register_payments(parsed_statement)

    assert len(results) == 1

    register, payments = results[0]

    assert len(payments) == 4
    assert register.is_valid

    expected = {Decimal('288739.32'), Decimal('91369.2'), Decimal('263306.28'), Decimal('10365.40')}
    assert set(p.summ for p in payments) == expected


def test_statement_sender(read_fixture, sftp_client, monkeypatch, bootstrap_db):
    sftp_client(files_contents={'one': read_fixture('STL-20161109.01.009.CSV', decode='utf-8-sig')})

    bootstrap_db()

    statements = PayPal.statement_downloader.process()
    statement_dict = list(PayPalStatementParser(statements[0]).parse().values())[0]

    assert len(statement_dict['transactions']) == 80

    totals = statement_dict['totals']['RUB']
    assert totals['begin_avail_balance'] == 75576800
    assert totals['begin_avail_dc_mark'] == 'CR'
    assert totals['end_avail_balance'] == 54500166
    assert totals['end_avail_dc_mark'] == 'CR'
    assert totals['fee_ct'] == 5416
    assert totals['fee_dt'] == 882761
    assert totals['gross_ct'] == 29567097
    assert totals['gross_dt'] == 49766386
    assert totals['record_count'] == 80

    filtered = PayPalStatementSender.transform_data(statement_dict)

    assert len(filtered['transactions']) == 79

    totals = filtered['totals']['RUB']
    assert totals['begin_avail_balance'] == 75576800
    assert totals['begin_avail_dc_mark'] == 'CR'
    assert totals['end_avail_balance'] == 104065952
    assert totals['end_avail_dc_mark'] == 'CR'
    assert totals['fee_ct'] == 5416
    assert totals['fee_dt'] == 882761
    assert totals['gross_ct'] == 29567097
    assert totals['gross_dt'] == 200600
    assert totals['record_count'] == 79

    emails = []

    def send(self, data):
        attachment_contents = self.context['attachments'][1]
        emails.append((self.context['subject'], attachment_contents))

    monkeypatch.setattr('bcl.toolbox.notifiers.SmtpNotifier.send', send)
    PayPalStatementSender(statements, 'ru').run()

    (subject, attachment), = emails
    assert subject == 'PayPal STL TP5CKP49PXXTS [RU] 2016-11-09'
    assert '"account_id": "TP5CKP49PXXTS"' in attachment


class TestPaymentCreator:

    def test_bundle_contents(self, mocker, get_payment_bundle, get_source_payment):
        bundle = get_payment_bundle([get_source_payment()])

        # Здесь эмулируется работа фонового задания.
        # При которой тип внешней системы не инстанциируется.
        creator = PayPal.payment_dispatcher.get_creator(bundle)

        payment_params = bytes({})
        creator.create = mocker.MagicMock(return_value=payment_params)
        creator.create_bundle()

        assert creator.bundle_file.zip_raw == bundle.file.zip_raw == payment_params


def test_paypa_configurator_check_timeout_passing(mocker):
    """PayPal не пробрасывает таймаут в библиотеку requests
    из-за этого запрос может висеть очень долго (пока его не прибьет worker)

    чтобы избежать зависания, патчится метод paypalrestsdk.Api.http_call
    """
    import requests

    # get configured paypal api
    alias = PayPalConnnector.settings_alias_default
    api = PayPal.connector_dispatcher.get_connector().client_cls.api_dict[f'{PayPalConnnector.party_alias}_{alias}']

    mocker.spy(requests, 'request')

    try:
        api.http_call('http://127.0.0.1', 'GET')

    except Exception:
        # suppress ConnectionError
        pass

    # check that timeout passed to requests.request
    assert 'timeout' in requests.request.call_args[1]
    assert requests.request.call_args[1]['timeout'] == 60


def test_paypal_equal_request_id_generation():
    payout = PayPal.connector_dispatcher.get_connector().get_payout_object({})

    req_id_1 = payout.generate_request_id()
    req_id_2 = payout.generate_request_id()

    assert req_id_1 == req_id_2


def test_payment_without_error_message(mocker):
    payout = {
        "items": [
            {
                "links": [
                    {
                        "href": "https://api.paypal.com/v1/payments/payouts-item/SMR4DNQKBJ6FA",
                        "method": "GET",
                        "rel": "item",
                        "encType": "application/json"
                    }
                ],
                "payout_item_id": "SMR4DNQKBJ6FA",
                "payout_batch_id": "UY44X5CEGMTZ8",
                "payout_item_fee": {
                    "currency": "USD",
                    "value": "0.11"
                },
                "transaction_status": "FAILED",
                "payout_item": {
                    "note": "Thank you.",
                    "amount": {
                        "currency": "USD",
                        "value": "5.58"
                    },
                    "sender_item_id": "1173451",
                    "recipient_type": "EMAIL",
                    "receiver": "kotcan@mail.ru"
                }
            }
        ],
        "batch_header": {
            "time_completed": "2018-03-06T20:39:24Z",
            "batch_status": "DENIED",
            "payout_batch_id": "UY44X5CEGMTZ8",
            "time_created": "2018-03-06T20:38:13Z",
            "amount": {
                "currency": "USD",
                "value": "5.58"
            },
            "sender_batch_header": {
                "email_subject": "You have a payment",
                "sender_batch_id": "1221458-9ebbf3632469b8ace14de4"
            },
            "fees": {
                "currency": "USD",
                "value": "0.11"
            }
        },
        "links": [
            {
                "href": "https://api.paypal.com/v1/payments/payouts/UY44X5CEGMTZ8?page_size=1000&page=1",
                "method": "GET",
                "rel": "self",
                "encType": "application/json"
            }
        ],
        "bcl_realm": "None",
        "bcl_time": "2018-03-30 14:11:40.073508"
    }
    to_dict = mocker.patch.object(XmlUtils, 'to_dict')
    to_dict.return_value = payout

    system_answer, status, status_paysys, status_hint = StatusBase().get_status_info(XmlUtils())
    assert status_hint == 'Processing for the payout item failed.'
    assert status_paysys == 'FAILED'
    assert status == 2
    payout['items'][0]['transaction_status'] = 'TEST'
    system_answer, status, status_paysys, status_hint = StatusBase().get_status_info(XmlUtils())
    assert status_hint == ''
    assert status_paysys == 'TEST'
    assert status == 2


def test_separated_service_summ(
        sftp_client, read_fixture, get_source_payment, get_assoc_acc_curr, bootstrap_db, init_user):

    get_assoc_acc_curr(PayPal.id, account={'number': '37ZY3NUB6BYAN/CHF', 'currency_code': Currency.CHF})
    get_assoc_acc_curr(PayPal.id, account={'number': '37ZY3NUB6BYAN', 'currency_code': Currency.USD})

    get_source_payment(
        service=Service.TOLOKA,
        attrs=dict(number='620793'),
        associate=PayPal
    )

    get_source_payment(
        service=Service.ZEN,
        attrs=dict(number='620798'),
        associate=PayPal
    )

    sftp_client(files_contents={'one': read_fixture('STL-20170818.01.009.CSV', decode='utf-8-sig')})

    bootstrap_db()

    statements = PayPal.statement_downloader.process()
    stat_file = statements[0]

    parser = PayPal.statement_dispatcher.get_parser(stat_file)
    parsed_statement = parser.parse()

    assert str(list(parsed_statement.values())[0]['period_end']) == '2017-08-18 23:59:59+02:00'

    results = parser.register_payments(parsed_statement)

    payments = {register.account.number: payments for register, payments in results}
    payments = payments['37ZY3NUB6BYAN']

    assert len(payments) == 4

    payments = sorted(payments, key=lambda payment: payment.summ)

    assert payments[0].summ == Decimal('0.8')
    assert payments[0].get_info_purpose() == 'PAYPAL_PAYMENT Яндекс.Толока по договору 00056384.0 от 18.04.2016'

    assert payments[1].summ == Decimal('1.23')
    assert payments[1].get_info_purpose() == 'PAYPAL_PAYMENT Яндекс.Дзен по договору 00056384.0 от 18.04.2016'

    assert payments[2].summ == Decimal('24.82')
    assert payments[2].get_info_purpose() == 'PAYPAL_COMMISSION по договору 00056384.0 от 18.04.2016'

    assert payments[3].summ == Decimal('1243.72')
    assert payments[3].get_info_purpose() == 'PAYPAL_PAYMENT по договору 00056384.0 от 18.04.2016'


def test_new_code(
    sftp_client, read_fixture, get_source_payment, get_assoc_acc_curr, bootstrap_db, init_user, sitemessages):

    get_assoc_acc_curr(PayPal.id, account={'number': '37ZY3NUB6BYAN/CHF', 'currency_code': Currency.CHF})
    get_assoc_acc_curr(PayPal.id, account={'number': '37ZY3NUB6BYAN', 'currency_code': Currency.USD})

    sftp_client(files_contents={'one': read_fixture('paypal_new_code.csv', decode='utf-8-sig')})

    bootstrap_db()

    statements = PayPal.statement_downloader.process()
    stat_file = statements[0]

    parser = PayPal.statement_dispatcher.get_parser(stat_file)

    parser.register_payments(parser.parse())

    messages = sitemessages()
    assert len(messages) == 1
    message = messages[0]
    assert message.context['subject'] == 'Paypal - новый код транзакции'
    assert 'Счёт: 37ZY3NUB6BYAN' in message.context['stext_']
    assert "T9876 () - ['10K84985A98422442']" in message.context['stext_']


def test_transaction_without_item_id(get_payment_bundle, get_source_payment, monkeypatch):
    def make_request(*args, **kwargs):
        raise ConnectionError

    client_api = PayPal.connector_dispatcher.get_connector().get_client(PayPalConnnector.settings_alias_default).Api

    monkeypatch.setattr(client_api, 'post', make_request)
    monkeypatch.setattr(client_api, 'get', make_request)

    payment = get_source_payment(attrs=dict(number='1276191'), service=True)

    bundle = get_payment_bundle([payment], account={'remote_id': 'sag'})
    PayPalPaymentCreator(bundle).create_bundle()

    with pytest.raises(ConnectionError):
        PayPalPaymentSender(bundle).prepare_send_sync()

    def check_send_sentry(*args):
        assert 1

    monkeypatch.setattr('bcl.toolbox.notifiers.SentryNotifier.send', check_send_sentry)

    payment.refresh_from_db()


def test_set_status_when_read_timeout(monkeypatch, get_payment_bundle, get_source_payment, read_fixture):
    import json

    payout = json.loads(read_fixture('paypal_create_response_with_transaction_id.json').decode('utf-8'))
    err = json.loads(read_fixture('paypal_user_business_error.json').decode('utf-8'))

    first = True
    client = PayPal.connector_dispatcher.get_connector().get_client(PayPalConnnector.settings_alias_default)

    class MockedPayout(client.Payout):
        def create(self, sync_mode=False, **kwargs):
            nonlocal first
            if first:
                first = False
                self.merge(payout)
                return 'TA8FKBTC4YHEJ'
            self.error = err
            return False

        @classmethod
        def find(cls, resource_id, api=None, refresh_token=None):
            return cls(payout, resource_id, api=api)

    monkeypatch.setattr(client, 'Payout', MockedPayout)
    monkeypatch.setattr(client.Api, 'post', lambda *args, **kwargs: None)
    monkeypatch.setattr(client.Api, 'get', lambda *args, **kwargs: None)

    payment = get_source_payment(attrs=dict(
        number='1234567',
        t_acc='billingtest1-buyer@yandex.ru',
        service_id=Service.TOLOKA,
        summ='1',
        currency_id=Currency.USD
    ), service=True, associate=PayPal)
    bundle = get_payment_bundle([payment])
    PayPalPaymentCreator(bundle).create_bundle()
    bundle.schedule()

    # отправка первый раз
    bundle = PaymentsBundle.get_scheduled()  # type: PaymentsBundle
    bundle.associate.automate_payments(bundle)
    bundle.status = bundle.state_processing_ready  # считаем транзакцию незавершенной
    bundle.remote_id = ''  # remote_id не получен, потому-что ReadTimeout
    bundle.save()
    bundle.refresh_from_db()

    # получение USER_BUSINESS_ERROR
    bundle.associate.automate_payments(bundle)
    bundle.refresh_from_db()

    assert bundle.status != 2
    assert bundle.status_title != 'Ошибка'
    assert bundle.remote_id != None


def test_non_service_payments_in_statement_exists(
        get_source_payment, read_fixture, sftp_client, monkeypatch, bootstrap_db, get_assoc_acc_curr):
    sftp_client(files_contents={'one': read_fixture('STL-20161109.01.009_non-service.CSV', decode='utf-8-sig')})

    bootstrap_db()

    get_source_payment(attrs=dict(
        number='1234567',
        t_acc='billingtest1-buyer@yandex.ru',
        service_id=Service.TOLOKA,
        summ='1',
        currency_id=Currency.USD,
        status=states.EXPORTED_H2H,
    ), service=True, associate=PayPal)

    get_assoc_acc_curr(PayPal.id, account={'number': 'TP5CKP49PXXTS', 'currency_code': Currency.USD})

    email = {}

    def send(self, data):
        email['subject'] = self.context['subject']
        email['data'] = data

    monkeypatch.setattr('bcl.toolbox.notifiers.SmtpNotifier.send', send)

    statements = PayPal.statement_downloader.process()
    parser = PayPal.statement_dispatcher.get_parser(statements[0])
    parser.process()

    assert 'Paypal - неизвестные платежи' == email['subject']
    assert 'Дата выписки 2016-11-09' in email['data']
    assert 'Счёт: TP5CKP49PXXTS' in email['data']
    assert '90W85897B50871829' not in email['data']
    assert all([tr in email['data'] for tr in ('7CU24864DP718944G', '5TC74390H33196826', '9HH785586C5480441')])

from datetime import datetime, timedelta
from typing import *

import pytest
from django.conf import settings
from freezegun import freeze_time
from lxml import etree

from bcl.banks.party_yoomoney import YooMoneyPaymentCreator, YooMoneyPaymentSender, YooMoneyPaymentSynchronizer
from bcl.banks.party_yoomoney.common import STATUS_REJECTED, STATUS_ERROR, \
    ERROR_ACCOUNT_BLOCKED, STATUS_PENDING, STATUS_SUCCESS, ERROR_TECH_PROBLEM, ERRORS
from bcl.banks.party_yoomoney.common import Signer, SignerError
from bcl.banks.registry import YooMoney
from bcl.core.models import states, Service, Currency
from bcl.core.tasks import process_bundles
from bcl.exceptions import DoesNotExist, ScheduledException, PaymentSystemException

if TYPE_CHECKING:
    from bcl.banks.base import Associate


@pytest.fixture
def create_bundle_and_get_status(build_payment_bundle, response_mock):

    def create_bundle_and_get_status_(yoomoney_status, bundle=None, error_code=None):

        if not bundle:
            bundle = build_payment_bundle(YooMoney, service=True, h2h=True)
            assert bundle.status == states.FOR_DELIVERY

        error = f'error="{error_code}"' if error_code else ''

        sync = YooMoneyPaymentSynchronizer(bundle)

        with response_mock(
            'POST https://bo-demo02.yamoney.ru:9094/webservice/deposition/api/makeDeposition -> 200:'
            f'<makeDepositionResponse clientOrderId="12345" status="{yoomoney_status}" '
            f'processedDT="2011-07-01T20:38:01.000Z" balance="1000.00" {error}/>'

        ) as http_mock:

            sync.run()

            assert b'makeDeposition' in http_mock.calls[0].request.body

        return bundle

    return create_bundle_and_get_status_


@pytest.mark.parametrize('yoomoney_status', [STATUS_ERROR, STATUS_REJECTED])
def test_synchronizer(yoomoney_status, create_bundle_and_get_status, mock_yoomoney_request_processor):

    bundle = create_bundle_and_get_status(yoomoney_status)

    assert bundle.status == states.ERROR


def test_get_bundle_contents(build_payment_bundle):
    associate = YooMoney
    bundle = build_payment_bundle(associate, service=Service.TOLOKA, h2h=True)
    contents = associate.payment_dispatcher.get_creator(bundle).get_bundle_contents()
    assert isinstance(contents, bytes)


def test_retry_delay():
    delay = YooMoney.get_requests_retry_delay(0)
    assert delay == 60
    delay = YooMoney.get_requests_retry_delay(2)
    assert delay == 300
    delay = YooMoney.get_requests_retry_delay(99)
    assert delay == 1800


def test_pending_payment(create_bundle_and_get_status, mock_yoomoney_request_processor):
    bundle = None

    for _ in range(3):
        bundle = create_bundle_and_get_status(STATUS_PENDING, bundle)
        assert bundle.status == states.PROCESSING

    with pytest.raises(ScheduledException):
        create_bundle_and_get_status(STATUS_PENDING, bundle, '30')

    bundle.refresh_from_db()
    assert bundle.status == states.PROCESSING

    create_bundle_and_get_status(STATUS_ERROR, bundle)
    assert bundle.status == states.ERROR


@pytest.fixture
def yoomoney_payment_pair(mock_yoomoney_request_processor, get_payment_bundle, get_source_payment):
    payment = get_source_payment(service=True)
    payments_bundle = get_payment_bundle([payment])
    creator = YooMoney.payment_dispatcher.get_creator(payments_bundle)
    sender = YooMoney.payment_sender(payments_bundle)

    sender.request_processor = mock_yoomoney_request_processor

    yield creator, sender


def test_prepare_send_sync(build_payment_bundle, mock_yoomoney_request_processor, time_shift, response_mock):

    def get_post_mock(*, status, api_method='testDeposition'):

        def mock_post(http_mock):
            error = ERROR_ACCOUNT_BLOCKED if status == STATUS_ERROR else ''

            http_mock.add(
                'POST',
                f'https://bo-demo02.yamoney.ru:9094/webservice/deposition/api/{api_method}',
                body=f'''<?xml version="1.0" encoding="UTF-8"?>
                    <testDepositionResponse clientOrderId="12345" status="{status}"
                        processedDT="2011-07-01T20:38:01.000Z" error="{error}"/>'''
            )

        return mock_post

    def make_bundle(curr_id=Currency.RUB):
        bundle = build_payment_bundle(YooMoney, service=True, h2h=True, payment_dicts=[{'currency_id': curr_id}])
        bundle.schedule()
        bundle.refresh_from_db()
        assert bundle.status == states.FOR_DELIVERY
        assert bundle.payments[0].status == states.BUNDLED
        return bundle

    time_gap = 0

    def process(bundle, *, mock_func):
        nonlocal time_gap
        time_gap += 600

        with response_mock('') as http_mock:
            mock_func(http_mock)

            with time_shift(time_gap):
                process_bundles(None)

        bundle.refresh_from_db()

    bundle = make_bundle()

    # Проверка остановки в случаи ошибки в ответе на ensure.
    process(bundle, mock_func=get_post_mock(status=STATUS_ERROR))

    assert 'currency="643"' in str(bundle.file.zip_raw)
    assert bundle.status == states.ERROR
    assert 'завершилась неудачей' in bundle.processing_notes
    assert not bundle.sent
    assert len(bundle.remote_responses) == 1
    payment = bundle.payments[0]
    assert payment.status == states.ERROR
    assert payment.error_code == ERROR_ACCOUNT_BLOCKED
    assert 'Счет в Системе заблокирован' in payment.error_message
    assert 'Счет в Системе заблокирован' in payment.processing_notes

    # Проверка синхронизации.
    bundle = make_bundle(curr_id=Currency.USD)
    process(bundle, mock_func=get_post_mock(status=STATUS_PENDING))

    assert f'currency="{Currency.USD}"' in str(bundle.file.zip_raw)
    assert bundle.remote_id == '12345'
    assert bundle.status == states.FOR_DELIVERY
    assert 'Connection refused by Responses' in bundle.processing_notes
    assert not bundle.sent
    assert len(bundle.remote_responses) == 1
    payment = bundle.payments[0]
    assert payment.status == states.BUNDLED
    assert payment.processing_notes == '[1]'
    assert payment.error_message == ''

    process(bundle, mock_func=get_post_mock(status=STATUS_SUCCESS, api_method='makeDeposition'))

    assert bundle.status == states.EXPORTED_H2H
    assert bundle.processing_notes == ''
    assert bundle.sent
    assert len(bundle.remote_responses) == 2
    payment = bundle.payments[0]
    assert payment.status == states.EXPORTED_H2H
    assert payment.processing_notes == '[0]'
    assert payment.error_message == ''


class TestSigner:

    @pytest.fixture()
    def signer(self, fixturesdir):
        yield Signer(
            fixturesdir.path('yoomoney/ym-client.cer'),
            fixturesdir.path('yoomoney/ym-client.key')
        )

    @pytest.mark.skipif(condition=settings.ARCADIA_RUN, reason='Сертификат не может быть открыт из бинарника')
    def test_encrypt(self, signer):
        assert signer.encrypt(b'hello world')

    def test_encrypt_without_key(self):
        signer = Signer(cert_path='foo')

        with pytest.raises(SignerError):
            assert signer.encrypt(b'hello world')

    def test_encrypt_without_cert(self):
        signer = Signer(cert_path='', key_path='foo')

        with pytest.raises(SignerError):
            assert signer.encrypt(b'hello world')

    @pytest.mark.skipif(condition=settings.ARCADIA_RUN, reason='Сертификат не может быть открыт из бинарника')
    def test_decrypt_wrong_pkcs(self, signer):
        with pytest.raises(SignerError):
            signer.decrypt(b'hello')

    def test_decrypt_without_cert(self):
        signer = Signer(cert_path='', key_path='foo')

        with pytest.raises(SignerError):
            signer.decrypt(b'hello world')


def test_phone_number(build_payment_bundle):

    bundle = build_payment_bundle(
        YooMoney,
        payment_dicts=[{
            'params': {'phone_op': '123', 'phone_num': '4567890'}
        }],
        service=True)

    assert '><paymentParams>' in  bundle.tst_compiled
    assert '<PROPERTY1>123</PROPERTY1><PROPERTY2>4567890</PROPERTY2>' in  bundle.tst_compiled


def test_yoomoney_process_request(mock_yoomoney_request_processor, response_mock):
    date = datetime.now().strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3]

    balance_data = f"""<?xml version="1.0" encoding="UTF-8"?>
        <balanceRequest agentId="200566" clientOrderId="12345" requestDT="{date}"/>"""

    with response_mock(
        'POST https://bo-demo02.yamoney.ru:9094/webservice/deposition/api/balance -> 200 :'
        '<balanceResponse clientOrderId="12345" status="3" error="53" processedDT="2011-07-01T20:38:01.000Z" '
        'balance="1000.00" />'
    ):
        data = mock_yoomoney_request_processor.process(balance_data, 'balance')

    assert data['status'] == '3'  # Отвергнут
    assert data['error'] == '53'  # Запрос подписан неизвестным Яндекс.Деньгам сертификатом


class TestYooMoneyPaymentCreator:

    def test_is_valid_wrong_status_code(self, yoomoney_payment_pair, mocker):
        creator, sender = yoomoney_payment_pair

        mocker.patch.object(creator, 'create')
        process_request = mocker.patch.object(sender.request_processor, 'process')
        process_request.return_value = {'status': '666'}

        # act
        with pytest.raises(PaymentSystemException):
            sender.ensure(sender.prepare_contents())

    def test_is_valid_correct_status_codes(self, yoomoney_payment_pair, mocker):
        creator, sender = yoomoney_payment_pair

        # arrange
        import xml.etree.ElementTree as ET

        mocker.patch.object(creator, 'create')

        yoomoney_answer = """<?xml version="1.0" encoding="UTF-8"?>
        <testDepositionResponse clientOrderId="12345"
                        status="{}"
                        processedDT="2011-07-01T20:38:01.000Z"
                        error="{}"/>
        """

        error_answer = ET.fromstring(yoomoney_answer.format(sender.yoomoney_status_error, '43')).attrib
        success_answer = ET.fromstring(yoomoney_answer.format(sender.yoomoney_status_success, '43')).attrib
        pending_answer = ET.fromstring(yoomoney_answer.format(sender.yoomoney_status_pending, '43')).attrib
        pending_with_error_answer = ET.fromstring(
            yoomoney_answer.format(sender.yoomoney_status_pending, ERROR_TECH_PROBLEM)).attrib
        rejected_answer = ET.fromstring(yoomoney_answer.format(sender.yoomoney_status_rejected, '43')).attrib

        process_request = mocker.patch.object(
            sender.request_processor,
            'process'
        )

        contents = sender.prepare_contents()

        process_request.return_value = error_answer
        validate_error = sender.ensure(contents)
        assert validate_error is False

        process_request.return_value = rejected_answer
        validate_reject = sender.ensure(contents)
        assert validate_reject is False

        process_request.return_value = success_answer
        validate_success = sender.ensure(contents)
        assert validate_success is True

        process_request.return_value = pending_answer
        validate_pending = sender.ensure(contents)
        assert validate_pending is True

        process_request.return_value = pending_with_error_answer
        with pytest.raises(ScheduledException) as e:
            sender.ensure(contents)
        assert 'Технические проблемы на стороне Яндекс.Денег' in e.value.msg

    error_states = [
        ('yoomoney_status_error', code)
        for code in ERRORS.keys()
        if code not in {ERROR_ACCOUNT_BLOCKED, ERROR_TECH_PROBLEM}]

    @pytest.mark.parametrize(
        'yoomoney_status, error_code',
        [
            ('yoomoney_status_error', ERROR_ACCOUNT_BLOCKED),
            ('yoomoney_status_rejected', ERROR_TECH_PROBLEM),
            ('yoomoney_status_rejected', ERROR_ACCOUNT_BLOCKED),
            ('yoomoney_status_success', None),
            ('yoomoney_status_pending', None),
        ] + error_states
    )
    def test_yoomoney_status_mapping(self, yoomoney_status, error_code, yoomoney_payment_pair, mocker):
        yoomoney_payment_creator, yoomoney_payment_sender = yoomoney_payment_pair
        error = f'error="{error_code}"' if error_code else ''

        if error_code is not None:
            error_code = str(error_code)

        mocker.patch.object(yoomoney_payment_creator, 'create')

        yoomoney_answer = """<?xml version="1.0" encoding="UTF-8"?>
                <makeDepositionResponse clientOrderId="12345"
                                status="{}"
                                processedDT="2011-07-01T20:38:01.000Z"
                                {}/>
                """

        payment_system_answer = dict(etree.fromstring(
            yoomoney_answer.format(getattr(yoomoney_payment_sender, yoomoney_status), error).encode('utf-8')).attrib)

        process_request = mocker.patch.object(
            yoomoney_payment_sender.request_processor,
            'process'
        )

        process_request.return_value = payment_system_answer

        def check_send_sentry(*args, **kwargs):
            assert 1

        mocker.patch('bcl.toolbox.notifiers.SentryNotifier.send', check_send_sentry)

        _, status_code, _ = yoomoney_payment_sender.handle_response(payment_system_answer, source=yoomoney_payment_sender.bundle)

        for payment in yoomoney_payment_sender.payments:
            assert payment.error_code == error_code or '0'
            assert payment.status == yoomoney_payment_sender.status_mapping[getattr(yoomoney_payment_sender, yoomoney_status)]

            if error_code:
                assert payment.error_message == ERRORS[error_code]


def test_callback_handler(mocker, monkeypatch, build_payment_bundle):

    # Пример запроса извне.
    _ = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<errorDepositionNotificationRequest '
        'clientOrderId="12345" '
        'requestDT="2011-07-01T20:38:00.000Z" '
        'dstAccount="410011234567" '
        'amount="10.00" '
        'currency="643" '
        'error="31"/>'
    )

    monkeypatch.setattr('bcl.banks.party_yoomoney.callback_handler.encryptor.encrypt', lambda data: data)

    bundle = build_payment_bundle(YooMoney, service=True, h2h=True)
    payment = bundle.payments[0]
    payment.t_acc = '410011234567'
    payment.status = states.PROCESSING
    payment.save()

    handler = YooMoney.callback_handler(request=None, realm=None)
    patched = mocker.patch.object(handler, 'get_request')
    patched.return_value = {
        'clientOrderId': str(payment.number),
        'error': '31',
        'dstAccount': payment.t_acc,
    }

    result, status = handler.run()
    assert bundle.status == states.FOR_DELIVERY
    assert b'status="0"' in result
    assert status == 200

    bundle.refresh_from_db()
    payment.refresh_from_db()

    assert payment.status == states.ERROR
    assert payment.processing_notes == '[31] Получатель перевода отклонил платеж.'
    assert payment.remote_responses[0]['error'] == '31'
    assert bundle.status == states.ERROR
    assert bundle.remote_responses[0]['error'] == '31'


def test_callback_handler_for_success_payment(mocker, monkeypatch, build_payment_bundle):

    """
    # Пример запроса извне.
    _ = (
        '<errorDepositionNotificationRequest '
        'clientOrderId="12345" '
        'requestDT="2011-07-01T20:38:00.000Z" '
        'dstAccount="410011234567" '
        'amount="10.00" '
        'currency="643" '
        'error="31"/>'
    )
    """

    monkeypatch.setattr('bcl.banks.party_yoomoney.callback_handler.encryptor.encrypt', lambda data: data)

    bundle = build_payment_bundle(YooMoney, service=True, h2h=True)
    payment = bundle.payments[0]
    payment.t_acc = '410011234567'
    payment.status = states.EXPORTED_H2H
    payment.save()

    handler = YooMoney.callback_handler(request=None, realm=None)
    patched = mocker.patch.object(handler, 'get_request')
    patched.return_value = {
        'clientOrderId': str(payment.number),
        'error': '31',
        'dstAccount': payment.t_acc,
    }

    with pytest.raises(DoesNotExist) as e:
        # Пришёл обратный вызов по уже экспортированному платежу.
        handler.run()

    assert ("'number': %s" % payment.number) in str(e.value)

    payment.refresh_from_db()
    assert payment.status == states.EXPORTED_H2H


def test_success_pay_without_error(
    mocker, monkeypatch, build_payment_bundle, get_source_payment, mock_yoomoney_request_processor,
):

    payments_bundle = build_payment_bundle(YooMoney, payment_dicts=[
        {'params': {'phone_op': 905, 'phone_num': 8182838}}
    ], account='fakeacc', service=True, h2h=True)

    yoomoney_payment_creator = YooMoneyPaymentCreator(payments_bundle)
    yoomoney_payment_sender = YooMoneyPaymentSender(payments_bundle)

    yoomoney_payment_sender.request_processor = mock_yoomoney_request_processor

    mocker.patch.object(yoomoney_payment_creator, 'create')
    yoomoney_answer = """<?xml version="1.0" encoding="UTF-8"?>
                    <makeDepositionResponse clientOrderId="12345"
                                    status="0"
                                    processedDT="2011-07-01T20:38:01.000Z"/>
                    """

    payment_system_answer = dict(etree.fromstring(yoomoney_answer.encode('utf-8')).attrib)

    process_request = mocker.patch.object(
        yoomoney_payment_sender.request_processor,
        'process'
    )

    process_request.return_value = payment_system_answer

    def check_send_sentry(*args, **kwargs):
        assert 1

    mocker.patch('bcl.toolbox.notifiers.SentryNotifier.send', check_send_sentry)

    _, status_code, _ = yoomoney_payment_sender.handle_response(
        payment_system_answer, source=yoomoney_payment_sender.bundle
    )
    payment = yoomoney_payment_sender.payments[0]
    payment.refresh_from_db()
    payment.status = states.PROCESSING

    sync = YooMoneyPaymentSynchronizer(payments_bundle)
    sync.request_processor = mock_yoomoney_request_processor
    process_request = mocker.patch.object(
        sync.request_processor,
        'process'
    )
    process_request.return_value = payment_system_answer
    with freeze_time(datetime.now() + timedelta(hours=2)):
        sync.run()
        payment.refresh_from_db()
        payment.status = states.PROCESSING

    with freeze_time(datetime.now() + timedelta(hours=3)):
        sync.run()
        payment.refresh_from_db()
        payment.status = states.EXPORTED_H2H


class TestYooMoneyBalanceGetter:

    @pytest.fixture()
    def balance_getter(
        self, get_assoc_acc_curr, mock_yoomoney_request_processor) -> Generator[YooMoney.balance_getter, None, None]:

        _, acc, _ = get_assoc_acc_curr(YooMoney)

        balance_getter = YooMoney.balance_getter(accounts=[acc])
        balance_getter._request_processor = mock_yoomoney_request_processor

        yield balance_getter

    def test_compose_request_document(self, balance_getter: YooMoney.balance_getter):

        acc = balance_getter.accounts[0]
        data = balance_getter.compose_request_document(acc).encode('utf-8')

        parsed = etree.fromstring(data)

        assert parsed.attrib['agentId'] == acc.number

    def test_process(self, balance_getter: YooMoney.balance_getter, response_mock):

        with response_mock(
            'POST https://bo-demo02.yamoney.ru:9094/webservice/deposition/api/balance -> 200 :'
            '<balanceResponse clientOrderId="12345" status="0" processedDT="2011-07-01T20:38:01.000Z" '
            'balance="1000.00" />'
        ):
            balance, _ = balance_getter.run(balance_getter.accounts[0])

        assert balance == '1000.00'

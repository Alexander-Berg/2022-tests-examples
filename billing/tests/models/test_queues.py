from datetime import datetime
from uuid import uuid4

from requests.exceptions import Timeout

from bcl.banks import registry
from bcl.banks.party_yoomoney.common import STATUS_SUCCESS
from bcl.core.models import Service, states, Payment, PaymentsBundle
from bcl.core.tasks import process_bundles


def test_queue_fail(get_assoc_acc_curr, monkeypatch, time_shift):

    monkeypatch.setattr('bcl.core.models.PaymentsBundle.processing_retries_max', 2)

    bank, account, curr = get_assoc_acc_curr(registry.YooMoney.id, account='1234')

    yoomoney = registry.YooMoney

    yoomoney.register_payment({
        'number_src': '34567890',
        'f_acc': account.number,
        't_acc': 'fake',
        'ground': 'some',
        'summ': '10.25',
        'currency_id': curr,
        'account_id': account.id,
    }, service_id=Service.TOLOKA)

    process_bundles(None)

    with time_shift(10 * 60):
        process_bundles(None)

    payments = Payment.objects.all()
    assert len(payments) == 1

    payment = payments[0]
    bundle = payment.bundle

    assert payment.status == states.ERROR
    assert bundle.is_processing_failed

    assert 'No such file or directory' in payment.processing_notes
    assert 'No such file or directory' in bundle.processing_notes
    assert bundle.processing_retries == 2


def test_payment_queue_processing(
    get_assoc_acc_curr, mocker, mock_yoomoney_request_processor, response_mock, monkeypatch
):

    bank, account, curr = get_assoc_acc_curr(registry.YooMoney, account='1234')

    def new_pay():
        PaymentsBundle.objects.all().delete()

        payment, _ = registry.YooMoney.register_payment({
            'number_src': str(uuid4()),
            'f_acc': account.number,
            't_acc': 'fake',
            'ground': 'some',
            'summ': '10.25',
            'currency_id': curr,
            'account_id': account.id,
        }, service_id=Service.TOLOKA)

        assert payment.status == states.NEW

        bundles = PaymentsBundle.objects.all()
        assert len(bundles) == 1
        bundle = bundles[0]
        assert bundle.is_processing_ready
        assert bundle.processing_retries == 0

        return payment, bundle

    def check_success(payment):
        mocker.patch('bcl.banks.party_yoomoney.payment_sender.YooMoneyPaymentSender.ensure', lambda *args, **kwargs: True)

        with response_mock(
            'POST https://bo-demo02.yamoney.ru:9094/webservice/deposition/api/makeDeposition -> 200 :'
            '<?xml version="1.0" encoding="UTF-8"?>'
            f'<makeDepositionResponse clientOrderId="12345" status="{STATUS_SUCCESS}" '
            'processedDT="2011-07-01T20:38:01.000Z" balance="1000.00"/>'
        ):
            process_bundles(None)

        payment.refresh_from_db()

        assert payment.status == states.EXPORTED_H2H

    def check_error(payment):

        def exception_probe(*args, **kwargs):
            raise Exception('allwrong')

        mocker.patch('bcl.banks.party_yoomoney.payment_sender.YooMoneyPaymentSender.ensure', exception_probe)

        process_bundles(None)

        payment.refresh_from_db()

    payment, bundle = new_pay()
    check_success(payment)

    # Далее проверка на обработку исключений.

    payment, bundle = new_pay()
    check_error(payment)

    bundle.refresh_from_db()
    assert bundle.processing_retries == 1
    bundle.processing_after_dt = datetime.utcnow()  # emulate timeout
    bundle.save()

    check_error(payment)

    bundle.refresh_from_db()
    assert bundle.processing_retries == 2
    bundle.processing_after_dt = datetime.utcnow()  # emulate timeout
    bundle.save()

    check_success(payment)

    # Проверим что в случаях обрыва связи будут произведены дополнительные
    # попытки запроса, даже если общее количество попыток исчерпано.
    def mock_send_request(*args, **kwargs):
        raise Timeout(response=None)

    monkeypatch.setattr(
        'bcl.banks.party_yoomoney.common.RequestProcessor._send_request', mock_send_request)

    bundle.refresh_from_db()
    bundle.status = bundle.state_processing_ready
    bundle.processing_notes = ''
    bundle.processing_retries = bundle.processing_retries_max + 1
    bundle.save()

    process_bundles(None)
    bundle.refresh_from_db()
    assert bundle.processing_notes == ''
    assert bundle.is_processing_ready

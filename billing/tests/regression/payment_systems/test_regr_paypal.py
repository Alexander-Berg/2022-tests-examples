import uuid

import pytest
import requests

PAYPAL_STATUSES_MAPPING = {
    'SUCCESS': 'exported_to_h2h',
    'DENIED': 'error',
    'FAILED': 'error',
    'RETURNED': 'error',
    'UNCLAIMED': 'other',
    'INSUFFICIENT_FUNDS': 'error',
    'ONHOLD': '',
    'BLOCKED': '',
    'CANCELLED': 'rejected',
    'VALIDATION_ERROR': 'error',
}

PAYMENTS_WITH_STATUSES = [
    ('paypalch@yandex-team.ru', 'PVC@yandex-test.ru', 'USD', 'SUCCESS'),
    ('paypalch@yandex-team.ru', 'PVC@yandex-test.ru', 'USD', 'INSUFFICIENT_FUNDS'),  # решила проверять в отдельном тесте
    ('valeriya.kostina@gmail.com', 'PVC@yandex-test.ru', 'USD', 'FAILED'),
    ('paypalch@yandex-team.ru', 'PVC@yandex-test/ru', 'USD', 'VALIDATION_ERROR'),
]

CANCEL_PAYMENT = [
    ('paypalch@yandex-team.ru', 'unrealistically-long-account-name-which-probably-wont-be-created@yandex-test.ru', 'USD', 'UNCLAIMED')
]


@pytest.mark.regression
@pytest.mark.parametrize(
    'f_acc, t_acc, currency, status',
    PAYMENTS_WITH_STATUSES + CANCEL_PAYMENT + [
        ('billingtest3@yandex.ru', 'PVC@yandex-test.ru', 'USD', 'SUCCESS'),
        ('billingtest3@yandex.ru', 'PVC@yandex-test.ru', 'USD', 'INSUFFICIENT_FUNDS'),
    ]
)
def test_send_and_check_payment(f_acc, t_acc, currency, status, bcl_send_payment, wait_processing_payment):
    transaction_id = str(uuid.uuid4()).upper()
    result = bcl_send_payment(
        transaction_id, f_acc=f_acc, f_bik='044525303', t_acc=t_acc, currency=currency,
        t_acc_type='email', summ='6000000.00' if status == 'INSUFFICIENT_FUNDS' else '2.01'
    )
    assert 'doc_number' in result.keys()
    assert result['payment_status'] == 'new'

    transaction_info = wait_processing_payment([transaction_id], source_oebs=False)[0]

    assert transaction_info["status"] == PAYPAL_STATUSES_MAPPING[status]
    if status != 'FAILED':
        assert status in transaction_info["payment_system_answer"]
    else:
        assert 'Счёт заблокирован.' in transaction_info["processing_notes"]


@pytest.mark.regression
@pytest.mark.parametrize('f_acc, t_acc, currency, status', CANCEL_PAYMENT)
def test_cancel_payment(
        f_acc, t_acc, currency, status, bcl_send_payment, bcl_cancel_payments, bcl_get_payments,
        wait_processing_payment):
    transaction_id = str(uuid.uuid4()).upper()
    bcl_send_payment(
        transaction_id, f_acc=f_acc, f_bik='044525303', t_acc=t_acc, currency=currency,
        t_acc_type='email'
    )
    transaction_info = wait_processing_payment([transaction_id], source_oebs=False)[0]
    assert transaction_info["status"] == PAYPAL_STATUSES_MAPPING[status]

    result = bcl_cancel_payments([transaction_id])
    assert result[transaction_id]['success'] is True

    payment_info = bcl_get_payments([transaction_id])[0]
    assert payment_info["status"] == 'rejected'


@pytest.mark.regression
@pytest.mark.parametrize('f_acc, t_acc, currency, status', PAYMENTS_WITH_STATUSES + CANCEL_PAYMENT)
def test_from_oebs_to_paypal(f_acc, t_acc, currency, status, bcl_set_pd, wait_processing_payment):
    transaction_id = str(uuid.uuid4()).upper()
    bcl_number = bcl_set_pd(
        transaction_id, f_acc=f_acc, f_bik='044525303', t_acc=t_acc, currency=currency,
        summ='6000000.00' if status == 'INSUFFICIENT_FUNDS' else '2.01'
    )
    assert type(bcl_number) == str
    transaction_info = wait_processing_payment([transaction_id])[0]
    expected_status = PAYPAL_STATUSES_MAPPING[status]

    assert transaction_info["ic_id"] == transaction_id
    assert transaction_info["status"] == (
        'denied_by_the_bank' if expected_status in ('error', 'other') else expected_status)
    assert ('Счёт заблокирован.' if status == 'FAILED' else status) in transaction_info["processing_notes"]


@pytest.mark.xfail(reason='Периодически падает при попытке авторизации в PayPal с ошибкой Network is unreachable')
@pytest.mark.regression
def test_paypal_info_from_token(bcl_paypal_info_from_token, get_paypal_auth_data):
    client_id, secret = get_paypal_auth_data
    response = requests.post(
        'https://api.sandbox.paypal.com/v1/oauth2/token', data={'grant_type': 'client_credentials'},
        auth=(client_id, secret), verify=False)
    assert response.status_code == 200
    response = bcl_paypal_info_from_token(response.json()['access_token'])
    assert not response.get('error')
    assert set(response.keys()) == set(['api', '__data__', 'error', 'headers', 'header', 'request_id'])
    assert response['api'].get('client_id')
    assert response['api'].get('client_secret')

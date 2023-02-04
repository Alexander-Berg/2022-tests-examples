import uuid
import random

import pytest

PAYONEER_STATUSES_MAPPING = {
    'Transferred': 'exported_to_h2h',
    '10403': 'error',
    '10303': 'error',
    '10005': 'error'
}

PAYMENTS_WITH_STATUSES = [
    pytest.param(
        '2774218', '21.00', 'USD', 'Transferred',
        marks=pytest.mark.xfail(reason='Часто Payoneer может отвечать долго')
    ),
    ('123321123', '2.00', 'USD', '10403'),
    ('123321123', '3001.00', 'RUB', '10303'),
    pytest.param(
        '6386409322', '22.00', 'USD', 'Transferred',
        marks=pytest.mark.xfail(reason='Часто Payoneer может отвечать долго')
    ),
]

CANCEL_PAYMENTS = [
    ('123321123', '21.00', 'rejected'),
    ('123321123', '21.00', 'processing'),
]


@pytest.mark.regression
@pytest.mark.parametrize('t_acc, amount, currency, status', PAYMENTS_WITH_STATUSES)
def test_send_and_check_payment(t_acc, amount, currency, status, bcl_send_payment, wait_processing_payment):
    # тест успешный платеж может падать по таймауту
    # чаще всего payoneer не успевает обработать платеж
    # было замечено, что утром это происходит быстрее всего
    transaction_id = str(uuid.uuid4()).upper()
    result = bcl_send_payment(
        transaction_id, f_acc='111222333', f_bik='PAYNUS31', t_acc=t_acc, currency=currency, summ=amount,
        t_acc_type='default', service_alias='market'
    )
    assert 'doc_number' in result.keys()
    assert result['payment_status'] == 'new'

    transaction_info = wait_processing_payment([transaction_id], source_oebs=False)[0]

    assert transaction_info['status'] == PAYONEER_STATUSES_MAPPING[status]
    assert status in transaction_info['payment_system_answer']


@pytest.mark.regression
@pytest.mark.parametrize('t_acc, amount, status', CANCEL_PAYMENTS)
def test_cancel_payment(
        t_acc, amount, status, bcl_send_payment, bcl_cancel_payments, bcl_get_payments, wait_processing_payment,
        wait_condition_and_do_something):
    transaction_id = str(uuid.uuid4()).upper()
    result = bcl_send_payment(
        transaction_id, f_acc='111222333', f_bik='PAYNUS31', t_acc=t_acc, currency='USD', summ=amount,
        t_acc_type='default', service_alias='market'
    )
    assert 'doc_number' in result.keys()
    assert result['payment_status'] == 'new'
    result = bcl_get_payments([transaction_id])[0]
    if status == 'rejected':
        run_func = lambda: bcl_get_payments([transaction_id])
        condition = lambda func_result: 'Pending' not in func_result[0]['processing_notes']
        result = wait_condition_and_do_something(condition, run_func, time_sleep=1)[0]

    assert result['status'] == 'processing'

    result = bcl_cancel_payments([transaction_id])
    assert result[transaction_id]['success'] == (True if status == 'rejected' else False)

    payment_info = bcl_get_payments([transaction_id])[0]
    assert payment_info['status'] == status


@pytest.mark.regression
@pytest.mark.parametrize(
    't_acc, currency, status_code, status', [
        pytest.param(
            '123321123', 'USD', '', 'exported_to_h2h',
            marks=pytest.mark.xfail(reason='Часто Payoneer может отвечать долго')
        ),
        ('123321123', 'RUB', '10403', 'denied_by_the_bank'),
        ('tester1', 'USD', '10005', 'denied_by_the_bank'),
    ]
)
def test_from_oebs_to_payoneer(t_acc, currency, status_code, status, bcl_set_pd, wait_processing_payment):
    transaction_id = str(uuid.uuid4()).upper()
    bcl_number = bcl_set_pd(
        transaction_id, f_acc='111222333', f_bik='PAYNUS31', t_acc=t_acc, currency=currency, summ='22.00',
        service_alias='market', purp='TestPayment'
    )
    assert type(bcl_number) == str
    transaction_info = wait_processing_payment([transaction_id], source_oebs=True)[0]

    assert transaction_info['status'] == status
    assert status_code in transaction_info['processing_notes']


@pytest.mark.regression
def test_api_get_login_link(bcl_payoneer_get_login_link):
    t_acc = str(random.randint(100000000, 9999999999))

    login_link = bcl_payoneer_get_login_link('100101370', t_acc)
    assert set(['audit_id', 'code', 'description', 'login_link']) == set(login_link.keys())
    assert 'https://payouts.sandbox.payoneer.com/partners/lp.aspx?token=' in login_link['login_link']


@pytest.mark.regression
def test_api_get_payee_status_not_found(bcl_payoneer_get_payee_status):
    t_acc = str(random.randint(100000000, 9999999999))

    result = bcl_payoneer_get_payee_status('100101370', t_acc)

    assert result['status'] == 'error'
    assert 'UserHandledException' in result['traceback']
    assert 'Please ensure that the payee has registered with Payoneer' in result['status_desc']


@pytest.mark.regression
def test_api_get_payee_status(bcl_payoneer_get_payee_status):
    payee_status = bcl_payoneer_get_payee_status('100101370', '123321123')
    assert set(['audit_id', 'code', 'description', 'status']) == set(payee_status.keys())
    assert payee_status['code'] == 0
    assert payee_status['description'] == 'Success'
    assert payee_status['status'] == 'ACTIVE'

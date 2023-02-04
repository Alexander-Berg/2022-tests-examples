import uuid
import random

import pytest

skip_pingpong = pytest.mark.skip(reason="Сломана интееграция с pingpong - после переезда в деплой не добавили новые ip в whitelist")

@skip_pingpong
@pytest.mark.regression
def test_api_get_login_link(bcl_pingpong_get_login_link):
    t_acc = str(random.randint(1000000, 99999999))

    login_link = bcl_pingpong_get_login_link(
        t_acc, 'USD', 'RU', 'Test', 'https://test.ru', 'https://test.ru'
    )
    assert set(['seller_Id', 'token', 'redirect_url']) == set(login_link.keys())
    assert f"/external/partners/account-bind?token={login_link['token']}" in login_link['redirect_url']

    assert login_link['seller_Id'] == t_acc


@skip_pingpong
@pytest.mark.regression
def test_api_get_payee_status_not_found(bcl_pingpong_get_payee_status):
    t_acc = str(random.randint(100000000, 9999999999))

    result = bcl_pingpong_get_payee_status(t_acc)
    assert result['status'] == 'error'
    assert 'UserHandledException' in result['traceback']
    assert '"data": null' in result['traceback']
    assert '"code": "4101"' in result['traceback']
    assert '"apiName": "/account/status"' in result['traceback']
    assert '"message": "RECORD_NOT_FOUND"' in result['traceback']


@skip_pingpong
@pytest.mark.regression
def test_api_get_payee_status(bcl_pingpong_get_payee_status):
    payee_status = bcl_pingpong_get_payee_status('11223349')
    assert set(['seller_id', 'status']) == set(payee_status.keys())
    assert payee_status['seller_id'] == '11223349'
    assert payee_status['status'] == 'Approved'


@skip_pingpong
@pytest.mark.regression
@pytest.mark.parametrize(
    'status, t_acc',
    [
        ('exported_to_h2h', 'yandex_seller_2'),
        ('denied_by_the_bank', '182863592'),
    ])
def test_set_pd(status, t_acc, bcl_set_pd, wait_processing_payment):
    transaction_id = str(uuid.uuid4()).upper()
    bcl_number = bcl_set_pd(
        transaction_id, f_acc='1001034', f_bik='PINGPONG', t_acc=t_acc, currency='USD', summ='22.00',
        n_ground='PingPong payment', service_alias='market'
    )
    assert type(bcl_number) == str

    transaction_info = wait_processing_payment([transaction_id], source_oebs=True)

    assert len(transaction_info) == 1
    assert transaction_info[0]['status'] == status


@skip_pingpong
@pytest.mark.regression
@pytest.mark.parametrize(
    'status, t_acc',
    [
        ('exported_to_h2h', 'yandex_seller_2'),
        ('exported_to_h2h', '11223349'),
        ('error', '182863592'),
    ])
def test_send_payment(status, t_acc, bcl_send_payment, wait_processing_payment):
    transaction_id = str(uuid.uuid4()).upper()
    result = bcl_send_payment(
        transaction_id, f_acc='1001034', f_bik='PINGPONG', t_acc=t_acc, currency='USD', t_acc_type='default',
        service_alias='market'
    )
    assert 'doc_number' in result.keys()
    assert result['payment_status'] == 'new'

    transaction_info = wait_processing_payment([transaction_id], source_oebs=False)

    assert len(transaction_info) == 1
    assert transaction_info[0]['status'] == status

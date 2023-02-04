import uuid
import time

import pytest

PAYONEER_STATUSES_MAPPING = {
    '5': 'exported_to_h2h',
    '4': 'error',
    '6': 'error',
    '10': 'error'
}

PAYMENTS_WITH_STATUSES = [
    ('[mock_server,success]', 'wallet', '5', '100.2', '79991112233'),
    ('[mock_server,success]', 'wallet', '4', '1.2', '79991112233'),
    ('[mock_server,wallet_does_not_exist]', 'wallet', '6', '100.2', '79991112233'),
    ('[mock_server,success]', 'card', '10', '100.2', '79991112233'),
    ('[mock_server,success]', 'card', '5', '100.2', '5541167599629344'),
    ('[mock_server,payment_forbidden]', 'card', '6', '100.2', '1837632156644971827'),
]


def create_payment(creation_func, acc_type, t_acc, summ, ground):
    transaction_id = str(uuid.uuid4()).upper()
    result = creation_func(
        transaction_id, f_acc='17', f_bik='044525416', t_acc=t_acc, currency='RUB', summ=summ,
        t_acc_type=acc_type, service_alias='toloka', ground=ground
    )
    assert 'doc_number' in result.keys()
    assert result['payment_status'] == 'new'
    return transaction_id


@pytest.mark.skip
@pytest.mark.regression
@pytest.mark.parametrize('ground, acc_type, status, summ, t_acc', PAYMENTS_WITH_STATUSES)
def test_send_and_check_payment(ground, acc_type, status, summ, t_acc, bcl_send_payment, wait_processing_payment):
    transaction_id = create_payment(bcl_send_payment, acc_type, t_acc, summ, ground)

    transaction_info = wait_processing_payment([transaction_id], source_oebs=False)[0]

    assert transaction_info['status'] == PAYONEER_STATUSES_MAPPING[status]
    assert f'[{status}]' in transaction_info['processing_notes']


@pytest.mark.skip
@pytest.mark.regression
@pytest.mark.parametrize('ground, acc_type, status, summ, t_acc', PAYMENTS_WITH_STATUSES)
def test_probe_payment(ground, acc_type, status, summ, t_acc, bcl_probe_payment, wait_processing_payment):
    transaction_id = str(uuid.uuid4()).upper()
    result = bcl_probe_payment(
        transaction_id, f_acc='17', f_bik='044525416', t_acc=t_acc, summ=101, t_acc_type=acc_type, ground=ground
    )
    if t_acc == '79991112233' and acc_type == 'card':
        assert result['status_bcl'] == 2
    else:
        assert result['status_bcl'] == 10


@pytest.mark.skip
@pytest.mark.regression
@pytest.mark.parametrize(
    'ground, acc_type, t_acc',
    [
        ('[mock_server,fail]', 'wallet', '79991112233'),
        ('[mock_server,internal_server_error]', 'wallet', '79991112233'),
        ('[mock_server,success]', 'card', '1837632156644971827'),
    ])
def test_send_processing_payment(ground, acc_type, t_acc, bcl_send_payment, bcl_get_payments):
    # chihiro: так как тесты просто спят, то я пропускаю их в регресси, но оставлю эти кейсы для ручных отладок
    transaction_id = create_payment(bcl_send_payment, acc_type, t_acc, '100.2', ground)
    time.sleep(10)

    transaction_info = bcl_get_payments([transaction_id])[0]

    assert transaction_info['status'] == 'processing'
    assert '[4]' in transaction_info['processing_notes']


@pytest.mark.skip
@pytest.mark.regression
def test_api_get_qiwi_config(bcl_qiwi_get_config):
    result = bcl_qiwi_get_config('17')
    assert result['result']
    assert result['error_code'] == 0
    assert len(result['data']) == 1
    assert set(result['data'][0].keys()) == set(['id', 'name', 'payways', 'rating'])


@pytest.mark.skip
@pytest.mark.regression
def test_api_qiwi_payment_calculation(bcl_qiwi_payment_calculation):
    result = bcl_qiwi_payment_calculation(
        '17', 643, 'qiwi_topup_rub_to_wallet[mock_server,success]', '101', 'ps_amount'
    )
    assert result['result']
    assert result['error_code'] == 0
    assert set(
        result['data'].keys()
    ) == set(['account_info_config', 'info', 'payee_receive', 'ps_currency', 'shop_currency', 'shop_write_off'])

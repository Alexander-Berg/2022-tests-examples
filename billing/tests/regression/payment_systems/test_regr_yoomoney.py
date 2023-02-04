import uuid

import pytest

YOOMONEY_STATUSES_MAPPING = {
    '0': 'exported_to_h2h',
    '3': 'error'
}

BCL_STATUSES_MAPPING = {
    '0': 7,
    '3': 2
}

PAYMENTS_WITH_STATUSES = [
    ('41001614575714', '0', ''),
    ('410011806060695', '3', '42'),
    ('41001614575572', '3', '43'),
    ('41001614575643', '3', '44'),
    ('410011806060766', '3', '45'),
    ('4100322407607', '3', 'Счёт заблокирован.'),
]


@pytest.mark.regression
@pytest.mark.parametrize('t_acc, yoomoney_status, error_code', PAYMENTS_WITH_STATUSES)
def test_send_and_check_payment(t_acc, yoomoney_status, error_code, bcl_send_payment, wait_processing_payment):
    transaction_id = str(uuid.uuid4()).upper()
    f_acc = '201635' if 'заблокирован' in error_code else '700500'
    params = '{"phone_op": 905, "phone_num": 8182838}' if t_acc == '2570066957329' else None
    result = bcl_send_payment(transaction_id, f_acc=f_acc, f_bik='044579444', t_acc=t_acc, params=params)
    assert 'doc_number' in result.keys()
    assert result['payment_status'] == 'new'

    transaction_info = wait_processing_payment([transaction_id], source_oebs=False)[0]

    assert transaction_info['status'] == YOOMONEY_STATUSES_MAPPING[yoomoney_status]
    if error_code:
        assert transaction_info['error_code'] == error_code


@pytest.mark.regression
@pytest.mark.parametrize('t_acc, yoomoney_status, error_code', PAYMENTS_WITH_STATUSES[:-1])
def test_probe_payment(t_acc, yoomoney_status, error_code, bcl_probe_payment):
    f_acc = '201635' if 'заблокирован' in error_code else '700500'
    transaction_id = str(uuid.uuid4()).upper()
    params = '{"phone_op": 905, "phone_num": 8182838}' if t_acc == '2570066957329' else None
    result = bcl_probe_payment(transaction_id, f_acc=f_acc, f_bik='044579444', t_acc=t_acc, params=params)

    assert set(result.keys()) == set(['status_remote', 'status_bcl', 'status_remote_hint'])
    assert result['status_remote'] == (error_code if error_code else yoomoney_status)
    assert result['status_bcl'] == BCL_STATUSES_MAPPING[yoomoney_status]


@pytest.mark.regression
@pytest.mark.parametrize('t_acc, yoomoney_status, error_code', PAYMENTS_WITH_STATUSES)
def test_from_oebs_to_yoomoney(bcl_set_pd, t_acc, yoomoney_status, error_code, wait_processing_payment):
    f_acc = '201635' if 'заблокирован' in error_code else '700500'
    mapping_states = YOOMONEY_STATUSES_MAPPING
    mapping_states.update({'3': 'denied_by_the_bank'})

    transaction_id = str(uuid.uuid4()).upper()
    bcl_number = bcl_set_pd(transaction_id, f_acc=f_acc, f_bik='044579444', t_acc=t_acc)
    assert type(bcl_number) == str
    transaction_info = wait_processing_payment([transaction_id])[0]

    assert transaction_info['ic_id'] == transaction_id
    assert transaction_info['status'] == mapping_states[yoomoney_status]
    if error_code:
        assert error_code in transaction_info['processing_notes']
        assert f', clientOrderId {bcl_number}' in transaction_info['processing_notes']

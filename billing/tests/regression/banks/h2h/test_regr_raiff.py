import time
import uuid
from datetime import datetime, timedelta

import pytest


def run_raiff_tasks(run_func, get_status_func, num_src, tasks, time_sleep=5, final_statuses=None):
    final_statuses = final_statuses or ['processing', 'denied_by_the_bank']
    until = time.time() + 60
    while get_status_func([num_src])[0]['status'] not in final_statuses and until > time.time():
        time.sleep(time_sleep)
        time_sleep += 2
        for task in tasks:
            run_func(task)


@pytest.mark.regression
@pytest.mark.xfail(reason='Иногда может не работать тестовая среда райффа')
@pytest.mark.parametrize(
    't_acc, status',
    [
        ('40702810600000000604', 'processing'),
        ('00000000000000000000', 'denied_by_the_bank')
    ])
@pytest.mark.parametrize("payment_count",
                         [1, 2])
def test_h2h_raiff_payment_rub(
        t_acc, status, payment_count, export_h2h_and_wait_processing, run_force_task_with_retries,
        bcl_get_multiple_status, raiff):
    set_pd_params = {
        'f_acc': '40702810500001400742', 'f_bik': raiff['bid'], 'summ': '13.00', 'currency': 'RUB',
        'f_bnk': 'АО "РАЙФФАЙЗЕНБАНК"', 'f_kpp': '770401001', 'f_name': 'ООО ЯНДЕКС', 'f_inn': '7736207543',
        'f_cacc': '30101810200000000700', 't_bik': '040173745', 't_bnk': '"СИБСОЦБАНК" ООО',
        't_cacc': '30101810800000000745', 't_kpp': '772501001', 't_inn': '7708764126', 't_name': 'Тестовый',
        't_addr': 'Г. МОСКВА', 't_acc': t_acc, 't_bnk_city': 'МОСКВА', 'purp': 'В том числе НДС 0.00.'
    }

    payments_list = export_h2h_and_wait_processing(raiff['id'], set_pd_params, payment_count, 'create')

    run_raiff_tasks(run_force_task_with_retries, bcl_get_multiple_status, payments_list[0][0], tasks=['raiff_payment_status_sync_prepare', 'raiff_payment_status_sync'])

    for number_src, _, _ in payments_list:
        payment_info = bcl_get_multiple_status([number_src])[0]

        assert payment_info['status'] == status

        if status == 'processing':
            assert ('Принят' in payment_info['processing_notes']) or ('Доставлен' in payment_info['processing_notes'])
        else:
            assert 'Ключ счета получателя указан неверно' in payment_info['processing_notes']


@pytest.mark.regression
@pytest.mark.xfail(reason='Иногда может не работать тестовая среда райффа')
@pytest.mark.parametrize(
    'oper_code, status, t_bnk_city, t_country',
    [
        ('21100', 'processing', 'AMSTERDAM', '528'),
        ('00000', 'denied_by_the_bank', 'AMSTERDAM', '528'),
        ('21200', 'processing', 'SANTA CLARA,CA', '840'),
        ('00000', 'denied_by_the_bank', 'SANTA CLARA,CA', '840')
    ])
def test_h2h_raiff_payment_cur(
        oper_code, status, t_bnk_city, t_country, export_h2h_and_wait_processing, run_force_task_with_retries,
        bcl_get_multiple_status, raiff):
    set_pd_params = {
        'f_acc': '40702840800001400742', 'f_bik': raiff['bid'], 'summ': '12.00', 'currency': 'USD',
        'f_bnk': 'АО "РАЙФФАЙЗЕНБАНК"', 'f_kpp': '', 'f_name': 'YANDEX LLC', 'f_inn': '7736207543', 'cur_op': oper_code,
        'f_cacc': '30101810200000000700', 't_bic': 'BOFANLNX', 't_bnk': 'ING Bank N.V', 't_country': t_country,
        't_kpp': '', 't_inn': '000000000000', 't_name': 'Ledeneva Valeria Andreevna', 't_acc': 'NL03INGB0662303938',
        't_iban': 'NL03INGB0662303938', 't_addr': 'Shmuel Ben Adaya, 7, apt 4, Tel Aviv-Yafo, Israel 6803207',
        't_bnk_city': t_bnk_city, 'purp': 'Payment to the Act dd 05/11/2018, Contract 10191583 dd 01/08/2018',
    }
    edit_params = [
        ('trans_pass', '11111111/2495/0000/4/0'),
        ('official_name', 'Test Robot'),
        ('official_phone', '+7 495 739-70-00 25437'),
        ('ground', set_pd_params['purp']),
        ('f_name', set_pd_params['f_name']),
        ('t_address', set_pd_params['t_addr']),
        ('paid_by', 'OUR'),
        ('contract_num', ''),
        ('contract_dt', ''),
        ('contract_sum', ''),
        ('contract_currency_id', ''),
        ('contract_currency_id', ''),
        ('expected_dt', (datetime.now() + timedelta(days=7)).strftime('%d-%m-%Y') if oper_code == '21100' else ''),
        ('advance_return_dt', ''),
        ('currency_op_docs', ''),
        ('i_info', ''),
        ('write_off_account', ''),
    ]

    transaction_id, bcl_number, payment_id = export_h2h_and_wait_processing(
        raiff['id'], set_pd_params, method='create', edit_params=edit_params
    )[0]

    run_raiff_tasks(run_force_task_with_retries, bcl_get_multiple_status, transaction_id, tasks=['raiff_payment_status_sync_prepare', 'raiff_payment_status_sync'])

    payment_info = bcl_get_multiple_status([transaction_id])[0]
    assert payment_info['status'] == status
    if status == 'processing':
        assert ('Принят' in payment_info['processing_notes']) or ('Доставлен' in payment_info['processing_notes'])
    else:
        assert 'Код вида валютной операции не найден в справочнике' in payment_info['processing_notes']


@pytest.mark.regression
@pytest.mark.parametrize(
    't_acc, status, t_bank',
    [
        ('79191234567', 'exported_to_h2h', 'RAIFFEISEN'),
        ('79191234567', 'error', 'RAIFFFEISEN'),
    ])
def test_sbp(t_acc, status, t_bank, bcl_send_payment, wait_processing_payment, run_force_task_with_retries, bcl_get_multiple_status, run_force_task):
    transaction_id = str(uuid.uuid4()).upper()
    result = bcl_send_payment(
        transaction_id, f_acc='40700000007736207543', f_bik='044525700', t_acc=t_acc, currency='RUB',
        t_acc_type='default', summ='2.01', params='{"t_bank_alias": "_t_bank_", "t_inn": "3664069397"}'.replace('_t_bank_', t_bank)
    )
    assert 'doc_number' in result.keys()
    assert result['payment_status'] == 'new'

    run_raiff_tasks(
        run_force_task_with_retries, bcl_get_multiple_status, transaction_id, tasks=['raiff_statuses'],
        final_statuses=['completed', 'denied_by_the_bank']
    )

    transaction_info = wait_processing_payment([transaction_id], source_oebs=False)[0]

    assert transaction_info["status"] == status


@pytest.mark.regression
@pytest.mark.parametrize(
    't_acc, status, t_bank',
    [
        ('79191234567', 'completed', 'RAIFFEISEN'),
        ('79191234567', 'denied_by_the_bank', 'RAIFFFEISEN'),
    ])
def test_sbp_oebs(t_acc, status, t_bank, bcl_set_pd, wait_processing_payment, run_force_task, raiff, bcl_get_multiple_status, run_force_task_with_retries):
    transaction_id = str(uuid.uuid4()).upper()
    bcl_number = bcl_set_pd(
        transaction_id, f_acc='40700000007736207543', f_bik='044525303', t_acc=t_acc, currency='RUB', t_bik=t_bank,
        t_inn='3664069397', payout_type=3, purp='test'
    )
    assert type(bcl_number) == str

    run_raiff_tasks(run_force_task_with_retries, bcl_get_multiple_status, transaction_id, tasks=['raiff_statuses'], final_statuses=['completed', 'denied_by_the_bank'])

    transaction_info = bcl_get_multiple_status([transaction_id])[0]

    assert transaction_info["ic_id"] == transaction_id
    assert transaction_info["status"] == status

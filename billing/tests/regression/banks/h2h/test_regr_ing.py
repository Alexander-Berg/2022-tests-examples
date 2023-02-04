import datetime

import pytest


@pytest.mark.regression
@pytest.mark.parametrize(
    'curr, t_bic, t_iban, t_bnk, t_bnk_city, t_acc, is_correct',
    [('USD', 'INGBNL2AXXX', 'NL03INGB0662303938', 'HSBC BANK USA, N.A.', 'NEW YORK,NY', '40702840600001001296', True),
     ('EUR', 'MRMDUS33XXX', 'DE85500210000110134450', 'Billing', 'Berlin', '000183474', False)]
)
def test_h2h_payment_accepted(
        curr, t_bic, t_iban, t_bnk, t_bnk_city, t_acc, is_correct, bcl_set_pd, bcl_get_multiple_status,
        get_bundle_status, get_bundle_id, wait_condition_and_run_task, export_h2h_and_wait_processing, ing_nl):
    date = datetime.datetime.now()
    account = 'NL43INGB0020010540' if is_correct else 'NL43INGB0020010541'
    set_pd_params = {
        'con_curr': 'USD', 'con_dt': date, 'i_bic': 'INGBDEFFXXX', 'purp': 'PAYPayment for test', 't_inn': '7725713770',
        'con_num': '666', 'cur_op': '01', 'con_sum': '2.00', 'paid_by': 'OUR', 'tr_dt': date, 'f_inn': '27265167',
        'currency': curr, 't_addr': 'USD  Palatine Dept Ch 19228 IL 60055-9228', 't_name': 'Computershare Inc',
        't_bic': t_bic, 't_bik': '044525222', 't_cacc': '30101810500000000222', 'f_cacc': '30101810300000000545',
        't_iban': t_iban, 'f_iban': account, 't_bnk': t_bnk, 't_kpp': '772501001', 't_bnk_city': t_bnk_city,
        'com_acc': '220410123784000', 'n_okato': '000017', 'f_name': 'Yandex N.V.', 'f_acc': account,
        'f_bnk': 'ING BANK N.V.', 'f_bic': 'INGBNL2AXXX', 'f_kpp': '997750001', 'tr_pass': '111', 't_acc': t_acc
    }

    transaction_id, bcl_number, _ = export_h2h_and_wait_processing(ing_nl['id'], set_pd_params, method='create')[0]

    bundle_id = get_bundle_id(bcl_number, ing_nl['id'])

    _condition = lambda: 'Выгружен в host-to-host' in get_bundle_status(bundle_id, ing_nl['id'])

    wait_condition_and_run_task(_condition, 'ing_update_payment_statuses')
    bundle_status = get_bundle_status(bundle_id, ing_nl['id'])

    if is_correct:
        assert ('Принят в host-to-host' in bundle_status) or ('В обработке' in bundle_status)

    else:
        assert 'Отказан банком' in bundle_status

        payment_info = bcl_get_multiple_status([transaction_id])[0]
        assert payment_info['status'] == 'denied_by_the_bank'
        assert 'Invalid' in payment_info['processing_notes']

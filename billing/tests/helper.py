# coding: utf-8
import datetime

from agency_rewards.rewards.utils.dates import (
    get_first_dt_prev_month,
    get_last_dt_prev_month,
)


def prev_month_from_dt(dt=None):
    dt = dt or datetime.datetime.now()
    return get_first_dt_prev_month(dt)


def prev_month_till_dt(dt=None, zero_seconds=0):
    dt = dt or datetime.datetime.now()
    return get_last_dt_prev_month(dt, zero_seconds)


def create_contract_data(
    contract_id=1,
    commission_type=0,
    scale=0,
    client_count=0,
    is_boc=False,
    is_boc_1m_ago=False,
    amt=0,
    amt_ttl=0,
    invoice_cnt=0,
    invoice_prep_cnt=0,
    from_dt=None,
    till_dt=None,
    prepayment_amt=0,
    postpayment_amt=0,
    payment_control_type=0,
):
    """
    Для создания data dict в юнит тестах для тестирования calc методов
    """
    return {
        'contract_id': contract_id,
        'contract_eid': 'C-{}'.format(contract_id),
        'commission_type': commission_type,
        'scale': scale,
        'client_count': client_count,
        'is_boc': is_boc,
        'is_boc_1m_ago': is_boc_1m_ago,
        'amt': amt,
        'amt_ttl': amt_ttl,
        'invoice_cnt': invoice_cnt,
        'invoice_prep_cnt': invoice_prep_cnt,
        'from_dt': from_dt or prev_month_from_dt(),
        'till_dt': till_dt or prev_month_till_dt(),
        'prepayment_amt': prepayment_amt or amt,
        'postpayment_amt': postpayment_amt,
        'payment_control_type': payment_control_type,
    }

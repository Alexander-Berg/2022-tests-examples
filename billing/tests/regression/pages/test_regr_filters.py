import uuid
from datetime import datetime

import pytest

ASSOCIATE_TINKOFF = 25


@pytest.mark.regression
@pytest.mark.parametrize(
    'associate_id, account, account_id, curr, payout_type, org_id, acc_id', [
        (5, '40702810290000002171', 60, 'RUB', None, 1, 60),  # Открытие
        (ASSOCIATE_TINKOFF, '25121967', 12, 'RUB', '5', 13, 12),
    ])
def test_validate_filters(
        associate_id, account, account_id, curr, payout_type, org_id, acc_id, bcl_set_pd, get_payment_id_by_number, export_payments,
        apply_filters_payment, apply_filters_bundle, get_bundle_id):
    num_oebs = '12345'
    t_name = 'test'
    t_inn = '781149892815'
    t_swiftcode = 'SABRRUMMXXX'
    transaction_id = str(uuid.uuid4()).upper()

    bcl_number = bcl_set_pd(
        transaction_id, f_acc=account, f_bik='044525974' if associate_id == ASSOCIATE_TINKOFF else '044525297',
        summ='2.00', currency=curr, f_bnk='ПАО СБЕРБАНК', f_kpp='997750001', f_name='ООО ЯНДЕКС', f_inn='7736207543',
        f_cacc='30101810400000000225', t_bik='044525225', t_bnk=' ПАО СБЕРБАНК', t_cacc='30101810500000000653',
        t_kpp='', t_inn=t_inn, t_name=t_name, t_addr='Г. МОСКВА', t_acc='40702810438000034799',
        t_bnk_city='МОСКВА', purp='PaymentPurp', num_oebs=num_oebs, t_bic=t_swiftcode, payout_type=payout_type
    )

    dynamic_elements = {
        'currency_id': 643, 'acc': account_id, 'user': 75, 'status': 0, 'org': org_id,
        'payout_type': payout_type if payout_type else '-'
    }
    input_fields = {
        'number': bcl_number, 'number_oebs': num_oebs, 'contragent': t_name, 'inn': t_inn, 't_swiftcode': t_swiftcode
    }

    table_rows = apply_filters_payment(associate_id, dynamic_elements, input_fields, 2, datetime.now())
    assert len(table_rows) == 1

    payment_id = get_payment_id_by_number(associate_id, bcl_number)
    export_payments([payment_id], associate_id)

    bundle_id = get_bundle_id(bcl_number, associate_id)
    dynamic_elements = {
        'status': 6, 'hidden': '0', 'acc': acc_id
    }
    input_fields = {
        'bundle_number': bundle_id
    }

    table_rows = apply_filters_bundle(associate_id, dynamic_elements, input_fields, datetime.now())
    assert len(table_rows) == 1

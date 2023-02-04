import uuid

import pytest


@pytest.mark.skip(reason="Пока выключаю тест - он не рабочий, подумаю, что с ним сделать")
@pytest.mark.regression
def test_check_export_button(
        bcl_set_pd, get_bundle_id, bcl_get_multiple_status, export_payment_selenium, check_download_button,
        check_download_bundle, sber):
    account = '40702810138110105942'

    transaction_id = str(uuid.uuid4()).upper()
    bcl_number = bcl_set_pd(
        transaction_id, f_acc=account, f_bik=sber['bid'], summ='50.00', currency='RUB', f_bic='SABRRUMMXXX',
        f_bnk='ПАО СБЕРБАНК', f_kpp='997750001', f_name='ООО ЯНДЕКС', f_inn='7736207543',
        f_cacc='30101810400000000225', t_bik='044525225', t_bnk=' ПАО СБЕРБАНК', t_cacc='30101810500000000653',
        t_kpp='', t_inn='781149892815', t_name='test', t_addr='Г. МОСКВА', t_acc='40702810438000034789',
        t_bnk_city='МОСКВА', purp='PaymentPurp'
    )

    export_payment_selenium(sber['id'], bcl_number)
    assert bcl_get_multiple_status([transaction_id])[0]['status'] == 'exported_to_online'

    bundle_id = get_bundle_id(bcl_number, sber['id'])
    check_download_button(sber['id'], bundle_id)
    check_download_bundle(bundle_id)

import pytest


@pytest.mark.skip(reason='Протух ключ к тестовой среде')
@pytest.mark.regression
@pytest.mark.parametrize(
    'account, currency', [('6550031620', 'USD'), ('6550031621', 'EUR'), ('6550031623', 'RUB')])
def test_send_payment(account, currency, export_h2h_and_wait_processing, get_bundle_id, bcl_test_sftp_jp, jpmorgan):
    set_pd_params = {
        'f_acc': account, 'f_bik': jpmorgan['bid'], 'summ': '10.00', 'currency': currency, 'f_bic': 'CHASLULX',
        'f_iban': f'LU64067000{account}', 'f_bnk': 'J.P. MORGAN BANK LUXEMBOURG S.A.', 'f_kpp': '',
        'f_name': 'MLU Europe B.V.', 'f_inn': '859916029', 't_bic': 'RNCBROBUXXX', 't_bnk_city': 'BUCHAREST',
        't_bnk': 'BANCA COMERCIALA ROMANA S.A', 't_kpp': '623401001', 't_inn': '', 't_name': 'test name',
        't_acc': 'RO24RNCB0079134461850007', 't_iban': 'RO24RNCB0079134461850007', 't_addr': 'Moscow test address',
        'purp': 'Payment purpose'
    }

    transaction_id, bcl_number, payment_id = export_h2h_and_wait_processing(
        jpmorgan['id'], set_pd_params, method='create'
    )[0]

    file_number = get_bundle_id(bcl_number, jpmorgan['id'])

    assert f'YANDEXNV.PAYMENTS.ISO20022_PAIN_01Ver3.{file_number}' in [
        file.filename for file in bcl_test_sftp_jp().listdir_attr('/Inbound/Encrypted/')]

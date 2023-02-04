import pytest
import uuid


@pytest.mark.xfail(reason='Тествый Райф часто отваливается')
@pytest.mark.regression
def test_api_get_creditors_by_status(bcl_get_creditors, raiff):
    result = bcl_get_creditors(raiff['bid'], statuses=['FETCHED'])
    assert not result.get('traceback')

    result = bcl_get_creditors(raiff['bid'], statuses=['SENT_TO_CHECK'])
    assert len(result) > 0


@pytest.mark.xfail(reason='Тествый Райф часто отваливается')
@pytest.mark.regression
def test_api_send_creditors(bcl_send_creditor, raiff, bcl_get_creditors, wait_condition_and_do_something):
    import random
    inn = random.randint(1000000000, 9999999999)
    result = {'traceback': True}
    wait_condition_and_do_something(
        lambda res: not res.get('traceback'),
        lambda: bcl_send_creditor(raiff['bid'],  "ООО ПК Аквариус", inn=str(inn),  ogrn=str(1027700032953))
    )
    assert result.get('state', {}).get('status', '') == 'FETCHED'

    result = bcl_send_creditor(raiff['bid'], inn='7901256405', name='ООО ПК Аквариус111', ogrn='1027700032953')
    assert 'exists' in result.get('message', '')

    result = bcl_send_creditor(raiff['bid'], inn='1111111111111', name='ООО ПК Аквариус', ogrn='1027700032953')
    assert 'inn must contain' in result.get('message', '')

    result = bcl_send_creditor(raiff['bid'], inn='1111111111', name='ООО ПК Аквариус', ogrn='10277000329531')
    assert 'Invalid OGRN' in result.get('message', '')

    result = bcl_get_creditors(raiff['bid'], inn=f'{inn}', name='ООО ПК Аквариус', ogrn='1027700032953')
    assert result.get('state', {}).get('status', '') == 'SENT_TO_CHECK'


@pytest.mark.xfail(reason='Тествый Райф часто отваливается')
@pytest.mark.regression
def test_send_payment(bcl_get_creditors, raiff, bcl_set_pd, export_h2h_and_wait_processing):
    account = 'TECH40702810200000020517'
    transaction_id = str(uuid.uuid4()).upper()
    transaction_id, bcl_number, _ = export_h2h_and_wait_processing(
        raiff['id'], {
            'num_src': transaction_id, 'f_acc': account, 'f_bik': raiff['bid'], 'summ': '10.45', 'currency': 'RUB',
            'f_bnk': 'АО «Райффайзенбанк»', 'f_kpp': '997750001', 'f_name': 'ООО Яндекс.Маркет', 'f_inn': '7704357909',
            'f_cacc': '30101810400000000225', 't_bik': '044525700', 't_bnk': 'АО «Райффайзенбанк»',
            't_cacc': '30101810200000000700', 't_kpp': '', 't_inn': '5029140360', 't_ogrn': '1105029008996',
            't_name': 'АЙ ПОИНТ ООО', 't_addr': 'Г. МОСКВА', 't_bic': '', 't_acc': '40702810900001430008',
            't_bnk_city': 'МОСКВА', 'purp': 'Оплата по контракту 111111 от 31.12.2021', 'tr_dt': '2021-10-28',
            'payout_type': 4, 'autobuild': 1, 'login': 'chihiro'
        }, method='create'
    )[0]


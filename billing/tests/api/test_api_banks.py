from bcl.core.models import Service
from bcl.banks.registry import Raiffeisen, Sber


def test_getfpsbanks(api_client, response_mock, get_assoc_acc_curr):
    _, account, _ = get_assoc_acc_curr(Raiffeisen, account='11111111')
    org = account.org
    org.connection_id = 'probki'
    org.save()

    api_client.mock_service(Service.TOLOKA)

    response = api_client.post('/api/proxy/getfpsbanks/', data={'acc_num': account.number}).json
    assert len(response['errors']) == 1

    response = api_client.post('/api/proxy/getfpsbanks/', data={'bik': Sber.bid}).json
    assert len(response['errors']) == 1

    account.sbp_payments = True
    account.save()

    with response_mock(
        'GET https://test.ecom.raiffeisen.ru/api/payout/v1/sbp/banks/ -> 200 : '
        '[{"alias": "RAIFFFEISEN", "name": "Райффайзенбанк"}]'
    ):
        response1 = api_client.post('/api/proxy/getfpsbanks/', data={'acc_num': account.number})
        response2 = api_client.post('/api/proxy/getfpsbanks/', data={'bik': Raiffeisen.bid})

    for response in [response1, response2]:
        response = response.json
        assert len(response['data']) == 1
        assert response['data'][0]['alias'] == 'RAIFFFEISEN'

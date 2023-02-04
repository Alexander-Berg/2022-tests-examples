from bcl.core.models import Service
from bcl.banks.registry import Raiffeisen
from bcl.banks.party_raiff.common import RaiffFactoringConnector


def test_get_creditor(api_client, response_mock, get_assoc_acc_curr, monkeypatch):

    monkeypatch.setattr(RaiffFactoringConnector, '_auth', lambda *args, **kwargs: ('1234', '5678'))
    api_client.mock_service(Service.MBI_BILLING)

    # не все параметры
    response = api_client.get(
        '/api/proxy/creditors/',
        data={'bik': Raiffeisen.bid, 'status': 'TEST', 'page': 1}).json

    assert len(response['errors']) == 1
    assert response['errors'][0]['description'] == 'Either (name + inn + orgn) or (statuses + page) is expected'

    # не все параметры
    response = api_client.get('/api/proxy/creditors/', data={'bik': Raiffeisen.bid}).json
    assert len(response['errors']) == 1
    assert response['errors'][0]['description'] == 'Either (name + inn + orgn) or (statuses + page) is expected'

    with response_mock(
        f'GET https://extest.openapi.raiffeisen.ru/payables-finance/creditors/by-statuses?statuses=FETCHED&'
        'size=500&page=1 -> 200 : [{"id": "26415d4a-de6b-11eb-ba80-0242ac130004", "fullName": "ОАО Поставляю все", '
        '"inn": 7704357909,  "ogrn": 116774649139, "status": {"status": "FETCHED", '
        '"statusDate": "2021-12-31T12:12:12.111111"}}]'
    ):
        response = api_client.get(f'/api/proxy/creditors/?statuses=["FETCHED"]&page=1&bik={Raiffeisen.bid}').json

    assert len(response['errors']) == 0


def test_create_creditor(api_client, response_mock, get_assoc_acc_curr, monkeypatch):

    monkeypatch.setattr(RaiffFactoringConnector, '_auth', lambda *args, **kwargs: ('1234', '5678'))
    api_client.mock_service(Service.MBI_BILLING)

    # не json
    with response_mock('POST https://extest.openapi.raiffeisen.ru/payables-finance/creditors/send-to-check -> 400:bad'):
        response = api_client.post(
            '/api/proxy/creditors/',
            data={'bik': Raiffeisen.bid, 'name': 'test', 'inn': '12334', 'ogrn': '123'}).json

    assert len(response['errors']) == 1

    # в ответе есть маркер, указывающий на наличие ошибки
    with response_mock(
        'POST https://extest.openapi.raiffeisen.ru/payables-finance/creditors/send-to-check -> 403:'
        '{"message": "nope"}'
    ):
        response = api_client.post(
            '/api/proxy/creditors/',
            data={'bik': Raiffeisen.bid, 'inn': '12334', 'ogrn': '123'}).json

    assert len(response['errors']) == 1

    # удачный вызов
    with response_mock(
        'POST https://extest.openapi.raiffeisen.ru/payables-finance/creditors/send-to-check -> 200 : '
        '{"id": "26415d4a-de6b-11eb-ba80-0242ac130004", "fullNameRus": "ОАО Поставляю все", "inn": 7704357909, '
        '"ogrn": 116774649139, "status": {"status": "FETCHED", "statusDate": "2021-12-31T12:12:12.111111"}}'
    ):
        response = api_client.post(
            '/api/proxy/creditors/',
            data={'bik': Raiffeisen.bid, 'name': 'test', 'inn': '12334', 'ogrn': '123'}).json

    assert len(response['errors']) == 0

    # удачный вызов с КПП
    with response_mock(
        'POST https://extest.openapi.raiffeisen.ru/payables-finance/creditors/send-to-check -> 200 : '
        '{"id": "1", "fullNameRus": "ОАО Поставляю все", "inn": 12334, "kpp": "12345", '
        '"ogrn": 123, "status": {"status": "FETCHED", "statusDate": "2021-12-31T12:12:12.111111"}}'
    ):
        response = api_client.post(
            '/api/proxy/creditors/',
            data={'bik': Raiffeisen.bid, 'name': 'test', 'inn': '12334', "kpp": "12345", 'ogrn': '123'}).json

    assert response['data']['kpp'] == '12345'

import json

from bcl.core.models import Service
from bcl.banks.registry import Qiwi


def test_showoutputconfig(api_client, response_mock, get_assoc_acc_curr):
    _, acc, _ = get_assoc_acc_curr(Qiwi, account='11')
    acc.remote_id = 'toloka'
    acc.save()

    api_client.mock_service(Service.TOLOKA)

    def call(resp, status_code=200):

        with response_mock(
            f'POST https://api-test.contactpay.io/gateway/v1/shop_output_config/shop -> {status_code} :{resp}'
        ):

            response = api_client.post('/api/proxy/qiwi/getconfig/', data={'shop_id': acc.number})

        return response

    response = call(
        '{"result": true, "message": "Ok", "error_code": 0, '
        '"data": [{"id": 10, "name": "card-usd", "rating": 2, '
        '"payways": [{"alias": "some-payway-usd", "currency": 978, "min_amount": 100, "max_amount": 1000000, '
        '"fee_config": {"percent": 0.1, "fix": 0.3, "min": 10, "max": 55}, '
        '"account_info_config": {"account": {"regex": "d{9,15}$", "title": "Номер Qiwi кошелька"}}}]}]}')
    assert response.ok

    response = response.json
    assert not response['errors']
    assert response['data']['result']
    assert response['data']['error_code'] == 0
    assert response['data']['data']


def test_checkaccount(api_client, response_mock, get_assoc_acc_curr):
    _, acc, _ = get_assoc_acc_curr(Qiwi, account='11')
    acc.remote_id = 'toloka'
    acc.save()

    api_client.mock_service(Service.TOLOKA)

    def call(resp, status_code=200):

        with response_mock(f'POST https://api-test.contactpay.io/gateway/v1/withdraw/try -> {status_code} :{resp}'):

            response = api_client.post('/api/proxy/qiwi/paymentcalculation/', data={
                    'shop_id': acc.number,
                    'shop_currency': acc.currency_id,
                    'payway': 'wallet_usd',
                    'amount': 100,
                    'amount_type': 'ps_amount',
                })

        return response
    resp = {
          'result': True,
          'message': 'Ok',
          'error_code': 0,
          'data': {
            'account_info_config': {
              'account': {
                'regex': '\\d{9,15}$',
                'title': 'Номер Qiwi кошелька, пример: 79123456789'
              }
            },
            'payee_receive': 100,
            'ps_currency': 978,
            'shop_currency': 978,
            'shop_write_off': '104'
          }
        }

    response = call(json.dumps(resp))
    assert response.ok

    response = response.json
    assert not response['errors']
    assert response['data']['result']
    assert response['data']['error_code'] == 0
    assert response['data']['data']
    assert set(response['data']['data'].keys()) == set(['account_info_config', 'payee_receive', 'ps_currency', 'shop_currency', 'shop_write_off'])

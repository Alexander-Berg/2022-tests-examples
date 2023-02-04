from bcl.core.models import Service


def test_getonboardinglink(api_client, response_mock):

    api_client.mock_service(Service.MARKET)

    def call(resp, status_code=200):

        with response_mock(f'POST https://test-ppapi.pingpongx.com/token/get -> {status_code} :{resp}'):

            response = api_client.post('/api/proxy/pingpong/getonboardinglink/', data={
                'seller_id': '10',
                'currency': 'USD',
                'country': 'US',
                'store_name': 'TestStore',
                'store_url': 'http://teststore',
                'notify_url': 'http://notify.url',
            })

        return response

    # Позитив.
    response = call(
        '{"apiName": "/token/get", "code": "0000", "message": "SUCCESS", "data": {'
        '"seller_Id":"10","token":"xxx","redirect_url":"http://notify.url?token=xxx"}}')
    assert response.ok

    response = response.json
    assert not response['errors']
    assert response['data'] == {
        'seller_Id': '10',
        'token': 'xxx',
        'redirect_url': 'http://notify.url?token=xxx'
    }

    # Негатив.
    response = call('{"apiName": "/token/get", "code": "5000", "message": "INTERNAL_ERROR", "data": null}', status_code=500)
    assert response.status_code == 500

    response = response.json
    assert response['errors'] == [{'apiName': '/token/get', 'code': '5000', 'message': 'INTERNAL_ERROR', 'data': None}]


def test_getsellerstatus(api_client, response_mock):

    api_client.mock_service(Service.MARKET)

    def call(resp, status_code=200):

        with response_mock(f'POST https://test-ppapi.pingpongx.com/account/status -> {status_code} :{resp}'):

            response = api_client.post('/api/proxy/pingpong/getsellerstatus/', data={
                'seller_id': '10',
            })

        return response

    # Позитив.
    response = call(
        '{"apiName": "/account/status", "code": "0000", "message": "SUCCESS", "data": {'
        '"seller_id":"10","status":"Approved","user_id":"123456"}}')
    assert response.ok

    response = response.json
    assert not response['errors']
    assert response['data'] == {'seller_id': '10', 'status': 'Approved', 'user_id': '123456'}

    # Негатив.
    response = call('{"code": "4101", "message": "RECORD_NOT_FOUND", "data": null}', status_code=404)
    assert response.status_code == 500

    response = response.json
    assert response['errors'] == [{'code': '4101', 'message': 'RECORD_NOT_FOUND', 'data': None}]

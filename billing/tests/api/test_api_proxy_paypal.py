from bcl.core.models import Service


def test_gettoken(api_client, response_mock):

    api_client.service_alias = Service.ALIASES[Service.MARKET]

    def call(resp, status_code=200):
        with response_mock(
            f'POST https://api.sandbox.paypal.com/v1/identity/openidconnect/tokenservice -> {status_code} :{resp}'
        ):
            response = api_client.post('/api/proxy/paypal/gettoken/', data={
                'auth_code': '1020',
            })

        return response

    # Позитив.
    response = call('{"token_type": "Bearer","expires_in": "10", "refresh_token": "yyy", "access_token": "zzz"}')
    assert response.ok
    response = response.json
    assert not response['errors']
    assert response['data'] == {
        'token_type': 'Bearer',
        'expires_in': '10',
        'refresh_token': 'yyy',
        'access_token': 'zzz'
    }

    # Негатив.
    response = call(
        '{"error_description":"Invalid authorization code","error":"access_denied",'
        '"correlation_id":"a3b3725994859",'
        '"information_link":"https://developer.paypal.com/docs/api/#errors"}',
        status_code=500)
    assert response.status_code == 500

    response = response.json
    assert response['errors'] == [{
        'error_description': 'Invalid authorization code',
        'error': 'access_denied', 'correlation_id': 'a3b3725994859',
        'information_link': 'https://developer.paypal.com/docs/api/#errors'}]


def test_nonjson_error_response(api_client, response_mock):

    api_client.service_alias = Service.ALIASES[Service.MARKET]

    with response_mock('POST https://api.sandbox.paypal.com/v1/identity/openidconnect/userinfo -> 500 :bogus'):
        response = api_client.post('/api/proxy/paypal/getuserinfo/', data={'token': 'testtoken'})
        assert response.status_code == 500
        assert response.json['errors'] == [{
            'raw': 'Failed. Response status: 500. Response message: Internal Server Error. Error message: bogus'}]


def test_getuserinfo(api_client, response_mock):

    api_client.service_alias = Service.ALIASES[Service.MARKET]

    def call(resp, status_code=200):

        with response_mock(
            f'POST https://api.sandbox.paypal.com/v1/identity/openidconnect/userinfo -> {status_code} :{resp}'
        ):
            response = api_client.post('/api/proxy/paypal/getuserinfo/', data={
                'token': 'testtoken',
            })

        return response

    # Позитив.
    response = call('{"language": "en_US", "user_id": "zzz", "email_verified": true}')
    assert response.ok

    response = response.json
    assert not response['errors']
    assert response['data'] == {'language': 'en_US', 'user_id': 'zzz', 'email_verified': True}

    # Негатив.
    response = call(
        '{"error_description":"Invalid authorization code","error":"access_denied",'
        '"correlation_id":"a3b3725994859",'
        '"information_link":"https://developer.paypal.com/docs/api/#errors"}',
        status_code=500)
    assert response.status_code == 500

    response = response.json
    assert response['errors'] == [{
        'error_description': 'Invalid authorization code',
        'error': 'access_denied', 'correlation_id': 'a3b3725994859',
        'information_link': 'https://developer.paypal.com/docs/api/#errors'}]

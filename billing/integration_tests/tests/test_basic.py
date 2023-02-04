import requests


def test_ping_yandexpay_backend(yandexpay_base_url):
    response = requests.get(yandexpay_base_url + '/ping')
    assert response.status_code == 200
    assert response.text == 'pong'


def test_ping_interaction_mock_server(interaction_mock_server_url):
    response = requests.get(interaction_mock_server_url + '/ping')
    assert response.status_code == 200
    assert response.text == 'pong'


def test_get_user_cards(yandexpay_base_url):
    response = requests.get(
        url=yandexpay_base_url + '/api/v1/user_cards',
        params={
            'default_uid': '7007',
        },
        cookies={
            'Session_id': 'bla',
            'yandexuid': '123',
        },
    )
    assert response.status_code == 200


def test_is_ready_to_pay_unauthorized(yandexpay_base_url, merchant, psp, merchant_origin, get_csrf_token):
    uid = 0
    yandexuid = '123'

    token = get_csrf_token(uid=uid, yandexuid=yandexuid)

    request_json = {
        'merchant_id': str(merchant.merchant_id),
        'merchant_origin': merchant_origin.origin,
        'existing_payment_method_required': True,
        'payment_methods': [{
            'type': 'CARD',
            'gateway': str(psp.psp_external_id),
            'gateway_merchant_id': 'some_merchant',
            'allowed_auth_methods': ['PAN_ONLY'],
            'allowed_card_networks': ['MASTERCARD'],
        }]
    }

    response = requests.post(
        url=yandexpay_base_url + '/api/v1/is_ready_to_pay',
        cookies={
            'yandexuid': yandexuid,
        },
        headers={
            'X-CSRF-TOKEN': token,
        },
        json=request_json,
    )

    assert response.status_code == 200
    print(response.text)


def test_is_ready_to_pay_authorized(yandexpay_base_url, merchant, psp, merchant_origin, get_csrf_token):
    uid = 5775
    yandexuid = '123'

    token = get_csrf_token(uid=uid, yandexuid=yandexuid)

    request_json = {
        'merchant_id': str(merchant.merchant_id),
        'merchant_origin': merchant_origin.origin,
        'existing_payment_method_required': True,
        'payment_methods': [{
            'type': 'CARD',
            'gateway': str(psp.psp_external_id),
            'gateway_merchant_id': 'some_merchant',
            'allowed_auth_methods': ['PAN_ONLY'],
            'allowed_card_networks': ['MASTERCARD'],
        }]
    }

    response = requests.post(
        url=yandexpay_base_url + '/api/v1/is_ready_to_pay',
        params={
            'default_uid': str(uid),
        },
        cookies={
            'yandexuid': yandexuid,
            'Session_id': 'sessionid',
        },
        headers={
            'X-CSRF-TOKEN': token,
        },
        json=request_json,
    )

    assert response.status_code == 200
    print(response.text)

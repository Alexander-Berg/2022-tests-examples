from bcl.core.models import Service


def test_getloginlink(api_client, response_mock):

    api_client.mock_service(Service.MARKET)

    def call(resp, status_code=200):

        with response_mock(
            'POST https://api.sandbox.payoneer.com/v2/programs/100101370/payees/login-link '
            f'-> {status_code} : {resp}'
        ):
            response = api_client.post('/api/proxy/payoneer/getloginlink/', data={
                'program_id': '100101370',
                'payee_id': '14327891927',
            })

        return response

    # Позитив.
    response = call(
        '{"audit_id":56025866,"code":0,"description":"Success",'
        '"login_link":"https://payouts.sandbox.payoneer.com/partners/lp.aspx?xxx"}')
    assert response.ok
    response = response.json
    assert not response['errors']
    assert response['data'] == {
        'audit_id': '56025866', 'code': 0, 'description': 'Success',
        'login_link': 'https://payouts.sandbox.payoneer.com/partners/lp.aspx?xxx'}

    # Негатив.
    response = call('{"audit_id":56025866,"code":666,"description":"Failure"}', status_code=404)
    assert response.status_code == 500
    response = response.json
    assert response['errors'] == [{'audit_id': '56025866', 'code': 666, 'description': 'Failure'}]


def test_getpayeestatus(api_client, response_mock):

    api_client.mock_service(Service.TOLOKA)

    def call(payee_id, resp, status_code=200):

        with response_mock(
            f'GET https://api.sandbox.payoneer.com/v2/programs/100101370/payees/{payee_id}/status '
            f'-> {status_code} : {resp}'
        ):
            response = api_client.post('/api/proxy/payoneer/getpayeestatus/', data={
                'program_id': '100101370',
                'payee_id': payee_id,
            })

        return response

    # Позитив.
    response = call('123321123', '{"audit_id":56026044,"code":0,"description":"Success","status":"ACTIVE"}')
    assert response.ok
    response = response.json
    assert not response['errors']
    assert response['data'] == {'audit_id': '56026044', 'code': 0, 'description': 'Success', 'status': 'ACTIVE'}

    # Негатив.
    response = call(
        '12332198903',
        '{"audit_id": 56026052, "code": 10005, "description": "Payee was not found", "hint": "Please ensure..."}',
        status_code=404)

    assert response.status_code == 500
    response = response.json
    assert response['errors'] == [{
        'audit_id': '56026052',
        'code': 10005,
        'description': 'Payee was not found',
        'hint': 'Please ensure...'
    }]

import mock


def test_check_passport_cookie(passport_client_mock):
    p = passport_client_mock.check_passport_cookie({}, 'host', 'ip', 'url')
    assert p.login is None

    resp = mock.Mock()
    resp.status_code = 200
    resp.headers = {'content-type': 'application/json'}
    resp.text = '{"status": {"value": "VALID"}, "error": "OK", "dbfields": {"accounts.login.uid": "uid"}}'
    r = mock.Mock(return_value=resp)
    with mock.patch('requests.Session.request', r):
        passport_client_mock.check_passport_cookie({'Session_id': 1}, 'host', 'ip', 'url')

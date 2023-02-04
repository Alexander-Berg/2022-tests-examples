import requests

from maps.infra.ratelimiter2.tools.pyhelpers.mocks import mock_backend, SimpleHandler, ProxyHandler


def test_run_and_handle():
    SECRET_STRING = 'FOO SERVICE IS READY'
    with mock_backend() as server:
        server.add('/sync', SimpleHandler(SECRET_STRING))
        assert SECRET_STRING == requests.get(f'{server.url}/sync').text


def test_correct_shutdown_on_exception():
    class TestException(Exception):
        pass

    try:
        with mock_backend():
            raise TestException()
    except TestException:
        pass


def test_multiple_handlers():
    with mock_backend() as server:
        for endpoint in ['/test', '/test2', '/']:
            server.add(endpoint, SimpleHandler(endpoint))

        assert '/test' == requests.get(f'{server.url}/test').text
        assert '/test' == requests.get(f'{server.url}/test?q=123').text
        # The '/test' rule matches before '/test2'
        assert '/test' == requests.get(f'{server.url}/test2').text
        assert '/' == requests.get(f'{server.url}/unknown').text


def test_proxy():
    with mock_backend() as server:
        def pong_handler(hnd):
            body_length = int(hnd.headers.get('content-length', '0'))
            hnd.send_response(200)
            hnd.end_headers()
            hnd.wfile.write(hnd.rfile.read(body_length) if body_length else b'TEST')

        server.add('', pong_handler)

        with mock_backend() as proxy:
            proxy.add('', ProxyHandler(server.url))

            assert requests.get(f'{proxy.url}/unknown').text == 'TEST'

            for data in [b'\rtest', b'test', b'\0\r\n\0']:
                assert data == requests.post(proxy.url, data=data).content

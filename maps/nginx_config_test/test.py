import pytest


def test_nginx_and_app_starts(nginx):
    pass


VHOST_CASES = [
    # Dedicated stable balancers
    'testapp.maps.yandex.net',
    'testapp.maps.yandex.ru',
    'testapp2.maps.yandex.net',
    'testapp2.maps.yandex.ru',
    '2x.testapp.maps.yandex.net',
    '2x.testapp.maps.yandex.ru',

    # Other dedicated balancers
    'testapp.unstable.maps.yandex.net',
    'testapp.testing.maps.yandex.net',
    'testapp.datatesting.maps.yandex.net',
    'testapp.datavalidation.maps.yandex.net',
    'testapp2.unstable.maps.yandex.net',
    'testapp2.testing.maps.yandex.net',
    'testapp2.datatesting.maps.yandex.net',
    'testapp2.datavalidation.maps.yandex.net',
    '2x.testapp.unstable.maps.yandex.net',
    '2x.testapp.testing.maps.yandex.net',
    '2x.testapp.datatesting.maps.yandex.net',
    '2x.testapp.datavalidation.maps.yandex.net',

    # Exact fqdn specified in code
    'testapp.maps.kinopoisk.ru',

    # Common balancers
    'testapp.common.unstable.maps.yandex.net',
    'testapp.common.testing.maps.yandex.net',
    'testapp.datatesting.maps.yandex.net',
    'testapp2.common.unstable.maps.yandex.net',
    'testapp2.common.testing.maps.yandex.net',
    'testapp2.datatesting.maps.yandex.net',
    '2x.testapp.common.unstable.maps.yandex.net',
    '2x.testapp.common.testing.maps.yandex.net',
    '2x.testapp.datatesting.maps.yandex.net',

    'testapp.unstable.maps.n.yandex.ru',
    'testapp.testing.maps.n.yandex.ru',
    'testapp.datatesting.maps.n.yandex.ru',
    'testapp2.unstable.maps.n.yandex.ru',
    'testapp2.testing.maps.n.yandex.ru',
    'testapp2.datatesting.maps.n.yandex.ru',
    '2x.testapp.unstable.maps.n.yandex.ru',
    '2x.testapp.testing.maps.n.yandex.ru',
    '2x.testapp.datatesting.maps.n.yandex.ru',

    # Containers fqdn
    'sas1-1702-sas-maps-core-dev-cpu10-160-32170.gencfg-c.yandex.net',
    'gvqvrppgzalswrtr.man.yp-c.yandex.net'
]


@pytest.mark.parametrize('vhost', VHOST_CASES)
def test_vhost(nginx, vhost):
    response = nginx.request(
        path='/ping',
        vhost=vhost
    )
    assert response.status_code == 200


def test_unknown_vhost(nginx):
    response = nginx.request(
        path='/ping',
        vhost='example.com'
    )
    assert response.status_code == 404


VHOST_WITH_CUSTOM_HANDLE_CASES = [
    ('testapp.maps.yandex.net', '/foo/bar', 404),
    ('testapp2.maps.yandex.net', '/foo/bar', 200),
    ('testapp3.maps.yandex.net', '/foo/bar', 200),
    ('testapp4.maps.yandex.net', '/foo/bar', 200),

    ('testapp.maps.yandex.net', '/foo/baz', 404),
    ('testapp2.maps.yandex.net', '/foo/baz', 404),
    ('testapp3.maps.yandex.net', '/foo/baz', 200),
    ('testapp4.maps.yandex.net', '/foo/baz', 404),
]


@pytest.mark.parametrize('vhost,handle,status_code', VHOST_WITH_CUSTOM_HANDLE_CASES)
def test_vhost_custom_handle(nginx, vhost, handle, status_code):
    response = nginx.request(
        path=handle,
        vhost=vhost
    )
    assert response.status_code == status_code


def test_prefix_smaller_location(nginx):
    vhost = VHOST_CASES[0]
    entity = 'e1'
    response = nginx.request(
        path=f'/prefix/{entity}/itemlist',
        vhost=vhost,
        method='get'
    )
    assert response.status_code == 200
    assert response.text == ':itemlist:'
    assert response.headers.get('X-Is-First-Location') == 'True'
    assert response.headers.get('X-Is-Second-Location') is None


def test_prefix_greater_location(nginx):
    vhost = VHOST_CASES[0]
    entity = 'e1'
    response = nginx.request(
        path=f'/prefix/{entity}/itemlist/import',
        vhost=vhost,
        method='post'
    )
    assert response.status_code == 200
    assert response.text == ':itemlist/import:'
    assert response.headers.get('X-Is-Second-Location') == 'True'
    assert response.headers.get('X-Is-First-Location') is None


def test_regex_trailing_slash(nginx):
    vhost = VHOST_CASES[0]
    dirname = 'd1'
    response = nginx.request(
        path=f'/dir/{dirname}/',
        vhost=vhost,
        method='get'
    )
    assert response.status_code == 200
    assert response.text == 'dir arg = d1\n'


def test_log_request_params(nginx):
    response = nginx.request(
        path='/log_request_params',
        vhost='testapp4.maps.yandex.net',
        method='get')
    assert response.status_code == 200
    assert response.text == 'OK'

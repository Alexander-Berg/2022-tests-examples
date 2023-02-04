import contextlib
import errno
import os
import random
import shutil

import mock
import requests
import pytest

from infra.ya_salt.lib import httpsalt


@contextlib.contextmanager
def rmtree_after_done(path):
    yield
    try:
        if os.path.isfile(path):
            os.unlink(path)
        else:
            shutil.rmtree(path)
    except EnvironmentError as e:
        if e.errno == errno.ENOENT:
            pass
        else:
            raise


def test_gen_cookie():
    masters = [
        'stout21.search.yandex.net',
        'stout22.search.yandex.net',
        'stout23.search.yandex.net',
        'stout24.search.yandex.net',
        'stout25.search.yandex.net',
    ]
    v1 = httpsalt.gen_cookie(masters)
    assert v1
    random.shuffle(masters)
    v2 = httpsalt.gen_cookie(masters)
    assert v2
    assert v1 == v2, 'order matters'


def test_modified_to_seconds():
    mts = httpsalt.modified_to_seconds
    assert mts('Tue, 30 Apr 2019 14:01:35 GMT') == 1556632895
    with pytest.raises(Exception):
        mts('should raise')


def test_seconds_to_modified():
    stm = httpsalt.seconds_to_modified
    assert stm(1556632895) == 'Tue, 30 Apr 2019 14:01:35 GMT'


def test_ensure_dir():
    ed = httpsalt.HttpSalt.ensure_dir
    path = './test-ensure-dir'
    # Test creation failed - file instead of directory
    open(path, 'w').close()
    with rmtree_after_done(path):
        err = ed(path)
        assert err is not None and err.startswith('path ')
    # Test creation succeeded
    with rmtree_after_done(path):
        assert ed(path) is None
        assert os.path.isdir(path)


def test_fetch_content():
    fc = httpsalt.HttpSalt.fetch_content
    # Test GET raises exception
    http_get_func = mock.Mock()
    http_get_func.side_effect = Exception('You fail me!')
    _, err = fc('http://test.local:8080', {'User-Agent': 'Test'}, 17, http_get_func)
    assert err is not None
    http_get_func.assert_called_once_with('http://test.local:8080', headers={'User-Agent': 'Test'}, timeout=17)
    # Test 5xx response
    resp = requests.Response()
    resp.status_code = 504
    resp.reason = 'Gateway Timeout'
    http_get_func.reset_mock(side_effect=True)
    http_get_func.return_value = resp
    _, err = fc('http://test.local:8080', {'User-Agent': 'Test'}, 17, http_get_func)
    assert err is not None and err == 'bad http code: 504 Gateway Timeout'
    http_get_func.assert_called_once_with('http://test.local:8080', headers={'User-Agent': 'Test'}, timeout=17)
    # Test Not Modified
    http_get_func.reset_mock()
    resp.status_code = 304
    resp.reason = 'Not Modified'
    res, err = fc('http://test.local:8080', {'User-Agent': 'Test'}, 17, http_get_func)
    assert err is None
    assert res.has_changed is False
    # Test 200 OK  without Last-Modified
    http_get_func.reset_mock()
    resp.status_code = 200
    resp.reason = 'OK'
    res, err = fc('http://test.local:8080', {'User-Agent': 'Test'}, 17, http_get_func)
    assert err is not None and err == 'no "Last-Modified" header in response'
    # Test invalid Last-Modified
    http_get_func.reset_mock()
    resp.status_code = 200
    resp.reason = 'OK'
    resp.headers['Last-Modified'] = "Can't parse this!"
    res, err = fc('http://test.local:8080', {'User-Agent': 'Test'}, 17, http_get_func)
    assert err is not None and err.startswith('failed to parse')
    # Test 200 OK and: no content-length
    resp.status_code = 200
    resp.reason = 'OK'
    resp.headers['Last-Modified'] = "Tue, 30 Apr 2019 14:01:35 GMT"
    resp.headers['Content-Length'] = ''
    res, err = fc('http://test.local:8080', {'User-Agent': 'Test'}, 17, http_get_func)
    assert err is not None and err.startswith('no content-length in response')
    # Test 200 OK and: invalid content length
    resp.status_code = 200
    resp.reason = 'OK'
    resp.headers['Last-Modified'] = "Tue, 30 Apr 2019 14:01:35 GMT"
    resp.headers['Content-Length'] = 'BaNaN"a'
    res, err = fc('http://test.local:8080', {'User-Agent': 'Test'}, 17, http_get_func)
    assert err is not None and err.startswith('failed to parse content-length')
    # Test 200 OK and: short response
    resp.status_code = 200
    resp.reason = 'OK'
    resp.headers['Last-Modified'] = "Tue, 30 Apr 2019 14:01:35 GMT"
    resp.headers['Content-Length'] = '1000'
    resp._content = 'ten bytes aaaand cut'
    res, err = fc('http://test.local:8080', {'User-Agent': 'Test'}, 17, http_get_func)
    assert err is not None and err.startswith('incorrect response size: got=20 expected=1000')
    # Test 200 OK and modified OK
    http_get_func.reset_mock()
    resp.status_code = 200
    resp.reason = 'OK'
    resp.headers['Last-Modified'] = "Tue, 30 Apr 2019 14:01:35 GMT"
    resp._content = 'This is zip'
    resp.headers['Content-Length'] = str(len(resp._content))
    res, err = fc('http://test.local:8080', {'User-Agent': 'Test'}, 17, http_get_func)
    assert err is None
    assert res.has_changed is True
    assert res.mtime == 1556632895
    assert res.content == resp.content

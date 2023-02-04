import mock

from infra.swatlib.flaskutil import handler_timeout


@handler_timeout.abort_on_timeout(timeout=1)
def handler_explicit_timeout():
    return 'response'


@handler_timeout.abort_on_timeout()
def handler():
    return 'response'


@mock.patch('flask.has_request_context')
@mock.patch('flask.request')
@mock.patch('gevent.Timeout')
def test_abort_on_timeount_post_method(g, r, rc):
    r.method = 'POST'
    r.headers = {}
    rc.return_value = True
    rsp = handler_explicit_timeout()
    assert not g.called
    assert rsp == 'response'


@mock.patch('flask.has_request_context')
@mock.patch('flask.request')
@mock.patch('gevent.Timeout')
def test_abort_on_timeount_get_method(g, r, rc):
    r.method = 'GET'
    r.headers = {}
    rc.return_value = True
    rsp = handler_explicit_timeout()
    g.assert_called_once_with(1)
    assert rsp == 'response'


@mock.patch('flask.has_request_context')
@mock.patch('flask.request')
@mock.patch('gevent.Timeout')
def test_abort_on_timeount_header_timeout(g, r, rc):
    r.method = 'GET'
    r.headers = {'X-Backend-Timeout': '1.2'}
    rc.return_value = True
    rsp = handler()
    g.assert_called_once_with(1.2)
    assert rsp == 'response'

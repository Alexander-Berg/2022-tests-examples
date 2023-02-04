import mock
import pytest
from sepelib.flask.auth.util import login_exempt
from sepelib.flask.h import args, combo, error_handlers, mime, mongo, util
from sepelib.flask import perfcounter, server, statuscounter
from werkzeug.exceptions import NotFound


def test_util_login_exempt():
    v = mock.Mock()
    v.need_auth = True
    login_exempt(v)
    assert not v.need_auth


def test_args():
    r = args.parse_list_req({"skip": 100, "limit": 10})
    assert r.skip == 100
    assert r.limit == 10


def test_combo(flask_test_app):
    with mock.patch('sepelib.core.config.get_value', return_value='json'):
        with flask_test_app.test_request_context():
            r = combo.empty_doc_response()
            assert r.status_code == 200


def test_error_handlers(flask_test_app):
    error_handlers.register_error_handlers(flask_test_app)


def test_mime(flask_test_app):
    assert mime.render_txt({"a": "b"}) == "a:b"


def test_get_mime(flask_test_app):
    with mock.patch('sepelib.core.config.get_value', return_value='json'):
        with flask_test_app.test_request_context():
            assert mime.choose_mime() == 'application/json'


def test_mongo():
    k = mock.Mock()
    k.DoesNotExist = Exception
    def e():
        raise k.DoesNotExist()
    k.objects = mock.Mock()
    k.objects.get = mock.Mock(side_effect=e)
    with pytest.raises(NotFound):
        mongo.get_obj_or_404(k)


def test_util():
    assert "Fri, 15 Jan 2027 08:00:00 GMT" == util.date_to_str(1800000000)
    assert util.format_percent((1, 0)) == 0


def test_perfcounter(flask_test_app):
    p = perfcounter.PerfCounter()
    p.augment(flask_test_app)
    assert "rule_timings" in p.get_snapshot()


def test_server(flask_test_app):
    server.setup_access_log_stream({'filepath': "stream_file"})
    cfg = {'web': {'http': {'host': 'localhost_test', 'port': 0}}}
    with mock.patch('sepelib.flask.server.WebServer._create_server_socket', return_value=mock.Mock()):
        with mock.patch('gevent.pywsgi.WSGIServer', return_value=mock.Mock()):
            server.WebServer(cfg, flask_test_app, '1.test')


def test_status_counter(flask_test_app):
    c = statuscounter.StatusCounter()
    c.augment(flask_test_app)

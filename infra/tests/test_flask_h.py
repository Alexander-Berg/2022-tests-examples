import flask
from six.moves import http_client as httplib
from sepelib.flask.h.error_handlers import error_happened


def test_html_error_page():
    app = flask.Flask(__name__)
    @app.route('/')
    def except_me():
        raise Exception

    app.register_error_handler(Exception, error_happened)
    response = app.test_client().get('/')
    assert response.status_code == httplib.INTERNAL_SERVER_ERROR
    assert 'text/html' in response.content_type
    assert b'<title>Error happened</title>' in response.data  # Pretty fragile check
